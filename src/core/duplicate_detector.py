"""
重复代码检测Agent
实现：基于Token序列的相似度匹配、代码克隆检测
"""
import re
import logging
from typing import List, Dict, Any, Optional, Tuple, Set
from collections import defaultdict
from dataclasses import dataclass, field
import hashlib

from ..gitnexus.models import CodeEntity, EntityType

logger = logging.getLogger(__name__)


@dataclass
class DuplicatePair:
    """重复代码对"""
    entity1_id: str
    entity1_name: str
    entity1_file: str
    entity1_lines: Tuple[int, int]
    
    entity2_id: str
    entity2_name: str
    entity2_file: str
    entity2_lines: Tuple[int, int]
    
    similarity: float  # 相似度 0-1
    duplicate_type: str  # exact, near, structural
    duplicate_lines: int  # 重复行数
    suggestion: str


@dataclass
class DuplicateReport:
    """重复代码报告"""
    total_entities: int
    duplicate_pairs: List[DuplicatePair]
    duplicate_groups: List[Dict[str, Any]]  # 多个重复实体组成的组
    statistics: Dict[str, Any]
    recommendations: List[str]


class DuplicateDetector:
    """重复代码检测器"""
    
    def __init__(self, config: Dict[str, Any] = None):
        self.config = config or {}
        
        # 检测配置
        self.min_lines = config.get("min_lines", 5) if config else 5  # 最小重复行数
        self.similarity_threshold = config.get("similarity_threshold", 0.7) if config else 0.7
        self.min_tokens = config.get("min_tokens", 20) if config else 20  # 最小token数
        
        # 忽略的模式
        self.ignore_patterns = [
            r'^\s*$',  # 空行
            r'^\s*#',  # 注释
            r'^\s*//',  # C风格注释
            r'^\s*/\*',  # 块注释开始
            r'^\s*\*/',  # 块注释结束
            r'^\s*import\s',  # import语句
            r'^\s*from\s+\w+\s+import',  # from import
        ]
    
    def detect(self, entities: List[CodeEntity]) -> DuplicateReport:
        """检测重复代码"""
        # 过滤可检测实体
        detectable_entities = self._filter_detectable_entities(entities)
        
        # 生成代码指纹
        fingerprints = self._generate_fingerprints(detectable_entities)
        
        # 检测精确重复
        exact_duplicates = self._detect_exact_duplicates(fingerprints)
        
        # 检测近似重复
        near_duplicates = self._detect_near_duplicates(detectable_entities, fingerprints)
        
        # 合并结果
        all_duplicates = exact_duplicates + near_duplicates
        
        # 去重（同一对实体只保留最高相似度）
        unique_duplicates = self._deduplicate_pairs(all_duplicates)
        
        # 构建重复组
        duplicate_groups = self._build_duplicate_groups(unique_duplicates)
        
        # 生成统计
        statistics = self._generate_statistics(detectable_entities, unique_duplicates)
        
        # 生成建议
        recommendations = self._generate_recommendations(unique_duplicates, statistics)
        
        return DuplicateReport(
            total_entities=len(detectable_entities),
            duplicate_pairs=unique_duplicates,
            duplicate_groups=duplicate_groups,
            statistics=statistics,
            recommendations=recommendations
        )
    
    def _filter_detectable_entities(self, entities: List[CodeEntity]) -> List[CodeEntity]:
        """过滤可检测的实体"""
        detectable = []
        
        for entity in entities:
            # 只检测函数/方法/类
            if entity.entity_type not in [EntityType.FUNCTION, EntityType.METHOD, EntityType.CLASS]:
                continue
            
            # 必须有内容
            if not entity.content:
                continue
            
            # 必须达到最小行数
            if entity.lines_of_code < self.min_lines:
                continue
            
            detectable.append(entity)
        
        return detectable
    
    def _generate_fingerprints(self, entities: List[CodeEntity]) -> Dict[str, Dict[str, Any]]:
        """生成代码指纹"""
        fingerprints = {}
        
        for entity in entities:
            # 标准化代码
            normalized = self._normalize_code(entity.content)
            
            # 生成多种指纹
            fingerprints[entity.id] = {
                "entity": entity,
                "normalized_code": normalized,
                "hash": self._compute_hash(normalized),
                "tokens": self._tokenize(normalized),
                "ngrams": self._generate_ngrams(normalized, n=5),
                "line_hashes": self._compute_line_hashes(normalized),
                "ast_signature": self._extract_ast_signature(entity)
            }
        
        return fingerprints
    
    def _normalize_code(self, code: str) -> str:
        """标准化代码"""
        lines = code.split('\n')
        normalized_lines = []
        
        for line in lines:
            # 跳过忽略的模式
            skip = False
            for pattern in self.ignore_patterns:
                if re.match(pattern, line):
                    skip = True
                    break
            
            if skip:
                continue
            
            # 标准化空白
            normalized = line.strip()
            
            # 替换字符串字面量为占位符
            normalized = re.sub(r'"[^"]*"', '"STRING"', normalized)
            normalized = re.sub(r"'[^']*'", "'STRING'", normalized)
            normalized = re.sub(r'`[^`]*`', '`STRING`', normalized)
            
            # 替换数字为占位符
            normalized = re.sub(r'\b\d+\b', 'NUM', normalized)
            
            # 标准化空白
            normalized = re.sub(r'\s+', ' ', normalized)
            
            if normalized:
                normalized_lines.append(normalized)
        
        return '\n'.join(normalized_lines)
    
    def _tokenize(self, code: str) -> List[str]:
        """代码分词"""
        # 简单分词：按空白和符号分割
        tokens = re.findall(r'\w+|[^\w\s]', code)
        return [t for t in tokens if t.strip()]
    
    def _compute_hash(self, code: str) -> str:
        """计算代码哈希"""
        return hashlib.md5(code.encode()).hexdigest()
    
    def _generate_ngrams(self, code: str, n: int = 5) -> Set[str]:
        """生成N-gram集合"""
        tokens = self._tokenize(code)
        ngrams = set()
        
        for i in range(len(tokens) - n + 1):
            ngram = ' '.join(tokens[i:i+n])
            ngrams.add(ngram)
        
        return ngrams
    
    def _compute_line_hashes(self, code: str) -> List[str]:
        """计算每行哈希"""
        lines = code.split('\n')
        return [hashlib.md5(line.encode()).hexdigest()[:8] for line in lines if line.strip()]
    
    def _extract_ast_signature(self, entity: CodeEntity) -> str:
        """提取AST签名"""
        parts = []
        
        # 类型
        parts.append(entity.entity_type.value)
        
        # 参数数量
        if entity.parameters:
            parts.append(f"params:{len(entity.parameters)}")
        
        # 返回类型
        if entity.return_type:
            parts.append(f"returns:{entity.return_type}")
        
        # 装饰器
        if entity.decorators:
            parts.append(f"decorators:{len(entity.decorators)}")
        
        return '|'.join(parts)
    
    def _detect_exact_duplicates(self, fingerprints: Dict[str, Dict]) -> List[DuplicatePair]:
        """检测精确重复"""
        duplicates = []
        
        # 按哈希分组
        hash_groups = defaultdict(list)
        for entity_id, fp in fingerprints.items():
            hash_groups[fp["hash"]].append(entity_id)
        
        # 只保留有多个实体的组
        for hash_val, entity_ids in hash_groups.items():
            if len(entity_ids) < 2:
                continue
            
            # 生成重复对
            for i in range(len(entity_ids)):
                for j in range(i + 1, len(entity_ids)):
                    fp1 = fingerprints[entity_ids[i]]
                    fp2 = fingerprints[entity_ids[j]]
                    
                    duplicates.append(DuplicatePair(
                        entity1_id=entity_ids[i],
                        entity1_name=fp1["entity"].name,
                        entity1_file=fp1["entity"].file_path,
                        entity1_lines=(fp1["entity"].start_line, fp1["entity"].end_line),
                        
                        entity2_id=entity_ids[j],
                        entity2_name=fp2["entity"].name,
                        entity2_file=fp2["entity"].file_path,
                        entity2_lines=(fp2["entity"].start_line, fp2["entity"].end_line),
                        
                        similarity=1.0,
                        duplicate_type="exact",
                        duplicate_lines=fp1["entity"].lines_of_code,
                        suggestion="完全相同的代码，建议提取为公共函数"
                    ))
        
        return duplicates
    
    def _detect_near_duplicates(
        self, 
        entities: List[CodeEntity],
        fingerprints: Dict[str, Dict]
    ) -> List[DuplicatePair]:
        """检测近似重复"""
        duplicates = []
        
        # 计算相似度矩阵
        entity_list = list(fingerprints.values())
        
        for i in range(len(entity_list)):
            for j in range(i + 1, len(entity_list)):
                fp1 = entity_list[i]
                fp2 = entity_list[j]
                
                # 跳过精确重复
                if fp1["hash"] == fp2["hash"]:
                    continue
                
                # 计算相似度
                similarity = self._compute_similarity(fp1, fp2)
                
                if similarity >= self.similarity_threshold:
                    # 计算重复行数
                    duplicate_lines = self._count_duplicate_lines(
                        fp1["line_hashes"], 
                        fp2["line_hashes"]
                    )
                    
                    if duplicate_lines >= self.min_lines:
                        dup_type = "structural" if similarity < 0.85 else "near"
                        
                        duplicates.append(DuplicatePair(
                            entity1_id=fp1["entity"].id,
                            entity1_name=fp1["entity"].name,
                            entity1_file=fp1["entity"].file_path,
                            entity1_lines=(fp1["entity"].start_line, fp1["entity"].end_line),
                            
                            entity2_id=fp2["entity"].id,
                            entity2_name=fp2["entity"].name,
                            entity2_file=fp2["entity"].file_path,
                            entity2_lines=(fp2["entity"].start_line, fp2["entity"].end_line),
                            
                            similarity=similarity,
                            duplicate_type=dup_type,
                            duplicate_lines=duplicate_lines,
                            suggestion=self._get_duplicate_suggestion(dup_type, similarity)
                        ))
        
        return duplicates
    
    def _compute_similarity(self, fp1: Dict, fp2: Dict) -> float:
        """计算相似度"""
        similarities = []
        
        # 1. N-gram相似度（Jaccard系数）
        ngrams1 = fp1["ngrams"]
        ngrams2 = fp2["ngrams"]
        
        if ngrams1 and ngrams2:
            intersection = len(ngrams1 & ngrams2)
            union = len(ngrams1 | ngrams2)
            ngram_sim = intersection / union if union > 0 else 0
            similarities.append(ngram_sim * 0.4)
        
        # 2. Token相似度
        tokens1 = set(fp1["tokens"])
        tokens2 = set(fp2["tokens"])
        
        if tokens1 and tokens2:
            token_intersection = len(tokens1 & tokens2)
            token_union = len(tokens1 | tokens2)
            token_sim = token_intersection / token_union if token_union > 0 else 0
            similarities.append(token_sim * 0.3)
        
        # 3. 行哈希相似度
        line_hashes1 = set(fp1["line_hashes"])
        line_hashes2 = set(fp2["line_hashes"])
        
        if line_hashes1 and line_hashes2:
            line_intersection = len(line_hashes1 & line_hashes2)
            line_union = len(line_hashes1 | line_hashes2)
            line_sim = line_intersection / line_union if line_union > 0 else 0
            similarities.append(line_sim * 0.2)
        
        # 4. AST签名相似度
        if fp1["ast_signature"] == fp2["ast_signature"]:
            similarities.append(0.1)
        
        return sum(similarities)
    
    def _count_duplicate_lines(
        self, 
        line_hashes1: List[str], 
        line_hashes2: List[str]
    ) -> int:
        """计算重复行数"""
        set1 = set(line_hashes1)
        set2 = set(line_hashes2)
        return len(set1 & set2)
    
    def _get_duplicate_suggestion(self, dup_type: str, similarity: float) -> str:
        """获取重复建议"""
        if dup_type == "exact":
            return "完全相同的代码，建议提取为公共函数"
        elif dup_type == "near":
            return f"高度相似代码（{similarity:.0%}），建议考虑合并或提取公共部分"
        else:
            return "结构相似代码，可能存在重构机会"
    
    def _deduplicate_pairs(self, pairs: List[DuplicatePair]) -> List[DuplicatePair]:
        """去重，同一对实体只保留最高相似度"""
        pair_map = {}
        
        for pair in pairs:
            # 使用排序后的ID对作为key
            key = tuple(sorted([pair.entity1_id, pair.entity2_id]))
            
            if key not in pair_map or pair.similarity > pair_map[key].similarity:
                pair_map[key] = pair
        
        return sorted(pair_map.values(), key=lambda p: p.similarity, reverse=True)
    
    def _build_duplicate_groups(self, pairs: List[DuplicatePair]) -> List[Dict[str, Any]]:
        """构建重复组（多个相似实体组成一组）"""
        # 使用并查集构建连通分量
        parent = {}
        
        def find(x):
            if x not in parent:
                parent[x] = x
            if parent[x] != x:
                parent[x] = find(parent[x])
            return parent[x]
        
        def union(x, y):
            px, py = find(x), find(y)
            if px != py:
                parent[px] = py
        
        # 合并重复对
        for pair in pairs:
            union(pair.entity1_id, pair.entity2_id)
        
        # 按组分类
        groups = defaultdict(list)
        for pair in pairs:
            root = find(pair.entity1_id)
            groups[root].append(pair)
        
        # 构建结果
        result = []
        for root, group_pairs in groups.items():
            if len(group_pairs) > 1:  # 多个重复实体
                entities = set()
                total_lines = 0
                for pair in group_pairs:
                    entities.add((pair.entity1_id, pair.entity1_name, pair.entity1_file))
                    entities.add((pair.entity2_id, pair.entity2_name, pair.entity2_file))
                    total_lines += pair.duplicate_lines
                
                result.append({
                    "group_id": root,
                    "entities": list(entities),
                    "pair_count": len(group_pairs),
                    "avg_similarity": sum(p.similarity for p in group_pairs) / len(group_pairs),
                    "total_duplicate_lines": total_lines,
                    "recommendation": f"发现 {len(entities)} 个相似实体，建议提取公共实现"
                })
        
        return sorted(result, key=lambda g: g["pair_count"], reverse=True)
    
    def _generate_statistics(
        self, 
        entities: List[CodeEntity],
        duplicates: List[DuplicatePair]
    ) -> Dict[str, Any]:
        """生成统计信息"""
        if not entities:
            return {}
        
        total_loc = sum(e.lines_of_code for e in entities)
        duplicate_loc = sum(p.duplicate_lines for p in duplicates)
        
        exact_count = len([p for p in duplicates if p.duplicate_type == "exact"])
        near_count = len([p for p in duplicates if p.duplicate_type == "near"])
        structural_count = len([p for p in duplicates if p.duplicate_type == "structural"])
        
        return {
            "total_entities": len(entities),
            "total_loc": total_loc,
            "duplicate_pair_count": len(duplicates),
            "duplicate_loc": duplicate_loc,
            "duplicate_percentage": duplicate_loc / total_loc * 100 if total_loc > 0 else 0,
            "exact_duplicate_count": exact_count,
            "near_duplicate_count": near_count,
            "structural_duplicate_count": structural_count,
            "affected_entity_count": len(set(
                [p.entity1_id for p in duplicates] + [p.entity2_id for p in duplicates]
            ))
        }
    
    def _generate_recommendations(
        self, 
        duplicates: List[DuplicatePair],
        statistics: Dict[str, Any]
    ) -> List[str]:
        """生成建议"""
        recommendations = []
        
        if statistics.get("duplicate_percentage", 0) > 10:
            recommendations.append(
                f"代码重复率较高（{statistics['duplicate_percentage']:.1f}%），"
                "建议进行代码重构以消除重复"
            )
        
        if statistics.get("exact_duplicate_count", 0) > 5:
            recommendations.append(
                f"发现 {statistics['exact_duplicate_count']} 对完全重复的代码，"
                "优先处理这些重复可快速减少代码量"
            )
        
        # 找出最严重的重复
        if duplicates:
            top_duplicate = duplicates[0]
            recommendations.append(
                f"最严重的重复：{top_duplicate.entity1_name} 和 {top_duplicate.entity2_name} "
                f"（相似度 {top_duplicate.similarity:.0%}，重复 {top_duplicate.duplicate_lines} 行）"
            )
        
        if not recommendations:
            recommendations.append("代码重复率较低，继续保持良好的编码习惯")
        
        return recommendations
    
    def export_report(self, report: DuplicateReport, output_path: str) -> str:
        """导出报告"""
        import json
        
        def convert(obj):
            if hasattr(obj, '__dict__'):
                return {k: convert(v) for k, v in obj.__dict__.items()}
            elif isinstance(obj, tuple):
                return list(obj)
            elif isinstance(obj, list):
                return [convert(item) for item in obj]
            elif isinstance(obj, dict):
                return {k: convert(v) for k, v in obj.items()}
            return obj
        
        data = convert(report)
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        return output_path
