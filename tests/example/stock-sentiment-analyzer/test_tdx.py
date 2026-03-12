#!/usr/bin/env python3
"""
通达信数据读取器测试脚本
测试通达信本地行情数据读取功能
"""

import os
import sys
import asyncio
from pathlib import Path

# 添加项目根目录到路径
sys.path.insert(0, str(Path(__file__).parent))

from src.data.tdx_data_provider import TdxDataProvider
from src.data.integrated_data_provider import IntegratedDataProvider


async def test_tdx_provider():
    """测试通达信数据提供器"""
    print("=" * 60)
    print("通达信数据提供器测试")
    print("=" * 60)
    
    # 模拟配置
    config = {
        'tdx': {
            'data_path': 'C:/zd_zsone',
            'enabled': True,
            'default_market': 'SH',
            'cache_enabled': True,
            'cache_ttl': 300
        }
    }
    
    tdx_provider = TdxDataProvider(config)
    
    # 测试通达信数据目录是否存在
    data_path = tdx_provider.data_path
    if not os.path.exists(data_path):
        print(f"警告: 通达信数据目录不存在: {data_path}")
        print("请确保通达信已安装，并设置正确的 data_path")
        print("跳过具体数据读取测试")
        return False
    
    print(f"通达信数据目录: {data_path}")
    
    try:
        # 1. 测试获取股票列表
        print("\n1. 测试获取股票列表...")
        stock_list = tdx_provider.get_stock_list('SH')
        print(f"   上海市场股票数量: {len(stock_list)}")
        if stock_list:
            print(f"   示例股票: {stock_list[0]}")
        
        # 2. 测试获取股票信息
        print("\n2. 测试获取股票信息...")
        if stock_list:
            sample_code = stock_list[0]['code']
            stock_info = tdx_provider.get_stock_info(sample_code, 'SH')
            print(f"   股票 {sample_code} 信息:")
            for key, value in list(stock_info.items())[:5]:
                print(f"     {key}: {value}")
        
        # 3. 测试获取日线数据
        print("\n3. 测试获取日线数据...")
        if stock_list:
            day_data = tdx_provider.get_stock_day_data(sample_code, 'SH')
            print(f"   日线数据数量: {len(day_data)}")
            if day_data:
                print(f"   最新日线数据: {day_data[-1]}")
        
        # 4. 测试获取板块列表
        print("\n4. 测试获取板块列表...")
        sectors = tdx_provider.get_sector_list()
        print(f"   板块数量: {len(sectors)}")
        if sectors:
            print(f"   示例板块: {sectors[0]}")
        
        return True
        
    except Exception as e:
        print(f"测试过程中出现异常: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_integrated_provider():
    """测试集成数据提供器"""
    print("\n" + "=" * 60)
    print("集成数据提供器测试")
    print("=" * 60)
    
    # 模拟配置
    config = {
        'tdx': {
            'data_path': 'C:/zd_zsone',
            'enabled': True,
            'default_market': 'SH',
            'cache_enabled': True,
            'cache_ttl': 300
        },
        'data_source_priority': ['api', 'tdx']
    }
    
    provider = IntegratedDataProvider(config)
    
    # 测试数据源状态
    print("\n1. 数据源状态:")
    status = provider.get_data_source_status()
    for key, value in status.items():
        print(f"   {key}: {value}")
    
    # 2. 测试获取股票信息（使用降级策略）
    print("\n2. 测试获取股票信息（降级策略）...")
    try:
        stock_info = await provider.get_stock_info('600036', 'SH')
        if stock_info:
            print(f"   获取成功，数据源: {stock_info.get('data_source', 'unknown')}")
            print(f"   股票名称: {stock_info.get('name')}")
            print(f"   当前价格: {stock_info.get('current_price')}")
        else:
            print("   获取失败")
    except Exception as e:
        print(f"   获取股票信息异常: {e}")
    
    # 3. 测试获取板块列表
    print("\n3. 测试获取板块列表...")
    sectors = provider.get_sector_list()
    print(f"   板块数量: {len(sectors)}")
    
    # 4. 测试多周期数据
    print("\n4. 测试多周期数据...")
    multi_data = provider.get_multi_period_data('600036', 'SH')
    for period, data in multi_data.items():
        print(f"   {period}周期数据数量: {len(data)}")
    
    return True


async def main():
    """主测试函数"""
    print("股票舆情分析系统 - 通达信数据集成测试")
    print("=" * 60)
    
    # 测试通达信数据提供器
    tdx_success = await test_tdx_provider()
    
    # 测试集成数据提供器
    integrated_success = await test_integrated_provider()
    
    print("\n" + "=" * 60)
    print("测试总结")
    print("=" * 60)
    
    if tdx_success:
        print("✓ 通达信数据提供器测试通过")
    else:
        print("✗ 通达信数据提供器测试未完成（可能数据目录不存在）")
    
    if integrated_success:
        print("✓ 集成数据提供器测试通过")
    else:
        print("✗ 集成数据提供器测试失败")
    
    print("\n提示:")
    print("1. 如果通达信数据目录不存在，请修改配置中的 data_path")
    print("2. 确保通达信已安装并下载了行情数据")
    print("3. 集成数据提供器会自动降级，API失败时会尝试本地数据")


if __name__ == "__main__":
    asyncio.run(main())