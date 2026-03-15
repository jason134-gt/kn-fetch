"""
质量模块 - 建立完善的质量保证体系

包含：
- 质量问题跟踪系统
- 问题检测和修复机制
- 质量评估和报告生成

设计目标：
- 建立完整的问题生命周期管理
- 提供智能的修复建议
- 支持问题的批量处理和自动修复
- 实现质量趋势的可视化分析
"""

from .issue_tracker import (
    IssueTracker,
    QualityIssue,
    IssueSeverity,
    IssueType,
    IssueStatus,
    IssueDetector,
    IssueFixer,
    IssueTrackerStats
)

__all__ = [
    "IssueTracker",
    "QualityIssue", 
    "IssueSeverity",
    "IssueType",
    "IssueStatus",
    "IssueDetector",
    "IssueFixer",
    "IssueTrackerStats"
]