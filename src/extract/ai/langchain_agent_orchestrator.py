"""
LangChain Agent 编排器

使用 LangGraph 实现多智能体协作编排
"""

import logging
import json
from typing import Dict, List, Any, Optional, TypedDict, Annotated
from operator import add
from pathlib import Path

# LangGraph 相关导入
from langgraph.graph import StateGraph, END

from .langchain_llm_client import LangChainLLMClient

logger = logging.getLogger(__name__)


class AgentState(TypedDict):
    """智能体状态"""
    messages: Annotated[List, add]
    knowledge_graph: Optional[Dict[str, Any]]
    analysis_results: Dict[str, Any]
    validated: bool = False
    optimized: bool = False


class LangChainAgentOrchestrator:
    """
    LangChain Agent 编排器
    
    实现多个智能体的协作编排，完成复杂的知识分析和优化任务
    """
    
    def __init__(self, config: Dict[str, Any]):
        """
        初始化编排器
        
        Args:
            config: AI 配置
        """
        self.config = config
        self.llm_client = LangChainLLMClient(config.get('ai', {}))
        
        if not self.llm_client.is_available():
            logger.warning("LLM 不可用，智能体编排功能受限")
        
        # 构建智能体图
        self.graph = self._build_agent_graph()
    
    def _build_agent_graph(self) -> StateGraph:
        """
        构建智能体图
        
        Returns:
            StateGraph 实例
        """
        # 创建状态图
        workflow = StateGraph(AgentState)
        
        # 添加节点
        workflow.add_node("analyzer", self._analysis_agent)
        workflow.add_node("validator", self._validation_agent)
        workflow.add_node("optimizer", self._optimization_agent)
        workflow.add_node("tester", self._testing_agent)
        workflow.add_node("summarizer", self._summarization_agent)
        
        # 定义边（流程）
        workflow.set_entry_point("analyzer")
        
        # 分析 -> 验证
        workflow.add_edge("analyzer", "validator")
        
        # 验证 -> 优化（如果需要）
        workflow.add_conditional_edges(
            "validator",
            self._should_optimize,
            {
                "optimize": "optimizer",
                "skip": "tester"
            }
        )
        
        # 优化 -> 测试
        workflow.add_edge("optimizer", "tester")
        
        # 测试 -> 总结
        workflow.add_edge("tester", "summarizer")
        
        # 总结 -> 结束
        workflow.add_edge("summarizer", END)
        
        return workflow.compile()
    
    async def _analysis_agent(self, state: AgentState) -> AgentState:
        """
        分析智能体 - 执行深度知识分析
        
        Args:
            state: 当前状态
            
        Returns:
            更新后的状态
        """
        logger.info("执行分析智能体...")
        
        from .langchain_deep_analyzer import LangChainDeepAnalyzer
        
        # 加载知识图谱
        graph = state.get("knowledge_graph", {})
        
        if not graph:
            logger.warning("知识图谱为空，跳过分析")
            state["messages"].append({
                "role": "assistant",
                "content": "知识图谱为空，跳过分析"
            })
            return state
        
        # 执行深度分析
        analyzer = LangChainDeepAnalyzer(self.config)
        results = await analyzer.analyze_all_async(graph)
        
        # 更新状态
        state["analysis_results"] = results
        state["messages"].append({
            "role": "assistant",
            "content": f"深度分析完成，生成了 {len(results)} 份文档"
        })
        
        return state
    
    async def _validation_agent(self, state: AgentState) -> AgentState:
        """
        验证智能体 - 验证知识图谱的准确性
        
        Args:
            state: 当前状态
            
        Returns:
            更新后的状态
        """
        logger.info("执行验证智能体...")
        
        graph = state.get("knowledge_graph", {})
        
        if not graph:
            logger.warning("知识图谱为空，跳过验证")
            state["validated"] = True
            return state
        
        # 执行验证
        validation_result = await self.llm_client.validate_knowledge(
            extracted_knowledge=graph,
            original_content="",  # 可以从原始文件读取
            extraction_type="code_analysis"
        )
        
        # 更新状态
        state["validated"] = validation_result.get("is_valid", True)
        state["messages"].append({
            "role": "assistant",
            "content": f"知识验证完成，准确性: {validation_result.get('confidence', 0):.2f}"
        })
        
        return state
    
    def _should_optimize(self, state: AgentState) -> str:
        """
        决定是否需要优化
        
        Args:
            state: 当前状态
            
        Returns:
            "optimize" 或 "skip"
        """
        # 如果验证通过，跳过优化
        if state.get("validated", True):
            return "skip"
        
        # 否则进行优化
        return "optimize"
    
    async def _optimization_agent(self, state: AgentState) -> AgentState:
        """
        优化智能体 - 优化知识图谱
        
        Args:
            state: 当前状态
            
        Returns:
            更新后的状态
        """
        logger.info("执行优化智能体...")
        
        graph = state.get("knowledge_graph", {})
        
        if not graph:
            logger.warning("知识图谱为空，跳过优化")
            state["optimized"] = True
            return state
        
        # 执行优化
        optimized_graph = await self.llm_client.optimize_knowledge(
            knowledge_graph=graph,
            optimization_level="medium"
        )
        
        # 更新状态
        state["knowledge_graph"] = optimized_graph
        state["optimized"] = True
        state["messages"].append({
            "role": "assistant",
            "content": "知识优化完成"
        })
        
        return state
    
    async def _testing_agent(self, state: AgentState) -> AgentState:
        """
        测试智能体 - 生成测试用例
        
        Args:
            state: 当前状态
            
        Returns:
            更新后的状态
        """
        logger.info("执行测试智能体...")
        
        graph = state.get("knowledge_graph", {})
        
        if not graph:
            logger.warning("知识图谱为空，跳过测试生成")
            return state
        
        # 为主要实体生成测试用例
        test_cases = []
        entities = graph.get("entities", {})
        
        for entity_id, entity in list(entities.items())[:10]:  # 限制数量
            test_cases.extend(
                await self.llm_client.generate_test_cases(
                    code_content=entity.get("content", ""),
                    entity_name=entity.get("name", "unknown"),
                    entity_type=entity.get("type", "unknown")
                )
            )
        
        state["messages"].append({
            "role": "assistant",
            "content": f"测试用例生成完成，共 {len(test_cases)} 个测试用例"
        })
        
        # 将测试用例保存到状态中
        state["test_cases"] = test_cases
        
        return state
    
    async def _summarization_agent(self, state: AgentState) -> AgentState:
        """
        总结智能体 - 生成最终总结报告
        
        Args:
            state: 当前状态
            
        Returns:
            更新后的状态
        """
        logger.info("执行总结智能体...")
        
        # 生成总结
        summary = f"""
# 知识分析总结报告

## 执行概览
- 深度分析: 已完成
- 知识验证: {'通过' if state.get('validated') else '需要优化'}
- 知识优化: {'已完成' if state.get('optimized') else '未执行'}
- 测试生成: 已完成

## 分析结果
{json.dumps(state.get('analysis_results', {}), ensure_ascii=False, indent=2)}

## 建议和下一步
1. 查看 `output/doc/` 目录下的详细设计文档
2. 根据验证结果进行必要的调整
3. 运行生成的测试用例
"""
        
        state["messages"].append({
            "role": "assistant",
            "content": summary
        })
        
        # 保存总结报告
        output_path = Path("output/doc") / "summary_report.md"
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(summary, encoding='utf-8')
        
        return state
    
    async def run(
        self,
        knowledge_graph: Dict[str, Any],
        initial_messages: Optional[List[Dict]] = None
    ) -> Dict[str, Any]:
        """
        运行智能体编排
        
        Args:
            knowledge_graph: 知识图谱
            initial_messages: 初始消息列表
            
        Returns:
            最终状态
        """
        # 初始化状态
        initial_state = AgentState(
            messages=initial_messages or [],
            knowledge_graph=knowledge_graph,
            analysis_results={},
            validated=False,
            optimized=False
        )
        
        # 运行智能体图
        final_state = await self.graph.ainvoke(initial_state)
        
        return final_state


class SimpleAgentOrchestrator:
    """
    简化版智能体编排器
    
    用于快速执行单个智能体任务
    """
    
    def __init__(self, config: Dict[str, Any]):
        """
        初始化编排器
        
        Args:
            config: AI 配置
        """
        self.config = config
        self.llm_client = LangChainLLMClient(config.get('ai', {}))
    
    async def analyze_and_validate(
        self,
        knowledge_graph: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        分析并验证知识图谱
        
        Args:
            knowledge_graph: 知识图谱
            
        Returns:
            分析和验证结果
        """
        results = {}
        
        # 深度分析
        if knowledge_graph:
            from .langchain_deep_analyzer import LangChainDeepAnalyzer
            analyzer = LangChainDeepAnalyzer(self.config)
            results["analysis"] = await analyzer.analyze_all_async(knowledge_graph)
        
        # 验证
        if knowledge_graph:
            results["validation"] = await self.llm_client.validate_knowledge(
                extracted_knowledge=knowledge_graph,
                original_content="",
                extraction_type="code_analysis"
            )
        
        return results
    
    async def optimize_and_test(
        self,
        knowledge_graph: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        优化并测试知识图谱
        
        Args:
            knowledge_graph: 知识图谱
            
        Returns:
            优化和测试结果
        """
        results = {}
        
        # 优化
        if knowledge_graph:
            results["optimized_graph"] = await self.llm_client.optimize_knowledge(
                knowledge_graph=knowledge_graph,
                optimization_level="medium"
            )
        
        # 测试
        if knowledge_graph:
            test_cases = []
            entities = knowledge_graph.get("entities", {})
            
            for entity_id, entity in list(entities.items())[:5]:
                test_cases.extend(
                    await self.llm_client.generate_test_cases(
                        code_content=entity.get("content", ""),
                        entity_name=entity.get("name", "unknown"),
                        entity_type=entity.get("type", "unknown")
                    )
                )
            
            results["test_cases"] = test_cases
        
        return results


__all__ = [
    "LangChainAgentOrchestrator",
    "SimpleAgentOrchestrator",
    "AgentState",
]
