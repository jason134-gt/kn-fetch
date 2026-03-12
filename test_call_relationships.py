#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""测试Java调用关系提取"""

import sys
from pathlib import Path

# 添加项目路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.gitnexus.parser_v2 import EnhancedCodeParser

# 测试代码
test_code = """
package com.example;

import java.util.List;

public class TestClass {
    private String name;
    
    public void methodA() {
        methodB();
        System.out.println("test");
    }
    
    public void methodB() {
        methodC();
    }
    
    private void methodC() {
        // do something
    }
}
"""

print("Testing Java call relationship extraction")
print("=" * 70)

parser = EnhancedCodeParser()

# 解析代码
entities, relationships = parser.parse(test_code, "TestClass.java")

print(f"\nExtracted entities: {len(entities)}")
for i, entity in enumerate(entities, 1):
    print(f"  {i}. {entity.entity_type.value}: {entity.name} (line {entity.start_line})")

print(f"\nExtracted relationships: {len(relationships)}")
for i, rel in enumerate(relationships, 1):
    print(f"  {i}. {rel.relationship_type.value}: {rel.metadata.get('caller_name', 'N/A')} -> {rel.metadata.get('callee_name', 'N/A')}")
    print(f"     Call site: {rel.call_site}")

print("\n" + "=" * 70)
if relationships:
    print("[OK] Call relationships extracted successfully!")
else:
    print("[WARNING] No call relationships found")
