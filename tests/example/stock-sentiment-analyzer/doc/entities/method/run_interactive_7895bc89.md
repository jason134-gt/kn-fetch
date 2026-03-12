---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 7895bc89c0ec4081
signature: 69a2c48cdd2da7d0f362f744316172ac
created: '2026-03-11T10:11:01.273944'
file_path: src\main.py
start_line: 156
end_line: 177
lines_of_code: 22
tags:
- entity
- method
- run_interactive
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# run_interactive

> **类型**: `EntityType.METHOD` | **文件**: `src\main.py` | **行数**: 156-177 (22行)

## 📋 概述

**说明**:

```
交互式运行
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |

## 💻 代码实现

```python
    async def run_interactive(self):
        """交互式运行"""
        self.logger.info("进入交互模式")
        self.logger.info("输入 'run' 执行分析，输入 'exit' 退出")

        while True:
            try:
                cmd = input("> ").strip().lower()

                if cmd == 'exit':
                    self.logger.info("退出交互模式")
                    break
                elif cmd == 'run':
                    await self.run_once()
                else:
                    self.logger.info("未知命令，请输入 'run' 或 'exit'")

            except KeyboardInterrupt:
                self.logger.info("\n收到中断信号，退出")
                break
            except Exception as e:
                self.logger.error(f"命令执行失败: {e}")
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\main.py`
- **起始行号**: 156
- **搜索关键词**: `run_interactive`

### 签名追踪
- **签名**: `69a2c48cdd2da7d0...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
