"""
知识加载器 - 加载知识提取智能体的输出

从以下文件加载知识：
- knowledge_graph.json (知识图谱)
- doc/design/*.md (LLM分析文档)
- doc/business/*.md (业务流程文档)
- doc/architecture/*.md (架构文档)
"""

import json
import logging
import re
from pathlib import Path
from typing import Dict, Any, List, Optional

from .refactoring_task import KnowledgeContext

logger = logging.getLogger(__name__)


class KnowledgeLoader:
    """知识加载器 - 加载知识提取智能体的输出"""
    
    def __init__(self, config: Dict[str, Any] = None):
        self.config = config or {}
        self._entities: Dict[str, Any] = {}
        self._relationships: List[Any] = []
    
    def load(self, doc_dir: str, knowledge_graph_path: str = None) -> KnowledgeContext:
        """
        加载知识上下文
        
        Args:
            doc_dir: 文档目录路径 (output/doc/xxx)
            knowledge_graph_path: 知识图谱文件路径（可选）
            
        Returns:
            KnowledgeContext对象
        """
        doc_path = Path(doc_dir)
        kg_path = Path(knowledge_graph_path) if knowledge_graph_path else None
        
        logger.info(f"加载知识上下文...")
        logger.info(f"  文档目录: {doc_dir}")
        logger.info(f"  知识图谱: {knowledge_graph_path or '未指定'}")
        
        # 加载知识图谱
        entity_dict, relationships = self._load_knowledge_graph(kg_path)
        self._entities = entity_dict
        self._relationships = relationships
        
        # 加载文档
        project_overview = self._load_doc(doc_path / "design" / "project_overview.md")
        business_flow = self._load_doc(doc_path / "business" / "business_flow.md")
        architecture_design = self._load_doc(doc_path / "architecture" / "architecture_design.md")
        feature_modules = self._load_doc(doc_path / "design" / "feature_modules.md")
        api_reference = self._load_doc(doc_path / "design" / "api_reference.md")
        
        # 提取项目信息
        project_info = self._extract_project_info(project_overview)
        
        # 构建上下文
        context = KnowledgeContext(
            project_name=project_info.get("name", "Unknown"),
            project_description=project_info.get("description", ""),
            tech_stack=project_info.get("tech_stack", []),
            entities=entity_dict,
            relationships=relationships,
            project_overview=project_overview,
            business_flow=business_flow,
            architecture_design=architecture_design,
            feature_modules=feature_modules,
            api_reference=api_reference,
            total_entities=len(entity_dict),
            total_relationships=len(relationships),
            total_files=len(set(e.get("file_path") for e in entity_dict.values())) if entity_dict else 0,
            metadata={"source_path": str(kg_path)}
        )
        
        logger.info(f"加载完成: {context.total_entities} 实体, {context.total_relationships} 关系")
        
        return context
    
    def _load_knowledge_graph(self, kg_path: Path) -> tuple:
        """加载知识图谱"""
        if kg_path is None or not kg_path.exists():
            if kg_path:
                logger.warning(f"知识图谱文件不存在: {kg_path}")
            else:
                logger.warning(f"知识图谱路径未指定，从文档目录加载实体")
            return self._load_entities_from_doc_dir(kg_path.parent.parent if kg_path else Path("."))
        
        try:
            with open(kg_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            # 处理不同格式
            if isinstance(data, dict):
                entities = data.get("entities", {})
                relationships = data.get("relationships", [])
                
                # 如果entities是对象形式，转换为字典
                if isinstance(entities, dict):
                    # 检查是否已经是 {id: entity} 格式
                    first_value = next(iter(entities.values()), None) if entities else None
                    if first_value and isinstance(first_value, dict) and "id" in first_value:
                        # 已经是正确格式
                        pass
                    else:
                        # 可能是嵌套结构，尝试提取
                        entities = self._flatten_entities(entities)
                
                return entities, relationships
            
            return {}, []
            
        except Exception as e:
            logger.error(f"加载知识图谱失败: {e}")
            return {}, []
    
    def _load_entities_from_doc_dir(self, doc_dir: Path) -> tuple:
        """从文档目录加载实体（当知识图谱不存在时）"""
        entities = {}
        relationships = []
        entity_id = 0
        
        # 尝试从 entities.json 加载
        entities_json = doc_dir / "knowledge_stock_datacenter" / "entities.json"
        if entities_json.exists():
            try:
                with open(entities_json, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                if isinstance(data, dict):
                    entities = data
                elif isinstance(data, list):
                    for item in data:
                        if isinstance(item, dict) and "id" in item:
                            entities[item["id"]] = item
                logger.info(f"从 entities.json 加载了 {len(entities)} 个实体")
                return entities, relationships
            except Exception as e:
                logger.warning(f"加载 entities.json 失败: {e}")
        
        # 尝试从entities目录的md文件提取
        entities_dir = doc_dir / "doc" / "stock_datacenter" / "entities"
        if entities_dir.exists():
            for md_file in entities_dir.glob("*.md"):
                try:
                    content = md_file.read_text(encoding='utf-8')
                    entity_id += 1
                    
                    # 提取实体名称（从标题）
                    name = md_file.stem
                    for line in content.split('\n')[:5]:
                        if line.startswith('# '):
                            name = line.lstrip('#').strip()
                            break
                    
                    entities[f"entity_{entity_id}"] = {
                        "id": f"entity_{entity_id}",
                        "name": name,
                        "file_path": str(md_file),
                        "entity_type": "module",
                        "docstring": content[:500]
                    }
                except Exception:
                    pass
            
            logger.info(f"从 entities 目录加载了 {len(entities)} 个实体")
        
        return entities, relationships
    
    def _flatten_entities(self, entities: Any) -> Dict[str, Any]:
        """展平实体结构"""
        result = {}
        
        if isinstance(entities, dict):
            for key, value in entities.items():
                if isinstance(value, dict):
                    if "id" in value:
                        result[value["id"]] = value
                    else:
                        # 递归处理
                        result.update(self._flatten_entities(value))
                elif isinstance(value, list):
                    for item in value:
                        if isinstance(item, dict) and "id" in item:
                            result[item["id"]] = item
        
        return result
    
    def _load_doc(self, path: Path) -> str:
        """加载文档内容，移除YAML前置元数据"""
        if not path.exists():
            logger.debug(f"文档不存在: {path}")
            return ""
        
        try:
            content = path.read_text(encoding='utf-8')
            
            # 移除YAML前置元数据
            if content.startswith('---'):
                parts = content.split('---', 2)
                if len(parts) >= 3:
                    return parts[2].strip()
            
            return content
            
        except Exception as e:
            logger.warning(f"加载文档失败 {path}: {e}")
            return ""
    
    def _extract_project_info(self, overview: str) -> Dict[str, Any]:
        """从项目概述提取项目信息"""
        info = {"name": "", "description": "", "tech_stack": []}
        
        if not overview:
            return info
        
        lines = overview.split('\n')
        for i, line in enumerate(lines[:50]):
            # 提取项目名称
            if line.startswith('# ') and not info["name"]:
                info["name"] = line.lstrip('#').strip()
            
            # 提取技术栈
            if '技术栈' in line or 'technology' in line.lower():
                # 尝试从下一行提取
                if i + 1 < len(lines):
                    next_line = lines[i + 1]
                    # 表格格式提取
                    techs = re.findall(r'\*\*([^*]+)\*\*', next_line)
                    if techs:
                        info["tech_stack"].extend(techs)
        
        # 提取描述（第一个段落）
        if '## ' in overview:
            parts = overview.split('## ')
            if len(parts) > 1:
                first_section = parts[1].split('\n')
                for line in first_section:
                    if line.strip() and not line.startswith('#'):
                        info["description"] = line.strip()[:200]
                        break
        
        return info
    
    def get_entity_by_id(self, entity_id: str) -> Optional[Dict[str, Any]]:
        """获取实体"""
        return self._entities.get(entity_id)
    
    def get_entities_by_file(self, file_path: str) -> List[Dict[str, Any]]:
        """获取文件内的所有实体"""
        return [e for e in self._entities.values() 
                if e.get("file_path") == file_path]
    
    def get_entities_by_type(self, entity_type: str) -> List[Dict[str, Any]]:
        """获取指定类型的所有实体"""
        return [e for e in self._entities.values()
                if e.get("entity_type") == entity_type]
    
    def get_related_entities(self, entity_id: str, relation_type: str = None) -> List[Dict[str, Any]]:
        """获取关联实体"""
        related = []
        
        for rel in self._relationships:
            # 过滤关系类型
            if relation_type and rel.get("type") != relation_type:
                continue
            
            if rel.get("source") == entity_id:
                target = self._entities.get(rel.get("target"))
                if target:
                    related.append({
                        "entity": target,
                        "relation": rel,
                        "role": "callee"
                    })
            elif rel.get("target") == entity_id:
                source = self._entities.get(rel.get("source"))
                if source:
                    related.append({
                        "entity": source,
                        "relation": rel,
                        "role": "caller"
                    })
        
        return related
    
    def search_entities(self, keyword: str) -> List[Dict[str, Any]]:
        """搜索实体"""
        keyword_lower = keyword.lower()
        return [e for e in self._entities.values()
                if keyword_lower in e.get("name", "").lower() or
                   keyword_lower in e.get("file_path", "").lower()]
    
    def get_statistics(self) -> Dict[str, Any]:
        """获取统计信息"""
        stats = {
            "total_entities": len(self._entities),
            "total_relationships": len(self._relationships),
            "total_files": len(set(e.get("file_path") for e in self._entities.values())),
            "entity_types": {},
            "file_distribution": {}
        }
        
        # 实体类型分布
        for entity in self._entities.values():
            etype = entity.get("entity_type", "unknown")
            stats["entity_types"][etype] = stats["entity_types"].get(etype, 0) + 1
        
        # 文件分布（Top 10）
        file_counts = {}
        for entity in self._entities.values():
            fpath = entity.get("file_path", "unknown")
            file_counts[fpath] = file_counts.get(fpath, 0) + 1
        
        sorted_files = sorted(file_counts.items(), key=lambda x: x[1], reverse=True)[:10]
        stats["file_distribution"] = dict(sorted_files)
        
        return stats
