"""
Agent基础类 - 基于design-v1方案的Agent抽象基类

实现Agent统一接口，支持异步执行、状态管理、错误处理等核心功能
"""

from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional, Union
from enum import Enum
import asyncio
import logging
from datetime import datetime
from dataclasses import dataclass


class AgentStatus(Enum):
    """Agent状态枚举"""
    IDLE = "idle"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


@dataclass
class AgentResult:
    """Agent执行结果"""
    success: bool
    data: Optional[Any] = None
    error: Optional[str] = None
    execution_time: float = 0.0
    metadata: Optional[Dict[str, Any]] = None


class BaseAgent(ABC):
    """Agent抽象基类"""
    
    def __init__(self, name: str, config: Dict[str, Any]):
        self.name = name
        self.config = config
        self.status = AgentStatus.IDLE
        self.logger = logging.getLogger(f"agent.{name}")
        self._start_time: Optional[datetime] = None
        self._result: Optional[AgentResult] = None
    
    async def execute(self, input_data: Any) -> AgentResult:
        """执行Agent任务（异步）"""
        self.status = AgentStatus.RUNNING
        self._start_time = datetime.now()
        
        try:
            self.logger.info(f"Agent {self.name} 开始执行")
            result = await self._execute_impl(input_data)
            
            # 计算执行时间
            execution_time = (datetime.now() - self._start_time).total_seconds()
            
            self._result = AgentResult(
                success=True,
                data=result,
                execution_time=execution_time,
                metadata={"agent_name": self.name}
            )
            
            self.status = AgentStatus.COMPLETED
            self.logger.info(f"Agent {self.name} 执行完成，耗时: {execution_time:.2f}s")
            return self._result
            
        except Exception as e:
            execution_time = (datetime.now() - self._start_time).total_seconds()
            error_msg = f"Agent {self.name} 执行失败: {str(e)}"
            
            self._result = AgentResult(
                success=False,
                error=error_msg,
                execution_time=execution_time
            )
            
            self.status = AgentStatus.FAILED
            self.logger.error(error_msg)
            return self._result
    
    @abstractmethod
    async def _execute_impl(self, input_data: Any) -> Any:
        """Agent具体实现（子类需要实现）"""
        pass
    
    def get_status(self) -> AgentStatus:
        """获取当前状态"""
        return self.status
    
    def get_result(self) -> Optional[AgentResult]:
        """获取执行结果"""
        return self._result
    
    def is_available(self) -> bool:
        """检查Agent是否可用"""
        return True
    
    def get_metrics(self) -> Dict[str, Any]:
        """获取Agent性能指标"""
        metrics = {
            "name": self.name,
            "status": self.status.value,
            "start_time": self._start_time.isoformat() if self._start_time else None
        }
        
        if self._result:
            metrics.update({
                "execution_time": self._result.execution_time,
                "success": self._result.success
            })
        
        return metrics