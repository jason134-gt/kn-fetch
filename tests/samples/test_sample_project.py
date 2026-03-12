#!/usr/bin/env python3
"""
样例工程测试脚本 - 确保API密钥正确加载
"""
import os
import sys
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 确保API密钥正确加载
print("=== 设置环境变量 ===")
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

print("环境变量检查:")
print(f"ARK_API_KEY 存在: {'ARK_API_KEY' in os.environ}")
if 'ARK_API_KEY' in os.environ:
    api_key = os.environ['ARK_API_KEY']
    print(f"API密钥长度: {len(api_key)}")
    print(f"API密钥前10位: {api_key[:10]}...")

# 测试LLM客户端
print("\n=== LLM客户端测试 ===")
from src.ai.llm_client import LLMClient
import yaml

with open('config/config.yaml', 'r', encoding='utf-8') as f:
    config = yaml.safe_load(f)

ai_config = config.get('ai', {})
llm_client = LLMClient(ai_config)
print(f"LLM客户端可用性: {llm_client.is_available()}")
config_info = llm_client.get_config_info()
print("配置信息:")
for key, value in config_info.items():
    print(f"  {key}: {value}")

# 测试简单调用
if llm_client.is_available():
    print("\n=== LLM调用测试 ===")
    response = llm_client.chat_sync(
        system_prompt='你是一个测试助手',
        user_prompt='请用一句话回答：你好吗？'
    )
    print(f"LLM响应: {response}")
    
    # 测试深度知识分析器
    print("\n=== 深度知识分析器测试 ===")
    from src.ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer
    
    analyzer = DeepKnowledgeAnalyzer(config, "test_output")
    print(f"深度分析器可用性: {analyzer.is_available()}")
    
    if analyzer.is_available():
        test_code = """
def calculate_sum(numbers):
    '''计算数字列表的总和'''
    return sum(numbers)

class Calculator:
    '''简单的计算器类'''
    def __init__(self):
        self.result = 0
    
    def add(self, x, y):
        '''加法运算'''
        return x + y
"""
        entities = analyzer.analyze_code_entity(test_code, "test_calculator.py")
        print(f"分析成功，提取到 {len(entities)} 个实体")
        for entity in entities[:3]:
            print(f"  - {entity.get('name', 'Unknown')}: {entity.get('type', 'Unknown')}")

# 运行样例工程分析
print("\n=== 运行样例工程分析 ===")
sample_project_path = "output/example/stock-sentiment-analyzer"

if Path(sample_project_path).exists():
    print(f"开始分析样例工程: {sample_project_path}")
    
    # 导入并运行分析
    from src.cli.cli import main
    
    # 模拟命令行参数
    sys.argv = ['kn-fetch.py', 'analyze', sample_project_path, '--deep', '--config', 'config/config.yaml']
    
    print("启动知识提取程序...")
    try:
        main()
        print("\n✅ 样例工程分析完成！")
    except Exception as e:
        print(f"❌ 分析过程中出现错误: {e}")
        import traceback
        traceback.print_exc()
else:
    print(f"❌ 样例工程路径不存在: {sample_project_path}")

print("\n=== 测试完成 ===")