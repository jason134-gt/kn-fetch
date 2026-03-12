---
type: skill
version: '1.0'
category: entities
entity_type: code
entity_id: ae73c5a43be4af3b
signature: 77be3dc08c8a52c19bf7336009a2ad27
created: '2026-03-10T20:58:25.516337'
file_path: D:\mywork\workspace\stock-sentiment-analyzer\DEPLOYMENT.md
start_line: 182
end_line: 191
lines_of_code: 10
tags:
- entity
- code
- 代码块 (bash)
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# 代码块 (bash)

> **类型**: `EntityType.CODE` | **文件**: `D:\mywork\workspace\stock-sentiment-analyzer\DEPLOYMENT.md` | **行数**: 182-191 (10行)

## 📋 概述

*该实体缺少文档说明*

## 💻 代码实现

```text
# CPU和内存使用
top -p $(pgrep -f "src/main.py")

# 磁盘使用
df -h

# 网络连接
netstat -an | grep ESTABLISHED
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `D:\mywork\workspace\stock-sentiment-analyzer\DEPLOYMENT.md`
- **起始行号**: 182
- **搜索关键词**: `代码块 (bash)`

### 签名追踪
- **签名**: `77be3dc08c8a52c1...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
