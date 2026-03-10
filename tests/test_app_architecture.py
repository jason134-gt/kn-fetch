"""
架构分析 API 端点测试
"""
import pytest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient


class TestArchitectureAPI:
    """架构分析 API 测试"""
    
    def test_get_architecture(self, client, mock_gitnexus_client, mock_architecture_analyzer, sample_knowledge_graph):
        """测试获取完整架构分析"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                response = client.get("/api/architecture")
                assert response.status_code == 200
                data = response.json()
                assert "modules" in data
                assert "features" in data
                assert "statistics" in data
    
    def test_get_architecture_no_data(self, client):
        """测试无数据时获取架构"""
        mock_client = MagicMock()
        mock_graph = MagicMock()
        mock_graph.entities = {}
        mock_client._load_knowledge_graph.return_value = mock_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_client):
            response = client.get("/api/architecture")
            assert response.status_code == 200
            assert "error" in response.json()


class TestArchitectureModulesAPI:
    """模块结构 API 测试"""
    
    def test_get_architecture_modules(self, client, mock_gitnexus_client, mock_architecture_analyzer):
        """测试获取模块结构"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                response = client.get("/api/architecture/modules")
                assert response.status_code == 200
                data = response.json()
                assert "modules" in data
    
    def test_get_architecture_modules_no_data(self, client):
        """测试无数据时获取模块"""
        mock_client = MagicMock()
        mock_graph = MagicMock()
        mock_graph.entities = {}
        mock_client._load_knowledge_graph.return_value = mock_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_client):
            response = client.get("/api/architecture/modules")
            assert response.status_code == 200
            assert "error" in response.json()


class TestArchitectureFeaturesAPI:
    """功能模块 API 测试"""
    
    def test_get_architecture_features(self, client, mock_gitnexus_client, mock_architecture_analyzer):
        """测试获取功能模块"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                response = client.get("/api/architecture/features")
                assert response.status_code == 200
                data = response.json()
                assert "features" in data


class TestArchitectureCallChainsAPI:
    """调用链 API 测试"""
    
    def test_get_architecture_call_chains(self, client, mock_gitnexus_client, mock_architecture_analyzer):
        """测试获取调用链分析"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                response = client.get("/api/architecture/call-chains")
                assert response.status_code == 200
                data = response.json()
                assert "call_chains" in data
    
    def test_get_architecture_call_chains_with_params(self, client, mock_gitnexus_client, mock_architecture_analyzer):
        """测试带参数获取调用链"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                response = client.get("/api/architecture/call-chains?limit=10&max_depth=5")
                assert response.status_code == 200


class TestArchitectureExceptionsAPI:
    """异常流 API 测试"""
    
    def test_get_architecture_exceptions(self, client, mock_gitnexus_client, mock_architecture_analyzer):
        """测试获取异常流分析"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                response = client.get("/api/architecture/exceptions")
                assert response.status_code == 200
                data = response.json()
                assert "exception_flows" in data


class TestRefactoringSuggestionsAPI:
    """重构建议 API 测试"""
    
    def test_get_refactoring_suggestions(self, client, mock_gitnexus_client, mock_architecture_analyzer, sample_knowledge_graph):
        """测试获取重构建议"""
        mock_architecture_analyzer.generate_statistics.return_value = {
            "most_called_entities": [
                {"id": "test-entity-1", "calls": 15}
            ]
        }
        
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureAnalyzer', return_value=mock_architecture_analyzer):
                response = client.get("/api/refactoring-suggestions")
                assert response.status_code == 200
                data = response.json()
                assert "suggestions" in data
                assert "summary" in data
    
    def test_get_refactoring_suggestions_no_data(self, client):
        """测试无数据时获取重构建议"""
        mock_client = MagicMock()
        mock_graph = MagicMock()
        mock_graph.entities = {}
        mock_client._load_knowledge_graph.return_value = mock_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_client):
            response = client.get("/api/refactoring-suggestions")
            assert response.status_code == 200
            assert "error" in response.json()


class TestExportArchitectureReportAPI:
    """导出架构报告 API 测试"""
    
    def test_export_architecture_report(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试导出架构报告"""
        mock_reporter = MagicMock()
        
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.ArchitectureReporter', return_value=mock_reporter):
                response = client.post("/api/export-architecture-report?output_path=/tmp/report.md")
                assert response.status_code == 200
                data = response.json()
                assert data["status"] == "success"
    
    def test_export_architecture_report_no_data(self, client):
        """测试无数据时导出报告"""
        mock_client = MagicMock()
        mock_graph = MagicMock()
        mock_graph.entities = {}
        mock_client._load_knowledge_graph.return_value = mock_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_client):
            response = client.post("/api/export-architecture-report?output_path=/tmp/report.md")
            assert response.status_code == 400
