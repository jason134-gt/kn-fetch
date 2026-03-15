"""
差异评估器

对比重构前后的代码变更，生成详细的差异评估报告
"""

import json
import logging
import subprocess
from pathlib import Path
from typing import Dict, List, Optional, Any
from datetime import datetime
from dataclasses import dataclass, field

logger = logging.getLogger(__name__)


@dataclass
class CodeStatistics:
    """代码统计"""
    files: int = 0
    total_lines: int = 0
    code_lines: int = 0
    comment_lines: int = 0
    blank_lines: int = 0
    classes: int = 0
    functions: int = 0
    imports: int = 0
    avg_complexity: float = 0.0
    max_complexity: int = 0
    doc_coverage: float = 0.0


@dataclass
class QualityMetrics:
    """质量指标"""
    code_duplication: float = 0.0
    test_coverage: float = 0.0
    technical_debt_count: int = 0
    security_issues: int = 0
    code_smells: int = 0
    maintainability_index: float = 0.0


@dataclass
class TestResults:
    """测试结果"""
    total_tests: int = 0
    passed: int = 0
    failed: int = 0
    skipped: int = 0
    duration: float = 0.0
    coverage: float = 0.0
    failures: List[Dict] = field(default_factory=list)


@dataclass
class RiskAssessment:
    """风险评估"""
    risk_type: str
    level: str  # high, medium, low
    description: str
    mitigation: str


@dataclass
class DiffReport:
    """差异评估报告"""
    report_id: str
    session_id: str
    generated_at: datetime
    
    # 代码变更统计
    stats_before: CodeStatistics
    stats_after: CodeStatistics
    
    # 质量指标
    quality_before: QualityMetrics
    quality_after: QualityMetrics
    
    # 测试结果
    test_results: TestResults
    
    # 风险评估
    risks: List[RiskAssessment]
    
    # 建议
    recommendations: List[str]
    
    # 变更文件列表
    changed_files: List[Dict]
    
    # 总体评价
    overall_assessment: str
    improvement_score: float  # 0-100


class DiffEvaluator:
    """差异评估器"""
    
    def __init__(self, project_path: str):
        self.project_path = Path(project_path)
    
    def evaluate(
        self,
        session_id: str,
        before_snapshot: Dict,
        after_snapshot: Dict,
        test_results: Dict = None
    ) -> DiffReport:
        """执行差异评估"""
        
        # 1. 统计代码变更
        stats_before = self._calculate_stats(before_snapshot)
        stats_after = self._calculate_stats(after_snapshot)
        
        # 2. 计算质量指标
        quality_before = self._calculate_quality(before_snapshot)
        quality_after = self._calculate_quality(after_snapshot)
        
        # 3. 处理测试结果
        test_res = self._process_test_results(test_results)
        
        # 4. 风险评估
        risks = self._assess_risks(
            stats_before, stats_after,
            quality_before, quality_after,
            test_res
        )
        
        # 5. 生成建议
        recommendations = self._generate_recommendations(
            stats_before, stats_after,
            quality_before, quality_after,
            test_res, risks
        )
        
        # 6. 计算改进分数
        improvement_score = self._calculate_improvement_score(
            stats_before, stats_after,
            quality_before, quality_after,
            test_res
        )
        
        # 7. 生成总体评价
        overall = self._generate_overall_assessment(
            improvement_score, risks, test_res
        )
        
        # 8. 获取变更文件列表
        changed_files = self._get_changed_files(before_snapshot, after_snapshot)
        
        return DiffReport(
            report_id=f"DIFF-{datetime.now().strftime('%Y%m%d%H%M%S')}",
            session_id=session_id,
            generated_at=datetime.now(),
            stats_before=stats_before,
            stats_after=stats_after,
            quality_before=quality_before,
            quality_after=quality_after,
            test_results=test_res,
            risks=risks,
            recommendations=recommendations,
            changed_files=changed_files,
            overall_assessment=overall,
            improvement_score=improvement_score
        )
    
    def _calculate_stats(self, snapshot: Dict) -> CodeStatistics:
        """计算代码统计"""
        entities = snapshot.get("entities", {})
        
        stats = CodeStatistics()
        
        # 统计文件数
        files = set()
        for entity in entities.values():
            file_path = entity.get("file_path")
            if file_path:
                files.add(file_path)
        stats.files = len(files)
        
        # 统计实体
        for entity in entities.values():
            entity_type = entity.get("entity_type", "")
            
            if entity_type == "class":
                stats.classes += 1
            elif entity_type in ["function", "method"]:
                stats.functions += 1
            elif entity_type == "import":
                stats.imports += 1
            
            # 行数
            loc = entity.get("lines_of_code", 0) or 0
            stats.total_lines += loc
            stats.code_lines += loc
            
            # 复杂度
            complexity = entity.get("complexity")
            if complexity:
                stats.avg_complexity += complexity
                stats.max_complexity = max(stats.max_complexity, complexity)
            
            # 文档覆盖
            if entity.get("docstring"):
                stats.doc_coverage += 1
        
        # 平均复杂度
        if stats.functions > 0:
            stats.avg_complexity /= stats.functions
            stats.doc_coverage = (stats.doc_coverage / stats.functions) * 100
        
        return stats
    
    def _calculate_quality(self, snapshot: Dict) -> QualityMetrics:
        """计算质量指标"""
        metrics = QualityMetrics()
        
        entities = snapshot.get("entities", {})
        
        # 统计技术债务
        for entity in entities.values():
            if not entity.get("docstring"):
                if entity.get("entity_type") in ["class", "function", "method"]:
                    metrics.technical_debt_count += 1
            
            complexity = entity.get("complexity", 0) or 0
            if complexity > 15:
                metrics.code_smells += 1
        
        # 简化的可维护性指数计算
        total_entities = len(entities)
        if total_entities > 0:
            good_entities = sum(
                1 for e in entities.values()
                if e.get("docstring") and (e.get("complexity", 0) or 0) <= 10
            )
            metrics.maintainability_index = (good_entities / total_entities) * 100
        
        return metrics
    
    def _process_test_results(self, test_results: Dict) -> TestResults:
        """处理测试结果"""
        if not test_results:
            return TestResults()
        
        results = TestResults()
        results.total_tests = test_results.get("total", 0)
        results.passed = test_results.get("passed", 0)
        results.failed = test_results.get("failed", 0)
        results.skipped = test_results.get("skipped", 0)
        results.duration = test_results.get("duration", 0.0)
        results.coverage = test_results.get("coverage", 0.0)
        results.failures = test_results.get("failures", [])
        
        return results
    
    def _assess_risks(
        self,
        stats_before: CodeStatistics,
        stats_after: CodeStatistics,
        quality_before: QualityMetrics,
        quality_after: QualityMetrics,
        test_results: TestResults
    ) -> List[RiskAssessment]:
        """风险评估"""
        risks = []
        
        # 功能回归风险
        if test_results.failed > 0:
            risks.append(RiskAssessment(
                risk_type="功能回归",
                level="中" if test_results.failed <= 2 else "高",
                description=f"有 {test_results.failed} 个测试失败",
                mitigation="检查失败测试并修复相关问题"
            ))
        else:
            risks.append(RiskAssessment(
                risk_type="功能回归",
                level="低",
                description="所有测试通过",
                mitigation="无需特殊处理"
            ))
        
        # 复杂度变化风险
        if stats_after.avg_complexity > stats_before.avg_complexity:
            risks.append(RiskAssessment(
                risk_type="复杂度增加",
                level="中",
                description=f"平均复杂度从 {stats_before.avg_complexity:.1f} 增加到 {stats_after.avg_complexity:.1f}",
                mitigation="审查新增代码，考虑进一步重构"
            ))
        
        # 测试覆盖率风险
        if test_results.coverage < 70:
            risks.append(RiskAssessment(
                risk_type="测试覆盖不足",
                level="中",
                description=f"测试覆盖率仅 {test_results.coverage:.1f}%",
                mitigation="增加单元测试以提高覆盖率"
            ))
        
        # 文档风险
        if stats_after.doc_coverage < stats_before.doc_coverage:
            risks.append(RiskAssessment(
                risk_type="文档覆盖下降",
                level="低",
                description="文档覆盖率有所下降",
                mitigation="为新代码添加文档注释"
            ))
        
        return risks
    
    def _generate_recommendations(
        self,
        stats_before: CodeStatistics,
        stats_after: CodeStatistics,
        quality_before: QualityMetrics,
        quality_after: QualityMetrics,
        test_results: TestResults,
        risks: List[RiskAssessment]
    ) -> List[str]:
        """生成建议"""
        recommendations = []
        
        # 测试相关
        if test_results.failed > 0:
            recommendations.append(f"处理 {test_results.failed} 个失败的测试用例")
        
        if test_results.coverage < 80:
            recommendations.append(f"提高测试覆盖率至80%以上（当前 {test_results.coverage:.1f}%）")
        
        # 复杂度相关
        if stats_after.avg_complexity > 10:
            recommendations.append("继续降低代码复杂度，目标平均复杂度 < 10")
        
        # 文档相关
        if stats_after.doc_coverage < 90:
            recommendations.append(f"补充文档注释，目标覆盖率 > 90%（当前 {stats_after.doc_coverage:.1f}%）")
        
        # 技术债务
        if quality_after.technical_debt_count > 0:
            recommendations.append(f"清理剩余 {quality_after.technical_debt_count} 项技术债务")
        
        # 风险相关
        high_risks = [r for r in risks if r.level == "高"]
        if high_risks:
            recommendations.append(f"优先处理 {len(high_risks)} 个高风险项")
        
        return recommendations
    
    def _calculate_improvement_score(
        self,
        stats_before: CodeStatistics,
        stats_after: CodeStatistics,
        quality_before: QualityMetrics,
        quality_after: QualityMetrics,
        test_results: TestResults
    ) -> float:
        """计算改进分数"""
        score = 50.0  # 基础分
        
        # 复杂度改进 (+20)
        if stats_after.avg_complexity < stats_before.avg_complexity:
            improvement = (stats_before.avg_complexity - stats_after.avg_complexity) / stats_before.avg_complexity
            score += min(20, improvement * 40)
        
        # 文档覆盖率改进 (+15)
        if stats_after.doc_coverage > stats_before.doc_coverage:
            improvement = stats_after.doc_coverage - stats_before.doc_coverage
            score += min(15, improvement * 0.3)
        
        # 测试通过率 (+15)
        if test_results.total_tests > 0:
            pass_rate = test_results.passed / test_results.total_tests
            score += pass_rate * 15
        
        # 技术债务减少 (+10)
        if quality_after.technical_debt_count < quality_before.technical_debt_count:
            reduction = quality_before.technical_debt_count - quality_after.technical_debt_count
            score += min(10, reduction * 0.5)
        
        return min(100, max(0, score))
    
    def _generate_overall_assessment(
        self,
        improvement_score: float,
        risks: List[RiskAssessment],
        test_results: TestResults
    ) -> str:
        """生成总体评价"""
        high_risks = sum(1 for r in risks if r.level == "高")
        medium_risks = sum(1 for r in risks if r.level == "中")
        
        if improvement_score >= 80 and high_risks == 0:
            return "重构成功，代码质量显著提升"
        elif improvement_score >= 60 and high_risks <= 1:
            return "重构基本成功，存在少量待处理项"
        elif improvement_score >= 40:
            return "重构部分成功，需要进一步优化"
        else:
            return "重构效果不理想，建议回滚并重新评估"
    
    def _get_changed_files(
        self,
        before_snapshot: Dict,
        after_snapshot: Dict
    ) -> List[Dict]:
        """获取变更文件列表"""
        changed = []
        
        before_files = set()
        after_files = set()
        
        for entity in before_snapshot.get("entities", {}).values():
            file_path = entity.get("file_path")
            if file_path:
                before_files.add(file_path)
        
        for entity in after_snapshot.get("entities", {}).values():
            file_path = entity.get("file_path")
            if file_path:
                after_files.add(file_path)
        
        # 新增文件
        for f in (after_files - before_files):
            changed.append({"file": f, "status": "added"})
        
        # 删除文件
        for f in (before_files - after_files):
            changed.append({"file": f, "status": "deleted"})
        
        # 修改文件
        for f in (before_files & after_files):
            changed.append({"file": f, "status": "modified"})
        
        return changed
    
    def generate_report_markdown(self, report: DiffReport) -> str:
        """生成Markdown格式报告"""
        lines = [
            "# 重构差异评估报告",
            "",
            f"**报告ID**: {report.report_id}",
            f"**会话ID**: {report.session_id}",
            f"**生成时间**: {report.generated_at.strftime('%Y-%m-%d %H:%M:%S')}",
            "",
            "---",
            "",
            "## 一、代码变更统计",
            "",
            "| 指标 | 变更前 | 变更后 | 差异 |",
            "|------|--------|--------|------|",
            f"| 文件数 | {report.stats_before.files} | {report.stats_after.files} | {report.stats_after.files - report.stats_before.files:+d} |",
            f"| 总行数 | {report.stats_before.total_lines} | {report.stats_after.total_lines} | {report.stats_after.total_lines - report.stats_before.total_lines:+d} |",
            f"| 类数量 | {report.stats_before.classes} | {report.stats_after.classes} | {report.stats_after.classes - report.stats_before.classes:+d} |",
            f"| 函数数量 | {report.stats_before.functions} | {report.stats_after.functions} | {report.stats_after.functions - report.stats_before.functions:+d} |",
            f"| 平均复杂度 | {report.stats_before.avg_complexity:.1f} | {report.stats_after.avg_complexity:.1f} | {report.stats_after.avg_complexity - report.stats_before.avg_complexity:+.1f} |",
            f"| 文档覆盖率 | {report.stats_before.doc_coverage:.1f}% | {report.stats_after.doc_coverage:.1f}% | {report.stats_after.doc_coverage - report.stats_before.doc_coverage:+.1f}% |",
            "",
            "---",
            "",
            "## 二、质量指标对比",
            "",
            "| 指标 | 变更前 | 变更后 | 改进 |",
            "|------|--------|--------|------|",
            f"| 技术债务数 | {report.quality_before.technical_debt_count} | {report.quality_after.technical_debt_count} | {report.quality_after.technical_debt_count - report.quality_before.technical_debt_count:+d} |",
            f"| 代码异味数 | {report.quality_before.code_smells} | {report.quality_after.code_smells} | {report.quality_after.code_smells - report.quality_before.code_smells:+d} |",
            f"| 可维护性指数 | {report.quality_before.maintainability_index:.1f} | {report.quality_after.maintainability_index:.1f} | {report.quality_after.maintainability_index - report.quality_before.maintainability_index:+.1f} |",
            "",
            "---",
            "",
            "## 三、测试结果",
            "",
            f"- **总测试数**: {report.test_results.total_tests}",
            f"- **通过**: {report.test_results.passed}",
            f"- **失败**: {report.test_results.failed}",
            f"- **跳过**: {report.test_results.skipped}",
            f"- **覆盖率**: {report.test_results.coverage:.1f}%",
            ""
        ]
        
        if report.test_results.failures:
            lines.extend([
                "### 失败的测试",
                ""
            ])
            for failure in report.test_results.failures[:5]:
                lines.append(f"- {failure.get('name', 'Unknown')}: {failure.get('message', '')}")
        
        lines.extend([
            "",
            "---",
            "",
            "## 四、风险评估",
            "",
            "| 风险类型 | 等级 | 描述 | 缓解措施 |",
            "|----------|------|------|----------|"
        ])
        
        for risk in report.risks:
            lines.append(f"| {risk.risk_type} | {risk.level} | {risk.description} | {risk.mitigation} |")
        
        lines.extend([
            "",
            "---",
            "",
            "## 五、改进建议",
            ""
        ])
        
        for i, rec in enumerate(report.recommendations, 1):
            lines.append(f"{i}. {rec}")
        
        lines.extend([
            "",
            "---",
            "",
            "## 六、总体评价",
            "",
            f"**改进分数**: {report.improvement_score:.1f}/100",
            "",
            f"**评价**: {report.overall_assessment}",
            "",
            "---",
            "",
            f"*本报告由重构智能体自动生成*"
        ])
        
        return "\n".join(lines)


__all__ = [
    'CodeStatistics',
    'QualityMetrics',
    'TestResults',
    'RiskAssessment',
    'DiffReport',
    'DiffEvaluator'
]
