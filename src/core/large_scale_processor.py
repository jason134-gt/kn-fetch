import os
import json
import mmap
import tempfile
import shutil
from typing import List, Dict, Any, Optional, Iterator
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import multiprocessing as mp
from tqdm import tqdm
import psutil
import sqlite3
from datetime import datetime

from ..gitnexus import KnowledgeGraph, AnalysisResult
from .knowledge_extractor import KnowledgeExtractor

class LargeScaleProcessor:
    """百万级规模代码/文档处理器
    实现内存优化、增量处理、分块处理、断点续传等特性，支持超大规模项目处理
    """
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.config = self._load_config(config_path)
        self.extractor = KnowledgeExtractor(config_path)
        self.max_workers = self.config.get("performance", {}).get("max_workers", mp.cpu_count())
        self.batch_size = self.config.get("performance", {}).get("batch_size", 200)
        self.memory_limit_gb = self.config.get("performance", {}).get("memory_limit_gb", 8)
        self.temp_dir = Path(tempfile.mkdtemp(prefix="kn_fetch_"))
        self.db_path = self.temp_dir / "processing_cache.db"
        self._init_cache_db()
        
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """加载配置文件"""
        import yaml
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                return yaml.safe_load(f)
        except Exception as e:
            raise Exception(f"加载配置文件失败: {str(e)}")
    
    def _init_cache_db(self):
        """初始化缓存数据库"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute('''
        CREATE TABLE IF NOT EXISTS processed_files (
            file_path TEXT PRIMARY KEY,
            file_hash TEXT,
            processed_at TIMESTAMP,
            status TEXT,
            result_path TEXT
        )
        ''')
        
        cursor.execute('''
        CREATE TABLE IF NOT EXISTS processing_state (
            key TEXT PRIMARY KEY,
            value TEXT
        )
        ''')
        
        conn.commit()
        conn.close()
    
    def process_large_project(
        self,
        directory_path: str,
        include_code: bool = True,
        include_docs: bool = True,
        incremental: bool = True,
        resume: bool = False
    ) -> KnowledgeGraph:
        """处理超大规模项目，支持百万行级别代码/文档
        Args:
            directory_path: 项目目录路径
            include_code: 是否包含代码分析
            include_docs: 是否包含文档分析
            incremental: 是否启用增量分析
            resume: 是否断点续传
        Returns:
            完整的知识图谱
        """
        directory = Path(directory_path).resolve()
        if not directory.exists():
            raise ValueError(f"目录不存在: {directory_path}")
        
        print(f"开始处理大规模项目: {directory}")
        
        # 获取所有目标文件
        all_files = self.extractor._get_all_files(directory, include_code, include_docs)
        total_files = len(all_files)
        print(f"发现 {total_files} 个目标文件")
        
        # 过滤已处理文件（增量/续传）
        if incremental or resume:
            processed_files = self._get_processed_files()
            files_to_process = []
            
            for file_path in all_files:
                file_hash = self._get_file_hash(file_path)
                if str(file_path) in processed_files:
                    if processed_files[str(file_path)] == file_hash and resume:
                        continue
                files_to_process.append(file_path)
            
            print(f"需要处理 {len(files_to_process)} 个文件（跳过 {total_files - len(files_to_process)} 个已处理文件）")
        else:
            files_to_process = all_files
            self._clear_cache()
        
        if not files_to_process:
            print("没有需要处理的文件")
            return self._load_final_graph()
        
        # 分批次处理
        total_batches = (len(files_to_process) + self.batch_size - 1) // self.batch_size
        all_result_paths = []
        
        for batch_idx in range(total_batches):
            start_idx = batch_idx * self.batch_size
            end_idx = min((batch_idx + 1) * self.batch_size, len(files_to_process))
            batch = files_to_process[start_idx:end_idx]
            
            print(f"\n处理批次 {batch_idx + 1}/{total_batches}，共 {len(batch)} 个文件")
            self._check_memory_usage()
            
            batch_results = self._process_batch(batch)
            
            # 保存批次结果到临时文件
            batch_result_path = self.temp_dir / f"batch_{batch_idx}.json"
            with open(batch_result_path, "w", encoding="utf-8") as f:
                json.dump([r.model_dump() for r in batch_results if r], f, ensure_ascii=False)
            
            all_result_paths.append(batch_result_path)
            
            # 更新处理状态
            self._mark_batch_processed(batch, batch_result_path)
        
        # 合并所有批次结果
        print("\n开始合并所有处理结果...")
        final_graph = self._merge_batch_results(all_result_paths)
        
        # 保存最终结果
        self._save_final_graph(final_graph)
        
        # 清理临时文件
        self._cleanup_temp_files()
        
        print(f"处理完成，共生成 {len(final_graph.entities)} 个实体，{len(final_graph.relationships)} 个关系")
        
        return final_graph
    
    def _process_batch(self, batch: List[Path]) -> List[Optional[AnalysisResult]]:
        """处理单个批次的文件"""
        results = []
        
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            futures = []
            for file_path in batch:
                if self.extractor._is_code_file(file_path):
                    futures.append(executor.submit(self.extractor._process_code_file, file_path, False))
                elif self.extractor._is_document_file(file_path):
                    futures.append(executor.submit(self.extractor._process_document_file, file_path, False))
            
            for future in tqdm(futures, desc="批次处理进度"):
                result = future.result()
                if result:
                    results.append(result)
        
        return results
    
    def _get_processed_files(self) -> Dict[str, str]:
        """获取已处理的文件列表"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        cursor.execute("SELECT file_path, file_hash FROM processed_files WHERE status = 'success'")
        results = cursor.fetchall()
        
        conn.close()
        
        return {row[0]: row[1] for row in results}
    
    def _mark_batch_processed(self, batch: List[Path], result_path: Path):
        """标记批次为已处理"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        for file_path in batch:
            file_hash = self._get_file_hash(file_path)
            cursor.execute('''
            INSERT OR REPLACE INTO processed_files 
            (file_path, file_hash, processed_at, status, result_path)
            VALUES (?, ?, ?, ?, ?)
            ''', (
                str(file_path),
                file_hash,
                datetime.now().isoformat(),
                "success",
                str(result_path)
            ))
        
        conn.commit()
        conn.close()
    
    def _merge_batch_results(self, result_paths: List[Path]) -> KnowledgeGraph:
        """合并多个批次的处理结果"""
        all_entities = {}
        all_relationships = []
        
        for result_path in tqdm(result_paths, desc="合并结果进度"):
            with open(result_path, "r", encoding="utf-8") as f:
                batch_results = json.load(f)
            
            for result_dict in batch_results:
                result = AnalysisResult(**result_dict)
                for entity in result.entities:
                    all_entities[entity.id] = entity
                all_relationships.extend(result.relationships)
        
        # 去重关系
        unique_rels = []
        seen_rel_keys = set()
        for rel in all_relationships:
            key = f"{rel.source_id}:{rel.target_id}:{rel.relationship_type}"
            if key not in seen_rel_keys:
                seen_rel_keys.add(key)
                unique_rels.append(rel)
        
        return KnowledgeGraph(
            entities=all_entities,
            relationships=unique_rels,
            generated_at=datetime.now().isoformat()
        )
    
    def _check_memory_usage(self):
        """检查内存使用情况，避免OOM"""
        memory_usage = psutil.Process().memory_info().rss / (1024 ** 3)
        if memory_usage > self.memory_limit_gb * 0.8:
            print(f"警告：内存使用已达 {memory_usage:.2f}GB，接近限制 {self.memory_limit_gb}GB，正在执行GC...")
            import gc
            gc.collect()
    
    def _get_file_hash(self, file_path: Path) -> str:
        """计算文件哈希值（使用快速哈希）"""
        import hashlib
        hasher = hashlib.sha1()
        with open(file_path, "rb") as f:
            while chunk := f.read(65536):
                hasher.update(chunk)
        return hasher.hexdigest()
    
    def _save_final_graph(self, graph: KnowledgeGraph):
        """保存最终知识图谱"""
        output_config = self.config.get("output", {})
        output_path = Path(output_config.get("path", "output/knowledge_graph.json"))
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(graph.model_dump(), f, ensure_ascii=False, indent=2)
    
    def _load_final_graph(self) -> KnowledgeGraph:
        """加载已有的最终知识图谱"""
        output_config = self.config.get("output", {})
        output_path = Path(output_config.get("path", "output/knowledge_graph.json"))
        
        if output_path.exists():
            with open(output_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                return KnowledgeGraph(**data)
        
        return KnowledgeGraph(entities={}, relationships=[])
    
    def _clear_cache(self):
        """清空处理缓存"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM processed_files")
        cursor.execute("DELETE FROM processing_state")
        conn.commit()
        conn.close()
        
        # 清空临时目录
        for file in self.temp_dir.glob("*.json"):
            file.unlink()
    
    def _cleanup_temp_files(self):
        """清理临时文件"""
        shutil.rmtree(self.temp_dir, ignore_errors=True)
