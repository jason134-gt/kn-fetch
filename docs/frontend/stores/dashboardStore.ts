// src/stores/dashboardStore.ts
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import * as dashboardApi from '../api/dashboard';

// ========== 类型定义 ==========

interface TrendData {
  type: 'up' | 'down';
  value: number;
}

export interface DashboardStats {
  projectCount: number;
  entityCount: number;
  problemCount: number;
  pendingReviewCount: number;
  projectTrend: TrendData;
  entityTrend: TrendData;
  problemTrend: TrendData;
  pendingReviewTrend: TrendData;
}

export interface EntityTypeStat {
  type: string;
  count: number;
  percentage: number;
}

export interface RelationTypeStat {
  type: string;
  count: number;
  percentage: number;
}

export interface KnowledgeStats {
  totalEntities: number;
  totalRelations: number;
  entityTypes: EntityTypeStat[];
  relationTypes: RelationTypeStat[];
}

export interface SeverityStat {
  severity: 'high' | 'medium' | 'low';
  count: number;
  percentage: number;
}

export interface ProblemTypeStat {
  type: string;
  count: number;
  percentage: number;
}

export interface RefactorStats {
  totalProblems: number;
  bySeverity: SeverityStat[];
  byType: ProblemTypeStat[];
  estimatedHours: number;
}

export interface ExtractTaskSummary {
  taskId: string;
  projectName: string;
  status: 'pending' | 'running' | 'completed' | 'failed' | 'paused';
  progress: number;
  entityCount: number;
  createdAt: string;
  completedAt?: string;
}

export interface RefactorTaskSummary {
  taskId: string;
  projectName: string;
  status: 'pending' | 'analyzing' | 'pending_review' | 'approved' | 'rejected' | 'finalized';
  problemCount: number;
  highPriorityCount: number;
  estimatedHours: number;
  createdAt: string;
}

export interface RecentTasks {
  extractTasks: ExtractTaskSummary[];
  refactorTasks: RefactorTaskSummary[];
}

// ========== Store 状态 ==========

interface DashboardState {
  // 数据
  stats: DashboardStats | null;
  knowledgeStats: KnowledgeStats | null;
  refactorStats: RefactorStats | null;
  recentTasks: RecentTasks | null;
  
  // 加载状态
  loading: {
    stats: boolean;
    knowledgeStats: boolean;
    refactorStats: boolean;
    recentTasks: boolean;
  };
  
  // 错误信息
  errors: {
    stats: string | null;
    knowledgeStats: string | null;
    refactorStats: string | null;
    recentTasks: string | null;
  };
  
  // 最后更新时间
  lastUpdated: {
    stats: Date | null;
    knowledgeStats: Date | null;
    refactorStats: Date | null;
    recentTasks: Date | null;
  };
  
  // 自动刷新定时器
  refreshInterval: NodeJS.Timeout | null;
  
  // 操作方法
  fetchStats: () => Promise<void>;
  fetchKnowledgeStats: () => Promise<void>;
  fetchRefactorStats: () => Promise<void>;
  fetchRecentTasks: (limit?: number) => Promise<void>;
  fetchAll: () => Promise<void>;
  
  // 刷新控制
  startAutoRefresh: (intervalMs?: number) => void;
  stopAutoRefresh: () => void;
  
  // 重置
  reset: () => void;
}

// ========== 初始状态 ==========

const initialState = {
  stats: null,
  knowledgeStats: null,
  refactorStats: null,
  recentTasks: null,
  loading: {
    stats: false,
    knowledgeStats: false,
    refactorStats: false,
    recentTasks: false,
  },
  errors: {
    stats: null,
    knowledgeStats: null,
    refactorStats: null,
    recentTasks: null,
  },
  lastUpdated: {
    stats: null,
    knowledgeStats: null,
    refactorStats: null,
    recentTasks: null,
  },
  refreshInterval: null,
};

// ========== Store 实现 ==========

export const useDashboardStore = create<DashboardState>()(
  devtools(
    (set, get) => ({
      ...initialState,
      
      // 获取概览统计
      fetchStats: async () => {
        set((state) => ({
          loading: { ...state.loading, stats: true },
          errors: { ...state.errors, stats: null },
        }));
        
        try {
          const data = await dashboardApi.getDashboardStats();
          set((state) => ({
            stats: data,
            loading: { ...state.loading, stats: false },
            lastUpdated: { ...state.lastUpdated, stats: new Date() },
          }));
        } catch (error: any) {
          set((state) => ({
            loading: { ...state.loading, stats: false },
            errors: { ...state.errors, stats: error.message || '获取统计数据失败' },
          }));
        }
      },
      
      // 获取知识图谱统计
      fetchKnowledgeStats: async () => {
        set((state) => ({
          loading: { ...state.loading, knowledgeStats: true },
          errors: { ...state.errors, knowledgeStats: null },
        }));
        
        try {
          const data = await dashboardApi.getKnowledgeStats();
          set((state) => ({
            knowledgeStats: data,
            loading: { ...state.loading, knowledgeStats: false },
            lastUpdated: { ...state.lastUpdated, knowledgeStats: new Date() },
          }));
        } catch (error: any) {
          set((state) => ({
            loading: { ...state.loading, knowledgeStats: false },
            errors: { ...state.errors, knowledgeStats: error.message || '获取知识统计失败' },
          }));
        }
      },
      
      // 获取重构问题统计
      fetchRefactorStats: async () => {
        set((state) => ({
          loading: { ...state.loading, refactorStats: true },
          errors: { ...state.errors, refactorStats: null },
        }));
        
        try {
          const data = await dashboardApi.getRefactorStats();
          set((state) => ({
            refactorStats: data,
            loading: { ...state.loading, refactorStats: false },
            lastUpdated: { ...state.lastUpdated, refactorStats: new Date() },
          }));
        } catch (error: any) {
          set((state) => ({
            loading: { ...state.loading, refactorStats: false },
            errors: { ...state.errors, refactorStats: error.message || '获取重构统计失败' },
          }));
        }
      },
      
      // 获取最近任务
      fetchRecentTasks: async (limit: number = 5) => {
        set((state) => ({
          loading: { ...state.loading, recentTasks: true },
          errors: { ...state.errors, recentTasks: null },
        }));
        
        try {
          const data = await dashboardApi.getRecentTasks(limit);
          set((state) => ({
            recentTasks: data,
            loading: { ...state.loading, recentTasks: false },
            lastUpdated: { ...state.lastUpdated, recentTasks: new Date() },
          }));
        } catch (error: any) {
          set((state) => ({
            loading: { ...state.loading, recentTasks: false },
            errors: { ...state.errors, recentTasks: error.message || '获取最近任务失败' },
          }));
        }
      },
      
      // 获取所有数据
      fetchAll: async () => {
        const { fetchStats, fetchKnowledgeStats, fetchRefactorStats, fetchRecentTasks } = get();
        await Promise.all([
          fetchStats(),
          fetchKnowledgeStats(),
          fetchRefactorStats(),
          fetchRecentTasks(),
        ]);
      },
      
      // 开启自动刷新
      startAutoRefresh: (intervalMs: number = 30000) => {
        const { stopAutoRefresh, fetchAll } = get();
        stopAutoRefresh();
        
        const interval = setInterval(fetchAll, intervalMs);
        set({ refreshInterval: interval });
      },
      
      // 停止自动刷新
      stopAutoRefresh: () => {
        const { refreshInterval } = get();
        if (refreshInterval) {
          clearInterval(refreshInterval);
          set({ refreshInterval: null });
        }
      },
      
      // 重置状态
      reset: () => {
        const { stopAutoRefresh } = get();
        stopAutoRefresh();
        set(initialState);
      },
    }),
    { name: 'dashboard-store' }
  )
);

// ========== 选择器 ==========

export const selectStats = (state: DashboardState) => state.stats;
export const selectKnowledgeStats = (state: DashboardState) => state.knowledgeStats;
export const selectRefactorStats = (state: DashboardState) => state.refactorStats;
export const selectRecentTasks = (state: DashboardState) => state.recentTasks;
export const selectIsLoading = (state: DashboardState) => 
  Object.values(state.loading).some(Boolean);
export const selectHasErrors = (state: DashboardState) => 
  Object.values(state.errors).some(Boolean);
