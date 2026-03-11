"""
FastAPI主应用 - 基于design-v1方案的RESTful API
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import logging
from typing import Dict, Any

from src.infrastructure.config_manager import get_config_manager

# 获取配置
config_manager = get_config_manager()
app_config = config_manager.get_app_config()

# 创建FastAPI应用
app = FastAPI(
    title=app_config.name,
    version=app_config.version,
    description="基于design-v1方案的代码资产扫描与语义建模系统",
    docs_url="/docs",
    redoc_url="/redoc"
)

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应该限制来源
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 配置日志
logging.basicConfig(
    level=getattr(logging, app_config.log_level.upper()),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger("api.main")


@app.on_event("startup")
async def startup_event():
    """应用启动事件"""
    logger.info(f"{app_config.name} v{app_config.version} 正在启动...")
    logger.info(f"运行环境: {app_config.environment.value}")
    logger.info(f"调试模式: {app_config.debug}")


@app.on_event("shutdown")
async def shutdown_event():
    """应用关闭事件"""
    logger.info("应用正在关闭...")


@app.get("/")
async def root() -> Dict[str, Any]:
    """根端点"""
    return {
        "message": f"欢迎使用 {app_config.name}",
        "version": app_config.version,
        "environment": app_config.environment.value,
        "status": "running"
    }


@app.get("/health")
async def health_check() -> Dict[str, Any]:
    """健康检查端点"""
    return {
        "status": "healthy",
        "timestamp": "2025-03-11T10:00:00Z",  # 应该使用实际时间
        "version": app_config.version
    }


# 导入路由
from .routers import analysis, projects, agents, health

# 注册路由
app.include_router(analysis.router, prefix="/api/v1", tags=["分析"])
app.include_router(projects.router, prefix="/api/v1", tags=["项目管理"])
app.include_router(agents.router, prefix="/api/v1", tags=["Agent管理"])
app.include_router(health.router, prefix="/api/v1", tags=["系统状态"])