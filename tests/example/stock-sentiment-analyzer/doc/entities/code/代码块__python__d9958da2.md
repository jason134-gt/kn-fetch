---
type: skill
version: '1.0'
category: entities
entity_type: code
entity_id: d9958da23210c27d
signature: 7738df04cd14bc607f303b6690d2d3fa
created: '2026-03-11T10:11:02.978394'
file_path: D:\mywork\workspace\kn-fetch\output\example\stock-sentiment-analyzer\docs\TDX_DATA_INTEGRATION.md
start_line: 69
end_line: 96
lines_of_code: 28
tags:
- entity
- code
- 代码块 (python)
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# 代码块 (python)

> **类型**: `EntityType.CODE` | **文件**: `D:\mywork\workspace\kn-fetch\output\example\stock-sentiment-analyzer\docs\TDX_DATA_INTEGRATION.md` | **行数**: 69-96 (28行)

## 📋 概述

*该实体缺少文档说明*

## 💻 代码实现

```text
import asyncio
from src.utils.config_loader import ConfigLoader
from src.data.tdx_data_provider import TdxDataProvider

# 加载配置
config_loader = ConfigLoader()
config = config_loader.load()

# 初始化通达信数据提供器
tdx_provider = TdxDataProvider(config)

# 获取股票日线数据
day_data = tdx_provider.get_stock_day_data('600036', 'SH')
print(f"日线数据: {len(day_data)} 条")

# 获取分钟线数据
minute_data = tdx_provider.get_stock_minute_data('600036', 'SH', period=5)
print(f"5分钟线数据: {len(minute_data)} 条")

# 获取概念板块列表
sectors = tdx_provider.get_sector_list()
print(f"概念板块: {len(sectors)} 个")

# 计算技术指标
indicators = tdx_provider.calculate_technical_indicators('600036', 'SH', 'day')
print(f"技术指标: {indicators}")
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `D:\mywork\workspace\kn-fetch\output\example\stock-sentiment-analyzer\docs\TDX_DATA_INTEGRATION.md`
- **起始行号**: 69
- **搜索关键词**: `代码块 (python)`

### 签名追踪
- **签名**: `7738df04cd14bc60...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
