---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 65163cc162a9a736
signature: d684ec50de4be38843bd202769e0cc79
created: '2026-03-11T10:11:01.486710'
file_path: src\gitnexus\incremental.py
start_line: 12
end_line: 38
lines_of_code: 27
tags:
- entity
- method
- get_changed_files
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 3
---

# get_changed_files

> **类型**: `EntityType.METHOD` | **文件**: `src\gitnexus\incremental.py` | **行数**: 12-38 (27行)

## 📋 概述

**说明**:

```
获取两个提交之间的变更文件
如果不指定base_commit，则与上次提交比较
如果不指定head_commit，则使用当前工作区
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `base_commit` | `any` | - |
| `head_commit` | `any` | - |

## 📤 返回值

**类型**: `List[str]`

## 💻 代码实现

```python
    def get_changed_files(
        self, 
        base_commit: Optional[str] = None, 
        head_commit: Optional[str] = None
    ) -> List[str]:
        """
        获取两个提交之间的变更文件
        如果不指定base_commit，则与上次提交比较
        如果不指定head_commit，则使用当前工作区
        """
        try:
            if base_commit is None:
                # 默认与上一次提交比较
                base_commit = self.repo.head.commit.hexsha
            
            if head_commit is None:
                # 比较工作区与指定提交的差异
                diff = self.repo.git.diff(base_commit, name_only=True)
            else:
                # 比较两个提交之间的差异
                diff = self.repo.git.diff(base_commit, head_commit, name_only=True)
            
            changed_files = [f.strip() for f in diff.splitlines() if f.strip()]
            return changed_files
            
        except Exception as e:
            raise GitOperationError(f"获取变更文件失败: {str(e)}")
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\incremental.py`
- **起始行号**: 12
- **搜索关键词**: `get_changed_files`

### 签名追踪
- **签名**: `d684ec50de4be388...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
