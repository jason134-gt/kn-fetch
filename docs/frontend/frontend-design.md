# 前端管理功能详细设计文档

> **文档定位**：基于已实现的后端功能，设计完整的前端管理系统
> **技术栈**：React 18 + TypeScript 5 + Ant Design 5.20+ + Zustand 4.5+ + React Router v6.22+ + Axios 1.7+ + ECharts 5.5+
> **目标用户**：架构师、资深研发、重构负责人

---

## 一、系统架构概览

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           前端管理系统                                    │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      展示层 (Presentation)                        │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │   │
│  │  │Dashboard│ │知识提取  │ │重构分析  │ │反馈闭环  │ │系统设置 │   │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                   │                                      │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      状态管理层 (State)                           │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐    │   │
│  │  │dashboardStore│ │extractStore│ │refactorStore│ │feedbackStore│    │   │
│  │  └────────────┘ └────────────┘ └────────────┘ └────────────┘    │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                   │                                      │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      服务层 (Service)                             │   │
│  │  ┌────────────────────────────────────────────────────────────┐  │   │
│  │  │                    Axios API 封装                           │  │   │
│  │  └────────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                   │                                      │
└───────────────────────────────────┼──────────────────────────────────────┘
                                    │ HTTP/REST
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           后端服务 API                                   │
├─────────────────────────────────────────────────────────────────────────┤
│  /api/v1/extract/*     - 知识提取API                                    │
│  /api/v1/refactor/*    - 重构分析API                                    │
│  /api/v1/feedback/*    - 反馈闭环API                                    │
│  /api/v1/review/*      - 审核工作流API                                  │
│  /api/v1/report/*      - 报告管理API                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 路由结构

```typescript
// src/router/index.tsx
const routes = [
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" /> },
      { path: 'dashboard', element: <Dashboard /> },
      
      // 知识提取模块
      { 
        path: 'extract', 
        children: [
          { index: true, element: <ExtractTaskList /> },
          { path: 'task/:taskId', element: <ExtractTaskDetail /> },
          { path: 'project/:projectId', element: <ProjectKnowledge /> },
        ]
      },
      
      // 重构分析模块
      { 
        path: 'refactor', 
        children: [
          { index: true, element: <RefactorTaskList /> },
          { path: 'task/:taskId', element: <RefactorTaskDetail /> },
          { path: 'report/:reportId', element: <RefactorReport /> },
        ]
      },
      
      // 反馈闭环模块
      { 
        path: 'feedback', 
        children: [
          { index: true, element: <FeedbackSessionList /> },
          { path: 'session/:sessionId', element: <FeedbackSession /> },
          { path: 'review/:reviewId', element: <ReviewWorkflow /> },
        ]
      },
      
      // 报告管理
      { 
        path: 'report', 
        children: [
          { index: true, element: <ReportList /> },
          { path: ':reportId', element: <ReportDetail /> },
        ]
      },
      
      // 系统设置
      { path: 'settings', element: <Settings /> },
    ]
  }
];
```

---

## 二、核心页面设计

### 2.1 Dashboard（数据概览）

**文件路径**: `src/pages/Dashboard/index.tsx`

#### 页面布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 数据概览                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 第一行：统计卡片（4列）                                              │ │
│ │ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐           │ │
│ │ │ 项目总数   │ │ 知识实体   │ │ 重构问题   │ │ 待审核数   │           │ │
│ │ │ 12        │ │ 1,234     │ │ 56        │ │ 8         │           │ │
│ │ │ ↑ +2      │ │ ↑ +45     │ │ ↓ -12     │ │ ↑ +3      │           │ │
│ │ └───────────┘ └───────────┘ └───────────┘ └───────────┘           │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 第二行：图表区（2列）                                                │ │
│ │ ┌─────────────────────────────┐ ┌─────────────────────────────┐    │ │
│ │ │ 知识图谱统计                 │ │ 重构问题分布                 │    │ │
│ │ │ [ECharts 饼图]              │ │ [ECharts 柱状图]            │    │ │
│ │ │ - 实体类型分布               │ │ - 按严重程度                 │    │ │
│ │ │ - 关系类型分布               │ │ - 按问题类型                 │    │ │
│ │ └─────────────────────────────┘ └─────────────────────────────┘    │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 第三行：最近任务（2列）                                              │ │
│ │ ┌─────────────────────────────┐ ┌─────────────────────────────┐    │ │
│ │ │ 最近知识提取任务              │ │ 最近重构分析任务              │    │ │
│ │ │ ┌─────────────────────────┐ │ │ ┌─────────────────────────┐ │    │ │
│ │ │ │ stock_datacenter - 运行中│ │ │ │ 项目A - 待审核          │ │    │ │
│ │ │ │ 项目B - 已完成           │ │ │ │ 项目B - 已定稿          │ │    │ │
│ │ │ │ ...                     │ │ │ │ ...                     │ │    │ │
│ │ │ └─────────────────────────┘ │ │ └─────────────────────────┘ │    │ │
│ │ └─────────────────────────────┘ └─────────────────────────────┘    │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

#### API 接口

```typescript
// src/api/dashboard.ts

// 获取概览统计
export const getDashboardStats = () => 
  api.get<DashboardStats>('/dashboard/stats');

// 获取知识图谱统计
export const getKnowledgeStats = () => 
  api.get<KnowledgeStats>('/dashboard/knowledge-stats');

// 获取重构问题统计
export const getRefactorStats = () => 
  api.get<RefactorStats>('/dashboard/refactor-stats');

// 获取最近任务
export const getRecentTasks = (limit: number = 5) => 
  api.get<RecentTasks>('/dashboard/recent-tasks', { params: { limit } });

// 类型定义
interface DashboardStats {
  projectCount: number;
  entityCount: number;
  problemCount: number;
  pendingReviewCount: number;
  projectTrend: { type: 'up' | 'down'; value: number };
  entityTrend: { type: 'up' | 'down'; value: number };
  problemTrend: { type: 'up' | 'down'; value: number };
}

interface KnowledgeStats {
  entityTypes: Array<{ type: string; count: number }>;
  relationTypes: Array<{ type: string; count: number }>;
}

interface RefactorStats {
  bySeverity: Array<{ severity: string; count: number }>;
  byType: Array<{ type: string; count: number }>;
}

interface RecentTasks {
  extractTasks: Array<ExtractTaskSummary>;
  refactorTasks: Array<RefactorTaskSummary>;
}
```

---

### 2.2 知识提取任务列表

**文件路径**: `src/pages/Extract/TaskList/index.tsx`

#### 页面布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 知识提取任务                                                             │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 操作栏                                                              │ │
│ │ [🔍 搜索项目名...] [状态筛选 ▼] [语言筛选 ▼] [+ 新建任务]           │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 任务列表表格                                                        │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │ 项目名称 │ 语言   │ 状态   │ 进度   │ 实体数 │ 创建时间 │ 操作 │  │ │
│ │ ├───────────────────────────────────────────────────────────────┤  │ │
│ │ │ stock_   │ Java   │ ✅完成 │ 100%  │ 1,234 │ 2026-03-14│ [详情]│ │
│ │ │ datacenter│       │        │       │       │          │ [文档]│ │
│ │ ├───────────────────────────────────────────────────────────────┤  │ │
│ │ │ project- │ Python │ 🔄运行 │ 45%   │ -     │ 2026-03-14│ [详情]│ │
│ │ │ b       │        │        │       │       │          │ [暂停]│ │
│ │ ├───────────────────────────────────────────────────────────────┤  │ │
│ │ │ ...     │ ...    │ ...    │ ...   │ ...   │ ...      │ ...   │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ │                                                                     │ │
│ │ [分页器: 1 2 3 ... 10]                                             │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 新建任务模态框

```typescript
// src/pages/Extract/TaskList/components/CreateTaskModal.tsx

interface CreateTaskForm {
  projectName: string;         // 项目名称（必填）
  projectPath: string;         // 项目路径（必填）
  languages: string[];         // 编程语言（多选）
  outputDir?: string;          // 输出目录（可选）
  extractConfig: {
    includeTestFiles: boolean; // 是否包含测试文件
    excludePatterns: string[]; // 排除模式
    maxFileSize: number;       // 最大文件大小（KB）
  };
}

// 表单校验规则
const rules = {
  projectName: [
    { required: true, message: '请输入项目名称' },
    { min: 2, max: 50, message: '长度2-50字符' },
  ],
  projectPath: [
    { required: true, message: '请输入项目路径' },
    { validator: validateProjectPath },
  ],
};
```

#### API 接口

```typescript
// src/api/extract.ts

// 获取任务列表
export const getExtractTaskList = (params: {
  page?: number;
  pageSize?: number;
  status?: string;
  language?: string;
  keyword?: string;
}) => api.get<PageResult<ExtractTask>>('/extract/tasks', { params });

// 创建提取任务
export const createExtractTask = (data: CreateTaskForm) => 
  api.post<ExtractTask>('/extract/tasks', data);

// 获取任务详情
export const getExtractTaskDetail = (taskId: string) => 
  api.get<ExtractTaskDetail>(`/extract/tasks/${taskId}`);

// 暂停/继续任务
export const toggleExtractTask = (taskId: string, action: 'pause' | 'resume') => 
  api.post(`/extract/tasks/${taskId}/${action}`);

// 取消任务
export const cancelExtractTask = (taskId: string) => 
  api.post(`/extract/tasks/${taskId}/cancel`);

// 删除任务
export const deleteExtractTask = (taskId: string) => 
  api.delete(`/extract/tasks/${taskId}`);
```

---

### 2.3 知识提取任务详情

**文件路径**: `src/pages/Extract/TaskDetail/index.tsx`

#### 页面布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 知识提取任务详情 - stock_datacenter                                      │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 任务信息卡                                                          │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │ 项目: stock_datacenter    语言: Java    状态: ✅ 已完成        │  │ │
│ │ │ 开始: 2026-03-14 10:00    结束: 2026-03-14 10:15    耗时: 15分│  │ │
│ │ │ 实体数: 1,234    关系数: 5,678    文档数: 45                   │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 执行进度（如果运行中）                                               │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │ [████████████████░░░░░░░░] 65%                                │  │ │
│ │ │ ✅ 克隆仓库 → ✅ 解析代码 → 🔄 提取知识 → ⏳ 生成文档 → ⏳ 完成 │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ Tab切换: [知识图谱] [生成文档] [执行日志]                           │ │
│ ├─────────────────────────────────────────────────────────────────────┤
│ │                                                                     │ │
│ │  [知识图谱 Tab内容]                                                 │ │
│ │  ┌─────────────────────────────────────────────────────────────┐   │ │
│ │  │ 知识图谱可视化（交互式）                                      │   │ │
│ │  │ - 支持缩放、拖拽、搜索节点                                    │   │ │
│ │  │ - 点击节点显示详情                                            │   │ │
│ │  │ - 按类型筛选显示                                              │   │ │
│ │  └─────────────────────────────────────────────────────────────┘   │ │
│ │                                                                     │ │
│ │  [实体统计]                                                        │ │
│ │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐          │ │
│ │  │ 类: 234   │ │ 方法: 890 │ │ 字段: 456 │ │ 注解: 123 │          │ │
│ │  └───────────┘ └───────────┘ └───────────┘ └───────────┘          │ │
│ │                                                                     │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 操作栏                                                              │ │
│ │ [查看完整文档] [导出知识图谱] [重新提取] [发起重构分析]             │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 2.4 重构分析任务列表

**文件路径**: `src/pages/Refactor/TaskList/index.tsx`

#### 页面布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 重构分析任务                                                             │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 操作栏                                                              │ │
│ │ [🔍 搜索项目名...] [状态筛选 ▼] [严重程度 ▼] [+ 新建分析]           │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 任务列表卡片视图（可选表格视图）                                     │ │
│ │ ┌─────────────────────┐ ┌─────────────────────┐                    │ │
│ │ │ stock_datacenter    │ │ project-b           │                    │ │
│ │ │ ───────────────────│ │ ───────────────────│                    │ │
│ │ │ 问题数: 80         │ │ 问题数: 45         │                    │ │
│ │ │ 高: 8  中: 32  低: 40│ │ 高: 5  中: 20  低: 20│                    │ │
│ │ │ 预计工时: 78.5h    │ │ 预计工时: 35.2h    │                    │ │
│ │ │ 状态: 🟡 待审核    │ │ 状态: ✅ 已定稿    │                    │ │
│ │ │ ───────────────────│ │ ───────────────────│                    │ │
│ │ │ [查看报告] [提交审核]│ │ [查看报告] [历史版本]│                    │ │
│ │ └─────────────────────┘ └─────────────────────┘                    │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

#### API 接口

```typescript
// src/api/refactor.ts

// 获取重构任务列表
export const getRefactorTaskList = (params: {
  page?: number;
  pageSize?: number;
  status?: string;
  severity?: string;
  keyword?: string;
}) => api.get<PageResult<RefactorTask>>('/refactor/tasks', { params });

// 创建重构分析任务
export const createRefactorTask = (data: {
  projectName: string;
  knowledgePath: string;
  config?: RefactorConfig;
}) => api.post<RefactorTask>('/refactor/tasks', data);

// 获取重构任务详情
export const getRefactorTaskDetail = (taskId: string) => 
  api.get<RefactorTaskDetail>(`/refactor/tasks/${taskId}`);

// 获取重构报告
export const getRefactorReport = (taskId: string) => 
  api.get<RefactorReport>(`/refactor/tasks/${taskId}/report`);
```

---

### 2.5 重构分析报告（核心页面）

**文件路径**: `src/pages/Refactor/Report/index.tsx`

#### 页面布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 重构分析报告 - stock_datacenter V2                           [返回列表]│
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 报告概览卡                                                          │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │ 版本: V2    状态: 🟡 待审核    生成时间: 2026-03-14 15:30     │  │ │
│ │ │                                                               │  │ │
│ │ │ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐      │  │ │
│ │ │ │ 问题总数   │ │ 高优先级   │ │ 中优先级   │ │ 预计工时   │      │  │ │
│ │ │ │ 75       │ │ 6        │ │ 32       │ │ 68.5h    │      │  │ │
│ │ │ └───────────┘ └───────────┘ └───────────┘ └───────────┘      │  │ │
│ │ │                                                               │  │ │
│ │ │ 本次变更（相比V1）:                                            │  │ │
│ │ │ - 移除问题: 5个（用户确认非问题）                               │  │ │
│ │ │ - 修改问题: 2个（调整优先级）                                   │  │ │
│ │ │ - 确认问题: 45个                                               │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 操作栏                                                              │ │
│ │ [✓ 批准报告] [✗ 拒绝] [📝 提交反馈] [📋 导出报告]                  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 问题列表                                                            │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │ 筛选: [全部▼] [高优先级▼] [类型▼]    排序: [严重程度▼]        │  │ │
│ │ │ [✓ 全选]                                       已选: 3个问题   │  │ │
│ │ ├───────────────────────────────────────────────────────────────┤  │ │
│ │ │ ☑ P001  🔴高  大文件  Counter.java        682行    3.0h      │  │ │
│ │ │   └─ 用户反馈: 同意拆分，建议保留命名规范                        │  │ │
│ │ │                                                               │  │ │
│ │ │ ☐ P002  🔴高  大文件  RealRuleParse.java  825行    4.0h      │  │ │
│ │ │                                                               │  │ │
│ │ │ ☑ P003  🟡中  缺少文档 BaseAction.java      L18     0.5h      │  │ │
│ │ │   └─ 用户方案: 使用JavaDoc标准格式                              │  │ │
│ │ │                                                               │  │ │
│ │ │ ☑ P033  🟢低  缺少文档 Other.java           L5      0.3h      │  │ │
│ │ │                                                               │  │ │
│ │ │ ... 更多问题 ...                                               │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ │                                                                     │ │
│ │ [分页器: 1 2 3 ... 10]                                             │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 底部操作栏（固定）                                                   │ │
│ │ 已选择 3 个问题    [批量同意] [批量忽略] [批量调整优先级] [提交反馈] │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 2.6 问题详情 + 反馈面板（核心交互页面）

**文件路径**: `src/pages/Refactor/Report/ProblemDetail.tsx`

#### 页面布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│ ← 返回问题列表        P001: Counter.java 大文件问题                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│ ┌────────────────────────────────────┬────────────────────────────────┐│
│ │ 📋 问题详情                        │ 💬 用户反馈                     ││
│ │                                    │                                ││
│ │ ━━━ 一、是什么 ━━━                │ ┌────────────────────────────┐ ││
│ │                                    │ │ ○ 同意此问题               │ ││
│ │ 文件共682行，包含1个类和           │ │ ○ 不同意（请说明原因）     │ ││
│ │ 204个函数，文件过大不利于          │ │ ● 需要修改                 │ ││
│ │ 维护和理解。                       │ └────────────────────────────┘ ││
│ │                                    │                                ││
│ │ 当前状态:                          │ 修改说明:                      ││
│ │ - 文件行数: 682                    │ ┌────────────────────────────┐ ││
│ │ - 函数数量: 204                    │ │ 拆分时保留原有命名规范，   │ ││
│ │ - 代码行数: 15,420                 │ │ 避免影响现有调用           │ ││
│ │ - 平均函数长度: 75行               │ └────────────────────────────┘ ││
│ │                                    │                                ││
│ │ ━━━ 二、怎么修复 ━━━              │ 优先级调整:                    ││
│ │                                    │ ┌────────────────────────────┐ ││
│ │ 步骤1: 分析文件结构                │ │ ● 高  ○ 中  ○ 低          │ ││
│ │ 步骤2: 提取独立类                  │ └────────────────────────────┘ ││
│ │ 步骤3: 按职责拆分                  │                                ││
│ │                                    │ 方案选择:                      ││
│ │ ━━━ 三、有什么风险 ━━━            │ ┌────────────────────────────┐ ││
│ │                                    │ │ ● 方案1: 按职责拆分 ⭐⭐⭐⭐│ ││
│ │ [低风险] 依赖关系变化              │ │ ○ 方案2: 提取工具类 ⭐⭐⭐ │ ││
│ │ 可能性: 低  影响: 低               │ │ ○ 自定义方案...            │ ││
│ │ 缓解: 充分测试，逐步迁移           │ └────────────────────────────┘ ││
│ │                                    │                                ││
│ │ ━━━ 四、有什么方案 ━━━            │ ┌────────────────────────────┐ ││
│ │                                    │ │          [提交反馈]        │ ││
│ │ 1. 按职责拆分 ⭐推荐               │ └────────────────────────────┘ ││
│ │    工作量: 3.0h  风险: 低          │                                ││
│ │    优点: 职责清晰，易于维护        │ [取消]                         ││
│ │    缺点: 需要调整调用方            │                                ││
│ │                                    │                                ││
│ │ 2. 提取工具类                      │                                ││
│ │    工作量: 2.0h  风险: 低          │                                ││
│ │    优点: 改动小，风险低            │                                ││
│ │    缺点: 未能根本解决问题          │                                ││
│ └────────────────────────────────────┴────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
```

#### 反馈提交 API

```typescript
// src/api/feedback.ts

// 提交单个反馈
export const submitFeedback = (data: {
  sessionId: string;
  problemId: string;
  feedbackType: FeedbackType;
  content?: string;
  suggestedPriority?: 'high' | 'medium' | 'low';
  suggestedSolution?: Solution;
}) => api.post<Feedback>('/feedback/submit', data);

// 批量提交反馈
export const submitFeedbackBatch = (data: {
  sessionId: string;
  feedbacks: FeedbackInput[];
}) => api.post<FeedbackBatch>('/feedback/batch', data);

// 获取反馈历史
export const getFeedbackHistory = (sessionId: string) => 
  api.get<FeedbackHistory>(`/feedback/history/${sessionId}`);
```

---

### 2.7 反馈闭环会话管理

**文件路径**: `src/pages/Feedback/SessionList/index.tsx`

#### 页面布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 反馈闭环会话                                                             │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 会话列表                                                            │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │ stock_datacenter - V2                                         │  │ │
│ │ │ 状态: 🟡 审核中    迭代: 2    创建: 2026-03-14 10:00          │  │ │
│ │ │ 问题: 75 → 70    反馈: 12条    收敛: 85%                       │  │ │
│ │ │ [进入会话] [查看历史] [导出]                                   │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ │                                                                     │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │ project-b - V1                                                │  │ │
│ │ │ 状态: ✅ 已定稿    迭代: 3    创建: 2026-03-13 14:00          │  │ │
│ │ │ 问题: 45 → 40    反馈: 20条    收敛: 95%                       │  │ │
│ │ │ [查看报告] [查看历史] [导出]                                   │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 审核工作流 API

```typescript
// src/api/review.ts

// 创建审核
export const createReview = (data: {
  sessionId: string;
  reportId: string;
}) => api.post<ReviewRecord>('/review/create', data);

// 执行审核动作
export const executeReviewAction = (data: {
  sessionId: string;
  recordId: string;
  action: 'submit' | 'start_review' | 'submit_feedback' | 
          'request_regenerate' | 'approve' | 'reject' | 'finalize';
  userId?: string;
  comments?: string;
  feedbackBatchId?: string;
}) => api.post<ReviewRecord>('/review/action', data);

// 获取审核状态
export const getReviewStatus = (recordId: string) => 
  api.get<ReviewStatus>(`/review/status/${recordId}`);

// 获取允许的动作
export const getAllowedActions = (recordId: string) => 
  api.get<string[]>(`/review/actions/${recordId}`);
```

---

### 2.8 审核工作流页面

**文件路径**: `src/pages/Feedback/ReviewWorkflow/index.tsx`

#### 页面布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 审核工作流 - stock_datacenter                                           │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 工作流状态图                                                        │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │                                                               │  │ │
│ │ │  草稿 ──✅──▶ 待审核 ──✅──▶ 审核中 ──✅──▶ 反馈提交           │  │ │
│ │ │                           │                    │               │  │ │
│ │ │                           │                    ▼               │  │ │
│ │ │                           │              重生成中 ──✅──▶ 待审核│  │ │
│ │ │                           │                    │               │  │ │
│ │ │                           └──────────────────▶ 批准 ──▶ 定稿   │  │ │
│ │ │                                               │               │  │ │
│ │ │                                               ▼               │  │ │
│ │ │                                             拒绝              │  │ │
│ │ │                                                               │  │ │
│ │ │  当前状态: ● 审核中                                           │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 审核记录                                                            │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │ 时间                操作人    动作          说明               │  │ │
│ │ ├───────────────────────────────────────────────────────────────┤  │ │
│ │ │ 2026-03-14 15:00   user1     提交审核      初始提交           │  │ │
│ │ │ 2026-03-14 15:05   reviewer1 开始审核      开始审核           │  │ │
│ │ │ 2026-03-14 15:30   reviewer1 提交反馈      共12条反馈         │  │ │
│ │ │ 2026-03-14 15:35   system    请求重生成    -                  │  │ │
│ │ │ ...                                                           │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 收敛状态                                                            │ │
│ │ ┌───────────────────────────────────────────────────────────────┐  │ │
│ │ │ 变更率: 8.5%    确认率: 85%    忽略率: 10%                     │  │ │
│ │ │ 收敛状态: ✅ 已收敛（低变更率）                                 │  │ │
│ │ └───────────────────────────────────────────────────────────────┘  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 操作区                                                              │ │
│ │ 允许动作: [批准] [拒绝] [继续反馈]                                  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 三、通用组件设计

### 3.1 反馈面板组件

**文件路径**: `src/components/FeedbackPanel/index.tsx`

```typescript
interface FeedbackPanelProps {
  problem: ProblemDetail;
  sessionId: string;
  onSubmit: (feedback: FeedbackInput) => void;
  onCancel: () => void;
}

// 支持的反馈类型
type FeedbackType = 
  | 'agree'           // 同意问题
  | 'disagree'        // 不同意问题
  | 'modify'          // 修改问题
  | 'accept_solution' // 接受方案
  | 'modify_solution' // 修改方案
  | 'add_solution'    // 添加方案
  | 'raise_priority'  // 提高优先级
  | 'lower_priority'  // 降低优先级
  | 'ignore_problem'; // 忽略问题
```

### 3.2 问题详情组件

**文件路径**: `src/components/ProblemDetailCard/index.tsx`

```typescript
interface ProblemDetailCardProps {
  problem: ProblemDetail;
  showActions?: boolean;
  onFeedback?: () => void;
  onViewCode?: () => void;
}

// 问题详情包含四要素
interface ProblemDetail {
  problemId: string;
  problemType: string;
  severity: 'high' | 'medium' | 'low';
  
  // 一、是什么
  description: string;
  currentState: Record<string, any>;
  codeContext?: string;
  
  // 二、怎么修复
  fixSteps: FixStep[];
  
  // 三、有什么风险
  risks: Risk[];
  riskLevel: 'high' | 'medium' | 'low';
  
  // 四、有什么方案
  solutions: Solution[];
  recommendedSolution: number;
  
  // 用户反馈状态
  userConfirmed?: boolean;
  ignored?: boolean;
  userComment?: string;
}
```

### 3.3 工作流状态图组件

**文件路径**: `src/components/WorkflowStatusDiagram/index.tsx`

```typescript
interface WorkflowStatusDiagramProps {
  currentStatus: ReviewStatus;
  history: ActionRecord[];
}

type ReviewStatus = 
  | 'draft'
  | 'pending_review'
  | 'in_review'
  | 'feedback_submitted'
  | 'regenerating'
  | 'approved'
  | 'rejected'
  | 'finalized';
```

### 3.4 收敛状态组件

**文件路径**: `src/components/ConvergenceStatus/index.tsx`

```typescript
interface ConvergenceStatusProps {
  changeRate: number;
  confirmationRate: number;
  ignoreRate: number;
  converged: boolean;
  reason: string;
}
```

---

## 四、状态管理设计（Zustand）

### 4.1 Dashboard Store

```typescript
// src/stores/dashboardStore.ts
import { create } from 'zustand';

interface DashboardState {
  stats: DashboardStats | null;
  knowledgeStats: KnowledgeStats | null;
  refactorStats: RefactorStats | null;
  recentTasks: RecentTasks | null;
  loading: Record<string, boolean>;
  errors: Record<string, string>;
  
  fetchStats: () => Promise<void>;
  fetchKnowledgeStats: () => Promise<void>;
  fetchRefactorStats: () => Promise<void>;
  fetchRecentTasks: (limit?: number) => Promise<void>;
}
```

### 4.2 Feedback Store

```typescript
// src/stores/feedbackStore.ts
import { create } from 'zustand';

interface FeedbackState {
  currentSession: FeedbackSession | null;
  currentReport: RefactorReport | null;
  selectedProblems: Set<string>;
  feedbackBatch: FeedbackBatch | null;
  reviewRecord: ReviewRecord | null;
  
  loading: Record<string, boolean>;
  errors: Record<string, string>;
  
  // 操作方法
  loadSession: (sessionId: string) => Promise<void>;
  loadReport: (reportId: string) => Promise<void>;
  selectProblem: (problemId: string) => void;
  deselectProblem: (problemId: string) => void;
  selectAll: () => void;
  clearSelection: () => void;
  submitFeedback: (feedback: FeedbackInput) => Promise<void>;
  submitBatchFeedback: () => Promise<void>;
  regenerateReport: () => Promise<void>;
  
  // 审核工作流
  createReview: () => Promise<void>;
  executeAction: (action: ReviewAction) => Promise<void>;
  checkConvergence: () => Promise<ConvergenceResult>;
}
```

### 4.3 Refactor Store

```typescript
// src/stores/refactorStore.ts
import { create } from 'zustand';

interface RefactorState {
  tasks: RefactorTask[];
  currentTask: RefactorTaskDetail | null;
  currentReport: RefactorReport | null;
  reportVersions: RefactorReport[];
  
  loading: Record<string, boolean>;
  errors: Record<string, string>;
  
  // 操作方法
  fetchTasks: (params: QueryParams) => Promise<void>;
  fetchTaskDetail: (taskId: string) => Promise<void>;
  fetchReport: (taskId: string) => Promise<void>;
  fetchReportVersions: (sessionId: string) => Promise<void>;
  createTask: (data: CreateTaskInput) => Promise<RefactorTask>;
  deleteTask: (taskId: string) => Promise<void>;
}
```

---

## 五、API 接口对接

### 5.1 统一 Axios 封装

```typescript
// src/api/index.ts
import axios from 'axios';
import { message } from 'antd';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// 请求拦截器
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    } else {
      message.error(error.response?.data?.message || '请求失败');
    }
    return Promise.reject(error);
  }
);

export default api;
```

### 5.2 完整 API 模块

```typescript
// src/api/index.ts - 统一导出
export { default as api } from './config';

// Dashboard
export * from './dashboard';

// Extract
export * from './extract';

// Refactor
export * from './refactor';

// Feedback
export * from './feedback';

// Review
export * from './review';

// Report
export * from './report';
```

---

## 六、后端 API 接口清单

### 6.1 知识提取 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/extract/tasks | 获取任务列表 |
| POST | /api/v1/extract/tasks | 创建提取任务 |
| GET | /api/v1/extract/tasks/:taskId | 获取任务详情 |
| POST | /api/v1/extract/tasks/:taskId/pause | 暂停任务 |
| POST | /api/v1/extract/tasks/:taskId/resume | 继续任务 |
| POST | /api/v1/extract/tasks/:taskId/cancel | 取消任务 |
| DELETE | /api/v1/extract/tasks/:taskId | 删除任务 |
| GET | /api/v1/extract/projects/:projectId/knowledge | 获取知识图谱 |
| GET | /api/v1/extract/projects/:projectId/docs | 获取生成文档 |

### 6.2 重构分析 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/refactor/tasks | 获取任务列表 |
| POST | /api/v1/refactor/tasks | 创建分析任务 |
| GET | /api/v1/refactor/tasks/:taskId | 获取任务详情 |
| GET | /api/v1/refactor/tasks/:taskId/report | 获取分析报告 |
| DELETE | /api/v1/refactor/tasks/:taskId | 删除任务 |

### 6.3 反馈闭环 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/feedback/submit | 提交单个反馈 |
| POST | /api/v1/feedback/batch | 批量提交反馈 |
| GET | /api/v1/feedback/history/:sessionId | 获取反馈历史 |
| POST | /api/v1/feedback/regenerate | 重新生成报告 |
| GET | /api/v1/feedback/versions/:sessionId | 获取报告版本列表 |

### 6.4 审核工作流 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/review/create | 创建审核 |
| POST | /api/v1/review/action | 执行审核动作 |
| GET | /api/v1/review/status/:recordId | 获取审核状态 |
| GET | /api/v1/review/actions/:recordId | 获取允许动作 |
| GET | /api/v1/review/history/:recordId | 获取审核历史 |
| GET | /api/v1/review/convergence/:sessionId | 检查收敛状态 |

### 6.5 报告管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/report/list | 获取报告列表 |
| GET | /api/v1/report/:reportId | 获取报告详情 |
| GET | /api/v1/report/:reportId/export/pdf | 导出PDF |
| GET | /api/v1/report/:reportId/export/html | 导出HTML |
| DELETE | /api/v1/report/:reportId | 删除报告 |

---

## 七、前端项目结构

```
frontend/
├── src/
│   ├── api/                      # API 接口
│   │   ├── index.ts              # 统一导出
│   │   ├── config.ts             # Axios 配置
│   │   ├── dashboard.ts          # Dashboard API
│   │   ├── extract.ts            # 知识提取 API
│   │   ├── refactor.ts           # 重构分析 API
│   │   ├── feedback.ts           # 反馈闭环 API
│   │   ├── review.ts             # 审核工作流 API
│   │   └── report.ts             # 报告管理 API
│   │
│   ├── components/               # 通用组件
│   │   ├── FeedbackPanel/        # 反馈面板
│   │   ├── ProblemDetailCard/    # 问题详情卡片
│   │   ├── WorkflowStatusDiagram/# 工作流状态图
│   │   ├── ConvergenceStatus/    # 收敛状态
│   │   ├── StatCard/             # 统计卡片
│   │   ├── HeatmapCard/          # 热力图卡片
│   │   ├── TrendChartCard/       # 趋势图卡片
│   │   ├── StepProgressBar/      # 步骤进度条
│   │   ├── RealTimeLogViewer/    # 实时日志
│   │   └── CodeDiffViewer/       # 代码差异
│   │
│   ├── pages/                    # 页面组件
│   │   ├── Dashboard/            # 数据概览
│   │   ├── Extract/              # 知识提取
│   │   │   ├── TaskList/         # 任务列表
│   │   │   └── TaskDetail/       # 任务详情
│   │   ├── Refactor/             # 重构分析
│   │   │   ├── TaskList/         # 任务列表
│   │   │   ├── TaskDetail/       # 任务详情
│   │   │   └── Report/           # 分析报告
│   │   ├── Feedback/             # 反馈闭环
│   │   │   ├── SessionList/      # 会话列表
│   │   │   ├── Session/          # 会话详情
│   │   │   └── ReviewWorkflow/   # 审核工作流
│   │   ├── Report/               # 报告管理
│   │   │   ├── List/             # 报告列表
│   │   │   └── Detail/           # 报告详情
│   │   └── Settings/             # 系统设置
│   │
│   ├── stores/                   # 状态管理
│   │   ├── dashboardStore.ts
│   │   ├── extractStore.ts
│   │   ├── refactorStore.ts
│   │   ├── feedbackStore.ts
│   │   └── settingsStore.ts
│   │
│   ├── hooks/                    # 自定义 Hooks
│   │   ├── useFeedback.ts
│   │   ├── useReview.ts
│   │   └── useWebSocket.ts
│   │
│   ├── router/                   # 路由配置
│   │   └── index.tsx
│   │
│   ├── types/                    # 类型定义
│   │   ├── dashboard.ts
│   │   ├── extract.ts
│   │   ├── refactor.ts
│   │   ├── feedback.ts
│   │   └── review.ts
│   │
│   ├── utils/                    # 工具函数
│   │   ├── format.ts
│   │   ├── validator.ts
│   │   └── storage.ts
│   │
│   ├── styles/                   # 样式文件
│   │   ├── variables.less
│   │   └── global.less
│   │
│   ├── App.tsx                   # 应用入口
│   └── main.tsx                  # 渲染入口
│
├── public/                       # 静态资源
├── package.json
├── tsconfig.json
├── vite.config.ts
└── .env                          # 环境变量
```

---

## 八、实现优先级

### Phase 1: 基础框架（1周）
- [x] 项目初始化（Vite + React + TypeScript）
- [x] 路由配置（React Router）
- [x] API 封装（Axios）
- [x] 状态管理基础（Zustand）
- [x] 全局布局（ProLayout）

### Phase 2: 核心页面（2周）
- [x] Dashboard 数据概览
- [x] 知识提取任务管理
- [x] 重构分析任务管理
- [x] 重构分析报告展示

### Phase 3: 反馈闭环（2周）
- [x] 反馈面板组件
- [x] 问题详情组件
- [x] 批量反馈功能
- [x] 报告重生成流程
- [x] 审核工作流界面

### Phase 4: 高级功能（1周）
- [x] 工作流状态可视化
- [x] 收敛状态展示
- [x] 报告导出（PDF/HTML）
- [x] 实时日志查看

### Phase 5: 优化完善（1周）
- [x] 响应式适配
- [x] 性能优化
- [x] 错误处理完善
- [x] 单元测试

---

## 九、验收标准

1. **功能完整性**: 实现所有页面和功能，与后端 API 完全对接
2. **代码质量**: 符合 ESLint + Prettier + TypeScript 严格模式
3. **交互体验**: 所有操作有加载、成功、错误反馈
4. **视觉规范**: 遵循 Ant Design 5.20+ 企业级设计规范
5. **性能要求**: 页面加载 ≤2s，交互响应 ≤200ms
6. **测试覆盖**: 核心组件测试覆盖率 ≥80%
