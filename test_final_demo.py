#!/usr/bin/env python3
"""
最终演示测试 - 展示样例工程分析效果
"""
import os
import sys
import json
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 设置API密钥
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

def analyze_project_structure():
    """分析项目结构"""
    print("📂 项目结构分析")
    print("=" * 60)
    
    sample_project_path = "output/example/stock-sentiment-analyzer"
    
    if not Path(sample_project_path).exists():
        print("❌ 样例项目不存在")
        return
    
    project_path = Path(sample_project_path)
    
    # 分析项目结构
    analysis_result = {
        "project_name": project_path.name,
        "total_files": 0,
        "file_types": {},
        "python_files": [],
        "documentation_files": [],
        "configuration_files": []
    }
    
    # 扫描文件
    for file_path in project_path.rglob("*"):
        if file_path.is_file():
            analysis_result["total_files"] += 1
            
            suffix = file_path.suffix.lower()
            analysis_result["file_types"][suffix] = analysis_result["file_types"].get(suffix, 0) + 1
            
            relative_path = file_path.relative_to(project_path)
            
            # 分类文件
            if suffix == ".py":
                file_size = file_path.stat().st_size
                analysis_result["python_files"].append({
                    "path": str(relative_path),
                    "size": file_size
                })
            elif suffix in [".md", ".txt"]:
                analysis_result["documentation_files"].append(str(relative_path))
            elif suffix in [".yaml", ".yml", ".json", ".ini", ".cfg"]:
                analysis_result["configuration_files"].append(str(relative_path))
    
    # 输出分析结果
    print(f"📁 项目名称: {analysis_result['project_name']}")
    print(f"📊 总文件数: {analysis_result['total_files']}")
    
    print("\n📋 文件类型分布:")
    for file_type, count in sorted(analysis_result["file_types"].items(), key=lambda x: x[1], reverse=True):
        if file_type:  # 过滤掉无扩展名的文件
            percentage = (count / analysis_result["total_files"]) * 100
            print(f"   {file_type:>6}: {count:>3} 个 ({percentage:5.1f}%)")
    
    print(f"\n🐍 Python文件 ({len(analysis_result['python_files'])} 个):")
    for py_file in sorted(analysis_result["python_files"], key=lambda x: x['size'], reverse=True)[:8]:
        size_kb = py_file['size'] / 1024
        print(f"   📄 {py_file['path']:<40} ({size_kb:5.1f} KB)")
    
    if len(analysis_result['python_files']) > 8:
        print(f"   ... 还有 {len(analysis_result['python_files']) - 8} 个Python文件")
    
    print(f"\n📚 文档文件 ({len(analysis_result['documentation_files'])} 个):")
    for doc_file in analysis_result['documentation_files'][:5]:
        print(f"   📖 {doc_file}")
    
    print(f"\n⚙️  配置文件 ({len(analysis_result['configuration_files'])} 个):")
    for config_file in analysis_result['configuration_files']:
        print(f"   ⚙️  {config_file}")
    
    # 保存分析结果
    output_file = "project_analysis_result.json"
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(analysis_result, f, ensure_ascii=False, indent=2)
    
    print(f"\n💾 分析结果已保存到: {output_file}")
    
    return analysis_result

def analyze_code_content():
    """分析代码内容"""
    print("\n\n🔍 代码内容分析")
    print("=" * 60)
    
    sample_project_path = "output/example/stock-sentiment-analyzer"
    
    if not Path(sample_project_path).exists():
        print("❌ 样例项目不存在")
        return
    
    # 分析主要Python文件
    main_files = [
        "gitnexus.py",
        "test.py", 
        "test_capital_flow.py",
        "test_main.py",
        "src/main.py"
    ]
    
    code_analysis = {}
    
    for filename in main_files:
        file_path = Path(sample_project_path) / filename
        
        if file_path.exists():
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # 基础代码分析
                lines = content.split('\n')
                total_lines = len(lines)
                code_lines = len([line for line in lines if line.strip() and not line.strip().startswith('#')])
                comment_lines = len([line for line in lines if line.strip().startswith('#')])
                
                # 提取函数和类（简化分析）
                functions = []
                classes = []
                
                for i, line in enumerate(lines):
                    line = line.strip()
                    if line.startswith('def '):
                        func_name = line[4:].split('(')[0].strip()
                        functions.append({"name": func_name, "line": i+1})
                    elif line.startswith('class '):
                        class_name = line[6:].split('(')[0].split(':')[0].strip()
                        classes.append({"name": class_name, "line": i+1})
                
                code_analysis[filename] = {
                    "size_bytes": len(content),
                    "total_lines": total_lines,
                    "code_lines": code_lines,
                    "comment_lines": comment_lines,
                    "comment_density": comment_lines / total_lines if total_lines > 0 else 0,
                    "functions": functions,
                    "classes": classes
                }
                
                print(f"📄 {filename}:")
                print(f"   大小: {len(content)} 字符, {total_lines} 行")
                print(f"   代码行: {code_lines}, 注释行: {comment_lines}")
                print(f"   注释密度: {code_analysis[filename]['comment_density']:.1%}")
                print(f"   函数: {len(functions)} 个")
                print(f"   类: {len(classes)} 个")
                
                # 显示前3个函数
                if functions:
                    print("   主要函数:")
                    for func in functions[:3]:
                        print(f"     - {func['name']} (第{func['line']}行)")
                
                if classes:
                    print("   主要类:")
                    for cls in classes[:3]:
                        print(f"     - {cls['name']} (第{cls['line']}行)")
                
                print()
                
            except Exception as e:
                print(f"❌ 分析文件 {filename} 失败: {e}")
    
    # 保存代码分析结果
    output_file = "code_analysis_result.json"
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(code_analysis, f, ensure_ascii=False, indent=2)
    
    print(f"💾 代码分析结果已保存到: {output_file}")
    
    return code_analysis

def generate_summary_report():
    """生成总结报告"""
    print("\n\n📊 项目分析总结报告")
    print("=" * 60)
    
    # 项目结构分析
    structure_result = analyze_project_structure()
    
    # 代码内容分析  
    code_result = analyze_code_content()
    
    # 生成总结
    print("\n🎯 项目特点总结:")
    
    if structure_result:
        print(f"📁 项目规模: {structure_result['total_files']} 个文件")
        
        # 文档丰富度
        doc_count = len(structure_result['documentation_files'])
        doc_ratio = doc_count / structure_result['total_files'] * 100
        print(f"📚 文档丰富度: {doc_count} 个文档文件 ({doc_ratio:.1f}%)")
        
        # 代码质量指标
        py_count = len(structure_result['python_files'])
        if py_count > 0:
            avg_file_size = sum(f['size'] for f in structure_result['python_files']) / py_count / 1024
            print(f"🐍 代码规模: {py_count} 个Python文件，平均 {avg_file_size:.1f} KB/文件")
    
    if code_result:
        total_code_lines = sum(analysis['code_lines'] for analysis in code_result.values())
        total_comment_lines = sum(analysis['comment_lines'] for analysis in code_result.values())
        
        if total_code_lines > 0:
            comment_density = total_comment_lines / total_code_lines
            print(f"💬 代码注释密度: {comment_density:.1%}")
        
        total_functions = sum(len(analysis['functions']) for analysis in code_result.values())
        total_classes = sum(len(analysis['classes']) for analysis in code_result.values())
        
        print(f"🔧 代码结构: {total_functions} 个函数，{total_classes} 个类")
    
    print("\n🔍 技术栈推测:")
    print("   - 主要语言: Python")
    print("   - 文档工具: Markdown")
    print("   - 配置管理: YAML")
    print("   - 项目类型: 数据分析/股票情感分析")
    
    print("\n📈 项目成熟度评估:")
    if structure_result and structure_result['total_files'] > 100:
        print("   ✅ 项目规模较大，结构完整")
        print("   ✅ 文档丰富，便于维护")
        print("   ✅ 包含测试和示例代码")
        print("   📊 成熟度: 高")
    else:
        print("   📊 成熟度: 中等")

def main():
    """主函数"""
    print("🎯 KN-Fetch 样例工程分析演示")
    print("=" * 60)
    
    try:
        # 生成总结报告
        generate_summary_report()
        
        print("\n" + "=" * 60)
        print("✅ 分析完成！")
        print("📁 生成的文件:")
        print("   - project_analysis_result.json (项目结构分析)")
        print("   - code_analysis_result.json (代码内容分析)")
        
    except Exception as e:
        print(f"❌ 分析过程中出现错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()