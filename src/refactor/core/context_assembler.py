"""
上下文组装器 - 为重构分析组装LLM所需的上下文

从知识图谱和文档中提取相关信息，组装成适合LLM分析的上下文
"""

import logging
from typing import Dict, Any, List, Optional

from .refactoring_task import RefactoringContext, KnowledgeContext

logger = logging.getLogger(__name__)


class ContextAssembler:
    """上下文组装器 - 为重构分析组装LLM所需的上下文"""
    
    DOMAIN_KEYWORDS = {
        "用户管理": ["user", "auth", "login", "account", "用户", "登录"],
        "订单管理": ["order", "订单"],
        "支付管理": ["payment", "pay", "支付", "付款", "结算"],
        "数据分析": ["analysis", "report", "统计", "分析"],
        "数据采集": ["collect", "crawl", "fetch", "采集", "爬取"],
        "配置管理": ["config", "setting", "配置"],
        "任务调度": ["schedule", "task", "timer", "调度", "任务"],
        "交易核心": ["trade", "trading", "交易", "行情"],
        "股票分析": ["stock", "market", "股票", "指数"],
        "API接口": ["api", "action", "handler", "controller"],
    }
    
    def __init__(self, config: Dict[str, Any] = None):
        self.config = config or {}
        self.max_context_tokens = self.config.get("max_context_tokens", 100000)
    
    def assemble(
        self, 
        entity_id: str, 
        knowledge_context: KnowledgeContext,
        semantic_contract: Optional[Dict[str, Any]] = None
    ) -> RefactoringContext:
        """为指定实体组装重构上下文"""
        target_entity = knowledge_context.entities.get(entity_id)
        if not target_entity:
            return RefactoringContext(target_entity_id=entity_id)
        
        callers = self._get_callers(entity_id, knowledge_context)
        callees = self._get_callees(entity_id, knowledge_context)
        dependencies = self._get_dependencies(entity_id, knowledge_context)
        related_docs = self._get_related_docs(target_entity, knowledge_context)
        business_domain, domain_tags = self._infer_business_domain(target_entity)
        context_token_count = self._estimate_token_count(target_entity, callers, callees)
        
        return RefactoringContext(
            target_entity_id=entity_id,
            target_entity=target_entity,
            semantic_contract=semantic_contract,
            callers=callers[:10],
            callees=callees[:20],
            dependencies=dependencies,
            related_docs=related_docs[:5],
            business_domain=business_domain,
            domain_tags=domain_tags,
            context_token_count=context_token_count
        )
    
    def _get_callers(self, entity_id: str, context: KnowledgeContext) -> List[Dict[str, Any]]:
        """获取调用方列表"""
        callers = []
        for rel in context.relationships:
            if rel.get("target") == entity_id and rel.get("type") in ["calls", "CALLS"]:
                caller = context.entities.get(rel.get("source"))
                if caller:
                    callers.append({
                        "entity_id": rel.get("source"),
                        "name": caller.get("name", "Unknown"),
                        "file_path": caller.get("file_path", ""),
                    })
        return callers
    
    def _get_callees(self, entity_id: str, context: KnowledgeContext) -> List[Dict[str, Any]]:
        """获取被调用方列表"""
        callees = []
        for rel in context.relationships:
            if rel.get("source") == entity_id and rel.get("type") in ["calls", "CALLS"]:
                callee = context.entities.get(rel.get("target"))
                if callee:
                    callees.append({
                        "entity_id": rel.get("target"),
                        "name": callee.get("name", "Unknown"),
                        "file_path": callee.get("file_path", ""),
                    })
        return callees
    
    def _get_dependencies(self, entity_id: str, context: KnowledgeContext) -> List[Dict[str, Any]]:
        """获取依赖关系"""
        deps = []
        for rel in context.relationships:
            if rel.get("source") == entity_id and rel.get("type") in ["imports", "depends_on", "extends"]:
                dep = context.entities.get(rel.get("target"))
                if dep:
                    deps.append({
                        "entity_id": rel.get("target"),
                        "name": dep.get("name", "Unknown"),
                        "relation": rel.get("type"),
                    })
        return deps
    
    def _get_related_docs(self, entity: Dict[str, Any], context: KnowledgeContext) -> List[str]:
        """获取相关文档片段"""
        docs = []
        name = entity.get("name", "").lower()
        
        if context.project_overview:
            for line in context.project_overview.split('\n')[:100]:
                if name in line.lower():
                    docs.append(line.strip())
                    if len(docs) >= 5:
                        break
        return docs
    
    def _infer_business_domain(self, entity: Dict[str, Any]) -> tuple:
        """推断业务域"""
        name = entity.get("name", "").lower()
        path = entity.get("file_path", "").lower()
        
        for domain, keywords in self.DOMAIN_KEYWORDS.items():
            for kw in keywords:
                if kw in name or kw in path:
                    return domain, [domain]
        return "其他", []
    
    def _estimate_token_count(self, entity: Dict, callers: List, callees: List) -> int:
        """估算Token数量"""
        total = len(str(entity)) + sum(len(str(c)) for c in callers + callees)
        return total // 4
    
    def get_priority_entities(self, context: KnowledgeContext) -> List[str]:
        """获取优先分析的实体列表"""
        caller_count = {}
        for rel in context.relationships:
            if rel.get("type") in ["calls", "CALLS"]:
                target = rel.get("target")
                caller_count[target] = caller_count.get(target, 0) + 1
        
        sorted_entities = sorted(caller_count.items(), key=lambda x: x[1], reverse=True)
        return [e[0] for e in sorted_entities[:100]]
