/**
 * 模拟数据服务
 * 提供所有页面的模拟数据
 */

// ========== 类型定义 ==========

export interface ExtractTask {
  taskId: string;
  projectName: string;
  projectPath: string;
  languages: string[];
  status: 'pending' | 'running' | 'completed' | 'failed' | 'paused';
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
    filesTotal: number;
    entitiesByType: Record<string, number>;
    relationsByType: Record<string, number>;
  };
  errors: Array<{
    file: string;
    message: string;
    timestamp: string;
  }>;
}

export interface KnowledgeEntity {
  id: string;
  type: string;
  name: string;
  description?: string;
  filePath: string;
  lineStart: number;
  lineEnd: number;
  code?: string;
}

export interface KnowledgeRelation {
  id: string;
  type: string;
  sourceId: string;
  targetId: string;
  sourceName: string;
  targetName: string;
}

export interface RefactorTask {
  taskId: string;
  projectName: string;
  knowledgePath: string;
  status: 'pending' | 'analyzing' | 'completed' | 'failed';
  problemCount: number;
  highSeverityCount: number;
  estimatedHours: number;
  createdAt: string;
  completedAt?: string;
}

export interface Problem {
  problemId: string;
  problemType: string;
  severity: 'high' | 'medium' | 'low';
  module: string;
  filePath: string;
  lineStart: number;
  lineEnd: number;
  entityName: string;
  description: string;
  solutions: Array<{
    id: string;
    name: string;
    description: string;
    steps: string[];
    workloadHours: number;
  }>;
  recommendedSolution: number;
  status: 'pending' | 'confirmed' | 'ignored';
}

export interface RefactorReport {
  reportId: string;
  taskId: string;
  projectName: string;
  generatedAt: string;
  version: number;
  status: 'draft' | 'pending_review' | 'approved' | 'rejected' | 'finalized';
  summary: {
    totalProblems: number;
    highSeverity: number;
    mediumSeverity: number;
    lowSeverity: number;
    estimatedHours: number;
  };
  problems: Problem[];
}

export interface FeedbackSession {
  sessionId: string;
  projectName: string;
  reportId: string;
  status: 'open' | 'in_review' | 'converged' | 'closed';
  totalFeedbacks: number;
  pendingReviews: number;
  createdAt: string;
  updatedAt: string;
}

export interface Feedback {
  feedbackId: string;
  sessionId: string;
  problemId: string;
  type: 'confirm' | 'ignore' | 'modify' | 'suggest';
  content: string;
  createdAt: string;
}

// ========== 模拟数据 ==========

const extractTasks: ExtractTask[] = [
  {
    taskId: 'ext-001',
    projectName: 'stock-service',
    projectPath: '/projects/stock-service',
    languages: ['Java', 'XML'],
    status: 'completed',
    progress: 100,
    entityCount: 5420,
    relationCount: 3250,
    documentCount: 12,
    createdAt: '2026-03-10 09:00:00',
    startedAt: '2026-03-10 09:01:00',
    completedAt: '2026-03-10 10:30:00',
  },
  {
    taskId: 'ext-002',
    projectName: 'user-center',
    projectPath: '/projects/user-center',
    languages: ['Java', 'Kotlin'],
    status: 'running',
    progress: 65,
    entityCount: 3200,
    relationCount: 1800,
    documentCount: 5,
    createdAt: '2026-03-15 08:00:00',
    startedAt: '2026-03-15 08:05:00',
  },
  {
    taskId: 'ext-003',
    projectName: 'order-system',
    projectPath: '/projects/order-system',
    languages: ['Java', 'SQL'],
    status: 'pending',
    progress: 0,
    entityCount: 0,
    relationCount: 0,
    documentCount: 0,
    createdAt: '2026-03-15 10:00:00',
  },
  {
    taskId: 'ext-004',
    projectName: 'payment-gateway',
    projectPath: '/projects/payment-gateway',
    languages: ['Java'],
    status: 'failed',
    progress: 23,
    entityCount: 800,
    relationCount: 450,
    documentCount: 0,
    createdAt: '2026-03-14 14:00:00',
    startedAt: '2026-03-14 14:05:00',
    error: '解析文件失败: pom.xml 格式错误',
  },
  {
    taskId: 'ext-005',
    projectName: 'inventory-service',
    projectPath: '/projects/inventory-service',
    languages: ['Java', 'XML'],
    status: 'paused',
    progress: 45,
    entityCount: 2100,
    relationCount: 1200,
    documentCount: 3,
    createdAt: '2026-03-13 11:00:00',
    startedAt: '2026-03-13 11:10:00',
  },
];

const refactorTasks: RefactorTask[] = [
  {
    taskId: 'ref-001',
    projectName: 'stock-service',
    knowledgePath: '/output/stock-service/knowledge.json',
    status: 'completed',
    problemCount: 45,
    highSeverityCount: 8,
    estimatedHours: 120,
    createdAt: '2026-03-10 11:00:00',
    completedAt: '2026-03-10 14:30:00',
  },
  {
    taskId: 'ref-002',
    projectName: 'user-center',
    knowledgePath: '/output/user-center/knowledge.json',
    status: 'analyzing',
    problemCount: 0,
    highSeverityCount: 0,
    estimatedHours: 0,
    createdAt: '2026-03-15 09:00:00',
  },
  {
    taskId: 'ref-003',
    projectName: 'order-system',
    knowledgePath: '/output/order-system/knowledge.json',
    status: 'pending',
    problemCount: 0,
    highSeverityCount: 0,
    estimatedHours: 0,
    createdAt: '2026-03-15 10:30:00',
  },
];

const refactorReports: RefactorReport[] = [
  {
    reportId: 'rpt-001',
    taskId: 'ref-001',
    projectName: 'stock-service',
    generatedAt: '2026-03-10 14:30:00',
    version: 3,
    status: 'approved',
    summary: {
      totalProblems: 45,
      highSeverity: 8,
      mediumSeverity: 22,
      lowSeverity: 15,
      estimatedHours: 120,
    },
    problems: [
      {
        problemId: 'prob-001',
        problemType: '循环依赖',
        severity: 'high',
        module: 'com.stock.service',
        filePath: 'src/main/java/com/stock/service/StockService.java',
        lineStart: 45,
        lineEnd: 80,
        entityName: 'StockService',
        description: 'StockService 与 DataService 之间存在循环依赖，导致代码耦合度高，难以维护和测试。',
        solutions: [
          {
            id: 'sol-001',
            name: '引入中介者模式',
            description: '创建一个中介者类来协调 StockService 和 DataService 的交互',
            steps: [
              '创建 StockDataMediator 类',
              '将共同依赖的逻辑提取到中介者中',
              '修改 StockService 和 DataService 使用中介者',
              '添加单元测试验证重构结果',
            ],
            workloadHours: 16,
          },
          {
            id: 'sol-002',
            name: '提取公共接口',
            description: '提取一个公共接口来解耦两个服务',
            steps: [
              '定义 StockDataInterface 接口',
              'StockService 实现该接口',
              'DataService 依赖接口而非具体实现',
            ],
            workloadHours: 8,
          },
        ],
        recommendedSolution: 1,
        status: 'pending',
      },
      {
        problemId: 'prob-002',
        problemType: '过长方法',
        severity: 'medium',
        module: 'com.stock.service',
        filePath: 'src/main/java/com/stock/service/TradeService.java',
        lineStart: 120,
        lineEnd: 350,
        entityName: 'executeTrade',
        description: 'executeTrade 方法超过 200 行，包含多个业务逻辑分支，难以理解和维护。',
        solutions: [
          {
            id: 'sol-003',
            name: '方法拆分',
            description: '将大方法拆分为多个职责单一的小方法',
            steps: [
              '识别方法内的独立功能块',
              '提取 validateOrder 方法',
              '提取 calculatePrice 方法',
              '提取 executeTransaction 方法',
              '重构主方法调用各个子方法',
            ],
            workloadHours: 12,
          },
        ],
        recommendedSolution: 0,
        status: 'pending',
      },
      {
        problemId: 'prob-003',
        problemType: '代码重复',
        severity: 'low',
        module: 'com.stock.util',
        filePath: 'src/main/java/com/stock/util/DateUtils.java',
        lineStart: 25,
        lineEnd: 45,
        entityName: 'formatDate',
        description: '日期格式化代码在多处重复出现，建议提取为工具方法。',
        solutions: [
          {
            id: 'sol-004',
            name: '提取工具方法',
            description: '统一日期格式化逻辑',
            steps: ['将重复代码合并到 DateUtils', '替换所有调用点', '添加单元测试'],
            workloadHours: 4,
          },
        ],
        recommendedSolution: 0,
        status: 'pending',
      },
    ],
  },
];

const feedbackSessions: FeedbackSession[] = [
  {
    sessionId: 'fb-001',
    projectName: 'stock-service',
    reportId: 'rpt-001',
    status: 'in_review',
    totalFeedbacks: 23,
    pendingReviews: 5,
    createdAt: '2026-03-11 09:00:00',
    updatedAt: '2026-03-15 10:30:00',
  },
  {
    sessionId: 'fb-002',
    projectName: 'user-center',
    reportId: 'rpt-002',
    status: 'open',
    totalFeedbacks: 0,
    pendingReviews: 15,
    createdAt: '2026-03-15 11:00:00',
    updatedAt: '2026-03-15 11:00:00',
  },
  {
    sessionId: 'fb-003',
    projectName: 'payment-gateway',
    reportId: 'rpt-003',
    status: 'converged',
    totalFeedbacks: 45,
    pendingReviews: 0,
    createdAt: '2026-03-10 10:00:00',
    updatedAt: '2026-03-14 16:00:00',
  },
];

// ========== API 模拟函数 ==========

export const mockApi = {
  // 提取任务相关
  getExtractTasks: async (params?: { status?: string; keyword?: string; page?: number; pageSize?: number }) => {
    await delay(300);
    let filtered = [...extractTasks];
    if (params?.status && params.status !== 'all') {
      filtered = filtered.filter(t => t.status === params.status);
    }
    if (params?.keyword) {
      filtered = filtered.filter(t => 
        t.projectName.toLowerCase().includes(params.keyword!.toLowerCase())
      );
    }
    const page = params?.page || 1;
    const pageSize = params?.pageSize || 10;
    const start = (page - 1) * pageSize;
    const items = filtered.slice(start, start + pageSize);
    return { items, total: filtered.length, page, pageSize };
  },

  getExtractTask: async (taskId: string): Promise<ExtractTaskDetail | null> => {
    await delay(200);
    const task = extractTasks.find(t => t.taskId === taskId);
    if (!task) return null;
    return {
      ...task,
      config: {
        includeTestFiles: false,
        excludePatterns: ['**/target/**', '**/node_modules/**'],
        maxFileSize: 1024 * 1024,
        outputDir: `/output/${task.projectName}`,
      },
      statistics: {
        filesProcessed: Math.floor(task.entityCount / 50),
        filesTotal: Math.floor(task.entityCount / 45),
        entitiesByType: {
          Class: Math.floor(task.entityCount * 0.4),
          Method: Math.floor(task.entityCount * 0.45),
          Field: Math.floor(task.entityCount * 0.1),
          Interface: Math.floor(task.entityCount * 0.05),
        },
        relationsByType: {
          calls: Math.floor(task.relationCount * 0.5),
          implements: Math.floor(task.relationCount * 0.2),
          extends: Math.floor(task.relationCount * 0.15),
          uses: Math.floor(task.relationCount * 0.15),
        },
      },
      errors: task.status === 'failed' ? [
        { file: 'pom.xml', message: '解析文件失败: XML格式错误', timestamp: '2026-03-14 14:10:00' }
      ] : [],
    };
  },

  getKnowledgeEntities: async (taskId: string): Promise<KnowledgeEntity[]> => {
    await delay(200);
    return [
      {
        id: 'ent-001',
        type: 'Class',
        name: 'StockService',
        description: '股票数据服务类，提供股票数据的增删改查功能',
        filePath: 'src/main/java/com/stock/service/StockService.java',
        lineStart: 15,
        lineEnd: 150,
      },
      {
        id: 'ent-002',
        type: 'Method',
        name: 'getStockPrice',
        description: '获取股票实时价格',
        filePath: 'src/main/java/com/stock/service/StockService.java',
        lineStart: 45,
        lineEnd: 65,
      },
      {
        id: 'ent-003',
        type: 'Class',
        name: 'DataService',
        description: '数据访问服务类',
        filePath: 'src/main/java/com/stock/service/DataService.java',
        lineStart: 10,
        lineEnd: 100,
      },
    ];
  },

  // 重构任务相关
  getRefactorTasks: async (params?: { status?: string; keyword?: string; page?: number; pageSize?: number }) => {
    await delay(300);
    let filtered = [...refactorTasks];
    if (params?.status && params.status !== 'all') {
      filtered = filtered.filter(t => t.status === params.status);
    }
    if (params?.keyword) {
      filtered = filtered.filter(t => 
        t.projectName.toLowerCase().includes(params.keyword!.toLowerCase())
      );
    }
    const page = params?.page || 1;
    const pageSize = params?.pageSize || 10;
    const start = (page - 1) * pageSize;
    const items = filtered.slice(start, start + pageSize);
    return { items, total: filtered.length, page, pageSize };
  },

  getRefactorReport: async (reportId: string): Promise<RefactorReport | null> => {
    await delay(300);
    return refactorReports.find(r => r.reportId === reportId) || null;
  },

  // 反馈相关
  getFeedbackSessions: async (params?: { status?: string; page?: number; pageSize?: number }) => {
    await delay(300);
    let filtered = [...feedbackSessions];
    if (params?.status && params.status !== 'all') {
      filtered = filtered.filter(s => s.status === params.status);
    }
    const page = params?.page || 1;
    const pageSize = params?.pageSize || 10;
    const start = (page - 1) * pageSize;
    const items = filtered.slice(start, start + pageSize);
    return { items, total: filtered.length, page, pageSize };
  },

  getFeedbackSession: async (sessionId: string) => {
    await delay(200);
    return feedbackSessions.find(s => s.sessionId === sessionId) || null;
  },

  // 报告管理
  getReports: async (params?: { status?: string; keyword?: string; page?: number; pageSize?: number }) => {
    await delay(300);
    let filtered = [...refactorReports];
    if (params?.status && params.status !== 'all') {
      filtered = filtered.filter(r => r.status === params.status);
    }
    if (params?.keyword) {
      filtered = filtered.filter(r => 
        r.projectName.toLowerCase().includes(params.keyword!.toLowerCase())
      );
    }
    const page = params?.page || 1;
    const pageSize = params?.pageSize || 10;
    const start = (page - 1) * pageSize;
    const items = filtered.slice(start, start + pageSize);
    return { items, total: filtered.length, page, pageSize };
  },

  // 统计数据
  getDashboardStats: async () => {
    await delay(200);
    return {
      projectCount: 12,
      entityCount: 15420,
      problemCount: 89,
      pendingReviewCount: 5,
      projectTrend: { type: 'up', value: 15 },
      entityTrend: { type: 'up', value: 23 },
      problemTrend: { type: 'down', value: 8 },
    };
  },
};

// 辅助函数
function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}
