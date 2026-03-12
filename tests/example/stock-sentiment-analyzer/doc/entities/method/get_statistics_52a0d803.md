---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 52a0d80333699a7d
signature: 9f084be123058d7a63dc054b7bf5d97a
created: '2026-03-11T10:11:01.462796'
file_path: src\gitnexus\gitnexus_client.py
start_line: 232
end_line: 248
lines_of_code: 17
tags:
- entity
- method
- get_statistics
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# get_statistics

> **类型**: `EntityType.METHOD` | **文件**: `src\gitnexus\gitnexus_client.py` | **行数**: 232-248 (17行)

## 📋 概述

**说明**:

```
获取分析统计信息
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |

## 📤 返回值

**类型**: `Dict[str, Any]`

## 💻 代码实现

```python
    def get_statistics(self) -> Dict[str, Any]:
        """获取分析统计信息"""
        graph = self._load_knowledge_graph()
        
        stats = {
            "total_files": len(set(e.file_path for e in graph.entities.values())),
            "total_entities": len(graph.entities),
            "total_relationships": len(graph.relationships),
            "lines_of_code": sum(e.lines_of_code for e in graph.entities.values() if hasattr(e, "lines_of_code")),
            "entity_types": {}
        }
        
        for entity in graph.entities.values():
            entity_type = entity.entity_type
            stats["entity_types"][entity_type] = stats["entity_types"].get(entity_type, 0) + 1
        
        return stats
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\gitnexus_client.py`
- **起始行号**: 232
- **搜索关键词**: `get_statistics`

### 签名追踪
- **签名**: `9f084be123058d7a...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
