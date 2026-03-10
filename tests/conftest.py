"""
测试配置和夹具
"""
import pytest
import tempfile
import os
import sys
from pathlib import Path
from unittest.mock import MagicMock, patch
import yaml

# 添加项目根目录到路径
PROJECT_ROOT = Path(__file__).parent.parent
sys.path.insert(0, str(PROJECT_ROOT))


@pytest.fixture
def test_config():
    """创建测试配置"""
    return {
        'ai': {
            'provider': 'volcengine',
            'volcengine': {
                'api_key': 'test-api-key-12345',
                'base_url': 'https://api.volcengine.com',
                'model': 'test-model',
                'tools': {
                    'enabled': False,
                    'web_search': {
                        'enabled': False
                    }
                }
            }
        },
        'analysis': {
            'include_code': True,
            'include_docs': True,
            'max_file_size': 1048576
        },
        'testing': {
            'enabled': True,
            'test_framework': 'pytest',
            'timeout_seconds': 30
        }
    }


@pytest.fixture
def temp_config_file(test_config):
    """创建临时配置文件"""
    with tempfile.NamedTemporaryFile(mode='w', suffix='.yaml', delete=False) as f:
        yaml.dump(test_config, f)
        config_path = f.name
    yield config_path
    os.unlink(config_path)


@pytest.fixture
def mock_config(temp_config_file):
    """Mock 配置路径"""
    with patch('src.web.app.CONFIG_PATH', Path(temp_config_file)):
        yield temp_config_file


@pytest.fixture
def sample_entity():
    """示例代码实体"""
    from src.gitnexus.models import CodeEntity, EntityType
    
    return CodeEntity(
        id="test-entity-1",
        entity_type=EntityType.FUNCTION,
        name="test_function",
        file_path="test/test_file.py",
        start_line=1,
        end_line=10,
        content="def test_function():\n    pass",
        docstring="测试函数",
        parameters=[{"name": "arg1", "type": "str"}],
        return_type="None",
        lines_of_code=10,
        complexity=1
    )


@pytest.fixture
def sample_class_entity():
    """示例类实体"""
    from src.gitnexus.models import CodeEntity, EntityType
    
    return CodeEntity(
        id="test-class-1",
        entity_type=EntityType.CLASS,
        name="TestClass",
        file_path="test/test_file.py",
        start_line=1,
        end_line=50,
        content="class TestClass:\n    pass",
        docstring="测试类",
        lines_of_code=50,
        complexity=5,
        children_ids=["test-method-1", "test-method-2"]
    )


@pytest.fixture
def sample_relationship():
    """示例关系"""
    from src.gitnexus.models import Relationship, RelationshipType
    
    return Relationship(
        id="test-rel-1",
        source_id="test-entity-1",
        target_id="test-entity-2",
        relationship_type=RelationshipType.CALLS,
        confidence=0.9
    )


@pytest.fixture
def sample_knowledge_graph(sample_entity, sample_class_entity, sample_relationship):
    """示例知识图谱"""
    from src.gitnexus.models import KnowledgeGraph, CodeEntity, EntityType
    
    # 添加更多实体
    entity2 = CodeEntity(
        id="test-entity-2",
        entity_type=EntityType.FUNCTION,
        name="called_function",
        file_path="test/test_file.py",
        start_line=11,
        end_line=20,
        content="def called_function():\n    pass",
        lines_of_code=10
    )
    
    method1 = CodeEntity(
        id="test-method-1",
        entity_type=EntityType.METHOD,
        name="test_method",
        file_path="test/test_file.py",
        start_line=5,
        end_line=15,
        content="def test_method(self):\n    pass",
        parent_id="test-class-1",
        lines_of_code=10
    )
    
    graph = KnowledgeGraph(
        entities={
            sample_entity.id: sample_entity,
            entity2.id: entity2,
            sample_class_entity.id: sample_class_entity,
            method1.id: method1
        },
        relationships=[sample_relationship],
        project_name="test-project",
        metadata={"test": True}
    )
    
    return graph


@pytest.fixture
def mock_gitnexus_client(sample_knowledge_graph):
    """Mock GitNexusClient"""
    mock_client = MagicMock()
    mock_client._load_knowledge_graph.return_value = sample_knowledge_graph
    mock_client.get_statistics.return_value = {
        "total_entities": 4,
        "total_relationships": 1,
        "total_files": 1
    }
    mock_client.export.return_value = "/tmp/output.md"
    return mock_client


@pytest.fixture
def mock_knowledge_extractor():
    """Mock KnowledgeExtractor"""
    mock_extractor = MagicMock()
    return mock_extractor


@pytest.fixture
def mock_llm_client():
    """Mock LLMClient"""
    mock_client = MagicMock()
    mock_client.is_available.return_value = True
    mock_client.chat_sync.return_value = "连接成功"
    mock_client.get_config_info.return_value = {
        "provider": "volcengine",
        "model": "test-model"
    }
    return mock_client


@pytest.fixture
def mock_architecture_analyzer(sample_knowledge_graph):
    """Mock ArchitectureAnalyzer"""
    from src.gitnexus.models import ArchitectureInfo, ModuleInfo, FeatureInfo, CallChain
    
    mock_analyzer = MagicMock()
    
    # 创建模拟架构信息
    architecture = ArchitectureInfo(
        modules={
            "test/module": ModuleInfo(
                name="test_module",
                path="test/module",
                file_count=1,
                entity_count=4
            )
        },
        features={
            "test-feature": FeatureInfo(
                id="test-feature",
                name="Test Feature",
                entities=["test-entity-1"]
            )
        },
        call_chains=[
            CallChain(
                id="chain-1",
                name="test_chain",
                entry_point="test-entity-1",
                nodes=["test-entity-1", "test-entity-2"],
                depth=2,
                total_calls=1
            )
        ],
        statistics={"total_modules": 1}
    )
    
    mock_analyzer.analyze_full.return_value = architecture
    mock_analyzer.analyze_modules.return_value = architecture.modules
    mock_analyzer.analyze_features.return_value = architecture.features
    mock_analyzer.analyze_call_chains.return_value = architecture.call_chains
    mock_analyzer.analyze_exception_flows.return_value = {}
    mock_analyzer.generate_statistics.return_value = {
        "most_called_entities": []
    }
    
    return mock_analyzer


@pytest.fixture
def mock_uml_generator(sample_knowledge_graph):
    """Mock UMLGenerator"""
    mock_generator = MagicMock()
    mock_generator.generate_class_diagram.return_value = "classDiagram\n    class TestClass"
    mock_generator.generate_sequence_diagram.return_value = "sequenceDiagram\n    A->>B: call"
    mock_generator.generate_flowchart.return_value = "flowchart TD\n    A --> B"
    mock_generator.generate_component_diagram.return_value = "graph TB\n    A --> B"
    return mock_generator


@pytest.fixture
def mock_knowledge_document_generator():
    """Mock KnowledgeDocumentGenerator"""
    mock_generator = MagicMock()
    mock_generator.generate_all_documents.return_value = {
        "index": "/tmp/doc/index.md",
        "entities": ["/tmp/doc/entities/test.md"],
        "modules": ["/tmp/doc/modules/test.md"],
        "architecture": ["/tmp/doc/architecture/test.md"],
        "uml": ["/tmp/doc/uml/test.md"]
    }
    mock_generator.get_document_list.return_value = {
        "index": "/tmp/doc/index.md",
        "entities": [],
        "modules": [],
        "architecture": [],
        "uml": [],
        "statistics": {"total_files": 1}
    }
    mock_generator.get_document_content.return_value = "# Test Document\n\nContent here."
    return mock_generator


@pytest.fixture
def client():
    """创建测试客户端"""
    from fastapi.testclient import TestClient
    from src.web.app import app
    
    return TestClient(app)


@pytest.fixture
def async_client():
    """创建异步测试客户端"""
    from httpx import AsyncClient, ASGITransport
    from src.web.app import app
    
    return AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test"
    )
