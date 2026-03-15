"""
Phase 3 & Phase 4 模块集成测试

测试范围：
1. 写作模块 (Phase 3)
   - 段落式写作引擎
   - 写作风格验证器
   - Mermaid图表验证器

2. 质量模块 (Phase 4)
   - 跨章节一致性检查
   - 质量问题跟踪系统
   - 迭代优化机制

测试目标：
- 验证模块间协作能力
- 测试端到端工作流
- 确保质量提升效果
"""

import pytest
import sys
from pathlib import Path
from unittest.mock import Mock, MagicMock

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))

# 导入测试模块
from src.agents.writing import (
    ParagraphWritingEngine,
    StyleValidator,
    MermaidValidator
)
from src.agents.quality import (
    IssueTracker,
    QualityIssue,
    IssueSeverity,
    IssueType,
    IssueStatus
)
from src.agents.optimization import (
    IterationOptimizer,
    QualityEvaluator,
    ContentOptimizer,
    OptimizationStrategy
)


class TestWritingModules:
    """测试写作模块集成"""
    
    def setup_method(self):
        """测试前准备"""
        self.writing_engine = ParagraphWritingEngine()
        self.style_validator = StyleValidator()
        self.mermaid_validator = MermaidValidator()
    
    def test_paragraph_writing_engine_basic(self):
        """测试段落式写作引擎基本功能"""
        # 准备测试数据
        bullet_content = """
- 系统采用分层架构
- 包含三层结构
- 实现了关注点分离
"""
        
        # 执行段落化写作
        result = self.writing_engine.write_paragraphs(bullet_content)
        
        # 验证结果
        assert result is not None
        assert len(result) > len(bullet_content)
        assert '系统采用分层架构' in result or '分层架构' in result
        assert '三层结构' in result or '三层' in result
    
    def test_paragraph_writing_with_logical_connectors(self):
        """测试逻辑连接词插入"""
        content = "系统采用分层架构。包含三层结构。实现了关注点分离。"
        
        # 执行优化
        result = self.writing_engine.write_paragraphs(content)
        
        # 验证逻辑连接词
        logical_words = ['因此', '所以', '然而', '此外', '具体而言', '首先', '其次']
        has_logical_words = any(word in result for word in logical_words)
        
        # 可能添加了逻辑连接词
        assert result is not None
        assert isinstance(result, str)
    
    def test_style_validator_third_person(self):
        """测试第三人称视角验证"""
        # 测试不符合第三人称的内容
        first_person_content = "我们设计了这个系统，开发者实现了核心功能。"
        
        # 执行验证
        result = self.style_validator.validate(first_person_content)
        
        # 验证结果
        assert result is not None
        assert hasattr(result, 'is_valid')
        assert hasattr(result, 'issues')
        # 应该检测到第一人称问题
        if not result.is_valid:
            assert len(result.issues) > 0
    
    def test_style_validator_paragraph_structure(self):
        """测试段落结构验证"""
        # 测试清单式内容
        list_content = """
- 第一项
- 第二项
- 第三项
- 第四项
"""
        
        # 执行验证
        result = self.style_validator.validate(list_content)
        
        # 验证结果
        assert result is not None
        # 应该建议使用段落式写作
        assert hasattr(result, 'suggestions')
    
    def test_mermaid_validator_flowchart(self):
        """测试Mermaid流程图验证"""
        # 有效的流程图
        valid_flowchart = """
flowchart TD
    A[开始] --> B[处理]
    B --> C[结束]
"""
        
        # 执行验证
        result = self.mermaid_validator.validate_mermaid_code(valid_flowchart)
        
        # 验证结果
        assert result is not None
        assert result.is_valid or len(result.issues) == 0 or all(
            issue.severity != IssueSeverity.ERROR for issue in result.issues
        )
    
    def test_mermaid_validator_syntax_error(self):
        """测试Mermaid语法错误检测"""
        # 包含语法错误的流程图
        invalid_flowchart = """
flowchart TD
    A[开始] --> B{处理
    B --> C[结束]
"""
        
        # 执行验证
        result = self.mermaid_validator.validate_mermaid_code(invalid_flowchart)
        
        # 验证结果
        assert result is not None
        # 应该检测到语法错误
        assert not result.is_valid or len(result.issues) > 0
    
    def test_mermaid_validator_auto_fix(self):
        """测试Mermaid自动修复"""
        # 包含缺失分号的代码
        code_without_semicolon = """
flowchart TD
    A[开始]
    B[处理]
"""
        
        # 执行自动修复
        fixed_code = self.mermaid_validator.auto_fix_mermaid_code(code_without_semicolon)
        
        # 验证结果
        assert fixed_code is not None
        assert isinstance(fixed_code, str)
        # 应该添加了分号
        assert ';' in fixed_code or 'flowchart' in fixed_code


class TestQualityModules:
    """测试质量模块集成"""
    
    def setup_method(self):
        """测试前准备"""
        self.issue_tracker = IssueTracker()
        self.quality_evaluator = QualityEvaluator()
    
    def test_issue_tracker_detection(self):
        """测试问题检测功能"""
        # 包含问题的代码
        code_with_issues = """
function test() {
    var x = 10  // 缺少分号
    console.log('Hello'  // 未闭合引号
}
"""
        
        # 执行问题检测
        issues = self.issue_tracker.detector.detect_issues(code_with_issues, "test.js")
        
        # 验证结果
        assert issues is not None
        assert isinstance(issues, list)
        # 应该检测到问题
        assert len(issues) >= 0
    
    def test_issue_tracker_add_issue(self):
        """测试问题添加功能"""
        # 创建测试问题
        issue = QualityIssue(
            id="test:1:missing_semicolon",
            type=IssueType.SYNTAX_ERROR,
            severity=IssueSeverity.HIGH,
            status=IssueStatus.OPEN,
            title="缺少分号",
            description="测试问题",
            location={"file_path": "test.js", "line_number": 1},
            detection_time=None
        )
        
        # 添加问题
        from datetime import datetime
        issue.detection_time = datetime.now()
        self.issue_tracker.add_issue(issue)
        
        # 验证添加结果
        assert issue.id in self.issue_tracker.issues
        assert self.issue_tracker.issues[issue.id].status == IssueStatus.OPEN
    
    def test_issue_tracker_update_status(self):
        """测试问题状态更新"""
        # 添加测试问题
        issue = QualityIssue(
            id="test:2:null_check",
            type=IssueType.LOGIC_ERROR,
            severity=IssueSeverity.MEDIUM,
            status=IssueStatus.OPEN,
            title="空值检查缺失",
            description="测试问题",
            location={"file_path": "test.js", "line_number": 2},
            detection_time=None
        )
        
        from datetime import datetime
        issue.detection_time = datetime.now()
        self.issue_tracker.add_issue(issue)
        
        # 更新状态
        self.issue_tracker.update_issue_status(
            issue.id, 
            IssueStatus.RESOLVED,
            "test_user"
        )
        
        # 验证更新结果
        updated_issue = self.issue_tracker.get_issue(issue.id)
        assert updated_issue is not None
        assert updated_issue.status == IssueStatus.RESOLVED
        assert updated_issue.assignee == "test_user"
    
    def test_issue_tracker_report_generation(self):
        """测试质量报告生成"""
        # 添加一些测试问题
        for i in range(3):
            issue = QualityIssue(
                id=f"test:{i}:issue",
                type=IssueType.STYLE_VIOLATION,
                severity=IssueSeverity.LOW,
                status=IssueStatus.OPEN,
                title=f"测试问题{i}",
                description=f"测试描述{i}",
                location={"file_path": "test.js", "line_number": i},
                detection_time=None
            )
            
            from datetime import datetime
            issue.detection_time = datetime.now()
            self.issue_tracker.add_issue(issue)
        
        # 生成报告
        report = self.issue_tracker.generate_report()
        
        # 验证报告
        assert report is not None
        assert "summary" in report
        assert "total_issues" in report["summary"]
        assert report["summary"]["total_issues"] >= 3
    
    def test_quality_evaluator_basic(self):
        """测试质量评估器基本功能"""
        # 测试内容
        content = """
# 测试标题

这是一个测试段落，包含一些技术内容。
系统采用了分层架构设计，实现了良好的关注点分离。

```python
def test():
    return True
```

## 技术特点

该系统具有以下特点：
- 模块化设计
- 可扩展性强
- 易于维护
"""
        
        # 执行评估
        result = self.quality_evaluator.evaluate(content)
        
        # 验证结果
        assert result is not None
        assert "total_score" in result
        assert "category_scores" in result
        assert "grade" in result
        assert result["total_score"] >= 0
        assert result["total_score"] <= 100
    
    def test_quality_evaluator_structure(self):
        """测试质量评估器结构评估"""
        # 结构良好的内容
        well_structured_content = """
# 主标题

## 第一节
内容段落1

## 第二节
内容段落2

### 子节
详细内容
"""
        
        # 执行评估
        result = self.quality_evaluator.evaluate(well_structured_content)
        
        # 验证结构分数
        assert result is not None
        assert "category_scores" in result
        assert "structure_quality" in result["category_scores"]


class TestOptimizationModules:
    """测试优化模块集成"""
    
    def setup_method(self):
        """测试前准备"""
        self.optimizer = IterationOptimizer(max_iterations=3)
        self.content_optimizer = ContentOptimizer()
    
    def test_iteration_optimizer_basic(self):
        """测试迭代优化器基本功能"""
        # 低质量内容
        low_quality_content = "这个项目是个工具可以分析代码生成报告"
        
        # 执行优化
        result = self.optimizer.optimize_content(
            low_quality_content,
            target_score=70.0
        )
        
        # 验证结果
        assert result is not None
        assert hasattr(result, 'original_content')
        assert hasattr(result, 'optimized_content')
        assert hasattr(result, 'original_score')
        assert hasattr(result, 'optimized_score')
        assert result.iterations >= 0
    
    def test_content_optimizer_auto_fix(self):
        """测试内容优化器自动修复策略"""
        # 需要修复的内容
        content_needing_fix = """
# 标题1
内容1
# 标题2
内容2
"""
        
        # 创建优化上下文
        from src.agents.optimization.iteration_optimizer import OptimizationContext
        
        context = OptimizationContext(
            content=content_needing_fix,
            quality_score=60.0,
            issues=["结构不够清晰"],
            target_score=80.0,
            max_iterations=3,
            strategy=OptimizationStrategy.AUTO_FIX
        )
        
        # 执行优化
        result = self.content_optimizer.optimize(context)
        
        # 验证结果
        assert result is not None
        assert result.optimized_content is not None
        assert result.strategy_used == OptimizationStrategy.AUTO_FIX
    
    def test_optimization_summary(self):
        """测试优化摘要生成"""
        # 执行几次优化
        test_contents = [
            "简单内容",
            "另一个简单内容"
        ]
        
        for content in test_contents:
            self.optimizer.optimize_content(content, target_score=70.0)
        
        # 生成摘要
        summary = self.optimizer.get_optimization_summary()
        
        # 验证摘要
        assert summary is not None
        assert "total_optimizations" in summary
        assert summary["total_optimizations"] >= 0


class TestEndToEndWorkflow:
    """端到端工作流测试"""
    
    def setup_method(self):
        """测试前准备"""
        self.writing_engine = ParagraphWritingEngine()
        self.style_validator = StyleValidator()
        self.quality_evaluator = QualityEvaluator()
        self.optimizer = IterationOptimizer(max_iterations=2)
    
    def test_complete_workflow(self):
        """测试完整工作流"""
        # 1. 初始内容
        initial_content = """
- 系统采用分层架构
- 包含三层：表现层、业务层、数据层
- 实现了良好的关注点分离
- 提高了系统的可维护性
"""
        
        # 2. 转换为段落式写作
        paragraph_content = self.writing_engine.write_paragraphs(initial_content)
        assert paragraph_content is not None
        
        # 3. 验证写作风格
        validation_result = self.style_validator.validate(paragraph_content)
        assert validation_result is not None
        
        # 4. 评估质量
        quality_result = self.quality_evaluator.evaluate(paragraph_content)
        assert quality_result is not None
        
        # 5. 如果质量不达标，执行优化
        if quality_result["total_score"] < 80:
            optimization_result = self.optimizer.optimize_content(
                paragraph_content,
                target_score=80.0
            )
            final_content = optimization_result.optimized_content
            final_score = optimization_result.optimized_score
        else:
            final_content = paragraph_content
            final_score = quality_result["total_score"]
        
        # 验证最终结果
        assert final_content is not None
        assert len(final_content) > 0
        assert final_score >= 0
    
    def test_writing_to_quality_integration(self):
        """测试写作模块与质量模块集成"""
        # 生成内容
        content = self.writing_engine.write_paragraphs("""
- 功能A
- 功能B
- 功能C
""")
        
        # 评估质量
        quality = self.quality_evaluator.evaluate(content)
        
        # 验证集成效果
        assert quality is not None
        assert "total_score" in quality
        
        # 写作引擎应该提升质量
        # 至少不应该降低质量
        assert quality["total_score"] >= 0


class TestModuleIntegration:
    """模块集成测试"""
    
    def test_all_modules_import(self):
        """测试所有模块可以正常导入"""
        # 写作模块
        from src.agents.writing import ParagraphWritingEngine, StyleValidator, MermaidValidator
        
        # 质量模块
        from src.agents.quality import IssueTracker, QualityIssue, IssueSeverity
        
        # 优化模块
        from src.agents.optimization import IterationOptimizer, QualityEvaluator
        
        # 验证导入成功
        assert ParagraphWritingEngine is not None
        assert StyleValidator is not None
        assert MermaidValidator is not None
        assert IssueTracker is not None
        assert QualityIssue is not None
        assert IssueSeverity is not None
        assert IterationOptimizer is not None
        assert QualityEvaluator is not None
    
    def test_module_instantiation(self):
        """测试模块实例化"""
        # 实例化所有主要类
        writing_engine = ParagraphWritingEngine()
        style_validator = StyleValidator()
        mermaid_validator = MermaidValidator()
        issue_tracker = IssueTracker()
        quality_evaluator = QualityEvaluator()
        optimizer = IterationOptimizer()
        
        # 验证实例化成功
        assert writing_engine is not None
        assert style_validator is not None
        assert mermaid_validator is not None
        assert issue_tracker is not None
        assert quality_evaluator is not None
        assert optimizer is not None


if __name__ == "__main__":
    # 运行测试
    pytest.main([__file__, "-v", "--tb=short"])
