"""
API传输模型 - 基于design-v1方案实现标准化的API传输模型
"""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field
from enum import Enum


class TaskStatus(str, Enum):
    """任务状态"""
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


class RepositoryInfo(BaseModel):
    """仓库信息"""
    url: str = Field(description="仓库URL")
    branch: str = Field("main", description="分支")
    commit_hash: Optional[str] = Field(None, description="提交哈希")
    local_path: Optional[str] = Field(None, description="本地路径")
    is_local: bool = Field(False, description="是否为本地仓库")


class ScanConfig(BaseModel):
    """扫描配置"""
    repository: RepositoryInfo = Field(description="仓库信息")
    include_patterns: List[str] = Field(default_factory=list, description="包含模式")
    exclude_patterns: List[str] = Field(default_factory=list, description="排除模式")
    max_file_size: int = Field(10 * 1024 * 1024, description="最大文件大小（字节）")
    enable_deep_analysis: bool = Field(True, description="启用深度分析")
    enable_llm_analysis: bool = Field(True, description="启用LLM分析")
    enable_risk_assessment: bool = Field(True, description="启用风险评估")
    parallel_workers: int = Field(4, description="并行工作线程数")
    timeout_minutes: int = Field(60, description="超时时间（分钟）")


class AnalysisRequest(BaseModel):
    """分析请求"""
    task_id: str = Field(description="任务ID")
    config: ScanConfig = Field(description="扫描配置")
    callback_url: Optional[str] = Field(None, description="回调URL")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="元数据")
    created_at: datetime = Field(default_factory=datetime.now, description="创建时间")


class AnalysisResponse(BaseModel):
    """分析响应"""
    task_id: str = Field(description="任务ID")
    status: TaskStatus = Field(description="任务状态")
    progress: float = Field(description="进度", ge=0.0, le=100.0)
    result: Optional[Dict[str, Any]] = Field(None, description="分析结果")
    error_message: Optional[str] = Field(None, description="错误信息")
    started_at: Optional[datetime] = Field(None, description="开始时间")
    completed_at: Optional[datetime] = Field(None, description="完成时间")
    estimated_remaining_time: Optional[int] = Field(None, description="预计剩余时间（秒）")
    
    class Config:
        use_enum_values = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }


class HealthCheckResponse(BaseModel):
    """健康检查响应"""
    status: str = Field(description="服务状态")
    version: str = Field(description="版本号")
    uptime: int = Field(description="运行时间（秒）")
    database_status: str = Field(description="数据库状态")
    llm_status: str = Field(description="LLM服务状态")
    last_scan_time: Optional[datetime] = Field(None, description="最后扫描时间")
    active_tasks: int = Field(description="活跃任务数")
    
    class Config:
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }