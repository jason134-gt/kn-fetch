---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: 6e7cf3a8917b36ee
signature: 82e7736ed888a13659d926e1f1297f1e
created: '2026-03-11T10:11:01.151146'
file_path: test_tdx.py
start_line: 19
end_line: 86
lines_of_code: 68
tags:
- entity
- function
- test_tdx_provider
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# test_tdx_provider

> **类型**: `EntityType.FUNCTION` | **文件**: `test_tdx.py` | **行数**: 19-86 (68行)

## 📋 概述

**说明**:

```
测试通达信数据提供器
```

## 💻 代码实现

```python
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
```

## 🔧 重构建议

- 方法过长 (68 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `test_tdx.py`
- **起始行号**: 19
- **搜索关键词**: `test_tdx_provider`

### 签名追踪
- **签名**: `82e7736ed888a136...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
