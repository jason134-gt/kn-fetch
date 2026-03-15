# Scripts Directory

This directory contains various utility scripts organized by category.

## Directory Structure

```
scripts/
├── check/          # 环境检查和配置验证脚本
│   ├── check_all_fixes.py
│   ├── check_env.py
│   └── check_relationships.py
│
├── diagnose/       # 问题诊断脚本
│   ├── diagnose_java_analysis.py
│   ├── diagnose_knowledge_extraction.py
│   └── diagnose_tree_sitter_api.py
│
├── verify/         # 格式验证脚本
│   └── verify_skill_format.py
│
├── analyze/        # 分析工具脚本
│   ├── analyze_call_chains.py
│   └── analyze_call_structure.py
│
├── debug/          # 调试辅助脚本
│   ├── debug_java_parser.py
│   └── debug_parser.py
│
├── demo/           # 功能演示脚本
│   ├── demo_phase3_phase4.py
│   └── demo_stock_analysis.py
│
└── tools/          # 实用工具脚本
    ├── regenerate_all_docs.py
    ├── regenerate_uml.py
    └── view_relationships.py
```

## Usage

所有脚本都应从项目根目录运行：

```bash
# 环境检查
python scripts/check/check_env.py

# 诊断问题
python scripts/diagnose/diagnose_java_analysis.py

# 运行演示
python scripts/demo/demo_stock_analysis.py

# 使用工具
python scripts/tools/regenerate_uml.py
```
