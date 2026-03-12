---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: 6a01ec4aabd59559
signature: 3e771549f40abc1bc421c66b636f0480
created: '2026-03-11T10:11:01.169260'
file_path: src\main.py
start_line: 180
end_line: 200
lines_of_code: 21
tags:
- entity
- function
- main
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# main

> **类型**: `EntityType.FUNCTION` | **文件**: `src\main.py` | **行数**: 180-200 (21行)

## 📋 概述

**说明**:

```
主函数
```

## 💻 代码实现

```python
async def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(description="股票舆情分析系统")
    parser.add_argument('--config', default='config/config.yaml', help='配置文件路径')
    parser.add_argument('--mode', choices=['once', 'scheduled', 'interactive'], default='once',
                        help='运行模式: once(单次), scheduled(定时), interactive(交互)')

    args = parser.parse_args()

    # 创建分析器实例
    analyzer = StockSentimentAnalyzer(args.config)

    # 根据模式运行
    if args.mode == 'once':
        await analyzer.run_once()
    elif args.mode == 'scheduled':
        await analyzer.run_scheduled()
    elif args.mode == 'interactive':
        await analyzer.run_interactive()
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\main.py`
- **起始行号**: 180
- **搜索关键词**: `main`

### 签名追踪
- **签名**: `3e771549f40abc1b...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
