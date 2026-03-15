/**
 * 反馈闭环 Hook
 * 文件路径: src/hooks/useFeedback.ts
 */

import { useState, useCallback, useEffect } from 'react';
import { message } from 'antd';
import { useFeedbackStore } from '../stores/feedbackStore';
import type {
  FeedbackType,
  Severity,
  Solution,
  CreateFeedbackInput,
  UserFeedback,
} from '../types';

interface UseFeedbackOptions {
  sessionId: string;
  problemId?: string;
  onSuccess?: (feedback: UserFeedback) => void;
  onError?: (error: Error) => void;
}

interface FeedbackInput {
  feedbackType: FeedbackType;
  content?: string;
  suggestedPriority?: Severity;
  selectedSolution?: number;
  customSolution?: Partial<Solution>;
}

/**
 * 单个问题反馈 Hook
 */
export function useFeedback(options: UseFeedbackOptions) {
  const { sessionId, problemId, onSuccess, onError } = options;
  const { submitFeedback: storeSubmitFeedback, loading } = useFeedbackStore();
  
  const [submitting, setSubmitting] = useState(false);

  const submit = useCallback(
    async (input: FeedbackInput) => {
      if (!problemId) {
        message.warning('未指定问题ID');
        return;
      }

      setSubmitting(true);
      try {
        const feedback = await storeSubmitFeedback({
          sessionId,
          problemId,
          feedbackType: input.feedbackType,
          content: input.content,
          suggestedPriority: input.suggestedPriority,
          // 其他字段映射...
        });

        message.success('反馈提交成功');
        onSuccess?.(feedback);
        return feedback;
      } catch (error: any) {
        message.error(error.message || '提交失败');
        onError?.(error);
        throw error;
      } finally {
        setSubmitting(false);
      }
    },
    [sessionId, problemId, storeSubmitFeedback, onSuccess, onError]
  );

  // 快捷方法
  const agree = useCallback(
    (content?: string) => submit({ feedbackType: 'agree', content }),
    [submit]
  );

  const disagree = useCallback(
    (content: string) => submit({ feedbackType: 'disagree', content }),
    [submit]
  );

  const modify = useCallback(
    (content: string, suggestedPriority?: Severity) =>
      submit({ feedbackType: 'modify', content, suggestedPriority }),
    [submit]
  );

  const ignore = useCallback(
    (content?: string) => submit({ feedbackType: 'ignore_problem', content }),
    [submit]
  );

  const acceptSolution = useCallback(
    (solutionIndex: number) =>
      submit({ feedbackType: 'accept_solution', selectedSolution: solutionIndex }),
    [submit]
  );

  return {
    submit,
    agree,
    disagree,
    modify,
    ignore,
    acceptSolution,
    submitting: submitting || loading.feedback,
  };
}

/**
 * 批量反馈 Hook
 */
export function useBatchFeedback(sessionId: string) {
  const {
    selectedProblems,
    submitBatchFeedback: storeSubmitBatch,
    clearSelection,
    loading,
  } = useFeedbackStore();

  const [submitting, setSubmitting] = useState(false);

  const submitBatch = useCallback(
    async (feedbackType: FeedbackType, content?: string) => {
      if (selectedProblems.size === 0) {
        message.warning('请先选择要处理的问题');
        return;
      }

      setSubmitting(true);
      try {
        const feedbacks: CreateFeedbackInput[] = Array.from(selectedProblems).map(
          (problemId) => ({
            sessionId,
            problemId,
            feedbackType,
            content,
          })
        );

        const batch = await storeSubmitBatch(feedbacks);
        message.success(`已批量处理 ${selectedProblems.size} 个问题`);
        clearSelection();
        return batch;
      } catch (error: any) {
        message.error(error.message || '批量处理失败');
        throw error;
      } finally {
        setSubmitting(false);
      }
    },
    [sessionId, selectedProblems, storeSubmitBatch, clearSelection]
  );

  // 快捷方法
  const agreeAll = useCallback(
    () => submitBatch('agree'),
    [submitBatch]
  );

  const ignoreAll = useCallback(
    (reason?: string) => submitBatch('ignore_problem', reason),
    [submitBatch]
  );

  return {
    selectedProblems,
    selectedCount: selectedProblems.size,
    submitBatch,
    agreeAll,
    ignoreAll,
    clearSelection,
    submitting: submitting || loading.feedback,
  };
}

/**
 * 反馈历史 Hook
 */
export function useFeedbackHistory(sessionId: string) {
  const { feedbackHistory, loadFeedbackHistory, loading } = useFeedbackStore();

  useEffect(() => {
    loadFeedbackHistory(sessionId);
  }, [sessionId, loadFeedbackHistory]);

  const refresh = useCallback(() => {
    loadFeedbackHistory(sessionId);
  }, [sessionId, loadFeedbackHistory]);

  return {
    history: feedbackHistory,
    loading: loading.feedback,
    refresh,
  };
}

/**
 * 问题选择 Hook
 */
export function useProblemSelection() {
  const {
    selectedProblems,
    selectProblem,
    deselectProblem,
    selectAll,
    clearSelection,
    toggleProblem,
  } = useFeedbackStore();

  const isSelected = useCallback(
    (problemId: string) => selectedProblems.has(problemId),
    [selectedProblems]
  );

  const selectedCount = selectedProblems.size;

  return {
    selectedProblems,
    selectedCount,
    isSelected,
    selectProblem,
    deselectProblem,
    selectAll,
    clearSelection,
    toggleProblem,
  };
}
