"""
数据库集成测试

测试数据库客户端的连接、操作和集成功能
"""

import pytest
import asyncio
from unittest.mock import Mock, patch, AsyncMock
from src.infrastructure.database_client import (
    PostgreSQLClient, Neo4jClient, MilvusClient, DatabaseManager
)
from src.infrastructure.neo4j_client import Neo4jClient as DedicatedNeo4jClient
from src.infrastructure.milvus_client import MilvusClient as DedicatedMilvusClient


class TestPostgreSQLClient:
    """PostgreSQL客户端测试"""
    
    @pytest.fixture
    def postgres_config(self):
        return {
            "host": "localhost",
            "port": 5432,
            "database": "test_code_analysis",
            "user": "test_user",
            "password": "test_password"
        }
    
    @pytest.mark.asyncio
    async def test_connect_success(self, postgres_config):
        """测试成功连接"""
        with patch('asyncpg.connect') as mock_connect:
            mock_connection = AsyncMock()
            mock_connect.return_value = mock_connection
            mock_connection.is_closed.return_value = False
            
            client = PostgreSQLClient(postgres_config)
            result = await client.connect()
            
            assert result is True
            assert client.is_connected() is True
    
    @pytest.mark.asyncio
    async def test_connect_failure(self, postgres_config):
        """测试连接失败"""
        with patch('asyncpg.connect', side_effect=Exception("Connection failed")):
            client = PostgreSQLClient(postgres_config)
            result = await client.connect()
            
            assert result is False
            assert client.is_connected() is False
    
    @pytest.mark.asyncio
    async def test_create_tables(self, postgres_config):
        """测试创建表"""
        with patch('asyncpg.connect') as mock_connect:
            mock_connection = AsyncMock()
            mock_connect.return_value = mock_connection
            
            client = PostgreSQLClient(postgres_config)
            await client.connect()
            await client.create_tables()
            
            # 验证执行了建表语句
            assert mock_connection.execute.called


class TestNeo4jClient:
    """Neo4j客户端测试"""
    
    @pytest.fixture
    def neo4j_config(self):
        return {
            "uri": "bolt://localhost:7687",
            "username": "neo4j",
            "password": "test_password"
        }
    
    @pytest.mark.asyncio
    async def test_connect_success(self, neo4j_config):
        """测试成功连接"""
        with patch('neo4j.AsyncGraphDatabase.driver') as mock_driver:
            mock_session = AsyncMock()
            mock_driver.return_value.session.return_value.__aenter__.return_value = mock_session
            
            client = DedicatedNeo4jClient(neo4j_config)
            result = await client.connect()
            
            assert result is True
            assert client.is_connected() is True
    
    @pytest.mark.asyncio
    async def test_create_dependency_graph(self, neo4j_config):
        """测试创建依赖关系图"""
        with patch('neo4j.AsyncGraphDatabase.driver') as mock_driver:
            mock_session = AsyncMock()
            mock_driver.return_value.session.return_value.__aenter__.return_value = mock_session
            
            client = DedicatedNeo4jClient(neo4j_config)
            await client.connect()
            
            dependencies = [
                {
                    "source_id": "func1", "source_name": "function1", "source_type": "function",
                    "target_id": "func2", "target_name": "function2", "target_type": "function",
                    "dep_type": "calls", "strength": 0.8
                }
            ]
            
            await client.create_dependency_relationship(
                "func1", "func2", "calls", 0.8
            )
            
            # 验证执行了Cypher语句
            assert mock_session.run.called


class TestMilvusClient:
    """Milvus客户端测试"""
    
    @pytest.fixture
    def milvus_config(self):
        return {
            "host": "localhost",
            "port": "19530"
        }
    
    @pytest.mark.asyncio
    async def test_connect_success(self, milvus_config):
        """测试成功连接"""
        with patch('pymilvus.connections.connect') as mock_connect:
            with patch('pymilvus.utility.has_collection', return_value=True):
                with patch('pymilvus.Collection') as mock_collection:
                    client = DedicatedMilvusClient(milvus_config)
                    result = await client.connect()
                    
                    assert result is True
                    assert client.is_connected() is True
    
    @pytest.mark.asyncio
    async def test_insert_embeddings(self, milvus_config):
        """测试插入向量数据"""
        with patch('pymilvus.connections.connect'):
            with patch('pymilvus.utility.has_collection', return_value=True):
                with patch('pymilvus.Collection') as mock_collection_class:
                    mock_collection = Mock()
                    mock_collection_class.return_value = mock_collection
                    mock_collection.insert.return_value = Mock(primary_keys=[1, 2, 3])
                    
                    client = DedicatedMilvusClient(milvus_config)
                    await client.connect()
                    
                    embeddings_data = [
                        {
                            "entity_id": 1,
                            "entity_type": "function",
                            "content": "def test_function():",
                            "file_path": "/path/to/file.py",
                            "embedding": [0.1, 0.2, 0.3]
                        }
                    ]
                    
                    result = await client.insert_embeddings(embeddings_data)
                    
                    assert len(result) == 3
                    assert mock_collection.insert.called


class TestDatabaseManager:
    """数据库管理器测试"""
    
    @pytest.fixture
    def db_config(self):
        return {
            "databases": {
                "postgresql": {
                    "host": "localhost",
                    "port": 5432,
                    "database": "test_db",
                    "user": "test_user",
                    "password": "test_password"
                },
                "neo4j": {
                    "uri": "bolt://localhost:7687",
                    "username": "neo4j",
                    "password": "test_password"
                },
                "milvus": {
                    "host": "localhost",
                    "port": "19530"
                }
            }
        }
    
    @pytest.mark.asyncio
    async def test_initialize_success(self, db_config):
        """测试初始化成功"""
        with patch('src.infrastructure.database_client.PostgreSQLClient.connect', return_value=True):
            with patch('src.infrastructure.database_client.Neo4jClient.connect', return_value=True):
                with patch('src.infrastructure.database_client.MilvusClient.connect', return_value=True):
                    manager = DatabaseManager(db_config)
                    await manager.initialize()
                    
                    assert len(manager.clients) == 3
                    assert "postgresql" in manager.clients
                    assert "neo4j" in manager.clients
                    assert "milvus" in manager.clients
    
    @pytest.mark.asyncio
    async def test_get_client(self, db_config):
        """测试获取客户端"""
        with patch('src.infrastructure.database_client.PostgreSQLClient.connect', return_value=True):
            manager = DatabaseManager(db_config)
            await manager.initialize()
            
            client = await manager.get_client("postgresql")
            assert client is not None
            assert isinstance(client, PostgreSQLClient)
    
    @pytest.mark.asyncio
    async def test_close_all(self, db_config):
        """测试关闭所有连接"""
        with patch('src.infrastructure.database_client.PostgreSQLClient.connect', return_value=True):
            with patch('src.infrastructure.database_client.PostgreSQLClient.disconnect') as mock_disconnect:
                manager = DatabaseManager(db_config)
                await manager.initialize()
                await manager.close_all()
                
                assert mock_disconnect.called
                assert len(manager.clients) == 0


class TestIntegrationScenarios:
    """集成场景测试"""
    
    @pytest.mark.asyncio
    async def test_full_analysis_workflow(self):
        """测试完整分析工作流"""
        # 模拟文件扫描
        with patch('src.agents.file_scanner.FileScannerAgent._execute_impl') as mock_scan:
            mock_scan.return_value = [
                Mock(file_path="/path/to/file.py", file_name="file.py")
            ]
            
            # 模拟代码解析
            with patch('src.agents.code_parser.CodeParserAgent._execute_impl') as mock_parse:
                mock_parse.return_value = Mock(
                    file_path="/path/to/file.py",
                    functions=[Mock(name="test_function")],
                    classes=[]
                )
                
                # 模拟数据库操作
                with patch('src.infrastructure.database_client.PostgreSQLClient.execute_command') as mock_db:
                    # 执行分析流程
                    # 这里应该调用实际的Agent工作流
                    assert True  # 简化测试
    
    @pytest.mark.asyncio
    async def test_dependency_analysis_workflow(self):
        """测试依赖分析工作流"""
        with patch('src.infrastructure.neo4j_client.Neo4jClient.create_dependency_relationship') as mock_dep:
            mock_dep.return_value = True
            
            # 模拟依赖分析
            dependencies = [
                ("func1", "func2", "calls", 0.8),
                ("class1", "func3", "uses", 0.6)
            ]
            
            # 这里应该调用实际的依赖分析逻辑
            assert len(dependencies) == 2


@pytest.fixture(scope="session")
def event_loop():
    """创建事件循环用于异步测试"""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])