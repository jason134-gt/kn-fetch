"""
Overview Agent - 总体架构分析

负责分析项目的整体架构和技术决策
"""
from typing import Dict, Any, List
import json

from ...base.agent import BaseAgent, AgentResult
from ...config.agent_configs import OVERVIEW_AGENT


class OverviewAgent(BaseAgent):
    """总体架构分析Agent
    
    作为首席系统架构师，负责分析项目的整体架构、技术决策和设计理念
    """
    
    def __init__(self):
        """
        初始化OverviewAgent
        """
        super().__init__(OVERVIEW_AGENT)
    
    def execute(self, context):
        """
        执行架构概览分析
        
        Args:
            context: Agent执行上下文
            
        Returns:
            Agent执行结果
        """
        import time
        start_time = time.time()
        
        try:
            self.logger.info(f"Starting overview analysis for project")
            
            # 1. 收集项目信息
            project_info = self._collect_project_info(context.graph)
            
            # 2. 获取关键类代码示例
            code_samples = self._get_key_code_samples(context.graph)
            
            # 3. 构建分析提示词
            prompt = self._build_analysis_prompt(project_info, code_samples, context)
            
            # 4. 调用LLM生成报告
            output_content = self.llm.chat_sync(
                system_prompt=f"你是一个{self.role.role}，擅长分析代码结构和设计理念。请严格遵循段落式写作规范，使用客观第三人称视角。",
                user_prompt=prompt,
                max_tokens=3000,
                temperature=0.3
            )
            
            # 5. 保存输出文件
            output_file = context.get_output_file(self.name)
            self._save_output(output_content, output_file)
            
            # 6. 提取跨模块备注
            cross_notes = self._extract_cross_module_notes(output_content)
            
            # 7. 统计信息
            stats = {
                "entities_analyzed": project_info["total_entities"],
                "files_analyzed": project_info["file_count"],
                "key_classes": len(project_info["main_classes"]),
                "diagrams_generated": output_content.count("```mermaid")
            }
            
            execution_time = time.time() - start_time
            
            return AgentResult(
                success=True,
                data=project_info,
                output="概述分析报告已生成",
                metrics=stats,
                cross_notes=cross_notes
            )
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.logger.error(f"Overview analysis failed: {str(e)}")
            
            return AgentResult(
                success=False,
                error=str(e),
                metrics={"execution_time": execution_time}
            )
    
    def _get_task_description(self, context: AgentContext) -> str:
        """获取任务描述"""
        return """基于代码库的全貌，撰写《系统架构设计报告》的"总体架构"章节。
透过代码表象，洞察系统的核心价值主张和顶层技术决策。"""
    
    def _collect_project_info(self, graph) -> Dict[str, Any]:
        """
        收集项目信息
        
        Args:
            graph: 知识图谱
            
        Returns:
            项目信息字典
        """
        # 统计实体类型分布
        entities_summary = {}
        for entity in graph.entities.values():
            entity_type = str(entity.entity_type.value)
            entities_summary[entity_type] = entities_summary.get(entity_type, 0) + 1
        
        # 获取主要类
        main_classes = [
            e for e in graph.entities.values() 
            if e.entity_type.value == 'class'
        ][:15]
        
        # 获取主要函数
        main_functions = [
            e for e in graph.entities.values() 
            if e.entity_type.value in ['function', 'method']
        ][:20]
        
        # 统计文件数
        file_count = len(set(e.file_path for e in graph.entities.values()))
        
        return {
            "total_entities": len(graph.entities),
            "total_relationships": len(graph.relationships),
            "file_count": file_count,
            "entities_summary": entities_summary,
            "main_classes": main_classes,
            "main_functions": main_functions
        }
    
    def _get_key_code_samples(self, graph) -> str:
        """
        获取关键代码示例
        
        Args:
            graph: 知识图谱
            
        Returns:
            代码示例字符串
        """
        # 获取前3个主要类
        classes = [
            e for e in graph.entities.values() 
            if e.entity_type.value == 'class'
        ][:3]
        
        samples = []
        for cls in classes:
            if hasattr(cls, 'content') and cls.content:
                sample = f"### {cls.name}\n```java\n{cls.content[:1000]}\n```\n"
                samples.append(sample)
        
        return "\n".join(samples)
    
    def _build_analysis_prompt(
        self, 
        project_info: Dict[str, Any], 
        code_samples: str,
        context: AgentContext
    ) -> str:
        """
        构建分析提示词
        
        Args:
            project_info: 项目信息
            code_samples: 代码示例
            context: Agent上下文
            
        Returns:
            完整的提示词
        """
        return f"""请分析以下代码项目，生成一份详细的项目概述文档。

## 项目统计
- 实体总数: {project_info['total_entities']}
- 关系总数: {project_info['total_relationships']}
- 文件数量: {project_info['file_count']}

## 实体类型分布
{json.dumps(project_info['entities_summary'], ensure_ascii=False, indent=2)}

## 主要类（{len(project_info['main_classes'])}个）
{self._format_entities(project_info['main_classes'])}

## 主要函数（{len(project_info['main_functions'])}个）
{self._format_entities(project_info['main_functions'][:15])}

## 关键类代码示例
{code_samples}

请生成以下内容（使用Markdown格式，严格遵循段落式写作规范）：

### 1. 项目定位
这个项目是做什么的？解决什么核心业务问题？在技术生态中处于什么位置？

### 2. 技术栈
使用了哪些核心技术、框架和工具？选型依据是什么？

### 3. 核心功能
主要功能模块有哪些？每个模块的核心职责是什么？模块间如何协作？

### 4. 设计理念
从代码结构推断设计思路和架构模式。采用了什么架构风格？核心设计原则是什么？

### 5. 适用场景
适合什么场景使用？有什么优势和限制？

### 6. 架构图
使用Mermaid语法绘制至少1个架构图，展示系统的整体结构。

**写作要求：**
- 采用段落式写作，禁止清单罗列
- 使用客观第三人称视角（"系统"、"该项目"、"设计"等）
- 善用逻辑连接词（"因此"、"然而"、"值得注意的是"、"进而"等）
- 描述"做什么"和"为什么"，非"怎么写的"
- 每个章节要有2-3个段落，层层递进

请用中文回答，内容要专业、详细、结构化。"""
    
    def _format_entities(self, entities: List[Any]) -> str:
        """
        格式化实体列表
        
        Args:
            entities: 实体列表
            
        Returns:
            格式化的字符串
        """
        lines = []
        for i, entity in enumerate(entities, 1):
            lines.append(f"{i}. **{entity.name}** - `{entity.file_path}:{entity.start_line}`")
        return "\n".join(lines)
