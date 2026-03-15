// src/stores/refactorStore.ts
import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import * as refactorApi from '../api/refactor';
import * as reportApi from '../api/report';

// ========== 类型定义 ==========

export type RefactorTaskStatus =
  | 'pending'
  | 'analyzing'
  | 'pending_review'
  | 'in_review'
  | 'regenerating'
  | 'approved'
  | 'rejected'
  | 'finalized';

export interface RefactorTask {
  taskId: string;
  projectName: string;
  knowledgePath: string;
  status: RefactorTaskStatus;
  problemCount: number;
  highPriorityCount: number;
  mediumPriorityCount: number;
  lowPriorityCount: number;
  estimatedHours: number;
  createdAt: string;
  completedAt?: string;
  reportId?: string;
}

export interface RefactorTaskDetail extends RefactorTask {
  config: {
    analysisDepth: 'quick' | 'normal' | 'deep';
    enabledRules: string[];
    excludedPaths: string[];
  };
  statistics: {
    filesAnalyzed: number;
    linesAnalyzed: number;
    duration: number;
  };
}

export interface ProblemDetail {
  problemId: string;
  problemType: string;
  severity: 'high' | 'medium' | 'low';
  
  // 一、是什么
  description: string;
  currentState: Record<string, any>;
  codeContext?: string;
  location: {
    file: string;
    lineStart: number;
    lineEnd?: number;
  };
  
  // 二、怎么修复
  fixSteps: Array<{
    step: number;
    description: string;
    codeExample?: string;
  }>;
  
  // 三、有什么风险
  risks: Array<{
    level: 'high' | 'medium' | 'low';
    description: string;
    probability: 'high' | 'medium' | 'low';
    impact: 'high' | 'medium' | 'low';
    mitigation: string;
  }>;
  overallRiskLevel: 'high' | 'medium' | 'low';
  
  // 四、有什么方案
  solutions: Array<{
    id: number;
    name: string;
    description: string;
    estimatedHours: number;
    riskLevel: 'high' | 'medium' | 'low';
    pros: string[];
    cons: string[];
    steps: string[];
  }>;
  recommendedSolution: number;
  
  // 用户反馈状态
  userConfirmed?: boolean;
  ignored?: boolean;
  userComment?: string;
  selectedSolution?: number;
  suggestedPriority?: 'high' | 'medium' | 'low';
}

export interface RefactorReport {
  reportId: string;
  taskId: string;
  version: number;
  sessionId: string;
  status: 'draft' | 'pending_review' | 'approved' | 'rejected' | 'finalized';
  
  // 统计
  totalProblems: number;
  highPriorityCount: number;
  mediumPriorityCount: number;
  lowPriorityCount: number;
  estimatedHours: number;
  
  // 问题列表
  problems: ProblemDetail[];
  
  // 变更信息
  changes?: {
    added: string[];
    removed: string[];
    modified: string[];
  };
  
  // 时间戳
  createdAt: string;
  updatedAt: string;
  generatedAt: string;
}

export interface QueryParams {
  page?: number;
  pageSize?: number;
  status?: RefactorTaskStatus;
  severity?: 'high' | 'medium' | 'low';
  keyword?: string;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

// ========== Store 状态 ==========

interface RefactorState {
  // 任务列表
  tasks: RefactorTask[];
  taskTotal: number;
  taskPage: number;
  taskPageSize: number;
  queryParams: QueryParams;
  
  // 当前任务详情
  currentTask: RefactorTaskDetail | null;
  
  // 当前报告
  currentReport: RefactorReport | null;
  
  // 报告版本列表
  reportVersions: RefactorReport[];
  
  // 选中问题
  selectedProblemIds: Set<string>;
  
  // 加载状态
  loading: {
    tasks: boolean;
    taskDetail: boolean;
    report: boolean;
    versions: boolean;
    create: boolean;
    update: boolean;
  };
  
  // 错误信息
  errors: {
    tasks: string | null;
    taskDetail: string | null;
    report: string | null;
    versions: string | null;
    create: string | null;
    update: string | null;
  };
  
  // 操作方法
  fetchTasks: (params?: QueryParams) => Promise<void>;
  fetchTaskDetail: (taskId: string) => Promise<void>;
  fetchReport: (taskId: string) => Promise<void>;
  fetchReportVersions: (sessionId: string) => Promise<void>;
  
  createTask: (data: CreateTaskInput) => Promise<RefactorTask>;
  deleteTask: (taskId: string) => Promise<void>;
  
  // 问题选择
  selectProblem: (problemId: string) => void;
  deselectProblem: (problemId: string) => void;
  selectAllProblems: () => void;
  clearSelection: () => void;
  
  // 分页
  setPage: (page: number) => void;
  setPageSize: (pageSize: number) => void;
  setQueryParams: (params: Partial<QueryParams>) => void;
  
  // 重置
  resetCurrentTask: () => void;
  reset: () => void;
}

interface CreateTaskInput {
  projectName: string;
  knowledgePath: string;
  config?: {
    analysisDepth?: 'quick' | 'normal' | 'deep';
    enabledRules?: string[];
    excludedPaths?: string[];
  };
}

// ========== 初始状态 ==========

const initialState = {
  tasks: [],
  taskTotal: 0,
  taskPage: 1,
  taskPageSize: 10,
  queryParams: {},
  currentTask: null,
  currentReport: null,
  reportVersions: [],
  selectedProblemIds: new Set<string>(),
  loading: {
    tasks: false,
    taskDetail: false,
    report: false,
    versions: false,
    create: false,
    update: false,
  },
  errors: {
    tasks: null,
    taskDetail: null,
    report: null,
    versions: null,
    create: null,
    update: null,
  },
};

// ========== Store 实现 ==========

export const useRefactorStore = create<RefactorState>()(
  devtools(
    persist(
      (set, get) => ({
        ...initialState,
        
        // 获取任务列表
        fetchTasks: async (params?: QueryParams) => {
          const { queryParams, taskPage, taskPageSize } = get();
          const mergedParams = {
            page: taskPage,
            pageSize: taskPageSize,
            ...queryParams,
            ...params,
          };
          
          set((state) => ({
            loading: { ...state.loading, tasks: true },
            errors: { ...state.errors, tasks: null },
          }));
          
          try {
            const response = await refactorApi.getRefactorTaskList(mergedParams);
            set((state) => ({
              tasks: response.data.items,
              taskTotal: response.data.total,
              taskPage: response.data.page,
              taskPageSize: response.data.pageSize,
              loading: { ...state.loading, tasks: false },
            }));
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, tasks: false },
              errors: { ...state.errors, tasks: error.message || '获取任务列表失败' },
            }));
          }
        },
        
        // 获取任务详情
        fetchTaskDetail: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, taskDetail: true },
            errors: { ...state.errors, taskDetail: null },
          }));
          
          try {
            const response = await refactorApi.getRefactorTaskDetail(taskId);
            set((state) => ({
              currentTask: response.data,
              loading: { ...state.loading, taskDetail: false },
            }));
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, taskDetail: false },
              errors: { ...state.errors, taskDetail: error.message || '获取任务详情失败' },
            }));
          }
        },
        
        // 获取报告
        fetchReport: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, report: true },
            errors: { ...state.errors, report: null },
          }));
          
          try {
            const response = await refactorApi.getRefactorReport(taskId);
            set((state) => ({
              currentReport: response.data,
              loading: { ...state.loading, report: false },
            }));
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, report: false },
              errors: { ...state.errors, report: error.message || '获取报告失败' },
            }));
          }
        },
        
        // 获取报告版本列表
        fetchReportVersions: async (sessionId: string) => {
          set((state) => ({
            loading: { ...state.loading, versions: true },
            errors: { ...state.errors, versions: null },
          }));
          
          try {
            const response = await reportApi.getReportVersions(sessionId);
            set((state) => ({
              reportVersions: response.data,
              loading: { ...state.loading, versions: false },
            }));
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, versions: false },
              errors: { ...state.errors, versions: error.message || '获取版本列表失败' },
            }));
          }
        },
        
        // 创建任务
        createTask: async (data: CreateTaskInput) => {
          set((state) => ({
            loading: { ...state.loading, create: true },
            errors: { ...state.errors, create: null },
          }));
          
          try {
            const response = await refactorApi.createRefactorTask(data);
            set((state) => ({
              loading: { ...state.loading, create: false },
            }));
            get().fetchTasks();
            return response.data;
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, create: false },
              errors: { ...state.errors, create: error.message || '创建任务失败' },
            }));
            throw error;
          }
        },
        
        // 删除任务
        deleteTask: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, update: true },
            errors: { ...state.errors, update: null },
          }));
          
          try {
            await refactorApi.deleteRefactorTask(taskId);
            set((state) => ({
              loading: { ...state.loading, update: false },
            }));
            get().fetchTasks();
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, update: false },
              errors: { ...state.errors, update: error.message || '删除任务失败' },
            }));
          }
        },
        
        // 问题选择
        selectProblem: (problemId: string) => {
          set((state) => {
            const newSet = new Set(state.selectedProblemIds);
            newSet.add(problemId);
            return { selectedProblemIds: newSet };
          });
        },
        
        deselectProblem: (problemId: string) => {
          set((state) => {
            const newSet = new Set(state.selectedProblemIds);
            newSet.delete(problemId);
            return { selectedProblemIds: newSet };
          });
        },
        
        selectAllProblems: () => {
          const { currentReport } = get();
          if (currentReport) {
            const allIds = new Set(currentReport.problems.map(p => p.problemId));
            set({ selectedProblemIds: allIds });
          }
        },
        
        clearSelection: () => {
          set({ selectedProblemIds: new Set() });
        },
        
        // 分页设置
        setPage: (page: number) => {
          set({ taskPage: page });
          get().fetchTasks({ page });
        },
        
        setPageSize: (pageSize: number) => {
          set({ taskPageSize: pageSize, taskPage: 1 });
          get().fetchTasks({ pageSize, page: 1 });
        },
        
        setQueryParams: (params: Partial<QueryParams>) => {
          set((state) => ({
            queryParams: { ...state.queryParams, ...params },
            taskPage: 1,
          }));
          get().fetchTasks({ ...params, page: 1 });
        },
        
        // 重置当前任务
        resetCurrentTask: () => {
          set({
            currentTask: null,
            currentReport: null,
            reportVersions: [],
            selectedProblemIds: new Set(),
          });
        },
        
        // 重置状态
        reset: () => {
          set(initialState);
        },
      }),
      {
        name: 'refactor-store',
        partialize: (state) => ({
          taskPage: state.taskPage,
          taskPageSize: state.taskPageSize,
          queryParams: state.queryParams,
        }),
      }
    ),
    { name: 'refactor-store' }
  )
);

// ========== 选择器 ==========

export const selectTasks = (state: RefactorState) => state.tasks;
export const selectCurrentTask = (state: RefactorState) => state.currentTask;
export const selectCurrentReport = (state: RefactorState) => state.currentReport;
export const selectReportVersions = (state: RefactorState) => state.reportVersions;
export const selectSelectedProblemIds = (state: RefactorState) => state.selectedProblemIds;
export const selectPagination = (state: RefactorState) => ({
  page: state.taskPage,
  pageSize: state.taskPageSize,
  total: state.taskTotal,
});
