import tree_sitter
import tree_sitter_java

# 创建Java语言和解析器
java_language = tree_sitter.Language(tree_sitter_java.language())
parser = tree_sitter.Parser()
parser.language = java_language

# 测试Java代码
test_java_code = b"""
package com.example;

import java.util.List;

public class StockService {
    private String stockCode;
    
    public StockService(String code) {
        this.stockCode = code;
    }
    
    public double getCurrentPrice() {
        return 100.0;
    }
    
    public List<String> getHistoryPrices() {
        return new ArrayList<>();
    }
}
"""

# 解析Java代码
tree = parser.parse(test_java_code)
root_node = tree.root_node

print("Java代码解析成功:")
print(f"根节点类型: {root_node.type}")
print(f"根节点子节点数量: {root_node.child_count}")

# 创建Java查询
query_pattern = b"""
(import_declaration) @import
(class_declaration name: (identifier) @class_name) @class
(interface_declaration name: (identifier) @interface_name) @interface
(method_declaration name: (identifier) @method_name) @method
(enum_declaration name: (identifier) @enum_name) @enum
(field_declaration) @field
"""

query = tree_sitter.Query(java_language, query_pattern)

print("\nJava查询对象的方法:")
for method in dir(query):
    if not method.startswith('_'):
        print(f"  {method}")

print("\n尝试执行Java查询...")

# 使用QueryCursor执行查询
cursor = tree_sitter.QueryCursor(query)

# 尝试使用captures方法
if hasattr(cursor, 'captures'):
    captures_dict = cursor.captures(root_node)
    print("使用captures方法:")
    print(f"captures类型: {type(captures_dict)}")
    
    # 处理字典格式的捕获结果
    for capture_name, nodes in captures_dict.items():
        print(f"捕获类型: {capture_name}")
        for node in nodes:
            print(f"  - 节点: {node.text.decode('utf-8')}")
                
    print(f"总共捕获到 {sum(len(nodes) for nodes in captures_dict.values())} 个Java节点")
# 尝试使用matches方法
elif hasattr(cursor, 'matches'):
    matches = cursor.matches(root_node)
    print("使用matches方法:")
    for match in matches:
        for capture in match[1]:  # match[1]包含捕获列表
            node = capture[0]
            capture_name = capture[1]
            print(f"捕获: {capture_name} - {node.text.decode('utf-8')}")
else:
    print("QueryCursor没有可用的查询方法")

print(f"总共捕获到 {len(captures)} 个Java节点")

# 检查Java语言支持
print("\n检查Java语言支持:")
print(f"Java语言对象: {java_language}")
print(f"Java语言版本: {java_language.version}")

# 检查解析器状态
print(f"解析器语言: {parser.language}")
print(f"解析树根节点: {tree.root_node}")