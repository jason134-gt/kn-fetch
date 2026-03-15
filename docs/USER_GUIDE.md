# KN-Fetch Agent框架 - 用户使用指南

**版本**: v1.0  
**更新日期**: 2026-03-13

---

## 📚 目录

1. [简介](#简介)
2. [快速开始](#快速开始)
3. [核心功能](#核心功能)
4. [模块使用指南](#模块使用指南)
5. [最佳实践](#最佳实践)
6. [常见问题](#常见问题)

---

## 简介

KN-Fetch Agent框架是一个专业的代码库分析和文档生成系统，通过多Agent协作机制，生成高质量的架构分析报告。

### 核心特性

- **多Agent协作**: 13个专业Agent并行分析代码库
- **质量保证**: 自动化质量检测和优化
- **专业输出**: 符合技术写作规范的高质量文档
- **智能优化**: 迭代优化机制持续改进文档质量

---

## 快速开始

### 环境要求

- Python 3.8+
- 操作系统: Windows/Linux/macOS

### 安装

```bash
# 克隆项目
git clone <repository_url>
cd kn-fetch

# 安装依赖
pip install -r requirements.txt
```

### 基本使用

```python
# 1. 导入必要的模块
from src.agents.writing import ParagraphWritingEngine, StyleValidator, MermaidValidator
from src.agents.quality import IssueTracker
from src.agents.optimization import IterationOptimizer, QualityEvaluator

# 2. 初始化模块
writing_engine = ParagraphWritingEngine()
style_validator = StyleValidator()
mermaid_validator = MermaidValidator()
quality_evaluator = QualityEvaluator()
optimizer = IterationOptimizer()

# 3. 使用写作引擎
bullet_content = """
- 系统采用分层架构
- 包含三层结构
- 实现了关注点分离
"""
paragraph_content = writing_engine.write_paragraphs(bullet_content)

# 4. 验证写作风格
validation_result = style_validator.validate(paragraph_content)

# 5. 评估质量
quality_result = quality_evaluator.evaluate(paragraph_content)

# 6. 如果质量不达标，执行优化
if quality_result["total_score"] < 80:
    optimization_result = optimizer.optimize_content(
        paragraph_content,
        target_score=80.0
    )
```

---

## 核心功能

### 1. 写作质量优化 (Phase 3)

#### 段落式写作引擎

将清单式内容转换为专业的段落式叙述。

**示例**:
```python
from src.agents.writing import ParagraphWritingEngine

engine = ParagraphWritingEngine()

# 输入：清单式内容
bullet_content = """
- 功能A
- 功能B
- 功能C
"""

# 输出：段落式内容
paragraph = engine.write_paragraphs(bullet_content)
```

**功能特性**:
- 自动段落化转换
- 逻辑连接词插入
- 专业术语保护
- 写作风格统一

#### 写作风格验证器

验证文档是否符合技术写作规范。

**示例**:
```python
from src.agents.writing import StyleValidator

validator = StyleValidator()

# 验证内容
content = "我们设计了一个系统。开发者实现了功能。"
result = validator.validate(content)

# 检查结果
if not result.is_valid:
    print("发现问题:")
    for issue in result.issues:
        print(f"- {issue}")
    print("\n建议:")
    for suggestion in result.suggestions:
        print(f"- {suggestion}")
```

**验证项目**:
- 第三人称视角检查
- 段落式写作验证
- 专业术语使用检查
- 逻辑连接词使用验证

#### Mermaid图表验证器

验证Mermaid图表的正确性并提供修复建议。

**示例**:
```python
from src.agents.writing import MermaidValidator

validator = MermaidValidator()

# 验证Mermaid代码
mermaid_code = """
flowchart TD
    A[开始] --> B[处理]
    B --> C[结束]
"""

analysis = validator.validate_mermaid_code(mermaid_code)

# 检查验证结果
if not analysis.is_valid:
    print("发现问题:")
    for issue in analysis.issues:
        print(f"- [{issue.severity.value}] {issue.message}")
    
    # 生成修复建议
    suggestions = validator.generate_fix_suggestions(analysis)
    print("\n修复建议:")
    for suggestion in suggestions:
        print(f"- {suggestion}")
    
    # 自动修复
    fixed_code = validator.auto_fix_mermaid_code(mermaid_code)
```

**支持图表类型**:
- 流程图 (flowchart)
- 序列图 (sequenceDiagram)
- 类图 (classDiagram)
- 状态图 (stateDiagram)
- 甘特图 (gantt)
- 饼图 (pie)

---

### 2. 质量控制增强 (Phase 4)

#### 质量问题跟踪系统

检测和跟踪代码质量问题。

**示例**:
```python
from src.agents.quality import IssueTracker, QualityIssue, IssueType, IssueSeverity, IssueStatus
from datetime import datetime

# 创建跟踪器
tracker = IssueTracker()

# 检测代码中的问题
code_files = {
    "example.js": """
function test() {
    var x = 10  // 缺少分号
    console.log('Hello'  // 未闭合引号
}
"""
}

issues = tracker.scan_codebase(code_files)
print(f"检测到 {len(issues)} 个问题")

# 查看问题详情
for issue in issues:
    print(f"\n问题ID: {issue.id}")
    print(f"类型: {issue.type.value}")
    print(f"严重性: {issue.severity.value}")
    print(f"描述: {issue.description}")
    print(f"建议: {issue.suggested_fixes}")

# 生成质量报告
report = tracker.generate_report()
print("\n质量报告:")
print(f"总问题数: {report['summary']['total_issues']}")
print(f"未处理问题: {report['summary']['open_issues']}")
print(f"解决率: {report['summary']['resolution_rate']}%")
```

**问题类型**:
- SYNTAX_ERROR: 语法错误
- LOGIC_ERROR: 逻辑错误
- PERFORMANCE_ISSUE: 性能问题
- SECURITY_ISSUE: 安全问题
- STYLE_VIOLATION: 代码风格违规
- ARCHITECTURE_ISSUE: 架构问题

**严重性级别**:
- CRITICAL: 严重问题，必须立即修复
- HIGH: 高优先级问题
- MEDIUM: 中等优先级问题
- LOW: 低优先级问题
- INFO: 信息性提示

#### 迭代优化机制

自动优化内容质量直到达到目标。

**示例**:
```python
from src.agents.optimization import IterationOptimizer

# 创建优化器
optimizer = IterationOptimizer(max_iterations=3)

# 待优化的内容
content = "这个项目是个工具可以分析代码生成报告"

# 执行优化
result = optimizer.optimize_content(
    content,
    target_score=80.0
)

# 查看优化结果
print(f"原始分数: {result.original_score}")
print(f"优化后分数: {result.optimized_score}")
print(f"改进幅度: {result.improvement}")
print(f"迭代次数: {result.iterations}")
print(f"使用策略: {result.strategy_used.value}")

print("\n优化后的内容:")
print(result.optimized_content)
```

**优化策略**:
- AUTO_FIX: 自动修复
- REGENERATE: 重新生成
- ENHANCE: 增强改进
- CONSOLIDATE: 整合优化
- SIMPLIFY: 简化优化

#### 质量评估器

多维度评估内容质量。

**示例**:
```python
from src.agents.optimization import QualityEvaluator

# 创建评估器
evaluator = QualityEvaluator()

# 评估内容
content = """
# 项目介绍

这是一个代码分析工具，采用分层架构设计。

## 技术特点

- 模块化设计
- 可扩展性强
- 易于维护

```python
def example():
    return True
```
"""

result = evaluator.evaluate(content)

# 查看评估结果
print(f"总分: {result['total_score']}")
print(f"等级: {result['grade']}")

print("\n各项评分:")
for category, score in result['category_scores'].items():
    print(f"  {category}: {score}")

if result['issues']:
    print("\n发现的问题:")
    for issue in result['issues']:
        print(f"  - {issue}")
```

**评估维度**:
- 内容质量 (30%): 准确性、完整性、相关性
- 写作质量 (30%): 清晰度、流畅性、专业性
- 结构质量 (20%): 组织结构、层次分明
- 技术质量 (20%): 术语准确、技术正确

---

## 模块使用指南

### 写作模块 (src.agents.writing)

#### 导入模块

```python
from src.agents.writing import (
    ParagraphWritingEngine,
    StyleValidator,
    MermaidValidator
)
```

#### API参考

**ParagraphWritingEngine**

```python
class ParagraphWritingEngine:
    def write_paragraphs(self, content: str) -> str:
        """将清单式内容转换为段落式叙述"""
        
    def protect_technical_terms(self, text: str, terms: List[str]) -> str:
        """保护专业术语不被错误修改"""
        
    def insert_logical_connectors(self, text: str) -> str:
        """自动插入逻辑连接词"""
```

**StyleValidator**

```python
class StyleValidator:
    def validate(self, content: str) -> ValidationResult:
        """验证写作风格"""
        
    def check_third_person(self, content: str) -> List[str]:
        """检查第三人称视角"""
        
    def check_paragraph_style(self, content: str) -> List[str]:
        """检查段落式写作"""
```

**MermaidValidator**

```python
class MermaidValidator:
    def validate_mermaid_code(self, mermaid_code: str) -> ChartAnalysis:
        """验证Mermaid代码"""
        
    def generate_fix_suggestions(self, analysis: ChartAnalysis) -> List[str]:
        """生成修复建议"""
        
    def auto_fix_mermaid_code(self, mermaid_code: str) -> str:
        """自动修复Mermaid代码"""
```

### 质量模块 (src.agents.quality)

#### 导入模块

```python
from src.agents.quality import (
    IssueTracker,
    QualityIssue,
    IssueSeverity,
    IssueType,
    IssueStatus
)
```

#### API参考

**IssueTracker**

```python
class IssueTracker:
    def scan_codebase(self, code_files: Dict[str, str]) -> List[QualityIssue]:
        """扫描代码库，检测质量问题"""
        
    def add_issue(self, issue: QualityIssue):
        """添加问题到跟踪系统"""
        
    def update_issue_status(self, issue_id: str, new_status: IssueStatus):
        """更新问题状态"""
        
    def generate_report(self) -> Dict[str, Any]:
        """生成质量报告"""
```

### 优化模块 (src.agents.optimization)

#### 导入模块

```python
from src.agents.optimization import (
    IterationOptimizer,
    QualityEvaluator,
    ContentOptimizer,
    OptimizationStrategy
)
```

#### API参考

**IterationOptimizer**

```python
class IterationOptimizer:
    def optimize_content(
        self,
        content: str,
        target_score: float = 80.0
    ) -> OptimizationResult:
        """迭代优化内容"""
        
    def optimize_batch(
        self,
        contents: Dict[str, str],
        target_score: float = 80.0
    ) -> Dict[str, OptimizationResult]:
        """批量优化多个内容"""
```

**QualityEvaluator**

```python
class QualityEvaluator:
    def evaluate(self, content: str) -> Dict[str, Any]:
        """评估内容质量"""
```

---

## 最佳实践

### 1. 文档生成工作流

```python
# 完整的文档生成和优化工作流
def generate_quality_document(content: str) -> str:
    # 1. 转换为段落式写作
    engine = ParagraphWritingEngine()
    paragraph = engine.write_paragraphs(content)
    
    # 2. 验证写作风格
    validator = StyleValidator()
    validation = validator.validate(paragraph)
    
    # 3. 评估质量
    evaluator = QualityEvaluator()
    quality = evaluator.evaluate(paragraph)
    
    # 4. 如果质量不达标，迭代优化
    if quality["total_score"] < 80:
        optimizer = IterationOptimizer(max_iterations=3)
        result = optimizer.optimize_content(paragraph, target_score=80.0)
        return result.optimized_content
    
    return paragraph
```

### 2. Mermaid图表验证流程

```python
def validate_and_fix_mermaid(mermaid_code: str) -> str:
    # 1. 验证图表
    validator = MermaidValidator()
    analysis = validator.validate_mermaid_code(mermaid_code)
    
    # 2. 如果有问题，尝试自动修复
    if not analysis.is_valid:
        fixed_code = validator.auto_fix_mermaid_code(mermaid_code)
        
        # 3. 重新验证
        new_analysis = validator.validate_mermaid_code(fixed_code)
        
        if new_analysis.is_valid:
            return fixed_code
        else:
            # 4. 如果自动修复失败，生成建议
            suggestions = validator.generate_fix_suggestions(new_analysis)
            print("自动修复失败，请手动修复:")
            for suggestion in suggestions:
                print(f"- {suggestion}")
            return mermaid_code
    
    return mermaid_code
```

### 3. 质量监控流程

```python
def monitor_quality(code_files: Dict[str, str]) -> None:
    # 1. 扫描代码
    tracker = IssueTracker()
    issues = tracker.scan_codebase(code_files)
    
    # 2. 分类问题
    critical_issues = [i for i in issues if i.severity == IssueSeverity.CRITICAL]
    high_issues = [i for i in issues if i.severity == IssueSeverity.HIGH]
    
    # 3. 生成报告
    report = tracker.generate_report()
    
    # 4. 输出关键指标
    print(f"总问题数: {report['summary']['total_issues']}")
    print(f"严重问题: {len(critical_issues)}")
    print(f"高优先级问题: {len(high_issues)}")
    print(f"解决率: {report['summary']['resolution_rate']}%")
```

---

## 常见问题

### Q1: 如何选择合适的优化策略？

**A**: 系统会根据质量差距自动选择策略：
- 差距 > 20分：使用REGENERATE策略
- 差距 10-20分：使用ENHANCE策略
- 差距 < 10分：使用AUTO_FIX策略

### Q2: Mermaid图表验证失败怎么办？

**A**: 
1. 查看错误详情：`analysis.issues`
2. 查看修复建议：`validator.generate_fix_suggestions(analysis)`
3. 尝试自动修复：`validator.auto_fix_mermaid_code(code)`
4. 手动修复后重新验证

### Q3: 如何提高文档质量分数？

**A**: 
1. 使用段落式写作，避免清单罗列
2. 添加逻辑连接词，提升流畅性
3. 包含技术术语和代码示例
4. 添加Mermaid图表辅助说明
5. 确保第三人称客观叙述

### Q4: 问题跟踪系统支持哪些文件类型？

**A**: 
- 支持所有文本文件
- 特别优化了Python, JavaScript, Java等语言
- 可扩展添加新的检测规则

---

## 技术支持

如有问题，请查阅：
- API文档: `docs/API.md`
- 集成测试报告: `docs/INTEGRATION_TEST_REPORT.md`
- 开发计划: `docs/KNOWLEDGE_EXTRACTION_EXECUTION_PLAN.md`

---

**文档版本**: v1.0  
**最后更新**: 2026-03-13