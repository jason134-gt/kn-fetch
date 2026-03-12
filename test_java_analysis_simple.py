import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from gitnexus.parser_v2 import EnhancedCodeParser
from pathlib import Path

# 创建Java解析器
parser = EnhancedCodeParser()

# 测试Java文件路径
java_file_path = Path("tests/example/Stock_service/src/com/yfzx/service/StockCenter.java")

print("=== 测试Java文件解析 ===")
print(f"Java文件路径: {java_file_path.absolute()}")
print(f"文件存在: {java_file_path.exists()}")

if java_file_path.exists():
    # 读取Java文件内容
    with open(java_file_path, "r", encoding="utf-8") as f:
        java_content = f.read()
    
    print(f"文件大小: {len(java_content)} 字符")
    print(f"前200字符: {java_content[:200]}")
    
    # 检测语言
    detected_language = parser.detect_language(java_content, str(java_file_path))
    print(f"检测到的语言: {detected_language}")
    
    # 解析Java文件
    entities, relationships = parser.parse(java_content, str(java_file_path))
    
    print(f"解析结果: {len(entities)} 个实体, {len(relationships)} 个关系")
    
    if entities:
        print("实体列表:")
        for entity in entities[:10]:  # 显示前10个实体
            print(f"  - {entity.entity_type}: {entity.name}")
            print(f"    文件: {entity.file_path}")
            print(f"    行号: {entity.start_line}-{entity.end_line}")
    
    # 测试多个Java文件
    print("\n=== 测试多个Java文件 ===")
    java_files = list(Path("tests/example/Stock_service").rglob("*.java"))
    print(f"总Java文件数量: {len(java_files)}")
    
    # 测试前5个文件
    for i, file_path in enumerate(java_files[:5]):
        print(f"\n文件 {i+1}: {file_path}")
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            entities, relationships = parser.parse(content, str(file_path))
            print(f"  解析结果: {len(entities)} 个实体")
            
            if entities:
                entity_types = {}
                for entity in entities:
                    entity_type = str(entity.entity_type)
                    entity_types[entity_type] = entity_types.get(entity_type, 0) + 1
                
                print(f"  实体类型分布: {entity_types}")
        except Exception as e:
            print(f"  解析失败: {e}")