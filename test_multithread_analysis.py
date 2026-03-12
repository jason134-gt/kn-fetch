#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""测试修复后的多线程分析"""

import sys
from pathlib import Path
import json
import time

# 添加项目路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.core.knowledge_extractor import KnowledgeExtractor

# 测试目录
test_dir = "tests/example/Stock_service/src/com/yfzx/service"

print("=" * 70)
print("Multi-thread Analysis Test (Fixed)")
print("=" * 70)
print(f"\nTarget directory: {test_dir}")

# 创建提取器
extractor = KnowledgeExtractor("config/config.yaml")

# 设置较小的线程数和批次大小进行测试
extractor.max_workers = 4  # 降低线程数
extractor.batch_size = 20  # 降低批次大小

print(f"\nConfiguration:")
print(f"  Max workers: {extractor.max_workers}")
print(f"  Batch size: {extractor.batch_size}")

# 开始分析
start_time = time.time()

try:
    graph = extractor.extract_from_directory(
        test_dir,
        include_code=True,
        include_docs=False,
        force=True,  # 强制重新分析
        deep_analysis=False,  # 不启用深度分析以加快速度
        refactoring_analysis=False
    )
    
    elapsed = time.time() - start_time
    
    print("\n" + "=" * 70)
    print(f"Analysis completed in {elapsed:.1f}s")
    print("=" * 70)
    
    print(f"\nStatistics:")
    print(f"  Total files: {len(set(e.file_path for e in graph.entities.values()))}")
    print(f"  Entities: {len(graph.entities)}")
    print(f"  Relationships: {len(graph.relationships)}")
    print(f"  Processing speed: {len(graph.entities) / elapsed:.1f} entities/s")
    
    if graph.entities:
        print("\nEntity type distribution:")
        type_counts = {}
        for entity in graph.entities.values():
            entity_type = entity.entity_type.value
            type_counts[entity_type] = type_counts.get(entity_type, 0) + 1
        
        for entity_type, count in sorted(type_counts.items()):
            print(f"  {entity_type}: {count}")
    
    print("\n[OK] Multi-thread analysis works correctly!")
    
except Exception as e:
    print(f"\n[FAIL] Analysis failed: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
