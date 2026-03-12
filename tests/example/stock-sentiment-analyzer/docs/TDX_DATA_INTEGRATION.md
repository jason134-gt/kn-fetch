# 通达信数据集成指南

## 概述

本指南介绍如何将通达信(TDX)软件的本地行情数据集成到股票情绪分析器项目中。通达信数据提供器可以读取通达信下载的日、周、月、分钟级别数据，以及概念板块数据，作为项目的补充数据源。

## 功能特性

1. **多周期数据支持**
   - 日线数据 (`get_stock_day_data`)
   - 周线数据 (`get_stock_week_data`)
   - 月线数据 (`get_stock_month_data`)
   - 分钟线数据 (`get_stock_minute_data`)

2. **板块数据支持**
   - 概念板块列表 (`get_sector_list`)
   - 板块内股票 (`get_sector_stocks`)
   - 板块分析 (`get_sector_analysis`)

3. **技术分析**
   - 移动平均线 (MA5, MA10, MA20, MA30, MA60)
   - MACD指标
   - RSI指标
   - KDJ指标

4. **数据源降级**
   - 网络API失败时自动使用本地数据
   - 支持多数据源合并

## 安装与配置

### 1. 确保通达信软件已安装

通达信数据提供器需要访问通达信软件的安装目录，默认路径为 `C:/zd_zsone`。如果您的安装路径不同，请在配置文件中修改。

### 2. 更新配置文件

在 `config/config.yaml` 中添加以下配置：

```yaml
# 通达信数据配置
tdx:
  data_path: "C:/zd_zsone"  # 通达信安装目录
  enabled: true             # 是否启用通达信数据源
  default_market: "SH"      # 默认市场，SH:上海，SZ:深圳，BJ:北京
  cache_enabled: true       # 是否启用缓存
  cache_ttl: 300            # 缓存时间（秒）
```

### 3. 验证数据路径

运行以下命令验证通达信数据路径是否正确：

```bash
python examples/tdx_example.py
```

如果路径不正确，程序会自动尝试以下默认路径：
- `C:/zd_zsone`
- `D:/zd_zsone`
- `E:/zd_zsone`
- `C:/new_tdx`
- `D:/new_tdx`

## 快速开始

### 基本使用

```python
import asyncio
from src.utils.config_loader import ConfigLoader
from src.data.tdx_data_provider import TdxDataProvider

# 加载配置
config_loader = ConfigLoader()
config = config_loader.load()

# 初始化通达信数据提供器
tdx_provider = TdxDataProvider(config)

# 获取股票日线数据
day_data = tdx_provider.get_stock_day_data('600036', 'SH')
print(f"日线数据: {len(day_data)} 条")

# 获取分钟线数据
minute_data = tdx_provider.get_stock_minute_data('600036', 'SH', period=5)
print(f"5分钟线数据: {len(minute_data)} 条")

# 获取概念板块列表
sectors = tdx_provider.get_sector_list()
print(f"概念板块: {len(sectors)} 个")

# 计算技术指标
indicators = tdx_provider.calculate_technical_indicators('600036', 'SH', 'day')
print(f"技术指标: {indicators}")
```

### 与现有项目集成

使用 `IntegratedDataProvider` 类实现数据源降级：

```python
from examples.tdx_integration import IntegratedDataProvider

# 初始化集成数据提供器
integrated_provider = IntegratedDataProvider(config)

# 获取股票信息（优先使用网络API，失败时使用通达信）
stock_info = await integrated_provider.get_stock_info_with_fallback('600036', 'SH')
print(f"数据源: {stock_info.get('data_source')}")
print(f"股票信息: {stock_info}")
```

## API 参考

### TdxDataProvider 类

#### 初始化
```python
tdx_provider = TdxDataProvider(config)
```

#### 主要方法

1. **获取股票日线数据**
```python
get_stock_day_data(stock_code, market='SH', start_date=None, end_date=None)
```

2. **获取股票分钟线数据**
```python
get_stock_minute_data(stock_code, market='SH', period=1, start_time=None, end_time=None)
```
- `period`: 分钟周期 (1, 5, 15, 30, 60)

3. **获取股票周线/月线数据**
```python
get_stock_week_data(stock_code, market='SH', start_date=None, end_date=None)
get_stock_month_data(stock_code, market='SH', start_date=None, end_date=None)
```

4. **获取股票列表**
```python
get_stock_list(market='SH')
```

5. **获取概念板块**
```python
get_sector_list()  # 获取所有板块
get_sector_stocks(sector_name)  # 获取指定板块的股票
```

6. **计算技术指标**
```python
calculate_technical_indicators(stock_code, market='SH', data_type='day', period=1, lookback=100)
```

7. **获取股票基本信息**
```python
get_stock_info(stock_code, market='SH')
```

### IntegratedDataProvider 类

集成数据提供器，支持数据源降级策略。

#### 主要方法

1. **带降级的股票信息获取**
```python
await get_stock_info_with_fallback(stock_code, market='SH')
```

2. **多数据源K线数据获取**
```python
await get_stock_kline_with_sources(stock_code, market='SH', period='1d', count=100)
```

3. **板块分析**
```python
get_sector_analysis(sector_name)
```

## 与现有模块集成

### 1. 技术面分析专家

通达信数据可以增强技术面分析：

```python
# 在 experts/technical_analysis.py 中
from src.data import TdxDataProvider

class TechnicalAnalysisExpert:
    def __init__(self, config):
        self.tdx_provider = TdxDataProvider(config) if config.get('tdx', {}).get('enabled') else None
    
    async def analyze(self, stock_code):
        if self.tdx_provider:
            # 使用通达信数据计算技术指标
            indicators = self.tdx_provider.calculate_technical_indicators(stock_code, 'SH', 'day')
            # 使用分钟线数据进行高频分析
            minute_data = self.tdx_provider.get_stock_minute_data(stock_code, 'SH', period=5)
            # ... 分析逻辑
```

### 2. 短线交易决策

通达信分钟线数据可用于高频交易信号检测：

```python
# 在 trader/short_term_trader.py 中
from src.data import TdxDataProvider

class ShortTermTrader:
    def __init__(self, config):
        self.tdx_provider = TdxDataProvider(config) if config.get('tdx', {}).get('enabled') else None
    
    async def detect_signals(self, stock_code):
        if self.tdx_provider:
            # 获取5分钟线数据
            minute_data = self.tdx_provider.get_stock_minute_data(stock_code, 'SH', period=5)
            # 检测交易信号
            signals = self._analyze_minute_data(minute_data)
            return signals
```

### 3. 板块轮动分析

通达信板块数据可用于板块热度分析：

```python
# 在 analyzer/sector_analyzer.py 中
from src.data import TdxDataProvider

class SectorAnalyzer:
    def __init__(self, config):
        self.tdx_provider = TdxDataProvider(config) if config.get('tdx', {}).get('enabled') else None
    
    def analyze_sector_rotation(self):
        if self.tdx_provider:
            sectors = self.tdx_provider.get_sector_list()
            sector_performance = []
            
            for sector in sectors[:10]:  # 分析前10个板块
                analysis = self.tdx_provider.get_sector_analysis(sector['name'])
                if analysis:
                    sector_performance.append(analysis)
            
            # 按表现排序
            sector_performance.sort(key=lambda x: x.get('avg_change_pct', 0), reverse=True)
            return sector_performance
```

## 数据格式说明

### 日线数据格式
```python
{
    'date': '2024-01-15',      # 日期
    'open': 35.20,             # 开盘价
    'high': 36.00,             # 最高价
    'low': 34.80,              # 最低价
    'close': 35.50,            # 收盘价
    'volume': 100000000,       # 成交量（股）
    'amount': 3500000000,      # 成交额（元）
    'turnover': 3500000.0      # 成交额（万元）
}
```

### 分钟线数据格式
```python
{
    'datetime': '2024-01-15 14:30:00',  # 日期时间
    'date': '2024-01-15',               # 日期
    'time': '14:30',                    # 时间
    'open': 35.25,                      # 开盘价
    'high': 35.30,                      # 最高价
    'low': 35.20,                       # 最低价
    'close': 35.28,                     # 收盘价
    'volume': 5000000,                  # 成交量
    'amount': 176400000,                # 成交额
    'period': '5分钟'                   # 周期
}
```

### 技术指标格式
```python
{
    'ma': {
        'ma5': 35.20,
        'ma10': 34.80,
        'ma20': 34.50,
        'ma30': 34.20,
        'ma60': 33.80
    },
    'macd': {
        'dif': 0.15,
        'dea': 0.10,
        'macd': 0.05
    },
    'rsi': 65.5,
    'kdj': {
        'k': 75.2,
        'd': 70.8,
        'j': 84.0
    }
}
```

## 故障排除

### 常见问题

1. **数据文件不存在**
   ```
   错误: 通达信数据路径不存在: C:/zd_zsone
   ```
   解决方案：在配置文件中设置正确的通达信安装路径。

2. **股票代码格式错误**
   - 上海市场: `600036`
   - 深圳市场: `000001`
   - 北京市场: `830799`

3. **数据解析失败**
   - 确保通达信软件已下载所需数据
   - 检查文件权限
   - 验证文件编码（通常为GBK）

### 调试模式

启用详细日志输出：

```python
import logging
logging.basicConfig(level=logging.DEBUG)

tdx_provider = TdxDataProvider(config)
```

## 性能优化

1. **启用缓存**
   ```yaml
   tdx:
     cache_enabled: true
     cache_ttl: 300  # 缓存5分钟
   ```

2. **批量处理**
   ```python
   # 批量获取股票信息
   for stock_code in stock_codes:
       data = tdx_provider.get_stock_day_data(stock_code, 'SH')
   ```

3. **数据预处理**
   ```python
   # 预加载常用数据
   common_stocks = ['600036', '000001', '600519']
   preloaded_data = {}
   for code in common_stocks:
       preloaded_data[code] = tdx_provider.get_stock_day_data(code, 'SH')
   ```

## 扩展开发

### 添加新的数据解析器

1. 继承 `TdxDataProvider` 类
2. 实现新的数据解析方法
3. 更新配置文件

### 支持其他市场数据

修改 `MARKET_CODES` 映射，添加新的市场代码：

```python
MARKET_CODES = {
    'SH': 'sh',      # 上海证券交易所
    'SZ': 'sz',      # 深圳证券交易所
    'BJ': 'bj',      # 北京证券交易所
    'HK': 'hk',      # 香港交易所（如需支持）
}
```

## 注意事项

1. **数据更新频率**
   - 通达信数据需要手动或定时更新
   - 建议设置定时任务同步数据

2. **数据完整性**
   - 本地数据可能不完整
   - 建议与网络API数据互补使用

3. **文件权限**
   - 确保程序有读取通达信数据文件的权限
   - 避免同时写入操作

4. **内存使用**
   - 大量数据加载时注意内存使用
   - 考虑分页加载或流式处理