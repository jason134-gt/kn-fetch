#!/usr/bin/env python3
"""
测试火山引擎LLM API连接
"""
import os
import sys
from openai import OpenAI

# 设置API密钥
api_key = "7c53500f-cf96-485d-bfe2-78db6827f926"
base_url = "https://ark.cn-beijing.volces.com/api/v3"

# 创建客户端
client = OpenAI(
    base_url=base_url,
    api_key=api_key
)

def test_chat_completions():
    """测试chat.completions接口"""
    print("=== 测试 chat.completions 接口 ===")
    try:
        response = client.chat.completions.create(
            model="deepseek-v3-1-terminus",
            messages=[
                {"role": "user", "content": "你好，请简单介绍一下你自己"}
            ],
            max_tokens=200,
            temperature=0.1
        )
        
        content = response.choices[0].message.content
        print("[SUCCESS] 连接成功！")
        # 处理可能的编码问题
        try:
            print(f"响应内容: {content}")
        except UnicodeEncodeError:
            # 如果包含特殊字符，使用编码处理
            safe_content = content.encode('utf-8', errors='ignore').decode('utf-8')
            print(f"响应内容: {safe_content}")
        return True
        
    except Exception as e:
        print(f"[FAILED] 连接失败: {e}")
        return False

def test_responses_api():
    """测试responses API接口（火山引擎特有）"""
    print("\n=== 测试 responses API 接口 ===")
    try:
        tools = [{
            "type": "web_search",
            "max_keyword": 2,
        }]
        
        response = client.responses.create(
            model="deepseek-v3-1-terminus",
            input=[{"role": "user", "content": "北京的天气怎么样？"}],
            tools=tools,
        )
        
        print("[SUCCESS] responses API 连接成功！")
        print(f"响应对象: {response}")
        if hasattr(response, 'output') and response.output:
            for output in response.output:
                if hasattr(output, 'content') and output.content:
                    for content_item in output.content:
                        if hasattr(content_item, 'text'):
                            print(f"响应内容: {content_item.text}")
        return True
        
    except Exception as e:
        print(f"[FAILED] responses API 连接失败: {e}")
        return False

def test_llm_client():
    """测试项目中的LLM客户端"""
    print("\n=== 测试项目LLM客户端 ===")
    try:
        # 导入项目配置
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        
        # 读取配置
        import yaml
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        ai_config = config.get('ai', {})
        
        # 创建LLM客户端
        from src.ai.llm_client import LLMClient
        llm_client = LLMClient(ai_config)
        
        print(f"客户端配置: {llm_client.get_config_info()}")
        
        if llm_client.is_available():
            print("[SUCCESS] LLM客户端初始化成功")
            
            # 测试同步调用
            response = llm_client.chat_sync(
                system_prompt="你是一个有帮助的AI助手",
                user_prompt="简单介绍一下你的功能"
            )
            
            if response:
                print("[SUCCESS] LLM调用成功")
                print(f"响应内容: {response[:200]}...")
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

if __name__ == "__main__":
    print("开始测试火山引擎LLM API连接...")
    print(f"API Key: {api_key[:10]}...{api_key[-4:]}")
    print(f"Base URL: {base_url}")
    
    # 执行测试
    result1 = test_chat_completions()
    result2 = test_responses_api()
    result3 = test_llm_client()
    
    print(f"\n=== 测试结果汇总 ===")
    print(f"chat.completions 接口: {'[PASS] 通过' if result1 else '[FAIL] 失败'}")
    print(f"responses API 接口: {'[PASS] 通过' if result2 else '[FAIL] 失败'}")
    print(f"项目LLM客户端: {'[PASS] 通过' if result3 else '[FAIL] 失败'}")
    
    if result1 and result2 and result3:
        print("\n[SUCCESS] 所有测试通过！火山引擎LLM API配置正确")
    else:
        print("\n[WARNING] 部分测试失败，请检查配置")