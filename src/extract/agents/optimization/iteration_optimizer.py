"""
迭代优化机制 - 支持自动优化和重新生成

功能：
1. 自动优化低质量内容
2. 质量反馈循环机制
3. 章节重新生成支持
4. 持续改进和迭代优化
5. 优化历史跟踪

设计目标：
- 建立完整的优化循环机制
- 支持自动化和手动优化
- 提供智能的优化建议
- 实现质量的持续改进
"""

from typing import Dict, List, Any, Optional
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
import json
from pathlib import Path


class OptimizationStrategy(Enum):
    """优化策略"""
    AUTO_FIX = "auto_fix"              # 自动修复
    REGENERATE = "regenerate"          # 重新生成
    ENHANCE = "enhance"                # 增强改进
    CONSOLIDATE = "consolidate"        # 整合优化
    SIMPLIFY = "simplify"              # 简化优化


class OptimizationTrigger(Enum):
    """优化触发条件"""
    LOW_QUALITY_SCORE = "low_quality_score"      # 质量分数过低
    USER_FEEDBACK = "user_feedback"              # 用户反馈
    CONSISTENCY_ISSUE = "consistency_issue"      # 一致性问题
    INCOMPLETE_CONTENT = "incomplete_content"    # 内容不完整
    PERFORMANCE_ISSUE = "performance_issue"      # 性能问题


@dataclass
class OptimizationContext:
    """优化上下文"""
    content: str                          # 待优化内容
    quality_score: float                  # 当前质量分数
    issues: List[Dict[str, Any]]          # 发现的问题
    target_score: float                   # 目标质量分数
    max_iterations: int                   # 最大迭代次数
    strategy: OptimizationStrategy        # 优化策略
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class OptimizationResult:
    """优化结果"""
    original_content: str                 # 原始内容
    optimized_content: str                # 优化后内容
    original_score: float                 # 原始分数
    optimized_score: float                # 优化后分数
    improvement: float                    # 改进幅度
    iterations: int                       # 迭代次数
    issues_fixed: List[str]               # 已修复问题
    remaining_issues: List[str]           # 剩余问题
    optimization_time: float              # 优化耗时
    strategy_used: OptimizationStrategy   # 使用的策略


@dataclass
class OptimizationHistory:
    """优化历史记录"""
    timestamp: datetime                   # 时间戳
    trigger: OptimizationTrigger          # 触发条件
    strategy: OptimizationStrategy        # 使用的策略
    result: OptimizationResult            # 优化结果
    feedback: Optional[str] = None        # 用户反馈


class QualityEvaluator:
    """质量评估器"""
    
    def __init__(self):
        self.evaluation_criteria = self._build_evaluation_criteria()
    
    def _build_evaluation_criteria(self) -> Dict[str, Any]:
        """构建评估标准"""
        return {
            "content_quality": {
                "weight": 0.3,
                "criteria": [
                    "准确性",
                    "完整性",
                    "相关性",
                    "深度"
                ]
            },
            "writing_quality": {
                "weight": 0.3,
                "criteria": [
                    "清晰度",
                    "流畅性",
                    "专业性",
                    "逻辑性"
                ]
            },
            "structure_quality": {
                "weight": 0.2,
                "criteria": [
                    "组织结构",
                    "层次分明",
                    "逻辑连贯",
                    "重点突出"
                ]
            },
            "technical_quality": {
                "weight": 0.2,
                "criteria": [
                    "术语准确",
                    "技术正确",
                    "图表质量",
                    "代码示例"
                ]
            }
        }
    
    def evaluate(self, content: str) -> Dict[str, Any]:
        """评估内容质量"""
        scores = {}
        
        # 内容质量评估
        scores["content_quality"] = self._evaluate_content_quality(content)
        
        # 写作质量评估
        scores["writing_quality"] = self._evaluate_writing_quality(content)
        
        # 结构质量评估
        scores["structure_quality"] = self._evaluate_structure_quality(content)
        
        # 技术质量评估
        scores["technical_quality"] = self._evaluate_technical_quality(content)
        
        # 计算总分
        total_score = 0
        for category, score in scores.items():
            weight = self.evaluation_criteria[category]["weight"]
            total_score += score * weight
        
        return {
            "total_score": round(total_score, 2),
            "category_scores": scores,
            "grade": self._get_quality_grade(total_score),
            "issues": self._identify_issues(scores)
        }
    
    def _evaluate_content_quality(self, content: str) -> float:
        """评估内容质量"""
        score = 100.0
        
        # 检查内容长度
        if len(content) < 500:
            score -= 20
        
        # 检查关键词密度
        if content.count('的') / len(content) > 0.05:
            score -= 10
        
        # 检查是否有实质内容
        if not any(keyword in content for keyword in ['架构', '设计', '实现', '技术']):
            score -= 15
        
        return max(0, score)
    
    def _evaluate_writing_quality(self, content: str) -> float:
        """评估写作质量"""
        score = 100.0
        
        # 检查段落长度
        paragraphs = content.split('\n\n')
        avg_paragraph_length = sum(len(p) for p in paragraphs) / len(paragraphs) if paragraphs else 0
        
        if avg_paragraph_length > 300:
            score -= 10
        elif avg_paragraph_length < 50:
            score -= 15
        
        # 检查是否有逻辑连接词
        logic_words = ['因此', '所以', '然而', '但是', '首先', '其次', '最后']
        if not any(word in content for word in logic_words):
            score -= 10
        
        return max(0, score)
    
    def _evaluate_structure_quality(self, content: str) -> float:
        """评估结构质量"""
        score = 100.0
        
        # 检查标题层次
        heading_count = content.count('#')
        if heading_count < 3:
            score -= 20
        elif heading_count > 20:
            score -= 10
        
        # 检查是否有列表过度使用
        list_count = content.count('\n- ') + content.count('\n* ')
        if list_count > 20:
            score -= 15
        
        return max(0, score)
    
    def _evaluate_technical_quality(self, content: str) -> float:
        """评估技术质量"""
        score = 100.0
        
        # 检查是否有技术术语
        tech_keywords = ['API', '架构', '模块', '接口', '类', '函数', '方法']
        if not any(keyword in content for keyword in tech_keywords):
            score -= 20
        
        # 检查是否有代码示例
        if '```' not in content:
            score -= 10
        
        # 检查是否有图表
        if 'mermaid' not in content.lower():
            score -= 5
        
        return max(0, score)
    
    def _get_quality_grade(self, score: float) -> str:
        """获取质量等级"""
        if score >= 90:
            return "优秀"
        elif score >= 80:
            return "良好"
        elif score >= 70:
            return "中等"
        elif score >= 60:
            return "及格"
        else:
            return "不及格"
    
    def _identify_issues(self, scores: Dict[str, float]) -> List[str]:
        """识别问题"""
        issues = []
        
        for category, score in scores.items():
            if score < 70:
                issues.append(f"{category}质量不足（{score}分）")
        
        return issues


class ContentOptimizer:
    """内容优化器"""
    
    def __init__(self, llm_client=None):
        self.llm = llm_client
        self.optimization_strategies = self._build_optimization_strategies()
    
    def _build_optimization_strategies(self) -> Dict[str, Any]:
        """构建优化策略"""
        return {
            OptimizationStrategy.AUTO_FIX: self._auto_fix_strategy,
            OptimizationStrategy.REGENERATE: self._regenerate_strategy,
            OptimizationStrategy.ENHANCE: self._enhance_strategy,
            OptimizationStrategy.CONSOLIDATE: self._consolidate_strategy,
            OptimizationStrategy.SIMPLIFY: self._simplify_strategy
        }
    
    def optimize(self, context: OptimizationContext) -> OptimizationResult:
        """执行优化"""
        start_time = datetime.now()
        
        # 选择优化策略
        strategy_func = self.optimization_strategies.get(context.strategy)
        if not strategy_func:
            raise ValueError(f"不支持的优化策略: {context.strategy}")
        
        # 执行优化
        optimized_content = strategy_func(context)
        
        # 评估优化结果
        evaluator = QualityEvaluator()
        optimized_evaluation = evaluator.evaluate(optimized_content)
        
        # 计算改进幅度
        improvement = optimized_evaluation["total_score"] - context.quality_score
        
        # 计算耗时
        optimization_time = (datetime.now() - start_time).total_seconds()
        
        return OptimizationResult(
            original_content=context.content,
            optimized_content=optimized_content,
            original_score=context.quality_score,
            optimized_score=optimized_evaluation["total_score"],
            improvement=improvement,
            iterations=1,
            issues_fixed=self._identify_fixed_issues(context.issues, optimized_evaluation["issues"]),
            remaining_issues=optimized_evaluation["issues"],
            optimization_time=optimization_time,
            strategy_used=context.strategy
        )
    
    def _auto_fix_strategy(self, context: OptimizationContext) -> str:
        """自动修复策略"""
        content = context.content
        
        # 自动修复常见问题
        # 1. 修复多余的空行
        content = self._fix_excessive_blank_lines(content)
        
        # 2. 修复标题格式
        content = self._fix_heading_format(content)
        
        # 3. 修复列表格式
        content = self._fix_list_format(content)
        
        # 4. 添加缺失的标点
        content = self._fix_missing_punctuation(content)
        
        return content
    
    def _regenerate_strategy(self, context: OptimizationContext) -> str:
        """重新生成策略"""
        if not self.llm:
            # 如果没有LLM，返回增强后的内容
            return self._enhance_content_fallback(context.content)
        
        # 使用LLM重新生成内容
        prompt = f"""请重新生成以下内容，提升其质量和可读性：

原文：
{context.content}

要求：
1. 保持核心信息不变
2. 提升表达的清晰度和专业性
3. 改善文章结构和逻辑性
4. 使用第三人称客观叙述
5. 采用段落式写作，避免清单罗列

请生成优化后的内容："""
        
        try:
            optimized = self.llm.chat_sync(
                system_prompt="你是一个专业的技术文档编辑，擅长优化和改进文档质量。",
                user_prompt=prompt,
                max_tokens=2000,
                temperature=0.7
            )
            return optimized
        except Exception as e:
            print(f"LLM重新生成失败: {e}")
            return self._enhance_content_fallback(context.content)
    
    def _enhance_strategy(self, context: OptimizationContext) -> str:
        """增强改进策略"""
        content = context.content
        
        # 1. 添加过渡句
        content = self._add_transitions(content)
        
        # 2. 丰富内容细节
        content = self._enrich_details(content)
        
        # 3. 添加实例说明
        content = self._add_examples(content)
        
        return content
    
    def _consolidate_strategy(self, context: OptimizationContext) -> str:
        """整合优化策略"""
        content = context.content
        
        # 1. 合并重复内容
        content = self._merge_duplicates(content)
        
        # 2. 重组结构
        content = self._reorganize_structure(content)
        
        # 3. 精简冗余表述
        content = self._simplify_redundancy(content)
        
        return content
    
    def _simplify_strategy(self, context: OptimizationContext) -> str:
        """简化优化策略"""
        content = context.content
        
        # 1. 简化复杂句子
        content = self._simplify_complex_sentences(content)
        
        # 2. 删除不必要的修饰
        content = self._remove_unnecessary_modifiers(content)
        
        # 3. 提取核心观点
        content = self._extract_core_points(content)
        
        return content
    
    # 辅助方法
    def _fix_excessive_blank_lines(self, content: str) -> str:
        """修复多余的空行"""
        import re
        # 将连续多个空行替换为两个空行
        content = re.sub(r'\n\s*\n\s*\n+', '\n\n', content)
        return content
    
    def _fix_heading_format(self, content: str) -> str:
        """修复标题格式"""
        lines = content.split('\n')
        fixed_lines = []
        
        for line in lines:
            # 确保标题前后有空行
            if line.startswith('#') and fixed_lines:
                if fixed_lines[-1].strip():
                    fixed_lines.append('')
            fixed_lines.append(line)
        
        return '\n'.join(fixed_lines)
    
    def _fix_list_format(self, content: str) -> str:
        """修复列表格式"""
        lines = content.split('\n')
        fixed_lines = []
        
        for i, line in enumerate(lines):
            if line.strip().startswith(('-', '*')):
                # 确保列表项之间只有一个空行
                if i > 0 and lines[i-1].strip() and not lines[i-1].strip().startswith(('-', '*')):
                    fixed_lines.append('')
            fixed_lines.append(line)
        
        return '\n'.join(fixed_lines)
    
    def _fix_missing_punctuation(self, content: str) -> str:
        """添加缺失的标点"""
        lines = content.split('\n')
        fixed_lines = []
        
        for line in lines:
            if line.strip() and not line.strip().startswith('#'):
                # 如果行末没有标点，添加句号
                if not line.rstrip().endswith(('.', '。', '!', '！', '?', '？', ':', '：', ';', '；')):
                    line = line.rstrip() + '。'
            fixed_lines.append(line)
        
        return '\n'.join(fixed_lines)
    
    def _add_transitions(self, content: str) -> str:
        """添加过渡句"""
        transitions = [
            "因此，",
            "此外，",
            "然而，",
            "具体而言，",
            "综上所述，"
        ]
        
        paragraphs = content.split('\n\n')
        enhanced_paragraphs = []
        
        for i, paragraph in enumerate(paragraphs):
            if i > 0 and not any(paragraph.startswith(t) for t in transitions):
                # 在段落开头添加过渡句
                import random
                transition = random.choice(transitions)
                paragraph = transition + paragraph[0].lower() + paragraph[1:]
            enhanced_paragraphs.append(paragraph)
        
        return '\n\n'.join(enhanced_paragraphs)
    
    def _enrich_details(self, content: str) -> str:
        """丰富内容细节"""
        # 简化实现：添加更多描述
        return content
    
    def _add_examples(self, content: str) -> str:
        """添加实例说明"""
        # 简化实现：在关键点后添加示例标记
        if '例如' not in content:
            content += "\n\n例如，在实际应用中..."
        return content
    
    def _merge_duplicates(self, content: str) -> str:
        """合并重复内容"""
        # 简化实现：删除完全相同的段落
        paragraphs = content.split('\n\n')
        unique_paragraphs = []
        seen = set()
        
        for paragraph in paragraphs:
            if paragraph.strip() not in seen:
                seen.add(paragraph.strip())
                unique_paragraphs.append(paragraph)
        
        return '\n\n'.join(unique_paragraphs)
    
    def _reorganize_structure(self, content: str) -> str:
        """重组结构"""
        # 简化实现：保持原结构
        return content
    
    def _simplify_redundancy(self, content: str) -> str:
        """精简冗余表述"""
        # 简化实现：删除一些常见的冗余词
        redundant_phrases = [
            '非常非常',
            '极其极其',
            '十分十分'
        ]
        
        for phrase in redundant_phrases:
            content = content.replace(phrase, phrase[:2])
        
        return content
    
    def _simplify_complex_sentences(self, content: str) -> str:
        """简化复杂句子"""
        # 简化实现：保持原内容
        return content
    
    def _remove_unnecessary_modifiers(self, content: str) -> str:
        """删除不必要的修饰"""
        # 简化实现：保持原内容
        return content
    
    def _extract_core_points(self, content: str) -> str:
        """提取核心观点"""
        # 简化实现：保持原内容
        return content
    
    def _identify_fixed_issues(self, original_issues: List, new_issues: List) -> List[str]:
        """识别已修复的问题"""
        return [issue for issue in original_issues if issue not in new_issues]
    
    def _enhance_content_fallback(self, content: str) -> str:
        """增强内容的降级方案"""
        # 简单的增强：添加标题和结构
        if not content.startswith('#'):
            content = "# 优化内容\n\n" + content
        
        return content


class IterationOptimizer:
    """迭代优化引擎"""
    
    def __init__(self, llm_client=None, max_iterations: int = 3):
        self.llm = llm_client
        self.max_iterations = max_iterations
        self.evaluator = QualityEvaluator()
        self.optimizer = ContentOptimizer(llm_client)
        self.history: List[OptimizationHistory] = []
    
    def optimize_content(
        self,
        content: str,
        target_score: float = 80.0,
        trigger: OptimizationTrigger = OptimizationTrigger.LOW_QUALITY_SCORE
    ) -> OptimizationResult:
        """迭代优化内容"""
        # 初始评估
        initial_evaluation = self.evaluator.evaluate(content)
        current_score = initial_evaluation["total_score"]
        
        # 如果已经达标，直接返回
        if current_score >= target_score:
            return OptimizationResult(
                original_content=content,
                optimized_content=content,
                original_score=current_score,
                optimized_score=current_score,
                improvement=0,
                iterations=0,
                issues_fixed=[],
                remaining_issues=initial_evaluation["issues"],
                optimization_time=0,
                strategy_used=OptimizationStrategy.AUTO_FIX
            )
        
        # 迭代优化
        optimized_content = content
        iterations = 0
        total_time = 0
        
        for iteration in range(self.max_iterations):
            iterations = iteration + 1
            
            # 选择优化策略
            strategy = self._select_strategy(current_score, target_score, iteration)
            
            # 创建优化上下文
            context = OptimizationContext(
                content=optimized_content,
                quality_score=current_score,
                issues=initial_evaluation["issues"],
                target_score=target_score,
                max_iterations=self.max_iterations,
                strategy=strategy
            )
            
            # 执行优化
            result = self.optimizer.optimize(context)
            optimized_content = result.optimized_content
            current_score = result.optimized_score
            total_time += result.optimization_time
            
            # 记录历史
            history_entry = OptimizationHistory(
                timestamp=datetime.now(),
                trigger=trigger,
                strategy=strategy,
                result=result
            )
            self.history.append(history_entry)
            
            # 检查是否达标
            if current_score >= target_score:
                break
            
            # 检查是否有改进
            if result.improvement <= 0:
                # 没有改进，尝试其他策略
                continue
        
        return OptimizationResult(
            original_content=content,
            optimized_content=optimized_content,
            original_score=initial_evaluation["total_score"],
            optimized_score=current_score,
            improvement=current_score - initial_evaluation["total_score"],
            iterations=iterations,
            issues_fixed=result.issues_fixed,
            remaining_issues=result.remaining_issues,
            optimization_time=total_time,
            strategy_used=strategy
        )
    
    def _select_strategy(self, current_score: float, target_score: float, iteration: int) -> OptimizationStrategy:
        """选择优化策略"""
        gap = target_score - current_score
        
        if gap > 20:
            # 差距较大，使用重新生成策略
            return OptimizationStrategy.REGENERATE
        elif gap > 10:
            # 中等差距，使用增强策略
            return OptimizationStrategy.ENHANCE
        else:
            # 小差距，使用自动修复
            return OptimizationStrategy.AUTO_FIX
    
    def optimize_batch(self, contents: Dict[str, str], target_score: float = 80.0) -> Dict[str, OptimizationResult]:
        """批量优化多个内容"""
        results = {}
        
        for content_id, content in contents.items():
            results[content_id] = self.optimize_content(
                content,
                target_score,
                OptimizationTrigger.LOW_QUALITY_SCORE
            )
        
        return results
    
    def get_optimization_summary(self) -> Dict[str, Any]:
        """获取优化摘要"""
        if not self.history:
            return {
                "total_optimizations": 0,
                "average_improvement": 0,
                "success_rate": 0
            }
        
        total_optimizations = len(self.history)
        improvements = [h.result.improvement for h in self.history]
        successes = sum(1 for h in self.history if h.result.optimized_score >= h.result.original_score * 1.1)
        
        return {
            "total_optimizations": total_optimizations,
            "average_improvement": round(sum(improvements) / len(improvements), 2),
            "success_rate": round(successes / total_optimizations * 100, 2),
            "strategies_used": {
                strategy.value: sum(1 for h in self.history if h.strategy == strategy)
                for strategy in OptimizationStrategy
            }
        }


# 使用示例
def test_iteration_optimizer():
    """测试迭代优化器"""
    # 创建优化器
    optimizer = IterationOptimizer(max_iterations=3)
    
    # 测试内容
    test_content = """
这个项目是一个代码分析工具
主要功能是分析代码
可以生成报告
"""
    
    # 执行优化
    result = optimizer.optimize_content(test_content, target_score=80.0)
    
    print("优化结果：")
    print(f"原始分数: {result.original_score}")
    print(f"优化后分数: {result.optimized_score}")
    print(f"改进幅度: {result.improvement}")
    print(f"迭代次数: {result.iterations}")
    print(f"使用策略: {result.strategy_used.value}")
    
    print("\n优化后内容：")
    print(result.optimized_content)
    
    # 获取优化摘要
    summary = optimizer.get_optimization_summary()
    print("\n优化摘要：")
    print(f"总优化次数: {summary['total_optimizations']}")
    print(f"平均改进: {summary['average_improvement']}")


if __name__ == "__main__":
    test_iteration_optimizer()