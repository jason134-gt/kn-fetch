#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简化版stock_datacenter测试
不依赖LLM，只测试基本的代码解析功能
"""

import sys
import os
from pathlib import Path
from datetime import datetime

# 添加项目根目录到路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

print("=" * 80)
print("Stock Datacenter 简化测试")
print("=" * 80)
print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print()

# 源目录
source_dir = Path("tests/example/stock_datacenter")
print(f"源目录: {source_dir.absolute()}")

# 统计文件
java_files = list(source_dir.rglob("*.java"))
xml_files = list(source_dir.rglob("*.xml"))

print(f"Java文件数: {len(java_files)}")
print(f"XML文件数: {len(xml_files)}")
print()

# 尝试导入核心模块
print("检查模块导入:")
print("-" * 80)

modules_status = {}

# 1. 检查基础模块
try:
    import yaml
    modules_status['yaml'] = 'OK'
    print(f"  [OK] yaml (v{yaml.__version__})")
except Exception as e:
    modules_status['yaml'] = f'FAIL: {e}'
    print(f"  [FAIL] yaml: {e}")

try:
    import git
    modules_status['git'] = 'OK'
    print(f"  [OK] git (GitPython)")
except Exception as e:
    modules_status['git'] = f'FAIL: {e}'
    print(f"  [FAIL] git: {e}")

try:
    import networkx
    modules_status['networkx'] = 'OK'
    print(f"  [OK] networkx")
except Exception as e:
    modules_status['networkx'] = f'FAIL: {e}'
    print(f"  [FAIL] networkx: {e}")

try:
    import tree_sitter
    modules_status['tree_sitter'] = 'OK'
    print(f"  [OK] tree_sitter")
except Exception as e:
    modules_status['tree_sitter'] = f'FAIL: {e}'
    print(f"  [FAIL] tree_sitter: {e}")

# 2. 检查项目模块
try:
    from src.models.code_metadata import FileMetadata
    modules_status['FileMetadata'] = 'OK'
    print(f"  [OK] FileMetadata")
except Exception as e:
    modules_status['FileMetadata'] = f'FAIL: {e}'
    print(f"  [FAIL] FileMetadata: {e}")

try:
    from src.agents.file_scanner import FileScannerAgent
    modules_status['FileScannerAgent'] = 'OK'
    print(f"  [OK] FileScannerAgent")
except Exception as e:
    modules_status['FileScannerAgent'] = f'FAIL: {e}'
    print(f"  [FAIL] FileScannerAgent: {e}")

try:
    from src.gitnexus import GitNexusClient
    modules_status['GitNexusClient'] = 'OK'
    print(f"  [OK] GitNexusClient")
except Exception as e:
    modules_status['GitNexusClient'] = f'FAIL: {e}'
    print(f"  [FAIL] GitNexusClient: {e}")

# 3. 如果基础模块都OK，尝试运行文件扫描
if all(modules_status.get(k) == 'OK' for k in ['yaml', 'git', 'networkx']):
    print()
    print("=" * 80)
    print("尝试运行文件扫描...")
    print("=" * 80)
    
    try:
        import asyncio
        
        async def scan_files():
            config = {
                "supported_extensions": [".java", ".xml"],
                "ignore_patterns": [
                    "**/target/**",
                    "**/build/**",
                    "**/.git/**"
                ]
            }
            
            scanner = FileScannerAgent(config)
            files = await scanner._execute_impl(str(source_dir))
            return files
        
        # 运行扫描
        files = asyncio.run(scan_files())
        
        print(f"\n扫描结果:")
        print(f"  扫描到的文件数: {len(files)}")
        
        # 显示前5个文件
        print(f"\n  前5个文件:")
        for i, f in enumerate(files[:5], 1):
            print(f"    {i}. {f.file_name} ({f.file_extension}) - {f.file_size} bytes")
        
    except Exception as e:
        print(f"  [ERROR] 文件扫描失败: {e}")
        import traceback
        traceback.print_exc()

print()
print("=" * 80)
print(f"测试完成: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print("=" * 80)
