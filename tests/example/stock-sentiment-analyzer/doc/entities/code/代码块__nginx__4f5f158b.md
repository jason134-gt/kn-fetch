---
type: skill
version: '1.0'
category: entities
entity_type: code
entity_id: 4f5f158b96b5c2fb
signature: 6da7851f75459fcf5241b94dbfff1a3c
created: '2026-03-10T20:58:25.507297'
file_path: D:\mywork\workspace\stock-sentiment-analyzer\DEPLOYMENT.md
start_line: 147
end_line: 163
lines_of_code: 17
tags:
- entity
- code
- 代码块 (nginx)
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# 代码块 (nginx)

> **类型**: `EntityType.CODE` | **文件**: `D:\mywork\workspace\stock-sentiment-analyzer\DEPLOYMENT.md` | **行数**: 147-163 (17行)

## 📋 概述

*该实体缺少文档说明*

## 💻 代码实现

```text
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /reports {
        alias /root/.openclaw/workspace/stock-sentiment-analyzer/data/reports;
        autoindex on;
    }
}
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `D:\mywork\workspace\stock-sentiment-analyzer\DEPLOYMENT.md`
- **起始行号**: 147
- **搜索关键词**: `代码块 (nginx)`

### 签名追踪
- **签名**: `6da7851f75459fcf...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
