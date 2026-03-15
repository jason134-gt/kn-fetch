"""
Agent基类和角色定义

提供Agent的基础抽象类和角色配置
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, Optional, List
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class AgentRole:
    """Agent角色定义
    
    定义Agent的专业角色、关注点和约束条件
    
    Attributes:
        name: Agent名称（唯一标识）
        role: 角色描述（中文）
        focus: 关注点列表
        constraint: 约束条件
        output_template: 输出模板路径
    """
    name: str                    # 角色名称
    role: str                    # 角色描述（中文）
    focus: str                   # 关注点
    constraint: str              # 约束条件
    output_template: str = ""    # 输出模板路径


class BaseAgent(ABC):
    """Agent基类
    
    所有专业Agent的抽象基类，定义统一的接口和通用方法
    
    Attributes:
        role: Agent角色配置
        name: Agent名称
        logger: 日志记录器
    """
    
    def __init__(self, role: AgentRole):
        """
        初始化Agent
        
        Args:
            role: Agent角色配置
        """
        import logging
        self.role = role
        self.name = role.name
        self.logger = logging.getLogger(f"{__name__}.{self.name}")
    
    @abstractmethod
    def execute(self, context):
        """
        执行分析任务（子类必须实现）
        
        Args:
            context: Agent执行上下文
            
        Returns:
            Agent执行结果
        """
        pass
    
    def _build_prompt(self, context: 'AgentContext') -> str:
        """
        构建LLM提示词
        
        Args:
            context: Agent执行上下文
            
        Returns:
            完整的提示词字符串
        """
        prompt = f"""[SPEC]
首先读取规范文件：
- Read: {context.output_dir}/specs/quality-standards.md
- Read: {context.output_dir}/specs/writing-style.md
严格遵循规范中的质量标准和段落式写作要求。

[ROLE] {self.role.role}

[TASK]
{self._get_task_description(context)}
输出: {context.get_output_file(self.name)}

[STYLE]
- 严谨专业的中文技术写作，专业术语保留英文
- 完全客观的第三人称视角，严禁"我们"、"开发者"
- 段落式叙述，采用"论点-论据-结论"结构
- 善用逻辑连接词体现设计推演过程

[FOCUS]
{self.role.focus}

[CONSTRAINT]
{self.role.constraint}

[RETURN JSON]
{{"status":"completed","output_file":"section-{self.name}.md","summary":"<50字>","cross_module_notes":[],"stats":{{}}}}
"""
        return prompt
    
    @abstractmethod
    def _get_task_description(self, context: 'AgentContext') -> str:
        """
        获取任务描述（子类必须实现）
        
        Args:
            context: Agent执行上下文
            
        Returns:
            任务描述字符串
        """
        pass
    
    def _save_output(self, content: str, output_file: Path):
        """
        保存输出文件
        
        Args:
            content: 文件内容
            output_file: 输出文件路径
        """
        output_file.parent.mkdir(parents=True, exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(content)
    
    def _extract_cross_module_notes(self, content: str) -> List[str]:
        """
        提取跨模块备注
        
        从生成的内容中提取涉及跨模块的信息
        
        Args:
            content: 生成的内容
            
        Returns:
            跨模块备注列表
        """
        notes = []
        lines = content.split('\n')
        for line in lines:
            # 简单实现：提取包含"模块"、"依赖"、"调用"关键词的句子
            if any(keyword in line for keyword in ['模块', '依赖', '调用', '集成', '接口']) and len(line) > 20:
                notes.append(line.strip())
        return notes[:5]  # 最多返回5条
    
    def __repr__(self) -> str:
        return f"<{self.__class__.__name__}(name='{self.name}', role='{self.role.role}')>"


@dataclass
class AgentResult:
    """Agent执行结果
    
    封装Agent执行的返回信息
    
    Attributes:
        success: 是否执行成功
        data: 执行结果数据
        output: 输出内容
        metrics: 性能指标
        error: 错误信息
        cross_notes: 跨模块备注
    """
    success: bool                    # 是否执行成功
    data: Optional[Dict[str, Any]] = None    # 执行结果数据
    output: Optional[str] = None     # 输出内容
    metrics: Optional[Dict[str, Any]] = None # 性能指标
    error: Optional[str] = None       # 错误信息
    cross_notes: Optional[List[str]] = None # 跨模块备注


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
    timestamp: str = field(default_factory=lambda: __import__('datetime').datetime.now().isoformat())
    
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
