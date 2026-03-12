#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""重新生成UML文档"""

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

# 重新生成UML文档
print("\nRegenerating UML documents...")
doc_generator = KnowledgeDocumentGenerator(output_dir="output/doc")
uml_files = doc_generator._generate_uml_documents(graph)

print("\nGenerated UML documents:")
for file_path in uml_files:
    print(f"  - {file_path}")

print("\nDone!")
