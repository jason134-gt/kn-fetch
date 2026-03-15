// src/hooks/useReview.ts
import { useCallback, useEffect, useState } from 'react';
import { message } from 'antd';
import * as reviewApi from '../api/review';
import { useFeedbackStore } from '../stores/feedbackStore';

// ========== 类型定义 ==========

export type ReviewStatus =
  | 'draft'
  | 'pending_review'
  | 'in_review'
  | 'feedback_submitted'
  | 'regenerating'
  | 'approved'
  | 'rejected'
  | 'finalized';

export type ReviewAction =
  | 'submit'
  | 'start_review'
  | 'submit_feedback'
  | 'request_regenerate'
  | 'approve'
  | 'reject'
  | 'finalize';

export interface ActionRecord {
  id: string;
  recordId: string;
  action: ReviewAction;
  userId: string;
  userName: string;
  comments?: string;
  metadata?: Record<string, any>;
  timestamp: string;
}

export interface ReviewRecord {
  recordId: string;
  sessionId: string;
  reportId: string;
  status: ReviewStatus;
  version: number;
  regenerateCount: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  history: ActionRecord[];
}

export interface ConvergenceResult {
  converged: boolean;
  reason: string;
  changeRate: number;
  confirmationRate: number;
  ignoreRate: number;
  details: {
    totalProblems: number;
    confirmedProblems: number;
    ignoredProblems: number;
    modifiedProblems: number;
    addedProblems: number;
    removedProblems: number;
  };
}

export interface AllowedActions {
  actions: ReviewAction[];
  currentStatus: ReviewStatus;
}

// ========== Hook 返回类型 ==========

interface UseReviewReturn {
  // 状态
  reviewRecord: ReviewRecord | null;
  allowedActions: ReviewAction[];
  convergence: ConvergenceResult | null;
  loading: boolean;
  error: string | null;
  
  // 操作
  createReview: (sessionId: string, reportId: string) => Promise<ReviewRecord>;
  executeAction: (action: ReviewAction, comments?: string, metadata?: Record<string, any>) => Promise<void>;
  fetchReviewStatus: () => Promise<void>;
  fetchAllowedActions: () => Promise<void>;
  checkConvergence: () => Promise<ConvergenceResult>;
  
  // 状态判断
  canApprove: boolean;
  canReject: boolean;
  canRegenerate: boolean;
  canSubmitFeedback: boolean;
  isFinalized: boolean;
  isPendingReview: boolean;
}

// ========== Hook 实现 ==========

export function useReview(sessionId: string): UseReviewReturn {
  const [reviewRecord, setReviewRecord] = useState<ReviewRecord | null>(null);
  const [allowedActions, setAllowedActions] = useState<ReviewAction[]>([]);
  const [convergence, setConvergence] = useState<ConvergenceResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const { currentSession } = useFeedbackStore();
  
  // 创建审核
  const createReview = useCallback(async (sid: string, reportId: string) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await reviewApi.createReview({
        sessionId: sid,
        reportId,
      });
      setReviewRecord(response.data);
      return response.data;
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || '创建审核失败';
      setError(errorMsg);
      message.error(errorMsg);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);
  
  // 执行审核动作
  const executeAction = useCallback(async (
    action: ReviewAction,
    comments?: string,
    metadata?: Record<string, any>
  ) => {
    if (!reviewRecord) {
      message.error('审核记录不存在');
      return;
    }
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await reviewApi.executeReviewAction({
        sessionId,
        recordId: reviewRecord.recordId,
        action,
        comments,
        metadata,
      });
      
      setReviewRecord(response.data);
      
      // 执行成功提示
      const actionMessages: Record<ReviewAction, string> = {
        submit: '已提交审核',
        start_review: '已开始审核',
        submit_feedback: '已提交反馈',
        request_regenerate: '已请求重新生成',
        approve: '已批准',
        reject: '已拒绝',
        finalize: '已定稿',
      };
      
      message.success(actionMessages[action]);
      
      // 刷新允许的动作
      await fetchAllowedActions();
      
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || '执行操作失败';
      setError(errorMsg);
      message.error(errorMsg);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [sessionId, reviewRecord]);
  
  // 获取审核状态
  const fetchReviewStatus = useCallback(async () => {
    if (!reviewRecord?.recordId) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await reviewApi.getReviewStatus(reviewRecord.recordId);
      setReviewRecord(response.data);
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || '获取审核状态失败';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  }, [reviewRecord?.recordId]);
  
  // 获取允许的动作
  const fetchAllowedActions = useCallback(async () => {
    if (!reviewRecord?.recordId) return;
    
    try {
      const response = await reviewApi.getAllowedActions(reviewRecord.recordId);
      setAllowedActions(response.data.actions);
    } catch (err: any) {
      console.error('获取允许动作失败:', err);
    }
  }, [reviewRecord?.recordId]);
  
  // 检查收敛状态
  const checkConvergence = useCallback(async () => {
    setLoading(true);
    
    try {
      const response = await reviewApi.checkConvergence(sessionId);
      setConvergence(response.data);
      return response.data;
    } catch (err: any) {
      console.error('检查收敛失败:', err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [sessionId]);
  
  // 初始化时获取允许的动作
  useEffect(() => {
    if (reviewRecord?.recordId) {
      fetchAllowedActions();
    }
  }, [reviewRecord?.recordId, fetchAllowedActions]);
  
  // 状态判断
  const canApprove = allowedActions.includes('approve');
  const canReject = allowedActions.includes('reject');
  const canRegenerate = allowedActions.includes('request_regenerate');
  const canSubmitFeedback = allowedActions.includes('submit_feedback');
  const isFinalized = reviewRecord?.status === 'finalized';
  const isPendingReview = reviewRecord?.status === 'pending_review';
  
  return {
    reviewRecord,
    allowedActions,
    convergence,
    loading,
    error,
    
    createReview,
    executeAction,
    fetchReviewStatus,
    fetchAllowedActions,
    checkConvergence,
    
    canApprove,
    canReject,
    canRegenerate,
    canSubmitFeedback,
    isFinalized,
    isPendingReview,
  };
}

// ========== 辅助 Hook: 审核历史 ==========

export function useReviewHistory(recordId: string) {
  const [history, setHistory] = useState<ActionRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const fetchHistory = useCallback(async () => {
    if (!recordId) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await reviewApi.getReviewHistory(recordId);
      setHistory(response.data);
    } catch (err: any) {
      setError(err.response?.data?.message || '获取审核历史失败');
    } finally {
      setLoading(false);
    }
  }, [recordId]);
  
  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);
  
  return {
    history,
    loading,
    error,
    refresh: fetchHistory,
  };
}

// ========== 辅助 Hook: 工作流状态图 ==========

export function useWorkflowDiagram(reviewRecord: ReviewRecord | null) {
  const getStatusPosition = useCallback((status: ReviewStatus): number => {
    const positions: Record<ReviewStatus, number> = {
      draft: 0,
      pending_review: 1,
      in_review: 2,
      feedback_submitted: 3,
      regenerating: 4,
      approved: 5,
      rejected: 6,
      finalized: 7,
    };
    return positions[status];
  }, []);
  
  const getActionLabel = useCallback((action: ReviewAction): string => {
    const labels: Record<ReviewAction, string> = {
      submit: '提交审核',
      start_review: '开始审核',
      submit_feedback: '提交反馈',
      request_regenerate: '请求重生成',
      approve: '批准',
      reject: '拒绝',
      finalize: '定稿',
    };
    return labels[action];
  }, []);
  
  const getStatusLabel = useCallback((status: ReviewStatus): string => {
    const labels: Record<ReviewStatus, string> = {
      draft: '草稿',
      pending_review: '待审核',
      in_review: '审核中',
      feedback_submitted: '反馈提交',
      regenerating: '重生成中',
      approved: '已批准',
      rejected: '已拒绝',
      finalized: '已定稿',
    };
    return labels[status];
  }, []);
  
  // 计算工作流路径
  const workflowPath = useMemo(() => {
    if (!reviewRecord?.history) return [];
    
    return reviewRecord.history.map(record => ({
      status: reviewRecord.status,
      action: record.action,
      label: getActionLabel(record.action),
      timestamp: record.timestamp,
      user: record.userName,
    }));
  }, [reviewRecord, getActionLabel]);
  
  return {
    getStatusPosition,
    getActionLabel,
    getStatusLabel,
    workflowPath,
    currentPosition: reviewRecord ? getStatusPosition(reviewRecord.status) : 0,
  };
}

import { useMemo } from 'react';
