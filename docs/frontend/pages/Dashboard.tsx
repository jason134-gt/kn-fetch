/**
 * Dashboard 数据概览页面
 */

import React, { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Table, Tag, Progress, Spin, message } from 'antd';
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
import { dashboardApi } from '../services/api';

// 类型定义
interface DashboardStats {
  projectCount: number;
  entityCount: number;
  problemCount: number;
  pendingReviewCount: number;
  projectTrend: { type: string; value: number };
  entityTrend: { type: string; value: number };
  problemTrend: { type: string; value: number };
}

interface KnowledgeStats {
  totalEntities: number;
  totalRelations: number;
  entityTypes: Array<{ type: string; count: number; percentage: number }>;
}

interface RefactorStats {
  totalProblems: number;
  bySeverity: Array<{ severity: string; count: number; percentage: number }>;
  estimatedHours: number;
}

interface RecentTasks {
  extractTasks: Array<{
    taskId: string;
    projectName: string;
    status: string;
    progress: number;
    entityCount: number;
    createdAt: string;
  }>;
  refactorTasks: Array<{
    taskId: string;
    projectName: string;
    status: string;
    problemCount: number;
    highPriorityCount: number;
    estimatedHours: number;
    createdAt: string;
  }>;
}

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  
  // 状态
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [knowledgeStats, setKnowledgeStats] = useState<KnowledgeStats | null>(null);
  const [refactorStats, setRefactorStats] = useState<RefactorStats | null>(null);
  const [recentTasks, setRecentTasks] = useState<RecentTasks | null>(null);

  // 加载数据
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [statsData, knowledgeData, refactorData, tasksData] = await Promise.all([
        dashboardApi.getStats(),
        dashboardApi.getKnowledgeStats(),
        dashboardApi.getRefactorStats(),
        dashboardApi.getRecentTasks(),
      ]);
      
      setStats(statsData);
      setKnowledgeStats(knowledgeData);
      setRefactorStats(refactorData);
      setRecentTasks(tasksData);
    } catch (error) {
      message.error('加载数据失败');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  // 统计卡片配置
  const getStatCards = () => {
    if (!stats) return [];
    
    return [
      {
        title: '项目总数',
        value: stats.projectCount,
        icon: <ProjectOutlined style={{ fontSize: 32, color: '#1677ff' }} />,
        trend: stats.projectTrend,
        color: '#1677ff',
        route: '/extract-tasks',
      },
      {
        title: '知识实体',
        value: stats.entityCount,
        icon: <DatabaseOutlined style={{ fontSize: 32, color: '#52c41a' }} />,
        trend: stats.entityTrend,
        color: '#52c41a',
        route: '/extract-tasks',
      },
      {
        title: '重构问题',
        value: stats.problemCount,
        icon: <BugOutlined style={{ fontSize: 32, color: '#faad14' }} />,
        trend: stats.problemTrend,
        color: '#faad14',
        route: '/refactor-tasks',
      },
      {
        title: '待审核数',
        value: stats.pendingReviewCount,
        icon: <ClockCircleOutlined style={{ fontSize: 32, color: '#ff4d4f' }} />,
        trend: null,
        color: '#ff4d4f',
        route: '/review-workflow',
      },
    ];
  };

  // 知识图谱饼图配置
  const getKnowledgePieOption = () => {
    if (!knowledgeStats) return {};
    
    return {
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
          data: knowledgeStats.entityTypes.map(item => ({
            value: item.count,
            name: item.type,
          })),
        },
      ],
    };
  };

  // 问题分布柱状图
  const getProblemBarOption = () => {
    if (!refactorStats) return {};
    
    return {
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
          data: refactorStats.bySeverity.map((item, index) => ({
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

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
        <Spin size="large" tip="加载数据中..." />
      </div>
    );
  }

  const statCards = getStatCards();

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
              option={getKnowledgePieOption()} 
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
              option={getProblemBarOption()} 
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
              dataSource={recentTasks?.extractTasks || []}
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
              dataSource={recentTasks?.refactorTasks || []}
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
