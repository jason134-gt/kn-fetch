"""
反馈API接口 - 提供REST API用于用户反馈和审核操作
"""

from typing import Optional, List, Dict, Any
from datetime import datetime
from pathlib import Path
from pydantic import BaseModel, Field
import json
import logging

# 本地导入
import sys
from pathlib import Path as PathLib
sys.path.insert(0, str(PathLib(__file__).parent.parent.parent))

from src.refactor.core.feedback_processor import (
    UserFeedback, FeedbackBatch, FeedbackType, FeedbackScope,
    create_feedback, create_feedback_batch
)
from src.refactor.core.report_regenerator import ReportRegenerator
from src.refactor.core.review_workflow import (
    ReviewWorkflow, ReviewAction, ReviewStatus
)
from src.refactor.core.problem_detail import EnhancedRefactoringReport

logger = logging.getLogger(__name__)


# ==================== 请求模型 ====================

class FeedbackCreateRequest(BaseModel):
    """创建反馈请求"""
    session_id: str = Field(..., description="会话ID")
    problem_id: Optional[str] = Field(None, description="问题ID")
    solution_id: Optional[str] = Field(None, description="方案ID")
    scope: str = Field(default="problem", description="反馈范围")
    feedback_type: str = Field(..., description="反馈类型")
    content: str = Field(default="", description="反馈内容")
    suggested_fix: Optional[str] = Field(None, description="建议的修复方式")
    suggested_solution: Optional[Dict[str, Any]] = Field(None, description="建议的方案")
    suggested_priority: Optional[str] = Field(None, description="建议的优先级")


class FeedbackBatchCreateRequest(BaseModel):
    """批量创建反馈请求"""
    session_id: str = Field(..., description="会话ID")
    feedbacks: List[FeedbackCreateRequest] = Field(..., description="反馈列表")


class RegenerateRequest(BaseModel):
    """重生成请求"""
    session_id: str = Field(..., description="会话ID")
    feedback_batch_id: str = Field(..., description="反馈批次ID")


class ReviewActionRequest(BaseModel):
    """审核动作请求"""
    session_id: str = Field(..., description="会话ID")
    record_id: str = Field(..., description="审核记录ID")
    action: str = Field(..., description="审核动作")
    user_id: Optional[str] = Field(None, description="用户ID")
    comments: Optional[str] = Field(None, description="评论")
    feedback_batch_id: Optional[str] = Field(None, description="反馈批次ID")


# ==================== 响应模型 ====================

class ApiResponse(BaseModel):
    """API响应"""
    success: bool = Field(..., description="是否成功")
    message: str = Field(default="", description="消息")
    data: Optional[Dict[str, Any]] = Field(None, description="数据")


# ==================== 服务类 ====================

class FeedbackService:
    """反馈服务"""
    
    def __init__(self, output_dir: str = "output/refactor"):
        self.output_dir = Path(output_dir)
        self.regenerator = ReportRegenerator()
        self.workflow = ReviewWorkflow()
        
        # 会话存储
        self.sessions: Dict[str, Dict[str, Any]] = {}
        self.reports: Dict[str, EnhancedRefactoringReport] = {}
        self.report_versions: Dict[str, List[EnhancedRefactoringReport]] = {}
        self.feedback_batches: Dict[str, FeedbackBatch] = {}
    
    def create_session(self, project_name: str) -> Dict[str, Any]:
        """创建会话"""
        session_id = f"SESSION-{datetime.now().strftime('%Y%m%d%H%M%S')}"
        
        session = {
            "session_id": session_id,
            "project_name": project_name,
            "created_at": datetime.now().isoformat(),
            "status": "created"
        }
        
        self.sessions[session_id] = session
        logger.info(f"创建会话: {session_id}")
        
        return session
    
    def get_session(self, session_id: str) -> Optional[Dict[str, Any]]:
        """获取会话"""
        return self.sessions.get(session_id)
    
    def load_report(self, session_id: str, report_path: str) -> Dict[str, Any]:
        """加载报告"""
        try:
            with open(report_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            report = EnhancedRefactoringReport(**data)
            report_id = report.report_id
            
            self.reports[report_id] = report
            
            if session_id not in self.report_versions:
                self.report_versions[session_id] = []
            self.report_versions[session_id].append(report)
            
            logger.info(f"加载报告: {report_id}, 会话: {session_id}")
            
            return {
                "report_id": report_id,
                "project_name": report.project_name,
                "version": report.version,
                "total_problems": report.summary.total_problems
            }
        except Exception as e:
            logger.error(f"加载报告失败: {e}")
            raise
    
    def submit_feedback(self, request: FeedbackCreateRequest) -> Dict[str, Any]:
        """提交单个反馈"""
        try:
            feedback_type = FeedbackType(request.feedback_type)
        except ValueError:
            feedback_type = FeedbackType.MODIFY
        
        feedback = create_feedback(
            session_id=request.session_id,
            problem_id=request.problem_id,
            feedback_type=feedback_type,
            content=request.content,
            suggested_fix=request.suggested_fix,
            suggested_solution=request.suggested_solution,
            suggested_priority=request.suggested_priority
        )
        
        return {
            "feedback_id": feedback.feedback_id,
            "session_id": feedback.session_id,
            "problem_id": feedback.problem_id,
            "feedback_type": feedback.feedback_type,
            "status": "created"
        }
    
    def submit_feedback_batch(self, request: FeedbackBatchCreateRequest) -> Dict[str, Any]:
        """批量提交反馈"""
        batch = create_feedback_batch(request.session_id)
        
        for fb in request.feedbacks:
            try:
                feedback_type = FeedbackType(fb.feedback_type)
            except ValueError:
                feedback_type = FeedbackType.MODIFY
            
            feedback = create_feedback(
                session_id=fb.session_id,
                problem_id=fb.problem_id,
                feedback_type=feedback_type,
                content=fb.content,
                suggested_fix=fb.suggested_fix,
                suggested_solution=fb.suggested_solution,
                suggested_priority=fb.suggested_priority
            )
            batch.add_feedback(feedback)
        
        self.feedback_batches[batch.batch_id] = batch
        
        logger.info(f"提交反馈批次: {batch.batch_id}, 数量: {batch.total_count}")
        
        return {
            "batch_id": batch.batch_id,
            "session_id": batch.session_id,
            "total_count": batch.total_count,
            "by_type": batch.by_type,
            "status": "created"
        }
    
    def regenerate_report(self, request: RegenerateRequest) -> Dict[str, Any]:
        """根据反馈重新生成报告"""
        batch = self.feedback_batches.get(request.feedback_batch_id)
        if not batch:
            raise ValueError(f"反馈批次不存在: {request.feedback_batch_id}")
        
        # 获取最新报告
        versions = self.report_versions.get(request.session_id, [])
        if not versions:
            raise ValueError(f"会话没有关联的报告: {request.session_id}")
        
        original_report = versions[-1]
        
        # 重新生成
        new_report = self.regenerator.regenerate(original_report, batch)
        
        # 保存
        self.reports[new_report.report_id] = new_report
        self.report_versions[request.session_id].append(new_report)
        
        # 保存到文件
        project_name = new_report.project_name
        output_dir = self.output_dir / project_name
        self.regenerator.save_report(new_report, output_dir)
        
        logger.info(f"重新生成报告: {new_report.report_id}, 版本: {new_report.version}")
        
        return {
            "report_id": new_report.report_id,
            "version": new_report.version,
            "parent_report_id": new_report.parent_report_id,
            "change_log": new_report.change_log,
            "total_problems": new_report.summary.total_problems
        }
    
    def get_feedback_history(self, session_id: str) -> List[Dict[str, Any]]:
        """获取反馈历史"""
        batches = [
            b.model_dump() 
            for b in self.feedback_batches.values() 
            if b.session_id == session_id
        ]
        return batches
    
    def get_report_versions(self, session_id: str) -> List[Dict[str, Any]]:
        """获取报告版本列表"""
        versions = self.report_versions.get(session_id, [])
        return [
            {
                "report_id": r.report_id,
                "version": r.version,
                "generated_at": r.generated_at.isoformat(),
                "total_problems": r.summary.total_problems,
                "change_log": r.change_log
            }
            for r in versions
        ]


class ReviewService:
    """审核服务"""
    
    def __init__(self):
        self.workflow = ReviewWorkflow()
    
    def create_review(self, session_id: str, report_id: str) -> Dict[str, Any]:
        """创建审核"""
        record = self.workflow.create_review(session_id, report_id)
        return self.workflow.get_status_info(record.record_id)
    
    def execute_action(self, request: ReviewActionRequest) -> Dict[str, Any]:
        """执行审核动作"""
        try:
            action = ReviewAction(request.action)
        except ValueError:
            raise ValueError(f"无效的审核动作: {request.action}")
        
        record = self.workflow.execute_action(
            record_id=request.record_id,
            action=action,
            user_id=request.user_id,
            comments=request.comments,
            feedback_batch_id=request.feedback_batch_id
        )
        
        return self.workflow.get_status_info(record.record_id)
    
    def get_review_status(self, record_id: str) -> Dict[str, Any]:
        """获取审核状态"""
        return self.workflow.get_status_info(record_id)
    
    def get_allowed_actions(self, record_id: str) -> List[str]:
        """获取允许的动作"""
        actions = self.workflow.get_allowed_actions(record_id)
        return [a.value for a in actions]
    
    def get_action_history(self, record_id: str) -> List[Dict[str, Any]]:
        """获取动作历史"""
        return self.workflow.get_action_history(record_id)


# ==================== API路由模拟 ====================

class FeedbackApi:
    """反馈API（模拟FastAPI路由）"""
    
    def __init__(self):
        self.feedback_service = FeedbackService()
        self.review_service = ReviewService()
    
    # === 会话管理 ===
    
    def create_session(self, project_name: str) -> ApiResponse:
        """POST /sessions"""
        try:
            session = self.feedback_service.create_session(project_name)
            return ApiResponse(
                success=True,
                message="会话创建成功",
                data=session
            )
        except Exception as e:
            return ApiResponse(success=False, message=str(e))
    
    def get_session(self, session_id: str) -> ApiResponse:
        """GET /sessions/{session_id}"""
        session = self.feedback_service.get_session(session_id)
        if session:
            return ApiResponse(success=True, data=session)
        return ApiResponse(success=False, message="会话不存在")
    
    # === 反馈管理 ===
    
    def submit_feedback(self, request: FeedbackCreateRequest) -> ApiResponse:
        """POST /feedback"""
        try:
            result = self.feedback_service.submit_feedback(request)
            return ApiResponse(
                success=True,
                message="反馈提交成功",
                data=result
            )
        except Exception as e:
            return ApiResponse(success=False, message=str(e))
    
    def submit_feedback_batch(self, request: FeedbackBatchCreateRequest) -> ApiResponse:
        """POST /feedback/batch"""
        try:
            result = self.feedback_service.submit_feedback_batch(request)
            return ApiResponse(
                success=True,
                message=f"批量反馈提交成功，共{result['total_count']}条",
                data=result
            )
        except Exception as e:
            return ApiResponse(success=False, message=str(e))
    
    def get_feedback_history(self, session_id: str) -> ApiResponse:
        """GET /feedback/history/{session_id}"""
        history = self.feedback_service.get_feedback_history(session_id)
        return ApiResponse(
            success=True,
            data={"history": history, "total": len(history)}
        )
    
    # === 报告管理 ===
    
    def load_report(self, session_id: str, report_path: str) -> ApiResponse:
        """POST /reports/load"""
        try:
            result = self.feedback_service.load_report(session_id, report_path)
            return ApiResponse(
                success=True,
                message="报告加载成功",
                data=result
            )
        except Exception as e:
            return ApiResponse(success=False, message=str(e))
    
    def regenerate_report(self, request: RegenerateRequest) -> ApiResponse:
        """POST /reports/regenerate"""
        try:
            result = self.feedback_service.regenerate_report(request)
            return ApiResponse(
                success=True,
                message=f"报告重新生成成功，版本{result['version']}",
                data=result
            )
        except Exception as e:
            return ApiResponse(success=False, message=str(e))
    
    def get_report_versions(self, session_id: str) -> ApiResponse:
        """GET /reports/versions/{session_id}"""
        versions = self.feedback_service.get_report_versions(session_id)
        return ApiResponse(
            success=True,
            data={"versions": versions, "total": len(versions)}
        )
    
    # === 审核管理 ===
    
    def create_review(self, session_id: str, report_id: str) -> ApiResponse:
        """POST /review/create"""
        try:
            result = self.review_service.create_review(session_id, report_id)
            return ApiResponse(
                success=True,
                message="审核创建成功",
                data=result
            )
        except Exception as e:
            return ApiResponse(success=False, message=str(e))
    
    def execute_review_action(self, request: ReviewActionRequest) -> ApiResponse:
        """POST /review/action"""
        try:
            result = self.review_service.execute_action(request)
            return ApiResponse(
                success=True,
                message=f"动作执行成功，当前状态: {result['status']}",
                data=result
            )
        except Exception as e:
            return ApiResponse(success=False, message=str(e))
    
    def get_review_status(self, record_id: str) -> ApiResponse:
        """GET /review/status/{record_id}"""
        result = self.review_service.get_review_status(record_id)
        if "error" in result:
            return ApiResponse(success=False, message=result["error"])
        return ApiResponse(success=True, data=result)
    
    def get_allowed_actions(self, record_id: str) -> ApiResponse:
        """GET /review/actions/{record_id}"""
        actions = self.review_service.get_allowed_actions(record_id)
        return ApiResponse(success=True, data={"allowed_actions": actions})


# 便捷函数
def create_api() -> FeedbackApi:
    """创建API实例"""
    return FeedbackApi()
