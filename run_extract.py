#!/usr/bin/env python
"""
知识提取智能体运行脚本
输出目录: output/extract/
"""
import sys
import os
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.extract.core.knowledge_extractor import KnowledgeExtractor

def main():
    # 配置
    config = {
        "output": {
            "path": "output/extract/knowledge_graph/stock_datacenter.json",
            "doc_dir": "output/extract/doc/stock_datacenter"
        },
        "performance": {
            "max_workers": 8,
            "batch_size": 100,
            "memory_limit_gb": 8
        },
        "analysis": {
            "include_code": [
                "**/*.py", "**/*.js", "**/*.ts", "**/*.java", 
                "**/*.cpp", "**/*.go", "**/*.rs", "**/*.php"
            ],
            "include_docs": ["**/*.md", "**/*.txt", "**/*.rst"],
            "exclude": [
                "**/node_modules/**", "**/.git/**", "**/__pycache__/**",
                "**/dist/**", "**/build/**", "**/target/**", "**/venv/**"
            ]
        },
        "ai": {
            "provider": "volcengine",
            "volcengine": {
                "api_key": os.environ.get("ARK_API_KEY", "7c53500f-cf96-485d-bfe2-78db6827f926"),
                "base_url": "https://ark.cn-beijing.volces.com/api/v3",
                "model": "deepseek-v3-1-terminus",
                "max_tokens": 4000,
                "temperature": 0.1
            }
        }
    }
    
    # 创建临时配置文件
    import yaml
    temp_config_path = "config/temp_stock_datacenter.yaml"
    os.makedirs("config", exist_ok=True)
    with open(temp_config_path, "w", encoding="utf-8") as f:
        yaml.dump(config, f, allow_unicode=True)
    
    # 运行分析
    source_dir = "tests/example/stock_datacenter"
    output_dir = config["output"]["doc_dir"]
    
    print("=" * 60)
    print("Stock Datacenter 知识提取")
    print("=" * 60)
    print(f"源目录: {source_dir}")
    print(f"输出目录: {output_dir}")
    print("=" * 60)
    
    # 清理旧数据
    import shutil
    if os.path.exists(output_dir):
        print(f"清理旧输出目录: {output_dir}")
        shutil.rmtree(output_dir)
    
    # 清理缓存
    cache_file = ".gitnexus_cache_stock_datacenter.db"
    if os.path.exists(cache_file):
        os.remove(cache_file)
        print(f"清理缓存: {cache_file}")
    
    # 创建输出目录
    os.makedirs(output_dir, exist_ok=True)
    
    # 执行提取
    extractor = KnowledgeExtractor(temp_config_path)
    graph = extractor.extract_from_directory(
        source_dir,
        include_code=True,
        include_docs=False,
        force=True,
        deep_analysis=True,  # 启用LLM深度分析
        refactoring_analysis=True  # 启用重构分析
    )
    
    print("\n" + "=" * 60)
    print("知识提取完成!")
    print("=" * 60)
    print(f"总文件数: {len(set(e.file_path for e in graph.entities.values()))}")
    print(f"实体数量: {len(graph.entities)}")
    print(f"关系数量: {len(graph.relationships)}")
    print(f"总代码行数: {sum(e.lines_of_code for e in graph.entities.values() if hasattr(e, 'lines_of_code'))}")
    print(f"\n输出目录: {output_dir}")

if __name__ == "__main__":
    main()
