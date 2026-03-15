"""
LLM客户端抽象基类 - 基于design-v1方案实现标准化的LLM客户端
"""

from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional, Union
from enum import Enum
import httpx
from loguru import logger


class LLMProvider(str, Enum):
    """LLM提供商"""
    VOLCENGINE = "volcengine"
    OPENAI = "openai"
    AZURE_OPENAI = "azure_openai"
    ANTHROPIC = "anthropic"
    LOCAL = "local"


class LLMBaseClient(ABC):
    """LLM客户端抽象基类"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.provider: LLMProvider = LLMProvider(config.get("provider", "volcengine"))
        self.base_url: str = config.get("base_url", "")
        self.api_key: str = config.get("api_key", "")
        self.model: str = config.get("model", "")
        self.timeout: int = config.get("timeout", 60)
        self.max_retries: int = config.get("max_retries", 3)
        self._client: Optional[httpx.AsyncClient] = None
        self._available = False
        
    @abstractmethod
    async def initialize(self) -> bool:
        """初始化客户端"""
        pass
    
    @abstractmethod
    async def chat(self, messages: List[Dict[str, str]], **kwargs) -> str:
        """聊天对话"""
        pass
    
    @abstractmethod
    async def chat_structured(self, messages: List[Dict[str, str]], 
                            response_format: Dict[str, Any], **kwargs) -> Dict[str, Any]:
        """结构化输出聊天"""
        pass
    
    @abstractmethod
    async def embed(self, texts: List[str]) -> List[List[float]]:
        """文本嵌入"""
        pass
    
    @property
    def is_available(self) -> bool:
        """检查客户端是否可用"""
        return self._available
    
    async def health_check(self) -> bool:
        """健康检查"""
        try:
            return await self.initialize()
        except Exception as e:
            logger.error(f"LLM健康检查失败: {e}")
            return False
    
    def get_config_info(self) -> Dict[str, Any]:
        """获取配置信息"""
        return {
            "provider": self.provider.value,
            "model": self.model,
            "base_url": self.base_url,
            "timeout": self.timeout,
            "max_retries": self.max_retries,
            "available": self._available
        }
    
    async def close(self):
        """关闭客户端"""
        if self._client:
            await self._client.aclose()
            self._client = None


class LLMClient(LLMBaseClient):
    """LLM客户端具体实现"""
    
    async def initialize(self) -> bool:
        """初始化客户端"""
        try:
            if not self.api_key:
                logger.warning("LLM API密钥未配置")
                self._available = False
                return False
            
            # 创建HTTP客户端
            self._client = httpx.AsyncClient(
                base_url=self.base_url,
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json"
                },
                timeout=self.timeout
            )
            
            # 测试连接
            if self.provider == LLMProvider.VOLCENGINE:
                # 火山引擎特定的测试
                test_response = await self._test_volcengine_connection()
            else:
                # 通用测试
                test_response = await self._test_generic_connection()
            
            self._available = test_response
            logger.info(f"LLM客户端初始化成功: {self.provider.value}")
            return self._available
            
        except Exception as e:
            logger.error(f"LLM客户端初始化失败: {e}")
            self._available = False
            return False
    
    async def _test_volcengine_connection(self) -> bool:
        """测试火山引擎连接"""
        try:
            # 火山引擎的简单测试请求
            response = await self._client.post(
                "/api/v3/chat/completions",
                json={
                    "model": self.model,
                    "messages": [{"role": "user", "content": "test"}],
                    "max_tokens": 10
                }
            )
            return response.status_code == 200
        except Exception as e:
            logger.debug(f"火山引擎连接测试失败: {e}")
            return False
    
    async def _test_generic_connection(self) -> bool:
        """测试通用连接"""
        try:
            # 通用的模型列表查询
            response = await self._client.get("/v1/models")
            return response.status_code == 200
        except Exception:
            # 如果模型列表查询失败，尝试简单聊天
            try:
                response = await self._client.post(
                    "/v1/chat/completions",
                    json={
                        "model": self.model,
                        "messages": [{"role": "user", "content": "test"}],
                        "max_tokens": 10
                    }
                )
                return response.status_code == 200
            except Exception as e:
                logger.debug(f"通用连接测试失败: {e}")
                return False
    
    async def chat(self, messages: List[Dict[str, str]], **kwargs) -> str:
        """聊天对话"""
        if not self._available:
            raise RuntimeError("LLM客户端不可用")
        
        try:
            # 根据提供商构建请求
            if self.provider == LLMProvider.VOLCENGINE:
                request_data = self._build_volcengine_request(messages, **kwargs)
                endpoint = "/api/v3/chat/completions"
            else:
                request_data = self._build_openai_request(messages, **kwargs)
                endpoint = "/v1/chat/completions"
            
            response = await self._client.post(endpoint, json=request_data)
            response.raise_for_status()
            
            result = response.json()
            
            if self.provider == LLMProvider.VOLCENGINE:
                return result.get("choices", [{}])[0].get("message", {}).get("content", "")
            else:
                return result.get("choices", [{}])[0].get("message", {}).get("content", "")
                
        except Exception as e:
            logger.error(f"LLM聊天请求失败: {e}")
            raise
    
    async def chat_structured(self, messages: List[Dict[str, str]], 
                            response_format: Dict[str, Any], **kwargs) -> Dict[str, Any]:
        """结构化输出聊天"""
        if not self._available:
            raise RuntimeError("LLM客户端不可用")
        
        try:
            # 构建结构化请求
            if self.provider == LLMProvider.VOLCENGINE:
                request_data = self._build_volcengine_request(messages, **kwargs)
                request_data["response_format"] = response_format
                endpoint = "/api/v3/chat/completions"
            else:
                request_data = self._build_openai_request(messages, **kwargs)
                request_data["response_format"] = response_format
                endpoint = "/v1/chat/completions"
            
            response = await self._client.post(endpoint, json=request_data)
            response.raise_for_status()
            
            result = response.json()
            
            # 解析结构化响应
            if self.provider == LLMProvider.VOLCENGINE:
                content = result.get("choices", [{}])[0].get("message", {}).get("content", "")
            else:
                content = result.get("choices", [{}])[0].get("message", {}).get("content", "")
            
            # 这里需要根据实际的响应格式进行解析
            # 简化的JSON解析
            import json
            try:
                return json.loads(content)
            except json.JSONDecodeError:
                logger.warning("结构化响应解析失败，返回原始内容")
                return {"content": content}
                
        except Exception as e:
            logger.error(f"LLM结构化聊天请求失败: {e}")
            raise
    
    async def embed(self, texts: List[str]) -> List[List[float]]:
        """文本嵌入"""
        if not self._available:
            raise RuntimeError("LLM客户端不可用")
        
        try:
            if self.provider == LLMProvider.VOLCENGINE:
                # 火山引擎嵌入API
                request_data = {
                    "model": "text-embedding-v1",
                    "input": texts
                }
                response = await self._client.post("/api/v3/embeddings", json=request_data)
            else:
                # OpenAI兼容的嵌入API
                request_data = {
                    "model": "text-embedding-ada-002",
                    "input": texts
                }
                response = await self._client.post("/v1/embeddings", json=request_data)
            
            response.raise_for_status()
            result = response.json()
            
            return [item["embedding"] for item in result.get("data", [])]
            
        except Exception as e:
            logger.error(f"LLM嵌入请求失败: {e}")
            raise
    
    def _build_volcengine_request(self, messages: List[Dict[str, str]], **kwargs) -> Dict[str, Any]:
        """构建火山引擎请求"""
        request_data = {
            "model": self.model,
            "messages": messages,
            "max_tokens": kwargs.get("max_tokens", 2048),
            "temperature": kwargs.get("temperature", 0.7),
            "top_p": kwargs.get("top_p", 0.9),
            "stream": kwargs.get("stream", False)
        }
        
        # 添加可选参数
        if "stop" in kwargs:
            request_data["stop"] = kwargs["stop"]
        
        return request_data
    
    def _build_openai_request(self, messages: List[Dict[str, str]], **kwargs) -> Dict[str, Any]:
        """构建OpenAI兼容请求"""
        request_data = {
            "model": self.model,
            "messages": messages,
            "max_tokens": kwargs.get("max_tokens", 2048),
            "temperature": kwargs.get("temperature", 0.7),
            "top_p": kwargs.get("top_p", 0.9),
            "stream": kwargs.get("stream", False)
        }
        
        # 添加可选参数
        if "stop" in kwargs:
            request_data["stop"] = kwargs["stop"]
        
        return request_data