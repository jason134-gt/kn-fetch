"""
LLM增强的语义理解模块
实现：GPT-4o/Claude 3集成、多模型交叉验证、业务规则精确提取
"""
import json
import logging
import re
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass, field
from concurrent.futures import ThreadPoolExecutor, as_completed
from collections import defaultdict, Counter

from ..ai.llm_client import LLMClient

# 定义EnhancedBusinessSemantic的简化版本，避免循环导入
@dataclass
class EnhancedBusinessSemantic:
    """增强的业务语义对象"""
    entity_id: str
    entity_name: str
    entity_type: str
    business_summary: str = ""
    business_domain: str = ""
    business_domain_tags: List[str] = field(default_factory=list)
    business_rules: List[str] = field(default_factory=list)
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "entity_id": self.entity_id,
            "entity_name": self.entity_name,
            "entity_type": self.entity_type,
            "business_summary": self.business_summary,
            "business_domain": self.business_domain,
            "business_domain_tags": self.business_domain_tags,
            "business_rules": self.business_rules
        }

logger = logging.getLogger(__name__)


@dataclass
class LLMSemanticResult:
    """LLM语义分析结果"""
    entity_id: str
    model_name: str
    raw_response: str
    parsed_data: Dict[str, Any]
    confidence: float
    error: Optional[str] = None
    

@dataclass
class CrossValidationResult:
    """交叉验证结果"""
    entity_id: str
    model_results: Dict[str, LLMSemanticResult]
    consensus_data: Dict[str, Any]
    agreement_score: float  # 0-1，模型间一致性分数
    needs_review: bool = False
    

class LLMEnhancedSemanticExtractor:
    """LLM增强的语义理解器"""
    
    def __init__(self, config: Dict[str, Any] = None):
        self.config = config or {}
        
        # 初始化LLM客户端
        self.llm_clients = self._init_llm_clients()
        
        # 配置
        self.max_workers = config.get("max_workers", 3)
        self.max_tokens = config.get("max_tokens", 2000)
        self.temperature = config.get("temperature", 0.1)
        
        # 缓存
        self._cache: Dict[str, CrossValidationResult] = {}
    
    def _init_llm_clients(self) -> Dict[str, LLMClient]:
        """初始化多个LLM客户端"""
        clients = {}
        
        ai_config = self.config.get('ai', {})
        
        # 主模型（优先使用）
        if ai_config.get('provider') in ['openai', 'volcengine', 'ark']:
            clients['primary'] = LLMClient(ai_config)
        
        # 备用模型配置
        backup_configs = {
            'deepseek': {'provider': 'deepseek', 'api_key': ai_config.get('deepseek_api_key')},
            'claude': {'provider': 'anthropic', 'api_key': ai_config.get('anthropic_api_key')}
        }
        
        for name, config in backup_configs.items():
            if config.get('api_key'):
                try:
                    clients[name] = LLMClient(config)
                except Exception as e:
                    logger.warning(f"初始化{name}客户端失败: {e}")
        
        return clients
    
    def extract_enhanced_semantics_batch(
        self, 
        entities: List[Any], 
        max_entities: int = 50
    ) -> Dict[str, CrossValidationResult]:
        """批量提取增强语义（多模型并行）"""
        results = {}
        
        # 过滤并排序实体（优先处理核心业务实体）
        prioritized_entities = self._prioritize_entities(entities)[:max_entities]
        
        # 并行处理
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            future_to_entity = {
                executor.submit(self.extract_single_entity_semantic, entity): entity
                for entity in prioritized_entities
            }
            
            for future in as_completed(future_to_entity):
                entity = future_to_entity[future]
                try:
                    result = future.result()
                    results[entity.id] = result
                    self._cache[entity.id] = result
                except Exception as e:
                    logger.error(f"处理实体 {entity.name} 失败: {e}")
        
        return results
    
    def extract_single_entity_semantic(self, entity: Any) -> CrossValidationResult:
        """提取单个实体的语义（多模型交叉验证）"""
        # 检查缓存
        if entity.id in self._cache:
            return self._cache[entity.id]
        
        model_results = {}
        
        # 并行调用多个模型
        with ThreadPoolExecutor(max_workers=min(3, len(self.llm_clients))) as executor:
            future_to_model = {
                executor.submit(self._call_single_model, model_name, entity): model_name
                for model_name in list(self.llm_clients.keys())[:3]  # 最多3个模型
            }
            
            for future in as_completed(future_to_model):
                model_name = future_to_model[future]
                try:
                    result = future.result()
                    model_results[model_name] = result
                except Exception as e:
                    logger.warning(f"模型 {model_name} 处理失败: {e}")
        
        # 交叉验证
        consensus_data, agreement_score = self._cross_validate(model_results)
        
        # 判断是否需要人工审核
        needs_review = (
            agreement_score < 0.7 or  # 模型间一致性差
            len(model_results) < 2 or  # 模型数量不足
            any('待人工确认' in str(result.parsed_data) for result in model_results.values())  # 模型不确定
        )
        
        return CrossValidationResult(
            entity_id=entity.id,
            model_results=model_results,
            consensus_data=consensus_data,
            agreement_score=agreement_score,
            needs_review=needs_review
        )
    
    def _call_single_model(self, model_name: str, entity: Any) -> LLMSemanticResult:
        """调用单个模型进行语义分析"""
        client = self.llm_clients[model_name]
        
        if not client.is_available():
            raise Exception(f"模型 {model_name} 不可用")
        
        # 构建Prompt
        prompt = self._build_semantic_prompt(entity)
        
        try:
            response = client.chat_sync(
                system_prompt=self._get_system_prompt(),
                user_prompt=prompt,
                max_tokens=self.max_tokens,
                temperature=self.temperature
            )
            
            # 解析响应
            parsed_data = self._parse_llm_response(response)
            
            return LLMSemanticResult(
                entity_id=entity.id,
                model_name=model_name,
                raw_response=response,
                parsed_data=parsed_data,
                confidence=self._calculate_confidence(parsed_data)
            )
            
        except Exception as e:
            return LLMSemanticResult(
                entity_id=entity.id,
                model_name=model_name,
                raw_response="",
                parsed_data={},
                confidence=0.0,
                error=str(e)
            )
    
    def _build_semantic_prompt(self, entity: Any) -> str:
        """构建语义分析Prompt"""
        code_snippet = entity.content[:1500] if entity.content else ""
        
        return f"""请分析以下代码实体的业务语义，严格按JSON格式输出：

输入信息：
1. 实体名称：{entity.name}
2. 实体类型：{entity.entity_type.value if hasattr(entity, 'entity_type') else 'unknown'}
3. 文件路径：{entity.file_path}
4. 文档注释：{entity.docstring or '无'}
5. 参数列表：{json.dumps(entity.parameters, ensure_ascii=False) if hasattr(entity, 'parameters') and entity.parameters else '无'}
6. 返回类型：{entity.return_type or '未知'}
7. 代码片段：
```
{code_snippet}
```

输出要求：
1. 必须使用JSON格式，不要包含任何额外解释
2. 如果不确定，请在相应字段标注"待人工确认"
3. 业务规则要具体明确，避免模糊描述
4. 副作用要详细指出操作了什么资源

输出JSON结构：
{{
  "business_summary": "用1-2句话描述核心业务功能，避免技术术语",
  "input_contract": [{{"param_name": "参数名", "business_meaning": "业务含义", "constraints": "约束条件"}}],
  "output_contract": {{"business_meaning": "输出含义", "data_structure": "数据结构"}},
  "side_effects": ["具体副作用1：如写数据库表order", "具体副作用2：如调用外部支付接口"],
  "business_rules": ["具体业务规则1：如订单金额必须大于0", "具体业务规则2：如用户等级需≥3才可享受折扣"],
  "exception_scenarios": ["异常场景1：如支付超时", "异常场景2：如库存不足"],
  "business_domain_tags": ["支付", "订单", "库存"],
  "confidence": 0.9
}}
"""
    
    def _get_system_prompt(self) -> str:
        """获取系统Prompt"""
        return """你是一位资深业务架构师，具备丰富的代码分析和业务理解能力。

请严格遵循以下要求：
1. 只分析代码的业务语义，不要关注技术实现细节
2. 用业务人员的语言描述功能，避免技术术语
3. 对不确定的内容标注"待人工确认"
4. 副作用描述要具体到资源级别（如"写user表"、"调用微信支付API"）
5. 业务规则要可验证、可测试

如果代码过于简单或信息不足，请如实反映，不要猜测。"""
    
    def _parse_llm_response(self, response: str) -> Dict[str, Any]:
        """解析LLM响应"""
        try:
            # 提取JSON部分
            json_match = re.search(r'\{[\s\S]*\}', response)
            if json_match:
                return json.loads(json_match.group())
            
            # 如果没有找到JSON，尝试直接解析
            lines = response.split('\n')
            json_start = None
            for i, line in enumerate(lines):
                if line.strip().startswith('{'):
                    json_start = i
                    break
            
            if json_start is not None:
                json_text = '\n'.join(lines[json_start:])
                return json.loads(json_text)
            
            # 如果还是失败，返回空结果
            return {"error": "无法解析JSON响应"}
            
        except Exception as e:
            return {"error": f"JSON解析失败: {e}"}
    
    def _calculate_confidence(self, parsed_data: Dict[str, Any]) -> float:
        """计算置信度"""
        if "error" in parsed_data:
            return 0.0
        
        confidence = 0.5  # 基础置信度
        
        # 检查关键字段完整性
        required_fields = ["business_summary", "business_rules", "side_effects"]
        for field in required_fields:
            if field in parsed_data and parsed_data[field]:
                confidence += 0.1
        
        # 检查是否有"待人工确认"
        data_str = str(parsed_data)
        if "待人工确认" in data_str:
            confidence *= 0.7
        
        return min(confidence, 1.0)
    
    def _cross_validate(self, model_results: Dict[str, LLMSemanticResult]) -> Tuple[Dict[str, Any], float]:
        """交叉验证多个模型的结果"""
        if len(model_results) < 2:
            # 只有一个模型结果，直接返回
            for result in model_results.values():
                return result.parsed_data, 0.5  # 单模型基础置信度
        
        # 提取各模型的关键字段
        summaries = []
        rules_sets = []
        side_effects_sets = []
        
        for result in model_results.values():
            data = result.parsed_data
            if "error" not in data:
                summaries.append(data.get("business_summary", ""))
                rules_sets.append(set(data.get("business_rules", [])))
                side_effects_sets.append(set(data.get("side_effects", [])))
        
        # 计算一致性分数
        agreement_scores = []
        
        # 业务摘要相似度
        if len(summaries) >= 2:
            # 简单的文本相似度计算（实际应使用embedding）
            score = self._text_similarity(summaries[0], summaries[1]) if len(summaries) >= 2 else 0.5
            agreement_scores.append(score)
        
        # 业务规则重叠度
        if len(rules_sets) >= 2:
            overlap = len(rules_sets[0] & rules_sets[1]) / len(rules_sets[0] | rules_sets[1]) if rules_sets[0] and rules_sets[1] else 0
            agreement_scores.append(overlap)
        
        # 副作用重叠度
        if len(side_effects_sets) >= 2:
            overlap = len(side_effects_sets[0] & side_effects_sets[1]) / len(side_effects_sets[0] | side_effects_sets[1]) if side_effects_sets[0] and side_effects_sets[1] else 0
            agreement_scores.append(overlap)
        
        # 计算平均一致性分数
        avg_agreement = sum(agreement_scores) / len(agreement_scores) if agreement_scores else 0.5
        
        # 选择最详细的结果作为共识
        best_result = max(model_results.values(), key=lambda r: len(str(r.parsed_data)))
        
        return best_result.parsed_data, avg_agreement
    
    def _text_similarity(self, text1: str, text2: str) -> float:
        """简单的文本相似度计算"""
        if not text1 or not text2:
            return 0.0
        
        words1 = set(text1.lower().split())
        words2 = set(text2.lower().split())
        
        if not words1 or not words2:
            return 0.0
        
        intersection = len(words1 & words2)
        union = len(words1 | words2)
        
        return intersection / union if union > 0 else 0.0
    
    def _prioritize_entities(self, entities: List[Any]) -> List[Any]:
        """实体优先级排序"""
        scored_entities = []
        
        for entity in entities:
            score = 0
            
            # 有详细文档的优先
            if entity.docstring and len(entity.docstring) > 100:
                score += 20
            
            # 公共API优先
            if hasattr(entity, 'visibility') and entity.visibility and entity.visibility.value == "public":
                score += 15
            
            # 复杂度高的优先
            if entity.complexity and entity.complexity > 10:
                score += 10
            
            # 代码行数多的优先
            if entity.lines_of_code > 30:
                score += 5
            
            # 入口点优先
            if self._is_entry_point(entity):
                score += 25
            
            scored_entities.append((entity, score))
        
        # 按分数排序
        scored_entities.sort(key=lambda x: x[1], reverse=True)
        return [e for e, _ in scored_entities]
    
    def _is_entry_point(self, entity: Any) -> bool:
        """判断是否为入口点"""
        # 检查装饰器
        if hasattr(entity, 'decorators'):
            for dec in entity.decorators:
                dec_lower = dec.lower()
                if any(kw in dec_lower for kw in ["route", "api", "endpoint", "get", "post", "put", "delete"]):
                    return True
        
        # 检查名称
        name_lower = entity.name.lower()
        if name_lower in ["main", "run", "start", "execute", "handler", "process_request"]:
            return True
        
        # 检查文件路径
        file_lower = entity.file_path.lower()
        if any(p in file_lower for p in ['controller', 'handler', 'api', 'endpoint', 'route', 'view']):
            return True
        
        return False
    
    def integrate_with_base_semantic(
        self, 
        base_semantic: EnhancedBusinessSemantic, 
        llm_result: CrossValidationResult
    ) -> EnhancedBusinessSemantic:
        """将LLM结果集成到基础语义中"""
        consensus = llm_result.consensus_data
        
        # 更新业务摘要
        if consensus.get("business_summary"):
            base_semantic.business_summary = consensus["business_summary"]
        
        # 更新业务标签
        if consensus.get("business_domain_tags"):
            base_semantic.business_domain_tags = list(set(
                base_semantic.business_domain_tags + consensus["business_domain_tags"]
            ))[:5]
        
        # 更新业务规则
        if consensus.get("business_rules"):
            base_semantic.business_rules = list(set(
                base_semantic.business_rules + consensus["business_rules"]
            ))
        
        # 更新副作用
        if consensus.get("side_effects"):
            base_semantic.side_effects = list(set(
                base_semantic.side_effects + consensus["side_effects"]
            ))[:5]
        
        # 更新异常场景
        if consensus.get("exception_scenarios"):
            base_semantic.exception_scenarios = list(set(
                base_semantic.exception_scenarios + consensus["exception_scenarios"]
            ))[:5]
        
        # 更新置信度
        base_semantic.confidence = llm_result.agreement_score
        base_semantic.needs_review = llm_result.needs_review
        
        return base_semantic