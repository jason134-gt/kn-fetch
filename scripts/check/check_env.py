#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查环境和模块"""

import os
import sys
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

print("=" * 80)
print("环境检查")
print("=" * 80)

# 检查API Key
api_key = os.environ.get("ARK_API_KEY", "")
print(f"ARK_API_KEY: {'已设置' if api_key else '未设置'}")

# 检查模块导入
try:
    from src.core.knowledge_extractor import KnowledgeExtractor
    print("KnowledgeExtractor: OK")
except ImportError as e:
    print(f"KnowledgeExtractor: 导入失败 - {e}")

# 检查源目录
source_dir = "tests/example/stock_datacenter"
if Path(source_dir).exists():
    print(f"源目录: 存在 - {source_dir}")
    # 统计文件数
    java_files = list(Path(source_dir).rglob("*.java"))
    xml_files = list(Path(source_dir).rglob("*.xml"))
    print(f"  Java文件数: {len(java_files)}")
    print(f"  XML文件数: {len(xml_files)}")
else:
    print(f"源目录: 不存在 - {source_dir}")

print("=" * 80)
