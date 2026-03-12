"""
报告生成模块
"""

import logging
import json
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, List


class ReportGenerator:
    """报告生成器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化报告生成器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        # 获取报告配置
        self.reporter_config = config.get('reporter', {})
        self.output_dir = Path(self.reporter_config.get('output_dir', './data/reports'))
        self.output_format = self.reporter_config.get('output_format', 'markdown')

        # 确保输出目录存在
        self.output_dir.mkdir(parents=True, exist_ok=True)

    async def generate(
        self,
        news_list: List[Dict[str, Any]],
        expert_results: List[Dict[str, Any]],
        aggregated_results: List[Dict[str, Any]],
        trading_decisions: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """生成分析报告

        Args:
            news_list: 资讯列表
            expert_results: 专家评估结果
            aggregated_results: 汇总结果
            trading_decisions: 交易决策

        Returns:
            报告信息
        """
        self.logger.info("开始生成报告")

        # 生成报告时间戳
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        report_id = datetime.now().strftime('%Y%m%d_%H%M%S')

        # 生成报告内容
        if self.output_format == 'markdown':
            content = self._generate_markdown(
                timestamp,
                news_list,
                expert_results,
                aggregated_results,
                trading_decisions
            )
            file_extension = '.md'
        elif self.output_format == 'json':
            content = self._generate_json(
                timestamp,
                news_list,
                expert_results,
                aggregated_results,
                trading_decisions
            )
            file_extension = '.json'
        else:
            content = self._generate_markdown(
                timestamp,
                news_list,
                expert_results,
                aggregated_results,
                trading_decisions
            )
            file_extension = '.md'

        # 保存报告
        file_name = f"report_{report_id}{file_extension}"
        file_path = self.output_dir / file_name

        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

        self.logger.info(f"报告已保存: {file_path}")

        return {
            'report_id': report_id,
            'timestamp': timestamp,
            'file_path': str(file_path),
            'format': self.output_format,
            'total_news': len(news_list),
            'valid_news': len(aggregated_results)
        }

    def _generate_markdown(
        self,
        timestamp: str,
        news_list: List[Dict[str, Any]],
        expert_results: List[Dict[str, Any]],
        aggregated_results: List[Dict[str, Any]],
        trading_decisions: List[Dict[str, Any]]
    ) -> str:
        """生成Markdown格式报告

        Args:
            timestamp: 时间戳
            news_list: 资讯列表
            expert_results: 专家评估结果
            aggregated_results: 汇总结果
            trading_decisions: 交易决策

        Returns:
            Markdown内容
        """
        lines = []

        # 标题
        lines.append("# 实时股票财经舆情分析报告")
        lines.append("")
        lines.append(f"**报告生成时间**：{timestamp}")
        lines.append(f"**采集资讯总数**：{len(news_list)}条")
        lines.append(f"**有效分析资讯**：{len(aggregated_results)}条")
        lines.append("")
        lines.append("---")
        lines.append("")

        # 一、核心资讯汇总
        lines.append("## 一、核心资讯汇总")
        lines.append("")

        for i, news in enumerate(news_list):
            lines.append(f"### 资讯{i+1}：{news.get('title', '')}")
            lines.append(f"**时间**：{news.get('timestamp', '')}")
            lines.append(f"**来源**：{news.get('source', '')}")
            lines.append(f"**链接**：{news.get('url', '')}")
            lines.append("")
            lines.append("**核心内容**：")
            lines.append(news.get('content', ''))
            lines.append("")

            # 专家评估共识
            if i < len(aggregated_results):
                aggregated = aggregated_results[i]
                result = aggregated.get('aggregated_result', {})

                lines.append("**专家圆桌评估共识**：")
                lines.append(f"- **影响等级**：{result.get('impact_level', '')}")
                lines.append(f"- **影响方向**：{result.get('impact_direction', '')}")
                lines.append(f"- **影响时长**：{result.get('impact_duration', '')}")
                lines.append(f"- **综合置信度**：{result.get('confidence', 0) * 100:.0f}%")

                # 提取共识逻辑
                consensus = aggregated.get('consensus', {})
                lines.append(f"- **核心逻辑**：{consensus.get('main_conclusion', '')}")
                lines.append("")

                # 评估分歧点
                divergence = aggregated.get('divergence', {})
                if divergence.get('has_divergence'):
                    lines.append("**评估分歧点**：")
                    lines.append(f"- **分歧类型**：{divergence.get('divergence_type', '')}")
                    for detail in divergence.get('divergence_details', []):
                        lines.append(f"- {detail}")
                    lines.append("")

            lines.append("---")
            lines.append("")

        # 二、概念/板块影响分析
        lines.append("## 二、概念/板块影响分析")
        lines.append("")
        lines.append("| 影响概念 | 影响等级 | 影响方向 | 核心驱动资讯 | 短线异动概率 |")
        lines.append("|----------|----------|----------|--------------|--------------|")

        all_concepts = {}
        for decision in trading_decisions:
            for concept in decision.get('affected_concepts', []):
                concept_name = concept.get('concept', '')
                if concept_name not in all_concepts:
                    all_concepts[concept_name] = concept

        for concept_name, concept in all_concepts.items():
            lines.append(f"| {concept_name} | {concept.get('impact_level', '')} | {concept.get('impact_direction', '')} | {decision.get('news_title', '')[:20]}... | {concept.get('short_term_probability', '')} |")

        lines.append("")
        lines.append("---")
        lines.append("")

        # 三、核心个股分析
        lines.append("## 三、核心个股分析（通达信格式）")
        lines.append("")
        lines.append("| 代码   | 名称   | 所属概念 | 影响方向 | 上涨概率 | 目标价区间 | 止损价 | 操作建议 |")
        lines.append("|--------|--------|----------|----------|----------|------------|--------|----------|")

        for decision in trading_decisions:
            for stock in decision.get('stock_analysis', []):
                lines.append(f"| {stock.get('code', '')} | {stock.get('name', '')} | {stock.get('sector', '')} | {stock.get('impact_direction', '')} | {stock.get('up_probability', 0) * 100:.0f}% | {stock.get('target_price_range', '')} | {stock.get('stop_loss_price', '')} | {stock.get('operation', '')} |")

        lines.append("")
        lines.append("---")
        lines.append("")

        # 四、短线操作建议
        lines.append("## 四、短线操作建议")
        lines.append("")

        # 优先关注个股
        lines.append("### 优先关注个股")
        lines.append("")

        count = 0
        for decision in trading_decisions:
            for stock in decision.get('stock_analysis', []):
                if stock.get('operation') == '买入' and count < 3:
                    count += 1
                    lines.append(f"{count}. **{stock.get('name', '')}（{stock.get('code', '')}）**")
                    lines.append(f"   - 逻辑：{stock.get('reason', '')}")
                    lines.append(f"   - 风险等级：{stock.get('risk_level', '')}")
                    lines.append("")

        # 风险提示
        lines.append("### 风险提示")
        lines.append("")
        lines.append("- **市场风险**：注意市场整体情绪变化，避免追高")
        lines.append("- **政策风险**：关注后续政策落地情况")
        lines.append("- **个股风险**：严格执行止损纪律，控制仓位")
        lines.append("")

        # 操作周期建议
        lines.append("### 操作周期建议")
        lines.append("")
        lines.append("**建议操作周期**：1-5个交易日")
        lines.append("**建议仓位控制**：单只股票不超过20%")
        lines.append("**止损纪律**：严格执行止损，亏损超过5%及时止损")
        lines.append("")

        lines.append("---")
        lines.append("")

        # 五、免责声明
        lines.append("## 五、免责声明")
        lines.append("")
        lines.append("本报告基于公开信息和AI分析生成，仅供参考，不构成投资建议。股市有风险，投资需谨慎。")
        lines.append("")

        return "\n".join(lines)

    def _generate_json(
        self,
        timestamp: str,
        news_list: List[Dict[str, Any]],
        expert_results: List[Dict[str, Any]],
        aggregated_results: List[Dict[str, Any]],
        trading_decisions: List[Dict[str, Any]]
    ) -> str:
        """生成JSON格式报告

        Args:
            timestamp: 时间戳
            news_list: 资讯列表
            expert_results: 专家评估结果
            aggregated_results: 汇总结果
            trading_decisions: 交易决策

        Returns:
            JSON内容
        """
        report = {
            'report_type': 'stock_sentiment_analysis',
            'timestamp': timestamp,
            'summary': {
                'total_news': len(news_list),
                'valid_news': len(aggregated_results),
                'total_stocks': sum(len(d.get('stock_analysis', [])) for d in trading_decisions)
            },
            'news_list': news_list,
            'expert_results': expert_results,
            'aggregated_results': aggregated_results,
            'trading_decisions': trading_decisions
        }

        return json.dumps(report, ensure_ascii=False, indent=2)
