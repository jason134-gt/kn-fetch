# API 接口文档

## 目录
- [1. 概述](#1-概述)
- [2. 状态接口](#2-状态接口)
- [3. AI 配置接口](#3-ai-配置接口)
- [4. 分析接口](#4-分析接口)
- [5. 知识接口](#5-知识接口)
- [6. 架构分析接口](#6-架构分析接口)
- [7. UML 接口](#7-uml-接口)
- [8. 增强分析接口](#8-增强分析接口)
- [9. 错误码说明](#9-错误码说明)

---

## 1. 概述

### 1.1 基础信息

- **Base URL**: `http://localhost:8000`
- **Content-Type**: `application/json`
- **认证方式**: 当前版本无需认证

### 1.2 通用响应格式

#### 成功响应
```json
{
  "status": "success",
  "data": { ... },
  "message": "操作成功"
}
```

#### 错误响应
```json
{
  "detail": "错误描述信息"
}
```

---

## 2. 状态接口

### 2.1 获取应用状态

**接口**: `GET /status`

**描述**: 获取当前应用运行状态

**响应示例**:
```json
{
  "status": "running",
  "version": "1.0.0",
  "tasks": {
    "running": false,
    "progress": 0,
    "current_step": ""
  }
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| status | string | 应用状态：running, idle |
| version | string | 版本号 |
| tasks | object | 任务状态 |
| tasks.running | boolean | 是否有任务在运行 |
| tasks.progress | number | 任务进度 (0-100) |
| tasks.current_step | string | 当前步骤描述 |

---

## 3. AI 配置接口

### 3.1 获取 AI 配置

**接口**: `GET /api/ai-config`

**描述**: 获取当前 AI 配置信息

**响应示例**:
```json
{
  "provider": "volcengine",
  "models": {
    "volcengine": "deepseek-v3-2-251201",
    "openai": "gpt-4o",
    "anthropic": "claude-3-opus-20240229"
  },
  "settings": {
    "temperature": 0.1,
    "max_tokens": 4000
  },
  "available": true
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| provider | string | 当前使用的提供商 |
| models | object | 各提供商的模型配置 |
| settings | object | 通用设置 |
| available | boolean | LLM 是否可用 |

### 3.2 更新 AI 配置

**接口**: `POST /api/ai-config`

**描述**: 更新 AI 配置

**请求体**:
```json
{
  "provider": "volcengine",
  "model": "deepseek-v3-2-251201",
  "temperature": 0.1,
  "max_tokens": 4000
}
```

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| provider | string | 是 | LLM 提供商 |
| model | string | 是 | 模型名称 |
| temperature | number | 否 | 温度参数 (0-1) |
| max_tokens | number | 否 | 最大 Token 数 |

**响应示例**:
```json
{
  "status": "success",
  "message": "AI 配置已更新"
}
```

### 3.3 测试 AI 连接

**接口**: `POST /api/ai-test`

**描述**: 测试 AI 连接是否正常

**响应示例**:
```json
{
  "status": "success",
  "message": "连接成功",
  "response": "连接成功",
  "latency_ms": 1250
}
```

**错误响应示例**:
```json
{
  "detail": "AI 连接失败：API Key 无效"
}
```

---

## 4. 分析接口

### 4.1 启动分析任务

**接口**: `POST /api/analysis`

**描述**: 启动代码和文档分析任务

**请求体**:
```json
{
  "directory": "./src",
  "include_code": true,
  "include_docs": true,
  "large_scale": false,
  "incremental": false,
  "force": false
}
```

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| directory | string | 是 | 分析目录路径 |
| include_code | boolean | 否 | 是否包含代码分析 |
| include_docs | boolean | 否 | 是否包含文档分析 |
| large_scale | boolean | 否 | 是否启用大规模处理 |
| incremental | boolean | 否 | 是否增量分析 |
| force | boolean | 否 | 是否强制重新分析 |

**响应示例**:
```json
{
  "status": "success",
  "message": "分析任务已启动",
  "task_id": "analysis_1234567890"
}
```

### 4.2 获取分析状态

**接口**: `GET /api/analysis/status`

**描述**: 获取当前分析任务的进度

**响应示例**:
```json
{
  "status": "running",
  "progress": 45,
  "current_step": "分析类和函数",
  "total_files": 150,
  "processed_files": 67
}
```

### 4.3 获取分析结果

**接口**: `GET /api/analysis/result`

**描述**: 获取分析结果摘要

**响应示例**:
```json
{
  "entities": {
    "total": 245,
    "by_type": {
      "class": 45,
      "function": 120,
      "method": 80
    }
  },
  "relationships": {
    "total": 380,
    "by_type": {
      "calls": 200,
      "extends": 35,
      "implements": 25
    }
  },
  "files": 67
}
```

---

## 5. 知识接口

### 5.1 获取实体列表

**接口**: `GET /api/entities`

**描述**: 获取知识图谱中的实体列表

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| entity_type | string | 否 | 实体类型过滤 (class, function, method) |
| limit | number | 否 | 返回数量限制 |
| offset | number | 否 | 偏移量 |

**请求示例**:
```
GET /api/entities?entity_type=class&limit=20&offset=0
```

**响应示例**:
```json
{
  "entities": [
    {
      "id": "class_User_py",
      "name": "User",
      "entity_type": "class",
      "file_path": "src/models/user.py",
      "lines": 150,
      "docstring": "用户模型类"
    }
  ],
  "total": 45,
  "page": 1,
  "page_size": 20
}
```

### 5.2 获取知识分析结果

**接口**: `GET /api/knowledge-result`

**描述**: 获取完整的知识分析结果

**响应示例**:
```json
{
  "entities_by_file": {
    "src/models/user.py": [
      {
        "id": "class_User_py",
        "name": "User",
        "file": "src/models/user.py",
        "lines": 150,
        "docstring": "用户模型类"
      }
    ]
  },
  "key_entities": {
    "classes": [],
    "functions": [],
    "methods": []
  },
  "relationship_summary": {
    "extends": 35,
    "implements": 25,
    "calls": 200
  }
}
```

### 5.3 获取实体详细信息

**接口**: `GET /api/entity/{entity_id}`

**描述**: 获取指定实体的详细信息

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| entity_id | string | 实体 ID |

**响应示例**:
```json
{
  "entity": {
    "id": "class_User_py",
    "name": "User",
    "entity_type": "class",
    "file_path": "src/models/user.py",
    "line_number": 15,
    "lines_of_code": 150,
    "docstring": "用户模型类",
    "parameters": [],
    "modifiers": ["public"]
  },
  "relationships": [
    {
      "id": "rel_1",
      "source_id": "class_User_py",
      "target_id": "class_Entity_py",
      "type": "extends"
    }
  ],
  "neighbors": [
    {
      "id": "class_Entity_py",
      "name": "Entity",
      "relation_type": "extends"
    }
  ]
}
```

### 5.4 验证知识

**接口**: `POST /api/validate`

**描述**: 验证知识提取的准确性

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| source_directory | string | 是 | 源代码目录 |

**请求示例**:
```
POST /api/validate?source_directory=./src
```

**响应示例**:
```json
{
  "accuracy": 0.92,
  "total_entities": 245,
  "validated_entities": 225,
  "issues": [
    {
      "entity_id": "class_Unknown_py",
      "issue_type": "missing_docstring",
      "description": "缺少文档字符串"
    }
  ]
}
```

### 5.5 导出知识

**接口**: `POST /api/export`

**描述**: 导出知识图谱

**请求体**:
```json
{
  "output_path": "./output/knowledge_graph",
  "format": "markdown"
}
```

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| output_path | string | 是 | 输出路径 |
| format | string | 否 | 导出格式：markdown, json, yaml |

**响应示例**:
```json
{
  "status": "success",
  "output_path": "./output/knowledge_graph",
  "files": [
    "project_overview.md",
    "architecture.md",
    "api_reference.md"
  ]
}
```

### 5.6 优化知识图谱

**接口**: `POST /api/optimize`

**描述**: 使用 LLM 优化知识图谱

**请求体**:
```json
{
  "level": "medium"
}
```

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| level | string | 否 | 优化级别：low, medium, high |

**响应示例**:
```json
{
  "status": "success",
  "message": "知识图谱优化完成",
  "optimized_entities": 45,
  "fixed_relationships": 12
}
```

---

## 6. 架构分析接口

### 6.1 架构分析

**接口**: `GET /api/architecture/analysis`

**描述**: 获取项目的架构分析结果

**响应示例**:
```json
{
  "layers": [
    {
      "name": "Presentation Layer",
      "components": ["Controller", "View"],
      "count": 25
    },
    {
      "name": "Business Layer",
      "components": ["Service", "Model"],
      "count": 45
    }
  ],
  "components": [
    {
      "name": "UserService",
      "layer": "Business Layer",
      "dependencies": ["UserRepository"]
    }
  ],
  "dependencies": [
    {
      "source": "UserService",
      "target": "UserRepository",
      "type": "uses"
    }
  ]
}
```

### 6.2 架构模式检测

**接口**: `GET /api/architecture/patterns`

**描述**: 检测项目使用的架构模式

**响应示例**:
```json
{
  "patterns": [
    {
      "name": "MVC",
      "description": "Model-View-Controller 架构模式",
      "confidence": 0.85,
      "components": {
        "models": 15,
        "views": 10,
        "controllers": 8
      }
    },
    {
      "name": "Repository Pattern",
      "description": "仓库模式",
      "confidence": 0.72,
      "components": 6
    }
  ]
}
```

### 6.3 架构质量评估

**接口**: `GET /api/architecture/quality`

**描述**: 评估项目架构质量

**响应示例**:
```json
{
  "quality_metrics": {
    "coupling": {
      "score": 0.65,
      "level": "moderate"
    },
    "cohesion": {
      "score": 0.78,
      "level": "good"
    },
    "complexity": {
      "score": 0.55,
      "level": "moderate"
    }
  },
  "overall_score": 0.66,
  "recommendations": [
    "降低模块间耦合",
    "提高代码复用性"
  ]
}
```

### 6.4 架构改进建议

**接口**: `GET /api/architecture/recommendations`

**描述**: 获取架构改进建议

**响应示例**:
```json
{
  "recommendations": [
    {
      "type": "refactoring",
      "priority": "high",
      "description": "UserService 类过于复杂，建议拆分",
      "suggestion": "将 UserService 拆分为 UserQueryService 和 UserCommandService",
      "affected_files": ["src/services/user_service.py"]
    }
  ]
}
```

### 6.5 架构可视化数据

**接口**: `GET /api/architecture/visualization`

**描述**: 获取架构可视化所需的数据

**响应示例**:
```json
{
  "nodes": [
    {
      "id": "UserService",
      "label": "UserService",
      "type": "service",
      "group": "Business Layer"
    }
  ],
  "edges": [
    {
      "source": "UserService",
      "target": "UserRepository",
      "label": "uses",
      "type": "dependency"
    }
  ]
}
```

---

## 7. UML 接口

### 7.1 生成 UML 图

**接口**: `POST /api/uml/generate`

**描述**: 生成项目的 UML 图

**请求体**:
```json
{
  "diagram_type": "class",
  "format": "plantuml",
  "include_details": true
}
```

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| diagram_type | string | 否 | 图类型：class, sequence, component |
| format | string | 否 | 格式：plantuml, mermaid |
| include_details | boolean | 否 | 是否包含详细信息 |

**响应示例**:
```json
{
  "status": "success",
  "uml_code": "@startuml\nclass User {\n  +id: int\n  +name: string\n}\n@enduml",
  "output_path": "./output/uml/class_diagram.puml"
}
```

### 7.2 获取 UML 图列表

**接口**: `GET /api/uml`

**描述**: 获取已生成的 UML 图列表

**响应示例**:
```json
{
  "diagrams": [
    {
      "id": "uml_001",
      "type": "class",
      "format": "plantuml",
      "file_path": "./output/uml/class_diagram.puml",
      "created_at": "2024-01-15T10:30:00Z"
    }
  ]
}
```

### 7.3 获取 UML 图状态

**接口**: `GET /api/uml/{uml_id}/status`

**描述**: 获取 UML 图生成状态

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| uml_id | string | UML 图 ID |

**响应示例**:
```json
{
  "status": "completed",
  "progress": 100,
  "output_path": "./output/uml/class_diagram.puml"
}
```

---

## 8. 增强分析接口

### 8.1 启动增强分析

**接口**: `POST /api/analysis/enhanced`

**描述**: 启动深度增强分析任务

**请求体**:
```json
{
  "command": "analyze_complexity",
  "parameters": {
    "depth": 2,
    "include_metrics": true
  }
}
```

**支持的命令**:
- `analyze_complexity`: 复杂度分析
- `analyze_dependencies`: 依赖分析
- `analyze_structure`: 结构分析
- `analyze_performance`: 性能分析
- `optimize_code`: 代码优化

**响应示例**:
```json
{
  "status": "accepted",
  "analysis_id": "enhanced_analysis_001",
  "command": "analyze_complexity"
}
```

### 8.2 获取增强分析状态

**接口**: `GET /api/analysis/enhanced/{analysis_id}/status`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| analysis_id | string | 分析任务 ID |

**响应示例**:
```json
{
  "status": "running",
  "progress": 60,
  "current_step": "计算圈复杂度",
  "estimated_time_remaining": 30
}
```

### 8.3 获取增强分析结果

**接口**: `GET /api/analysis/enhanced/{analysis_id}/result`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| analysis_id | string | 分析任务 ID |

**响应示例**:
```json
{
  "analysis_id": "enhanced_analysis_001",
  "command": "analyze_complexity",
  "result": {
    "overall_complexity": 0.65,
    "by_module": {
      "src/services": 0.72,
      "src/models": 0.45
    },
    "recommendations": [
      "简化 UserService 类的复杂度"
    ]
  }
}
```

### 8.4 获取增强分析列表

**接口**: `GET /api/analysis/enhanced`

**响应示例**:
```json
{
  "analyses": [
    {
      "id": "enhanced_analysis_001",
      "command": "analyze_complexity",
      "status": "completed",
      "created_at": "2024-01-15T10:30:00Z"
    }
  ]
}
```

### 8.5 取消增强分析

**接口**: `DELETE /api/analysis/enhanced/{analysis_id}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| analysis_id | string | 分析任务 ID |

**响应示例**:
```json
{
  "status": "cancelled",
  "analysis_id": "enhanced_analysis_001",
  "message": "分析任务已取消"
}
```

---

## 9. 错误码说明

### 9.1 HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 9.2 错误响应示例

#### 参数错误 (400)
```json
{
  "detail": "参数 'directory' 不能为空"
}
```

#### 资源不存在 (404)
```json
{
  "detail": "实体 'class_Unknown_py' 不存在"
}
```

#### 服务器错误 (500)
```json
{
  "detail": "分析过程中发生错误：文件读取失败"
}
```

---

## 附录

### A. 使用示例

#### Python 示例
```python
import requests

# 1. 获取应用状态
response = requests.get("http://localhost:8000/status")
status = response.json()

# 2. 启动分析
analysis_data = {
    "directory": "./src",
    "include_code": True,
    "include_docs": True
}
response = requests.post("http://localhost:8000/api/analysis", json=analysis_data)
result = response.json()

# 3. 获取实体列表
response = requests.get("http://localhost:8000/api/entities?entity_type=class")
entities = response.json()
```

#### cURL 示例
```bash
# 启动分析
curl -X POST http://localhost:8000/api/analysis \
  -H "Content-Type: application/json" \
  -d '{"directory": "./src"}'

# 获取实体列表
curl http://localhost:8000/api/entities?entity_type=class
```

### B. 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0.0 | 2024-01-15 | 初始版本 |
