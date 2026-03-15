import tree_sitter
import tree_sitter_python

# 创建语言和解析器
python_language = tree_sitter.Language(tree_sitter_python.language())
parser = tree_sitter.Parser()
parser.language = python_language

# 测试代码
test_code = b"""
def test_function():
    pass

class TestClass:
    def method(self):
        pass
"""

# 解析代码
tree = parser.parse(test_code)
root_node = tree.root_node

# 创建查询
query_pattern = b"""
(function_definition name: (identifier) @function)
(class_definition name: (identifier) @class)
(method_definition name: (identifier) @method)
"""

query = tree_sitter.Query(python_language, query_pattern)

print("Query对象的方法:")
for method in dir(query):
    if not method.startswith('_'):
        print(f"  {method}")

print("\n尝试使用QueryCursor执行查询...")

# 使用QueryCursor来执行查询
cursor = tree_sitter.QueryCursor()
cursor.execute(query, root_node)

captures = []
for match in cursor:
    for capture in match[1]:  # match[1]包含捕获列表
        node = capture[0]
        capture_name = capture[1]
        captures.append((node, capture_name))
        print(f"捕获: {capture_name} - {node.text.decode('utf-8')}")

print(f"总共捕获到 {len(captures)} 个节点")

# 检查是否有其他可用的查询方法
print("\n检查其他可能的查询方法...")
if hasattr(query, 'captures'):
    print("captures方法存在，尝试使用...")
    try:
        captures = query.captures(root_node)
        print(f"captures方法结果: {len(captures)} 个捕获")
    except Exception as e:
        print(f"captures方法失败: {e}")