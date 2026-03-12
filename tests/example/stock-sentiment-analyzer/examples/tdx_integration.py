"""
通达信数据提供器与现有项目集成示例
展示如何将通达信数据与现有股票数据提供器结合使用
"""

import os
import sys
import asyncio
from datetime import datetime, timedelta

# 添加项目根目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.utils.config_loader import ConfigLoader
from src.data.stock_data_provider import StockDataProvider
from src.data.tdx_data_provider import TdxDataProvider


class IntegratedDataProvider:
    """集成数据提供器，结合网络API和本地通达信数据"""
    
    def __init__(self, config: dict):
        """初始化集成数据提供器
        
        Args:
            config: 配置字典
        """
        self.config = config
        self.use_tdx = config.get('tdx', {}).get('enabled', False)
        
        # 初始化两个数据提供器
        self.stock_provider = StockDataProvider(config)
        
        if self.use_tdx:
            self.tdx_provider = TdxDataProvider(config)
        else:
            self.tdx_provider = None
        
        print(f"集成数据提供器初始化完成，通达信数据源: {'启用' if self.use_tdx else '禁用'}")
    
    async def get_stock_info_with_fallback(self, stock_code: str, market: str = 'SH') -> dict:
        """获取股票信息，支持降级策略
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            
        Returns:
            股票信息字典
        """
        # 首先尝试从网络API获取
        try:
            print(f"尝试从网络API获取 {stock_code}.{market} 信息...")
            api_info = await self.stock_provider.get_stock_info(stock_code)
            
            if api_info and api_info.get('current_price', 0) > 0:
                print(f"网络API获取成功: {api_info.get('name')} - {api_info.get('current_price')}")
                api_info['data_source'] = 'api'
                return api_info
        
        except Exception as e:
            print(f"网络API获取失败: {e}")
        
        # 如果网络API失败，尝试使用通达信数据
        if self.use_tdx and self.tdx_provider:
            try:
                print(f"尝试从通达信数据获取 {stock_code}.{market} 信息...")
                tdx_info = self.tdx_provider.get_stock_info(stock_code, market)
                
                if tdx_info and tdx_info.get('current_price', 0) > 0:
                    print(f"通达信数据获取成功: {tdx_info.get('name')} - {tdx_info.get('current_price')}")
                    tdx_info['data_source'] = 'tdx'
                    return tdx_info
            
            except Exception as e:
                print(f"通达信数据获取失败: {e}")
        
        print(f"所有数据源获取失败: {stock_code}.{market}")
        return {}
    
    async def get_stock_kline_with_sources(self, stock_code: str, market: str = 'SH', 
                                         period: str = '1d', count: int = 100) -> list:
        """获取股票K线数据，结合多个数据源
        
        Args:
            stock_code: 股票代码
            market: 市场代码
            period: 周期 ('1d', '1w', '1m', 'min')
            count: 数据条数
            
        Returns:
            K线数据列表
        """
        kline_data = []
        
        # 根据周期选择数据源
        if period == '1d':
            # 日线数据，优先使用通达信（历史数据更完整）
            if self.use_tdx and self.tdx_provider:
                print(f"从通达信获取日线数据: {stock_code}.{market}")
                tdx_data = self.tdx_provider.get_stock_day_data(stock_code, market)
                if tdx_data:
                    # 转换为统一格式
                    for item in tdx_data[-count:]:
                        kline_data.append({
                            'date': item['date'],
                            'open': item['open'],
                            'close': item['close'],
                            'high': item['high'],
                            'low': item['low'],
                            'volume': item['volume']
                        })
                    print(f"通达信日线数据: {len(kline_data)} 条")
        
        elif period == '1w':
            # 周线数据，使用通达信
            if self.use_tdx and self.tdx_provider:
                print(f"从通达信获取周线数据: {stock_code}.{market}")
                tdx_data = self.tdx_provider.get_stock_week_data(stock_code, market)
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
        
        elif period == '1m':
            # 月线数据，使用通达信
            if self.use_tdx and self.tdx_provider:
                print(f"从通达信获取月线数据: {stock_code}.{market}")
                tdx_data = self.tdx_provider.get_stock_month_data(stock_code, market)
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
        
        elif period == 'min':
            # 分钟线数据，使用通达信
            if self.use_tdx and self.tdx_provider:
                print(f"从通达信获取分钟线数据: {stock_code}.{market}")
                tdx_data = self.tdx_provider.get_stock_minute_data(stock_code, market, period=5)
                if tdx_data:
                    for item in tdx_data[-count:]:
                        kline_data.append({
                            'datetime': item['datetime'],
                            'open': item['open'],
                            'close': item['close'],
                            'high': item['high'],
                            'low': item['low'],
                            'volume': item['volume']
                        })
        
        # 如果通达信数据不足，使用网络API作为补充
        if len(kline_data) < count and period == '1d':
            try:
                print(f"补充网络API日线数据: {stock_code}")
                api_data = await self.stock_provider.get_stock_kline(stock_code, period, count)
                if api_data:
                    # 合并数据，去重
                    existing_dates = {item['date'] for item in kline_data}
                    for item in api_data:
                        if item['date'] not in existing_dates:
                            kline_data.append(item)
                            existing_dates.add(item['date'])
                    
                    # 限制数量
                    kline_data = kline_data[-count:]
                    
                    print(f"补充后总数据: {len(kline_data)} 条")
            
            except Exception as e:
                print(f"网络API补充失败: {e}")
        
        return kline_data
    
    def get_sector_analysis(self, sector_name: str) -> dict:
        """获取板块分析数据
        
        Args:
            sector_name: 板块名称
            
        Returns:
            板块分析数据
        """
        if not self.use_tdx or not self.tdx_provider:
            return {'error': '通达信数据源未启用'}
        
        # 获取板块股票列表
        sector_stocks = self.tdx_provider.get_sector_stocks(sector_name)
        if not sector_stocks:
            return {'error': f'未找到板块: {sector_name}'}
        
        print(f"分析板块: {sector_name}, 包含 {len(sector_stocks)} 只股票")
        
        # 获取板块内股票的基本信息（简化示例）
        stock_infos = []
        sample_stocks = sector_stocks[:5]  # 只分析前5只股票作为示例
        
        for stock in sample_stocks:
            stock_code = stock['code']
            # 简单判断市场
            market = 'SH' if stock_code.startswith('6') else 'SZ'
            
            try:
                stock_info = self.tdx_provider.get_stock_info(stock_code, market)
                if stock_info:
                    stock_infos.append(stock_info)
                    print(f"   {stock['name']}({stock_code}): {stock_info.get('current_price')}")
            except Exception as e:
                print(f"   获取股票信息失败 {stock_code}: {e}")
        
        # 计算板块平均指标
        if stock_infos:
            avg_price = sum(info.get('current_price', 0) for info in stock_infos) / len(stock_infos)
            avg_change = sum(info.get('change_pct', 0) for info in stock_infos) / len(stock_infos)
            
            return {
                'sector_name': sector_name,
                'stock_count': len(sector_stocks),
                'analyzed_count': len(stock_infos),
                'avg_price': round(avg_price, 2),
                'avg_change_pct': round(avg_change, 2),
                'sample_stocks': [
                    {
                        'code': info.get('code'),
                        'name': info.get('name'),
                        'price': info.get('current_price'),
                        'change_pct': info.get('change_pct')
                    }
                    for info in stock_infos[:3]
                ]
            }
        
        return {'error': '未能获取股票信息'}


async def main():
    """主函数"""
    print("=== 通达信数据提供器集成示例 ===")
    
    # 1. 加载配置
    print("\n1. 加载配置...")
    config_loader = ConfigLoader()
    config = config_loader.load()
    
    # 2. 初始化集成数据提供器
    print("\n2. 初始化集成数据提供器...")
    integrated_provider = IntegratedDataProvider(config)
    
    # 3. 测试降级策略
    print("\n3. 测试数据获取降级策略...")
    
    # 3.1 获取股票信息（优先使用网络API，失败时使用通达信）
    stock_info = await integrated_provider.get_stock_info_with_fallback('600036', 'SH')
    if stock_info:
        print(f"   获取成功: {stock_info.get('name')}")
        print(f"   数据源: {stock_info.get('data_source')}")
        print(f"   当前价: {stock_info.get('current_price')}")
        print(f"   涨跌幅: {stock_info.get('change_pct')}%")
    else:
        print("   获取失败")
    
    # 4. 测试多周期K线数据
    print("\n4. 测试多周期K线数据获取...")
    
    # 4.1 日线数据
    day_kline = await integrated_provider.get_stock_kline_with_sources('600036', 'SH', '1d', 20)
    print(f"   日线数据: {len(day_kline)} 条")
    if day_kline:
        print(f"   最新日线: {day_kline[-1]}")
    
    # 4.2 周线数据
    week_kline = await integrated_provider.get_stock_kline_with_sources('600036', 'SH', '1w', 10)
    print(f"   周线数据: {len(week_kline)} 条")
    
    # 4.3 月线数据
    month_kline = await integrated_provider.get_stock_kline_with_sources('600036', 'SH', '1m', 6)
    print(f"   月线数据: {len(month_kline)} 条")
    
    # 4.4 分钟线数据
    if integrated_provider.use_tdx:
        minute_kline = await integrated_provider.get_stock_kline_with_sources('600036', 'SH', 'min', 30)
        print(f"   分钟线数据: {len(minute_kline)} 条")
    
    # 5. 测试板块分析
    print("\n5. 测试板块分析功能...")
    
    if integrated_provider.use_tdx:
        # 获取板块列表
        sectors = integrated_provider.tdx_provider.get_sector_list()
        if sectors:
            # 分析第一个板块
            first_sector = sectors[0]['name']
            print(f"   分析板块: {first_sector}")
            
            sector_analysis = integrated_provider.get_sector_analysis(first_sector)
            if 'error' not in sector_analysis:
                print(f"   板块分析结果:")
                print(f"   股票数量: {sector_analysis.get('stock_count')}")
                print(f"   平均价格: {sector_analysis.get('avg_price')}")
                print(f"   平均涨跌幅: {sector_analysis.get('avg_change_pct')}%")
                print(f"   样本股票: {sector_analysis.get('sample_stocks')}")
            else:
                print(f"   板块分析失败: {sector_analysis.get('error')}")
    
    # 6. 与现有项目功能集成示例
    print("\n6. 与现有项目功能集成示例...")
    print("""
    现有股票情绪分析器的主要功能模块:
    
    1. 资讯采集 (collectors/)
       - 通达信数据可以作为技术面分析的补充数据源
       - 板块数据可用于资讯的板块分类
    
    2. 专家评估 (experts/)
       - 技术面分析专家可以使用通达信的多周期数据
       - 资金面分析专家可以使用分钟级别的成交量数据
    
    3. 短线决策 (trader/)
       - 使用通达信的分钟线数据进行高频交易信号检测
       - 利用板块数据进行板块轮动策略
    
    4. 报告生成 (reporter/)
       - 在报告中加入技术指标图表
       - 添加板块热度分析
    
    集成建议:
    - 创建统一的数据访问层，屏蔽数据源差异
    - 在配置中设置数据源优先级
    - 增加数据源健康检查，自动切换
    """)
    
    print("\n=== 集成示例运行完成 ===")


if __name__ == "__main__":
    asyncio.run(main())