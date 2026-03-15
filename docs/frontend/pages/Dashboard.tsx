/**
 * Dashboard 数据概览页面
 * 简化版本，使用 mock 数据
 */

import React from 'react';
import { Row, Col, Card, Statistic, Table, Tag, Progress } from 'antd';
import {
  ProjectOutlined,
  DatabaseOutlined,
  BugOutlined,
  ClockCircleOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import ReactECharts from 'echarts-for-react';
import styles from './index.module.less';

// Mock 数据
const mockStats = {
  projectCount: 12,
  entityCount: 15420,
  problemCount: 89,
  pendingReviewCount: 5,
  projectTrend: { type: 'up' as const, value: 15 },
  entityTrend: { type: 'up' as const, value: 23 },
  problemTrend: { type: 'down' as const, value: 8 },
};

const mockKnowledgeStats = {
  totalEntities: 15420,
  totalRelations: 8650,
  entityTypes: [
    { type: 'Class', count: 5200, percentage: 33.7 },
    { type: 'Method', count: 6800, percentage: 44.1 },
    { type: 'Field', count: 2100, percentage: 13.6 },
    { type: 'Interface', count: 1320, percentage: 8.6 },
  ],
};

const mockRefactorStats = {
  totalProblems: 89,
  bySeverity: [
    { severity: 'high' as const, count: 12, percentage: 13.5 },
    { severity: 'medium' as const, count: 35, percentage: 39.3 },
    { severity: 'low' as const, count: 42, percentage: 47.2 },
  ],
  estimatedHours: 156,
};

const mockRecentTasks = {
  extractTasks: [
    { taskId: '1', projectName: 'stock-service', status: 'completed' as const, progress: 100, entityCount: 5200, createdAt: '2026-03-14 10:30:00' },
    { taskId: '2', projectName: 'user-center', status: 'running' as const, progress: 65, entityCount: 3200, createdAt: '2026-03-15 09:00:00' },
  ],
  refactorTasks: [
    { taskId: '3', projectName: 'stock-service', status: 'completed' as const, problemCount: 45, highPriorityCount: 8, estimatedHours: 80, createdAt: '2026-03-14 14:00:00' },
  ],
};

const Dashboard: React.FC = () => {
  const navigate = useNavigate();

  // 统计卡片配置
  const statCards = [
    {
      title: '项目总数',
      value: mockStats.projectCount,
      icon: <ProjectOutlined style={{ fontSize: 32, color: '#1677ff' }} />,
      trend: mockStats.projectTrend,
      color: '#1677ff',
      route: '/extract-tasks', // 跳转到提取任务列表
    },
    {
      title: '知识实体',
      value: mockStats.entityCount,
      icon: <DatabaseOutlined style={{ fontSize: 32, color: '#52c41a' }} />,
      trend: mockStats.entityTrend,
      color: '#52c41a',
      route: '/extract-tasks', // 跳转到提取任务列表
    },
    {
      title: '重构问题',
      value: mockStats.problemCount,
      icon: <BugOutlined style={{ fontSize: 32, color: '#faad14' }} />,
      trend: mockStats.problemTrend,
      color: '#faad14',
      route: '/refactor-tasks', // 跳转到重构任务列表
    },
    {
      title: '待审核数',
      value: mockStats.pendingReviewCount,
      icon: <ClockCircleOutlined style={{ fontSize: 32, color: '#ff4d4f' }} />,
      trend: null,
      color: '#ff4d4f',
      route: '/review-workflow', // 跳转到审核工作流
    },
  ];

  // 知识图谱饼图配置
  const knowledgePieOption = {
    title: {
      text: '知识图谱统计',
      left: 'center',
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      left: 'left',
    },
    series: [
      {
        name: '实体类型',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold',
          },
        },
        labelLine: {
          show: false,
        },
        data: mockKnowledgeStats.entityTypes.map(item => ({
          value: item.count,
          name: item.type,
        })),
      },
    ],
  };

  // 问题分布柱状图
  const problemBarOption = {
    title: {
      text: '重构问题分布',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
    },
    xAxis: {
      type: 'category',
      data: ['高优先级', '中优先级', '低优先级'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        data: mockRefactorStats.bySeverity.map((item, index) => ({
          value: item.count,
          itemStyle: {
            color: index === 0 ? '#ff4d4f' : index === 1 ? '#faad14' : '#52c41a',
          },
        })),
        type: 'bar',
        barWidth: '40%',
      },
    ],
  };

  // 任务表格列配置
  const extractTaskColumns = [
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          completed: 'success',
          running: 'processing',
          failed: 'error',
          pending: 'default',
        };
        return <Tag color={colorMap[status]}>{status}</Tag>;
      },
    },
    {
      title: '进度',
      dataIndex: 'progress',
      key: 'progress',
      render: (progress: number) => (
        <Progress percent={progress} size="small" style={{ width: 100 }} />
      ),
    },
    {
      title: '实体数',
      dataIndex: 'entityCount',
      key: 'entityCount',
      render: (count: number) => count?.toLocaleString() || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
  ];

  return (
    <div className={styles.dashboard}>
      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        {statCards.map((card, index) => (
          <Col span={6} key={index}>
            <Card 
              hoverable 
              className={styles.statCard}
              onClick={() => navigate(card.route)}
              style={{ cursor: 'pointer' }}
            >
              <div className={styles.statCardContent}>
                <div className={styles.statIcon}>{card.icon}</div>
                <div className={styles.statValue}>
                  <Statistic
                    title={card.title}
                    value={card.value}
                    valueStyle={{ color: card.color }}
                    suffix={
                      card.trend && (
                        <span style={{ fontSize: 14 }}>
                          {card.trend.type === 'up' ? (
                            <ArrowUpOutlined style={{ color: '#52c41a' }} />
                          ) : (
                            <ArrowDownOutlined style={{ color: '#ff4d4f' }} />
                          )}
                          {card.trend.value}%
                        </span>
                      )
                    }
                  />
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* 图表区域 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={12}>
          <Card style={{ cursor: 'pointer' }}>
            <ReactECharts 
              option={knowledgePieOption} 
              style={{ height: 300 }}
              onEvents={{
                click: () => navigate('/extract-tasks'),
              }}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card style={{ cursor: 'pointer' }}>
            <ReactECharts 
              option={problemBarOption} 
              style={{ height: 300 }}
              onEvents={{
                click: () => navigate('/refactor-tasks'),
              }}
            />
          </Card>
        </Col>
      </Row>

      {/* 最近任务 */}
      <Row gutter={16}>
        <Col span={12}>
          <Card title="最近提取任务" className={styles.taskCard}>
            <Table
              dataSource={mockRecentTasks.extractTasks}
              columns={extractTaskColumns}
              rowKey="taskId"
              pagination={false}
              size="small"
              onRow={(record) => ({
                onClick: () => navigate(`/extract-tasks/${record.taskId}`),
                style: { cursor: 'pointer' },
              })}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="最近重构任务" className={styles.taskCard}>
            <Table
              dataSource={mockRecentTasks.refactorTasks}
              columns={[
                {
                  title: '项目名称',
                  dataIndex: 'projectName',
                  key: 'projectName',
                },
                {
                  title: '问题数',
                  dataIndex: 'problemCount',
                  key: 'problemCount',
                },
                {
                  title: '高优先级',
                  dataIndex: 'highPriorityCount',
                  key: 'highPriorityCount',
                  render: (count: number) => (
                    <Tag color="error">{count}</Tag>
                  ),
                },
                {
                  title: '预估工时',
                  dataIndex: 'estimatedHours',
                  key: 'estimatedHours',
                  render: (hours: number) => `${hours}h`,
                },
              ]}
              rowKey="taskId"
              pagination={false}
              size="small"
              onRow={(record) => ({
                onClick: () => navigate(`/refactor-tasks/${record.taskId}`),
                style: { cursor: 'pointer' },
              })}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
