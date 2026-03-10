"""
LangChain 提示词模板模块

使用 LangChain 的 ChatPromptTemplate 管理所有提示词模板
"""

from langchain_core.prompts import ChatPromptTemplate, SystemMessagePromptTemplate, HumanMessagePromptTemplate
from langchain_core.prompts import MessagesPlaceholder
from typing import Dict, Any, List, Optional


class KnowledgePrompts:
    """知识提取相关提示词"""
    
    # 项目概述分析
    PROJECT_OVERVIEW = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是一个资深的软件架构师，擅长分析代码结构和设计理念。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请分析以下代码项目，生成一份详细的项目概述文档。

## 项目统计
- 实体总数: {entity_count}
- 关系总数: {relationship_count}
- 文件数量: {file_count}

## 主要类 ({main_class_count}个)
{main_classes}

## 主要函数 ({main_function_count}个)
{main_functions}

## 实体类型分布
{entities_summary}

请生成以下内容（使用Markdown格式）：
1. **项目定位** - 这个项目是做什么的？解决什么问题？
2. **技术栈** - 使用了哪些技术和框架？
3. **核心功能** - 主要功能模块有哪些？
4. **设计理念** - 从代码结构推断设计思路
5. **适用场景** - 适合什么场景使用？

请用中文回答，内容要专业、详细。"""
        )
    ])
    
    # 业务流程分析
    BUSINESS_FLOW = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是一个资深的业务分析师，擅长分析软件系统的业务流程。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请分析以下代码项目，生成业务流程分析文档。

## 项目信息
{project_info}

## 核心业务函数
{core_functions}

## 业务类
{business_classes}

请生成以下内容（使用Markdown格式）：
1. **主业务流程** - 系统的主要业务流程是什么？
2. **流程图** - 使用 Mermaid 语法绘制流程图
3. **数据流向** - 数据如何在系统中流动？
4. **关键节点** - 流程中的关键节点和决策点

请用中文回答。"""
        )
    ])
    
    # 架构设计分析
    ARCHITECTURE_DESIGN = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是一个资深的软件架构师，擅长分析系统架构设计。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请分析以下代码项目，生成架构设计分析文档。

## 项目信息
{project_info}

## 核心组件
{core_components}

## 依赖关系
{dependencies}

请生成以下内容（使用Markdown格式）：
1. **架构风格** - 识别架构风格（分层架构、微服务、事件驱动等）
2. **架构图** - 使用 Mermaid 语法绘制架构图
3. **分层说明** - 每一层的职责是什么？
4. **设计模式** - 使用了哪些设计模式？
5. **关键机制** - 如依赖注入、消息队列等

请用中文回答。"""
        )
    ])
    
    # 功能模块分析
    FUNCTIONAL_MODULE = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是一个产品经理和技术文档专家，擅长分析软件功能模块。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请分析以下代码项目的功能模块。

## 功能模块信息
{module_info}

## 模块依赖
{module_dependencies}

请为每个功能模块生成以下信息（使用Markdown格式）：
1. **功能描述** - 模块的功能说明
2. **使用场景** - 何时使用这个模块？
3. **输入输出** - 模块的输入和输出是什么？
4. **依赖关系** - 模块依赖哪些其他模块？
5. **关键接口** - 模块的主要接口列表

请用中文回答。"""
        )
    ])
    
    # 核心实体分析
    CORE_ENTITY = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是一个领域驱动设计（DDD）专家，擅长分析业务领域模型。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请分析以下代码项目的核心实体。

## 核心实体列表
{core_entities}

## 实体关系
{entity_relationships}

请为每个核心实体生成以下信息（使用Markdown格式）：
1. **业务含义** - 这个实体代表什么业务概念？
2. **职责边界** - 实体的核心职责是什么？
3. **生命周期** - 实体的创建、修改、删除流程
4. **不变量** - 实体需要维护哪些不变量？
5. **领域事件** - 实体会触发哪些领域事件？

请用中文回答。"""
        )
    ])
    
    # API 接口分析
    API_INTERFACE = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是一个 API 文档专家，擅长分析和整理 API 接口。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请分析以下代码项目的 API 接口。

## API 接口列表
{api_interfaces}

## 路由定义
{routes}

请为每个 API 生成以下信息（使用Markdown格式）：
1. **接口详情** - 请求方法、路径、参数
2. **功能说明** - 接口的用途和功能
3. **请求示例** - 请求格式和示例
4. **响应示例** - 响应格式和示例
5. **认证方式** - 需要什么认证？
6. **错误码** - 可能的错误码及说明

请用中文回答。"""
        )
    ])


class TestingPrompts:
    """测试相关提示词"""
    
    # 测试用例生成
    TEST_CASE_GENERATION = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是测试用例生成专家，请为提供的代码实体生成单元测试用例。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请为以下代码生成单元测试用例。

## 代码实体
{entity_name}
{entity_code}

## 代码类型
{entity_type}

请生成测试用例，每个测试用例包含：
1. 测试名称
2. 测试场景描述
3. 输入参数
4. 预期输出
5. 测试步骤

请使用 JSON 数组格式返回，每个元素是一个测试用例对象。"""
        )
    ])
    
    # 测试失败分析
    TEST_FAILURE_ANALYSIS = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是测试专家，请分析测试失败的原因，并给出代码修复建议。"
        ),
        HumanMessagePromptTemplate.from_template(
            """以下测试运行失败，请分析失败原因。

## 测试代码
{test_code}

## 错误信息
{error_message}

## 被测代码
{source_code}

请分析并返回 JSON 格式的结果，包含：
1. **failure_reason** - 失败原因（一句话概括）
2. **code_issues** - 代码问题列表
3. **fix_suggestions** - 修复建议（代码级别的建议）
4. **test_improvements** - 测试用例改进建议（如果有）"""
        )
    ])


class ValidationPrompts:
    """验证相关提示词"""
    
    # 知识验证
    KNOWLEDGE_VALIDATION = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是知识验证专家，请验证提取的知识是否与原始内容一致。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请验证以下提取的知识是否准确。

## 提取的知识
{extracted_knowledge}

## 原始内容
{original_content}

## 提取类型
{extraction_type}

请验证并返回 JSON 格式的结果，包含：
1. **is_valid** - 布尔值，表示知识是否准确
2. **confidence** - 0-1 的置信度
3. **errors** - 错误列表（如果有）
4. **suggestions** - 修正建议（如果有）"""
        )
    ])
    
    # 知识图谱验证
    GRAPH_VALIDATION = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是知识图谱验证专家，请验证知识图谱的准确性和完整性。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请验证以下知识图谱。

## 图谱统计
{graph_stats}

## 关键实体
{key_entities}

## 关键关系
{key_relationships}

请验证并返回 JSON 格式的结果，包含：
1. **accuracy** - 准确性评分（0-100）
2. **completeness** - 完整性评分（0-100）
3. **consistency** - 一致性评分（0-100）
4. **issues** - 发现的问题列表
5. **recommendations** - 改进建议"""
        )
    ])


class OptimizationPrompts:
    """优化相关提示词"""
    
    # 知识优化
    KNOWLEDGE_OPTIMIZATION = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是一个代码知识图谱优化专家，请对输入的代码知识图谱进行优化。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请优化以下代码知识图谱。

## 当前图谱
{knowledge_graph}

## 优化级别
{optimization_level}

请执行以下优化任务：
1. 补全缺失的函数描述、参数说明、返回值说明
2. 修正错误的依赖关系和调用关系
3. 识别潜在的技术债务和重构点
4. 补充代码复杂度、性能等分析信息
5. 优化实体命名和描述的准确性

请以结构化的格式返回优化结果。"""
        )
    ])
    
    # 代码优化建议
    CODE_OPTIMIZATION = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是资深软件工程师，擅长{analysis_type}分析和代码优化。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请分析以下代码，提供优化建议。

## 代码内容
{code_content}

## 上下文信息
{context_info}

请返回 JSON 格式的分析结果，包含：
1. **issues** - 发现的问题列表
2. **suggestions** - 优化建议
3. **refactored_code** - 重构后的代码（可选）
4. **explanation** - 详细说明"""
        )
    ])


class AnalysisPrompts:
    """分析相关提示词"""
    
    # 代码复杂度分析
    COMPLEXITY_ANALYSIS = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是代码分析专家，擅长分析代码复杂度。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请分析以下代码的复杂度。

## 代码内容
{code_content}

## 代码类型
{code_type}

请返回 JSON 格式的分析结果，包含：
1. **cyclomatic_complexity** - 圈复杂度
2. **cognitive_complexity** - 认知复杂度
3. **maintainability_index** - 可维护性指数
4. **complexity_level** - 复杂度等级（低/中/高）
5. **recommendations** - 简化建议"""
        )
    ])
    
    # 架构模式检测
    ARCHITECTURE_PATTERN_DETECTION = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是架构分析专家，擅长识别软件架构模式。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请分析以下项目的架构模式。

## 项目结构
{project_structure}

## 依赖关系
{dependencies}

## 关键类和模块
{key_components}

请返回 JSON 格式的分析结果，包含：
1. **detected_patterns** - 检测到的架构模式列表
2. **pattern_descriptions** - 每个模式的描述
3. **pattern_location** - 每个模式在代码中的位置
4. **recommendations** - 架构改进建议"""
        )
    ])
    
    # 代码异味检测
    CODE_SMELL_DETECTION = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template(
            "你是代码质量专家，擅长检测代码异味。"
        ),
        HumanMessagePromptTemplate.from_template(
            """请检测以下代码中的代码异味。

## 代码内容
{code_content}

## 文件结构
{file_structure}

请返回 JSON 格式的分析结果，包含：
1. **smells** - 检测到的代码异味列表
2. **severity** - 每个异味的严重程度
3. **location** - 每个异味的位置
4. **fix_suggestions** - 修复建议"""
        )
    ])


def format_entities_for_prompt(entities: List[Dict[str, Any]], max_count: int = 20) -> str:
    """
    格式化实体列表，用于提示词
    
    Args:
        entities: 实体列表
        max_count: 最大显示数量
        
    Returns:
        格式化后的字符串
    """
    if not entities:
        return "无"
    
    formatted = []
    for i, entity in enumerate(entities[:max_count]):
        formatted.append(f"- {entity.get('name', 'Unknown')} ({entity.get('type', 'unknown')})")
    
    if len(entities) > max_count:
        formatted.append(f"\n... 还有 {len(entities) - max_count} 个实体")
    
    return "\n".join(formatted)


__all__ = [
    "KnowledgePrompts",
    "TestingPrompts",
    "ValidationPrompts",
    "OptimizationPrompts",
    "AnalysisPrompts",
    "format_entities_for_prompt",
]
