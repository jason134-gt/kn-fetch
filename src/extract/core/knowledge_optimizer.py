"""
知识优化器 - 实现与LLM交互的知识优化、自测试、自验证能力
"""
import logging
from typing import List, Dict, Any, Optional
from ..ai.llm_client import LLMClient
from ..gitnexus import KnowledgeGraph, CodeEntity, AnalysisResult
logger = logging.getLogger(__name__)
class KnowledgeOptimizer:
    """知识优化器"""
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.llm_client = LLMClient(config.get('llm', {}))
        self.enable_auto_optimization = config.get('enable_auto_optimization', True)
        self.enable_self_validation = config.get('enable_self_validation', True)
    async def optimize_knowledge(self, knowledge_graph: KnowledgeGraph) -> KnowledgeGraph:
        """优化知识图谱，补全缺失信息，修正错误"""
        if not self.enable_auto_optimization:
            return knowledge_graph
        logger.info("开始优化知识图谱")
        try:
            # 生成知识图谱摘要
            graph_summary = self._generate_graph_summary(knowledge_graph)
            # 调用LLM优化
            optimization_result = await self.llm_client.chat(
                system_prompt=self._get_optimization_prompt(),
                user_prompt=f"请优化以下知识图谱：\n{graph_summary}"
            )
            # 应用优化结果
            knowledge_graph = self._apply_optimization(knowledge_graph, optimization_result)
            logger.info("知识图谱优化完成")
        except Exception as e:
            logger.error(f"知识优化失败: {e}", exc_info=True)
        return knowledge_graph
    async def validate_knowledge(self, knowledge_graph: KnowledgeGraph) -> Dict[str, Any]:
        """自验证知识准确性"""
        if not self.enable_self_validation:
            return {"valid": True, "errors": [], "warnings": []}
        logger.info("开始验证知识准确性")
        validation_result = {
            "valid": True,
            "errors": [],
            "warnings": [],
            "confidence_score": 0.0
        }
        try:
            # 基础校验
            validation_result = self._basic_validation(knowledge_graph, validation_result)
            # LLM深度校验
            if self.llm_client.is_available():
                validation_result = await self._llm_deep_validation(knowledge_graph, validation_result)
            # 计算置信度
            total_checks = len(validation_result["errors"]) + len(validation_result["warnings"]) + 1
            validation_result["confidence_score"] = max(0.0, 1.0 - len(validation_result["errors"]) / total_checks)
            if validation_result["errors"]:
                validation_result["valid"] = False
            logger.info(f"知识验证完成，置信度: {validation_result['confidence_score']:.2f}, 错误: {len(validation_result['errors'])}, 警告: {len(validation_result['warnings'])}")
        except Exception as e:
            logger.error(f"知识验证失败: {e}", exc_info=True)
            validation_result["errors"].append(f"验证失败: {str(e)}")
            validation_result["valid"] = False
        return validation_result
    async def generate_test_cases(self, knowledge_graph: KnowledgeGraph) -> List[Dict[str, Any]]:
        """自动生成测试用例"""
        if not self.llm_client.is_available():
            logger.warning("LLM不可用，无法生成测试用例")
            return []
        logger.info("开始生成测试用例")
        test_cases = []
        try:
            # 提取核心接口和类
            core_entities = [e for e in knowledge_graph.entities if e.type in ["class", "function", "method"]]
            if core_entities:
                entities_summary = "\n".join([f"- {e.type}: {e.name} - {e.description or '无描述'}" for e in core_entities[:20]])
                test_cases_result = await self.llm_client.chat(
                    system_prompt=self._get_test_case_prompt(),
                    user_prompt=f"请为以下代码实体生成测试用例：\n{entities_summary}"
                )
                test_cases = self._parse_test_cases(test_cases_result)
            logger.info(f"生成了 {len(test_cases)} 个测试用例")
        except Exception as e:
            logger.error(f"生成测试用例失败: {e}", exc_info=True)
        return test_cases
    def _generate_graph_summary(self, knowledge_graph: KnowledgeGraph) -> str:
        """生成知识图谱摘要"""
        summary = []
        summary.append(f"实体总数: {len(knowledge_graph.entities)}")
        summary.append(f"关系总数: {len(knowledge_graph.relations)}")
        # 按类型统计实体
        type_counts = {}
        for entity in knowledge_graph.entities:
            type_counts[entity.type] = type_counts.get(entity.type, 0) + 1
        summary.append("实体类型统计:")
        for etype, count in type_counts.items():
            summary.append(f"  - {etype}: {count}")
        # 示例实体
        summary.append("\n核心实体示例:")
        for entity in knowledge_graph.entities[:10]:
            summary.append(f"  - [{entity.type}] {entity.name} (文件: {entity.file_path}:{entity.line_start})")
        return "\n".join(summary)
    def _get_optimization_prompt(self) -> str:
        """获取优化提示词"""
        return """
你是一个代码知识图谱优化专家，请对输入的代码知识图谱进行优化：
1. 补全缺失的函数描述、参数说明、返回值说明
2. 修正错误的依赖关系和调用关系
3. 识别潜在的技术债务和重构点
4. 补充代码复杂度、性能等分析信息
请以结构化的格式返回优化结果。
"""
    def _apply_optimization(self, knowledge_graph: KnowledgeGraph, optimization_result: str) -> KnowledgeGraph:
        """应用优化结果到知识图谱"""
        # 简单实现，实际项目中需要解析优化结果并更新实体
        knowledge_graph.optimization_notes = optimization_result
        return knowledge_graph
    def _basic_validation(self, knowledge_graph: KnowledgeGraph, validation_result: Dict[str, Any]) -> Dict[str, Any]:
        """基础校验"""
        for entity in knowledge_graph.entities:
            # 检查必填字段
            if not entity.name:
                validation_result["errors"].append(f"实体ID {entity.id} 缺少名称")
            if not entity.file_path:
                validation_result["warnings"].append(f"实体 {entity.name} 缺少文件路径")
            # 检查循环依赖
            if self._has_cyclic_dependency(entity, knowledge_graph):
                validation_result["warnings"].append(f"实体 {entity.name} 存在循环依赖")
        return validation_result
    def _has_cyclic_dependency(self, entity: CodeEntity, knowledge_graph: KnowledgeGraph) -> bool:
        """检查是否存在循环依赖"""
        # 简化实现
        visited = set()
        def dfs(current_id: str, path: set) -> bool:
            if current_id in path:
                return True
            if current_id in visited:
                return False
            visited.add(current_id)
            path.add(current_id)
            # 查找依赖关系
            for rel in knowledge_graph.relations:
                if rel.source_id == current_id and rel.relation_type == "depends_on":
                    if dfs(rel.target_id, path):
                        return True
            path.remove(current_id)
            return False
        return dfs(entity.id, set())
    async def _llm_deep_validation(self, knowledge_graph: KnowledgeGraph, validation_result: Dict[str, Any]) -> Dict[str, Any]:
        """LLM深度校验"""
        try:
            # 提取部分实体进行校验
            sample_entities = knowledge_graph.entities[:10]
            entities_str = "\n".join([f"{e.type}: {e.name} - {e.description or ''}" for e in sample_entities])
            result = await self.llm_client.chat(
                system_prompt="你是代码验证专家，请检查以下代码实体描述是否准确，指出存在的错误和警告。",
                user_prompt=f"请检查这些代码实体信息是否准确：\n{entities_str}\n返回格式：错误列表：每个错误一行；警告列表：每个警告一行。"
            )
            # 解析结果
            if "错误列表：" in result:
                errors_part = result.split("错误列表：")[1].split("警告列表：")[0] if "警告列表：" in result else result.split("错误列表：")[1]
                errors = [e.strip() for e in errors_part.split("\n") if e.strip()]
                validation_result["errors"].extend(errors)
            if "警告列表：" in result:
                warnings_part = result.split("警告列表：")[1]
                warnings = [w.strip() for w in warnings_part.split("\n") if w.strip()]
                validation_result["warnings"].extend(warnings)
        except Exception as e:
            logger.warning(f"LLM深度校验失败: {e}")
        return validation_result
    def _get_test_case_prompt(self) -> str:
        """获取测试用例生成提示词"""
        return """
你是测试用例生成专家，请为提供的代码实体生成单元测试用例。
每个测试用例包含：
- 测试名称
- 测试场景
- 输入参数
- 预期输出
- 测试步骤
请使用JSON数组格式返回。
"""
    def _parse_test_cases(self, test_cases_result: str) -> List[Dict[str, Any]]:
        """解析测试用例结果"""
        import json
        try:
            # 尝试提取JSON部分
            if "```json" in test_cases_result:
                json_part = test_cases_result.split("```json")[1].split("```")[0].strip()
                return json.loads(json_part)
            elif "[" in test_cases_result and "]" in test_cases_result:
                json_part = test_cases_result[test_cases_result.index("["):test_cases_result.rindex("]")+1]
                return json.loads(json_part)
        except Exception as e:
            logger.warning(f"解析测试用例失败: {e}")
        # 简单解析文本格式
        test_cases = []
        current_case = {}
        for line in test_cases_result.split("\n"):
            line = line.strip()
            if line.startswith("- 测试名称:"):
                if current_case:
                    test_cases.append(current_case)
                current_case = {"name": line.replace("- 测试名称:", "").strip()}
            elif line.startswith("测试场景:") and current_case:
                current_case["scenario"] = line.replace("测试场景:", "").strip()
            elif line.startswith("输入参数:") and current_case:
                current_case["input"] = line.replace("输入参数:", "").strip()
            elif line.startswith("预期输出:") and current_case:
                current_case["expected"] = line.replace("预期输出:", "").strip()
        if current_case:
            test_cases.append(current_case)
        return test_cases
