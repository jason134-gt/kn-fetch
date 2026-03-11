# KN-Fetch API 参考文档

## 1. API 概览

### 1.1 总体介绍
KN-Fetch 是一款先进的代码分析工具，通过深度语义分析和人工智能技术，提供智能化的代码理解、架构分析和文档生成功能。API 采用 RESTful 设计风格，支持同步和异步调用模式。

### 1.2 使用场景
- 代码质量评估与优化建议
- 软件架构分析与可视化
- 自动化文档生成
- 代码安全漏洞检测
- 技术债务分析

## 2. 认证机制

### 2.1 认证方式
KN-Fetch 使用 JWT (JSON Web Token) 进行 API 认证。

```python
# 认证示例
from kn_fetch import KNFetchClient

# 初始化客户端
client = KNFetchClient(
    api_key="your_api_key",
    base_url="https://api.kn-fetch.com/v1"
)

# 自动处理认证令牌
```

### 2.2 令牌管理
- 令牌有效期：24小时
- 自动刷新机制
- 支持多环境配置

## 3. 主要接口说明

### 3.1 代码分析接口

#### POST /v1/analysis/code
深度代码语义分析

**参数说明：**
```python
{
    "code_content": "string",          # 必需，源代码内容
    "language": "string",              # 必需，编程语言
    "analysis_level": "string",        # 可选，分析深度级别
    "include_metrics": "boolean"       # 可选，是否包含代码度量
}
```

### 3.2 架构分析接口

#### POST /v1/analysis/architecture
软件架构分析

**参数说明：**
```python
{
    "project_structure": "object",     # 必需，项目结构
    "dependencies": "array",          # 可选，依赖关系
    "config_files": "array"           # 可选，配置文件
}
```

### 3.3 文档生成接口

#### POST /v1/generate/documentation
自动化文档生成

**参数说明：**
```python
{
    "analysis_id": "string",          # 必需，分析任务ID
    "template_type": "string",        # 可选，文档模板类型
    "output_format": "string"         # 可选，输出格式
}
```

## 4. 请求/响应示例

### 4.1 代码分析示例

**请求示例：**
```python
import asyncio
from kn_fetch import KNFetchClient

async def analyze_code():
    client = KNFetchClient(api_key="your_api_key")
    
    response = await client.analyze_code(
        code_content="""
        def calculate_fibonacci(n):
            if n <= 1:
                return n
            return calculate_fibonacci(n-1) + calculate_fibonacci(n-2)
        """,
        language="python",
        analysis_level="deep",
        include_metrics=True
    )
    
    return response

# 执行分析
result = asyncio.run(analyze_code())
```

**成功响应：**
```json
{
    "status": "success",
    "analysis_id": "ana_123456789",
    "results": {
        "complexity": 15,
        "maintainability": 85,
        "security_issues": [],
        "performance_suggestions": ["建议使用记忆化优化递归"]
    },
    "timestamp": "2024-01-15T10:30:00Z"
}
```

**错误处理：**
```python
try:
    response = await client.analyze_code(invalid_code)
except KNFetchAPIError as e:
    print(f"API错误: {e.status_code} - {e.message}")
except KNFetchTimeoutError as e:
    print(f"请求超时: {e}")
```

### 4.2 异步任务示例

```python
# 提交异步分析任务
task = await client.submit_async_analysis(
    repository_url="https://github.com/example/repo",
    analysis_type="full_scan"
)

# 轮询任务状态
while task.status in ["pending", "running"]:
    await asyncio.sleep(5)
    task = await client.get_task_status(task.id)

if task.status == "completed":
    results = await client.get_analysis_results(task.id)
```

## 5. 错误码说明

### 5.1 错误码分类

| 错误码 | 类型 | 描述 |
|--------|------|------|
| 400xx | 请求错误 | 客户端请求参数错误 |
| 401xx | 认证错误 | 身份验证失败 |
| 403xx | 权限错误 | 访问权限不足 |
| 404xx | 资源未找到 | 请求的资源不存在 |
| 429xx | 限流错误 | 请求频率超限 |
| 500xx | 服务器错误 | 服务端内部错误 |

### 5.2 常见错误码详情

**40001 - 无效的代码内容**
```json
{
    "error_code": "40001",
    "message": "提供的代码内容格式无效",
    "details": "请检查代码语法是否正确"
}
```

**42901 - 请求频率超限**
```json
{
    "error_code": "42901",
    "message": "API调用频率超限",
    "retry_after": 60
}
```

## 6. 使用限制

### 6.1 调用限制

| 限制类型 | 免费版 | 专业版 | 企业版 |
|----------|--------|--------|--------|
| 每分钟请求数 | 60 | 300 | 1000 |
| 每月总请求数 | 10,000 | 100,000 | 无限制 |
| 最大代码大小 | 1MB | 10MB | 50MB |
| 并发任务数 | 1 | 5 | 20 |

### 6.2 配额管理

```python
# 查询配额使用情况
quota_info = await client.get_quota_info()
print(f"已使用: {quota_info.used}/{quota_info.total}")

# 设置速率限制
client.set_rate_limit(requests_per_minute=300)
```

## 7. 最佳实践

### 7.1 使用建议

**批量处理优化：**
```python
async def batch_analyze_files(file_paths):
    # 使用异步并发处理
    tasks = []
    for file_path in file_paths:
        with open(file_path, 'r') as f:
            code_content = f.read()
        
        task = client.analyze_code(
            code_content=code_content,
            language="python"
        )
        tasks.append(task)
    
    # 并发执行所有分析任务
    results = await asyncio.gather(*tasks, return_exceptions=True)
    return results
```

**缓存策略：**
```python
from functools import lru_cache

class CachedAnalyzer:
    def __init__(self, client):
        self.client = client
        self.cache = {}
    
    @lru_cache(maxsize=1000)
    async def analyze_with_cache(self, code_hash, code_content):
        # 检查缓存
        if code_hash in self.cache:
            return self.cache[code_hash]
        
        # 执行分析并缓存结果
        result = await self.client.analyze_code(code_content)
        self.cache[code_hash] = result
        return result
```

### 7.2 性能优化

**连接池配置：**
```python
import aiohttp

# 优化HTTP连接池
session = aiohttp.ClientSession(
    connector=aiohttp.TCPConnector(limit=100, limit_per_host=30),
    timeout=aiohttp.ClientTimeout(total=30)
)

client = KNFetchClient(
    api_key="your_api_key",
    session=session
)
```

**错误重试机制：**
```python
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=4, max=10)
)
async def robust_analysis(code_content):
    return await client.analyze_code(code_content)
```

### 7.3 监控和日志

```python
import logging

# 配置详细日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger('kn-fetch')

# 添加监控指标
class MonitoredClient:
    def __init__(self, client):
        self.client = client
        self.metrics = {
            'requests_total': 0,
            'errors_total': 0,
            'total_response_time': 0
        }
    
    async def monitored_analyze(self, code_content):
        start_time = time.time()
        try:
            result = await self.client.analyze_code(code_content)
            self.metrics['requests_total'] += 1
            return result
        except Exception as e:
            self.metrics['errors_total'] += 1
            logger.error(f"分析失败: {e}")
            raise
        finally:
            response_time = time.time() - start_time
            self.metrics['total_response_time'] += response_time
```

---

**文档版本：** v1.2.0  
**最后更新：** 2024年1月15日  
**技术支持：** support@kn-fetch.com