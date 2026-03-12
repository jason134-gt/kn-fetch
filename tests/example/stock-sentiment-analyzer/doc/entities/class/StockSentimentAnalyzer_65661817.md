---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: 65661817ed9f50e1
signature: 2a07bcc8de7a35cdaccbac2f53b1737b
created: '2026-03-11T10:11:01.188129'
file_path: src\main.py
start_line: 26
end_line: 177
lines_of_code: 152
tags:
- entity
- class
- StockSentimentAnalyzer
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# StockSentimentAnalyzer

> **类型**: `EntityType.CLASS` | **文件**: `src\main.py` | **行数**: 26-177 (152行)

## 📋 概述

**说明**:

```
股票舆情分析系统主类
```

## 💻 代码片段（节选）

```python
class StockSentimentAnalyzer:
    """股票舆情分析系统主类"""

    def __init__(self, config_path: str = "config/config.yaml"):
        """初始化分析系统

        Args:
            config_path: 配置文件路径
        """
        # 加载配置
        self.config = ConfigLoader.load(config_path)

        # 设置日志
        self.logger = setup_logger(
            level=self.config.get('logging', {}).get('level', 'INFO'),
            log_file=self.config.get('logging', {}).get('file', './logs/analyzer.log')
        )

        # 初始化各模块
        self.collector = NewsCollector(self.config)
        self.eastmoney_collector = EastMoneyCollector(self.config)
        self.cls_collector = ClsCollector(self.config)
        self.xueqiu_collector = XueqiuCollector(self.config)
        self.expert_panel = ExpertPanel(self.config)
        self.aggregator = ResultAggregator(self.config)
        self.trader = ShortTermTrader(self.config)
        self.reporter = ReportGenerator(self.config)
        self.scheduler = TaskScheduler(self.config)

        # 初始化AI客户端
        self.ai_client = create_ai_client(self.config)
        if self.ai_client:
            self.logger.info("AI客户端初始化成功")
        else:
            self.logger.warning("AI客户端初始化失败，将使用规则引擎")

        # 初始化集成数据提供器（支持API和通达信数据）
        self.stock_data_provider = IntegratedDataProvider(self.config)

        self.logger.info("股票舆情分析系统初始化完成")

    async def run_once(self):
        """执行一次完整的分析流程"""
        self.logger.info("=" * 60)
        self.logger.info("开始执行分析流程")
        self.logger.info("=" * 60)

        try:
            # 1. 采集资讯
            self.logger.info("步骤1: 采集实时资讯...")

            # 使用真实数据源
            eastmoney_news = await self.eastmoney_collector.collect()
            cls_news = await self.cls_collector.collect()
            xueqiu_data = await self.xu
// ... 省略 3262 字符 ...
```

## 🔧 重构建议

- 方法过长 (152 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\main.py`
- **起始行号**: 26
- **搜索关键词**: `StockSentimentAnalyzer`

### 签名追踪
- **签名**: `2a07bcc8de7a35cd...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
