# 重构智能体系统详细设计文档与可执行开发任务清单
> 文档定位：AI编码工具可1:1落地执行的工程化开发规范，无模糊描述，所有模块、接口、数据结构、开发步骤均明确可编码
> 核心约束：AI编码工具必须严格遵循本文档的所有定义，禁止擅自修改接口、数据结构、核心流程，所有代码必须配套单元测试，符合编码规范

---

## 一、文档总则与核心红线规则
### 1.1 文档目的
本文档定义**代码资产扫描建模+重构智能体全系统**的工程化实现规范，指导AI编码工具完成从0到1的编码开发，最终交付可直接运行的、支持百万行级代码库的AI重构智能体系统。

### 1.2 不可突破的核心红线（AI编码工具必须100%遵守）
1.  **业务契约不变原则**：所有重构相关逻辑必须严格锁定代码的业务语义契约，禁止任何修改业务行为的逻辑实现
2.  **接口隔离原则**：所有外部依赖（LLM、数据库、Git、扫描工具）必须做抽象层封装，禁止业务代码直接耦合具体实现
3.  **可追溯原则**：所有任务执行、代码修改、LLM调用、重构动作必须留痕，生成可审计的全链路日志
4.  **渐进式原则**：重构逻辑必须拆分为原子化步骤，禁止实现大爆炸式全量重写逻辑
5.  **验证闭环原则**：所有重构代码生成后必须配套验证逻辑，无验证的重构代码禁止合并输出
6.  **代码规范原则**：所有代码必须符合本文档定义的编码规范，配套单元测试，核心模块测试覆盖率≥90%

### 1.3 目标运行环境
- 操作系统：Linux/Windows/macOS 兼容
- Python版本：Python 3.11+
- 部署模式：支持本地单机部署、Docker容器化部署
- 并发能力：支持10+并行代码扫描任务、100+并行原子重构任务

---

## 二、技术栈选型（固定不可擅自修改）
| 技术领域 | 选型固定值 | 用途说明 |
|----------|------------|----------|
| 核心开发语言 | Python 3.11+ | 全系统开发语言，AI生态最成熟 |
| Web/API框架 | FastAPI 0.109+ | 提供系统RESTful API，异步高性能 |
| LLM编排框架 | LangChain 0.2+ | 统一管理Prompt、RAG链路、Agent调用 |
| 代码解析 | Tree-sitter 0.20+、GitPython 3.1+ | 多语言AST解析、Git仓库操作与提交历史分析 |
| 向量数据库 | Milvus 2.3+ | 存储代码语义向量、业务文档向量，支撑RAG检索 |
| 图数据库 | Neo4j 5.15+ | 存储代码依赖关系、调用链路、业务-代码映射关系 |
| 关系型数据库 | PostgreSQL 15+ | 存储元数据、任务信息、配置、重构台账、验证报告 |
| ORM框架 | SQLAlchemy 2.0+ | 数据库ORM操作，类型安全 |
| 配置管理 | Pydantic Settings 2.0+ | 环境变量、配置文件统一管理 |
| 任务队列 | Celery 5.3+ + Redis 7.0+ | 并行处理代码扫描、重构任务，异步调度 |
| 日志管理 | Loguru 0.7+ | 结构化全链路日志 |
| 代码规范工具 | Ruff、Black、isort | Python代码格式化、lint校验 |
| 测试框架 | Pytest 7.4+ | 单元测试、集成测试、E2E测试 |
| 模板引擎 | Jinja2 3.1+ | LLM Prompt模板统一管理 |
| 静态扫描集成 | SonarQube API、Semgrep | 代码质量、安全漏洞扫描集成 |

---

## 三、系统整体架构与目录结构
### 3.1 分层架构定义
```
┌─────────────────────────────────────────────────────────────┐
│ 接入层：API网关、任务调度入口、Git仓库接入、Web控制台        │
├─────────────────────────────────────────────────────────────┤
│ 核心Agent层：扫描建模Agent集群 + 重构执行Agent集群          │
│ 1. 代码仓库接入Agent        2. 代码解析与预处理Agent        │
│ 3. 语义理解与业务抽象Agent  4. 依赖分析与架构逆向Agent      │
│ 5. 风险评估与优先级Agent    6. 业务-代码映射Agent           │
│ 7. 任务接收与拆解Agent      8. 上下文检索与聚合Agent        │
│ 9. 重构规划Agent            10. 代码生成Agent               │
│ 11. 验证与修复Agent         12. 合并与台账更新Agent         │
│ 13. 可视化与报告生成Agent                                    │
├─────────────────────────────────────────────────────────────┤
│ 基础设施层：LLM客户端抽象、数据库客户端、工具集、异常处理    │
├─────────────────────────────────────────────────────────────┤
│ 存储层：PostgreSQL、Milvus、Neo4j、Redis、本地文件存储      │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 固定项目目录结构（AI编码工具必须严格生成）
```
refactoring-agent-system/
├── pyproject.toml                # 项目依赖、元数据配置
├── .gitignore                     # Git忽略文件
├── ruff.toml                      # Ruff代码规范配置
├── black.toml                     # Black格式化配置
├── pytest.ini                     # 测试框架配置
├── .env.example                   # 环境变量示例文件
├── README.md                      # 项目说明文档
├── config/                        # 配置文件目录
│   ├── __init__.py
│   ├── settings.py                # 全局配置类
│   ├── llm_config.yaml            # LLM模型配置
│   └── refactoring_rules.yaml     # 重构规范、编码规则配置
├── templates/                     # Jinja2 Prompt模板目录
│   ├── semantic_understanding/    # 语义理解相关Prompt模板
│   ├── refactoring_planning/      # 重构规划相关Prompt模板
│   ├── code_generation/           # 代码生成相关Prompt模板
│   └── validation/                # 验证相关Prompt模板
├── src/                           # 核心源码目录
│   ├── __init__.py
│   ├── infrastructure/            # 基础设施层
│   │   ├── __init__.py
│   │   ├── llm/                   # LLM客户端抽象与实现
│   │   ├── database/              # 数据库客户端抽象与实现
│   │   ├── git/                   # Git操作工具封装
│   │   ├── parser/                # 代码AST解析工具封装
│   │   ├── scanner/               # 静态扫描工具封装
│   │   ├── common/                # 通用工具、异常类、常量
│   │   └── task_queue/            # 任务队列封装
│   ├── models/                    # 数据模型定义
│   │   ├── __init__.py
│   │   ├── code_metadata.py       # 代码元数据相关模型
│   │   ├── semantic_contract.py   # 业务语义契约模型
│   │   ├── refactoring_task.py    # 重构任务相关模型
│   │   ├── dependency.py          # 依赖关系模型
│   │   └── validation.py          # 验证报告模型
│   ├── agents/                    # 核心Agent实现
│   │   ├── __init__.py
│   │   ├── scan_agents/           # 扫描建模Agent集群
│   │   └── refactoring_agents/    # 重构执行Agent集群
│   ├── services/                  # 业务服务层
│   │   ├── __init__.py
│   │   ├── scan_service.py        # 扫描建模全流程服务
│   │   ├── refactoring_service.py # 重构全流程服务
│   │   └── report_service.py      # 报告生成服务
│   ├── rag/                       # RAG检索引擎实现
│   │   ├── __init__.py
│   │   ├── retriever.py           # 多模态检索器
│   │   ├── index_builder.py       # 索引构建器
│   │   └── context_assembler.py   # 上下文组装器
│   └── api/                       # API接口层
│       ├── __init__.py
│       ├── app.py                 # FastAPI应用入口
│       ├── routes/                # API路由
│       └── schemas/               # API请求/响应模型
├── tests/                         # 测试目录
│   ├── __init__.py
│   ├── unit/                      # 单元测试
│   ├── integration/               # 集成测试
│   └── e2e/                       # 端到端测试
└── docker/                        # Docker部署相关
    ├── Dockerfile
    ├── docker-compose.yml
    └── docker-compose.env
```

---

## 四、核心数据模型详细定义（AI编码工具必须严格实现）
所有模型基于`Pydantic v2`和`SQLAlchemy 2.0`实现，分为**API传输模型**、**业务域模型**、**数据库ORM模型**三类，以下为核心模型的字段定义。

### 4.1 代码元数据核心模型
```python
# src/models/code_metadata.py
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
from enum import Enum

class CodeLanguage(str, Enum):
    JAVA = "java"
    PYTHON = "python"
    GO = "go"
    JAVASCRIPT = "javascript"
    TYPESCRIPT = "typescript"
    CPP = "cpp"
    CSHARP = "csharp"

class CodeRiskLevel(str, Enum):
    P0 = "P0" # 核心业务，最高风险
    P1 = "P1" # 重要业务，高风险
    P2 = "P2" # 通用模块，中风险
    P3 = "P3" # 边缘模块，低风险

class CodeFunctionMetadata(BaseModel):
    """函数/方法元数据模型"""
    function_id: str = Field(description="全局唯一函数ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    file_id: str = Field(description="所属文件ID")
    module_id: str = Field(description="所属模块ID")
    repository_id: str = Field(description="所属仓库ID")
    function_name: str = Field(description="函数名")
    start_line: int = Field(description="起始行号")
    end_line: int = Field(description="结束行号")
    code_content: str = Field(description="函数代码原文")
    comment: Optional[str] = Field(description="函数注释", default=None)
    parameters: List[dict] = Field(description="参数列表，包含name、type、default_value")
    return_type: Optional[str] = Field(description="返回值类型", default=None)
    called_functions: List[str] = Field(description="本函数调用的其他函数ID列表", default_factory=list)
    caller_functions: List[str] = Field(description="调用本函数的其他函数ID列表", default_factory=list)
    cyclomatic_complexity: int = Field(description="圈复杂度", ge=0)
    cognitive_complexity: int = Field(description="认知复杂度", ge=0)
    is_dead_code: bool = Field(description="是否为死代码", default=False)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)

class CodeFileMetadata(BaseModel):
    """代码文件元数据模型"""
    file_id: str = Field(description="全局唯一文件ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    module_id: str = Field(description="所属模块ID")
    repository_id: str = Field(description="所属仓库ID")
    file_path: str = Field(description="文件相对仓库根目录的路径")
    file_name: str = Field(description="文件名")
    language: CodeLanguage = Field(description="代码语言")
    total_lines: int = Field(description="总行数", ge=0)
    code_lines: int = Field(description="代码行数", ge=0)
    comment_lines: int = Field(description="注释行数", ge=0)
    blank_lines: int = Field(description="空行数", ge=0)
    functions: List[CodeFunctionMetadata] = Field(description="文件内的函数列表", default_factory=list)
    classes: List[dict] = Field(description="文件内的类列表", default_factory=list)
    imports: List[str] = Field(description="导入依赖列表", default_factory=list)
    hard_coded_values: List[dict] = Field(description="硬编码值列表", default_factory=list)
    risk_level: CodeRiskLevel = Field(description="风险等级", default=CodeRiskLevel.P2)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)

class CodeRepositoryMetadata(BaseModel):
    """代码仓库元数据模型"""
    repository_id: str = Field(description="全局唯一仓库ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    repository_name: str = Field(description="仓库名")
    git_url: str = Field(description="Git仓库地址")
    default_branch: str = Field(description="默认分支", default="main")
    target_branch: str = Field(description="扫描/重构目标分支", default="main")
    local_path: str = Field(description="本地克隆路径")
    total_files: int = Field(description="总文件数", ge=0)
    total_code_lines: int = Field(description="总代码行数", ge=0)
    language_distribution: dict = Field(description="语言分布统计", default_factory=dict)
    scan_status: str = Field(description="扫描状态", default="pending")
    last_scan_time: Optional[datetime] = Field(description="最后扫描时间", default=None)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
```

### 4.2 业务语义契约核心模型
```python
# src/models/semantic_contract.py
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class BusinessSemanticContract(BaseModel):
    """业务语义契约模型，扫描建模的核心输出"""
    contract_id: str = Field(description="全局唯一契约ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    related_id: str = Field(description="关联的函数/文件/模块ID")
    related_type: str = Field(description="关联类型：function/file/module", enum=["function", "file", "module"])
    repository_id: str = Field(description="所属仓库ID")
    business_summary: str = Field(description="业务功能摘要，自然语言描述")
    input_contract: List[dict] = Field(description="输入契约，包含参数名、业务含义、约束条件", default_factory=list)
    output_contract: dict = Field(description="输出契约，包含业务含义、数据结构", default_factory=dict)
    side_effects: List[str] = Field(description="副作用列表，如写库、调用外部接口、发送消息", default_factory=list)
    business_rules: List[str] = Field(description="核心业务规则列表", default_factory=list)
    exception_scenarios: List[str] = Field(description="异常处理场景列表", default_factory=list)
    business_domain_tags: List[str] = Field(description="业务域标签列表", default_factory=list)
    semantic_vector_id: Optional[str] = Field(description="关联的语义向量ID", default=None)
    is_manual_verified: bool = Field(description="是否经过人工审核", default=False)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
```

### 4.3 重构任务核心模型
```python
# src/models/refactoring_task.py
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
from enum import Enum

class RefactoringTaskStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    VERIFYING = "verifying"
    SUCCESS = "success"
    FAILED = "failed"
    ROLLED_BACK = "rolled_back"

class RefactoringActionType(str, Enum):
    # 低风险全自动动作
    EXTRACT_CONSTANT = "extract_constant"
    RENAME = "rename"
    FORMAT_CODE = "format_code"
    REMOVE_DEAD_CODE = "remove_dead_code"
    COMPLETE_COMMENT = "complete_comment"
    EXTRACT_DUPLICATE_CODE = "extract_duplicate_code"
    # 中风险需审核动作
    SPLIT_FUNCTION = "split_function"
    SPLIT_CLASS = "split_class"
    EXTRACT_INTERFACE = "extract_interface"
    DECOUPLE_DEPENDENCY = "decouple_dependency"
    DEPENDENCY_INJECTION = "dependency_injection"
    # 高风险需审批动作
    SPLIT_MODULE = "split_module"
    ARCHITECTURE_ADJUSTMENT = "architecture_adjustment"
    FRAMEWORK_UPGRADE = "framework_upgrade"
    CROSS_LANGUAGE_MIGRATION = "cross_language_migration"

class AtomicRefactoringTask(BaseModel):
    """原子重构任务模型，重构的最小执行单元"""
    task_id: str = Field(description="全局唯一任务ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    parent_task_id: Optional[str] = Field(description="父任务ID", default=None)
    repository_id: str = Field(description="所属仓库ID")
    related_id: str = Field(description="关联的函数/文件/模块ID")
    related_type: str = Field(description="关联类型：function/file/module", enum=["function", "file", "module"])
    action_type: RefactoringActionType = Field(description="重构动作类型")
    refactoring_plan: dict = Field(description="单步重构方案，包含修改前、修改后、操作说明")
    target_branch: str = Field(description="重构目标分支")
    commit_id_before: Optional[str] = Field(description="重构前的commit ID", default=None)
    commit_id_after: Optional[str] = Field(description="重构后的commit ID", default=None)
    status: RefactoringTaskStatus = Field(description="任务状态", default=RefactoringTaskStatus.PENDING)
    risk_level: str = Field(description="风险等级", enum=["low", "medium", "high"])
    rollback_plan: str = Field(description="回滚方案")
    validation_criteria: List[str] = Field(description="验收标准", default_factory=list)
    validation_report_id: Optional[str] = Field(description="关联的验证报告ID", default=None)
    error_message: Optional[str] = Field(description="失败时的错误信息", default=None)
    created_by: str = Field(description="创建人", default="system")
    approved_by: Optional[str] = Field(description="审批人", default=None)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)

class RefactoringRootTask(BaseModel):
    """根重构任务，对应一个完整的重构需求"""
    root_task_id: str = Field(description="全局唯一根任务ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    repository_id: str = Field(description="所属仓库ID")
    refactoring_requirement: str = Field(description="用户原始重构需求")
    target_scope: List[str] = Field(description="重构范围，模块/文件ID列表")
    atomic_tasks: List[AtomicRefactoringTask] = Field(description="原子任务列表", default_factory=list)
    status: RefactoringTaskStatus = Field(description="任务状态", default=RefactoringTaskStatus.PENDING)
    start_time: Optional[datetime] = Field(description="开始时间", default=None)
    end_time: Optional[datetime] = Field(description="结束时间", default=None)
    created_by: str = Field(description="创建人", default="user")
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
```

### 4.4 验证报告核心模型
```python
# src/models/validation.py
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class ValidationResult(str, Enum):
    PASS = "pass"
    FAIL = "fail"
    WARNING = "warning"

class ValidationReport(BaseModel):
    """重构验证报告模型"""
    report_id: str = Field(description="全局唯一报告ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    task_id: str = Field(description="关联的原子重构任务ID")
    repository_id: str = Field(description="所属仓库ID")
    test_validation_result: ValidationResult = Field(description="测试用例验证结果")
    test_details: dict = Field(description="测试详情，包含通过率、失败用例、错误信息", default_factory=dict)
    semantic_validation_result: ValidationResult = Field(description="语义一致性验证结果")
    semantic_diff_details: str = Field(description="语义Diff详情", default="")
    static_scan_result: ValidationResult = Field(description="静态扫描验证结果")
    static_scan_details: dict = Field(description="静态扫描详情", default_factory=dict)
    overall_result: ValidationResult = Field(description="整体验证结果")
    is_manual_verified: bool = Field(description="是否经过人工审核", default=False)
    created_at: datetime = Field(default_factory=datetime.now)
```

---

## 五、核心模块详细设计（AI编码工具必须严格按接口实现）
### 5.1 基础设施层-LLM客户端抽象
**文件路径**：`src/infrastructure/llm/base.py` + `src/infrastructure/llm/impl.py`
#### 核心接口定义
```python
# src/infrastructure/llm/base.py
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
from pydantic import BaseModel

class LLMConfig(BaseModel):
    model_name: str
    api_key: str
    api_base: Optional[str] = None
    temperature: float = 0.0
    max_tokens: int = 4096
    timeout: int = 120
    max_retries: int = 3

class LLMBaseClient(ABC):
    """LLM客户端抽象基类，所有具体实现必须继承此类"""

    @abstractmethod
    def __init__(self, config: LLMConfig):
        pass

    @abstractmethod
    def invoke(self, prompt: str, system_prompt: Optional[str] = None) -> str:
        """同步调用LLM，返回纯文本结果"""
        pass

    @abstractmethod
    async def ainvoke(self, prompt: str, system_prompt: Optional[str] = None) -> str:
        """异步调用LLM，返回纯文本结果"""
        pass

    @abstractmethod
    def invoke_with_structured_output(
        self,
        prompt: str,
        output_schema: BaseModel,
        system_prompt: Optional[str] = None
    ) -> BaseModel:
        """同步调用LLM，返回结构化Pydantic模型结果"""
        pass

    @abstractmethod
    async def ainvoke_with_structured_output(
        self,
        prompt: str,
        output_schema: BaseModel,
        system_prompt: Optional[str] = None
    ) -> BaseModel:
        """异步调用LLM，返回结构化Pydantic模型结果"""
        pass
```
#### 实现要求
1.  必须实现OpenAI、Anthropic Claude、DeepSeek三个主流模型的具体实现类
2.  必须实现重试机制、超时处理、异常捕获、全链路日志记录
3.  必须实现Token用量统计，记录每次调用的输入/输出Token数
4.  必须实现Prompt模板渲染，集成Jinja2模板引擎

### 5.2 核心Agent-代码解析与预处理Agent
**文件路径**：`src/agents/scan_agents/code_parser_agent.py`
#### 核心类与接口定义
```python
from pydantic import BaseModel, Field
from typing import List, Optional
from src.models.code_metadata import CodeFileMetadata, CodeRepositoryMetadata, CodeFunctionMetadata
from src.infrastructure.parser.ast_parser import ASTParser
from src.infrastructure.git.git_client import GitClient
from loguru import logger

class CodeParserAgent:
    """
    代码解析与预处理Agent
    核心职责：拉取Git仓库、解析代码AST、提取代码元数据、计算复杂度、识别死代码
    """

    def __init__(
        self,
        ast_parser: ASTParser,
        git_client: GitClient
    ):
        self.ast_parser = ast_parser
        self.git_client = git_client
        logger.info("CodeParserAgent initialized")

    def clone_repository(self, git_url: str, target_branch: str) -> CodeRepositoryMetadata:
        """
        克隆Git仓库，生成仓库基础元数据
        :param git_url: Git仓库地址
        :param target_branch: 目标分支
        :return: 仓库元数据模型
        """
        # 实现要求：
        # 1. 调用git_client克隆仓库到本地临时目录
        # 2. 统计仓库基础信息：文件数、代码行数、语言分布
        # 3. 生成全局唯一repository_id
        # 4. 返回CodeRepositoryMetadata模型实例
        pass

    def parse_repository_files(self, repository: CodeRepositoryMetadata) -> List[CodeFileMetadata]:
        """
        全量解析仓库内的代码文件，生成文件元数据列表
        :param repository: 仓库元数据
        :return: 代码文件元数据列表
        """
        # 实现要求：
        # 1. 遍历仓库内的所有代码文件，过滤二进制文件、构建产物、第三方依赖
        # 2. 按文件类型并行调用parse_single_file方法
        # 3. 汇总所有文件元数据，更新仓库的统计信息
        # 4. 返回文件元数据列表
        pass

    def parse_single_file(self, file_path: str, repository_id: str) -> CodeFileMetadata:
        """
        解析单个代码文件，生成文件元数据
        :param file_path: 文件绝对路径
        :param repository_id: 所属仓库ID
        :return: 代码文件元数据模型
        """
        # 实现要求：
        # 1. 读取文件内容，判断代码语言
        # 2. 调用ast_parser解析文件AST
        # 3. 提取文件内的所有函数、类、导入、硬编码值
        # 4. 计算文件的行数统计、复杂度
        # 5. 生成全局唯一file_id
        # 6. 返回CodeFileMetadata模型实例
        pass

    def parse_function_metadata(self, ast_node: Any, file_metadata: CodeFileMetadata) -> CodeFunctionMetadata:
        """
        解析单个函数的AST节点，生成函数元数据
        :param ast_node: 函数AST节点
        :param file_metadata: 所属文件元数据
        :return: 函数元数据模型
        """
        # 实现要求：
        # 1. 提取函数名、参数、返回值、代码内容、注释
        # 2. 计算圈复杂度、认知复杂度
        # 3. 提取函数内调用的其他函数
        # 4. 生成全局唯一function_id
        # 5. 返回CodeFunctionMetadata模型实例
        pass

    def identify_dead_code(self, repository: CodeRepositoryMetadata, files: List[CodeFileMetadata]) -> List[CodeFileMetadata]:
        """
        识别仓库内的死代码，更新函数元数据的is_dead_code字段
        :param repository: 仓库元数据
        :param files: 文件元数据列表
        :return: 更新后的文件元数据列表
        """
        # 实现要求：
        # 1. 基于调用链路分析，识别无任何调用方的函数/类
        # 2. 识别已注释的代码块、永远无法执行的代码分支
        # 3. 更新对应函数元数据的is_dead_code字段
        # 4. 返回更新后的文件元数据列表
        pass
```

### 5.3 核心Agent-语义理解与业务抽象Agent
**文件路径**：`src/agents/scan_agents/semantic_understanding_agent.py`
#### 核心类与接口定义
```python
from typing import List
from src.models.code_metadata import CodeFileMetadata, CodeFunctionMetadata
from src.models.semantic_contract import BusinessSemanticContract
from src.infrastructure.llm.base import LLMBaseClient
from src.rag.index_builder import IndexBuilder
from loguru import logger
from jinja2 import Environment, FileSystemLoader

class SemanticUnderstandingAgent:
    """
    语义理解与业务抽象Agent
    核心职责：基于代码元数据，生成业务语义契约，构建语义向量索引
    """

    def __init__(
        self,
        llm_client: LLMBaseClient,
        index_builder: IndexBuilder,
        template_env: Environment
    ):
        self.llm_client = llm_client
        self.index_builder = index_builder
        self.template_env = template_env
        self.prompt_template = self.template_env.get_template("semantic_understanding/function_semantic_extract.j2")
        logger.info("SemanticUnderstandingAgent initialized")

    def generate_function_semantic_contract(
        self,
        function: CodeFunctionMetadata,
        file_metadata: CodeFileMetadata
    ) -> BusinessSemanticContract:
        """
        为单个函数生成业务语义契约
        :param function: 函数元数据
        :param file_metadata: 所属文件元数据
        :return: 业务语义契约模型
        """
        # 实现要求：
        # 1. 渲染Prompt模板，传入函数代码、注释、文件上下文
        # 2. 调用LLM的invoke_with_structured_output方法，指定输出为BusinessSemanticContract模型
        # 3. 生成全局唯一contract_id，关联函数ID
        # 4. 调用index_builder，将契约内容转为语义向量，存入向量数据库
        # 5. 返回BusinessSemanticContract模型实例
        pass

    def batch_generate_semantic_contracts(
        self,
        files: List[CodeFileMetadata]
    ) -> List[BusinessSemanticContract]:
        """
        批量为所有函数生成业务语义契约
        :param files: 文件元数据列表
        :return: 业务语义契约列表
        """
        # 实现要求：
        # 1. 遍历所有文件的所有函数，并行调用generate_function_semantic_contract方法
        # 2. 汇总所有契约，处理LLM调用异常
        # 3. 批量构建语义向量索引
        # 4. 返回契约列表
        pass

    def generate_file_level_contract(
        self,
        file_metadata: CodeFileMetadata,
        function_contracts: List[BusinessSemanticContract]
    ) -> BusinessSemanticContract:
        """
        基于函数契约，生成文件级的业务语义契约
        :param file_metadata: 文件元数据
        :param function_contracts: 文件内的函数契约列表
        :return: 文件级业务语义契约模型
        """
        # 实现要求：
        # 1. 聚合文件内所有函数的契约信息
        # 2. 调用LLM生成文件级的业务摘要、域标签
        # 3. 返回文件级BusinessSemanticContract模型实例
        pass
```

### 5.4 核心Agent-上下文检索与聚合Agent
**文件路径**：`src/agents/refactoring_agents/context_retrieval_agent.py`
#### 核心类与接口定义
```python
from typing import List, Dict, Any
from pydantic import BaseModel
from src.models.refactoring_task import AtomicRefactoringTask
from src.rag.retriever import MultiModalRetriever
from loguru import logger

class RefactoringContext(BaseModel):
    """重构上下文模型，LLM重构的核心输入"""
    task_id: str
    target_code: str
    target_metadata: Dict[str, Any]
    semantic_contract: Dict[str, Any]
    caller_context: List[Dict[str, Any]]
    dependency_context: List[Dict[str, Any]]
    business_domain_context: List[Dict[str, Any]]
    refactoring_rules: Dict[str, Any]
    test_cases: List[Dict[str, Any]]
    similar_refactoring_cases: List[Dict[str, Any]]
    context_token_count: int

class ContextRetrievalAgent:
    """
    上下文检索与聚合Agent
    核心职责：为重构任务检索最相关的上下文，组装成符合LLM窗口的结构化Prompt
    """

    def __init__(
        self,
        retriever: MultiModalRetriever,
        max_context_token: int = 180000
    ):
        self.retriever = retriever
        self.max_context_token = max_context_token
        logger.info("ContextRetrievalAgent initialized")

    def retrieve_and_assemble_context(
        self,
        task: AtomicRefactoringTask
    ) -> RefactoringContext:
        """
        检索并组装重构任务的上下文
        :param task: 原子重构任务
        :return: 重构上下文模型
        """
        # 实现要求：
        # 1. 基于任务的related_id，检索目标代码、元数据、语义契约
        # 2. 检索调用方上下文：所有调用目标代码的函数/文件信息
        # 3. 检索依赖上下文：目标代码依赖的所有函数/类/模块信息
        # 4. 检索同业务域的上下文：同业务标签的其他代码信息
        # 5. 检索重构规则、测试用例、类似重构案例
        # 6. 按优先级排序上下文，控制总Token数不超过max_context_token
        # 7. 生成RefactoringContext模型实例，统计Token数
        # 8. 返回重构上下文
        pass

    def retrieve_caller_context(self, related_id: str, related_type: str) -> List[Dict[str, Any]]:
        """
        检索调用方上下文
        :param related_id: 关联的函数/文件ID
        :param related_type: 关联类型
        :return: 调用方上下文列表
        """
        # 实现要求：从Neo4j图数据库检索调用链路，返回调用方的代码、元数据、契约
        pass

    def retrieve_dependency_context(self, related_id: str, related_type: str) -> List[Dict[str, Any]]:
        """
        检索依赖上下文
        :param related_id: 关联的函数/文件ID
        :param related_type: 关联类型
        :return: 依赖上下文列表
        """
        # 实现要求：从Neo4j图数据库检索依赖关系，返回依赖项的代码、元数据、契约
        pass
```

### 5.5 核心Agent-重构规划Agent
**文件路径**：`src/agents/refactoring_agents/refactoring_planning_agent.py`
#### 核心类与接口定义
```python
from typing import List
from src.models.refactoring_task import AtomicRefactoringTask, RefactoringActionType, RefactoringRootTask
from src.agents.refactoring_agents.context_retrieval_agent import RefactoringContext
from src.infrastructure.llm.base import LLMBaseClient
from loguru import logger
from jinja2 import Environment, FileSystemLoader

class RefactoringPlanningAgent:
    """
    重构规划Agent
    核心职责：基于重构上下文，设计原子化的重构方案，输出可执行的重构步骤
    """

    def __init__(
        self,
        llm_client: LLMBaseClient,
        template_env: Environment
    ):
        self.llm_client = llm_client
        self.template_env = template_env
        self.prompt_template = self.template_env.get_template("refactoring_planning/atomic_refactoring_plan.j2")
        logger.info("RefactoringPlanningAgent initialized")

    def decompose_root_task(
        self,
        root_task: RefactoringRootTask
    ) -> List[AtomicRefactoringTask]:
        """
        拆解根重构任务为原子重构任务
        :param root_task: 根重构任务
        :return: 原子重构任务列表
        """
        # 实现要求：
        # 1. 基于重构需求和目标范围，分析依赖关系，生成拓扑排序的任务拆解顺序
        # 2. 按P3→P2→P1→P0的风险等级排序，优先处理低风险任务
        # 3. 每个原子任务只包含一个重构动作，单次修改范围最小化
        # 4. 为每个原子任务设置风险等级、回滚方案、验收标准
        # 5. 返回原子重构任务列表
        pass

    def generate_atomic_refactoring_plan(
        self,
        task: AtomicRefactoringTask,
        context: RefactoringContext
    ) -> AtomicRefactoringTask:
        """
        为单个原子重构任务生成详细的重构方案
        :param task: 原子重构任务
        :param context: 重构上下文
        :return: 填充了重构方案的原子任务模型
        """
        # 实现要求：
        # 1. 渲染Prompt模板，传入重构上下文、任务信息、重构规则
        # 2. 调用LLM的invoke_with_structured_output方法，输出结构化的重构方案
        # 3. 校验重构方案是否符合「业务契约不变」原则，是否为原子化步骤
        # 4. 填充task的refactoring_plan、rollback_plan、validation_criteria字段
        # 5. 返回更新后的原子任务模型
        pass

    def validate_refactoring_plan(
        self,
        task: AtomicRefactoringTask,
        context: RefactoringContext
    ) -> bool:
        """
        验证重构方案的可行性与合规性
        :param task: 原子重构任务
        :param context: 重构上下文
        :return: 验证是否通过
        """
        # 实现要求：
        # 1. 检查重构方案是否修改了业务语义契约
        # 2. 检查重构方案是否符合重构规范
        # 3. 检查重构方案的依赖关系是否闭环
        # 4. 检查回滚方案是否可行
        # 5. 全部通过返回True，否则返回False
        pass
```

### 5.6 核心Agent-验证与修复Agent
**文件路径**：`src/agents/refactoring_agents/validation_agent.py`
#### 核心类与接口定义
```python
from src.models.refactoring_task import AtomicRefactoringTask
from src.models.validation import ValidationReport, ValidationResult
from src.agents.refactoring_agents.context_retrieval_agent import RefactoringContext
from src.infrastructure.llm.base import LLMBaseClient
from src.infrastructure.scanner.static_scanner import StaticScanner
from src.infrastructure.test.test_runner import TestRunner
from loguru import logger
from jinja2 import Environment, FileSystemLoader

class ValidationAgent:
    """
    验证与修复Agent
    核心职责：执行重构后的三层验证闭环，自动修复小问题，生成验证报告
    """

    def __init__(
        self,
        llm_client: LLMBaseClient,
        static_scanner: StaticScanner,
        test_runner: TestRunner,
        template_env: Environment
    ):
        self.llm_client = llm_client
        self.static_scanner = static_scanner
        self.test_runner = test_runner
        self.template_env = template_env
        self.semantic_diff_template = self.template_env.get_template("validation/semantic_diff_check.j2")
        logger.info("ValidationAgent initialized")

    def execute_full_validation(
        self,
        task: AtomicRefactoringTask,
        context: RefactoringContext,
        code_before: str,
        code_after: str
    ) -> ValidationReport:
        """
        执行全量三层验证闭环，生成验证报告
        :param task: 原子重构任务
        :param context: 重构上下文
        :param code_before: 重构前的代码
        :param code_after: 重构后的代码
        :return: 验证报告模型
        """
        # 实现要求：
        # 1. 执行测试用例验证，调用test_runner运行全量测试用例
        # 2. 执行语义一致性验证，调用LLM做语义Diff检查
        # 3. 执行静态扫描验证，调用static_scanner做代码质量、规范检查
        # 4. 汇总三个维度的验证结果，生成整体验证结果
        # 5. 生成全局唯一report_id，关联任务ID
        # 6. 返回ValidationReport模型实例
        pass

    def run_test_validation(
        self,
        task: AtomicRefactoringTask,
        context: RefactoringContext
    ) -> tuple[ValidationResult, dict]:
        """
        执行测试用例验证
        :param task: 原子重构任务
        :param context: 重构上下文
        :return: 验证结果、测试详情
        """
        # 实现要求：
        # 1. 调用test_runner运行上下文内的所有测试用例
        # 2. 统计通过率、失败用例、错误信息
        # 3. 100%通过返回PASS，否则返回FAIL
        # 4. 返回验证结果和测试详情
        pass

    def run_semantic_validation(
        self,
        code_before: str,
        code_after: str,
        semantic_contract: dict
    ) -> tuple[ValidationResult, str]:
        """
        执行业务语义一致性验证
        :param code_before: 重构前的代码
        :param code_after: 重构后的代码
        :param semantic_contract: 业务语义契约
        :return: 验证结果、语义Diff详情
        """
        # 实现要求：
        # 1. 渲染Prompt模板，传入重构前后代码、语义契约
        # 2. 调用LLM分析是否有业务行为、输入输出、副作用、业务规则的差异
        # 3. 无差异返回PASS，有差异返回FAIL
        # 4. 返回验证结果和语义Diff详情
        pass

    def run_static_scan_validation(
        self,
        file_path: str
    ) -> tuple[ValidationResult, dict]:
        """
        执行静态扫描验证
        :param file_path: 重构后的文件路径
        :return: 验证结果、扫描详情
        """
        # 实现要求：
        # 1. 调用static_scanner扫描重构后的文件
        # 2. 检查是否有新增技术债务、安全漏洞、规范违规
        # 3. 无新增问题返回PASS，有严重问题返回FAIL，警告返回WARNING
        # 4. 返回验证结果和扫描详情
        pass

    def auto_fix_code(
        self,
        task: AtomicRefactoringTask,
        context: RefactoringContext,
        code_after: str,
        validation_report: ValidationReport
    ) -> str:
        """
        基于验证报告自动修复代码问题
        :param task: 原子重构任务
        :param context: 重构上下文
        :param code_after: 重构后的有问题的代码
        :param validation_report: 验证报告
        :return: 修复后的代码
        """
        # 实现要求：
        # 1. 基于验证报告的错误信息，渲染修复Prompt
        # 2. 调用LLM生成修复后的代码
        # 3. 严格禁止修改业务语义契约，仅修复验证不通过的问题
        # 4. 返回修复后的代码
        pass
```

---

## 六、Prompt模板库固定规范
所有Prompt模板必须放在`templates/`目录下，按模块分类，严格遵循Jinja2语法，必须包含**明确的角色定义、约束规则、输入变量、输出格式要求**，禁止自由格式的Prompt。

### 核心模板清单（AI编码工具必须逐个实现）
| 模板路径 | 用途 | 输出格式 |
|----------|------|----------|
| templates/semantic_understanding/function_semantic_extract.j2 | 函数级业务语义契约提取 | JSON格式，严格匹配BusinessSemanticContract模型 |
| templates/semantic_understanding/file_level_semantic.j2 | 文件级业务语义契约生成 | JSON格式 |
| templates/refactoring_planning/task_decomposition.j2 | 根任务拆解为原子任务 | JSON格式，严格匹配AtomicRefactoringTask模型 |
| templates/refactoring_planning/atomic_refactoring_plan.j2 | 原子重构方案生成 | JSON格式 |
| templates/code_generation/atomic_code_refactor.j2 | 原子重构代码生成 | 纯代码格式，无额外解释 |
| templates/validation/semantic_diff_check.j2 | 语义一致性验证 | JSON格式，包含是否有差异、差异详情 |
| templates/validation/test_case_generation.j2 | 单元测试用例生成 | 纯代码格式 |

---

## 八、编码规范与强制约束
1.  **命名规范**：
    - 类名使用大驼峰命名法（PascalCase）
    - 函数、变量、参数使用蛇形命名法（snake_case）
    - 常量使用全大写蛇形命名法（UPPER_SNAKE_CASE）
    - 私有方法/变量使用单下划线前缀
2.  **注释规范**：
    - 所有类、公共方法必须有Google风格的docstring，包含功能描述、参数、返回值、异常
    - 核心业务逻辑必须有行内注释，说明设计思路
    - 禁止无意义的注释
3.  **异常处理规范**：
    - 所有外部调用必须有try-except捕获，禁止裸奔代码
    - 异常必须记录完整的日志，包含Traceback
    - 禁止空except块，必须处理异常或向上抛出
4.  **类型安全规范**：
    - 所有函数、方法必须有完整的类型注解，包含参数、返回值
    - 禁止使用Any类型，除非特殊场景
    - 所有数据模型必须基于Pydantic，实现类型校验
5.  **安全规范**：
    - 所有API接口必须有参数校验，防止注入攻击
    - 所有敏感配置（API密钥、数据库密码）必须通过环境变量加载，禁止硬编码
    - 所有用户输入必须经过校验和转义，禁止直接拼接SQL、Prompt

---

## 九、最终系统验收标准
1.  功能完整性：实现本文档定义的所有Agent、接口、功能，无遗漏
2.  代码质量：全量代码符合编码规范，核心模块测试覆盖率≥90%，所有测试用例100%通过
3.  性能要求：支持100万行代码库的全量扫描，完成时间≤24小时；支持100+并行原子重构任务
4.  准确性：业务语义契约提取准确率≥85%，重构代码语义一致性验证通过率≥95%
5.  可部署性：支持Docker一键部署，可在Linux环境稳定运行
6.  可扩展性：所有模块基于接口编程，可轻松替换LLM模型、数据库、扫描工具