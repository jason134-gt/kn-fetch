# 重构智能体增强方案 v3 - 闭环重构系统

## 一、总体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        重构智能体系统                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │ 知识提取  │───▶│ 问题分析  │───▶│ 方案生成  │───▶│ 报告输出  │  │
│  │  Agent   │    │  Agent   │    │  Agent   │    │  Agent   │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│                                       │                         │
│                                       ▼                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │ 差异评估  │◀───│ 测试修正  │◀───│ 自动重构  │◀───│ 用户选择  │  │
│  │  Agent   │    │  Agent   │    │  Agent   │    │  问题   │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、功能模块设计

### 2.1 报告输出模块（增强）

#### 2.1.1 报告层次结构

```
Level 0: 执行摘要（dashboard）
├── 项目健康度评分
├── 关键指标
├── TOP 问题速览
└── 快速操作入口

Level 1: 问题清单列表
├── 问题摘要表（可筛选、排序、搜索）
├── 批量选择功能
├── 问题分类统计
└── 执行计划建议

Level 2: 问题详情页
├── 一、是什么（问题描述）
│   ├── 基本信息
│   ├── 当前状态指标
│   ├── 代码上下文
│   └── 影响范围分析
├── 二、怎么修复（修复方案）
│   ├── 修复步骤
│   ├── 代码示例
│   └── 预期效果
├── 三、有什么风险（风险评估）
│   ├── 风险矩阵
│   ├── 影响范围
│   └── 回滚方案
└── 四、有什么方案（方案对比）
    ├── 多种方案列表
    ├── 方案对比表
    └── 推荐方案及理由

Level 3: 代码级详情
├── 原始代码展示
├── 问题标注
├── 修复预览
└── 相关实体
```

#### 2.1.2 用户交互功能

```markdown
## 问题详情页交互设计

### 按钮操作区

┌─────────────────────────────────────────────────────────────┐
│  [选择此问题]  [查看代码]  [自定义方案]  [重新分析]  [反馈]  │
└─────────────────────────────────────────────────────────────┘

### 选择模式

- [x] 单选：选择当前问题进行重构
- [ ] 批量：选择多个问题批量处理
- [ ] 依赖联动：自动选择关联问题

### 方案定制

用户可自定义：
- 修改推荐方案
- 调整修复参数
- 添加额外约束
- 指定测试要求
```

---

### 2.2 自动重构模块

#### 2.2.1 重构流程

```
用户选择问题
    │
    ▼
┌─────────────────┐
│ 1. 预检查        │
│   - 依赖分析     │
│   - 影响评估     │
│   - 风险确认     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. 备份代码      │
│   - Git stash   │
│   - 创建分支     │
│   - 记录快照     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 3. 执行重构      │
│   - 应用模板     │
│   - 代码生成     │
│   - 格式化       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 4. 自动测试      │
│   - 单元测试     │
│   - 集成测试     │
│   - 回归测试     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 5. 结果验证      │
│   - 编译检查     │
│   - 静态分析     │
│   - 功能验证     │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
  成功       失败
    │         │
    ▼         ▼
┌────────┐ ┌─────────┐
│ 差异评估│ │ 自动修正 │
└────────┘ └─────────┘
```

#### 2.2.2 重构类型实现

| 重构类型 | 实现方式 | 自动化程度 |
|----------|----------|------------|
| 添加文档 | 模板生成 | 100%自动 |
| 提取方法 | AST重构 | 90%自动 |
| 重命名 | 符号重构 | 100%自动 |
| 拆分文件 | 手动+辅助 | 50%自动 |
| 代码格式化 | 格式化工具 | 100%自动 |
| 移除死代码 | 分析+确认 | 80%自动 |

---

### 2.3 测试修正模块

#### 2.3.1 测试策略

```python
class RefactoringTestStrategy:
    """重构测试策略"""
    
    def execute(self, refactoring_result):
        # 1. 编译检查
        if not self.compile_check():
            return TestResult.COMPILE_ERROR
        
        # 2. 单元测试
        unit_result = self.run_unit_tests()
        
        # 3. 集成测试
        integration_result = self.run_integration_tests()
        
        # 4. 回归测试
        regression_result = self.run_regression_tests()
        
        # 5. 覆盖率检查
        coverage = self.check_coverage()
        
        return TestResult(
            unit_tests=unit_result,
            integration_tests=integration_result,
            regression_tests=regression_result,
            coverage=coverage
        )
```

#### 2.3.2 自动修正策略

```
测试失败类型          修正策略
─────────────────────────────────────
编译错误         →   语法修正、导入修复
类型错误         →   类型推断、类型转换
运行时错误       →   异常处理、边界检查
逻辑错误         →   回滚+人工干预
性能退化         →   优化建议
测试失败         →   LLM辅助修复
```

---

### 2.4 差异评估模块

#### 2.4.1 评估维度

```markdown
## 差异评估报告

### 1. 代码变更统计

| 指标 | 变更前 | 变更后 | 差异 |
|------|--------|--------|------|
| 文件数 | 15 | 18 | +3 |
| 总行数 | 2500 | 2450 | -50 |
| 类数量 | 8 | 10 | +2 |
| 函数数量 | 45 | 52 | +7 |
| 平均复杂度 | 18 | 8 | -10 |
| 文档覆盖率 | 30% | 85% | +55% |

### 2. 质量指标对比

| 指标 | 变更前 | 变更后 | 改进 |
|------|--------|--------|------|
| 代码重复率 | 15% | 8% | ✅ -7% |
| 测试覆盖率 | 45% | 72% | ✅ +27% |
| 技术债务数 | 25 | 12 | ✅ -13 |
| 安全问题 | 3 | 0 | ✅ -3 |

### 3. 功能验证结果

| 测试类型 | 通过率 | 失败数 | 状态 |
|----------|--------|--------|------|
| 单元测试 | 98% | 2/100 | ⚠️ |
| 集成测试 | 100% | 0/20 | ✅ |
| 回归测试 | 100% | 0/50 | ✅ |
| 性能测试 | 100% | 0/10 | ✅ |

### 4. 风险评估

| 风险项 | 等级 | 说明 |
|--------|------|------|
| 功能回归 | 低 | 所有核心功能测试通过 |
| 性能退化 | 低 | 性能测试无退化 |
| 兼容性 | 中 | 需要更新部分调用方 |
| 维护性 | 改善 | 代码结构更清晰 |

### 5. 后续建议

- [ ] 处理2个失败的单元测试
- [ ] 更新API文档
- [ ] 通知调用方接口变更
```

---

## 三、数据模型设计

### 3.1 核心数据模型

```python
# src/refactor/models/refactoring_session.py

from pydantic import BaseModel, Field
from typing import List, Dict, Optional
from enum import Enum
from datetime import datetime

class SessionStatus(str, Enum):
    """会话状态"""
    CREATED = "created"
    ANALYZING = "analyzing"
    REVIEWING = "reviewing"
    REFACTORING = "refactoring"
    TESTING = "testing"
    COMPLETED = "completed"
    FAILED = "failed"
    ROLLED_BACK = "rolled_back"

class RefactoringSession(BaseModel):
    """重构会话"""
    session_id: str
    project_name: str
    status: SessionStatus
    created_at: datetime
    
    # 选中的问题
    selected_problems: List[str] = []
    
    # 自定义配置
    custom_config: Dict = {}
    
    # 执行结果
    refactoring_steps: List[Dict] = []
    test_results: Optional[Dict] = None
    diff_report: Optional[Dict] = None
    
    # 回滚信息
    backup_branch: Optional[str] = None
    can_rollback: bool = True

class UserFeedback(BaseModel):
    """用户反馈"""
    feedback_id: str
    session_id: str
    problem_id: str
    
    # 反馈类型
    feedback_type: str  # approve, reject, modify, comment
    feedback_content: str
    
    # 用户自定义方案
    custom_solution: Optional[Dict] = None
    
    created_at: datetime

class DiffReport(BaseModel):
    """差异评估报告"""
    report_id: str
    session_id: str
    generated_at: datetime
    
    # 代码变更统计
    code_stats_before: Dict
    code_stats_after: Dict
    code_diff: Dict
    
    # 质量指标
    quality_before: Dict
    quality_after: Dict
    quality_improvement: Dict
    
    # 测试结果
    test_results: Dict
    
    # 风险评估
    risks: List[Dict]
    
    # 建议
    recommendations: List[str]
```

### 3.2 API接口设计

```python
# src/refactor/api/refactoring_api.py

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional

router = APIRouter(prefix="/api/refactor", tags=["refactoring"])

@router.post("/sessions")
async def create_session(project_name: str):
    """创建重构会话"""
    pass

@router.get("/sessions/{session_id}")
async def get_session(session_id: str):
    """获取会话状态"""
    pass

@router.get("/reports/{session_id}/problems")
async def get_problems(
    session_id: str,
    severity: Optional[str] = None,
    type: Optional[str] = None,
    page: int = 1,
    size: int = 20
):
    """获取问题列表（支持筛选和分页）"""
    pass

@router.get("/reports/{session_id}/problems/{problem_id}")
async def get_problem_detail(session_id: str, problem_id: str):
    """获取问题详情（Level 2）"""
    pass

@router.get("/reports/{session_id}/problems/{problem_id}/code")
async def get_problem_code(
    session_id: str, 
    problem_id: str,
    view: str = "full"  # full, diff, context
):
    """获取问题相关代码（Level 3）"""
    pass

@router.post("/sessions/{session_id}/select")
async def select_problems(
    session_id: str,
    problem_ids: List[str],
    mode: str = "single"  # single, batch, dependency
):
    """选择要重构的问题"""
    pass

@router.post("/sessions/{session_id}/customize")
async def customize_solution(
    session_id: str,
    problem_id: str,
    custom_config: Dict
):
    """自定义重构方案"""
    pass

@router.post("/sessions/{session_id}/feedback")
async def submit_feedback(
    session_id: str,
    problem_id: str,
    feedback_type: str,
    feedback_content: str,
    custom_solution: Optional[Dict] = None
):
    """提交用户反馈"""
    pass

@router.post("/sessions/{session_id}/regenerate")
async def regenerate_report(
    session_id: str,
    based_on_feedback: bool = True
):
    """基于用户反馈重新生成报告"""
    pass

@router.post("/sessions/{session_id}/execute")
async def execute_refactoring(
    session_id: str,
    dry_run: bool = False
):
    """执行重构"""
    pass

@router.get("/sessions/{session_id}/diff")
async def get_diff_report(session_id: str):
    """获取差异评估报告"""
    pass

@router.post("/sessions/{session_id}/rollback")
async def rollback(session_id: str):
    """回滚重构"""
    pass
```

---

## 四、用户交互流程

### 4.1 标准流程

```
┌─────────────────────────────────────────────────────────────────┐
│ 用户交互流程                                                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 查看报告                                                                 │
│     └── 浏览问题清单 → 筛选/搜索 → 点击查看详情                                     │
│                                                                 │
│  2. 选择问题                                                                 │
│     └── 单选/批量选择 → 查看影响范围 → 确认选择                                     │
│                                                                 │
│  3. 定制方案（可选）                                                            │
│     └── 修改推荐方案 → 调整参数 → 保存配置                                        │
│                                                                 │
│  4. 提交反馈（可选）                                                            │
│     └── 反馈问题 → 提供建议 → 重新生成报告                                        │
│                                                                 │
│  5. 执行重构                                                                 │
│     └── 预览变更 → 确认执行 → 等待完成                                           │
│                                                                 │
│  6. 查看结果                                                                 │
│     └── 测试结果 → 差异评估 → 决定接受/回滚                                       │
│                                                                 │
│  7. 后续处理                                                                 │
│     └── 处理失败项 → 更新文档 → 提交代码                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 报告下钻流程

```
Level 0: Dashboard
│
│  点击"查看详情"
│
▼
Level 1: 问题清单
│   [筛选] [排序] [搜索]
│   ┌────────────────────────────────────────┐
│   │ [ ] P001 大文件 Counter.java 高      │
│   │ [ ] P002 大文件 RealRuleParse.java 高 │
│   │ [ ] P003 缺少文档 BaseAction.java 低  │
│   └────────────────────────────────────────┘
│   [选择选中项] [导出报告]
│
│  点击问题行
│
▼
Level 2: 问题详情
│   ┌────────────────────────────────────────┐
│   │ P001: Counter                          │
│   │                                        │
│   │ 一、是什么                              │
│   │    文件过大，共682行...                 │
│   │                                        │
│   │ 二、怎么修复                            │
│   │    步骤1: 分析文件结构...              │
│   │                                        │
│   │ 三、有什么风险                          │
│   │    依赖关系变化...                     │
│   │                                        │
│   │ 四、有什么方案                          │
│   │    方案对比表...                       │
│   └────────────────────────────────────────┘
│   [选择此问题] [查看代码] [自定义方案] [反馈]
│
│  点击"查看代码"
│
▼
Level 3: 代码详情
    ┌────────────────────────────────────────┐
    │ 文件: Counter.java                      │
    │ 行号: 1-682                             │
    │                                         │
    │ 001: public class Counter {            │
    │ 002:     // 问题区域高亮显示            │
    │ 003:     private int count;            │
    │ ...                                     │
    │                                         │
    │ [原始视图] [问题标注] [修复预览]         │
    └────────────────────────────────────────┘
    [返回详情]
```

---

## 五、实现文件结构

```
src/refactor/
├── core/
│   ├── problem_detail.py              # 问题详情模型
│   ├── enhanced_report_generator.py   # 增强报告生成器
│   ├── refactoring_session.py         # 会话管理
│   └── diff_evaluator.py              # 差异评估器
│
├── executor/
│   ├── refactoring_executor.py        # 重构执行器
│   ├── code_backup.py                 # 代码备份
│   ├── ast_transformer.py             # AST转换器
│   └── template_applier.py            # 模板应用器
│
├── tester/
│   ├── test_runner.py                 # 测试运行器
│   ├── auto_fixer.py                  # 自动修正器
│   └── coverage_analyzer.py           # 覆盖率分析
│
├── api/
│   ├── refactoring_api.py             # REST API
│   └── websocket_handler.py           # WebSocket推送
│
├── templates/
│   ├── add_docstring.py               # 添加文档模板
│   ├── extract_method.py              # 提取方法模板
│   ├── rename_symbol.py               # 重命名模板
│   └── split_file.py                  # 拆分文件模板
│
└── web/
    ├── static/
    │   ├── js/
    │   │   ├── report.js              # 报告交互
    │   │   ├── problem_selector.js    # 问题选择器
    │   │   └── diff_viewer.js         # 差异查看器
    │   └── css/
    │       └── report.css
    └── templates/
        ├── dashboard.html             # Level 0
        ├── problem_list.html          # Level 1
        ├── problem_detail.html        # Level 2
        └── code_view.html             # Level 3
```

---

## 六、实现步骤

| 阶段 | 内容 | 预计时间 |
|------|------|----------|
| **Phase 1** | 报告下钻功能 | 2天 |
| - | Level 0-3 页面实现 | 1天 |
| - | API接口实现 | 0.5天 |
| - | 前端交互实现 | 0.5天 |
| **Phase 2** | 问题选择与反馈 | 1天 |
| - | 选择功能实现 | 0.5天 |
| - | 反馈收集与处理 | 0.5天 |
| **Phase 3** | 自动重构执行 | 3天 |
| - | 执行器框架 | 1天 |
| - | 重构模板实现 | 1天 |
| - | 代码备份与回滚 | 1天 |
| **Phase 4** | 测试与修正 | 2天 |
| - | 测试运行器 | 1天 |
| - | 自动修正器 | 1天 |
| **Phase 5** | 差异评估 | 1天 |
| **Phase 6** | 集成测试 | 1天 |
| **总计** | | **10天** |

---

## 七、示例交互

### 用户选择问题并执行重构

```bash
# 1. 查看问题列表
GET /api/refactor/sessions/s001/problems?severity=high

# 2. 选择问题
POST /api/refactor/sessions/s001/select
{
  "problem_ids": ["P001", "P002"],
  "mode": "batch"
}

# 3. 执行重构（预览模式）
POST /api/refactor/sessions/s001/execute?dry_run=true

# 4. 确认执行
POST /api/refactor/sessions/s001/execute

# 5. 查看差异评估
GET /api/refactor/sessions/s001/diff

# 6. 如需要，回滚
POST /api/refactor/sessions/s001/rollback
```

### 用户反馈并重新生成报告

```bash
# 1. 提交反馈
POST /api/refactor/sessions/s001/feedback
{
  "problem_id": "P001",
  "feedback_type": "modify",
  "feedback_content": "希望保留原有命名规范",
  "custom_solution": {
    "naming_convention": "camelCase"
  }
}

# 2. 重新生成报告
POST /api/refactor/sessions/s001/regenerate?based_on_feedback=true
```
