# 用户反馈闭环系统设计方案

## 一、概述

### 1.1 目标

实现用户与重构分析系统的交互闭环：
1. 系统生成初步重构分析报告
2. 用户查看报告并提出反馈意见
3. 系统根据反馈重新生成报告
4. 用户审核确认，可继续迭代

### 1.2 核心流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           用户反馈闭环流程                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐        │
│   │ 生成报告  │───▶│ 用户查看  │───▶│ 提交反馈  │───▶│ 重新生成  │        │
│   └──────────┘    └──────────┘    └──────────┘    └──────────┘        │
│        ▲                                                │              │
│        │                                                ▼              │
│        │                                         ┌──────────┐         │
│        │                                         │ 用户审核  │         │
│        │                                         └──────────┘         │
│        │                                              │               │
│        │              ┌───────────────────────────────┘               │
│        │              │                                               │
│        │              ▼                                               │
│        │       ┌──────────┐    否    ┌──────────┐                    │
│        └───────│ 是否满意  │◀─────────│ 继续反馈  │                    │
│                └──────────┘          └──────────┘                    │
│                     │ 是                                              │
│                     ▼                                                 │
│               ┌──────────┐                                           │
│               │ 确认报告  │                                           │
│               └──────────┘                                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、反馈类型定义

### 2.1 反馈类型枚举

```python
from enum import Enum
from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field

class FeedbackType(str, Enum):
    """反馈类型"""
    
    # 问题级别反馈
    AGREE = "agree"                    # 同意该问题
    DISAGREE = "disagree"              # 不同意该问题
    MODIFY = "modify"                  # 需要修改问题
    
    # 方案级别反馈
    ACCEPT_SOLUTION = "accept_solution"           # 接受方案
    REJECT_SOLUTION = "reject_solution"           # 拒绝方案
    MODIFY_SOLUTION = "modify_solution"           # 修改方案
    ADD_SOLUTION = "add_solution"                 # 添加新方案
    
    # 优先级反馈
    RAISE_PRIORITY = "raise_priority"    # 提高优先级
    LOWER_PRIORITY = "lower_priority"    # 降低优先级
    
    # 全局反馈
    ADD_PROBLEM = "add_problem"          # 添加新问题
    IGNORE_PROBLEM = "ignore_problem"    # 忽略问题
    GLOBAL_COMMENT = "global_comment"    # 全局评论


class FeedbackScope(str, Enum):
    """反馈作用范围"""
    PROBLEM = "problem"       # 单个问题
    SOLUTION = "solution"     # 单个方案
    FILE = "file"             # 文件级别
    MODULE = "module"         # 模块级别
    GLOBAL = "global"         # 全局


class FeedbackStatus(str, Enum):
    """反馈处理状态"""
    PENDING = "pending"       # 待处理
    PROCESSING = "processing" # 处理中
    APPLIED = "applied"       # 已应用
    REJECTED = "rejected"     # 已拒绝
```

### 2.2 反馈数据模型

```python
class UserFeedback(BaseModel):
    """用户反馈"""
    
    feedback_id: str = Field(..., description="反馈ID")
    session_id: str = Field(..., description="会话ID")
    
    # 反馈目标
    problem_id: Optional[str] = Field(None, description="问题ID")
    solution_id: Optional[str] = Field(None, description="方案ID")
    scope: FeedbackScope = Field(..., description="反馈范围")
    
    # 反馈内容
    feedback_type: FeedbackType = Field(..., description="反馈类型")
    content: str = Field(..., description="反馈内容")
    
    # 修改建议
    suggested_fix: Optional[str] = Field(None, description="建议的修复方式")
    suggested_solution: Optional[Dict[str, Any]] = Field(None, description="建议的方案")
    suggested_priority: Optional[str] = Field(None, description="建议的优先级")
    
    # 元数据
    created_at: datetime = Field(default_factory=datetime.now)
    user_id: Optional[str] = Field(None, description="用户ID")
    context: Optional[Dict[str, Any]] = Field(None, description="上下文信息")
    
    # 处理状态
    status: FeedbackStatus = Field(default=FeedbackStatus.PENDING)
    applied_changes: Optional[Dict[str, Any]] = Field(None, description="应用的变更")


class FeedbackBatch(BaseModel):
    """批量反馈"""
    
    batch_id: str = Field(..., description="批次ID")
    session_id: str = Field(..., description="会话ID")
    feedbacks: List[UserFeedback] = Field(default_factory=list)
    
    # 统计
    total_count: int = Field(0, description="总数")
    by_type: Dict[str, int] = Field(default_factory=dict, description="按类型统计")
    by_scope: Dict[str, int] = Field(default_factory=dict, description="按范围统计")
    
    created_at: datetime = Field(default_factory=datetime.now)
```

---

## 三、反馈处理机制

### 3.1 反馈处理器架构

```python
# src/refactor/core/feedback_processor.py

from typing import List, Dict, Any, Optional
from abc import ABC, abstractmethod
import logging

logger = logging.getLogger(__name__)


class FeedbackHandler(ABC):
    """反馈处理器基类"""
    
    @property
    @abstractmethod
    def supported_types(self) -> List[FeedbackType]:
        """支持的反馈类型"""
        pass
    
    @abstractmethod
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        处理反馈
        
        Returns:
            处理结果，包含需要应用的变更
        """
        pass


class AgreeHandler(FeedbackHandler):
    """同意问题处理器"""
    
    @property
    def supported_types(self) -> List[FeedbackType]:
        return [FeedbackType.AGREE]
    
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        problem = context.get("problem", {})
        return {
            "action": "confirm_problem",
            "problem_id": feedback.problem_id,
            "confirmed": True,
            "severity": problem.get("severity"),
            "keep_in_report": True
        }


class DisagreeHandler(FeedbackHandler):
    """不同意问题处理器"""
    
    @property
    def supported_types(self) -> List[FeedbackType]:
        return [FeedbackType.DISAGREE]
    
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "action": "remove_problem",
            "problem_id": feedback.problem_id,
            "reason": feedback.content,
            "keep_in_report": False,
            "add_to_ignore_list": True
        }


class ModifyProblemHandler(FeedbackHandler):
    """修改问题处理器"""
    
    @property
    def supported_types(self) -> List[FeedbackType]:
        return [FeedbackType.MODIFY]
    
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        problem = context.get("problem", {})
        changes = {}
        
        # 应用修改建议
        if feedback.suggested_fix:
            changes["description"] = feedback.suggested_fix
        if feedback.suggested_priority:
            changes["severity"] = feedback.suggested_priority
            
        return {
            "action": "modify_problem",
            "problem_id": feedback.problem_id,
            "changes": changes,
            "reason": feedback.content
        }


class ModifySolutionHandler(FeedbackHandler):
    """修改方案处理器"""
    
    @property
    def supported_types(self) -> List[FeedbackType]:
        return [FeedbackType.MODIFY_SOLUTION]
    
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        solution = feedback.suggested_solution or {}
        
        return {
            "action": "modify_solution",
            "problem_id": feedback.problem_id,
            "solution_id": feedback.solution_id,
            "changes": solution,
            "reason": feedback.content,
            "recalculate_workload": True
        }


class AddSolutionHandler(FeedbackHandler):
    """添加方案处理器"""
    
    @property
    def supported_types(self) -> List[FeedbackType]:
        return [FeedbackType.ADD_SOLUTION]
    
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        new_solution = feedback.suggested_solution or {
            "name": "用户自定义方案",
            "description": feedback.content,
            "steps": feedback.suggested_fix,
            "workload_hours": 2.0,
            "priority": 3
        }
        
        return {
            "action": "add_solution",
            "problem_id": feedback.problem_id,
            "new_solution": new_solution
        }


class AddProblemHandler(FeedbackHandler):
    """添加问题处理器"""
    
    @property
    def supported_types(self) -> List[FeedbackType]:
        return [FeedbackType.ADD_PROBLEM]
    
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        suggested = feedback.suggested_solution or {}
        
        new_problem = {
            "problem_id": f"P-U{datetime.now().strftime('%Y%m%d%H%M%S')}",
            "problem_type": suggested.get("type", "user_defined"),
            "severity": suggested.get("severity", "medium"),
            "entity_name": suggested.get("entity_name", "Unknown"),
            "file_path": suggested.get("file_path"),
            "description": feedback.content,
            "source": "user_reported",
            "solutions": suggested.get("solutions", [])
        }
        
        return {
            "action": "add_problem",
            "new_problem": new_problem
        }


class FeedbackProcessor:
    """反馈处理器"""
    
    def __init__(self):
        self.handlers: Dict[FeedbackType, FeedbackHandler] = {}
        self._register_handlers()
    
    def _register_handlers(self):
        """注册处理器"""
        handlers = [
            AgreeHandler(),
            DisagreeHandler(),
            ModifyProblemHandler(),
            ModifySolutionHandler(),
            AddSolutionHandler(),
            AddProblemHandler()
        ]
        
        for handler in handlers:
            for fb_type in handler.supported_types:
                self.handlers[fb_type] = handler
    
    def process_feedback(
        self,
        feedback: UserFeedback,
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """处理单个反馈"""
        handler = self.handlers.get(feedback.feedback_type)
        
        if not handler:
            logger.warning(f"未找到处理器: {feedback.feedback_type}")
            return {
                "action": "unknown",
                "status": "skipped"
            }
        
        try:
            result = handler.process(feedback, context)
            result["status"] = "success"
            feedback.status = FeedbackStatus.APPLIED
            feedback.applied_changes = result
            return result
        except Exception as e:
            logger.error(f"处理反馈失败: {e}")
            feedback.status = FeedbackStatus.REJECTED
            return {
                "action": "error",
                "status": "failed",
                "error": str(e)
            }
    
    def process_batch(
        self,
        batch: FeedbackBatch,
        context: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """批量处理反馈"""
        results = []
        
        for feedback in batch.feedbacks:
            fb_context = self._build_context(feedback, context)
            result = self.process_feedback(feedback, fb_context)
            results.append(result)
        
        return results
    
    def _build_context(
        self,
        feedback: UserFeedback,
        base_context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """构建反馈处理上下文"""
        context = base_context.copy()
        
        # 添加问题和方案详情
        if feedback.problem_id:
            problems = base_context.get("problems", {})
            context["problem"] = problems.get(feedback.problem_id)
        
        if feedback.solution_id:
            solutions = base_context.get("solutions", {})
            context["solution"] = solutions.get(feedback.solution_id)
        
        return context
```

### 3.2 报告重生成器

```python
# src/refactor/core/report_regenerator.py

from typing import Dict, Any, List, Optional
from pathlib import Path
import logging
from datetime import datetime
import json

from .problem_detail import (
    ProblemDetail, ProblemSummary, ProblemType, Severity,
    EnhancedRefactoringReport
)
from .enhanced_report_generator import EnhancedReportGenerator
from .feedback_processor import FeedbackProcessor, UserFeedback, FeedbackBatch

logger = logging.getLogger(__name__)


class ReportChange:
    """报告变更记录"""
    
    def __init__(self):
        self.added_problems: List[Dict] = []
        self.removed_problems: List[str] = []
        self.modified_problems: Dict[str, Dict] = {}
        self.added_solutions: Dict[str, List[Dict]] = {}
        self.modified_solutions: Dict[str, Dict] = {}
        self.priority_changes: Dict[str, str] = {}
        self.ignored_problems: List[str] = []
        self.user_comments: Dict[str, str] = {}


class ReportRegenerator:
    """报告重生成器"""
    
    def __init__(self):
        self.feedback_processor = FeedbackProcessor()
        self.report_generator = EnhancedReportGenerator()
    
    def regenerate(
        self,
        original_report: EnhancedRefactoringReport,
        feedback_batch: FeedbackBatch,
        knowledge_context: Dict[str, Any]
    ) -> EnhancedRefactoringReport:
        """
        根据用户反馈重新生成报告
        
        Args:
            original_report: 原始报告
            feedback_batch: 反馈批次
            knowledge_context: 知识上下文
            
        Returns:
            重新生成的报告
        """
        logger.info(f"开始重新生成报告，反馈数量: {len(feedback_batch.feedbacks)}")
        
        # 1. 构建处理上下文
        context = self._build_context(original_report, knowledge_context)
        
        # 2. 处理所有反馈
        change_tracker = ReportChange()
        results = self.feedback_processor.process_batch(feedback_batch, context)
        
        # 3. 应用变更
        for i, result in enumerate(results):
            feedback = feedback_batch.feedbacks[i]
            self._apply_change(result, change_tracker, feedback)
        
        # 4. 生成新报告
        new_report = self._create_new_report(
            original_report, change_tracker, feedback_batch
        )
        
        logger.info(f"报告重生成完成，变更: {self._summarize_changes(change_tracker)}")
        
        return new_report
    
    def _build_context(
        self,
        report: EnhancedRefactoringReport,
        knowledge_context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """构建处理上下文"""
        problems = {}
        solutions = {}
        
        for problem in report.problems:
            problems[problem.problem_id] = problem.model_dump()
            for i, solution in enumerate(problem.solutions):
                solutions[f"{problem.problem_id}_s{i}"] = solution
        
        return {
            "report": report,
            "problems": problems,
            "solutions": solutions,
            "knowledge_context": knowledge_context,
            "entities": knowledge_context.get("entities", {}),
            "relationships": knowledge_context.get("relationships", [])
        }
    
    def _apply_change(
        self,
        result: Dict[str, Any],
        change_tracker: ReportChange,
        feedback: UserFeedback
    ):
        """应用单个变更"""
        action = result.get("action")
        
        if action == "remove_problem":
            problem_id = result["problem_id"]
            change_tracker.removed_problems.append(problem_id)
            if result.get("add_to_ignore_list"):
                change_tracker.ignored_problems.append(problem_id)
                
        elif action == "modify_problem":
            problem_id = result["problem_id"]
            change_tracker.modified_problems[problem_id] = result["changes"]
            if feedback.content:
                change_tracker.user_comments[problem_id] = feedback.content
                
        elif action == "add_problem":
            change_tracker.added_problems.append(result["new_problem"])
            
        elif action == "modify_solution":
            key = f"{result['problem_id']}_{result['solution_id']}"
            change_tracker.modified_solutions[key] = result["changes"]
            
        elif action == "add_solution":
            problem_id = result["problem_id"]
            if problem_id not in change_tracker.added_solutions:
                change_tracker.added_solutions[problem_id] = []
            change_tracker.added_solutions[problem_id].append(result["new_solution"])
    
    def _create_new_report(
        self,
        original: EnhancedRefactoringReport,
        changes: ReportChange,
        feedback_batch: FeedbackBatch
    ) -> EnhancedRefactoringReport:
        """创建新报告"""
        
        # 复制原始问题列表
        new_problems = []
        problem_map = {p.problem_id: p for p in original.problems}
        
        # 处理移除
        remaining_ids = set(problem_map.keys()) - set(changes.removed_problems)
        
        # 处理修改
        for problem_id in remaining_ids:
            problem = problem_map[problem_id]
            problem_dict = problem.model_dump()
            
            if problem_id in changes.modified_problems:
                problem_dict.update(changes.modified_problems[problem_id])
            
            # 处理方案变更
            if problem_id in changes.added_solutions:
                problem_dict["solutions"].extend(changes.added_solutions[problem_id])
            
            if problem_id in changes.user_comments:
                problem_dict["user_comment"] = changes.user_comments[problem_id]
            
            # 重建问题对象
            new_problem = self._rebuild_problem(problem_dict)
            new_problems.append(new_problem)
        
        # 处理新增
        for new_problem_dict in changes.added_problems:
            new_problem = self._rebuild_problem(new_problem_dict)
            new_problems.append(new_problem)
        
        # 计算新统计
        new_summary = self._calculate_summary(new_problems)
        
        # 生成新报告
        new_report = EnhancedRefactoringReport(
            report_id=f"RPT-{datetime.now().strftime('%Y%m%d%H%M%S')}",
            project_name=original.project_name,
            generated_at=datetime.now(),
            summary=new_summary,
            problems=new_problems,
            version=original.version + 1,
            parent_report_id=original.report_id,
            feedback_batch_id=feedback_batch.batch_id,
            change_log=self._build_change_log(changes)
        )
        
        return new_report
    
    def _rebuild_problem(self, problem_dict: Dict) -> ProblemDetail:
        """重建问题对象"""
        return ProblemDetail(
            problem_id=problem_dict["problem_id"],
            problem_type=problem_dict.get("problem_type", "unknown"),
            severity=Severity(problem_dict.get("severity", "medium")),
            entity_name=problem_dict.get("entity_name", ""),
            file_path=problem_dict.get("file_path", ""),
            line_start=problem_dict.get("line_start"),
            line_end=problem_dict.get("line_end"),
            description=problem_dict.get("description", ""),
            current_state=problem_dict.get("current_state", {}),
            fix_steps=problem_dict.get("fix_steps", []),
            risks=problem_dict.get("risks", []),
            solutions=problem_dict.get("solutions", []),
            recommended_solution=problem_dict.get("recommended_solution"),
            user_comment=problem_dict.get("user_comment")
        )
    
    def _calculate_summary(self, problems: List[ProblemDetail]) -> ProblemSummary:
        """计算汇总"""
        severity_count = {"high": 0, "medium": 0, "low": 0}
        type_count = {}
        total_workload = 0.0
        
        for p in problems:
            severity_count[p.severity.value] += 1
            type_count[p.problem_type] = type_count.get(p.problem_type, 0) + 1
            
            if p.solutions:
                recommended = p.solutions[p.recommended_solution] if p.recommended_solution else p.solutions[0]
                total_workload += recommended.get("workload_hours", 0)
        
        return ProblemSummary(
            total_problems=len(problems),
            high_severity=severity_count["high"],
            medium_severity=severity_count["medium"],
            low_severity=severity_count["low"],
            estimated_workload_hours=total_workload,
            problem_types=type_count
        )
    
    def _build_change_log(self, changes: ReportChange) -> Dict[str, Any]:
        """构建变更日志"""
        return {
            "added_problems": len(changes.added_problems),
            "removed_problems": len(changes.removed_problems),
            "modified_problems": len(changes.modified_problems),
            "added_solutions": sum(len(v) for v in changes.added_solutions.values()),
            "modified_solutions": len(changes.modified_solutions),
            "ignored_problems": changes.ignored_problems,
            "details": {
                "removed": changes.removed_problems,
                "modified": list(changes.modified_problems.keys()),
                "added": [p["problem_id"] for p in changes.added_problems]
            }
        }
    
    def _summarize_changes(self, changes: ReportChange) -> str:
        """生成变更摘要"""
        parts = []
        if changes.added_problems:
            parts.append(f"新增{len(changes.added_problems)}个问题")
        if changes.removed_problems:
            parts.append(f"移除{len(changes.removed_problems)}个问题")
        if changes.modified_problems:
            parts.append(f"修改{len(changes.modified_problems)}个问题")
        if changes.added_solutions:
            parts.append(f"新增{sum(len(v) for v in changes.added_solutions.values())}个方案")
        return ", ".join(parts) if parts else "无变更"
```

---

## 四、用户审核流程

### 4.1 审核状态机

```python
# src/refactor/core/review_workflow.py

from enum import Enum
from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field
import logging

logger = logging.getLogger(__name__)


class ReviewStatus(str, Enum):
    """审核状态"""
    DRAFT = "draft"                    # 草稿
    PENDING_REVIEW = "pending_review"   # 待审核
    IN_REVIEW = "in_review"             # 审核中
    FEEDBACK_SUBMITTED = "feedback_submitted"  # 已提交反馈
    REGENERATING = "regenerating"       # 重生成中
    APPROVED = "approved"               # 已批准
    REJECTED = "rejected"               # 已拒绝
    FINALIZED = "finalized"             # 已定稿


class ReviewAction(str, Enum):
    """审核动作"""
    SUBMIT = "submit"           # 提交审核
    START_REVIEW = "start_review"  # 开始审核
    SUBMIT_FEEDBACK = "submit_feedback"  # 提交反馈
    REQUEST_REGENERATE = "request_regenerate"  # 请求重生成
    APPROVE = "approve"         # 批准
    REJECT = "reject"           # 拒绝
    FINALIZE = "finalize"       # 定稿


class ReviewRecord(BaseModel):
    """审核记录"""
    
    record_id: str = Field(..., description="记录ID")
    report_id: str = Field(..., description="报告ID")
    version: int = Field(default=1, description="版本号")
    
    status: ReviewStatus = Field(default=ReviewStatus.DRAFT)
    
    # 审核人信息
    reviewer_id: Optional[str] = Field(None)
    reviewer_name: Optional[str] = Field(None)
    
    # 时间戳
    created_at: datetime = Field(default_factory=datetime.now)
    submitted_at: Optional[datetime] = Field(None)
    reviewed_at: Optional[datetime] = Field(None)
    finalized_at: Optional[datetime] = Field(None)
    
    # 反馈关联
    feedback_batch_ids: List[str] = Field(default_factory=list)
    regenerate_count: int = Field(default=0)
    
    # 审核意见
    review_comments: List[Dict[str, Any]] = Field(default_factory=list)
    approval_decision: Optional[str] = Field(None)


class ReviewWorkflow:
    """审核工作流"""
    
    # 状态转换规则
    TRANSITIONS = {
        ReviewStatus.DRAFT: {
            ReviewAction.SUBMIT: ReviewStatus.PENDING_REVIEW
        },
        ReviewStatus.PENDING_REVIEW: {
            ReviewAction.START_REVIEW: ReviewStatus.IN_REVIEW
        },
        ReviewStatus.IN_REVIEW: {
            ReviewAction.SUBMIT_FEEDBACK: ReviewStatus.FEEDBACK_SUBMITTED,
            ReviewAction.APPROVE: ReviewStatus.APPROVED,
            ReviewAction.REJECT: ReviewStatus.REJECTED
        },
        ReviewStatus.FEEDBACK_SUBMITTED: {
            ReviewAction.REQUEST_REGENERATE: ReviewStatus.REGENERATING
        },
        ReviewStatus.REGENERATING: {
            ReviewAction.SUBMIT: ReviewStatus.PENDING_REVIEW
        },
        ReviewStatus.APPROVED: {
            ReviewAction.FINALIZE: ReviewStatus.FINALIZED
        },
        ReviewStatus.REJECTED: {
            ReviewAction.REQUEST_REGENERATE: ReviewStatus.REGENERATING
        }
    }
    
    def __init__(self):
        self.records: Dict[str, ReviewRecord] = {}
    
    def create_review(self, report_id: str) -> ReviewRecord:
        """创建审核记录"""
        record = ReviewRecord(
            record_id=f"REV-{datetime.now().strftime('%Y%m%d%H%M%S')}",
            report_id=report_id
        )
        self.records[record.record_id] = record
        logger.info(f"创建审核记录: {record.record_id}")
        return record
    
    def execute_action(
        self,
        record_id: str,
        action: ReviewAction,
        user_id: str = None,
        comments: str = None,
        feedback_batch_id: str = None
    ) -> ReviewRecord:
        """执行审核动作"""
        record = self.records.get(record_id)
        if not record:
            raise ValueError(f"审核记录不存在: {record_id}")
        
        current_status = record.status
        allowed_actions = self.TRANSITIONS.get(current_status, {})
        
        if action not in allowed_actions:
            raise ValueError(
                f"当前状态 [{current_status}] 不允许执行动作 [{action}]"
            )
        
        # 执行状态转换
        new_status = allowed_actions[action]
        record.status = new_status
        
        # 记录动作
        action_record = {
            "action": action.value,
            "from_status": current_status.value,
            "to_status": new_status.value,
            "user_id": user_id,
            "comments": comments,
            "timestamp": datetime.now().isoformat()
        }
        record.review_comments.append(action_record)
        
        # 更新特定字段
        if action == ReviewAction.SUBMIT:
            record.submitted_at = datetime.now()
        elif action == ReviewAction.START_REVIEW:
            record.reviewer_id = user_id
        elif action == ReviewAction.SUBMIT_FEEDBACK:
            if feedback_batch_id:
                record.feedback_batch_ids.append(feedback_batch_id)
        elif action == ReviewAction.REQUEST_REGENERATE:
            record.regenerate_count += 1
        elif action == ReviewAction.FINALIZE:
            record.finalized_at = datetime.now()
        
        logger.info(f"审核动作执行: {action.value}, 状态: {current_status} -> {new_status}")
        
        return record
    
    def get_review_history(self, record_id: str) -> List[Dict[str, Any]]:
        """获取审核历史"""
        record = self.records.get(record_id)
        if not record:
            return []
        return record.review_comments
```

### 4.2 审核界面设计

```markdown
## 审核界面设计

### Level 0: 报告概览

┌─────────────────────────────────────────────────────────────────┐
│  📊 重构分析报告 - V2                                          │
│  项目: stock_datacenter    版本: 2    状态: 待审核              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ 问题总数     │  │ 高优先级     │  │ 预计工时     │             │
│  │    75       │  │     6       │  │   78.5h     │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
│  📈 本次变更（相比V1）                                           │
│  • 移除问题: 5个（用户确认非问题）                                │
│  • 新增问题: 0个                                                 │
│  • 修改优先级: 2个                                               │
│  • 新增方案: 3个                                                 │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  [✓ 批准报告]  [✗ 拒绝]  [📝 继续反馈]  [📋 查看详情]       ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘


### Level 1: 问题列表

┌─────────────────────────────────────────────────────────────────┐
│  问题列表 (75)                    [🔍 筛选] [📊 排序] [+ 添加]  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ ☑ P001  高   大文件   Counter.java        682行   3.0h     ││
│  │   └─ 用户反馈: 同意拆分，建议保留命名规范                     ││
│  │                                                             ││
│  │ ☐ P002  高   大文件   RealRuleParse.java  825行   4.0h     ││
│  │   └─ 已忽略（用户标记）                                       ││
│  │                                                             ││
│  │ ☑ P003  高   大文件   RealDataCompute...   512行   2.5h    ││
│  │                                                             ││
│  │ ☑ P033  低   缺少文档 BaseAction.java      L18    0.5h     ││
│  │   └─ 用户方案: 使用JavaDoc标准格式                           ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  [✓ 全选] [提交反馈]  已选: 3个问题                              │
└─────────────────────────────────────────────────────────────────┘


### Level 2: 问题详情 + 反馈面板

┌─────────────────────────────────────────────────────────────────┐
│  P001: Counter.java 大文件问题                                  │
│  严重程度: 高    文件: Counter.java:1-682                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────┬───────────────────────────────┐│
│  │ 📋 问题详情                  │ 💬 用户反馈                   ││
│  │                             │                               ││
│  │ 一、是什么                   │ ┌───────────────────────────┐ ││
│  │ 文件共682行，包含1个类和     │ │ ○ 同意此问题              │ ││
│  │ 204个函数，文件过大不利于    │ │ ○ 不同意（请说明原因）    │ ││
│  │ 维护和理解。                 │ │ ● 需要修改                │ ││
│  │                             │ └───────────────────────────┘ ││
│  │ 二、怎么修复                 │                               ││
│  │ 步骤1: 分析文件结构          │ 修改说明:                     ││
│  │ 步骤2: 提取独立类            │ ┌───────────────────────────┐ ││
│  │ 步骤3: 按职责拆分            │ │ 拆分时保留原有命名规范，  │ ││
│  │                             │ │ 避免影响现有调用          │ ││
│  │ 三、有什么风险               │ └───────────────────────────┘ ││
│  │ [低风险] 依赖关系变化        │                               ││
│  │                             │ 方案选择:                     ││
│  │ 四、有什么方案               │ ┌───────────────────────────┐ ││
│  │ 1. 按职责拆分 ⭐推荐         │ │ ● 方案1: 按职责拆分      │ ││
│  │    工作量: 3.0h              │ │ ○ 方案2: 提取工具类      │ ││
│  │                             │ │ ○ 自定义方案...          │ ││
│  │ 2. 提取工具类                │ └───────────────────────────┘ ││
│  │    工作量: 2.0h              │                               ││
│  │                             │ [提交反馈]  [取消]            ││
│  └─────────────────────────────┴───────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

---

## 五、迭代优化策略

### 5.1 迭代限制

```python
class IterationConfig:
    """迭代配置"""
    
    # 最大迭代次数
    MAX_ITERATIONS = 5
    
    # 每次迭代最大反馈数
    MAX_FEEDBACKS_PER_ITERATION = 50
    
    # 自动批准阈值
    AUTO_APPROVE_THRESHOLD = 0.8  # 80%问题已确认
    
    # 时间限制
    ITERATION_TIMEOUT_HOURS = 24
    
    # 收敛判断
    CONVERGENCE_THRESHOLD = 0.1  # 变更小于10%认为已收敛
```

### 5.2 收敛检测

```python
class ConvergenceDetector:
    """收敛检测器"""
    
    def check_convergence(
        self,
        report_history: List[EnhancedRefactoringReport]
    ) -> Dict[str, Any]:
        """
        检测迭代是否收敛
        
        Returns:
            收敛状态和原因
        """
        if len(report_history) < 2:
            return {"converged": False, "reason": "insufficient_history"}
        
        current = report_history[-1]
        previous = report_history[-2]
        
        # 计算变更率
        change_rate = self._calculate_change_rate(previous, current)
        
        if change_rate < ConvergenceConfig.CONVERGENCE_THRESHOLD:
            return {
                "converged": True,
                "reason": "low_change_rate",
                "change_rate": change_rate
            }
        
        # 检查未确认问题比例
        unconfirmed_rate = self._calculate_unconfirmed_rate(current)
        
        if unconfirmed_rate < (1 - ConvergenceConfig.AUTO_APPROVE_THRESHOLD):
            return {
                "converged": True,
                "reason": "high_confirmation_rate",
                "unconfirmed_rate": unconfirmed_rate
            }
        
        return {
            "converged": False,
            "reason": "still_evolving",
            "change_rate": change_rate
        }
    
    def _calculate_change_rate(
        self,
        previous: EnhancedRefactoringReport,
        current: EnhancedRefactoringReport
    ) -> float:
        """计算变更率"""
        prev_ids = {p.problem_id for p in previous.problems}
        curr_ids = {p.problem_id for p in current.problems}
        
        # 新增和移除的问题
        added = len(curr_ids - prev_ids)
        removed = len(prev_ids - curr_ids)
        
        # 修改的问题
        modified = 0
        for p in current.problems:
            if p.problem_id in prev_ids:
                # 比较关键字段
                prev_p = next(
                    x for x in previous.problems 
                    if x.problem_id == p.problem_id
                )
                if self._is_modified(p, prev_p):
                    modified += 1
        
        total_changes = added + removed + modified
        total_problems = max(len(prev_ids), len(curr_ids))
        
        return total_changes / total_problems if total_problems > 0 else 0
```

---

## 六、API接口设计

```python
# src/refactor/api/feedback_api.py

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

router = APIRouter(prefix="/feedback", tags=["feedback"])


class FeedbackCreateRequest(BaseModel):
    """创建反馈请求"""
    session_id: str
    problem_id: Optional[str] = None
    solution_id: Optional[str] = None
    feedback_type: str
    content: str
    suggested_fix: Optional[str] = None
    suggested_solution: Optional[dict] = None


class FeedbackBatchCreateRequest(BaseModel):
    """批量创建反馈请求"""
    session_id: str
    feedbacks: List[FeedbackCreateRequest]


class RegenerateRequest(BaseModel):
    """重生成请求"""
    session_id: str
    feedback_batch_id: str


@router.post("/submit")
async def submit_feedback(request: FeedbackCreateRequest):
    """提交单个反馈"""
    # 实现提交逻辑
    pass


@router.post("/batch")
async def submit_feedback_batch(request: FeedbackBatchCreateRequest):
    """批量提交反馈"""
    # 实现批量提交逻辑
    pass


@router.post("/regenerate")
async def regenerate_report(request: RegenerateRequest):
    """根据反馈重新生成报告"""
    # 实现重生成逻辑
    pass


@router.get("/history/{session_id}")
async def get_feedback_history(session_id: str):
    """获取反馈历史"""
    # 实现历史查询逻辑
    pass


# 审核相关API

@router.post("/review/start/{session_id}")
async def start_review(session_id: str, reviewer_id: str):
    """开始审核"""
    pass


@router.post("/review/approve/{session_id}")
async def approve_report(session_id: str, comments: str = None):
    """批准报告"""
    pass


@router.post("/review/reject/{session_id}")
async def reject_report(session_id: str, reason: str):
    """拒绝报告"""
    pass


@router.post("/review/finalize/{session_id}")
async def finalize_report(session_id: str):
    """定稿报告"""
    pass
```

---

## 七、实现计划

### Phase 1: 反馈收集 (2天)

| 任务 | 说明 |
|------|------|
| 反馈数据模型 | `UserFeedback`, `FeedbackBatch` |
| 反馈处理器 | `FeedbackProcessor` 及各类Handler |
| API接口 | `/feedback/submit`, `/feedback/batch` |

### Phase 2: 报告重生成 (2天)

| 任务 | 说明 |
|------|------|
| 变更追踪 | `ReportChange` |
| 报告重生成器 | `ReportRegenerator` |
| 版本管理 | 报告版本链 |

### Phase 3: 审核流程 (2天)

| 任务 | 说明 |
|------|------|
| 审核状态机 | `ReviewWorkflow` |
| 审核API | `/review/*` |
| 审核界面 | Web模板 |

### Phase 4: 迭代优化 (1天)

| 任务 | 说明 |
|------|------|
| 收敛检测 | `ConvergenceDetector` |
| 迭代限制 | 配置和逻辑 |
| 自动批准 | 规则引擎 |

### Phase 5: 集成测试 (1天)

| 任务 | 说明 |
|------|------|
| 端到端测试 | 完整流程测试 |
| 边界测试 | 异常场景 |
| 性能测试 | 大量反馈处理 |

---

## 八、总结

本方案实现了完整的用户反馈闭环系统：

| 功能 | 说明 |
|------|------|
| **反馈收集** | 多类型、多范围、批量支持 |
| **智能处理** | 按类型分发处理器，自动应用变更 |
| **报告重生成** | 基于反馈调整问题、方案、优先级 |
| **审核流程** | 状态机驱动，支持多轮迭代 |
| **收敛检测** | 自动判断迭代是否完成 |
| **版本追溯** | 完整的报告版本链和变更日志 |
