"""
语义提取Agent - 负责代码语义特征提取与LLM增强分析

功能：
- 代码语义特征提取
- LLM增强的语义理解
- 代码意图识别
- 业务逻辑标注
- 语义关系分析
"""

from typing import List, Dict, Any, Optional
from .base_agent import BaseAgent
from src.models.business_semantic import BusinessSemanticContract, BusinessEntity
from src.ai.llm_client import LLMClient


class SemanticExtractorAgent(BaseAgent):
    """语义提取Agent"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__("semantic_extractor", config)
        self.llm_client = None
        self.enable_llm = config.get("enable_llm", True)
        
        # 初始化LLM客户端
        if self.enable_llm:
            ai_config = config.get("ai", {})
            self.llm_client = LLMClient(ai_config)
    
    async def _execute_impl(self, input_data: Any) -> BusinessSemanticContract:
        """执行语义提取"""
        if isinstance(input_data, dict):
            code_metadata = input_data.get("code_metadata")
            file_content = input_data.get("content")
        else:
            raise ValueError("输入数据应包含代码元数据和内容")
        
        if not code_metadata:
            raise ValueError("缺少代码元数据")
        
        self.logger.info(f"开始语义提取: {code_metadata.file_path}")
        
        # 创建语义契约
        semantic_contract = BusinessSemanticContract(
            file_path=code_metadata.file_path
        )
        
        # 提取基础语义特征
        await self._extract_basic_semantics(code_metadata, semantic_contract)
        
        # 如果启用LLM，进行深度语义分析
        if self.enable_llm and self.llm_client and self.llm_client.is_available():
            await self._extract_llm_enhanced_semantics(
                code_metadata, file_content, semantic_contract
            )
        
        self.logger.info(f"语义提取完成，识别 {len(semantic_contract.entities)} 个业务实体")
        return semantic_contract
    
    async def _extract_basic_semantics(self, code_metadata: Any, semantic_contract: BusinessSemanticContract):
        """提取基础语义特征"""
        # 从函数和类中提取业务语义
        for func in code_metadata.functions:
            entity = self._analyze_function_semantics(func)
            if entity:
                semantic_contract.entities.append(entity)
        
        for cls in code_metadata.classes:
            entity = self._analyze_class_semantics(cls)
            if entity:
                semantic_contract.entities.append(entity)
        
        # 分析代码意图
        semantic_contract.intentions = self._analyze_code_intentions(code_metadata)
    
    def _analyze_function_semantics(self, func_metadata: Any) -> Optional[BusinessEntity]:
        """分析函数语义"""
        if not func_metadata.name:
            return None
        
        # 根据函数名和文档推断业务语义
        entity_type = self._classify_entity_type(func_metadata.name, func_metadata.docstring)
        
        if entity_type == "unknown":
            return None
        
        return BusinessEntity(
            name=func_metadata.name,
            entity_type=entity_type,
            description=func_metadata.docstring or f"函数: {func_metadata.name}",
            source_type="function",
            metadata={
                "parameters": func_metadata.parameters,
                "return_type": func_metadata.return_type,
                "start_line": func_metadata.start_line,
                "end_line": func_metadata.end_line
            }
        )
    
    def _analyze_class_semantics(self, class_metadata: Any) -> Optional[BusinessEntity]:
        """分析类语义"""
        if not class_metadata.name:
            return None
        
        # 根据类名和文档推断业务语义
        entity_type = self._classify_entity_type(class_metadata.name, class_metadata.docstring)
        
        if entity_type == "unknown":
            return None
        
        return BusinessEntity(
            name=class_metadata.name,
            entity_type=entity_type,
            description=class_metadata.docstring or f"类: {class_metadata.name}",
            source_type="class",
            metadata={
                "base_classes": class_metadata.base_classes,
                "methods_count": len(class_metadata.methods),
                "variables_count": len(class_metadata.class_variables)
            }
        )
    
    def _classify_entity_type(self, name: str, docstring: Optional[str]) -> str:
        """分类实体类型"""
        name_lower = name.lower()
        
        # 业务操作相关
        if any(keyword in name_lower for keyword in [
            'create', 'add', 'insert', 'new', 'build', 'make', 'generate'
        ]):
            return "creation"
        
        if any(keyword in name_lower for keyword in [
            'update', 'modify', 'change', 'edit', 'adjust'
        ]):
            return "modification"
        
        if any(keyword in name_lower for keyword in [
            'delete', 'remove', 'destroy', 'erase', 'clear'
        ]):
            return "deletion"
        
        if any(keyword in name_lower for keyword in [
            'get', 'fetch', 'query', 'find', 'search', 'read', 'select'
        ]):
            return "query"
        
        if any(keyword in name_lower for keyword in [
            'validate', 'check', 'verify', 'test', 'assert'
        ]):
            return "validation"
        
        if any(keyword in name_lower for keyword in [
            'calculate', 'compute', 'process', 'analyze', 'transform'
        ]):
            return "calculation"
        
        # 业务实体相关
        if any(keyword in name_lower for keyword in [
            'user', 'customer', 'client', 'member', 'person'
        ]):
            return "user"
        
        if any(keyword in name_lower for keyword in [
            'order', 'transaction', 'payment', 'invoice', 'bill'
        ]):
            return "transaction"
        
        if any(keyword in name_lower for keyword in [
            'product', 'item', 'service', 'good', 'commodity'
        ]):
            return "product"
        
        if any(keyword in name_lower for keyword in [
            'report', 'statistic', 'metric', 'analytics', 'dashboard'
        ]):
            return "report"
        
        return "unknown"
    
    def _analyze_code_intentions(self, code_metadata: Any) -> List[str]:
        """分析代码意图"""
        intentions = []
        
        # 根据函数和类名分析意图
        all_names = [func.name for func in code_metadata.functions] + [cls.name for cls in code_metadata.classes]
        
        for name in all_names:
            name_lower = name.lower()
            
            if any(keyword in name_lower for keyword in ['api', 'endpoint', 'route', 'controller']):
                intentions.append("api_implementation")
            
            if any(keyword in name_lower for keyword in ['service', 'manager', 'handler', 'processor']):
                intentions.append("business_logic")
            
            if any(keyword in name_lower for keyword in ['model', 'entity', 'schema', 'dto']):
                intentions.append("data_modeling")
            
            if any(keyword in name_lower for keyword in ['util', 'helper', 'tool', 'common']):
                intentions.append("utility_function")
        
        return list(set(intentions))
    
    async def _extract_llm_enhanced_semantics(self, 
                                            code_metadata: Any, 
                                            file_content: str,
                                            semantic_contract: BusinessSemanticContract):
        """使用LLM进行深度语义分析"""
        if not self.llm_client or not self.llm_client.is_available():
            return
        
        try:
            # 构建LLM提示词
            prompt = self._build_llm_prompt(code_metadata, file_content)
            
            # 调用LLM
            response = await self.llm_client.chat_async(
                system_prompt="你是一个专业的代码语义分析专家，能够准确识别代码中的业务逻辑和语义特征。",
                user_prompt=prompt
            )
            
            # 解析LLM响应
            if response:
                self._parse_llm_response(response, semantic_contract)
                
        except Exception as e:
            self.logger.warning(f"LLM语义分析失败: {e}")
    
    def _build_llm_prompt(self, code_metadata: Any, file_content: str) -> str:
        """构建LLM提示词"""
        prompt = f"""
请分析以下代码的语义特征和业务逻辑：

文件路径: {code_metadata.file_path}

代码内容：
```
{file_content}
```

请从以下维度进行分析：
1. 识别代码中的业务实体（如用户、订单、产品等）
2. 分析业务操作类型（增删改查、计算、验证等）
3. 识别代码的业务意图和领域逻辑
4. 标注重要的业务规则和约束

请以JSON格式返回分析结果，包含以下字段：
- entities: 业务实体列表
- operations: 业务操作类型
- business_rules: 业务规则
- domain_logic: 领域逻辑描述
"""
        return prompt
    
    def _parse_llm_response(self, response: str, semantic_contract: BusinessSemanticContract):
        """解析LLM响应"""
        try:
            # 尝试解析JSON响应
            import json
            response_data = json.loads(response)
            
            # 更新语义契约
            if "entities" in response_data:
                for entity_data in response_data["entities"]:
                    entity = BusinessEntity(
                        name=entity_data.get("name", ""),
                        entity_type=entity_data.get("type", "unknown"),
                        description=entity_data.get("description", ""),
                        source_type="llm_analysis",
                        metadata=entity_data
                    )
                    semantic_contract.entities.append(entity)
            
            if "business_rules" in response_data:
                semantic_contract.business_rules = response_data["business_rules"]
            
            if "domain_logic" in response_data:
                semantic_contract.domain_logic = response_data["domain_logic"]
                
        except json.JSONDecodeError:
            # 如果不是JSON格式，尝试提取关键信息
            self._extract_info_from_text_response(response, semantic_contract)
    
    def _extract_info_from_text_response(self, response: str, semantic_contract: BusinessSemanticContract):
        """从文本响应中提取信息"""
        # 简单的关键词提取
        import re
        
        # 提取业务实体
        entity_patterns = [
            r"业务实体[:：]\s*([^。]+)",
            r"实体[:：]\s*([^。]+)",
            r"识别到[^:：]*[:：]\s*([^。]+)"
        ]
        
        for pattern in entity_patterns:
            matches = re.findall(pattern, response)
            for match in matches:
                entity = BusinessEntity(
                    name=match.strip(),
                    entity_type="extracted",
                    description=f"从LLM分析中提取: {match}",
                    source_type="llm_text_analysis"
                )
                semantic_contract.entities.append(entity)
    
    def is_available(self) -> bool:
        """检查Agent是否可用"""
        if self.enable_llm:
            return self.llm_client and self.llm_client.is_available()
        return True