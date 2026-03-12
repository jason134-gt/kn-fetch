import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from gitnexus.parser_v2 import EnhancedCodeParser

# 创建解析器
parser = EnhancedCodeParser()

print("=== Java解析器状态检查 ===")
print(f"支持的编程语言: {list(parser.languages.keys())}")

# 检查Java语言是否已加载
if "java" in parser.languages:
    print("Java语言已加载")
    java_language = parser.languages["java"]
    print(f"Java语言对象: {java_language}")
    print(f"Java解析器对象: {parser.parsers.get('java')}")
else:
    print("Java语言未加载")

# 测试Java代码解析
test_java_code = """
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
}
"""

print("\n=== 测试Java代码解析 ===")
print(f"检测到的语言: {parser.detect_language(test_java_code, 'Test.java')}")

# 尝试解析Java代码
entities, relationships = parser.parse(test_java_code, "Test.java")

print(f"解析结果: {len(entities)} 个实体, {len(relationships)} 个关系")

for entity in entities:
    print(f"  - {entity.entity_type}: {entity.name}")

# 检查文件扩展名映射
print("\n=== 文件扩展名映射 ===")
for ext, lang in parser.extension_map.items():
    print(f"{ext} -> {lang}")