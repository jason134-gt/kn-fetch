# KN-Fetch API 参考文档

## 1. API 概览

### 1.1 总体介绍
KN-Fetch 是一个先进的代码分析工具，通过人工智能技术对代码库进行深度知识提取和分析。系统采用模块化架构，提供智能代码理解、语义提取、工作流引擎等核心功能。

### 1.2 使用场景
- **代码质量分析**：自动识别代码中的设计模式和架构问题
- **知识图谱构建**：从代码中提取语义信息构建知识网络
- **文档自动生成**：基于代码分析自动生成技术文档
- **业务逻辑分析**：深度理解代码中的业务规则和流程

## 2. 认证机制

### 2.1 认证方式
```python
# API 密钥认证示例
from kn_fetch import KNFetchClient

client = KNFetchClient(
    api_key="your_api_key_here",
    base_url="https://api.kn-fetch.com/v1"
)
```

### 2.2 令牌管理
```python
# 令牌刷新机制
try:
    analysis_result = client.analyze_code(repository_path)
except AuthenticationError:
    client.refresh_token()
    analysis_result = client.analyze_code(repository_path)
```

## 3. 主要接口说明

### 3.1 kn-fetch.py 主接口

#### analyze_repository()
**功能**：分析整个代码仓库
```python
def analyze_repository(repo_path: str, 
                      analysis_depth: str = "deep",
                      output_format: str = "json") -> AnalysisResult
```

**参数说明**：
- `repo_path`：代码仓库路径（必需）
- `analysis_depth`：分析深度（"quick" | "standard" | "deep"）
- `output_format`：输出格式（"json" | "xml" | "yaml"）

### 3.2 LLM Client 接口

#### generate_insights()
**功能**：基于LLM生成代码洞察
```python
def generate_insights(code_snippets: List[str], 
                     context: Dict[str, Any],
                     model: str = "gpt-4") -> InsightResult
```

### 3.3 Deep Knowledge Analyzer

#### extract_knowledge_graph()
**功能**：从代码中提取知识图谱
```python
def extract_knowledge_graph(source_files: List[FileObject],
                           relationship_types: List[str] = None) -> KnowledgeGraph
```

## 4. 请求/响应示例

### 4.1 基本代码分析请求
```python
# 请求示例
from kn_fetch import KNFetchClient

client = KNFetchClient(api_key="your_key")
result = client.analyze_repository(
    repo_path="/path/to/your/code",
    analysis_depth="deep",
    output_format="json"
)

# 响应示例
{
    "status": "success",
    "analysis_id": "ana_123456",
    "summary": {
        "total_files": 156,
        "analysis_time": "2m34s",
        "issues_found": 23
    },
    "detailed_results": {
        "architectural_issues": [...],
        "code_smells": [...],
        "business_logic": [...]
    }
}
```

### 4.2 错误处理示例
```python
try:
    result = client.analyze_repository("/invalid/path")
except RepositoryNotFoundError as e:
    print(f"仓库未找到: {e}")
except AnalysisTimeoutError as e:
    print(f"分析超时: {e}")
except APIQuotaExceededError as e:
    print(f"API配额不足: {e}")
```

## 5. 错误码说明

### 5.1 客户端错误 (4xx)
| 错误码 | 含义 | 解决方案 |
|--------|------|----------|
| 400 | 请求参数错误 | 检查参数格式和必填字段 |
| 401 | 认证失败 | 验证API密钥有效性 |
| 403 | 权限不足 | 检查账户权限设置 |
| 404 | 资源未找到 | 确认仓库路径正确 |
| 429 | 请求频率超限 | 降低请求频率或升级套餐 |

### 5.2 服务端错误 (5xx)
| 错误码 | 含义 | 解决方案 |
|--------|------|----------|
| 500 | 内部服务器错误 | 联系技术支持 |
| 502 | 网关错误 | 稍后重试 |
| 503 | 服务不可用 | 检查服务状态页 |
| 504 | 网关超时 | 优化分析配置 |

## 6. 使用限制

### 6.1 调用限制
- **免费版**：100次调用/天，最大文件数：1000个
- **专业版**：1000次调用/天，最大文件数：10000个
- **企业版**：自定义限制，支持大代码库分析

### 6.2 配额管理
```python
# 检查配额使用情况
quota_info = client.get_quota_info()
print(f"今日已用: {quota_info.used_today}")
print(f"剩余配额: {quota_info.remaining}")
```

## 7. 最佳实践

### 7.1 性能优化建议

#### 代码预处理
```python
# 优化分析性能
optimized_config = {
    "exclude_patterns": [".git", "node_modules", "*.min.js"],
    "include_extensions": [".py", ".js", ".java", ".cpp"],
    "max_file_size": "10MB"
}

result = client.analyze_repository(
    repo_path,
    analysis_config=optimized_config
)
```

#### 增量分析
```python
# 只分析变化的文件
changed_files = get_git_changes_since_last_analysis()
result = client.analyze_files(changed_files)
```

### 7.2 使用建议

#### 批量处理
```python
# 批量分析多个仓库
repositories = ["/repo1", "/repo2", "/repo3"]
results = []

for repo in repositories:
    try:
        result = client.analyze_repository(repo)
        results.append(result)
    except Exception as e:
        print(f"分析 {repo} 失败: {e}")
        continue
```

#### 结果缓存
```python
# 实现结果缓存机制
def cached_analysis(repo_path, cache_duration=3600):
    cache_key = f"analysis_{hash(repo_path)}"
    cached_result = cache.get(cache_key)
    
    if cached_result:
        return cached_result
    
    result = client.analyze_repository(repo_path)
    cache.set(cache_key, result, cache_duration)
    return result
```

### 7.3 监控和日志
```python
# 添加监控和日志
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("kn_fetch")

def monitored_analysis(repo_path):
    start_time = time.time()
    logger.info(f"开始分析仓库: {repo_path}")
    
    try:
        result = client.analyze_repository(repo_path)
        duration = time.time() - start_time
        logger.info(f"分析完成，耗时: {duration:.2f}s")
        return result
    except Exception as e:
        logger.error(f"分析失败: {e}")
        raise
```

---

**文档版本**: v1.0  
**最后更新**: 2024年1月  
**技术支持**: support@kn-fetch.com  
**API文档**: https://docs.kn-fetch.com/api