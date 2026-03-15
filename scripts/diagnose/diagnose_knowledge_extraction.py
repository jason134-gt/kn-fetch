import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from src.core.knowledge_extractor import KnowledgeExtractor

# 创建知识提取器
extractor = KnowledgeExtractor("config/config.yaml")

# 测试提取Stock_service
print("=== 测试知识提取流程 ===")
print("开始提取知识...")

try:
    graph = extractor.extract_from_directory(
        "tests/example/Stock_service",
        include_code=True,
        include_docs=False,
        force=True
    )

    print(f"\n提取完成!")
    print(f"实体数量: {len(graph.entities)}")
    print(f"关系数量: {len(graph.relationships)}")
    
    if graph.entities:
        print("\n前10个实体:")
        for i, (entity_id, entity) in enumerate(list(graph.entities.items())[:10]):
            print(f"  {i+1}. {entity.entity_type}: {entity.name}")
    else:
        print("\n警告: 没有提取到任何实体!")
        
except Exception as e:
    print(f"错误: {e}")
    import traceback
    traceback.print_exc()