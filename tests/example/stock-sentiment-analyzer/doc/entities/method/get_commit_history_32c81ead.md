---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 32c81ead00d4727e
signature: 356866b64e69c1c3e85b1de69df09fb2
created: '2026-03-11T10:11:01.495052'
file_path: src\gitnexus\incremental.py
start_line: 60
end_line: 76
lines_of_code: 17
tags:
- entity
- method
- get_commit_history
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 2
---

# get_commit_history

> **类型**: `EntityType.METHOD` | **文件**: `src\gitnexus\incremental.py` | **行数**: 60-76 (17行)

## 📋 概述

**说明**:

```
获取提交历史记录
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `limit` | `any` | - |

## 📤 返回值

**类型**: `List[dict]`

## 💻 代码实现

```python
    def get_commit_history(self, limit: int = 100) -> List[dict]:
        """获取提交历史记录"""
        try:
            commits = []
            for commit in self.repo.iter_commits(max_count=limit):
                commits.append({
                    "hexsha": commit.hexsha,
                    "author": commit.author.name,
                    "email": commit.author.email,
                    "date": commit.authored_datetime.isoformat(),
                    "message": commit.message.strip(),
                    "changed_files": list(commit.stats.files.keys())
                })
            return commits
            
        except Exception as e:
            raise GitOperationError(f"获取提交历史失败: {str(e)}")
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\incremental.py`
- **起始行号**: 60
- **搜索关键词**: `get_commit_history`

### 签名追踪
- **签名**: `356866b64e69c1c3...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
