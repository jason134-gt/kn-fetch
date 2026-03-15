"""
Git客户端 - 基于design-v1方案实现标准化的Git仓库访问
"""

import os
import shutil
from typing import List, Optional, Dict, Any
from datetime import datetime
from pathlib import Path
from loguru import logger


class GitClient:
    """Git客户端封装"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.workspace_dir = config.get("workspace_dir", "./workspace")
        self.max_clone_depth = config.get("max_clone_depth", 100)
        
        # 确保工作目录存在
        os.makedirs(self.workspace_dir, exist_ok=True)
    
    async def clone_repository(self, repo_url: str, branch: str = "main", 
                             local_path: Optional[str] = None) -> str:
        """克隆仓库"""
        try:
            import git
            
            # 确定本地路径
            if not local_path:
                repo_name = repo_url.split('/')[-1].replace('.git', '')
                local_path = os.path.join(self.workspace_dir, repo_name)
            
            # 如果目录已存在，先删除
            if os.path.exists(local_path):
                shutil.rmtree(local_path)
            
            logger.info(f"正在克隆仓库: {repo_url} -> {local_path}")
            
            # 克隆仓库
            repo = git.Repo.clone_from(
                repo_url,
                local_path,
                branch=branch,
                depth=self.max_clone_depth
            )
            
            logger.info(f"仓库克隆成功: {local_path}")
            return local_path
            
        except Exception as e:
            logger.error(f"克隆仓库失败: {e}")
            raise
    
    async def get_repository_info(self, local_path: str) -> Dict[str, Any]:
        """获取仓库信息"""
        try:
            import git
            
            repo = git.Repo(local_path)
            
            return {
                "url": next(repo.remote().urls),
                "branch": repo.active_branch.name,
                "commit_hash": repo.head.commit.hexsha,
                "commit_message": repo.head.commit.message.strip(),
                "commit_author": repo.head.commit.author.name,
                "commit_date": datetime.fromtimestamp(repo.head.commit.committed_date),
                "file_count": self._count_files(local_path),
                "total_size": self._calculate_size(local_path)
            }
            
        except Exception as e:
            logger.error(f"获取仓库信息失败: {e}")
            raise
    
    async def get_file_history(self, local_path: str, file_path: str, 
                             limit: int = 10) -> List[Dict[str, Any]]:
        """获取文件变更历史"""
        try:
            import git
            
            repo = git.Repo(local_path)
            file_commits = []
            
            # 获取文件的提交历史
            for commit in repo.iter_commits(paths=file_path, max_count=limit):
                file_commits.append({
                    "hash": commit.hexsha,
                    "message": commit.message.strip(),
                    "author": commit.author.name,
                    "date": datetime.fromtimestamp(commit.committed_date),
                    "changes": self._get_file_changes(repo, commit, file_path)
                })
            
            return file_commits
            
        except Exception as e:
            logger.error(f"获取文件历史失败: {e}")
            return []
    
    async def get_changed_files(self, local_path: str, since_commit: Optional[str] = None) -> List[str]:
        """获取变更文件列表"""
        try:
            import git
            
            repo = git.Repo(local_path)
            changed_files = []
            
            if since_commit:
                # 获取特定提交后的变更
                commits = list(repo.iter_commits(since=since_commit))
            else:
                # 获取最近的变更
                commits = [repo.head.commit]
            
            for commit in commits:
                if commit.parents:
                    # 对比父提交
                    diff = commit.parents[0].diff(commit)
                    for file_diff in diff:
                        if file_diff.a_path:
                            changed_files.append(file_diff.a_path)
            
            return list(set(changed_files))
            
        except Exception as e:
            logger.error(f"获取变更文件失败: {e}")
            return []
    
    def _count_files(self, directory: str) -> int:
        """统计文件数量"""
        count = 0
        for root, dirs, files in os.walk(directory):
            # 忽略.git目录
            if '.git' in dirs:
                dirs.remove('.git')
            count += len(files)
        return count
    
    def _calculate_size(self, directory: str) -> int:
        """计算目录大小"""
        total_size = 0
        for root, dirs, files in os.walk(directory):
            # 忽略.git目录
            if '.git' in dirs:
                dirs.remove('.git')
            for file in files:
                file_path = os.path.join(root, file)
                total_size += os.path.getsize(file_path)
        return total_size
    
    def _get_file_changes(self, repo, commit, file_path: str) -> Dict[str, Any]:
        """获取文件变更详情"""
        try:
            if not commit.parents:
                return {"type": "initial", "added": 0, "deleted": 0}
            
            parent = commit.parents[0]
            diff_index = parent.diff(commit, paths=file_path)
            
            if not diff_index:
                return {"type": "unknown", "added": 0, "deleted": 0}
            
            diff = diff_index[0]
            
            return {
                "type": diff.change_type,
                "added": diff.a_blob.size if diff.a_blob else 0,
                "deleted": diff.b_blob.size if diff.b_blob else 0,
                "changes": diff.diff.decode('utf-8', errors='ignore') if diff.diff else ""
            }
            
        except Exception as e:
            logger.debug(f"获取文件变更详情失败: {e}")
            return {"type": "error", "added": 0, "deleted": 0}