---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: e6498997706bbe5e
signature: b8e6f11d21be83900071cf0de961cbbe
created: '2026-03-11T10:11:01.243081'
file_path: src\gitnexus\incremental.py
start_line: 6
end_line: 100
lines_of_code: 95
tags:
- entity
- class
- IncrementalAnalyzer
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# IncrementalAnalyzer

> **类型**: `EntityType.CLASS` | **文件**: `src\gitnexus\incremental.py` | **行数**: 6-100 (95行)

## 📋 概述

**说明**:

```
增量分析器，基于Git diff检测变更文件
```

## 💻 代码片段（节选）

```python
class IncrementalAnalyzer:
    """增量分析器，基于Git diff检测变更文件"""
    
    def __init__(self, repo: git.Repo):
        self.repo = repo
        
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
            raise GitOperationErro
// ... 省略 1724 字符 ...
```

## 🔧 重构建议

- 方法过长 (95 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\incremental.py`
- **起始行号**: 6
- **搜索关键词**: `IncrementalAnalyzer`

### 签名追踪
- **签名**: `b8e6f11d21be8390...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
