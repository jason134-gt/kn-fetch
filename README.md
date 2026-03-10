# 知识提取智能体 (KN-Fetch)

一个功能强大的知识提取智能体，能够分析代码和文档目录，生成结构化知识图谱，支持百万行级别项目处理，具备LLM交互能力，可自优化、自测试、自验证，提取足够重构所需的技术细节。

## 功能特性

### 1. 多类型内容分析
- ✅ 支持多种编程语言代码分析：Python、JavaScript、TypeScript、Java、C++、Go等
- ✅ 支持多种文档格式解析：Markdown、TXT、PDF、DOCX、HTML等
- ✅ 自动识别文件类型，混合目录混合分析

### 2. 大规模处理能力
- ✅ 支持百万行级别代码/文档库处理
- ✅ 分批次处理、内存优化，避免OOM
- ✅ 增量分析，只处理变更文件
- ✅ 断点续传，任务中断后可继续

### 3. LLM交互与自优化
- ✅ 集成大模型能力，支持OpenAI、Claude、通义千问等
- ✅ 自动生成复杂度分析和重构建议
- ✅ 自测试：自动生成测试用例并运行
- ✅ 自验证：验证知识提取的准确性
- ✅ 自优化：自动修正提取错误，补全缺失信息

### 4. 多种使用方式
- ✅ 命令行工具，支持脚本化运行
- ✅ Web管理界面，可视化操作和结果展示
- ✅ 支持Python API调用，可集成到其他系统

### 5. 多格式导出
- ✅ JSON、Markdown、HTML、CSV
- ✅ GraphML（可导入Neo4j等图数据库）
- ✅ PlantUML类图
- ✅ Markmap思维导图

## 快速开始

### 1. 安装依赖
```bash
pip install -r requirements.txt
```

### 2. 配置
复制并修改配置文件：
```bash
cp config/config.yaml.example config/config.yaml
```

配置说明：
- `project`：项目基本信息
- `analysis`：分析规则，包含/排除文件模式
- `ai`：LLM配置，设置API Key和模型参数
- `performance`：性能参数，根据硬件配置调整

### 3. 命令行使用

#### 分析目录
```bash
# 普通分析
python kn-fetch.py analyze /path/to/your/project

# 大规模项目分析（适合百万行级别）
python kn-fetch.py analyze /path/to/your/project --large-scale

# 只分析代码，不分析文档
python kn-fetch.py analyze /path/to/your/project --no-docs

# 强制重新分析，忽略缓存
python kn-fetch.py analyze /path/to/your/project --force
```

#### 导出结果
```bash
# 导出为Markdown
python kn-fetch.py export -o output/result -f markdown

# 导出为JSON
python kn-fetch.py export -o output/result -f json

# 导出为HTML
python kn-fetch.py export -o output/result -f html
```

#### 优化知识图谱
```bash
# 优化知识图谱，添加重构建议
python kn-fetch.py optimize --level medium
```

#### 自测试
```bash
# 自动为高复杂度代码生成并运行测试用例
python kn-fetch.py test
```

#### 自验证
```bash
# 验证知识提取的准确性
python kn-fetch.py validate /path/to/your/project -o output/validation_report.md
```

#### 查看统计信息
```bash
python kn-fetch.py stats
```

#### 启动Web界面
```bash
python kn-fetch.py web --port 8000
```
访问 http://localhost:8000 即可使用Web管理界面。

## 项目结构
```
kn-fetch/
├── kn-fetch.py              # 命令行入口
├── config/                  # 配置文件目录
│   └── config.yaml          # 主配置文件
├── src/
│   ├── gitnexus/            # GitNexus代码分析引擎
│   ├── core/                # 核心知识提取模块
│   │   ├── knowledge_extractor.py    # 知识提取器
│   │   ├── document_parser.py        # 文档解析器
│   │   └── large_scale_processor.py  # 大规模处理器
│   ├── ai/                  # LLM交互与自能能力模块
│   │   ├── ai_client.py             # LLM客户端
│   │   ├── knowledge_optimizer.py   # 知识优化器
│   │   ├── self_testing.py          # 自测试引擎
│   │   └── self_validation.py       # 自验证引擎
│   ├── cli/                 # 命令行模块
│   │   └── cli.py
│   └── web/                 # Web界面模块
│       ├── app.py                  # FastAPI应用
│       ├── templates/              # HTML模板
│       └── static/                 # 静态资源
├── output/                # 输出目录
├── tests/                 # 测试代码
├── docs/                  # 文档
├── requirements.txt       # 依赖列表
└── README.md              # 项目说明
```

## 使用场景

### 代码重构辅助
- 自动分析代码结构，识别高复杂度模块
- 生成重构建议，评估重构风险
- 提供完整的依赖关系图，避免遗漏

### 项目交接与文档生成
- 自动生成项目技术文档
- 分析代码中的隐含逻辑和依赖
- 生成API参考和架构图

### 技术债务管理
- 定期分析代码质量变化
- 识别技术债务热点
- 跟踪重构进度和效果

### 知识库构建
- 分析项目所有文档和代码，统一结构化存储
- 支持全局搜索和关联查询
- 导出多种格式的知识库

## 性能指标

| 项目规模 | 处理时间 | 内存占用 |
|----------|----------|----------|
| 10万行 | < 5分钟 | < 2GB |
| 50万行 | < 20分钟 | < 8GB |
| 100万行 | < 40分钟 | < 16GB |

*测试环境：8核CPU，16GB内存，SSD硬盘

## 技术栈

- **核心引擎**：Python + Tree-sitter + GitPython
- **多语言解析**：Tree-sitter语法解析器
- **大规模处理**：多进程并行 + 分块处理 + 内存优化
- **LLM集成**：OpenAI API + 自定义提示工程
- **Web界面**：FastAPI + Jinja2 + TailwindCSS + Chart.js
- **数据存储**：SQLite缓存 + JSON知识图谱

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！
