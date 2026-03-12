"""
通达信数据提供器使用示例
"""

import os
import sys
import asyncio
from datetime import datetime, timedelta

# 添加项目根目录到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.utils.config_loader import ConfigLoader
from src.data.tdx_data_provider import TdxDataProvider


async def main():
    """主函数"""
    print("=== 通达信数据提供器使用示例 ===")
    
    # 1. 加载配置
    print("\n1. 加载配置...")
    config_loader = ConfigLoader()
    config = config_loader.load()
    
    # 2. 初始化通达信数据提供器
    print("2. 初始化通达信数据提供器...")
    tdx_provider = TdxDataProvider(config)
    
    # 3. 获取股票列表
    print("\n3. 获取上海市场股票列表...")
    sh_stocks = tdx_provider.get_stock_list('SH')
    print(f"   上海市场共有 {len(sh_stocks)} 只股票")
    if sh_stocks:
        print(f"   示例股票: {sh_stocks[:3]}")
    
    # 4. 获取概念板块列表
    print("\n4. 获取概念板块列表...")
    sectors = tdx_provider.get_sector_list()
    print(f"   共有 {len(sectors)} 个概念板块")
    if sectors:
        for i, sector in enumerate(sectors[:5]):
            print(f"   {i+1}. {sector['name']} ({len(sector.get('stocks', []))} 只股票)")
    
    # 5. 获取股票日线数据
    print("\n5. 获取股票日线数据...")
    # 使用招商银行作为示例 (600036.SH)
    day_data = tdx_provider.get_stock_day_data('600036', 'SH')
    if day_data:
        print(f"   招商银行日线数据共有 {len(day_data)} 条记录")
        print(f"   最新数据: {day_data[-1]}")
        
        # 获取最近30天的数据
        end_date = datetime.now().strftime('%Y-%m-%d')
        start_date = (datetime.now() - timedelta(days=30)).strftime('%Y-%m-%d')
        recent_data = tdx_provider.get_stock_day_data('600036', 'SH', start_date, end_date)
        print(f"   最近30天数据: {len(recent_data)} 条记录")
    
    # 6. 获取股票分钟线数据
    print("\n6. 获取股票分钟线数据...")
    minute_data = tdx_provider.get_stock_minute_data('600036', 'SH', period=5)
    if minute_data:
        print(f"   招商银行5分钟线数据共有 {len(minute_data)} 条记录")
        if minute_data:
            print(f"   最新5分钟线: {minute_data[-1]}")
    
    # 7. 获取周线和月线数据
    print("\n7. 获取股票周线数据...")
    week_data = tdx_provider.get_stock_week_data('600036', 'SH')
    if week_data:
        print(f"   招商银行周线数据共有 {len(week_data)} 条记录")
    
    print("\n8. 获取股票月线数据...")
    month_data = tdx_provider.get_stock_month_data('600036', 'SH')
    if month_data:
        print(f"   招商银行月线数据共有 {len(month_data)} 条记录")
    
    # 8. 计算技术指标
    print("\n9. 计算技术指标...")
    indicators = tdx_provider.calculate_technical_indicators('600036', 'SH', 'day')
    if indicators:
        print(f"   技术指标计算完成:")
        print(f"   MA指标: {indicators.get('ma', {})}")
        print(f"   MACD: {indicators.get('macd', {})}")
        print(f"   RSI: {indicators.get('rsi', 0)}")
        print(f"   KDJ: {indicators.get('kdj', {})}")
    
    # 9. 获取股票基本信息
    print("\n10. 获取股票基本信息...")
    stock_info = tdx_provider.get_stock_info('600036', 'SH')
    if stock_info:
        print(f"   股票基本信息:")
        print(f"   代码: {stock_info.get('code')}")
        print(f"   名称: {stock_info.get('name')}")
        print(f"   当前价格: {stock_info.get('current_price')}")
        print(f"   涨跌幅: {stock_info.get('change_pct')}%")
        print(f"   成交量: {stock_info.get('volume')}")
        print(f"   成交额: {stock_info.get('amount')}")
    
    # 10. 获取指定板块的股票
    print("\n11. 获取人工智能板块股票...")
    ai_stocks = tdx_provider.get_sector_stocks('人工智能')
    if ai_stocks:
        print(f"   人工智能板块共有 {len(ai_stocks)} 只股票")
        if ai_stocks:
            print(f"   示例股票: {ai_stocks[:3]}")
    else:
        print("   未找到人工智能板块数据，尝试其他板块...")
        for sector in sectors:
            if '科技' in sector['name'] or '智能' in sector['name']:
                sector_stocks = sector.get('stocks', [])
                print(f"   {sector['name']}板块: {len(sector_stocks)} 只股票")
                if sector_stocks:
                    print(f"   示例股票: {sector_stocks[:3]}")
                break
    
    print("\n=== 示例运行完成 ===")
    
    # 11. 与现有StockDataProvider对比
    print("\n12. 与现有StockDataProvider集成建议:")
    print("""
    通达信数据提供器可以与现有项目通过以下方式集成:
    
    1. 作为备用数据源:
       - 当网络API不可用时，使用本地通达信数据
       - 提供历史数据补充
    
    2. 增强数据源:
       - 提供分钟级别数据，用于高频分析
       - 提供概念板块数据，用于板块轮动分析
    
    3. 技术分析:
       - 使用本地数据计算技术指标，减少API调用
       - 提供多周期技术分析
    
    4. 集成方式:
       - 修改src/data/stock_data_provider.py，增加通达信数据源选项
       - 在配置文件中添加数据源优先级设置
       - 创建统一的数据接口层
    """)


if __name__ == "__main__":
    asyncio.run(main())