"""
集成适配器 - 将ArchAI CodeExplainer和GitUML与项目核心功能打通
提供统一的接口，增强知识提取能力
"""
import os
import json
from typing import List, Dict, Any, Optional, Union
from pathlib import Path
from datetime import datetime
from dataclasses import asdict

# 导入项目模块
import sys
sys.path.insert(0, str(Path(__file__).parent.parent.parent))
from src.gitnexus.models import (
    KnowledgeGraph, CodeEntity, EntityType, Relationship, RelationshipType,
    ArchitectureInfo, ModuleInfo, FeatureInfo, CallChain
)
from src.core.architecture_analyzer import ArchitectureAnalyzer
from src.core.uml_generator import UMLGenerator
from src.integrations.archai_code_explainer import ArchAICodeExplainer, ArchitecturePattern, CodeSmellType
from src.integrations.gituml_integration import GitUMLIntegration, GitUMLDiagram, DiagramType


class EnhancedKnowledgeExtractor:
    """增强版知识提取器 - 集成ArchAI和GitUML"""
    
    def __init__(
        self,
        graph: KnowledgeGraph,
        repo_path: str = ".",
        ai_client=None,
        config: Optional[Dict] = None
    ):
        self.graph = graph
        self.repo_path = repo_path
        self.config = config or {}
        
        # 初始化架构分析器
        self.architecture_analyzer = ArchitectureAnalyzer(graph)
        
        # 执行架构分析
        if not graph.architecture:
            graph.architecture = self.architecture_analyzer.analyze_full()
        
        # 初始化集成工具
        self.code_explainer = ArchAICodeExplainer(graph, ai_client)
        self.gituml = GitUMLIntegration(graph, repo_path, graph.architecture)
    
    def extract_enhanced_knowledge(self) -> Dict[str, Any]:
        """提取增强版知识"""
        result = {
            "metadata": {
                "extracted_at": datetime.now().isoformat(),
                "project_name": self.graph.project_name,
                "tools_used": ["ArchAI CodeExplainer", "GitUML"]
            },
            "architecture": {},
            "code_explanations": {},
            "refactoring_analysis": {},
            "uml_diagrams": {}
        }
        
        # 1. 架构分析
        print("执行架构分析...")
        arch_result = self.code_explainer.analyze_architecture()
        result["architecture"] = {
            "primary_pattern": arch_result.primary_pattern.value,
            "secondary_patterns": [p.value for p in arch_result.secondary_patterns],
            "layers": arch_result.layers,
            "circular_dependencies": arch_result.circular_dependencies,
            "design_issues": arch_result.design_issues,
            "improvement_suggestions": arch_result.improvement_suggestions
        }
        
        # 2. 代码解释 - 对关键实体进行深度分析
        print("生成代码解释...")
        key_entities = self._identify_key_entities()
        for entity_id in key_entities[:30]:  # 限制数量
            try:
                explanation = self.code_explainer.explain_entity(entity_id)
                result["code_explanations"][entity_id] = {
                    "entity_name": explanation.entity_name,
                    "purpose": explanation.purpose,
                    "functionality": explanation.functionality,
                    "business_logic": explanation.business_logic,
                    "intent": explanation.intent,
                    "complexity_score": explanation.complexity_score,
                    "maintainability_index": explanation.maintainability_index,
                    "technical_debt_score": explanation.technical_debt_score,
                    "code_smells": explanation.code_smells,
                    "refactoring_suggestions": explanation.refactoring_suggestions,
                    "detected_patterns": [p.value for p in explanation.detected_patterns]
                }
            except Exception as e:
                print(f"解释实体 {entity_id} 失败: {e}")
        
        # 3. 重构分析报告
        print("生成重构分析报告...")
        result["refactoring_analysis"] = self.code_explainer.generate_refactoring_report()
        
        # 4. UML图表生成
        print("生成UML图表...")
        diagrams = self.gituml.generate_all_diagrams(format="mermaid")
        result["uml_diagrams"] = {
            name: {
                "diagram_type": diagram.diagram_type.value,
                "format": diagram.format.value,
                "content": diagram.content,
                "title": diagram.title,
                "description": diagram.description,
                "entity_count": diagram.entity_count
            }
            for name, diagram in diagrams.items()
        }
        
        return result
    
    def analyze_for_refactoring(self) -> Dict[str, Any]:
        """专为重构支持的分析"""
        result = {
            "refactoring_ready": True,
            "priority_items": [],
            "architecture_issues": [],
            "code_smells_summary": {},
            "hotspots": [],
            "uml_visualizations": {},
            "recommendations": []
        }
        
        # 1. 重构热点
        hotspots = self.code_explainer._identify_refactoring_hotspots()
        result["hotspots"] = hotspots
        
        # 2. 架构问题
        arch_result = self.code_explainer.analyze_architecture()
        result["architecture_issues"] = arch_result.design_issues
        
        # 3. 代码异味汇总
        all_smells = []
        for entity in self.graph.entities.values():
            smells = self.code_explainer._detect_code_smells(entity)
            all_smells.extend(smells)
        
        smell_summary = {}
        for smell in all_smells:
            smell_type = smell.get("type", "unknown")
            if smell_type not in smell_summary:
                smell_summary[smell_type] = {"count": 0, "high_severity": 0}
            smell_summary[smell_type]["count"] += 1
            if smell.get("severity") == "high":
                smell_summary[smell_type]["high_severity"] += 1
        
        result["code_smells_summary"] = smell_summary
        
        # 4. 优先级项目
        result["priority_items"] = self.code_explainer._prioritize_refactoring_items(hotspots, all_smells)
        
        # 5. 生成关键UML图
        result["uml_visualizations"]["class_diagram"] = self.gituml.generate_class_diagram_enhanced(
            format="mermaid",
            highlight_recent_changes=True
        ).content
        
        result["uml_visualizations"]["module_dependency"] = self.gituml.generate_module_dependency_diagram(
            format="mermaid"
        ).content
        
        # 6. 综合建议
        result["recommendations"] = self._generate_refactoring_recommendations(
            hotspots, arch_result, smell_summary
        )
        
        return result
    
    def generate_documentation(self, output_path: Optional[str] = None) -> str:
        """生成增强版文档"""
        docs = []
        docs.append("# 项目架构分析报告\n")
        docs.append(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        docs.append(f"工具: ArchAI CodeExplainer + GitUML\n\n")
        
        # 1. 项目概览
        docs.append("## 1. 项目概览\n\n")
        stats = self.code_explainer._calculate_architecture_statistics()
        docs.append(f"- 总实体数: {stats['total_entities']}\n")
        docs.append(f"- 总关系数: {stats['total_relationships']}\n")
        docs.append(f"- 模块数: {stats['modules']}\n")
        docs.append(f"- 功能模块数: {stats['features']}\n\n")
        
        # 2. 架构分析
        docs.append("## 2. 架构分析\n\n")
        arch_result = self.code_explainer.analyze_architecture()
        docs.append(f"### 主要架构模式: {arch_result.primary_pattern.value}\n\n")
        
        if arch_result.secondary_patterns:
            docs.append("### 辅助模式:\n")
            for pattern in arch_result.secondary_patterns:
                docs.append(f"- {pattern.value}\n")
        docs.append("\n")
        
        # 分层结构
        docs.append("### 分层结构\n\n")
        for layer_name, layer_info in arch_result.layers.items():
            if layer_info.get("entity_count", 0) > 0:
                docs.append(f"#### {layer_name.title()}层\n")
                docs.append(f"- 实体数: {layer_info['entity_count']}\n")
                docs.append(f"- 文件数: {layer_info['file_count']}\n\n")
        
        # 3. 设计问题
        if arch_result.design_issues:
            docs.append("## 3. 设计问题\n\n")
            for issue in arch_result.design_issues[:10]:
                docs.append(f"### {issue['type']}\n")
                docs.append(f"- 严重程度: {issue['severity']}\n")
                docs.append(f"- 描述: {issue['description']}\n")
                docs.append(f"- 建议: {issue['suggestion']}\n\n")
        
        # 4. 重构热点
        docs.append("## 4. 重构热点\n\n")
        hotspots = self.code_explainer._identify_refactoring_hotspots()
        for idx, hotspot in enumerate(hotspots[:10], 1):
            docs.append(f"### {idx}. {hotspot['entity_name']}\n")
            docs.append(f"- 类型: {hotspot['entity_type']}\n")
            docs.append(f"- 文件: {hotspot['file_path']}\n")
            docs.append(f"- 热点分数: {hotspot['hotspot_score']}\n")
            docs.append(f"- 优先级: {hotspot['refactoring_priority']}\n")
            docs.append(f"- 原因: {', '.join(hotspot['reasons'])}\n\n")
        
        # 5. UML图
        docs.append("## 5. 架构图\n\n")
        docs.append("### 类图\n\n")
        class_diagram = self.gituml.generate_class_diagram_enhanced(max_entities=20)
        docs.append(f"```mermaid\n{class_diagram.content}\n```\n\n")
        
        docs.append("### 模块依赖图\n\n")
        module_diagram = self.gituml.generate_module_dependency_diagram()
        docs.append(f"```mermaid\n{module_diagram.content}\n```\n\n")
        
        # 6. 改进建议
        docs.append("## 6. 改进建议\n\n")
        for suggestion in arch_result.improvement_suggestions:
            docs.append(f"- **{suggestion['category']}**: {suggestion['suggestion']} (优先级: {suggestion['priority']})\n")
        
        content = "".join(docs)
        
        if output_path:
            with open(output_path, "w", encoding="utf-8") as f:
                f.write(content)
        
        return content
    
    def get_entity_deep_analysis(self, entity_id: str) -> Dict[str, Any]:
        """获取实体深度分析"""
        entity = self.graph.entities.get(entity_id)
        if not entity:
            return {"error": f"实体不存在: {entity_id}"}
        
        result = {
            "entity_info": {
                "id": entity.id,
                "name": entity.name,
                "type": entity.entity_type.value,
                "file_path": entity.file_path,
                "lines_of_code": entity.lines_of_code,
                "complexity": entity.complexity
            },
            "explanation": None,
            "relationships": {
                "calls": [],
                "called_by": [],
                "depends_on": [],
                "used_by": []
            },
            "uml_snippet": None
        }
        
        # 代码解释
        try:
            explanation = self.code_explainer.explain_entity(entity_id)
            result["explanation"] = {
                "purpose": explanation.purpose,
                "functionality": explanation.functionality,
                "business_logic": explanation.business_logic,
                "intent": explanation.intent,
                "invariants": explanation.invariants,
                "preconditions": explanation.preconditions,
                "postconditions": explanation.postconditions,
                "code_smells": explanation.code_smells,
                "refactoring_suggestions": explanation.refactoring_suggestions
            }
        except Exception as e:
            result["explanation_error"] = str(e)
        
        # 关系分析
        for rel in self.graph.relationships:
            if rel.source_id == entity_id:
                if rel.relationship_type == RelationshipType.CALLS:
                    target = self.graph.entities.get(rel.target_id)
                    if target:
                        result["relationships"]["calls"].append({
                            "target_id": rel.target_id,
                            "target_name": target.name,
                            "call_site": rel.call_site
                        })
                elif rel.relationship_type in [RelationshipType.DEPENDS_ON, RelationshipType.IMPORTS]:
                    target = self.graph.entities.get(rel.target_id)
                    if target:
                        result["relationships"]["depends_on"].append({
                            "target_id": rel.target_id,
                            "target_name": target.name
                        })
            
            if rel.target_id == entity_id:
                if rel.relationship_type == RelationshipType.CALLS:
                    source = self.graph.entities.get(rel.source_id)
                    if source:
                        result["relationships"]["called_by"].append({
                            "source_id": rel.source_id,
                            "source_name": source.name
                        })
                elif rel.relationship_type in [RelationshipType.USES, RelationshipType.DEPENDS_ON]:
                    source = self.graph.entities.get(rel.source_id)
                    if source:
                        result["relationships"]["used_by"].append({
                            "source_id": rel.source_id,
                            "source_name": source.name
                        })
        
        # 生成实体相关的UML片段
        if entity.entity_type == EntityType.CLASS:
            result["uml_snippet"] = self.gituml.generate_class_diagram_enhanced(
                entities=[entity_id],
                format="mermaid"
            ).content
        
        return result
    
    def compare_commits(self, commit1: str, commit2: str) -> Dict[str, Any]:
        """比较两次提交"""
        diff_diagram = self.gituml.generate_diff_diagram(commit1, commit2)
        
        result = {
            "base_commit": commit1,
            "target_commit": commit2,
            "changed_files": diff_diagram.affected_files,
            "affected_entities_count": diff_diagram.entity_count,
            "uml_diff": diff_diagram.content,
            "analysis": {}
        }
        
        # 分析受影响的实体
        affected_entities = [
            self.graph.entities.get(eid) 
            for eid in diff_diagram.affected_entities
        ]
        
        # 计算影响范围
        total_calls_affected = 0
        for entity in affected_entities:
            if entity:
                callers = self.code_explainer.reverse_call_graph.get(entity.id, set())
                total_calls_affected += len(callers)
        
        result["analysis"] = {
            "files_changed": len(diff_diagram.affected_files),
            "entities_affected": diff_diagram.entity_count,
            "potential_impact": total_calls_affected,
            "risk_level": "high" if total_calls_affected > 50 else "medium" if total_calls_affected > 20 else "low"
        }
        
        return result
    
    def _identify_key_entities(self) -> List[str]:
        """识别关键实体"""
        key_entities = []
        
        # 1. 高被调用实体
        call_counts = sorted(
            self.code_explainer.call_counts.items(),
            key=lambda x: x[1],
            reverse=True
        )
        for entity_id, _ in call_counts[:15]:
            key_entities.append(entity_id)
        
        # 2. 高复杂度实体
        complex_entities = sorted(
            [e for e in self.graph.entities.values() if e.complexity],
            key=lambda x: x.complexity,
            reverse=True
        )
        for entity in complex_entities[:15]:
            if entity.id not in key_entities:
                key_entities.append(entity.id)
        
        # 3. 大类
        large_classes = sorted(
            [e for e in self.graph.entities.values() if e.entity_type == EntityType.CLASS],
            key=lambda x: len(x.children_ids),
            reverse=True
        )
        for entity in large_classes[:10]:
            if entity.id not in key_entities:
                key_entities.append(entity.id)
        
        return key_entities
    
    def _generate_refactoring_recommendations(
        self,
        hotspots: List[Dict],
        arch_result,
        smell_summary: Dict
    ) -> List[Dict[str, Any]]:
        """生成重构建议"""
        recommendations = []
        
        # 基于热点
        critical_hotspots = [h for h in hotspots if h.get("refactoring_priority") == "critical"]
        if critical_hotspots:
            recommendations.append({
                "priority": 1,
                "category": "critical_hotspots",
                "title": "处理关键重构热点",
                "description": f"发现 {len(critical_hotspots)} 个关键重构热点需要优先处理",
                "actions": [
                    f"重构 {h['entity_name']} ({', '.join(h['reasons'][:2])})"
                    for h in critical_hotspots[:3]
                ]
            })
        
        # 基于架构问题
        high_issues = [i for i in arch_result.design_issues if i.get("severity") == "high"]
        if high_issues:
            recommendations.append({
                "priority": 2,
                "category": "architecture",
                "title": "解决架构设计问题",
                "description": f"发现 {len(high_issues)} 个高严重程度的设计问题",
                "actions": [i["suggestion"] for i in high_issues[:3]]
            })
        
        # 基于代码异味
        high_smell_types = [
            st for st, info in smell_summary.items()
            if info.get("high_severity", 0) > 5
        ]
        if high_smell_types:
            recommendations.append({
                "priority": 3,
                "category": "code_quality",
                "title": "改善代码质量",
                "description": f"发现 {len(high_smell_types)} 种代码异味大量存在",
                "actions": [f"处理 {st} 类型问题" for st in high_smell_types[:3]]
            })
        
        # 基于架构模式建议
        if arch_result.primary_pattern == ArchitecturePattern.MONOLITHIC:
            recommendations.append({
                "priority": 4,
                "category": "architecture",
                "title": "考虑架构演进",
                "description": "当前为单体架构，建议逐步演进",
                "actions": [
                    "识别边界上下文",
                    "拆分核心模块",
                    "引入分层架构"
                ]
            })
        
        return recommendations


def create_enhanced_extractor(
    graph: KnowledgeGraph,
    repo_path: str = ".",
    ai_client=None,
    config: Optional[Dict] = None
) -> EnhancedKnowledgeExtractor:
    """工厂函数：创建增强版知识提取器"""
    return EnhancedKnowledgeExtractor(graph, repo_path, ai_client, config)
