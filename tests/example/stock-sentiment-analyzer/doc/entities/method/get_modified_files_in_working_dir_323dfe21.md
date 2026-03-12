---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 323dfe216ff2d872
signature: 587e666bb82b60a660393fa8847ed910
created: '2026-03-11T10:11:01.489580'
file_path: src\gitnexus\incremental.py
start_line: 40
end_line: 58
lines_of_code: 19
tags:
- entity
- method
- get_modified_files_in_working_dir
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# get_modified_files_in_working_dir

> **类型**: `EntityType.METHOD` | **文件**: `src\gitnexus\incremental.py` | **行数**: 40-58 (19行)

## 📋 概述

**说明**:

```
获取工作区中已修改但未提交的文件
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |

## 📤 返回值

**类型**: `List[str]`

## 💻 代码实现

```python
    def get_modified_files_in_working_dir(self) -> List[str]:
        """获取工作区中已修改但未提交的文件"""
        try:
            # 获取已修改但未暂存的文件
            modified = self.repo.git.diff(name_only=True)
            # 获取已暂存的文件
            staged = self.repo.git.diff("--cached", name_only=True)
            # 获取未跟踪的文件
            untracked = self.repo.untracked_files
            
            all_changed = set()
            all_changed.update([f.strip() for f in modified.splitlines() if f.strip()])
            all_changed.update([f.strip() for f in staged.splitlines() if f.strip()])
            all_changed.update(untracked)
            
            return list(all_changed)
            
        except Exception as e:
            raise GitOperationError(f"获取工作区变更文件失败: {str(e)}")
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\incremental.py`
- **起始行号**: 40
- **搜索关键词**: `get_modified_files_in_working_dir`

### 签名追踪
- **签名**: `587e666bb82b60a6...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
