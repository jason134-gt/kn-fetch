"""
Neo4j图数据库客户端 - 专门用于依赖关系图存储

功能：
- 图数据库连接管理
- 依赖关系图构建
- 图查询和遍历
- 路径分析
"""

import os
import json
from typing import Dict, Any, List, Optional, Tuple
from abc import ABC, abstractmethod
import logging


class Neo4jClient:
    """Neo4j图数据库客户端"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("neo4j.client")
        self._driver = None
        self._connected = False
    
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
    
    async def execute_query(self, query: str, params: Optional[Dict] = None) -> List[Dict[str, Any]]:
        """执行Cypher查询"""
        if not await self.is_connected():
            await self.connect()
        
        try:
            async with self._driver.session() as session:
                result = await session.run(query, params or {})
                records = await result.data()
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
            "CREATE CONSTRAINT IF NOT EXISTS FOR (e:Entity) REQUIRE e.id IS UNIQUE",
            "CREATE CONSTRAINT IF NOT EXISTS FOR (d:Dependency) REQUIRE d.id IS UNIQUE"
        ]
        
        for constraint in constraints:
            await self.execute_command(constraint)
        
        self.logger.info("Neo4j图模式创建完成")
    
    async def create_project_node(self, project_data: Dict[str, Any]) -> str:
        """创建项目节点"""
        query = """
        MERGE (p:Project {id: $id})
        SET p.name = $name,
            p.path = $path,
            p.description = $description,
            p.created_at = datetime(),
            p.updated_at = datetime()
        RETURN p.id
        """
        
        result = await self.execute_query(query, project_data)
        return result[0]["p.id"] if result else None
    
    async def create_file_node(self, file_data: Dict[str, Any]) -> str:
        """创建文件节点"""
        query = """
        MERGE (f:File {id: $id})
        SET f.path = $path,
            f.name = $name,
            f.extension = $extension,
            f.size = $size,
            f.hash = $hash,
            f.encoding = $encoding,
            f.created_at = datetime()
        RETURN f.id
        """
        
        result = await self.execute_query(query, file_data)
        return result[0]["f.id"] if result else None
    
    async def create_entity_node(self, entity_data: Dict[str, Any]) -> str:
        """创建代码实体节点"""
        query = """
        MERGE (e:Entity {id: $id})
        SET e.name = $name,
            e.type = $type,
            e.start_line = $start_line,
            e.end_line = $end_line,
            e.file_id = $file_id,
            e.metadata = $metadata
        RETURN e.id
        """
        
        result = await self.execute_query(query, entity_data)
        return result[0]["e.id"] if result else None
    
    async def create_dependency_relationship(self, source_id: str, target_id: str, 
                                           dep_type: str, strength: float = 0.5) -> bool:
        """创建依赖关系"""
        query = """
        MATCH (source:Entity {id: $source_id}), (target:Entity {id: $target_id})
        MERGE (source)-[r:DEPENDS_ON {type: $dep_type}]->(target)
        SET r.strength = $strength,
            r.created_at = datetime()
        RETURN r
        """
        
        params = {
            "source_id": source_id,
            "target_id": target_id,
            "dep_type": dep_type,
            "strength": strength
        }
        
        result = await self.execute_query(query, params)
        return len(result) > 0
    
    async def create_belongs_to_relationship(self, entity_id: str, file_id: str) -> bool:
        """创建属于关系"""
        query = """
        MATCH (e:Entity {id: $entity_id}), (f:File {id: $file_id})
        MERGE (e)-[r:BELONGS_TO]->(f)
        SET r.created_at = datetime()
        RETURN r
        """
        
        params = {
            "entity_id": entity_id,
            "file_id": file_id
        }
        
        result = await self.execute_query(query, params)
        return len(result) > 0
    
    async def find_dependencies(self, entity_id: str, depth: int = 3) -> List[Dict[str, Any]]:
        """查找依赖关系"""
        query = """
        MATCH (source:Entity {id: $entity_id})-[r:DEPENDS_ON*1..$depth]->(target:Entity)
        RETURN source, r, target
        """
        
        params = {
            "entity_id": entity_id,
            "depth": depth
        }
        
        return await self.execute_query(query, params)
    
    async def find_dependents(self, entity_id: str, depth: int = 3) -> List[Dict[str, Any]]:
        """查找依赖者"""
        query = """
        MATCH (target:Entity {id: $entity_id})<-[r:DEPENDS_ON*1..$depth]-(source:Entity)
        RETURN source, r, target
        """
        
        params = {
            "entity_id": entity_id,
            "depth": depth
        }
        
        return await self.execute_query(query, params)
    
    async def find_shortest_path(self, source_id: str, target_id: str) -> List[Dict[str, Any]]:
        """查找最短路径"""
        query = """
        MATCH path = shortestPath((source:Entity {id: $source_id})-[*]-(target:Entity {id: $target_id}))
        RETURN path
        """
        
        params = {
            "source_id": source_id,
            "target_id": target_id
        }
        
        return await self.execute_query(query, params)
    
    async def analyze_architecture(self, project_id: str) -> Dict[str, Any]:
        """分析架构"""
        # 分析模块依赖
        module_deps_query = """
        MATCH (f1:File)-[:BELONGS_TO]->(p:Project {id: $project_id})
        MATCH (f2:File)-[:BELONGS_TO]->(p)
        MATCH (e1:Entity)-[:BELONGS_TO]->(f1)
        MATCH (e2:Entity)-[:BELONGS_TO]->(f2)
        MATCH (e1)-[r:DEPENDS_ON]->(e2)
        RETURN f1.name as source_file, f2.name as target_file, COUNT(r) as dep_count
        ORDER BY dep_count DESC
        """
        
        module_deps = await self.execute_query(module_deps_query, {"project_id": project_id})
        
        # 分析循环依赖
        cycle_query = """
        MATCH (e:Entity)-[:BELONGS_TO]->(f:File)-[:BELONGS_TO]->(p:Project {id: $project_id})
        MATCH path = (e)-[r:DEPENDS_ON*2..10]->(e)
        RETURN DISTINCT e.name as entity_name, LENGTH(path) as cycle_length
        """
        
        cycles = await self.execute_query(cycle_query, {"project_id": project_id})
        
        # 分析关键路径
        critical_path_query = """
        MATCH (e:Entity)-[:BELONGS_TO]->(f:File)-[:BELONGS_TO]->(p:Project {id: $project_id})
        WITH e, SIZE([(e)--() | 1]) as degree
        RETURN e.name as entity_name, degree
        ORDER BY degree DESC
        LIMIT 10
        """
        
        critical_paths = await self.execute_query(critical_path_query, {"project_id": project_id})
        
        return {
            "module_dependencies": module_deps,
            "cyclic_dependencies": cycles,
            "critical_paths": critical_paths
        }
    
    async def export_graph_data(self, project_id: str) -> Dict[str, Any]:
        """导出图数据"""
        # 导出节点
        nodes_query = """
        MATCH (n)-[:BELONGS_TO]->(f:File)-[:BELONGS_TO]->(p:Project {id: $project_id})
        RETURN n
        """
        
        # 导出关系
        relationships_query = """
        MATCH (n1)-[r]->(n2)
        WHERE (n1)-[:BELONGS_TO]->(:File)-[:BELONGS_TO]->(:Project {id: $project_id})
        AND (n2)-[:BELONGS_TO]->(:File)-[:BELONGS_TO]->(:Project {id: $project_id})
        RETURN n1.id as source, n2.id as target, TYPE(r) as type, r
        """
        
        nodes = await self.execute_query(nodes_query, {"project_id": project_id})
        relationships = await self.execute_query(relationships_query, {"project_id": project_id})
        
        return {
            "nodes": nodes,
            "relationships": relationships
        }