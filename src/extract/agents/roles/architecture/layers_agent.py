"""
Layers Agent - 分层结构分析

负责分析系统的逻辑分层和职责边界
"""
from typing import Dict, Any, List
from pathlib import Path
import json

from ...base.agent import BaseAgent, AgentResult
from ...config.agent_configs import LAYERS_AGENT


class LayersAgent(BaseAgent):
    """分层结构分析Agent
    
    作为资深软件设计师，负责分析系统的逻辑分层结构和职责边界
    """
    
    def __init__(self):
        """
        初始化LayersAgent
        """
        super().__init__(LAYERS_AGENT)
    
    def execute(self, context):
        """
        执行分层结构分析
        
        Args:
            context: Agent执行上下文
            
        Returns:
            Agent执行结果
        """
        import time
        start_time = time.time()
        
        try:
            self.logger.info(f"Starting layers structure analysis")
            
            # 1. 分析包结构
            packages = self._analyze_packages(context.graph)
            
            # 2. 推断分层
            layers = self._infer_layers(packages)
            
            # 3. 分析层级间关系
            layer_relations = self._analyze_layer_relations(context.graph, layers)
            
            # 4. 构建分析提示词
            prompt = self._build_analysis_prompt(
                packages, 
                layers, 
                layer_relations, 
                context
            )
            
            # 5. 调用LLM生成报告
            output_content = self.llm.chat_sync(
                system_prompt=f"你是一个{self.role.role}，擅长分析系统分层架构。请严格遵循段落式写作规范，使用客观第三人称视角。",
                user_prompt=prompt,
                max_tokens=3000,
                temperature=0.3
            )
            
            # 6. 保存输出文件
            output_file = context.get_output_file(self.name)
            self._save_output(output_content, output_file)
            
            # 7. 提取跨模块备注
            cross_notes = self._extract_cross_module_notes(output_content)
            
            # 8. 统计信息
            stats = {
                "packages_analyzed": len(packages),
                "layers_identified": len(set(l['layer'] for l in layers)),
                "relations_analyzed": len(layer_relations),
                "diagrams_generated": output_content.count("```mermaid")
            }
            
            execution_time = time.time() - start_time
            
            return AgentResult(
                agent_name=self.name,
                status="completed",
                output_file=str(output_file),
                summary="分析了系统的分层结构、职责边界和层级关系",
                cross_module_notes=cross_notes,
                stats=stats,
                execution_time=execution_time
            )
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.logger.error(f"Layers analysis failed: {str(e)}")
            
            return AgentResult(
                agent_name=self.name,
                status="failed",
                output_file=None,
                summary="",
                error=str(e),
                execution_time=execution_time
            )
    
    def _get_task_description(self, context: AgentContext) -> str:
        """获取任务描述"""
        return """分析系统的逻辑分层结构，撰写《系统架构设计报告》的"逻辑视点与分层架构"章节。
重点揭示系统如何通过分层来隔离关注点。"""
    
    def _analyze_packages(self, graph) -> Dict[str, Dict]:
        """
        分析包结构
        
        Args:
            graph: 知识图谱
            
        Returns:
            包结构字典
        """
        packages = {}
        
        for entity in graph.entities.values():
            package = self._extract_package(entity.file_path)
            
            if package not in packages:
                packages[package] = {
                    "classes": [],
                    "functions": [],
                    "entity_count": 0
                }
            
            if entity.entity_type.value == 'class':
                packages[package]["classes"].append({
                    "name": entity.name,
                    "file": entity.file_path,
                    "line": entity.start_line
                })
            elif entity.entity_type.value in ['function', 'method']:
                packages[package]["functions"].append({
                    "name": entity.name,
                    "file": entity.file_path,
                    "line": entity.start_line
                })
            
            packages[package]["entity_count"] += 1
        
        return packages
    
    def _extract_package(self, file_path: str) -> str:
        """
        提取包路径
        
        Args:
            file_path: 文件路径
            
        Returns:
            包路径
        """
        parts = Path(file_path).parts
        # 取前3层目录作为包路径
        if len(parts) > 3:
            return "/".join(parts[:3])
        elif len(parts) > 1:
            return "/".join(parts[:-1])
        return "root"
    
    def _infer_layers(self, packages: Dict[str, Dict]) -> List[Dict]:
        """
        推断分层
        
        Args:
            packages: 包结构
            
        Returns:
            分层列表
        """
        layers = []
        
        # 分层关键词映射
        layer_keywords = {
            "controller": {"layer": "表现层", "order": 1},
            "api": {"layer": "接口层", "order": 1},
            "service": {"layer": "业务层", "order": 2},
            "business": {"layer": "业务层", "order": 2},
            "manager": {"layer": "管理层", "order": 2},
            "repository": {"layer": "数据层", "order": 3},
            "dao": {"layer": "数据层", "order": 3},
            "model": {"layer": "领域层", "order": 4},
            "entity": {"layer": "领域层", "order": 4},
            "domain": {"layer": "领域层", "order": 4},
            "util": {"layer": "工具层", "order": 5},
            "common": {"layer": "公共层", "order": 5},
            "config": {"layer": "配置层", "order": 6}
        }
        
        for package, info in packages.items():
            matched = False
            for keyword, layer_info in layer_keywords.items():
                if keyword in package.lower():
                    layers.append({
                        "package": package,
                        "layer": layer_info["layer"],
                        "order": layer_info["order"],
                        "classes": len(info["classes"]),
                        "functions": len(info["functions"]),
                        "entity_count": info["entity_count"]
                    })
                    matched = True
                    break
            
            if not matched and info["entity_count"] > 3:
                # 未匹配但实体较多的包，标记为其他层
                layers.append({
                    "package": package,
                    "layer": "其他",
                    "order": 99,
                    "classes": len(info["classes"]),
                    "functions": len(info["functions"]),
                    "entity_count": info["entity_count"]
                })
        
        # 按order排序
        layers.sort(key=lambda x: x["order"])
        
        return layers
    
    def _analyze_layer_relations(
        self, 
        graph, 
        layers: List[Dict]
    ) -> List[Dict]:
        """
        分析层级间关系
        
        Args:
            graph: 知识图谱
            layers: 分层列表
            
        Returns:
            层级关系列表
        """
        relations = []
        
        # 构建包到层的映射
        package_to_layer = {l["package"]: l["layer"] for l in layers}
        
        # 分析调用关系
        for rel in graph.relationships:
            if rel.relationship_type.value == 'calls':
                source_entity = graph.entities.get(rel.source_id)
                target_entity = graph.entities.get(rel.target_id)
                
                if source_entity and target_entity:
                    source_package = self._extract_package(source_entity.file_path)
                    target_package = self._extract_package(target_entity.file_path)
                    
                    source_layer = package_to_layer.get(source_package)
                    target_layer = package_to_layer.get(target_package)
                    
                    if source_layer and target_layer and source_layer != target_layer:
                        relations.append({
                            "source_layer": source_layer,
                            "target_layer": target_layer,
                            "relation": "calls"
                        })
        
        return relations
    
    def _build_analysis_prompt(
        self, 
        packages: Dict[str, Dict],
        layers: List[Dict],
        layer_relations: List[Dict],
        context: AgentContext
    ) -> str:
        """
        构建分析提示词
        
        Args:
            packages: 包结构
            layers: 分层列表
            layer_relations: 层级关系
            context: Agent上下文
            
        Returns:
            完整的提示词
        """
        # 统计各层信息
        layer_stats = {}
        for layer in layers:
            layer_name = layer["layer"]
            if layer_name not in layer_stats:
                layer_stats[layer_name] = {
                    "packages": [],
                    "total_classes": 0,
                    "total_functions": 0
                }
            layer_stats[layer_name]["packages"].append(layer["package"])
            layer_stats[layer_name]["total_classes"] += layer["classes"]
            layer_stats[layer_name]["total_functions"] += layer["functions"]
        
        return f"""请分析以下系统的分层结构，撰写《系统架构设计报告》的"逻辑视点与分层架构"章节。

## 包结构统计
- 总包数: {len(packages)}
- 识别出的层数: {len(set(l['layer'] for l in layers))}

## 分层识别结果

{json.dumps(layer_stats, ensure_ascii=False, indent=2)}

## 层级关系
{json.dumps(layer_relations[:20], ensure_ascii=False, indent=2)}

请生成以下内容（使用Markdown格式，严格遵循段落式写作规范）：

### 1. 分层架构概述
系统被划分为哪几个逻辑层级？为什么采用这种分层方式？分层的核心设计意图是什么？

### 2. 各层职责
详细描述每一层的核心职责、输入输出和关键组件。每层解决什么问题？有什么约束？

### 3. 层级边界与隔离
各层之间如何解耦？采用了什么隔离策略（接口抽象、DTO转换、依赖注入）？如何防止实现细节泄露？

### 4. 数据流向与约束
数据在各层之间如何流动？是否遵循单向依赖规则？有什么数据转换机制？

### 5. 异常处理流
异常信息如何在分层结构中传递和转化？各层如何处理异常？

### 6. 分层架构图
使用Mermaid语法绘制分层架构图，展示层级关系和依赖方向。

**写作要求：**
- 采用段落式写作，禁止清单罗列
- 使用客观第三人称视角（"该层"、"设计"、"系统"等）
- 善用逻辑连接词（"因此"、"然而"、"值得注意的是"、"进而"等）
- 关注层级间的契约和隔离的艺术，不要列举具体的文件名列表
- 描述"做什么"和"为什么"，而非"怎么写的"

请用中文回答，内容要专业、详细、结构化。"""
