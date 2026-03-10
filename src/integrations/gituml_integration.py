"""
GitUML Integration - Git增强的UML图生成器
结合Git历史和知识图谱，生成交互式、版本感知的UML图
"""
import os
import json
import subprocess
from typing import List, Dict, Any, Optional, Set, Tuple
from dataclasses import dataclass, field
from enum import Enum
from collections import defaultdict, Counter
from pathlib import Path
from datetime import datetime

# 导入项目模块
import sys
sys.path.insert(0, str(Path(__file__).parent.parent.parent))
from src.gitnexus.models import (
    KnowledgeGraph, CodeEntity, EntityType, Relationship, RelationshipType,
    ArchitectureInfo, ModuleInfo, FeatureInfo, CallChain
)
from src.core.uml_generator import UMLGenerator


class DiagramType(Enum):
    """图表类型"""
    CLASS = "class"
    SEQUENCE = "sequence"
    COMPONENT = "component"
    PACKAGE = "package"
    DEPLOYMENT = "deployment"
    ACTIVITY = "activity"
    STATE = "state"
    USECASE = "usecase"
    FLOWCHART = "flowchart"
    ER = "er"  # 实体关系图
    MINDMAP = "mindmap"
    TIMELINE = "timeline"
    GANTT = "gantt"
    GITGRAPH = "gitgraph"


class OutputFormat(Enum):
    """输出格式"""
    PLANTUML = "plantuml"
    MERMAID = "mermaid"
    GRAPHVIZ = "graphviz"
    JSON = "json"
    SVG = "svg"


@dataclass
class GitCommitInfo:
    """Git提交信息"""
    hash: str
    author: str
    date: str
    message: str
    files_changed: List[str] = field(default_factory=list)
    additions: int = 0
    deletions: int = 0


@dataclass
class GitUMLDiagram:
    """GitUML图表结果"""
    diagram_type: DiagramType
    format: OutputFormat
    content: str
    
    # 元数据
    title: str
    description: str
    generated_at: str = field(default_factory=lambda: datetime.now().isoformat())
    
    # Git相关信息
    base_commit: Optional[str] = None
    target_commit: Optional[str] = None
    affected_files: List[str] = field(default_factory=list)
    affected_entities: List[str] = field(default_factory=list)
    
    # 统计
    entity_count: int = 0
    relationship_count: int = 0


class GitUMLIntegration:
    """Git增强的UML生成器"""
    
    def __init__(self, graph: KnowledgeGraph, repo_path: str = ".", architecture: Optional[ArchitectureInfo] = None):
        self.graph = graph
        self.architecture = architecture
        self.repo_path = repo_path
        self.base_uml_generator = UMLGenerator(graph, architecture)
        self._init_git_info()
    
    def _init_git_info(self):
        """初始化Git信息"""
        self.git_available = self._check_git_available()
        self.current_branch = self._get_current_branch() if self.git_available else "unknown"
        self.recent_commits = self._get_recent_commits(10) if self.git_available else []
    
    def _check_git_available(self) -> bool:
        """检查Git是否可用"""
        try:
            result = subprocess.run(
                ['git', 'rev-parse', '--git-dir'],
                cwd=self.repo_path,
                capture_output=True,
                text=True,
                timeout=5
            )
            return result.returncode == 0
        except Exception:
            return False
    
    def _get_current_branch(self) -> str:
        """获取当前分支"""
        try:
            result = subprocess.run(
                ['git', 'branch', '--show-current'],
                cwd=self.repo_path,
                capture_output=True,
                text=True,
                timeout=5
            )
            return result.stdout.strip() or "unknown"
        except Exception:
            return "unknown"
    
    def _get_recent_commits(self, count: int = 10) -> List[GitCommitInfo]:
        """获取最近的提交"""
        commits = []
        try:
            result = subprocess.run(
                ['git', 'log', f'-{count}', '--pretty=format:%H|%an|%ad|%s', '--date=short'],
                cwd=self.repo_path,
                capture_output=True,
                text=True,
                timeout=10
            )
            
            for line in result.stdout.strip().split('\n'):
                if line:
                    parts = line.split('|')
                    if len(parts) >= 4:
                        commits.append(GitCommitInfo(
                            hash=parts[0][:7],
                            author=parts[1],
                            date=parts[2],
                            message=parts[3]
                        ))
        except Exception:
            pass
        
        return commits
    
    def _get_file_commit_history(self, file_path: str, count: int = 10) -> List[GitCommitInfo]:
        """获取文件的提交历史"""
        commits = []
        try:
            result = subprocess.run(
                ['git', 'log', f'-{count}', '--pretty=format:%H|%an|%ad|%s', '--date=short', '--', file_path],
                cwd=self.repo_path,
                capture_output=True,
                text=True,
                timeout=10
            )
            
            for line in result.stdout.strip().split('\n'):
                if line:
                    parts = line.split('|')
                    if len(parts) >= 4:
                        commits.append(GitCommitInfo(
                            hash=parts[0][:7],
                            author=parts[1],
                            date=parts[2],
                            message=parts[3]
                        ))
        except Exception:
            pass
        
        return commits
    
    def _get_diff_files(self, commit1: str, commit2: str) -> List[str]:
        """获取两个提交之间变更的文件"""
        try:
            result = subprocess.run(
                ['git', 'diff', '--name-only', commit1, commit2],
                cwd=self.repo_path,
                capture_output=True,
                text=True,
                timeout=30
            )
            return [f for f in result.stdout.strip().split('\n') if f]
        except Exception:
            return []
    
    # ==================== 增强UML生成方法 ====================
    
    def generate_class_diagram_enhanced(
        self,
        entities: Optional[List[str]] = None,
        format: str = "mermaid",
        include_git_info: bool = True,
        highlight_recent_changes: bool = False,
        max_entities: int = 50
    ) -> GitUMLDiagram:
        """生成增强版类图"""
        
        # 选择实体
        if entities:
            class_entities = [self.graph.entities.get(eid) for eid in entities if eid in self.graph.entities]
        else:
            class_entities = [
                e for e in self.graph.entities.values()
                if e.entity_type in [EntityType.CLASS, EntityType.INTERFACE, EntityType.ENUM]
            ]
            # 按重要性排序
            class_entities.sort(key=lambda e: len(self._get_callers(e.id)), reverse=True)
            class_entities = class_entities[:max_entities]
        
        # 收集最近修改的文件
        recently_modified = set()
        if highlight_recent_changes and self.git_available and self.recent_commits:
            for commit in self.recent_commits[:5]:
                try:
                    result = subprocess.run(
                        ['git', 'diff-tree', '--no-commit-id', '--name-only', '-r', commit.hash],
                        cwd=self.repo_path,
                        capture_output=True,
                        text=True,
                        timeout=10
                    )
                    recently_modified.update(result.stdout.strip().split('\n'))
                except Exception:
                    pass
        
        # 生成内容
        if format == "mermaid":
            content = self._generate_class_diagram_mermaid_enhanced(
                class_entities, include_git_info, recently_modified
            )
        else:
            content = self._generate_class_diagram_plantuml_enhanced(
                class_entities, include_git_info, recently_modified
            )
        
        return GitUMLDiagram(
            diagram_type=DiagramType.CLASS,
            format=OutputFormat.MERMAID if format == "mermaid" else OutputFormat.PLANTUML,
            content=content,
            title="类图",
            description="展示系统中的类、接口及其关系",
            affected_entities=[e.id for e in class_entities if e],
            entity_count=len([e for e in class_entities if e])
        )
    
    def _generate_class_diagram_mermaid_enhanced(
        self,
        entities: List[CodeEntity],
        include_git_info: bool,
        recently_modified: Set[str]
    ) -> str:
        """生成增强版Mermaid类图"""
        lines = ["classDiagram"]
        lines.append("  %% GitUML Enhanced Class Diagram")
        lines.append(f"  %% Branch: {self.current_branch}")
        
        if self.recent_commits and include_git_info:
            lines.append(f"  %% Latest Commit: {self.recent_commits[0].hash} - {self.recent_commits[0].message[:30]}")
        
        lines.append("")
        
        # 生成类定义
        for entity in entities:
            if not entity:
                continue
            
            # 检查是否最近修改
            is_modified = entity.file_path in recently_modified
            
            if entity.entity_type == EntityType.INTERFACE:
                lines.append(f'  class {entity.name}{{')
                lines.append("    <<interface>>")
            elif entity.entity_type == EntityType.ENUM:
                lines.append(f'  class {entity.name}{{')
                lines.append("    <<enumeration>>")
            else:
                lines.append(f'  class {entity.name}{{')
            
            # 添加样式标记
            if is_modified:
                lines.append("    %% 最近修改")
            
            # 添加属性
            for attr in entity.attributes[:10]:
                attr_type = attr.get('type', '')
                attr_name = attr.get('name', '')
                visibility = self._get_visibility_symbol(attr.get('visibility', 'public'))
                if attr_type:
                    lines.append(f"    {visibility}{attr_type} {attr_name}")
                else:
                    lines.append(f"    {visibility}{attr_name}")
            
            # 添加方法
            for child_id in entity.children_ids[:15]:
                child = self.graph.entities.get(child_id)
                if child and child.entity_type in [EntityType.METHOD, EntityType.FUNCTION]:
                    visibility = self._get_visibility_symbol(child.visibility)
                    params = self._format_params(child.parameters)
                    return_type = child.return_type or "void"
                    lines.append(f"    {visibility}{child.name}({params}) {return_type}")
            
            # Git信息注释
            if include_git_info and self.git_available:
                git_history = self._get_file_commit_history(entity.file_path, 3)
                if git_history:
                    lines.append(f"    %% Git History:")
                    for gh in git_history[:2]:
                        lines.append(f"    %%   {gh.hash} {gh.date}")
            
            lines.append("  }")
        
        # 生成关系
        lines.append("")
        lines.append("  %% Relationships")
        
        entity_ids = {e.id for e in entities if e}
        added_relations = set()
        
        for rel in self.graph.relationships:
            source = self.graph.entities.get(rel.source_id)
            target = self.graph.entities.get(rel.target_id)
            
            if not source or not target:
                continue
            if source.id not in entity_ids or target.id not in entity_ids:
                continue
            
            rel_key = (source.name, target.name)
            if rel_key in added_relations:
                continue
            added_relations.add(rel_key)
            
            if rel.relationship_type in [RelationshipType.INHERITS, RelationshipType.EXTENDS]:
                lines.append(f"  {target.name} <|-- {source.name} : extends")
            elif rel.relationship_type == RelationshipType.IMPLEMENTS:
                lines.append(f"  {target.name} <|.. {source.name} : implements")
            elif rel.relationship_type in [RelationshipType.HAS, RelationshipType.CONTAINS]:
                lines.append(f"  {source.name} *-- {target.name} : contains")
            elif rel.relationship_type == RelationshipType.AGGREGATES:
                lines.append(f"  {source.name} o-- {target.name} : aggregates")
            elif rel.relationship_type in [RelationshipType.USES, RelationshipType.DEPENDS_ON]:
                lines.append(f"  {source.name} ..> {target.name} : uses")
        
        return "\n".join(lines)
    
    def _generate_class_diagram_plantuml_enhanced(
        self,
        entities: List[CodeEntity],
        include_git_info: bool,
        recently_modified: Set[str]
    ) -> str:
        """生成增强版PlantUML类图"""
        lines = [
            "@startuml",
            "skinparam classAttributeIconSize 0",
            "skinparam classBorderColor Black",
            "skinparam classBackgroundColor White",
            ""
        ]
        
        lines.append("' GitUML Enhanced Class Diagram")
        lines.append(f"' Branch: {self.current_branch}")
        
        if self.recent_commits and include_git_info:
            lines.append(f"' Latest Commit: {self.recent_commits[0].hash}")
        
        lines.append("")
        
        # 定义样式
        lines.append("' Style for recently modified")
        lines.append("skinparam class<<Modified>> BorderColor Red")
        lines.append("skinparam class<<Modified>> BackgroundColor LightYellow")
        lines.append("")
        
        # 生成类定义
        for entity in entities:
            if not entity:
                continue
            
            is_modified = entity.file_path in recently_modified
            stereotype = "<<Modified>> " if is_modified else ""
            
            if entity.entity_type == EntityType.INTERFACE:
                lines.append(f"interface {entity.name} {stereotype}{{")
            elif entity.entity_type == EntityType.ENUM:
                lines.append(f"enum {entity.name} {stereotype}{{")
            else:
                lines.append(f"class {entity.name} {stereotype}{{")
            
            # 属性
            for attr in entity.attributes[:10]:
                visibility = self._get_visibility_symbol(attr.get('visibility', 'public'))
                attr_type = attr.get('type', '')
                attr_name = attr.get('name', '')
                if attr_type:
                    lines.append(f"  {visibility}{attr_name} : {attr_type}")
                else:
                    lines.append(f"  {visibility}{attr_name}")
            
            # 方法
            for child_id in entity.children_ids[:15]:
                child = self.graph.entities.get(child_id)
                if child and child.entity_type in [EntityType.METHOD, EntityType.FUNCTION]:
                    visibility = self._get_visibility_symbol(child.visibility)
                    params = self._format_params(child.parameters)
                    return_type = f" : {child.return_type}" if child.return_type else ""
                    lines.append(f"  {visibility}{child.name}({params}){return_type}")
            
            lines.append("}")
            
            # Git note
            if include_git_info and self.git_available:
                git_history = self._get_file_commit_history(entity.file_path, 3)
                if git_history:
                    note_lines = [f"{gh.hash} {gh.date}" for gh in git_history[:2]]
                    lines.append(f"note right of {entity.name}")
                    lines.append(f"  {entity.file_path}")
                    for nl in note_lines:
                        lines.append(f"  {nl}")
                    lines.append("end note")
        
        # 关系
        lines.append("")
        lines.append("' Relationships")
        
        entity_ids = {e.id for e in entities if e}
        added_relations = set()
        
        for rel in self.graph.relationships:
            source = self.graph.entities.get(rel.source_id)
            target = self.graph.entities.get(rel.target_id)
            
            if not source or not target:
                continue
            if source.id not in entity_ids or target.id not in entity_ids:
                continue
            
            rel_key = (source.name, target.name)
            if rel_key in added_relations:
                continue
            added_relations.add(rel_key)
            
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
    
    def generate_sequence_diagram_enhanced(
        self,
        call_chain: CallChain,
        format: str = "mermaid",
        include_timing: bool = True
    ) -> GitUMLDiagram:
        """生成增强版时序图"""
        
        if format == "mermaid":
            content = self._generate_sequence_mermaid_enhanced(call_chain, include_timing)
        else:
            content = self._generate_sequence_plantuml_enhanced(call_chain, include_timing)
        
        return GitUMLDiagram(
            diagram_type=DiagramType.SEQUENCE,
            format=OutputFormat.MERMAID if format == "mermaid" else OutputFormat.PLANTUML,
            content=content,
            title=f"时序图 - {call_chain.name}",
            description="展示方法调用顺序",
            affected_entities=call_chain.nodes,
            entity_count=len(call_chain.nodes)
        )
    
    def _generate_sequence_mermaid_enhanced(self, call_chain: CallChain, include_timing: bool) -> str:
        """生成增强版Mermaid时序图"""
        lines = ["sequenceDiagram"]
        lines.append("  autonumber")
        lines.append("")
        
        # 参与者
        participants = set()
        for node_id in call_chain.nodes:
            entity = self.graph.entities.get(node_id)
            if entity:
                participants.add(entity.name)
        
        for name in sorted(participants):
            entity = next((e for e in self.graph.entities.values() if e.name == name), None)
            if entity:
                if entity.entity_type == EntityType.CLASS:
                    lines.append(f"  participant {name} as {name}")
                else:
                    lines.append(f"  participant {name}")
        
        lines.append("")
        lines.append("  %% Sequence Flow")
        
        # 消息
        for idx, (source_id, target_id) in enumerate(call_chain.edges):
            source = self.graph.entities.get(source_id)
            target = self.graph.entities.get(target_id)
            if source and target:
                # 获取调用信息
                call_info = self._get_call_info(source_id, target_id)
                
                if include_timing:
                    lines.append(f"  {source.name}->>+{target.name}: {call_info}")
                else:
                    lines.append(f"  {source.name}->>{target.name}: {call_info}")
                
                # 添加返回
                if include_timing:
                    return_type = target.return_type if target.return_type else "void"
                    lines.append(f"  {target.name}-->>-{source.name}: {return_type}")
        
        return "\n".join(lines)
    
    def _generate_sequence_plantuml_enhanced(self, call_chain: CallChain, include_timing: bool) -> str:
        """生成增强版PlantUML时序图"""
        lines = ["@startuml"]
        lines.append("autonumber")
        lines.append("skinparam sequenceMessageAlign center")
        lines.append("")
        
        # 参与者
        participants = set()
        for node_id in call_chain.nodes:
            entity = self.graph.entities.get(node_id)
            if entity:
                participants.add(entity.name)
        
        for name in sorted(participants):
            lines.append(f"participant {name}")
        
        lines.append("")
        
        # 消息
        for source_id, target_id in call_chain.edges:
            source = self.graph.entities.get(source_id)
            target = self.graph.entities.get(target_id)
            if source and target:
                call_info = self._get_call_info(source_id, target_id)
                
                lines.append(f"{source.name} -> {target.name} : {call_info}")
                
                if include_timing:
                    return_type = target.return_type if target.return_type else "void"
                    lines.append(f"{target.name} --> {source.name} : {return_type}")
        
        lines.append("@enduml")
        return "\n".join(lines)
    
    def generate_diff_diagram(
        self,
        commit1: str,
        commit2: str,
        format: str = "mermaid"
    ) -> GitUMLDiagram:
        """生成差异图，展示两次提交之间的变更"""
        
        if not self.git_available:
            return GitUMLDiagram(
                diagram_type=DiagramType.FLOWCHART,
                format=OutputFormat.MERMAID,
                content="Git不可用",
                title="差异图",
                description="Git不可用，无法生成差异图"
            )
        
        # 获取变更文件
        changed_files = self._get_diff_files(commit1, commit2)
        
        # 找出受影响的实体
        affected_entities = []
        for entity in self.graph.entities.values():
            if entity.file_path in changed_files:
                affected_entities.append(entity)
        
        # 生成内容
        if format == "mermaid":
            content = self._generate_diff_mermaid(commit1, commit2, changed_files, affected_entities)
        else:
            content = self._generate_diff_plantuml(commit1, commit2, changed_files, affected_entities)
        
        return GitUMLDiagram(
            diagram_type=DiagramType.FLOWCHART,
            format=OutputFormat.MERMAID if format == "mermaid" else OutputFormat.PLANTUML,
            content=content,
            title=f"差异图: {commit1}..{commit2}",
            description="展示两次提交之间的变更",
            base_commit=commit1,
            target_commit=commit2,
            affected_files=changed_files,
            affected_entities=[e.id for e in affected_entities],
            entity_count=len(affected_entities)
        )
    
    def _generate_diff_mermaid(
        self,
        commit1: str,
        commit2: str,
        changed_files: List[str],
        affected_entities: List[CodeEntity]
    ) -> str:
        """生成Mermaid差异图"""
        lines = ["graph TD"]
        lines.append("  subgraph Commits[\"提交对比\"]")
        lines.append(f"    C1[\"{commit1}<br/>Base\"]")
        lines.append(f"    C2[\"{commit2}<br/>Target\"]")
        lines.append("  end")
        lines.append("")
        lines.append("  subgraph Files[\"变更文件\"]")
        
        for idx, file_path in enumerate(changed_files[:20]):
            safe_id = file_path.replace("/", "_").replace(".", "_").replace("-", "_")[:30]
            lines.append(f'    F{idx}["{Path(file_path).name}"]')
        
        lines.append("  end")
        lines.append("")
        lines.append("  subgraph Entities[\"受影响实体\"]")
        
        for idx, entity in enumerate(affected_entities[:20]):
            lines.append(f'    E{idx}["{entity.name}<br/>{entity.entity_type.value}"]')
        
        lines.append("  end")
        lines.append("")
        lines.append("  C1 --> C2")
        
        for idx in range(min(len(changed_files), 20)):
            lines.append(f"  C2 --> F{idx}")
        
        for idx, entity in enumerate(affected_entities[:20]):
            file_idx = changed_files.index(entity.file_path) if entity.file_path in changed_files else -1
            if file_idx >= 0:
                lines.append(f"  F{file_idx} --> E{idx}")
        
        return "\n".join(lines)
    
    def _generate_diff_plantuml(
        self,
        commit1: str,
        commit2: str,
        changed_files: List[str],
        affected_entities: List[CodeEntity]
    ) -> str:
        """生成PlantUML差异图"""
        lines = [
            "@startuml",
            "skinparam componentStyle uml2",
            ""
        ]
        
        lines.append(f'package "Commits" {{')
        lines.append(f'  card "{commit1}\\nBase" as C1')
        lines.append(f'  card "{commit2}\\nTarget" as C2')
        lines.append('}')
        lines.append("")
        
        lines.append(f'package "Changed Files ({len(changed_files)})" {{')
        for idx, file_path in enumerate(changed_files[:15]):
            lines.append(f'  rectangle "{Path(file_path).name}" as F{idx}')
        lines.append('}')
        lines.append("")
        
        lines.append(f'package "Affected Entities ({len(affected_entities)})" {{')
        for idx, entity in enumerate(affected_entities[:15]):
            lines.append(f'  class "{entity.name}" as E{idx}')
        lines.append('}')
        lines.append("")
        
        lines.append('C1 --> C2 : changes')
        
        for idx in range(min(len(changed_files), 15)):
            lines.append(f'C2 --> F{idx}')
        
        for idx, entity in enumerate(affected_entities[:15]):
            file_idx = changed_files.index(entity.file_path) if entity.file_path in changed_files else -1
            if file_idx >= 0:
                lines.append(f'F{file_idx} --> E{idx} : contains')
        
        lines.append("@enduml")
        return "\n".join(lines)
    
    def generate_module_dependency_diagram(
        self,
        format: str = "mermaid",
        highlight_cycles: bool = True
    ) -> GitUMLDiagram:
        """生成模块依赖图"""
        
        if not self.architecture:
            return GitUMLDiagram(
                diagram_type=DiagramType.COMPONENT,
                format=OutputFormat.MERMAID,
                content="无架构信息",
                title="模块依赖图",
                description="缺少架构分析信息"
            )
        
        # 检测循环依赖
        cycles = self._find_cycles({
            path: info.dependencies 
            for path, info in self.architecture.modules.items()
        }) if highlight_cycles else []
        
        if format == "mermaid":
            content = self._generate_module_dep_mermaid(cycles)
        else:
            content = self._generate_module_dep_plantuml(cycles)
        
        return GitUMLDiagram(
            diagram_type=DiagramType.COMPONENT,
            format=OutputFormat.MERMAID if format == "mermaid" else OutputFormat.PLANTUML,
            content=content,
            title="模块依赖图",
            description="展示模块之间的依赖关系",
            entity_count=len(self.architecture.modules)
        )
    
    def _generate_module_dep_mermaid(self, cycles: List[List[str]]) -> str:
        """生成Mermaid模块依赖图"""
        lines = ["graph TD"]
        
        # 模块节点
        for module_path, module_info in self.architecture.modules.items():
            node_id = self._safe_id(module_path)
            label = f"{module_info.name}<br/>({module_info.entity_count} entities)"
            lines.append(f'  {node_id}["{label}"]')
        
        lines.append("")
        
        # 依赖边
        added_deps = set()
        for module_path, module_info in self.architecture.modules.items():
            source_id = self._safe_id(module_path)
            for dep in module_info.dependencies:
                target_id = self._safe_id(dep)
                dep_key = (source_id, target_id)
                if dep_key not in added_deps:
                    added_deps.add(dep_key)
                    
                    # 检查是否是循环依赖
                    is_cycle = any(
                        source_id in [self._safe_id(m) for m in cycle] and
                        target_id in [self._safe_id(m) for m in cycle]
                        for cycle in cycles
                    )
                    
                    if is_cycle:
                        lines.append(f'  {source_id} -.->|循环| {target_id}')
                    else:
                        lines.append(f'  {source_id} --> {target_id}')
        
        # 循环依赖警告
        if cycles:
            lines.append("")
            lines.append("  %% 警告：检测到循环依赖")
            for cycle in cycles:
                lines.append(f"  %% {' -> '.join(cycle)}")
        
        return "\n".join(lines)
    
    def _generate_module_dep_plantuml(self, cycles: List[List[str]]) -> str:
        """生成PlantUML模块依赖图"""
        lines = [
            "@startuml",
            "skinparam componentStyle uml2",
            ""
        ]
        
        # 模块
        for module_path, module_info in self.architecture.modules.items():
            component_name = module_info.name.replace(" ", "_")
            lines.append(f'component "{module_info.name}" as {component_name} {{')
            lines.append(f'  [Classes: {len(module_info.classes)}]')
            lines.append(f'  [Functions: {len(module_info.functions)}]')
            lines.append("}")
        
        lines.append("")
        
        # 依赖
        added_deps = set()
        for module_path, module_info in self.architecture.modules.items():
            source_name = module_info.name.replace(" ", "_")
            for dep in module_info.dependencies:
                dep_module = self.architecture.modules.get(dep)
                if dep_module:
                    target_name = dep_module.name.replace(" ", "_")
                    dep_key = (source_name, target_name)
                    if dep_key not in added_deps:
                        added_deps.add(dep_key)
                        lines.append(f"{source_name} --> {target_name}")
        
        lines.append("@enduml")
        return "\n".join(lines)
    
    def generate_feature_diagram(
        self,
        feature_id: str,
        format: str = "mermaid"
    ) -> GitUMLDiagram:
        """生成功能模块图"""
        
        if not self.architecture or feature_id not in self.architecture.features:
            return GitUMLDiagram(
                diagram_type=DiagramType.MINDMAP,
                format=OutputFormat.MERMAID,
                content="功能模块不存在",
                title="功能模块图",
                description=f"未找到功能模块: {feature_id}"
            )
        
        feature = self.architecture.features[feature_id]
        
        if format == "mermaid":
            content = self._generate_feature_mermaid(feature)
        else:
            content = self._generate_feature_plantuml(feature)
        
        return GitUMLDiagram(
            diagram_type=DiagramType.MINDMAP,
            format=OutputFormat.MERMAID if format == "mermaid" else OutputFormat.PLANTUML,
            content=content,
            title=f"功能模块: {feature.name}",
            description=feature.description or f"{feature.name}功能模块结构",
            affected_entities=feature.entities,
            entity_count=len(feature.entities)
        )
    
    def _generate_feature_mermaid(self, feature: FeatureInfo) -> str:
        """生成Mermaid功能模块图"""
        lines = ["mindmap"]
        lines.append(f"  root(({feature.name}))")
        
        # 入口点
        if feature.entry_points:
            lines.append("    EntryPoints")
            for ep_id in feature.entry_points[:5]:
                entity = self.graph.entities.get(ep_id)
                if entity:
                    lines.append(f"      {entity.name}")
        
        # 相关实体
        lines.append("    Entities")
        for entity_id in feature.entities[:15]:
            entity = self.graph.entities.get(entity_id)
            if entity:
                lines.append(f"      {entity.name}({entity.entity_type.value})")
        
        # 依赖
        if feature.depends_on:
            lines.append("    DependsOn")
            for dep in feature.depends_on[:5]:
                lines.append(f"      {dep}")
        
        return "\n".join(lines)
    
    def _generate_feature_plantuml(self, feature: FeatureInfo) -> str:
        """生成PlantUML功能模块图"""
        lines = [
            "@startuml",
            "!define RECTANGLE class",
            ""
        ]
        
        lines.append(f'package "{feature.name}" {{')
        
        if feature.entry_points:
            lines.append("  package \"Entry Points\" {")
            for ep_id in feature.entry_points[:5]:
                entity = self.graph.entities.get(ep_id)
                if entity:
                    lines.append(f'    class {entity.name}')
            lines.append("  }")
        
        lines.append("  package \"Entities\" {")
        for entity_id in feature.entities[:15]:
            entity = self.graph.entities.get(entity_id)
            if entity:
                lines.append(f'    class {entity.name}')
        lines.append("  }")
        
        lines.append("}")
        lines.append("@enduml")
        return "\n".join(lines)
    
    def generate_git_history_timeline(
        self,
        file_path: Optional[str] = None,
        count: int = 20,
        format: str = "mermaid"
    ) -> GitUMLDiagram:
        """生成Git历史时间线"""
        
        if not self.git_available:
            return GitUMLDiagram(
                diagram_type=DiagramType.TIMELINE,
                format=OutputFormat.MERMAID,
                content="Git不可用",
                title="Git历史时间线",
                description="Git不可用"
            )
        
        if file_path:
            commits = self._get_file_commit_history(file_path, count)
        else:
            commits = self.recent_commits[:count]
        
        if format == "mermaid":
            content = self._generate_timeline_mermaid(commits, file_path)
        else:
            content = self._generate_timeline_plantuml(commits, file_path)
        
        return GitUMLDiagram(
            diagram_type=DiagramType.TIMELINE,
            format=OutputFormat.MERMAID if format == "mermaid" else OutputFormat.PLANTUML,
            content=content,
            title=f"Git历史时间线{' - ' + file_path if file_path else ''}",
            description="展示提交历史"
        )
    
    def _generate_timeline_mermaid(self, commits: List[GitCommitInfo], file_path: Optional[str]) -> str:
        """生成Mermaid时间线"""
        lines = ["timeline"]
        lines.append(f"  title Git History{' - ' + Path(file_path).name if file_path else ''}")
        lines.append("")
        
        for commit in commits:
            date = commit.date
            lines.append(f"  section {date}")
            lines.append(f"    {commit.hash} : {commit.message[:50]}")
            lines.append(f"    : by {commit.author}")
        
        return "\n".join(lines)
    
    def _generate_timeline_plantuml(self, commits: List[GitCommitInfo], file_path: Optional[str]) -> str:
        """生成PlantUML时间线"""
        lines = [
            "@startuml",
            f"title Git History{' - ' + file_path if file_path else ''}",
            ""
        ]
        
        lines.append("robust \"Commit History\" as CH")
        lines.append("")
        
        for idx, commit in enumerate(commits):
            lines.append(f"@{commit.date}")
            lines.append(f"CH is {commit.hash}")
        
        lines.append("")
        lines.append("@end")
        lines.append("@enduml")
        return "\n".join(lines)
    
    def generate_all_diagrams(
        self,
        format: str = "mermaid",
        include_diff: bool = False,
        commit1: Optional[str] = None,
        commit2: Optional[str] = None
    ) -> Dict[str, GitUMLDiagram]:
        """生成所有图表"""
        diagrams = {}
        
        # 类图
        diagrams["class_diagram"] = self.generate_class_diagram_enhanced(
            format=format,
            include_git_info=True,
            highlight_recent_changes=True
        )
        
        # 模块依赖图
        diagrams["module_dependency"] = self.generate_module_dependency_diagram(format=format)
        
        # 时序图
        if self.architecture and self.architecture.call_chains:
            diagrams["sequence_diagram"] = self.generate_sequence_diagram_enhanced(
                self.architecture.call_chains[0],
                format=format
            )
        
        # Git历史
        diagrams["git_timeline"] = self.generate_git_history_timeline(format=format)
        
        # 功能模块图
        if self.architecture and self.architecture.features:
            first_feature = list(self.architecture.features.keys())[0]
            diagrams["feature_diagram"] = self.generate_feature_diagram(first_feature, format=format)
        
        # 差异图
        if include_diff and commit1 and commit2:
            diagrams["diff_diagram"] = self.generate_diff_diagram(commit1, commit2, format=format)
        
        return diagrams
    
    # ==================== 辅助方法 ====================
    
    def _get_callers(self, entity_id: str) -> List[str]:
        """获取调用者列表"""
        callers = []
        for rel in self.graph.relationships:
            if rel.target_id == entity_id and rel.relationship_type == RelationshipType.CALLS:
                callers.append(rel.source_id)
        return callers
    
    def _get_visibility_symbol(self, visibility) -> str:
        """获取可见性符号"""
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
    
    def _format_params(self, parameters: Optional[List[Dict]]) -> str:
        """格式化参数"""
        if not parameters:
            return ""
        
        parts = []
        for param in parameters[:5]:
            name = param.get("name", "")
            parts.append(name)
        
        result = ", ".join(parts)
        if len(parameters) > 5:
            result += ", ..."
        
        return result
    
    def _get_call_info(self, source_id: str, target_id: str) -> str:
        """获取调用信息"""
        source = self.graph.entities.get(source_id)
        target = self.graph.entities.get(target_id)
        
        if source and target:
            # 查找调用关系
            for rel in self.graph.relationships:
                if rel.source_id == source_id and rel.target_id == target_id and rel.relationship_type == RelationshipType.CALLS:
                    if rel.call_site:
                        return f"{target.name}() @ {rel.call_site}"
            return f"{target.name}()"
        
        return "call"
    
    def _safe_id(self, id_str: str) -> str:
        """生成安全的ID"""
        return id_str.replace("/", "_").replace("-", "_").replace(".", "_").replace(":", "_")[:30]
    
    def _find_cycles(self, graph: Dict[str, List[str]]) -> List[List[str]]:
        """查找循环依赖"""
        cycles = []
        visited = set()
        rec_stack = set()
        path = []
        
        def dfs(node):
            visited.add(node)
            rec_stack.add(node)
            path.append(node)
            
            for neighbor in graph.get(node, []):
                if neighbor not in visited:
                    dfs(neighbor)
                elif neighbor in rec_stack:
                    cycle_start = path.index(neighbor)
                    cycle = path[cycle_start:]
                    if len(cycle) > 1:
                        cycles.append(cycle)
            
            path.pop()
            rec_stack.remove(node)
        
        for node in graph:
            if node not in visited:
                dfs(node)
        
        return cycles
