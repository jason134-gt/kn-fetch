"""
业务语义逆向提取Agent - 核心层
实现：业务语义摘要、业务规则契约提取、业务领域标签、语义向量生成
"""
import json
import logging
import re
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass, field
from collections import Counter
import hashlib

from ..gitnexus.models import CodeEntity, EntityType, KnowledgeGraph
from ..ai.llm_client import LLMClient

logger = logging.getLogger(__name__)


@dataclass
class InputContract:
    """输入契约"""
    param_name: str
    business_meaning: str
    data_type: str
    constraints: List[str] = field(default_factory=list)
    required: bool = True


@dataclass
class OutputContract:
    """输出契约"""
    business_meaning: str
    data_type: str
    data_structure: Optional[str] = None
    examples: List[Any] = field(default_factory=list)


@dataclass
class EnhancedBusinessSemantic:
    """增强版业务语义模型"""
    entity_id: str
    entity_name: str
    file_path: str
    
    # 业务语义
    business_summary: str  # 用1-2句话描述核心业务功能
    business_domain: str  # 业务域
    
    # 契约
    input_contracts: List[InputContract] = field(default_factory=list)
    output_contract: Optional[OutputContract] = None
    
    # 副作用
    side_effects: List[str] = field(default_factory=list)  # 如：写数据库表order, 调用外部支付接口
    
    # 业务规则
    business_rules: List[str] = field(default_factory=list)
    
    # 异常场景
    exception_scenarios: List[str] = field(default_factory=list)
    
    # 边界条件
    boundary_conditions: List[str] = field(default_factory=list)
    
    # 业务标签
    business_domain_tags: List[str] = field(default_factory=list)
    
    # 语义向量（用于相似度检索）
    semantic_vector: Optional[List[float]] = None
    
    # 置信度
    confidence: float = 1.0
    
    # 是否需要人工确认
    needs_review: bool = False
    
    # 元数据
    metadata: Dict[str, Any] = field(default_factory=dict)


class SemanticExtractor:
    """业务语义逆向提取Agent"""
    
    # 业务域关键词映射
    BUSINESS_DOMAIN_KEYWORDS = {
        "用户管理": ["user", "auth", "login", "account", "register", "profile", "用户", "登录", "注册", "权限"],
        "订单管理": ["order", "订单", "购买", "下单", "checkout", "cart", "购物车"],
        "支付管理": ["payment", "pay", "transaction", "支付", "付款", "交易", "结算", "settlement"],
        "商品管理": ["product", "item", "goods", "inventory", "商品", "库存", "货品"],
        "数据分析": ["analytics", "analysis", "report", "statistics", "统计", "分析", "报表", "dashboard"],
        "数据采集": ["collect", "crawl", "fetch", "scrape", "采集", "爬取", "抓取", "extract"],
        "配置管理": ["config", "setting", "preference", "配置", "设置", "参数"],
        "任务调度": ["schedule", "task", "job", "cron", "调度", "任务", "定时"],
        "消息通信": ["message", "event", "notify", "notification", "消息", "通知", "事件", "push"],
        "数据处理": ["process", "transform", "etl", "pipeline", "处理", "转换", "清洗"],
        "API接口": ["api", "endpoint", "route", "controller", "handler", "rest", "graphql"],
        "文件管理": ["file", "upload", "download", "storage", "文件", "上传", "下载", "存储"],
        "搜索引擎": ["search", "query", "index", "检索", "搜索", "查询", "索引"],
        "日志监控": ["log", "monitor", "trace", "metric", "日志", "监控", "追踪", "指标"],
        "安全认证": ["security", "auth", "token", "jwt", "oauth", "安全", "认证", "授权"],
        "缓存管理": ["cache", "redis", "memcache", "缓存"],
        "数据库": ["database", "db", "sql", "dao", "repository", "数据库", "持久化"],
    }
    
    # 副作用关键词
    SIDE_EFFECT_KEYWORDS = {
        "数据库写操作": ["insert", "update", "delete", "save", "create", "write", "写入", "保存", "删除", "更新"],
        "数据库读操作": ["select", "query", "find", "get", "read", "查询", "读取", "获取"],
        "消息发送": ["send", "emit", "publish", "push", "notify", "发送", "发布", "推送", "通知"],
        "外部API调用": ["http", "request", "fetch", "call", "invoke", "调用", "请求", "接口"],
        "文件操作": ["write_file", "read_file", "create_file", "文件操作", "保存文件"],
        "缓存操作": ["cache", "redis", "set", "get", "缓存"],
    }
    
    # 业务规则关键词
    BUSINESS_RULE_KEYWORDS = [
        "必须", "应该", "需要", "要求", "规则", "限制", "如果", "当", "验证",
        "限制为", "不能超过", "必须大于", "范围", "有效期", "有效期至",
        "only if", "must", "should", "required", "validate", "constraint",
        "if", "when", "condition", "rule", "limit", "range"
    ]
    
    # 异常场景关键词
    EXCEPTION_KEYWORDS = [
        "异常", "错误", "失败", "超时", "不存在", "已存在", "无效", "过期",
        "exception", "error", "fail", "timeout", "not found", "invalid", "expired"
    ]
    
    def __init__(self, config: Dict[str, Any] = None):
        self.config = config or {}
        self.llm = LLMClient(self.config.get('ai', {})) if self.config else None
        
        # 缓存
        self._semantic_cache: Dict[str, EnhancedBusinessSemantic] = {}
    
    def extract_semantic(self, entity: CodeEntity, graph: KnowledgeGraph = None) -> EnhancedBusinessSemantic:
        """提取单个实体的业务语义"""
        # 检查缓存
        if entity.id in self._semantic_cache:
            return self._semantic_cache[entity.id]
        
        semantic = EnhancedBusinessSemantic(
            entity_id=entity.id,
            entity_name=entity.name,
            file_path=entity.file_path,
            business_summary="",
            business_domain=self._infer_business_domain(entity),
            business_domain_tags=self._extract_business_tags(entity)
        )
        
        # 1. 提取业务摘要
        semantic.business_summary = self._extract_business_summary(entity)
        
        # 2. 提取输入契约
        semantic.input_contracts = self._extract_input_contracts(entity)
        
        # 3. 提取输出契约
        semantic.output_contract = self._extract_output_contract(entity)
        
        # 4. 提取副作用
        semantic.side_effects = self._extract_side_effects(entity, graph)
        
        # 5. 提取业务规则
        semantic.business_rules = self._extract_business_rules(entity)
        
        # 6. 提取异常场景
        semantic.exception_scenarios = self._extract_exception_scenarios(entity)
        
        # 7. 提取边界条件
        semantic.boundary_conditions = self._extract_boundary_conditions(entity)
        
        # 缓存
        self._semantic_cache[entity.id] = semantic
        return semantic
    
    def extract_semantics_batch(
        self, 
        entities: List[CodeEntity], 
        graph: KnowledgeGraph = None,
        use_llm: bool = True,
        max_llm_calls: int = 50
    ) -> Dict[str, EnhancedBusinessSemantic]:
        """批量提取业务语义"""
        results = {}
        
        # 先用规则提取
        for entity in entities:
            semantic = self.extract_semantic(entity, graph)
            results[entity.id] = semantic
        
        # 如果LLM可用，增强核心实体
        if use_llm and self.llm and self.llm.is_available():
            # 选择需要LLM增强的实体（优先级：核心业务 > 公共API > 复杂逻辑）
            entities_to_enhance = self._select_entities_for_llm_enhancement(entities, results)
            entities_to_enhance = entities_to_enhance[:max_llm_calls]
            
            for entity in entities_to_enhance:
                try:
                    enhanced_semantic = self._enhance_with_llm(entity, results[entity.id])
                    results[entity.id] = enhanced_semantic
                except Exception as e:
                    logger.warning(f"LLM增强失败 {entity.name}: {e}")
        
        return results
    
    def _infer_business_domain(self, entity: CodeEntity) -> str:
        """推断业务域"""
        name_lower = entity.name.lower()
        file_lower = entity.file_path.lower()
        doc_lower = (entity.docstring or "").lower()
        
        for domain, keywords in self.BUSINESS_DOMAIN_KEYWORDS.items():
            for kw in keywords:
                if kw in name_lower or kw in file_lower or kw in doc_lower:
                    return domain
        
        return "其他"
    
    def _extract_business_tags(self, entity: CodeEntity) -> List[str]:
        """提取业务标签"""
        tags = set()
        
        text = f"{entity.name} {entity.file_path} {entity.docstring or ''}".lower()
        
        for domain, keywords in self.BUSINESS_DOMAIN_KEYWORDS.items():
            for kw in keywords:
                if kw in text:
                    tags.add(domain)
        
        # 从装饰器提取
        for dec in entity.decorators:
            if any(kw in dec.lower() for kw in ["api", "route", "get", "post", "put", "delete"]):
                tags.add("API接口")
            if any(kw in dec.lower() for kw in ["task", "celery", "job"]):
                tags.add("任务调度")
            if any(kw in dec.lower() for kw in ["event", "listener", "handler"]):
                tags.add("事件处理")
        
        return list(tags)[:5]  # 限制标签数量
    
    def _extract_business_summary(self, entity: CodeEntity) -> str:
        """提取业务摘要"""
        # 优先使用docstring的第一行
        if entity.docstring:
            lines = entity.docstring.strip().split('\n')
            first_line = lines[0].strip()
            if len(first_line) > 10:
                return first_line
        
        # 从函数名推断
        name = entity.name
        if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD]:
            # 常见动词模式
            if name.startswith("get_") or name.startswith("fetch_"):
                return f"获取{name[4:] if name.startswith('get_') else name[6:]}"
            elif name.startswith("create_") or name.startswith("add_"):
                return f"创建{name[7:] if name.startswith('create_') else name[4:]}"
            elif name.startswith("update_") or name.startswith("modify_"):
                return f"更新{name[7:] if name.startswith('update_') else name[7:]}"
            elif name.startswith("delete_") or name.startswith("remove_"):
                return f"删除{name[7:] if name.startswith('delete_') else name[7:]}"
            elif name.startswith("process_") or name.startswith("handle_"):
                return f"处理{name[8:] if name.startswith('process_') else name[7:]}"
            elif name.startswith("validate_") or name.startswith("check_"):
                return f"验证{name[9:] if name.startswith('validate_') else name[6:]}"
            elif name.startswith("calculate_") or name.startswith("compute_"):
                return f"计算{name[10:] if name.startswith('calculate_') else name[8:]}"
        
        return f"{entity.entity_type.value}: {entity.name}"
    
    def _extract_input_contracts(self, entity: CodeEntity) -> List[InputContract]:
        """提取输入契约"""
        contracts = []
        
        if not entity.parameters:
            return contracts
        
        for param in entity.parameters:
            name = param.get("name", "")
            if name in ["self", "cls"]:
                continue
            
            annotation = param.get("annotation", "Any")
            default = param.get("default")
            
            contract = InputContract(
                param_name=name,
                business_meaning=self._infer_param_meaning(name),
                data_type=annotation or "Any",
                constraints=self._infer_param_constraints(name, annotation, entity.docstring),
                required=default is None
            )
            contracts.append(contract)
        
        return contracts
    
    def _extract_output_contract(self, entity: CodeEntity) -> Optional[OutputContract]:
        """提取输出契约"""
        if entity.entity_type not in [EntityType.FUNCTION, EntityType.METHOD]:
            return None
        
        return_type = entity.return_type or "Any"
        
        return OutputContract(
            business_meaning=self._infer_return_meaning(entity.name, entity.docstring),
            data_type=return_type,
            data_structure=self._infer_data_structure(return_type, entity.docstring)
        )
    
    def _extract_side_effects(self, entity: CodeEntity, graph: KnowledgeGraph = None) -> List[str]:
        """提取副作用"""
        effects = []
        
        # 从名称推断
        name_lower = entity.name.lower()
        for effect_type, keywords in self.SIDE_EFFECT_KEYWORDS.items():
            for kw in keywords:
                if kw in name_lower:
                    effects.append(f"可能的{effect_type}")
                    break
        
        # 从调用关系推断
        if graph:
            for rel in graph.relationships:
                if rel.source_id == entity.id:
                    target = graph.entities.get(rel.target_id)
                    if target:
                        target_name = target.name.lower()
                        for effect_type, keywords in self.SIDE_EFFECT_KEYWORDS.items():
                            for kw in keywords:
                                if kw in target_name:
                                    effect = f"调用 {target.name}（{effect_type}）"
                                    if effect not in effects:
                                        effects.append(effect)
                                    break
        
        # 从文档推断
        if entity.docstring:
            doc_lower = entity.docstring.lower()
            if "note:" in doc_lower or "warning:" in doc_lower:
                effects.append("查看文档中的注意事项")
        
        return effects[:5]  # 限制数量
    
    def _extract_business_rules(self, entity: CodeEntity) -> List[str]:
        """提取业务规则"""
        rules = []
        
        if not entity.docstring:
            return rules
        
        lines = entity.docstring.split('\n')
        
        for line in lines:
            line = line.strip()
            if not line:
                continue
            
            # 检查是否包含业务规则关键词
            for kw in self.BUSINESS_RULE_KEYWORDS:
                if kw in line.lower():
                    rules.append(line)
                    break
        
        # 从Args和Raises部分提取
        in_args = False
        in_raises = False
        for line in lines:
            line_lower = line.lower().strip()
            if line_lower.startswith("args:"):
                in_args = True
            elif line_lower.startswith("raises:"):
                in_raises = True
                in_args = False
            elif line_lower.startswith(("returns:", "example:", "note:")):
                in_args = False
                in_raises = False
            elif in_args and ":" in line:
                # 参数约束
                rules.append(f"参数约束: {line.strip()}")
        
        return rules[:10]
    
    def _extract_exception_scenarios(self, entity: CodeEntity) -> List[str]:
        """提取异常场景"""
        scenarios = []
        
        # 从raises列表提取
        for exc in entity.raises:
            scenarios.append(f"可能抛出: {exc}")
        
        # 从文档提取
        if entity.docstring:
            lines = entity.docstring.split('\n')
            in_raises = False
            for line in lines:
                line_lower = line.lower().strip()
                if line_lower.startswith("raises:"):
                    in_raises = True
                elif line_lower.startswith(("returns:", "example:", "note:")):
                    in_raises = False
                elif in_raises and line.strip():
                    scenarios.append(line.strip())
        
        # 从名称推断
        name_lower = entity.name.lower()
        for kw in self.EXCEPTION_KEYWORDS:
            if kw in name_lower:
                scenarios.append(f"处理场景: {kw}")
        
        return scenarios[:5]
    
    def _extract_boundary_conditions(self, entity: CodeEntity) -> List[str]:
        """提取边界条件"""
        conditions = []
        
        if not entity.docstring:
            return conditions
        
        lines = entity.docstring.split('\n')
        
        boundary_keywords = [
            "边界", "条件", "限制", "范围", "最大", "最小", "最大值", "最小值",
            "boundary", "condition", "limit", "range", "max", "min", "maximum", "minimum",
            "超过", "低于", "大于", "小于", "等于", "exceed", "below", "above"
        ]
        
        for line in lines:
            line_lower = line.lower()
            for kw in boundary_keywords:
                if kw in line_lower:
                    conditions.append(line.strip())
                    break
        
        return conditions[:5]
    
    def _infer_param_meaning(self, param_name: str) -> str:
        """推断参数含义"""
        # 常见参数名映射
        common_meanings = {
            "id": "唯一标识符",
            "user_id": "用户ID",
            "order_id": "订单ID",
            "name": "名称",
            "title": "标题",
            "content": "内容",
            "data": "数据",
            "config": "配置",
            "options": "选项",
            "params": "参数",
            "query": "查询条件",
            "page": "页码",
            "size": "大小/数量",
            "limit": "限制数量",
            "offset": "偏移量",
            "start": "开始位置/时间",
            "end": "结束位置/时间",
            "callback": "回调函数",
            "handler": "处理函数",
        }
        
        param_lower = param_name.lower()
        return common_meanings.get(param_lower, param_name)
    
    def _infer_param_constraints(
        self, 
        param_name: str, 
        annotation: str, 
        docstring: str
    ) -> List[str]:
        """推断参数约束"""
        constraints = []
        
        # 类型约束
        if annotation:
            if "int" in annotation.lower():
                constraints.append("整数类型")
            elif "str" in annotation.lower():
                constraints.append("字符串类型")
            elif "bool" in annotation.lower():
                constraints.append("布尔类型")
            elif "list" in annotation.lower():
                constraints.append("列表类型")
            elif "dict" in annotation.lower():
                constraints.append("字典类型")
        
        # 从文档推断
        if docstring:
            # 查找参数相关描述
            lines = docstring.split('\n')
            in_args = False
            for line in lines:
                line_lower = line.lower().strip()
                if line_lower.startswith("args:"):
                    in_args = True
                elif line_lower.startswith(("returns:", "raises:", "example:")):
                    in_args = False
                elif in_args and param_name in line:
                    # 提取约束信息
                    if "必须" in line or "must" in line.lower():
                        constraints.append(line.strip())
        
        return constraints
    
    def _infer_return_meaning(self, name: str, docstring: str) -> str:
        """推断返回值含义"""
        if docstring:
            lines = docstring.split('\n')
            in_returns = False
            for line in lines:
                line_lower = line.lower().strip()
                if line_lower.startswith("returns:"):
                    in_returns = True
                elif line_lower.startswith(("args:", "raises:", "example:")):
                    in_returns = False
                elif in_returns and line.strip():
                    return line.strip()
        
        # 从名称推断
        name_lower = name.lower()
        if name_lower.startswith(("get_", "fetch_", "find_", "query_")):
            return "查询结果"
        elif name_lower.startswith(("create_", "add_", "insert_")):
            return "创建结果/新资源ID"
        elif name_lower.startswith(("update_", "modify_")):
            return "更新结果"
        elif name_lower.startswith(("delete_", "remove_")):
            return "删除结果"
        elif name_lower.startswith(("validate_", "check_")):
            return "验证结果"
        
        return "返回值"
    
    def _infer_data_structure(self, return_type: str, docstring: str) -> Optional[str]:
        """推断数据结构"""
        if "list" in return_type.lower():
            return "列表"
        elif "dict" in return_type.lower():
            return "字典"
        elif "tuple" in return_type.lower():
            return "元组"
        elif "bool" in return_type.lower():
            return "布尔值"
        
        return None
    
    def _select_entities_for_llm_enhancement(
        self, 
        entities: List[CodeEntity],
        semantics: Dict[str, EnhancedBusinessSemantic]
    ) -> List[CodeEntity]:
        """选择需要LLM增强的实体"""
        scored_entities = []
        
        for entity in entities:
            if entity.entity_type not in [EntityType.CLASS, EntityType.FUNCTION, EntityType.METHOD]:
                continue
            
            score = 0
            
            # 有文档的优先
            if entity.docstring and len(entity.docstring) > 50:
                score += 20
            
            # 公共API优先
            if entity.visibility and entity.visibility.value == "public":
                score += 15
            
            # 复杂度高的优先
            if entity.complexity and entity.complexity > 10:
                score += 10
            
            # 核心业务域优先
            semantic = semantics.get(entity.id)
            if semantic and semantic.business_domain != "其他":
                score += 15
            
            # 代码行数多的优先
            if entity.lines_of_code > 30:
                score += 5
            
            scored_entities.append((entity, score))
        
        # 按分数排序
        scored_entities.sort(key=lambda x: x[1], reverse=True)
        return [e for e, _ in scored_entities]
    
    def _enhance_with_llm(
        self, 
        entity: CodeEntity, 
        base_semantic: EnhancedBusinessSemantic
    ) -> EnhancedBusinessSemantic:
        """使用LLM增强语义分析"""
        # 构建Prompt
        prompt = self._build_semantic_extraction_prompt(entity)
        
        try:
            result = self.llm.chat_sync(
                system_prompt="""你是一位资深业务架构师，现在需要分析代码的业务语义。
请严格按JSON格式输出，不要包含任何额外解释。
如果不确定，请在相应字段标注"待人工确认".""",
                user_prompt=prompt,
                max_tokens=2000,
                temperature=0.1
            )
            
            if result:
                # 解析JSON
                json_match = re.search(r'\{[\s\S]*\}', result)
                if json_match:
                    data = json.loads(json_match.group())
                    
                    # 更新语义
                    if data.get("business_summary"):
                        base_semantic.business_summary = data["business_summary"]
                    
                    if data.get("business_domain_tags"):
                        base_semantic.business_domain_tags = list(set(
                            base_semantic.business_domain_tags + data["business_domain_tags"]
                        ))[:5]
                    
                    if data.get("business_rules"):
                        base_semantic.business_rules = list(set(
                            base_semantic.business_rules + data["business_rules"]
                        ))
                    
                    if data.get("side_effects"):
                        base_semantic.side_effects = list(set(
                            base_semantic.side_effects + data["side_effects"]
                        ))[:5]
                    
                    if data.get("exception_scenarios"):
                        base_semantic.exception_scenarios = list(set(
                            base_semantic.exception_scenarios + data["exception_scenarios"]
                        ))[:5]
                    
                    # 标记置信度
                    base_semantic.confidence = 0.9
                    
                    # 检查是否需要人工确认
                    if "待人工确认" in str(data):
                        base_semantic.needs_review = True
        
        except Exception as e:
            logger.warning(f"LLM语义提取失败: {e}")
            base_semantic.needs_review = True
        
        return base_semantic
    
    def _build_semantic_extraction_prompt(self, entity: CodeEntity) -> str:
        """构建语义提取Prompt"""
        code_snippet = entity.content[:2000] if entity.content else ""
        
        return f"""请分析以下代码实体的业务语义，严格按JSON格式输出：

输入：
1. 实体名称：{entity.name}
2. 实体类型：{entity.entity_type.value}
3. 文件路径：{entity.file_path}
4. 文档注释：{entity.docstring or '无'}
5. 参数列表：{json.dumps(entity.parameters, ensure_ascii=False) if entity.parameters else '无'}
6. 返回类型：{entity.return_type or '未知'}
7. 代码片段：
```
{code_snippet[:1000]}
```

输出JSON结构：
{{
  "business_summary": "用1-2句话描述这段代码实现的核心业务功能，避免技术术语",
  "business_rules": ["业务规则1", "业务规则2"],
  "side_effects": ["副作用1：如写数据库表order", "副作用2：如调用外部支付接口"],
  "exception_scenarios": ["异常场景1", "异常场景2"],
  "business_domain_tags": ["支付", "订单", "库存"]
}}

注意：
1. 业务规则应该是非技术人员也能理解的规则
2. 副作用要具体指出操作了什么资源
3. 如果不确定，请标注"待人工确认"
"""
    
    def generate_semantic_vector(self, semantic: EnhancedBusinessSemantic) -> List[float]:
        """生成语义向量（简化版，实际应使用embedding模型）"""
        # 将语义信息拼接成文本
        text = f"{semantic.business_summary} {' '.join(semantic.business_rules)} {' '.join(semantic.side_effects)}"
        
        # 简单的词袋向量（实际应使用sentence-transformers等）
        words = text.lower().split()
        word_counts = Counter(words)
        
        # 哈希到固定维度向量
        vector = [0.0] * 128
        for word, count in word_counts.items():
            idx = int(hashlib.md5(word.encode()).hexdigest(), 16) % 128
            vector[idx] = min(count / 10.0, 1.0)
        
        return vector
    
    def export_to_json(self, semantics: Dict[str, EnhancedBusinessSemantic]) -> str:
        """导出为JSON"""
        def convert(obj):
            if hasattr(obj, '__dict__'):
                return {k: convert(v) for k, v in obj.__dict__.items()}
            elif isinstance(obj, list):
                return [convert(item) for item in obj]
            elif isinstance(obj, dict):
                return {k: convert(v) for k, v in obj.items()}
            return obj
        
        return json.dumps(convert(semantics), ensure_ascii=False, indent=2)
