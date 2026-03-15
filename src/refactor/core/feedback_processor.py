"""
反馈处理器 - 处理用户对重构报告的反馈意见
"""

from enum import Enum
from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field
from abc import ABC, abstractmethod
import logging

logger = logging.getLogger(__name__)


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


class UserFeedback(BaseModel):
    """用户反馈"""
    
    feedback_id: str = Field(..., description="反馈ID")
    session_id: str = Field(..., description="会话ID")
    
    # 反馈目标
    problem_id: Optional[str] = Field(None, description="问题ID")
    solution_id: Optional[str] = Field(None, description="方案ID")
    scope: FeedbackScope = Field(default=FeedbackScope.PROBLEM, description="反馈范围")
    
    # 反馈内容
    feedback_type: FeedbackType = Field(..., description="反馈类型")
    content: str = Field(default="", description="反馈内容")
    
    # 修改建议
    suggested_fix: Optional[str] = Field(None, description="建议的修复方式")
    suggested_solution: Optional[Dict[str, Any]] = Field(None, description="建议的方案")
    suggested_priority: Optional[str] = Field(None, description="建议的优先级")
    suggested_severity: Optional[str] = Field(None, description="建议的严重程度")
    
    # 元数据
    created_at: datetime = Field(default_factory=datetime.now)
    user_id: Optional[str] = Field(None, description="用户ID")
    context: Optional[Dict[str, Any]] = Field(None, description="上下文信息")
    
    # 处理状态
    status: FeedbackStatus = Field(default=FeedbackStatus.PENDING)
    applied_changes: Optional[Dict[str, Any]] = Field(None, description="应用的变更")
    
    class Config:
        use_enum_values = True


class FeedbackBatch(BaseModel):
    """批量反馈"""
    
    batch_id: str = Field(..., description="批次ID")
    session_id: str = Field(..., description="会话ID")
    feedbacks: List[UserFeedback] = Field(default_factory=list)
    
    # 统计
    total_count: int = Field(default=0, description="总数")
    by_type: Dict[str, int] = Field(default_factory=dict, description="按类型统计")
    by_scope: Dict[str, int] = Field(default_factory=dict, description="按范围统计")
    
    created_at: datetime = Field(default_factory=datetime.now)
    
    def add_feedback(self, feedback: UserFeedback):
        """添加反馈"""
        self.feedbacks.append(feedback)
        self.total_count = len(self.feedbacks)
        
        # 更新统计
        fb_type = feedback.feedback_type.value if isinstance(feedback.feedback_type, FeedbackType) else feedback.feedback_type
        self.by_type[fb_type] = self.by_type.get(fb_type, 0) + 1
        
        scope = feedback.scope.value if isinstance(feedback.scope, FeedbackScope) else feedback.scope
        self.by_scope[scope] = self.by_scope.get(scope, 0) + 1


class FeedbackHandler(ABC):
    """反馈处理器基类"""
    
    @property
    @abstractmethod
    def supported_types(self) -> List[FeedbackType]:
        """支持的反馈类型"""
        pass
    
    @abstractmethod
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        """处理反馈"""
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
        changes = {}
        
        if feedback.suggested_fix:
            changes["description"] = feedback.suggested_fix
        if feedback.suggested_priority:
            changes["severity"] = feedback.suggested_priority
        if feedback.suggested_severity:
            changes["severity"] = feedback.suggested_severity
        if feedback.content:
            changes["user_comment"] = feedback.content
            
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
            "steps": [feedback.suggested_fix] if feedback.suggested_fix else [],
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


class IgnoreProblemHandler(FeedbackHandler):
    """忽略问题处理器"""
    
    @property
    def supported_types(self) -> List[FeedbackType]:
        return [FeedbackType.IGNORE_PROBLEM]
    
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "action": "ignore_problem",
            "problem_id": feedback.problem_id,
            "reason": feedback.content,
            "keep_in_report": True,
            "mark_as_ignored": True
        }


class RaisePriorityHandler(FeedbackHandler):
    """提高优先级处理器"""
    
    @property
    def supported_types(self) -> List[FeedbackType]:
        return [FeedbackType.RAISE_PRIORITY]
    
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        problem = context.get("problem", {})
        current = problem.get("severity", "medium")
        
        priority_map = {"low": "medium", "medium": "high", "high": "high"}
        new_priority = priority_map.get(current, "medium")
        
        return {
            "action": "modify_problem",
            "problem_id": feedback.problem_id,
            "changes": {"severity": new_priority},
            "reason": feedback.content or "用户要求提高优先级"
        }


class LowerPriorityHandler(FeedbackHandler):
    """降低优先级处理器"""
    
    @property
    def supported_types(self) -> List[FeedbackType]:
        return [FeedbackType.LOWER_PRIORITY]
    
    def process(self, feedback: UserFeedback, context: Dict[str, Any]) -> Dict[str, Any]:
        problem = context.get("problem", {})
        current = problem.get("severity", "medium")
        
        priority_map = {"high": "medium", "medium": "low", "low": "low"}
        new_priority = priority_map.get(current, "medium")
        
        return {
            "action": "modify_problem",
            "problem_id": feedback.problem_id,
            "changes": {"severity": new_priority},
            "reason": feedback.content or "用户要求降低优先级"
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
            AddProblemHandler(),
            IgnoreProblemHandler(),
            RaisePriorityHandler(),
            LowerPriorityHandler()
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
                "status": "skipped",
                "feedback_id": feedback.feedback_id
            }
        
        try:
            result = handler.process(feedback, context)
            result["status"] = "success"
            result["feedback_id"] = feedback.feedback_id
            feedback.status = FeedbackStatus.APPLIED
            feedback.applied_changes = result
            return result
        except Exception as e:
            logger.error(f"处理反馈失败: {e}")
            feedback.status = FeedbackStatus.REJECTED
            return {
                "action": "error",
                "status": "failed",
                "error": str(e),
                "feedback_id": feedback.feedback_id
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
        
        if feedback.problem_id:
            problems = base_context.get("problems", {})
            context["problem"] = problems.get(feedback.problem_id)
        
        if feedback.solution_id:
            solutions = base_context.get("solutions", {})
            context["solution"] = solutions.get(feedback.solution_id)
        
        return context


def create_feedback(
    session_id: str,
    problem_id: str = None,
    feedback_type: FeedbackType = FeedbackType.AGREE,
    content: str = "",
    **kwargs
) -> UserFeedback:
    """创建反馈的便捷函数"""
    feedback_id = f"FB-{datetime.now().strftime('%Y%m%d%H%M%S')}-{problem_id or 'global'}"
    
    return UserFeedback(
        feedback_id=feedback_id,
        session_id=session_id,
        problem_id=problem_id,
        feedback_type=feedback_type,
        content=content,
        **kwargs
    )


def create_feedback_batch(session_id: str, feedbacks: List[UserFeedback] = None) -> FeedbackBatch:
    """创建反馈批次"""
    batch_id = f"FB-BATCH-{datetime.now().strftime('%Y%m%d%H%M%S')}"
    
    batch = FeedbackBatch(
        batch_id=batch_id,
        session_id=session_id,
        feedbacks=feedbacks or []
    )
    
    return batch
