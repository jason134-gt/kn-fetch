"""
代码解析Agent - 负责代码语法解析与AST分析

功能：
- 多语言语法解析（Python、JavaScript、Java等）
- AST抽象语法树构建
- 语法元素提取（函数、类、变量等）
- 语法错误检测
- 代码结构分析
"""

import ast
import re
from typing import List, Dict, Any, Optional, Union
from .base_agent import BaseAgent
from src.extract.models.code_metadata import (
    CodeMetadata, FunctionMetadata, ClassMetadata, CodeLanguage, FileMetadata, 
    ImportMetadata, VariableMetadata, CodeComplexity
)
from pathlib import Path


class CodeParserAgent(BaseAgent):
    """代码解析Agent"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__("code_parser", config)
        self.language_parsers = {
            ".py": self._parse_python,
            ".js": self._parse_javascript,
            ".ts": self._parse_typescript,
            ".java": self._parse_java,
            ".cpp": self._parse_cpp,
            ".c": self._parse_c,
            ".go": self._parse_go,
        }
    
    async def _execute_impl(self, input_data: Any) -> CodeMetadata:
        """执行代码解析"""
        if isinstance(input_data, dict):
            file_path = input_data.get("file_path")
            file_content = input_data.get("content")
            file_metadata = input_data.get("metadata")
        else:
            raise ValueError("输入数据应包含文件路径和内容")
        
        if not file_path or not file_content:
            raise ValueError("缺少必要的文件路径或内容")
        
        self.logger.info(f"开始解析文件: {file_path}")
        
        # 根据文件扩展选择解析器
        file_extension = Path(file_path).suffix.lower()
        parser_func = self.language_parsers.get(file_extension)
        
        if not parser_func:
            self.logger.warning(f"不支持的文件类型: {file_extension}")
            return FileMetadata(file_path=file_path, language=CodeLanguage.PYTHON)
        
        # 执行解析
        file_metadata = await parser_func(file_path, file_content)
        
        self.logger.info(f"解析完成，提取 {len(file_metadata.functions)} 个函数，{len(file_metadata.classes)} 个类")
        return file_metadata
    
    async def _parse_python(self, file_path: str, content: str) -> FileMetadata:
        """解析Python代码"""
        try:
            tree = ast.parse(content)
            
            file_metadata = FileMetadata(file_path=file_path, language=CodeLanguage.PYTHON)
            
            # 提取函数
            for node in ast.walk(tree):
                if isinstance(node, ast.FunctionDef):
                    func_metadata = self._extract_python_function(node, content)
                    if func_metadata:
                        file_metadata.functions.append(func_metadata)
                
                elif isinstance(node, ast.ClassDef):
                    class_metadata = self._extract_python_class(node, content)
                    if class_metadata:
                        file_metadata.classes.append(class_metadata)
                
                elif isinstance(node, ast.Import) or isinstance(node, ast.ImportFrom):
                    import_metadata = self._extract_python_import(node)
                    if import_metadata:
                        file_metadata.imports.append(import_metadata)
            
            # 提取变量（全局变量）
            global_vars = self._extract_python_global_variables(tree, content)
            file_metadata.variables.extend(global_vars)
            
            return file_metadata
            
        except SyntaxError as e:
            self.logger.warning(f"Python语法错误 {file_path}: {e}")
            return FileMetadata(file_path=file_path, language=CodeLanguage.PYTHON, has_syntax_errors=True)
        except Exception as e:
            self.logger.error(f"解析Python文件失败 {file_path}: {e}")
            return FileMetadata(file_path=file_path, language=CodeLanguage.PYTHON)
    
    def _extract_python_function(self, node: ast.FunctionDef, content: str) -> FunctionMetadata:
        """提取Python函数元数据"""
        # 获取函数定义行
        start_line = node.lineno
        end_line = self._get_function_end_line(node, content)
        
        # 提取参数
        args = []
        if node.args.args:
            for arg in node.args.args:
                args.append(arg.arg)
        
        # 提取装饰器
        decorators = []
        if node.decorator_list:
            for decorator in node.decorator_list:
                if isinstance(decorator, ast.Name):
                    decorators.append(decorator.id)
        
        # 提取文档字符串
        docstring = ast.get_docstring(node)
        
        # 创建复杂度指标（简化实现）
        complexity = CodeComplexity(
            cyclomatic_complexity=1,
            cognitive_complexity=1,
            lines_of_code=end_line - start_line + 1,
            comment_density=0.1,
            maintainability_index=80.0
        )
        
        return FunctionMetadata(
            name=node.name,
            signature=f"def {node.name}({', '.join(args)})",
            parameters=args,
            return_type=self._extract_return_type(node),
            docstring=docstring,
            complexity=complexity,
            start_line=start_line,
            end_line=end_line,
            is_public=not node.name.startswith('_'),
            is_async=isinstance(node, ast.AsyncFunctionDef),
            decorators=decorators
        )
    
    def _extract_python_class(self, node: ast.ClassDef, content: str) -> ClassMetadata:
        """提取Python类元数据"""
        # 获取类定义行
        start_line = node.lineno
        end_line = self._get_class_end_line(node, content)
        
        # 提取基类
        base_classes = []
        if node.bases:
            for base in node.bases:
                if isinstance(base, ast.Name):
                    base_classes.append(base.id)
        
        # 提取类变量和方法
        class_vars = []
        methods = []
        
        for item in node.body:
            if isinstance(item, ast.Assign):
                # 类变量
                for target in item.targets:
                    if isinstance(target, ast.Name):
                        class_vars.append(target.id)
            elif isinstance(item, ast.FunctionDef):
                # 方法
                method_metadata = self._extract_python_function(item, content)
                methods.append(method_metadata)
        
        # 提取文档字符串
        docstring = ast.get_docstring(node)
        
        # 创建复杂度指标（简化实现）
        complexity = CodeComplexity(
            cyclomatic_complexity=1,
            cognitive_complexity=1,
            lines_of_code=end_line - start_line + 1,
            comment_density=0.1,
            maintainability_index=80.0
        )
        
        return ClassMetadata(
            name=node.name,
            base_classes=base_classes,
            docstring=docstring,
            methods=methods,
            properties=class_vars,
            complexity=complexity,
            start_line=start_line,
            end_line=end_line,
            is_abstract=any('abstract' in dec for dec in node.decorator_list),
            decorators=[dec.id for dec in node.decorator_list if isinstance(dec, ast.Name)]
        )
    
    def _extract_python_import(self, node: Union[ast.Import, ast.ImportFrom]) -> ImportMetadata:
        """提取Python导入元数据"""
        if isinstance(node, ast.Import):
            # import module
            module_names = [alias.name for alias in node.names]
            return ImportMetadata(module_names=module_names)
        else:
            # from module import names
            module = node.module or ""
            imported_names = [alias.name for alias in node.names]
            return ImportMetadata(module_names=[module], imported_names=imported_names)
    
    def _extract_python_global_variables(self, tree: ast.AST, content: str) -> List[VariableMetadata]:
        """提取Python全局变量"""
        variables = []
        
        for node in ast.walk(tree):
            if isinstance(node, ast.Assign) and not self._is_inside_function_or_class(node):
                for target in node.targets:
                    if isinstance(target, ast.Name):
                        # 获取起始行号
                        start_line = node.lineno
                        # 简化实现：结束行号设为起始行号
                        end_line = start_line
                        
                        variables.append(VariableMetadata(
                            name=target.id,
                            variable_type="global",
                            value_type=self._infer_variable_type(node.value),
                            start_line=start_line,
                            end_line=end_line
                        ))
        
        return variables
    
    def _get_function_end_line(self, node: ast.FunctionDef, content: str) -> int:
        """获取函数结束行号"""
        lines = content.split('\n')
        current_line = node.lineno
        
        # 查找函数结束的缩进级别
        function_indent = self._get_line_indent(lines[current_line - 1])
        
        for i in range(current_line, len(lines)):
            line_indent = self._get_line_indent(lines[i])
            if line_indent <= function_indent and lines[i].strip():
                return i
        
        return len(lines)
    
    def _get_class_end_line(self, node: ast.ClassDef, content: str) -> int:
        """获取类结束行号"""
        lines = content.split('\n')
        current_line = node.lineno
        
        # 查找类结束的缩进级别
        class_indent = self._get_line_indent(lines[current_line - 1])
        
        for i in range(current_line, len(lines)):
            line_indent = self._get_line_indent(lines[i])
            if line_indent <= class_indent and lines[i].strip() and not lines[i].strip().startswith('#'):
                return i
        
        return len(lines)
    
    def _get_line_indent(self, line: str) -> int:
        """获取行缩进级别"""
        return len(line) - len(line.lstrip())
    
    def _is_inside_function_or_class(self, node: ast.AST) -> bool:
        """检查节点是否在函数或类内部"""
        parent = node
        while hasattr(parent, 'parent'):
            parent = parent.parent
            if isinstance(parent, (ast.FunctionDef, ast.ClassDef, ast.AsyncFunctionDef)):
                return True
        return False
    
    def _extract_return_type(self, node: ast.FunctionDef) -> Optional[str]:
        """提取返回类型注解"""
        if node.returns:
            if isinstance(node.returns, ast.Name):
                return node.returns.id
            elif isinstance(node.returns, ast.Subscript):
                return self._extract_subscript_type(node.returns)
        return None
    
    def _extract_subscript_type(self, node: ast.Subscript) -> str:
        """提取泛型类型注解"""
        if isinstance(node.value, ast.Name):
            base_type = node.value.id
            if isinstance(node.slice, ast.Name):
                return f"{base_type}[{node.slice.id}]"
            elif isinstance(node.slice, ast.Index):
                if isinstance(node.slice.value, ast.Name):
                    return f"{base_type}[{node.slice.value.id}]"
        return "unknown"
    
    def _infer_variable_type(self, value_node: ast.AST) -> str:
        """推断变量类型"""
        if isinstance(value_node, ast.Constant):
            return type(value_node.value).__name__
        elif isinstance(value_node, ast.List):
            return "list"
        elif isinstance(value_node, ast.Dict):
            return "dict"
        elif isinstance(value_node, ast.Set):
            return "set"
        elif isinstance(value_node, ast.Tuple):
            return "tuple"
        elif isinstance(value_node, ast.Call):
            if isinstance(value_node.func, ast.Name):
                return value_node.func.id
        return "unknown"
    
    # 使用增强的多语言解析器
    async def _parse_javascript(self, file_path: str, content: str) -> FileMetadata:
        """解析JavaScript代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_javascript(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.JAVASCRIPT)
    
    async def _parse_typescript(self, file_path: str, content: str) -> FileMetadata:
        """解析TypeScript代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_typescript(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.TYPESCRIPT)
    
    async def _parse_java(self, file_path: str, content: str) -> FileMetadata:
        """解析Java代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_java(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.JAVA)
    
    async def _parse_cpp(self, file_path: str, content: str) -> FileMetadata:
        """解析C++代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_cpp(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.CPP)
    
    async def _parse_c(self, file_path: str, content: str) -> FileMetadata:
        """解析C代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_c(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.C)
    
    async def _parse_go(self, file_path: str, content: str) -> FileMetadata:
        """解析Go代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_go(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.GO)
    
    async def _parse_rust(self, file_path: str, content: str) -> FileMetadata:
        """解析Rust代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_rust(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.RUST)
    
    async def _parse_php(self, file_path: str, content: str) -> FileMetadata:
        """解析PHP代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_php(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.PHP)
    
    async def _parse_ruby(self, file_path: str, content: str) -> FileMetadata:
        """解析Ruby代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_ruby(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.RUBY)
    
    async def _parse_csharp(self, file_path: str, content: str) -> FileMetadata:
        """解析C#代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_csharp(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.CSHARP)
    
    async def _parse_swift(self, file_path: str, content: str) -> FileMetadata:
        """解析Swift代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_swift(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.SWIFT)
    
    async def _parse_kotlin(self, file_path: str, content: str) -> FileMetadata:
        """解析Kotlin代码"""
        from .enhanced_language_parsers import EnhancedLanguageParser
        
        parser = EnhancedLanguageParser()
        metadata = parser.parse_kotlin(file_path, content)
        
        return self._convert_to_file_metadata(metadata, CodeLanguage.KOTLIN)
    
    def _convert_to_file_metadata(self, metadata: Dict[str, Any], language: CodeLanguage) -> FileMetadata:
        """将解析结果转换为FileMetadata对象"""
        file_metadata = FileMetadata(
            file_path=metadata["file_path"],
            language=language,
            has_syntax_errors=metadata.get("has_errors", False)
        )
        
        # 转换函数信息
        for func_data in metadata.get("functions", []):
            func_metadata = FunctionMetadata(
                name=func_data.get("name", ""),
                start_line=func_data.get("start_line", 0),
                end_line=func_data.get("end_line", 0),
                parameters=func_data.get("parameters", []),
                is_public=True
            )
            file_metadata.functions.append(func_metadata)
        
        # 转换类信息
        for class_data in metadata.get("classes", []):
            class_metadata = ClassMetadata(
                name=class_data.get("name", ""),
                start_line=class_data.get("start_line", 0),
                end_line=class_data.get("end_line", 0),
                base_classes=class_data.get("base_classes", [])
            )
            file_metadata.classes.append(class_metadata)
        
        return file_metadata


# 路径导入补丁
from pathlib import Path