"""
Code Parser Agent - 代码解析与预处理

负责解析代码的语法结构，提取类、函数、方法等代码元素
"""
from typing import Dict, Any, List, Optional
import os
import json
from pathlib import Path
import time

from ...base.agent import BaseAgent, AgentResult
from ...config.agent_configs import CODE_PARSER_AGENT


class CodeParserAgent(BaseAgent):
    """代码解析与预处理Agent
    
    作为代码解析与预处理专家，负责分析代码的语法结构和复杂度
    """
    
    def __init__(self):
        """
        初始化CodeParserAgent
        """
        super().__init__(CODE_PARSER_AGENT)
        self.supported_extensions = {
            '.java', '.py', '.js', '.ts', '.go', '.cpp', '.c', '.h', '.hpp',
            '.cs', '.php', '.rb', '.rs', '.swift', '.kt', '.scala', '.m', '.sql'
        }
    
    def execute(self, context):
        """
        执行代码解析
        
        Args:
            context: Agent执行上下文
            
        Returns:
            Agent执行结果
        """
        start_time = time.time()
        
        try:
            self.logger.info(f"Starting code parsing for project")
            
            # 1. 扫描项目文件
            project_files = self._scan_project_files(context.project_root)
            
            # 2. 过滤支持的文件
            supported_files = self._filter_supported_files(project_files)
            
            # 3. 解析代码结构
            parsing_results = self._parse_code_files(supported_files, context.project_root)
            
            # 4. 分析代码复杂度
            complexity_analysis = self._analyze_complexity(parsing_results)
            
            # 5. 构建分析报告
            analysis_report = self._build_analysis_report(
                parsing_results, complexity_analysis, len(project_files)
            )
            
            # 6. 统计信息
            stats = {
                "total_files": len(project_files),
                "supported_files": len(supported_files),
                "total_classes": sum(len(result.get("classes", [])) for result in parsing_results),
                "total_functions": sum(len(result.get("functions", [])) for result in parsing_results),
                "total_methods": sum(len(result.get("methods", [])) for result in parsing_results),
                "avg_complexity": complexity_analysis.get("average_cyclomatic_complexity", 0)
            }
            
            execution_time = time.time() - start_time
            
            return AgentResult(
                success=True,
                data={
                    "parsing_results": parsing_results,
                    "complexity_analysis": complexity_analysis,
                    "file_statistics": {
                        "total_files": len(project_files),
                        "supported_files": len(supported_files)
                    }
                },
                output=analysis_report,
                metrics=stats,
                cross_notes=self._extract_cross_module_notes(analysis_report)
            )
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.logger.error(f"Code parsing failed: {str(e)}")
            
            return AgentResult(
                success=False,
                error=str(e),
                metrics={"execution_time": execution_time}
            )
    
    def _get_task_description(self, context):
        """获取任务描述"""
        return """分析代码库的语法结构，包括：
1. 多语言代码文件扫描和识别
2. 提取类、接口、函数、方法等代码结构元素
3. 分析代码复杂度和可维护性指标
4. 识别潜在的代码质量问题"""
    
    def _scan_project_files(self, project_root: str) -> List[Dict[str, Any]]:
        """
        扫描项目文件
        
        Args:
            project_root: 项目根目录
            
        Returns:
            文件信息列表
        """
        files = []
        project_path = Path(project_root)
        
        # 排除常见的非代码目录
        exclude_dirs = {
            '.git', 'node_modules', '__pycache__', '.idea', '.vscode',
            'build', 'dist', 'target', 'bin', 'obj', 'venv', 'env'
        }
        
        for file_path in project_path.rglob('*'):
            # 跳过目录
            if file_path.is_dir():
                continue
            
            # 跳过隐藏文件和排除目录
            if any(part.startswith('.') and part != '.gitignore' for part in file_path.parts):
                continue
            
            # 跳过排除目录中的文件
            if any(exclude_dir in file_path.parts for exclude_dir in exclude_dirs):
                continue
            
            # 获取文件信息
            try:
                file_info = {
                    "path": str(file_path.relative_to(project_path)),
                    "absolute_path": str(file_path),
                    "extension": file_path.suffix.lower(),
                    "size": file_path.stat().st_size,
                    "last_modified": file_path.stat().st_mtime
                }
                files.append(file_info)
            except (OSError, ValueError):
                continue
        
        return files
    
    def _filter_supported_files(self, files: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        过滤支持的文件类型
        
        Args:
            files: 文件信息列表
            
        Returns:
            支持的文件列表
        """
        return [
            file_info for file_info in files
            if file_info["extension"] in self.supported_extensions
        ]
    
    def _parse_code_files(self, files: List[Dict[str, Any]], project_root: str) -> List[Dict[str, Any]]:
        """
        解析代码文件
        
        Args:
            files: 文件信息列表
            project_root: 项目根目录
            
        Returns:
            解析结果列表
        """
        parsing_results = []
        
        for file_info in files:
            try:
                result = self._parse_single_file(file_info, project_root)
                if result:
                    parsing_results.append(result)
            except Exception as e:
                self.logger.warning(f"Failed to parse file {file_info['path']}: {str(e)}")
                # 添加基础解析结果
                parsing_results.append({
                    "file_path": file_info["path"],
                    "language": self._detect_language(file_info["extension"]),
                    "error": str(e),
                    "classes": [],
                    "functions": [],
                    "methods": [],
                    "imports": [],
                    "line_count": 0
                })
        
        return parsing_results
    
    def _parse_single_file(self, file_info: Dict[str, Any], project_root: str) -> Optional[Dict[str, Any]]:
        """
        解析单个文件
        
        Args:
            file_info: 文件信息
            project_root: 项目根目录
            
        Returns:
            解析结果字典
        """
        file_path = file_info["absolute_path"]
        extension = file_info["extension"]
        language = self._detect_language(extension)
        
        # 读取文件内容
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
        except Exception as e:
            self.logger.warning(f"Failed to read file {file_path}: {str(e)}")
            return None
        
        # 基础解析（简化版，实际项目中应该使用专业的AST解析器）
        result = {
            "file_path": file_info["path"],
            "language": language,
            "classes": self._extract_classes(content, language),
            "functions": self._extract_functions(content, language),
            "methods": self._extract_methods(content, language),
            "imports": self._extract_imports(content, language),
            "line_count": len(content.splitlines()),
            "file_size": file_info["size"]
        }
        
        return result
    
    def _detect_language(self, extension: str) -> str:
        """
        根据文件扩展名检测编程语言
        
        Args:
            extension: 文件扩展名
            
        Returns:
            编程语言名称
        """
        language_map = {
            '.java': 'Java',
            '.py': 'Python',
            '.js': 'JavaScript',
            '.ts': 'TypeScript',
            '.go': 'Go',
            '.cpp': 'C++',
            '.c': 'C',
            '.h': 'C/C++ Header',
            '.hpp': 'C++ Header',
            '.cs': 'C#',
            '.php': 'PHP',
            '.rb': 'Ruby',
            '.rs': 'Rust',
            '.swift': 'Swift',
            '.kt': 'Kotlin',
            '.scala': 'Scala',
            '.m': 'Objective-C',
            '.sql': 'SQL'
        }
        return language_map.get(extension, 'Unknown')
    
    def _extract_classes(self, content: str, language: str) -> List[Dict[str, Any]]:
        """
        提取类定义（简化实现）
        
        Args:
            content: 文件内容
            language: 编程语言
            
        Returns:
            类定义列表
        """
        classes = []
        
        if language == 'Java':
            import re
            pattern = r'class\s+(\w+)(?:\s+extends\s+\w+)?(?:\s+implements\s+[^{]+)?\s*{'
            matches = re.finditer(pattern, content)
            for match in matches:
                classes.append({
                    "name": match.group(1),
                    "type": "class",
                    "line": content[:match.start()].count('\n') + 1
                })
        
        elif language == 'Python':
            import re
            pattern = r'class\s+(\w+)(?:\([^)]+\))?:'
            matches = re.finditer(pattern, content)
            for match in matches:
                classes.append({
                    "name": match.group(1),
                    "type": "class",
                    "line": content[:match.start()].count('\n') + 1
                })
        
        return classes
    
    def _extract_functions(self, content: str, language: str) -> List[Dict[str, Any]]:
        """
        提取函数定义（简化实现）
        
        Args:
            content: 文件内容
            language: 编程语言
            
        Returns:
            函数定义列表
        """
        functions = []
        
        if language == 'Python':
            import re
            pattern = r'def\s+(\w+)\s*\('
            matches = re.finditer(pattern, content)
            for match in matches:
                functions.append({
                    "name": match.group(1),
                    "type": "function",
                    "line": content[:match.start()].count('\n') + 1
                })
        
        elif language in ['JavaScript', 'TypeScript']:
            import re
            pattern = r'(?:function\s+(\w+)|const\s+(\w+)\s*=\s*\()'
            matches = re.finditer(pattern, content)
            for match in matches:
                name = match.group(1) or match.group(2)
                functions.append({
                    "name": name,
                    "type": "function",
                    "line": content[:match.start()].count('\n') + 1
                })
        
        return functions
    
    def _extract_methods(self, content: str, language: str) -> List[Dict[str, Any]]:
        """
        提取方法定义（简化实现）
        
        Args:
            content: 文件内容
            language: 编程语言
            
        Returns:
            方法定义列表
        """
        methods = []
        
        if language == 'Java':
            import re
            # 简化的Java方法提取
            pattern = r'(?:public|private|protected)?\s+(?:static\s+)?(?:\w+\s+)*(\w+)\s*\([^)]*\)\s*(?:throws\s+[^{]+)?\s*{'
            matches = re.finditer(pattern, content)
            for match in matches:
                methods.append({
                    "name": match.group(1),
                    "type": "method",
                    "line": content[:match.start()].count('\n') + 1
                })
        
        return methods
    
    def _extract_imports(self, content: str, language: str) -> List[str]:
        """
        提取导入语句（简化实现）
        
        Args:
            content: 文件内容
            language: 编程语言
            
        Returns:
            导入语句列表
        """
        imports = []
        
        if language == 'Java':
            import re
            pattern = r'import\s+([^;]+);'
            matches = re.finditer(pattern, content)
            for match in matches:
                imports.append(match.group(1).strip())
        
        elif language == 'Python':
            import re
            pattern = r'(?:from\s+(\S+)\s+)?import\s+([^#\n]+)'
            matches = re.finditer(pattern, content)
            for match in matches:
                if match.group(1):
                    imports.append(f"from {match.group(1)} import {match.group(2)}")
                else:
                    imports.append(f"import {match.group(2)}")
        
        return imports
    
    def _analyze_complexity(self, parsing_results: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        分析代码复杂度
        
        Args:
            parsing_results: 解析结果列表
            
        Returns:
            复杂度分析结果
        """
        total_files = len(parsing_results)
        if total_files == 0:
            return {
                "average_cyclomatic_complexity": 0,
                "total_lines": 0,
                "average_lines_per_file": 0,
                "complexity_distribution": {}
            }
        
        total_lines = sum(result.get("line_count", 0) for result in parsing_results)
        
        # 简化的复杂度分析（实际项目中应该使用专业的复杂度分析工具）
        complexity_scores = []
        for result in parsing_results:
            # 基于函数/方法数量估算复杂度
            functions_count = len(result.get("functions", []))
            methods_count = len(result.get("methods", []))
            line_count = result.get("line_count", 0)
            
            # 简化的圈复杂度估算
            complexity = max(1, (functions_count + methods_count) * 2 + line_count // 50)
            complexity_scores.append(complexity)
        
        avg_complexity = sum(complexity_scores) / total_files if complexity_scores else 0
        
        return {
            "average_cyclomatic_complexity": avg_complexity,
            "total_lines": total_lines,
            "average_lines_per_file": total_lines / total_files,
            "complexity_distribution": {
                "low": len([c for c in complexity_scores if c < 10]),
                "medium": len([c for c in complexity_scores if 10 <= c < 20]),
                "high": len([c for c in complexity_scores if c >= 20])
            }
        }
    
    def _build_analysis_report(self, parsing_results: List[Dict[str, Any]], 
                              complexity_analysis: Dict[str, Any], 
                              total_files: int) -> str:
        """
        构建分析报告
        
        Args:
            parsing_results: 解析结果列表
            complexity_analysis: 复杂度分析结果
            total_files: 总文件数
            
        Returns:
            分析报告字符串
        """
        supported_files = len(parsing_results)
        
        # 按语言统计
        language_stats = {}
        for result in parsing_results:
            language = result.get("language", "Unknown")
            if language not in language_stats:
                language_stats[language] = 0
            language_stats[language] += 1
        
        # 统计代码结构
        total_classes = sum(len(result.get("classes", [])) for result in parsing_results)
        total_functions = sum(len(result.get("functions", [])) for result in parsing_results)
        total_methods = sum(len(result.get("methods", [])) for result in parsing_results)
        
        report = f"""# 代码解析分析报告

## 1. 文件统计
- **总文件数**: {total_files}
- **支持解析的文件数**: {supported_files}
- **支持率**: {supported_files/total_files*100:.1f}%

## 2. 语言分布
"""
        
        for language, count in sorted(language_stats.items(), key=lambda x: x[1], reverse=True):
            percentage = count / supported_files * 100
            report += f"""
- **{language}**: {count} 个文件 ({percentage:.1f}%)
"""
        
        report += f"""
## 3. 代码结构分析
- **总类数**: {total_classes}
- **总函数数**: {total_functions}
- **总方法数**: {total_methods}
- **平均每文件代码元素**: {(total_classes + total_functions + total_methods) / max(supported_files, 1):.1f}

## 4. 复杂度分析
- **平均圈复杂度**: {complexity_analysis.get('average_cyclomatic_complexity', 0):.1f}
- **总代码行数**: {complexity_analysis.get('total_lines', 0)}
- **平均每文件行数**: {complexity_analysis.get('average_lines_per_file', 0):.1f}

## 5. 复杂度分布
- **低复杂度文件**: {complexity_analysis.get('complexity_distribution', {}).get('low', 0)}
- **中等复杂度文件**: {complexity_analysis.get('complexity_distribution', {}).get('medium', 0)}
- **高复杂度文件**: {complexity_analysis.get('complexity_distribution', {}).get('high', 0)}

## 6. 质量评估

基于代码结构分析，该项目的代码质量评估如下：

### 结构完整性
- **代码组织**: {'良好' if total_classes > 0 else '需要改进'}
- **函数设计**: {'合理' if total_functions > 0 else '需要改进'}
- **模块化程度**: {'高' if total_classes > 10 else '中' if total_classes > 5 else '低'}

### 复杂度管理
- **整体复杂度**: {'低' if complexity_analysis.get('average_cyclomatic_complexity', 0) < 10 else '中等' if complexity_analysis.get('average_cyclomatic_complexity', 0) < 20 else '高'}
- **可维护性**: {'高' if complexity_analysis.get('average_cyclomatic_complexity', 0) < 15 else '中等' if complexity_analysis.get('average_cyclomatic_complexity', 0) < 25 else '低'}

### 改进建议
1. 对于高复杂度文件，考虑重构和分解
2. 确保代码遵循单一职责原则
3. 定期进行代码审查和质量检查
4. 建立代码质量标准和自动化检查
"""
        
        return report