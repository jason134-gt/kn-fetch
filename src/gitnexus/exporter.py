import json
import csv
from typing import Dict, Any
from pathlib import Path
import networkx as nx
import markdown
from jinja2 import Template
import pandas as pd

from .models import KnowledgeGraph, ExportFormat, EntityType, RelationshipType
from .exceptions import ExportError


class KnowledgeExporter:
    """知识图谱导出器，支持多种导出格式"""
    
    def __init__(self, graph: KnowledgeGraph, config: Dict[str, Any]):
        self.graph = graph
        self.config = config
        self.output_config = config.get("output", {})
        self.sections = self.output_config.get("sections", [
            "project_overview",
            "architecture",
            "core_modules",
            "api_reference",
            "configuration",
            "usage_guide",
            "tech_stack",
            "development_standards"
        ])
    
    def export(self, output_path: str, format: ExportFormat) -> str:
        """导出知识图谱到指定格式"""
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        try:
            if format == ExportFormat.JSON:
                return self._export_json(output_path)
            elif format == ExportFormat.MARKDOWN:
                return self._export_markdown(output_path)
            elif format == ExportFormat.HTML:
                return self._export_html(output_path)
            elif format == ExportFormat.CSV:
                return self._export_csv(output_path)
            elif format == ExportFormat.GRAPHML:
                return self._export_graphml(output_path)
            elif format == ExportFormat.PLANTUML:
                return self._export_plantuml(output_path)
            elif format == ExportFormat.MERMAID:
                return self._export_mermaid(output_path)
            elif format == ExportFormat.MINDMAP:
                return self._export_mindmap(output_path)
            elif format == ExportFormat.ARCHITECTURE:
                return self._export_architecture(output_path)
            else:
                raise ExportError(f"不支持的导出格式: {format}")
        except Exception as e:
            raise ExportError(f"导出失败: {str(e)}")
    
    def _export_json(self, output_path: Path) -> str:
        """导出为JSON格式"""
        output_path = output_path.with_suffix(".json")
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(self.graph.model_dump(), f, ensure_ascii=False, indent=2)
        return str(output_path)
    
    def _export_markdown(self, output_path: Path) -> str:
        """导出为Markdown格式"""
        output_path = output_path.with_suffix(".md")
        
        content = f"# {self.config.get('project', {}).get('name', '项目知识库')}\n\n"
        content += f"> 生成时间: {self.graph.generated_at}\n"
        content += f"> 项目版本: {self.graph.project_version or '未知'}\n\n"
        
        if "project_overview" in self.sections:
            content += "## 项目概览\n\n"
            content += self.config.get('project', {}).get('description', '暂无描述') + "\n\n"
            stats = self._get_statistics()
            content += "### 统计信息\n\n"
            content += f"- 总文件数: {stats['total_files']}\n"
            content += f"- 代码实体数: {stats['total_entities']}\n"
            content += f"- 关系数: {stats['total_relationships']}\n"
            content += f"- 总代码行数: {stats['lines_of_code']}\n\n"
            
            content += "### 实体类型分布\n\n"
            for entity_type, count in stats['entity_types'].items():
                content += f"- {entity_type}: {count}\n"
            content += "\n"
        
        if "core_modules" in self.sections:
            content += "## 核心模块\n\n"
            
            # 按文件分组
            files = {}
            for entity in self.graph.entities.values():
                if entity.file_path not in files:
                    files[entity.file_path] = []
                files[entity.file_path].append(entity)
            
            for file_path, entities in files.items():
                content += f"### {file_path}\n\n"
                for entity in entities:
                    if entity.entity_type in ["class", "function", "module"]:
                        content += f"#### {entity.entity_type.capitalize()}: {entity.name}\n\n"
                        if entity.docstring:
                            content += f"{entity.docstring}\n\n"
                        content += f"- 位置: 第 {entity.start_line} - {entity.end_line} 行\n"
                        if entity.parameters:
                            params = [p["name"] + (f": {p['annotation']}" if p['annotation'] else "") for p in entity.parameters]
                            content += f"- 参数: {', '.join(params)}\n"
                        if entity.return_type:
                            content += f"- 返回类型: {entity.return_type}\n"
                        content += "\n"
        
        if "api_reference" in self.sections:
            content += "## API参考\n\n"
            for entity in self.graph.entities.values():
                if entity.entity_type in ["function", "method"] and len(entity.name) > 0 and not entity.name.startswith("_"):
                    content += f"### {entity.name}\n\n"
                    if entity.docstring:
                        content += f"{entity.docstring}\n\n"
                    if entity.parameters:
                        content += "**参数:**\n\n"
                        for param in entity.parameters:
                            annotation = param["annotation"] or "Any"
                            content += f"- `{param['name']}`: {annotation}\n"
                        content += "\n"
                    if entity.return_type:
                        content += f"**返回:** {entity.return_type}\n\n"
        
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(content)
        
        return str(output_path)
    
    def _export_html(self, output_path: Path) -> str:
        """导出为HTML格式"""
        md_path = output_path.with_suffix(".md")
        self._export_markdown(md_path)
        
        with open(md_path, "r", encoding="utf-8") as f:
            md_content = f.read()
        
        html_content = markdown.markdown(md_content, extensions=['extra', 'toc', 'codehilite'])
        
        html_template = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <title>{{ title }}</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; line-height: 1.6; }
                h1, h2, h3, h4 { color: #2c3e50; margin-top: 2em; }
                pre { background: #f8f9fa; padding: 15px; border-radius: 5px; overflow-x: auto; }
                code { background: #f1f3f4; padding: 2px 6px; border-radius: 3px; }
                .toc { background: #f8f9fa; padding: 20px; border-radius: 5px; margin-bottom: 30px; }
                .toc ul { list-style: none; padding-left: 20px; }
            </style>
        </head>
        <body>
            {{ content }}
        </body>
        </html>
        """
        
        template = Template(html_template)
        rendered = template.render(
            title=self.config.get('project', {}).get('name', '项目知识库'),
            content=html_content
        )
        
        output_path = output_path.with_suffix(".html")
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(rendered)
        
        return str(output_path)
    
    def _export_csv(self, output_path: Path) -> str:
        """导出为CSV格式"""
        # 导出实体
        entities_path = output_path.with_stem(output_path.stem + "_entities").with_suffix(".csv")
        entity_data = []
        for entity in self.graph.entities.values():
            entity_data.append({
                "id": entity.id,
                "type": entity.entity_type,
                "name": entity.name,
                "file_path": entity.file_path,
                "start_line": entity.start_line,
                "end_line": entity.end_line,
                "lines_of_code": entity.lines_of_code,
                "parent_id": entity.parent_id or ""
            })
        
        df = pd.DataFrame(entity_data)
        df.to_csv(entities_path, index=False, encoding="utf-8-sig")
        
        # 导出关系
        rels_path = output_path.with_stem(output_path.stem + "_relationships").with_suffix(".csv")
        rel_data = []
        for rel in self.graph.relationships:
            rel_data.append({
                "source_id": rel.source_id,
                "target_id": rel.target_id,
                "relationship_type": rel.relationship_type,
                "confidence": rel.confidence
            })
        
        df = pd.DataFrame(rel_data)
        df.to_csv(rels_path, index=False, encoding="utf-8-sig")
        
        return f"{entities_path}, {rels_path}"
    
    def _export_graphml(self, output_path: Path) -> str:
        """导出为GraphML格式（可用于Neo4j等图数据库）"""
        G = nx.DiGraph()
        
        # 添加节点
        for entity_id, entity in self.graph.entities.items():
            G.add_node(
                entity_id,
                name=entity.name,
                type=entity.entity_type,
                file_path=entity.file_path,
                start_line=entity.start_line,
                end_line=entity.end_line,
                lines_of_code=entity.lines_of_code
            )
        
        # 添加边
        for rel in self.graph.relationships:
            G.add_edge(
                rel.source_id,
                rel.target_id,
                type=rel.relationship_type,
                confidence=rel.confidence
            )
        
        output_path = output_path.with_suffix(".graphml")
        nx.write_graphml(G, output_path)
        return str(output_path)
    
    def _export_plantuml(self, output_path: Path) -> str:
        """导出为PlantUML类图格式"""
        content = "@startuml\n"
        content += "skinparam class {\n    BackgroundColor White\n    BorderColor Black\n    ArrowColor Black\n}\n\n"
        
        # 处理类
        for entity in self.graph.entities.values():
            if entity.entity_type == "class":
                content += f"class {entity.name} {{\n"
                # 添加方法
                for method in self.graph.entities.values():
                    if method.entity_type == "method" and method.parent_id == entity.id:
                        params = []
                        for p in method.parameters or []:
                            params.append(p["name"])
                        return_type = method.return_type or "void"
                        content += f"    {return_type} {method.name}({', '.join(params)})\n"
                content += "}\n\n"
        
        # 处理继承关系
        for rel in self.graph.relationships:
            if rel.relationship_type == "inherits":
                source = self.graph.entities.get(rel.source_id)
                target = self.graph.entities.get(rel.target_id)
                if source and target:
                    content += f"{source.name} <|-- {target.name}\n"
        
        content += "@enduml"
        
        output_path = output_path.with_suffix(".puml")
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(content)
        
        return str(output_path)
    
    def _export_mindmap(self, output_path: Path) -> str:
        """导出为Markmap思维导图格式"""
        content = f"# {self.config.get('project', {}).get('name', '项目知识库')}\n\n"
        
        content += "## 项目结构\n\n"
        files = {}
        for entity in self.graph.entities.values():
            parts = Path(entity.file_path).parts
            current = files
            for part in parts[:-1]:
                if part not in current:
                    current[part] = {}
                current = current[part]
            if parts[-1] not in current:
                current[parts[-1]] = []
            current[parts[-1]].append(entity)
        
        def render_tree(tree, level=2):
            res = ""
            for key, value in tree.items():
                if isinstance(value, dict):
                    res += f"{'#' * level} {key}\n\n"
                    res += render_tree(value, level + 1)
                else:
                    res += f"{'#' * level} {key}\n\n"
                    for entity in value:
                        if entity.entity_type in ["class", "function"]:
                            res += f"- {entity.entity_type.capitalize()}: {entity.name}\n"
                    res += "\n"
            return res
        
        content += render_tree(files)
        
        output_path = output_path.with_stem(output_path.stem + "_mindmap").with_suffix(".md")
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(content)
        
        return str(output_path)
    
    def _get_statistics(self) -> Dict[str, Any]:
        """获取统计信息"""
        stats = {
            "total_files": len(set(e.file_path for e in self.graph.entities.values())),
            "total_entities": len(self.graph.entities),
            "total_relationships": len(self.graph.relationships),
            "lines_of_code": sum(e.lines_of_code for e in self.graph.entities.values()),
            "entity_types": {}
        }
        
        for entity in self.graph.entities.values():
            entity_type = entity.entity_type.value if hasattr(entity.entity_type, 'value') else str(entity.entity_type)
            stats["entity_types"][entity_type] = stats["entity_types"].get(entity_type, 0) + 1
        
        return stats
    
    def _export_mermaid(self, output_path: Path) -> str:
        """导出为Mermaid格式"""
        lines = ["graph TD"]
        
        # 添加节点
        for entity_id, entity in self.graph.entities.items():
            if entity.entity_type in [EntityType.CLASS, EntityType.INTERFACE, EntityType.FUNCTION]:
                label = entity.name.replace('"', "'")
                node_id = entity_id[:8]
                lines.append(f'  {node_id}["{label}"]')
        
        # 添加关系
        for rel in self.graph.relationships:
            rel_type = rel.relationship_type.value if hasattr(rel.relationship_type, 'value') else str(rel.relationship_type)
            source_id = rel.source_id[:8]
            target_id = rel.target_id[:8]
            
            if rel_type in ["inherits", "extends"]:
                lines.append(f'  {source_id} -->|inherits| {target_id}')
            elif rel_type in ["implements"]:
                lines.append(f'  {source_id} -->|implements| {target_id}')
            elif rel_type in ["calls"]:
                lines.append(f'  {source_id} -->|calls| {target_id}')
            elif rel_type in ["uses", "depends_on"]:
                lines.append(f'  {source_id} -->|uses| {target_id}')
        
        output_path = output_path.with_suffix(".mmd")
        with open(output_path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines))
        
        return str(output_path)
    
    def _export_architecture(self, output_path: Path) -> str:
        """导出完整的架构分析报告"""
        # 使用架构报告导出器
        from ..core.architecture_reporter import ArchitectureReporter
        
        reporter = ArchitectureReporter(self.graph)
        reporter.export_to_file(str(output_path.with_suffix(".md")))
        
        return str(output_path.with_suffix(".md"))
