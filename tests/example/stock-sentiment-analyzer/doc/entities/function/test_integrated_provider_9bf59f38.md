---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: 9bf59f3845c0f2f5
signature: 9c0e57039889af929bcbd0f0401b0070
created: '2026-03-11T10:11:01.154970'
file_path: test_tdx.py
start_line: 89
end_line: 139
lines_of_code: 51
tags:
- entity
- function
- test_integrated_provider
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# test_integrated_provider

> **类型**: `EntityType.FUNCTION` | **文件**: `test_tdx.py` | **行数**: 89-139 (51行)

## 📋 概述

**说明**:

```
测试集成数据提供器
```

## 💻 代码实现

```python
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
```

## 🔧 重构建议

- 方法过长 (51 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `test_tdx.py`
- **起始行号**: 89
- **搜索关键词**: `test_integrated_provider`

### 签名追踪
- **签名**: `9c0e57039889af92...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
