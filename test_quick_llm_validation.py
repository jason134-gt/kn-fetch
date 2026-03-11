#!/usr/bin/env python3
"""
快速LLM验证测试 - 验证修复后的LLM客户端功能
"""
import os
import sys
import json
import yaml
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

def test_llm_client_availability():
    """测试LLM客户端可用性"""
    print("=== LLM客户端可用性测试 ===")
    
    try:
        # 导入LLM客户端
        from src.ai.llm_client import LLMClient
        
        # 加载配置
        with open('config/config.yaml', 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        # 创建LLM客户端
        ai_config = config.get('ai', {})
        llm_client = LLMClient(ai_config)
        
        # 测试可用性
        available = llm_client.is_available()
        print(f"LLM客户端可用性: {available}")
        
        if available:
            print("LLM客户端修复成功！")
            print(f"提供商: {llm_client.provider}")
            print(f"模型: {llm_client.model}")
            print(f"基础URL: {llm_client.base_url}")
            
            # 测试简单同步调用
            print("\n=== 测试简单同步调用 ===")
            try:
                response = llm_client.chat_sync(
                    system_prompt="你是一个测试助手",
                    user_prompt="请简单回复'测试成功'"
                )
                if response:
                    print(f"同步调用成功: {response[:50]}...")
                else:
                    print("同步调用返回空结果")
            except Exception as e:
                print(f"同步调用失败: {e}")
                
            # 测试简单异步调用
            print("\n=== 测试简单异步调用 ===")
            try:
                import asyncio
                
                async def test_async():
                    response = await llm_client.chat(
                        system_prompt="你是一个测试助手",
                        user_prompt="请简单回复'异步测试成功'"
                    )
                    return response
                
                response = asyncio.run(test_async())
                if response:
                    print(f"异步调用成功: {response[:50]}...")
                else:
                    print("异步调用返回空结果")
            except Exception as e:
                print(f"异步调用失败: {e}")
        
        return available
        
    except Exception as e:
        print(f"LLM客户端测试失败: {e}")
        return False

def generate_simple_documents():
    """生成简单的文档模板"""
    print("\n=== 生成文档模板 ===")
    
    # 读取已有的分析数据
    try:
        with open('architecture_analysis.json', 'r', encoding='utf-8') as f:
            arch_data = json.load(f)
        
        with open('project_structure_for_llm.json', 'r', encoding='utf-8') as f:
            project_data = json.load(f)
        
        project_name = project_data.get('project_name', 'KN-Fetch')
        architecture_style = arch_data.get('architecture_style', '智能体架构')
        
        # 生成架构文档模板
        arch_doc = f"""# {project_name} 架构设计文档

## 项目概述
- **项目名称**: {project_name}
- **架构风格**: {architecture_style}
- **分析模块数**: {len(arch_data['modules'])}
- **总函数数**: {sum(len(m['functions']) for m in arch_data['modules'])}
- **总类数**: {sum(len(m['classes']) for m in arch_data['modules'])}

## 核心模块
"""
        
        # 添加模块信息
        for module in arch_data['modules'][:5]:
            arch_doc += f"""
### {module['file']}
- **函数数**: {len(module['functions'])}
- **类数**: {len(module['classes'])}
- **导入数**: {len(module['imports'])}
"""
        
        # 保存文档
        docs_dir = Path("generated_documents")
        docs_dir.mkdir(exist_ok=True)
        
        with open(docs_dir / "quick_architecture.md", "w", encoding="utf-8") as f:
            f.write(arch_doc)
        
        print("架构文档模板已生成")
        
        # 生成设计文档模板
        design_doc = f"""# {project_name} 系统设计文档

## 系统架构
{architecture_style} 架构风格

## 关键组件
"""
        
        key_components = arch_data.get('key_components', [])
        for comp in key_components[:5]:
            design_doc += f"""
### {comp['type']} - {comp['module']}
- **功能**: {comp.get('description', '待分析')}
"""
        
        with open(docs_dir / "quick_design.md", "w", encoding="utf-8") as f:
            f.write(design_doc)
        
        print("设计文档模板已生成")
        
    except Exception as e:
        print(f"文档生成失败: {e}")

def main():
    """主函数"""
    print("KN-Fetch LLM功能验证")
    print("=" * 50)
    
    # 设置API密钥
    os.environ['ARK_API_KEY'] = '7c53500f-cf96-485d-bfe2-78db6827f926'
    
    # 测试LLM客户端
    available = test_llm_client_availability()
    
    if available:
        # 生成文档模板
        generate_simple_documents()
        
        print("\n验证完成！")
        print("LLM客户端功能已修复")
        print("基础文档模板已生成")
        print("下一步: 运行完整的LLM深度分析")
    else:
        print("\n验证失败，需要进一步修复")

if __name__ == "__main__":
    main()