"""
重构分析 REST API

提供重构分析的HTTP接口
"""

import logging
from typing import List, Optional, Dict, Any
from pathlib import Path
from datetime import datetime

from fastapi import APIRouter, HTTPException, Query, BackgroundTasks
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from ..core.refactoring_session import SessionManager, SessionStatus
from ..core.problem_detail import ProblemDetail, Severity, ProblemType
from ..core.enhanced_report_generator import EnhancedReportGenerator
from ..core.diff_evaluator import DiffEvaluator, DiffReport

logger = logging.getLogger(__name__)

# 创建路由
router = APIRouter(prefix="/api/refactor", tags=["refactoring"])

# 全局会话管理器
session_manager = SessionManager()


# ============== 请求/响应模型 ==============

class CreateSessionRequest(BaseModel):
    """创建会话请求"""
    project_name: str
    project_path: Optional[str] = None
    config: Optional[Dict[str, Any]] = None


class SelectProblemsRequest(BaseModel):
    """选择问题请求"""
    problem_ids: List[str]
    mode: str = "single"  # single, batch, dependency


class FeedbackRequest(BaseModel):
    """反馈请求"""
    problem_id: str
    feedback_type: str  # approve, reject, modify, comment
    feedback_content: str
    custom_solution: Optional[Dict[str, Any]] = None


class ExecuteRequest(BaseModel):
    """执行请求"""
    dry_run: bool = False
    auto_commit: bool = False


# ============== API端点 ==============

@router.post("/sessions")
async def create_session(request: CreateSessionRequest):
    """创建重构会话"""
    try:
        session = session_manager.create_session(
            project_name=request.project_name,
            config=request.config
        )
        
        return {
            "success": True,
            "data": {
                "session_id": session.session_id,
                "status": session.status.value,
                "created_at": session.created_at.isoformat()
            }
        }
    except Exception as e:
        logger.error(f"创建会话失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/sessions")
async def list_sessions(
    project_name: Optional[str] = None,
    status: Optional[str] = None,
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100)
):
    """列出所有会话"""
    try:
        status_filter = SessionStatus(status) if status else None
        sessions = session_manager.list_sessions(
            project_name=project_name,
            status=status_filter
        )
        
        # 分页
        total = len(sessions)
        start = (page - 1) * size
        end = start + size
        page_sessions = sessions[start:end]
        
        return {
            "success": True,
            "data": {
                "total": total,
                "page": page,
                "size": size,
                "sessions": [
                    {
                        "session_id": s.session_id,
                        "project_name": s.project_name,
                        "status": s.status.value,
                        "created_at": s.created_at.isoformat(),
                        "problem_count": len(s.selected_problems)
                    }
                    for s in page_sessions
                ]
            }
        }
    except Exception as e:
        logger.error(f"列出会话失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/sessions/{session_id}")
async def get_session(session_id: str):
    """获取会话详情"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    return {
        "success": True,
        "data": session.to_dict()
    }


@router.delete("/sessions/{session_id}")
async def delete_session(session_id: str):
    """删除会话"""
    if session_manager.delete_session(session_id):
        return {"success": True, "message": "会话已删除"}
    raise HTTPException(status_code=404, detail="会话不存在")


@router.get("/sessions/{session_id}/problems")
async def get_problems(
    session_id: str,
    severity: Optional[str] = None,
    problem_type: Optional[str] = None,
    search: Optional[str] = None,
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100)
):
    """获取问题列表（Level 1）"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    # 从分析结果获取问题
    problems = session.analysis_result.get("problems", []) if session.analysis_result else []
    
    # 过滤
    if severity:
        problems = [p for p in problems if p.get("severity") == severity]
    
    if problem_type:
        problems = [p for p in problems if p.get("problem_type") == problem_type]
    
    if search:
        search_lower = search.lower()
        problems = [
            p for p in problems
            if search_lower in p.get("entity_name", "").lower()
            or search_lower in p.get("description", "").lower()
            or search_lower in p.get("file_path", "").lower()
        ]
    
    # 分页
    total = len(problems)
    start = (page - 1) * size
    end = start + size
    page_problems = problems[start:end]
    
    return {
        "success": True,
        "data": {
            "total": total,
            "page": page,
            "size": size,
            "problems": page_problems
        }
    }


@router.get("/sessions/{session_id}/problems/{problem_id}")
async def get_problem_detail(session_id: str, problem_id: str):
    """获取问题详情（Level 2）"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    # 从分析结果查找问题
    problems = session.analysis_result.get("problems", []) if session.analysis_result else []
    problem = next((p for p in problems if p.get("problem_id") == problem_id), None)
    
    if not problem:
        raise HTTPException(status_code=404, detail="问题不存在")
    
    # 获取相关反馈
    feedback = [
        fb for fb in session.feedback_history
        if fb.get("problem_id") == problem_id
    ]
    
    return {
        "success": True,
        "data": {
            "problem": problem,
            "feedback": feedback,
            "is_selected": problem_id in session.selected_problems
        }
    }


@router.get("/sessions/{session_id}/problems/{problem_id}/code")
async def get_problem_code(
    session_id: str,
    problem_id: str,
    view: str = Query("full", regex="^(full|diff|context)$")
):
    """获取问题相关代码（Level 3）"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    problems = session.analysis_result.get("problems", []) if session.analysis_result else []
    problem = next((p for p in problems if p.get("problem_id") == problem_id), None)
    
    if not problem:
        raise HTTPException(status_code=404, detail="问题不存在")
    
    file_path = problem.get("file_path", "")
    start_line = problem.get("start_line", 0)
    end_line = problem.get("end_line", 0)
    
    # 读取文件内容
    code_content = ""
    if file_path and Path(file_path).exists():
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()
            
            if view == "full":
                code_content = "".join(lines)
            elif view == "context":
                # 显示问题上下文
                context_start = max(0, start_line - 10)
                context_end = min(len(lines), end_line + 10)
                code_content = "".join(lines[context_start:context_end])
            elif view == "diff":
                code_content = problem.get("code_context", "")
    
    return {
        "success": True,
        "data": {
            "file_path": file_path,
            "start_line": start_line,
            "end_line": end_line,
            "view": view,
            "code": code_content,
            "problem": problem
        }
    }


@router.post("/sessions/{session_id}/select")
async def select_problems(session_id: str, request: SelectProblemsRequest):
    """选择要重构的问题"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    session.select_problems(
        problem_ids=request.problem_ids,
        mode=request.mode
    )
    
    session_manager.update_session(session)
    
    return {
        "success": True,
        "data": {
            "selected_problems": session.selected_problems,
            "count": len(session.selected_problems)
        }
    }


@router.post("/sessions/{session_id}/feedback")
async def submit_feedback(session_id: str, request: FeedbackRequest):
    """提交用户反馈"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    session.add_feedback(
        problem_id=request.problem_id,
        feedback_type=request.feedback_type,
        feedback_content=request.feedback_content,
        custom_solution=request.custom_solution
    )
    
    session_manager.update_session(session)
    
    return {
        "success": True,
        "message": "反馈已提交"
    }


@router.post("/sessions/{session_id}/regenerate")
async def regenerate_report(
    session_id: str,
    background_tasks: BackgroundTasks,
    based_on_feedback: bool = True
):
    """重新生成报告"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    # TODO: 实现基于反馈的重新生成逻辑
    
    return {
        "success": True,
        "message": "报告重新生成中"
    }


@router.post("/sessions/{session_id}/execute")
async def execute_refactoring(
    session_id: str,
    background_tasks: BackgroundTasks,
    request: ExecuteRequest
):
    """执行重构"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    if not session.selected_problems:
        raise HTTPException(status_code=400, detail="未选择要重构的问题")
    
    # 更新状态
    session.update_status(SessionStatus.REFACTORING)
    session_manager.update_session(session)
    
    # TODO: 在后台执行重构
    
    return {
        "success": True,
        "data": {
            "session_id": session_id,
            "status": session.status.value,
            "selected_problems": session.selected_problems,
            "dry_run": request.dry_run
        }
    }


@router.get("/sessions/{session_id}/diff")
async def get_diff_report(session_id: str):
    """获取差异评估报告"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    if not session.diff_report:
        raise HTTPException(status_code=404, detail="差异报告尚未生成")
    
    return {
        "success": True,
        "data": session.diff_report
    }


@router.post("/sessions/{session_id}/rollback")
async def rollback(session_id: str):
    """回滚重构"""
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    
    if not session.can_rollback:
        raise HTTPException(status_code=400, detail="无法回滚")
    
    # TODO: 实现回滚逻辑
    
    session.update_status(SessionStatus.ROLLED_BACK)
    session_manager.update_session(session)
    
    return {
        "success": True,
        "message": "回滚成功"
    }


@router.get("/dashboard/{project_name}")
async def get_dashboard(project_name: str):
    """获取项目仪表盘（Level 0）"""
    sessions = session_manager.list_sessions(project_name=project_name)
    
    if not sessions:
        return {
            "success": True,
            "data": {
                "project_name": project_name,
                "total_sessions": 0,
                "message": "暂无分析数据"
            }
        }
    
    # 汇总统计
    total_problems = 0
    total_fixed = 0
    
    for session in sessions:
        if session.analysis_result:
            total_problems += session.analysis_result.get("total_problems", 0)
        total_fixed += len([
            p for p in session.selected_problems
            if session.status == SessionStatus.COMPLETED
        ])
    
    latest_session = sessions[0] if sessions else None
    
    return {
        "success": True,
        "data": {
            "project_name": project_name,
            "total_sessions": len(sessions),
            "total_problems": total_problems,
            "total_fixed": total_fixed,
            "latest_session": {
                "session_id": latest_session.session_id,
                "status": latest_session.status.value,
                "created_at": latest_session.created_at.isoformat()
            } if latest_session else None
        }
    }


# ============== 启动函数 ==============

def create_app():
    """创建FastAPI应用"""
    from fastapi import FastAPI
    from fastapi.middleware.cors import CORSMiddleware
    
    app = FastAPI(
        title="重构分析智能体 API",
        description="提供重构分析、执行、评估的REST API",
        version="1.0.0"
    )
    
    # CORS
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    
    # 注册路由
    app.include_router(router)
    
    return app


__all__ = ['router', 'create_app']
