"""
Agent协作框架

提供多Agent协作的知识提取能力，支持：
- 角色专业化：每个Agent专注一个分析领域
- 并行执行：多个Agent同时工作提升效率
- 消息传递：Agent间可交换信息
- 质量控制：统一的输出质量检查
"""

from .base.agent import BaseAgent, AgentRole
from .base.context import AgentContext
from .base.result import AgentResult
from .base.message import AgentMessage
from .orchestrator.orchestrator import AgentOrchestrator

__all__ = [
    'BaseAgent',
    'AgentRole',
    'AgentContext',
    'AgentResult',
    'AgentMessage',
    'AgentOrchestrator'
]
