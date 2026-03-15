#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""测试yaml模块"""

import sys
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

try:
    import yaml
    print(f"yaml模块: OK (version {yaml.__version__})")
except ImportError as e:
    print(f"yaml模块: 失败 - {e}")

try:
    from src.core.knowledge_extractor import KnowledgeExtractor
    print("KnowledgeExtractor: OK")
except ImportError as e:
    print(f"KnowledgeExtractor: 失败 - {e}")
    import traceback
    traceback.print_exc()
