"""
审核工作流 - 管理重构报告的审核状态和流程
"""

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
    SUBMIT = "submit"                   # 提交审核
    START_REVIEW = "start_review"       # 开始审核
    SUBMIT_FEEDBACK = "submit_feedback" # 提交反馈
    REQUEST_REGENERATE = "request_regenerate"  # 请求重生成
    APPROVE = "approve"                 # 批准
    REJECT = "reject"                   # 拒绝
    FINALIZE = "finalize"               # 定稿
    ROLLBACK = "rollback"               # 回滚


class ActionRecord(BaseModel):
    """动作记录"""
    action: str
    from_status: str
    to_status: str
    user_id: Optional[str] = None
    comments: Optional[str] = None
    timestamp: datetime = Field(default_factory=datetime.now)
    extra_data: Optional[Dict[str, Any]] = None


class ReviewRecord(BaseModel):
    """审核记录"""
    
    record_id: str = Field(..., description="记录ID")
    session_id: str = Field(..., description="会话ID")
    report_id: str = Field(..., description="报告ID")
    version: int = Field(default=1, description="版本号")
    
    status: ReviewStatus = Field(default=ReviewStatus.DRAFT, description="当前状态")
    
    # 审核人信息
    reviewer_id: Optional[str] = Field(None, description="审核人ID")
    reviewer_name: Optional[str] = Field(None, description="审核人姓名")
    
    # 时间戳
    created_at: datetime = Field(default_factory=datetime.now)
    submitted_at: Optional[datetime] = Field(None)
    reviewed_at: Optional[datetime] = Field(None)
    finalized_at: Optional[datetime] = Field(None)
    
    # 反馈关联
    feedback_batch_ids: List[str] = Field(default_factory=list, description="反馈批次ID列表")
    regenerate_count: int = Field(default=0, description="重生成次数")
    
    # 审核意见
    action_history: List[ActionRecord] = Field(default_factory=list, description="动作历史")
    approval_decision: Optional[str] = Field(None, description="批准决定")
    approval_comments: Optional[str] = Field(None, description="批准意见")


class ReviewWorkflow:
    """审核工作流"""
    
    # 状态转换规则
    TRANSITIONS = {
        ReviewStatus.DRAFT: {
            ReviewAction.SUBMIT: ReviewStatus.PENDING_REVIEW
        },
        ReviewStatus.PENDING_REVIEW: {
            ReviewAction.START_REVIEW: ReviewStatus.IN_REVIEW,
            ReviewAction.APPROVE: ReviewStatus.APPROVED,
            ReviewAction.REJECT: ReviewStatus.REJECTED
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
            ReviewAction.REQUEST_REGENERATE: ReviewStatus.REGENERATING,
            ReviewAction.SUBMIT: ReviewStatus.PENDING_REVIEW
        }
    }
    
    # 最大迭代次数
    MAX_ITERATIONS = 5
    
    def __init__(self):
        self.records: Dict[str, ReviewRecord] = {}
        self.session_records: Dict[str, List[str]] = {}  # session_id -> record_ids
    
    def create_review(
        self,
        session_id: str,
        report_id: str
    ) -> ReviewRecord:
        """创建审核记录"""
        record_id = f"REV-{datetime.now().strftime('%Y%m%d%H%M%S')}"
        
        record = ReviewRecord(
            record_id=record_id,
            session_id=session_id,
            report_id=report_id
        )
        
        self.records[record_id] = record
        
        if session_id not in self.session_records:
            self.session_records[session_id] = []
        self.session_records[session_id].append(record_id)
        
        logger.info(f"创建审核记录: {record_id}, 会话: {session_id}, 报告: {report_id}")
        return record
    
    def execute_action(
        self,
        record_id: str,
        action: ReviewAction,
        user_id: str = None,
        comments: str = None,
        feedback_batch_id: str = None,
        extra_data: Dict[str, Any] = None
    ) -> ReviewRecord:
        """执行审核动作"""
        record = self.records.get(record_id)
        if not record:
            raise ValueError(f"审核记录不存在: {record_id}")
        
        current_status = record.status
        allowed_actions = self.TRANSITIONS.get(current_status, {})
        
        if action not in allowed_actions:
            raise ValueError(
                f"当前状态 [{current_status.value}] 不允许执行动作 [{action.value}]"
            )
        
        # 检查迭代限制
        if action == ReviewAction.REQUEST_REGENERATE:
            if record.regenerate_count >= self.MAX_ITERATIONS:
                raise ValueError(f"已达到最大迭代次数 ({self.MAX_ITERATIONS})")
        
        # 执行状态转换
        new_status = allowed_actions[action]
        record.status = new_status
        
        # 记录动作
        action_record = ActionRecord(
            action=action.value,
            from_status=current_status.value,
            to_status=new_status.value,
            user_id=user_id,
            comments=comments,
            extra_data=extra_data
        )
        record.action_history.append(action_record)
        
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
        elif action == ReviewAction.APPROVE:
            record.reviewed_at = datetime.now()
            record.approval_decision = "approved"
            record.approval_comments = comments
        elif action == ReviewAction.REJECT:
            record.reviewed_at = datetime.now()
            record.approval_decision = "rejected"
            record.approval_comments = comments
        elif action == ReviewAction.FINALIZE:
            record.finalized_at = datetime.now()
        
        logger.info(f"审核动作执行: {action.value}, 状态: {current_status.value} -> {new_status.value}")
        
        return record
    
    def get_record(self, record_id: str) -> Optional[ReviewRecord]:
        """获取审核记录"""
        return self.records.get(record_id)
    
    def get_session_records(self, session_id: str) -> List[ReviewRecord]:
        """获取会话的所有审核记录"""
        record_ids = self.session_records.get(session_id, [])
        return [self.records[rid] for rid in record_ids if rid in self.records]
    
    def get_action_history(self, record_id: str) -> List[Dict[str, Any]]:
        """获取审核历史"""
        record = self.records.get(record_id)
        if not record:
            return []
        return [a.model_dump() for a in record.action_history]
    
    def can_execute(self, record_id: str, action: ReviewAction) -> bool:
        """检查是否可以执行动作"""
        record = self.records.get(record_id)
        if not record:
            return False
        
        allowed_actions = self.TRANSITIONS.get(record.status, {})
        return action in allowed_actions
    
    def get_allowed_actions(self, record_id: str) -> List[ReviewAction]:
        """获取当前允许的动作"""
        record = self.records.get(record_id)
        if not record:
            return []
        
        return list(self.TRANSITIONS.get(record.status, {}).keys())
    
    def is_finalized(self, record_id: str) -> bool:
        """检查是否已定稿"""
        record = self.records.get(record_id)
        return record and record.status == ReviewStatus.FINALIZED
    
    def get_status_info(self, record_id: str) -> Dict[str, Any]:
        """获取状态信息"""
        record = self.records.get(record_id)
        if not record:
            return {"error": "记录不存在"}
        
        return {
            "record_id": record.record_id,
            "session_id": record.session_id,
            "report_id": record.report_id,
            "version": record.version,
            "status": record.status.value,
            "reviewer_id": record.reviewer_id,
            "regenerate_count": record.regenerate_count,
            "allowed_actions": [a.value for a in self.get_allowed_actions(record_id)],
            "created_at": record.created_at.isoformat(),
            "submitted_at": record.submitted_at.isoformat() if record.submitted_at else None,
            "reviewed_at": record.reviewed_at.isoformat() if record.reviewed_at else None,
            "finalized_at": record.finalized_at.isoformat() if record.finalized_at else None
        }


class ConvergenceConfig:
    """收敛配置"""
    MAX_ITERATIONS = 5
    MAX_FEEDBACKS_PER_ITERATION = 50
    AUTO_APPROVE_THRESHOLD = 0.8
    ITERATION_TIMEOUT_HOURS = 24
    CONVERGENCE_THRESHOLD = 0.1


class ConvergenceDetector:
    """收敛检测器"""
    
    def check_convergence(
        self,
        report_versions: List[Any]
    ) -> Dict[str, Any]:
        """
        检测迭代是否收敛
        
        Args:
            report_versions: 报告版本列表
            
        Returns:
            收敛状态和原因
        """
        if len(report_versions) < 2:
            return {
                "converged": False,
                "reason": "insufficient_history",
                "message": "历史版本不足，需要至少2个版本"
            }
        
        current = report_versions[-1]
        previous = report_versions[-2]
        
        # 计算变更率
        change_rate = self._calculate_change_rate(previous, current)
        
        if change_rate < ConvergenceConfig.CONVERGENCE_THRESHOLD:
            return {
                "converged": True,
                "reason": "low_change_rate",
                "change_rate": change_rate,
                "message": f"变更率 {change_rate:.1%} 低于阈值，已收敛"
            }
        
        # 检查确认比例
        confirmation_rate = self._calculate_confirmation_rate(current)
        
        if confirmation_rate >= ConvergenceConfig.AUTO_APPROVE_THRESHOLD:
            return {
                "converged": True,
                "reason": "high_confirmation_rate",
                "confirmation_rate": confirmation_rate,
                "message": f"确认率 {confirmation_rate:.1%} 达到阈值，可自动批准"
            }
        
        # 检查忽略比例
        ignore_rate = self._calculate_ignore_rate(current)
        
        if ignore_rate > 0.5:
            return {
                "converged": True,
                "reason": "high_ignore_rate",
                "ignore_rate": ignore_rate,
                "message": f"忽略率 {ignore_rate:.1%} 较高，剩余问题已处理"
            }
        
        return {
            "converged": False,
            "reason": "still_evolving",
            "change_rate": change_rate,
            "confirmation_rate": confirmation_rate,
            "message": f"变更率 {change_rate:.1%}，继续迭代"
        }
    
    def _calculate_change_rate(self, previous: Any, current: Any) -> float:
        """计算变更率"""
        prev_problems = getattr(previous, 'problems', [])
        curr_problems = getattr(current, 'problems', [])
        
        prev_ids = {p.problem_id for p in prev_problems}
        curr_ids = {p.problem_id for p in curr_problems}
        
        # 新增和移除的问题
        added = len(curr_ids - prev_ids)
        removed = len(prev_ids - curr_ids)
        
        # 修改的问题
        modified = 0
        prev_map = {p.problem_id: p for p in prev_problems}
        for p in curr_problems:
            if p.problem_id in prev_map:
                prev_p = prev_map[p.problem_id]
                if self._is_problem_modified(p, prev_p):
                    modified += 1
        
        total_changes = added + removed + modified
        total_problems = max(len(prev_ids), len(curr_ids), 1)
        
        return total_changes / total_problems
    
    def _is_problem_modified(self, current: Any, previous: Any) -> bool:
        """检查问题是否被修改"""
        # 检查关键字段
        if current.severity != previous.severity:
            return True
        if current.description != previous.description:
            return True
        if len(current.solutions) != len(previous.solutions):
            return True
        if getattr(current, 'user_confirmed', False) != getattr(previous, 'user_confirmed', False):
            return True
        if getattr(current, 'ignored', False) != getattr(previous, 'ignored', False):
            return True
        
        return False
    
    def _calculate_confirmation_rate(self, report: Any) -> float:
        """计算确认率"""
        problems = getattr(report, 'problems', [])
        if not problems:
            return 1.0
        
        confirmed = sum(1 for p in problems if getattr(p, 'user_confirmed', False))
        ignored = sum(1 for p in problems if getattr(p, 'ignored', False))
        
        total_decided = confirmed + ignored
        return total_decided / len(problems) if problems else 1.0
    
    def _calculate_ignore_rate(self, report: Any) -> float:
        """计算忽略率"""
        problems = getattr(report, 'problems', [])
        if not problems:
            return 0.0
        
        ignored = sum(1 for p in problems if getattr(p, 'ignored', False))
        return ignored / len(problems)
    
    def should_auto_approve(self, report: Any) -> Dict[str, Any]:
        """判断是否应该自动批准"""
        confirmation_rate = self._calculate_confirmation_rate(report)
        
        if confirmation_rate >= ConvergenceConfig.AUTO_APPROVE_THRESHOLD:
            return {
                "auto_approve": True,
                "reason": "confirmation_threshold_reached",
                "confirmation_rate": confirmation_rate,
                "threshold": ConvergenceConfig.AUTO_APPROVE_THRESHOLD
            }
        
        return {
            "auto_approve": False,
            "reason": "threshold_not_reached",
            "confirmation_rate": confirmation_rate,
            "threshold": ConvergenceConfig.AUTO_APPROVE_THRESHOLD
        }
