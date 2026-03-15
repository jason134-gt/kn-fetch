"""
Mermaid图表验证器 - 提升图表渲染成功率

功能：
1. Mermaid语法验证
2. 图表结构完整性检查
3. 渲染兼容性检查
4. 自动修复建议

支持的图表类型：
- 流程图 (flowchart)
- 序列图 (sequenceDiagram)
- 类图 (classDiagram)
- 状态图 (stateDiagram)
- 甘特图 (gantt)
- 饼图 (pie)
"""

import re
from typing import List, Dict, Any, Optional
from dataclasses import dataclass
from enum import Enum


class ChartType(Enum):
    """图表类型"""
    FLOWCHART = "flowchart"
    SEQUENCE = "sequenceDiagram"
    CLASS = "classDiagram"
    STATE = "stateDiagram"
    GANTT = "gantt"
    PIE = "pie"
    UNKNOWN = "unknown"


class ValidationSeverity(Enum):
    """验证严重性"""
    ERROR = "error"      # 语法错误，无法渲染
    WARNING = "warning"   # 兼容性问题，可能渲染异常
    INFO = "info"        # 优化建议


@dataclass
class MermaidValidationResult:
    """Mermaid验证结果"""
    line: int
    severity: ValidationSeverity
    message: str
    suggestion: str
    code_snippet: str


@dataclass
class ChartAnalysis:
    """图表分析结果"""
    chart_type: ChartType
    node_count: int
    edge_count: int
    complexity_score: int
    issues: List[MermaidValidationResult]
    is_valid: bool


class MermaidValidator:
    """Mermaid图表验证器"""
    
    def __init__(self):
        # Mermaid语法规则
        self.syntax_rules = self._build_syntax_rules()
        
        # 图表类型识别模式
        self.chart_patterns = {
            ChartType.FLOWCHART: r'^\s*flowchart\s',
            ChartType.SEQUENCE: r'^\s*sequenceDiagram\s',
            ChartType.CLASS: r'^\s*classDiagram\s',
            ChartType.STATE: r'^\s*stateDiagram\s',
            ChartType.GANTT: r'^\s*gantt\s',
            ChartType.PIE: r'^\s*pie\s'
        }
        
        # 常见错误模式
        self.error_patterns = {
            "missing_semicolon": r'^[^;\n]*$',
            "unclosed_quotes": r'"[^"\n]*$',
            "unclosed_brackets": r'\([^)\n]*$',
            "invalid_node_id": r'[^a-zA-Z0-9_]',
            "duplicate_node_ids": None,  # 需要特殊处理
            "orphan_nodes": None,        # 需要特殊处理
        }
        
        # 兼容性检查规则
        self.compatibility_rules = {
            "max_nodes": 50,      # 最大节点数
            "max_edges": 100,      # 最大边数
            "max_label_length": 50, # 标签最大长度
        }
    
    def _build_syntax_rules(self) -> List[Dict[str, Any]]:
        """构建语法规则"""
        return [
            {
                "name": "flowchart_start",
                "pattern": r'^\s*flowchart\s+TD\b',
                "severity": ValidationSeverity.ERROR,
                "message": "流程图必须以'flowchart TD'开头",
                "suggestion": "添加'TD'方向标识符"
            },
            {
                "name": "sequence_start",
                "pattern": r'^\s*sequenceDiagram\b',
                "severity": ValidationSeverity.ERROR,
                "message": "序列图必须以'sequenceDiagram'开头",
                "suggestion": "确保正确的图表类型声明"
            },
            {
                "name": "class_start",
                "pattern": r'^\s*classDiagram\b',
                "severity": ValidationSeverity.ERROR,
                "message": "类图必须以'classDiagram'开头",
                "suggestion": "确保正确的图表类型声明"
            },
            {
                "name": "missing_semicolon",
                "pattern": r'^\s*[a-zA-Z]',
                "severity": ValidationSeverity.ERROR,
                "message": "语句必须以分号结尾",
                "suggestion": "在语句末尾添加分号"
            },
            {
                "name": "valid_node_id",
                "pattern": r'^\s*([^\s\-]+)\s*-->',
                "severity": ValidationSeverity.ERROR,
                "message": "节点ID只能包含字母、数字和下划线",
                "suggestion": "使用有效的节点标识符"
            }
        ]
    
    def validate_mermaid_code(self, mermaid_code: str) -> ChartAnalysis:
        """
        验证Mermaid代码
        
        Args:
            mermaid_code: Mermaid图表代码
            
        Returns:
            图表分析结果
        """
        lines = mermaid_code.strip().split('\n')
        issues = []
        
        # 识别图表类型
        chart_type = self._identify_chart_type(lines)
        
        # 基本语法验证
        issues.extend(self._validate_basic_syntax(lines, chart_type))
        
        # 图表特定验证
        if chart_type != ChartType.UNKNOWN:
            issues.extend(self._validate_chart_specific_rules(lines, chart_type))
        
        # 结构完整性验证
        issues.extend(self._validate_structure_integrity(lines, chart_type))
        
        # 兼容性检查
        issues.extend(self._validate_compatibility(lines, chart_type))
        
        # 分析图表复杂度
        node_count, edge_count = self._analyze_complexity(lines, chart_type)
        complexity_score = self._calculate_complexity_score(node_count, edge_count)
        
        # 检查是否有效
        is_valid = all(issue.severity != ValidationSeverity.ERROR for issue in issues)
        
        return ChartAnalysis(
            chart_type=chart_type,
            node_count=node_count,
            edge_count=edge_count,
            complexity_score=complexity_score,
            issues=issues,
            is_valid=is_valid
        )
    
    def _identify_chart_type(self, lines: List[str]) -> ChartType:
        """识别图表类型"""
        for line in lines:
            for chart_type, pattern in self.chart_patterns.items():
                if re.search(pattern, line, re.IGNORECASE):
                    return chart_type
        return ChartType.UNKNOWN
    
    def _validate_basic_syntax(self, lines: List[str], chart_type: ChartType) -> List[MermaidValidationResult]:
        """验证基本语法"""
        issues = []
        
        for i, line in enumerate(lines, 1):
            line = line.strip()
            if not line or line.startswith('%%'):  # 跳过空行和注释
                continue
            
            # 检查分号结尾（部分图表类型需要）
            if chart_type in [ChartType.FLOWCHART, ChartType.CLASS, ChartType.STATE]:
                if not line.endswith(';') and not line.endswith('{') and not line.endswith('}') and not line.startswith('subgraph'):
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.ERROR,
                        message="语句必须以分号、大括号或subgraph关键字结尾",
                        suggestion="在语句末尾添加适当的分隔符",
                        code_snippet=line
                    ))
            
            # 检查未闭合的引号
            if line.count('"') % 2 != 0:
                issues.append(MermaidValidationResult(
                    line=i,
                    severity=ValidationSeverity.ERROR,
                    message="未闭合的引号",
                    suggestion="确保每个引号都有对应的闭合引号",
                    code_snippet=line
                ))
            
            # 检查未闭合的括号
            if line.count('(') != line.count(')'):
                issues.append(MermaidValidationResult(
                    line=i,
                    severity=ValidationSeverity.ERROR,
                    message="未闭合的括号",
                    suggestion="确保每个左括号都有对应的右括号",
                    code_snippet=line
                ))
        
        return issues
    
    def _validate_chart_specific_rules(self, lines: List[str], chart_type: ChartType) -> List[MermaidValidationResult]:
        """验证图表特定规则"""
        issues = []
        
        if chart_type == ChartType.FLOWCHART:
            issues.extend(self._validate_flowchart_rules(lines))
        elif chart_type == ChartType.SEQUENCE:
            issues.extend(self._validate_sequence_rules(lines))
        elif chart_type == ChartType.CLASS:
            issues.extend(self._validate_class_rules(lines))
        
        return issues
    
    def _validate_flowchart_rules(self, lines: List[str]) -> List[MermaidValidationResult]:
        """验证流程图规则"""
        issues = []
        node_ids = set()
        
        for i, line in enumerate(lines, 1):
            line = line.strip()
            
            # 提取节点ID
            node_match = re.match(r'^([^\s\-]+)\s', line)
            if node_match:
                node_id = node_match.group(1)
                
                # 检查节点ID有效性
                if not re.match(r'^[a-zA-Z_][a-zA-Z0-9_]*$', node_id):
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.ERROR,
                        message=f"无效的节点ID: {node_id}",
                        suggestion="节点ID必须以字母或下划线开头，只能包含字母、数字和下划线",
                        code_snippet=line
                    ))
                
                # 检查重复节点ID
                if node_id in node_ids:
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.WARNING,
                        message=f"重复的节点ID: {node_id}",
                        suggestion="为节点使用唯一的标识符",
                        code_snippet=line
                    ))
                else:
                    node_ids.add(node_id)
            
            # 检查箭头语法
            if '-->' in line or '--' in line:
                arrow_match = re.search(r'(-->|--)\s*([^\s]+)', line)
                if arrow_match and arrow_match.group(2) not in node_ids and arrow_match.group(2) != '|':
                    target_node = arrow_match.group(2)
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.WARNING,
                        message=f"未定义的节点引用: {target_node}",
                        suggestion="确保引用的节点已定义，或检查拼写错误",
                        code_snippet=line
                    ))
        
        return issues
    
    def _validate_sequence_rules(self, lines: List[str]) -> List[MermaidValidationResult]:
        """验证序列图规则"""
        issues = []
        participants = set()
        
        for i, line in enumerate(lines, 1):
            line = line.strip()
            
            # 提取参与者
            participant_match = re.match(r'^participant\s+([^\s]+)', line)
            if participant_match:
                participant = participant_match.group(1)
                participants.add(participant)
            
            # 检查消息语法
            message_match = re.match(r'^([^\s]+)\s+->\s+([^\s]+)', line)
            if message_match:
                source, target = message_match.groups()
                if source not in participants and source != 'actor':
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.ERROR,
                        message=f"未定义的参与者: {source}",
                        suggestion="在使用参与者前先使用'participant'关键字定义",
                        code_snippet=line
                    ))
                
                if target not in participants and target != 'actor':
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.ERROR,
                        message=f"未定义的参与者: {target}",
                        suggestion="在使用参与者前先使用'participant'关键字定义",
                        code_snippet=line
                    ))
        
        return issues
    
    def _validate_class_rules(self, lines: List[str]) -> List[MermaidValidationResult]:
        """验证类图规则"""
        issues = []
        classes = set()
        
        for i, line in enumerate(lines, 1):
            line = line.strip()
            
            # 提取类名
            class_match = re.match(r'^class\s+([^\s{]+)', line)
            if class_match:
                class_name = class_match.group(1)
                classes.add(class_name)
            
            # 检查关系语法
            relation_match = re.match(r'^([^\s]+)\s+([<|>|]+\.?)+\s+([^\s]+)', line)
            if relation_match:
                source, relation, target = relation_match.groups()
                
                if source not in classes:
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.ERROR,
                        message=f"未定义的类: {source}",
                        suggestion="在使用类前先使用'class'关键字定义",
                        code_snippet=line
                    ))
                
                if target not in classes:
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.ERROR,
                        message=f"未定义的类: {target}",
                        suggestion="在使用类前先使用'class'关键字定义",
                        code_snippet=line
                    ))
        
        return issues
    
    def _validate_structure_integrity(self, lines: List[str], chart_type: ChartType) -> List[MermaidValidationResult]:
        """验证结构完整性"""
        issues = []
        
        # 检查是否有孤立节点（仅适用于流程图）
        if chart_type == ChartType.FLOWCHART:
            issues.extend(self._check_orphan_nodes(lines))
        
        # 检查子图结构
        issues.extend(self._validate_subgraph_structure(lines))
        
        return issues
    
    def _check_orphan_nodes(self, lines: List[str]) -> List[MermaidValidationResult]:
        """检查孤立节点"""
        issues = []
        defined_nodes = set()
        referenced_nodes = set()
        
        for i, line in enumerate(lines, 1):
            line = line.strip()
            
            # 提取定义的节点
            node_match = re.match(r'^([^\s\-]+)\s', line)
            if node_match:
                node_id = node_match.group(1)
                defined_nodes.add(node_id)
            
            # 提取引用的节点
            ref_match = re.search(r'-->\s*([^\s\]]+)', line)
            if ref_match:
                ref_node = ref_match.group(1)
                if ref_node not in ['|', '']:
                    referenced_nodes.add(ref_node)
        
        # 找出被引用但未定义的节点
        undefined_refs = referenced_nodes - defined_nodes
        for node in undefined_refs:
            issues.append(MermaidValidationResult(
                line=0,  # 无法确定具体行号
                severity=ValidationSeverity.ERROR,
                message=f"被引用但未定义的节点: {node}",
                suggestion="确保所有被引用的节点都已正确定义",
                code_snippet=node
            ))
        
        # 找出定义但未被引用的节点（孤立节点）
        orphan_nodes = defined_nodes - referenced_nodes
        for node in orphan_nodes:
            # 跳过起始节点（可能没有被引用）
            if len(orphan_nodes) == len(defined_nodes):
                continue  # 所有节点都是孤立的，可能是起始节点
                
            issues.append(MermaidValidationResult(
                line=0,
                severity=ValidationSeverity.WARNING,
                message=f"孤立节点: {node}",
                suggestion="确保节点被正确连接，或考虑删除未使用的节点",
                code_snippet=node
            ))
        
        return issues
    
    def _validate_subgraph_structure(self, lines: List[str]) -> List[MermaidValidationResult]:
        """验证子图结构"""
        issues = []
        subgraph_stack = []
        
        for i, line in enumerate(lines, 1):
            line = line.strip()
            
            # 检查子图开始
            if line.startswith('subgraph'):
                subgraph_stack.append(i)
            
            # 检查子图结束
            elif line == 'end':
                if not subgraph_stack:
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.ERROR,
                        message="多余的'end'语句",
                        suggestion="删除多余的'end'或添加对应的'subgraph'",
                        code_snippet=line
                    ))
                else:
                    subgraph_stack.pop()
        
        # 检查未闭合的子图
        for line_num in subgraph_stack:
            issues.append(MermaidValidationResult(
                line=line_num,
                severity=ValidationSeverity.ERROR,
                message="未闭合的子图",
                suggestion="在适当位置添加'end'语句",
                code_snippet="subgraph"
            ))
        
        return issues
    
    def _validate_compatibility(self, lines: List[str], chart_type: ChartType) -> List[MermaidValidationResult]:
        """验证兼容性"""
        issues = []
        
        # 统计节点和边数
        node_count, edge_count = self._analyze_complexity(lines, chart_type)
        
        # 检查节点数量限制
        if node_count > self.compatibility_rules["max_nodes"]:
            issues.append(MermaidValidationResult(
                line=0,
                severity=ValidationSeverity.WARNING,
                message=f"节点数量过多: {node_count}",
                suggestion=f"建议将节点数量控制在{self.compatibility_rules['max_nodes']}以内以提高可读性",
                code_snippet=f"Total nodes: {node_count}"
            ))
        
        # 检查边数量限制
        if edge_count > self.compatibility_rules["max_edges"]:
            issues.append(MermaidValidationResult(
                line=0,
                severity=ValidationSeverity.WARNING,
                message=f"边数量过多: {edge_count}",
                suggestion=f"建议将边数量控制在{self.compatibility_rules['max_edges']}以内",
                code_snippet=f"Total edges: {edge_count}"
            ))
        
        # 检查标签长度
        for i, line in enumerate(lines, 1):
            label_match = re.search(r'\["([^"]+)"\]', line)
            if label_match:
                label = label_match.group(1)
                if len(label) > self.compatibility_rules["max_label_length"]:
                    issues.append(MermaidValidationResult(
                        line=i,
                        severity=ValidationSeverity.INFO,
                        message=f"标签过长: {len(label)}字符",
                        suggestion=f"建议将标签长度控制在{self.compatibility_rules['max_label_length']}字符以内",
                        code_snippet=label[:50] + "..."
                    ))
        
        return issues
    
    def _analyze_complexity(self, lines: List[str], chart_type: ChartType) -> tuple:
        """分析图表复杂度"""
        node_count = 0
        edge_count = 0
        
        for line in lines:
            line = line.strip()
            
            if chart_type == ChartType.FLOWCHART:
                # 统计节点
                if re.match(r'^[^\s\-]+\s', line) and not line.startswith('flowchart'):
                    node_count += 1
                
                # 统计边
                if '-->' in line or '--' in line:
                    edge_count += 1
            
            elif chart_type == ChartType.SEQUENCE:
                # 统计参与者
                if line.startswith('participant'):
                    node_count += 1
                
                # 统计消息
                if '->' in line:
                    edge_count += 1
            
            elif chart_type == ChartType.CLASS:
                # 统计类
                if line.startswith('class'):
                    node_count += 1
                
                # 统计关系
                if any(arrow in line for arrow in ['<|--', '|>--', '*--', 'o--', '<--', '-->']):
                    edge_count += 1
        
        return node_count, edge_count
    
    def _calculate_complexity_score(self, node_count: int, edge_count: int) -> int:
        """计算复杂度分数"""
        # 简化的复杂度计算
        complexity = node_count + edge_count * 0.5
        
        if complexity < 10:
            return 1  # 简单
        elif complexity < 25:
            return 2  # 中等
        elif complexity < 50:
            return 3  # 复杂
        else:
            return 4  # 非常复杂
    
    def generate_fix_suggestions(self, analysis: ChartAnalysis) -> List[str]:
        """生成修复建议"""
        suggestions = []
        
        # 根据问题类型生成建议
        error_issues = [issue for issue in analysis.issues if issue.severity == ValidationSeverity.ERROR]
        warning_issues = [issue for issue in analysis.issues if issue.severity == ValidationSeverity.WARNING]
        
        if error_issues:
            suggestions.append("存在语法错误，必须修复后才能正确渲染")
            for issue in error_issues[:3]:  # 只显示前3个错误
                suggestions.append(f"- 第{issue.line}行: {issue.message}")
        
        if warning_issues:
            suggestions.append("存在兼容性问题，建议优化以提高渲染质量")
            for issue in warning_issues[:2]:
                suggestions.append(f"- {issue.message}")
        
        # 复杂度建议
        if analysis.complexity_score >= 3:
            suggestions.append("图表复杂度较高，建议简化或分割为多个图表")
        
        # 图表类型特定建议
        if analysis.chart_type == ChartType.FLOWCHART and analysis.node_count > 20:
            suggestions.append("流程图节点较多，建议使用子图组织相关节点")
        
        return suggestions
    
    def auto_fix_mermaid_code(self, mermaid_code: str) -> str:
        """自动修复Mermaid代码"""
        lines = mermaid_code.split('\n')
        fixed_lines = []
        
        for line in lines:
            fixed_line = line
            
            # 自动添加缺失的分号
            if (fixed_line.strip() and 
                not fixed_line.strip().endswith(';') and 
                not fixed_line.strip().endswith('{') and 
                not fixed_line.strip().endswith('}') and 
                not fixed_line.strip().startswith('subgraph') and
                not fixed_line.strip().startswith('end')):
                fixed_line = fixed_line.rstrip() + ';'
            
            fixed_lines.append(fixed_line)
        
        return '\n'.join(fixed_lines)


# 使用示例
def test_mermaid_validator():
    """测试Mermaid验证器"""
    validator = MermaidValidator()
    
    # 示例Mermaid代码（包含一些错误）
    mermaid_code = """
flowchart TD
    A[开始] --> B{处理}
    B --> C[结束]
    D[孤立节点]
    """
    
    # 验证代码
    analysis = validator.validate_mermaid_code(mermaid_code)
    
    print("图表分析结果：")
    print(f"图表类型: {analysis.chart_type.value}")
    print(f"节点数量: {analysis.node_count}")
    print(f"边数量: {analysis.edge_count}")
    print(f"复杂度评分: {analysis.complexity_score}")
    print(f"是否有效: {analysis.is_valid}")
    
    print("\n发现问题：")
    for issue in analysis.issues:
        print(f"第{issue.line}行 [{issue.severity.value}]: {issue.message}")
        print(f"  建议: {issue.suggestion}")
        print(f"  代码片段: {issue.code_snippet}")
    
    # 生成修复建议
    suggestions = validator.generate_fix_suggestions(analysis)
    print("\n修复建议：")
    for suggestion in suggestions:
        print(f"- {suggestion}")
    
    # 自动修复
    fixed_code = validator.auto_fix_mermaid_code(mermaid_code)
    print("\n自动修复后的代码：")
    print(fixed_code)


if __name__ == "__main__":
    test_mermaid_validator()