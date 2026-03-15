"""
LLM客户端 - 统一的大模型调用接口
支持OpenAI兼容接口，包括火山引擎等
"""
import logging
import asyncio
import os
from typing import Optional, Dict, Any, List
from openai import AsyncOpenAI, OpenAI

logger = logging.getLogger(__name__)


class LLMClient:
    """LLM客户端，支持OpenAI兼容接口"""

    # 支持的提供商默认配置
    PROVIDER_DEFAULTS = {
        "volcengine": {
            "base_url": "https://ark.cn-beijing.volces.com/api/v3",
            "model": "deepseek-v3-1-terminus",
            "env_key": "ARK_API_KEY"
        },
        "openai": {
            "base_url": "https://api.openai.com/v1",
            "model": "gpt-4o",
            "env_key": "OPENAI_API_KEY"
        },
        "anthropic": {
            "base_url": "https://api.anthropic.com/v1",
            "model": "claude-3-opus-20240229",
            "env_key": "ANTHROPIC_API_KEY"
        }
    }

    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.provider = config.get('provider', 'openai')

        # 获取提供商特定配置
        provider_config = self._get_provider_config()

        # API配置 - 优先使用provider特定配置，其次使用通用配置，最后使用环境变量
        self.api_key = provider_config.get('api_key') or config.get('api_key') or self._get_env_api_key()
        self.base_url = provider_config.get('base_url') or config.get('base_url') or self._get_default_base_url()
        self.model = provider_config.get('model') or config.get('model') or self._get_default_model()
        self.timeout = config.get('timeout', 60)
        self.max_retries = config.get('max_retries', 3)

        # 工具配置
        self.tools_config = provider_config.get('tools', {})
        self.tools_enabled = self.tools_config.get('enabled', False)
        self.web_search_config = self.tools_config.get('web_search', {})

        # 客户端状态
        self.enabled = bool(self.api_key)
        self.client: Optional[AsyncOpenAI] = None
        self.sync_client: Optional[OpenAI] = None

        if self.enabled:
            try:
                self.client = AsyncOpenAI(
                    api_key=self.api_key,
                    base_url=self.base_url,
                    timeout=self.timeout
                )
                self.sync_client = OpenAI(
                    api_key=self.api_key,
                    base_url=self.base_url,
                    timeout=self.timeout
                )
                logger.info(f"LLM客户端初始化成功 - Provider: {self.provider}, Model: {self.model}")
            except Exception as e:
                logger.error(f"LLM客户端初始化失败: {e}", exc_info=True)
                self.enabled = False

    def _get_provider_config(self) -> Dict[str, Any]:
        """获取当前提供商的特定配置"""
        provider_config = self.config.get(self.provider, {})
        return provider_config if isinstance(provider_config, dict) else {}

    def _get_env_api_key(self) -> str:
        """从环境变量获取API Key"""
        provider_info = self.PROVIDER_DEFAULTS.get(self.provider, {})
        env_key = provider_info.get('env_key', f'{self.provider.upper()}_API_KEY')
        return os.getenv(env_key, '')

    def _get_default_base_url(self) -> str:
        """获取默认Base URL"""
        provider_info = self.PROVIDER_DEFAULTS.get(self.provider, {})
        return provider_info.get('base_url', 'https://api.openai.com/v1')

    def _get_default_model(self) -> str:
        """获取默认模型"""
        provider_info = self.PROVIDER_DEFAULTS.get(self.provider, {})
        return provider_info.get('model', 'gpt-3.5-turbo')

    def _build_tools(self) -> Optional[List[Dict[str, Any]]]:
        """构建工具配置"""
        if not self.tools_enabled:
            return None

        tools = []
        if self.web_search_config.get('enabled', False):
            tools.append({
                "type": "web_search",
                "max_keyword": self.web_search_config.get('max_keyword', 2)
            })

        return tools if tools else None

    def is_available(self) -> bool:
        """检查LLM是否可用"""
        return self.enabled and self.client is not None

    async def chat(self, system_prompt: str, user_prompt: str, **kwargs) -> str:
        """
        调用LLM聊天接口

        Args:
            system_prompt: 系统提示词
            user_prompt: 用户提示词
            **kwargs: 额外参数
                - model: 模型名称
                - temperature: 温度参数
                - max_tokens: 最大token数
                - tools: 工具配置(可选)
                - use_tools: 是否使用配置的工具(默认False)

        Returns:
            模型响应文本
        """
        if not self.is_available():
            logger.warning("LLM不可用，无法调用")
            return ""

        model = kwargs.get('model', self.model)
        temperature = kwargs.get('temperature', 0.1)
        max_tokens = kwargs.get('max_tokens', 2000)

        # 工具配置
        use_tools = kwargs.get('use_tools', False)
        tools = kwargs.get('tools') or (self._build_tools() if use_tools else None)

        for attempt in range(self.max_retries):
            try:
                params = {
                    "model": model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt}
                    ],
                    "temperature": temperature,
                    "max_tokens": max_tokens
                }

                # 如果有工具配置，添加到参数中
                if tools:
                    params["tools"] = tools

                response = await self.client.chat.completions.create(**params)

                # 处理响应
                choice = response.choices[0]
                content = choice.message.content or ""

                return content.strip()

            except Exception as e:
                logger.warning(f"LLM调用失败 (尝试 {attempt+1}/{self.max_retries}): {e}")
                if attempt < self.max_retries - 1:
                    await asyncio.sleep(2 ** attempt)  # 指数退避
                else:
                    logger.error(f"LLM调用最终失败: {e}", exc_info=True)
                    return ""

        return ""

    def chat_sync(self, system_prompt: str, user_prompt: str, **kwargs) -> str:
        """
        同步调用LLM聊天接口

        Args:
            system_prompt: 系统提示词
            user_prompt: 用户提示词
            **kwargs: 额外参数

        Returns:
            模型响应文本
        """
        if not self.enabled or not self.sync_client:
            logger.warning("LLM不可用，无法调用")
            return ""

        model = kwargs.get('model', self.model)
        temperature = kwargs.get('temperature', 0.1)
        max_tokens = kwargs.get('max_tokens', 2000)

        # 工具配置
        use_tools = kwargs.get('use_tools', False)
        tools = kwargs.get('tools') or (self._build_tools() if use_tools else None)

        for attempt in range(self.max_retries):
            try:
                params = {
                    "model": model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt}
                    ],
                    "temperature": temperature,
                    "max_tokens": max_tokens
                }

                if tools:
                    params["tools"] = tools

                response = self.sync_client.chat.completions.create(**params)
                choice = response.choices[0]
                content = choice.message.content or ""

                return content.strip()

            except Exception as e:
                logger.warning(f"LLM同步调用失败 (尝试 {attempt+1}/{self.max_retries}): {e}")
                if attempt < self.max_retries - 1:
                    import time
                    time.sleep(2 ** attempt)
                else:
                    logger.error(f"LLM同步调用最终失败: {e}", exc_info=True)
                    return ""

        return ""

    async def chat_with_responses(self, user_prompt: str, system_prompt: str = "", **kwargs) -> Dict[str, Any]:
        """
        使用responses API调用(火山引擎支持)

        Args:
            user_prompt: 用户提示词
            system_prompt: 系统提示词(可选)
            **kwargs: 额外参数

        Returns:
            完整响应对象
        """
        if not self.enabled or not self.sync_client:
            logger.warning("LLM不可用，无法调用")
            return {}

        model = kwargs.get('model', self.model)

        # 工具配置
        use_tools = kwargs.get('use_tools', False)
        tools = kwargs.get('tools') or (self._build_tools() if use_tools else None)

        try:
            # 构建输入消息
            input_messages = []
            if system_prompt:
                input_messages.append({"role": "system", "content": system_prompt})
            input_messages.append({"role": "user", "content": user_prompt})

            params = {
                "model": model,
                "input": input_messages
            }

            if tools:
                params["tools"] = tools

            # 使用responses.create (火山引擎API)
            response = self.sync_client.responses.create(**params)

            return {
                "success": True,
                "response": response,
                "content": str(response) if hasattr(response, '__str__') else ""
            }

        except Exception as e:
            logger.error(f"LLM responses调用失败: {e}", exc_info=True)
            return {
                "success": False,
                "error": str(e)
            }

    def get_config_info(self) -> Dict[str, Any]:
        """获取当前配置信息(用于调试/显示)"""
        return {
            "provider": self.provider,
            "model": self.model,
            "base_url": self.base_url,
            "api_key_set": bool(self.api_key),
            "tools_enabled": self.tools_enabled,
            "available": self.is_available()
        }
