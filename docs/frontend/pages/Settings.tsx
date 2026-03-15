/**
 * 系统设置页面
 */

import React, { useState } from 'react';
import {
  Card, Tabs, Form, Input, Button, Switch, Select, Divider,
  message, Space, Alert
} from 'antd';
import {
  SettingOutlined, DatabaseOutlined, ApiOutlined,
  BellOutlined, SafetyOutlined
} from '@ant-design/icons';

const { TabPane } = Tabs;
const { Option } = Select;
const { TextArea } = Input;

const Settings: React.FC = () => {
  const [generalForm] = Form.useForm();
  const [llmForm] = Form.useForm();
  const [dbForm] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const saveGeneralSettings = async () => {
    setLoading(true);
    try {
      await generalForm.validateFields();
      message.success('设置已保存');
    } catch (error) {
      message.error('保存失败');
    } finally {
      setLoading(false);
    }
  };

  const saveLLMSettings = async () => {
    setLoading(true);
    try {
      await llmForm.validateFields();
      message.success('LLM 配置已保存');
    } catch (error) {
      message.error('保存失败');
    } finally {
      setLoading(false);
    }
  };

  const saveDBSettings = async () => {
    setLoading(true);
    try {
      await dbForm.validateFields();
      message.success('数据库配置已保存');
    } catch (error) {
      message.error('保存失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <Tabs defaultActiveKey="general">
        <TabPane
          tab={<span><SettingOutlined /> 通用设置</span>}
          key="general"
        >
          <Form
            form={generalForm}
            layout="vertical"
            style={{ maxWidth: 600 }}
            initialValues={{
              language: 'zh-CN',
              theme: 'light',
              autoRefresh: true,
              refreshInterval: 30,
            }}
          >
            <Form.Item name="language" label="语言">
              <Select style={{ width: 200 }}>
                <Option value="zh-CN">简体中文</Option>
                <Option value="en-US">English</Option>
              </Select>
            </Form.Item>
            <Form.Item name="theme" label="主题">
              <Select style={{ width: 200 }}>
                <Option value="light">浅色主题</Option>
                <Option value="dark">深色主题</Option>
              </Select>
            </Form.Item>
            <Form.Item name="autoRefresh" label="自动刷新" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="refreshInterval" label="刷新间隔 (秒)">
              <Input type="number" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item>
              <Button type="primary" onClick={saveGeneralSettings} loading={loading}>
                保存设置
              </Button>
            </Form.Item>
          </Form>
        </TabPane>

        <TabPane
          tab={<span><ApiOutlined /> LLM 配置</span>}
          key="llm"
        >
          <Alert
            message="LLM 配置说明"
            description="配置大语言模型 API 用于知识提取和分析"
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <Form
            form={llmForm}
            layout="vertical"
            style={{ maxWidth: 600 }}
            initialValues={{
              provider: 'openai',
              model: 'gpt-4',
              temperature: 0.7,
              maxTokens: 4096,
            }}
          >
            <Form.Item name="provider" label="提供商">
              <Select style={{ width: 200 }}>
                <Option value="openai">OpenAI</Option>
                <Option value="anthropic">Anthropic</Option>
                <Option value="azure">Azure OpenAI</Option>
                <Option value="local">本地模型</Option>
              </Select>
            </Form.Item>
            <Form.Item name="apiKey" label="API Key">
              <Input.Password placeholder="请输入 API Key" />
            </Form.Item>
            <Form.Item name="apiEndpoint" label="API Endpoint">
              <Input placeholder="https://api.openai.com/v1" />
            </Form.Item>
            <Form.Item name="model" label="模型">
              <Select style={{ width: 200 }}>
                <Option value="gpt-4">GPT-4</Option>
                <Option value="gpt-4-turbo">GPT-4 Turbo</Option>
                <Option value="gpt-3.5-turbo">GPT-3.5 Turbo</Option>
                <Option value="claude-3">Claude 3</Option>
              </Select>
            </Form.Item>
            <Form.Item name="temperature" label="Temperature">
              <Input type="number" step="0.1" min="0" max="2" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item name="maxTokens" label="最大 Token 数">
              <Input type="number" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" onClick={saveLLMSettings} loading={loading}>
                  保存配置
                </Button>
                <Button>测试连接</Button>
              </Space>
            </Form.Item>
          </Form>
        </TabPane>

        <TabPane
          tab={<span><DatabaseOutlined /> 数据库配置</span>}
          key="database"
        >
          <Form
            form={dbForm}
            layout="vertical"
            style={{ maxWidth: 600 }}
            initialValues={{
              dbType: 'neo4j',
              host: 'localhost',
              port: 7687,
              username: 'neo4j',
            }}
          >
            <Form.Item name="dbType" label="数据库类型">
              <Select style={{ width: 200 }}>
                <Option value="neo4j">Neo4j</Option>
                <Option value="postgresql">PostgreSQL</Option>
                <Option value="mysql">MySQL</Option>
              </Select>
            </Form.Item>
            <Form.Item name="host" label="主机地址">
              <Input placeholder="localhost" />
            </Form.Item>
            <Form.Item name="port" label="端口">
              <Input type="number" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item name="username" label="用户名">
              <Input placeholder="neo4j" />
            </Form.Item>
            <Form.Item name="password" label="密码">
              <Input.Password placeholder="请输入密码" />
            </Form.Item>
            <Form.Item name="database" label="数据库名">
              <Input placeholder="neo4j" />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" onClick={saveDBSettings} loading={loading}>
                  保存配置
                </Button>
                <Button>测试连接</Button>
              </Space>
            </Form.Item>
          </Form>
        </TabPane>

        <TabPane
          tab={<span><SafetyOutlined /> 安全设置</span>}
          key="security"
        >
          <Form layout="vertical" style={{ maxWidth: 600 }}>
            <Form.Item label="修改密码">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Input.Password placeholder="当前密码" />
                <Input.Password placeholder="新密码" />
                <Input.Password placeholder="确认新密码" />
                <Button type="primary">修改密码</Button>
              </Space>
            </Form.Item>
            <Divider />
            <Form.Item label="两步验证">
              <Switch /> 启用两步验证
            </Form.Item>
          </Form>
        </TabPane>

        <TabPane
          tab={<span><BellOutlined /> 通知设置</span>}
          key="notification"
        >
          <Form layout="vertical" style={{ maxWidth: 600 }}>
            <Form.Item label="任务完成通知">
              <Switch defaultChecked /> 启用邮件通知
            </Form.Item>
            <Form.Item label="错误告警通知">
              <Switch defaultChecked /> 启用邮件通知
            </Form.Item>
            <Form.Item label="日报/周报">
              <Select style={{ width: 200 }} defaultValue="none">
                <Option value="none">不发送</Option>
                <Option value="daily">每日发送</Option>
                <Option value="weekly">每周发送</Option>
              </Select>
            </Form.Item>
            <Form.Item label="通知邮箱">
              <Input placeholder="your@email.com" style={{ width: 300 }} />
            </Form.Item>
            <Form.Item>
              <Button type="primary">保存通知设置</Button>
            </Form.Item>
          </Form>
        </TabPane>
      </Tabs>
    </Card>
  );
};

export default Settings;
