"""
知识文档功能测试
"""
import pytest
from fastapi.testclient import TestClient

def test_get_entities(client: TestClient):
    """测试获取实体列表"""
    response = client.get("/api/entities")
    assert response.status_code == 200
    
    data = response.json()
    assert "entities" in data
    assert isinstance(data["entities"], list)

def test_get_entities_with_type_filter(client: TestClient):
    """测试按类型过滤获取实体列表"""
    response = client.get("/api/entities?entity_type=class")
    assert response.status_code == 200
    
    data = response.json()
    assert "entities" in data
    # 如果系统中有class类型的实体，这里应该会有数据
    # 如果没有，返回空列表也是合理的

def test_get_knowledge_result(client: TestClient):
    """测试获取知识分析结果"""
    response = client.get("/api/knowledge-result")
    assert response.status_code == 200
    
    data = response.json()
    # 可能返回错误信息（如果无数据）或实际结果
    if "error" in data:
        assert "暂无知识图谱数据" in data["error"]
    else:
        assert "entities_by_file" in data
        assert "key_entities" in data
        assert "relationship_summary" in data

def test_get_knowledge_stats(client: TestClient):
    """测试获取知识统计信息"""
    response = client.get("/api/knowledge-stats")
    assert response.status_code == 200
    
    data = response.json()
    if "error" in data:
        assert "暂无数据" in data["error"]
    else:
        assert "module_analysis" in data
        assert "complexity_analysis" in data
        assert "quality_analysis" in data
        assert "coupling_analysis" in data

def test_get_entity_details(client: TestClient):
    """测试获取实体详细信息"""
    # 先获取实体列表
    entities_response = client.get("/api/entities")
    if entities_response.status_code == 200:
        entities = entities_response.json()["entities"]
        if entities:
            # 使用第一个实体ID
            entity_id = entities[0]["id"]
            response = client.get(f"/api/entity/{entity_id}")
            assert response.status_code == 200
            
            data = response.json()
            assert "entity" in data
            assert "relationships" in data
            assert "neighbors" in data
        else:
            # 没有实体时的测试
            response = client.get("/api/entity/nonexistent")
            assert response.status_code == 404
            assert "detail" in response.json()
    else:
        # API错误时的测试
        response = client.get("/api/entity/test123")
        # 可能是404或200（取决于实现）
        assert response.status_code in [200, 404]

def test_validate_knowledge(client: TestClient):
    """测试验证知识提取准确性"""
    # 使用当前目录作为测试目录
    test_directory = "."
    response = client.post("/api/validate", data={"source_directory": test_directory})
    assert response.status_code == 200
    
    data = response.json()
    # 验证结果应该包含一些验证指标
    assert isinstance(data, dict)

def test_export_knowledge(client: TestClient):
    """测试导出知识图谱"""
    payload = {
        "output_path": "./test_export",
        "format": "markdown"
    }
    
    response = client.post("/api/export", json=payload)
    assert response.status_code == 200
    
    data = response.json()
    assert "status" in data
    assert data["status"] == "success"
    assert "output_path" in data

def test_export_knowledge_invalid_format(client: TestClient):
    """测试导出知识图谱 - 无效格式"""
    payload = {
        "output_path": "./test_export",
        "format": "invalid_format"
    }
    
    response = client.post("/api/export", json=payload)
    # 可能是400或500错误
    assert response.status_code in [400, 500]
    
    if response.status_code == 400:
        assert "detail" in response.json()

def test_optimize_knowledge(client: TestClient):
    """测试优化知识图谱"""
    payload = {
        "level": "medium"
    }
    
    response = client.post("/api/optimize", json=payload)
    assert response.status_code == 200
    
    data = response.json()
    assert "status" in data
    assert data["status"] == "success"
    assert "message" in data

def test_optimize_knowledge_invalid_level(client: TestClient):
    """测试优化知识图谱 - 无效级别"""
    payload = {
        "level": "invalid_level"
    }
    
    response = client.post("/api/optimize", json=payload)
    # 可能是400或500错误
    assert response.status_code in [400, 500]
    
    if response.status_code == 400:
        assert "detail" in response.json()

def test_architecture_analysis(client: TestClient):
    """测试架构分析"""
    response = client.get("/api/architecture/analysis")
    assert response.status_code == 200
    
    data = response.json()
    if "error" in data:
        assert "暂无数据" in data["error"]
    else:
        assert "layers" in data
        assert "components" in data
        assert "dependencies" in data

def test_architecture_patterns(client: TestClient):
    """测试架构模式检测"""
    response = client.get("/api/architecture/patterns")
    assert response.status_code == 200
    
    data = response.json()
    if "error" in data:
        assert "暂无数据" in data["error"]
    else:
        assert "patterns" in data
        assert isinstance(data["patterns"], list)

def test_architecture_recommendations(client: TestClient):
    """测试架构改进建议"""
    response = client.get("/api/architecture/recommendations")
    assert response.status_code == 200
    
    data = response.json()
    if "error" in data:
        assert "暂无数据" in data["error"]
    else:
        assert "recommendations" in data
        assert isinstance(data["recommendations"], list)

def test_architecture_quality(client: TestClient):
    """测试架构质量评估"""
    response = client.get("/api/architecture/quality")
    assert response.status_code == 200
    
    data = response.json()
    if "error" in data:
        assert "暂无数据" in data["error"]
    else:
        assert "quality_metrics" in data
        assert isinstance(data["quality_metrics"], dict)

def test_architecture_visualization(client: TestClient):
    """测试架构可视化数据"""
    response = client.get("/api/architecture/visualization")
    assert response.status_code == 200
    
    data = response.json()
    if "error" in data:
        assert "暂无数据" in data["error"]
    else:
        assert "nodes" in data
        assert "edges" in data
        assert isinstance(data["nodes"], list)
        assert isinstance(data["edges"], list)