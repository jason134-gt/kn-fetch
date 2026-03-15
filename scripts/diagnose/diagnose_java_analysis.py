import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from gitnexus.gitnexus_client import GitNexusClient
from pathlib import Path

# 创建GitNexusClient实例
print("=== 诊断Java分析流程 ===")

# 检查Stock_service目录结构
stock_service_path = Path("tests/example/Stock_service")
print(f"目标目录: {stock_service_path.absolute()}")
print(f"目录存在: {stock_service_path.exists()}")

# 统计Java文件数量
java_files = list(stock_service_path.rglob("*.java"))
print(f"Java文件数量: {len(java_files)}")

if java_files:
    print("前5个Java文件:")
    for file in java_files[:5]:
        print(f"  - {file}")

# 创建GitNexusClient实例
try:
    client = GitNexusClient(str(stock_service_path))
    print("GitNexusClient创建成功")
    
    # 获取所有目标文件
    all_files = client._get_all_target_files()
    print(f"发现的目标文件数量: {len(all_files)}")
    
    if all_files:
        print("前5个目标文件:")
        for file in all_files[:5]:
            print(f"  - {file}")
        
        # 测试分析单个Java文件
        test_file = all_files[0]
        print(f"\n=== 测试分析文件: {test_file} ===")
        
        result = client._analyze_single_file(test_file)
        if result:
            print(f"分析结果:")
            print(f"  文件路径: {result.file_path}")
            print(f"  语言: {result.language}")
            print(f"  实体数量: {len(result.entities)}")
            print(f"  关系数量: {len(result.relationships)}")
            print(f"  代码行数: {result.lines_of_code}")
            
            if result.entities:
                print("  实体列表:")
                for entity in result.entities[:3]:  # 显示前3个实体
                    print(f"    - {entity.entity_type}: {entity.name}")
        else:
            print("❌ 文件分析失败")
    
    # 测试全量分析
    print("\n=== 测试全量分析 ===")
    graph = client.analyze_full()
    
    print(f"知识图谱统计:")
    print(f"  实体总数: {len(graph.entities)}")
    print(f"  关系总数: {len(graph.relationships)}")
    print(f"  模块总数: {len(graph.modules)}")
    
except Exception as e:
    print(f"❌ 错误: {e}")
    import traceback
    traceback.print_exc()