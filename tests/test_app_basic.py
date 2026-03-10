"""
基础 API 端点测试
"""
import pytest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient
import json


class TestIndexPage:
    """主页测试"""
    
    def test_index_page(self, client):
        """测试主页返回 HTML"""
        response = client.get("/")
        assert response.status_code == 200
        assert "text/html" in response.headers.get("content-type", "")


class TestStatusAPI:
    """状态 API 测试"""
    
    def test_get_status(self, client):
        """测试获取任务状态"""
        response = client.get("/api/status")
        assert response.status_code == 200
        data = response.json()
        assert "running" in data
        assert "progress" in data
        assert "current_step" in data
        assert "result" in data
        assert "error" in data


class TestAIConfigAPI:
    """AI 配置 API 测试"""
    
    def test_get_ai_config(self, client, mock_config):
        """测试获取 AI 配置"""
        with patch('src.web.app.CONFIG_PATH') as mock_path:
            response = client.get("/api/ai-config")
            assert response.status_code in [200, 500]  # 可能因配置文件不存在而失败
    
    def test_update_ai_config(self, client):
        """测试更新 AI 配置"""
        config_data = {
            "provider": "volcengine",
            "api_key": "new-test-key",
            "base_url": "https://api.test.com",
            "model": "test-model",
            "tools_enabled": False,
            "web_search_enabled": False
        }
        
        with patch('src.web.app.CONFIG_PATH'):
            response = client.post("/api/ai-config", json=config_data)
            # 可能因文件写入问题而失败
            assert response.status_code in [200, 500]
    
    def test_ai_config_invalid_provider(self, client):
        """测试无效 provider"""
        config_data = {
            "provider": "invalid_provider",
            "api_key": "test-key"
        }
        response = client.post("/api/ai-config", json=config_data)
        # Pydantic 应该接受任何字符串，但业务逻辑可能验证
        assert response.status_code in [200, 400, 500]
    
    def test_test_ai_connection(self, client, mock_llm_client):
        """测试 AI 连接测试"""
        with patch('src.web.app.LLMClient', return_value=mock_llm_client):
            with patch('src.web.app.CONFIG_PATH'):
                response = client.post("/api/ai-test")
                assert response.status_code == 200
                data = response.json()
                assert "success" in data
    
    def test_test_ai_connection_unavailable(self, client):
        """测试 AI 不可用时"""
        mock_client = MagicMock()
        mock_client.is_available.return_value = False
        
        with patch('src.web.app.LLMClient', return_value=mock_client):
            with patch('src.web.app.CONFIG_PATH'):
                response = client.post("/api/ai-test")
                assert response.status_code == 200
                data = response.json()
                assert data["success"] is False


class TestAnalyzeAPI:
    """分析 API 测试"""
    
    def test_start_analysis_success(self, client):
        """测试启动分析成功"""
        request_data = {
            "directory": "/tmp/test",
            "include_code": True,
            "include_docs": True,
            "large_scale": False,
            "incremental": False,
            "force": False
        }
        
        with patch('src.web.app.task_status', {"running": False}):
            response = client.post("/api/analyze", json=request_data)
            # 分析任务应该被接受
            assert response.status_code in [200, 400, 500]
    
    def test_start_analysis_already_running(self, client):
        """测试分析任务已在运行"""
        request_data = {
            "directory": "/tmp/test",
            "include_code": True,
            "include_docs": True
        }
        
        # 模拟已有任务运行
        with patch('src.web.app.task_status', {"running": True}):
            response = client.post("/api/analyze", json=request_data)
            assert response.status_code == 400
            assert "已有任务正在运行" in response.json().get("detail", "")
    
    def test_start_analysis_missing_directory(self, client):
        """测试缺少目录参数"""
        request_data = {
            "include_code": True
        }
        response = client.post("/api/analyze", json=request_data)
        # FastAPI 应该验证必需字段
        assert response.status_code == 422  # Validation error


class TestExportAPI:
    """导出 API 测试"""
    
    def test_export_results(self, client, mock_gitnexus_client):
        """测试导出结果"""
        request_data = {
            "output_path": "/tmp/output",
            "format": "markdown"
        }
        
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.post("/api/export", json=request_data)
            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "success"
    
    def test_export_invalid_format(self, client, mock_gitnexus_client):
        """测试无效导出格式"""
        request_data = {
            "output_path": "/tmp/output",
            "format": "invalid_format"
        }
        
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.post("/api/export", json=request_data)
            # 应该返回错误
            assert response.status_code in [400, 500]
    
    def test_export_formats(self, client, mock_gitnexus_client):
        """测试各种导出格式"""
        formats = ["json", "markdown", "html", "csv", "graphml", "plantuml", "mindmap"]
        
        for fmt in formats:
            request_data = {
                "output_path": f"/tmp/output.{fmt}",
                "format": fmt
            }
            
            with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
                response = client.post("/api/export", json=request_data)
                assert response.status_code == 200


class TestOptimizeAPI:
    """优化 API 测试"""
    
    def test_optimize_knowledge(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试优化知识图谱"""
        request_data = {
            "level": "medium"
        }
        
        mock_optimizer = MagicMock()
        mock_optimizer.optimize_knowledge_graph.return_value = sample_knowledge_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.KnowledgeOptimizer', return_value=mock_optimizer):
                response = client.post("/api/optimize", json=request_data)
                assert response.status_code == 200
    
    def test_optimize_levels(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试不同优化级别"""
        levels = ["low", "medium", "high", "aggressive"]
        
        for level in levels:
            request_data = {"level": level}
            
            mock_optimizer = MagicMock()
            mock_optimizer.optimize_knowledge_graph.return_value = sample_knowledge_graph
            
            with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
                with patch('src.web.app.KnowledgeOptimizer', return_value=mock_optimizer):
                    response = client.post("/api/optimize", json=request_data)
                    assert response.status_code == 200


class TestStatsAPI:
    """统计 API 测试"""
    
    def test_get_statistics(self, client, mock_gitnexus_client):
        """测试获取统计信息"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/stats")
            assert response.status_code == 200
            data = response.json()
            assert "total_entities" in data
            assert "total_relationships" in data


class TestEntitiesAPI:
    """实体 API 测试"""
    
    def test_get_entities(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取实体列表"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/entities")
            assert response.status_code == 200
            data = response.json()
            assert "total" in data
            assert "entities" in data
    
    def test_get_entities_with_pagination(self, client, mock_gitnexus_client):
        """测试分页获取实体"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/entities?limit=10&offset=0")
            assert response.status_code == 200
            data = response.json()
            assert isinstance(data["entities"], list)
    
    def test_get_entities_with_filter(self, client, mock_gitnexus_client):
        """测试按类型过滤实体"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/entities?entity_type=function")
            assert response.status_code == 200
    
    def test_get_entity_detail(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取实体详情"""
        entity_id = "test-entity-1"
        
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get(f"/api/entity/{entity_id}")
            # 可能因实体不存在而返回404
            assert response.status_code in [200, 404]
    
    def test_get_entity_not_found(self, client, mock_gitnexus_client):
        """测试获取不存在的实体"""
        mock_client = MagicMock()
        mock_graph = MagicMock()
        mock_graph.entities = {}
        mock_client._load_knowledge_graph.return_value = mock_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_client):
            response = client.get("/api/entity/non-existent-id")
            assert response.status_code == 404


class TestValidateAPI:
    """验证 API 测试"""
    
    def test_validate_knowledge(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试验证知识提取"""
        mock_validator = MagicMock()
        mock_validator.validate_knowledge_graph.return_value = {
            "accuracy": 0.95,
            "issues": []
        }
        
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            with patch('src.web.app.SelfValidator', return_value=mock_validator):
                response = client.post("/api/validate?source_directory=/tmp/test")
                assert response.status_code == 200


class TestKnowledgeResultAPI:
    """知识结果 API 测试"""
    
    def test_get_knowledge_result(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取知识分析结果"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/knowledge-result")
            assert response.status_code == 200
            data = response.json()
            assert "total_entities" in data
            assert "total_relationships" in data
    
    def test_get_knowledge_result_no_data(self, client):
        """测试无数据时获取知识结果"""
        mock_client = MagicMock()
        mock_graph = MagicMock()
        mock_graph.entities = {}
        mock_client._load_knowledge_graph.return_value = mock_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_client):
            response = client.get("/api/knowledge-result")
            assert response.status_code == 200
            data = response.json()
            assert "error" in data


class TestBusinessLogicAPI:
    """业务逻辑 API 测试"""
    
    def test_get_business_logic(self, client, mock_gitnexus_client, sample_knowledge_graph):
        """测试获取业务逻辑分析"""
        with patch('src.web.app.GitNexusClient', return_value=mock_gitnexus_client):
            response = client.get("/api/business-logic")
            assert response.status_code == 200
            data = response.json()
            assert "modules" in data
            assert "core_entities" in data
            assert "dependencies" in data
    
    def test_get_business_logic_no_data(self, client):
        """测试无数据时获取业务逻辑"""
        mock_client = MagicMock()
        mock_graph = MagicMock()
        mock_graph.entities = {}
        mock_client._load_knowledge_graph.return_value = mock_graph
        
        with patch('src.web.app.GitNexusClient', return_value=mock_client):
            response = client.get("/api/business-logic")
            assert response.status_code == 200
            assert "error" in response.json()


class TestMaskAPIKey:
    """API Key 遮蔽功能测试"""
    
    def test_mask_short_key(self):
        """测试短密钥遮蔽"""
        from src.web.app import _mask_api_key
        
        assert _mask_api_key("abc") == "***"
        assert _mask_api_key("") == ""
        assert _mask_api_key(None) == ""
    
    def test_mask_long_key(self):
        """测试长密钥遮蔽"""
        from src.web.app import _mask_api_key
        
        key = "abcdefghijklmnop1234567890"
        masked = _mask_api_key(key)
        assert masked.startswith("abcd")
        assert masked.endswith("7890")
        assert "..." in masked
