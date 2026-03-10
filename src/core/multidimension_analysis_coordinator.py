"""
多维度分析协调器

协调多个分析器进行多维度分析，实现：
1. 静态分析：语法结构、依赖关系
2. 动态分析：运行时行为、数据流
3. 语义分析：代码意图、业务逻辑
4. 交叉验证：验证各维度结果的一致性
5. 结果融合：综合多维度知识

核心优势：
- 提升知识准确性
- 发现隐藏问题
- 全面理解代码
"""

import asyncio
from pathlib import Path
from typing import Dict, List, Set, Optional, Tuple, Any, Callable
from dataclasses import dataclass, field
from enum import Enum
from datetime import datetime
import re


class AnalysisDimension(Enum):
    """分析维度"""
    STATIC = "static"       # 静态分析
    DYNAMIC = "dynamic"     # 动态分析
    SEMANTIC = "semantic"   # 语义分析
    METRICS = "metrics"     # 指标分析
    SECURITY = "security"   # 安全分析


@dataclass
class AnalysisResult:
    """分析结果"""
    
    dimension: AnalysisDimension  # 分析维度
    entity_id: str              # 实体 ID
    entity_name: str            # 实体名称
    entity_type: str           # 实体类型
    success: bool              # 是否成功
    data: Dict[str, Any] = field(default_factory=dict)  # 分析数据
    confidence: float = 0.0     # 置信度 (0-1)
    issues: List[str] = field(default_factory=list)  # 发现的问题
    suggestions: List[str] = field(default_factory=list)  # 改进建议
    metadata: Dict[str, Any] = field(default_factory=dict)  # 元数据
    timestamp: datetime = field(default_factory=datetime.now)
    
    def get_issue_count(self) -> int:
        """获取问题数量"""
        return len(self.issues)
    
    def get_suggestion_count(self) -> int:
        """获取建议数量"""
        return len(self.suggestions)
    
    def has_critical_issues(self) -> bool:
        """是否有严重问题"""
        return any("严重" in issue or "critical" in issue.lower() for issue in self.issues)


@dataclass
class ValidationResult:
    """验证结果"""
    
    entity_id: str              # 实体 ID
    entity_name: str            # 实体名称
    is_valid: bool             # 是否有效
    confidence: float          # 置信度
    conflicts: List[str] = field(default_factory=list)  # 冲突列表
    inconsistencies: List[str] = field(default_factory=list)  # 不一致列表
    resolutions: Dict[str, str] = field(default_factory=dict)  # 解决方案
    timestamp: datetime = field(default_factory=datetime.now)
    
    def get_conflict_count(self) -> int:
        """获取冲突数量"""
        return len(self.conflicts)
    
    def get_inconsistency_count(self) -> int:
        """获取不一致数量"""
        return len(self.inconsistencies)


@dataclass
class MergedResult:
    """融合结果"""
    
    entity_id: str              # 实体 ID
    entity_name: str            # 实体名称
    entity_type: str           # 实体类型
    results: Dict[AnalysisDimension, AnalysisResult] = field(default_factory=dict)  # 各维度结果
    merged_data: Dict[str, Any] = field(default_factory=dict)  # 融合数据
    overall_confidence: float = 0.0  # 综合置信度
    validation_result: Optional[ValidationResult] = None  # 验证结果
    timestamp: datetime = field(default_factory=datetime.now)
    
    def get_available_dimensions(self) -> List[AnalysisDimension]:
        """获取可用的分析维度"""
        return list(self.results.keys())
    
    def get_dimension_confidence(self, dimension: AnalysisDimension) -> float:
        """获取指定维度的置信度"""
        if dimension in self.results:
            return self.results[dimension].confidence
        return 0.0
    
    def get_all_issues(self) -> List[str]:
        """获取所有问题"""
        issues = []
        for result in self.results.values():
            issues.extend(result.issues)
        return issues
    
    def get_all_suggestions(self) -> List[str]:
        """获取所有建议"""
        suggestions = []
        for result in self.results.values():
            suggestions.extend(result.suggestions)
        return suggestions


@dataclass
class Conflict:
    """冲突信息"""
    
    entity_id: str              # 实体 ID
    entity_name: str            # 实体名称
    dimension1: AnalysisDimension  # 维度1
    dimension2: AnalysisDimension  # 维度2
    field: str                 # 冲突字段
    value1: Any                # 值1
    value2: Any                # 值2
    severity: str              # 严重程度 (low, medium, high)
    timestamp: datetime = field(default_factory=datetime.now)


@dataclass
class Resolution:
    """解决方案"""
    
    conflict_id: str           # 冲突 ID
    strategy: str              # 解决策略
    resolved_value: Any        # 解决后的值
    reasoning: str            # 解决理由
    timestamp: datetime = field(default_factory=datetime.now)


class StaticAnalyzer:
    """
    静态分析器
    
    功能：
    - 语法结构分析
    - 依赖关系分析
    - 代码复杂度分析
    - 代码风格检查
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化静态分析器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
    
    def analyze(self, entity, source_code: str = None) -> AnalysisResult:
        """
        执行静态分析
        
        Args:
            entity: 代码实体
            source_code: 源代码
            
        Returns:
            分析结果
        """
        entity_id = getattr(entity, "id", "")
        entity_name = getattr(entity, "name", "")
        entity_type = getattr(entity, "entity_type", "")
        
        data = {}
        issues = []
        suggestions = []
        
        # 语法结构分析
        structure_data = self._analyze_structure(entity, source_code)
        data["structure"] = structure_data
        
        # 依赖关系分析
        dependencies = self._analyze_dependencies(entity, source_code)
        data["dependencies"] = dependencies
        
        # 复杂度分析
        complexity = self._analyze_complexity(entity, source_code)
        data["complexity"] = complexity
        
        if complexity.get("cyclomatic", 0) > 10:
            issues.append(f"圈复杂度过高: {complexity['cyclomatic']}")
            suggestions.append("建议拆分复杂函数")
        
        # 代码风格检查
        style_issues = self._check_code_style(entity, source_code)
        if style_issues:
            issues.extend(style_issues)
        
        return AnalysisResult(
            dimension=AnalysisDimension.STATIC,
            entity_id=entity_id,
            entity_name=entity_name,
            entity_type=entity_type,
            success=True,
            data=data,
            confidence=0.95,  # 静态分析置信度较高
            issues=issues,
            suggestions=suggestions,
            metadata={"analyzer": "static"}
        )
    
    def _analyze_structure(self, entity, source_code: str) -> Dict[str, Any]:
        """分析代码结构"""
        structure = {
            "has_docstring": bool(getattr(entity, "docstring", "")),
            "line_count": len(source_code.split('\n')) if source_code else 0,
            "parameter_count": len(getattr(entity, "parameters", [])),
            "modifier_count": len(getattr(entity, "modifiers", []))
        }
        return structure
    
    def _analyze_dependencies(self, entity, source_code: str) -> List[Dict[str, str]]:
        """分析依赖关系"""
        dependencies = []
        
        # 简化实现：基于 import 语句
        if source_code:
            import_patterns = [
                r'^import\s+([^\s]+)',
                r'^from\s+([^\s]+)\s+import'
            ]
            
            for line in source_code.split('\n'):
                for pattern in import_patterns:
                    match = re.match(pattern, line.strip())
                    if match:
                        dependencies.append({
                            "module": match.group(1),
                            "type": "import"
                        })
        
        return dependencies
    
    def _analyze_complexity(self, entity, source_code: str) -> Dict[str, Any]:
        """分析代码复杂度"""
        complexity = {
            "cyclomatic": 1,  # 基础复杂度
            "nesting_depth": 0,
            "cognitive": 0
        }
        
        if source_code:
            # 简化的圈复杂度计算
            control_flow_keywords = ["if", "elif", "for", "while", "except", "case"]
            for keyword in control_flow_keywords:
                complexity["cyclomatic"] += source_code.count(f"{keyword} ")
            
            # 嵌套深度（简化）
            complexity["nesting_depth"] = source_code.count("    ") //4 + 1
        
        return complexity
    
    def _check_code_style(self, entity, source_code: str) -> List[str]:
        """检查代码风格"""
        issues = []
        
        if source_code:
            # 检查行长度
            for i, line in enumerate(source_code.split('\n'), 1):
                if len(line) > 100:
                    issues.append(f"第{i}行过长 ({len(line)}字符)")
            
            # 检查命名规范
            entity_name = getattr(entity, "name", "")
            if entity_type := getattr(entity, "entity_type", ""):
                if entity_type == "function" and not re.match(r'^[a-z_][a-z0-9_]*$', entity_name):
                    issues.append(f"函数命名不符合 PEP8: {entity_name}")
        
        return issues


class DynamicAnalyzer:
    """
    动态分析器
    
    功能：
    - 运行时行为分析
    - 数据流分析
    - 性能分析
    - 内存使用分析
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化动态分析器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
    
    def analyze(self, entity, source_code: str = None) -> AnalysisResult:
        """
        执行动态分析
        
        Args:
            entity: 代码实体
            source_code: 源代码
            
        Returns:
            分析结果
        """
        entity_id = getattr(entity, "id", "")
        entity_name = getattr(entity, "name", "")
        entity_type = getattr(entity, "entity_type", "")
        
        data = {}
        issues = []
        suggestions = []
        
        # 数据流分析
        dataflow = self._analyze_dataflow(entity, source_code)
        data["dataflow"] = dataflow
        
        # 性能分析（简化）
        performance = self._analyze_performance(entity, source_code)
        data["performance"] = performance
        
        if performance.get("estimated_time", 0) > 100:  # ms
            issues.append("预计执行时间较长")
            suggestions.append("考虑优化算法")
        
        # 内存使用分析（简化）
        memory = self._analyze_memory(entity, source_code)
        data["memory"] = memory
        
        return AnalysisResult(
            dimension=AnalysisDimension.DYNAMIC,
            entity_id=entity_id,
            entity_name=entity_name,
            entity_type=entity_type,
            success=True,
            data=data,
            confidence=0.75,  # 动态分析置信度中等
            issues=issues,
            suggestions=suggestions,
            metadata={"analyzer": "dynamic"}
        )
    
    def _analyze_dataflow(self, entity, source_code: str) -> Dict[str, Any]:
        """分析数据流"""
        dataflow = {
            "input_vars": [],
            "output_vars": [],
            "intermediate_vars": [],
            "data_transformations": []
        }
        
        if source_code:
            # 简化实现：查找赋值语句
            assignments = re.findall(r'(\w+)\s*=', source_code)
            dataflow["intermediate_vars"] = list(set(assignments))
            
            # 查找 return 语句
            returns = re.findall(r'return\s+(.+)', source_code)
            dataflow["output_vars"] = returns
        
        return dataflow
    
    def _analyze_performance(self, entity, source_code: str) -> Dict[str, Any]:
        """分析性能"""
        performance = {
            "time_complexity": "O(1)",
            "space_complexity": "O(1)",
            "estimated_time": 0.0
        }
        
        if source_code:
            # 简化实现：基于代码行数估算
            line_count = len(source_code.split('\n'))
            performance["estimated_time"] = line_count * 0.5  # 假设每行 0.5ms
            
            # 简单的复杂度推断
            if "for " in source_code or "while " in source_code:
                performance["time_complexity"] = "O(n)"
            if source_code.count("for ") > 1 or source_code.count("while ") > 1:
                performance["time_complexity"] = "O(n²)"
        
        return performance
    
    def _analyze_memory(self, entity, source_code: str) -> Dict[str, Any]:
        """分析内存使用"""
        memory = {
            "estimated_usage": 0,  # bytes
            "potential_leaks": [],
            "large_allocations": []
        }
        
        if source_code:
            # 简化实现：查找可能的内存分配
            large_patterns = ["[] *", "{} *", "list("]
            for pattern in large_patterns:
                if pattern in source_code:
                    memory["large_allocations"].append(f"发现大内存分配模式: {pattern}")
        
        return memory


class SemanticAnalyzer:
    """
    语义分析器
    
    功能：
    - 代码意图分析
    - 业务逻辑分析
    - 注释一致性检查
    - 命名语义分析
    """
    
    def __init__(self, config: Dict[str, Any] = None, llm_client=None):
        """
        初始化语义分析器
        
        Args:
            config: 配置字典
            llm_client: LLM 客户端
        """
        self.config = config or {}
        self.llm_client = llm_client
    
    def analyze(self, entity, source_code: str = None) -> AnalysisResult:
        """
        执行语义分析
        
        Args:
            entity: 代码实体
            source_code: 源代码
            
        Returns:
            分析结果
        """
        entity_id = getattr(entity, "id", "")
        entity_name = getattr(entity, "name", "")
        entity_type = getattr(entity, "entity_type", "")
        
        data = {}
        issues = []
        suggestions = []
        
        # 代码意图分析
        intent = self._analyze_intent(entity, source_code)
        data["intent"] = intent
        
        # 注释一致性检查
        consistency = self._check_comment_consistency(entity, source_code)
        data["comment_consistency"] = consistency
        
        if not consistency.get("is_consistent", True):
            issues.append("代码与注释不一致")
            suggestions.append("更新注释或修改代码")
        
        # 命名语义分析
        naming = self._analyze_naming_semantics(entity)
        data["naming"] = naming
        
        return AnalysisResult(
            dimension=AnalysisDimension.SEMANTIC,
            entity_id=entity_id,
            entity_name=entity_name,
            entity_type=entity_type,
            success=True,
            data=data,
            confidence=0.85,  # 语义分析置信度较高
            issues=issues,
            suggestions=suggestions,
            metadata={"analyzer": "semantic"}
        )
    
    def _analyze_intent(self, entity, source_code: str) -> Dict[str, Any]:
        """分析代码意图"""
        intent = {
            "primary_intent": "unknown",
            "secondary_intents": [],
            "business_domain": "general",
            "description": ""
        }
        
        # 简化实现：基于函数名和关键词推断意图
        entity_name = getattr(entity, "name", "").lower()
        
        intent_keywords = {
            "calculate": ["calc", "compute", "calculate"],
            "validate": ["check", "validate", "verify"],
            "transform": ["convert", "transform", "parse"],
            "retrieve": ["get", "fetch", "load", "find"],
            "store": ["save", "store", "write", "persist"],
            "process": ["process", "handle", "manage"]
        }
        
        for intent_type, keywords in intent_keywords.items():
            for keyword in keywords:
                if keyword in entity_name:
                    intent["primary_intent"] = intent_type
                    break
        
        return intent
    
    def _check_comment_consistency(self, entity, source_code: str) -> Dict[str, Any]:
        """检查注释一致性"""
        consistency = {
            "is_consistent": True,
            "docstring_present": False,
            "comment_matches_code": True
        }
        
        docstring = getattr(entity, "docstring", "")
        consistency["docstring_present"] = bool(docstring)
        
        if docstring and source_code:
            # 简化检查：检查 docstring 是否包含函数名
            entity_name = getattr(entity, "name", "")
            consistency["comment_matches_code"] = entity_name.lower() in docstring.lower()
            consistency["is_consistent"] = consistency["comment_matches_code"]
        
        return consistency
    
    def _analyze_naming_semantics(self, entity) -> Dict[str, Any]:
        """分析命名语义"""
        naming = {
            "is_meaningful": True,
            "follows_convention": True,
            "suggestions": []
        }
        
        entity_name = getattr(entity, "name", "")
        
        # 检查命名是否过于简单
        if entity_name in ["x", "y", "z", "tmp", "temp"]:
            naming["is_meaningful"] = False
            naming["suggestions"].append("使用更具描述性的名称")
        
        # 检查缩写
        abbreviations = re.findall(r'[A-Z]{2,}', entity_name)
        if abbreviations:
            naming["suggestions"].append(
                f"考虑展开缩写: {', '.join(abbreviations)}"
            )
        
        return naming


class MetricsAnalyzer:
    """
    指标分析器
    
    功能：
    - 代码质量指标计算
    - 可维护性指标
    - 可测试性指标
    - 技术债务评估
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化指标分析器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
    
    def analyze(self, entity, source_code: str = None) -> AnalysisResult:
        """
        执行指标分析
        
        Args:
            entity: 代码实体
            source_code: 源代码
            
        Returns:
            分析结果
        """
        entity_id = getattr(entity, "id", "")
        entity_name = getattr(entity, "name", "")
        entity_type = getattr(entity, "entity_type", "")
        
        data = {}
        issues = []
        suggestions = []
        
        # 代码质量指标
        quality_metrics = self._calculate_quality_metrics(entity, source_code)
        data["quality_metrics"] = quality_metrics
        
        # 可维护性指标
        maintainability = self._calculate_maintainability(entity, source_code)
        data["maintainability"] = maintainability
        
        if maintainability.get("score", 100) < 70:
            issues.append("可维护性得分较低")
            suggestions.append("考虑重构以提高可维护性")
        
        # 技术债务
        tech_debt = self._estimate_technical_debt(entity, source_code)
        data["technical_debt"] = tech_debt
        
        return AnalysisResult(
            dimension=AnalysisDimension.METRICS,
            entity_id=entity_id,
            entity_name=entity_name,
            entity_type=entity_type,
            success=True,
            data=data,
            confidence=0.90,
            issues=issues,
            suggestions=suggestions,
            metadata={"analyzer": "metrics"}
        )
    
    def _calculate_quality_metrics(self, entity, source_code: str) -> Dict[str, Any]:
        """计算代码质量指标"""
        metrics = {
            "lines_of_code": 0,
            "comment_ratio": 0.0,
            "cyclomatic_complexity": 1,
            "maintainability_index": 0,
            "code_duplication": 0
        }
        
        if source_code:
            lines = source_code.split('\n')
            metrics["lines_of_code"] = len(lines)
            
            # 注释比例
            comment_lines = sum(1 for line in lines if line.strip().startswith('#'))
            metrics["comment_ratio"] = comment_lines / len(lines) if lines else 0
            
            # 简化的可维护性指数
            metrics["maintainability_index"] = max(0, 171 - 5.2 * metrics["cyclomatic_complexity"] - 0.23 * metrics["lines_of_code"])
        
        return metrics
    
    def _calculate_maintainability(self, entity, source_code: str) -> Dict[str, Any]:
        """计算可维护性指标"""
        maintainability = {
            "score": 100,
            "factors": {},
            "recommendations": []
        }
        
        factors = maintainability["factors"]
        score = 100
        
        # 代码长度影响
        if source_code:
            line_count = len(source_code.split('\n'))
            if line_count > 100:
                score -= 10
                factors["length"] = "代码过长"
                maintainability["recommendations"].append("考虑拆分长函数")
        
        # 复杂度影响
        complexity = self._calculate_quality_metrics(entity, source_code).get("cyclomatic_complexity", 1)
        if complexity > 10:
            score -= 15
            factors["complexity"] = "复杂度过高"
            maintainability["recommendations"].append("降低函数复杂度")
        
        maintainability["score"] = max(0, score)
        return maintainability
    
    def _estimate_technical_debt(self, entity, source_code: str) -> Dict[str, Any]:
        """估算技术债务"""
        debt = {
            "total_debt": 0,  # 小时
            "interest": 0,     # 每周额外时间
            "items": []
        }
        
        # 简化实现：基于代码质量评估债务
        issues = []
        
        if source_code:
            # 长函数
            if len(source_code.split('\n')) > 50:
                debt["items"].append({
                    "type": "long_function",
                    "description": "函数过长",
                    "estimated_cost": 2  # 小时
                })
            
            # 缺少文档
            if not getattr(entity, "docstring", ""):
                debt["items"].append({
                    "type": "missing_docstring",
                    "description": "缺少文档字符串",
                    "estimated_cost": 0.5  # 小时
                })
        
        debt["total_debt"] = sum(item.get("estimated_cost", 0) for item in debt["items"])
        debt["interest"] = debt["total_debt"] * 0.1  # 10% 利息
        
        return debt


class SecurityAnalyzer:
    """
    安全分析器
    
    功能：
    - 安全漏洞检测
    - 敏感信息泄露检查
    - 输入验证检查
    - 权限控制检查
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化安全分析器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
        self.vulnerability_patterns = self.config.get("vulnerability_patterns", {
            "sql_injection": [
                r'execute\s*\(\s*f["\']\s*\+',
                r'exec\(',
                r'eval\s*\('
            ],
            "command_injection": [
                r'os\.system\(',
                r'subprocess\.\w+\(\s*[^,]*,\s*shell\s*='
            ],
            "hardcoded_secrets": [
                r'password\s*=\s*["\'][^"\']+["\']',
                r'api_key\s*=\s*["\'][^"\']+["\']',
                r'secret\s*=\s*["\'][^"\']+["\']'
            ]
        })
    
    def analyze(self, entity, source_code: str = None) -> AnalysisResult:
        """
        执行安全分析
        
        Args:
            entity: 代码实体
            source_code: 源代码
            
        Returns:
            分析结果
        """
        entity_id = getattr(entity, "id", "")
        entity_name = getattr(entity, "name", "")
        entity_type = getattr(entity, "entity_type", "")
        
        data = {}
        issues = []
        suggestions = []
        
        # 安全漏洞检测
        vulnerabilities = self._detect_vulnerabilities(entity, source_code)
        data["vulnerabilities"] = vulnerabilities
        
        for vuln in vulnerabilities:
            severity = vuln.get("severity", "low")
            severity_text = "严重" if severity == "high" else ("中等" if severity == "medium" else "低")
            issues.append(f"{severity_text}安全漏洞: {vuln.get('type')}")
            suggestions.append(f"修复 {vuln.get('type')} 漏洞")
        
        # 输入验证检查
        input_validation = self._check_input_validation(entity, source_code)
        data["input_validation"] = input_validation
        
        if not input_validation.get("has_validation", False):
            issues.append("缺少输入验证")
            suggestions.append("添加输入验证逻辑")
        
        return AnalysisResult(
            dimension=AnalysisDimension.SECURITY,
            entity_id=entity_id,
            entity_name=entity_name,
            entity_type=entity_type,
            success=True,
            data=data,
            confidence=0.80,
            issues=issues,
            suggestions=suggestions,
            metadata={"analyzer": "security"}
        )
    
    def _detect_vulnerabilities(self, entity, source_code: str) -> List[Dict[str, Any]]:
        """检测安全漏洞"""
        vulnerabilities = []
        
        if source_code:
            for vuln_type, patterns in self.vulnerability_patterns.items():
                for pattern in patterns:
                    matches = re.finditer(pattern, source_code, re.IGNORECASE)
                    for match in matches:
                        line_num = source_code[:match.start()].count('\n') + 1
                        vulnerabilities.append({
                            "type": vuln_type,
                            "severity": "high" if vuln_type in ["sql_injection", "command_injection"] else "medium",
                            "line_number": line_num,
                            "pattern": pattern
                        })
        
        return vulnerabilities
    
    def _check_input_validation(self, entity, source_code: str) -> Dict[str, Any]:
        """检查输入验证"""
        validation = {
            "has_validation": False,
            "validation_methods": []
        }
        
        if source_code:
            # 查找常见的验证模式
            validation_patterns = [
                "if not ",
                "assert ",
                "validate",
                "check",
                "verify",
                "isinstance"
            ]
            
            for pattern in validation_patterns:
                if pattern in source_code:
                    validation["has_validation"] = True
                    validation["validation_methods"].append(pattern)
        
        return validation


class MultiDimensionAnalysisCoordinator:
    """
    多维度分析协调器
    
    功能：
    1. 协调多个分析器
    2. 交叉验证结果
    3. 融合多维度知识
    4. 冲突解决
    5. 综合报告生成
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化协调器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
        
        # 初始化各维度分析器
        self.static_analyzer = StaticAnalyzer(config)
        self.dynamic_analyzer = DynamicAnalyzer(config)
        self.semantic_analyzer = SemanticAnalyzer(config)
        self.metrics_analyzer = MetricsAnalyzer(config)
        self.security_analyzer = SecurityAnalyzer(config)
        
        # 分析器映射
        self.analyzers = {
            AnalysisDimension.STATIC: self.static_analyzer,
            AnalysisDimension.DYNAMIC: self.dynamic_analyzer,
            AnalysisDimension.SEMANTIC: self.semantic_analyzer,
            AnalysisDimension.METRICS: self.metrics_analyzer,
            AnalysisDimension.SECURITY: self.security_analyzer
        }
    
    async def coordinate_analysis(
        self,
        entity,
        source_code: str = None,
        dimensions: List[AnalysisDimension] = None
    ) -> MergedResult:
        """
        协调多维度分析
        
        Args:
            entity: 代码实体
            source_code: 源代码
            dimensions: 需要分析的维度（None 表示所有维度）
            
        Returns:
            融合结果
        """
        entity_id = getattr(entity, "id", "")
        entity_name = getattr(entity, "name", "")
        entity_type = getattr(entity, "entity_type", "")
        
        # 确定需要分析的维度
        if dimensions is None:
            dimensions = list(self.analyzers.keys())
        
        # 并行执行各维度分析
        results = {}
        tasks = []
        
        for dimension in dimensions:
            if dimension in self.analyzers:
                analyzer = self.analyzers[dimension]
                tasks.append(analyzer.analyze(entity, source_code))
        
        # 执行分析
        for i, dimension in enumerate(dimensions):
            results[dimension] = tasks[i]
        
        # 交叉验证
        validation_result = self.validate_cross_dimension(entity, results)
        
        # 融合结果
        merged_data = self.merge_analysis_results(entity, results)
        
        # 计算综合置信度
        overall_confidence = self._calculate_overall_confidence(results)
        
        return MergedResult(
            entity_id=entity_id,
            entity_name=entity_name,
            entity_type=entity_type,
            results=results,
            merged_data=merged_data,
            overall_confidence=overall_confidence,
            validation_result=validation_result
        )
    
    def validate_cross_dimension(
        self,
        entity,
        results: Dict[AnalysisDimension, AnalysisResult]
    ) -> ValidationResult:
        """
        交叉验证各维度结果
        
        Args:
            entity: 代码实体
            results: 各维度分析结果
            
        Returns:
            验证结果
        """
        entity_id = getattr(entity, "id", "")
        entity_name = getattr(entity, "name", "")
        
        conflicts = []
        inconsistencies = []
        resolutions = {}
        
        # 检查复杂度一致性
        static_complexity = results.get(AnalysisDimension.STATIC, {}).data.get("complexity", {}).get("cyclomatic", 0)
        metrics_maintainability = results.get(AnalysisDimension.METRICS, {}).data.get("maintainability", {}).get("score", 100)
        
        if static_complexity > 10 and metrics_maintainability > 80:
            inconsistencies.append(
                f"静态分析复杂度高 ({static_complexity})，"
                f"但可维护性得分高 ({metrics_maintainability})"
            )
            resolutions["complexity_maintainability"] = "以可维护性得分为准，可能复杂度计算有误"
        
        # 检查安全问题与代码风格
        security_issues = results.get(AnalysisDimension.SECURITY, {}).issues
        static_issues = results.get(AnalysisDimension.STATIC, {}).issues
        
        if security_issues and not static_issues:
            inconsistencies.append(
                f"发现 {len(security_issues)} 个安全问题，"
                f"但静态分析未发现代码问题"
            )
        
        # 验证结果
        is_valid = len(conflicts) == 0 and len(inconsistencies) <= 1
        confidence = 1.0 if is_valid else 0.7
        
        return ValidationResult(
            entity_id=entity_id,
            entity_name=entity_name,
            is_valid=is_valid,
            confidence=confidence,
            conflicts=conflicts,
            inconsistencies=inconsistencies,
            resolutions=resolutions
        )
    
    def merge_analysis_results(
        self,
        entity,
        results: Dict[AnalysisDimension, AnalysisResult]
    ) -> Dict[str, Any]:
        """
        融合多维度分析结果
        
        Args:
            entity: 代码实体
            results: 各维度分析结果
            
        Returns:
            融合数据
        """
        merged = {
            "entity_summary": {},
            "issues": [],
            "suggestions": [],
            "metrics": {},
            "tags": []
        }
        
        # 收集所有问题和建议
        for dimension, result in results.items():
            merged["issues"].extend([
                f"[{dimension.value}] {issue}"
                for issue in result.issues
            ])
            merged["suggestions"].extend([
                f"[{dimension.value}] {suggestion}"
                for suggestion in result.suggestions
            ])
        
        # 融合指标
        if AnalysisDimension.STATIC in results:
            merged["metrics"].update(
                results[AnalysisDimension.STATIC].data.get("complexity", {})
            )
        
        if AnalysisDimension.METRICS in results:
            merged["metrics"].update(
                results[AnalysisDimension.METRICS].data.get("quality_metrics", {})
            )
        
        # 生成标签
        for dimension, result in results.items():
            if result.has_critical_issues():
                merged["tags"].append(f"critical:{dimension.value}")
            elif result.get_issue_count() > 0:
                merged["tags"].append(f"issues:{dimension.value}")
        
        # 实体摘要
        merged["entity_summary"] = {
            "name": getattr(entity, "name", ""),
            "type": getattr(entity, "entity_type", ""),
            "file": getattr(entity, "file_path", ""),
            "line": getattr(entity, "line_number", 0),
            "total_issues": len(merged["issues"]),
            "total_suggestions": len(merged["suggestions"])
        }
        
        return merged
    
    def _calculate_overall_confidence(
        self,
        results: Dict[AnalysisDimension, AnalysisResult]
    ) -> float:
        """
        计算综合置信度
        
        Args:
            results: 各维度分析结果
            
        Returns:
            综合置信度
        """
        if not results:
            return 0.0
        
        # 加权平均置信度
        weights = {
            AnalysisDimension.STATIC: 0.30,
            AnalysisDimension.SEMANTIC: 0.25,
            AnalysisDimension.DYNAMIC: 0.20,
            AnalysisDimension.METRICS: 0.15,
            AnalysisDimension.SECURITY: 0.10
        }
        
        total_weight = 0.0
        weighted_sum = 0.0
        
        for dimension, result in results.items():
            weight = weights.get(dimension, 0.2)
            weighted_sum += result.confidence * weight
            total_weight += weight
        
        return weighted_sum / total_weight if total_weight > 0 else 0.0
    
    def resolve_conflicts(
        self,
        conflicts: List[Conflict]
    ) -> List[Resolution]:
        """
        解决冲突
        
        Args:
            conflicts: 冲突列表
            
        Returns:
            解决方案列表
        """
        resolutions = []
        
        for conflict in conflicts:
            # 简化实现：优先使用静态分析结果
            if conflict.dimension1 == AnalysisDimension.STATIC:
                resolved_value = conflict.value1
                strategy = "prefer_static"
            elif conflict.dimension2 == AnalysisDimension.STATIC:
                resolved_value = conflict.value2
                strategy = "prefer_static"
            else:
                # 如果没有静态分析，选择置信度高的
                resolved_value = conflict.value1  # 默认
                strategy = "prefer_higher_confidence"
            
            resolution = Resolution(
                conflict_id=f"{conflict.entity_id}_{conflict.field}",
                strategy=strategy,
                resolved_value=resolved_value,
                reasoning=f"解决 {conflict.dimension1.value} 和 {conflict.dimension2.value} 之间的冲突"
            )
            resolutions.append(resolution)
        
        return resolutions
    
    def generate_comprehensive_report(
        self,
        merged_result: MergedResult
    ) -> Dict[str, Any]:
        """
        生成综合报告
        
        Args:
            merged_result: 融合结果
            
        Returns:
            综合报告
        """
        report = {
            "entity": {
                "id": merged_result.entity_id,
                "name": merged_result.entity_name,
                "type": merged_result.entity_type
            },
            "analysis_summary": {
                "dimensions_analyzed": len(merged_result.results),
                "overall_confidence": merged_result.overall_confidence,
                "validation_status": "passed" if merged_result.validation_result.is_valid else "failed"
            },
            "findings": {
                "total_issues": len(merged_result.get_all_issues()),
                "total_suggestions": len(merged_result.get_all_suggestions()),
                "critical_issues": [
                    issue for issue in merged_result.get_all_issues()
                    if "严重" in issue or "critical" in issue.lower()
                ]
            },
            "dimension_details": {}
        }
        
        # 添加各维度详情
        for dimension, result in merged_result.results.items():
            report["dimension_details"][dimension.value] = {
                "confidence": result.confidence,
                "issue_count": result.get_issue_count(),
                "suggestion_count": result.get_suggestion_count(),
                "has_critical": result.has_critical_issues()
            }
        
        return report


# 使用示例
if __name__ == "__main__":
    import asyncio
    
    # 模拟实体
    from dataclasses import dataclass
    
    @dataclass
    class MockEntity:
        id: str
        name: str
        entity_type: str
        file_path: str
        line_number: int
        content: str
        docstring: str = ""
        parameters: list = None
    
    entity = MockEntity(
        id="test_1",
        name="calculate_total",
        entity_type="function",
        file_path="src/example.py",
        line_number=10,
        content="""def calculate_total(items):
    total = 0
    for item in items:
        total += item['price']
    return total""",
        docstring="计算总价",
        parameters=[]
    )
    
    # 创建协调器
    coordinator = MultiDimensionAnalysisCoordinator()
    
    # 执行协调分析（异步）
    merged_result = asyncio.run(coordinator.coordinate_analysis(
        entity,
        source_code=entity.content
    ))
    
    # 输出结果
    print(f"实体: {merged_result.entity_name}")
    print(f"分析维度: {len(merged_result.results)}")
    print(f"综合置信度: {merged_result.overall_confidence:.2f}")
    
    print("\n各维度结果:")
    for dimension, result in merged_result.results.items():
        print(f"  {dimension.value}:")
        print(f"    置信度: {result.confidence:.2f}")
        print(f"    问题数: {result.get_issue_count()}")
        print(f"    建议数: {result.get_suggestion_count()}")
    
    # 生成综合报告
    report = coordinator.generate_comprehensive_report(merged_result)
    print("\n综合报告:")
    print(f"  总问题数: {report['findings']['total_issues']}")
    print(f"  总建议数: {report['findings']['total_suggestions']}")
    print(f"  验证状态: {report['analysis_summary']['validation_status']}")
