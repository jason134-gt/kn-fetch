"""
测试Agent配置和角色定义
"""

import unittest
from src.agents.config.agent_configs import (
    OVERVIEW_AGENT, 
    LAYERS_AGENT, 
    DEPENDENCIES_AGENT,
    get_agent_role,
    get_agents_by_report_type,
    get_agent_for_exploration,
    list_all_agents,
    get_agent_info,
    AGENT_ROLES,
    REPORT_TYPE_AGENTS,
    EXPLORATION_TO_AGENT
)


class TestAgentConfigs(unittest.TestCase):
    """测试Agent配置模块"""
    
    def test_overview_agent_config(self):
        """测试OverviewAgent配置"""
        self.assertEqual(OVERVIEW_AGENT.name, "overview")
        self.assertEqual(OVERVIEW_AGENT.role, "首席系统架构师")
        self.assertIn("架构范式", OVERVIEW_AGENT.focus)
        self.assertIn("避免罗列目录结构", OVERVIEW_AGENT.constraint)
        self.assertEqual(OVERVIEW_AGENT.output_template, "templates/sections/overview.md")
    
    def test_layers_agent_config(self):
        """测试LayersAgent配置"""
        self.assertEqual(LAYERS_AGENT.name, "layers")
        self.assertEqual(LAYERS_AGENT.role, "资深软件设计师")
        self.assertIn("职责分配体系", LAYERS_AGENT.focus)
        self.assertIn("不要列举具体的文件名列表", LAYERS_AGENT.constraint)
        self.assertEqual(LAYERS_AGENT.output_template, "templates/sections/layers.md")
    
    def test_dependencies_agent_config(self):
        """测试DependenciesAgent配置"""
        self.assertEqual(DEPENDENCIES_AGENT.name, "dependencies")
        self.assertEqual(DEPENDENCIES_AGENT.1 role, "集成架构专家")
        self.assertIn("外部集成拓扑", DEPENDENCIES_AGENT.focus)
        self.assertIn("禁止简单列出依赖配置文件的内容", DEPENDENCIES_AGENT.constraint)
        self.assertEqual(DEPENDENCIES_AGENT.output_template, "templates/sections/dependencies.md")
    
    def test_get_agent_role(self):
        """测试获取Agent角色"""
        role = get_agent_role("overview")
        self.assertEqual(role.name, "overview")
        
        role = get_agent_role("layers")
        self.assertEqual(role.name, "layers")
        
        role = get_agent_role("dependencies")
        self.assertEqual(role.name, "dependencies")
    
    def test_get_agent_role_unknown(self):
        """测试获取未知Agent角色"""
        with self.assertRaises(ValueError):
            get_agent_role("unknown_agent")
    
    def test_get_agents_by_report_type(self):
        """测试根据报告类型获取Agent"""
        architecture_agents = get_agents_by_report_type("architecture")
        self.assertEqual(len(architecture_agents), 5)
        self.assertIn("overview", architecture_agents)
        self.assertIn("layers", architecture_agents)
        self.assertIn("dependencies", architecture_agents)
        
        design_agents = get_agents_by_report_type("design")
        self.assertEqual(len(design_agents), 4)
        self.assertIn("patterns", design_agents)
        self.assertIn("classes", design_agents)
        
        methods_agents = get_agents_by_report_type("methods")
        self.assertEqual(len(methods_agents), 4)
        self.assertIn("algorithms", methods_agents)
        self.assertIn("paths", methods_agents)
        
        comprehensive_agents = get_agents_by_report_type("comprehensive")
        self.assertEqual(len(comprehensive_agents), 13)
    
    def test_get_agents_by_report_type_unknown(self):
        """测试获取未知报告类型的Agent"""
        with self.assertRaises(ValueError):
            get_agents_by_report_type("unknown_type")
    
    def test_get_agent_for_exploration(self):
        """测试根据探索角度获取Agent"""
        agent_name = get_agent_for_exploration("layer-structure")
        self.assertEqual(agent_name, "layers")
        
        agent_name = get_agent_for_exploration("module-dependencies")
        self.assertEqual(agent_name, "dependencies")
        
        agent_name = get_agent_for_exploration("architecture")
        self.assertEqual(agent_name, "overview")
    
    def test_get_agent_for_exploration_unknown(self):
        """测试获取未知探索角度的Agent"""
        agent_name = get_agent_for_exploration("unknown-angle")
        self.assertIsNone(agent1_name)
    
    def test_list_all_agents(self):
        """测试列出所有Agent"""
        agents = list_all_agents()
        self.assertEqual(len(agents), 13)
        self.assertIn("overview", agents)
        self.assertIn("layers", agents)
        self.assertIn("dependencies", agents)
        self.assertIn("patterns", agents)
        self.assertIn("classes", agents)
    
    def test_get_agent_info(self):
        """测试获取Agent详细信息"""
        info = get_agent_info("overview")
        self.assertEqual(info["name"], "overview")
        self.assertEqual(info["role"], "首席系统架构师")
        self.assertIn("架构范式", info["focus"])
        self.assertIn("避免罗列目录结构", info["constraint"])
        self.assertEqual(info["output_template"], "templates/sections/overview.md")
        
        info = get_agent_info("layers")
        self.assertEqual(info["name"], "layers")
        self.assertEqual(info["role"], "资深软件设计师")
        self.assertIn("职责分配体系", info["focus"])
        self.assertIn("不要列举具体的文件名列表", info["constraint"])
        self.assertEqual(info["output_template"], "templates/sections/layers.md")
    
    def test_agent_roles_mapping(self):
        """测试Agent角色映射"""
        self.assertEqual(len(AGENT_ROLES), 13)
        self.assertEqual(AGENT_ROLES["overview"], OVERVIEW_AGENT)
        self.assertEqual(AGENT_ROLES["layers"], LAYERS_AGENT)
        self.assertEqual(AGENT_ROLES["dependencies"], DEPENDENCIES_AGENT)
    
    def test_report_type_agents_mapping(self):
        """测试报告类型映射"""
        self.assertEqual(len(REPORT_TYPE_AGENTS), 4)
        self.assertEqual(len(REPORT_TYPE_AGENTS["architecture"]), 5)
        self.assertEqual(len(REPORT_TYPE_AGENTS["design"]), 4)
        self.assertEqual(len(REPORT_TYPE_AGENTS["methods"]), 4)
        self.assertEqual(len(REPORT_TYPE_AGENTS["comprehensive"]), 13)
    
    def test_exploration_to_agent_mapping(self):
        """测试探索角度映射"""
        self.assertEqual(len(EXPLORATION_TO_AGENT), 13)
        self.assertEqual(EXPLORATION_TO_AGENT["layer-structure"], "layers")
        self.assertEqual(EXPLORATION_TO_AGENT["module-dependencies"], "dependencies")
        self.assertEqual(EXPLORATION_TO_AGENT["architecture"], "overview")
        self.assertEqual(EXPLORATION_TO_AGENT["design-patterns"], "patterns")


if __name__ == '__main__':
    unittest.main()