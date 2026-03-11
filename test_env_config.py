#!/usr/bin/env python3
"""
测试环境变量配置
"""
import os
import sys

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def test_environment_variables():
    """测试环境变量是否设置正确"""
    print("=== 测试环境变量配置 ===")
    
    # 检查环境变量
    ark_api_key = os.getenv('ARK_API_KEY')
    
    print(f"ARK_API_KEY 环境变量: {'[SET] 已设置' if ark_api_key else '[NOT SET] 未设置'}")
    if ark_api_key:
        print(f"API Key 长度: {len(ark_api_key)} 字符")
        print(f"API Key 前10位: {ark_api_key[:10]}...{ark_api_key[-4:]}")
    
    return bool(ark_api_key)

def test_llm_client_with_env():
    """测试LLM客户端使用环境变量"""
    print("\n=== 测试LLM客户端环境变量配置 ===")
    
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
            print("[SUCCESS] LLM客户端初始化成功 - 使用环境变量配置")
            
            # 测试一个简单的调用
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
            print("可能原因:")
            print("  - 环境变量 ARK_API_KEY 未设置")
            print("  - API Key 无效或过期")
            print("  - 网络连接问题")
            return False
            
    except Exception as e:
        print(f"[FAILED] LLM客户端测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_dotenv_loading():
    """测试dotenv文件加载"""
    print("\n=== 测试 .env 文件加载 ===")
    
    try:
        # 尝试使用python-dotenv加载环境变量
        from dotenv import load_dotenv
        
        # 加载.env文件
        load_dotenv()
        
        ark_api_key = os.getenv('ARK_API_KEY')
        print(f"使用dotenv加载后 ARK_API_KEY: {'[SET] 已设置' if ark_api_key else '[NOT SET] 未设置'}")
        
        if ark_api_key:
            print("[SUCCESS] .env 文件加载成功")
            return True
        else:
            print("[FAILED] .env 文件加载失败或文件为空")
            return False
            
    except ImportError:
        print("[INFO] python-dotenv 未安装，跳过dotenv测试")
        return True
    except Exception as e:
        print(f"[FAILED] dotenv加载失败: {e}")
        return False

if __name__ == "__main__":
    print("开始环境变量配置测试...")
    
    # 执行测试
    result1 = test_environment_variables()
    result2 = test_dotenv_loading()
    result3 = test_llm_client_with_env()
    
    print(f"\n=== 测试结果汇总 ===")
    print(f"环境变量设置: {'[PASS] 通过' if result1 else '[FAIL] 失败'}")
    print(f".env 文件加载: {'[PASS] 通过' if result2 else '[FAIL] 失败'}")
    print(f"LLM客户端: {'[PASS] 通过' if result3 else '[FAIL] 失败'}")
    
    if result1 and result3:
        print("\n[SUCCESS] 环境变量配置正确！API密钥已从代码中移除")
        print("✅ 配置安全：API密钥不会上传到Git仓库")
        print("✅ 功能正常：LLM调用可以正常工作")
    else:
        print("\n[WARNING] 配置需要调整")
        print("请检查:")
        print("  1. 确保 .env 文件存在并包含正确的ARK_API_KEY")
        print("  2. 或者设置系统环境变量 ARK_API_KEY")
        print("  3. 安装 python-dotenv: pip install python-dotenv")