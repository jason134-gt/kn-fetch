"""
复杂度扫描器 - 基于design-v1方案实现标准化的代码复杂度分析
"""

import os
from typing import List, Dict, Any, Optional
from pathlib import Path
from loguru import logger


class ComplexityScanner:
    """代码复杂度扫描器"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.supported_languages = [
            "python", "javascript", "typescript", "java", 
            "cpp", "go", "php", "ruby"
        ]
    
    def scan_file(self, file_path: str) -> Optional[Dict[str, Any]]:
        """扫描单个文件的复杂度"""
        try:
            if not os.path.exists(file_path):
                logger.error(f"文件不存在: {file_path}")
                return None
            
            # 获取文件语言
            language = self._detect_language(file_path)
            if not language:
                logger.warning(f"不支持的文件类型: {file_path}")
                return None
            
            # 读取文件内容
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            
            # 使用lizard进行复杂度分析
            if self._has_lizard():
                return self._scan_with_lizard(file_path, content, language)
            else:
                # 回退到简化分析
                return self._scan_simple(file_path, content, language)
                
        except Exception as e:
            logger.error(f"扫描文件复杂度失败 {file_path}: {e}")
            return None
    
    def scan_directory(self, directory: str) -> List[Dict[str, Any]]:
        """扫描整个目录的复杂度"""
        results = []
        
        try:
            for root, dirs, files in os.walk(directory):
                # 忽略隐藏目录和特定目录
                dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', '__pycache__']]
                
                for file in files:
                    file_path = os.path.join(root, file)
                    
                    # 检查文件类型
                    if self._is_supported_file(file_path):
                        result = self.scan_file(file_path)
                        if result:
                            results.append(result)
            
            logger.info(f"成功扫描 {len(results)} 个文件的复杂度")
            return results
            
        except Exception as e:
            logger.error(f"扫描目录复杂度失败 {directory}: {e}")
            return results
    
    def _has_lizard(self) -> bool:
        """检查是否安装了lizard"""
        try:
            import lizard
            return True
        except ImportError:
            return False
    
    def _scan_with_lizard(self, file_path: str, content: str, language: str) -> Dict[str, Any]:
        """使用lizard进行复杂度分析"""
        try:
            import lizard
            
            # 分析文件
            analysis_result = lizard.analyze_file(file_path)
            
            # 计算文件级指标
            total_ccn = sum(func.cyclomatic_complexity for func in analysis_result.function_list)
            total_lines = analysis_result.nloc
            
            # 构建结果
            result = {
                "file_path": file_path,
                "language": language,
                "metrics": {
                    "cyclomatic_complexity": total_ccn,
                    "cognitive_complexity": total_ccn,  # lizard不直接提供认知复杂度
                    "lines_of_code": total_lines,
                    "comment_density": self._calculate_comment_density(content),
                    "maintainability_index": self._calculate_maintainability_index(total_ccn, total_lines)
                },
                "functions": []
            }
            
            # 提取函数级复杂度
            for func in analysis_result.function_list:
                result["functions"].append({
                    "name": func.name,
                    "cyclomatic_complexity": func.cyclomatic_complexity,
                    "lines_of_code": func.length,
                    "start_line": func.start_line,
                    "end_line": func.end_line
                })
            
            return result
            
        except Exception as e:
            logger.warning(f"lizard分析失败，使用简化分析: {e}")
            return self._scan_simple(file_path, content, language)
    
    def _scan_simple(self, file_path: str, content: str, language: str) -> Dict[str, Any]:
        """简化复杂度分析（回退方案）"""
        lines = content.split('\n')
        code_lines = [line for line in lines if line.strip() and not line.strip().startswith(('#', '//', '/*', '*'))]
        comment_lines = [line for line in lines if line.strip() and line.strip().startswith(('#', '//', '/*', '*'))]
        
        # 简化的复杂度估算
        complexity = self._estimate_complexity(content, language)
        
        return {
            "file_path": file_path,
            "language": language,
            "metrics": {
                "cyclomatic_complexity": complexity,
                "cognitive_complexity": complexity,
                "lines_of_code": len(code_lines),
                "comment_density": len(comment_lines) / max(len(code_lines), 1),
                "maintainability_index": max(0, 171 - 5.2 * complexity - 0.23 * len(code_lines))
            },
            "functions": []
        }
    
    def _detect_language(self, file_path: str) -> Optional[str]:
        """检测文件语言"""
        ext_map = {
            '.py': 'python',
            '.js': 'javascript',
            '.ts': 'typescript',
            '.java': 'java',
            '.cpp': 'cpp', '.cc': 'cpp', '.cxx': 'cpp', '.h': 'cpp', '.hpp': 'cpp',
            '.go': 'go',
            '.php': 'php',
            '.rb': 'ruby'
        }
        
        ext = Path(file_path).suffix.lower()
        return ext_map.get(ext)
    
    def _is_supported_file(self, file_path: str) -> bool:
        """检查是否支持的文件类型"""
        language = self._detect_language(file_path)
        return language is not None
    
    def _calculate_comment_density(self, content: str) -> float:
        """计算注释密度"""
        lines = content.split('\n')
        code_lines = [line for line in lines if line.strip() and not line.strip().startswith(('#', '//', '/*', '*'))]
        comment_lines = [line for line in lines if line.strip() and line.strip().startswith(('#', '//', '/*', '*'))]
        
        total_lines = len(code_lines) + len(comment_lines)
        if total_lines == 0:
            return 0.0
        
        return len(comment_lines) / total_lines
    
    def _calculate_maintainability_index(self, ccn: int, loc: int) -> float:
        """计算可维护性指数"""
        # 简化的可维护性指数计算
        # MI = 171 - 5.2 * ln(CC) - 0.23 * ln(LOC) - 16.2 * ln(评论行数)
        # 这里使用简化版本
        import math
        
        if ccn == 0 or loc == 0:
            return 100.0
        
        mi = 171 - 5.2 * math.log(ccn) - 0.23 * math.log(loc)
        return max(0, min(100, mi))
    
    def _estimate_complexity(self, content: str, language: str) -> int:
        """估算复杂度"""
        # 基于控制流关键字估算
        complexity_keywords = {
            'python': ['if', 'elif', 'else', 'for', 'while', 'try', 'except', 'with'],
            'javascript': ['if', 'else', 'for', 'while', 'try', 'catch', 'switch', 'case'],
            'typescript': ['if', 'else', 'for', 'while', 'try', 'catch', 'switch', 'case'],
            'java': ['if', 'else', 'for', 'while', 'try', 'catch', 'switch', 'case'],
            'cpp': ['if', 'else', 'for', 'while', 'try', 'catch', 'switch', 'case'],
            'go': ['if', 'else', 'for', 'while', 'switch', 'case', 'select'],
            'php': ['if', 'else', 'for', 'while', 'try', 'catch', 'switch', 'case'],
            'ruby': ['if', 'else', 'for', 'while', 'begin', 'rescue', 'case', 'when']
        }
        
        keywords = complexity_keywords.get(language, [])
        complexity = 1  # 基础复杂度
        
        for keyword in keywords:
            complexity += content.lower().count(f' {keyword} ')
            complexity += content.lower().count(f'\t{keyword} ')
            complexity += content.lower().count(f'\n{keyword} ')
        
        return complexity