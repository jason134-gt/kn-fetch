"""
集成测试脚本 - 验证design-v1优化版本的系统功能
"""

import asyncio
import os
import sys
from datetime import datetime

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from src.infrastructure.config_manager import init_config, get_config_manager
from src.core.workflow_engine import get_workflow_engine
from src.agents import FileScannerAgent, CodeParserAgent


async def test_config_manager():
    """测试配置管理器"""
    print("\n=== 测试配置管理器 ===")
    
    try:
        # 初始化配置管理器
        config_manager = init_config("config")
        
        # 测试配置加载
        if config_manager.load_config():
            print("✅ 配置加载成功")
        else:
            print("❌ 配置加载失败")
            return False
        
        # 测试配置获取
        app_config = config_manager.get_app_config()
        ai_config = config_manager.get_ai_config()
        
        print(f"✅ 应用配置: {app_config.name} v{app_config.version}")
        print(f"✅ AI配置: {ai_config.provider} ({ai_config.model})")
        
        # 测试Agent配置
        file_scanner_config = config_manager.get_agent_config("file_scanner")
        print(f"✅ Agent配置: 文件扫描器配置加载成功")
        
        return True
        
    except Exception as e:
        print(f"❌ 配置管理器测试失败: {e}")
        return False


async def test_agents():
    """测试Agent功能"""
    print("\n=== 测试Agent功能 ===")
    
    try:
        config_manager = get_config_manager()
        
        # 测试文件扫描Agent
        file_scanner_config = config_manager.get_agent_config("file_scanner")
        file_scanner = FileScannerAgent(file_scanner_config)
        
        if file_scanner.is_available():
            print("✅ 文件扫描Agent初始化成功")
        else:
            print("❌ 文件扫描Agent不可用")
            return False
        
        # 测试代码解析Agent
        code_parser_config = config_manager.get_agent_config("code_parser")
        code_parser = CodeParserAgent(code_parser_config)
        
        if code_parser.is_available():
            print("✅ 代码解析Agent初始化成功")
        else:
            print("❌ 代码解析Agent不可用")
            return False
        
        # 测试简单的文件扫描
        test_files = await file_scanner._scan_files(".")
        print(f"✅ 文件扫描测试: 发现 {len(test_files)} 个文件")
        
        return True
        
    except Exception as e:
        print(f"❌ Agent测试失败: {e}")
        return False


async def test_workflow_engine():
    """测试工作流引擎"""
    print("\n=== 测试工作流引擎 ===")
    
    try:
        workflow_engine = get_workflow_engine()
        
        # 测试工作流列表
        available_workflows = workflow_engine.list_available_workflows()
        print(f"✅ 可用工作流: {available_workflows}")
        
        # 测试工作流定义
        full_analysis_workflow = workflow_engine.get_workflow_definition("full_analysis")
        if full_analysis_workflow:
            print(f"✅ 完整分析工作流定义: {len(full_analysis_workflow)} 个步骤")
        else:
            print("❌ 工作流定义获取失败")
            return False
        
        # 测试工作流依赖检查
        if full_analysis_workflow:
            # 检查第一个步骤的依赖
            first_step = full_analysis_workflow[0]
            if not first_step.dependencies:
                print("✅ 第一个步骤无依赖（正确）")
            else:
                print("❌ 第一个步骤不应该有依赖")
                return False
            
            # 检查后续步骤的依赖
            for i, step in enumerate(full_analysis_workflow[1:], 1):
                if step.dependencies:
                    print(f"✅ 步骤 {i+1} ({step.name}) 有依赖: {step.dependencies}")
                else:
                    print(f"⚠️ 步骤 {i+1} ({step.name}) 无依赖")
        
        return True
        
    except Exception as e:
        print(f"❌ 工作流引擎测试失败: {e}")
        return False


async def test_api_structure():
    """测试API结构"""
    print("\n=== 测试API结构 ===")
    
    try:
        # 测试API模块导入
        from src.api.main import app
        
        if app:
            print("✅ FastAPI应用创建成功")
            print(f"✅ API标题: {app.title}")
            print(f"✅ API版本: {app.version}")
        else:
            print("❌ FastAPI应用创建失败")
            return False
        
        # 测试API路由
        routes = [route for route in app.routes if hasattr(route, 'path')]
        print(f"✅ 注册路由数: {len(routes)}")
        
        # 显示主要路由
        main_routes = [route for route in routes if route.path.startswith('/api')]
        for route in main_routes[:5]:  # 显示前5个API路由
            methods = getattr(route, 'methods', ['GET'])
            print(f"  - {list(methods)[0]} {route.path}")
        
        return True
        
    except Exception as e:
        print(f"❌ API结构测试失败: {e}")
        return False


async def test_web_interface():
    """测试Web界面结构"""
    print("\n=== 测试Web界面结构 ===")
    
    try:
        # 测试Web模块导入
        from src.web.main import create_web_app
        
        if create_web_app:
            print("✅ Streamlit应用函数导入成功")
        else:
            print("❌ Streamlit应用函数导入失败")
            return False
        
        # 测试配置管理器在Web中的使用
        from src.infrastructure.config_manager import get_config_manager
        config_manager = get_config_manager()
        app_config = config_manager.get_app_config()
        
        print(f"✅ Web界面配置: {app_config.name}")
        print(f"✅ 运行环境: {app_config.environment.value}")
        
        return True
        
    except Exception as e:
        print(f"❌ Web界面测试失败: {e}")
        return False


async def run_all_tests():
    """运行所有集成测试"""
    print("🚀 开始design-v1优化版本集成测试")
    print("=" * 60)
    
    test_results = []
    
    # 运行各个测试
    tests = [
        ("配置管理器", test_config_manager),
        ("Agent功能", test_agents),
        ("工作流引擎", test_workflow_engine),
        ("API结构", test_api_structure),
        ("Web界面", test_web_interface)
    ]
    
    for test_name, test_func in tests:
        try:
            result = await test_func()
            test_results.append((test_name, result))
        except Exception as e:
            print(f"❌ {test_name} 测试异常: {e}")
            test_results.append((test_name, False))
    
    # 输出测试结果
    print("\n" + "=" * 60)
    print("📊 集成测试结果汇总")
    print("=" * 60)
    
    passed_tests = 0
    total_tests = len(test_results)
    
    for test_name, result in test_results:
        status = "✅ 通过" if result else "❌ 失败"
        print(f"{test_name}: {status}")
        if result:
            passed_tests += 1
    
    print("-" * 60)
    success_rate = (passed_tests / total_tests) * 100
    print(f"测试通过率: {passed_tests}/{total_tests} ({success_rate:.1f}%)")
    
    if passed_tests == total_tests:
        print("🎉 所有测试通过！design-v1优化版本集成成功！")
    else:
        print(f"⚠️  {total_tests - passed_tests} 个测试失败，需要进一步检查")
    
    return passed_tests == total_tests


async def main():
    """主函数"""
    
    # 设置环境变量（确保API密钥正确加载）
    os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'
    os.environ['KN_FETCH_ENV'] = 'development'
    
    # 运行集成测试
    success = await run_all_tests()
    
    if success:
        print("\n🎯 下一步行动:")
        print("1. 启动API服务: python -m src.api.main")
        print("2. 启动Web界面: streamlit run src/web/main.py")
        print("3. 测试完整分析流程")
        print("4. 验证数据库连接")
    else:
        print("\n🔧 需要修复的问题:")
        print("1. 检查配置文件格式")
        print("2. 验证依赖包安装")
        print("3. 检查环境变量设置")
    
    return success


if __name__ == "__main__":
    # 运行异步测试
    success = asyncio.run(main())
    sys.exit(0 if success else 1)