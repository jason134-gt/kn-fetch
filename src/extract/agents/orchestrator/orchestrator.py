"""
Agent编排器

管理和协调多个Agent的执行
"""
from typing import Dict, List, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
import time
import logging
from pathlib import Path

from ..base.agent import BaseAgent, AgentContext, AgentResult
from ..base.message import AgentMessage

logger = logging.getLogger(__name__)


class AgentOrchestrator:
    """Agent编排器
    
    负责Agent的注册、调度和并行执行
    
    Attributes:
        agents: 已注册的Agent字典
        max_workers: 最大并行工作线程数
        message_queue: Agent间消息队列
    """
    
    def __init__(self, max_workers: int = 5):
        """
        初始化编排器
        
        Args:
            max_workers: 最大并行工作线程数
        """
        self.agents: Dict[str, BaseAgent] = {}
        self.max_workers = max_workers
        self.message_queue: List[AgentMessage] = []
        self.logger = logging.getLogger(f"{__name__}.AgentOrchestrator")
    
    def register_agent(self, name: str, agent: BaseAgent) -> None:
        """
        注册Agent
        
        Args:
            name: Agent名称
            agent: Agent实例
            
        Raises:
            ValueError: 如果Agent名称已存在
        """
        if name in self.agents:
            raise ValueError(f"Agent '{name}' already registered")
        
        self.agents[name] = agent
        self.logger.info(f"✓ Registered agent: {name} ({agent.role.role})")
    
    def register_agents(self, agents: Dict[str, BaseAgent]) -> None:
        """
        批量注册Agent
        
        Args:
            agents: Agent字典 {name: agent}
        """
        for name, agent in agents.items():
            self.register_agent(name, agent)
    
    def unregister_agent(self, name: str) -> bool:
        """
        注销Agent
        
        Args:
            name: Agent名称
            
        Returns:
            是否成功注销
        """
        if name in self.agents:
            del self.agents[name]
            self.logger.info(f"Unregistered agent: {name}")
            return True
        return False
    
    def has_agent(self, name: str) -> bool:
        """
        检查Agent是否已注册
        
        Args:
            name: Agent名称
            
        Returns:
            是否已注册
        """
        return name in self.agents
    
    def get_agent(self, name: str) -> Optional[BaseAgent]:
        """
        获取Agent实例
        
        Args:
            name: Agent名称
            
        Returns:
            Agent实例，如果不存在返回None
        """
        return self.agents.get(name)
    
    def run_single(self, agent_name: str, context: AgentContext) -> AgentResult:
        """
        执行单个Agent
        
        Args:
            agent_name: Agent名称
            context: Agent执行上下文
            
        Returns:
            Agent执行结果
            
        Raises:
            ValueError: 如果Agent不存在
        """
        if agent_name not in self.agents:
            raise ValueError(f"Agent '{agent_name}' not found")
        
        agent = self.agents[agent_name]
        start_time = time.time()
        
        try:
            self.logger.info(f"▶ Running agent: {agent_name}")
            result = agent.analyze(context)
            result.execution_time = time.time() - start_time
            
            if result.is_success():
                self.logger.info(f"✓ Agent {agent_name} completed in {result.execution_time:.2f}s")
            else:
                self.logger.warning(f"⚠ Agent {agent_name} finished with status: {result.status}")
            
            return result
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.logger.error(f"✗ Agent {agent_name} failed: {str(e)}")
            
            return AgentResult(
                agent_name=agent_name,
                status="failed",
                output_file=None,
                summary="",
                error=str(e),
                execution_time=execution_time
            )
    
    def run_parallel(
        self, 
        agent_names: List[str], 
        context: AgentContext,
        fail_fast: bool = False
    ) -> Dict[str, AgentResult]:
        """
        并行执行多个Agent
        
        Args:
            agent_names: Agent名称列表
            context: Agent执行上下文
            fail_fast: 是否在第一个失败时立即停止
            
        Returns:
            Agent执行结果字典 {agent_name: result}
        """
        self.logger.info(f"🚀 Starting parallel execution of {len(agent_names)} agents")
        
        # 验证所有Agent都存在
        missing_agents = [name for name in agent_names if name not in self.agents]
        if missing_agents:
            raise ValueError(f"Agents not found: {missing_agents}")
        
        results: Dict[str, AgentResult] = {}
        start_time = time.time()
        
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            # 提交所有任务
            future_to_agent = {
                executor.submit(self.run_single, name, context): name 
                for name in agent_names
            }
            
            # 收集结果
            for future in as_completed(future_to_agent):
                agent_name = future_to_agent[future]
                
                try:
                    result = future.result()
                    results[agent_name] = result
                    
                    # 如果启用fail_fast且执行失败，取消其他任务
                    if fail_fast and not result.is_success():
                        self.logger.error(f"Fail-fast triggered by agent: {agent_name}")
                        for f in future_to_agent:
                            if not f.done():
                                f.cancel()
                        break
                        
                except Exception as e:
                    self.logger.error(f"✗ Agent {agent_name} execution failed: {str(e)}")
                    results[agent_name] = AgentResult(
                        agent_name=agent_name,
                        status="failed",
                        output_file=None,
                        summary="",
                        error=str(e)
                    )
        
        total_time = time.time() - start_time
        
        # 统计结果
        success_count = sum(1 for r in results.values() if r.is_success())
        failed_count = len(results) - success_count
        
        self.logger.info(
            f"✓ Parallel execution completed in {total_time:.2f}s: "
            f"{success_count} succeeded, {failed_count} failed"
        )
        
        return results
    
    def run_sequential(
        self, 
        agent_names: List[str], 
        context: AgentContext
    ) -> Dict[str, AgentResult]:
        """
        顺序执行多个Agent
        
        Args:
            agent_names: Agent名称列表
            context: Agent执行上下文
            
        Returns:
            Agent执行结果字典 {agent_name: result}
        """
        self.logger.info(f"▶ Starting sequential execution of {len(agent_names)} agents")
        
        results: Dict[str, AgentResult] = {}
        start_time = time.time()
        
        for agent_name in agent_names:
            result = self.run_single(agent_name, context)
            results[agent_name] = result
            
            # 如果执行失败，记录但继续
            if not result.is_success():
                self.logger.warning(f"Agent {agent_name} failed, continuing...")
        
        total_time = time.time() - start_time
        success_count = sum(1 for r in results.values() if r.is_success())
        
        self.logger.info(
            f"✓ Sequential execution completed in {total_time:.2f}s: "
            f"{success_count}/{len(agent_names)} succeeded"
        )
        
        return results
    
    def send_message(self, message: AgentMessage) -> None:
        """
        Agent间消息传递
        
        Args:
            message: Agent消息
        """
        self.message_queue.append(message)
        self.logger.debug(
            f"Message queued: {message.from_agent} -> {message.to_agent} "
            f"(type: {message.message_type})"
        )
    
    def get_messages(
        self, 
        to_agent: str, 
        message_type: Optional[str] = None
    ) -> List[AgentMessage]:
        """
        获取指定Agent的消息
        
        Args:
            to_agent: 接收方Agent名称
            message_type: 消息类型过滤（可选）
            
        Returns:
            消息列表
        """
        messages = [msg for msg in self.message_queue if msg.to_agent == to_agent]
        
        if message_type:
            messages = [msg for msg in messages if msg.message_type == message_type]
        
        return messages
    
    def clear_messages(self) -> None:
        """清空消息队列"""
        self.message_queue.clear()
        self.logger.debug("Message queue cleared")
    
    def get_statistics(self) -> Dict[str, any]:
        """
        获取编排器统计信息
        
        Returns:
            统计信息字典
        """
        return {
            "total_agents": len(self.agents),
            "registered_agents": list(self.agents.keys()),
            "pending_messages": len(self.message_queue),
            "max_workers": self.max_workers
        }
    
    def __repr__(self) -> str:
        return f"<AgentOrchestrator(agents={len(self.agents)}, workers={self.max_workers})>"
