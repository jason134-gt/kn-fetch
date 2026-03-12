# 股票舆情分析系统

## 项目简介

这是一个基于AI的股票舆情分析系统，通过采集实时财经资讯，经过多维度专家评估，生成短线交易建议。

## 系统架构

```
stock-sentiment-analyzer/
├── config/           # 配置文件
│   └── config.yaml
├── src/
│   ├── collectors/   # 资讯采集模块
│   ├── experts/      # 专家评估模块
│   ├── aggregator/   # 汇总模块
│   ├── trader/       # 短线决策模块
│   ├── reporter/     # 报告生成模块
│   ├── scheduler/    # 定时调度模块
│   └── utils/        # 工具模块
├── data/             # 数据存储
├── prompts/          # 提示词
├── logs/             # 日志文件
├── requirements.txt  # 依赖包
└── README.md
```

## 核心功能

### 1. 实时资讯采集
- 7x24小时不间断采集
- 多数据源支持（官方渠道、权威媒体、核心平台）
- 智能去重和过滤

### 2. 专家圆桌评估
- 5位专家独立评估
- 多维度分析（宏观、行业、技术、资金、风险）
- 加权汇总计算

### 3. 短线高手决策
- 智能选股
- 操作建议（买入/卖出/观望）
- 目标价和止损价计算

### 4. 报告生成
- Markdown格式报告
- 通达信格式输出
- 结构化数据展示

### 5. 多数据源行情数据
- 支持通达信本地行情数据（日、周、月、分钟级别）
- 支持网络API实时行情（东方财富）
- 智能数据源降级策略（API失败时自动使用本地数据）
- 概念板块数据支持
- 资金流向数据支持（主力资金、北向资金、板块资金流向）

## 安装步骤

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置系统

编辑 `config/config.yaml`，配置：
- 数据源
- 专家权重
- 股票池
- 关注板块

### 3. 运行系统

```bash
# 单次运行
python src/main.py --mode once

# 定时运行
python src/main.py --mode scheduled

# 交互模式
python src/main.py --mode interactive
```

## 配置说明

### 数据源配置

```yaml
data_sources:
  official:
    - name: "上交所公告"
      url: "http://www.sse.com.cn"
      enabled: true
  media:
    - name: "财新网"
      url: "https://www.caixin.com"
      enabled: true
  platforms:
    - name: "东方财富网"
      url: "https://www.eastmoney.com"
      enabled: true
```

### 通达信数据配置

```yaml
tdx:
  data_path: "C:/zd_zsone"  # 通达信安装目录
  enabled: true
  default_market: "SH"      # 默认市场，SH:上海，SZ:深圳，BJ:北京
  cache_enabled: true
  cache_ttl: 300            # 缓存时间（秒）
```

### 专家权重配置

```yaml
expert_weights:
  macro_economy: 0.25      # 宏观经济专家
  industry_research: 0.25  # 行业研究员
  technical_analysis: 0.20 # 技术面分析专家
  capital_flow: 0.20       # 资金面分析专家
  risk_control: 0.10       # 风险管控专家
```

### 股票池配置

```yaml
stock_pool:
  - code: "600036"
    name: "招商银行"
    sector: "银行"
  - code: "000858"
    name: "五粮液"
    sector: "白酒"
```

## 输出示例

### 报告格式

```
# 实时股票财经舆情分析报告

报告生成时间：2024-01-15 14:30:00
采集资讯总数：10条  有效分析资讯：8条

## 一、核心资讯汇总

### 资讯1：央行降准0.5个百分点
- 影响等级：强影响
- 影响方向：利好
- 影响时长：中期

## 二、概念/板块影响分析

| 影响概念 | 影响等级 | 影响方向 | 短线异动概率 |
|----------|----------|----------|--------------|
| 银行     | 强影响   | 利好     | 高           |

## 三、核心个股分析（通达信格式）

| 代码   | 名称   | 影响方向 | 上涨概率 | 目标价区间 | 止损价 |
|--------|--------|----------|----------|------------|--------|
| 600036 | 招商银行 | 利好   | 75%      | 35-37     | 32     |
```

## 注意事项

1. 本系统仅供学习和研究使用
2. 不构成投资建议，股市有风险
3. 实际使用需要接入真实数据源
4. 需要配置AI模型进行智能分析

## 开发计划

- [x] 接入真实数据源API（通达信本地数据）
- [ ] 集成AI模型（GPT/ Claude）
- [ ] 添加Web界面
- [ ] 实现消息通知功能
- [ ] 添加回测功能
- [ ] 增强本地数据源支持
- [ ] 性能优化

## 许可证

MIT License
