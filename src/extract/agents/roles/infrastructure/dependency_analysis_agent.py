"""
依赖分析与架构逆向Agent (DependencyAnalysisAgent)

职责：深度分析项目依赖关系，支持架构逆向工程和依赖可视化
"""

from typing import Dict, List, Any, Set, Tuple
import os
import json
import ast
import re
from pathlib import Path
from collections import defaultdict, deque
from ..base.agent import BaseAgent
from ..base.context import AgentContext
from ..base.result import AgentResult
from ...config.agent_configs import DEPENDENCY_ANALYSIS_AGENT


class DependencyAnalysisAgent(BaseAgent):
    """依赖分析与架构逆向Agent
    
    专注于：
    1. 深度依赖关系分析
    2. 架构逆向工程
    3. 依赖图谱生成
    4. 架构风险识别
    """
    
    def __init__(self):
        super().__init__(DEPENDENCIES_AGENT)
        self.dependency_graph = defaultdict(list)
        self.reverse_dependency_graph = defaultdict(list)
        self.module_mapping = {}
        self.cyclical_dependencies = []
    
    def execute(self, context: AgentContext) -> AgentResult:
        """执行依赖关系深度分析"""
        
        project_root = context.project_root
        
        # 构建依赖图谱
        dependency_data = self._build_dependency_graph(project_root)
        
        # 分析依赖关系
        analysis_results = self._analyze_dependencies(dependency_data)
        
        # 生成架构逆向报告
        output = self._generate_architecture_report(analysis_results)
        
        return AgentResult(
            success=True,
            data=analysis_results,
            output=output,
            metrics={
                "total_modules": len(dependency_data["modules"]),
                "total_dependencies": len(dependency_data["dependencies"]),
                "cyclical_dependencies_count": len(analysis_results["cyclical_dependencies"]),
                "high_risk_dependencies": len(analysis_results["high_risk_dependencies"]),
                "architecture_clusters": len(analysis_results["architecture_clusters"])
            }
        )
    
    def _build_dependency_graph(self, project_root: str) -> Dict[str, Any]:
        """构建项目依赖图谱"""
        
        modules = self._discover_modules(project_root)
        dependencies = []
        
        for module_path, module_info in modules.items():
            # 分析模块依赖
            module_deps = self._analyze_module_dependencies(module_path, module_info)
            dependencies.extend(module_deps)
            
            # 构建依赖图谱
            for dep in module_deps:
                self.dependency_graph[module_info["module_id"]].append(dep["target_module_id"])
                self.reverse_dependency_graph[dep["target_module_id"]].append(module_info["module_id"])
        
        return {
            "modules": modules,
            "dependencies": dependencies,
            "project_root": project_root
        }
    
    def _discover_modules(self, project_root: str) -> Dict[str, Dict]:
        """发现项目中的所有模块"""
        
        modules = {}
        module_id_counter = 0
        
        # 分析Python模块
        for root, dirs, files in os.walk(project_root):
            # 跳过隐藏目录和测试目录
            if any(part.startswith('.') for part in Path(root).parts):
                continue
            if 'test' in root.lower() or 'tests' in root.lower():
                continue
            
            for file in files:
                if file.endswith('.py'):
                    module_path = os.path.join(root, file)
                    module_info = self._analyze_python_module(module_path, project_root)
                    if module_info:
                        module_id = f"m{module_id_counter}"
                        module_info["module_id"] = module_id
                        modules[module_path] = module_info
                        self.module_mapping[module_id] = module_info
                        module_id_counter += 1
        
        return modules
    
    def _analyze_python_module(self, module_path: str, project_root: str) -> Dict[str, Any]:
        """分析Python模块"""
        
        try:
            with open(module_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 解析Python文件
            tree = ast.parse(content)
            
            # 提取模块基本信息
            module_info = {
                "file_path": module_path,
                "relative_path": os.path.relpath(module_path, project_root),
                "language": "python",
                "classes": [],
                "functions": [],
                "imports": [],
                "complexity": 0
            }
            
            # 分析导入语句
            for node in ast.walk(tree):
                if isinstance(node, ast.Import):
                    for alias in node.names:
                        module_info["imports"].append({
                            "type": "import",
                            "module": alias.name,
                            "alias": alias.asname
                        })
                elif isinstance(node, ast.ImportFrom):
                    module_name = node.module or ""
                    for alias in node.names:
                        module_info["imports"].append({
                            "type": "from_import",
                            "module": module_name,
                            "name": alias.name,
                            "alias": alias.asname
                        })
                elif isinstance(node, ast.ClassDef):
                    module_info["classes"].append(node.name)
                elif isinstance(node, ast.FunctionDef):
                    module_info["functions"].append(node.name)
            
            # 计算复杂度（简化版：函数数量 + 类数量）
            module_info["complexity"] = len(module_info["functions"]) + len(module_info["classes"])
            
            return module_info
            
        except Exception as e:
            print(f"Error analyzing {module_path}: {e}")
            return None
    
    def _analyze_module_dependencies(self, module_path: str, module_info: Dict) -> List[Dict]:
        """分析模块的依赖关系"""
        
        dependencies = []
        project_root = Path(module_info["file_path"]).parent
        
        for import_info in module_info["imports"]:
            # 分析是否是内部依赖
            target_module = self._resolve_import_target(import_info, project_root)
            if target_module:
                dependency_info = {
                    "source_module_id": module_info["module_id"],
                    "target_module_id": target_module["module_id"],
                    "type": "internal",
                    "import_type": import_info["type"],
                    "import_path": import_info.get("module", ""),
                    "specific_element": import_info.get("name", "")
                }
                dependencies.append(dependency_info)
        
        return dependencies
    
    def _resolve_import_target(self, import_info: Dict, project_root: Path) -> Dict:
        """解析导入目标模块"""
        
        # 简化的内部模块解析逻辑
        # 在实际实现中，这里需要更复杂的路径解析逻辑
        
        # 检查是否是标准库或第三方库
        if self._is_standard_library(import_info.get("module", "")):
            return None
        
        # 检查是否是项目内部模块
        module_path = self._find_module_in_project(import_info, project_root)
        if module_path and module_path in self.module_mapping:
            return self.module_mapping[module_path]
        
        return None
    
    def _is_standard_library(self, module_name: str) -> bool:
        """检查是否是标准库"""
        
        standard_libs = {
            'os', 'sys', 'json', 're', 'datetime', 'collections',
            'itertools', 'functools', 'pathlib', 'typing'
        }
        
        if not module_name:
            return False
        
        first_part = module_name.split('.')[0]
        return first_part in standard_libs
    
    def _find_module_in_project(self, import_info: Dict, project_root: Path) -> str:
        """在项目中查找模块"""
        
        # 简化的模块查找逻辑
        # 在实际实现中，这里需要更复杂的路径解析和模块查找
        
        module_name = import_info.get("module", "")
        if not module_name:
            return None
        
        # 将模块名转换为文件路径
        module_path = module_name.replace('.', os.sep) + '.py'
        full_path = project_root / module_path
        
        if full_path.exists():
            return str(full_path)
        
        return None
    
    def _analyze_dependencies(self, dependency_data: Dict) -> Dict[str, Any]:
        """深度分析依赖关系"""
        
        # 检测循环依赖
        cyclical_deps = self._detect_cyclical_dependencies()
        
        # 识别高风险依赖
        high_risk_deps = self._identify_high_risk_dependencies()
        
        # 分析架构聚类
        architecture_clusters = self._cluster_modules_by_architecture()
        
        # 分析依赖强度
        dependency_strength = self._analyze_dependency_strength()
        
        return {
            "cyclical_dependencies": cyclical_deps,
            "high_risk_dependencies": high_risk_deps,
            "architecture_clusters": architecture_clusters,
            "dependency_strength": dependency_strength,
            "dependency_graph": dict(self.dependency_graph),
            "reverse_dependency_graph": dict(self.reverse_dependency_graph)
        }
    
    def _detect_cyclical_dependencies(self) -> List[List[str]]:
        """检测循环依赖"""
        
        cyclical = []
        visited = set()
        
        for module_id in self.dependency_graph:
            if module_id not in visited:
                cycle = self._find_cycle(module_id, set(), visited)
                if cycle:
                    cyclical.append(cycle)
        
        return cyclical
    
    def _find_cycle(self, current: str, path: Set[str], visited: Set[str]) -> List[str]:
        """查找循环依赖"""
        
        if current in path:
            # 找到循环
            cycle_start = list(path).index(current)
            return list(path)[cycle_start:]
        
        if current in visited:
            return None
        
        path.add(current)
        visited.add(current)
        
        for neighbor in self.dependency_graph.get(current, []):
            cycle = self._find_cycle(neighbor, path.copy(), visited)
            if cycle:
                return cycle
        
        return None
    
    def _identify_high_risk_dependencies(self) -> List[Dict]:
        """识别高风险依赖"""
        
        high_risk = []
        
        for module_id, deps in self.dependency_graph.items():
            # 高依赖度模块
            if len(deps) > 10:
                high_risk.append({
                    "module_id": module_id,
                    "risk_type": "high_dependency_count",
                    "dependency_count": len(deps),
                    "description": f"模块依赖过多其他模块，可能存在设计问题"
                })
            
            # 被过度依赖的模块
            reverse_count = len(self.reverse_dependency_graph.get(module_id, []))
            if reverse_count > 15:
                high_risk.append({
                    "module_id": module_id,
                    "risk_type": "high_reverse_dependency",
                    "reverse_dependency_count": reverse_count,
                    "description": f"模块被过多其他模块依赖，可能存在单点故障风险"
                })
        
        return high_risk
    
    def _cluster_modules_by_architecture(self) -> List[Dict]:
        """根据架构模式聚类模块"""
        
        # 简化的聚类算法
        # 在实际实现中，这里可以使用更复杂的聚类算法
        
        clusters = []
        visited = set()
        
        for module_id, module_info in self.module_mapping.items():
            if module_id not in visited:
                cluster = self._find_connected_components(module_id, visited)
                if cluster:
                    clusters.append({
                        "cluster_id": f"cluster_{len(clusters)}",
                        "modules": cluster,
                        "size": len(cluster),
                        "description": self._describe_cluster(cluster)
                    })
        
        return clusters
    
    def _find_connected_components(self, start: str, visited: Set[str]) -> List[str]:
        """查找连通组件"""
        
        cluster = []
        queue = deque([start])
        
        while queue:
            current = queue.popleft()
            if current not in visited:
                visited.add(current)
                cluster.append(current)
                
                # 添加直接依赖的模块
                for dep in self.dependency_graph.get(current, []):
                    if dep not in visited:
                        queue.append(dep)
                
                # 添加依赖当前模块的模块
                for reverse_dep in self.reverse_dependency_graph.get(current, []):
                    if reverse_dep not in visited:
                        queue.append(reverse_dep)
        
        return cluster
    
    def _describe_cluster(self, cluster: List[str]) -> str:
        """描述集群特征"""
        
        if len(cluster) == 1:
            return "独立模块"
        elif len(cluster) <= 3:
            return "小型模块组"
        elif len(cluster) <= 10:
            return "中型模块组"
        else:
            return "大型模块组"
    
    def _analyze_dependency_strength(self) -> Dict[str, Any]:
        """分析依赖强度"""
        
        total_dependencies = sum(len(deps) for deps in self.dependency_graph.values())
        total_modules = len(self.module_mapping)
        
        return {
            "average_dependencies_per_module": total_dependencies / total_modules if total_modules > 0 else 0,
            "max_dependencies": max((len(deps) for deps in self.dependency_graph.values()), default=0),
            "min_dependencies": min((len(deps) for deps in self.dependency_graph.values()), default=0),
            "dependency_density": total_dependencies / (total_modules * (total_modules - 1)) if total_modules > 1 else 0
        }
    
    def _generate_architecture_report(self, analysis_results: Dict) -> str:
        """生成架构逆向报告"""
        
        report_lines = []
        
        report_lines.append("# 架构依赖分析与逆向工程报告")
        report_lines.append("")
        
        # 总体统计
        report_lines.append("## 总体统计")
        report_lines.append("")
        report_lines.append(f"- 分析模块数量: **{len(self.module_mapping)}**")
        report_lines.append(f"- 总依赖关系: **{sum(len(deps) for deps in self.dependency_graph.values())}**")
        report_lines.append(f"- 循环依赖数量: **{len(analysis_results['cyclical_dependencies'])}**")
        report_lines.append(f"- 高风险依赖: **{len(analysis_results['high_risk_dependencies'])}**")
        report_lines.append(f"- 架构聚类: **{len(analysis_results['architecture_clusters'])}**")
        report_lines.append("")
        
        # 循环依赖分析
        if analysis_results["cyclical_dependencies"]:
            report_lines.append("## 循环依赖检测")
            report_lines.append("")
            report_lines.append("发现以下循环依赖关系:")
            report_lines.append("")
            
            for i, cycle in enumerate(analysis_results["cyclical_dependencies"], 1):
                report_lines.append(f"**循环依赖 {i}:**")
                cycle_modules = [self.module_mapping[module_id]["relative_path"] for module_id in cycle]
                report_lines.append(" → ".join(cycle_modules))
                report_lines.append("")
        
        # 高风险依赖
        if analysis_results["high_risk_dependencies"]:
            report_lines.append("## 高风险依赖识别")
            report_lines.append("")
            report_lines.append("| 模块路径 | 风险类型 | 风险描述 |")
            report_lines.append("|----------|----------|----------|")
            
            for risk in analysis_results["high_risk_dependencies"]:
                module_info = self.module_mapping.get(risk["module_id"], {})
                module_path = module_info.get("relative_path", "unknown")
                
                report_lines.append(f"| {module_path} | {risk['risk_type']} | {risk['description']} |")
            report_lines.append("")
        
        # 架构聚类
        report_lines.append("## 架构聚类分析")
        report_lines.append("")
        
        for cluster in analysis_results["architecture_clusters"]:
            report_lines.append(f"### 集群 {cluster['cluster_id']} ({cluster['description']})")
            report_lines.append(f"- 模块数量: {cluster['size']}")
            
            # 显示集群中的主要模块
            sample_modules = cluster["modules"][:5]  # 限制显示数量
            module_paths = [self.module_mapping[module_id]["relative_path"] for module_id in sample_modules]
            
            report_lines.append("- 主要模块:")
            for path in module_paths:
                report_lines.append(f"  - {path}")
            
            if cluster["size"] > 5:
                report_lines.append(f"  - ... 还有 {cluster['size'] - 5} 个模块")
            
            report_lines.append("")
        
        # 依赖强度分析
        strength = analysis_results["dependency_strength"]
        report_lines.append("## 依赖强度分析")
        report_lines.append("")
        report_lines.append(f"- 平均依赖数/模块: **{strength['average_dependencies_per_module']:.2f}**")
        report_lines.append(f"- 最大依赖数: **{strength['max_dependencies']}**")
        report_lines.append(f"- 最小依赖数: **{strength['min_dependencies']}**")
        report_lines.append(f"- 依赖密度: **{strength['dependency_density']:.4f}**")
        report_lines.append("")
        
        # 依赖图谱可视化
        report_lines.append("## 依赖关系图谱")
        report_lines.append("")
        report_lines.append("```mermaid")
        report_lines.append("graph TD")
        report_lines.append("")
        
        # 添加集群节点
        for cluster in analysis_results["architecture_clusters"][:3]:  # 限制显示数量
            cluster_id = cluster["cluster_id"]
            report_lines.append(f"    subgraph {cluster_id}")
            
            for module_id in cluster["modules"][:5]:  # 每个集群限制显示5个模块
                module_info = self.module_mapping.get(module_id, {})
                module_name = Path(module_info.get("relative_path", "unknown")).stem
                report_lines.append(f"        {module_id}[{module_name}]")
            
            report_lines.append("    end")
            report_lines.append("")
        
        # 添加主要依赖关系
        added_edges = set()
        for source, targets in list(self.dependency_graph.items())[:10]:  # 限制边数量
            for target in targets[:3]:  # 每个源模块限制3个目标
                edge = f"{source}-->{target}"
                if edge not in added_edges:
                    report_lines.append(f"    {source} --> {target}")
                    added_edges.add(edge)
        
        report_lines.append("```")
        report_lines.append("")
        
        # 架构建议
        report_lines.append("## 架构优化建议")
        report_lines.append("")
        
        if analysis_results["cyclical_dependencies"]:
            report_lines.append("### 解决循环依赖")
            report_lines.append("- 引入依赖注入或事件驱动机制打破循环")
            report_lines.append("- 提取公共功能到独立模块")
            report_lines.append("- 使用接口抽象降低耦合度")
            report_lines.append("")
        
        if analysis_results["high_risk_dependencies"]:
            report_lines.append("### 降低高风险依赖")
            report_lines.append("- 对高依赖度模块进行功能拆分")
            report_lines.append("- 对高被依赖模块进行抽象和接口化")
            report_lines.append("- 引入中间层降低直接依赖")
            report_lines.append("")
        
        report_lines.append("### 架构优化策略")
        report_lines.append("- 根据聚类结果识别潜在的业务边界")
        report_lines.append("- 考虑将大型集群拆分为微服务")
        report_lines.append("- 建立清晰的依赖规则和架构约束")
        
        return "\n".join(report_lines)