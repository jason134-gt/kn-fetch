"""
重构分析编排器 - 协调所有Agent完成重构分析全流程

主入口，协调知识加载、上下文组装、Agent执行和报告生成
"""

import asyncio
import json
import logging
from pathlib import Path
from typing import Dict, Any, List, Optional
from datetime import datetime

from .knowledge_loader import KnowledgeLoader
from .context_assembler import ContextAssembler
from .refactoring_task import (
    KnowledgeContext,
    RefactoringReport,
    RiskAssessment,
    TechnicalDebt,
    BusinessContract,
    RefactoringPlan,
    ImprovementSuggestion,
    RiskLevel
)
from .enhanced_report_generator import EnhancedReportGenerator

logger = logging.getLogger(__name__)


class RefactoringOrchestrator:
    """重构分析编排器 - 协调所有Agent完成重构分析全流程"""
    
    def __init__(self, config: Dict[str, Any] = None):
        self.config = config or {}
        
        # 初始化核心组件
        self.knowledge_loader = KnowledgeLoader(config)
        self.context_assembler = ContextAssembler(config)
        
        # Agent将在后续阶段注入
        self.risk_agent = None
        self.debt_agent = None
        self.contract_agent = None
        self.planner_agent = None
        self.suggestion_agent = None
        
        # LLM客户端
        self.llm_client = None
    
    def set_llm_client(self, llm_client):
        """设置LLM客户端"""
        self.llm_client = llm_client
    
    def set_agents(self, risk_agent=None, debt_agent=None, contract_agent=None, 
                   planner_agent=None, suggestion_agent=None):
        """设置Agent实例"""
        self.risk_agent = risk_agent
        self.debt_agent = debt_agent
        self.contract_agent = contract_agent
        self.planner_agent = planner_agent
        self.suggestion_agent = suggestion_agent
    
    async def analyze(
        self,
        doc_dir: str,
        knowledge_graph_path: str,
        output_dir: str = "output/refactoring"
    ) -> RefactoringReport:
        """
        执行重构分析全流程
        
        Args:
            doc_dir: 文档目录路径
            knowledge_graph_path: 知识图谱文件路径
            output_dir: 输出目录
            
        Returns:
            RefactoringReport对象
        """
        print("\n" + "=" * 60)
        print("重构分析智能体启动")
        print("=" * 60)
        
        # 阶段1：加载知识上下文
        print("\n[1/6] 加载知识上下文...")
        knowledge_context = self.knowledge_loader.load(doc_dir, knowledge_graph_path)
        
        # 阶段2：风险评估
        print("[2/6] 执行风险评估...")
        risk_assessments = await self._run_risk_assessment(knowledge_context)
        risk_summary = self._summarize_risks(risk_assessments)
        
        # 阶段3：技术债务扫描
        print("[3/6] 扫描技术债务...")
        debts = await self._run_debt_scanner(knowledge_context)
        debt_summary = self._summarize_debts(debts)
        
        # 阶段4：业务契约提取
        print("[4/6] 提取业务契约...")
        contracts = await self._run_contract_extraction(knowledge_context)
        
        # 阶段5：生成重构方案
        print("[5/6] 生成重构方案...")
        plans = await self._run_refactoring_planner(
            knowledge_context, risk_assessments, debts
        )
        
        # 阶段6：生成改进建议
        print("[6/6] 生成改进建议...")
        suggestions = await self._run_suggestion_generator(
            knowledge_context, risk_assessments, debts, contracts
        )
        
        # 生成增强版报告
        print("\n生成重构分析报告...")
        enhanced_generator = EnhancedReportGenerator()
        enhanced_report = enhanced_generator.generate(
            knowledge_context=knowledge_context,
            risk_assessments=risk_assessments,
            debts=debts,
            contracts=contracts,
            plans=plans
        )
        
        # 保存增强版报告
        enhanced_generator.save_report(enhanced_report, output_dir)
        
        # 生成基础报告（兼容旧接口）
        report = self._generate_report(
            knowledge_context=knowledge_context,
            risk_assessments=risk_assessments,
            debts=debts,
            contracts=contracts,
            plans=plans,
            suggestions=suggestions
        )
        
        print("\n" + "=" * 60)
        print("重构分析完成！")
        print(f"输出目录: {output_dir}")
        print("=" * 60)
        
        return report
    
    async def _run_risk_assessment(self, context: KnowledgeContext) -> List[RiskAssessment]:
        """执行风险评估"""
        if self.risk_agent:
            result = await self.risk_agent.execute(context)
            return result.data if result.success else []
        
        # 使用内置逻辑
        return self._assess_risks_builtin(context)
    
    async def _run_debt_scanner(self, context: KnowledgeContext) -> List[TechnicalDebt]:
        """执行债务扫描"""
        if self.debt_agent:
            result = await self.debt_agent.execute(context)
            return result.data if result.success else []
        
        return self._scan_debts_builtin(context)
    
    async def _run_contract_extraction(self, context: KnowledgeContext) -> Dict[str, BusinessContract]:
        """执行契约提取"""
        if self.contract_agent:
            result = await self.contract_agent.execute(context)
            return result.data if result.success else {}
        
        return self._extract_contracts_builtin(context)
    
    async def _run_refactoring_planner(
        self, 
        context: KnowledgeContext,
        assessments: List[RiskAssessment],
        debts: List[TechnicalDebt]
    ) -> List[RefactoringPlan]:
        """执行重构规划"""
        if self.planner_agent:
            result = await self.planner_agent.execute({
                "context": context,
                "assessments": assessments,
                "debts": debts
            })
            return result.data if result.success else []
        
        return self._plan_refactoring_builtin(assessments, debts)
    
    async def _run_suggestion_generator(
        self,
        context: KnowledgeContext,
        assessments: List[RiskAssessment],
        debts: List[TechnicalDebt],
        contracts: Dict[str, BusinessContract]
    ) -> List[ImprovementSuggestion]:
        """执行建议生成"""
        if self.suggestion_agent:
            result = await self.suggestion_agent.execute({
                "context": context,
                "assessments": assessments,
                "debts": debts,
                "contracts": contracts
            })
            return result.data if result.success else []
        
        return self._generate_suggestions_builtin(assessments, debts)
    
    # ===== 内置实现（当Agent未注入时使用） =====
    
    def _assess_risks_builtin(self, context: KnowledgeContext) -> List[RiskAssessment]:
        """内置风险评估逻辑"""
        assessments = []
        
        for entity_id, entity in context.entities.items():
            assessment = self._assess_single_entity(entity, context)
            assessments.append(assessment)
        
        return assessments
    
    def _assess_single_entity(self, entity: Dict, context: KnowledgeContext) -> RiskAssessment:
        """评估单个实体风险"""
        score = 0
        reasons = []
        
        name = entity.get("name", "").lower()
        file_path = entity.get("file_path", "").lower()
        
        # 业务核心性
        core_keywords = ["trade", "payment", "order", "transaction", "交易", "支付", "订单"]
        for kw in core_keywords:
            if kw in name or kw in file_path:
                score += 40
                reasons.append(f"核心业务关键词: {kw}")
                break
        
        # 调用频率
        caller_count = 0
        for rel in context.relationships:
            if rel.get("target") == entity.get("id") and rel.get("type") in ["calls", "CALLS"]:
                caller_count += 1
        
        if caller_count > 10:
            score += 30
            reasons.append(f"被调用次数多: {caller_count}")
        elif caller_count > 5:
            score += 15
        
        # 复杂度
        complexity = entity.get("complexity", 0)
        if complexity and complexity > 15:
            score += 20
            reasons.append(f"代码复杂度高: {complexity}")
        
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
            entity_id=entity.get("id", ""),
            entity_name=entity.get("name", ""),
            file_path=entity.get("file_path", ""),
            risk_level=risk_level,
            score=score,
            reasons=reasons
        )
    
    def _scan_debts_builtin(self, context: KnowledgeContext) -> List[TechnicalDebt]:
        """内置债务扫描逻辑"""
        from .refactoring_task import TechnicalDebtType
        
        debts = []
        debt_id = 0
        
        for entity in context.entities.values():
            # 高复杂度
            complexity = entity.get("complexity", 0)
            if complexity and complexity > 15:
                debt_id += 1
                debts.append(TechnicalDebt(
                    debt_id=f"debt_{debt_id}",
                    debt_type=TechnicalDebtType.HIGH_COMPLEXITY,
                    severity="high" if complexity > 25 else "medium",
                    entity_id=entity.get("id"),
                    entity_name=entity.get("name"),
                    file_path=entity.get("file_path", ""),
                    description=f"代码复杂度过高 ({complexity})",
                    suggestion="考虑拆分函数或方法"
                ))
        
            # 大文件
            loc = entity.get("lines_of_code", 0)
            if loc and loc > 300:
                debt_id += 1
                debts.append(TechnicalDebt(
                    debt_id=f"debt_{debt_id}",
                    debt_type=TechnicalDebtType.CODE_SMELL,
                    severity="medium",
                    entity_id=entity.get("id"),
                    entity_name=entity.get("name"),
                    file_path=entity.get("file_path", ""),
                    description=f"代码行数过多 ({loc}行)",
                    suggestion="考虑拆分为多个文件/类"
                ))
        
            # 缺少文档
            if not entity.get("docstring") and entity.get("entity_type") in ["function", "method", "class"]:
                debt_id += 1
                debts.append(TechnicalDebt(
                    debt_id=f"debt_{debt_id}",
                    debt_type=TechnicalDebtType.MISSING_TESTS,
                    severity="low",
                    entity_id=entity.get("id"),
                    entity_name=entity.get("name"),
                    file_path=entity.get("file_path", ""),
                    description="缺少文档注释",
                    suggestion="添加docstring说明功能和参数"
                ))
        
        return debts
    
    def _extract_contracts_builtin(self, context: KnowledgeContext) -> Dict[str, BusinessContract]:
        """内置契约提取逻辑"""
        contracts = {}
        
        for entity_id, entity in list(context.entities.items())[:50]:
            contracts[entity_id] = BusinessContract(
                entity_id=entity_id,
                entity_name=entity.get("name", ""),
                business_summary=entity.get("docstring", "")[:200] if entity.get("docstring") else "",
                input_contract={},
                output_contract={}
            )
        
        return contracts
    
    def _plan_refactoring_builtin(
        self, 
        assessments: List[RiskAssessment],
        debts: List[TechnicalDebt]
    ) -> List[RefactoringPlan]:
        """内置重构规划逻辑"""
        from .refactoring_task import RefactoringActionType
        
        plans = []
        
        for debt in debts[:20]:
            # 只为P2和P3级别的实体生成方案
            assessment = next((a for a in assessments if a.entity_id == debt.entity_id), None)
            if assessment and assessment.risk_level in [RiskLevel.P2_COMMON, RiskLevel.P3_EDGE]:
                action_type = self._determine_action_type(debt)
                if action_type:
                    plans.append(RefactoringPlan(
                        entity_id=debt.entity_id or "",
                        entity_name=debt.entity_name or "",
                        action_type=action_type,
                        risk_level=assessment.risk_level if assessment else RiskLevel.P2_COMMON,
                        changes_description=debt.suggestion,
                        validation_criteria=["功能不变", "测试通过"]
                    ))
        
        return plans
    
    def _determine_action_type(self, debt: TechnicalDebt):
        """根据债务类型确定重构动作"""
        from .refactoring_task import TechnicalDebtType, RefactoringActionType
        
        mapping = {
            TechnicalDebtType.HIGH_COMPLEXITY: RefactoringActionType.SPLIT_FUNCTION,
            TechnicalDebtType.DEAD_CODE: RefactoringActionType.REMOVE_DEAD_CODE,
            TechnicalDebtType.DUPLICATION: RefactoringActionType.EXTRACT_METHOD,
            TechnicalDebtType.CODE_SMELL: RefactoringActionType.FORMAT_CODE,
            TechnicalDebtType.MISSING_TESTS: RefactoringActionType.COMPLETE_COMMENT,
        }
        
        return mapping.get(debt.debt_type, RefactoringActionType.FORMAT_CODE)
    
    def _generate_suggestions_builtin(
        self, 
        assessments: List[RiskAssessment],
        debts: List[TechnicalDebt]
    ) -> List[ImprovementSuggestion]:
        """内置建议生成逻辑"""
        suggestions = []
        
        # 统计风险分布
        p0_count = sum(1 for a in assessments if a.risk_level == RiskLevel.P0_CRITICAL)
        p1_count = sum(1 for a in assessments if a.risk_level == RiskLevel.P1_IMPORTANT)
        
        if p0_count > 0 or p1_count > 10:
            suggestions.append(ImprovementSuggestion(
                category="architecture",
                priority="high",
                title="核心模块需要重点关注",
                description=f"发现{p0_count}个P0核心模块和{p1_count}个P1重要模块",
                rationale="核心模块风险高，需要优先进行测试覆盖和代码审查",
                implementation_steps=[
                    "为核心模块补充完整的单元测试",
                    "建立代码审查机制",
                    "制定重构计划"
                ]
            ))
        
        if len(debts) > 50:
            suggestions.append(ImprovementSuggestion(
                category="code_quality",
                priority="high",
                title="技术债务需要清理",
                description=f"发现{len(debts)}项技术债务",
                rationale="技术债务会影响代码质量和可维护性",
                implementation_steps=[
                    "按优先级清理技术债务",
                    "建立债务跟踪机制",
                    "定期进行代码质量检查"
                ]
            ))
        
        return suggestions
    
    def _summarize_risks(self, assessments: List[RiskAssessment]) -> Dict[str, int]:
        """汇总风险分布"""
        summary = {}
        for level in RiskLevel:
            summary[level.value] = sum(1 for a in assessments if a.risk_level == level)
        return summary
    
    def _summarize_debts(self, debts: List[TechnicalDebt]) -> Dict[str, Any]:
        """汇总债务分布"""
        by_type = {}
        by_severity = {}
        
        for debt in debts:
            dtype = debt.debt_type.value
            by_type[dtype] = by_type.get(dtype, 0) + 1
            
            severity = debt.severity
            by_severity[severity] = by_severity.get(severity, 0) + 1
        
        return {
            "by_type": by_type,
            "by_severity": by_severity,
            "total": len(debts)
        }
    
    def _generate_report(
        self,
        knowledge_context: KnowledgeContext,
        risk_assessments: List[RiskAssessment],
        debts: List[TechnicalDebt],
        contracts: Dict[str, BusinessContract],
        plans: List[RefactoringPlan],
        suggestions: List[ImprovementSuggestion]
    ) -> RefactoringReport:
        """生成重构报告"""
        
        # 高风险实体
        high_risk = sorted(
            [a for a in risk_assessments if a.risk_level in [RiskLevel.P0_CRITICAL, RiskLevel.P1_IMPORTANT]],
            key=lambda x: x.score,
            reverse=True
        )[:10]
        
        # 债务统计
        debt_summary = self._summarize_debts(debts)
        
        # 执行优先级
        execution_priority = [
            p.entity_id for p in sorted(plans, key=lambda x: [
                RiskLevel.P3_EDGE, RiskLevel.P2_COMMON, RiskLevel.P1_IMPORTANT, RiskLevel.P0_CRITICAL
            ].index(x.risk_level))
        ]
        
        return RefactoringReport(
            project_name=knowledge_context.project_name,
            risk_summary=self._summarize_risks(risk_assessments),
            high_risk_entities=high_risk,
            total_debts=len(debts),
            debt_by_type=debt_summary["by_type"],
            debt_by_severity=debt_summary["by_severity"],
            top_debts=sorted(debts, key=lambda d: ["critical", "high", "medium", "low"].index(d.severity))[:20],
            improvement_suggestions=suggestions,
            refactoring_plans=plans,
            execution_priority=execution_priority
        )
    
    def _save_report(self, report: RefactoringReport, output_dir: str):
        """保存报告"""
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # 保存JSON
        data_dir = output_path / "data"
        data_dir.mkdir(exist_ok=True)
        
        with open(data_dir / "report.json", "w", encoding="utf-8") as f:
            # 使用 model_dump(mode='json') 处理 datetime 序列化
            data = report.model_dump(mode='json') if hasattr(report, 'model_dump') else report.dict()
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        # 保存Markdown
        md_content = self._generate_markdown_report(report)
        with open(output_path / "report.md", "w", encoding="utf-8") as f:
            f.write(md_content)
        
        print(f"  已保存到: {output_path}")
    
    def _generate_markdown_report(self, report: RefactoringReport) -> str:
        """生成Markdown格式报告"""
        lines = []
        
        lines.append("# 重构分析报告\n")
        lines.append(f"\n项目: {report.project_name}\n")
        lines.append(f"生成时间: {report.generated_at.strftime('%Y-%m-%d %H:%M:%S')}\n")
        
        # 风险评估
        lines.append("\n## 一、风险评估\n")
        for level, count in report.risk_summary.items():
            lines.append(f"- {level}: {count}个实体\n")
        
        # 高风险实体
        if report.high_risk_entities:
            lines.append("\n### 高风险实体 TOP 10\n")
            for i, e in enumerate(report.high_risk_entities[:10], 1):
                lines.append(f"{i+1}. **{e.entity_name}** ({e.file_path})\n")
                lines.append(f"   - 风险等级: {e.risk_level.value}, 分数: {e.score:.1f}\n")
        
        # 技术债务
        lines.append(f"\n## 二、技术债务\n")
        lines.append(f"总计: {report.total_debts} 项\n")
        
        lines.append("\n### 按类型分布\n")
        for dtype, count in report.debt_by_type.items():
            lines.append(f"- {dtype}: {count}项\n")
        
        lines.append("\n### 按严重程度分布\n")
        for severity, count in report.debt_by_severity.items():
            lines.append(f"- {severity}: {count}项\n")
        
        # 改进建议
        if report.improvement_suggestions:
            lines.append("\n## 三、改进建议\n")
            for i, s in enumerate(report.improvement_suggestions[:10], 1):
                lines.append(f"\n### {i+1}. {s.title}\n")
                lines.append(f"- 类别: {s.category}\n")
                lines.append(f"- 优先级: {s.priority}\n")
                lines.append(f"- 描述: {s.description}\n")
        
        # 重构方案
        if report.refactoring_plans:
            lines.append(f"\n## 四、重构方案 (共{len(report.refactoring_plans)}个)\n")
            for i, p in enumerate(report.refactoring_plans[:20], 1):
                lines.append(f"{i+1}. **{p.entity_name}** - {p.action_type.value}\n")
                lines.append(f"   - 风险等级: {p.risk_level.value}\n")
                lines.append(f"   - 变更描述: {p.changes_description}\n")
        
        # 执行建议
        lines.append("\n## 五、执行建议\n")
        lines.append("1. **阶段一**: P3边缘模块清理（低风险）\n")
        lines.append("2. **阶段二**: P2通用模块优化\n")
        lines.append("3. **阶段三**: P1重要模块审查\n")
        lines.append("4. **阶段四**: P0核心模块专项处理\n")
        
        return "\n".join(lines)
    
    def run(self, doc_dir: str, knowledge_graph_path: str, output_dir: str = "output/refactoring") -> RefactoringReport:
        """同步运行分析"""
        return asyncio.run(self.analyze(doc_dir, knowledge_graph_path, output_dir))
