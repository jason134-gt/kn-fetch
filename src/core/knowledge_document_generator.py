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
        result = {"index": None, "entities": [], "modules": [], "architecture": [], "uml": [], "api": [], "message_flows": []}
        result["index"] = self._generate_index_document(graph)
        result["entities"] = self._generate_entity_documents(graph)
        result["modules"] = self._generate_module_documents(graph)
        result["architecture"] = self._generate_architecture_documents(graph)
        result["uml"] = self._generate_uml_documents(graph)
        
        # 新增：API端点文档
        result["api"] = self._generate_api_documents(graph)
        
        # 新增：消息流转流程文档
        result["message_flows"] = self._generate_message_flow_documents(graph)
        
        return result
    
    def _generate_index_document(self, graph) -> str:
        """生成索引文档 - Skill格式"""
        import yaml
        
        # Skill格式的YAML前置元数据
        frontmatter = {
            "type": "skill",
            "version": self.SKILL_VERSION,
            "category": "index",
            "created": datetime.now().isoformat(),
            "tags": ["index", "knowledge", "navigation"],
            "project_name": graph.project_name or '未知项目',
            "total_entities": len(graph.entities),
            "total_relationships": len(graph.relationships)
        }
        
        doc = "---\n" + yaml.dump(frontmatter, allow_unicode=True, default_flow_style=False, sort_keys=False) + "---\n\n"
        
        doc += f"""# 项目知识索引

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
├── index.md                    # 本索引文件
├── entities/                   # 实体知识文档（Skill格式）
│   ├── class/                 # 类定义
│   ├── method/                # 方法定义
│   ├── attribute/             # 属性定义
│   └── import/                # 导入声明
├── modules/                    # 模块知识文档
├── architecture/               # 架构知识文档
│   └── overview.md            # 架构概览
├── uml/                        # UML设计文档
│   ├── tech-stack.md          # 技术栈
│   ├── sequence-diagram-*.md  # 时序图
│   ├── class-diagram.md       # 类图
│   └── call-diagram.md        # 调用关系图
├── design/                     # LLM深度分析文档（需要API Key）
│   ├── project_overview.md    # 项目概述
│   ├── feature_modules.md     # 功能模块
│   └── core_entities.md       # 核心实体
├── business/                   # 业务流程文档（LLM生成）
│   └── business_flow.md       # 业务流程
└── api/                        # API接口文档
    └── *.md                   # 各服务API详情
```

## 🤖 AI智能体使用指南

### 1. 快速开始
- **入口**: 本索引文件提供全局导航
- **搜索**: 通过tags字段快速定位相关文档

### 2. 代码理解
- **实体文档** (`entities/`): 类、方法的详细信息和代码片段
- **模块文档** (`modules/`): 模块级别的知识组织

### 3. 架构分析
- **架构概览** (`architecture/overview.md`): 整体架构说明
- **UML图** (`uml/`): 时序图、类图、调用关系图

### 4. 业务理解（LLM生成）
- **项目概述** (`design/project_overview.md`): 项目定位、技术栈
- **业务流程** (`business/business_flow.md`): 核心业务流程图
- **功能模块** (`design/feature_modules.md`): 功能划分说明

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
        # 1. 技术选型文档
        lang_counts = defaultdict(int)
        file_set = set()
        for entity in graph.entities.values():
            file_set.add(entity.file_path)
            ext = Path(entity.file_path).suffix.lower()
            lang_counts[ext] += 1
        
        ext_to_lang = {'.py': 'Python', '.js': 'JavaScript', '.ts': 'TypeScript', '.java': 'Java', '.go': 'Go', '.cpp': 'C++', '.c': 'C'}
        tech_doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: uml\ncreated: {datetime.now().isoformat()}\ntags: [uml, tech-stack]\n---\n\n# 技术选型\n\n## 项目概览\n\n- **总文件数**: {len(file_set)}\n- **总实体数**: {len(graph.entities)}\n- **总关系数**: {len(graph.relationships)}\n\n## 编程语言分布\n\n"
        for ext, count in sorted(lang_counts.items(), key=lambda x: x[1], reverse=True):
            lang = ext_to_lang.get(ext, ext)
            tech_doc += f"- **{lang}**: {count} 个实体\n"
        
        tech_path = self.output_dir / "uml" / "tech-stack.md"
        with open(tech_path, "w", encoding="utf-8") as f:
            f.write(tech_doc)
        generated_files.append(str(tech_path))
        
        # 2. 生成时序图（对外服务调用链）
        if graph.relationships:
            sequence_diagrams = self._generate_service_sequence_diagrams(graph)
            for i, seq_doc in enumerate(sequence_diagrams, 1):
                seq_path = self.output_dir / "uml" / f"sequence-diagram-{i}.md"
                with open(seq_path, "w", encoding="utf-8") as f:
                    f.write(seq_doc)
                generated_files.append(str(seq_path))
        
        # 3. 核心类图
        class_entities = [e for e in graph.entities.values() if e.entity_type in ['class', 'interface']]
        if class_entities:
            class_diagram = self._generate_class_diagram(class_entities[:20], graph)
            class_path = self.output_dir / "uml" / "class-diagram.md"
            with open(class_path, "w", encoding="utf-8") as f:
                f.write(class_diagram)
            generated_files.append(str(class_path))
        
        # 4. 调用关系图
        if graph.relationships:
            call_diagram = self._generate_call_diagram(graph.relationships[:50], graph)
            call_path = self.output_dir / "uml" / "call-diagram.md"
            with open(call_path, "w", encoding="utf-8") as f:
                f.write(call_diagram)
            generated_files.append(str(call_path))
        
        # 5. 模块依赖图
        module_diagram = self._generate_module_diagram(graph)
        if module_diagram:
            module_path = self.output_dir / "uml" / "module-diagram.md"
            with open(module_path, "w", encoding="utf-8") as f:
                f.write(module_diagram)
            generated_files.append(str(module_path))
        
        return generated_files
    
    def _generate_service_sequence_diagrams(self, graph) -> List[str]:
        """生成对外服务的时序图"""
        diagrams = []
        
        # 按调用者分组关系
        caller_relationships = defaultdict(list)
        for rel in graph.relationships:
            if rel.relationship_type == 'calls':
                caller_relationships[rel.source_id].append(rel)
        
        # 找出调用其他方法最多的方法作为入口点
        top_callers = sorted(
            caller_relationships.items(),
            key=lambda x: len(x[1]),
            reverse=True
        )[:10]
        
        # 为每个入口点生成时序图
        for entry_id, calls in top_callers:
            entry_entity = graph.entities.get(entry_id)
            if not entry_entity:
                continue
            
            diagram = self._generate_sequence_from_metadata(entry_entity, calls, graph)
            if diagram:
                diagrams.append(diagram)
        
        # 如果没有找到合适的入口点，生成一个通用时序图
        if not diagrams and graph.relationships:
            diagram = self._generate_generic_sequence_diagram(graph)
            if diagram:
                diagrams.append(diagram)
        
        return diagrams
    
    def _generate_sequence_from_metadata(self, entry_entity, calls, graph) -> str:
        """从关系metadata生成时序图"""
        if not calls:
            return ""
        
        # 构建调用链
        call_chain = []
        visited_methods = {entry_entity.name}
        
        # 添加入口点
        call_chain.append({
            'name': entry_entity.name,
            'type': entry_entity.entity_type,
            'file': entry_entity.file_path,
            'line': entry_entity.start_line
        })
        
        # 添加直接调用的方法
        for rel in calls[:10]:  # 最多10个调用
            callee_name = rel.metadata.get('callee_name', '') if hasattr(rel, 'metadata') and rel.metadata else ''
            call_site = rel.call_site if hasattr(rel, 'call_site') else ''
            
            if callee_name and callee_name not in visited_methods:
                visited_methods.add(callee_name)
                
                # 查找被调用方法的实体
                callee_entity = None
                for entity in graph.entities.values():
                    if entity.name == callee_name and entity.entity_type == 'method':
                        callee_entity = entity
                        break
                
                call_chain.append({
                    'name': callee_name,
                    'type': 'method',
                    'entity': callee_entity,
                    'call_site': call_site
                })
        
        if len(call_chain) < 2:
            return ""
        
        # 生成文档
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: uml\ncreated: {datetime.now().isoformat()}\ntags: [uml, sequence-diagram]\n---\n\n"
        doc += f"# 服务时序图 - {entry_entity.name}\n\n"
        
        # 添加业务逻辑说明
        business_desc = self._infer_business_logic(entry_entity, graph)
        doc += f"## 业务逻辑说明\n\n{business_desc}\n\n"
        doc += f"**调用次数**: {len(calls)} 次方法调用\n\n"
        
        # 生成时序图
        doc += "## 时序图\n\n```mermaid\nsequenceDiagram\n    autonumber\n"
        
        # 添加参与者
        for item in call_chain:
            doc += f"    participant {item['name']}\n"
        
        # 添加消息
        for i in range(len(call_chain) - 1):
            source = call_chain[i]
            target = call_chain[i + 1]
            comment = self._infer_call_business_logic_simple(source['name'], target['name'])
            doc += f"    {source['name']}->>{target['name']}: {comment}\n"
        
        doc += "```\n\n"
        
        # 添加调用链说明
        doc += "## 调用链详情\n\n"
        for i, item in enumerate(call_chain, 1):
            doc += f"{i}. **{item['name']}** ({item.get('type', 'method')})\n"
            if 'file' in item:
                doc += f"   - 位置: {item['file']}:{item.get('line', '?')}\n"
            if 'call_site' in item:
                doc += f"   - 调用位置: {item['call_site']}\n"
            if 'entity' in item and item['entity']:
                entity = item['entity']
                if hasattr(entity, 'docstring') and entity.docstring:
                    doc += f"   - 说明: {entity.docstring[:100]}\n"
            
            # 添加业务逻辑推断
            if i > 1:
                logic = self._infer_method_purpose(item['name'])
                doc += f"   - 功能: {logic}\n"
        
        return doc
    
    def _infer_call_business_logic_simple(self, source_name: str, target_name: str) -> str:
        """简化版的调用业务逻辑推断"""
        # 从目标方法名推断
        target_lower = target_name.lower()
        
        if 'get' in target_lower or 'fetch' in target_lower or 'load' in target_lower:
            return "获取数据"
        elif 'save' in target_lower or 'create' in target_lower or 'add' in target_lower:
            return "保存数据"
        elif 'update' in target_lower or 'modify' in target_lower or 'edit' in target_lower:
            return "更新数据"
        elif 'delete' in target_lower or 'remove' in target_lower:
            return "删除数据"
        elif 'send' in target_lower or 'push' in target_lower or 'notify' in target_lower:
            return "发送通知"
        elif 'validate' in target_lower or 'check' in target_lower or 'verify' in target_lower:
            return "数据校验"
        elif 'process' in target_lower or 'handle' in target_lower:
            return "业务处理"
        elif 'cache' in target_lower:
            return "缓存操作"
        elif 'parse' in target_lower or 'convert' in target_lower:
            return "数据转换"
        else:
            return f"调用 {target_name}"
    
    def _infer_method_purpose(self, method_name: str) -> str:
        """推断方法用途"""
        name_lower = method_name.lower()
        
        # 常见的方法模式
        patterns = {
            'get': '数据查询',
            'fetch': '数据获取',
            'load': '数据加载',
            'save': '数据保存',
            'create': '数据创建',
            'update': '数据更新',
            'delete': '数据删除',
            'remove': '数据移除',
            'send': '消息发送',
            'push': '消息推送',
            'process': '业务处理',
            'handle': '事件处理',
            'validate': '数据验证',
            'check': '条件检查',
            'convert': '数据转换',
            'parse': '数据解析',
            'format': '数据格式化',
            'init': '初始化',
            'config': '配置管理',
            'cache': '缓存管理',
            'log': '日志记录'
        }
        
        for pattern, purpose in patterns.items():
            if pattern in name_lower:
                return purpose
        
        return "业务处理"
    
    def _generate_api_documents(self, graph) -> List[str]:
        """生成API接口文档"""
        generated_files = []
        
        # 识别API端点
        api_endpoints = self._identify_api_endpoints(graph)
        
        if not api_endpoints:
            return generated_files
        
        # 生成API文档
        api_doc = self._create_api_document(api_endpoints, graph)
        
        api_path = self.output_dir / "api"
        api_path.mkdir(exist_ok=True)
        
        # 生成API索引文档
        index_doc = self._create_api_index_document(api_endpoints, graph)
        index_path = api_path / "index.md"
        with open(index_path, "w", encoding="utf-8") as f:
            f.write(index_doc)
        generated_files.append(str(index_path))
        
        # 为每个API端点生成详细文档
        for endpoint in api_endpoints[:50]:  # 最多50个
            endpoint_doc = self._create_api_endpoint_document(endpoint, graph)
            safe_name = "".join(c if c.isalnum() or c in ['_', '-'] else '_' for c in endpoint['name'])
            if safe_name and len(safe_name) > 2:
                endpoint_path = api_path / f"{safe_name}.md"
                with open(endpoint_path, "w", encoding="utf-8") as f:
                    f.write(endpoint_doc)
                generated_files.append(str(endpoint_path))
        
        return generated_files
    
    def _identify_api_endpoints(self, graph) -> List[Dict]:
        """识别API端点 - 改进版"""
        endpoints = []
        
        # 常见的API端点模式
        api_patterns = [
            'Controller', 'API', 'Endpoint', 'Service', 'Handler',
            'Resource', 'Servlet', 'Client'
        ]
        
        # 构建类到方法的映射
        class_methods = {}
        for entity in graph.entities.values():
            if entity.entity_type == 'method':
                # 从文件路径推断所属类
                file_path = entity.file_path
                if file_path not in class_methods:
                    class_methods[file_path] = []
                class_methods[file_path].append(entity)
        
        # 遍历所有类实体
        for entity in graph.entities.values():
            if entity.entity_type in ['class', 'interface']:
                name = entity.name
                file_path = entity.file_path
                
                # 过滤掉不合法的名称
                if not name or len(name) < 2 or name.startswith('{') or '\n' in name:
                    continue
                
                # 检查是否匹配API端点模式
                is_api = False
                for pattern in api_patterns:
                    if pattern in name:
                        is_api = True
                        break
                
                # 检查文件路径
                if not is_api:
                    path_lower = file_path.lower()
                    if any(p in path_lower for p in ['api', 'controller', 'endpoint', 'handler', 'resource', 'service']):
                        is_api = True
                
                if is_api:
                    # 直接从同一文件中收集方法
                    methods = []
                    for method in class_methods.get(file_path, []):
                        # 确保方法是该类内部的（行号在类范围内）
                        if method.start_line >= entity.start_line and method.start_line <= entity.end_line:
                            # 过滤不合法的方法名
                            method_name = method.name
                            if (method_name and len(method_name) >= 2 and 
                                not method_name.startswith('{') and 
                                '\n' not in method_name and
                                '{' not in method_name and
                                '}' not in method_name and
                                not method_name.startswith('class_name') and
                                method_name.isidentifier() or 
                                (method_name[0].islower() or method_name.startswith('_'))):
                                
                                methods.append({
                                    'name': method_name,
                                    'id': method.id,
                                    'docstring': method.docstring if hasattr(method, 'docstring') else None,
                                    'file_path': method.file_path,
                                    'start_line': method.start_line,
                                    'parameters': method.parameters if hasattr(method, 'parameters') else [],
                                    'return_type': method.return_type if hasattr(method, 'return_type') else None,
                                    'lines_of_code': method.lines_of_code if hasattr(method, 'lines_of_code') else 0
                                })
                    
                    # 如果没有找到方法，收集文件中所有方法
                    if not methods:
                        for method in class_methods.get(file_path, [])[:20]:
                            methods.append({
                                'name': method.name,
                                'id': method.id,
                                'docstring': method.docstring if hasattr(method, 'docstring') else None,
                                'file_path': method.file_path,
                                'start_line': method.start_line,
                                'parameters': method.parameters if hasattr(method, 'parameters') else [],
                                'return_type': method.return_type if hasattr(method, 'return_type') else None,
                                'lines_of_code': method.lines_of_code if hasattr(method, 'lines_of_code') else 0
                            })
                    
                    endpoints.append({
                        'name': name,
                        'type': entity.entity_type,
                        'file_path': file_path,
                        'start_line': entity.start_line,
                        'end_line': entity.end_line,
                        'methods': methods,
                        'id': entity.id,
                        'docstring': entity.docstring if hasattr(entity, 'docstring') else None,
                        'attributes': entity.attributes if hasattr(entity, 'attributes') else [],
                        'lines_of_code': entity.lines_of_code if hasattr(entity, 'lines_of_code') else 0
                    })
        
        return endpoints
    
    def _create_api_document(self, endpoints, graph) -> str:
        """创建API文档"""
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: api\ncreated: {datetime.now().isoformat()}\ntags: [api, endpoints]\n---\n\n"
        doc += "# API接口文档\n\n"
        doc += f"## 概述\n\n"
        doc += f"检测到 {len(endpoints)} 个API端点类\n\n"
        doc += "## API端点列表\n\n"
        
        for i, endpoint in enumerate(endpoints[:100], 1):  # 最多100个
            # 过滤不合法名称
            name = endpoint['name']
            if not name or len(name) < 2 or '\n' in name or '{' in name:
                continue
            
            doc += f"### {i}. {name}\n\n"
            doc += f"- **类型**: {endpoint['type']}\n"
            doc += f"- **文件**: `{endpoint['file_path']}`\n"
            doc += f"- **方法数**: {len(endpoint['methods'])}\n"
            doc += f"- **代码行数**: {endpoint.get('lines_of_code', 'N/A')}\n\n"
            
            if endpoint.get('docstring'):
                doc += f"**说明**: {endpoint['docstring'][:200]}\n\n"
            
            if endpoint['methods']:
                doc += "#### 主要方法\n\n"
                for method in endpoint['methods'][:10]:
                    purpose = self._infer_method_purpose(method['name'])
                    doc += f"- `{method['name']}` - {purpose}\n"
                doc += "\n"
        
        return doc
    
    def _create_api_index_document(self, endpoints, graph) -> str:
        """创建API索引文档"""
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: api\ncreated: {datetime.now().isoformat()}\ntags: [api, index]\n---\n\n"
        doc += "# API索引\n\n"
        doc += f"共检测到 {len(endpoints)} 个API端点\n\n"
        
        # 按模块分组
        modules = {}
        for endpoint in endpoints:
            name = endpoint['name']
            if not name or len(name) < 2 or '\n' in name or '{' in name:
                continue
            
            # 从文件路径提取模块名
            file_path = endpoint['file_path']
            parts = file_path.replace('\\', '/').split('/')
            if len(parts) > 2:
                module = parts[-2] if parts[-2] != 'src' else 'root'
            else:
                module = 'root'
            
            if module not in modules:
                modules[module] = []
            modules[module].append(endpoint)
        
        doc += "## 按模块分组\n\n"
        for module, eps in sorted(modules.items()):
            doc += f"### {module}\n\n"
            for endpoint in eps[:20]:  # 每个模块最多20个
                safe_name = "".join(c if c.isalnum() or c in ['_', '-'] else '_' for c in endpoint['name'])
                method_count = len(endpoint['methods'])
                doc += f"- [{endpoint['name']}]({safe_name}.md) - {endpoint['type']} ({method_count} 方法)\n"
            doc += "\n"
        
        return doc
    
    def _create_api_endpoint_document(self, endpoint, graph) -> str:
        """创建单个API端点文档 - 增强版"""
        name = endpoint['name']
        
        # Skill格式元数据
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: api\ncreated: {datetime.now().isoformat()}\ntags: [api, endpoint, {name}]\nentity_id: {endpoint['id']}\nfile_path: {endpoint['file_path']}\nmethod_count: {len(endpoint['methods'])}\n---\n\n"
        
        doc += f"# {name}\n\n"
        
        # 基本信息
        doc += "## 基本信息\n\n"
        doc += f"| 属性 | 值 |\n|------|------|\n"
        doc += f"| 类型 | {endpoint['type']} |\n"
        doc += f"| 文件 | `{endpoint['file_path']}` |\n"
        doc += f"| 行号 | {endpoint['start_line']}-{endpoint.get('end_line', '?')} |\n"
        doc += f"| 代码行数 | {endpoint.get('lines_of_code', 'N/A')} |\n"
        doc += f"| 方法数 | {len(endpoint['methods'])} |\n\n"
        
        # 类说明
        if endpoint.get('docstring'):
            doc += "## 类说明\n\n"
            doc += f"{endpoint['docstring']}\n\n"
        
        # 属性列表
        if endpoint.get('attributes'):
            doc += "## 属性\n\n"
            doc += "| 属性名 | 类型 | 说明 |\n|--------|------|------|\n"
            for attr in endpoint['attributes'][:15]:
                attr_name = attr.get('name', '?') if isinstance(attr, dict) else str(attr)
                attr_type = attr.get('type', 'unknown') if isinstance(attr, dict) else 'unknown'
                doc += f"| `{attr_name}` | `{attr_type}` | - |\n"
            doc += "\n"
        
        # 方法列表
        if endpoint['methods']:
            doc += "## 方法列表\n\n"
            doc += "| 方法名 | 行号 | 参数 | 说明 |\n"
            doc += "|--------|------|------|------|\n"
            
            for method in endpoint['methods'][:30]:
                method_name = method['name']
                params = method.get('parameters', [])
                param_str = ', '.join([p.get('name', '?') if isinstance(p, dict) else str(p) for p in params[:5]]) if params else '-'
                
                if method.get('docstring'):
                    desc = method['docstring'][:60] + '...' if len(method['docstring']) > 60 else method['docstring']
                else:
                    desc = self._infer_method_purpose(method_name)
                
                doc += f"| `{method_name}` | {method['start_line']} | `{param_str}` | {desc} |\n"
            doc += "\n"
            
            # 方法详情
            doc += "## 方法详情\n\n"
            for method in endpoint['methods'][:10]:
                doc += f"### {method['name']}\n\n"
                doc += f"- **位置**: `{method['file_path']}:{method['start_line']}`\n"
                doc += f"- **代码行数**: {method.get('lines_of_code', 'N/A')}\n"
                
                if method.get('parameters'):
                    doc += f"- **参数**: {len(method['parameters'])} 个\n"
                if method.get('return_type'):
                    doc += f"- **返回类型**: `{method['return_type']}`\n"
                if method.get('docstring'):
                    doc += f"- **说明**: {method['docstring'][:200]}\n"
                doc += "\n"
        
        # 调用关系
        doc += "## 调用关系\n\n"
        # 查找该类的方法调用了哪些其他方法
        called_methods = set()
        for method in endpoint['methods']:
            for rel in graph.relationships:
                if rel.relationship_type == 'calls' and rel.source_id == method['id']:
                    callee_name = rel.metadata.get('callee_name', '') if hasattr(rel, 'metadata') and rel.metadata else ''
                    if callee_name:
                        called_methods.add(callee_name)
        
        if called_methods:
            doc += "**调用的方法**:\n"
            for cm in list(called_methods)[:10]:
                doc += f"- `{cm}`\n"
            doc += "\n"
        
        doc += "---\n*Skill格式 v{}*\n".format(self.SKILL_VERSION)
        
        return doc
    
    def _generate_message_flow_documents(self, graph) -> List[str]:
        """生成消息流转流程文档"""
        generated_files = []
        
        # 识别消息处理器
        message_handlers = self._identify_message_handlers(graph)
        
        if not message_handlers:
            return generated_files
        
        # 创建消息流目录
        msg_path = self.output_dir / "message-flows"
        msg_path.mkdir(exist_ok=True)
        
        # 生成消息流索引文档
        index_doc = self._create_message_flow_index(message_handlers)
        index_path = msg_path / "index.md"
        with open(index_path, "w", encoding="utf-8") as f:
            f.write(index_doc)
        generated_files.append(str(index_path))
        
        # 为每个消息处理器生成详细文档
        for handler in message_handlers[:15]:  # 最多15个
            handler_doc = self._create_message_handler_document(handler, graph)
            safe_name = "".join(c if c.isalnum() or c in ['_', '-'] else '_' for c in handler['name'])
            handler_path = msg_path / f"{safe_name}.md"
            with open(handler_path, "w", encoding="utf-8") as f:
                f.write(handler_doc)
            generated_files.append(str(handler_path))
        
        # 生成消息流转流程图
        flow_diagram = self._create_message_flow_diagram(message_handlers, graph)
        flow_path = msg_path / "flow-diagram.md"
        with open(flow_path, "w", encoding="utf-8") as f:
            f.write(flow_diagram)
        generated_files.append(str(flow_path))
        
        return generated_files
    
    def _identify_message_handlers(self, graph) -> List[Dict]:
        """识别消息处理器"""
        handlers = []
        
        # 常见的消息处理器模式
        handler_patterns = [
            'Handler', 'Listener', 'Consumer', 'Subscriber',
            'Processor', 'Receiver'
        ]
        
        # 遍历所有实体
        for entity in graph.entities.values():
            if entity.entity_type == 'class':
                name = entity.name
                
                # 检查是否匹配消息处理器模式
                is_handler = False
                for pattern in handler_patterns:
                    if pattern in name:
                        is_handler = True
                        break
                
                if is_handler:
                    # 查找该类的方法
                    methods = []
                    for rel in graph.relationships:
                        if rel.relationship_type == 'contains':
                            for e in graph.entities.values():
                                if e.id == rel.target_id and e.entity_type == 'method':
                                    # 检查是否是handle方法
                                    if 'handle' in e.name.lower() or 'process' in e.name.lower():
                                        methods.append({
                                            'name': e.name,
                                            'id': e.id,
                                            'docstring': e.docstring if hasattr(e, 'docstring') else None,
                                            'file_path': e.file_path,
                                            'start_line': e.start_line
                                        })
                    
                    handlers.append({
                        'name': name,
                        'file_path': entity.file_path,
                        'start_line': entity.start_line,
                        'methods': methods,
                        'id': entity.id,
                        'docstring': entity.docstring if hasattr(entity, 'docstring') else None
                    })
        
        return handlers
    
    def _create_message_flow_index(self, handlers) -> str:
        """创建消息流索引文档"""
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: message-flows\ncreated: {datetime.now().isoformat()}\ntags: [message, flow, index]\n---\n\n"
        doc += "# 消息流转流程索引\n\n"
        doc += f"检测到 {len(handlers)} 个消息处理器\n\n"
        doc += "## 消息处理器列表\n\n"
        
        for i, handler in enumerate(handlers, 1):
            safe_name = "".join(c if c.isalnum() or c in ['_', '-'] else '_' for c in handler['name'])
            doc += f"{i}. [{handler['name']}]({safe_name}.md)\n"
            doc += f"   - 位置: `{handler['file_path']}`\n"
            doc += f"   - 方法数: {len(handler['methods'])}\n"
        
        return doc
    
    def _create_message_handler_document(self, handler, graph) -> str:
        """创建单个消息处理器文档"""
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: message-flows\ncreated: {datetime.now().isoformat()}\ntags: [message, handler]\nentity_id: {handler['id']}\n---\n\n"
        doc += f"# {handler['name']}\n\n"
        
        # 基本信息
        doc += "## 基本信息\n\n"
        doc += f"- **文件**: `{handler['file_path']}`\n"
        doc += f"- **起始行**: {handler['start_line']}\n"
        
        if handler['docstring']:
            doc += f"- **说明**: {handler['docstring']}\n"
        
        doc += "\n"
        
        # 处理方法
        if handler['methods']:
            doc += "## 处理方法\n\n"
            
            for method in handler['methods']:
                doc += f"### {method['name']}\n\n"
                doc += f"- **位置**: `{method['file_path']}:{method['start_line']}`\n"
                
                if method['docstring']:
                    doc += f"- **说明**: {method['docstring']}\n\n"
                else:
                    # 推断方法用途
                    purpose = self._infer_method_purpose(method['name'])
                    doc += f"- **功能**: {purpose}\n\n"
        
        # 消息流
        doc += "## 消息流转流程\n\n"
        flow_desc = self._infer_message_flow(handler)
        doc += flow_desc + "\n"
        
        return doc
    
    def _infer_message_flow(self, handler) -> str:
        """推断消息流转流程"""
        name_lower = handler['name'].lower()
        
        # 基于名称推断消息类型
        if 'user' in name_lower:
            flow = "用户消息 → 接收处理 → 业务逻辑处理 → 推送给客户端\n"
        elif 'trade' in name_lower:
            flow = "交易消息 → 接收处理 → 更新交易状态 → 通知相关用户\n"
        elif 'stock' in name_lower:
            flow = "股票消息 → 接收处理 → 更新股票数据 → 推送给订阅者\n"
        elif 'remind' in name_lower:
            flow = "提醒消息 → 接收处理 → 生成提醒 → 推送给目标用户\n"
        elif 'mobile' in name_lower:
            flow = "移动端消息 → 接收处理 → 移动端推送 → 客户端接收\n"
        else:
            flow = "消息接收 → 业务处理 → 结果推送\n"
        
        # 添加调用链
        flow += "\n**调用流程**:\n"
        flow += "1. 接收消息事件（NotifyEvent/UserMsg）\n"
        flow += "2. 解析消息内容和属性\n"
        flow += "3. 执行业务逻辑处理\n"
        flow += "4. 推送处理结果或通知\n"
        
        return flow
    
    def _create_message_flow_diagram(self, handlers, graph) -> str:
        """创建消息流转流程图"""
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: message-flows\ncreated: {datetime.now().isoformat()}\ntags: [message, flow, diagram]\n---\n\n"
        doc += "# 消息流转流程图\n\n"
        doc += "## 整体架构\n\n"
        doc += "```mermaid\ngraph LR\n"
        doc += "    A[消息源] --> B[消息队列]\n"
        doc += "    B --> C[消息处理器]\n"
        doc += "    C --> D[业务处理]\n"
        doc += "    D --> E[结果推送]\n"
        doc += "```\n\n"
        
        doc += "## 消息处理器分布\n\n"
        doc += "```mermaid\nflowchart TB\n"
        
        # 按类型分组
        handler_groups = defaultdict(list)
        for handler in handlers:
            name = handler['name']
            if 'User' in name:
                handler_groups['User'].append(handler)
            elif 'Trade' in name:
                handler_groups['Trade'].append(handler)
            elif 'Stock' in name:
                handler_groups['Stock'].append(handler)
            elif 'Remind' in name:
                handler_groups['Remind'].append(handler)
            else:
                handler_groups['Other'].append(handler)
        
        # 生成分组图
        for group, group_handlers in handler_groups.items():
            doc += f"    subgraph {group}Group[{group}消息处理]\n"
            for handler in group_handlers[:5]:  # 每组最多5个
                safe_name = "".join(c if c.isalnum() or c in ['_', '-'] else '_' for c in handler['name'])
                doc += f"        {safe_name}[{handler['name']}]\n"
            doc += "    end\n"
        
        doc += "```\n\n"
        
        doc += "## 说明\n\n"
        doc += "- 消息处理器负责接收和处理各类消息事件\n"
        doc += "- 不同类型的消息由对应的Handler处理\n"
        doc += "- 处理完成后会推送给相关用户或客户端\n"
        
        return doc
    
    def _identify_service_entry_points(self, graph) -> List[Any]:
        """识别对外服务的入口点"""
        entry_points = []
        
        # 常见的对外服务入口点模式
        entry_patterns = [
            'Controller', 'Service', 'API', 'Endpoint', 'Handler',
            'main', 'run', 'execute', 'process', 'handle'
        ]
        
        for entity in graph.entities.values():
            if entity.entity_type in ['class', 'method', 'function']:
                # 检查名称是否匹配入口点模式
                for pattern in entry_patterns:
                    if pattern.lower() in entity.name.lower():
                        entry_points.append(entity)
                        break
                
                # 检查是否有公开方法
                if hasattr(entity, 'visibility') and entity.visibility == 'public':
                    if entity not in entry_points:
                        entry_points.append(entity)
        
        # 优先选择public方法
        entry_points.sort(key=lambda e: (
            0 if hasattr(e, 'visibility') and e.visibility == 'public' else 1,
            -len([r for r in graph.relationships if r.source_id == e.id])
        ))
        
        return entry_points[:10]
    
    def _generate_single_sequence_diagram(self, entry_point, graph) -> str:
        """生成单个服务的时序图"""
        # 追踪调用链
        call_chain = self._trace_call_chain(entry_point.id, graph, max_depth=6)
        
        if len(call_chain) < 2:
            return ""
        
        # 生成Mermaid时序图
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: uml\ncreated: {datetime.now().isoformat()}\ntags: [uml, sequence-diagram]\n---\n\n# 服务时序图 - {entry_point.name}\n\n"
        
        # 添加业务逻辑说明
        business_desc = self._infer_business_logic(entry_point, graph)
        if business_desc:
            doc += f"## 业务逻辑说明\n\n{business_desc}\n\n"
        
        doc += f"## 时序图\n\n```mermaid\nsequenceDiagram\n    autonumber\n"
        
        # 添加参与者
        participants = set()
        for entity_id in call_chain:
            entity = graph.entities.get(entity_id)
            if entity:
                participants.add(entity.name)
        
        for name in sorted(participants):
            doc += f"    participant {name}\n"
        
        # 生成消息
        edges_added = set()
        for i in range(len(call_chain) - 1):
            source_id = call_chain[i]
            target_id = call_chain[i + 1]
            source = graph.entities.get(source_id)
            target = graph.entities.get(target_id)
            
            if source and target:
                edge_key = f"{source_id}->{target_id}"
                if edge_key not in edges_added:
                    # 添加业务逻辑注释
                    comment = self._infer_call_business_logic(source, target)
                    doc += f"    {source.name}->>{target.name}: {comment}\n"
                    edges_added.add(edge_key)
        
        doc += "```\n\n## 调用链说明\n\n"
        for i, entity_id in enumerate(call_chain, 1):
            entity = graph.entities.get(entity_id)
            if entity:
                doc += f"{i}. **{entity.name}** ({entity.entity_type})\n"
                if hasattr(entity, 'docstring') and entity.docstring:
                    doc += f"   - 说明: {entity.docstring[:100]}\n"
                else:
                    doc += f"   - 位置: {entity.file_path}:{entity.start_line}\n"
        
        return doc
    
    def _trace_call_chain(self, entry_id: str, graph, max_depth: int) -> List[str]:
        """追踪调用链"""
        chain = [entry_id]
        visited = {entry_id}
        
        # 使用metadata构建调用关系映射
        caller_to_callees = defaultdict(list)
        callee_to_callers = defaultdict(list)
        
        for rel in graph.relationships:
            if rel.relationship_type == 'calls':
                source_id = rel.source_id
                target_name = rel.metadata.get('callee_name', '') if hasattr(rel, 'metadata') and rel.metadata else ''
                
                if target_name:
                    caller_to_callees[source_id].append((target_name, rel.call_site if hasattr(rel, 'call_site') else ''))
        
        def traverse(caller_id: str, depth: int):
            if depth > max_depth:
                return
            
            # 找到这个方法调用的所有方法
            callees = caller_to_callees.get(caller_id, [])
            
            for callee_name, call_site in callees[:3]:  # 最多追踪3个分支
                # 查找同名的方法实体
                callee_entity_id = None
                for entity in graph.entities.values():
                    if entity.name == callee_name and entity.entity_type == 'method':
                        callee_entity_id = entity.id
                        break
                
                if callee_entity_id and callee_entity_id not in visited:
                    visited.add(callee_entity_id)
                    chain.append(callee_entity_id)
                    traverse(callee_entity_id, depth + 1)
        
        traverse(entry_id, 0)
        return chain
    
    def _infer_business_logic(self, entity, graph) -> str:
        """推断业务逻辑说明"""
        descriptions = []
        
        # 从方法名推断
        name = entity.name
        if 'get' in name.lower():
            descriptions.append("获取数据操作")
        elif 'set' in name.lower():
            descriptions.append("设置数据操作")
        elif 'save' in name.lower() or 'create' in name.lower():
            descriptions.append("创建/保存操作")
        elif 'update' in name.lower() or 'modify' in name.lower():
            descriptions.append("更新/修改操作")
        elif 'delete' in name.lower() or 'remove' in name.lower():
            descriptions.append("删除操作")
        elif 'process' in name.lower() or 'handle' in name.lower():
            descriptions.append("业务处理操作")
        elif 'send' in name.lower() or 'push' in name.lower():
            descriptions.append("消息推送操作")
        
        # 从文件路径推断
        if 'service' in entity.file_path.lower():
            descriptions.append("服务层业务逻辑")
        elif 'controller' in entity.file_path.lower():
            descriptions.append("控制器层接口")
        elif 'db' in entity.file_path.lower():
            descriptions.append("数据访问层操作")
        
        return "、".join(descriptions) if descriptions else "业务处理流程"
    
    def _infer_call_business_logic(self, source, target) -> str:
        """推断调用关系的业务逻辑"""
        target_name = target.name
        
        # 从目标方法名推断
        if 'get' in target_name.lower():
            return "查询数据"
        elif 'save' in target_name.lower() or 'create' in target_name.lower():
            return "保存数据"
        elif 'update' in target_name.lower():
            return "更新数据"
        elif 'delete' in target_name.lower():
            return "删除数据"
        elif 'send' in target_name.lower() or 'push' in target_name.lower():
            return "发送消息"
        elif 'validate' in target_name.lower() or 'check' in target_name.lower():
            return "数据校验"
        else:
            return f"调用{target_name}"
    
    def _generate_generic_sequence_diagram(self, graph) -> str:
        """生成通用时序图"""
        # 选择有调用关系的方法生成时序图
        methods_with_calls = []
        for entity in graph.entities.values():
            if entity.entity_type == 'method':
                call_count = len([r for r in graph.relationships if r.source_id == entity.id])
                if call_count > 0:
                    methods_with_calls.append((entity, call_count))
        
        # 选择调用最多的方法
        methods_with_calls.sort(key=lambda x: x[1], reverse=True)
        top_methods = [m[0] for m in methods_with_calls[:5]]
        
        if not top_methods:
            return ""
        
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: uml\ncreated: {datetime.now().isoformat()}\ntags: [uml, sequence-diagram]\n---\n\n# 核心服务调用时序图\n\n> 展示系统中最活跃的服务调用链\n\n## 时序图\n\n```mermaid\nsequenceDiagram\n    autonumber\n"
        
        # 添加参与者
        for entity in top_methods:
            doc += f"    participant {entity.name}\n"
        
        # 添加调用关系
        for entity in top_methods:
            for rel in graph.relationships:
                if rel.source_id == entity.id and rel.relationship_type == 'calls':
                    target = graph.entities.get(rel.target_id)
                    if target and target in top_methods:
                        comment = self._infer_call_business_logic(entity, target)
                        doc += f"    {entity.name}->>{target.name}: {comment}\n"
                        break
        
        doc += "```\n\n## 说明\n\n此图展示了系统中调用最频繁的方法及其调用关系。\n"
        
        return doc
    
    def _generate_class_diagram(self, classes, graph) -> str:
        """生成类图（Mermaid格式）"""
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: uml\ncreated: {datetime.now().isoformat()}\ntags: [uml, class-diagram]\n---\n\n# 核心类图\n\n> 显示前 {len(classes)} 个核心类\n\n## 类图\n\n```mermaid\nclassDiagram\n"
        
        for entity in classes:
            class_name = entity.name
            doc += f"    class {class_name} {{\n"
            doc += f"        +{class_name}\n"
            doc += "    }\n"
        
        # 添加关系
        for rel in graph.relationships[:20]:
            source = graph.entities.get(rel.source_id)
            target = graph.entities.get(rel.target_id)
            if source and target and source.entity_type in ['class', 'interface'] and target.entity_type in ['class', 'interface']:
                if rel.relationship_type == 'calls':
                    doc += f"    {source.name} --> {target.name} : calls\n"
        
        doc += "```\n\n## 说明\n\n- 实线箭头表示调用关系\n- 类名显示类的标识\n- 详细的类信息请查看实体文档\n"
        
        return doc
    
    def _generate_call_diagram(self, relationships, graph) -> str:
        """生成调用关系图（Mermaid格式）"""
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: uml\ncreated: {datetime.now().isoformat()}\ntags: [uml, call-diagram]\n---\n\n# 核心调用关系图\n\n> 显示前 {len(relationships)} 个调用关系\n\n## 调用关系图\n\n```mermaid\nflowchart LR\n"
        
        # 生成节点
        shown_nodes = set()
        edge_count = 0
        
        for rel in relationships:
            source = graph.entities.get(rel.source_id)
            # 使用metadata中的名称作为target名称
            target_name = rel.metadata.get('callee_name', 'unknown') if hasattr(rel, 'metadata') and rel.metadata else 'unknown'
            source_name = source.name if source else (rel.metadata.get('caller_name', 'unknown') if hasattr(rel, 'metadata') and rel.metadata else 'unknown')
            
            # 清理名称中的特殊字符，用于Mermaid节点ID
            source_node_id = "".join(c if c.isalnum() else '_' for c in source_name)
            target_node_id = "".join(c if c.isalnum() else '_' for c in target_name)
            
            if source_node_id not in shown_nodes:
                doc += f"    {source_node_id}[\"{source_name}\"]\n"
                shown_nodes.add(source_node_id)
            
            if target_node_id not in shown_nodes:
                doc += f"    {target_node_id}[\"{target_name}\"]\n"
                shown_nodes.add(target_node_id)
            
            # 添加边
            doc += f"    {source_node_id} --> {target_node_id}\n"
            edge_count += 1
            
            if edge_count >= 30:  # 限制边的数量
                break
        
        doc += "```\n\n## 说明\n\n- 左侧为调用者，右侧为被调用者\n- 箭头方向表示调用方向\n- 更详细的调用信息请查看实体文档\n"
        
        return doc
    
    def _generate_module_diagram(self, graph) -> str:
        """生成模块依赖图"""
        # 按目录分组统计
        module_entities = defaultdict(lambda: {'count': 0, 'classes': [], 'methods': []})
        
        for entity in graph.entities.values():
            # 提取模块路径（取前3级目录）
            parts = Path(entity.file_path).parts
            module_path = '/'.join(parts[:min(3, len(parts))])
            
            module_entities[module_path]['count'] += 1
            if entity.entity_type == 'class':
                module_entities[module_path]['classes'].append(entity.name)
            elif entity.entity_type == 'method':
                module_entities[module_path]['methods'].append(entity.name)
        
        if not module_entities:
            return ""
        
        doc = f"---\ntype: skill\nversion: {self.SKILL_VERSION}\ncategory: uml\ncreated: {datetime.now().isoformat()}\ntags: [uml, module-diagram]\n---\n\n# 模块结构图\n\n> 显示主要模块及其规模\n\n## 模块图\n\n```mermaid\ngraph TB\n"
        
        # 只显示最大的10个模块
        top_modules = sorted(module_entities.items(), key=lambda x: x[1]['count'], reverse=True)[:10]
        
        for i, (module_path, info) in enumerate(top_modules):
            module_name = module_path.replace('/', '_').replace('\\\\', '_')
            doc += f"    {module_name}[{module_path}\\n实体数: {info['count']}\\n类: {len(info['classes'])}\\n方法: {len(info['methods'])}]\n"
        
        doc += "```\n\n## 模块统计\n\n| 模块路径 | 实体数 | 类数 | 方法数 |\n|---------|--------|------|--------|\n"
        
        for module_path, info in top_modules:
            doc += f"| {module_path} | {info['count']} | {len(info['classes'])} | {len(info['methods'])} |\n"
        
        doc += "\n## 说明\n\n- 节点大小反映模块规模\n- 更详细的模块信息请查看模块文档\n"
        
        return doc
    
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
