#!/usr/bin/env python3
"""
简化版design-v1测试脚本 - 测试核心功能
"""
import os
import sys
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 设置API密钥
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

def test_agent_creation():
    """测试Agent创建"""
    print("=== 测试Agent创建 ===")
    
    try:
        # 测试基础Agent
        from src.agents.base_agent import BaseAgent
        
        class TestAgent(BaseAgent):
            def __init__(self):
                super().__init__("test_agent", {})
            
            async def _execute_impl(self, input_data):
                return {"result": "test_success"}
        
        test_agent = TestAgent()
        print(f"基础Agent创建成功: {test_agent.agent_id}")
        
        # 测试文件扫描Agent
        from src.agents.file_scanner import FileScannerAgent
        file_scanner = FileScannerAgent({})
        print(f"文件扫描Agent创建成功: {file_scanner.agent_id}")
        
        # 测试代码解析Agent
        from src.agents.code_parser import CodeParserAgent
        code_parser = CodeParserAgent({})
        print(f"代码解析Agent创建成功: {code_parser.agent_id}")
        
        return True
        
    except Exception as e:
        print(f"Agent创建测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_models():
    """测试数据模型"""
    print("\n=== 测试数据模型 ===")
    
    try:
        from src.models.code_metadata import (
            CodeLanguage, CodeComplexity, FunctionMetadata, 
            ClassMetadata, FileMetadata, CodeMetadata
        )
        
        # 测试复杂度模型
        complexity = CodeComplexity(
            cyclomatic_complexity=5,
            cognitive_complexity=3,
            lines_of_code=50,
            comment_density=0.2,
            maintainability_index=75.0
        )
        print(f"✅ 复杂度模型创建成功")
        
        # 测试函数模型
        function = FunctionMetadata(
            name="test_function",
            signature="def test_function(x, y)",
            parameters=["x", "y"],
            return_type="int",
            docstring="测试函数",
            complexity=complexity,
            start_line=10,
            end_line=20,
            is_public=True,
            is_async=False,
            decorators=[]
        )
        print(f"✅ 函数模型创建成功: {function.name}")
        
        # 测试类模型
        class_model = ClassMetadata(
            name="TestClass",
            base_classes=["BaseClass"],
            docstring="测试类",
            methods=[function],
            properties=["attr1", "attr2"],
            complexity=complexity,
            start_line=5,
            end_line=25,
            is_abstract=False,
            decorators=[]
        )
        print(f"✅ 类模型创建成功: {class_model.name}")
        
        # 测试文件模型
        file_model = FileMetadata(
            file_path="/test/test.py",
            language=CodeLanguage.PYTHON,
            functions=[function],
            classes=[class_model],
            variables=[],
            imports=[],
            exports=[],
            complexity=complexity,
            file_size=1024,
            last_modified="2024-01-01T00:00:00",
            encoding="utf-8"
        )
        print(f"✅ 文件模型创建成功: {file_model.file_path}")
        
        # 测试代码元数据模型
        code_metadata = CodeMetadata(
            repository_info={"name": "test_repo"},
            files=[file_model],
            total_files=1,
            total_lines=100,
            total_functions=1,
            total_classes=1,
            average_complexity=complexity
        )
        print(f"✅ 代码元数据模型创建成功")
        
        return True
        
    except Exception as e:
        print(f"❌ 数据模型测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_config_manager():
    """测试配置管理器"""
    print("\n=== 测试配置管理器 ===")
    
    try:
        from src.infrastructure.config_manager import ConfigManager
        
        config_manager = ConfigManager()
        print(f"✅ 配置管理器创建成功")
        
        # 测试配置获取
        ai_config = config_manager.get_ai_config()
        print(f"✅ AI配置获取成功: {len(ai_config)} 个配置项")
        
        database_config = config_manager.get_database_config()
        print(f"✅ 数据库配置获取成功: {len(database_config)} 个配置项")
        
        return True
        
    except Exception as e:
        print(f"❌ 配置管理器测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_file_scanning():
    """测试文件扫描功能"""
    print("\n=== 测试文件扫描 ===")
    
    try:
        from src.agents.file_scanner import FileScannerAgent
        
        # 创建文件扫描Agent
        file_scanner = FileScannerAgent({})
        
        # 测试扫描样例项目
        sample_project_path = "output/example/stock-sentiment-analyzer"
        
        if Path(sample_project_path).exists():
            print(f"📁 扫描项目: {sample_project_path}")
            
            # 执行文件扫描
            result = file_scanner.scan_project(sample_project_path)
            
            if result:
                print(f"✅ 文件扫描成功")
                print(f"   发现文件数: {len(result.get('files', []))}")
                
                # 显示前5个文件
                files = result.get('files', [])[:5]
                for file_info in files:
                    print(f"   - {file_info.get('path')}")
                
                return True
            else:
                print(f"❌ 文件扫描失败")
                return False
        else:
            print(f"❌ 样例项目路径不存在: {sample_project_path}")
            return False
            
    except Exception as e:
        print(f"❌ 文件扫描测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    """主测试函数"""
    print("design-v1优化版本简化测试")
    print("=" * 50)
    
    # 测试Agent创建
    agent_test = test_agent_creation()
    
    # 测试数据模型
    model_test = test_models()
    
    # 测试配置管理器
    config_test = test_config_manager()
    
    # 测试文件扫描
    scan_test = test_file_scanning()
    
    # 总结测试结果
    print("\n" + "=" * 50)
    print("测试结果总结:")
    print(f"   Agent创建: {'通过' if agent_test else '失败'}")
    print(f"   数据模型: {'通过' if model_test else '失败'}")
    print(f"   配置管理器: {'通过' if config_test else '失败'}")
    print(f"   文件扫描: {'通过' if scan_test else '失败'}")
    
    # 总体评估
    total_tests = 4
    passed_tests = sum([agent_test, model_test, config_test, scan_test])
    
    print(f"\n总体通过率: {passed_tests}/{total_tests} ({passed_tests/total_tests*100:.1f}%)")
    
    if passed_tests == total_tests:
        print("design-v1优化版本核心功能测试通过！")
    else:
        print("部分功能测试失败，需要进一步调试")

if __name__ == "__main__":
    main()