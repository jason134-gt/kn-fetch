---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: b9edf1c19820337b
signature: 87c4af3000dce57b1fe5f8cef1f4e10c
created: '2026-03-11T10:11:01.210240'
file_path: src\gitnexus\exceptions.py
start_line: 5
end_line: 7
lines_of_code: 3
tags:
- entity
- class
- ConfigurationError
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# ConfigurationError

> **类型**: `EntityType.CLASS` | **文件**: `src\gitnexus\exceptions.py` | **行数**: 5-7 (3行)

## 📋 概述

**说明**:

```
配置错误
```

## 💻 代码实现

```python
class ConfigurationError(GitNexusError):
    """配置错误"""
    pass
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\exceptions.py`
- **起始行号**: 5
- **搜索关键词**: `ConfigurationError`

### 签名追踪
- **签名**: `87c4af3000dce57b...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
