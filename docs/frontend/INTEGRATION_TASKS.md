# 前后端数据打通任务列表

## 一、现状分析

### 1.1 前端页面数据源状态

| 页面 | 当前数据源 | 状态 |
|------|-----------|------|
| Dashboard.tsx | 本地 mock 常量 | 待修改 |
| ExtractTaskList.tsx | mockApi | 待修改 |
| ExtractTaskDetail.tsx | 真实 API | 已完成 |
| RefactorTaskList.tsx | mockApi | 待修改 |
| RefactorReport.tsx | 需检查 | 待确认 |
| ReportManagement.tsx | mockApi | 待修改 |
| FeedbackSessionList.tsx | mockApi | 待修改 |
| ReviewWorkflow.tsx | 需检查 | 待确认 |
| Settings.tsx | 需检查 | 待确认 |

### 1.2 后端 API 状态

所有 API 端点均返回硬编码模拟数据，需要连接真实数据源。

---

## 二、任务分解

### 阶段一：后端数据源连接（优先级：高）

#### 任务 1.1：创建数据存储层
**目标**：建立统一的数据访问层，支持从文件系统/数据库读取真实数据

**修改文件**：
- 新建 `src/web/data_store.py` - 数据存储访问层

**具体内容**：
- [ ] 实现 `TaskDataStore` 类，管理任务数据
- [ ] 实现 `ReportDataStore` 类，管理报告数据
- [ ] 实现 `KnowledgeDataStore` 类，管理知识实体数据
- [ ] 实现从 `output/` 目录读取已生成的知识文件
- [ ] 实现从 `generated_documents/` 目录读取已生成的报告

#### 任务 1.2：修改 Dashboard API
**目标**：返回真实的仪表盘统计数据

**修改文件**：
- `frontend_api_server.py` 或 `src/web/frontend_api.py`

**API 端点**：
```
GET /api/dashboard/stats         - 返回真实项目/实体/问题统计
GET /api/dashboard/knowledge-stats - 返回真实知识图谱统计
GET /api/dashboard/refactor-stats - 返回真实重构问题统计
GET /api/dashboard/recent-tasks   - 返回真实最近任务
```

**具体内容**：
- [ ] 扫描 `output/` 目录统计项目数和实体数
- [ ] 扫描 `generated_documents/` 目录统计报告和问题数
- [ ] 从文件元数据读取最近任务

#### 任务 1.3：修改知识提取任务 API
**目标**：返回真实的提取任务数据

**API 端点**：
```
GET    /api/extract/tasks           - 获取提取任务列表
GET    /api/extract/tasks/{id}      - 获取任务详情
POST   /api/extract/tasks           - 创建新任务
POST   /api/extract/tasks/{id}/pause  - 暂停任务
POST   /api/extract/tasks/{id}/resume - 恢复任务
POST   /api/extract/tasks/{id}/cancel - 取消任务
DELETE /api/extract/tasks/{id}      - 删除任务
GET    /api/extract/tasks/{id}/entities - 获取知识实体
```

**具体内容**：
- [ ] 从 `output/` 目录扫描现有提取任务
- [ ] 读取任务元数据文件（如 `task_meta.json`）
- [ ] 读取知识实体文件（如 `knowledge.json`）
- [ ] 实现任务创建（调用实际提取流程）

#### 任务 1.4：修改重构分析任务 API
**目标**：返回真实的重构任务数据

**API 端点**：
```
GET    /api/refactor/tasks        - 获取重构任务列表
GET    /api/refactor/tasks/{id}   - 获取重构报告
POST   /api/refactor/tasks        - 创建重构任务
DELETE /api/refactor/tasks/{id}   - 删除任务
```

**具体内容**：
- [ ] 从 `generated_documents/` 目录扫描现有重构报告
- [ ] 读取重构报告 JSON 文件
- [ ] 解析报告中的问题和解决方案
- [ ] 实现任务创建（调用实际分析流程）

#### 任务 1.5：修改反馈会话 API
**目标**：返回真实的反馈会话数据

**API 端点**：
```
GET  /api/feedback/sessions        - 获取反馈会话列表
GET  /api/feedback/sessions/{id}   - 获取会话详情
POST /api/feedback/sessions/{id}/approve - 通过审核
POST /api/feedback/sessions/{id}/reject  - 拒绝审核
```

**具体内容**：
- [ ] 设计反馈数据存储结构
- [ ] 实现反馈会话的创建和查询
- [ ] 实现审核状态管理

#### 任务 1.6：修改报告管理 API
**目标**：返回真实的报告数据

**API 端点**：
```
GET  /api/reports              - 获取报告列表
GET  /api/reports/{id}         - 获取报告详情
POST /api/reports/{id}/export  - 导出报告
POST /api/reports/{id}/archive - 归档报告
```

**具体内容**：
- [ ] 从 `generated_documents/` 目录读取报告列表
- [ ] 实现报告详情查询
- [ ] 实现报告导出功能

---

### 阶段二：前端数据源替换（优先级：高）

#### 任务 2.1：修改 Dashboard 页面
**目标**：使用真实 API 替换本地 mock 数据

**修改文件**：
- `docs/frontend/pages/Dashboard.tsx`

**具体内容**：
- [ ] 引入 `dashboardApi` 从 `services/api.ts`
- [ ] 添加 `useEffect` 调用真实 API
- [ ] 添加 loading 状态处理
- [ ] 添加错误处理

#### 任务 2.2：修改 ExtractTaskList 页面
**目标**：使用真实 API 替换 mockApi

**修改文件**：
- `docs/frontend/pages/ExtractTaskList.tsx`

**具体内容**：
- [ ] 将 `mockApi` 替换为 `extractApi`
- [ ] 适配 API 返回数据格式
- [ ] 处理 API 错误

#### 任务 2.3：修改 RefactorTaskList 页面
**目标**：使用真实 API 替换 mockApi

**修改文件**：
- `docs/frontend/pages/RefactorTaskList.tsx`

**具体内容**：
- [ ] 将 `mockApi` 替换为 `refactorApi`
- [ ] 适配 API 返回数据格式
- [ ] 处理 API 错误

#### 任务 2.4：修改 ReportManagement 页面
**目标**：使用真实 API 替换 mockApi

**修改文件**：
- `docs/frontend/pages/ReportManagement.tsx`

**具体内容**：
- [ ] 将 `mockApi` 替换为 `reportApi`
- [ ] 适配 API 返回数据格式
- [ ] 处理 API 错误

#### 任务 2.5：修改 FeedbackSessionList 页面
**目标**：使用真实 API 替换 mockApi

**修改文件**：
- `docs/frontend/pages/FeedbackSessionList.tsx`

**具体内容**：
- [ ] 将 `mockApi` 替换为 `feedbackApi`
- [ ] 适配 API 返回数据格式
- [ ] 处理 API 错误

#### 任务 2.6：检查并修改其他页面
**目标**：确保所有页面使用真实 API

**需要检查的文件**：
- `docs/frontend/pages/RefactorReport.tsx`
- `docs/frontend/pages/ReviewWorkflow.tsx`
- `docs/frontend/pages/Settings.tsx`

---

### 阶段三：API 服务层完善（优先级：中）

#### 任务 3.1：完善 API 类型定义
**目标**：确保前端类型与后端返回数据一致

**修改文件**：
- `docs/frontend/api-types.ts`

**具体内容**：
- [ ] 检查所有类型定义与后端返回数据匹配
- [ ] 添加缺失的类型定义
- [ ] 修复类型不匹配问题

#### 任务 3.2：完善 API 服务层
**目标**：添加缺失的 API 调用方法

**修改文件**：
- `docs/frontend/services/api.ts`

**具体内容**：
- [ ] 检查所有页面需要的 API 方法
- [ ] 添加缺失的 API 方法
- [ ] 添加错误处理和重试逻辑

---

### 阶段四：测试与验证（优先级：中）

#### 任务 4.1：API 接口测试
**目标**：验证所有 API 端点正常工作

**具体内容**：
- [ ] 测试 Dashboard 所有 API
- [ ] 测试提取任务所有 API
- [ ] 测试重构任务所有 API
- [ ] 测试反馈所有 API
- [ ] 测试报告管理所有 API

#### 任务 4.2：前端页面功能测试
**目标**：验证所有页面正常显示真实数据

**具体内容**：
- [ ] 测试 Dashboard 页面数据加载
- [ ] 测试任务列表页面数据加载
- [ ] 测试任务详情页面数据加载
- [ ] 测试报告管理页面数据加载
- [ ] 测试下钻导航功能

---

## 三、执行顺序

1. **阶段一（任务 1.1-1.6）**：后端数据源连接
2. **阶段二（任务 2.1-2.6）**：前端数据源替换
3. **阶段三（任务 3.1-3.2）**：API 类型和服务层完善
4. **阶段四（任务 4.1-4.2）**：测试与验证

---

## 四、注意事项

1. **数据格式兼容**：确保后端返回数据格式与前端类型定义一致
2. **错误处理**：所有 API 调用需要添加错误处理
3. **Loading 状态**：页面加载时显示 loading 状态
4. **空数据处理**：处理数据为空的情况
5. **分页处理**：确保分页参数正确传递

---

## 五、预计工时

| 阶段 | 预计工时 |
|------|----------|
| 阶段一 | 4-6 小时 |
| 阶段二 | 2-3 小时 |
| 阶段三 | 1-2 小时 |
| 阶段四 | 1-2 小时 |
| **总计** | **8-13 小时** |
