---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: f5ab8f9eb1b65608
signature: 3bdd2165bbc56f684945a9a6c36f52fa
created: '2026-03-11T10:11:01.503020'
file_path: src\gitnexus\incremental.py
start_line: 95
end_line: 100
lines_of_code: 6
tags:
- entity
- method
- get_file_content_at_commit
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 3
---

# get_file_content_at_commit

> **类型**: `EntityType.METHOD` | **文件**: `src\gitnexus\incremental.py` | **行数**: 95-100 (6行)

## 📋 概述

**说明**:

```
获取指定提交版本的文件内容
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `file_path` | `any` | - |
| `commit_hexsha` | `any` | - |

## 📤 返回值

**类型**: `str`

## 💻 代码实现

```python
    def get_file_content_at_commit(self, file_path: str, commit_hexsha: str) -> str:
        """获取指定提交版本的文件内容"""
        try:
            return self.repo.git.show(f"{commit_hexsha}:{file_path}")
        except Exception as e:
            raise GitOperationError(f"获取文件历史内容失败: {str(e)}")
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\incremental.py`
- **起始行号**: 95
- **搜索关键词**: `get_file_content_at_commit`

### 签名追踪
- **签名**: `3bdd2165bbc56f68...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
