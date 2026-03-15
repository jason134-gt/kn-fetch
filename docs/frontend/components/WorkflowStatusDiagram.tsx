/**
 * 工作流状态图组件
 * 文件路径: src/components/WorkflowStatusDiagram/index.tsx
 */

import React from 'react';
import { Steps, Card, Tag, Space, Timeline, Typography, Tooltip } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  EditOutlined,
  CheckOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import type { ReviewStatus, ActionRecord } from '../api-types';

const { Text } = Typography;

interface WorkflowStatusDiagramProps {
  currentStatus: ReviewStatus;
  history: ActionRecord[];
  compact?: boolean;
}

// 状态配置
const STATUS_CONFIG: Record<
  ReviewStatus,
  { label: string; color: string; icon: React.ReactNode; description: string }
> = {
  draft: {
    label: '草稿',
    color: '#86909c',
    icon: <EditOutlined />,
    description: '报告已创建，待提交审核',
  },
  pending_review: {
    label: '待审核',
    color: '#1677ff',
    icon: <ClockCircleOutlined />,
    description: '等待审核人员开始审核',
  },
  in_review: {
    label: '审核中',
    color: '#1677ff',
    icon: <SyncOutlined spin />,
    description: '审核人员正在审核报告',
  },
  feedback_submitted: {
    label: '反馈已提交',
    color: '#faad14',
    icon: <FileTextOutlined />,
    description: '用户已提交反馈意见',
  },
  regenerating: {
    label: '重生成中',
    color: '#faad14',
    icon: <SyncOutlined spin />,
    description: '系统正在根据反馈重新生成报告',
  },
  approved: {
    label: '已批准',
    color: '#52c41a',
    icon: <CheckCircleOutlined />,
    description: '报告已审核通过',
  },
  rejected: {
    label: '已拒绝',
    color: '#ff4d4f',
    icon: <CloseCircleOutlined />,
    description: '报告被拒绝，需要修改',
  },
  finalized: {
    label: '已定稿',
    color: '#52c41a',
    icon: <CheckOutlined />,
    description: '报告已最终确认',
  },
};

// 状态流程定义
const WORKFLOW_STEPS: Array<{ status: ReviewStatus; nextStatuses: ReviewStatus[] }> = [
  { status: 'draft', nextStatuses: ['pending_review'] },
  { status: 'pending_review', nextStatuses: ['in_review', 'approved', 'rejected'] },
  { status: 'in_review', nextStatuses: ['feedback_submitted', 'approved', 'rejected'] },
  { status: 'feedback_submitted', nextStatuses: ['regenerating'] },
  { status: 'regenerating', nextStatuses: ['pending_review'] },
  { status: 'approved', nextStatuses: ['finalized'] },
  { status: 'rejected', nextStatuses: ['regenerating', 'pending_review'] },
  { status: 'finalized', nextStatuses: [] },
];

const WorkflowStatusDiagram: React.FC<WorkflowStatusDiagramProps> = ({
  currentStatus,
  history,
  compact = false,
}) => {
  const currentConfig = STATUS_CONFIG[currentStatus];

  if (compact) {
    // 紧凑视图 - 仅显示当前状态
    return (
      <Space>
        <Tag color={currentConfig.color} icon={currentConfig.icon}>
          {currentConfig.label}
        </Tag>
        <Text type="secondary">{currentConfig.description}</Text>
      </Space>
    );
  }

  // 完整视图 - 显示状态流程和历史记录
  return (
    <Card size="small" title="工作流状态">
      {/* 当前状态 */}
      <div style={{ marginBottom: 24 }}>
        <Text type="secondary">当前状态: </Text>
        <Tag color={currentConfig.color} icon={currentConfig.icon} style={{ fontSize: 14, padding: '4px 12px' }}>
          {currentConfig.label}
        </Tag>
        <Text type="secondary" style={{ marginLeft: 8 }}>
          {currentConfig.description}
        </Text>
      </div>

      {/* 状态流程图 */}
      <Steps
        current={getStepIndex(currentStatus)}
        status={currentStatus === 'rejected' ? 'error' : 'process'}
        size="small"
        items={[
          {
            title: '草稿',
            status: getStepStatus('draft', currentStatus),
            icon: getStepIcon('draft', currentStatus),
          },
          {
            title: '审核',
            status: getStepStatus('in_review', currentStatus),
            icon: getStepIcon('in_review', currentStatus),
            description: '待审核 → 审核中',
          },
          {
            title: '反馈',
            status: getStepStatus('feedback_submitted', currentStatus),
            icon: getStepIcon('feedback_submitted', currentStatus),
            description: '提交反馈 → 重生成',
          },
          {
            title: '批准',
            status: getStepStatus('approved', currentStatus),
            icon: getStepIcon('approved', currentStatus),
          },
          {
            title: '定稿',
            status: getStepStatus('finalized', currentStatus),
            icon: getStepIcon('finalized', currentStatus),
          },
        ]}
        style={{ marginBottom: 24 }}
      />

      {/* 操作历史 */}
      {history.length > 0 && (
        <div>
          <Text strong style={{ display: 'block', marginBottom: 12 }}>
            操作历史:
          </Text>
          <Timeline
            items={history.slice(-10).reverse().map((record) => ({
              color: getActionColor(record.action),
              children: (
                <div>
                  <Space>
                    <Text strong>{formatAction(record.action)}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {new Date(record.timestamp).toLocaleString()}
                    </Text>
                  </Space>
                  <div style={{ marginTop: 4 }}>
                    <Text type="secondary">
                      {STATUS_CONFIG[record.fromStatus as ReviewStatus]?.label || record.fromStatus}
                      {' → '}
                      {STATUS_CONFIG[record.toStatus as ReviewStatus]?.label || record.toStatus}
                    </Text>
                  </div>
                  {record.comments && (
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {record.comments}
                    </Text>
                  )}
                  {record.userId && (
                    <Tag style={{ marginTop: 4 }}>{record.userId}</Tag>
                  )}
                </div>
              ),
            }))}
          />
        </div>
      )}
    </Card>
  );
};

// 辅助函数
function getStepIndex(status: ReviewStatus): number {
  const stepOrder: ReviewStatus[] = [
    'draft',
    'pending_review',
    'in_review',
    'feedback_submitted',
    'approved',
    'finalized',
  ];
  
  // 特殊状态处理
  if (status === 'regenerating') return 2;
  if (status === 'rejected') return 3;
  
  const index = stepOrder.indexOf(status);
  return index >= 0 ? index : 0;
}

function getStepStatus(stepStatus: ReviewStatus, currentStatus: ReviewStatus): 'wait' | 'process' | 'finish' | 'error' {
  const currentIndex = getStepIndex(currentStatus);
  const stepIndex = getStepIndex(stepStatus);
  
  if (currentStatus === 'rejected' && stepStatus === 'approved') {
    return 'error';
  }
  
  if (stepIndex < currentIndex) return 'finish';
  if (stepIndex === currentIndex) return 'process';
  return 'wait';
}

function getStepIcon(stepStatus: ReviewStatus, currentStatus: ReviewStatus): React.ReactNode {
  const config = STATUS_CONFIG[stepStatus];
  const stepStatusValue = getStepStatus(stepStatus, currentStatus);
  
  if (stepStatusValue === 'finish') {
    return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
  }
  if (stepStatusValue === 'error') {
    return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;
  }
  return config.icon;
}

function getActionColor(action: string): string {
  const actionColors: Record<string, string> = {
    submit: 'blue',
    start_review: 'blue',
    submit_feedback: 'orange',
    request_regenerate: 'orange',
    approve: 'green',
    reject: 'red',
    finalize: 'green',
  };
  return actionColors[action] || 'gray';
}

function formatAction(action: string): string {
  const actionNames: Record<string, string> = {
    submit: '提交审核',
    start_review: '开始审核',
    submit_feedback: '提交反馈',
    request_regenerate: '请求重生成',
    approve: '批准',
    reject: '拒绝',
    finalize: '定稿',
    rollback: '回滚',
  };
  return actionNames[action] || action;
}

export default WorkflowStatusDiagram;
