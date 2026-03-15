"""
前端 API 路由
为前端页面提供完整的 RESTful API 接口
"""

from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import JSONResponse
from typing import Optional, List
from pydantic import BaseModel
from datetime import datetime
import random

router = APIRouter(prefix="/api", tags=["frontend"])

# ============================================================
# 数据模型
# ============================================================

class TrendData(BaseModel):
    type: str  # 'up' | 'down' | 'none'
    value: int

class DashboardStats(BaseModel):
    projectCount: int
    entityCount: int
    problemCount: int
    pendingReviewCount: int
    projectTrend: TrendData
    entityTrend: TrendData
    problemTrend: TrendData

class EntityTypeStats(BaseModel):
    type: str
    count: int
    percentage: float

class KnowledgeStats(BaseModel):
    totalEntities: int
    totalRelations: int
    entityTypes: List[EntityTypeStats]

class SeverityStats(BaseModel):
    severity: str
    count: int
    percentage: float

class RefactorStats(BaseModel):
    totalProblems: int
    bySeverity: List[SeverityStats]
    estimatedHours: int

class ExtractTaskSummary(BaseModel):
    taskId: str
    projectName: str
    status: str
    progress: int
    entityCount: int
    createdAt: str

class RefactorTaskSummary(BaseModel):
    taskId: str
    projectName: str
    status: str
    problemCount: int
    highPriorityCount: int
    estimatedHours: int
    createdAt: str

class RecentTasks(BaseModel):
    extractTasks: List[ExtractTaskSummary]
    refactorTasks: List[RefactorTaskSummary]

# ============================================================
# Dashboard API
# ============================================================

@router.get("/dashboard/stats", response_model=DashboardStats)
async def get_dashboard_stats():
    """获取仪表盘统计数据"""
    # TODO: 从数据库或文件系统读取真实数据
    # 当前返回模拟数据
    return JSONResponse(content={
        "projectCount": 12,
        "entityCount": 15420,
        "problemCount": 89,
        "pendingReviewCount": 5,
        "projectTrend": {"type": "up", "value": 15},
        "entityTrend": {"type": "up", "value": 23},
        "problemTrend": {"type": "down", "value": 8}
    })

@router.get("/dashboard/knowledge-stats", response_model=KnowledgeStats)
async def get_knowledge_stats():
    """获取知识图谱统计"""
    # TODO: 从数据库读取真实统计
    return JSONResponse(content={
        "totalEntities": 15420,
        "totalRelations": 8650,
        "entityTypes": [
            {"type": "Class", "count": 5200, "percentage": 33.7},
            {"type": "Method", "count": 6800, "percentage": 44.1},
            {"type": "Field", "count": 2100, "percentage": 13.6},
            {"type": "Interface", "count": 1320, "percentage": 8.6}
        ]
    })

@router.get("/dashboard/refactor-stats", response_model=RefactorStats)
async def get_refactor_stats():
    """获取重构问题统计"""
    # TODO: 从数据库读取真实统计
    return JSONResponse(content={
        "totalProblems": 89,
        "bySeverity": [
            {"severity": "high", "count": 12, "percentage": 13.5},
            {"severity": "medium", "count": 35, "percentage": 39.3},
            {"severity": "low", "count": 42, "percentage": 47.2}
        ],
        "estimatedHours": 156
    })

@router.get("/dashboard/recent-tasks", response_model=RecentTasks)
async def get_recent_tasks():
    """获取最近任务"""
    # TODO: 从数据库读取真实任务
    return JSONResponse(content={
        "extractTasks": [
            {
                "taskId": "ext-001",
                "projectName": "stock-service",
                "status": "completed",
                "progress": 100,
                "entityCount": 5200,
                "createdAt": "2026-03-14 10:30:00"
            },
            {
                "taskId": "ext-002",
                "projectName": "user-center",
                "status": "running",
                "progress": 65,
                "entityCount": 3200,
                "createdAt": "2026-03-15 09:00:00"
            }
        ],
        "refactorTasks": [
            {
                "taskId": "ref-001",
                "projectName": "stock-service",
                "status": "completed",
                "problemCount": 45,
                "highPriorityCount": 8,
                "estimatedHours": 80,
                "createdAt": "2026-03-14 14:00:00"
            }
        ]
    })

# ============================================================
# Extract Task API
# ============================================================

class ExtractTaskCreate(BaseModel):
    projectName: str
    projectPath: str
    languages: List[str]
    includeTestFiles: bool = False
    maxFileSize: int = 1024

class PageResult(BaseModel):
    items: List
    total: int
    page: int
    pageSize: int
    totalPages: int

@router.get("/extract/tasks")
async def get_extract_tasks(
    status: Optional[str] = Query(None),
    keyword: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取提取任务列表"""
    # TODO: 从数据库读取真实数据
    # 模拟数据
    all_tasks = [
        {
            "taskId": "ext-001",
            "projectName": "stock-service",
            "projectPath": "/projects/stock-service",
            "languages": ["Java"],
            "status": "completed",
            "progress": 100,
            "entityCount": 5200,
            "relationCount": 3800,
            "createdAt": "2026-03-14 10:30:00"
        },
        {
            "taskId": "ext-002",
            "projectName": "user-center",
            "projectPath": "/projects/user-center",
            "languages": ["Java", "Kotlin"],
            "status": "running",
            "progress": 65,
            "entityCount": 3200,
            "relationCount": 2100,
            "createdAt": "2026-03-15 09:00:00"
        },
        {
            "taskId": "ext-003",
            "projectName": "order-service",
            "projectPath": "/projects/order-service",
            "languages": ["Java"],
            "status": "pending",
            "progress": 0,
            "entityCount": 0,
            "relationCount": 0,
            "createdAt": "2026-03-15 11:00:00"
        }
    ]
    
    # 筛选
    filtered = all_tasks
    if status and status != "all":
        filtered = [t for t in filtered if t["status"] == status]
    if keyword:
        filtered = [t for t in filtered if keyword.lower() in t["projectName"].lower()]
    
    # 分页
    total = len(filtered)
    start = (page - 1) * pageSize
    end = start + pageSize
    items = filtered[start:end]
    
    return JSONResponse(content={
        "items": items,
        "total": total,
        "page": page,
        "pageSize": pageSize,
        "totalPages": (total + pageSize - 1) // pageSize
    })

@router.get("/extract/tasks/{task_id}")
async def get_extract_task_detail(task_id: str):
    """获取提取任务详情"""
    # TODO: 从数据库读取真实数据
    if task_id == "ext-001":
        return JSONResponse(content={
            "taskId": "ext-001",
            "projectName": "stock-service",
            "projectPath": "/projects/stock-service",
            "languages": ["Java"],
            "status": "completed",
            "progress": 100,
            "entityCount": 5200,
            "relationCount": 3800,
            "documentCount": 45,
            "createdAt": "2026-03-14 10:30:00",
            "updatedAt": "2026-03-14 12:45:00",
            "config": {
                "includeTestFiles": False,
                "maxFileSize": 1024,
                "excludePatterns": ["*.test.java", "*Test.java"],
                "outputDir": "/output/stock-service"
            },
            "statistics": {
                "filesProcessed": 156,
                "filesTotal": 156,
                "entitiesByType": {
                    "Class": 520,
                    "Method": 3500,
                    "Field": 1180
                }
            },
            "errors": []
        })
    else:
        raise HTTPException(status_code=404, detail="Task not found")

@router.post("/extract/tasks")
async def create_extract_task(task: ExtractTaskCreate):
    """创建提取任务"""
    # TODO: 实现真实创建逻辑
    task_id = f"ext-{random.randint(100, 999)}"
    return JSONResponse(content={
        "taskId": task_id,
        "message": "Task created successfully"
    }, status_code=201)

@router.post("/extract/tasks/{task_id}/pause")
async def pause_extract_task(task_id: str):
    """暂停提取任务"""
    # TODO: 实现真实暂停逻辑
    return JSONResponse(content={"message": "Task paused successfully"})

@router.post("/extract/tasks/{task_id}/resume")
async def resume_extract_task(task_id: str):
    """恢复提取任务"""
    # TODO: 实现真实恢复逻辑
    return JSONResponse(content={"message": "Task resumed successfully"})

@router.post("/extract/tasks/{task_id}/cancel")
async def cancel_extract_task(task_id: str):
    """取消提取任务"""
    # TODO: 实现真实取消逻辑
    return JSONResponse(content={"message": "Task cancelled successfully"})

@router.delete("/extract/tasks/{task_id}")
async def delete_extract_task(task_id: str):
    """删除提取任务"""
    # TODO: 实现真实删除逻辑
    return JSONResponse(content={"message": "Task deleted successfully"})

@router.get("/extract/tasks/{task_id}/entities")
async def get_knowledge_entities(
    task_id: str,
    type: Optional[str] = Query(None),
    keyword: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取知识实体列表"""
    # TODO: 从数据库读取真实数据
    # 模拟数据
    entities = [
        {
            "id": "ent-001",
            "type": "Class",
            "name": "StockService",
            "description": "股票服务主类",
            "filePath": "/src/main/java/com/yfzx/service/StockService.java",
            "lineStart": 15,
            "lineEnd": 120,
            "createdAt": "2026-03-14 10:35:00"
        },
        {
            "id": "ent-002",
            "type": "Method",
            "name": "getStockPrice",
            "description": "获取股票价格",
            "filePath": "/src/main/java/com/yfzx/service/StockService.java",
            "lineStart": 45,
            "lineEnd": 68,
            "createdAt": "2026-03-14 10:35:00"
        }
    ]
    
    return JSONResponse(content={
        "items": entities,
        "total": 2,
        "page": page,
        "pageSize": pageSize,
        "totalPages": 1
    })

# ============================================================
# Refactor Task API
# ============================================================

class RefactorTaskCreate(BaseModel):
    projectName: str
    knowledgePath: str

@router.get("/refactor/tasks")
async def get_refactor_tasks(
    status: Optional[str] = Query(None),
    keyword: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取重构任务列表"""
    # TODO: 从数据库读取真实数据
    all_tasks = [
        {
            "taskId": "ref-001",
            "projectName": "stock-service",
            "status": "completed",
            "problemCount": 45,
            "highSeverityCount": 8,
            "estimatedHours": 80,
            "createdAt": "2026-03-14 14:00:00"
        },
        {
            "taskId": "ref-002",
            "projectName": "user-center",
            "status": "analyzing",
            "problemCount": 0,
            "highSeverityCount": 0,
            "estimatedHours": 0,
            "createdAt": "2026-03-15 10:00:00"
        }
    ]
    
    # 筛选
    filtered = all_tasks
    if status and status != "all":
        filtered = [t for t in filtered if t["status"] == status]
    if keyword:
        filtered = [t for t in filtered if keyword.lower() in t["projectName"].lower()]
    
    # 分页
    total = len(filtered)
    start = (page - 1) * pageSize
    end = start + pageSize
    items = filtered[start:end]
    
    return JSONResponse(content={
        "items": items,
        "total": total,
        "page": page,
        "pageSize": pageSize,
        "totalPages": (total + pageSize - 1) // pageSize
    })

@router.get("/refactor/tasks/{task_id}")
async def get_refactor_report(task_id: str):
    """获取重构报告详情"""
    # TODO: 从数据库读取真实数据
    if task_id == "ref-001":
        return JSONResponse(content={
            "reportId": "rep-001",
            "taskId": "ref-001",
            "projectName": "stock-service",
            "version": 1,
            "status": "approved",
            "generatedAt": "2026-03-14 15:30:00",
            "summary": {
                "totalProblems": 45,
                "highSeverity": 8,
                "mediumSeverity": 20,
                "lowSeverity": 17,
                "estimatedHours": 80
            },
            "problems": [
                {
                    "problemId": "prob-001",
                    "problemType": "代码重复",
                    "severity": "high",
                    "module": "com.yfzx.service",
                    "entityName": "StockService",
                    "filePath": "/src/main/java/com/yfzx/service/StockService.java",
                    "lineStart": 45,
                    "lineEnd": 68,
                    "description": "发现重复的代码块",
                    "status": "confirmed",
                    "solutions": [
                        {
                            "name": "提取公共方法",
                            "description": "将重复代码提取为公共方法",
                            "steps": ["分析重复代码", "提取公共方法", "替换重复调用"],
                            "workloadHours": 2
                        }
                    ],
                    "recommendedSolution": 0
                }
            ]
        })
    else:
        raise HTTPException(status_code=404, detail="Task not found")

@router.post("/refactor/tasks")
async def create_refactor_task(task: RefactorTaskCreate):
    """创建重构分析任务"""
    # TODO: 实现真实创建逻辑
    task_id = f"ref-{random.randint(100, 999)}"
    return JSONResponse(content={
        "taskId": task_id,
        "message": "Task created successfully"
    }, status_code=201)

@router.delete("/refactor/tasks/{task_id}")
async def delete_refactor_task(task_id: str):
    """删除重构任务"""
    # TODO: 实现真实删除逻辑
    return JSONResponse(content={"message": "Task deleted successfully"})

# ============================================================
# Feedback API
# ============================================================

@router.get("/feedback/sessions")
async def get_feedback_sessions(
    status: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取反馈会话列表"""
    # TODO: 从数据库读取真实数据
    all_sessions = [
        {
            "sessionId": "fb-001",
            "projectName": "stock-service",
            "reportId": "rep-001",
            "status": "converged",
            "totalFeedbacks": 15,
            "pendingReviews": 0,
            "createdAt": "2026-03-14 16:00:00",
            "updatedAt": "2026-03-14 18:30:00"
        },
        {
            "sessionId": "fb-002",
            "projectName": "user-center",
            "reportId": "rep-002",
            "status": "in_review",
            "totalFeedbacks": 8,
            "pendingReviews": 3,
            "createdAt": "2026-03-15 10:00:00",
            "updatedAt": "2026-03-15 12:00:00"
        }
    ]
    
    # 筛选
    filtered = all_sessions
    if status and status != "all":
        filtered = [s for s in filtered if s["status"] == status]
    
    # 分页
    total = len(filtered)
    start = (page - 1) * pageSize
    end = start + pageSize
    items = filtered[start:end]
    
    return JSONResponse(content={
        "items": items,
        "total": total,
        "page": page,
        "pageSize": pageSize,
        "totalPages": (total + pageSize - 1) // pageSize
    })

@router.get("/feedback/sessions/{session_id}")
async def get_feedback_session(session_id: str):
    """获取会话详情"""
    # TODO: 从数据库读取真实数据
    if session_id == "fb-001":
        return JSONResponse(content={
            "sessionId": "fb-001",
            "projectName": "stock-service",
            "reportId": "rep-001",
            "status": "converged",
            "totalFeedbacks": 15,
            "pendingReviews": 0,
            "createdAt": "2026-03-14 16:00:00",
            "updatedAt": "2026-03-14 18:30:00"
        })
    else:
        raise HTTPException(status_code=404, detail="Session not found")

@router.post("/feedback/sessions/{session_id}/approve")
async def approve_feedback(session_id: str):
    """通过审核"""
    return JSONResponse(content={"message": "Feedback approved successfully"})

@router.post("/feedback/sessions/{session_id}/reject")
async def reject_feedback(session_id: str):
    """拒绝审核"""
    return JSONResponse(content={"message": "Feedback rejected"})

@router.post("/feedback/sessions/{session_id}/request-changes")
async def request_changes(session_id: str):
    """请求修改"""
    return JSONResponse(content={"message": "Changes requested"})

# ============================================================
# Report API
# ============================================================

@router.get("/reports")
async def get_reports(
    status: Optional[str] = Query(None),
    keyword: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取报告列表"""
    # TODO: 从数据库读取真实数据
    all_reports = [
        {
            "reportId": "rep-001",
            "projectName": "stock-service",
            "version": 1,
            "status": "approved",
            "summary": {
                "totalProblems": 45,
                "highSeverity": 8,
                "mediumSeverity": 20,
                "lowSeverity": 17,
                "estimatedHours": 80
            },
            "generatedAt": "2026-03-14 15:30:00"
        },
        {
            "reportId": "rep-002",
            "projectName": "user-center",
            "version": 1,
            "status": "pending_review",
            "summary": {
                "totalProblems": 30,
                "highSeverity": 5,
                "mediumSeverity": 15,
                "lowSeverity": 10,
                "estimatedHours": 50
            },
            "generatedAt": "2026-03-15 11:00:00"
        }
    ]
    
    # 筛选
    filtered = all_reports
    if status and status != "all":
        filtered = [r for r in filtered if r["status"] == status]
    if keyword:
        filtered = [r for r in filtered if keyword.lower() in r["projectName"].lower()]
    
    # 分页
    total = len(filtered)
    start = (page - 1) * pageSize
    end = start + pageSize
    items = filtered[start:end]
    
    return JSONResponse(content={
        "items": items,
        "total": total,
        "page": page,
        "pageSize": pageSize,
        "totalPages": (total + pageSize - 1) // pageSize
    })

@router.post("/reports/{report_id}/export")
async def export_report(report_id: str, format: str = "markdown"):
    """导出报告"""
    return JSONResponse(content={"message": "Report export started", "format": format})

@router.post("/reports/{report_id}/archive")
async def archive_report(report_id: str):
    """归档报告"""
    return JSONResponse(content={"message": "Report archived successfully"})

# ============================================================
# Settings API
# ============================================================

@router.get("/settings")
async def get_settings():
    """获取系统配置"""
    # TODO: 从配置文件读取
    return JSONResponse(content={
        "dataRetentionDays": 90,
        "autoCleanup": True,
        "maxConcurrentTasks": 3,
        "defaultLanguage": "Java",
        "outputFormat": "markdown"
    })

@router.put("/settings")
async def update_settings(settings: dict):
    """更新系统配置"""
    # TODO: 保存到配置文件
    return JSONResponse(content={"message": "Settings updated successfully"})

@router.get("/settings/ai-config")
async def get_ai_settings():
    """获取AI配置"""
    # TODO: 从配置文件读取
    return JSONResponse(content={
        "provider": "volcengine",
        "model": "doubao-pro-32k",
        "baseUrl": "",
        "toolsEnabled": False,
        "webSearchEnabled": False
    })

@router.put("/settings/ai-config")
async def update_ai_settings(config: dict):
    """更新AI配置"""
    # TODO: 保存到配置文件
    return JSONResponse(content={"message": "AI config updated successfully"})
