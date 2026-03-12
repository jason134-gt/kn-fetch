---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: aaecb2633d0a32f8
signature: 840d4ced273315955e0ebac7be2607ee
created: '2026-03-11T10:11:01.136134'
file_path: gitnexus.py
start_line: 79
end_line: 95
lines_of_code: 17
tags:
- entity
- function
- stats
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# stats

> **类型**: `EntityType.FUNCTION` | **文件**: `gitnexus.py` | **行数**: 79-95 (17行)

## 📋 概述

**说明**:

```
显示分析统计信息
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `config` | `any` | - |

## 💻 代码实现

```python
def stats(config):
    """显示分析统计信息"""
    try:
        client = GitNexusClient(config)
        stats = client.get_statistics()
        
        click.echo("=== 项目分析统计 ===")
        click.echo(f"总文件数: {stats['total_files']}")
        click.echo(f"总代码实体数: {stats['total_entities']}")
        click.echo(f"总关系数: {stats['total_relationships']}")
        click.echo(f"总代码行数: {stats['lines_of_code']}")
        click.echo("\n实体类型分布:")
        for entity_type, count in stats['entity_types'].items():
            click.echo(f"  {entity_type}: {count}")
        
    except GitNexusError as e:
        click.echo(f"获取统计信息失败: {str(e)}", err=True)
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `gitnexus.py`
- **起始行号**: 79
- **搜索关键词**: `stats`

### 签名追踪
- **签名**: `840d4ced27331595...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
