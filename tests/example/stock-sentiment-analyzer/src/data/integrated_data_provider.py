"""
集成数据提供器
结合网络API和本地通达信数据，提供可靠的数据访问
"""

import asyncio
import logging
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta

from .stock_data_provider import StockDataProvider
from .tdx_data_provider import TdxDataProvider
from .capital_flow_provider import ApiCapitalFlowProvider, TdxCapitalFlowProvider


class IntegratedDataProvider:
    """集成数据提供器，支持多数据源和降级策略"""
    
    def __init__(self, config: Dict[str, Any]):
        """初始化集成数据提供器
        
        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)
        
        # 初始化各个数据提供器
        self.api_provider = StockDataProvider(config)
        self.use_tdx = config.get('tdx', {}).get('enabled', False)
        
        if self.use_tdx:
            self.tdx_provider = TdxDataProvider(config)
        else:
            self.tdx_provider = None
            self.logger.info("通达信数据源未启用")
        
        # 初始化资金流向数据提供器
        self.api_flow_provider = ApiCapitalFlowProvider(config)
        if self.use_tdx:
            self.tdx_flow_provider = TdxCapitalFlowProvider(config)
        else:
            self.tdx_flow_provider = None
        
        # 数据源优先级配置
        self.priority = config.get('data_source_priority', ['api', 'tdx'])
        
        # 缓存配置
        self.cache_enabled = config.get('tdx', {}).get('cache_enabled', True)
        self.cache_ttl = config.get('tdx', {}).get('cache_ttl', 300)
        self.cache = {}
        
        self.logger.info(f"集成数据提供器初始化完成，启用数据源: API, {'TDX' if self.use_tdx else '无'}")

    async def get_stock_info(self, stock_code: str, market: str = 'SH') -> Dict[str, Any]:
        """获取股票信息，支持降级策略
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            
        Returns:
            股票信息字典
        """
        # 检查缓存
        cache_key = f"stock_info_{stock_code}_{market}"
        if self.cache_enabled and cache_key in self.cache:
            cached_data, cached_time = self.cache[cache_key]
            if (datetime.now() - cached_time).seconds < self.cache_ttl:
                self.logger.debug(f"使用缓存数据: {stock_code}.{market}")
                return cached_data
        
        # 按优先级尝试各个数据源
        for source in self.priority:
            try:
                if source == 'api':
                    self.logger.debug(f"尝试从API获取 {stock_code}.{market} 信息")
                    info = await self.api_provider.get_stock_info(stock_code)
                    
                    if info and info.get('current_price', 0) > 0:
                        info['data_source'] = 'api'
                        # 缓存结果
                        if self.cache_enabled:
                            self.cache[cache_key] = (info, datetime.now())
                        self.logger.info(f"API获取成功: {stock_code}.{market}")
                        return info
                
                elif source == 'tdx' and self.use_tdx and self.tdx_provider:
                    self.logger.debug(f"尝试从通达信获取 {stock_code}.{market} 信息")
                    info = self.tdx_provider.get_stock_info(stock_code, market)
                    
                    if info and info.get('current_price', 0) > 0:
                        info['data_source'] = 'tdx'
                        # 缓存结果
                        if self.cache_enabled:
                            self.cache[cache_key] = (info, datetime.now())
                        self.logger.info(f"通达信获取成功: {stock_code}.{market}")
                        return info
            
            except Exception as e:
                self.logger.warning(f"数据源 {source} 获取失败 {stock_code}.{market}: {e}")
                continue
        
        self.logger.error(f"所有数据源获取失败: {stock_code}.{market}")
        return {}

    async def get_batch_stock_info(self, stock_codes: List[str], market: str = 'SH') -> Dict[str, Dict[str, Any]]:
        """批量获取股票信息
        
        Args:
            stock_codes: 股票代码列表
            market: 市场代码
            
        Returns:
            股票信息字典
        """
        results = {}
        
        # 并发获取
        tasks = [self.get_stock_info(code, market) for code in stock_codes]
        stock_infos = await asyncio.gather(*tasks, return_exceptions=True)
        
        for code, info in zip(stock_codes, stock_infos):
            if isinstance(info, Exception):
                self.logger.error(f"获取股票信息失败: {code}, {info}")
            elif info:
                results[code] = info
        
        return results

    async def get_stock_kline(self, stock_code: str, period: str = '1d', count: int = 100) -> List[Dict[str, Any]]:
        """获取股票K线数据，智能选择数据源
        
        Args:
            stock_code: 股票代码
            period: 周期 (1d=日线, 1w=周线, 1m=月线, min=分钟线)
            count: 数量
            
        Returns:
            K线数据列表
        """
        kline_data = []
        
        # 根据周期选择最佳数据源
        if period in ['1w', '1m'] or (period == 'min' and self.use_tdx):
            # 周线、月线、分钟线优先使用通达信
            if self.use_tdx and self.tdx_provider:
                try:
                    if period == '1w':
                        data = self.tdx_provider.get_stock_week_data(stock_code, 'SH')
                    elif period == '1m':
                        data = self.tdx_provider.get_stock_month_data(stock_code, 'SH')
                    elif period == 'min':
                        data = self.tdx_provider.get_stock_minute_data(stock_code, 'SH', period=5)
                    
                    if data:
                        # 转换为统一格式
                        for item in data[-count:]:
                            if period == 'min':
                                kline_data.append({
                                    'date': item['datetime'],
                                    'open': item['open'],
                                    'close': item['close'],
                                    'high': item['high'],
                                    'low': item['low'],
                                    'volume': item['volume']
                                })
                            else:
                                kline_data.append({
                                    'date': item['date'],
                                    'open': item['open'],
                                    'close': item['close'],
                                    'high': item['high'],
                                    'low': item['low'],
                                    'volume': item['volume']
                                })
                        self.logger.info(f"通达信{period}线数据获取成功: {stock_code}, {len(kline_data)}条")
                except Exception as e:
                    self.logger.warning(f"通达信{period}线数据获取失败 {stock_code}: {e}")
        
        # 日线数据，优先使用API，失败时使用通达信
        if period == '1d' or (not kline_data and period in ['1w', '1m']):
            try:
                # 首先尝试API
                api_data = await self.api_provider.get_stock_kline(stock_code, period, count)
                if api_data:
                    kline_data = api_data[-count:]
                    self.logger.info(f"API {period}线数据获取成功: {stock_code}, {len(kline_data)}条")
            except Exception as e:
                self.logger.warning(f"API {period}线数据获取失败 {stock_code}: {e}")
                
                # API失败，尝试通达信（如果是日线）
                if period == '1d' and self.use_tdx and self.tdx_provider:
                    try:
                        tdx_data = self.tdx_provider.get_stock_day_data(stock_code, 'SH')
                        if tdx_data:
                            for item in tdx_data[-count:]:
                                kline_data.append({
                                    'date': item['date'],
                                    'open': item['open'],
                                    'close': item['close'],
                                    'high': item['high'],
                                    'low': item['low'],
                                    'volume': item['volume']
                                })
                            self.logger.info(f"通达信日线数据获取成功: {stock_code}, {len(kline_data)}条")
                    except Exception as e2:
                        self.logger.warning(f"通达信日线数据获取失败 {stock_code}: {e2}")
        
        return kline_data

    async def calculate_technical_indicators(self, stock_code: str, period: str = '1d') -> Dict[str, Any]:
        """计算技术指标，优先使用本地数据
        
        Args:
            stock_code: 股票代码
            period: 周期
            
        Returns:
            技术指标
        """
        # 优先使用通达信计算技术指标（减少API调用）
        if self.use_tdx and self.tdx_provider:
            try:
                if period == '1d':
                    data_type = 'day'
                elif period == '1w':
                    data_type = 'week'
                elif period == '1m':
                    data_type = 'month'
                else:
                    data_type = 'day'
                
                indicators = self.tdx_provider.calculate_technical_indicators(stock_code, 'SH', data_type)
                if indicators:
                    self.logger.debug(f"通达信技术指标计算成功: {stock_code}")
                    return indicators
            except Exception as e:
                self.logger.warning(f"通达信技术指标计算失败 {stock_code}: {e}")
        
        # 通达信失败，使用API
        try:
            indicators = await self.api_provider.calculate_technical_indicators(stock_code, period)
            if indicators:
                self.logger.debug(f"API技术指标计算成功: {stock_code}")
                return indicators
        except Exception as e:
            self.logger.warning(f"API技术指标计算失败 {stock_code}: {e}")
        
        return {}

    def get_sector_list(self) -> List[Dict[str, Any]]:
        """获取概念板块列表（仅通达信支持）
        
        Returns:
            板块列表
        """
        if self.use_tdx and self.tdx_provider:
            try:
                sectors = self.tdx_provider.get_sector_list()
                self.logger.info(f"获取板块列表成功: {len(sectors)}个板块")
                return sectors
            except Exception as e:
                self.logger.error(f"获取板块列表失败: {e}")
        
        self.logger.warning("板块数据需要通达信数据源支持")
        return []

    def get_sector_stocks(self, sector_name: str) -> List[Dict[str, str]]:
        """获取指定板块的股票列表
        
        Args:
            sector_name: 板块名称
            
        Returns:
            股票列表
        """
        if self.use_tdx and self.tdx_provider:
            try:
                stocks = self.tdx_provider.get_sector_stocks(sector_name)
                self.logger.info(f"获取板块股票成功: {sector_name}, {len(stocks)}只股票")
                return stocks
            except Exception as e:
                self.logger.error(f"获取板块股票失败 {sector_name}: {e}")
        
        return []

    def get_stock_list(self, market: str = 'SH') -> List[Dict[str, str]]:
        """获取股票列表
        
        Args:
            market: 市场代码
            
        Returns:
            股票列表
        """
        if self.use_tdx and self.tdx_provider:
            try:
                stocks = self.tdx_provider.get_stock_list(market)
                self.logger.info(f"获取股票列表成功: {market}, {len(stocks)}只股票")
                return stocks
            except Exception as e:
                self.logger.error(f"通达信获取股票列表失败 {market}: {e}")
        
        # 通达信失败，返回空列表
        self.logger.warning("股票列表数据需要通达信数据源支持")
        return []

    def get_multi_period_data(self, stock_code: str, market: str = 'SH') -> Dict[str, List[Dict[str, Any]]]:
        """获取多周期数据
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            
        Returns:
            多周期数据字典
        """
        result = {}
        
        if self.use_tdx and self.tdx_provider:
            try:
                # 日线数据
                day_data = self.tdx_provider.get_stock_day_data(stock_code, market)
                if day_data:
                    result['day'] = day_data[-30:]  # 最近30天
                
                # 周线数据
                week_data = self.tdx_provider.get_stock_week_data(stock_code, market)
                if week_data:
                    result['week'] = week_data[-20:]  # 最近20周
                
                # 月线数据
                month_data = self.tdx_provider.get_stock_month_data(stock_code, market)
                if month_data:
                    result['month'] = month_data[-12:]  # 最近12个月
                
                # 分钟线数据
                minute_data = self.tdx_provider.get_stock_minute_data(stock_code, market, period=5)
                if minute_data:
                    result['minute'] = minute_data[-48:]  # 最近48个5分钟线
                
                self.logger.info(f"多周期数据获取成功: {stock_code}.{market}")
                
            except Exception as e:
                self.logger.error(f"多周期数据获取失败 {stock_code}.{market}: {e}")
        
        return result

    # 资金流向相关方法
    async def get_stock_capital_flow(self, stock_code: str, market: str = 'SH') -> Dict[str, Any]:
        """获取个股资金流向数据，支持降级策略
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            
        Returns:
            资金流向数据
        """
        # 检查缓存
        cache_key = f"stock_capital_flow_{stock_code}_{market}"
        if self.cache_enabled and cache_key in self.cache:
            cached_data, cached_time = self.cache[cache_key]
            if (datetime.now() - cached_time).seconds < self.cache_ttl:
                self.logger.debug(f"使用缓存资金流向数据: {stock_code}.{market}")
                return cached_data
        
        # 按优先级尝试各个数据源
        for source in self.priority:
            try:
                if source == 'api':
                    self.logger.debug(f"尝试从API获取资金流向数据: {stock_code}.{market}")
                    flow_data = await self.api_flow_provider.get_stock_capital_flow(stock_code, market)
                    
                    if flow_data:
                        flow_data['data_source'] = 'api'
                        # 缓存结果
                        if self.cache_enabled:
                            self.cache[cache_key] = (flow_data, datetime.now())
                        self.logger.info(f"API资金流向数据获取成功: {stock_code}.{market}")
                        return flow_data
                
                elif source == 'tdx' and self.use_tdx and self.tdx_flow_provider:
                    self.logger.debug(f"尝试从通达信获取资金流向数据: {stock_code}.{market}")
                    flow_data = await self.tdx_flow_provider.get_stock_capital_flow(stock_code, market)
                    
                    if flow_data:
                        flow_data['data_source'] = 'tdx'
                        # 缓存结果
                        if self.cache_enabled:
                            self.cache[cache_key] = (flow_data, datetime.now())
                        self.logger.info(f"通达信资金流向数据获取成功: {stock_code}.{market}")
                        return flow_data
            
            except Exception as e:
                self.logger.warning(f"数据源 {source} 获取资金流向数据失败 {stock_code}.{market}: {e}")
                continue
        
        self.logger.warning(f"所有数据源获取资金流向数据失败: {stock_code}.{market}")
        return {}
    
    async def get_sector_capital_flow(self, sector_name: str) -> Dict[str, Any]:
        """获取板块资金流向数据
        
        Args:
            sector_name: 板块名称
            
        Returns:
            板块资金流向数据
        """
        # 优先使用API，通达信可能不支持
        try:
            flow_data = await self.api_flow_provider.get_sector_capital_flow(sector_name)
            if flow_data:
                flow_data['data_source'] = 'api'
                return flow_data
        except Exception as e:
            self.logger.warning(f"API获取板块资金流向数据失败 {sector_name}: {e}")
        
        # API失败，尝试通达信
        if self.use_tdx and self.tdx_flow_provider:
            try:
                flow_data = await self.tdx_flow_provider.get_sector_capital_flow(sector_name)
                if flow_data:
                    flow_data['data_source'] = 'tdx'
                    return flow_data
            except Exception as e:
                self.logger.warning(f"通达信获取板块资金流向数据失败 {sector_name}: {e}")
        
        return {}
    
    async def get_market_capital_flow(self, market: str = 'SH') -> Dict[str, Any]:
        """获取市场整体资金流向数据
        
        Args:
            market: 市场代码
            
        Returns:
            市场资金流向数据
        """
        # 检查缓存
        cache_key = f"market_capital_flow_{market}"
        if self.cache_enabled and cache_key in self.cache:
            cached_data, cached_time = self.cache[cache_key]
            if (datetime.now() - cached_time).seconds < self.cache_ttl:
                self.logger.debug(f"使用缓存市场资金流向数据: {market}")
                return cached_data
        
        # 按优先级尝试各个数据源
        for source in self.priority:
            try:
                if source == 'api':
                    self.logger.debug(f"尝试从API获取市场资金流向数据: {market}")
                    flow_data = await self.api_flow_provider.get_market_capital_flow(market)
                    
                    if flow_data:
                        flow_data['data_source'] = 'api'
                        # 缓存结果
                        if self.cache_enabled:
                            self.cache[cache_key] = (flow_data, datetime.now())
                        self.logger.info(f"API市场资金流向数据获取成功: {market}")
                        return flow_data
                
                elif source == 'tdx' and self.use_tdx and self.tdx_flow_provider:
                    self.logger.debug(f"尝试从通达信获取市场资金流向数据: {market}")
                    flow_data = await self.tdx_flow_provider.get_market_capital_flow(market)
                    
                    if flow_data:
                        flow_data['data_source'] = 'tdx'
                        # 缓存结果
                        if self.cache_enabled:
                            self.cache[cache_key] = (flow_data, datetime.now())
                        self.logger.info(f"通达信市场资金流向数据获取成功: {market}")
                        return flow_data
            
            except Exception as e:
                self.logger.warning(f"数据源 {source} 获取市场资金流向数据失败 {market}: {e}")
                continue
        
        return {}
    
    async def get_northbound_flow(self) -> Dict[str, Any]:
        """获取北向资金流向数据
        
        Returns:
            北向资金流向数据
        """
        # 北向资金流向数据通常只从API获取
        try:
            flow_data = await self.api_flow_provider.get_northbound_flow()
            if flow_data:
                flow_data['data_source'] = 'api'
                return flow_data
        except Exception as e:
            self.logger.error(f"获取北向资金流向数据失败: {e}")
        
        return {}
    
    async def get_main_force_flow(self, stock_code: str = None, market: str = 'SH') -> Dict[str, Any]:
        """获取主力资金流向数据
        
        Args:
            stock_code: 股票代码，None表示获取市场整体
            market: 市场代码
            
        Returns:
            主力资金流向数据
        """
        if stock_code:
            # 获取个股主力资金流向
            return await self.get_stock_capital_flow(stock_code, market)
        else:
            # 获取市场整体主力资金流向
            return await self.get_market_capital_flow(market)
    
    async def get_comprehensive_stock_data(self, stock_code: str, market: str = 'SH') -> Dict[str, Any]:
        """获取股票综合数据（基本信息+K线+资金流向+技术指标）
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            
        Returns:
            综合数据字典
        """
        # 并行获取各类数据
        tasks = [
            self.get_stock_info(stock_code, market),
            self.get_stock_kline(stock_code, '1d', 30),
            self.get_stock_capital_flow(stock_code, market),
            self.calculate_technical_indicators(stock_code, '1d'),
        ]
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        stock_info = results[0] if not isinstance(results[0], Exception) else {}
        kline_data = results[1] if not isinstance(results[1], Exception) else []
        capital_flow = results[2] if not isinstance(results[2], Exception) else {}
        indicators = results[3] if not isinstance(results[3], Exception) else {}
        
        return {
            'basic_info': stock_info,
            'kline_data': kline_data,
            'capital_flow': capital_flow,
            'technical_indicators': indicators,
            'timestamp': datetime.now().isoformat(),
        }

    def clear_cache(self):
        """清除缓存"""
        self.cache.clear()
        self.logger.info("数据缓存已清除")

    def get_data_source_status(self) -> Dict[str, bool]:
        """获取数据源状态
        
        Returns:
            数据源状态字典
        """
        return {
            'api': True,  # API总是可用
            'tdx': self.use_tdx and self.tdx_provider is not None,
            'capital_flow_api': True,
            'capital_flow_tdx': self.use_tdx and self.tdx_flow_provider is not None,
            'cache_enabled': self.cache_enabled,
            'cache_size': len(self.cache)
        }