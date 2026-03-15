/**
 * 审核工作流页面
 */

import React, { useEffect, useState } from 'react';
import {
  Card, Steps, Button, Space, Descriptions, Tag, Timeline,
  Statistic, Row, Col, Progress, Alert, message, Divider
} from 'antd';
import {
  CheckCircleOutlined, CloseCircleOutlined, SyncOutlined,
  FileTextOutlined, EditOutlined, ArrowLeftOutlined
} from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { mockApi } from '../services/mockData';
import type { FeedbackSession } from '../services/mockData';

const statusSteps = [
  { key: 'open', title: '开启', icon: <FileTextOutlined /> },
  { key: 'in_review', title: '审核中', icon: <EditOutlined /> },
  { key: 'converged', title: '已收敛', icon: <CheckCircleOutlined /> },
  { key: 'closed', title: '已关闭', icon: <CheckCircleOutlined /> },
];

const ReviewWorkflow: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const [session, setSession] = useState<FeedbackSession | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadSession = async () => {
      if (!sessionId) return;
      setLoading(true);
      try {
        const data = await mockApi.getFeedbackSession(sessionId);
        setSession(data);
      } catch (error) {
        console.error('加载会话失败:', error);
      } finally {
        setLoading(false);
      }
    };
    loadSession();
  }, [sessionId]);

  if (loading) {
    return <Card loading />;
  }

  if (!session) {
    return <Alert type="error" message="会话不存在" />;
  }

  const currentStepIndex = statusSteps.findIndex(s => s.key === session.status);

  const handleApprove = () => {
    message.success('审核已通过');
    navigate('/feedback');
  };

  const handleReject = () => {
    message.warning('审核已拒绝');
  };

  const handleRequestChanges = () => {
    message.info('已请求修改');
  };

  return (
    <div>
      {/* 进度步骤 */}
      <Card style={{ marginBottom: 16 }}>
        <Steps current={currentStepIndex} items={statusSteps} />
      </Card>

      {/* 会话概览 */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={24}>
          <Col span={12}>
            <Descriptions column={1}>
              <Descriptions.Item label="会话ID">{session.sessionId}</Descriptions.Item>
              <Descriptions.Item label="项目名称">{session.projectName}</Descriptions.Item>
              <Descriptions.Item label="关联报告">{session.reportId}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={currentStepIndex >= 2 ? 'success' : 'processing'}>
                  {statusSteps[currentStepIndex]?.title}
                </Tag>
              </Descriptions.Item>
            </Descriptions>
          </Col>
          <Col span={12}>
            <Row gutter={16}>
              <Col span={12}>
                <Statistic
                  title="反馈总数"
                  value={session.totalFeedbacks}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="待审核"
                  value={session.pendingReviews}
                  valueStyle={{ color: session.pendingReviews > 0 ? '#ff4d4f' : '#52c41a' }}
                />
              </Col>
            </Row>
            <Divider />
            <Progress
              percent={Math.round((session.totalFeedbacks - session.pendingReviews) / session.totalFeedbacks * 100) || 0}
              status={session.pendingReviews > 0 ? 'active' : 'success'}
            />
          </Col>
        </Row>
      </Card>

      {/* 审核时间线 */}
      <Card title="审核历史" style={{ marginBottom: 16 }}>
        <Timeline
          items={[
            {
              color: 'green',
              children: (
                <>
                  <p><strong>会话创建</strong></p>
                  <p>{session.createdAt}</p>
                </>
              ),
            },
            {
              color: 'blue',
              children: (
                <>
                  <p><strong>开始审核</strong></p>
                  <p>{session.updatedAt}</p>
                </>
              ),
            },
            {
              color: session.status === 'converged' ? 'green' : 'gray',
              children: (
                <>
                  <p><strong>收敛完成</strong></p>
                  <p>{session.status === 'converged' ? session.updatedAt : '待完成'}</p>
                </>
              ),
            },
          ]}
        />
      </Card>

      {/* 操作按钮 */}
      <Card>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/feedback')}>
            返回列表
          </Button>
          {session.status !== 'closed' && (
            <>
              <Button type="primary" icon={<CheckCircleOutlined />} onClick={handleApprove}>
                通过审核
              </Button>
              <Button danger icon={<CloseCircleOutlined />} onClick={handleReject}>
                拒绝
              </Button>
              <Button icon={<SyncOutlined />} onClick={handleRequestChanges}>
                请求修改
              </Button>
            </>
          )}
        </Space>
      </Card>
    </div>
  );
};

export default ReviewWorkflow;
