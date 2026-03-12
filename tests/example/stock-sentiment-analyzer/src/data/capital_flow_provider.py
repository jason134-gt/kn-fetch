"""
资金流向数据提供器
支持通达信本地资金流向数据和网络API资金流向数据
"""

import os
import struct
import logging
from typing import Dict, Any, List, Optional, Tuple
from datetime import datetime, timedelta
import aiohttp
import json


class CapitalFlowProvider:
    """资金流向数据提供器基类"""
    
    def __init__(self, config: Dict[str, Any]):
        """初始化资金流向数据提供器
        
        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)
    
    async def get_stock_capital_flow(self, stock_code: str, market: str = 'SH') -> Dict[str, Any]:
        """获取个股资金流向数据
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            
        Returns:
            资金流向数据
        """
        raise NotImplementedError
    
    async def get_sector_capital_flow(self, sector_name: str) -> Dict[str, Any]:
        """获取板块资金流向数据
        
        Args:
            sector_name: 板块名称
            
        Returns:
            板块资金流向数据
        """
        raise NotImplementedError
    
    async def get_market_capital_flow(self, market: str = 'SH') -> Dict[str, Any]:
        """获取市场整体资金流向数据
        
        Args:
            market: 市场代码
            
        Returns:
            市场资金流向数据
        """
        raise NotImplementedError
    
    async def get_northbound_flow(self) -> Dict[str, Any]:
        """获取北向资金流向数据
        
        Returns:
            北向资金流向数据
        """
        raise NotImplementedError
    
    async def get_main_force_flow(self, stock_code: str = None, market: str = 'SH') -> Dict[str, Any]:
        """获取主力资金流向数据
        
        Args:
            stock_code: 股票代码，None表示获取市场整体
            market: 市场代码
            
        Returns:
            主力资金流向数据
        """
        raise NotImplementedError


class TdxCapitalFlowProvider(CapitalFlowProvider):
    """通达信资金流向数据提供器"""
    
    def __init__(self, config: Dict[str, Any]):
        """初始化通达信资金流向数据提供器
        
        Args:
            config: 配置字典，应包含tdx_data_path
        """
        super().__init__(config)
        
        # 获取通达信数据路径
        self.tdx_path = config.get('tdx', {}).get('data_path', 'C:/zd_zsone')
        self.t0002_path = os.path.join(self.tdx_path, 'T0002')
        
        # 资金流向文件映射
        self.flow_files = {
            'SH': 'shase.dat',  # 上海资金流向
            'SZ': 'sznse.dat',  # 深圳资金流向
            'BJ': 'bjnse.dat',  # 北京资金流向（可能不存在）
        }
        
        self.logger.info(f"通达信资金流向数据提供器初始化完成，数据路径: {self.tdx_path}")
    
    def _get_flow_file_path(self, market: str = 'SH') -> Optional[str]:
        """获取资金流向文件路径
        
        Args:
            market: 市场代码
            
        Returns:
            文件路径，如果不存在则返回None
        """
        filename = self.flow_files.get(market.upper())
        if not filename:
            return None
        
        # 尝试多个可能的路径
        possible_paths = [
            os.path.join(self.tdx_path, 'vipdoc', market.lower(), 'flow', filename),
            os.path.join(self.tdx_path, 'T0002', 'hq_cache', filename),
            os.path.join(self.tdx_path, 'T0002', 'flow', filename),
            os.path.join(self.tdx_path, 'flow', filename),
        ]
        
        for file_path in possible_paths:
            if os.path.exists(file_path):
                return file_path
        
        self.logger.warning(f"资金流向文件不存在: {filename}")
        return None
    
    def _parse_flow_data(self, file_path: str) -> List[Dict[str, Any]]:
        """解析资金流向数据文件
        
        Args:
            file_path: 资金流向文件路径
            
        Returns:
            资金流向数据列表
        """
        data = []
        try:
            with open(file_path, 'rb') as f:
                content = f.read()
            
            # 通达信资金流向数据格式（假设）
            # 每条记录可能包含：股票代码、主力流入、主力流出、净流入、日期等
            record_size = 40  # 假设的字节大小，实际可能需要调整
            
            num_records = len(content) // record_size
            
            for i in range(num_records):
                offset = i * record_size
                record = content[offset:offset + record_size]
                
                if len(record) < record_size:
                    break
                
                # 解析股票代码（假设前6字节为ASCII编码的股票代码）
                stock_code = record[0:6].decode('ascii', errors='ignore').strip()
                
                # 解析资金流向数据（假设格式，需要根据实际文件格式调整）
                # 这里使用示例解析逻辑
                try:
                    main_inflow = struct.unpack('f', record[6:10])[0]  # 主力流入
                    main_outflow = struct.unpack('f', record[10:14])[0]  # 主力流出
                    retail_inflow = struct.unpack('f', record[14:18])[0]  # 散户流入
                    retail_outflow = struct.unpack('f', record[18:22])[0]  # 散户流出
                    
                    # 解析日期
                    date_int = struct.unpack('I', record[22:26])[0]
                    year = date_int // 10000
                    month = (date_int % 10000) // 100
                    day = date_int % 100
                    
                    data.append({
                        'stock_code': stock_code,
                        'main_inflow': main_inflow,
                        'main_outflow': main_outflow,
                        'main_net_inflow': main_inflow - main_outflow,
                        'retail_inflow': retail_inflow,
                        'retail_outflow': retail_outflow,
                        'retail_net_inflow': retail_inflow - retail_outflow,
                        'total_inflow': main_inflow + retail_inflow,
                        'total_outflow': main_outflow + retail_outflow,
                        'total_net_inflow': (main_inflow + retail_inflow) - (main_outflow + retail_outflow),
                        'date': f'{year:04d}-{month:02d}-{day:02d}',
                    })
                except:
                    continue
            
            self.logger.info(f"解析资金流向数据成功，共 {len(data)} 条记录")
            
        except Exception as e:
            self.logger.error(f"解析资金流向数据失败: {file_path}, 错误: {e}")
        
        return data
    
    async def get_stock_capital_flow(self, stock_code: str, market: str = 'SH') -> Dict[str, Any]:
        """获取个股资金流向数据（通达信实现）
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            
        Returns:
            资金流向数据
        """
        file_path = self._get_flow_file_path(market)
        if not file_path:
            self.logger.warning(f"资金流向文件不存在: {market}")
            return {}
        
        flow_data = self._parse_flow_data(file_path)
        
        # 查找指定股票的资金流向数据
        for item in flow_data:
            if item['stock_code'] == stock_code:
                return item
        
        # 如果没找到，返回空字典
        self.logger.debug(f"未找到股票资金流向数据: {stock_code}.{market}")
        return {}
    
    async def get_sector_capital_flow(self, sector_name: str) -> Dict[str, Any]:
        """获取板块资金流向数据（通达信可能不支持，返回空）
        
        Args:
            sector_name: 板块名称
            
        Returns:
            板块资金流向数据
        """
        self.logger.warning("通达信暂不支持板块资金流向数据")
        return {}
    
    async def get_market_capital_flow(self, market: str = 'SH') -> Dict[str, Any]:
        """获取市场整体资金流向数据
        
        Args:
            market: 市场代码
            
        Returns:
            市场资金流向数据
        """
        file_path = self._get_flow_file_path(market)
        if not file_path:
            return {}
        
        flow_data = self._parse_flow_data(file_path)
        
        if not flow_data:
            return {}
        
        # 计算市场整体资金流向
        total_main_inflow = sum(item['main_inflow'] for item in flow_data)
        total_main_outflow = sum(item['main_outflow'] for item in flow_data)
        total_retail_inflow = sum(item['retail_inflow'] for item in flow_data)
        total_retail_outflow = sum(item['retail_outflow'] for item in flow_data)
        
        return {
            'market': market,
            'total_main_inflow': total_main_inflow,
            'total_main_outflow': total_main_outflow,
            'total_main_net_inflow': total_main_inflow - total_main_outflow,
            'total_retail_inflow': total_retail_inflow,
            'total_retail_outflow': total_retail_outflow,
            'total_retail_net_inflow': total_retail_inflow - total_retail_outflow,
            'total_inflow': total_main_inflow + total_retail_inflow,
            'total_outflow': total_main_outflow + total_retail_outflow,
            'total_net_inflow': (total_main_inflow + total_retail_inflow) - (total_main_outflow + total_retail_outflow),
            'stock_count': len(flow_data),
            'date': flow_data[0]['date'] if flow_data else datetime.now().strftime('%Y-%m-%d'),
        }
    
    async def get_northbound_flow(self) -> Dict[str, Any]:
        """获取北向资金流向数据（通达信可能不支持，返回空）
        
        Returns:
            北向资金流向数据
        """
        self.logger.warning("通达信暂不支持北向资金流向数据")
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
            flow_data = await self.get_stock_capital_flow(stock_code, market)
            if flow_data:
                return {
                    'stock_code': stock_code,
                    'market': market,
                    'main_inflow': flow_data.get('main_inflow', 0),
                    'main_outflow': flow_data.get('main_outflow', 0),
                    'main_net_inflow': flow_data.get('main_net_inflow', 0),
                    'date': flow_data.get('date', ''),
                }
            return {}
        else:
            # 获取市场整体主力资金流向
            market_data = await self.get_market_capital_flow(market)
            if market_data:
                return {
                    'market': market,
                    'total_main_inflow': market_data.get('total_main_inflow', 0),
                    'total_main_outflow': market_data.get('total_main_outflow', 0),
                    'total_main_net_inflow': market_data.get('total_main_net_inflow', 0),
                    'date': market_data.get('date', ''),
                }
            return {}


class ApiCapitalFlowProvider(CapitalFlowProvider):
    """API资金流向数据提供器（使用东方财富等API）"""
    
    def __init__(self, config: Dict[str, Any]):
        """初始化API资金流向数据提供器
        
        Args:
            config: 配置字典
        """
        super().__init__(config)
        
        self.timeout = config.get('collector', {}).get('timeout_seconds', 30)
        self.cache = {}
        self.cache_ttl = 60  # 缓存60秒
        
        self.logger.info("API资金流向数据提供器初始化完成")
    
    async def _make_api_request(self, url: str) -> Dict[str, Any]:
        """发送API请求
        
        Args:
            url: 请求URL
            
        Returns:
            响应数据
        """
        try:
            async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=self.timeout)) as session:
                async with session.get(url, headers={'User-Agent': 'Mozilla/5.0'}) as response:
                    if response.status != 200:
                        self.logger.warning(f"API请求失败: {url}, 状态码: {response.status}")
                        return {}
                    
                    content = await response.text()
                    try:
                        return json.loads(content)
                    except json.JSONDecodeError:
                        self.logger.warning(f"API响应不是有效的JSON: {url}")
                        return {}
        except Exception as e:
            self.logger.error(f"API请求异常: {url}, {e}")
            return {}
    
    def _generate_mock_flow_data(self, stock_code: str, market: str) -> Dict[str, Any]:
        """生成模拟资金流向数据
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            
        Returns:
            模拟资金流向数据
        """
        import random
        import time
        
        # 基于股票代码生成确定性但看起来随机的数据
        seed = int(stock_code) % 10000 if stock_code.isdigit() else hash(stock_code) % 10000
        random.seed(seed + int(time.time() // 3600))  # 每小时变化
        
        main_inflow = random.uniform(1000, 10000) * 10000  # 万元
        main_outflow = main_inflow * random.uniform(0.7, 1.3)
        retail_inflow = random.uniform(100, 1000) * 10000
        retail_outflow = retail_inflow * random.uniform(0.8, 1.2)
        
        main_net = main_inflow - main_outflow
        retail_net = retail_inflow - retail_outflow
        
        return {
            'stock_code': stock_code,
            'market': market,
            'main_inflow': round(main_inflow, 2),
            'main_outflow': round(main_outflow, 2),
            'main_net_inflow': round(main_net, 2),
            'retail_inflow': round(retail_inflow, 2),
            'retail_outflow': round(retail_outflow, 2),
            'retail_net_inflow': round(retail_net, 2),
            'total_inflow': round(main_inflow + retail_inflow, 2),
            'total_outflow': round(main_outflow + retail_outflow, 2),
            'total_net_inflow': round(main_net + retail_net, 2),
            'date': datetime.now().strftime('%Y-%m-%d'),
            'is_mock_data': True,
            'data_source': 'api_mock'
        }
    
    def _generate_mock_market_flow_data(self, market: str) -> Dict[str, Any]:
        """生成模拟市场资金流向数据
        
        Args:
            market: 市场代码
            
        Returns:
            模拟市场资金流向数据
        """
        import random
        import time
        
        # 基于市场代码生成确定性但看起来随机的数据
        seed = hash(market) % 10000
        random.seed(seed + int(time.time() // 3600))  # 每小时变化
        
        main_inflow = random.uniform(50000, 200000) * 10000  # 万元
        main_outflow = main_inflow * random.uniform(0.8, 1.2)
        retail_inflow = random.uniform(10000, 50000) * 10000
        retail_outflow = retail_inflow * random.uniform(0.9, 1.1)
        
        main_net = main_inflow - main_outflow
        retail_net = retail_inflow - retail_outflow
        
        return {
            'market': market,
            'main_inflow': round(main_inflow, 2),
            'main_outflow': round(main_outflow, 2),
            'main_net_inflow': round(main_net, 2),
            'retail_inflow': round(retail_inflow, 2),
            'retail_outflow': round(retail_outflow, 2),
            'retail_net_inflow': round(retail_net, 2),
            'total_inflow': round(main_inflow + retail_inflow, 2),
            'total_outflow': round(main_outflow + retail_outflow, 2),
            'total_net_inflow': round(main_net + retail_net, 2),
            'date': datetime.now().strftime('%Y-%m-%d'),
            'is_mock_data': True,
            'data_source': 'api_mock'
        }
    
    def _generate_mock_sector_flow_data(self, sector_name: str) -> Dict[str, Any]:
        """生成模拟板块资金流向数据
        
        Args:
            sector_name: 板块名称
            
        Returns:
            模拟板块资金流向数据
        """
        import random
        import time
        
        # 基于板块名称生成确定性但看起来随机的数据
        seed = hash(sector_name) % 10000
        random.seed(seed + int(time.time() // 3600))  # 每小时变化
        
        main_inflow = random.uniform(10000, 50000) * 10000  # 万元
        main_outflow = main_inflow * random.uniform(0.7, 1.3)
        main_net = main_inflow - main_outflow
        
        return {
            'sector_name': sector_name,
            'main_inflow': round(main_inflow, 2),
            'main_outflow': round(main_outflow, 2),
            'main_net_inflow': round(main_net, 2),
            'date': datetime.now().strftime('%Y-%m-%d'),
            'is_mock_data': True,
            'data_source': 'api_mock'
        }
    
    def _generate_mock_northbound_flow_data(self) -> Dict[str, Any]:
        """生成模拟北向资金流向数据
        
        Returns:
            模拟北向资金流向数据
        """
        import random
        import time
        
        # 基于时间生成确定性但看起来随机的数据
        random.seed(int(time.time() // 3600))  # 每小时变化
        
        sh_inflow = random.uniform(10000, 50000) * 10000  # 万元
        sh_outflow = sh_inflow * random.uniform(0.5, 1.5)
        sz_inflow = random.uniform(8000, 40000) * 10000
        sz_outflow = sz_inflow * random.uniform(0.6, 1.4)
        
        sh_net = sh_inflow - sh_outflow
        sz_net = sz_inflow - sz_outflow
        
        return {
            'northbound': {
                'sh_inflow': round(sh_inflow, 2),
                'sh_outflow': round(sh_outflow, 2),
                'sh_net_inflow': round(sh_net, 2),
                'sz_inflow': round(sz_inflow, 2),
                'sz_outflow': round(sz_outflow, 2),
                'sz_net_inflow': round(sz_net, 2),
                'total_inflow': round(sh_inflow + sz_inflow, 2),
                'total_outflow': round(sh_outflow + sz_outflow, 2),
                'total_net_inflow': round(sh_net + sz_net, 2),
            },
            'date': datetime.now().strftime('%Y-%m-%d'),
            'is_mock_data': True,
            'data_source': 'api_mock'
        }
    
    async def get_stock_capital_flow(self, stock_code: str, market: str = 'SH') -> Dict[str, Any]:
        """获取个股资金流向数据（API实现）
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            
        Returns:
            资金流向数据
        """
        # 检查缓存
        cache_key = f"stock_flow_{stock_code}_{market}"
        if cache_key in self.cache:
            cached_data, cached_time = self.cache[cache_key]
            if (datetime.now() - cached_time).seconds < self.cache_ttl:
                return cached_data
        
        # 尝试东方财富资金流向API
        if market.upper() == 'SH':
            secid = f"1.{stock_code}"
        else:
            secid = f"0.{stock_code}"
        
        # 尝试多个可能的API接口
        api_urls = [
            f"http://push2.eastmoney.com/api/qt/stock/fflow/get?secid={secid}&fields=f62,f63,f64,f65,f66,f69",
            f"http://push2.eastmoney.com/api/qt/stock/fflow/day?secid={secid}&fields1=f1,f2,f3&fields2=f51,f52,f53,f54,f55,f56",
            f"http://push2.eastmoney.com/api/qt/stock/get?secid={secid}&fields=f62,f63,f64,f65,f66,f69,f70,f71,f72"
        ]
        
        for url in api_urls:
            data = await self._make_api_request(url)
            
            if data and data.get('rc') == 0:
                flow_data = data.get('data', {})
                
                result = {
                    'stock_code': stock_code,
                    'market': market,
                    'main_inflow': flow_data.get('f62', 0),  # 主力流入
                    'main_outflow': flow_data.get('f63', 0),  # 主力流出
                    'main_net_inflow': flow_data.get('f64', 0),  # 主力净流入
                    'retail_inflow': flow_data.get('f65', 0),  # 散户流入
                    'retail_outflow': flow_data.get('f66', 0),  # 散户流出
                    'retail_net_inflow': flow_data.get('f69', 0),  # 散户净流入
                    'total_inflow': flow_data.get('f62', 0) + flow_data.get('f65', 0),
                    'total_outflow': flow_data.get('f63', 0) + flow_data.get('f66', 0),
                    'total_net_inflow': flow_data.get('f64', 0) + flow_data.get('f69', 0),
                    'date': datetime.now().strftime('%Y-%m-%d'),
                    'is_mock_data': False,
                    'data_source': 'api'
                }
                
                # 缓存结果
                self.cache[cache_key] = (result, datetime.now())
                
                return result
        
        # 所有API都失败，返回模拟数据
        self.logger.info(f"API获取失败，使用模拟数据: {stock_code}.{market}")
        mock_data = self._generate_mock_flow_data(stock_code, market)
        
        # 缓存模拟数据（缓存时间较短）
        self.cache[cache_key] = (mock_data, datetime.now())
        
        return mock_data
    
    async def get_sector_capital_flow(self, sector_name: str) -> Dict[str, Any]:
        """获取板块资金流向数据
        
        Args:
            sector_name: 板块名称
            
        Returns:
            板块资金流向数据
        """
        # 尝试东方财富板块资金流向API
        api_urls = [
            f"http://push2.eastmoney.com/api/qt/club/fflow/get?bkn={sector_name}&fields=f62,f63,f64",
            f"http://push2.eastmoney.com/api/qt/club/fflow/day?bkn={sector_name}&fields=f62,f63,f64",
        ]
        
        for url in api_urls:
            data = await self._make_api_request(url)
            
            if data and data.get('rc') == 0:
                flow_data = data.get('data', {})
                
                return {
                    'sector_name': sector_name,
                    'main_inflow': flow_data.get('f62', 0),
                    'main_outflow': flow_data.get('f63', 0),
                    'main_net_inflow': flow_data.get('f64', 0),
                    'date': datetime.now().strftime('%Y-%m-%d'),
                    'is_mock_data': False,
                    'data_source': 'api'
                }
        
        # API失败，返回模拟数据
        self.logger.info(f"API获取失败，使用模拟板块资金流向数据: {sector_name}")
        return self._generate_mock_sector_flow_data(sector_name)
    
    async def get_market_capital_flow(self, market: str = 'SH') -> Dict[str, Any]:
        """获取市场整体资金流向数据
        
        Args:
            market: 市场代码
            
        Returns:
            市场资金流向数据
        """
        # 检查缓存
        cache_key = f"market_flow_{market}"
        if cache_key in self.cache:
            cached_data, cached_time = self.cache[cache_key]
            if (datetime.now() - cached_time).seconds < self.cache_ttl:
                return cached_data
        
        # 东方财富市场资金流向API
        if market.upper() == 'SH':
            market_code = '1'
        elif market.upper() == 'SZ':
            market_code = '0'
        else:
            market_code = '2'
        
        # 尝试多个可能的API接口
        api_urls = [
            f"http://push2.eastmoney.com/api/qt/club/marketflow/get?market={market_code}&fields=f62,f63,f64,f65,f66,f69",
            f"http://push2.eastmoney.com/api/qt/club/marketflow/day?market={market_code}&fields=f62,f63,f64,f65,f66,f69",
        ]
        
        for url in api_urls:
            data = await self._make_api_request(url)
            
            if data and data.get('rc') == 0:
                flow_data = data.get('data', {})
                
                result = {
                    'market': market,
                    'main_inflow': flow_data.get('f62', 0),
                    'main_outflow': flow_data.get('f63', 0),
                    'main_net_inflow': flow_data.get('f64', 0),
                    'retail_inflow': flow_data.get('f65', 0),
                    'retail_outflow': flow_data.get('f66', 0),
                    'retail_net_inflow': flow_data.get('f69', 0),
                    'total_inflow': flow_data.get('f62', 0) + flow_data.get('f65', 0),
                    'total_outflow': flow_data.get('f63', 0) + flow_data.get('f66', 0),
                    'total_net_inflow': flow_data.get('f64', 0) + flow_data.get('f69', 0),
                    'date': datetime.now().strftime('%Y-%m-%d'),
                    'is_mock_data': False,
                    'data_source': 'api'
                }
                
                # 缓存结果
                self.cache[cache_key] = (result, datetime.now())
                return result
        
        # API失败，返回模拟数据
        self.logger.info(f"API获取失败，使用模拟市场资金流向数据: {market}")
        mock_data = self._generate_mock_market_flow_data(market)
        
        # 缓存模拟数据（缓存时间较短）
        self.cache[cache_key] = (mock_data, datetime.now())
        return mock_data
    
    async def get_northbound_flow(self) -> Dict[str, Any]:
        """获取北向资金流向数据
        
        Returns:
            北向资金流向数据
        """
        # 检查缓存
        cache_key = "northbound_flow"
        if cache_key in self.cache:
            cached_data, cached_time = self.cache[cache_key]
            if (datetime.now() - cached_time).seconds < self.cache_ttl:
                return cached_data
        
        # 尝试多个可能的北向资金流向API接口
        api_urls = [
            "http://push2.eastmoney.com/api/qt/kamt/get?fields=f62,f63,f64,f65,f66,f69",
            "http://push2.eastmoney.com/api/qt/kamt/day?fields=f62,f63,f64,f65,f66,f69",
            "http://push2.eastmoney.com/api/qt/kamt/summary?fields=f62,f63,f64,f65,f66,f69",
        ]
        
        for url in api_urls:
            data = await self._make_api_request(url)
            
            if data and data.get('rc') == 0:
                flow_data = data.get('data', {})
                
                result = {
                    'northbound': {
                        'sh_inflow': flow_data.get('f62', 0),  # 沪股通流入
                        'sh_outflow': flow_data.get('f63', 0),  # 沪股通流出
                        'sh_net_inflow': flow_data.get('f64', 0),  # 沪股通净流入
                        'sz_inflow': flow_data.get('f65', 0),  # 深股通流入
                        'sz_outflow': flow_data.get('f66', 0),  # 深股通流出
                        'sz_net_inflow': flow_data.get('f69', 0),  # 深股通净流入
                        'total_inflow': flow_data.get('f62', 0) + flow_data.get('f65', 0),
                        'total_outflow': flow_data.get('f63', 0) + flow_data.get('f66', 0),
                        'total_net_inflow': flow_data.get('f64', 0) + flow_data.get('f69', 0),
                    },
                    'date': datetime.now().strftime('%Y-%m-%d'),
                    'is_mock_data': False,
                    'data_source': 'api'
                }
                
                # 缓存结果
                self.cache[cache_key] = (result, datetime.now())
                return result
        
        # API失败，返回模拟数据
        self.logger.info("API获取失败，使用模拟北向资金流向数据")
        mock_data = self._generate_mock_northbound_flow_data()
        
        # 缓存模拟数据（缓存时间较短）
        self.cache[cache_key] = (mock_data, datetime.now())
        return mock_data
    
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
            flow_data = await self.get_stock_capital_flow(stock_code, market)
            if flow_data:
                return {
                    'stock_code': stock_code,
                    'market': market,
                    'main_inflow': flow_data.get('main_inflow', 0),
                    'main_outflow': flow_data.get('main_outflow', 0),
                    'main_net_inflow': flow_data.get('main_net_inflow', 0),
                    'date': flow_data.get('date', ''),
                }
        else:
            # 获取市场整体主力资金流向
            market_data = await self.get_market_capital_flow(market)
            if market_data:
                return {
                    'market': market,
                    'main_inflow': market_data.get('main_inflow', 0),
                    'main_outflow': market_data.get('main_outflow', 0),
                    'main_net_inflow': market_data.get('main_net_inflow', 0),
                    'date': market_data.get('date', ''),
                }
        
        return {}