"""
增强分析功能测试
"""
import pytest
from fastapi.testclient import TestClient

def test_enhanced_analysis_success(client: TestClient):
    """测试增强分析功能 - 成功案例"""
    payload = {
        "command": "analyze_complexity",
        "parameters": {
            "depth": 2,
            "include_metrics": True
        }
    }
    
    response = client.post("/api/analysis/enhanced", json=payload)
    assert response.status_code == 200
    
    data = response.json()
    assert "status" in data
    assert "analysis_id" in data
    assert data["status"] == "accepted"

def test_enhanced_analysis_invalid_command(client: TestClient):
    """测试无效命令的增强分析"""
    payload = {
        "command": "invalid_command",
        "parameters": {}
    }
    
    response = client.post("/api/analysis/enhanced", json=payload)
    assert response.status_code == 400
    
    data = response.json()
    assert "detail" in data

def test_enhanced_analysis_with_path(client: TestClient):
    """测试带路径参数的增强分析"""
    payload = {
        "command": "analyze_dependencies",
        "parameters": {
            "target_path": "src/gitnexus",
            "recursive": True
        }
    }
    
    response = client.post("/api/analysis/enhanced", json=payload)
    assert response.status_code == 200
    
    data = response.json()
    assert "analysis_id" in data

def test_enhanced_analysis_status(client: TestClient):
    """测试获取增强分析状态"""
    # 先创建一个分析任务
    payload = {
        "command": "analyze_complexity",
        "parameters": {"depth": 1}
    }
    
    create_response = client.post("/api/analysis/enhanced", json=payload)
    assert create_response.status_code == 200
    
    analysis_id = create_response.json()["analysis_id"]
    
    # 获取状态
    response = client.get(f"/api/analysis/enhanced/{analysis_id}/status")
    assert response.status_code == 200
    
    data = response.json()
    assert "status" in data
    assert "progress" in data

def test_enhanced_analysis_result(client: TestClient):
    """测试获取增强分析结果"""
    # 先创建一个分析任务
    payload = {
        "command": "analyze_structure",
        "parameters": {"level": "module"}
    }
    
    create_response = client.post("/api/analysis/enhanced", json=payload)
    assert create_response.status_code == 200
    
    analysis_id = create_response.json()["analysis_id"]
    
    # 获取结果
    response = client.get(f"/api/analysis/enhanced/{analysis_id}/result")
    assert response.status_code == 200
    
    data = response.json()
    assert "analysis_id" in data
    assert "command" in data

def test_enhanced_analysis_list(client: TestClient):
    """测试获取增强分析列表"""
    response = client.get("/api/analysis/enhanced")
    assert response.status_code == 200
    
    data = response.json()
    assert isinstance(data, list)

def test_enhanced_analysis_cancel(client: TestClient):
    """测试取消增强分析"""
    # 先创建一个分析任务
    payload = {
        "command": "analyze_performance",
        "parameters": {}
    }
    
    create_response = client.post("/api/analysis/enhanced", json=payload)
    assert create_response.status_code == 200
    
    analysis_id = create_response.json()["analysis_id"]
    
    # 取消分析
    response = client.delete(f"/api/analysis/enhanced/{analysis_id}")
    assert response.status_code == 200
    
    data = response.json()
    assert "status" in data
    assert data["status"] == "cancelled"