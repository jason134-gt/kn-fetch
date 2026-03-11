"""
业务语义模型 - 基于design-v1方案实现标准化的业务语义模型
"""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field
from enum import Enum


class BusinessEntityType(str, Enum):
    """业务实体类型"""
    USER = "user"
    PRODUCT = "product"
    ORDER = "order"
    PAYMENT = "payment"
    INVENTORY = "inventory"
    CUSTOMER = "customer"
    SUPPLIER = "supplier"
    REPORT = "report"
    ANALYTICS = "analytics"
    NOTIFICATION = "notification"
    AUTHENTICATION = "authentication"
    AUTHORIZATION = "authorization"


class BusinessOperationType(str, Enum):
    """业务操作类型"""
    CREATE = "create"
    READ = "read"
    UPDATE = "update"
    DELETE = "delete"
    VALIDATE = "validate"
    PROCESS = "process"
    NOTIFY = "notify"
    ANALYZE = "analyze"
    IMPORT = "import"
    EXPORT = "export"


class BusinessEntity(BaseModel):
    """业务实体"""
    name: str = Field(description="实体名称")
    type: BusinessEntityType = Field(description="实体类型")
    description: str = Field(description="实体描述")
    attributes: Dict[str, str] = Field(default_factory=dict, description="属性定义")
    operations: List[str] = Field(default_factory=list, description="支持的操作")
    relationships: Dict[str, str] = Field(default_factory=dict, description="关系定义")
    source_file: str = Field(description="源文件")
    confidence_score: float = Field(description="置信度分数")
    
    class Config:
        use_enum_values = True


class BusinessFlow(BaseModel):
    """业务流程"""
    name: str = Field(description="流程名称")
    description: str = Field(description="流程描述")
    steps: List[Dict[str, Any]] = Field(default_factory=list, description="流程步骤")
    participants: List[str] = Field(default_factory=list, description="参与者")
    input_data: List[str] = Field(default_factory=list, description="输入数据")
    output_data: List[str] = Field(default_factory=list, description="输出数据")
    business_rules: List[str] = Field(default_factory=list, description="业务规则")
    exceptions: List[str] = Field(default_factory=list, description="异常处理")
    source_files: List[str] = Field(default_factory=list, description="源文件列表")
    confidence_score: float = Field(description="置信度分数")


class BusinessSemanticContract(BaseModel):
    """业务语义契约"""
    project_name: str = Field(description="项目名称")
    domain: str = Field(description="业务领域")
    entities: List[BusinessEntity] = Field(default_factory=list, description="业务实体列表")
    flows: List[BusinessFlow] = Field(default_factory=list, description="业务流程列表")
    business_rules: List[str] = Field(default_factory=list, description="业务规则列表")
    constraints: List[str] = Field(default_factory=list, description="约束条件")
    quality_attributes: Dict[str, Any] = Field(default_factory=dict, description="质量属性")
    created_at: datetime = Field(default_factory=datetime.now, description="创建时间")
    updated_at: datetime = Field(default_factory=datetime.now, description="更新时间")
    
    class Config:
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }
        
    def update_timestamp(self):
        """更新时间戳"""
        self.updated_at = datetime.now()