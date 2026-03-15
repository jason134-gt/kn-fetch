/**
 * 报告变更日志组件
 * 文件路径: src/components/ReportChangeLog/index.tsx
 */

import React from 'react';
import {
  Card,
  Table,
  Tag,
  Space,
  Typography,
  Timeline,
  Collapse,
  Badge,
  Empty,
  Button,
  Modal,
} from 'antd';
import {
  PlusOutlined,
  MinusOutlined,
  EditOutlined,
  CheckOutlined,
  StopOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import type { ChangeLog } from '../api-types';

const { Panel } = Collapse;
const { Text } = Typography;

interface ReportChangeLogProps {
  changeLog: ChangeLog;
  version: number;
  compact?: boolean;
  onViewProblem?: (problemId: string) => void;
}

const ReportChangeLog: React.FC<ReportChangeLogProps> = ({
  changeLog,
  version,
  compact = false,
  onViewProblem,
}) => {
  const [modalVisible, setModalVisible] = React.useState(false);

  const hasChanges = 
    changeLog.addedProblems > 0 ||
    changeLog.removedProblems > 0 ||
    changeLog.modifiedProblems > 0 ||
    changeLog.confirmedProblems > 0 ||
    changeLog.ignoredProblems > 0;

  if (!hasChanges) {
    return (
      <Card size="small" title={`V${version} 变更记录`}>
        <Empty description="本次无变更" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </Card>
    );
  }

  if (compact) {
    return (
      <Space wrap>
        {changeLog.addedProblems > 0 && (
          <Tag icon={<PlusOutlined />} color="green">
            +{changeLog.addedProblems} 新增
          </Tag>
        )}
        {changeLog.removedProblems > 0 && (
          <Tag icon={<MinusOutlined />} color="red">
            -{changeLog.removedProblems} 移除
          </Tag>
        )}
        {changeLog.modifiedProblems > 0 && (
          <Tag icon={<EditOutlined />} color="blue">
            {changeLog.modifiedProblems} 修改
          </Tag>
        )}
        {changeLog.confirmedProblems > 0 && (
          <Tag icon={<CheckOutlined />} color="green">
            {changeLog.confirmedProblems} 确认
          </Tag>
        )}
        {changeLog.ignoredProblems > 0 && (
          <Tag icon={<StopOutlined />} color="default">
            {changeLog.ignoredProblems} 忽略
          </Tag>
        )}
      </Space>
    );
  }

  // 详细变更日志表格
  const columns = [
    {
      title: '变更类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: string) => {
        const typeConfig: Record<string, { color: string; icon: React.ReactNode }> = {
          added: { color: 'green', icon: <PlusOutlined /> },
          removed: { color: 'red', icon: <MinusOutlined /> },
          modified: { color: 'blue', icon: <EditOutlined /> },
          confirmed: { color: 'green', icon: <CheckOutlined /> },
          ignored: { color: 'default', icon: <StopOutlined /> },
        };
        const config = typeConfig[type] || typeConfig.modified;
        return (
          <Tag color={config.color} icon={config.icon}>
            {type}
          </Tag>
        );
      },
    },
    {
      title: '问题ID',
      dataIndex: 'problemId',
      key: 'problemId',
      width: 100,
      render: (id: string) => <Text code>{id}</Text>,
    },
    {
      title: '变更详情',
      dataIndex: 'details',
      key: 'details',
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_: any, record: any) => (
        onViewProblem && (
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => onViewProblem(record.problemId)}
          >
            查看
          </Button>
        )
      ),
    },
  ];

  // 构建表格数据
  const buildTableData = () => {
    const data: any[] = [];
    const details = changeLog.details || {};

    details.added?.forEach((id) => {
      data.push({
        key: `added-${id}`,
        type: 'added',
        problemId: id,
        details: '新增问题',
      });
    });

    details.removed?.forEach((id) => {
      data.push({
        key: `removed-${id}`,
        type: 'removed',
        problemId: id,
        details: '从报告中移除',
      });
    });

    details.modified?.forEach((id) => {
      data.push({
        key: `modified-${id}`,
        type: 'modified',
        problemId: id,
        details: '问题内容已修改',
      });
    });

    details.confirmed?.forEach((id) => {
      data.push({
        key: `confirmed-${id}`,
        type: 'confirmed',
        problemId: id,
        details: '用户已确认此问题',
      });
    });

    details.ignored?.forEach((id) => {
      data.push({
        key: `ignored-${id}`,
        type: 'ignored',
        problemId: id,
        details: '用户选择忽略此问题',
      });
    });

    return data;
  };

  return (
    <Card
      size="small"
      title={`V${version} 变更记录`}
      extra={
        <Button type="link" onClick={() => setModalVisible(true)}>
          查看完整变更
        </Button>
      }
    >
      {/* 变更统计 */}
      <Space direction="vertical" style={{ width: '100%' }}>
        <Timeline>
          {changeLog.addedProblems > 0 && (
            <Timeline.Item color="green" dot={<PlusOutlined />}>
              <Text>新增问题: </Text>
              <Text strong>{changeLog.addedProblems}</Text>
              <Text> 个</Text>
            </Timeline.Item>
          )}
          {changeLog.removedProblems > 0 && (
            <Timeline.Item color="red" dot={<MinusOutlined />}>
              <Text>移除问题: </Text>
              <Text strong>{changeLog.removedProblems}</Text>
              <Text> 个</Text>
            </Timeline.Item>
          )}
          {changeLog.modifiedProblems > 0 && (
            <Timeline.Item color="blue" dot={<EditOutlined />}>
              <Text>修改问题: </Text>
              <Text strong>{changeLog.modifiedProblems}</Text>
              <Text> 个</Text>
            </Timeline.Item>
          )}
          {changeLog.confirmedProblems > 0 && (
            <Timeline.Item color="green" dot={<CheckOutlined />}>
              <Text>确认问题: </Text>
              <Text strong>{changeLog.confirmedProblems}</Text>
              <Text> 个</Text>
            </Timeline.Item>
          )}
          {changeLog.ignoredProblems > 0 && (
            <Timeline.Item color="gray" dot={<StopOutlined />}>
              <Text>忽略问题: </Text>
              <Text strong>{changeLog.ignoredProblems}</Text>
              <Text> 个</Text>
            </Timeline.Item>
          )}
        </Timeline>
      </Space>

      {/* 详细变更模态框 */}
      <Modal
        title={`V${version} 完整变更记录`}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={800}
      >
        <Table
          columns={columns}
          dataSource={buildTableData()}
          pagination={{ pageSize: 10 }}
          size="small"
        />
      </Modal>
    </Card>
  );
};

export default ReportChangeLog;
