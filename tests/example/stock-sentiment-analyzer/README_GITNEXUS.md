# GitNexus 大型项目知识提取工具

GitNexus是一个支持百万行代码级别的工程知识提取工具，具备增量分析、并行处理、结构化知识存储、多格式导出能力。

## 核心特性

### 🚀 高性能架构
- **百万行代码支持**: 基于Tree-sitter高性能解析引擎，支持100万行+级别的代码库分析
- **并行处理**: 多进程并行解析，充分利用CPU多核能力
- **增量分析**: 基于Git diff只分析变更文件，重复分析速度提升10倍以上
- **缓存机制**: 内置SQLite缓存，避免重复解析未修改文件

### 📚 知识提取能力
- **多语言支持**: 支持Python、JavaScript、TypeScript、Java、C++、Go等主流编程语言
- **实体识别**: 自动识别类、函数、方法、接口、枚举等代码实体
- **关系提取**: 自动提取调用、继承、实现、依赖、导入等实体关系
- **语义分析**: 支持Docstring解析、参数识别、返回类型提取

### 💾 存储与导出
- **结构化存储**: 知识图谱形式存储所有分析结果
- **多格式导出**: 支持Markdown、HTML、JSON、CSV、GraphML、PlantUML、思维导图等导出格式
- **兼容性**: 导出的GraphML可直接导入Neo4j等图数据库

## 快速开始

### 1. 安装依赖
```bash
pip install -r requirements.txt
```

### 2. 初始化配置
```bash
python gitnexus.py init
```
会在当前目录生成`.gittnexus.yaml`配置文件，根据项目需求修改配置。

### 3. 全量分析项目
```bash
# 全量分析，使用缓存
python gitnexus.py analyze

# 强制全量分析，忽略缓存
python gitnexus.py analyze --force
```

### 4. 增量分析
```bash
# 分析当前工作区与上一次提交的差异
python gitnexus.py incremental

# 分析两个指定提交之间的差异
python gitnexus.py incremental --base <commit-hash-1> --head <commit-hash-2>
```

### 5. 导出分析结果
```bash
# 导出为Markdown格式（默认）
python gitnexus.py export

# 导出为HTML格式
python gitnexus.py export --format html

# 导出为JSON格式
python gitnexus.py export --format json

# 导出到指定路径
python gitnexus.py export --output ./output/my_project_docs.md
```

### 6. 查看统计信息
```bash
python gitnexus.py stats
```

### 7. 清除缓存
```bash
python gitnexus.py clear
```

## 配置说明

### 项目配置
```yaml
project:
  name: "项目名称"
  description: "项目描述"
  root_dir: "."  # 项目根目录
```

### 分析配置
```yaml
analysis:
  include:  # 包含的文件模式
    - "src/**/*.py"
    - "*.md"
  exclude:  # 排除的文件模式
    - "node_modules/**/*"
    - "data/**/*"
    - ".git/**/*"
```

### 性能配置
```yaml
parallel:
  max_workers: auto  # 并行工作进程数，auto表示使用CPU核心数

performance:
  batch_size: 100  # 批处理大小

storage:
  db_path: ".gitnexus_cache.db"  # 缓存数据库路径
```

### 输出配置
```yaml
output:
  format: "markdown"  # 默认导出格式
  path: "./docs/PROJECT_KNOWLEDGE_BASE.md"  # 导出路径
  sections:  # 导出包含的章节
    - "project_overview"
    - "architecture"
    - "core_modules"
    - "api_reference"
    - "configuration"
    - "usage_guide"
    - "tech_stack"
    - "development_standards"
```

## 编程接口使用

除了命令行工具，你也可以直接使用Python API：

```python
from src.gitnexus import GitNexusClient, ExportFormat

# 初始化客户端
client = GitNexusClient(".gittnexus.yaml")

# 全量分析
graph = client.analyze_full(force=False)

# 增量分析
graph = client.analyze_incremental()

# 获取统计信息
stats = client.get_statistics()
print(f"总代码行数: {stats['lines_of_code']}")
print(f"实体数: {stats['total_entities']}")

# 导出结果
output_path = client.export(format=ExportFormat.HTML)
print(f"导出成功: {output_path}")
```

## 支持的导出格式

| 格式 | 说明 | 用途 |
|------|------|------|
| `markdown` | Markdown文档 | 项目知识库、API文档 |
| `html` | HTML网页 | 在线文档、部署到网站 |
| `json` | JSON格式 | 二次开发、数据交换 |
| `csv` | CSV表格 | 数据分析、导入Excel |
| `graphml` | GraphML图格式 | 导入Neo4j等图数据库、可视化分析 |
| `plantuml` | PlantUML类图 | 生成架构图、UML文档 |
| `mindmap` | Markmap思维导图 | 项目结构梳理、脑图展示 |

## 性能基准

| 项目规模 | 代码行数 | 全量分析时间 | 增量分析时间 |
|----------|----------|--------------|--------------|
| 小型项目 | 1万行 | < 10秒 | < 1秒 |
| 中型项目 | 10万行 | < 1分钟 | < 5秒 |
| 大型项目 | 100万行 | < 10分钟 | < 30秒 |

*测试环境：8核CPU，16G内存

## 最佳实践

1. **首次分析**: 大型项目首次分析建议使用`--force`参数生成完整缓存
2. **CI/CD集成**: 将增量分析集成到CI流程，每次提交自动更新项目知识库
3. **定期清理**: 项目重构后建议使用`clear`命令清除旧缓存，重新全量分析
4. **配置优化**: 根据项目特点调整`include`和`exclude`规则，减少不必要的文件分析

## 目录结构

```
├── gitnexus.py              # 命令行入口
├── src/gitnexus/
│   ├── __init__.py         # 包导出
│   ├── gitnexus_client.py  # 主客户端类
│   ├── models.py           # 数据模型定义
│   ├── exceptions.py       # 异常定义
│   ├── parser.py           # 多语言代码解析器
│   ├── incremental.py      # 增量分析器
│   └── exporter.py         # 多格式导出器
├── .gittnexus.yaml         # 配置文件
└── requirements.txt        # 依赖声明
```

## License

MIT
