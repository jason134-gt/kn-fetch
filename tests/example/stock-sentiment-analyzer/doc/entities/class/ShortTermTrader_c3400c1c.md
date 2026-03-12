---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: c3400c1c7e75d444
signature: 01a3271d640aaf24fd37db42f761a15f
created: '2026-03-11T10:11:01.249312'
file_path: src\trader\short_term_trader.py
start_line: 9
end_line: 441
lines_of_code: 433
tags:
- entity
- class
- ShortTermTrader
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# ShortTermTrader

> **类型**: `EntityType.CLASS` | **文件**: `src\trader\short_term_trader.py` | **行数**: 9-441 (433行)

## 📋 概述

**说明**:

```
短线交易决策器
```

## 💻 代码片段（节选）

```python
class ShortTermTrader:
    """短线交易决策器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化交易决策器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        # 获取交易配置
        self.trader_config = config.get('trader', {})
        self.analyzer_config = config.get('analyzer', {})

        # 获取股票池
        self.stock_pool = config.get('stock_pool', [])

        # 选股标准
        self.min_volume = self.trader_config.get('min_volume', 100000000)
        self.max_volume = self.trader_config.get('max_volume', 10000000000)
        self.min_turnover = self.trader_config.get('min_turnover', 2.0)
        self.max_pe_ratio = self.trader_config.get('max_pe_ratio', 100)
        self.min_market_cap = self.trader_config.get('min_market_cap', 5000000000)

    async def decide(self, news: Dict[str, Any], aggregated_result: Dict[str, Any]) -> Dict[str, Any]:
        """短线决策

        Args:
            news: 资讯信息
            aggregated_result: 汇总结果

        Returns:
            决策结果
        """
        self.logger.info(f"开始短线决策: {news.get('title', '')}")

        # 获取汇总结果
        aggregated = aggregated_result.get('aggregated_result', {})
        impact_level = aggregated.get('impact_level', '微影响')
        impact_direction = aggregated.get('impact_direction', '中性')
        affected_sectors = aggregated.get('affected_sectors', [])
        confidence = aggregated.get('confidence', 0.5)

        # 定位受影响的概念
        affected_concepts = self._identify_concepts(news, affected_sectors)

        # 筛选个股
        stock_analysis = self._filter_stocks(
            affected_concepts,
            impact_level,
            impact_direction,
            confidence
        )

        # 构建结果
        result = {
            '
// ... 省略 10221 字符 ...
```

## 🔧 重构建议

- 方法过长 (433 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\trader\short_term_trader.py`
- **起始行号**: 9
- **搜索关键词**: `ShortTermTrader`

### 签名追踪
- **签名**: `01a3271d640aaf24...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
