"""
依赖关系分析Agent (DependenciesAgent)

职责：分析项目的依赖关系，包括内部依赖、外部依赖和依赖边界
"""

from typing import Dict, List, Any
import os
import json
from pathlib import Path
from ..base.agent import BaseAgent
from ..base.context import AgentContext
from ..base.result import AgentResult
from ...config.agent_configs import DEPENDENCIES_AGENT


class DependenciesAgent(BaseAgent):
    """依赖关系分析Agent
    
    专注于分析：
    1. 内部模块依赖关系
    2. 外部库依赖关系
    3. 依赖边界和技术隔离
    4. 依赖优化建议
    """
    
    def __init__(self):
        super().__init__(DEPENDENCIES_AGENT)
    
    def execute(self, context: AgentContext) -> AgentResult:
        """执行依赖关系分析"""
        
        # 获取项目根目录
        project_root = context.project_root
        
        # 分析各种依赖关系
        dependencies_data = self._analyze_dependencies(project_root)
        
        # 生成输出
        output = self._format_dependencies_report(dependencies_data)
        
        return AgentResult(
            success=True,
            data=dependencies_data,
            output=output,
            metrics={
                "internal_dependencies_count": len(dependencies_data["internal_dependencies"]),
                "external_dependencies_count": len(dependencies_data["external_dependencies"]),
                "dependency_boundaries_count": len(dependencies_data["dependency_boundaries"])
            }
        )
    
    def _analyze_dependencies(self, project_root: str) -> Dict[str, Any]:
        """分析项目的依赖关系"""
        
        dependencies = {
            "internal_dependencies": [],     # 内部模块依赖
            "external_dependencies": [],     # 外部库依赖
            "dependency_boundaries": [],     # 依赖边界
            "dependency_issues": [],         # 依赖问题
            "optimization_suggestions": []   # 优化建议
        }
        
        # 分析Python依赖
        if os.path.exists(os.path.join(project_root, "requirements.txt")):
            self._analyze_python_dependencies(project_root, dependencies)
        
        # 分析JavaScript依赖
        if os.path.exists(os.path.join(project_root, "package.json")):
            self._analyze_javascript_dependencies(project_root, dependencies)
        
        # 分析Go依赖
        if os.path.exists(os.path.join(project_root, "go.mod")):
            self._analyze_go_dependencies(project_root, dependencies)
        
        # 分析Java依赖
        if os.path.exists(os.path.join(project_root, "pom.xml")):
            self._analyze_java_dependencies(project_root, dependencies)
        
        # 分析C#依赖
        if os.path.exists(os.path.join(project_root, "*.csproj")):
            self._analyze_csharp_dependencies(project_root, dependencies)
        
        # 分析项目内部模块依赖
        self._analyze_internal_module_dependencies(project_root, dependencies)
        
        return dependencies
    
    def _analyze_python_dependencies(self, project_root: str, dependencies: Dict[str, Any]) -> None:
        """分析Python依赖"""
        req_file = os.path.join(project_root, "requirements.txt")
        
        try:
            with open(req_file, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#'):
                        # 解析依赖项
                        dep_info = {
                            "name": self._extract_package_name(line),
                            "version": self._extract_version(line),
                            "type": "python",
                            "source": "requirements.txt",
                            "category": self._classify_python_dependency(line)
                        }
                        dependencies["external_dependencies"].append(dep_info)
        except FileNotFoundError:
            pass
    
    def _analyze_javascript_dependencies(self, project_root: str, dependencies: Dict[str, Any]) -> None:
        """分析JavaScript依赖"""
        package_file = os.path.join(project_root, "package.json")
        
        try:
            with open(package_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                
                # 分析依赖
                if "dependencies" in data:
                    for dep_name, dep_version in data["dependencies"].items():
                        dep_info = {
                            "name": dep_name,
                            "version": dep_version,
                            "type": "javascript",
                            "source": "package.json",
                            "category": self._classify_javascript_dependency(dep_name)
                        }
                        dependencies["external_dependencies"].append(dep_info)
                
                # 分析开发依赖
                if "devDependencies" in data:
                    for dep_name, dep_version in data["devDependencies"].items():
                        dep_info = {
                            "name": dep_name,
                            "version": dep_version,
                            "type": "javascript",
                            "source": "package.json (dev)",
                            "category": "development"
                        }
                        dependencies["external_dependencies"].append(dep_info)
        except FileNotFoundError:
            pass
    
    def _analyze_go_dependencies(self, project_root: str, dependencies: Dict[str, Any]) -> None:
        """分析Go依赖"""
        go_mod_file = os.path.join(project_root, "go.mod")
        
        try:
            with open(go_mod_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()
                for line in lines:
                    if line.startswith('require'):
                        parts = line.strip().split()
                        if len(parts) >= 3:
                            dep_info = {
                                "name": parts[1],
                                "version": parts[2],
                                "type": "go",
                                "source": "go.mod",
                                "category": "runtime"
                            }
                            dependencies["external_dependencies"].append(dep_info)
        except FileNotFoundError:
            pass
    
    def _analyze_java_dependencies(self, project_root: str, dependencies: Dict[str, Any]) -> None:
        """分析Java依赖"""
        pom_file = os.path.join(project_root, "pom.xml")
        
        try:
            with open(pom_file, 'r', encoding='utf-8') as f:
                content = f.read()
                
                # 简单的XML解析（简化版）
                import re
                # 查找dependency标签
                dependencies_pattern = r'<dependency>.*?</dependency>'
                matches = re.findall(dependencies_pattern, content, re.DOTALL)
                
                for match in matches:
                    # 提取groupId、artifactId、version
                    group_id = re.search(r'<groupId>(.*?)</groupId>', match)
                    artifact_id = re.search(r'<artifactId>(.*?)</artifactId>', match)
                    version = re.search(r'<version>(.*?)</version>', match)
                    
                    if artifact_id:
                        dep_info = {
                            "name": artifact_id.group(1),
                            "group_id": group_id.group(1) if group_id else "unknown",
                            "version": version.group(1) if version else "unknown",
                            "type": "java",
                            "source": "pom.xml",
                            "category": "runtime"
                        }
                        dependencies["external_dependencies"].append(dep_info)
        except FileNotFoundError:
            pass
    
    def _analyze_csharp_dependencies(self, project_root: str, dependencies: Dict[str, Any]) -> None:
        """分析C#依赖"""
        # 查找.csproj文件
        csproj_files = []
        for root, dirs, files in os.walk(project_root):
            for file in files:
                if file.endswith('.csproj'):
                    csproj_files.append(os.path.join(root, file))
        
        if csproj_files:
            for csproj_file in csproj_files[:1]:  # 只分析第一个文件
                try:
                    with open(csproj_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                        
                        # 简单的PackageReference解析
                        import re
                        package_ref_pattern = r'<PackageReference Include="(.*?)" Version="(.*?)"'
                        matches = re.findall(package_ref_pattern, content)
                        
                        for match in matches:
                            dep_info = {
                                "name": match[0],
                                "version": match[1],
                                "type": "csharp",
                                "source": csproj_file,
                                "category": "runtime"
                            }
                            dependencies["external_dependencies"].append(dep_info)
                except Exception:
                    pass
    
    def _analyze_internal_module_dependencies(self, project_root: str, dependencies: Dict[str, Any]) -> None:
        """分析内部模块依赖关系"""
        
        # 分析Python项目
        python_modules = []
        for root, dirs, files in os.walk(project_root):
            for file in files:
                if file.endswith('.py'):
                    python_modules.append(os.path.join(root, file))
        
        if python_modules:
            # 简化分析：基于目录结构推测依赖
            for module in python_modules[:10]:  # 限制数量
                module_path = Path(module)
                module_name = module_path.name
                module_dir = module_path.parent
                
                # 检查import语句
                try:
                    with open(module, 'r', encoding='utf-8') as f:
                        lines = f.readlines()
                        imports = []
                        for line in lines:
                            if line.strip().startswith('import'):
                                imports.append(line.strip())
                            elif line.strip().startswith('from'):
                                imports.append(line.strip())
                        
                        if imports:
                            dep_info = {
                                "module": module_name,
                                "module_path": str(module_path),
                                "imports": imports,
                                "dependency_count": len(imports),
                                "complexity": len(imports) > 5
                            }
                            dependencies["internal_dependencies"].append(dep_info)
                except Exception:
                    pass
    
    def _extract_package_name(self, requirement_line: str) -> str:
        """提取Python包名"""
        # 移除版本约束
        line = requirement_line.strip()
        if '==' in line:
            return line.split('==')[0]
        elif '>=' in line:
            return line.split('>=')[0]
        elif '<=' in line:
            return line.split('<=')[0]
        elif '~=' in line:
            return line.split('~=')[0]
        else:
            return line
    
    def _extract_version(self, requirement_line: str) -> str:
        """提取版本信息"""
        line = requirement_line.strip()
        if '==' in line:
            return line.split('==')[1]
        elif '>=' in line:
            return line.split('>=')[1]
        elif '<=' in line:
            return line.split('<=')[1]
        elif '~=' in line:
            return line.split('~=')[1]
        else:
            return "unknown"
    
    def _classify_python_dependency(self, requirement_line: str) -> str:
        """分类Python依赖"""
        line = requirement_line.strip()
        package_name = self._extract_package_name(line)
        
        # 常见分类
        if any(name in package_name.lower() for name in ['flask', 'django', 'fastapi']):
            return "web框架"
        elif any(name in package_name.lower() for name in ['pandas', 'numpy', 'scipy']):
            return "数据处理"
        elif any(name in package_name.lower() for name in ['scikit', 'tensorflow', 'pytorch']):
            return "机器学习"
        elif any(name in package_name.lower() for name in ['sqlalchemy', 'psycopg', 'mysql']):
            return "数据库"
        elif any(name in package_name.lower() for name in ['requests', 'httpx', 'aiohttp']):
            return "网络通信"
        elif any(name in package_name.lower() for name in ['click', 'argparse', 'rich']):
            return "命令行工具"
        elif any(name in package_name.lower() for name in ['pytest', 'unittest', 'tox']):
            return "测试工具"
        elif any(name in package_name.lower() for name in ['black', 'flake8', 'isort']):
            return "代码格式化"
        else:
            return "通用库"
    
    def _classify_javascript_dependency(self, dep_name: str) -> str:
        """分类JavaScript依赖"""
        if any(name in dep_name.lower() for name in ['react', 'vue', 'angular', 'svelte']):
            return "前端框架"
        elif any(name in dep_name.lower() for name in ['express', 'koa', 'nestjs', 'fastify']):
            return "后端框架"
        elif any(name in dep_name.lower() for name in ['lodash', 'moment', 'axios', 'jquery']):
            return "实用工具"
        elif any(name in dep_name.lower() for name in ['webpack', 'rollup', 'vite', 'parcel']):
            return "构建工具"
        elif any(name in dep_name.lower() for name in ['jest', 'mocha', 'cypress', 'playwright']):
            return "测试工具"
        elif any(name in dep_name.lower() for name in ['eslint', 'prettier', 'stylelint']):
            return "代码格式化"
        else:
            return "通用库"
    
    def _format_dependencies_report(self, dependencies_data: Dict[str, Any]) -> str:
        """格式化依赖关系报告"""
        
        report_lines = []
        
        report_lines.append("## 依赖关系分析报告")
        report_lines.append("")
        
        # 外部依赖统计
        external_count = len(dependencies_data["external_dependencies"])
        report_lines.append(f"### 外部依赖统计 ({external_count}个)")
        report_lines.append("")
        
        if external_count > 0:
            report_lines.append("| 名称 | 版本 | 类型 | 分类 | 来源 |")
            report_lines.append("|------|------|------|------|------|")
            
            for dep in dependencies_data["external_dependencies"]:
                report_lines.append(f"| {dep['name']} | {dep['version']} | {dep['type']} | {dep['category']} | {dep['source']} |")
            
            report_lines.append("")
        
        # 内部依赖统计
        internal_count = len(dependencies_data["internal_dependencies"])
        report_lines.append(f"### 内部模块依赖统计 ({internal_count}个模块)")
        report_lines.append("")
        
        if internal_count > 0:
            report_lines.append("| 模块名称 | 依赖数量 | 复杂度 | 路径 |")
            report_lines.append("|----------|----------|--------|------|")
            
            for dep in dependencies_data["internal_dependencies"]:
                complexity_label = "高" if dep['complexity'] else "低"
                report_lines.append(f"| {dep['module']} | {dep['dependency_count']} | {complexity_label} | {dep['module_path']} |")
            
            report_lines.append("")
        
        # 依赖问题
        issues_count = len(dependencies_data["dependency_issues"])
        if issues_count > 0:
            report_lines.append(f"### 依赖问题 ({issues_count}个)")
            report_lines.append("")
            
            for issue in dependencies_data["dependency_issues"]:
                report_lines.append(f"- **{issue['type']}**: {issue['description']}")
            
            report_lines.append("")
        
        # 优化建议
        suggestions_count = len(dependencies_data["optimization_suggestions"])
        if suggestions_count > 0:
            report_lines.append(f"### 依赖优化建议 ({suggestions_count}个)")
            report_lines.append("")
            
            for suggestion in dependencies_data["optimization_suggestions"]:
                report_lines.append(f"- **{suggestion['category']}**: {suggestion['recommendation']}")
            
            report_lines.append("")
        
        # 依赖图谱（Mermaid格式）
        report_lines.append("### 依赖关系图谱")
        report_lines.append("")
        report_lines.append("```mermaid")
        report_lines.append("graph TD")
        report_lines.append("")
        
        # 添加一些示例节点
        report_lines.append("    Project[项目主体]")
        report_lines.append("    ")
        
        # 外部依赖节点
        for i, dep in enumerate(dependencies_data["external_dependencies"][:5]):  # 限制数量
            node_id = f"ExtDep{i}"
            report_lines.append(f"    {node_id}[{dep['name']}]")
            report_lines.append(f"    Project --> {node_id}")
        
        # 内部依赖关系
        for i, dep in enumerate(dependencies_data["internal_dependencies"][:5]):  # 限制数量
            node_id = f"IntDep{i}"
            report_lines.append(f"    {node_id}[{dep['module']}]")
            report_lines.append(f"    Project --> {node_id}")
        
        report_lines.append("```")
        report_lines.append("")
        
        # 总结
        report_lines.append("### 总结")
        report_lines.append("")
        
        total_deps = external_count + internal_count
        report_lines.append(f"项目共有 **{total_deps}** 个依赖项，其中：")
        report_lines.append(f"- 外部依赖：**{external_count}** 个")
        report_lines.append(f"- 内部依赖：**{internal_count}** 个")
        
        if issues_count > 0:
            report_lines.append(f"- 发现问题：**{issues_count}** 个")
        
        if suggestions_count > 0:
            report_lines.append(f"- 优化建议：**{suggestions_count}** 条")
        
        return "\n".join(report_lines)