#!/usr/bin/env python3
"""使用端口 8001 启动前端 API 服务"""
import uvicorn
from frontend_api_server import app

if __name__ == "__main__":
    print("=" * 70)
    print("KN-Fetch 前端 API 服务")
    print("=" * 70)
    print("服务地址: http://127.0.0.1:8001")
    print("API 文档: http://127.0.0.1:8001/docs")
    print("=" * 70)
    uvicorn.run(app, host="127.0.0.1", port=8001)
