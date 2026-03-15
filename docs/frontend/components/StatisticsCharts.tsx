/**
 * 统计图表组件
 */

import React from 'react';
import ReactECharts from 'echarts-for-react';
import { Card, Row, Col, Statistic, Empty } from 'antd';
import {
  PieChartOutlined, BarChartOutlined, LineChartOutlined,
  AreaChartOutlined
} from '@ant-design/icons';

interface StatisticsChartsProps {
  data: {
    problemTypes: { type: string; count: number }[];
    severityDistribution: { severity: string; count: number }[];
    trend: { date: string; count: number }[];
    fileDistribution: { file: string; count: number }[];
  };
}

const StatisticsCharts: React.FC<StatisticsChartsProps> = ({ data }) => {
  // 问题类型饼图配置
  const problemTypeOption = {
    title: {
      text: '问题类型分布',
      left: 'center',
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold',
          },
        },
        labelLine: {
          show: false,
        },
        data: data.problemTypes.map(item => ({
          value: item.count,
          name: item.type,
        })),
      },
    ],
  };

  // 严重程度柱状图配置
  const severityOption = {
    title: {
      text: '严重程度分布',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    xAxis: {
      type: 'category',
      data: data.severityDistribution.map(item => item.severity),
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        data: data.severityDistribution.map((item, index) => ({
          value: item.count,
          itemStyle: {
            color: item.severity === '高' ? '#ff4d4f' :
                   item.severity === '中' ? '#faad14' : '#52c41a',
          },
        })),
        type: 'bar',
        barWidth: '60%',
      },
    ],
  };

  // 趋势折线图配置
  const trendOption = {
    title: {
      text: '问题发现趋势',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.trend.map(item => item.date),
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        name: '问题数',
        type: 'line',
        smooth: true,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
              { offset: 1, color: 'rgba(24, 144, 255, 0.05)' },
            ],
          },
        },
        lineStyle: { width: 3 },
        data: data.trend.map(item => item.count),
      },
    ],
  };

  // 文件问题分布
  const fileDistributionOption = {
    title: {
      text: '文件问题 TOP 10',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'value',
    },
    yAxis: {
      type: 'category',
      data: data.fileDistribution.slice(0, 10).map(item => item.file).reverse(),
      axisLabel: {
        width: 100,
        overflow: 'truncate',
      },
    },
    series: [
      {
        type: 'bar',
        data: data.fileDistribution.slice(0, 10).map(item => item.count).reverse(),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#1890ff' },
              { offset: 1, color: '#36cfc9' },
            ],
          },
        },
      },
    ],
  };

  if (!data || !data.problemTypes?.length) {
    return <Empty description="暂无数据" />;
  }

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Card>
            <ReactECharts
              option={problemTypeOption}
              style={{ height: 300 }}
              notMerge
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            <ReactECharts
              option={severityOption}
              style={{ height: 300 }}
              notMerge
            />
          </Card>
        </Col>
        <Col span={16}>
          <Card>
            <ReactECharts
              option={trendOption}
              style={{ height: 300 }}
              notMerge
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <ReactECharts
              option={fileDistributionOption}
              style={{ height: 300 }}
              notMerge
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default StatisticsCharts;
