/**
 * 前端 TypeScript 类型定义
 * 文件路径: src/types/index.ts
 */

// ============================================================
// 通用类型
// ============================================================

/** 分页请求参数 */
export interface PaginationParams {
  page?: number;
  pageSize?: number;
}

/** 分页响应结果 */
export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

/** API 响应包装 */
export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// ============================================================
// Dashboard 类型
// ============================================================

/** 仪表盘统计数据 */
export interface DashboardStats {
  projectCount: number;
  entityCount: number;
  problemCount: number;
  pendingReviewCount: number;
  projectTrend: Trend;
  entityTrend: Trend;
  problemTrend: Trend;
}

/** 趋势数据 */
export interface Trend {
  type: 'up' | 'down' | 'none';
  value: number;
}

/** 知识图谱统计 */
export interface KnowledgeStats {
  entityTypes: Array<{ type: string; count: number }>;
  relationTypes: Array<{ type: string; count: number }>;
  modules: Array<{ name: string; entityCount: number }>;
}

/** 重构问题统计 */
export interface RefactorStats {
  bySeverity: Array<{ severity: string; count: number }>;
  byType: Array<{ type: string; count: number }>;
  byModule: Array<{ module: string; count: number }>;
}

/** 最近任务 */
export interface RecentTasks {
  extractTasks: ExtractTaskSummary[];
  refactorTasks: RefactorTaskSummary[];
}

// ============================================================
// 知识提取类型
// ============================================================

/** 提取任务状态 */
export type ExtractTaskStatus = 
  | 'pending' 
  | 'running' 
  | 'paused' 
  | 'completed' 
  | 'failed' 
  | 'cancelled';

/** 提取任务摘要 */
export interface ExtractTaskSummary {
  id: string;
  projectName: string;
  status: ExtractTaskStatus;
  progress: number;
  entityCount?: number;
  createdAt: string;
  updatedAt: string;
}

/** 提取任务详情 */
export interface ExtractTaskDetail extends ExtractTaskSummary {
  projectPath: string;
  languages: string[];
  outputDir: string;
  config: ExtractConfig;
  startedAt?: string;
  completedAt?: string;
  duration?: number;
  errors: TaskError[];
  stats: ExtractStats;
}

/** 提取配置 */
export interface ExtractConfig {
  includeTestFiles: boolean;
  excludePatterns: string[];
  maxFileSize: number;
  parseOptions: Record<string, any>;
}

/** 提取统计 */
export interface ExtractStats {
  totalFiles: number;
  parsedFiles: number;
  failedFiles: number;
  entities: EntityStats;
  relations: number;
  documents: number;
}

/** 实体统计 */
export interface EntityStats {
  total: number;
  byType: Record<string, number>;
}

/** 任务错误 */
export interface TaskError {
  code: string;
  message: string;
  timestamp: string;
  details?: any;
}

/** 创建提取任务输入 */
export interface CreateExtractTaskInput {
  projectName: string;
  projectPath: string;
  languages: string[];
  outputDir?: string;
  config?: Partial<ExtractConfig>;
}

// ============================================================
// 重构分析类型
// ============================================================

/** 重构任务状态 */
export type RefactorTaskStatus = 
  | 'pending' 
  | 'analyzing' 
  | 'completed' 
  | 'failed';

/** 重构任务摘要 */
export interface RefactorTaskSummary {
  id: string;
  projectName: string;
  status: RefactorTaskStatus;
  problemCount: number;
  highSeverityCount: number;
  estimatedHours: number;
  createdAt: string;
}

/** 重构任务详情 */
export interface RefactorTaskDetail extends RefactorTaskSummary {
  knowledgePath: string;
  config: RefactorConfig;
  startedAt?: string;
  completedAt?: string;
  reportId?: string;
}

/** 重构配置 */
export interface RefactorConfig {
  analyzeDepth: 'shallow' | 'medium' | 'deep';
  focusAreas: string[];
  excludePatterns: string[];
  customRules: string[];
}

/** 创建重构任务输入 */
export interface CreateRefactorTaskInput {
  projectName: string;
  knowledgePath: string;
  config?: Partial<RefactorConfig>;
}

// ============================================================
// 重构报告类型
// ============================================================

/** 严重程度 */
export type Severity = 'high' | 'medium' | 'low';

/** 风险等级 */
export type RiskLevel = 'high' | 'medium' | 'low';

/** 问题详情 */
export interface ProblemDetail {
  problemId: string;
  problemType: string;
  severity: Severity;
  module: string;
  filePath: string;
  lineStart?: number;
  lineEnd?: number;
  entityName: string;
  entityType: string;
  
  // 一、是什么
  description: string;
  currentState: Record<string, any>;
  codeContext?: string;
  
  // 二、怎么修复
  fixSteps: FixStep[];
  
  // 三、有什么风险
  risks: Risk[];
  riskLevel: RiskLevel;
  rollbackPlan?: string;
  
  // 四、有什么方案
  solutions: Solution[];
  recommendedSolution: number;
  
  // 用户反馈状态
  userConfirmed: boolean;
  ignored: boolean;
  userComment?: string;
  ignoreReason?: string;
  
  // 元数据
  createdAt: string;
  tags: string[];
}

/** 修复步骤 */
export interface FixStep {
  step: number;
  title: string;
  description: string;
  codeExample?: string;
  estimatedTime?: number;
}

/** 风险项 */
export interface Risk {
  type: string;
  description: string;
  probability: 'high' | 'medium' | 'low';
  impact: 'high' | 'medium' | 'low';
  mitigation: string;
}

/** 解决方案 */
export interface Solution {
  id: string;
  name: string;
  description: string;
  steps: string[];
  workloadHours: number;
  riskLevel: RiskLevel;
  recommendation: number;
  pros?: string[];
  cons?: string[];
}

/** 问题摘要 */
export interface ProblemSummary {
  totalProblems: number;
  highSeverity: number;
  mediumSeverity: number;
  lowSeverity: number;
  estimatedWorkloadHours: number;
  problemTypes: Record<string, number>;
}

/** 模块健康度 */
export interface ModuleHealth {
  moduleName: string;
  healthScore: number;
  problemCount: number;
  highSeverityCount: number;
  mainIssues: string[];
}

/** 执行计划 */
export interface ExecutionPlan {
  phase: string;
  priority: string;
  problems: string[];
  estimatedHours: number;
  description: string;
}

/** 变更日志 */
export interface ChangeLog {
  addedProblems: number;
  removedProblems: number;
  modifiedProblems: number;
  addedSolutions: number;
  confirmedProblems: number;
  ignoredProblems: number;
  details?: {
    removed: string[];
    modified: string[];
    added: string[];
    ignored: string[];
    confirmed: string[];
  };
}

/** 重构报告 */
export interface RefactorReport {
  reportId: string;
  projectName: string;
  generatedAt: string;
  
  // 版本控制
  version: number;
  parentReportId?: string;
  feedbackBatchId?: string;
  changeLog?: ChangeLog;
  
  // 执行摘要
  summary: ProblemSummary;
  
  // 问题详情
  problems: ProblemDetail[];
  
  // 模块健康度
  moduleHealth: ModuleHealth[];
  
  // 执行计划
  executionPlan: ExecutionPlan[];
  
  // 文件问题汇总
  fileProblems: Record<string, string[]>;
}

// ============================================================
// 反馈闭环类型
// ============================================================

/** 反馈类型 */
export type FeedbackType =
  | 'agree'            // 同意该问题
  | 'disagree'         // 不同意该问题
  | 'modify'           // 需要修改问题
  | 'accept_solution'  // 接受方案
  | 'reject_solution'  // 拒绝方案
  | 'modify_solution'  // 修改方案
  | 'add_solution'     // 添加新方案
  | 'raise_priority'   // 提高优先级
  | 'lower_priority'   // 降低优先级
  | 'add_problem'      // 添加新问题
  | 'ignore_problem';  // 忽略问题

/** 反馈作用范围 */
export type FeedbackScope = 
  | 'problem' 
  | 'solution' 
  | 'file' 
  | 'module' 
  | 'global';

/** 反馈处理状态 */
export type FeedbackStatus = 
  | 'pending' 
  | 'processing' 
  | 'applied' 
  | 'rejected';

/** 用户反馈 */
export interface UserFeedback {
  feedbackId: string;
  sessionId: string;
  problemId?: string;
  solutionId?: string;
  scope: FeedbackScope;
  feedbackType: FeedbackType;
  content: string;
  suggestedFix?: string;
  suggestedSolution?: Partial<Solution>;
  suggestedPriority?: Severity;
  suggestedSeverity?: Severity;
  createdAt: string;
  userId?: string;
  context?: Record<string, any>;
  status: FeedbackStatus;
  appliedChanges?: Record<string, any>;
}

/** 反馈批次 */
export interface FeedbackBatch {
  batchId: string;
  sessionId: string;
  feedbacks: UserFeedback[];
  totalCount: number;
  byType: Record<string, number>;
  byScope: Record<string, number>;
  createdAt: string;
}

/** 反馈会话 */
export interface FeedbackSession {
  sessionId: string;
  projectName: string;
  reportId: string;
  status: 'active' | 'completed' | 'archived';
  iteration: number;
  createdAt: string;
  updatedAt: string;
  feedbackCount: number;
  convergenceRate: number;
}

/** 创建反馈输入 */
export interface CreateFeedbackInput {
  sessionId: string;
  problemId?: string;
  solutionId?: string;
  scope?: FeedbackScope;
  feedbackType: FeedbackType;
  content?: string;
  suggestedFix?: string;
  suggestedSolution?: Partial<Solution>;
  suggestedPriority?: Severity;
}

/** 批量创建反馈输入 */
export interface CreateFeedbackBatchInput {
  sessionId: string;
  feedbacks: CreateFeedbackInput[];
}

// ============================================================
// 审核工作流类型
// ============================================================

/** 审核状态 */
export type ReviewStatus =
  | 'draft'              // 草稿
  | 'pending_review'     // 待审核
  | 'in_review'          // 审核中
  | 'feedback_submitted' // 已提交反馈
  | 'regenerating'       // 重生成中
  | 'approved'           // 已批准
  | 'rejected'           // 已拒绝
  | 'finalized';         // 已定稿

/** 审核动作 */
export type ReviewAction =
  | 'submit'              // 提交审核
  | 'start_review'        // 开始审核
  | 'submit_feedback'     // 提交反馈
  | 'request_regenerate'  // 请求重生成
  | 'approve'             // 批准
  | 'reject'              // 拒绝
  | 'finalize'            // 定稿
  | 'rollback';           // 回滚

/** 动作记录 */
export interface ActionRecord {
  action: string;
  fromStatus: string;
  toStatus: string;
  userId?: string;
  comments?: string;
  timestamp: string;
  extraData?: Record<string, any>;
}

/** 审核记录 */
export interface ReviewRecord {
  recordId: string;
  sessionId: string;
  reportId: string;
  version: number;
  status: ReviewStatus;
  reviewerId?: string;
  reviewerName?: string;
  createdAt: string;
  submittedAt?: string;
  reviewedAt?: string;
  finalizedAt?: string;
  feedbackBatchIds: string[];
  regenerateCount: number;
  actionHistory: ActionRecord[];
  approvalDecision?: string;
  approvalComments?: string;
}

/** 收敛检测结果 */
export interface ConvergenceResult {
  converged: boolean;
  reason: 'low_change_rate' | 'high_confirmation_rate' | 'high_ignore_rate' | 'insufficient_history' | 'still_evolving';
  changeRate?: number;
  confirmationRate?: number;
  ignoreRate?: number;
  message: string;
}

/** 执行审核动作输入 */
export interface ExecuteReviewActionInput {
  sessionId: string;
  recordId: string;
  action: ReviewAction;
  userId?: string;
  comments?: string;
  feedbackBatchId?: string;
}

// ============================================================
// 报告管理类型
// ============================================================

/** 报告类型 */
export type ReportType = 'extract' | 'refactor';

/** 报告摘要 */
export interface ReportSummary {
  id: string;
  name: string;
  projectName: string;
  type: ReportType;
  version: number;
  createdAt: string;
  status: 'draft' | 'finalized';
}

/** 报告详情 */
export interface ReportDetail extends ReportSummary {
  content: string;
  metadata: Record<string, any>;
  downloadUrls: {
    pdf?: string;
    html?: string;
    json?: string;
  };
}

// ============================================================
// 系统设置类型
// ============================================================

/** LLM 配置 */
export interface LLMConfig {
  provider: string;
  model: string;
  apiKey: string;
  apiEndpoint?: string;
  temperature?: number;
  maxTokens?: number;
}

/** 数据库配置 */
export interface DatabaseConfig {
  postgresql: {
    host: string;
    port: number;
    database: string;
    username: string;
    password: string;
  };
  milvus: {
    host: string;
    port: number;
    collection: string;
  };
  neo4j: {
    uri: string;
    username: string;
    password: string;
  };
  redis: {
    host: string;
    port: number;
    db: number;
  };
}

/** 任务队列配置 */
export interface TaskQueueConfig {
  workerCount: number;
  concurrency: number;
  timeout: number;
  retryCount: number;
}

/** 扫描规则配置 */
export interface ScanRuleConfig {
  fileFilters: string[];
  languageFilters: string[];
  complexityThreshold: number;
  maxFileSize: number;
}

/** 重构规则配置 */
export interface RefactorRuleConfig {
  codingStandards: string[];
  architecturePatterns: string[];
  riskThresholds: Record<string, number>;
}

/** 系统设置 */
export interface SystemSettings {
  general: {
    platformName: string;
    logo?: string;
    defaultBranch: string;
    defaultOutputDir: string;
  };
  llm: LLMConfig;
  database: DatabaseConfig;
  taskQueue: TaskQueueConfig;
  scanRules: ScanRuleConfig;
  refactorRules: RefactorRuleConfig;
}

// ============================================================
// WebSocket 消息类型
// ============================================================

/** WebSocket 消息类型 */
export type WSMessageType =
  | 'task_progress'
  | 'task_completed'
  | 'task_failed'
  | 'report_updated'
  | 'review_status_changed';

/** WebSocket 消息 */
export interface WSMessage<T = any> {
  type: WSMessageType;
  payload: T;
  timestamp: string;
}

/** 任务进度消息 */
export interface TaskProgressPayload {
  taskId: string;
  taskType: 'extract' | 'refactor';
  progress: number;
  currentStep: string;
  message?: string;
}

/** 报告更新消息 */
export interface ReportUpdatedPayload {
  reportId: string;
  version: number;
  changes: ChangeLog;
}

/** 审核状态变更消息 */
export interface ReviewStatusChangedPayload {
  recordId: string;
  oldStatus: ReviewStatus;
  newStatus: ReviewStatus;
  actionBy?: string;
}
