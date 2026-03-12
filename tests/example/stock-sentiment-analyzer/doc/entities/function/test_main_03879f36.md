---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: 03879f36a0067583
signature: 7e6c29a8f1f5701cf8df1769ff0bab8d
created: '2026-03-11T10:11:01.146741'
file_path: test_main.py
start_line: 13
end_line: 60
lines_of_code: 48
tags:
- entity
- function
- test_main
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# test_main

> **类型**: `EntityType.FUNCTION` | **文件**: `test_main.py` | **行数**: 13-60 (48行)

## 📋 概述

**说明**:

```
测试主程序
```

## 💻 代码实现

```python
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
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `test_main.py`
- **起始行号**: 13
- **搜索关键词**: `test_main`

### 签名追踪
- **签名**: `7e6c29a8f1f5701c...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
