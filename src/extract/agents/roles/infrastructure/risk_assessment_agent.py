"""
风险评估和可视化Agent (RiskAssessmentAgent)

职责：分析架构风险，生成可视化报告和风险缓解建议
"""

from typing import Dict, List, Any, Tuple
import os
import json
import math
from pathlib import Path
from collections import defaultdict
from ..base.agent import BaseAgent
from ..base.context import AgentContext
from ..base.result import AgentResult
from ...config.agent_configs import RISK_ASSESSMENT_AGENT


class RiskAssessmentAgent(BaseAgent):
    """风险评估和可视化Agent
    
    专注于：
    1. 架构风险评估
    2. 技术债务识别
    3. 可视化报告生成
    4. 风险缓解建议
    """
    
    def __init__(self):
        super().__init__(DEPENDENCY_ANALYSIS_AGENT)
        self.risk_categories = {
            "dependency": "依赖风险",
            "complexity": "复杂度风险", 
            "maintainability": "可维护性风险",
            "security": "安全风险",
            "performance": "性能风险"
        }
    
    def execute(self, context: AgentContext) -> AgentResult:
        """执行风险评估和可视化分析"""
        
        project_root = context.project_root
        
        # 收集项目数据
        project_data = self._collect_project_data(project_root)
        
        # 分析各类风险
        risk_assessment = self._assess_risks(project_data)
        
        # 生成可视化报告
        output = self._generate_visualization_report(risk_assessment)
        
        return AgentResult(
            success=True,
            data=risk_assessment,
            output=output,
            metrics={
                "total_risks": len(risk_assessment["identified_risks"]),
                "high_risks": len([r for r in risk_assessment["identified_risks"] if r["severity"] == "high"]),
                "medium_risks": len([r for r in risk_assessment["identified_risks"] if r["severity"] == "medium"]),
                "low_risks": len([r for r in risk_assessment["identified_risks"] if r["severity"] == "low"]),
                "overall_risk_score": risk_assessment["overall_risk_score"]
            }
        )
    
    def _collect_project_data(self, project_root: str) -> Dict[str, Any]:
        """收集项目数据用于风险评估"""
        
        data = {
            "file_stats": self._analyze_file_statistics(project_root),
            "dependency_data": self._analyze_dependencies(project_root),
            "complexity_metrics": self._analyze_complexity(project_root),
            "code_quality": self._analyze_code_quality(project_root),
            "security_indicators": self._analyze_security_indicators(project_root)
        }
        
        return data
    
    def _analyze_file_statistics(self, project_root: str) -> Dict[str, Any]:
        """分析文件统计信息"""
        
        stats = {
            "total_files": 0,
            "file_types": defaultdict(int),
            "file_sizes": [],
            "large_files": [],
            "empty_files": []
        }
        
        for root, dirs, files in os.walk(project_root):
            # 跳过隐藏目录和特定目录
            if any(part.startswith('.') for part in Path(root).parts):
                continue
            
            for file in files:
                file_path = os.path.join(root, file)
                
                # 获取文件扩展名
                ext = Path(file).suffix.lower()
                stats["file_types"][ext] += 1
                stats["total_files"] += 1
                
                # 分析文件大小
                try:
                    file_size = os.path.getsize(file_path)
                    stats["file_sizes"].append(file_size)
                    
                    # 识别大文件
                    if file_size > 100 * 1024:  # 100KB
                        stats["large_files"].append({
                            "path": os.path.relpath(file_path, project_root),
                            "size_kb": file_size / 1024
                        })
                    
                    # 识别空文件
                    if file_size == 0:
                        stats["empty_files"].append(os.path.relpath(file_path, project_root))
                        
                except OSError:
                    continue
        
        # 计算平均文件大小
        if stats["file_sizes"]:
            stats["avg_file_size"] = sum(stats["file_sizes"]) / len(stats["file_sizes"])
            stats["max_file_size"] = max(stats["file_sizes"])
        else:
            stats["avg_file_size"] = 0
            stats["max_file_size"] = 0
        
        return stats
    
    def _analyze_dependencies(self, project_root: str) -> Dict[str, Any]:
        """分析依赖关系"""
        
        dependencies = {
            "external_deps": [],
            "outdated_deps": [],
            "vulnerable_deps": [],
            "duplicate_deps": []
        }
        
        # 分析Python依赖
        req_file = os.path.join(project_root, "requirements.txt")
        if os.path.exists(req_file):
            try:
                with open(req_file, 'r', encoding='utf-8') as f:
                    for line in f:
                        line = line.strip()
                        if line and not line.startswith('#'):
                            dep_info = self._parse_python_dependency(line)
                            if dep_info:
                                dependencies["external_deps"].append(dep_info)
            except Exception:
                pass
        
        # 分析JavaScript依赖
        package_file = os.path.join(project_root, "package.json")
        if os.path.exists(package_file):
            try:
                with open(package_file, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    
                    if "dependencies" in data:
                        for dep_name, version in data["dependencies"].items():
                            dependencies["external_deps"].append({
                                "name": dep_name,
                                "version": version,
                                "type": "javascript",
                                "source": "package.json"
                            })
            except Exception:
                pass
        
        return dependencies
    
    def _parse_python_dependency(self, line: str) -> Dict[str, str]:
        """解析Python依赖行"""
        
        line = line.strip()
        if not line or line.startswith('#'):
            return None
        
        # 简单的依赖解析
        if '==' in line:
            parts = line.split('==')
            return {
                "name": parts[0].strip(),
                "version": parts[1].strip(),
                "type": "python",
                "source": "requirements.txt"
            }
        elif '>=' in line:
            parts = line.split('>=')
            return {
                "name": parts[0].strip(),
                "version": parts[1].strip(),
                "type": "python",
                "source": "requirements.txt"
            }
        else:
            return {
                "name": line,
                "version": "unknown",
                "type": "python",
                "source": "requirements.txt"
            }
    
    def _analyze_complexity(self, project_root: str) -> Dict[str, Any]:
        """分析代码复杂度"""
        
        complexity = {
            "high_complexity_files": [],
            "long_methods": [],
            "deep_nesting": [],
            "code_duplication": []
        }
        
        # 分析Python文件复杂度
        for root, dirs, files in os.walk(project_root):
            if any(part.startswith('.') for part in Path(root).parts):
                continue
            
            for file in files:
                if file.endswith('.py'):
                    file_path = os.path.join(root, file)
                    file_complexity = self._analyze_python_complexity(file_path, project_root)
                    
                    if file_complexity["complexity_score"] > 0.7:  # 高复杂度阈值
                        complexity["high_complexity_files"].append(file_complexity)
        
        return complexity
    
    def _analyze_python_complexity(self, file_path: str, project_root: str) -> Dict[str, Any]:
        """分析Python文件复杂度"""
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 简化的复杂度分析
            lines = content.split('\n')
            total_lines = len(lines)
            
            # 计算函数数量
            function_count = content.count('def ')
            class_count = content.count('class ')
            
            # 计算嵌套深度（简化版）
            nesting_depth = self._estimate_nesting_depth(content)
            
            # 计算复杂度分数
            complexity_score = min(1.0, (function_count * 0.2 + class_count * 0.3 + nesting_depth * 0.5) / 10)
            
            return {
                "file_path": os.path.relpath(file_path, project_root),
                "total_lines": total_lines,
                "function_count": function_count,
                "class_count": class_count,
                "nesting_depth": nesting_depth,
                "complexity_score": complexity_score,
                "risk_level": "high" if complexity_score > 0.7 else "medium" if complexity_score > 0.4 else "low"
            }
            
        except Exception as e:
            return {
                "file_path": os.path.relpath(file_path, project_root),
                "error": str(e),
                "complexity_score": 0,
                "risk_level": "unknown"
            }
    
    def _estimate_nesting_depth(self, content: str) -> int:
        """估计代码嵌套深度"""
        
        max_depth = 0
        current_depth = 0
        
        for line in content.split('\n'):
            stripped = line.strip()
            
            # 增加嵌套深度的关键字
            if stripped.startswith(('if ', 'for ', 'while ', 'def ', 'class ', 'try:', 'with ')):
                current_depth += 1
                max_depth = max(max_depth, current_depth)
            
            # 减少嵌套深度的关键字
            if stripped.startswith(('return', 'break', 'continue', 'pass')):
                current_depth = max(0, current_depth - 1)
        
        return max_depth
    
    def _analyze_code_quality(self, project_root: str) -> Dict[str, Any]:
        """分析代码质量"""
        
        quality = {
            "code_smells": [],
            "maintainability_issues": [],
            "documentation_coverage": 0.0
        }
        
        # 分析Python代码质量
        total_files = 0
        documented_files = 0
        
        for root, dirs, files in os.walk(project_root):
            if any(part.startswith('.') for part in Path(root).parts):
                continue
            
            for file in files:
                if file.endswith('.py'):
                    file_path = os.path.join(root, file)
                    total_files += 1
                    
                    if self._has_documentation(file_path):
                        documented_files += 1
        
        if total_files > 0:
            quality["documentation_coverage"] = documented_files / total_files
        
        return quality
    
    def _has_documentation(self, file_path: str) -> bool:
        """检查文件是否有文档"""
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 检查是否有模块级文档字符串
            return '"""' in content or "'''" in content
        except Exception:
            return False
    
    def _analyze_security_indicators(self, project_root: str) -> Dict[str, Any]:
        """分析安全指标"""
        
        security = {
            "potential_vulnerabilities": [],
            "hardcoded_secrets": [],
            "insecure_configurations": []
        }
        
        # 检查常见的安全问题模式
        for root, dirs, files in os.walk(project_root):
            if any(part.startswith('.') for part in Path(root).parts):
                continue
            
            for file in files:
                if file.endswith(('.py', '.js', '.java', '.go')):
                    file_path = os.path.join(root, file)
                    self._check_file_security(file_path, project_root, security)
        
        return security
    
    def _check_file_security(self, file_path: str, project_root: str, security: Dict) -> None:
        """检查文件安全风险"""
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read().lower()
            
            relative_path = os.path.relpath(file_path, project_root)
            
            # 检查硬编码密码
            if any(keyword in content for keyword in ['password=', 'secret=', 'apikey=', 'token=']):
                security["hardcoded_secrets"].append({
                    "file": relative_path,
                    "issue": "潜在的硬编码密钥",
                    "severity": "high"
                })
            
            # 检查SQL注入风险
            if 'select *' in content and 'where' in content and not 'parameter' in content:
                security["potential_vulnerabilities"].append({
                    "file": relative_path,
                    "issue": "潜在的SQL注入风险",
                    "severity": "high"
                })
            
            # 检查eval使用
            if 'eval(' in content:
                security["potential_vulnerabilities"].append({
                    "file": relative_path,
                    "issue": "使用eval函数，存在安全风险",
                    "severity": "medium"
                })
                
        except Exception:
            pass
    
    def _assess_risks(self, project_data: Dict) -> Dict[str, Any]:
        """评估项目风险"""
        
        identified_risks = []
        
        # 依赖风险
        identified_risks.extend(self._assess_dependency_risks(project_data["dependency_data"]))
        
        # 复杂度风险
        identified_risks.extend(self._assess_complexity_risks(project_data["complexity_metrics"]))
        
        # 可维护性风险
        identified_risks.extend(self._assess_maintainability_risks(project_data["code_quality"]))
        
        # 安全风险
        identified_risks.extend(self._assess_security_risks(project_data["security_indicators"]))
        
        # 性能风险
        identified_risks.extend(self._assess_performance_risks(project_data["file_stats"]))
        
        # 计算总体风险分数
        overall_score = self._calculate_overall_risk_score(identified_risks)
        
        return {
            "identified_risks": identified_risks,
            "overall_risk_score": overall_score,
            "risk_distribution": self._calculate_risk_distribution(identified_risks),
            "top_risks": sorted(identified_risks, key=lambda x: x.get('severity_score', 0), reverse=True)[:10]
        }
    
    def _assess_dependency_risks(self, dependency_data: Dict) -> List[Dict]:
        """评估依赖风险"""
        
        risks = []
        
        # 外部依赖数量风险
        external_deps_count = len(dependency_data["external_deps"])
        if external_deps_count > 50:
            risks.append({
                "category": "dependency",
                "type": "high_external_dependencies",
                "severity": "high",
                "severity_score": 0.8,
                "description": f"项目有 {external_deps_count} 个外部依赖，依赖过多会增加维护难度和安全风险",
                "recommendation": "考虑减少不必要的依赖，定期审查依赖使用情况"
            })
        
        return risks
    
    def _assess_complexity_risks(self, complexity_data: Dict) -> List[Dict]:
        """评估复杂度风险"""
        
        risks = []
        
        # 高复杂度文件风险
        high_complexity_count = len(complexity_data["high_complexity_files"])
        if high_complexity_count > 5:
            risks.append({
                "category": "complexity",
                "type": "high_complexity_files",
                "severity": "medium",
                "severity_score": 0.6,
                "description": f"发现 {high_complexity_count} 个高复杂度文件，可能影响代码可维护性",
                "recommendation": "重构高复杂度文件，提取公共功能，降低单个文件的复杂度"
            })
        
        return risks
    
    def _assess_maintainability_risks(self, quality_data: Dict) -> List[Dict]:
        """评估可维护性风险"""
        
        risks = []
        
        # 文档覆盖率风险
        doc_coverage = quality_data["documentation_coverage"]
        if doc_coverage < 0.3:
            risks.append({
                "category": "maintainability",
                "type": "low_documentation_coverage",
                "severity": "medium",
                "severity_score": 0.5,
                "description": f"文档覆盖率仅为 {doc_coverage:.1%}，可能影响代码可维护性",
                "recommendation": "增加代码文档，特别是关键业务逻辑和复杂算法的文档"
            })
        
        return risks
    
    def _assess_security_risks(self, security_data: Dict) -> List[Dict]:
        """评估安全风险"""
        
        risks = []
        
        # 硬编码密钥风险
        if security_data["hardcoded_secrets"]:
            risks.append({
                "category": "security",
                "type": "hardcoded_secrets",
                "severity": "high",
                "severity_score": 0.9,
                "description": f"发现 {len(security_data['hardcoded_secrets'])} 个潜在的硬编码密钥问题",
                "recommendation": "使用环境变量或配置文件存储敏感信息，避免硬编码"
            })
        
        # 潜在漏洞风险
        if security_data["potential_vulnerabilities"]:
            risks.append({
                "category": "security",
                "type": "potential_vulnerabilities",
                "severity": "medium",
                "severity_score": 0.7,
                "description": f"发现 {len(security_data['potential_vulnerabilities'])} 个潜在的安全漏洞",
                "recommendation": "进行安全代码审查，修复潜在的安全问题"
            })
        
        return risks
    
    def _assess_performance_risks(self, file_stats: Dict) -> List[Dict]:
        """评估性能风险"""
        
        risks = []
        
        # 大文件风险
        if file_stats["large_files"]:
            risks.append({
                "category": "performance",
                "type": "large_files",
                "severity": "low",
                "severity_score": 0.3,
                "description": f"发现 {len(file_stats['large_files'])} 个大文件，可能影响加载性能",
                "recommendation": "考虑拆分大文件，优化资源加载"
            })
        
        return risks
    
    def _calculate_overall_risk_score(self, risks: List[Dict]) -> float:
        """计算总体风险分数"""
        
        if not risks:
            return 0.0
        
        severity_scores = {
            "high": 0.8,
            "medium": 0.5,
            "low": 0.2
        }
        
        total_score = sum(severity_scores.get(risk["severity"], 0) for risk in risks)
        normalized_score = min(1.0, total_score / (len(risks) * 0.8))  # 归一化到0-1
        
        return normalized_score
    
    def _calculate_risk_distribution(self, risks: List[Dict]) -> Dict[str, int]:
        """计算风险分布"""
        
        distribution = defaultdict(int)
        
        for risk in risks:
            distribution[risk["category"]] += 1
        
        return dict(distribution)
    
    def _generate_visualization_report(self, risk_assessment: Dict) -> str:
        """生成可视化风险评估报告"""
        
        report_lines = []
        
        report_lines.append("# 架构风险评估与可视化报告")
        report_lines.append("")
        
        # 总体风险概览
        overall_score = risk_assessment["overall_risk_score"]
        risk_level = self._get_risk_level(overall_score)
        
        report_lines.append("## 总体风险评估")
        report_lines.append("")
        report_lines.append(f"**风险分数:** {overall_score:.2f}/1.0")
        report_lines.append(f"**风险等级:** {risk_level}")
        report_lines.append(f"**发现风险数量:** {len(risk_assessment['identified_risks'])}")
        report_lines.append("")
        
        # 风险分布饼图
        distribution = risk_assessment["risk_distribution"]
        if distribution:
            report_lines.append("## 风险分布")
            report_lines.append("")
            report_lines.append("```mermaid")
            report_lines.append("pie title 风险类别分布")
            
            for category, count in distribution.items():
                category_name = self.risk_categories.get(category, category)
                report_lines.append(f'    "{category_name}" : {count}')
            
            report_lines.append("```")
            report_lines.append("")
        
        # 风险雷达图
        report_lines.append("## 风险维度分析")
        report_lines.append("")
        report_lines.append("```mermaid")
        report_lines.append("radar-beta")
        report_lines.append("  title 风险维度雷达图")
        report_lines.append("  axis reliability [可靠性, 性能, 安全性, 可维护性, 复杂度]")
        
        # 计算各维度分数（简化版）
        dimension_scores = self._calculate_dimension_scores(risk_assessment["identified_risks"])
        
        report_lines.append("  reliability [" + ", ".join(str(score) for score in dimension_scores) + "]")
        report_lines.append("  max 5")
        report_lines.append("```")
        report_lines.append("")
        
        # 高风险列表
        top_risks = risk_assessment["top_risks"]
        if top_risks:
            report_lines.append("## 高风险项")
            report_lines.append("")
            report_lines.append("| 风险类别 | 风险类型 | 严重程度 | 描述 | 建议 |")
            report_lines.append("|----------|----------|----------|------|------|")
            
            for risk in top_risks:
                category_name = self.risk_categories.get(risk["category"], risk["category"])
                severity_emoji = {"high": "🔴", "medium": "🟡", "low": "🟢"}.get(risk["severity"], "⚪")
                
                report_lines.append(f"| {category_name} | {risk['type']} | {severity_emoji} {risk['severity']} | {risk['description'][:50]}... | {risk['recommendation'][:30]}... |")
            
            report_lines.append("")
        
        # 风险趋势图
        report_lines.append("## 风险趋势分析")
        report_lines.append("")
        report_lines.append("```mermaid")
        report_lines.append("xychart-beta")
        report_lines.append("    title "风险趋势预测"")
        report_lines.append("    x-axis [当前, 3个月后, 6个月后, 1年后]")
        report_lines.append("    y-axis "风险分数" 0 --> 1")
        
        # 预测风险趋势（简化版）
        current_score = overall_score
        trend_scores = self._predict_risk_trend(current_score, len(risk_assessment["identified_risks"]))
        
        report_lines.append(f"    line [{current_score:.2f}, {trend_scores[0]:.2f}, {trend_scores[1]:.2f}, {trend_scores[2]:.2f}]")
        report_lines.append("```")
        report_lines.append("")
        
        # 风险缓解路线图
        report_lines.append("## 风险缓解路线图")
        report_lines.append("")
        report_lines.append("```mermaid")
        report_lines.append("timeline")
        report_lines.append("    title 风险缓解计划")
        report_lines.append("    section 立即处理 (高危风险)")
        report_lines.append("        安全风险修复 : 修复硬编码密钥等安全问题")
        report_lines.append("    section 短期计划 (1-3个月)")
        report_lines.append("        复杂度优化 : 重构高复杂度文件")
        report_lines.append("        依赖管理 : 优化依赖关系")
        report_lines.append("    section 中期计划 (3-6个月)")
        report_lines.append("        文档完善 : 提升文档覆盖率")
        report_lines.append("        性能优化 : 处理大文件等问题")
        report_lines.append("    section 长期计划 (6个月以上)")
        report_lines.append("        架构优化 : 持续架构改进")
        report_lines.append("```")
        report_lines.append("")
        
        # 总结和建议
        report_lines.append("## 总结与建议")
        report_lines.append("")
        
        if risk_level == "高危":
            report_lines.append("🔴 **项目处于高危状态，建议立即采取行动：**")
            report_lines.append("- 优先处理安全风险")
            report_lines.append("- 制定紧急修复计划")
            report_lines.append("- 加强代码审查")
        elif risk_level == "中危":
            report_lines.append("🟡 **项目存在中等风险，需要关注：**")
            report_lines.append("- 制定风险缓解计划")
            report_lines.append("- 定期进行风险评估")
            report_lines.append("- 逐步优化架构问题")
        else:
            report_lines.append("🟢 **项目风险较低，继续保持：**")
            report_lines.append("- 定期监控风险指标")
            report_lines.append("- 持续改进代码质量")
            report_lines.append("- 建立预防机制")
        
        return "\n".join(report_lines)
    
    def _get_risk_level(self, score: float) -> str:
        """根据分数获取风险等级"""
        
        if score >= 0.7:
            return "高危"
        elif score >= 0.4:
            return "中危"
        else:
            return "低危"
    
    def _calculate_dimension_scores(self, risks: List[Dict]) -> List[float]:
        """计算各维度风险分数（简化版）"""
        
        # 简化的维度分数计算
        dimension_map = {
            "reliability": ["dependency", "performance"],
            "performance": ["performance"],
            "security": ["security"],
            "maintainability": ["maintainability"],
            "complexity": ["complexity"]
        }
        
        scores = []
        for dimension, categories in dimension_map.items():
            dimension_risks = [r for r in risks if r["category"] in categories]
            if dimension_risks:
                avg_severity = sum(0.8 if r["severity"] == "high" else 0.5 if r["severity"] == "medium" else 0.2 
                                 for r in dimension_risks) / len(dimension_risks)
                scores.append(min(5, int(avg_severity * 5)))
            else:
                scores.append(1)  # 最低分数
        
        return scores
    
    def _predict_risk_trend(self, current_score: float, risk_count: int) -> Tuple[float, float, float]:
        """预测风险趋势（简化版）"""
        
        # 简化的趋势预测逻辑
        if risk_count > 10:
            # 高风险项目，趋势可能恶化
            return [
                min(1.0, current_score * 1.1),  # 3个月后
                min(1.0, current_score * 1.2),  # 6个月后
                min(1.0, current_score * 1.3)   # 1年后
            ]
        elif risk_count > 5:
            # 中等风险，保持稳定
            return [current_score, current_score, current_score]
        else:
            # 低风险，可能改善
            return [
                max(0, current_score * 0.9),  # 3个月后
                max(0, current_score * 0.8),  # 6个月后
                max(0, current_score * 0.7)   # 1年后
            ]