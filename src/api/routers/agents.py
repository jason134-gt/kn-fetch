"""
Agent管理API路由 - Agent状态监控和配置管理
"""

from fastapi import APIRouter, HTTPException
from typing import Dict, Any, List
from enum import Enum

router = APIRouter()


class AgentStatus(Enum):
    """Agent状态枚举"""
    IDLE = "idle"
    RUNNING = "running" 
    COMPLETED = "completed"
    FAILED = "failed"
    DISABLED = "disabled"


class AgentType(Enum):
    """Agent类型枚举"""
    FILE_SCANNER = "file_scanner"
    CODE_PARSER = "code_parser"
    SEMANTIC_EXTRACTOR = "semantic_extractor"
    ARCHITECTURE_ANALYZER = "architecture_analyzer"
    BUSINESS_LOGIC = "business_logic"
    DOCUMENTATION = "documentation"
    QUALITY_ASSESSOR = "quality_assessor"


# 模拟Agent状态存储（实际应用中应该使用数据库）
agents_status = {
    "file_scanner": {
        "name": "文件扫描Agent",
        "type": AgentType.FILE_SCANNER,
        "status": AgentStatus.IDLE,
        "last_execution": None,
        "metrics": {
            "files_processed": 0,
            "processing_time": 0.0,
            "error_count": 0
        },
        "config": {
            "supported_extensions": [".py", ".js", ".ts", ".java", ".cpp", ".c", ".h"],
            "ignore_patterns": ["**/__pycache__/**", "**/.git/**", "**/node_modules/**"],
            "max_file_size": 10485760
        }
    },
    "code_parser": {
        "name": "代码解析Agent",
        "type": AgentType.CODE_PARSER,
        "status": AgentStatus.IDLE,
        "last_execution": None,
        "metrics": {
            "files_parsed": 0,
            "entities_extracted": 0,
            "parsing_time": 0.0
        },
        "config": {
            "enable_ast_parsing": True,
            "enable_complexity_analysis": True,
            "max_file_size": 5242880
        }
    },
    "semantic_extractor": {
        "name": "语义提取Agent",
        "type": AgentType.SEMANTIC_EXTRACTOR,
        "status": AgentStatus.IDLE,
        "last_execution": None,
        "metrics": {
            "semantic_entities": 0,
            "extraction_time": 0.0,
            "llm_calls": 0
        },
        "config": {
            "enable_llm": True,
            "max_context_length": 4000,
            "semantic_threshold": 0.7
        }
    },
    "architecture_analyzer": {
        "name": "架构分析Agent",
        "type": AgentType.ARCHITECTURE_ANALYZER,
        "status": AgentStatus.IDLE,
        "last_execution": None,
        "metrics": {
            "dependencies_found": 0,
            "patterns_identified": 0,
            "risk_assessments": 0
        },
        "config": {
            "detect_design_patterns": True,
            "risk_assessment_threshold": 0.7,
            "enable_visualization": True
        }
    },
    "business_logic": {
        "name": "业务逻辑Agent",
        "type": AgentType.BUSINESS_LOGIC,
        "status": AgentStatus.IDLE,
        "last_execution": None,
        "metrics": {
            "business_flows": 0,
            "domain_entities": 0,
            "cross_domain_links": 0
        },
        "config": {
            "domain_classification": True,
            "cross_domain_analysis": True,
            "business_flow_min_complexity": 0.3
        }
    },
    "documentation": {
        "name": "文档生成Agent",
        "type": AgentType.DOCUMENTATION,
        "status": AgentStatus.IDLE,
        "last_execution": None,
        "metrics": {
            "documents_generated": 0,
            "document_size": 0,
            "generation_time": 0.0
        },
        "config": {
            "output_dir": "docs",
            "generate_knowledge_graph": True,
            "template_type": "standard"
        }
    },
    "quality_assessor": {
        "name": "质量评估Agent",
        "type": AgentType.QUALITY_ASSESSOR,
        "status": AgentStatus.IDLE,
        "last_execution": None,
        "metrics": {
            "quality_assessments": 0,
            "refactoring_suggestions": 0,
            "complexity_reports": 0
        },
        "config": {
            "enable_test_generation": True,
            "complexity_threshold": 10,
            "quality_metrics": ["cyclomatic", "cognitive", "maintainability"]
        }
    }
}


@router.get("/agents")
async def list_agents() -> Dict[str, Any]:
    """列出所有Agent及其状态"""
    
    return {
        "agents": [
            {
                "id": agent_id,
                "name": agent_info["name"],
                "type": agent_info["type"].value,
                "status": agent_info["status"].value,
                "last_execution": agent_info["last_execution"],
                "metrics": agent_info["metrics"]
            }
            for agent_id, agent_info in agents_status.items()
        ],
        "total_count": len(agents_status)
    }


@router.get("/agents/{agent_id}")
async def get_agent_status(agent_id: str) -> Dict[str, Any]:
    """获取指定Agent的详细状态"""
    
    if agent_id not in agents_status:
        raise HTTPException(status_code=404, detail=f"Agent {agent_id} 不存在")
    
    agent_info = agents_status[agent_id]
    
    return {
        "id": agent_id,
        "name": agent_info["name"],
        "type": agent_info["type"].value,
        "status": agent_info["status"].value,
        "last_execution": agent_info["last_execution"],
        "metrics": agent_info["metrics"],
        "config": agent_info["config"]
    }


@router.post("/agents/{agent_id}/start")
async def start_agent(agent_id: str) -> Dict[str, Any]:
    """启动指定Agent"""
    
    if agent_id not in agents_status:
        raise HTTPException(status_code=404, detail=f"Agent {agent_id} 不存在")
    
    agent_info = agents_status[agent_id]
    
    if agent_info["status"] == AgentStatus.RUNNING:
        raise HTTPException(status_code=400, detail=f"Agent {agent_id} 已在运行中")
    
    # 模拟启动过程
    agent_info["status"] = AgentStatus.RUNNING
    
    return {
        "message": f"Agent {agent_id} 已启动",
        "agent_id": agent_id,
        "status": agent_info["status"].value
    }


@router.post("/agents/{agent_id}/stop")
async def stop_agent(agent_id: str) -> Dict[str, Any]:
    """停止指定Agent"""
    
    if agent_id not in agents_status:
        raise HTTPException(status_code=404, detail=f"Agent {agent_id} 不存在")
    
    agent_info = agents_status[agent_id]
    
    if agent_info["status"] != AgentStatus.RUNNING:
        raise HTTPException(status_code=400, detail=f"Agent {agent_id} 未在运行中")
    
    # 模拟停止过程
    agent_info["status"] = AgentStatus.IDLE
    
    return {
        "message": f"Agent {agent_id} 已停止",
        "agent_id": agent_id,
        "status": agent_info["status"].value
    }


@router.put("/agents/{agent_id}/config")
async def update_agent_config(agent_id: str, config_update: Dict[str, Any]) -> Dict[str, Any]:
    """更新Agent配置"""
    
    if agent_id not in agents_status:
        raise HTTPException(status_code=404, detail=f"Agent {agent_id} 不存在")
    
    agent_info = agents_status[agent_id]
    
    # 更新配置
    for key, value in config_update.items():
        if key in agent_info["config"]:
            agent_info["config"][key] = value
    
    return {
        "message": f"Agent {agent_id} 配置已更新",
        "agent_id": agent_id,
        "updated_config": agent_info["config"]
    }


@router.get("/agents/{agent_id}/metrics")
async def get_agent_metrics(agent_id: str) -> Dict[str, Any]:
    """获取Agent性能指标"""
    
    if agent_id not in agents_status:
        raise HTTPException(status_code=404, detail=f"Agent {agent_id} 不存在")
    
    agent_info = agents_status[agent_id]
    
    return {
        "agent_id": agent_id,
        "name": agent_info["name"],
        "metrics": agent_info["metrics"],
        "last_updated": agent_info["last_execution"]
    }


@router.post("/agents/batch-start")
async def batch_start_agents(agent_ids: List[str]) -> Dict[str, Any]:
    """批量启动多个Agent"""
    
    results = []
    
    for agent_id in agent_ids:
        if agent_id in agents_status:
            agent_info = agents_status[agent_id]
            if agent_info["status"] != AgentStatus.RUNNING:
                agent_info["status"] = AgentStatus.RUNNING
                results.append({
                    "agent_id": agent_id,
                    "status": "started"
                })
            else:
                results.append({
                    "agent_id": agent_id,
                    "status": "already_running"
                })
        else:
            results.append({
                "agent_id": agent_id,
                "status": "not_found"
            })
    
    return {
        "message": "批量启动操作完成",
        "results": results
    }


@router.get("/agents/overall-status")
async def get_overall_status() -> Dict[str, Any]:
    """获取整体Agent集群状态"""
    
    status_counts = {
        "idle": 0,
        "running": 0,
        "completed": 0,
        "failed": 0,
        "disabled": 0
    }
    
    for agent_info in agents_status.values():
        status_counts[agent_info["status"].value] += 1
    
    total_agents = len(agents_status)
    available_agents = status_counts["idle"] + status_counts["running"]
    
    return {
        "total_agents": total_agents,
        "available_agents": available_agents,
        "status_distribution": status_counts,
        "health_score": (available_agents / total_agents) * 100 if total_agents > 0 else 0
    }