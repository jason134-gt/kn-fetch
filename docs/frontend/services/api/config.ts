/**
 * Axios 配置
 * 创建配置好的 axios 实例
 */

import axios from 'axios';

// 创建 axios 实例
const api = axios.create({
  baseURL: '/api',  // 使用 Vite 代理
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 可以在这里添加认证 token
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
    // 统一错误处理
    if (error.response) {
      const { status } = error.response;
      if (status === 401) {
        console.error('未授权，请重新登录');
      } else if (status === 403) {
        console.error('拒绝访问');
      } else if (status === 404) {
        console.error('请求的资源不存在');
      } else if (status >= 500) {
        console.error('服务器错误');
      }
    } else if (error.request) {
      console.error('网络错误，请检查网络连接');
    }
    return Promise.reject(error);
  }
);

export default api;
