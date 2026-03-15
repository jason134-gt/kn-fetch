"""
数据库客户端 - 基于design-v1方案实现多数据库支持

支持：
- PostgreSQL: 结构化数据存储
- Neo4j: 图数据库，用于依赖关系存储
- Milvus: 向量数据库，用于语义相似性搜索
"""

import os
import json
from typing import Dict, Any, List, Optional
from abc import ABC, abstractmethod
from enum import Enum
import logging


class DatabaseType(Enum):
    """数据库类型枚举"""
    POSTGRESQL = "postgresql"
    NEO4J = "neo4j"
    MILVUS = "milvus"


class DatabaseClient(ABC):
    """数据库客户端抽象基类"""
    
    def __init__(self, db_type: DatabaseType, config: Dict[str, Any]):
        self.db_type = db_type
        self.config = config
        self.logger = logging.getLogger(f"db.{db_type.value}")
        self._connected = False
    
    @abstractmethod
    async def connect(self) -> bool:
        """连接数据库"""
        pass
    
    @abstractmethod
    async def disconnect(self):
        """断开数据库连接"""
        pass
    
    @abstractmethod
    async def is_connected(self) -> bool:
        """检查连接状态"""
        pass
    
    @abstractmethod
    async def execute_query(self, query: str, params: Optional[Dict] = None) -> Any:
        """执行查询"""
        pass
    
    @abstractmethod
    async def execute_command(self, command: str, params: Optional[Dict] = None) -> bool:
        """执行命令"""
        pass


class PostgreSQLClient(DatabaseClient):
    """PostgreSQL客户端"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__(DatabaseType.POSTGRESQL, config)
        self._connection = None
    
    async def connect(self) -> bool:
        """连接PostgreSQL数据库"""
        try:
            import asyncpg
            
            connection_config = {
                "host": self.config.get("host", "localhost"),
                "port": self.config.get("port", 5432),
                "database": self.config.get("database", "code_analysis"),
                "user": self.config.get("user", "postgres"),
                "password": self.config.get("password", "")
            }
            
            self._connection = await asyncpg.connect(**connection_config)
            self._connected = True
            self.logger.info("PostgreSQL连接成功")
            return True
            
        except Exception as e:
            self.logger.error(f"PostgreSQL连接失败: {e}")
            self._connected = False
            return False
    
    async def disconnect(self):
        """断开PostgreSQL连接"""
        if self._connection:
            await self._connection.close()
            self._connected = False
            self.logger.info("PostgreSQL连接已断开")
    
    async def is_connected(self) -> bool:
        """检查连接状态"""
        return self._connected and not self._connection.is_closed()
    
    async def execute_query(self, query: str, params: Optional[Dict] = None) -> Any:
        """执行PostgreSQL查询"""
        if not await self.is_connected():
            await self.connect()
        
        try:
            result = await self._connection.fetch(query, *(params or {}).values())
            return result
        except Exception as e:
            self.logger.error(f"PostgreSQL查询失败: {e}")
            raise
    
    async def execute_command(self, command: str, params: Optional[Dict] = None) -> bool:
        """执行PostgreSQL命令"""
        if not await self.is_connected():
            await self.connect()
        
        try:
            await self._connection.execute(command, *(params or {}).values())
            return True
        except Exception as e:
            self.logger.error(f"PostgreSQL命令执行失败: {e}")
            return False
    
    async def create_tables(self):
        """创建代码分析相关表"""
        tables_sql = """
        -- 项目表
        CREATE TABLE IF NOT EXISTS projects (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            path TEXT NOT NULL,
            description TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        
        -- 文件表
        CREATE TABLE IF NOT EXISTS files (
            id SERIAL PRIMARY KEY,
            project_id INTEGER REFERENCES projects(id),
            file_path TEXT NOT NULL,
            file_name VARCHAR(255) NOT NULL,
            file_extension VARCHAR(10),
            file_size BIGINT,
            content_hash VARCHAR(32),
            encoding VARCHAR(20),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        
        -- 代码实体表
        CREATE TABLE IF NOT EXISTS code_entities (
            id SERIAL PRIMARY KEY,
            file_id INTEGER REFERENCES files(id),
            entity_type VARCHAR(50) NOT NULL, -- function, class, variable, etc.
            entity_name VARCHAR(255) NOT NULL,
            start_line INTEGER,
            end_line INTEGER,
            parameters JSONB,
            return_type VARCHAR(100),
            docstring TEXT,
            metadata JSONB
        );
        
        -- 依赖关系表
        CREATE TABLE IF NOT EXISTS dependencies (
            id SERIAL PRIMARY KEY,
            source_entity_id INTEGER REFERENCES code_entities(id),
            target_entity_id INTEGER REFERENCES code_entities(id),
            dependency_type VARCHAR(50) NOT NULL,
            strength FLOAT DEFAULT 0.5
        );
        
        -- 创建索引
        CREATE INDEX IF NOT EXISTS idx_files_project_id ON files(project_id);
        CREATE INDEX IF NOT EXISTS idx_code_entities_file_id ON code_entities(file_id);
        CREATE INDEX IF NOT EXISTS idx_dependencies_source ON dependencies(source_entity_id);
        CREATE INDEX IF NOT EXISTS idx_dependencies_target ON dependencies(target_entity_id);
        """
        
        # 执行建表语句
        for sql in tables_sql.split(';'):
            sql = sql.strip()
            if sql:
                await self.execute_command(sql)
        
        self.logger.info("PostgreSQL表结构创建完成")


class Neo4jClient(DatabaseClient):
    """Neo4j图数据库客户端"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__(DatabaseType.NEO4J, config)
        self._driver = None
    
    async def connect(self) -> bool:
        """连接Neo4j数据库"""
        try:
            from neo4j import AsyncGraphDatabase
            
            uri = self.config.get("uri", "bolt://localhost:7687")
            username = self.config.get("username", "neo4j")
            password = self.config.get("password", "")
            
            self._driver = AsyncGraphDatabase.driver(uri, auth=(username, password))
            
            # 测试连接
            async with self._driver.session() as session:
                await session.run("RETURN 1")
            
            self._connected = True
            self.logger.info("Neo4j连接成功")
            return True
            
        except Exception as e:
            self.logger.error(f"Neo4j连接失败: {e}")
            self._connected = False
            return False
    
    async def disconnect(self):
        """断开Neo4j连接"""
        if self._driver:
            await self._driver.close()
            self._connected = False
            self.logger.info("Neo4j连接已断开")
    
    async def is_connected(self) -> bool:
        """检查连接状态"""
        return self._connected and self._driver is not None
    
    async def execute_query(self, query: str, params: Optional[Dict] = None) -> Any:
        """执行Cypher查询"""
        if not await self.is_connected():
            await self.connect()
        
        try:
            async with self._driver.session() as session:
                result = await session.run(query, params or {})
                records = await result.values()
                return records
        except Exception as e:
            self.logger.error(f"Neo4j查询失败: {e}")
            raise
    
    async def execute_command(self, command: str, params: Optional[Dict] = None) -> bool:
        """执行Cypher命令"""
        if not await self.is_connected():
            await self.connect()
        
        try:
            async with self._driver.session() as session:
                await session.run(command, params or {})
                return True
        except Exception as e:
            self.logger.error(f"Neo4j命令执行失败: {e}")
            return False
    
    async def create_graph_schema(self):
        """创建图数据库模式"""
        # 创建约束确保唯一性
        constraints = [
            "CREATE CONSTRAINT IF NOT EXISTS FOR (p:Project) REQUIRE p.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (f:File) REQUIRE f.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (e:Entity) REQUIRE e.id IS UNIQUE"
        ]
        
        for constraint in constraints:
            await self.execute_command(constraint)
        
        self.logger.info("Neo4j图模式创建完成")
    
    async def create_dependency_graph(self, dependencies: List[Dict[str, Any]]):
        """创建依赖关系图"""
        # 批量创建节点和关系
        batch_size = 100
        
        for i in range(0, len(dependencies), batch_size):
            batch = dependencies[i:i + batch_size]
            
            # 构建Cypher语句
            cypher_statements = []
            for dep in batch:
                cypher = f"""
                MERGE (source:Entity {{id: $source_id_{i}, name: $source_name_{i}, type: $source_type_{i}}})
                MERGE (target:Entity {{id: $target_id_{i}, name: $target_name_{i}, type: $target_type_{i}}})
                MERGE (source)-[r:DEPENDS_ON {{type: $dep_type_{i}, strength: $strength_{i}}}]->(target)
                """
                cypher_statements.append(cypher)
            
            # 执行批量操作
            await self.execute_command("\n".join(cypher_statements))
        
        self.logger.info(f"依赖关系图创建完成，共处理 {len(dependencies)} 个依赖关系")


class MilvusClient(DatabaseClient):
    """Milvus向量数据库客户端"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__(DatabaseType.MILVUS, config)
        self._client = None
        self._collection = None
    
    async def connect(self) -> bool:
        """连接Milvus数据库"""
        try:
            from pymilvus import connections, utility
            
            host = self.config.get("host", "localhost")
            port = self.config.get("port", "19530")
            
            connections.connect("default", host=host, port=port)
            
            # 检查连接
            if utility.has_collection("code_embeddings"):
                self._collection = "code_embeddings"
            
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
    
    async def execute_query(self, query: str, params: Optional[Dict] = None) -> Any:
        """执行向量查询"""
        if not await self.is_connected():
            await self.connect()
        
        try:
            from pymilvus import Collection
            
            collection = Collection("code_embeddings")
            collection.load()
            
            # 执行向量搜索
            search_params = {
                "metric_type": "L2",
                "offset": 0,
                "ignore_growing": False,
                "params": {"nprobe": 10}
            }
            
            # 这里需要根据实际查询需求实现
            # 简化实现，返回空结果
            return []
            
        except Exception as e:
            self.logger.error(f"Milvus查询失败: {e}")
            raise
    
    async def execute_command(self, command: str, params: Optional[Dict] = None) -> bool:
        """执行Milvus命令"""
        # Milvus主要通过API操作，这里简化处理
        return True
    
    async def create_collection(self):
        """创建向量集合"""
        try:
            from pymilvus import (
                connections, FieldSchema, CollectionSchema, DataType,
                Collection, utility
            )
            
            if utility.has_collection("code_embeddings"):
                self.logger.info("code_embeddings集合已存在")
                return
            
            # 定义字段
            fields = [
                FieldSchema("id", DataType.INT64, is_primary=True, auto_id=True),
                FieldSchema("entity_id", DataType.INT64),
                FieldSchema("embedding", DataType.FLOAT_VECTOR, dim=768),
                FieldSchema("entity_type", DataType.VARCHAR, max_length=50),
                FieldSchema("content", DataType.VARCHAR, max_length=1000)
            ]
            
            schema = CollectionSchema(fields, "代码实体向量存储")
            collection = Collection("code_embeddings", schema)
            
            # 创建索引
            index_params = {
                "metric_type": "L2",
                "index_type": "IVF_FLAT",
                "params": {"nlist": 1024}
            }
            
            collection.create_index("embedding", index_params)
            self.logger.info("Milvus集合创建完成")
            
        except Exception as e:
            self.logger.error(f"创建Milvus集合失败: {e}")


class DatabaseManager:
    """数据库管理器 - 统一管理多个数据库连接"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("db.manager")
        self.clients = {}
    
    async def initialize(self):
        """初始化所有数据库客户端"""
        db_configs = self.config.get("databases", {})
        
        # 初始化PostgreSQL
        if "postgresql" in db_configs:
            postgres_client = PostgreSQLClient(db_configs["postgresql"])
            if await postgres_client.connect():
                await postgres_client.create_tables()
                self.clients["postgresql"] = postgres_client
        
        # 初始化Neo4j
        if "neo4j" in db_configs:
            neo4j_client = Neo4jClient(db_configs["neo4j"])
            if await neo4j_client.connect():
                await neo4j_client.create_graph_schema()
                self.clients["neo4j"] = neo4j_client
        
        # 初始化Milvus
        if "milvus" in db_configs:
            milvus_client = MilvusClient(db_configs["milvus"])
            if await milvus_client.connect():
                await milvus_client.create_collection()
                self.clients["milvus"] = milvus_client
        
        self.logger.info(f"数据库管理器初始化完成，已连接 {len(self.clients)} 个数据库")
    
    async def get_client(self, db_type: str) -> Optional[DatabaseClient]:
        """获取指定类型的数据库客户端"""
        return self.clients.get(db_type)
    
    async def close_all(self):
        """关闭所有数据库连接"""
        for client in self.clients.values():
            await client.disconnect()
        
        self.clients.clear()
        self.logger.info("所有数据库连接已关闭")