"""
UML 相关 API 端点测试
"""
import pytest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient


class TestUMLClassDiagramAPI:
    """类图 API 测试"""
    
    def test_get_uml_class_diagram(self, client, mock_gitnexus_client, mock_uml_generator, sample_knowledge_graph):
        """测试获取类图"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.UMLGenerator', return_value=mock_uml_generator):
                response = client.get("/api/uml/class-diagram")
                assert response.status_code == 200
                data = response.json()
                assert "format" in data
                assert "diagram" in data
    
    def test_get_uml_class_diagram_with_format(self, client, mock_gitnexus_client, mock_uml_generator):
        """测试指定格式获取类图"""
        formats = ["mermaid", "plantuml"]
        
        for fmt in formats:
            with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
                with patch('src.web.app.UMLGenerator', return_value=mock_uml_generator):
                    response = client.get(f"/api/uml/class-diagram?format={fmt}")
                    assert response.status_code == 200
    
    def test_get_uml_class_diagram_with_max_entities(self, client, mock_gitnexus_client, mock_uml_generator):
        """测试限制实体数量获取类图"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.UMLGenerator', return_value=mock_uml_generator):
                response = client.get("/api/uml/class-diagram?max_entities=20")
                assert response.status_code == 200
    
    def test_get_uml_class_diagram_no_data(self, client):
        """测试无数据时获取类图"""
        mock_client = MagicMock()
        mock_graph = MagicMock()
        mock_graph.entities = {}
        mock_client._load_knowledge_graph.return_value = mock_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_client):
            response = client.get("/api/uml/class-diagram")
            assert response.status_code == 200
            assert "error" in response.json()


class TestUMLSequenceDiagramAPI:
    """时序图 API 测试"""
    
    def test_get_uml_sequence_diagram(self, client, mock_gitnexus_client, mock_uml_generator, mock_architecture_analyzer, sample_knowledge_graph):
        """测试获取时序图"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                with patch('src.web.app.UMLGenerator', return_value=mock_uml_generator):
                    response = client.get("/api/uml/sequence-diagram/0")
                    assert response.status_code in [200, 404]
    
    def test_get_uml_sequence_diagram_invalid_index(self, client, mock_gitnexus_client, mock_architecture_analyzer):
        """测试无效索引获取时序图"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                response = client.get("/api/uml/sequence-diagram/999")
                assert response.status_code == 404


class TestUMLFlowchartAPI:
    """流程图 API 测试"""
    
    def test_get_uml_flowchart(self, client, mock_gitnexus_client, mock_uml_generator, mock_architecture_analyzer, sample_knowledge_graph):
        """测试获取流程图"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                with patch('src.web.app.UMLGenerator', return_value=mock_uml_generator):
                    response = client.get("/api/uml/flowchart")
                    assert response.status_code == 200
                    data = response.json()
                    assert "format" in data
                    assert "diagram" in data
    
    def test_get_uml_flowchart_types(self, client, mock_gitnexus_client, mock_uml_generator, mock_architecture_analyzer):
        """测试不同类型流程图"""
        flow_types = ["call", "data", "exception"]
        
        for flow_type in flow_types:
            with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
                with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                    with patch('src.web.app.UMLGenerator', return_value=mock_uml_generator):
                        response = client.get(f"/api/uml/flowchart?flow_type={flow_type}")
                        assert response.status_code == 200


class TestUMLComponentDiagramAPI:
    """组件图 API 测试"""
    
    def test_get_uml_component_diagram(self, client, mock_gitnexus_client, mock_uml_generator, mock_architecture_analyzer, sample_knowledge_graph):
        """测试获取组件图"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                with patch('src.web.app.UMLGenerator', return_value=mock_uml_generator):
                    response = client.get("/api/uml/component-diagram")
                    assert response.status_code == 200
                    data = response.json()
                    assert "diagram" in data


class TestUMLDesignAPI:
    """UML 设计 API 测试"""
    
    def test_get_technology_stack(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取技术选型"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/uml-design/technology-stack")
            assert response.status_code == 200
            data = response.json()
            assert "languages" in data
            assert "frameworks" in data
    
    def test_get_technology_stack_no_data(self, client):
        """测试无数据时获取技术选型"""
        mock_client = MagicMock()
        mock_graph = MagicMock()
        mock_graph.entities = {}
        mock_client._load_knowledge_graph.return_value = mock_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_client):
            response = client.get("/api/uml-design/technology-stack")
            assert response.status_code == 200
            assert "error" in response.json()
    
    def test_get_uml_architecture(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取架构设计"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/uml-design/architecture")
            assert response.status_code == 200
            data = response.json()
            assert "components" in data
            assert "layers" in data
    
    def test_get_uml_outline(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取概要设计"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/uml-design/outline")
            assert response.status_code == 200
            data = response.json()
            assert "packages" in data
            assert "mermaid_diagram" in data
    
    def test_get_uml_detail(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取详细设计"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/uml-design/detail")
            assert response.status_code == 200
            data = response.json()
            assert "classes" in data
            assert "mermaid_diagram" in data
    
    def test_get_software_flow(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取软件脉络"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/uml-design/software-flow")
            assert response.status_code == 200
            data = response.json()
            assert "entry_points" in data
            assert "call_chains" in data
    
    def test_get_complete_uml_design(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取完整 UML 设计"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/uml-design/complete")
            assert response.status_code == 200
            data = response.json()
            assert "design_document" in data
