"""
架构分析Agent - 负责代码架构关系分析与依赖图构建

功能：
- 模块依赖关系分析
- 架构层次识别
- 设计模式检测
- 架构质量评估
- 依赖图可视化
"""

from typing import List, Dict, Any, Set, Optional
from collections import defaultdict
from .base_agent import BaseAgent
from src.models.dependency import Dependency, DependencyGraph, RiskAssessment


class ArchitectureAnalyzerAgent(BaseAgent):
    """架构分析Agent"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__("architecture_analyzer", config)
        self.architecture_patterns = self._load_architecture_patterns()
    
    async def _execute_impl(self, input_data: Any) -> DependencyGraph:
        """执行架构分析"""
        if isinstance(input_data, dict):
            code_metadata_list = input_data.get("code_metadata_list", [])
            semantic_contracts = input_data.get("semantic_contracts", [])
        else:
            raise ValueError("输入数据应包含代码元数据列表和语义契约")
        
        self.logger.info(f"开始架构分析，处理 {len(code_metadata_list)} 个文件")
        
        # 创建依赖图
        dependency_graph = DependencyGraph()
        
        # 分析模块依赖关系
        await self._analyze_module_dependencies(code_metadata_list, dependency_graph)
        
        # 识别架构层次
        await self._identify_architecture_layers(dependency_graph, semantic_contracts)
        
        # 检测设计模式
        await self._detect_design_patterns(code_metadata_list, dependency_graph)
        
        # 评估架构风险
        await self._assess_architecture_risks(dependency_graph)
        
        self.logger.info(f"架构分析完成，识别 {len(dependency_graph.dependencies)} 个依赖关系")
        return dependency_graph
    
    async def _analyze_module_dependencies(self, code_metadata_list: List[Any], graph: DependencyGraph):
        """分析模块依赖关系"""
        # 构建文件路径到模块的映射
        file_to_module = {}
        for metadata in code_metadata_list:
            module_name = self._extract_module_name(metadata.file_path)
            file_to_module[metadata.file_path] = module_name
            
            # 添加模块节点
            graph.add_module(module_name, metadata.file_path)
        
        # 分析导入依赖
        for metadata in code_metadata_list:
            source_module = file_to_module.get(metadata.file_path)
            if not source_module:
                continue
                
            # 分析导入关系
            for import_metadata in metadata.imports:
                for imported_module in import_metadata.module_names:
                    if imported_module and imported_module != source_module:
                        # 创建依赖关系
                        dependency = Dependency(
                            source=source_module,
                            target=imported_module,
                            dependency_type="import",
                            strength=self._calculate_dependency_strength(metadata, imported_module),
                            metadata={
                                "import_type": "module",
                                "file_path": metadata.file_path
                            }
                        )
                        graph.add_dependency(dependency)
            
            # 分析类继承关系
            for class_metadata in metadata.classes:
                for base_class in class_metadata.base_classes:
                    base_module = self._resolve_base_class_module(base_class, file_to_module)
                    if base_module and base_module != source_module:
                        dependency = Dependency(
                            source=source_module,
                            target=base_module,
                            dependency_type="inheritance",
                            strength=0.8,  # 继承关系强度较高
                            metadata={
                                "class_name": class_metadata.name,
                                "base_class": base_class
                            }
                        )
                        graph.add_dependency(dependency)
    
    def _extract_module_name(self, file_path: str) -> str:
        """从文件路径提取模块名"""
        from pathlib import Path
        path = Path(file_path)
        
        # 如果是Python项目，使用包结构
        if path.suffix == ".py":
            # 移除.py后缀，将路径分隔符转换为点
            module_parts = path.with_suffix('').parts
            module_name = ".".join(module_parts)
            return module_name
        
        # 其他语言使用文件路径
        return str(path.parent / path.stem)
    
    def _calculate_dependency_strength(self, metadata: Any, imported_module: str) -> float:
        """计算依赖关系强度"""
        # 基础强度
        strength = 0.5
        
        # 根据导入数量调整强度
        import_count = len(metadata.imports)
        if import_count > 10:
            strength += 0.2
        elif import_count > 5:
            strength += 0.1
        
        # 根据文件大小调整强度
        if hasattr(metadata, 'file_size') and metadata.file_size > 1000:
            strength += 0.1
        
        return min(strength, 1.0)
    
    def _resolve_base_class_module(self, base_class: str, file_to_module: Dict[str, str]) -> Optional[str]:
        """解析基类所属模块"""
        # 简单的模块解析逻辑
        for module_name in file_to_module.values():
            if base_class.lower() in module_name.lower():
                return module_name
        return None
    
    async def _identify_architecture_layers(self, graph: DependencyGraph, semantic_contracts: List[Any]):
        """识别架构层次"""
        # 基于依赖关系和语义分析识别层次
        layers = {
            "presentation": set(),  # 表现层
            "business": set(),     # 业务层
            "data": set(),         # 数据层
            "infrastructure": set() # 基础设施层
        }
        
        # 根据模块名和语义特征分类
        for module_name in graph.modules.keys():
            layer = self._classify_module_layer(module_name, semantic_contracts)
            layers[layer].add(module_name)
        
        # 更新图的层次信息
        graph.architecture_layers = layers
    
    def _classify_module_layer(self, module_name: str, semantic_contracts: List[Any]) -> str:
        """分类模块层次"""
        module_name_lower = module_name.lower()
        
        # 基于模块名关键词分类
        if any(keyword in module_name_lower for keyword in [
            'controller', 'view', 'ui', 'web', 'api', 'route', 'endpoint'
        ]):
            return "presentation"
        
        if any(keyword in module_name_lower for keyword in [
            'service', 'business', 'logic', 'manager', 'handler', 'processor'
        ]):
            return "business"
        
        if any(keyword in module_name_lower for keyword in [
            'model', 'entity', 'schema', 'dto', 'dao', 'repository'
        ]):
            return "data"
        
        if any(keyword in module_name_lower for keyword in [
            'util', 'helper', 'common', 'config', 'constant'
        ]):
            return "infrastructure"
        
        # 基于语义契约进一步分类
        for contract in semantic_contracts:
            if contract.file_path and module_name in contract.file_path:
                if any(intention in contract.intentions for intention in ["api_implementation"]):
                    return "presentation"
                elif any(intention in contract.intentions for intention in ["business_logic"]):
                    return "business"
                elif any(intention in contract.intentions for intention in ["data_modeling"]):
                    return "data"
        
        # 默认分类为基础设施层
        return "infrastructure"
    
    async def _detect_design_patterns(self, code_metadata_list: List[Any], graph: DependencyGraph):
        """检测设计模式"""
        detected_patterns = []
        
        for metadata in code_metadata_list:
            # 检测单例模式
            if self._detect_singleton_pattern(metadata):
                detected_patterns.append({
                    "pattern": "singleton",
                    "file_path": metadata.file_path,
                    "confidence": 0.8
                })
            
            # 检测工厂模式
            if self._detect_factory_pattern(metadata):
                detected_patterns.append({
                    "pattern": "factory",
                    "file_path": metadata.file_path,
                    "confidence": 0.7
                })
            
            # 检测观察者模式
            if self._detect_observer_pattern(metadata):
                detected_patterns.append({
                    "pattern": "observer",
                    "file_path": metadata.file_path,
                    "confidence": 0.6
                })
        
        graph.design_patterns = detected_patterns
    
    def _detect_singleton_pattern(self, metadata: Any) -> bool:
        """检测单例模式"""
        for class_metadata in metadata.classes:
            # 检查是否有get_instance或instance方法
            has_get_instance = any(
                "get_instance" in method.name.lower() or "instance" in method.name.lower()
                for method in class_metadata.methods
            )
            
            # 检查是否有私有构造函数
            has_private_constructor = any(
                method.name == "__init__" and "private" in str(method.metadata).lower()
                for method in class_metadata.methods
            )
            
            if has_get_instance or has_private_constructor:
                return True
        
        return False
    
    def _detect_factory_pattern(self, metadata: Any) -> bool:
        """检测工厂模式"""
        for class_metadata in metadata.classes:
            # 检查类名是否包含Factory
            if "factory" in class_metadata.name.lower():
                return True
            
            # 检查是否有create_xxx方法
            create_methods = [
                method for method in class_metadata.methods
                if "create" in method.name.lower() or "make" in method.name.lower()
            ]
            
            if len(create_methods) >= 2:
                return True
        
        return False
    
    def _detect_observer_pattern(self, metadata: Any) -> bool:
        """检测观察者模式"""
        for class_metadata in metadata.classes:
            # 检查是否有subscribe/unsubscribe/notify方法
            observer_methods = [
                method for method in class_metadata.methods
                if any(keyword in method.name.lower() 
                      for keyword in ["subscribe", "unsubscribe", "notify", "publish"])
            ]
            
            if len(observer_methods) >= 2:
                return True
        
        return False
    
    async def _assess_architecture_risks(self, graph: DependencyGraph):
        """评估架构风险"""
        risks = []
        
        # 检测循环依赖
        cyclic_dependencies = self._detect_cyclic_dependencies(graph)
        if cyclic_dependencies:
            risks.append(RiskAssessment(
                risk_type="cyclic_dependency",
                severity="high",
                description=f"发现 {len(cyclic_dependencies)} 个循环依赖",
                affected_modules=list(cyclic_dependencies),
                recommendation="重构模块依赖关系，打破循环依赖"
            ))
        
        # 检测过度耦合
        highly_coupled_modules = self._detect_high_coupling(graph)
        if highly_coupled_modules:
            risks.append(RiskAssessment(
                risk_type="high_coupling",
                severity="medium",
                description=f"发现 {len(highly_coupled_modules)} 个高度耦合的模块",
                affected_modules=highly_coupled_modules,
                recommendation="考虑模块拆分或接口抽象"
            ))
        
        # 检测架构违规
        architecture_violations = self._detect_architecture_violations(graph)
        if architecture_violations:
            risks.append(RiskAssessment(
                risk_type="architecture_violation",
                severity="medium",
                description=f"发现 {len(architecture_violations)} 个架构层次违规",
                affected_modules=architecture_violations,
                recommendation="遵循架构分层原则，调整模块依赖关系"
            ))
        
        graph.risk_assessments = risks
    
    def _detect_cyclic_dependencies(self, graph: DependencyGraph) -> Set[str]:
        """检测循环依赖"""
        # 简化的循环依赖检测
        visited = set()
        cyclic_modules = set()
        
        def dfs(module, path):
            if module in path:
                # 发现循环
                cyclic_modules.add(module)
                return
            
            if module in visited:
                return
            
            visited.add(module)
            path.add(module)
            
            # 遍历依赖
            for dep in graph.dependencies:
                if dep.source == module:
                    dfs(dep.target, path.copy())
            
            path.remove(module)
        
        for module in graph.modules:
            if module not in visited:
                dfs(module, set())
        
        return cyclic_modules
    
    def _detect_high_coupling(self, graph: DependencyGraph) -> List[str]:
        """检测高度耦合"""
        # 统计每个模块的依赖数量
        dependency_count = defaultdict(int)
        for dep in graph.dependencies:
            dependency_count[dep.source] += 1
        
        # 找出依赖数量超过阈值的模块
        threshold = 10  # 依赖数量阈值
        return [
            module for module, count in dependency_count.items()
            if count > threshold
        ]
    
    def _detect_architecture_violations(self, graph: DependencyGraph) -> List[str]:
        """检测架构层次违规"""
        violations = []
        
        # 检查依赖方向是否违反架构层次
        layer_order = ["presentation", "business", "data", "infrastructure"]
        
        for dep in graph.dependencies:
            source_layer = self._get_module_layer(dep.source, graph)
            target_layer = self._get_module_layer(dep.target, graph)
            
            if source_layer and target_layer:
                source_index = layer_order.index(source_layer)
                target_index = layer_order.index(target_layer)
                
                # 高层模块不应该依赖低层模块
                if source_index < target_index:
                    violations.append(f"{dep.source} -> {dep.target}")
        
        return violations
    
    def _get_module_layer(self, module: str, graph: DependencyGraph) -> Optional[str]:
        """获取模块所属层次"""
        for layer, modules in graph.architecture_layers.items():
            if module in modules:
                return layer
        return None
    
    def _load_architecture_patterns(self) -> Dict[str, Any]:
        """加载架构模式配置"""
        return {
            "layered": {
                "description": "分层架构模式",
                "layers": ["presentation", "business", "data", "infrastructure"]
            },
            "microservices": {
                "description": "微服务架构模式",
                "characteristics": ["independent_deployment", "bounded_context"]
            },
            "event_driven": {
                "description": "事件驱动架构模式",
                "components": ["event_producer", "event_consumer", "event_bus"]
            }
        }