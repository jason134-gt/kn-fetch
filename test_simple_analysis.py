#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""简化的测试脚本 - 逐步处理文件避免多线程问题"""

import sys
from pathlib import Path
import json
import time

# 添加项目路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.gitnexus import GitNexusClient, KnowledgeGraph

# 测试目录
test_dir = Path("tests/example/Stock_service/src/com/yfzx/service")

print("=" * 70)
print("简化版分析测试（单线程）")
print("=" * 70)
print(f"\n目标目录: {test_dir}")

# 初始化客户端
print("\n初始化GitNexusClient...")
client = GitNexusClient("config/config.yaml")
client.root_dir = test_dir.resolve()
print("客户端初始化成功")

# 获取所有Java文件
print("\n扫描Java文件...")
java_files = list(test_dir.rglob("*.java"))
print(f"发现 {len(java_files)} 个Java文件")

# 限制测试文件数量，避免运行太久
max_files = 20  # 只测试前20个文件
java_files = java_files[:max_files]
print(f"（限制为前 {max_files} 个文件进行快速测试）")

# 分批处理，每批5个文件
batch_size = 5
all_results = []
start_time = time.time()

for batch_idx in range(0, len(java_files), batch_size):
    batch = java_files[batch_idx:batch_idx + batch_size]
    batch_num = batch_idx // batch_size + 1
    total_batches = (len(java_files) + batch_size - 1) // batch_size
    
    print(f"\n批次 {batch_num}/{total_batches}")
    print("-" * 70)
    
    batch_results = []
    for i, file_path in enumerate(batch, 1):
        try:
            file_start = time.time()
            print(f"  [{i}/{len(batch)}] {file_path.name:<40}", end="", flush=True)
            
            result = client._analyze_single_file(file_path, force=True, root_dir=test_dir)
            
            file_time = time.time() - file_start
            if result:
                batch_results.append(result)
                print(f" OK {len(result.entities):3d} entities ({file_time:.2f}s)")
            else:
                print(f" -- skip ({file_time:.2f}s)")
        except KeyboardInterrupt:
            print("\n\nUser interrupt")
            sys.exit(1)
        except Exception as e:
            print(f" ERR: {str(e)[:50]}")
    
    all_results.extend(batch_results)
    elapsed = time.time() - start_time
    print(f"\n  累计: {len(all_results)} 个文件, {elapsed:.1f}秒")

# 构建知识图谱
print("\n" + "=" * 70)
print("构建知识图谱...")
graph = KnowledgeGraph.build_from_results(all_results)

total_time = time.time() - start_time
print(f"\n分析完成！总耗时: {total_time:.1f}秒")
print(f"\n统计信息:")
print(f"  处理文件: {len(all_results)}/{max_files}")
print(f"  实体数量: {len(graph.entities)}")
print(f"  关系数量: {len(graph.relationships)}")
print(f"  平均速度: {len(all_results)/total_time:.2f} 文件/秒")

if graph.entities:
    print("\n实体类型分布:")
    type_counts = {}
    for entity in graph.entities.values():
        entity_type = entity.entity_type.value
        type_counts[entity_type] = type_counts.get(entity_type, 0) + 1
    
    for entity_type, count in sorted(type_counts.items()):
        print(f"  {entity_type}: {count}")

# 保存结果
output_file = Path("test_simple_output.json")
print(f"\n保存结果到: {output_file}")
with open(output_file, "w", encoding="utf-8") as f:
    json.dump(graph.model_dump(), f, ensure_ascii=False, indent=2)

print("\n" + "=" * 70)
print("[OK] Test completed successfully")
print("=" * 70)
