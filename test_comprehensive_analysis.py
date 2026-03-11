#!/usr/bin/env python3
"""
综合代码资产扫描、知识提取与语法建模测试
使用KN-Fetch核心功能进行完整分析
"""
import os
import sys
import json
import yaml
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 设置API密钥
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

def comprehensive_asset_scan(project_path):
    """综合资产扫描"""
    print("=== 综合代码资产扫描 ===")
    print("=" * 60)
    
    try:
        project_dir = Path(project_path)
        
        # 详细扫描项目结构
        scan_result = {
            "project_info": {
                "name": project_dir.name,
                "path": str(project_dir.absolute())
            },
            "file_statistics": {
                "total_files": 0,
                "by_type": {},
                "by_size_category": {"small": 0, "medium": 0, "large": 0}
            },
            "code_analysis": {
                "languages": {},
                "modules": [],
                "packages": []
            },
            "documentation_analysis": {
                "total_docs": 0,
                "doc_types": {},
                "readme_files": []
            }
        }
        
        # 遍历项目文件
        for file_path in project_dir.rglob("*"):
            if file_path.is_file():
                scan_result["file_statistics"]["total_files"] += 1
                
                # 文件类型统计
                suffix = file_path.suffix.lower()
                scan_result["file_statistics"]["by_type"][suffix] = \
                    scan_result["file_statistics"]["by_type"].get(suffix, 0) + 1
                
                # 文件大小分类
                file_size = file_path.stat().st_size
                if file_size < 1024:  # < 1KB
                    scan_result["file_statistics"]["by_size_category"]["small"] += 1
                elif file_size < 10240:  # < 10KB
                    scan_result["file_statistics"]["by_size_category"]["medium"] += 1
                else:  # >= 10KB
                    scan_result["file_statistics"]["by_size_category"]["large"] += 1
                
                # 代码文件分析
                if suffix in [".py", ".js", ".ts", ".java", ".cpp", ".c", ".go", ".rs", ".php", ".rb", ".cs"]:
                    language = {
                        ".py": "Python", ".js": "JavaScript", ".ts": "TypeScript",
                        ".java": "Java", ".cpp": "C++", ".c": "C", ".go": "Go",
                        ".rs": "Rust", ".php": "PHP", ".rb": "Ruby", ".cs": "C#"
                    }.get(suffix, "Unknown")
                    
                    scan_result["code_analysis"]["languages"][language] = \
                        scan_result["code_analysis"]["languages"].get(language, 0) + 1
                
                # 文档文件分析
                if suffix in [".md", ".txt", ".rst", ".docx", ".pdf", ".html", ".htm"]:
                    scan_result["documentation_analysis"]["total_docs"] += 1
                    scan_result["documentation_analysis"]["doc_types"][suffix] = \
                        scan_result["documentation_analysis"]["doc_types"].get(suffix, 0) + 1
                    
                    # 检查README文件
                    if "readme" in file_path.name.lower():
                        scan_result["documentation_analysis"]["readme_files"].append(
                            str(file_path.relative_to(project_dir))
                        )
        
        # 输出扫描结果
        print(f"项目路径: {scan_result['project_info']['path']}")
        print(f"总文件数: {scan_result['file_statistics']['total_files']}")
        
        print("\n文件类型分布:")
        for file_type, count in sorted(scan_result["file_statistics"]["by_type"].items(), 
                                      key=lambda x: x[1], reverse=True)[:10]:
            if file_type:  # 过滤掉无扩展名的文件
                percentage = (count / scan_result["file_statistics"]["total_files"]) * 100
                print(f"   {file_type:>6}: {count:>3} 个 ({percentage:5.1f}%)")
        
        print("\n文件大小分布:")
        size_stats = scan_result["file_statistics"]["by_size_category"]
        total = scan_result["file_statistics"]["total_files"]
        print(f"   小文件 (<1KB): {size_stats['small']} 个 ({(size_stats['small']/total)*100:.1f}%)")
        print(f"   中文件 (1-10KB): {size_stats['medium']} 个 ({(size_stats['medium']/total)*100:.1f}%)")
        print(f"   大文件 (>=10KB): {size_stats['large']} 个 ({(size_stats['large']/total)*100:.1f}%)")
        
        print("\n编程语言统计:")
        for lang, count in sorted(scan_result["code_analysis"]["languages"].items(), 
                                 key=lambda x: x[1], reverse=True):
            print(f"   {lang}: {count} 个文件")
        
        print(f"\n文档文件: {scan_result['documentation_analysis']['total_docs']} 个")
        if scan_result["documentation_analysis"]["readme_files"]:
            print("README文件:")
            for readme in scan_result["documentation_analysis"]["readme_files"]:
                print(f"   - {readme}")
        
        # 保存详细扫描结果
        with open("comprehensive_asset_scan.json", "w", encoding="utf-8") as f:
            json.dump(scan_result, f, ensure_ascii=False, indent=2)
        
        print(f"\n详细资产扫描结果已保存到: comprehensive_asset_scan.json")
        
        return scan_result
        
    except Exception as e:
        print(f"综合资产扫描失败: {e}")
        return None

def advanced_syntax_modeling(project_path):
    """高级语法建模"""
    print("\n\n=== 高级语法建模 ===")
    print("=" * 60)
    
    try:
        project_dir = Path(project_path)
        
        # 高级语法模型
        syntax_model = {
            "project_metadata": {
                "name": project_dir.name,
                "language_profile": {},
                "architecture_patterns": []
            },
            "code_structure": {
                "modules": [],
                "packages": [],
                "dependencies": []
            },
            "syntactic_features": {
                "import_patterns": {},
                "function_patterns": {},
                "class_hierarchy": {},
                "code_complexity": {}
            }
        }
        
        # 分析Python文件
        py_files = list(project_dir.rglob("*.py"))
        
        if not py_files:
            print("未发现Python文件")
            return None
        
        print(f"分析 {len(py_files)} 个Python文件")
        
        # 分析文件结构
        total_imports = 0
        total_functions = 0
        total_classes = 0
        
        for py_file in py_files[:10]:  # 分析前10个文件
            try:
                relative_path = str(py_file.relative_to(project_dir))
                
                with open(py_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                lines = content.split('\n')
                
                # 分析导入模式
                imports = []
                for line in lines:
                    line = line.strip()
                    if line.startswith('import ') or line.startswith('from '):
                        imports.append(line)
                        total_imports += 1
                
                # 分析函数定义
                functions = []
                for i, line in enumerate(lines):
                    line = line.strip()
                    if line.startswith('def '):
                        func_name = line[4:].split('(')[0].strip()
                        functions.append({
                            "name": func_name,
                            "line": i + 1,
                            "file": relative_path
                        })
                        total_functions += 1
                
                # 分析类定义
                classes = []
                for i, line in enumerate(lines):
                    line = line.strip()
                    if line.startswith('class '):
                        class_name = line[6:].split('(')[0].split(':')[0].strip()
                        classes.append({
                            "name": class_name,
                            "line": i + 1,
                            "file": relative_path
                        })
                        total_classes += 1
                
                # 添加到语法模型
                file_analysis = {
                    "file_path": relative_path,
                    "size": len(content),
                    "lines": len(lines),
                    "imports": imports,
                    "functions": functions,
                    "classes": classes
                }
                
                syntax_model["code_structure"]["modules"].append(file_analysis)
                
            except Exception as e:
                print(f"分析文件失败 {py_file}: {e}")
                continue
        
        # 统计语法特征
        syntax_model["syntactic_features"]["import_patterns"] = {
            "total_imports": total_imports,
            "average_imports_per_file": total_imports / len(py_files[:10]) if py_files[:10] else 0
        }
        
        syntax_model["syntactic_features"]["function_patterns"] = {
            "total_functions": total_functions,
            "average_functions_per_file": total_functions / len(py_files[:10]) if py_files[:10] else 0
        }
        
        syntax_model["syntactic_features"]["class_hierarchy"] = {
            "total_classes": total_classes,
            "average_classes_per_file": total_classes / len(py_files[:10]) if py_files[:10] else 0
        }
        
        # 输出语法分析结果
        print(f"语法分析统计:")
        print(f"   导入语句: {total_imports} 个")
        print(f"   函数定义: {total_functions} 个")
        print(f"   类定义: {total_classes} 个")
        
        if py_files[:10]:
            avg_imports = total_imports / len(py_files[:10])
            avg_functions = total_functions / len(py_files[:10])
            avg_classes = total_classes / len(py_files[:10])
            
            print(f"   平均每个文件:")
            print(f"     - 导入: {avg_imports:.1f} 个")
            print(f"     - 函数: {avg_functions:.1f} 个") 
            print(f"     - 类: {avg_classes:.1f} 个")
        
        # 保存语法模型
        with open("advanced_syntax_model.json", "w", encoding="utf-8") as f:
            json.dump(syntax_model, f, ensure_ascii=False, indent=2)
        
        print(f"高级语法模型已保存到: advanced_syntax_model.json")
        
        return syntax_model
        
    except Exception as e:
        print(f"高级语法建模失败: {e}")
        import traceback
        traceback.print_exc()
        return None

def knowledge_extraction_analysis(project_path):
    """知识提取分析"""
    print("\n\n=== 知识提取分析 ===")
    print("=" * 60)
    
    try:
        project_dir = Path(project_path)
        
        # 知识提取结果
        knowledge_result = {
            "project_knowledge": {
                "technology_stack": [],
                "architecture_patterns": [],
                "business_domains": []
            },
            "code_knowledge": {
                "design_patterns": [],
                "coding_conventions": [],
                "api_interfaces": []
            },
            "documentation_knowledge": {
                "project_description": "",
                "usage_guides": [],
                "configuration_guides": []
            }
        }
        
        # 分析README文件获取项目信息
        readme_files = list(project_dir.glob("README*"))
        for readme in readme_files:
            try:
                with open(readme, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # 简单提取项目描述（前200字符）
                knowledge_result["documentation_knowledge"]["project_description"] = content[:200] + "..."
                break
            except:
                pass
        
        # 分析配置文件获取技术栈信息
        config_files = list(project_dir.rglob("*.yaml")) + list(project_dir.rglob("*.yml")) + \
                      list(project_dir.rglob("requirements*.txt")) + list(project_dir.rglob("pyproject.toml"))
        
        for config_file in config_files[:3]:  # 分析前3个配置文件
            try:
                relative_path = str(config_file.relative_to(project_dir))
                
                # 根据文件类型提取技术信息
                if "requirements" in config_file.name.lower():
                    knowledge_result["project_knowledge"]["technology_stack"].append({
                        "type": "Python依赖",
                        "file": relative_path
                    })
                elif "pyproject.toml" in config_file.name:
                    knowledge_result["project_knowledge"]["technology_stack"].append({
                        "type": "Python项目配置",
                        "file": relative_path
                    })
                elif config_file.suffix.lower() in [".yaml", ".yml"]:
                    knowledge_result["project_knowledge"]["technology_stack"].append({
                        "type": "YAML配置",
                        "file": relative_path
                    })
                        
            except Exception as e:
                print(f"分析配置文件失败 {config_file}: {e}")
        
        # 分析Python代码获取设计模式信息
        py_files = list(project_dir.rglob("*.py"))
        
        for py_file in py_files[:5]:  # 分析前5个Python文件
            try:
                relative_path = str(py_file.relative_to(project_dir))
                
                with open(py_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # 简单分析代码特征
                lines = content.split('\n')
                
                # 检测可能的API接口（包含route、api等关键词）
                api_keywords = ["@app.route", "@api", "def api", "def route", "endpoint"]
                api_lines = [line for line in lines if any(keyword in line.lower() for keyword in api_keywords)]
                
                if api_lines:
                    knowledge_result["code_knowledge"]["api_interfaces"].append({
                        "file": relative_path,
                        "api_count": len(api_lines)
                    })
                
                # 检测可能的类设计模式
                class_count = len([line for line in lines if line.strip().startswith('class ')])
                if class_count > 0:
                    knowledge_result["code_knowledge"]["design_patterns"].append({
                        "file": relative_path,
                        "class_count": class_count,
                        "pattern": "面向对象设计"
                    })
                        
            except Exception as e:
                print(f"分析代码文件失败 {py_file}: {e}")
        
        # 输出知识提取结果
        print("知识提取结果:")
        
        tech_stack = knowledge_result["project_knowledge"]["technology_stack"]
        if tech_stack:
            print(f"技术栈识别: {len(tech_stack)} 个技术组件")
            for tech in tech_stack:
                print(f"   - {tech['type']}: {tech['file']}")
        
        design_patterns = knowledge_result["code_knowledge"]["design_patterns"]
        if design_patterns:
            print(f"设计模式识别: {len(design_patterns)} 个文件包含设计模式")
            for pattern in design_patterns:
                print(f"   - {pattern['pattern']}: {pattern['file']} ({pattern['class_count']} 个类)")
        
        api_interfaces = knowledge_result["code_knowledge"]["api_interfaces"]
        if api_interfaces:
            print(f"API接口识别: {len(api_interfaces)} 个文件包含API")
            for api in api_interfaces:
                print(f"   - {api['file']}: {api['api_count']} 个API端点")
        
        # 保存知识提取结果
        with open("knowledge_extraction.json", "w", encoding="utf-8") as f:
            json.dump(knowledge_result, f, ensure_ascii=False, indent=2)
        
        print(f"知识提取结果已保存到: knowledge_extraction.json")
        
        return knowledge_result
        
    except Exception as e:
        print(f"知识提取分析失败: {e}")
        import traceback
        traceback.print_exc()
        return None

def generate_comprehensive_report(asset_scan, syntax_model, knowledge_result):
    """生成综合报告"""
    print("\n\n=== 综合分析报告 ===")
    print("=" * 60)
    
    report = {
        "analysis_overview": {
            "timestamp": str(Path(__file__).stat().st_ctime),
            "analysis_type": "代码资产扫描、知识提取与语法建模",
            "status": "completed"
        },
        "project_characteristics": {},
        "technical_insights": {},
        "quality_assessment": {},
        "recommendations": []
    }
    
    # 项目特征
    if asset_scan:
        report["project_characteristics"] = {
            "size": asset_scan["file_statistics"]["total_files"],
            "language_distribution": asset_scan["code_analysis"]["languages"],
            "documentation_richness": asset_scan["documentation_analysis"]["total_docs"],
            "file_size_distribution": asset_scan["file_statistics"]["by_size_category"]
        }
    
    # 技术洞察
    if syntax_model:
        report["technical_insights"] = {
            "code_complexity": syntax_model["syntactic_features"]["function_patterns"],
            "architecture_patterns": syntax_model["syntactic_features"]["class_hierarchy"],
            "dependency_analysis": syntax_model["syntactic_features"]["import_patterns"]
        }
    
    # 质量评估
    quality_score = 0
    quality_factors = []
    
    if asset_scan:
        total_files = asset_scan["file_statistics"]["total_files"]
        doc_files = asset_scan["documentation_analysis"]["total_docs"]
        code_files = sum(asset_scan["code_analysis"]["languages"].values())
        
        # 文档覆盖率
        doc_coverage = (doc_files / total_files) * 100 if total_files > 0 else 0
        if doc_coverage > 30:
            quality_score += 25
            quality_factors.append("文档丰富")
        
        # 代码质量
        if code_files > 0:
            quality_score += 25
            quality_factors.append("代码结构完整")
    
    if syntax_model:
        if syntax_model["syntactic_features"]["function_patterns"]["total_functions"] > 0:
            quality_score += 25
            quality_factors.append("功能模块化")
        
        if syntax_model["syntactic_features"]["class_hierarchy"]["total_classes"] > 0:
            quality_score += 25
            quality_factors.append("面向对象设计")
    
    report["quality_assessment"] = {
        "score": quality_score,
        "factors": quality_factors,
        "rating": "优秀" if quality_score >= 80 else "良好" if quality_score >= 60 else "一般"
    }
    
    # 建议
    if quality_score < 80:
        report["recommendations"].append("建议增加测试覆盖率")
    if asset_scan and asset_scan["documentation_analysis"]["total_docs"] < 10:
        report["recommendations"].append("建议完善项目文档")
    
    # 输出报告
    print("项目分析报告:")
    print(f"   项目规模: {report['project_characteristics'].get('size', 0)} 个文件")
    print(f"   主要语言: {', '.join(report['project_characteristics'].get('language_distribution', {}).keys())}")
    print(f"   文档丰富度: {report['project_characteristics'].get('documentation_richness', 0)} 个文档文件")
    
    print(f"\n技术洞察:")
    insights = report["technical_insights"]
    if "code_complexity" in insights:
        print(f"   函数数量: {insights['code_complexity'].get('total_functions', 0)}")
    if "architecture_patterns" in insights:
        print(f"   类数量: {insights['architecture_patterns'].get('total_classes', 0)}")
    
    print(f"\n质量评估:")
    quality = report["quality_assessment"]
    print(f"   综合评分: {quality.get('score', 0)}/100")
    print(f"   评级: {quality.get('rating', '未知')}")
    print(f"   优势: {', '.join(quality.get('factors', []))}")
    
    if report["recommendations"]:
        print(f"\n改进建议:")
        for rec in report["recommendations"]:
            print(f"   - {rec}")
    
    # 保存报告
    with open("comprehensive_analysis_report.json", "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    print(f"\n综合分析报告已保存到: comprehensive_analysis_report.json")
    
    return report

def main():
    """主函数"""
    print("KN-Fetch 综合代码分析演示")
    print("=" * 60)
    
    # 使用当前项目作为分析目标
    project_path = "."
    
    try:
        # 1. 综合资产扫描
        asset_scan = comprehensive_asset_scan(project_path)
        
        # 2. 高级语法建模
        syntax_model = advanced_syntax_modeling(project_path)
        
        # 3. 知识提取分析
        knowledge_result = knowledge_extraction_analysis(project_path)
        
        # 4. 生成综合报告
        report = generate_comprehensive_report(asset_scan, syntax_model, knowledge_result)
        
        print("\n" + "=" * 60)
        print("综合代码分析完成！")
        print("生成的分析文件:")
        print("   - comprehensive_asset_scan.json (综合资产扫描)")
        print("   - advanced_syntax_model.json (高级语法模型)")
        print("   - knowledge_extraction.json (知识提取结果)")
        print("   - comprehensive_analysis_report.json (综合分析报告)")
        
        print("\n分析总结:")
        print("✅ 代码资产扫描: 完成项目结构分析和文件分类")
        print("✅ 语法建模: 完成代码结构分析和语法特征提取")
        print("✅ 知识提取: 完成技术栈识别和设计模式分析")
        print("✅ 质量评估: 完成项目质量综合评估")
        
    except Exception as e:
        print(f"分析过程中出现错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()