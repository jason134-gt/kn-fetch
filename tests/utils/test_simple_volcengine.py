#!/usr/bin/env python3
"""
简化版火山引擎LLM API测试
"""
import os
import sys

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def test_llm_client():
    """测试项目LLM客户端"""
    print("=== 测试项目LLM客户端 ===")
    
    try:
        # 读取配置
        import yaml
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        ai_config = config.get('ai', {})
        
        # 创建LLM客户端
        from src.ai.llm_client import LLMClient
        llm_client = LLMClient(ai_config)
        
        print("客户端配置信息:")
        config_info = llm_client.get_config_info()
        for key, value in config_info.items():
            print(f"  {key}: {value}")
        
        if llm_client.is_available():
            print("[SUCCESS] LLM客户端初始化成功")
            
            # 测试一个简单的调用（不使用工具）
            response = llm_client.chat_sync(
                system_prompt="你是一个代码分析助手",
                user_prompt="请用一句话介绍Python编程语言"
            )
            
            if response:
                print("[SUCCESS] LLM调用成功")
                print(f"响应内容: {response}")
                return True
            else:
                print("[FAILED] LLM调用返回空响应")
                return False
        else:
            print("[FAILED] LLM客户端不可用")
            return False
            
    except Exception as e:
        print(f"[FAILED] LLM客户端测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_deep_analysis():
    """测试深度分析功能"""
    print("\n=== 测试深度分析功能 ===")
    
    try:
        from src.ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer
        
        # 创建分析器
        analyzer = DeepKnowledgeAnalyzer()
        
        # 测试一个简单的代码分析
        test_code = """
def calculate_sum(numbers):
    '''计算数字列表的总和'''
    return sum(numbers)

class Calculator:
    '''简单的计算器类'''
    def __init__(self):
        self.result = 0
    
    def add(self, x, y):
        return x + y
"""
        
        analysis_result = analyzer.analyze_code_entity(test_code, "test.py")
        
        if analysis_result:
            print("[SUCCESS] 深度分析功能正常")
            print(f"分析结果包含 {len(analysis_result)} 个实体")
            return True
        else:
            print("[FAILED] 深度分析返回空结果")
            return False
            
    except Exception as e:
        print(f"[FAILED] 深度分析测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("开始简化版火山引擎LLM测试...")
    
    # 执行测试
    result1 = test_llm_client()
    result2 = test_deep_analysis()
    
    print(f"\n=== 测试结果汇总 ===")
    print(f"LLM客户端: {'[PASS] 通过' if result1 else '[FAIL] 失败'}")
    print(f"深度分析: {'[PASS] 通过' if result2 else '[FAIL] 失败'}")
    
    if result1 and result2:
        print("\n[SUCCESS] 所有核心功能测试通过！火山引擎LLM配置正确")
        print("可以正常进行代码知识提取和深度分析")
    else:
        print("\n[WARNING] 部分功能测试失败，可能需要调整配置")