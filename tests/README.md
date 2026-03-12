# KN-Fetch 测试脚本分类说明

## 测试脚本组织结构

本项目对测试脚本进行了系统性的分类整理，便于管理和执行。

### 分类结构
```
tests/
├── unit/           # 单元测试 - 核心模块和基础功能测试
├── integration/    # 集成测试 - 模块间集成和系统功能验证
├── llm/           # LLM相关测试 - AI功能和文档生成测试
├── workflow/      # 工作流测试 - 完整端到端流程测试
├── samples/       # 样例工程测试 - 使用示例项目的测试
├── utils/         # 工具和辅助测试 - API和外部服务测试
└── README.md      # 本说明文档
```

## 各分类详细说明

### 1. 单元测试 (unit/)
**用途**: 验证核心模块的基本功能和导入正确性

**包含文件**:
- `test_basic_design_v1.py` - 核心模块导入测试
- `test_simple_design_v1.py` - 简化版核心功能测试
- `test_env_config.py` - 环境变量配置测试

**运行方式**:
```bash
python -m pytest tests/unit/ -v
```

### 2. 集成测试 (integration/)
**用途**: 验证模块间集成和系统整体功能

**包含文件**:
- `test_integration.py` - 系统集成测试
- `test_semantic_integration.py` - 语义提取器集成测试
- `test_existing_functionality.py` - 现有功能验证
- `test_design_v1_analysis.py` - design-v1优化版本测试

**运行方式**:
```bash
python -m pytest tests/integration/ -v
```

### 3. LLM相关测试 (llm/)
**用途**: 测试AI功能和文档生成能力

**包含文件**:
- `test_llm_api_documentation.py` - LLM API文档生成测试
- `test_llm_documentation_generation.py` - LLM深度分析文档生成
- `test_full_llm_analysis.py` - 完整LLM分析测试
- `test_quick_llm_validation.py` - 快速LLM验证测试

**运行方式**:
```bash
python -m pytest tests/llm/ -v
```

### 4. 工作流测试 (workflow/)
**用途**: 验证完整端到端分析流程

**包含文件**:
- `test_full_agent_workflow.py` - 完整智能体工作流测试
- `test_full_analysis.py` - 完整代码资产扫描测试
- `test_comprehensive_analysis.py` - 综合分析测试
- `test_final_demo.py` - 最终演示测试

**运行方式**:
```bash
python -m pytest tests/workflow/ -v
```

### 5. 样例工程测试 (samples/)
**用途**: 使用示例项目进行功能验证

**包含文件**:
- `test_sample_project.py` - 样例工程基础测试
- `test_sample_project_direct.py` - 直接CLI测试

**运行方式**:
```bash
python -m pytest tests/samples/ -v
```

### 6. 工具和辅助测试 (utils/)
**用途**: API连接和外部服务测试

**包含文件**:
- `test_simple_volcengine.py` - 火山引擎LLM API简化测试
- `test_volcengine_api.py` - 火山引擎API连接测试

**运行方式**:
```bash
python -m pytest tests/utils/ -v
```

## 原有测试文件说明

在 `tests/` 根目录下保留了原有的应用测试文件：

- `test_app_architecture.py` - 应用架构测试
- `test_app_basic.py` - 应用基础功能测试  
- `test_app_enhanced.py` - 应用增强功能测试
- `test_app_knowledge.py` - 应用知识提取测试
- `test_app_uml.py` - 应用UML生成测试

这些文件继续使用原有的测试框架，可以直接运行：
```bash
python -m pytest tests/ -v
```

## 测试运行建议

### 1. 快速验证
```bash
# 运行单元测试
python -m pytest tests/unit/ -v

# 运行集成测试
python -m pytest tests/integration/ -v
```

### 2. 完整测试套件
```bash
# 运行所有测试
python -m pytest tests/ -v

# 运行特定分类的所有测试
python -m pytest tests/unit/ tests/integration/ -v
```

### 3. LLM功能测试
```bash
# 需要配置有效的API密钥
python -m pytest tests/llm/ -v
```

## 注意事项

1. **LLM测试**需要配置有效的API密钥才能正常运行
2. **工作流测试**可能会消耗较多资源，建议在性能较好的环境中运行
3. **样例工程测试**依赖 `output/example/` 目录下的样例工程
4. 所有测试都需要在激活的虚拟环境中运行

通过这种分类结构，测试脚本更加清晰有序，便于维护和扩展。