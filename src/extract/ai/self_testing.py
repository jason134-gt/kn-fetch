import os
import tempfile
import subprocess
from typing import Dict, Any, List, Tuple
from pathlib import Path
import importlib.util
import sys

from .ai_client import AIClient
from ..gitnexus import CodeEntity

class SelfTester:
    """自测试引擎，自动生成和运行测试用例"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.ai_client = AIClient(config_path)
        self.config = self._load_config(config_path)
        self.test_config = self.config.get("testing", {})
        self.temp_dir = Path(tempfile.mkdtemp(prefix="kn_fetch_test_"))
        
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """加载配置文件"""
        import yaml
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                return yaml.safe_load(f)
        except Exception as e:
            raise Exception(f"加载配置文件失败: {str(e)}")
    
    def test_entity(self, entity: CodeEntity) -> Dict[str, Any]:
        """为单个代码实体生成并运行测试"""
        if entity.entity_type not in ["function", "method", "class"]:
            return {"status": "skipped", "reason": "不支持的实体类型"}
        
        print(f"正在为 {entity.name} 生成测试用例...")
        
        # 生成测试用例
        test_code = self.ai_client.generate_test_cases(
            entity.content,
            language=entity.language if hasattr(entity, 'language') else "python"
        )
        
        # 保存测试文件
        test_file = self.temp_dir / f"test_{entity.name}.py"
        with open(test_file, "w", encoding="utf-8") as f:
            f.write(test_code)
        
        # 运行测试
        test_result = self._run_test(test_file)
        
        # 分析测试结果
        analysis = self._analyze_test_result(entity, test_code, test_result)
        
        return {
            "entity_name": entity.name,
            "test_code": test_code,
            "test_result": test_result,
            "analysis": analysis
        }
    
    def _run_test(self, test_file: Path) -> Dict[str, Any]:
        """运行测试用例"""
        try:
            result = subprocess.run(
                [sys.executable, "-m", "pytest", str(test_file), "-v"],
                capture_output=True,
                text=True,
                timeout=30
            )
            
            return {
                "returncode": result.returncode,
                "stdout": result.stdout,
                "stderr": result.stderr,
                "passed": result.returncode == 0
            }
        except subprocess.TimeoutExpired:
            return {
                "returncode": -1,
                "stdout": "",
                "stderr": "测试运行超时",
                "passed": False,
                "timeout": True
            }
        except Exception as e:
            return {
                "returncode": -2,
                "stdout": "",
                "stderr": f"测试运行出错: {str(e)}",
                "passed": False,
                "error": str(e)
            }
    
    def _analyze_test_result(
        self,
        entity: CodeEntity,
        test_code: str,
        test_result: Dict[str, Any]
    ) -> Dict[str, Any]:
        """分析测试结果，给出改进建议"""
        if test_result["passed"]:
            return {
                "status": "passed",
                "message": "测试通过"
            }
        
        system_prompt = """你是测试专家，请分析测试失败的原因，并给出代码修复建议。返回JSON格式结果，包含failure_reason（失败原因）、code_issues（代码问题列表）、fix_suggestions（修复建议）、test_improvements（测试用例改进建议）字段。"""
        
        user_content = f"""
代码实体: {entity.name}
代码内容:
```
{entity.content}
```

测试代码:
```
{test_code}
```

测试输出:
标准输出:
{test_result["stdout"]}

标准错误:
{test_result["stderr"]}
"""
        
        messages = [{"role": "user", "content": user_content}]
        response = self.ai_client.chat_completion(messages, system_prompt)
        
        try:
            return json.loads(response)
        except:
            return {"raw_analysis": response}
    
    def run_regression_tests(self, entities: List[CodeEntity]) -> List[Dict[str, Any]]:
        """运行回归测试"""
        results = []
        for entity in entities:
            result = self.test_entity(entity)
            results.append(result)
        
        return results
    
    def cleanup(self):
        """清理临时文件"""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
