#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""诊断完整数据流 - 从文件解析到知识图谱构建"""

import sys
from pathlib import Path
import json

# 添加项目路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.gitnexus import GitNexusClient, KnowledgeGraph, AnalysisResult

# 选择一个测试文件
test_file = Path("tests/example/Stock_service/src/com/yfzx/service/StockCenter.java")

if not test_file.exists():
    print(f"[FAIL] 文件不存在: {test_file}")
    sys.exit(1)

print("=== 步骤 1: 测试 GitNexusClient._analyze_single_file ===")

client = GitNexusClient("config/config.yaml")
print(f"[OK] GitNexusClient 创建成功")

# 设置root_dir
client.root_dir = Path("tests/example/Stock_service").resolve()

# 分析单个文件
print(f"\n分析文件: {test_file}")
result = client._analyze_single_file(test_file, force=True, root_dir=client.root_dir)

if result:
    print(f"[OK] 分析结果:")
    print(f"  文件路径: {result.file_path}")
    print(f"  语言: {result.language}")
    print(f"  实体数量: {len(result.entities)}")
    print(f"  关系数: {len(result.relationships)}")
    print(f"  代码行数: {result.lines_of_code}")
    
    if result.entities:
        print(f"\n  前5个实体:")
        for i, entity in enumerate(result.entities[:5], 1):
            print(f"    {i}. {entity.entity_type.value}: {entity.name} (ID: {entity.id[:8]}...)")
else:
    print(f"[FAIL] 分析失败，返回None")
    sys.exit(1)

print("\n=== 步骤 2: 测试 KnowledgeGraph.build_from_results ===")

# 创建一个包含单个结果的列表
results = [result]

graph = KnowledgeGraph.build_from_results(results)
print(f"[OK] 知识图谱构建成功")
print(f"  实体数量: {len(graph.entities)}")
print(f"  关系数量: {len(graph.relationships)}")

if graph.entities:
    print(f"\n  前5个实体:")
    for i, (entity_id, entity) in enumerate(list(graph.entities.items())[:5], 1):
        print(f"    {i}. {entity.entity_type.value}: {entity.name} (ID: {entity_id[:8]}...)")
else:
    print(f"[FAIL] 知识图谱没有实体")

print("\n=== 步骤 3: 模拟 knowledge_extractor 的流程 ===")

from src.core.knowledge_extractor import KnowledgeExtractor

extractor = KnowledgeExtractor("config/config.yaml")
print(f"[OK] KnowledgeExtractor 创建成功")

# 设置root_dir
extractor.gitnexus_client.root_dir = Path("tests/example/Stock_service").resolve()

# 手动处理单个文件
print(f"\n手动处理文件: {test_file}")
manual_result = extractor._process_code_file(test_file, force=True)

if manual_result:
    print(f"[OK] 手动处理结果:")
    print(f"  类型: {type(manual_result)}")
    print(f"  实体数量: {len(manual_result.entities)}")
    print(f"  关系数: {len(manual_result.relationships)}")
else:
    print(f"[FAIL] 手动处理失败，返回None")

print("\n=== 步骤 4: 测试完整的 extract_from_directory 流程 ===")

# 只分析一个子目录，避免处理太多文件
test_dir = Path("tests/example/Stock_service/src/com/yfzx/service")
print(f"\n分析目录: {test_dir}")

graph = extractor.extract_from_directory(
    str(test_dir),
    include_code=True,
    include_docs=False,
    force=True,
    deep_analysis=False,
    refactoring_analysis=False
)

print(f"\n[OK] 提取完成")
print(f"  实体数量: {len(graph.entities)}")
print(f"  关系数量: {len(graph.relationships)}")

if graph.entities:
    print(f"\n  文件分布:")
    file_counts = {}
    for entity in graph.entities.values():
        file_path = entity.file_path
        file_counts[file_path] = file_counts.get(file_path, 0) + 1
    
    for file_path, count in sorted(file_counts.items())[:10]:
        print(f"    {file_path}: {count} 个实体")
        
    print(f"\n  实体类型分布:")
    type_counts = {}
    for entity in graph.entities.values():
        entity_type = entity.entity_type.value
        type_counts[entity_type] = type_counts.get(entity_type, 0) + 1
    
    for entity_type, count in sorted(type_counts.items()):
        print(f"    {entity_type}: {count}")
else:
    print(f"[FAIL] 最终知识图谱没有实体!")

# 保存知识图谱到文件以供检查
output_path = Path("test_output_graph.json")
output_path.parent.mkdir(parents=True, exist_ok=True)
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(graph.model_dump(), f, ensure_ascii=False, indent=2)
print(f"\n知识图谱已保存到: {output_path}")
