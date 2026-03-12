---
type: skill
version: '1.0'
category: entities
entity_type: code
entity_id: ff6db60af9fb2588
signature: 7e1a4d1e98dbf8ed5c4375af49d87f39
created: '2026-03-10T20:58:25.731616'
file_path: D:\mywork\workspace\stock-sentiment-analyzer\docs\TDX_DATA_INTEGRATION.md
start_line: 102
end_line: 112
lines_of_code: 11
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

> **类型**: `EntityType.CODE` | **文件**: `D:\mywork\workspace\stock-sentiment-analyzer\docs\TDX_DATA_INTEGRATION.md` | **行数**: 102-112 (11行)

## 📋 概述

*该实体缺少文档说明*

## 💻 代码实现

```text
from examples.tdx_integration import IntegratedDataProvider

# 初始化集成数据提供器
integrated_provider = IntegratedDataProvider(config)

# 获取股票信息（优先使用网络API，失败时使用通达信）
stock_info = await integrated_provider.get_stock_info_with_fallback('600036', 'SH')
print(f"数据源: {stock_info.get('data_source')}")
print(f"股票信息: {stock_info}")
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `D:\mywork\workspace\stock-sentiment-analyzer\docs\TDX_DATA_INTEGRATION.md`
- **起始行号**: 102
- **搜索关键词**: `代码块 (python)`

### 签名追踪
- **签名**: `7e1a4d1e98dbf8ed...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
