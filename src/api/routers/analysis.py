"""
分析API路由 - 代码分析和知识提取相关接口
"""

from fastapi import APIRouter, HTTPException, BackgroundTasks
from typing import Dict, Any, List
import uuid
from datetime import datetime

from src.models.api_models import AnalysisRequest, AnalysisResponse, TaskStatus

router = APIRouter()

# 模拟任务存储（实际应用中应该使用数据库）
analysis_tasks = {}


@router.post("/analyze", response_model=AnalysisResponse)
async def analyze_code(request: AnalysisRequest, background_tasks: BackgroundTasks) -> AnalysisResponse:
    """启动代码分析任务"""
    
    # 生成任务ID
    task_id = str(uuid.uuid4())
    
    # 创建任务记录
    analysis_tasks[task_id] = {
        "task_id": task_id,
        "status": TaskStatus.PENDING,
        "created_at": datetime.now(),
        "request": request.dict(),
        "progress": 0,
        "result": None,
        "error": None
    }
    
    # 在后台启动分析任务
    background_tasks.add_task(execute_analysis, task_id, request)
    
    return AnalysisResponse(
        task_id=task_id,
        status=TaskStatus.PENDING,
        message="分析任务已启动"
    )


@router.get("/analyze/{task_id}", response_model=AnalysisResponse)
async def get_analysis_status(task_id: str) -> AnalysisResponse:
    """获取分析任务状态"""
    
    if task_id not in analysis_tasks:
        raise HTTPException(status_code=404, detail="任务不存在")
    
    task = analysis_tasks[task_id]
    
    return AnalysisResponse(
        task_id=task_id,
        status=task["status"],
        progress=task["progress"],
        result=task["result"],
        error=task["error"],
        message=_get_status_message(task["status"])
    )


@router.get("/analyze/{task_id}/result")
async def get_analysis_result(task_id: str) -> Dict[str, Any]:
    """获取分析结果"""
    
    if task_id not in analysis_tasks:
        raise HTTPException(status_code=404, detail="任务不存在")
    
    task = analysis_tasks[task_id]
    
    if task["status"] != TaskStatus.COMPLETED:
        raise HTTPException(status_code=400, detail="任务尚未完成")
    
    return {
        "task_id": task_id,
        "result": task["result"],
        "metadata": {
            "created_at": task["created_at"],
            "completed_at": task.get("completed_at")
        }
    }


@router.delete("/analyze/{task_id}")
async def cancel_analysis(task_id: str) -> Dict[str, Any]:
    """取消分析任务"""
    
    if task_id not in analysis_tasks:
        raise HTTPException(status_code=404, detail="任务不存在")
    
    task = analysis_tasks[task_id]
    
    if task["status"] in [TaskStatus.COMPLETED, TaskStatus.FAILED]:
        raise HTTPException(status_code=400, detail="任务已结束，无法取消")
    
    task["status"] = TaskStatus.CANCELLED
    task["error"] = "任务已被用户取消"
    
    return {"message": "任务已取消"}


@router.get("/analyze")
async def list_analysis_tasks(limit: int = 10, offset: int = 0) -> Dict[str, Any]:
    """列出分析任务"""
    
    tasks = list(analysis_tasks.values())
    
    # 分页
    paginated_tasks = tasks[offset:offset + limit]
    
    return {
        "tasks": paginated_tasks,
        "pagination": {
            "total": len(tasks),
            "limit": limit,
            "offset": offset
        }
    }


async def execute_analysis(task_id: str, request: AnalysisRequest):
    """执行分析任务（后台任务）"""
    
    try:
        # 更新任务状态为运行中
        analysis_tasks[task_id]["status"] = TaskStatus.RUNNING
        analysis_tasks[task_id]["progress"] = 10
        
        # 模拟分析过程
        # 这里应该调用实际的Agent工作流
        await _simulate_analysis_process(task_id, request)
        
        # 分析完成
        analysis_tasks[task_id]["status"] = TaskStatus.COMPLETED
        analysis_tasks[task_id]["progress"] = 100
        analysis_tasks[task_id]["completed_at"] = datetime.now()
        
    except Exception as e:
        # 分析失败
        analysis_tasks[task_id]["status"] = TaskStatus.FAILED
        analysis_tasks[task_id]["error"] = str(e)


async def _simulate_analysis_process(task_id: str, request: AnalysisRequest):
    """模拟分析过程"""
    
    # 模拟文件扫描
    analysis_tasks[task_id]["progress"] = 20
    
    # 模拟代码解析
    analysis_tasks[task_id]["progress"] = 40
    
    # 模拟语义分析
    analysis_tasks[task_id]["progress"] = 60
    
    # 模拟架构分析
    analysis_tasks[task_id]["progress"] = 80
    
    # 生成结果
    analysis_tasks[task_id]["result"] = {
        "summary": {
            "total_files": 583,
            "total_entities": 5148,
            "total_lines": 18553
        },
        "architecture": {
            "modules": 25,
            "dependencies": 328,
            "design_patterns": ["singleton", "factory"]
        },
        "business_flows": [
            {
                "name": "用户下单流程",
                "complexity": "medium",
                "entities": ["User", "Order", "Product"]
            }
        ]
    }


def _get_status_message(status: TaskStatus) -> str:
    """获取状态消息"""
    messages = {
        TaskStatus.PENDING: "任务等待执行",
        TaskStatus.RUNNING: "任务正在执行",
        TaskStatus.COMPLETED: "任务执行完成",
        TaskStatus.FAILED: "任务执行失败",
        TaskStatus.CANCELLED: "任务已被取消"
    }
    return messages.get(status, "未知状态")