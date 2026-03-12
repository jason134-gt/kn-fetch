---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: a21ce343cfd46c48
signature: ece1cc8bab366ae3ea7704bdd6617a69
created: '2026-03-11T10:11:01.688166'
file_path: src\utils\config_loader.py
start_line: 15
end_line: 41
lines_of_code: 27
tags:
- entity
- method
- load
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# load

> **类型**: `EntityType.METHOD` | **文件**: `src\utils\config_loader.py` | **行数**: 15-41 (27行)

## 📋 概述

**说明**:

```
加载配置文件

Args:
    config_path: 配置文件路径

Returns:
    配置字典
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `config_path` | `any` | - |

## 📤 返回值

**类型**: `Dict[str, Any]`

## 💻 代码实现

```python
    def load(config_path: str) -> Dict[str, Any]:
        """加载配置文件

        Args:
            config_path: 配置文件路径

        Returns:
            配置字典
        """
        logger = logging.getLogger(__name__)

        config_file = Path(config_path)

        if not config_file.exists():
            logger.warning(f"配置文件不存在: {config_path}，使用默认配置")
            return ConfigLoader._get_default_config()

        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)

            logger.info(f"配置文件加载成功: {config_path}")
            return config

        except Exception as e:
            logger.error(f"配置文件加载失败: {e}，使用默认配置")
            return ConfigLoader._get_default_config()
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\utils\config_loader.py`
- **起始行号**: 15
- **搜索关键词**: `load`

### 签名追踪
- **签名**: `ece1cc8bab366ae3...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
