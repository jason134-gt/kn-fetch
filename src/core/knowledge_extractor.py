import os
import json
from typing import List, Dict, Any, Optional, Union
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import multiprocessing as mp
from tqdm import tqdm
import hashlib
import mmap

from ..gitnexus import GitNexusClient, KnowledgeGraph, AnalysisResult
from ..gitnexus.models import CodeEntity
from .document_parser import DocumentParser
from .knowledge_document_generator import KnowledgeDocumentGenerator
from ..ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer

class KnowledgeExtractor:
    """核心知识提取引擎
    支持代码和文档目录的结构化知识提取，具备百万级规模处理能力
    """
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.config = self._load_config(config_path)
        self.gitnexus_client = GitNexusClient(config_path)
        self.document_parser = DocumentParser()
        self.max_workers = self.config.get("performance", {}).get("max_workers", mp.cpu_count())
        self.batch_size = self.config.get("performance", {}).get("batch_size", 100)
        self.memory_limit = self.config.get("performance", {}).get("memory_limit_gb", 8) * 1024 * 1024 * 1024
        
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """加载配置文件"""
        import yaml
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                return yaml.safe_load(f)
        except Exception as e:
            raise Exception(f"加载配置文件失败: {str(e)}")
    
    def extract_from_directory(
        self,
        directory_path: str,
        include_code: bool = True,
        include_docs: bool = True,
        force: bool = False,
        deep_analysis: bool = False
    ) -> KnowledgeGraph:
        """从指定目录提取结构化知识
        Args:
            directory_path: 目标目录路径
            include_code: 是否包含代码分析
            include_docs: 是否包含文档分析
            force: 是否强制重新分析，忽略缓存
            deep_analysis: 是否启用LLM深度分析（生成设计文档）
        Returns:
            完整的知识图谱
        """
        directory = Path(directory_path).resolve()
        if not directory.exists():
            raise ValueError(f"目录不存在: {directory_path}")
        
        # 更新 gitnexus_client 的 root_dir 为当前分析目录
        self.gitnexus_client.root_dir = directory
        
        print(f"开始分析目录: {directory}")
        
        # 获取所有目标文件
        all_files = self._get_all_files(directory, include_code, include_docs)
        print(f"发现 {len(all_files)} 个目标文件")
        
        # 分批次处理，避免内存溢出
        all_results = []
        batches = [all_files[i:i+self.batch_size] for i in range(0, len(all_files), self.batch_size)]
        
        for batch_idx, batch in enumerate(batches):
            print(f"处理批次 {batch_idx + 1}/{len(batches)}，共 {len(batch)} 个文件")
            
            with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
                futures = []
                for file_path in batch:
                    if self._is_code_file(file_path):
                        futures.append(executor.submit(self._process_code_file, file_path, force))
                    elif self._is_document_file(file_path):
                        futures.append(executor.submit(self._process_document_file, file_path, force))
                
                for future in tqdm(futures, desc="处理进度"):
                    result = future.result()
                    if result:
                        all_results.append(result)
        
        # 构建知识图谱
        graph = KnowledgeGraph.build_from_results(all_results)
        
        # 知识后处理和优化
        graph = self._optimize_knowledge_graph(graph)
        
        # 保存结果
        self._save_knowledge_graph(graph, directory_path, deep_analysis)
        
        return graph
    
    def _get_all_files(self, directory: Path, include_code: bool, include_docs: bool) -> List[Path]:
        """获取所有需要分析的文件"""
        files = []
        
        include_patterns = []
        if include_code:
            include_patterns.extend(self.config.get("analysis", {}).get("include_code", ["**/*.py", "**/*.js", "**/*.ts", "**/*.java", "**/*.cpp", "**/*.go"]))
        if include_docs:
            include_patterns.extend(self.config.get("analysis", {}).get("include_docs", ["**/*.md", "**/*.txt", "**/*.rst", "**/*.docx", "**/*.pdf"]))
            
        exclude_patterns = self.config.get("analysis", {}).get("exclude", ["**/node_modules/**", "**/.git/**", "**/__pycache__/**", "**/dist/**", "**/build/**"])
        
        for pattern in include_patterns:
            for file_path in directory.glob(pattern):
                if file_path.is_file():
                    # 检查排除规则
                    excluded = False
                    for exclude_pattern in exclude_patterns:
                        if file_path.match(exclude_pattern):
                            excluded = True
                            break
                    if not excluded:
                        files.append(file_path)
        
        return files
    
    def _is_code_file(self, file_path: Path) -> bool:
        """判断是否为代码文件"""
        code_extensions = ['.py', '.js', '.jsx', '.ts', '.tsx', '.java', '.cpp', '.cc', '.cxx', '.h', '.hpp', '.go', '.rs', '.php', '.rb', '.cs']
        return file_path.suffix.lower() in code_extensions
    
    def _is_document_file(self, file_path: Path) -> bool:
        """判断是否为文档文件"""
        doc_extensions = ['.md', '.txt', '.rst', '.docx', '.pdf', '.html', '.htm', '.doc', '.ppt', '.pptx']
        return file_path.suffix.lower() in doc_extensions
    
    def _process_code_file(self, file_path: Path, force: bool) -> Optional[AnalysisResult]:
        """处理单个代码文件"""
        try:
            # 使用GitNexus分析代码
            return self.gitnexus_client._analyze_single_file(file_path, force)
        except Exception as e:
            print(f"处理代码文件 {file_path} 失败: {str(e)}")
            return None
    
    def _process_document_file(self, file_path: Path, force: bool) -> Optional[AnalysisResult]:
        """处理单个文档文件"""
        try:
            return self.document_parser.parse_document(file_path)
        except Exception as e:
            print(f"处理文档文件 {file_path} 失败: {str(e)}")
            return None
    
    def _optimize_knowledge_graph(self, graph: KnowledgeGraph) -> KnowledgeGraph:
        """优化知识图谱，去重、补全关系、规范化"""
        # 去重实体
        unique_entities = {}
        seen_entity_keys = set()
        
        for entity_id, entity in graph.entities.items():
            key = f"{entity.file_path}:{entity.name}:{entity.start_line}"
            if key not in seen_entity_keys:
                seen_entity_keys.add(key)
                unique_entities[entity_id] = entity
        
        # 去重关系
        unique_rels = []
        seen_rel_keys = set()
        
        for rel in graph.relationships:
            key = f"{rel.source_id}:{rel.target_id}:{rel.relationship_type}"
            if key not in seen_rel_keys:
                seen_rel_keys.add(key)
                unique_rels.append(rel)
        
        graph.entities = unique_entities
        graph.relationships = unique_rels
        
        return graph
    
    def _save_knowledge_graph(self, graph: KnowledgeGraph, source_dir: str = None, deep_analysis: bool = False):
        """保存知识图谱到存储"""
        output_config = self.config.get("output", {})
        output_path = Path(output_config.get("path", "output/knowledge_graph.json"))
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(graph.model_dump(), f, ensure_ascii=False, indent=2)
        
        # 生成 Skill 格式的知识文档
        print("生成知识文档...")
        doc_generator = KnowledgeDocumentGenerator(output_dir="output/doc")
        doc_result = doc_generator.generate_all_documents(graph)
        print(f"知识文档生成完成:")
        print(f"  - 索引: {doc_result['index']}")
        print(f"  - 实体文档: {len(doc_result['entities'])} 个")
        print(f"  - 模块文档: {len(doc_result['modules'])} 个")
        
        # LLM深度分析（生成设计文档）
        if deep_analysis:
            print("\n启动LLM深度分析...")
            deep_analyzer = DeepKnowledgeAnalyzer(self.config, output_dir="output/doc")
            if deep_analyzer.is_available():
                deep_result = deep_analyzer.analyze_all(graph, source_dir)
                print(f"\n深度分析文档生成完成:")
                for key, value in deep_result.items():
                    if isinstance(value, dict) and value.get('file'):
                        print(f"  - {key}: {value['file']}")
            else:
                print("警告: LLM不可用，请配置API Key环境变量 (ARK_API_KEY)")
                print("  详细设计文档生成已跳过")
    
    def get_statistics(self) -> Dict[str, Any]:
        """获取分析统计信息"""
        return self.gitnexus_client.get_statistics()
