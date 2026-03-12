#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""诊断 tree-sitter API 版本和查询方法"""

try:
    from tree_sitter import Language, Parser, QueryCursor
    print("[OK] 使用新版 tree-sitter API (带 QueryCursor)")
    TREE_SITTER_VERSION = "new"
except ImportError as e:
    print(f"[FAIL] 导入新版 API 失败: {e}")
    try:
        from tree_sitter import Language, Parser
        print("[OK] 使用旧版 tree-sitter API")
        TREE_SITTER_VERSION = "old"
    except ImportError as e2:
        print(f"[FAIL] 导入旧版 API 也失败: {e2}")
        exit(1)

# 测试 Java 语言支持
try:
    import tree_sitter_java
    print("[OK] tree_sitter_java 模块已加载")
    
    # 获取语言对象
    if hasattr(tree_sitter_java, 'language'):
        java_language = Language(tree_sitter_java.language())
        print(f"[OK] Java 语言对象创建成功: {java_language}")
    elif hasattr(tree_sitter_java, 'Language'):
        java_language = tree_sitter_java.Language()
        print(f"[OK] Java 语言对象创建成功 (旧方式): {java_language}")
    else:
        print("[FAIL] 无法从 tree_sitter_java 获取语言对象")
        print(f"  可用属性: {dir(tree_sitter_java)}")
        exit(1)
    
    # 创建解析器
    parser = Parser(java_language)
    print("[OK] Java 解析器创建成功")
    
    # 测试代码
    test_code = """
    package com.example;
    
    import java.util.List;
    
    public class TestClass {
        private String name;
        
        public void testMethod() {
            System.out.println("Hello");
        }
    }
    """
    
    # 解析代码
    tree = parser.parse(bytes(test_code, "utf8"))
    root_node = tree.root_node
    print(f"[OK] 代码解析成功，根节点类型: {root_node.type}")
    
    # 测试查询
    query_patterns = """
        (import_declaration) @import
        (class_declaration name: (identifier) @class_name) @class
        (method_declaration name: (identifier) @method_name) @method
        (field_declaration) @field
    """
    
    query = java_language.query(query_patterns)
    print(f"[OK] 查询对象创建成功: {query}")
    print(f"  查询对象类型: {type(query)}")
    print(f"  查询对象属性: {dir(query)}")
    
    # 测试不同的查询方法
    if TREE_SITTER_VERSION == "new":
        print("\n=== 测试新版 API ===")
        
        # 方法1: 使用 QueryCursor
        try:
            cursor = QueryCursor(query)
            print(f"[OK] QueryCursor 创建成功: {type(cursor)}")
            print(f"  QueryCursor 属性: {dir(cursor)}")
            
            # 测试 captures 方法
            if hasattr(cursor, 'captures'):
                print("  [OK] QueryCursor 有 captures 方法")
                try:
                    captures_dict = cursor.captures(root_node)
                    print(f"  [OK] captures() 调用成功")
                    print(f"    返回类型: {type(captures_dict)}")
                    print(f"    捕获数量: {len(captures_dict)}")
                    
                    if isinstance(captures_dict, dict):
                        for capture_name, nodes in captures_dict.items():
                            print(f"    - {capture_name}: {len(nodes)} 个节点")
                            if nodes:
                                print(f"      第一个节点: {nodes[0].type}")
                    else:
                        print(f"    captures 返回值: {captures_dict}")
                        
                except Exception as e:
                    print(f"  [FAIL] captures() 调用失败: {e}")
                    import traceback
                    traceback.print_exc()
            else:
                print("  [FAIL] QueryCursor 没有 captures 方法")
            
            # 测试 matches 方法
            if hasattr(cursor, 'matches'):
                print("  [OK] QueryCursor 有 matches 方法")
                try:
                    matches = cursor.matches(root_node)
                    print(f"  [OK] matches() 调用成功")
                    print(f"    返回类型: {type(matches)}")
                    print(f"    匹配数量: {len(matches)}")
                    
                    if matches:
                        for i, match in enumerate(matches[:3]):
                            print(f"    匹配 {i}: {match}")
                except Exception as e:
                    print(f"  [FAIL] matches() 调用失败: {e}")
                    import traceback
                    traceback.print_exc()
            else:
                print("  [FAIL] QueryCursor 没有 matches 方法")
                
        except Exception as e:
            print(f"[FAIL] QueryCursor 创建失败: {e}")
            import traceback
            traceback.print_exc()
        
        # 方法2: 直接在 query 对象上调用
        print("\n=== 测试 Query 对象的直接方法 ===")
        if hasattr(query, 'captures'):
            print("  [OK] Query 有 captures 方法")
            try:
                # 旧版API: query.captures(root_node)
                captures = query.captures(root_node)
                print(f"  [OK] query.captures() 调用成功")
                print(f"    返回类型: {type(captures)}")
                print(f"    捕获数量: {len(captures)}")
            except Exception as e:
                print(f"  [FAIL] query.captures() 调用失败: {e}")
        else:
            print("  [FAIL] Query 没有 captures 方法")
            
        if hasattr(query, 'matches'):
            print("  [OK] Query 有 matches 方法")
            try:
                matches = query.matches(root_node)
                print(f"  [OK] query.matches() 调用成功")
                print(f"    返回类型: {type(matches)}")
                print(f"    匹配数量: {len(matches)}")
            except Exception as e:
                print(f"  [FAIL] query.matches() 调用失败: {e}")
        else:
            print("  [FAIL] Query 没有 matches 方法")
    
except Exception as e:
    print(f"[FAIL] 测试失败: {e}")
    import traceback
    traceback.print_exc()
