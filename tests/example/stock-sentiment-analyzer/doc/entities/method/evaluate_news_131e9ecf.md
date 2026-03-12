---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 131e9ecff31bcdfc
signature: b83cf57081cc08001291b351747ac400
created: '2026-03-11T10:11:01.326076'
file_path: src\ai\ai_client.py
start_line: 37
end_line: 47
lines_of_code: 11
tags:
- entity
- method
- evaluate_news
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 3
---

# evaluate_news

> **类型**: `EntityType.METHOD` | **文件**: `src\ai\ai_client.py` | **行数**: 37-47 (11行)

## 📋 概述

**说明**:

```
评估资讯

Args:
    news: 资讯信息
    expert_type: 专家类型

Returns:
    评估结果
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `news` | `any` | - |
| `expert_type` | `any` | - |

## 📤 返回值

**类型**: `Dict[str, Any]`

## 💻 代码实现

```python
    async def evaluate_news(self, news: Dict[str, Any], expert_type: str) -> Dict[str, Any]:
        """评估资讯

        Args:
            news: 资讯信息
            expert_type: 专家类型

        Returns:
            评估结果
        """
        pass
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\ai\ai_client.py`
- **起始行号**: 37
- **搜索关键词**: `evaluate_news`

### 签名追踪
- **签名**: `b83cf57081cc0800...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
