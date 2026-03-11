#!/usr/bin/env python3
"""
测试现有功能 - 使用原始版本的功能测试样例工程
"""
import os
import sys
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 设置API密钥
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

def test_file_scanning():
    """测试文件扫描功能"""
    print("=== 测试文件扫描功能 ===")
    
    sample_project_path = "output/example/stock-sentiment-analyzer"
    
    if not Path(sample_project_path).exists():
        print(f"样例项目不存在: {sample_project_path}")
        return False
    
    try:
        # 使用现有的文件扫描功能
        from src.core.semantic_extractor import scan_project_files
        
        print(f"扫描项目: {sample_project_path}")
        
        # 扫描项目文件
        files = scan_project_files(sample_project_path)
        
        if files:
            print(f"文件扫描成功: 发现 {len(files)} 个文件")
            
            # 显示前10个文件
            for i, file_path in enumerate(files[:10]):
                print(f"   {i+1}. {file_path}")
            
            if len(files) > 10:
                print(f"   ... 还有 {len(files) - 10} 个文件")
            
            return True
        else:
            print("文件扫描失败: 未发现文件")
            return False
            
    except Exception as e:
        print(f"文件扫描失败: {e}")
        return False

def test_code_analysis():
    """测试代码分析功能"""
    print("\n=== 测试代码分析功能 ===")
    
    sample_project_path = "output/example/stock-sentiment-analyzer"
    
    if not Path(sample_project_path).exists():
        print(f"样例项目不存在: {sample_project_path}")
        return False
    
    try:
        # 测试单个Python文件分析
        test_file = Path(sample_project_path) / "gitnexus.py"
        
        if not test_file.exists():
            print(f"测试文件不存在: {test_file}")
            # 尝试其他文件
            py_files = list(Path(sample_project_path).rglob("*.py"))
            if py_files:
                test_file = py_files[0]
                print(f"使用文件: {test_file}")
            else:
                print("项目中未找到Python文件")
                return False
        
        # 读取文件内容
        with open(test_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        print(f"分析文件: {test_file}")
        print(f"文件大小: {len(content)} 字符")
        
        # 使用现有的代码分析功能
        from src.ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer
        import yaml
        
        # 加载配置
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        # 创建分析器
        analyzer = DeepKnowledgeAnalyzer(config, "test_output_existing")
        
        if analyzer.is_available():
            print("深度知识分析器可用")
            
            # 分析代码实体
            entities = analyzer.analyze_code_entity(content, str(test_file))
            
            if entities:
                print(f"代码分析成功: 提取到 {len(entities)} 个实体")
                
                # 显示前5个实体
                for i, entity in enumerate(entities[:5]):
                    print(f"   {i+1}. {entity.get('name', 'Unknown')} ({entity.get('type', 'Unknown')})")
                
                return True
            else:
                print("代码分析失败: 未提取到实体")
                return False
        else:
            print("深度知识分析器不可用")
            return False
            
    except Exception as e:
        print(f"代码分析失败: {e}")
        return False

def test_llm_client():
    """测试LLM客户端功能"""
    print("\n=== 测试LLM客户端功能 ===")
    
    try:
        from src.ai.llm_client import LLMClient
        import yaml
        
        # 加载配置
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        ai_config = config.get('ai', {})
        llm_client = LLMClient(ai_config)
        
        if llm_client.is_available():
            print("LLM客户端可用")
            
            # 测试简单调用
            response = llm_client.chat_sync(
                system_prompt='你是一个代码分析助手',
                user_prompt='请用一句话介绍Python编程语言'
            )
            
            if response:
                print(f"LLM响应测试成功: {response[:100]}...")
                return True
            else:
                print("LLM响应测试失败: 无响应")
                return False
        else:
            print("LLM客户端不可用")
            return False
            
    except Exception as e:
        print(f"LLM客户端测试失败: {e}")
        return False

def test_semantic_extraction():
    """测试语义提取功能"""
    print("\n=== 测试语义提取功能 ===")
    
    sample_project_path = "output/example/stock-sentiment-analyzer"
    
    if not Path(sample_project_path).exists():
        print(f"样例项目不存在: {sample_project_path}")
        return False
    
    try:
        # 使用现有的语义提取功能
        from src.core.semantic_extractor import extract_semantic_features
        
        print(f"语义提取项目: {sample_project_path}")
        
        # 提取语义特征
        features = extract_semantic_features(sample_project_path)
        
        if features:
            print("语义提取成功")
            print(f"   提取特征数: {len(features)}")
            
            # 显示特征类型统计
            feature_types = {}
            for feature in features:
                feature_type = feature.get('type', 'unknown')
                feature_types[feature_type] = feature_types.get(feature_type, 0) + 1
            
            for feature_type, count in feature_types.items():
                print(f"   {feature_type}: {count}")
            
            return True
        else:
            print("语义提取失败: 未提取到特征")
            return False
            
    except Exception as e:
        print(f"语义提取失败: {e}")
        return False

def main():
    """主测试函数"""
    print("现有功能测试 - 样例工程分析")
    print("=" * 50)
    
    # 测试文件扫描
    file_scan_test = test_file_scanning()
    
    # 测试代码分析
    code_analysis_test = test_code_analysis()
    
    # 测试LLM客户端
    llm_test = test_llm_client()
    
    # 测试语义提取
    semantic_test = test_semantic_extraction()
    
    # 总结测试结果
    print("\n" + "=" * 50)
    print("测试结果总结:")
    print(f"   文件扫描: {'通过' if file_scan_test else '失败'}")
    print(f"   代码分析: {'通过' if code_analysis_test else '失败'}")
    print(f"   LLM客户端: {'通过' if llm_test else '失败'}")
    print(f"   语义提取: {'通过' if semantic_test else '失败'}")
    
    # 总体评估
    total_tests = 4
    passed_tests = sum([file_scan_test, code_analysis_test, llm_test, semantic_test])
    
    print(f"\n总体通过率: {passed_tests}/{total_tests} ({passed_tests/total_tests*100:.1f}%)")
    
    if passed_tests > 0:
        print("现有功能测试完成，部分功能可用")
    else:
        print("所有功能测试失败")

if __name__ == "__main__":
    main()