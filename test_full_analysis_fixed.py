#!/usr/bin/env python3
"""
完整代码资产扫描、知识提取与语法建模测试
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

def scan_project_assets(project_path):
    """扫描代码资产"""
    print("=== 代码资产扫描 ===")
    print("=" * 60)
    
    if not Path(project_path).exists():
        print(f"ERROR: 项目路径不存在: {project_path}")
        return None
    
    try:
        # 扫描项目结构
        project_dir = Path(project_path)
        asset_scan = {
            "project_name": project_dir.name,
            "total_files": 0,
            "file_types": {},
            "code_files": [],
            "documentation_files": [],
            "configuration_files": [],
            "build_files": [],
            "test_files": []
        }
        
        # 遍历所有文件
        for file_path in project_dir.rglob("*"):
            if file_path.is_file():
                asset_scan["total_files"] += 1
                
                suffix = file_path.suffix.lower()
                asset_scan["file_types"][suffix] = asset_scan["file_types"].get(suffix, 0) + 1
                
                relative_path = str(file_path.relative_to(project_dir))
                file_size = file_path.stat().st_size
                
                # 分类文件
                file_info = {
                    "path": relative_path,
                    "size": file_size,
                    "type": "other"
                }
                
                if suffix in [".py", ".js", ".ts", ".java", ".cpp", ".c", ".go", ".rs", ".php", ".rb", ".cs"]:
                    file_info["type"] = "code"
                    asset_scan["code_files"].append(file_info)
                elif suffix in [".md", ".txt", ".rst", ".docx", ".pdf", ".html", ".htm"]:
                    file_info["type"] = "documentation"
                    asset_scan["documentation_files"].append(file_info)
                elif suffix in [".yaml", ".yml", ".json", ".ini", ".cfg", ".toml", ".properties"]:
                    file_info["type"] = "configuration"
                    asset_scan["configuration_files"].append(file_info)
                elif suffix in ["makefile", ".mk", ".sh", ".bat", ".ps1", ".dockerfile"] or file_path.name.lower() == "dockerfile":
                    file_info["type"] = "build"
                    asset_scan["build_files"].append(file_info)
                elif "test" in relative_path.lower() or suffix in [".spec", ".test"]:
                    file_info["type"] = "test"
                    asset_scan["test_files"].append(file_info)
                else:
                    asset_scan["code_files"].append(file_info)  # 默认归为代码文件
        
        # 输出扫描结果
        print(f"项目名称: {asset_scan['project_name']}")
        print(f"总文件数: {asset_scan['total_files']}")
        
        print("\n文件类型分布:")
        for file_type, count in sorted(asset_scan["file_types"].items(), key=lambda x: x[1], reverse=True):
            if file_type:  # 过滤掉无扩展名的文件
                percentage = (count / asset_scan["total_files"]) * 100
                print(f"   {file_type:>6}: {count:>3} 个 ({percentage:5.1f}%)")
        
        print(f"\n代码文件: {len(asset_scan['code_files'])} 个")
        print(f"文档文件: {len(asset_scan['documentation_files'])} 个")
        print(f"配置文件: {len(asset_scan['configuration_files'])} 个")
        print(f"构建文件: {len(asset_scan['build_files'])} 个")
        print(f"测试文件: {len(asset_scan['test_files'])} 个")
        
        # 保存扫描结果
        with open("asset_scan_result.json", "w", encoding="utf-8") as f:
            json.dump(asset_scan, f, ensure_ascii=False, indent=2)
        
        print(f"\n资产扫描结果已保存到: asset_scan_result.json")
        
        return asset_scan
        
    except Exception as e:
        print(f"资产扫描失败: {e}")
        return None

def extract_knowledge(project_path):
    """知识提取"""
    print("\n\n=== 知识提取 ===")
    print("=" * 60)
    
    try:
        # 导入必要的模块
        from src.core.semantic_extractor import scan_project_files, extract_semantic_features
        
        print(f"提取知识项目: {project_path}")
        
        # 扫描项目文件
        files = scan_project_files(project_path)
        
        if not files:
            print("未发现可分析的文件")
            return None
        
        print(f"发现 {len(files)} 个文件")
        
        # 提取语义特征
        print("提取语义特征...")
        features = extract_semantic_features(project_path)
        
        if features:
            print(f"提取到 {len(features)} 个语义特征")
            
            # 分析特征类型
            feature_types = {}
            for feature in features:
                if isinstance(feature, dict):
                    feature_type = feature.get('type', 'unknown')
                    feature_types[feature_type] = feature_types.get(feature_type, 0) + 1
            
            print("特征类型分布:")
            for feature_type, count in sorted(feature_types.items(), key=lambda x: x[1], reverse=True):
                print(f"   {feature_type}: {count}")
            
            # 保存特征数据
            with open("knowledge_features.json", "w", encoding="utf-8") as f:
                json.dump(features, f, ensure_ascii=False, indent=2)
            
            print(f"知识特征已保存到: knowledge_features.json")
            
            return features
        else:
            print("未提取到语义特征")
            return None
            
    except Exception as e:
        print(f"知识提取失败: {e}")
        import traceback
        traceback.print_exc()
        return None

def build_syntax_model(project_path):
    """语法建模"""
    print("\n\n=== 语法建模 ===")
    print("=" * 60)
    
    try:
        # 导入语法分析模块
        from src.gitnexus.parser import CodeParser
        
        print(f"构建语法模型: {project_path}")
        
        # 创建语法解析器
        parser = CodeParser()
        
        # 分析主要代码文件
        syntax_model = {
            "project_name": Path(project_path).name,
            "languages": {},
            "file_structures": [],
            "import_dependencies": [],
            "function_definitions": [],
            "class_definitions": [],
            "variable_declarations": []
        }
        
        # 扫描Python文件
        py_files = list(Path(project_path).rglob("*.py"))
        
        if not py_files:
            print("未发现Python文件")
            return None
        
        print(f"发现 {len(py_files)} 个Python文件")
        
        # 分析前5个文件
        analyzed_files = 0
        for py_file in py_files[:5]:
            try:
                relative_path = str(py_file.relative_to(project_path))
                print(f"   分析: {relative_path}")
                
                # 读取文件内容
                with open(py_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # 分析文件结构
                file_structure = {
                    "file_path": relative_path,
                    "size": len(content),
                    "lines": len(content.split('\n')),
                    "imports": [],
                    "functions": [],
                    "classes": []
                }
                
                # 提取导入语句（简单正则匹配）
                import_lines = []
                for line in content.split('\n'):
                    line = line.strip()
                    if line.startswith('import ') or line.startswith('from '):
                        import_lines.append(line)
                
                file_structure["imports"] = import_lines
                
                # 提取函数定义（简单正则匹配）
                function_defs = []
                for i, line in enumerate(content.split('\n')):
                    line = line.strip()
                    if line.startswith('def '):
                        func_name = line[4:].split('(')[0].strip()
                        function_defs.append({
                            "name": func_name,
                            "line": i + 1
                        })
                
                file_structure["functions"] = function_defs
                
                # 提取类定义（简单正则匹配）
                class_defs = []
                for i, line in enumerate(content.split('\n')):
                    line = line.strip()
                    if line.startswith('class '):
                        class_name = line[6:].split('(')[0].split(':')[0].strip()
                        class_defs.append({
                            "name": class_name,
                            "line": i + 1
                        })
                
                file_structure["classes"] = class_defs
                
                syntax_model["file_structures"].append(file_structure)
                
                # 更新统计
                syntax_model["import_dependencies"].extend(import_lines)
                syntax_model["function_definitions"].extend(function_defs)
                syntax_model["class_definitions"].extend(class_defs)
                
                analyzed_files += 1
                
            except Exception as e:
                print(f"   分析文件失败: {e}")
                continue
        
        # 统计语言使用
        syntax_model["languages"]["Python"] = len(py_files)
        
        # 输出语法模型统计
        print(f"\n语法模型统计:")
        print(f"   分析文件数: {analyzed_files}")
        print(f"   导入依赖: {len(syntax_model['import_dependencies'])} 个")
        print(f"   函数定义: {len(syntax_model['function_definitions'])} 个")
        print(f"   类定义: {len(syntax_model['class_definitions'])} 个")
        
        # 保存语法模型
        with open("syntax_model.json", "w", encoding="utf-8") as f:
            json.dump(syntax_model, f, ensure_ascii=False, indent=2)
        
        print(f"语法模型已保存到: syntax_model.json")
        
        return syntax_model
        
    except Exception as e:
        print(f"语法建模失败: {e}")
        import traceback
        traceback.print_exc()
        return None

def generate_analysis_report(asset_scan, knowledge_features, syntax_model):
    """生成分析报告"""
    print("\n\n=== 分析报告生成 ===")
    print("=" * 60)
    
    report = {
        "analysis_summary": {
            "timestamp": str(Path(__file__).stat().st_ctime),
            "status": "completed"
        },
        "asset_scan_summary": {},
        "knowledge_extraction_summary": {},
        "syntax_modeling_summary": {},
        "technical_assessment": {}
    }
    
    # 资产扫描总结
    if asset_scan:
        report["asset_scan_summary"] = {
            "project_name": asset_scan.get("project_name", "Unknown"),
            "total_files": asset_scan.get("total_files", 0),
            "code_files": len(asset_scan.get("code_files", [])),
            "documentation_files": len(asset_scan.get("documentation_files", [])),
            "configuration_files": len(asset_scan.get("configuration_files", [])),
            "build_files": len(asset_scan.get("build_files", [])),
            "test_files": len(asset_scan.get("test_files", []))
        }
    
    # 知识提取总结
    if knowledge_features:
        feature_types = {}
        for feature in knowledge_features:
            if isinstance(feature, dict):
                feature_type = feature.get('type', 'unknown')
                feature_types[feature_type] = feature_types.get(feature_type, 0) + 1
        
        report["knowledge_extraction_summary"] = {
            "total_features": len(knowledge_features),
            "feature_types": feature_types
        }
    
    # 语法建模总结
    if syntax_model:
        report["syntax_modeling_summary"] = {
            "languages": syntax_model.get("languages", {}),
            "files_analyzed": len(syntax_model.get("file_structures", [])),
            "imports": len(syntax_model.get("import_dependencies", [])),
            "functions": len(syntax_model.get("function_definitions", [])),
            "classes": len(syntax_model.get("class_definitions", []))
        }
    
    # 技术评估
    report["technical_assessment"] = {
        "code_quality": "良好" if asset_scan and asset_scan.get("total_files", 0) > 0 else "未知",
        "documentation": "丰富" if asset_scan and len(asset_scan.get("documentation_files", [])) > 5 else "基础",
        "test_coverage": "良好" if asset_scan and len(asset_scan.get("test_files", [])) > 0 else "未知",
        "architecture": "模块化" if syntax_model and len(syntax_model.get("file_structures", [])) > 0 else "未知"
    }
    
    # 保存报告
    with open("analysis_report.json", "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    # 输出报告摘要
    print("分析报告摘要:")
    print(f"   项目名称: {report['asset_scan_summary'].get('project_name', 'Unknown')}")
    print(f"   总文件数: {report['asset_scan_summary'].get('total_files', 0)}")
    print(f"   代码文件: {report['asset_scan_summary'].get('code_files', 0)}")
    print(f"   文档文件: {report['asset_scan_summary'].get('documentation_files', 0)}")
    print(f"   知识特征: {report['knowledge_extraction_summary'].get('total_features', 0)}")
    print(f"   语法元素: {report['syntax_modeling_summary'].get('functions', 0)} 函数, {report['syntax_modeling_summary'].get('classes', 0)} 类")
    
    print(f"\n完整分析报告已保存到: analysis_report.json")
    
    return report

def main():
    """主函数"""
    print("KN-Fetch 完整代码分析演示")
    print("=" * 60)
    
    # 使用当前项目作为分析目标
    project_path = "."
    
    try:
        # 1. 代码资产扫描
        asset_scan = scan_project_assets(project_path)
        
        # 2. 知识提取
        knowledge_features = extract_knowledge(project_path)
        
        # 3. 语法建模
        syntax_model = build_syntax_model(project_path)
        
        # 4. 生成分析报告
        report = generate_analysis_report(asset_scan, knowledge_features, syntax_model)
        
        print("\n" + "=" * 60)
        print("完整代码分析完成！")
        print("生成的文件:")
        print("   - asset_scan_result.json (资产扫描)")
        print("   - knowledge_features.json (知识特征)")
        print("   - syntax_model.json (语法模型)")
        print("   - analysis_report.json (分析报告)")
        
    except Exception as e:
        print(f"分析过程中出现错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()