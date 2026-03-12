---
type: skill
version: '1.0'
category: entities
entity_type: code
entity_id: 6c293dc50e8dd695
signature: 22f90af34de001fedbb9b015526fd7ed
created: '2026-03-10T20:58:25.499880'
file_path: D:\mywork\workspace\stock-sentiment-analyzer\DEPLOYMENT.md
start_line: 63
end_line: 80
lines_of_code: 18
tags:
- entity
- code
- 代码块 (dockerfile)
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# 代码块 (dockerfile)

> **类型**: `EntityType.CODE` | **文件**: `D:\mywork\workspace\stock-sentiment-analyzer\DEPLOYMENT.md` | **行数**: 63-80 (18行)

## 📋 概述

*该实体缺少文档说明*

## 💻 代码实现

```text
FROM python:3.9-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制代码
COPY . .

# 创建日志目录
RUN mkdir -p logs data/reports

# 运行系统
CMD ["python3", "src/main.py", "--mode", "scheduled"]
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `D:\mywork\workspace\stock-sentiment-analyzer\DEPLOYMENT.md`
- **起始行号**: 63
- **搜索关键词**: `代码块 (dockerfile)`

### 签名追踪
- **签名**: `22f90af34de001fe...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
