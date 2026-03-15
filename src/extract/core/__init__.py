from .knowledge_extractor import KnowledgeExtractor
from .document_parser import DocumentParser
from .large_scale_processor import LargeScaleProcessor
from .knowledge_optimizer import KnowledgeOptimizer
from .architecture_analyzer import ArchitectureAnalyzer
from .uml_generator import UMLGenerator
from .architecture_reporter import ArchitectureReporter
from .refactoring_analyzer import RefactoringAnalyzer, RiskLevel, TechnicalDebt, BusinessSemantic, ArchitectureDiagnosis

# 新增模块
from .semantic_extractor import SemanticExtractor, EnhancedBusinessSemantic, InputContract, OutputContract
from .code_asset_visualizer import CodeAssetVisualizer
from .duplicate_detector import DuplicateDetector, DuplicateReport, DuplicatePair
from .security_scanner import SecurityScanner, SecurityScanResult, Vulnerability, VulnerabilityType, Severity
from .metadata_repository import MetadataRepository, EntityMetadata, ModuleMetadata

__all__ = [
    "KnowledgeExtractor",
    "DocumentParser", 
    "LargeScaleProcessor",
    "KnowledgeOptimizer",
    "ArchitectureAnalyzer",
    "UMLGenerator",
    "ArchitectureReporter",
    "RefactoringAnalyzer",
    "RiskLevel",
    "TechnicalDebt",
    "BusinessSemantic",
    "ArchitectureDiagnosis",
    # 新增
    "SemanticExtractor",
    "EnhancedBusinessSemantic",
    "InputContract",
    "OutputContract",
    "CodeAssetVisualizer",
    "DuplicateDetector",
    "DuplicateReport",
    "DuplicatePair",
    "SecurityScanner",
    "SecurityScanResult",
    "Vulnerability",
    "VulnerabilityType",
    "Severity",
    "MetadataRepository",
    "EntityMetadata",
    "ModuleMetadata"
]
