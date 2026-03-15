import ast
import re
import hashlib
from typing import List, Optional, Any
from pathlib import Path

# 尝试导入不同版本的tree-sitter
try:
    from tree_sitter import Language, Parser
    TREE_SITTER_VERSION = "new"
except ImportError:
    from tree_sitter import Language, Parser
    TREE_SITTER_VERSION = "old"

from .models import CodeEntity, EntityType, Relationship, RelationshipType, AnalysisResult
from .exceptions import ParseError

def _get_language(module):
    """兼容不同版本的tree-sitter语言获取方式"""
    # 新版本 tree-sitter (>= 0.21) 直接暴露 Language
    if hasattr(module, 'Language'):
        return module.Language
    # TypeScript 特殊处理
    if hasattr(module, 'language_typescript'):
        try:
            return Language(module.language_typescript())
        except:
            pass
    # 中间版本使用 language() 函数
    if hasattr(module, 'language'):
        try:
            return Language(module.language())
        except:
            pass
    raise ImportError(f"无法从 {module.__name__} 获取语言定义")

def _create_parser(language):
    """创建解析器，兼容不同API版本"""
    try:
        # 新版本 API
        return Parser(language)
    except TypeError:
        # 旧版本 API
        parser = Parser()
        parser.set_language(language)
        return parser

class CodeParser:
    """多语言代码解析器"""
    
    def __init__(self):
        # 初始化Tree-sitter解析器
        self.parsers = {}
        self.languages = {}
        
        # 延迟加载语言模块
        self._init_languages()
        
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
    
    def _init_languages(self):
        """初始化语言解析器，处理可能的导入错误"""
        language_modules = {
            "python": "tree_sitter_python",
            "javascript": "tree_sitter_javascript",
            "typescript": "tree_sitter_typescript",
            "java": "tree_sitter_java",
            "cpp": "tree_sitter_cpp",
            "go": "tree_sitter_go"
        }
        
        for lang, module_name in language_modules.items():
            try:
                module = __import__(module_name)
                language = _get_language(module)
                self.languages[lang] = language
                self.parsers[lang] = _create_parser(language)
            except Exception as e:
                print(f"警告: 无法加载 {lang} 语言支持: {e}")
                continue
    
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
    
    def parse(self, content: str, file_path: str) -> AnalysisResult:
        """解析代码文件，提取代码实体和关系"""
        language = self.detect_language(content, file_path)
        
        if language == "unknown":
            return AnalysisResult(
                file_path=file_path,
                file_hash=self._get_file_hash(content),
                language=language,
                entities=[],
                relationships=[],
                lines_of_code=len(content.splitlines())
            )
        
        try:
            if language == "python":
                entities = self._parse_python(content, file_path)
                # 从元数据中提取关系
                relationships = []
                for entity in entities:
                    if entity.metadata and "relationships" in entity.metadata:
                        relationships.extend([Relationship(**r) for r in entity.metadata["relationships"]])
                
                return AnalysisResult(
                    file_path=file_path,
                    file_hash=self._get_file_hash(content),
                    language=language,
                    entities=entities,
                    relationships=relationships,
                    lines_of_code=len(content.splitlines())
                )
            else:
                entities = self._parse_with_tree_sitter(content, file_path, language)
                return AnalysisResult(
                    file_path=file_path,
                    file_hash=self._get_file_hash(content),
                    language=language,
                    entities=entities,
                    relationships=[],
                    lines_of_code=len(content.splitlines())
                )
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
            """处理AST节点，避免类型检查错误"""
            try:
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
                        modifiers=[],
                        parent_id=parent_id,
                        lines_of_code=end_lineno - node.lineno + 1
                    )
                    entities.append(entity)
                    
                    # 处理类成员
                    for item in node.body:
                        process_node(item, parent_id=entity_id)
                        
                elif isinstance(node, ast.FunctionDef) or isinstance(node, ast.AsyncFunctionDef):
                    is_method = parent_id is not None
                    entity_type = EntityType.METHOD if is_method else EntityType.FUNCTION
                    
                    entity_id = self._generate_entity_id(file_path, node.name, node.lineno)
                    docstring = extract_docstring(node)
                    end_lineno = node.end_lineno if hasattr(node, "end_lineno") else node.lineno
                    
                    # 提取参数
                    params = []
                    for arg in node.args.args:
                        params.append({
                            "name": arg.arg,
                            "annotation": None
                        })
                    
                    entity = CodeEntity(
                        id=entity_id,
                        entity_type=entity_type,
                        name=node.name,
                        file_path=file_path,
                        start_line=node.lineno,
                        end_line=end_lineno,
                        content="\n".join(lines[node.lineno-1:end_lineno]),
                        docstring=docstring,
                        modifiers=[],
                        parameters=params,
                        return_type=None,
                        parent_id=parent_id,
                        lines_of_code=end_lineno - node.lineno + 1
                    )
                    entities.append(entity)
                    
                    # 处理嵌套函数
                    for item in node.body:
                        if isinstance(item, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
                            process_node(item, parent_id=entity_id)
            except Exception as e:
                # 忽略单个节点的解析错误，继续处理其他节点
                print(f"解析节点 {type(node).__name__} 时出错: {e}")
        
        # 处理顶层实体
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
    
    def _get_file_hash(self, content: str) -> str:
        """计算文件内容哈希值"""
        return hashlib.md5(content.encode()).hexdigest()
