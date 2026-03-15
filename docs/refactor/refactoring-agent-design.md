# 重构分析智能体详细设计文档

> 文档版本：v1.0
> 更新时间：2026-03-13
> 基于现有能力：知识提取智能体、RefactoringAnalyzer、DeepKnowledgeAnalyzer

---

## 一、设计背景与目标

### 1.1 背景

当前项目已完成**知识提取智能体**的开发，能够：
- 从代码仓库提取结构化知识（5954实体、6484关系）
- 生成LLM深度分析文档（项目概述、业务流程、架构设计、功能模块）
- 构建知识图谱和代码资产全景图

现需开发**重构分析智能体**，基于知识提取的输出，进行智能重构分析与建议生成。

### 1.2 核心目标

| 目标 | 描述 |
|------|------|
| **输入复用** | 完全复用知识提取智能体的输出（知识图谱、文档、实体数据） |
| **增量分析** | 在知识提取基础上增量执行重构分析，无需重复扫描代码 |
| **LLM驱动** | 利用LLM生成高质量重构方案和代码改进建议 |
| **风险可控** | 按P0-P3分级管理重构风险，确保业务契约不变 |

### 1.3 与现有系统的关系

```
┌─────────────────────────────────────────────────────────────────────┐
│                    知识提取智能体（已完成）                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ 代码扫描引擎  │→│ 知识图谱构建 │→│ 文档生成引擎 │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
│         ↓                  ↓                  ↓                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ entities.json│  │knowledge.json│  │  doc/*.md    │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ 输入复用
┌─────────────────────────────────────────────────────────────────────┐
│                    重构分析智能体（本次设计）                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ 知识加载引擎  │→│ 重构分析引擎 │→│ 建议生成引擎 │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
│         ↓                  ↓                  ↓                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ 上下文组装   │  │ 风险评估     │  │ 重构方案     │              │
│  │ 业务契约提取 │  │ 债务扫描     │  │ 改进建议     │              │
│  │ 依赖分析     │  │ 热点识别     │  │ 验证计划     │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 二、系统架构设计

### 2.1 整体架构

```
src/refactoring/
├── __init__.py
├── agent/
│   ├── __init__.py
│   ├── base_refactoring_agent.py    # 重构智能体基类
│   ├── risk_assessment_agent.py     # 风险评估智能体
│   ├── debt_scanner_agent.py        # 技术债务扫描智能体
│   ├── semantic_contract_agent.py   # 业务契约提取智能体
│   ├── refactoring_planner_agent.py # 重构规划智能体
│   ├── suggestion_generator_agent.py# 建议生成智能体
│   └── validation_agent.py          # 验证规划智能体
├── core/
│   ├── __init__.py
│   ├── knowledge_loader.py          # 知识加载器
│   ├── context_assembler.py         # 上下文组装器
│   ├── refactoring_orchestrator.py  # 重构编排器
│   └── refactoring_task.py          # 重构任务模型
├── analyzers/
│   ├── __init__.py
│   ├── dependency_analyzer.py       # 依赖分析器
│   ├── hotspot_analyzer.py          # 热点分析器
│   ├── architecture_analyzer.py     # 架构分析器
│   └── complexity_analyzer.py       # 复杂度分析器
├── generators/
│   ├── __init__.py
│   ├── refactoring_plan_generator.py    # 重构方案生成器
│   ├── improvement_suggestion_generator.py # 改进建议生成器
│   └── validation_plan_generator.py     # 验证计划生成器
├── output/
│   ├── __init__.py
│   ├── report_generator.py          # 报告生成器
│   └── visualization_exporter.py    # 可视化导出器
└── templates/
    ├── __init__.py
    ├── risk_assessment.j2           # 风险评估Prompt模板
    ├── semantic_contract.j2         # 业务契约提取Prompt模板
    ├── refactoring_plan.j2          # 重构方案Prompt模板
    └── improvement_suggestion.j2    # 改进建议Prompt模板
```

### 2.2 核心模块职责

| 模块 | 职责 | 输入 | 输出 |
|------|------|------|------|
| **KnowledgeLoader** | 加载知识提取输出 | doc/*.md, knowledge.json | KnowledgeContext |
| **ContextAssembler** | 组装LLM上下文 | KnowledgeContext, 目标范围 | RefactoringContext |
| **RiskAssessmentAgent** | 代码风险分级 | CodeEntity, 调用链 | RiskAssessment |
| **DebtScannerAgent** | 技术债务扫描 | KnowledgeGraph | TechnicalDebt[] |
| **SemanticContractAgent** | 业务契约提取 | CodeEntity, LLM | BusinessContract |
| **RefactoringPlannerAgent** | 重构方案规划 | RiskAssessment, Debts | RefactoringPlan |
| **SuggestionGeneratorAgent** | 改进建议生成 | Context, LLM | ImprovementSuggestion[] |
| **ValidationAgent** | 验证计划制定 | RefactoringPlan | ValidationPlan |

---

## 三、核心数据模型

### 3.1 知识上下文模型

```python
# src/refactoring/core/refactoring_task.py

from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
from datetime import datetime
from enum import Enum

class RiskLevel(str, Enum):
    """风险等级"""
    P0_CRITICAL = "P0"     # 核心模块：人工100%把控
    P1_IMPORTANT = "P1"    # 重要模块：人工审核核心逻辑
    P2_COMMON = "P2"       # 通用模块：AI可全自动重构
    P3_EDGE = "P3"         # 边缘模块：可直接评估下线

class RefactoringActionType(str, Enum):
    """重构动作类型"""
    # 低风险全自动动作
    EXTRACT_CONSTANT = "extract_constant"
    RENAME = "rename"
    FORMAT_CODE = "format_code"
    REMOVE_DEAD_CODE = "remove_dead_code"
    COMPLETE_COMMENT = "complete_comment"
    # 中风险需审核动作
    SPLIT_FUNCTION = "split_function"
    EXTRACT_METHOD = "extract_method"
    DECOUPLE_DEPENDENCY = "decouple_dependency"
    # 高风险需审批动作
    SPLIT_MODULE = "split_module"
    ARCHITECTURE_ADJUSTMENT = "architecture_adjustment"

class KnowledgeContext(BaseModel):
    """知识上下文 - 从知识提取输出加载"""
    # 项目信息
    project_name: str
    project_description: str
    tech_stack: List[str]
    
    # 知识图谱
    entities: Dict[str, Any]  # CodeEntity字典
    relationships: List[Any]  # Relationship列表
    
    # 文档
    project_overview: str     # design/project_overview.md
    business_flow: str        # business/business_flow.md
    architecture_design: str  # architecture/architecture_design.md
    feature_modules: str      # design/feature_modules.md
    api_reference: str        # design/api_reference.md
    
    # 统计信息
    total_entities: int
    total_relationships: int
    total_files: int
    
    # 元数据
    loaded_at: datetime = Field(default_factory=datetime.now)

class RefactoringContext(BaseModel):
    """重构上下文 - 组装LLM输入"""
    target_entity_id: str
    target_entity: Dict[str, Any]
    
    # 业务契约
    semantic_contract: Optional[Dict[str, Any]] = None
    
    # 依赖上下文
    callers: List[Dict[str, Any]] = []      # 调用方
    callees: List[Dict[str, Any]] = []      # 被调用方
    dependencies: List[Dict[str, Any]] = [] # 依赖项
    
    # 相关文档
    related_docs: List[str] = []
    
    # 业务域上下文
    business_domain: str = "其他"
    domain_tags: List[str] = []
    
    # Token统计
    context_token_count: int = 0

class TechnicalDebt(BaseModel):
    """技术债务"""
    debt_id: str
    debt_type: str
    severity: str  # critical, high, medium, low
    entity_id: Optional[str]
    file_path: str
    line_start: int
    line_end: int
    description: str
    suggestion: str
    estimated_effort: str
    related_refactoring_type: Optional[RefactoringActionType] = None

class RiskAssessment(BaseModel):
    """风险评估结果"""
    entity_id: str
    entity_name: str
    file_path: str
    risk_level: RiskLevel
    score: float  # 0-100
    reasons: List[str] = []
    impact_scope: List[str] = []
    business_domain: str = "其他"
    refactoring_recommendation: str = ""

class BusinessContract(BaseModel):
    """业务语义契约"""
    entity_id: str
    entity_name: str
    business_summary: str
    input_contract: Dict[str, Any] = {}
    output_contract: Dict[str, Any] = {}
    side_effects: List[str] = []
    business_rules: List[str] = []
    exception_scenarios: List[str] = []
    boundary_conditions: List[str] = []
    
class RefactoringPlan(BaseModel):
    """重构方案"""
    plan_id: str
    entity_id: str
    action_type: RefactoringActionType
    risk_level: RiskLevel
    
    # 重构内容
    before_code: str
    after_code: str
    changes_description: str
    
    # 验证计划
    validation_criteria: List[str] = []
    test_requirements: List[str] = []
    
    # 回滚方案
    rollback_plan: str = ""
    
    # 审批信息
    requires_approval: bool = False
    approver_roles: List[str] = []
    
    # 状态
    status: str = "pending"  # pending, approved, executing, completed, failed

class ImprovementSuggestion(BaseModel):
    """改进建议"""
    suggestion_id: str
    category: str  # architecture, code_quality, performance, security, testability
    priority: str  # high, medium, low
    target_scope: List[str] = []  # 实体ID列表
    
    title: str
    description: str
    rationale: str  # 建议理由
    
    implementation_steps: List[str] = []
    expected_benefits: List[str] = []
    potential_risks: List[str] = []
    
    related_debts: List[str] = []  # 关联的技术债务ID
    related_entities: List[str] = []

class RefactoringReport(BaseModel):
    """重构分析报告"""
    report_id: str
    project_name: str
    generated_at: datetime
    
    # 风险分析
    risk_summary: Dict[str, int] = {}  # {P0: 10, P1: 50, ...}
    high_risk_entities: List[RiskAssessment] = []
    
    # 技术债务
    total_debts: int = 0
    debt_by_type: Dict[str, int] = {}
    debt_by_severity: Dict[str, int] = {}
    top_debts: List[TechnicalDebt] = []
    
    # 架构分析
    architecture_style: str = ""
    architecture_issues: List[Dict[str, Any]] = []
    cyclic_dependencies: List[List[str]] = []
    
    # 热点分析
    hotspots: List[Dict[str, Any]] = []
    
    # 改进建议
    improvement_suggestions: List[ImprovementSuggestion] = []
    
    # 重构方案
    refactoring_plans: List[RefactoringPlan] = []
    
    # 执行建议
    execution_priority: List[str] = []  # 按优先级排序的实体ID
```

---

## 四、核心Agent设计

### 4.1 重构智能体基类

```python
# src/refactoring/agent/base_refactoring_agent.py

from abc import ABC, abstractmethod
from typing import Any, Dict, Optional
from src.agents.base_agent import BaseAgent, AgentResult
from src.ai.llm_client import LLMClient
from src.refactoring.core.refactoring_task import KnowledgeContext

class BaseRefactoringAgent(BaseAgent, ABC):
    """重构智能体基类 - 复用知识提取的输出"""
    
    def __init__(
        self, 
        name: str, 
        config: Dict[str, Any],
        llm_client: Optional[LLMClient] = None
    ):
        super().__init__(name, config)
        self.llm_client = llm_client
        self.knowledge_context: Optional[KnowledgeContext] = None
    
    def set_knowledge_context(self, context: KnowledgeContext):
        """设置知识上下文"""
        self.knowledge_context = context
    
    @abstractmethod
    async def _execute_impl(self, input_data: Any) -> Any:
        """子类实现具体逻辑"""
        pass
    
    def get_entity_by_id(self, entity_id: str) -> Optional[Dict[str, Any]]:
        """从知识上下文获取实体"""
        if self.knowledge_context:
            return self.knowledge_context.entities.get(entity_id)
        return None
    
    def get_related_entities(self, entity_id: str, relation_type: str = "calls") -> List[str]:
        """获取关联实体"""
        if not self.knowledge_context:
            return []
        
        related = []
        for rel in self.knowledge_context.relationships:
            if rel.get("type") == relation_type:
                if rel.get("source") == entity_id:
                    related.append(rel.get("target"))
                elif rel.get("target") == entity_id:
                    related.append(rel.get("source"))
        return related
```

### 4.2 风险评估智能体

```python
# src/refactoring/agent/risk_assessment_agent.py

from typing import Dict, List, Any
from src.refactoring.agent.base_refactoring_agent import BaseRefactoringAgent
from src.refactoring.core.refactoring_task import RiskAssessment, RiskLevel

class RiskAssessmentAgent(BaseRefactoringAgent):
    """风险评估智能体 - 基于知识图谱进行风险分级"""
    
    async def _execute_impl(self, input_data: Any) -> List[RiskAssessment]:
        """执行风险评估"""
        if not self.knowledge_context:
            raise ValueError("知识上下文未设置")
        
        assessments = []
        
        for entity_id, entity in self.knowledge_context.entities.items():
            assessment = self._assess_entity(entity)
            assessments.append(assessment)
        
        return assessments
    
    def _assess_entity(self, entity: Dict[str, Any]) -> RiskAssessment:
        """评估单个实体风险"""
        score = 0
        reasons = []
        
        # 1. 业务核心性评估（权重40%）
        core_score = self._evaluate_business_criticality(entity)
        score += core_score * 0.4
        
        # 2. 影响范围评估（权重30%）
        impact_score, affected = self._evaluate_impact_scope(entity)
        score += impact_score * 0.3
        
        # 3. 代码复杂度评估（权重20%）
        complexity_score = self._evaluate_complexity(entity)
        score += complexity_score * 0.2
        
        # 4. 变更频率评估（权重10%）
        score += 30 * 0.1  # 默认中等
        
        # 确定风险等级
        if score >= 75:
            risk_level = RiskLevel.P0_CRITICAL
        elif score >= 55:
            risk_level = RiskLevel.P1_IMPORTANT
        elif score >= 35:
            risk_level = RiskLevel.P2_COMMON
        else:
            risk_level = RiskLevel.P3_EDGE
        
        return RiskAssessment(
            entity_id=entity.get("id"),
            entity_name=entity.get("name"),
            file_path=entity.get("file_path"),
            risk_level=risk_level,
            score=score,
            reasons=reasons,
            impact_scope=affected[:10],
            business_domain=self._infer_business_domain(entity),
            refactoring_recommendation=self._get_recommendation(risk_level)
        )
    
    def _evaluate_business_criticality(self, entity: Dict[str, Any]) -> float:
        """评估业务核心性"""
        score = 0
        name_lower = entity.get("name", "").lower()
        file_lower = entity.get("file_path", "").lower()
        
        core_keywords = ["payment", "trade", "order", "transaction", "支付", "交易", "订单"]
        for kw in core_keywords:
            if kw in name_lower or kw in file_lower:
                score += 20
        
        # 使用知识文档辅助判断
        if self.knowledge_context:
            if name_lower in self.knowledge_context.project_overview.lower():
                score += 15
        
        return min(score, 100)
    
    def _evaluate_impact_scope(self, entity: Dict[str, Any]) -> tuple:
        """评估影响范围"""
        entity_id = entity.get("id")
        callers = self.get_related_entities(entity_id, "calls")
        
        # 追溯间接调用者
        all_affected = set(callers)
        for caller in callers[:10]:
            indirect = self.get_related_entities(caller, "calls")
            all_affected.update(indirect[:5])
        
        score = min(len(all_affected) * 3, 100)
        return score, list(all_affected)
    
    def _evaluate_complexity(self, entity: Dict[str, Any]) -> float:
        """评估代码复杂度"""
        score = 0
        
        complexity = entity.get("complexity", 0)
        if complexity > 20:
            score += 40
        elif complexity > 10:
            score += 25
        
        loc = entity.get("lines_of_code", 0)
        if loc > 200:
            score += 30
        elif loc > 100:
            score += 20
        
        return min(score, 100)
```

### 4.3 业务契约提取智能体

```python
# src/refactoring/agent/semantic_contract_agent.py

from typing import Dict, Any, Optional
from src.refactoring.agent.base_refactoring_agent import BaseRefactoringAgent
from src.refactoring.core.refactoring_task import BusinessContract

class SemanticContractAgent(BaseRefactoringAgent):
    """业务契约提取智能体 - 使用LLM提取业务语义"""
    
    async def _execute_impl(self, input_data: Any) -> Dict[str, BusinessContract]:
        """执行业务契约提取"""
        if not self.knowledge_context:
            raise ValueError("知识上下文未设置")
        
        contracts = {}
        
        # 选择核心实体进行LLM分析
        core_entities = self._select_core_entities()
        
        for entity in core_entities[:50]:  # 限制数量
            contract = await self._extract_contract_with_llm(entity)
            if contract:
                contracts[entity.get("id")] = contract
        
        return contracts
    
    def _select_core_entities(self) -> list:
        """选择核心实体"""
        entities = []
        for entity in self.knowledge_context.entities.values():
            # 优先选择有文档、被调用的实体
            if entity.get("docstring") or self.get_related_entities(entity.get("id")):
                entities.append(entity)
        return entities
    
    async def _extract_contract_with_llm(self, entity: Dict[str, Any]) -> Optional[BusinessContract]:
        """使用LLM提取业务契约"""
        if not self.llm_client:
            return self._extract_contract_heuristic(entity)
        
        # 组装上下文
        context = self._build_llm_context(entity)
        
        prompt = f"""请分析以下代码实体，提取业务语义契约：

## 实体信息
- 名称：{entity.get('name')}
- 类型：{entity.get('entity_type')}
- 文件：{entity.get('file_path')}

## 代码内容
```
{entity.get('content', '')[:2000]}
```

## 项目上下文
{self.knowledge_context.project_overview[:1000]}

## 文档注释
{entity.get('docstring', '无')}

请提取以下信息（JSON格式）：
1. business_summary: 业务功能摘要
2. input_contract: 输入契约（参数名、类型、约束）
3. output_contract: 输出契约（返回值类型、含义）
4. side_effects: 副作用列表
5. business_rules: 核心业务规则
6. exception_scenarios: 异常处理场景
"""
        
        try:
            result = self.llm_client.chat_sync(
                system_prompt="你是一个业务分析专家，擅长从代码中提取业务语义契约。",
                user_prompt=prompt,
                max_tokens=2000,
                temperature=0.1
            )
            
            # 解析LLM返回
            import json
            data = json.loads(result)
            
            return BusinessContract(
                entity_id=entity.get("id"),
                entity_name=entity.get("name"),
                business_summary=data.get("business_summary", ""),
                input_contract=data.get("input_contract", {}),
                output_contract=data.get("output_contract", {}),
                side_effects=data.get("side_effects", []),
                business_rules=data.get("business_rules", []),
                exception_scenarios=data.get("exception_scenarios", [])
            )
        except Exception as e:
            self.logger.warning(f"LLM提取失败: {e}")
            return self._extract_contract_heuristic(entity)
    
    def _extract_contract_heuristic(self, entity: Dict[str, Any]) -> BusinessContract:
        """启发式提取业务契约"""
        return BusinessContract(
            entity_id=entity.get("id"),
            entity_name=entity.get("name"),
            business_summary=entity.get("docstring", "")[:200] if entity.get("docstring") else "",
            input_contract=self._infer_input_contract(entity),
            output_contract={"type": entity.get("return_type", "Any")},
            side_effects=self._infer_side_effects(entity)
        )
```

### 4.4 重构规划智能体

```python
# src/refactoring/agent/refactoring_planner_agent.py

from typing import List, Dict, Any
from src.refactoring.agent.base_refactoring_agent import BaseRefactoringAgent
from src.refactoring.core.refactoring_task import (
    RefactoringPlan, RefactoringActionType, RiskLevel, TechnicalDebt
)

class RefactoringPlannerAgent(BaseRefactoringAgent):
    """重构规划智能体 - 生成原子化重构方案"""
    
    async def _execute_impl(self, input_data: Dict[str, Any]) -> List[RefactoringPlan]:
        """生成重构方案"""
        debts: List[TechnicalDebt] = input_data.get("debts", [])
        risk_assessments = input_data.get("risk_assessments", {})
        
        plans = []
        
        for debt in debts:
            # 跳过高风险实体的自动重构
            entity_id = debt.entity_id
            if entity_id:
                risk = risk_assessments.get(entity_id)
                if risk and risk.risk_level in [RiskLevel.P0_CRITICAL]:
                    continue
            
            plan = await self._generate_plan(debt, risk_assessments.get(entity_id))
            if plan:
                plans.append(plan)
        
        # 按风险等级排序（低风险优先）
        plans.sort(key=lambda p: [RiskLevel.P3_EDGE, RiskLevel.P2_COMMON, 
                                   RiskLevel.P1_IMPORTANT, RiskLevel.P0_CRITICAL].index(p.risk_level))
        
        return plans
    
    async def _generate_plan(
        self, 
        debt: TechnicalDebt, 
        risk_assessment: Any
    ) -> RefactoringPlan:
        """生成单个重构方案"""
        action_type = self._determine_action_type(debt)
        
        # 使用LLM生成详细方案
        if self.llm_client:
            return await self._generate_plan_with_llm(debt, action_type, risk_assessment)
        
        # 回退到模板生成
        return self._generate_plan_template(debt, action_type, risk_assessment)
    
    def _determine_action_type(self, debt: TechnicalDebt) -> RefactoringActionType:
        """根据债务类型确定重构动作"""
        mapping = {
            "high_complexity": RefactoringActionType.SPLIT_FUNCTION,
            "dead_code": RefactoringActionType.REMOVE_DEAD_CODE,
            "duplication": RefactoringActionType.EXTRACT_METHOD,
            "missing_tests": RefactoringActionType.COMPLETE_COMMENT,
        }
        return mapping.get(debt.debt_type, RefactoringActionType.FORMAT_CODE)
```

---

## 五、核心流程设计

### 5.1 主流程

```python
# src/refactoring/core/refactoring_orchestrator.py

from typing import Dict, Any
from pathlib import Path
import asyncio

from src.refactoring.core.knowledge_loader import KnowledgeLoader
from src.refactoring.core.context_assembler import ContextAssembler
from src.refactoring.agent.risk_assessment_agent import RiskAssessmentAgent
from src.refactoring.agent.debt_scanner_agent import DebtScannerAgent
from src.refactoring.agent.semantic_contract_agent import SemanticContractAgent
from src.refactoring.agent.refactoring_planner_agent import RefactoringPlannerAgent
from src.refactoring.agent.suggestion_generator_agent import SuggestionGeneratorAgent
from src.refactoring.output.report_generator import ReportGenerator

class RefactoringOrchestrator:
    """重构分析编排器 - 主入口"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        
        # 初始化组件
        self.knowledge_loader = KnowledgeLoader(config)
        self.context_assembler = ContextAssembler(config)
        self.report_generator = ReportGenerator(config)
        
        # 初始化Agent
        self.risk_agent = RiskAssessmentAgent("risk_assessment", config)
        self.debt_agent = DebtScannerAgent("debt_scanner", config)
        self.contract_agent = SemanticContractAgent("semantic_contract", config)
        self.planner_agent = RefactoringPlannerAgent("refactoring_planner", config)
        self.suggestion_agent = SuggestionGeneratorAgent("suggestion_generator", config)
    
    async def analyze(
        self, 
        doc_dir: str, 
        knowledge_graph_path: str,
        output_dir: str = "output/refactoring"
    ) -> Dict[str, Any]:
        """执行重构分析全流程"""
        
        print("=" * 60)
        print("重构分析智能体启动")
        print("=" * 60)
        
        # 阶段1：加载知识上下文
        print("\n[1/6] 加载知识上下文...")
        knowledge_context = self.knowledge_loader.load(doc_dir, knowledge_graph_path)
        self._set_agents_context(knowledge_context)
        
        # 阶段2：风险评估
        print("[2/6] 执行风险评估...")
        risk_assessments = await self.risk_agent.execute(None)
        risk_summary = self._summarize_risks(risk_assessments.data)
        
        # 阶段3：技术债务扫描
        print("[3/6] 扫描技术债务...")
        debts = await self.debt_agent.execute(None)
        
        # 阶段4：业务契约提取
        print("[4/6] 提取业务契约...")
        contracts = await self.contract_agent.execute(None)
        
        # 阶段5：生成重构方案
        print("[5/6] 生成重构方案...")
        plans = await self.planner_agent.execute({
            "debts": debts.data,
            "risk_assessments": {r.entity_id: r for r in risk_assessments.data}
        })
        
        # 阶段6：生成改进建议
        print("[6/6] 生成改进建议...")
        suggestions = await self.suggestion_agent.execute({
            "risk_assessments": risk_assessments.data,
            "debts": debts.data,
            "contracts": contracts.data
        })
        
        # 生成报告
        print("\n生成重构分析报告...")
        report = self.report_generator.generate({
            "knowledge_context": knowledge_context,
            "risk_assessments": risk_assessments.data,
            "debts": debts.data,
            "contracts": contracts.data,
            "plans": plans.data,
            "suggestions": suggestions.data
        })
        
        # 保存报告
        self.report_generator.save(report, output_dir)
        
        print("\n" + "=" * 60)
        print("重构分析完成！")
        print(f"输出目录: {output_dir}")
        print("=" * 60)
        
        return report
    
    def _set_agents_context(self, context):
        """设置所有Agent的知识上下文"""
        for agent in [self.risk_agent, self.debt_agent, 
                      self.contract_agent, self.planner_agent, 
                      self.suggestion_agent]:
            agent.set_knowledge_context(context)
```

### 5.2 知识加载器

```python
# src/refactoring/core/knowledge_loader.py

from typing import Dict, Any
from pathlib import Path
import json
from src.refactoring.core.refactoring_task import KnowledgeContext

class KnowledgeLoader:
    """知识加载器 - 加载知识提取智能体的输出"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
    
    def load(self, doc_dir: str, knowledge_graph_path: str) -> KnowledgeContext:
        """加载知识上下文"""
        doc_path = Path(doc_dir)
        
        # 加载知识图谱
        with open(knowledge_graph_path, 'r', encoding='utf-8') as f:
            kg_data = json.load(f)
        
        # 加载文档
        project_overview = self._load_doc(doc_path / "design" / "project_overview.md")
        business_flow = self._load_doc(doc_path / "business" / "business_flow.md")
        architecture_design = self._load_doc(doc_path / "architecture" / "architecture_design.md")
        feature_modules = self._load_doc(doc_path / "design" / "feature_modules.md")
        api_reference = self._load_doc(doc_path / "design" / "api_reference.md")
        
        # 提取项目信息
        project_info = self._extract_project_info(project_overview)
        
        return KnowledgeContext(
            project_name=project_info.get("name", "Unknown"),
            project_description=project_info.get("description", ""),
            tech_stack=project_info.get("tech_stack", []),
            entities=kg_data.get("entities", {}),
            relationships=kg_data.get("relationships", []),
            project_overview=project_overview,
            business_flow=business_flow,
            architecture_design=architecture_design,
            feature_modules=feature_modules,
            api_reference=api_reference,
            total_entities=len(kg_data.get("entities", {})),
            total_relationships=len(kg_data.get("relationships", [])),
            total_files=len(set(e.get("file_path") for e in kg_data.get("entities", {}).values()))
        )
    
    def _load_doc(self, path: Path) -> str:
        """加载文档内容"""
        if path.exists():
            content = path.read_text(encoding='utf-8')
            # 移除YAML前置元数据
            if content.startswith('---'):
                parts = content.split('---', 2)
                if len(parts) >= 3:
                    return parts[2].strip()
            return content
        return ""
    
    def _extract_project_info(self, overview: str) -> Dict[str, Any]:
        """从项目概述提取项目信息"""
        info = {}
        lines = overview.split('\n')
        for line in lines[:50]:
            if line.startswith('#'):
                info["name"] = line.lstrip('#').strip()
            if '技术栈' in line or '技术' in line:
                info["tech_stack"] = []
        return info
```

---

## 六、输出产物设计

### 6.1 输出目录结构

```
output/refactoring/
├── report/
│   ├── index.md                    # 总索引
│   ├── risk_assessment.md          # 风险评估报告
│   ├── technical_debts.md          # 技术债务清单
│   ├── semantic_contracts.md       # 业务契约清单
│   ├── refactoring_plans.md        # 重构方案列表
│   └── improvement_suggestions.md  # 改进建议
├── data/
│   ├── risk_assessments.json       # 风险评估数据
│   ├── technical_debts.json        # 技术债务数据
│   ├── semantic_contracts.json     # 业务契约数据
│   ├── refactoring_plans.json      # 重构方案数据
│   └── improvement_suggestions.json # 改进建议数据
├── visualization/
│   ├── risk_heatmap.html           # 风险热力图
│   ├── dependency_graph.html       # 依赖关系图
│   └── debt_distribution.html      # 债务分布图
└── execution/
    ├── priority_queue.json         # 执行优先队列
    └── validation_checklist.md     # 验证检查清单
```

### 6.2 报告格式示例

```markdown
# 重构分析报告

## 项目概况

- **项目名称**: stock_datacenter
- **分析时间**: 2026-03-13 15:00:00
- **实体总数**: 5954
- **关系总数**: 6484

## 风险评估摘要

| 风险等级 | 数量 | 占比 | 重构策略 |
|---------|------|------|----------|
| P0 核心模块 | 23 | 0.4% | 禁止AI全自动重构 |
| P1 重要模块 | 156 | 2.6% | 需人工审核 |
| P2 通用模块 | 892 | 15.0% | AI可全自动重构 |
| P3 边缘模块 | 4883 | 82.0% | 可评估下线 |

## 高风险实体TOP10

| 排名 | 实体名 | 文件路径 | 风险分数 | 原因 |
|------|--------|----------|----------|------|
| 1 | TradeService | portal/service/TradeService.java | 92 | 核心交易业务 |
| 2 | PaymentHandler | portal/handler/PaymentHandler.java | 88 | 支付核心链路 |
| ... | ... | ... | ... | ... |

## 技术债务统计

| 类型 | 数量 | 严重程度 |
|------|------|----------|
| 高复杂度代码 | 45 | High |
| 死代码 | 128 | Low |
| 循环依赖 | 3 | High |
| 缺少测试 | 234 | Medium |

## 执行建议

### 第一阶段：P3边缘模块清理（预计2周）
- 清理128个死代码
- 移除未使用的导入

### 第二阶段：P2通用模块重构（预计4周）
- 拆分45个高复杂度函数
- 消除循环依赖

### 第三阶段：P1重要模块优化（预计3周）
- 补充测试覆盖
- 优化代码结构
```

---

## 七、与现有系统集成

### 7.1 集成到知识提取流程

```python
# 修改 src/core/knowledge_extractor.py

class KnowledgeExtractor:
    def extract_from_directory(
        self,
        directory_path: str,
        include_code: bool = True,
        include_docs: bool = True,
        force: bool = False,
        deep_analysis: bool = True,
        refactoring_analysis: bool = False  # 新增参数
    ) -> KnowledgeGraph:
        """从指定目录提取结构化知识"""
        
        # ... 现有代码 ...
        
        # 保存结果
        self._save_knowledge_graph(graph, directory_path, deep_analysis)
        
        # 新增：重构分析
        if refactoring_analysis:
            self._run_refactoring_analysis(graph, output_dir)
        
        return graph
    
    def _run_refactoring_analysis(self, graph: KnowledgeGraph, output_dir: str):
        """运行重构分析"""
        from src.refactoring.core.refactoring_orchestrator import RefactoringOrchestrator
        
        print("\n启动重构分析...")
        orchestrator = RefactoringOrchestrator(self.config)
        
        import asyncio
        asyncio.run(orchestrator.analyze(
            doc_dir=f"{output_dir}/doc",
            knowledge_graph_path=f"{output_dir}/knowledge_graph.json",
            output_dir=f"{output_dir}/refactoring"
        ))
```

### 7.2 命令行入口

```python
# 新增 run_refactoring_analysis.py

#!/usr/bin/env python
"""运行重构分析 - 基于已生成的知识文档"""

import sys
import asyncio
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from src.refactoring.core.refactoring_orchestrator import RefactoringOrchestrator
import yaml

async def main():
    # 加载配置
    with open("config/config.yaml", "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)
    
    # 运行分析
    orchestrator = RefactoringOrchestrator(config)
    
    await orchestrator.analyze(
        doc_dir="output/doc/stock_datacenter",
        knowledge_graph_path="output/knowledge_stock_datacenter/knowledge_graph.json",
        output_dir="output/refactoring/stock_datacenter"
    )

if __name__ == "__main__":
    asyncio.run(main())
```

---

## 八、开发计划

### 8.1 开发阶段

| 阶段 | 内容 | 预计时间 |
|------|------|----------|
| **Phase 1** | 基础设施搭建 | 2天 |
| - KnowledgeLoader | 知识加载器 | |
| - ContextAssembler | 上下文组装器 | |
| - RefactoringOrchestrator | 编排器 | |
| **Phase 2** | 核心Agent开发 | 3天 |
| - RiskAssessmentAgent | 风险评估 | |
| - DebtScannerAgent | 债务扫描 | |
| - SemanticContractAgent | 契约提取 | |
| **Phase 3** | 方案生成开发 | 2天 |
| - RefactoringPlannerAgent | 重构规划 | |
| - SuggestionGeneratorAgent | 建议生成 | |
| - ReportGenerator | 报告生成 | |
| **Phase 4** | 集成测试 | 1天 |
| - 与知识提取集成 | | |
| - 全流程测试 | | |

### 8.2 技术要点

1. **知识复用**：完全复用知识提取输出，避免重复扫描
2. **增量分析**：支持增量执行，只分析变更部分
3. **LLM优化**：控制Token消耗，优先分析核心实体
4. **风险控制**：严格按P0-P3分级，确保业务契约不变

---

## 九、总结

本设计文档定义了重构分析智能体的完整架构：

1. **输入复用**：完全复用知识提取智能体的输出
2. **Agent架构**：6个核心Agent协作完成重构分析
3. **风险控制**：P0-P3分级管理，确保业务安全
4. **输出产物**：完整的重构报告和执行计划

下一步：按开发计划逐步实现各模块。
