"""
ArchAI CodeExplainer - AI驱动的代码解释与架构分析器
用于深度理解代码语义、识别架构模式、生成重构建议
"""
import os
import json
import re
from typing import List, Dict, Any, Optional, Set, Tuple
from dataclasses import dataclass, field
from enum import Enum
from collections import defaultdict, Counter
from pathlib import Path
from datetime import datetime

# 导入项目模型
import sys
sys.path.insert(0, str(Path(__file__).parent.parent.parent))
from src.gitnexus.models import (
    KnowledgeGraph, CodeEntity, EntityType, Relationship, RelationshipType,
    ArchitectureInfo, ModuleInfo, FeatureInfo, CallChain
)


class ArchitecturePattern(Enum):
    """架构模式枚举"""
    MVC = "Model-View-Controller"
    MVVM = "Model-View-ViewModel"
    MVP = "Model-View-Presenter"
    LAYERED = "Layered Architecture"
    MICROSERVICE = "Microservices"
    MONOLITHIC = "Monolithic"
    EVENT_DRIVEN = "Event-Driven"
    CQRS = "Command Query Responsibility Segregation"
    REPOSITORY = "Repository Pattern"
    FACTORY = "Factory Pattern"
    SINGLETON = "Singleton Pattern"
    OBSERVER = "Observer Pattern"
    STRATEGY = "Strategy Pattern"
    DECORATOR = "Decorator Pattern"
    DEPENDENCY_INJECTION = "Dependency Injection"
    CLEAN_ARCHITECTURE = "Clean Architecture"
    HEXAGONAL = "Hexagonal Architecture"
    UNKNOWN = "Unknown"


class CodeSmellType(Enum):
    """代码异味类型"""
    LONG_METHOD = "long_method"
    LARGE_CLASS = "large_class"
    LONG_PARAMETER_LIST = "long_parameter_list"
    DUPLICATED_CODE = "duplicated_code"
    DEAD_CODE = "dead_code"
    FEATURE_ENVY = "feature_envy"
    DATA_CLUMP = "data_clump"
    PRIMITIVE_OBSESSION = "primitive_obsession"
    SWITCH_STATEMENTS = "switch_statements"
    SPECULATIVE_GENERALITY = "speculative_generality"
    GOD_CLASS = "god_class"
    CIRCULAR_DEPENDENCY = "circular_dependency"
    SHOTGUN_SURGERY = "shotgun_surgery"
    PARALLEL_INHERITANCE = "parallel_inheritance"
    MIDDLE_MAN = "middle_man"
    INAPPROPRIATE_INTIMACY = "inappropriate_intimacy"
    ALTERNATIVE_CLASSES = "alternative_classes"
    INCOMPLETE_LIBRARY = "incomplete_library"
    REFUSED_BEQUEST = "refused_bequest"
    COMMENTS = "comments"


@dataclass
class CodeExplanation:
    """代码解释结果"""
    entity_id: str
    entity_name: str
    
    # 功能解释
    purpose: str                                    # 代码目的
    functionality: str                              # 功能描述
    business_logic: str                            # 业务逻辑
    
    # 语义分析
    intent: str                                    # 开发者意图
    invariants: List[str] = field(default_factory=list)  # 不变量
    preconditions: List[str] = field(default_factory=list)  # 前置条件
    postconditions: List[str] = field(default_factory=list)  # 后置条件
    
    # 依赖分析
    dependencies: List[str] = field(default_factory=list)
    dependents: List[str] = field(default_factory=list)
    
    # 复杂度分析
    complexity_score: float = 0.0
    maintainability_index: float = 0.0
    technical_debt_score: float = 0.0
    
    # 代码异味
    code_smells: List[Dict[str, Any]] = field(default_factory=list)
    
    # 重构建议
    refactoring_suggestions: List[Dict[str, Any]] = field(default_factory=list)
    
    # 架构模式
    detected_patterns: List[ArchitecturePattern] = field(default_factory=list)
    
    # 元数据
    confidence: float = 1.0
    analysis_timestamp: str = field(default_factory=lambda: datetime.now().isoformat())


@dataclass
class ArchitectureAnalysisResult:
    """架构分析结果"""
    # 整体架构模式
    primary_pattern: ArchitecturePattern
    secondary_patterns: List[ArchitecturePattern]
    
    # 分层分析
    layers: Dict[str, Dict[str, Any]]
    
    # 模块关系
    module_dependencies: Dict[str, List[str]]
    circular_dependencies: List[List[str]]
    
    # 设计问题
    design_issues: List[Dict[str, Any]]
    
    # 改进建议
    improvement_suggestions: List[Dict[str, Any]]
    
    # 重构热点
    refactoring_hotspots: List[Dict[str, Any]]
    
    # 统计信息
    statistics: Dict[str, Any]


class ArchAICodeExplainer:
    """AI驱动的代码解释器"""
    
    def __init__(self, graph: KnowledgeGraph, ai_client=None):
        self.graph = graph
        self.ai_client = ai_client
        self._build_analysis_indexes()
    
    def _build_analysis_indexes(self):
        """构建分析索引"""
        # 实体索引
        self.entities_by_file: Dict[str, List[CodeEntity]] = defaultdict(list)
        self.entities_by_type: Dict[EntityType, List[CodeEntity]] = defaultdict(list)
        self.entities_by_module: Dict[str, List[CodeEntity]] = defaultdict(list)
        
        # 调用关系索引
        self.call_graph: Dict[str, Set[str]] = defaultdict(set)  # caller -> callees
        self.reverse_call_graph: Dict[str, Set[str]] = defaultdict(set)  # callee -> callers
        self.call_counts: Counter = Counter()
        
        # 依赖关系索引
        self.dependency_graph: Dict[str, Set[str]] = defaultdict(set)
        self.reverse_dependency_graph: Dict[str, Set[str]] = defaultdict(set)
        
        # 构建索引
        for entity in self.graph.entities.values():
            self.entities_by_file[entity.file_path].append(entity)
            self.entities_by_type[entity.entity_type].append(entity)
            if entity.module_path:
                self.entities_by_module[entity.module_path].append(entity)
        
        for rel in self.graph.relationships:
            if rel.relationship_type == RelationshipType.CALLS:
                self.call_graph[rel.source_id].add(rel.target_id)
                self.reverse_call_graph[rel.target_id].add(rel.source_id)
                self.call_counts[rel.target_id] += 1
            elif rel.relationship_type in [RelationshipType.DEPENDS_ON, RelationshipType.IMPORTS]:
                self.dependency_graph[rel.source_id].add(rel.target_id)
                self.reverse_dependency_graph[rel.target_id].add(rel.source_id)
    
    def explain_entity(self, entity_id: str) -> CodeExplanation:
        """解释单个实体"""
        entity = self.graph.entities.get(entity_id)
        if not entity:
            raise ValueError(f"实体不存在: {entity_id}")
        
        # 基础信息
        explanation = CodeExplanation(
            entity_id=entity_id,
            entity_name=entity.name,
            purpose=self._infer_purpose(entity),
            functionality=self._extract_functionality(entity),
            business_logic=self._extract_business_logic(entity),
            intent=self._infer_intent(entity)
        )
        
        # 依赖分析
        explanation.dependencies = list(self.call_graph.get(entity_id, set()))
        explanation.dependents = list(self.reverse_call_graph.get(entity_id, set()))
        
        # 复杂度分析
        explanation.complexity_score = self._calculate_complexity(entity)
        explanation.maintainability_index = self._calculate_maintainability(entity)
        explanation.technical_debt_score = self._calculate_technical_debt(entity)
        
        # 代码异味检测
        explanation.code_smells = self._detect_code_smells(entity)
        
        # 重构建议
        explanation.refactoring_suggestions = self._generate_refactoring_suggestions(entity, explanation.code_smells)
        
        # 检测模式
        explanation.detected_patterns = self._detect_patterns_in_entity(entity)
        
        return explanation
    
    def analyze_architecture(self) -> ArchitectureAnalysisResult:
        """分析整体架构"""
        # 检测架构模式
        primary_pattern, secondary_patterns = self._detect_architecture_patterns()
        
        # 分层分析
        layers = self._analyze_layers()
        
        # 模块依赖分析
        module_deps, circular_deps = self._analyze_module_dependencies()
        
        # 设计问题检测
        design_issues = self._detect_design_issues()
        
        # 改进建议
        improvements = self._generate_improvement_suggestions(primary_pattern, design_issues)
        
        # 重构热点
        hotspots = self._identify_refactoring_hotspots()
        
        # 统计信息
        statistics = self._calculate_architecture_statistics()
        
        return ArchitectureAnalysisResult(
            primary_pattern=primary_pattern,
            secondary_patterns=secondary_patterns,
            layers=layers,
            module_dependencies=module_deps,
            circular_dependencies=circular_deps,
            design_issues=design_issues,
            improvement_suggestions=improvements,
            refactoring_hotspots=hotspots,
            statistics=statistics
        )
    
    def explain_feature(self, feature_id: str) -> Dict[str, Any]:
        """解释功能模块"""
        if not self.graph.architecture or feature_id not in self.graph.architecture.features:
            return {"error": f"功能模块不存在: {feature_id}"}
        
        feature = self.graph.architecture.features[feature_id]
        
        # 收集功能相关的所有解释
        entity_explanations = []
        for entity_id in feature.entities:
            if entity_id in self.graph.entities:
                entity_explanations.append(self.explain_entity(entity_id))
        
        return {
            "feature_id": feature_id,
            "feature_name": feature.name,
            "description": feature.description,
            "entry_points": feature.entry_points,
            "complexity": feature.complexity,
            "importance": feature.importance,
            "entity_explanations": [e.__dict__ for e in entity_explanations],
            "overall_purpose": self._infer_feature_purpose(feature),
            "business_value": self._assess_business_value(feature),
            "refactoring_priority": self._calculate_refactoring_priority(feature, entity_explanations)
        }
    
    def generate_refactoring_report(self) -> Dict[str, Any]:
        """生成重构分析报告"""
        report = {
            "generated_at": datetime.now().isoformat(),
            "project_name": self.graph.project_name,
            "summary": {},
            "architecture_analysis": {},
            "code_smells": [],
            "refactoring_suggestions": [],
            "priority_items": [],
            "statistics": {}
        }
        
        # 架构分析
        arch_analysis = self.analyze_architecture()
        report["architecture_analysis"] = {
            "primary_pattern": arch_analysis.primary_pattern.value,
            "secondary_patterns": [p.value for p in arch_analysis.secondary_patterns],
            "circular_dependencies": arch_analysis.circular_dependencies,
            "design_issues": arch_analysis.design_issues,
            "improvement_suggestions": arch_analysis.improvement_suggestions
        }
        
        # 代码异味汇总
        all_smells = []
        for entity in self.graph.entities.values():
            smells = self._detect_code_smells(entity)
            for smell in smells:
                smell["entity_id"] = entity.id
                smell["entity_name"] = entity.name
                smell["file_path"] = entity.file_path
                all_smells.append(smell)
        
        report["code_smells"] = self._group_and_rank_smells(all_smells)
        
        # 重构建议汇总
        report["refactoring_suggestions"] = arch_analysis.refactoring_hotspots
        
        # 优先级排序的项目
        report["priority_items"] = self._prioritize_refactoring_items(
            arch_analysis.refactoring_hotspots, 
            all_smells
        )
        
        # 统计信息
        report["statistics"] = {
            "total_entities": len(self.graph.entities),
            "total_smells": len(all_smells),
            "total_hotspots": len(arch_analysis.refactoring_hotspots),
            "circular_dependency_count": len(arch_analysis.circular_dependencies)
        }
        
        return report
    
    # ==================== 内部分析方法 ====================
    
    def _infer_purpose(self, entity: CodeEntity) -> str:
        """推断实体目的"""
        # 基于名称推断
        name_hints = {
            'handler': '处理请求或事件',
            'controller': '协调业务流程',
            'service': '提供业务服务',
            'manager': '管理资源或状态',
            'factory': '创建对象实例',
            'builder': '构建复杂对象',
            'adapter': '适配接口',
            'proxy': '代理访问控制',
            'observer': '观察状态变化',
            'listener': '监听事件',
            'validator': '验证数据',
            'parser': '解析数据',
            'converter': '转换数据格式',
            'calculator': '执行计算',
            'processor': '处理数据',
            'generator': '生成内容',
            'repository': '数据持久化操作',
            'dao': '数据访问对象',
            'dto': '数据传输对象',
            'vo': '值对象',
            'entity': '业务实体',
            'model': '数据模型'
        }
        
        name_lower = entity.name.lower()
        for hint, purpose in name_hints.items():
            if hint in name_lower:
                return purpose
        
        # 基于docstring推断
        if entity.docstring:
            first_line = entity.docstring.split('\n')[0].strip()
            if len(first_line) < 100:
                return first_line
        
        return f"定义{entity.entity_type.value} {entity.name}"
    
    def _extract_functionality(self, entity: CodeEntity) -> str:
        """提取功能描述"""
        parts = []
        
        # 基于类型
        if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD]:
            parts.append(f"方法 {entity.name}")
            if entity.parameters:
                params = [p.get('name', '') for p in entity.parameters[:5]]
                parts.append(f"接受参数: {', '.join(params)}")
            if entity.return_type:
                parts.append(f"返回 {entity.return_type}")
        elif entity.entity_type == EntityType.CLASS:
            parts.append(f"类 {entity.name}")
            parts.append(f"包含 {len(entity.children_ids)} 个成员")
            if entity.bases:
                parts.append(f"继承自 {', '.join(entity.bases)}")
        
        # 基于装饰器
        if entity.decorators:
            decs = [d.replace('@', '') for d in entity.decorators[:3]]
            parts.append(f"装饰器: {', '.join(decs)}")
        
        return ' | '.join(parts) if parts else entity.name
    
    def _extract_business_logic(self, entity: CodeEntity) -> str:
        """提取业务逻辑"""
        # 从docstring提取
        if entity.docstring:
            lines = entity.docstring.strip().split('\n')
            # 查找业务逻辑相关描述
            keywords = ['业务', '功能', '处理', '实现', '执行', '计算', '验证', '查询', '更新', '创建', '删除']
            for line in lines:
                for kw in keywords:
                    if kw in line:
                        return line.strip()
        
        # 从代码内容推断
        if entity.content:
            # 查找关键操作
            operations = []
            if 'validate' in entity.content.lower():
                operations.append('数据验证')
            if 'save' in entity.content.lower() or 'persist' in entity.content.lower():
                operations.append('数据持久化')
            if 'calculate' in entity.content.lower() or 'compute' in entity.content.lower():
                operations.append('计算处理')
            if 'query' in entity.content.lower() or 'find' in entity.content.lower():
                operations.append('数据查询')
            if 'transform' in entity.content.lower() or 'convert' in entity.content.lower():
                operations.append('数据转换')
            
            if operations:
                return f"涉及: {', '.join(operations)}"
        
        return "通用功能实现"
    
    def _infer_intent(self, entity: CodeEntity) -> str:
        """推断开发者意图"""
        intents = []
        
        # 基于命名推断意图
        name = entity.name.lower()
        
        if name.startswith('get_') or name.startswith('fetch_'):
            intents.append("获取数据")
        elif name.startswith('set_') or name.startswith('update_'):
            intents.append("更新状态")
        elif name.startswith('create_') or name.startswith('add_'):
            intents.append("创建资源")
        elif name.startswith('delete_') or name.startswith('remove_'):
            intents.append("删除资源")
        elif name.startswith('validate_') or name.startswith('check_'):
            intents.append("验证检查")
        elif name.startswith('process_') or name.startswith('handle_'):
            intents.append("处理流程")
        elif name.startswith('calculate_') or name.startswith('compute_'):
            intents.append("执行计算")
        elif name.startswith('parse_') or name.startswith('convert_'):
            intents.append("解析转换")
        elif 'factory' in name:
            intents.append("对象创建")
        elif 'builder' in name:
            intents.append("对象构建")
        elif 'adapter' in name:
            intents.append("接口适配")
        
        # 基于修饰符
        if 'async' in entity.modifiers:
            intents.append("异步处理")
        if 'static' in entity.modifiers:
            intents.append("工具方法")
        
        return ' | '.join(intents) if intents else "实现特定功能"
    
    def _calculate_complexity(self, entity: CodeEntity) -> float:
        """计算复杂度分数"""
        score = 0.0
        
        # 基础复杂度
        if entity.complexity:
            score += entity.complexity * 2
        
        # 代码行数影响
        score += min(entity.lines_of_code / 10, 20)
        
        # 参数数量影响
        if entity.parameters:
            score += min(len(entity.parameters), 10)
        
        # 嵌套深度影响
        if entity.nesting_depth:
            score += entity.nesting_depth * 3
        
        # 子实体数量影响
        score += min(len(entity.children_ids), 15)
        
        # 调用数量影响
        callees = self.call_graph.get(entity.id, set())
        score += min(len(callees), 10)
        
        return min(score, 100)  # 归一化到0-100
    
    def _calculate_maintainability(self, entity: CodeEntity) -> float:
        """计算可维护性指数"""
        score = 100.0
        
        # 代码行数惩罚
        if entity.lines_of_code > 50:
            score -= min((entity.lines_of_code - 50) / 2, 30)
        
        # 复杂度惩罚
        if entity.complexity and entity.complexity > 10:
            score -= min((entity.complexity - 10) * 2, 30)
        
        # 参数过多惩罚
        if entity.parameters and len(entity.parameters) > 5:
            score -= min((len(entity.parameters) - 5) * 5, 20)
        
        # 缺少文档惩罚
        if not entity.docstring:
            score -= 10
        
        # 代码异味惩罚
        smells = self._detect_code_smells(entity)
        score -= len(smells) * 5
        
        return max(score, 0)
    
    def _calculate_technical_debt(self, entity: CodeEntity) -> float:
        """计算技术债务分数"""
        debt = 0.0
        
        # 长方法债务
        if entity.lines_of_code > 30:
            debt += (entity.lines_of_code - 30) * 0.5
        
        # 高复杂度债务
        if entity.complexity and entity.complexity > 8:
            debt += (entity.complexity - 8) * 2
        
        # 缺失文档债务
        if not entity.docstring:
            debt += 5
        
        # TODO注释债务
        todos = [c for c in entity.comments if 'TODO' in c.upper()]
        debt += len(todos) * 3
        
        # 异常处理缺失债务
        if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD]:
            if not entity.raises and 'try' not in entity.content.lower():
                # 检查是否有可能抛出异常的操作
                if any(kw in entity.content.lower() for kw in ['open(', 'read', 'write', 'connect', 'request']):
                    debt += 5
        
        return debt
    
    def _detect_code_smells(self, entity: CodeEntity) -> List[Dict[str, Any]]:
        """检测代码异味"""
        smells = []
        
        # 长方法
        if entity.lines_of_code > 50:
            smells.append({
                "type": CodeSmellType.LONG_METHOD.value,
                "severity": "high" if entity.lines_of_code > 100 else "medium",
                "description": f"方法过长 ({entity.lines_of_code} 行)",
                "suggestion": "考虑拆分为多个小方法"
            })
        
        # 大类
        if entity.entity_type == EntityType.CLASS:
            if len(entity.children_ids) > 20:
                smells.append({
                    "type": CodeSmellType.LARGE_CLASS.value,
                    "severity": "high" if len(entity.children_ids) > 30 else "medium",
                    "description": f"类过大 ({len(entity.children_ids)} 个成员)",
                    "suggestion": "考虑拆分职责"
                })
        
        # 参数过多
        if entity.parameters and len(entity.parameters) > 5:
            smells.append({
                "type": CodeSmellType.LONG_PARAMETER_LIST.value,
                "severity": "high" if len(entity.parameters) > 7 else "medium",
                "description": f"参数过多 ({len(entity.parameters)} 个)",
                "suggestion": "考虑使用参数对象或配置对象"
            })
        
        # 高复杂度
        if entity.complexity and entity.complexity > 10:
            smells.append({
                "type": "high_complexity",
                "severity": "high" if entity.complexity > 20 else "medium",
                "description": f"圈复杂度过高 ({entity.complexity})",
                "suggestion": "简化条件逻辑，提取方法"
            })
        
        # 深层嵌套
        if entity.nesting_depth and entity.nesting_depth > 4:
            smells.append({
                "type": "deep_nesting",
                "severity": "high" if entity.nesting_depth > 6 else "medium",
                "description": f"嵌套过深 (深度 {entity.nesting_depth})",
                "suggestion": "使用卫语句或提取方法"
            })
        
        # 上帝类检测
        if entity.entity_type == EntityType.CLASS:
            attr_count = len(entity.attributes)
            method_count = len([cid for cid in entity.children_ids 
                               if self.graph.entities.get(cid, CodeEntity(id="", entity_type=EntityType.FUNCTION, name="", file_path="", start_line=0, end_line=0)).entity_type == EntityType.METHOD])
            
            if attr_count > 15 and method_count > 10:
                smells.append({
                    "type": CodeSmellType.GOD_CLASS.value,
                    "severity": "high",
                    "description": f"上帝类嫌疑 (属性{attr_count}, 方法{method_count})",
                    "suggestion": "拆分职责，遵循单一职责原则"
                })
        
        # 缺失文档
        if not entity.docstring and entity.entity_type in [EntityType.CLASS, EntityType.FUNCTION, EntityType.METHOD]:
            smells.append({
                "type": "missing_documentation",
                "severity": "low",
                "description": "缺少文档字符串",
                "suggestion": "添加docstring说明功能"
            })
        
        return smells
    
    def _generate_refactoring_suggestions(self, entity: CodeEntity, smells: List[Dict]) -> List[Dict[str, Any]]:
        """生成重构建议"""
        suggestions = []
        
        for smell in smells:
            suggestion = {
                "target_smell": smell["type"],
                "priority": "high" if smell["severity"] == "high" else "medium",
                "description": smell["suggestion"],
                "techniques": []
            }
            
            # 根据异味类型推荐重构技术
            if smell["type"] == CodeSmellType.LONG_METHOD.value:
                suggestion["techniques"] = [
                    "Extract Method",
                    "Replace Temp with Query",
                    "Introduce Parameter Object"
                ]
            elif smell["type"] == CodeSmellType.LARGE_CLASS.value:
                suggestion["techniques"] = [
                    "Extract Class",
                    "Extract Subclass",
                    "Extract Interface"
                ]
            elif smell["type"] == CodeSmellType.LONG_PARAMETER_LIST.value:
                suggestion["techniques"] = [
                    "Introduce Parameter Object",
                    "Preserve Whole Object",
                    "Remove Assignments to Parameters"
                ]
            elif smell["type"] == "high_complexity":
                suggestion["techniques"] = [
                    "Decompose Conditional",
                    "Consolidate Conditional Expression",
                    "Replace Nested Conditional with Guard Clauses"
                ]
            
            suggestions.append(suggestion)
        
        return suggestions
    
    def _detect_patterns_in_entity(self, entity: CodeEntity) -> List[ArchitecturePattern]:
        """检测实体中的设计模式"""
        patterns = []
        name_lower = entity.name.lower()
        
        # 单例模式
        if entity.entity_type == EntityType.CLASS:
            if 'instance' in name_lower or '_instance' in str(entity.attributes).lower():
                for method_id in entity.children_ids:
                    method = self.graph.entities.get(method_id)
                    if method and method.name == 'get_instance':
                        patterns.append(ArchitecturePattern.SINGLETON)
                        break
        
        # 工厂模式
        if 'factory' in name_lower:
            patterns.append(ArchitecturePattern.FACTORY)
        
        # 观察者模式
        if 'observer' in name_lower or 'listener' in name_lower or 'subscriber' in name_lower:
            patterns.append(ArchitecturePattern.OBSERVER)
        
        # 策略模式
        if 'strategy' in name_lower or 'context' in name_lower:
            patterns.append(ArchitecturePattern.STRATEGY)
        
        # 装饰器模式
        if entity.decorators:
            patterns.append(ArchitecturePattern.DECORATOR)
        
        # 仓储模式
        if 'repository' in name_lower or 'dao' in name_lower:
            patterns.append(ArchitecturePattern.REPOSITORY)
        
        return list(set(patterns))
    
    def _detect_architecture_patterns(self) -> Tuple[ArchitecturePattern, List[ArchitecturePattern]]:
        """检测整体架构模式"""
        patterns_found = []
        
        # 统计目录结构
        dir_keywords = defaultdict(int)
        for entity in self.graph.entities.values():
            path = entity.file_path.lower().replace('\\', '/')
            
            if 'controller' in path or 'api' in path:
                dir_keywords['controller'] += 1
            if 'service' in path:
                dir_keywords['service'] += 1
            if 'model' in path or 'entity' in path or 'domain' in path:
                dir_keywords['model'] += 1
            if 'view' in path or 'template' in path or 'ui' in path:
                dir_keywords['view'] += 1
            if 'repository' in path or 'dao' in path:
                dir_keywords['repository'] += 1
            if 'event' in path or 'message' in path or 'queue' in path:
                dir_keywords['event'] += 1
        
        # MVC检测
        if dir_keywords['controller'] > 0 and dir_keywords['model'] > 0:
            patterns_found.append(ArchitecturePattern.MVC)
        
        # MVVM检测
        if dir_keywords['view'] > 0 and dir_keywords['model'] > 0 and dir_keywords.get('viewmodel', 0) > 0:
            patterns_found.append(ArchitecturePattern.MVVM)
        
        # 分层架构检测
        if sum(1 for k in ['controller', 'service', 'repository', 'model'] if dir_keywords[k] > 0) >= 3:
            patterns_found.append(ArchitecturePattern.LAYERED)
        
        # 事件驱动检测
        if dir_keywords['event'] > 5:
            patterns_found.append(ArchitecturePattern.EVENT_DRIVEN)
        
        # 微服务检测 (基于模块数量)
        if self.graph.architecture and len(self.graph.architecture.modules) > 5:
            patterns_found.append(ArchitecturePattern.MICROSERVICE)
        
        # CQRS检测
        if dir_keywords.get('command', 0) > 0 and dir_keywords.get('query', 0) > 0:
            patterns_found.append(ArchitecturePattern.CQRS)
        
        # 确定主模式
        if patterns_found:
            primary = patterns_found[0]
        else:
            primary = ArchitecturePattern.MONOLITHIC
        
        return primary, patterns_found[1:]
    
    def _analyze_layers(self) -> Dict[str, Dict[str, Any]]:
        """分析分层架构"""
        layers = {
            "presentation": {"entities": [], "files": set(), "description": "表现层 - 处理用户交互"},
            "business": {"entities": [], "files": set(), "description": "业务层 - 核心业务逻辑"},
            "data": {"entities": [], "files": set(), "description": "数据层 - 数据存储访问"},
            "infrastructure": {"entities": [], "files": set(), "description": "基础设施层 - 通用服务"},
            "cross_cutting": {"entities": [], "files": set(), "description": "横切关注点 - 日志、安全等"}
        }
        
        for entity in self.graph.entities.values():
            path = entity.file_path.lower().replace('\\', '/')
            
            if any(p in path for p in ['api', 'controller', 'view', 'route', 'endpoint', 'handler', 'servlet']):
                layers["presentation"]["entities"].append(entity.id)
                layers["presentation"]["files"].add(entity.file_path)
            elif any(p in path for p in ['service', 'business', 'logic', 'manager', 'processor', 'domain']):
                layers["business"]["entities"].append(entity.id)
                layers["business"]["files"].add(entity.file_path)
            elif any(p in path for p in ['data', 'dao', 'repository', 'model', 'entity', 'db', 'database', 'persistence']):
                layers["data"]["entities"].append(entity.id)
                layers["data"]["files"].add(entity.file_path)
            elif any(p in path for p in ['config', 'util', 'common', 'helper', 'core', 'base', 'lib']):
                layers["infrastructure"]["entities"].append(entity.id)
                layers["infrastructure"]["files"].add(entity.file_path)
            elif any(p in path for p in ['middleware', 'auth', 'log', 'cache', 'intercept', 'aop']):
                layers["cross_cutting"]["entities"].append(entity.id)
                layers["cross_cutting"]["files"].add(entity.file_path)
        
        # 转换set为list
        for layer in layers.values():
            layer["files"] = list(layer["files"])
            layer["entity_count"] = len(layer["entities"])
            layer["file_count"] = len(layer["files"])
        
        return layers
    
    def _analyze_module_dependencies(self) -> Tuple[Dict[str, List[str]], List[List[str]]]:
        """分析模块依赖"""
        if not self.graph.architecture:
            return {}, []
        
        module_deps = {}
        for module_path, module_info in self.graph.architecture.modules.items():
            module_deps[module_path] = module_info.dependencies
        
        # 检测循环依赖
        circular_deps = self._find_cycles(module_deps)
        
        return module_deps, circular_deps
    
    def _find_cycles(self, graph: Dict[str, List[str]]) -> List[List[str]]:
        """查找循环依赖"""
        cycles = []
        visited = set()
        rec_stack = set()
        path = []
        
        def dfs(node):
            visited.add(node)
            rec_stack.add(node)
            path.append(node)
            
            for neighbor in graph.get(node, []):
                if neighbor not in visited:
                    dfs(neighbor)
                elif neighbor in rec_stack:
                    # 找到循环
                    cycle_start = path.index(neighbor)
                    cycle = path[cycle_start:]
                    if len(cycle) > 1:
                        cycles.append(cycle)
            
            path.pop()
            rec_stack.remove(node)
        
        for node in graph:
            if node not in visited:
                dfs(node)
        
        return cycles
    
    def _detect_design_issues(self) -> List[Dict[str, Any]]:
        """检测设计问题"""
        issues = []
        
        # 检查循环依赖
        _, circular_deps = self._analyze_module_dependencies()
        for cycle in circular_deps:
            issues.append({
                "type": "circular_dependency",
                "severity": "high",
                "description": f"循环依赖: {' -> '.join(cycle)}",
                "suggestion": "引入中间层或接口解耦"
            })
        
        # 检查过长调用链
        if self.graph.architecture:
            for chain in self.graph.architecture.call_chains:
                if chain.depth > 8:
                    issues.append({
                        "type": "deep_call_chain",
                        "severity": "medium",
                        "description": f"调用链过深: {chain.name} (深度 {chain.depth})",
                        "suggestion": "考虑简化调用层次"
                    })
        
        # 检查大类
        for entity in self.graph.entities.values():
            if entity.entity_type == EntityType.CLASS:
                if len(entity.children_ids) > 25:
                    issues.append({
                        "type": "large_class",
                        "severity": "medium",
                        "description": f"大类: {entity.name} ({len(entity.children_ids)} 成员)",
                        "entity_id": entity.id,
                        "suggestion": "拆分职责"
                    })
        
        # 检查高耦合
        for entity_id, callees in self.call_graph.items():
            if len(callees) > 15:
                entity = self.graph.entities.get(entity_id)
                if entity:
                    issues.append({
                        "type": "high_coupling",
                        "severity": "medium",
                        "description": f"高耦合: {entity.name} (调用 {len(callees)} 个方法)",
                        "entity_id": entity_id,
                        "suggestion": "减少依赖，考虑依赖注入"
                    })
        
        return issues
    
    def _generate_improvement_suggestions(self, primary_pattern: ArchitecturePattern, issues: List[Dict]) -> List[Dict[str, Any]]:
        """生成改进建议"""
        suggestions = []
        
        # 基于架构模式的建议
        if primary_pattern == ArchitecturePattern.MONOLITHIC:
            suggestions.append({
                "category": "architecture",
                "suggestion": "考虑模块化拆分，逐步演进为分层架构或微服务",
                "priority": "medium"
            })
        
        # 基于问题生成建议
        issue_types = Counter(i["type"] for i in issues)
        
        if issue_types.get("circular_dependency", 0) > 0:
            suggestions.append({
                "category": "design",
                "suggestion": "解决循环依赖，引入接口或中间层解耦模块",
                "priority": "high"
            })
        
        if issue_types.get("large_class", 0) > 3:
            suggestions.append({
                "category": "code_quality",
                "suggestion": "执行大类拆分重构，遵循单一职责原则",
                "priority": "high"
            })
        
        if issue_types.get("high_coupling", 0) > 5:
            suggestions.append({
                "category": "design",
                "suggestion": "降低耦合度，使用依赖注入和接口抽象",
                "priority": "medium"
            })
        
        return suggestions
    
    def _identify_refactoring_hotspots(self) -> List[Dict[str, Any]]:
        """识别重构热点"""
        hotspots = []
        
        for entity in self.graph.entities.values():
            score = 0
            reasons = []
            
            # 高复杂度
            if entity.complexity and entity.complexity > 15:
                score += 30
                reasons.append(f"高复杂度({entity.complexity})")
            
            # 大类
            if entity.entity_type == EntityType.CLASS and len(entity.children_ids) > 15:
                score += 20
                reasons.append(f"类过大({len(entity.children_ids)}成员)")
            
            # 高调用次数
            call_count = self.call_counts.get(entity.id, 0)
            if call_count > 10:
                score += 15
                reasons.append(f"被频繁调用({call_count}次)")
            
            # 低可维护性
            maintainability = self._calculate_maintainability(entity)
            if maintainability < 50:
                score += 25
                reasons.append(f"低可维护性({maintainability:.1f})")
            
            # 技术债务
            debt = self._calculate_technical_debt(entity)
            if debt > 20:
                score += 20
                reasons.append(f"技术债务({debt:.1f})")
            
            if score >= 40:
                hotspots.append({
                    "entity_id": entity.id,
                    "entity_name": entity.name,
                    "entity_type": entity.entity_type.value,
                    "file_path": entity.file_path,
                    "hotspot_score": score,
                    "reasons": reasons,
                    "refactoring_priority": "critical" if score >= 70 else "high" if score >= 55 else "medium"
                })
        
        # 按分数排序
        hotspots.sort(key=lambda x: x["hotspot_score"], reverse=True)
        return hotspots[:20]  # 返回前20个热点
    
    def _calculate_architecture_statistics(self) -> Dict[str, Any]:
        """计算架构统计信息"""
        return {
            "total_entities": len(self.graph.entities),
            "total_relationships": len(self.graph.relationships),
            "entity_types": dict(Counter(e.entity_type.value for e in self.graph.entities.values())),
            "modules": len(self.graph.architecture.modules) if self.graph.architecture else 0,
            "features": len(self.graph.architecture.features) if self.graph.architecture else 0,
            "call_chains": len(self.graph.architecture.call_chains) if self.graph.architecture else 0
        }
    
    def _infer_feature_purpose(self, feature: FeatureInfo) -> str:
        """推断功能模块目的"""
        if feature.description:
            return feature.description
        
        # 基于入口点推断
        if feature.entry_points:
            entry_entity = self.graph.entities.get(feature.entry_points[0])
            if entry_entity:
                return self._infer_purpose(entry_entity)
        
        return f"提供 {feature.name} 功能"
    
    def _assess_business_value(self, feature: FeatureInfo) -> str:
        """评估业务价值"""
        value = feature.importance
        if value > 50:
            return "核心业务功能"
        elif value > 20:
            return "重要业务功能"
        else:
            return "辅助功能"
    
    def _calculate_refactoring_priority(self, feature: FeatureInfo, explanations: List[CodeExplanation]) -> str:
        """计算重构优先级"""
        # 基于复杂度和代码异味
        total_complexity = sum(e.complexity_score for e in explanations)
        total_smells = sum(len(e.code_smells) for e in explanations)
        
        if total_complexity > 200 or total_smells > 15:
            return "high"
        elif total_complexity > 100 or total_smells > 8:
            return "medium"
        return "low"
    
    def _group_and_rank_smells(self, smells: List[Dict]) -> List[Dict]:
        """分组和排序代码异味"""
        # 按类型分组
        grouped = defaultdict(list)
        for smell in smells:
            grouped[smell["type"]].append(smell)
        
        # 生成汇总
        result = []
        for smell_type, items in grouped.items():
            result.append({
                "type": smell_type,
                "count": len(items),
                "severity": "high" if any(i.get("severity") == "high" for i in items) else "medium",
                "affected_entities": list(set(i["entity_name"] for i in items))[:10],
                "suggestion": items[0].get("suggestion", "") if items else ""
            })
        
        # 按数量排序
        result.sort(key=lambda x: x["count"], reverse=True)
        return result
    
    def _prioritize_refactoring_items(self, hotspots: List[Dict], smells: List[Dict]) -> List[Dict]:
        """优先级排序重构项"""
        priority_items = []
        
        # 从热点中选择
        for hotspot in hotspots[:10]:
            priority_items.append({
                "type": "hotspot",
                "entity_id": hotspot["entity_id"],
                "entity_name": hotspot["entity_name"],
                "file_path": hotspot["file_path"],
                "priority": hotspot["refactoring_priority"],
                "reason": ", ".join(hotspot["reasons"]),
                "estimated_effort": "high" if hotspot["hotspot_score"] >= 70 else "medium"
            })
        
        # 从异味中选择高严重性的
        high_severity_smells = [s for s in smells if s.get("severity") == "high"]
        for smell in high_severity_smells[:5]:
            if not any(p["entity_id"] == smell.get("entity_id") for p in priority_items):
                priority_items.append({
                    "type": "code_smell",
                    "entity_id": smell.get("entity_id"),
                    "entity_name": smell.get("entity_name"),
                    "file_path": smell.get("file_path"),
                    "priority": "high",
                    "reason": smell.get("description", ""),
                    "estimated_effort": "low"
                })
        
        return priority_items
