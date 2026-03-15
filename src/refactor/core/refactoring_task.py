"""
重构分析智能体核心数据模型

定义重构分析所需的全部数据结构
"""

from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
from datetime import datetime
from enum import Enum
import uuid


class RiskLevel(str, Enum):
    """风险等级"""
    P0_CRITICAL = "P0"     # 核心模块：人工100%把控
    P1_IMPORTANT = "P1"    # 重要模块：人工审核核心逻辑
    P2_COMMON = "P2"       # 通用模块：AI可全自动重构
    P3_EDGE = "P3"         # 边缘模块：可直接评估下线


class RefactoringActionType(str, Enum):
    """重构动作类型"""
    # 低风险全自动动作
    EXTRACT_CONSTANT = "extract_constant"
    RENAME = "rename"
    FORMAT_CODE = "format_code"
    REMOVE_DEAD_CODE = "remove_dead_code"
    COMPLETE_COMMENT = "complete_comment"
    EXTRACT_DUPLICATE_CODE = "extract_duplicate_code"
    # 中风险需审核动作
    SPLIT_FUNCTION = "split_function"
    SPLIT_CLASS = "split_class"
    EXTRACT_INTERFACE = "extract_interface"
    EXTRACT_METHOD = "extract_method"
    DECOUPLE_DEPENDENCY = "decouple_dependency"
    DEPENDENCY_INJECTION = "dependency_injection"
    # 高风险需审批动作
    SPLIT_MODULE = "split_module"
    ARCHITECTURE_ADJUSTMENT = "architecture_adjustment"


class TechnicalDebtType(str, Enum):
    """技术债务类型"""
    CODE_SMELL = "code_smell"
    DEAD_CODE = "dead_code"
    CIRCULAR_DEPENDENCY = "circular_dependency"
    HIGH_COMPLEXITY = "high_complexity"
    DUPLICATION = "duplication"
    MISSING_TESTS = "missing_tests"
    SECURITY_ISSUE = "security_issue"
    PERFORMANCE_ISSUE = "performance_issue"
    OUTDATED_DEPENDENCY = "outdated_dependency"


class KnowledgeContext(BaseModel):
    """知识上下文 - 从知识提取输出加载"""
    project_name: str = Field(default="Unknown")
    project_description: str = Field(default="")
    tech_stack: List[str] = Field(default_factory=list)
    entities: Dict[str, Any] = Field(default_factory=dict)
    relationships: List[Any] = Field(default_factory=list)
    project_overview: str = Field(default="")
    business_flow: str = Field(default="")
    architecture_design: str = Field(default="")
    feature_modules: str = Field(default="")
    api_reference: str = Field(default="")
    total_entities: int = Field(default=0)
    total_relationships: int = Field(default=0)
    total_files: int = Field(default=0)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class RefactoringContext(BaseModel):
    """重构上下文 - 组装LLM输入"""
    target_entity_id: str
    target_entity: Dict[str, Any] = Field(default_factory=dict)
    semantic_contract: Optional[Dict[str, Any]] = None
    callers: List[Dict[str, Any]] = Field(default_factory=list)
    callees: List[Dict[str, Any]] = Field(default_factory=list)
    dependencies: List[Dict[str, Any]] = Field(default_factory=list)
    related_docs: List[str] = Field(default_factory=list)
    business_domain: str = Field(default="其他")
    domain_tags: List[str] = Field(default_factory=list)
    context_token_count: int = Field(default=0)


class TechnicalDebt(BaseModel):
    """技术债务"""
    debt_id: str = Field(default_factory=lambda: str(uuid.uuid4())[:8])
    debt_type: TechnicalDebtType
    severity: str = Field(default="medium")  # critical, high, medium, low
    entity_id: Optional[str] = None
    entity_name: Optional[str] = None
    file_path: str = Field(default="")
    line_start: int = Field(default=0)
    line_end: int = Field(default=0)
    description: str = Field(default="")
    suggestion: str = Field(default="")
    estimated_effort: str = Field(default="未知")


class RiskAssessment(BaseModel):
    """风险评估结果"""
    entity_id: str
    entity_name: str = Field(default="")
    file_path: str = Field(default="")
    risk_level: RiskLevel = Field(default=RiskLevel.P2_COMMON)
    score: float = Field(default=0.0)
    reasons: List[str] = Field(default_factory=list)
    impact_scope: List[str] = Field(default_factory=list)
    business_domain: str = Field(default="其他")
    refactoring_recommendation: str = Field(default="")


class BusinessContract(BaseModel):
    """业务语义契约"""
    entity_id: str
    entity_name: str = Field(default="")
    business_summary: str = Field(default="")
    input_contract: Dict[str, Any] = Field(default_factory=dict)
    output_contract: Dict[str, Any] = Field(default_factory=dict)
    side_effects: List[str] = Field(default_factory=list)
    business_rules: List[str] = Field(default_factory=list)
    exception_scenarios: List[str] = Field(default_factory=list)
    boundary_conditions: List[str] = Field(default_factory=list)


class RefactoringPlan(BaseModel):
    """重构方案"""
    plan_id: str = Field(default_factory=lambda: str(uuid.uuid4())[:8])
    entity_id: str
    entity_name: str = Field(default="")
    action_type: RefactoringActionType
    risk_level: RiskLevel = Field(default=RiskLevel.P2_COMMON)
    before_code: str = Field(default="")
    after_code: str = Field(default="")
    changes_description: str = Field(default="")
    validation_criteria: List[str] = Field(default_factory=list)
    test_requirements: List[str] = Field(default_factory=list)
    rollback_plan: str = Field(default="")
    requires_approval: bool = Field(default=False)
    approver_roles: List[str] = Field(default_factory=list)
    status: str = Field(default="pending")


class ImprovementSuggestion(BaseModel):
    """改进建议"""
    suggestion_id: str = Field(default_factory=lambda: str(uuid.uuid4())[:8])
    category: str = Field(default="code_quality")  # architecture, code_quality, performance, security
    priority: str = Field(default="medium")  # high, medium, low
    target_scope: List[str] = Field(default_factory=list)
    title: str = Field(default="")
    description: str = Field(default="")
    rationale: str = Field(default="")
    implementation_steps: List[str] = Field(default_factory=list)
    expected_benefits: List[str] = Field(default_factory=list)
    potential_risks: List[str] = Field(default_factory=list)
    related_debts: List[str] = Field(default_factory=list)
    related_entities: List[str] = Field(default_factory=list)


class RefactoringReport(BaseModel):
    """重构分析报告"""
    report_id: str = Field(default_factory=lambda: str(uuid.uuid4())[:8])
    project_name: str = Field(default="Unknown")
    generated_at: datetime = Field(default_factory=datetime.now)
    risk_summary: Dict[str, int] = Field(default_factory=dict)
    high_risk_entities: List[RiskAssessment] = Field(default_factory=list)
    total_debts: int = Field(default=0)
    debt_by_type: Dict[str, int] = Field(default_factory=dict)
    debt_by_severity: Dict[str, int] = Field(default_factory=dict)
    top_debts: List[TechnicalDebt] = Field(default_factory=list)
    architecture_style: str = Field(default="")
    architecture_issues: List[Dict[str, Any]] = Field(default_factory=list)
    cyclic_dependencies: List[List[str]] = Field(default_factory=list)
    hotspots: List[Dict[str, Any]] = Field(default_factory=list)
    improvement_suggestions: List[ImprovementSuggestion] = Field(default_factory=list)
    refactoring_plans: List[RefactoringPlan] = Field(default_factory=list)
    execution_priority: List[str] = Field(default_factory=list)
