"""
定时任务调度模块
"""

import asyncio
import logging
from typing import Callable, Dict, Any
from datetime import datetime


class TaskScheduler:
    """任务调度器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化调度器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        # 获取调度配置
        self.scheduler_config = config.get('scheduler', {})
        self.enabled = self.scheduler_config.get('enabled', False)
        self.cron_expression = self.scheduler_config.get('cron_expression', '0 */30 * * * *')
        self.timezone = self.scheduler_config.get('timezone', 'Asia/Shanghai')

        # 解析cron表达式
        self.interval_minutes = self._parse_cron_interval(self.cron_expression)

        self.running = False
        self.task = None

    def _parse_cron_interval(self, cron_expression: str) -> int:
        """解析cron表达式，获取间隔分钟数

        Args:
            cron_expression: cron表达式

        Returns:
            间隔分钟数
        """
        # 简化处理，只支持 */N 格式
        parts = cron_expression.split()
        if len(parts) >= 2:
            minute_part = parts[0]
            if minute_part.startswith('*/'):
                try:
                    return int(minute_part[2:])
                except ValueError:
                    pass
        return 30  # 默认30分钟

    async def start(self, task_func: Callable):
        """启动定时任务

        Args:
            task_func: 任务函数
        """
        if not self.enabled:
            self.logger.warning("定时任务未启用")
            return

        self.running = True
        self.logger.info(f"定时任务已启动，间隔: {self.interval_minutes}分钟")

        while self.running:
            try:
                self.logger.info(f"执行定时任务: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
                await task_func()
                self.logger.info(f"定时任务完成: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
            except Exception as e:
                self.logger.error(f"定时任务执行失败: {e}", exc_info=True)

            # 等待下一次执行
            await asyncio.sleep(self.interval_minutes * 60)

    def stop(self):
        """停止定时任务"""
        self.running = False
        self.logger.info("定时任务已停止")
