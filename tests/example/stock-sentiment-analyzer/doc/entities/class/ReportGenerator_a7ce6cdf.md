---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: a7ce6cdf28bf5484
signature: 35325fbf21fb1c5dfb66432cc05c1540
created: '2026-03-11T10:11:01.249312'
file_path: src\reporter\report_generator.py
start_line: 12
end_line: 290
lines_of_code: 279
tags:
- entity
- class
- ReportGenerator
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# ReportGenerator

> **类型**: `EntityType.CLASS` | **文件**: `src\reporter\report_generator.py` | **行数**: 12-290 (279行)

## 📋 概述

**说明**:

```
报告生成器
```

## 💻 代码片段（节选）

```python
class ReportGenerator:
    """报告生成器"""

    def __init__(self, config: Dict[str, Any]):
        """初始化报告生成器

        Args:
            config: 配置字典
        """
        self.config = config
        self.logger = logging.getLogger(__name__)

        # 获取报告配置
        self.reporter_config = config.get('reporter', {})
        self.output_dir = Path(self.reporter_config.get('output_dir', './data/reports'))
        self.output_format = self.reporter_config.get('output_format', 'markdown')

        # 确保输出目录存在
        self.output_dir.mkdir(parents=True, exist_ok=True)

    async def generate(
        self,
        news_list: List[Dict[str, Any]],
        expert_results: List[Dict[str, Any]],
        aggregated_results: List[Dict[str, Any]],
        trading_decisions: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """生成分析报告

        Args:
            news_list: 资讯列表
            expert_results: 专家评估结果
            aggregated_results: 汇总结果
            trading_decisions: 交易决策

        Returns:
            报告信息
        """
        self.logger.info("开始生成报告")

        # 生成报告时间戳
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        report_id = datetime.now().strftime('%Y%m%d_%H%M%S')

        # 生成报告内容
        if self.output_format == 'markdown':
            content = self._generate_markdown(
                timestamp,
                news_list,
                expert_results,
                aggregated_results,
                trading_decisions
            )
            file_extension = '.md'
        elif self.output_format == 'json':
            content = self._generate_json(
                timestamp,
                news_list,
                expert_results,
                aggregated_results,
                trading_decisions
            )
            file_e
// ... 省略 7654 字符 ...
```

## 🔧 重构建议

- 方法过长 (279 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\reporter\report_generator.py`
- **起始行号**: 12
- **搜索关键词**: `ReportGenerator`

### 签名追踪
- **签名**: `35325fbf21fb1c5d...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
