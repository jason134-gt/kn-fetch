# LangChain 重构使用说明

## 概述

项目已使用 LangChain 对智能体和 LLM 部分进行了重构，提供了更强大、更灵活的 AI 能力。

## 新增组件

### 1. LangChain 配置模块 (`src/ai/langchain_config.py`)

提供统一的 LLM 提供商配置和管理。

```python
from src.ai import LangChainConfig, create_langchain_llm

# 创建配置
config = LangChainConfig(config_dict)

# 创建 LLM 实例
llm = create_langchain_llm(config)
```

### 2. LangChain LLM 客户端 (`src/ai/langchain_llm_client.py`)

基于 LangChain 的统一 LLM 调用接口。

```python
from src.ai import LangChainLLMClient

# 创建客户端
client = LangChainLLMClient(config)

# 同步调用
result = client.chat_sync("你是一个专家", "分析这段代码")

# 异步调用
result = await client.chat("你是一个专家", "分析这段代码")

# 代码分析
analysis = await client.analyze_code(code_content, "complexity")

# 生成测试用例
tests = await client.generate_test_cases(code, "EntityName", "function")
```

### 3. LangChain 提示词模板 (`src/ai/langchain_prompts.py`)

使用 LangChain 的 ChatPromptTemplate 管理所有提示词。

```python
from src.ai import KnowledgePrompts, TestingPrompts, ValidationPrompts

# 使用预定义的提示词模板
prompt = KnowledgePrompts.PROJECT_OVERVIEW
chain = prompt | llm
result = await chain.ainvoke({...})
```

### 4. LangChain 深度分析器 (`src/ai/langchain_deep_analyzer.py`)

使用 LangChain 重构的深度知识分析器。

```python
from src.ai import LangChainDeepAnalyzer

analyzer = LangChainDeepAnalyzer(config)
results = analyzer.analyze_all(graph)
```

### 5. LangChain Agent 编排器 (`src/ai/langchain_agent_orchestrator.py`)

使用 LangGraph 实现多智能体协作编排。

```python
from src.ai import LangChainAgentOrchestrator, SimpleAgentOrchestrator

# 完整版编排器（使用 LangGraph）
orchestrator = LangChainAgentOrchestrator(config)
final_state = await orchestrator.run(knowledge_graph)

# 简化版编排器（快速执行）
simple_orchestrator = SimpleAgentOrchestrator(config)
results = await simple_orchestrator.analyze_and_validate(graph)
```

## 安装依赖

```bash
# 安装 LangChain 相关依赖
pip install -r requirements.txt
```

新增的依赖包括：
- `langchain>=0.1.0`
- `langchain-openai>=0.0.5`
- `langchain-community>=0.0.20`
- `langgraph>=0.0.20`

## 配置文件更新

在 `config/config.yaml` 中添加了新的配置选项：

```yaml
ai:
  langchain:
    enabled: true  # 是否启用 LangChain
  
  agent:
    enabled: true
    orchestrator_type: "graph"  # "graph" 或 "simple"
    max_iterations: 10
    timeout_seconds: 300
  
  # 支持的提供商扩展
  anthropic:
    api_key: ""
    model: "claude-3-opus-20240229"
  
  qwen:
    api_key: ""
    model: "qwen-max"
```

## 使用示例

### 示例 1: 基础 LLM 调用

```python
import asyncio
from src.ai import LangChainLLMClient

async def main():
    client = LangChainLLMClient({
        "provider": "volcengine",
        "volcengine": {
            "api_key": "your-api-key"
        }
    })
    
    result = await client.chat(
        system_prompt="你是一个代码分析专家",
        user_prompt="分析以下 Python 代码..."
    )
    
    print(result)

asyncio.run(main())
```

### 示例 2: 深度知识分析

```python
import asyncio
from src.ai import LangChainDeepAnalyzer
from src.gitnexus import GitNexusClient

async def main():
    # 加载知识图谱
    client = GitNexusClient("config/config.yaml")
    graph = client._load_knowledge_graph()
    
    # 创建分析器
    analyzer = LangChainDeepAnalyzer(client.config)
    
    # 执行分析
    results = analyzer.analyze_all(graph)
    
    print(f"生成了 {len(results)} 份文档")

asyncio.run(main())
```

### 示例 3: Agent 编排

```python
import asyncio
from src.ai import LangChainAgentOrchestrator

async def main():
    # 创建编排器
    orchestrator = LangChainAgentOrchestrator(config)
    
    # 运行智能体流程
    final_state = await orchestrator.run(knowledge_graph)
    
    # 查看结果
    print(f"分析完成: {final_state['analysis_results']}")
    print(f"验证通过: {final_state['validated']}")
    print(f"优化完成: {final_state['optimized']}")

asyncio.run(main())
```

## 兼容性说明

- 旧版模块（`AIClient`, `LLMClient` 等）仍然保留，确保向后兼容
- 新旧模块可以共存，可以逐步迁移到 LangChain 版本
- 建议新功能使用 LangChain 模块开发

## 性能优化

1. **异步支持**: 所有 LangChain 模块都支持异步调用，提高并发性能
2. **缓存机制**: `LangChainLLMClientFactory` 提供实例缓存
3. **链式调用**: 使用 LangChain 的 LCEL 语法，提高调用效率

## 扩展指南

### 添加新的提示词模板

```python
# 在 langchain_prompts.py 中添加
class CustomPrompts:
    CUSTOM_ANALYSIS = ChatPromptTemplate.from_messages([
        SystemMessagePromptTemplate.from_template("你是一个专家"),
        HumanMessagePromptTemplate.from_template("自定义提示词")
    ])
```

### 添加新的智能体

```python
# 在 langchain_agent_orchestrator.py 中添加
async def _custom_agent(self, state: AgentState) -> AgentState:
    # 自定义逻辑
    return state

# 在 _build_agent_graph 中注册
workflow.add_node("custom_agent", self._custom_agent)
```

## 故障排查

### 1. 导入错误

```
ModuleNotFoundError: No module named 'langchain'
```

解决: `pip install langchain`

### 2. API 连接失败

```
ValueError: LLM 不可用，请检查配置和 API Key
```

解决: 检查 `config/config.yaml` 中的 `api_key` 配置

### 3. LangGraph 不可用

```
ModuleNotFoundError: No module named 'langgraph'
```

解决: `pip install langgraph`

## 参考资料

- [LangChain 文档](https://python.langchain.com/)
- [LangGraph 文档](https://langchain-ai.github.io/langgraph/)
- [项目 README](../../README.md)

## 贡献指南

欢迎提交 PR 和 Issue，帮助改进 LangChain 集成。
