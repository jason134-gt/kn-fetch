#!/usr/bin/env python3
"""
前端 API 服务 - 独立版本
不依赖其他模块，直接启动
"""
import uvicorn
from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional, List
from pydantic import BaseModel
import sys
from pathlib import Path

# 添加项目路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root / "src"))

# 直接导入数据存储层，避免 __init__.py 中 streamlit 的导入
import importlib.util
spec = importlib.util.spec_from_file_location("data_store", project_root / "src" / "web" / "data_store.py")
data_store_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(data_store_module)

knowledge_store = data_store_module.knowledge_store
refactor_store = data_store_module.refactor_store
session_store = data_store_module.session_store
dashboard_store = data_store_module.dashboard_store

# 创建 FastAPI 应用
app = FastAPI(
    title="KN-Fetch 前端 API",
    description="为前端页面提供数据接口",
    version="1.0.0"
)

# 添加 CORS 中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源
    allow_credentials=True,
    allow_methods=["*"],  # 允许所有方法
    allow_headers=["*"],  # 允许所有头
)

# ============================================================
# 数据模型
# ============================================================

class TrendData(BaseModel):
    type: str
    value: int

# ============================================================
# Dashboard API
# ============================================================

@app.get("/api/dashboard/stats")
async def get_dashboard_stats():
    """获取仪表盘统计数据"""
    try:
        stats = dashboard_store.get_dashboard_stats()
        return stats
    except Exception as e:
        print(f"Error getting dashboard stats: {e}")
        # 返回默认值
        return {
            "projectCount": 0,
            "entityCount": 0,
            "problemCount": 0,
            "pendingReviewCount": 0,
            "projectTrend": {"type": "none", "value": 0},
            "entityTrend": {"type": "none", "value": 0},
            "problemTrend": {"type": "none", "value": 0}
        }

@app.get("/api/dashboard/knowledge-stats")
async def get_knowledge_stats():
    """获取知识图谱统计"""
    try:
        stats = dashboard_store.get_knowledge_stats()
        return stats
    except Exception as e:
        print(f"Error getting knowledge stats: {e}")
        return {
            "totalEntities": 0,
            "totalRelations": 0,
            "entityTypes": []
        }

@app.get("/api/dashboard/refactor-stats")
async def get_refactor_stats():
    """获取重构问题统计"""
    try:
        stats = dashboard_store.get_refactor_stats()
        return stats
    except Exception as e:
        print(f"Error getting refactor stats: {e}")
        return {
            "totalProblems": 0,
            "bySeverity": [],
            "estimatedHours": 0
        }

@app.get("/api/dashboard/recent-tasks")
async def get_recent_tasks():
    """获取最近任务"""
    try:
        tasks = dashboard_store.get_recent_tasks()
        return tasks
    except Exception as e:
        print(f"Error getting recent tasks: {e}")
        return {
            "extractTasks": [],
            "refactorTasks": []
        }

# ============================================================
# Extract Task API
# ============================================================

@app.get("/api/extract/tasks")
async def get_extract_tasks(
    status: Optional[str] = Query(None),
    keyword: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取提取任务列表"""
    try:
        projects = knowledge_store.get_all_projects()
        
        # 转换为任务格式
        all_tasks = []
        for p in projects:
            all_tasks.append({
                "taskId": f"ext-{p['project_name']}",
                "projectName": p["project_name"],
                "projectPath": p.get("file_path", ""),
                "languages": ["Java"],  # 默认Java
                "status": "completed",
                "progress": 100,
                "entityCount": p["entity_count"],
                "relationCount": 0,
                "createdAt": p.get("modified_at", "")
            })
        
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
        
        return {
            "items": items,
            "total": total,
            "page": page,
            "pageSize": pageSize,
            "totalPages": (total + pageSize - 1) // pageSize if total > 0 else 0
        }
    except Exception as e:
        print(f"Error getting extract tasks: {e}")
        return {"items": [], "total": 0, "page": page, "pageSize": pageSize, "totalPages": 0}

@app.get("/api/extract/tasks/{task_id}")
async def get_extract_task_detail(task_id: str):
    """获取提取任务详情"""
    try:
        # 从 task_id 提取项目名
        if task_id.startswith("ext-"):
            project_name = task_id[4:]
        else:
            project_name = task_id
        
        # 获取项目的实体统计
        entities_data = knowledge_store.get_project_entities(project_name, page=1, page_size=1000)
        
        if not entities_data["items"]:
            raise HTTPException(status_code=404, detail="Task not found")
        
        # 统计实体类型
        entities = entities_data["items"]
        entity_types = {}
        for entity in entities:
            et = entity.get("entity_type", "unknown")
            entity_types[et] = entity_types.get(et, 0) + 1
        
        return {
            "taskId": task_id,
            "projectName": project_name,
            "projectPath": f"/projects/{project_name}",
            "languages": ["Java"],
            "status": "completed",
            "progress": 100,
            "entityCount": entities_data["total"],
            "relationCount": 0,
            "documentCount": 0,
            "createdAt": "",
            "updatedAt": "",
            "config": {
                "includeTestFiles": False,
                "maxFileSize": 1024,
                "excludePatterns": [],
                "outputDir": f"/output/{project_name}"
            },
            "statistics": {
                "filesProcessed": 0,
                "filesTotal": 0,
                "entitiesByType": entity_types
            },
            "errors": []
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error getting extract task detail: {e}")
        raise HTTPException(status_code=404, detail="Task not found")

@app.post("/api/extract/tasks")
async def create_extract_task(task: dict):
    """创建提取任务"""
    import uuid
    task_id = f"ext-{uuid.uuid4().hex[:8]}"
    return {"taskId": task_id, "message": "Task created successfully"}

@app.post("/api/extract/tasks/{task_id}/pause")
async def pause_extract_task(task_id: str):
    """暂停提取任务"""
    return {"message": "Task paused successfully"}

@app.post("/api/extract/tasks/{task_id}/resume")
async def resume_extract_task(task_id: str):
    """恢复提取任务"""
    return {"message": "Task resumed successfully"}

@app.post("/api/extract/tasks/{task_id}/cancel")
async def cancel_extract_task(task_id: str):
    """取消提取任务"""
    return {"message": "Task cancelled successfully"}

@app.delete("/api/extract/tasks/{task_id}")
async def delete_extract_task(task_id: str):
    """删除提取任务"""
    return {"message": "Task deleted successfully"}

@app.get("/api/extract/tasks/{task_id}/entities")
async def get_knowledge_entities(
    task_id: str,
    type: Optional[str] = Query(None),
    keyword: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取知识实体列表"""
    try:
        # 从 task_id 提取项目名
        if task_id.startswith("ext-"):
            project_name = task_id[4:]
        else:
            project_name = task_id
        
        result = knowledge_store.get_project_entities(
            project_name,
            entity_type=type,
            keyword=keyword,
            page=page,
            page_size=pageSize
        )
        
        # 转换实体格式
        items = []
        for entity in result["items"]:
            items.append({
                "id": entity.get("id"),
                "type": entity.get("entity_type"),
                "name": entity.get("name"),
                "description": entity.get("docstring") or "",
                "filePath": entity.get("file_path"),
                "lineStart": entity.get("start_line"),
                "lineEnd": entity.get("end_line"),
                "createdAt": ""
            })
        
        return {
            "items": items,
            "total": result["total"],
            "page": result["page"],
            "pageSize": result["page_size"],
            "totalPages": result["total_pages"]
        }
    except Exception as e:
        print(f"Error getting knowledge entities: {e}")
        return {"items": [], "total": 0, "page": page, "pageSize": pageSize, "totalPages": 0}

# ============================================================
# Refactor Task API
# ============================================================

@app.get("/api/refactor/tasks")
async def get_refactor_tasks(
    status: Optional[str] = Query(None),
    keyword: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取重构任务列表"""
    try:
        reports = refactor_store.get_all_reports()
        
        # 转换为任务格式
        all_tasks = []
        for r in reports:
            all_tasks.append({
                "taskId": f"ref-{r['project_name']}",
                "projectName": r["project_name"],
                "status": "completed" if r["status"] == "approved" else "analyzing",
                "problemCount": r["total_problems"],
                "highSeverityCount": r["high_severity"],
                "estimatedHours": r["estimated_hours"],
                "createdAt": r["generated_at"]
            })
        
        # 筛选和分页
        filtered = all_tasks
        if status and status != "all":
            filtered = [t for t in filtered if t["status"] == status]
        if keyword:
            filtered = [t for t in filtered if keyword.lower() in t["projectName"].lower()]
        
        total = len(filtered)
        start = (page - 1) * pageSize
        end = start + pageSize
        items = filtered[start:end]
        
        return {
            "items": items,
            "total": total,
            "page": page,
            "pageSize": pageSize,
            "totalPages": (total + pageSize - 1) // pageSize if total > 0 else 0
        }
    except Exception as e:
        print(f"Error getting refactor tasks: {e}")
        return {"items": [], "total": 0, "page": page, "pageSize": pageSize, "totalPages": 0}

@app.get("/api/refactor/tasks/{task_id}")
async def get_refactor_report(task_id: str):
    """获取重构报告详情"""
    try:
        # 从 task_id 提取项目名
        if task_id.startswith("ref-"):
            project_name = task_id[4:]
        else:
            project_name = task_id
        
        report = refactor_store.get_report_by_project(project_name)
        
        if not report:
            raise HTTPException(status_code=404, detail="Report not found")
        
        # 转换报告格式
        summary = report.get("summary", {})
        problems = report.get("problems", [])
        
        return {
            "reportId": report.get("report_id"),
            "taskId": task_id,
            "projectName": report.get("project_name"),
            "version": report.get("version", 1),
            "status": report.get("status", "draft"),
            "generatedAt": report.get("generated_at"),
            "summary": {
                "totalProblems": summary.get("total_problems", 0),
                "highSeverity": summary.get("high_severity", 0),
                "mediumSeverity": summary.get("medium_severity", 0),
                "lowSeverity": summary.get("low_severity", 0),
                "estimatedHours": summary.get("estimated_workload_hours", 0)
            },
            "problems": [
                {
                    "problemId": p.get("problem_id"),
                    "problemType": p.get("problem_type"),
                    "severity": p.get("severity"),
                    "module": p.get("module", ""),
                    "entityName": p.get("entity_name"),
                    "filePath": p.get("file_path"),
                    "lineStart": p.get("line_start"),
                    "lineEnd": p.get("line_end"),
                    "description": p.get("description"),
                    "status": "confirmed" if p.get("user_confirmed") else "pending",
                    "solutions": p.get("solutions", []),
                    "recommendedSolution": p.get("recommended_solution", 0)
                }
                for p in problems
            ]
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error getting refactor report: {e}")
        raise HTTPException(status_code=404, detail="Report not found")

# ============================================================
# Feedback API
# ============================================================

@app.get("/api/feedback/sessions")
async def get_feedback_sessions(
    status: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取反馈会话列表"""
    try:
        sessions = session_store.get_all_sessions()
        
        # 筛选
        filtered = sessions
        if status and status != "all":
            filtered = [s for s in filtered if s["status"] == status]
        
        # 分页
        total = len(filtered)
        start = (page - 1) * pageSize
        end = start + pageSize
        items = filtered[start:end]
        
        return {
            "items": items,
            "total": total,
            "page": page,
            "pageSize": pageSize,
            "totalPages": (total + pageSize - 1) // pageSize if total > 0 else 0
        }
    except Exception as e:
        print(f"Error getting feedback sessions: {e}")
        return {"items": [], "total": 0, "page": page, "pageSize": pageSize, "totalPages": 0}

# ============================================================
# Report API
# ============================================================

@app.get("/api/reports")
async def get_reports(
    status: Optional[str] = Query(None),
    keyword: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    pageSize: int = Query(10, ge=1, le=100)
):
    """获取报告列表"""
    try:
        reports = refactor_store.get_all_reports()
        
        # 筛选
        filtered = reports
        if status and status != "all":
            filtered = [r for r in filtered if r["status"] == status]
        if keyword:
            filtered = [r for r in filtered if keyword.lower() in r["project_name"].lower()]
        
        # 分页
        total = len(filtered)
        start = (page - 1) * pageSize
        end = start + pageSize
        items = filtered[start:end]
        
        # 转换格式
        result_items = [
            {
                "reportId": r["report_id"],
                "projectName": r["project_name"],
                "version": r["version"],
                "status": r["status"],
                "summary": {
                    "totalProblems": r["total_problems"],
                    "highSeverity": r["high_severity"],
                    "mediumSeverity": r["medium_severity"],
                    "lowSeverity": r["low_severity"],
                    "estimatedHours": r["estimated_hours"]
                },
                "generatedAt": r["generated_at"]
            }
            for r in items
        ]
        
        return {
            "items": result_items,
            "total": total,
            "page": page,
            "pageSize": pageSize,
            "totalPages": (total + pageSize - 1) // pageSize if total > 0 else 0
        }
    except Exception as e:
        print(f"Error getting reports: {e}")
        return {"items": [], "total": 0, "page": page, "pageSize": pageSize, "totalPages": 0}

# ============================================================
# Settings API
# ============================================================

@app.get("/api/settings")
async def get_settings():
    """获取系统配置"""
    return {
        "dataRetentionDays": 90,
        "autoCleanup": True,
        "maxConcurrentTasks": 3,
        "defaultLanguage": "Java",
        "outputFormat": "markdown"
    }

@app.put("/api/settings")
async def update_settings(settings: dict):
    """更新系统配置"""
    return {"message": "Settings updated successfully"}

@app.get("/api/settings/ai-config")
async def get_ai_settings():
    """获取AI配置"""
    return {
        "provider": "volcengine",
        "model": "doubao-pro-32k",
        "baseUrl": "",
        "toolsEnabled": False,
        "webSearchEnabled": False
    }

@app.put("/api/settings/ai-config")
async def update_ai_settings(config: dict):
    """更新AI配置"""
    return {"message": "AI config updated successfully"}

# ============================================================
# 主程序
# ============================================================

if __name__ == "__main__":
    print("=" * 70)
    print("KN-Fetch 前端 API 服务")
    print("=" * 70)
    print(f"服务地址: http://127.0.0.1:8000")
    print(f"API 文档: http://127.0.0.1:8000/docs")
    print(f"前端应用: http://localhost:3000")
    print("=" * 70)
    uvicorn.run(app, host="127.0.0.1", port=8000)
