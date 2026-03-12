---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: e8099c16fbeffeeb
signature: 4e40ff7f492ed8fea2d880c9793a8518
created: '2026-03-11T10:11:01.265707'
file_path: src\main.py
start_line: 146
end_line: 154
lines_of_code: 9
tags:
- entity
- method
- run_scheduled
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# run_scheduled

> **类型**: `EntityType.METHOD` | **文件**: `src\main.py` | **行数**: 146-154 (9行)

## 📋 概述

**说明**:

```
启动定时任务
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |

## 💻 代码实现

```python
    async def run_scheduled(self):
        """启动定时任务"""
        self.logger.info("启动定时任务调度器")

        if not self.config.get('scheduler', {}).get('enabled', False):
            self.logger.warning("定时任务未启用，请检查配置")
            return

        await self.scheduler.start(self.run_once)
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\main.py`
- **起始行号**: 146
- **搜索关键词**: `run_scheduled`

### 签名追踪
- **签名**: `4e40ff7f492ed8fe...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
