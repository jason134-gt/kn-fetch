---
type: skill
version: '1.0'
category: entities
entity_type: code
entity_id: e0c83b57779220ec
signature: ed2bf6a3e950c76a71deb34e5234b696
created: '2026-03-10T20:58:25.539880'
file_path: D:\mywork\workspace\stock-sentiment-analyzer\EXAMPLES.md
start_line: 150
end_line: 177
lines_of_code: 28
tags:
- entity
- code
- 代码块 (yaml)
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# 代码块 (yaml)

> **类型**: `EntityType.CODE` | **文件**: `D:\mywork\workspace\stock-sentiment-analyzer\EXAMPLES.md` | **行数**: 150-177 (28行)

## 📋 概述

*该实体缺少文档说明*

## 💻 代码实现

```text
# 添加更多股票
stock_pool:
  - code: "600036"
    name: "招商银行"
    sector: "银行"
  - code: "000858"
    name: "五粮液"
    sector: "白酒"
  - code: "300750"
    name: "宁德时代"
    sector: "新能源"

# 调整专家权重
expert_weights:
  macro_economy: 0.30      # 增加宏观权重
  industry_research: 0.20
  technical_analysis: 0.20
  capital_flow: 0.20
  risk_control: 0.10

# 启用AI模型
ai:
  provider: "openai"
  openai:
    api_key: "sk-xxx"
    model: "gpt-4"
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `D:\mywork\workspace\stock-sentiment-analyzer\EXAMPLES.md`
- **起始行号**: 150
- **搜索关键词**: `代码块 (yaml)`

### 签名追踪
- **签名**: `ed2bf6a3e950c76a...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
