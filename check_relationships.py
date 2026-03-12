#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查关系数据结构"""

import json
from pathlib import Path

# 读取知识图谱
with open("output/knowledge_graph", "r", encoding="utf-8") as f:
    data = json.load(f)

entities = data.get("entities", {})
relationships = data.get("relationships", [])

print(f"Total entities: {len(entities)}")
print(f"Total relationships: {len(relationships)}")

# 检查前几个关系
print("\nFirst 5 relationships:")
for i, rel in enumerate(relationships[:5], 1):
    print(f"\n{i}. Relationship:")
    print(f"  Source ID: {rel.get('source_id')}")
    print(f"  Target ID: {rel.get('target_id')}")
    print(f"  Type: {rel.get('relationship_type')}")
    print(f"  Call site: {rel.get('call_site')}")
    
    # 检查实体是否存在
    source_id = rel.get('source_id')
    target_id = rel.get('target_id')
    
    if source_id in entities:
        source = entities[source_id]
        print(f"  Source name: {source.get('name')}")
        print(f"  Source type: {source.get('entity_type')}")
    else:
        print(f"  Source NOT FOUND in entities")
    
    if target_id in entities:
        target = entities[target_id]
        print(f"  Target name: {target.get('name')}")
        print(f"  Target type: {target.get('entity_type')}")
    else:
        print(f"  Target NOT FOUND in entities")

# 统计匹配情况
matched = 0
unmatched_source = 0
unmatched_target = 0

for rel in relationships:
    source_id = rel.get('source_id')
    target_id = rel.get('target_id')
    
    if source_id in entities:
        if target_id in entities:
            matched += 1
        else:
            unmatched_target += 1
    else:
        unmatched_source += 1

print(f"\n\nMatching statistics:")
print(f"  Both matched: {matched}")
print(f"  Source not found: {unmatched_source}")
print(f"  Target not found: {unmatched_target}")
