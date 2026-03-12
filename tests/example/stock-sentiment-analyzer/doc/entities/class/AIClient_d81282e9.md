---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: d81282e9a861af61
signature: 49450409fe36cafc3821f4eae3884565
created: '2026-03-11T10:11:01.194181'
file_path: src\ai\ai_client.py
start_line: 11
end_line: 47
lines_of_code: 37
tags:
- entity
- class
- AIClient
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# AIClient

> **类型**: `EntityType.CLASS` | **文件**: `src\ai\ai_client.py` | **行数**: 11-47 (37行)

## 📋 概述

**说明**:

```
AI客户端基类
```

## 💻 代码实现

```python
class AIClient(ABC):
    """AI客户端基类"""

    def __init__(self, config: Dict[str, Any]):
        """初始化AI客户端

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

    @abstractmethod
    async def chat(self, messages: List[Dict[str, str]], **kwargs) -> str:
        """聊天接口

        Args:
            messages: 消息列表
            **kwargs: 其他参数

        Returns:
            AI回复
        """
        pass

    @abstractmethod
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
- **起始行号**: 11
- **搜索关键词**: `AIClient`

### 签名追踪
- **签名**: `49450409fe36cafc...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
