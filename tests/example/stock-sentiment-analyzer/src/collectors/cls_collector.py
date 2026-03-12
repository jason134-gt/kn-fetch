"""
财联社数据采集器
"""

import aiohttp
import logging
from typing import List, Dict, Any
from datetime import datetime
from bs4 import BeautifulSoup


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
                    if news:
                        news_list.append(news)

        except Exception as e:
            self.logger.error(f"采集财联社电报失败: {e}")

        return news_list

    def _parse_telegraph_item(self, item: Dict[str, Any]) -> Dict[str, Any]:
        """解析电报项

        Args:
            item: 电报数据

        Returns:
            资讯字典
        """
        try:
            title = item.get('brief', '')
            content = item.get('content', '') or title
            url = f"https://www.cls.cn/telegraph/{item.get('id', '')}"

            # 时间戳转换
            time_str = item.get('time', '')
            if time_str:
                try:
                    # 财联社时间格式: "2024-01-15 14:30:00"
                    timestamp = time_str
                except:
                    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            else:
                timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

            # 提取股票代码
            stock_codes = item.get('stock', [])
            if stock_codes:
                stock_info = stock_codes[0]
                stock_name = stock_info.get('name', '')
                stock_code = stock_info.get('code', '')
            else:
                stock_name = ''
                stock_code = ''

            return {
                'timestamp': timestamp,
                'source': '财联社',
                'title': title,
                'content': content,
                'url': url,
                'category': '电报',
                'sector': self._extract_sectors(title + content),
                'importance': '高',
                'tags': self._extract_tags(title + content),
                'stock_code': stock_code,
                'stock_name': stock_name
            }

        except Exception as e:
            self.logger.debug(f"解析电报项失败: {e}")
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
