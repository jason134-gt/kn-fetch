"""
基于 LangChain 的 LLM 客户端

使用 LangChain 重构的统一大模型调用接口
"""

import asyncio
import json
from typing import Optional, Dict, Any, List, Union
from langchain_core.language_models.base import BaseLanguageModel
from langchain_core.output_parsers import StrOutputParser, JsonOutputParser
from langchain_core.runnables import RunnablePassthrough

from .langchain_config import LangChainConfig, create_langchain_llm, LangChainLLMWrapper
from .langchain_prompts import (
    KnowledgePrompts, TestingPrompts, ValidationPrompts,
    OptimizationPrompts, AnalysisPrompts
)


class LangChainLLMClient:
    """
    基于 LangChain 的统一 LLM 客户端
    
    支持多种 LLM 提供商，提供统一的调用接口
    """
    
    def __init__(self, config: Dict[str, Any]):
        """
        初始化 LLM 客户端
        
        Args:
            config: AI 配置字典
        """
        self.config = LangChainConfig(config)
        self.llm = create_langchain_llm(self.config)
        self.is_available_flag = self.config.is_available()
        
        # 输出解析器
        self.str_parser = StrOutputParser()
        self.json_parser = JsonOutputParser()
    
    def is_available(self) -> bool:
        """检查 LLM 是否可用"""
        return self.is_available_flag
    
    async def chat(
        self,
        system_prompt: str,
        user_prompt: str,
        use_json: bool = False,
        **kwargs
    ) -> str:
        """
        异步调用 LLM
        
        Args:
            system_prompt: 系统提示词
            user_prompt: 用户提示词
            use_json: 是否使用 JSON 解析器
            **kwargs: 额外参数（temperature, max_tokens 等）
            
        Returns:
            LLM 响应内容
        """
        if not self.is_available():
            raise ValueError("LLM 不可用，请检查配置和 API Key")
        
        # 构建提示链
        from langchain_core.prompts import ChatPromptTemplate
        
        prompt = ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            ("user", user_prompt)
        ])
        
        # 选择输出解析器
        parser = self.json_parser if use_json else self.str_parser
        
        # 构建链
        chain = prompt | self.llm | parser
        
        # 调用
        result = await chain.ainvoke({
            **kwargs
        })
        
        if isinstance(result, dict):
            return json.dumps(result, ensure_ascii=False)
        return str(result)
    
    def chat_sync(
        self,
        system_prompt: str,
        user_prompt: str,
        use_json: bool = False,
        **kwargs
    ) -> str:
        """
        同步调用 LLM
        
        Args:
            system_prompt: 系统提示词
            user_prompt: 用户提示词
            use_json: 是否使用 JSON 解析器
            **kwargs: 额外参数
            
        Returns:
            LLM 响应内容
        """
        if not self.is_available():
            raise ValueError("LLM 不可用，请检查配置和 API Key")
        
        # 构建提示链
        from langchain_core.prompts import ChatPromptTemplate
        
        prompt = ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            ("user", user_prompt)
        ])
        
        # 选择输出解析器
        parser = self.json_parser if use_json else self.str_parser
        
        # 构建链
        chain = prompt | self.llm | parser
        
        # 调用
        result = chain.invoke(kwargs)
        
        if isinstance(result, dict):
            return json.dumps(result, ensure_ascii=False)
        return str(result)
    
    async def analyze_code(
        self,
        code_content: str,
        analysis_type: str = "complexity",
        context_info: str = ""
    ) -> Dict[str, Any]:
        """
        分析代码
        
        Args:
            code_content: 代码内容
            analysis_type: 分析类型（complexity, refactor, performance 等）
            context_info: 上下文信息
            
        Returns:
            分析结果字典
        """
        if analysis_type == "complexity":
            prompt = AnalysisPrompts.COMPLEXITY_ANALYSIS
        else:
            prompt = OptimizationPrompts.CODE_OPTIMIZATION.partial(
                analysis_type=analysis_type
            )
        
        # 构建链
        chain = prompt | self.llm | self.json_parser
        
        result = await chain.ainvoke({
            "code_content": code_content,
            "code_type": "function" if "def " in code_content else "class",
            "context_info": context_info
        })
        
        return result if isinstance(result, dict) else {"result": result}
    
    async def generate_test_cases(
        self,
        code_content: str,
        entity_name: str,
        entity_type: str
    ) -> List[Dict[str, Any]]:
        """
        生成测试用例
        
        Args:
            code_content: 代码内容
            entity_name: 实体名称
            entity_type: 实体类型
            
        Returns:
            测试用例列表
        """
        # 构建链
        chain = TestingPrompts.TEST_CASE_GENERATION | self.llm | self.json_parser
        
        result = await chain.ainvoke({
            "entity_name": entity_name,
            "entity_code": code_content,
            "entity_type": entity_type
        })
        
        if isinstance(result, list):
            return result
        elif isinstance(result, dict):
            return result.get("test_cases", [])
        return []
    
    async def validate_knowledge(
        self,
        extracted_knowledge: Dict[str, Any],
        original_content: str,
        extraction_type: str
    ) -> Dict[str, Any]:
        """
        验证提取的知识
        
        Args:
            extracted_knowledge: 提取的知识
            original_content: 原始内容
            extraction_type: 提取类型
            
        Returns:
            验证结果
        """
        # 构建链
        chain = ValidationPrompts.KNOWLEDGE_VALIDATION | self.llm | self.json_parser
        
        result = await chain.ainvoke({
            "extracted_knowledge": json.dumps(extracted_knowledge, ensure_ascii=False),
            "original_content": original_content[:5000],  # 限制长度
            "extraction_type": extraction_type
        })
        
        return result if isinstance(result, dict) else {"is_valid": True}
    
    async def optimize_knowledge(
        self,
        knowledge_graph: Dict[str, Any],
        optimization_level: str = "medium"
    ) -> Dict[str, Any]:
        """
        优化知识图谱
        
        Args:
            knowledge_graph: 知识图谱
            optimization_level: 优化级别（low, medium, high）
            
        Returns:
            优化后的知识图谱
        """
        # 构建链
        chain = OptimizationPrompts.KNOWLEDGE_OPTIMIZATION | self.llm | self.json_parser
        
        result = await chain.ainvoke({
            "knowledge_graph": json.dumps(knowledge_graph, ensure_ascii=False, indent=2),
            "optimization_level": optimization_level
        })
        
        return result if isinstance(result, dict) else knowledge_graph
    
    async def analyze_test_failure(
        self,
        test_code: str,
        error_message: str,
        source_code: str
    ) -> Dict[str, Any]:
        """
        分析测试失败原因
        
        Args:
            test_code: 测试代码
            error_message: 错误信息
            source_code: 被测代码
            
        Returns:
            分析结果
        """
        # 构建链
        chain = TestingPrompts.TEST_FAILURE_ANALYSIS | self.llm | self.json_parser
        
        result = await chain.ainvoke({
            "test_code": test_code,
            "error_message": error_message,
            "source_code": source_code[:5000]
        })
        
        return result if isinstance(result, dict) else {}
    
    async def detect_architecture_patterns(
        self,
        project_structure: str,
        dependencies: str,
        key_components: str
    ) -> Dict[str, Any]:
        """
        检测架构模式
        
        Args:
            project_structure: 项目结构
            dependencies: 依赖关系
            key_components: 关键组件
            
        Returns:
            架构模式检测结果
        """
        # 构建链
        chain = AnalysisPrompts.ARCHITECTURE_PATTERN_DETECTION | self.llm | self.json_parser
        
        result = await chain.ainvoke({
            "project_structure": project_structure,
            "dependencies": dependencies,
            "key_components": key_components
        })
        
        return result if isinstance(result, dict) else {}
    
    async def detect_code_smells(
        self,
        code_content: str,
        file_structure: str = ""
    ) -> Dict[str, Any]:
        """
        检测代码异味
        
        Args:
            code_content: 代码内容
            file_structure: 文件结构
            
        Returns:
            代码异味检测结果
        """
        # 构建链
        chain = AnalysisPrompts.CODE_SMELL_DETECTION | self.llm | self.json_parser
        
        result = await chain.ainvoke({
            "code_content": code_content,
            "file_structure": file_structure
        })
        
        return result if isinstance(result, dict) else {}
    
    def get_config(self) -> LangChainConfig:
        """获取配置"""
        return self.config
    
    def get_provider(self) -> str:
        """获取当前提供商"""
        return self.config.provider


class LangChainLLMClientFactory:
    """LangChain LLM 客户端工厂"""
    
    _instances: Dict[str, LangChainLLMClient] = {}
    
    @classmethod
    def create(cls, config: Dict[str, Any]) -> LangChainLLMClient:
        """
        创建或获取 LLM 客户端实例
        
        Args:
            config: AI 配置
            
        Returns:
            LLM 客户端实例
        """
        provider = config.get("provider", "volcengine")
        
        # 使用简单的缓存键
        cache_key = provider
        
        if cache_key not in cls._instances:
            cls._instances[cache_key] = LangChainLLMClient(config)
        
        return cls._instances[cache_key]
    
    @classmethod
    def clear_cache(cls):
        """清除缓存的实例"""
        cls._instances.clear()


__all__ = [
    "LangChainLLMClient",
    "LangChainLLMClientFactory",
]
