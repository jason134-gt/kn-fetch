---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: 8bd3d7f8176c7ded
signature: 6740ba702fc6f1e8fab9144bdf04afd1
created: '2026-03-11T10:11:01.138656'
file_path: gitnexus.py
start_line: 99
end_line: 107
lines_of_code: 9
tags:
- entity
- function
- clear
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# clear

> **类型**: `EntityType.FUNCTION` | **文件**: `gitnexus.py` | **行数**: 99-107 (9行)

## 📋 概述

**说明**:

```
清除分析缓存
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `config` | `any` | - |

## 💻 代码实现

```python
def clear(config):
    """清除分析缓存"""
    try:
        client = GitNexusClient(config)
        if click.confirm("确定要清除所有分析缓存吗?"):
            client.clear_cache()
            click.echo("缓存已清除")
    except GitNexusError as e:
        click.echo(f"清除缓存失败: {str(e)}", err=True)
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `gitnexus.py`
- **起始行号**: 99
- **搜索关键词**: `clear`

### 签名追踪
- **签名**: `6740ba702fc6f1e8...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
