#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
运行 stock_datacenter 完整知识提取

前置条件:
1. 安装所有依赖: pip install -r requirements.txt
2. 设置API密钥: set ARK_API_KEY=your_key

输出位置:
- output/knowledge_graph_stock_datacenter/  知识图谱（JSON）
- output/doc/stock_datacenter/              分析文档（Markdown）
"""

import sys
from pathlib import Path

project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

print("=" * 80)
print("Stock Datacenter 知识提取说明")
print("=" * 80)
print()

print("📋 前置条件:")
print("  1. 安装依赖包:")
print("     pip install pydantic pandas networkx sqlalchemy")
print("     pip install tree-sitter tree-sitter-java")
print()
print("  2. 设置API密钥（用于LLM深度分析）:")
print("     set ARK_API_KEY=your_api_key")
print()
print("  3. 或者使用简化模式（不依赖LLM）:")
print("     修改run_stock_datacenter.py中的 deep_analysis=False")
print()

print("=" * 80)
print("📂 输出位置说明")
print("=" * 80)
print()

print("1. 知识图谱（JSON格式）:")
print("   📄 output/knowledge_graph_stock_datacenter/")
print("      - entities.json       # 所有代码实体")
print("      - relationships.json  # 实体间关系")
print("      - metadata.json       # 元数据信息")
print()

print("2. 分析文档（Markdown格式）:")
print("   📄 output/doc/stock_datacenter/")
print("      - index.md           # 知识索引")
print("      - entities/          # 实体文档")
print("      - modules/           # 模块文档")
print("      - architecture/      # 架构文档")
print("      - uml/               # UML图表")
print()

print("3. 知识提取包含:")
print("   ✅ 代码实体识别（类、方法、字段）")
print("   ✅ 依赖关系分析")
print("   ✅ 调用链路追踪")
print("   ✅ 架构层次分析")
print("   ✅ 模块划分识别")
print("   ✅ UML图生成")
print()

print("=" * 80)
print("📖 查看已有知识")
print("=" * 80)
print()

print("1. 文档输出:")
print("   📂 output/doc/")
print("      - 368个Markdown文件")
print("      - 包含实体、模块、架构文档")
print()

print("2. 知识图谱:")
print("   📄 test_output_graph.json (14.08 MB)")
print("      - 完整的实体和关系数据")
print("      - 从StockCenter等项目提取")
print()

print("3. 查看方式:")
print("   a) Markdown文档: 使用任何Markdown阅读器")
print("   b) JSON图谱: 使用JSON查看器或文本编辑器")
print("   c) Web界面: 运行 python start_web.py")
print()

print("=" * 80)
print("🚀 开始提取")
print("=" * 80)
print()

print("完整提取命令:")
print("  python run_stock_datacenter.py")
print()
print("简化提取（不使用LLM）:")
print("  1. 修改 run_stock_datacenter.py 第91行:")
print("     deep_analysis=False  # 改为False")
print("  2. 运行: python run_stock_datacenter.py")
print()

print("=" * 80)
