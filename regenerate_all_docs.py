#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""重新生成所有文档，包括API和消息流文档"""

import json
import sys
from pathlib import Path

# 添加项目路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.gitnexus.models import KnowledgeGraph
from src.core.knowledge_document_generator import KnowledgeDocumentGenerator

# 读取知识图谱
print("Reading knowledge graph...")
with open("output/knowledge_graph", "r", encoding="utf-8") as f:
    data = json.load(f)

# 重建KnowledgeGraph对象
print("Rebuilding knowledge graph object...")
graph = KnowledgeGraph(**data)

print(f"Entities: {len(graph.entities)}")
print(f"Relationships: {len(graph.relationships)}")

# 重新生成所有文档
print("\nRegenerating all documents...")
doc_generator = KnowledgeDocumentGenerator(output_dir="output/doc")
all_docs = doc_generator.generate_all_documents(graph)

print("\nGenerated documents:")
for doc_type, files in all_docs.items():
    if isinstance(files, list):
        print(f"\n{doc_type.upper()}:")
        for file_path in files[:10]:  # 只显示前10个
            print(f"  - {file_path}")
        if len(files) > 10:
            print(f"  ... and {len(files) - 10} more files")
    else:
        print(f"\n{doc_type.upper()}: {files}")

print("\nDone!")
