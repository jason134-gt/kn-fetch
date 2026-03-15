"""
LangChain 配置和工具类

提供 LangChain 集成的统一配置和工具函数，支持多 LLM 提供商。
"""

import os
from typing import Optional, Dict, Any
from langchain_openai import ChatOpenAI
from langchain_core.language_models.base import BaseLanguageModel

# 可选导入 - 仅在需要时加载
try:
    from langchain_anthropic import ChatAnthropic
except ImportError:
    ChatAnthropic = None

try:
    from langchain_community.chat_models import QianfanChatEndpoint
except ImportError:
    QianfanChatEndpoint = None


class LangChainConfig:
    """LangChain 配置类"""
    
    # 提供商默认配置
    PROVIDER_DEFAULTS = {
        "volcengine": {
            "base_url": "https://ark.cn-beijing.volces.com/api/v3",
            "model": "deepseek-v3-2-251201",
            "env_key": "ARK_API_KEY",
            "temperature": 0.1,
            "max_tokens": 4000
        },
        "openai": {
            "base_url": "https://api.openai.com/v1",
            "model": "gpt-4o",
            "env_key": "OPENAI_API_KEY",
            "temperature": 0.1,
            "max_tokens": 4000
        },
        "anthropic": {
            "base_url": "https://api.anthropic.com/v1",
            "model": "claude-3-opus-20240229",
            "env_key": "ANTHROPIC_API_KEY",
            "temperature": 0.1,
            "max_tokens": 4000
        },
        "qwen": {
            "env_key": "DASHSCOPE_API_KEY",
            "model": "qwen-max",
            "temperature": 0.1,
            "max_tokens": 4000
        }
    }
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        """
        初始化配置
        
        Args:
            config: AI 配置字典，从配置文件加载
        """
        self.config = config or {}
        self.provider = self.config.get("provider", "volcengine")
        self.provider_config = self.config.get(self.provider, {})
        
        # 合并默认配置
        defaults = self.PROVIDER_DEFAULTS.get(self.provider, {})
        for key, value in defaults.items():
            if key not in self.provider_config:
                self.provider_config[key] = value
    
    def get_api_key(self) -> Optional[str]:
        """获取 API Key"""
        env_key = self.provider_config.get("env_key")
        if env_key:
            # 先从环境变量获取
            api_key = os.getenv(env_key)
            if not api_key:
                # 再从配置文件获取
                api_key = self.provider_config.get("api_key")
            return api_key
        return None
    
    def get_base_url(self) -> Optional[str]:
        """获取 Base URL"""
        return self.provider_config.get("base_url")
    
    def get_model(self) -> str:
        """获取模型名称"""
        return self.provider_config.get("model", "gpt-3.5-turbo")
    
    def get_temperature(self) -> float:
        """获取温度参数"""
        return self.provider_config.get("temperature", 0.1)
    
    def get_max_tokens(self) -> int:
        """获取最大 Token 数"""
        return self.provider_config.get("max_tokens", 4000)
    
    def is_available(self) -> bool:
        """检查配置是否可用"""
        return bool(self.get_api_key())


def create_langchain_llm(config: LangChainConfig) -> BaseLanguageModel:
    """
    创建 LangChain LLM 实例
    
    Args:
        config: LangChain 配置实例
        
    Returns:
        LangChain LLM 实例
        
    Raises:
        ValueError: 当提供商不支持或配置无效时
    """
    provider = config.provider
    api_key = config.get_api_key()
    
    if not api_key:
        raise ValueError(f"无法获取 API Key，提供商: {provider}")
    
    common_params = {
        "model": config.get_model(),
        "temperature": config.get_temperature(),
        "max_tokens": config.get_max_tokens(),
    }
    
    if provider == "volcengine":
        # 火山引擎（兼容 OpenAI API）
        base_url = config.get_base_url()
        return ChatOpenAI(
            api_key=api_key,
            base_url=base_url,
            **common_params
        )
    
    elif provider == "openai":
        # OpenAI
        return ChatOpenAI(
            api_key=api_key,
            **common_params
        )
    
    elif provider == "anthropic":
        # Anthropic (Claude)
        if ChatAnthropic is None:
            raise ImportError("langchain-anthropic 未安装，请运行: pip install langchain-anthropic")
        return ChatAnthropic(
            api_key=api_key,
            **common_params
        )
    
    elif provider == "qwen":
        # 通义千问
        if QianfanChatEndpoint is None:
            raise ImportError("langchain-community 未安装，请运行: pip install langchain-community")
        return QianfanChatEndpoint(
            qianfan_ak=api_key,
            model_name=common_params["model"],
            temperature=common_params["temperature"]
        )
    
    else:
        raise ValueError(f"不支持的提供商: {provider}")


def create_langchain_llm_from_dict(config_dict: Dict[str, Any]) -> BaseLanguageModel:
    """
    从配置字典创建 LangChain LLM 实例
    
    Args:
        config_dict: 配置字典
        
    Returns:
        LangChain LLM 实例
    """
    config = LangChainConfig(config_dict)
    return create_langchain_llm(config)


class LangChainLLMWrapper:
    """
    LangChain LLM 包装器，提供简化的调用接口
    """
    
    def __init__(self, config: LangChainConfig):
        """
        初始化包装器
        
        Args:
            config: LangChain 配置实例
        """
        self.config = config
        self.llm = create_langchain_llm(config)
    
    def invoke(self, prompt: str, **kwargs) -> str:
        """
        同步调用 LLM
        
        Args:
            prompt: 提示词
            **kwargs: 额外参数
            
        Returns:
            LLM 响应内容
        """
        response = self.llm.invoke(prompt, **kwargs)
        return response.content
    
    async def ainvoke(self, prompt: str, **kwargs) -> str:
        """
        异步调用 LLM
        
        Args:
            prompt: 提示词
            **kwargs: 额外参数
            
        Returns:
            LLM 响应内容
        """
        response = await self.llm.ainvoke(prompt, **kwargs)
        return response.content
    
    def is_available(self) -> bool:
        """检查 LLM 是否可用"""
        return self.config.is_available()
    
    def get_config(self) -> LangChainConfig:
        """获取配置"""
        return self.config


__all__ = [
    "LangChainConfig",
    "create_langchain_llm",
    "create_langchain_llm_from_dict",
    "LangChainLLMWrapper",
]
