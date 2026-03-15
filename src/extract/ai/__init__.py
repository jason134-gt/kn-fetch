from .ai_client import AIClient
from .knowledge_optimizer import KnowledgeOptimizer
from .self_testing import SelfTester
from .self_validation import SelfValidator
from .llm_client import LLMClient
from .deep_knowledge_analyzer import DeepKnowledgeAnalyzer

# LangChain 重构模块
from .langchain_config import LangChainConfig, create_langchain_llm, LangChainLLMWrapper
from .langchain_llm_client import LangChainLLMClient, LangChainLLMClientFactory
from .langchain_prompts import (
    KnowledgePrompts, TestingPrompts, ValidationPrompts,
    OptimizationPrompts, AnalysisPrompts, format_entities_for_prompt
)
from .langchain_deep_analyzer import LangChainDeepAnalyzer
from .langchain_agent_orchestrator import (
    LangChainAgentOrchestrator,
    SimpleAgentOrchestrator,
    AgentState
)

__all__ = [
    # 旧版模块（保留兼容性）
    "AIClient",
    "KnowledgeOptimizer",
    "SelfTester",
    "SelfValidator",
    "LLMClient",
    "DeepKnowledgeAnalyzer",
    
    # LangChain 新版模块
    "LangChainConfig",
    "create_langchain_llm",
    "LangChainLLMWrapper",
    "LangChainLLMClient",
    "LangChainLLMClientFactory",
    
    # LangChain 深度分析器
    "LangChainDeepAnalyzer",
    
    # LangChain Agent 编排器
    "LangChainAgentOrchestrator",
    "SimpleAgentOrchestrator",
    "AgentState",
    
    # 提示词模板
    "KnowledgePrompts",
    "TestingPrompts",
    "ValidationPrompts",
    "OptimizationPrompts",
    "AnalysisPrompts",
    "format_entities_for_prompt",
]
