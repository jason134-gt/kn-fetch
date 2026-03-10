from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel
from typing import Optional
from pathlib import Path
import asyncio

from starlette.requests import Request

# 获取项目根目录
PROJECT_ROOT = Path(__file__).parent.parent.parent
CONFIG_PATH = PROJECT_ROOT / "config" / "config.yaml"

app = FastAPI(title="知识提取智能体管理界面")

# 模板和静态文件配置
templates = Jinja2Templates(directory=Path(__file__).parent / "templates")
static_dir = Path(__file__).parent / "static"
static_dir.mkdir(exist_ok=True)
app.mount("/static", StaticFiles(directory=static_dir), name="static")

# 全局状态
task_status = {
    "running": False,
    "progress": 0,
    "current_step": "",
    "result": None,
    "error": None
}

class AnalysisRequest(BaseModel):
    directory: str
    include_code: bool = True
    include_docs: bool = True
    large_scale: bool = False
    incremental: bool = False
    force: bool = False

class ExportRequest(BaseModel):
    output_path: str
    format: str = "markdown"

class OptimizeRequest(BaseModel):
    level: str = "medium"

class AIConfigRequest(BaseModel):
    """AI配置请求"""
    provider: str = "volcengine"
    api_key: str = ""
    base_url: str = ""
    model: str = ""
    tools_enabled: bool = False
    web_search_enabled: bool = False

@app.get("/", response_class=HTMLResponse)
async def index(request: Request):
    """主页"""
    return templates.TemplateResponse("index.html", {"request": request})

@app.get("/api/status")
async def get_status():
    """获取当前任务状态"""
    return JSONResponse(content=task_status)

# ============ AI配置管理 API ============

@app.get("/api/ai-config")
async def get_ai_config():
    """获取当前AI配置"""
    try:
        import yaml
        
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        ai_config = config.get('ai', {})
        provider = ai_config.get('provider', 'volcengine')
        provider_config = ai_config.get(provider, {})
        
        # 返回配置信息（不返回完整api_key）
        return JSONResponse(content={
            "provider": provider,
            "api_key_set": bool(provider_config.get('api_key') or ai_config.get('api_key')),
            "api_key_preview": _mask_api_key(provider_config.get('api_key') or ai_config.get('api_key', '')),
            "base_url": provider_config.get('base_url') or ai_config.get('base_url', ''),
            "model": provider_config.get('model') or ai_config.get('model', ''),
            "tools_enabled": provider_config.get('tools', {}).get('enabled', False),
            "web_search_enabled": provider_config.get('tools', {}).get('web_search', {}).get('enabled', False),
            "available_providers": ["volcengine", "openai"]
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ai-config")
async def update_ai_config(request: AIConfigRequest):
    """更新AI配置"""
    try:
        import yaml
        
        # 读取现有配置
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        if 'ai' not in config:
            config['ai'] = {}
        
        # 更新provider
        config['ai']['provider'] = request.provider
        
        # 确保provider配置存在
        if request.provider not in config['ai']:
            config['ai'][request.provider] = {}
        
        # 更新provider特定配置
        if request.api_key:
            config['ai'][request.provider]['api_key'] = request.api_key
        if request.base_url:
            config['ai'][request.provider]['base_url'] = request.base_url
        if request.model:
            config['ai'][request.provider]['model'] = request.model
        
        # 更新工具配置
        if 'tools' not in config['ai'][request.provider]:
            config['ai'][request.provider]['tools'] = {}
        config['ai'][request.provider]['tools']['enabled'] = request.tools_enabled
        
        if 'web_search' not in config['ai'][request.provider]['tools']:
            config['ai'][request.provider]['tools']['web_search'] = {}
        config['ai'][request.provider]['tools']['web_search']['enabled'] = request.web_search_enabled
        
        # 保存配置
        with open(CONFIG_PATH, 'w', encoding='utf-8') as f:
            yaml.dump(config, f, allow_unicode=True, default_flow_style=False)
        
        return JSONResponse(content={
            "status": "success",
            "message": "配置已保存，重启服务生效"
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ai-test")
async def test_ai_connection():
    """测试AI连接"""
    try:
        from src.ai.llm_client import LLMClient
        import yaml
        
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        
        llm_config = config.get('ai', {})
        client = LLMClient(llm_config)
        
        if not client.is_available():
            return JSONResponse(content={
                "success": False,
                "message": "API Key未配置或无效"
            })
        
        # 测试简单调用
        result = client.chat_sync(
            system_prompt="你是一个助手",
            user_prompt="请回复'连接成功'三个字",
            max_tokens=50
        )
        
        if result:
            return JSONResponse(content={
                "success": True,
                "message": f"连接成功，模型响应: {result[:50]}",
                "config": client.get_config_info()
            })
        else:
            return JSONResponse(content={
                "success": False,
                "message": "模型调用失败，请检查配置"
            })
    except Exception as e:
        return JSONResponse(content={
            "success": False,
            "message": f"连接失败: {str(e)}"
        })

def _mask_api_key(key: str) -> str:
    """遮蔽API Key，只显示前后几位"""
    if not key or len(key) < 8:
        return "***" if key else ""
    return f"{key[:4]}...{key[-4:]}"

@app.post("/api/analyze")
async def start_analysis(request: AnalysisRequest, background_tasks: BackgroundTasks):
    """启动分析任务"""
    if task_status["running"]:
        raise HTTPException(status_code=400, detail="已有任务正在运行")
    
    task_status.update({
        "running": True,
        "progress": 0,
        "current_step": "初始化分析器",
        "result": None,
        "error": None
    })
    
    background_tasks.add_task(run_analysis_task, request)
    
    return {"status": "success", "message": "分析任务已启动"}

async def run_analysis_task(request: AnalysisRequest):
    """后台运行分析任务"""
    try:
        from src.core.knowledge_extractor import KnowledgeExtractor
        from src.core.large_scale_processor import LargeScaleProcessor
        
        task_status["current_step"] = "加载配置"
        await asyncio.sleep(0.5)
        
        if request.large_scale:
            processor = LargeScaleProcessor()
            task_status["current_step"] = "开始大规模项目分析"
            task_status["progress"] = 10
            await asyncio.sleep(0.5)
            
            graph = processor.process_large_project(
                request.directory,
                include_code=request.include_code,
                include_docs=request.include_docs,
                incremental=request.incremental,
                resume=False
            )
        else:
            extractor = KnowledgeExtractor()
            task_status["current_step"] = "开始目录分析"
            task_status["progress"] = 10
            await asyncio.sleep(0.5)
            
            graph = extractor.extract_from_directory(
                request.directory,
                include_code=request.include_code,
                include_docs=request.include_docs,
                force=request.force
            )
        
        task_status["progress"] = 90
        task_status["current_step"] = "分析完成，生成统计信息"
        await asyncio.sleep(0.5)
        
        stats = {
            "total_files": len(set(e.file_path for e in graph.entities.values())),
            "total_entities": len(graph.entities),
            "total_relationships": len(graph.relationships),
            "lines_of_code": sum(e.lines_of_code for e in graph.entities.values() if hasattr(e, 'lines_of_code'))
        }
        
        task_status["progress"] = 100
        task_status["current_step"] = "任务完成"
        task_status["result"] = {
            "stats": stats,
            "graph_summary": {
                "entities": {k: v.model_dump() for k, v in list(graph.entities.items())[:100]},  # 只返回前100个实体
                "relationships": graph.relationships[:100]
            }
        }
        
    except Exception as e:
        task_status["error"] = str(e)
        task_status["current_step"] = "任务失败"
    finally:
        task_status["running"] = False

@app.post("/api/export")
async def export_results(request: ExportRequest):
    """导出分析结果"""
    try:
        from src.gitnexus import GitNexusClient, ExportFormat
        
        client = GitNexusClient(str(CONFIG_PATH))
        
        format_map = {
            "json": ExportFormat.JSON,
            "markdown": ExportFormat.MARKDOWN,
            "html": ExportFormat.HTML,
            "csv": ExportFormat.CSV,
            "graphml": ExportFormat.GRAPHML,
            "plantuml": ExportFormat.PLANTUML,
            "mindmap": ExportFormat.MINDMAP
        }
        
        output_path = client.export(
            output_path=request.output_path,
            format=format_map[request.format]
        )
        
        return {"status": "success", "output_path": output_path}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/optimize")
async def optimize_knowledge(request: OptimizeRequest):
    """优化知识图谱"""
    try:
        from src.ai.knowledge_optimizer import KnowledgeOptimizer
        from src.gitnexus import GitNexusClient
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        optimizer = KnowledgeOptimizer()
        optimized_graph = optimizer.optimize_knowledge_graph(graph, optimization_level=request.level)
        
        client._save_knowledge_graph(optimized_graph)
        
        return {"status": "success", "message": "知识图谱优化完成"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/stats")
async def get_statistics():
    """获取统计信息"""
    try:
        from src.gitnexus import GitNexusClient
        
        client = GitNexusClient(str(CONFIG_PATH))
        stats = client.get_statistics()
        
        return JSONResponse(content=stats)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/entities")
async def get_entities(limit: int = 100, offset: int = 0, entity_type: Optional[str] = None):
    """获取实体列表"""
    try:
        from src.gitnexus import GitNexusClient
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        entities = list(graph.entities.values())
        if entity_type:
            entities = [e for e in entities if e.entity_type == entity_type]
        
        total = len(entities)
        entities = entities[offset:offset+limit]
        
        return {
            "total": total,
            "entities": [e.model_dump() for e in entities]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/validate")
async def validate_knowledge(source_directory: str):
    """验证知识提取准确性"""
    try:
        from src.ai.self_validation import SelfValidator
        from src.gitnexus import GitNexusClient
        
        validator = SelfValidator()
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        validation_results = validator.validate_knowledge_graph(graph, source_directory)
        
        return JSONResponse(content=validation_results)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/knowledge-result")
async def get_knowledge_result():
    """获取知识分析结果"""
    try:
        from src.gitnexus import GitNexusClient
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无知识图谱数据，请先运行分析"})
        
        # 按文件分组实体
        file_groups = {}
        for entity in graph.entities.values():
            file_path = entity.file_path
            if file_path not in file_groups:
                file_groups[file_path] = []
            file_groups[file_path].append(entity.model_dump())
        
        # 提取关键实体（函数、类、方法）
        key_entities = {
            "classes": [],
            "functions": [],
            "methods": []
        }
        for entity in graph.entities.values():
            if entity.entity_type == "class":
                key_entities["classes"].append({
                    "id": entity.id,
                    "name": entity.name,
                    "file": entity.file_path,
                    "lines": entity.lines_of_code,
                    "docstring": entity.docstring
                })
            elif entity.entity_type == "function":
                key_entities["functions"].append({
                    "id": entity.id,
                    "name": entity.name,
                    "file": entity.file_path,
                    "lines": entity.lines_of_code,
                    "params": entity.parameters,
                    "docstring": entity.docstring
                })
            elif entity.entity_type == "method":
                key_entities["methods"].append({
                    "id": entity.id,
                    "name": entity.name,
                    "file": entity.file_path,
                    "lines": entity.lines_of_code,
                    "params": entity.parameters,
                    "docstring": entity.docstring
                })
        
        # 关系分析
        relationship_summary = {}
        for rel in graph.relationships:
            rel_type = rel.relationship_type
            if rel_type not in relationship_summary:
                relationship_summary[rel_type] = 0
            relationship_summary[rel_type] += 1
        
        return JSONResponse(content={
            "project_name": graph.project_name,
            "generated_at": graph.generated_at,
            "total_entities": len(graph.entities),
            "total_relationships": len(graph.relationships),
            "file_groups": file_groups,
            "key_entities": key_entities,
            "relationship_summary": relationship_summary,
            "metadata": graph.metadata
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/business-logic")
async def get_business_logic():
    """分析项目业务逻辑"""
    try:
        from src.gitnexus import GitNexusClient
        from collections import defaultdict
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据，请先运行分析"})
        
        # 1. 模块结构分析
        modules = defaultdict(lambda: {"entities": [], "total_lines": 0, "types": defaultdict(int)})
        for entity in graph.entities.values():
            file_path = entity.file_path
            # 提取模块路径
            parts = file_path.replace("\\", "/").split("/")
            if len(parts) > 1:
                module = "/".join(parts[:-1]) if len(parts) > 2 else parts[0]
            else:
                module = "root"
            
            modules[module]["entities"].append({
                "name": entity.name,
                "type": entity.entity_type,
                "lines": entity.lines_of_code
            })
            modules[module]["total_lines"] += entity.lines_of_code
            modules[module]["types"][entity.entity_type] += 1
        
        # 2. 核心功能识别（根据调用关系和实体复杂度）
        entity_call_count = defaultdict(int)
        entity_called_count = defaultdict(int)
        
        for rel in graph.relationships:
            if rel.relationship_type == "calls":
                entity_call_count[rel.source_id] += 1
                entity_called_count[rel.target_id] += 1
        
        # 识别核心实体（被调用次数多或调用其他多）
        core_entities = []
        for entity_id, entity in graph.entities.items():
            if entity.entity_type in ["function", "method", "class"]:
                called_times = entity_called_count.get(entity_id, 0)
                calls_times = entity_call_count.get(entity_id, 0)
                importance_score = called_times * 2 + calls_times
                
                if importance_score > 0 or entity.lines_of_code > 50:
                    core_entities.append({
                        "id": entity_id,
                        "name": entity.name,
                        "type": entity.entity_type,
                        "file": entity.file_path,
                        "lines": entity.lines_of_code,
                        "called_times": called_times,
                        "calls_times": calls_times,
                        "importance": importance_score
                    })
        
        # 按重要性排序
        core_entities.sort(key=lambda x: x["importance"], reverse=True)
        core_entities = core_entities[:50]  # 取前50个
        
        # 3. 依赖关系分析
        dependencies = {
            "imports": [],
            "calls": [],
            "inherits": [],
            "uses": []
        }
        
        for rel in graph.relationships:
            source = graph.entities.get(rel.source_id)
            target = graph.entities.get(rel.target_id)
            if source and target:
                dep_info = {
                    "source": source.name,
                    "source_file": source.file_path,
                    "target": target.name,
                    "target_file": target.file_path,
                    "confidence": rel.confidence
                }
                if rel.relationship_type in dependencies:
                    dependencies[rel.relationship_type].append(dep_info)
        
        # 4. 项目层级结构
        hierarchy = defaultdict(lambda: {"children": [], "entities": []})
        for entity in graph.entities.values():
            if entity.parent_id:
                parent = graph.entities.get(entity.parent_id)
                if parent:
                    hierarchy[parent.name]["children"].append(entity.name)
            else:
                hierarchy["__root__"]["entities"].append({
                    "name": entity.name,
                    "type": entity.entity_type,
                    "file": entity.file_path
                })
        
        # 5. 业务流程推断（基于调用链）
        call_chains = []
        visited = set()
        
        def build_call_chain(entity_id, chain, depth=0):
            if depth > 5 or entity_id in visited:
                return
            visited.add(entity_id)
            
            entity = graph.entities.get(entity_id)
            if not entity:
                return
            
            chain.append(entity.name)
            
            # 找到该实体调用的其他实体
            calls = [r for r in graph.relationships if r.source_id == entity_id and r.relationship_type == "calls"]
            
            if calls:
                for call in calls[:3]:  # 最多追踪3个分支
                    build_call_chain(call.target_id, chain.copy(), depth + 1)
            else:
                if len(chain) > 1:
                    call_chains.append(chain)
        
        # 从核心实体开始构建调用链
        for core in core_entities[:10]:
            visited.clear()
            build_call_chain(core["id"], [])
        
        return JSONResponse(content={
            "modules": {k: {
                "entity_count": len(v["entities"]),
                "total_lines": v["total_lines"],
                "types": dict(v["types"])
            } for k, v in modules.items()},
            "core_entities": core_entities,
            "dependencies": {
                k: v[:20] for k, v in dependencies.items()  # 每种依赖最多20个
            },
            "hierarchy": {
                k: v for k, v in hierarchy.items() if k != "__root__" or v["entities"]
            },
            "call_chains": call_chains[:20],  # 最多20条调用链
            "summary": {
                "total_modules": len(modules),
                "total_core_entities": len(core_entities),
                "total_call_chains": len(call_chains)
            }
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/entity/{entity_id}")
async def get_entity_detail(entity_id: str):
    """获取实体详细信息"""
    try:
        from src.gitnexus import GitNexusClient
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        entity = graph.entities.get(entity_id)
        if not entity:
            raise HTTPException(status_code=404, detail="实体不存在")
        
        # 获取依赖关系
        dependencies = graph.get_entity_dependencies(entity_id)
        usages = graph.get_entity_usage(entity_id)
        
        return JSONResponse(content={
            "entity": entity.model_dump(),
            "dependencies": [d.model_dump() for d in dependencies],
            "usages": [u.model_dump() for u in usages]
        })
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ============ 架构分析 API ============

@app.get("/api/architecture")
async def get_architecture():
    """获取完整架构分析"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据，请先运行分析"})
        
        analyzer = ArchitectureAnalyzer(graph)
        architecture = analyzer.analyze_full()
        
        return JSONResponse(content={
            "modules": {k: v.model_dump() for k, v in architecture.modules.items()},
            "features": {k: v.model_dump() for k, v in architecture.features.items()},
            "call_chains": [c.model_dump() for c in architecture.call_chains[:20]],
            "exception_flows": {k: v.model_dump() for k, v in architecture.exception_flows.items()},
            "message_flows": {k: v.model_dump() for k, v in architecture.message_flows.items()},
            "api_endpoints": [e.model_dump() for e in architecture.api_endpoints],
            "layers": architecture.layers,
            "statistics": architecture.statistics
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/architecture/modules")
async def get_architecture_modules():
    """获取模块结构"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        analyzer = ArchitectureAnalyzer(graph)
        modules = analyzer.analyze_modules()
        
        return JSONResponse(content={
            "modules": {k: v.model_dump() for k, v in modules.items()}
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/architecture/features")
async def get_architecture_features():
    """获取功能模块"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        analyzer = ArchitectureAnalyzer(graph)
        features = analyzer.analyze_features()
        
        return JSONResponse(content={
            "features": {k: v.model_dump() for k, v in features.items()}
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/architecture/call-chains")
async def get_architecture_call_chains(limit: int = 20, max_depth: int = 10):
    """获取调用链分析"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        analyzer = ArchitectureAnalyzer(graph)
        call_chains = analyzer.analyze_call_chains(max_chains=limit, max_depth=max_depth)
        
        return JSONResponse(content={
            "call_chains": [c.model_dump() for c in call_chains]
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/architecture/exceptions")
async def get_architecture_exceptions():
    """获取异常流分析"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        analyzer = ArchitectureAnalyzer(graph)
        exception_flows = analyzer.analyze_exception_flows()
        
        return JSONResponse(content={
            "exception_flows": {k: v.model_dump() for k, v in exception_flows.items()}
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/uml/class-diagram")
async def get_uml_class_diagram(format: str = "mermaid", max_entities: int = 50):
    """获取类图"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.uml_generator import UMLGenerator
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        generator = UMLGenerator(graph)
        diagram = generator.generate_class_diagram(format=format, max_entities=max_entities)
        
        return JSONResponse(content={
            "format": format,
            "diagram": diagram
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/uml/sequence-diagram/{chain_index}")
async def get_uml_sequence_diagram(chain_index: int, format: str = "mermaid"):
    """获取时序图"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        from src.core.uml_generator import UMLGenerator
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        analyzer = ArchitectureAnalyzer(graph)
        call_chains = analyzer.analyze_call_chains()
        
        if chain_index >= len(call_chains):
            raise HTTPException(status_code=404, detail="调用链不存在")
        
        generator = UMLGenerator(graph)
        diagram = generator.generate_sequence_diagram(call_chains[chain_index], format=format)
        
        return JSONResponse(content={
            "format": format,
            "diagram": diagram,
            "chain_name": call_chains[chain_index].name
        })
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/uml/flowchart")
async def get_uml_flowchart(flow_type: str = "call", format: str = "mermaid"):
    """获取流程图"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        from src.core.uml_generator import UMLGenerator
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        analyzer = ArchitectureAnalyzer(graph)
        architecture = analyzer.analyze_full()
        
        generator = UMLGenerator(graph, architecture)
        diagram = generator.generate_flowchart(flow_type=flow_type, format=format)
        
        return JSONResponse(content={
            "format": format,
            "flow_type": flow_type,
            "diagram": diagram
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/uml/component-diagram")
async def get_uml_component_diagram(format: str = "mermaid"):
    """获取组件图"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        from src.core.uml_generator import UMLGenerator
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        analyzer = ArchitectureAnalyzer(graph)
        architecture = analyzer.analyze_full()
        
        generator = UMLGenerator(graph, architecture)
        diagram = generator.generate_component_diagram(format=format)
        
        return JSONResponse(content={
            "format": format,
            "diagram": diagram
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/export-architecture-report")
async def export_architecture_report(output_path: str):
    """导出架构分析报告"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_reporter import ArchitectureReporter
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            raise HTTPException(status_code=400, detail="暂无数据，请先运行分析")
        
        reporter = ArchitectureReporter(graph)
        reporter.export_to_file(output_path)
        
        return {"status": "success", "output_path": output_path}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/refactoring-suggestions")
async def get_refactoring_suggestions():
    """获取重构建议"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        from src.gitnexus.models import EntityType
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        analyzer = ArchitectureAnalyzer(graph)
        stats = analyzer.generate_statistics()
        
        suggestions = []
        
        # 高复杂度实体
        high_complexity = [
            e for e in graph.entities.values()
            if e.complexity and e.complexity > 15
        ]
        for entity in high_complexity[:10]:
            suggestions.append({
                "type": "high_complexity",
                "severity": "high",
                "entity": entity.name,
                "file": entity.file_path,
                "line": entity.start_line,
                "detail": f"圈复杂度 {entity.complexity}，建议拆分或简化",
                "suggestion": "考虑将此函数拆分为多个更小的函数，或使用策略模式/状态模式降低复杂度"
            })
        
        # 过长函数
        long_functions = [
            e for e in graph.entities.values()
            if e.entity_type in [EntityType.FUNCTION, EntityType.METHOD]
            and e.lines_of_code > 50
        ]
        for entity in long_functions[:10]:
            suggestions.append({
                "type": "long_function",
                "severity": "medium",
                "entity": entity.name,
                "file": entity.file_path,
                "line": entity.start_line,
                "detail": f"函数长度 {entity.lines_of_code} 行",
                "suggestion": "考虑将此函数拆分为多个更小的函数，提高可读性和可维护性"
            })
        
        # 循环依赖
        architecture = analyzer.analyze_full()
        cycles = []
        visited = set()
        
        def detect_cycles(node, path, all_modules):
            if node in visited:
                return
            visited.add(node)
            module = all_modules.get(node)
            if module:
                for dep in module.dependencies:
                    if dep in path:
                        cycle_start = path.index(dep)
                        cycles.append(path[cycle_start:] + [dep])
                    else:
                        detect_cycles(dep, path + [node], all_modules)
        
        for module_path in architecture.modules.keys():
            detect_cycles(module_path, [], architecture.modules)
        
        for cycle in cycles[:5]:
            suggestions.append({
                "type": "cyclic_dependency",
                "severity": "high",
                "entities": cycle,
                "detail": f"循环依赖: {' → '.join(cycle)}",
                "suggestion": "引入接口或中介者模式来打破循环依赖"
            })
        
        # 热点实体（过度使用）
        for entity_info in stats.get('most_called_entities', [])[:5]:
            if entity_info['calls'] > 10:
                entity = graph.entities.get(entity_info['id'])
                if entity:
                    suggestions.append({
                        "type": "hotspot",
                        "severity": "medium",
                        "entity": entity.name,
                        "file": entity.file_path,
                        "line": entity.start_line,
                        "detail": f"被调用 {entity_info['calls']} 次",
                        "suggestion": "高频调用的实体，考虑缓存优化或接口优化"
                    })
        
        return JSONResponse(content={
            "suggestions": suggestions,
            "summary": {
                "high_complexity_count": len(high_complexity),
                "long_function_count": len(long_functions),
                "cyclic_dependency_count": len(cycles),
                "total_suggestions": len(suggestions)
            }
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ============ ArchAI CodeExplainer + GitUML 集成 API ============

class EnhancedAnalysisRequest(BaseModel):
    """增强分析请求"""
    mode: str = "full"  # full, refactor, doc

class GitUMLRequest(BaseModel):
    """GitUML请求"""
    diagram_type: str = "class"  # class, sequence, module, diff, feature, timeline
    format: str = "mermaid"  # mermaid, plantuml
    commit1: Optional[str] = None
    commit2: Optional[str] = None
    highlight_changes: bool = False


@app.get("/api/enhanced/architecture")
async def get_enhanced_architecture():
    """获取增强版架构分析（ArchAI）"""
    try:
        from src.gitnexus import GitNexusClient
        from src.integrations import create_enhanced_extractor
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据，请先运行分析"})
        
        extractor = create_enhanced_extractor(graph)
        arch_result = extractor.code_explainer.analyze_architecture()
        
        return JSONResponse(content={
            "primary_pattern": arch_result.primary_pattern.value,
            "secondary_patterns": [p.value for p in arch_result.secondary_patterns],
            "layers": arch_result.layers,
            "circular_dependencies": arch_result.circular_dependencies,
            "design_issues": arch_result.design_issues[:10],
            "improvement_suggestions": arch_result.improvement_suggestions,
            "statistics": arch_result.statistics
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/enhanced/hotspots")
async def get_refactoring_hotspots():
    """获取重构热点（ArchAI）"""
    try:
        from src.gitnexus import GitNexusClient
        from src.integrations import create_enhanced_extractor
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        extractor = create_enhanced_extractor(graph)
        hotspots = extractor.code_explainer._identify_refactoring_hotspots()
        
        return JSONResponse(content={
            "hotspots": hotspots,
            "total": len(hotspots)
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/enhanced/explain/{entity_id}")
async def explain_entity(entity_id: str):
    """深度解释实体（ArchAI）"""
    try:
        from src.gitnexus import GitNexusClient
        from src.integrations import create_enhanced_extractor
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        extractor = create_enhanced_extractor(graph)
        result = extractor.get_entity_deep_analysis(entity_id)
        
        return JSONResponse(content=result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/enhanced/smells")
async def get_code_smells():
    """获取代码异味汇总（ArchAI）"""
    try:
        from src.gitnexus import GitNexusClient
        from src.integrations import create_enhanced_extractor
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        extractor = create_enhanced_extractor(graph)
        
        all_smells = []
        for entity in graph.entities.values():
            smells = extractor.code_explainer._detect_code_smells(entity)
            for smell in smells:
                smell["entity_id"] = entity.id
                smell["entity_name"] = entity.name
                smell["file_path"] = entity.file_path
                all_smells.append(smell)
        
        # 按类型汇总
        smell_summary = extractor.code_explainer._group_and_rank_smells(all_smells)
        
        return JSONResponse(content={
            "smells": smell_summary,
            "total_smells": len(all_smells),
            "high_severity_count": sum(1 for s in all_smells if s.get("severity") == "high")
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/enhanced/refactor-analysis")
async def get_refactor_analysis():
    """获取完整重构分析（ArchAI + GitUML）"""
    try:
        from src.gitnexus import GitNexusClient
        from src.integrations import create_enhanced_extractor
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据，请先运行分析"})
        
        extractor = create_enhanced_extractor(graph)
        result = extractor.analyze_for_refactoring()
        
        return JSONResponse(content=result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/gituml/generate")
async def generate_gituml_diagram(request: GitUMLRequest):
    """生成GitUML增强图表"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.architecture_analyzer import ArchitectureAnalyzer
        from src.integrations import GitUMLIntegration
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据，请先运行分析"})
        
        # 分析架构
        analyzer = ArchitectureAnalyzer(graph)
        architecture = analyzer.analyze_full()
        
        # 初始化GitUML
        gituml = GitUMLIntegration(graph, ".", architecture)
        
        diagram = None
        
        if request.diagram_type == "class":
            diagram = gituml.generate_class_diagram_enhanced(
                format=request.format,
                highlight_recent_changes=request.highlight_changes
            )
        elif request.diagram_type == "module":
            diagram = gituml.generate_module_dependency_diagram(format=request.format)
        elif request.diagram_type == "sequence":
            if architecture.call_chains:
                diagram = gituml.generate_sequence_diagram_enhanced(
                    architecture.call_chains[0],
                    format=request.format
                )
            else:
                return JSONResponse(content={"error": "无调用链数据"})
        elif request.diagram_type == "diff":
            if request.commit1 and request.commit2:
                diagram = gituml.generate_diff_diagram(
                    request.commit1, request.commit2, format=request.format
                )
            else:
                return JSONResponse(content={"error": "差异图需要指定commit1和commit2"})
        elif request.diagram_type == "feature":
            if architecture.features:
                first_feature = list(architecture.features.keys())[0]
                diagram = gituml.generate_feature_diagram(first_feature, format=request.format)
            else:
                return JSONResponse(content={"error": "无功能模块数据"})
        elif request.diagram_type == "timeline":
            diagram = gituml.generate_git_history_timeline(format=request.format)
        
        if diagram:
            return JSONResponse(content={
                "diagram_type": diagram.diagram_type.value,
                "format": diagram.format.value,
                "content": diagram.content,
                "title": diagram.title,
                "description": diagram.description,
                "entity_count": diagram.entity_count
            })
        else:
            return JSONResponse(content={"error": "无法生成图表"})
            
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/gituml/all")
async def generate_all_gituml_diagrams(format: str = "mermaid"):
    """生成所有GitUML图表"""
    try:
        from src.gitnexus import GitNexusClient
        from src.integrations import create_enhanced_extractor
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据，请先运行分析"})
        
        extractor = create_enhanced_extractor(graph)
        diagrams = extractor.gituml.generate_all_diagrams(format=format)
        
        return JSONResponse(content={
            name: {
                "diagram_type": diagram.diagram_type.value,
                "content": diagram.content,
                "title": diagram.title,
                "entity_count": diagram.entity_count
            }
            for name, diagram in diagrams.items()
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/gituml/git-info")
async def get_git_info():
    """获取Git信息"""
    try:
        from src.gitnexus import GitNexusClient
        from src.integrations import GitUMLIntegration
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        gituml = GitUMLIntegration(graph, ".")
        
        return JSONResponse(content={
            "git_available": gituml.git_available,
            "current_branch": gituml.current_branch,
            "recent_commits": [
                {
                    "hash": c.hash,
                    "author": c.author,
                    "date": c.date,
                    "message": c.message
                }
                for c in gituml.recent_commits
            ]
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/enhanced/feature/{feature_id}")
async def explain_feature(feature_id: str):
    """解释功能模块"""
    try:
        from src.gitnexus import GitNexusClient
        from src.integrations import create_enhanced_extractor
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        extractor = create_enhanced_extractor(graph)
        result = extractor.code_explainer.explain_feature(feature_id)
        
        return JSONResponse(content=result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/enhanced/compare-commits")
async def compare_commits(commit1: str, commit2: str):
    """比较两次提交"""
    try:
        from src.gitnexus import GitNexusClient
        from src.integrations import create_enhanced_extractor
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        extractor = create_enhanced_extractor(graph)
        result = extractor.compare_commits(commit1, commit2)
        
        return JSONResponse(content=result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ============ UML设计思想知识组织 API ============

@app.get("/api/uml-design/technology-stack")
async def get_technology_stack():
    """获取项目技术选型分析"""
    try:
        from src.gitnexus import GitNexusClient
        from collections import defaultdict
        import re
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据，请先运行分析"})
        
        # 分析技术栈
        tech_stack = {
            "languages": defaultdict(int),
            "frameworks": [],
            "libraries": [],
            "databases": [],
            "tools": [],
            "patterns": []
        }
        
        # 语言识别
        file_extensions = defaultdict(int)
        for entity in graph.entities.values():
            file_path = entity.file_path.lower()
            ext = file_path.split('.')[-1] if '.' in file_path else ''
            if ext:
                file_extensions[ext] += 1
        
        # 扩展名到语言映射
        ext_to_lang = {
            'py': 'Python', 'js': 'JavaScript', 'ts': 'TypeScript',
            'java': 'Java', 'go': 'Go', 'rs': 'Rust',
            'cpp': 'C++', 'c': 'C', 'h': 'C/C++ Header',
            'cs': 'C#', 'rb': 'Ruby', 'php': 'PHP',
            'swift': 'Swift', 'kt': 'Kotlin', 'scala': 'Scala',
            'vue': 'Vue', 'jsx': 'React JSX', 'tsx': 'React TSX',
            'html': 'HTML', 'css': 'CSS', 'scss': 'SCSS',
            'sql': 'SQL', 'sh': 'Shell', 'yaml': 'YAML',
            'json': 'JSON', 'xml': 'XML', 'md': 'Markdown'
        }
        
        for ext, count in file_extensions.items():
            lang = ext_to_lang.get(ext, ext.upper())
            tech_stack["languages"][lang] += count
        
        # 从导入语句识别框架和库
        framework_keywords = {
            'django': 'Django', 'flask': 'Flask', 'fastapi': 'FastAPI',
            'express': 'Express.js', 'react': 'React', 'vue': 'Vue.js',
            'angular': 'Angular', 'spring': 'Spring', 'numpy': 'NumPy',
            'pandas': 'Pandas', 'tensorflow': 'TensorFlow', 'torch': 'PyTorch',
            'scikit': 'Scikit-learn', 'matplotlib': 'Matplotlib',
            'sqlalchemy': 'SQLAlchemy', 'mongoose': 'Mongoose',
            'sequelize': 'Sequelize', 'prisma': 'Prisma',
            'redis': 'Redis', 'celery': 'Celery', 'kafka': 'Kafka',
            'grpc': 'gRPC', 'graphql': 'GraphQL', 'apollo': 'Apollo',
            'next': 'Next.js', 'nuxt': 'Nuxt.js', 'svelte': 'Svelte',
            'tailwind': 'Tailwind CSS', 'bootstrap': 'Bootstrap',
            'pytest': 'pytest', 'jest': 'Jest', 'mocha': 'Mocha',
            'unittest': 'unittest', 'selenium': 'Selenium'
        }
        
        detected_frameworks = set()
        for entity in graph.entities.values():
            content = entity.content.lower() if entity.content else ""
            for keyword, name in framework_keywords.items():
                if keyword in content:
                    detected_frameworks.add(name)
        
        tech_stack["frameworks"] = list(detected_frameworks)
        
        # 分析架构模式
        pattern_indicators = {
            "MVC": ["controller", "model", "view"],
            "MVVM": ["viewmodel", "binding"],
            "Repository Pattern": ["repository", "dao"],
            "Factory Pattern": ["factory", "create"],
            "Singleton": ["singleton", "instance"],
            "Observer": ["observer", "subscribe", "emit"],
            "Strategy": ["strategy", "algorithm"],
            "Decorator": ["decorator", "wrapper"],
            "Dependency Injection": ["inject", "container", "di"],
            "Microservices": ["service", "api", "endpoint", "route"],
            "Layered Architecture": ["layer", "service", "dal", "bll"]
        }
        
        detected_patterns = []
        for pattern, keywords in pattern_indicators.items():
            matches = 0
            for entity in graph.entities.values():
                name_lower = entity.name.lower() if entity.name else ""
                for kw in keywords:
                    if kw in name_lower:
                        matches += 1
                        break
            if matches >= 2:
                detected_patterns.append({
                    "name": pattern,
                    "confidence": min(matches / 5, 1.0),
                    "matches": matches
                })
        
        tech_stack["patterns"] = sorted(detected_patterns, key=lambda x: x["confidence"], reverse=True)
        
        # 项目统计
        total_lines = sum(e.lines_of_code for e in graph.entities.values())
        
        return JSONResponse(content={
            "languages": dict(tech_stack["languages"]),
            "frameworks": tech_stack["frameworks"],
            "detected_patterns": tech_stack["patterns"],
            "statistics": {
                "total_files": len(set(e.file_path for e in graph.entities.values())),
                "total_entities": len(graph.entities),
                "total_lines": total_lines,
                "extension_distribution": dict(file_extensions)
            }
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/uml-design/architecture")
async def get_uml_architecture():
    """获取架构设计（UML组件图风格）"""
    try:
        from src.gitnexus import GitNexusClient
        from collections import defaultdict
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        # 1. 识别系统组件
        components = defaultdict(lambda: {
            "name": "",
            "type": "module",
            "entities": [],
            "dependencies": set(),
            "lines_of_code": 0,
            "description": ""
        })
        
        for entity in graph.entities.values():
            file_path = entity.file_path.replace("\\", "/")
            parts = file_path.split("/")
            
            # 识别组件层级
            if len(parts) >= 2:
                component_path = "/".join(parts[:2])
            else:
                component_path = parts[0] if parts else "root"
            
            components[component_path]["name"] = component_path
            components[component_path]["entities"].append({
                "id": entity.id,
                "name": entity.name,
                "type": entity.entity_type
            })
            components[component_path]["lines_of_code"] += entity.lines_of_code
        
        # 2. 分析组件依赖
        for rel in graph.relationships:
            source = graph.entities.get(rel.source_id)
            target = graph.entities.get(rel.target_id)
            if source and target:
                source_path = "/".join(source.file_path.replace("\\", "/").split("/")[:2])
                target_path = "/".join(target.file_path.replace("\\", "/").split("/")[:2])
                if source_path != target_path:
                    components[source_path]["dependencies"].add(target_path)
        
        # 3. 识别架构层
        layer_keywords = {
            "presentation": ["view", "ui", "template", "page", "component"],
            "business": ["service", "logic", "manager", "handler", "controller"],
            "data": ["dao", "repository", "model", "entity", "schema", "db"],
            "infrastructure": ["config", "util", "helper", "common", "core"]
        }
        
        layers = defaultdict(list)
        for comp_path, comp_info in components.items():
            path_lower = comp_path.lower()
            assigned = False
            for layer, keywords in layer_keywords.items():
                if any(kw in path_lower for kw in keywords):
                    layers[layer].append({
                        "name": comp_path,
                        "entities": len(comp_info["entities"]),
                        "lines": comp_info["lines_of_code"]
                    })
                    assigned = True
                    break
            if not assigned:
                layers["other"].append({
                    "name": comp_path,
                    "entities": len(comp_info["entities"]),
                    "lines": comp_info["lines_of_code"]
                })
        
        # 4. 生成Mermaid组件图
        mermaid_diagram = "graph TB\n"
        for layer, comps in layers.items():
            mermaid_diagram += f"    subgraph {layer}[{layer.upper()}层]\n"
            for comp in comps[:10]:
                safe_name = comp["name"].replace("/", "_").replace("-", "_")
                mermaid_diagram += f"        {safe_name}[{comp['name']}]\n"
            mermaid_diagram += "    end\n"
        
        # 添加依赖关系
        for comp_path, comp_info in list(components.items())[:20]:
            safe_source = comp_path.replace("/", "_").replace("-", "_")
            for dep in list(comp_info["dependencies"])[:5]:
                safe_dep = dep.replace("/", "_").replace("-", "_")
                mermaid_diagram += f"    {safe_source} --> {safe_dep}\n"
        
        return JSONResponse(content={
            "components": {
                k: {
                    "name": v["name"],
                    "entity_count": len(v["entities"]),
                    "lines_of_code": v["lines_of_code"],
                    "dependencies": list(v["dependencies"])
                } for k, v in components.items()
            },
            "layers": dict(layers),
            "mermaid_diagram": mermaid_diagram,
            "statistics": {
                "total_components": len(components),
                "layer_distribution": {k: len(v) for k, v in layers.items()}
            }
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/uml-design/outline")
async def get_uml_outline():
    """获取概要设计（UML包图风格）"""
    try:
        from src.gitnexus import GitNexusClient
        from collections import defaultdict
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        # 1. 模块划分
        packages = defaultdict(lambda: {
            "name": "",
            "classes": [],
            "interfaces": [],
            "functions": [],
            "dependencies": set(),
            "exports": []
        })
        
        for entity in graph.entities.values():
            file_path = entity.file_path.replace("\\", "/")
            parts = file_path.split("/")
            package = "/".join(parts[:-1]) if len(parts) > 1 else "default"
            
            packages[package]["name"] = package
            
            if entity.entity_type == "class":
                packages[package]["classes"].append({
                    "name": entity.name,
                    "lines": entity.lines_of_code,
                    "methods_count": len([e for e in graph.entities.values() 
                                         if e.parent_id == entity.id])
                })
            elif entity.entity_type == "interface":
                packages[package]["interfaces"].append(entity.name)
            elif entity.entity_type == "function":
                packages[package]["functions"].append({
                    "name": entity.name,
                    "params": len(entity.parameters) if entity.parameters else 0,
                    "lines": entity.lines_of_code
                })
        
        # 2. 包依赖分析
        for rel in graph.relationships:
            source = graph.entities.get(rel.source_id)
            target = graph.entities.get(rel.target_id)
            if source and target:
                source_pkg = "/".join(source.file_path.replace("\\", "/").split("/")[:-1])
                target_pkg = "/".join(target.file_path.replace("\\", "/").split("/")[:-1])
                if source_pkg != target_pkg:
                    packages[source_pkg]["dependencies"].add(target_pkg)
        
        # 3. 生成Mermaid包图
        mermaid_diagram = "graph TB\n"
        for pkg_name, pkg_info in list(packages.items())[:15]:
            safe_name = pkg_name.replace("/", "_").replace("-", "_").replace(".", "_")
            class_count = len(pkg_info["classes"])
            func_count = len(pkg_info["functions"])
            mermaid_diagram += f'    {safe_name}["{pkg_name}\\n类:{class_count} 函数:{func_count}"]\n'
        
        for pkg_name, pkg_info in list(packages.items())[:15]:
            safe_source = pkg_name.replace("/", "_").replace("-", "_").replace(".", "_")
            for dep in list(pkg_info["dependencies"])[:5]:
                safe_dep = dep.replace("/", "_").replace("-", "_").replace(".", "_")
                if safe_dep in [p.replace("/", "_").replace("-", "_").replace(".", "_") 
                               for p in packages.keys()]:
                    mermaid_diagram += f"    {safe_source} --> {safe_dep}\n"
        
        # 4. 功能模块摘要
        functional_modules = []
        for pkg_name, pkg_info in sorted(packages.items(), 
                                         key=lambda x: len(x[1]["classes"]) + len(x[1]["functions"]), 
                                         reverse=True)[:10]:
            functional_modules.append({
                "name": pkg_name,
                "summary": f"{len(pkg_info['classes'])}个类, {len(pkg_info['functions'])}个函数",
                "key_entities": [c["name"] for c in pkg_info["classes"][:3]] + 
                               [f["name"] for f in pkg_info["functions"][:3]]
            })
        
        return JSONResponse(content={
            "packages": {
                k: {
                    "name": v["name"],
                    "classes": v["classes"][:10],
                    "functions": v["functions"][:10],
                    "dependencies": list(v["dependencies"])
                } for k, v in list(packages.items())[:20]
            },
            "mermaid_diagram": mermaid_diagram,
            "functional_modules": functional_modules,
            "statistics": {
                "total_packages": len(packages),
                "total_classes": sum(len(v["classes"]) for v in packages.values()),
                "total_functions": sum(len(v["functions"]) for v in packages.values())
            }
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/uml-design/detail")
async def get_uml_detail():
    """获取详细设计（UML类图风格）"""
    try:
        from src.gitnexus import GitNexusClient
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        # 1. 类详细设计
        class_details = []
        for entity in graph.entities.values():
            if entity.entity_type == "class":
                # 获取类的成员
                attributes = []
                methods = []
                
                for child_id in entity.children_ids if hasattr(entity, 'children_ids') else []:
                    child = graph.entities.get(child_id)
                    if child:
                        if child.entity_type in ["method", "function"]:
                            methods.append({
                                "name": child.name,
                                "params": child.parameters,
                                "return_type": child.return_type,
                                "visibility": child.visibility or "public"
                            })
                        elif child.entity_type in ["attribute", "field", "variable"]:
                            attributes.append({
                                "name": child.name,
                                "type": getattr(child, 'return_type', None),
                                "visibility": child.visibility or "private"
                            })
                
                # 获取继承关系
                inherits = []
                implements = []
                for rel in graph.relationships:
                    if rel.source_id == entity.id:
                        if rel.relationship_type in ["inherits", "extends"]:
                            parent = graph.entities.get(rel.target_id)
                            if parent:
                                inherits.append(parent.name)
                        elif rel.relationship_type == "implements":
                            iface = graph.entities.get(rel.target_id)
                            if iface:
                                implements.append(iface.name)
                
                class_details.append({
                    "id": entity.id,
                    "name": entity.name,
                    "file": entity.file_path,
                    "lines": entity.lines_of_code,
                    "docstring": entity.docstring,
                    "attributes": attributes[:10],
                    "methods": methods[:15],
                    "inherits": inherits,
                    "implements": implements
                })
        
        # 2. 接口定义
        interface_details = []
        for entity in graph.entities.values():
            if entity.entity_type == "interface":
                methods = []
                for child_id in entity.children_ids if hasattr(entity, 'children_ids') else []:
                    child = graph.entities.get(child_id)
                    if child and child.entity_type in ["method", "function"]:
                        methods.append({
                            "name": child.name,
                            "params": child.parameters,
                            "return_type": child.return_type
                        })
                
                interface_details.append({
                    "id": entity.id,
                    "name": entity.name,
                    "methods": methods
                })
        
        # 3. 生成Mermaid类图
        mermaid_diagram = "classDiagram\n"
        
        for cls in class_details[:15]:
            safe_name = cls["name"].replace(".", "_")
            # 类定义
            mermaid_diagram += f"    class {safe_name} {{\n"
            # 属性
            for attr in cls["attributes"][:5]:
                visibility = "+" if attr.get("visibility") == "public" else "-"
                mermaid_diagram += f"        {visibility}{attr['name']}\n"
            # 方法
            for method in cls["methods"][:8]:
                visibility = "+" if method.get("visibility") == "public" else "-"
                params = ", ".join([p.get("name", str(p)) if isinstance(p, dict) else str(p) 
                                   for p in (method.get("params") or [])])
                mermaid_diagram += f"        {visibility}{method['name']}({params})\n"
            mermaid_diagram += "    }\n"
        
        # 继承关系
        for cls in class_details[:15]:
            safe_name = cls["name"].replace(".", "_")
            for parent in cls["inherits"][:2]:
                safe_parent = parent.replace(".", "_")
                mermaid_diagram += f"    {safe_parent} <|-- {safe_name}\n"
            for iface in cls["implements"][:2]:
                safe_iface = iface.replace(".", "_")
                mermaid_diagram += f"    {safe_iface} <|.. {safe_name}\n"
        
        return JSONResponse(content={
            "classes": class_details[:20],
            "interfaces": interface_details[:10],
            "mermaid_diagram": mermaid_diagram,
            "statistics": {
                "total_classes": len(class_details),
                "total_interfaces": len(interface_details),
                "total_methods": sum(len(c["methods"]) for c in class_details)
            }
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/uml-design/software-flow")
async def get_software_flow():
    """获取软件脉络（UML序列图 + 活动图风格）"""
    try:
        from src.gitnexus import GitNexusClient
        from collections import defaultdict
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        # 1. 入口点识别
        entry_points = []
        for entity in graph.entities.values():
            if entity.entity_type in ["function", "method"]:
                # 判断是否为入口点（如main、handler、endpoint等）
                name_lower = entity.name.lower()
                if any(kw in name_lower for kw in ["main", "run", "start", "handle", "process", "execute"]):
                    entry_points.append({
                        "id": entity.id,
                        "name": entity.name,
                        "file": entity.file_path,
                        "type": "entry"
                    })
                # 被调用次数为0的可能是入口
                called_count = sum(1 for r in graph.relationships 
                                  if r.target_id == entity.id and r.relationship_type == "calls")
                if called_count == 0:
                    entry_points.append({
                        "id": entity.id,
                        "name": entity.name,
                        "file": entity.file_path,
                        "type": "potential_entry"
                    })
        
        # 2. 构建调用链
        def build_call_chain(entity_id, visited=None, depth=0):
            if visited is None:
                visited = set()
            if depth > 6 or entity_id in visited:
                return []
            
            visited.add(entity_id)
            entity = graph.entities.get(entity_id)
            if not entity:
                return []
            
            chain = [{"id": entity_id, "name": entity.name, "type": entity.entity_type}]
            
            # 获取调用的函数
            calls = [(r.target_id, r) for r in graph.relationships 
                    if r.source_id == entity_id and r.relationship_type == "calls"]
            
            if calls:
                for target_id, _ in calls[:3]:
                    sub_chain = build_call_chain(target_id, visited.copy(), depth + 1)
                    if sub_chain:
                        chain.extend(sub_chain)
                        break  # 只跟随第一个分支
            
            return chain
        
        call_chains = []
        for entry in entry_points[:10]:
            chain = build_call_chain(entry["id"])
            if len(chain) > 1:
                call_chains.append({
                    "entry": entry["name"],
                    "chain": chain,
                    "depth": len(chain)
                })
        
        # 3. 数据流分析
        data_flows = []
        for rel in graph.relationships:
            if rel.relationship_type in ["reads", "writes", "flows_to"]:
                source = graph.entities.get(rel.source_id)
                target = graph.entities.get(rel.target_id)
                if source and target:
                    data_flows.append({
                        "from": source.name,
                        "to": target.name,
                        "type": rel.relationship_type
                    })
        
        # 4. 生成Mermaid序列图
        mermaid_sequence = "sequenceDiagram\n"
        participants = set()
        
        for chain_info in call_chains[:3]:
            for node in chain_info["chain"][:8]:
                if node["name"] not in participants:
                    safe_name = node["name"].replace(".", "_").replace(" ", "_")
                    mermaid_sequence += f"    participant {safe_name}\n"
                    participants.add(node["name"])
        
        for chain_info in call_chains[:3]:
            chain = chain_info["chain"][:8]
            for i in range(len(chain) - 1):
                from_name = chain[i]["name"].replace(".", "_").replace(" ", "_")
                to_name = chain[i + 1]["name"].replace(".", "_").replace(" ", "_")
                mermaid_sequence += f"    {from_name}->>{to_name}: call\n"
        
        # 5. 生成Mermaid活动图
        mermaid_activity = "flowchart TD\n"
        activity_nodes = set()
        
        for chain_info in call_chains[:2]:
            chain = chain_info["chain"][:10]
            for i, node in enumerate(chain):
                safe_name = node["name"].replace(".", "_").replace(" ", "_")
                if safe_name not in activity_nodes:
                    shape = "[[" + node["name"] + "]]" if i == 0 else "[" + node["name"] + "]"
                    mermaid_activity += f"    {safe_name}{shape}\n"
                    activity_nodes.add(safe_name)
                
                if i < len(chain) - 1:
                    next_name = chain[i + 1]["name"].replace(".", "_").replace(" ", "_")
                    mermaid_activity += f"    {safe_name} --> {next_name}\n"
        
        return JSONResponse(content={
            "entry_points": entry_points[:15],
            "call_chains": call_chains[:10],
            "data_flows": data_flows[:20],
            "mermaid_sequence": mermaid_sequence,
            "mermaid_activity": mermaid_activity,
            "statistics": {
                "total_entry_points": len(entry_points),
                "total_call_chains": len(call_chains),
                "avg_chain_depth": sum(c["depth"] for c in call_chains) / len(call_chains) if call_chains else 0
            }
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/uml-design/complete")
async def get_complete_uml_design():
    """获取完整的UML设计文档"""
    try:
        from src.gitnexus import GitNexusClient
        from collections import defaultdict
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            return JSONResponse(content={"error": "暂无数据"})
        
        # 调用各个子模块API
        results = {}
        
        # 技术选型
        tech_stack = await get_technology_stack()
        results["technology_stack"] = tech_stack.body.decode() if hasattr(tech_stack, 'body') else {}
        
        # 架构设计
        architecture = await get_uml_architecture()
        results["architecture"] = architecture.body.decode() if hasattr(architecture, 'body') else {}
        
        # 概要设计
        outline = await get_uml_outline()
        results["outline"] = outline.body.decode() if hasattr(outline, 'body') else {}
        
        # 详细设计
        detail = await get_uml_detail()
        results["detail"] = detail.body.decode() if hasattr(detail, 'body') else {}
        
        # 软件脉络
        flow = await get_software_flow()
        results["software_flow"] = flow.body.decode() if hasattr(flow, 'body') else {}
        
        return JSONResponse(content={
            "project_name": graph.project_name,
            "generated_at": graph.generated_at,
            "design_document": results
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ============ 知识文档管理 API ============

@app.post("/api/knowledge-docs/generate")
async def generate_knowledge_documents():
    """生成知识文档（Skill格式）"""
    try:
        from src.gitnexus import GitNexusClient
        from src.core.knowledge_document_generator import KnowledgeDocumentGenerator
        
        client = GitNexusClient(str(CONFIG_PATH))
        graph = client._load_knowledge_graph()
        
        if not graph or not graph.entities:
            raise HTTPException(status_code=400, detail="暂无知识图谱数据，请先运行分析")
        
        # 生成知识文档
        generator = KnowledgeDocumentGenerator(output_dir=str(PROJECT_ROOT / "output" / "doc"))
        result = generator.generate_all_documents(graph)
        
        return JSONResponse(content={
            "status": "success",
            "message": "知识文档生成完成",
            "generated_files": {
                "index": result["index"],
                "entities_count": len(result["entities"]),
                "modules_count": len(result["modules"]),
                "architecture_count": len(result["architecture"]),
                "uml_count": len(result["uml"])
            },
            "output_directory": str(PROJECT_ROOT / "output" / "doc")
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/knowledge-docs/list")
async def list_knowledge_documents():
    """获取知识文档列表"""
    try:
        from src.core.knowledge_document_generator import KnowledgeDocumentGenerator
        
        generator = KnowledgeDocumentGenerator(output_dir=str(PROJECT_ROOT / "output" / "doc"))
        doc_list = generator.get_document_list()
        
        # 转换为相对路径便于前端展示
        base_path = str(PROJECT_ROOT / "output" / "doc")
        
        result = {
            "index": doc_list["index"],
            "categories": {},
            "statistics": doc_list["statistics"],
            "tree": []
        }
        
        # 构建树形结构
        tree_nodes = []
        
        # 实体文档
        if doc_list["entities"]:
            entities_tree = {
                "name": "entities",
                "label": "实体知识",
                "icon": "fa-cube",
                "children": []
            }
            
            # 按类型分组
            entities_by_type = defaultdict(list)
            for f in doc_list["entities"]:
                parts = f.replace(base_path, "").strip("\\/").split("\\")
                if len(parts) >= 2:
                    entity_type = parts[1]
                    entities_by_type[entity_type].append({
                        "name": Path(f).stem,
                        "path": f,
                        "relative_path": f.replace(base_path, "").strip("\\/")
                    })
            
            for etype, files in entities_by_type.items():
                type_node = {
                    "name": etype,
                    "label": f"{etype} ({len(files)})",
                    "icon": "fa-folder",
                    "children": files[:50]  # 限制数量
                }
                entities_tree["children"].append(type_node)
            
            result["categories"]["entities"] = len(doc_list["entities"])
            tree_nodes.append(entities_tree)
        
        # 模块文档
        if doc_list["modules"]:
            modules_tree = {
                "name": "modules",
                "label": f"模块知识 ({len(doc_list['modules'])})",
                "icon": "fa-puzzle-piece",
                "children": [{
                    "name": Path(f).stem,
                    "path": f,
                    "relative_path": f.replace(base_path, "").strip("\\/")
                } for f in doc_list["modules"][:50]]
            }
            result["categories"]["modules"] = len(doc_list["modules"])
            tree_nodes.append(modules_tree)
        
        # 架构文档
        if doc_list["architecture"]:
            arch_tree = {
                "name": "architecture",
                "label": f"架构知识 ({len(doc_list['architecture'])})",
                "icon": "fa-building",
                "children": [{
                    "name": Path(f).stem,
                    "path": f,
                    "relative_path": f.replace(base_path, "").strip("\\/")
                } for f in doc_list["architecture"]]
            }
            result["categories"]["architecture"] = len(doc_list["architecture"])
            tree_nodes.append(arch_tree)
        
        # UML文档
        if doc_list["uml"]:
            uml_tree = {
                "name": "uml",
                "label": f"UML设计 ({len(doc_list['uml'])})",
                "icon": "fa-drafting-compass",
                "children": [{
                    "name": Path(f).stem,
                    "path": f,
                    "relative_path": f.replace(base_path, "").strip("\\/")
                } for f in doc_list["uml"]]
            }
            result["categories"]["uml"] = len(doc_list["uml"])
            tree_nodes.append(uml_tree)
        
        result["tree"] = tree_nodes
        
        return JSONResponse(content=result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/knowledge-docs/content")
async def get_knowledge_document_content(path: str):
    """获取知识文档内容"""
    try:
        from src.core.knowledge_document_generator import KnowledgeDocumentGenerator
        
        # 安全检查：确保路径在doc目录内
        doc_dir = (PROJECT_ROOT / "output" / "doc").resolve()
        target_path = Path(path).resolve()
        
        if not str(target_path).startswith(str(doc_dir)):
            raise HTTPException(status_code=403, detail="访问路径不允许")
        
        if not target_path.exists():
            raise HTTPException(status_code=404, detail="文档不存在")
        
        generator = KnowledgeDocumentGenerator(output_dir=str(doc_dir))
        content = generator.get_document_content(str(target_path))
        
        if content is None:
            raise HTTPException(status_code=404, detail="无法读取文档内容")
        
        # 解析元数据
        metadata = {}
        if content.startswith("---"):
            parts = content.split("---", 2)
            if len(parts) >= 3:
                for line in parts[1].strip().split("\n"):
                    if ":" in line:
                        key, value = line.split(":", 1)
                        metadata[key.strip()] = value.strip()
        
        return JSONResponse(content={
            "path": str(target_path),
            "relative_path": str(target_path.relative_to(doc_dir)),
            "metadata": metadata,
            "content": content,
            "size": len(content)
        })
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/knowledge-docs/search")
async def search_knowledge_documents(query: str):
    """搜索知识文档"""
    try:
        from src.core.knowledge_document_generator import KnowledgeDocumentGenerator
        
        doc_dir = PROJECT_ROOT / "output" / "doc"
        generator = KnowledgeDocumentGenerator(output_dir=str(doc_dir))
        doc_list = generator.get_document_list()
        
        results = []
        query_lower = query.lower()
        
        # 搜索所有文档
        all_docs = (doc_list.get("entities", []) + 
                   doc_list.get("modules", []) + 
                   doc_list.get("architecture", []) + 
                   doc_list.get("uml", []))
        
        for doc_path in all_docs:
            content = generator.get_document_content(doc_path)
            if content and query_lower in content.lower():
                # 提取匹配上下文
                content_lower = content.lower()
                idx = content_lower.find(query_lower)
                
                if idx >= 0:
                    start = max(0, idx - 100)
                    end = min(len(content), idx + len(query) + 100)
                    context = content[start:end]
                    
                    results.append({
                        "path": doc_path,
                        "relative_path": str(Path(doc_path).relative_to(doc_dir)),
                        "name": Path(doc_path).stem,
                        "context": context,
                        "match_position": idx
                    })
        
        # 限制结果数量
        results = results[:50]
        
        return JSONResponse(content={
            "query": query,
            "total_results": len(results),
            "results": results
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/knowledge-docs/tree")
async def get_knowledge_docs_tree():
    """获取知识文档树形结构"""
    try:
        from src.core.knowledge_document_generator import KnowledgeDocumentGenerator
        
        doc_dir = PROJECT_ROOT / "output" / "doc"
        generator = KnowledgeDocumentGenerator(output_dir=str(doc_dir))
        doc_list = generator.get_document_list()
        
        base_path = str(doc_dir)
        
        def build_tree_node(name: str, label: str, icon: str, files: list, max_items: int = 100):
            """构建树节点"""
            children = []
            for f in files[:max_items]:
                rel_path = f.replace(base_path, "").strip("\\/")
                children.append({
                    "name": Path(f).stem,
                    "path": f,
                    "relative_path": rel_path,
                    "icon": "fa-file-alt",
                    "is_file": True
                })
            
            if len(files) > max_items:
                children.append({
                    "name": f"... 还有 {len(files) - max_items} 项",
                    "path": "",
                    "icon": "fa-ellipsis-h",
                    "is_file": False
                })
            
            return {
                "name": name,
                "label": f"{label} ({len(files)})",
                "icon": icon,
                "is_file": False,
                "children": children
            }
        
        tree = []
        
        # 索引文件
        if doc_list["index"]:
            tree.append({
                "name": "index",
                "label": "知识索引",
                "path": doc_list["index"],
                "relative_path": "index.md",
                "icon": "fa-home",
                "is_file": True
            })
        
        # 各类文档
        if doc_list["entities"]:
            tree.append(build_tree_node("entities", "实体知识", "fa-cube", doc_list["entities"], 50))
        
        if doc_list["modules"]:
            tree.append(build_tree_node("modules", "模块知识", "fa-puzzle-piece", doc_list["modules"], 30))
        
        if doc_list["architecture"]:
            tree.append(build_tree_node("architecture", "架构知识", "fa-building", doc_list["architecture"]))
        
        if doc_list["uml"]:
            tree.append(build_tree_node("uml", "UML设计", "fa-drafting-compass", doc_list["uml"]))
        
        return JSONResponse(content={
            "tree": tree,
            "statistics": doc_list["statistics"]
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
