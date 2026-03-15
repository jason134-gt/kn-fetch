"""
架构分析相关的Agent角色
"""

from .overview_agent import OverviewAgent
from .layers_agent import LayersAgent
from .dependencies_agent import DependenciesAgent

# 导出的Agent列表
ARCHITECTURE_AGENTS = [
    OverviewAgent,
    LayersAgent,
    DependenciesAgent
]

# 方便的函数获取所有架构Agent
def get_all_architecture_agents():
    """获取所有架构分析Agent"""
    return ARCHITECTURE_AGENTS