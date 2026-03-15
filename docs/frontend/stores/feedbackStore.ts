/**
 * 反馈闭环状态管理 Store
 * 文件路径: src/stores/feedbackStore.ts
 */

import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import type {
  FeedbackSession,
  FeedbackBatch,
  UserFeedback,
  ReviewRecord,
  RefactorReport,
  ConvergenceResult,
  CreateFeedbackInput,
  FeedbackType,
} from '../types';
import * as feedbackApi from '../api/feedback';
import * as reviewApi from '../api/review';
import * as refactorApi from '../api/refactor';

interface FeedbackState {
  // 当前会话
  currentSession: FeedbackSession | null;
  
  // 当前报告
  currentReport: RefactorReport | null;
  reportVersions: RefactorReport[];
  
  // 选中问题
  selectedProblems: Set<string>;
  
  // 反馈批次
  currentBatch: FeedbackBatch | null;
  feedbackHistory: UserFeedback[];
  
  // 审核工作流
  reviewRecord: ReviewRecord | null;
  allowedActions: string[];
  
  // 收敛状态
  convergenceResult: ConvergenceResult | null;
  
  // 加载状态
  loading: {
    session: boolean;
    report: boolean;
    feedback: boolean;
    review: boolean;
    regenerate: boolean;
  };
  
  // 错误信息
  errors: {
    session?: string;
    report?: string;
    feedback?: string;
    review?: string;
  };
  
  // ========== 会话操作 ==========
  loadSession: (sessionId: string) => Promise<void>;
  createSession: (projectName: string, reportId: string) => Promise<FeedbackSession>;
  
  // ========== 报告操作 ==========
  loadReport: (reportId: string) => Promise<void>;
  loadReportVersions: (sessionId: string) => Promise<void>;
  
  // ========== 问题选择 ==========
  selectProblem: (problemId: string) => void;
  deselectProblem: (problemId: string) => void;
  selectAll: (problemIds: string[]) => void;
  clearSelection: () => void;
  toggleProblem: (problemId: string) => void;
  
  // ========== 反馈操作 ==========
  submitFeedback: (input: CreateFeedbackInput) => Promise<UserFeedback>;
  submitBatchFeedback: (feedbacks: CreateFeedbackInput[]) => Promise<FeedbackBatch>;
  loadFeedbackHistory: (sessionId: string) => Promise<void>;
  
  // ========== 报告重生成 ==========
  regenerateReport: (sessionId: string) => Promise<void>;
  
  // ========== 审核工作流 ==========
  createReview: (sessionId: string, reportId: string) => Promise<ReviewRecord>;
  loadReviewStatus: (recordId: string) => Promise<void>;
  executeReviewAction: (
    action: string,
    comments?: string,
    feedbackBatchId?: string
  ) => Promise<void>;
  loadAllowedActions: (recordId: string) => Promise<void>;
  
  // ========== 收敛检测 ==========
  checkConvergence: (sessionId: string) => Promise<ConvergenceResult>;
  
  // ========== 工具方法 ==========
  reset: () => void;
  clearErrors: () => void;
}

const initialState = {
  currentSession: null,
  currentReport: null,
  reportVersions: [],
  selectedProblems: new Set<string>(),
  currentBatch: null,
  feedbackHistory: [],
  reviewRecord: null,
  allowedActions: [],
  convergenceResult: null,
  loading: {
    session: false,
    report: false,
    feedback: false,
    review: false,
    regenerate: false,
  },
  errors: {},
};

export const useFeedbackStore = create<FeedbackState>()(
  devtools(
    persist(
      (set, get) => ({
        ...initialState,

        // ========== 会话操作 ==========
        loadSession: async (sessionId: string) => {
          set((state) => ({
            loading: { ...state.loading, session: true },
            errors: { ...state.errors, session: undefined },
          }));

          try {
            const session = await feedbackApi.getFeedbackSession(sessionId);
            set({ currentSession: session });
          } catch (error: any) {
            set((state) => ({
              errors: { ...state.errors, session: error.message },
            }));
            throw error;
          } finally {
            set((state) => ({
              loading: { ...state.loading, session: false },
            }));
          }
        },

        createSession: async (projectName: string, reportId: string) => {
          const session = await feedbackApi.createFeedbackSession({
            projectName,
            reportId,
          });
          set({ currentSession: session });
          return session;
        },

        // ========== 报告操作 ==========
        loadReport: async (reportId: string) => {
          set((state) => ({
            loading: { ...state.loading, report: true },
            errors: { ...state.errors, report: undefined },
          }));

          try {
            const report = await refactorApi.getRefactorReport(reportId);
            set({ currentReport: report });
          } catch (error: any) {
            set((state) => ({
              errors: { ...state.errors, report: error.message },
            }));
            throw error;
          } finally {
            set((state) => ({
              loading: { ...state.loading, report: false },
            }));
          }
        },

        loadReportVersions: async (sessionId: string) => {
          const versions = await feedbackApi.getReportVersions(sessionId);
          set({ reportVersions: versions });
        },

        // ========== 问题选择 ==========
        selectProblem: (problemId: string) => {
          set((state) => {
            const newSet = new Set(state.selectedProblems);
            newSet.add(problemId);
            return { selectedProblems: newSet };
          });
        },

        deselectProblem: (problemId: string) => {
          set((state) => {
            const newSet = new Set(state.selectedProblems);
            newSet.delete(problemId);
            return { selectedProblems: newSet };
          });
        },

        selectAll: (problemIds: string[]) => {
          set({ selectedProblems: new Set(problemIds) });
        },

        clearSelection: () => {
          set({ selectedProblems: new Set() });
        },

        toggleProblem: (problemId: string) => {
          const { selectedProblems, selectProblem, deselectProblem } = get();
          if (selectedProblems.has(problemId)) {
            deselectProblem(problemId);
          } else {
            selectProblem(problemId);
          }
        },

        // ========== 反馈操作 ==========
        submitFeedback: async (input: CreateFeedbackInput) => {
          set((state) => ({
            loading: { ...state.loading, feedback: true },
            errors: { ...state.errors, feedback: undefined },
          }));

          try {
            const feedback = await feedbackApi.submitFeedback(input);
            // 添加到历史记录
            set((state) => ({
              feedbackHistory: [...state.feedbackHistory, feedback],
            }));
            return feedback;
          } catch (error: any) {
            set((state) => ({
              errors: { ...state.errors, feedback: error.message },
            }));
            throw error;
          } finally {
            set((state) => ({
              loading: { ...state.loading, feedback: false },
            }));
          }
        },

        submitBatchFeedback: async (feedbacks: CreateFeedbackInput[]) => {
          set((state) => ({
            loading: { ...state.loading, feedback: true },
          }));

          try {
            const batch = await feedbackApi.submitFeedbackBatch({
              sessionId: feedbacks[0].sessionId,
              feedbacks,
            });
            set({ currentBatch: batch });
            return batch;
          } finally {
            set((state) => ({
              loading: { ...state.loading, feedback: false },
            }));
          }
        },

        loadFeedbackHistory: async (sessionId: string) => {
          const history = await feedbackApi.getFeedbackHistory(sessionId);
          set({ feedbackHistory: history });
        },

        // ========== 报告重生成 ==========
        regenerateReport: async (sessionId: string) => {
          set((state) => ({
            loading: { ...state.loading, regenerate: true },
          }));

          try {
            await feedbackApi.regenerateReport(sessionId);
          } finally {
            set((state) => ({
              loading: { ...state.loading, regenerate: false },
            }));
          }
        },

        // ========== 审核工作流 ==========
        createReview: async (sessionId: string, reportId: string) => {
          const record = await reviewApi.createReview({ sessionId, reportId });
          set({ reviewRecord: record });
          return record;
        },

        loadReviewStatus: async (recordId: string) => {
          set((state) => ({
            loading: { ...state.loading, review: true },
          }));

          try {
            const [record, actions] = await Promise.all([
              reviewApi.getReviewStatus(recordId),
              reviewApi.getAllowedActions(recordId),
            ]);
            set({ reviewRecord: record, allowedActions: actions });
          } finally {
            set((state) => ({
              loading: { ...state.loading, review: false },
            }));
          }
        },

        executeReviewAction: async (
          action: string,
          comments?: string,
          feedbackBatchId?: string
        ) => {
          const { reviewRecord } = get();
          if (!reviewRecord) return;

          set((state) => ({
            loading: { ...state.loading, review: true },
          }));

          try {
            const record = await reviewApi.executeReviewAction({
              sessionId: reviewRecord.sessionId,
              recordId: reviewRecord.recordId,
              action: action as any,
              comments,
              feedbackBatchId,
            });
            
            // 更新状态并获取新的允许动作
            const actions = await reviewApi.getAllowedActions(record.recordId);
            set({ reviewRecord: record, allowedActions: actions });
          } finally {
            set((state) => ({
              loading: { ...state.loading, review: false },
            }));
          }
        },

        loadAllowedActions: async (recordId: string) => {
          const actions = await reviewApi.getAllowedActions(recordId);
          set({ allowedActions: actions });
        },

        // ========== 收敛检测 ==========
        checkConvergence: async (sessionId: string) => {
          const result = await reviewApi.checkConvergence(sessionId);
          set({ convergenceResult: result });
          return result;
        },

        // ========== 工具方法 ==========
        reset: () => {
          set(initialState);
        },

        clearErrors: () => {
          set({ errors: {} });
        },
      }),
      {
        name: 'feedback-store',
        partialize: (state) => ({
          // 只持久化必要的字段
          currentSession: state.currentSession,
          selectedProblems: Array.from(state.selectedProblems),
        }),
        onRehydrateStorage: () => (state) => {
          // 恢复 Set 类型
          if (state && Array.isArray((state as any).selectedProblems)) {
            (state as any).selectedProblems = new Set((state as any).selectedProblems);
          }
        },
      }
    ),
    { name: 'FeedbackStore' }
  )
);

// 选择器 Hooks
export const useCurrentSession = () => useFeedbackStore((state) => state.currentSession);
export const useCurrentReport = () => useFeedbackStore((state) => state.currentReport);
export const useSelectedProblems = () => useFeedbackStore((state) => state.selectedProblems);
export const useReviewRecord = () => useFeedbackStore((state) => state.reviewRecord);
export const useConvergenceResult = () => useFeedbackStore((state) => state.convergenceResult);
export const useFeedbackLoading = () => useFeedbackStore((state) => state.loading);
