import git
from typing import List, Optional, Set, Tuple
from pathlib import Path
from .exceptions import GitOperationError

class IncrementalAnalyzer:
    """增量分析器，基于Git diff检测变更文件"""
    
    def __init__(self, repo: git.Repo):
        self.repo = repo
        
    def get_changed_files(
        self, 
        base_commit: Optional[str] = None, 
        head_commit: Optional[str] = None
    ) -> List[str]:
        """
        获取两个提交之间的变更文件
        如果不指定base_commit，则与上次提交比较
        如果不指定head_commit，则使用当前工作区
        """
        try:
            if base_commit is None:
                # 默认与上一次提交比较
                base_commit = self.repo.head.commit.hexsha
            
            if head_commit is None:
                # 比较工作区与指定提交的差异
                diff = self.repo.git.diff(base_commit, name_only=True)
            else:
                # 比较两个提交之间的差异
                diff = self.repo.git.diff(base_commit, head_commit, name_only=True)
            
            changed_files = [f.strip() for f in diff.splitlines() if f.strip()]
            return changed_files
            
        except Exception as e:
            raise GitOperationError(f"获取变更文件失败: {str(e)}")
    
    def get_modified_files_in_working_dir(self) -> List[str]:
        """获取工作区中已修改但未提交的文件"""
        try:
            # 获取已修改但未暂存的文件
            modified = self.repo.git.diff(name_only=True)
            # 获取已暂存的文件
            staged = self.repo.git.diff("--cached", name_only=True)
            # 获取未跟踪的文件
            untracked = self.repo.untracked_files
            
            all_changed = set()
            all_changed.update([f.strip() for f in modified.splitlines() if f.strip()])
            all_changed.update([f.strip() for f in staged.splitlines() if f.strip()])
            all_changed.update(untracked)
            
            return list(all_changed)
            
        except Exception as e:
            raise GitOperationError(f"获取工作区变更文件失败: {str(e)}")
    
    def get_commit_history(self, limit: int = 100) -> List[dict]:
        """获取提交历史记录"""
        try:
            commits = []
            for commit in self.repo.iter_commits(max_count=limit):
                commits.append({
                    "hexsha": commit.hexsha,
                    "author": commit.author.name,
                    "email": commit.author.email,
                    "date": commit.authored_datetime.isoformat(),
                    "message": commit.message.strip(),
                    "changed_files": list(commit.stats.files.keys())
                })
            return commits
            
        except Exception as e:
            raise GitOperationError(f"获取提交历史失败: {str(e)}")
    
    def get_file_history(self, file_path: str, limit: int = 100) -> List[dict]:
        """获取指定文件的修改历史"""
        try:
            commits = []
            for commit in self.repo.iter_commits(paths=file_path, max_count=limit):
                commits.append({
                    "hexsha": commit.hexsha,
                    "author": commit.author.name,
                    "email": commit.author.email,
                    "date": commit.authored_datetime.isoformat(),
                    "message": commit.message.strip()
                })
            return commits
            
        except Exception as e:
            raise GitOperationError(f"获取文件历史失败: {str(e)}")
            
    def get_file_content_at_commit(self, file_path: str, commit_hexsha: str) -> str:
        """获取指定提交版本的文件内容"""
        try:
            return self.repo.git.show(f"{commit_hexsha}:{file_path}")
        except Exception as e:
            raise GitOperationError(f"获取文件历史内容失败: {str(e)}")
