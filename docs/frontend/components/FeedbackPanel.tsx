/**
 * 反馈面板组件实现示例
 * 文件路径: src/components/FeedbackPanel/index.tsx
 */

import React, { useState } from 'react';
import {
  Card,
  Radio,
  Input,
  Button,
  Space,
  Select,
  Divider,
  message,
  Spin,
} from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  SolutionOutlined,
  UpOutlined,
  DownOutlined,
} from '@ant-design/icons';
import type { FeedbackType, Severity, Solution } from '../api-types';

const { TextArea } = Input;
const { Option } = Select;

interface FeedbackPanelProps {
  problemId: string;
  sessionId: string;
  solutions: Solution[];
  recommendedSolution: number;
  onSubmit: (feedback: FeedbackInput) => Promise<void>;
  onCancel: () => void;
  loading?: boolean;
}

interface FeedbackInput {
  problemId: string;
  feedbackType: FeedbackType;
  content: string;
  suggestedPriority?: Severity;
  selectedSolution?: number;
  customSolution?: Partial<Solution>;
}

const FEEDBACK_TYPES = [
  { value: 'agree', label: '同意此问题', icon: <CheckCircleOutlined /> },
  { value: 'disagree', label: '不同意', icon: <CloseCircleOutlined /> },
  { value: 'modify', label: '需要修改', icon: <EditOutlined /> },
];

const PRIORITY_OPTIONS = [
  { value: 'high', label: '高', color: '#ff4d4f' },
  { value: 'medium', label: '中', color: '#faad14' },
  { value: 'low', label: '低', color: '#52c41a' },
];

const FeedbackPanel: React.FC<FeedbackPanelProps> = ({
  problemId,
  sessionId,
  solutions,
  recommendedSolution,
  onSubmit,
  onCancel,
  loading = false,
}) => {
  const [feedbackType, setFeedbackType] = useState<FeedbackType>('agree');
  const [content, setContent] = useState('');
  const [suggestedPriority, setSuggestedPriority] = useState<Severity>();
  const [selectedSolution, setSelectedSolution] = useState<number>(recommendedSolution);
  const [customSolutionDesc, setCustomSolutionDesc] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (feedbackType === 'disagree' && !content.trim()) {
      message.warning('请说明不同意的原因');
      return;
    }

    setSubmitting(true);
    try {
      await onSubmit({
        problemId,
        feedbackType,
        content: content.trim(),
        suggestedPriority,
        selectedSolution,
        customSolution: selectedSolution === -1 ? { description: customSolutionDesc } : undefined,
      });
      message.success('反馈提交成功');
    } catch (error: any) {
      message.error(error.message || '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const showPrioritySelect = ['modify', 'raise_priority', 'lower_priority'].includes(feedbackType);
  const showSolutionSelect = ['agree', 'accept_solution'].includes(feedbackType);

  return (
    <Card
      title="💬 用户反馈"
      size="small"
      extra={
        <Button type="link" onClick={onCancel}>
          取消
        </Button>
      }
    >
      <Spin spinning={loading}>
        {/* 反馈类型选择 */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>反馈类型:</div>
          <Radio.Group
            value={feedbackType}
            onChange={(e) => setFeedbackType(e.target.value)}
            style={{ width: '100%' }}
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              {FEEDBACK_TYPES.map((type) => (
                <Radio.Button
                  key={type.value}
                  value={type.value}
                  style={{
                    width: '100%',
                    height: 'auto',
                    padding: '12px 16px',
                    borderRadius: 6,
                    border: feedbackType === type.value ? '1px solid #1677ff' : '1px solid #d9d9d9',
                    background: feedbackType === type.value ? '#f0f5ff' : '#fff',
                  }}
                >
                  <Space>
                    {type.icon}
                    <span>{type.label}</span>
                  </Space>
                </Radio.Button>
              ))}
            </Space>
          </Radio.Group>
        </div>

        {/* 意见输入 */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>
            {feedbackType === 'disagree' ? '原因说明:' : '修改说明:'}
          </div>
          <TextArea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={
              feedbackType === 'disagree'
                ? '请说明为什么不同意此问题...'
                : '请输入您的反馈意见...'
            }
            rows={3}
            maxLength={500}
            showCount
          />
        </div>

        {/* 优先级调整 */}
        {showPrioritySelect && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ marginBottom: 8, fontWeight: 500 }}>建议优先级:</div>
            <Select
              value={suggestedPriority}
              onChange={setSuggestedPriority}
              style={{ width: '100%' }}
              placeholder="选择建议的优先级"
              allowClear
            >
              {PRIORITY_OPTIONS.map((opt) => (
                <Option key={opt.value} value={opt.value}>
                  <span style={{ color: opt.color }}>●</span> {opt.label}
                </Option>
              ))}
            </Select>
          </div>
        )}

        {/* 方案选择 */}
        {showSolutionSelect && solutions.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ marginBottom: 8, fontWeight: 500 }}>方案选择:</div>
            <Radio.Group
              value={selectedSolution}
              onChange={(e) => setSelectedSolution(e.target.value)}
              style={{ width: '100%' }}
            >
              <Space direction="vertical" style={{ width: '100%' }}>
                {solutions.map((solution, index) => (
                  <Radio.Button
                    key={solution.id || index}
                    value={index}
                    style={{
                      width: '100%',
                      height: 'auto',
                      padding: '12px 16px',
                      borderRadius: 6,
                      border: selectedSolution === index ? '1px solid #1677ff' : '1px solid #d9d9d9',
                      background: selectedSolution === index ? '#f0f5ff' : '#fff',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span>{solution.name}</span>
                      <span style={{ color: '#f39c12' }}>
                        {'⭐'.repeat(solution.recommendation)}
                      </span>
                    </div>
                  </Radio.Button>
                ))}
                <Radio.Button
                  value={-1}
                  style={{
                    width: '100%',
                    height: 'auto',
                    padding: '12px 16px',
                    borderRadius: 6,
                    border: selectedSolution === -1 ? '1px solid #1677ff' : '1px solid #d9d9d9',
                    background: selectedSolution === -1 ? '#f0f5ff' : '#fff',
                  }}
                >
                  <SolutionOutlined style={{ marginRight: 8 }} />
                  自定义方案...
                </Radio.Button>
              </Space>
            </Radio.Group>

            {/* 自定义方案输入 */}
            {selectedSolution === -1 && (
              <TextArea
                value={customSolutionDesc}
                onChange={(e) => setCustomSolutionDesc(e.target.value)}
                placeholder="请描述您的自定义方案..."
                rows={3}
                style={{ marginTop: 8 }}
              />
            )}
          </div>
        )}

        <Divider />

        {/* 提交按钮 */}
        <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
          <Button onClick={onCancel}>取消</Button>
          <Button
            type="primary"
            onClick={handleSubmit}
            loading={submitting}
            icon={<CheckCircleOutlined />}
          >
            提交反馈
          </Button>
        </Space>
      </Spin>
    </Card>
  );
};

export default FeedbackPanel;
