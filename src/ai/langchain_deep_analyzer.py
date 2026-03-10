"""
基于 LangChain 的深度知识分析器

使用 LangChain 重构的深度知识分析器，生成详细设计文档
"""

import asyncio
import json
import logging
from typing import Dict, List, Any, Optional
from pathlib import Path
from datetime import datetime
from collections import defaultdict

from .langchain_llm_client import LangChainLLMClient
from .langchain_prompts import (
    KnowledgePrompts, format_entities_for_prompt
)

logger = logging.getLogger(__name__)


class LangChainDeepAnalyzer:
    """基于 LangChain 的深度知识分析器"""
    
    def __init__(self, config: Dict[str, Any], output_dir: str = "output/doc"):
        """
        初始化分析器
        
        Args:
            config: AI 配置
            output_dir: 输出目录
        """
        self.config = config
        self.output_dir = Path(output_dir)
        self.llm_client = LangChainLLMClient(config.get('ai', {}))
        
        # 确保目录存在
        (self.output_dir / "design").mkdir(parents=True, exist_ok=True)
        (self.output_dir / "business").mkdir(parents=True, exist_ok=True)
        (self.output_dir / "architecture").mkdir(parents=True, exist_ok=True)
    
    def is_available(self) -> bool:
        """检查 LLM 是否可用"""
        return self.llm_client.is_available()
    
    def analyze_all(self, graph, source_dir: str = None) -> Dict[str, Any]:
        """
        执行完整的深度分析（同步版本）
        
        Args:
            graph: 知识图谱
            source_dir: 源代码目录
            
        Returns:
            分析结果字典
        """
        return asyncio.run(self.analyze_all_async(graph, source_dir))
    
    async def analyze_all_async(self, graph, source_dir: str = None) -> Dict[str, Any]:
        """
        执行完整的深度分析（异步版本）
        
        Args:
            graph: 知识图谱
            source_dir: 源代码目录
            
        Returns:
            分析结果字典
        """
        results = {}
        
        if not self.is_available():
            logger.warning("LLM 不可用，跳过深度分析")
            return {"error": "LLM 不可用，请配置 API Key"}
        
        print("\n" + "="*60)
        print("开始 LLM 深度知识分析 (LangChain 版)...")
        print("="*60)
        
        # 1. 项目概述分析
        print("\n[1/6] 生成项目概述...")
        results["project_overview"] = await self._analyze_project_overview(graph, source_dir)
        
        # 2. 业务流程分析
        print("[2/6] 分析业务流程...")
        results["business_flows"] = await self._analyze_business_flows(graph)
        
        # 3. 架构设计分析
        print("[3/6] 分析架构设计...")
        results["architecture"] = await self._analyze_architecture(graph)
        
        # 4. 功能模块分析
        print("[4/6] 分析功能模块...")
        results["features"] = await self._analyze_features(graph)
        
        # 5. 核心实体分析
        print("[5/6] 分析核心实体...")
        results["entities"] = await self._analyze_core_entities(graph)
        
        # 6. API 接口分析
        print("[6/6] 分析 API 接口...")
        results["api"] = await self._analyze_api_interfaces(graph)
        
        print("\n深度分析完成！")
        return results
    
    async def _analyze_project_overview(self, graph, source_dir: str = None) -> Dict[str, Any]:
        """分析项目概述"""
        # 收集项目信息
        entities_summary = self._get_entities_summary(graph)
        main_classes = self._get_main_classes(graph)
        main_functions = self._get_main_functions(graph)
        
        # 使用 LangChain Prompt
        chain = KnowledgePrompts.PROJECT_OVERVIEW | self.llm_client.llm
        
        result = await chain.ainvoke({
            "entity_count": len(graph.entities),
            "relationship_count": len(graph.relationships),
            "file_count": len(set(e.file_path for e in graph.entities.values())),
            "main_class_count": len(main_classes),
            "main_function_count": len(main_functions),
            "main_classes": self._format_entities_for_prompt(main_classes[:15]),
            "main_functions": self._format_entities_for_prompt(main_functions[:20]),
            "entities_summary": json.dumps(entities_summary, ensure_ascii=False, indent=2)
        })
        
        # 保存文档
        output_path = self.output_dir / "design" / "00_project_overview.md"
        output_path.write_text(result.content, encoding='utf-8')
        
        return {
            "content": result.content,
            "file_path": str(output_path)
        }
    
    async def _analyze_business_flows(self, graph) -> Dict[str, Any]:
        """分析业务流程"""
        # 收集核心业务函数和类
        core_functions = self._get_core_functions(graph)
        business_classes = self._get_business_classes(graph)
        
        # 构建项目信息
        project_info = self._build_project_info(graph)
        
        # 使用 LangChain Prompt
        chain = KnowledgePrompts.BUSINESS_FLOW | self.llm_client.llm
        
        result = await chain.ainvoke({
            "project_info": project_info,
            "core_functions": self._format_entities_for_prompt(core_functions[:15]),
            "business_classes": self._format_entities_for_prompt(business_classes[:10])
        })
        
        # 保存文档
        output_path = self.output_dir / "business" / "01_business_flows.md"
        output_path.write_text(result.content, encoding='utf-8')
        
        return {
            "content": result.content,
            "file_path": str(output_path)
        }
    
    async def _analyze_architecture(self, graph) -> Dict[str, Any]:
        """分析架构设计"""
        # 收集核心组件和依赖关系
        core_components = self._get_core_components(graph)
        dependencies = self._build_dependencies_summary(graph)
        
        # 构建项目信息
        project_info = self._build_project_info(graph)
        
        # 使用 LangChain Prompt
        chain = KnowledgePrompts.ARCHITECTURE_DESIGN | self.llm_client.llm
        
        result = await chain.ainvoke({
            "project_info": project_info,
            "core_components": self._format_components_for_prompt(core_components[:15]),
            "dependencies": dependencies
        })
        
        # 保存文档
        output_path = self.output_dir / "architecture" / "02_architecture_design.md"
        output_path.write_text(result.content, encoding='utf-8')
        
        return {
            "content": result.content,
            "file_path": str(output_path)
        }
    
    async def _analyze_features(self, graph) -> Dict[str, Any]:
        """分析功能模块"""
        # 按模块分组实体
        modules = self._group_by_module(graph)
        
        # 构建模块信息
        module_info = self._build_module_info(modules)
        module_dependencies = self._build_module_dependencies(graph)
        
        # 使用 LangChain Prompt
        chain = KnowledgePrompts.FUNCTIONAL_MODULE | self.llm_client.llm
        
        result = await chain.ainvoke({
            "module_info": module_info,
            "module_dependencies": module_dependencies
        })
        
        # 保存文档
        output_path = self.output_dir / "design" / "03_functional_modules.md"
        output_path.write_text(result.content, encoding='utf-8')
        
        return {
            "content": result.content,
            "file_path": str(output_path)
        }
    
    async def _analyze_core_entities(self, graph) -> Dict[str, Any]:
        """分析核心实体"""
        # 获取核心实体
        core_entities = self._get_core_entities(graph)
        
        # 构建实体关系
        entity_relationships = self._build_entity_relationships(core_entities, graph)
        
        # 使用 LangChain Prompt
        chain = KnowledgePrompts.CORE_ENTITY | self.llm_client.llm
        
        result = await chain.ainvoke({
            "core_entities": self._format_entities_for_prompt(core_entities[:15]),
            "entity_relationships": entity_relationships
        })
        
        # 保存文档
        output_path = self.output_dir / "design" / "04_core_entities.md"
        output_path.write_text(result.content, encoding='utf-8')
        
        return {
            "content": result.content,
            "file_path": str(output_path)
        }
    
    async def _analyze_api_interfaces(self, graph) -> Dict[str, Any]:
        """分析 API 接口"""
        # 获取 API 相关实体
        api_interfaces = self._get_api_interfaces(graph)
        routes = self._get_routes(graph)
        
        # 使用 LangChain Prompt
        chain = KnowledgePrompts.API_INTERFACE | self.llm_client.llm
        
        result = await chain.ainvoke({
            "api_interfaces": self._format_entities_for_prompt(api_interfaces[:20]),
            "routes": routes
        })
        
        # 保存文档
        output_path = self.output_dir / "design" / "05_api_interfaces.md"
        output_path.write_text(result.content, encoding='utf-8')
        
        return {
            "content": result.content,
            "file_path": str(output_path)
        }
    
    # ========== 辅助方法 ==========
    
    def _get_entities_summary(self, graph) -> Dict[str, int]:
        """获取实体类型统计"""
        summary = defaultdict(int)
        for entity in graph.entities.values():
            summary[entity.entity_type] += 1
        return dict(summary)
    
    def _get_main_classes(self, graph) -> List[Dict[str, Any]]:
        """获取主要类（按代码行数排序）"""
        classes = [
            {
                "name": entity.name,
                "type": entity.entity_type,
                "file": entity.file_path,
                "lines": entity.lines_of_code
            }
            for entity in graph.entities.values()
            if entity.entity_type == "class"
        ]
        return sorted(classes, key=lambda x: x["lines"], reverse=True)
    
    def _get_main_functions(self, graph) -> List[Dict[str, Any]]:
        """获取主要函数（按代码行数排序）"""
        functions = [
            {
                "name": entity.name,
                "type": entity.entity_type,
                "file": entity.file_path,
                "lines": entity.lines_of_code
            }
            for entity in graph.entities.values()
            if entity.entity_type in ["function", "method"]
        ]
        return sorted(functions, key=lambda x: x["lines"], reverse=True)
    
    def _get_core_functions(self, graph) -> List[Dict[str, Any]]:
        """获取核心业务函数"""
        return self._get_main_functions(graph)[:30]
    
    def _get_business_classes(self, graph) -> List[Dict[str, Any]]:
        """获取业务类"""
        return self._get_main_classes(graph)[:20]
    
    def _get_core_components(self, graph) -> List[Dict[str, Any]]:
        """获取核心组件"""
        return self._get_main_classes(graph)[:20]
    
    def _get_core_entities(self, graph) -> List[Dict[str, Any]]:
        """获取核心实体"""
        classes = self._get_main_classes(graph)
        functions = self._get_main_functions(graph)
        return classes[:15] + functions[:15]
    
    def _get_api_interfaces(self, graph) -> List[Dict[str, Any]]:
        """获取 API 接口"""
        api_entities = [
            {
                "name": entity.name,
                "type": entity.entity_type,
                "file": entity.file_path,
                "description": entity.docstring or ""
            }
            for entity in graph.entities.values()
            if "api" in entity.name.lower() or "route" in entity.name.lower() or
               "endpoint" in entity.name.lower() or "handler" in entity.name.lower()
        ]
        return api_entities
    
    def _get_routes(self, graph) -> str:
        """获取路由定义"""
        # 这里可以根据实际项目结构解析路由
        # 简化版本：返回提示信息
        return "请在实际项目中实现路由解析逻辑"
    
    def _build_project_info(self, graph) -> str:
        """构建项目信息"""
        return f"""
实体总数: {len(graph.entities)}
关系总数: {len(graph.relationships)}
文件数量: {len(set(e.file_path for e in graph.entities.values()))}
"""
    
    def _build_dependencies_summary(self, graph) -> str:
        """构建依赖关系摘要"""
        # 简化版本
        return f"共有 {len(graph.relationships)} 个依赖关系"
    
    def _group_by_module(self, graph) -> Dict[str, List[Any]]:
        """按模块分组实体"""
        modules = defaultdict(list)
        for entity in graph.entities.values():
            module = self._get_module_name(entity.file_path)
            modules[module].append(entity)
        return dict(modules)
    
    def _build_module_info(self, modules: Dict[str, List[Any]]) -> str:
        """构建模块信息"""
        info = []
        for module, entities in modules.items():
            info.append(f"\n### {module}")
            info.append(f"- 实体数: {len(entities)}")
            info.append(f"- 类型分布: {self._count_entity_types(entities)}")
        return "\n".join(info)
    
    def _build_module_dependencies(self, graph) -> str:
        """构建模块依赖关系"""
        # 简化版本
        return f"请根据实际的模块依赖关系补充此部分"
    
    def _build_entity_relationships(self, entities: List[Dict], graph) -> str:
        """构建实体关系"""
        # 简化版本
        return f"请根据实际的实体关系补充此部分"
    
    def _format_entities_for_prompt(self, entities: List[Dict[str, Any]], max_count: int = 20) -> str:
        """格式化实体列表，用于提示词"""
        if not entities:
            return "无"
        
        formatted = []
        for i, entity in enumerate(entities[:max_count]):
            formatted.append(f"- {entity['name']} ({entity['type']}) - {entity.get('file', 'unknown')}")
        
        if len(entities) > max_count:
            formatted.append(f"\n... 还有 {len(entities) - max_count} 个实体")
        
        return "\n".join(formatted)
    
    def _format_components_for_prompt(self, components: List[Dict[str, Any]], max_count: int = 15) -> str:
        """格式化组件列表，用于提示词"""
        return self._format_entities_for_prompt(components, max_count)
    
    def _get_module_name(self, file_path: str) -> str:
        """从文件路径获取模块名"""
        parts = file_path.split("/")
        if "src" in parts:
            idx = parts.index("src")
            return "/".join(parts[idx+1:-1])
        return "/".join(parts[:-1])
    
    def _count_entity_types(self, entities: List[Any]) -> str:
        """统计实体类型"""
        types = defaultdict(int)
        for entity in entities:
            types[entity.entity_type] += 1
        return ", ".join([f"{k}:{v}" for k, v in types.items()])


__all__ = [
    "LangChainDeepAnalyzer",
]
