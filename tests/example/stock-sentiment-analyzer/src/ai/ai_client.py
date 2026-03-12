"""
AI模型客户端
"""

import logging
import json
from typing import Dict, Any, List, Optional
from abc import ABC, abstractmethod


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

        except Exception as e:
            self.logger.error(f"OpenAI调用失败: {e}", exc_info=True)
            return ""

    async def evaluate_news(self, news: Dict[str, Any], expert_type: str) -> Dict[str, Any]:
        """评估资讯

        Args:
            news: 资讯信息
            expert_type: 专家类型

        Returns:
            评估结果
        """
        # 构建提示词
        prompt = self._build_evaluation_prompt(news, expert_type)

        # 调用AI
        response = await self.chat([
            {'role': 'system', 'content': prompt['system']},
            {'role': 'user', 'content': prompt['user']}
        ])

        # 解析响应
        return self._parse_evaluation_response(response, expert_type)

    def _build_evaluation_prompt(self, news: Dict[str, Any], expert_type: str) -> Dict[str, str]:
        """构建评估提示词

        Args:
            news: 资讯信息
            expert_type: 专家类型

        Returns:
            提示词字典
        """
        # 专家类型描述
        expert_descriptions = {
            'macro_economy': '宏观经济专家，专注于政策、利率、通胀、汇率等宏观因素对市场的影响',
            'industry_research': '行业研究员，专注于行业景气度、竞争格局变化',
            'technical_analysis': '技术面分析专家，专注于技术形态、量价关系',
            'capital_flow': '资金面分析专家，专注于主力资金、北向资金流向',
            'risk_control': '风险管控专家，专注于风险点、黑天鹅概率、监管风险'
        }

        expert_desc = expert_descriptions.get(expert_type, '专家')

        system_prompt = f"""你是{expert_desc}。

请对以下股票资讯进行评估，输出从JSON格式。

评估维度：
1. 影响等级：无影响 | 微影响 | 中影响 | 强影响
2. 影响方向：利好 | 利空 | 中性
3. 影响时长：短期(<3天) | 中期(1-2周) | 长期(>2周)
4. 核心逻辑依据：简要说明评估理由
5. 置信度：0-1之间的数值，表示评估的可信度
6. 受影响板块：列表，列出受影响的板块

输出格式：
{{
  "impact_level": "中影响",
  "impact_direction": "利好",
  "impact_duration": "中期",
  "logic": "央行降准释放流动性，利好银行板块",
  "confidence": 0.85,
  "affected_sectors": ["银行", "券商"]
}}"""

        user_prompt = f"""资讯标题：{news.get('title', '')}
资讯内容：{news.get('content', '')}
资讯来源：{news.get('source', '')}
发布时间：{news.get('timestamp', '')}

请评估这条资讯对市场的影响。"""

        return {
            'system': system_prompt,
            'user': user_prompt
        }

    def _parse_evaluation_response(self, response: str, expert_type: str) -> Dict[str, Any]:
        """解析评估响应

        Args:
            response: AI响应
            expert_type: 专家类型

        Returns:
            评估结果
        """
        try:
            # 尝试解析JSON
            result = json.loads(response)

            return {
                'expert_id': expert_type,
                'expert_name': self._get_expert_name(expert_type),
                'impact_level': result.get('impact_level', '微影响'),
                'impact_direction': result.get('impact_direction', '中性'),
                'impact_duration': result.get('impact_duration', '短期'),
                'logic': result.get('logic', ''),
                'confidence': result.get('confidence', 0.5),
                'affected_sectors': result.get('affected_sectors', [])
            }

        except Exception as e:
            self.logger.error(f"解析AI响应失败: {e}")
            return {
                'expert_id': expert_type,
                'expert_name': self._get_expert_name(expert_type),
                'impact_level': '微影响',
                'impact_direction': '中性',
                'impact_duration': '短期',
                'logic': 'AI解析失败，使用默认评估',
                'confidence': 0.3,
                'affected_sectors': []
            }

    def _get_expert_name(self, expert_type: str) -> str:
        """获取专家名称

        Args:
            expert_type: 专家类型

        Returns:
            专家名称
        """
        names = {
            'macro_economy': '宏观经济专家',
            'industry_research': '行业研究员',
            'technical_analysis': '技术面分析专家',
            'capital_flow': '资金面分析专家',
            'risk_control': '风险管控专家'
        }
        return names.get(expert_type, '专家')


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
                async with session.post(
                    f"{self.base_url}/messages",
                    headers=headers,
                    json=data
                ) as response:
                    if response.status != 200:
                        error_text = await response.text()
                        self.logger.error(f"Claude API错误: {response.status}, {error_text}")
                        return ""

                    result = await response.json()

                    return result['content'][0]['text']

        except Exception as e:
            self.logger.error(f"Claude调用失败: {e}", exc_info=True)
            return ""

    async def evaluate_news(self, news: Dict[str, Any], expert_type: str) -> Dict[str, Any]:
        """评估资讯

        Args:
            news: 资讯信息
            expert_type: 专家类型

        Returns:
            评估结果
        """
        # 构建提示词
        prompt = self._build_evaluation_prompt(news, expert_type)

        # 调用AI
        response = await self.chat([
            {'role': 'system', 'content': prompt['system']},
            {'role': 'user', 'content': prompt['user']}
        ])

        # 解析响应
        return self._parse_evaluation_response(response, expert_type)

    def _build_evaluation_prompt(self, news: Dict[str, Any], expert_type: str) -> Dict[str, str]:
        """构建评估提示词

        Args:
            news: 资讯信息
            expert_type: 专家类型

        Returns:
            提示词字典
        """
        # 专家类型描述
        expert_descriptions = {
            'macro_economy': '宏观经济专家，专注于政策、利率、通胀、汇率等宏观因素对市场的影响',
            'industry_research': '行业研究员，专注于行业景气度、竞争格局变化',
            'technical_analysis': '技术面分析专家，专注于技术形态、量价关系',
            'capital_flow': '资金面分析专家，专注于主力资金、北向资金流向',
            'risk_control': '风险管控专家，专注于风险点、黑天鹅概率、监管风险'
        }

        expert_desc = expert_descriptions.get(expert_type, '专家')

        system_prompt = f"""你是{expert_desc}。

请对以下股票资讯进行评估，输出JSON格式。

评估维度：
1. 影响等级：无影响 | 微影响 | 中影响 | 强影响
2. 影响方向：利好 | 利空 | 中性
3. 影响时长：短期(<3天) | 中期(1-2周) | 长期(>2周)
4. 核心逻辑依据：简要说明评估理由
5. 置信度：0-1之间的数值，表示评估的可信度
6. 受影响板块：列表，列出受影响的板块

输出格式：
{{
  "impact_level": "中影响",
  "impact_direction": "利好",
  "impact_duration": "中期",
  "logic": "央行降准释放流动性，利好银行板块",
  "confidence": 0.85,
  "affected_sectors": ["银行", "券商"]
}}"""

        user_prompt = f"""资讯标题：{news.get('title', '')}
资讯内容：{news.get('content', '')}
资讯来源：{news.get('source', '')}
发布时间：{news.get('timestamp', '')}

请评估这条资讯对市场的影响。"""

        return {
            'system': system_prompt,
            'user': user_prompt
        }

    def _parse_evaluation_response(self, response: str, expert_type: str) -> Dict[str, Any]:
        """解析评估响应

        Args:
            response: AI响应
            expert_type: 专家类型

        Returns:
            评估结果
        """
        try:
            # 尝试解析JSON
            result = json.loads(response)

            return {
                'expert_id': expert_type,
                'expert_name': self._get_expert_name(expert_type),
                'impact_level': result.get('impact_level', '微影响'),
                'impact_direction': result.get('impact_direction', '中性'),
                'impact_duration': result.get('impact_duration', '短期'),
                'logic': result.get('logic', ''),
                'confidence': result.get('confidence', 0.5),
                'affected_sectors': result.get('affected_sectors', [])
            }

        except Exception as e:
            self.logger.error(f"解析AI响应失败: {e}")
            return {
                'expert_id': expert_type,
                'expert_name': self._get_expert_name(expert_type),
                'impact_level': '微影响',
                'impact_direction': '中性',
                'impact_duration': '短期',
                'logic': 'AI解析失败，使用默认评估',
                'confidence': 0.3,
                'affected_sectors': []
            }

    def _get_expert_name(self, expert_type: str) -> str:
        """获取专家名称

        Args:
            expert_type: 专家类型

        Returns:
            专家名称
        """
        names = {
            'macro_economy': '宏观经济专家',
            'industry_research': '行业研究员',
            'technical_analysis': '技术面分析专家',
            'capital_flow': '资金面分析专家',
            'risk_control': '风险管控专家'
        }
        return names.get(expert_type, '专家')


def create_ai_client(config: Dict[str, Any]) -> Optional[AIClient]:
    """创建AI客户端

    Args:
        config: 配置字典

    Returns:
        AI客户端实例
    """
    ai_config = config.get('ai', {})
    provider = ai_config.get('provider', 'openai')

    if provider == 'openai':
        return OpenAIClient(config)
    elif provider == 'claude':
        return ClaudeClient(config)
    else:
        logging.getLogger(__name__).warning(f"不支持的AI提供商: {provider}")
        return None
