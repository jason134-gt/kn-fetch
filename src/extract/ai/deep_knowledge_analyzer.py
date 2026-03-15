"""
深度知识分析器 - 利用LLM生成详细设计文档
生成业务流程、架构说明、功能模块、实体说明等设计文档
"""
import json
import logging
from typing import Dict, List, Any, Optional
from pathlib import Path
from datetime import datetime
from collections import defaultdict

from .llm_client import LLMClient

logger = logging.getLogger(__name__)


class DeepKnowledgeAnalyzer:
    """深度知识分析器 - LLM增强版"""
    
    def __init__(self, config: Dict[str, Any], output_dir: str = "output/doc"):
        self.config = config
        self.output_dir = Path(output_dir)
        self.llm = LLMClient(config.get('ai', {}))
        
        # 确保目录存在
        (self.output_dir / "design").mkdir(parents=True, exist_ok=True)
        (self.output_dir / "business").mkdir(parents=True, exist_ok=True)
        (self.output_dir / "architecture").mkdir(parents=True, exist_ok=True)
    
    def is_available(self) -> bool:
        """检查LLM是否可用"""
        return self.llm.is_available()
    
    def analyze_code_entity(self, code: str, file_path: str) -> List[Dict[str, Any]]:
        """分析单个代码实体"""
        if not self.is_available():
            logger.warning("LLM不可用，跳过代码实体分析")
            return []
        
        try:
            prompt = f"""请分析以下Python代码，提取其中的主要实体（类、函数、方法）及其关系：

文件路径: {file_path}

```python
{code}
```

请返回JSON格式的实体列表，每个实体包含以下字段：
- name: 实体名称
- type: 实体类型（class/function/method）
- description: 实体描述
- file_path: 文件路径
- start_line: 起始行号
- end_line: 结束行号

只返回JSON格式的数据，不要有其他内容。"""
            
            response = self.llm.chat_sync(
                system_prompt="你是一个代码分析专家，擅长从代码中提取结构化信息。",
                user_prompt=prompt,
                max_tokens=2000,
                temperature=0.1
            )
            
            if response:
                # 尝试解析JSON响应
                import json
                try:
                    # 提取JSON部分（可能包含代码块标记）
                    if "```json" in response:
                        json_str = response.split("```json")[1].split("```")[0].strip()
                    elif "```" in response:
                        json_str = response.split("```")[1].split("```")[0].strip()
                    else:
                        json_str = response.strip()
                    
                    entities = json.loads(json_str)
                    if isinstance(entities, list):
                        return entities
                    else:
                        logger.warning("LLM返回的实体格式不正确")
                        return []
                except json.JSONDecodeError as e:
                    logger.warning(f"LLM返回的JSON解析失败: {e}")
                    return []
            else:
                return []
                
        except Exception as e:
            logger.error(f"代码实体分析失败: {e}")
            return []
    
    def analyze_all(self, graph, source_dir: str = None) -> Dict[str, Any]:
        """执行完整的深度分析"""
        results = {}
        
        if not self.is_available():
            logger.warning("LLM不可用，跳过深度分析")
            return {"error": "LLM不可用，请配置API Key"}
        
        print("\n" + "="*60)
        print("开始LLM深度知识分析...")
        print("="*60)
        
        # 1. 项目概述分析
        print("\n[1/6] 生成项目概述...")
        results["project_overview"] = self._analyze_project_overview(graph, source_dir)
        
        # 2. 业务流程分析
        print("[2/6] 分析业务流程...")
        results["business_flows"] = self._analyze_business_flows(graph)
        
        # 3. 架构设计分析
        print("[3/6] 分析架构设计...")
        results["architecture"] = self._analyze_architecture(graph)
        
        # 4. 功能模块分析
        print("[4/6] 分析功能模块...")
        results["features"] = self._analyze_features(graph)
        
        # 5. 核心实体分析
        print("[5/6] 分析核心实体...")
        results["entities"] = self._analyze_core_entities(graph)
        
        # 6. API接口分析
        print("[6/6] 分析API接口...")
        results["api"] = self._analyze_api_interfaces(graph)
        
        print("\n深度分析完成！")
        return results
    
    def _analyze_project_overview(self, graph, source_dir: str = None) -> Dict[str, Any]:
        """分析项目概述"""
        # 收集项目信息
        entities_summary = self._get_entities_summary(graph)
        main_classes = self._get_main_classes(graph)
        main_functions = self._get_main_functions(graph)
        
        # 获取关键类的完整代码
        class_code_samples = self._get_class_code_samples(main_classes[:5], graph)
        
        prompt = f"""请分析以下代码项目，生成一份详细的项目概述文档。

## 项目统计
- 实体总数: {len(graph.entities)}
- 关系总数: {len(graph.relationships)}
- 文件数量: {len(set(e.file_path for e in graph.entities.values()))}

## 主要类 ({len(main_classes)}个)
{self._format_entities_for_prompt(main_classes[:15])}

## 主要函数 ({len(main_functions)}个)
{self._format_entities_for_prompt(main_functions[:20])}

## 实体类型分布
{json.dumps(entities_summary, ensure_ascii=False, indent=2)}

## 关键类代码示例
{class_code_samples}

请生成以下内容（使用Markdown格式）：
1. **项目定位** - 这个项目是做什么的？解决什么问题？
2. **技术栈** - 使用了哪些技术和框架？
3. **核心功能** - 主要功能模块有哪些？每个模块的职责是什么？
4. **设计理念** - 从代码结构推断设计思路和架构模式
5. **适用场景** - 适合什么场景使用？
6. **关键API** - 列出主要的对外服务接口

请用中文回答，内容要专业、详细、结构化。"""

        result = self._call_llm(prompt, "你是一个资深的软件架构师，擅长分析代码结构和设计理念。")
        
        if result:
            self._save_document("design/project_overview.md", result, {
                "type": "design",
                "category": "project_overview",
                "generated_at": datetime.now().isoformat()
            })
        
        return {"content": result, "file": "design/project_overview.md"}
    
    def _get_class_code_samples(self, classes, graph) -> str:
        """获取类的代码示例"""
        samples = []
        for cls in classes:
            if hasattr(cls, 'content') and cls.content:
                sample = f"### {cls.name}\n```java\n{cls.content[:1500]}\n```\n"
                samples.append(sample)
        return "\n".join(samples[:3])
    
    def _analyze_business_flows(self, graph) -> Dict[str, Any]:
        """分析业务流程"""
        # 获取主要类和方法
        classes = [e for e in graph.entities.values() if e.entity_type.value == 'class']
        methods = [e for e in graph.entities.values() if e.entity_type.value == 'method']
        
        # 收集关键流程信息
        flow_info = self._extract_flow_info(graph)
        
        prompt = f"""请分析以下代码，绘制核心业务流程图和说明。

## 核心类 ({len(classes)}个)
{self._format_entities_for_prompt(classes[:10])}

## 主要方法流程
{flow_info}

请生成以下内容：
1. **主业务流程** - 描述核心业务是如何流转的
2. **流程图** - 使用Mermaid语法绘制核心业务流程图
3. **关键节点说明** - 解释每个关键步骤的作用
4. **数据流向** - 数据是如何在各个模块间流转的
5. **异常处理流程** - 错误是如何被处理和传播的

请用中文回答，流程图使用Mermaid flowchart语法。"""

        result = self._call_llm(prompt, "你是一个业务分析师，擅长从代码中梳理业务流程。")
        
        if result:
            self._save_document("business/business_flow.md", result, {
                "type": "business",
                "category": "flow",
                "generated_at": datetime.now().isoformat()
            })
        
        return {"content": result, "file": "business/business_flow.md"}
    
    def _analyze_architecture(self, graph) -> Dict[str, Any]:
        """分析架构设计"""
        # 模块分析
        modules = self._group_by_module(graph)
        
        # 收集架构信息
        arch_info = self._extract_architecture_info(graph)
        
        prompt = f"""请分析以下代码的架构设计。

## 模块结构
{json.dumps({k: {"count": len(v), "types": list(set(e.entity_type.value for e in v))} for k, v in modules.items()}, ensure_ascii=False, indent=2)}

## 架构信息
{arch_info}

## 主要组件
{self._get_components_info(graph)}

请生成以下内容：
1. **架构风格** - 这是什么架构风格？(分层、微服务、事件驱动等)
2. **架构图** - 使用Mermaid绘制架构图
3. **分层说明** - 描述各层的职责
4. **模块职责** - 每个模块负责什么
5. **依赖关系** - 模块间的依赖关系
6. **设计模式** - 使用了哪些设计模式
7. **扩展性分析** - 架构的可扩展性如何

请用中文回答，架构图使用Mermaid graph语法。"""

        result = self._call_llm(prompt, "你是一个软件架构师，精通各种架构模式和设计原则。")
        
        if result:
            self._save_document("architecture/architecture_design.md", result, {
                "type": "architecture",
                "category": "design",
                "generated_at": datetime.now().isoformat()
            })
        
        return {"content": result, "file": "architecture/architecture_design.md"}
    
    def _analyze_features(self, graph) -> Dict[str, Any]:
        """分析功能模块"""
        # 按功能域分组
        feature_groups = self._group_by_feature(graph)
        
        features_info = []
        for feature_name, entities in feature_groups.items():
            classes = [e for e in entities if e.entity_type.value == 'class']
            functions = [e for e in entities if e.entity_type.value in ['function', 'method']]
            
            features_info.append(f"""
### {feature_name}
- 类数量: {len(classes)}
- 方法数量: {len(functions)}
- 主要类: {', '.join([c.name for c in classes[:5]])}
""")
        
        prompt = f"""请分析以下代码的功能模块划分。

## 功能域分析
{''.join(features_info[:10])}

请为每个主要功能模块生成：
1. **功能名称** - 清晰的功能命名
2. **功能描述** - 这个功能是做什么的
3. **使用场景** - 什么情况下使用这个功能
4. **输入输出** - 功能的输入是什么，输出是什么
5. **依赖关系** - 依赖哪些其他功能
6. **使用示例** - 如何使用这个功能的代码示例

请用中文回答，使用Markdown格式。"""

        result = self._call_llm(prompt, "你是一个产品经理和技术文档专家，擅长功能分析和文档编写。")
        
        if result:
            self._save_document("design/feature_modules.md", result, {
                "type": "design",
                "category": "features",
                "generated_at": datetime.now().isoformat()
            })
        
        return {"content": result, "file": "design/feature_modules.md"}
    
    def _analyze_core_entities(self, graph) -> Dict[str, Any]:
        """分析核心实体（业务视角）"""
        # 获取核心业务类
        core_classes = self._identify_core_entities(graph)
        
        entities_info = []
        for entity in core_classes[:15]:
            info = f"""
### {entity.name}
- 文件: {entity.file_path}
- 行数: {entity.start_line}-{entity.end_line}
- 代码行: {entity.lines_of_code}
"""
            if entity.docstring:
                info += f"- 说明: {entity.docstring[:200]}"
            if entity.attributes:
                info += f"- 属性: {', '.join([a.get('name', '') for a in entity.attributes[:10]])}"
            entities_info.append(info)
        
        prompt = f"""请从业务视角分析以下核心实体（类）。

## 核心实体
{''.join(entities_info)}

请为每个核心实体生成：
1. **业务含义** - 这个实体在业务中代表什么
2. **职责边界** - 它负责什么，不负责什么
3. **生命周期** - 实例是如何创建、使用、销毁的
4. **关键属性** - 最重要的属性及其业务含义
5. **业务规则** - 与这个实体相关的业务规则
6. **关联实体** - 与哪些其他实体有关系

请用中文回答，使用DDD（领域驱动设计）的视角来分析。"""

        result = self._call_llm(prompt, "你是一个领域驱动设计专家，擅长从业务视角分析代码实体。")
        
        if result:
            self._save_document("design/core_entities.md", result, {
                "type": "design",
                "category": "entities",
                "generated_at": datetime.now().isoformat()
            })
        
        return {"content": result, "file": "design/core_entities.md"}
    
    def _analyze_api_interfaces(self, graph) -> Dict[str, Any]:
        """分析API接口"""
        # 提取API相关信息
        api_info = self._extract_api_info(graph)
        
        if not api_info:
            return {"content": "未检测到明显的API接口定义", "file": None}
        
        prompt = f"""请分析以下API接口信息。

## API信息
{api_info}

请生成API文档，包括：
1. **接口概览** - 有哪些主要接口
2. **接口详情** - 每个接口的详细说明
   - 请求方法和路径
   - 请求参数
   - 响应格式
   - 使用示例
3. **认证方式** - 如何认证
4. **错误码说明** - 常见错误及处理
5. **调用示例** - curl或代码示例

请用中文回答，使用Markdown格式。"""

        result = self._call_llm(prompt, "你是一个API文档专家，擅长编写清晰的技术文档。")
        
        if result:
            self._save_document("design/api_reference.md", result, {
                "type": "design",
                "category": "api",
                "generated_at": datetime.now().isoformat()
            })
        
        return {"content": result, "file": "design/api_reference.md"}
    
    # ===== 辅助方法 =====
    
    def _call_llm(self, prompt: str, system_prompt: str) -> str:
        """调用LLM"""
        try:
            return self.llm.chat_sync(
                system_prompt=system_prompt,
                user_prompt=prompt,
                max_tokens=4000,
                temperature=0.3
            )
        except Exception as e:
            logger.error(f"LLM调用失败: {e}")
            return ""
    
    def _save_document(self, relative_path: str, content: str, metadata: Dict[str, Any]):
        """保存文档 - Skill格式"""
        file_path = self.output_dir / relative_path
        
        # 确保符合Skill格式标准
        skill_metadata = {
            "type": "skill",
            "version": "1.0",
            "category": metadata.get("category", "design"),
            "created": metadata.get("generated_at", datetime.now().isoformat()),
            "tags": self._generate_tags(relative_path, metadata)
        }
        
        # 如果有entity_id，添加到元数据
        if "entity_id" in metadata:
            skill_metadata["entity_id"] = metadata["entity_id"]
        
        # 添加YAML前置元数据
        import yaml
        frontmatter = "---\n" + yaml.dump(skill_metadata, allow_unicode=True, default_flow_style=False, sort_keys=False) + "---\n\n"
        
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(frontmatter + content)
        
        print(f"  已保存: {relative_path}")
    
    def _generate_tags(self, relative_path: str, metadata: Dict[str, Any]) -> List[str]:
        """根据文档类型生成标签"""
        tags = []
        
        # 根据路径推断标签
        if "design" in relative_path:
            tags.append("design")
        if "business" in relative_path:
            tags.append("business")
        if "architecture" in relative_path:
            tags.append("architecture")
        
        # 根据category添加标签
        category = metadata.get("category", "")
        if category:
            tags.append(category)
        
        # 去重并确保有标签
        tags = list(dict.fromkeys(tags))
        if not tags:
            tags = ["document"]
        
        return tags
    
    def _get_entities_summary(self, graph) -> Dict[str, int]:
        """获取实体类型统计"""
        summary = defaultdict(int)
        for entity in graph.entities.values():
            etype = entity.entity_type.value if hasattr(entity.entity_type, 'value') else str(entity.entity_type)
            summary[etype] += 1
        return dict(summary)
    
    def _get_main_classes(self, graph) -> List:
        """获取主要类"""
        classes = [e for e in graph.entities.values() if e.entity_type.value == 'class']
        return sorted(classes, key=lambda x: x.lines_of_code or 0, reverse=True)
    
    def _get_main_functions(self, graph) -> List:
        """获取主要函数"""
        functions = [e for e in graph.entities.values() if e.entity_type.value in ['function', 'method']]
        return sorted(functions, key=lambda x: x.lines_of_code or 0, reverse=True)
    
    def _format_entities_for_prompt(self, entities: List) -> str:
        """格式化实体用于prompt"""
        lines = []
        for e in entities:
            doc = e.docstring[:100] if e.docstring else "无说明"
            lines.append(f"- **{e.name}** ({e.file_path}:{e.start_line})\n  {doc}")
        return "\n".join(lines)
    
    def _group_by_module(self, graph) -> Dict[str, List]:
        """按模块分组"""
        modules = defaultdict(list)
        for entity in graph.entities.values():
            parts = entity.file_path.replace("\\", "/").split("/")
            if len(parts) > 1:
                module = "/".join(parts[:-1])
            else:
                module = "root"
            modules[module].append(entity)
        return dict(modules)
    
    def _group_by_feature(self, graph) -> Dict[str, List]:
        """按功能域分组"""
        features = defaultdict(list)
        
        # 基于命名和模块推断功能域
        feature_keywords = {
            'collector': '数据采集',
            'crawler': '数据采集',
            'fetcher': '数据采集',
            'parser': '数据解析',
            'analyzer': '分析处理',
            'processor': '处理引擎',
            'aggregator': '聚合汇总',
            'reporter': '报告生成',
            'report': '报告生成',
            'trader': '交易决策',
            'trade': '交易决策',
            'expert': '专家分析',
            'ai': 'AI分析',
            'llm': 'AI分析',
            'scheduler': '任务调度',
            'schedule': '任务调度',
            'config': '配置管理',
            'util': '工具函数',
            'helper': '工具函数',
            'model': '数据模型',
            'data': '数据处理',
            'provider': '数据提供',
            'api': 'API接口',
            'exporter': '导出功能',
            'validator': '验证功能'
        }
        
        for entity in graph.entities.values():
            name_lower = entity.name.lower()
            file_lower = entity.file_path.lower()
            
            matched = False
            for keyword, feature in feature_keywords.items():
                if keyword in name_lower or keyword in file_lower:
                    features[feature].append(entity)
                    matched = True
                    break
            
            if not matched:
                features['其他'].append(entity)
        
        return dict(features)
    
    def _identify_core_entities(self, graph) -> List:
        """识别核心业务实体"""
        classes = [e for e in graph.entities.values() if e.entity_type.value == 'class']
        
        # 计算重要性分数
        scored = []
        for cls in classes:
            score = 0
            # 代码行数
            score += (cls.lines_of_code or 0) / 10
            # 有文档说明
            if cls.docstring:
                score += 5
            # 有属性
            if cls.attributes:
                score += len(cls.attributes)
            # 名称暗示核心性
            core_names = ['analyzer', 'manager', 'service', 'handler', 'controller', 'engine', 'client']
            if any(n in cls.name.lower() for n in core_names):
                score += 10
            
            scored.append((cls, score))
        
        # 按分数排序
        scored.sort(key=lambda x: x[1], reverse=True)
        return [s[0] for s in scored]
    
    def _extract_flow_info(self, graph) -> str:
        """提取流程信息"""
        info = []
        
        # 查找主要流程方法
        flow_methods = ['run', 'execute', 'process', 'analyze', 'collect', 'start', 'main']
        for entity in graph.entities.values():
            if entity.entity_type.value in ['function', 'method']:
                if any(m in entity.name.lower() for m in flow_methods):
                    doc = entity.docstring or "无说明"
                    info.append(f"- **{entity.name}** ({entity.file_path})\n  {doc[:150]}")
        
        return "\n".join(info[:20])
    
    def _extract_architecture_info(self, graph) -> str:
        """提取架构信息"""
        info = []
        
        # 收集模块信息
        modules = self._group_by_module(graph)
        for module, entities in sorted(modules.items(), key=lambda x: len(x[1]), reverse=True)[:10]:
            types = defaultdict(int)
            for e in entities:
                types[e.entity_type.value] += 1
            info.append(f"- **{module}**: {len(entities)}个实体, 类型分布: {dict(types)}")
        
        return "\n".join(info)
    
    def _get_components_info(self, graph) -> str:
        """获取组件信息"""
        classes = self._get_main_classes(graph)[:10]
        info = []
        for cls in classes:
            attrs = len(cls.attributes) if cls.attributes else 0
            info.append(f"- **{cls.name}** ({cls.lines_of_code}行, {attrs}属性)")
        return "\n".join(info)
    
    def _extract_api_info(self, graph) -> str:
        """提取对外API接口信息 - 只提取Action类（HTTP接口）"""
        info = []
        
        # 只查找Action类 - 这些是对外的HTTP接口
        action_classes = []
        for entity in graph.entities.values():
            if entity.entity_type.value == 'class':
                name = entity.name
                # Action类是对外的HTTP接口，排除基类BaseAction
                if 'Action' in name and name != 'BaseAction':
                    # 过滤不合法名称
                    if name and len(name) > 2 and '\n' not in name and '{' not in name:
                        action_classes.append(entity)
        
        # 按代码行数排序
        action_classes.sort(key=lambda x: x.lines_of_code or 0, reverse=True)
        
        for entity in action_classes[:30]:  # 最多30个Action类
            name = entity.name
            doc = entity.docstring or "无说明"
            
            # 收集该类的公开方法（对外接口）
            methods_info = []
            for method in graph.entities.values():
                if method.entity_type.value == 'method':
                    # 检查是否属于该类
                    if (method.file_path == entity.file_path and 
                        method.start_line >= entity.start_line and 
                        method.start_line <= (entity.end_line or 999999)):
                        
                        # 只收集公开方法
                        visibility = getattr(method, 'visibility', 'public')
                        if visibility == 'public' or 'public' in str(getattr(method, 'modifiers', [])):
                            params = method.parameters or []
                            params_str = ", ".join([p.get('name', '?') if isinstance(p, dict) else str(p) for p in params]) if params else ""
                            
                            method_doc = method.docstring or ""
                            return_type = getattr(method, 'return_type', 'void') or 'void'
                            
                            m = "    - `" + method.name + "(" + params_str + ")` -> " + return_type + "\n"
                            m += "      - 位置: " + method.file_path + ":" + str(method.start_line) + "\n"
                            m += "      - 说明: " + (method_doc[:100] if method_doc else '无说明') + "\n"
                            methods_info.append(m)
            
            # 获取类的代码片段
            class_code = ""
            if hasattr(entity, 'content') and entity.content:
                class_code = "\n```java\n" + entity.content[:1500] + "\n```\n"
            
            # 推断API路径
            api_path = "/" + name.replace("Action", "").lower()
            
            item = "\n### " + name + "\n"
            item += "- **API路径**: `" + api_path + "`\n"
            item += "- 文件: " + entity.file_path + "\n"
            item += "- 行号: " + str(entity.start_line) + "-" + str(entity.end_line or '?') + "\n"
            item += "- 代码行数: " + str(entity.lines_of_code or 0) + "\n"
            item += "- 类说明: " + doc[:300] + "\n"
            item += class_code + "\n"
            item += "**对外接口方法** (" + str(len(methods_info)) + "个):\n"
            item += ''.join(methods_info[:15]) + "\n"
            info.append(item)
        
        return "\n".join(info) if info else ""
