# 开发指南

## 目录
- [1. 环境搭建](#1-环境搭建)
- [2. 项目结构](#2-项目结构)
- [3. 开发规范](#3-开发规范)
- [4. 测试指南](#4-测试指南)
- [5. 调试技巧](#5-调试技巧)
- [6. 发布流程](#6-发布流程)

---

## 1. 环境搭建

### 1.1 系统要求

- Python 3.7 或更高版本
- Git (用于版本控制)
- 至少 8GB 内存
- Windows/macOS/Linux

### 1.2 安装步骤

```bash
# 1. 克隆仓库
git clone https://github.com/your-repo/kn-fetch.git
cd kn-fetch

# 2. 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows

# 3. 安装依赖
pip install -r requirements.txt

# 4. 配置 AI 服务
# 编辑 config/config.yaml，填入 API Key
# 或设置环境变量：
# export ARK_API_KEY="your-key"

# 5. 验证安装
python -m pytest tests/ -v
```

---

## 2. 项目结构

```
kn-fetch/
├── config/              # 配置文件
│   └── config.yaml
├── docs/                # 文档
│   ├── ARCHITECTURE.md
│   ├── API.md
│   └── ...
├── src/                 # 源代码
│   ├── ai/             # AI 模块
│   │   ├── langchain_*.py
│   │   └── ...
│   ├── cli/            # 命令行接口
│   ├── core/           # 核心功能
│   ├── gitnexus/       # 知识图谱引擎
│   ├── integrations/    # 集成模块
│   └── web/            # Web 界面
├── tests/               # 测试代码
│   ├── conftest.py
│   └── test_*.py
├── output/              # 输出目录
├── requirements.txt     # 依赖列表
├── pytest.ini          # 测试配置
├── kn-fetch.py         # 主入口
├── start_web.py        # Web 服务启动
└── README.md          # 项目说明
```

---

## 3. 开发规范

### 3.1 设计先行原则 ⚠️

**重要：所有新功能开发必须遵循"先设计后开发"流程！**

#### 开发流程

```
需求分析 → 设计方案文档 → 设计评审 → 编码实现 → 测试验证 → 文档更新
```

#### 设计方案文档要求

任何新功能或重大重构，必须先创建设计方案文档（`docs/DESIGN_*.md`），内容包含：

| 章节 | 必填 | 说明 |
|------|------|------|
| 目标与输出 | ✅ | 核心目标、预期输出物 |
| 架构设计 | ✅ | 模块划分、数据流、技术选型 |
| 数据结构 | ✅ | 关键类/接口定义、数据库表结构 |
| 工作流程 | ✅ | 核心流程图、时序图 |
| 性能考虑 | ⭕ | 性能优化策略（如涉及） |
| 风险评估 | ⭕ | 潜在风险与应对方案 |

#### 设计方案文档命名规范

```
docs/DESIGN_<功能名称>.md

示例：
docs/DESIGN_CODE_ASSET_AGENT.md    # 代码资产扫描智能体
docs/DESIGN_REFACTORING_AGENT.md   # 重构智能体
docs/DESIGN_VECTOR_STORE.md        # 向量存储模块
```

#### 设计文档模板

```markdown
# <功能名称>设计方案

> 文档版本：v1.0
> 创建日期：YYYY-MM-DD
> 状态：设计中/已评审/已实现

## 一、核心目标与输出定义
...

## 二、架构设计
...

## 三、数据结构设计
...

## 四、工作流程
...

## 五、相关文件
| 文件 | 说明 |
|------|------|
| src/xxx.py | 实现文件 |

## 六、后续迭代计划
...
```

#### 已有设计方案文档

| 文档 | 说明 | 状态 |
|------|------|------|
| [DESIGN_CODE_ASSET_AGENT.md](./DESIGN_CODE_ASSET_AGENT.md) | 全量代码资产AI扫描与语义建模智能体 | 已实现 |

---

### 3.2 代码风格

- 遵循 PEP 8 规范
- 使用 4 空格缩进
- 每行最大 88 字符
- 使用类型注解

### 3.3 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 模块 | 小写 + 下划线 | `knowledge_extractor.py` |
| 类 | 大驼峰 | `KnowledgeExtractor` |
| 函数/方法 | 小写 + 下划线 | `extract_entities()` |
| 常量 | 大写 + 下划线 | `MAX_RETRIES` |
| 私有成员 | 前缀下划线 | `_internal_method()` |

### 3.4 注释规范

#### 模块 Docstring
```python
"""
模块简短描述

详细描述（可选）
"""

__all__ = ["ExportedClass", "exported_function"]
```

#### 类 Docstring
```python
class KnowledgeExtractor:
    """
    类的简短描述
    
    详细说明：
    - 功能1
    - 功能2
    
    Attributes:
        attr1: 属性说明
    """
    
    def __init__(self, param1: str):
        """初始化方法"""
        self.attr1 = param1
```

#### 函数 Docstring
```python
def extract_from_directory(
    directory_path: str,
    include_code: bool = True
) -> KnowledgeGraph:
    """
    函数简短描述
    
    Args:
        directory_path: 参数说明
        include_code: 参数说明
        
    Returns:
        返回值说明
        
    Raises:
        ValueError: 异常说明
    """
    pass
```

### 3.5 Git 提交规范

提交信息格式：
```
<type>(<scope>): <subject>

<body>

<footer>
```

Type 类型：
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

示例：
```
feat(ai): 添加 LangChain 支持

- 新增 LangChain LLM 客户端
- 新增提示词模板管理
- 支持多个 LLM 提供商

Closes #123
```

---

## 4. 测试指南

### 4.1 运行测试

```bash
# 运行所有测试
python -m pytest tests/ -v

# 运行特定测试文件
python -m pytest tests/test_app_basic.py -v

# 运行特定测试
python -m pytest tests/test_app_basic.py::test_status -v

# 查看覆盖率
python -m pytest tests/ --cov=src --cov-report=html
```

### 4.2 编写测试

```python
import pytest
from src.ai import LangChainLLMClient

def test_llm_client_creation():
    """测试 LLM 客户端创建"""
    config = {
        "provider": "volcengine",
        "volcengine": {"api_key": "test-key"}
    }
    client = LangChainLLMClient(config)
    assert client is not None

@pytest.mark.asyncio
async def test_llm_chat():
    """测试 LLM 聊天功能"""
    client = LangChainLLMClient(config)
    # 使用 mock 避免实际调用
    result = await client.chat("你是一个专家", "测试")
    assert "test" in result
```

---

## 5. 调试技巧

### 5.1 启用调试日志

```python
import logging

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)
```

### 5.2 使用 Python 调试器

```bash
# 使用 pdb
python -m pdb kn-fetch.py analyze ./src

# 使用 ipdb（更强大）
pip install ipdb
python -m ipdb kn-fetch.py analyze ./src
```

---

## 6. 发布流程

### 6.1 版本号管理

遵循语义化版本：`MAJOR.MINOR.PATCH`

- `MAJOR`: 不兼容的 API 变更
- `MINOR`: 向后兼容的功能新增
- `PATCH`: 向后兼容的 Bug 修复

### 6.2 发布步骤

```bash
# 1. 更新版本号
# 修改 setup.py 或 __init__.py 中的版本号

# 2. 更新 CHANGELOG.md
# 记录本次更新的内容

# 3. 创建 Git 标签
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# 4. 发布到 PyPI（可选）
python setup.py sdist bdist_wheel
twine upload dist/*
```

---

## 更多信息

- [API 文档](./API.md)
- [架构文档](./ARCHITECTURE.md)
- [贡献指南](./CONTRIBUTING.md)
