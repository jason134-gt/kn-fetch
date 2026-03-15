import os

# 测试环境变量
print("当前环境变量:")
print(f"ARK_API_KEY: {os.getenv('ARK_API_KEY', '未设置')}")
print(f"OPENAI_API_KEY: {os.getenv('OPENAI_API_KEY', '未设置')}")

# 测试KN-Fetch的API Key读取逻辑
from src.ai.llm_client import LLMClient
from src.infrastructure.config_manager import ConfigManager

# 测试配置管理器
config_manager = ConfigManager()
config = config_manager.get_config()

print("\n配置中的AI设置:")
print(f"Provider: {config.get('ai', {}).get('provider', '未设置')}")
print(f"API Key: {config.get('ai', {}).get('api_key', '未设置')}")

# 测试LLM客户端
try:
    llm_client = LLMClient(config.get('ai', {}))
    print(f"\nLLM客户端API Key: {llm_client.api_key}")
    print(f"LLM可用性: {llm_client.is_available()}")
except Exception as e:
    print(f"LLM客户端测试失败: {e}")