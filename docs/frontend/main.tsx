/**
 * 应用入口文件
 */

import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/index.less';

// 配置 dayjs
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

// 全局错误处理
const handleError = (error: Error) => {
  console.error('Global error:', error);
  // 可以发送到错误监控系统
};

window.onerror = (message, source, lineno, colno, error) => {
  handleError(error || new Error(String(message)));
  return false;
};

window.onunhandledrejection = (event) => {
  handleError(event.reason);
};

// 渲染应用
const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);

root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
