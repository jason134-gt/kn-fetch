"""
数据存储访问层
为前端 API 提供真实数据访问
"""

import json
import os
from pathlib import Path
from typing import Any, Dict, List, Optional
from datetime import datetime
import glob


class DataStore:
    """数据存储基类"""

    def __init__(self, base_path: str = None):
        if base_path is None:
            # 默认使用项目根目录下的 output 文件夹
            project_root = Path(__file__).parent.parent.parent
            base_path = project_root / "output"
        self.base_path = Path(base_path)

    def _read_json(self, file_path: Path) -> Optional[Dict]:
        """读取 JSON 文件"""
        try:
            if file_path.exists():
                with open(file_path, 'r', encoding='utf-8') as f:
                    return json.load(f)
        except Exception as e:
            print(f"Error reading {file_path}: {e}")
        return None

    def _write_json(self, file_path: Path, data: Dict) -> bool:
        """写入 JSON 文件"""
        try:
            file_path.parent.mkdir(parents=True, exist_ok=True)
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2, default=str)
            return True
        except Exception as e:
            print(f"Error writing {file_path}: {e}")
        return False


class KnowledgeDataStore(DataStore):
    """知识提取数据存储"""

    def __init__(self, base_path: str = None):
        super().__init__(base_path)
        self.extract_path = self.base_path / "extract"
        self.knowledge_path = self.extract_path / "knowledge_graph"

    def get_all_projects(self) -> List[Dict]:
        """获取所有项目列表"""
        projects = []
        if not self.knowledge_path.exists():
            return projects

        for json_file in self.knowledge_path.glob("*.json"):
            data = self._read_json(json_file)
            if data and "entities" in data:
                project_name = json_file.stem
                entities = data.get("entities", {})

                # 统计实体
                entity_types = {}
                for entity in entities.values():
                    entity_type = entity.get("entity_type", "unknown")
                    entity_types[entity_type] = entity_types.get(entity_type, 0) + 1

                # 获取文件修改时间
                stat = json_file.stat()
                modified_time = datetime.fromtimestamp(stat.st_mtime).strftime("%Y-%m-%d %H:%M:%S")

                projects.append({
                    "project_name": project_name,
                    "entity_count": len(entities),
                    "entity_types": entity_types,
                    "file_path": str(json_file),
                    "modified_at": modified_time,
                })

        return projects

    def get_project_entities(
        self,
        project_name: str,
        entity_type: Optional[str] = None,
        keyword: Optional[str] = None,
        page: int = 1,
        page_size: int = 10
    ) -> Dict:
        """获取项目的知识实体"""
        json_file = self.knowledge_path / f"{project_name}.json"
        data = self._read_json(json_file)

        if not data or "entities" not in data:
            return {"items": [], "total": 0, "page": page, "page_size": page_size, "total_pages": 0}

        entities = list(data.get("entities", {}).values())

        # 筛选
        if entity_type:
            entities = [e for e in entities if e.get("entity_type") == entity_type]
        if keyword:
            keyword_lower = keyword.lower()
            entities = [
                e for e in entities
                if keyword_lower in e.get("name", "").lower()
                or keyword_lower in e.get("file_path", "").lower()
            ]

        # 分页
        total = len(entities)
        start = (page - 1) * page_size
        end = start + page_size
        items = entities[start:end]

        return {
            "items": items,
            "total": total,
            "page": page,
            "page_size": page_size,
            "total_pages": (total + page_size - 1) // page_size if total > 0 else 0
        }

    def get_entity_by_id(self, project_name: str, entity_id: str) -> Optional[Dict]:
        """获取单个实体"""
        json_file = self.knowledge_path / f"{project_name}.json"
        data = self._read_json(json_file)

        if data and "entities" in data:
            return data["entities"].get(entity_id)
        return None

    def get_entity_stats(self) -> Dict:
        """获取实体统计"""
        stats = {
            "total_entities": 0,
            "total_relations": 0,
            "entity_types": {},
            "projects": []
        }

        for project in self.get_all_projects():
            stats["total_entities"] += project["entity_count"]
            stats["projects"].append({
                "name": project["project_name"],
                "entity_count": project["entity_count"]
            })
            for entity_type, count in project["entity_types"].items():
                stats["entity_types"][entity_type] = stats["entity_types"].get(entity_type, 0) + count

        return stats


class RefactorDataStore(DataStore):
    """重构分析数据存储"""

    def __init__(self, base_path: str = None):
        super().__init__(base_path)
        self.refactor_path = self.base_path / "refactor"

    def get_all_reports(self) -> List[Dict]:
        """获取所有重构报告"""
        reports = []
        if not self.refactor_path.exists():
            return reports

        # 查找所有报告文件
        for report_file in self.refactor_path.glob("**/report_v*.json"):
            data = self._read_json(report_file)
            if data:
                # 获取文件修改时间
                stat = report_file.stat()
                modified_time = datetime.fromtimestamp(stat.st_mtime).strftime("%Y-%m-%d %H:%M:%S")

                summary = data.get("summary", {})
                reports.append({
                    "report_id": data.get("report_id"),
                    "project_name": data.get("project_name"),
                    "version": data.get("version", 1),
                    "status": self._determine_status(data),
                    "generated_at": data.get("generated_at", modified_time),
                    "total_problems": summary.get("total_problems", 0),
                    "high_severity": summary.get("high_severity", 0),
                    "medium_severity": summary.get("medium_severity", 0),
                    "low_severity": summary.get("low_severity", 0),
                    "estimated_hours": summary.get("estimated_workload_hours", 0),
                    "file_path": str(report_file),
                })

        return reports

    def _determine_status(self, report: Dict) -> str:
        """确定报告状态"""
        # 检查是否有用户确认
        problems = report.get("problems", [])
        if not problems:
            return "draft"

        confirmed_count = sum(1 for p in problems if p.get("user_confirmed"))
        if confirmed_count == len(problems):
            return "approved"
        elif confirmed_count > 0:
            return "pending_review"
        return "draft"

    def get_report(self, report_id: str) -> Optional[Dict]:
        """获取单个报告"""
        for report_file in self.refactor_path.glob("**/report_v*.json"):
            data = self._read_json(report_file)
            if data and data.get("report_id") == report_id:
                return data
        return None

    def get_report_by_project(self, project_name: str) -> Optional[Dict]:
        """根据项目名获取报告"""
        report_path = self.refactor_path / project_name / "report_v2.json"
        if report_path.exists():
            return self._read_json(report_path)
        return None

    def get_problem_stats(self) -> Dict:
        """获取问题统计"""
        stats = {
            "total_problems": 0,
            "by_severity": {"high": 0, "medium": 0, "low": 0},
            "by_type": {},
            "estimated_hours": 0
        }

        for report in self.get_all_reports():
            stats["total_problems"] += report.get("total_problems", 0)
            stats["by_severity"]["high"] += report.get("high_severity", 0)
            stats["by_severity"]["medium"] += report.get("medium_severity", 0)
            stats["by_severity"]["low"] += report.get("low_severity", 0)
            stats["estimated_hours"] += report.get("estimated_hours", 0)

        return stats


class SessionDataStore(DataStore):
    """会话数据存储"""

    def __init__(self, base_path: str = None):
        super().__init__(base_path)
        self.sessions_path = self.base_path / "refactor" / "sessions"

    def get_all_sessions(self) -> List[Dict]:
        """获取所有会话"""
        sessions = []
        if not self.sessions_path.exists():
            self.sessions_path.mkdir(parents=True, exist_ok=True)
            return sessions

        for session_file in self.sessions_path.glob("*.json"):
            data = self._read_json(session_file)
            if data:
                stat = session_file.stat()
                modified_time = datetime.fromtimestamp(stat.st_mtime).strftime("%Y-%m-%d %H:%M:%S")

                sessions.append({
                    "session_id": data.get("session_id"),
                    "project_name": data.get("project_name"),
                    "status": data.get("status", "created"),
                    "created_at": data.get("created_at", modified_time),
                    "updated_at": data.get("updated_at", modified_time),
                    "feedback_count": len(data.get("feedback_history", [])),
                    "pending_reviews": self._count_pending_reviews(data),
                })

        return sessions

    def _count_pending_reviews(self, session: Dict) -> int:
        """计算待审核数量"""
        # 简化实现，实际应该检查反馈状态
        feedback_history = session.get("feedback_history", [])
        return sum(1 for f in feedback_history if f.get("status") == "pending")

    def get_session(self, session_id: str) -> Optional[Dict]:
        """获取单个会话"""
        session_file = self.sessions_path / f"{session_id}.json"
        return self._read_json(session_file)

    def create_session(self, project_name: str, config: Dict = None) -> Dict:
        """创建新会话"""
        session_id = f"RSN-{datetime.now().strftime('%Y%m%d%H%M%S')}-{os.urandom(3).hex()}"
        now = datetime.now().isoformat()

        session = {
            "session_id": session_id,
            "project_name": project_name,
            "status": "created",
            "created_at": now,
            "updated_at": now,
            "config": config or {},
            "selected_problems": [],
            "feedback_history": [],
            "analysis_result": None,
            "refactoring_steps": [],
            "test_results": None,
            "diff_report": None,
            "backup_branch": None,
            "backup_commit": None,
            "can_rollback": True,
            "metadata": {}
        }

        session_file = self.sessions_path / f"{session_id}.json"
        self._write_json(session_file, session)
        return session

    def update_session(self, session_id: str, updates: Dict) -> Optional[Dict]:
        """更新会话"""
        session = self.get_session(session_id)
        if session:
            session.update(updates)
            session["updated_at"] = datetime.now().isoformat()
            session_file = self.sessions_path / f"{session_id}.json"
            self._write_json(session_file, session)
            return session
        return None


class DashboardDataStore:
    """仪表盘数据聚合"""

    def __init__(self, base_path: str = None):
        self.knowledge_store = KnowledgeDataStore(base_path)
        self.refactor_store = RefactorDataStore(base_path)
        self.session_store = SessionDataStore(base_path)

    def get_dashboard_stats(self) -> Dict:
        """获取仪表盘统计数据"""
        entity_stats = self.knowledge_store.get_entity_stats()
        problem_stats = self.refactor_store.get_problem_stats()
        sessions = self.session_store.get_all_sessions()

        pending_reviews = sum(s.get("pending_reviews", 0) for s in sessions)

        return {
            "projectCount": len(entity_stats["projects"]),
            "entityCount": entity_stats["total_entities"],
            "problemCount": problem_stats["total_problems"],
            "pendingReviewCount": pending_reviews,
            "projectTrend": {"type": "up", "value": 0},  # 需要历史数据计算
            "entityTrend": {"type": "up", "value": 0},
            "problemTrend": {"type": "down", "value": 0},
        }

    def get_knowledge_stats(self) -> Dict:
        """获取知识图谱统计"""
        entity_stats = self.knowledge_store.get_entity_stats()

        entity_types = [
            {"type": k, "count": v, "percentage": round(v / max(entity_stats["total_entities"], 1) * 100, 1)}
            for k, v in entity_stats["entity_types"].items()
        ]
        # 按数量排序
        entity_types.sort(key=lambda x: x["count"], reverse=True)

        return {
            "totalEntities": entity_stats["total_entities"],
            "totalRelations": 0,  # TODO: 需要从数据中统计关系
            "entityTypes": entity_types[:10],  # 最多显示10种类型
        }

    def get_refactor_stats(self) -> Dict:
        """获取重构问题统计"""
        problem_stats = self.refactor_store.get_problem_stats()
        total = problem_stats["total_problems"]

        by_severity = [
            {
                "severity": "high",
                "count": problem_stats["by_severity"]["high"],
                "percentage": round(problem_stats["by_severity"]["high"] / max(total, 1) * 100, 1)
            },
            {
                "severity": "medium",
                "count": problem_stats["by_severity"]["medium"],
                "percentage": round(problem_stats["by_severity"]["medium"] / max(total, 1) * 100, 1)
            },
            {
                "severity": "low",
                "count": problem_stats["by_severity"]["low"],
                "percentage": round(problem_stats["by_severity"]["low"] / max(total, 1) * 100, 1)
            },
        ]

        return {
            "totalProblems": total,
            "bySeverity": by_severity,
            "estimatedHours": problem_stats["estimated_hours"],
        }

    def get_recent_tasks(self, limit: int = 5) -> Dict:
        """获取最近任务"""
        # 获取最近的知识提取项目
        projects = self.knowledge_store.get_all_projects()
        projects.sort(key=lambda x: x.get("modified_at", ""), reverse=True)
        extract_tasks = [
            {
                "taskId": f"ext-{p['project_name']}",
                "projectName": p["project_name"],
                "status": "completed",
                "progress": 100,
                "entityCount": p["entity_count"],
                "createdAt": p["modified_at"],
            }
            for p in projects[:limit]
        ]

        # 获取最近的重构报告
        reports = self.refactor_store.get_all_reports()
        reports.sort(key=lambda x: x.get("generated_at", ""), reverse=True)
        refactor_tasks = [
            {
                "taskId": f"ref-{r['project_name']}",
                "projectName": r["project_name"],
                "status": "completed" if r["status"] == "approved" else "analyzing",
                "problemCount": r["total_problems"],
                "highPriorityCount": r["high_severity"],
                "estimatedHours": r["estimated_hours"],
                "createdAt": r["generated_at"],
            }
            for r in reports[:limit]
        ]

        return {
            "extractTasks": extract_tasks,
            "refactorTasks": refactor_tasks,
        }


# 全局实例
knowledge_store = KnowledgeDataStore()
refactor_store = RefactorDataStore()
session_store = SessionDataStore()
dashboard_store = DashboardDataStore()
