#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""测试实际Java文件解析的完整流程"""

import sys
from pathlib import Path

# 添加项目路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.gitnexus.parser_v2 import EnhancedCodeParser

# 选择一个Java文件进行测试
java_file = Path("tests/example/Stock_service/src/com/yfzx/service/StockCenter.java")

if not java_file.exists():
    print(f"[FAIL] 文件不存在: {java_file}")
    sys.exit(1)

print(f"[OK] 测试文件: {java_file}")

# 读取文件内容
with open(java_file, "r", encoding="utf-8") as f:
    content = f.read()

print(f"[OK] 文件大小: {len(content)} 字节")
print(f"[OK] 文件行数: {len(content.splitlines())}")

# 创建解析器
parser = EnhancedCodeParser()
print(f"[OK] 解析器创建成功")
print(f"  支持的语言: {list(parser.parsers.keys())}")

# 解析文件
print("\n开始解析...")
try:
    entities, relationships = parser.parse(content, str(java_file))
    print(f"[OK] 解析完成")
    print(f"  实体数量: {len(entities)}")
    print(f"  关系数量: {len(relationships)}")
    
    if entities:
        print("\n实体列表:")
        for i, entity in enumerate(entities[:10], 1):
            print(f"  {i}. {entity.entity_type.value}: {entity.name} (行 {entity.start_line}-{entity.end_line})")
            if i == 10 and len(entities) > 10:
                print(f"  ... 还有 {len(entities) - 10} 个实体")
                break
    else:
        print("\n[WARNING] 没有提取到任何实体!")
        
        # 尝试手动调试
        print("\n手动调试tree-sitter查询:")
        import tree_sitter_java
        from tree_sitter import Language, Parser, QueryCursor
        
        java_language = Language(tree_sitter_java.language())
        parser_obj = Parser(java_language)
        tree = parser_obj.parse(bytes(content, "utf8"))
        root_node = tree.root_node
        
        print(f"  根节点类型: {root_node.type}")
        print(f"  子节点数量: {len(root_node.children)}")
        
        # 显示前几个子节点
        for i, child in enumerate(root_node.children[:5]):
            print(f"    子节点 {i}: {child.type}")
        
        # 执行查询
        query_patterns = """
            (import_declaration) @import
            (class_declaration name: (identifier) @class_name) @class
            (interface_declaration name: (identifier) @interface_name) @interface
            (method_declaration name: (identifier) @method_name) @method
            (enum_declaration name: (identifier) @enum_name) @enum
            (field_declaration) @field
        """
        
        query = java_language.query(query_patterns)
        cursor = QueryCursor(query)
        captures_dict = cursor.captures(root_node)
        
        print(f"  捕获结果:")
        for capture_name, nodes in captures_dict.items():
            print(f"    {capture_name}: {len(nodes)} 个节点")
        
except Exception as e:
    print(f"[FAIL] 解析失败: {e}")
    import traceback
    traceback.print_exc()
