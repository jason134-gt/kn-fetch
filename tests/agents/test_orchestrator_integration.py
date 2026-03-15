"""
测试Agent编排器集成
"""

import unittest
from src.agents.orchestrator.orchestrator import AgentOrchestrator
from src.agents.base.context import AgentContext
from src.agents.roles.architecture.overview_agent import OverviewAgent
from src.agents.roles.architecture.layers_agent import LayersAgent
from src.agents.roles.architecture.dependencies_agent import DependenciesAgent


class TestAgentOrchestratorIntegration(unittest.TestCase):
    """测试Agent编排器集成"""
    
    def setUp(self):
        """设置测试环境"""
        self.orchestrator = AgentOrchestrator(max_workers=3)
        
        # 创建Agent
        self.overview_agent = OverviewAgent()
        self.layers_agent = LayersAgent()
        self.dependencies_agent = DependenciesAgent()
        
        # 注册Agent
        self.orchestrator.register_agent("overview", self.overview_agent)
        self.orchestrator.register_agent("layers", self.layers_agent)
        self.orchestrator.register_agent("dependencies", self.dependencies_agent)
        
        # 创建模拟上下文
        self.mock_context = AgentContext(
            project_root="/test/project",
            graph={
                "entities": {
                    "entity1": {"name": "TestClass", "type": "class", "file": "src/main.py"},
                    "entity2": {"name": "test_function", "type": "function", "file": "src/utils.py"}
                },
                "relationships": [
                    {"source": "entity1", "target": "entity2", "type": "calls"}
                ]
            },
            output_dir="/test/output"
        )
    
    def test_single_agent_execution(self):
        """测试单个Agent执行"""
        # 测试OverviewAgent
        result1 = self.orchestrator.run_single("overview", self.mock_context)
        self.assertTrue(result1.success)
        self.assertIn("entities_analyzed", result1.metrics)
        
        # 测试LayersAgent
        result2 = self.orchestrator.run_single("layers", self.mock_context)
        self.assertTrue(result2.success)
        self.assertIn("packages_analyzed", result2.metrics)
        
        # 测试DependenciesAgent
        result3 = self.orchestrator.run_single("dependencies", self.mock_context)
        self.assertTrue(result3.success)
        self.assertIn("external_dependencies_count", result3.metrics)
    
    def test_parallel_execution(self):
        """测试并行执行"""
        agent_names = ["overview", "layers", "dependencies"]
        results = self.orchestrator.run_parallel(agent_names, self.mock_context)
        
        self.assertEqual(len(results), 3)
        
        # 验证每个Agent的结果
        self.assertTrue(results["overview"].success)
        self.assertTrue(results["layers"].success)
        self.assertTrue(results["dependencies"].success)
        
        # 验证统计信息
        stats = self.orchestrator.get_statistics()
        self.assertEqual(stats["total_agents"], 3)
        self.assertEqual(stats["registered_agents"], agent_names)
        self.assertEqual(stats["max_workers"], 3)
    
    def test_sequential_execution(self):
        """测试顺序执行"""
        agent_names = ["overview", "layers", "dependencies"]
        results = self.orchestrator.run_sequential(agent_names, self.mock_context)
        
        self.assertEqual(len(results), 3)
        
        # 验证执行顺序
        for agent_name in agent_names:
            self.assertTrue(results[agent_name].success)
    
    def test_fail_fast_execution(self):
        """测试失败快速执行模式"""
        # 创建一个会失败的Agent
        class FaultyAgent(OverviewAgent):
            def execute(self, context):
                raise Exception("模拟故障")
        
        faulty_agent = FaultyAgent()
        self.orchestrator.register_agent("faulty", faulty_agent)
        
        # 测试fail_fast模式
        try:
            results = self.orchestrator.run_parallel(["overview", "faulty", "layers"], self.mock_context, fail_fast=True)
            # 如果这里没有被捕获，说明fail_fast工作不正常
            self.fail("应该捕获异常")
        except Exception as e:
            # 验证异常消息
            self.assertIn("Agent 'faulty' execution failed", str(e))
    
    def test_message_system(self):
        """测试消息系统"""
        from src.agents.base.message import AgentMessage
        
        # 发送消息
        message1 = AgentMessage(
            source="overview",
            target="layers",
            type="request",
            content="请提供分层结构信息"
        )
        
        message2 = AgentMessage(
            source="layers",
            target="dependencies",
            type="request",
            content="需要依赖关系分析结果"
        )
        
        self.orchestrator.send_message(message1)
        self.orchestrator.send_message(message2)
        
        # 获取layers的消息
        layers_messages = self.orchestrator.get_messages("layers")
        self.assertEqual(len(layers_messages), 1)
        self.assertEqual(layers_messages[0].source, "overview")
        self.assertEqual(layers_messages[0].content, "请提供分层结构信息")
        
        # 获取dependencies的消息
        dependencies_messages = self.orchestrator.get_messages("dependencies")
        self.assertEqual(len(dependencies_messages), 1)
        self.assertEqual(dependencies_messages[0].source, "layers")
        self.assertEqual(dependencies_messages[0].content, "需要依赖关系分析结果")
        
        # 获取特定类型的消息
        request_messages = self.orchestrator.get_messages("layers", "request")
        self.assertEqual(len(request_messages), 1)
        
        # 清空消息
        self.orchestrator.clear_messages()
        self.assertEqual(len(self.orchestrator.message_queue), 0)
    
    def test_agent_removal(self):
        """测试Agent移除"""
        self.assertTrue(self.orchestrator.has_agent("overview"))
        
        # 移除Agent
        removed = self.orchestrator.unregister_agent("overview")
        self.assertTrue(removed)
        self.assertFalse(self.orchestrator.has_agent("overview"))
        
        # 尝试再次执行
        try:
            self.orchestrator.run_single("overview", self.mock_context)
            self.fail("应该抛出异常")
        except ValueError as e:
            self.assertIn("Agent 'overview' not found", str(e))
    
    def test_statistics(self):
        """测试统计功能"""
        # 执行几个Agent
        self.orchestrator.run_single("overview", self.mock_context)
        self.orchestrator.run_single("layers", self.mock_context)
        
        # 发送消息
        from src.agents.base.message import AgentMessage
        message = AgentMessage(
            source="overview",
            target="layers",
            type="notification",
            content="测试消息"
        )
        self.orchestrator.send_message(message)
        
        # 获取统计信息
        stats = self.orchestrator.get_statistics()
        
        self.assertEqual(stats["total_agents"], 3)
        self.assertEqual(stats["registered_agents"], ["overview", "layers", "dependencies"])
        self.assertEqual(stats["pending_messages"], 1)
        self.assertEqual(stats["max_workers"], 3)
    
    def test_max_workers_configuration(self):
        """测试最大工作线程配置"""
        orchestrator1 = AgentOrchestrator(max_workers=1)
        orchestrator2 = AgentOrchestrator(max_workers=10)
        
        self.assertEqual(orchestrator1.max_workers, 1)
        self.assertEqual(orchestrator2.max_workers, 10)
    
    def test_agent_get_function(self):
        """测试获取Agent函数"""
        agent = self.orchestrator.get_agent("overview")
        self.assertIsInstance(agent, OverviewAgent)
        
        non_existent = self.orchestrator.get_agent("non_existent")
        self.assertIsNone(non_existent)


class TestAgentContext(unittest.TestCase):
    """测试AgentContext"""
    
    def test_context_creation(self):
        """测试上下文创建"""
        context = AgentContext(
            project_root="/test/project",
            graph={"entities": [], "relationships": []},
            output_dir="/test/output"
        )
        
        self.assertEqual(context.project_root, "/test/project")
        self.assertEqual(context.graph, {"entities": [], "relationships": []})
        self.assertEqual(context.output_dir, "/test/output")
        self.assertEqual(context.max_agents, 5)
    
    def test_get_output_file(self):
        """测试获取输出文件"""
        context = AgentContext(
            project_root="/test/project",
            graph={"entities": [], "relationships": []},
            output_dir="/test/output"
        )
        
        output_file = context.get_output_file("test_agent")
        self.assertEqual(output_file, "/test/output/test_agent.md")
        
        output_file = context.get_output_file("another_agent")
        self.assertEqual(output_file, "/test/output/another_agent.md")
    
    def test_max_agents_configuration(self):
        """测试最大Agent数量配置"""
        context1 = AgentContext(
            project_root="/test/project",
            graph={"entities": [], "relationships": []},
            output_dir="/test/output",
            max_agents=3
        )
        
        context2 = AgentContext(
            project_root="/test/project",
            graph={"entities": [], "relationships": []},
            output_dir="/test/output",
            max_agents=10
        )
        
        self.assertEqual(context1.max_agents, 3)
        self.assertEqual(context2.max_agents, 10)


class TestAgentResult(unittest.TestCase):
    """测试AgentResult"""
    
    def test_result_success(self):
        """测试成功的结果"""
        from src.agents.base.result import AgentResult
        
        result = AgentResult(
            success=True,
            data={"test": "data"},
            output="测试输出内容",
            metrics={"score": 100},
            cross_notes=["备注1", "备注2"]
        )
        
        self.assertTrue(result.success)
        self.assertEqual(result.data, {"test": "data"})
        self.assertEqual(result.output, "测试输出内容")
        self.assertEqual(result.metrics, {"score": 100})
        self.assertEqual(result.cross_notes, ["备注1", "备注2"])
        self.assertIsNone(result.error)
    
    def test_result_failure(self):
        """测试失败的结果"""
        from src.agents.base.result import AgentResult
        
        result = AgentResult(
            success=False,
            error="执行失败",
            metrics={"execution_time": 2.5}
        )
        
        self.assertFalse(result.success)
        self.assertEqual(result.error, "执行失败")
        self.assertEqual(result.metrics, {"execution_time": 2.5})
        self.assertIsNone(result.data)
        self.assertIsNone(result.output)
        self.assertEqual(result.cross_notes, [])


if __name__ == '__main__':
    unittest.main()