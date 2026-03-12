---
type: skill
version: '1.0'
category: entities
entity_type: method
entity_id: a263c8bdff1471da
signature: 5e9330079b5b5816b97edbbeeb90e47d
created: '2026-03-11T10:11:01.453403'
file_path: src\gitnexus\gitnexus_client.py
start_line: 173
end_line: 192
lines_of_code: 20
tags:
- entity
- method
- analyze_incremental
related_entities: []
dependencies:
  out: []
  in: []
metrics:
  complexity: null
  param_count: 3
---

# analyze_incremental

> **类型**: `EntityType.METHOD` | **文件**: `src\gitnexus\gitnexus_client.py` | **行数**: 173-192 (20行)

## 📋 概述

**说明**:

```
增量分析项目
```

## 📥 参数

| 名称 | 类型 | 说明 |
|------|------|------|
| `self` | `any` | - |
| `base_commit` | `any` | - |
| `head_commit` | `any` | - |

## 📤 返回值

**类型**: `KnowledgeGraph`

## 💻 代码实现

```python
    def analyze_incremental(self, base_commit: Optional[str] = None, head_commit: Optional[str] = None) -> KnowledgeGraph:
        """增量分析项目"""
        changed_files = self.incremental_analyzer.get_changed_files(base_commit, head_commit)
        print(f"检测到 {len(changed_files)} 个变更文件")
        
        results = []
        for file_path in tqdm(changed_files, desc="增量分析进度"):
            full_path = self.root_dir / file_path
            if full_path.exists() and full_path.is_file():
                result = self._analyze_single_file(full_path, force=True)
                if result:
                    results.append(result)
        
        # 合并到现有知识图谱
        existing_graph = self._load_knowledge_graph()
        new_graph = KnowledgeGraph.build_from_results(results)
        merged_graph = existing_graph.merge(new_graph)
        
        self._save_knowledge_graph(merged_graph)
        return merged_graph
```

---
## 🤖 AI智能体指南

### 快速定位
- **文件路径**: `src\gitnexus\gitnexus_client.py`
- **起始行号**: 173
- **搜索关键词**: `analyze_incremental`

### 签名追踪
- **签名**: `5e9330079b5b5816...`

---
*由知识提取智能体自动生成 | Skill格式 v1.0*
