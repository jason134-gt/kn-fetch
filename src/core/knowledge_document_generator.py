"""
知识文档生成器 - 将知识图谱转换为智能体可读的Skill格式文档
支持层级化、结构化的知识组织，便于重构智能体读取和理解

Skill格式说明：
- 采用YAML前置元数据 + Markdown内容
- 便于AI智能体快速解析和理解
- 支持标签化检索和关联分析
"""
import os
import json
import hashlib
from typing import Dict, List, Any, Optional, Set
from pathlib import Path
from datetime import datetime
from collections import defaultdict


class KnowledgeDocumentGenerator:
    """知识文档生成器 - Skill格式"""
    
    SKILL_VERSION = "1.0"
    
    def __init__(self, output_dir: str = "output/doc"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        (self.output_dir / "entities").mkdir(exist_ok=True)
        (self.output_dir / "modules").mkdir(exist_ok=True)
        (self.output_dir / "architecture").mkdir(exist_ok=True)
        (self.output_dir / "uml").mkdir(exist_ok=True)
        self._processed_entities: Set[str] = set()
    
    def generate_all_documents(self, graph) -> Dict[str, Any]:
        """生成所有知识文档"""
        result = {"index": None, "entities": [], "modules": [], "architecture": [], "uml": []}
        result["index"] = self._generate_index_document(graph)
        result["entities"] = self._generate_entity_documents(graph)
        result["modules"] = self._generate_module_documents(graph)
        result["architecture"] = self._generate_architecture_documents(graph)
        result["uml"] = self._generate_uml_documents(graph)
        return result
    
    def _generate_index_document(self, graph) -> str:
        """生成索引文档"""
        doc = f"""# 项目知识索引

> 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
> 项目名称: {graph.project_name or '未知项目'}
> Skill格式版本: v{self.SKILL_VERSION}

## 📊 项目概览

| 指标 | 数量 |
|------|------|
| 总实体数 | {len(graph.entities)} |
| 总关系数 | {len(graph.relationships)} |
| 文件数 | {len(set(e.file_path for e in graph.entities.values()))} |

## 📁 知识结构

```
doc/
├── index.md              # 本索引文件
├── entities/             # 实体知识文档（类Skill格式）
│   ├── class/           # 类定义
│   ├── function/        # 函数定义
│   └── method/          # 方法定义
├── modules/              # 模块知识文档
├── architecture/         # 架构知识文档
└── uml/                  # UML设计文档
```

## 🤖 重构智能体使用指南

1. **读取实体知识**: 访问 `entities/` 目录下的文档
2. **理解模块结构**: 访问 `modules/` 目录下的文档
3. **架构分析**: 访问 `architecture/` 目录下的文档
4. **UML设计参考**: 访问 `uml/` 目录下的文档

---
*由知识提取智能体自动生成 | Skill格式 v{self.SKILL_VERSION}*
"""
        index_path = self.output_dir / "index.md"
        with open(index_path, "w", encoding="utf-8") as f:
            f.write(doc)
        return str(index_path)
    
    def _generate_entity_documents(self, graph) -> List[str]:
        """生成实体知识文档"""
        generated_files = []
        entities_by_type = defaultdict(list)
        for entity in graph.entities.values():
            entities_by_type[entity.entity_type].append(entity)
        
        for entity_type, entities in entities_by_type.items():
            type_dir = self.output_dir / "entities" / entity_type
            type_dir.mkdir(parents=True, exist_ok=True)
            
            index_doc = f"# {entity_type.upper()} 实体知识\n\n> 实体数量: {len(entities)}\n\n## 实体列表\n\n"
            
            for entity in entities[:100]:
                entity_file = self._generate_single_entity_document(entity, graph, type_dir)
                if entity_file:
                    generated_files.append(entity_file)
                    rel_path = entity_file.replace(str(self.output_dir) + "\\", "").replace(str(self.output_dir) + "/", "")
                    index_doc += f"- [{entity.name}]({rel_path})\n"
            
            index_path = type_dir / "index.md"
            with open(index_path, "w", encoding="utf-8") as f:
                f.write(index_doc)
            generated_files.append(str(index_path))
        
        return generated_files
    
    def _generate_single_entity_document(self, entity, graph, output_dir: Path) -> Optional[str]:
        """生成单个实体知识文档（Skill格式）"""
        try:
            entity_signature = self._calculate_entity_signature(entity)
            dependencies = self._get_entity_dependencies(entity.id, graph)
            
            import yaml
            frontmatter = {
                "type": "skill",
                "version": self.SKILL_VERSION,
                "category": "entities",
                "entity_type": str(entity.entity_type.value) if hasattr(entity.entity_type, 'value') else str(entity.entity_type),
                "entity_id": entity.id,
                "signature": entity_signature,
                "created": datetime.now().isoformat(),
                "file_path": entity.file_path,
                "start_line": entity.start_line,
                "end_line": entity.end_line,
                "lines_of_code": entity.lines_of_code,
                "tags": ["entity", str(entity.entity_type.value) if hasattr(entity.entity_type, 'value') else str(entity.entity_type), entity.name],
                "related_entities": list(set([d['name'] for d in dependencies['out'][:5]] + [d['name'] for d in dependencies['in'][:5]]))[:10],
                "dependencies": {"out": [d['name'] for d in dependencies['out'][:10]], "in": [d['name'] for d in dependencies['in'][:10]]},
                "metrics": {"complexity": getattr(entity, 'complexity', None), "param_count": len(entity.parameters) if entity.parameters else 0}
            }
            
            doc = "---\n" + yaml.dump(frontmatter, allow_unicode=True, default_flow_style=False, sort_keys=False) + "---\n"
            doc += f"\n# {entity.name}\n\n> **类型**: `{entity.entity_type}` | **文件**: `{entity.file_path}` | **行数**: {entity.start_line}-{entity.end_line} ({entity.lines_of_code}行)\n\n## 📋 概述\n\n"
            
            if entity.docstring:
                doc += f"**说明**:\n\n```\n{entity.docstring[:500]}{'...' if len(entity.docstring) > 500 else ''}\n```\n\n"
            else:
                doc += "*该实体缺少文档说明*\n\n"
            
            if entity.parameters:
                doc += "## 📥 参数\n\n| 名称 | 类型 | 说明 |\n|------|------|------|\n"
                for param in entity.parameters[:15]:
                    if isinstance(param, dict):
                        doc += f"| `{param.get('name', '?')}` | `{param.get('type', 'any')}` | - |\n"
                    else:
                        doc += f"| `{param}` | - | - |\n"
                doc += "\n"
            
            if hasattr(entity, 'return_type') and entity.return_type:
                doc += f"## 📤 返回值\n\n**类型**: `{entity.return_type}`\n\n"
            
            if dependencies['out'] or dependencies['in']:
                doc += "## 🔗 依赖关系\n\n"
                if dependencies['out']:
                    doc += "### 调用/依赖\n\n"
                    for dep in dependencies['out'][:10]:
                        doc += f"- `{dep['name']}` ({dep['type']}) - *{dep['rel_type']}*\n"
                    doc += "\n"
                if dependencies['in']:
                    doc += "### 被调用/被依赖\n\n"
                    for dep in dependencies['in'][:10]:
                        doc += f"- `{dep['name']}` ({dep['type']}) - *{dep['rel_type']}*\n"
                    doc += "\n"
            
            if entity.content:
                lang = self._get_language_from_file(entity.file_path)
                if len(entity.content) < 2500:
                    doc += f"## 💻 代码实现\n\n```{lang}\n{entity.content}\n```\n\n"
                else:
                    doc += f"## 💻 代码片段（节选）\n\n```{lang}\n{entity.content[:1800]}\n// ... 省略 {len(entity.content) - 1800} 字符 ...\n```\n\n"
            
            refactoring_hints = self._generate_refactoring_hints(entity)
            if refactoring_hints:
                doc += f"## 🔧 重构建议\n\n{refactoring_hints}\n\n"
            
            doc += f"---\n## 🤖 AI智能体指南\n\n### 快速定位\n- **文件路径**: `{entity.file_path}`\n- **起始行号**: {entity.start_line}\n- **搜索关键词**: `{entity.name}`\n\n### 签名追踪\n- **签名**: `{entity_signature[:16]}...`\n\n---\n*由知识提取智能体自动生成 | Skill格式 v{self.SKILL_VERSION}*\n"
            
            safe_name = "".join(c if c.isalnum() or c in ['_', '-'] else '_' for c in entity.name)
            file_path = output_dir / f"{safe_name}_{entity.id[:8]}.md"
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(doc)
            
            self._processed_entities.add(entity.id)
            return str(file_path)
        except Exception as e:
            print(f"生成实体文档失败 {entity.name}: {e}")
            return None
    
    def _calculate_entity_signature(self, entity) -> str:
        """计算实体签名"""
        content = f"{entity.name}|{entity.entity_type}|{entity.start_line}|{entity.end_line}"
        if entity.parameters:
            content += "|" + str(entity.parameters)
        if entity.docstring:
            content += "|" + entity.docstring[:100]
        return hashlib.md5(content.encode()).hexdigest()
    
    def _generate_module_documents(self, graph) -> List[str]:
        """生成模块知识文档"""
        generated_files = []
        modules = defaultdict(list)
        for entity in graph.entities.values():
            parts = entity.file_path.replace("\\", "/").split("/")
            module_path = "/".join(parts[:-1]) if len(parts) > 1 else "root"
            modules[module_path].append(entity)
        
        for module_path, entities in modules.items():
            entity_types = defaultdict(int)
            total_lines = sum(e.lines_of_code for e in entities)
            for entity in entities:
                etype = str(entity.entity_type.value) if hasattr(entity.entity_type, 'value') else str(entity.entity_type)
                entity_types[etype] += 1
            
            doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: modules\nmodule_path: {module_path}\ncreated: {datetime.now().isoformat()}\ntags: [module]\n---\n\n# 模块: {module_path}\n\n> 实体数量: {len(entities)} | 代码行数: {total_lines}\n\n## 实体类型分布\n\n| 类型 | 数量 |\n|------|------|\n"
            for etype, count in sorted(entity_types.items()):
                doc += f"| {etype} | {count} |\n"
            
            classes = [e for e in entities if e.entity_type == "class"]
            if classes:
                doc += "\n## 主要类\n\n"
                for cls in classes[:10]:
                    doc += f"- **{cls.name}** ({cls.lines_of_code} 行)\n"
            
            functions = [e for e in entities if e.entity_type == "function"]
            if functions:
                doc += "\n## 主要函数\n\n"
                for func in functions[:15]:
                    doc += f"- **{func.name}()** ({func.lines_of_code} 行)\n"
            
            doc += f"\n---\n*Skill格式 v{self.SKILL_VERSION}*\n"
            
            safe_name = module_path.replace("/", "_").replace("\\", "_")
            file_path = self.output_dir / "modules" / f"{safe_name}.md"
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(doc)
            generated_files.append(str(file_path))
        
        return generated_files
    
    def _generate_architecture_documents(self, graph) -> List[str]:
        """生成架构知识文档"""
        generated_files = []
        
        # 架构概览
        entity_types = defaultdict(int)
        for entity in graph.entities.values():
            entity_types[entity.entity_type] += 1
        
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: architecture\ncreated: {datetime.now().isoformat()}\ntags: [architecture, overview]\n---\n\n# 架构概览\n\n> 项目: {graph.project_name or '未知'} | 实体: {len(graph.entities)} | 关系: {len(graph.relationships)}\n\n## 实体类型分布\n\n"
        for etype, count in sorted(entity_types.items()):
            doc += f"- **{etype}**: {count}\n"
        
        arch_patterns = self._detect_architecture_patterns(graph)
        if arch_patterns:
            doc += "\n## 检测到的架构模式\n\n"
            for pattern in arch_patterns:
                doc += f"- {pattern}\n"
        
        doc += f"\n---\n*Skill格式 v{self.SKILL_VERSION}*\n"
        
        overview_path = self.output_dir / "architecture" / "overview.md"
        with open(overview_path, "w", encoding="utf-8") as f:
            f.write(doc)
        generated_files.append(str(overview_path))
        
        return generated_files
    
    def _generate_uml_documents(self, graph) -> List[str]:
        """生成UML设计文档"""
        generated_files = []
        
        # 技术选型
        lang_counts = defaultdict(int)
        for entity in graph.entities.values():
            ext = Path(entity.file_path).suffix.lower()
            lang_counts[ext] += 1
        
        ext_to_lang = {'.py': 'Python', '.js': 'JavaScript', '.ts': 'TypeScript', '.java': 'Java', '.go': 'Go'}
        tech_doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: uml\ncreated: {datetime.now().isoformat()}\ntags: [uml, tech-stack]\n---\n\n# 技术选型\n\n## 编程语言\n\n"
        for ext, count in sorted(lang_counts.items(), key=lambda x: x[1], reverse=True):
            lang = ext_to_lang.get(ext, ext)
            tech_doc += f"- **{lang}**: {count} 个文件\n"
        
        tech_path = self.output_dir / "uml" / "tech-stack.md"
        with open(tech_path, "w", encoding="utf-8") as f:
            f.write(tech_doc)
        generated_files.append(str(tech_path))
        
        return generated_files
    
    # ==================== 辅助方法 ====================
    
    def _get_entity_dependencies(self, entity_id: str, graph) -> Dict[str, List]:
        """获取实体的依赖关系"""
        deps = {'out': [], 'in': []}
        for rel in graph.relationships:
            target = graph.entities.get(rel.target_id)
            source = graph.entities.get(rel.source_id)
            if rel.source_id == entity_id and target:
                deps['out'].append({'name': target.name, 'type': target.entity_type, 'rel_type': rel.relationship_type})
            if rel.target_id == entity_id and source:
                deps['in'].append({'name': source.name, 'type': source.entity_type, 'rel_type': rel.relationship_type})
        return deps
    
    def _get_language_from_file(self, file_path: str) -> str:
        """从文件路径获取语言类型"""
        ext_map = {'.py': 'python', '.js': 'javascript', '.ts': 'typescript', '.java': 'java', '.go': 'go', '.rs': 'rust', '.cpp': 'cpp', '.c': 'c', '.cs': 'csharp'}
        return ext_map.get(Path(file_path).suffix.lower(), 'text')
    
    def _generate_refactoring_hints(self, entity) -> str:
        """生成重构建议"""
        hints = []
        if entity.lines_of_code > 50:
            hints.append(f"- 方法过长 ({entity.lines_of_code} 行)，建议拆分")
        if hasattr(entity, 'complexity') and entity.complexity and entity.complexity > 10:
            hints.append(f"- 圈复杂度较高 ({entity.complexity})，建议简化")
        if entity.parameters and len(entity.parameters) > 5:
            hints.append(f"- 参数过多 ({len(entity.parameters)} 个)，建议使用参数对象")
        if not entity.docstring and entity.entity_type in ['class', 'function', 'method']:
            hints.append("- 缺少文档说明")
        return "\n".join(hints) if hints else ""
    
    def _detect_architecture_patterns(self, graph) -> List[str]:
        """检测架构模式"""
        patterns = []
        dir_keywords = defaultdict(int)
        for entity in graph.entities.values():
            path = entity.file_path.lower()
            if 'controller' in path: dir_keywords['controller'] += 1
            if 'service' in path: dir_keywords['service'] += 1
            if 'model' in path or 'entity' in path: dir_keywords['model'] += 1
        if dir_keywords['controller'] > 0 and dir_keywords['model'] > 0:
            patterns.append("MVC架构")
        if dir_keywords['service'] > 0:
            patterns.append("分层架构")
        return patterns
    
    def get_document_list(self) -> Dict[str, Any]:
        """获取所有文档列表"""
        result = {"index": None, "entities": [], "modules": [], "architecture": [], "uml": [], "statistics": {"total_files": 0, "by_category": {}}}
        index_path = self.output_dir / "index.md"
        if index_path.exists():
            result["index"] = str(index_path)
        for category in ["entities", "modules", "architecture", "uml"]:
            category_dir = self.output_dir / category
            if category_dir.exists():
                files = list(category_dir.rglob("*.md"))
                result[category] = [str(f) for f in files]
                result["statistics"]["by_category"][category] = len(files)
                result["statistics"]["total_files"] += len(files)
        return result
    
    def get_document_content(self, doc_path: str) -> Optional[str]:
        """获取文档内容"""
        path = Path(doc_path)
        if path.exists() and path.suffix == '.md':
            with open(path, "r", encoding="utf-8") as f:
                return f.read()
        return None
