"""
报告重生成器 - 根据用户反馈重新生成重构分析报告
"""

from typing import Dict, Any, List, Optional
from pathlib import Path
import logging
from datetime import datetime
import json

from .problem_detail import (
    ProblemDetail, ProblemSummary, ProblemType, Severity,
    EnhancedRefactoringReport
)
from .feedback_processor import (
    FeedbackProcessor, UserFeedback, FeedbackBatch,
    FeedbackType, FeedbackStatus
)

logger = logging.getLogger(__name__)


class ReportChange:
    """报告变更记录"""
    
    def __init__(self):
        self.added_problems: List[Dict] = []
        self.removed_problems: List[str] = []
        self.modified_problems: Dict[str, Dict] = {}
        self.added_solutions: Dict[str, List[Dict]] = {}
        self.modified_solutions: Dict[str, Dict] = {}
        self.ignored_problems: List[str] = []
        self.user_comments: Dict[str, str] = {}
        self.confirmed_problems: List[str] = []
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "added_problems": len(self.added_problems),
            "removed_problems": len(self.removed_problems),
            "modified_problems": len(self.modified_problems),
            "added_solutions": sum(len(v) for v in self.added_solutions.values()),
            "modified_solutions": len(self.modified_solutions),
            "ignored_problems": len(self.ignored_problems),
            "confirmed_problems": len(self.confirmed_problems),
            "details": {
                "removed": self.removed_problems,
                "modified": list(self.modified_problems.keys()),
                "added": [p.get("problem_id") for p in self.added_problems],
                "ignored": self.ignored_problems,
                "confirmed": self.confirmed_problems
            }
        }


class ReportRegenerator:
    """报告重生成器"""
    
    def __init__(self):
        self.feedback_processor = FeedbackProcessor()
    
    def regenerate(
        self,
        original_report: EnhancedRefactoringReport,
        feedback_batch: FeedbackBatch,
        knowledge_context: Dict[str, Any] = None
    ) -> EnhancedRefactoringReport:
        """
        根据用户反馈重新生成报告
        
        Args:
            original_report: 原始报告
            feedback_batch: 反馈批次
            knowledge_context: 知识上下文（可选）
            
        Returns:
            重新生成的报告
        """
        logger.info(f"开始重新生成报告，反馈数量: {len(feedback_batch.feedbacks)}")
        
        # 1. 构建处理上下文
        context = self._build_context(original_report, knowledge_context or {})
        
        # 2. 处理所有反馈
        change_tracker = ReportChange()
        results = self.feedback_processor.process_batch(feedback_batch, context)
        
        # 3. 应用变更
        for i, result in enumerate(results):
            feedback = feedback_batch.feedbacks[i]
            self._apply_change(result, change_tracker, feedback)
        
        # 4. 生成新报告
        new_report = self._create_new_report(
            original_report, change_tracker, feedback_batch
        )
        
        logger.info(f"报告重生成完成: {self._summarize_changes(change_tracker)}")
        
        return new_report
    
    def _build_context(
        self,
        report: EnhancedRefactoringReport,
        knowledge_context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """构建处理上下文"""
        problems = {}
        solutions = {}
        
        for problem in report.problems:
            problems[problem.problem_id] = problem.model_dump()
            for i, solution in enumerate(problem.solutions):
                solutions[f"{problem.problem_id}_s{i}"] = solution
        
        return {
            "report": report.model_dump() if hasattr(report, 'model_dump') else report.dict(),
            "problems": problems,
            "solutions": solutions,
            "knowledge_context": knowledge_context,
            "entities": knowledge_context.get("entities", {}),
            "relationships": knowledge_context.get("relationships", [])
        }
    
    def _apply_change(
        self,
        result: Dict[str, Any],
        change_tracker: ReportChange,
        feedback: UserFeedback
    ):
        """应用单个变更"""
        action = result.get("action")
        problem_id = result.get("problem_id")
        
        if action == "remove_problem":
            change_tracker.removed_problems.append(problem_id)
            if result.get("add_to_ignore_list"):
                change_tracker.ignored_problems.append(problem_id)
                
        elif action == "confirm_problem":
            change_tracker.confirmed_problems.append(problem_id)
                
        elif action == "modify_problem":
            change_tracker.modified_problems[problem_id] = result.get("changes", {})
            if feedback.content:
                change_tracker.user_comments[problem_id] = feedback.content
                
        elif action == "add_problem":
            change_tracker.added_problems.append(result["new_problem"])
            
        elif action == "modify_solution":
            key = f"{problem_id}_{result.get('solution_id', '0')}"
            change_tracker.modified_solutions[key] = result.get("changes", {})
            
        elif action == "add_solution":
            if problem_id not in change_tracker.added_solutions:
                change_tracker.added_solutions[problem_id] = []
            change_tracker.added_solutions[problem_id].append(result["new_solution"])
        
        elif action == "ignore_problem":
            change_tracker.ignored_problems.append(problem_id)
    
    def _create_new_report(
        self,
        original: EnhancedRefactoringReport,
        changes: ReportChange,
        feedback_batch: FeedbackBatch
    ) -> EnhancedRefactoringReport:
        """创建新报告"""
        
        # 复制原始问题列表
        new_problems = []
        problem_map = {p.problem_id: p for p in original.problems}
        
        # 处理移除
        removed_set = set(changes.removed_problems)
        remaining_ids = [pid for pid in problem_map.keys() if pid not in removed_set]
        
        # 处理修改和方案变更
        for problem_id in remaining_ids:
            problem = problem_map[problem_id]
            problem_dict = problem.model_dump() if hasattr(problem, 'model_dump') else problem.dict()
            
            # 应用问题修改
            if problem_id in changes.modified_problems:
                problem_dict.update(changes.modified_problems[problem_id])
            
            # 添加方案
            if problem_id in changes.added_solutions:
                problem_dict["solutions"].extend(changes.added_solutions[problem_id])
            
            # 添加用户评论
            if problem_id in changes.user_comments:
                problem_dict["user_comment"] = changes.user_comments[problem_id]
            
            # 标记确认状态
            if problem_id in changes.confirmed_problems:
                problem_dict["user_confirmed"] = True
            
            # 标记忽略状态
            if problem_id in changes.ignored_problems:
                problem_dict["ignored"] = True
                problem_dict["ignore_reason"] = changes.user_comments.get(problem_id, "")
            
            # 重建问题对象
            new_problem = self._rebuild_problem(problem_dict)
            new_problems.append(new_problem)
        
        # 处理新增问题
        for new_problem_dict in changes.added_problems:
            new_problem = self._rebuild_problem(new_problem_dict)
            new_problems.append(new_problem)
        
        # 计算新统计
        new_summary = self._calculate_summary(new_problems)
        
        # 生成新报告
        new_version = getattr(original, 'version', 1) + 1
        parent_id = getattr(original, 'report_id', None)
        
        new_report = EnhancedRefactoringReport(
            report_id=f"RPT-{datetime.now().strftime('%Y%m%d%H%M%S')}",
            project_name=original.project_name,
            generated_at=datetime.now(),
            summary=new_summary,
            problems=new_problems,
            version=new_version,
            parent_report_id=parent_id,
            feedback_batch_id=feedback_batch.batch_id,
            change_log=changes.to_dict()
        )
        
        return new_report
    
    def _rebuild_problem(self, problem_dict: Dict) -> ProblemDetail:
        """重建问题对象"""
        # 处理 severity - 支持多种格式
        severity = problem_dict.get("severity", "medium")
        if isinstance(severity, str):
            # 映射不同格式的severity
            severity_map = {
                "high": Severity.HIGH,
                "medium": Severity.MEDIUM,
                "low": Severity.LOW,
                "高": Severity.HIGH,
                "中": Severity.MEDIUM,
                "低": Severity.LOW,
                "HIGH": Severity.HIGH,
                "MEDIUM": Severity.MEDIUM,
                "LOW": Severity.LOW,
            }
            severity = severity_map.get(severity.lower() if severity.lower() in ["high", "medium", "low"] else severity, Severity.MEDIUM)
        elif isinstance(severity, Severity):
            pass  # 已经是Severity类型
        else:
            severity = Severity.MEDIUM
        
        return ProblemDetail(
            problem_id=problem_dict.get("problem_id", "P-Unknown"),
            problem_type=problem_dict.get("problem_type", "unknown"),
            severity=severity,
            entity_name=problem_dict.get("entity_name", ""),
            file_path=problem_dict.get("file_path", ""),
            line_start=problem_dict.get("line_start"),
            line_end=problem_dict.get("line_end"),
            description=problem_dict.get("description", ""),
            current_state=problem_dict.get("current_state", {}),
            fix_steps=problem_dict.get("fix_steps", []),
            risks=problem_dict.get("risks", []),
            solutions=problem_dict.get("solutions", []),
            recommended_solution=problem_dict.get("recommended_solution"),
            user_comment=problem_dict.get("user_comment"),
            user_confirmed=problem_dict.get("user_confirmed", False),
            ignored=problem_dict.get("ignored", False)
        )
    
    def _calculate_summary(self, problems: List[ProblemDetail]) -> ProblemSummary:
        """计算汇总"""
        severity_count = {"high": 0, "medium": 0, "low": 0}
        type_count = {}
        total_workload = 0.0
        
        for p in problems:
            # 跳过忽略的问题
            if getattr(p, 'ignored', False):
                continue
                
            severity_val = p.severity.value if hasattr(p.severity, 'value') else str(p.severity)
            severity_count[severity_val] = severity_count.get(severity_val, 0) + 1
            type_count[p.problem_type] = type_count.get(p.problem_type, 0) + 1
            
            if p.solutions:
                # 获取推荐方案的工作量
                rec_idx = p.recommended_solution or 0
                if rec_idx < len(p.solutions):
                    recommended = p.solutions[rec_idx]
                    total_workload += recommended.get("workload_hours", 0)
        
        return ProblemSummary(
            total_problems=len([p for p in problems if not getattr(p, 'ignored', False)]),
            high_severity=severity_count.get("high", 0),
            medium_severity=severity_count.get("medium", 0),
            low_severity=severity_count.get("low", 0),
            estimated_workload_hours=total_workload,
            problem_types=type_count
        )
    
    def _summarize_changes(self, changes: ReportChange) -> str:
        """生成变更摘要"""
        parts = []
        if changes.added_problems:
            parts.append(f"新增{len(changes.added_problems)}个问题")
        if changes.removed_problems:
            parts.append(f"移除{len(changes.removed_problems)}个问题")
        if changes.modified_problems:
            parts.append(f"修改{len(changes.modified_problems)}个问题")
        if changes.added_solutions:
            parts.append(f"新增{sum(len(v) for v in changes.added_solutions.values())}个方案")
        if changes.confirmed_problems:
            parts.append(f"确认{len(changes.confirmed_problems)}个问题")
        if changes.ignored_problems:
            parts.append(f"忽略{len(changes.ignored_problems)}个问题")
        return ", ".join(parts) if parts else "无变更"
    
    def save_report(
        self,
        report: EnhancedRefactoringReport,
        output_dir: Path,
        generate_markdown: bool = True
    ):
        """保存报告"""
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        
        # 保存JSON
        data_dir = output_dir / "data"
        data_dir.mkdir(exist_ok=True)
        
        json_path = data_dir / f"report_v{report.version}.json"
        with open(json_path, "w", encoding="utf-8") as f:
            data = report.model_dump(mode='json') if hasattr(report, 'model_dump') else report.dict()
            json.dump(data, f, ensure_ascii=False, indent=2, default=str)
        
        # 保存Markdown
        if generate_markdown:
            md_content = self._generate_markdown(report)
            md_path = output_dir / f"report_v{report.version}.md"
            with open(md_path, "w", encoding="utf-8") as f:
                f.write(md_content)
        
        logger.info(f"报告已保存: {output_dir}")
    
    def _generate_markdown(self, report: EnhancedRefactoringReport) -> str:
        """生成Markdown报告"""
        lines = [
            f"# 重构分析报告 V{report.version}",
            "",
            f"**项目**: {report.project_name}",
            f"**生成时间**: {report.generated_at.strftime('%Y-%m-%d %H:%M:%S')}",
            f"**基于反馈**: {report.feedback_batch_id or '无'}",
            "",
            "---",
            "",
            "## 一、执行摘要",
            "",
            "| 指标 | 数值 |",
            "|------|------|",
            f"| 问题总数 | {report.summary.total_problems} |",
            f"| 高严重度 | {report.summary.high_severity} |",
            f"| 中严重度 | {report.summary.medium_severity} |",
            f"| 低严重度 | {report.summary.low_severity} |",
            f"| 预计修复工时 | {report.summary.estimated_workload_hours:.1f} 小时 |",
            "",
            "---",
            "",
            "## 二、变更记录",
            ""
        ]
        
        change_log = report.change_log or {}
        if change_log:
            lines.extend([
                "| 变更类型 | 数量 |",
                "|----------|------|",
                f"| 新增问题 | {change_log.get('added_problems', 0)} |",
                f"| 移除问题 | {change_log.get('removed_problems', 0)} |",
                f"| 修改问题 | {change_log.get('modified_problems', 0)} |",
                f"| 新增方案 | {change_log.get('added_solutions', 0)} |",
                f"| 确认问题 | {change_log.get('confirmed_problems', 0)} |",
                f"| 忽略问题 | {change_log.get('ignored_problems', 0)} |",
                "",
                "---",
                ""
            ])
        
        # 问题列表
        lines.extend([
            "## 三、问题清单",
            "",
            "| 编号 | 类型 | 严重程度 | 实体 | 描述 | 状态 |",
            "|------|------|----------|------|------|------|"
        ])
        
        for p in report.problems:
            status = "✅确认" if getattr(p, 'user_confirmed', False) else ("❌忽略" if getattr(p, 'ignored', False) else "待审核")
            desc = p.description[:30] + "..." if len(p.description) > 30 else p.description
            lines.append(f"| {p.problem_id} | {p.problem_type} | {p.severity.value} | {p.entity_name[:15]} | {desc} | {status} |")
        
        return "\n".join(lines)
