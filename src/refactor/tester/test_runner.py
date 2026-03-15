"""
测试运行器

执行重构后的测试验证
"""

import json
import logging
import subprocess
import time
from pathlib import Path
from typing import Dict, List, Optional, Any
from datetime import datetime
from dataclasses import dataclass, field

logger = logging.getLogger(__name__)


@dataclass
class TestResult:
    """测试结果"""
    test_type: str
    total: int = 0
    passed: int = 0
    failed: int = 0
    skipped: int = 0
    duration: float = 0.0
    coverage: float = 0.0
    failures: List[Dict] = field(default_factory=list)
    output: str = ""
    
    @property
    def success(self) -> bool:
        return self.failed == 0
    
    @property
    def pass_rate(self) -> float:
        if self.total == 0:
            return 0.0
        return (self.passed / self.total) * 100


@dataclass
class TestReport:
    """测试报告"""
    session_id: str
    generated_at: datetime
    unit_tests: TestResult
    integration_tests: TestResult
    regression_tests: TestResult
    
    @property
    def overall_success(self) -> bool:
        return (
            self.unit_tests.success and
            self.integration_tests.success and
            self.regression_tests.success
        )
    
    @property
    def total_failed(self) -> int:
        return (
            self.unit_tests.failed +
            self.integration_tests.failed +
            self.regression_tests.failed
        )


class TestRunner:
    """测试运行器"""
    
    def __init__(self, project_path: str, config: Dict = None):
        self.project_path = Path(project_path)
        self.config = config or {}
    
    def run_all_tests(
        self,
        session_id: str,
        include_unit: bool = True,
        include_integration: bool = True,
        include_regression: bool = True
    ) -> TestReport:
        """运行所有测试"""
        
        unit_result = TestResult(test_type="unit")
        integration_result = TestResult(test_type="integration")
        regression_result = TestResult(test_type="regression")
        
        # 运行单元测试
        if include_unit:
            unit_result = self.run_unit_tests()
        
        # 如果单元测试通过，运行集成测试
        if include_integration and unit_result.success:
            integration_result = self.run_integration_tests()
        
        # 如果集成测试通过，运行回归测试
        if include_regression and integration_result.success:
            regression_result = self.run_regression_tests()
        
        return TestReport(
            session_id=session_id,
            generated_at=datetime.now(),
            unit_tests=unit_result,
            integration_tests=integration_result,
            regression_tests=regression_result
        )
    
    def run_unit_tests(self) -> TestResult:
        """运行单元测试"""
        result = TestResult(test_type="unit")
        
        # 检测测试框架
        test_framework = self._detect_test_framework()
        
        if test_framework == "pytest":
            result = self._run_pytest()
        elif test_framework == "junit":
            result = self._run_junit()
        elif test_framework == "maven":
            result = self._run_maven_test()
        else:
            result.output = "未检测到测试框架"
        
        return result
    
    def run_integration_tests(self) -> TestResult:
        """运行集成测试"""
        result = TestResult(test_type="integration")
        
        # 查找集成测试目录
        integration_test_dirs = [
            self.project_path / "tests" / "integration",
            self.project_path / "test" / "integration",
            self.project_path / "src" / "test" / "java" / "integration"
        ]
        
        test_dir = None
        for d in integration_test_dirs:
            if d.exists():
                test_dir = d
                break
        
        if not test_dir:
            result.output = "未找到集成测试目录"
            return result
        
        # 运行测试
        test_framework = self._detect_test_framework()
        
        if test_framework == "pytest":
            result = self._run_pytest(str(test_dir))
        elif test_framework in ["junit", "maven"]:
            result = self._run_maven_test("integration")
        
        return result
    
    def run_regression_tests(self) -> TestResult:
        """运行回归测试"""
        # 回归测试通常是运行所有测试
        return self.run_unit_tests()
    
    def _detect_test_framework(self) -> str:
        """检测测试框架"""
        # Python项目
        if (self.project_path / "pytest.ini").exists():
            return "pytest"
        if (self.project_path / "setup.py").exists():
            return "pytest"
        if list(self.project_path.glob("test_*.py")):
            return "pytest"
        
        # Java项目
        if (self.project_path / "pom.xml").exists():
            return "maven"
        if (self.project_path / "build.gradle").exists():
            return "junit"
        
        return "unknown"
    
    def _run_pytest(self, test_path: str = None) -> TestResult:
        """运行pytest"""
        result = TestResult(test_type="unit")
        
        try:
            cmd = ["pytest", "-v", "--json-report", "--json-report-file=-"]
            
            if test_path:
                cmd.append(test_path)
            else:
                cmd.append("tests/")
            
            start_time = time.time()
            
            process = subprocess.run(
                cmd,
                cwd=self.project_path,
                capture_output=True,
                text=True,
                timeout=300  # 5分钟超时
            )
            
            result.duration = time.time() - start_time
            result.output = process.stdout
            
            # 解析输出
            self._parse_pytest_output(process.stdout, result)
            
        except subprocess.TimeoutExpired:
            result.output = "测试超时"
        except FileNotFoundError:
            result.output = "pytest未安装"
        except Exception as e:
            result.output = str(e)
        
        return result
    
    def _parse_pytest_output(self, output: str, result: TestResult):
        """解析pytest输出"""
        lines = output.split("\n")
        
        for line in lines:
            # 解析测试数量
            if "passed" in line.lower():
                parts = line.split()
                for i, part in enumerate(parts):
                    if part.isdigit():
                        if i + 1 < len(parts) and "passed" in parts[i + 1].lower():
                            result.passed = int(part)
                        elif i + 1 < len(parts) and "failed" in parts[i + 1].lower():
                            result.failed = int(part)
                        elif i + 1 < len(parts) and "skipped" in parts[i + 1].lower():
                            result.skipped = int(part)
        
        result.total = result.passed + result.failed + result.skipped
        
        # 解析失败测试
        current_failure = None
        for line in lines:
            if "FAILED" in line:
                current_failure = {
                    "name": line.split("FAILED")[-1].strip(),
                    "message": ""
                }
                result.failures.append(current_failure)
            elif current_failure and line.strip():
                current_failure["message"] += line + "\n"
    
    def _run_maven_test(self, test_type: str = "unit") -> TestResult:
        """运行Maven测试"""
        result = TestResult(test_type=test_type)
        
        try:
            cmd = ["mvn", "test", "-q"]
            
            start_time = time.time()
            
            process = subprocess.run(
                cmd,
                cwd=self.project_path,
                capture_output=True,
                text=True,
                timeout=600  # 10分钟超时
            )
            
            result.duration = time.time() - start_time
            result.output = process.stdout
            
            # 解析输出
            self._parse_maven_output(process.stdout, result)
            
        except subprocess.TimeoutExpired:
            result.output = "测试超时"
        except FileNotFoundError:
            result.output = "Maven未安装"
        except Exception as e:
            result.output = str(e)
        
        return result
    
    def _parse_maven_output(self, output: str, result: TestResult):
        """解析Maven输出"""
        lines = output.split("\n")
        
        for line in lines:
            if "Tests run:" in line:
                # 格式: Tests run: 100, Failures: 2, Errors: 0, Skipped: 5
                parts = line.split(",")
                for part in parts:
                    if "Tests run:" in part:
                        result.total = int(part.split(":")[1].strip())
                    elif "Failures:" in part:
                        result.failed = int(part.split(":")[1].strip())
                    elif "Skipped:" in part:
                        result.skipped = int(part.split(":")[1].strip())
        
        result.passed = result.total - result.failed - result.skipped
    
    def _run_junit(self) -> TestResult:
        """运行JUnit测试"""
        return self._run_maven_test()
    
    def check_code_compiles(self) -> bool:
        """检查代码是否可编译"""
        test_framework = self._detect_test_framework()
        
        if test_framework == "maven":
            try:
                process = subprocess.run(
                    ["mvn", "compile", "-q"],
                    cwd=self.project_path,
                    capture_output=True,
                    timeout=120
                )
                return process.returncode == 0
            except Exception:
                return False
        
        elif test_framework == "pytest":
            try:
                process = subprocess.run(
                    ["python", "-m", "py_compile"] + 
                    list(str(p) for p in self.project_path.glob("**/*.py")[:50]),
                    cwd=self.project_path,
                    capture_output=True,
                    timeout=60
                )
                return process.returncode == 0
            except Exception:
                return False
        
        return True


class AutoFixer:
    """自动修正器"""
    
    def __init__(self, config: Dict = None):
        self.config = config or {}
    
    def auto_fix(
        self,
        test_result: TestResult,
        refactoring_result: Any,
        max_attempts: int = 3
    ) -> List[Dict]:
        """自动修复测试失败"""
        fixes = []
        
        for failure in test_result.failures[:max_attempts]:
            fix = self._attempt_fix(failure, refactoring_result)
            if fix:
                fixes.append(fix)
        
        return fixes
    
    def _attempt_fix(
        self,
        failure: Dict,
        refactoring_result: Any
    ) -> Optional[Dict]:
        """尝试修复单个失败"""
        failure_name = failure.get("name", "")
        failure_message = failure.get("message", "")
        
        # 根据错误类型选择修复策略
        if "ImportError" in failure_message or "ModuleNotFoundError" in failure_message:
            return self._fix_import_error(failure, refactoring_result)
        
        elif "AttributeError" in failure_message:
            return self._fix_attribute_error(failure, refactoring_result)
        
        elif "TypeError" in failure_message:
            return self._fix_type_error(failure, refactoring_result)
        
        elif "AssertionError" in failure_message:
            return self._fix_assertion_error(failure, refactoring_result)
        
        return None
    
    def _fix_import_error(
        self,
        failure: Dict,
        refactoring_result: Any
    ) -> Optional[Dict]:
        """修复导入错误"""
        # 导入错误通常是因为重构改变了模块结构
        # 需要更新导入语句
        return {
            "type": "import_fix",
            "description": "需要更新导入语句",
            "manual": True  # 需要人工确认
        }
    
    def _fix_attribute_error(
        self,
        failure: Dict,
        refactoring_result: Any
    ) -> Optional[Dict]:
        """修复属性错误"""
        # 属性错误可能是因为重命名
        return {
            "type": "attribute_fix",
            "description": "属性名称可能需要更新",
            "manual": True
        }
    
    def _fix_type_error(
        self,
        failure: Dict,
        refactoring_result: Any
    ) -> Optional[Dict]:
        """修复类型错误"""
        return {
            "type": "type_fix",
            "description": "类型不匹配，需要检查函数签名",
            "manual": True
        }
    
    def _fix_assertion_error(
        self,
        failure: Dict,
        refactoring_result: Any
    ) -> Optional[Dict]:
        """修复断言错误"""
        # 断言错误可能意味着重构改变了行为
        # 这是严重问题，需要人工干预
        return {
            "type": "assertion_fix",
            "description": "测试断言失败，可能是行为改变",
            "manual": True,
            "priority": "high"
        }


__all__ = [
    'TestResult',
    'TestReport',
    'TestRunner',
    'AutoFixer'
]
