# CodeRefactor AI 智能重构平台控制台页面原型设计文档
> **文档定位**：AI开发工具可1:1落地执行的企业级前端开发规范，无模糊描述，所有页面、组件、布局、交互、API、状态管理均明确可编码
> **目标系统**：合并「全量代码资产AI扫描与语义建模系统」+「AI重构智能体系统」后的统一平台
> **目标用户**：架构师、资深研发、重构负责人
> **技术栈（固定不可修改）**：React 18 + TypeScript 5 + Ant Design 5.20+ + Zustand 4.5+ + React Router v6.22+ + Axios 1.7+ + ECharts 5.5+
> **AI开发工具执行规则（必须100%遵守）**：
> 1. 严格遵循Ant Design 5.20+官方API，禁止自定义核心组件
> 2. 所有页面必须响应式，适配1920x1080及以上桌面端，最小适配1366x768
> 3. 所有交互必须有**加载状态（Spin/Skeleton）、错误提示（Message.error/Modal.error）、成功反馈（Message.success/Notification.success）**
> 4. 所有数据必须通过统一封装的Axios API获取，禁止硬编码
> 5. 所有表单必须有完整的Ant Design Form校验规则
> 6. 所有状态管理用Zustand，按功能模块划分Store
> 7. 所有路由用React Router v6.22+的嵌套路由结构
> 8. 所有图表用ECharts 5.5+，严格遵循Ant Design的配色规范
> 9. 所有代码必须符合ESLint + Prettier + TypeScript严格模式
> 10. 所有页面必须配套基础的单元测试（用Vitest + React Testing Library）

---

## 一、全局设计规范
### 1.1 配色规范（严格遵循Ant Design 5.20+默认企业级配色）
| 用途 | 颜色值 | 说明 |
|------|--------|------|
| 主色 | #1677ff | 按钮、链接、选中状态、进度条主色 |
| 成功色 | #52c41a | 成功提示、通过状态、绿色进度条 |
| 警告色 | #faad14 | 警告提示、中等风险、黄色进度条 |
| 错误色 | #ff4d4f | 错误提示、高风险/严重风险、红色进度条 |
| 信息色 | #1890ff | 信息提示、低风险、蓝色进度条 |
| 背景色 | #f5f7fa | 页面背景、卡片背景 |
| 文字主色 | #1f2329 | 标题、正文主内容 |
| 文字次色 | #86909c | 辅助文字、说明文字 |
| 边框色 | #e5e6eb | 卡片边框、分割线 |

### 1.2 布局规范
- **全局布局**：Ant Design ProLayout（固定侧边栏、顶部导航栏、内容区自适应）
  - 侧边栏宽度：240px（可折叠为64px）
  - 顶部导航栏高度：64px
  - 内容区padding：24px
- **卡片布局**：Ant Design Card，默认带阴影（shadow="hover"），标题栏带操作区
- **表格布局**：Ant Design Table，默认带分页（pageSize=20）、排序、筛选、导出功能
- **表单布局**：Ant Design Form，默认labelCol={{span: 6}}、wrapperCol={{span: 18}}，提交按钮在底部居中

### 1.3 交互规范
- **加载状态**：
  - 页面级加载：Ant Design Spin（全屏居中，size="large"）
  - 组件级加载：Ant Design Skeleton（表格用Skeleton.Table，卡片用Skeleton.Avatar+Skeleton.Paragraph）
  - 按钮级加载：Ant Design Button的loading属性
- **错误处理**：
  - API请求失败：统一用Axios拦截器处理，弹出Message.error（显示错误信息，duration=3s）
  - 表单校验失败：Ant Design Form自动显示错误提示
  - 严重错误（如仓库克隆失败、重构任务崩溃）：弹出Modal.error（显示详细错误信息，带“查看日志”按钮）
- **成功反馈**：
  - 操作成功（如创建任务、提交表单）：弹出Message.success（显示成功信息，duration=2s）
  - 重要操作成功（如扫描完成、重构完成）：弹出Notification.success（显示标题、内容、跳转链接，duration=5s）

---

## 二、路由结构（固定嵌套路由）
```typescript
// src/router/index.tsx
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProLayout } from '@ant-design/pro-components';
import { HomeOutlined, CodeOutlined, ScanOutlined, ToolOutlined, FileTextOutlined, SettingOutlined } from '@ant-design/icons';
import Dashboard from '../pages/Dashboard';
import RepositoryList from '../pages/Repository/RepositoryList';
import RepositoryDetail from '../pages/Repository/RepositoryDetail';
import ScanTaskList from '../pages/Scan/ScanTaskList';
import ScanTaskDetail from '../pages/Scan/ScanTaskDetail';
import RefactoringTaskList from '../pages/Refactoring/RefactoringTaskList';
import RefactoringTaskDetail from '../pages/Refactoring/RefactoringTaskDetail';
import ReportList from '../pages/Report/ReportList';
import ReportDetail from '../pages/Report/ReportDetail';
import Settings from '../pages/Settings';

const router = createBrowserRouter([
  {
    path: '/',
    element: (
      <ProLayout
        title="CodeRefactor AI"
        logo="/logo.svg"
        layout="side"
        siderWidth={240}
        fixedHeader
        fixedSider
        menu={{
          locale: false,
          items: [
            {
              key: '/dashboard',
              icon: <HomeOutlined />,
              label: '数据概览',
            },
            {
              key: '/repository',
              icon: <CodeOutlined />,
              label: '代码仓库',
            },
            {
              key: '/scan',
              icon: <ScanOutlined />,
              label: '扫描建模',
            },
            {
              key: '/refactoring',
              icon: <ToolOutlined />,
              label: 'AI重构',
            },
            {
              key: '/report',
              icon: <FileTextOutlined />,
              label: '分析报告',
            },
            {
              key: '/settings',
              icon: <SettingOutlined />,
              label: '系统设置',
            },
          ],
        }}
      >
        {/* 嵌套路由内容区 */}
      </ProLayout>
    ),
    children: [
      {
        index: true,
        element: <Navigate to="/dashboard" replace />,
      },
      {
        path: '/dashboard',
        element: <Dashboard />,
      },
      {
        path: '/repository',
        children: [
          {
            index: true,
            element: <RepositoryList />,
          },
          {
            path: ':repositoryId',
            element: <RepositoryDetail />,
          },
        ],
      },
      {
        path: '/scan',
        children: [
          {
            index: true,
            element: <ScanTaskList />,
          },
          {
            path: ':taskId',
            element: <ScanTaskDetail />,
          },
        ],
      },
      {
        path: '/refactoring',
        children: [
          {
            index: true,
            element: <RefactoringTaskList />,
          },
          {
            path: ':taskId',
            element: <RefactoringTaskDetail />,
          },
        ],
      },
      {
        path: '/report',
        children: [
          {
            index: true,
            element: <ReportList />,
          },
          {
            path: ':reportId',
            element: <ReportDetail />,
          },
        ],
      },
      {
        path: '/settings',
        element: <Settings />,
      },
    ],
  },
]);

export default router;
```

---

## 三、核心页面详细设计（按优先级排序）
### 页面1：数据概览（Dashboard）
**文件路径**：`src/pages/Dashboard/index.tsx`
**页面状态管理**：`src/stores/dashboardStore.ts`
**API调用**：统一封装在`src/api/dashboard.ts`
#### 1.1 页面布局
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ 顶部：全局ProLayout（固定）                                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ 内容区（padding:24px）                                                          │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 第一行：4个统计卡片（24px间距，响应式：1920x1080→4列，1366x768→2列）     │ │
│ │ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ │ │
│ │ 代码仓库总数   │ 扫描任务总数   │ 重构任务总数   │ 技术债务总数   │ │ │
│ │ （带趋势图）   │ （带状态分布） │ （带状态分布） │ （带风险分布） │ │ │
│ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 第二行：2个图表卡片（24px间距，响应式：1920x1080→2列，1366x768→1列）     │ │
│ │ ┌──────────────────────────────────┐ ┌──────────────────────────────────┐ │ │
│ │ 左侧：代码资产热力图（按模块/风险） │ 右侧：任务执行趋势图（近30天）    │ │ │
│ └──────────────────────────────────┘ └──────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 第三行：2个列表卡片（24px间距，响应式：1920x1080→2列，1366x768→1列）     │ │
│ │ ┌──────────────────────────────────┐ ┌──────────────────────────────────┐ │ │
│ │ 左侧：最近10个扫描任务             │ 右侧：最近10个重构任务             │ │ │
│ └──────────────────────────────────┘ └──────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘
```
#### 1.2 核心组件设计
##### 组件1.2.1：统计卡片（StatCard）
**文件路径**：`src/components/StatCard/index.tsx`
**Props定义**：
```typescript
import { ReactNode } from 'react';

interface StatCardProps {
  title: string;
  value: number | string;
  trend?: {
    type: 'up' | 'down' | 'none';
    value: number; // 百分比，如10表示+10%
  };
  icon?: ReactNode;
  color?: 'blue' | 'green' | 'yellow' | 'red';
  extra?: ReactNode;
}
```
**实现要求**：
- 用Ant Design Card封装
- 标题用文字次色，值用文字主色（字号24px，加粗）
- 趋势图用Ant Design Progress的微型环形图（如果有trend）
- 图标放在左上角，颜色与主色一致
- 响应式宽度：100%

##### 组件1.2.2：热力图卡片（HeatmapCard）
**文件路径**：`src/components/HeatmapCard/index.tsx`
**Props定义**：
```typescript
interface HeatmapCardProps {
  title: string;
  data: Array<{
    name: string;
    value: number;
    riskLevel?: 'P0' | 'P1' | 'P2' | 'P3';
  }>;
  xAxisName?: string;
  yAxisName?: string;
  extra?: ReactNode;
}
```
**实现要求**：
- 用Ant Design Card封装
- 图表用ECharts的热力图
- 配色严格遵循风险等级：P0→#ff4d4f，P1→#faad14，P2→#1890ff，P3→#52c41a
- 支持点击热力图单元格跳转到对应仓库/模块详情页
- 响应式高度：400px

##### 组件1.2.3：趋势图卡片（TrendChartCard）
**文件路径**：`src/components/TrendChartCard/index.tsx`
**Props定义**：
```typescript
interface TrendChartCardProps {
  title: string;
  data: Array<{
    date: string;
    scanCount?: number;
    refactoringCount?: number;
    successCount?: number;
    failedCount?: number;
  }>;
  extra?: ReactNode;
}
```
**实现要求**：
- 用Ant Design Card封装
- 图表用ECharts的折线图
- 配色：scanCount→#1677ff，refactoringCount→#52c41a，successCount→#52c41a，failedCount→#ff4d4f
- 支持日期范围筛选（默认近30天，可选近7天、近90天）
- 响应式高度：400px

##### 组件1.2.4：最近任务列表卡片（RecentTaskListCard）
**文件路径**：`src/components/RecentTaskListCard/index.tsx`
**Props定义**：
```typescript
interface RecentTaskListCardProps {
  title: string;
  taskType: 'scan' | 'refactoring';
  data: Array<{
    id: string;
    name: string;
    repositoryName: string;
    status: string;
    progress?: number;
    createdAt: string;
  }>;
  extra?: ReactNode;
}
```
**实现要求**：
- 用Ant Design Card封装
- 列表用Ant Design List，带avatar（任务类型图标）、title（任务名）、description（仓库名+创建时间）、extra（状态标签+进度条）
- 状态标签用Ant Design Tag，配色：pending→#86909c，running→#1677ff，completed→#52c41a，failed→#ff4d4f
- 支持点击任务跳转到任务详情页
- 响应式高度：400px

#### 1.3 状态管理（Zustand Store）
```typescript
// src/stores/dashboardStore.ts
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { getDashboardStats, getHeatmapData, getTrendData, getRecentTasks } from '../api/dashboard';

interface DashboardState {
  // 统计数据
  stats: {
    repositoryCount: number;
    scanTaskCount: number;
    refactoringTaskCount: number;
    technicalDebtCount: number;
    repositoryTrend?: { type: 'up' | 'down' | 'none'; value: number };
    scanTaskStatus?: { pending: number; running: number; completed: number; failed: number };
    refactoringTaskStatus?: { pending: number; running: number; completed: number; failed: number };
    technicalDebtRisk?: { P0: number; P1: number; P2: number; P3: number };
  };
  // 热力图数据
  heatmapData: Array<{
    name: string;
    value: number;
    riskLevel?: 'P0' | 'P1' | 'P2' | 'P3';
  }>;
  // 趋势图数据
  trendData: Array<{
    date: string;
    scanCount?: number;
    refactoringCount?: number;
    successCount?: number;
    failedCount?: number;
  }>;
  // 最近任务数据
  recentScanTasks: Array<{
    id: string;
    name: string;
    repositoryName: string;
    status: string;
    progress?: number;
    createdAt: string;
  }>;
  recentRefactoringTasks: Array<{
    id: string;
    name: string;
    repositoryName: string;
    status: string;
    progress?: number;
    createdAt: string;
  }>;
  // 加载状态
  loading: {
    stats: boolean;
    heatmap: boolean;
    trend: boolean;
    recentTasks: boolean;
  };
  // 错误状态
  error: {
    stats?: string;
    heatmap?: string;
    trend?: string;
    recentTasks?: string;
  };
  // 操作方法
  fetchStats: () => Promise<void>;
  fetchHeatmapData: (type: 'module' | 'risk') => Promise<void>;
  fetchTrendData: (days: 7 | 30 | 90) => Promise<void>;
  fetchRecentTasks: () => Promise<void>;
}

export const useDashboardStore = create<DashboardState>()(
  devtools(
    (set) => ({
      // 初始状态
      stats: {
        repositoryCount: 0,
        scanTaskCount: 0,
        refactoringTaskCount: 0,
        technicalDebtCount: 0,
      },
      heatmapData: [],
      trendData: [],
      recentScanTasks: [],
      recentRefactoringTasks: [],
      loading: {
        stats: false,
        heatmap: false,
        trend: false,
        recentTasks: false,
      },
      error: {},
      // 操作方法
      fetchStats: async () => {
        set({ loading: { ...useDashboardStore.getState().loading, stats: true }, error: { ...useDashboardStore.getState().error, stats: undefined } });
        try {
          const data = await getDashboardStats();
          set({ stats: data });
        } catch (err: any) {
          set({ error: { ...useDashboardStore.getState().error, stats: err.message } });
        } finally {
          set({ loading: { ...useDashboardStore.getState().loading, stats: false } });
        }
      },
      fetchHeatmapData: async (type) => {
        set({ loading: { ...useDashboardStore.getState().loading, heatmap: true }, error: { ...useDashboardStore.getState().error, heatmap: undefined } });
        try {
          const data = await getHeatmapData(type);
          set({ heatmapData: data });
        } catch (err: any) {
          set({ error: { ...useDashboardStore.getState().error, heatmap: err.message } });
        } finally {
          set({ loading: { ...useDashboardStore.getState().loading, heatmap: false } });
        }
      },
      fetchTrendData: async (days) => {
        set({ loading: { ...useDashboardStore.getState().loading, trend: true }, error: { ...useDashboardStore.getState().error, trend: undefined } });
        try {
          const data = await getTrendData(days);
          set({ trendData: data });
        } catch (err: any) {
          set({ error: { ...useDashboardStore.getState().error, trend: err.message } });
        } finally {
          set({ loading: { ...useDashboardStore.getState().loading, trend: false } });
        }
      },
      fetchRecentTasks: async () => {
        set({ loading: { ...useDashboardStore.getState().loading, recentTasks: true }, error: { ...useDashboardStore.getState().error, recentTasks: undefined } });
        try {
          const { scanTasks, refactoringTasks } = await getRecentTasks();
          set({ recentScanTasks: scanTasks, recentRefactoringTasks: refactoringTasks });
        } catch (err: any) {
          set({ error: { ...useDashboardStore.getState().error, recentTasks: err.message } });
        } finally {
          set({ loading: { ...useDashboardStore.getState().loading, recentTasks: false } });
        }
      },
    }),
    { name: 'dashboard-store' }
  )
);
```

#### 1.4 API调用（统一Axios封装）
```typescript
// src/api/index.ts（统一Axios封装）
import axios from 'axios';
import { message } from 'ant-design';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 这里可以添加Token认证
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      if (status === 401) {
        // 未登录，跳转到登录页
        localStorage.removeItem('token');
        window.location.href = '/login';
      } else if (status === 403) {
        message.error('无权限访问');
      } else if (status === 500) {
        message.error('服务器错误，请稍后重试');
      } else {
        message.error(data.message || '请求失败');
      }
    } else if (error.request) {
      message.error('网络错误，请检查网络连接');
    } else {
      message.error(error.message || '请求失败');
    }
    return Promise.reject(error);
  }
);

export default api;
```

```typescript
// src/api/dashboard.ts
import api from './index';

// 获取数据概览统计
export const getDashboardStats = async () => {
  return await api.get('/dashboard/stats');
};

// 获取热力图数据
export const getHeatmapData = async (type: 'module' | 'risk') => {
  return await api.get('/dashboard/heatmap', { params: { type } });
};

// 获取趋势图数据
export const getTrendData = async (days: 7 | 30 | 90) => {
  return await api.get('/dashboard/trend', { params: { days } });
};

// 获取最近任务
export const getRecentTasks = async () => {
  return await api.get('/dashboard/recent-tasks');
};
```

---

### 页面2：代码仓库列表（RepositoryList）
**文件路径**：`src/pages/Repository/RepositoryList/index.tsx`
**页面状态管理**：`src/stores/repositoryStore.ts`
**API调用**：统一封装在`src/api/repository.ts`
#### 2.1 页面布局
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ 顶部：全局ProLayout（固定）                                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ 内容区（padding:24px）                                                          │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 第一行：操作栏（24px间距）                                                    │ │
│ │ ┌─────────────────────────────────────────────────────────────────────────┐ │ │
│ │ │ 左侧：搜索框（placeholder="搜索仓库名/Git地址"，支持模糊搜索）            │ │ │
│ │ │ 中间：筛选器（语言筛选、风险等级筛选、扫描状态筛选）                        │ │ │
│ │ │ 右侧：新增仓库按钮（主色，带Git图标）                                      │ │ │
│ │ └─────────────────────────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 第二行：仓库列表表格（100%宽度）                                              │ │
│ │ ┌─────────────────────────────────────────────────────────────────────────┐ │ │
│ │ │ 列：仓库名（带Git图标，可点击跳转详情）、Git地址、语言分布、风险等级、    │ │ │
│ │ │      扫描状态、最后扫描时间、操作（查看详情、发起扫描、发起重构、删除）    │ │ │
│ │ └─────────────────────────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────────────────┐ │
│ │ 第三行：分页器（居中）                                                         │ │
│ └─────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘
```
#### 2.2 核心组件设计
##### 组件2.2.1：新增仓库模态框（AddRepositoryModal）
**文件路径**：`src/pages/Repository/RepositoryList/components/AddRepositoryModal.tsx`
**Props定义**：
```typescript
interface AddRepositoryModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}
```
**实现要求**：
- 用Ant Design Modal封装，title="新增代码仓库"，width=600px
- 表单用Ant Design Form，包含以下字段：
  1.  **仓库名**（必填，text，placeholder="请输入仓库名"，校验规则：长度2-50，只能包含字母、数字、下划线、连字符）
  2.  **Git地址**（必填，text，placeholder="请输入Git地址（HTTPS/SSH）"，校验规则：必须是有效的Git地址）
  3.  **目标分支**（必填，select，默认值="main"，选项：main、master、dev、自定义，自定义时显示输入框）
  4.  **访问凭证**（可选，text，placeholder="请输入访问Token（私有仓库必填）"，密码框显示）
  5.  **本地存储路径**（可选，text，placeholder="请输入本地存储路径（默认/tmp/code-repos）"）
- 提交按钮在底部，loading状态绑定API请求
- 提交成功后调用onSuccess，关闭模态框，刷新仓库列表

---

（由于篇幅限制，剩余核心页面：**代码仓库详情、扫描任务列表、扫描任务详情、重构任务列表、重构任务详情、分析报告列表、分析报告详情、系统设置**的详细设计，将采用**结构化JSON Schema+关键组件/API/状态管理摘要**的格式，确保AI开发工具可直接解析执行）

---

## 四、剩余核心页面结构化设计摘要
### 页面3：代码仓库详情（RepositoryDetail）
**路由**：`/repository/:repositoryId`
**核心功能**：
1.  仓库基础信息展示（仓库名、Git地址、分支、统计数据）
2.  模块树展示（可展开/折叠，带风险等级标签）
3.  代码资产热力图（按文件/风险）
4.  技术债务列表（可筛选、排序、导出）
5.  操作栏（发起全量扫描、发起增量扫描、发起重构、查看报告）
**关键组件**：ModuleTree、FileHeatmapCard、TechnicalDebtTable
**状态管理**：useRepositoryStore（扩展）
**API调用**：getRepositoryDetail、getModuleTree、getTechnicalDebtList、createScanTask、createRefactoringTask

---

### 页面4：扫描任务列表（ScanTaskList）
**路由**：`/scan`
**核心功能**：
1.  搜索框（搜索任务名/仓库名）
2.  筛选器（任务状态、仓库、扫描类型：全量/增量/模块）
3.  扫描任务表格（列：任务名、仓库名、扫描类型、状态、进度、创建时间、操作：查看详情、暂停/继续、取消、删除）
4.  分页器
**关键组件**：无（复用通用组件）
**状态管理**：useScanTaskStore
**API调用**：getScanTaskList、pauseScanTask、resumeScanTask、cancelScanTask、deleteScanTask

---

### 页面5：扫描任务详情（ScanTaskDetail）
**路由**：`/scan/:taskId`
**核心功能**：
1.  任务基础信息展示（任务名、仓库名、扫描类型、状态、进度、创建时间、耗时）
2.  实时进度条（带步骤：克隆仓库→解析代码→语义提取→依赖分析→风险评估→生成报告）
3.  实时日志展示（可滚动、可筛选级别：INFO/WARNING/ERROR）
4.  扫描结果预览（统计数据、热力图、风险清单）
5.  操作栏（查看完整报告、导出报告、重新扫描）
**关键组件**：StepProgressBar、RealTimeLogViewer
**状态管理**：useScanTaskStore（扩展）
**API调用**：getScanTaskDetail、getScanTaskLogs、getScanTaskPreview、exportScanReport、retryScanTask

---

### 页面6：重构任务列表（RefactoringTaskList）
**路由**：`/refactoring`
**核心功能**：
1.  搜索框（搜索任务名/仓库名）
2.  筛选器（任务状态、仓库、重构类型：解耦/拆分/版本升级/框架迁移）
3.  重构任务表格（列：任务名、仓库名、重构类型、状态、进度、创建时间、操作：查看详情、暂停/继续、取消、删除）
4.  分页器
**关键组件**：无（复用通用组件）
**状态管理**：useRefactoringTaskStore
**API调用**：getRefactoringTaskList、pauseRefactoringTask、resumeRefactoringTask、cancelRefactoringTask、deleteRefactoringTask

---

### 页面7：重构任务详情（RefactoringTaskDetail）
**路由**：`/refactoring/:taskId`
**核心功能**：
1.  任务基础信息展示（任务名、仓库名、重构类型、状态、进度、创建时间、耗时）
2.  实时进度条（带步骤：需求拆解→上下文检索→重构规划→代码生成→验证修复→合并更新）
3.  原子任务列表（可展开/折叠，带状态、进度、操作：查看详情、跳过、回滚）
4.  实时日志展示（可滚动、可筛选级别：INFO/WARNING/ERROR）
5.  重构结果预览（修改文件数、代码行数变化、技术债务减少量）
6.  操作栏（查看完整报告、导出报告、合并代码、回滚代码、重新重构）
**关键组件**：AtomicTaskList、CodeDiffViewer
**状态管理**：useRefactoringTaskStore（扩展）
**API调用**：getRefactoringTaskDetail、getRefactoringTaskLogs、getAtomicTaskList、getCodeDiff、exportRefactoringReport、mergeRefactoringCode、rollbackRefactoringCode、retryRefactoringTask

---

### 页面8：分析报告列表（ReportList）
**路由**：`/report`
**核心功能**：
1.  搜索框（搜索报告名/仓库名）
2.  筛选器（报告类型：扫描报告/重构报告、仓库、创建时间范围）
3.  分析报告表格（列：报告名、仓库名、报告类型、创建时间、操作：查看详情、导出、删除）
4.  分页器
**关键组件**：无（复用通用组件）
**状态管理**：useReportStore
**API调用**：getReportList、exportReport、deleteReport

---

### 页面9：分析报告详情（ReportDetail）
**路由**：`/report/:reportId`
**核心功能**：
1.  报告基础信息展示（报告名、仓库名、报告类型、创建时间、生成耗时）
2.  报告目录（可点击跳转对应章节）
3.  报告内容（分章节展示：代码资产概览、技术债务分析、依赖关系分析、重构建议、重构收益预估）
4.  图表展示（热力图、趋势图、分布图）
5.  操作栏（导出PDF、导出HTML、分享报告）
**关键组件**：ReportTableOfContents、ReportContentRenderer
**状态管理**：useReportStore（扩展）
**API调用**：getReportDetail、exportReportAsPDF、exportReportAsHTML、shareReport

---

### 页面10：系统设置（Settings）
**路由**：`/settings`
**核心功能**：
1.  基础设置（平台名称、Logo、默认分支、默认本地存储路径）
2.  LLM设置（模型选择、API密钥、API地址、温度、最大Token数）
3.  数据库设置（PostgreSQL、Milvus、Neo4j、Redis的连接配置）
4.  任务队列设置（Celery Worker数量、并发数、超时时间）
5.  扫描规则设置（文件过滤规则、语言过滤规则、复杂度阈值）
6.  重构规则设置（编码规范、架构规范、风险等级阈值）
**关键组件**：SettingsForm（分Tab展示）
**状态管理**：useSettingsStore
**API调用**：getSettings、updateSettings、testLLMConnection、testDatabaseConnection、testTaskQueueConnection

---

## 五、通用组件库（必须优先实现）
### 通用组件清单
| 组件名 | 文件路径 | 用途 |
|--------|----------|------|
| StatCard | src/components/StatCard/index.tsx | 数据概览统计卡片 |
| HeatmapCard | src/components/HeatmapCard/index.tsx | 热力图卡片 |
| TrendChartCard | src/components/TrendChartCard/index.tsx | 趋势图卡片 |
| RecentTaskListCard | src/components/RecentTaskListCard/index.tsx | 最近任务列表卡片 |
| StepProgressBar | src/components/StepProgressBar/index.tsx | 步骤进度条 |
| RealTimeLogViewer | src/components/RealTimeLogViewer/index.tsx | 实时日志查看器 |
| CodeDiffViewer | src/components/CodeDiffViewer/index.tsx | 代码差异查看器（基于react-diff-viewer） |
| ModuleTree | src/components/ModuleTree/index.tsx | 模块树 |
| TechnicalDebtTable | src/components/TechnicalDebtTable/index.tsx | 技术债务表格 |
| ReportTableOfContents | src/components/ReportTableOfContents/index.tsx | 报告目录 |
| ReportContentRenderer | src/components/ReportContentRenderer/index.tsx | 报告内容渲染器（支持Markdown） |

---

## 六、最终验收标准
1.  **功能完整性**：实现本文档定义的所有页面、组件、功能，无遗漏
2.  **代码质量**：全量代码符合ESLint + Prettier + TypeScript严格模式，核心组件测试覆盖率≥80%
3.  **交互体验**：所有交互有加载状态、错误提示、成功反馈，响应式适配1366x768及以上桌面端
4.  **API对接**：所有数据通过统一封装的Axios API获取，与后端接口完全对接
5.  **视觉规范**：严格遵循Ant Design 5.20+默认企业级配色、布局、交互规范
6.  **性能要求**：页面加载时间≤2s，表格分页切换≤1s，图表渲染≤1s