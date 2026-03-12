---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 462ba38dbce32ecc
signature: b9d5584911a487d72af66a4a7d894050
created: '2026-03-11T10:11:01.495052'
file_path: src\gitnexus\incremental.py
start_line: 78
end_line: 93
lines_of_code: 16
tags:
- entity
- method
- get_file_history
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 3
---

# get_file_history

> **类型**: `EntityType.METHOD` | **文件**: `src\gitnexus\incremental.py` | **行数**: 78-93 (16行)

## 📋 概述

**说明**:

```
获取指定文件的修改历史
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `file_path` | `any` | - |
| `limit` | `any` | - |

## 📤 返回值

**类型**: `List[dict]`

## 💻 代码实现

```python
    def get_file_history(self, file_path: str, limit: int = 100) -> List[dict]:
        """获取指定文件的修改历史"""
        try:
            commits = []
            for commit in self.repo.iter_commits(paths=file_path, max_count=limit):
                commits.append({
                    "hexsha": commit.hexsha,
                    "author": commit.author.name,
                    "email": commit.author.email,
                    "date": commit.authored_datetime.isoformat(),
                    "message": commit.message.strip()
                })
            return commits
            
        except Exception as e:
            raise GitOperationError(f"获取文件历史失败: {str(e)}")
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\incremental.py`
- **起始行号**: 78
- **搜索关键词**: `get_file_history`

### 签名追踪
- **签名**: `b9d5584911a487d7...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
