#!/usr/bin/env python3
"""
快速测试脚本
"""

import asyncio
import sys
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

from src.main import StockSentimentAnalyzer


async def test_system():
    """测试系统"""
    print("=" * 60)
    print("股票舆情分析系统 - 快速测试")
    print("=" * 60)
    print()

    # 检查配置文件
    config_path = "config/config.yaml"
    if not Path(config_path).exists():
        print(f"❌ 配置文件不存在: {config_path}")
        return False

    print(f"✅ 配置文件存在: {config_path}")
    print()

    # 初始化系统
    print("正在初始化系统...")
    try:
        analyzer = StockSentimentAnalyzer(config_path)
        print("✅ 系统初始化成功")
    except Exception as e:
        print(f"❌ 系统初始化失败: {e}")
        return False

    print()

    # 测试采集
    print("正在测试资讯采集...")
    try:
        news_list = await analyzer.collector.collect()
        print(f"✅ 采集测试成功，采集到 {len(news_list)} 条资讯")
    except Exception as e:
        print(f"❌ 采集测试失败: {e}")
        return False

    print()

    # 测试专家评估
    if news_list:
        print("正在测试专家评估...")
        try:
            news = news_list[0]
            expert_results = await analyzer.expert_panel.evaluate(news)
            print(f"✅ 专家评估测试成功")
            print(f"   - 评估专家数: {len(expert_results.get('evaluations', {}))}")
        except Exception as e:
            print(f"❌ 专家评估测试失败: {e}")
            return False

        print()

        # 测试汇总
        print("正在测试结果汇总...")
        try:
            aggregated_result = await analyzer.aggregator.aggregate(news, expert_results)
            print(f"✅ 结果汇总测试成功")
            print(f"   - 影响等级: {aggregated_result.get('aggregated_result', {}).get('impact_level', '')}")
            print(f"   - 影响方向: {aggregated_result.get('aggregated_result', {}).get('impact_direction', '')}")
        except Exception as e:
            print(f"❌ 结果汇总测试失败: {e}")
            return False

        print()

        # 测试决策
        print("正在测试短线决策...")
        try:
            trading_decision = await analyzer.trader.decide(news, aggregated_result)
            print(f"✅ 短线决策测试成功")
            print(f"   - 受影响概念数: {len(trading_decision.get('affected_concepts', []))}")
            print(f"   - 推荐个股数: {len(trading_decision.get('stock_analysis', []))}")
        except Exception as e:
            print(f"❌ 短线决策测试失败: {e}")
            return False

        print()

    # 测试报告生成
    print("正在测试报告生成...")
    try:
        report = await analyzer.reporter.generate(
            news_list=news_list[:1] if news_list else [],
            expert_results=[expert_results] if news_list else [],
            aggregated_results=[aggregated_result] if news_list else [],
            trading_decisions=[trading_decision] if news_list else []
        )
        print(f"✅ 报告生成测试成功")
        print(f"   - 报告路径: {report.get('file_path', '')}")
    except Exception as e:
        print(f"❌ 报告生成测试失败: {e}")
        return False

    print()
    print("=" * 60)
    print("✅ 所有测试通过！系统运行正常。")
    print("=" * 60)
    print()
    print("提示：")
    print("  - 运行完整分析: python3 src/main.py --mode once")
    print("  - 查看报告: cat data/reports/report_*.md")
    print("  - 查看日志: tail -f logs/analyzer.log")
    print()

    return True


if __name__ == '__main__':
    success = asyncio.run(test_system())
    sys.exit(0 if success else 1)
