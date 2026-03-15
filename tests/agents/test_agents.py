"""
测试具体Agent实现
"""

import unittest
from unittest.mock import Mock
from src.agents.base.context import AgentContext
from src.agents.roles.architecture.overview_agent import OverviewAgent
from src.agents.roles.architecture.layers_agent import LayersAgent
from src.agents.roles.architecture.dependencies_agent import DependenciesAgent


class TestOverviewAgent(unittest.TestCase):
    """测试OverviewAgent"""
    
    def setUp(self):
        """设置测试环境"""
        self.agent = OverviewAgent()
        self.context = AgentContext(
            project_root="/test/project",
            graph={
                "entities": {
                    "entity1": Mock(entity_type=Mock(value="class"), name="TestClass", file_path="test.py", start_line=10),
                    "entity2": Mock(entity_type=Mock(value="function"), name="test_function", file_path="test.py", start_line=20),
                    "entity3": Mock(entity_type=Mock(value="method"), name="test_method", file_path="test.py", start_line=30)
                },
                "relationships": [
                    Mock(source_id="entity1", target_id="entity2"),
                    Mock(source_id="entity1", target_id="entity3")
                ]
            },
            output_dir="/test/output"
        )
    
    def test_agent_creation(self):
        """测试Agent创建"""
        self.assertEqual(self.agent.name, "overview")
        self.assertEqual(self.agent.role.role, "首席系统架构师")
        
    def test_agent_execute(self):
        """测试Agent执行"""
        result = self.agent.execute(self.context)
        
        self.assertTrue(result.success)
        self.assertIn("total_entities", result.data)
        self.assertIn("file_count", result.data)
        self.assertIn("entities_analyzed", result.metrics)
        self.assertIn("files_analyzed", result.metrics)
        
    def test_get_task_description(self):
        """测试获取任务描述"""
        description = self.agent._get_task_description(self.context)
        self.assertIn("总体架构", description)
        self.assertIn("技术决策", description)


class TestLayersAgent(unittest.TestCase):
    """测试LayersAgent"""
    
    def setUp(self):
        """设置测试环境"""
        self.agent = LayersAgent()
        self.context = AgentContext(
            project_root="/test/project",
            graph={
                "entities": {
                    "entity1": Mock(entity_type=Mock(value="class"), name="TestController", file_path="controller/test.py", start_line=10),
                    "entity2": Mock(entity_type=Mock(value="class"), name="TestService", file_path="service/test.py", start_line=20),
                    "entity3": Mock(entity_type=Mock(value="class"), name="TestRepository", file_path="repository/test.py", start_line=30),
                    "entity4": Mock(entity_type=Mock(value="function"), name="test_func", file_path="util/test.py", start_line=40)
                },
                "relationships": [
                    Mock(source_id="entity1", target_id="entity2", relationship_type=Mock(value="calls")),
                    Mock(source_id="entity2", target_id="entity3", relationship_type=Mock(value="calls"))
                ]
            },
            output_dir="/test/output"
        )
    
    def test_agent_creation(self):
        """测试Agent创建"""
        self.assertEqual(self.agent.name, "layers")
        self.assertEqual(self.agent.role.role, "资深软件设计师")
        
    def test_agent_execute(self):
        """测试Agent执行"""
        result = self.agent.execute(self.context)
        
        self.assertTrue(result.success)
        self.assertIn("packages_analyzed", result.metrics)
        self.assertIn("layers_identified", result.metrics)
        
    def test_extract_package(self):
        """测试提取包路径"""
        package_path = self.agent._extract_package("/test/project/src/controller/test.py")
        self.assertEqual(package_path, "/test/project/src")
        
        package_path = self.agent._extract_package("/test/project/test.py")
        self.assertEqual(package_path, "/test/project")
        
        package_path = self.agent._extract_package("test.py")
        self.assertEqual(package_path, "root")


class TestDependenciesAgent(unittest.TestCase):
    """测试DependenciesAgent"""
    
    def setUp(self):
        """设置测试环境"""
        self.agent = DependenciesAgent()
        self.context = AgentContext(
            project_root="/test/project",
            graph={"entities": {}, "relationships": []},
            output_dir="/test/output"
        )
        
        # 创建测试目录结构
        import os
        import tempfile
        
        self.temp_dir = tempfile.TemporaryDirectory()
        self.project_root = self.temp_dir.name
        
        # 创建测试文件
        requirements_content = """
# Python依赖测试
flask==2.3.2
django>=3.2
pandas~=1.5
requests
"""
        
        package_json_content = """
{
  "name": "test-project",
  "version": "1.0.0",
  "dependencies": {
    "react": "^18.2.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "typescript": "^5.3.3"
  }
}
"""
        
        # 写入测试文件
        with open(os.path.join(self.project_root, "requirements.txt"), "w", encoding="utf-8") as f:
            f.write(requirements_content)
        
        with open(os.path.join(self.project_root, "package.json"), "w", encoding="utf-8") as f:
            f.write(package_json_content)
        
        # 创建Python模块
        with open(os.path.join(self.project_root, "main.py"), "w", encoding="utf-8") as f:
            f.write("import flask\nimport pandas\nfrom models import User\n")
        
        with open(os.path.join(self.project_root, "models.py"), "w", encoding="utf-8") as f:
            f.write("from typing import Dict\nimport os\n\nclass User:\n    pass\n")
    
    def tearDown(self):
        """清理测试环境"""
        self.temp_dir.cleanup()
    
    def test_agent_creation(self):
        """测试Agent创建"""
        self.assertEqual(self.agent.name, "dependencies")
        self.assertEqual(self.agent.role.role, "集成架构专家")
    
    def test_agent_execute(self):
        """测试Agent执行"""
        test_context = AgentContext(
            project_root=self.project_root,
            graph={"entities": {}, "relationships": []},
            output_dir="/test/output"
        )
        
        result = self.agent.execute(test_context)
        
        self.assertTrue(result.success)
        self.assertIn("external_dependencies", result.data)
        self.assertIn("internal_dependencies", result.data)
        self.assertIn("dependency_boundaries", result.data)
        self.assertIn("dependency_issues", result.data)
        
        # 验证Python依赖
        external_deps = result.data["external_dependencies"]
        python_deps = [dep for dep in external_deps if dep["type"] == "python"]
        self.assertGreater(len(python_deps), 0)
        
        # 验证JavaScript依赖
        javascript_deps = [dep for dep in external_deps if dep["type"] == "javascript"]
        self.assertGreater(len(javascript_deps), 0)
        
        # 验证依赖数量统计
        external_count = result.metrics.get("external_dependencies_count")
        internal_count = result.metrics.get("internal_dependencies_count")
        self.assertGreater(external_count, 0)
        self.assertGreater(internal_count, 0)
    
    def test_extract_package_name(self):
        """测试提取Python包名"""
        # 测试各种版本约束格式
        self.assertEqual(self.agent._extract_package_name("flask==2.3.2"), "flask")
        self.assertEqual(self.agent._extract_package_name("django>=3.2"), "django")
        self.assertEqual(self.agent._extract_package_name("pandas~=1.5"), "pandas")
        self.assertEqual(self.agent._extract_package_name("requests"), "requests")
    
    def test_extract_version(self):
        """测试提取版本信息"""
        # 测试各种版本约束格式
        self.assertEqual(self.agent._extract_version("flask==2.3.2"), "2.3.2")
        self.assertEqual(self.agent._extract_version("django>=3.2"), "3.2")
        self.assertEqual(self.agent._extract_version("pandas~=1.5"), "1.5")
        self.assertEqual(self.agent._extract_version("requests"), "unknown")
    
    def test_classify_python_dependency(self):
        """测试分类Python依赖"""
        # 测试各种库的分类
        self.assertEqual(self.agent._classify_python_dependency("flask==2.3.2"), "web框架")
        self.assertEqual(self.agent._classify_python_dependency("django>=3.2"), "web框架")
        self.assertEqual(self.agent._classify_python_dependency("pandas~=1.5"), "数据处理")
        self.assertEqual(self.agent._classify_python_dependency("requests"), "网络通信")
        self.assertEqual(self.agent._classify_python_dependency("pytest==7.4.0"), "测试工具")
        self.assertEqual(self.agent._classify_python_dependency("black==23.9.0"), "代码格式化")
        self.assertEqual(self.agent._classify_python_dependency("unknown-library"), "通用库")
    
    def test_classify_javascript_dependency(self):
        """测试分类JavaScript依赖"""
        # 测试各种库的分类
        self.assertEqual(self.agent._classify_javascript_dependency("react"), "前端框架")
        self.assertEqual(self.agent._classify_javascript_dependency("express"), "后端框架")
        self.assertEqual(self.agent._classify_javascript_dependency("lodash"), "实用工具")
        self.assertEqual(self.agent._classify_javascript_dependency("webpack"), "构建工具")
        self.assertEqual(self.agent._classify_javascript_dependency("jest"), "测试工具")
        self.assertEqual(self.agent._classify_javascript_dependency("eslint"), "代码格式化")
        self.assertEqual(self.agent._classify_javascript_dependency("unknown-library"), "通用库")


class TestAgentIntegration(unittest.TestCase):
    """测试Agent集成"""
    
    def test_agent_compatibility(self):
        """测试Agent兼容性"""
        # 测试不同Agent之间的兼容性
        agents = [
            OverviewAgent(),
            LayersAgent(),
            DependenciesAgent()
        ]
        
        for agent in agents:
            self.assertIsInstance(agent, BaseAgent)
            self.assertIsNotNone(agent.name)
            self.assertIsNotNone(agent.role)
            self.assertIsNotNone(agent.logger)
    
    def test_role_configs(self):
        """测试角色配置"""
        from src.agents.config.agent_configs import OVERVIEW_AGENT, LAYERS_AGENT, DEPENDENCIES_AGENT
        
        # 验证OverviewAgent角色配置
        self.assertEqual(OVERVIEW_AGENT.name, "overview")
        self.assertEqual(OVERVIEW_AGENT.role, "首席系统架构师")
        self.assertIn("架构范式", OVERVIEW_AGENT.focus)
        
        # 验证LayersAgent角色配置
        self.assertEqual(LAYERS_AGENT.name, "layers")
        self.assertEqual(LAYERS_AGENT.role, "资深软件设计师")
        self.assertIn("职责分配体系", LAYERS_AGENT.focus)
        
        # 验证DependenciesAgent角色配置
        self.assertEqual(DEPENDENCIES_AGENT.name, "dependencies")
        self.assertEqual(DEPENDENCIES_AGENT.role, "集成架构专家")
        self.assertIn("外部集成拓扑", DEPENDENCIES_AGENT.focus)


if __name__ == '__main__':
    unittest.main()