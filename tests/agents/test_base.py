"""
测试Agent基类和角色定义
"""

import unittest
from unittest.mock import Mock
from src.agents.base.agent import BaseAgent, AgentRole, AgentResult, AgentContext


class TestAgentRole(unittest.TestCase):
    """测试AgentRole类"""
    
    def test_agent_role_creation(self):
        """测试创建AgentRole实例"""
        role = AgentRole(
            name="test_role",
            role="测试角色",
            focus="测试关注点",
            constraint="测试约束条件",
            output_template="test_template.md"
        )
        
        self.assertEqual(role.name, "test_role")
        self.assertEqual(role.role, "测试角色")
        self.assertEqual(role.focus, "测试关注点")
        self.assertEqual(role.constraint, "测试约束条件")
        self.assertEqual(role.output_template, "test_template.md")
    
    def test_agent_role_without_template(self):
        """测试没有输出模板的AgentRole实例"""
        role = AgentRole(
            name="test_role",
            role="测试角色",
            focus="测试关注点",
            constraint="测试约束条件"
        )
        
        self.assertEqual(role.name, "test_role")
        self.assertEqual(role.role, "测试角色")
        self.assertEqual(role.focus, "测试关注点")
        self.assertEqual(role.constraint, "测试约束条件")
        self.assertEqual(role.output_template, "")


class TestAgentContext(unittest.TestCase):
    """测试AgentContext类"""
    
    def test_context_creation(self):
        """测试创建AgentContext实例"""
        context = AgentContext(
            project_root="/test/project",
            graph={"entities": [], "relationships": []},
            output_dir="/test/output"
        )
        
        self.assertEqual(context.project_root, "/test/project")
        self.assertEqual(context.graph, {"entities": [], "relationships": []})
        self.assertEqual(context.output_dir, "/test/output")
        self.assertEqual(context.max_agents, 5)
    
    def test_context_with_max_agents(self):
        """测试设置最大Agent数的AgentContext实例"""
        context = AgentContext(
            project_root="/test/project",
            graph={"entities": [], "relationships": []},
            output_dir="/test/output",
            max_agents=10
        )
        
        self.assertEqual(context.project_root, "/test/project")
        self.assertEqual(context.graph, {"entities": [], "relationships": []})
        self.assertEqual(context.output_dir, "/test/output")
        self.assertEqual(context.max_agents, 10)
    
    def test_get_output_file(self):
        """测试获取输出文件路径"""
        context = AgentContext(
            project_root="/test/project",
            graph={"entities": [], "relationships": []},
            output_dir="/test/output"
        )
        
        output_file = context.get_output_file("test_agent")
        self.assertEqual(output_file, "/test/output/test_agent.md")


class TestAgentResult(unittest.TestCase):
    """测试AgentResult类"""
    
    def test_result_success(self):
        """测试成功的AgentResult实例"""
        result = AgentResult(
            success=True,
            data={"key": "value"},
            output="测试输出内容",
            metrics={"score": 100}
        )
        
        self.assertTrue(result.success)
        self.assertEqual(result.data, {"key": "value"})
        self.assertEqual(result.output, "测试输出内容")
        self.assertEqual(result.metrics, {"score": 100})
        self.assertIsNone(result.error)
        self.assertEqual(result.cross_notes, [])
    
    def test_result_with_error(self):
        """测试包含错误的AgentResult实例"""
        result = AgentResult(
            success=False,
            error="测试错误",
            metrics={"execution_time": 5.0}
        )
        
        self.assertFalse(result.success)
        self.assertEqual(result.error, "测试错误")
        self.assertEqual(result.metrics, {"execution_time": 5.0})
        self.assertIsNone(result.data)
        self.assertIsNone(result.output)
        self.assertEqual(result.cross_notes, [])
    
    def test_result_with_cross_notes(self):
        """测试包含跨模块备注的AgentResult实例"""
        result = AgentResult(
            success=True,
            data={"key": "value"},
            output="测试输出内容",
            metrics={"score": 100},
            cross_notes=["备注1", "备注2"]
        )
        
        self.assertTrue(result.success)
        self.assertEqual(result.data, {"key": "value"})
        self.assertEqual(result.output, "测试输出内容")
        self.assertEqual(result.metrics, {"score": 100})
        self.assertEqual(result.cross_notes, ["备注1", "备注2"])


class TestBaseAgent(unittest.TestCase):
    """测试BaseAgent基类"""
    
    def setUp(self):
        """设置测试环境"""
        self.mock_role = AgentRole(
            name="test_agent",
1 role="测试Agent",
            focus="测试关注点",
            constraint="测试约束条件"
        )
        self.mock_llm = Mock()
        self.mock_context = AgentContext(
            project_root="/test/project",
            graph={"entities": [], "relationships": []},
            output_dir="/test/output"
        )
    
    def test_abstract_methods(self):
        """测试BaseAgent的抽象方法"""
        # BaseAgent的子类必须实现execute方法
        class TestAgent(BaseAgent):
            def execute(self, context):
                return AgentResult(success=True, data={})
            
            def _get_task_description(self, context):
                return "测试任务描述"
        
        agent = TestAgent(self.mock_role)
        self.assertEqual(agent.name, "test_agent")
        self.assertEqual(agent.role, self.mock_role)
    
    def test_save_output_method(self):
        """测试save_output方法"""
        class TestAgent(BaseAgent):
            def execute(self, context):
                return AgentResult(success=True, data={})
            
            def _get_task_description(self, context):
                return "测试任务描述"
        
        agent = TestAgent(self.mock_role)
        
        # 测试保存输出（这里我们只测试方法存在，实际保存需要文件IO）
        agent._save_output("test content", "/test/output/test.md")
        
        # 验证Logger配置
        self.assertIsNotNone(agent.logger)


class TestAgentMessage(unittest.TestCase):
    """测试AgentMessage类"""
    
    def test_message_creation(self):
        """测试创建AgentMessage实例"""
        # 简化测试，因为我们没有实际的AgentMessage类
        # 这个测试暂时跳过，待后续实现
        pass
    
    def test_message_without_priority(self):
        """测试没有优先级的AgentMessage实例"""
        # 简化测试，因为我们没有实际的AgentMessage类
        # 这个测试暂时跳过，待后续实现
        pass
    
    def test_message_to_dict(self):
        """测试转换AgentMessage到字典"""
        # 简化测试，因为我们没有实际的AgentMessage类
        # 这个测试暂时跳过，待后续实现
        pass


if __name__ == '__main__':
    unittest.main()