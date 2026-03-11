"""
文件扫描Agent - 负责文件系统扫描与预处理

功能：
- 递归扫描目录结构
- 文件类型识别与过滤
- 文件元数据提取
- 文件编码检测
- 文件依赖性分析
"""

import os
import glob
from pathlib import Path
from typing import List, Dict, Any
from .base_agent import BaseAgent, AgentResult
from src.models.code_metadata import FileMetadata


class FileScannerAgent(BaseAgent):
    """文件扫描Agent"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__("file_scanner", config)
        self.supported_extensions = self.config.get("supported_extensions", [
            ".py", ".js", ".ts", ".java", ".cpp", ".c", ".h", ".hpp",
            ".go", ".rs", ".php", ".rb", ".cs", ".swift", ".kt",
            ".md", ".txt", ".json", ".yaml", ".yml", ".xml", ".html"
        ])
        self.ignore_patterns = self.config.get("ignore_patterns", [
            "**/__pycache__/**", "**/.git/**", "**/node_modules/**",
            "**/build/**", "**/dist/**", "**/target/**", "**/venv/**"
        ])
    
    async def _execute_impl(self, input_data: Any) -> List[FileMetadata]:
        """执行文件扫描"""
        if isinstance(input_data, str):
            scan_path = input_data
        elif isinstance(input_data, dict):
            scan_path = input_data.get("path", ".")
        else:
            raise ValueError("输入数据应为路径字符串或包含'path'键的字典")
        
        self.logger.info(f"开始扫描目录: {scan_path}")
        
        # 扫描文件
        files = await self._scan_files(scan_path)
        
        # 提取文件元数据
        file_metadata_list = []
        for file_path in files:
            metadata = await self._extract_file_metadata(file_path)
            if metadata:
                file_metadata_list.append(metadata)
        
        self.logger.info(f"扫描完成，发现 {len(file_metadata_list)} 个文件")
        return file_metadata_list
    
    async def _scan_files(self, scan_path: str) -> List[str]:
        """递归扫描文件"""
        files = []
        scan_path = Path(scan_path).resolve()
        
        for pattern in self.supported_extensions:
            # 使用glob模式匹配
            search_pattern = str(scan_path / "**" / f"*{pattern}")
            matched_files = glob.glob(search_pattern, recursive=True)
            
            # 过滤忽略模式
            for file_path in matched_files:
                if not self._should_ignore(file_path):
                    files.append(file_path)
        
        # 去重并排序
        files = sorted(list(set(files)))
        return files
    
    def _should_ignore(self, file_path: str) -> bool:
        """检查文件是否应该被忽略"""
        file_path_str = str(file_path)
        for pattern in self.ignore_patterns:
            if glob.fnmatch.fnmatch(file_path_str, pattern):
                return True
        return False
    
    async def _extract_file_metadata(self, file_path: str) -> FileMetadata:
        """提取文件元数据"""
        path = Path(file_path)
        
        try:
            # 获取文件统计信息
            stat = path.stat()
            
            # 读取文件内容计算哈希
            content_hash = self._calculate_file_hash(file_path)
            
            # 检测文件编码
            encoding = await self._detect_encoding(file_path)
            
            # 创建文件元数据对象
            metadata = FileMetadata(
                file_path=str(path),
                file_name=path.name,
                file_extension=path.suffix.lower(),
                file_size=stat.st_size,
                created_time=stat.st_ctime,
                modified_time=stat.st_mtime,
                content_hash=content_hash,
                encoding=encoding,
                relative_path=str(path.relative_to(Path.cwd()))
            )
            
            return metadata
            
        except Exception as e:
            self.logger.warning(f"提取文件 {file_path} 元数据失败: {e}")
            return None
    
    def _calculate_file_hash(self, file_path: str) -> str:
        """计算文件内容哈希"""
        import hashlib
        
        try:
            with open(file_path, "rb") as f:
                content = f.read()
                return hashlib.md5(content).hexdigest()
        except:
            return "unknown"
    
    async def _detect_encoding(self, file_path: str) -> str:
        """检测文件编码"""
        import chardet
        
        try:
            with open(file_path, "rb") as f:
                raw_data = f.read(4096)  # 读取前4KB用于编码检测
                result = chardet.detect(raw_data)
                return result.get("encoding", "utf-8")
        except:
            return "utf-8"