"""
重构会话管理模块

管理重构分析的全生命周期会话
"""

import json
import uuid
import logging
from pathlib import Path
from typing import Dict, List, Optional, Any
from datetime import datetime
from enum import Enum

logger = logging.getLogger(__name__)


class SessionStatus(str, Enum):
    """会话状态"""
    CREATED = "created"
    ANALYZING = "analyzing"
    REVIEWING = "reviewing"
    REFACTORING = "refactoring"
    TESTING = "testing"
    COMPLETED = "completed"
    FAILED = "failed"
    ROLLED_BACK = "rolled_back"


class RefactoringSession:
    """重构会话"""
    
    def __init__(
        self,
        project_name: str,
        session_id: str = None,
        config: Dict = None
    ):
        self.session_id = session_id or f"RSN-{datetime.now().strftime('%Y%m%d%H%M%S')}-{uuid.uuid4().hex[:6]}"
        self.project_name = project_name
        self.status = SessionStatus.CREATED
        self.created_at = datetime.now()
        self.updated_at = datetime.now()
        
        # 配置
        self.config = config or {}
        
        # 选中的问题
        self.selected_problems: List[str] = []
        
        # 用户反馈
        self.feedback_history: List[Dict] = []
        
        # 执行结果
        self.analysis_result: Optional[Dict] = None
        self.refactoring_steps: List[Dict] = []
        self.test_results: Optional[Dict] = None
        self.diff_report: Optional[Dict] = None
        
        # 回滚信息
        self.backup_branch: Optional[str] = None
        self.backup_commit: Optional[str] = None
        self.can_rollback: bool = True
        
        # 元数据
        self.metadata: Dict = {}
    
    def to_dict(self) -> Dict:
        """转换为字典"""
        return {
            "session_id": self.session_id,
            "project_name": self.project_name,
            "status": self.status.value,
            "created_at": self.created_at.isoformat(),
            "updated_at": self.updated_at.isoformat(),
            "config": self.config,
            "selected_problems": self.selected_problems,
            "feedback_history": self.feedback_history,
            "analysis_result": self.analysis_result,
            "refactoring_steps": self.refactoring_steps,
            "test_results": self.test_results,
            "diff_report": self.diff_report,
            "backup_branch": self.backup_branch,
            "backup_commit": self.backup_commit,
            "can_rollback": self.can_rollback,
            "metadata": self.metadata
        }
    
    @classmethod
    def from_dict(cls, data: Dict) -> "RefactoringSession":
        """从字典创建"""
        session = cls(
            project_name=data["project_name"],
            session_id=data["session_id"],
            config=data.get("config", {})
        )
        session.status = SessionStatus(data["status"])
        session.created_at = datetime.fromisoformat(data["created_at"])
        session.updated_at = datetime.fromisoformat(data["updated_at"])
        session.selected_problems = data.get("selected_problems", [])
        session.feedback_history = data.get("feedback_history", [])
        session.analysis_result = data.get("analysis_result")
        session.refactoring_steps = data.get("refactoring_steps", [])
        session.test_results = data.get("test_results")
        session.diff_report = data.get("diff_report")
        session.backup_branch = data.get("backup_branch")
        session.backup_commit = data.get("backup_commit")
        session.can_rollback = data.get("can_rollback", True)
        session.metadata = data.get("metadata", {})
        return session
    
    def update_status(self, status: SessionStatus):
        """更新状态"""
        self.status = status
        self.updated_at = datetime.now()
    
    def select_problems(self, problem_ids: List[str], mode: str = "single"):
        """选择问题"""
        if mode == "single":
            self.selected_problems = problem_ids
        elif mode == "batch":
            self.selected_problems.extend(problem_ids)
            self.selected_problems = list(set(self.selected_problems))
        elif mode == "dependency":
            # TODO: 实现依赖联动选择
            self.selected_problems.extend(problem_ids)
            self.selected_problems = list(set(self.selected_problems))
        
        self.updated_at = datetime.now()
    
    def add_feedback(
        self,
        problem_id: str,
        feedback_type: str,
        feedback_content: str,
        custom_solution: Dict = None
    ):
        """添加用户反馈"""
        feedback = {
            "feedback_id": f"FB-{uuid.uuid4().hex[:6]}",
            "problem_id": problem_id,
            "feedback_type": feedback_type,  # approve, reject, modify, comment
            "feedback_content": feedback_content,
            "custom_solution": custom_solution,
            "created_at": datetime.now().isoformat()
        }
        self.feedback_history.append(feedback)
        self.updated_at = datetime.now()
    
    def set_backup_info(self, branch: str = None, commit: str = None):
        """设置备份信息"""
        self.backup_branch = branch
        self.backup_commit = commit
        self.can_rollback = True


class SessionManager:
    """会话管理器"""
    
    def __init__(self, storage_dir: str = "output/refactor/sessions"):
        self.storage_dir = Path(storage_dir)
        self.storage_dir.mkdir(parents=True, exist_ok=True)
        self._sessions: Dict[str, RefactoringSession] = {}
    
    def create_session(
        self,
        project_name: str,
        config: Dict = None
    ) -> RefactoringSession:
        """创建新会话"""
        session = RefactoringSession(
            project_name=project_name,
            config=config
        )
        self._sessions[session.session_id] = session
        self._save_session(session)
        
        logger.info(f"Created session: {session.session_id}")
        return session
    
    def get_session(self, session_id: str) -> Optional[RefactoringSession]:
        """获取会话"""
        if session_id in self._sessions:
            return self._sessions[session_id]
        
        # 尝试从存储加载
        session_file = self.storage_dir / f"{session_id}.json"
        if session_file.exists():
            with open(session_file, "r", encoding="utf-8") as f:
                data = json.load(f)
            session = RefactoringSession.from_dict(data)
            self._sessions[session_id] = session
            return session
        
        return None
    
    def update_session(self, session: RefactoringSession):
        """更新会话"""
        session.updated_at = datetime.now()
        self._sessions[session.session_id] = session
        self._save_session(session)
    
    def delete_session(self, session_id: str) -> bool:
        """删除会话"""
        if session_id in self._sessions:
            del self._sessions[session_id]
        
        session_file = self.storage_dir / f"{session_id}.json"
        if session_file.exists():
            session_file.unlink()
            return True
        return False
    
    def list_sessions(
        self,
        project_name: str = None,
        status: SessionStatus = None
    ) -> List[RefactoringSession]:
        """列出会话"""
        sessions = []
        
        # 从存储加载所有会话
        for session_file in self.storage_dir.glob("*.json"):
            if session_file.stem not in self._sessions:
                with open(session_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                session = RefactoringSession.from_dict(data)
                self._sessions[session.session_id] = session
        
        # 过滤
        for session in self._sessions.values():
            if project_name and session.project_name != project_name:
                continue
            if status and session.status != status:
                continue
            sessions.append(session)
        
        # 按时间排序
        sessions.sort(key=lambda s: s.created_at, reverse=True)
        return sessions
    
    def _save_session(self, session: RefactoringSession):
        """保存会话到文件"""
        session_file = self.storage_dir / f"{session.session_id}.json"
        with open(session_file, "w", encoding="utf-8") as f:
            json.dump(session.to_dict(), f, ensure_ascii=False, indent=2)


class UserFeedbackCollector:
    """用户反馈收集器"""
    
    def __init__(self, session_manager: SessionManager):
        self.session_manager = session_manager
    
    def collect_feedback(
        self,
        session_id: str,
        problem_id: str,
        feedback_type: str,
        feedback_content: str,
        custom_solution: Dict = None
    ) -> bool:
        """收集用户反馈"""
        session = self.session_manager.get_session(session_id)
        if not session:
            return False
        
        session.add_feedback(
            problem_id=problem_id,
            feedback_type=feedback_type,
            feedback_content=feedback_content,
            custom_solution=custom_solution
        )
        
        self.session_manager.update_session(session)
        return True
    
    def get_feedback_for_problem(
        self,
        session_id: str,
        problem_id: str
    ) -> List[Dict]:
        """获取特定问题的反馈"""
        session = self.session_manager.get_session(session_id)
        if not session:
            return []
        
        return [
            fb for fb in session.feedback_history
            if fb["problem_id"] == problem_id
        ]
    
    def get_all_feedback(self, session_id: str) -> List[Dict]:
        """获取会话的所有反馈"""
        session = self.session_manager.get_session(session_id)
        if not session:
            return []
        
        return session.feedback_history


__all__ = [
    'SessionStatus',
    'RefactoringSession',
    'SessionManager',
    'UserFeedbackCollector'
]
