# 全量代码资产AI扫描与语义建模智能体设计方案

> 文档版本：v2.0  
> 创建日期：2026-03-11  
> 状态：重新规划中（基于现状分析）

## 一、核心目标与输出定义

本智能体的核心目标是**将百万行级"黑盒"代码库转化为结构化、可检索、业务语义清晰的"白盒"数字资产**，为后续AI重构提供精准依据。

### 核心输出物

1. **代码资产全景图**：模块依赖图、调用链路图、技术债务热力图、代码复杂度分布图
2. **业务语义模型库**：逐函数/类的业务规则、输入输出契约、副作用、边界条件、业务标签
3. **风险与优先级清单**：死代码清单、循环依赖清单、安全漏洞清单、高频修改/bug模块清单、P0-P3风险分级台账
4. **业务-代码映射台账**：代码模块与业务域/限界上下文的绑定关系
5. **结构化元数据仓库**：存储所有代码元数据、语义向量、依赖关系的可查询数据库

---

## 二、智能体整体架构

采用**分层多Agent协作架构**，从底层代码解析到上层业务语义抽象，逐层递进。

```
┌─────────────────────────────────────────────────────────────┐
│                    代码仓库接入层                            │
│              (GitPython / 多平台支持)                        │
└─────────────────────┬───────────────────────────────────────┘
                      ▼
┌─────────────────────────────────────────────────────────────┐
│               代码解析与预处理Agent                          │
│     (Tree-sitter AST解析 / 复杂度计算 / 重复代码检测)         │
└─────────────────────┬───────────────────────────────────────┘
                      ▼
┌──────────────────┬──────────────────┬───────────────────────┐
│  语义理解Agent   │  依赖分析Agent   │   风险评估Agent        │
│ (业务语义提取)   │ (架构逆向)       │ (安全扫描/债务识别)    │
└────────┬─────────┴────────┬─────────┴──────────┬────────────┘
         │                  │                    │
         └──────────────────┼────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│               业务-代码映射Agent                             │
│           (领域聚类 / 文档关联)                              │
└─────────────────────┬───────────────────────────────────────┘
                      ▼
┌─────────────────────────────────────────────────────────────┐
│            可视化与报告生成Agent                             │
│       (Mermaid图 / HTML热力图 / Markdown报告)               │
└─────────────────────┬───────────────────────────────────────┘
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              元数据仓库与API层                               │
│         (SQLite / 向量数据库 / RESTful API)                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、模块设计详细说明

### 3.1 语义理解与业务抽象Agent (`semantic_extractor.py`)

**职责**：让AI"读懂"代码在做什么业务

**核心能力**：
- 逐函数/类生成业务语义摘要
- 提取业务规则契约：输入输出参数含义、边界条件、异常处理
- 识别副作用（写库、发消息、调用外部接口）
- 自动打业务领域标签

**数据结构**：

```python
@dataclass
class InputContract:
    """输入契约"""
    param_name: str
    business_meaning: str
    constraints: str
    is_required: bool = True

@dataclass
class OutputContract:
    """输出契约"""
    business_meaning: str
    data_structure: str
    possible_values: List[str] = field(default_factory=list)

@dataclass
class EnhancedBusinessSemantic:
    """增强业务语义模型"""
    entity_id: str
    entity_name: str
    entity_type: str
    file_path: str
    business_summary: str
    input_contract: List[InputContract]
    output_contract: OutputContract
    side_effects: List[str]
    business_rules: List[str]
    exception_scenarios: List[str]
    business_domain: str
    business_domain_tags: List[str]
    confidence: float
```

**LLM Prompt设计**：

```markdown
你是一位资深业务架构师，分析以下代码的业务语义，按JSON格式输出：

输入：
- 代码片段：{code_snippet}
- 类/函数名：{function_name}
- 相关注释：{comments}

输出JSON结构：
{
  "business_summary": "用1-2句话描述核心业务功能",
  "input_contract": [{"param_name": "xxx", "business_meaning": "xxx", "constraints": "xxx"}],
  "output_contract": {"business_meaning": "xxx", "data_structure": "xxx"},
  "side_effects": ["写数据库表", "调用外部接口"],
  "business_rules": ["规则1", "规则2"],
  "exception_scenarios": ["异常场景1"],
  "business_domain_tags": ["支付", "订单"]
}
```

---

### 3.2 代码资产可视化Agent (`code_asset_visualizer.py`)

**职责**：生成人类可读的代码资产全景图

**输出物**：

| 可视化类型 | 格式 | 用途 |
|-----------|------|------|
| 模块依赖图 | Mermaid/HTML | 展示模块间依赖关系 |
| 调用链路图 | JSON+Mermaid | 从入口到底层的调用路径 |
| 技术债务热力图 | HTML | 按模块显示债务密度 |
| 复杂度分布图 | HTML | 圈复杂度/认知复杂度分布 |

**技术选型**：
- Mermaid：嵌入式图表
- ECharts：交互式热力图

---

### 3.3 重复代码检测Agent (`duplicate_detector.py`)

**职责**：识别代码重复，减少维护成本

**检测类型**：
- 精确重复（100%相同）
- 近似重复（相似度>80%）
- 结构重复（AST结构相似）

**算法**：
- 基于Token序列的相似度匹配
- Jaccard相似系数计算
- 最长公共子序列(LCS)

**输出**：

```python
@dataclass
class DuplicatePair:
    """重复代码对"""
    entity1_id: str
    entity1_name: str
    entity1_file: str
    entity2_id: str
    entity2_name: str
    entity2_file: str
    similarity: float
    duplicate_type: str  # exact/approximate/structural
```

---

### 3.4 安全漏洞扫描Agent (`security_scanner.py`)

**职责**：识别代码中的安全隐患

**检测规则**：

| 漏洞类型 | CWE编号 | 检测模式 |
|---------|---------|---------|
| SQL注入 | CWE-89 | 字符串拼接SQL |
| 命令注入 | CWE-78 | os.system/subprocess |
| 路径遍历 | CWE-22 | 文件路径拼接 |
| XSS | CWE-79 | 未转义输出 |
| 硬编码密钥 | CWE-798 | API Key/密码明文 |
| 敏感信息泄露 | CWE-200 | 日志输出敏感数据 |

**风险分级**：
- Critical：可被直接利用
- High：可能导致数据泄露
- Medium：需要特定条件触发
- Low：潜在风险

---

### 3.5 元数据仓库 (`metadata_repository.py`)

**职责**：存储所有分析结果，提供查询API

**存储架构**：

```
┌─────────────────┐     ┌─────────────────┐
│    SQLite       │     │   向量数据库     │
│  (结构化元数据)  │     │ (语义向量存储)   │
└────────┬────────┘     └────────┬────────┘
         │                       │
         └───────────┬───────────┘
                     ▼
         ┌─────────────────────┐
         │    统一查询API       │
         │  - 按业务域查询      │
         │  - 按风险等级查询    │
         │  - 关键词搜索        │
         │  - 相似代码检索      │
         └─────────────────────┘
```

**表结构设计**：

```sql
-- 实体元数据表
CREATE TABLE entity_metadata (
    id TEXT PRIMARY KEY,
    name TEXT,
    type TEXT,
    file_path TEXT,
    business_domain TEXT,
    risk_level TEXT,
    business_summary TEXT,
    created_at TIMESTAMP
);

-- 模块元数据表
CREATE TABLE module_metadata (
    id TEXT PRIMARY KEY,
    name TEXT,
    path TEXT,
    entity_count INTEGER,
    avg_complexity REAL,
    debt_count INTEGER
);

-- 关系表
CREATE TABLE relationships (
    id TEXT PRIMARY KEY,
    source_id TEXT,
    target_id TEXT,
    relationship_type TEXT
);

-- 风险清单表
CREATE TABLE risk_registry (
    id TEXT PRIMARY KEY,
    entity_id TEXT,
    risk_type TEXT,
    severity TEXT,
    description TEXT,
    suggestion TEXT
);
```

---

## 四、工作流程

### 4.1 完整分析流程

```python
def analyze_full(self):
    # 1. 代码模块风险分级（风险评估Agent）
    results["risk_assessment"] = self.analyze_risk_levels()
    
    # 2. 业务语义逆向提取（语义理解Agent）
    results["business_semantics"] = self.extract_business_semantics_enhanced()
    
    # 3. 技术债务扫描（风险评估Agent）
    results["technical_debts"] = self.scan_technical_debts()
    
    # 4. 热点与风险代码识别（风险评估Agent）
    results["hotspots"] = self.identify_hotspots()
    
    # 5. 架构腐化诊断（依赖分析Agent）
    results["architecture_diagnosis"] = self.diagnose_architecture()
    
    # 6. 安全漏洞扫描（风险评估Agent）
    results["security_scan"] = self.scan_security_vulnerabilities()
    
    # 7. 重复代码检测（预处理Agent）
    results["duplicate_detection"] = self.detect_duplicate_code()
    
    # 8. 生成代码资产全景图（可视化Agent）
    results["visualization"] = self.generate_code_asset_visualization()
```

### 4.2 输出目录结构

```
output/refactoring/
├── analysis_result.json       # 完整分析结果
├── boundary_report.md         # 重构范围边界说明书
├── technical_debts.md         # 技术债务清单
├── security_report.md         # 安全漏洞报告
├── duplicate_report.json      # 重复代码报告
├── business_code_mapping.json # 业务-代码映射
└── metadata.json              # 元数据导出

output/visualization/
├── module_dependency.html     # 模块依赖图
├── call_chain_data.json       # 调用链路数据
├── debt_heatmap.html          # 技术债务热力图
└── complexity_distribution.html # 复杂度分布图
```

---

## 五、性能优化策略

### 5.1 百万行级代码处理

| 策略 | 说明 |
|------|------|
| 并行处理 | 按文件/模块切分，多进程并行解析 |
| 增量扫描 | 首次全量后，仅扫描Git变更文件 |
| LLM成本优化 | 小模型初筛，核心模块用大模型精处理 |
| 结果缓存 | 避免重复解析相同代码 |

### 5.2 长代码处理

- 按逻辑块切分（结合AST）
- 选用长上下文模型（Claude 3 Opus: 200K token）
- 摘要聚合：函数摘要 → 类摘要 → 模块摘要

---

## 六、质量指标

| 指标 | 目标值 |
|------|--------|
| 语义理解准确率 | P0核心模块≥95%，其他≥85% |
| 扫描性能 | 百万行≤24小时 |
| 风险识别覆盖率 | ≥90% |
| 业务映射准确率 | ≥90% |

---

## 七、后续迭代计划

### Phase 1: MVP验证（已完成）
- ✅ 单语言（Python）全流程
- ✅ 基础语义提取
- ✅ 可视化报告生成

### Phase 2: 核心能力完善
- [ ] 多语言AST解析（Java/Go/JS）
- [ ] 向量数据库集成
- [ ] 增量扫描支持

### Phase 3: 工程化
- [ ] 并行处理框架
- [ ] 元数据仓库API服务化
- [ ] 与重构智能体对接

---

## 八、相关文件

| 文件 | 说明 |
|------|------|
| `src/core/semantic_extractor.py` | 语义理解Agent实现 |
| `src/core/code_asset_visualizer.py` | 可视化Agent实现 |
| `src/core/duplicate_detector.py` | 重复代码检测实现 |
| `src/core/security_scanner.py` | 安全扫描实现 |
| `src/core/metadata_repository.py` | 元数据仓库实现 |
| `src/core/refactoring_analyzer.py` | 主分析器（集成所有Agent） |
