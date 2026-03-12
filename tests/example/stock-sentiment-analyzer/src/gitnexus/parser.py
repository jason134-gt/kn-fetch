import ast
import re
import hashlib
from typing import List, Optional, Any
from pathlib import Path
from tree_sitter import Language, Parser
import tree_sitter_python
import tree_sitter_javascript
import tree_sitter_typescript
import tree_sitter_java
import tree_sitter_cpp
import tree_sitter_go

from .models import CodeEntity, EntityType
from .exceptions import ParseError

class CodeParser:
    """多语言代码解析器"""
    
    def __init__(self):
        # 初始化Tree-sitter解析器
        self.parsers = {}
        self.languages = {
            "python": Language(tree_sitter_python.language()),
            "javascript": Language(tree_sitter_javascript.language()),
            "typescript": Language(tree_sitter_typescript.language()),
            "java": Language(tree_sitter_java.language()),
            "cpp": Language(tree_sitter_cpp.language()),
            "go": Language(tree_sitter_go.language())
        }
        
        for lang, language in self.languages.items():
            parser = Parser()
            parser.set_language(language)
            self.parsers[lang] = parser
        
        # 文件扩展名映射
        self.extension_map = {
            ".py": "python",
            ".js": "javascript",
            ".jsx": "javascript",
            ".ts": "typescript",
            ".tsx": "typescript",
            ".java": "java",
            ".cpp": "cpp",
            ".cc": "cpp",
            ".cxx": "cpp",
            ".h": "cpp",
            ".hpp": "cpp",
            ".go": "go"
        }
    
    def detect_language(self, content: str, file_path: Optional[str] = None) -> str:
        """检测文件编程语言"""
        if file_path:
            ext = Path(file_path).suffix.lower()
            if ext in self.extension_map:
                return self.extension_map[ext]
        
        # 基于内容检测
        if re.search(r'^import\s+|def\s+\w+\s*\(|class\s+\w+(\(|\s*:)', content, re.MULTILINE):
            return "python"
        elif re.search(r'function\s+\w+|const\s+\w+\s*=|let\s+\w+|var\s+\w+', content):
            return "javascript"
        elif re.search(r'package\s+[\w.]+;|public\s+(class|interface|enum)', content):
            return "java"
        
        return "unknown"
    
    def parse(self, content: str, file_path: str) -> List[CodeEntity]:
        """解析代码文件，提取代码实体"""
        language = self.detect_language(content, file_path)
        
        if language == "unknown":
            return []
        
        try:
            if language == "python":
                return self._parse_python(content, file_path)
            else:
                return self._parse_with_tree_sitter(content, file_path, language)
        except Exception as e:
            raise ParseError(f"解析文件 {file_path} 失败: {str(e)}")
    
    def _parse_python(self, content: str, file_path: str) -> List[CodeEntity]:
        """使用Python AST解析Python代码"""
        entities = []
        try:
            tree = ast.parse(content)
        except SyntaxError as e:
            return []
        
        lines = content.splitlines()
        
        def extract_docstring(node: Any) -> Optional[str]:
            return ast.get_docstring(node)
        
        def process_node(node: Any, parent_id: Optional[str] = None):
            if isinstance(node, ast.ClassDef):
                entity_id = self._generate_entity_id(file_path, node.name, node.lineno)
                docstring = extract_docstring(node)
                end_lineno = node.end_lineno if hasattr(node, "end_lineno") else node.lineno
                
                entity = CodeEntity(
                    id=entity_id,
                    entity_type=EntityType.CLASS,
                    name=node.name,
                    file_path=file_path,
                    start_line=node.lineno,
                    end_line=end_lineno,
                    content="\n".join(lines[node.lineno-1:end_lineno]),
                    docstring=docstring,
                    modifiers=[decorator.id for decorator in node.decorator_list if isinstance(decorator, ast.Name)],
                    parent_id=parent_id,
                    lines_of_code=end_lineno - node.lineno + 1
                )
                entities.append(entity)
                
                # 处理类成员
                for item in node.body:
                    process_node(item, parent_id=entity_id)
                    
            elif isinstance(node, ast.FunctionDef) or isinstance(node, ast.AsyncFunctionDef):
                is_method = parent_id is not None and any(isinstance(n, ast.ClassDef) for n in ast.walk(tree) if hasattr(n, 'body') and node in n.body)
                entity_type = EntityType.METHOD if is_method else EntityType.FUNCTION
                
                entity_id = self._generate_entity_id(file_path, node.name, node.lineno)
                docstring = extract_docstring(node)
                end_lineno = node.end_lineno if hasattr(node, "end_lineno") else node.lineno
                
                # 提取参数
                params = []
                for arg in node.args.args:
                    params.append({
                        "name": arg.arg,
                        "annotation": ast.unparse(arg.annotation) if arg.annotation else None
                    })
                
                return_type = ast.unparse(node.returns) if node.returns else None
                
                entity = CodeEntity(
                    id=entity_id,
                    entity_type=entity_type,
                    name=node.name,
                    file_path=file_path,
                    start_line=node.lineno,
                    end_line=end_lineno,
                    content="\n".join(lines[node.lineno-1:end_lineno]),
                    docstring=docstring,
                    modifiers=[decorator.id for decorator in node.decorator_list if isinstance(decorator, ast.Name)],
                    parameters=params,
                    return_type=return_type,
                    parent_id=parent_id,
                    lines_of_code=end_lineno - node.lineno + 1
                )
                entities.append(entity)
                
                # 处理嵌套函数
                for item in node.body:
                    if isinstance(item, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
                        process_node(item, parent_id=entity_id)
        
        for node in ast.walk(tree):
            if isinstance(node, (ast.ClassDef, ast.FunctionDef, ast.AsyncFunctionDef)):
                # 只处理顶层节点，内部节点由递归处理
                if not any(isinstance(parent, (ast.ClassDef, ast.FunctionDef, ast.AsyncFunctionDef)) for parent in ast.walk(tree) if hasattr(parent, 'body') and node in parent.body):
                    process_node(node)
        
        return entities
    
    def _parse_with_tree_sitter(self, content: str, file_path: str, language: str) -> List[CodeEntity]:
        """使用Tree-sitter解析其他语言"""
        if language not in self.parsers:
            return []
        
        parser = self.parsers[language]
        tree = parser.parse(bytes(content, "utf8"))
        root_node = tree.root_node
        
        entities = []
        lines = content.splitlines()
        
        # 这里简化处理，不同语言的查询规则可以扩展
        query_patterns = {
            "javascript": """
                (class_declaration name: (identifier) @name) @class
                (function_declaration name: (identifier) @name) @function
                (method_definition name: (property_identifier) @name) @method
            """,
            "typescript": """
                (class_declaration name: (type_identifier) @name) @class
                (function_declaration name: (identifier) @name) @function
                (method_definition name: (property_identifier) @name) @method
                (interface_declaration name: (type_identifier) @name) @interface
            """,
            "java": """
                (class_declaration name: (identifier) @name) @class
                (method_declaration name: (identifier) @name) @method
                (interface_declaration name: (identifier) @name) @interface
                (enum_declaration name: (identifier) @name) @enum
            """,
            "cpp": """
                (class_specifier name: (type_identifier) @name) @class
                (function_definition declarator: (function_declarator declarator: (identifier) @name)) @function
            """,
            "go": """
                (type_declaration (type_spec name: (type_identifier) @name type: (struct_type)) @class)
                (function_declaration name: (identifier) @name) @function
                (method_declaration name: (field_identifier) @name) @method
            """
        }
        
        if language not in query_patterns:
            return []
        
        query = self.languages[language].query(query_patterns[language])
        captures = query.captures(root_node)
        
        for node, capture_name in captures:
            if capture_name in ["class", "interface", "enum"]:
                entity_type = EntityType.CLASS if capture_name == "class" else \
                             EntityType.INTERFACE if capture_name == "interface" else \
                             EntityType.ENUM
                
                name_node = [n for n, c in captures if c == "name" and n.start_byte >= node.start_byte and n.end_byte <= node.end_byte][0]
                name = content[name_node.start_byte:name_node.end_byte]
                
                entity_id = self._generate_entity_id(file_path, name, node.start_point[0] + 1)
                
                entity = CodeEntity(
                    id=entity_id,
                    entity_type=entity_type,
                    name=name,
                    file_path=file_path,
                    start_line=node.start_point[0] + 1,
                    end_line=node.end_point[0] + 1,
                    content="\n".join(lines[node.start_point[0]:node.end_point[0]+1]),
                    lines_of_code=node.end_point[0] - node.start_point[0] + 1
                )
                entities.append(entity)
                
            elif capture_name in ["function", "method"]:
                entity_type = EntityType.FUNCTION if capture_name == "function" else EntityType.METHOD
                
                name_node = [n for n, c in captures if c == "name" and n.start_byte >= node.start_byte and n.end_byte <= node.end_byte][0]
                name = content[name_node.start_byte:name_node.end_byte]
                
                entity_id = self._generate_entity_id(file_path, name, node.start_point[0] + 1)
                
                entity = CodeEntity(
                    id=entity_id,
                    entity_type=entity_type,
                    name=name,
                    file_path=file_path,
                    start_line=node.start_point[0] + 1,
                    end_line=node.end_point[0] + 1,
                    content="\n".join(lines[node.start_point[0]:node.end_point[0]+1]),
                    lines_of_code=node.end_point[0] - node.start_point[0] + 1
                )
                entities.append(entity)
        
        return entities
    
    def _generate_entity_id(self, file_path: str, name: str, line: int) -> str:
        """生成实体唯一ID"""
        key = f"{file_path}:{name}:{line}"
        return hashlib.md5(key.encode()).hexdigest()[:16]
