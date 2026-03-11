"""
文档生成Agent - 负责多维度文档生成与知识图谱构建

功能：
- 技术文档生成
- 业务文档生成
- 架构文档生成
- API文档生成
- 知识图谱可视化
"""

import os
import json
from datetime import datetime
from typing import List, Dict, Any, Optional
from pathlib import Path
from .base_agent import BaseAgent


class DocumentationAgent(BaseAgent):
    """文档生成Agent"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__("documentation", config)
        self.output_dir = config.get("output_dir", "docs")
        self.template_dir = config.get("template_dir", "templates")
        
        # 确保输出目录存在
        os.makedirs(self.output_dir, exist_ok=True)
    
    async def _execute_impl(self, input_data: Any) -> Dict[str, Any]:
        """执行文档生成"""
        if isinstance(input_data, dict):
            analysis_results = input_data.get("analysis_results", {})
            project_info = input_data.get("project_info", {})
        else:
            raise ValueError("输入数据应包含分析结果和项目信息")
        
        self.logger.info("开始文档生成")
        
        # 生成各类文档
        generated_docs = await self._generate_all_documents(analysis_results, project_info)
        
        # 构建知识图谱
        knowledge_graph = await self._build_knowledge_graph(analysis_results)
        
        # 生成文档索引
        await self._generate_document_index(generated_docs, project_info)
        
        self.logger.info(f"文档生成完成，生成 {len(generated_docs)} 类文档")
        
        return {
            "generated_docs": generated_docs,
            "knowledge_graph": knowledge_graph,
            "output_dir": self.output_dir
        }
    
    async def _generate_all_documents(self, analysis_results: Dict[str, Any], project_info: Dict[str, Any]) -> Dict[str, str]:
        """生成所有类型文档"""
        generated_docs = {}
        
        # 技术文档
        generated_docs["technical_overview"] = await self._generate_technical_overview(analysis_results, project_info)
        generated_docs["architecture_design"] = await self._generate_architecture_document(analysis_results)
        generated_docs["api_reference"] = await self._generate_api_document(analysis_results)
        
        # 业务文档
        generated_docs["business_overview"] = await self._generate_business_overview(analysis_results)
        generated_docs["business_flows"] = await self._generate_business_flows_document(analysis_results)
        
        # 开发文档
        generated_docs["development_guide"] = await self._generate_development_guide(analysis_results)
        generated_docs["deployment_guide"] = await self._generate_deployment_guide(analysis_results)
        
        # 知识图谱
        generated_docs["knowledge_graph"] = await self._generate_knowledge_graph_document(analysis_results)
        
        # 保存文档到文件
        await self._save_documents_to_files(generated_docs)
        
        return generated_docs
    
    async def _generate_technical_overview(self, analysis_results: Dict[str, Any], project_info: Dict[str, Any]) -> str:
        """生成技术概览文档"""
        code_metadata_list = analysis_results.get("code_metadata", [])
        dependency_graph = analysis_results.get("dependency_graph", {})
        
        # 统计信息
        total_files = len(code_metadata_list)
        total_functions = sum(len(metadata.functions) for metadata in code_metadata_list)
        total_classes = sum(len(metadata.classes) for metadata in code_metadata_list)
        total_dependencies = len(dependency_graph.get("dependencies", []))
        
        content = f"""# 技术概览文档

## 项目信息
- **项目名称**: {project_info.get('name', 'Unknown')}
- **分析时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
- **分析版本**: {project_info.get('version', '1.0.0')}

## 代码统计
- **总文件数**: {total_files}
- **总函数数**: {total_functions}
- **总类数**: {total_classes}
- **依赖关系数**: {total_dependencies}

## 技术栈分析
{self._generate_tech_stack_analysis(code_metadata_list)}

## 架构概述
{self._generate_architecture_overview(dependency_graph)}

## 质量评估
{self._generate_quality_assessment(analysis_results)}
"""
        
        return content
    
    def _generate_tech_stack_analysis(self, code_metadata_list: List[Any]) -> str:
        """生成技术栈分析"""
        # 统计文件类型
        file_extensions = {}
        for metadata in code_metadata_list:
            ext = Path(metadata.file_path).suffix.lower()
            file_extensions[ext] = file_extensions.get(ext, 0) + 1
        
        tech_stack = ""
        for ext, count in sorted(file_extensions.items(), key=lambda x: x[1], reverse=True):
            if ext:
                language = self._get_language_name(ext)
                tech_stack += f"- {language} ({ext}): {count} 个文件\n"
        
        return tech_stack if tech_stack else "未识别到主要技术栈"
    
    def _get_language_name(self, extension: str) -> str:
        """获取编程语言名称"""
        language_map = {
            ".py": "Python",
            ".js": "JavaScript", 
            ".ts": "TypeScript",
            ".java": "Java",
            ".cpp": "C++",
            ".c": "C",
            ".go": "Go",
            ".rs": "Rust",
            ".php": "PHP",
            ".rb": "Ruby",
            ".cs": "C#",
            ".swift": "Swift",
            ".kt": "Kotlin"
        }
        return language_map.get(extension, extension[1:].upper())
    
    def _generate_architecture_overview(self, dependency_graph: Dict[str, Any]) -> str:
        """生成架构概述"""
        layers = dependency_graph.get("architecture_layers", {})
        
        overview = "## 架构层次\n"
        for layer_name, modules in layers.items():
            overview += f"- **{layer_name}层**: {len(modules)} 个模块\n"
        
        # 设计模式检测
        patterns = dependency_graph.get("design_patterns", [])
        if patterns:
            overview += "\n## 检测到的设计模式\n"
            for pattern in patterns[:5]:  # 显示前5个
                overview += f"- {pattern.get('pattern', 'Unknown')} (置信度: {pattern.get('confidence', 0)})\n"
        
        return overview
    
    def _generate_quality_assessment(self, analysis_results: Dict[str, Any]) -> str:
        """生成质量评估"""
        dependency_graph = analysis_results.get("dependency_graph", {})
        risks = dependency_graph.get("risk_assessments", [])
        
        assessment = "## 架构质量评估\n"
        
        if not risks:
            assessment += "✅ 架构质量良好，未发现明显风险\n"
        else:
            assessment += "⚠️ 发现以下架构风险：\n"
            for risk in risks:
                assessment += f"- **{risk.get('risk_type', 'Unknown')}** ({risk.get('severity', 'unknown')}): {risk.get('description', '')}\n"
        
        return assessment
    
    async def _generate_architecture_document(self, analysis_results: Dict[str, Any]) -> str:
        """生成架构设计文档"""
        dependency_graph = analysis_results.get("dependency_graph", {})
        
        content = f"""# 架构设计文档

## 系统架构概述
{self._generate_system_architecture_overview(dependency_graph)}

## 模块依赖关系
{self._generate_module_dependencies(dependency_graph)}

## 架构层次设计
{self._generate_architecture_layers_design(dependency_graph)}

## 设计模式应用
{self._generate_design_patterns_application(dependency_graph)}

## 架构演进建议
{self._generate_architecture_evolution_recommendations(dependency_graph)}
"""
        
        return content
    
    def _generate_system_architecture_overview(self, dependency_graph: Dict[str, Any]) -> str:
        """生成系统架构概述"""
        modules = dependency_graph.get("modules", {})
        dependencies = dependency_graph.get("dependencies", [])
        
        overview = f"""
系统包含 **{len(modules)}** 个核心模块，模块间存在 **{len(dependencies)}** 个依赖关系。

主要架构特征：
- **模块化程度**: {self._assess_modularity(dependencies, modules)}
- **耦合度**: {self._assess_coupling(dependencies, modules)}
- **内聚性**: {self._assess_cohesion(modules)}
"""
        
        return overview
    
    def _assess_modularity(self, dependencies: List[Any], modules: Dict[str, Any]) -> str:
        """评估模块化程度"""
        if len(modules) == 0:
            return "未知"
        
        avg_deps_per_module = len(dependencies) / len(modules)
        
        if avg_deps_per_module < 2:
            return "高"
        elif avg_deps_per_module < 5:
            return "中"
        else:
            return "低"
    
    def _assess_coupling(self, dependencies: List[Any], modules: Dict[str, Any]) -> str:
        """评估耦合度"""
        if len(modules) == 0:
            return "未知"
        
        # 计算模块间依赖密度
        max_possible_deps = len(modules) * (len(modules) - 1)
        if max_possible_deps == 0:
            return "无依赖"
        
        coupling_density = len(dependencies) / max_possible_deps
        
        if coupling_density < 0.1:
            return "低"
        elif coupling_density < 0.3:
            return "中"
        else:
            return "高"
    
    def _assess_cohesion(self, modules: Dict[str, Any]) -> str:
        """评估内聚性"""
        # 简化的内聚性评估
        return "需要进一步分析"
    
    def _generate_module_dependencies(self, dependency_graph: Dict[str, Any]) -> str:
        """生成模块依赖关系"""
        dependencies = dependency_graph.get("dependencies", [])
        
        if not dependencies:
            return "未发现模块依赖关系"
        
        content = """
| 源模块 | 目标模块 | 依赖类型 | 强度 |
|--------|---------|----------|------|
"""
        
        for dep in dependencies[:20]:  # 显示前20个依赖关系
            content += f"| {dep.get('source', '')} | {dep.get('target', '')} | {dep.get('dependency_type', '')} | {dep.get('strength', 0)} |\n"
        
        if len(dependencies) > 20:
            content += f"\n... 还有 {len(dependencies) - 20} 个依赖关系未显示"
        
        return content
    
    def _generate_architecture_layers_design(self, dependency_graph: Dict[str, Any]) -> str:
        """生成架构层次设计"""
        layers = dependency_graph.get("architecture_layers", {})
        
        content = """
| 层次 | 模块数量 | 主要职责 |
|------|---------|----------|
"""
        
        layer_descriptions = {
            "presentation": "用户界面、API接口、请求处理",
            "business": "业务逻辑、业务规则、业务流程",
            "data": "数据模型、数据访问、数据持久化", 
            "infrastructure": "工具函数、配置管理、通用组件"
        }
        
        for layer_name, modules in layers.items():
            description = layer_descriptions.get(layer_name, "未知")
            content += f"| {layer_name} | {len(modules)} | {description} |\n"
        
        return content
    
    def _generate_design_patterns_application(self, dependency_graph: Dict[str, Any]) -> str:
        """生成设计模式应用"""
        patterns = dependency_graph.get("design_patterns", [])
        
        if not patterns:
            return "未检测到明确的设计模式应用"
        
        content = """
| 设计模式 | 应用位置 | 置信度 |
|----------|---------|--------|
"""
        
        for pattern in patterns:
            content += f"| {pattern.get('pattern', '')} | {pattern.get('file_path', '')} | {pattern.get('confidence', 0)} |\n"
        
        return content
    
    def _generate_architecture_evolution_recommendations(self, dependency_graph: Dict[str, Any]) -> str:
        """生成架构演进建议"""
        risks = dependency_graph.get("risk_assessments", [])
        
        if not risks:
            return "当前架构设计良好，建议保持现有架构风格"
        
        content = "## 架构演进建议\n"
        
        for risk in risks:
            content += f"### {risk.get('risk_type', '风险')}处理建议\n"
            content += f"**问题**: {risk.get('description', '')}\n"
            content += f"**建议**: {risk.get('recommendation', '')}\n"
            content += f"**影响模块**: {', '.join(risk.get('affected_modules', []))}\n\n"
        
        return content
    
    async def _generate_api_document(self, analysis_results: Dict[str, Any]) -> str:
        """生成API文档"""
        # 简化的API文档生成
        content = """# API参考文档

## 概述
本文档描述了系统中提供的API接口。

## API端点列表

（API端点信息需要从代码中进一步提取）

## 使用示例

（使用示例需要根据具体API实现）
"""
        
        return content
    
    async def _generate_business_overview(self, analysis_results: Dict[str, Any]) -> str:
        """生成业务概览文档"""
        business_flows = analysis_results.get("business_flows", [])
        
        content = """# 业务概览文档

## 业务领域分析

（业务领域分析需要从业务逻辑中提取）

## 核心业务流程

（核心业务流程描述）
"""
        
        return content
    
    async def _generate_business_flows_document(self, analysis_results: Dict[str, Any]) -> str:
        """生成业务流文档"""
        # 简化的业务流文档生成
        content = """# 业务流程文档

## 业务流程概述

（业务流程概述信息）

## 详细业务流程

（详细业务流程描述）
"""
        
        return content
    
    async def _generate_development_guide(self, analysis_results: Dict[str, Any]) -> str:
        """生成开发指南"""
        content = """# 开发指南

## 开发环境搭建

（开发环境搭建指南）

## 代码规范

（代码规范和最佳实践）
"""
        
        return content
    
    async def _generate_deployment_guide(self, analysis_results: Dict[str, Any]) -> str:
        """生成部署指南"""
        content = """# 部署指南

## 部署环境要求

（部署环境要求说明）

## 部署步骤

（详细的部署步骤说明）
"""
        
        return content
    
    async def _generate_knowledge_graph_document(self, analysis_results: Dict[str, Any]) -> str:
        """生成知识图谱文档"""
        content = """# 知识图谱文档

## 知识图谱概述

（知识图谱结构和内容概述）

## 实体关系图

（实体关系可视化）
"""
        
        return content
    
    async def _build_knowledge_graph(self, analysis_results: Dict[str, Any]) -> Dict[str, Any]:
        """构建知识图谱"""
        # 简化的知识图谱构建
        knowledge_graph = {
            "nodes": [],
            "edges": [],
            "metadata": {
                "generated_at": datetime.now().isoformat(),
                "node_count": 0,
                "edge_count": 0
            }
        }
        
        return knowledge_graph
    
    async def _generate_document_index(self, generated_docs: Dict[str, str], project_info: Dict[str, Any]):
        """生成文档索引"""
        index_content = """# 文档索引

## 项目信息
- **项目名称**: {name}
- **文档版本**: {version}
- **生成时间**: {time}

## 文档列表

| 文档类型 | 文档名称 | 描述 | 文件路径 |
|----------|---------|------|----------|
""".format(
            name=project_info.get('name', 'Unknown'),
            version=project_info.get('version', '1.0.0'),
            time=datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        )
        
        doc_descriptions = {
            "technical_overview": "技术概览文档",
            "architecture_design": "架构设计文档", 
            "api_reference": "API参考文档",
            "business_overview": "业务概览文档",
            "business_flows": "业务流程文档",
            "development_guide": "开发指南",
            "deployment_guide": "部署指南",
            "knowledge_graph": "知识图谱文档"
        }
        
        for doc_type, content in generated_docs.items():
            description = doc_descriptions.get(doc_type, "未知文档")
            file_path = f"{self.output_dir}/{doc_type}.md"
            index_content += f"| {doc_type} | {description} | {description} | {file_path} |\n"
        
        # 保存索引文件
        index_file = os.path.join(self.output_dir, "index.md")
        with open(index_file, 'w', encoding='utf-8') as f:
            f.write(index_content)
    
    async def _save_documents_to_files(self, generated_docs: Dict[str, str]):
        """保存文档到文件"""
        for doc_type, content in generated_docs.items():
            file_path = os.path.join(self.output_dir, f"{doc_type}.md")
            
            try:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                self.logger.info(f"保存文档: {file_path}")
            except Exception as e:
                self.logger.error(f"保存文档失败 {file_path}: {e}")