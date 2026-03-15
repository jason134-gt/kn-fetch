"""
Agent配置模块

定义所有专业Agent的角色配置
"""
from ..base.agent import AgentRole


# ============================================================================
# 基础设施角色 (Infrastructure Agents)
# ============================================================================

REPOSITORY_ACCESS_AGENT = AgentRole(
    name="repository_access",
    role="代码仓库接入专家",
    focus="""- 仓库元数据提取：分析Git仓库的结构、分支、提交历史、作者信息等元数据
- 变更检测与增量扫描：识别自上次扫描以来的代码变更，支持增量分析
- 多分支支持：支持分析多个分支，比较不同分支间的代码差异
- 大仓库优化：针对大型代码仓库的优化策略，避免内存溢出和性能问题""",
    constraint="专注于仓库层面的元数据提取，不涉及具体代码内容分析",
    output_template="templates/sections/repository_access.md"
)

CODE_PARSER_AGENT = AgentRole(
    name="code_parser",
    role="代码解析与预处理专家",
    focus="""- 多语言AST解析：支持Java、Python、JavaScript、Go等主流编程语言的抽象语法树解析
- 代码结构提取：提取类、函数、方法、接口、枚举等代码结构元素
- 复杂度分析：计算代码的圈复杂度、认知复杂度、代码行数等指标
- 死代码识别：识别未被引用的函数、类、变量等潜在死代码
- 代码质量评估：基于代码结构分析代码质量和可维护性""",
    constraint="专注于代码语法层面的解析，不涉及业务语义理解",
    output_template="templates/sections/code_parser.md"
)

SEMANTIC_UNDERSTANDING_AGENT = AgentRole(
    name="semantic_understanding",
    role="语义理解与业务抽象专家",
    focus="""- 业务语义识别：从代码中识别业务实体、业务操作和业务规则
- 语义关系分析：分析代码元素之间的语义关系和依赖关系
- 业务抽象建模：构建业务语义契约，描述系统的业务逻辑和领域模型
- LLM增强分析：使用大语言模型进行深度语义理解和意图识别
- 语义向量化：将代码语义特征转换为向量表示，支持语义检索""",
    constraint="专注于业务语义层面的理解，不涉及具体代码语法细节",
    output_template="templates/sections/semantic_understanding.md"
)

DEPENDENCY_ANALYSIS_AGENT = AgentRole(
    name="dependency_analysis",
    role="依赖分析与架构逆向专家",
    focus="""- 深度依赖关系分析：构建完整的模块依赖图谱，识别循环依赖和高风险依赖
- 架构逆向工程：通过依赖关系还原系统架构，识别架构聚类和边界
- 依赖可视化：生成依赖关系图和架构图，支持架构理解和优化
- 架构风险评估：识别依赖相关的架构风险和优化机会""",
    constraint="专注于依赖关系的技术分析和架构层面，不涉及具体业务逻辑",
    output_template="templates/sections/dependency_analysis.md"
)

RISK_ASSESSMENT_AGENT = AgentRole(
    name="risk_assessment",
    role="风险评估和可视化专家",
    focus="""- 架构风险评估：综合分析代码质量、复杂度、依赖关系等风险因素
- 技术债务识别：识别技术债务和架构缺陷，评估其对项目的影响
- 可视化报告生成：生成风险雷达图、趋势分析、缓解路线图等可视化报告
- 风险缓解建议：提供具体的风险缓解策略和优化建议""",
    constraint="专注于风险评估和可视化分析，不涉及具体的技术实现细节",
    output_template="templates/sections/risk_assessment.md"
)


# ============================================================================
# 架构分析角色 (Architecture Report)
# ============================================================================

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


# ============================================================================
# 设计分析角色 (Design Report)
# ============================================================================

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


# ============================================================================
# 方法分析角色 (Methods Report)
# ============================================================================

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


# ============================================================================
# Agent角色注册表
# ============================================================================

AGENT_ROLES = {
    # Infrastructure Agents
    "repository_access": REPOSITORY_ACCESS_AGENT,
    "code_parser": CODE_PARSER_AGENT,
    "semantic_understanding": SEMANTIC_UNDERSTANDING_AGENT,
    "dependency_analysis": DEPENDENCY_ANALYSIS_AGENT,
    "risk_assessment": RISK_ASSESSMENT_AGENT,
    
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


# ============================================================================
# 报告类型到Agent的映射
# ============================================================================

REPORT_TYPE_AGENTS = {
    "architecture": ["overview", "layers", "dependencies", "dataflow", "entrypoints"],
    "design": ["patterns", "classes", "interfaces", "state"],
    "methods": ["algorithms", "paths", "apis", "logic"],
    "comprehensive": list(AGENT_ROLES.keys())
}


# ============================================================================
# 探索角度到Agent的映射
# ============================================================================

EXPLORATION_TO_AGENT = {
    # Architecture Report 角度
    'layer-structure': 'layers',
    'module-dependencies': 'dependencies',
    'entry-points': 'entrypoints',
    'data-flow': 'dataflow',
    
    # Design Report 角度
    'design-patterns': 'patterns',
    'class-relationships': 'classes',
    'interface-contracts': 'interfaces',
    'state-management': 'state',
    
    # Methods Report 角度
    'core-algorithms': 'algorithms',
    'critical-paths': 'paths',
    'public-apis': 'apis',
    'complex-logic': 'logic',
    
    # Comprehensive 角度
    'architecture': 'overview',
    'patterns': 'patterns',
    'dependencies': 'dependencies',
    'integration-points': 'entrypoints'
}


def get_agent_role(agent_name: str) -> AgentRole:
    """
    获取Agent角色配置
    
    Args:
        agent_name: Agent名称
        
    Returns:
        AgentRole实例
        
    Raises:
        ValueError: 如果Agent名称不存在
    """
    if agent_name not in AGENT_ROLES:
        raise ValueError(f"Unknown agent: {agent_name}")
    return AGENT_ROLES[agent_name]


def get_agents_by_report_type(report_type: str) -> list:
    """
    根据报告类型获取Agent列表
    
    Args:
        report_type: 报告类型（architecture/design/methods/comprehensive）
        
    Returns:
        Agent名称列表
        
    Raises:
        ValueError: 如果报告类型不存在
    """
    if report_type not in REPORT_TYPE_AGENTS:
        raise ValueError(f"Unknown report type: {report_type}")
    return REPORT_TYPE_AGENTS[report_type]


def get_agent_for_exploration(exploration_angle: str) -> str:
    """
    根据探索角度获取对应的Agent名称
    
    Args:
        exploration_angle: 探索角度
        
    Returns:
        Agent名称，如果没有对应Agent返回None
    """
    return EXPLORATION_TO_AGENT.get(exploration_angle)


def list_all_agents() -> list:
    """
    列出所有Agent名称
    
    Returns:
        Agent名称列表
    """
    return list(AGENT_ROLES.keys())


def get_agent_info(agent_name: str) -> dict:
    """
    获取Agent详细信息
    
    Args:
        agent_name: Agent名称
        
    Returns:
        包含Agent详细信息的字典
    """
    role = get_agent_role(agent_name)
    return {
        "name": role.name,
        "role": role.role,
        "focus": role.focus,
        "constraint": role.constraint,
        "output_template": role.output_template
    }
