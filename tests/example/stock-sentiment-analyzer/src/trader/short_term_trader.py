"""
短线决策模块
"""

import logging
from typing import Dict, Any, List


class ShortTermTrader:
    """短线交易决策器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化交易决策器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        # 获取交易配置
        self.trader_config = config.get('trader', {})
        self.analyzer_config = config.get('analyzer', {})

        # 获取股票池
        self.stock_pool = config.get('stock_pool', [])

        # 选股标准
        self.min_volume = self.trader_config.get('min_volume', 100000000)
        self.max_volume = self.trader_config.get('max_volume', 10000000000)
        self.min_turnover = self.trader_config.get('min_turnover', 2.0)
        self.max_pe_ratio = self.trader_config.get('max_pe_ratio', 100)
        self.min_market_cap = self.trader_config.get('min_market_cap', 5000000000)

    async def decide(self, news: Dict[str, Any], aggregated_result: Dict[str, Any]) -> Dict[str, Any]:
        """短线决策

        Args:
            news: 资讯信息
            aggregated_result: 汇总结果

        Returns:
            决策结果
        """
        self.logger.info(f"开始短线决策: {news.get('title', '')}")

        # 获取汇总结果
        aggregated = aggregated_result.get('aggregated_result', {})
        impact_level = aggregated.get('impact_level', '微影响')
        impact_direction = aggregated.get('impact_direction', '中性')
        affected_sectors = aggregated.get('affected_sectors', [])
        confidence = aggregated.get('confidence', 0.5)

        # 定位受影响的概念
        affected_concepts = self._identify_concepts(news, affected_sectors)

        # 筛选个股
        stock_analysis = self._filter_stocks(
            affected_concepts,
            impact_level,
            impact_direction,
            confidence
        )

        # 构建结果
        result = {
            'news_id': aggregated_result.get('news_id'),
            'news_title': news.get('title', ''),
            'affected_concepts': affected_concepts,
            'stock_analysis': stock_analysis
        }

        self.logger.info(f"短线决策完成: {news.get('title', '')}")
        return result

    def _identify_concepts(self, news: Dict[str, Any], affected_sectors: List[str]) -> List[Dict[str, Any]]:
        """识别受影响的概念

        Args:
            news: 资讯信息
            affected_sectors: 受影响的板块

        Returns:
            概念列表
        """
        concepts = []

        # 从资讯中提取板块信息
        news_sectors = news.get('sector', [])

        # 合并板块
        all_sectors = list(set(news_sectors + affected_sectors))

        # 为每个板块生成概念信息
        for sector in all_sectors:
            # 简单判断影响等级和方向
            # 实际应用中应该根据资讯内容进行更精细的判断

            content = news.get('content', '').lower()
            title = news.get('title', '').lower()
            combined = content + title

            impact_level = '中影响'
            impact_direction = '中性'
            short_term_probability = '中'

            if '利好' in combined or '支持' in combined or '降准' in combined:
                impact_direction = '利好'
                short_term_probability = '高'
            elif '利空' in combined or '限制' in combined:
                impact_direction = '利空'
                short_term_probability = '高'

            if '强' in combined or '重大' in combined:
                impact_level = '强影响'
                short_term_probability = '高'

            concepts.append({
                'concept': sector,
                'impact_level': impact_level,
                'impact_direction': impact_direction,
                'short_term_probability': short_term_probability
            })

        return concepts

    def _filter_stocks(
        self,
        concepts: List[Dict[str, Any]],
        impact_level: str,
        impact_direction: str,
        confidence: float
    ) -> List[Dict[str, Any]]:
        """筛选个股

        Args:
            concepts: 受影响的概念
            impact_level: 影响等级
            impact_direction: 影响方向
            confidence: 置信度

        Returns:
            个股分析列表
        """
        stock_analysis = []

        # 如果没有受影响的概念，返回空列表
        if not concepts:
            return stock_analysis

        # 获取所有受影响的板块
        affected_sectors = [c['concept'] for c in concepts]

        # 从股票池中筛选符合条件的个股
        for stock in self.stock_pool:
            stock_sector = stock.get('sector', '')

            # 检查是否在受影响的板块中
            if not any(sector in stock_sector for sector in affected_sectors):
                continue

            # 模拟个股数据（实际应用中应该从数据源获取）
            stock_data = self._get_stock_data(stock)

            # 基本面筛选面筛选
            if not self._check_fundamental(stock_data):
                continue

            # 计算操作建议
            operation = self._determine_operation(
                impact_level,
                impact_direction,
                confidence,
                stock_data
            )

            # 计算上涨概率
            up_probability = self._calculate_up_probability(
                impact_level,
                impact_direction,
                confidence,
                stock_data
            )

            # 计算目标价和止损价
            current_price = stock_data.get('current_price', 0)
            target_price_range, stop_loss_price = self._calculate_prices(
                current_price,
                impact_level,
                operation
            )

            # 生成个股分析
            analysis = {
                'code': stock.get('code'),
                'name': stock.get('name'),
                'sector': stock.get('sector'),
                'current_price': current_price,
                'impact_direction': impact_direction,
                'up_probability': up_probability,
                'target_price_range': target_price_range,
                'stop_loss_price': stop_loss_price,
                'operation': operation,
                'reason': self._generate_reason(impact_level, impact_direction, stock_data),
                'risk_level': self._assess_risk_level(impact_level, stock_data)
            }

            stock_analysis.append(analysis)

        # 限制推荐数量
        if len(stock_analysis) > 5:
            stock_analysis = stock_analysis[:5]

        return stock_analysis

    def _get_stock_data(self, stock: Dict[str, Any]) -> Dict[str, Any]:
        """获取个股数据（模拟）

        Args:
            stock: 股票信息

        Returns:
            个股数据
        """
        # 模拟数据，实际应用中应该从数据源获取
        code = stock.get('code', '')

        # 根据股票代码生成模拟数据
        mock_data = {
            '600036': {'current_price': 33.50, 'volume': 500000000, 'turnover': 3.5, 'pe_ratio': 8.5, 'market_cap': 800000000000},
            '000858': {'current_price': 178.00, 'volume': 300000000, 'turnover': 1.8, 'pe_ratio': 25.0, 'market_cap': 700000000000},
            '600519': {'current_price': 1800.00, 'volume': 200000000, 'turnover': 1.5, 'pe_ratio': 30.0, 'market_cap': 2200000000000},
            '000001': {'current_price': 12.50, 'volume': 400000000, 'turnover': 2.8, 'pe_ratio': 6.5, 'market_cap': 250000000000},
            '600030': {'current_price': 25.00, 'volume': 350000000, 'turnover': 3.2, 'pe_ratio': 15.0, 'market_cap': 180000000000}
        }

        return mock_data.get(code, {
            'current_price': 100.00,
            'volume': 300000000,
            'turnover': 2.5,
            'pe_ratio': 20.0,
            'market_cap': 500000000000
        })

    def _check_fundamental(self, stock_data: Dict[str, Any]) -> bool:
        """基本面筛选

        Args:
            stock_data: 个股数据

        Returns:
            是否符合条件
        """
        volume = stock_data.get('volume', 0)
        turnover = stock_data.get('turnover', 0)
        pe_ratio = stock_data.get('pe_ratio', 0)
        market_cap = stock_data.get('market_cap', 0)

        # 检查成交额
        if volume < self.min_volume or volume > self.max_volume:
            return False

        # 检查换手率
        if turnover < self.min_turnover:
            return False

        # 检查市盈率
        if pe_ratio > self.max_pe_ratio:
            return False

        # 检查市值
        if market_cap < self.min_market_cap:
            return False

        return True

    def _determine_operation(
        self,
        impact_level: str,
        impact_direction: str,
        confidence: float,
        stock_data: Dict[str, Any]
    ) -> str:
        """确定操作建议

        Args:
            impact_level: 影响等级
            impact_direction: 影响方向
            confidence: 置信度
            stock_data: 个股数据

        Returns:
            操作建议
        """
        # 买入条件
        if (impact_direction == '利好' and
            impact_level in ['中影响', '强影响'] and
            confidence > 0.6):
            return '买入'

        # 卖出条件
        if (impact_direction == '利空' and
            impact_level in ['中影响', '强影响']):
            return '卖出'

        # 其他情况观望
        return '观望'

    def _calculate_up_probability(
        self,
        impact_level: str,
        impact_direction: str,
        confidence: float,
        stock_data: Dict[str, Any]
    ) -> float:
        """计算上涨概率

        Args:
            impact_level: 影响等级
            impact_direction: 影响方向
            confidence: 置信度
            stock_data: 个股数据

        Returns:
            上涨概率
        """
        # 基础概率
        base_probability = 0.50

        # 影响系数
        impact_coefficient = 1.0
        if impact_direction == '利好':
            if impact_level == '强影响':
                impact_coefficient = 1.3
            elif impact_level == '中影响':
                impact_coefficient = 1.2
            else:
                impact_coefficient = 1.1
        elif impact_direction == '利空':
            impact_coefficient = 0.7

        # 技术系数（简化处理）
        technical_coefficient = 1.0

        # 资金系数（简化处理）
        capital_coefficient = 1.0

        # 计算上涨概率
        up_probability = base_probability * impact_coefficient * technical_coefficient * capital_coefficient

        # 限制在0-1之间
        up_probability = max(0.0, min(1.0, up_probability))

        return round(up_probability, 2)

    def _calculate_prices(
        self,
        current_price: float,
        impact_level: str,
        operation: str
    ) -> tuple:
        """计算目标价和止损价

        Args:
            current_price: 当前价格
            impact_level: 影响等级
            operation: 操作建议

        Returns:
            (目标价区间, 止损价)
        """
        if operation == '观望':
            return (f"{current_price}-{current_price * 1.02:.2f}", current_price * 0.95)

        # 计算涨幅预期
        if impact_level == '强影响':
            target_increase = 0.10
        elif impact_level == '中影响':
            target_increase = 0.05
        else:
            target_increase = 0.03

        # 计算目标价
        target_price_high = current_price * (1 + target_increase)
        target_price_low = current_price * (1 + target_increase * 0.5)

        # 计算止损价
        stop_loss_price = current_price * 0.95

        return (f"{target_price_low:.2f}-{target_price_high:.2f}", round(stop_loss_price, 2))

    def _generate_reason(
        self,
        impact_level: str,
        impact_direction: str,
        stock_data: Dict[str, Any]
    ) -> str:
        """生成操作理由

        Args:
            impact_level: 影响等级
            impact_direction: 影响方向
            stock_data: 个股数据

        Returns:
            操作理由
        """
        reasons = []

        if impact_direction == '利好':
            reasons.append("资讯利好")
        elif impact_direction == '利空':
            reasons.append("资讯利空")

        if impact_level == '强影响':
            reasons.append("影响等级高")

        # 添加技术面理由（简化）
        reasons.append("技术面确认")

        # 添加资金面理由（简化）
        reasons.append("资金流入")

        return "，".join(reasons)

    def _assess_risk_level(self, impact_level: str, stock_data: Dict[str, Any]) -> str:
        """评估风险等级

        Args:
            impact_level: 影响等级
            stock_data: 个股数据

        Returns:
            风险等级
        """
        if impact_level == '强影响':
            return '高'
        elif impact_level == '中影响':
            return '中'
        else:
            return '低'
