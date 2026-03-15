#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""简单导入测试"""

import sys
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

print("开始导入测试...")
print()

# 测试写作模块
print("1. 测试写作模块导入...")
try:
    from src.agents.writing import ParagraphWritingEngine
    print("   [OK] ParagraphWritingEngine")
except Exception as e:
    print(f"   [FAIL] ParagraphWritingEngine: {e}")

try:
    from src.agents.writing import StyleValidator
    print("   [OK] StyleValidator")
except Exception as e:
    print(f"   [FAIL] StyleValidator: {e}")

try:
    from src.agents.writing import MermaidValidator
    print("   [OK] MermaidValidator")
except Exception as e:
    print(f"   [FAIL] MermaidValidator: {e}")

# 测试质量模块
print("\n2. 测试质量模块导入...")
try:
    from src.agents.quality import IssueTracker
    print("   [OK] IssueTracker")
except Exception as e:
    print(f"   [FAIL] IssueTracker: {e}")

try:
    from src.agents.quality import QualityIssue
    print("   [OK] QualityIssue")
except Exception as e:
    print(f"   [FAIL] QualityIssue: {e}")

try:
    from src.agents.quality import IssueSeverity
    print("   [OK] IssueSeverity")
except Exception as e:
    print(f"   [FAIL] IssueSeverity: {e}")

# 测试优化模块
print("\n3. 测试优化模块导入...")
try:
    from src.agents.optimization import IterationOptimizer
    print("   [OK] IterationOptimizer")
except Exception as e:
    print(f"   [FAIL] IterationOptimizer: {e}")

try:
    from src.agents.optimization import QualityEvaluator
    print("   [OK] QualityEvaluator")
except Exception as e:
    print(f"   [FAIL] QualityEvaluator: {e}")

print("\n导入测试完成！")
