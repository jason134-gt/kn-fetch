"""
写作风格验证器 - 确保文档符合规范

验证规则：
1. 第三人称视角验证
2. 段落式写作验证
3. 专业术语使用验证
4. 逻辑连接词使用验证
5. 文档结构完整性验证

验证标准：
- 完全客观的第三人称视角
- 严谨专业的中文技术写作
- 段落式叙述，避免清单罗列
- 善用逻辑连接词体现设计推演过程
"""

import re
from typing import List, Dict, Any, Set
from dataclasses import dataclass
from enum import Enum


class ValidationLevel(Enum):
    """验证级别"""
    ERROR = "error"      # 严重问题，必须修复
    WARNING = "warning"   # 建议修复
    INFO = "info"        # 信息提示


@dataclass
class ValidationRule:
    """验证规则"""
    name: str
    pattern: str
    level: ValidationLevel
    message: str
    description: str


@dataclass
class ValidationResult:
    """验证结果"""
    rule: ValidationRule
    line_number: int
    matched_text: str
    suggestion: str


class StyleValidator:
    """写作风格验证器"""
    
    def __init__(self):
        # 专业术语保护列表
        self.protected_terms = {
            "API", "REST", "GraphQL", "ORM", "DTO", "VO", "Entity", 
            "Repository", "Service", "Controller", "Middleware",
            "CQRS", "Event Sourcing", "Microservice", "Monolith",
            "Dependency Injection", "IoC", "AOP", "MVC", "MVVM",
            "JWT", "OAuth", "SQL", "NoSQL", "Redis", "Kafka",
            "Docker", "Kubernetes", "Git", "CI/CD", "DevOps"
        }
        
        # 逻辑连接词列表
        self.logical_connectors = {
            "因果": ["因此", "所以", "因而", "由此可见", "由此可知"],
            "转折": ["然而", "但是", "不过", "相反", "尽管"],
            "顺序": ["首先", "其次", "然后", "接着", "最后"],
            "举例": ["例如", "比如", "举例来说", "以...为例"],
            "总结": ["综上所述", "总而言之", "总的来说", "简而言之"]
        }
        
        # 禁止使用的第一人称词汇
        self.first_person_indicators = {
            "我们", "我们的", "我", "我的", "笔者", "本人",
            "开发者", "程序员", "工程师", "作者"
        }
        
        # 过度使用的词汇（应该避免）
        self.overused_phrases = {
            "非常", "特别", "极其", "十分", "很多", "大量",
            "各种各样的", "不同的", "各种各样的"
        }
        
        # 验证规则集合（必须在所有属性定义之后）
        self.rules = self._build_validation_rules()
    
    def _build_validation_rules(self) -> List[ValidationRule]:
        """构建验证规则"""
        rules = []
        
        # 1. 第三人称验证规则
        rules.append(ValidationRule(
            name="third_person_required",
            pattern=r'(我们|我们的|我|我的|笔者|本人|开发者|程序员|工程师|作者)',
            level=ValidationLevel.ERROR,
            message="发现第一人称表述，请使用第三人称",
            description="技术文档应使用客观的第三人称视角"
        ))
        
        # 2. 清单式写作检测
        rules.append(ValidationRule(
            name="avoid_bullet_points",
            pattern=r'^\s*[-*•]\s+|^\s*\d+\.\s+',
            level=ValidationLevel.WARNING,
            message="发现清单式标记，建议改为段落式叙述",
            description="技术文档应采用段落式写作，避免清单罗列"
        ))
        
        # 3. 过度口语化检测
        rules.append(ValidationRule(
            name="avoid_colloquial_language",
            pattern=r'(说白了|说白了就是|说白了就是|说穿了|说白了)',
            level=ValidationLevel.WARNING,
            message="发现口语化表达，建议使用更专业的表述",
            description="技术文档应保持专业严谨的语言风格"
        ))
        
        # 4. 英文术语未保护检测
        rules.append(ValidationRule(
            name="unprotected_english_terms",
            pattern=r'\b(' + '|'.join(self.protected_terms) + r')\b(?!`)',
            level=ValidationLevel.WARNING,
            message="发现未保护的英文术语，建议使用反引号包裹",
            description="专业术语应使用反引号包裹以保持一致性"
        ))
        
        # 5. 过度使用的词汇
        rules.append(ValidationRule(
            name="overused_phrases",
            pattern=r'\b(' + '|'.join(self.overused_phrases) + r')\b',
            level=ValidationLevel.INFO,
            message="发现可能过度使用的词汇，建议替换为更精确的表达",
            description="避免使用模糊的量化词汇，应使用具体描述"
        ))
        
        # 6. 缺少逻辑连接词
        rules.append(ValidationRule(
            name="missing_logical_connectors",
            pattern=r'^[^。！？]*[。！？]\s*[^。！？]*[。！？]',
            level=ValidationLevel.INFO,
            message="连续句子间缺少逻辑连接词",
            description="建议在句子间添加逻辑连接词以体现设计推演过程"
        ))
        
        # 7. 段落长度检查
        rules.append(ValidationRule(
            name="paragraph_length_check",
            pattern=r'.{600,}',
            level=ValidationLevel.WARNING,
            message="段落过长，建议分割为多个段落",
            description="理想段落长度应在200-500字之间"
        ))
        
        # 8. 段落过短检查
        rules.append(ValidationRule(
            name="paragraph_too_short",
            pattern=r'^.{1,50}$',
            level=ValidationLevel.WARNING,
            message="段落过短，建议扩展内容",
            description="段落应包含完整的思想表达"
        ))
        
        # 9. 技术术语使用不当
        rules.append(ValidationRule(
            name="misused_technical_terms",
            pattern=r'(架构师|设计师|工程师).*(认为|觉得|以为)',
            level=ValidationLevel.WARNING,
            message="技术术语使用不当，建议使用客观描述",
            description="避免主观判断，应使用客观事实描述"
        ))
        
        # 10. 中文标点符号规范
        rules.append(ValidationRule(
            name="chinese_punctuation",
            pattern=r'[a-zA-Z0-9][，。！？]|[，。！？][a-zA-Z]',
            level=ValidationLevel.INFO,
            message="中英文标点混用，建议统一使用中文标点",
            description="中文文档应使用全角中文标点符号"
        ))
        
        return rules
    
    def validate_content(self, content: str) -> Dict[str, Any]:
        """
        验证内容是否符合写作风格规范
        
        Args:
            content: 待验证的内容
            
        Returns:
            验证结果统计
        """
        lines = content.split('\n')
        results = []
        
        # 按行应用验证规则
        for line_num, line in enumerate(lines, 1):
            for rule in self.rules:
                matches = re.finditer(rule.pattern, line, re.IGNORECASE | re.MULTILINE)
                for match in matches:
                    result = ValidationResult(
                        rule=rule,
                        line_number=line_num,
                        matched_text=match.group(),
                        suggestion=self._generate_suggestion(rule, match.group())
                    )
                    results.append(result)
        
        # 计算统计信息
        stats = self._calculate_statistics(results)
        
        return {
            "validation_results": results,
            "statistics": stats,
            "content_quality_score": self._calculate_quality_score(stats, len(lines))
        }
    
    def _generate_suggestion(self, rule: ValidationRule, matched_text: str) -> str:
        """生成修复建议"""
        if rule.name == "third_person_required":
            return f"建议将'{matched_text}'替换为客观的第三人称表述"
        elif rule.name == "avoid_bullet_points":
            return "建议将清单式标记改为段落式叙述，使用逻辑连接词衔接"
        elif rule.name == "avoid_colloquial_language":
            return "建议使用更专业的术语替代口语化表达"
        elif rule.name == "unprotected_english_terms":
            return f"建议将'{matched_text}'包裹为`{matched_text}`"
        elif rule.name == "overused_phrases":
            return "建议使用更精确的量化描述替代模糊词汇"
        elif rule.name == "missing_logical_connectors":
            return "建议在句子间添加逻辑连接词，如'因此'、'然而'、'例如'等"
        elif rule.name == "paragraph_length_check":
            return "建议将长段落分割为多个小段落，每个段落聚焦一个核心观点"
        elif rule.name == "paragraph_too_short":
            return "建议扩展段落内容，提供更多细节和解释"
        elif rule.name == "misused_technical_terms":
            return "建议使用客观事实描述替代主观判断"
        elif rule.name == "chinese_punctuation":
            return "建议统一使用中文全角标点符号"
        else:
            return "请检查并修正"
    
    def _calculate_statistics(self, results: List[ValidationResult]) -> Dict[str, int]:
        """计算统计信息"""
        stats = {
            "total_issues": len(results),
            "errors": 0,
            "warnings": 0,
            "info": 0,
            "by_rule": {}
        }
        
        for result in results:
            # 按级别统计
            if result.rule.level == ValidationLevel.ERROR:
                stats["errors"] += 1
            elif result.rule.level == ValidationLevel.WARNING:
                stats["warnings"] += 1
            elif result.rule.level == ValidationLevel.INFO:
                stats["info"] += 1
            
            # 按规则统计
            rule_name = result.rule.name
            if rule_name not in stats["by_rule"]:
                stats["by_rule"][rule_name] = 0
            stats["by_rule"][rule_name] += 1
        
        return stats
    
    def _calculate_quality_score(self, stats: Dict[str, int], total_lines: int) -> int:
        """计算内容质量分数"""
        base_score = 100
        
        # 严重问题扣分
        base_score -= stats["errors"] * 10
        
        # 警告问题扣分
        base_score -= stats["warnings"] * 5
        
        # 信息问题扣分
        base_score -= stats["info"] * 2
        
        # 问题密度扣分（每行问题数）
        if total_lines > 0:
            issue_density = stats["total_issues"] / total_lines
            if issue_density > 0.5:  # 每行超过0.5个问题
                base_score -= 20
            elif issue_density > 0.3:
                base_score -= 10
            elif issue_density > 0.1:
                base_score -= 5
        
        return max(0, base_score)
    
    def analyze_logical_structure(self, content: str) -> Dict[str, Any]:
        """分析逻辑结构"""
        analysis = {
            "paragraph_count": 0,
            "sentence_count": 0,
            "logical_connectors_found": [],
            "connector_density": 0.0,
            "structure_analysis": {}
        }
        
        # 计算段落数量
        paragraphs = [p for p in content.split('\n\n') if p.strip()]
        analysis["paragraph_count"] = len(paragraphs)
        
        # 计算句子数量
        sentences = re.split(r'[.!?。！？]\s*', content)
        sentences = [s for s in sentences if s.strip()]
        analysis["sentence_count"] = len(sentences)
        
        # 分析逻辑连接词使用
        connectors_found = []
        for connector_type, connectors in self.logical_connectors.items():
            for connector in connectors:
                if connector in content:
                    connectors_found.append({
                        "type": connector_type,
                        "connector": connector,
                        "count": content.count(connector)
                    })
        
        analysis["logical_connectors_found"] = connectors_found
        
        # 计算连接词密度
        if analysis["sentence_count"] > 0:
            total_connectors = sum(c["count"] for c in connectors_found)
            analysis["connector_density"] = total_connectors / analysis["sentence_count"]
        
        # 结构分析
        analysis["structure_analysis"] = self._analyze_paragraph_structure(paragraphs)
        
        return analysis
    
    def _analyze_paragraph_structure(self, paragraphs: List[str]) -> Dict[str, Any]:
        """分析段落结构"""
        structure = {
            "introduction_paragraphs": 0,
            "body_paragraphs": 0,
            "conclusion_paragraphs": 0,
            "avg_paragraph_length": 0,
            "paragraph_length_variance": 0
        }
        
        if not paragraphs:
            return structure
        
        # 计算段落长度
        paragraph_lengths = [len(p) for p in paragraphs]
        structure["avg_paragraph_length"] = sum(paragraph_lengths) / len(paragraphs)
        
        # 计算长度方差（衡量段落长度一致性）
        if len(paragraph_lengths) > 1:
            variance = sum((x - structure["avg_paragraph_length"]) ** 2 for x in paragraph_lengths) / len(paragraphs)
            structure["paragraph_length_variance"] = variance
        
        # 简单分类段落类型（基于内容和位置）
        for i, paragraph in enumerate(paragraphs):
            if i == 0:  # 第一段通常是介绍
                structure["introduction_paragraphs"] += 1
            elif i == len(paragraphs) - 1:  # 最后一段通常是结论
                structure["conclusion_paragraphs"] += 1
            else:  # 中间段落是主体
                structure["body_paragraphs"] += 1
        
        return structure
    
    def generate_improvement_report(self, content: str) -> Dict[str, Any]:
        """生成改进报告"""
        # 执行验证
        validation_result = self.validate_content(content)
        
        # 分析逻辑结构
        logical_analysis = self.analyze_logical_structure(content)
        
        # 生成改进建议
        improvement_suggestions = self._generate_improvement_suggestions(
            validation_result, logical_analysis
        )
        
        return {
            "validation_summary": validation_result["statistics"],
            "quality_score": validation_result["content_quality_score"],
            "logical_analysis": logical_analysis,
            "improvement_suggestions": improvement_suggestions,
            "detailed_issues": validation_result["validation_results"]
        }
    
    def _generate_improvement_suggestions(self, validation_result: Dict, logical_analysis: Dict) -> List[str]:
        """生成改进建议"""
        suggestions = []
        
        stats = validation_result["statistics"]
        quality_score = validation_result["content_quality_score"]
        
        # 基于验证结果生成建议
        if stats["errors"] > 0:
            suggestions.append("存在严重写作风格问题，建议优先修复第一人称表述")
        
        if stats["warnings"] > 0:
            suggestions.append("存在写作风格警告，建议检查并优化清单式标记和段落结构")
        
        # 基于逻辑分析生成建议
        connector_density = logical_analysis["connector_density"]
        if connector_density < 0.1:
            suggestions.append("逻辑连接词使用不足，建议增加因果、转折等连接词")
        elif connector_density > 0.5:
            suggestions.append("逻辑连接词使用可能过多，建议保持适度")
        
        # 基于段落结构生成建议
        paragraph_count = logical_analysis["paragraph_count"]
        if paragraph_count < 3:
            suggestions.append("段落数量较少，建议将内容组织为3-5个逻辑段落")
        elif paragraph_count > 6:
            suggestions.append("段落数量较多，建议合并相关段落")
        
        # 基于质量分数生成建议
        if quality_score < 60:
            suggestions.append("文档质量较低，建议全面检查并重写")
        elif quality_score < 80:
            suggestions.append("文档质量中等，建议重点优化主要问题")
        else:
            suggestions.append("文档质量良好，继续保持")
        
        return suggestions


# 使用示例
def test_style_validator():
    """测试写作风格验证器"""
    validator = StyleValidator()
    
    # 示例内容（包含一些典型问题）
    sample_content = """
    我们设计了这个系统，采用了分层架构。
    
    - 表现层处理用户请求
    - 业务层包含核心逻辑
    - 数据层管理持久化
    
    我们使用了Repository模式，这个模式非常有用。
    开发者认为这种设计特别合理。
    """
    
    # 验证内容
    result = validator.validate_content(sample_content)
    
    print("验证结果：")
    print(f"总问题数: {result['statistics']['total_issues']}")
    print(f"错误: {result['statistics']['errors']}")
    print(f"警告: {result['statistics']['warnings']}")
    print(f"信息: {result['statistics']['info']}")
    print(f"质量分数: {result['content_quality_score']}")
    
    print("\n详细问题：")
    for issue in result["validation_results"]:
        print(f"第{issue.line_number}行: {issue.rule.message}")
        print(f"  匹配文本: {issue.matched_text}")
        print(f"  建议: {issue.suggestion}")
        print()
    
    # 生成改进报告
    report = validator.generate_improvement_report(sample_content)
    print("改进报告：")
    for suggestion in report["improvement_suggestions"]:
        print(f"- {suggestion}")


if __name__ == "__main__":
    test_style_validator()