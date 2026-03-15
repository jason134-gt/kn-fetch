/**
 * 问题详情卡片组件
 * 文件路径: src/components/ProblemDetailCard/index.tsx
 */

import React, { useState } from 'react';
import {
  Card,
  Tabs,
  Table,
  Tag,
  Button,
  Space,
  Collapse,
  Typography,
  Tooltip,
  Badge,
  Descriptions,
  Empty,
} from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  CodeOutlined,
  FileTextOutlined,
  WarningOutlined,
  SolutionOutlined,
  CommentOutlined,
} from '@ant-design/icons';
import type { ProblemDetail, Severity, RiskLevel } from '../api-types';

const { Panel } = Collapse;
const { Text, Paragraph } = Typography;
const { TabPane } = Tabs;

interface ProblemDetailCardProps {
  problem: ProblemDetail;
  showActions?: boolean;
  showFeedbackStatus?: boolean;
  onFeedback?: () => void;
  onViewCode?: () => void;
}

const SEVERITY_CONFIG: Record<Severity, { color: string; text: string; icon: React.ReactNode }> = {
  high: { color: '#ff4d4f', text: '高', icon: <ExclamationCircleOutlined /> },
  medium: { color: '#faad14', text: '中', icon: <WarningOutlined /> },
  low: { color: '#52c41a', text: '低', icon: <CheckCircleOutlined /> },
};

const RISK_CONFIG: Record<RiskLevel, { color: string; text: string }> = {
  high: { color: '#ff4d4f', text: '高风险' },
  medium: { color: '#faad14', text: '中等风险' },
  low: { color: '#52c41a', text: '低风险' },
};

const ProblemDetailCard: React.FC<ProblemDetailCardProps> = ({
  problem,
  showActions = true,
  showFeedbackStatus = true,
  onFeedback,
  onViewCode,
}) => {
  const [activeTab, setActiveTab] = useState('description');

  const severityConfig = SEVERITY_CONFIG[problem.severity];
  const riskConfig = RISK_CONFIG[problem.riskLevel];

  // 状态徽章
  const getStatusBadge = () => {
    if (problem.ignored) {
      return <Badge status="default" text="已忽略" />;
    }
    if (problem.userConfirmed) {
      return <Badge status="success" text="已确认" />;
    }
    return <Badge status="processing" text="待处理" />;
  };

  // 风险表格列定义
  const riskColumns = [
    {
      title: '风险类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: '可能性',
      dataIndex: 'probability',
      key: 'probability',
      width: 80,
      render: (prob: string) => (
        <Tag color={prob === 'high' ? '#ff4d4f' : prob === 'medium' ? '#faad14' : '#52c41a'}>
          {prob === 'high' ? '高' : prob === 'medium' ? '中' : '低'}
        </Tag>
      ),
    },
    {
      title: '影响',
      dataIndex: 'impact',
      key: 'impact',
      width: 80,
      render: (impact: string) => (
        <Tag color={impact === 'high' ? '#ff4d4f' : impact === 'medium' ? '#faad14' : '#52c41a'}>
          {impact === 'high' ? '高' : impact === 'medium' ? '中' : '低'}
        </Tag>
      ),
    },
    {
      title: '缓解措施',
      dataIndex: 'mitigation',
      key: 'mitigation',
      width: 200,
      ellipsis: true,
    },
  ];

  return (
    <Card
      title={
        <Space>
          <Tag color={severityConfig.color}>
            {severityConfig.icon} {severityConfig.text}
          </Tag>
          <Text strong>{problem.problemId}: {problem.entityName}</Text>
        </Space>
      }
      extra={
        showActions && (
          <Space>
            {onViewCode && (
              <Button icon={<CodeOutlined />} onClick={onViewCode}>
                查看代码
              </Button>
            )}
            {onFeedback && (
              <Button type="primary" icon={<CommentOutlined />} onClick={onFeedback}>
                提交反馈
              </Button>
            )}
          </Space>
        )
      }
    >
      {/* 基础信息 */}
      <Descriptions size="small" column={4} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="类型">{problem.problemType}</Descriptions.Item>
        <Descriptions.Item label="文件">{problem.filePath}</Descriptions.Item>
        <Descriptions.Item label="位置">
          {problem.lineStart && problem.lineEnd
            ? `L${problem.lineStart}-${problem.lineEnd}`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="状态">
          {showFeedbackStatus ? getStatusBadge() : '-'}
        </Descriptions.Item>
      </Descriptions>

      {/* 用户反馈提示 */}
      {showFeedbackStatus && problem.userComment && (
        <Card size="small" style={{ marginBottom: 16, background: '#f6ffed' }}>
          <Space>
            <CommentOutlined style={{ color: '#52c41a' }} />
            <Text type="secondary">用户反馈:</Text>
            <Text>{problem.userComment}</Text>
          </Space>
        </Card>
      )}

      {/* 四要素详情 */}
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        {/* 一、是什么 */}
        <TabPane
          tab={
            <span>
              <FileTextOutlined />
              是什么
            </span>
          }
          key="description"
        >
          <Space direction="vertical" style={{ width: '100%' }}>
            {/* 问题描述 */}
            <Card size="small" title="问题描述">
              <Paragraph>{problem.description}</Paragraph>
            </Card>

            {/* 当前状态 */}
            <Card size="small" title="当前状态">
              <Descriptions size="small" column={2}>
                {Object.entries(problem.currentState).map(([key, value]) => (
                  <Descriptions.Item key={key} label={key}>
                    {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                  </Descriptions.Item>
                ))}
              </Descriptions>
            </Card>

            {/* 代码上下文 */}
            {problem.codeContext && (
              <Card size="small" title="代码上下文">
                <pre
                  style={{
                    background: '#f5f5f5',
                    padding: 12,
                    borderRadius: 4,
                    overflow: 'auto',
                    maxHeight: 200,
                    fontSize: 12,
                  }}
                >
                  {problem.codeContext}
                </pre>
              </Card>
            )}
          </Space>
        </TabPane>

        {/* 二、怎么修复 */}
        <TabPane
          tab={
            <span>
              <SolutionOutlined />
              怎么修复
            </span>
          }
          key="fix"
        >
          <Collapse defaultActiveKey={problem.fixSteps.map((_, i) => i.toString())}>
            {problem.fixSteps.map((step, index) => (
              <Panel
                header={
                  <Space>
                    <Tag color="#1677ff">{index + 1}</Tag>
                    <Text strong>{step.title}</Text>
                    {step.estimatedTime && (
                      <Text type="secondary">({step.estimatedTime}min)</Text>
                    )}
                  </Space>
                }
                key={index.toString()}
              >
                <Paragraph>{step.description}</Paragraph>
                {step.codeExample && (
                  <pre
                    style={{
                      background: '#f5f5f5',
                      padding: 12,
                      borderRadius: 4,
                      overflow: 'auto',
                      fontSize: 12,
                    }}
                  >
                    {step.codeExample}
                  </pre>
                )}
              </Panel>
            ))}
          </Collapse>
        </TabPane>

        {/* 三、有什么风险 */}
        <TabPane
          tab={
            <span>
              <WarningOutlined />
              有什么风险
            </span>
          }
          key="risk"
        >
          <Space direction="vertical" style={{ width: '100%' }}>
            <div style={{ marginBottom: 16 }}>
              <Text type="secondary">整体风险等级: </Text>
              <Tag color={riskConfig.color}>{riskConfig.text}</Tag>
            </div>

            <Table
              columns={riskColumns}
              dataSource={problem.risks}
              rowKey={(record) => record.type}
              size="small"
              pagination={false}
            />

            {problem.rollbackPlan && (
              <Card size="small" title="回滚方案">
                <Text>{problem.rollbackPlan}</Text>
              </Card>
            )}
          </Space>
        </TabPane>

        {/* 四、有什么方案 */}
        <TabPane
          tab={
            <span>
              <CheckCircleOutlined />
              有什么方案
            </span>
          }
          key="solutions"
        >
          {problem.solutions.length > 0 ? (
            <Space direction="vertical" style={{ width: '100%' }}>
              {problem.solutions.map((solution, index) => (
                <Card
                  key={solution.id || index}
                  size="small"
                  title={
                    <Space>
                      <Text strong>{solution.name}</Text>
                      {index === problem.recommendedSolution && (
                        <Tag color="#1677ff">⭐ 推荐</Tag>
                      )}
                    </Space>
                  }
                  extra={
                    <Space>
                      <Tooltip title="工作量">
                        <Tag>{solution.workloadHours}h</Tag>
                      </Tooltip>
                      <Tooltip title="推荐度">
                        <Text style={{ color: '#f39c12' }}>
                          {'⭐'.repeat(solution.recommendation)}
                        </Text>
                      </Tooltip>
                    </Space>
                  }
                  style={{
                    borderLeft:
                      index === problem.recommendedSolution
                        ? '3px solid #1677ff'
                        : undefined,
                  }}
                >
                  <Paragraph>{solution.description}</Paragraph>

                  {solution.steps && solution.steps.length > 0 && (
                    <div style={{ marginBottom: 12 }}>
                      <Text type="secondary">步骤:</Text>
                      <ol style={{ marginLeft: 16, marginTop: 8 }}>
                        {solution.steps.map((step, i) => (
                          <li key={i}>{step}</li>
                        ))}
                      </ol>
                    </div>
                  )}

                  {(solution.pros || solution.cons) && (
                    <div style={{ display: 'flex', gap: 24 }}>
                      {solution.pros && solution.pros.length > 0 && (
                        <div>
                          <Text type="success">优点:</Text>
                          <ul style={{ marginLeft: 16, marginTop: 4 }}>
                            {solution.pros.map((pro, i) => (
                              <li key={i} style={{ color: '#52c41a' }}>
                                {pro}
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                      {solution.cons && solution.cons.length > 0 && (
                        <div>
                          <Text type="danger">缺点:</Text>
                          <ul style={{ marginLeft: 16, marginTop: 4 }}>
                            {solution.cons.map((con, i) => (
                              <li key={i} style={{ color: '#ff4d4f' }}>
                                {con}
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  )}
                </Card>
              ))}
            </Space>
          ) : (
            <Empty description="暂无解决方案" />
          )}
        </TabPane>
      </Tabs>
    </Card>
  );
};

export default ProblemDetailCard;
