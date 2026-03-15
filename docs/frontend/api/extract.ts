// src/api/extract.ts
import api from './config';

// ========== 类型定义 ==========

export interface ExtractTask {
  taskId: string;
  projectName: string;
  projectPath: string;
  languages: string[];
  status: string;
  progress: number;
  entityCount: number;
  relationCount: number;
  documentCount: number;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  error?: string;
}

export interface ExtractTaskDetail extends ExtractTask {
  config: {
    includeTestFiles: boolean;
    excludePatterns: string[];
    maxFileSize: number;
    outputDir: string;
  };
  statistics: {
    filesProcessed: number;
    filesSkipped: number;
    linesOfCode: number;
    duration: number;
  };
  errors: Array<{
    file: string;
    error: string;
    timestamp: string;
  }>;
}

export interface KnowledgeEntity {
  id: string;
  type: string;
  name: string;
  fullName: string;
  filePath: string;
  lineStart: number;
  lineEnd: number;
  description?: string;
  annotations?: string[];
  modifiers?: string[];
}

export interface KnowledgeRelation {
  id: string;
  type: string;
  sourceId: string;
  targetId: string;
  metadata?: Record<string, any>;
}

export interface KnowledgeGraph {
  entities: KnowledgeEntity[];
  relations: KnowledgeRelation[];
  metadata?: {
    totalEntities: number;
    totalRelations: number;
    languages: string[];
    extractedAt: string;
  };
}

export interface GeneratedDoc {
  name: string;
  path: string;
  type: string;
  size: number;
  createdAt: string;
}

export interface CreateTaskInput {
  projectName: string;
  projectPath: string;
  languages: string[];
  outputDir?: string;
  extractConfig?: {
    includeTestFiles?: boolean;
    excludePatterns?: string[];
    maxFileSize?: number;
  };
}

export interface QueryParams {
  page?: number;
  pageSize?: number;
  status?: string;
  language?: string;
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

// ========== API 函数 ==========

/**
 * 获取提取任务列表
 */
export const getExtractTaskList = (params: QueryParams) =>
  api.get<PageResult<ExtractTask>>('/extract/tasks', { params });

/**
 * 创建提取任务
 */
export const createExtractTask = (data: CreateTaskInput) =>
  api.post<ExtractTask>('/extract/tasks', data);

/**
 * 获取任务详情
 */
export const getExtractTaskDetail = (taskId: string) =>
  api.get<ExtractTaskDetail>(`/extract/tasks/${taskId}`);

/**
 * 暂停任务
 */
export const pauseExtractTask = (taskId: string) =>
  api.post(`/extract/tasks/${taskId}/pause`);

/**
 * 继续任务
 */
export const resumeExtractTask = (taskId: string) =>
  api.post(`/extract/tasks/${taskId}/resume`);

/**
 * 暂停/继续任务
 */
export const toggleExtractTask = (taskId: string, action: 'pause' | 'resume') =>
  api.post(`/extract/tasks/${taskId}/${action}`);

/**
 * 取消任务
 */
export const cancelExtractTask = (taskId: string) =>
  api.post(`/extract/tasks/${taskId}/cancel`);

/**
 * 删除任务
 */
export const deleteExtractTask = (taskId: string) =>
  api.delete(`/extract/tasks/${taskId}`);

/**
 * 获取知识图谱
 */
export const getKnowledgeGraph = (taskId: string) =>
  api.get<KnowledgeGraph>(`/extract/tasks/${taskId}/knowledge`);

/**
 * 获取生成的文档
 */
export const getGeneratedDocs = (taskId: string) =>
  api.get<GeneratedDoc[]>(`/extract/tasks/${taskId}/docs`);

/**
 * 导出知识图谱
 */
export const exportKnowledgeGraph = (taskId: string, format: 'json' | 'csv' | 'graphml' = 'json') =>
  api.get(`/extract/tasks/${taskId}/export`, {
    params: { format },
    responseType: 'blob',
  });

/**
 * 获取任务执行日志
 */
export const getTaskLogs = (taskId: string, params?: {
  startLine?: number;
  limit?: number;
}) =>
  api.get<string[]>(`/extract/tasks/${taskId}/logs`, { params });

/**
 * 获取实时任务进度 (WebSocket)
 */
export const getTaskProgressWs = (taskId: string) =>
  `${api.defaults.baseURL}/extract/tasks/${taskId}/progress`.replace('http', 'ws');
