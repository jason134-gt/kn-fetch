#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""分析调用链追踪问题"""

import json
from pathlib import Path

# 读取知识图谱
with open("output/knowledge_graph", "r", encoding="utf-8") as f:
    data = json.load(f)

entities = data.get("entities", {})
relationships = data.get("relationships", [])

print(f"Total entities: {len(entities)}")
print(f"Total relationships: {len(relationships)}")

# 选择一个入口点
entry_entity = None
for entity in entities.values():
    if 'Service' in entity.get('name', '') and entity.get('entity_type') == 'method':
        entry_entity = entity
        break

if not entry_entity:
    print("No suitable entry point found")
    exit(1)

print(f"\nEntry point: {entry_entity.get('name')} (ID: {entry_entity.get('id')})")

# 追踪调用链
def trace_calls(entity_id, depth=0, max_depth=5):
    if depth > max_depth:
        return []
    
    chain = [entity_id]
    
    # 找到这个实体调用的所有方法
    for rel in relationships:
        if rel.get('source_id') == entity_id and rel.get('relationship_type') == 'calls':
            target_id = rel.get('target_id')
            print(f"  {'  ' * depth}Found call to: {target_id}")
            
            # 检查目标实体是否存在
            if target_id in entities:
                target_entity = entities[target_id]
                print(f"  {'  ' * depth}  -> {target_entity.get('name')} (exists)")
                # 递归追踪
                sub_chain = trace_calls(target_id, depth + 1, max_depth)
                chain.extend(sub_chain)
            else:
                print(f"  {'  ' * depth}  -> Target NOT in entities")
    
    return chain

print("\nTracing call chain:")
call_chain = trace_calls(entry_entity.get('id'))

print(f"\nCall chain length: {len(call_chain)}")
print("\nCall chain entities:")
for i, entity_id in enumerate(call_chain[:10], 1):
    if entity_id in entities:
        entity = entities[entity_id]
        print(f"  {i}. {entity.get('name')}")
    else:
        print(f"  {i}. ID: {entity_id} (not in entities)")
