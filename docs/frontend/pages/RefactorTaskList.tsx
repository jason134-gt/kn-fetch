/**
 * 重构任务列表页面
 */

import React, { useEffect, useState } from 'react';
import {
  Table, Card, Button, Input, Select, Space, Tag, Modal,
  Form, message, Popconfirm, Tooltip, Statistic, Row, Col
} from 'antd';
import {
  PlusOutlined, ReloadOutlined, EyeOutlined, DeleteOutlined,
  BugOutlined, ClockCircleOutlined, WarningOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { refactorApi } from '../services/api';
import type { ColumnsType } from 'antd/es/table';

// 类型定义
interface RefactorTask {
  taskId: string;
  projectName: string;
  status: 'pending' | 'analyzing' | 'completed' | 'failed';
  problemCount: number;
  highSeverityCount: number;
  estimatedHours: number;
  createdAt: string;
}

const { Search } = Input;
const { Option } = Select;

const statusConfig: Record<string, { color: string; text: string }> = {
  pending: { color: 'default', text: '待处理' },
  analyzing: { color: 'processing', text: '分析中' },
  completed: { color: 'success', text: '已完成' },
  failed: { color: 'error', text: '失败' },
};

const RefactorTaskList: React.FC = () => {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState<RefactorTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [filters, setFilters] = useState({ status: 'all', keyword: '' });
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [createForm] = Form.useForm();

  const loadTasks = async () => {
    setLoading(true);
    try {
      const result = await refactorApi.getTasks({
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
  }, [pagination.current, pagination.pageSize, filters.status, filters.keyword]);

  const handleCreateTask = async (values: any) => {
    try {
      await refactorApi.createTask(values);
      message.success('任务创建成功');
      setCreateModalVisible(false);
      createForm.resetFields();
      loadTasks();
    } catch (error) {
      message.error('创建任务失败');
    }
  };

  const handleDeleteTask = async (taskId: string) => {
    try {
      await refactorApi.deleteTask(taskId);
      message.success('任务已删除');
      loadTasks();
    } catch (error) {
      message.error('删除任务失败');
    }
  };

  const stats = {
    total: pagination.total,
    highSeverity: tasks.reduce((sum, t) => sum + t.highSeverityCount, 0),
    estimatedHours: tasks.reduce((sum, t) => sum + t.estimatedHours, 0),
  };

  const columns: ColumnsType<RefactorTask> = [
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
      title: '问题数',
      dataIndex: 'problemCount',
      key: 'problemCount',
      width: 100,
      render: (count: number) => (
        <Space>
          <BugOutlined style={{ color: '#faad14' }} />
          <span>{count}</span>
        </Space>
      ),
    },
    {
      title: '高优先级',
      dataIndex: 'highSeverityCount',
      key: 'highSeverityCount',
      width: 100,
      render: (count: number) => (
        <Tag color={count > 0 ? 'error' : 'default'}>
          <WarningOutlined /> {count}
        </Tag>
      ),
    },
    {
      title: '预估工时',
      dataIndex: 'estimatedHours',
      key: 'estimatedHours',
      width: 120,
      render: (hours: number) => (
        <Space>
          <ClockCircleOutlined />
          <span>{hours}h</span>
        </Space>
      ),
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
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          <Tooltip title="查看报告">
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => navigate(`/refactor/${record.taskId}`)}
            />
          </Tooltip>
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
      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card hoverable style={{ cursor: 'pointer' }} onClick={() => navigate('/refactor-tasks')}>
            <Statistic
              title="任务总数"
              value={stats.total}
              prefix={<BugOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable style={{ cursor: 'pointer' }} onClick={() => {
            setFilters(prev => ({ ...prev, status: 'all' }));
            setPagination(prev => ({ ...prev, current: 1 }));
          }}>
            <Statistic
              title="高优先级问题"
              value={stats.highSeverity}
              valueStyle={{ color: '#ff4d4f' }}
              prefix={<WarningOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable style={{ cursor: 'pointer' }} onClick={() => navigate('/refactor-report')}>
            <Statistic
              title="预估总工时"
              value={stats.estimatedHours}
              suffix="小时"
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* 任务列表 */}
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
              <Option value="analyzing">分析中</Option>
              <Option value="completed">已完成</Option>
              <Option value="failed">失败</Option>
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
              新建分析任务
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
          scroll={{ x: 1100 }}
          onRow={(record) => ({
            onClick: () => navigate(`/refactor-tasks/${record.taskId}`),
            style: { cursor: 'pointer' },
          })}
        />
      </Card>

      {/* 创建任务对话框 */}
      <Modal
        title="新建重构分析任务"
        open={createModalVisible}
        onCancel={() => setCreateModalVisible(false)}
        onOk={() => createForm.submit()}
        width={600}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreateTask}>
          <Form.Item
            name="projectName"
            label="项目名称"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input placeholder="请输入项目名称" />
          </Form.Item>
          <Form.Item
            name="knowledgePath"
            label="知识库路径"
            rules={[{ required: true, message: '请输入知识库路径' }]}
          >
            <Input placeholder="例如：/output/stock-service" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default RefactorTaskList;
