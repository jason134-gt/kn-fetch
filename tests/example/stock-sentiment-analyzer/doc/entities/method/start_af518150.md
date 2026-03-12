---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: af5181502122be86
signature: 0eacf4ce2d9d34d45c9ed6eaabc78677
created: '2026-03-11T10:11:01.554765'
file_path: src\scheduler\task_scheduler.py
start_line: 55
end_line: 77
lines_of_code: 23
tags:
- entity
- method
- start
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 2
---

# start

> **类型**: `EntityType.METHOD` | **文件**: `src\scheduler\task_scheduler.py` | **行数**: 55-77 (23行)

## 📋 概述

**说明**:

```
启动定时任务

Args:
    task_func: 任务函数
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `task_func` | `any` | - |

## 💻 代码实现

```python
    async def start(self, task_func: Callable):
        """启动定时任务

        Args:
            task_func: 任务函数
        """
        if not self.enabled:
            self.logger.warning("定时任务未启用")
            return

        self.running = True
        self.logger.info(f"定时任务已启动，间隔: {self.interval_minutes}分钟")

        while self.running:
            try:
                self.logger.info(f"执行定时任务: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
                await task_func()
                self.logger.info(f"定时任务完成: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
            except Exception as e:
                self.logger.error(f"定时任务执行失败: {e}", exc_info=True)

            # 等待下一次执行
            await asyncio.sleep(self.interval_minutes * 60)
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\scheduler\task_scheduler.py`
- **起始行号**: 55
- **搜索关键词**: `start`

### 签名追踪
- **签名**: `0eacf4ce2d9d34d4...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
