"""
依赖关系模型 - 基于design-v1方案实现标准化的依赖关系模型
"""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field
from enum import Enum


class DependencyType(str, Enum):
    """依赖类型"""
    IMPORT = "import"
    INHERITANCE = "inheritance"
    COMPOSITION = "composition"
    AGGREGATION = "aggregation"
    ASSOCIATION = "association"
    DEPENDENCY = "dependency"
    CALL = "call"
    REFERENCE = "reference"
    EXTENSION = "extension"
    IMPLEMENTATION = "implementation"


class RiskLevel(str, Enum):
    """风险等级"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


class Dependency(BaseModel):
    """依赖关系"""
    source: str = Field(description="源实体")
    target: str = Field(description="目标实体")
    type: DependencyType = Field(description="依赖类型")
    strength: float = Field(description="依赖强度", ge=0.0, le=1.0)
    description: Optional[str] = Field(None, description="依赖描述")
    source_file: str = Field(description="源文件")
    line_number: Optional[int] = Field(None, description="行号")
    
    class Config:
        use_enum_values = True


class DependencyGraph(BaseModel):
    """依赖关系图"""
    nodes: List[str] = Field(default_factory=list, description="节点列表")
    edges: List[Dependency] = Field(default_factory=list, description="边列表")
    subgraphs: Dict[str, List[str]] = Field(default_factory=dict, description="子图")
    metrics: Dict[str, Any] = Field(default_factory=dict, description="图指标")
    
    def add_node(self, node: str):
        """添加节点"""
        if node not in self.nodes:
            self.nodes.append(node)
    
    def add_edge(self, edge: Dependency):
        """添加边"""
        self.add_node(edge.source)
        self.add_node(edge.target)
        self.edges.append(edge)
    
    def get_node_degree(self, node: str) -> int:
        """获取节点度数"""
        return len([e for e in self.edges if e.source == node or e.target == node])
    
    def get_connected_components(self) -> List[List[str]]:
        """获取连通分量"""
        # 简化的连通分量计算
        visited = set()
        components = []
        
        for node in self.nodes:
            if node not in visited:
                component = []
                stack = [node]
                
                while stack:
                    current = stack.pop()
                    if current not in visited:
                        visited.add(current)
                        component.append(current)
                        # 添加相邻节点
                        neighbors = [e.target for e in self.edges if e.source == current]
                        neighbors.extend([e.source for e in self.edges if e.target == current])
                        stack.extend(neighbors)
                
                components.append(component)
        
        return components


class RiskAssessment(BaseModel):
    """风险评估"""
    level: RiskLevel = Field(description="风险等级")
    category: str = Field(description="风险类别")
    description: str = Field(description="风险描述")
    impact: str = Field(description="影响范围")
    mitigation: str = Field(description="缓解措施")
    confidence: float = Field(description="置信度", ge=0.0, le=1.0)
    evidence: List[str] = Field(default_factory=list, description="证据列表")
    source_files: List[str] = Field(default_factory=list, description="源文件列表")
    created_at: datetime = Field(default_factory=datetime.now, description="创建时间")
    
    class Config:
        use_enum_values = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }