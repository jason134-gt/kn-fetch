"""
集成模块 - ArchAI CodeExplainer + GitUML
用于增强知识提取能力，支持软件重构分析
"""
from .archai_code_explainer import (
    ArchAICodeExplainer, 
    CodeExplanation, 
    ArchitecturePattern,
    CodeSmellType,
    ArchitectureAnalysisResult
)
from .gituml_integration import (
    GitUMLIntegration, 
    GitUMLDiagram, 
    DiagramType,
    OutputFormat,
    GitCommitInfo
)
from .integration_adapter import (
    EnhancedKnowledgeExtractor,
    create_enhanced_extractor
)

__all__ = [
    # ArchAI CodeExplainer
    'ArchAICodeExplainer',
    'CodeExplanation',
    'ArchitecturePattern',
    'CodeSmellType',
    'ArchitectureAnalysisResult',
    
    # GitUML Integration
    'GitUMLIntegration',
    'GitUMLDiagram',
    'DiagramType',
    'OutputFormat',
    'GitCommitInfo',
    
    # Integration Adapter
    'EnhancedKnowledgeExtractor',
    'create_enhanced_extractor'
]
