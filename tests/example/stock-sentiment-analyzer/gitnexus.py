#!/usr/bin/env python3
"""
GitNexus大型项目知识提取工具
支持百万行代码级别的工程分析，具备增量分析、并行处理、
结构化知识存储、多格式导出能力
"""

import click
import json
from pathlib import Path
from src.gitnexus import GitNexusClient, ExportFormat
from src.gitnexus.exceptions import GitNexusError

@click.group()
@click.version_option(version="1.0.0")
def cli():
    """GitNexus 大型项目知识提取工具"""
    pass

@cli.command()
@click.option("--config", "-c", default=".gittnexus.yaml", help="配置文件路径")
@click.option("--force", "-f", is_flag=True, help="强制全量分析，忽略缓存")
def analyze(config, force):
    """分析项目代码"""
    try:
        client = GitNexusClient(config)
        click.echo(f"开始分析项目: {client.config['project']['name']}")
        
        graph = client.analyze_full(force=force)
        
        stats = client.get_statistics()
        click.echo("\n分析完成!")
        click.echo(f"总文件数: {stats['total_files']}")
        click.echo(f"实体数: {stats['total_entities']}")
        click.echo(f"关系数: {stats['total_relationships']}")
        click.echo(f"总代码行数: {stats['lines_of_code']}")
        
    except GitNexusError as e:
        click.echo(f"分析失败: {str(e)}", err=True)

@cli.command()
@click.option("--config", "-c", default=".gittnexus.yaml", help="配置文件路径")
@click.option("--base", "-b", help="基准提交哈希")
@click.option("--head", "-h", help="目标提交哈希")
def incremental(config, base, head):
    """增量分析项目代码"""
    try:
        client = GitNexusClient(config)
        click.echo("开始增量分析...")
        
        graph = client.analyze_incremental(base, head)
        
        stats = client.get_statistics()
        click.echo("\n增量分析完成!")
        click.echo(f"总实体数: {stats['total_entities']}")
        click.echo(f"总关系数: {stats['total_relationships']}")
        
    except GitNexusError as e:
        click.echo(f"增量分析失败: {str(e)}", err=True)

@cli.command()
@click.option("--config", "-c", default=".gittnexus.yaml", help="配置文件路径")
@click.option("--output", "-o", help="输出文件路径")
@click.option("--format", "-f", type=click.Choice([f.value for f in ExportFormat]), default="markdown", help="导出格式")
def export(config, output, format):
    """导出分析结果"""
    try:
        client = GitNexusClient(config)
        export_format = ExportFormat(format)
        
        output_path = client.export(output, export_format)
        click.echo(f"导出成功: {output_path}")
        
    except GitNexusError as e:
        click.echo(f"导出失败: {str(e)}", err=True)

@cli.command()
@click.option("--config", "-c", default=".gittnexus.yaml", help="配置文件路径")
def stats(config):
    """显示分析统计信息"""
    try:
        client = GitNexusClient(config)
        stats = client.get_statistics()
        
        click.echo("=== 项目分析统计 ===")
        click.echo(f"总文件数: {stats['total_files']}")
        click.echo(f"总代码实体数: {stats['total_entities']}")
        click.echo(f"总关系数: {stats['total_relationships']}")
        click.echo(f"总代码行数: {stats['lines_of_code']}")
        click.echo("\n实体类型分布:")
        for entity_type, count in stats['entity_types'].items():
            click.echo(f"  {entity_type}: {count}")
        
    except GitNexusError as e:
        click.echo(f"获取统计信息失败: {str(e)}", err=True)

@cli.command()
@click.option("--config", "-c", default=".gittnexus.yaml", help="配置文件路径")
def clear(config):
    """清除分析缓存"""
    try:
        client = GitNexusClient(config)
        if click.confirm("确定要清除所有分析缓存吗?"):
            client.clear_cache()
            click.echo("缓存已清除")
    except GitNexusError as e:
        click.echo(f"清除缓存失败: {str(e)}", err=True)

@cli.command()
def init():
    """初始化GitNexus配置文件"""
    config_content = """project:
  name: "你的项目名称"
  description: "项目描述"
  root_dir: "."

analysis:
  include:
    - "src/**/*.py"
    - "src/**/*.js"
    - "src/**/*.ts"
    - "*.md"
    - "requirements.txt"
    - "package.json"
  exclude:
    - "node_modules/**/*"
    - "data/**/*"
    - "logs/**/*"
    - "__pycache__/**/*"
    - "*.pyc"
    - ".git/**/*"
    - "dist/**/*"
    - "build/**/*"

parallel:
  max_workers: auto  # 自动使用CPU核心数

performance:
  batch_size: 100

storage:
  db_path: ".gitnexus_cache.db"

output:
  format: "markdown"
  path: "./docs/PROJECT_KNOWLEDGE_BASE.md"
  sections:
    - "project_overview"
    - "architecture"
    - "core_modules"
    - "api_reference"
    - "configuration"
    - "usage_guide"
    - "tech_stack"
    - "development_standards"
"""
    
    config_path = Path(".gittnexus.yaml")
    if config_path.exists():
        if not click.confirm(".gittnexus.yaml 已存在，是否覆盖?"):
            return
    
    with open(config_path, "w", encoding="utf-8") as f:
        f.write(config_content)
    
    click.echo("配置文件已生成: .gittnexus.yaml")
    click.echo("请根据你的项目需求修改配置后使用")

if __name__ == "__main__":
    cli()
