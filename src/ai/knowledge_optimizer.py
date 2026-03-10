import json
from typing import Dict, Any, List
from pathlib import Path
from ..gitnexus import KnowledgeGraph, CodeEntity
from .ai_client import AIClient

class KnowledgeOptimizer:
    """知识优化器，基于LLM实现知识自优化"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.ai_client = AIClient(config_path)
        self.config = self._load_config(config_path)
        self.optimization_config = self.config.get("optimization", {})
        
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """加载配置文件"""
        import yaml
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                return yaml.safe_load(f)
        except Exception as e:
            raise Exception(f"加载配置文件失败: {str(e)}")
    
    def optimize_knowledge_graph(
        self,
        graph: KnowledgeGraph,
        optimization_level: str = "medium"
    ) -> KnowledgeGraph:
        """优化知识图谱，补充重构所需的技术细节"""
        print(f"开始优化知识图谱，优化级别: {optimization_level}")
        
        # 1. 补充实体的复杂度分析
        graph = self._add_complexity_analysis(graph)
        
        # 2. 优化关系，补全缺失的依赖
        graph = self._enrich_relationships(graph)
        
        # 3. 生成重构建议
        graph = self._generate_refactoring_suggestions(graph)
        
        # 4. 合并重复实体
        graph = self._merge_duplicate_entities(graph)
        
        print("知识图谱优化完成")
        return graph
    
    def _add_complexity_analysis(self, graph: KnowledgeGraph) -> KnowledgeGraph:
        """为代码实体添加复杂度分析"""
        entities_to_analyze = [
            entity for entity in graph.entities.values()
            if entity.entity_type in ["class", "function", "method"]
            and len(entity.content) > 0
        ]
        
        print(f"正在分析 {len(entities_to_analyze)} 个实体的复杂度...")
        
        for entity in entities_to_analyze:
            analysis_result = self.ai_client.analyze_code(
                entity.content,
                analysis_type="complexity",
                context=f"实体名称: {entity.name}, 类型: {entity.entity_type}"
            )
            
            entity.metadata["complexity_analysis"] = analysis_result
            
            if "cyclomatic_complexity" in analysis_result:
                entity.complexity = analysis_result["cyclomatic_complexity"]
        
        return graph
    
    def _enrich_relationships(self, graph: KnowledgeGraph) -> KnowledgeGraph:
        """补全实体之间的关系"""
        # 这里可以实现更智能的关系补全逻辑
        # 例如基于代码内容分析调用关系、依赖关系等
        return graph
    
    def _generate_refactoring_suggestions(self, graph: KnowledgeGraph) -> KnowledgeGraph:
        """生成重构建议"""
        high_complexity_entities = [
            entity for entity in graph.entities.values()
            if entity.complexity and entity.complexity > 10  # 复杂度阈值可配置
        ]
        
        print(f"为 {len(high_complexity_entities)} 个高复杂度实体生成重构建议...")
        
        for entity in high_complexity_entities:
            refactor_suggestion = self.ai_client.analyze_code(
                entity.content,
                analysis_type="refactoring",
                context=f"实体名称: {entity.name}, 类型: {entity.entity_type}, 当前复杂度: {entity.complexity}"
            )
            
            entity.metadata["refactoring_suggestion"] = refactor_suggestion
        
        # 生成整体重构建议
        overall_suggestion = self.ai_client.optimize_knowledge(
            graph.model_dump(),
            optimization_goal="refactoring_support"
        )
        
        if graph.metadata is None:
            graph.metadata = {}
        graph.metadata["overall_refactoring_suggestion"] = overall_suggestion
        
        return graph
    
    def _merge_duplicate_entities(self, graph: KnowledgeGraph) -> KnowledgeGraph:
        """合并重复的实体"""
        # 基于实体名称和内容相似度合并重复实体
        return graph
    
    def optimize_entity(
        self,
        entity: CodeEntity,
        source_content: str = None
    ) -> CodeEntity:
        """优化单个实体的知识"""
        validation_result = self.ai_client.validate_knowledge(
            entity.model_dump(),
            source_content or entity.content
        )
        
        if not validation_result.get("is_valid", True):
            # 修正实体信息
            for error in validation_result.get("errors", []):
                # 应用修正
                pass
        
        return entity
