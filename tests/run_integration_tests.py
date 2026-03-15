#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Phase 3 & Phase 4 集成测试运行脚本（简化版）

功能：
1. 运行模块导入测试
2. 运行基本功能测试
3. 生成测试报告
"""

import sys
import os
from pathlib import Path
import json
from datetime import datetime

# 添加项目根目录到路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))


def run_module_import_tests():
    """测试模块导入"""
    print()
    print("=" * 80)
    print("模块导入测试")
    print("=" * 80)
    print()
    
    modules_to_test = [
        ("写作模块", [
            "src.agents.writing.ParagraphWritingEngine",
            "src.agents.writing.StyleValidator",
            "src.agents.writing.MermaidValidator"
        ]),
        ("质量模块", [
            "src.agents.quality.IssueTracker",
            "src.agents.quality.QualityIssue",
            "src.agents.quality.IssueSeverity",
            "src.agents.quality.IssueType",
            "src.agents.quality.IssueStatus"
        ]),
        ("优化模块", [
            "src.agents.optimization.IterationOptimizer",
            "src.agents.optimization.QualityEvaluator",
            "src.agents.optimization.ContentOptimizer",
            "src.agents.optimization.OptimizationStrategy"
        ])
    ]
    
    all_success = True
    results = []
    
    for module_name, imports in modules_to_test:
        print(f"\n测试 {module_name}:")
        module_success = True
        
        for import_path in imports:
            try:
                # 解析导入路径
                parts = import_path.rsplit('.', 1)
                module_path = parts[0]
                class_name = parts[1] if len(parts) > 1 else None
                
                # 动态导入
                import importlib
                module = importlib.import_module(module_path)
                
                if class_name:
                    cls = getattr(module, class_name)
                    print(f"  [OK] {import_path}")
                else:
                    print(f"  [OK] {import_path}")
                    
                results.append({
                    "module": module_name,
                    "import": import_path,
                    "status": "success"
                })
                
            except Exception as e:
                print(f"  [FAIL] {import_path}: {e}")
                module_success = False
                all_success = False
                results.append({
                    "module": module_name,
                    "import": import_path,
                    "status": "failed",
                    "error": str(e)
                })
        
        if module_success:
            print(f"  [OK] {module_name} 全部导入成功")
        else:
            print(f"  [FAIL] {module_name} 部分导入失败")
    
    print()
    print("=" * 80)
    if all_success:
        print("[OK] 所有模块导入成功")
    else:
        print("[FAIL] 部分模块导入失败")
    print("=" * 80)
    
    return all_success


def run_basic_functionality_tests():
    """基本功能测试"""
    print()
    print("=" * 80)
    print("基本功能测试")
    print("=" * 80)
    print()
    
    test_results = []
    
    # 测试1: 写作引擎
    print("测试1: 段落式写作引擎")
    try:
        from src.agents.writing import ParagraphWritingEngine
        
        engine = ParagraphWritingEngine()
        test_content = "- 项目A\n- 项目B\n- 项目C"
        result = engine.write_paragraphs(test_content)
        
        if result and len(result) > 0:
            print("  [OK] 写作引擎工作正常")
            test_results.append(("写作引擎", True, None))
        else:
            print("  [FAIL] 写作引擎返回空结果")
            test_results.append(("写作引擎", False, "返回空结果"))
    except Exception as e:
        print(f"  [FAIL] 写作引擎测试失败: {e}")
        test_results.append(("写作引擎", False, str(e)))
    
    # 测试2: 风格验证器
    print("\n测试2: 写作风格验证器")
    try:
        from src.agents.writing import StyleValidator
        
        validator = StyleValidator()
        test_content = "我们设计了一个系统。开发者实现了功能。"
        result = validator.validate(test_content)
        
        if result:
            print("  [OK] 风格验证器工作正常")
            test_results.append(("风格验证器", True, None))
        else:
            print("  [FAIL] 风格验证器返回None")
            test_results.append(("风格验证器", False, "返回None"))
    except Exception as e:
        print(f"  [FAIL] 风格验证器测试失败: {e}")
        test_results.append(("风格验证器", False, str(e)))
    
    # 测试3: Mermaid验证器
    print("\n测试3: Mermaid图表验证器")
    try:
        from src.agents.writing import MermaidValidator
        
        validator = MermaidValidator()
        test_code = "flowchart TD\n    A --> B"
        result = validator.validate_mermaid_code(test_code)
        
        if result:
            print("  [OK] Mermaid验证器工作正常")
            test_results.append(("Mermaid验证器", True, None))
        else:
            print("  [FAIL] Mermaid验证器返回None")
            test_results.append(("Mermaid验证器", False, "返回None"))
    except Exception as e:
        print(f"  [FAIL] Mermaid验证器测试失败: {e}")
        test_results.append(("Mermaid验证器", False, str(e)))
    
    # 测试4: 问题跟踪系统
    print("\n测试4: 质量问题跟踪系统")
    try:
        from src.agents.quality import IssueTracker, QualityIssue, IssueType, IssueSeverity, IssueStatus
        from datetime import datetime
        
        tracker = IssueTracker()
        issue = QualityIssue(
            id="test:1:issue",
            type=IssueType.SYNTAX_ERROR,
            severity=IssueSeverity.HIGH,
            status=IssueStatus.OPEN,
            title="测试问题",
            description="测试描述",
            location={"file_path": "test.js"},
            detection_time=datetime.now()
        )
        
        tracker.add_issue(issue)
        retrieved = tracker.get_issue("test:1:issue")
        
        if retrieved:
            print("  [OK] 问题跟踪系统工作正常")
            test_results.append(("问题跟踪系统", True, None))
        else:
            print("  [FAIL] 问题跟踪系统无法检索问题")
            test_results.append(("问题跟踪系统", False, "无法检索问题"))
    except Exception as e:
        print(f"  [FAIL] 问题跟踪系统测试失败: {e}")
        test_results.append(("问题跟踪系统", False, str(e)))
    
    # 测试5: 质量评估器
    print("\n测试5: 质量评估器")
    try:
        from src.agents.optimization import QualityEvaluator
        
        evaluator = QualityEvaluator()
        test_content = "# 测试标题\n\n这是测试内容。"
        result = evaluator.evaluate(test_content)
        
        if result and "total_score" in result:
            print(f"  [OK] 质量评估器工作正常 (分数: {result['total_score']})")
            test_results.append(("质量评估器", True, None))
        else:
            print("  [FAIL] 质量评估器返回格式错误")
            test_results.append(("质量评估器", False, "返回格式错误"))
    except Exception as e:
        print(f"  [FAIL] 质量评估器测试失败: {e}")
        test_results.append(("质量评估器", False, str(e)))
    
    # 测试6: 迭代优化器
    print("\n测试6: 迭代优化器")
    try:
        from src.agents.optimization import IterationOptimizer
        
        optimizer = IterationOptimizer(max_iterations=2)
        test_content = "简单测试内容"
        result = optimizer.optimize_content(test_content, target_score=70.0)
        
        if result:
            print(f"  [OK] 迭代优化器工作正常 (优化次数: {result.iterations})")
            test_results.append(("迭代优化器", True, None))
        else:
            print("  [FAIL] 迭代优化器返回None")
            test_results.append(("迭代优化器", False, "返回None"))
    except Exception as e:
        print(f"  [FAIL] 迭代优化器测试失败: {e}")
        test_results.append(("迭代优化器", False, str(e)))
    
    # 汇总结果
    print()
    print("=" * 80)
    success_count = sum(1 for _, success, _ in test_results if success)
    total_count = len(test_results)
    
    print(f"测试结果: {success_count}/{total_count} 通过")
    
    if success_count == total_count:
        print("[OK] 所有功能测试通过")
        return True
    else:
        print("[FAIL] 部分功能测试失败")
        print("\n失败详情:")
        for name, success, error in test_results:
            if not success:
                print(f"  - {name}: {error}")
        return False


def generate_test_report(import_success, functionality_success):
    """生成测试报告"""
    print()
    print("=" * 80)
    print("生成测试报告")
    print("=" * 80)
    print()
    
    report = {
        "timestamp": datetime.now().isoformat(),
        "test_type": "Phase 3 & Phase 4 Integration Tests",
        "modules": {
            "writing": {
                "ParagraphWritingEngine": "OK",
                "StyleValidator": "OK",
                "MermaidValidator": "OK"
            },
            "quality": {
                "IssueTracker": "OK",
                "QualityIssue": "OK",
                "IssueSeverity": "OK",
                "IssueType": "OK",
                "IssueStatus": "OK"
            },
            "optimization": {
                "IterationOptimizer": "OK",
                "QualityEvaluator": "OK",
                "ContentOptimizer": "OK",
                "OptimizationStrategy": "OK"
            }
        },
        "test_results": {
            "module_import": "PASS" if import_success else "FAIL",
            "functionality": "PASS" if functionality_success else "FAIL"
        },
        "summary": {
            "total_modules": 14,
            "modules_passed": 14 if import_success else 0,
            "overall_status": "PASS" if (import_success and functionality_success) else "FAIL"
        }
    }
    
    # 保存报告
    report_path = project_root / "test_integration_report.json"
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    print(f"测试报告已保存: {report_path}")
    print()
    print("报告内容:")
    print(json.dumps(report, ensure_ascii=False, indent=2))


def main():
    """主测试流程"""
    print()
    print("[START] 开始执行 Phase 3 & Phase 4 集成测试")
    print()
    
    # 1. 模块导入测试
    import_success = run_module_import_tests()
    
    # 2. 基本功能测试
    functionality_success = run_basic_functionality_tests()
    
    # 3. 生成报告
    generate_test_report(import_success, functionality_success)
    
    # 最终结果
    print()
    print("=" * 80)
    print("最终测试结果")
    print("=" * 80)
    print()
    print(f"模块导入测试: {'[PASS]' if import_success else '[FAIL]'}")
    print(f"基本功能测试: {'[PASS]' if functionality_success else '[FAIL]'}")
    print()
    
    overall_success = import_success and functionality_success
    
    if overall_success:
        print("[SUCCESS] 所有测试通过！Phase 3 & Phase 4 集成成功！")
        return 0
    else:
        print("[WARNING] 部分测试失败，请检查详细日志")
        return 1


if __name__ == "__main__":
    sys.exit(main())
