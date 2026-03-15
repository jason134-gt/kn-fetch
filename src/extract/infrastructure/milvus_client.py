"""
Milvus向量数据库客户端 - 专门用于语义向量存储和相似性搜索

功能：
- 向量数据库连接管理
- 语义向量存储
- 相似性搜索
- 向量索引管理
"""

import os
import json
import numpy as np
from typing import Dict, Any, List, Optional, Tuple
import logging


class MilvusClient:
    """Milvus向量数据库客户端"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("milvus.client")
        self._client = None
        self._collection = None
        self._connected = False
    
    async def connect(self) -> bool:
        """连接Milvus数据库"""
        try:
            from pymilvus import connections, utility
            
            host = self.config.get("host", "localhost")
            port = self.config.get("port", "19530")
            
            connections.connect("default", host=host, port=port)
            
            # 检查连接
            if utility.has_collection("code_embeddings"):
                from pymilvus import Collection
                self._collection = Collection("code_embeddings")
            
            self._connected = True
            self.logger.info("Milvus连接成功")
            return True
            
        except Exception as e:
            self.logger.error(f"Milvus连接失败: {e}")
            self._connected = False
            return False
    
    async def disconnect(self):
        """断开Milvus连接"""
        if self._connected:
            from pymilvus import connections
            connections.disconnect("default")
            self._connected = False
            self.logger.info("Milvus连接已断开")
    
    async def is_connected(self) -> bool:
        """检查连接状态"""
        return self._connected
    
    async def create_collection(self, collection_name: str = "code_embeddings", 
                              dimension: int = 768) -> bool:
        """创建向量集合"""
        try:
            from pymilvus import (
                connections, FieldSchema, CollectionSchema, DataType,
                Collection, utility
            )
            
            if utility.has_collection(collection_name):
                self._collection = Collection(collection_name)
                self.logger.info(f"集合 {collection_name} 已存在")
                return True
            
            # 定义字段
            fields = [
                FieldSchema("id", DataType.INT64, is_primary=True, auto_id=True),
                FieldSchema("entity_id", DataType.INT64),
                FieldSchema("entity_type", DataType.VARCHAR, max_length=50),
                FieldSchema("content", DataType.VARCHAR, max_length=1000),
                FieldSchema("file_path", DataType.VARCHAR, max_length=500),
                FieldSchema("embedding", DataType.FLOAT_VECTOR, dim=dimension)
            ]
            
            schema = CollectionSchema(fields, "代码实体向量存储")
            self._collection = Collection(collection_name, schema)
            
            # 创建索引
            index_params = {
                "metric_type": "L2",
                "index_type": "IVF_FLAT",
                "params": {"nlist": 1024}
            }
            
            self._collection.create_index("embedding", index_params)
            self.logger.info(f"Milvus集合 {collection_name} 创建完成")
            return True
            
        except Exception as e:
            self.logger.error(f"创建Milvus集合失败: {e}")
            return False
    
    async def insert_embeddings(self, embeddings_data: List[Dict[str, Any]]) -> List[int]:
        """插入向量数据"""
        if not self._collection or not await self.is_connected():
            await self.connect()
        
        try:
            # 准备数据
            entity_ids = []
            entity_types = []
            contents = []
            file_paths = []
            embeddings = []
            
            for data in embeddings_data:
                entity_ids.append(data.get("entity_id", 0))
                entity_types.append(data.get("entity_type", "unknown"))
                contents.append(data.get("content", ""))
                file_paths.append(data.get("file_path", ""))
                embeddings.append(data.get("embedding", []))
            
            # 插入数据
            insert_result = self._collection.insert([
                entity_ids,
                entity_types,
                contents,
                file_paths,
                embeddings
            ])
            
            # 加载集合到内存
            self._collection.load()
            
            self.logger.info(f"插入 {len(embeddings_data)} 个向量成功")
            return insert_result.primary_keys
            
        except Exception as e:
            self.logger.error(f"插入向量数据失败: {e}")
            return []
    
    async def search_similar(self, query_embedding: List[float], 
                           top_k: int = 10, 
                           filters: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """相似性搜索"""
        if not self._collection or not await self.is_connected():
            await self.connect()
        
        try:
            # 搜索参数
            search_params = {
                "metric_type": "L2",
                "params": {"nprobe": 10}
            }
            
            # 构建搜索表达式
            expr = ""
            if filters:
                filter_parts = []
                if "entity_type" in filters:
                    filter_parts.append(f"entity_type == '{filters['entity_type']}'")
                if "file_path" in filters:
                    filter_parts.append(f"file_path == '{filters['file_path']}'")
                
                if filter_parts:
                    expr = " and ".join(filter_parts)
            
            # 执行搜索
            search_result = self._collection.search(
                data=[query_embedding],
                anns_field="embedding",
                param=search_params,
                limit=top_k,
                expr=expr,
                output_fields=["entity_id", "entity_type", "content", "file_path"]
            )
            
            # 格式化结果
            results = []
            for hits in search_result:
                for hit in hits:
                    results.append({
                        "entity_id": hit.entity.get("entity_id"),
                        "entity_type": hit.entity.get("entity_type"),
                        "content": hit.entity.get("content"),
                        "file_path": hit.entity.get("file_path"),
                        "similarity": 1 - hit.distance,  # 转换为相似度分数
                        "distance": hit.distance
                    })
            
            return results
            
        except Exception as e:
            self.logger.error(f"相似性搜索失败: {e}")
            return []
    
    async def search_by_semantic_similarity(self, query_text: str, 
                                          top_k: int = 10,
                                          embedding_model: Any = None) -> List[Dict[str, Any]]:
        """基于语义相似性搜索"""
        if not embedding_model:
            self.logger.warning("未提供嵌入模型，无法进行语义搜索")
            return []
        
        # 生成查询向量
        query_embedding = await self._generate_embedding(query_text, embedding_model)
        
        if not query_embedding:
            return []
        
        # 执行向量搜索
        return await self.search_similar(query_embedding, top_k)
    
    async def find_similar_functions(self, function_content: str, 
                                   top_k: int = 5,
                                   embedding_model: Any = None) -> List[Dict[str, Any]]:
        """查找相似函数"""
        filters = {"entity_type": "function"}
        
        if embedding_model:
            query_embedding = await self._generate_embedding(function_content, embedding_model)
            return await self.search_similar(query_embedding, top_k, filters)
        else:
            # 使用文本相似性搜索（简化实现）
            return await self._text_similarity_search(function_content, "function", top_k)
    
    async def find_similar_classes(self, class_content: str, 
                                 top_k: int = 5,
                                 embedding_model: Any = None) -> List[Dict[str, Any]]:
        """查找相似类"""
        filters = {"entity_type": "class"}
        
        if embedding_model:
            query_embedding = await self._generate_embedding(class_content, embedding_model)
            return await self.search_similar(query_embedding, top_k, filters)
        else:
            return await self._text_similarity_search(class_content, "class", top_k)
    
    async def _generate_embedding(self, text: str, model: Any) -> Optional[List[float]]:
        """生成文本嵌入向量"""
        try:
            # 简化实现，实际应该调用嵌入模型
            # 这里返回一个随机向量作为示例
            import random
            return [random.random() for _ in range(768)]
        except Exception as e:
            self.logger.error(f"生成嵌入向量失败: {e}")
            return None
    
    async def _text_similarity_search(self, query_text: str, entity_type: str, top_k: int) -> List[Dict[str, Any]]:
        """文本相似性搜索（简化实现）"""
        # 简化实现，实际应该使用向量搜索
        # 这里返回空结果
        return []
    
    async def get_collection_stats(self) -> Dict[str, Any]:
        """获取集合统计信息"""
        if not self._collection:
            return {}
        
        try:
            from pymilvus import utility
            
            stats = utility.collection_stats(self._collection.name)
            return {
                "collection_name": self._collection.name,
                "row_count": stats.get("row_count", 0),
                "partitions": stats.get("partitions", [])
            }
        except Exception as e:
            self.logger.error(f"获取集合统计信息失败: {e}")
            return {}
    
    async def flush_collection(self):
        """刷新集合（将内存中的数据持久化）"""
        if self._collection:
            self._collection.flush()
            self.logger.info("集合数据已刷新")
    
    async def optimize_collection(self):
        """优化集合性能"""
        if self._collection:
            # 加载集合到内存
            self._collection.load()
            self.logger.info("集合已优化")
    
    async def export_embeddings(self, output_file: str) -> bool:
        """导出嵌入向量"""
        try:
            # 查询所有数据
            query_result = self._collection.query(
                expr="",
                output_fields=["entity_id", "entity_type", "content", "file_path", "embedding"]
            )
            
            # 保存到文件
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(query_result, f, ensure_ascii=False, indent=2)
            
            self.logger.info(f"嵌入向量已导出到 {output_file}")
            return True
            
        except Exception as e:
            self.logger.error(f"导出嵌入向量失败: {e}")
            return False