"""
质量评估Agent - 基于design-v1方案实现代码质量评估和重构建议
"""

import asyncio
from typing import Dict, Any, List
from datetime import datetime
from .base_agent import BaseAgent, AgentResult


class QualityAssessorAgent(BaseAgent):
    """质量评估Agent"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__("quality_assessor", config)
        
        # 配置参数
        self.enable_test_generation = config.get("enable_test_generation", True)
        self.complexity_threshold = config.get("complexity_threshold", 10)
        self.quality_metrics = config.get("quality_metrics", ["cyclomatic", "cognitive", "maintainability"])
    
    async def _execute_impl(self, input_data: Any) -> Dict[str, Any]:
        """执行质量评估"""
        
        if not isinstance(input_data, dict):
            raise ValueError("输入数据格式错误，应为字典类型")
        
        code_metadata = input_data.get("code_metadata", {})
        architecture_data = input_data.get("architecture_data", {})
        
        # 执行质量评估
        quality_report = await self._assess_quality(code_metadata, architecture_data)
        
        # 生成重构建议
        refactoring_suggestions = await self._generate_refactoring_suggestions(quality_report)
        
        # 生成测试用例（如果需要）
        test_cases = []
        if self.enable_test_generation:
            test_cases = await self._generate_test_cases(quality_report)
        
        return {
            "quality_report": quality_report,
            "refactoring_suggestions": refactoring_suggestions,
            "test_cases": test_cases,
            "assessment_summary": self._generate_summary(quality_report)
        }
    
    async def _assess_quality(self, code_metadata: Dict[str, Any], architecture_data: Dict[str, Any]) -> Dict[str, Any]:
        """评估代码质量"""
        
        quality_metrics = {}
        
        # 1. 复杂度分析
        complexity_metrics = await self._analyze_complexity(code_metadata)
        quality_metrics["complexity"] = complexity_metrics
        
        # 2. 代码规范检查
        code_standards = await self._check_code_standards(code_metadata)
        quality_metrics["standards"] = code_standards
        
        # 3. 依赖关系质量
        dependency_quality = await self._analyze_dependency_quality(architecture_data)
        quality_metrics["dependencies"] = dependency_quality
        
        # 4. 可维护性评估
        maintainability = await self._assess_maintainability(code_metadata)
        quality_metrics["maintainability"] = maintainability
        
        # 5. 性能指标
        performance_metrics = await self._assess_performance(code_metadata)
        quality_metrics["performance"] = performance_metrics
        
        return quality_metrics
    
    async def _analyze_complexity(self, code_metadata: Dict[str, Any]) -> Dict[str, Any]:
        """分析代码复杂度"""
        
        complexity_report = {
            "high_complexity_files": [],
            "complexity_distribution": {},
            "overall_score": 0.0
        }
        
        total_complexity = 0
        file_count = 0
        
        # 分析每个文件的复杂度
        for file_meta in code_metadata.get("files", []):
            complexity = file_meta.get("complexity", {})
            cyclomatic = complexity.get("cyclomatic_complexity", 0)
            
            total_complexity += cyclomatic
            file_count += 1
            
            # 识别高复杂度文件
            if cyclomatic > self.complexity_threshold:
                complexity_report["high_complexity_files"].append({
                    "file_path": file_meta.get("file_path", ""),
                    "cyclomatic_complexity": cyclomatic,
                    "lines_of_code": complexity.get("lines_of_code", 0)
                })
        
        # 计算平均复杂度
        if file_count > 0:
            avg_complexity = total_complexity / file_count
            complexity_report["overall_score"] = max(0, 100 - (avg_complexity * 5))
        
        return complexity_report
    
    async def _check_code_standards(self, code_metadata: Dict[str, Any]) -> Dict[str, Any]:
        """检查代码规范"""
        
        standards_report = {
            "violations": [],
            "compliance_score": 100.0
        }
        
        # 检查代码规范
        violations_count = 0
        total_checks = 0
        
        # 检查注释密度
        for file_meta in code_metadata.get("files", []):
            complexity = file_meta.get("complexity", {})
            comment_density = complexity.get("comment_density", 0)
            
            total_checks += 1
            
            # 注释密度过低
            if comment_density < 0.1:  # 低于10%
                violations_count += 1
                standards_report["violations"].append({
                    "type": "low_comment_density",
                    "file_path": file_meta.get("file_path", ""),
                    "current_value": comment_density,
                    "recommended": "≥10%"
                })
        
        # 计算合规分数
        if total_checks > 0:
            standards_report["compliance_score"] = 100 - (violations_count / total_checks * 100)
        
        return standards_report
    
    async def _analyze_dependency_quality(self, architecture_data: Dict[str, Any]) -> Dict[str, Any]:
        """分析依赖关系质量"""
        
        dependency_report = {
            "circular_dependencies": [],
            "high_coupling_modules": [],
            "dependency_quality_score": 100.0
        }
        
        # 分析依赖关系
        dependencies = architecture_data.get("dependencies", [])
        
        # 识别循环依赖
        circular_deps = self._detect_circular_dependencies(dependencies)
        dependency_report["circular_dependencies"] = circular_deps
        
        # 识别高耦合模块
        high_coupling = self._detect_high_coupling(dependencies)
        dependency_report["high_coupling_modules"] = high_coupling
        
        # 计算依赖质量分数
        if dependencies:
            quality_score = max(0, 100 - (len(circular_deps) * 10 - len(high_coupling) * 5))
            dependency_report["dependency_quality_score"] = quality_score
        
        return dependency_report
    
    def _detect_circular_dependencies(self, dependencies: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """检测循环依赖"""
        
        # 简化实现：在实际应用中应该使用图算法
        circular_deps = []
        
        # 这里实现循环依赖检测逻辑
        # 返回检测到的循环依赖列表
        
        return circular_deps
    
    def _detect_high_coupling(self, dependencies: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """检测高耦合模块"""
        
        # 简化实现：在实际应用中应该使用图算法
        high_coupling = []
        
        # 这里实现高耦合检测逻辑
        # 返回高耦合模块列表
        
        return high_coupling
    
    async def _assess_maintainability(self, code_metadata: Dict[str, Any]) -> Dict[str, Any]:
        """评估可维护性"""
        
        maintainability_report = {
            "maintainability_index": 0.0,
            "risk_factors": [],
            "recommendations": []
        }
        
        # 计算可维护性指数
        total_mi = 0
        file_count = 0
        
        for file_meta in code_metadata.get("files", []):
            complexity = file_meta.get("complexity", {})
            mi = complexity.get("maintainability_index", 0)
            
            total_mi += mi
            file_count += 1
        
        if file_count > 0:
            maintainability_report["maintainability_index"] = total_mi / file_count
        
        # 识别风险因素
        if maintainability_report["maintainability_index"] < 60:
            maintainability_report["risk_factors"].append("可维护性指数偏低")
        
        return maintainability_report
    
    async def _assess_performance(self, code_metadata: Dict[str, Any]) -> Dict[str, Any]:
        """评估性能指标"""
        
        performance_report = {
            "performance_indicators": [],
            "potential_bottlenecks": [],
            "performance_score": 100.0
        }
        
        # 分析性能指标
        # 这里可以实现性能分析逻辑
        
        return performance_report
    
    async def _generate_refactoring_suggestions(self, quality_report: Dict[str, Any]) -> List[Dict[str, Any]]:
        """生成重构建议"""
        
        suggestions = []
        
        # 基于复杂度分析的建议
        complexity_data = quality_report.get("complexity", {})
        for high_complexity_file in complexity_data.get("high_complexity_files", []):
            suggestions.append({
                "type": "complexity_reduction",
                "file_path": high_complexity_file["file_path"],
                "description": f"函数复杂度过高（{high_complexity_file['cyclomatic_complexity']}），建议拆分重构",
                "priority": "high",
                "estimated_effort": "medium"
            })
        
        # 基于代码规范的建议
        standards_data = quality_report.get("standards", {})
        for violation in standards_data.get("violations", []):
            suggestions.append({
                "type": "code_standards",
                "file_path": violation["file_path"],
                "description": f"{violation['type']}：当前值 {violation['current_value']}，建议 {violation['recommended']}",
                "priority": "medium",
                "estimated_effort": "low"
            })
        
        # 基于依赖关系的建议
        dependency_data = quality_report.get("dependencies", {})
        for circular_dep in dependency_data.get("circular_dependencies", []):
            suggestions.append({
                "type": "dependency_optimization",
                "description": f"检测到循环依赖：{circular_dep}",
                "priority": "high",
                "estimated_effort": "high"
            })
        
        return suggestions
    
    async def _generate_test_cases(self, quality_report: Dict[str, Any]) -> List[Dict[str, Any]]:
        """生成测试用例"""
        
        test_cases = []
        
        # 为高复杂度代码生成测试用例
        complexity_data = quality_report.get("complexity", {})
        for high_complexity_file in complexity_data.get("high_complexity_files", []):
            test_cases.append({
                "file_path": high_complexity_file["file_path"],
                "test_type": "unit_test",
                "coverage_target": 0.8,
                "test_cases": [
                    {
                        "name": "test_basic_functionality",
                        "description": "测试基本功能",
                        "priority": "high"
                    },
                    {
                        "name": "test_edge_cases", 
                        "description": "测试边界情况",
                        "priority": "medium"
                    }
                ]
            })
        
        return test_cases
    
    def _generate_summary(self, quality_report: Dict[str, Any]) -> Dict[str, Any]:
        """生成质量评估摘要"""
        
        complexity_score = quality_report.get("complexity", {}).get("overall_score", 0)
        standards_score = quality_report.get("standards", {}).get("compliance_score", 0)
        dependency_score = quality_report.get("dependencies", {}).get("dependency_quality_score", 0)
        maintainability_score = quality_report.get("maintainability", {}).get("maintainability_index", 0)
        
        # 计算综合质量分数
        overall_score = (complexity_score + standards_score + dependency_score + maintainability_score) / 4
        
        return {
            "overall_quality_score": round(overall_score, 2),
            "component_scores": {
                "complexity": round(complexity_score, 2),
                "standards": round(standards_score, 2),
                "dependencies": round(dependency_score, 2),
                "maintainability": round(maintainability_score, 2)
            },
            "quality_level": self._get_quality_level(overall_score),
            "recommendations_count": len(quality_report.get("complexity", {}).get("high_complexity_files", [])) + 
                                     len(quality_report.get("standards", {}).get("violations", []))
        }
    
    def _get_quality_level(self, score: float) -> str:
        """获取质量等级"""
        
        if score >= 90:
            return "excellent"
        elif score >= 80:
            return "good"
        elif score >= 70:
            return "fair"
        elif score >= 60:
            return "poor"
        else:
            return "critical"
    
    async def validate_input(self, input_data: Any) -> bool:
        """验证输入数据"""
        
        if not isinstance(input_data, dict):
            return False
        
        required_keys = ["code_metadata", "architecture_data"]
        return all(key in input_data for key in required_keys)
    
    def get_agent_info(self) -> Dict[str, Any]:
        """获取Agent信息"""
        
        return {
            "name": self.name,
            "version": "1.0.0",
            "description": "代码质量评估和重构建议Agent",
            "capabilities": [
                "代码复杂度分析",
                "代码规范检查",
                "依赖关系质量评估",
                "可维护性评估",
                "重构建议生成",
                "测试用例生成"
            ],
            "config": {
                "enable_test_generation": self.enable_test_generation,
                "complexity_threshold": self.complexity_threshold,
                "quality_metrics": self.quality_metrics
            }
        }