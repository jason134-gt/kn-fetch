---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: 492258cf241bb5a8
signature: 42ee332d862a4f37caadb926cc1aa0df
created: '2026-03-11T10:11:01.203321'
file_path: src\collectors\cls_collector.py
start_line: 12
end_line: 199
lines_of_code: 188
tags:
- entity
- class
- ClsCollector
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# ClsCollector

> **类型**: `EntityType.CLASS` | **文件**: `src\collectors\cls_collector.py` | **行数**: 12-199 (188行)

## 📋 概述

**说明**:

```
财联社采集器
```

## 💻 代码片段（节选）

```python
class ClsCollector:
    """财联社采集器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化采集器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        self.base_url = "https://www.cls.cn"
        self.news_url = "https://www.cls.cn/telegraph"
        self.timeout = config.get('collector', {}).get('timeout_seconds', 30)

    async def collect(self) -> List[Dict[str, Any]]:
        """采集财联社资讯

        Returns:
            资讯列表
        """
        self.logger.info("开始采集财联社资讯")

        try:
            async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=self.timeout)) as session:
                # 采集电报
                telegraph_news = await self._collect_telegraph(session)

                self.logger.info(f"财联社采集完成，共 {len(telegraph_news)} 条资讯")
                return telegraph_news

        except Exception as e:
            self.logger.error(f"财联社采集失败: {e}", exc_info=True)
            return []

    async def _collect_telegraph(self, session: aiohttp.ClientSession) -> List[Dict[str, Any]]:
        """采集电报

        Args:
            session: HTTP会话

        Returns:
            电报列表
        """
        news_list = []

        try:
            # 财联社电报API
            api_url = "https://www.cls.cn/nodeapi/telegraphs?app=CailianpressWeb&last_time=0&os=web&rn=20"

            async with session.get(api_url) as response:
                if response.status != 200:
                    self.logger.warning(f"财联社电报API返回状态码: {response.status}")
                    return []

                data = await response.json()

                # 解析数据
                for item in data.get('data', {}).get('list', []):
                    news = self._parse_telegraph_item(item)
         
// ... 省略 3551 字符 ...
```

## 🔧 重构建议

- 方法过长 (188 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\collectors\cls_collector.py`
- **起始行号**: 12
- **搜索关键词**: `ClsCollector`

### 签名追踪
- **签名**: `42ee332d862a4f37...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
