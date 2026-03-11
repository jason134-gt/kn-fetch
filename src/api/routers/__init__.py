"""
API路由模块
"""

from .analysis import router as analysis_router
from .projects import router as projects_router
from .agents import router as agents_router
from .health import router as health_router

__all__ = ["analysis_router", "projects_router", "agents_router", "health_router"]