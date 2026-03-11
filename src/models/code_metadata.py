"""
代码元数据模型 - 基于design-v1方案实现标准化的代码元数据模型
"""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field
from enum import Enum


class CodeLanguage(str, Enum):
    """支持的编程语言"""
    PYTHON = "python"
    JAVASCRIPT = "javascript"
    TYPESCRIPT = "typescript"
    JAVA = "java"
    CPP = "cpp"
    GO = "go"
    RUST = "rust"
    PHP = "php"
    RUBY = "ruby"
    CSHARP = "csharp"
    SWIFT = "swift"
    KOTLIN = "kotlin"
    SCALA = "scala"


class CodeComplexity(BaseModel):
    """代码复杂度指标"""
    cyclomatic_complexity: int = Field(description="圈复杂度")
    cognitive_complexity: int = Field(description="认知复杂度")
    lines_of_code: int = Field(description="代码行数")
    comment_density: float = Field(description="注释密度")
    maintainability_index: float = Field(description="可维护性指数")


class FunctionMetadata(BaseModel):
    """函数元数据"""
    name: str = Field(description="函数名")
    signature: str = Field(description="函数签名")
    parameters: List[str] = Field(default_factory=list, description="参数列表")
    return_type: Optional[str] = Field(None, description="返回类型")
    docstring: Optional[str] = Field(None, description="文档字符串")
    complexity: CodeComplexity = Field(description="复杂度指标")
    start_line: int = Field(description="起始行号")
    end_line: int = Field(description="结束行号")
    is_public: bool = Field(True, description="是否公开")
    is_async: bool = Field(False, description="是否异步")
    decorators: List[str] = Field(default_factory=list, description="装饰器列表")


class ClassMetadata(BaseModel):
    """类元数据"""
    name: str = Field(description="类名")
    base_classes: List[str] = Field(default_factory=list, description="基类列表")
    docstring: Optional[str] = Field(None, description="类文档字符串")
    methods: List[FunctionMetadata] = Field(default_factory=list, description="方法列表")
    properties: List[str] = Field(default_factory=list, description="属性列表")
    complexity: CodeComplexity = Field(description="复杂度指标")
    start_line: int = Field(description="起始行号")
    end_line: int = Field(description="结束行号")
    is_abstract: bool = Field(False, description="是否抽象类")
    decorators: List[str] = Field(default_factory=list, description="装饰器列表")


class VariableMetadata(BaseModel):
    """变量元数据"""
    name: str = Field(description="变量名")
    variable_type: str = Field(description="变量类型（global/local/class）")
    value_type: str = Field(description="值类型")
    start_line: int = Field(description="起始行号")
    end_line: int = Field(description="结束行号")


class ImportMetadata(BaseModel):
    """导入元数据"""
    module_names: List[str] = Field(default_factory=list, description="模块名列表")
    imported_names: List[str] = Field(default_factory=list, description="导入名称列表")
    alias: Optional[str] = Field(None, description="别名")


class FileMetadata(BaseModel):
    """文件元数据"""
    file_path: str = Field(description="文件路径")
    language: CodeLanguage = Field(description="编程语言")
    functions: List[FunctionMetadata] = Field(default_factory=list, description="函数列表")
    classes: List[ClassMetadata] = Field(default_factory=list, description="类列表")
    variables: List[VariableMetadata] = Field(default_factory=list, description="变量列表")
    imports: List[ImportMetadata] = Field(default_factory=list, description="导入列表")
    exports: List[str] = Field(default_factory=list, description="导出列表")
    complexity: CodeComplexity = Field(description="复杂度指标")
    file_size: int = Field(description="文件大小（字节）")
    last_modified: datetime = Field(description="最后修改时间")
    encoding: str = Field("utf-8", description="文件编码")
    has_syntax_errors: bool = Field(False, description="是否有语法错误")


class CodeMetadata(BaseModel):
    """代码元数据聚合"""
    repository_info: Dict[str, Any] = Field(description="仓库信息")
    files: List[FileMetadata] = Field(default_factory=list, description="文件元数据列表")
    total_files: int = Field(description="总文件数")
    total_lines: int = Field(description="总代码行数")
    total_functions: int = Field(description="总函数数")
    total_classes: int = Field(description="总类数")
    average_complexity: CodeComplexity = Field(description="平均复杂度")
    created_at: datetime = Field(default_factory=datetime.now, description="创建时间")
    
    class Config:
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }