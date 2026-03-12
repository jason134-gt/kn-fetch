---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 43d39eba48e1bf33
signature: 118c513e714e0f419cd8e4121fa7ad6a
created: '2026-03-11T10:11:01.281986'
file_path: src\aggregator\result_aggregator.py
start_line: 51
end_line: 85
lines_of_code: 35
tags:
- entity
- method
- aggregate
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 3
---

# aggregate

> **类型**: `EntityType.METHOD` | **文件**: `src\aggregator\result_aggregator.py` | **行数**: 51-85 (35行)

## 📋 概述

**说明**:

```
汇总专家评估结果

Args:
    news: 资讯信息
    expert_results: 专家评估结果

Returns:
    汇总结果
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `news` | `any` | - |
| `expert_results` | `any` | - |

## 📤 返回值

**类型**: `Dict[str, Any]`

## 💻 代码实现

```python
    async def aggregate(self, news: Dict[str, Any], expert_results: Dict[str, Any]) -> Dict[str, Any]:
        """汇总专家评估结果

        Args:
            news: 资讯信息
            expert_results: 专家评估结果

        Returns:
            汇总结果
        """
        self.logger.info(f"开始汇总: {news.get('title', '')}")

        evaluations = expert_results.get('evaluations', {})

        # 计算加权平均
        aggregated = self._calculate_weighted_average(evaluations)

        # 识别分歧
        divergence = self._identify_divergence(evaluations)

        # 提炼共识
        consensus = self._extract_consensus(evaluations, aggregated)

        # 构建结果
        result = {
            'news_id': expert_results.get('news_id'),
            'news_title': news.get('title', ''),
            'aggregated_result': aggregated,
            'consensus': consensus,
            'divergence': divergence,
            'expert_details': evaluations
        }

        self.logger.info(f"汇总完成: {news.get('title', '')}")
        return result
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\aggregator\result_aggregator.py`
- **起始行号**: 51
- **搜索关键词**: `aggregate`

### 签名追踪
- **签名**: `118c513e714e0f41...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
