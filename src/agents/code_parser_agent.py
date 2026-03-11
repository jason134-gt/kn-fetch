"""
代码解析Agent - 基于design-v1方案实现标准化的代码解析功能
"""

from typing import Dict, Any, List
from pathlib import Path
from loguru import logger

from .base_agent import BaseAgent, AgentType
from ..models.code_metadata import CodeMetadata, FileMetadata, FunctionMetadata, ClassMetadata, CodeComplexity
from ..infrastructure.ast_parser import ASTParser
from ..infrastructure.complexity_scanner import ComplexityScanner


class CodeParserAgent(BaseAgent):
    """代码解析Agent"""
    
    def __init__(self, config: Dict[str, Any]):
        super().__init__(AgentType.CODE_PARSER, config)
        self.ast_parser: Optional[ASTParser] = None
        self.complexity_scanner: Optional[ComplexityScanner] = None
        
    async def initialize(self) -> bool:
        """初始化Agent"""
        try:
            self.ast_parser = ASTParser(self.config.get("ast_parser", {}))
            self.complexity_scanner = ComplexityScanner(self.config.get("complexity_scanner", {}))
            logger.info("代码解析Agent初始化成功")
            return True
        except Exception as e:
            logger.error(f"代码解析Agent初始化失败: {e}")
            return False
    
    async def execute(self, input_data: Dict[str, Any]) -> Dict[str, Any]:
        """执行代码解析任务"""
        try:
            # 解析输入数据
            local_path = input_data["local_path"]
            file_structure = input_data["file_structure"]
            
            self.update_progress(10, "开始代码解析")
            
            # 过滤支持的文件
            supported_files = [f for f in file_structure if f.get("is_supported", False)]
            
            self.update_progress(20, f"发现 {len(supported_files)} 个支持的文件")
            
            # 解析文件
            parsed_files = []
            total_files = len(supported_files)
            
            for i, file_info in enumerate(supported_files):
                file_path = file_info["absolute_path"]
                
                # 更新进度
                progress = 20 + (i / total_files) * 70
                self.update_progress(progress, f"正在解析文件: {file_info['path']}")
                
                # 解析文件
                parsed_file = await self._parse_single_file(file_path)
                if parsed_file:
                    parsed_files.append(parsed_file)
                
                # 记录进度
                if i % 10 == 0:
                    logger.info(f"已解析 {i}/{total_files} 个文件")
            
            self.update_progress(95, "正在聚合分析结果")
            
            # 聚合分析结果
            code_metadata = self._aggregate_analysis(parsed_files, input_data["repository_info"])
            
            self.update_progress(100, "代码解析完成")
            
            # 构建结果
            result = {
                "code_metadata": code_metadata.dict(),
                "parsed_files": len(parsed_files),
                "total_functions": code_metadata.total_functions,
                "total_classes": code_metadata.total_classes,
                "total_lines": code_metadata.total_lines
            }
            
            # 记录指标
            self.record_metric("parsed_files", len(parsed_files))
            self.record_metric("total_functions", code_metadata.total_functions)
            self.record_metric("total_classes", code_metadata.total_classes)
            self.record_metric("total_lines", code_metadata.total_lines)
            
            return result
            
        except Exception as e:
            logger.error(f"代码解析任务执行失败: {e}")
            raise
    
    async def validate_input(self, input_data: Dict[str, Any]) -> bool:
        """验证输入数据"""
        try:
            # 检查必要的字段
            required_fields = ["local_path", "file_structure", "repository_info"]
            for field in required_fields:
                if field not in input_data:
                    return False
            
            # 验证路径存在
            local_path = input_data["local_path"]
            if not Path(local_path).exists():
                return False
            
            return True
            
        except Exception as e:
            logger.error(f"输入数据验证失败: {e}")
            return False
    
    async def validate_output(self, output_data: Dict[str, Any]) -> bool:
        """验证输出数据"""
        try:
            # 检查必要的字段
            required_fields = ["code_metadata", "parsed_files", "total_functions", "total_classes"]
            for field in required_fields:
                if field not in output_data:
                    return False
            
            # 验证数据合理性
            if output_data["parsed_files"] < 0:
                return False
            
            return True
            
        except Exception as e:
            logger.error(f"输出数据验证失败: {e}")
            return False
    
    async def _parse_single_file(self, file_path: str) -> Optional[FileMetadata]:
        """解析单个文件"""
        try:
            # AST解析
            ast_result = self.ast_parser.parse_file(file_path)
            if not ast_result:
                return None
            
            # 复杂度分析
            complexity_result = self.complexity_scanner.scan_file(file_path)
            
            # 构建文件元数据
            file_metadata = FileMetadata(
                file_path=file_path,
                language=ast_result["language"],
                functions=[],  # 需要从AST结果中提取
                classes=[],    # 需要从AST结果中提取
                imports=ast_result.get("imports", []),
                exports=ast_result.get("exports", []),
                complexity=CodeComplexity(**complexity_result["metrics"]) if complexity_result else CodeComplexity(
                    cyclomatic_complexity=0,
                    cognitive_complexity=0,
                    lines_of_code=ast_result["line_count"],
                    comment_density=0.0,
                    maintainability_index=0.0
                ),
                file_size=ast_result["file_size"],
                last_modified=Path(file_path).stat().st_mtime
            )
            
            return file_metadata
            
        except Exception as e:
            logger.warning(f"解析文件失败 {file_path}: {e}")
            return None
    
    def _aggregate_analysis(self, parsed_files: List[FileMetadata], repo_info: Dict[str, Any]) -> CodeMetadata:
        """聚合分析结果"""
        # 计算总指标
        total_files = len(parsed_files)
        total_lines = sum(f.complexity.lines_of_code for f in parsed_files)
        total_functions = sum(len(f.functions) for f in parsed_files)
        total_classes = sum(len(f.classes) for f in parsed_files)
        
        # 计算平均复杂度
        avg_ccn = sum(f.complexity.cyclomatic_complexity for f in parsed_files) / max(total_files, 1)
        avg_cog = sum(f.complexity.cognitive_complexity for f in parsed_files) / max(total_files, 1)
        avg_loc = total_lines / max(total_files, 1)
        avg_comment = sum(f.complexity.comment_density for f in parsed_files) / max(total_files, 1)
        avg_mi = sum(f.complexity.maintainability_index for f in parsed_files) / max(total_files, 1)
        
        avg_complexity = CodeComplexity(
            cyclomatic_complexity=avg_ccn,
            cognitive_complexity=avg_cog,
            lines_of_code=avg_loc,
            comment_density=avg_comment,
            maintainability_index=avg_mi
        )
        
        # 构建代码元数据
        code_metadata = CodeMetadata(
            repository_info=repo_info,
            files=parsed_files,
            total_files=total_files,
            total_lines=total_lines,
            total_functions=total_functions,
            total_classes=total_classes,
            average_complexity=avg_complexity
        )
        
        return code_metadata