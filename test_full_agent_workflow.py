#!/usr/bin/env python3
"""
完整智能体工作流测试 - 执行LLM深度分析和完整智能体流程
生成详细设计文档、架构文档等
"""
import os
import sys
import asyncio
import json
import yaml
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

# 设置API密钥
os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'

async def test_full_analysis_workflow():
    """测试完整分析工作流"""
    print("=== 完整智能体工作流测试 ===")
    print("=" * 60)
    
    try:
        # 导入工作流引擎
        from src.core.workflow_engine import get_workflow_engine
        
        # 获取工作流引擎
        workflow_engine = get_workflow_engine()
        
        print("可用工作流:")
        for workflow in workflow_engine.list_available_workflows():
            print(f"  - {workflow}")
        
        # 使用当前项目作为分析目标
        project_path = "."
        
        print(f"\n执行完整分析工作流: {project_path}")
        
        # 执行完整分析工作流
        workflow_result = await workflow_engine.execute_workflow(
            workflow_name="full_analysis",
            input_data={
                "project_path": project_path,
                "output_dir": "test_output_full_workflow"
            }
        )
        
        # 输出工作流执行结果
        print(f"\n工作流执行状态: {workflow_result.status.value}")
        print(f"执行时间: {workflow_result.execution_time:.2f}秒")
        
        if workflow_result.status.value == "completed":
            print("\n工作流执行成功!")
            
            # 显示各步骤结果
            print("\n各步骤执行结果:")
            for step_name, step_result in workflow_result.results.items():
                print(f"  - {step_name}: 成功")
                
                # 显示关键数据
                if step_name == "file_scanning" and step_result:
                    print(f"    扫描文件数: {len(step_result) if isinstance(step_result, list) else '未知'}")
                elif step_name == "code_parsing" and step_result:
                    print(f"    解析代码实体数: {len(step_result) if isinstance(step_result, list) else '未知'}")
                elif step_name == "semantic_extraction" and step_result:
                    print(f"    提取语义特征数: {len(step_result) if isinstance(step_result, list) else '未知'}")
                elif step_name == "architecture_analysis" and step_result:
                    print(f"    架构分析结果: {len(step_result) if isinstance(step_result, dict) else '未知'} 个组件")
                elif step_name == "business_logic_analysis" and step_result:
                    print(f"    业务逻辑分析: {len(step_result) if isinstance(step_result, list) else '未知'} 个流程")
                elif step_name == "documentation_generation" and step_result:
                    print(f"    文档生成: 完成")
                    
            # 检查生成的文档
            output_dir = Path("test_output_full_workflow")
            if output_dir.exists():
                print(f"\n生成的文档文件:")
                md_files = list(output_dir.rglob("*.md"))
                for md_file in md_files:
                    relative_path = md_file.relative_to(output_dir)
                    print(f"  - {relative_path}")
                    
                # 显示主要文档内容
                if md_files:
                    print("\n主要文档内容预览:")
                    for doc_type in ["architecture", "design", "business", "api"]:
                        doc_files = list(output_dir.rglob(f"*{doc_type}*.md"))
                        if doc_files:
                            main_doc = doc_files[0]
                            print(f"\n{doc_type.upper()} 文档: {main_doc.name}")
                            try:
                                with open(main_doc, 'r', encoding='utf-8') as f:
                                    content = f.read()[:500]  # 显示前500字符
                                print(f"内容预览: {content}...")
                            except:
                                print("无法读取文档内容")
            
            return workflow_result
            
        else:
            print(f"\n工作流执行失败!")
            if workflow_result.errors:
                print("错误信息:")
                for step, error in workflow_result.errors.items():
                    print(f"  - {step}: {error}")
            return None
            
    except Exception as e:
        print(f"工作流执行失败: {e}")
        import traceback
        traceback.print_exc()
        return None

async def test_llm_deep_analysis():
    """测试LLM深度分析"""
    print("\n\n=== LLM深度分析测试 ===")
    print("=" * 60)
    
    try:
        # 导入深度知识分析器
        from src.ai.deep_knowledge_analyzer import DeepKnowledgeAnalyzer
        import yaml
        
        # 加载配置
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        # 创建深度分析器
        analyzer = DeepKnowledgeAnalyzer(config, "test_llm_deep_analysis")
        
        if not analyzer.is_available():
            print("深度知识分析器不可用")
            return None
        
        print("深度知识分析器可用")
        
        # 分析一个Python文件
        test_file = Path("src/agents/architecture_analyzer.py")
        
        if not test_file.exists():
            print(f"测试文件不存在: {test_file}")
            return None
        
        # 读取文件内容
        with open(test_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        print(f"分析文件: {test_file}")
        print(f"文件大小: {len(content)} 字符")
        
        # 执行LLM深度分析
        print("执行LLM深度分析...")
        
        # 分析代码实体
        entities = analyzer.analyze_code_entity(content, str(test_file))
        
        if entities:
            print(f"LLM分析成功: 提取到 {len(entities)} 个实体")
            
            # 分析实体类型分布
            entity_types = {}
            for entity in entities:
                if isinstance(entity, dict):
                    entity_type = entity.get('type', 'unknown')
                    entity_types[entity_type] = entity_types.get(entity_type, 0) + 1
            
            print("实体类型分布:")
            for entity_type, count in sorted(entity_types.items(), key=lambda x: x[1], reverse=True):
                print(f"  {entity_type}: {count}")
            
            # 显示前5个实体详情
            print("\n前5个实体详情:")
            for i, entity in enumerate(entities[:5]):
                if isinstance(entity, dict):
                    name = entity.get('name', 'Unknown')
                    entity_type = entity.get('type', 'Unknown')
                    description = entity.get('description', '')[:100]
                    print(f"  {i+1}. {name} ({entity_type})")
                    print(f"     描述: {description}...")
            
            # 保存分析结果
            with open("llm_deep_analysis_result.json", "w", encoding="utf-8") as f:
                json.dump(entities, f, ensure_ascii=False, indent=2)
            
            print(f"\nLLM深度分析结果已保存到: llm_deep_analysis_result.json")
            
            return entities
        else:
            print("LLM分析失败: 未提取到实体")
            return None
            
    except Exception as e:
        print(f"LLM深度分析失败: {e}")
        import traceback
        traceback.print_exc()
        return None

async def test_agent_orchestration():
    """测试智能体编排"""
    print("\n\n=== 智能体编排测试 ===")
    print("=" * 60)
    
    try:
        # 导入智能体
        from src.agents.architecture_analyzer import ArchitectureAnalyzerAgent
        from src.agents.business_logic import BusinessLogicAgent
        from src.agents.documentation import DocumentationAgent
        import yaml
        
        # 加载配置
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        # 测试架构分析智能体
        print("测试架构分析智能体...")
        arch_agent = ArchitectureAnalyzerAgent(config.get('ai', {}))
        
        if arch_agent.is_available():
            print("架构分析智能体可用")
            
            # 模拟分析数据
            test_data = {
                "code_metadata_list": [
                    {
                        "file_path": "src/main.py",
                        "functions": ["main", "setup", "run"],
                        "classes": ["App", "Config"],
                        "imports": ["os", "sys", "json"]
                    }
                ],
                "semantic_contracts": [
                    {
                        "type": "function",
                        "name": "main",
                        "description": "应用主入口"
                    }
                ]
            }
            
            # 执行架构分析
            result = await arch_agent.execute(test_data)
            
            if result.success:
                print("架构分析成功")
                print(f"分析结果: {len(result.data) if isinstance(result.data, dict) else '未知'} 个架构组件")
            else:
                print(f"架构分析失败: {result.error}")
        else:
            print("架构分析智能体不可用")
        
        # 测试文档生成智能体
        print("\n测试文档生成智能体...")
        doc_agent = DocumentationAgent(config.get('ai', {}))
        
        if doc_agent.is_available():
            print("文档生成智能体可用")
            
            # 模拟分析结果
            test_analysis = {
                "code_metadata": [
                    {
                        "file_path": "src/main.py",
                        "functions": ["main", "setup", "run"],
                        "classes": ["App", "Config"]
                    }
                ],
                "dependency_graph": {
                    "modules": ["main", "config", "utils"],
                    "dependencies": {
                        "main": ["config", "utils"],
                        "config": []
                    }
                },
                "business_flows": [
                    {
                        "name": "用户注册流程",
                        "steps": ["验证输入", "创建用户", "发送邮件"]
                    }
                ]
            }
            
            # 执行文档生成
            result = await doc_agent.execute({
                "analysis_results": test_analysis,
                "project_info": {
                    "name": "测试项目",
                    "version": "1.0.0"
                }
            })
            
            if result.success:
                print("文档生成成功")
                print(f"生成文档数: {len(result.data) if isinstance(result.data, dict) else '未知'}")
            else:
                print(f"文档生成失败: {result.error}")
        else:
            print("文档生成智能体不可用")
        
        return True
        
    except Exception as e:
        print(f"智能体编排测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def check_generated_documents():
    """检查生成的文档"""
    print("\n\n=== 检查生成的文档 ===")
    print("=" * 60)
    
    # 检查各种可能的输出目录
    output_dirs = [
        "test_output_full_workflow",
        "output",
        "output/knowledge_graph",
        "output/doc"
    ]
    
    for output_dir in output_dirs:
        dir_path = Path(output_dir)
        if dir_path.exists():
            print(f"\n检查目录: {output_dir}")
            
            # 查找文档文件
            md_files = list(dir_path.rglob("*.md"))
            json_files = list(dir_path.rglob("*.json"))
            
            print(f"  Markdown文档: {len(md_files)} 个")
            print(f"  JSON数据文件: {len(json_files)} 个")
            
            # 显示主要文档
            if md_files:
                print("\n  主要文档文件:")
                for md_file in md_files[:10]:  # 显示前10个
                    relative_path = md_file.relative_to(dir_path)
                    file_size = md_file.stat().st_size
                    print(f"    - {relative_path} ({file_size} 字节)")
            
            # 检查架构文档
            arch_docs = list(dir_path.rglob("*architecture*.md"))
            if arch_docs:
                print(f"\n  架构文档: {len(arch_docs)} 个")
                for doc in arch_docs:
                    print(f"    - {doc.name}")
            
            # 检查设计文档
            design_docs = list(dir_path.rglob("*design*.md"))
            if design_docs:
                print(f"\n  设计文档: {len(design_docs)} 个")
                for doc in design_docs:
                    print(f"    - {doc.name}")
            
            # 检查API文档
            api_docs = list(dir_path.rglob("*api*.md"))
            if api_docs:
                print(f"\n  API文档: {len(api_docs)} 个")
                for doc in api_docs:
                    print(f"    - {doc.name}")

async def main():
    """主函数"""
    print("KN-Fetch 完整智能体工作流测试")
    print("=" * 60)
    
    try:
        # 1. 测试完整分析工作流
        workflow_result = await test_full_analysis_workflow()
        
        # 2. 测试LLM深度分析
        llm_result = await test_llm_deep_analysis()
        
        # 3. 测试智能体编排
        agent_result = await test_agent_orchestration()
        
        # 4. 检查生成的文档
        check_generated_documents()
        
        print("\n" + "=" * 60)
        print("测试完成!")
        
        # 总结测试结果
        print("\n测试结果总结:")
        print(f"  完整工作流: {'成功' if workflow_result else '失败'}")
        print(f"  LLM深度分析: {'成功' if llm_result else '失败'}")
        print(f"  智能体编排: {'成功' if agent_result else '失败'}")
        
        # 生成建议
        print("\n建议:")
        if not workflow_result:
            print("  - 检查工作流引擎配置和Agent可用性")
        if not llm_result:
            print("  - 检查LLM配置和API密钥")
        if not agent_result:
            print("  - 检查智能体实现和依赖")
        
        print("\n下一步:")
        print("  1. 查看生成的文档文件")
        print("  2. 检查LLM分析结果")
        print("  3. 验证智能体编排效果")
        
    except Exception as e:
        print(f"测试过程中出现错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(main())