/**
 * 问题过滤器组件
 */

import React from 'react';
import {
  Card, Form, Input, Select, Slider, Button, Space, Collapse,
  Tag, Row, Col, Switch, DatePicker, InputNumber
} from 'antd';
import {
  FilterOutlined, ReloadOutlined, CloseOutlined,
  DownOutlined, UpOutlined
} from '@ant-design/icons';

const { Option } = Select;
const { RangePicker } = DatePicker;
const { Panel } = Collapse;

interface ProblemFilterProps {
  filters: ProblemFilters;
  onChange: (filters: ProblemFilters) => void;
  onReset: () => void;
}

interface ProblemFilters {
  keyword?: string;
  type?: string[];
  severity?: string[];
  status?: string[];
  confidenceRange?: [number, number];
  filePattern?: string;
  dateRange?: [any, any];
  hasCode?: boolean;
  hasLinks?: boolean;
}

const ProblemFilter: React.FC<ProblemFilterProps> = ({
  filters,
  onChange,
  onReset,
}) => {
  const [form] = Form.useForm();
  const [expanded, setExpanded] = React.useState(false);

  // 问题类型选项
  const typeOptions = [
    { label: '设计问题', value: 'design' },
    { label: '代码异味', value: 'code_smell' },
    { label: '架构问题', value: 'architecture' },
    { label: '性能问题', value: 'performance' },
    { label: '安全问题', value: 'security' },
    { label: '依赖问题', value: 'dependency' },
  ];

  // 严重程度选项
  const severityOptions = [
    { label: '高', value: 'high', color: 'red' },
    { label: '中', value: 'medium', color: 'orange' },
    { label: '低', value: 'low', color: 'green' },
  ];

  // 状态选项
  const statusOptions = [
    { label: '待处理', value: 'pending' },
    { label: '已确认', value: 'confirmed' },
    { label: '已忽略', value: 'ignored' },
    { label: '已修复', value: 'fixed' },
  ];

  const handleValuesChange = (changedValues: any, allValues: ProblemFilters) => {
    onChange(allValues);
  };

  const handleReset = () => {
    form.resetFields();
    onReset();
  };

  return (
    <Card
      title={
        <Space>
          <FilterOutlined />
          <span>过滤器</span>
        </Space>
      }
      extra={
        <Space>
          <Button
            type="text"
            icon={expanded ? <UpOutlined /> : <DownOutlined />}
            onClick={() => setExpanded(!expanded)}
          >
            {expanded ? '收起' : '展开'}
          </Button>
          <Button
            type="text"
            icon={<ReloadOutlined />}
            onClick={handleReset}
          >
            重置
          </Button>
        </Space>
      }
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={filters}
        onValuesChange={handleValuesChange}
      >
        {/* 基础过滤 */}
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item name="keyword" label="关键词">
              <Input.Search placeholder="搜索问题标题或描述" allowClear />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="type" label="问题类型">
              <Select
                mode="multiple"
                placeholder="选择类型"
                allowClear
                optionLabelProp="label"
              >
                {typeOptions.map(opt => (
                  <Option key={opt.value} value={opt.value} label={opt.label}>
                    <Tag>{opt.label}</Tag>
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="severity" label="严重程度">
              <Select
                mode="multiple"
                placeholder="选择严重程度"
                allowClear
              >
                {severityOptions.map(opt => (
                  <Option key={opt.value} value={opt.value}>
                    <Tag color={opt.color}>{opt.label}</Tag>
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
        </Row>

        {/* 高级过滤 */}
        {expanded && (
          <>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item name="status" label="状态">
                  <Select
                    mode="multiple"
                    placeholder="选择状态"
                    allowClear
                  >
                    {statusOptions.map(opt => (
                      <Option key={opt.value} value={opt.value}>
                        {opt.label}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="filePattern" label="文件路径">
                  <Input placeholder="支持通配符，如: src/**/*.java" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="confidenceRange" label="置信度范围">
                  <Slider
                    range
                    min={0}
                    max={100}
                    marks={{ 0: '0%', 50: '50%', 100: '100%' }}
                  />
                </Form.Item>
              </Col>
            </Row>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item name="dateRange" label="发现时间">
                  <RangePicker style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="hasCode" label="包含代码" valuePropName="checked">
                  <Switch checkedChildren="是" unCheckedChildren="否" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="hasLinks" label="有关联" valuePropName="checked">
                  <Switch checkedChildren="是" unCheckedChildren="否" />
                </Form.Item>
              </Col>
            </Row>
          </>
        )}

        {/* 快速标签 */}
        <Form.Item label="快速筛选">
          <Space wrap>
            <Tag
              style={{ cursor: 'pointer' }}
              color={filters.severity?.includes('high') ? 'red' : undefined}
              onClick={() => {
                const newSeverity = filters.severity?.includes('high')
                  ? filters.severity.filter(s => s !== 'high')
                  : [...(filters.severity || []), 'high'];
                onChange({ ...filters, severity: newSeverity });
              }}
            >
              高优先级
            </Tag>
            <Tag
              style={{ cursor: 'pointer' }}
              color={filters.type?.includes('security') ? 'blue' : undefined}
              onClick={() => {
                const newType = filters.type?.includes('security')
                  ? filters.type.filter(t => t !== 'security')
                  : [...(filters.type || []), 'security'];
                onChange({ ...filters, type: newType });
              }}
            >
              安全问题
            </Tag>
            <Tag
              style={{ cursor: 'pointer' }}
              color={filters.status?.includes('pending') ? 'orange' : undefined}
              onClick={() => {
                const newStatus = filters.status?.includes('pending')
                  ? filters.status.filter(s => s !== 'pending')
                  : [...(filters.status || []), 'pending'];
                onChange({ ...filters, status: newStatus });
              }}
            >
              待处理
            </Tag>
            <Tag
              style={{ cursor: 'pointer' }}
              onClick={() => onChange({ ...filters, hasCode: !filters.hasCode })}
              color={filters.hasCode ? 'green' : undefined}
            >
              有代码
            </Tag>
          </Space>
        </Form.Item>

        {/* 当前筛选条件展示 */}
        {Object.keys(filters).some(key => filters[key as keyof ProblemFilters]) && (
          <Form.Item label="当前筛选">
            <Space wrap>
              {filters.keyword && (
                <Tag closable onClose={() => onChange({ ...filters, keyword: undefined })}>
                  关键词: {filters.keyword}
                </Tag>
              )}
              {filters.type?.map(t => (
                <Tag key={t} closable onClose={() => {
                  onChange({
                    ...filters,
                    type: filters.type?.filter(x => x !== t),
                  });
                }}>
                  类型: {typeOptions.find(o => o.value === t)?.label}
                </Tag>
              ))}
              {filters.severity?.map(s => (
                <Tag key={s} color={severityOptions.find(o => o.value === s)?.color} closable onClose={() => {
                  onChange({
                    ...filters,
                    severity: filters.severity?.filter(x => x !== s),
                  });
                }}>
                  严重: {severityOptions.find(o => o.value === s)?.label}
                </Tag>
              ))}
            </Space>
          </Form.Item>
        )}
      </Form>
    </Card>
  );
};

export default ProblemFilter;
