#!/usr/bin/env python3
"""
前端 API 服务启动脚本
"""
import sys
from pathlib import Path

# 添加项目根目录到 Python 路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

import uvicorn
from src.web.app import app

if __name__ == "__main__":
    print("=" * 60)
    print("KN-Fetch 前端 API 服务")
    print("=" * 60)
    print(f"服务地址: http://127.0.0.1:8000")
    print(f"API 文档: http://127.0.0.1:8000/docs")
    print(f"前端代理: http://localhost:3000 -> http://localhost:8000/api")
    print("=" * 60)
    uvicorn.run(app, host="127.0.0.1", port=8000)
