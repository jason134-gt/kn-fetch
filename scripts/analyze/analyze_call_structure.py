#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""详细分析关系数据结构"""

import json
from pathlib import Path

# 读取知识图谱
with open("output/knowledge_graph", "r", encoding="utf-8") as f:
    data = json.load(f)

entities = data.get("entities", {})
relationships = data.get("relationships", [])

print("=" * 70)
print("关系数据分析")
print("=" * 70)

# 统计方法的调用关系
method_call_out = {}  # method -> [calls to]
method_call_in = {}   # method -> [called by]

for entity in entities.values():
    if entity.get('entity_type') == 'method':
        entity_id = entity.get('id')
        method_call_out[entity_id] = []
        method_call_in[entity_id] = []

print(f"\n方法实体数: {len(method_call_out)}")

# 分析关系
matched_relationships = 0
unmatched_source = 0
unmatched_target = 0

for rel in relationships:
    source_id = rel.get('source_id')
    target_id = rel.get('target_id')
    
    source_exists = source_id in entities
    target_exists = target_id in entities
    
    if source_exists and target_exists:
        matched_relationships += 1
        # 记录调用关系
        if source_id in method_call_out:
            method_call_out[source_id].append(target_id)
        if target_id in method_call_in:
            method_call_in[target_id].append(source_id)
    elif not source_exists:
        unmatched_source += 1
    elif not target_exists:
        unmatched_target += 1

print(f"\n关系统计:")
print(f"  匹配的关系: {matched_relationships}")
print(f"  源实体不存在: {unmatched_source}")
print(f"  目标实体不存在: {unmatched_target}")

# 找出调用其他方法最多的方法
top_callers = sorted(
    [(eid, len(calls)) for eid, calls in method_call_out.items() if calls],
    key=lambda x: x[1],
    reverse=True
)[:10]

print(f"\n调用其他方法最多的方法（Top 10）:")
for i, (method_id, call_count) in enumerate(top_callers, 1):
    entity = entities.get(method_id, {})
    print(f"  {i}. {entity.get('name', 'unknown')}: 调用 {call_count} 个方法")
    print(f"     文件: {entity.get('file_path', 'unknown')}")
    # 显示调用链
    if method_call_out[method_id]:
        for j, target_id in enumerate(method_call_out[method_id][:3], 1):
            target_entity = entities.get(target_id, {})
            print(f"       {j}. -> {target_entity.get('name', 'unknown')}")

# 找出被调用最多的方法
top_callees = sorted(
    [(eid, len(callers)) for eid, callers in method_call_in.items() if callers],
    key=lambda x: x[1],
    reverse=True
)[:10]

print(f"\n被调用最多的方法（Top 10）:")
for i, (method_id, caller_count) in enumerate(top_callees, 1):
    entity = entities.get(method_id, {})
    print(f"  {i}. {entity.get('name', 'unknown')}: 被调用 {caller_count} 次")

print("\n" + "=" * 70)
