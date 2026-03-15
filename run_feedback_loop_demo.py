#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
用户反馈闭环系统 - 演示脚本

运行方式:
    python run_feedback_loop_demo.py

功能:
    1. 创建会话
    2. 加载/创建报告
    3. 提交反馈
    4. 重新生成报告
    5. 审核工作流
    6. 收敛检测
"""

import sys
from pathlib import Path
from datetime import datetime
import json

sys.path.insert(0, str(Path(__file__).parent))

from src.refactor.core.feedback_processor import (
    FeedbackType, create_feedback, create_feedback_batch
)
from src.refactor.core.report_regenerator import ReportRegenerator
from src.refactor.core.review_workflow import (
    ReviewWorkflow, ReviewAction, ConvergenceDetector
)
from src.refactor.core.problem_detail import (
    ProblemDetail, ProblemSummary, Severity, EnhancedRefactoringReport
)


def create_demo_report() -> EnhancedRefactoringReport:
    """创建演示报告"""
    problems = [
        ProblemDetail(
            problem_id="P001",
            problem_type="large_file",
            severity=Severity.HIGH,
            entity_name="Counter.java",
            file_path="src/main/java/Counter.java",
            line_start=1,
            line_end=682,
            description="文件过大，包含682行代码，204个函数",
            current_state={"lines": 682, "functions": 204},
            solutions=[
                {
                    "name": "按职责拆分",
                    "description": "将Counter类按职责拆分为多个小类",
                    "workload_hours": 3.0,
                    "recommendation": 4
                },
                {
                    "name": "提取工具类",
                    "description": "提取工具方法到独立的工具类",
                    "workload_hours": 2.0,
                    "recommendation": 3
                }
            ],
            recommended_solution=0
        ),
        ProblemDetail(
            problem_id="P002",
            problem_type="missing_docs",
            severity=Severity.MEDIUM,
            entity_name="RealRuleParse.java",
            file_path="src/main/java/RealRuleParse.java",
            line_start=1,
            line_end=825,
            description="缺少必要的文档注释",
            solutions=[
                {
                    "name": "添加JavaDoc",
                    "description": "为公共方法添加JavaDoc文档",
                    "workload_hours": 2.0,
                    "recommendation": 3
                }
            ],
            recommended_solution=0
        ),
        ProblemDetail(
            problem_id="P003",
            problem_type="complex_method",
            severity=Severity.LOW,
            entity_name="DataProcessor.processData",
            file_path="src/main/java/DataProcessor.java",
            line_start=150,
            line_end=280,
            description="方法复杂度过高，圈复杂度25",
            solutions=[
                {
                    "name": "重构方法",
                    "description": "将复杂方法拆分为多个小方法",
                    "workload_hours": 4.0,
                    "recommendation": 4
                }
            ],
            recommended_solution=0
        ),
        ProblemDetail(
            problem_id="P004",
            problem_type="duplicate_code",
            severity=Severity.MEDIUM,
            entity_name="UserService",
            file_path="src/main/java/UserService.java",
            description="存在重复代码块",
            solutions=[
                {
                    "name": "提取公共方法",
                    "description": "将重复代码提取为公共方法",
                    "workload_hours": 1.5,
                    "recommendation": 4
                }
            ],
            recommended_solution=0
        ),
        ProblemDetail(
            problem_id="P005",
            problem_type="long_parameter_list",
            severity=Severity.LOW,
            entity_name="OrderService.createOrder",
            file_path="src/main/java/OrderService.java",
            description="参数列表过长(8个参数)",
            solutions=[
                {
                    "name": "使用参数对象",
                    "description": "创建参数对象封装多个参数",
                    "workload_hours": 1.0,
                    "recommendation": 3
                }
            ],
            recommended_solution=0
        )
    ]
    
    return EnhancedRefactoringReport(
        report_id=f"RPT-{datetime.now().strftime('%Y%m%d%H%M%S')}",
        project_name="stock_datacenter",
        generated_at=datetime.now(),
        summary=ProblemSummary(
            total_problems=5,
            high_severity=1,
            medium_severity=2,
            low_severity=2,
            estimated_workload_hours=11.5,
            problem_types={
                "large_file": 1,
                "missing_docs": 1,
                "complex_method": 1,
                "duplicate_code": 1,
                "long_parameter_list": 1
            }
        ),
        problems=problems,
        version=1
    )


def run_demo():
    """运行演示"""
    print("\n" + "=" * 70)
    print("             用户反馈闭环系统 - 完整演示")
    print("=" * 70)
    
    # ========== 步骤1: 创建初始报告 ==========
    print("\n[步骤1] 创建初始重构分析报告")
    print("-" * 50)
    
    report_v1 = create_demo_report()
    print(f"  报告ID: {report_v1.report_id}")
    print(f"  项目: {report_v1.project_name}")
    print(f"  版本: V{report_v1.version}")
    print(f"  问题总数: {report_v1.summary.total_problems}")
    print(f"  高优先级: {report_v1.summary.high_severity}")
    print(f"  中优先级: {report_v1.summary.medium_severity}")
    print(f"  低优先级: {report_v1.summary.low_severity}")
    print(f"  预计工时: {report_v1.summary.estimated_workload_hours}h")
    
    print("\n  问题清单:")
    for p in report_v1.problems:
        print(f"    - {p.problem_id}: [{p.severity.value}] {p.problem_type} - {p.entity_name}")
    
    # ========== 步骤2: 创建审核工作流 ==========
    print("\n[步骤2] 创建审核工作流")
    print("-" * 50)
    
    workflow = ReviewWorkflow()
    review = workflow.create_review("demo-session", report_v1.report_id)
    print(f"  审核记录: {review.record_id}")
    print(f"  初始状态: {review.status.value}")
    print(f"  允许动作: {[a.value for a in workflow.get_allowed_actions(review.record_id)]}")
    
    # 提交审核
    workflow.execute_action(review.record_id, ReviewAction.SUBMIT, "developer1", "提交审核")
    print(f"\n  执行 SUBMIT 后状态: {review.status.value}")
    
    # 开始审核
    workflow.execute_action(review.record_id, ReviewAction.START_REVIEW, "reviewer1", "开始审核")
    print(f"  执行 START_REVIEW 后状态: {review.status.value}")
    
    # ========== 步骤3: 用户提交反馈 ==========
    print("\n[步骤3] 用户提交反馈")
    print("-" * 50)
    
    batch = create_feedback_batch("demo-session")
    
    # 添加各类反馈
    feedbacks = [
        ("P001", FeedbackType.AGREE, "确认此问题，同意按职责拆分"),
        ("P002", FeedbackType.MODIFY, "建议提高优先级", "high"),
        ("P003", FeedbackType.AGREE, "确认问题，选择推荐方案"),
        ("P004", FeedbackType.IGNORE_PROBLEM, "暂时忽略，下个版本处理"),
        ("P005", FeedbackType.DISAGREE, "参数数量合理，不需要修改"),
    ]
    
    for item in feedbacks:
        problem_id = item[0]
        fb_type = item[1]
        content = item[2]
        suggested_priority = item[3] if len(item) > 3 else None
        
        feedback = create_feedback(
            session_id="demo-session",
            problem_id=problem_id,
            feedback_type=fb_type,
            content=content,
            suggested_priority=suggested_priority
        )
        batch.add_feedback(feedback)
        print(f"  + {problem_id}: {fb_type.value} - {content[:20]}...")
    
    print(f"\n  反馈批次: {batch.batch_id}")
    print(f"  反馈总数: {batch.total_count}")
    print(f"  类型统计: {batch.by_type}")
    
    # ========== 步骤4: 重新生成报告 ==========
    print("\n[步骤4] 根据反馈重新生成报告")
    print("-" * 50)
    
    regenerator = ReportRegenerator()
    report_v2 = regenerator.regenerate(report_v1, batch)
    
    print(f"  新报告ID: {report_v2.report_id}")
    print(f"  版本: V{report_v2.version}")
    print(f"  父报告ID: {report_v2.parent_report_id}")
    print(f"  问题总数: {report_v2.summary.total_problems}")
    
    print("\n  变更日志:")
    change_log = report_v2.change_log or {}
    print(f"    - 新增问题: {change_log.get('added_problems', 0)}")
    print(f"    - 移除问题: {change_log.get('removed_problems', 0)}")
    print(f"    - 修改问题: {change_log.get('modified_problems', 0)}")
    print(f"    - 确认问题: {change_log.get('confirmed_problems', 0)}")
    print(f"    - 忽略问题: {change_log.get('ignored_problems', 0)}")
    
    print("\n  当前问题状态:")
    for p in report_v2.problems:
        status = "已确认" if getattr(p, 'user_confirmed', False) else ("已忽略" if getattr(p, 'ignored', False) else "待处理")
        print(f"    - {p.problem_id}: [{p.severity.value}] {status}")
    
    # ========== 步骤5: 收敛检测 ==========
    print("\n[步骤5] 检测迭代收敛状态")
    print("-" * 50)
    
    detector = ConvergenceDetector()
    convergence = detector.check_convergence([report_v1, report_v2])
    
    print(f"  是否收敛: {convergence['converged']}")
    print(f"  收敛原因: {convergence['reason']}")
    print(f"  说明: {convergence.get('message', 'N/A')}")
    
    # ========== 步骤6: 完成审核 ==========
    print("\n[步骤6] 完成审核流程")
    print("-" * 50)
    
    # 提交反馈后请求重新生成
    workflow.execute_action(
        review.record_id,
        ReviewAction.SUBMIT_FEEDBACK,
        "reviewer1",
        "反馈已提交",
        batch.batch_id
    )
    print(f"  SUBMIT_FEEDBACK 后状态: {review.status.value}")
    
    workflow.execute_action(
        review.record_id,
        ReviewAction.REQUEST_REGENERATE,
        "reviewer1",
        "请求重新生成报告"
    )
    print(f"  REQUEST_REGENERATE 后状态: {review.status.value}")
    print(f"  重生成次数: {review.regenerate_count}")
    
    # 重新生成后需要再次提交审核
    workflow.execute_action(
        review.record_id,
        ReviewAction.SUBMIT,
        "developer1",
        "重新提交审核"
    )
    print(f"  SUBMIT 后状态: {review.status.value}")
    
    # 开始新一轮审核
    workflow.execute_action(
        review.record_id,
        ReviewAction.START_REVIEW,
        "reviewer1",
        "开始新一轮审核"
    )
    print(f"  START_REVIEW 后状态: {review.status.value}")
    
    # 批准报告
    workflow.execute_action(
        review.record_id,
        ReviewAction.APPROVE,
        "reviewer1",
        "报告审核通过"
    )
    print(f"\n  APPROVE 后状态: {review.status.value}")
    
    # 定稿
    workflow.execute_action(
        review.record_id,
        ReviewAction.FINALIZE,
        "reviewer1",
        "报告已定稿"
    )
    print(f"  FINALIZE 后状态: {review.status.value}")
    
    # ========== 步骤7: 保存报告 ==========
    print("\n[步骤7] 保存最终报告")
    print("-" * 50)
    
    output_dir = Path("output/refactor") / report_v2.project_name
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # 保存JSON
    json_path = output_dir / f"report_v{report_v2.version}.json"
    with open(json_path, "w", encoding="utf-8") as f:
        data = report_v2.model_dump(mode='json')
        json.dump(data, f, ensure_ascii=False, indent=2, default=str)
    print(f"  JSON保存: {json_path}")
    
    # ========== 总结 ==========
    print("\n" + "=" * 70)
    print("                           演示完成!")
    print("=" * 70)
    
    print("\n[总结]")
    print(f"  - 初始问题数: {report_v1.summary.total_problems}")
    print(f"  - 最终问题数: {report_v2.summary.total_problems} (移除{change_log.get('removed_problems', 0)}个)")
    print(f"  - 确认问题: {change_log.get('confirmed_problems', 0)}个")
    print(f"  - 忽略问题: {change_log.get('ignored_problems', 0)}个")
    print(f"  - 迭代次数: {review.regenerate_count}")
    print(f"  - 最终状态: {review.status.value}")
    print(f"  - 收敛状态: {'已收敛' if convergence['converged'] else '未收敛'}")
    
    print("\n[后续操作]")
    print("  - 查看报告: cat output/refactor/stock_datacenter/report_v2.json")
    print("  - 继续反馈: python -m src.refactor.cli.feedback_cli")
    
    print("\n" + "=" * 70 + "\n")


if __name__ == "__main__":
    run_demo()
