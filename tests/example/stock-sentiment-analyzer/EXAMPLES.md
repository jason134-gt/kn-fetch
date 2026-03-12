# 股票舆情分析系统 - 使用示例

## 基础使用

### 1. 单次运行

```bash
cd /root/.openclaw/workspace/stock-sentiment-analyzer
python3 src/main.py --mode once
```

**输出示例：**

```
2024-01-15 14:30:00 - __main__ - INFO - ============================================================
2024-01-15 14:30:00 - __main__ - INFO - 开始执行分析流程
2024-01-15 14:30:00 - __main__ - INFO - ============================================================
2024-01-15 14:30:00 - __main__ - INFO - 步骤1: 采集实时资讯...
2024-01-15 14:30:05 - __main__ - INFO - 采集到 15 条资讯
2024-01-15 15:30:00 - __main__ - INFO - 步骤2: 专家圆桌评估...
2024-01-15 14:30:10 - __main__ - INFO - 完成 15 条资讯的专家评估
2024-01-15 14:30:15 - __main__ - INFO - 步骤3: 综合汇总...
2024-01-15 14:30:18 - __main__ - INFO - 完成 15 条资讯的综合汇总
2024-01-15 14:30:20 - __main__ - INFO - 步骤4: 短线高手决策...
2024-01-15 14:30:25 - __main__ - INFO - 完成 15 条资讯的短线决策
2024-01-15 14:30:28 - __main__ - INFO - 步骤5: 生成分析报告...
2024-01-15 14:30:30 - __main__ - INFO - 报告已保存: data/reports/report_20240115_143030.md
2024-01-15 14:30:30 - __main__ - INFO - ============================================================
2024-01-15 14:30:30 - __main__ - INFO - 分析流程执行完成
2024-01-15 14:30:30 - __main__ - INFO - ============================================================
```

### 2. 查看报告

```bash
cat data/reports/report_20240115_143030.md
```

**报告示例：**

```markdown
# 实时股票财经舆情分析报告

报告生成时间：2024-01-15 14:30:30
采集资讯总数：15条  有效分析资讯：12条

---

## 一、核心资讯汇总

### 资讯1：央行降准0.5个百分点，释放长期资金约1万亿元
**时间**：2024-01-15 14:25:00
**来源**：东方财富网
**链接**：https://www.eastmoney.com/news/123456

**核心内容**：
中国人民银行决定于近期下调金融机构存款准备金率0.5个百分点，释放长期资金约1万亿元，以支持实体经济发展。

**专家圆桌评估共识**：
- **影响等级**：强影响
- **影响方向**：利好
- **影响时长**：中期
- **综合置信度**：78%
- **核心逻辑**：利好，强影响，中期影响

---

## 二、概念/板块影响分析

| 影响概念 | 影响等级 | 影度方向 | 核心驱动资讯 | 短线异动概率 |
|----------|----------|----------|--------------|--------------|
| 银行     | 强影响   | 利好     | 央行降准...   | 高           |
| 券商     | 中影响   | 利好     | 央行降准...   | 中           |

---

## 三、核心个股分析（通达信格式）

| 代码   | 名称   | 所属概念 | 影响方向 | 上涨概率 | 目标价区间 | 止损价 | 操作建议 |
|--------|--------|----------|----------|----------|------------|--------|----------|
| 600036 | 招商银行 | 银行/大金融 | 利好   | 75%      | 35-37     | 32     | 买入 |
| 000001 | 平安银行 | 银行/大金融 | 利好   | 72%      | 12-13     | 11     | 买入 |
| 600030 | 中信证券 | 券商/大金融 | 利好   | 68%      | 25-27     | 23     | 买入 |

---

## 四、短线操作建议

### 优先关注个股
1. **招商银行（600036）**
   - 逻辑：资讯利好，影响等级高，技术面确认
   - 风险等级：中

2. **平安银行（000001）**
   - 逻辑：资讯利好，影响等级高，技术面确认
   - 风险等级：中

### 风险提示
- **市场风险**：注意市场整体情绪变化，避免追高
- **政策风险**：关注后续政策落地情况
- **个股风险**：严格执行止损纪律，控制仓位

### 操作周期建议
**建议操作周期**：1-5个交易日
**建议仓位控制**：单只股票不超过20%
**止损纪律**：严格执行止损，亏损超过5%及时止损
```

## 高级使用

### 1. 定时运行

```bash
# 每30分钟自动运行一次
python3 src/main.py --mode scheduled
```

### 2. 交互模式

```bash
python3 src/main.py --mode interactive
```

**交互示例：**

```
> run
============================================================
开始执行分析流程
============================================================
...
============================================================
分析流程执行完成
============================================================

> run
============================================================
开始执行分析流程
============================================================
...

> exit
退出交互模式
```

### 3. 自定义配置

编辑 `config/config.yaml`：

```yaml
# 添加更多股票
stock_pool:
  - code: "600036"
    name: "招商银行"
    sector: "银行"
  - code: "000858"
    name: "五粮液"
    sector: "白酒"
  - code: "300750"
    name: "宁德时代"
    sector: "新能源"

# 调整专家权重
expert_weights:
  macro_economy: 0.30      # 增加宏观权重
  industry_research: 0.20
  technical_analysis: 0.20
  capital_flow: 0.20
  risk_control: 0.10

# 启用AI模型
ai:
  provider: "openai"
  openai:
    api_key: "sk-xxx"
    model: "gpt-4"
```

### 4. 使用AI模型

配置OpenAI API Key后，系统会自动使用AI进行评估：

```yaml
ai:
  provider: "openai"
  openai:
    api_key: "sk-proj-xxx"
    model: "gpt-4"
    temperature: 0.7
```

**AI评估优势：**
- 更准确的资讯理解
- 更细致的影响分析
- 更专业的板块识别

### 5. 获取股票数据

使用Python脚本获取实时股票数据：

```python
import asyncio
from src.data.stock_data_provider import StockDataProvider
from utils.config_loader import ConfigLoader

async def main():
    config = ConfigLoader.load('config/config.yaml')
    provider = StockDataProvider(config)

    # 获取股票信息
    stock_info = await provider.get_stock_info('600036')
    print(f"招商银行: {stock_info}")

    # 获取K线数据
    kline_data = await provider.get_stock_kline('600036', period='1d', count=10)
    print(f"K线数据: {kline_data}")

    # 计算技术指标
    indicators = await provider.calculate_technical_indicators('600036')
    print(f"技术指标: {indicators}")

asyncio.run(main())
```

## 集成使用

### 1. 导入为模块

```python
from src.main import StockSentimentAnalyzer

async def analyze():
    analyzer = StockSentimentAnalyzer('config/config.yaml')
    report = await analyzer.run_once()
    return report

# 运行分析
report = asyncio.run(analyze())
print(f"报告已生成: {report['file_path']}")
```

### 2. 集成到Web应用

```python
from flask import Flask, jsonify
from src.main import StockSentimentAnalyzer

app = Flask(__name__)
analyzer = StockSentimentAnalyzer('config/config.yaml')

@app.route('/api/analyze', methods=['POST'])
def analyze():
    report = asyncio.run(analyzer.run_once())
    return jsonify(report)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)
```

### 3. 集成到定时任务

```python
import schedule
import time
from src.main import StockSentimentAnalyzer

analyzer = StockSentimentAnalyzer('config/config.yaml')

def job():
    print("执行定时分析...")
    asyncio.run(analyzer.run_once())

# 每30分钟执行一次
schedule.every(30).minutes.do(job)

while True:
    schedule.run_pending()
    time.sleep(1)
```

## 数据导出

### 1. 导出为JSON

修改配置：

```yaml
reporter:
  output_format: "json"
```

### 2. 导出通达信格式

从报告中提取通达信格式数据：

```bash
grep -A 100 "通达信格式" data/reports/report_*.md | grep "^|" | tail -n +2
```

### 3. 导入通达信

将通达信格式数据复制到通达信的自选股：

```
600036,招商银行,银行/大金融,利好,75%,35-37,32
000001,平安银行,银行/大金融,利好,72%,12-13,11
600030,中信证券,券商/大金融,利好,68%,25-27,23
```

## 性能调优

### 1. 增加并发数

```yaml
collector:
  batch_size: 30  # 增加批处理大小
```

### 2. 启用缓存

```yaml
stock_data_provider:
  cache_ttl: 300  # 缓存5分钟
```

### 3. 使用更快的存储

```yaml
storage:
  type: "sqlite"
  path: "./data/storage.db"
```

## 常见问题

### Q1: 为什么采集到的资讯很少？

**A:** 可能原因：
1. 网络连接问题
2. 数据源限制
3. 时间段问题

**解决方法：**
```bash
# 测试网络连接
curl -I https://www.eastmoney.com
curl -I https://www.cls.cn
```

### Q2: AI评估失败怎么办？

**A:** 系统会自动降级到规则引擎，不影响基本功能。

检查配置：
```bash
# 检查API Key是否正确
grep api_key config/config.yaml
```

### Q3: 如何提高分析准确性？

**A:**
1. 配置AI模型（OpenAI/Claude）
2. 调整专家权重
3. 扩充股票池
4. 增加数据源

### Q4: 报告在哪里？

**A:** 报告保存在 `data/reports/` 目录：

```bash
# 查看最新报告
ls -lt data/reports/ | head -n 2

# 查看报告内容
cat data/reports/report_*.md
```

## 更多帮助

- **部署指南**：查看 `DEPLOYMENT.md`
- **项目文档**：查看 `README.md`
- **进度报告**：查看 `PROJECT_STATUS.md`
- **日志文件**：查看 `logs/analyzer.log`
