---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: 5605ea342e11b1d3
signature: eb7ed898de1888097c1e010338eda282
created: '2026-03-11T10:11:01.184366'
file_path: src\utils\logger.py
start_line: 11
end_line: 54
lines_of_code: 44
tags:
- entity
- function
- setup_logger
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 2
---

# setup_logger

> **类型**: `EntityType.FUNCTION` | **文件**: `src\utils\logger.py` | **行数**: 11-54 (44行)

## 📋 概述

**说明**:

```
设置日志

Args:
    level: 日志级别
    log_file: 日志文件路径

Returns:
    Logger实例
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `level` | `any` | - |
| `log_file` | `any` | - |

## 📤 返回值

**类型**: `logging.Logger`

## 💻 代码实现

```python
def setup_logger(level: str = 'INFO', log_file: str = './logs/analyzer.log') -> logging.Logger:
    """设置日志

    Args:
        level: 日志级别
        log_file: 日志文件路径

    Returns:
        Logger实例
    """
    # 创建logger
    logger = logging.getLogger()
    logger.setLevel(getattr(logging, level.upper(), logging.INFO))

    # 清除已有的handlers
    logger.handlers.clear()

    # 创建formatter
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )

    # 控制台handler
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

    # 文件handler
    log_path = Path(log_file)
    log_path.parent.mkdir(parents=True, exist_ok=True)

    file_handler = RotatingFileHandler(
        log_file,
        maxBytes=100 * 1024 * 1024,  # 100MB
        backupCount=10,
        encoding='utf-8'
    )
    file_handler.setLevel(getattr(logging, level.upper(), logging.INFO))
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)

    return logger
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\utils\logger.py`
- **起始行号**: 11
- **搜索关键词**: `setup_logger`

### 签名追踪
- **签名**: `eb7ed898de188809...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
