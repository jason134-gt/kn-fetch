# API密钥配置说明

## 安全配置指南

为了保护您的API密钥安全，请按照以下步骤配置：

### 1. 创建环境变量文件

将 `.env.example` 文件复制为 `.env`：

```bash
cp .env.example .env
```

### 2. 编辑 `.env` 文件

在 `.env` 文件中填入您的API密钥：

```env
# 火山引擎 API 密钥
ARK_API_KEY=7c53500f-cf96-485d-bfe2-78db6827f926

# 其他可选API密钥（如果使用其他LLM服务）
# OPENAI_API_KEY=您的OpenAI API密钥
# ANTHROPIC_API_KEY=您的Anthropic API密钥
# DASHSCOPE_API_KEY=您的通义千问API密钥
```

### 3. 确保 `.env` 文件被忽略

`.env` 文件已添加到 `.gitignore` 中，不会被提交到Git仓库。

### 4. 设置环境变量（可选）

除了使用 `.env` 文件，您也可以在系统环境变量中设置：

**Windows PowerShell:**
```powershell
$env:ARK_API_KEY = "7c53500f-cf96-485d-bfe2-78db6827f926"
```

**Windows 命令提示符:**
```cmd
set ARK_API_KEY=7c53500f-cf96-485d-bfe2-78db6827f926
```

**Linux/macOS:**
```bash
export ARK_API_KEY="7c53500f-cf96-485d-bfe2-78db6827f926"
```

### 5. 验证配置

运行测试脚本来验证配置是否正确：

```bash
python test_simple_volcengine.py
```

## 配置优先级

系统会按照以下优先级读取API密钥：

1. **环境变量**（系统环境变量或`.env`文件）
2. 配置文件中的空值（用于向后兼容）

## 安全提醒

- ❌ 永远不要将包含真实API密钥的配置文件提交到Git仓库
- ✅ 使用 `.env` 文件或系统环境变量存储敏感信息
- ✅ 确保 `.env` 文件在 `.gitignore` 中
- ✅ 定期轮换API密钥以保证安全