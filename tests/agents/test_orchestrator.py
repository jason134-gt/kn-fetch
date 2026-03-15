"""
测试Agent编排器
"""

import unittest
from unittest.mock import Mock, patch
from src.agents.orchestrator.orchestrator import AgentOrchestrator
from src.agents.base.agent import BaseAgent, AgentRole
from src.agents.base.context import AgentContext
from src.agents.base.result import AgentResult
from src.agents.base.message import AgentMessage


class MockAgent(BaseAgent):
    """模拟Agent用于测试"""
    
    def __init__(self, name: str):
        role = AgentRole(name=name, role="Mock Agent", focus="测试", constraint="测试")
        super().__init__(role, None)  # 传递None作为LLM客户端
    
    def analyze(self, context):
        """模拟分析方法"""
        return AgentResult(
            success=True,
            data={"test": "data"},
            output=f"测试输出 from {self.name}",
            metrics={"execution_time": 1.0}
        )


class TestAgentOrchestrator(unittest.TestCase):
    """测试AgentOrchestrator类"""
    
    def setUp(self):
        """设置测试环境"""
        self.orchestrator = AgentOrchestrator(max_workers=2)
        self.mock_context = AgentContext(
            project_root="/test/project",
            graph={"entities": [], "relationships": []},
            output_dir="/test/output"
        )
    
    def test_initialization(self):
        """测试初始化"""
        self.assertEqual(len(self.orchestrator.agents), 0)
        self.assertEqual(self.orchestrator.max_workers, 2)
        self.assertEqual(len(self.orchestrator.message_queue), 0)
    
    def test_register_agent(self):
        """测试注册Agent"""
        agent = MockAgent("test_agent")
        self.orchestrator.register_agent("test_agent", agent)
        
        self.assertTrue(self.orchestrator.has_agent("test_agent"))
        self.assertEqual(self.orchestrator.agents["test_agent"], agent)
    
    def test_register_agent_already_exists(self):
        """测试注册已存在的Agent"""
        agent1 = MockAgent("test_agent")
        agent2 = MockAgent("test_agent")
        
        self.orchestrator.register_agent("test_agent", agent1)
        
        with self.assertRaises(ValueError):
            self.orchestrator.register_agent("test_agent", agent2)
    
    def test_register_agents_batch(self):
        """测试批量注册Agent"""
        agents = {
            "agent1": MockAgent("agent1"),
            "agent2": MockAgent("agent2"),
            "agent3": MockAgent("agent3")
        }
        
        self.orchestrator.register_agents(agents)
        
        self.assertTrue(self.orchestrator.has_agent("agent1"))
        self.assertTrue(self.orchestrator.has_agent("agent2"))
        self.assertTrue(self.orchestrator.has_agent("agent3"))
        self.assertEqual(len(self.orchestrator.agents), 3)
    
    def test_unregister_agent(self):
        """测试注销Agent"""
        agent = MockAgent("test_agent")
        self.orchestrator.register_agent("test_agent", agent)
        
        result = self.orchestrator.unregister_agent("test_agent")
        self.assertTrue(result)
        self.assertFalse(self.orchestrator.has_agent("test_agent"))
    
    def test_unregister_non_existent_agent(self):
        """测试注销不存在的Agent"""
        result = self.orchestrator.unregister_agent("non_existent")
        self.assertFalse(result)
    
    def test_get_agent(self):
        """测试获取Agent"""
        agent = MockAgent("test_agent")
        self.orchestrator.register_agent("test_agent", agent)
        
        retrieved_agent = self.orchestrator.get_agent("test_agent")
        self.assertEqual(retrieved_agent, agent)
    
    def test_get_non_existent_agent(self):
        """测试获取不存在的Agent"""
        agent = self.orchestrator.get_agent("non_existent")
        self.assertIsNone(agent)
    
    def test_run_single(self):
        """测试执行单个Agent"""
        agent = MockAgent("test_agent")
        self.orchestrator.register_agent("test_agent", agent)
        
        result = self.orchestrator.run_single("test_agent", self.mock_context)
        
        self.assertTrue(result.success)
        self.assertEqual(result.data, {"test": "data"})
        self.assertEqual(result.output, "测试输出 from test_agent")
    
    def test_run_single_not_found(self):
        """测试执行不存在的Agent"""
        with self.assertRaises(ValueError):
            self.orchestrator.run_single("non_existent", self.mock_context)
    
    def test_run_single_with_error(self):
        """测试执行Agent时出错"""
        class ErrorAgent(BaseAgent):
            def __init__(self):
                role = AgentRole(name="error_agent", role="Error Agent", focus="测试", constraint="测试")
                super().__init__(role, None)
            
            def analyze(self, context):
                raise Exception("测试错误")
        
        agent = ErrorAgent()
        self.orchestrator.register_agent("error_agent", agent)
        
        result = self.orchestrator.run_single("error_agent", self.mock_context)
        
        self.assertFalse(result.success)
        self.assertEqual(result.error, "测试错误")
    
    def test_run_parallel(self):
        """测试并行执行多个Agent"""
        agents = {
            "agent1": MockAgent("agent1"),
            "agent2": MockAgent("agent2"),
            "agent3": MockAgent("agent3")
        }
        
        self.orchestrator.register_agents(agents)
        
        results = self.orchestrator.run_parallel(["agent1", "agent2", "agent3"], self.mock_context)
        
        self.assertEqual(len(results), 3)
        self.assertTrue(results["agent1"].success)
        self.assertTrue(results["agent2"].success)
        self.assertTrue(results["agent3"].success)
        self.assertEqual(results["agent1"].output, "测试输出 from agent1")
        self.assertEqual(results["agent2"].output, "测试输出 from agent2")
        self.assertEqual(results["agent3"].output, "测试输出 from agent3")
    
    def test_run_parallel_missing_agent(self):
        """测试并行执行缺失的Agent"""
        agent = MockAgent("agent1")
        self.orchestrator.register_agent("agent1", agent)
        
        with self.assertRaises(ValueError):
            self.orchestrator.run_parallel(["agent1", "agent2"], self.mock_context)
    
    def test_run_sequential(self):
        """测试顺序执行多个Agent"""
        agents = {
            "agent1": MockAgent("agent1"),
            "agent2": MockAgent("agent2"),
            "agent3": MockAgent("agent3")
        }
        
        self.orchestrator.register_agents(agents)
        
        results = self.orchestrator.run_sequential(["agent1", "agent2", "agent3"], self.mock_context)
        
        self.assertEqual(len(results), 3)
        self.assertTrue(results["agent1"].success)
        self.assertTrue(results["agent2"].success)
        self.assertTrue(results["agent3"].success)
        self.assertEqual(results["agent1"].output, "测试输出 from agent1")
        self.assertEqual(results["agent2"].output, "测试输出 from agent2")
        self.assertEqual(results["agent3"].output, "测试输出 from agent3")
    
    def test_send_and_get_message(self):
        """测试消息发送和获取"""
        message1 = AgentMessage(
            source="agent_a",
            target="agent_b",
            type="request",
            content="消息1"
1        )
        
        message2 = AgentMessage(
            source="agent_a",
            target="agent_b",
            type="response",
            content="消息2"
        )
        
        message3 = AgentMessage(
            source="agent_c",
            target="agent_b",
            type="notification",
            content="消息3"
        )
        
        self.orchestrator.send_message(message1)
        self.orchestrator.send_message(message2)
        self.orchestrator.send_message(message3)
        
        # 获取所有给agent_b的消息
        messages = self.orchestrator.get_messages("agent_b")
        self.assertEqual(len(messages), 3)
        
        # 获取特定类型的消息
        request_messages = self.orchestrator.get_messages("agent_b", "request")
        self.assertEqual(len(request_messages), 1)
        self.assertEqual(request_messages[0].content, "消息1")
        
        response_messages = self.orchestrator.get_messages("agent_b", "response")
        self.assertEqual(len(response_messages), 1)
        self.assertEqual(response_messages[0].content, "消息2")
        
        notification_messages = self.orchestrator.get_messages("agent_b", "notification")
        self.assertEqual(len(notification_messages), 1)
        self.assertEqual(notification_messages[0].content, "消息3")
    
    def test_clear_messages(self):
        """测试清空消息队列"""
        message = AgentMessage(
            source="agent_a",
            target="agent_b",
            type="request",
            content="测试消息"
        )
        
        self.orchestrator.send_message(message)
        self.assertEqual(len(self.orchestrator.message_queue), 1)
        
        self.orchestrator.clear_messages()
        self.assertEqual(len(self.orchestrator.message_queue), 0)
    
    def test_get_statistics(self):
        """测试获取统计信息"""
        agents = {
            "agent1": MockAgent("agent1"),
            "agent2": MockAgent("agent2")
        }
        
        self.orchestrator.register_agents(agents)
        
        message = AgentMessage(
            source="agent_a",
            target="agent_b",
            type="request",
            content="测试消息"
        )
        
        self.orchestrator.send_message(message)
        
        stats = self.orchestrator.get_statistics()
        
        self.assertEqual(stats["total_agents"], 2)
        self.assertEqual(stats["registered_agents"], ["agent1", "agent2"])
        self.assertEqual(stats["pending_messages"], 1)
        self.assertEqual(stats["max_workers"], 2)
    
    def test_repr(self):
        """测试repr方法"""
        agents = {
            "agent1": MockAgent("agent1"),
            "agent2": MockAgent("agent2")
        }
        
        self.orchestrator.register_agents(agents)
        
        repr_str = repr(self.orchestrator)
        self.assertIn("AgentOrchestrator", repr_str)
        self.assertIn("agents=2", repr_str)
        self.assertIn("workers=2", repr_str)


class TestAgentMessage(unittest.TestCase):
    """测试AgentMessage类"""
    
    def test_message_creation(self):
        """测试创建消息"""
        message = AgentMessage(
            source="agent_a",
            target="agent_b",
            type="request",
            content="测试消息内容"
        )
        
        self.assertEqual(message.source, "agent_a")
        self.assertEqual(message.target, "agent_b")
        self.assertEqual(message.type, "request")
        self.assertEqual(message.content, "测试消息内容")
        self.assertIsNotNone(message.created_at)
    
    def test_message_with_priority(self):
        """测试带优先级的消息"""
        message = AgentMessage(
            source="agent_a",
            target="agent_b",
            type="request",
            content="测试消息内容",
            priority=2
        )
        
        self.assertEqual(message.priority, 2)
    
    def test_message_to_dict(self):
        """测试消息转换为字典"""
        message = AgentMessage(
            source="agent_a",
            target="agent_b",
            type="request",
            content="测试消息内容",
            priority=1
        )
        
        dict_data = message.to_dict()
        self.assertEqual(dict_data["source"], "agent_a")
        self.assertEqual(dict_data["target"], "agent_b")
        self.assertEqual(dict_data["type"], "request")
        self.assertEqual(dict_data["content"], "测试消息内容")
        self.assertEqual(dict_data["priority"], 1)
        self.assertIn("created_at", dict_data)


if __name__ == '__main__':
    unittest.main()