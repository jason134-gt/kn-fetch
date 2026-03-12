---
type: skill
version: '1.0'
category: entities
entity_type: code
entity_id: 70f3fb52414ab642
signature: 6f924f5c2271d6ba1a0310f2b4a2b59e
created: '2026-03-11T10:11:02.378872'
file_path: D:\mywork\workspace\kn-fetch\output\example\stock-sentiment-analyzer\EXAMPLES.md
start_line: 262
end_line: 279
lines_of_code: 18
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

> **类型**: `EntityType.CODE` | **文件**: `D:\mywork\workspace\kn-fetch\output\example\stock-sentiment-analyzer\EXAMPLES.md` | **行数**: 262-279 (18行)

## 📋 概述

*该实体缺少文档说明*

## 💻 代码实现

```text
import schedule
import time
from src.main import StockSentimentAnalyzer

analyzer = StockSentimentAnalyzer('config/config.yaml')

def job():
    print("执行定时分析...")
    asyncio.run(analyzer.run_once())

# 每30分钟执行一次
schedule.every(30).minutes.do(job)

while True:
    schedule.run_pending()
    time.sleep(1)
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `D:\mywork\workspace\kn-fetch\output\example\stock-sentiment-analyzer\EXAMPLES.md`
- **起始行号**: 262
- **搜索关键词**: `代码块 (python)`

### 签名追踪
- **签名**: `6f924f5c2271d6ba...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
