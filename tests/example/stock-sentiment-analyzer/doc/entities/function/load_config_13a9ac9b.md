---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: 13a9ac9b2b15c05e
signature: eeb01f913f89360a5d6bdbe9161f96c5
created: '2026-03-11T10:11:01.175655'
file_path: src\utils\config_loader.py
start_line: 107
end_line: 116
lines_of_code: 10
tags:
- entity
- function
- load_config
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# load_config

> **类型**: `EntityType.FUNCTION` | **文件**: `src\utils\config_loader.py` | **行数**: 107-116 (10行)

## 📋 概述

**说明**:

```
加载配置文件（兼容性函数）

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
def load_config(config_path: str = "config/config.yaml") -> Dict[str, Any]:
    """加载配置文件（兼容性函数）
    
    Args:
        config_path: 配置文件路径
        
    Returns:
        配置字典
    """
    return ConfigLoader.load(config_path)
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\utils\config_loader.py`
- **起始行号**: 107
- **搜索关键词**: `load_config`

### 签名追踪
- **签名**: `eeb01f913f89360a...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
