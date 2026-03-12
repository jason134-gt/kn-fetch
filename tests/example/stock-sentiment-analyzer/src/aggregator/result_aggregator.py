"""
结果汇总模块
"""

import logging
from typing import Dict, Any, List


class ResultAggregator:
    """结果汇总器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化汇总器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        # 获取专家权重配置
        self.expert_weights = config.get('expert_weights', {})

        # 影响等级映射
        self.impact_level_map = {
            '无影响': 0,
            '微影响': 1,
            '中影响': 2,
            '强影响': 3
        }

        # 影响方向映射
        self.impact_direction_map = {
            '利空': -1,
            '中性': 0,
            '利好': 1
        }

        # 影响时长映射
        self.impact_duration_map = {
            '短期': 1,
            '中期': 2,
            '长期': 3
        }

        # 反向映射
        self.reverse_level_map = {v: k for k, v in self.impact_level_map.items()}
        self.reverse_direction_map = {v: k for k, v in self.impact_direction_map.items()}
        self.reverse_duration_map = {v: k for k, v in self.impact_duration_map.items()}

    async def aggregate(self, news: Dict[str, Any], expert_results: Dict[str, Any]) -> Dict[str, Any]:
        """汇总专家评估结果

        Args:
            news: 资讯信息
            expert_results: 专家评估结果

        Returns:
            汇总结果
        """
        self.logger.info(f"开始汇总: {news.get('title', '')}")

        evaluations = expert_results.get('evaluations', {})

        # 计算加权平均
        aggregated = self._calculate_weighted_average(evaluations)

        # 识别分歧
        divergence = self._identify_divergence(evaluations)

        # 提炼共识
        consensus = self._extract_consensus(evaluations, aggregated)

        # 构建结果
        result = {
            'news_id': expert_results.get('news_id'),
            'news_title': news.get('title', ''),
            'aggregated_result': aggregated,
            'consensus': consensus,
            'divergence': divergence,
            'expert_details': evaluations
        }

        self.logger.info(f"汇总完成: {news.get('title', '')}")
        return result

    def _calculate_weighted_average(self, evaluations: Dict[str, Any]) -> Dict[str, Any]:
        """计算加权平均

        Args:
            evaluations: 专家评估结果

        Returns:
            加权平均结果
        """
        total_weight = 0
        weighted_level = 0
        weighted_direction = 0
        weighted_duration = 0
        weighted_confidence = 0

        all_sectors = set()

        for expert_id, evaluation in evaluations.items():
            weight = self.expert_weights.get(expert_id, 0.2)

            # 影响等级
            level = self.impact_level_map.get(evaluation.get('impact_level', '微影响'), 1)
            weighted_level += level * weight

            # 影响方向
            direction = self.impact_direction_map.get(evaluation.get('impact_direction', '中性'), 0)
            weighted_direction += direction * weight

            # 影响时长
            duration = self.impact_duration_map.get(evaluation.get('impact_duration', '短期'), 1)
            weighted_duration += duration * weight

            # 置信度
            confidence = evaluation.get('confidence', 0.5)
            weighted_confidence += confidence * weight

            total_weight += weight

            # 收集所有受影响板块
            sectors = evaluation.get('affected_sectors', [])
            all_sectors.update(sectors)

        # 归一化
        if total_weight > 0:
            weighted_level /= total_weight
            weighted_direction /= total_weight
            weighted_duration /= total_weight
            weighted_confidence /= total_weight

        # 转换回字符串
        impact_level = self._map_to_level(weighted_level)
        impact_direction = self._map_to_direction(weighted_direction)
        impact_duration = self._map_to_duration(weighted_duration)

        return {
            'impact_level': impact_level,
            'impact_level_score': round(weighted_level, 2),
            'impact_direction': impact_direction,
            'impact_direction_score': round(weighted_direction, 2),
            'impact_duration': impact_duration,
            'impact_duration_score': round(weighted_duration, 2),
            'confidence': round(weighted_confidence, 2),
            'affected_sectors': list(all_sectors)
        }

    def _map_to_level(self, score: float) -> str:
        """将分数映射到影响等级

        Args:
            score: 分数

        Returns:
            影响等级
        """
        if score < 0.5:
            return '无影响'
        elif score < 1.5:
            return '微影响'
        elif score < 2.5:
            return '中影响'
        else:
            return '强影响'

    def _map_to_direction(self, score: float) -> str:
        """将分数映射到影响方向

        Args:
            score: 分数

        Returns:
            影响方向
        """
        if score < -0.3:
            return '利空'
        elif score > 0.3:
            return '利好'
        else:
            return '中性'

    def _map_to_duration(self, score: float) -> str:
        """将分数映射到影响时长

        Args:
            score: 分数

        Returns:
            影响时长
        """
        if score < 1.5:
            return '短期'
        elif score < 2.5:
            return '中期'
        else:
            return '长期'

    def _identify_divergence(self, evaluations: Dict[str, Any]) -> Dict[str, Any]:
        """识别专家分歧

        Args:
            evaluations: 专家评估结果

        Returns:
            分歧信息
        """
        directions = []
        levels = []

        for evaluation in evaluations.values():
            directions.append(evaluation.get('impact_direction', '中性'))
            levels.append(self.impact_level_map.get(evaluation.get('impact_level', '微影响'), 1))

        # 检查方向分歧
        direction_set = set(directions)
        has_direction_divergence = len(direction_set) >= 2

        # 检查等级分歧
        level_diff = max(levels) - min(levels)
        has_level_divergence = level_diff >= 2

        # 判断分歧类型
        divergence_type = None
        divergence_details = []

        if has_direction_divergence and has_level_divergence:
            divergence_type = '严重分歧'
            divergence_details.append(f"影响方向不一致: {', '.join(direction_set)}")
            divergence_details.append(f"影响等级差异较大: {level_diff}")
        elif has_direction_divergence:
            divergence_type = '轻微分歧'
            divergence_details.append(f"影响方向不一致: {', '.join(direction_set)}")
        elif has_level_divergence:
            divergence_type = '轻微分歧'
            divergence_details.append(f"影响等级差异: {level_diff}")

        return {
            'has_divergence': divergence_type is not None,
            'divergence_type': divergence_type,
            'divergence_details': divergence_details
        }

    def _extract_consensus(self, evaluations: Dict[str, Any], aggregated: Dict[str, Any]) -> Dict[str, Any]:
        """提炼共识

        Args:
            evaluations: 专家评估结果
            aggregated: 汇总结果

        Returns:
            共识信息
        """
        # 找出支持多数派意见的专家
        main_direction = aggregated.get('impact_direction', '中性')
        supporting_experts = []
        opposing_experts = []

        for expert_id, evaluation in evaluations.items():
            if evaluation.get('impact_direction') == main_direction:
                supporting_experts.append(expert_id)
            else:
                opposing_experts.append(expert_id)

        # 生成主要结论
        impact_level = aggregated.get('impact_level', '微影响')
        impact_direction = aggregated.get('impact_direction', '中性')
        impact_duration = aggregated.get('impact_duration', '短期')

        main_conclusion = f"{impact_direction}，{impact_level}，{impact_duration}影响"

        return {
            'main_conclusion': main_conclusion,
            'supporting_experts': supporting_experts,
            'opposing_experts': opposing_experts
        }
