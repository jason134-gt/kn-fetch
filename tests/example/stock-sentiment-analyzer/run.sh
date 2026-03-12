#!/bin/bash

# 股票舆情分析系统 - 快速启动脚本

echo "========================================="
echo "股票舆情分析系统 - 启动脚本"
echo "========================================="
echo ""

# 检查Python版本
echo "检查Python版本..."
python3 --version

# 安装依赖
echo ""
echo "安装依赖包..."
pip3 install -r requirements.txt

# 运行系统
echo ""
echo "启动系统..."
echo ""
python3 src/main.py --mode once
