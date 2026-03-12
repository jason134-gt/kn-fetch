# 股票舆情分析系统 - 部署指南

## 系统要求

- Python 3.8+
- 4GB+ 内存
- 10GB+ 磁盘空间
- 稳定的网络连接

## 快速部署

### 1. 克隆项目

```bash
cd /root/.openclaw/workspace/stock-sentiment-analyzer
```

### 2. 安装依赖

```bash
pip3 install -r requirements.txt
```

### 3. 配置系统

编辑 `config/config.yaml`：

```yaml
# AI配置（可选，不配置则使用规则引擎）
ai:
  provider: "openai"  # openai | claude
  openai:
    api_key: "sk-xxx"  # 填入你的API Key
    model: "gpt-4"

# 股票池配置
stock_pool:
  - code: "600036"
    name: "招商银行"
    sector: "银行"
  - code: "000858"
    name: "五粮液"
    sector: "白酒"
```

### 4. 运行系统

```bash
# 单次运行
python3 src/main.py --mode once

# 定时运行（每30分钟）
python3 src/main.py --mode scheduled

# 交互模式
python3 src/main.py --mode interactive
```

## Docker部署

### 1. 创建Dockerfile

```dockerfile
FROM python:3.9-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制代码
COPY . .

# 创建日志目录
RUN mkdir -p logs data/reports

# 运行系统
CMD ["python3", "src/main.py", "--mode", "scheduled"]
```

### 2. 构建镜像

```bash
docker build -t stock-sentiment-analyzer .
```

### 3. 运行容器

```bash
docker run -d \
  --name stock-analyzer \
  -v $(pwd)/config:/app/config \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
stock-sentiment-analyzer
```

## Systemd服务部署

### 1. 创建服务文件

```bash
sudo nano /etc/systemd/system/stock-analyzer.service
```

```ini
[Unit]
Description=Stock Sentiment Analyzer
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/root/.openclaw/workspace/stock-sentiment-analyzer
ExecStart=/usr/bin/python3 /root/.openclaw/workspace/stock-sentiment-analyzer/src/main.py --mode scheduled
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 2. 启动服务

```bash
# 重载systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start stock-analyzer

# 设置开机自启
sudo systemctl enable stock-analyzer

# 查看状态
sudo systemctl status stock-analyzer

# 查看日志
sudo journalctl -u stock-analyzer -f
```

## Nginx反向代理（可选）

如果需要Web界面访问，配置Nginx：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /reports {
        alias /root/.openclaw/workspace/stock-sentiment-analyzer/data/reports;
        autoindex on;
    }
}
```

## 监控和日志

### 查看日志

```bash
# 实时日志
tail -f logs/analyzer.log

# 查看最近100行
tail -n 100 logs/analyzer.log

# 搜索错误
grep ERROR logs/analyzer.log
```

### 监控系统资源

```bash
# CPU和内存使用
top -p $(pgrep -f "src/main.py")

# 磁盘使用
df -h

# 网络连接
netstat -an | grep ESTABLISHED
```

## 故障排查

### 1. 依赖问题

```bash
# 重新安装依赖
pip3 install --force-reinstall -r requirements.txt
```

### 2. 权限问题

```bash
# 确保有写权限
chmod -R 755 /root/.openclaw/workspace/stock-sentiment-analyzer
chmod -R 777 /root/.openclaw/workspace/stock-sentiment-analyzer/data
chmod -R 777 /root/.openclaw/workspace/stock-sentiment-analyzer/logs
```

### 3. 网络问题

```bash
# 测试网络连接
curl -I https://www.eastmoney.com
curl -I https://www.cls.cn
```

### 4. API配置问题

```bash
# 测试OpenAI API
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer YOUR_API_KEY"
```

## 性能优化

### 1. 增加并发数

编辑 `config/config.yaml`：

```yaml
collector:
  batch_size: 20  # 增加批处理大小
  timeout_seconds: 60  # 增加超时时间
```

### 2. 启用缓存

```yaml
stock_data_provider:
  cache_ttl: 300  # 缓存5分钟
```

### 3. 使用更快的存储

```yaml
storage:
  type: "sqlite"  # 使用SQLite代替JSON
  path: "./data/storage.db"
```

## 安全建议

1. **保护API Key**：不要将API Key提交到版本控制
2. **使用环境变量**：敏感配置使用环境变量
3. **限制网络访问**：使用防火墙限制出站连接
4. **定期更新**：及时更新依赖包
5. **日志轮转**：配置日志轮转，避免磁盘占满

## 备份和恢复

### 备份

```bash
# 备份配置和数据
tar -czf backup_$(date +%Y%m%d).tar.gz \
  config/ data/ logs/
```

### 恢复

```bash
# 恢复备份
tar -xzf backup_20240115.tar.gz
```

## 升级

```bash
# 1. 备份当前版本
cp -r . ../stock-sentiment-analyzer.backup

# 2. 拉取新代码
git pull

# 3. 更新依赖
pip3 install -r requirements.txt

# 4. 重启服务
sudo systemctl restart stock-analyzer
```

## 联系支持

如有问题，请查看：
- 日志文件：`logs/analyzer.log`
- 项目文档：`README.md`
- 进度报告：`PROJECT_STATUS.md`
