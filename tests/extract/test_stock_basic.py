#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
最基础的stock_datacenter测试
只测试文件读取和基本的代码统计
"""

import os
import sys
from pathlib import Path
from datetime import datetime
from collections import defaultdict

# 添加项目根目录到路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

print("=" * 80)
print("Stock Datacenter 基础测试")
print("=" * 80)
print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print()

# 源目录
source_dir = Path("tests/example/stock_datacenter")
print(f"源目录: {source_dir.absolute()}")
print()

# 1. 文件统计
print("=" * 80)
print("1. 文件统计")
print("=" * 80)

file_stats = defaultdict(lambda: {'count': 0, 'size': 0, 'lines': 0})

for file_path in source_dir.rglob("*"):
    if file_path.is_file():
        ext = file_path.suffix.lower()
        if ext in ['.java', '.xml', '.properties']:
            try:
                size = file_path.stat().st_size
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                    lines = len(f.readlines())
                
                file_stats[ext]['count'] += 1
                file_stats[ext]['size'] += size
                file_stats[ext]['lines'] += lines
            except Exception as e:
                print(f"  警告: 无法读取文件 {file_path.name}: {e}")

print(f"\n文件类型统计:")
for ext, stats in sorted(file_stats.items()):
    print(f"  {ext:12s}: {stats['count']:4d} 个文件, "
          f"{stats['size']/1024:8.2f} KB, "
          f"{stats['lines']:6d} 行代码")

# 2. Java文件分析
print(f"\n" + "=" * 80)
print("2. Java文件分析")
print("=" * 80)

java_files = list(source_dir.rglob("*.java"))
print(f"找到 {len(java_files)} 个Java文件")

# 分析前10个Java文件
print(f"\n分析前10个Java文件:")
java_info = []
for i, java_file in enumerate(java_files[:10], 1):
    try:
        with open(java_file, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            lines = len(content.split('\n'))
            
            # 简单的类和方法统计
            class_count = content.count('class ')
            method_count = content.count('public ') + content.count('private ') + content.count('protected ')
            
            rel_path = java_file.relative_to(source_dir)
            java_info.append({
                'path': str(rel_path),
                'lines': lines,
                'classes': class_count,
                'methods': method_count
            })
            
            print(f"  {i:2d}. {rel_path}")
            print(f"      行数: {lines}, 类数: {class_count}, 方法数: {method_count}")
    except Exception as e:
        print(f"  {i:2d}. {java_file.name}: 读取失败 - {e}")

# 3. 项目结构分析
print(f"\n" + "=" * 80)
print("3. 项目结构分析")
print("=" * 80)

# 目录结构
dir_structure = defaultdict(int)
for dir_path in source_dir.rglob("*"):
    if dir_path.is_dir():
        java_in_dir = len(list(dir_path.glob("*.java")))
        if java_in_dir > 0:
            rel_dir = dir_path.relative_to(source_dir)
            dir_structure[str(rel_dir)] = java_in_dir

print(f"\n包含Java文件的目录 (前10个):")
for dir_path, count in sorted(dir_structure.items(), key=lambda x: -x[1])[:10]:
    print(f"  {dir_path:50s}: {count:3d} 个文件")

# 4. pom.xml分析
print(f"\n" + "=" * 80)
print("4. Maven配置分析")
print("=" * 80)

pom_file = source_dir / "pom.xml"
if pom_file.exists():
    try:
        with open(pom_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # 提取基本信息
        import re
        group_id = re.search(r'<groupId>(.*?)</groupId>', content, re.DOTALL)
        artifact_id = re.search(r'<artifactId>(.*?)</artifactId>', content, re.DOTALL)
        version = re.search(r'<version>(.*?)</version>', content, re.DOTALL)
        
        print(f"\n项目信息:")
        if group_id:
            print(f"  Group ID: {group_id.group(1).strip()}")
        if artifact_id:
            print(f"  Artifact ID: {artifact_id.group(1).strip()}")
        if version:
            print(f"  Version: {version.group(1).strip()}")
        
        # 统计依赖
        dependencies = re.findall(r'<dependency>', content)
        print(f"  依赖数量: {len(dependencies)}")
        
    except Exception as e:
        print(f"  警告: 无法读取pom.xml: {e}")
else:
    print("  未找到pom.xml文件")

# 5. 总结
print(f"\n" + "=" * 80)
print("5. 测试总结")
print("=" * 80)

total_files = sum(stats['count'] for stats in file_stats.values())
total_size = sum(stats['size'] for stats in file_stats.values())
total_lines = sum(stats['lines'] for stats in file_stats.values())

print(f"\n总计:")
print(f"  文件数: {total_files}")
print(f"  总大小: {total_size/1024/1024:.2f} MB")
print(f"  总行数: {total_lines:,}")
print(f"\n完成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print("=" * 80)
