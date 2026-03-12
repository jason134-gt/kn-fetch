from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional, Union
from enum import Enum
from datetime import datetime

class EntityType(str, Enum):
    """代码实体类型"""
    CLASS = "class"
    FUNCTION = "function"
    METHOD = "method"
    MODULE = "module"
    VARIABLE = "variable"
    CONSTANT = "constant"
    INTERFACE = "interface"
    ENUM = "enum"
    COMMENT = "comment"
    DOCSTRING = "docstring"
    IMPORT = "import"

class ExportFormat(str, Enum):
    """导出格式"""
    MARKDOWN = "markdown"
    HTML = "html"
    PDF = "pdf"
    JSON = "json"
    CSV = "csv"
    GRAPHML = "graphml"
    PLANTUML = "plantuml"
    MINDMAP = "mindmap"

class CodeEntity(BaseModel):
    """代码实体模型"""
    id: str = Field(description="实体唯一ID")
    entity_type: EntityType = Field(description="实体类型")
    name: str = Field(description="实体名称")
    file_path: str = Field(description="所在文件路径")
    start_line: int = Field(description="起始行号")
    end_line: int = Field(description="结束行号")
    content: str = Field(description="实体内容")
    docstring: Optional[str] = Field(default=None, description="文档字符串")
    modifiers: List[str] = Field(default_factory=list, description="修饰符列表")
    parameters: Optional[List[Dict[str, Any]]] = Field(default=None, description="参数列表（函数/方法）")
    return_type: Optional[str] = Field(default=None, description="返回类型")
    parent_id: Optional[str] = Field(default=None, description="父实体ID")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="元数据")
    complexity: Optional[int] = Field(default=None, description="圈复杂度")
    lines_of_code: int = Field(default=0, description="代码行数")

class Relationship(BaseModel):
    """实体关系模型"""
    source_id: str = Field(description="源实体ID")
    target_id: str = Field(description="目标实体ID")
    relationship_type: str = Field(description="关系类型: calls, inherits, implements, uses, imports, etc.")
    confidence: float = Field(default=1.0, description="关系置信度")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="元数据")

class AnalysisResult(BaseModel):
    """单个文件分析结果"""
    file_path: str = Field(description="文件路径")
    file_hash: str = Field(description="文件哈希值")
    language: str = Field(description="编程语言")
    entities: List[CodeEntity] = Field(default_factory=list, description="代码实体列表")
    relationships: List[Relationship] = Field(default_factory=list, description="关系列表")
    lines_of_code: int = Field(default=0, description="代码行数")
    comment_lines: int = Field(default=0, description="注释行数")
    blank_lines: int = Field(default=0, description="空行数")
    analyzed_at: str = Field(default_factory=lambda: datetime.now().isoformat(), description="分析时间")
    errors: List[str] = Field(default_factory=list, description="分析错误列表")

class KnowledgeGraph(BaseModel):
    """知识图谱模型"""
    entities: Dict[str, CodeEntity] = Field(default_factory=dict, description="实体字典，key为实体ID")
    relationships: List[Relationship] = Field(default_factory=list, description="关系列表")
    project_name: Optional[str] = Field(default=None, description="项目名称")
    project_version: Optional[str] = Field(default=None, description="项目版本")
    generated_at: str = Field(default_factory=lambda: datetime.now().isoformat(), description="生成时间")
    
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
        
        # 合并关系，去重
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
            if rel.source_id == entity_id and rel.relationship_type in ["uses", "calls", "imports"]:
                if rel.target_id in self.entities:
                    dependencies.append(self.entities[rel.target_id])
        return dependencies
    
    def get_entity_usage(self, entity_id: str) -> List[CodeEntity]:
        """获取实体的所有使用位置"""
        usages = []
        for rel in self.relationships:
            if rel.target_id == entity_id and rel.relationship_type in ["uses", "calls", "imports"]:
                if rel.source_id in self.entities:
                    usages.append(self.entities[rel.source_id])
        return usages
