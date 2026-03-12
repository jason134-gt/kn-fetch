---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: 71edde94f5f5d726
signature: de4895190e533737703f8af6f9b7ad2d
created: '2026-03-11T10:11:01.190152'
file_path: src\aggregator\result_aggregator.py
start_line: 9
end_line: 279
lines_of_code: 271
tags:
- entity
- class
- ResultAggregator
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# ResultAggregator

> **类型**: `EntityType.CLASS` | **文件**: `src\aggregator\result_aggregator.py` | **行数**: 9-279 (271行)

## 📋 概述

**说明**:

```
结果汇总器
```

## 💻 代码片段（节选）

```python
class ResultAggregator:
    """结果汇总器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化汇总器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        # 获取专家权重配置
        self.expert_weights = config.get('expert_weights', {})

        # 影响等级映射
        self.impact_level_map = {
            '无影响': 0,
            '微影响': 1,
            '中影响': 2,
            '强影响': 3
        }

        # 影响方向映射
        self.impact_direction_map = {
            '利空': -1,
            '中性': 0,
            '利好': 1
        }

        # 影响时长映射
        self.impact_duration_map = {
            '短期': 1,
            '中期': 2,
            '长期': 3
        }

        # 反向映射
        self.reverse_level_map = {v: k for k, v in self.impact_level_map.items()}
        self.reverse_direction_map = {v: k for k, v in self.impact_direction_map.items()}
        self.reverse_duration_map = {v: k for k, v in self.impact_duration_map.items()}

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
     
// ... 省略 5951 字符 ...
```

## 🔧 重构建议

- 方法过长 (271 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\aggregator\result_aggregator.py`
- **起始行号**: 9
- **搜索关键词**: `ResultAggregator`

### 签名追踪
- **签名**: `de4895190e533737...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
