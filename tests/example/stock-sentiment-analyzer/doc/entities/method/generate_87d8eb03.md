---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: 87d8eb0304f98c64
signature: 0aa19f77737f8c00e5a44cb368243396
created: '2026-03-11T10:11:01.503020'
file_path: src\reporter\report_generator.py
start_line: 32
end_line: 101
lines_of_code: 70
tags:
- entity
- method
- generate
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 5
---

# generate

> **类型**: `EntityType.METHOD` | **文件**: `src\reporter\report_generator.py` | **行数**: 32-101 (70行)

## 📋 概述

**说明**:

```
生成分析报告

Args:
    news_list: 资讯列表
    expert_results: 专家评估结果
    aggregated_results: 汇总结果
    trading_decisions: 交易决策

Returns:
    报告信息
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `news_list` | `any` | - |
| `expert_results` | `any` | - |
| `aggregated_results` | `any` | - |
| `trading_decisions` | `any` | - |

## 📤 返回值

**类型**: `Dict[str, Any]`

## 💻 代码实现

```python
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
            file_extension = '.json'
        else:
            content = self._generate_markdown(
                timestamp,
                news_list,
                expert_results,
                aggregated_results,
                trading_decisions
            )
            file_extension = '.md'

        # 保存报告
        file_name = f"report_{report_id}{file_extension}"
        file_path = self.output_dir / file_name

        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

        self.logger.info(f"报告已保存: {file_path}")

        return {
            'report_id': report_id,
            'timestamp': timestamp,
            'file_path': str(file_path),
            'format': self.output_format,
            'total_news': len(news_list),
            'valid_news': len(aggregated_results)
        }
```

## 🔧 重构建议

- 方法过长 (70 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\reporter\report_generator.py`
- **起始行号**: 32
- **搜索关键词**: `generate`

### 签名追踪
- **签名**: `0aa19f77737f8c00...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
