"""Agent配置模块"""

from .agent_configs import (
    AGENT_ROLES,
    REPORT_TYPE_AGENTS,
    EXPLORATION_TO_AGENT,
    get_agent_role,
    get_agents_by_report_type,
    get_agent_for_exploration,
    list_all_agents,
    get_agent_info
)

__all__ = [
    'AGENT_ROLES',
    'REPORT_TYPE_AGENTS',
    'EXPLORATION_TO_AGENT',
    'get_agent_role',
    'get_agents_by_report_type',
    'get_agent_for_exploration',
    'list_all_agents',
    'get_agent_info'
]
