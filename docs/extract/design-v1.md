# 全量代码资产AI扫描与语义建模系统详细设计文档与可执行开发任务清单
> 文档定位：AI编码工具可1:1落地执行的工程化开发规范，无模糊描述，所有模块、接口、数据结构、开发步骤均明确可编码
> 核心约束：AI编码工具必须严格遵循本文档的所有定义，禁止擅自修改接口、数据结构、核心流程，所有代码必须配套单元测试，符合编码规范

---

## 一、文档总则与核心红线规则
### 1.1 文档目的
本文档定义**全量代码资产AI扫描与语义建模系统**的工程化实现规范，指导AI编码工具完成从0到1的编码开发，最终交付可直接运行的、支持百万行级代码库的扫描建模系统，为后续AI重构提供精准的数据支撑。

### 1.2 不可突破的核心红线（AI编码工具必须100%遵守）
1.  **元数据完整性原则**：所有代码元数据（函数、类、依赖、注释、复杂度）必须100%提取，无遗漏
2.  **语义准确性原则**：业务语义契约提取必须基于代码原文，禁止LLM凭空捏造业务规则，不确定场景必须标注「待人工确认」
3.  **可追溯原则**：所有扫描结果、LLM调用、元数据变更必须留痕，生成可审计的全链路日志
4.  **并行性能原则**：百万行级代码库的全量扫描必须支持并行处理，完成时间≤24小时
5.  **增量扫描原则**：首次全量扫描后，必须支持基于Git变更的增量扫描，避免重复解析
6.  **代码规范原则**：所有代码必须符合本文档定义的编码规范，配套单元测试，核心模块测试覆盖率≥90%

### 1.3 目标运行环境
- 操作系统：Linux/Windows/macOS 兼容
- Python版本：Python 3.11+
- 部署模式：支持本地单机部署、Docker容器化部署
- 并发能力：支持10+并行仓库扫描、100+并行文件解析任务

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
| 关系型数据库 | PostgreSQL 15+ | 存储元数据、任务信息、配置、扫描台账、报告 |
| ORM框架 | SQLAlchemy 2.0+ | 数据库ORM操作，类型安全 |
| 配置管理 | Pydantic Settings 2.0+ | 环境变量、配置文件统一管理 |
| 任务队列 | Celery 5.3+ + Redis 7.0+ | 并行处理代码扫描、解析任务，异步调度 |
| 日志管理 | Loguru 0.7+ | 结构化全链路日志 |
| 代码规范工具 | Ruff、Black、isort | Python代码格式化、lint校验 |
| 测试框架 | Pytest 7.4+ | 单元测试、集成测试、E2E测试 |
| 模板引擎 | Jinja2 3.1+ | LLM Prompt模板统一管理 |
| 静态扫描集成 | SonarQube API、Semgrep、Lizard | 代码质量、安全漏洞、复杂度扫描集成 |
| 可视化工具 | ECharts、Mermaid、PlantUML | 代码资产全景图、依赖图、热力图生成 |

---

## 三、系统整体架构与目录结构
### 3.1 分层架构定义
```
┌─────────────────────────────────────────────────────────────┐
│ 接入层：API网关、任务调度入口、Git仓库接入、Web控制台        │
├─────────────────────────────────────────────────────────────┤
│ 核心Agent层：扫描建模Agent集群                                │
│ 1. 代码仓库接入Agent        2. 代码解析与预处理Agent        │
│ 3. 语义理解与业务抽象Agent  4. 依赖分析与架构逆向Agent      │
│ 5. 风险评估与优先级Agent    6. 业务-代码映射Agent           │
│ 7. 可视化与报告生成Agent                                    │
├─────────────────────────────────────────────────────────────┤
│ 基础设施层：LLM客户端抽象、数据库客户端、工具集、异常处理    │
├─────────────────────────────────────────────────────────────┤
│ 存储层：PostgreSQL、Milvus、Neo4j、Redis、本地文件存储      │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 固定项目目录结构（AI编码工具必须严格生成）
```
code-asset-scanner/
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
│   └── scan_rules.yaml            # 扫描规则、过滤规则配置
├── templates/                     # Jinja2 Prompt模板目录
│   ├── semantic_understanding/    # 语义理解相关Prompt模板
│   ├── dependency_analysis/       # 依赖分析相关Prompt模板
│   └── risk_assessment/           # 风险评估相关Prompt模板
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
│   │   ├── dependency.py          # 依赖关系模型
│   │   ├── risk.py                # 风险评估模型
│   │   └── scan_task.py           # 扫描任务相关模型
│   ├── agents/                    # 核心Agent实现
│   │   ├── __init__.py
│   │   └── scan_agents/           # 扫描建模Agent集群
│   ├── services/                  # 业务服务层
│   │   ├── __init__.py
│   │   ├── scan_service.py        # 扫描全流程服务
│   │   └── report_service.py      # 报告生成服务
│   ├── rag/                       # RAG索引引擎实现
│   │   ├── __init__.py
│   │   ├── index_builder.py       # 索引构建器
│   │   └── retriever.py           # 检索器（为后续重构预留）
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
from typing import List, Optional, Dict, Any
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
    RUST = "rust"
    PHP = "php"

class CodeRiskLevel(str, Enum):
    P0 = "P0" # 核心业务，最高风险
    P1 = "P1" # 重要业务，高风险
    P2 = "P2" # 通用模块，中风险
    P3 = "P3" # 边缘模块，低风险

class CodeParameter(BaseModel):
    """函数参数模型"""
    name: str = Field(description="参数名")
    type_hint: Optional[str] = Field(description="类型注解", default=None)
    default_value: Optional[str] = Field(description="默认值", default=None)
    description: Optional[str] = Field(description="参数描述（从注释提取）", default=None)

class CodeClassMetadata(BaseModel):
    """类元数据模型"""
    class_id: str = Field(description="全局唯一类ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    file_id: str = Field(description="所属文件ID")
    module_id: str = Field(description="所属模块ID")
    repository_id: str = Field(description="所属仓库ID")
    class_name: str = Field(description="类名")
    start_line: int = Field(description="起始行号")
    end_line: int = Field(description="结束行号")
    code_content: str = Field(description="类代码原文")
    comment: Optional[str] = Field(description="类注释", default=None)
    parent_classes: List[str] = Field(description="父类列表", default_factory=list)
    implemented_interfaces: List[str] = Field(description="实现的接口列表", default_factory=list)
    methods: List[str] = Field(description="类内方法ID列表", default_factory=list)
    attributes: List[Dict[str, Any]] = Field(description="类属性列表", default_factory=list)
    cyclomatic_complexity: int = Field(description="类整体圈复杂度", ge=0)
    is_god_class: bool = Field(description="是否为上帝类（方法数≥20或属性数≥15）", default=False)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)

class CodeFunctionMetadata(BaseModel):
    """函数/方法元数据模型"""
    function_id: str = Field(description="全局唯一函数ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    file_id: str = Field(description="所属文件ID")
    module_id: str = Field(description="所属模块ID")
    repository_id: str = Field(description="所属仓库ID")
    class_id: Optional[str] = Field(description="所属类ID（类方法时必填）", default=None)
    function_name: str = Field(description="函数名")
    is_method: bool = Field(description="是否为类方法", default=False)
    access_modifier: Optional[str] = Field(description="访问修饰符（public/private/protected）", default=None)
    start_line: int = Field(description="起始行号")
    end_line: int = Field(description="结束行号")
    code_content: str = Field(description="函数代码原文")
    comment: Optional[str] = Field(description="函数注释", default=None)
    parameters: List[CodeParameter] = Field(description="参数列表", default_factory=list)
    return_type: Optional[str] = Field(description="返回值类型", default=None)
    called_functions: List[str] = Field(description="本函数调用的其他函数ID列表", default_factory=list)
    caller_functions: List[str] = Field(description="调用本函数的其他函数ID列表", default_factory=list)
    imported_functions: List[str] = Field(description="本函数使用的导入函数/类列表", default_factory=list)
    cyclomatic_complexity: int = Field(description="圈复杂度", ge=0)
    cognitive_complexity: int = Field(description="认知复杂度", ge=0)
    code_lines: int = Field(description="函数代码行数", ge=0)
    is_dead_code: bool = Field(description="是否为死代码", default=False)
    is_long_function: bool = Field(description="是否为过长函数（代码行数≥50）", default=False)
    has_hard_coded_values: bool = Field(description="是否包含硬编码值", default=False)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)

class CodeFileMetadata(BaseModel):
    """代码文件元数据模型"""
    file_id: str = Field(description="全局唯一文件ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    module_id: str = Field(description="所属模块ID")
    repository_id: str = Field(description="所属仓库ID")
    file_path: str = Field(description="文件相对仓库根目录的路径")
    file_name: str = Field(description="文件名")
    file_extension: str = Field(description="文件扩展名")
    language: CodeLanguage = Field(description="代码语言")
    total_lines: int = Field(description="总行数", ge=0)
    code_lines: int = Field(description="代码行数", ge=0)
    comment_lines: int = Field(description="注释行数", ge=0)
    blank_lines: int = Field(description="空行数", ge=0)
    comment_ratio: float = Field(description="注释率", ge=0.0, le=1.0)
    functions: List[CodeFunctionMetadata] = Field(description="文件内的函数列表", default_factory=list)
    classes: List[CodeClassMetadata] = Field(description="文件内的类列表", default_factory=list)
    imports: List[Dict[str, Any]] = Field(description="导入依赖列表，包含source、target、type", default_factory=list)
    hard_coded_values: List[Dict[str, Any]] = Field(description="硬编码值列表，包含value、line、type", default_factory=list)
    risk_level: CodeRiskLevel = Field(description="风险等级", default=CodeRiskLevel.P2)
    is_binary: bool = Field(description="是否为二进制文件", default=False)
    is_third_party: bool = Field(description="是否为第三方依赖文件", default=False)
    is_generated: bool = Field(description="是否为自动生成文件", default=False)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)

class CodeModuleMetadata(BaseModel):
    """代码模块元数据模型（按目录划分）"""
    module_id: str = Field(description="全局唯一模块ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    repository_id: str = Field(description="所属仓库ID")
    module_path: str = Field(description="模块相对仓库根目录的路径")
    module_name: str = Field(description="模块名")
    total_files: int = Field(description="总文件数", ge=0)
    total_code_lines: int = Field(description="总代码行数", ge=0)
    files: List[str] = Field(description="模块内的文件ID列表", default_factory=list)
    sub_modules: List[str] = Field(description="子模块ID列表", default_factory=list)
    risk_level: CodeRiskLevel = Field(description="模块风险等级", default=CodeRiskLevel.P2)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)

class CodeRepositoryMetadata(BaseModel):
    """代码仓库元数据模型"""
    repository_id: str = Field(description="全局唯一仓库ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    repository_name: str = Field(description="仓库名")
    git_url: str = Field(description="Git仓库地址")
    default_branch: str = Field(description="默认分支", default="main")
    target_branch: str = Field(description="扫描目标分支", default="main")
    local_path: str = Field(description="本地克隆路径")
    total_files: int = Field(description="总文件数", ge=0)
    total_code_files: int = Field(description="总代码文件数", ge=0)
    total_code_lines: int = Field(description="总代码行数", ge=0)
    language_distribution: Dict[str, int] = Field(description="语言分布统计", default_factory=dict)
    modules: List[str] = Field(description="模块ID列表", default_factory=list)
    scan_status: str = Field(description="扫描状态", enum=["pending", "running", "completed", "failed", "partial"], default="pending")
    last_scan_commit: Optional[str] = Field(description="最后扫描的commit ID", default=None)
    last_scan_time: Optional[datetime] = Field(description="最后扫描时间", default=None)
    scan_error_message: Optional[str] = Field(description="扫描失败时的错误信息", default=None)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
```

### 4.2 业务语义契约核心模型
```python
# src/models/semantic_contract.py
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime

class BusinessSemanticContract(BaseModel):
    """业务语义契约模型，扫描建模的核心输出"""
    contract_id: str = Field(description="全局唯一契约ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    related_id: str = Field(description="关联的函数/类/文件/模块ID")
    related_type: str = Field(description="关联类型：function/class/file/module", enum=["function", "class", "file", "module"])
    repository_id: str = Field(description="所属仓库ID")
    business_summary: str = Field(description="业务功能摘要，自然语言描述，1-2句话")
    detailed_description: Optional[str] = Field(description="详细业务描述", default=None)
    input_contract: List[Dict[str, Any]] = Field(description="输入契约，包含param_name、business_meaning、constraints、example", default_factory=list)
    output_contract: Dict[str, Any] = Field(description="输出契约，包含business_meaning、data_structure、example", default_factory=dict)
    side_effects: List[str] = Field(description="副作用列表，如写数据库表、调用外部接口、发送MQ消息、修改全局变量", default_factory=list)
    business_rules: List[str] = Field(description="核心业务规则列表，必须从代码原文提取", default_factory=list)
    exception_scenarios: List[Dict[str, Any]] = Field(description="异常处理场景列表，包含scenario、handling_logic", default_factory=list)
    business_domain_tags: List[str] = Field(description="业务域标签列表，如支付、订单、用户、权限", default_factory=list)
    technical_tags: List[str] = Field(description="技术标签列表，如REST API、数据库操作、缓存、异步", default_factory=list)
    semantic_vector_id: Optional[str] = Field(description="关联的语义向量ID", default=None)
    is_manual_verified: bool = Field(description="是否经过人工审核", default=False)
    verification_status: str = Field(description="验证状态", enum=["pending", "verified", "rejected", "needs_review"], default="pending")
    confidence_score: float = Field(description="LLM提取的置信度分数，0.0-1.0", ge=0.0, le=1.0, default=0.5)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
```

### 4.3 依赖关系核心模型
```python
# src/models/dependency.py
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
from enum import Enum

class DependencyType(str, Enum):
    FUNCTION_CALL = "function_call"
    CLASS_INHERITANCE = "class_inheritance"
    INTERFACE_IMPLEMENTATION = "interface_implementation"
    IMPORT = "import"
    MODULE_DEPENDENCY = "module_dependency"
    EXTERNAL_API = "external_api"
    DATABASE_TABLE = "database_table"

class CodeDependency(BaseModel):
    """代码依赖关系模型"""
    dependency_id: str = Field(description="全局唯一依赖ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    repository_id: str = Field(description="所属仓库ID")
    source_id: str = Field(description="依赖源ID（调用方/导入方）")
    source_type: str = Field(description="依赖源类型", enum=["function", "class", "file", "module"])
    target_id: str = Field(description="依赖目标ID（被调用方/被导入方）")
    target_type: str = Field(description="依赖目标类型", enum=["function", "class", "file", "module", "external", "database"])
    dependency_type: DependencyType = Field(description="依赖类型")
    is_circular: bool = Field(description="是否为循环依赖", default=False)
    circular_path: Optional[List[str]] = Field(description="循环依赖路径", default=None)
    call_count: int = Field(description="调用次数（函数调用场景）", ge=0, default=1)
    created_at: datetime = Field(default_factory=datetime.now)

class CallLink(BaseModel):
    """调用链路模型"""
    link_id: str = Field(description="全局唯一链路ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    repository_id: str = Field(description="所属仓库ID")
    entry_point_id: str = Field(description="入口点ID（如API接口函数）")
    entry_point_type: str = Field(description="入口点类型")
    call_chain: List[str] = Field(description="调用链ID列表，从入口到末端")
    total_depth: int = Field(description="调用链深度", ge=1)
    is_core_link: bool = Field(description="是否为核心业务链路", default=False)
    created_at: datetime = Field(default_factory=datetime.now)
```

### 4.4 风险评估核心模型
```python
# src/models/risk.py
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime
from enum import Enum

class RiskType(str, Enum):
    TECHNICAL_DEBT = "technical_debt"
    SECURITY_VULNERABILITY = "security_vulnerability"
    CODE_SMELL = "code_smell"
    DEAD_CODE = "dead_code"
    CIRCULAR_DEPENDENCY = "circular_dependency"
    HOTSPOT = "hotspot"
    HARD_CODED = "hard_coded"

class RiskSeverity(str, Enum):
    CRITICAL = "critical"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
    INFO = "info"

class CodeRisk(BaseModel):
    """代码风险模型"""
    risk_id: str = Field(description="全局唯一风险ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    repository_id: str = Field(description="所属仓库ID")
    related_id: str = Field(description="关联的函数/类/文件/模块ID")
    related_type: str = Field(description="关联类型", enum=["function", "class", "file", "module"])
    risk_type: RiskType = Field(description="风险类型")
    severity: RiskSeverity = Field(description="风险严重程度")
    title: str = Field(description="风险标题")
    description: str = Field(description="风险详细描述")
    location: Dict[str, Any] = Field(description="风险位置，包含file_path、start_line、end_line", default_factory=dict)
    recommendation: Optional[str] = Field(description="修复建议", default=None)
    is_fixed: bool = Field(description="是否已修复", default=False)
    fixed_at: Optional[datetime] = Field(description="修复时间", default=None)
    created_at: datetime = Field(default_factory=datetime.now)

class HotspotAnalysis(BaseModel):
    """热点代码分析模型"""
    hotspot_id: str = Field(description="全局唯一热点ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    repository_id: str = Field(description="所属仓库ID")
    related_id: str = Field(description="关联的函数/类/文件ID")
    related_type: str = Field(description="关联类型")
    commit_count: int = Field(description="提交次数", ge=0)
    bug_count: int = Field(description="关联Bug数", ge=0)
    complexity_score: float = Field(description="复杂度分数", ge=0.0)
    hotspot_score: float = Field(description="热点综合分数，0.0-1.0", ge=0.0, le=1.0)
    risk_level: str = Field(description="风险等级", enum=["P0", "P1", "P2", "P3"])
    created_at: datetime = Field(default_factory=datetime.now)
```

### 4.5 扫描任务核心模型
```python
# src/models/scan_task.py
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
from enum import Enum

class ScanTaskStatus(str, Enum):
    PENDING = "pending"
    CLONING = "cloning"
    PARSING = "parsing"
    SEMANTIC_EXTRACTING = "semantic_extracting"
    DEPENDENCY_ANALYZING = "dependency_analyzing"
    RISK_ASSESSING = "risk_assessing"
    REPORT_GENERATING = "report_generating"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"

class ScanTask(BaseModel):
    """扫描任务模型"""
    task_id: str = Field(description="全局唯一任务ID", pattern=r"^[a-zA-Z0-9_.-]+$")
    repository_id: str = Field(description="所属仓库ID")
    task_type: str = Field(description="任务类型", enum=["full_scan", "incremental_scan", "module_scan"])
    target_scope: Optional[List[str]] = Field(description="扫描范围，模块/文件路径列表（增量/模块扫描时必填）", default=None)
    target_commit: Optional[str] = Field(description="目标commit ID（增量扫描时必填）", default=None)
    base_commit: Optional[str] = Field(description="基准commit ID（增量扫描时必填）", default=None)
    status: ScanTaskStatus = Field(description="任务状态", default=ScanTaskStatus.PENDING)
    progress: int = Field(description="进度百分比，0-100", ge=0, le=100, default=0)
    progress_message: Optional[str] = Field(description="进度消息", default=None)
    start_time: Optional[datetime] = Field(description="开始时间", default=None)
    end_time: Optional[datetime] = Field(description="结束时间", default=None)
    error_message: Optional[str] = Field(description="失败时的错误信息", default=None)
    created_by: str = Field(description="创建人", default="system")
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
```

---

## 五、核心模块详细设计（AI编码工具必须严格按接口实现）
### 5.1 基础设施层-LLM客户端抽象
**文件路径**：`src/infrastructure/llm/base.py` + `src/infrastructure/llm/impl.py`
#### 核心接口定义
```python
# src/infrastructure/llm/base.py
from abc import ABC, abstractmethod
from typing import Optional
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

### 5.2 核心Agent-代码仓库接入Agent
**文件路径**：`src/agents/scan_agents/repository_access_agent.py`
#### 核心类与接口定义
```python
from typing import Optional
from src.models.code_metadata import CodeRepositoryMetadata
from src.infrastructure.git.git_client import GitClient
from src.infrastructure.common.utils import generate_unique_id
from loguru import logger

class RepositoryAccessAgent:
    """
    代码仓库接入Agent
    核心职责：克隆Git仓库、更新仓库、生成仓库基础元数据、识别仓库结构
    """

    def __init__(self, git_client: GitClient):
        self.git_client = git_client
        logger.info("RepositoryAccessAgent initialized")

    def clone_and_init_repository(
        self,
        git_url: str,
        repository_name: str,
        target_branch: str = "main",
        local_base_path: str = "/tmp/code-repos"
    ) -> CodeRepositoryMetadata:
        """
        克隆Git仓库并初始化仓库元数据
        :param git_url: Git仓库地址
        :param repository_name: 仓库名
        :param target_branch: 目标分支
        :param local_base_path: 本地仓库存储根路径
        :return: 仓库元数据模型
        """
        # 实现要求：
        # 1. 生成全局唯一repository_id
        # 2. 调用git_client克隆仓库到本地临时目录
        # 3. 切换到目标分支
        # 4. 统计仓库基础信息：总文件数、语言分布
        # 5. 生成CodeRepositoryMetadata模型实例
        # 6. 返回仓库元数据
        pass

    def update_repository(
        self,
        repository: CodeRepositoryMetadata
    ) -> CodeRepositoryMetadata:
        """
        更新已存在的仓库到最新commit
        :param repository: 仓库元数据
        :return: 更新后的仓库元数据
        """
        # 实现要求：
        # 1. 调用git_client拉取最新代码
        # 2. 获取最新commit ID
        # 3. 更新仓库元数据的last_scan_commit字段
        # 4. 返回更新后的仓库元数据
        pass

    def get_changed_files(
        self,
        repository: CodeRepositoryMetadata,
        base_commit: str,
        target_commit: str
    ) -> list[str]:
        """
        获取两个commit之间的变更文件列表
        :param repository: 仓库元数据
        :param base_commit: 基准commit ID
        :param target_commit: 目标commit ID
        :return: 变更文件相对路径列表
        """
        # 实现要求：
        # 1. 调用git_client获取两个commit之间的diff
        # 2. 解析diff，提取变更文件列表
        # 3. 过滤二进制文件、第三方依赖文件
        # 4. 返回变更文件列表
        pass

    def identify_repository_modules(
        self,
        repository: CodeRepositoryMetadata
    ) -> list[str]:
        """
        识别仓库的模块结构（按目录划分）
        :param repository: 仓库元数据
        :return: 模块相对路径列表
        """
        # 实现要求：
        # 1. 遍历仓库目录结构
        # 2. 按常见项目结构识别模块（如src/下的子目录、packages/下的子目录）
        # 3. 过滤构建产物、测试目录、文档目录
        # 4. 返回模块相对路径列表
        pass
```

### 5.3 核心Agent-代码解析与预处理Agent
**文件路径**：`src/agents/scan_agents/code_parser_agent.py`
#### 核心类与接口定义
```python
from typing import List, Optional
from src.models.code_metadata import (
    CodeRepositoryMetadata,
    CodeModuleMetadata,
    CodeFileMetadata,
    CodeFunctionMetadata,
    CodeClassMetadata,
    CodeLanguage
)
from src.infrastructure.parser.ast_parser import ASTParser
from src.infrastructure.scanner.complexity_scanner import ComplexityScanner
from src.infrastructure.common.utils import generate_unique_id
from loguru import logger

class CodeParserAgent:
    """
    代码解析与预处理Agent
    核心职责：解析代码AST、提取代码元数据、计算复杂度、识别死代码、过滤第三方文件
    """

    def __init__(
        self,
        ast_parser: ASTParser,
        complexity_scanner: ComplexityScanner
    ):
        self.ast_parser = ast_parser
        self.complexity_scanner = complexity_scanner
        logger.info("CodeParserAgent initialized")

    def parse_repository(
        self,
        repository: CodeRepositoryMetadata,
        target_files: Optional[List[str]] = None
    ) -> tuple[List[CodeModuleMetadata], List[CodeFileMetadata]]:
        """
        全量或增量解析仓库代码
        :param repository: 仓库元数据
        :param target_files: 目标文件列表（增量扫描时必填），None表示全量解析
        :return: 模块元数据列表、文件元数据列表
        """
        # 实现要求：
        # 1. 识别仓库模块结构
        # 2. 遍历目标文件（或全量文件）
        # 3. 按文件类型并行调用parse_single_file方法
        # 4. 汇总文件元数据，生成模块元数据
        # 5. 识别死代码，更新函数元数据
        # 6. 返回模块和文件元数据列表
        pass

    def parse_single_file(
        self,
        file_path: str,
        repository: CodeRepositoryMetadata,
        module: Optional[CodeModuleMetadata] = None
    ) -> CodeFileMetadata:
        """
        解析单个代码文件，生成文件元数据
        :param file_path: 文件绝对路径
        :param repository: 仓库元数据
        :param module: 所属模块元数据（可选）
        :return: 代码文件元数据模型
        """
        # 实现要求：
        # 1. 读取文件内容，判断是否为二进制文件、第三方文件、自动生成文件
        # 2. 根据文件扩展名判断代码语言
        # 3. 调用ast_parser解析文件AST
        # 4. 提取文件内的所有类、函数、导入、硬编码值
        # 5. 调用complexity_scanner计算文件、类、函数的复杂度
        # 6. 统计文件的行数、注释率
        # 7. 生成全局唯一file_id、module_id（如果需要）
        # 8. 返回CodeFileMetadata模型实例
        pass

    def parse_function(
        self,
        ast_node: any,
        file_metadata: CodeFileMetadata,
        class_metadata: Optional[CodeClassMetadata] = None
    ) -> CodeFunctionMetadata:
        """
        解析单个函数的AST节点，生成函数元数据
        :param ast_node: 函数AST节点
        :param file_metadata: 所属文件元数据
        :param class_metadata: 所属类元数据（类方法时必填）
        :return: 函数元数据模型
        """
        # 实现要求：
        # 1. 提取函数名、访问修饰符、参数、返回值、代码内容、注释
        # 2. 提取函数内调用的其他函数、使用的导入
        # 3. 计算圈复杂度、认知复杂度、代码行数
        # 4. 判断是否为过长函数、是否包含硬编码值
        # 5. 生成全局唯一function_id
        # 6. 返回CodeFunctionMetadata模型实例
        pass

    def parse_class(
        self,
        ast_node: any,
        file_metadata: CodeFileMetadata
    ) -> CodeClassMetadata:
        """
        解析单个类的AST节点，生成类元数据
        :param ast_node: 类AST节点
        :param file_metadata: 所属文件元数据
        :return: 类元数据模型
        """
        # 实现要求：
        # 1. 提取类名、父类、实现的接口、代码内容、注释
        # 2. 提取类内的所有方法、属性
        # 3. 计算类整体圈复杂度
        # 4. 判断是否为上帝类
        # 5. 生成全局唯一class_id
        # 6. 返回CodeClassMetadata模型实例
        pass

    def identify_dead_code(
        self,
        files: List[CodeFileMetadata]
    ) -> List[CodeFileMetadata]:
        """
        识别仓库内的死代码，更新函数元数据的is_dead_code字段
        :param files: 文件元数据列表
        :return: 更新后的文件元数据列表
        """
        # 实现要求：
        # 1. 构建函数调用关系图
        # 2. 从入口点（如API接口、main函数）开始遍历，标记可达函数
        # 3. 未标记的函数标记为死代码
        # 4. 识别已注释的代码块、永远无法执行的代码分支
        # 5. 更新对应函数元数据的is_dead_code字段
        # 6. 返回更新后的文件元数据列表
        pass
```

### 5.4 核心Agent-语义理解与业务抽象Agent
**文件路径**：`src/agents/scan_agents/semantic_understanding_agent.py`
#### 核心类与接口定义
```python
from typing import List
from src.models.code_metadata import CodeFileMetadata, CodeFunctionMetadata, CodeClassMetadata
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
        self.function_semantic_template = self.template_env.get_template("semantic_understanding/function_semantic_extract.j2")
        self.class_semantic_template = self.template_env.get_template("semantic_understanding/class_semantic_extract.j2")
        self.file_semantic_template = self.template_env.get_template("semantic_understanding/file_semantic_aggregate.j2")
        logger.info("SemanticUnderstandingAgent initialized")

    def generate_function_semantic_contract(
        self,
        function: CodeFunctionMetadata,
        file_metadata: CodeFileMetadata,
        class_metadata: CodeClassMetadata = None
    ) -> BusinessSemanticContract:
        """
        为单个函数生成业务语义契约
        :param function: 函数元数据
        :param file_metadata: 所属文件元数据
        :param class_metadata: 所属类元数据（类方法时必填）
        :return: 业务语义契约模型
        """
        # 实现要求：
        # 1. 渲染Prompt模板，传入函数代码、注释、文件上下文、类上下文（如果有）
        # 2. 调用LLM的invoke_with_structured_output方法，指定输出为BusinessSemanticContract模型
        # 3. 生成全局唯一contract_id，关联函数ID
        # 4. 计算LLM提取的置信度分数（基于输出的完整性、一致性）
        # 5. 调用index_builder，将契约内容转为语义向量，存入向量数据库
        # 6. 返回BusinessSemanticContract模型实例
        pass

    def generate_class_semantic_contract(
        self,
        class_metadata: CodeClassMetadata,
        file_metadata: CodeFileMetadata,
        function_contracts: List[BusinessSemanticContract]
    ) -> BusinessSemanticContract:
        """
        基于类元数据和方法契约，生成类级的业务语义契约
        :param class_metadata: 类元数据
        :param file_metadata: 所属文件元数据
        :param function_contracts: 类内方法的契约列表
        :return: 类级业务语义契约模型
        """
        # 实现要求：
        # 1. 聚合类内所有方法的契约信息
        # 2. 渲染Prompt模板，传入类代码、注释、方法契约
        # 3. 调用LLM生成类级的业务摘要、域标签、技术标签
        # 4. 生成全局唯一contract_id，关联类ID
        # 5. 构建语义向量并存入向量数据库
        # 6. 返回类级BusinessSemanticContract模型实例
        pass

    def generate_file_semantic_contract(
        self,
        file_metadata: CodeFileMetadata,
        function_contracts: List[BusinessSemanticContract],
        class_contracts: List[BusinessSemanticContract]
    ) -> BusinessSemanticContract:
        """
        基于文件元数据和内部契约，生成文件级的业务语义契约
        :param file_metadata: 文件元数据
        :param function_contracts: 文件内函数契约列表
        :param class_contracts: 文件内类契约列表
        :return: 文件级业务语义契约模型
        """
        # 实现要求：
        # 1. 聚合文件内所有函数、类的契约信息
        # 2. 渲染Prompt模板，传入文件路径、元数据、内部契约
        # 3. 调用LLM生成文件级的业务摘要、域标签、技术标签
        # 4. 生成全局唯一contract_id，关联文件ID
        # 5. 构建语义向量并存入向量数据库
        # 6. 返回文件级BusinessSemanticContract模型实例
        pass

    def batch_generate_semantic_contracts(
        self,
        files: List[CodeFileMetadata]
    ) -> List[BusinessSemanticContract]:
        """
        批量为所有函数、类、文件生成业务语义契约
        :param files: 文件元数据列表
        :return: 业务语义契约列表
        """
        # 实现要求：
        # 1. 遍历所有文件的所有函数，并行调用generate_function_semantic_contract方法
        # 2. 遍历所有文件的所有类，并行调用generate_class_semantic_contract方法
        # 3. 遍历所有文件，并行调用generate_file_semantic_contract方法
        # 4. 汇总所有契约，处理LLM调用异常（异常时标记为needs_review）
        # 5. 批量构建语义向量索引
        # 6. 返回契约列表
        pass
```

### 5.5 核心Agent-依赖分析与架构逆向Agent
**文件路径**：`src/agents/scan_agents/dependency_analysis_agent.py`
#### 核心类与接口定义
```python
from typing import List
from src.models.code_metadata import CodeRepositoryMetadata, CodeFileMetadata, CodeFunctionMetadata
from src.models.dependency import CodeDependency, CallLink, DependencyType
from src.infrastructure.database.neo4j_client import Neo4jClient
from loguru import logger

class DependencyAnalysisAgent:
    """
    依赖分析与架构逆向Agent
    核心职责：构建代码依赖关系图、识别循环依赖、生成调用链路、逆向架构结构
    """

    def __init__(self, neo4j_client: Neo4jClient):
        self.neo4j_client = neo4j_client
        logger.info("DependencyAnalysisAgent initialized")

    def build_dependency_graph(
        self,
        repository: CodeRepositoryMetadata,
        files: List[CodeFileMetadata]
    ) -> List[CodeDependency]:
        """
        构建代码依赖关系图，存入Neo4j
        :param repository: 仓库元数据
        :param files: 文件元数据列表
        :return: 依赖关系列表
        """
        # 实现要求：
        # 1. 遍历所有文件的导入关系，生成IMPORT类型的依赖
        # 2. 遍历所有函数的调用关系，生成FUNCTION_CALL类型的依赖
        # 3. 遍历所有类的继承、实现关系，生成CLASS_INHERITANCE、INTERFACE_IMPLEMENTATION类型的依赖
        # 4. 识别外部API调用、数据库表操作，生成EXTERNAL_API、DATABASE_TABLE类型的依赖
        # 5. 生成全局唯一dependency_id
        # 6. 将所有依赖关系存入Neo4j图数据库
        # 7. 返回依赖关系列表
        pass

    def detect_circular_dependencies(
        self,
        repository: CodeRepositoryMetadata
    ) -> List[CodeDependency]:
        """
        检测循环依赖
        :param repository: 仓库元数据
        :return: 循环依赖列表
        """
        # 实现要求：
        # 1. 从Neo4j查询模块、文件级的依赖关系
        # 2. 使用图算法检测循环依赖路径
        # 3. 更新对应依赖的is_circular、circular_path字段
        # 4. 将更新后的依赖存入Neo4j
        # 5. 返回循环依赖列表
        pass

    def generate_call_links(
        self,
        repository: CodeRepositoryMetadata,
        files: List[CodeFileMetadata]
    ) -> List[CallLink]:
        """
        生成调用链路
        :param repository: 仓库元数据
        :param files: 文件元数据列表
        :return: 调用链路列表
        """
        # 实现要求：
        # 1. 识别入口点函数（如REST API接口、main函数、定时任务入口）
        # 2. 从每个入口点开始，遍历调用关系，生成调用链
        # 3. 计算调用链深度，标记核心业务链路
        # 4. 生成全局唯一link_id
        # 5. 将调用链路存入Neo4j
        # 6. 返回调用链路列表
        pass

    def reverse_architecture(
        self,
        repository: CodeRepositoryMetadata,
        files: List[CodeFileMetadata]
    ) -> dict:
        """
        逆向架构结构
        :param repository: 仓库元数据
        :param files: 文件元数据列表
        :return: 架构结构字典，包含分层架构、模块划分、依赖关系
        """
        # 实现要求：
        # 1. 基于依赖关系和目录结构，识别分层架构（如Controller→Service→DAO）
        # 2. 识别模块划分、限界上下文（基于业务域标签和依赖关系）
        # 3. 检查架构合规性，识别越界调用
        # 4. 返回架构结构字典
        pass
```

### 5.6 核心Agent-风险评估与优先级排序Agent
**文件路径**：`src/agents/scan_agents/risk_assessment_agent.py`
#### 核心类与接口定义
```python
from typing import List
from src.models.code_metadata import CodeRepositoryMetadata, CodeFileMetadata, CodeFunctionMetadata, CodeClassMetadata
from src.models.dependency import CodeDependency
from src.models.risk import CodeRisk, HotspotAnalysis, RiskType, RiskSeverity
from src.infrastructure.git.git_client import GitClient
from src.infrastructure.scanner.static_scanner import StaticScanner
from src.infrastructure.common.utils import generate_unique_id
from loguru import logger

class RiskAssessmentAgent:
    """
    风险评估与优先级排序Agent
    核心职责：识别技术债务、安全漏洞、代码坏味道、热点代码，生成风险清单，进行P0-P3风险分级
    """

    def __init__(
        self,
        git_client: GitClient,
        static_scanner: StaticScanner
    ):
        self.git_client = git_client
        self.static_scanner = static_scanner
        logger.info("RiskAssessmentAgent initialized")

    def assess_all_risks(
        self,
        repository: CodeRepositoryMetadata,
        files: List[CodeFileMetadata],
        dependencies: List[CodeDependency]
    ) -> tuple[List[CodeRisk], List[HotspotAnalysis]]:
        """
        全量风险评估
        :param repository: 仓库元数据
        :param files: 文件元数据列表
        :param dependencies: 依赖关系列表
        :return: 风险清单、热点代码分析清单
        """
        # 实现要求：
        # 1. 调用assess_technical_debt识别技术债务
        # 2. 调用assess_security_vulnerabilities识别安全漏洞
        # 3. 调用assess_code_smells识别代码坏味道
        # 4. 调用assess_circular_dependencies识别循环依赖风险
        # 5. 调用analyze_hotspots分析热点代码
        # 6. 汇总所有风险，进行P0-P3风险分级
        # 7. 返回风险清单和热点代码分析清单
        pass

    def assess_technical_debt(
        self,
        files: List[CodeFileMetadata]
    ) -> List[CodeRisk]:
        """
        识别技术债务
        :param files: 文件元数据列表
        :return: 技术债务风险清单
        """
        # 实现要求：
        # 1. 识别死代码、过长函数、上帝类、高复杂度函数
        # 2. 识别硬编码值、魔法数
        # 3. 识别低注释率文件
        # 4. 生成CodeRisk模型实例，设置风险类型、严重程度、修复建议
        # 5. 返回技术债务风险清单
        pass

    def assess_security_vulnerabilities(
        self,
        repository: CodeRepositoryMetadata,
        files: List[CodeFileMetadata]
    ) -> List[CodeRisk]:
        """
        识别安全漏洞
        :param repository: 仓库元数据
        :param files: 文件元数据列表
        :return: 安全漏洞风险清单
        """
        # 实现要求：
        # 1. 调用static_scanner进行SAST静态安全扫描
        # 2. 识别SQL注入、XSS、反序列化漏洞、硬编码密钥等安全问题
        # 3. 生成CodeRisk模型实例，设置风险类型、严重程度、修复建议
        # 4. 返回安全漏洞风险清单
        pass

    def assess_code_smells(
        self,
        files: List[CodeFileMetadata]
    ) -> List[CodeRisk]:
        """
        识别代码坏味道
        :param files: 文件元数据列表
        :return: 代码坏味道风险清单
        """
        # 实现要求：
        # 1. 基于代码元数据识别常见代码坏味道（如重复代码、过长参数列表、发散式变化）
        # 2. 调用static_scanner辅助识别
        # 3. 生成CodeRisk模型实例，设置风险类型、严重程度、修复建议
        # 4. 返回代码坏味道风险清单
        pass

    def assess_circular_dependencies(
        self,
        dependencies: List[CodeDependency]
    ) -> List[CodeRisk]:
        """
        识别循环依赖风险
        :param dependencies: 依赖关系列表
        :return: 循环依赖风险清单
        """
        # 实现要求：
        # 1. 遍历循环依赖列表
        # 2. 生成CodeRisk模型实例，设置风险类型、严重程度、修复建议
        # 3. 返回循环依赖风险清单
        pass

    def analyze_hotspots(
        self,
        repository: CodeRepositoryMetadata,
        files: List[CodeFileMetadata]
    ) -> List[HotspotAnalysis]:
        """
        分析热点代码
        :param repository: 仓库元数据
        :param files: 文件元数据列表
        :return: 热点代码分析清单
        """
        # 实现要求：
        # 1. 调用git_client获取Git提交历史
        # 2. 统计每个文件、函数的提交次数
        # 3. 结合复杂度分数，计算热点综合分数
        # 4. 生成HotspotAnalysis模型实例，设置风险等级
        # 5. 返回热点代码分析清单
        pass

    def prioritize_risks(
        self,
        risks: List[CodeRisk],
        hotspots: List[HotspotAnalysis]
    ) -> List[CodeRisk]:
        """
        风险优先级排序，更新风险的risk_level字段
        :param risks: 风险清单
        :param hotspots: 热点代码分析清单
        :return: 更新后的风险清单
        """
        # 实现要求：
        # 1. 基于风险严重程度、热点分数、业务域标签（核心业务优先）
        # 2. 将风险分为P0-P3四个等级
        # 3. 更新风险的risk_level字段
        # 4. 返回更新后的风险清单
        pass
```

---

## 六、Prompt模板库固定规范
所有Prompt模板必须放在`templates/`目录下，按模块分类，严格遵循Jinja2语法，必须包含**明确的角色定义、约束规则、输入变量、输出格式要求**，禁止自由格式的Prompt。

### 核心模板清单（AI编码工具必须逐个实现）
| 模板路径 | 用途 | 输出格式 |
|----------|------|----------|
| templates/semantic_understanding/function_semantic_extract.j2 | 函数级业务语义契约提取 | JSON格式，严格匹配BusinessSemanticContract模型 |
| templates/semantic_understanding/class_semantic_extract.j2 | 类级业务语义契约提取 | JSON格式 |
| templates/semantic_understanding/file_semantic_aggregate.j2 | 文件级业务语义契约聚合 | JSON格式 |
| templates/dependency_analysis/architecture_reverse.j2 | 架构逆向分析 | JSON格式 |
| templates/risk_assessment/technical_debt_description.j2 | 技术债务描述生成 | JSON格式 |

---

## 七、可执行开发任务清单（AI编码工具必须按顺序严格执行）
> 任务规则：每个任务完成后必须生成对应的代码文件+单元测试，通过pytest校验、ruff规范校验，才能进入下一个任务

### 阶段1：项目初始化与基础脚手架开发（预计1天完成）
| 任务ID | 任务详情 | 验收标准 |
|--------|----------|----------|
| T1-001 | 初始化Python项目，生成固定的目录结构 | 1. 严格生成3.2章节定义的目录结构；2. 所有目录包含__init__.py文件 |
| T1-002 | 编写pyproject.toml文件，添加所有依赖包，指定Python版本≥3.11 | 1. 包含2章节所有技术栈的依赖包，指定稳定版本号；2. 配置Black、Ruff、Pytest的集成；3. 可通过pip install . 完成安装 |
| T1-003 | 编写配置文件：.gitignore、ruff.toml、black.toml、pytest.ini、.env.example | 1. .gitignore符合Python项目最佳实践；2. Ruff配置严格遵循PEP8规范；3. 环境变量示例包含所有必填配置项 |
| T1-004 | 编写全局配置类config/settings.py，基于Pydantic Settings实现 | 1. 包含LLM、数据库、Git、任务队列、日志的所有配置项；2. 支持环境变量和yaml配置文件双渠道加载；3. 配置项有明确的类型注解和默认值 |
| T1-005 | 编写全局日志模块src/infrastructure/common/logger.py，基于Loguru实现 | 1. 支持控制台和文件双输出；2. 结构化日志格式，包含时间、模块、级别、消息、TraceID；3. 支持日志分级、轮转、压缩 |
| T1-006 | 编写全局异常体系src/infrastructure/common/exceptions.py | 1. 定义基类AppException，继承自Exception；2. 按模块定义细分异常类：LLMException、ParserException、DatabaseException、TaskException；3. 每个异常包含错误码、错误信息、详情字段 |
| T1-007 | 编写通用工具类src/infrastructure/common/utils.py | 1. 实现全局唯一ID生成器（雪花算法）；2. 实现Token计数工具；3. 实现文件读写工具；4. 实现异步任务工具 |

### 阶段2：基础设施层与数据模型开发（预计2天完成）
| 任务ID | 任务详情 | 验收标准 |
|--------|----------|----------|
| T2-001 | 实现4章节所有核心数据模型，包含Pydantic域模型和SQLAlchemy ORM模型 | 1. 严格遵循4章节的字段定义，无遗漏字段；2. 每个模型有明确的类型注解和校验规则；3. ORM模型与Pydantic模型可双向转换；4. 配套单元测试 |
| T2-002 | 实现LLM客户端抽象与具体实现类 | 1. 严格遵循5.1章节的接口定义；2. 实现OpenAI、Claude、DeepSeek三个模型的客户端；3. 实现重试、超时、异常处理、Token统计；4. 支持结构化输出；5. 配套单元测试 |
| T2-003 | 实现代码AST解析工具封装src/infrastructure/parser/ast_parser.py | 1. 基于Tree-sitter实现，支持Java/Python/Go/JS/TS五种语言；2. 实现AST解析、函数/类/参数/返回值/注释/调用关系提取；3. 配套单元测试 |
| T2-004 | 实现复杂度扫描工具封装src/infrastructure/scanner/complexity_scanner.py | 1. 基于Lizard实现，支持圈复杂度、认知复杂度计算；2. 支持文件、类、函数级别的复杂度计算；3. 配套单元测试 |
| T2-005 | 实现Git操作工具封装src/infrastructure/git/git_client.py | 1. 基于GitPython实现，支持仓库克隆、分支切换、提交、回滚、日志分析、diff获取；2. 支持临时目录自动清理；3. 配套单元测试 |
| T2-006 | 实现静态扫描工具封装src/infrastructure/scanner/static_scanner.py | 1. 集成SonarQube API、Semgrep；2. 支持代码质量扫描、安全漏洞扫描、代码坏味道识别；3. 配套单元测试 |
| T2-007 | 实现数据库客户端封装src/infrastructure/database/ | 1. 实现PostgreSQL连接池、Session管理；2. 实现Neo4j客户端封装，支持依赖关系的增删改查、图算法；3. 实现Milvus客户端封装，支持向量的增删改查、检索；4. 配套单元测试 |
| T2-008 | 实现任务队列封装src/infrastructure/task_queue/ | 1. 基于Celery+Redis实现，支持异步任务调度、并行执行、重试、失败回调；2. 定义扫描任务、解析任务、语义提取任务的Worker；3. 配套单元测试 |

### 阶段3：RAG索引引擎开发（预计1天完成）
| 任务ID | 任务详情 | 验收标准 |
|--------|----------|----------|
| T3-001 | 实现索引构建器src/rag/index_builder.py | 1. 支持代码语义、业务契约的向量嵌入生成；2. 支持Milvus向量索引的构建、更新、删除；3. 支持Neo4j图索引的构建、更新；4. 配套单元测试 |
| T3-002 | 实现检索器src/rag/retriever.py（为后续重构预留） | 1. 支持向量语义检索；2. 支持图数据库的依赖关系检索；3. 支持元数据的关键词检索；4. 配套单元测试 |

### 阶段4：核心Agent模块开发（预计3天完成）
| 任务ID | 任务详情 | 验收标准 |
|--------|----------|----------|
| T4-001 | 实现代码仓库接入Agent | 1. 严格遵循5.2章节的接口定义；2. 实现仓库克隆、更新、变更文件获取、模块识别；3. 配套完整的单元测试 |
| T4-002 | 实现代码解析与预处理Agent | 1. 严格遵循5.3章节的接口定义；2. 实现全量/增量文件解析、函数/类元数据提取、复杂度计算、死代码识别；3. 支持并行解析；4. 配套完整的单元测试 |
| T4-003 | 实现语义理解与业务抽象Agent | 1. 严格遵循5.4章节的接口定义；2. 实现函数级/类级/文件级业务语义契约生成；3. 实现语义向量索引构建；4. 配套完整的单元测试 |
| T4-004 | 实现依赖分析与架构逆向Agent | 1. 严格遵循5.5章节的接口定义；2. 实现依赖关系图构建、循环依赖检测、调用链路生成、架构逆向；3. 实现Neo4j图数据的写入与查询；4. 配套完整的单元测试 |
| T4-005 | 实现风险评估与优先级排序Agent | 1. 严格遵循5.6章节的接口定义；2. 实现技术债务、安全漏洞、代码坏味道、循环依赖风险识别；3. 实现热点代码分析、风险优先级排序；4. 配套完整的单元测试 |
| T4-006 | 实现业务-代码映射Agent | 1. 实现代码模块到业务域的聚类；2. 生成业务-代码映射台账；3. 配套完整的单元测试 |
| T4-007 | 实现可视化与报告生成Agent | 1. 实现代码资产全景图生成；2. 实现依赖图、调用链路图、技术债务热力图生成；3. 生成HTML/PDF格式的分析报告；4. 配套完整的单元测试 |

### 阶段5：业务服务层与API开发（预计1天完成）
| 任务ID | 任务详情 | 验收标准 |
|--------|----------|----------|
| T5-001 | 实现扫描全流程服务src/services/scan_service.py | 1. 封装扫描建模Agent集群的全流程；2. 支持全量扫描、增量扫描、模块扫描；3. 支持任务状态查询、进度跟踪、暂停、取消；4. 配套单元测试 |
| T5-002 | 实现报告生成服务src/services/report_service.py | 1. 封装报告生成Agent的能力；2. 支持代码资产报告、风险报告、依赖报告的生成；3. 配套单元测试 |
| T5-003 | 实现FastAPI应用入口src/api/app.py | 1. 实现FastAPI应用初始化；2. 集成CORS、异常处理、日志中间件；3. 注册所有API路由；4. 配套健康检查接口 |
| T5-004 | 实现所有API路由src/api/routes/ | 1. 实现仓库管理API；2. 实现扫描任务API；3. 实现代码资产查询API；4. 实现风险查询API；5. 实现报告查询API；6. 所有接口有明确的请求/响应模型、参数校验；7. 配套接口测试 |

### 阶段6：集成测试、部署配置与文档完善（预计1天完成）
| 任务ID | 任务详情 | 验收标准 |
|--------|----------|----------|
| T6-001 | 编写端到端集成测试，覆盖扫描全流程 | 1. 基于测试代码仓库（如小型开源项目），完成从克隆→解析→语义提取→依赖分析→风险评估→报告生成的全流程E2E测试；2. 测试通过率100% |
| T6-002 | 编写Docker部署相关文件 | 1. 编写Dockerfile，实现应用容器化；2. 编写docker-compose.yml，包含应用、PostgreSQL、Milvus、Neo4j、Redis；3. 编写docker-compose.env环境变量示例；4. 可通过docker compose up一键启动 |
| T6-003 | 完善README.md文档 | 1. 包含项目介绍、功能特性、技术栈、快速启动指南、API文档、部署指南、使用示例；2. 内容完整，可直接指导用户使用 |
| T6-004 | 全量代码规范校验与测试覆盖率统计 | 1. 全量代码通过Ruff、Black规范校验，无警告；2. 核心模块单元测试覆盖率≥90%；3. 所有测试用例100%通过 |

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
1.  **功能完整性**：实现本文档定义的所有Agent、接口、功能，无遗漏
2.  **代码质量**：全量代码符合编码规范，核心模块测试覆盖率≥90%，所有测试用例100%通过
3.  **性能要求**：支持100万行代码库的全量扫描，完成时间≤24小时；支持100+并行文件解析任务
4.  **准确性**：代码元数据提取完整性≥99%；业务语义契约提取准确率≥85%；循环依赖检测准确率≥95%
5.  **可部署性**：支持Docker一键部署，可在Linux环境稳定运行
6.  **可扩展性**：所有模块基于接口编程，可轻松替换LLM模型、数据库、扫描工具
7.  **可视化输出**：生成清晰的代码资产全景图、依赖图、技术债务热力图，支持HTML/PDF格式报告导出