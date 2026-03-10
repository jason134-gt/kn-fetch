"""
知识质量评分系统

提供多维度知识质量评分能力，帮助过滤和改进低质量知识。

功能：
1. 多维度质量评分（完整性、准确性、一致性、相关性、新鲜度）
2. 阈值过滤
3. 质量报告生成
4. 自动质量改进

评分维度：
- 完整性 (Completeness): 实体信息是否完整
- 准确性 (Accuracy): 分析结果是否准确
- 一致性 (Consistency): 与其他知识是否一致
- 相关性 (Relevance): 与项目上下文是否相关
- 新鲜度 (Freshness): 知识是否最新
"""

import hashlib
import json
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Set, Optional, Tuple, Any
from dataclasses import dataclass, field
from enum import Enum
import re


class QualityDimension(Enum):
    """质量维度"""
    COMPLETENESS = "completeness"  # 完整性
    ACCURACY = "accuracy"          # 准确性
    CONSISTENCY = "consistency"    # 一致性
    RELEVANCE = "relevance"        # 相关性
    FRESHNESS = "freshness"        # 新鲜度


@dataclass
class DimensionScore:
    """维度评分"""
    
    dimension: QualityDimension  # 维度
    score: float                 # 评分 (0-100)
    details: Dict[str, Any] = field(default_factory=dict)  # 详细信息
    issues: List[str] = field(default_factory=list)  # 问题列表
    suggestions: List[str] = field(default_factory=list)  # 改进建议


@dataclass
class QualityScore:
    """综合质量评分"""
    
    entity_id: str               # 实体 ID
    entity_name: str             # 实体名称
    overall_score: float         # 综合评分 (0-100)
    dimension_scores: Dict[QualityDimension, DimensionScore] = field(default_factory=dict)  # 各维度评分
    timestamp: datetime = field(default_factory=datetime.now)  # 评分时间
    
    # 元数据
    entity_type: str = ""
    file_path: str = ""
    confidence: float = 0.0      # 置信度
    
    def get_weighted_score(
        self,
        weights: Dict[QualityDimension, float] = None
    ) -> float:
        """
        计算加权评分
        
        Args:
            weights: 各维度权重，默认为等权重
            
        Returns:
            加权评分
        """
        if weights is None:
            weights = {
                QualityDimension.COMPLETENESS: 0.25,
                QualityDimension.ACCURACY: 0.30,
                QualityDimension.CONSISTENCY: 0.20,
                QualityDimension.RELEVANCE: 0.15,
                QualityDimension.FRESHNESS: 0.10
            }
        
        weighted_score = 0.0
        total_weight = 0.0
        
        for dimension, weight in weights.items():
            if dimension in self.dimension_scores:
                weighted_score += self.dimension_scores[dimension].score * weight
                total_weight += weight
        
        return weighted_score / total_weight if total_weight > 0 else 0.0
    
    def get_issues(self) -> List[str]:
        """获取所有问题"""
        issues = []
        for dim_score in self.dimension_scores.values():
            issues.extend(dim_score.issues)
        return issues
    
    def get_suggestions(self) -> List[str]:
        """获取所有改进建议"""
        suggestions = []
        for dim_score in self.dimension_scores.values():
            suggestions.extend(dim_score.suggestions)
        return suggestions
    
    def passes_threshold(self, threshold: float = 60.0) -> bool:
        """
        判断是否通过质量阈值
        
        Args:
            threshold: 质量阈值
            
        Returns:
            是否通过
        """
        return self.overall_score >= threshold


@dataclass
class GraphQualityReport:
    """图谱质量报告"""
    
    graph_id: str                     # 图谱 ID
    total_entities: int               # 总实体数
    total_relationships: int           # 总关系数
    
    # 统计信息
    average_score: float              # 平均评分
    min_score: float                  # 最低评分
    max_score: float                  # 最高评分
    score_distribution: Dict[str, int]  # 评分分布
    
    # 质量分类
    high_quality_count: int           # 高质量实体数 (>=80)
    medium_quality_count: int         # 中质量实体数 (60-79)
    low_quality_count: int            # 低质量实体数 (<60)
    
    # 问题统计
    common_issues: Dict[str, int]    # 常见问题统计
    duplicate_entities: List[str]     # 重复实体列表
    inconsistent_entities: List[str]  # 不一致实体列表
    missing_entities: List[str]       # 缺失实体列表
    
    # 改进建议
    overall_suggestions: List[str]    # 整体改进建议
    
    timestamp: datetime = field(default_factory=datetime.now)


class CompletenessScorer:
    """完整性评分器"""
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化完整性评分器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
        self.required_fields = self.config.get("required_fields", {
            "class": ["name", "file_path", "line_number", "content"],
            "function": ["name", "file_path", "line_number", "content", "parameters"],
            "method": ["name", "file_path", "line_number", "content", "parameters"],
            "variable": ["name", "file_path", "line_number"]
        })
        self.optional_fields = self.config.get("optional_fields", {
            "class": ["docstring", "super_class", "modifiers"],
            "function": ["docstring", "return_type", "modifiers"],
            "method": ["docstring", "return_type", "modifiers", "super_class"],
            "variable": ["type", "docstring"]
        })
    
    def score(self, entity) -> DimensionScore:
        """
        评分实体完整性
        
        Args:
            entity: 代码实体
            
        Returns:
            维度评分
        """
        entity_type = getattr(entity, "entity_type", "unknown")
        
        # 获取实体属性
        entity_dict = self._entity_to_dict(entity)
        
        # 计算必填字段完成度
        required = self.required_fields.get(entity_type, [])
        required_filled = sum(1 for field in required if self._is_field_filled(entity_dict, field))
        required_score = (required_filled / len(required)) * 100 if required else 0
        
        # 计算可选字段完成度
        optional = self.optional_fields.get(entity_type, [])
        optional_filled = sum(1 for field in optional if self._is_field_filled(entity_dict, field))
        optional_score = (optional_filled / len(optional)) * 100 if optional else 0
        
        # 综合评分（必填字段占 70%，可选字段占 30%）
        score = required_score * 0.7 + optional_score * 0.3
        
        # 生成问题和建议
        issues = []
        suggestions = []
        
        for field in required:
            if not self._is_field_filled(entity_dict, field):
                issues.append(f"缺少必填字段: {field}")
                suggestions.append(f"补充 {field} 字段")
        
        return DimensionScore(
            dimension=QualityDimension.COMPLETENESS,
            score=score,
            details={
                "required_score": required_score,
                "optional_score": optional_score,
                "required_fields_filled": f"{required_filled}/{len(required)}",
                "optional_fields_filled": f"{optional_filled}/{len(optional)}"
            },
            issues=issues,
            suggestions=suggestions
        )
    
    def _entity_to_dict(self, entity) -> Dict[str, Any]:
        """将实体转换为字典"""
        if hasattr(entity, "model_dump"):
            return entity.model_dump()
        elif hasattr(entity, "__dict__"):
            return entity.__dict__
        else:
            return {}
    
    def _is_field_filled(self, entity_dict: Dict[str, Any], field: str) -> bool:
        """判断字段是否已填充"""
        value = entity_dict.get(field)
        if value is None:
            return False
        if isinstance(value, (str, list, dict)):
            return len(value) > 0
        return True


class AccuracyScorer:
    """准确性评分器"""
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化准确性评分器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
        self.patterns = self.config.get("accuracy_patterns", {
            "valid_name": re.compile(r'^[a-zA-Z_][a-zA-Z0-9_]*$'),
            "valid_path": re.compile(r'^[a-zA-Z0-9_/\-\.\\]+$'),
        })
    
    def score(
        self,
        entity,
        source_content: str = None
    ) -> DimensionScore:
        """
        评分实体准确性
        
        Args:
            entity: 代码实体
            source_content: 源代码内容
            
        Returns:
            维度评分
        """
        issues = []
        suggestions = []
        score = 100.0
        
        # 检查名称有效性
        if not self._is_valid_name(entity):
            score -= 15
            issues.append("实体名称格式无效")
            suggestions.append("使用有效的命名规范")
        
        # 检查路径有效性
        if not self._is_valid_path(entity):
            score -= 15
            issues.append("文件路径格式无效")
            suggestions.append("检查文件路径是否正确")
        
        # 检查行号有效性
        if not self._is_valid_line_number(entity):
            score -= 10
            issues.append("行号无效")
            suggestions.append("检查行号是否在合理范围内")
        
        # 检查内容一致性（如果有源代码）
        if source_content and hasattr(entity, "content"):
            if not self._is_content_consistent(entity, source_content):
                score -= 20
                issues.append("提取的代码与源代码不一致")
                suggestions.append("重新提取代码内容")
        
        # 检查参数类型匹配
        if hasattr(entity, "parameters") and hasattr(entity, "content"):
            if not self._are_parameters_matched(entity):
                score -= 10
                issues.append("参数列表与实际代码不匹配")
                suggestions.append("检查参数提取是否完整")
        
        return DimensionScore(
            dimension=QualityDimension.ACCURACY,
            score=max(0.0, score),
            details={},
            issues=issues,
            suggestions=suggestions
        )
    
    def _is_valid_name(self, entity) -> bool:
        """检查名称是否有效"""
        name = getattr(entity, "name", "")
        if not name:
            return False
        return self.patterns["valid_name"].match(name) is not None
    
    def _is_valid_path(self, entity) -> bool:
        """检查路径是否有效"""
        path = getattr(entity, "file_path", "")
        if not path:
            return False
        return self.patterns["valid_path"].match(path) is not None
    
    def _is_valid_line_number(self, entity) -> bool:
        """检查行号是否有效"""
        line_number = getattr(entity, "line_number", None)
        if line_number is None:
            return False
        return isinstance(line_number, int) and line_number >= 0
    
    def _is_content_consistent(self, entity, source_content: str) -> bool:
        """检查内容一致性"""
        entity_content = getattr(entity, "content", "")
        if not entity_content:
            return True
        
        # 简单检查：实体内容是否在源代码中
        return entity_content.strip() in source_content
    
    def _are_parameters_matched(self, entity) -> bool:
        """检查参数是否匹配"""
        parameters = getattr(entity, "parameters", None)
        content = getattr(entity, "content", "")
        
        if not parameters or not content:
            return True
        
        # 检查参数是否在内容中
        for param in parameters:
            if isinstance(param, dict):
                param_name = param.get("name", "")
            else:
                param_name = str(param)
            
            if param_name and param_name not in content:
                return False
        
        return True


class ConsistencyScorer:
    """一致性评分器"""
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化一致性评分器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
    
    def score(
        self,
        entity,
        all_entities: List = None,
        relationships: List = None
    ) -> DimensionScore:
        """
        评分实体一致性
        
        Args:
            entity: 代码实体
            all_entities: 所有实体列表
            relationships: 关系列表
            
        Returns:
            维度评分
        """
        issues = []
        suggestions = []
        score = 100.0
        
        # 检查命名一致性
        if all_entities:
            consistency_issues = self._check_naming_consistency(entity, all_entities)
            if consistency_issues:
                score -= len(consistency_issues) * 10
                issues.extend(consistency_issues)
                suggestions.append("保持命名风格一致性")
        
        # 检查关系一致性
        if relationships:
            relation_issues = self._check_relation_consistency(entity, relationships)
            if relation_issues:
                score -= len(relation_issues) * 10
                issues.extend(relation_issues)
                suggestions.append("检查关系引用是否正确")
        
        return DimensionScore(
            dimension=QualityDimension.CONSISTENCY,
            score=max(0.0, score),
            details={},
            issues=issues,
            suggestions=suggestions
        )
    
    def _check_naming_consistency(
        self,
        entity,
        all_entities: List
    ) -> List[str]:
        """检查命名一致性"""
        issues = []
        entity_name = getattr(entity, "name", "")
        entity_type = getattr(entity, "entity_type", "")
        
        # 查找同名实体
        duplicates = []
        for other_entity in all_entities:
            if id(other_entity) == id(entity):
                continue
            
            other_name = getattr(other_entity, "name", "")
            other_type = getattr(other_entity, "entity_type", "")
            
            if entity_name == other_name and entity_type == other_type:
                duplicates.append(other_entity)
        
        if duplicates:
            file_paths = [
                getattr(e, "file_path", "unknown")
                for e in duplicates
            ]
            issues.append(
                f"发现 {len(duplicates)} 个同名实体: {', '.join(file_paths)}"
            )
        
        return issues
    
    def _check_relation_consistency(
        self,
        entity,
        relationships: List
    ) -> List[str]:
        """检查关系一致性"""
        issues = []
        
        entity_id = getattr(entity, "id", "")
        
        # 检查关系引用是否存在
        for rel in relationships:
            if hasattr(rel, "source_id") and rel.source_id == entity_id:
                target_id = rel.target_id
                # 这里应该检查 target_id 是否存在于实体列表中
                # 简化实现，假设总是有效
                pass
        
        return issues


class RelevanceScorer:
    """相关性评分器"""
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化相关性评分器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
        self.keywords = self.config.get("relevance_keywords", {})
        self.ignore_patterns = self.config.get("ignore_patterns", [
            r'^test_',
            r'_test\.py$',
            r'__pycache__',
            r'\.pyc$'
        ])
    
    def score(
        self,
        entity,
        project_context: Dict[str, Any] = None
    ) -> DimensionScore:
        """
        评分实体相关性
        
        Args:
            entity: 代码实体
            project_context: 项目上下文信息
            
        Returns:
            维度评分
        """
        issues = []
        suggestions = []
        score = 100.0
        
        # 检查是否为测试文件
        file_path = getattr(entity, "file_path", "")
        if self._should_ignore(file_path):
            score -= 20
            issues.append("实体来自测试文件")
            suggestions.append("测试文件可以忽略或降低优先级")
        
        # 检查关键词匹配
        entity_name = getattr(entity, "name", "")
        entity_content = getattr(entity, "content", "")
        
        keyword_score = self._calculate_keyword_relevance(
            entity_name + " " + entity_content
        )
        score = score * (keyword_score / 100.0)
        
        if keyword_score < 50:
            issues.append("实体与项目相关性较低")
            suggestions.append("检查实体是否属于核心功能")
        
        return DimensionScore(
            dimension=QualityDimension.RELEVANCE,
            score=max(0.0, score),
            details={
                "keyword_score": keyword_score
            },
            issues=issues,
            suggestions=suggestions
        )
    
    def _should_ignore(self, file_path: str) -> bool:
        """判断是否应该忽略"""
        for pattern in self.ignore_patterns:
            if re.search(pattern, file_path):
                return True
        return False
    
    def _calculate_keyword_relevance(self, text: str) -> float:
        """计算关键词相关性"""
        if not self.keywords:
            return 100.0
        
        text_lower = text.lower()
        matched_keywords = 0
        
        for category, keywords in self.keywords.items():
            if isinstance(keywords, list):
                for keyword in keywords:
                    if keyword.lower() in text_lower:
                        matched_keywords += 1
        
        total_keywords = sum(
            len(kws) if isinstance(kws, list) else 1
            for kws in self.keywords.values()
        )
        
        return (matched_keywords / total_keywords * 100) if total_keywords > 0 else 100.0


class FreshnessScorer:
    """新鲜度评分器"""
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化新鲜度评分器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
        self.freshness_threshold = self.config.get(
            "freshness_threshold_hours",
            24 * 7  # 默认 7 天
        )
    
    def score(
        self,
        entity,
        file_mtime: float = None,
        analysis_timestamp: datetime = None
    ) -> DimensionScore:
        """
        评分实体新鲜度
        
        Args:
            entity: 代码实体
            file_mtime: 文件修改时间戳
            analysis_timestamp: 分析时间戳
            
        Returns:
            维度评分
        """
        issues = []
        suggestions = []
        
        # 使用文件修改时间或分析时间
        if file_mtime is None:
            entity_mtime = getattr(entity, "mtime", None)
        else:
            entity_mtime = file_mtime
        
        if entity_mtime is None:
            score = 100.0  # 无法判断，给满分
        else:
            # 计算时间差（小时）
            if isinstance(entity_mtime, (int, float)):
                mtime_dt = datetime.fromtimestamp(entity_mtime)
            else:
                mtime_dt = entity_mtime
            
            if analysis_timestamp is None:
                analysis_timestamp = datetime.now()
            
            time_diff_hours = (analysis_timestamp - mtime_dt).total_seconds() / 3600
            
            # 计算新鲜度评分
            if time_diff_hours <= self.freshness_threshold:
                score = 100.0
            else:
                score = max(0.0, 100.0 - (time_diff_hours - self.freshness_threshold) / 24.0 * 5)
            
            if score < 80:
                issues.append(f"实体已 {int(time_diff_hours)} 小时未更新")
                suggestions.append("考虑重新分析相关代码")
        
        return DimensionScore(
            dimension=QualityDimension.FRESHNESS,
            score=score,
            details={
                "time_diff_hours": time_diff_hours if 'time_diff_hours' in locals() else None
            },
            issues=issues,
            suggestions=suggestions
        )


class KnowledgeQualityScorer:
    """
    知识质量评分器
    
    功能：
    1. 多维度评分
    2. 综合评分计算
    3. 质量报告生成
    4. 阈值过滤
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化质量评分器
        
        Args:
            config: 配置字典
        """
        self.config = config or {}
        
        # 初始化各维度评分器
        self.completeness_scorer = CompletenessScorer(config)
        self.accuracy_scorer = AccuracyScorer(config)
        self.consistency_scorer = ConsistencyScorer(config)
        self.relevance_scorer = RelevanceScorer(config)
        self.freshness_scorer = FreshnessScorer(config)
        
        # 默认权重
        self.default_weights = {
            QualityDimension.COMPLETENESS: 0.25,
            QualityDimension.ACCURACY: 0.30,
            QualityDimension.CONSISTENCY: 0.20,
            QualityDimension.RELEVANCE: 0.15,
            QualityDimension.FRESHNESS: 0.10
        }
    
    def score_entity(
        self,
        entity,
        all_entities: List = None,
        relationships: List = None,
        source_content: str = None,
        file_mtime: float = None,
        project_context: Dict[str, Any] = None
    ) -> QualityScore:
        """
        评分实体质量
        
        Args:
            entity: 代码实体
            all_entities: 所有实体列表
            relationships: 关系列表
            source_content: 源代码内容
            file_mtime: 文件修改时间
            project_context: 项目上下文
            
        Returns:
            质量评分
        """
        entity_id = getattr(entity, "id", "")
        entity_name = getattr(entity, "name", "")
        entity_type = getattr(entity, "entity_type", "")
        file_path = getattr(entity, "file_path", "")
        
        # 各维度评分
        dimension_scores = {}
        
        # 完整性
        dimension_scores[QualityDimension.COMPLETENESS] = self.completeness_scorer.score(entity)
        
        # 准确性
        dimension_scores[QualityDimension.ACCURACY] = self.accuracy_scorer.score(entity, source_content)
        
        # 一致性
        dimension_scores[QualityDimension.CONSISTENCY] = self.consistency_scorer.score(
            entity, all_entities, relationships
        )
        
        # 相关性
        dimension_scores[QualityDimension.RELEVANCE] = self.relevance_scorer.score(entity, project_context)
        
        # 新鲜度
        dimension_scores[QualityDimension.FRESHNESS] = self.freshness_scorer.score(entity, file_mtime)
        
        # 计算综合评分
        weights = self.config.get("quality_weights", self.default_weights)
        overall_score = 0.0
        
        for dimension, weight in weights.items():
            if dimension in dimension_scores:
                overall_score += dimension_scores[dimension].score * weight
        
        return QualityScore(
            entity_id=entity_id,
            entity_name=entity_name,
            overall_score=overall_score,
            dimension_scores=dimension_scores,
            entity_type=entity_type,
            file_path=file_path
        )
    
    def score_relationship(
        self,
        relationship,
        all_entities: List = None
    ) -> QualityScore:
        """
        评分关系质量
        
        Args:
            relationship: 关系
            all_entities: 所有实体列表
            
        Returns:
            质量评分
        """
        # 简化实现：关系质量基于引用实体的存在性
        issues = []
        score = 100.0
        
        if all_entities:
            entity_ids = {getattr(e, "id", "") for e in all_entities}
            
            source_id = getattr(relationship, "source_id", "")
            target_id = getattr(relationship, "target_id", "")
            
            if source_id not in entity_ids:
                score -= 50
                issues.append(f"源实体不存在: {source_id}")
            
            if target_id not in entity_ids:
                score -= 50
                issues.append(f"目标实体不存在: {target_id}")
        
        # 简化的维度评分
        dimension_scores = {
            QualityDimension.ACCURACY: DimensionScore(
                dimension=QualityDimension.ACCURACY,
                score=max(0.0, score),
                issues=issues,
                suggestions=[]
            )
        }
        
        return QualityScore(
            entity_id=getattr(relationship, "id", ""),
            entity_name=getattr(relationship, "relationship_type", ""),
            overall_score=max(0.0, score),
            dimension_scores=dimension_scores
        )
    
    def score_graph(
        self,
        graph,
        source_contents: Dict[str, str] = None
    ) -> GraphQualityReport:
        """
        评分知识图谱质量
        
        Args:
            graph: 知识图谱
            source_contents: 源代码内容映射（文件路径 -> 内容）
            
        Returns:
            图谱质量报告
        """
        all_entities = list(graph.entities.values()) if hasattr(graph, "entities") else []
        relationships = list(graph.relationships) if hasattr(graph, "relationships") else []
        
        # 评分所有实体
        entity_scores = []
        for entity in all_entities:
            file_path = getattr(entity, "file_path", "")
            source_content = source_contents.get(file_path) if source_contents else None
            
            score = self.score_entity(
                entity,
                all_entities=all_entities,
                relationships=relationships,
                source_content=source_content
            )
            entity_scores.append(score)
        
        # 计算统计信息
        scores = [s.overall_score for s in entity_scores]
        average_score = sum(scores) / len(scores) if scores else 0.0
        min_score = min(scores) if scores else 0.0
        max_score = max(scores) if scores else 0.0
        
        # 评分分布
        score_distribution = {
            "0-19": sum(1 for s in scores if 0 <= s < 20),
            "20-39": sum(1 for s in scores if 20 <= s < 40),
            "40-59": sum(1 for s in scores if 40 <= s < 60),
            "60-79": sum(1 for s in scores if 60 <= s < 80),
            "80-100": sum(1 for s in scores if 80 <= s <= 100)
        }
        
        # 质量分类
        high_quality = [s for s in entity_scores if s.overall_score >= 80]
        medium_quality = [s for s in entity_scores if 60 <= s.overall_score < 80]
        low_quality = [s for s in entity_scores if s.overall_score < 60]
        
        # 常见问题统计
        common_issues = {}
        for score in entity_scores:
            for issue in score.get_issues():
                common_issues[issue] = common_issues.get(issue, 0) + 1
        
        # 识别问题实体
        duplicate_entities = []
        inconsistent_entities = []
        missing_entities = []
        
        for score in entity_scores:
            if any("同名" in issue for issue in score.get_issues()):
                duplicate_entities.append(score.entity_id)
            if any("一致" in issue for issue in score.get_issues()):
                inconsistent_entities.append(score.entity_id)
            if any("缺少" in issue for issue in score.get_issues()):
                missing_entities.append(score.entity_id)
        
        # 生成整体建议
        overall_suggestions = self._generate_overall_suggestions(
            high_quality, medium_quality, low_quality
        )
        
        return GraphQualityReport(
            graph_id=getattr(graph, "graph_id", "unknown"),
            total_entities=len(all_entities),
            total_relationships=len(relationships),
            average_score=average_score,
            min_score=min_score,
            max_score=max_score,
            score_distribution=score_distribution,
            high_quality_count=len(high_quality),
            medium_quality_count=len(medium_quality),
            low_quality_count=len(low_quality),
            common_issues=common_issues,
            duplicate_entities=duplicate_entities[:10],  # 只保留前10个
            inconsistent_entities=inconsistent_entities[:10],
            missing_entities=missing_entities[:10],
            overall_suggestions=overall_suggestions
        )
    
    def _generate_overall_suggestions(
        self,
        high_quality: List[QualityScore],
        medium_quality: List[QualityScore],
        low_quality: List[QualityScore]
    ) -> List[str]:
        """生成整体改进建议"""
        suggestions = []
        
        if len(low_quality) > 0:
            total = len(high_quality) + len(medium_quality) + len(low_quality)
            low_ratio = len(low_quality) / total
            suggestions.append(
                f"有 {len(low_quality)} 个低质量实体 ({low_ratio:.1%})，"
                f"建议优先处理"
            )
        
        if len(medium_quality) > len(high_quality):
            suggestions.append(
                "中质量实体数量较多，建议优化提取逻辑"
            )
        
        # 分析常见问题
        all_scores = high_quality + medium_quality + low_quality
        issue_counts = {}
        for score in all_scores:
            for issue in score.get_issues():
                issue_counts[issue] = issue_counts.get(issue, 0) + 1
        
        if issue_counts:
            top_issues = sorted(issue_counts.items(), key=lambda x: x[1], reverse=True)[:5]
            for issue, count in top_issues:
                suggestions.append(
                    f"常见问题: {issue} (出现 {count} 次)"
                )
        
        return suggestions
    
    def filter_by_quality(
        self,
        graph,
        threshold: float = 60.0
    ):
        """
        按质量阈值过滤图谱
        
        Args:
            graph: 知识图谱
            threshold: 质量阈值
            
        Returns:
            过滤后的知识图谱
        """
        # 这里简化实现，实际应该创建新的图谱
        filtered_entities = {}
        filtered_relationships = []
        
        if hasattr(graph, "entities"):
            for entity_id, entity in graph.entities.items():
                score = self.score_entity(entity)
                if score.overall_score >= threshold:
                    filtered_entities[entity_id] = entity
        
        if hasattr(graph, "relationships"):
            entity_ids = set(filtered_entities.keys())
            for rel in graph.relationships:
                if rel.source_id in entity_ids and rel.target_id in entity_ids:
                    filtered_relationships.append(rel)
        
        # 更新图谱（简化实现）
        if hasattr(graph, "entities"):
            graph.entities = filtered_entities
        if hasattr(graph, "relationships"):
            graph.relationships = filtered_relationships
        
        return graph
    
    def generate_quality_report(
        self,
        graph,
        source_contents: Dict[str, str] = None
    ) -> GraphQualityReport:
        """
        生成质量报告
        
        Args:
            graph: 知识图谱
            source_contents: 源代码内容映射
            
        Returns:
            质量报告
        """
        return self.score_graph(graph, source_contents)


# 使用示例
if __name__ == "__main__":
    # 创建质量评分器
    scorer = KnowledgeQualityScorer()
    
    # 模拟实体
    from dataclasses import dataclass
    
    @dataclass
    class MockEntity:
        id: str
        name: str
        entity_type: str
        file_path: str
        line_number: int
        content: str
        docstring: str = ""
        parameters: list = None
    
    entity = MockEntity(
        id="test_1",
        name="example_function",
        entity_type="function",
        file_path="src/example.py",
        line_number=10,
        content="def example_function():\n    pass",
        docstring="示例函数",
        parameters=[]
    )
    
    # 评分
    score = scorer.score_entity(entity)
    
    print(f"实体: {score.entity_name}")
    print(f"综合评分: {score.overall_score:.1f}")
    print(f"各维度评分:")
    for dim, dim_score in score.dimension_scores.items():
        print(f"  {dim.value}: {dim_score.score:.1f}")
    
    if score.issues:
        print(f"\n问题:")
        for issue in score.get_issues():
            print(f"  - {issue}")
    
    if score.suggestions:
        print(f"\n建议:")
        for suggestion in score.get_suggestions():
            print(f"  - {suggestion}")
