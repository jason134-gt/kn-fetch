# KN-Fetch Agent框架 - API快速参考

**版本**: v1.0  
**更新日期**: 2026-03-13

---

## 快速导航

- [写作模块 API](#写作模块-api)
- [质量模块 API](#质量模块-api)
- [优化模块 API](#优化模块-api)

---

## 写作模块 API

### ParagraphWritingEngine

```python
from src.agents.writing import ParagraphWritingEngine

# 初始化
engine = ParagraphWritingEngine()

# 转换清单为段落
paragraph = engine.write_paragraphs(bullet_content: str) -> str
```

### StyleValidator

```python
from src.agents.writing import StyleValidator

# 初始化
validator = StyleValidator()

# 验证写作风格
result = validator.validate(content: str) -> ValidationResult

# 检查第三人称
issues = validator.check_third_person(content: str) -> List[str]

# 检查段落风格
issues = validator.check_paragraph_style(content: str) -> List[str]
```

### MermaidValidator

```python
from src.agents.writing import MermaidValidator

# 初始化
validator = MermaidValidator()

# 验证Mermaid代码
analysis = validator.validate_mermaid_code(mermaid_code: str) -> ChartAnalysis

# 生成修复建议
suggestions = validator.generate_fix_suggestions(analysis: ChartAnalysis) -> List[str]

# 自动修复
fixed_code = validator.auto_fix_mermaid_code(mermaid_code: str) -> str
```

---

## 质量模块 API

### IssueTracker

```python
from src.agents.quality import IssueTracker, QualityIssue, IssueType, IssueSeverity, IssueStatus

# 初始化
tracker = IssueTracker(storage_path: Optional[str] = None)

# 扫描代码库
issues = tracker.scan_codebase(code_files: Dict[str, str]) -> List[QualityIssue]

# 添加问题
tracker.add_issue(issue: QualityIssue) -> None

# 更新状态
tracker.update_issue_status(
    issue_id: str, 
    new_status: IssueStatus,
    assignee: Optional[str] = None
) -> None

# 获取问题
issue = tracker.get_issue(issue_id: str) -> Optional[QualityIssue]

# 生成报告
report = tracker.generate_report() -> Dict[str, Any]

# 自动修复
fixed_files = tracker.auto_fix_issues(code_files: Dict[str, str]) -> Dict[str, str]
```

### QualityIssue

```python
from datetime import datetime

issue = QualityIssue(
    id: str,
    type: IssueType,
    severity: IssueSeverity,
    status: IssueStatus,
    title: str,
    description: str,
    location: Dict[str, Any],
    detection_time: datetime,
    assignee: Optional[str] = None,
    priority: int = 0,
    tags: List[str] = [],
    evidence: List[str] = [],
    suggested_fixes: List[str] = [],
    related_issues: List[str] = [],
    history: List[Dict[str, Any]] = []
)
```

### 枚举类型

```python
# 问题类型
from src.agents.quality import IssueType
IssueType.SYNTAX_ERROR          # 语法错误
IssueType.LOGIC_ERROR           # 逻辑错误
IssueType.PERFORMANCE_ISSUE     # 性能问题
IssueType.SECURITY_ISSUE        # 安全问题
IssueType.STYLE_VIOLATION       # 代码风格违规
IssueType.ARCHITECTURE_ISSUE    # 架构问题

# 严重性
from src.agents.quality import IssueSeverity
IssueSeverity.CRITICAL          # 严重
IssueSeverity.HIGH              # 高
IssueSeverity.MEDIUM            # 中等
IssueSeverity.LOW               # 低
IssueSeverity.INFO              # 信息

# 状态
from src.agents.quality import IssueStatus
IssueStatus.OPEN                # 打开
IssueStatus.IN_PROGRESS         # 进行中
IssueStatus.RESOLVED            # 已解决
IssueStatus.CLOSED              # 已关闭
IssueStatus.WONT_FIX            # 不修复
```

---

## 优化模块 API

### IterationOptimizer

```python
from src.agents.optimization import IterationOptimizer

# 初始化
optimizer = IterationOptimizer(
    llm_client=None,
    max_iterations: int = 3
)

# 优化单个内容
result = optimizer.optimize_content(
    content: str,
    target_score: float = 80.0,
    trigger: OptimizationTrigger = OptimizationTrigger.LOW_QUALITY_SCORE
) -> OptimizationResult

# 批量优化
results = optimizer.optimize_batch(
    contents: Dict[str, str],
    target_score: float = 80.0
) -> Dict[str, OptimizationResult]

# 获取优化摘要
summary = optimizer.get_optimization_summary() -> Dict[str, Any]
```

### QualityEvaluator

```python
from src.agents.optimization import QualityEvaluator

# 初始化
evaluator = QualityEvaluator()

# 评估质量
result = evaluator.evaluate(content: str) -> Dict[str, Any]
# 返回: {"total_score": float, "category_scores": dict, "grade": str, "issues": list}
```

### ContentOptimizer

```python
from src.agents.optimization import ContentOptimizer

# 初始化
optimizer = ContentOptimizer(llm_client=None)

# 执行优化
result = optimizer.optimize(context: OptimizationContext) -> OptimizationResult
```

### 枚举类型

```python
# 优化策略
from src.agents.optimization import OptimizationStrategy
OptimizationStrategy.AUTO_FIX      # 自动修复
OptimizationStrategy.REGENERATE    # 重新生成
OptimizationStrategy.ENHANCE       # 增强
OptimizationStrategy.CONSOLIDATE   # 整合
OptimizationStrategy.SIMPLIFY      # 简化

# 优化触发条件
from src.agents.optimization import OptimizationTrigger
OptimizationTrigger.LOW_QUALITY_SCORE     # 质量分数低
OptimizationTrigger.USER_FEEDBACK         # 用户反馈
OptimizationTrigger.CONSISTENCY_ISSUE     # 一致性问题
OptimizationTrigger.INCOMPLETE_CONTENT    # 内容不完整
OptimizationTrigger.PERFORMANCE_ISSUE     # 性能问题
```

---

## 数据模型

### ValidationResult

```python
@dataclass
class ValidationResult:
    is_valid: bool              # 是否通过
    issues: List[str]           # 问题列表
    suggestions: List[str]      # 建议
    scores: Dict[str, float]    # 评分
    overall_score: float        # 总分
```

### ChartAnalysis

```python
@dataclass
class ChartAnalysis:
    chart_type: ChartType
    node_count: int
    edge_count: int
    complexity_score: int
    issues: List[MermaidValidationResult]
    is_valid: bool
```

### OptimizationResult

```python
@dataclass
class OptimizationResult:
    original_content: str
    optimized_content: str
    original_score: float
    optimized_score: float
    improvement: float
    iterations: int
    issues_fixed: List[str]
    remaining_issues: List[str]
    optimization_time: float
    strategy_used: OptimizationStrategy
```

---

## 完整示例

```python
from src.agents.writing import ParagraphWritingEngine, StyleValidator
from src.agents.quality import IssueTracker
from src.agents.optimization import IterationOptimizer, QualityEvaluator

# 1. 写作优化
engine = ParagraphWritingEngine()
content = engine.write_paragraphs("- 功能A\n- 功能B")

# 2. 风格验证
validator = StyleValidator()
result = validator.validate(content)

# 3. 质量评估
evaluator = QualityEvaluator()
quality = evaluator.evaluate(content)

# 4. 迭代优化
if quality["total_score"] < 80:
    optimizer = IterationOptimizer(max_iterations=3)
    result = optimizer.optimize_content(content, target_score=80.0)
    content = result.optimized_content

# 5. 问题跟踪
tracker = IssueTracker()
issues = tracker.scan_codebase({"file.js": "code content"})
report = tracker.generate_report()
```

---

**API文档版本**: v1.0  
**最后更新**: 2026-03-13