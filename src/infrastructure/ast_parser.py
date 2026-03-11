"""
AST解析器 - 基于design-v1方案实现标准化的多语言AST解析
"""

import os
from typing import List, Dict, Any, Optional
from pathlib import Path
from loguru import logger


class ASTParser:
    """多语言AST解析器"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.supported_languages = [
            "python", "javascript", "typescript", "java", 
            "cpp", "go", "rust", "php", "ruby"
        ]
        self._parsers = {}
        
        # 初始化tree-sitter解析器
        self._init_parsers()
    
    def _init_parsers(self):
        """初始化tree-sitter解析器"""
        try:
            import tree_sitter
            from tree_sitter import Language, Parser
            
            # 尝试加载预编译的语言库
            language_libs = {
                "python": "tree_sitter_python",
                "javascript": "tree_sitter_javascript", 
                "typescript": "tree_sitter_typescript",
                "java": "tree_sitter_java",
                "cpp": "tree_sitter_cpp",
                "go": "tree_sitter_go",
                "rust": "tree_sitter_rust",
                "php": "tree_sitter_php",
                "ruby": "tree_sitter_ruby"
            }
            
            for lang, lib_name in language_libs.items():
                try:
                    lib = __import__(lib_name)
                    language = Language(lib.language())
                    parser = Parser()
                    parser.set_language(language)
                    self._parsers[lang] = parser
                    logger.info(f"成功加载 {lang} 解析器")
                except ImportError:
                    logger.warning(f"无法加载 {lang} 解析器库")
                    
        except ImportError:
            logger.warning("tree-sitter未安装，使用简化解析模式")
    
    def parse_file(self, file_path: str) -> Optional[Dict[str, Any]]:
        """解析单个文件"""
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
            
            # 使用tree-sitter解析
            if language in self._parsers:
                return self._parse_with_treesitter(file_path, content, language)
            else:
                # 回退到简化解析
                return self._parse_simple(file_path, content, language)
                
        except Exception as e:
            logger.error(f"解析文件失败 {file_path}: {e}")
            return None
    
    def parse_directory(self, directory: str) -> List[Dict[str, Any]]:
        """解析整个目录"""
        results = []
        
        try:
            for root, dirs, files in os.walk(directory):
                # 忽略隐藏目录和特定目录
                dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', '__pycache__']]
                
                for file in files:
                    file_path = os.path.join(root, file)
                    
                    # 检查文件类型
                    if self._is_supported_file(file_path):
                        result = self.parse_file(file_path)
                        if result:
                            results.append(result)
            
            logger.info(f"成功解析 {len(results)} 个文件")
            return results
            
        except Exception as e:
            logger.error(f"解析目录失败 {directory}: {e}")
            return results
    
    def _detect_language(self, file_path: str) -> Optional[str]:
        """检测文件语言"""
        ext_map = {
            '.py': 'python',
            '.js': 'javascript',
            '.ts': 'typescript',
            '.java': 'java',
            '.cpp': 'cpp', '.cc': 'cpp', '.cxx': 'cpp', '.h': 'cpp', '.hpp': 'cpp',
            '.go': 'go',
            '.rs': 'rust',
            '.php': 'php',
            '.rb': 'ruby'
        }
        
        ext = Path(file_path).suffix.lower()
        return ext_map.get(ext)
    
    def _is_supported_file(self, file_path: str) -> bool:
        """检查是否支持的文件类型"""
        language = self._detect_language(file_path)
        return language is not None
    
    def _parse_with_treesitter(self, file_path: str, content: str, language: str) -> Dict[str, Any]:
        """使用tree-sitter解析"""
        try:
            parser = self._parsers[language]
            tree = parser.parse(bytes(content, 'utf8'))
            
            # 提取基本结构信息
            result = {
                "file_path": file_path,
                "language": language,
                "file_size": len(content),
                "line_count": content.count('\n') + 1,
                "functions": [],
                "classes": [],
                "imports": [],
                "exports": []
            }
            
            # 语言特定的解析逻辑
            if language == "python":
                self._parse_python_tree(tree, result)
            elif language in ["javascript", "typescript"]:
                self._parse_javascript_tree(tree, result)
            elif language == "java":
                self._parse_java_tree(tree, result)
            
            return result
            
        except Exception as e:
            logger.warning(f"tree-sitter解析失败，使用简化解析: {e}")
            return self._parse_simple(file_path, content, language)
    
    def _parse_simple(self, file_path: str, content: str, language: str) -> Dict[str, Any]:
        """简化解析（回退方案）"""
        return {
            "file_path": file_path,
            "language": language,
            "file_size": len(content),
            "line_count": content.count('\n') + 1,
            "functions": [],
            "classes": [],
            "imports": [],
            "exports": []
        }
    
    def _parse_python_tree(self, tree, result: Dict[str, Any]):
        """解析Python AST树"""
        # 简化的Python解析逻辑
        # 实际实现需要遍历AST树提取函数、类等信息
        pass
    
    def _parse_javascript_tree(self, tree, result: Dict[str, Any]):
        """解析JavaScript/TypeScript AST树"""
        # 简化的JS/TS解析逻辑
        pass
    
    def _parse_java_tree(self, tree, result: Dict[str, Any]):
        """解析Java AST树"""
        # 简化的Java解析逻辑
        pass