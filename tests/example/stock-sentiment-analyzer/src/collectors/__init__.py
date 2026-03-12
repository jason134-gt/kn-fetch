# 资讯采集模块
from .eastmoney_collector import EastMoneyCollector
from .cls_collector import ClsCollector
from .news_collector import NewsCollector
from .xueqiu_collector import XueqiuCollector

__all__ = [
    'EastMoneyCollector',
    'ClsCollector',
    'NewsCollector',
    'XueqiuCollector'
]
