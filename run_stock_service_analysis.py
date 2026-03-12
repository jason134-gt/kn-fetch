#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""重新运行 Stock_service 的完整分析流程"""

import sys
from pathlib import Path
import json
import yaml

# 添加项目路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.core.knowledge_extractor import KnowledgeExtractor
from src.ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer

# 分析目标目录
target_dir = "tests/example/Stock_service"

print("=" * 70)
print("重新运行 Stock_service 完整分析")
print("=" * 70)

# 加载配置
with open("config/config.yaml", "r", encoding="utf-8") as f:
    config = yaml.safe_load(f)

# 步骤 1: 基础知识提取
print(f"\n[步骤 1/2] 基础知识提取")
print("-" * 70)

extractor = KnowledgeExtractor("config/config.yaml")

graph = extractor.extract_from_directory(
    target_dir,
    include_code=True,
    include_docs=False,
    force=True,  # 强制重新分析
    deep_analysis=False,  # 先不启用深度分析
    refactoring_analysis=False
)

print("\n基础知识提取完成:")
print(f"  实体数量: {len(graph.entities)}")
print(f"  关系数量: {len(graph.relationships)}")
print(f"  总文件数: {len(set(e.file_path for e in graph.entities.values()))}")

if len(graph.entities) == 0:
    print("\n错误: 实体数量为0，无法继续深度分析")
    sys.exit(1)

# 步骤 2: LLM深度分析
print(f"\n[步骤 2/2] LLM深度知识分析")
print("-" * 70)

deep_analyzer = DeepKnowledgeAnalyzer(config, output_dir="output/doc")

if deep_analyzer.is_available():
    print("LLM服务可用，开始深度分析...")
    deep_result = deep_analyzer.analyze_all(graph, target_dir)
    
    print("\n深度分析完成:")
    for key, value in deep_result.items():
        if isinstance(value, dict) and value.get('file'):
            print(f"  - {key}: {value['file']}")
else:
    print("警告: LLM不可用，跳过深度分析")
    print("请确保已配置火山引擎API Key (ARK_API_KEY环境变量或config.yaml)")

print("\n" + "=" * 70)
print("完整分析流程完成")
print("=" * 70)

# 显示生成的文档
print("\n生成的文档:")
doc_dir = Path("output/doc")
if doc_dir.exists():
    for md_file in sorted(doc_dir.rglob("*.md")):
        print(f"  - {md_file.relative_to('output/doc')}")
