/**
 * Dashboard API 模块
 * 文件路径: src/api/dashboard.ts
 */

import api from './config';
import type {
  DashboardStats,
  KnowledgeStats,
  RefactorStats,
  RecentTasks,
} from '../api-types';

/**
 * 获取仪表盘统计数据
 */
export const getDashboardStats = async (): Promise<DashboardStats> => {
  return api.get('/dashboard/stats');
};

/**
 * 获取知识图谱统计
 */
export const getKnowledgeStats = async (): Promise<KnowledgeStats> => {
  return api.get('/dashboard/knowledge-stats');
};

/**
 * 获取重构问题统计
 */
export const getRefactorStats = async (): Promise<RefactorStats> => {
  return api.get('/dashboard/refactor-stats');
};

/**
 * 获取最近任务
 */
export const getRecentTasks = async (limit: number = 5): Promise<RecentTasks> => {
  return api.get('/dashboard/recent-tasks', { params: { limit } });
};

/**
 * 获取所有仪表盘数据（并行请求）
 */
export const getDashboardAllData = async (): Promise<{
  stats: DashboardStats;
  knowledgeStats: KnowledgeStats;
  refactorStats: RefactorStats;
  recentTasks: RecentTasks;
}> => {
  const [stats, knowledgeStats, refactorStats, recentTasks] = await Promise.all([
    getDashboardStats(),
    getKnowledgeStats(),
    getRefactorStats(),
    getRecentTasks(5),
  ]);

  return { stats, knowledgeStats, refactorStats, recentTasks };
};
