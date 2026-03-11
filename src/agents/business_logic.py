"""
业务逻辑Agent - 负责业务逻辑识别与业务流分析

功能：
- 业务实体识别与分类
- 业务流程建模
- 业务规则提取
- 业务约束分析
- 业务价值评估
"""

from typing import List, Dict, Any, Optional
from collections import defaultdict
from .base_agent import BaseAgent
from src.models.business_semantic import BusinessFlow, BusinessEntity


class BusinessLogicAgent(BaseAgent):
    """业务逻辑Agent"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__("business_logic", config)
        self.business_domains = self._load_business_domains()
    
    async def _execute_impl(self, input_data: Any) -> List[BusinessFlow]:
        """执行业务逻辑分析"""
        if isinstance(input_data, dict):
            semantic_contracts = input_data.get("semantic_contracts", [])
            code_metadata_list = input_data.get("code_metadata_list", [])
        else:
            raise ValueError("输入数据应包含语义契约和代码元数据")
        
        self.logger.info(f"开始业务逻辑分析，处理 {len(semantic_contracts)} 个语义契约")
        
        # 提取所有业务实体
        all_entities = self._extract_all_entities(semantic_contracts)
        
        # 识别业务领域
        business_domains = self._identify_business_domains(all_entities)
        
        # 构建业务流
        business_flows = await self._build_business_flows(
            all_entities, business_domains, code_metadata_list
        )
        
        # 分析业务规则
        await self._analyze_business_rules(business_flows, semantic_contracts)
        
        self.logger.info(f"业务逻辑分析完成，识别 {len(business_flows)} 个业务流")
        return business_flows
    
    def _extract_all_entities(self, semantic_contracts: List[Any]) -> List[BusinessEntity]:
        """提取所有业务实体"""
        entities = []
        for contract in semantic_contracts:
            entities.extend(contract.entities)
        
        # 去重（基于实体名和类型）
        seen = set()
        unique_entities = []
        
        for entity in entities:
            key = (entity.name, entity.entity_type)
            if key not in seen:
                seen.add(key)
                unique_entities.append(entity)
        
        return unique_entities
    
    def _identify_business_domains(self, entities: List[BusinessEntity]) -> Dict[str, List[BusinessEntity]]:
        """识别业务领域"""
        domains = defaultdict(list)
        
        for entity in entities:
            domain = self._classify_business_domain(entity)
            domains[domain].append(entity)
        
        return dict(domains)
    
    def _classify_business_domain(self, entity: BusinessEntity) -> str:
        """分类业务领域"""
        entity_name = entity.name.lower()
        entity_type = entity.entity_type.lower()
        
        # 用户管理领域
        if any(keyword in entity_name for keyword in [
            'user', 'customer', 'client', 'member', 'person', 'account', 'profile'
        ]) or entity_type in ['user', 'customer']:
            return "user_management"
        
        # 订单交易领域
        if any(keyword in entity_name for keyword in [
            'order', 'transaction', 'payment', 'invoice', 'bill', 'purchase', 'sale'
        ]) or entity_type in ['transaction', 'order']:
            return "order_management"
        
        # 产品库存领域
        if any(keyword in entity_name for keyword in [
            'product', 'item', 'inventory', 'stock', 'catalog', 'goods', 'commodity'
        ]) or entity_type in ['product', 'inventory']:
            return "product_management"
        
        # 内容管理领域
        if any(keyword in entity_name for keyword in [
            'content', 'article', 'post', 'blog', 'news', 'media', 'file'
        ]) or entity_type in ['content', 'media']:
            return "content_management"
        
        # 数据分析领域
        if any(keyword in entity_name for keyword in [
            'report', 'analytics', 'statistic', 'metric', 'dashboard', 'analysis'
        ]) or entity_type in ['report', 'analytics']:
            return "data_analytics"
        
        # 系统管理领域
        if any(keyword in entity_name for keyword in [
            'system', 'config', 'setting', 'admin', 'permission', 'role'
        ]) or entity_type in ['system', 'admin']:
            return "system_management"
        
        return "general"
    
    async def _build_business_flows(self, 
                                  entities: List[BusinessEntity], 
                                  domains: Dict[str, List[BusinessEntity]],
                                  code_metadata_list: List[Any]) -> List[BusinessFlow]:
        """构建业务流"""
        business_flows = []
        
        # 为每个业务领域构建业务流
        for domain_name, domain_entities in domains.items():
            flow = await self._create_business_flow(domain_name, domain_entities, code_metadata_list)
            if flow:
                business_flows.append(flow)
        
        # 构建跨领域的业务流
        cross_domain_flows = await self._build_cross_domain_flows(entities, domains)
        business_flows.extend(cross_domain_flows)
        
        return business_flows
    
    async def _create_business_flow(self, 
                                   domain_name: str, 
                                   entities: List[BusinessEntity],
                                   code_metadata_list: List[Any]) -> Optional[BusinessFlow]:
        """创建业务流"""
        if not entities:
            return None
        
        # 分析实体关系
        entity_relationships = self._analyze_entity_relationships(entities, code_metadata_list)
        
        # 识别主要业务流程
        main_processes = self._identify_main_processes(entities, domain_name)
        
        # 构建业务流
        flow = BusinessFlow(
            flow_name=f"{domain_name}_flow",
            domain=domain_name,
            description=f"{domain_name.replace('_', ' ').title()} 业务流程",
            entities=entities,
            processes=main_processes,
            relationships=entity_relationships,
            complexity=self._assess_flow_complexity(entities, entity_relationships)
        )
        
        return flow
    
    def _analyze_entity_relationships(self, 
                                     entities: List[BusinessEntity], 
                                     code_metadata_list: List[Any]) -> Dict[str, List[str]]:
        """分析实体关系"""
        relationships = defaultdict(list)
        
        # 基于实体类型和上下文分析关系
        for i, entity1 in enumerate(entities):
            for j, entity2 in enumerate(entities):
                if i != j:
                    relationship = self._determine_relationship(entity1, entity2, code_metadata_list)
                    if relationship:
                        relationships[entity1.name].append(f"{relationship} -> {entity2.name}")
        
        return dict(relationships)
    
    def _determine_relationship(self, 
                              entity1: BusinessEntity, 
                              entity2: BusinessEntity,
                              code_metadata_list: List[Any]) -> Optional[str]:
        """确定实体关系"""
        # 基于实体类型和业务逻辑推断关系
        
        # 用户-订单关系
        if (entity1.entity_type == "user" and entity2.entity_type == "transaction") or \
           (entity2.entity_type == "user" and entity1.entity_type == "transaction"):
            return "creates"
        
        # 订单-产品关系
        if (entity1.entity_type == "transaction" and entity2.entity_type == "product") or \
           (entity2.entity_type == "transaction" and entity1.entity_type == "product"):
            return "contains"
        
        # 内容-用户关系
        if (entity1.entity_type == "content" and entity2.entity_type == "user") or \
           (entity2.entity_type == "content" and entity1.entity_type == "user"):
            return "created_by"
        
        # 基于名称相似性推断关系
        if entity1.name.lower() in entity2.name.lower() or entity2.name.lower() in entity1.name.lower():
            return "related_to"
        
        return None
    
    def _identify_main_processes(self, entities: List[BusinessEntity], domain_name: str) -> List[str]:
        """识别主要业务流程"""
        processes = []
        
        # 基于领域和实体类型识别流程
        if domain_name == "user_management":
            processes = ["用户注册", "用户登录", "用户信息更新", "权限管理"]
        elif domain_name == "order_management":
            processes = ["下单流程", "支付处理", "订单状态跟踪", "退款处理"]
        elif domain_name == "product_management":
            processes = ["产品上架", "库存管理", "价格调整", "产品分类"]
        elif domain_name == "content_management":
            processes = ["内容创建", "内容审核", "内容发布", "内容归档"]
        elif domain_name == "data_analytics":
            processes = ["数据收集", "数据处理", "报表生成", "趋势分析"]
        else:
            # 通用流程
            creation_entities = [e for e in entities if e.entity_type in ["creation", "query", "modification"]]
            if creation_entities:
                processes.append("数据创建流程")
            if any(e.entity_type == "validation" for e in entities):
                processes.append("数据验证流程")
        
        return processes
    
    def _assess_flow_complexity(self, entities: List[BusinessEntity], relationships: Dict[str, List[str]]) -> str:
        """评估业务流复杂度"""
        entity_count = len(entities)
        relationship_count = sum(len(rels) for rels in relationships.values())
        
        complexity_score = entity_count + relationship_count / 2
        
        if complexity_score < 5:
            return "simple"
        elif complexity_score < 15:
            return "medium"
        else:
            return "complex"
    
    async def _build_cross_domain_flows(self, 
                                      entities: List[BusinessEntity], 
                                      domains: Dict[str, List[BusinessEntity]]) -> List[BusinessFlow]:
        """构建跨领域业务流"""
        cross_flows = []
        
        # 用户下单流程（用户管理 + 订单管理）
        if "user_management" in domains and "order_management" in domains:
            user_entities = domains["user_management"]
            order_entities = domains["order_management"]
            
            cross_flow = BusinessFlow(
                flow_name="user_order_flow",
                domain="cross_domain",
                description="用户下单跨领域业务流程",
                entities=user_entities + order_entities,
                processes=["用户认证", "商品浏览", "下单支付", "订单处理"],
                relationships={
                    "用户": ["创建订单", "支付订单"],
                    "订单": ["属于用户", "包含商品"]
                },
                complexity="complex"
            )
            cross_flows.append(cross_flow)
        
        # 内容发布流程（内容管理 + 用户管理）
        if "content_management" in domains and "user_management" in domains:
            content_entities = domains["content_management"]
            user_entities = domains["user_management"]
            
            cross_flow = BusinessFlow(
                flow_name="content_publishing_flow",
                domain="cross_domain",
                description="内容发布跨领域业务流程",
                entities=content_entities + user_entities,
                processes=["内容创建", "内容审核", "内容发布", "用户通知"],
                relationships={
                    "内容": ["由用户创建", "需要审核"],
                    "用户": ["创建内容", "接收通知"]
                },
                complexity="medium"
            )
            cross_flows.append(cross_flow)
        
        return cross_flows
    
    async def _analyze_business_rules(self, business_flows: List[BusinessFlow], semantic_contracts: List[Any]):
        """分析业务规则"""
        for flow in business_flows:
            # 从语义契约中提取业务规则
            flow.business_rules = self._extract_business_rules_for_flow(flow, semantic_contracts)
            
            # 分析业务约束
            flow.constraints = self._analyze_business_constraints(flow)
    
    def _extract_business_rules_for_flow(self, flow: BusinessFlow, semantic_contracts: List[Any]) -> List[str]:
        """为业务流提取业务规则"""
        rules = []
        
        # 从相关语义契约中提取规则
        for contract in semantic_contracts:
            if contract.business_rules:
                # 检查规则是否与当前业务流相关
                relevant_rules = [
                    rule for rule in contract.business_rules
                    if self._is_rule_relevant_to_flow(rule, flow)
                ]
                rules.extend(relevant_rules)
        
        return list(set(rules))
    
    def _is_rule_relevant_to_flow(self, rule: str, flow: BusinessFlow) -> bool:
        """检查规则是否与业务流相关"""
        rule_lower = rule.lower()
        
        # 检查规则是否包含业务流中的实体或流程
        for entity in flow.entities:
            if entity.name.lower() in rule_lower:
                return True
        
        for process in flow.processes:
            if process.lower() in rule_lower:
                return True
        
        return False
    
    def _analyze_business_constraints(self, flow: BusinessFlow) -> Dict[str, Any]:
        """分析业务约束"""
        constraints = {}
        
        # 基于业务流复杂度分析约束
        if flow.complexity == "complex":
            constraints["performance"] = "需要优化性能，支持高并发"
            constraints["scalability"] = "需要考虑水平扩展"
        
        # 基于领域分析约束
        if flow.domain == "order_management":
            constraints["transaction"] = "需要保证事务一致性"
            constraints["security"] = "需要严格的安全控制"
        
        if flow.domain == "user_management":
            constraints["privacy"] = "需要保护用户隐私数据"
            constraints["compliance"] = "需要符合相关法规要求"
        
        return constraints
    
    def _load_business_domains(self) -> Dict[str, Any]:
        """加载业务领域配置"""
        return {
            "user_management": {
                "description": "用户管理领域",
                "key_entities": ["user", "customer", "account"],
                "typical_flows": ["registration", "authentication", "profile_management"]
            },
            "order_management": {
                "description": "订单交易领域", 
                "key_entities": ["order", "transaction", "payment"],
                "typical_flows": ["order_creation", "payment_processing", "order_tracking"]
            },
            "product_management": {
                "description": "产品库存领域",
                "key_entities": ["product", "inventory", "catalog"],
                "typical_flows": ["product_listing", "inventory_management", "pricing"]
            },
            "content_management": {
                "description": "内容管理领域",
                "key_entities": ["content", "article", "media"],
                "typical_flows": ["content_creation", "content_review", "content_publishing"]
            },
            "data_analytics": {
                "description": "数据分析领域",
                "key_entities": ["report", "analytics", "metric"],
                "typical_flows": ["data_collection", "data_processing", "report_generation"]
            }
        }