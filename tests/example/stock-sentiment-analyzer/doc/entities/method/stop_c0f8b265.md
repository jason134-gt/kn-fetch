---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: c0f8b265e607c13b
signature: 580fecc2fad9bf3db9ed800b77a29c34
created: '2026-03-11T10:11:01.582340'
file_path: src\scheduler\task_scheduler.py
start_line: 79
end_line: 82
lines_of_code: 4
tags:
- entity
- method
- stop
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# stop

> **类型**: `EntityType.METHOD` | **文件**: `src\scheduler\task_scheduler.py` | **行数**: 79-82 (4行)

## 📋 概述

**说明**:

```
停止定时任务
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |

## 💻 代码实现

```python
    def stop(self):
        """停止定时任务"""
        self.running = False
        self.logger.info("定时任务已停止")
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\scheduler\task_scheduler.py`
- **起始行号**: 79
- **搜索关键词**: `stop`

### 签名追踪
- **签名**: `580fecc2fad9bf3d...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
