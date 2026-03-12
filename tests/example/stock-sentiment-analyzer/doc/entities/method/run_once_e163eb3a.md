---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: e163eb3ac773d8f1
signature: 95e8b6b3c7632a28302f0bdc985ec5dd
created: '2026-03-11T10:11:01.265707'
file_path: src\main.py
start_line: 67
end_line: 144
lines_of_code: 78
tags:
- entity
- method
- run_once
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# run_once

> **类型**: `EntityType.METHOD` | **文件**: `src\main.py` | **行数**: 67-144 (78行)

## 📋 概述

**说明**:

```
执行一次完整的分析流程
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |

## 💻 代码片段（节选）

```python
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
            xueqiu_data = await self.xueqiu_collector.collect()

            # 合并资讯
            news_list = eastmoney_news + cls_news + xueqiu_data

            # 去重
            seen_urls = set()
            unique_news = []
            for news in news_list:
                url = news.get('url', '')
                if url and url not in seen_urls:
                    seen_urls.add(url)
                    unique_news.append(news)

            news_list = unique_news

            self.logger.info(f"采集到 {len(news_list)} 条资讯")

            if not news_list:
                self.logger.warning("未采集到有效资讯，跳过后续分析")
                return

            # 2. 专家评估
            self.logger.info("步骤2: 专家圆桌评估...")
            expert_results = []
            for news in news_list:
                result = await self.expert_panel.evaluate(news)
                expert_results.append(result)
            self.logger.info(f"完成 {len(expert_results)} 条资讯的专家评估")

            # 3. 结果汇总
            self.logger.info("步骤3: 综合汇总...")
            aggregated_results = []
            for i, news in enumerate(news_list):
                result = await self.aggregator.aggregate(news, expert_results[i])
                aggregated_results.append(result)
            self.logger.info(f"完成 {len(aggregated_results)} 条资讯的综合汇总")

            # 4. 短线决策
            self.logger.info("步骤4: 短线高手决策...")
            trading_dec
// ... 省略 911 字符 ...
```

## 🔧 重构建议

- 方法过长 (78 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\main.py`
- **起始行号**: 67
- **搜索关键词**: `run_once`

### 签名追踪
- **签名**: `95e8b6b3c7632a28...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
