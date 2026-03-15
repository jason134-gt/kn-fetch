/**
 * 知识提取任务列表页面
 */

import React, { useEffect, useState } from 'react';
import {
  Table, Card, Button, Input, Select, Space, Tag, Progress, Modal,
  Form, message, Popconfirm, Tooltip
} from 'antd';
import {
  PlusOutlined, ReloadOutlined, EyeOutlined, PauseCircleOutlined,
  PlayCircleOutlined, CloseCircleOutlined, DeleteOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { extractApi } from '../services/api';
import type { ColumnsType } from 'antd/es/table';

// 类型定义
interface ExtractTask {
  taskId: string;
  projectName: string;
  projectPath: string;
  languages: string[];
  status: 'pending' | 'running' | 'completed' | 'failed' | 'paused';
  progress: number;
  entityCount: number;
  relationCount: number;
  createdAt: string;
}

const { Search } = Input;
const { Option } = Select;

const statusConfig: Record<string, { color: string; text: string }> = {
  pending: { color: 'default', text: '待处理' },
  running: { color: 'processing', text: '运行中' },
  completed: { color: 'success', text: '已完成' },
  failed: { color: 'error', text: '失败' },
  paused: { color: 'warning', text: '已暂停' },
};

const ExtractTaskList: React.FC = () => {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState<ExtractTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [filters, setFilters] = useState({ status: 'all', keyword: '' });
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [createForm] = Form.useForm();

  const loadTasks = async () => {
    setLoading(true);
    try {
      const result = await extractApi.getTasks({
        status: filters.status === 'all' ? undefined : filters.status,
        keyword: filters.keyword || undefined,
        page: pagination.current,
        pageSize: pagination.pageSize,
      });
      setTasks(result.items);
      setPagination(prev => ({ ...prev, total: result.total }));
    } catch (error) {
      message.error('加载任务列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTasks();
  }, [pagination.current, pagination.pageSize, filters.status]);

  useEffect(() => {
    if (filters.keyword !== undefined) {
      loadTasks();
    }
  }, [filters.keyword]);

  const handleCreateTask = async (values: any) => {
    try {
      await extractApi.createTask(values);
      message.success('任务创建成功');
      setCreateModalVisible(false);
      createForm.resetFields();
      loadTasks();
    } catch (error) {
      message.error('创建任务失败');
    }
  };

  const handlePauseTask = async (taskId: string) => {
    try {
      await extractApi.pauseTask(taskId);
      message.success('任务已暂停');
      loadTasks();
    } catch (error) {
      message.error('暂停任务失败');
    }
  };

  const handleResumeTask = async (taskId: string) => {
    try {
      await extractApi.resumeTask(taskId);
      message.success('任务已恢复');
      loadTasks();
    } catch (error) {
      message.error('恢复任务失败');
    }
  };

  const handleCancelTask = async (taskId: string) => {
    try {
      await extractApi.cancelTask(taskId);
      message.success('任务已取消');
      loadTasks();
    } catch (error) {
      message.error('取消任务失败');
    }
  };

  const handleDeleteTask = async (taskId: string) => {
    try {
      await extractApi.deleteTask(taskId);
      message.success('任务已删除');
      loadTasks();
    } catch (error) {
      message.error('删除任务失败');
    }
  };

  const columns: ColumnsType<ExtractTask> = [
    {
      title: '项目名称',
      dataIndex: 'projectName',
      key: 'projectName',
      width: 180,
    },
    {
      title: '项目路径',
      dataIndex: 'projectPath',
      key: 'projectPath',
      width: 250,
      ellipsis: true,
    },
    {
      title: '语言',
      dataIndex: 'languages',
      key: 'languages',
      width: 150,
      render: (languages: string[]) => (
        <Space size={4}>
          {languages.map(lang => (
            <Tag key={lang}>{lang}</Tag>
          ))}
        </Space>
      ),
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
      title: '进度',
      dataIndex: 'progress',
      key: 'progress',
      width: 150,
      render: (progress: number) => (
        <Progress percent={progress} size="small" style={{ width: 120 }} />
      ),
    },
    {
      title: '实体数',
      dataIndex: 'entityCount',
      key: 'entityCount',
      width: 100,
      render: (count: number) => count?.toLocaleString() || '-',
    },
    {
      title: '关系数',
      dataIndex: 'relationCount',
      key: 'relationCount',
      width: 100,
      render: (count: number) => count?.toLocaleString() || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          <Tooltip title="查看详情">
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => navigate(`/extract/${record.taskId}`)}
            />
          </Tooltip>
          {record.status === 'running' && (
            <Tooltip title="暂停">
              <Button
                type="text"
                size="small"
                icon={<PauseCircleOutlined />}
                onClick={() => handlePauseTask(record.taskId)}
              />
            </Tooltip>
          )}
          {record.status === 'paused' && (
            <Tooltip title="恢复">
              <Button
                type="text"
                size="small"
                icon={<PlayCircleOutlined />}
                onClick={() => handleResumeTask(record.taskId)}
              />
            </Tooltip>
          )}
          {(record.status === 'running' || record.status === 'paused') && (
            <Popconfirm
              title="确定要取消此任务吗？"
              onConfirm={() => handleCancelTask(record.taskId)}
            >
              <Tooltip title="取消">
                <Button type="text" size="small" icon={<CloseCircleOutlined />} />
              </Tooltip>
            </Popconfirm>
          )}
          <Popconfirm
            title="确定要删除此任务吗？"
            onConfirm={() => handleDeleteTask(record.taskId)}
          >
            <Tooltip title="删除">
              <Button type="text" size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Select
              value={filters.status}
              onChange={status => setFilters(prev => ({ ...prev, status }))}
              style={{ width: 120 }}
            >
              <Option value="all">全部状态</Option>
              <Option value="pending">待处理</Option>
              <Option value="running">运行中</Option>
              <Option value="completed">已完成</Option>
              <Option value="failed">失败</Option>
              <Option value="paused">已暂停</Option>
            </Select>
            <Search
              placeholder="搜索项目名称"
              allowClear
              style={{ width: 250 }}
              onSearch={keyword => setFilters(prev => ({ ...prev, keyword }))}
            />
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={loadTasks}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalVisible(true)}>
              新建任务
            </Button>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={tasks}
          rowKey="taskId"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: total => `共 ${total} 条记录`,
            onChange: (page, pageSize) => setPagination(prev => ({ ...prev, current: page, pageSize })),
          }}
          scroll={{ x: 1400 }}
          onRow={(record) => ({
            onClick: () => navigate(`/extract-tasks/${record.taskId}`),
            style: { cursor: 'pointer' },
          })}
        />
      </Card>

      <Modal
        title="新建知识提取任务"
        open={createModalVisible}
        onCancel={() => setCreateModalVisible(false)}
        onOk={() => createForm.submit()}
        width={600}
        destroyOnClose
      >
        <Form
          form={createForm}
          layout="vertical"
          onFinish={handleCreateTask}
          initialValues={{ includeTestFiles: false, maxFileSize: 1024 }}
        >
          <Form.Item
            name="projectName"
            label="项目名称"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input placeholder="请输入项目名称" />
          </Form.Item>
          <Form.Item
            name="projectPath"
            label="项目路径"
            rules={[{ required: true, message: '请输入项目路径' }]}
          >
            <Input placeholder="例如：/projects/my-project" />
          </Form.Item>
          <Form.Item
            name="languages"
            label="编程语言"
            rules={[{ required: true, message: '请选择编程语言' }]}
          >
            <Select mode="multiple" placeholder="请选择编程语言">
              <Option value="Java">Java</Option>
              <Option value="Kotlin">Kotlin</Option>
              <Option value="Python">Python</Option>
              <Option value="TypeScript">TypeScript</Option>
              <Option value="JavaScript">JavaScript</Option>
              <Option value="Go">Go</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ExtractTaskList;
