#!/usr/bin/env python3
"""
design-v1优化版本测试脚本 - 使用样例工程测试完整分析流程
"""
import os
import sys
import asyncio
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 设置API密钥
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

def test_workflow_engine():
    """测试工作流引擎功能"""
    print("=== 测试工作流引擎 ===")
    
    try:
        from src.core.workflow_engine import WorkflowEngine
        from src.infrastructure.config_manager import ConfigManager
        
        # 加载配置
        config_manager = ConfigManager()
        
        # 创建工作流引擎
        workflow_engine = WorkflowEngine(config_manager)
        
        print(f"工作流引擎初始化成功")
        print(f"可用的工作流: {list(workflow_engine.workflows.keys())}")
        
        return workflow_engine, config_manager
        
    except Exception as e:
        print(f"工作流引擎初始化失败: {e}")
        import traceback
        traceback.print_exc()
        return None, None

async def test_agent_execution(workflow_engine, config_manager):
    """测试Agent执行"""
    print("\n=== 测试Agent执行 ===")
    
    try:
        # 创建测试项目
        test_project_path = "output/example/stock-sentiment-analyzer"
        
        if not Path(test_project_path).exists():
            print(f"测试项目路径不存在: {test_project_path}")
            return False
            
        print(f"测试项目: {test_project_path}")
        
        # 执行快速扫描工作流
        workflow_type = "quick_scan"
        print(f"执行工作流: {workflow_type}")
        
        result = await workflow_engine.execute_workflow(
            workflow_type=workflow_type,
            project_path=test_project_path,
            output_dir="test_output_design_v1"
        )
        
        if result.success:
            print(f"工作流执行成功")
            print(f"   执行时间: {result.execution_time:.2f}秒")
            print(f"   处理的Agent数量: {len(result.agent_results)}")
            
            # 显示每个Agent的结果
            for agent_name, agent_result in result.agent_results.items():
                status = "成功" if agent_result.success else "失败"
                print(f"   {status} {agent_name}: {agent_result.execution_time:.2f}秒")
                
            return True
        else:
            print(f"工作流执行失败")
            print(f"   错误: {result.error}")
            return False
            
    except Exception as e:
        print(f"Agent执行测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_api_layer():
    """测试API层功能"""
    print("\n=== 测试API层 ===")
    
    try:
        from src.api.main import app
        from fastapi.testclient import TestClient
        
        client = TestClient(app)
        
        # 测试健康检查
        response = client.get("/health")
        print(f"健康检查: {response.status_code}")
        
        # 测试系统状态
        response = client.get("/api/v1/system/status")
        print(f"系统状态: {response.status_code}")
        
        return True
        
    except Exception as e:
        print(f"API层测试失败: {e}")
        return False

def test_database_client():
    """测试数据库客户端"""
    print("\n=== 测试数据库客户端 ===")
    
    try:
        from src.infrastructure.database_client import DatabaseClient
        from src.infrastructure.config_manager import ConfigManager
        
        config_manager = ConfigManager()
        
        # 创建数据库客户端（不实际连接，只测试配置加载）
        db_client = DatabaseClient(config_manager)
        
        print(f"数据库客户端初始化成功")
        print(f"   支持的数据库类型: {list(db_client.db_managers.keys())}")
        
        return True
        
    except Exception as e:
        print(f"数据库客户端测试失败: {e}")
        return False

def test_web_interface():
    """测试Web界面"""
    print("\n=== 测试Web界面 ===")
    
    try:
        from src.web.main import create_web_app
        
        # 创建Web应用（不实际启动）
        app = create_web_app()
        
        print(f"Web界面应用创建成功")
        
        # 检查是否有Streamlit组件
        import streamlit as st
        print(f"Streamlit可用")
        
        return True
        
    except Exception as e:
        print(f"Web界面测试失败: {e}")
        return False

async def main():
    """主测试函数"""
    print("design-v1优化版本功能测试")
    print("=" * 50)
    
    # 测试数据库客户端
    db_test = test_database_client()
    
    # 测试工作流引擎
    workflow_engine, config_manager = test_workflow_engine()
    
    # 测试API层
    api_test = test_api_layer()
    
    # 测试Web界面
    web_test = test_web_interface()
    
    # 如果工作流引擎初始化成功，执行Agent测试
    agent_test = False
    if workflow_engine and config_manager:
        agent_test = await test_agent_execution(workflow_engine, config_manager)
    
    # 总结测试结果
    print("\n" + "=" * 50)
    print("测试结果总结:")
    print(f"   数据库客户端: {'通过' if db_test else '失败'}")
    print(f"   工作流引擎: {'通过' if workflow_engine else '失败'}")
    print(f"   API层: {'通过' if api_test else '失败'}")
    print(f"   Web界面: {'通过' if web_test else '失败'}")
    print(f"   Agent执行: {'通过' if agent_test else '失败'}")
    
    # 检查输出文件
    output_dir = Path("test_output_design_v1")
    if output_dir.exists():
        print(f"\n输出文件检查:")
        for file_path in output_dir.rglob("*.md"):
            relative_path = file_path.relative_to(output_dir)
            print(f"   {relative_path}")
    
    print("\ndesign-v1优化版本测试完成！")

if __name__ == "__main__":
    asyncio.run(main())