"""
重构分析器 - 百万行级存量项目AI驱动重构支持模块
实现：代码模块风险分级、业务语义逆向提取、技术债务扫描、架构腐化诊断
"""
import json
import logging
from typing import List, Dict, Any, Optional, Set, Tuple
from pathlib import Path
from collections import defaultdict, Counter
from datetime import datetime
from enum import Enum
from dataclasses import dataclass, field

from ..gitnexus.models import (
    KnowledgeGraph, CodeEntity, EntityType, Relationship, RelationshipType,
    ModuleInfo, ArchitectureInfo
)
from ..ai.llm_client import LLMClient

logger = logging.getLogger(__name__)


class RiskLevel(str, Enum):
    """代码模块风险分级"""
    P0_CRITICAL = "P0"     # 核心模块：核心交易/支付/主链路，人工100%把控
    P1_IMPORTANT = "P1"    # 重要模块：非核心但影响范围广，人工审核核心逻辑
    P2_COMMON = "P2"       # 通用模块：工具类/通用组件，AI可全自动重构
    P3_EDGE = "P3"         # 边缘模块：下线业务/死代码，AI可直接评估下线


class TechnicalDebtType(str, Enum):
    """技术债务类型"""
    CODE_SMELL = "code_smell"           # 代码坏味道
    DEAD_CODE = "dead_code"             # 死代码
    CIRCULAR_DEPENDENCY = "circular_dependency"  # 循环依赖
    HIGH_COMPLEXITY = "high_complexity" # 高复杂度
    DUPLICATION = "duplication"         # 代码重复
    MISSING_TESTS = "missing_tests"     # 缺少测试
    SECURITY_ISSUE = "security_issue"   # 安全问题
    PERFORMANCE_ISSUE = "performance_issue"  # 性能问题
    OUTDATED_DEPENDENCY = "outdated_dependency"  # 过时依赖


@dataclass
class RiskAssessment:
    """风险评估结果"""
    entity_id: str
    risk_level: RiskLevel
    score: float  # 0-100
    reasons: List[str] = field(default_factory=list)
    impact_scope: List[str] = field(default_factory=list)  # 影响范围
    business_domain: Optional[str] = None  # 业务域
    refactoring_recommendation: Optional[str] = None


@dataclass
class TechnicalDebt:
    """技术债务"""
    id: str
    debt_type: TechnicalDebtType
    severity: str  # critical, high, medium, low
    entity_id: Optional[str]
    file_path: str
    line_start: int
    line_end: int
    description: str
    suggestion: str
    estimated_effort: str  # 预估工作量


@dataclass
class BusinessSemantic:
    """业务语义"""
    entity_id: str
    business_domain: str  # 业务域
    business_rules: List[str]  # 业务规则
    input_contract: Dict[str, Any]  # 输入契约
    output_contract: Dict[str, Any]  # 输出契约
    side_effects: List[str]  # 副作用
    exception_handling: List[str]  # 异常处理逻辑
    boundary_conditions: List[str]  # 边界条件
    dependencies: List[str]  # 业务依赖


@dataclass
class ArchitectureDiagnosis:
    """架构腐化诊断结果"""
    diagnosis_id: str
    architecture_style: str  # 当前架构风格
    issues: List[Dict[str, Any]]  # 架构问题列表
    violations: List[Dict[str, Any]]  # 违反设计原则的场景
    recommendations: List[str]  # 重构建议
    hotspots: List[Dict[str, Any]]  # 热点区域
    cyclic_dependencies: List[List[str]]  # 循环依赖链
    layer_violations: List[Dict[str, Any]]  # 分层违规


class RefactoringAnalyzer:
    """重构分析器 - 支持百万行级项目重构"""
    
    def __init__(self, graph: KnowledgeGraph, config: Dict[str, Any] = None):
        self.graph = graph
        self.config = config or {}
        self.llm = LLMClient(self.config.get('ai', {})) if self.config else None
        
        # 索引
        self._build_indexes()
        
        # 风险评估配置
        self.risk_config = {
            # 核心业务关键词
            "core_keywords": [
                "payment", "trade", "order", "transaction", "checkout",
                "支付", "交易", "订单", "结算", "核心", "main", "critical"
            ],
            # 入口点关键词
            "entry_keywords": [
                "api", "controller", "handler", "endpoint", "route",
                "main", "run", "start", "execute", "process"
            ],
            # 高风险修饰符
            "high_risk_modifiers": [
                "async", "transactional", "lock", "sync", "critical"
            ],
            # 复杂度阈值
            "complexity_threshold": 15,
            "loc_threshold": 200
        }
    
    def _build_indexes(self):
        """构建加速索引"""
        self.caller_to_callees: Dict[str, List[str]] = defaultdict(list)
        self.callee_to_callers: Dict[str, List[str]] = defaultdict(list)
        self.file_entities: Dict[str, List[CodeEntity]] = defaultdict(list)
        self.module_entities: Dict[str, List[CodeEntity]] = defaultdict(list)
        
        for entity in self.graph.entities.values():
            self.file_entities[entity.file_path].append(entity)
            if entity.module_path:
                self.module_entities[entity.module_path].append(entity)
        
        for rel in self.graph.relationships:
            if rel.relationship_type == RelationshipType.CALLS:
                self.caller_to_callees[rel.source_id].append(rel.target_id)
                self.callee_to_callers[rel.target_id].append(rel.source_id)
    
    # ==================== 阶段1：前置准备与资产盘点 ====================
    
    def analyze_full(self) -> Dict[str, Any]:
        """完整重构分析"""
        print("\n" + "="*60)
        print("开始重构分析...")
        print("="*60)
        
        results = {}
        
        # 1. 代码模块风险分级
        print("\n[1/5] 代码模块风险分级...")
        results["risk_assessment"] = self.analyze_risk_levels()
        
        # 2. 业务语义逆向提取
        print("[2/5] 业务语义逆向提取...")
        results["business_semantics"] = self.extract_business_semantics()
        
        # 3. 技术债务扫描
        print("[3/5] 技术债务扫描...")
        results["technical_debts"] = self.scan_technical_debts()
        
        # 4. 热点与风险代码识别
        print("[4/5] 热点与风险代码识别...")
        results["hotspots"] = self.identify_hotspots()
        
        # 5. 架构腐化诊断
        print("[5/5] 架构腐化诊断...")
        results["architecture_diagnosis"] = self.diagnose_architecture()
        
        # 生成重构范围边界说明书
        results["boundary_report"] = self._generate_boundary_report(results)
        
        print("\n重构分析完成！")
        return results
    
    def analyze_risk_levels(self) -> Dict[str, List[RiskAssessment]]:
        """代码模块风险分级（P0-P3）"""
        assessments = {
            RiskLevel.P0_CRITICAL: [],
            RiskLevel.P1_IMPORTANT: [],
            RiskLevel.P2_COMMON: [],
            RiskLevel.P3_EDGE: []
        }
        
        for entity in self.graph.entities.values():
            assessment = self._assess_entity_risk(entity)
            assessments[assessment.risk_level].append(assessment)
        
        # 打印统计
        for level, items in assessments.items():
            print(f"  {level.value}: {len(items)} 个实体")
        
        return assessments
    
    def _assess_entity_risk(self, entity: CodeEntity) -> RiskAssessment:
        """评估单个实体的风险等级"""
        score = 0
        reasons = []
        impact_scope = []
        
        # 1. 业务核心性评估（权重40%）
        core_score = self._evaluate_business_criticality(entity)
        score += core_score * 0.4
        if core_score > 60:
            reasons.append("属于核心业务模块")
        
        # 2. 影响范围评估（权重30%）
        impact_score, affected_entities = self._evaluate_impact_scope(entity)
        score += impact_score * 0.3
        impact_scope = affected_entities[:10]
        if impact_score > 50:
            reasons.append(f"影响范围广（{len(affected_entities)}个依赖方）")
        
        # 3. 代码复杂度评估（权重20%）
        complexity_score = self._evaluate_complexity(entity)
        score += complexity_score * 0.2
        if complexity_score > 60:
            reasons.append(f"代码复杂度高（复杂度：{entity.complexity or 'N/A'}）")
        
        # 4. 变更频率评估（权重10%）
        # 注：实际项目中应从git历史获取
        change_score = 30  # 默认中等
        score += change_score * 0.1
        
        # 确定风险等级
        if score >= 75:
            risk_level = RiskLevel.P0_CRITICAL
        elif score >= 55:
            risk_level = RiskLevel.P1_IMPORTANT
        elif score >= 35:
            risk_level = RiskLevel.P2_COMMON
        else:
            risk_level = RiskLevel.P3_EDGE
        
        # 如果是核心关键词相关，至少是P1
        name_lower = entity.name.lower()
        if any(kw in name_lower for kw in self.risk_config["core_keywords"]):
            if risk_level not in [RiskLevel.P0_CRITICAL]:
                risk_level = RiskLevel.P1_IMPORTANT
                reasons.append("名称包含核心业务关键词")
        
        return RiskAssessment(
            entity_id=entity.id,
            risk_level=risk_level,
            score=score,
            reasons=reasons,
            impact_scope=impact_scope,
            refactoring_recommendation=self._get_refactoring_recommendation(risk_level, entity)
        )
    
    def _evaluate_business_criticality(self, entity: CodeEntity) -> float:
        """评估业务核心性"""
        score = 0
        name_lower = entity.name.lower()
        file_lower = entity.file_path.lower()
        
        # 检查核心关键词
        for kw in self.risk_config["core_keywords"]:
            if kw in name_lower or kw in file_lower:
                score += 20
        
        # 检查是否为入口点
        for kw in self.risk_config["entry_keywords"]:
            if kw in name_lower or kw in file_lower:
                score += 15
        
        # 检查装饰器
        for dec in entity.decorators:
            if any(kw in dec.lower() for kw in ["route", "api", "endpoint", "transaction"]):
                score += 15
        
        # 检查是否被大量调用
        callers = self.callee_to_callers.get(entity.id, [])
        score += min(len(callers) * 2, 20)
        
        return min(score, 100)
    
    def _evaluate_impact_scope(self, entity: CodeEntity) -> Tuple[float, List[str]]:
        """评估影响范围"""
        affected = set()
        
        # 直接调用者
        callers = self.callee_to_callers.get(entity.id, [])
        affected.update(callers)
        
        # 间接影响（向上追溯3层）
        def trace_callers(entity_id: str, depth: int, visited: Set[str]):
            if depth <= 0 or entity_id in visited:
                return
            visited.add(entity_id)
            for caller_id in self.callee_to_callers.get(entity_id, []):
                affected.add(caller_id)
                trace_callers(caller_id, depth - 1, visited)
        
        trace_callers(entity.id, 3, set())
        
        # 计算分数
        score = min(len(affected) * 3, 100)
        return score, list(affected)
    
    def _evaluate_complexity(self, entity: CodeEntity) -> float:
        """评估代码复杂度"""
        score = 0
        
        # 圈复杂度
        if entity.complexity:
            if entity.complexity > 20:
                score += 40
            elif entity.complexity > 10:
                score += 25
            elif entity.complexity > 5:
                score += 10
        
        # 代码行数
        if entity.lines_of_code > 200:
            score += 30
        elif entity.lines_of_code > 100:
            score += 20
        elif entity.lines_of_code > 50:
            score += 10
        
        # 嵌套深度
        if entity.nesting_depth and entity.nesting_depth > 4:
            score += 20
        
        # 参数数量
        if entity.parameters and len(entity.parameters) > 5:
            score += 10
        
        return min(score, 100)
    
    def _get_refactoring_recommendation(self, risk_level: RiskLevel, entity: CodeEntity) -> str:
        """获取重构建议"""
        recommendations = {
            RiskLevel.P0_CRITICAL: "核心模块，禁止AI全自动重构。需要：1) 完整测试覆盖 2) 架构师审批 3) 人工审核所有变更",
            RiskLevel.P1_IMPORTANT: "重要模块，AI生成方案需人工审核。建议：1) 补充单元测试 2) 技术负责人审批",
            RiskLevel.P2_COMMON: "通用模块，AI可执行全自动重构，人工抽检即可。",
            RiskLevel.P3_EDGE: "边缘模块，可直接评估下线或全量重写。"
        }
        return recommendations.get(risk_level, "")
    
    def extract_business_semantics(self) -> Dict[str, BusinessSemantic]:
        """业务语义逆向提取"""
        semantics = {}
        
        # 识别核心业务实体
        core_entities = self._identify_business_entities()
        
        for entity in core_entities:
            semantic = self._extract_entity_semantic(entity)
            semantics[entity.id] = semantic
        
        # 如果LLM可用，进行深度语义分析
        if self.llm and self.llm.is_available():
            semantics = self._enhance_semantics_with_llm(semantics)
        
        return semantics
    
    def _identify_business_entities(self) -> List[CodeEntity]:
        """识别业务相关实体"""
        entities = []
        
        for entity in self.graph.entities.values():
            # 跳过非核心实体
            if entity.entity_type in [EntityType.IMPORT, EntityType.COMMENT, EntityType.TODO]:
                continue
            
            # 有文档或注释的实体优先
            if entity.docstring:
                entities.append(entity)
                continue
            
            # 被调用的实体
            if self.callee_to_callers.get(entity.id):
                entities.append(entity)
                continue
            
            # 公共API
            if entity.visibility == Visibility.PUBLIC if hasattr(Visibility, 'PUBLIC') else True:
                if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD, EntityType.CLASS]:
                    entities.append(entity)
        
        return entities
    
    def _extract_entity_semantic(self, entity: CodeEntity) -> BusinessSemantic:
        """提取单个实体的业务语义"""
        # 从文档中提取业务规则
        business_rules = self._extract_business_rules(entity)
        
        # 从参数和返回值推断契约
        input_contract = self._infer_input_contract(entity)
        output_contract = self._infer_output_contract(entity)
        
        # 从调用关系推断副作用
        side_effects = self._infer_side_effects(entity)
        
        # 异常处理
        exception_handling = entity.raises if entity.raises else []
        
        # 边界条件（从注释和代码推断）
        boundary_conditions = self._extract_boundary_conditions(entity)
        
        # 业务域推断
        business_domain = self._infer_business_domain(entity)
        
        return BusinessSemantic(
            entity_id=entity.id,
            business_domain=business_domain,
            business_rules=business_rules,
            input_contract=input_contract,
            output_contract=output_contract,
            side_effects=side_effects,
            exception_handling=exception_handling,
            boundary_conditions=boundary_conditions,
            dependencies=[rel.target_id for rel in self.graph.relationships 
                         if rel.source_id == entity.id and rel.relationship_type == RelationshipType.DEPENDS_ON]
        )
    
    def _extract_business_rules(self, entity: CodeEntity) -> List[str]:
        """从文档和注释中提取业务规则"""
        rules = []
        
        if entity.docstring:
            # 简单提取：查找包含关键词的句子
            keywords = ["必须", "应该", "需要", "要求", "规则", "限制", "如果", "当", "验证"]
            lines = entity.docstring.split('\n')
            for line in lines:
                if any(kw in line for kw in keywords):
                    rules.append(line.strip())
        
        return rules[:10]  # 限制数量
    
    def _infer_input_contract(self, entity: CodeEntity) -> Dict[str, Any]:
        """推断输入契约"""
        contract = {}
        if entity.parameters:
            for param in entity.parameters:
                name = param.get("name", "")
                annotation = param.get("annotation")
                contract[name] = {
                    "type": annotation or "Any",
                    "required": param.get("default") is None
                }
        return contract
    
    def _infer_output_contract(self, entity: CodeEntity) -> Dict[str, Any]:
        """推断输出契约"""
        return {
            "type": entity.return_type or "Any",
            "description": ""
        }
    
    def _infer_side_effects(self, entity: CodeEntity) -> List[str]:
        """推断副作用"""
        effects = []
        
        # 检查调用的方法
        callees = self.caller_to_callees.get(entity.id, [])
        for callee_id in callees:
            callee = self.graph.entities.get(callee_id)
            if callee:
                name_lower = callee.name.lower()
                # 写操作
                if any(kw in name_lower for kw in ["write", "save", "update", "delete", "insert", "create"]):
                    effects.append(f"调用 {callee.name}（可能的写操作）")
                # 发送操作
                if any(kw in name_lower for kw in ["send", "emit", "publish", "notify"]):
                    effects.append(f"调用 {callee.name}（可能的通信操作）")
        
        return effects[:5]
    
    def _extract_boundary_conditions(self, entity: CodeEntity) -> List[str]:
        """提取边界条件"""
        conditions = []
        
        # 从注释中提取
        for comment in entity.comments:
            if any(kw in comment.lower() for kw in ["边界", "条件", "限制", "范围", "最大", "最小"]):
                conditions.append(comment)
        
        # 从文档中提取
        if entity.docstring:
            for line in entity.docstring.split('\n'):
                if any(kw in line.lower() for kw in ["边界", "条件", "限制", "范围", "args:", "raises:", "note:"]):
                    conditions.append(line.strip())
        
        return conditions[:5]
    
    def _infer_business_domain(self, entity: CodeEntity) -> str:
        """推断业务域"""
        name_lower = entity.name.lower()
        file_lower = entity.file_path.lower()
        
        domain_keywords = {
            "用户管理": ["user", "auth", "login", "account", "用户", "登录"],
            "订单管理": ["order", "订单"],
            "支付管理": ["payment", "pay", "支付", "付款"],
            "数据分析": ["analytics", "analysis", "report", "统计", "分析"],
            "数据采集": ["collect", "crawl", "fetch", "采集", "爬取"],
            "配置管理": ["config", "setting", "配置", "设置"],
            "任务调度": ["schedule", "task", "job", "调度", "任务"],
            "消息通信": ["message", "event", "notify", "消息", "通知", "事件"],
            "数据处理": ["process", "transform", "data", "处理", "转换"],
            "API接口": ["api", "endpoint", "route", "controller", "handler"],
        }
        
        for domain, keywords in domain_keywords.items():
            if any(kw in name_lower or kw in file_lower for kw in keywords):
                return domain
        
        return "其他"
    
    def _enhance_semantics_with_llm(self, semantics: Dict[str, BusinessSemantic]) -> Dict[str, BusinessSemantic]:
        """使用LLM增强语义分析"""
        # 选择部分核心实体进行LLM分析
        entity_ids = list(semantics.keys())[:20]
        
        for entity_id in entity_ids:
            semantic = semantics[entity_id]
            entity = self.graph.entities.get(entity_id)
            if not entity:
                continue
            
            prompt = f"""请分析以下代码实体的业务语义：

名称：{entity.name}
类型：{entity.entity_type.value}
文件：{entity.file_path}
文档：{entity.docstring or '无'}

请提取：
1. 业务规则（列表形式）
2. 输入输出契约
3. 可能的副作用
4. 边界条件

以JSON格式返回。"""
            
            try:
                result = self.llm.chat_sync(
                    system_prompt="你是一个业务分析专家，擅长从代码中提取业务语义。",
                    user_prompt=prompt,
                    max_tokens=1000,
                    temperature=0.1
                )
                # 解析结果并更新语义（简化实现）
                if result:
                    semantic.business_rules.extend([f"LLM提取: {result[:100]}"])
            except Exception as e:
                logger.warning(f"LLM语义增强失败: {e}")
        
        return semantics
    
    def scan_technical_debts(self) -> List[TechnicalDebt]:
        """技术债务扫描"""
        debts = []
        debt_id = 0
        
        # 1. 高复杂度代码
        for entity in self.graph.entities.values():
            if entity.complexity and entity.complexity > 15:
                debt_id += 1
                debts.append(TechnicalDebt(
                    id=f"debt_{debt_id}",
                    debt_type=TechnicalDebtType.HIGH_COMPLEXITY,
                    severity="high" if entity.complexity > 25 else "medium",
                    entity_id=entity.id,
                    file_path=entity.file_path,
                    line_start=entity.start_line,
                    line_end=entity.end_line,
                    description=f"代码复杂度过高（圈复杂度：{entity.complexity}）",
                    suggestion="考虑拆分函数或简化逻辑",
                    estimated_effort="2-4小时"
                ))
        
        # 2. 死代码检测
        dead_entities = self._detect_dead_code()
        for entity in dead_entities:
            debt_id += 1
            debts.append(TechnicalDebt(
                id=f"debt_{debt_id}",
                debt_type=TechnicalDebtType.DEAD_CODE,
                severity="low",
                entity_id=entity.id,
                file_path=entity.file_path,
                line_start=entity.start_line,
                line_end=entity.end_line,
                description=f"疑似死代码：{entity.name}（无调用者）",
                suggestion="评估是否可以删除",
                estimated_effort="30分钟"
            ))
        
        # 3. 循环依赖检测
        cycles = self._detect_circular_dependencies()
        for cycle in cycles:
            debt_id += 1
            debts.append(TechnicalDebt(
                id=f"debt_{debt_id}",
                debt_type=TechnicalDebtType.CIRCULAR_DEPENDENCY,
                severity="high",
                entity_id=None,
                file_path="",
                line_start=0,
                line_end=0,
                description=f"循环依赖：{' -> '.join(cycle)}",
                suggestion="引入中介层或重构依赖关系",
                estimated_effort="4-8小时"
            ))
        
        # 4. 大文件/大类检测
        for entity in self.graph.entities.values():
            if entity.entity_type == EntityType.CLASS and entity.lines_of_code > 500:
                debt_id += 1
                debts.append(TechnicalDebt(
                    id=f"debt_{debt_id}",
                    debt_type=TechnicalDebtType.CODE_SMELL,
                    severity="medium",
                    entity_id=entity.id,
                    file_path=entity.file_path,
                    line_start=entity.start_line,
                    line_end=entity.end_line,
                    description=f"大类（{entity.lines_of_code}行），违反单一职责原则",
                    suggestion="拆分为多个小类",
                    estimated_effort="4-8小时"
                ))
        
        # 5. 缺少文档
        for entity in self.graph.entities.values():
            if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD, EntityType.CLASS]:
                if not entity.docstring and entity.visibility != "private":
                    debt_id += 1
                    debts.append(TechnicalDebt(
                        id=f"debt_{debt_id}",
                        debt_type=TechnicalDebtType.CODE_SMELL,
                        severity="low",
                        entity_id=entity.id,
                        file_path=entity.file_path,
                        line_start=entity.start_line,
                        line_end=entity.end_line,
                        description=f"{entity.name} 缺少文档注释",
                        suggestion="添加docstring说明功能和参数",
                        estimated_effort="15分钟"
                    ))
        
        print(f"  发现 {len(debts)} 项技术债务")
        return debts
    
    def _detect_dead_code(self) -> List[CodeEntity]:
        """检测死代码"""
        dead = []
        
        for entity in self.graph.entities.values():
            if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD]:
                callers = self.callee_to_callers.get(entity.id, [])
                # 没有调用者且不是入口点
                if not callers and not self._is_entry_point(entity):
                    dead.append(entity)
        
        return dead
    
    def _is_entry_point(self, entity: CodeEntity) -> bool:
        """判断是否为入口点"""
        # 检查装饰器
        for dec in entity.decorators:
            if any(kw in dec.lower() for kw in ["route", "api", "endpoint", "get", "post", "put", "delete"]):
                return True
        
        # 检查名称
        if entity.name in ["main", "run", "start", "execute"]:
            return True
        
        return False
    
    def _detect_circular_dependencies(self) -> List[List[str]]:
        """检测循环依赖"""
        cycles = []
        visited = set()
        
        def dfs(entity_id: str, path: List[str], path_set: Set[str]):
            if entity_id in path_set:
                # 找到循环
                cycle_start = path.index(entity_id)
                cycle = path[cycle_start:] + [entity_id]
                return cycle
            
            if entity_id in visited:
                return None
            
            path.append(entity_id)
            path_set.add(entity_id)
            
            # 查找依赖
            for callee_id in self.caller_to_callees.get(entity_id, []):
                cycle = dfs(callee_id, path, path_set)
                if cycle:
                    return cycle
            
            path.pop()
            path_set.remove(entity_id)
            visited.add(entity_id)
            return None
        
        for entity_id in self.graph.entities:
            cycle = dfs(entity_id, [], set())
            if cycle:
                # 标准化循环（从最小ID开始）
                min_idx = cycle.index(min(cycle[:-1]))
                normalized = cycle[min_idx:-1] + cycle[:min_idx] + [cycle[min_idx]]
                if normalized not in cycles:
                    cycles.append(normalized)
                    if len(cycles) >= 10:  # 限制数量
                        break
        
        return cycles
    
    def identify_hotspots(self) -> List[Dict[str, Any]]:
        """识别热点与风险代码"""
        hotspots = []
        
        # 1. 高频修改热点（基于被调用次数）
        call_hotspots = []
        for entity_id, callers in self.callee_to_callers.items():
            if len(callers) > 5:
                entity = self.graph.entities.get(entity_id)
                if entity:
                    call_hotspots.append({
                        "entity_id": entity_id,
                        "name": entity.name,
                        "file_path": entity.file_path,
                        "caller_count": len(callers),
                        "type": "high_frequency_call",
                        "risk": "高" if len(callers) > 20 else "中"
                    })
        
        call_hotspots.sort(key=lambda x: x["caller_count"], reverse=True)
        hotspots.extend(call_hotspots[:20])
        
        # 2. 复杂度热点
        complexity_hotspots = []
        for entity in self.graph.entities.values():
            if entity.complexity and entity.complexity > 10:
                complexity_hotspots.append({
                    "entity_id": entity.id,
                    "name": entity.name,
                    "file_path": entity.file_path,
                    "complexity": entity.complexity,
                    "loc": entity.lines_of_code,
                    "type": "high_complexity",
                    "risk": "高" if entity.complexity > 20 else "中"
                })
        
        complexity_hotspots.sort(key=lambda x: x["complexity"], reverse=True)
        hotspots.extend(complexity_hotspots[:15])
        
        # 3. 核心链路识别
        core_chains = self._identify_core_chains()
        hotspots.extend(core_chains[:10])
        
        print(f"  识别 {len(hotspots)} 个热点区域")
        return hotspots
    
    def _identify_core_chains(self) -> List[Dict[str, Any]]:
        """识别核心业务链路"""
        chains = []
        
        # 从入口点开始追踪
        for entity in self.graph.entities.values():
            if self._is_entry_point(entity):
                chain = self._trace_call_chain(entity.id, max_depth=5)
                if len(chain) > 2:
                    chains.append({
                        "entity_id": entity.id,
                        "name": entity.name,
                        "file_path": entity.file_path,
                        "chain": chain,
                        "type": "core_chain",
                        "risk": "高"
                    })
        
        return chains
    
    def _trace_call_chain(self, entity_id: str, max_depth: int) -> List[str]:
        """追踪调用链"""
        chain = [entity_id]
        visited = {entity_id}
        
        def traverse(eid: str, depth: int):
            if depth <= 0:
                return
            callees = self.caller_to_callees.get(eid, [])
            for cid in callees[:3]:  # 限制分支
                if cid not in visited:
                    visited.add(cid)
                    chain.append(cid)
                    traverse(cid, depth - 1)
        
        traverse(entity_id, max_depth)
        return chain
    
    # ==================== 阶段2：全局架构梳理与重构体系搭建 ====================
    
    def diagnose_architecture(self) -> ArchitectureDiagnosis:
        """架构腐化诊断"""
        diagnosis_id = f"diag_{datetime.now().strftime('%Y%m%d%H%M%S')}"
        
        # 1. 识别架构风格
        architecture_style = self._identify_architecture_style()
        
        # 2. 检测架构问题
        issues = self._detect_architecture_issues()
        
        # 3. 检测设计原则违规
        violations = self._detect_design_violations()
        
        # 4. 检测循环依赖
        cyclic_dependencies = self._detect_circular_dependencies()
        
        # 5. 检测分层违规
        layer_violations = self._detect_layer_violations()
        
        # 6. 生成重构建议
        recommendations = self._generate_refactoring_recommendations(issues, violations)
        
        return ArchitectureDiagnosis(
            diagnosis_id=diagnosis_id,
            architecture_style=architecture_style,
            issues=issues,
            violations=violations,
            recommendations=recommendations,
            hotspots=self._identify_architecture_hotspots(),
            cyclic_dependencies=cyclic_dependencies,
            layer_violations=layer_violations
        )
    
    def _identify_architecture_style(self) -> str:
        """识别架构风格"""
        # 基于模块和层级分布推断
        layer_counts = defaultdict(int)
        
        for entity in self.graph.entities.values():
            file_path = entity.file_path.lower().replace('\\', '/')
            
            if any(p in file_path for p in ['api', 'controller', 'view', 'route', 'endpoint']):
                layer_counts["presentation"] += 1
            elif any(p in file_path for p in ['service', 'business', 'logic', 'manager']):
                layer_counts["business"] += 1
            elif any(p in file_path for p in ['data', 'dao', 'repository', 'model', 'entity', 'db']):
                layer_counts["data"] += 1
            elif any(p in file_path for p in ['core', 'base', 'common', 'util', 'helper']):
                layer_counts["infrastructure"] += 1
        
        if len(layer_counts) >= 3:
            return "分层架构"
        elif 'service' in str(layer_counts).lower() or 'business' in str(layer_counts).lower():
            return "服务导向架构"
        else:
            return "单体架构"
    
    def _detect_architecture_issues(self) -> List[Dict[str, Any]]:
        """检测架构问题"""
        issues = []
        
        # 1. 大泥球检测
        large_modules = []
        for module_path, entities in self.module_entities.items():
            if len(entities) > 50:
                large_modules.append({
                    "type": "big_ball_of_mud",
                    "module": module_path,
                    "entity_count": len(entities),
                    "description": f"模块过大，包含 {len(entities)} 个实体，可能成为大泥球",
                    "severity": "high" if len(entities) > 100 else "medium"
                })
        issues.extend(large_modules[:5])
        
        # 2. 职责越界检测
        cross_layer_calls = self._detect_cross_layer_calls()
        issues.extend(cross_layer_calls[:10])
        
        # 3. 过度耦合检测
        highly_coupled = []
        for entity_id, callees in self.caller_to_callees.items():
            if len(callees) > 10:
                entity = self.graph.entities.get(entity_id)
                if entity:
                    highly_coupled.append({
                        "type": "high_coupling",
                        "entity": entity.name,
                        "file_path": entity.file_path,
                        "coupling_count": len(callees),
                        "description": f"{entity.name} 过度耦合，调用 {len(callees)} 个其他实体",
                        "severity": "medium"
                    })
        issues.extend(highly_coupled[:5])
        
        return issues
    
    def _detect_cross_layer_calls(self) -> List[Dict[str, Any]]:
        """检测跨层调用"""
        violations = []
        
        # 定义层级顺序
        layer_order = {
            "presentation": 1,
            "business": 2,
            "data": 3,
            "infrastructure": 0
        }
        
        def get_layer(file_path: str) -> str:
            file_lower = file_path.lower()
            if any(p in file_lower for p in ['api', 'controller', 'view', 'route', 'endpoint']):
                return "presentation"
            elif any(p in file_lower for p in ['service', 'business', 'logic', 'manager', 'processor']):
                return "business"
            elif any(p in file_lower for p in ['data', 'dao', 'repository', 'model', 'entity', 'db']):
                return "data"
            return "infrastructure"
        
        for rel in self.graph.relationships:
            if rel.relationship_type == RelationshipType.CALLS:
                source = self.graph.entities.get(rel.source_id)
                target = self.graph.entities.get(rel.target_id)
                
                if source and target:
                    source_layer = get_layer(source.file_path)
                    target_layer = get_layer(target.file_path)
                    
                    # 检测违规：数据层直接调用表现层
                    if layer_order.get(source_layer, 0) > layer_order.get(target_layer, 0):
                        violations.append({
                            "type": "cross_layer_call",
                            "source": source.name,
                            "source_layer": source_layer,
                            "target": target.name,
                            "target_layer": target_layer,
                            "description": f"{source_layer} -> {target_layer} 违规调用",
                            "severity": "medium"
                        })
        
        return violations
    
    def _detect_design_violations(self) -> List[Dict[str, Any]]:
        """检测设计原则违规"""
        violations = []
        
        # 1. 单一职责违规（大类）
        for entity in self.graph.entities.values():
            if entity.entity_type == EntityType.CLASS:
                if entity.lines_of_code > 300:
                    violations.append({
                        "type": "single_responsibility_violation",
                        "entity": entity.name,
                        "file_path": entity.file_path,
                        "description": f"类过大（{entity.lines_of_code}行），可能违反单一职责原则",
                        "severity": "medium"
                    })
        
        # 2. 依赖倒置违规（高层直接依赖具体实现）
        for entity in self.graph.entities.values():
            if entity.entity_type == EntityType.CLASS:
                # 检查是否直接依赖具体类而非接口
                for base in entity.bases:
                    if not base.startswith('I') and not base.startswith('Abstract'):
                        # 这只是一个简单的启发式检查
                        pass
        
        return violations[:10]
    
    def _detect_layer_violations(self) -> List[Dict[str, Any]]:
        """检测分层违规"""
        return self._detect_cross_layer_calls()
    
    def _identify_architecture_hotspots(self) -> List[Dict[str, Any]]:
        """识别架构热点"""
        hotspots = []
        
        # 计算每个模块的耦合度
        module_coupling = defaultdict(lambda: {"in": 0, "out": 0})
        
        for rel in self.graph.relationships:
            if rel.relationship_type == RelationshipType.CALLS:
                source = self.graph.entities.get(rel.source_id)
                target = self.graph.entities.get(rel.target_id)
                
                if source and target:
                    source_module = source.module_path or source.file_path
                    target_module = target.module_path or target.file_path
                    
                    if source_module != target_module:
                        module_coupling[source_module]["out"] += 1
                        module_coupling[target_module]["in"] += 1
        
        # 找出高耦合模块
        for module, coupling in sorted(module_coupling.items(), 
                                       key=lambda x: x[1]["in"] + x[1]["out"], 
                                       reverse=True)[:10]:
            hotspots.append({
                "module": module,
                "in_coupling": coupling["in"],
                "out_coupling": coupling["out"],
                "total_coupling": coupling["in"] + coupling["out"]
            })
        
        return hotspots
    
    def _generate_refactoring_recommendations(self, issues: List, violations: List) -> List[str]:
        """生成重构建议"""
        recommendations = []
        
        # 基于问题类型生成建议
        issue_types = Counter(i.get("type") for i in issues)
        
        if issue_types.get("big_ball_of_mud", 0) > 0:
            recommendations.append("建议对大模块进行拆分，按职责划分为独立子模块")
        
        if issue_types.get("cross_layer_call", 0) > 0:
            recommendations.append("建议引入分层架构规范，禁止跨层直接调用，使用依赖注入解耦")
        
        if issue_types.get("high_coupling", 0) > 0:
            recommendations.append("建议使用中介者模式或事件驱动架构降低模块间耦合")
        
        # 循环依赖
        cycles = self._detect_circular_dependencies()
        if cycles:
            recommendations.append(f"发现 {len(cycles)} 个循环依赖，建议引入接口层或事件机制解耦")
        
        # 通用建议
        recommendations.extend([
            "建议为核心模块补充完整的单元测试",
            "建议建立代码审查机制，在CI中集成静态分析门禁",
            "建议为重构制定分阶段计划，从P3边缘模块开始渐进式重构"
        ])
        
        return recommendations
    
    def _generate_boundary_report(self, results: Dict[str, Any]) -> str:
        """生成重构范围边界说明书"""
        report = []
        report.append("# 重构范围边界说明书\n")
        report.append(f"生成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        
        # 风险分级统计
        risk = results.get("risk_assessment", {})
        report.append("\n## 一、代码模块风险分级\n")
        for level in [RiskLevel.P0_CRITICAL, RiskLevel.P1_IMPORTANT, RiskLevel.P2_COMMON, RiskLevel.P3_EDGE]:
            entities = risk.get(level, [])
            report.append(f"\n### {level.value} 级模块（{len(entities)}个）\n")
            if level == RiskLevel.P0_CRITICAL:
                report.append("**禁止AI全自动重构**，需要架构师审批和完整测试覆盖\n")
            for e in entities[:5]:
                entity = self.graph.entities.get(e.entity_id)
                if entity:
                    report.append(f"- `{entity.name}` ({entity.file_path})\n")
        
        # 技术债务统计
        debts = results.get("technical_debts", [])
        report.append(f"\n## 二、技术债务统计\n")
        report.append(f"发现 {len(debts)} 项技术债务\n")
        
        debt_types = Counter(d.debt_type.value for d in debts)
        for dtype, count in debt_types.most_common():
            report.append(f"- {dtype}: {count}项\n")
        
        # 架构诊断
        arch = results.get("architecture_diagnosis")
        if arch:
            report.append(f"\n## 三、架构诊断结果\n")
            report.append(f"当前架构风格：{arch.architecture_style}\n")
            report.append(f"架构问题：{len(arch.issues)}项\n")
            report.append(f"循环依赖：{len(arch.cyclic_dependencies)}个\n")
        
        # 重构建议
        report.append("\n## 四、重构执行建议\n")
        report.append("1. **阶段优先级**：P3 → P2 → P1 → P0\n")
        report.append("2. **测试先行**：无测试不重构\n")
        report.append("3. **原子化变更**：单次commit只做一个重构动作\n")
        report.append("4. **人工审核**：P0/P1模块所有变更需人工审核\n")
        
        return "".join(report)
    
    def save_analysis_report(self, results: Dict[str, Any], output_dir: str = "output/refactoring"):
        """保存分析报告"""
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # 保存完整结果
        def convert_to_serializable(obj):
            if hasattr(obj, '__dict__'):
                return {k: convert_to_serializable(v) for k, v in obj.__dict__.items()}
            elif isinstance(obj, list):
                return [convert_to_serializable(item) for item in obj]
            elif isinstance(obj, dict):
                return {k: convert_to_serializable(v) for k, v in obj.items()}
            elif isinstance(obj, Enum):
                return obj.value
            return obj
        
        # 保存JSON
        with open(output_path / "analysis_result.json", "w", encoding="utf-8") as f:
            json.dump(convert_to_serializable(results), f, ensure_ascii=False, indent=2)
        
        # 保存边界说明书
        if "boundary_report" in results:
            with open(output_path / "boundary_report.md", "w", encoding="utf-8") as f:
                f.write(results["boundary_report"])
        
        # 保存技术债务清单
        debts = results.get("technical_debts", [])
        if debts:
            with open(output_path / "technical_debts.md", "w", encoding="utf-8") as f:
                f.write("# 技术债务清单\n\n")
                for debt in debts:
                    f.write(f"## {debt.id}\n")
                    f.write(f"- 类型：{debt.debt_type.value}\n")
                    f.write(f"- 严重性：{debt.severity}\n")
                    f.write(f"- 位置：{debt.file_path}:{debt.line_start}\n")
                    f.write(f"- 描述：{debt.description}\n")
                    f.write(f"- 建议：{debt.suggestion}\n")
                    f.write(f"- 预估工作量：{debt.estimated_effort}\n\n")
        
        print(f"\n分析报告已保存到：{output_path}")
        return output_path


# 从gitnexus.models导入Visibility
from ..gitnexus.models import Visibility
