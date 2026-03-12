---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: 2d300641081f2ac5
signature: 2397957b3479550a16995166af207a6a
created: '2026-03-11T10:11:01.175655'
file_path: src\ai\ai_client.py
start_line: 458
end_line: 476
lines_of_code: 19
tags:
- entity
- function
- create_ai_client
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# create_ai_client

> **类型**: `EntityType.FUNCTION` | **文件**: `src\ai\ai_client.py` | **行数**: 458-476 (19行)

## 📋 概述

**说明**:

```
创建AI客户端

Args:
    config: 配置字典

Returns:
    AI客户端实例
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `config` | `any` | - |

## 📤 返回值

**类型**: `Optional[AIClient]`

## 💻 代码实现

```python
def create_ai_client(config: Dict[str, Any]) -> Optional[AIClient]:
    """创建AI客户端

    Args:
        config: 配置字典

    Returns:
        AI客户端实例
    """
    ai_config = config.get('ai', {})
    provider = ai_config.get('provider', 'openai')

    if provider == 'openai':
        return OpenAIClient(config)
    elif provider == 'claude':
        return ClaudeClient(config)
    else:
        logging.getLogger(__name__).warning(f"不支持的AI提供商: {provider}")
        return None
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\ai\ai_client.py`
- **起始行号**: 458
- **搜索关键词**: `create_ai_client`

### 签名追踪
- **签名**: `2397957b3479550a...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
