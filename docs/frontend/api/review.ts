/**
 * 审核工作流 API 模块
 * 文件路径: src/api/review.ts
 */

import api from './config';
import type {
  ReviewRecord,
  ReviewStatus,
  ReviewAction,
  ActionRecord,
  ConvergenceResult,
  ExecuteReviewActionInput,
} from '../api-types';

/**
 * 创建审核记录
 */
export const createReview = async (data: {
  sessionId: string;
  reportId: string;
}): Promise<ReviewRecord> => {
  return api.post('/review/create', data);
};

/**
 * 执行审核动作
 */
export const executeReviewAction = async (
  data: ExecuteReviewActionInput
): Promise<ReviewRecord> => {
  return api.post('/review/action', data);
};

/**
 * 获取审核状态
 */
export const getReviewStatus = async (
  recordId: string
): Promise<ReviewRecord> => {
  return api.get(`/review/status/${recordId}`);
};

/**
 * 获取允许的动作列表
 */
export const getAllowedActions = async (
  recordId: string
): Promise<string[]> => {
  return api.get(`/review/actions/${recordId}`);
};

/**
 * 获取审核历史记录
 */
export const getReviewHistory = async (
  recordId: string
): Promise<ActionRecord[]> => {
  return api.get(`/review/history/${recordId}`);
};

/**
 * 检查收敛状态
 */
export const checkConvergence = async (
  sessionId: string
): Promise<ConvergenceResult> => {
  return api.get(`/review/convergence/${sessionId}`);
};

/**
 * 获取审核记录列表
 */
export const getReviewRecords = async (params: {
  sessionId?: string;
  status?: ReviewStatus;
  page?: number;
  pageSize?: number;
}): Promise<{
  items: ReviewRecord[];
  total: number;
}> => {
  return api.get('/review/records', { params });
};

/**
 * 批准报告
 */
export const approveReport = async (
  recordId: string,
  comments?: string
): Promise<ReviewRecord> => {
  return executeReviewAction({
    sessionId: '',
    recordId,
    action: 'approve' as ReviewAction,
    comments,
  });
};

/**
 * 拒绝报告
 */
export const rejectReport = async (
  recordId: string,
  comments: string
): Promise<ReviewRecord> => {
  return executeReviewAction({
    sessionId: '',
    recordId,
    action: 'reject' as ReviewAction,
    comments,
  });
};

/**
 * 定稿报告
 */
export const finalizeReport = async (
  recordId: string,
  comments?: string
): Promise<ReviewRecord> => {
  return executeReviewAction({
    sessionId: '',
    recordId,
    action: 'finalize' as ReviewAction,
    comments,
  });
};
