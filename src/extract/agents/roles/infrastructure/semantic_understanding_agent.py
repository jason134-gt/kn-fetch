"""
Semantic Understanding Agent - 语义理解与业务抽象

负责从代码中识别业务语义、构建业务抽象模型和语义关系分析
"""
from typing import Dict, Any, List, Optional
import json
import re
from pathlib import Path
import time

from ...base.agent import BaseAgent, AgentResult
from ...config.agent_configs import SEMANTIC_UNDERSTANDING_AGENT


class SemanticUnderstandingAgent(BaseAgent):
    """语义理解与业务抽象Agent
    
    作为语义理解与业务抽象专家，负责分析代码的业务语义和领域逻辑
    """
    
    def __init__(self):
        """
        初始化SemanticUnderstandingAgent
        """
        super().__init__(SEMANTIC_UNDERSTANDING_AGENT)
        
        # 业务实体类型映射
        self.entity_types = {
            # 业务操作类型
            "creation": ["create", "add", "insert", "new", "build", "make", "generate"],
            "modification": ["update", "modify", "change", "edit", "adjust"],
            "deletion": ["delete", "remove", "destroy", "erase", "clear"],
            "query": ["get", "fetch", "query", "find", "search", "read", "select"],
            "validation": ["validate", "check", "verify", "test", "assert"],
            "calculation": ["calculate", "compute", "process", "analyze", "transform"],
            
            # 业务实体类型
            "user": ["user", "customer", "client", "member", "person"],
            "transaction": ["order", "transaction", "payment", "invoice", "bill"],
            "product": ["product", "item", "service", "good", "commodity"],
            "report": ["report", "statistic", "metric", "analytics", "dashboard"],
            "system": ["system", "service", "manager", "handler", "controller"]
        }
        
        # 代码意图类型映射
        self.intention_types = {
            "api_implementation": ["api", "endpoint", "route", "controller", "rest"],
            "business_logic": ["service", "manager", "handler", "processor", "logic"],
            "data_modeling": ["model", "entity", "schema", "dto", "vo", "pojo"],
            "utility_function": ["util", "helper", "tool", "common", "utility"],
            "infrastructure": ["config", "setting", "database", "cache", "queue"]
        }
    
    def execute(self, context):
        """
        执行语义理解分析
        
        Args:
            context: Agent执行上下文
            
        Returns:
            Agent执行结果
        """
        start_time = time.time()
        
        try:
            self.logger.info(f"Starting semantic understanding analysis for project")
            
            # 1. 获取代码解析结果
            if not hasattr(context, 'parsing_results') or not context.parsing_results:
                return AgentResult(
                    success=False,
                    error="需要先运行CodeParserAgent获取代码解析结果",
                    metrics={"execution_time": time.time() - start_time}
                )
            
            # 2. 提取业务语义
            semantic_results = self._extract_semantics(context.parsing_results)
            
            # 3. 分析语义关系
            semantic_relationships = self._analyze_semantic_relationships(semantic_results)
            
            # 4. 构建业务抽象模型
            business_abstraction = self._build_business_abstraction(semantic_results, semantic_relationships)
            
            # 5. 构建分析报告
            analysis_report = self._build_analysis_report(semantic_results, semantic_relationships, business_abstraction)
            
            # 6. 统计信息
            stats = {
                "total_entities": len(semantic_results.get("entities", [])),
                "total_operations": len(semantic_results.get("operations", [])),
                "total_relationships": len(semantic_relationships),
                "business_domains": len(business_abstraction.get("domains", [])),
                "semantic_richness": self._calculate_semantic_richness(semantic_results)
            }
            
            execution_time = time.time() - start_time
            
            return AgentResult(
                success=True,
                data={
                    "semantic_results": semantic_results,
                    "semantic_relationships": semantic_relationships,
                    "business_abstraction": business_abstraction
                },
                output=analysis_report,
                metrics=stats,
                cross_notes=self._extract_cross_module_notes(analysis_report)
            )
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.logger.error(f"Semantic understanding analysis failed: {str(e)}")
            
            return AgentResult(
                success=False,
                error=str(e),
                metrics={"execution_time": execution_time}
            )
    
    def _get_task_description(self, context):
        """获取任务描述"""
        return """分析代码库的业务语义和领域逻辑，包括：
1. 识别业务实体、业务操作和业务规则
2. 分析代码元素之间的语义关系和依赖关系
3. 构建业务语义契约和领域模型
4. 使用语义分析技术理解代码的业务意图"""
    
    def _extract_semantics(self, parsing_results: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        从代码解析结果中提取业务语义
        
        Args:
            parsing_results: 代码解析结果列表
            
        Returns:
            语义分析结果字典
        """
        semantic_results = {
            "entities": [],
            "operations": [],
            "intentions": [],
            "business_rules": [],
            "semantic_features": {}
        }
        
        for file_result in parsing_results:
            # 提取文件级别的语义
            file_semantics = self._extract_file_semantics(file_result)
            semantic_results["entities"].extend(file_semantics.get("entities", []))
            semantic_results["operations"].extend(file_semantics.get("operations", []))
            semantic_results["intentions"].extend(file_semantics.get("intentions", []))
            semantic_results["business_rules"].extend(file_semantics.get("business_rules", []))
        
        # 去重
        semantic_results["intentions"] = list(set(semantic_results["intentions"]))
        
        # 计算语义特征
        semantic_results["semantic_features"] = self._calculate_semantic_features(semantic_results)
        
        return semantic_results
    
    def _extract_file_semantics(self, file_result: Dict[str, Any]) -> Dict[str, Any]:
        """
        提取单个文件的语义信息
        
        Args:
            file_result: 文件解析结果
            
        Returns:
            文件语义信息字典
        """
        file_semantics = {
            "entities": [],
            "operations": [],
            "intentions": [],
            "business_rules": []
        }
        
        # 分析类语义
        for class_info in file_result.get("classes", []):
            entity = self._analyze_class_semantics(class_info, file_result["file_path"])
            if entity:
                file_semantics["entities"].append(entity)
            
            operation = self._analyze_operation_semantics(class_info["name"], "class")
            if operation:
                file_semantics["operations"].append(operation)
        
        # 分析函数语义
        for function_info in file_result.get("functions", []):
            entity = self._analyze_function_semantics(function_info, file_result["file_path"])
            if entity:
                file_semantics["entities"].append(entity)
            
            operation = self._analyze_operation_semantics(function_info["name"], "function")
            if operation:
                file_semantics["operations"].append(operation)
        
        # 分析方法语义
        for method_info in file_result.get("methods", []):
            entity = self._analyze_method_semantics(method_info, file_result["file_path"])
            if entity:
                file_semantics["entities"].append(entity)
            
            operation = self._analyze_operation_semantics(method_info["name"], "method")
            if operation:
                file_semantics["operations"].append(operation)
        
        # 分析文件意图
        file_intentions = self._analyze_file_intentions(file_result)
        file_semantics["intentions"].extend(file_intentions)
        
        # 提取业务规则（简化实现）
        business_rules = self._extract_business_rules(file_result)
        file_semantics["business_rules"].extend(business_rules)
        
        return file_semantics
    
    def _analyze_class_semantics(self, class_info: Dict[str, Any], file_path: str) -> Optional[Dict[str, Any]]:
        """分析类语义"""
        class_name = class_info.get("name", "")
        if not class_name:
            return None
        
        entity_type = self._classify_entity_type(class_name)
        if entity_type == "unknown":
            return None
        
        return {
            "name": class_name,
            "type": entity_type,
            "category": "class",
            "file_path": file_path,
            "line": class_info.get("line", 0),
            "description": f"类: {class_name}"
        }
    
    def _analyze_function_semantics(self, function_info: Dict[str, Any], file_path: str) -> Optional[Dict[str, Any]]:
        """分析函数语义"""
        function_name = function_info.get("name", "")
        if not function_name:
            return None
        
        entity_type = self._classify_entity_type(function_name)
        if entity_type == "unknown":
            return None
        
        return {
            "name": function_name,
            "type": entity_type,
            "category": "function",
            "file_path": file_path,
            "line": function_info.get("line", 0),
            "description": f"函数: {function_name}"
        }
    
    def _analyze_method_semantics(self, method_info: Dict[str, Any], file_path: str) -> Optional[Dict[str, Any]]:
        """分析方法语义"""
        method_name = method_info.get("name", "")
        if not method_name:
            return None
        
        entity_type = self._classify_entity_type(method_name)
        if entity_type == "unknown":
            return None
        
        return {
            "name": method_name,
            "type": entity_type,
            "category": "method",
            "file_path": file_path,
            "line": method_info.get("line", 0),
            "description": f"方法: {method_name}"
        }
    
    def _classify_entity_type(self, name: str) -> str:
        """分类实体类型"""
        name_lower = name.lower()
        
        for entity_type, keywords in self.entity_types.items():
            if any(keyword in name_lower for keyword in keywords):
                return entity_type
        
        return "unknown"
    
    def _analyze_operation_semantics(self, name: str, category: str) -> Optional[Dict[str, Any]]:
        """分析操作语义"""
        name_lower = name.lower()
        
        # 业务操作类型
        operation_types = {
            "create": ["create", "add", "insert", "new", "build", "make", "generate"],
            "update": ["update", "modify", "change", "edit", "adjust"],
            "delete": ["delete", "remove", "destroy", "erase", "clear"],
            "query": ["get", "fetch", "query", "find", "search", "read", "select"],
            "validate": ["validate", "check", "verify", "test", "assert"]
        }
        
        for op_type, keywords in operation_types.items():
            if any(keyword in name_lower for keyword in keywords):
                return {
                    "name": name,
                    "type": op_type,
                    "category": category,
                    "description": f"{op_type}操作: {name}"
                }
        
        return None
    
    def _analyze_file_intentions(self, file_result: Dict[str, Any]) -> List[str]:
        """分析文件意图"""
        intentions = []
        file_path = file_result.get("file_path", "").lower()
        
        # 基于文件名和路径分析意图
        for intention_type, keywords in self.intention_types.items():
            if any(keyword in file_path for keyword in keywords):
                intentions.append(intention_type)
        
        # 基于类名分析意图
        for class_info in file_result.get("classes", []):
            class_name = class_info.get("name", "").lower()
            for intention_type, keywords in self.intention_types.items():
                if any(keyword in class_name for keyword in keywords):
                    if intention_type not in intentions:
                        intentions.append(intention_type)
        
        return intentions
    
    def _extract_business_rules(self, file_result: Dict[str, Any]) -> List[str]:
        """提取业务规则（简化实现）"""
        business_rules = []
        
        # 基于方法名提取业务规则
        for method_info in file_result.get("methods", []):
            method_name = method_info.get("name", "").lower()
            
            # 检查是否包含业务规则相关的关键词
            rule_keywords = ["validate", "check", "require", "must", "should", "rule", "policy"]
            if any(keyword in method_name for keyword in rule_keywords):
                business_rules.append(f"业务规则方法: {method_info.get('name', '')}")
        
        return business_rules
    
    def _calculate_semantic_features(self, semantic_results: Dict[str, Any]) -> Dict[str, Any]:
        """计算语义特征"""
        features = {}
        
        # 实体类型分布
        entity_type_dist = {}
        for entity in semantic_results.get("entities", []):
            entity_type = entity.get("type", "unknown")
            entity_type_dist[entity_type] = entity_type_dist.get(entity_type, 0) + 1
        features["entity_type_distribution"] = entity_type_dist
        
        # 操作类型分布
        operation_type_dist = {}
        for operation in semantic_results.get("operations", []):
            op_type = operation.get("type", "unknown")
            operation_type_dist[op_type] = operation_type_dist.get(op_type, 0) + 1
        features["operation_type_distribution"] = operation_type_dist
        
        # 语义丰富度
        total_entities = len(semantic_results.get("entities", []))
        unique_entity_types = len(set(entity.get("type", "") for entity in semantic_results.get("entities", [])))
        features["semantic_richness"] = unique_entity_types / max(total_entities, 1)
        
        return features
    
    def _analyze_semantic_relationships(self, semantic_results: Dict[str, Any]) -> List[Dict[str, Any]]:
        """分析语义关系"""
        relationships = []
        entities = semantic_results.get("entities", [])
        
        # 分析实体间的关系（简化实现）
        for i, entity1 in enumerate(entities):
            for j, entity2 in enumerate(entities):
                if i != j:
                    relationship = self._analyze_entity_relationship(entity1, entity2)
                    if relationship:
                        relationships.append(relationship)
        
        return relationships
    
    def _analyze_entity_relationship(self, entity1: Dict[str, Any], entity2: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """分析实体间的关系"""
        # 基于实体类型和名称分析关系（简化实现）
        name1 = entity1.get("name", "").lower()
        name2 = entity2.get("name", "").lower()
        type1 = entity1.get("type", "")
        type2 = entity2.get("type", "")
        
        # 检查是否包含相同的关键词
        common_words = set(name1.split('_')) & set(name2.split('_'))
        if common_words:
            return {
                "source": entity1["name"],
                "target": entity2["name"],
                "type": "semantic_similarity",
                "strength": len(common_words) / max(len(set(name1.split('_'))), 1),
                "description": f"语义相似性: 共享关键词 {list(common_words)}"
            }
        
        # 检查实体类型是否匹配（如用户-订单关系）
        if type1 == "user" and type2 == "transaction":
            return {
                "source": entity1["name"],
                "target": entity2["name"],
                "type": "business_relationship",
                "strength": 0.8,
                "description": "业务关系: 用户与交易"
            }
        
        return None
    
    def _build_business_abstraction(self, semantic_results: Dict[str, Any], 
                                   relationships: List[Dict[str, Any]]) -> Dict[str, Any]:
        """构建业务抽象模型"""
        abstraction = {
            "domains": [],
            "core_entities": [],
            "key_operations": [],
            "business_processes": []
        }
        
        # 识别核心业务领域
        domains = self._identify_business_domains(semantic_results)
        abstraction["domains"] = domains
        
        # 识别核心实体
        core_entities = self._identify_core_entities(semantic_results)
        abstraction["core_entities"] = core_entities
        
        # 识别关键操作
        key_operations = self._identify_key_operations(semantic_results)
        abstraction["key_operations"] = key_operations
        
        # 识别业务流程
        business_processes = self._identify_business_processes(semantic_results, relationships)
        abstraction["business_processes"] = business_processes
        
        return abstraction
    
    def _identify_business_domains(self, semantic_results: Dict[str, Any]) -> List[str]:
        """识别业务领域"""
        domains = []
        entity_types = set(entity.get("type", "") for entity in semantic_results.get("entities", []))
        
        # 基于实体类型映射到业务领域
        domain_mapping = {
            "user": "用户管理",
            "transaction": "交易管理",
            "product": "产品管理",
            "report": "报表分析",
            "system": "系统管理"
        }
        
        for entity_type in entity_types:
            if entity_type in domain_mapping and entity_type != "unknown":
                domain = domain_mapping[entity_type]
                if domain not in domains:
                    domains.append(domain)
        
        return domains
    
    def _identify_core_entities(self, semantic_results: Dict[str, Any]) -> List[Dict[str, Any]]:
        """识别核心实体"""
        # 基于出现频率和重要性识别核心实体
        entity_count = {}
        for entity in semantic_results.get("entities", []):
            entity_type = entity.get("type", "")
            if entity_type != "unknown":
                entity_count[entity_type] = entity_count.get(entity_type, 0) + 1
        
        core_entities = []
        for entity_type, count in sorted(entity_count.items(), key=lambda x: x[1], reverse=True)[:5]:
            core_entities.append({
                "type": entity_type,
                "count": count,
                "importance": count / max(len(semantic_results.get("entities", [])), 1)
            })
        
        return core_entities
    
    def _identify_key_operations(self, semantic_results: Dict[str, Any]) -> List[Dict[str, Any]]:
        """识别关键操作"""
        # 基于操作频率识别关键操作
        operation_count = {}
        for operation in semantic_results.get("operations", []):
            op_type = operation.get("type", "")
            if op_type != "unknown":
                operation_count[op_type] = operation_count.get(op_type, 0) + 1
        
        key_operations = []
        for op_type, count in sorted(operation_count.items(), key=lambda x: x[1], reverse=True)[:5]:
            key_operations.append({
                "type": op_type,
                "count": count,
                "importance": count / max(len(semantic_results.get("operations", [])), 1)
            })
        
        return key_operations
    
    def _identify_business_processes(self, semantic_results: Dict[str, Any], 
                                   relationships: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """识别业务流程"""
        processes = []
        
        # 基于实体和关系识别业务流程（简化实现）
        # 例如：用户创建 -> 订单创建 -> 支付处理
        key_entities = [entity for entity in semantic_results.get("entities", []) 
                       if entity.get("type") in ["user", "transaction", "product"]]
        
        if len(key_entities) >= 2:
            processes.append({
                "name": "核心业务流程",
                "entities": [entity["name"] for entity in key_entities[:3]],
                "description": "识别的主要业务实体和它们之间的关系"
            })
        
        return processes
    
    def _calculate_semantic_richness(self, semantic_results: Dict[str, Any]) -> float:
        """计算语义丰富度"""
        total_entities = len(semantic_results.get("entities", []))
        if total_entities == 0:
            return 0.0
        
        unique_entity_types = len(set(entity.get("type", "") for entity in semantic_results.get("entities", [])))
        unique_operations = len(set(op.get("type", "") for op in semantic_results.get("operations", [])))
        
        # 语义丰富度 = (实体类型多样性 + 操作类型多样性) / 2
        entity_diversity = unique_entity_types / max(total_entities, 1)
        operation_diversity = unique_operations / max(len(semantic_results.get("operations", [])), 1)
        
        return (entity_diversity + operation_diversity) / 2
    
    def _build_analysis_report(self, semantic_results: Dict[str, Any], 
                              relationships: List[Dict[str, Any]], 
                              business_abstraction: Dict[str, Any]) -> str:
        """构建分析报告"""
        report = f"""# 语义理解分析报告

## 1. 业务语义概览
- **识别实体总数**: {len(semantic_results.get('entities', []))}
- **识别操作总数**: {len(semantic_results.get('operations', []))}
- **语义关系数量**: {len(relationships)}
- **语义丰富度**: {semantic_results.get('semantic_features', {}).get('semantic_richness', 0):.2f}

## 2. 业务实体分析

**实体类型分布**:
"""
        
        # 实体类型分布
        entity_dist = semantic_results.get('semantic_features', {}).get('entity_type_distribution', {})
        for entity_type, count in sorted(entity_dist.items(), key=lambda x: x[1], reverse=True):
            percentage = count / max(len(semantic_results.get('entities', [])), 1) * 100
            report += f"""
- **{entity_type}**: {count} 个实体 ({percentage:.1f}%)
"""
        
        report += f"""
## 3. 业务操作分析

**操作类型分布**:
"""
        
        # 操作类型分布
        op_dist = semantic_results.get('semantic_features', {}).get('operation_type_distribution', {})
        for op_type, count in sorted(op_dist.items(), key=lambda x: x[1], reverse=True):
            percentage = count / max(len(semantic_results.get('operations', [])), 1) * 100
            report += f"""
- **{op_type}**: {count} 个操作 ({percentage:.1f}%)
"""
        
        report += f"""
## 4. 业务领域识别

**识别的主要业务领域**:
"""
        
        # 业务领域
        for domain in business_abstraction.get('domains', []):
            report += f"""
- **{domain}**
"""
        
        report += f"""
## 5. 核心业务实体

**最重要的业务实体**:
"""
        
        # 核心实体
        for i, entity in enumerate(business_abstraction.get('core_entities', [])[:5]):
            report += f"""
{i+1}. **{entity['type']}** - 出现 {entity['count']} 次 (重要性: {entity['importance']:.2f})
"""
        
        report += f"""
## 6. 关键业务操作

**最频繁的业务操作**:
"""
        
        # 关键操作
        for i, operation in enumerate(business_abstraction.get('key_operations', [])[:5]):
            report += f"""
{i+1}. **{operation['type']}** - 出现 {operation['count']} 次 (重要性: {operation['importance']:.2f})
"""
        
        report += f"""
## 7. 语义理解评估

### 业务语义完整性
- **实体覆盖度**: {'良好' if len(semantic_results.get('entities', [])) > 10 else '一般' if len(semantic_results.get('entities', [])) > 5 else '不足'}
- **操作多样性**: {'丰富' if len(set(op.get('type', '') for op in semantic_results.get('operations', []))) > 3 else '一般' if len(set(op.get('type', '') for op in semantic_results.get('operations', []))) > 1 else '单一'}
- **语义关系复杂度**: {'复杂' if len(relationships) > 20 else '中等' if len(relationships) > 10 else '简单'}

### 业务抽象质量
- **领域识别准确性**: {'高' if len(business_abstraction.get('domains', [])) > 2 else '中等' if len(business_abstraction.get('domains', [])) > 1 else '低'}
- **核心实体识别**: {'准确' if len(business_abstraction.get('core_entities', [])) > 3 else '一般'}

### 改进建议
1. 对于语义稀疏的代码，考虑添加更多业务语义注释
2. 确保关键业务实体和操作有清晰的命名规范
3. 建立业务语义文档，提高代码的可理解性
4. 定期进行语义分析，监控业务语义的演变
"""
        
        return report