---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 32cab51d7f3e1abd
signature: 31d74113a7670b83c0af184c5d68360c
created: '2026-03-11T10:11:01.388499'
file_path: src\collectors\cls_collector.py
start_line: 28
end_line: 46
lines_of_code: 19
tags:
- entity
- method
- collect
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 1
---

# collect

> **类型**: `EntityType.METHOD` | **文件**: `src\collectors\cls_collector.py` | **行数**: 28-46 (19行)

## 📋 概述

**说明**:

```
采集财联社资讯

Returns:
    资讯列表
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |

## 📤 返回值

**类型**: `List[Dict[str, Any]]`

## 💻 代码实现

```python
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
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\collectors\cls_collector.py`
- **起始行号**: 28
- **搜索关键词**: `collect`

### 签名追踪
- **签名**: `31d74113a7670b83...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
