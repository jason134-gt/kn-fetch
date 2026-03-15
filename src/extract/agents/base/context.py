"""
Agent上下文

定义Agent执行时的上下文环境
"""
from typing import Dict, Any, Optional
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path


@dataclass
class AgentContext:
    """Agent执行上下文
    
    提供Agent执行所需的全部环境和数据
    
    Attributes:
        graph: 知识图谱对象
        config: 分析配置
        output_dir: 输出目录
        exploration_data: 探索结果数据映射
        session_id: 会话ID
        timestamp: 时间戳
    """
    graph: Any                   # KnowledgeGraph
    config: Any                  # AnalysisConfig
    output_dir: Path             # 输出目录
    exploration_data: Dict[str, Path] = field(default_factory=dict)
    session_id: str = ""
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    
    def get_exploration_file(self, angle: str) -> Optional[Path]:
        """
        获取探索文件路径
        
        Args:
            angle: 探索角度名称
            
        Returns:
            探索文件路径，如果不存在返回None
        """
        return self.exploration_data.get(angle)
    
    def get_output_file(self, agent_name: str) -> Path:
        """
        获取Agent输出文件路径
        
        Args:
            agent_name: Agent名称
            
        Returns:
            输出文件路径
        """
        return self.output_dir / "sections" / f"section-{agent_name}.md"
    
    def get_spec_file(self, spec_name: str) -> Path:
        """
        获取规范文件路径
        
        Args:
            spec_name: 规范文件名（不含扩展名）
            
        Returns:
            规范文件路径
        """
        return self.output_dir / "specs" / f"{spec_name}.md"
    
    def __repr__(self) -> str:
        return f"<AgentContext(session_id='{self.session_id}', output_dir='{self.output_dir}')>"
