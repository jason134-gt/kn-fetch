"""
段落式写作引擎 - 优化Agent输出质量

功能：
1. 段落化写作：将内容转换为专业的段落式叙述
2. 逻辑连接：自动添加逻辑连接词体现设计推演过程
3. 结构优化：采用"论点-论据-结论"结构
4. 风格统一：确保专业术语保留英文，使用第三人称视角

设计原则：
- 严谨专业的中文技术写作
- 完全客观的第三人称视角
- 段落式叙述，避免清单罗列
- 善用逻辑连接词体现设计推演过程
"""

import re
from typing import List, Dict, Any
from dataclasses import dataclass
from enum import Enum


class ParagraphType(Enum):
    """段落类型"""
    INTRODUCTION = "introduction"  # 引入段落
    THESIS = "thesis"             # 论点段落
    EVIDENCE = "evidence"         # 论据段落
    CONCLUSION = "conclusion"     # 结论段落
    TRANSITION = "transition"     # 过渡段落


@dataclass
class Paragraph:
    """段落结构"""
    type: ParagraphType
    content: str
    logical_connectors: List[str] = None
    
    def __post_init__(self):
        if self.logical_connectors is None:
            self.logical_connectors = []


class ParagraphWritingEngine:
    """段落式写作引擎"""
    
    def __init__(self):
        # 逻辑连接词库
        self.logical_connectors = {
            "causal": ["因此", "所以", "因而", "由此可见", "由此可知"],
            "contrast": ["然而", "但是", "不过", "相反", "尽管"],
            "sequential": ["首先", "其次", "然后", "接着", "最后"],
            "exemplification": ["例如", "比如", "举例来说", "以...为例"],
            "conclusion": ["综上所述", "总而言之", "总的来说", "简而言之"]
        }
        
        # 专业术语保护（英文术语不翻译）
        self.protected_terms = {
            "API", "REST", "GraphQL", "ORM", "DTO", "VO", "Entity", 
            "Repository", "Service", "Controller", "Middleware",
            "CQRS", "Event Sourcing", "Microservice", "Monolith",
            "Dependency Injection", "IoC", "AOP", "MVC", "MVVM"
        }
        
        # 写作风格规则
        self.style_rules = {
            "third_person_only": True,
            "avoid_list_bullets": True,
            "min_paragraph_length": 100,
            "max_paragraph_length": 500
        }
    
    def convert_to_paragraphs(self, content: str, content_type: str = "analysis") -> str:
        """
        将内容转换为段落式写作
        
        Args:
            content: 原始内容
            content_type: 内容类型（analysis/architecture/design）
            
        Returns:
            段落化后的内容
        """
        # 1. 预处理内容
        cleaned_content = self._preprocess_content(content)
        
        # 2. 提取关键信息
        key_points = self._extract_key_points(cleaned_content, content_type)
        
        # 3. 构建段落结构
        paragraphs = self._build_paragraph_structure(key_points, content_type)
        
        # 4. 应用写作风格
        formatted_content = self._apply_writing_style(paragraphs)
        
        # 5. 质量检查
        quality_checked = self._quality_check(formatted_content)
        
        return quality_checked
    
    def _preprocess_content(self, content: str) -> str:
        """预处理内容"""
        # 移除多余的换行和空格
        cleaned = re.sub(r'\n\s*\n', '\n\n', content)
        cleaned = re.sub(r' +', ' ', cleaned)
        
        # 保护专业术语
        for term in self.protected_terms:
            cleaned = re.sub(fr'\b{term}\b', f'`{term}`', cleaned, flags=re.IGNORECASE)
        
        return cleaned.strip()
    
    def _extract_key_points(self, content: str, content_type: str) -> List[Dict[str, Any]]:
        """提取关键信息点"""
        key_points = []
        
        # 按句子分割
        sentences = re.split(r'[.!?。！？]\s*', content)
        sentences = [s.strip() for s in sentences if s.strip()]
        
        for sentence in sentences:
            if len(sentence) < 10:  # 太短的句子跳过
                continue
            
            # 根据内容类型确定权重
            weight = self._calculate_sentence_weight(sentence, content_type)
            
            if weight > 0.3:  # 只保留重要的句子
                key_points.append({
                    "sentence": sentence,
                    "weight": weight,
                    "type": self._classify_sentence_type(sentence)
                })
        
        # 按权重排序
        key_points.sort(key=lambda x: x["weight"], reverse=True)
        return key_points
    
    def _calculate_sentence_weight(self, sentence: str, content_type: str) -> float:
        """计算句子权重"""
        weight = 0.0
        
        # 关键词权重
        keywords = {
            "architecture": ["架构", "分层", "模块", "依赖", "接口", "抽象"],
            "design": ["设计", "模式", "原则", "策略", "实现", "封装"],
            "analysis": ["分析", "发现", "结论", "建议", "优化", "评估"]
        }
        
        for keyword in keywords.get(content_type, []):
            if keyword in sentence:
                weight += 0.2
        
        # 长度权重（适中的句子更易读）
        sentence_length = len(sentence)
        if 30 <= sentence_length <= 100:
            weight += 0.3
        elif sentence_length > 100:
            weight += 0.1
        
        # 专业术语权重
        term_count = sum(1 for term in self.protected_terms if term.lower() in sentence.lower())
        weight += term_count * 0.1
        
        return min(weight, 1.0)
    
    def _classify_sentence_type(self, sentence: str) -> str:
        """分类句子类型"""
        if any(word in sentence for word in ["因此", "所以", "因而"]):
            return "conclusion"
        elif any(word in sentence for word in ["例如", "比如", "举例"]):
            return "example"
        elif any(word in sentence for word in ["首先", "其次", "然后"]):
            return "sequential"
        elif any(word in sentence for word in ["然而", "但是", "不过"]):
            return "contrast"
        else:
            return "statement"
    
    def _build_paragraph_structure(self, key_points: List[Dict], content_type: str) -> List[Paragraph]:
        """构建段落结构"""
        paragraphs = []
        
        # 开头段落
        intro_points = [p for p in key_points if p["type"] in ["statement", "sequential"]]
        if intro_points:
            paragraphs.append(Paragraph(
                type=ParagraphType.INTRODUCTION,
                content=self._combine_sentences(intro_points[:3]),
                logical_connectors=["首先", "项目采用"]
            ))
        
        # 论点论据段落
        thesis_points = [p for p in key_points if p["weight"] > 0.5]
        evidence_points = [p for p in key_points if p["weight"] <= 0.5]
        
        if thesis_points:
            paragraphs.append(Paragraph(
                type=ParagraphType.THESIS,
                content=self._combine_sentences(thesis_points[:2]),
                logical_connectors=["具体而言", "从设计角度看"]
            ))
        
        if evidence_points:
            paragraphs.append(Paragraph(
                type=ParagraphType.EVIDENCE,
                content=self._combine_sentences(evidence_points[:3]),
                logical_connectors=["例如", "具体表现为"]
            ))
        
        # 结论段落
        conclusion_points = [p for p in key_points if p["type"] == "conclusion"]
        if not conclusion_points:
            conclusion_points = key_points[-2:]  # 取最后两句作为结论
        
        if conclusion_points:
            paragraphs.append(Paragraph(
                type=ParagraphType.CONCLUSION,
                content=self._combine_sentences(conclusion_points),
                logical_connectors=["综上所述", "总而言之"]
            ))
        
        return paragraphs
    
    def _combine_sentences(self, points: List[Dict]) -> str:
        """合并句子为段落"""
        sentences = [p["sentence"] for p in points]
        
        # 添加逻辑连接词
        connected_sentences = []
        for i, sentence in enumerate(sentences):
            if i == 0:
                connected_sentences.append(sentence)
            else:
                # 根据上下文选择合适的连接词
                connector = self._select_connector(sentences[i-1], sentence)
                connected_sentences.append(f"{connector}{sentence}")
        
        return "。".join(connected_sentences) + "。"
    
    def _select_connector(self, prev_sentence: str, current_sentence: str) -> str:
        """选择合适的逻辑连接词"""
        # 简化的连接词选择逻辑
        if "因此" in prev_sentence or "所以" in prev_sentence:
            return ""
        elif "例如" in prev_sentence or "比如" in prev_sentence:
            return ""
        else:
            return "此外，"
    
    def _apply_writing_style(self, paragraphs: List[Paragraph]) -> str:
        """应用写作风格"""
        formatted_paragraphs = []
        
        for i, paragraph in enumerate(paragraphs):
            content = paragraph.content
            
            # 确保第三人称
            if self.style_rules["third_person_only"]:
                content = self._ensure_third_person(content)
            
            # 避免清单式写作
            if self.style_rules["avoid_list_bullets"]:
                content = self._remove_bullet_points(content)
            
            # 控制段落长度
            if len(content) < self.style_rules["min_paragraph_length"]:
                content = self._expand_paragraph(content)
            elif len(content) > self.style_rules["max_paragraph_length"]:
                content = self._shorten_paragraph(content)
            
            # 添加段落标记
            if i > 0:  # 非首段添加空行
                formatted_paragraphs.append("")
            
            formatted_paragraphs.append(content)
        
        return "\n".join(formatted_paragraphs)
    
    def _ensure_third_person(self, content: str) -> str:
        """确保第三人称"""
        # 替换第一人称
        replacements = {
            "我们": "该项目",
            "我们的": "该项目的",
            "我": "该分析",
            "我的": "该分析的",
            "开发者": "系统设计者",
            "用户": "使用者"
        }
        
        for old, new in replacements.items():
            content = content.replace(old, new)
        
        return content
    
    def _remove_bullet_points(self, content: str) -> str:
        """移除清单式标记"""
        # 移除Markdown清单标记
        content = re.sub(r'^[-*]\s+', '', content, flags=re.MULTILINE)
        
        # 将数字清单转换为连续文本
        content = re.sub(r'^\d+\.\s+', '', content, flags=re.MULTILINE)
        
        return content
    
    def _expand_paragraph(self, content: str) -> str:
        """扩展过短的段落"""
        if len(content) < 50:
            # 添加解释性内容
            connectors = ["具体而言，", "从技术角度看，", "在实现层面，"]
            import random
            content = random.choice(connectors) + content
        
        return content
    
    def _shorten_paragraph(self, content: str) -> str:
        """缩短过长的段落"""
        if len(content) > 600:
            # 简单分割为两个段落
            sentences = re.split(r'[.!?。！？]', content)
            sentences = [s.strip() for s in sentences if s.strip()]
            
            if len(sentences) > 2:
                mid_point = len(sentences) // 2
                first_half = "。".join(sentences[:mid_point]) + "。"
                second_half = "。".join(sentences[mid_point:]) + "。"
                return first_half + "\n\n" + second_half
        
        return content
    
    def _quality_check(self, content: str) -> str:
        """质量检查"""
        # 检查段落数量
        paragraphs = content.split('\n\n')
        if len(paragraphs) < 2:
            # 添加过渡段落
            transition = "综上所述，该分析揭示了系统的核心架构特征和设计决策。"
            content += "\n\n" + transition
        
        # 检查专业术语保护
        for term in self.protected_terms:
            if term in content and f'`{term}`' not in content:
                content = content.replace(term, f'`{term}`')
        
        return content
    
    def validate_paragraph_structure(self, content: str) -> Dict[str, Any]:
        """验证段落结构"""
        validation_result = {
            "paragraph_count": 0,
            "avg_paragraph_length": 0,
            "logical_connectors_found": [],
            "third_person_violations": [],
            "bullet_points_found": 0,
            "quality_score": 0
        }
        
        # 计算段落数量
        paragraphs = [p for p in content.split('\n\n') if p.strip()]
        validation_result["paragraph_count"] = len(paragraphs)
        
        # 计算平均段落长度
        if paragraphs:
            avg_length = sum(len(p) for p in paragraphs) / len(paragraphs)
            validation_result["avg_paragraph_length"] = int(avg_length)
        
        # 检查逻辑连接词
        for connector_type, connectors in self.logical_connectors.items():
            for connector in connectors:
                if connector in content:
                    validation_result["logical_connectors_found"].append(connector)
        
        # 检查第三人称违规
        first_person_indicators = ["我们", "我们的", "我", "我的", "开发者认为", "我们设计"]
        for indicator in first_person_indicators:
            if indicator in content:
                validation_result["third_person_violations"].append(indicator)
        
        # 检查清单标记
        bullet_patterns = [r'^[-*]\s+', r'^\d+\.\s+']
        for pattern in bullet_patterns:
            matches = re.findall(pattern, content, flags=re.MULTILINE)
            validation_result["bullet_points_found"] += len(matches)
        
        # 计算质量分数
        quality_score = 100
        
        # 段落数量得分（理想3-5段）
        if validation_result["paragraph_count"] < 2:
            quality_score -= 20
        elif validation_result["paragraph_count"] > 6:
            quality_score -= 10
        
        # 段落长度得分（理想200-400字）
        avg_len = validation_result["avg_paragraph_length"]
        if avg_len < 100:
            quality_score -= 15
        elif avg_len > 500:
            quality_score -= 10
        
        # 逻辑连接词得分
        if len(validation_result["logical_connectors_found"]) < 2:
            quality_score -= 10
        
        # 第三人称违规扣分
        quality_score -= len(validation_result["third_person_violations"]) * 5
        
        # 清单标记扣分
        quality_score -= validation_result["bullet_points_found"] * 3
        
        validation_result["quality_score"] = max(0, quality_score)
        
        return validation_result


# 使用示例
def test_paragraph_writing_engine():
    """测试段落式写作引擎"""
    engine = ParagraphWritingEngine()
    
    # 示例内容
    sample_content = """
    该系统采用了分层架构设计。首先，表现层负责处理用户请求。其次，业务层包含核心逻辑。然后，数据层管理持久化。
    使用了Repository模式进行数据访问。依赖注入控制对象创建。API设计遵循RESTful原则。
    """
    
    # 转换为段落式写作
    result = engine.convert_to_paragraphs(sample_content, "architecture")
    print("转换结果：")
    print(result)
    print("\n" + "="*50 + "\n")
    
    # 验证结构
    validation = engine.validate_paragraph_structure(result)
    print("验证结果：")
    for key, value in validation.items():
        print(f"{key}: {value}")


if __name__ == "__main__":
    test_paragraph_writing_engine()