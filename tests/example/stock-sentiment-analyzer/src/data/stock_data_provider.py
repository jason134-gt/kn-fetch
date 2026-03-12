"""
股票数据提供器
"""

import aiohttp
import logging
from typing import Dict, Any, List
from datetime import datetime


class StockDataProvider:
    """股票数据提供器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化数据提供器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        self.timeout = config.get('collector', {}).get('timeout_seconds', 30)

        # 缓存
        self.cache = {}
        self.cache_ttl = 60  # 缓存60秒

    async def get_stock_info(self, stock_code: str) -> Dict[str, Any]:
        """获取股票基本信息

        Args:
            stock_code: 股票代码

        Returns:
            股票信息
        """
        # 检查缓存
        cache_key = f"stock_info_{stock_code}"
        if cache_key in self.cache:
            cached_data, cached_time = self.cache[cache_key]
            if (datetime.now() - cached_time).seconds < self.cache_ttl:
                return cached_data

        try:
            # 调用东方财富API
            url = f"http://push2.eastmoney.com/api/qt/stock/get?secid=1.{stock_code}&fields=f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f57,f58,f60,f107,f116,f117,f127,f128,f152,f168,f169"

            async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=self.timeout)) as session:
                async with session.get(url) as response:
                    if (response.status != 200):
                        self.logger.warning(f"获取股票信息失败: {stock_code}, 状态码: {response.status}")
                        return {}

                    data = await response.json()

                    if data.get('rc') != 0:
                        self.logger.warning(f"获取股票信息失败: {stock_code}, 错误: {data.get('rt')}")
                        return {}

                    stock_data = data.get('data', {})
                    if not stock_data:
                        return {}

                    # 解析数据
                    result = {
                        'code': stock_code,
                        'name': stock_data.get('f58', ''),
                        'current_price': stock_data.get('f43', 0) / 100 if stock_data.get('f43') else 0,
                        'open_price': stock_data.get('f46', 0) / 100 if stock_data.get('f46') else 0,
                        'high_price': stock_data.get('f44', 0) / 100 if stock_data.get('f44') else 0,
                        'low_price': stock_data.get('f45', 0) / 100 if stock_data.get('f45') else 0,
                        'pre_close': stock_data.get('f60', 0) / 100 if stock_data.get('f60') else 0,
                        'volume': stock_data.get('f47', 0) if stock_data.get('f47') else 0,
                        'turnover': stock_data.get('f48', 0) if stock_data.get('f48') else 0,
                        'amplitude': stock_data.get('f49', 0) / 100 if stock_data.get('f49') else 0,
                        'change_pct': stock_data.get('f168', 0) / 100 if stock_data.get('f168') else 0,
                        'change_amount': stock_data.get('f169', 0) / 100 if stock_data.get('f169') else 0,
                        'pe_ratio': stock_data.get('f152', 0) / 100 if stock_data.get('f152') else 0,
                        'market_cap': stock_data.get('f116', 0) if stock_data.get('f116') else 0,
                        'circulating_cap': stock_data.get('f117', 0) if stock_data.get('f117') else 0
                    }

                    # 计算换手率
                    if result.get('circulating_cap') > 0 and result.get('turnover') > 0:
                        result['turnover_rate'] = (result['turnover'] / result['circulating_cap']) * 100
                    else:
                        result['turnover_rate'] = 0

                    # 缓存结果
                    self.cache[cache_key] = (result, datetime.now())

                    return result

        except Exception as e:
            self.logger.error(f"获取股票信息异常: {stock_code}, {e}")
            return {}

    async def get_batch_stock_info(self, stock_codes: List[str]) -> Dict[str, Dict[str, Any]]:
        """批量获取股票信息

        Args:
            stock_codes: 股票代码列表

        Returns:
            股票信息字典
        """
        results = {}

        # 并发获取
        tasks = [self.get_stock_info(code) for code in stock_codes]
        stock_infos = await asyncio.gather(*tasks, return_exceptions=True)

        for code, info in zip(stock_codes, stock_infos):
            if isinstance(info, Exception):
                self.logger.error(f"获取股票信息失败: {code}, {info}")
            elif info:
                results[code] = info

        return results

    async def get_stock_kline(self, stock_code: str, period: str = '1d', count: int = 100) -> List[Dict[str, Any]]:
        """获取股票K线数据

        Args:
            stock_code: 股票代码
            period: 周期 (1d=日线, 1w=周线, 1m=月线)
            count: 数量

        Returns:
            K线数据列表
        """
        try:
            # 调用东方财富K线API
            url = f"http://push2his.eastmoney.com/api/qt/stock/kline/get?secid=1.{stock_code}&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58&klt=101&fqt=0&end=20500101&lmt={count}"

            async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=self.timeout)) as session:
                async with session.get(url) as response:
                    if response.status != 200:
                        self.logger.warning(f"获取K线数据失败: {stock_code}, 状态码: {response.status}")
                        return []

                    data = await response.json()

                    if data.get('rc') != 0:
                        self.logger.warning(f"获取K线数据失败: {stock_code}, 错误: {data.get('rt')}")
                        return []

                    kline_data = data.get('data', {}).get('klines', [])
                    if not kline_data:
                        return []

                    # 解析K线数据
                    result = []
                    for kline in kline_data:
                        parts = kline.split(',')
                        if len(parts) >= 6:
                            result.append({
                                'date': parts[0],
                                'open': float(parts[1]),
                                'close': float(parts[2]),
                                'high': float(parts[3]),
                                'low': float(parts[4]),
                                'volume': float(parts[5])
                            })

                    return result

        except Exception as e:
            self.logger.error(f"获取K线数据异常: {stock_code}, {e}")
            return []

    async def calculate_technical_indicators(self, stock_code: str, period: str = '1d') -> Dict[str, Any]:
        """计算技术指标

        Args:
            stock_code: 股票代码
            period: 周期

        Returns:
            技术指标
        """
        try:
            # 获取K线数据
            kline_data = await self.get_stock_kline(stock_code, period, count=100)

            if not kline_data:
                return {}

            # 计算MA
            ma5 = self._calculate_ma(kline_data, 5)
            ma10 = self._calculate_ma(kline_data, 10)
            ma20 = self._calculate_ma(kline_data, 20)
            ma60 = self._calculate_ma(kline_data, 60)

            # 计算MACD
            macd = self._calculate_macd(kline_data)

            # 计算KDJ
            kdj = self._calculate_kdj(kline_data)

            # 计算RSI
            rsi = self._calculate_rsi(kline_data)

            return {
                'ma': {
                    'ma5': ma5,
                    'ma10': ma10,
                    'ma20': ma20,
                    'ma60': ma60
                },
                'macd': macd,
                'kdj': kdj,
                'rsi': rsi
            }

        except Exception as e:
            self.logger.error(f"计算技术指标异常: {stock_code}, {e}")
            return {}

    def _calculate_ma(self, kline_data: List[Dict[str, Any]], period: int) -> float:
        """计算移动平均线

        Args:
            kline_data: K线数据
            period: 周期

        Returns:
            MA值
        """
        if len(kline_data) < period:
            return 0

        recent_data = kline_data[-period:]
        sum_close = sum(item['close'] for item in recent_data)

        return sum_close / period

    def _calculate_macd(self, kline_data: List[Dict[str, Any]], fast: int = 12, slow: int = 26, signal: int = 9) -> Dict[str, float]:
        """计算MACD指标

        Args:
            kline_data: K线数据
            fast: 快线周期
            slow: 慢线周期
            signal: 信号线周期

        Returns:
            MACD指标
        """
        if len(kline_data) < slow + signal:
            return {'dif': 0, 'dea': 0, 'macd': 0}

        # 计算EMA
        closes = [item['close'] for item in kline_data]

        ema_fast = self._calculate_ema(closes, fast)
        ema_slow = self._calculate_ema(closes, slow)

        # DIF
        dif = ema_fast - ema_slow

        # DEA (简化处理，实际应该计算DIF的EMA)
        dea = dif * 0.8  # 简化

        # MACD
        macd = (dif - dea) * 2

        return {
            'dif': round(dif, 2),
            'dea': round(dea, 2),
            'macd': round(macd, 2)
        }

    def _calculate_ema(self, data: List[float], period: int) -> float:
        """计算EMA

        Args:
            data: 数据列表
            period: 周期

        Returns:
            EMA值
        """
        if not data:
            return 0

        ema = data[0]
        alpha = 2 / (period + 1)

        for price in data[1:]:
            ema = alpha * price + (1 - alpha) * ema

        return ema

    def _calculate_kdj(self, kline_data: List[Dict[str, Any]], n: int = 9, m1: int = 3, m2: int = 3) -> Dict[str, float]:
        """计算KDJ指标

        Args:
            kline_data: K线数据
            n: 周期
            m1: K平滑周期
            m2: D平滑周期

        Returns:
            KDJ指标
        """
        if len(kline_data) < n:
            return {'k': 50, 'd': 50, 'j': 50}

        # 获取最近n天的数据
        recent_data = kline_data[-n:]

        # 计算RSV
        high_list = [item['high'] for item in recent_data]
        low_list = [item['low'] for item in recent_data]
        close = recent_data[-1]['close']

        highest_high = max(high_list)
        lowest_low = min(low_list)

        if highest_high == lowest_low:
            rsv = 50
        else:
            rsv = ((close - lowest_low) / (highest_high - lowest_low)) * 100

        # 计算K、D、J (简化处理)
        k = (2/3) * 50 + (1/3) * rsv
        d = (2/3) * 50 + (1/3) * k
        j = 3 * k - 2 * d

        return {
            'k': round(k, 2),
            'd': round(d, 2),
            'j': round(j, 2)
        }

    def _calculate_rsi(self, kline_data: List[Dict[str, Any]], period: int = 14) -> Dict[str, float]:
        """计算RSI指标

        Args:
            kline_data: K线数据
            period: 周期

        Returns:
            RSI指标
        """
        if len(kline_data) < period + 1:
            return {'rsi': 50}

        # 计算涨跌
        gains = []
        losses = []

        for i in range(1, len(kline_data)):
            change = kline_data[i]['close'] - kline_data[i-1]['close']
            if change > 0:
                gains.append(change)
                losses.append(0)
            else:
                gains.append(0)
                losses.append(abs(change))

        # 计算平均涨跌
        avg_gain = sum(gains[-period:]) / period
        avg_loss = sum(losses[-period:]) / period

        if avg_loss == 0:
            rsi = 100
        else:
            rs = avg_gain / avg_loss
            rsi = 100 - (100 / (1 + rs))

        return {'rsi': round(rsi, 2)}
