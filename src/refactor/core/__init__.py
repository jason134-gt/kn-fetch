"""核心模块"""

from .refactoring_task import (
    RiskLevel,
    RefactoringActionType,
    TechnicalDebtType,
    KnowledgeContext,
    RefactoringContext,
    TechnicalDebt,
    RiskAssessment,
    BusinessContract,
    RefactoringPlan,
    ImprovementSuggestion,
    RefactoringReport
)
from .knowledge_loader import KnowledgeLoader
from .context_assembler import ContextAssembler
from .refactoring_orchestrator import RefactoringOrchestrator
from .enhanced_report_generator import EnhancedReportGenerator
from .problem_detail import (
    ProblemDetail,
    ProblemSummary,
    ProblemType,
    Severity,
    RiskLevel as ProblemRiskLevel,
    EnhancedRefactoringReport
)
from .feedback_processor import (
    FeedbackType,
    FeedbackScope,
    FeedbackStatus,
    UserFeedback,
    FeedbackBatch,
    FeedbackProcessor,
    create_feedback,
    create_feedback_batch
)
from .report_regenerator import (
    ReportChange,
    ReportRegenerator
)
from .review_workflow import (
    ReviewStatus,
    ReviewAction,
    ReviewWorkflow,
    ReviewRecord,
    ConvergenceDetector,
    ConvergenceConfig
)

__all__ = [
    # 枚举
    'RiskLevel',
    'RefactoringActionType',
    'TechnicalDebtType',
    # 数据模型
    'KnowledgeContext',
    'RefactoringContext',
    'TechnicalDebt',
    'RiskAssessment',
    'BusinessContract',
    'RefactoringPlan',
    'ImprovementSuggestion',
    'RefactoringReport',
    # 核心组件
    'KnowledgeLoader',
    'ContextAssembler',
    'RefactoringOrchestrator',
    # 增强报告
    'EnhancedReportGenerator',
    'ProblemDetail',
    'ProblemSummary',
    'ProblemType',
    'Severity',
    'EnhancedRefactoringReport',
    # 反馈处理
    'FeedbackType',
    'FeedbackScope',
    'FeedbackStatus',
    'UserFeedback',
    'FeedbackBatch',
    'FeedbackProcessor',
    'create_feedback',
    'create_feedback_batch',
    # 报告重生成
    'ReportChange',
    'ReportRegenerator',
    # 审核工作流
    'ReviewStatus',
    'ReviewAction',
    'ReviewWorkflow',
    'ReviewRecord',
    'ConvergenceDetector',
    'ConvergenceConfig'
]
