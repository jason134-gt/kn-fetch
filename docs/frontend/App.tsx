/**
 * 主应用入口组件
 * 包含路由配置、全局布局、Provider 配置
 */

import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation, Outlet } from 'react-router-dom';
import { ConfigProvider, Layout, Menu, theme, Breadcrumb, Dropdown, Avatar, Space } from 'antd';
import {
  DashboardOutlined,
  DatabaseOutlined,
  CodeOutlined,
  CommentOutlined,
  FileTextOutlined,
  SettingOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import zhCN from 'antd/locale/zh_CN';

// 导入页面组件
import Dashboard from './pages/Dashboard';
import ExtractTaskList from './pages/ExtractTaskList';
import ExtractTaskDetail from './pages/ExtractTaskDetail';
import RefactorTaskList from './pages/RefactorTaskList';
import RefactorReport from './pages/RefactorReport';
import FeedbackSessionList from './pages/FeedbackSessionList';
import ReviewWorkflow from './pages/ReviewWorkflow';
import ReportManagement from './pages/ReportManagement';
import Settings from './pages/Settings';

// 导入 Store Providers
import { useDashboardStore } from './stores/dashboardStore';
import { useExtractStore } from './stores/extractStore';
import { useRefactorStore } from './stores/refactorStore';
import { useFeedbackStore } from './stores/feedbackStore';

const { Header, Sider, Content } = Layout;

// 菜单配置
const menuItems = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: '仪表盘',
  },
  {
    key: '/extract-tasks',
    icon: <DatabaseOutlined />,
    label: '知识提取',
  },
  {
    key: '/refactor-tasks',
    icon: <CodeOutlined />,
    label: '重构分析',
  },
  {
    key: '/feedback',
    icon: <CommentOutlined />,
    label: '反馈闭环',
  },
  {
    key: '/reports',
    icon: <FileTextOutlined />,
    label: '报告管理',
  },
  {
    key: '/settings',
    icon: <SettingOutlined />,
    label: '系统设置',
  },
];

// 主布局组件
const MainLayout: React.FC = () => {
  const [collapsed, setCollapsed] = React.useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  // 获取当前选中的菜单项
  const getSelectedKey = () => {
    const path = location.pathname;
    // 处理详情页面的菜单选中状态
    if (path.startsWith('/extract-tasks')) return '/extract-tasks';
    if (path.startsWith('/refactor-tasks')) return '/refactor-tasks';
    if (path.startsWith('/feedback') || path === '/review-workflow') return '/feedback';
    if (path.startsWith('/reports')) return '/reports';
    return path;
  };

  // 初始化 WebSocket 连接（暂时禁用，后端服务未启动）
  useEffect(() => {
    // WebSocket 连接暂时禁用
    // const ws = new WebSocket('ws://localhost:8000/ws');
    // ws.onmessage = (event) => {
    //   const data = JSON.parse(event.data);
    //   switch (data.type) {
    //     case 'task_progress':
    //       useExtractStore.getState().updateTaskProgress?.(data.payload);
    //       break;
    //   }
    // };
    // return () => ws.close();
  }, []);

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider 
        collapsible 
        collapsed={collapsed} 
        onCollapse={setCollapsed}
        theme="light"
      >
        <div className="logo">
          <h2>{collapsed ? 'KN' : 'KN-Fetch'}</h2>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[getSelectedKey()]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ 
          padding: '0 24px', 
          background: '#fff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}>
          <Breadcrumb>
            <Breadcrumb.Item>首页</Breadcrumb.Item>
            <Breadcrumb.Item>
              {menuItems.find(item => item.key === location.pathname)?.label}
            </Breadcrumb.Item>
          </Breadcrumb>
          <UserMenu />
        </Header>
        <Content style={{ margin: '24px', background: '#fff', padding: '24px', borderRadius: '8px' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

// 用户菜单组件
const UserMenu: React.FC = () => {
  const items = [
    {
      key: 'profile',
      label: '个人设置',
      icon: <UserOutlined />,
    },
    {
      key: 'logout',
      label: '退出登录',
      icon: <LogoutOutlined />,
    },
  ];

  return (
    <Dropdown menu={{ items }} placement="bottomRight">
      <Space>
        <Avatar icon={<UserOutlined />} />
        <span>管理员</span>
      </Space>
    </Dropdown>
  );
};

// 路由配置
const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route element={<MainLayout />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/extract" element={<ExtractTaskList />} />
        <Route path="/extract/:id" element={<ExtractTaskDetail />} />
        <Route path="/refactor" element={<RefactorTaskList />} />
        <Route path="/refactor/:id" element={<RefactorReport />} />
        <Route path="/feedback" element={<FeedbackSessionList />} />
        <Route path="/feedback/:sessionId" element={<ReviewWorkflow />} />
        <Route path="/report" element={<ReportManagement />} />
        <Route path="/settings" element={<Settings />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};

// 主应用组件
const App: React.FC = () => {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#1890ff',
          borderRadius: 6,
        },
      }}
    >
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </ConfigProvider>
  );
};

export default App;
