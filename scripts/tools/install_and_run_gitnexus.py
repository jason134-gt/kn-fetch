#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速安装依赖并运行GitNexus完整系统
"""

import subprocess
import sys

print("=" * 80)
print("GitNexus完整系统 - 依赖安装和运行")
print("=" * 80)
print()

# 检查依赖
dependencies = {
    'sqlalchemy': 'sqlalchemy>=2.0.25',
    'openai': 'openai>=1.10.0',
    'langchain': 'langchain>=0.1.0',
    'pydantic': 'pydantic>=2.5.3'
}

print("1. 检查依赖状态:")
print("-" * 80)

missing = []
for module, package in dependencies.items():
    try:
        __import__(module)
        print(f"  [OK] {module}")
    except ImportError:
        print(f"  [MISSING] {module}")
        missing.append(package)

if missing:
    print()
    print("2. 安装缺失的依赖:")
    print("-" * 80)
    print(f"  将安装: {', '.join(missing)}")
    print()
    
    # 安装命令
    install_cmd = [sys.executable, '-m', 'pip', 'install'] + missing
    
    try:
        subprocess.check_call(install_cmd)
        print()
        print("  [SUCCESS] 依赖安装成功！")
    except subprocess.CalledProcessError as e:
        print(f"  [ERROR] 安装失败: {e}")
        print()
        print("  请手动安装:")
        print(f"    pip install {' '.join(missing)}")
        sys.exit(1)
else:
    print()
    print("  所有依赖已安装！")

print()
print("=" * 80)
print("3. 运行GitNexus完整系统:")
print("=" * 80)
print()
print("  两种模式可选:")
print()
print("  [模式1] 完整分析（需要API密钥）:")
print("    - 设置环境变量: set ARK_API_KEY=your_key")
print("    - 运行: python run_stock_datacenter.py")
print()
print("  [模式2] 简化分析（不需要API密钥）:")
print("    - 修改 run_stock_datacenter.py 第91行:")
print("      deep_analysis=False")
print("    - 运行: python run_stock_datacenter.py")
print()
print("=" * 80)
