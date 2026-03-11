"""
工作流引擎 - 基于design-v1方案的Agent任务编排和流水线执行

功能：
- Agent任务编排与调度
- 流水线执行管理
- 任务依赖关系处理
- 执行状态监控
- 错误处理与重试机制
"""

import asyncio
from typing import Dict, Any, List, Optional, Callable
from enum import Enum
from dataclasses import dataclass, field
import logging
from datetime import datetime

from src.agents import (
    FileScannerAgent, CodeParserAgent, SemanticExtractorAgent,
    ArchitectureAnalyzerAgent, BusinessLogicAgent, DocumentationAgent
)
from src.infrastructure.config_manager import get_config_manager


class WorkflowStatus(Enum):
    """工作流状态枚举"""
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


@dataclass
class WorkflowStep:
    """工作流步骤"""
    name: str
    agent_class: Callable
    dependencies: List[str] = field(default_factory=list)
    config: Dict[str, Any] = field(default_factory=dict)
    timeout: int = 300  # 5分钟默认超时
    retry_count: int = 3


@dataclass
class WorkflowResult:
    """工作流执行结果"""
    workflow_id: str
    status: WorkflowStatus
    start_time: datetime
    end_time: Optional[datetime] = None
    results: Dict[str, Any] = field(default_factory=dict)
    errors: Dict[str, str] = field(default_factory=dict)
    execution_time: Optional[float] = None


class WorkflowEngine:
    """工作流引擎"""
    
    def __init__(self):
        self.config_manager = get_config_manager()
        self.logger = logging.getLogger("workflow.engine")
        
        # 工作流定义
        self.workflows = {
            "full_analysis": self._create_full_analysis_workflow(),
            "quick_scan": self._create_quick_scan_workflow(),
            "architecture_only": self._create_architecture_only_workflow(),
            "business_only": self._create_business_only_workflow()
        }
        
        # 执行状态跟踪
        self.running_workflows: Dict[str, WorkflowResult] = {}
    
    def _create_full_analysis_workflow(self) -> List[WorkflowStep]:
        """创建完整分析工作流"""
        return [
            WorkflowStep(
                name="file_scanning",
                agent_class=FileScannerAgent,
                config=self.config_manager.get_agent_config("file_scanner"),
                dependencies=[]
            ),
            WorkflowStep(
                name="code_parsing",
                agent_class=CodeParserAgent,
                config=self.config_manager.get_agent_config("code_parser"),
                dependencies=["file_scanning"]
            ),
            WorkflowStep(
                name="semantic_extraction",
                agent_class=SemanticExtractorAgent,
                config=self.config_manager.get_agent_config("semantic_extractor"),
                dependencies=["code_parsing"]
            ),
            WorkflowStep(
                name="architecture_analysis",
                agent_class=ArchitectureAnalyzerAgent,
                config=self.config_manager.get_agent_config("architecture_analyzer"),
                dependencies=["code_parsing", "semantic_extraction"]
            ),
            WorkflowStep(
                name="business_logic_analysis",
                agent_class=BusinessLogicAgent,
                config=self.config_manager.get_agent_config("business_logic"),
                dependencies=["semantic_extraction"]
            ),
            WorkflowStep(
                name="documentation_generation",
                agent_class=DocumentationAgent,
                config=self.config_manager.get_agent_config("documentation"),
                dependencies=["architecture_analysis", "business_logic_analysis"]
            )
        ]
    
    def _create_quick_scan_workflow(self) -> List[WorkflowStep]:
        """创建快速扫描工作流"""
        return [
            WorkflowStep(
                name="file_scanning",
                agent_class=FileScannerAgent,
                config=self.config_manager.get_agent_config("file_scanner"),
                dependencies=[]
            ),
            WorkflowStep(
                name="code_parsing",
                agent_class=CodeParserAgent,
                config=self.config_manager.get_agent_config("code_parser"),
                dependencies=["file_scanning"]
            )
        ]
    
    def _create_architecture_only_workflow(self) -> List[WorkflowStep]:
        """创建仅架构分析工作流"""
        return [
            WorkflowStep(
                name="file_scanning",
                agent_class=FileScannerAgent,
                config=self.config_manager.get_agent_config("file_scanner"),
                dependencies=[]
            ),
            WorkflowStep(
                name="code_parsing",
                agent_class=CodeParserAgent,
                config=self.config_manager.get_agent_config("code_parser"),
                dependencies=["file_scanning"]
            ),
            WorkflowStep(
                name="architecture_analysis",
                agent_class=ArchitectureAnalyzerAgent,
                config=self.config_manager.get_agent_config("architecture_analyzer"),
                dependencies=["code_parsing"]
            )
        ]
    
    def _create_business_only_workflow(self) -> List[WorkflowStep]:
        """创建仅业务分析工作流"""
        return [
            WorkflowStep(
                name="file_scanning",
                agent_class=FileScannerAgent,
                config=self.config_manager.get_agent_config("file_scanner"),
                dependencies=[]
            ),
            WorkflowStep(
                name="code_parsing",
                agent_class=CodeParserAgent,
                config=self.config_manager.get_agent_config("code_parser"),
                dependencies=["file_scanning"]
            ),
            WorkflowStep(
                name="semantic_extraction",
                agent_class=SemanticExtractorAgent,
                config=self.config_manager.get_agent_config("semantic_extractor"),
                dependencies=["code_parsing"]
            ),
            WorkflowStep(
                name="business_logic_analysis",
                agent_class=BusinessLogicAgent,
                config=self.config_manager.get_agent_config("business_logic"),
                dependencies=["semantic_extraction"]
            )
        ]
    
    async def execute_workflow(self, workflow_name: str, input_data: Any, workflow_id: str = None) -> WorkflowResult:
        """执行工作流"""
        
        if workflow_name not in self.workflows:
            raise ValueError(f"未知的工作流: {workflow_name}")
        
        # 生成工作流ID
        if not workflow_id:
            import uuid
            workflow_id = str(uuid.uuid4())
        
        # 创建工作流结果
        workflow_result = WorkflowResult(
            workflow_id=workflow_id,
            status=WorkflowStatus.RUNNING,
            start_time=datetime.now()
        )
        
        # 记录执行状态
        self.running_workflows[workflow_id] = workflow_result
        
        self.logger.info(f"开始执行工作流: {workflow_name} (ID: {workflow_id})")
        
        try:
            # 获取工作流定义
            workflow_steps = self.workflows[workflow_name]
            
            # 执行工作流步骤
            await self._execute_workflow_steps(workflow_steps, input_data, workflow_result)
            
            # 工作流执行成功
            workflow_result.status = WorkflowStatus.COMPLETED
            workflow_result.end_time = datetime.now()
            workflow_result.execution_time = (workflow_result.end_time - workflow_result.start_time).total_seconds()
            
            self.logger.info(f"工作流执行完成: {workflow_name} (ID: {workflow_id})")
            
        except Exception as e:
            # 工作流执行失败
            workflow_result.status = WorkflowStatus.FAILED
            workflow_result.end_time = datetime.now()
            workflow_result.errors["workflow"] = str(e)
            
            self.logger.error(f"工作流执行失败: {workflow_name} (ID: {workflow_id}): {e}")
        
        finally:
            # 清理执行状态
            if workflow_id in self.running_workflows:
                del self.running_workflows[workflow_id]
        
        return workflow_result
    
    async def _execute_workflow_steps(self, workflow_steps: List[WorkflowStep], input_data: Any, workflow_result: WorkflowResult):
        """执行工作流步骤"""
        
        # 步骤执行结果缓存
        step_results = {}
        
        for step in workflow_steps:
            self.logger.info(f"执行步骤: {step.name}")
            
            try:
                # 检查依赖是否满足
                if not self._check_dependencies(step.dependencies, step_results):
                    raise Exception(f"步骤 {step.name} 的依赖未满足: {step.dependencies}")
                
                # 准备步骤输入数据
                step_input = self._prepare_step_input(step, input_data, step_results)
                
                # 执行步骤
                step_result = await self._execute_step_with_retry(step, step_input)
                
                # 记录步骤结果
                step_results[step.name] = step_result
                workflow_result.results[step.name] = step_result
                
                self.logger.info(f"步骤执行成功: {step.name}")
                
            except Exception as e:
                self.logger.error(f"步骤执行失败: {step.name}: {e}")
                workflow_result.errors[step.name] = str(e)
                raise
    
    def _check_dependencies(self, dependencies: List[str], step_results: Dict[str, Any]) -> bool:
        """检查步骤依赖是否满足"""
        for dep in dependencies:
            if dep not in step_results:
                return False
        return True
    
    def _prepare_step_input(self, step: WorkflowStep, initial_input: Any, step_results: Dict[str, Any]) -> Any:
        """准备步骤输入数据"""
        
        # 第一个步骤使用初始输入
        if not step.dependencies:
            return initial_input
        
        # 后续步骤使用依赖步骤的结果
        input_data = {}
        
        # 添加所有依赖步骤的结果
        for dep in step.dependencies:
            if dep in step_results:
                input_data[dep] = step_results[dep]
        
        # 根据步骤类型添加特定数据
        if step.name == "code_parsing":
            # 代码解析需要文件扫描结果
            if "file_scanning" in step_results:
                input_data["file_metadata_list"] = step_results["file_scanning"]
        
        elif step.name == "semantic_extraction":
            # 语义提取需要代码解析结果
            if "code_parsing" in step_results:
                input_data["code_metadata_list"] = step_results["code_parsing"]
        
        elif step.name == "architecture_analysis":
            # 架构分析需要代码解析和语义提取结果
            if "code_parsing" in step_results:
                input_data["code_metadata_list"] = step_results["code_parsing"]
            if "semantic_extraction" in step_results:
                input_data["semantic_contracts"] = step_results["semantic_extraction"]
        
        elif step.name == "business_logic_analysis":
            # 业务逻辑分析需要语义提取结果
            if "semantic_extraction" in step_results:
                input_data["semantic_contracts"] = step_results["semantic_extraction"]
            if "code_parsing" in step_results:
                input_data["code_metadata_list"] = step_results["code_parsing"]
        
        elif step.name == "documentation_generation":
            # 文档生成需要所有分析结果
            input_data["analysis_results"] = {
                "code_metadata": step_results.get("code_parsing", []),
                "semantic_contracts": step_results.get("semantic_extraction", []),
                "dependency_graph": step_results.get("architecture_analysis", {}),
                "business_flows": step_results.get("business_logic_analysis", [])
            }
            input_data["project_info"] = {
                "name": "分析项目",
                "version": "1.0.0"
            }
        
        return input_data
    
    async def _execute_step_with_retry(self, step: WorkflowStep, input_data: Any) -> Any:
        """带重试机制的步骤执行"""
        
        last_error = None
        
        for attempt in range(step.retry_count):
            try:
                # 创建Agent实例
                agent = step.agent_class(step.config)
                
                # 检查Agent可用性
                if not agent.is_available():
                    raise Exception(f"Agent {step.name} 不可用")
                
                # 设置执行超时
                async with asyncio.timeout(step.timeout):
                    result = await agent.execute(input_data)
                
                # 检查执行结果
                if not result.success:
                    raise Exception(f"Agent执行失败: {result.error}")
                
                return result.data
                
            except asyncio.TimeoutError:
                last_error = Exception(f"步骤 {step.name} 执行超时")
                self.logger.warning(f"步骤 {step.name} 第 {attempt + 1} 次尝试超时")
                
            except Exception as e:
                last_error = e
                self.logger.warning(f"步骤 {step.name} 第 {attempt + 1} 次尝试失败: {e}")
            
            # 如果不是最后一次尝试，等待后重试
            if attempt < step.retry_count - 1:
                await asyncio.sleep(2 ** attempt)  # 指数退避
        
        # 所有重试都失败
        raise last_error if last_error else Exception("步骤执行失败")
    
    def get_workflow_status(self, workflow_id: str) -> Optional[WorkflowResult]:
        """获取工作流状态"""
        return self.running_workflows.get(workflow_id)
    
    def list_available_workflows(self) -> List[str]:
        """列出可用工作流"""
        return list(self.workflows.keys())
    
    def get_workflow_definition(self, workflow_name: str) -> Optional[List[WorkflowStep]]:
        """获取工作流定义"""
        return self.workflows.get(workflow_name)
    
    async def cancel_workflow(self, workflow_id: str) -> bool:
        """取消工作流执行"""
        if workflow_id in self.running_workflows:
            workflow_result = self.running_workflows[workflow_id]
            workflow_result.status = WorkflowStatus.CANCELLED
            workflow_result.end_time = datetime.now()
            return True
        return False


# 全局工作流引擎实例
_workflow_engine: Optional[WorkflowEngine] = None


def get_workflow_engine() -> WorkflowEngine:
    """获取全局工作流引擎实例"""
    global _workflow_engine
    if _workflow_engine is None:
        _workflow_engine = WorkflowEngine()
    return _workflow_engine