#!/usr/bin/env python3
"""
股票舆情分析系统 - 主程序
"""

import asyncio
import logging
from pathlib import Path
from datetime import datetime

from src.collectors.news_collector import NewsCollector
from src.collectors.eastmoney_collector import EastMoneyCollector
from src.collectors.cls_collector import ClsCollector
from src.collectors.xueqiu_collector import XueqiuCollector
from src.experts.expert_panel import ExpertPanel
from src.aggregator.result_aggregator import ResultAggregator
from src.trader.short_term_trader import ShortTermTrader
from src.reporter.report_generator import ReportGenerator
from src.scheduler.task_scheduler import TaskScheduler
from src.ai.ai_client import create_ai_client
from src.data.integrated_data_provider import IntegratedDataProvider
from src.utils.config_loader import ConfigLoader
from src.utils.logger import setup_logger


class StockSentimentAnalyzer:
    """股票舆情分析系统主类"""

    def __init__(self, config_path: str = "config/config.yaml"):
        """初始化分析系统

        Args:
            config_path: 配置文件路径
        """
        # 加载配置
        self.config = ConfigLoader.load(config_path)

        # 设置日志
        self.logger = setup_logger(
            level=self.config.get('logging', {}).get('level', 'INFO'),
            log_file=self.config.get('logging', {}).get('file', './logs/analyzer.log')
        )

        # 初始化各模块
        self.collector = NewsCollector(self.config)
        self.eastmoney_collector = EastMoneyCollector(self.config)
        self.cls_collector = ClsCollector(self.config)
        self.xueqiu_collector = XueqiuCollector(self.config)
        self.expert_panel = ExpertPanel(self.config)
        self.aggregator = ResultAggregator(self.config)
        self.trader = ShortTermTrader(self.config)
        self.reporter = ReportGenerator(self.config)
        self.scheduler = TaskScheduler(self.config)

        # 初始化AI客户端
        self.ai_client = create_ai_client(self.config)
        if self.ai_client:
            self.logger.info("AI客户端初始化成功")
        else:
            self.logger.warning("AI客户端初始化失败，将使用规则引擎")

        # 初始化集成数据提供器（支持API和通达信数据）
        self.stock_data_provider = IntegratedDataProvider(self.config)

        self.logger.info("股票舆情分析系统初始化完成")

    async def run_once(self):
        """执行一次完整的分析流程"""
        self.logger.info("=" * 60)
        self.logger.info("开始执行分析流程")
        self.logger.info("=" * 60)

        try:
            # 1. 采集资讯
            self.logger.info("步骤1: 采集实时资讯...")

            # 使用真实数据源
            eastmoney_news = await self.eastmoney_collector.collect()
            cls_news = await self.cls_collector.collect()
            xueqiu_data = await self.xueqiu_collector.collect()

            # 合并资讯
            news_list = eastmoney_news + cls_news + xueqiu_data

            # 去重
            seen_urls = set()
            unique_news = []
            for news in news_list:
                url = news.get('url', '')
                if url and url not in seen_urls:
                    seen_urls.add(url)
                    unique_news.append(news)

            news_list = unique_news

            self.logger.info(f"采集到 {len(news_list)} 条资讯")

            if not news_list:
                self.logger.warning("未采集到有效资讯，跳过后续分析")
                return

            # 2. 专家评估
            self.logger.info("步骤2: 专家圆桌评估...")
            expert_results = []
            for news in news_list:
                result = await self.expert_panel.evaluate(news)
                expert_results.append(result)
            self.logger.info(f"完成 {len(expert_results)} 条资讯的专家评估")

            # 3. 结果汇总
            self.logger.info("步骤3: 综合汇总...")
            aggregated_results = []
            for i, news in enumerate(news_list):
                result = await self.aggregator.aggregate(news, expert_results[i])
                aggregated_results.append(result)
            self.logger.info(f"完成 {len(aggregated_results)} 条资讯的综合汇总")

            # 4. 短线决策
            self.logger.info("步骤4: 短线高手决策...")
            trading_decisions = []
            for i, news in enumerate(news_list):
                decision = await self.trader.decide(news, aggregated_results[i])
                trading_decisions.append(decision)
            self.logger.info(f"完成 {len(trading_decisions)} 条资讯的短线决策")

            # 5. 生成报告
            self.logger.info("步骤5: 生成分析报告...")
            report = await self.reporter.generate(
                news_list=news_list,
                expert_results=expert_results,
                aggregated_results=aggregated_results,
                trading_decisions=trading_decisions
            )
            self.logger.info(f"报告生成完成: {report['file_path']}")

            self.logger.info("=" * 60)
            self.logger.info("分析流程执行完成")
            self.logger.info("=" * 60)

            return report

        except Exception as e:
            self.logger.error(f"分析流程执行失败: {e}", exc_info=True)
            raise

    async def run_scheduled(self):
        """启动定时任务"""
        self.logger.info("启动定时任务调度器")

        if not self.config.get('scheduler', {}).get('enabled', False):
            self.logger.warning("定时任务未启用，请检查配置")
            return

        await self.scheduler.start(self.run_once)

    async def run_interactive(self):
        """交互式运行"""
        self.logger.info("进入交互模式")
        self.logger.info("输入 'run' 执行分析，输入 'exit' 退出")

        while True:
            try:
                cmd = input("> ").strip().lower()

                if cmd == 'exit':
                    self.logger.info("退出交互模式")
                    break
                elif cmd == 'run':
                    await self.run_once()
                else:
                    self.logger.info("未知命令，请输入 'run' 或 'exit'")

            except KeyboardInterrupt:
                self.logger.info("\n收到中断信号，退出")
                break
            except Exception as e:
                self.logger.error(f"命令执行失败: {e}")


async def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(description="股票舆情分析系统")
    parser.add_argument('--config', default='config/config.yaml', help='配置文件路径')
    parser.add_argument('--mode', choices=['once', 'scheduled', 'interactive'], default='once',
                        help='运行模式: once(单次), scheduled(定时), interactive(交互)')

    args = parser.parse_args()

    # 创建分析器实例
    analyzer = StockSentimentAnalyzer(args.config)

    # 根据模式运行
    if args.mode == 'once':
        await analyzer.run_once()
    elif args.mode == 'scheduled':
        await analyzer.run_scheduled()
    elif args.mode == 'interactive':
        await analyzer.run_interactive()


if __name__ == '__main__':
    asyncio.run(main())
