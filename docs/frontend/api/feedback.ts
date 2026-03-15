/**
 * 前端 API 对接实现
 * 文件路径: src/api/feedback.ts
 */

import api from './config';
import type {
  ApiResponse,
  FeedbackBatch,
  FeedbackSession,
  UserFeedback,
  CreateFeedbackInput,
  CreateFeedbackBatchInput,
  ReviewRecord,
  ConvergenceResult,
  ExecuteReviewActionInput,
  RefactorReport,
} from '../api-types';

// ============================================================
// 反馈相关 API
// ============================================================

/**
 * 提交单个反馈
 */
export async function submitFeedback(
  input: CreateFeedbackInput
): Promise<UserFeedback> {
  const response = await api.post<ApiResponse<UserFeedback>>('/feedback/submit', input);
  return response.data;
}

/**
 * 批量提交反馈
 */
export async function submitFeedbackBatch(
  input: CreateFeedbackBatchInput
): Promise<FeedbackBatch> {
  const response = await api.post<ApiResponse<FeedbackBatch>>('/feedback/batch', input);
  return response.data;
}

/**
 * 获取反馈历史
 */
export async function getFeedbackHistory(
  sessionId: string
): Promise<UserFeedback[]> {
  const response = await api.get<ApiResponse<UserFeedback[]>>(
    `/feedback/history/${sessionId}`
  );
  return response.data;
}

/**
 * 重新生成报告
 */
export async function regenerateReport(input: {
  sessionId: string;
  feedbackBatchId: string;
}): Promise<RefactorReport> {
  const response = await api.post<ApiResponse<RefactorReport>>(
    '/feedback/regenerate',
    input
  );
  return response.data;
}

/**
 * 获取报告版本列表
 */
export async function getReportVersions(
  sessionId: string
): Promise<RefactorReport[]> {
  const response = await api.get<ApiResponse<RefactorReport[]>>(
    `/feedback/versions/${sessionId}`
  );
  return response.data;
}

// ============================================================
// 审核工作流 API
// ============================================================

/**
 * 创建审核
 */
export async function createReview(input: {
  sessionId: string;
  reportId: string;
}): Promise<ReviewRecord> {
  const response = await api.post<ApiResponse<ReviewRecord>>(
    '/review/create',
    input
  );
  return response.data;
}

/**
 * 执行审核动作
 */
export async function executeReviewAction(
  input: ExecuteReviewActionInput
): Promise<ReviewRecord> {
  const response = await api.post<ApiResponse<ReviewRecord>>(
    '/review/action',
    input
  );
  return response.data;
}

/**
 * 获取审核状态
 */
export async function getReviewStatus(
  recordId: string
): Promise<ReviewRecord> {
  const response = await api.get<ApiResponse<ReviewRecord>>(
    `/review/status/${recordId}`
  );
  return response.data;
}

/**
 * 获取允许的动作
 */
export async function getAllowedActions(
  recordId: string
): Promise<string[]> {
  const response = await api.get<ApiResponse<{ allowedActions: string[] }>>(
    `/review/actions/${recordId}`
  );
  return response.data.allowedActions;
}

/**
 * 获取审核历史
 */
export async function getReviewHistory(
  recordId: string
): Promise<ReviewRecord['actionHistory']> {
  const response = await api.get<ApiResponse<ReviewRecord['actionHistory']>>(
    `/review/history/${recordId}`
  );
  return response.data;
}

/**
 * 检查收敛状态
 */
export async function checkConvergence(
  sessionId: string
): Promise<ConvergenceResult> {
  const response = await api.get<ApiResponse<ConvergenceResult>>(
    `/review/convergence/${sessionId}`
  );
  return response.data;
}

// ============================================================
// 会话管理 API
// ============================================================

/**
 * 创建反馈会话
 */
export async function createFeedbackSession(input: {
  projectName: string;
  reportId: string;
}): Promise<FeedbackSession> {
  const response = await api.post<ApiResponse<FeedbackSession>>(
    '/feedback/session/create',
    input
  );
  return response.data;
}

/**
 * 获取会话详情
 */
export async function getFeedbackSession(
  sessionId: string
): Promise<FeedbackSession> {
  const response = await api.get<ApiResponse<FeedbackSession>>(
    `/feedback/session/${sessionId}`
  );
  return response.data;
}

/**
 * 获取会话列表
 */
export async function getFeedbackSessions(params?: {
  status?: string;
  projectName?: string;
}): Promise<FeedbackSession[]> {
  const response = await api.get<ApiResponse<FeedbackSession[]>>(
    '/feedback/sessions',
    { params }
  );
  return response.data;
}
