"""
仓库接入Agent - 基于design-v1方案实现标准化的仓库接入功能
"""

import os
from typing import Dict, Any, List, Optional
from pathlib import Path
from loguru import logger

from .base_agent import BaseAgent, AgentType
from ..models.api_models import RepositoryInfo
from ..infrastructure.git_client import GitClient


class RepositoryAccessAgent(BaseAgent):
    """仓库接入Agent"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__(AgentType.REPOSITORY_ACCESS, config)
        self.git_client: Optional[GitClient] = None
        
    async def initialize(self) -> bool:
        """初始化Agent"""
        try:
            self.git_client = GitClient(self.config.get("git", {}))
            logger.info("仓库接入Agent初始化成功")
            return True
        except Exception as e:
            logger.error(f"仓库接入Agent初始化失败: {e}")
            return False
    
    async def execute(self, input_data: Dict[str, Any]) -> Dict[str, Any]:
        """执行仓库接入任务"""
        try:
            # 解析输入数据
            repo_info = RepositoryInfo(**input_data["repository_info"])
            
            self.update_progress(10, "开始接入仓库")
            
            # 处理仓库接入
            if repo_info.is_local:
                # 本地仓库
                local_path = repo_info.local_path
                if not local_path or not Path(local_path).exists():
                    raise ValueError(f"本地仓库路径不存在: {local_path}")
                
                logger.info(f"使用本地仓库: {local_path}")
                
            else:
                # 远程仓库，需要克隆
                if not repo_info.url:
                    raise ValueError("远程仓库URL不能为空")
                
                self.update_progress(30, "正在克隆远程仓库")
                
                local_path = await self.git_client.clone_repository(
                    repo_url=repo_info.url,
                    branch=repo_info.branch,
                    local_path=repo_info.local_path
                )
                
                logger.info(f"远程仓库克隆完成: {local_path}")
            
            self.update_progress(60, "正在获取仓库信息")
            
            # 获取仓库详细信息
            repo_details = await self.git_client.get_repository_info(local_path)
            
            self.update_progress(90, "正在扫描文件结构")
            
            # 扫描文件结构
            file_structure = self._scan_file_structure(local_path)
            
            self.update_progress(100, "仓库接入完成")
            
            # 构建结果
            result = {
                "local_path": local_path,
                "repository_info": repo_details,
                "file_structure": file_structure,
                "total_files": len(file_structure),
                "supported_files": self._count_supported_files(file_structure)
            }
            
            # 记录指标
            self.record_metric("total_files", len(file_structure))
            self.record_metric("supported_files", result["supported_files"])
            self.record_metric("repository_size", repo_details.get("total_size", 0))
            
            return result
            
        except Exception as e:
            logger.error(f"仓库接入任务执行失败: {e}")
            raise
    
    async def validate_input(self, input_data: Dict[str, Any]) -> bool:
        """验证输入数据"""
        try:
            # 检查必要的字段
            if "repository_info" not in input_data:
                return False
            
            repo_info = input_data["repository_info"]
            
            # 验证仓库信息
            if not repo_info.get("is_local"):
                if not repo_info.get("url"):
                    return False
            else:
                if not repo_info.get("local_path"):
                    return False
            
            return True
            
        except Exception as e:
            logger.error(f"输入数据验证失败: {e}")
            return False
    
    async def validate_output(self, output_data: Dict[str, Any]) -> bool:
        """验证输出数据"""
        try:
            # 检查必要的字段
            required_fields = ["local_path", "repository_info", "file_structure", "total_files"]
            for field in required_fields:
                if field not in output_data:
                    return False
            
            # 验证路径存在
            local_path = output_data["local_path"]
            if not Path(local_path).exists():
                return False
            
            return True
            
        except Exception as e:
            logger.error(f"输出数据验证失败: {e}")
            return False
    
    def _scan_file_structure(self, directory: str) -> List[Dict[str, Any]]:
        """扫描文件结构"""
        file_structure = []
        
        try:
            for root, dirs, files in os.walk(directory):
                # 忽略隐藏目录和特定目录
                dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', '__pycache__', '.git']]
                
                for file in files:
                    file_path = os.path.join(root, file)
                    relative_path = os.path.relpath(file_path, directory)
                    
                    file_info = {
                        "path": relative_path,
                        "absolute_path": file_path,
                        "size": os.path.getsize(file_path),
                        "language": self._detect_language(file_path),
                        "is_supported": self._is_supported_file(file_path)
                    }
                    
                    file_structure.append(file_info)
            
            return file_structure
            
        except Exception as e:
            logger.error(f"扫描文件结构失败: {e}")
            return []
    
    def _detect_language(self, file_path: str) -> str:
        """检测文件语言"""
        ext_map = {
            '.py': 'python',
            '.js': 'javascript',
            '.ts': 'typescript',
            '.java': 'java',
            '.cpp': 'cpp', '.cc': 'cpp', '.cxx': 'cpp', '.h': 'cpp', '.hpp': 'cpp',
            '.go': 'go',
            '.rs': 'rust',
            '.php': 'php',
            '.rb': 'ruby',
            '.html': 'html', '.css': 'css', '.json': 'json', '.yaml': 'yaml', '.yml': 'yaml',
            '.md': 'markdown', '.txt': 'text'
        }
        
        ext = Path(file_path).suffix.lower()
        return ext_map.get(ext, "unknown")
    
    def _is_supported_file(self, file_path: str) -> bool:
        """检查是否支持的文件类型"""
        supported_extensions = [
            '.py', '.js', '.ts', '.java', '.cpp', '.cc', '.cxx', '.h', '.hpp',
            '.go', '.rs', '.php', '.rb'
        ]
        
        ext = Path(file_path).suffix.lower()
        return ext in supported_extensions
    
    def _count_supported_files(self, file_structure: List[Dict[str, Any]]) -> int:
        """统计支持的文件数量"""
        return sum(1 for file_info in file_structure if file_info.get("is_supported", False))