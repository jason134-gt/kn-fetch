"""
用户反馈闭环CLI工具 - 提供命令行界面用于交互式反馈
"""

import argparse
import json
import sys
from pathlib import Path
from datetime import datetime
from typing import Optional, List

# 添加项目根目录到路径
sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent))

from src.refactor.core.feedback_processor import (
    FeedbackType, FeedbackScope, UserFeedback, FeedbackBatch,
    FeedbackProcessor, create_feedback, create_feedback_batch
)
from src.refactor.core.report_regenerator import ReportRegenerator
from src.refactor.core.review_workflow import (
    ReviewWorkflow, ReviewAction, ReviewStatus, ReviewRecord,
    ConvergenceDetector, ConvergenceConfig
)
from src.refactor.core.problem_detail import (
    ProblemDetail, ProblemSummary, Severity, EnhancedRefactoringReport
)
from src.refactor.api.feedback_api import FeedbackApi, FeedbackService


class FeedbackLoopCLI:
    """反馈闭环CLI"""
    
    def __init__(self, output_dir: str = "output/refactor"):
        self.output_dir = Path(output_dir)
        self.api = FeedbackApi()
        self.current_session: Optional[str] = None
        self.current_report: Optional[EnhancedRefactoringReport] = None
        self.current_review: Optional[str] = None
        self.report_versions: List[EnhancedRefactoringReport] = []
        
    def create_session(self, project_name: str) -> str:
        """创建会话"""
        result = self.api.create_session(project_name)
        if result.success:
            self.current_session = result.data["session_id"]
            print(f"[OK] 创建会话: {self.current_session}")
            return self.current_session
        else:
            print(f"[ERROR] {result.message}")
            return None
    
    def load_report(self, report_path: str) -> bool:
        """加载报告"""
        if not self.current_session:
            print("[ERROR] 请先创建会话")
            return False
        
        result = self.api.load_report(self.current_session, report_path)
        if result.success:
            print(f"[OK] 加载报告: {result.data['report_id']}")
            print(f"     项目: {result.data['project_name']}")
            print(f"     版本: {result.data['version']}")
            print(f"     问题数: {result.data['total_problems']}")
            return True
        else:
            print(f"[ERROR] {result.message}")
            return False
    
    def show_report_summary(self):
        """显示报告摘要"""
        if not self.current_session:
            print("[ERROR] 没有活动会话")
            return
        
        result = self.api.get_report_versions(self.current_session)
        if result.success and result.data["versions"]:
            versions = result.data["versions"]
            latest = versions[-1]
            
            print("\n" + "=" * 60)
            print(f"报告摘要 - V{latest['version']}")
            print("=" * 60)
            print(f"报告ID: {latest['report_id']}")
            print(f"生成时间: {latest['generated_at']}")
            print(f"问题总数: {latest['total_problems']}")
            
            if latest.get('change_log'):
                log = latest['change_log']
                print("\n变更记录:")
                print(f"  - 新增问题: {log.get('added_problems', 0)}")
                print(f"  - 移除问题: {log.get('removed_problems', 0)}")
                print(f"  - 修改问题: {log.get('modified_problems', 0)}")
                print(f"  - 确认问题: {log.get('confirmed_problems', 0)}")
                print(f"  - 忽略问题: {log.get('ignored_problems', 0)}")
            print("=" * 60 + "\n")
    
    def submit_feedback_interactive(self):
        """交互式提交反馈"""
        if not self.current_session:
            print("[ERROR] 请先创建会话")
            return
        
        print("\n" + "=" * 60)
        print("提交反馈")
        print("=" * 60)
        
        # 选择反馈类型
        print("\n选择反馈类型:")
        feedback_types = [
            ("1", "agree", "同意问题"),
            ("2", "disagree", "不同意问题"),
            ("3", "modify", "修改问题"),
            ("4", "add_solution", "添加方案"),
            ("5", "raise_priority", "提高优先级"),
            ("6", "lower_priority", "降低优先级"),
            ("7", "ignore_problem", "忽略问题"),
        ]
        
        for num, _, desc in feedback_types:
            print(f"  {num}. {desc}")
        
        choice = input("\n请选择 (1-7): ").strip()
        type_map = {num: fb_type for num, fb_type, _ in feedback_types}
        feedback_type = type_map.get(choice, "modify")
        
        # 输入问题ID
        problem_id = input("问题ID (如 P001): ").strip()
        
        # 输入反馈内容
        content = input("反馈内容: ").strip()
        
        # 可选：建议优先级
        suggested_priority = None
        if feedback_type in ["modify", "raise_priority", "lower_priority"]:
            print("建议优先级: 1.high  2.medium  3.low")
            priority_choice = input("选择 (留空跳过): ").strip()
            if priority_choice == "1":
                suggested_priority = "high"
            elif priority_choice == "2":
                suggested_priority = "medium"
            elif priority_choice == "3":
                suggested_priority = "low"
        
        # 提交反馈
        from src.refactor.api.feedback_api import FeedbackCreateRequest
        request = FeedbackCreateRequest(
            session_id=self.current_session,
            problem_id=problem_id,
            feedback_type=feedback_type,
            content=content,
            suggested_priority=suggested_priority
        )
        
        result = self.api.submit_feedback(request)
        if result.success:
            print(f"\n[OK] 反馈已创建: {result.data['feedback_id']}")
            return result.data['feedback_id']
        else:
            print(f"\n[ERROR] {result.message}")
            return None
    
    def submit_batch_feedback(self, feedbacks: List[dict]) -> str:
        """批量提交反馈"""
        from src.refactor.api.feedback_api import FeedbackBatchCreateRequest, FeedbackCreateRequest
        
        fb_list = [
            FeedbackCreateRequest(
                session_id=self.current_session,
                problem_id=fb.get("problem_id"),
                feedback_type=fb.get("feedback_type", "modify"),
                content=fb.get("content", ""),
                suggested_priority=fb.get("suggested_priority")
            )
            for fb in feedbacks
        ]
        
        request = FeedbackBatchCreateRequest(
            session_id=self.current_session,
            feedbacks=fb_list
        )
        
        result = self.api.submit_feedback_batch(request)
        if result.success:
            print(f"[OK] 批量反馈已创建: {result.data['batch_id']}")
            print(f"     反馈数量: {result.data['total_count']}")
            print(f"     类型统计: {result.data['by_type']}")
            return result.data['batch_id']
        else:
            print(f"[ERROR] {result.message}")
            return None
    
    def regenerate_report(self, batch_id: str = None):
        """重新生成报告"""
        if not self.current_session:
            print("[ERROR] 请先创建会话")
            return None
        
        # 获取最近的反馈批次
        if not batch_id:
            history = self.api.get_feedback_history(self.current_session)
            if history.success and history.data["history"]:
                batch_id = history.data["history"][-1]["batch_id"]
            else:
                print("[ERROR] 没有找到反馈批次")
                return None
        
        from src.refactor.api.feedback_api import RegenerateRequest
        request = RegenerateRequest(
            session_id=self.current_session,
            feedback_batch_id=batch_id
        )
        
        result = self.api.regenerate_report(request)
        if result.success:
            print(f"\n[OK] 报告已重新生成")
            print(f"     新版本: V{result.data['version']}")
            print(f"     报告ID: {result.data['report_id']}")
            print(f"     问题总数: {result.data['total_problems']}")
            
            if result.data.get('change_log'):
                log = result.data['change_log']
                print("\n变更记录:")
                print(f"  - 新增问题: {log.get('added_problems', 0)}")
                print(f"  - 移除问题: {log.get('removed_problems', 0)}")
                print(f"  - 修改问题: {log.get('modified_problems', 0)}")
            
            return result.data['report_id']
        else:
            print(f"[ERROR] {result.message}")
            return None
    
    def create_review(self) -> str:
        """创建审核"""
        if not self.current_session:
            print("[ERROR] 请先创建会话")
            return None
        
        # 获取最新报告
        versions = self.api.get_report_versions(self.current_session)
        if not versions.success or not versions.data["versions"]:
            print("[ERROR] 没有可用的报告")
            return None
        
        report_id = versions.data["versions"][-1]["report_id"]
        
        result = self.api.create_review(self.current_session, report_id)
        if result.success:
            self.current_review = result.data["record_id"]
            print(f"[OK] 创建审核: {self.current_review}")
            print(f"     状态: {result.data['status']}")
            return self.current_review
        else:
            print(f"[ERROR] {result.message}")
            return None
    
    def execute_review_action(self, action: str, comments: str = None):
        """执行审核动作"""
        if not self.current_review:
            print("[ERROR] 请先创建审核")
            return
        
        from src.refactor.api.feedback_api import ReviewActionRequest
        request = ReviewActionRequest(
            session_id=self.current_session,
            record_id=self.current_review,
            action=action,
            comments=comments
        )
        
        result = self.api.execute_review_action(request)
        if result.success:
            print(f"[OK] 动作执行成功")
            print(f"     当前状态: {result.data['status']}")
        else:
            print(f"[ERROR] {result.message}")
    
    def check_convergence(self) -> dict:
        """检查收敛状态"""
        if not self.current_session:
            print("[ERROR] 请先创建会话")
            return None
        
        versions = self.api.get_report_versions(self.current_session)
        if not versions.success:
            return None
        
        # 这里需要实际的报告对象
        # 简化处理，返回模拟结果
        versions_list = versions.data["versions"]
        if len(versions_list) < 2:
            return {"converged": False, "reason": "insufficient_history"}
        
        # 检查变更率
        latest = versions_list[-1]
        if latest.get('change_log'):
            log = latest['change_log']
            total_changes = (
                log.get('added_problems', 0) +
                log.get('removed_problems', 0) +
                log.get('modified_problems', 0)
            )
            total = latest.get('total_problems', 1)
            change_rate = total_changes / max(total, 1)
            
            if change_rate < 0.1:
                return {
                    "converged": True,
                    "reason": "low_change_rate",
                    "change_rate": change_rate
                }
        
        return {"converged": False, "reason": "still_evolving"}
    
    def run_demo(self):
        """运行演示"""
        print("\n" + "=" * 60)
        print("用户反馈闭环系统演示")
        print("=" * 60)
        
        # 1. 创建会话
        print("\n[步骤1] 创建会话")
        self.create_session("demo-project")
        
        # 2. 加载报告
        print("\n[步骤2] 加载报告")
        report_path = self.output_dir / "stock_datacenter" / "data" / "report_v1.json"
        if report_path.exists():
            self.load_report(str(report_path))
        else:
            print(f"[WARN] 报告文件不存在: {report_path}")
            print("       将创建模拟报告...")
            self._create_mock_report()
        
        # 3. 显示摘要
        print("\n[步骤3] 显示报告摘要")
        self.show_report_summary()
        
        # 4. 创建审核
        print("\n[步骤4] 创建审核")
        self.create_review()
        
        # 5. 提交反馈
        print("\n[步骤5] 提交反馈")
        batch_id = self.submit_batch_feedback([
            {"problem_id": "P001", "feedback_type": "agree", "content": "确认此问题"},
            {"problem_id": "P002", "feedback_type": "modify", "content": "建议调整优先级", "suggested_priority": "medium"},
            {"problem_id": "P003", "feedback_type": "disagree", "content": "这不是问题"},
        ])
        
        # 6. 重新生成报告
        print("\n[步骤6] 重新生成报告")
        self.regenerate_report(batch_id)
        
        # 7. 检查收敛
        print("\n[步骤7] 检查收敛状态")
        result = self.check_convergence()
        if result:
            print(f"收敛状态: {result}")
        
        # 8. 显示最终摘要
        print("\n[步骤8] 显示最终报告摘要")
        self.show_report_summary()
        
        print("\n" + "=" * 60)
        print("演示完成")
        print("=" * 60)
    
    def _create_mock_report(self):
        """创建模拟报告"""
        from datetime import datetime
        
        problems = [
            ProblemDetail(
                problem_id="P001",
                problem_type="large_file",
                severity=Severity.HIGH,
                entity_name="Counter.java",
                file_path="src/main/java/Counter.java",
                description="文件过大，包含682行代码",
                solutions=[
                    {"name": "按职责拆分", "workload_hours": 3.0, "recommendation": 4}
                ]
            ),
            ProblemDetail(
                problem_id="P002",
                problem_type="missing_docs",
                severity=Severity.MEDIUM,
                entity_name="RealRuleParse.java",
                file_path="src/main/java/RealRuleParse.java",
                description="缺少必要文档",
                solutions=[
                    {"name": "添加JavaDoc", "workload_hours": 1.0, "recommendation": 3}
                ]
            ),
            ProblemDetail(
                problem_id="P003",
                problem_type="complex_method",
                severity=Severity.LOW,
                entity_name="DataProcessor",
                file_path="src/main/java/DataProcessor.java",
                description="方法复杂度过高",
                solutions=[
                    {"name": "重构方法", "workload_hours": 2.0, "recommendation": 3}
                ]
            )
        ]
        
        report = EnhancedRefactoringReport(
            report_id="RPT-DEMO-001",
            project_name="demo-project",
            generated_at=datetime.now(),
            summary=ProblemSummary(
                total_problems=3,
                high_severity=1,
                medium_severity=1,
                low_severity=1,
                estimated_workload_hours=6.0
            ),
            problems=problems,
            version=1
        )
        
        # 保存报告
        self.output_dir.mkdir(parents=True, exist_ok=True)
        data_dir = self.output_dir / "demo-project" / "data"
        data_dir.mkdir(parents=True, exist_ok=True)
        
        report_path = data_dir / "report_v1.json"
        with open(report_path, "w", encoding="utf-8") as f:
            data = report.model_dump(mode='json')
            json.dump(data, f, ensure_ascii=False, indent=2, default=str)
        
        print(f"[OK] 模拟报告已创建: {report_path}")
        
        # 加载到会话
        self.load_report(str(report_path))


def main():
    """主入口"""
    parser = argparse.ArgumentParser(description="用户反馈闭环CLI")
    parser.add_argument("--demo", action="store_true", help="运行演示")
    parser.add_argument("--session", type=str, help="会话ID")
    parser.add_argument("--report", type=str, help="报告路径")
    parser.add_argument("--action", type=str, help="动作类型")
    parser.add_argument("--problem-id", type=str, help="问题ID")
    parser.add_argument("--feedback-type", type=str, help="反馈类型")
    parser.add_argument("--content", type=str, help="反馈内容")
    
    args = parser.parse_args()
    
    cli = FeedbackLoopCLI()
    
    if args.demo:
        cli.run_demo()
    elif args.action:
        if args.action == "create_session":
            cli.create_session(args.session or "default")
        elif args.action == "load_report":
            cli.load_report(args.report)
        elif args.action == "submit_feedback":
            cli.submit_feedback_interactive()
        else:
            print(f"未知动作: {args.action}")
    else:
        # 交互模式
        cli.run_demo()


if __name__ == "__main__":
    main()
