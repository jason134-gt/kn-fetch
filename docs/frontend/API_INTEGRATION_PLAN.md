# 前后端数据打通修改计划

## 问题诊断

### 当前状态
1. **前端配置**：API base URL = `/api`，代理到 `http://localhost:8000`
2. **后端服务**：运行在 `http://127.0.0.1:8892`
3. **问题**：
   - 端口不匹配（前端代理 8000，后端运行 8892）
   - 所有页面使用 mock 数据，未连接真实后端
   - 后端 API 不完整，缺少前端需要的接口

### 影响页面
所有页面都使用 `mockApi`，包括：
- Dashboard.tsx
- ExtractTaskList.tsx
- ExtractTaskDetail.tsx
- RefactorTaskList.tsx
- RefactorReport.tsx
- FeedbackSessionList.tsx
- ReviewWorkflow.tsx
- ReportManagement.tsx
- Settings.tsx

---

## 修改计划

### 第一阶段：后端 API 完善（优先级：高）

#### 1.1 后端 API 端口统一
- [ ] **任务**：统一后端服务端口为 8000 或更新前端代理配置
- [ ] **文件**：
  - `start_web.py` - 修改端口为 8000
  - `docs/frontend/vite.config.ts` - 确认代理配置正确
- [ ] **预期结果**：前端可以正确连接后端服务

#### 1.2 Dashboard 数据接口
- [ ] **任务**：实现仪表盘统计接口
- [ ] **后端接口**：
  ```python
  GET /api/dashboard/stats
  # 返回：项目数、实体数、问题数、待审核数及趋势
  
  GET /api/dashboard/knowledge-stats
  # 返回：知识图谱统计（实体类型分布）
  
  GET /api/dashboard/refactor-stats
  # 返回：重构问题统计（按严重程度分布）
  
  GET /api/dashboard/recent-tasks
  # 返回：最近提取任务和重构任务
  ```
- [ ] **前端修改**：`pages/Dashboard.tsx` - 替换 mockApi 调用为真实 API

#### 1.3 知识提取任务接口
- [ ] **任务**：实现提取任务的完整 CRUD 接口
- [ ] **后端接口**：
  ```python
  GET /api/extract/tasks
  # 参数：status, keyword, page, pageSize
  # 返回：任务列表（分页）
  
  POST /api/extract/tasks
  # 创建新任务
  
  GET /api/extract/tasks/{task_id}
  # 返回：任务详情
  
  PUT /api/extract/tasks/{task_id}/pause
  # 暂停任务
  
  PUT /api/extract/tasks/{task_id}/resume
  # 恢复任务
  
  PUT /api/extract/tasks/{task_id}/cancel
  # 取消任务
  
  DELETE /api/extract/tasks/{task_id}
  # 删除任务
  
  GET /api/extract/tasks/{task_id}/entities
  # 返回：任务的知识实体列表
  ```
- [ ] **前端修改**：
  - `pages/ExtractTaskList.tsx` - 替换 mockApi
  - `pages/ExtractTaskDetail.tsx` - 替换 mockApi

#### 1.4 重构分析任务接口
- [ ] **任务**：实现重构分析任务的完整接口
- [ ] **后端接口**：
  ```python
  GET /api/refactor/tasks
  # 参数：status, keyword, page, pageSize
  # 返回：任务列表（分页）
  
  POST /api/refactor/tasks
  # 创建新分析任务
  
  GET /api/refactor/tasks/{task_id}
  # 返回：重构报告详情
  
  DELETE /api/refactor/tasks/{task_id}
  # 删除任务
  ```
- [ ] **前端修改**：
  - `pages/RefactorTaskList.tsx` - 替换 mockApi
  - `pages/RefactorReport.tsx` - 替换 mockApi

#### 1.5 反馈闭环接口
- [ ] **任务**：实现反馈会话和审核工作流接口
- [ ] **后端接口**：
  ```python
  GET /api/feedback/sessions
  # 参数：status, page, pageSize
  # 返回：会话列表
  
  GET /api/feedback/sessions/{session_id}
  # 返回：会话详情
  
  POST /api/feedback/sessions/{session_id}/approve
  # 通过审核
  
  POST /api/feedback/sessions/{session_id}/reject
  # 拒绝审核
  
  POST /api/feedback/sessions/{session_id}/request-changes
  # 请求修改
  ```
- [ ] **前端修改**：
  - `pages/FeedbackSessionList.tsx` - 替换 mockApi
  - `pages/ReviewWorkflow.tsx` - 替换 mockApi

#### 1.6 报告管理接口
- [ ] **任务**：实现报告管理接口
- [ ] **后端接口**：
  ```python
  GET /api/reports
  # 参数：status, keyword, page, pageSize
  # 返回：报告列表
  
  GET /api/reports/{report_id}
  # 返回：报告详情
  
  POST /api/reports/{report_id}/export
  # 导出报告
  
  POST /api/reports/{report_id}/archive
  # 归档报告
  ```
- [ ] **前端修改**：`pages/ReportManagement.tsx` - 替换 mockApi

#### 1.7 系统设置接口
- [ ] **任务**：实现系统配置接口
- [ ] **后端接口**：
  ```python
  GET /api/settings
  # 返回：系统配置
  
  PUT /api/settings
  # 更新系统配置
  
  GET /api/settings/ai-config
  # 返回：AI 配置
  
  PUT /api/settings/ai-config
  # 更新 AI 配置
  ```
- [ ] **前端修改**：`pages/Settings.tsx` - 替换 mockApi

---

### 第二阶段：前端 API 服务层重构（优先级：高）

#### 2.1 创建统一的 API 服务层
- [ ] **任务**：创建 `services/api.ts` 文件，封装所有 API 调用
- [ ] **文件**：`docs/frontend/services/api.ts`
- [ ] **内容**：
  ```typescript
  import api from './api/config';
  import type { ... } from '../api-types';
  
  export const dashboardApi = {
    getStats: () => api.get('/dashboard/stats'),
    getKnowledgeStats: () => api.get('/dashboard/knowledge-stats'),
    // ...
  };
  
  export const extractApi = {
    getTasks: (params) => api.get('/extract/tasks', { params }),
    getTaskDetail: (id) => api.get(`/extract/tasks/${id}`),
    // ...
  };
  
  // ... 其他 API 模块
  ```

#### 2.2 创建类型安全的 API hooks
- [ ] **任务**：创建 React hooks 封装 API 调用
- [ ] **文件**：`docs/frontend/hooks/useApi.ts`
- [ ] **内容**：
  ```typescript
  export const useExtractTasks = (params) => {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    
    useEffect(() => {
      // 调用 API
    }, [params]);
    
    return { data, loading, error, refetch };
  };
  ```

#### 2.3 替换所有页面的 mockApi 调用
- [ ] **任务**：逐个页面替换 mockApi 为真实 API
- [ ] **顺序**：
  1. Dashboard.tsx（最简单，验证流程）
  2. ExtractTaskList.tsx
  3. ExtractTaskDetail.tsx
  4. RefactorTaskList.tsx
  5. RefactorReport.tsx
  6. FeedbackSessionList.tsx
  7. ReviewWorkflow.tsx
  8. ReportManagement.tsx
  9. Settings.tsx

---

### 第三阶段：错误处理和优化（优先级：中）

#### 3.1 统一错误处理
- [ ] **任务**：添加全局错误处理和用户提示
- [ ] **文件**：`docs/frontend/utils/errorHandler.ts`
- [ ] **内容**：
  - 统一错误提示（使用 Ant Design message）
  - 错误日志记录
  - 网络错误重试机制

#### 3.2 Loading 状态优化
- [ ] **任务**：优化页面加载状态
- [ ] **内容**：
  - 添加骨架屏（Skeleton）
  - 优化 loading 提示
  - 防止页面闪烁

#### 3.3 数据缓存策略
- [ ] **任务**：实现数据缓存，提升性能
- [ ] **方案**：
  - 使用 React Query 或 SWR 进行数据缓存
  - 或者在 Zustand store 中实现缓存逻辑

---

### 第四阶段：WebSocket 实时通信（优先级：低）

#### 4.1 任务进度实时更新
- [ ] **任务**：实现任务进度实时推送
- [ ] **后端接口**：
  ```python
  WebSocket /ws
  # 推送任务进度更新
  ```
- [ ] **前端实现**：
  - WebSocket 连接管理
  - 实时更新任务状态
  - 断线重连机制

---

## 实施顺序（建议）

### 第一步：基础设施（必须）
1. ✅ 修改后端端口为 8000
2. ✅ 确认前端代理配置正确
3. ✅ 创建 API 服务层框架

### 第二步：核心功能（必须）
4. ✅ 实现 Dashboard 接口
5. ✅ 实现知识提取任务接口
6. ✅ 实现重构分析任务接口

### 第三步：完整功能（重要）
7. ✅ 实现反馈闭环接口
8. ✅ 实现报告管理接口
9. ✅ 实现系统设置接口

### 第四步：优化完善（可选）
10. ⭕ 错误处理优化
11. ⭕ Loading 状态优化
12. ⭕ 数据缓存策略
13. ⭕ WebSocket 实时通信

---

## 技术栈

### 后端
- FastAPI
- SQLAlchemy（如果需要数据库）
- Pydantic（数据验证）

### 前端
- Axios（HTTP 客户端）
- React Query 或 SWR（数据缓存，可选）
- Ant Design（UI 组件）

---

## 风险和注意事项

1. **数据一致性**：确保前后端数据结构一致
2. **错误处理**：所有 API 调用都需要错误处理
3. **性能优化**：避免频繁请求，实现数据缓存
4. **安全性**：API 需要添加认证和授权
5. **测试**：每个接口都需要测试

---

## 预计工作量

- **第一阶段**：后端 API 完善 - 3-5 天
- **第二阶段**：前端 API 服务层 - 2-3 天
- **第三阶段**：错误处理和优化 - 1-2 天
- **第四阶段**：WebSocket 实时通信 - 1-2 天

**总计**：7-12 天

---

## 当前进度

- [ ] 第一阶段未开始
- [ ] 第二阶段未开始
- [ ] 第三阶段未开始
- [ ] 第四阶段未开始
