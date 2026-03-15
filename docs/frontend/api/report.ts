// src/api/report.ts
import api from './config';

// ========== 类型定义 ==========

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
  problems: ProblemSummary[];
  
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

export interface ProblemSummary {
  problemId: string;
  problemType: string;
  severity: 'high' | 'medium' | 'low';
  file: string;
  line: number;
  description: string;
  estimatedHours: number;
  userConfirmed?: boolean;
  ignored?: boolean;
}

export interface ReportDetail extends RefactorReport {
  // 详细问题列表
  problemDetails: any[];
  
  // 项目摘要
  projectSummary: {
    name: string;
    languages: string[];
    totalFiles: number;
    totalLines: number;
    analyzedAt: string;
  };
  
  // 统计图表数据
  charts: {
    severityDistribution: Array<{ severity: string; count: number }>;
    typeDistribution: Array<{ type: string; count: number }>;
    fileHeatmap: Array<{ file: string; count: number }>;
  };
}

export interface ReportVersion {
  version: number;
  reportId: string;
  status: string;
  problemCount: number;
  createdAt: string;
  summary: string;
}

export interface ExportOptions {
  format: 'pdf' | 'html' | 'markdown' | 'json';
  includeDetails?: boolean;
  includeCodeContext?: boolean;
  language?: 'zh' | 'en';
}

// ========== API 函数 ==========

/**
 * 获取报告列表
 */
export const getReportList = (params?: {
  page?: number;
  pageSize?: number;
  status?: string;
  keyword?: string;
}) =>
  api.get<{ items: RefactorReport[]; total: number }>('/report/list', { params });

/**
 * 获取报告详情
 */
export const getReportDetail = (reportId: string) =>
  api.get<ReportDetail>(`/report/${reportId}`);

/**
 * 获取报告版本列表
 */
export const getReportVersions = (sessionId: string) =>
  api.get<ReportVersion[]>(`/report/versions/${sessionId}`);

/**
 * 对比两个版本
 */
export const compareReportVersions = (reportId1: string, reportId2: string) =>
  api.get<{
    added: ProblemSummary[];
    removed: ProblemSummary[];
    modified: Array<{
      problemId: string;
      before: any;
      after: any;
    }>;
  }>(`/report/compare`, {
    params: { reportId1, reportId2 },
  });

/**
 * 导出报告
 */
export const exportReport = (reportId: string, options: ExportOptions) =>
  api.get(`/report/${reportId}/export/${options.format}`, {
    params: {
      includeDetails: options.includeDetails,
      includeCodeContext: options.includeCodeContext,
      language: options.language,
    },
    responseType: 'blob',
  });

/**
 * 导出为PDF
 */
export const exportReportPdf = (reportId: string) =>
  exportReport(reportId, { format: 'pdf', includeDetails: true });

/**
 * 导出为HTML
 */
export const exportReportHtml = (reportId: string) =>
  exportReport(reportId, { format: 'html', includeDetails: true });

/**
 * 导出为Markdown
 */
export const exportReportMarkdown = (reportId: string) =>
  exportReport(reportId, { format: 'markdown', includeDetails: true });

/**
 * 删除报告
 */
export const deleteReport = (reportId: string) =>
  api.delete(`/report/${reportId}`);

/**
 * 获取报告分享链接
 */
export const getReportShareLink = (reportId: string, expiresIn?: number) =>
  api.post<{ shareUrl: string; expiresAt: string }>(`/report/${reportId}/share`, {
    expiresIn,
  });

/**
 * 获取公开报告 (通过分享链接)
 */
export const getPublicReport = (shareId: string) =>
  api.get<ReportDetail>(`/report/public/${shareId}`);
