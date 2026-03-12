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


class ProjectCreate(BaseModel):
    """项目创建请求"""
    name: str = Field(description="项目名称")
    description: Optional[str] = Field(None, description="项目描述")
    path: str = Field(description="项目路径")
    

class ProjectResponse(BaseModel):
    """项目响应"""
    id: str = Field(description="项目ID")
    name: str = Field(description="项目名称")
    description: Optional[str] = Field(None, description="项目描述")
    path: str = Field(description="项目路径")
    created_at: datetime = Field(description="创建时间")
    updated_at: datetime = Field(description="更新时间")
    
    class Config:
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }


class ProjectListResponse(BaseModel):
    """项目列表响应"""
    projects: List[ProjectResponse] = Field(description="项目列表")
    pagination: Dict[str, Any] = Field(description="分页信息")


class AgentStatus(str, Enum):
    """Agent状态枚举"""
    IDLE = "idle"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    DISABLED = "disabled"


class AgentType(str, Enum):
    """Agent类型枚举"""
    FILE_SCANNER = "file_scanner"
    CODE_PARSER = "code_parser"
    SEMANTIC_EXTRACTOR = "semantic_extractor"
    ARCHITECTURE_ANALYZER = "architecture_analyzer"
    BUSINESS_LOGIC = "business_logic"
    DOCUMENTATION = "documentation"
    QUALITY_ASSESSOR = "quality_assessor"


class AgentResponse(BaseModel):
    """Agent响应"""
    id: str = Field(description="Agent ID")
    name: str = Field(description="Agent名称")
    type: AgentType = Field(description="Agent类型")
    status: AgentStatus = Field(description="Agent状态")
    last_execution: Optional[datetime] = Field(None, description="最后执行时间")
    metrics: Dict[str, Any] = Field(description="性能指标")
    
    class Config:
        use_enum_values = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }


class AgentListResponse(BaseModel):
    """Agent列表响应"""
    agents: List[AgentResponse] = Field(description="Agent列表")
    total_count: int = Field(description="总数")


class AgentConfigUpdate(BaseModel):
    """Agent配置更新"""
    config: Dict[str, Any] = Field(description="配置更新")


class SystemMetrics(BaseModel):
    """系统指标"""
    cpu_usage: float = Field(description="CPU使用率")
    memory_usage: float = Field(description="内存使用率")
    disk_usage: float = Field(description="磁盘使用率")
    active_connections: int = Field(description="活跃连接数")
    

class PerformanceMetrics(BaseModel):
    """性能指标"""
    average_response_time: float = Field(description="平均响应时间")
    requests_per_second: float = Field(description="每秒请求数")
    error_rate: float = Field(description="错误率")
    uptime: int = Field(description="运行时间")
    

class SystemStatusResponse(BaseModel):
    """系统状态响应"""
    application: Dict[str, Any] = Field(description="应用信息")
    system: Dict[str, Any] = Field(description="系统信息")
    performance: PerformanceMetrics = Field(description="性能指标")
    resources: Dict[str, Any] = Field(description="资源使用")


class ErrorResponse(BaseModel):
    """错误响应"""
    error: str = Field(description="错误消息")
    code: int = Field(description="错误代码")
    details: Optional[Dict[str, Any]] = Field(None, description="错误详情")
    
    class Config:
        schema_extra = {
            "example": {
                "error": "资源未找到",
                "code": 404,
                "details": {"resource_id": "12345"}
            }
        }