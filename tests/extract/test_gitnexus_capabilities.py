#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查GitNexus系统的完整能力
"""

import sys
from pathlib import Path

project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

print("=" * 80)
print("GitNexus系统能力检查")
print("=" * 80)
print()

# 检查核心模块
modules_status = {}

print("1. 核心模块检查:")
print("-" * 80)

try:
    from src.gitnexus import GitNexusClient, KnowledgeGraph
    modules_status['GitNexusClient'] = 'OK'
    print("  [OK] GitNexusClient - 代码分析核心")
except Exception as e:
    modules_status['GitNexusClient'] = f'FAIL: {e}'
    print(f"  [FAIL] GitNexusClient: {e}")

try:
    from src.core.knowledge_extractor import KnowledgeExtractor
    modules_status['KnowledgeExtractor'] = 'OK'
    print("  [OK] KnowledgeExtractor - 知识提取引擎")
except Exception as e:
    modules_status['KnowledgeExtractor'] = f'FAIL: {e}'
    print(f"  [FAIL] KnowledgeExtractor: {e}")

try:
    from src.core.knowledge_document_generator import KnowledgeDocumentGenerator
    modules_status['KnowledgeDocumentGenerator'] = 'OK'
    print("  [OK] KnowledgeDocumentGenerator - 知识文档生成器")
except Exception as e:
    modules_status['KnowledgeDocumentGenerator'] = f'FAIL: {e}'
    print(f"  [FAIL] KnowledgeDocumentGenerator: {e}")

try:
    from src.ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer
    modules_status['DeepKnowledgeAnalyzer'] = 'OK'
    print("  [OK] DeepKnowledgeAnalyzer - LLM深度分析器")
except Exception as e:
    modules_status['DeepKnowledgeAnalyzer'] = f'FAIL: {e}'
    print(f"  [FAIL] DeepKnowledgeAnalyzer: {e}")

print()
print("=" * 80)
print("2. GitNexus系统能力:")
print("=" * 80)

if modules_status.get('GitNexusClient') == 'OK':
    print()
    print("  代码分析能力:")
    print("    - 代码实体识别（类、方法、字段）")
    print("    - 代码关系分析（继承、依赖、调用）")
    print("    - 复杂度分析")
    print("    - 代码模式识别")
    
if modules_status.get('KnowledgeDocumentGenerator') == 'OK':
    print()
    print("  文档生成能力:")
    print("    - 实体文档 (entities/)")
    print("    - 模块文档 (modules/)")
    print("    - 架构文档 (architecture/)")
    print("    - UML文档 (uml/)")
    print("    - API文档 (api/)")
    print("    - 消息流文档 (message_flows/)")

if modules_status.get('DeepKnowledgeAnalyzer') == 'OK':
    print()
    print("  LLM深度分析能力:")
    print("    - 架构分析文档")
    print("    - 设计模式分析")
    print("    - 业务流程分析")
    print("    - 技术债务识别")

print()
print("=" * 80)
print("3. 我刚才的错误:")
print("=" * 80)

print()
print("  我创建的 extract_stock_knowledge.py 脚本:")
print("    - 只做了简单的文本解析")
print("    - 没有使用GitNexus的代码分析能力")
print("    - 没有生成架构文档")
print("    - 没有生成UML图")
print("    - 没有LLM深度分析")
print()
print("  这导致:")
print("    - 丢失了代码关系分析")
print("    - 丢失了架构文档")
print("    - 丢失了设计文档")
print("    - 丢失了UML图")
print()

print("=" * 80)
print("4. 解决方案:")
print("=" * 80)

print()
if all(modules_status.get(k) == 'OK' for k in ['GitNexusClient', 'KnowledgeExtractor']):
    print("  [方案1] 运行完整的GitNexus系统:")
    print("    python run_stock_datacenter.py")
    print()
    print("  这将生成:")
    print("    - 完整的知识图谱（包含代码关系）")
    print("    - 架构分析文档")
    print("    - UML类图")
    print("    - 设计模式分析")
    print("    - API文档")
    print()
    print("  注意:")
    print("    - 需要安装依赖: pip install -r requirements.txt")
    print("    - LLM深度分析需要API密钥")
    print("    - 可以关闭LLM分析: deep_analysis=False")
else:
    print("  [问题] 缺少核心模块依赖")
    print()
    print("  需要安装:")
    print("    pip install pydantic pandas networkx sqlalchemy")
    print("    pip install tree-sitter tree-sitter-java")

print()
print("=" * 80)
