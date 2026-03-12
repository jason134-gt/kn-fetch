#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""查看实体关系数据"""

import json
import sys
from pathlib import Path
from collections import defaultdict

# 读取知识图谱
graph_file = Path("output/knowledge_graph")
if not graph_file.exists():
    print("[ERROR] 知识图谱文件不存在: output/knowledge_graph")
    print("请先运行分析: python kn-fetch.py analyze <target_directory>")
    sys.exit(1)

print("读取知识图谱数据...")
with open(graph_file, "r", encoding="utf-8") as f:
    data = json.load(f)

entities = data.get("entities", {})
relationships = data.get("relationships", [])

print("\n" + "=" * 70)
print("Entity Relationship Data Overview")
print("=" * 70)

print(f"\n[Statistics]")
print(f"  Total entities: {len(entities)}")
print(f"  Total relationships: {len(relationships)}")

# 统计实体类型
entity_types = defaultdict(int)
for entity in entities.values():
    entity_types[entity.get('entity_type', 'unknown')] += 1

print(f"\n[Entity Type Distribution]")
for entity_type, count in sorted(entity_types.items(), key=lambda x: x[1], reverse=True):
    print(f"  {entity_type}: {count}")

# 统计关系类型
relationship_types = defaultdict(int)
for rel in relationships:
    relationship_types[rel.get('relationship_type', 'unknown')] += 1

print(f"\n[Relationship Type Distribution]")
for rel_type, count in sorted(relationship_types.items(), key=lambda x: x[1], reverse=True):
    print(f"  {rel_type}: {count}")

# 找出调用最多的方法
method_calls = defaultdict(int)
for rel in relationships:
    if rel.get('relationship_type') == 'calls':
        target_id = rel.get('target_id')
        if target_id in entities:
            target_name = entities[target_id].get('name', 'unknown')
            method_calls[target_name] += 1

print(f"\n[Most Called Methods - Top 10]")
for i, (method_name, count) in enumerate(sorted(method_calls.items(), key=lambda x: x[1], reverse=True)[:10], 1):
    print(f"  {i}. {method_name}: {count} calls")

# 找出调用其他方法最多的方法
caller_counts = defaultdict(int)
for rel in relationships:
    if rel.get('relationship_type') == 'calls':
        source_id = rel.get('source_id')
        if source_id in entities:
            caller_counts[entities[source_id].get('name', 'unknown')] += 1

print(f"\n[Most Active Callers - Top 10]")
for i, (method_name, count) in enumerate(sorted(caller_counts.items(), key=lambda x: x[1], reverse=True)[:10], 1):
    print(f"  {i}. {method_name}: calls {count} methods")

# 显示一些示例关系
print(f"\n[Sample Call Relationships - First 20]")
for i, rel in enumerate(relationships[:20], 1):
    source = entities.get(rel.get('source_id'), {})
    target = entities.get(rel.get('target_id'), {})
    source_name = source.get('name', 'unknown')
    target_name = target.get('name', 'unknown')
    call_site = rel.get('call_site', 'unknown')
    print(f"  {i}. {source_name} -> {target_name}")
    print(f"     Location: {call_site}")

print("\n" + "=" * 70)
print("[Tips]")
print("  - Full data stored in: output/knowledge_graph (JSON format)")
print("  - Entity docs stored in: output/doc/entities/ (Skill format)")
print("  - Each entity doc contains dependency information")
print("=" * 70)
