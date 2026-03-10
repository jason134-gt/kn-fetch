"""
UML图生成器 - 生成PlantUML和Mermaid格式的UML图
支持：类图、时序图、流程图、组件图、部署图等
"""
from typing import List, Dict, Any, Optional, Set
from collections import defaultdict

from ..gitnexus.models import (
    KnowledgeGraph, CodeEntity, EntityType, Relationship, RelationshipType,
    ArchitectureInfo, ModuleInfo, CallChain, ExceptionFlow
)


class UMLGenerator:
    """UML图生成器"""
    
    def __init__(self, graph: KnowledgeGraph, architecture: Optional[ArchitectureInfo] = None):
        self.graph = graph
        self.architecture = architecture
    
    def generate_class_diagram(self, format: str = "plantuml", 
                                entities: Optional[List[str]] = None,
                                max_entities: int = 50) -> str:
        """
        生成类图
        Args:
            format: 输出格式 ("plantuml" 或 "mermaid")
            entities: 指定实体ID列表，为None时自动选择
            max_entities: 最大实体数量
        """
        if format == "mermaid":
            return self._generate_class_diagram_mermaid(entities, max_entities)
        return self._generate_class_diagram_plantuml(entities, max_entities)
    
    def _generate_class_diagram_plantuml(self, entities: Optional[List[str]], max_entities: int) -> str:
        """生成PlantUML类图"""
        lines = ["@startuml", "skinparam classAttributeIconSize 0"]
        
        # 选择要展示的类
        if entities:
            class_entities = [self.graph.entities.get(eid) for eid in entities if eid in self.graph.entities]
        else:
            class_entities = [
                e for e in self.graph.entities.values()
                if e.entity_type in [EntityType.CLASS, EntityType.INTERFACE, EntityType.ENUM]
            ]
            # 按重要性排序（被调用次数）
            class_entities.sort(key=lambda e: len(self._get_callers(e.id)), reverse=True)
            class_entities = class_entities[:max_entities]
        
        # 生成类定义
        for entity in class_entities:
            if not entity:
                continue
            
            # 类声明
            if entity.entity_type == EntityType.INTERFACE:
                lines.append(f"interface {entity.name} {{")
            elif entity.entity_type == EntityType.ENUM:
                lines.append(f"enum {entity.name} {{")
            else:
                lines.append(f"class {entity.name} {{")
            
            # 添加属性
            for attr in entity.attributes:
                visibility = self._get_uml_visibility(attr.get('visibility', 'public'))
                attr_type = attr.get('type', '')
                attr_name = attr.get('name', '')
                if attr_type:
                    lines.append(f"  {visibility}{attr_name} : {attr_type}")
                else:
                    lines.append(f"  {visibility}{attr_name}")
            
            # 添加方法
            for child_id in entity.children_ids:
                child = self.graph.entities.get(child_id)
                if child and child.entity_type in [EntityType.METHOD, EntityType.FUNCTION]:
                    visibility = self._get_uml_visibility(child.visibility)
                    params = self._format_params(child.parameters)
                    return_type = f" : {child.return_type}" if child.return_type else ""
                    lines.append(f"  {visibility}{child.name}({params}){return_type}")
            
            lines.append("}")
        
        # 生成关系
        added_relations = set()
        for rel in self.graph.relationships:
            source = self.graph.entities.get(rel.source_id)
            target = self.graph.entities.get(rel.target_id)
            
            if not source or not target:
                continue
            if source not in class_entities or target not in class_entities:
                continue
            
            rel_key = (source.name, target.name, rel.relationship_type)
            if rel_key in added_relations:
                continue
            added_relations.add(rel_key)
            
            # 转换关系类型
            if rel.relationship_type in [RelationshipType.INHERITS, RelationshipType.EXTENDS]:
                lines.append(f"{target.name} <|-- {source.name}")
            elif rel.relationship_type == RelationshipType.IMPLEMENTS:
                lines.append(f"{target.name} <|.. {source.name}")
            elif rel.relationship_type in [RelationshipType.HAS, RelationshipType.CONTAINS]:
                lines.append(f"{source.name} *-- {target.name}")
            elif rel.relationship_type == RelationshipType.AGGREGATES:
                lines.append(f"{source.name} o-- {target.name}")
            elif rel.relationship_type in [RelationshipType.USES, RelationshipType.DEPENDS_ON]:
                lines.append(f"{source.name} ..> {target.name} : uses")
        
        lines.append("@enduml")
        return "\n".join(lines)
    
    def _generate_class_diagram_mermaid(self, entities: Optional[List[str]], max_entities: int) -> str:
        """生成Mermaid类图"""
        lines = ["classDiagram"]
        
        # 选择类
        if entities:
            class_entities = [self.graph.entities.get(eid) for eid in entities if eid in self.graph.entities]
        else:
            class_entities = [
                e for e in self.graph.entities.values()
                if e.entity_type in [EntityType.CLASS, EntityType.INTERFACE]
            ]
            class_entities.sort(key=lambda e: len(self._get_callers(e.id)), reverse=True)
            class_entities = class_entities[:max_entities]
        
        # 生成类定义
        for entity in class_entities:
            if not entity:
                continue
            
            if entity.entity_type == EntityType.INTERFACE:
                lines.append(f"  class {entity.name}{{<<interface>>")
            else:
                lines.append(f"  class {entity.name}{{")
            
            # 添加属性
            for attr in entity.attributes:
                attr_type = attr.get('type', '')
                attr_name = attr.get('name', '')
                lines.append(f"    +{attr_type} {attr_name}")
            
            # 添加方法
            for child_id in entity.children_ids:
                child = self.graph.entities.get(child_id)
                if child and child.entity_type in [EntityType.METHOD, EntityType.FUNCTION]:
                    params = self._format_params(child.parameters, mermaid=True)
                    return_type = child.return_type or "void"
                    lines.append(f"    +{child.name}({params}) {return_type}")
            
            lines.append("  }")
        
        # 生成关系
        added_relations = set()
        for rel in self.graph.relationships:
            source = self.graph.entities.get(rel.source_id)
            target = self.graph.entities.get(rel.target_id)
            
            if not source or not target:
                continue
            if source not in class_entities or target not in class_entities:
                continue
            
            rel_key = (source.name, target.name)
            if rel_key in added_relations:
                continue
            added_relations.add(rel_key)
            
            if rel.relationship_type in [RelationshipType.INHERITS, RelationshipType.EXTENDS]:
                lines.append(f"  {target.name} <|-- {source.name}")
            elif rel.relationship_type == RelationshipType.IMPLEMENTS:
                lines.append(f"  {target.name} <|.. {source.name}")
            elif rel.relationship_type in [RelationshipType.HAS, RelationshipType.CONTAINS]:
                lines.append(f"  {source.name} *-- {target.name}")
        
        return "\n".join(lines)
    
    def generate_sequence_diagram(self, call_chain: CallChain, format: str = "plantuml") -> str:
        """生成时序图"""
        if format == "mermaid":
            return self._generate_sequence_mermaid(call_chain)
        return self._generate_sequence_plantuml(call_chain)
    
    def _generate_sequence_plantuml(self, call_chain: CallChain) -> str:
        """生成PlantUML时序图"""
        lines = ["@startuml"]
        
        # 收集参与者
        participants = set()
        for node_id in call_chain.nodes:
            entity = self.graph.entities.get(node_id)
            if entity:
                participants.add(entity.name)
        
        # 添加参与者声明
        for name in sorted(participants):
            lines.append(f"participant {name}")
        
        # 生成消息
        for source_id, target_id in call_chain.edges:
            source = self.graph.entities.get(source_id)
            target = self.graph.entities.get(target_id)
            if source and target:
                lines.append(f"{source.name} -> {target.name} : call")
        
        lines.append("@enduml")
        return "\n".join(lines)
    
    def _generate_sequence_mermaid(self, call_chain: CallChain) -> str:
        """生成Mermaid时序图"""
        lines = ["sequenceDiagram"]
        
        # 收集参与者
        participants = set()
        for node_id in call_chain.nodes:
            entity = self.graph.entities.get(node_id)
            if entity:
                participants.add(entity.name)
        
        # 添加参与者声明
        for name in sorted(participants):
            lines.append(f"  participant {name}")
        
        # 生成消息
        for source_id, target_id in call_chain.edges:
            source = self.graph.entities.get(source_id)
            target = self.graph.entities.get(target_id)
            if source and target:
                lines.append(f"  {source.name}->>{target.name}: call")
        
        return "\n".join(lines)
    
    def generate_flowchart(self, flow_type: str = "call", format: str = "mermaid",
                           max_nodes: int = 30) -> str:
        """
        生成流程图
        Args:
            flow_type: 流程类型 ("call", "exception", "data")
            format: 输出格式
            max_nodes: 最大节点数
        """
        if format == "plantuml":
            return self._generate_flowchart_plantuml(flow_type, max_nodes)
        return self._generate_flowchart_mermaid(flow_type, max_nodes)
    
    def _generate_flowchart_mermaid(self, flow_type: str, max_nodes: int) -> str:
        """生成Mermaid流程图"""
        lines = ["graph TD"]
        
        if flow_type == "call":
            # 调用流程
            call_counts = defaultdict(int)
            for rel in self.graph.relationships:
                if rel.relationship_type == RelationshipType.CALLS:
                    call_counts[rel.source_id] += 1
                    call_counts[rel.target_id] += 1
            
            # 选择最重要的节点
            top_nodes = sorted(call_counts.items(), key=lambda x: x[1], reverse=True)[:max_nodes]
            top_node_ids = {nid for nid, _ in top_nodes}
            
            # 生成节点
            for node_id in top_node_ids:
                entity = self.graph.entities.get(node_id)
                if entity:
                    label = entity.name.replace('"', "'")
                    lines.append(f'  {self._safe_id(node_id)}["{label}"]')
            
            # 生成边
            for rel in self.graph.relationships:
                if rel.relationship_type == RelationshipType.CALLS:
                    if rel.source_id in top_node_ids and rel.target_id in top_node_ids:
                        lines.append(f'  {self._safe_id(rel.source_id)} --> {self._safe_id(rel.target_id)}')
        
        elif flow_type == "exception" and self.architecture:
            # 异常流程
            for exc_name, flow in self.architecture.exception_flows.items():
                lines.append(f'  subgraph {self._safe_id(exc_name)}["{exc_name}"]')
                
                for thrower_id in flow.thrown_by[:5]:
                    entity = self.graph.entities.get(thrower_id)
                    if entity:
                        lines.append(f'    {self._safe_id(thrower_id)}["{entity.name} throws"]')
                
                for catcher_id in flow.caught_by[:5]:
                    entity = self.graph.entities.get(catcher_id)
                    if entity:
                        lines.append(f'    {self._safe_id(catcher_id)}["{entity.name} catches"]')
                
                lines.append("  end")
        
        return "\n".join(lines)
    
    def _generate_flowchart_plantuml(self, flow_type: str, max_nodes: int) -> str:
        """生成PlantUML流程图"""
        lines = ["@startuml"]
        
        if flow_type == "call":
            call_counts = defaultdict(int)
            for rel in self.graph.relationships:
                if rel.relationship_type == RelationshipType.CALLS:
                    call_counts[rel.source_id] += 1
            
            top_nodes = sorted(call_counts.items(), key=lambda x: x[1], reverse=True)[:max_nodes]
            top_node_ids = {nid for nid, _ in top_nodes}
            
            for node_id in top_node_ids:
                entity = self.graph.entities.get(node_id)
                if entity:
                    lines.append(f'({entity.name})')
            
            for rel in self.graph.relationships:
                if rel.relationship_type == RelationshipType.CALLS:
                    if rel.source_id in top_node_ids and rel.target_id in top_node_ids:
                        source = self.graph.entities.get(rel.source_id)
                        target = self.graph.entities.get(rel.target_id)
                        if source and target:
                            lines.append(f'({source.name}) --> ({target.name})')
        
        lines.append("@enduml")
        return "\n".join(lines)
    
    def generate_component_diagram(self, format: str = "plantuml") -> str:
        """生成组件图"""
        if not self.architecture:
            return ""
        
        if format == "mermaid":
            return self._generate_component_mermaid()
        return self._generate_component_plantuml()
    
    def _generate_component_plantuml(self) -> str:
        """生成PlantUML组件图"""
        lines = [
            "@startuml",
            "skinparam componentStyle uml2"
        ]
        
        # 添加组件
        for module_path, module_info in self.architecture.modules.items():
            component_name = module_info.name.replace(" ", "_")
            lines.append(f'component "{module_info.name}" as {component_name} {{')
            lines.append(f'  [Classes: {len(module_info.classes)}]')
            lines.append(f'  [Functions: {len(module_info.functions)}]')
            lines.append("}")
        
        # 添加依赖关系
        for module_path, module_info in self.architecture.modules.items():
            component_name = module_info.name.replace(" ", "_")
            for dep in module_info.dependencies:
                dep_module = self.architecture.modules.get(dep)
                if dep_module:
                    dep_name = dep_module.name.replace(" ", "_")
                    lines.append(f"{component_name} --> {dep_name} : depends")
        
        lines.append("@enduml")
        return "\n".join(lines)
    
    def _generate_component_mermaid(self) -> str:
        """生成Mermaid组件图"""
        lines = ["graph TB"]
        
        # 添加组件
        for module_path, module_info in self.architecture.modules.items():
            component_id = self._safe_id(module_path)
            label = f"{module_info.name}\\n({module_info.entity_count} entities)"
            lines.append(f'  subgraph {component_id}["{label}"]')
            lines.append(f'    direction TB')
            for cls in module_info.classes[:5]:
                lines.append(f'    {self._safe_id(cls)}["{cls}"]')
            lines.append("  end")
        
        # 添加依赖
        for module_path, module_info in self.architecture.modules.items():
            component_id = self._safe_id(module_path)
            for dep in module_info.dependencies:
                dep_id = self._safe_id(dep)
                lines.append(f'  {component_id} --> {dep_id}')
        
        return "\n".join(lines)
    
    def generate_package_diagram(self, format: str = "plantuml") -> str:
        """生成包图"""
        if not self.architecture:
            return ""
        
        lines = ["@startuml"] if format == "plantuml" else ["graph TB"]
        
        # 按层级组织
        for layer_name, entity_ids in self.architecture.layers.items():
            if not entity_ids:
                continue
            
            if format == "plantuml":
                lines.append(f'package "{layer_name.title()}" {{')
            else:
                lines.append(f'  subgraph {layer_name}["{layer_name.title()}"]')
            
            for entity_id in entity_ids[:20]:
                entity = self.graph.entities.get(entity_id)
                if entity:
                    if format == "plantuml":
                        lines.append(f'  class {entity.name}')
                    else:
                        lines.append(f'    {self._safe_id(entity_id)}["{entity.name}"]')
            
            lines.append("}")
        
        if format == "plantuml":
            lines.append("@enduml")
        
        return "\n".join(lines)
    
    def generate_all(self, format: str = "plantuml") -> Dict[str, str]:
        """生成所有UML图"""
        diagrams = {
            "class_diagram": self.generate_class_diagram(format),
            "flowchart": self.generate_flowchart("call", format),
            "component_diagram": self.generate_component_diagram(format),
            "package_diagram": self.generate_package_diagram(format)
        }
        
        # 添加时序图
        if self.architecture and self.architecture.call_chains:
            diagrams["sequence_diagram"] = self.generate_sequence_diagram(
                self.architecture.call_chains[0], format
            )
        
        return diagrams
    
    def _get_callers(self, entity_id: str) -> List[str]:
        """获取调用者列表"""
        callers = []
        for rel in self.graph.relationships:
            if rel.target_id == entity_id and rel.relationship_type == RelationshipType.CALLS:
                callers.append(rel.source_id)
        return callers
    
    def _get_uml_visibility(self, visibility) -> str:
        """获取UML可见性符号"""
        if isinstance(visibility, str):
            visibility_map = {
                "public": "+",
                "private": "-",
                "protected": "#",
                "internal": "~",
                "package": "~"
            }
            return visibility_map.get(visibility.lower(), "+")
        return "+"
    
    def _format_params(self, parameters: Optional[List[Dict]], mermaid: bool = False) -> str:
        """格式化参数列表"""
        if not parameters:
            return ""
        
        parts = []
        for param in parameters[:5]:  # 最多显示5个参数
            name = param.get("name", "")
            type_ = param.get("type") or param.get("annotation")
            if type_ and not mermaid:
                parts.append(f"{name}: {type_}")
            else:
                parts.append(name)
        
        result = ", ".join(parts)
        if len(parameters) > 5:
            result += ", ..."
        
        return result
    
    def _safe_id(self, id_str: str) -> str:
        """生成安全的ID（用于Mermaid）"""
        return id_str.replace("/", "_").replace("-", "_").replace(".", "_").replace(":", "_")[:20]
