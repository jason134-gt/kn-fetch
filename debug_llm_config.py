#!/usr/bin/env python3
"""
调试LLM配置问题
"""
import os
import sys

# 加载环境变量
try:
    with open('.env', 'r', encoding='utf-8') as f:
        for line in f:
            if '=' in line and not line.startswith('#'):
                key, value = line.strip().split('=', 1)
                os.environ[key] = value
                print(f"设置环境变量: {key} = {'*' * len(value)}")
except FileNotFoundError:
    print("未找到.env文件")

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

print("=== 调试LLM配置 ===")

# 读取配置
import yaml
with open('config/config.yaml', 'r', encoding='utf-8') as f:
    config = yaml.safe_load(f)

ai_config = config.get('ai', {})
print(f"AI配置: {ai_config}")

# 检查API密钥
if 'ARK_API_KEY' in os.environ:
    print("[SUCCESS] ARK_API_KEY环境变量存在")
else:
    print("[FAILED] ARK_API_KEY环境变量不存在")

# 测试LLM客户端
from src.ai.llm_client import LLMClient

print("\n=== 测试LLM客户端 ===")
llm_client = LLMClient(ai_config)

print(f"LLM客户端状态: {llm_client.is_available()}")
print(f"API密钥设置: {bool(llm_client.api_key)}")
print(f"模型: {llm_client.model}")
print(f"Base URL: {llm_client.base_url}")

# 测试实际调用
if llm_client.is_available():
    print("\n=== 测试实际调用 ===")
    try:
        response = llm_client.chat_sync(
            system_prompt="你是一个AI助手",
            user_prompt="请用一句话介绍自己"
        )
        print(f"[SUCCESS] 调用成功: {response}")
    except Exception as e:
        print(f"❌ 调用失败: {e}")
        import traceback
        traceback.print_exc()
else:
    print("❌ LLM客户端不可用")
    
    # 调试具体原因
    print(f"enabled: {llm_client.enabled}")
    print(f"client: {llm_client.client}")
    print(f"sync_client: {llm_client.sync_client}")
    print(f"api_key: {bool(llm_client.api_key)}")

print("\n=== 测试深度知识分析器 ===")
from src.ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer

try:
    # 确保配置正确
    test_ai_config = ai_config.copy()
    
    # 如果provider配置为空，设置默认值
    if not test_ai_config.get('provider'):
        test_ai_config['provider'] = 'volcengine'
    
    if not test_ai_config.get('volcengine'):
        test_ai_config['volcengine'] = {
            'api_key': '',
            'base_url': 'https://ark.cn-beijing.volces.com/api/v3',
            'model': 'deepseek-v3-1-terminus'
        }
    
    analyzer = DeepKnowledgeAnalyzer(test_ai_config, "test_output")
    
    print(f"深度分析器可用性: {analyzer.is_available()}")
    print(f"LLM客户端可用性: {analyzer.llm.is_available()}")
    
    if analyzer.is_available():
        print("✅ 深度分析器可用")
        
        # 测试分析
        test_code = """
def test_function():
    '''测试函数'''
    return "Hello World"
"""
        
        entities = analyzer.analyze_code_entity(test_code, "test.py")
        print(f"分析结果: {len(entities)} 个实体")
        
    else:
        print("❌ 深度分析器不可用")
        
        # 进一步调试
        print(f"分析器LLM客户端状态: {analyzer.llm.is_available()}")
        print(f"分析器LLM客户端类型: {type(analyzer.llm)}")
        
        # 检查LLM客户端内部状态
        llm = analyzer.llm
        print(f"LLM enabled: {llm.enabled}")
        print(f"LLM client: {llm.client}")
        print(f"LLM sync_client: {llm.sync_client}")
        print(f"LLM api_key: {bool(llm.api_key)}")
        
        
except Exception as e:
    print(f"❌ 深度分析器初始化失败: {e}")
    import traceback
    traceback.print_exc()

print("\n=== 调试完成 ===")