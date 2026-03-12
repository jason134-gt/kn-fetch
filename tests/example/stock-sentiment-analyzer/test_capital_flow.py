#!/usr/bin/env python3
"""
资金流向数据测试脚本
测试通达信和API资金流向数据获取功能
"""

import asyncio
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from src.data.integrated_data_provider import IntegratedDataProvider
from src.utils.config_loader import load_config


async def test_capital_flow():
    """测试资金流向数据获取"""
    print("=== 资金流向数据测试 ===\n")
    
    # 加载配置
    config = load_config()
    
    # 初始化集成数据提供器
    provider = IntegratedDataProvider(config)
    
    # 测试数据源状态
    print("1. 数据源状态:")
    status = provider.get_data_source_status()
    for key, value in status.items():
        print(f"   {key}: {value}")
    print()
    
    # 测试个股资金流向
    print("2. 测试个股资金流向 (600036.SH 招商银行):")
    try:
        flow_data = await provider.get_stock_capital_flow('600036', 'SH')
        if flow_data:
            print(f"   数据源: {flow_data.get('data_source', '未知')}")
            print(f"   主力流入: {flow_data.get('main_inflow', 0):.2f} 万元")
            print(f"   主力流出: {flow_data.get('main_outflow', 0):.2f} 万元")
            print(f"   主力净流入: {flow_data.get('main_net_inflow', 0):.2f} 万元")
            print(f"   散户流入: {flow_data.get('retail_inflow', 0):.2f} 万元")
            print(f"   散户流出: {flow_data.get('retail_outflow', 0):.2f} 万元")
            print(f"   散户净流入: {flow_data.get('retail_net_inflow', 0):.2f} 万元")
            print(f"   总净流入: {flow_data.get('total_net_inflow', 0):.2f} 万元")
            print(f"   日期: {flow_data.get('date', '未知')}")
        else:
            print("   获取资金流向数据失败")
    except Exception as e:
        print(f"   测试失败: {e}")
    print()
    
    # 测试市场资金流向
    print("3. 测试市场资金流向 (SH 上海市场):")
    try:
        market_flow = await provider.get_market_capital_flow('SH')
        if market_flow:
            print(f"   数据源: {market_flow.get('data_source', '未知')}")
            print(f"   市场: {market_flow.get('market', '未知')}")
            print(f"   主力净流入: {market_flow.get('main_net_inflow', 0):.2f} 万元")
            print(f"   散户净流入: {market_flow.get('retail_net_inflow', 0):.2f} 万元")
            print(f"   总净流入: {market_flow.get('total_net_inflow', 0):.2f} 万元")
            print(f"   日期: {market_flow.get('date', '未知')}")
        else:
            print("   获取市场资金流向数据失败")
    except Exception as e:
        print(f"   测试失败: {e}")
    print()
    
    # 测试北向资金流向
    print("4. 测试北向资金流向:")
    try:
        northbound_flow = await provider.get_northbound_flow()
        if northbound_flow:
            print(f"   数据源: {northbound_flow.get('data_source', '未知')}")
            nb_data = northbound_flow.get('northbound', {})
            print(f"   沪股通净流入: {nb_data.get('sh_net_inflow', 0):.2f} 万元")
            print(f"   深股通净流入: {nb_data.get('sz_net_inflow', 0):.2f} 万元")
            print(f"   北向总净流入: {nb_data.get('total_net_inflow', 0):.2f} 万元")
            print(f"   日期: {northbound_flow.get('date', '未知')}")
        else:
            print("   获取北向资金流向数据失败")
    except Exception as e:
        print(f"   测试失败: {e}")
    print()
    
    # 测试主力资金流向
    print("5. 测试主力资金流向:")
    try:
        main_force = await provider.get_main_force_flow('600036', 'SH')
        if main_force:
            print(f"   数据源: {main_force.get('data_source', '未知')}")
            print(f"   股票: {main_force.get('stock_code', '未知')}")
            print(f"   主力净流入: {main_force.get('main_net_inflow', 0):.2f} 万元")
            print(f"   日期: {main_force.get('date', '未知')}")
        else:
            print("   获取主力资金流向数据失败")
    except Exception as e:
        print(f"   测试失败: {e}")
    print()
    
    # 测试综合股票数据
    print("6. 测试综合股票数据 (600036.SH):")
    try:
        comprehensive = await provider.get_comprehensive_stock_data('600036', 'SH')
        if comprehensive:
            print(f"   基本信息: {'成功获取' if comprehensive.get('basic_info') else '获取失败'}")
            print(f"   K线数据: {len(comprehensive.get('kline_data', []))} 条")
            print(f"   资金流向: {'成功获取' if comprehensive.get('capital_flow') else '获取失败'}")
            print(f"   技术指标: {'成功获取' if comprehensive.get('technical_indicators') else '获取失败'}")
            print(f"   时间戳: {comprehensive.get('timestamp', '未知')}")
        else:
            print("   获取综合数据失败")
    except Exception as e:
        print(f"   测试失败: {e}")
    print()
    
    # 测试板块资金流向（如果配置了关注板块）
    print("7. 测试板块资金流向:")
    watched_sectors = config.get('watched_sectors', [])
    if watched_sectors:
        test_sector = watched_sectors[0]
        print(f"   测试板块: {test_sector}")
        try:
            sector_flow = await provider.get_sector_capital_flow(test_sector)
            if sector_flow:
                print(f"   数据源: {sector_flow.get('data_source', '未知')}")
                print(f"   板块: {sector_flow.get('sector_name', test_sector)}")
                print(f"   主力净流入: {sector_flow.get('main_net_inflow', 0):.2f} 万元")
                print(f"   日期: {sector_flow.get('date', '未知')}")
            else:
                print("   获取板块资金流向数据失败（可能该数据源不支持）")
        except Exception as e:
            print(f"   测试失败: {e}")
    else:
        print("   未配置关注板块，跳过测试")
    print()
    
    print("=== 测试完成 ===")


async def test_tdx_capital_flow():
    """测试通达信资金流向数据（如果启用）"""
    print("=== 通达信资金流向数据测试 ===\n")
    
    config = load_config()
    use_tdx = config.get('tdx', {}).get('enabled', False)
    
    if not use_tdx:
        print("通达信数据源未启用，跳过测试")
        return
    
    provider = IntegratedDataProvider(config)
    
    # 测试通达信资金流向文件读取
    print("1. 测试通达信资金流向文件:")
    try:
        # 这里可以添加通达信特定测试
        print("   通达信资金流向数据提供器已初始化")
        print("   注意：需要通达信数据目录包含资金流向文件（shase.dat/sznse.dat等）")
    except Exception as e:
        print(f"   测试失败: {e}")
    print()


async def main():
    """主函数"""
    print("股票舆情分析系统 - 资金流向数据测试\n")
    
    # 测试API资金流向
    await test_capital_flow()
    
    # 测试通达信资金流向
    await test_tdx_capital_flow()
    
    print("\n测试完成！")


if __name__ == "__main__":
    asyncio.run(main())