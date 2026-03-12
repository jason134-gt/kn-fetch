---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: 56dca22d188183ad
signature: d278d8dd52d66a8bffbf059facb67cc3
created: '2026-03-11T10:11:01.197157'
file_path: src\ai\ai_client.py
start_line: 50
end_line: 243
lines_of_code: 194
tags:
- entity
- class
- OpenAIClient
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# OpenAIClient

> **类型**: `EntityType.CLASS` | **文件**: `src\ai\ai_client.py` | **行数**: 50-243 (194行)

## 📋 概述

**说明**:

```
OpenAI客户端
```

## 💻 代码片段（节选）

```python
class OpenAIClient(AIClient):
    """OpenAI客户端"""

    def __init__(self, config: Dict[str, Any]):
        """初始化OpenAI客户端

        Args:
            config: 配置字典
        """
        super().__init__(config)

        self.api_key = config.get('openai', {}).get('api_key', '')
        self.base_url = config.get('openai', {}).get('base_url', 'https://api.openai.com/v1')
        self.model = config.get('openai', {}).get('model', 'gpt-4')
        self.temperature = config.get('openai', {}).get('temperature', 0.7)

        if not self.api_key:
            self.logger.warning("OpenAI API Key未配置")

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
                'Authorization': f'Bearer {self.api_key}',
                'Content-Type': 'application/json'
            }

            data = {
                'model': self.model,
                'messages': messages,
                'temperature': kwargs.get('temperature', self.temperature)
            }

            async with aiohttp.ClientSession() as session:
                async with session.post(
                    f"{self.base_url}/chat/completions",
                    headers=headers,
                    json=data
                ) as response:
                    if response.status != 200:
                        error_text = await response.text()
                        self.logger.error(f"OpenAI API错误: {response.status}, {error_text}")
                        return ""

                    result = await response.json()

                    return result['choices'][0]['message']['content']

        
// ... 省略 3766 字符 ...
```

## 🔧 重构建议

- 方法过长 (194 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\ai\ai_client.py`
- **起始行号**: 50
- **搜索关键词**: `OpenAIClient`

### 签名追踪
- **签名**: `d278d8dd52d66a8b...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
