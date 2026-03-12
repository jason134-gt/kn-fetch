---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: 6c3d18567a9c617d
signature: 4f22eb303e524418578eda7e4edbdc5a
created: '2026-03-11T10:11:01.200372'
file_path: src\ai\ai_client.py
start_line: 246
end_line: 455
lines_of_code: 210
tags:
- entity
- class
- ClaudeClient
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# ClaudeClient

> **类型**: `EntityType.CLASS` | **文件**: `src\ai\ai_client.py` | **行数**: 246-455 (210行)

## 📋 概述

**说明**:

```
Claude客户端
```

## 💻 代码片段（节选）

```python
class ClaudeClient(AIClient):
    """Claude客户端"""

    def __init__(self, config: Dict[str, Any]):
        """初始化Claude客户端

        Args:
            config: 配置字典
        """
        super().__init__(config)

        self.api_key = config.get('claude', {}).get('api_key', '')
        self.base_url = config.get('claude', {}).get('base_url', 'https://api.anthropic.com/v1')
        self.model = config.get('claude', {}).get('model', 'claude-3-sonnet-20240229')
        self.temperature = config.get('claude', {}).get('temperature', 0.7)

        if not self.api_key:
            self.logger.warning("Claude API Key未配置")

    async def chat(self, messages: List[Dict[str, str]], **kwargs) -> str:
        """聊天接口

        Args:
            messages: 消息列表
            **kwargs: 其他参数

        Returns:
            AI回复
        """
        try:
            import aiohttp

            headers = {
                'x-api-key': self.api_key,
                'Content-Type': 'application/json',
                'anthropic-version': '2023-06-01'
            }

            # 转换消息格式
            system_message = ''
            user_messages = []

            for msg in messages:
                if msg['role'] == 'system':
                    system_message = msg['content']
                else:
                    user_messages.append({
                        'role': msg['role'],
                        'content': msg['content']
                    })

            data = {
                'model': self.model,
                'system': system_message,
                'messages': user_messages,
                'temperature': kwargs.get('temperature', self.temperature),
                'max_tokens': 4096
            }

            async with aiohttp.ClientSession() as session:
                async 
// ... 省略 4281 字符 ...
```

## 🔧 重构建议

- 方法过长 (210 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\ai\ai_client.py`
- **起始行号**: 246
- **搜索关键词**: `ClaudeClient`

### 签名追踪
- **签名**: `4f22eb303e524418...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
