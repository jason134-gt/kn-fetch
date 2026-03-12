"""
项目管理API路由 - 项目CRUD和项目分析管理
"""

from fastapi import APIRouter, HTTPException
from typing import Dict, Any, List
import uuid
from datetime import datetime

from src.models.api_models import ProjectCreate, ProjectResponse, ProjectListResponse

router = APIRouter()

# 模拟项目存储（实际应用中应该使用数据库）
projects = {}


@router.post("/projects", response_model=ProjectResponse)
async def create_project(request: ProjectCreate) -> ProjectResponse:
    """创建新项目"""
    
    project_id = str(uuid.uuid4())
    
    projects[project_id] = {
        "id": project_id,
        "name": request.name,
        "description": request.description,
        "path": request.path,
        "created_at": datetime.now(),
        "updated_at": datetime.now(),
        "analysis_count": 0,
        "last_analysis": None
    }
    
    return ProjectResponse(
        id=project_id,
        name=request.name,
        description=request.description,
        path=request.path,
        created_at=projects[project_id]["created_at"],
        updated_at=projects[project_id]["updated_at"]
    )


@router.get("/projects/{project_id}", response_model=ProjectResponse)
async def get_project(project_id: str) -> ProjectResponse:
    """获取项目详情"""
    
    if project_id not in projects:
        raise HTTPException(status_code=404, detail="项目不存在")
    
    project = projects[project_id]
    
    return ProjectResponse(
        id=project_id,
        name=project["name"],
        description=project["description"],
        path=project["path"],
        created_at=project["created_at"],
        updated_at=project["updated_at"]
    )


@router.put("/projects/{project_id}", response_model=ProjectResponse)
async def update_project(project_id: str, request: ProjectCreate) -> ProjectResponse:
    """更新项目信息"""
    
    if project_id not in projects:
        raise HTTPException(status_code=404, detail="项目不存在")
    
    project = projects[project_id]
    project["name"] = request.name
    project["description"] = request.description
    project["path"] = request.path
    project["updated_at"] = datetime.now()
    
    return ProjectResponse(
        id=project_id,
        name=project["name"],
        description=project["description"],
        path=project["path"],
        created_at=project["created_at"],
        updated_at=project["updated_at"]
    )


@router.delete("/projects/{project_id}")
async def delete_project(project_id: str) -> Dict[str, Any]:
    """删除项目"""
    
    if project_id not in projects:
        raise HTTPException(status_code=404, detail="项目不存在")
    
    del projects[project_id]
    
    return {"message": "项目删除成功"}


@router.get("/projects", response_model=ProjectListResponse)
async def list_projects(limit: int = 10, offset: int = 0) -> ProjectListResponse:
    """列出所有项目"""
    
    project_list = list(projects.values())
    
    # 分页
    paginated_projects = project_list[offset:offset + limit]
    
    return ProjectListResponse(
        projects=[
            ProjectResponse(
                id=project["id"],
                name=project["name"],
                description=project["description"],
                path=project["path"],
                created_at=project["created_at"],
                updated_at=project["updated_at"]
            ) for project in paginated_projects
        ],
        pagination={
            "total": len(project_list),
            "limit": limit,
            "offset": offset
        }
    )


@router.post("/projects/{project_id}/analyze")
async def analyze_project(project_id: str) -> Dict[str, Any]:
    """启动项目分析"""
    
    if project_id not in projects:
        raise HTTPException(status_code=404, detail="项目不存在")
    
    # 模拟分析过程
    project = projects[project_id]
    project["analysis_count"] += 1
    project["last_analysis"] = datetime.now()
    
    return {
        "message": "项目分析已启动",
        "project_id": project_id,
        "analysis_count": project["analysis_count"]
    }


@router.get("/projects/{project_id}/analyses")
async def get_project_analyses(project_id: str) -> Dict[str, Any]:
    """获取项目分析历史"""
    
    if project_id not in projects:
        raise HTTPException(status_code=404, detail="项目不存在")
    
    project = projects[project_id]
    
    # 模拟分析历史数据
    analyses = [
        {
            "id": str(uuid.uuid4()),
            "timestamp": project["last_analysis"] or datetime.now(),
            "status": "completed",
            "summary": {
                "files": 583,
                "entities": 5148,
                "complexity": "medium"
            }
        }
        for _ in range(min(project["analysis_count"], 5))
    ]
    
    return {
        "project_id": project_id,
        "analyses": analyses
    }