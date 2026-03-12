---
type: skill
version: '1.0'
category: entities
entity_type: class
entity_id: 7016aedfbe3ce103
signature: c5a528f0fecb3de675007ad20840e1bf
created: '2026-03-11T10:11:01.257554'
file_path: src\utils\config_loader.py
start_line: 11
end_line: 104
lines_of_code: 94
tags:
- entity
- class
- ConfigLoader
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 0
---

# ConfigLoader

> **类型**: `EntityType.CLASS` | **文件**: `src\utils\config_loader.py` | **行数**: 11-104 (94行)

## 📋 概述

**说明**:

```
配置加载器
```

## 💻 代码片段（节选）

```python
class ConfigLoader:
    """配置加载器"""

    @staticmethod
    def load(config_path: str) -> Dict[str, Any]:
        """加载配置文件

        Args:
            config_path: 配置文件路径

        Returns:
            配置字典
        """
        logger = logging.getLogger(__name__)

        config_file = Path(config_path)

        if not config_file.exists():
            logger.warning(f"配置文件不存在: {config_path}，使用默认配置")
            return ConfigLoader._get_default_config()

        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)

            logger.info(f"配置文件加载成功: {config_path}")
            return config

        except Exception as e:
            logger.error(f"配置文件加载失败: {e}，使用默认配置")
            return ConfigLoader._get_default_config()

    @staticmethod
    def _get_default_config() -> Dict[str, Any]:
        """获取默认配置
        
        Returns:
            默认配置字典
        """
        return {
            'data_sources': {
                'official': [],
                'media': [],
                'platforms': []
            },
            'expert_weights': {
                'macro_economy': 0.25,
                'industry_research': 0.25,
                'technical_analysis': 0.20,
                'capital_flow': 0.20,
                'risk_control': 0.10
            },
            'collector': {
                'interval_minutes': 30,
                'batch_size': 10,
                'max_items_per_source': 20,
                'timeout_seconds': 30
            },
            'analyzer': {
                'min_confidence': 0.6,
                'min_impact_score': 0.3,
                'short_term_days': 5
            },
            'trader': {
                'min_volume': 100000000,
                'max_volume': 10000000000
// ... 省略 908 字符 ...
```

## 🔧 重构建议

- 方法过长 (94 行)，建议拆分

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\utils\config_loader.py`
- **起始行号**: 11
- **搜索关键词**: `ConfigLoader`

### 签名追踪
- **签名**: `c5a528f0fecb3de6...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
