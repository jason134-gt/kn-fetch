"""
Agent消息

定义Agent间通信的消息格式
"""
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Dict, Optional


@dataclass
class AgentMessage:
    """Agent间消息
    
    用于Agent之间的信息交换
    
    Attributes:
        from_agent: 发送方Agent名称
        to_agent: 接收方Agent名称
        message_type: 消息类型
        content: 消息内容
        timestamp: 时间戳
        priority: 优先级（1-10，数字越大优先级越高）
    """
    from_agent: str              # 发送方Agent
    to_agent: str                # 接收方Agent
    message_type: str            # 消息类型
    content: Dict[str, Any]      # 消息内容
    timestamp: Optional[str] = None
    priority: int = 5            # 默认优先级
    
    def __post_init__(self):
        """初始化后处理"""
        if self.timestamp is None:
            self.timestamp = datetime.now().isoformat()
    
    def to_json(self) -> Dict[str, Any]:
        """
        转换为JSON格式
        
        Returns:
            JSON字典
        """
        return {
            "from_agent": self.from_agent,
            "to_agent": self.to_agent,
            "message_type": self.message_type,
            "content": self.content,
            "timestamp": self.timestamp,
            "priority": self.priority
        }
    
    @classmethod
    def from_json(cls, data: Dict[str, Any]) -> 'AgentMessage':
        """
        从JSON创建消息
        
        Args:
            data: JSON字典
            
        Returns:
            AgentMessage实例
        """
        return cls(
            from_agent=data["from_agent"],
            to_agent=data["to_agent"],
            message_type=data["message_type"],
            content=data["content"],
            timestamp=data.get("timestamp"),
            priority=data.get("priority", 5)
        )
    
    def __repr__(self) -> str:
        return f"<AgentMessage({self.from_agent} -> {self.to_agent}, type='{self.message_type}')>"
