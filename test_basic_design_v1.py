#!/usr/bin/env python3
"""
基础design-v1测试脚本 - 测试核心模块导入
"""
import os
import sys
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 设置API密钥
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

def test_import_modules():
    """测试模块导入"""
    print("=== 测试模块导入 ===")
    
    modules_to_test = [
        "src.models.code_metadata",
        "src.agents.base_agent", 
        "src.infrastructure.config_manager",
    ]
    
    success_count = 0
    
    for module_name in modules_to_test:
        try:
            __import__(module_name)
            print(f"模块导入成功: {module_name}")
            success_count += 1
        except Exception as e:
            print(f"模块导入失败 {module_name}: {e}")
    
    return success_count == len(modules_to_test)

def test_models_creation():
    """测试数据模型创建"""
    print("\n=== 测试数据模型创建 ===")
    
    try:
        from src.models.code_metadata import (
            CodeLanguage, CodeComplexity, FunctionMetadata, 
            ClassMetadata, FileMetadata
        )
        
        # 创建测试复杂度
        complexity = CodeComplexity(
            cyclomatic_complexity=5,
            cognitive_complexity=3,
            lines_of_code=50,
            comment_density=0.2,
            maintainability_index=75.0
        )
        
        # 创建测试函数
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
        
        # 创建测试文件
        file_model = FileMetadata(
            file_path="/test/test.py",
            language=CodeLanguage.PYTHON,
            functions=[function],
            classes=[],
            variables=[],
            imports=[],
            exports=[],
            complexity=complexity,
            file_size=1024,
            last_modified="2024-01-01T00:00:00",
            encoding="utf-8"
        )
        
        print("数据模型创建成功")
        print(f"   - 函数: {function.name}")
        print(f"   - 文件: {file_model.file_path}")
        
        return True
        
    except Exception as e:
        print(f"数据模型创建失败: {e}")
        return False

def test_config_manager():
    """测试配置管理器"""
    print("\n=== 测试配置管理器 ===")
    
    try:
        from src.infrastructure.config_manager import ConfigManager
        
        config_manager = ConfigManager()
        
        # 获取配置
        ai_config = config_manager.get_ai_config()
        database_config = config_manager.get_database_config()
        
        print("配置管理器测试成功")
        print(f"   AI配置项数: {len(ai_config)}")
        print(f"   数据库配置项数: {len(database_config)}")
        
        return True
        
    except Exception as e:
        print(f"配置管理器测试失败: {e}")
        return False

def test_sample_project_analysis():
    """测试样例项目分析"""
    print("\n=== 测试样例项目分析 ===")
    
    sample_project_path = "output/example/stock-sentiment-analyzer"
    
    if not Path(sample_project_path).exists():
        print(f"样例项目不存在: {sample_project_path}")
        return False
    
    try:
        # 测试文件扫描功能
        from src.agents.file_scanner import FileScannerAgent
        
        file_scanner = FileScannerAgent({})
        
        # 扫描项目
        result = file_scanner.scan_project(sample_project_path)
        
        if result and 'files' in result:
            files = result['files']
            print(f"文件扫描成功: 发现 {len(files)} 个文件")
            
            # 显示前5个文件
            for file_info in files[:5]:
                print(f"   - {file_info.get('path')}")
            
            return True
        else:
            print("文件扫描失败")
            return False
            
    except Exception as e:
        print(f"样例项目分析失败: {e}")
        return False

def main():
    """主测试函数"""
    print("design-v1优化版本基础测试")
    print("=" * 50)
    
    # 测试模块导入
    import_test = test_import_modules()
    
    # 测试数据模型
    model_test = test_models_creation()
    
    # 测试配置管理器
    config_test = test_config_manager()
    
    # 测试样例项目分析
    project_test = test_sample_project_analysis()
    
    # 总结测试结果
    print("\n" + "=" * 50)
    print("测试结果总结:")
    print(f"   模块导入: {'通过' if import_test else '失败'}")
    print(f"   数据模型: {'通过' if model_test else '失败'}")
    print(f"   配置管理器: {'通过' if config_test else '失败'}")
    print(f"   样例项目分析: {'通过' if project_test else '失败'}")
    
    # 总体评估
    total_tests = 4
    passed_tests = sum([import_test, model_test, config_test, project_test])
    
    print(f"\n总体通过率: {passed_tests}/{total_tests} ({passed_tests/total_tests*100:.1f}%)")
    
    if passed_tests == total_tests:
        print("design-v1优化版本基础功能测试通过！")
    else:
        print("部分功能测试失败")

if __name__ == "__main__":
    main()