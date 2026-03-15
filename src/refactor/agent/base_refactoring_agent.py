"""
重构分析Agent基类

提供重构分析的通用Agent基类，复用知识提取智能体的输出
"""

import logging
from typing import Any, Dict, List, Optional
from abc import abstractmethod

from src.extract.agents.base_agent import BaseAgent, AgentResult
from ..core.refactoring_task import KnowledgeContext, RiskAssessment, RiskLevel
from src.extract.ai.llm_client import LLMClient

logger = logging.getLogger(__name__)


class BaseRefactoringAgent(BaseAgent):
    """重构分析Agent基类 - 复用知识提取输出"""
    
    def __init__(
        self,
        name: str,
        config: Dict[str, Any],
        llm_client: Optional[LLMClient] = None
    ):
        super().__init__(name, config)
        self.llm_client = llm_client
        self._knowledge_context: Optional[KnowledgeContext] = None
    
    def set_knowledge_context(self, context: KnowledgeContext):
        """设置知识上下文"""
        self._knowledge_context = context
    
    def get_entity(self, entity_id: str) -> Optional[Dict]:
        """获取实体"""
        if self._knowledge_context:
            return self._knowledge_context.entities.get(entity_id)
        return None
    
    def get_relationships(self, entity_id: str, relation_type: str = None) -> List[Dict]:
        """获取关系"""
        if not self._knowledge_context:
            return []
        
        relationships = []
        for rel in self._knowledge_context.relationships:
            if relation_type is None or rel.get("type") == relation_type:
                if rel.get("source") == entity_id:
                    relationships.append(rel)
                if rel.get("target") == entity_id:
                    relationships.append(rel)
        
        return relationships
    
    @abstractmethod
    async def _execute_impl(self, input_data: Any) -> Any:
        """子类实现具体逻辑"""
        pass
