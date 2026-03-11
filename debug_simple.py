#!/usr/bin/env python3
"""
简单调试脚本
"""
import os
import sys

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

print("=== 简单调试 ===")

# 读取配置
import yaml
with open('config/config.yaml', 'r', encoding='utf-8') as f:
    config = yaml.safe_load(f)

ai_config = config.get('ai', {})
print(f"AI配置: {ai_config}")

# 检查环境变量
print(f"ARK_API_KEY in env: {'ARK_API_KEY' in os.environ}")

# 测试LLM客户端
from src.ai.llm_client import LLMClient

print("\n=== 测试LLM客户端 ===")
llm_client = LLMClient(ai_config)

print(f"LLM客户端状态: {llm_client.is_available()}")
print(f"API密钥设置: {bool(llm_client.api_key)}")

# 检查深度分析器
from src.ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer

print("\n=== 测试深度分析器 ===")

try:
    analyzer = DeepKnowledgeAnalyzer(ai_config, "test_output")
    print(f"深度分析器可用性: {analyzer.is_available()}")
    print(f"LLM客户端可用性: {analyzer.llm.is_available()}")
    
    # 检查LLM客户端内部状态
    llm = analyzer.llm
    print(f"LLM enabled: {llm.enabled}")
    print(f"LLM client: {llm.client}")
    print(f"LLM sync_client: {llm.sync_client}")
    print(f"LLM api_key: {bool(llm.api_key)}")
    
except Exception as e:
    print(f"错误: {e}")
    import traceback
    traceback.print_exc()

print("\n=== 调试完成 ===")