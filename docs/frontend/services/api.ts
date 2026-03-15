/**
 * API 服务层
 * 封装所有 API 调用，提供类型安全的接口
 */

import api from './api/config';
import type {
  DashboardStats,
  KnowledgeStats,
  RefactorStats,
  RecentTasks,
  ExtractTask,
  ExtractTaskDetail,
  KnowledgeEntity,
  RefactorTask,
  RefactorReport,
  FeedbackSession,
  Report,
  PageResult,
} from '../api-types';

// ============================================================
// Dashboard API
// ============================================================

export const dashboardApi = {
  /** 获取仪表盘统计数据 */
  getStats: () => 
    api.get<any, DashboardStats>('/dashboard/stats'),
  
  /** 获取知识图谱统计 */
  getKnowledgeStats: () => 
    api.get<any, KnowledgeStats>('/dashboard/knowledge-stats'),
  
  /** 获取重构问题统计 */
  getRefactorStats: () => 
    api.get<any, RefactorStats>('/dashboard/refactor-stats'),
  
  /** 获取最近任务 */
  getRecentTasks: () => 
    api.get<any, RecentTasks>('/dashboard/recent-tasks'),
};

// ============================================================
// Extract Task API
// ============================================================

export const extractApi = {
  /** 获取提取任务列表 */
  getTasks: (params?: {
    status?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
  }) => 
    api.get<any, PageResult<ExtractTask>>('/extract/tasks', { params }),
  
  /** 获取提取任务详情 */
  getTaskDetail: (taskId: string) => 
    api.get<any, ExtractTaskDetail>(`/extract/tasks/${taskId}`),
  
  /** 创建提取任务 */
  createTask: (data: {
    projectName: string;
    projectPath: string;
    languages: string[];
    includeTestFiles?: boolean;
    maxFileSize?: number;
  }) => 
    api.post<any, { taskId: string }>('/extract/tasks', data),
  
  /** 暂停任务 */
  pauseTask: (taskId: string) => 
    api.post(`/extract/tasks/${taskId}/pause`),
  
  /** 恢复任务 */
  resumeTask: (taskId: string) => 
    api.post(`/extract/tasks/${taskId}/resume`),
  
  /** 取消任务 */
  cancelTask: (taskId: string) => 
    api.post(`/extract/tasks/${taskId}/cancel`),
  
  /** 删除任务 */
  deleteTask: (taskId: string) => 
    api.delete(`/extract/tasks/${taskId}`),
  
  /** 获取知识实体列表 */
  getEntities: (taskId: string, params?: {
    type?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
  }) => 
    api.get<any, PageResult<KnowledgeEntity>>(`/extract/tasks/${taskId}/entities`, { params }),
};

// ============================================================
// Refactor Task API
// ============================================================

export const refactorApi = {
  /** 获取重构任务列表 */
  getTasks: (params?: {
    status?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
  }) => 
    api.get<any, PageResult<RefactorTask>>('/refactor/tasks', { params }),
  
  /** 获取重构报告详情 */
  getReport: (taskId: string) => 
    api.get<any, RefactorReport>(`/refactor/tasks/${taskId}`),
  
  /** 创建重构分析任务 */
  createTask: (data: {
    projectName: string;
    knowledgePath: string;
  }) => 
    api.post<any, { taskId: string }>('/refactor/tasks', data),
  
  /** 删除任务 */
  deleteTask: (taskId: string) => 
    api.delete(`/refactor/tasks/${taskId}`),
};

// ============================================================
// Feedback API
// ============================================================

export const feedbackApi = {
  /** 获取反馈会话列表 */
  getSessions: (params?: {
    status?: string;
    page?: number;
    pageSize?: number;
  }) => 
    api.get<any, PageResult<FeedbackSession>>('/feedback/sessions', { params }),
  
  /** 获取会话详情 */
  getSession: (sessionId: string) => 
    api.get<any, FeedbackSession>(`/feedback/sessions/${sessionId}`),
  
  /** 通过审核 */
  approve: (sessionId: string) => 
    api.post(`/feedback/sessions/${sessionId}/approve`),
  
  /** 拒绝审核 */
  reject: (sessionId: string) => 
    api.post(`/feedback/sessions/${sessionId}/reject`),
  
  /** 请求修改 */
  requestChanges: (sessionId: string) => 
    api.post(`/feedback/sessions/${sessionId}/request-changes`),
};

// ============================================================
// Report API
// ============================================================

export const reportApi = {
  /** 获取报告列表 */
  getReports: (params?: {
    status?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
  }) => 
    api.get<any, PageResult<Report>>('/reports', { params }),
  
  /** 获取报告详情 */
  getReport: (reportId: string) => 
    api.get<any, Report>(`/reports/${reportId}`),
  
  /** 导出报告 */
  exportReport: (reportId: string, format: string = 'markdown') => 
    api.post(`/reports/${reportId}/export`, { format }),
  
  /** 归档报告 */
  archiveReport: (reportId: string) => 
    api.post(`/reports/${reportId}/archive`),
};

// ============================================================
// Settings API
// ============================================================

export const settingsApi = {
  /** 获取系统配置 */
  getSettings: () => 
    api.get<any, any>('/settings'),
  
  /** 更新系统配置 */
  updateSettings: (data: any) => 
    api.put('/settings', data),
  
  /** 获取 AI 配置 */
  getAiConfig: () => 
    api.get<any, any>('/settings/ai-config'),
  
  /** 更新 AI 配置 */
  updateAiConfig: (data: any) => 
    api.put('/settings/ai-config', data),
};

// 默认导出所有 API
export default {
  dashboard: dashboardApi,
  extract: extractApi,
  refactor: refactorApi,
  feedback: feedbackApi,
  report: reportApi,
  settings: settingsApi,
};
