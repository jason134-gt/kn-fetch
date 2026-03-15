#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""最小化测试 - 检查重复输出"""

import sys
from pathlib import Path

print("=" * 80)
print("MINIMAL TEST START")
print("=" * 80)

# 设置路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

print(f"Project root: {project_root}")
print(f"Python version: {sys.version}")

print("=" * 80)
print("MINIMAL TEST END")
print("=" * 80)
