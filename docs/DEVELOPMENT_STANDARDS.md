# 开发规范文档

本文档定义了项目的目录结构、模块划分、代码组织等开发规范。

## 1. 目录结构规范

### 1.1 顶层目录结构

```
kn-fetch/
├── src/                    # 源码目录
├── tests/                  # 测试目录
├── scripts/                # 脚本目录
├── output/                 # 输出目录
├── docs/                   # 文档目录
├── config/                 # 配置目录
└── [入口脚本]              # 根目录入口脚本
```

### 1.2 源码目录结构 (src/)

```
src/
├── extract/                # 知识提取模块
│   ├── core/               # 核心组件
│   ├── agents/             # Agent组件
│   ├── ai/                 # AI服务
│   ├── gitnexus/           # GitNexus模块
│   ├── infrastructure/     # 基础设施
│   └── models/             # 数据模型
│
├── refactor/               # 重构分析模块
│   ├── core/               # 核心组件
│   └── agent/              # Agent组件
│
├── api/                    # API接口
├── cli/                    # 命令行工具
├── integrations/           # 第三方集成
└── web/                    # Web服务
```

### 1.3 测试目录结构 (tests/)

```
tests/
├── extract/                # 知识提取测试
├── refactor/               # 重构分析测试
├── example/                # 示例项目
├── samples/                # 测试样本
└── conftest.py             # pytest配置
```

### 1.4 脚本目录结构 (scripts/)

```
scripts/
├── check/                  # 环境检查脚本
├── diagnose/               # 问题诊断脚本
├── verify/                 # 格式验证脚本
├── analyze/                # 分析工具脚本
├── debug/                  # 调试辅助脚本
├── demo/                   # 功能演示脚本
└── tools/                  # 实用工具脚本
```

### 1.5 输出目录结构 (output/)

```
output/
├── extract/                # 知识提取输出
│   ├── doc/                # 文档输出
│   │   └── {project_name}/
│   │       ├── entities/   # 实体文档
│   │       ├── modules/    # 模块文档
│   │       ├── api/        # API文档
│   │       ├── uml/        # UML图
│   │       ├── design/     # 设计文档
│   │       ├── business/   # 业务文档
│   │       └── architecture/ # 架构文档
│   └── knowledge_graph/    # 知识图谱
│       └── {project_name}.json
│
└── refactor/               # 重构分析输出
    └── {project_name}/
        ├── data/           # JSON数据
        └── report.md       # 分析报告
```

---

## 2. 模块划分规范

### 2.1 知识提取模块 (src/extract/)

**职责**: 代码解析、知识提取、文档生成

**核心组件**:
- `KnowledgeExtractor`: 知识提取主引擎
- `KnowledgeDocumentGenerator`: 文档生成器
- `UMLGenerator`: UML图生成器
- `DeepKnowledgeAnalyzer`: LLM深度分析

**导入方式**:
```python
from src.extract import KnowledgeExtractor, KnowledgeDocumentGenerator
from src.extract.agents.base_agent import BaseAgent
from src.extract.ai.llm_client import LLMClient
```

### 2.2 重构分析模块 (src/refactor/)

**职责**: 风险评估、技术债务扫描、重构方案生成

**核心组件**:
- `RefactoringOrchestrator`: 重构分析编排器
- `KnowledgeLoader`: 知识加载器
- `ContextAssembler`: 上下文组装器

**导入方式**:
```python
from src.refactor import RefactoringOrchestrator, KnowledgeLoader
from src.refactor.core.refactoring_task import RiskLevel, TechnicalDebt
```

### 2.3 模块依赖原则

```
extract模块: 无外部模块依赖
refactor模块: 依赖extract模块的输出
api/cli/web: 依赖extract和refactor模块
```

---

## 3. 入口脚本规范

### 3.1 根目录入口脚本

根目录只保留核心入口脚本：

| 脚本 | 用途 |
|------|------|
| `kn-fetch.py` | 主入口程序 |
| `run_extract.py` | 知识提取入口 |
| `run_refactor.py` | 重构分析入口 |
| `start_web.py` | Web服务入口 |

### 3.2 入口脚本模板

```python
#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
模块入口脚本

功能说明...
"""

import os
import sys
from pathlib import Path

# 设置控制台编码
os.environ['PYTHONIOENCODING'] = 'utf-8'

# 添加项目根目录
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

# 导入模块
from src.{module} import {Component}

def main():
    """主入口"""
    # 配置路径
    config = {...}
    
    # 执行逻辑
    ...

if __name__ == "__main__":
    sys.exit(main())
```

---

## 4. 脚本分类规范

### 4.1 环境检查 (scripts/check/)

用于检查环境配置、依赖安装等。

**命名规范**: `check_*.py`

**示例**:
- `check_env.py` - 环境变量检查
- `check_relationships.py` - 关系检查

### 4.2 问题诊断 (scripts/diagnose/)

用于诊断特定问题、排查错误。

**命名规范**: `diagnose_*.py`

**示例**:
- `diagnose_java_analysis.py` - Java分析诊断
- `diagnose_knowledge_extraction.py` - 知识提取诊断

### 4.3 格式验证 (scripts/verify/)

用于验证文件格式、代码规范。

**命名规范**: `verify_*.py`

**示例**:
- `verify_skill_format.py` - Skill格式验证

### 4.4 分析工具 (scripts/analyze/)

用于代码分析、结构分析。

**命名规范**: `analyze_*.py`

**示例**:
- `analyze_call_chains.py` - 调用链分析
- `analyze_call_structure.py` - 调用结构分析

### 4.5 调试辅助 (scripts/debug/)

用于开发调试、问题定位。

**命名规范**: `debug_*.py`

**示例**:
- `debug_java_parser.py` - Java解析器调试
- `debug_parser.py` - 解析器调试

### 4.6 功能演示 (scripts/demo/)

用于功能演示、示例运行。

**命名规范**: `demo_*.py` 或 `run_*.py`

**示例**:
- `demo_stock_analysis.py` - 股票分析演示
- `demo_phase3_phase4.py` - 阶段演示

### 4.7 实用工具 (scripts/tools/)

用于生成文档、数据处理等工具。

**命名规范**: 动词开头，如 `regenerate_*.py`, `view_*.py`

**示例**:
- `regenerate_all_docs.py` - 重新生成文档
- `regenerate_uml.py` - 重新生成UML
- `view_relationships.py` - 查看关系

---

## 5. 测试组织规范

### 5.1 测试文件命名

- 单元测试: `test_{module}_{function}.py`
- 集成测试: `test_{feature}_integration.py`
- 端到端测试: `test_{scenario}_e2e.py`

### 5.2 测试文件放置

```
tests/
├── extract/                # 知识提取相关测试
│   ├── test_knowledge_extractor.py
│   ├── test_document_generator.py
│   └── ...
├── refactor/               # 重构分析相关测试
│   ├── test_refactoring_orchestrator.py
│   └── ...
└── conftest.py             # 共享fixture
```

### 5.3 测试运行

```bash
# 运行所有测试
pytest tests/

# 运行extract模块测试
pytest tests/extract/

# 运行refactor模块测试
pytest tests/refactor/

# 运行单个测试文件
pytest tests/extract/test_knowledge_extractor.py
```

---

## 6. 导入路径规范

### 6.1 模块内部导入

使用相对导入：

```python
# 同级目录
from .module import Class

# 上级目录
from ..module import Class

# 下级目录
from .subpackage.module import Class
```

### 6.2 跨模块导入

使用绝对导入：

```python
# 从extract模块导入
from src.extract.core.knowledge_extractor import KnowledgeExtractor

# 从refactor模块导入
from src.refactor.core.refactoring_orchestrator import RefactoringOrchestrator
```

### 6.3 禁止的导入方式

```python
# 禁止：直接从src导入（应从具体模块导入）
from src import KnowledgeExtractor  # ❌

# 正确：从具体模块导入
from src.extract import KnowledgeExtractor  # ✅
```

---

## 7. 配置文件规范

### 7.1 配置文件位置

```
config/
├── config.yaml             # 主配置文件
├── ai_config.yaml          # AI配置
└── analysis_config.yaml    # 分析配置
```

### 7.2 配置加载方式

```python
import yaml
from pathlib import Path

def load_config(config_path: str = "config/config.yaml") -> dict:
    """加载配置文件"""
    config_file = Path(__file__).parent.parent / config_path
    with open(config_file, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)
```

---

## 8. 输出文件规范

### 8.1 知识提取输出

```
output/extract/
├── doc/{project_name}/
│   ├── index.md                    # 索引文件
│   ├── entities/                   # 实体文档
│   ├── modules/                    # 模块文档
│   ├── api/                        # API文档
│   ├── uml/                        # UML图
│   ├── design/                     # 设计文档
│   │   ├── project_overview.md
│   │   ├── feature_modules.md
│   │   └── api_reference.md
│   ├── business/                   # 业务文档
│   │   └── business_flow.md
│   └── architecture/               # 架构文档
│       ├── overview.md
│       └── architecture_design.md
└── knowledge_graph/
    └── {project_name}.json         # 知识图谱
```

### 8.2 重构分析输出

```
output/refactor/{project_name}/
├── data/
│   └── report.json                 # JSON格式报告
└── report.md                       # Markdown格式报告
```

---

## 9. 版本控制规范

### 9.1 忽略文件 (.gitignore)

```gitignore
# 输出目录
output/

# 缓存
__pycache__/
*.pyc
.pytest_cache/

# 环境配置
.env
*.egg-info/

# IDE
.vscode/
.idea/
```

### 9.2 提交规范

提交信息格式：
```
<type>(<scope>): <subject>

<body>
```

类型(type):
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

---

## 10. 代码风格规范

### 10.1 代码格式化

使用 `black` 和 `ruff` 进行代码格式化：

```bash
# 格式化代码
black src/ tests/

# 检查代码风格
ruff check src/ tests/
```

### 10.2 类型注解

推荐使用类型注解：

```python
from typing import Dict, List, Optional

def process_data(
    data: Dict[str, Any],
    options: Optional[List[str]] = None
) -> Dict[str, Any]:
    """处理数据"""
    ...
```

### 10.3 文档字符串

使用Google风格的文档字符串：

```python
def function_name(param1: str, param2: int) -> bool:
    """函数简要描述。

    更详细的描述...

    Args:
        param1: 参数1说明
        param2: 参数2说明

    Returns:
        返回值说明

    Raises:
        ValueError: 异常说明
    """
    ...
```

---

## 附录：快速参考

### 常用命令

```bash
# 知识提取
python run_extract.py

# 重构分析
python run_refactor.py

# 运行测试
pytest tests/

# 环境检查
python scripts/check/check_env.py

# 重新生成文档
python scripts/tools/regenerate_all_docs.py
```

### 目录快速查找

| 内容 | 路径 |
|------|------|
| 知识提取源码 | `src/extract/` |
| 重构分析源码 | `src/refactor/` |
| 知识提取测试 | `tests/extract/` |
| 重构分析测试 | `tests/refactor/` |
| 知识提取输出 | `output/extract/` |
| 重构分析输出 | `output/refactor/` |
| 检查脚本 | `scripts/check/` |
| 诊断脚本 | `scripts/diagnose/` |
| 工具脚本 | `scripts/tools/` |

---

## 11. 用户反馈闭环规范

### 11.1 反馈类型

用户可以对重构报告提出以下类型的反馈：

| 反馈类型 | 说明 | 使用场景 |
|----------|------|----------|
| `agree` | 同意该问题 | 用户确认问题是真实存在的 |
| `disagree` | 不同意该问题 | 用户认为误报，应移除 |
| `modify` | 需要修改问题 | 调整问题描述、优先级等 |
| `accept_solution` | 接受方案 | 用户认可推荐方案 |
| `modify_solution` | 修改方案 | 调整方案内容 |
| `add_solution` | 添加新方案 | 用户提出自己的解决方案 |
| `raise_priority` | 提高优先级 | 用户认为问题更紧急 |
| `lower_priority` | 降低优先级 | 用户认为问题不紧急 |
| `add_problem` | 添加新问题 | 用户发现遗漏的问题 |
| `ignore_problem` | 忽略问题 | 暂不处理该问题 |

### 11.2 反馈数据结构

```python
from src.refactor.core.feedback_processor import (
    UserFeedback, FeedbackBatch, FeedbackType, create_feedback
)

# 创建单个反馈
feedback = create_feedback(
    session_id="session-001",
    problem_id="P001",
    feedback_type=FeedbackType.MODIFY,
    content="拆分时保留原有命名规范",
    suggested_priority="medium"
)

# 创建反馈批次
batch = FeedbackBatch(session_id="session-001")
batch.add_feedback(feedback)
```

### 11.3 报告重生成

```python
from src.refactor.core.report_regenerator import ReportRegenerator

# 根据反馈重新生成报告
regenerator = ReportRegenerator()
new_report = regenerator.regenerate(original_report, feedback_batch)

# 新报告包含：
# - version: 版本号+1
# - parent_report_id: 父报告ID
# - feedback_batch_id: 关联的反馈批次
# - change_log: 变更日志
```

### 11.4 迭代流程

```
生成报告 → 用户审核 → 提交反馈 → 重生成报告 → 用户审核 → ...
```

**迭代限制**:
- 最大迭代次数: 5次
- 每次迭代最大反馈数: 50条

### 11.5 相关文档

详细的反馈闭环设计请参考: `docs/refactor/feedback-loop-design.md`
