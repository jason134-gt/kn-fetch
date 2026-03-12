"""
东方财富网数据采集器
"""

import aiohttp
import logging
from typing import List, Dict, Any
from datetime import datetime
from bs4 import BeautifulSoup


class EastMoneyCollector:
    """东方财富网采集器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化采集器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        self.base_url = "https://www.eastmoney.com"
        self.news_url = "https://finance.eastmoney.com/a/cjkx.html"
        self.timeout = config.get('collector', {}).get('timeout_seconds', 30)

    async def collect(self) -> List[Dict[str, Any]]:
        """采集东方财富网资讯

        Returns:
            资讯列表
        """
        self.logger.info("开始采集东方财富网资讯")

        try:
            async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=self.timeout)) as session:
                # 采集快讯
                fast_news = await self._collect_fast_news(session)

                # 采集重要新闻
                important_news = await self._collect_important_news(session)

                # 合并结果
                all_news = fast_news + important_news

                self.logger.info(f"东方财富网采集完成，共 {len(all_news)} 条资讯")
                return all_news

        except Exception as e:
            self.logger.error(f"东方财富网采集失败: {e}", exc_info=True)
            return []

    async def _collect_fast_news(self, session: aiohttp.ClientSession) -> List[Dict[str, Any]]:
        """采集快讯

        Args:
            session: HTTP会话

        Returns:
            快讯列表
        """
        news_list = []

        try:
            # 东方财富快讯API
            api_url = "https://np-anotice-stock.eastmoney.com/api/security/ann?listType=1&clientType=web&pageSize=50"

            async with session.get(api_url) as response:
                if response.status != 200:
                    self.logger.warning(f"东方财富快讯API返回状态码: {response.status}")
                    return []

                data = await response.json()

                if data.get('code') != 0:
                    self.logger.warning(f"东方财富快讯API返回错误: {data.get('msg')}")
                    return []

                # 解析数据
                for item in data.get('data', {}).get('list', []):
                    news = self._parse_fast_news_item(item)
                    if news:
                        news_list.append(news)

        except Exception as e:
            self.logger.error(f"采集东方财富快讯失败: {e}")

        return news_list

    async def _collect_important_news(self, session: aiohttp.ClientSession) -> List[Dict[str, Any]]:
        """采集重要新闻

        Args:
            session: HTTP会话

        Returns:
            新闻列表
        """
        news_list = []

        try:
            async with session.get(self.news_url) as response:
                if response.status != 200:
                    return []

                html = await response.text()
                soup = BeautifulSoup(html, 'html.parser')

                # 查找新闻列表
                news_items = soup.find_all('div', class_='list-content')

                for item in news_items[:20]:  # 限制数量
                    try:
                        # 提取标题
                        title_elem = item.find('a')
                        if not title_elem:
                            continue

                        title = title_elem.text.strip()
                        url = title_elem.get('href', '')

                        # 提取时间
                        time_elem = item.find('span', class_='time')
                        timestamp = time_elem.text.strip() if time_elem else datetime.now().strftime('%Y-%m-%d %H:%M:%S')

                        # 提取摘要
                        summary_elem = item.find('p')
                        content = summary_elem.text.strip() if summary_elem else title

                        news_list.append({
                            'timestamp': timestamp,
                            'source': '东方财富网',
                            'title': title,
                            'content': content,
                            'url': url,
                            'category': '重要新闻',
                            'sector': self._extract_sectors(title + content),
                            'importance': '高',
                            'tags': self._extract_tags(title + content)
                        })

                    except Exception as e:
                        self.logger.debug(f"解析新闻项失败: {e}")
                        continue

        except Exception as e:
            self.logger.error(f"采集东方财富重要新闻失败: {e}")

        return news_list

    def _parse_fast_news_item(self, item: Dict[str, Any]) -> Dict[str, Any]:
        """解析快讯项

        Args:
            item: 快讯数据

        Returns:
            资讯字典
        """
        try:
            title = item.get('title', '')
            content = item.get('digest', '') or title
            url = item.get('articleUrl', '')

            # 时间戳转换
            notice_time = item.get('noticeTime', '')
            if notice_time:
                # 格式化时间
                try:
                    dt = datetime.fromtimestamp(notice_time / 1000)
                    timestamp = dt.strftime('%Y-%m-%d %H:%M:%S')
                except:
                    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            else:
                timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

            return {
                'timestamp': timestamp,
                'source': '东方财富网',
                'title': title,
                'content': content,
                'url': url,
                'category': '快讯',
                'sector': self._extract_sectors(title + content),
                'importance': '中',
                'tags': self._extract_tags(title + content)
            }

        except Exception as e:
            self.logger.debug(f"解析快讯项失败: {e}")
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

        # 定义板块关键词
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

        # 定义标签关键词
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
            '监管': ['监管', '处罚']
        }

        for tag, keywords in tag_keywords.items():
            if any(keyword in text_lower for keyword in keywords):
                tags.append(tag)

        return tags
