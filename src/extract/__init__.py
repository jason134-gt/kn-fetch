"""
知识提取智能体模块

包含代码解析、知识提取、文档生成等核心功能
"""

from .core.knowledge_extractor import KnowledgeExtractor
from .core.knowledge_document_generator import KnowledgeDocumentGenerator
from .core.uml_generator import UMLGenerator
from .core.architecture_analyzer import ArchitectureAnalyzer
from .core.semantic_extractor import SemanticExtractor
from .core.duplicate_detector import DuplicateDetector

from .agents.base_agent import BaseAgent, AgentResult
from .agents.file_scanner import FileScannerAgent as FileScanner
from .agents.code_parser import CodeParserAgent as CodeParser

from .ai.llm_client import LLMClient
from .ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer

from .gitnexus.gitnexus_client import GitNexusClient
from .gitnexus.models import CodeEntity, KnowledgeGraph

__all__ = [
    # 核心组件
    'KnowledgeExtractor',
    'KnowledgeDocumentGenerator',
    'UMLGenerator',
    'ArchitectureAnalyzer',
    'SemanticExtractor',
    'DuplicateDetector',
    # Agent
    'BaseAgent',
    'AgentResult',
    'FileScanner',
    'CodeParser',
    # AI
    'LLMClient',
    'DeepKnowledgeAnalyzer',
    # GitNexus
    'GitNexusClient',
    'CodeEntity',
    'KnowledgeGraph',
]
