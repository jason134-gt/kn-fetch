/**
 * 知识提取任务详情页面
 */

import React, { useEffect, useState } from 'react';
import {
  Card, Tabs, Descriptions, Progress, Table, Tag, Row, Col,
  Statistic, Empty, Button, Space, Alert
} from 'antd';
import {
  DatabaseOutlined, FileTextOutlined, CodeOutlined,
  CheckCircleOutlined, ClockCircleOutlined
} from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { extractApi } from '../services/api';
import type { ExtractTaskDetail, KnowledgeEntity } from '../api-types';

const statusConfig: Record<string, { color: string; text: string }> = {
  pending: { color: 'default', text: '待处理' },
  running: { color: 'processing', text: '运行中' },
  completed: { color: 'success', text: '已完成' },
  failed: { color: 'error', text: '失败' },
  paused: { color: 'warning', text: '已暂停' },
};

const ExtractTaskDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [task, setTask] = useState<ExtractTaskDetail | null>(null);
  const [entities, setEntities] = useState<KnowledgeEntity[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      if (!id) return;
      setLoading(true);
      try {
        const taskData = await extractApi.getTaskDetail(id);
        setTask(taskData);
        const entitiesData = await extractApi.getEntities(id);
        setEntities(entitiesData.items || []);
      } catch (error) {
        console.error('加载数据失败:', error);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [id]);

  if (loading) {
    return <Card loading />;
  }

  if (!task) {
    return <Empty description="任务不存在" />;
  }

  const statusInfo = statusConfig[task.status] || { color: 'default', text: task.status };

  const entityColumns = [
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: string) => <Tag color="blue">{type}</Tag>,
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '文件路径',
      dataIndex: 'filePath',
      key: 'filePath',
      width: 300,
      ellipsis: true,
    },
    {
      title: '行号',
      key: 'lines',
      width: 100,
      render: (_: any, record: KnowledgeEntity) => `${record.lineStart}-${record.lineEnd}`,
    },
  ];

  return (
    <div>
      {/* 任务概览 */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={24}>
          <Col span={12}>
            <Descriptions column={1}>
              <Descriptions.Item label="任务ID">
                <code>{task.taskId}</code>
              </Descriptions.Item>
              <Descriptions.Item label="项目名称">{task.projectName}</Descriptions.Item>
              <Descriptions.Item label="项目路径">
                <code>{task.projectPath}</code>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="编程语言">
                {task.languages.map(lang => <Tag key={lang}>{lang}</Tag>)}
              </Descriptions.Item>
            </Descriptions>
          </Col>
          <Col span={12}>
            <Row gutter={16}>
              <Col span={6}>
                <Card hoverable style={{ cursor: 'pointer' }} onClick={() => navigate('/extract-tasks')}>
                  <Statistic
                    title="进度"
                    value={task.progress}
                    suffix="%"
                    prefix={<ClockCircleOutlined />}
                  />
                  <Progress percent={task.progress} size="small" showInfo={false} />
                </Card>
              </Col>
              <Col span={6}>
                <Card hoverable style={{ cursor: 'pointer' }} onClick={() => {}}>
                  <Statistic
                    title="实体数"
                    value={task.entityCount}
                    prefix={<DatabaseOutlined />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card hoverable style={{ cursor: 'pointer' }} onClick={() => {}}>
                  <Statistic
                    title="关系数"
                    value={task.relationCount}
                    prefix={<CodeOutlined />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card hoverable style={{ cursor: 'pointer' }} onClick={() => navigate('/refactor-tasks')}>
                  <Statistic
                    title="文档数"
                    value={task.documentCount}
                    prefix={<FileTextOutlined />}
                  />
                </Card>
              </Col>
            </Row>
          </Col>
        </Row>
      </Card>

      {/* 详细信息 */}
      <Tabs
        items={[
          {
            key: 'entities',
            label: `知识实体 (${entities.length})`,
            children: (
              <Card>
                <Table
                  columns={entityColumns}
                  dataSource={entities}
                  rowKey="id"
                  pagination={{ pageSize: 10 }}
                />
              </Card>
            ),
          },
          {
            key: 'config',
            label: '配置信息',
            children: (
              <Card>
                <Descriptions column={2} bordered>
                  <Descriptions.Item label="包含测试文件">
                    {task.config.includeTestFiles ? '是' : '否'}
                  </Descriptions.Item>
                  <Descriptions.Item label="最大文件大小">
                    {task.config.maxFileSize} KB
                  </Descriptions.Item>
                  <Descriptions.Item label="排除模式" span={2}>
                    {task.config.excludePatterns.join(', ') || '无'}
                  </Descriptions.Item>
                  <Descriptions.Item label="输出目录">
                    {task.config.outputDir}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            ),
          },
          {
            key: 'statistics',
            label: '统计信息',
            children: (
              <Card>
                <Descriptions column={2} bordered>
                  <Descriptions.Item label="已处理文件">
                    {task.statistics.filesProcessed} / {task.statistics.filesTotal}
                  </Descriptions.Item>
                  <Descriptions.Item label="实体类型分布">
                    {Object.entries(task.statistics.entitiesByType).map(([type, count]) => (
                      <Tag key={type}>{type}: {count}</Tag>
                    ))}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            ),
          },
          {
            key: 'errors',
            label: `错误信息 (${task.errors?.length || 0})`,
            children: (
              <Card>
                {task.errors && task.errors.length > 0 ? (
                  task.errors.map((err, idx) => (
                    <Alert
                      key={idx}
                      type="error"
                      message={err.file}
                      description={err.message}
                      style={{ marginBottom: 8 }}
                    />
                  ))
                ) : (
                  <Empty description="无错误信息" />
                )}
              </Card>
            ),
          },
        ]}
      />

      {/* 返回按钮 */}
      <div style={{ marginTop: 16 }}>
        <Button onClick={() => navigate('/extract-tasks')}>返回列表</Button>
      </div>
    </div>
  );
};

export default ExtractTaskDetail;
