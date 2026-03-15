#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
重构分析智能体运行脚本

基于知识提取智能体的输出，执行重构分析全流程
"""

import os
import sys
import asyncio
from pathlib import Path

# 设置控制台编码
os.environ['PYTHONIOENCODING'] = 'utf-8'

# 添加项目根目录
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from src.refactor import RefactoringOrchestrator


def main():
    """主入口"""
    
    # 配置路径 - 基于知识提取智能体的输出
    doc_dir = "output/extract/doc/stock_datacenter"
    knowledge_graph_path = "output/extract/knowledge_graph/stock_datacenter.json"
    output_dir = "output/refactor/stock_datacenter"
    
    print("=" * 60)
    print("重构分析智能体")
    print("=" * 60)
    print(f"知识文档目录: {doc_dir}")
    print(f"知识图谱路径: {knowledge_graph_path}")
    print(f"输出目录: {output_dir}")
    print("=" * 60)
    
    # 检查输入文件是否存在
    if not Path(doc_dir).exists():
        print(f"[ERROR] 文档目录不存在: {doc_dir}")
        print("请先运行知识提取智能体: python run_extract.py")
        return 1
    
    kg_path = Path(knowledge_graph_path)
    if not kg_path.exists():
        # 尝试其他可能的位置
        alt_paths = [
            "output/extract/knowledge_graph/stock_datacenter.json",
            "output/extract/doc/stock_datacenter/entities.json"
        ]
        for alt in alt_paths:
            if Path(alt).exists():
                knowledge_graph_path = alt
                print(f"[INFO] 使用替代路径: {knowledge_graph_path}")
                break
        else:
            print(f"[WARN] 知识图谱文件不存在，将仅使用文档目录")
            knowledge_graph_path = None
    
    # 创建编排器并执行分析
    orchestrator = RefactoringOrchestrator()
    
    try:
        report = orchestrator.run(
            doc_dir=doc_dir,
            knowledge_graph_path=knowledge_graph_path,
            output_dir=output_dir
        )
        
        print("\n" + "=" * 60)
        print("重构分析完成!")
        print("=" * 60)
        print(f"项目: {report.project_name}")
        print(f"风险分布: {report.risk_summary}")
        print(f"技术债务: {report.total_debts}项")
        print(f"重构方案: {len(report.refactoring_plans)}个")
        print(f"改进建议: {len(report.improvement_suggestions)}条")
        print("=" * 60)
        print(f"详细报告: {output_dir}/report.md")
        print("=" * 60)
        
        return 0
        
    except Exception as e:
        print(f"\n[ERROR] 执行失败: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
