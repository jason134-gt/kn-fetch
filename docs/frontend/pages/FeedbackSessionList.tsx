/**
 * 反馈会话列表页面
 */

import React, { useEffect, useState } from 'react';
import {
  Table, Card, Button, Input, Select, Space, Tag, Badge,
  Statistic, Row, Col, message
} from 'antd';
import {
  ReloadOutlined, EyeOutlined, CommentOutlined,
  CheckCircleOutlined, ClockCircleOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { mockApi } from '../services/mockData';
import type { FeedbackSession } from '../services/mockData';
import type { ColumnsType } from 'antd/es/table';

const { Search } = Input;
const { Option } = Select;

const statusConfig: Record<string, { color: string; text: string }> = {
  open: { color: 'processing', text: '进行中' },
  in_review: { color: 'warning', text: '审核中' },
  converged: { color: 'success', text: '已收敛' },
  closed: { color: 'default', text: '已关闭' },
};

const FeedbackSessionList: React.FC = () => {
  const navigate = useNavigate();
  const [sessions, setSessions] = useState<FeedbackSession[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [filters, setFilters] = useState({ status: 'all' });

  const loadSessions = async () => {
    setLoading(true);
    try {
      const result = await mockApi.getFeedbackSessions({
        status: filters.status === 'all' ? undefined : filters.status,
        page: pagination.current,
        pageSize: pagination.pageSize,
      });
      setSessions(result.items);
      setPagination(prev => ({ ...prev, total: result.total }));
    } catch (error) {
      message.error('加载会话列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSessions();
  }, [pagination.current, pagination.pageSize, filters.status]);

  const stats = {
    total: pagination.total,
    open: sessions.filter(s => s.status === 'open' || s.status === 'in_review').length,
    pendingReviews: sessions.reduce((sum, s) => sum + s.pendingReviews, 0),
  };

  const columns: ColumnsType<FeedbackSession> = [
    {
      title: '会话ID',
      dataIndex: 'sessionId',
      key: 'sessionId',
      width: 120,
    },
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      width: 180,
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
      title: '反馈总数',
      dataIndex: 'totalFeedbacks',
      key: 'totalFeedbacks',
      width: 100,
      render: (count: number) => (
        <Space>
          <CommentOutlined />
          <span>{count}</span>
        </Space>
      ),
    },
    {
      title: '待审核',
      dataIndex: 'pendingReviews',
      key: 'pendingReviews',
      width: 100,
      render: (count: number) => (
        <Badge count={count} showZero color={count > 0 ? '#ff4d4f' : '#52c41a'} />
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button
          type="text"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/feedback/${record.sessionId}`)}
        >
          查看
        </Button>
      ),
    },
  ];

  return (
    <div>
      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card hoverable style={{ cursor: 'pointer' }} onClick={() => {
            setFilters(prev => ({ ...prev, status: 'all' }));
            setPagination(prev => ({ ...prev, current: 1 }));
          }}>
            <Statistic
              title="会话总数"
              value={stats.total}
              prefix={<CommentOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable style={{ cursor: 'pointer' }} onClick={() => {
            setFilters(prev => ({ ...prev, status: 'open' }));
            setPagination(prev => ({ ...prev, current: 1 }));
          }}>
            <Statistic
              title="进行中"
              value={stats.open}
              valueStyle={{ color: '#1677ff' }}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable style={{ cursor: 'pointer' }} onClick={() => navigate('/review-workflow')}>
            <Statistic
              title="待审核反馈"
              value={stats.pendingReviews}
              valueStyle={{ color: stats.pendingReviews > 0 ? '#ff4d4f' : '#52c41a' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* 会话列表 */}
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Select
              value={filters.status}
              onChange={status => setFilters(prev => ({ ...prev, status }))}
              style={{ width: 120 }}
            >
              <Option value="all">全部状态</Option>
              <Option value="open">进行中</Option>
              <Option value="in_review">审核中</Option>
              <Option value="converged">已收敛</Option>
              <Option value="closed">已关闭</Option>
            </Select>
          </Space>
          <Button icon={<ReloadOutlined />} onClick={loadSessions}>
            刷新
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={sessions}
          rowKey="sessionId"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: total => `共 ${total} 条记录`,
            onChange: (page, pageSize) => setPagination(prev => ({ ...prev, current: page, pageSize })),
          }}
          onRow={(record) => ({
            onClick: () => navigate(`/feedback/${record.sessionId}`),
            style: { cursor: 'pointer' },
          })}
        />
      </Card>
    </div>
  );
};

export default FeedbackSessionList;
