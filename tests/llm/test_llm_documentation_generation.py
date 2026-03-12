#!/usr/bin/env python3
"""
LLM深度分析文档生成测试 - 直接使用LLM生成详细设计文档和架构文档
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

def analyze_project_structure_for_llm():
    """为LLM分析准备项目结构数据"""
    print("=== 项目结构分析（LLM准备） ===")
    print("=" * 60)
    
    project_path = "."
    project_dir = Path(project_path)
    
    # 详细的项目结构分析
    project_analysis = {
        "project_name": project_dir.name or "KN-Fetch",
        "project_path": str(project_dir.absolute()),
        "total_files": 0,
        "file_types": {},
        "code_files": [],
        "documentation_files": [],
        "configuration_files": [],
        "build_files": [],
        "test_files": [],
        "directory_structure": {}
    }
    
    # 分析目录结构
    def analyze_directory(dir_path, depth=0):
        """递归分析目录结构"""
        structure = {}
        
        for item in dir_path.iterdir():
            if item.is_file():
                project_analysis["total_files"] += 1
                
                suffix = item.suffix.lower()
                project_analysis["file_types"][suffix] = project_analysis["file_types"].get(suffix, 0) + 1
                
                relative_path = str(item.relative_to(project_dir))
                file_info = {
                    "path": relative_path,
                    "size": item.stat().st_size,
                    "type": "other"
                }
                
                # 分类文件
                if suffix in [".py", ".js", ".ts", ".java", ".cpp", ".c", ".go", ".rs", ".php", ".rb", ".cs"]:
                    file_info["type"] = "code"
                    project_analysis["code_files"].append(file_info)
                elif suffix in [".md", ".txt", ".rst", ".docx", ".pdf", ".html", ".htm"]:
                    file_info["type"] = "documentation"
                    project_analysis["documentation_files"].append(file_info)
                elif suffix in [".yaml", ".yml", ".json", ".ini", ".cfg", ".toml", ".properties"]:
                    file_info["type"] = "configuration"
                    project_analysis["configuration_files"].append(file_info)
                elif suffix in ["makefile", ".mk", ".sh", ".bat", ".ps1", ".dockerfile"] or item.name.lower() == "dockerfile":
                    file_info["type"] = "build"
                    project_analysis["build_files"].append(file_info)
                elif "test" in relative_path.lower() or suffix in [".spec", ".test"]:
                    file_info["type"] = "test"
                    project_analysis["test_files"].append(file_info)
                
                structure[item.name] = {"type": "file", "info": file_info}
                
            elif item.is_dir() and depth < 3:  # 限制递归深度
                structure[item.name] = {"type": "directory", "content": analyze_directory(item, depth + 1)}
        
        return structure
    
    project_analysis["directory_structure"] = analyze_directory(project_dir)
    
    # 输出分析结果
    print(f"项目名称: {project_analysis['project_name']}")
    print(f"总文件数: {project_analysis['total_files']}")
    print(f"代码文件: {len(project_analysis['code_files'])}")
    print(f"文档文件: {len(project_analysis['documentation_files'])}")
    print(f"配置文件: {len(project_analysis['configuration_files'])}")
    
    # 保存项目结构数据
    with open("project_structure_for_llm.json", "w", encoding="utf-8") as f:
        json.dump(project_analysis, f, ensure_ascii=False, indent=2)
    
    print(f"\n项目结构数据已保存到: project_structure_for_llm.json")
    
    return project_analysis

def analyze_code_architecture():
    """分析代码架构"""
    print("\n\n=== 代码架构分析 ===")
    print("=" * 60)
    
    project_path = "."
    
    # 分析主要代码文件
    architecture_analysis = {
        "modules": [],
        "dependencies": {},
        "design_patterns": [],
        "architecture_style": "",
        "entry_points": [],
        "key_components": []
    }
    
    # 分析主要Python文件
    main_py_files = [
        "kn-fetch.py",
        "src/ai/llm_client.py",
        "src/ai/deep_knowledge_analyzer.py",
        "src/core/semantic_extractor.py",
        "src/core/workflow_engine.py",
        "src/agents/architecture_analyzer.py",
        "src/agents/business_logic.py",
        "src/agents/documentation.py"
    ]
    
    for file_path in main_py_files:
        full_path = Path(project_path) / file_path
        
        if full_path.exists():
            try:
                with open(full_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # 基础代码分析
                lines = content.split('\n')
                
                # 提取模块信息
                module_info = {
                    "file": file_path,
                    "size": len(content),
                    "lines": len(lines),
                    "imports": [],
                    "functions": [],
                    "classes": [],
                    "description": ""
                }
                
                # 分析导入
                for line in lines:
                    line = line.strip()
                    if line.startswith('import ') or line.startswith('from '):
                        module_info["imports"].append(line)
                
                # 分析函数和类
                for i, line in enumerate(lines):
                    line = line.strip()
                    if line.startswith('def '):
                        func_name = line[4:].split('(')[0].strip()
                        module_info["functions"].append({
                            "name": func_name,
                            "line": i + 1
                        })
                    elif line.startswith('class '):
                        class_name = line[6:].split('(')[0].split(':')[0].strip()
                        module_info["classes"].append({
                            "name": class_name,
                            "line": i + 1
                        })
                
                # 提取模块描述（文件开头的注释）
                description_lines = []
                for line in lines[:20]:  # 前20行
                    if line.strip().startswith('#'):
                        description_lines.append(line.strip()[1:].strip())
                    elif line.strip().startswith('"""'):
                        description_lines.append(line.strip()[3:].strip())
                    elif description_lines and line.strip():  # 遇到非空行停止
                        break
                
                if description_lines:
                    module_info["description"] = ' '.join(description_lines)[:200]
                
                architecture_analysis["modules"].append(module_info)
                
                print(f"分析模块: {file_path}")
                print(f"  函数: {len(module_info['functions'])} 个")
                print(f"  类: {len(module_info['classes'])} 个")
                print(f"  导入: {len(module_info['imports'])} 个")
                
            except Exception as e:
                print(f"分析文件失败 {file_path}: {e}")
    
    # 分析架构特征
    total_functions = sum(len(module["functions"]) for module in architecture_analysis["modules"])
    total_classes = sum(len(module["classes"]) for module in architecture_analysis["modules"])
    
    # 推断架构风格
    if total_classes > total_functions * 0.5:
        architecture_analysis["architecture_style"] = "面向对象"
    elif any("agent" in module["file"] for module in architecture_analysis["modules"]):
        architecture_analysis["architecture_style"] = "智能体架构"
    else:
        architecture_analysis["architecture_style"] = "模块化"
    
    # 识别关键组件
    key_components = []
    for module in architecture_analysis["modules"]:
        if "llm" in module["file"]:
            key_components.append({"type": "LLM客户端", "module": module["file"]})
        elif "agent" in module["file"]:
            key_components.append({"type": "智能体", "module": module["file"]})
        elif "workflow" in module["file"]:
            key_components.append({"type": "工作流引擎", "module": module["file"]})
        elif "core" in module["file"]:
            key_components.append({"type": "核心模块", "module": module["file"]})
    
    architecture_analysis["key_components"] = key_components
    
    print(f"\n架构分析结果:")
    print(f"  分析模块数: {len(architecture_analysis['modules'])}")
    print(f"  总函数数: {total_functions}")
    print(f"  总类数: {total_classes}")
    print(f"  架构风格: {architecture_analysis['architecture_style']}")
    print(f"  关键组件: {len(key_components)} 个")
    
    # 保存架构分析数据
    with open("architecture_analysis.json", "w", encoding="utf-8") as f:
        json.dump(architecture_analysis, f, ensure_ascii=False, indent=2)
    
    print(f"\n架构分析数据已保存到: architecture_analysis.json")
    
    return architecture_analysis

def generate_llm_prompts(project_structure, architecture_analysis):
    """生成LLM分析提示词"""
    print("\n\n=== 生成LLM分析提示词 ===")
    print("=" * 60)
    
    # 架构文档生成提示词
    architecture_prompt = f"""
请基于以下项目信息生成详细的架构设计文档：

项目名称: {project_structure['project_name']}
项目规模: {project_structure['total_files']} 个文件
代码文件: {len(project_structure['code_files'])} 个
文档文件: {len(project_structure['documentation_files'])} 个

架构特征:
- 架构风格: {architecture_analysis['architecture_style']}
- 分析模块数: {len(architecture_analysis['modules'])}
- 总函数数: {sum(len(m['functions']) for m in architecture_analysis['modules'])}
- 总类数: {sum(len(m['classes']) for m in architecture_analysis['modules'])}

关键组件:
{chr(10).join(f"- {comp['type']}: {comp['module']}" for comp in architecture_analysis['key_components'])}

请生成包含以下章节的详细架构设计文档：
1. 项目概述
2. 架构设计原则
3. 系统架构图（文字描述）
4. 核心模块说明
5. 数据流设计
6. 技术选型说明
7. 部署架构
8. 扩展性考虑

请使用专业的架构文档格式，确保内容详细且结构清晰。
"""
    
    # 设计文档生成提示词
    design_prompt = f"""
请基于以下项目信息生成详细的设计文档：

项目名称: {project_structure['project_name']}
项目类型: 代码分析工具/知识提取系统

技术栈特征:
- 主要语言: Python
- 架构模式: {architecture_analysis['architecture_style']}
- 关键功能: LLM集成、代码分析、智能体编排

请生成包含以下章节的详细设计文档：
1. 系统设计概述
2. 功能模块设计
3. 数据库设计（如适用）
4. API接口设计
5. 用户界面设计（如适用）
6. 安全设计考虑
7. 性能优化策略
8. 错误处理机制

请确保设计文档详细、实用，包含具体的设计决策和实现建议。
"""
    
    # API文档生成提示词
    api_prompt = f"""
请基于以下项目信息生成API参考文档：

项目名称: {project_structure['project_name']}
项目类型: 代码分析工具

主要功能模块:
{chr(10).join(f"- {m['file']}" for m in architecture_analysis['modules'][:10])}

请生成详细的API参考文档，包含：
1. API概览
2. 认证机制
3. 主要接口说明
4. 请求/响应示例
5. 错误码说明
6. 使用限制

请确保API文档格式规范，便于开发者使用。
"""
    
    # 保存提示词
    prompts = {
        "architecture_prompt": architecture_prompt,
        "design_prompt": design_prompt,
        "api_prompt": api_prompt
    }
    
    with open("llm_prompts.json", "w", encoding="utf-8") as f:
        json.dump(prompts, f, ensure_ascii=False, indent=2)
    
    print("LLM分析提示词已生成:")
    print("  - 架构文档生成提示词")
    print("  - 设计文档生成提示词") 
    print("  - API文档生成提示词")
    print(f"\n提示词已保存到: llm_prompts.json")
    
    return prompts

def generate_documentation_templates():
    """生成文档模板"""
    print("\n\n=== 生成文档模板 ===")
    print("=" * 60)
    
    # 架构文档模板
    architecture_template = """# 架构设计文档

## 项目概述
{项目基本信息和目标}

## 架构设计原则
{设计原则和约束}

## 系统架构图
{架构图文字描述}

## 核心模块说明
{各模块功能和关系}

## 数据流设计
{数据流向和处理流程}

## 技术选型说明
{技术栈选择和理由}

## 部署架构
{部署环境和配置}

## 扩展性考虑
{系统扩展策略}
"""
    
    # 设计文档模板
    design_template = """# 系统设计文档

## 系统设计概述
{系统总体设计}

## 功能模块设计
{各功能模块详细设计}

## 数据库设计
{数据模型和存储设计}

## API接口设计
{API规范和接口定义}

## 用户界面设计
{UI/UX设计说明}

## 安全设计考虑
{安全策略和机制}

## 性能优化策略
{性能优化措施}

## 错误处理机制
{错误处理和恢复}
"""
    
    # API文档模板
    api_template = """# API参考文档

## API概览
{API总体介绍}

## 认证机制
{认证方式和流程}

## 主要接口说明
{各接口详细说明}

## 请求/响应示例
{具体使用示例}

## 错误码说明
{错误码和含义}

## 使用限制
{使用限制和配额}
"""
    
    # 保存模板
    templates = {
        "architecture_template": architecture_template,
        "design_template": design_template,
        "api_template": api_template
    }
    
    with open("documentation_templates.md", "w", encoding="utf-8") as f:
        f.write("# 文档模板库\n\n")
        f.write("## 架构设计文档模板\n```markdown\n")
        f.write(architecture_template)
        f.write("\n```\n\n")
        f.write("## 系统设计文档模板\n```markdown\n")
        f.write(design_template)
        f.write("\n```\n\n")
        f.write("## API参考文档模板\n```markdown\n")
        f.write(api_template)
        f.write("\n```\n")
    
    print("文档模板已生成:")
    print("  - 架构设计文档模板")
    print("  - 系统设计文档模板")
    print("  - API参考文档模板")
    print(f"\n模板已保存到: documentation_templates.md")
    
    return templates

def create_sample_documents():
    """创建样例文档"""
    print("\n\n=== 创建样例文档 ===")
    print("=" * 60)
    
    # 创建样例架构文档
    sample_architecture = """# KN-Fetch 架构设计文档

## 项目概述
KN-Fetch是一个基于AI的代码资产扫描与知识提取系统，旨在自动化分析代码仓库并生成结构化知识图谱。

## 架构设计原则
- **模块化设计**: 各功能模块独立，便于维护和扩展
- **智能体驱动**: 基于Agent的工作流编排，支持复杂分析任务
- **LLM集成**: 深度集成大语言模型，提升分析准确性
- **可扩展性**: 支持插件机制，便于功能扩展

## 系统架构图
系统采用分层架构：
1. **数据采集层**: 文件扫描、代码解析
2. **分析处理层**: 语义提取、架构分析、业务逻辑分析
3. **智能体层**: 工作流编排、任务调度
4. **输出层**: 文档生成、报告输出

## 核心模块说明
- **FileScanner**: 项目文件扫描和分类
- **CodeParser**: 代码语法解析和元数据提取
- **LLMClient**: LLM API集成和调用管理
- **WorkflowEngine**: 智能体工作流编排
- **DocumentationAgent**: 文档自动生成

## 数据流设计
1. 输入: 代码仓库路径
2. 处理: 文件扫描 → 代码解析 → 语义提取 → 架构分析 → 文档生成
3. 输出: 结构化知识图谱和详细文档

## 技术选型说明
- **编程语言**: Python 3.8+
- **LLM提供商**: 火山引擎、OpenAI等
- **数据存储**: SQLite（本地缓存）、知识图谱
- **Web框架**: FastAPI（可选）

## 部署架构
支持本地部署和容器化部署，提供CLI和Web两种使用方式。

## 扩展性考虑
- 插件机制支持自定义分析器
- 可配置的LLM提供商
- 模块化的输出格式支持
"""
    
    # 创建样例设计文档
    sample_design = """# KN-Fetch 系统设计文档

## 系统设计概述
KN-Fetch采用微服务架构思想，将核心功能拆分为独立的智能体模块，通过工作流引擎进行编排。

## 功能模块设计
### 文件扫描模块
- 功能: 递归扫描项目文件，分类处理
- 输入: 项目路径
- 输出: 文件元数据列表

### 代码解析模块  
- 功能: 解析代码语法，提取结构信息
- 技术: 抽象语法树分析
- 输出: 代码实体元数据

### LLM集成模块
- 功能: 统一LLM API调用接口
- 支持: 多提供商、配置管理
- 特性: 异步调用、错误重试

## 数据库设计
使用SQLite作为本地缓存，存储分析结果和配置信息。

## API接口设计
提供RESTful API接口，支持:
- 项目分析任务提交
- 分析进度查询
- 结果数据获取

## 安全设计考虑
- API密钥安全管理
- 输入路径验证
- 错误信息脱敏

## 性能优化策略
- 异步任务处理
- 结果缓存机制
- 批量LLM调用

## 错误处理机制
- 分级错误处理
- 任务重试机制
- 详细错误日志
"""
    
    # 保存样例文档
    docs_dir = Path("generated_documents")
    docs_dir.mkdir(exist_ok=True)
    
    with open(docs_dir / "sample_architecture.md", "w", encoding="utf-8") as f:
        f.write(sample_architecture)
    
    with open(docs_dir / "sample_design.md", "w", encoding="utf-8") as f:
        f.write(sample_design)
    
    print("样例文档已创建:")
    print("  - sample_architecture.md (架构设计文档)")
    print("  - sample_design.md (系统设计文档)")
    print(f"\n文档已保存到: {docs_dir}/")

def main():
    """主函数"""
    print("KN-Fetch LLM深度分析文档生成测试")
    print("=" * 60)
    
    try:
        # 1. 分析项目结构
        project_structure = analyze_project_structure_for_llm()
        
        # 2. 分析代码架构
        architecture_analysis = analyze_code_architecture()
        
        # 3. 生成LLM提示词
        prompts = generate_llm_prompts(project_structure, architecture_analysis)
        
        # 4. 生成文档模板
        templates = generate_documentation_templates()
        
        # 5. 创建样例文档
        create_sample_documents()
        
        print("\n" + "=" * 60)
        print("LLM深度分析文档生成完成!")
        
        print("\n生成的文件:")
        print("  1. project_structure_for_llm.json - 项目结构数据")
        print("  2. architecture_analysis.json - 架构分析数据") 
        print("  3. llm_prompts.json - LLM分析提示词")
        print("  4. documentation_templates.md - 文档模板库")
        print("  5. generated_documents/ - 样例文档目录")
        
        print("\n下一步建议:")
        print("  1. 使用LLM API执行提示词生成详细文档")
        print("  2. 基于模板完善各类技术文档")
        print("  3. 验证文档的准确性和完整性")
        
    except Exception as e:
        print(f"文档生成过程中出现错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()