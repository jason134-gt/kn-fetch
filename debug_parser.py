#!/usr/bin/env python3
"""
诊断代码解析器问题
"""

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from gitnexus.parser_v2 import EnhancedCodeParser as CodeParser
from gitnexus.models import AnalysisResult, CodeEntity

# 测试简单的Python文件
test_python_code = '''
class TestClass:
    """测试类"""
    
    def __init__(self):
        self.value = "test"
    
    def test_method(self, param):
        """测试方法"""
        return f"Hello {param}"

def test_function():
    """测试函数"""
    return "Hello World"
'''

def test_parser():
    """测试解析器功能"""
    parser = CodeParser()
    
    # 测试解析
    try:
        print("=== 测试解析器 ===")
        entities, relationships = parser.parse(test_python_code, "test.py")
        print(f"解析成功: 实体数量={len(entities)}, 关系数量={len(relationships)}")
        
        for entity in entities:
            print(f"  - {entity.entity_type}: {entity.name}")
            
    except Exception as e:
        print(f"解析失败: {e}")
        import traceback
        traceback.print_exc()

def test_analysis_result():
    """测试AnalysisResult模型"""
    try:
        print("\n=== 测试AnalysisResult模型 ===")
        
        # 测试正确的AnalysisResult创建
        entity = CodeEntity(
            id="test_id",
            entity_type="class",
            name="TestClass",
            file_path="test.py",
            start_line=1,
            end_line=10
        )
        
        result = AnalysisResult(
            file_path="test.py",
            file_hash="test_hash",
            language="python",
            entities=[entity],
            relationships=[]
        )
        
        print("模型创建成功")
        
    except Exception as e:
        print(f"模型创建失败: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_parser()
    test_analysis_result()