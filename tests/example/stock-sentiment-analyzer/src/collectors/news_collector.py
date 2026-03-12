"""
实时资讯采集模块
"""

import asyncio
import aiohttp
import logging
from datetime import datetime
from typing import List, Dict, Any
from bs4 import BeautifulSoup


class NewsCollector:
    """资讯采集器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化采集器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        # 获取数据源配置
        self.data_sources = config.get('data_sources', {})
        self.collector_config = config.get('collector', {})

        # 采集配置
        self.interval_minutes = self.collector_config.get('interval_minutes', 30)
        self.batch_size = self.collector_config.get('batch_size', 10)
        self.max_items_per_source = self.collector_config.get('max_items_per_source', 20)
        self.timeout_seconds = self.collector_config.get('timeout_seconds', 30)

        # 去重缓存
        self.seen_urls = set()

    async def collect(self) -> List[Dict[str, Any]]:
        """采集资讯

        Returns:
            资讯列表
        """
        all_news = []

        # 采集官方渠道
        official_news = await self._collect_from_sources(
            self.data_sources.get('official', [])
        )
        all_news.extend(official_news)

        # 采集权威媒体
        media_news = await self._collect_from_sources(
            self.data_sources.get('media', [])
        )
        all_news.extend(media_news)

        # 采集核心平台
        platform_news = await self._collect_from_sources(
            self.data_sources.get('platforms', [])
        )
        all_news.extend(platform_news)

        # 去重
        unique_news = self._deduplicate(all_news)

        # 按时间排序
        unique_news.sort(key=lambda x: x['timestamp'], reverse=True)

        # 限制数量
        if len(unique_news) > self.batch_size:
            unique_news = unique_news[:self.batch_size]

        self.logger.info(f"采集完成，共 {len(unique_news)} 条有效资讯")
        return unique_news

    async def _collect_from_sources(self, sources: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """从指定数据源采集资讯

        Args:
            sources: 数据源列表

        Returns:
            资讯列表
        """
        all_news = []

        async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=self.timeout_seconds)) as session:
            tasks = []
            for source in sources:
                if source.get('enabled', True):
                    task = self._fetch_from_source(session, source)
                    tasks.append(task)

            results = await asyncio.gather(*tasks, return_exceptions=True)

            for result in results:
                if isinstance(result, Exception):
                    self.logger.error(f"采集失败: {result}")
                elif result:
                    all_news.extend(result)

        return all_news

    async def _fetch_from_source(self, session: aiohttp.ClientSession, source: Dict[str, Any]) -> List[Dict[str, Any]]:
        """从单个数据源获取资讯

        Args:
            session: HTTP会话
            source: 数据源配置

        Returns:
            资讯列表
        """
        url = source.get('url')
        name = source.get('name')

        if not url:
            return []

        try:
            self.logger.info(f"正在采集: {name} ({url})")

            async with session.get(url) as response:
                if response.status != 200:
                    self.logger.warning(f"{name} 返回状态码: {response.status}")
                    return []

                html = await response.text()
                news_list = self._parse_html(html, source)

                self.logger.info(f"{name} 采集到 {len(news_list)} 条资讯")
                return news_list

        except Exception as e:
            self.logger.error(f"{name} 采集异常: {e}")
            return []

    def _parse_html(self, html: str, source: Dict[str, Any]) -> List[Dict[str, Any]]:
        """解析HTML，提取资讯

        Args:
            html: HTML内容
            source: 数据源配置

        Returns:
            资讯列表
        """
        news_list = []
        soup = BeautifulSoup(html, 'html.parser')

        # 这里需要根据不同网站的结构进行解析
        # 以下是示例代码，实际使用时需要针对每个网站编写解析逻辑

        # 示例：提取标题和链接
        # articles = soup.find_all('div', class_='article-item')
        # for article in articles[:self.max_items_per_source]:
        #     title = article.find('a').text.strip()
        #     url = article.find('a')['href']
        #     timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        #
        #     news_list.append({
        #         'timestamp': timestamp,
        #         'source': source.get('name'),
        #         'title': title,
        #         'content': title,  # 简化处理，实际应提取正文
        #         'url': url,
        #         'category': '未知',
        #         'sector': [],
        #         'importance': '中',
        #         'tags': []
        #     })

        # 模拟数据（用于演示）
        if len(news_list) == 0:
            news_list = self._generate_mock_data(source)

        return news_list

    def _generate_mock_data(self, source: Dict[str, Any]) -> List[Dict[str, Any]]:
        """生成模拟数据（用于演示）

        Args:
            source: 数据源配置

        Returns:
            模拟资讯列表
        """
        mock_news = [
            {
                'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                'source': source.get('name'),
                'title': '央行宣布降准0.5个百分点，释放长期资金约1万亿元',
                'content': '中国人民银行决定于近期下调金融机构存款准备金率0.5个百分点，释放长期资金约1万亿元，以支持实体经济发展。',
                'url': f"{source.get('url')}/news/001",
                'category': '政策变动',
                'sector': ['银行', '金融'],
                'importance': '高',
                'tags': ['降准', '货币政策', '流动性']
            },
            {
                'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                'source': source.get('name'),
                'title': '半导体行业获得政策支持，国产替代加速',
                'content': '国家发改委等部门发布通知，加大对半导体产业的支持力度，推动国产替代进程。',
                'url': f"{source.get('url')}/news/002",
                'category': '行业利好',
                'sector': ['半导体', '芯片', '电子'],
                'importance': '高',
                'tags': ['半导体', '国产替代', '政策支持']
            }
        ]

        return mock_news[:self.max_items_per_source]

    def _deduplicate(self, news_list: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """去重

        Args:
            news_list: 资讯列表

        Returns:
            去重后的资讯列表
        """
        unique_news = []
        seen_urls = set()

        for news in news_list:
            url = news.get('url')
            if url and url not in seen_urls:
                seen_urls.add(url)
                unique_news.append(news)

        return unique_news
