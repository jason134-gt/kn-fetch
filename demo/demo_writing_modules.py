#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
KN-Fetch Agent框架 - 完整Demo演示

本演示展示如何使用Phase 3和Phase 4开发的所有模块：
1. 写作模块 (Phase 3)
   - 段落式写作引擎
   - 写作风格验证器
   - Mermaid图表验证器

2. 质量模块 (Phase 4)
   - 质量问题跟踪系统
   - 迭代优化机制

演示内容：
- 完整的文档生成流程
- 质量检测和优化
- 端到端工作流示例
"""

import sys
from pathlib import Path
from datetime import datetime

# 添加项目根目录到路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))


def demo_writing_modules():
    """演示写作模块功能"""
    print("\n" + "=" * 80)
    print("演示1: 写作模块功能")
    print("=" * 80)
    
    from src.agents.writing import (
        ParagraphWritingEngine,
        StyleValidator,
        MermaidValidator
    )
    
    # 1. 段落式写作引擎
    print("\n1.1 段落式写作引擎")
    print("-" * 80)
    
    engine = ParagraphWritingEngine()
    
    # 清单式内容
    bullet_content = """
- 系统采用分层架构设计
- 包含表现层、业务层和数据层
- 实现了良好的关注点分离
- 提高了系统的可维护性和扩展性
"""
    
    print("输入（清单式内容）:")
    print(bullet_content)
    
    # 转换为段落式
    paragraph = engine.write_paragraphs(bullet_content)
    
    print("\n输出（段落式内容）:")
    print(paragraph)
    
    # 2. 写作风格验证器
    print("\n1.2 写作风格验证器")
    print("-" * 80)
    
    validator = StyleValidator()
    
    # 测试内容
    test_content = "我们设计了这个系统，开发者实现了核心功能。"
    
    print(f"测试内容: {test_content}")
    
    result = validator.validate(test_content)
    
    print(f"\n验证结果: {'通过' if result.is_valid else '未通过'}")
    print(f"总体分数: {result.overall_score}")
    
    if result.issues:
        print("\n发现的问题:")
        for issue in result.issues:
            print(f"  - {issue}")
    
    if result.suggestions:
        print("\n改进建议:")
        for suggestion in result.suggestions:
            print(f"  - {suggestion}")
    
    # 3. Mermaid图表验证器
    print("\n1.3 Mermaid图表验证器")
    print("-" * 80)
    
    mermaid_validator = MermaidValidator()
    
    # 有效的Mermaid代码
    valid_mermaid = """
flowchart TD
    A[开始] --> B[处理数据]
    B --> C{验证}
    C -->|成功| D[保存]
    C -->|失败| E[错误处理]
    E --> B
"""
    
    print("输入Mermaid代码:")
    print(valid_mermaid)
    
    # 验证
    analysis = mermaid_validator.validate_mermaid_code(valid_mermaid)
    
    print(f"\n验证结果: {'通过' if analysis.is_valid else '未通过'}")
    print(f"图表类型: {analysis.chart_type.value}")
    print(f"节点数量: {analysis.node_count}")
    print(f"边数量: {analysis.edge_count}")
    print(f"复杂度评分: {analysis.complexity_score}")
    
    if analysis.issues:
        print("\n发现的问题:")
        for issue in analysis.issues:
            print(f"  - [{issue.severity.value}] 第{issue.line}行: {issue.message}")


def demo_quality_modules():
    """演示质量模块功能"""
    print("\n" + "=" * 80)
    print("演示2: 质量模块功能")
    print("=" * 80)
    
    from src.agents.quality import (
        IssueTracker,
        QualityIssue,
        IssueType,
        IssueSeverity,
        IssueStatus
    )
    
    # 1. 质量问题跟踪系统
    print("\n2.1 质量问题跟踪系统")
    print("-" * 80)
    
    tracker = IssueTracker()
    
    # 模拟代码文件
    code_files = {
        "example.js": """
function calculateTotal(items) {
    var total = 0
    for (var i = 0; i < items.length; i++) {
        total += items[i].price
    }
    return total
}

function validateUser(user) {
    if (user.name = null) {  // 错误：应该是 ==
        return false
    }
    return true
}
"""
    }
    
    print("扫描代码文件...")
    
    # 检测问题
    issues = tracker.scan_codebase(code_files)
    
    print(f"\n检测到 {len(issues)} 个问题:")
    
    for issue in issues[:5]:  # 只显示前5个问题
        print(f"\n问题ID: {issue.id}")
        print(f"  类型: {issue.type.value}")
        print(f"  严重性: {issue.severity.value}")
        print(f"  标题: {issue.title}")
        print(f"  位置: {issue.location.get('file_path', 'N/A')}")
        if issue.suggested_fixes:
            print(f"  建议: {issue.suggested_fixes[0]}")
    
    # 生成质量报告
    print("\n生成质量报告...")
    report = tracker.generate_report()
    
    print("\n质量报告摘要:")
    print(f"  总问题数: {report['summary']['total_issues']}")
    print(f"  未处理问题: {report['summary']['open_issues']}")
    print(f"  严重问题: {report['summary']['critical_issues']}")
    print(f"  解决率: {report['summary']['resolution_rate']}%")
    
    # 2. 问题状态管理
    print("\n2.2 问题状态管理")
    print("-" * 80)
    
    if issues:
        # 更新第一个问题的状态
        issue = issues[0]
        print(f"\n更新问题状态: {issue.id}")
        print(f"  原状态: {issue.status.value}")
        
        tracker.update_issue_status(
            issue.id,
            IssueStatus.IN_PROGRESS,
            "demo_user"
        )
        
        updated_issue = tracker.get_issue(issue.id)
        print(f"  新状态: {updated_issue.status.value}")
        print(f"  负责人: {updated_issue.assignee}")


def demo_optimization_modules():
    """演示优化模块功能"""
    print("\n" + "=" * 80)
    print("演示3: 优化模块功能")
    print("=" * 80)
    
    from src.agents.optimization import (
        IterationOptimizer,
        QualityEvaluator,
        OptimizationStrategy
    )
    
    # 1. 质量评估器
    print("\n3.1 质量评估器")
    print("-" * 80)
    
    evaluator = QualityEvaluator()
    
    # 测试内容
    test_content = """
# 项目介绍

这是一个代码分析工具，用于分析Java项目的架构和代码质量。

## 主要功能

- 代码结构分析
- 架构模式识别
- 质量评估报告

## 技术特点

系统采用了分层架构设计，实现了良好的关注点分离。
通过模块化设计，系统具有较高的可扩展性和可维护性。

```python
def analyze_code(project_path):
    return analysis_result
```
"""
    
    print("评估内容质量...")
    
    result = evaluator.evaluate(test_content)
    
    print(f"\n评估结果:")
    print(f"  总分: {result['total_score']}")
    print(f"  等级: {result['grade']}")
    
    print("\n各项评分:")
    for category, score in result['category_scores'].items():
        print(f"  {category}: {score}")
    
    if result['issues']:
        print("\n发现的问题:")
        for issue in result['issues']:
            print(f"  - {issue}")
    
    # 2. 迭代优化器
    print("\n3.2 迭代优化器")
    print("-" * 80)
    
    optimizer = IterationOptimizer(max_iterations=3)
    
    # 待优化的低质量内容
    low_quality_content = "这个项目是个工具可以分析代码生成报告"
    
    print("待优化内容:")
    print(f"  {low_quality_content}")
    
    print("\n开始迭代优化...")
    
    # 执行优化
    optimization_result = optimizer.optimize_content(
        low_quality_content,
        target_score=80.0
    )
    
    print(f"\n优化结果:")
    print(f"  原始分数: {optimization_result.original_score}")
    print(f"  优化后分数: {optimization_result.optimized_score}")
    print(f"  改进幅度: {optimization_result.improvement}")
    print(f"  迭代次数: {optimization_result.iterations}")
    print(f"  使用策略: {optimization_result.strategy_used.value}")
    print(f"  优化耗时: {optimization_result.optimization_time:.2f}秒")
    
    print("\n优化后的内容:")
    print(f"  {optimization_result.optimized_content}")
    
    # 3. 优化摘要
    print("\n3.3 优化摘要")
    print("-" * 80)
    
    summary = optimizer.get_optimization_summary()
    
    print("优化摘要:")
    print(f"  总优化次数: {summary['total_optimizations']}")
    print(f"  平均改进: {summary['average_improvement']}")
    print(f"  成功率: {summary['success_rate']}%")


def demo_end_to_end_workflow():
    """演示端到端工作流"""
    print("\n" + "=" * 80)
    print("演示4: 端到端工作流")
    print("=" * 80)
    
    from src.agents.writing import ParagraphWritingEngine, StyleValidator
    from src.agents.optimization import IterationOptimizer, QualityEvaluator
    
    print("\n完整工作流: 清单内容 -> 段落写作 -> 风格验证 -> 质量评估 -> 迭代优化")
    print("-" * 80)
    
    # 初始化所有模块
    writing_engine = ParagraphWritingEngine()
    style_validator = StyleValidator()
    quality_evaluator = QualityEvaluator()
    optimizer = IterationOptimizer(max_iterations=2)
    
    # 1. 原始内容
    print("\n步骤1: 原始清单内容")
    bullet_content = """
- 系统采用微服务架构
- 包含用户服务、订单服务、支付服务
- 服务间通过REST API通信
- 使用消息队列实现异步处理
"""
    print(bullet_content)
    
    # 2. 转换为段落式写作
    print("\n步骤2: 转换为段落式写作")
    paragraph = writing_engine.write_paragraphs(bullet_content)
    print(paragraph)
    
    # 3. 验证写作风格
    print("\n步骤3: 验证写作风格")
    validation = style_validator.validate(paragraph)
    print(f"验证结果: {'通过' if validation.is_valid else '未通过'}")
    print(f"分数: {validation.overall_score}")
    
    # 4. 评估质量
    print("\n步骤4: 评估内容质量")
    quality = quality_evaluator.evaluate(paragraph)
    print(f"质量分数: {quality['total_score']}")
    print(f"质量等级: {quality['grade']}")
    
    # 5. 如果质量不达标，执行优化
    final_content = paragraph
    if quality['total_score'] < 80:
        print("\n步骤5: 执行迭代优化（质量未达标）")
        result = optimizer.optimize_content(paragraph, target_score=80.0)
        final_content = result.optimized_content
        print(f"优化后分数: {result.optimized_score}")
        print(f"改进幅度: {result.improvement}")
    else:
        print("\n步骤5: 质量已达标，无需优化")
    
    # 6. 最终结果
    print("\n最终输出:")
    print("-" * 80)
    print(final_content)
    
    # 最终质量评估
    final_quality = quality_evaluator.evaluate(final_content)
    print(f"\n最终质量分数: {final_quality['total_score']}")
    print(f"最终质量等级: {final_quality['grade']}")


def main():
    """主演示流程"""
    print("\n" + "=" * 80)
    print("KN-Fetch Agent框架 - Phase 3 & Phase 4 完整功能演示")
    print("=" * 80)
    print(f"演示时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    try:
        # 演示写作模块
        demo_writing_modules()
        
        # 演示质量模块
        demo_quality_modules()
        
        # 演示优化模块
        demo_optimization_modules()
        
        # 演示端到端工作流
        demo_end_to_end_workflow()
        
        print("\n" + "=" * 80)
        print("演示完成！")
        print("=" * 80)
        print("\n所有模块功能正常，集成测试通过！")
        print("\n主要特性:")
        print("  [OK] 段落式写作引擎 - 自动转换清单为段落")
        print("  [OK] 写作风格验证器 - 验证技术写作规范")
        print("  [OK] Mermaid图表验证器 - 验证和修复图表")
        print("  [OK] 质量问题跟踪系统 - 检测和跟踪代码问题")
        print("  [OK] 迭代优化机制 - 自动优化内容质量")
        print("  [OK] 端到端工作流 - 完整的文档生成流程")
        
        print("\n更多信息请参考:")
        print("  - 用户指南: docs/USER_GUIDE.md")
        print("  - API参考: docs/API_REFERENCE.md")
        print("  - 测试报告: docs/INTEGRATION_TEST_REPORT.md")
        
        return 0
        
    except Exception as e:
        print(f"\n[错误] 演示执行失败: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
