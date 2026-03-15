"""
增强版代码解析器 - 支持提取完整的软件架构信息
支持：类/接口/函数/变量/导入/调用关系/异常处理/事件机制等
"""
import ast
import re
import hashlib
from typing import List, Optional, Any, Dict, Tuple, Set
from pathlib import Path
from collections import defaultdict

try:
    from tree_sitter import Language, Parser, QueryCursor
    TREE_SITTER_VERSION = "new"
except ImportError:
    from tree_sitter import Language, Parser
    TREE_SITTER_VERSION = "old"

from .models import (
    CodeEntity, EntityType, Relationship, RelationshipType, 
    Visibility, AnalysisResult
)
from .exceptions import ParseError


def _get_language(module):
    """兼容不同版本的tree-sitter语言获取方式"""
    if hasattr(module, 'Language'):
        return module.Language
    if hasattr(module, 'language_typescript'):
        try:
            return Language(module.language_typescript())
        except:
            pass
    if hasattr(module, 'language'):
        try:
            return Language(module.language())
        except:
            pass
    raise ImportError(f"无法从 {module.__name__} 获取语言定义")


def _create_parser(language):
    """创建解析器，兼容不同API版本"""
    try:
        return Parser(language)
    except TypeError:
        parser = Parser()
        parser.set_language(language)
        return parser


class EnhancedCodeParser:
    """增强版多语言代码解析器"""
    
    def __init__(self):
        self.parsers = {}
        self.languages = {}
        self._init_languages()
        
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
        """初始化语言解析器"""
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
        
        if re.search(r'^import\s+|def\s+\w+\s*\(|class\s+\w+(\(|\s*:)', content, re.MULTILINE):
            return "python"
        elif re.search(r'function\s+\w+|const\s+\w+\s*=|let\s+\w+|var\s+\w+', content):
            return "javascript"
        elif re.search(r'package\s+[\w.]+;|public\s+(class|interface|enum)', content):
            return "java"
        
        return "unknown"
    
    def parse(self, content: str, file_path: str) -> Tuple[List[CodeEntity], List[Relationship]]:
        """解析代码文件，提取代码实体和关系"""
        language = self.detect_language(content, file_path)
        
        if language == "unknown":
            return [], []
        
        try:
            if language == "python":
                return self._parse_python_enhanced(content, file_path)
            else:
                return self._parse_with_tree_sitter_enhanced(content, file_path, language)
        except Exception as e:
            raise ParseError(f"解析文件 {file_path} 失败: {str(e)}")
    
    def _parse_python_enhanced(self, content: str, file_path: str) -> Tuple[List[CodeEntity], List[Relationship]]:
        """增强版Python解析器"""
        entities = []
        relationships = []
        
        try:
            tree = ast.parse(content)
        except SyntaxError:
            return [], []
        
        lines = content.splitlines()
        entity_map = {}  # 用于建立关系
        
        # 1. 提取模块级导入
        imports = self._extract_imports(tree, file_path, lines)
        entities.extend(imports['entities'])
        relationships.extend(imports['relationships'])
        
        # 2. 提取模块级变量和常量
        variables = self._extract_module_variables(tree, file_path, lines)
        entities.extend(variables)
        
        # 3. 提取类
        for node in ast.walk(tree):
            if isinstance(node, ast.ClassDef):
                class_entities, class_rels = self._extract_class(node, file_path, lines, content)
                entities.extend(class_entities)
                relationships.extend(class_rels)
        
        # 4. 提取顶层函数
        for node in ast.iter_child_nodes(tree):
            if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                func_entities, func_rels = self._extract_function(node, file_path, lines, content, None)
                entities.extend(func_entities)
                relationships.extend(func_rels)
        
        # 5. 分析调用关系
        call_rels = self._extract_call_relationships(tree, file_path, entities)
        relationships.extend(call_rels)
        
        return entities, relationships
    
    def _extract_imports(self, tree: ast.AST, file_path: str, lines: List[str]) -> Dict:
        """提取导入语句"""
        entities = []
        relationships = []
        
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                for alias in node.names:
                    entity = CodeEntity(
                        id=self._generate_entity_id(file_path, f"import_{alias.name}", node.lineno),
                        entity_type=EntityType.IMPORT,
                        name=alias.name,
                        file_path=file_path,
                        start_line=node.lineno,
                        end_line=node.end_lineno or node.lineno,
                        content=lines[node.lineno - 1] if node.lineno <= len(lines) else "",
                        metadata={"module": alias.name, "alias": alias.asname}
                    )
                    entities.append(entity)
            
            elif isinstance(node, ast.ImportFrom):
                module = node.module or ""
                for alias in node.names:
                    entity = CodeEntity(
                        id=self._generate_entity_id(file_path, f"import_{module}.{alias.name}", node.lineno),
                        entity_type=EntityType.IMPORT,
                        name=f"{module}.{alias.name}" if module else alias.name,
                        file_path=file_path,
                        start_line=node.lineno,
                        end_line=node.end_lineno or node.lineno,
                        content=lines[node.lineno - 1] if node.lineno <= len(lines) else "",
                        metadata={"module": module, "name": alias.name, "alias": alias.asname}
                    )
                    entities.append(entity)
        
        return {"entities": entities, "relationships": relationships}
    
    def _extract_module_variables(self, tree: ast.AST, file_path: str, lines: List[str]) -> List[CodeEntity]:
        """提取模块级变量和常量"""
        entities = []
        
        for node in ast.iter_child_nodes(tree):
            if isinstance(node, ast.Assign):
                for target in node.targets:
                    if isinstance(target, ast.Name):
                        # 判断是否为常量（大写）
                        is_constant = target.id.isupper() or target.id.startswith('_')
                        entity = CodeEntity(
                            id=self._generate_entity_id(file_path, target.id, node.lineno),
                            entity_type=EntityType.CONSTANT if is_constant else EntityType.VARIABLE,
                            name=target.id,
                            file_path=file_path,
                            start_line=node.lineno,
                            end_line=node.end_lineno or node.lineno,
                            content=lines[node.lineno - 1] if node.lineno <= len(lines) else "",
                            metadata={"value": ast.unparse(node.value) if hasattr(ast, 'unparse') else ""}
                        )
                        entities.append(entity)
        
        return entities
    
    def _extract_class(self, node: ast.ClassDef, file_path: str, lines: List[str], content: str) -> Tuple[List[CodeEntity], List[Relationship]]:
        """提取类及其成员"""
        entities = []
        relationships = []
        
        class_id = self._generate_entity_id(file_path, node.name, node.lineno)
        end_lineno = node.end_lineno if hasattr(node, "end_lineno") else node.lineno
        
        # 提取基类
        bases = []
        for base in node.bases:
            if isinstance(base, ast.Name):
                bases.append(base.id)
            elif isinstance(base, ast.Attribute):
                bases.append(ast.unparse(base) if hasattr(ast, 'unparse') else "")
        
        # 提取装饰器
        decorators = []
        for dec in node.decorator_list:
            if isinstance(dec, ast.Name):
                decorators.append(dec.id)
            elif isinstance(dec, ast.Attribute):
                decorators.append(ast.unparse(dec) if hasattr(ast, 'unparse') else "")
            elif isinstance(dec, ast.Call):
                if isinstance(dec.func, ast.Name):
                    decorators.append(dec.func.id)
        
        # 提取属性
        attributes = []
        for item in node.body:
            if isinstance(item, ast.AnnAssign) and isinstance(item.target, ast.Name):
                attributes.append({
                    "name": item.target.id,
                    "type": ast.unparse(item.annotation) if item.annotation and hasattr(ast, 'unparse') else None
                })
        
        class_entity = CodeEntity(
            id=class_id,
            entity_type=EntityType.CLASS,
            name=node.name,
            file_path=file_path,
            start_line=node.lineno,
            end_line=end_lineno,
            content="\n".join(lines[node.lineno - 1:end_lineno]),
            docstring=ast.get_docstring(node),
            decorators=decorators,
            bases=bases,
            attributes=attributes,
            lines_of_code=end_lineno - node.lineno + 1,
            visibility=self._get_visibility(node.name)
        )
        entities.append(class_entity)
        
        # 添加继承关系
        for base in bases:
            base_id = self._generate_entity_id(file_path, base, node.lineno)  # 临时ID，后续会修正
            rel = Relationship(
                source_id=class_id,
                target_id=base_id,
                relationship_type=RelationshipType.INHERITS,
                metadata={"base_name": base}
            )
            relationships.append(rel)
        
        # 提取类成员
        children_ids = []
        for item in node.body:
            if isinstance(item, (ast.FunctionDef, ast.AsyncFunctionDef)):
                func_entities, func_rels = self._extract_function(item, file_path, lines, content, class_id)
                entities.extend(func_entities)
                relationships.extend(func_rels)
                if func_entities:
                    children_ids.append(func_entities[0].id)
            elif isinstance(item, ast.AnnAssign):
                # 类属性
                if isinstance(item.target, ast.Name):
                    attr_entity = CodeEntity(
                        id=self._generate_entity_id(file_path, f"{node.name}.{item.target.id}", item.lineno),
                        entity_type=EntityType.ATTRIBUTE,
                        name=item.target.id,
                        file_path=file_path,
                        start_line=item.lineno,
                        end_line=item.end_lineno or item.lineno,
                        content=lines[item.lineno - 1] if item.lineno <= len(lines) else "",
                        parent_id=class_id,
                        metadata={"type": ast.unparse(item.annotation) if item.annotation and hasattr(ast, 'unparse') else None}
                    )
                    entities.append(attr_entity)
                    children_ids.append(attr_entity.id)
        
        # 更新类的children_ids
        class_entity.children_ids = children_ids
        
        return entities, relationships
    
    def _extract_function(self, node, file_path: str, lines: List[str], content: str, parent_id: Optional[str]) -> Tuple[List[CodeEntity], List[Relationship]]:
        """提取函数/方法"""
        entities = []
        relationships = []
        
        is_method = parent_id is not None
        func_name = node.name
        
        # 判断特殊方法
        entity_type = EntityType.METHOD if is_method else EntityType.FUNCTION
        if func_name == "__init__":
            entity_type = EntityType.CONSTRUCTOR
        elif func_name.startswith("__") and func_name.endswith("__"):
            entity_type = EntityType.METHOD
        
        func_id = self._generate_entity_id(file_path, func_name, node.lineno)
        end_lineno = node.end_lineno if hasattr(node, "end_lineno") else node.lineno
        
        # 提取参数
        params = []
        for arg in node.args.args:
            params.append({
                "name": arg.arg,
                "type": ast.unparse(arg.annotation) if arg.annotation and hasattr(ast, 'unparse') else None
            })
        
        # 处理 *args 和 **kwargs
        if node.args.vararg:
            params.append({"name": f"*{node.args.vararg.arg}", "type": None})
        if node.args.kwarg:
            params.append({"name": f"**{node.args.kwarg.arg}", "type": None})
        
        # 提取返回类型
        return_type = ast.unparse(node.returns) if node.returns and hasattr(ast, 'unparse') else None
        
        # 提取装饰器
        decorators = []
        for dec in node.decorator_list:
            if isinstance(dec, ast.Name):
                decorators.append(dec.id)
            elif isinstance(dec, ast.Attribute):
                decorators.append(ast.unparse(dec) if hasattr(ast, 'unparse') else "")
        
        # 提取异常（raise语句）
        raises = []
        for child in ast.walk(node):
            if isinstance(child, ast.Raise):
                if isinstance(child.exc, ast.Name):
                    raises.append(child.exc.id)
                elif isinstance(child, ast.Call) and isinstance(child.exc.func, ast.Name):
                    raises.append(child.exc.func.id)
        
        # 计算复杂度
        complexity = self._calculate_complexity(node)
        
        func_entity = CodeEntity(
            id=func_id,
            entity_type=entity_type,
            name=func_name,
            file_path=file_path,
            start_line=node.lineno,
            end_line=end_lineno,
            content="\n".join(lines[node.lineno - 1:end_lineno]),
            docstring=ast.get_docstring(node),
            decorators=decorators,
            parameters=params,
            return_type=return_type,
            parent_id=parent_id,
            raises=raises,
            complexity=complexity,
            lines_of_code=end_lineno - node.lineno + 1,
            visibility=self._get_visibility(func_name),
            modifiers=["async"] if isinstance(node, ast.AsyncFunctionDef) else []
        )
        entities.append(func_entity)
        
        # 添加异常抛出关系
        for exc in raises:
            exc_id = self._generate_entity_id(file_path, exc, node.lineno)
            rel = Relationship(
                source_id=func_id,
                target_id=exc_id,
                relationship_type=RelationshipType.THROWS,
                metadata={"exception": exc}
            )
            relationships.append(rel)
        
        return entities, relationships
    
    def _extract_call_relationships(self, tree: ast.AST, file_path: str, entities: List[CodeEntity]) -> List[Relationship]:
        """提取调用关系"""
        relationships = []
        
        # 构建实体映射
        entity_map = {e.name: e for e in entities}
        
        # 遍历所有函数/方法
        for node in ast.walk(tree):
            if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                caller_id = self._generate_entity_id(file_path, node.name, node.lineno)
                
                # 查找函数内的所有调用
                for child in ast.walk(node):
                    if isinstance(child, ast.Call):
                        callee_name = None
                        
                        if isinstance(child.func, ast.Name):
                            callee_name = child.func.id
                        elif isinstance(child.func, ast.Attribute):
                            callee_name = child.func.attr
                        
                        if callee_name:
                            # 创建调用关系
                            callee_id = self._generate_entity_id(file_path, callee_name, child.lineno)
                            rel = Relationship(
                                source_id=caller_id,
                                target_id=callee_id,
                                relationship_type=RelationshipType.CALLS,
                                call_site=f"{file_path}:{child.lineno}",
                                metadata={"callee_name": callee_name}
                            )
                            relationships.append(rel)
        
        return relationships
    
    def _calculate_complexity(self, node: ast.AST) -> int:
        """计算圈复杂度"""
        complexity = 1  # 基础复杂度
        
        for child in ast.walk(node):
            # 条件分支
            if isinstance(child, (ast.If, ast.While, ast.For, ast.ExceptHandler)):
                complexity += 1
            # 逻辑运算符
            elif isinstance(child, ast.BoolOp):
                complexity += len(child.values) - 1
            # 条件表达式
            elif isinstance(child, ast.IfExp):
                complexity += 1
            # and/or 表达式
            elif isinstance(child, ast.BoolOp):
                complexity += len(child.values) - 1
        
        return complexity
    
    def _get_visibility(self, name: str) -> Visibility:
        """根据命名判断可见性"""
        if name.startswith('__') and name.endswith('__'):
            return Visibility.PUBLIC  # 魔法方法
        elif name.startswith('__'):
            return Visibility.PRIVATE
        elif name.startswith('_'):
            return Visibility.PROTECTED
        else:
            return Visibility.PUBLIC
    
    def _parse_with_tree_sitter_enhanced(self, content: str, file_path: str, language: str) -> Tuple[List[CodeEntity], List[Relationship]]:
        """增强版Tree-sitter解析器"""
        if language not in self.parsers:
            return [], []
        
        entities = []
        relationships = []
        
        parser = self.parsers[language]
        tree = parser.parse(bytes(content, "utf8"))
        root_node = tree.root_node
        lines = content.splitlines()
        
        # 扩展的查询模式
        query_patterns = {
            "javascript": """
                (import_statement) @import
                (class_declaration name: (identifier) @class_name) @class
                (function_declaration name: (identifier) @func_name) @function
                (method_definition name: (property_identifier) @method_name) @method
                (variable_declarator name: (identifier) @var_name) @variable
                (call_expression) @call
            """,
            "typescript": """
                (import_statement) @import
                (class_declaration name: (type_identifier) @class_name) @class
                (interface_declaration name: (type_identifier) @interface_name) @interface
                (function_declaration name: (identifier) @func_name) @function
                (method_definition name: (property_identifier) @method_name) @method
                (enum_declaration name: (identifier) @enum_name) @enum
                (type_alias_declaration name: (type_identifier) @type_name) @type
            """,
            "java": """
                (import_declaration) @import
                (class_declaration name: (identifier) @class_name) @class
                (interface_declaration name: (identifier) @interface_name) @interface
                (method_declaration name: (identifier) @method_name) @method
                (enum_declaration name: (identifier) @enum_name) @enum
                (field_declaration) @field
            """,
            "cpp": """
                (preproc_include) @import
                (class_specifier name: (type_identifier) @class_name) @class
                (struct_specifier name: (type_identifier) @struct_name) @struct
                (function_definition declarator: (function_declarator declarator: (identifier) @func_name)) @function
                (declaration declarator: (identifier) @var_name) @variable
            """,
            "go": """
                (import_declaration) @import
                (type_declaration (type_spec name: (type_identifier) @type_name)) @type
                (function_declaration name: (identifier) @func_name) @function
                (method_declaration name: (field_identifier) @method_name) @method
                (var_declaration) @variable
            """
        }
        
        if language not in query_patterns:
            return [], []
        
        try:
            query = self.languages[language].query(query_patterns[language])
            
            # 使用QueryCursor执行查询 (新版API)
            cursor = QueryCursor(query)
            
            # 处理捕获结果
            if hasattr(cursor, 'captures'):
                # captures方法返回字典格式
                captures_dict = cursor.captures(root_node)
                for capture_name, nodes in captures_dict.items():
                    for node in nodes:
                        entity = self._process_tree_sitter_capture(
                            node, capture_name, content, lines, file_path
                        )
                        if entity:
                            entities.append(entity)
            elif hasattr(cursor, 'matches'):
                # matches方法返回列表格式
                matches = cursor.matches(root_node)
                for match in matches:
                    for capture in match[1]:  # match[1]包含捕获列表
                        node = capture[0]
                        capture_name = capture[1]
                        entity = self._process_tree_sitter_capture(
                            node, capture_name, content, lines, file_path
                        )
                        if entity:
                            entities.append(entity)
            else:
                print(f"Tree-sitter查询失败: QueryCursor没有可用的查询方法")
                return [], []
        
        except Exception as e:
            print(f"Tree-sitter查询失败: {e}")
        
        # 提取调用关系（Java等语言）
        if language in ["java", "javascript", "typescript"]:
            call_rels = self._extract_tree_sitter_call_relationships(
                root_node, content, file_path, entities
            )
            relationships.extend(call_rels)
        
        return entities, relationships
    
    def _process_tree_sitter_capture(self, node, capture_name: str, content: str, 
                                      lines: List[str], file_path: str) -> Optional[CodeEntity]:
        """处理Tree-sitter捕获的节点"""
        start_line = node.start_point[0] + 1
        end_line = node.end_point[0] + 1
        
        entity_type_map = {
            "class": EntityType.CLASS,
            "interface": EntityType.INTERFACE,
            "enum": EntityType.ENUM,
            "function": EntityType.FUNCTION,
            "method": EntityType.METHOD,
            "variable": EntityType.VARIABLE,
            "import": EntityType.IMPORT,
            "struct": EntityType.STRUCT,
            "type": EntityType.CLASS,
            "field": EntityType.ATTRIBUTE
        }
        
        # 提取名称 - 改进版
        name = ""
        
        # 方法1: 从Query捕获的节点中提取名称
        # capture_name可能是 "class_name", "method_name" 等
        if capture_name.endswith("_name"):
            # 查找对应名称的子节点
            for child in node.children:
                if child.type in ["identifier", "type_identifier", "property_identifier", "field_identifier"]:
                    name = content[child.start_byte:child.end_byte].strip()
                    break
        
        # 方法2: 如果没有找到，尝试从节点类型推断
        if not name:
            # 对于method_declaration，方法名通常在identifier类型的子节点中
            if node.type == "method_declaration":
                # Java方法声明结构: [modifiers] [type] identifier (parameters) [throws] {body}
                for child in node.children:
                    # 查找方法标识符
                    if child.type == "identifier":
                        potential_name = content[child.start_byte:child.end_byte].strip()
                        # 过滤掉看起来像类型名的标识符（如void, String等）
                        if potential_name and not potential_name[0].isupper() and potential_name not in ['void', 'int', 'long', 'boolean', 'double', 'float', 'char', 'byte', 'short']:
                            name = potential_name
                            break
                    # 也检查field_identifier
                    elif child.type == "field_identifier":
                        name = content[child.start_byte:child.end_byte].strip()
                        break
            
            # 对于class_declaration
            elif node.type == "class_declaration":
                for child in node.children:
                    if child.type == "identifier":
                        name = content[child.start_byte:child.end_byte].strip()
                        break
            
            # 对于interface_declaration
            elif node.type == "interface_declaration":
                for child in node.children:
                    if child.type == "identifier":
                        name = content[child.start_byte:child.end_byte].strip()
                        break
            
            # 对于field_declaration（属性）
            elif node.type == "field_declaration":
                # 查找variable_declarator
                for child in node.children:
                    if child.type == "variable_declarator":
                        for subchild in child.children:
                            if subchild.type == "identifier":
                                name = content[subchild.start_byte:subchild.end_byte].strip()
                                break
                        if name:
                            break
        
        # 方法3: 如果仍然没有找到，使用通用方法
        if not name:
            for child in node.children:
                if child.type in ["identifier", "type_identifier", "property_identifier", "field_identifier"]:
                    potential_name = content[child.start_byte:child.end_byte].strip()
                    if potential_name and len(potential_name) > 0:
                        name = potential_name
                        break
        
        # 过滤不合法的名称
        if not name or len(name) < 1 or '\n' in name or '{' in name or '}' in name:
            # 尝试从节点内容中提取第一行作为标识
            node_content = content[node.start_byte:node.end_byte]
            first_line = node_content.split('\n')[0].strip()
            # 提取可能的名称
            import re
            
            # 对于方法：public/private/protected ... name(
            match = re.search(r'(?:public|private|protected|static)?\s*(?:\w+\s+)?(\w+)\s*\(', first_line)
            if match:
                name = match.group(1)
            # 对于字段声明：public static final Type NAME =
            elif 'field' in capture_name or node.type == 'field_declaration':
                # 提取常量名或变量名
                match = re.search(r'(\w+)\s*[;=]', first_line)
                if match:
                    name = match.group(1)
            # 对于import声明
            elif 'import' in capture_name or node.type == 'import_declaration':
                # 提取导入的类名或包名
                match = re.search(r'import\s+(?:static\s+)?([^;]+)', first_line)
                if match:
                    import_path = match.group(1).strip()
                    # 取最后一部分作为名称
                    name = import_path.split('.')[-1] if '.' in import_path else import_path
            else:
                # 尝试提取任何标识符
                match = re.search(r'\b([a-zA-Z_][a-zA-Z0-9_]*)\b', first_line)
                if match:
                    name = match.group(1)
                else:
                    name = f"unknown_{capture_name}"
        
        # 确定实体类型
        entity_type_key = capture_name.replace("_name", "").replace("_", "")
        if capture_name == "method":
            entity_type_key = "method"
        elif capture_name == "class":
            entity_type_key = "class"
        
        entity_type = entity_type_map.get(entity_type_key, EntityType.UNKNOWN)
        
        # 获取节点内容
        node_content = "\n".join(lines[start_line - 1:end_line]) if start_line <= len(lines) else ""
        
        entity = CodeEntity(
            id=self._generate_entity_id(file_path, name, start_line),
            entity_type=entity_type,
            name=name,
            file_path=file_path,
            start_line=start_line,
            end_line=end_line,
            content=node_content,
            lines_of_code=end_line - start_line + 1
        )
        
        return entity
    
    def _extract_tree_sitter_call_relationships(self, root_node, content: str, 
                                                  file_path: str, entities: List[CodeEntity]) -> List[Relationship]:
        """从tree-sitter AST中提取调用关系"""
        relationships = []
        
        # 构建实体映射（用于快速查找）
        entity_map = {}
        method_entities = []
        for entity in entities:
            if entity.entity_type in [EntityType.METHOD, EntityType.FUNCTION]:
                method_entities.append(entity)
                entity_map[(entity.start_line, entity.end_line)] = entity
        
        # 遍历AST查找方法调用
        def find_method_calls(node, current_method=None):
            """递归查找方法调用"""
            # 检查是否进入方法声明
            if node.type == "method_declaration":
                # 找到方法名
                method_name = None
                for child in node.children:
                    if child.type == "identifier":
                        method_name = content[child.start_byte:child.end_byte]
                        break
                
                if method_name:
                    # 查找对应的实体
                    start_line = node.start_point[0] + 1
                    end_line = node.end_point[0] + 1
                    for entity in method_entities:
                        if entity.start_line == start_line and entity.name == method_name:
                            current_method = entity
                            break
                
                # 继续遍历子节点
                for child in node.children:
                    find_method_calls(child, current_method)
                
                return
            
            # 检查是否是方法调用
            if node.type == "method_invocation":
                if current_method:
                    # 提取被调用方法名
                    call_name = None
                    for child in node.children:
                        if child.type == "identifier":
                            call_name = content[child.start_byte:child.end_byte]
                            break
                    
                    if call_name:
                        # 创建调用关系
                        call_line = node.start_point[0] + 1
                        callee_id = self._generate_entity_id(file_path, call_name, call_line)
                        
                        rel = Relationship(
                            source_id=current_method.id,
                            target_id=callee_id,
                            relationship_type=RelationshipType.CALLS,
                            call_site=f"{file_path}:{call_line}",
                            metadata={
                                "caller_name": current_method.name,
                                "callee_name": call_name
                            }
                        )
                        relationships.append(rel)
            
            # 递归遍历子节点
            for child in node.children:
                find_method_calls(child, current_method)
        
        # 从根节点开始遍历
        find_method_calls(root_node)
        
        return relationships
    
    def _generate_entity_id(self, file_path: str, name: str, line: int) -> str:
        """生成实体唯一ID"""
        key = f"{file_path}:{name}:{line}"
        return hashlib.md5(key.encode()).hexdigest()[:16]


# 向后兼容
CodeParser = EnhancedCodeParser
