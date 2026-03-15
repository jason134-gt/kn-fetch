/**
 * 重构分析 API 模块
 * 文件路径: src/api/refactor.ts
 */

import api from './config';
import type {
  PageResult,
  RefactorTask,
  RefactorTaskDetail,
  RefactorReport,
  CreateRefactorTaskInput,
  RefactorConfig,
} from '../api-types';

/**
 * 获取重构任务列表
 */
export const getRefactorTaskList = async (params: {
  page?: number;
  pageSize?: number;
  status?: string;
  severity?: string;
  keyword?: string;
}): Promise<PageResult<RefactorTask>> => {
  return api.get('/refactor/tasks', { params });
};

/**
 * 创建重构分析任务
 */
export const createRefactorTask = async (
  data: CreateRefactorTaskInput
): Promise<RefactorTask> => {
  return api.post('/refactor/tasks', data);
};

/**
 * 获取重构任务详情
 */
export const getRefactorTaskDetail = async (
  taskId: string
): Promise<RefactorTaskDetail> => {
  return api.get(`/refactor/tasks/${taskId}`);
};

/**
 * 获取重构分析报告
 */
export const getRefactorReport = async (
  taskId: string
): Promise<RefactorReport> => {
  return api.get(`/refactor/tasks/${taskId}/report`);
};

/**
 * 获取报告详情（通过 reportId）
 */
export const getReportById = async (
  reportId: string
): Promise<RefactorReport> => {
  return api.get(`/refactor/reports/${reportId}`);
};

/**
 * 删除重构任务
 */
export const deleteRefactorTask = async (taskId: string): Promise<void> => {
  return api.delete(`/refactor/tasks/${taskId}`);
};

/**
 * 取消重构任务
 */
export const cancelRefactorTask = async (taskId: string): Promise<void> => {
  return api.post(`/refactor/tasks/${taskId}/cancel`);
};

/**
 * 获取重构配置模板
 */
export const getRefactorConfigTemplate = async (
  language?: string
): Promise<RefactorConfig> => {
  return api.get('/refactor/config/template', { params: { language } });
};

/**
 * 导出重构报告
 */
export const exportRefactorReport = async (
  reportId: string,
  format: 'pdf' | 'html' | 'json'
): Promise<{ url: string }> => {
  return api.get(`/refactor/reports/${reportId}/export/${format}`);
};
