/**
 * 报告管理页面
 */

import React, { useEffect, useState } from 'react';
import {
  Table, Card, Button, Input, Select, Space, Tag, Modal,
  message, Descriptions, Drawer, Statistic, Row, Col, Badge
} from 'antd';
import {
  ReloadOutlined, EyeOutlined, DownloadOutlined,
  InboxOutlined, StarOutlined, StarFilled, FileTextOutlined
} from '@ant-design/icons';
import { reportApi } from '../services/api';
import type { Report, RefactorReport } from '../api-types';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';

const { Search } = Input;
const { Option } = Select;

const statusConfig: Record<string, { color: string; text: string }> = {
  draft: { color: 'default', text: '草稿' },
  pending_review: { color: 'warning', text: '待审核' },
  approved: { color: 'success', text: '已通过' },
  rejected: { color: 'error', text: '已拒绝' },
  finalized: { color: 'success', text: '已定稿' },
};

const ReportManagement: React.FC = () => {
  const navigate = useNavigate();
  const [reports, setReports] = useState<RefactorReport[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [filters, setFilters] = useState({ status: 'all', keyword: '' });
  const [detailDrawer, setDetailDrawer] = useState<{ visible: boolean; report: RefactorReport | null }>({
    visible: false,
    report: null,
  });

  const loadReports = async () => {
    setLoading(true);
    try {
      const result = await reportApi.getReports({
        status: filters.status === 'all' ? undefined : filters.status,
        keyword: filters.keyword || undefined,
        page: pagination.current,
        pageSize: pagination.pageSize,
      });
      setReports(result.items);
      setPagination(prev => ({ ...prev, total: result.total }));
    } catch (error) {
      message.error('加载报告列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
  }, [pagination.current, pagination.pageSize, filters.status, filters.keyword]);

  const stats = {
    total: pagination.total,
    pending: reports.filter(r => r.status === 'pending_review').length,
    approved: reports.filter(r => r.status === 'approved' || r.status === 'finalized').length,
    totalProblems: reports.reduce((sum, r) => sum + r.summary.totalProblems, 0),
  };

  const handleExport = async (reportId: string) => {
    try {
      await reportApi.exportReport(reportId, 'markdown');
      message.success('报告导出任务已开始');
    } catch (error) {
      message.error('导出报告失败');
    }
  };

  const handleArchive = async (reportId: string) => {
    try {
      await reportApi.archiveReport(reportId);
      message.success('报告已归档');
      loadReports();
    } catch (error) {
      message.error('归档报告失败');
    }
  };

  const columns: ColumnsType<RefactorReport> = [
    {
      title: '报告ID',
      dataIndex: 'reportId',
      key: 'reportId',
      width: 120,
    },
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      width: 180,
    },
    {
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 80,
      render: (v: number) => `v${v}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const config = statusConfig[status] || { color: 'default', text: status };
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '问题总数',
      dataIndex: ['summary', 'totalProblems'],
      key: 'totalProblems',
      width: 100,
      render: (count: number) => <Badge count={count} showZero style={{ backgroundColor: '#1677ff' }} />,
    },
    {
      title: '高优先级',
      dataIndex: ['summary', 'highSeverity'],
      key: 'highSeverity',
      width: 100,
      render: (count: number) => (
        <Badge count={count} showZero style={{ backgroundColor: count > 0 ? '#ff4d4f' : '#52c41a' }} />
      ),
    },
    {
      title: '预估工时',
      dataIndex: ['summary', 'estimatedHours'],
      key: 'estimatedHours',
      width: 100,
      render: (hours: number) => `${hours}h`,
    },
    {
      title: '生成时间',
      dataIndex: 'generatedAt',
      key: 'generatedAt',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => setDetailDrawer({ visible: true, report: record })}
          >
            查看
          </Button>
          <Button
            type="text"
            size="small"
            icon={<DownloadOutlined />}
            onClick={() => handleExport(record.reportId)}
          >
            导出
          </Button>
          <Button
            type="text"
            size="small"
            icon={<InboxOutlined />}
            onClick={() => handleArchive(record.reportId)}
          >
            归档
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="报告总数"
              value={stats.total}
              prefix={<FileTextOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="待审核"
              value={stats.pending}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已通过"
              value={stats.approved}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="问题总数"
              value={stats.totalProblems}
            />
          </Card>
        </Col>
      </Row>

      {/* 报告列表 */}
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Select
              value={filters.status}
              onChange={status => setFilters(prev => ({ ...prev, status }))}
              style={{ width: 120 }}
            >
              <Option value="all">全部状态</Option>
              <Option value="draft">草稿</Option>
              <Option value="pending_review">待审核</Option>
              <Option value="approved">已通过</Option>
              <Option value="rejected">已拒绝</Option>
            </Select>
            <Search
              placeholder="搜索项目名称"
              allowClear
              style={{ width: 250 }}
              onSearch={keyword => setFilters(prev => ({ ...prev, keyword }))}
            />
          </Space>
          <Button icon={<ReloadOutlined />} onClick={loadReports}>
            刷新
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={reports}
          rowKey="reportId"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: total => `共 ${total} 条记录`,
            onChange: (page, pageSize) => setPagination(prev => ({ ...prev, current: page, pageSize })),
          }}
          scroll={{ x: 1200 }}
          onRow={(record) => ({
            onClick: () => setDetailDrawer({ visible: true, report: record }),
            style: { cursor: 'pointer' },
          })}
        />
      </Card>

      {/* 详情抽屉 */}
      <Drawer
        title="报告详情"
        placement="right"
        width={600}
        onClose={() => setDetailDrawer({ visible: false, report: null })}
        open={detailDrawer.visible}
      >
        {detailDrawer.report && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="报告ID">{detailDrawer.report.reportId}</Descriptions.Item>
            <Descriptions.Item label="项目名称">{detailDrawer.report.projectName}</Descriptions.Item>
            <Descriptions.Item label="版本">v{detailDrawer.report.version}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={statusConfig[detailDrawer.report.status]?.color}>
                {statusConfig[detailDrawer.report.status]?.text}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="问题总数">{detailDrawer.report.summary.totalProblems}</Descriptions.Item>
            <Descriptions.Item label="高优先级">{detailDrawer.report.summary.highSeverity}</Descriptions.Item>
            <Descriptions.Item label="中优先级">{detailDrawer.report.summary.mediumSeverity}</Descriptions.Item>
            <Descriptions.Item label="低优先级">{detailDrawer.report.summary.lowSeverity}</Descriptions.Item>
            <Descriptions.Item label="预估工时">{detailDrawer.report.summary.estimatedHours} 小时</Descriptions.Item>
            <Descriptions.Item label="生成时间">{detailDrawer.report.generatedAt}</Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  );
};

export default ReportManagement;
