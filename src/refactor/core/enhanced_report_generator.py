"""
增强版重构分析报告生成器

生成包含四要素的详细报告：
- 是什么：问题描述、位置、原因分析
- 怎么修复：具体修复步骤、代码示例
- 有什么风险：风险分析、影响范围
- 有什么方案：多种解决方案对比
"""

import json
import logging
from pathlib import Path
from typing import Dict, List, Any, Optional
from datetime import datetime

from .problem_detail import (
    ProblemDetail, ProblemSummary, ProblemType, Severity, RiskLevel,
    Risk, Solution, FixStep, ImpactScope, CurrentState, ExpectedEffect,
    ModuleHealth, ExecutionPlan, EnhancedRefactoringReport
)
from .refactoring_task import KnowledgeContext, RiskLevel as TaskRiskLevel

logger = logging.getLogger(__name__)


class EnhancedReportGenerator:
    """增强版报告生成器"""
    
    def __init__(self, config: Dict[str, Any] = None):
        self.config = config or {}
        self.problem_counter = 0
    
    def generate(
        self,
        knowledge_context: KnowledgeContext,
        risk_assessments: List,
        debts: List,
        contracts: Dict,
        plans: List
    ) -> EnhancedRefactoringReport:
        """生成增强版报告"""
        
        print("\n生成增强版重构分析报告...")
        
        # 1. 生成问题详情
        problems = self._generate_problem_details(
            knowledge_context, risk_assessments, debts
        )
        
        # 2. 生成问题摘要
        problem_summary = [self._create_summary(p) for p in problems]
        
        # 3. 计算模块健康度
        module_health = self._calculate_module_health(
            knowledge_context, problems
        )
        
        # 4. 生成执行计划
        execution_plan = self._create_execution_plan(problems)
        
        # 5. 文件问题汇总
        file_problems = self._aggregate_file_problems(problems)
        
        # 6. 统计信息
        total_problems = len(problems)
        high_count = sum(1 for p in problems if p.severity == Severity.HIGH)
        medium_count = sum(1 for p in problems if p.severity == Severity.MEDIUM)
        low_count = sum(1 for p in problems if p.severity == Severity.LOW)
        estimated_hours = sum(
            s.work_hours for p in problems 
            for s in p.solutions if s.name == p.recommended_solution
        )
        
        # 7. 风险分布
        risk_dist = {}
        for level in TaskRiskLevel:
            risk_dist[level.value] = sum(
                1 for a in risk_assessments if a.risk_level == level
            )
        
        report = EnhancedRefactoringReport(
            project_name=knowledge_context.project_name,
            total_entities=knowledge_context.total_entities,
            total_files=knowledge_context.total_files,
            total_problems=total_problems,
            high_severity_count=high_count,
            medium_severity_count=medium_count,
            low_severity_count=low_count,
            estimated_fix_hours=estimated_hours,
            risk_distribution=risk_dist,
            problems=problems[:50],  # 限制数量
            problem_summary=problem_summary[:100],
            module_health=module_health,
            execution_plan=execution_plan,
            file_problems=file_problems
        )
        
        print(f"  已识别 {total_problems} 个问题")
        print(f"  高严重度: {high_count}, 中: {medium_count}, 低: {low_count}")
        
        return report
    
    def _generate_problem_details(
        self,
        context: KnowledgeContext,
        assessments: List,
        debts: List
    ) -> List[ProblemDetail]:
        """生成问题详情"""
        problems = []
        
        # 1. 高复杂度问题
        problems.extend(self._analyze_complexity(context))
        
        # 2. 大文件问题
        problems.extend(self._analyze_large_files(context))
        
        # 3. 缺少文档问题
        problems.extend(self._analyze_missing_docs(context))
        
        # 4. 从技术债务生成问题
        for debt in debts[:50]:
            problem = self._debt_to_problem(debt, context)
            if problem:
                problems.append(problem)
        
        # 按严重程度排序
        severity_order = {Severity.HIGH: 0, Severity.MEDIUM: 1, Severity.LOW: 2}
        problems.sort(key=lambda p: severity_order.get(p.severity, 99))
        
        return problems
    
    def _analyze_complexity(self, context: KnowledgeContext) -> List[ProblemDetail]:
        """分析高复杂度问题"""
        problems = []
        
        for entity_id, entity in context.entities.items():
            complexity = entity.get("complexity")
            if complexity and complexity > 15:
                self.problem_counter += 1
                problem = self._create_complexity_problem(entity, complexity)
                problems.append(problem)
        
        return problems[:20]  # 限制数量
    
    def _create_complexity_problem(
        self, entity: Dict, complexity: int
    ) -> ProblemDetail:
        """创建高复杂度问题"""
        entity_name = entity.get("name", "Unknown")
        file_path = entity.get("file_path", "")
        start_line = entity.get("start_line", 0)
        end_line = entity.get("end_line", 0)
        entity_type = entity.get("entity_type", "function")
        content = entity.get("content", "")
        
        # 确定严重程度
        if complexity > 25:
            severity = Severity.HIGH
        elif complexity > 20:
            severity = Severity.MEDIUM
        else:
            severity = Severity.LOW
        
        # 创建修复步骤
        fix_steps = self._create_complexity_fix_steps(entity, complexity)
        
        # 创建解决方案
        solutions = self._create_complexity_solutions(complexity)
        
        # 创建风险
        risks = self._create_refactoring_risks(entity)
        
        return ProblemDetail(
            problem_id=f"P{self.problem_counter:03d}",
            problem_type=ProblemType.HIGH_COMPLEXITY,
            severity=severity,
            module=self._extract_module(file_path),
            file_path=file_path,
            start_line=start_line,
            end_line=end_line,
            entity_name=entity_name,
            entity_type=entity_type,
            description=f"函数 `{entity_name}` 的圈复杂度为 {complexity}，超过阈值 15。"
                       f"包含过多的条件分支和嵌套逻辑，导致代码难以理解和维护。",
            current_state=CurrentState(
                complexity=complexity,
                lines_of_code=end_line - start_line + 1 if end_line > start_line else 0,
                has_docstring=bool(entity.get("docstring"))
            ),
            code_context=self._format_code_context(content, start_line, end_line),
            impact_scope=ImpactScope(
                caller_count=entity.get("caller_count", 0),
                callers=[],
                related_tests=[],
                related_files=[]
            ),
            fix_steps=fix_steps,
            expected_effect=ExpectedEffect(
                complexity_before=complexity,
                complexity_after=5,
                readability_improvement="显著提升",
                test_coverage_before="低",
                test_coverage_after="高"
            ),
            risks=risks,
            risk_level=RiskLevel.MEDIUM if severity == Severity.HIGH else RiskLevel.LOW,
            rollback_plan=f"git checkout -- {file_path}",
            solutions=solutions,
            recommended_solution="提取方法" if complexity < 25 else "策略模式",
            recommendation_reason="改动最小，风险可控，可渐进式进行"
        )
    
    def _create_complexity_fix_steps(
        self, entity: Dict, complexity: int
    ) -> List[FixStep]:
        """创建复杂度修复步骤"""
        entity_name = entity.get("name", "function")
        content = entity.get("content", "")
        
        steps = [
            FixStep(
                step_number=1,
                title="分析函数逻辑",
                description=f"分析 `{entity_name}` 函数的逻辑结构，识别可提取的代码块",
                original_code=self._truncate_code(content, 200),
                explanation="找出重复代码、嵌套过深的代码块、独立的业务逻辑"
            ),
            FixStep(
                step_number=2,
                title="提取验证逻辑",
                description="将输入验证逻辑提取为独立方法",
                original_code="if (input != null && input.isValid()) { ... }",
                fixed_code="validateInput(input);\nprocessData(input);",
                explanation="将验证逻辑分离，降低主函数复杂度"
            ),
            FixStep(
                step_number=3,
                title="提取业务逻辑",
                description="将核心业务逻辑提取为独立方法",
                original_code="// 大量业务逻辑代码",
                fixed_code=f"private void process{entity_name.title()}() {{ ... }}",
                explanation="每个提取的方法只做一件事"
            ),
            FixStep(
                step_number=4,
                title="简化主函数",
                description="重构后的主函数应简洁清晰",
                fixed_code=f"public void {entity_name}() {{\n"
                          f"    validateInput();\n"
                          f"    processData();\n"
                          f"    saveResult();\n"
                          f"}}",
                explanation="主函数只负责流程编排"
            )
        ]
        return steps
    
    def _create_complexity_solutions(self, complexity: int) -> List[Solution]:
        """创建复杂度解决方案"""
        solutions = [
            Solution(
                name="提取方法",
                description="将大函数拆分为多个职责单一的小函数",
                pros=["改动最小", "风险可控", "渐进式重构", "易于测试"],
                cons=["可能产生过多小函数"],
                work_hours=2.0,
                risk_level="低",
                recommendation=5
            ),
            Solution(
                name="策略模式",
                description="使用策略模式处理不同条件分支",
                pros=["扩展性好", "符合开闭原则", "易于添加新策略"],
                cons=["改动较大", "需要更多测试", "可能过度设计"],
                work_hours=4.0,
                risk_level="中",
                recommendation=4
            ),
            Solution(
                name="责任链模式",
                description="使用责任链处理流程",
                pros=["灵活性高", "可组合", "解耦"],
                cons=["过度设计风险", "调试困难"],
                work_hours=6.0,
                risk_level="中",
                recommendation=3
            )
        ]
        return solutions
    
    def _create_refactoring_risks(self, entity: Dict) -> List[Risk]:
        """创建重构风险"""
        return [
            Risk(
                risk_type="功能回归",
                description="提取方法可能改变原有逻辑行为",
                probability="中",
                impact="高",
                mitigation="编写完整的单元测试，确保行为一致"
            ),
            Risk(
                risk_type="性能影响",
                description="方法调用增加可能影响性能",
                probability="低",
                impact="低",
                mitigation="进行性能基准测试"
            ),
            Risk(
                risk_type="调用链变化",
                description="调用者可能依赖内部实现细节",
                probability="低",
                impact="中",
                mitigation="检查所有调用者，更新相关代码"
            )
        ]
    
    def _analyze_large_files(self, context: KnowledgeContext) -> List[ProblemDetail]:
        """分析大文件问题"""
        problems = []
        
        # 按文件分组统计行数
        file_stats = {}
        for entity in context.entities.values():
            file_path = entity.get("file_path", "")
            if file_path not in file_stats:
                file_stats[file_path] = {
                    "lines": 0,
                    "entities": [],
                    "classes": 0,
                    "functions": 0
                }
            
            loc = entity.get("lines_of_code", 0) or 0
            file_stats[file_path]["lines"] += loc
            file_stats[file_path]["entities"].append(entity)
            
            if entity.get("entity_type") == "class":
                file_stats[file_path]["classes"] += 1
            elif entity.get("entity_type") in ["function", "method"]:
                file_stats[file_path]["functions"] += 1
        
        # 找出大文件
        for file_path, stats in file_stats.items():
            if stats["lines"] > 300:
                self.problem_counter += 1
                problem = self._create_large_file_problem(file_path, stats)
                problems.append(problem)
        
        return problems[:10]
    
    def _create_large_file_problem(
        self, file_path: str, stats: Dict
    ) -> ProblemDetail:
        """创建大文件问题"""
        loc = stats["lines"]
        
        severity = Severity.HIGH if loc > 500 else Severity.MEDIUM
        
        return ProblemDetail(
            problem_id=f"P{self.problem_counter:03d}",
            problem_type=ProblemType.LARGE_FILE,
            severity=severity,
            module=self._extract_module(file_path),
            file_path=file_path,
            start_line=1,
            end_line=loc,
            entity_name=Path(file_path).stem,
            entity_type="file",
            description=f"文件 `{Path(file_path).name}` 共有 {loc} 行代码，"
                       f"包含 {stats['classes']} 个类和 {stats['functions']} 个函数。"
                       f"文件过大不利于维护和理解。",
            current_state=CurrentState(
                lines_of_code=loc,
                complexity=None
            ),
            code_context=f"# 文件统计\n- 总行数: {loc}\n- 类数量: {stats['classes']}\n- 函数数量: {stats['functions']}",
            impact_scope=ImpactScope(
                caller_count=0,
                related_files=[file_path]
            ),
            fix_steps=[
                FixStep(
                    step_number=1,
                    title="分析文件结构",
                    description="识别文件中可以分离的职责模块"
                ),
                FixStep(
                    step_number=2,
                    title="提取独立类",
                    description="将相关的类提取到独立文件中"
                ),
                FixStep(
                    step_number=3,
                    title="按职责拆分",
                    description="按照单一职责原则拆分文件"
                )
            ],
            expected_effect=ExpectedEffect(
                readability_improvement="每个文件不超过300行"
            ),
            risks=[
                Risk(
                    risk_type="依赖关系变化",
                    description="拆分后需要更新import语句",
                    probability="高",
                    impact="低",
                    mitigation="使用IDE自动重构功能"
                )
            ],
            risk_level=RiskLevel.LOW,
            rollback_plan=f"git checkout -- {file_path}",
            solutions=[
                Solution(
                    name="按职责拆分",
                    description="按照单一职责原则拆分为多个文件",
                    pros=["职责清晰", "易于维护", "易于测试"],
                    cons=["文件数量增加"],
                    work_hours=3.0,
                    risk_level="低",
                    recommendation=4
                )
            ],
            recommended_solution="按职责拆分",
            recommendation_reason="符合单一职责原则，提高可维护性"
        )
    
    def _analyze_missing_docs(self, context: KnowledgeContext) -> List[ProblemDetail]:
        """分析缺少文档问题"""
        problems = []
        
        for entity_id, entity in context.entities.items():
            entity_type = entity.get("entity_type", "")
            if entity_type in ["class", "function", "method"]:
                if not entity.get("docstring"):
                    self.problem_counter += 1
                    problem = self._create_missing_doc_problem(entity)
                    problems.append(problem)
                    
                    if len(problems) >= 20:
                        break
        
        return problems
    
    def _create_missing_doc_problem(self, entity: Dict) -> ProblemDetail:
        """创建缺少文档问题"""
        entity_name = entity.get("name", "Unknown")
        file_path = entity.get("file_path", "")
        entity_type = entity.get("entity_type", "function")
        content = entity.get("content", "")
        
        return ProblemDetail(
            problem_id=f"P{self.problem_counter:03d}",
            problem_type=ProblemType.MISSING_DOC,
            severity=Severity.LOW,
            module=self._extract_module(file_path),
            file_path=file_path,
            start_line=entity.get("start_line", 0),
            end_line=entity.get("end_line", 0),
            entity_name=entity_name,
            entity_type=entity_type,
            description=f"{entity_type} `{entity_name}` 缺少文档注释。"
                       f"缺少文档会降低代码可读性和可维护性。",
            current_state=CurrentState(
                has_docstring=False
            ),
            code_context=self._truncate_code(content, 100),
            impact_scope=ImpactScope(),
            fix_steps=[
                FixStep(
                    step_number=1,
                    title="添加文档注释",
                    description=f"为 `{entity_name}` 添加文档注释",
                    fixed_code=self._generate_doc_template(entity),
                    explanation="描述功能、参数、返回值"
                )
            ],
            expected_effect=ExpectedEffect(
                readability_improvement="提升代码可读性"
            ),
            risks=[
                Risk(
                    risk_type="无",
                    description="添加文档注释无风险",
                    probability="低",
                    impact="低",
                    mitigation="无需特殊处理"
                )
            ],
            risk_level=RiskLevel.LOW,
            rollback_plan="无需回滚",
            solutions=[
                Solution(
                    name="添加文档注释",
                    description="编写规范的文档注释",
                    pros=["提高可读性", "便于维护", "自动生成API文档"],
                    cons=["需要时间"],
                    work_hours=0.5,
                    risk_level="低",
                    recommendation=5
                )
            ],
            recommended_solution="添加文档注释",
            recommendation_reason="简单有效，无风险"
        )
    
    def _debt_to_problem(
        self, debt: Any, context: KnowledgeContext
    ) -> Optional[ProblemDetail]:
        """将技术债务转换为问题详情"""
        self.problem_counter += 1
        
        return ProblemDetail(
            problem_id=f"P{self.problem_counter:03d}",
            problem_type=ProblemType.CODE_SMELL,
            severity=Severity.MEDIUM if debt.severity == "high" else Severity.LOW,
            module=self._extract_module(debt.file_path),
            file_path=debt.file_path,
            start_line=0,
            end_line=0,
            entity_name=debt.entity_name or "Unknown",
            entity_type="code",
            description=debt.description,
            current_state=CurrentState(),
            code_context="",
            impact_scope=ImpactScope(),
            fix_steps=[
                FixStep(
                    step_number=1,
                    title="修复问题",
                    description=debt.suggestion
                )
            ],
            expected_effect=ExpectedEffect(),
            risks=[],
            risk_level=RiskLevel.LOW,
            rollback_plan=f"git checkout -- {debt.file_path}",
            solutions=[
                Solution(
                    name=debt.suggestion,
                    description=debt.suggestion,
                    pros=["解决技术债务"],
                    cons=[],
                    work_hours=1.0,
                    risk_level="低",
                    recommendation=4
                )
            ],
            recommended_solution=debt.suggestion,
            recommendation_reason="直接解决发现的问题"
        )
    
    def _create_summary(self, problem: ProblemDetail) -> ProblemSummary:
        """创建问题摘要"""
        return ProblemSummary(
            problem_id=problem.problem_id,
            problem_type=problem.problem_type,
            severity=problem.severity,
            file_path=problem.file_path,
            entity_name=problem.entity_name,
            description=problem.description[:100],
            recommended_solution=problem.recommended_solution
        )
    
    def _calculate_module_health(
        self, context: KnowledgeContext, problems: List[ProblemDetail]
    ) -> List[ModuleHealth]:
        """计算模块健康度"""
        module_stats = {}
        
        for problem in problems:
            module = problem.module or "unknown"
            if module not in module_stats:
                module_stats[module] = {
                    "problems": [],
                    "high_count": 0
                }
            module_stats[module]["problems"].append(problem.problem_id)
            if problem.severity == Severity.HIGH:
                module_stats[module]["high_count"] += 1
        
        module_health = []
        for module, stats in module_stats.items():
            problem_count = len(stats["problems"])
            # 健康度计算：100 - 问题数*5 - 高严重度*10
            health_score = max(0, 100 - problem_count * 5 - stats["high_count"] * 10)
            
            main_issues = list(set(
                p.problem_type.value for p in problems if p.module == module
            ))[:3]
            
            module_health.append(ModuleHealth(
                module_name=module,
                health_score=health_score,
                problem_count=problem_count,
                high_severity_count=stats["high_count"],
                main_issues=main_issues
            ))
        
        # 按健康度排序
        module_health.sort(key=lambda m: m.health_score)
        return module_health[:10]
    
    def _create_execution_plan(
        self, problems: List[ProblemDetail]
    ) -> List[ExecutionPlan]:
        """创建执行计划"""
        high_problems = [p for p in problems if p.severity == Severity.HIGH]
        medium_problems = [p for p in problems if p.severity == Severity.MEDIUM]
        low_problems = [p for p in problems if p.severity == Severity.LOW]
        
        return [
            ExecutionPlan(
                phase="阶段一",
                priority="P0/P1 紧急修复",
                problems=[p.problem_id for p in high_problems[:10]],
                estimated_hours=sum(
                    s.work_hours for p in high_problems[:10]
                    for s in p.solutions if s.name == p.recommended_solution
                ),
                description="优先解决高风险问题，确保系统稳定性"
            ),
            ExecutionPlan(
                phase="阶段二",
                priority="P2 重点优化",
                problems=[p.problem_id for p in medium_problems[:20]],
                estimated_hours=sum(
                    s.work_hours for p in medium_problems[:20]
                    for s in p.solutions if s.name == p.recommended_solution
                ),
                description="优化中等风险问题，提升代码质量"
            ),
            ExecutionPlan(
                phase="阶段三",
                priority="P3 持续改进",
                problems=[p.problem_id for p in low_problems[:30]],
                estimated_hours=sum(
                    s.work_hours for p in low_problems[:30]
                    for s in p.solutions if s.name == p.recommended_solution
                ),
                description="逐步改进低风险问题，完善代码规范"
            )
        ]
    
    def _aggregate_file_problems(
        self, problems: List[ProblemDetail]
    ) -> Dict[str, List[str]]:
        """按文件汇总问题"""
        file_probs = {}
        
        for p in problems:
            if p.file_path not in file_probs:
                file_probs[p.file_path] = []
            file_probs[p.file_path].append(
                f"{p.problem_id}: {p.problem_type.value}"
            )
        
        return file_probs
    
    # === 辅助方法 ===
    
    def _extract_module(self, file_path: str) -> str:
        """从文件路径提取模块名"""
        if not file_path:
            return "unknown"
        
        parts = Path(file_path).parts
        if len(parts) > 2:
            return parts[-2]
        return Path(file_path).stem
    
    def _format_code_context(
        self, content: str, start_line: int, end_line: int
    ) -> str:
        """格式化代码上下文"""
        if not content:
            return ""
        
        lines = content.split('\n')
        context_lines = lines[:20]  # 限制行数
        
        result = []
        for i, line in enumerate(context_lines, start=start_line):
            result.append(f"{i:4d}: {line}")
        
        if len(lines) > 20:
            result.append(f"... 省略 {len(lines) - 20} 行")
        
        return '\n'.join(result)
    
    def _truncate_code(self, code: str, max_length: int) -> str:
        """截断代码"""
        if not code:
            return ""
        if len(code) <= max_length:
            return code
        return code[:max_length] + "..."
    
    def _generate_doc_template(self, entity: Dict) -> str:
        """生成文档模板"""
        entity_type = entity.get("entity_type", "function")
        name = entity.get("name", "")
        
        if entity_type == "class":
            return f'''/**
 * {name} - [类描述]
 * 
 * [详细描述]
 * 
 * @author [作者]
 * @since [日期]
 */'''
        else:
            return f'''/**
 * {name} - [功能描述]
 * 
 * [详细描述]
 * 
 * @param [参数名] [参数描述]
 * @return [返回值描述]
 * @throws [异常描述]
 */'''
    
    def _safe_filename(self, name: str) -> str:
        """清理文件名中的特殊字符"""
        import re
        # 替换特殊字符为下划线
        safe = re.sub(r'[<>:"/\\|?*\s\(\)\[\]\{\}=]', '_', name)
        # 移除连续下划线
        safe = re.sub(r'_+', '_', safe)
        # 移除首尾下划线
        safe = safe.strip('_')
        return safe if safe else "unknown"
    
    def save_report(
        self, report: EnhancedRefactoringReport, output_dir: str
    ):
        """保存报告"""
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # 保存JSON
        data_dir = output_path / "data"
        data_dir.mkdir(exist_ok=True)
        
        with open(data_dir / "report.json", "w", encoding="utf-8") as f:
            json.dump(report.model_dump(mode='json'), f, ensure_ascii=False, indent=2)
        
        # 保存详细问题
        problems_dir = output_path / "problems"
        problems_dir.mkdir(exist_ok=True)
        
        for problem in report.problems[:20]:  # 限制数量
            problem_md = self._generate_problem_markdown(problem)
            # 清理文件名中的特殊字符
            safe_name = self._safe_filename(problem.entity_name[:20])
            with open(
                problems_dir / f"{problem.problem_id}_{safe_name}.md",
                "w", encoding="utf-8"
            ) as f:
                f.write(problem_md)
        
        # 保存主报告
        main_report = self._generate_main_report_markdown(report)
        with open(output_path / "report.md", "w", encoding="utf-8") as f:
            f.write(main_report)
        
        print(f"  报告已保存到: {output_path}")
    
    def _generate_problem_markdown(self, p: ProblemDetail) -> str:
        """生成问题详情Markdown"""
        lines = [
            f"# {p.problem_id}: {p.entity_name}",
            "",
            f"**问题类型**: {p.problem_type.value}",
            f"**严重程度**: {p.severity.value}",
            f"**文件位置**: `{p.file_path}:{p.start_line}-{p.end_line}`",
            "",
            "---",
            "",
            "## 一、是什么（问题描述）",
            "",
            f"**问题描述**:",
            "",
            p.description,
            "",
            "**当前状态**:",
            "",
            "| 指标 | 值 |",
            "|------|-----|"
        ]
        
        if p.current_state.complexity:
            lines.append(f"| 复杂度 | {p.current_state.complexity} |")
        if p.current_state.lines_of_code:
            lines.append(f"| 代码行数 | {p.current_state.lines_of_code} |")
        if p.current_state.has_docstring is not None:
            lines.append(f"| 有文档 | {'是' if p.current_state.has_docstring else '否'} |")
        
        if p.code_context:
            lines.extend([
                "",
                "**代码上下文**:",
                "",
                "```",
                p.code_context[:500],
                "```"
            ])
        
        lines.extend([
            "",
            "---",
            "",
            "## 二、怎么修复",
            ""
        ])
        
        for step in p.fix_steps:
            lines.extend([
                f"### 步骤{step.step_number}: {step.title}",
                "",
                step.description,
                ""
            ])
            if step.original_code:
                lines.extend([
                    "**原始代码**:",
                    "```",
                    step.original_code,
                    "```",
                    ""
                ])
            if step.fixed_code:
                lines.extend([
                    "**修复后代码**:",
                    "```",
                    step.fixed_code,
                    "```",
                    ""
                ])
        
        if p.expected_effect.complexity_before:
            lines.extend([
                "**预期效果**:",
                "",
                f"- 复杂度: {p.expected_effect.complexity_before} → {p.expected_effect.complexity_after}",
                f"- 可读性: {p.expected_effect.readability_improvement}",
                ""
            ])
        
        lines.extend([
            "---",
            "",
            "## 三、有什么风险",
            ""
        ])
        
        if p.risks:
            lines.extend([
                "| 风险类型 | 描述 | 可能性 | 影响 | 缓解措施 |",
                "|----------|------|--------|------|----------|"
            ])
            for risk in p.risks:
                lines.append(
                    f"| {risk.risk_type} | {risk.description} | "
                    f"{risk.probability} | {risk.impact} | {risk.mitigation} |"
                )
        
        lines.extend([
            "",
            f"**整体风险等级**: {p.risk_level.value}",
            "",
            f"**回滚方案**: `{p.rollback_plan}`",
            "",
            "---",
            "",
            "## 四、有什么方案",
            ""
        ])
        
        lines.extend([
            "| 方案 | 描述 | 优点 | 缺点 | 工作量 | 推荐度 |",
            "|------|------|------|------|--------|--------|"
        ])
        
        for s in p.solutions:
            pros = ", ".join(s.pros[:2]) if s.pros else "-"
            cons = ", ".join(s.cons[:2]) if s.cons else "-"
            stars = "⭐" * s.recommendation
            lines.append(
                f"| {s.name} | {s.description[:30]} | {pros[:20]} | "
                f"{cons[:20]} | {s.work_hours}h | {stars} |"
            )
        
        lines.extend([
            "",
            f"**推荐方案**: {p.recommended_solution}",
            "",
            f"**推荐理由**: {p.recommendation_reason}"
        ])
        
        return "\n".join(lines)
    
    def _generate_main_report_markdown(self, report: EnhancedRefactoringReport) -> str:
        """生成主报告Markdown"""
        lines = [
            "# 重构分析报告",
            "",
            f"**项目**: {report.project_name}",
            f"**生成时间**: {report.generated_at.strftime('%Y-%m-%d %H:%M:%S')}",
            "",
            "---",
            "",
            "## 一、执行摘要",
            "",
            "| 指标 | 数值 |",
            "|------|------|",
            f"| 总实体数 | {report.total_entities} |",
            f"| 总文件数 | {report.total_files} |",
            f"| 问题总数 | {report.total_problems} |",
            f"| 高严重度 | {report.high_severity_count} |",
            f"| 中严重度 | {report.medium_severity_count} |",
            f"| 低严重度 | {report.low_severity_count} |",
            f"| 预计修复工时 | {report.estimated_fix_hours:.1f} 小时 |",
            "",
            "### 风险分布",
            ""
        ]
        
        for level, count in report.risk_distribution.items():
            lines.append(f"- {level}: {count} 个实体")
        
        lines.extend([
            "",
            "---",
            "",
            "## 二、问题详情清单",
            ""
        ])
        
        # 问题摘要表
        lines.extend([
            "| 编号 | 类型 | 严重程度 | 文件 | 实体 | 描述 | 推荐方案 |",
            "|------|------|----------|------|------|------|----------|"
        ])
        
        for ps in report.problem_summary[:30]:
            lines.append(
                f"| {ps.problem_id} | {ps.problem_type.value} | {ps.severity.value} | "
                f"`{Path(ps.file_path).name}` | {ps.entity_name[:15]} | "
                f"{ps.description[:30]}... | {ps.recommended_solution} |"
            )
        
        # 模块健康度
        if report.module_health:
            lines.extend([
                "",
                "---",
                "",
                "## 三、模块健康度",
                "",
                "| 模块 | 健康度 | 问题数 | 高严重度 | 主要问题 |",
                "|------|--------|--------|----------|----------|"
            ])
            
            for mh in report.module_health:
                issues = ", ".join(mh.main_issues[:2])
                lines.append(
                    f"| {mh.module_name} | {mh.health_score}/100 | "
                    f"{mh.problem_count} | {mh.high_severity_count} | {issues} |"
                )
        
        # 执行计划
        lines.extend([
            "",
            "---",
            "",
            "## 四、执行计划",
            ""
        ])
        
        for ep in report.execution_plan:
            lines.extend([
                f"### {ep.phase}: {ep.priority}",
                "",
                f"**描述**: {ep.description}",
                "",
                f"**预计工时**: {ep.estimated_hours:.1f} 小时",
                "",
                f"**问题清单**: {', '.join(ep.problems[:10])}",
                ""
            ])
        
        # 附录
        lines.extend([
            "---",
            "",
            "## 五、附录",
            "",
            "### A. 文件问题汇总",
            ""
        ])
        
        for file_path, probs in list(report.file_problems.items())[:20]:
            lines.append(f"- `{Path(file_path).name}`: {', '.join(probs[:5])}")
        
        lines.extend([
            "",
            "---",
            "",
            "*详细问题报告请查看 `problems/` 目录*"
        ])
        
        return "\n".join(lines)


__all__ = ['EnhancedReportGenerator']
