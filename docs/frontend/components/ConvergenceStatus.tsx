/**
 * 收敛状态组件
 * 文件路径: src/components/ConvergenceStatus/index.tsx
 */

import React from 'react';
import { Card, Progress, Space, Tag, Typography, Tooltip, Statistic, Row, Col, Alert } from 'antd';
import {
  CheckCircleOutlined,
  SyncOutlined,
  WarningOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import type { ConvergenceResult } from '../api-types';

const { Text, Title } = Typography;

interface ConvergenceStatusProps {
  result: ConvergenceResult;
  showDetails?: boolean;
}

const REASON_CONFIG: Record<string, { label: string; description: string; type: 'success' | 'warning' | 'info' }> = {
  low_change_rate: {
    label: '低变更率',
    description: '报告变更率已低于阈值，内容趋于稳定',
    type: 'success',
  },
  high_confirmation_rate: {
    label: '高确认率',
    description: '大部分问题已被用户确认或处理',
    type: 'success',
  },
  high_ignore_rate: {
    label: '高忽略率',
    description: '大部分问题已被用户忽略，剩余问题已处理',
    type: 'info',
  },
  insufficient_history: {
    label: '历史不足',
    description: '需要更多版本数据来判断收敛状态',
    type: 'warning',
  },
  still_evolving: {
    label: '仍在演进',
    description: '报告仍在持续变化中，建议继续迭代',
    type: 'warning',
  },
};

const ConvergenceStatus: React.FC<ConvergenceStatusProps> = ({
  result,
  showDetails = true,
}) => {
  const reasonConfig = REASON_CONFIG[result.reason] || REASON_CONFIG.still_evolving;

  // 计算收敛进度
  const getConvergenceProgress = (): number => {
    if (result.converged) return 100;
    
    const { changeRate = 1, confirmationRate = 0 } = result;
    
    // 综合评估：低变更率 + 高确认率 = 高收敛度
    const changeScore = Math.max(0, (1 - changeRate) * 50);
    const confirmScore = confirmationRate * 50;
    
    return Math.min(95, changeScore + confirmScore);
  };

  const progress = getConvergenceProgress();

  if (!showDetails) {
    // 简洁视图
    return (
      <Space>
        {result.converged ? (
          <Tag color="success" icon={<CheckCircleOutlined />}>
            已收敛
          </Tag>
        ) : (
          <Tag color="processing" icon={<SyncOutlined spin />}>
            迭代中
          </Tag>
        )}
        <Tooltip title={result.message}>
          <InfoCircleOutlined style={{ color: '#86909c' }} />
        </Tooltip>
      </Space>
    );
  }

  // 详细视图
  return (
    <Card size="small" title="收敛状态检测">
      {/* 整体状态 */}
      <Alert
        message={result.converged ? '已收敛' : '继续迭代'}
        description={result.message}
        type={result.converged ? 'success' : 'warning'}
        showIcon
        icon={result.converged ? <CheckCircleOutlined /> : <SyncOutlined spin />}
        style={{ marginBottom: 16 }}
      />

      {/* 收敛进度条 */}
      <div style={{ marginBottom: 16 }}>
        <Space style={{ marginBottom: 8 }}>
          <Text strong>收敛进度</Text>
          <Tooltip title="基于变更率和确认率计算">
            <InfoCircleOutlined style={{ color: '#86909c' }} />
          </Tooltip>
        </Space>
        <Progress
          percent={progress}
          status={result.converged ? 'success' : 'active'}
          strokeColor={{
            '0%': '#faad14',
            '100%': '#52c41a',
          }}
        />
      </div>

      {/* 详细指标 */}
      <Row gutter={16}>
        {result.changeRate !== undefined && (
          <Col span={8}>
            <Statistic
              title="变更率"
              value={(result.changeRate * 100).toFixed(1)}
              suffix="%"
              valueStyle={{
                color: result.changeRate < 0.1 ? '#52c41a' : result.changeRate < 0.3 ? '#faad14' : '#ff4d4f',
              }}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              阈值: &lt;10%
            </Text>
          </Col>
        )}
        
        {result.confirmationRate !== undefined && (
          <Col span={8}>
            <Statistic
              title="确认率"
              value={(result.confirmationRate * 100).toFixed(1)}
              suffix="%"
              valueStyle={{
                color: result.confirmationRate > 0.8 ? '#52c41a' : result.confirmationRate > 0.5 ? '#faad14' : '#ff4d4f',
              }}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              阈值: &gt;80%
            </Text>
          </Col>
        )}
        
        {result.ignoreRate !== undefined && (
          <Col span={8}>
            <Statistic
              title="忽略率"
              value={(result.ignoreRate * 100).toFixed(1)}
              suffix="%"
              valueStyle={{
                color: result.ignoreRate < 0.3 ? '#52c41a' : result.ignoreRate < 0.5 ? '#faad14' : '#ff4d4f',
              }}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              建议: &lt;30%
            </Text>
          </Col>
        )}
      </Row>

      {/* 收敛原因 */}
      <div style={{ marginTop: 16 }}>
        <Text type="secondary">收敛原因: </Text>
        <Tag color={reasonConfig.type}>{reasonConfig.label}</Tag>
        <Text type="secondary" style={{ marginLeft: 8 }}>
          {reasonConfig.description}
        </Text>
      </div>
    </Card>
  );
};

export default ConvergenceStatus;
