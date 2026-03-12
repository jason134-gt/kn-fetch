"""
配置加载模块
"""

import yaml
import logging
from pathlib import Path
from typing import Dict, Any


class ConfigLoader:
    """配置加载器"""

    @staticmethod
    def load(config_path: str) -> Dict[str, Any]:
        """加载配置文件

        Args:
            config_path: 配置文件路径

        Returns:
            配置字典
        """
        logger = logging.getLogger(__name__)

        config_file = Path(config_path)

        if not config_file.exists():
            logger.warning(f"配置文件不存在: {config_path}，使用默认配置")
            return ConfigLoader._get_default_config()

        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)

            logger.info(f"配置文件加载成功: {config_path}")
            return config

        except Exception as e:
            logger.error(f"配置文件加载失败: {e}，使用默认配置")
            return ConfigLoader._get_default_config()

    @staticmethod
    def _get_default_config() -> Dict[str, Any]:
        """获取默认配置
        
        Returns:
            默认配置字典
        """
        return {
            'data_sources': {
                'official': [],
                'media': [],
                'platforms': []
            },
            'expert_weights': {
                'macro_economy': 0.25,
                'industry_research': 0.25,
                'technical_analysis': 0.20,
                'capital_flow': 0.20,
                'risk_control': 0.10
            },
            'collector': {
                'interval_minutes': 30,
                'batch_size': 10,
                'max_items_per_source': 20,
                'timeout_seconds': 30
            },
            'analyzer': {
                'min_confidence': 0.6,
                'min_impact_score': 0.3,
                'short_term_days': 5
            },
            'trader': {
                'min_volume': 100000000,
                'max_volume': 10000000000,
                'min_turnover': 2.0,
                'max_pe_ratio': 100,
                'min_market_cap': 5000000000
            },
            'reporter': {
                'output_dir': './data/reports',
                'output_format': 'markdown',
                'include_charts': True,
                'send_notification': False
            },
            'scheduler': {
                'enabled': False,
                'cron_expression': '0 */30 * * * *',
                'timezone': 'Asia/Shanghai'
            },
            'logging': {
                'level': 'INFO',
                'file': './logs/analyzer.log',
                'max_size_mb': 100,
                'backup_count': 10
            },
            'storage': {
                'type': 'json',
                'path': './data/storage.json'
            },
            'watched_sectors': [],
            'stock_pool': []
        }


def load_config(config_path: str = "config/config.yaml") -> Dict[str, Any]:
    """加载配置文件（兼容性函数）
    
    Args:
        config_path: 配置文件路径
        
    Returns:
        配置字典
    """
    return ConfigLoader.load(config_path)
