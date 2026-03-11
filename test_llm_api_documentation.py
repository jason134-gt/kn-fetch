#!/usr/bin/env python3
"""
LLM API文档生成测试 - 使用真实LLM API生成详细设计文档和架构文档
"""
import os
import sys
import json
import yaml
import asyncio
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 设置API密钥
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

async def test_llm_api_documentation():
    """测试LLM API文档生成"""
    print("=== LLM API文档生成测试 ===")
    print("=" * 60)
    
    try:
        # 导入LLM客户端
        from src.ai.llm_client import LLMClient
        
        # 加载配置
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        # 创建LLM客户端
        ai_config = config.get('ai', {})
        llm_client = LLMClient(ai_config)
        
        if not llm_client.is_available():
            print("LLM客户端不可用")
            return False
        
        print("LLM客户端可用")
        
        # 加载项目分析数据
        with open('project_structure_for_llm.json', 'r', encoding='utf-8') as f:
            project_structure = json.load(f)
        
        with open('architecture_analysis.json', 'r', encoding='utf-8') as f:
            architecture_analysis = json.load(f)
        
        # 生成架构文档
        print("\n生成架构设计文档...")
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

主要模块分析:
{chr(10).join(f"- {m['file']}: {len(m['functions'])} 函数, {len(m['classes'])} 类, {len(m['imports'])} 导入" for m in architecture_analysis['modules'][:5])}

请生成包含以下章节的详细架构设计文档：
1. 项目概述（项目背景、目标、范围）
2. 架构设计原则（设计理念、约束条件）
3. 系统架构图（分层架构、组件关系）
4. 核心模块说明（各模块功能、职责、接口）
5. 数据流设计（数据处理流程、数据模型）
6. 技术选型说明（技术栈选择理由、版本要求）
7. 部署架构（部署环境、配置要求）
8. 扩展性考虑（扩展策略、性能优化）

请使用专业的架构文档格式，确保内容详细且结构清晰，包含具体的技术实现细节。
"""
        
        # 调用LLM生成架构文档
        architecture_response = await llm_client.chat(
            system_prompt="你是一个资深软件架构师，擅长编写详细的技术架构文档",
            user_prompt=architecture_prompt,
            max_tokens=4000
        )
        
        if architecture_response:
            # 保存架构文档
            docs_dir = Path("llm_generated_documents")
            docs_dir.mkdir(exist_ok=True)
            
            with open(docs_dir / "architecture_design.md", "w", encoding="utf-8") as f:
                f.write(architecture_response)
            
            print("架构设计文档已生成")
            print(f"文档长度: {len(architecture_response)} 字符")
        else:
            print("架构文档生成失败")
        
        # 生成设计文档
        print("\n生成系统设计文档...")
        design_prompt = f"""
请基于以下项目信息生成详细的系统设计文档：

项目名称: {project_structure['project_name']}
项目类型: 代码分析工具/知识提取系统

技术栈特征:
- 主要语言: Python
- 架构模式: {architecture_analysis['architecture_style']}
- 关键功能: LLM集成、代码分析、智能体编排

系统规模:
- 总文件数: {project_structure['total_files']}
- 代码文件: {len(project_structure['code_files'])}
- 核心模块: {len(architecture_analysis['modules'])}

请生成包含以下章节的详细系统设计文档：
1. 系统设计概述（系统目标、设计理念）
2. 功能模块设计（各模块详细设计、接口定义）
3. 数据库设计（数据模型、存储方案）
4. API接口设计（接口规范、请求/响应格式）
5. 用户界面设计（界面布局、交互设计）
6. 安全设计考虑（认证授权、数据安全）
7. 性能优化策略（性能指标、优化方法）
8. 错误处理机制（错误分类、处理流程）

请确保设计文档详细、实用，包含具体的设计决策和实现建议。
"""
        
        # 调用LLM生成设计文档
        design_response = await llm_client.chat(
            system_prompt="你是一个资深系统设计师，擅长编写详细的系统设计文档",
            user_prompt=design_prompt,
            max_tokens=4000
        )
        
        if design_response:
            # 保存设计文档
            with open(docs_dir / "system_design.md", "w", encoding="utf-8") as f:
                f.write(design_response)
            
            print("系统设计文档已生成")
            print(f"文档长度: {len(design_response)} 字符")
        else:
            print("设计文档生成失败")
        
        # 生成API文档
        print("\n生成API参考文档...")
        api_prompt = f"""
请基于以下项目信息生成API参考文档：

项目名称: {project_structure['project_name']}
项目类型: 代码分析工具

主要功能模块:
{chr(10).join(f"- {m['file']}" for m in architecture_analysis['modules'][:10])}

模块功能概述:
{chr(10).join(f"- {m['file']}: {m.get('description', '无描述')[:100]}..." for m in architecture_analysis['modules'][:5])}

请生成详细的API参考文档，包含：
1. API概览（API总体介绍、使用场景）
2. 认证机制（认证方式、令牌管理）
3. 主要接口说明（各接口详细说明、参数说明）
4. 请求/响应示例（具体使用示例、错误处理）
5. 错误码说明（错误码分类、含义说明）
6. 使用限制（调用限制、配额管理）
7. 最佳实践（使用建议、性能优化）

请确保API文档格式规范，便于开发者使用，包含详细的代码示例。
"""
        
        # 调用LLM生成API文档
        api_response = await llm_client.chat(
            system_prompt="你是一个资深API文档工程师，擅长编写规范的API参考文档",
            user_prompt=api_prompt,
            max_tokens=3000
        )
        
        if api_response:
            # 保存API文档
            with open(docs_dir / "api_reference.md", "w", encoding="utf-8") as f:
                f.write(api_response)
            
            print("API参考文档已生成")
            print(f"文档长度: {len(api_response)} 字符")
        else:
            print("API文档生成失败")
        
        # 生成技术文档总结
        print("\n生成技术文档总结...")
        summary_prompt = f"""
基于以上分析，请为{project_structure['project_name']}项目生成一个技术文档总结，包含：

1. 项目技术特点总结
2. 架构设计亮点
3. 关键技术实现
4. 文档完整性评估
5. 后续改进建议

请用简洁明了的语言进行总结。
"""
        
        summary_response = await llm_client.chat(
            system_prompt="你是一个技术文档专家，擅长总结和评估技术文档",
            user_prompt=summary_prompt,
            max_tokens=1500
        )
        
        if summary_response:
            with open(docs_dir / "technical_summary.md", "w", encoding="utf-8") as f:
                f.write(summary_response)
            
            print("技术文档总结已生成")
        else:
            print("技术文档总结生成失败")
        
        # 输出生成结果
        print("\n" + "=" * 60)
        print("LLM API文档生成完成!")
        
        print(f"\n生成的文档保存在: {docs_dir}/")
        print("生成的文件:")
        
        if (docs_dir / "architecture_design.md").exists():
            file_size = (docs_dir / "architecture_design.md").stat().st_size
            print(f"  - architecture_design.md ({file_size} 字节)")
        
        if (docs_dir / "system_design.md").exists():
            file_size = (docs_dir / "system_design.md").stat().st_size
            print(f"  - system_design.md ({file_size} 字节)")
        
        if (docs_dir / "api_reference.md").exists():
            file_size = (docs_dir / "api_reference.md").stat().st_size
            print(f"  - api_reference.md ({file_size} 字节)")
        
        if (docs_dir / "technical_summary.md").exists():
            file_size = (docs_dir / "technical_summary.md").stat().st_size
            print(f"  - technical_summary.md ({file_size} 字节)")
        
        # 显示文档预览
        print("\n文档内容预览:")
        if (docs_dir / "architecture_design.md").exists():
            with open(docs_dir / "architecture_design.md", 'r', encoding='utf-8') as f:
                content = f.read()[:500]
            print("架构设计文档预览:")
            print(content + "...")
        
        return True
        
    except Exception as e:
        print(f"LLM API文档生成失败: {e}")
        import traceback
        traceback.print_exc()
        return False

async def test_llm_code_analysis():
    """测试LLM代码分析"""
    print("\n\n=== LLM代码分析测试 ===")
    print("=" * 60)
    
    try:
        # 导入深度知识分析器（简化版）
        from src.ai.llm_client import LLMClient
        
        # 加载配置
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        # 创建LLM客户端
        ai_config = config.get('ai', {})
        llm_client = LLMClient(ai_config)
        
        if not llm_client.is_available():
            print("LLM客户端不可用")
            return False
        
        print("LLM客户端可用")
        
        # 分析一个核心代码文件
        test_file = Path("src/ai/llm_client.py")
        
        if not test_file.exists():
            print(f"测试文件不存在: {test_file}")
            return False
        
        # 读取文件内容
        with open(test_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        print(f"分析文件: {test_file}")
        print(f"文件大小: {len(content)} 字符")
        
        # 使用LLM分析代码
        code_analysis_prompt = f"""
请分析以下Python代码文件，提供详细的技术分析：

文件路径: {test_file}
文件内容:
```python
{content[:2000]}  # 限制内容长度
```

请从以下角度进行分析：
1. 代码结构和组织
2. 主要类和函数的功能
3. 设计模式和架构特点
4. 代码质量和可维护性
5. 改进建议

请提供详细的分析报告。
"""
        
        code_analysis_response = await llm_client.chat(
            system_prompt="你是一个资深代码审查专家，擅长分析代码质量和架构设计",
            user_prompt=code_analysis_prompt,
            max_tokens=2000
        )
        
        if code_analysis_response:
            # 保存代码分析报告
            docs_dir = Path("llm_generated_documents")
            docs_dir.mkdir(exist_ok=True)
            
            with open(docs_dir / "code_analysis_report.md", "w", encoding="utf-8") as f:
                f.write(f"# 代码分析报告\n\n")
                f.write(f"**分析文件**: {test_file}\n")
                f.write(f"**分析时间**: {Path(__file__).stat().st_ctime}\n\n")
                f.write(code_analysis_response)
            
            print("代码分析报告已生成")
            print(f"报告长度: {len(code_analysis_response)} 字符")
            
            # 显示报告预览
            print("\n代码分析报告预览:")
            print(code_analysis_response[:500] + "...")
        else:
            print("代码分析失败")
        
        return True
        
    except Exception as e:
        print(f"LLM代码分析失败: {e}")
        import traceback
        traceback.print_exc()
        return False

async def main():
    """主函数"""
    print("KN-Fetch LLM API文档生成测试")
    print("=" * 60)
    
    try:
        # 1. 测试LLM API文档生成
        doc_result = await test_llm_api_documentation()
        
        # 2. 测试LLM代码分析
        code_result = await test_llm_code_analysis()
        
        print("\n" + "=" * 60)
        print("LLM深度分析测试完成!")
        
        print("\n测试结果:")
        print(f"  文档生成: {'成功' if doc_result else '失败'}")
        print(f"  代码分析: {'成功' if code_result else '失败'}")
        
        if doc_result or code_result:
            print("\n生成的文档:")
            docs_dir = Path("llm_generated_documents")
            if docs_dir.exists():
                md_files = list(docs_dir.rglob("*.md"))
                for md_file in md_files:
                    file_size = md_file.stat().st_size
                    print(f"  - {md_file.name} ({file_size} 字节)")
        
        print("\n下一步:")
        print("  1. 查看生成的详细文档")
        print("  2. 验证文档的准确性和完整性")
        print("  3. 基于分析结果优化项目设计")
        
    except Exception as e:
        print(f"测试过程中出现错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(main())