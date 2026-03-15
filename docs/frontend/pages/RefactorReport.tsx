/**
 * 重构报告详情页面
 */

import React, { useEffect, useState } from 'react';
import {
  Card, Tabs, Table, Tag, Descriptions, Statistic, Row, Col,
  Progress, Button, Space, Alert, Collapse, Empty
} from 'antd';
import {
  BugOutlined, WarningOutlined, CheckCircleOutlined,
  ClockCircleOutlined, ArrowLeftOutlined
} from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { mockApi } from '../services/mockData';
import type { RefactorReport, Problem } from '../services/mockData';

const severityConfig: Record<string, { color: string; icon: React.ReactNode }> = {
  high: { color: 'error', icon: <WarningOutlined /> },
  medium: { color: 'warning', icon: <CheckCircleOutlined /> },
  low: { color: 'success', icon: <CheckCircleOutlined /> },
};

const RefactorReport: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [report, setReport] = useState<RefactorReport | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadReport = async () => {
      if (!id) return;
      setLoading(true);
      try {
        const data = await mockApi.getRefactorReport(id);
        setReport(data);
      } catch (error) {
        console.error('加载报告失败:', error);
      } finally {
        setLoading(false);
      }
    };
    loadReport();
  }, [id]);

  if (loading) {
    return <Card loading />;
  }

  if (!report) {
    return <Empty description="报告不存在" />;
  }

  const problemColumns = [
    {
      title: '问题类型',
      dataIndex: 'problemType',
      key: 'problemType',
      width: 120,
      render: (type: string) => <Tag color="blue">{type}</Tag>,
    },
    {
      title: '严重程度',
      dataIndex: 'severity',
      key: 'severity',
      width: 100,
      render: (severity: string) => {
        const config = severityConfig[severity] || { color: 'default', icon: null };
        return (
          <Tag color={config.color} icon={config.icon}>
            {severity === 'high' ? '高' : severity === 'medium' ? '中' : '低'}
          </Tag>
        );
      },
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      width: 150,
    },
    {
      title: '实体名称',
      dataIndex: 'entityName',
      key: 'entityName',
      width: 150,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={status === 'confirmed' ? 'success' : status === 'ignored' ? 'default' : 'processing'}>
          {status === 'confirmed' ? '已确认' : status === 'ignored' ? '已忽略' : '待处理'}
        </Tag>
      ),
    },
  ];

  return (
    <div>
      {/* 报告概览 */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={24}>
          <Col span={8}>
            <Descriptions column={1}>
              <Descriptions.Item label="报告ID">{report.reportId}</Descriptions.Item>
              <Descriptions.Item label="项目名称">{report.projectName}</Descriptions.Item>
              <Descriptions.Item label="生成时间">{report.generatedAt}</Descriptions.Item>
              <Descriptions.Item label="版本">v{report.version}</Descriptions.Item>
            </Descriptions>
          </Col>
          <Col span={16}>
            <Row gutter={16}>
              <Col span={6}>
                <Card hoverable style={{ cursor: 'pointer' }} onClick={() => navigate('/reports')}>
                  <Statistic
                    title="问题总数"
                    value={report.summary.totalProblems}
                    prefix={<BugOutlined />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card hoverable style={{ cursor: 'pointer' }} onClick={() => navigate('/review-workflow')}>
                  <Statistic
                    title="高优先级"
                    value={report.summary.highSeverity}
                    valueStyle={{ color: '#ff4d4f' }}
                    prefix={<WarningOutlined />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card hoverable style={{ cursor: 'pointer' }} onClick={() => navigate('/refactor-tasks')}>
                  <Statistic
                    title="中优先级"
                    value={report.summary.mediumSeverity}
                    valueStyle={{ color: '#faad14' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card hoverable style={{ cursor: 'pointer' }} onClick={() => navigate('/refactor-tasks')}>
                  <Statistic
                    title="预估工时"
                    value={report.summary.estimatedHours}
                    suffix="小时"
                    prefix={<ClockCircleOutlined />}
                  />
                </Card>
              </Col>
            </Row>
          </Col>
        </Row>
      </Card>

      {/* 问题列表 */}
      <Card title="问题列表">
        <Table
          columns={problemColumns}
          dataSource={report.problems}
          rowKey="problemId"
          expandable={{
            expandedRowRender: (record: Problem) => (
              <div style={{ padding: 12 }}>
                <p><strong>文件路径：</strong>{record.filePath}</p>
                <p><strong>行号：</strong>{record.lineStart} - {record.lineEnd}</p>
                <p><strong>问题描述：</strong>{record.description}</p>
                <Collapse
                  items={record.solutions.map((sol, idx) => ({
                    key: idx,
                    label: `方案 ${idx + 1}: ${sol.name} ${idx === record.recommendedSolution ? '(推荐)' : ''}`,
                    children: (
                      <div>
                        <p>{sol.description}</p>
                        <p><strong>步骤：</strong></p>
                        <ol>
                          {sol.steps.map((step, i) => <li key={i}>{step}</li>)}
                        </ol>
                        <p><strong>预估工时：</strong>{sol.workloadHours} 小时</p>
                      </div>
                    ),
                  }))}
                />
              </div>
            ),
          }}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* 返回按钮 */}
      <div style={{ marginTop: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/refactor-tasks')}>
          返回列表
        </Button>
      </div>
    </div>
  );
};

export default RefactorReport;
