---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: 08dbed0c38656151
signature: 3e61790d7320f0ec099f7006a43a91ef
created: '2026-03-11T10:11:01.216587'
file_path: src\gitnexus\exceptions.py
start_line: 13
end_line: 15
lines_of_code: 3
tags:
- entity
- class
- ParseError
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# ParseError

> **类型**: `EntityType.CLASS` | **文件**: `src\gitnexus\exceptions.py` | **行数**: 13-15 (3行)

## 📋 概述

**说明**:

```
解析错误
```

## 💻 代码实现

```python
class ParseError(GitNexusError):
    """解析错误"""
    pass
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\exceptions.py`
- **起始行号**: 13
- **搜索关键词**: `ParseError`

### 签名追踪
- **签名**: `3e61790d7320f0ec...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
