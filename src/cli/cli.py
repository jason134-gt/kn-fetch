#!/usr/bin/env python3
import argparse
import os
import sys
import json
from pathlib import Path
from typing import Optional

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))

from src.core.knowledge_extractor import KnowledgeExtractor
from src.core.large_scale_processor import LargeScaleProcessor
from src.gitnexus import ExportFormat
from src.ai.knowledge_optimizer import KnowledgeOptimizer
from src.ai.self_testing import SelfTester
from src.ai.self_validation import SelfValidator

# 导入集成工具
from src.integrations import (
    create_enhanced_extractor,
    ArchAICodeExplainer,
    GitUMLIntegration,
    DiagramType
)

def main():
    parser = argparse.ArgumentParser(
        description="知识提取智能体 - 分析代码/文档目录生成结构化知识",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    
    subparsers = parser.add_subparsers(dest="command", required=True, help="可用命令")
    
    # 分析命令
    analyze_parser = subparsers.add_parser("analyze", help="分析指定目录")
    analyze_parser.add_argument("directory", help="要分析的目录路径")
    analyze_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    analyze_parser.add_argument("--no-code", action="store_true", help="不分析代码文件")
    analyze_parser.add_argument("--no-docs", action="store_true", help="不分析文档文件")
    analyze_parser.add_argument("--force", "-f", action="store_true", help="强制重新分析，忽略缓存")
    analyze_parser.add_argument("--large-scale", action="store_true", help="启用大规模处理模式（适合百万行级项目）")
    analyze_parser.add_argument("--incremental", action="store_true", help="启用增量分析")
    analyze_parser.add_argument("--resume", action="store_true", help="断点续传模式")
    analyze_parser.add_argument("--deep", "-d", action="store_true", help="启用LLM深度分析，生成详细设计文档（需要配置API Key）")
    analyze_parser.add_argument("--refactoring", "-r", action="store_true", help="启用重构分析，包括风险分级、技术债务扫描、架构诊断")
    
    # 导出命令
    export_parser = subparsers.add_parser("export", help="导出分析结果")
    export_parser.add_argument("--output", "-o", required=True, help="输出文件路径")
    export_parser.add_argument("--format", "-f", default="markdown", 
                              choices=["json", "markdown", "html", "csv", "graphml", "plantuml", "mermaid", "mindmap", "architecture"],
                              help="导出格式")
    export_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    
    # 优化命令
    optimize_parser = subparsers.add_parser("optimize", help="优化知识图谱，添加重构建议")
    optimize_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    optimize_parser.add_argument("--level", choices=["low", "medium", "high"], default="medium", help="优化级别")
    
    # 测试命令
    test_parser = subparsers.add_parser("test", help="自测试：自动生成并运行测试用例")
    test_parser.add_argument("--entity", help="指定要测试的实体名称，不指定则测试所有高复杂度实体")
    test_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    
    # 验证命令
    validate_parser = subparsers.add_parser("validate", help="自验证：验证知识提取的准确性")
    validate_parser.add_argument("source_directory", help="原始项目目录路径，用于对比验证")
    validate_parser.add_argument("--output", "-o", help="验证报告输出路径")
    validate_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    
    # 统计命令
    stats_parser = subparsers.add_parser("stats", help="查看分析统计信息")
    stats_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    
    # 架构分析命令
    arch_parser = subparsers.add_parser("architecture", help="生成架构分析报告")
    arch_parser.add_argument("--output", "-o", default="output/architecture_report.md", help="输出文件路径")
    arch_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    arch_parser.add_argument("--uml", action="store_true", help="同时导出UML图")
    
    # UML图生成命令
    uml_parser = subparsers.add_parser("uml", help="生成UML图")
    uml_parser.add_argument("--type", "-t", choices=["class", "sequence", "component", "flowchart", "all"], 
                           default="class", help="UML图类型")
    uml_parser.add_argument("--format", "-f", choices=["plantuml", "mermaid"], default="mermaid", help="输出格式")
    uml_parser.add_argument("--output", "-o", default="output/uml", help="输出目录")
    uml_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    
    # 重构建议命令
    refactor_parser = subparsers.add_parser("refactor", help="生成重构建议")
    refactor_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    refactor_parser.add_argument("--output", "-o", help="输出文件路径")
    refactor_parser.add_argument("--enhanced", "-e", action="store_true", help="使用增强版分析（ArchAI+GitUML）")
    
    # 重构分析命令（新增）
    refactoring_parser = subparsers.add_parser("refactoring", help="重构分析：风险分级、技术债务扫描、架构诊断")
    refactoring_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    refactoring_parser.add_argument("--graph", "-g", help="知识图谱JSON文件路径（可选，默认使用最近分析结果）")
    refactoring_parser.add_argument("--output", "-o", default="output/refactoring", help="输出目录")
    
    # 增强分析命令 (ArchAI CodeExplainer)
    enhanced_parser = subparsers.add_parser("enhanced", help="增强版知识提取（ArchAI+GitUML）")
    enhanced_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    enhanced_parser.add_argument("--output", "-o", default="output/enhanced_knowledge.json", help="输出文件路径")
    enhanced_parser.add_argument("--mode", "-m", choices=["full", "refactor", "doc"], default="full",
                                help="分析模式: full=完整分析, refactor=重构分析, doc=文档生成")
    
    # 实体解释命令
    explain_parser = subparsers.add_parser("explain", help="深度解释代码实体")
    explain_parser.add_argument("entity", help="实体名称或ID")
    explain_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    explain_parser.add_argument("--output", "-o", help="输出文件路径")
    
    # GitUML增强图生成命令
    gituml_parser = subparsers.add_parser("gituml", help="Git增强版UML图生成")
    gituml_parser.add_argument("--type", "-t", 
                              choices=["class", "sequence", "module", "diff", "feature", "timeline", "all"],
                              default="class", help="图表类型")
    gituml_parser.add_argument("--format", "-f", choices=["mermaid", "plantuml"], default="mermaid", help="输出格式")
    gituml_parser.add_argument("--output", "-o", default="output/gituml", help="输出目录")
    gituml_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    gituml_parser.add_argument("--commit1", help="差异图起始提交")
    gituml_parser.add_argument("--commit2", help="差异图目标提交")
    gituml_parser.add_argument("--highlight-changes", action="store_true", help="高亮最近变更")
    
    # Web UI命令
    web_parser = subparsers.add_parser("web", help="启动Web管理界面")
    web_parser.add_argument("--port", "-p", type=int, default=8892, help="Web服务器端口")
    web_parser.add_argument("--host", default="127.0.0.1", help="绑定地址")
    web_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
    
    args = parser.parse_args()
    
    if args.command == "analyze":
        handle_analyze(args)
    elif args.command == "export":
        handle_export(args)
    elif args.command == "optimize":
        handle_optimize(args)
    elif args.command == "test":
        handle_test(args)
    elif args.command == "validate":
        handle_validate(args)
    elif args.command == "stats":
        handle_stats(args)
    elif args.command == "architecture":
        handle_architecture(args)
    elif args.command == "uml":
        handle_uml(args)
    elif args.command == "refactor":
        handle_refactor(args)
    elif args.command == "refactoring":
        handle_refactoring_analysis(args)
    elif args.command == "enhanced":
        handle_enhanced(args)
    elif args.command == "explain":
        handle_explain(args)
    elif args.command == "gituml":
        handle_gituml(args)
    elif args.command == "web":
        handle_web(args)

def handle_analyze(args):
    """处理分析命令"""
    print(f"开始分析目录: {args.directory}")
    
    if args.large_scale:
        processor = LargeScaleProcessor(args.config)
        graph = processor.process_large_project(
            args.directory,
            include_code=not args.no_code,
            include_docs=not args.no_docs,
            incremental=args.incremental,
            resume=args.resume
        )
    else:
        extractor = KnowledgeExtractor(args.config)
        graph = extractor.extract_from_directory(
            args.directory,
            include_code=not args.no_code,
            include_docs=not args.no_docs,
            force=args.force,
            deep_analysis=args.deep if hasattr(args, 'deep') else False,
            refactoring_analysis=args.refactoring if hasattr(args, 'refactoring') else False
        )
    
    print(f"\n分析完成！")
    print(f"总文件数: {len(set(e.file_path for e in graph.entities.values()))}")
    print(f"实体数量: {len(graph.entities)}")
    print(f"关系数量: {len(graph.relationships)}")
    print(f"总代码行数: {sum(e.lines_of_code for e in graph.entities.values() if hasattr(e, 'lines_of_code'))}")

def handle_export(args):
    """处理导出命令"""
    from src.gitnexus import GitNexusClient
    
    client = GitNexusClient(args.config)
    
    format_map = {
        "json": ExportFormat.JSON,
        "markdown": ExportFormat.MARKDOWN,
        "html": ExportFormat.HTML,
        "csv": ExportFormat.CSV,
        "graphml": ExportFormat.GRAPHML,
        "plantuml": ExportFormat.PLANTUML,
        "mindmap": ExportFormat.MINDMAP
    }
    
    output_path = client.export(
        output_path=args.output,
        format=format_map[args.format]
    )
    
    print(f"导出成功！文件已保存到: {output_path}")

def handle_optimize(args):
    """处理优化命令"""
    optimizer = KnowledgeOptimizer(args.config)
    
    # 加载现有知识图谱
    from src.gitnexus import GitNexusClient
    client = GitNexusClient(args.config)
    graph = client._load_knowledge_graph()
    
    optimized_graph = optimizer.optimize_knowledge_graph(graph, optimization_level=args.level)
    
    # 保存优化后的图谱
    client._save_knowledge_graph(optimized_graph)
    
    print("知识图谱优化完成！已补充重构建议、复杂度分析等信息。")

def handle_test(args):
    """处理自测试命令"""
    tester = SelfTester(args.config)
    
    # 加载现有知识图谱
    from src.gitnexus import GitNexusClient
    client = GitNexusClient(args.config)
    graph = client._load_knowledge_graph()
    
    if args.entity:
        # 测试指定实体
        entity = next((e for e in graph.entities.values() if e.name == args.entity), None)
        if not entity:
            print(f"错误：未找到实体 {args.entity}")
            sys.exit(1)
        
        result = tester.test_entity(entity)
        print(f"测试结果: {'通过' if result['test_result']['passed'] else '失败'}")
        if not result['test_result']['passed']:
            print(f"错误信息: {result['test_result']['stderr']}")
            print(f"分析建议: {result['analysis']}")
    else:
        # 测试所有高复杂度实体
        high_complexity_entities = [
            e for e in graph.entities.values() 
            if e.entity_type in ["function", "method", "class"] 
            and hasattr(e, 'complexity') and e.complexity > 10
        ]
        
        print(f"找到 {len(high_complexity_entities)} 个高复杂度实体，开始测试...")
        
        results = tester.run_regression_tests(high_complexity_entities)
        
        passed = sum(1 for r in results if r['test_result']['passed'])
        print(f"\n测试完成: {passed}/{len(results)} 测试通过")
    
    tester.cleanup()

def handle_validate(args):
    """处理自验证命令"""
    validator = SelfValidator(args.config)
    
    # 加载现有知识图谱
    from src.gitnexus import GitNexusClient
    client = GitNexusClient(args.config)
    graph = client._load_knowledge_graph()
    
    validation_results = validator.validate_knowledge_graph(graph, args.source_directory)
    
    if args.output:
        report_path = validator.generate_validation_report(validation_results, args.output)
        print(f"验证报告已保存到: {report_path}")
    
    print(f"验证完成，整体准确率: {validation_results['overall_accuracy']:.2%}")
    print(f"验证状态: {validation_results['overall_status']}")

def handle_stats(args):
    """处理统计命令"""
    from src.gitnexus import GitNexusClient
    
    client = GitNexusClient(args.config)
    stats = client.get_statistics()
    
    print("=== 分析统计信息 ===")
    print(f"总文件数: {stats['total_files']}")
    print(f"实体总数: {stats['total_entities']}")
    print(f"关系总数: {stats['total_relationships']}")
    print(f"总代码行数: {stats['lines_of_code']}")
    print("\n实体类型分布:")
    for entity_type, count in stats['entity_types'].items():
        print(f"  {entity_type}: {count}")

def handle_web(args):
    """处理Web UI命令"""
    import uvicorn
    from src.web.app import app
    
    print(f"启动Web管理界面，访问地址: http://{args.host}:{args.port}")
    uvicorn.run(app, host=args.host, port=args.port)


def handle_refactoring_analysis(args):
    """处理重构分析命令"""
    from src.core.refactoring_analyzer import RefactoringAnalyzer
    from src.gitnexus import GitNexusClient
    from pathlib import Path
    
    # 加载知识图谱
    if args.graph:
        with open(args.graph, "r", encoding="utf-8") as f:
            graph_data = json.load(f)
            graph = KnowledgeGraph(**graph_data)
        print(f"从 {args.graph} 加载知识图谱...")
    else:
        client = GitNexusClient(args.config)
        graph = client._load_knowledge_graph()
        if not graph or not graph.entities:
            print("错误：暂无知识图谱数据，请先运行分析")
            sys.exit(1)
        print("使用缓存的知识图谱...")
    
    print(f"\n开始重构分析...")
    print(f"实体数量: {len(graph.entities)}")
    print(f"关系数量: {len(graph.relationships)}")
    
    # 执行重构分析
    config = {}
    try:
        import yaml
        with open(args.config, "r", encoding="utf-8") as f:
            config = yaml.safe_load(f)
    except:
        pass
    
    analyzer = RefactoringAnalyzer(graph, config)
    results = analyzer.analyze_full()
    
    # 保存结果
    output_dir = Path(args.output)
    analyzer.save_analysis_report(results, str(output_dir))
    
    # 打印摘要
    print("\n" + "="*60)
    print("重构分析完成！")
    print("="*60)
    
    # 风险分级统计
    risk = results.get("risk_assessment", {})
    print("\n【风险分级统计】")
    for level in ["P0", "P1", "P2", "P3"]:
        count = len(risk.get(level, []))
        if level == "P0":
            print(f"  P0 核心模块: {count} 个（禁止AI全自动重构）")
        elif level == "P1":
            print(f"  P1 重要模块: {count} 个（需人工审核核心逻辑）")
        elif level == "P2":
            print(f"  P2 通用模块: {count} 个（AI可全自动重构）")
        else:
            print(f"  P3 边缘模块: {count} 个（可直接评估下线）")
    
    # 技术债务统计
    debts = results.get("technical_debts", [])
    print(f"\n【技术债务】共 {len(debts)} 项")
    
    # 架构诊断
    arch = results.get("architecture_diagnosis")
    if arch:
        print(f"\n【架构诊断】")
        print(f"  架构风格: {arch.architecture_style}")
        print(f"  架构问题: {len(arch.issues)} 项")
        print(f"  循环依赖: {len(arch.cyclic_dependencies)} 个")
    
    print(f"\n详细报告已保存到: {output_dir}")


def handle_architecture(args):
    """处理架构分析命令"""
    from src.gitnexus import GitNexusClient
    from src.core.architecture_reporter import ArchitectureReporter
    
    client = GitNexusClient(args.config)
    graph = client._load_knowledge_graph()
    
    if not graph or not graph.entities:
        print("错误：暂无知识图谱数据，请先运行分析")
        sys.exit(1)
    
    print("正在生成架构分析报告...")
    reporter = ArchitectureReporter(graph)
    reporter.export_to_file(args.output)
    
    print(f"\n架构分析报告已保存到: {args.output}")
    
    if args.uml:
        uml_dir = Path(args.output).parent / "uml"
        print(f"UML图已保存到: {uml_dir}")


def handle_uml(args):
    """处理UML图生成命令"""
    from src.gitnexus import GitNexusClient
    from src.core.architecture_analyzer import ArchitectureAnalyzer
    from src.core.uml_generator import UMLGenerator
    
    client = GitNexusClient(args.config)
    graph = client._load_knowledge_graph()
    
    if not graph or not graph.entities:
        print("错误：暂无知识图谱数据，请先运行分析")
        sys.exit(1)
    
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # 分析架构
    print("正在分析架构...")
    analyzer = ArchitectureAnalyzer(graph)
    architecture = analyzer.analyze_full()
    
    # 生成UML图
    generator = UMLGenerator(graph, architecture)
    
    if args.type == "all" or args.type == "class":
        print("生成类图...")
        diagram = generator.generate_class_diagram(args.format)
        ext = ".puml" if args.format == "plantuml" else ".md"
        with open(output_dir / f"class_diagram{ext}", "w", encoding="utf-8") as f:
            if args.format == "mermaid":
                f.write(f"```mermaid\n{diagram}\n```")
            else:
                f.write(diagram)
    
    if args.type == "all" or args.type == "sequence":
        print("生成时序图...")
        if architecture.call_chains:
            for i, chain in enumerate(architecture.call_chains[:5]):
                diagram = generator.generate_sequence_diagram(chain, args.format)
                ext = ".puml" if args.format == "plantuml" else ".md"
                with open(output_dir / f"sequence_diagram_{i}{ext}", "w", encoding="utf-8") as f:
                    if args.format == "mermaid":
                        f.write(f"```mermaid\n{diagram}\n```")
                    else:
                        f.write(diagram)
    
    if args.type == "all" or args.type == "component":
        print("生成组件图...")
        diagram = generator.generate_component_diagram(args.format)
        ext = ".puml" if args.format == "plantuml" else ".md"
        with open(output_dir / f"component_diagram{ext}", "w", encoding="utf-8") as f:
            if args.format == "mermaid":
                f.write(f"```mermaid\n{diagram}\n```")
            else:
                f.write(diagram)
    
    if args.type == "all" or args.type == "flowchart":
        print("生成流程图...")
        diagram = generator.generate_flowchart("call", args.format)
        ext = ".puml" if args.format == "plantuml" else ".md"
        with open(output_dir / f"call_flowchart{ext}", "w", encoding="utf-8") as f:
            if args.format == "mermaid":
                f.write(f"```mermaid\n{diagram}\n```")
            else:
                f.write(diagram)
    
    print(f"\nUML图已生成到: {output_dir}")


def handle_refactor(args):
    """处理重构建议命令"""
    from src.gitnexus import GitNexusClient
    from src.core.architecture_analyzer import ArchitectureAnalyzer
    from src.gitnexus.models import EntityType
    from pathlib import Path
    
    client = GitNexusClient(args.config)
    graph = client._load_knowledge_graph()
    
    if not graph or not graph.entities:
        print("错误：暂无知识图谱数据，请先运行分析")
        sys.exit(1)
    
    # 使用增强版分析
    if args.enhanced:
        print("正在使用 ArchAI CodeExplainer 进行增强版重构分析...")
        extractor = create_enhanced_extractor(graph, client.repo_path if hasattr(client, 'repo_path') else ".")
        result = extractor.analyze_for_refactoring()
        
        output = []
        output.append("# 增强版重构建议报告\n\n")
        output.append(f"生成时间: {graph.generated_at}\n\n")
        output.append("使用工具: ArchAI CodeExplainer + GitUML\n\n")
        
        # 重构热点
        output.append("## 🔥 重构热点\n\n")
        for idx, hotspot in enumerate(result["hotspots"][:10], 1):
            output.append(f"### {idx}. {hotspot['entity_name']}\n")
            output.append(f"- **类型**: {hotspot['entity_type']}\n")
            output.append(f"- **文件**: `{hotspot['file_path']}`\n")
            output.append(f"- **热点分数**: {hotspot['hotspot_score']}\n")
            output.append(f"- **优先级**: {hotspot['refactoring_priority']}\n")
            output.append(f"- **原因**: {', '.join(hotspot['reasons'])}\n\n")
        
        # 架构问题
        output.append("## 🏗️ 架构问题\n\n")
        for issue in result["architecture_issues"][:10]:
            output.append(f"- **{issue['type']}** ({issue['severity']}): {issue['description']}\n")
            output.append(f"  - 建议: {issue['suggestion']}\n")
        
        # 代码异味汇总
        output.append("\n## 👃 代码异味统计\n\n")
        for smell_type, info in result["code_smells_summary"].items():
            output.append(f"- **{smell_type}**: {info['count']} 处 (高严重: {info['high_severity']})\n")
        
        # 优先级项目
        output.append("\n## ⚡ 优先处理项目\n\n")
        for item in result["priority_items"][:10]:
            output.append(f"- **{item['entity_name']}** [{item['priority']}]: {item['reason']}\n")
        
        # 建议
        output.append("\n## 💡 综合建议\n\n")
        for rec in result["recommendations"]:
            output.append(f"### {rec['title']}\n")
            output.append(f"{rec['description']}\n\n")
            for action in rec.get('actions', []):
                output.append(f"- {action}\n")
            output.append("\n")
        
        report = "".join(output)
        
        if args.output:
            output_path = Path(args.output)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            with open(output_path, "w", encoding="utf-8") as f:
                f.write(report)
            print(f"增强版重构建议报告已保存到: {args.output}")
        else:
            print(report)
        return
    
    # 原有逻辑
    print("正在分析重构建议...")
    
    analyzer = ArchitectureAnalyzer(graph)
    stats = analyzer.generate_statistics()
    architecture = analyzer.analyze_full()
    
    suggestions = []
    
    # 高复杂度实体
    high_complexity = [
        e for e in graph.entities.values()
        if e.complexity and e.complexity > 15
    ]
    for entity in high_complexity:
        suggestions.append({
            "type": "高复杂度",
            "severity": "高",
            "entity": entity.name,
            "file": entity.file_path,
            "line": entity.start_line,
            "detail": f"圈复杂度 {entity.complexity}",
            "suggestion": "考虑拆分此函数或使用设计模式降低复杂度"
        })
    
    # 过长函数
    long_functions = [
        e for e in graph.entities.values()
        if e.entity_type in [EntityType.FUNCTION, EntityType.METHOD]
        and e.lines_of_code > 50
    ]
    for entity in long_functions:
        suggestions.append({
            "type": "过长函数",
            "severity": "中",
            "entity": entity.name,
            "file": entity.file_path,
            "line": entity.start_line,
            "detail": f"函数长度 {entity.lines_of_code} 行",
            "suggestion": "考虑将此函数拆分为多个更小的函数"
        })
    
    # 热点实体
    for entity_info in stats.get('most_called_entities', [])[:5]:
        if entity_info['calls'] > 10:
            entity = graph.entities.get(entity_info['id'])
            if entity:
                suggestions.append({
                    "type": "热点实体",
                    "severity": "中",
                    "entity": entity.name,
                    "file": entity.file_path,
                    "line": entity.start_line,
                    "detail": f"被调用 {entity_info['calls']} 次",
                    "suggestion": "高频调用的实体，考虑性能优化或接口优化"
                })
    
    # 输出结果
    output = []
    output.append("# 重构建议报告\n")
    output.append(f"生成时间: {graph.generated_at}\n\n")
    output.append(f"总建议数: {len(suggestions)}\n\n")
    
    output.append("## 高优先级\n\n")
    high_priority = [s for s in suggestions if s['severity'] == '高']
    for s in high_priority:
        output.append(f"### {s['entity']}\n")
        output.append(f"- **类型**: {s['type']}\n")
        output.append(f"- **位置**: `{s['file']}:{s['line']}`\n")
        output.append(f"- **详情**: {s['detail']}\n")
        output.append(f"- **建议**: {s['suggestion']}\n\n")
    
    output.append("## 中优先级\n\n")
    medium_priority = [s for s in suggestions if s['severity'] == '中']
    for s in medium_priority:
        output.append(f"- **{s['entity']}** ({s['type']}): {s['detail']} - {s['suggestion']}\n")
    
    report = "".join(output)
    
    if args.output:
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(report)
        print(f"重构建议报告已保存到: {args.output}")
    else:
        print(report)


def handle_enhanced(args):
    """处理增强版知识提取命令"""
    from src.gitnexus import GitNexusClient
    
    client = GitNexusClient(args.config)
    graph = client._load_knowledge_graph()
    
    if not graph or not graph.entities:
        print("错误：暂无知识图谱数据，请先运行分析")
        sys.exit(1)
    
    print("正在使用 ArchAI CodeExplainer + GitUML 进行增强版知识提取...")
    
    extractor = create_enhanced_extractor(graph, client.repo_path if hasattr(client, 'repo_path') else ".")
    
    if args.mode == "refactor":
        result = extractor.analyze_for_refactoring()
    elif args.mode == "doc":
        doc = extractor.generate_documentation(args.output.replace('.json', '.md') if args.output else None)
        print(f"\n文档已生成")
        return
    else:
        result = extractor.extract_enhanced_knowledge()
    
    # 保存结果
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2, default=str)
    
    print(f"\n增强版知识提取完成！")
    print(f"结果已保存到: {args.output}")
    print(f"包含:")
    print(f"  - 架构分析: {len(result.get('architecture', {}))} 项")
    print(f"  - 代码解释: {len(result.get('code_explanations', {}))} 个实体")
    print(f"  - UML图表: {len(result.get('uml_diagrams', {}))} 张")


def handle_explain(args):
    """处理实体解释命令"""
    from src.gitnexus import GitNexusClient
    
    client = GitNexusClient(args.config)
    graph = client._load_knowledge_graph()
    
    if not graph or not graph.entities:
        print("错误：暂无知识图谱数据，请先运行分析")
        sys.exit(1)
    
    # 查找实体
    entity = None
    for e in graph.entities.values():
        if e.name == args.entity or e.id == args.entity:
            entity = e
            break
    
    if not entity:
        print(f"错误：未找到实体 '{args.entity}'")
        sys.exit(1)
    
    print(f"正在深度解释实体: {entity.name}...")
    
    extractor = create_enhanced_extractor(graph, client.repo_path if hasattr(client, 'repo_path') else ".")
    result = extractor.get_entity_deep_analysis(entity.id)
    
    # 格式化输出
    output = []
    output.append(f"# 实体深度分析: {entity.name}\n\n")
    
    # 基本信息
    output.append("## 基本信息\n\n")
    output.append(f"- **类型**: {result['entity_info']['type']}\n")
    output.append(f"- **文件**: `{result['entity_info']['file_path']}`\n")
    output.append(f"- **代码行数**: {result['entity_info']['lines_of_code']}\n")
    output.append(f"- **复杂度**: {result['entity_info']['complexity'] or '未计算'}\n\n")
    
    # 代码解释
    if result.get('explanation'):
        exp = result['explanation']
        output.append("## 代码解释\n\n")
        output.append(f"### 目的\n{exp.get('purpose', 'N/A')}\n\n")
        output.append(f"### 功能\n{exp.get('functionality', 'N/A')}\n\n")
        output.append(f"### 业务逻辑\n{exp.get('business_logic', 'N/A')}\n\n")
        output.append(f"### 开发者意图\n{exp.get('intent', 'N/A')}\n\n")
        
        if exp.get('code_smells'):
            output.append("### 代码异味\n\n")
            for smell in exp['code_smells']:
                output.append(f"- **{smell['type']}** ({smell['severity']}): {smell['description']}\n")
            output.append("\n")
        
        if exp.get('refactoring_suggestions'):
            output.append("### 重构建议\n\n")
            for sug in exp['refactoring_suggestions']:
                output.append(f"- {sug.get('description', sug.get('suggestion', ''))}\n")
            output.append("\n")
    
    # 关系
    output.append("## 关系分析\n\n")
    if result['relationships']['calls']:
        output.append("### 调用\n\n")
        for call in result['relationships']['calls'][:10]:
            output.append(f"- `{call['target_name']}`")
            if call.get('call_site'):
                output.append(f" @ {call['call_site']}")
            output.append("\n")
        output.append("\n")
    
    if result['relationships']['called_by']:
        output.append("### 被调用\n\n")
        for caller in result['relationships']['called_by'][:10]:
            output.append(f"- `{caller['source_name']}`\n")
        output.append("\n")
    
    # UML片段
    if result.get('uml_snippet'):
        output.append("## UML 图示\n\n")
        output.append(f"```mermaid\n{result['uml_snippet']}\n```\n")
    
    report = "".join(output)
    
    if args.output:
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(report)
        print(f"实体分析报告已保存到: {args.output}")
    else:
        print(report)


def handle_gituml(args):
    """处理GitUML增强图生成命令"""
    from src.gitnexus import GitNexusClient
    from src.core.architecture_analyzer import ArchitectureAnalyzer
    
    client = GitNexusClient(args.config)
    graph = client._load_knowledge_graph()
    
    if not graph or not graph.entities:
        print("错误：暂无知识图谱数据，请先运行分析")
        sys.exit(1)
    
    # 分析架构
    print("正在分析架构...")
    analyzer = ArchitectureAnalyzer(graph)
    architecture = analyzer.analyze_full()
    
    # 初始化GitUML
    gituml = GitUMLIntegration(graph, client.repo_path if hasattr(client, 'repo_path') else ".", architecture)
    
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    ext = ".puml" if args.format == "plantuml" else ".md"
    
    def save_diagram(diagram, name):
        """保存图表"""
        file_path = output_dir / f"{name}{ext}"
        with open(file_path, "w", encoding="utf-8") as f:
            if args.format == "mermaid":
                f.write(f"```mermaid\n{diagram.content}\n```")
            else:
                f.write(diagram.content)
        print(f"  - {name}: {diagram.title}")
    
    print("正在生成GitUML图表...")
    
    if args.type in ["class", "all"]:
        print("\n生成类图...")
        diagram = gituml.generate_class_diagram_enhanced(
            format=args.format,
            highlight_recent_changes=args.highlight_changes
        )
        save_diagram(diagram, "class_diagram")
    
    if args.type in ["module", "all"]:
        print("\n生成模块依赖图...")
        diagram = gituml.generate_module_dependency_diagram(format=args.format)
        save_diagram(diagram, "module_dependency")
    
    if args.type in ["sequence", "all"]:
        print("\n生成时序图...")
        if architecture.call_chains:
            diagram = gituml.generate_sequence_diagram_enhanced(
                architecture.call_chains[0],
                format=args.format
            )
            save_diagram(diagram, "sequence_diagram")
        else:
            print("  - 无调用链数据，跳过时序图生成")
    
    if args.type in ["feature", "all"]:
        print("\n生成功能模块图...")
        if architecture.features:
            first_feature = list(architecture.features.keys())[0]
            diagram = gituml.generate_feature_diagram(first_feature, format=args.format)
            save_diagram(diagram, "feature_diagram")
        else:
            print("  - 无功能模块数据，跳过功能模块图生成")
    
    if args.type in ["timeline", "all"]:
        print("\n生成Git历史时间线...")
        diagram = gituml.generate_git_history_timeline(format=args.format)
        save_diagram(diagram, "git_timeline")
    
    if args.type == "diff" and args.commit1 and args.commit2:
        print(f"\n生成差异图: {args.commit1}..{args.commit2}...")
        diagram = gituml.generate_diff_diagram(args.commit1, args.commit2, format=args.format)
        save_diagram(diagram, "diff_diagram")
    elif args.type == "diff":
        print("  - 差异图需要指定 --commit1 和 --commit2 参数")
    
    print(f"\nGitUML图表已生成到: {output_dir}")


if __name__ == "__main__":
    main()
