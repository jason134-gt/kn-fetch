# 知识提取智能体 - 文档中心

欢迎来到 kn-fetch 项目文档中心！这里提供了项目的完整文档资源，帮助你快速了解、使用和参与项目。

## 📖 快速导航

| 角色 | 推荐阅读路径 |
|------|-------------|
| 👤 **新手用户** | [项目 README](../README.md) → [API 接口文档](./API.md) → [常见问题](#faq) |
| 🚀 **API 集成者** | [API 接口文档](./API.md) → [系统架构设计](./ARCHITECTURE.md) |
| 👨‍💻 **开发者** | [开发指南](./DEVELOPMENT.md) → [系统架构设计](./ARCHITECTURE.md) → [代码规范](#代码规范) |
| 🤖 **AI 开发者** | [LangChain 迁移](./LANGCHAIN_MIGRATION.md) → [系统架构设计](./ARCHITECTURE.md#33-ai-模块) |
| 🤝 **贡献者** | [贡献指南](../CONTRIBUTING.md) → [开发指南](./DEVELOPMENT.md) |

---

## 📚 核心文档

### 架构与设计文档

| 文档 | 描述 | 适用人群 |
|------|------|----------|
| [系统架构设计](./ARCHITECTURE.md) | 整体架构、模块设计、数据模型、技术选型 | 所有人 |
| [API 接口文档](./API.md) | Web API 完整参考，包含所有端点和使用示例 | API 集成者、开发者 |

### AI 与 LLM 相关

| 文档 | 描述 | 适用人群 |
|------|------|----------|
| [LangChain 迁移指南](./LANGCHAIN_MIGRATION.md) | LangChain 集成使用说明、配置方法、扩展示例 | AI 开发者 |

### 开发与贡献

| 文档 | 描述 | 适用人群 |
|------|------|----------|
| [开发指南](./DEVELOPMENT.md) | 环境搭建、代码规范、测试指南、调试技巧 | 开发者 |
| [贡献指南](../CONTRIBUTING.md) | 如何报告 Bug、提新功能、代码提交流程 | 贡献者 |
| [CLI 注释指南](./CLI_COMMENT_GUIDE.md) | CLI 模块的注释规范和最佳实践 | 代码审查者 |

### 项目文档

| 文档 | 描述 | 适用人群 |
|------|------|----------|
| [项目 README](../README.md) | 项目概述、功能特性、安装使用、常见问题 | 所有人 |
| [变更日志](../CHANGELOG.md) | 版本更新历史 | 所有人 |

---

## 🎯 按主题浏览

### 💡 了解项目

如果你是第一次接触本项目，建议按以下顺序阅读：

1. **[项目 README](../README.md)** - 了解项目是什么、能做什么
2. **[系统架构设计](./ARCHITECTURE.md)** - 理解整体架构和核心模块
3. **[API 接口文档](./API.md)** - 了解如何使用 API

### 🔌 使用 API

学习如何通过 API 使用项目功能：

- **[API 接口文档](./API.md)** - 完整的 API 参考文档
  - [状态接口](./API.md#2-状态接口) - 检查服务状态
  - [分析接口](./API.md#4-分析接口) - 执行代码分析
  - [知识接口](./API.md#5-知识接口) - 管理知识图谱
  - [架构分析接口](./API.md#6-架构分析接口) - 获取架构分析结果
  - [UML 接口](./API.md#7-uml-接口) - 生成和查询 UML 图
  - [增强分析接口](./API.md#8-增强分析接口) - 执行复杂分析任务

### 🤖 AI 功能

了解项目中的 AI 能力和 LLM 集成：

- **[LangChain 迁移指南](./LANGCHAIN_MIGRATION.md)** - 使用 LangChain 的 AI 功能
  - 配置 LangChain
  - 使用 LangChain LLM 客户端
  - 使用 Agent 编排器
  - 扩展自定义智能体
- **[深度知识分析](./ARCHITECTURE.md#33-ai-模块)** - LLM 增强的知识分析

### 🛠️ 开发与贡献

参与项目开发：

- **[开发指南](./DEVELOPMENT.md)** - 环境搭建和开发规范
  - 环境搭建步骤
  - 项目结构说明
  - 代码规范（命名、注释、提交）
  - 测试指南
  - 调试技巧
- **[贡献指南](../CONTRIBUTING.md)** - 如何参与项目贡献
  - 如何报告 Bug
  - 如何提新功能
  - 代码提交流程
  - PR 模板

---

## 🗺️ 文档地图

```
kn-fetch/
│
├── 📄 README.md                     # 项目主文档
├── 📄 CONTRIBUTING.md               # 贡献指南
│
└── 📁 docs/                         # 文档目录
    ├── 📖 README.md                # 本文档：文档导航
    │
    ├── 🏗️ ARCHITECTURE.md          # 系统架构设计
    │   ├── 整体架构
    │   ├── 模块设计
    │   ├── 数据模型
    │   └── 技术选型
    │
    ├── 🔌 API.md                   # API 接口文档
    │   ├── 认证接口
    │   ├── 状态接口
    │   ├── AI 配置接口
    │   ├── 分析接口
    │   ├── 知识接口
    │   ├── 架构分析接口
    │   ├── UML 接口
    │   └── 增强分析接口
    │
    ├── 🤖 LANGCHAIN_MIGRATION.md   # LangChain 迁移指南
    │   ├── 依赖安装
    │   ├── 配置方法
    │   ├── 使用示例
    │   └── 扩展指南
    │
    ├── 🛠️ DEVELOPMENT.md           # 开发指南
    │   ├── 环境搭建
    │   ├── 代码规范
    │   ├── 测试指南
    │   └── 发布流程
    │
    └── 📝 CLI_COMMENT_GUIDE.md     # CLI 注释指南
        ├── 模块级注释规范
        ├── 函数级注释模板
        ├── 行内注释规范
        └── 实际代码示例
```

---

## 📋 文档更新记录

| 日期 | 文档 | 更新内容 |
|------|------|----------|
| 2026-03-11 | README.md | 完善文档导航结构，添加主题分类和文档地图 |
| 2026-03-11 | CLI_COMMENT_GUIDE.md | 新增 CLI 模块注释指南 |
| 2026-03-11 | LANGCHAIN_MIGRATION.md | 新增 LangChain 迁移和使用说明 |
| 2026-03-11 | DEVELOPMENT.md | 新增开发环境和规范指南 |
| 2026-03-11 | CONTRIBUTING.md | 新增贡献流程和行为准则 |
| 2026-03-11 | API.md | 新增完整的 API 接口文档 |
| 2026-03-11 | ARCHITECTURE.md | 新增系统架构设计文档 |

---

## ❓ FAQ (常见问题)

### Q1: 如何快速上手？
**A:** 阅读顺序：[项目 README](../README.md) → [API 接口文档](./API.md) → [开发指南](./DEVELOPMENT.md)

### Q2: 文档有中文版吗？
**A:** 是的，所有文档目前都是中文版本。

### Q3: 如何获取帮助？
**A:** 参考 [需要帮助](#需要帮助) 部分。

### Q4: 文档会更新吗？
**A:** 是的，文档会随着项目发展持续更新。查看 [文档更新记录](#文档更新记录) 了解最新变动。

---

## 🔗 相关资源

### 官方资源
- 🏠 [项目主页](https://github.com/your-repo/kn-fetch)
- 📦 [PyPI 包](https://pypi.org/project/kn-fetch/)
- 🐛 [问题追踪](https://github.com/your-repo/kn-fetch/issues)

### 学习资源
- 📚 [Python 官方文档](https://docs.python.org/zh-cn/3/)
- 📚 [FastAPI 文档](https://fastapi.tiangolo.com/zh/)
- 📚 [LangChain 文档](https://python.langchain.com/)

### 社区
- 💬 [Discussions](https://github.com/your-repo/kn-fetch/discussions)
- 🐦 [Twitter](https://twitter.com/your-repo)

---

## 🤝 需要帮助？

如果你找不到需要的信息：

1. **搜索文档** - 使用 Ctrl+F 或 Cmd+F 在本文档中搜索关键词
2. **查看 FAQ** - 参考 [常见问题](#faq) 部分
3. **提交 Issue** - 在 [GitHub Issues](https://github.com/your-repo/kn-fetch/issues) 提问
4. **参与讨论** - 在 [GitHub Discussions](https://github.com/your-repo/kn-fetch/discussions) 发帖讨论

---

## 📝 文档贡献

欢迎改进文档！如果你发现错误、遗漏或可以改进的地方：

1. 参考 [贡献指南](../CONTRIBUTING.md)
2. Fork 项目并创建分支
3. 修改文档并提交 PR

---

## 📧 联系方式

如有问题或建议，欢迎通过以下方式联系：

- 📧 邮箱: support@example.com
- 💬 Discussions: [GitHub Discussions](https://github.com/your-repo/kn-fetch/discussions)
- 🐛 Issues: [GitHub Issues](https://github.com/your-repo/kn-fetch/issues)

---

**最后更新**: 2026-03-11 | **文档版本**: 1.0.0
