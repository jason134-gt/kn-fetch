---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: e6b6cebaadcd34a5
signature: 711da625ce67e6a4742c9739486a4fd2
created: '2026-03-11T10:11:01.121089'
file_path: gitnexus.py
start_line: 23
end_line: 39
lines_of_code: 17
tags:
- entity
- function
- analyze
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 2
---

# analyze

> **类型**: `EntityType.FUNCTION` | **文件**: `gitnexus.py` | **行数**: 23-39 (17行)

## 📋 概述

**说明**:

```
分析项目代码
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `config` | `any` | - |
| `force` | `any` | - |

## 💻 代码实现

```python
def analyze(config, force):
    """分析项目代码"""
    try:
        client = GitNexusClient(config)
        click.echo(f"开始分析项目: {client.config['project']['name']}")
        
        graph = client.analyze_full(force=force)
        
        stats = client.get_statistics()
        click.echo("\n分析完成!")
        click.echo(f"总文件数: {stats['total_files']}")
        click.echo(f"实体数: {stats['total_entities']}")
        click.echo(f"关系数: {stats['total_relationships']}")
        click.echo(f"总代码行数: {stats['lines_of_code']}")
        
    except GitNexusError as e:
        click.echo(f"分析失败: {str(e)}", err=True)
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `gitnexus.py`
- **起始行号**: 23
- **搜索关键词**: `analyze`

### 签名追踪
- **签名**: `711da625ce67e6a4...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
