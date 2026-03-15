"""
Repository Access Agent - 代码仓库接入

负责分析Git仓库的结构、元数据和变更历史
"""
from typing import Dict, Any, List
import os
import subprocess
import json
from pathlib import Path

from ...base.agent import BaseAgent, AgentResult
from ...config.agent_configs import REPOSITORY_ACCESS_AGENT


class RepositoryAccessAgent(BaseAgent):
    """代码仓库接入Agent
    
    作为代码仓库接入专家，负责分析Git仓库的结构、分支、提交历史等元数据
    """
    
    def __init__(self):
        """
        初始化RepositoryAccessAgent
        """
        super().__init__(REPOSITORY_ACCESS_AGENT)
    
    def execute(self, context):
        """
        执行仓库分析
        
        Args:
            context: Agent执行上下文
            
        Returns:
            Agent执行结果
        """
        import time
        start_time = time.time()
        
        try:
            self.logger.info(f"Starting repository analysis for project")
            
            # 1. 检查是否为Git仓库
            if not self._is_git_repository(context.project_root):
                return AgentResult(
                    success=False,
                    error="Not a Git repository",
                    metrics={"execution_time": time.time() - start_time}
                )
            
            # 2. 收集仓库元数据
            repo_info = self._collect_repository_info(context.project_root)
            
            # 3. 分析分支信息
            branch_info = self._analyze_branches(context.project_root)
            
            # 4. 分析提交历史
            commit_history = self._analyze_commit_history(context.project_root)
            
            # 5. 构建分析报告
            analysis_report = self._build_analysis_report(repo_info, branch_info, commit_history)
            
            # 6. 统计信息
            stats = {
                "total_commits": commit_history.get("total_commits", 0),
                "total_branches": len(branch_info.get("branches", [])),
                "total_authors": len(commit_history.get("authors", [])),
                "repository_age_days": repo_info.get("age_days", 0)
            }
            
            execution_time = time.time() - start_time
            
            return AgentResult(
                success=True,
                data={
                    "repository_info": repo_info,
                    "branch_info": branch_info,
                    "commit_history": commit_history
                },
                output=analysis_report,
                metrics=stats,
                cross_notes=self._extract_cross_module_notes(analysis_report)
            )
            
        except Exception as e:
            execution_time = time.time() - start_time
            self.logger.error(f"Repository analysis failed: {str(e)}")
            
            return AgentResult(
                success=False,
                error=str(e),
                metrics={"execution_time": execution_time}
            )
    
    def _get_task_description(self, context):
        """获取任务描述"""
        return """分析代码仓库的Git元数据，包括：
1. 仓库基本信息（远程URL、默认分支、创建时间等）
2. 分支结构分析（活跃分支、保护分支、分支关系）
3. 提交历史分析（活跃度、贡献者、变更模式）
4. 代码库规模和结构概览"""
    
    def _is_git_repository(self, project_root: str) -> bool:
        """
        检查是否为Git仓库
        
        Args:
            project_root: 项目根目录
            
        Returns:
            是否为Git仓库
        """
        git_dir = Path(project_root) / ".git"
        return git_dir.exists() and git_dir.is_dir()
    
    def _run_git_command(self, project_root: str, command: str) -> str:
        """
        执行Git命令
        
        Args:
            project_root: 项目根目录
            command: Git命令
            
        Returns:
            命令输出
        """
        try:
            result = subprocess.run(
                command.split(),
                cwd=project_root,
                capture_output=True,
                text=True,
                check=True
            )
            return result.stdout.strip()
        except subprocess.CalledProcessError as e:
            self.logger.warning(f"Git command failed: {command}, error: {e}")
            return ""
    
    def _collect_repository_info(self, project_root: str) -> Dict[str, Any]:
        """
        收集仓库基本信息
        
        Args:
            project_root: 项目根目录
            
        Returns:
            仓库信息字典
        """
        repo_info = {}
        
        # 获取远程URL
        remote_url = self._run_git_command(project_root, "git config --get remote.origin.url")
        repo_info["remote_url"] = remote_url if remote_url else "No remote configured"
        
        # 获取默认分支
        default_branch = self._run_git_command(project_root, "git symbolic-ref refs/remotes/origin/HEAD")
        if default_branch:
            default_branch = default_branch.replace("refs/remotes/origin/", "")
        repo_info["default_branch"] = default_branch if default_branch else "main"
        
        # 获取仓库创建时间（第一个提交的时间）
        first_commit_date = self._run_git_command(
            project_root, 
            "git log --reverse --format=%cd --date=iso-strict | head -1"
        )
        repo_info["first_commit_date"] = first_commit_date if first_commit_date else "Unknown"
        
        # 计算仓库年龄（天数）
        if first_commit_date:
            from datetime import datetime
            try:
                first_date = datetime.fromisoformat(first_commit_date)
                current_date = datetime.now()
                age_days = (current_date - first_date).days
                repo_info["age_days"] = age_days
            except:
                repo_info["age_days"] = 0
        
        return repo_info
    
    def _analyze_branches(self, project_root: str) -> Dict[str, Any]:
        """
        分析分支信息
        
        Args:
            project_root: 项目根目录
            
        Returns:
            分支信息字典
        """
        branch_info = {"branches": []}
        
        # 获取所有分支
        branches_output = self._run_git_command(project_root, "git branch -r")
        branches = [b.strip() for b in branches_output.split('\n') if b.strip()]
        
        for branch in branches:
            branch_name = branch.replace("origin/", "")
            
            # 获取分支最后提交信息
            last_commit = self._run_git_command(
                project_root, 
                f"git log -1 --format=%H|%cd|%s --date=iso-strict origin/{branch_name}"
            )
            
            if last_commit:
                commit_hash, commit_date, commit_subject = last_commit.split("|", 2)
                
                # 获取分支提交数量
                commit_count = self._run_git_command(
                    project_root,
                    f"git rev-list --count origin/{branch_name}"
                )
                
                branch_data = {
                    "name": branch_name,
                    "last_commit_hash": commit_hash,
                    "last_commit_date": commit_date,
                    "last_commit_subject": commit_subject,
                    "commit_count": int(commit_count) if commit_count.isdigit() else 0,
                    "is_protected": branch_name in ["main", "master", "develop"]
                }
                
                branch_info["branches"].append(branch_data)
        
        # 按提交数量排序
        branch_info["branches"].sort(key=lambda x: x["commit_count"], reverse=True)
        
        return branch_info
    
    def _analyze_commit_history(self, project_root: str) -> Dict[str, Any]:
        """
        分析提交历史
        
        Args:
            project_root: 项目根目录
            
        Returns:
            提交历史信息字典
        """
        commit_history = {"authors": [], "activity": {}}
        
        # 获取总提交数
        total_commits = self._run_git_command(project_root, "git rev-list --count HEAD")
        commit_history["total_commits"] = int(total_commits) if total_commits.isdigit() else 0
        
        # 获取作者统计
        author_stats = self._run_git_command(
            project_root,
            "git shortlog -s -n --all"
        )
        
        authors = []
        for line in author_stats.split('\n'):
            if line.strip():
                parts = line.strip().split('\t')
                if len(parts) == 2:
                    commit_count, author_name = parts
                    authors.append({
                        "name": author_name.strip(),
                        "commit_count": int(commit_count.strip())
                    })
        
        commit_history["authors"] = authors
        
        # 获取最近一年的提交活动
        monthly_activity = self._run_git_command(
            project_root,
            "git log --since='1 year ago' --format=%cd --date=format:%Y-%m | sort | uniq -c | sort -k2"
        )
        
        activity_data = {}
        for line in monthly_activity.split('\n'):
            if line.strip():
                parts = line.strip().split()
                if len(parts) >= 2:
                    count, month = parts[0], parts[1]
                    activity_data[month] = int(count)
        
        commit_history["activity"] = activity_data
        
        return commit_history
    
    def _build_analysis_report(self, repo_info: Dict, branch_info: Dict, commit_history: Dict) -> str:
        """
        构建分析报告
        
        Args:
            repo_info: 仓库信息
            branch_info: 分支信息
            commit_history: 提交历史
            
        Returns:
            分析报告字符串
        """
        report = f"""# 代码仓库分析报告

## 1. 仓库基本信息
- **远程仓库**: {repo_info.get('remote_url', 'N/A')}
- **默认分支**: {repo_info.get('default_branch', 'N/A')}
- **创建时间**: {repo_info.get('first_commit_date', 'N/A')}
- **仓库年龄**: {repo_info.get('age_days', 0)} 天

## 2. 分支结构分析

**活跃分支统计**:
"""
        
        # 添加分支信息
        for i, branch in enumerate(branch_info.get("branches", [])[:10]):  # 只显示前10个分支
            report += f"""
{i+1}. **{branch['name']}**
   - 提交数量: {branch['commit_count']}
   - 最后提交: {branch['last_commit_date']}
   - 提交主题: {branch['last_commit_subject'][:100]}{'...' if len(branch['last_commit_subject']) > 100 else ''}
   - 保护分支: {'是' if branch['is_protected'] else '否'}
"""
        
        report += f"""
## 3. 提交历史分析

**总体统计**:
- 总提交数: {commit_history.get('total_commits', 0)}
- 活跃作者数: {len(commit_history.get('authors', []))}

**主要贡献者**:
"""
        
        # 添加作者信息
        for i, author in enumerate(commit_history.get("authors", [])[:5]):  # 只显示前5个作者
            report += f"""
{i+1}. **{author['name']}**: {author['commit_count']} 次提交
"""
        
        report += f"""
## 4. 仓库健康度评估

基于以上分析，该代码仓库的健康度评估如下：

### 活跃度指标
- 提交频率: {'高' if commit_history.get('total_commits', 0) > 100 else '中' if commit_history.get('total_commits', 0) > 10 else '低'}
- 分支管理: {'良好' if len(branch_info.get('branches', [])) > 1 else '简单'}
- 协作程度: {'高' if len(commit_history.get('authors', [])) > 3 else '中' if len(commit_history.get('authors', [])) > 1 else '低'}

### 维护建议
1. 定期清理过期分支
2. 确保主要分支的保护设置
3. 监控代码提交频率和模式
4. 建立规范的提交信息格式
"""
        
        return report