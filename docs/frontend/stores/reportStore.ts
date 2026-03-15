/**
 * 报告管理状态管理 Store
 */

import { create } from 'zustand';
import type { RefactorReport, ReportVersion } from '../api-types';
import * as reportApi from '../api/report';

interface ReportState {
  reports: RefactorReport[];
  currentReport: RefactorReport | null;
  versions: ReportVersion[];
  loading: boolean;
  statistics: {
    total: number;
    pending: number;
    approved: number;
    archived: number;
    starred: number;
    thisMonth: number;
  };

  // Actions
  fetchReports: (filters?: any) => Promise<void>;
  fetchReport: (id: string) => Promise<void>;
  createReport: (data: Partial<RefactorReport>) => Promise<RefactorReport>;
  updateReport: (id: string, data: Partial<RefactorReport>) => Promise<void>;
  deleteReport: (id: string) => Promise<void>;
  archiveReport: (id: string) => Promise<void>;
  restoreReport: (id: string) => Promise<void>;
  exportReport: (id: string, format: string) => Promise<Blob>;
  fetchVersions: (reportId: string) => Promise<ReportVersion[]>;
  compareVersions: (v1: string, v2: string) => Promise<any>;
  toggleStar: (id: string) => Promise<void>;
  addTag: (id: string, tag: string) => Promise<void>;
  removeTag: (id: string, tag: string) => Promise<void>;
}

export const useReportStore = create<ReportState>((set, get) => ({
  reports: [],
  currentReport: null,
  versions: [],
  loading: false,
  statistics: {
    total: 0,
    pending: 0,
    approved: 0,
    archived: 0,
    starred: 0,
    thisMonth: 0,
  },

  fetchReports: async (filters = {}) => {
    set({ loading: true });
    try {
      const response = await reportApi.getReports(filters);
      set({
        reports: response.data,
        statistics: response.statistics,
      });
    } catch (error) {
      console.error('Failed to fetch reports:', error);
    } finally {
      set({ loading: false });
    }
  },

  fetchReport: async (id: string) => {
    set({ loading: true });
    try {
      const report = await reportApi.getReport(id);
      set({ currentReport: report });
    } catch (error) {
      console.error('Failed to fetch report:', error);
    } finally {
      set({ loading: false });
    }
  },

  createReport: async (data: Partial<RefactorReport>) => {
    const report = await reportApi.createReport(data);
    set(state => ({
      reports: [...state.reports, report],
    }));
    return report;
  },

  updateReport: async (id: string, data: Partial<RefactorReport>) => {
    const report = await reportApi.updateReport(id, data);
    set(state => ({
      reports: state.reports.map(r => r.report_id === id ? report : r),
      currentReport: state.currentReport?.report_id === id ? report : state.currentReport,
    }));
  },

  deleteReport: async (id: string) => {
    await reportApi.deleteReport(id);
    set(state => ({
      reports: state.reports.filter(r => r.report_id !== id),
      currentReport: state.currentReport?.report_id === id ? null : state.currentReport,
    }));
  },

  archiveReport: async (id: string) => {
    await reportApi.archiveReport(id);
    set(state => ({
      reports: state.reports.map(r =>
        r.report_id === id ? { ...r, status: 'archived' as const } : r
      ),
    }));
  },

  restoreReport: async (id: string) => {
    await reportApi.restoreReport(id);
    set(state => ({
      reports: state.reports.map(r =>
        r.report_id === id ? { ...r, status: 'draft' as const } : r
      ),
    }));
  },

  exportReport: async (id: string, format: string) => {
    const blob = await reportApi.exportReport(id, format);
    return blob;
  },

  fetchVersions: async (reportId: string) => {
    const versions = await reportApi.getVersions(reportId);
    set({ versions });
    return versions;
  },

  compareVersions: async (v1: string, v2: string) => {
    const diff = await reportApi.compareVersions(v1, v2);
    return diff;
  },

  toggleStar: async (id: string) => {
    await reportApi.toggleStar(id);
    set(state => ({
      reports: state.reports.map(r =>
        r.report_id === id ? { ...r, starred: !r.starred } : r
      ),
    }));
  },

  addTag: async (id: string, tag: string) => {
    await reportApi.addTag(id, tag);
    set(state => ({
      reports: state.reports.map(r =>
        r.report_id === id
          ? { ...r, tags: [...(r.tags || []), tag] }
          : r
      ),
    }));
  },

  removeTag: async (id: string, tag: string) => {
    await reportApi.removeTag(id, tag);
    set(state => ({
      reports: state.reports.map(r =>
        r.report_id === id
          ? { ...r, tags: (r.tags || []).filter(t => t !== tag) }
          : r
      ),
    }));
  },
}));
