"""
Agent结果

定义Agent执行的返回结果
"""
from typing import Dict, Any, Optional, List
from dataclasses import dataclass, field


@dataclass
class AgentResult:
    """Agent执行结果
    
    封装Agent执行的返回信息
    
    Attributes:
        agent_name: Agent名称
        status: 执行状态（completed/partial/failed）
        output_file: 输出文件路径
        summary: 结果摘要（50字以内）
        cross_module_notes: 跨模块备注列表
        stats: 统计信息
        error: 错误信息
        execution_time: 执行时间（秒）
    """
    agent_name: str              # Agent名称
    status: str                  # completed/partial/failed
    output_file: Optional[str]   # 输出文件路径
    summary: str                 # 50字以内的摘要
    cross_module_notes: List[str] = field(default_factory=list)
    stats: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None
    execution_time: float = 0.0
    
    def to_json(self) -> Dict[str, Any]:
        """
        转换为JSON格式
        
        Returns:
            JSON字典
        """
        return {
            "status": self.status,
            "output_file": self.output_file,
            "summary": self.summary,
            "cross_module_notes": self.cross_module_notes,
            "stats": self.stats
        }
    
    def is_success(self) -> bool:
        """
        判断是否执行成功
        
        Returns:
            是否成功
        """
        return self.status == "completed"
    
    def __repr__(self) -> str:
        return f"<AgentResult(agent='{self.agent_name}', status='{self.status}', time={self.execution_time:.2f}s)>"
