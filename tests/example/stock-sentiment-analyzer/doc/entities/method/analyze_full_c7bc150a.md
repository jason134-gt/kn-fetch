---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: c7bc150a9f8e59b4
signature: 1378d39ddcafabc13d959599e7d6afeb
created: '2026-03-11T10:11:01.450075'
file_path: src\gitnexus\gitnexus_client.py
start_line: 153
end_line: 171
lines_of_code: 19
tags:
- entity
- method
- analyze_full
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 2
---

# analyze_full

> **类型**: `EntityType.METHOD` | **文件**: `src\gitnexus\gitnexus_client.py` | **行数**: 153-171 (19行)

## 📋 概述

**说明**:

```
全量分析项目
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `force` | `any` | - |

## 📤 返回值

**类型**: `KnowledgeGraph`

## 💻 代码实现

```python
    def analyze_full(self, force: bool = False) -> KnowledgeGraph:
        """全量分析项目"""
        all_files = self._get_all_target_files()
        print(f"开始全量分析，共 {len(all_files)} 个文件")
        
        results = []
        with ProcessPoolExecutor(max_workers=self.max_workers) as executor:
            futures = [executor.submit(self._analyze_single_file, file, force) for file in all_files]
            
            for future in tqdm(as_completed(futures), total=len(futures), desc="分析进度"):
                result = future.result()
                if result:
                    results.append(result)
        
        # 构建知识图谱
        graph = KnowledgeGraph.build_from_results(results)
        self._save_knowledge_graph(graph)
        
        return graph
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\gitnexus_client.py`
- **起始行号**: 153
- **搜索关键词**: `analyze_full`

### 签名追踪
- **签名**: `1378d39ddcafabc1...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
