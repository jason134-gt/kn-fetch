#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""快速测试 Stock_service 分析"""

import sys
from pathlib import Path

# 添加项目路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.core.knowledge_extractor import KnowledgeExtractor

# 只分析service子目录进行快速测试
test_dir = "tests/example/Stock_service/src/com/yfzx/service"

print(f"开始快速测试: {test_dir}")
print("=" * 60)

extractor = KnowledgeExtractor("config/config.yaml")

graph = extractor.extract_from_directory(
    test_dir,
    include_code=True,
    include_docs=False,
    force=True,  # 强制重新分析
    deep_analysis=False,  # 不启用深度分析以加快速度
    refactoring_analysis=False
)

print("\n" + "=" * 60)
print("分析结果:")
print("=" * 60)
print(f"总文件数: {len(set(e.file_path for e in graph.entities.values()))}")
print(f"实体数量: {len(graph.entities)}")
print(f"关系数量: {len(graph.relationships)}")
print(f"总代码行数: {sum(e.lines_of_code for e in graph.entities.values() if hasattr(e, 'lines_of_code'))}")

if graph.entities:
    print("\n实体类型分布:")
    type_counts = {}
    for entity in graph.entities.values():
        entity_type = entity.entity_type.value
        type_counts[entity_type] = type_counts.get(entity_type, 0) + 1
    
    for entity_type, count in sorted(type_counts.items()):
        print(f"  {entity_type}: {count}")
    
    print("\n✓ 测试成功！实体提取正常工作")
else:
    print("\n✗ 测试失败！没有提取到任何实体")
    sys.exit(1)
