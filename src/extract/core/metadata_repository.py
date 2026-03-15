"""
元数据仓库与API层
实现：结构化元数据存储、查询接口、向量存储集成
"""
import json
import logging
from typing import List, Dict, Any, Optional, Set
from pathlib import Path
from collections import defaultdict
from datetime import datetime
from dataclasses import dataclass, field, asdict
import sqlite3
import threading

from ..gitnexus.models import (
    KnowledgeGraph, CodeEntity, EntityType, Relationship, RelationshipType
)

logger = logging.getLogger(__name__)


@dataclass
class EntityMetadata:
    """实体元数据"""
    id: str
    name: str
    entity_type: str
    file_path: str
    module_path: Optional[str]
    start_line: int
    end_line: int
    lines_of_code: int
    complexity: Optional[int]
    docstring: Optional[str]
    visibility: Optional[str]
    
    # 风险评估
    risk_level: str = "P2"
    risk_score: float = 0.0
    
    # 业务语义
    business_domain: str = "其他"
    business_tags: List[str] = field(default_factory=list)
    business_summary: str = ""
    
    # 调用统计
    caller_count: int = 0
    callee_count: int = 0
    
    # 技术债务
    debt_count: int = 0
    debt_types: List[str] = field(default_factory=list)
    
    # 安全
    vulnerability_count: int = 0
    
    # 语义向量ID（用于向量检索）
    vector_id: Optional[str] = None
    
    # 时间戳
    created_at: str = field(default_factory=lambda: datetime.now().isoformat())
    updated_at: str = field(default_factory=lambda: datetime.now().isoformat())


@dataclass
class ModuleMetadata:
    """模块元数据"""
    path: str
    name: str
    
    # 统计
    entity_count: int = 0
    total_loc: int = 0
    avg_complexity: float = 0.0
    
    # 依赖
    dependencies: List[str] = field(default_factory=list)
    dependents: List[str] = field(default_factory=list)
    
    # 风险
    risk_entities: Dict[str, int] = field(default_factory=dict)
    
    # 技术债务
    total_debts: int = 0


@dataclass
class RelationshipMetadata:
    """关系元数据"""
    id: str
    source_id: str
    target_id: str
    relationship_type: str
    weight: float = 1.0
    confidence: float = 1.0


class MetadataRepository:
    """元数据仓库"""
    
    def __init__(self, db_path: str = "output/metadata.db"):
        self.db_path = db_path
        self._lock = threading.Lock()
        
        # 确保目录存在
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        
        # 初始化数据库
        self._init_database()
        
        # 内存缓存
        self._entity_cache: Dict[str, EntityMetadata] = {}
        self._module_cache: Dict[str, ModuleMetadata] = {}
        self._relationship_cache: List[RelationshipMetadata] = []
    
    def _init_database(self):
        """初始化数据库表"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            
            # 实体表
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS entities (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    file_path TEXT NOT NULL,
                    module_path TEXT,
                    start_line INTEGER,
                    end_line INTEGER,
                    lines_of_code INTEGER,
                    complexity INTEGER,
                    docstring TEXT,
                    visibility TEXT,
                    risk_level TEXT,
                    risk_score REAL,
                    business_domain TEXT,
                    business_tags TEXT,
                    business_summary TEXT,
                    caller_count INTEGER,
                    callee_count INTEGER,
                    debt_count INTEGER,
                    debt_types TEXT,
                    vulnerability_count INTEGER,
                    vector_id TEXT,
                    created_at TEXT,
                    updated_at TEXT
                )
            """)
            
            # 模块表
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS modules (
                    path TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    entity_count INTEGER,
                    total_loc INTEGER,
                    avg_complexity REAL,
                    dependencies TEXT,
                    dependents TEXT,
                    risk_entities TEXT,
                    total_debts INTEGER
                )
            """)
            
            # 关系表
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS relationships (
                    id TEXT PRIMARY KEY,
                    source_id TEXT NOT NULL,
                    target_id TEXT NOT NULL,
                    relationship_type TEXT NOT NULL,
                    weight REAL,
                    confidence REAL,
                    FOREIGN KEY (source_id) REFERENCES entities(id),
                    FOREIGN KEY (target_id) REFERENCES entities(id)
                )
            """)
            
            # 业务域索引
            cursor.execute("""
                CREATE INDEX IF NOT EXISTS idx_business_domain 
                ON entities(business_domain)
            """)
            
            # 风险等级索引
            cursor.execute("""
                CREATE INDEX IF NOT EXISTS idx_risk_level 
                ON entities(risk_level)
            """)
            
            # 模块路径索引
            cursor.execute("""
                CREATE INDEX IF NOT EXISTS idx_module_path 
                ON entities(module_path)
            """)
            
            # 文件路径索引
            cursor.execute("""
                CREATE INDEX IF NOT EXISTS idx_file_path 
                ON entities(file_path)
            """)
            
            conn.commit()
    
    def store_knowledge_graph(
        self, 
        graph: KnowledgeGraph,
        analysis_results: Dict[str, Any] = None
    ) -> int:
        """存储知识图谱到元数据仓库"""
        with self._lock:
            stored_count = 0
            
            # 构建索引
            caller_to_callees = defaultdict(list)
            callee_to_callers = defaultdict(list)
            
            for rel in graph.relationships:
                if rel.relationship_type == RelationshipType.CALLS:
                    caller_to_callees[rel.source_id].append(rel.target_id)
                    callee_to_callers[rel.target_id].append(rel.source_id)
            
            # 存储实体
            for entity in graph.entities.values():
                metadata = self._entity_to_metadata(
                    entity, 
                    callee_to_callers,
                    caller_to_callees,
                    analysis_results
                )
                self._store_entity(metadata)
                self._entity_cache[entity.id] = metadata
                stored_count += 1
            
            # 存储关系
            for rel in graph.relationships:
                rel_meta = RelationshipMetadata(
                    id=rel.id or f"{rel.source_id}_{rel.target_id}_{rel.relationship_type.value}",
                    source_id=rel.source_id,
                    target_id=rel.target_id,
                    relationship_type=rel.relationship_type.value,
                    weight=rel.weight,
                    confidence=rel.confidence
                )
                self._store_relationship(rel_meta)
                self._relationship_cache.append(rel_meta)
            
            # 存储模块
            self._build_module_metadata(graph)
            
            return stored_count
    
    def _entity_to_metadata(
        self,
        entity: CodeEntity,
        callee_to_callers: Dict,
        caller_to_callees: Dict,
        analysis_results: Dict[str, Any] = None
    ) -> EntityMetadata:
        """将实体转换为元数据"""
        metadata = EntityMetadata(
            id=entity.id,
            name=entity.name,
            entity_type=entity.entity_type.value,
            file_path=entity.file_path,
            module_path=entity.module_path,
            start_line=entity.start_line,
            end_line=entity.end_line,
            lines_of_code=entity.lines_of_code,
            complexity=entity.complexity,
            docstring=entity.docstring[:500] if entity.docstring else None,
            visibility=entity.visibility.value if entity.visibility else None,
            caller_count=len(callee_to_callers.get(entity.id, [])),
            callee_count=len(caller_to_callees.get(entity.id, []))
        )
        
        # 添加分析结果
        if analysis_results:
            # 风险评估
            risk_assessment = analysis_results.get("risk_assessment", {})
            for level, assessments in risk_assessment.items():
                for assessment in assessments:
                    if hasattr(assessment, 'entity_id') and assessment.entity_id == entity.id:
                        metadata.risk_level = assessment.risk_level.value if hasattr(assessment.risk_level, 'value') else str(assessment.risk_level)
                        metadata.risk_score = assessment.score
                        break
            
            # 业务语义
            semantics = analysis_results.get("business_semantics", {})
            if entity.id in semantics:
                semantic = semantics[entity.id]
                if hasattr(semantic, 'business_domain'):
                    metadata.business_domain = semantic.business_domain
                    metadata.business_tags = semantic.business_domain_tags[:5]
                    metadata.business_summary = semantic.business_summary[:200] if semantic.business_summary else ""
            
            # 技术债务
            debts = analysis_results.get("technical_debts", [])
            entity_debts = [d for d in debts if hasattr(d, 'entity_id') and d.entity_id == entity.id]
            if entity_debts:
                metadata.debt_count = len(entity_debts)
                metadata.debt_types = list(set(d.debt_type.value for d in entity_debts if hasattr(d, 'debt_type')))
            
            # 安全漏洞
            security_result = analysis_results.get("security_scan")
            if security_result and hasattr(security_result, 'vulnerabilities'):
                entity_vulns = [v for v in security_result.vulnerabilities if v.entity_id == entity.id]
                metadata.vulnerability_count = len(entity_vulns)
        
        return metadata
    
    def _store_entity(self, metadata: EntityMetadata):
        """存储实体到数据库"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute("""
                INSERT OR REPLACE INTO entities VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
            """, (
                metadata.id,
                metadata.name,
                metadata.entity_type,
                metadata.file_path,
                metadata.module_path,
                metadata.start_line,
                metadata.end_line,
                metadata.lines_of_code,
                metadata.complexity,
                metadata.docstring,
                metadata.visibility,
                metadata.risk_level,
                metadata.risk_score,
                metadata.business_domain,
                json.dumps(metadata.business_tags),
                metadata.business_summary,
                metadata.caller_count,
                metadata.callee_count,
                metadata.debt_count,
                json.dumps(metadata.debt_types),
                metadata.vulnerability_count,
                metadata.vector_id,
                metadata.created_at,
                metadata.updated_at
            ))
            conn.commit()
    
    def _store_relationship(self, metadata: RelationshipMetadata):
        """存储关系到数据库"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute("""
                INSERT OR REPLACE INTO relationships VALUES (?, ?, ?, ?, ?, ?)
            """, (
                metadata.id,
                metadata.source_id,
                metadata.target_id,
                metadata.relationship_type,
                metadata.weight,
                metadata.confidence
            ))
            conn.commit()
    
    def _build_module_metadata(self, graph: KnowledgeGraph):
        """构建模块元数据"""
        module_entities = defaultdict(list)
        
        for entity in graph.entities.values():
            module = entity.module_path or self._extract_module_from_path(entity.file_path)
            module_entities[module].append(entity)
        
        for module_path, entities in module_entities.items():
            complexities = [e.complexity for e in entities if e.complexity]
            
            metadata = ModuleMetadata(
                path=module_path,
                name=module_path.split('/')[-1] if '/' in module_path else module_path,
                entity_count=len(entities),
                total_loc=sum(e.lines_of_code for e in entities),
                avg_complexity=sum(complexities) / len(complexities) if complexities else 0.0
            )
            
            self._store_module(metadata)
            self._module_cache[module_path] = metadata
    
    def _extract_module_from_path(self, file_path: str) -> str:
        """从文件路径提取模块名"""
        parts = file_path.replace('\\', '/').split('/')
        if 'src' in parts:
            idx = parts.index('src')
            if idx + 1 < len(parts):
                return parts[idx + 1]
        return parts[-2] if len(parts) >= 2 else parts[0]
    
    def _store_module(self, metadata: ModuleMetadata):
        """存储模块到数据库"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute("""
                INSERT OR REPLACE INTO modules VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                metadata.path,
                metadata.name,
                metadata.entity_count,
                metadata.total_loc,
                metadata.avg_complexity,
                json.dumps(metadata.dependencies),
                json.dumps(metadata.dependents),
                json.dumps(metadata.risk_entities),
                metadata.total_debts
            ))
            conn.commit()
    
    # ==================== 查询API ====================
    
    def get_entity(self, entity_id: str) -> Optional[EntityMetadata]:
        """获取单个实体"""
        # 先查缓存
        if entity_id in self._entity_cache:
            return self._entity_cache[entity_id]
        
        # 查数据库
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM entities WHERE id = ?", (entity_id,))
            row = cursor.fetchone()
            
            if row:
                return self._row_to_entity(row)
        return None
    
    def get_entities_by_type(self, entity_type: str) -> List[EntityMetadata]:
        """按类型获取实体"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM entities WHERE entity_type = ?", (entity_type,))
            return [self._row_to_entity(row) for row in cursor.fetchall()]
    
    def get_entities_by_module(self, module_path: str) -> List[EntityMetadata]:
        """按模块获取实体"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM entities WHERE module_path = ?", (module_path,))
            return [self._row_to_entity(row) for row in cursor.fetchall()]
    
    def get_entities_by_risk_level(self, risk_level: str) -> List[EntityMetadata]:
        """按风险等级获取实体"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM entities WHERE risk_level = ?", (risk_level,))
            return [self._row_to_entity(row) for row in cursor.fetchall()]
    
    def get_entities_by_domain(self, domain: str) -> List[EntityMetadata]:
        """按业务域获取实体"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM entities WHERE business_domain = ?", (domain,))
            return [self._row_to_entity(row) for row in cursor.fetchall()]
    
    def get_high_risk_entities(self, limit: int = 20) -> List[EntityMetadata]:
        """获取高风险实体"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("""
                SELECT * FROM entities 
                WHERE risk_level IN ('P0', 'P1') OR vulnerability_count > 0
                ORDER BY risk_score DESC 
                LIMIT ?
            """, (limit,))
            return [self._row_to_entity(row) for row in cursor.fetchall()]
    
    def get_callers(self, entity_id: str) -> List[EntityMetadata]:
        """获取调用者"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("""
                SELECT e.* FROM entities e
                JOIN relationships r ON e.id = r.source_id
                WHERE r.target_id = ? AND r.relationship_type = 'calls'
            """, (entity_id,))
            return [self._row_to_entity(row) for row in cursor.fetchall()]
    
    def get_callees(self, entity_id: str) -> List[EntityMetadata]:
        """获取被调用者"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("""
                SELECT e.* FROM entities e
                JOIN relationships r ON e.id = r.target_id
                WHERE r.source_id = ? AND r.relationship_type = 'calls'
            """, (entity_id,))
            return [self._row_to_entity(row) for row in cursor.fetchall()]
    
    def search_entities(self, keyword: str) -> List[EntityMetadata]:
        """搜索实体"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()
            cursor.execute("""
                SELECT * FROM entities 
                WHERE name LIKE ? OR business_summary LIKE ? OR docstring LIKE ?
            """, (f'%{keyword}%', f'%{keyword}%', f'%{keyword}%'))
            return [self._row_to_entity(row) for row in cursor.fetchall()]
    
    def get_statistics(self) -> Dict[str, Any]:
        """获取统计信息"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            
            # 实体统计
            cursor.execute("SELECT COUNT(*) FROM entities")
            total_entities = cursor.fetchone()[0]
            
            cursor.execute("SELECT entity_type, COUNT(*) FROM entities GROUP BY entity_type")
            entity_types = dict(cursor.fetchall())
            
            cursor.execute("SELECT risk_level, COUNT(*) FROM entities GROUP BY risk_level")
            risk_levels = dict(cursor.fetchall())
            
            cursor.execute("SELECT business_domain, COUNT(*) FROM entities GROUP BY business_domain")
            domains = dict(cursor.fetchall())
            
            # 模块统计
            cursor.execute("SELECT COUNT(*) FROM modules")
            total_modules = cursor.fetchone()[0]
            
            # 关系统计
            cursor.execute("SELECT COUNT(*) FROM relationships")
            total_relationships = cursor.fetchone()[0]
            
            return {
                "total_entities": total_entities,
                "total_modules": total_modules,
                "total_relationships": total_relationships,
                "entity_types": entity_types,
                "risk_levels": risk_levels,
                "business_domains": domains
            }
    
    def _row_to_entity(self, row) -> EntityMetadata:
        """数据库行转实体元数据"""
        return EntityMetadata(
            id=row['id'],
            name=row['name'],
            entity_type=row['entity_type'],
            file_path=row['file_path'],
            module_path=row['module_path'],
            start_line=row['start_line'],
            end_line=row['end_line'],
            lines_of_code=row['lines_of_code'],
            complexity=row['complexity'],
            docstring=row['docstring'],
            visibility=row['visibility'],
            risk_level=row['risk_level'],
            risk_score=row['risk_score'],
            business_domain=row['business_domain'],
            business_tags=json.loads(row['business_tags']) if row['business_tags'] else [],
            business_summary=row['business_summary'] or '',
            caller_count=row['caller_count'],
            callee_count=row['callee_count'],
            debt_count=row['debt_count'],
            debt_types=json.loads(row['debt_types']) if row['debt_types'] else [],
            vulnerability_count=row['vulnerability_count'],
            vector_id=row['vector_id'],
            created_at=row['created_at'],
            updated_at=row['updated_at']
        )
    
    def export_to_json(self, output_path: str) -> str:
        """导出为JSON"""
        statistics = self.get_statistics()
        
        data = {
            "metadata": {
                "exported_at": datetime.now().isoformat(),
                "version": "1.0"
            },
            "statistics": statistics,
            "entities": [asdict(e) for e in self._entity_cache.values()],
            "modules": [asdict(m) for m in self._module_cache.values()],
            "relationships": [asdict(r) for r in self._relationship_cache]
        }
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2, default=str)
        
        return output_path
