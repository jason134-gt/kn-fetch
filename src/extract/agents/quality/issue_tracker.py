"""
质量问题跟踪系统 - 建立问题检测和修复机制

功能：
1. 质量问题检测和分类
2. 问题优先级管理
3. 自动修复建议生成
4. 问题跟踪和状态管理
5. 质量趋势分析

设计目标：
- 建立完整的问题生命周期管理
- 提供智能的修复建议
- 支持问题的批量处理和自动修复
- 实现质量趋势的可视化分析
"""

import re
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
import json
from pathlib import Path


class IssueSeverity(Enum):
    """问题严重性级别"""
    CRITICAL = "critical"      # 严重问题，必须立即修复
    HIGH = "high"              # 高优先级问题，建议尽快修复
    MEDIUM = "medium"          # 中等优先级问题，需要关注
    LOW = "low"                # 低优先级问题，可以延后处理
    INFO = "info"              # 信息性提示，无需立即处理


class IssueType(Enum):
    """问题类型"""
    SYNTAX_ERROR = "syntax_error"              # 语法错误
    LOGIC_ERROR = "logic_error"                # 逻辑错误
    PERFORMANCE_ISSUE = "performance_issue"    # 性能问题
    SECURITY_ISSUE = "security_issue"          # 安全问题
    STYLE_VIOLATION = "style_violation"        # 代码风格违规
    ARCHITECTURE_ISSUE = "architecture_issue"  # 架构问题
    DOCUMENTATION_ISSUE = "documentation_issue" # 文档问题
    TEST_COVERAGE_ISSUE = "test_coverage_issue" # 测试覆盖问题


class IssueStatus(Enum):
    """问题状态"""
    OPEN = "open"                  # 新发现的问题
    IN_PROGRESS = "in_progress"    # 正在处理中
    RESOLVED = "resolved"          # 已解决
    CLOSED = "closed"              # 已关闭
    WONT_FIX = "wont_fix"          # 不会修复


@dataclass
class QualityIssue:
    """质量问题定义"""
    id: str                          # 问题唯一标识
    type: IssueType                  # 问题类型
    severity: IssueSeverity          # 严重性级别
    status: IssueStatus              # 当前状态
    title: str                       # 问题标题
    description: str                 # 详细描述
    location: Dict[str, Any]         # 问题位置信息
    detection_time: datetime         # 检测时间
    assignee: Optional[str] = None   # 负责人
    priority: int = 0                # 优先级分数
    tags: List[str] = field(default_factory=list)  # 标签
    evidence: List[str] = field(default_factory=list)  # 证据
    suggested_fixes: List[str] = field(default_factory=list)  # 修复建议
    related_issues: List[str] = field(default_factory=list)  # 相关问题
    history: List[Dict[str, Any]] = field(default_factory=list)  # 历史记录


@dataclass
class IssueTrackerStats:
    """问题跟踪统计"""
    total_issues: int = 0
    open_issues: int = 0
    resolved_issues: int = 0
    critical_issues: int = 0
    high_priority_issues: int = 0
    issues_by_type: Dict[str, int] = field(default_factory=dict)
    issues_by_severity: Dict[str, int] = field(default_factory=dict)
    average_resolution_time: float = 0.0
    resolution_rate: float = 0.0


class IssueDetector:
    """问题检测器"""
    
    def __init__(self):
        # 问题检测规则
        self.detection_rules = self._build_detection_rules()
        
        # 严重性评分规则
        self.severity_rules = self._build_severity_rules()
    
    def _build_detection_rules(self) -> List[Dict[str, Any]]:
        """构建问题检测规则"""
        return [
            # 语法错误检测规则
            {
                "type": IssueType.SYNTAX_ERROR,
                "name": "unclosed_quotes",
                "pattern": r'"[^"\n]*$',
                "description": "未闭合的引号",
                "severity": IssueSeverity.CRITICAL
            },
            {
                "type": IssueType.SYNTAX_ERROR,
                "name": "unclosed_brackets",
                "pattern": r'\([^)\n]*$',
                "description": "未闭合的括号",
                "severity": IssueSeverity.CRITICAL
            },
            {
                "type": IssueType.SYNTAX_ERROR,
                "name": "missing_semicolon",
                "pattern": r'^[^;\n]*$',
                "description": "缺失分号",
                "severity": IssueSeverity.HIGH
            },
            
            # 逻辑错误检测规则
            {
                "type": IssueType.LOGIC_ERROR,
                "name": "infinite_loop",
                "pattern": r'while\s*\(\s*true\s*\)|for\s*\(\s*;\s*;\s*\)',
                "description": "潜在的死循环",
                "severity": IssueSeverity.HIGH
            },
            {
                "type": IssueType.LOGIC_ERROR,
                "name": "null_check",
                "pattern": r'[^!=]=null|null[^!=]',
                "description": "空值检查缺失",
                "severity": IssueSeverity.MEDIUM
            },
            
            # 性能问题检测规则
            {
                "type": IssueType.PERFORMANCE_ISSUE,
                "name": "nested_loops",
                "pattern": r'for\s*\([^}]*for[^}]*for[^}]*\)',
                "description": "三层嵌套循环，可能影响性能",
                "severity": IssueSeverity.MEDIUM
            },
            
            # 代码风格违规检测规则
            {
                "type": IssueType.STYLE_VIOLATION,
                "name": "long_function",
                "pattern": r'function\s+\w+\s*\([^}]{500,}',
                "description": "函数长度过长",
                "severity": IssueSeverity.LOW
            },
            {
                "type": IssueType.STYLE_VIOLATION,
                "name": "magic_number",
                "pattern": r'\b\d{3,}\b',
                "description": "魔法数值",
                "severity": IssueSeverity.LOW
            }
        ]
    
    def _build_severity_rules(self) -> Dict[str, Any]:
        """构建严重性评分规则"""
        return {
            "impact_factors": {
                "security": 10,
                "correctness": 8,
                "performance": 6,
                "maintainability": 4,
                "style": 2
            },
            "likelihood_factors": {
                "certain": 10,
                "likely": 8,
                "possible": 6,
                "unlikely": 4,
                "rare": 2
            }
        }
    
    def detect_issues(self, code_content: str, file_path: str) -> List[QualityIssue]:
        """检测代码中的问题"""
        issues = []
        
        # 按行分析代码
        lines = code_content.split('\n')
        
        for line_num, line in enumerate(lines, 1):
            line_issues = self._analyze_line(line, line_num, file_path)
            issues.extend(line_issues)
        
        # 分析代码结构问题
        structural_issues = self._analyze_structure(code_content, file_path)
        issues.extend(structural_issues)
        
        return issues
    
    def _analyze_line(self, line: str, line_num: int, file_path: str) -> List[QualityIssue]:
        """分析单行代码"""
        issues = []
        
        for rule in self.detection_rules:
            matches = re.finditer(rule["pattern"], line)
            
            for match in matches:
                issue = QualityIssue(
                    id=f"{file_path}:{line_num}:{rule['name']}",
                    type=rule["type"],
                    severity=rule["severity"],
                    status=IssueStatus.OPEN,
                    title=rule["description"],
                    description=f"在{file_path}第{line_num}行发现{rule['description']}",
                    location={
                        "file_path": file_path,
                        "line_number": line_num,
                        "column_start": match.start(),
                        "column_end": match.end(),
                        "code_snippet": line.strip()
                    },
                    detection_time=datetime.now(),
                    priority=self._calculate_priority(rule["severity"])
                )
                
                # 添加修复建议
                issue.suggested_fixes = self._generate_fixes(rule, line)
                issues.append(issue)
        
        return issues
    
    def _analyze_structure(self, code_content: str, file_path: str) -> List[QualityIssue]:
        """分析代码结构问题"""
        issues = []
        
        # 检测长函数
        function_pattern = r'function\s+\w+\s*\([^}]{300,}'
        if re.search(function_pattern, code_content):
            issue = QualityIssue(
                id=f"{file_path}:structure:long_function",
                type=IssueType.STYLE_VIOLATION,
                severity=IssueSeverity.MEDIUM,
                status=IssueStatus.OPEN,
                title="函数过长",
                description=f"{file_path}中的函数长度超过300字符，建议拆分",
                location={"file_path": file_path},
                detection_time=datetime.now(),
                priority=60
            )
            issue.suggested_fixes = ["将长函数拆分为多个小函数", "提取重复代码为独立函数"]
            issues.append(issue)
        
        # 检测复杂条件判断
        complex_condition_pattern = r'if\s*\([^)]{100,}'
        if re.search(complex_condition_pattern, code_content):
            issue = QualityIssue(
                id=f"{file_path}:structure:complex_condition",
                type=IssueType.LOGIC_ERROR,
                severity=IssueSeverity.MEDIUM,
                status=IssueStatus.OPEN,
                title="复杂条件判断",
                description=f"{file_path}中的条件判断过于复杂",
                location={"file_path": file_path},
                detection_time=datetime.now(),
                priority=70
            )
            issue.suggested_fixes = ["提取复杂条件为独立函数", "使用卫语句简化条件判断"]
            issues.append(issue)
        
        return issues
    
    def _calculate_priority(self, severity: IssueSeverity) -> int:
        """计算问题优先级"""
        priority_map = {
            IssueSeverity.CRITICAL: 100,
            IssueSeverity.HIGH: 80,
            IssueSeverity.MEDIUM: 60,
            IssueSeverity.LOW: 40,
            IssueSeverity.INFO: 20
        }
        return priority_map.get(severity, 0)
    
    def _generate_fixes(self, rule: Dict[str, Any], line: str) -> List[str]:
        """生成修复建议"""
        fixes = []
        
        rule_name = rule["name"]
        
        if rule_name == "unclosed_quotes":
            fixes.append("在行末添加闭合引号")
            fixes.append("检查字符串拼接逻辑")
        elif rule_name == "unclosed_brackets":
            fixes.append("检查括号配对，确保每个左括号都有对应的右括号")
        elif rule_name == "missing_semicolon":
            fixes.append("在语句末尾添加分号")
        elif rule_name == "infinite_loop":
            fixes.append("添加循环终止条件")
            fixes.append("使用break语句退出循环")
        elif rule_name == "null_check":
            fixes.append("添加空值检查")
            fixes.append("使用可选链操作符避免空指针异常")
        
        return fixes


class IssueFixer:
    """问题修复器"""
    
    def __init__(self):
        self.fix_strategies = self._build_fix_strategies()
    
    def _build_fix_strategies(self) -> Dict[str, Any]:
        """构建修复策略"""
        return {
            "syntax_errors": {
                "unclosed_quotes": self._fix_unclosed_quotes,
                "unclosed_brackets": self._fix_unclosed_brackets,
                "missing_semicolon": self._fix_missing_semicolon
            },
            "style_violations": {
                "long_function": self._fix_long_function,
                "magic_number": self._fix_magic_number
            }
        }
    
    def auto_fix_issue(self, issue: QualityIssue, original_code: str) -> str:
        """自动修复问题"""
        # 根据问题类型选择修复策略
        issue_type = issue.type.value
        rule_name = issue.id.split(':')[-1]
        
        if issue_type in self.fix_strategies:
            strategies = self.fix_strategies[issue_type]
            if rule_name in strategies:
                return strategies[rule_name](issue, original_code)
        
        # 默认返回原代码
        return original_code
    
    def _fix_unclosed_quotes(self, issue: QualityIssue, code: str) -> str:
        """修复未闭合的引号"""
        lines = code.split('\n')
        line_num = issue.location["line_number"] - 1
        
        if 0 <= line_num < len(lines):
            lines[line_num] = lines[line_num].rstrip() + '"'
        
        return '\n'.join(lines)
    
    def _fix_unclosed_brackets(self, issue: QualityIssue, code: str) -> str:
        """修复未闭合的括号"""
        lines = code.split('\n')
        line_num = issue.location["line_number"] - 1
        
        if 0 <= line_num < len(lines):
            line = lines[line_num]
            # 在行末添加闭合括号
            if '(' in line and ')' not in line:
                lines[line_num] = line.rstrip() + ')'
        
        return '\n'.join(lines)
    
    def _fix_missing_semicolon(self, issue: QualityIssue, code: str) -> str:
        """修复缺失的分号"""
        lines = code.split('\n')
        line_num = issue.location["line_number"] - 1
        
        if 0 <= line_num < len(lines):
            line = lines[line_num].strip()
            if line and not line.endswith(';') and not line.endswith('{') and not line.endswith('}'):
                lines[line_num] = lines[line_num].rstrip() + ';'
        
        return '\n'.join(lines)
    
    def _fix_long_function(self, issue: QualityIssue, code: str) -> str:
        """修复长函数问题"""
        # 长函数需要手动重构，这里只提供建议
        return code
    
    def _fix_magic_number(self, issue: QualityIssue, code: str) -> str:
        """修复魔法数值"""
        # 魔法数值需要定义常量，这里只提供建议
        return code


class IssueTracker:
    """质量问题跟踪系统"""
    
    def __init__(self, storage_path: Optional[str] = None):
        self.issues: Dict[str, QualityIssue] = {}
        self.detector = IssueDetector()
        self.fixer = IssueFixer()
        self.storage_path = Path(storage_path) if storage_path else Path("quality_issues.json")
        
        # 加载已有问题
        self._load_issues()
    
    def scan_codebase(self, code_files: Dict[str, str]) -> List[QualityIssue]:
        """扫描代码库，检测质量问题"""
        all_issues = []
        
        for file_path, content in code_files.items():
            file_issues = self.detector.detect_issues(content, file_path)
            all_issues.extend(file_issues)
            
            # 将问题添加到跟踪系统
            for issue in file_issues:
                self.add_issue(issue)
        
        # 保存问题记录
        self._save_issues()
        
        return all_issues
    
    def add_issue(self, issue: QualityIssue):
        """添加问题到跟踪系统"""
        # 检查是否已存在相同问题
        if issue.id not in self.issues:
            self.issues[issue.id] = issue
            
            # 记录添加历史
            issue.history.append({
                "action": "detected",
                "timestamp": datetime.now(),
                "status": issue.status.value
            })
    
    def update_issue_status(self, issue_id: str, new_status: IssueStatus, assignee: Optional[str] = None):
        """更新问题状态"""
        if issue_id in self.issues:
            issue = self.issues[issue_id]
            old_status = issue.status
            issue.status = new_status
            
            if assignee:
                issue.assignee = assignee
            
            # 记录状态变更历史
            issue.history.append({
                "action": "status_change",
                "timestamp": datetime.now(),
                "old_status": old_status.value,
                "new_status": new_status.value,
                "assignee": assignee
            })
            
            self._save_issues()
    
    def get_issue(self, issue_id: str) -> Optional[QualityIssue]:
        """获取特定问题"""
        return self.issues.get(issue_id)
    
    def get_issues_by_filter(self, filters: Dict[str, Any]) -> List[QualityIssue]:
        """根据过滤器获取问题列表"""
        filtered_issues = []
        
        for issue in self.issues.values():
            match = True
            
            if "status" in filters and issue.status != filters["status"]:
                match = False
            if "severity" in filters and issue.severity != filters["severity"]:
                match = False
            if "type" in filters and issue.type != filters["type"]:
                match = False
            if "priority_min" in filters and issue.priority < filters["priority_min"]:
                match = False
            
            if match:
                filtered_issues.append(issue)
        
        # 按优先级排序
        filtered_issues.sort(key=lambda x: x.priority, reverse=True)
        
        return filtered_issues
    
    def auto_fix_issues(self, code_files: Dict[str, str]) -> Dict[str, str]:
        """自动修复可修复的问题"""
        fixed_files = code_files.copy()
        
        for file_path, content in code_files.items():
            # 获取该文件的所有可修复问题
            file_issues = [issue for issue in self.issues.values() 
                          if issue.location.get("file_path") == file_path 
                          and issue.status == IssueStatus.OPEN]
            
            fixed_content = content
            for issue in file_issues:
                # 尝试自动修复
                try:
                    fixed_content = self.fixer.auto_fix_issue(issue, fixed_content)
                    
                    # 标记问题为已修复
                    self.update_issue_status(issue.id, IssueStatus.RESOLVED, "auto_fixer")
                except Exception as e:
                    print(f"自动修复失败 {issue.id}: {e}")
            
            fixed_files[file_path] = fixed_content
        
        return fixed_files
    
    def generate_report(self) -> Dict[str, Any]:
        """生成质量报告"""
        stats = self._calculate_statistics()
        
        report = {
            "summary": {
                "total_issues": stats.total_issues,
                "open_issues": stats.open_issues,
                "resolved_issues": stats.resolved_issues,
                "critical_issues": stats.critical_issues,
                "resolution_rate": stats.resolution_rate
            },
            "issues_by_type": stats.issues_by_type,
            "issues_by_severity": stats.issues_by_severity,
            "top_issues": self._get_top_issues(10),
            "recommendations": self._generate_recommendations(stats)
        }
        
        return report
    
    def _calculate_statistics(self) -> IssueTrackerStats:
        """计算统计信息"""
        stats = IssueTrackerStats()
        
        stats.total_issues = len(self.issues)
        
        for issue in self.issues.values():
            # 统计状态
            if issue.status == IssueStatus.OPEN:
                stats.open_issues += 1
            elif issue.status in [IssueStatus.RESOLVED, IssueStatus.CLOSED]:
                stats.resolved_issues += 1
            
            # 统计严重性
            if issue.severity == IssueSeverity.CRITICAL:
                stats.critical_issues += 1
            if issue.priority >= 80:
                stats.high_priority_issues += 1
            
            # 按类型统计
            issue_type = issue.type.value
            stats.issues_by_type[issue_type] = stats.issues_by_type.get(issue_type, 0) + 1
            
            # 按严重性统计
            severity = issue.severity.value
            stats.issues_by_severity[severity] = stats.issues_by_severity.get(severity, 0) + 1
        
        # 计算解决率
        if stats.total_issues > 0:
            stats.resolution_rate = round(stats.resolved_issues / stats.total_issues * 100, 2)
        
        return stats
    
    def _get_top_issues(self, limit: int) -> List[QualityIssue]:
        """获取优先级最高的问题"""
        open_issues = [issue for issue in self.issues.values() 
                      if issue.status == IssueStatus.OPEN]
        
        # 按优先级排序
        open_issues.sort(key=lambda x: x.priority, reverse=True)
        
        return open_issues[:limit]
    
    def _generate_recommendations(self, stats: IssueTrackerStats) -> List[str]:
        """生成修复建议"""
        recommendations = []
        
        if stats.critical_issues > 0:
            recommendations.append(f"发现{stats.critical_issues}个严重问题，建议立即处理")
        
        if stats.open_issues > 10:
            recommendations.append(f"未处理问题较多({stats.open_issues}个)，建议制定修复计划")
        
        if stats.resolution_rate < 50:
            recommendations.append("问题解决率较低，建议加强质量管理")
        
        # 基于问题类型的建议
        for issue_type, count in stats.issues_by_type.items():
            if count > 5:
                recommendations.append(f"{issue_type}类型问题较多({count}个)，建议重点检查")
        
        return recommendations
    
    def _save_issues(self):
        """保存问题到文件"""
        try:
            issues_data = []
            for issue in self.issues.values():
                issue_data = {
                    "id": issue.id,
                    "type": issue.type.value,
                    "severity": issue.severity.value,
                    "status": issue.status.value,
                    "title": issue.title,
                    "description": issue.description,
                    "location": issue.location,
                    "detection_time": issue.detection_time.isoformat(),
                    "assignee": issue.assignee,
                    "priority": issue.priority,
                    "tags": issue.tags,
                    "evidence": issue.evidence,
                    "suggested_fixes": issue.suggested_fixes,
                    "related_issues": issue.related_issues,
                    "history": issue.history
                }
                issues_data.append(issue_data)
            
            with open(self.storage_path, 'w', encoding='utf-8') as f:
                json.dump(issues_data, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"保存问题失败: {e}")
    
    def _load_issues(self):
        """从文件加载问题"""
        if not self.storage_path.exists():
            return
        
        try:
            with open(self.storage_path, 'r', encoding='utf-8') as f:
                issues_data = json.load(f)
            
            for issue_data in issues_data:
                issue = QualityIssue(
                    id=issue_data["id"],
                    type=IssueType(issue_data["type"]),
                    severity=IssueSeverity(issue_data["severity"]),
                    status=IssueStatus(issue_data["status"]),
                    title=issue_data["title"],
                    description=issue_data["description"],
                    location=issue_data["location"],
                    detection_time=datetime.fromisoformat(issue_data["detection_time"]),
                    assignee=issue_data.get("assignee"),
                    priority=issue_data["priority"],
                    tags=issue_data.get("tags", []),
                    evidence=issue_data.get("evidence", []),
                    suggested_fixes=issue_data.get("suggested_fixes", []),
                    related_issues=issue_data.get("related_issues", []),
                    history=issue_data.get("history", [])
                )
                self.issues[issue.id] = issue
        except Exception as e:
            print(f"加载问题失败: {e}")


# 使用示例
def test_issue_tracker():
    """测试问题跟踪系统"""
    # 创建跟踪器
    tracker = IssueTracker()
    
    # 模拟代码文件
    test_files = {
        "example.js": """
function test() {
    var x = 10  // 缺少分号
    if (x > 5 {
        console.log('Hello'  // 未闭合引号和括号
    }
}
"""
    }
    
    # 扫描代码
    issues = tracker.scan_codebase(test_files)
    
    print(f"检测到 {len(issues)} 个问题：")
    for issue in issues:
        print(f"- [{issue.severity.value}] {issue.title}: {issue.description}")
    
    # 生成报告
    report = tracker.generate_report()
    print("\n质量报告：")
    print(f"总问题数: {report['summary']['total_issues']}")
    print(f"未处理问题: {report['summary']['open_issues']}")
    print(f"解决率: {report['summary']['resolution_rate']}%")
    
    # 尝试自动修复
    fixed_files = tracker.auto_fix_issues(test_files)
    print("\n修复后的代码：")
    print(fixed_files["example.js"])


if __name__ == "__main__":
    test_issue_tracker()