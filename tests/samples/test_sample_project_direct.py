#!/usr/bin/env python3
"""
直接测试样例工程 - 使用现有CLI功能
"""
import os
import sys
import subprocess
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 设置API密钥
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

def test_cli_analyze():
    """测试CLI分析功能"""
    print("=== 测试CLI分析功能 ===")
    
    sample_project_path = "output/example/stock-sentiment-analyzer"
    
    if not Path(sample_project_path).exists():
        print(f"样例项目不存在: {sample_project_path}")
        return False
    
    try:
        # 使用subprocess运行CLI命令
        cmd = [
            sys.executable, "kn-fetch.py", 
            "analyze", sample_project_path,
            "--config", "config/config.yaml",
            "--output", "test_output_direct"
        ]
        
        print(f"执行命令: {' '.join(cmd)}")
        
        # 运行命令（设置超时避免卡死）
        result = subprocess.run(
            cmd, 
            capture_output=True, 
            text=True, 
            timeout=300,  # 5分钟超时
            cwd=Path(__file__).parent
        )
        
        print(f"返回码: {result.returncode}")
        print(f"标准输出: {result.stdout[-500:] if result.stdout else '无输出'}")  # 显示最后500字符
        if result.stderr:
            print(f"标准错误: {result.stderr[-500:]}")  # 显示最后500字符
        
        if result.returncode == 0:
            print("CLI分析成功")
            
            # 检查输出文件
            output_dir = Path("test_output_direct")
            if output_dir.exists():
                print("输出文件检查:")
                md_files = list(output_dir.rglob("*.md"))
                for md_file in md_files[:10]:  # 显示前10个文件
                    relative_path = md_file.relative_to(output_dir)
                    print(f"   - {relative_path}")
                
                if len(md_files) > 10:
                    print(f"   ... 还有 {len(md_files) - 10} 个文件")
            
            return True
        else:
            print("CLI分析失败")
            return False
            
    except subprocess.TimeoutExpired:
        print("CLI分析超时")
        return False
    except Exception as e:
        print(f"CLI分析失败: {e}")
        return False

def test_simple_analysis():
    """测试简化分析功能"""
    print("\n=== 测试简化分析功能 ===")
    
    sample_project_path = "output/example/stock-sentiment-analyzer"
    
    if not Path(sample_project_path).exists():
        print(f"样例项目不存在: {sample_project_path}")
        return False
    
    try:
        # 直接导入并使用现有的分析功能
        from src.ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer
        import yaml
        
        # 加载配置
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        # 创建分析器
        analyzer = DeepKnowledgeAnalyzer(config, "test_output_simple")
        
        if not analyzer.is_available():
            print("深度知识分析器不可用")
            return False
        
        print("深度知识分析器可用")
        
        # 分析样例项目中的一个文件
        test_file = Path(sample_project_path) / "gitnexus.py"
        
        if not test_file.exists():
            print(f"测试文件不存在: {test_file}")
            return False
        
        # 读取文件内容
        with open(test_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        print(f"分析文件: {test_file}")
        print(f"文件大小: {len(content)} 字符")
        
        # 分析代码实体
        entities = analyzer.analyze_code_entity(content, str(test_file))
        
        if entities and isinstance(entities, list):
            print(f"代码分析成功: 提取到 {len(entities)} 个实体")
            
            # 显示实体统计
            entity_types = {}
            for entity in entities:
                if isinstance(entity, dict):
                    entity_type = entity.get('type', 'unknown')
                    entity_types[entity_type] = entity_types.get(entity_type, 0) + 1
            
            for entity_type, count in entity_types.items():
                print(f"   {entity_type}: {count}")
            
            # 显示前5个实体
            for i, entity in enumerate(entities[:5]):
                if isinstance(entity, dict):
                    name = entity.get('name', 'Unknown')
                    entity_type = entity.get('type', 'Unknown')
                    print(f"   {i+1}. {name} ({entity_type})")
            
            return True
        else:
            print("代码分析失败: 未提取到有效实体")
            return False
            
    except Exception as e:
        print(f"简化分析失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_project_structure():
    """测试项目结构分析"""
    print("\n=== 测试项目结构分析 ===")
    
    sample_project_path = "output/example/stock-sentiment-analyzer"
    
    if not Path(sample_project_path).exists():
        print(f"样例项目不存在: {sample_project_path}")
        return False
    
    try:
        # 分析项目结构
        project_path = Path(sample_project_path)
        
        print(f"项目路径: {project_path}")
        
        # 统计文件类型
        file_types = {}
        total_files = 0
        
        for file_path in project_path.rglob("*"):
            if file_path.is_file():
                total_files += 1
                suffix = file_path.suffix.lower()
                file_types[suffix] = file_types.get(suffix, 0) + 1
        
        print(f"总文件数: {total_files}")
        print("文件类型统计:")
        
        for file_type, count in sorted(file_types.items(), key=lambda x: x[1], reverse=True):
            if file_type:  # 过滤掉无扩展名的文件
                print(f"   {file_type}: {count}")
        
        # 显示主要Python文件
        py_files = list(project_path.rglob("*.py"))
        if py_files:
            print("Python文件:")
            for py_file in py_files[:10]:  # 显示前10个
                relative_path = py_file.relative_to(project_path)
                file_size = py_file.stat().st_size
                print(f"   - {relative_path} ({file_size} 字节)")
        
        # 检查README文件
        readme_files = list(project_path.glob("README*"))
        if readme_files:
            print("README文件:")
            for readme in readme_files:
                relative_path = readme.relative_to(project_path)
                print(f"   - {relative_path}")
        
        return True
        
    except Exception as e:
        print(f"项目结构分析失败: {e}")
        return False

def main():
    """主测试函数"""
    print("样例工程分析测试")
    print("=" * 50)
    
    # 测试项目结构
    structure_test = test_project_structure()
    
    # 测试简化分析
    simple_test = test_simple_analysis()
    
    # 测试CLI分析
    cli_test = test_cli_analyze()
    
    # 总结测试结果
    print("\n" + "=" * 50)
    print("测试结果总结:")
    print(f"   项目结构分析: {'通过' if structure_test else '失败'}")
    print(f"   简化分析: {'通过' if simple_test else '失败'}")
    print(f"   CLI分析: {'通过' if cli_test else '失败'}")
    
    # 总体评估
    total_tests = 3
    passed_tests = sum([structure_test, simple_test, cli_test])
    
    print(f"\n总体通过率: {passed_tests}/{total_tests} ({passed_tests/total_tests*100:.1f}%)")
    
    if passed_tests > 0:
        print("样例工程分析测试完成")
        
        # 输出分析结果摘要
        if structure_test:
            print("\n分析结果摘要:")
            print("   项目结构分析提供了项目的文件组织信息")
        if simple_test:
            print("   代码实体分析识别了代码中的函数、类等元素")
        if cli_test:
            print("   CLI分析生成了详细的文档输出")
    else:
        print("所有测试失败")

if __name__ == "__main__":
    main()