// src/stores/extractStore.ts
import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import * as extractApi from '../api/extract';

// ========== 类型定义 ==========

export type ExtractTaskStatus = 
  | 'pending' 
  | 'cloning' 
  | 'parsing' 
  | 'extracting' 
  | 'generating' 
  | 'completed' 
  | 'failed' 
  | 'paused';

export interface ExtractTask {
  taskId: string;
  projectName: string;
  projectPath: string;
  languages: string[];
  status: ExtractTaskStatus;
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
  type: 'class' | 'interface' | 'method' | 'field' | 'annotation' | 'enum';
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
  type: 'extends' | 'implements' | 'calls' | 'uses' | 'contains' | 'annotates';
  sourceId: string;
  targetId: string;
  metadata?: Record<string, any>;
}

export interface KnowledgeGraph {
  entities: KnowledgeEntity[];
  relations: KnowledgeRelation[];
}

export interface QueryParams {
  page?: number;
  pageSize?: number;
  status?: ExtractTaskStatus;
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

// ========== 知识单元类型 ==========

export interface KnowledgeUnit {
  unit_id: string;
  type: string;
  name: string;
  description?: string;
  file_path: string;
  start_line: number;
  end_line: number;
  confidence: number;
  code?: string;
  relationships?: Array<{
    type: string;
    target_name: string;
  }>;
}

export interface ExtractionResult {
  code_snippets?: Array<{
    file_path: string;
    start_line: number;
    end_line: number;
    code: string;
  }>;
  entities?: KnowledgeEntity[];
  relations?: KnowledgeRelation[];
}

// ========== Store 状态 ==========

interface ExtractState {
  // 任务列表
  tasks: ExtractTask[];
  taskTotal: number;
  taskPage: number;
  taskPageSize: number;
  queryParams: QueryParams;
  
  // 当前任务详情
  currentTask: ExtractTaskDetail | null;
  
  // 知识图谱
  knowledgeGraph: KnowledgeGraph | null;
  
  // 知识单元列表
  knowledgeUnits: KnowledgeUnit[];
  
  // 提取结果
  extractionResults: ExtractionResult;
  
  // 生成的文档
  generatedDocs: Array<{
    name: string;
    path: string;
    type: string;
    size: number;
    createdAt: string;
  }>;
  
  // 加载状态
  loading: {
    tasks: boolean;
    taskDetail: boolean;
    knowledgeGraph: boolean;
    knowledgeUnits: boolean;
    extractionResults: boolean;
    docs: boolean;
    create: boolean;
    update: boolean;
  };
  
  // 错误信息
  errors: {
    tasks: string | null;
    taskDetail: string | null;
    knowledgeGraph: string | null;
    knowledgeUnits: string | null;
    extractionResults: string | null;
    docs: string | null;
    create: string | null;
    update: string | null;
  };
  
  // 操作方法
  fetchTasks: (params?: QueryParams) => Promise<void>;
  fetchTask: (taskId: string) => Promise<void>;
  fetchTaskDetail: (taskId: string) => Promise<void>;
  fetchKnowledgeGraph: (taskId: string) => Promise<void>;
  fetchKnowledgeUnits: (taskId: string) => Promise<void>;
  fetchExtractionResults: (taskId: string) => Promise<void>;
  fetchGeneratedDocs: (taskId: string) => Promise<void>;
  exportResults: (taskId: string) => void;
  
  createTask: (data: CreateTaskInput) => Promise<ExtractTask>;
  pauseTask: (taskId: string) => Promise<void>;
  resumeTask: (taskId: string) => Promise<void>;
  cancelTask: (taskId: string) => Promise<void>;
  deleteTask: (taskId: string) => Promise<void>;
  
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
  projectPath: string;
  languages: string[];
  outputDir?: string;
  extractConfig?: {
    includeTestFiles?: boolean;
    excludePatterns?: string[];
    maxFileSize?: number;
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
  knowledgeGraph: null,
  knowledgeUnits: [],
  extractionResults: {},
  generatedDocs: [],
  loading: {
    tasks: false,
    taskDetail: false,
    knowledgeGraph: false,
    knowledgeUnits: false,
    extractionResults: false,
    docs: false,
    create: false,
    update: false,
  },
  errors: {
    tasks: null,
    taskDetail: null,
    knowledgeGraph: null,
    knowledgeUnits: null,
    extractionResults: null,
    docs: null,
    create: null,
    update: null,
  },
};

// ========== Store 实现 ==========

export const useExtractStore = create<ExtractState>()(
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
            const response = await extractApi.getExtractTaskList(mergedParams);
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
            const response = await extractApi.getExtractTaskDetail(taskId);
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
        
        // fetchTask 别名
        fetchTask: async (taskId: string) => {
          const { fetchTaskDetail } = get();
          await fetchTaskDetail(taskId);
        },
        
        // 获取知识单元列表
        fetchKnowledgeUnits: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, knowledgeUnits: true },
            errors: { ...state.errors, knowledgeUnits: null },
          }));
          
          try {
            const response = await extractApi.getKnowledgeGraph(taskId);
            const units: KnowledgeUnit[] = (response.data?.entities || []).map((entity: any) => ({
              unit_id: entity.id,
              type: entity.type,
              name: entity.name,
              description: entity.description,
              file_path: entity.filePath,
              start_line: entity.lineStart,
              end_line: entity.lineEnd,
              confidence: 0.85,
              code: entity.code,
            }));
            set((state) => ({
              knowledgeUnits: units,
              loading: { ...state.loading, knowledgeUnits: false },
            }));
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, knowledgeUnits: false },
              errors: { ...state.errors, knowledgeUnits: error.message || '获取知识单元失败' },
            }));
          }
        },
        
        // 获取提取结果
        fetchExtractionResults: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, extractionResults: true },
            errors: { ...state.errors, extractionResults: null },
          }));
          
          try {
            const response = await extractApi.getExtractTaskDetail(taskId);
            set((state) => ({
              extractionResults: {
                code_snippets: [],
                entities: [],
                relations: [],
              },
              loading: { ...state.loading, extractionResults: false },
            }));
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, extractionResults: false },
              errors: { ...state.errors, extractionResults: error.message || '获取提取结果失败' },
            }));
          }
        },
        
        // 导出结果
        exportResults: (taskId: string) => {
          extractApi.exportKnowledgeGraph(taskId, 'json')
            .then((response: any) => {
              const blob = new Blob([JSON.stringify(response.data, null, 2)], { type: 'application/json' });
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = `extraction-result-${taskId}.json`;
              a.click();
              URL.revokeObjectURL(url);
            })
            .catch((error: any) => {
              console.error('导出失败:', error);
            });
        },
        
        // 获取知识图谱
        fetchKnowledgeGraph: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, knowledgeGraph: true },
            errors: { ...state.errors, knowledgeGraph: null },
          }));
          
          try {
            const response = await extractApi.getKnowledgeGraph(taskId);
            set((state) => ({
              knowledgeGraph: response.data,
              loading: { ...state.loading, knowledgeGraph: false },
            }));
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, knowledgeGraph: false },
              errors: { ...state.errors, knowledgeGraph: error.message || '获取知识图谱失败' },
            }));
          }
        },
        
        // 获取生成的文档
        fetchGeneratedDocs: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, docs: true },
            errors: { ...state.errors, docs: null },
          }));
          
          try {
            const response = await extractApi.getGeneratedDocs(taskId);
            set((state) => ({
              generatedDocs: response.data,
              loading: { ...state.loading, docs: false },
            }));
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, docs: false },
              errors: { ...state.errors, docs: error.message || '获取文档失败' },
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
            const response = await extractApi.createExtractTask(data);
            set((state) => ({
              loading: { ...state.loading, create: false },
            }));
            // 刷新列表
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
        
        // 暂停任务
        pauseTask: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, update: true },
            errors: { ...state.errors, update: null },
          }));
          
          try {
            await extractApi.toggleExtractTask(taskId, 'pause');
            set((state) => ({
              loading: { ...state.loading, update: false },
            }));
            get().fetchTasks();
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, update: false },
              errors: { ...state.errors, update: error.message || '暂停任务失败' },
            }));
          }
        },
        
        // 继续任务
        resumeTask: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, update: true },
            errors: { ...state.errors, update: null },
          }));
          
          try {
            await extractApi.toggleExtractTask(taskId, 'resume');
            set((state) => ({
              loading: { ...state.loading, update: false },
            }));
            get().fetchTasks();
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, update: false },
              errors: { ...state.errors, update: error.message || '继续任务失败' },
            }));
          }
        },
        
        // 取消任务
        cancelTask: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, update: true },
            errors: { ...state.errors, update: null },
          }));
          
          try {
            await extractApi.cancelExtractTask(taskId);
            set((state) => ({
              loading: { ...state.loading, update: false },
            }));
            get().fetchTasks();
          } catch (error: any) {
            set((state) => ({
              loading: { ...state.loading, update: false },
              errors: { ...state.errors, update: error.message || '取消任务失败' },
            }));
          }
        },
        
        // 删除任务
        deleteTask: async (taskId: string) => {
          set((state) => ({
            loading: { ...state.loading, update: true },
            errors: { ...state.errors, update: null },
          }));
          
          try {
            await extractApi.deleteExtractTask(taskId);
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
            knowledgeGraph: null,
            generatedDocs: [],
          });
        },
        
        // 重置状态
        reset: () => {
          set(initialState);
        },
      }),
      {
        name: 'extract-store',
        partialize: (state) => ({
          taskPage: state.taskPage,
          taskPageSize: state.taskPageSize,
          queryParams: state.queryParams,
        }),
      }
    ),
    { name: 'extract-store' }
  )
);

// ========== 选择器 ==========

export const selectTasks = (state: ExtractState) => state.tasks;
export const selectCurrentTask = (state: ExtractState) => state.currentTask;
export const selectKnowledgeGraph = (state: ExtractState) => state.knowledgeGraph;
export const selectPagination = (state: ExtractState) => ({
  page: state.taskPage,
  pageSize: state.taskPageSize,
  total: state.taskTotal,
});
