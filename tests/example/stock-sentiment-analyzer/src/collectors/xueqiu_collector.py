"""
雪球网数据采集器
"""

import aiohttp
import logging
from typing import List, Dict, Any
from datetime import datetime
from bs4 import BeautifulSoup
import re


class XueqiuCollector:
    """雪球网采集器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化采集器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        self.base_url = "https://xueqiu.com"
        self.hot_url = "https://xueqiu.com/hots"
        self.news_url = "https://xueqiu.com/news"
        self.timeout = config.get('collector', {}).get('timeout_seconds', 30)
        # 雪球需要浏览器UA
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Referer': 'https://xueqiu.com/'
        }

    async def collect(self) -> List[Dict[str, Any]]:
        """采集雪球网数据

        Returns:
            资讯/讨论列表
        """
        self.logger.info("开始采集雪球网数据")

        try:
            async with aiohttp.ClientSession(
                timeout=aiohttp.ClientTimeout(total=self.timeout),
                headers=self.headers
            ) as session:
                # 先获取cookie
                await self._init_session(session)
                
                # 采集热门讨论
                hot_discussions = await self._collect_hot_discussions(session)

                # 采集财经新闻
                news = await self._collect_news(session)

                # 合并结果
                all_data = hot_discussions + news

                self.logger.info(f"雪球网采集完成，共 {len(all_data)} 条数据")
                return all_data

        except Exception as e:
            self.logger.error(f"雪球网采集失败: {e}", exc_info=True)
            return []

    async def _init_session(self, session: aiohttp.ClientSession):
        """初始化会话，获取必要的cookie"""
        try:
            await session.get(self.base_url)
        except Exception as e:
            self.logger.debug(f"初始化雪球会话失败: {e}")

    async def _collect_hot_discussions(self, session: aiohttp.ClientSession) -> List[Dict[str, Any]]:
        """采集热门讨论

        Args:
            session: HTTP会话

        Returns:
            讨论列表
        """
        discussions = []

        try:
            # 雪球热门讨论API
            api_url = "https://xueqiu.com/v4/statuses/public_timeline_by_category.json?category=-1&count=50"

            async with session.get(api_url) as response:
                if response.status != 200:
                    self.logger.warning(f"雪球热门讨论API返回状态码: {response.status}")
                    return []

                data = await response.json()

                # 解析数据
                for item in data.get('list', []):
                    item_data = item.get('data', {})
                    discussion = self._parse_discussion_item(item_data)
                    if discussion:
                        discussions.append(discussion)

        except Exception as e:
            self.logger.error(f"采集雪球热门讨论失败: {e}")

        return discussions

    async def _collect_news(self, session: aiohttp.ClientSession) -> List[Dict[str, Any]]:
        """采集财经新闻

        Args:
            session: HTTP会话

        Returns:
            新闻列表
        """
        news_list = []

        try:
            # 雪球新闻API
            api_url = "https://xueqiu.com/v4/news/home.json?count=30"

            async with session.get(api_url) as response:
                if response.status != 200:
                    self.logger.warning(f"雪球新闻API返回状态码: {response.status}")
                    return []

                data = await response.json()

                # 解析数据
                for item in data.get('news', []):
                    news = self._parse_news_item(item)
                    if news:
                        news_list.append(news)

        except Exception as e:
            self.logger.error(f"采集雪球新闻失败: {e}")

        return news_list

    def _parse_discussion_item(self, item: Dict[str, Any]) -> Dict[str, Any]:
        """解析讨论项

        Args:
            item: 讨论数据

        Returns:
            资讯字典
        """
        try:
            user = item.get('user', {})
            title = item.get('title', '') or item.get('description', '')
            content = item.get('text', '') or title
            
            # 移除HTML标签
            content = re.sub(r'<[^>]+>', '', content)
            title = re.sub(r'<[^>]+>', '', title)
            
            if not title:
                return None
                
            url = f"{self.base_url}{item.get('target', '')}"

            # 时间戳转换
            created_at = item.get('created_at', 0)
            if created_at:
                try:
                    dt = datetime.fromtimestamp(created_at / 1000)
                    timestamp = dt.strftime('%Y-%m-%d %H:%M:%S')
                except:
                    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            else:
                timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

            # 讨论热度
            view_count = item.get('view_count', 0)
            importance = '高' if view_count > 100000 else '中' if view_count > 10000 else '低'

            return {
                'timestamp': timestamp,
                'source': '雪球网',
                'author': user.get('screen_name', '匿名用户'),
                'title': title,
                'content': content,
                'url': url,
                'category': '用户讨论',
                'view_count': view_count,
                'like_count': item.get('like_count', 0),
                'reply_count': item.get('reply_count', 0),
                'sector': self._extract_sectors(title + content),
                'importance': importance,
                'tags': self._extract_tags(title + content)
            }

        except Exception as e:
            self.logger.debug(f"解析讨论项失败: {e}")
            return None

    def _parse_news_item(self, item: Dict[str, Any]) -> Dict[str, Any]:
        """解析新闻项

        Args:
            item: 新闻数据

        Returns:
            资讯字典
        """
        try:
            title = item.get('title', '')
            content = item.get('description', '') or title
            url = f"{self.base_url}{item.get('target', '')}"

            # 时间戳转换
            created_at = item.get('created_at', 0)
            if created_at:
                try:
                    dt = datetime.fromtimestamp(created_at / 1000)
                    timestamp = dt.strftime('%Y-%m-%d %H:%M:%S')
                except:
                    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            else:
                timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

            return {
                'timestamp': timestamp,
                'source': '雪球网',
                'title': title,
                'content': content,
                'url': url,
                'category': '财经新闻',
                'sector': self._extract_sectors(title + content),
                'importance': '中',
                'tags': self._extract_tags(title + content)
            }

        except Exception as e:
            self.logger.debug(f"解析新闻项失败: {e}")
            return None

    def _extract_sectors(self, text: str) -> List[str]:
        """从文本中提取板块

        Args:
            text: 文本内容

        Returns:
            板块列表
        """
        sectors = []
        text_lower = text.lower()

        # 定义板块关键词（和其他采集器保持一致）
        sector_keywords = {
            '银行': ['银行', '金融'],
            '券商': ['券商', '证券'],
            '保险': ['保险'],
            '白酒': ['白酒', '茅台', '五粮液'],
            '医药': ['医药', '医疗', '生物'],
            '半导体': ['半导体', '芯片', '集成电路'],
            '新能源': ['新能源', '光伏', '风电', '锂电'],
            '军工': ['军工', '航空', '航天'],
            '房地产': ['房地产', '地产', '住房'],
            '基建': ['基建', '建筑', '工程'],
            '人工智能': ['人工智能', 'AI', '算力'],
            '数字经济': ['数字', '云计算', '大数据']
        }

        for sector, keywords in sector_keywords.items():
            if any(keyword in text_lower for keyword in keywords):
                sectors.append(sector)

        return sectors

    def _extract_tags(self, text: str) -> List[str]:
        """从文本中提取标签

        Args:
            text: 文本内容

        Returns:
            标签列表
        """
        tags = []
        text_lower = text.lower()

        # 定义标签关键词（和其他采集器保持一致）
        tag_keywords = {
            '降准': ['降准', '存款准备金'],
            '降息': ['降息', '利率'],
            '加息': ['加息'],
            '回购': ['回购'],
            '重组': ['重组', '并购'],
            '减持': ['减持'],
            '增持': ['增持'],
            '分红': ['分红', '派息'],
            '业绩': ['业绩', '财报', '净利润'],
            '政策': ['政策', '通知', '意见'],
            '监管': ['监管', '处罚'],
            '看多': ['看多', '买入', '上涨', '利好'],
            '看空': ['看空', '卖出', '下跌', '利空']
        }

        for tag, keywords in tag_keywords.items():
            if any(keyword in text_lower for keyword in keywords):
                tags.append(tag)

        return tags
