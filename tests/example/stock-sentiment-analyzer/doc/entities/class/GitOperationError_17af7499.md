---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: 17af7499052ac131
signature: c25f591d4bd0b002fb89a674725fa59e
created: '2026-03-11T10:11:01.221754'
file_path: src\gitnexus\exceptions.py
start_line: 21
end_line: 23
lines_of_code: 3
tags:
- entity
- class
- GitOperationError
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# GitOperationError

> **类型**: `EntityType.CLASS` | **文件**: `src\gitnexus\exceptions.py` | **行数**: 21-23 (3行)

## 📋 概述

**说明**:

```
Git操作错误
```

## 💻 代码实现

```python
class GitOperationError(GitNexusError):
    """Git操作错误"""
    pass
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\exceptions.py`
- **起始行号**: 21
- **搜索关键词**: `GitOperationError`

### 签名追踪
- **签名**: `c25f591d4bd0b002...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
