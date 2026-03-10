from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional, Union, Set
from enum import Enum
from datetime import datetime


class EntityType(str, Enum):
    """代码实体类型 - 基于UML扩展"""
    # 结构型实体
    CLASS = "class"
    INTERFACE = "interface"
    ENUM = "enum"
    STRUCT = "struct"
    MODULE = "module"
    PACKAGE = "package"
    NAMESPACE = "namespace"
    
    # 行为型实体
    FUNCTION = "function"
    METHOD = "method"
    CONSTRUCTOR = "constructor"
    DESTRUCTOR = "destructor"
    PROPERTY = "property"
    ATTRIBUTE = "attribute"
    
    # 数据型实体
    VARIABLE = "variable"
    CONSTANT = "constant"
    FIELD = "field"
    PARAMETER = "parameter"
    
    # 架构级实体
    FEATURE = "feature"           # 功能模块
    COMPONENT = "component"       # 组件
    SERVICE = "service"           # 服务
    API_ENDPOINT = "api_endpoint" # API端点
    ROUTE = "route"              # 路由
    
    # 通信型实体
    EVENT = "event"              # 事件/消息
    MESSAGE = "message"          # 消息
    SIGNAL = "signal"            # 信号
    
    # 错误处理实体
    EXCEPTION = "exception"      # 异常类
    ERROR_CODE = "error_code"    # 错误码
    
    # 文档型实体
    COMMENT = "comment"
    DOCSTRING = "docstring"
    TODO = "todo"
    CODE = "code"              # 代码块
    
    # 依赖型实体
    IMPORT = "import"
    EXPORT = "export"
    DEPENDENCY = "dependency"
    
    # 其他
    UNKNOWN = "unknown"


class RelationshipType(str, Enum):
    """关系类型 - 基于UML扩展"""
    # 继承与实现
    INHERITS = "inherits"           # 继承
    IMPLEMENTS = "implements"       # 实现
    EXTENDS = "extends"             # 扩展
    
    # 依赖关系
    DEPENDS_ON = "depends_on"       # 依赖
    USES = "uses"                   # 使用
    IMPORTS = "imports"             # 导入
    REFERENCES = "references"       # 引用
    
    # 调用关系
    CALLS = "calls"                 # 调用
    INVOKES = "invokes"             # 调用（方法）
    SENDS = "sends"                 # 发送（消息/事件）
    EMITS = "emits"                 # 发射（信号/事件）
    
    # 组合关系
    CONTAINS = "contains"           # 包含
    HAS = "has"                     # 拥有（组合）
    AGGREGATES = "aggregates"       # 聚合
    
    # 关联关系
    ASSOCIATES = "associates"       # 关联
    BELONGS_TO = "belongs_to"       # 属于
    
    # 数据流
    READS = "reads"                 # 读取
    WRITES = "writes"               # 写入
    FLOWS_TO = "flows_to"           # 流向
    
    # 异常处理
    THROWS = "throws"               # 抛出异常
    CATCHES = "catches"             # 捕获异常
    HANDLES = "handles"             # 处理异常
    
    # 其他
    OVERRIDES = "overrides"         # 重写
    IMPLEMENTS_METHOD = "implements_method"  # 实现方法


class Visibility(str, Enum):
    """可见性"""
    PUBLIC = "public"
    PRIVATE = "private"
    PROTECTED = "protected"
    INTERNAL = "internal"
    PACKAGE = "package"


class ExportFormat(str, Enum):
    """导出格式"""
    MARKDOWN = "markdown"
    HTML = "html"
    PDF = "pdf"
    JSON = "json"
    CSV = "csv"
    GRAPHML = "graphml"
    PLANTUML = "plantuml"
    MERMAID = "mermaid"
    MINDMAP = "mindmap"
    ARCHITECTURE = "architecture"   # 架构报告


class CodeEntity(BaseModel):
    """代码实体模型"""
    id: str = Field(description="实体唯一ID")
    entity_type: EntityType = Field(description="实体类型")
    name: str = Field(description="实体名称")
    full_name: Optional[str] = Field(default=None, description="完整限定名")
    file_path: str = Field(description="所在文件路径")
    start_line: int = Field(description="起始行号")
    end_line: int = Field(description="结束行号")
    content: str = Field(default="", description="实体内容")
    
    # 文档
    docstring: Optional[str] = Field(default=None, description="文档字符串")
    comments: List[str] = Field(default_factory=list, description="注释列表")
    
    # 修饰符
    modifiers: List[str] = Field(default_factory=list, description="修饰符列表 (static, async, abstract等)")
    visibility: Optional[Visibility] = Field(default=None, description="可见性")
    decorators: List[str] = Field(default_factory=list, description="装饰器/注解列表")
    
    # 函数/方法特有
    parameters: Optional[List[Dict[str, Any]]] = Field(default=None, description="参数列表")
    return_type: Optional[str] = Field(default=None, description="返回类型")
    raises: List[str] = Field(default_factory=list, description="可能抛出的异常")
    
    # 类特有
    bases: List[str] = Field(default_factory=list, description="基类/父类列表")
    implements: List[str] = Field(default_factory=list, description="实现的接口列表")
    attributes: List[Dict[str, Any]] = Field(default_factory=list, description="属性列表")
    
    # 层级关系
    parent_id: Optional[str] = Field(default=None, description="父实体ID")
    children_ids: List[str] = Field(default_factory=list, description="子实体ID列表")
    
    # 架构信息
    module_path: Optional[str] = Field(default=None, description="模块路径")
    package_name: Optional[str] = Field(default=None, description="包名")
    namespace: Optional[str] = Field(default=None, description="命名空间")
    
    # 复杂度指标
    complexity: Optional[int] = Field(default=None, description="圈复杂度")
    cognitive_complexity: Optional[int] = Field(default=None, description="认知复杂度")
    lines_of_code: int = Field(default=0, description="代码行数")
    nesting_depth: Optional[int] = Field(default=None, description="嵌套深度")
    
    # 调用统计
    call_count: int = Field(default=0, description="被调用次数")
    caller_count: int = Field(default=0, description="调用者数量")
    
    # 元数据
    metadata: Dict[str, Any] = Field(default_factory=dict, description="扩展元数据")
    tags: List[str] = Field(default_factory=list, description="标签")


class Relationship(BaseModel):
    """实体关系模型"""
    id: Optional[str] = Field(default=None, description="关系唯一ID")
    source_id: str = Field(description="源实体ID")
    target_id: str = Field(description="目标实体ID")
    relationship_type: RelationshipType = Field(description="关系类型")
    
    # 关系详情
    label: Optional[str] = Field(default=None, description="关系标签")
    weight: float = Field(default=1.0, description="关系权重")
    confidence: float = Field(default=1.0, description="关系置信度")
    
    # 调用特有
    call_site: Optional[str] = Field(default=None, description="调用位置")
    call_count: int = Field(default=1, description="调用次数")
    
    # 数据流
    data_type: Optional[str] = Field(default=None, description="数据类型")
    
    # 元数据
    metadata: Dict[str, Any] = Field(default_factory=dict, description="元数据")


class AnalysisResult(BaseModel):
    """单个文件分析结果"""
    file_path: str = Field(description="文件路径")
    file_hash: str = Field(description="文件哈希值")
    language: str = Field(description="编程语言")
    
    entities: List[CodeEntity] = Field(default_factory=list, description="代码实体列表")
    relationships: List[Relationship] = Field(default_factory=list, description="关系列表")
    
    # 代码统计
    lines_of_code: int = Field(default=0, description="代码行数")
    comment_lines: int = Field(default=0, description="注释行数")
    blank_lines: int = Field(default=0, description="空行数")
    
    # 导入导出
    imports: List[str] = Field(default_factory=list, description="导入列表")
    exports: List[str] = Field(default_factory=list, description="导出列表")
    
    # 时间戳
    analyzed_at: str = Field(default_factory=lambda: datetime.now().isoformat(), description="分析时间")
    
    # 错误信息
    errors: List[str] = Field(default_factory=list, description="分析错误列表")
    warnings: List[str] = Field(default_factory=list, description="警告列表")


class ModuleInfo(BaseModel):
    """模块信息"""
    name: str = Field(description="模块名称")
    path: str = Field(description="模块路径")
    
    # 统计信息
    file_count: int = Field(default=0, description="文件数量")
    entity_count: int = Field(default=0, description="实体数量")
    lines_of_code: int = Field(default=0, description="代码行数")
    
    # 依赖
    dependencies: List[str] = Field(default_factory=list, description="依赖的模块")
    dependents: List[str] = Field(default_factory=list, description="被依赖的模块")
    
    # 实体
    classes: List[str] = Field(default_factory=list, description="类列表")
    functions: List[str] = Field(default_factory=list, description="函数列表")
    interfaces: List[str] = Field(default_factory=list, description="接口列表")


class FeatureInfo(BaseModel):
    """功能模块信息"""
    id: str = Field(description="功能ID")
    name: str = Field(description="功能名称")
    description: Optional[str] = Field(default=None, description="功能描述")
    
    # 组成
    entry_points: List[str] = Field(default_factory=list, description="入口点")
    modules: List[str] = Field(default_factory=list, description="包含模块")
    entities: List[str] = Field(default_factory=list, description="包含实体")
    
    # 依赖
    depends_on: List[str] = Field(default_factory=list, description="依赖的其他功能")
    
    # 统计
    complexity: int = Field(default=0, description="功能复杂度")
    importance: float = Field(default=0.0, description="功能重要性")


class CallChain(BaseModel):
    """调用链"""
    id: str = Field(description="调用链ID")
    name: str = Field(description="调用链名称")
    entry_point: str = Field(description="入口点")
    nodes: List[str] = Field(description="节点列表（实体ID）")
    edges: List[tuple] = Field(default_factory=list, description="边列表")
    depth: int = Field(description="调用深度")
    total_calls: int = Field(description="总调用次数")


class ExceptionFlow(BaseModel):
    """异常流"""
    exception_type: str = Field(description="异常类型")
    thrown_by: List[str] = Field(default_factory=list, description="抛出位置")
    caught_by: List[str] = Field(default_factory=list, description="捕获位置")
    handled_by: List[str] = Field(default_factory=list, description="处理位置")
    propagation_path: List[str] = Field(default_factory=list, description="传播路径")


class MessageFlow(BaseModel):
    """消息/事件流"""
    event_type: str = Field(description="事件类型")
    emitters: List[str] = Field(default_factory=list, description="发送者")
    receivers: List[str] = Field(default_factory=list, description="接收者")
    handlers: List[str] = Field(default_factory=list, description="处理者")


class APIEndpoint(BaseModel):
    """API端点"""
    path: str = Field(description="API路径")
    method: str = Field(description="HTTP方法")
    handler: str = Field(description="处理函数")
    parameters: List[Dict[str, Any]] = Field(default_factory=list, description="参数")
    responses: Dict[str, Any] = Field(default_factory=dict, description="响应定义")
    middleware: List[str] = Field(default_factory=list, description="中间件")


class ArchitectureInfo(BaseModel):
    """架构信息"""
    # 模块结构
    modules: Dict[str, ModuleInfo] = Field(default_factory=dict, description="模块信息")
    
    # 功能划分
    features: Dict[str, FeatureInfo] = Field(default_factory=dict, description="功能模块")
    
    # 调用链
    call_chains: List[CallChain] = Field(default_factory=list, description="主要调用链")
    
    # 异常处理
    exception_flows: Dict[str, ExceptionFlow] = Field(default_factory=dict, description="异常流")
    
    # 消息机制
    message_flows: Dict[str, MessageFlow] = Field(default_factory=list, description="消息流")
    
    # API端点
    api_endpoints: List[APIEndpoint] = Field(default_factory=list, description="API端点")
    
    # 层级结构
    layers: Dict[str, List[str]] = Field(default_factory=dict, description="层级结构")
    
    # 统计
    statistics: Dict[str, Any] = Field(default_factory=dict, description="统计信息")


class KnowledgeGraph(BaseModel):
    """知识图谱模型"""
    entities: Dict[str, CodeEntity] = Field(default_factory=dict, description="实体字典")
    relationships: List[Relationship] = Field(default_factory=list, description="关系列表")
    
    # 项目信息
    project_name: Optional[str] = Field(default=None, description="项目名称")
    project_version: Optional[str] = Field(default=None, description="项目版本")
    project_description: Optional[str] = Field(default=None, description="项目描述")
    
    # 架构信息
    architecture: Optional[ArchitectureInfo] = Field(default=None, description="架构信息")
    
    # 时间戳
    generated_at: str = Field(default_factory=lambda: datetime.now().isoformat(), description="生成时间")
    
    # 元数据
    metadata: Dict[str, Any] = Field(default_factory=dict, description="元数据")
    
    @classmethod
    def build_from_results(cls, results: List[AnalysisResult]) -> "KnowledgeGraph":
        """从多个分析结果构建知识图谱"""
        entities = {}
        relationships = []
        
        for result in results:
            for entity in result.entities:
                entities[entity.id] = entity
            relationships.extend(result.relationships)
        
        return cls(
            entities=entities,
            relationships=relationships
        )
    
    def merge(self, other: "KnowledgeGraph") -> "KnowledgeGraph":
        """合并另一个知识图谱"""
        merged_entities = {**self.entities, **other.entities}
        
        existing_rel_keys = set()
        merged_rels = []
        
        for rel in self.relationships + other.relationships:
            key = (rel.source_id, rel.target_id, rel.relationship_type)
            if key not in existing_rel_keys:
                existing_rel_keys.add(key)
                merged_rels.append(rel)
        
        return KnowledgeGraph(
            entities=merged_entities,
            relationships=merged_rels,
            generated_at=datetime.now().isoformat()
        )
    
    def get_entity_dependencies(self, entity_id: str) -> List[CodeEntity]:
        """获取实体的所有依赖"""
        dependencies = []
        for rel in self.relationships:
            if rel.source_id == entity_id and rel.relationship_type in [
                RelationshipType.USES, RelationshipType.CALLS, 
                RelationshipType.IMPORTS, RelationshipType.DEPENDS_ON
            ]:
                if rel.target_id in self.entities:
                    dependencies.append(self.entities[rel.target_id])
        return dependencies
    
    def get_entity_usage(self, entity_id: str) -> List[CodeEntity]:
        """获取实体的所有使用位置"""
        usages = []
        for rel in self.relationships:
            if rel.target_id == entity_id and rel.relationship_type in [
                RelationshipType.USES, RelationshipType.CALLS,
                RelationshipType.IMPORTS, RelationshipType.DEPENDS_ON
            ]:
                if rel.source_id in self.entities:
                    usages.append(self.entities[rel.source_id])
        return usages
    
    def get_callers(self, entity_id: str) -> List[CodeEntity]:
        """获取调用者"""
        callers = []
        for rel in self.relationships:
            if rel.target_id == entity_id and rel.relationship_type == RelationshipType.CALLS:
                if rel.source_id in self.entities:
                    callers.append(self.entities[rel.source_id])
        return callers
    
    def get_callees(self, entity_id: str) -> List[CodeEntity]:
        """获取被调用者"""
        callees = []
        for rel in self.relationships:
            if rel.source_id == entity_id and rel.relationship_type == RelationshipType.CALLS:
                if rel.target_id in self.entities:
                    callees.append(self.entities[rel.target_id])
        return callees
    
    def get_inheritance_tree(self, entity_id: str) -> Dict[str, Any]:
        """获取继承树"""
        tree = {"id": entity_id, "children": []}
        for rel in self.relationships:
            if rel.source_id == entity_id and rel.relationship_type in [
                RelationshipType.INHERITS, RelationshipType.EXTENDS
            ]:
                if rel.target_id in self.entities:
                    tree["children"].append(self.get_inheritance_tree(rel.target_id))
        return tree
    
    def get_entities_by_type(self, entity_type: EntityType) -> List[CodeEntity]:
        """按类型获取实体"""
        return [e for e in self.entities.values() if e.entity_type == entity_type]
    
    def get_entities_by_file(self, file_path: str) -> List[CodeEntity]:
        """按文件获取实体"""
        return [e for e in self.entities.values() if e.file_path == file_path]
    
    def get_entities_by_module(self, module_path: str) -> List[CodeEntity]:
        """按模块获取实体"""
        return [e for e in self.entities.values() if e.module_path == module_path]
    
    def get_relationships_by_type(self, rel_type: RelationshipType) -> List[Relationship]:
        """按类型获取关系"""
        return [r for r in self.relationships if r.relationship_type == rel_type]
