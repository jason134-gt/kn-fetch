---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: dd92737db675be97
signature: 59bf9a2d6c518600a8f67cd6d5046d27
created: '2026-03-11T10:11:01.462796'
file_path: src\gitnexus\gitnexus_client.py
start_line: 222
end_line: 230
lines_of_code: 9
tags:
- entity
- method
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

> **类型**: `EntityType.METHOD` | **文件**: `src\gitnexus\gitnexus_client.py` | **行数**: 222-230 (9行)

## 📋 概述

**说明**:

```
导出分析结果
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `output_path` | `any` | - |
| `format` | `any` | - |

## 📤 返回值

**类型**: `str`

## 💻 代码实现

```python
    def export(self, output_path: Optional[str] = None, format: ExportFormat = ExportFormat.MARKDOWN) -> str:
        """导出分析结果"""
        graph = self._load_knowledge_graph()
        exporter = KnowledgeExporter(graph, self.config)
        
        if output_path is None:
            output_path = self.config["output"]["path"]
        
        return exporter.export(output_path, format)
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\gitnexus_client.py`
- **起始行号**: 222
- **搜索关键词**: `export`

### 签名追踪
- **签名**: `59bf9a2d6c518600...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
