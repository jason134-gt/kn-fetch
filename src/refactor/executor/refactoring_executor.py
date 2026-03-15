"""
重构执行器

执行重构操作的核心引擎，支持多种重构类型
"""

import json
import logging
import shutil
import subprocess
from pathlib import Path
from typing import Dict, List, Optional, Any, Callable
from datetime import datetime
from abc import ABC, abstractmethod
from enum import Enum

logger = logging.getLogger(__name__)


class RefactoringType(str, Enum):
    """重构类型"""
    ADD_DOCSTRING = "add_docstring"
    EXTRACT_METHOD = "extract_method"
    RENAME_SYMBOL = "rename_symbol"
    SPLIT_FILE = "split_file"
    REMOVE_DEAD_CODE = "remove_dead_code"
    FORMAT_CODE = "format_code"
    FIX_CODE_SMELL = "fix_code_smell"


class ExecutionStatus(str, Enum):
    """执行状态"""
    PENDING = "pending"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"
    ROLLED_BACK = "rolled_back"


class RefactoringResult:
    """重构结果"""
    
    def __init__(
        self,
        problem_id: str,
        refactoring_type: RefactoringType,
        status: ExecutionStatus
    ):
        self.problem_id = problem_id
        self.refactoring_type = refactoring_type
        self.status = status
        self.started_at = datetime.now()
        self.completed_at: Optional[datetime] = None
        self.changes: List[Dict] = []
        self.errors: List[str] = []
        self.warnings: List[str] = []
        self.backup_path: Optional[str] = None
    
    def to_dict(self) -> Dict:
        return {
            "problem_id": self.problem_id,
            "refactoring_type": self.refactoring_type.value,
            "status": self.status.value,
            "started_at": self.started_at.isoformat(),
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
            "changes": self.changes,
            "errors": self.errors,
            "warnings": self.warnings,
            "backup_path": self.backup_path
        }


class RefactoringTemplate(ABC):
    """重构模板基类"""
    
    @property
    @abstractmethod
    def refactoring_type(self) -> RefactoringType:
        """返回重构类型"""
        pass
    
    @abstractmethod
    def can_apply(self, problem: Dict) -> bool:
        """判断是否可以应用此模板"""
        pass
    
    @abstractmethod
    def apply(self, problem: Dict, dry_run: bool = False) -> RefactoringResult:
        """应用重构"""
        pass
    
    @abstractmethod
    def validate(self, problem: Dict) -> List[str]:
        """验证重构前置条件"""
        pass


class AddDocstringTemplate(RefactoringTemplate):
    """添加文档注释模板"""
    
    @property
    def refactoring_type(self) -> RefactoringType:
        return RefactoringType.ADD_DOCSTRING
    
    def can_apply(self, problem: Dict) -> bool:
        return problem.get("problem_type") == "missing_doc"
    
    def validate(self, problem: Dict) -> List[str]:
        errors = []
        
        file_path = problem.get("file_path")
        if not file_path or not Path(file_path).exists():
            errors.append(f"文件不存在: {file_path}")
        
        return errors
    
    def apply(self, problem: Dict, dry_run: bool = False) -> RefactoringResult:
        result = RefactoringResult(
            problem_id=problem.get("problem_id", ""),
            refactoring_type=self.refactoring_type,
            status=ExecutionStatus.RUNNING
        )
        
        try:
            file_path = Path(problem.get("file_path", ""))
            start_line = problem.get("start_line", 0)
            entity_name = problem.get("entity_name", "")
            entity_type = problem.get("entity_type", "function")
            
            if not file_path.exists():
                result.status = ExecutionStatus.FAILED
                result.errors.append(f"文件不存在: {file_path}")
                return result
            
            # 读取文件
            with open(file_path, "r", encoding="utf-8") as f:
                lines = f.readlines()
            
            # 生成文档模板
            doc_template = self._generate_doc_template(entity_name, entity_type, problem)
            
            if dry_run:
                result.changes.append({
                    "type": "add_docstring",
                    "file": str(file_path),
                    "line": start_line,
                    "content": doc_template,
                    "dry_run": True
                })
            else:
                # 插入文档注释
                lines.insert(start_line - 1, doc_template + "\n")
                
                # 写回文件
                with open(file_path, "w", encoding="utf-8") as f:
                    f.writelines(lines)
                
                result.changes.append({
                    "type": "add_docstring",
                    "file": str(file_path),
                    "line": start_line,
                    "content": doc_template
                })
            
            result.status = ExecutionStatus.SUCCESS
            
        except Exception as e:
            result.status = ExecutionStatus.FAILED
            result.errors.append(str(e))
        
        finally:
            result.completed_at = datetime.now()
        
        return result
    
    def _generate_doc_template(
        self,
        entity_name: str,
        entity_type: str,
        problem: Dict
    ) -> str:
        """生成文档模板"""
        # 根据文件扩展名判断语言
        file_path = problem.get("file_path", "")
        
        if file_path.endswith(".java"):
            return self._generate_java_doc(entity_name, entity_type)
        elif file_path.endswith(".py"):
            return self._generate_python_doc(entity_name, entity_type)
        else:
            return f"// TODO: Add documentation for {entity_name}"
    
    def _generate_java_doc(self, name: str, entity_type: str) -> str:
        if entity_type == "class":
            return f'''/**
 * {name} - [类描述]
 *
 * [详细描述]
 *
 * @author [作者]
 * @since {datetime.now().strftime("%Y-%m-%d")}
 */'''
        else:
            return f'''/**
 * {name} - [功能描述]
 *
 * [详细描述]
 *
 * @param [参数名] [参数描述]
 * @return [返回值描述]
 */'''
    
    def _generate_python_doc(self, name: str, entity_type: str) -> str:
        if entity_type == "class":
            return f'"""{name} - [类描述]\n\n[详细描述]\n"""'
        else:
            return f'"""{name} - [功能描述]\n\nArgs:\n    [参数名]: [参数描述]\n\nReturns:\n    [返回值描述]\n"""'


class CodeBackup:
    """代码备份管理"""
    
    def __init__(self, backup_dir: str = "output/refactor/backups"):
        self.backup_dir = Path(backup_dir)
        self.backup_dir.mkdir(parents=True, exist_ok=True)
    
    def create_backup(
        self,
        file_path: str,
        session_id: str
    ) -> str:
        """创建备份"""
        source = Path(file_path)
        if not source.exists():
            return ""
        
        # 创建备份路径
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        backup_name = f"{source.stem}_{timestamp}_{session_id[:8]}{source.suffix}"
        backup_path = self.backup_dir / backup_name
        
        # 复制文件
        shutil.copy2(source, backup_path)
        
        logger.info(f"Created backup: {backup_path}")
        return str(backup_path)
    
    def restore_backup(self, backup_path: str, target_path: str) -> bool:
        """从备份恢复"""
        backup = Path(backup_path)
        target = Path(target_path)
        
        if not backup.exists():
            logger.error(f"Backup not found: {backup_path}")
            return False
        
        try:
            shutil.copy2(backup, target)
            logger.info(f"Restored from backup: {backup_path}")
            return True
        except Exception as e:
            logger.error(f"Restore failed: {e}")
            return False
    
    def create_git_stash(self, message: str = "Auto stash before refactoring") -> str:
        """创建Git Stash"""
        try:
            result = subprocess.run(
                ["git", "stash", "push", "-m", message],
                capture_output=True,
                text=True
            )
            if result.returncode == 0:
                # 获取stash ID
                result = subprocess.run(
                    ["git", "stash", "list", "-1", "--format=%H"],
                    capture_output=True,
                    text=True
                )
                return result.stdout.strip()
        except Exception as e:
            logger.error(f"Git stash failed: {e}")
        return ""
    
    def create_git_branch(self, branch_name: str) -> bool:
        """创建Git分支"""
        try:
            subprocess.run(
                ["git", "checkout", "-b", branch_name],
                capture_output=True,
                check=True
            )
            return True
        except Exception as e:
            logger.error(f"Create branch failed: {e}")
            return False


class RefactoringExecutor:
    """重构执行器"""
    
    def __init__(self, config: Dict = None):
        self.config = config or {}
        self.backup_manager = CodeBackup()
        
        # 注册模板
        self.templates: Dict[RefactoringType, RefactoringTemplate] = {
            RefactoringType.ADD_DOCSTRING: AddDocstringTemplate(),
        }
    
    def execute(
        self,
        problems: List[Dict],
        dry_run: bool = False,
        auto_backup: bool = True,
        progress_callback: Callable = None
    ) -> List[RefactoringResult]:
        """执行重构"""
        results = []
        
        for i, problem in enumerate(problems):
            if progress_callback:
                progress_callback(i + 1, len(problems), problem)
            
            # 获取合适的模板
            template = self._get_template(problem)
            
            if not template:
                result = RefactoringResult(
                    problem_id=problem.get("problem_id", ""),
                    refactoring_type=RefactoringType.FIX_CODE_SMELL,
                    status=ExecutionStatus.FAILED
                )
                result.errors.append("No suitable template found")
                results.append(result)
                continue
            
            # 验证
            errors = template.validate(problem)
            if errors:
                result = RefactoringResult(
                    problem_id=problem.get("problem_id", ""),
                    refactoring_type=template.refactoring_type,
                    status=ExecutionStatus.FAILED
                )
                result.errors.extend(errors)
                results.append(result)
                continue
            
            # 创建备份
            if auto_backup and not dry_run:
                backup_path = self.backup_manager.create_backup(
                    problem.get("file_path", ""),
                    "session"
                )
            
            # 执行重构
            result = template.apply(problem, dry_run)
            if backup_path:
                result.backup_path = backup_path
            
            results.append(result)
        
        return results
    
    def _get_template(self, problem: Dict) -> Optional[RefactoringTemplate]:
        """获取合适的模板"""
        for template in self.templates.values():
            if template.can_apply(problem):
                return template
        return None
    
    def rollback(self, results: List[RefactoringResult]) -> List[bool]:
        """回滚重构"""
        rollback_results = []
        
        for result in results:
            if result.backup_path and result.status == ExecutionStatus.SUCCESS:
                # 从备份恢复
                for change in result.changes:
                    if "file" in change:
                        success = self.backup_manager.restore_backup(
                            result.backup_path,
                            change["file"]
                        )
                        rollback_results.append(success)
                        if success:
                            result.status = ExecutionStatus.ROLLED_BACK
        
        return rollback_results


__all__ = [
    'RefactoringType',
    'ExecutionStatus',
    'RefactoringResult',
    'RefactoringTemplate',
    'AddDocstringTemplate',
    'CodeBackup',
    'RefactoringExecutor'
]
