"""
问题详情数据模型

定义重构分析报告中的问题详情结构，包含四要素：
- 是什么：问题描述、位置、原因分析
- 怎么修复：具体修复步骤、代码示例
- 有什么风险：风险分析、影响范围
- 有什么方案：多种解决方案对比
"""

from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
from enum import Enum
from datetime import datetime


class Severity(str, Enum):
    """严重程度"""
    HIGH = "高"
    MEDIUM = "中"
    LOW = "低"


class RiskLevel(str, Enum):
    """风险等级"""
    HIGH = "高风险"
    MEDIUM = "中等风险"
    LOW = "低风险"


class ProblemType(str, Enum):
    """问题类型"""
    HIGH_COMPLEXITY = "高复杂度"
    LARGE_FILE = "大文件"
    MISSING_DOC = "缺少文档"
    MISSING_TEST = "缺少测试"
    CODE_DUPLICATION = "代码重复"
    CIRCULAR_DEPENDENCY = "循环依赖"
    CODE_SMELL = "代码异味"
    SECURITY_ISSUE = "安全问题"
    PERFORMANCE_ISSUE = "性能问题"


class Risk(BaseModel):
    """风险项"""
    risk_type: str = Field(description="风险类型")
    description: str = Field(description="风险描述")
    probability: str = Field(default="中", description="可能性：高/中/低")
    impact: str = Field(default="中", description="影响程度：高/中/低")
    mitigation: str = Field(default="", description="缓解措施")


class Solution(BaseModel):
    """解决方案"""
    name: str = Field(description="方案名称")
    description: str = Field(description="方案描述")
    pros: List[str] = Field(default_factory=list, description="优点")
    cons: List[str] = Field(default_factory=list, description="缺点")
    work_hours: float = Field(default=1.0, description="工作量（小时）")
    risk_level: str = Field(default="低", description="风险等级")
    recommendation: int = Field(default=3, ge=1, le=5, description="推荐度（1-5星）")


class FixStep(BaseModel):
    """修复步骤"""
    step_number: int = Field(description="步骤编号")
    title: str = Field(description="步骤标题")
    description: str = Field(description="步骤描述")
    original_code: Optional[str] = Field(default=None, description="原始代码")
    fixed_code: Optional[str] = Field(default=None, description="修复后代码")
    explanation: Optional[str] = Field(default=None, description="说明")


class ImpactScope(BaseModel):
    """影响范围"""
    caller_count: int = Field(default=0, description="被调用次数")
    callers: List[str] = Field(default_factory=list, description="调用者列表")
    related_tests: List[str] = Field(default_factory=list, description="关联测试")
    related_files: List[str] = Field(default_factory=list, description="关联文件")


class CurrentState(BaseModel):
    """当前状态"""
    complexity: Optional[int] = Field(default=None, description="复杂度")
    lines_of_code: Optional[int] = Field(default=None, description="代码行数")
    caller_count: Optional[int] = Field(default=None, description="调用次数")
    has_docstring: Optional[bool] = Field(default=None, description="是否有文档")
    has_tests: Optional[bool] = Field(default=None, description="是否有测试")


class ExpectedEffect(BaseModel):
    """预期效果"""
    complexity_before: Optional[int] = Field(default=None, description="重构前复杂度")
    complexity_after: Optional[int] = Field(default=None, description="重构后复杂度")
    readability_improvement: Optional[str] = Field(default=None, description="可读性提升")
    test_coverage_before: Optional[str] = Field(default=None, description="重构前测试覆盖率")
    test_coverage_after: Optional[str] = Field(default=None, description="重构后测试覆盖率")


class ProblemDetail(BaseModel):
    """问题详情 - 四要素模型"""
    
    # === 基本信息 ===
    problem_id: str = Field(description="问题编号，如 P001")
    problem_type: str = Field(default="unknown", description="问题类型")  # 改为str以支持更多类型
    severity: Severity = Field(default=Severity.LOW, description="严重程度")
    module: str = Field(default="", description="所属模块")
    file_path: str = Field(default="", description="文件路径")
    line_start: Optional[int] = Field(default=None, description="起始行号")
    line_end: Optional[int] = Field(default=None, description="结束行号")
    entity_name: str = Field(default="", description="实体名称（类名/函数名等）")
    entity_type: str = Field(default="function", description="实体类型")
    
    # === 是什么（问题描述） ===
    description: str = Field(default="", description="问题描述")
    current_state: Dict[str, Any] = Field(default_factory=dict, description="当前状态")
    code_context: str = Field(default="", description="代码上下文")
    
    # === 怎么修复 ===
    fix_steps: List[Dict[str, Any]] = Field(default_factory=list, description="修复步骤")
    
    # === 有什么风险 ===
    risks: List[Dict[str, Any]] = Field(default_factory=list, description="风险列表")
    
    # === 有什么方案 ===
    solutions: List[Dict[str, Any]] = Field(default_factory=list, description="解决方案列表")
    recommended_solution: Optional[int] = Field(default=0, description="推荐方案索引")
    
    # === 用户反馈 ===
    user_comment: Optional[str] = Field(default=None, description="用户评论")
    user_confirmed: bool = Field(default=False, description="用户是否确认")
    ignored: bool = Field(default=False, description="是否被忽略")
    ignore_reason: Optional[str] = Field(default=None, description="忽略原因")
    
    # === 元数据 ===
    created_at: datetime = Field(default_factory=datetime.now, description="创建时间")
    tags: List[str] = Field(default_factory=list, description="标签")


class ProblemSummary(BaseModel):
    """问题摘要"""
    total_problems: int = Field(default=0, description="问题总数")
    high_severity: int = Field(default=0, description="高严重度数量")
    medium_severity: int = Field(default=0, description="中严重度数量")
    low_severity: int = Field(default=0, description="低严重度数量")
    estimated_workload_hours: float = Field(default=0.0, description="预计工时")
    problem_types: Dict[str, int] = Field(default_factory=dict, description="问题类型统计")


class ModuleHealth(BaseModel):
    """模块健康度"""
    module_name: str
    health_score: float = Field(ge=0, le=100, description="健康度评分")
    problem_count: int
    high_severity_count: int
    main_issues: List[str]


class ExecutionPlan(BaseModel):
    """执行计划"""
    phase: str
    priority: str
    problems: List[str]
    estimated_hours: float
    description: str


class EnhancedRefactoringReport(BaseModel):
    """增强版重构分析报告"""
    
    # 基本信息
    report_id: str = Field(default_factory=lambda: f"RPT-{datetime.now().strftime('%Y%m%d%H%M%S')}")
    project_name: str = Field(default="Unknown")
    generated_at: datetime = Field(default_factory=datetime.now)
    
    # 版本控制
    version: int = Field(default=1, description="报告版本号")
    parent_report_id: Optional[str] = Field(default=None, description="父报告ID")
    feedback_batch_id: Optional[str] = Field(default=None, description="反馈批次ID")
    change_log: Optional[Dict[str, Any]] = Field(default=None, description="变更日志")
    
    # 执行摘要
    summary: ProblemSummary = Field(default_factory=ProblemSummary, description="问题摘要")
    
    # 问题详情
    problems: List[ProblemDetail] = Field(default_factory=list, description="问题列表")
    
    # 模块健康度
    module_health: List[ModuleHealth] = Field(default_factory=list, description="模块健康度")
    
    # 执行计划
    execution_plan: List[ExecutionPlan] = Field(default_factory=list, description="执行计划")
    
    # 文件问题汇总
    file_problems: Dict[str, List[str]] = Field(default_factory=dict, description="文件问题汇总")


__all__ = [
    'Severity',
    'RiskLevel', 
    'ProblemType',
    'Risk',
    'Solution',
    'FixStep',
    'CurrentState',
    'ProblemDetail',
    'ProblemSummary',
    'ModuleHealth',
    'ExecutionPlan',
    'EnhancedRefactoringReport'
]
