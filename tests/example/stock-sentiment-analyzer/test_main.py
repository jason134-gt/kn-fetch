#!/usr/bin/env python3
"""
测试主程序运行
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

import asyncio


async def test_main():
    """测试主程序"""
    print("=== 测试主程序运行 ===\n")
    
    # 测试主程序导入
    try:
        from src.main import StockSentimentAnalyzer
        print("✅ 主程序导入成功")
    except Exception as e:
        print(f"❌ 主程序导入失败: {e}")
        import traceback
        traceback.print_exc()
        return
    
    # 测试配置加载
    try:
        from src.utils.config_loader import load_config
        config = load_config()
        print("✅ 配置加载成功")
        print(f"   专家权重: {config.get('expert_weights', {})}")
        print(f"   关注板块: {config.get('watched_sectors', [])}")
        print(f"   股票池: {len(config.get('stock_pool', []))} 只股票")
    except Exception as e:
        print(f"❌ 配置加载失败: {e}")
    
    # 测试数据提供器
    try:
        from src.data.integrated_data_provider import IntegratedDataProvider
        provider = IntegratedDataProvider(config)
        status = provider.get_data_source_status()
        print("✅ 数据提供器初始化成功")
        print(f"   数据源状态: {status}")
    except Exception as e:
        print(f"❌ 数据提供器初始化失败: {e}")
    
    # 测试资金流向数据
    try:
        flow_data = await provider.get_stock_capital_flow('600036', 'SH')
        if flow_data:
            print("✅ 资金流向数据获取成功")
            print(f"   数据源: {flow_data.get('data_source', '未知')}")
            print(f"   是否模拟数据: {flow_data.get('is_mock_data', False)}")
        else:
            print("❌ 资金流向数据获取失败")
    except Exception as e:
        print(f"❌ 资金流向数据测试失败: {e}")
    
    print("\n=== 主程序测试完成 ===")


if __name__ == "__main__":
    asyncio.run(test_main())