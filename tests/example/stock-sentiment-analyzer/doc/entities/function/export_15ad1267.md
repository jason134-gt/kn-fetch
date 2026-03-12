---
type: skill
version: '1.0'
category: entities
entity_type: function
entity_id: 15ad1267d9753b09
signature: aee86b398f248fbb4587c9670aeaeac7
created: '2026-03-11T10:11:01.129074'
file_path: gitnexus.py
start_line: 65
end_line: 75
lines_of_code: 11
tags:
- entity
- function
- export
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 3
---

# export

> **类型**: `EntityType.FUNCTION` | **文件**: `gitnexus.py` | **行数**: 65-75 (11行)

## 📋 概述

**说明**:

```
导出分析结果
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `config` | `any` | - |
| `output` | `any` | - |
| `format` | `any` | - |

## 💻 代码实现

```python
def export(config, output, format):
    """导出分析结果"""
    try:
        client = GitNexusClient(config)
        export_format = ExportFormat(format)
        
        output_path = client.export(output, export_format)
        click.echo(f"导出成功: {output_path}")
        
    except GitNexusError as e:
        click.echo(f"导出失败: {str(e)}", err=True)
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `gitnexus.py`
- **起始行号**: 65
- **搜索关键词**: `export`

### 签名追踪
- **签名**: `aee86b398f248fbb...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
