"""
通达信(TDX)本地数据提供器
用于读取通达信软件下载的本地行情数据，包括日、周、月、分钟级别数据
"""

import os
import struct
import logging
from datetime import datetime, timedelta
from typing import Dict, Any, List, Optional, Tuple
import pandas as pd
import numpy as np


class TdxDataProvider:
    """通达信数据提供器"""

    # 市场代码映射
    MARKET_CODES = {
        'SH': 'sh',  # 上海证券交易所
        'SZ': 'sz',  # 深圳证券交易所
        'BJ': 'bj',  # 北京证券交易所
    }

    # 数据类型映射
    DATA_TYPES = {
        'day': 'lday',      # 日线
        'min': 'minline',   # 分钟线
        'week': 'lweek',    # 周线
        'month': 'lmonth',  # 月线
    }

    def __init__(self, config: Dict[str, Any]):
        """初始化通达信数据提供器

        Args:
            config: 配置字典，应包含tdx_data_path
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        # 获取通达信数据路径
        self.tdx_path = config.get('tdx', {}).get('data_path', 'C:/zd_zsone')
        if not os.path.exists(self.tdx_path):
            self.logger.warning(f"通达信数据路径不存在: {self.tdx_path}")
            # 尝试使用默认路径
            default_paths = [
                'C:/zd_zsone',
                'D:/zd_zsone',
                'E:/zd_zsone',
                'C:/new_tdx',
                'D:/new_tdx',
            ]
            for path in default_paths:
                if os.path.exists(path):
                    self.tdx_path = path
                    self.logger.info(f"使用默认路径: {self.tdx_path}")
                    break

        self.vipdoc_path = os.path.join(self.tdx_path, 'vipdoc')
        self.t0002_path = os.path.join(self.tdx_path, 'T0002')

        self.logger.info(f"通达信数据提供器初始化完成，数据路径: {self.tdx_path}")

    def _get_stock_file_path(self, stock_code: str, market: str = 'SH', 
                            data_type: str = 'day', period: int = 1) -> Optional[str]:
        """获取股票数据文件路径

        Args:
            stock_code: 股票代码，如 '000001'
            market: 市场代码，'SH'|'SZ'|'BJ'
            data_type: 数据类型，'day'|'week'|'month'|'min'
            period: 分钟周期，1|5|15|30|60，仅对分钟线有效

        Returns:
            文件路径，如果不存在则返回None
        """
        market_lower = self.MARKET_CODES.get(market.upper(), market.lower())
        data_type_dir = self.DATA_TYPES.get(data_type, data_type)

        # 构建基础路径
        if data_type == 'min':
            # 分钟线数据
            if period == 1:
                filename = f'{market_lower}{stock_code}.lc1'
            elif period == 5:
                filename = f'{market_lower}{stock_code}.lc5'
            elif period == 15:
                filename = f'{market_lower}{stock_code}.lc15'
            elif period == 30:
                filename = f'{market_lower}{stock_code}.lc30'
            elif period == 60:
                filename = f'{market_lower}{stock_code}.lc60'
            else:
                self.logger.warning(f"不支持的分钟周期: {period}")
                return None
        else:
            # 日、周、月线数据
            if data_type == 'day':
                ext = '.day'
            elif data_type == 'week':
                ext = '.week'
            elif data_type == 'month':
                ext = '.month'
            else:
                ext = '.day'
            
            filename = f'{market_lower}{stock_code}{ext}'

        # 构建完整路径
        file_path = os.path.join(self.vipdoc_path, market_lower, data_type_dir, filename)
        
        if not os.path.exists(file_path):
            self.logger.debug(f"数据文件不存在: {file_path}")
            # 尝试备用路径
            alt_path = os.path.join(self.tdx_path, 'vipdoc', market_lower, data_type_dir, filename)
            if os.path.exists(alt_path):
                return alt_path
            return None
        
        return file_path

    def _parse_day_data(self, file_path: str) -> List[Dict[str, Any]]:
        """解析日线数据文件

        Args:
            file_path: 日线文件路径

        Returns:
            日线数据列表
        """
        data = []
        try:
            with open(file_path, 'rb') as f:
                content = f.read()
            
            # 通达信日线数据格式：每条记录32字节
            # 格式：date(4字节), open(4字节), high(4字节), low(4字节), close(4字节), 
            #       amount(4字节), volume(4字节), reserve(4字节)
            record_size = 32
            num_records = len(content) // record_size
            
            for i in range(num_records):
                offset = i * record_size
                record = content[offset:offset + record_size]
                
                if len(record) < record_size:
                    break
                
                # 解析二进制数据
                # 日期：存储为整数，格式如20240115
                date_int = struct.unpack('I', record[0:4])[0]
                year = date_int // 10000
                month = (date_int % 10000) // 100
                day = date_int % 100
                
                # 价格数据：存储为整数，实际价格需要除以100
                open_price = struct.unpack('I', record[4:8])[0] / 100.0
                high_price = struct.unpack('I', record[8:12])[0] / 100.0
                low_price = struct.unpack('I', record[12:16])[0] / 100.0
                close_price = struct.unpack('I', record[16:20])[0] / 100.0
                
                # 成交额：单位元
                amount = struct.unpack('f', record[20:24])[0]
                
                # 成交量：单位股
                volume = struct.unpack('I', record[24:28])[0]
                
                # 保留字段
                reserve = struct.unpack('I', record[28:32])[0]
                
                data.append({
                    'date': f'{year:04d}-{month:02d}-{day:02d}',
                    'open': open_price,
                    'high': high_price,
                    'low': low_price,
                    'close': close_price,
                    'volume': volume,
                    'amount': amount,
                    'turnover': amount / 10000.0,  # 转换为万元
                })
            
            self.logger.info(f"解析日线数据成功，共 {len(data)} 条记录")
            
        except Exception as e:
            self.logger.error(f"解析日线数据失败: {file_path}, 错误: {e}")
        
        return data

    def _parse_minute_data(self, file_path: str, period: int = 1) -> List[Dict[str, Any]]:
        """解析分钟线数据文件

        Args:
            file_path: 分钟线文件路径
            period: 分钟周期

        Returns:
            分钟线数据列表
        """
        data = []
        try:
            with open(file_path, 'rb') as f:
                content = f.read()
            
            # 通达信分钟线数据格式：每条记录32字节
            # 格式：date(4字节), time(4字节), open(4字节), high(4字节), low(4字节), 
            #       close(4字节), amount(4字节), volume(4字节)
            record_size = 32
            num_records = len(content) // record_size
            
            for i in range(num_records):
                offset = i * record_size
                record = content[offset:offset + record_size]
                
                if len(record) < record_size:
                    break
                
                # 解析日期和时间
                date_int = struct.unpack('I', record[0:4])[0]
                time_int = struct.unpack('I', record[4:8])[0]
                
                year = date_int // 10000
                month = (date_int % 10000) // 100
                day = date_int % 100
                
                hour = time_int // 100
                minute = time_int % 100
                
                # 价格数据
                open_price = struct.unpack('I', record[8:12])[0] / 100.0
                high_price = struct.unpack('I', record[12:16])[0] / 100.0
                low_price = struct.unpack('I', record[16:20])[0] / 100.0
                close_price = struct.unpack('I', record[20:24])[0] / 100.0
                
                # 成交额和成交量
                amount = struct.unpack('f', record[24:28])[0]
                volume = struct.unpack('I', record[28:32])[0]
                
                data.append({
                    'datetime': f'{year:04d}-{month:02d}-{day:02d} {hour:02d}:{minute:02d}:00',
                    'date': f'{year:04d}-{month:02d}-{day:02d}',
                    'time': f'{hour:02d}:{minute:02d}',
                    'open': open_price,
                    'high': high_price,
                    'low': low_price,
                    'close': close_price,
                    'volume': volume,
                    'amount': amount,
                    'period': f'{period}分钟',
                })
            
            self.logger.info(f"解析分钟线数据成功，共 {len(data)} 条记录，周期: {period}分钟")
            
        except Exception as e:
            self.logger.error(f"解析分钟线数据失败: {file_path}, 错误: {e}")
        
        return data

    def _parse_week_month_data(self, file_path: str, data_type: str) -> List[Dict[str, Any]]:
        """解析周线或月线数据文件

        Args:
            file_path: 数据文件路径
            data_type: 数据类型，'week'或'month'

        Returns:
            周线或月线数据列表
        """
        data = []
        try:
            with open(file_path, 'rb') as f:
                content = f.read()
            
            # 周线和月线数据格式与日线类似
            record_size = 32
            num_records = len(content) // record_size
            
            for i in range(num_records):
                offset = i * record_size
                record = content[offset:offset + record_size]
                
                if len(record) < record_size:
                    break
                
                # 解析日期
                date_int = struct.unpack('I', record[0:4])[0]
                year = date_int // 10000
                month = (date_int % 10000) // 100
                day = date_int % 100
                
                # 价格数据
                open_price = struct.unpack('I', record[4:8])[0] / 100.0
                high_price = struct.unpack('I', record[8:12])[0] / 100.0
                low_price = struct.unpack('I', record[12:16])[0] / 100.0
                close_price = struct.unpack('I', record[16:20])[0] / 100.0
                
                # 成交额和成交量
                amount = struct.unpack('f', record[20:24])[0]
                volume = struct.unpack('I', record[24:28])[0]
                
                data.append({
                    'date': f'{year:04d}-{month:02d}-{day:02d}',
                    'open': open_price,
                    'high': high_price,
                    'low': low_price,
                    'close': close_price,
                    'volume': volume,
                    'amount': amount,
                    'type': data_type,
                })
            
            self.logger.info(f"解析{data_type}线数据成功，共 {len(data)} 条记录")
            
        except Exception as e:
            self.logger.error(f"解析{data_type}线数据失败: {file_path}, 错误: {e}")
        
        return data

    def get_stock_day_data(self, stock_code: str, market: str = 'SH', 
                          start_date: str = None, end_date: str = None) -> List[Dict[str, Any]]:
        """获取股票日线数据

        Args:
            stock_code: 股票代码
            market: 市场代码
            start_date: 开始日期，格式 'YYYY-MM-DD'
            end_date: 结束日期，格式 'YYYY-MM-DD'

        Returns:
            日线数据列表
        """
        file_path = self._get_stock_file_path(stock_code, market, 'day')
        if not file_path:
            self.logger.warning(f"日线数据文件不存在: {stock_code}.{market}")
            return []
        
        data = self._parse_day_data(file_path)
        
        # 按日期范围过滤
        if start_date or end_date:
            filtered_data = []
            for item in data:
                item_date = item['date']
                if start_date and item_date < start_date:
                    continue
                if end_date and item_date > end_date:
                    continue
                filtered_data.append(item)
            data = filtered_data
        
        return data

    def get_stock_minute_data(self, stock_code: str, market: str = 'SH',
                             period: int = 1, start_time: str = None, 
                             end_time: str = None) -> List[Dict[str, Any]]:
        """获取股票分钟线数据

        Args:
            stock_code: 股票代码
            market: 市场代码
            period: 分钟周期，1|5|15|30|60
            start_time: 开始时间，格式 'YYYY-MM-DD HH:MM:SS'
            end_time: 结束时间，格式 'YYYY-MM-DD HH:MM:SS'

        Returns:
            分钟线数据列表
        """
        file_path = self._get_stock_file_path(stock_code, market, 'min', period)
        if not file_path:
            self.logger.warning(f"分钟线数据文件不存在: {stock_code}.{market}, 周期: {period}分钟")
            return []
        
        data = self._parse_minute_data(file_path, period)
        
        # 按时间范围过滤
        if start_time or end_time:
            filtered_data = []
            for item in data:
                item_datetime = item['datetime']
                if start_time and item_datetime < start_time:
                    continue
                if end_time and item_datetime > end_time:
                    continue
                filtered_data.append(item)
            data = filtered_data
        
        return data

    def get_stock_week_data(self, stock_code: str, market: str = 'SH',
                           start_date: str = None, end_date: str = None) -> List[Dict[str, Any]]:
        """获取股票周线数据

        Args:
            stock_code: 股票代码
            market: 市场代码
            start_date: 开始日期
            end_date: 结束日期

        Returns:
            周线数据列表
        """
        file_path = self._get_stock_file_path(stock_code, market, 'week')
        if not file_path:
            self.logger.warning(f"周线数据文件不存在: {stock_code}.{market}")
            return []
        
        data = self._parse_week_month_data(file_path, 'week')
        
        # 按日期范围过滤
        if start_date or end_date:
            filtered_data = []
            for item in data:
                item_date = item['date']
                if start_date and item_date < start_date:
                    continue
                if end_date and item_date > end_date:
                    continue
                filtered_data.append(item)
            data = filtered_data
        
        return data

    def get_stock_month_data(self, stock_code: str, market: str = 'SH',
                            start_date: str = None, end_date: str = None) -> List[Dict[str, Any]]:
        """获取股票月线数据

        Args:
            stock_code: 股票代码
            market: 市场代码
            start_date: 开始日期
            end_date: 结束日期

        Returns:
            月线数据列表
        """
        file_path = self._get_stock_file_path(stock_code, market, 'month')
        if not file_path:
            self.logger.warning(f"月线数据文件不存在: {stock_code}.{market}")
            return []
        
        data = self._parse_week_month_data(file_path, 'month')
        
        # 按日期范围过滤
        if start_date or end_date:
            filtered_data = []
            for item in data:
                item_date = item['date']
                if start_date and item_date < start_date:
                    continue
                if end_date and item_date > end_date:
                    continue
                filtered_data.append(item)
            data = filtered_data
        
        return data

    def get_stock_list(self, market: str = 'SH') -> List[Dict[str, str]]:
        """获取股票列表

        Args:
            market: 市场代码

        Returns:
            股票列表
        """
        stock_list = []
        try:
            # 通达信股票列表文件通常位于 T0002/hq_cache 或 T0002/blocknew
            block_path = os.path.join(self.t0002_path, 'hq_cache', 'blocknew.cfg')
            if not os.path.exists(block_path):
                block_path = os.path.join(self.t0002_path, 'blocknew', 'blocknew.cfg')
            
            if os.path.exists(block_path):
                with open(block_path, 'r', encoding='gbk') as f:
                    lines = f.readlines()
                
                for line in lines:
                    line = line.strip()
                    if line and '|' in line:
                        parts = line.split('|')
                        if len(parts) >= 3:
                            code = parts[0]
                            name = parts[1]
                            market_code = code[:2].lower()
                            
                            # 过滤市场
                            target_market = self.MARKET_CODES.get(market.upper(), market.lower())
                            if market_code == target_market:
                                stock_list.append({
                                    'code': code[2:],  # 去除市场前缀
                                    'name': name,
                                    'market': market,
                                    'full_code': code,
                                })
            
            self.logger.info(f"获取股票列表成功，共 {len(stock_list)} 只股票")
            
        except Exception as e:
            self.logger.error(f"获取股票列表失败: {e}")
        
        return stock_list

    def get_sector_list(self) -> List[Dict[str, Any]]:
        """获取概念板块列表

        Returns:
            概念板块列表
        """
        sectors = []
        try:
            # 通达信板块文件
            sector_path = os.path.join(self.t0002_path, 'hq_cache', 'blocknew.cfg')
            if not os.path.exists(sector_path):
                sector_path = os.path.join(self.t0002_path, 'blocknew', 'blocknew.cfg')
            
            if os.path.exists(sector_path):
                with open(sector_path, 'r', encoding='gbk') as f:
                    lines = f.readlines()
                
                current_sector = None
                for line in lines:
                    line = line.strip()
                    if line.startswith('#'):
                        # 板块标题行
                        if current_sector:
                            sectors.append(current_sector)
                        
                        parts = line[1:].split('|')
                        if len(parts) >= 2:
                            current_sector = {
                                'name': parts[0],
                                'code': parts[1] if len(parts) > 1 else '',
                                'stocks': []
                            }
                    elif line and current_sector and '|' in line:
                        # 股票行
                        parts = line.split('|')
                        if len(parts) >= 2:
                            stock_code = parts[0]
                            stock_name = parts[1]
                            current_sector['stocks'].append({
                                'code': stock_code,
                                'name': stock_name
                            })
                
                # 添加最后一个板块
                if current_sector:
                    sectors.append(current_sector)
            
            self.logger.info(f"获取概念板块列表成功，共 {len(sectors)} 个板块")
            
        except Exception as e:
            self.logger.error(f"获取概念板块列表失败: {e}")
        
        return sectors

    def get_sector_stocks(self, sector_name: str) -> List[Dict[str, str]]:
        """获取指定概念板块的股票列表

        Args:
            sector_name: 板块名称

        Returns:
            股票列表
        """
        sectors = self.get_sector_list()
        for sector in sectors:
            if sector['name'] == sector_name:
                return sector['stocks']
        
        return []

    def calculate_technical_indicators(self, stock_code: str, market: str = 'SH',
                                     data_type: str = 'day', period: int = 1,
                                     lookback: int = 100) -> Dict[str, Any]:
        """计算技术指标

        Args:
            stock_code: 股票代码
            market: 市场代码
            data_type: 数据类型，'day'|'week'|'month'|'min'
            period: 分钟周期，仅对分钟线有效
            lookback: 回溯周期数

        Returns:
            技术指标
        """
        # 获取K线数据
        if data_type == 'day':
            kline_data = self.get_stock_day_data(stock_code, market)
        elif data_type == 'week':
            kline_data = self.get_stock_week_data(stock_code, market)
        elif data_type == 'month':
            kline_data = self.get_stock_month_data(stock_code, market)
        elif data_type == 'min':
            kline_data = self.get_stock_minute_data(stock_code, market, period)
        else:
            self.logger.warning(f"不支持的数据类型: {data_type}")
            return {}
        
        if not kline_data:
            return {}
        
        # 限制数据量
        kline_data = kline_data[-lookback:]
        
        # 计算移动平均线
        closes = [item['close'] for item in kline_data]
        
        def calculate_ma(data, period):
            if len(data) < period:
                return 0
            return sum(data[-period:]) / period
        
        ma5 = calculate_ma(closes, 5)
        ma10 = calculate_ma(closes, 10)
        ma20 = calculate_ma(closes, 20)
        ma30 = calculate_ma(closes, 30)
        ma60 = calculate_ma(closes, 60)
        
        # 计算MACD（简化版）
        ema12 = self._calculate_ema(closes, 12)
        ema26 = self._calculate_ema(closes, 26)
        dif = ema12 - ema26
        dea = self._calculate_ema([dif], 9)
        macd = (dif - dea) * 2
        
        # 计算RSI
        rsi = self._calculate_rsi(closes, 14)
        
        # 计算KDJ（简化版）
        high_prices = [item['high'] for item in kline_data]
        low_prices = [item['low'] for item in kline_data]
        
        if len(kline_data) >= 9:
            recent_highs = high_prices[-9:]
            recent_lows = low_prices[-9:]
            close = closes[-1]
            
            highest_high = max(recent_highs)
            lowest_low = min(recent_lows)
            
            if highest_high != lowest_low:
                rsv = ((close - lowest_low) / (highest_high - lowest_low)) * 100
            else:
                rsv = 50
            
            # 简化计算K、D、J
            k = (2/3) * 50 + (1/3) * rsv
            d = (2/3) * 50 + (1/3) * k
            j = 3 * k - 2 * d
        else:
            k = d = j = 50
        
        return {
            'ma': {
                'ma5': round(ma5, 2),
                'ma10': round(ma10, 2),
                'ma20': round(ma20, 2),
                'ma30': round(ma30, 2),
                'ma60': round(ma60, 2),
            },
            'macd': {
                'dif': round(dif, 2),
                'dea': round(dea, 2),
                'macd': round(macd, 2),
            },
            'rsi': round(rsi, 2),
            'kdj': {
                'k': round(k, 2),
                'd': round(d, 2),
                'j': round(j, 2),
            }
        }

    def _calculate_ema(self, data: List[float], period: int) -> float:
        """计算指数移动平均

        Args:
            data: 价格列表
            period: 周期

        Returns:
            EMA值
        """
        if not data:
            return 0
        
        # 使用简单移动平均作为初始值
        if len(data) < period:
            return sum(data) / len(data) if data else 0
        
        ema = sum(data[:period]) / period
        alpha = 2 / (period + 1)
        
        for price in data[period:]:
            ema = alpha * price + (1 - alpha) * ema
        
        return ema

    def _calculate_rsi(self, prices: List[float], period: int = 14) -> float:
        """计算RSI

        Args:
            prices: 价格列表
            period: 周期

        Returns:
            RSI值
        """
        if len(prices) < period + 1:
            return 50
        
        gains = []
        losses = []
        
        for i in range(1, len(prices)):
            change = prices[i] - prices[i-1]
            if change > 0:
                gains.append(change)
                losses.append(0)
            else:
                gains.append(0)
                losses.append(abs(change))
        
        avg_gain = sum(gains[-period:]) / period
        avg_loss = sum(losses[-period:]) / period
        
        if avg_loss == 0:
            return 100
        
        rs = avg_gain / avg_loss
        rsi = 100 - (100 / (1 + rs))
        
        return rsi

    def get_stock_info(self, stock_code: str, market: str = 'SH') -> Dict[str, Any]:
        """获取股票基本信息

        Args:
            stock_code: 股票代码
            market: 市场代码

        Returns:
            股票信息
        """
        # 获取最新日线数据
        day_data = self.get_stock_day_data(stock_code, market)
        if not day_data:
            return {}
        
        latest_data = day_data[-1]
        
        # 获取股票列表以获取名称
        stock_list = self.get_stock_list(market)
        stock_name = ""
        for stock in stock_list:
            if stock['code'] == stock_code:
                stock_name = stock['name']
                break
        
        # 计算技术指标
        indicators = self.calculate_technical_indicators(stock_code, market, 'day')
        
        return {
            'code': stock_code,
            'name': stock_name,
            'market': market,
            'current_price': latest_data['close'],
            'open_price': latest_data['open'],
            'high_price': latest_data['high'],
            'low_price': latest_data['low'],
            'pre_close': day_data[-2]['close'] if len(day_data) >= 2 else latest_data['open'],
            'volume': latest_data['volume'],
            'amount': latest_data['amount'],
            'change_pct': ((latest_data['close'] - day_data[-2]['close']) / day_data[-2]['close'] * 100) if len(day_data) >= 2 else 0,
            'change_amount': (latest_data['close'] - day_data[-2]['close']) if len(day_data) >= 2 else 0,
            'indicators': indicators,
        }