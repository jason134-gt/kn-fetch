"""
架构报告导出器 - 生成完整的重构支持报告
输出格式：Markdown、HTML
"""
from typing import Dict, Any, Optional, List
from pathlib import Path
from datetime import datetime
from collections import Counter

from ..gitnexus.models import (
    KnowledgeGraph, EntityType, RelationshipType,
    ArchitectureInfo, ModuleInfo, FeatureInfo, CallChain, ExceptionFlow
)
from .architecture_analyzer import ArchitectureAnalyzer
from .uml_generator import UMLGenerator


class ArchitectureReporter:
    """架构报告导出器"""
    
    def __init__(self, graph: KnowledgeGraph):
        self.graph = graph
        self.analyzer = ArchitectureAnalyzer(graph)
        self.architecture = self.analyzer.analyze_full()
        self.uml_generator = UMLGenerator(graph, self.architecture)
    
    def generate_full_report(self) -> str:
        """生成完整架构报告（Markdown格式）"""
        sections = [
            self._generate_header(),
            self._generate_toc(),
            self._generate_executive_summary(),
            self._generate_functional_overview(),
            self._generate_module_structure(),
            self._generate_interface_analysis(),
            self._generate_call_flow_analysis(),
            self._generate_class_relationships(),
            self._generate_message_flow(),
            self._generate_exception_handling(),
            self._generate_complexity_analysis(),
            self._generate_refactoring_recommendations(),
            self._generate_appendix()
        ]
        
        return "\n\n".join(sections)
    
    def _generate_header(self) -> str:
        """生成报告头部"""
        return f"""# 软件架构分析报告

**项目**: {self.graph.project_name or '未命名项目'}  
**版本**: {self.graph.project_version or 'N/A'}  
**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  
**分析工具**: KN-Fetch 知识提取智能体

---

"""
    
    def _generate_toc(self) -> str:
        """生成目录"""
        return """## 目录

1. [执行摘要](#执行摘要)
2. [功能概览](#功能概览)
3. [模块结构](#模块结构)
4. [接口分析](#接口分析)
5. [调用流程分析](#调用流程分析)
6. [类与关系](#类与关系)
7. [消息机制](#消息机制)
8. [异常处理](#异常处理)
9. [复杂度分析](#复杂度分析)
10. [重构建议](#重构建议)
11. [附录](#附录)

"""
    
    def _generate_executive_summary(self) -> str:
        """生成执行摘要"""
        stats = self.architecture.statistics
        
        return f"""## 执行摘要

### 项目概览

| 指标 | 数值 |
|------|------|
| 总实体数 | {stats['total_entities']} |
| 总关系数 | {stats['total_relationships']} |
| 代码行数 | {stats['total_lines_of_code']:,} |
| 模块数量 | {len(self.architecture.modules)} |
| 功能模块 | {len(self.architecture.features)} |
| 平均复杂度 | {stats['average_complexity']:.1f} |
| 最大复杂度 | {stats['max_complexity']} |

### 实体类型分布

| 类型 | 数量 |
|------|------|
{self._format_entity_types_table(stats['entity_types'])}

### 关键发现

{self._generate_key_findings()}

"""
    
    def _generate_key_findings(self) -> str:
        """生成关键发现"""
        findings = []
        stats = self.architecture.statistics
        
        # 复杂度分析
        if stats['average_complexity'] > 10:
            findings.append("⚠️ **高复杂度警告**: 平均圈复杂度较高，建议优先重构复杂模块")
        
        # 模块依赖分析
        cyclic_deps = self._detect_cyclic_dependencies()
        if cyclic_deps:
            findings.append(f"🔄 **循环依赖**: 检测到 {len(cyclic_deps)} 组循环依赖")
        
        # 最复杂实体
        if stats['most_complex_entities']:
            top = stats['most_complex_entities'][0]
            findings.append(f"📊 **最复杂实体**: `{top['name']}` (复杂度: {top['complexity']})")
        
        # 最被调用实体
        if stats['most_called_entities']:
            top = stats['most_called_entities'][0]
            findings.append(f"🔥 **热点实体**: `{top['name']}` (被调用 {top['calls']} 次)")
        
        return "\n".join(f"- {f}" for f in findings) if findings else "- 项目结构良好，无明显问题"
    
    def _generate_functional_overview(self) -> str:
        """生成功能概览"""
        sections = ["""## 功能概览

### 功能模块列表

"""]
        
        for feature_id, feature in list(self.architecture.features.items())[:20]:
            desc = feature.description or "无描述"
            if len(desc) > 100:
                desc = desc[:100] + "..."
            
            sections.append(f"""#### {feature.name}

- **描述**: {desc}
- **入口点**: {len(feature.entry_points)} 个
- **相关实体**: {len(feature.entities)} 个
- **复杂度**: {feature.complexity}
- **重要性**: {feature.importance:.1f}

""")
        
        return "".join(sections)
    
    def _generate_module_structure(self) -> str:
        """生成模块结构"""
        sections = ["""## 模块结构

### 模块概览

"""]
        
        for module_path, module in self.architecture.modules.items():
            sections.append(f"""#### `{module.path}`

| 属性 | 值 |
|------|-----|
| 文件数 | {module.file_count} |
| 实体数 | {module.entity_count} |
| 代码行数 | {module.lines_of_code:,} |
| 依赖模块 | {len(module.dependencies)} |
| 被依赖 | {len(module.dependents)} |

**主要类**: {', '.join(module.classes[:10]) or '无'}

**主要函数**: {', '.join(module.functions[:10]) or '无'}

""")
        
        # 添加模块依赖图
        sections.append("""### 模块依赖关系

```mermaid
""")
        sections.append(self.uml_generator.generate_component_diagram("mermaid"))
        sections.append("\n```\n")
        
        return "".join(sections)
    
    def _generate_interface_analysis(self) -> str:
        """生成接口分析"""
        sections = ["""## 接口分析

### 公共接口列表

"""]
        
        # 查找公共接口
        interfaces = []
        for entity in self.graph.entities.values():
            if entity.entity_type == EntityType.INTERFACE:
                interfaces.append(entity)
            elif entity.visibility == "public" and entity.entity_type in [
                EntityType.FUNCTION, EntityType.CLASS, EntityType.METHOD
            ]:
                # 检查是否作为接口使用
                callers = self.analyzer.callee_to_callers.get(entity.id, [])
                if len(callers) > 1:  # 被多个调用者使用
                    interfaces.append(entity)
        
        # 按被调用次数排序
        interfaces.sort(
            key=lambda e: len(self.analyzer.callee_to_callers.get(e.id, [])),
            reverse=True
        )
        
        for entity in interfaces[:30]:
            callers = len(self.analyzer.callee_to_callers.get(entity.id, []))
            sections.append(f"""#### `{entity.name}`

- **类型**: {entity.entity_type.value}
- **位置**: `{entity.file_path}:{entity.start_line}`
- **调用者数**: {callers}
- **参数**: {self._format_parameters(entity.parameters)}
- **返回类型**: {entity.return_type or 'N/A'}
- **文档**: {entity.docstring[:100] + '...' if entity.docstring and len(entity.docstring) > 100 else entity.docstring or '无'}

""")
        
        # API端点分析
        if self.architecture.api_endpoints:
            sections.append("""### API 端点

""")
            for endpoint in self.architecture.api_endpoints[:20]:
                handler = self.graph.entities.get(endpoint.handler)
                handler_name = handler.name if handler else "Unknown"
                sections.append(f"- **{endpoint.method}** `{endpoint.path}` → `{handler_name}`\n")
        
        return "".join(sections)
    
    def _generate_call_flow_analysis(self) -> str:
        """生成调用流程分析"""
        sections = ["""## 调用流程分析

### 主要调用链

"""]
        
        for i, chain in enumerate(self.architecture.call_chains[:10], 1):
            sections.append(f"""#### 调用链 {i}: {chain.name}

- **深度**: {chain.depth} 层
- **调用次数**: {chain.total_calls}
- **入口**: `{chain.entry_point}`

**调用路径**:

""")
            # 生成调用路径
            path_entities = []
            for node_id in chain.nodes[:15]:
                entity = self.graph.entities.get(node_id)
                if entity:
                    path_entities.append(f"`{entity.name}`")
            
            sections.append(" → ".join(path_entities))
            sections.append("\n\n")
            
            # 时序图
            sections.append("""**时序图**:

```mermaid
""")
            sections.append(self.uml_generator.generate_sequence_diagram(chain, "mermaid"))
            sections.append("\n```\n\n")
        
        return "".join(sections)
    
    def _generate_class_relationships(self) -> str:
        """生成类与关系"""
        sections = ["""## 类与关系

### 类继承层次

"""]
        
        # 收集继承关系
        inheritance_trees = {}
        for entity in self.graph.entities.values():
            if entity.entity_type == EntityType.CLASS:
                if not entity.bases:
                    inheritance_trees[entity.id] = self.analyzer.get_inheritance_tree(entity.id)
        
        # 显示继承树
        for root_id, tree in list(inheritance_trees.items())[:10]:
            root = self.graph.entities.get(root_id)
            if root:
                sections.append(f"**{root.name}** 层次:\n")
                sections.append(self._format_inheritance_tree(tree, 0))
                sections.append("\n")
        
        # 类图
        sections.append("""### 类关系图

```mermaid
""")
        sections.append(self.uml_generator.generate_class_diagram("mermaid"))
        sections.append("\n```\n")
        
        # 关系统计
        sections.append("""### 关系统计

""")
        rel_stats = Counter(r.relationship_type.value for r in self.graph.relationships)
        sections.append("| 关系类型 | 数量 |\n|----------|------|\n")
        for rel_type, count in rel_stats.most_common():
            sections.append(f"| {rel_type} | {count} |\n")
        
        return "".join(sections)
    
    def _generate_message_flow(self) -> str:
        """生成消息机制"""
        sections = ["""## 消息机制

"""]
        
        if not self.architecture.message_flows:
            sections.append("未检测到明显的消息/事件机制。\n")
            return "".join(sections)
        
        for event_name, flow in self.architecture.message_flows.items():
            sections.append(f"""### `{event_name}`

""")
            
            if flow.emitters:
                emitters = [self.graph.entities.get(eid) for eid in flow.emitters]
                emitter_names = [e.name for e in emitters if e]
                sections.append(f"- **发送者**: {', '.join(emitter_names)}\n")
            
            if flow.handlers:
                handlers = [self.graph.entities.get(hid) for hid in flow.handlers]
                handler_names = [h.name for h in handlers if h]
                sections.append(f"- **处理者**: {', '.join(handler_names)}\n")
            
            sections.append("\n")
        
        return "".join(sections)
    
    def _generate_exception_handling(self) -> str:
        """生成异常处理"""
        sections = ["""## 异常处理

### 异常流分析

"""]
        
        if not self.architecture.exception_flows:
            sections.append("未检测到显式的异常处理机制。\n")
            return "".join(sections)
        
        for exc_name, flow in self.architecture.exception_flows.items():
            sections.append(f"""#### `{exc_name}`

""")
            
            if flow.thrown_by:
                sections.append("**抛出位置**:\n")
                for thrower_id in flow.thrown_by[:5]:
                    entity = self.graph.entities.get(thrower_id)
                    if entity:
                        sections.append(f"- `{entity.file_path}:{entity.start_line}` - `{entity.name}`\n")
            
            if flow.caught_by:
                sections.append("\n**捕获位置**:\n")
                for catcher_id in flow.caught_by[:5]:
                    entity = self.graph.entities.get(catcher_id)
                    if entity:
                        sections.append(f"- `{entity.file_path}:{entity.start_line}` - `{entity.name}`\n")
            
            sections.append("\n")
        
        return "".join(sections)
    
    def _generate_complexity_analysis(self) -> str:
        """生成复杂度分析"""
        stats = self.architecture.statistics
        
        sections = [f"""## 复杂度分析

### 复杂度指标

| 指标 | 值 |
|------|-----|
| 平均圈复杂度 | {stats['average_complexity']:.2f} |
| 最大圈复杂度 | {stats['max_complexity']} |
| 高复杂度实体(>10) | {sum(1 for e in self.graph.entities.values() if e.complexity and e.complexity > 10)} |

### 最复杂实体 TOP 10

| 名称 | 类型 | 复杂度 | 位置 |
|------|------|--------|------|
"""]
        
        for entity_info in stats['most_complex_entities']:
            entity = self.graph.entities.get(entity_info['id'])
            if entity:
                sections.append(f"| `{entity.name}` | {entity.entity_type.value} | {entity_info['complexity']} | `{entity.file_path}:{entity.start_line}` |\n")
        
        sections.append("""
### 最被调用实体 TOP 10

| 名称 | 类型 | 调用次数 | 位置 |
|------|------|----------|------|
""")
        
        for entity_info in stats['most_called_entities']:
            entity = self.graph.entities.get(entity_info['id'])
            if entity:
                sections.append(f"| `{entity.name}` | {entity.entity_type.value} | {entity_info['calls']} | `{entity.file_path}:{entity.start_line}` |\n")
        
        return "".join(sections)
    
    def _generate_refactoring_recommendations(self) -> str:
        """生成重构建议"""
        sections = ["""## 重构建议

### 高优先级

"""]
        
        recommendations = []
        
        # 复杂度建议
        high_complexity = [
            e for e in self.graph.entities.values()
            if e.complexity and e.complexity > 15
        ]
        if high_complexity:
            recommendations.append(f"""#### 降低复杂度

以下实体圈复杂度过高，建议拆分或简化：

""")
            for entity in high_complexity[:5]:
                recommendations.append(f"- `{entity.name}` ({entity.file_path}:{entity.start_line}) - 复杂度: {entity.complexity}\n")
        
        # 循环依赖
        cyclic = self._detect_cyclic_dependencies()
        if cyclic:
            recommendations.append(f"""
#### 消除循环依赖

检测到 {len(cyclic)} 组循环依赖：

""")
            for cycle in cyclic[:5]:
                recommendations.append(f"- {' → '.join(cycle)}\n")
        
        # 重复代码检测（简化版）
        large_functions = [
            e for e in self.graph.entities.values()
            if e.entity_type in [EntityType.FUNCTION, EntityType.METHOD]
            and e.lines_of_code > 50
        ]
        if large_functions:
            recommendations.append(f"""
#### 拆分大函数

以下函数过长，建议拆分：

""")
            for entity in large_functions[:5]:
                recommendations.append(f"- `{entity.name}` ({entity.file_path}:{entity.start_line}) - {entity.lines_of_code} 行\n")
        
        # 低内聚模块
        low_cohesion_modules = [
            (path, m) for path, m in self.architecture.modules.items()
            if len(m.classes) > 5 and len(m.dependencies) > 3
        ]
        if low_cohesion_modules:
            recommendations.append(f"""
#### 提高模块内聚

以下模块可能存在低内聚问题：

""")
            for path, module in low_cohesion_modules[:5]:
                recommendations.append(f"- `{path}` - 类数: {len(module.classes)}, 依赖数: {len(module.dependencies)}\n")
        
        if not recommendations:
            sections.append("- 项目结构良好，暂无高优先级重构建议\n")
        else:
            sections.extend(recommendations)
        
        # 中优先级
        sections.append("""
### 中优先级

""")
        
        # 添加更多建议
        if self.architecture.api_endpoints:
            sections.append(f"- **API规范化**: 当前检测到 {len(self.architecture.api_endpoints)} 个API端点，建议统一接口风格\n")
        
        if len(self.architecture.modules) > 10:
            sections.append("- **模块重组**: 模块数量较多，考虑合并相关模块以提高内聚性\n")
        
        sections.append("""
### 低优先级

- 补充缺失的文档字符串
- 统一命名规范
- 添加类型注解
- 优化导入语句

""")
        
        return "".join(sections)
    
    def _generate_appendix(self) -> str:
        """生成附录"""
        sections = ["""## 附录

### A. 完整实体列表

<details>
<summary>点击展开</summary>

| 名称 | 类型 | 文件 | 行号 |
|------|------|------|------|
"""]
        
        for entity in list(self.graph.entities.values())[:200]:
            sections.append(f"| `{entity.name}` | {entity.entity_type.value} | `{entity.file_path}` | {entity.start_line}-{entity.end_line} |\n")
        
        sections.append("""
</details>

### B. UML图源码

<details>
<summary>PlantUML 类图</summary>

```
""")
        sections.append(self.uml_generator.generate_class_diagram("plantuml"))
        sections.append("""
```
</details>

### C. 分析配置

""")
        if self.graph.metadata:
            for key, value in self.graph.metadata.items():
                sections.append(f"- **{key}**: {value}\n")
        
        return "".join(sections)
    
    # 辅助方法
    def _format_entity_types_table(self, types: Counter) -> str:
        """格式化实体类型表格"""
        lines = []
        for type_name, count in types.most_common():
            lines.append(f"| {type_name} | {count} |")
        return "\n".join(lines)
    
    def _format_parameters(self, params: Optional[List[Dict]]) -> str:
        """格式化参数"""
        if not params:
            return "无参数"
        
        parts = []
        for p in params[:5]:
            name = p.get("name", "")
            type_ = p.get("type") or p.get("annotation")
            if type_:
                parts.append(f"{name}: {type_}")
            else:
                parts.append(name)
        
        result = ", ".join(parts)
        if len(params) > 5:
            result += ", ..."
        
        return result
    
    def _format_inheritance_tree(self, tree: Dict, depth: int) -> str:
        """格式化继承树"""
        entity = self.graph.entities.get(tree['id'])
        if not entity:
            return ""
        
        indent = "  " * depth
        result = f"{indent}- `{entity.name}`\n"
        
        for child in tree.get('children', []):
            result += self._format_inheritance_tree(child, depth + 1)
        
        return result
    
    def _detect_cyclic_dependencies(self) -> List[List[str]]:
        """检测循环依赖"""
        cycles = []
        visited = set()
        rec_stack = set()
        
        def dfs(node: str, path: List[str]):
            visited.add(node)
            rec_stack.add(node)
            
            module = self.architecture.modules.get(node)
            if module:
                for dep in module.dependencies:
                    if dep not in visited:
                        cycle = dfs(dep, path + [dep])
                        if cycle:
                            return cycle
                    elif dep in rec_stack:
                        cycle_start = path.index(dep)
                        return path[cycle_start:] + [dep]
            
            rec_stack.remove(node)
            return None
        
        for module_path in self.architecture.modules.keys():
            if module_path not in visited:
                cycle = dfs(module_path, [module_path])
                if cycle:
                    cycles.append(cycle)
        
        return cycles
    
    def export_to_file(self, output_path: str):
        """导出到文件"""
        report = self.generate_full_report()
        
        path = Path(output_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(path, "w", encoding="utf-8") as f:
            f.write(report)
        
        print(f"架构报告已导出到: {path}")
        
        # 同时导出UML图
        uml_dir = path.parent / "uml"
        uml_dir.mkdir(exist_ok=True)
        
        diagrams = self.uml_generator.generate_all("plantuml")
        for name, content in diagrams.items():
            uml_path = uml_dir / f"{name}.puml"
            with open(uml_path, "w", encoding="utf-8") as f:
                f.write(content)
        
        diagrams = self.uml_generator.generate_all("mermaid")
        for name, content in diagrams.items():
            uml_path = uml_dir / f"{name}_mermaid.md"
            with open(uml_path, "w", encoding="utf-8") as f:
                f.write(f"```mermaid\n{content}\n```")
        
        print(f"UML图已导出到: {uml_dir}")
