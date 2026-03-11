# CLI 模块注释指南

## 注释完善要求

### 模块级注释

```python
#!/usr/bin/env python3
"""
知识提取智能体 - 命令行接口 (CLI)

提供命令行方式使用知识提取智能体的所有功能。

主要命令：
    - analyze: 分析代码和文档目录
    - export: 导出分析结果
    - optimize: 优化知识图谱
    - test: 自测试功能
    - validate: 自验证功能
    - architecture: 架构分析
    - uml: 生成 UML 图
    - refactor: 生成重构建议
    - enhanced: 增强版知识提取
    - explain: 深度解释代码实体
    - gituml: Git 增强版 UML 图
    - web: 启动 Web 管理界面

使用方式：
    python kn-fetch.py analyze <directory> [options]
    python kn-fetch.py export --output <path> [options]
    python kn-fetch.py optimize --level <level>
"""
```

### 函数级注释

```python
def handle_analyze(args):
    """
    处理分析命令
    
    根据用户参数执行代码和文档分析，可以选择：
    - 普通模式或大规模处理模式
    - 是否包含代码/文档分析
    - 是否启用 LLM 深度分析
    
    Args:
        args: 命令行参数对象
            - directory: 分析目录路径
            - config: 配置文件路径
            - no_code: 是否跳过代码分析
            - no_docs: 是否跳过文档分析
            - force: 是否强制重新分析
            - large_scale: 是否启用大规模处理
            - incremental: 是否增量分析
            - resume: 是否断点续传
            - deep: 是否启用 LLM 深度分析
    
    Returns:
        None: 分析结果会保存到默认位置
        
    Raises:
        FileNotFoundError: 当分析目录不存在时
        ValueError: 当参数无效时
    """
    # 1. 显示分析信息
    print(f"开始分析目录: {args.directory}")
    
    # 2. 根据模式选择处理器
    if args.large_scale:
        # 大规模处理模式：适合百万行级项目
        processor = LargeScaleProcessor(args.config)
        graph = processor.process_large_project(
            args.directory,
            include_code=not args.no_code,
            include_docs=not args.no_docs,
            incremental=args.incremental,
            resume=args.resume
        )
    else:
        # 普通模式：适合中小型项目
        extractor = KnowledgeExtractor(args.config)
        graph = extractor.extract_from_directory(
            args.directory,
            include_code=not args.no_code,
            include_docs=not args.no_docs,
            force=args.force,
            deep_analysis=args.deep if hasattr(args, 'deep') else False
        )
    
    # 3. 输出分析结果
    print(f"\n分析完成！")
    print(f"总文件数: {len(set(e.file_path for e in graph.entities.values()))}")
    print(f"实体数量: {len(graph.entities)}")
    print(f"关系数量: {len(graph.relationships)}")
    print(f"总代码行数: {sum(e.lines_of_code for e in graph.entities.values() if hasattr(e, 'lines_of_code'))}")
```

### 行内注释

```python
# ===== 1. 参数解析 =====
# 创建主解析器
parser = argparse.ArgumentParser(
    description="知识提取智能体 - 分析代码/文档目录生成结构化知识",
    formatter_class=argparse.ArgumentDefaultsHelpFormatter
)

# 创建子命令解析器
subparsers = parser.add_subparsers(dest="command", required=True, help="可用命令")

# ===== 2. 分析命令 =====
analyze_parser = subparsers.add_parser("analyze", help="分析指定目录")
# 必需参数：分析目录
analyze_parser.add_argument("directory", help="要分析的目录路径")
# 可选参数：配置文件路径
analyze_parser.add_argument("--config", "-c", default="config/config.yaml", help="配置文件路径")
# 标志参数：不分析代码文件
analyze_parser.add_argument("--no-code", action="store_true", help="不分析代码文件")
```

## 注释规范总结

### 1. 模块级注释
- 使用三引号 docstring
- 包含模块用途说明
- 列出主要功能/命令
- 提供使用示例

### 2. 类/函数级注释
- 使用 Google 风格的 docstring
- 包含简短描述
- 详细列出 Args 参数
- 说明 Returns 返回值
- 说明可能 Raises 的异常

### 3. 行内注释
- 解释"为什么"而非"是什么"
- 标记代码块的功能（使用 =====）
- 对复杂逻辑添加详细说明
- 对关键算法添加引用

### 4. 命令处理函数注释模板

```python
def handle_<command>(args):
    """
    处理 <command> 命令
    
    功能描述（1-2句话）
    
    Args:
        args: 命令行参数对象
            - param1: 参数说明
            - param2: 参数说明
    
    Returns:
        返回值说明（如果有）
    
    Raises:
        可能的异常说明
    """
    # 实现代码
    pass
```

## 当前 CLI 模块的改进计划

### P0 - 立即改进
- [ ] 添加模块级 docstring
- [ ] 为所有 handle_* 函数添加完整 docstring
- [ ] 为关键代码块添加注释标记

### P1 - 短期改进
- [ ] 添加错误处理注释
- [ ] 为复杂逻辑添加详细说明
- [ ] 添加性能优化注释

### P2 - 长期改进
- [ ] 重构为面向对象风格
- [ ] 添加命令模式抽象
- [ ] 改进错误消息提示
