"""
Agent集群模块 - 基于design-v1方案实现7个核心Agent

Agent职责分工：
1. FileScannerAgent - 文件扫描与预处理
2. CodeParserAgent - 代码解析与AST分析
3. SemanticExtractorAgent - 语义特征提取
4. ArchitectureAnalyzerAgent - 架构关系分析
5. BusinessLogicAgent - 业务逻辑识别
6. DocumentationAgent - 文档生成
7. QualityAssessorAgent - 质量评估
"""

from .base_agent import BaseAgent
from .file_scanner import FileScannerAgent
from .code_parser import CodeParserAgent
from .semantic_extractor import SemanticExtractorAgent
from .architecture_analyzer import ArchitectureAnalyzerAgent
from .business_logic import BusinessLogicAgent
from .documentation import DocumentationAgent
from .quality_assessor import QualityAssessorAgent

__all__ = [
    "BaseAgent",
    "FileScannerAgent",
    "CodeParserAgent", 
    "SemanticExtractorAgent",
    "ArchitectureAnalyzerAgent",
    "BusinessLogicAgent",
    "DocumentationAgent",
    "QualityAssessorAgent"
]