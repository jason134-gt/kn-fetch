import os
import json
import yaml
import git
import hashlib
import multiprocessing as mp
from typing import List, Dict, Any, Optional, Tuple
from pathlib import Path
from concurrent.futures import ProcessPoolExecutor, as_completed
from tqdm import tqdm
import pandas as pd
import networkx as nx
from sqlalchemy import create_engine, Column, String, Text, DateTime, Integer, JSON
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from datetime import datetime
import tempfile
import shutil

from .models import CodeEntity, KnowledgeGraph, AnalysisResult, ExportFormat
from .exceptions import GitNexusError, AnalysisError, ConfigurationError, ExportError
from .parser import CodeParser
from .incremental import IncrementalAnalyzer
from .exporter import KnowledgeExporter

Base = declarative_base()

class AnalysisCache(Base):
    __tablename__ = "analysis_cache"
    file_path = Column(String(1024), primary_key=True)
    file_hash = Column(String(64), index=True)
    analysis_result = Column(JSON)
    analyzed_at = Column(DateTime)
    version = Column(String(64))

class GitNexusClient:
    """
    GitNexus大型项目知识提取客户端
    支持百万行代码级别的工程分析，具备增量分析、并行处理、
    结构化知识存储、多格式导出能力
    """
    
    def __init__(self, config_path: str = ".gittnexus.yaml"):
        self.config = self._load_config(config_path)
        self.root_dir = Path(self.config["project"]["root_dir"]).resolve()
        self.repo = self._init_git_repo()
        self.parser = CodeParser()
        self.incremental_analyzer = IncrementalAnalyzer(self.repo)
        self.db_engine = self._init_database()
        self.Session = sessionmaker(bind=self.db_engine)
        self.max_workers = self.config.get("parallel", {}).get("max_workers", mp.cpu_count())
        self.batch_size = self.config.get("performance", {}).get("batch_size", 100)
        
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """加载配置文件"""
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                return yaml.safe_load(f)
        except Exception as e:
            raise ConfigurationError(f"加载配置文件失败: {str(e)}")
    
    def _init_git_repo(self) -> git.Repo:
        """初始化Git仓库"""
        try:
            return git.Repo(self.root_dir)
        except Exception as e:
            raise GitNexusError(f"初始化Git仓库失败: {str(e)}")
    
    def _init_database(self) -> Any:
        """初始化数据库连接"""
        db_path = self.config.get("storage", {}).get("db_path", ".gitnexus_cache.db")
        engine = create_engine(f"sqlite:///{db_path}")
        Base.metadata.create_all(engine)
        return engine
    
    def _get_file_hash(self, file_path: Path) -> str:
        """计算文件哈希值用于增量分析"""
        hasher = hashlib.sha256()
        with open(file_path, "rb") as f:
            while chunk := f.read(8192):
                hasher.update(chunk)
        return hasher.hexdigest()
    
    def _get_all_target_files(self) -> List[Path]:
        """获取所有需要分析的目标文件"""
        include_patterns = self.config["analysis"]["include"]
        exclude_patterns = self.config["analysis"]["exclude"]
        
        files = []
        for pattern in include_patterns:
            for file_path in self.root_dir.glob(pattern):
                if file_path.is_file():
                    # 检查是否在排除列表中
                    excluded = False
                    for exclude_pattern in exclude_patterns:
                        if file_path.match(exclude_pattern):
                            excluded = True
                            break
                    if not excluded:
                        files.append(file_path)
        
        return files
    
    def _analyze_single_file(self, file_path: Path, force: bool = False) -> Optional[AnalysisResult]:
        """分析单个文件"""
        try:
            relative_path = file_path.relative_to(self.root_dir)
            file_hash = self._get_file_hash(file_path)
            
            # 检查缓存
            if not force:
                with self.Session() as session:
                    cache = session.query(AnalysisCache).filter(
                        AnalysisCache.file_path == str(relative_path),
                        AnalysisCache.file_hash == file_hash
                    ).first()
                    if cache:
                        return AnalysisResult(**cache.analysis_result)
            
            # 解析文件
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            entities = self.parser.parse(content, str(relative_path))
            
            result = AnalysisResult(
                file_path=str(relative_path),
                file_hash=file_hash,
                language=self.parser.detect_language(content),
                entities=entities,
                lines_of_code=len(content.splitlines()),
                analyzed_at=datetime.now().isoformat()
            )
            
            # 更新缓存
            with self.Session() as session:
                cache = AnalysisCache(
                    file_path=str(relative_path),
                    file_hash=file_hash,
                    analysis_result=result.dict(),
                    analyzed_at=datetime.now(),
                    version=self.repo.head.commit.hexsha
                )
                session.merge(cache)
                session.commit()
            
            return result
            
        except Exception as e:
            print(f"分析文件 {file_path} 失败: {str(e)}")
            return None
    
    def analyze_full(self, force: bool = False) -> KnowledgeGraph:
        """全量分析项目"""
        all_files = self._get_all_target_files()
        print(f"开始全量分析，共 {len(all_files)} 个文件")
        
        results = []
        with ProcessPoolExecutor(max_workers=self.max_workers) as executor:
            futures = [executor.submit(self._analyze_single_file, file, force) for file in all_files]
            
            for future in tqdm(as_completed(futures), total=len(futures), desc="分析进度"):
                result = future.result()
                if result:
                    results.append(result)
        
        # 构建知识图谱
        graph = KnowledgeGraph.build_from_results(results)
        self._save_knowledge_graph(graph)
        
        return graph
    
    def analyze_incremental(self, base_commit: Optional[str] = None, head_commit: Optional[str] = None) -> KnowledgeGraph:
        """增量分析项目"""
        changed_files = self.incremental_analyzer.get_changed_files(base_commit, head_commit)
        print(f"检测到 {len(changed_files)} 个变更文件")
        
        results = []
        for file_path in tqdm(changed_files, desc="增量分析进度"):
            full_path = self.root_dir / file_path
            if full_path.exists() and full_path.is_file():
                result = self._analyze_single_file(full_path, force=True)
                if result:
                    results.append(result)
        
        # 合并到现有知识图谱
        existing_graph = self._load_knowledge_graph()
        new_graph = KnowledgeGraph.build_from_results(results)
        merged_graph = existing_graph.merge(new_graph)
        
        self._save_knowledge_graph(merged_graph)
        return merged_graph
    
    def _save_knowledge_graph(self, graph: KnowledgeGraph):
        """保存知识图谱到存储"""
        output_config = self.config["output"]
        output_path = Path(output_config["path"])
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path.with_suffix(".json"), "w", encoding="utf-8") as f:
            json.dump(graph.dict(), f, ensure_ascii=False, indent=2)
        
        # 也保存到数据库
        with self.Session() as session:
            # 保存节点
            for entity in graph.entities.values():
                # 实现节点保存逻辑
                pass
        
    def _load_knowledge_graph(self) -> KnowledgeGraph:
        """加载已有的知识图谱"""
        output_config = self.config["output"]
        output_path = Path(output_config["path"]).with_suffix(".json")
        
        if output_path.exists():
            with open(output_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                return KnowledgeGraph(**data)
        
        return KnowledgeGraph(entities={}, relationships=[])
    
    def export(self, output_path: Optional[str] = None, format: ExportFormat = ExportFormat.MARKDOWN) -> str:
        """导出分析结果"""
        graph = self._load_knowledge_graph()
        exporter = KnowledgeExporter(graph, self.config)
        
        if output_path is None:
            output_path = self.config["output"]["path"]
        
        return exporter.export(output_path, format)
    
    def get_statistics(self) -> Dict[str, Any]:
        """获取分析统计信息"""
        graph = self._load_knowledge_graph()
        
        stats = {
            "total_files": len(set(e.file_path for e in graph.entities.values())),
            "total_entities": len(graph.entities),
            "total_relationships": len(graph.relationships),
            "lines_of_code": sum(e.lines_of_code for e in graph.entities.values() if hasattr(e, "lines_of_code")),
            "entity_types": {}
        }
        
        for entity in graph.entities.values():
            entity_type = entity.entity_type
            stats["entity_types"][entity_type] = stats["entity_types"].get(entity_type, 0) + 1
        
        return stats
    
    def clear_cache(self):
        """清除分析缓存"""
        with self.Session() as session:
            session.query(AnalysisCache).delete()
            session.commit()
        
        db_path = self.config.get("storage", {}).get("db_path", ".gitnexus_cache.db")
        if os.path.exists(db_path):
            os.remove(db_path)
        
        output_config = self.config["output"]
        json_path = Path(output_config["path"]).with_suffix(".json")
        if json_path.exists():
            os.remove(json_path)
        
        print("缓存已清除")
