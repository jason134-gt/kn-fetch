"""
架构分析器 - 从知识图谱中提取软件架构信息
支持：功能识别、模块分析、调用链分析、异常流分析、消息机制分析等
"""
import json
from typing import List, Dict, Any, Optional, Set, Tuple
from pathlib import Path
from collections import defaultdict, Counter
from datetime import datetime

from ..gitnexus.models import (
    KnowledgeGraph, CodeEntity, EntityType, Relationship, RelationshipType,
    ModuleInfo, FeatureInfo, CallChain, ExceptionFlow, MessageFlow, 
    APIEndpoint, ArchitectureInfo
)


class ArchitectureAnalyzer:
    """架构分析器"""
    
    def __init__(self, graph: KnowledgeGraph):
        self.graph = graph
        self._build_indexes()
    
    def _build_indexes(self):
        """构建索引以加速查询"""
        # 按文件索引
        self.file_entities: Dict[str, List[CodeEntity]] = defaultdict(list)
        # 按模块索引
        self.module_entities: Dict[str, List[CodeEntity]] = defaultdict(list)
        # 按类型索引
        self.type_entities: Dict[EntityType, List[CodeEntity]] = defaultdict(list)
        # 调用关系索引
        self.caller_to_callees: Dict[str, List[Tuple[str, str]]] = defaultdict(list)  # caller -> [(callee, call_site)]
        self.callee_to_callers: Dict[str, List[str]] = defaultdict(list)  # callee -> [callers]
        
        for entity in self.graph.entities.values():
            self.file_entities[entity.file_path].append(entity)
            self.type_entities[entity.entity_type].append(entity)
            
            if entity.module_path:
                self.module_entities[entity.module_path].append(entity)
        
        for rel in self.graph.relationships:
            if rel.relationship_type == RelationshipType.CALLS:
                self.caller_to_callees[rel.source_id].append((rel.target_id, rel.call_site or ""))
                self.callee_to_callers[rel.target_id].append(rel.source_id)
    
    def analyze_full(self) -> ArchitectureInfo:
        """完整架构分析"""
        return ArchitectureInfo(
            modules=self.analyze_modules(),
            features=self.analyze_features(),
            call_chains=self.analyze_call_chains(),
            exception_flows=self.analyze_exception_flows(),
            message_flows=self.analyze_message_flows(),
            api_endpoints=self.analyze_api_endpoints(),
            layers=self.analyze_layers(),
            statistics=self.generate_statistics()
        )
    
    def analyze_modules(self) -> Dict[str, ModuleInfo]:
        """分析模块结构"""
        modules = {}
        
        # 按目录分组
        dir_entities: Dict[str, List[CodeEntity]] = defaultdict(list)
        
        for entity in self.graph.entities.values():
            # 提取目录路径
            file_path = entity.file_path.replace('\\', '/')
            parts = file_path.split('/')
            
            # 使用第一级或第二级目录作为模块
            if len(parts) > 1:
                if parts[0] in ['src', 'lib', 'app', 'core']:
                    module_path = '/'.join(parts[:2]) if len(parts) > 2 else parts[0]
                else:
                    module_path = parts[0]
            else:
                module_path = 'root'
            
            dir_entities[module_path].append(entity)
        
        # 构建模块信息
        for module_path, entities in dir_entities.items():
            classes = [e for e in entities if e.entity_type == EntityType.CLASS]
            functions = [e for e in entities if e.entity_type in [EntityType.FUNCTION, EntityType.METHOD]]
            interfaces = [e for e in entities if e.entity_type == EntityType.INTERFACE]
            
            # 分析模块依赖
            dependencies = set()
            for entity in entities:
                for rel in self.graph.relationships:
                    if rel.source_id == entity.id and rel.relationship_type in [
                        RelationshipType.IMPORTS, RelationshipType.DEPENDS_ON
                    ]:
                        if rel.target_id in self.graph.entities:
                            target = self.graph.entities[rel.target_id]
                            target_module = self._get_module_of_entity(target)
                            if target_module and target_module != module_path:
                                dependencies.add(target_module)
            
            module_info = ModuleInfo(
                name=module_path.split('/')[-1],
                path=module_path,
                file_count=len(set(e.file_path for e in entities)),
                entity_count=len(entities),
                lines_of_code=sum(e.lines_of_code for e in entities),
                dependencies=list(dependencies),
                classes=[c.name for c in classes],
                functions=[f.name for f in functions],
                interfaces=[i.name for i in interfaces]
            )
            modules[module_path] = module_info
        
        # 计算模块被依赖
        for module_path, module_info in modules.items():
            for dep in module_info.dependencies:
                if dep in modules:
                    modules[dep].dependents.append(module_path)
        
        return modules
    
    def analyze_features(self) -> Dict[str, FeatureInfo]:
        """识别功能模块"""
        features = {}
        
        # 方法1: 基于入口点识别功能
        entry_points = self._identify_entry_points()
        
        for entry_id in entry_points:
            entry_entity = self.graph.entities.get(entry_id)
            if not entry_entity:
                continue
            
            # 追踪调用链，识别相关实体
            related_entities = self._trace_related_entities(entry_id, max_depth=5)
            
            # 提取功能名称
            feature_name = self._extract_feature_name(entry_entity)
            
            # 计算功能复杂度
            complexity = sum(
                e.complexity or 1 
                for e in [self.graph.entities.get(eid) for eid in related_entities]
                if e
            )
            
            # 计算重要性（被调用次数 + 调用次数）
            importance = sum(
                len(self.callee_to_callers.get(eid, [])) + 
                len(self.caller_to_callees.get(eid, []))
                for eid in related_entities
            )
            
            feature = FeatureInfo(
                id=self._generate_feature_id(feature_name),
                name=feature_name,
                description=entry_entity.docstring,
                entry_points=[entry_id],
                entities=list(related_entities),
                complexity=complexity,
                importance=importance
            )
            features[feature.id] = feature
        
        # 方法2: 基于类簇识别功能
        class_clusters = self._identify_class_clusters()
        for cluster_name, class_ids in class_clusters.items():
            if cluster_name not in features:
                feature = FeatureInfo(
                    id=self._generate_feature_id(cluster_name),
                    name=cluster_name,
                    entities=class_ids,
                    importance=sum(len(self.callee_to_callers.get(cid, [])) for cid in class_ids)
                )
                features[feature.id] = feature
        
        return features
    
    def analyze_call_chains(self, max_chains: int = 50, max_depth: int = 10) -> List[CallChain]:
        """分析调用链"""
        chains = []
        
        # 从入口点开始追踪
        entry_points = self._identify_entry_points()
        
        visited_chains = set()
        
        for entry_id in entry_points:
            if len(chains) >= max_chains:
                break
            
            chain = self._build_call_chain(entry_id, max_depth)
            if chain and chain['id'] not in visited_chains:
                visited_chains.add(chain['id'])
                chains.append(CallChain(
                    id=chain['id'],
                    name=chain['name'],
                    entry_point=entry_id,
                    nodes=chain['nodes'],
                    edges=chain['edges'],
                    depth=chain['depth'],
                    total_calls=chain['total_calls']
                ))
        
        # 按深度和调用次数排序
        chains.sort(key=lambda c: (c.depth, c.total_calls), reverse=True)
        
        return chains[:max_chains]
    
    def analyze_exception_flows(self) -> Dict[str, ExceptionFlow]:
        """分析异常流"""
        exception_flows = {}
        
        # 收集所有异常
        exception_entities = [
            e for e in self.graph.entities.values()
            if e.entity_type == EntityType.EXCEPTION or 
               (e.entity_type == EntityType.CLASS and 'Exception' in e.name) or
               (e.entity_type == EntityType.CLASS and 'Error' in e.name)
        ]
        
        # 分析每个异常的抛出和捕获
        for exc_entity in exception_entities:
            thrown_by = []
            caught_by = []
            handled_by = []
            
            # 查找抛出该异常的函数
            for rel in self.graph.relationships:
                if rel.relationship_type == RelationshipType.THROWS:
                    if rel.target_id == exc_entity.id or exc_entity.name in rel.metadata.get('exception', ''):
                        thrown_by.append(rel.source_id)
                
                if rel.relationship_type == RelationshipType.CATCHES:
                    if rel.target_id == exc_entity.id or exc_entity.name in rel.metadata.get('exception', ''):
                        caught_by.append(rel.source_id)
                
                if rel.relationship_type == RelationshipType.HANDLES:
                    if rel.target_id == exc_entity.id or exc_entity.name in rel.metadata.get('exception', ''):
                        handled_by.append(rel.source_id)
            
            if thrown_by or caught_by:
                flow = ExceptionFlow(
                    exception_type=exc_entity.name,
                    thrown_by=thrown_by,
                    caught_by=caught_by,
                    handled_by=handled_by
                )
                exception_flows[exc_entity.name] = flow
        
        # 从函数的raises属性分析
        for entity in self.graph.entities.values():
            if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD] and entity.raises:
                for exc_name in entity.raises:
                    if exc_name not in exception_flows:
                        exception_flows[exc_name] = ExceptionFlow(
                            exception_type=exc_name,
                            thrown_by=[entity.id]
                        )
                    else:
                        if entity.id not in exception_flows[exc_name].thrown_by:
                            exception_flows[exc_name].thrown_by.append(entity.id)
        
        return exception_flows
    
    def analyze_message_flows(self) -> Dict[str, MessageFlow]:
        """分析消息/事件机制"""
        message_flows = {}
        
        # 查找事件相关的实体
        event_entities = [
            e for e in self.graph.entities.values()
            if e.entity_type == EntityType.EVENT or
               'event' in e.name.lower() or
               'message' in e.name.lower() or
               'signal' in e.name.lower()
        ]
        
        # 分析事件流
        for event in event_entities:
            emitters = []
            receivers = []
            handlers = []
            
            # 查找发送者
            for rel in self.graph.relationships:
                if rel.relationship_type in [RelationshipType.SENDS, RelationshipType.EMITS]:
                    if rel.target_id == event.id or event.name in str(rel.metadata):
                        emitters.append(rel.source_id)
            
            # 查找处理者
            for entity in self.graph.entities.values():
                if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD]:
                    if event.name.lower() in entity.name.lower():
                        handlers.append(entity.id)
            
            if emitters or handlers:
                message_flows[event.name] = MessageFlow(
                    event_type=event.name,
                    emitters=emitters,
                    receivers=receivers,
                    handlers=handlers
                )
        
        return message_flows
    
    def analyze_api_endpoints(self) -> List[APIEndpoint]:
        """分析API端点"""
        endpoints = []
        
        # 查找路由定义
        for entity in self.graph.entities.values():
            if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD]:
                # 检查装饰器
                for decorator in entity.decorators:
                    endpoint = self._parse_route_decorator(decorator, entity)
                    if endpoint:
                        endpoints.append(endpoint)
        
        return endpoints
    
    def analyze_layers(self) -> Dict[str, List[str]]:
        """分析分层架构"""
        layers = {
            "presentation": [],  # 表现层
            "business": [],      # 业务层
            "data": [],          # 数据层
            "infrastructure": [], # 基础设施层
            "cross_cutting": []  # 横切关注点
        }
        
        # 基于模块路径和命名推断层级
        for entity in self.graph.entities.values():
            file_path = entity.file_path.lower().replace('\\', '/')
            
            if any(p in file_path for p in ['api', 'controller', 'view', 'route', 'endpoint', 'handler']):
                layers["presentation"].append(entity.id)
            elif any(p in file_path for p in ['service', 'business', 'logic', 'manager', 'processor']):
                layers["business"].append(entity.id)
            elif any(p in file_path for p in ['data', 'dao', 'repository', 'model', 'entity', 'db', 'database']):
                layers["data"].append(entity.id)
            elif any(p in file_path for p in ['config', 'util', 'common', 'helper', 'core', 'base']):
                layers["infrastructure"].append(entity.id)
            elif any(p in file_path for p in ['middleware', 'auth', 'log', 'cache', 'intercept']):
                layers["cross_cutting"].append(entity.id)
        
        return layers
    
    def generate_statistics(self) -> Dict[str, Any]:
        """生成统计信息"""
        stats = {
            "total_entities": len(self.graph.entities),
            "total_relationships": len(self.graph.relationships),
            "entity_types": Counter(e.entity_type.value for e in self.graph.entities.values()),
            "relationship_types": Counter(r.relationship_type.value for r in self.graph.relationships),
            "total_lines_of_code": sum(e.lines_of_code for e in self.graph.entities.values()),
            "average_complexity": 0,
            "max_complexity": 0,
            "most_called_entities": [],
            "most_complex_entities": [],
            "module_count": 0,
            "feature_count": 0,
            "call_chain_depth_avg": 0
        }
        
        # 复杂度统计
        complexities = [e.complexity for e in self.graph.entities.values() if e.complexity]
        if complexities:
            stats["average_complexity"] = sum(complexities) / len(complexities)
            stats["max_complexity"] = max(complexities)
        
        # 最被调用的实体
        call_counts = [(entity_id, len(callers)) for entity_id, callers in self.callee_to_callers.items()]
        call_counts.sort(key=lambda x: x[1], reverse=True)
        stats["most_called_entities"] = [
            {"id": eid, "name": self.graph.entities.get(eid, CodeEntity(id="", entity_type="", name="", file_path="", start_line=0, end_line=0)).name, "calls": count}
            for eid, count in call_counts[:10]
            if eid in self.graph.entities
        ]
        
        # 最复杂的实体
        complex_entities = [
            (e.id, e.name, e.complexity) 
            for e in self.graph.entities.values() 
            if e.complexity
        ]
        complex_entities.sort(key=lambda x: x[2], reverse=True)
        stats["most_complex_entities"] = [
            {"id": eid, "name": name, "complexity": c}
            for eid, name, c in complex_entities[:10]
        ]
        
        return stats
    
    def _identify_entry_points(self) -> List[str]:
        """识别入口点"""
        entry_points = []
        
        # 策略1: 没有调用者的公共函数/方法
        for entity in self.graph.entities.values():
            if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD]:
                callers = self.callee_to_callers.get(entity.id, [])
                if not callers and entity.visibility == 'public':
                    entry_points.append(entity.id)
        
        # 策略2: 特定装饰器标记的函数
        for entity in self.graph.entities.values():
            if entity.decorators:
                for dec in entity.decorators:
                    if dec in ['app.route', 'get', 'post', 'put', 'delete', 'api_view', 'route']:
                        if entity.id not in entry_points:
                            entry_points.append(entity.id)
        
        # 策略3: 特定命名模式
        for entity in self.graph.entities.values():
            if entity.entity_type == EntityType.FUNCTION:
                if entity.name in ['main', 'run', 'start', 'execute', 'handle', 'process']:
                    if entity.id not in entry_points:
                        entry_points.append(entity.id)
        
        return entry_points[:50]  # 限制数量
    
    def _trace_related_entities(self, entity_id: str, max_depth: int, visited: Set[str] = None) -> Set[str]:
        """追踪相关实体"""
        if visited is None:
            visited = set()
        
        if max_depth <= 0 or entity_id in visited:
            return visited
        
        visited.add(entity_id)
        
        # 追踪调用的实体
        callees = self.caller_to_callees.get(entity_id, [])
        for callee_id, _ in callees:
            self._trace_related_entities(callee_id, max_depth - 1, visited)
        
        return visited
    
    def _extract_feature_name(self, entity: CodeEntity) -> str:
        """提取功能名称"""
        # 尝试从docstring提取
        if entity.docstring:
            first_line = entity.docstring.split('\n')[0].strip()
            if len(first_line) < 50:
                return first_line
        
        # 使用函数名
        name = entity.name.replace('_', ' ').replace('-', ' ')
        
        # 移除常见前缀
        for prefix in ['handle', 'process', 'execute', 'run', 'get', 'post', 'put', 'delete']:
            if name.lower().startswith(prefix):
                name = name[len(prefix):].strip()
        
        return name or entity.name
    
    def _identify_class_clusters(self) -> Dict[str, List[str]]:
        """识别类簇"""
        clusters = defaultdict(list)
        
        # 基于继承关系聚类
        for entity in self.graph.entities.values():
            if entity.entity_type == EntityType.CLASS:
                if entity.bases:
                    # 按基类聚类
                    for base in entity.bases:
                        cluster_name = f"{base} Related"
                        clusters[cluster_name].append(entity.id)
                else:
                    # 按模块聚类
                    module = self._get_module_of_entity(entity)
                    if module:
                        clusters[f"{module} Classes"].append(entity.id)
        
        return clusters
    
    def _build_call_chain(self, entry_id: str, max_depth: int) -> Dict[str, Any]:
        """构建调用链"""
        nodes = []
        edges = []
        
        def traverse(caller_id: str, depth: int, visited: Set[str]):
            if depth > max_depth or caller_id in visited:
                return
            
            visited.add(caller_id)
            nodes.append(caller_id)
            
            callees = self.caller_to_callees.get(caller_id, [])
            for callee_id, call_site in callees:
                edges.append((caller_id, callee_id))
                traverse(callee_id, depth + 1, visited)
        
        visited = set()
        traverse(entry_id, 0, visited)
        
        entity = self.graph.entities.get(entry_id)
        name = entity.name if entity else "Unknown"
        
        return {
            'id': f"chain_{entry_id}",
            'name': name,
            'nodes': nodes,
            'edges': edges,
            'depth': len(set(nodes)) - 1,
            'total_calls': len(edges)
        }
    
    def _get_module_of_entity(self, entity: CodeEntity) -> Optional[str]:
        """获取实体所属模块"""
        if entity.module_path:
            return entity.module_path
        
        file_path = entity.file_path.replace('\\', '/')
        parts = file_path.split('/')
        
        if len(parts) > 1:
            return '/'.join(parts[:-1])
        
        return None
    
    def _parse_route_decorator(self, decorator: str, entity: CodeEntity) -> Optional[APIEndpoint]:
        """解析路由装饰器"""
        # 简单的路由解析
        patterns = [
            (r'@app\.route\([\'"]([^\'"]+)[\'"]', 'GET'),
            (r'@([a-z]+)\.route\([\'"]([^\'"]+)[\'"]', None),
            (r'@(get|post|put|delete)\([\'"]([^\'"]+)[\'"]', None),
        ]
        
        import re
        
        for pattern, method in patterns:
            match = re.search(pattern, decorator, re.IGNORECASE)
            if match:
                if method:
                    path = match.group(1)
                else:
                    method = match.group(1).upper()
                    path = match.group(2)
                
                return APIEndpoint(
                    path=path,
                    method=method or 'GET',
                    handler=entity.id,
                    parameters=[],
                    responses={}
                )
        
        return None
    
    def _generate_feature_id(self, name: str) -> str:
        """生成功能ID"""
        import hashlib
        return hashlib.md5(name.encode()).hexdigest()[:12]
