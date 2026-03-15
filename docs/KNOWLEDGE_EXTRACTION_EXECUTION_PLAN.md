# 知识提取智能体优化 - 执行计划

> 版本：v1.0
> 创建时间：2026-03-12
> 总周期：9周
> 方案：渐进式重构

---

## 📅 总体时间表

```
Week 1-2: Phase 1 - Agent协作框架
Week 3-5: Phase 2 - 多阶段工作流
Week 6-7: Phase 3 - 输出质量优化
Week 8-9: Phase 4 - 质量控制增强
```

---

## 🎯 Phase 1: Agent协作框架（Week 1-2）

### 目标
建立多Agent协作基础架构，实现Agent定义、注册、并行执行机制。

### 核心交付物

#### 1.1 Agent基础框架（Week 1）

**文件结构：**
```
src/agents/
├── __init__.py
├── base/
│   ├── __init__.py
│   ├── agent.py              # BaseAgent基类
│   ├── context.py            # AgentContext上下文
│   ├── result.py             # AgentResult结果类
│   └── message.py            # AgentMessage消息类
├── orchestrator/
│   ├── __init__.py
│   ├── orchestrator.py       # AgentOrchestrator编排器
│   ├── executor.py           # AgentExecutor执行器
│   └── scheduler.py          # AgentScheduler调度器
├── roles/
│   ├── __init__.py
│   ├── architecture/         # 架构分析角色
│   │   ├── overview_agent.py
│   │   ├── layers_agent.py
│   │   ├── dependencies_agent.py
│   │   ├── dataflow_agent.py
│   │   └── entrypoints_agent.py
│   ├── design/               # 设计分析角色
│   │   ├── patterns_agent.py
│   │   ├── classes_agent.py
│   │   ├── interfaces_agent.py
│   │   └── state_agent.py
│   └── methods/              # 方法分析角色
│       ├── algorithms_agent.py
│       ├── paths_agent.py
│       ├── apis_agent.py
│       └── logic_agent.py
└── config/
    ├── __init__.py
    └── agent_configs.py      # Agent配置定义
```

**核心类设计：**

##### BaseAgent（基类）
```python
# src/agents/base/agent.py
from abc import ABC, abstractmethod
from typing import Dict, Any, Optional
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

@dataclass
class AgentRole:
    """Agent角色定义"""
    name: str                    # 角色名称
    role: str                    # 角色描述（中文）
    focus: str                   # 关注点
    constraint: str              # 约束条件
    output_template: str         # 输出模板路径

@dataclass
class AgentContext:
    """Agent执行上下文"""
    graph: Any                   # KnowledgeGraph
    config: Any                  # AnalysisConfig
    output_dir: Path             # 输出目录
    exploration_data: Dict[str, Any] = field(default_factory=dict)
    session_id: str = ""
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    
    def get_exploration_file(self, angle: str) -> Optional[Path]:
        """获取探索文件路径"""
        return self.exploration_data.get(angle)
    
    def get_output_file(self, agent_name: str) -> Path:
        """获取输出文件路径"""
        return self.output_dir / "sections" / f"section-{agent_name}.md"

@dataclass
class AgentResult:
    """Agent执行结果"""
    agent_name: str              # Agent名称
    status: str                  # completed/partial/failed
    output_file: Optional[str]   # 输出文件路径
    summary: str                 # 50字以内的摘要
    cross_module_notes: list = field(default_factory=list)
    stats: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None
    execution_time: float = 0.0
    
    def to_json(self) -> Dict[str, Any]:
        """转换为JSON格式"""
        return {
            "status": self.status,
            "output_file": self.output_file,
            "summary": self.summary,
            "cross_module_notes": self.cross_module_notes,
            "stats": self.stats
        }

class BaseAgent(ABC):
    """Agent基类"""
    
    def __init__(self, role: AgentRole, llm_client: Any):
        self.role = role
        self.llm = llm_client
        self.name = role.name
    
    @abstractmethod
    def analyze(self, context: AgentContext) -> AgentResult:
        """
        执行分析任务
        
        Args:
            context: Agent执行上下文
            
        Returns:
            Agent执行结果
        """
        pass
    
    def _build_prompt(self, context: AgentContext) -> str:
        """构建LLM提示词"""
        prompt = f"""[SPEC]
首先读取规范文件：
- Read: {context.output_dir}/specs/quality-standards.md
- Read: {context.output_dir}/specs/writing-style.md
严格遵循规范中的质量标准和段落式写作要求。

[ROLE] {self.role.role}

[TASK]
{self._get_task_description(context)}
输出: {context.get_output_file(self.name)}

[STYLE]
- 严谨专业的中文技术写作，专业术语保留英文
- 完全客观的第三人称视角，严禁"我们"、"开发者"
- 段落式叙述，采用"论点-论据-结论"结构
- 善用逻辑连接词体现设计推演过程

[FOCUS]
{self.role.focus}

[CONSTRAINT]
{self.role.constraint}

[RETURN JSON]
{{"status":"completed","output_file":"section-{self.name}.md","summary":"<50字>","cross_module_notes":[],"stats":{{}}}}
"""
        return prompt
    
    @abstractmethod
    def _get_task_description(self, context: AgentContext) -> str:
        """获取任务描述（子类实现）"""
        pass
    
    def _save_output(self, content: str, output_file: Path):
        """保存输出文件"""
        output_file.parent.mkdir(parents=True, exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(content)
```

##### AgentOrchestrator（编排器）
```python
# src/agents/orchestrator/orchestrator.py
from typing import Dict, List, Any
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import time
import logging

from ..base.agent import BaseAgent, AgentContext, AgentResult
from ..base.message import AgentMessage

logger = logging.getLogger(__name__)

class AgentOrchestrator:
    """Agent编排器 - 管理Agent的注册和调度"""
    
    def __init__(self, max_workers: int = 5):
        self.agents: Dict[str, BaseAgent] = {}
        self.max_workers = max_workers
        self.message_queue = []  # Agent间消息队列
    
    def register_agent(self, name: str, agent: BaseAgent):
        """注册Agent"""
        self.agents[name] = agent
        logger.info(f"Registered agent: {name}")
    
    def register_agents(self, agents: Dict[str, BaseAgent]):
        """批量注册Agent"""
        for name, agent in agents.items():
            self.register_agent(name, agent)
    
    def run_single(self, agent_name: str, context: AgentContext) -> AgentResult:
        """执行单个Agent"""
        if agent_name not in self.agents:
            raise ValueError(f"Agent '{agent_name}' not found")
        
        agent = self.agents[agent_name]
        start_time = time.time()
        
        try:
            logger.info(f"Running agent: {agent_name}")
            result = agent.analyze(context)
            result.execution_time = time.time() - start_time
            logger.info(f"Agent {agent_name} completed in {result.execution_time:.2f}s")
            return result
        except Exception as e:
            logger.error(f"Agent {agent_name} failed: {str(e)}")
            return AgentResult(
                agent_name=agent_name,
                status="failed",
                output_file=None,
                summary="",
                error=str(e),
                execution_time=time.time() - start_time
            )
    
    def run_parallel(
        self, 
        agent_names: List[str], 
        context: AgentContext
    ) -> Dict[str, AgentResult]:
        """并行执行多个Agent"""
        results = {}
        
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            # 提交所有任务
            future_to_agent = {
                executor.submit(self.run_single, name, context): name 
                for name in agent_names
            }
            
            # 收集结果
            for future in as_completed(future_to_agent):
                agent_name = future_to_agent[future]
                try:
                    result = future.result()
                    results[agent_name] = result
                except Exception as e:
                    logger.error(f"Agent {agent_name} execution failed: {str(e)}")
                    results[agent_name] = AgentResult(
                        agent_name=agent_name,
                        status="failed",
                        output_file=None,
                        summary="",
                        error=str(e)
                    )
        
        return results
    
    def send_message(self, message: AgentMessage):
        """Agent间消息传递"""
        self.message_queue.append(message)
        logger.debug(f"Message queued: {message.from_agent} -> {message.to_agent}")
    
    def get_messages(self, to_agent: str) -> List[AgentMessage]:
        """获取指定Agent的消息"""
        return [msg for msg in self.message_queue if msg.to_agent == to_agent]
```

##### AgentMessage（消息类）
```python
# src/agents/base/message.py
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Dict

@dataclass
class AgentMessage:
    """Agent间消息"""
    from_agent: str              # 发送方Agent
    to_agent: str                # 接收方Agent
    message_type: str            # 消息类型
    content: Dict[str, Any]      # 消息内容
    timestamp: str = None
    
    def __post_init__(self):
        if self.timestamp is None:
            self.timestamp = datetime.now().isoformat()
```

#### 1.2 专业Agent角色定义（Week 1）

**文件：** `src/agents/config/agent_configs.py`

```python
from ..base.agent import AgentRole

# 架构分析角色
OVERVIEW_AGENT = AgentRole(
    name="overview",
    role="首席系统架构师",
    focus="""- 领域边界与定位：系统旨在解决什么核心业务问题？其在更大的技术生态中处于什么位置？
- 架构范式：采用何种架构风格（分层、六边形、微服务、事件驱动等）？选择该范式的根本原因是什么？
- 核心技术决策：关键技术栈的选型依据，这些选型如何支撑系统的非功能性需求（性能、扩展性、维护性）
- 顶层模块划分：系统在最高层级被划分为哪些逻辑单元？它们之间的高层协作机制是怎样的？""",
    constraint="避免罗列目录结构，重点阐述设计意图，包含至少1个 Mermaid 架构图",
    output_template="templates/sections/overview.md"
)

LAYERS_AGENT = AgentRole(
    name="layers",
    role="资深软件设计师",
    focus="""- 职责分配体系：系统被划分为哪几个逻辑层级？每一层的核心职责和输入输出是什么？
- 数据流向与约束：数据在各层之间是如何流动的？是否存在严格的单向依赖规则？
- 边界隔离策略：各层之间通过何种方式解耦（接口抽象、DTO转换、依赖注入）？如何防止下层实现细节泄露到上层？
- 异常处理流：异常信息如何在分层结构中传递和转化？""",
    constraint="不要列举具体的文件名列表，关注层级间的契约和隔离的艺术",
    output_template="templates/sections/layers.md"
)

DEPENDENCIES_AGENT = AgentRole(
    name="dependencies",
    role="集成架构专家",
    focus="""- 外部集成拓扑：系统如何与外部世界（第三方API、数据库、中间件）交互？采用了何种适配器或防腐层设计来隔离外部变化？
- 核心依赖分析：区分核心业务依赖与基础设施依赖。系统对关键框架的依赖程度如何？是否存在被锁定的风险？
- 依赖注入与控制反转：系统内部模块间的组装方式是什么？是否实现了依赖倒置原则以支持可测试性？
- 供应链安全与治理：对于复杂的依赖树，系统采用了何种策略来管理版本和兼容性？""",
    constraint="禁止简单列出依赖配置文件的内容，必须分析依赖背后的集成策略和风险控制模型",
    output_template="templates/sections/dependencies.md"
)

DATAFLOW_AGENT = AgentRole(
    name="dataflow",
    role="数据架构师",
    focus="""- 数据入口与出口：数据从何处进入系统，最终流向何处？边界处的数据校验和转换策略是什么？
- 数据转换管道：数据在各层/模块间经历了怎样的形态变化？DTO、Entity、VO 等数据对象的职责边界如何划分？
- 持久化策略：系统如何设计数据存储方案？采用了何种 ORM 策略或数据访问模式？
- 一致性保障：系统如何处理事务边界？分布式场景下如何保证数据一致性？""",
    constraint="关注数据的生命周期和形态演变，不要罗列数据库表结构",
    output_template="templates/sections/dataflow.md"
)

ENTRYPOINTS_AGENT = AgentRole(
    name="entrypoints",
    role="系统边界分析师",
    focus="""- 入口类型与职责：系统提供了哪些类型的入口（REST API、CLI、消息队列消费者、定时任务）？各入口的设计目的和适用场景是什么？
- 请求处理管道：从入口到核心逻辑，请求经过了怎样的处理管道？中间件/拦截器的编排逻辑是什么？
- 关键业务路径：最重要的几条业务流程的调用链是怎样的？关键节点的设计考量是什么？
- 异常与边界处理：系统如何统一处理异常？异常信息如何传播和转化？""",
    constraint="关注入口的设计哲学而非 API 清单，不要逐个列举所有端点",
    output_template="templates/sections/entrypoints.md"
)

# 设计分析角色
PATTERNS_AGENT = AgentRole(
    name="patterns",
    role="核心开发规范制定者",
    focus="""- 架构级模式：识别系统中广泛使用的架构模式（CQRS、Event Sourcing、Repository Pattern、Unit of Work）。阐述引入这些模式解决了什么特定难题
- 通信与并发模式：分析组件间的通信机制（同步/异步、观察者模式、发布订阅）以及并发控制策略
- 横切关注点实现：系统如何统一处理日志、鉴权、缓存、事务管理等横切逻辑（AOP、中间件管道、装饰器）？
- 抽象与复用策略：分析基类、泛型、工具类的设计思想，系统如何通过抽象来减少重复代码并提高一致性？""",
    constraint="避免教科书式地解释设计模式定义，必须结合当前项目上下文说明其应用场景",
    output_template="templates/sections/patterns.md"
)

CLASSES_AGENT = AgentRole(
    name="classes",
    role="领域模型设计师",
    focus="""- 领域模型设计：系统的核心领域概念有哪些？它们之间的关系如何建模（聚合、实体、值对象）？
- 继承与组合策略：系统倾向于使用继承还是组合？基类/接口的设计意图是什么？
- 职责分配原则：类的职责划分遵循了什么原则？是否体现了单一职责原则？
- 类型安全与约束：系统如何利用类型系统来表达业务约束和不变量？""",
    constraint="关注建模思想而非类的属性列表，用 UML 类图辅助说明核心关系",
    output_template="templates/sections/classes.md"
)

INTERFACES_AGENT = AgentRole(
    name="interfaces",
    role="契约设计专家",
    focus="""- 抽象层次设计：系统定义了哪些核心接口/抽象类？这些抽象的设计意图和职责边界是什么？
- 契约与实现分离：接口如何隔离契约与实现？多态机制如何被运用？
- 扩展点设计：系统预留了哪些扩展点？如何在不修改核心代码的情况下扩展功能？
- 版本演进策略：接口如何支持版本演进？向后兼容性如何保障？""",
    constraint="关注接口的设计哲学，不要逐个列举接口方法签名",
    output_template="templates/sections/interfaces.md"
)

STATE_AGENT = AgentRole(
    name="state",
    role="状态管理架构师",
    focus="""- 状态模型设计：系统需要管理哪些类型的状态（会话状态、应用状态、领域状态）？状态的存储位置和作用域是什么？
- 状态生命周期：状态如何创建、更新、销毁？生命周期管理的机制是什么？
- 并发与一致性：多线程/多实例场景下，状态如何保持一致？采用了何种并发控制策略？
- 状态恢复与容错：系统如何处理状态丢失或损坏？是否有状态恢复机制？""",
    constraint="关注状态管理的设计决策，不要列举具体的变量名",
    output_template="templates/sections/state.md"
)

# 方法分析角色
ALGORITHMS_AGENT = AgentRole(
    name="algorithms",
    role="算法架构师",
    focus="""- 算法选型与权衡：系统的核心业务逻辑采用了哪些关键算法？选择这些算法的考量因素是什么（时间复杂度、空间复杂度、可维护性）？
- 计算模型设计：复杂计算如何被分解和组织？是否采用了流水线、Map-Reduce 等计算模式？
- 性能与可扩展性：算法设计如何考虑性能和可扩展性？是否有针对大数据量的优化策略？
- 正确性保障：关键算法的正确性如何保障？是否有边界条件的特殊处理？""",
    constraint="关注算法思想而非具体实现代码，用流程图辅助说明复杂逻辑",
    output_template="templates/sections/algorithms.md"
)

PATHS_AGENT = AgentRole(
    name="paths",
    role="性能架构师",
    focus="""- 关键业务路径：系统中最重要的几条业务执行路径是什么？这些路径的设计目标和约束是什么？
- 性能敏感区域：哪些环节是性能敏感的？系统采用了何种优化策略（缓存、异步、批处理）？
- 瓶颈识别与缓解：潜在的性能瓶颈在哪里？设计中是否预留了扩展空间？
- 降级与熔断：在高负载或故障场景下，系统如何保护关键路径？""",
    constraint="关注路径设计的战略考量，不要罗列所有代码执行步骤",
    output_template="templates/sections/paths.md"
)

APIS_AGENT = AgentRole(
    name="apis",
    role="API 设计规范专家",
    focus="""- API 设计风格：系统采用了何种 API 设计风格（RESTful、GraphQL、RPC）？选择该风格的原因是什么？
- 命名与结构规范：API 的命名、路径结构、参数设计遵循了什么规范？是否有一致性保障机制？
- 版本管理策略：API 如何支持版本演进？向后兼容性策略是什么？
- 错误处理规范：API 错误响应的设计规范是什么？错误码体系如何组织？""",
    constraint="关注设计规范和一致性，不要逐个列举所有 API 端点",
    output_template="templates/sections/apis.md"
)

LOGIC_AGENT = AgentRole(
    name="logic",
    role="业务逻辑架构师",
    focus="""- 业务规则建模：核心业务规则如何被表达和组织？是否采用了规则引擎或策略模式？
- 决策点设计：系统中的关键决策点有哪些？决策逻辑如何被封装和测试？
- 边界条件处理：系统如何处理边界条件和异常情况？是否有防御性编程措施？
- 业务流程编排：复杂业务流程如何被编排？是否采用了工作流引擎或状态机？""",
    constraint="关注业务逻辑的组织方式，不要逐行解释代码逻辑",
    output_template="templates/sections/logic.md"
)

# Agent角色注册表
AGENT_ROLES = {
    # Architecture Report
    "overview": OVERVIEW_AGENT,
    "layers": LAYERS_AGENT,
    "dependencies": DEPENDENCIES_AGENT,
    "dataflow": DATAFLOW_AGENT,
    "entrypoints": ENTRYPOINTS_AGENT,
    
    # Design Report
    "patterns": PATTERNS_AGENT,
    "classes": CLASSES_AGENT,
    "interfaces": INTERFACES_AGENT,
    "state": STATE_AGENT,
    
    # Methods Report
    "algorithms": ALGORITHMS_AGENT,
    "paths": PATHS_AGENT,
    "apis": APIS_AGENT,
    "logic": LOGIC_AGENT
}

# 报告类型到Agent的映射
REPORT_TYPE_AGENTS = {
    "architecture": ["overview", "layers", "dependencies", "dataflow", "entrypoints"],
    "design": ["patterns", "classes", "interfaces", "state"],
    "methods": ["algorithms", "paths", "apis", "logic"],
    "comprehensive": list(AGENT_ROLES.keys())
}
```

#### 1.3 实现具体Agent示例（Week 2）

**示例：OverviewAgent**

```python
# src/agents/roles/architecture/overview_agent.py
from typing import Optional
from pathlib import Path

from ...base.agent import BaseAgent, AgentContext, AgentResult
from ...config.agent_configs import OVERVIEW_AGENT

class OverviewAgent(BaseAgent):
    """总体架构分析Agent"""
    
    def __init__(self, llm_client):
        super().__init__(OVERVIEW_AGENT, llm_client)
    
    def analyze(self, context: AgentContext) -> AgentResult:
        """执行架构概览分析"""
        try:
            # 1. 收集项目信息
            project_info = self._collect_project_info(context.graph)
            
            # 2. 获取关键类代码示例
            code_samples = self._get_key_code_samples(context.graph)
            
            # 3. 构建提示词
            prompt = self._build_analysis_prompt(project_info, code_samples, context)
            
            # 4. 调用LLM生成报告
            output_content = self.llm.chat_sync(
                system_prompt=f"你是一个{self.role.role}，擅长分析代码结构和设计理念。",
                user_prompt=prompt,
                max_tokens=3000,
                temperature=0.3
            )
            
            # 5. 保存输出文件
            output_file = context.get_output_file(self.name)
            self._save_output(output_content, output_file)
            
            # 6. 提取跨模块备注
            cross_notes = self._extract_cross_module_notes(output_content)
            
            return AgentResult(
                agent_name=self.name,
                status="completed",
                output_file=str(output_file),
                summary="分析了项目的整体架构和技术决策",
                cross_module_notes=cross_notes,
                stats={
                    "entities_analyzed": len(project_info.get("entities", [])),
                    "diagrams": output_content.count("```mermaid")
                }
            )
            
        except Exception as e:
            return AgentResult(
                agent_name=self.name,
                status="failed",
                output_file=None,
                summary="",
                error=str(e)
            )
    
    def _get_task_description(self, context: AgentContext) -> str:
        return """基于代码库的全貌，撰写《系统架构设计报告》的"总体架构"章节。
透过代码表象，洞察系统的核心价值主张和顶层技术决策。"""
    
    def _collect_project_info(self, graph) -> dict:
        """收集项目信息"""
        entities_summary = {}
        for entity in graph.entities.values():
            entity_type = str(entity.entity_type.value)
            entities_summary[entity_type] = entities_summary.get(entity_type, 0) + 1
        
        main_classes = [
            e for e in graph.entities.values() 
            if e.entity_type.value == 'class'
        ][:10]
        
        return {
            "total_entities": len(graph.entities),
            "total_relationships": len(graph.relationships),
            "file_count": len(set(e.file_path for e in graph.entities.values())),
            "entities_summary": entities_summary,
            "main_classes": main_classes
        }
    
    def _get_key_code_samples(self, graph) -> str:
        """获取关键代码示例"""
        classes = [
            e for e in graph.entities.values() 
            if e.entity_type.value == 'class'
        ][:3]
        
        samples = []
        for cls in classes:
            if hasattr(cls, 'content') and cls.content:
                sample = f"### {cls.name}\n```java\n{cls.content[:1000]}\n```\n"
                samples.append(sample)
        
        return "\n".join(samples)
    
    def _build_analysis_prompt(self, project_info: dict, code_samples: str, context: AgentContext) -> str:
        """构建分析提示词"""
        import json
        
        return f"""请分析以下代码项目，生成一份详细的项目概述文档。

## 项目统计
- 实体总数: {project_info['total_entities']}
- 关系总数: {project_info['total_relationships']}
- 文件数量: {project_info['file_count']}

## 实体类型分布
{json.dumps(project_info['entities_summary'], ensure_ascii=False, indent=2)}

## 主要类（{len(project_info['main_classes'])}个）
{self._format_entities(project_info['main_classes'])}

## 关键类代码示例
{code_samples}

请生成以下内容（使用Markdown格式）：
1. **项目定位** - 这个项目是做什么的？解决什么问题？
2. **技术栈** - 使用了哪些技术和框架？
3. **核心功能** - 主要功能模块有哪些？每个模块的职责是什么？
4. **设计理念** - 从代码结构推断设计思路和架构模式
5. **适用场景** - 适合什么场景使用？
6. **关键API** - 列出主要的对外服务接口

请用中文回答，内容要专业、详细、结构化。采用段落式写作，禁止清单罗列。"""
    
    def _format_entities(self, entities: list) -> str:
        """格式化实体列表"""
        lines = []
        for i, entity in enumerate(entities, 1):
            lines.append(f"{i}. **{entity.name}** - {entity.file_path}:{entity.start_line}")
        return "\n".join(lines)
    
    def _extract_cross_module_notes(self, content: str) -> list:
        """提取跨模块备注"""
        # 简单实现：提取包含"模块"关键词的句子
        notes = []
        lines = content.split('\n')
        for line in lines:
            if '模块' in line and len(line) > 20:
                notes.append(line.strip())
        return notes[:5]  # 最多返回5条
```

### 验收标准

#### Phase 1 完成标准（Week 2结束）

✅ **代码质量**
- [ ] 所有Agent基类和核心类实现完成
- [ ] 至少实现3个专业Agent（overview, layers, dependencies）
- [ ] AgentOrchestrator支持并行执行
- [ ] 单元测试覆盖率 >= 80%

✅ **功能验证**
- [ ] 能够成功注册和调度Agent
- [ ] 并行执行3个Agent并正确收集结果
- [ ] Agent能够生成符合Skill格式的Markdown文件
- [ ] 错误处理机制完善

✅ **性能指标**
- [ ] 并行执行3个Agent耗时 < 单个Agent执行时间 × 2
- [ ] 内存使用合理（无内存泄漏）

✅ **文档完整**
- [ ] Agent框架API文档
- [ ] 如何实现新Agent的教程
- [ ] Demo示例代码

---

## 🎯 Phase 2: 多阶段工作流（Week 3-5）

### 目标
实现完整的5阶段工作流，从需求发现到报告生成。

### 核心交付物

#### 2.1 Phase 1: 需求发现（Week 3）

**文件结构：**
```
src/workflow/
├── __init__.py
├── phases/
│   ├── __init__.py
│   ├── base_phase.py
│   ├── phase1_requirements.py
│   ├── phase2_exploration.py
│   ├── phase3_analysis.py
│   ├── phase35_consolidation.py
│   ├── phase4_generation.py
│   └── phase5_refinement.py
├── workflow_engine.py         # 工作流引擎
└── config.py                  # 工作流配置
```

**Phase 1实现：**
```python
# src/workflow/phases/phase1_requirements.py
from typing import Dict, Any
from pathlib import Path
import questionary

from .base_phase import BasePhase, PhaseResult
from ..config import AnalysisConfig

class RequirementsDiscoveryPhase(BasePhase):
    """Phase 1: 需求发现"""
    
    def __init__(self):
        super().__init__("Phase 1: Requirements Discovery")
    
    def execute(self) -> PhaseResult:
        """执行需求发现阶段"""
        self.logger.info("开始需求发现阶段...")
        
        # 1. 报告类型选择
        report_type = self._ask_report_type()
        
        # 2. 深度级别选择
        depth_level = self._ask_depth_level()
        
        # 3. 分析范围选择
        scope = self._ask_scope()
        
        # 4. 构建配置
        config = AnalysisConfig(
            type=report_type,
            depth=depth_level,
            scope=scope,
            focus_areas=self._get_focus_areas(report_type)
        )
        
        # 5. 保存配置
        self._save_config(config)
        
        return PhaseResult(
            phase_name=self.name,
            status="completed",
            data={"config": config}
        )
    
    def _ask_report_type(self) -> str:
        """询问报告类型"""
        return questionary.select(
            "What type of project analysis report would you like?",
            choices=[
                questionary.Choice(
                    "Architecture (Recommended)",
                    value="architecture",
                    description="System structure, module relationships, layer analysis, dependency graph"
                ),
                questionary.Choice(
                    "Design",
                    value="design",
                    description="Design patterns, class relationships, component interactions"
                ),
                questionary.Choice(
                    "Methods",
                    value="methods",
                    description="Key algorithms, critical code paths, core function explanations"
                ),
                questionary.Choice(
                    "Comprehensive",
                    value="comprehensive",
                    description="All above combined into a complete project analysis"
                )
            ]
        ).ask()
    
    def _ask_depth_level(self) -> str:
        """询问分析深度"""
        return questionary.select(
            "What depth level do you need?",
            choices=[
                questionary.Choice(
                    "Overview",
                    value="overview",
                    description="High-level understanding, suitable for onboarding"
                ),
                questionary.Choice(
                    "Detailed",
                    value="detailed",
                    description="In-depth analysis with code examples"
                ),
                questionary.Choice(
                    "Deep-Dive",
                    value="deep-dive",
                    description="Exhaustive analysis with implementation details"
                )
            ]
        ).ask()
    
    def _ask_scope(self) -> str:
        """询问分析范围"""
        scope_type = questionary.select(
            "What scope should the analysis cover?",
            choices=[
                questionary.Choice("Full Project", value="full"),
                questionary.Choice("Specific Module", value="module"),
                questionary.Choice("Custom Path", value="custom")
            ]
        ).ask()
        
        if scope_type == "full":
            return "**/*"
        elif scope_type == "module":
            module = questionary.text("Enter module path:").ask()
            return f"{module}/**/*"
        else:
            return questionary.text("Enter custom path pattern:").ask()
    
    def _get_focus_areas(self, report_type: str) -> list:
        """获取关注领域"""
        focus_map = {
            "architecture": [
                "Layer Structure",
                "Module Dependencies",
                "Entry Points",
                "Data Flow"
            ],
            "design": [
                "Design Patterns",
                "Class Relationships",
                "Interface Contracts",
                "State Management"
            ],
            "methods": [
                "Core Algorithms",
                "Critical Paths",
                "Public APIs",
                "Complex Logic"
            ],
            "comprehensive": [
                "All above combined"
            ]
        }
        return focus_map.get(report_type, [])
    
    def _save_config(self, config: AnalysisConfig):
        """保存配置"""
        import json
        config_file = self.output_dir / "analysis-config.json"
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(config.to_dict(), f, ensure_ascii=False, indent=2)
        self.logger.info(f"配置已保存到: {config_file}")
```

#### 2.2 Phase 2: 项目探索（Week 3）

```python
# src/workflow/phases/phase2_exploration.py
from typing import List, Dict, Any
from pathlib import Path
import json

from .base_phase import BasePhase, PhaseResult
from ..config import AnalysisConfig

class ProjectExplorationPhase(BasePhase):
    """Phase 2: 项目探索"""
    
    def __init__(self, knowledge_graph):
        super().__init__("Phase 2: Project Exploration")
        self.graph = knowledge_graph
    
    def execute(self, config: AnalysisConfig) -> PhaseResult:
        """执行项目探索阶段"""
        self.logger.info("开始项目探索阶段...")
        
        # 根据报告类型确定探索角度
        exploration_angles = self._get_exploration_angles(config.type)
        
        # 执行探索
        exploration_results = {}
        for angle in exploration_angles:
            result = self._explore_angle(angle, config)
            exploration_results[angle] = result
        
        # 保存探索结果
        self._save_exploration_results(exploration_results)
        
        return PhaseResult(
            phase_name=self.name,
            status="completed",
            data={"exploration_results": exploration_results}
        )
    
    def _get_exploration_angles(self, report_type: str) -> List[str]:
        """获取探索角度"""
        angle_map = {
            "architecture": [
                "layer-structure",
                "module-dependencies",
                "entry-points",
                "data-flow"
            ],
            "design": [
                "design-patterns",
                "class-relationships",
                "interface-contracts",
                "state-management"
            ],
            "methods": [
                "core-algorithms",
                "critical-paths",
                "public-apis",
                "complex-logic"
            ],
            "comprehensive": [
                "architecture",
                "patterns",
                "dependencies",
                "integration-points"
            ]
        }
        return angle_map.get(report_type, [])
    
    def _explore_angle(self, angle: str, config: AnalysisConfig) -> Dict[str, Any]:
        """探索特定角度"""
        self.logger.info(f"探索角度: {angle}")
        
        # 根据角度收集相关信息
        if angle == "layer-structure":
            return self._explore_layers()
        elif angle == "module-dependencies":
            return self._explore_dependencies()
        elif angle == "entry-points":
            return self._explore_entry_points()
        # ... 其他角度
        
        return {}
    
    def _explore_layers(self) -> Dict[str, Any]:
        """探索层级结构"""
        # 分析包结构，识别分层
        packages = {}
        for entity in self.graph.entities.values():
            package = self._extract_package(entity.file_path)
            if package not in packages:
                packages[package] = {
                    "classes": [],
                    "functions": [],
                    "imports": []
                }
            
            if entity.entity_type.value == 'class':
                packages[package]["classes"].append(entity.name)
            elif entity.entity_type.value in ['function', 'method']:
                packages[package]["functions"].append(entity.name)
        
        return {
            "packages": packages,
            "layer_hints": self._infer_layers(packages)
        }
    
    def _explore_dependencies(self) -> Dict[str, Any]:
        """探索依赖关系"""
        dependencies = {
            "external": [],
            "internal": []
        }
        
        for rel in self.graph.relationships:
            if rel.relationship_type.value == 'imports':
                dependencies["external"].append({
                    "source": rel.source_id,
                    "target": rel.target_id
                })
            elif rel.relationship_type.value == 'calls':
                dependencies["internal"].append({
                    "source": rel.source_id,
                    "target": rel.target_id
                })
        
        return dependencies
    
    def _explore_entry_points(self) -> Dict[str, Any]:
        """探索入口点"""
        entry_points = []
        
        # 查找主类、主方法
        for entity in self.graph.entities.values():
            if entity.name in ['main', '__main__', 'Main', 'Application']:
                entry_points.append({
                    "type": "main",
                    "name": entity.name,
                    "file": entity.file_path,
                    "line": entity.start_line
                })
        
        # 查找公开API
        for entity in self.graph.entities.values():
            if entity.entity_type.value == 'method' and entity.name.startswith('get_'):
                entry_points.append({
                    "type": "api",
                    "name": entity.name,
                    "file": entity.file_path,
                    "line": entity.start_line
                })
        
        return {"entry_points": entry_points}
    
    def _save_exploration_results(self, results: Dict[str, Any]):
        """保存探索结果"""
        for angle, result in results.items():
            file_path = self.output_dir / f"exploration-{angle}.json"
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(result, f, ensure_ascii=False, indent=2)
            self.logger.info(f"探索结果已保存: {file_path}")
    
    def _extract_package(self, file_path: str) -> str:
        """提取包路径"""
        parts = Path(file_path).parts
        if len(parts) > 2:
            return "/".join(parts[:3])
        return "/".join(parts[:-1])
    
    def _infer_layers(self, packages: Dict) -> List[Dict]:
        """推断分层"""
        layers = []
        layer_keywords = {
            "controller": "表现层",
            "service": "业务层",
            "repository": "数据层",
            "model": "领域层",
            "util": "工具层"
        }
        
        for package, info in packages.items():
            for keyword, layer_name in layer_keywords.items():
                if keyword in package.lower():
                    layers.append({
                        "package": package,
                        "layer": layer_name,
                        "classes": len(info["classes"])
                    })
        
        return layers
```

#### 2.3 Phase 3: 深度分析（Week 3-4）

```python
# src/workflow/phases/phase3_analysis.py
from typing import List, Dict
from pathlib import Path

from .base_phase import BasePhase, PhaseResult
from ..config import AnalysisConfig
from ...agents.orchestrator import AgentOrchestrator
from ...agents.config.agent_configs import AGENT_ROLES, REPORT_TYPE_AGENTS
from ...agents.roles.architecture import OverviewAgent, LayersAgent, DependenciesAgent
# ... 导入其他Agent

class DeepAnalysisPhase(BasePhase):
    """Phase 3: 深度分析"""
    
    def __init__(self, knowledge_graph, llm_client):
        super().__init__("Phase 3: Deep Analysis")
        self.graph = knowledge_graph
        self.llm = llm_client
        self.orchestrator = AgentOrchestrator(max_workers=5)
    
    def execute(self, config: AnalysisConfig, exploration_results: Dict) -> PhaseResult:
        """执行深度分析阶段"""
        self.logger.info("开始深度分析阶段...")
        
        # 1. 注册Agent
        self._register_agents(config.type)
        
        # 2. 准备上下文
        context = self._prepare_context(config, exploration_results)
        
        # 3. 获取需要执行的Agent列表
        agent_names = REPORT_TYPE_AGENTS[config.type]
        
        # 4. 并行执行Agent
        results = self.orchestrator.run_parallel(agent_names, context)
        
        # 5. 收集结果摘要
        agent_summaries = {
            name: result.to_json() 
            for name, result in results.items()
        }
        
        # 6. 提取跨模块备注
        cross_notes = []
        for result in results.values():
            cross_notes.extend(result.cross_module_notes)
        
        return PhaseResult(
            phase_name=self.name,
            status="completed",
            data={
                "agent_summaries": agent_summaries,
                "cross_module_notes": cross_notes
            }
        )
    
    def _register_agents(self, report_type: str):
        """注册Agent"""
        # 创建Agent实例
        agents = {
            "overview": OverviewAgent(self.llm),
            "layers": LayersAgent(self.llm),
            "dependencies": DependenciesAgent(self.llm),
            # ... 其他Agent
        }
        
        # 注册到编排器
        for name, agent in agents.items():
            self.orchestrator.register_agent(name, agent)
    
    def _prepare_context(self, config: AnalysisConfig, exploration_results: Dict) -> AgentContext:
        """准备Agent上下文"""
        from ...agents.base.agent import AgentContext
        
        # 准备探索数据映射
        exploration_data = {}
        for angle, result in exploration_results.items():
            file_path = self.output_dir / f"exploration-{angle}.json"
            exploration_data[angle] = file_path
        
        return AgentContext(
            graph=self.graph,
            config=config,
            output_dir=self.output_dir,
            exploration_data=exploration_data
        )
```

#### 2.4 Phase 3.5: 汇总整合（Week 4）

```python
# src/workflow/phases/phase35_consolidation.py
from typing import List, Dict
from pathlib import Path
import json

from .base_phase import BasePhase, PhaseResult

class ConsolidationPhase(BasePhase):
    """Phase 3.5: 汇总整合"""
    
    def __init__(self, llm_client):
        super().__init__("Phase 3.5: Consolidation")
        self.llm = llm_client
    
    def execute(
        self,
        agent_summaries: Dict,
        cross_module_notes: List[str]
    ) -> PhaseResult:
        """执行汇总整合阶段"""
        self.logger.info("开始汇总整合阶段...")
        
        # 1. 读取所有章节文件
        sections = self._read_sections(agent_summaries)
        
        # 2. 生成综合分析
        synthesis = self._generate_synthesis(sections, cross_module_notes)
        
        # 3. 提取章节摘要
        section_summaries = self._extract_section_summaries(agent_summaries)
        
        # 4. 跨章节关联分析
        cross_analysis = self._analyze_cross_references(sections)
        
        # 5. 质量检查
        quality_score = self._quality_check(sections, agent_summaries)
        
        # 6. 问题检测
        issues = self._detect_issues(sections, quality_score)
        
        # 7. 保存汇总报告
        consolidation_file = self._save_consolidation_report(
            synthesis,
            section_summaries,
            cross_analysis,
            quality_score,
            issues
        )
        
        return PhaseResult(
            phase_name=self.name,
            status="completed",
            data={
                "synthesis": synthesis,
                "section_summaries": section_summaries,
                "cross_analysis": cross_analysis,
                "quality_score": quality_score,
                "issues": issues,
                "consolidation_file": str(consolidation_file)
            }
        )
    
    def _read_sections(self, agent_summaries: Dict) -> List[Dict]:
        """读取章节文件"""
        sections = []
        for agent_name, summary in agent_summaries.items():
            if summary.get("output_file"):
                file_path = self.output_dir / "sections" / summary["output_file"]
                if file_path.exists():
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    sections.append({
                        "agent": agent_name,
                        "file": str(file_path),
                        "content": content,
                        "summary": summary.get("summary", "")
                    })
        return sections
    
    def _generate_synthesis(self, sections: List[Dict], cross_notes: List[str]) -> str:
        """生成综合分析"""
        # 构建提示词
        prompt = f"""请阅读以下分析章节，生成2-3段的项目全貌描述：

章节数量: {len(sections)}

跨模块备注:
{chr(10).join(cross_notes[:10])}

请生成：
1. 第一段：项目定位与核心架构特征
2. 第二段：关键设计决策与技术选型
3. 第三段：整体质量评价与显著特点

要求：
- 段落式写作，逻辑连贯
- 客观第三人称视角
- 突出核心价值
"""
        
        # 调用LLM生成
        synthesis = self.llm.chat_sync(
            system_prompt="你是一个资深的软件架构师，擅长项目综合分析。",
            user_prompt=prompt,
            max_tokens=1000,
            temperature=0.3
        )
        
        return synthesis
    
    def _extract_section_summaries(self, agent_summaries: Dict) -> List[Dict]:
        """提取章节摘要"""
        summaries = []
        for agent_name, summary in agent_summaries.items():
            summaries.append({
                "file": f"section-{agent_name}.md",
                "title": self._get_section_title(agent_name),
                "summary": summary.get("summary", "")
            })
        return summaries
    
    def _get_section_title(self, agent_name: str) -> str:
        """获取章节标题"""
        title_map = {
            "overview": "系统概述",
            "layers": "层次分析",
            "dependencies": "依赖管理",
            "dataflow": "数据流转",
            "entrypoints": "系统入口",
            "patterns": "设计模式",
            "classes": "类型体系",
            "interfaces": "接口契约",
            "state": "状态管理",
            "algorithms": "核心算法",
            "paths": "关键路径",
            "apis": "API设计",
            "logic": "业务逻辑"
        }
        return title_map.get(agent_name, agent_name)
    
    def _analyze_cross_references(self, sections: List[Dict]) -> str:
        """跨章节关联分析"""
        # 构建提示词
        prompt = f"""请分析以下章节间的关联关系：

章节数: {len(sections)}

请描述：
1. 模块间的依赖关系如何体现在各章节
2. 设计决策如何贯穿多个层面
3. 潜在的一致性或冲突

要求段落式描述。
"""
        
        cross_analysis = self.llm.chat_sync(
            system_prompt="你是一个架构分析专家。",
            user_prompt=prompt,
            max_tokens=800,
            temperature=0.3
        )
        
        return cross_analysis
    
    def _quality_check(self, sections: List[Dict], agent_summaries: Dict) -> Dict:
        """质量检查"""
        scores = {
            "completeness": 0,
            "consistency": 0,
            "depth": 0,
            "readability": 0
        }
        
        # 完整性检查
        expected_sections = len(agent_summaries)
        actual_sections = len(sections)
        scores["completeness"] = int(actual_sections / expected_sections * 100) if expected_sections > 0 else 0
        
        # 一致性检查
        consistency_issues = self._check_consistency(sections)
        scores["consistency"] = max(0, 100 - len(consistency_issues) * 10)
        
        # 深度检查
        avg_length = sum(len(s["content"]) for s in sections) / len(sections) if sections else 0
        scores["depth"] = min(100, int(avg_length / 50))  # 假设平均5000字符为满分
        
        # 可读性检查
        readability_score = self._check_readability(sections)
        scores["readability"] = readability_score
        
        overall = sum(scores.values()) / len(scores)
        scores["overall"] = int(overall)
        
        return scores
    
    def _check_consistency(self, sections: List[Dict]) -> List[str]:
        """一致性检查"""
        issues = []
        
        # 检查术语一致性
        # 简化实现：检查是否有明显的不一致
        all_content = "\n".join(s["content"] for s in sections)
        
        # TODO: 实现更复杂的一致性检查
        
        return issues
    
    def _check_readability(self, sections: List[Dict]) -> int:
        """可读性检查"""
        score = 100
        
        for section in sections:
            content = section["content"]
            
            # 检查是否过度使用清单
            list_count = content.count('\n- ') + content.count('\n* ')
            if list_count > 10:
                score -= 10
            
            # 检查是否有过多的表格
            table_count = content.count('|---')
            if table_count > 5:
                score -= 5
        
        return max(0, score)
    
    def _detect_issues(self, sections: List[Dict], quality_score: Dict) -> Dict:
        """检测问题"""
        issues = {
            "errors": [],
            "warnings": [],
            "info": []
        }
        
        # 检查完整性
        if quality_score["completeness"] < 80:
            issues["warnings"].append({
                "id": "W001",
                "type": "incompleteness",
                "message": f"章节完整性不足：{quality_score['completeness']}%"
            })
        
        # 检查一致性
        if quality_score["consistency"] < 80:
            issues["warnings"].append({
                "id": "W002",
                "type": "inconsistency",
                "message": "发现潜在的一致性问题"
            })
        
        # 检查可读性
        if quality_score["readability"] < 80:
            issues["info"].append({
                "id": "I001",
                "type": "readability",
                "message": "可读性可以改进"
            })
        
        return issues
    
    def _save_consolidation_report(
        self,
        synthesis: str,
        section_summaries: List[Dict],
        cross_analysis: str,
        quality_score: Dict,
        issues: Dict
    ) -> Path:
        """保存汇总报告"""
        report = f"""# 分析汇总报告

## 综合分析

{synthesis}

## 章节摘要

| 章节 | 文件 | 核心发现 |
|------|------|----------|
"""
        for summary in section_summaries:
            report += f"| {summary['title']} | {summary['file']} | {summary['summary']} |\n"
        
        report += f"""
## 架构洞察

{cross_analysis}

---

## 质量评估

### 评分

| 维度 | 得分 | 说明 |
|------|------|------|
| 完整性 | {quality_score['completeness']}% | - |
| 一致性 | {quality_score['consistency']}% | - |
| 深度 | {quality_score['depth']}% | - |
| 可读性 | {quality_score['readability']}% | - |
| 综合 | {quality_score['overall']}% | - |

### 发现的问题

"""
        if issues["errors"]:
            report += "#### 严重问题\n"
            for issue in issues["errors"]:
                report += f"| {issue['id']} | {issue['type']} | {issue['message']} |\n"
            report += "\n"
        
        if issues["warnings"]:
            report += "#### 警告\n"
            for issue in issues["warnings"]:
                report += f"| {issue['id']} | {issue['type']} | {issue['message']} |\n"
            report += "\n"
        
        if issues["info"]:
            report += "#### 提示\n"
            for issue in issues["info"]:
                report += f"| {issue['id']} | {issue['type']} | {issue['message']} |\n"
        
        file_path = self.output_dir / "consolidation-summary.md"
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(report)
        
        return file_path
```

#### 2.5 Phase 4: 报告生成（Week 4）

#### 2.6 Phase 5: 迭代优化（Week 5）

### 验收标准

#### Phase 2 完成标准（Week 5结束）

✅ **功能完整性**
- [ ] 5个阶段全部实现并可运行
- [ ] 用户可以通过CLI交互完成需求发现
- [ ] 工作流可以完整执行并生成报告

✅ **集成测试**
- [ ] 端到端测试通过
- [ ] 使用示例项目测试各种报告类型
- [ ] 错误处理完善

---

## 🎯 Phase 3: 输出质量优化（Week 6-7）

### 目标
提升输出文档质量，符合专业写作规范。

### 核心交付物

#### 3.1 段落式写作引擎
#### 3.2 写作风格验证器
#### 3.3 Mermaid图表验证

### 验收标准

#### Phase 3 完成标准（Week 7结束）

✅ **质量提升**
- [ ] 输出文档符合段落式写作规范
- [ ] Mermaid图表渲染成功率 >= 95%
- [ ] 写作风格验证通过率 >= 90%

---

## 🎯 Phase 4: 质量控制增强（Week 8-9）

### 目标
建立完善的质量保证体系。

### 核心交付物

#### 4.1 跨章节一致性检查
#### 4.2 质量问题跟踪系统
#### 4.3 迭代优化机制

### 验收标准

#### Phase 4 完成标准（Week 9结束）

✅ **质量保证**
- [ ] 跨章节一致性检查实现
- [ ] 质量问题跟踪系统可用
- [ ] 迭代优化可以自动执行

✅ **文档完善**
- [ ] 用户手册更新
- [ ] 开发文档完善
- [ ] Demo视频制作

---

## 📊 进度跟踪

### 周报模板

```markdown
# Week X 进度报告

## 本周完成
- [ ] 任务1
- [ ] 任务2

## 遇到的问题
- 问题1：描述和解决方案

## 下周计划
- [ ] 任务1
- [ ] 任务2

## 风险评估
- 风险1：描述和缓解措施
```

### 里程碑验收

每个Phase结束时需要：
1. 代码审查
2. 功能测试
3. 文档检查
4. 性能测试
5. 用户验收测试

---

## 🎓 培训材料

### Agent框架培训
- Agent基础概念
- 如何实现新Agent
- Agent协作机制
- 最佳实践

### 工作流培训
- 工作流引擎原理
- 如何扩展新阶段
- 错误处理策略
- 性能优化技巧

---

## 📝 总结

本执行计划详细规划了9周的实施路线，通过渐进式重构的方式，将知识提取能力提升到顶级智能体水平。关键成功因素：

1. **严格按计划执行**
2. **每个阶段充分测试**
3. **及时调整优化**
4. **保持代码质量**

**下一步：开始Phase 1实施！**
