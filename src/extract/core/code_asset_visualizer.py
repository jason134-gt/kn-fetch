"""
代码资产全景图生成Agent
实现：模块依赖图、调用链路图、技术债务热力图、代码复杂度分布图
"""
import json
import logging
from typing import List, Dict, Any, Optional, Tuple
from pathlib import Path
from collections import defaultdict, Counter
from datetime import datetime

from ..gitnexus.models import (
    KnowledgeGraph, CodeEntity, EntityType, Relationship, RelationshipType
)

logger = logging.getLogger(__name__)


class CodeAssetVisualizer:
    """代码资产全景图生成器"""
    
    def __init__(self, graph: KnowledgeGraph, config: Dict[str, Any] = None):
        self.graph = graph
        self.config = config or {}
        
        # 构建索引
        self._build_indexes()
    
    def _build_indexes(self):
        """构建加速索引"""
        self.caller_to_callees: Dict[str, List[str]] = defaultdict(list)
        self.callee_to_callers: Dict[str, List[str]] = defaultdict(list)
        self.file_entities: Dict[str, List[CodeEntity]] = defaultdict(list)
        self.module_entities: Dict[str, List[CodeEntity]] = defaultdict(list)
        
        for entity in self.graph.entities.values():
            self.file_entities[entity.file_path].append(entity)
            if entity.module_path:
                self.module_entities[entity.module_path].append(entity)
        
        for rel in self.graph.relationships:
            if rel.relationship_type == RelationshipType.CALLS:
                self.caller_to_callees[rel.source_id].append(rel.target_id)
                self.callee_to_callers[rel.target_id].append(rel.source_id)
    
    def generate_full_visualization(self, output_dir: str = "output/visualization") -> Dict[str, str]:
        """生成完整的可视化资产"""
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        results = {}
        
        # 1. 模块依赖图
        print("生成模块依赖图...")
        results["module_dependency"] = self._generate_module_dependency_graph(output_path)
        
        # 2. 调用链路图
        print("生成调用链路图...")
        results["call_chain"] = self._generate_call_chain_graph(output_path)
        
        # 3. 技术债务热力图
        print("生成技术债务热力图...")
        results["debt_heatmap"] = self._generate_debt_heatmap(output_path)
        
        # 4. 代码复杂度分布图
        print("生成代码复杂度分布图...")
        results["complexity_distribution"] = self._generate_complexity_distribution(output_path)
        
        # 5. 代码资产全景报告
        print("生成代码资产全景报告...")
        results["panorama_report"] = self._generate_panorama_report(output_path)
        
        return results
    
    def _generate_module_dependency_graph(self, output_path: Path) -> str:
        """生成模块依赖图（Mermaid格式）"""
        # 计算模块间依赖
        module_deps: Dict[Tuple[str, str], int] = defaultdict(int)
        
        for rel in self.graph.relationships:
            if rel.relationship_type in [RelationshipType.CALLS, RelationshipType.DEPENDS_ON, RelationshipType.IMPORTS]:
                source = self.graph.entities.get(rel.source_id)
                target = self.graph.entities.get(rel.target_id)
                
                if source and target:
                    source_module = source.module_path or self._extract_module_from_path(source.file_path)
                    target_module = target.module_path or self._extract_module_from_path(target.file_path)
                    
                    if source_module and target_module and source_module != target_module:
                        module_deps[(source_module, target_module)] += 1
        
        # 生成Mermaid图
        lines = ["graph TD"]
        
        # 模块节点
        modules = set()
        for (src, tgt), count in module_deps.items():
            modules.add(src)
            modules.add(tgt)
        
        for module in sorted(modules):
            node_id = self._sanitize_node_id(module)
            node_label = module.split('/')[-1] if '/' in module else module
            lines.append(f'    {node_id}["{node_label}"]')
        
        # 依赖边
        for (src, tgt), count in sorted(module_deps.items(), key=lambda x: x[1], reverse=True)[:50]:
            src_id = self._sanitize_node_id(src)
            tgt_id = self._sanitize_node_id(tgt)
            # 根据调用次数设置线条粗细
            style = '---' if count < 5 else ('===' if count < 15 else '-.-')
            lines.append(f'    {src_id} {style}|{count}次调用| {tgt_id}')
        
        # 添加样式
        lines.extend([
            "",
            "    %% 样式定义",
            "    classDef core fill:#ff6b6b,stroke:#c92a2a",
            "    classDef important fill:#ffd43b,stroke:#fab005", 
            "    classDef normal fill:#69db7c,stroke:#37b24d",
            "    classDef edge fill:#748ffc,stroke:#4c6ef5"
        ])
        
        mermaid_content = "\n".join(lines)
        
        # 保存文件
        file_path = output_path / "module_dependency.mmd"
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(mermaid_content)
        
        # 同时生成HTML版本
        html_content = self._wrap_mermaid_html(mermaid_content, "模块依赖图")
        html_path = output_path / "module_dependency.html"
        with open(html_path, "w", encoding="utf-8") as f:
            f.write(html_content)
        
        return str(file_path)
    
    def _generate_call_chain_graph(self, output_path: Path) -> str:
        """生成调用链路图"""
        # 识别入口点
        entry_points = self._identify_entry_points()
        
        # 为每个入口点生成调用链
        chains_data = []
        
        for entry in entry_points[:10]:  # 限制入口点数量
            chain = self._trace_call_chain(entry.id, max_depth=6)
            if len(chain) > 1:
                chains_data.append({
                    "entry": entry.name,
                    "entry_id": entry.id,
                    "chain": chain,
                    "depth": len(chain)
                })
        
        # 生成Mermaid时序图
        lines = ["sequenceDiagram"]
        lines.append("    autonumber")
        
        # 参与者
        participants = set()
        for chain_data in chains_data[:3]:  # 只展示前3个主要调用链
            for entity_id in chain_data["chain"][:10]:
                entity = self.graph.entities.get(entity_id)
                if entity:
                    participant_name = self._sanitize_name(entity.name)
                    participants.add(participant_name)
        
        for p in sorted(participants)[:15]:
            lines.append(f"    participant {p}")
        
        # 调用关系
        for chain_data in chains_data[:3]:
            prev_participant = None
            for entity_id in chain_data["chain"][:10]:
                entity = self.graph.entities.get(entity_id)
                if entity:
                    participant = self._sanitize_name(entity.name)
                    if prev_participant and participant != prev_participant:
                        lines.append(f"    {prev_participant}->>+{participant}: 调用")
                    prev_participant = participant
        
        mermaid_content = "\n".join(lines)
        
        # 保存文件
        file_path = output_path / "call_chain.mmd"
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(mermaid_content)
        
        # 生成JSON数据（供前端使用）
        json_path = output_path / "call_chain_data.json"
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(chains_data, f, ensure_ascii=False, indent=2)
        
        return str(file_path)
    
    def _generate_debt_heatmap(self, output_path: Path) -> str:
        """生成技术债务热力图"""
        # 按模块统计技术债务
        module_debts: Dict[str, Dict[str, Any]] = defaultdict(lambda: {
            "complexity_issues": 0,
            "missing_docs": 0,
            "dead_code": 0,
            "high_coupling": 0,
            "total_issues": 0,
            "entities": []
        })
        
        for entity in self.graph.entities.values():
            module = entity.module_path or self._extract_module_from_path(entity.file_path)
            
            issues = 0
            
            # 高复杂度
            if entity.complexity and entity.complexity > 15:
                module_debts[module]["complexity_issues"] += 1
                issues += 1
            
            # 缺少文档
            if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD, EntityType.CLASS]:
                if not entity.docstring:
                    module_debts[module]["missing_docs"] += 1
                    issues += 1
            
            # 死代码（无调用者且非入口点）
            callers = self.callee_to_callers.get(entity.id, [])
            if not callers and not self._is_entry_point(entity):
                if entity.entity_type in [EntityType.FUNCTION, EntityType.METHOD]:
                    module_debts[module]["dead_code"] += 1
                    issues += 1
            
            # 高耦合
            callees = self.caller_to_callees.get(entity.id, [])
            if len(callees) > 10:
                module_debts[module]["high_coupling"] += 1
                issues += 1
            
            if issues > 0:
                module_debts[module]["entities"].append({
                    "name": entity.name,
                    "issues": issues
                })
            
            module_debts[module]["total_issues"] += issues
        
        # 生成热力图数据
        heatmap_data = []
        for module, data in sorted(module_debts.items(), key=lambda x: x[1]["total_issues"], reverse=True):
            heatmap_data.append({
                "module": module,
                **data,
                "heat_score": data["total_issues"] * 10  # 热度分数
            })
        
        # 保存JSON
        json_path = output_path / "debt_heatmap.json"
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(heatmap_data, f, ensure_ascii=False, indent=2)
        
        # 生成HTML可视化
        html_content = self._generate_heatmap_html(heatmap_data)
        html_path = output_path / "debt_heatmap.html"
        with open(html_path, "w", encoding="utf-8") as f:
            f.write(html_content)
        
        return str(json_path)
    
    def _generate_complexity_distribution(self, output_path: Path) -> str:
        """生成代码复杂度分布图"""
        # 收集复杂度数据
        complexity_data = []
        
        for entity in self.graph.entities.values():
            if entity.complexity is not None:
                complexity_data.append({
                    "name": entity.name,
                    "file": entity.file_path,
                    "complexity": entity.complexity,
                    "loc": entity.lines_of_code,
                    "type": entity.entity_type.value
                })
        
        # 按复杂度分组统计
        complexity_ranges = {
            "简单 (1-5)": 0,
            "中等 (6-10)": 0,
            "复杂 (11-20)": 0,
            "非常复杂 (21-50)": 0,
            "极高 (>50)": 0
        }
        
        for data in complexity_data:
            c = data["complexity"]
            if c <= 5:
                complexity_ranges["简单 (1-5)"] += 1
            elif c <= 10:
                complexity_ranges["中等 (6-10)"] += 1
            elif c <= 20:
                complexity_ranges["复杂 (11-20)"] += 1
            elif c <= 50:
                complexity_ranges["非常复杂 (21-50)"] += 1
            else:
                complexity_ranges["极高 (>50)"] += 1
        
        # 生成分布数据
        distribution = {
            "by_range": complexity_ranges,
            "top_complex": sorted(complexity_data, key=lambda x: x["complexity"], reverse=True)[:20],
            "statistics": {
                "total": len(complexity_data),
                "avg_complexity": sum(d["complexity"] for d in complexity_data) / len(complexity_data) if complexity_data else 0,
                "max_complexity": max(d["complexity"] for d in complexity_data) if complexity_data else 0,
                "high_complexity_count": len([d for d in complexity_data if d["complexity"] > 15])
            }
        }
        
        # 保存JSON
        json_path = output_path / "complexity_distribution.json"
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(distribution, f, ensure_ascii=False, indent=2)
        
        # 生成HTML图表
        html_content = self._generate_complexity_chart_html(distribution)
        html_path = output_path / "complexity_distribution.html"
        with open(html_path, "w", encoding="utf-8") as f:
            f.write(html_content)
        
        return str(json_path)
    
    def _generate_panorama_report(self, output_path: Path) -> str:
        """生成代码资产全景报告"""
        # 统计信息
        total_entities = len(self.graph.entities)
        total_relationships = len(self.graph.relationships)
        
        entity_types = Counter(e.entity_type.value for e in self.graph.entities.values())
        relationship_types = Counter(r.relationship_type.value for r in self.graph.relationships)
        
        # 模块统计
        modules = set()
        for entity in self.graph.entities.values():
            if entity.module_path:
                modules.add(entity.module_path)
        
        # 复杂度统计
        complexities = [e.complexity for e in self.graph.entities.values() if e.complexity]
        avg_complexity = sum(complexities) / len(complexities) if complexities else 0
        
        # 文档覆盖率
        documented = len([e for e in self.graph.entities.values() if e.docstring])
        doc_coverage = documented / total_entities * 100 if total_entities > 0 else 0
        
        # 生成报告
        report = f"""# 代码资产全景报告

生成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

## 一、项目概览

| 指标 | 数值 |
|------|------|
| 总实体数 | {total_entities} |
| 总关系数 | {total_relationships} |
| 模块数量 | {len(modules)} |
| 平均复杂度 | {avg_complexity:.1f} |
| 文档覆盖率 | {doc_coverage:.1f}% |

## 二、实体类型分布

| 类型 | 数量 | 占比 |
|------|------|------|
"""
        
        for etype, count in entity_types.most_common():
            percentage = count / total_entities * 100 if total_entities > 0 else 0
            report += f"| {etype} | {count} | {percentage:.1f}% |\n"
        
        report += f"""
## 三、关系类型分布

| 类型 | 数量 | 占比 |
|------|------|------|
"""
        
        for rtype, count in relationship_types.most_common():
            percentage = count / total_relationships * 100 if total_relationships > 0 else 0
            report += f"| {rtype} | {count} | {percentage:.1f}% |\n"
        
        # 热点模块
        report += """
## 四、热点模块

按实体数量排序的Top模块：

| 模块 | 实体数 | 复杂度均值 |
|------|--------|-----------|
"""
        
        module_stats = []
        for module_path, entities in self.module_entities.items():
            complexities = [e.complexity for e in entities if e.complexity]
            avg_c = sum(complexities) / len(complexities) if complexities else 0
            module_stats.append((module_path, len(entities), avg_c))
        
        for module, count, avg_c in sorted(module_stats, key=lambda x: x[1], reverse=True)[:10]:
            report += f"| {module} | {count} | {avg_c:.1f} |\n"
        
        # 高复杂度实体
        report += """
## 五、高复杂度实体

复杂度超过15的实体（需重点关注）：

| 实体名称 | 文件 | 复杂度 | 代码行数 |
|---------|------|--------|---------|
"""
        
        high_complexity = sorted(
            [e for e in self.graph.entities.values() if e.complexity and e.complexity > 15],
            key=lambda x: x.complexity,
            reverse=True
        )[:20]
        
        for entity in high_complexity:
            report += f"| {entity.name} | {entity.file_path} | {entity.complexity} | {entity.lines_of_code} |\n"
        
        # 可视化链接
        report += """
## 六、可视化资产

- [模块依赖图](./module_dependency.html)
- [调用链路图](./call_chain_data.json)
- [技术债务热力图](./debt_heatmap.html)
- [代码复杂度分布](./complexity_distribution.html)

---
*此报告由代码资产全景图生成Agent自动生成*
"""
        
        # 保存报告
        report_path = output_path / "panorama_report.md"
        with open(report_path, "w", encoding="utf-8") as f:
            f.write(report)
        
        return str(report_path)
    
    def _identify_entry_points(self) -> List[CodeEntity]:
        """识别入口点"""
        entry_points = []
        
        for entity in self.graph.entities.values():
            if self._is_entry_point(entity):
                entry_points.append(entity)
        
        # 按被调用次数排序
        entry_points.sort(key=lambda e: len(self.callee_to_callers.get(e.id, [])), reverse=True)
        return entry_points
    
    def _is_entry_point(self, entity: CodeEntity) -> bool:
        """判断是否为入口点"""
        # 检查装饰器
        for dec in entity.decorators:
            dec_lower = dec.lower()
            if any(kw in dec_lower for kw in ["route", "api", "endpoint", "get", "post", "put", "delete", "app.route", "blueprint"]):
                return True
        
        # 检查名称
        name_lower = entity.name.lower()
        if name_lower in ["main", "run", "start", "execute", "handler", "process_request"]:
            return True
        
        # 检查是否是Controller/Handler
        file_lower = entity.file_path.lower()
        if any(p in file_lower for p in ['controller', 'handler', 'api', 'endpoint', 'route', 'view']):
            if entity.entity_type in [EntityType.CLASS, EntityType.FUNCTION, EntityType.METHOD]:
                return True
        
        return False
    
    def _trace_call_chain(self, entity_id: str, max_depth: int) -> List[str]:
        """追踪调用链"""
        chain = [entity_id]
        visited = {entity_id}
        
        def traverse(eid: str, depth: int):
            if depth <= 0:
                return
            callees = self.caller_to_callees.get(eid, [])
            for cid in callees[:5]:  # 限制分支
                if cid not in visited:
                    visited.add(cid)
                    chain.append(cid)
                    traverse(cid, depth - 1)
        
        traverse(entity_id, max_depth)
        return chain
    
    def _extract_module_from_path(self, file_path: str) -> str:
        """从文件路径提取模块名"""
        parts = file_path.replace('\\', '/').split('/')
        
        # 找到src目录后的第一级目录
        if 'src' in parts:
            idx = parts.index('src')
            if idx + 1 < len(parts):
                return parts[idx + 1]
        
        # 使用倒数第二级目录
        if len(parts) >= 2:
            return parts[-2]
        
        return parts[0] if parts else "unknown"
    
    def _sanitize_node_id(self, name: str) -> str:
        """生成合法的节点ID"""
        return name.replace('/', '_').replace('-', '_').replace('.', '_').replace(' ', '_')
    
    def _sanitize_name(self, name: str) -> str:
        """生成合法的参与者名称"""
        return ''.join(c if c.isalnum() else '_' for c in name)
    
    def _wrap_mermaid_html(self, mermaid_content: str, title: str) -> str:
        """将Mermaid图包装为HTML"""
        return f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>{title}</title>
    <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }}
        h1 {{ color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }}
        .mermaid {{ text-align: center; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>{title}</h1>
        <div class="mermaid">
{mermaid_content}
        </div>
    </div>
    <script>
        mermaid.initialize({{ startOnLoad: true, theme: 'default' }});
    </script>
</body>
</html>"""
    
    def _generate_heatmap_html(self, heatmap_data: List[Dict]) -> str:
        """生成热力图HTML"""
        rows = ""
        for item in heatmap_data[:30]:
            heat_level = min(item["heat_score"] / 100, 1)
            r = int(255 * heat_level)
            g = int(100 * (1 - heat_level))
            color = f"rgb({r}, {g}, 50)"
            
            rows += f"""
            <tr style="background-color: {color}; color: white;">
                <td>{item['module']}</td>
                <td>{item['total_issues']}</td>
                <td>{item['complexity_issues']}</td>
                <td>{item['missing_docs']}</td>
                <td>{item['dead_code']}</td>
                <td>{item['high_coupling']}</td>
            </tr>"""
        
        return f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>技术债务热力图</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        table {{ width: 100%; border-collapse: collapse; }}
        th, td {{ padding: 10px; text-align: left; border: 1px solid #ddd; }}
        th {{ background: #333; color: white; }}
    </style>
</head>
<body>
    <h1>技术债务热力图</h1>
    <table>
        <tr>
            <th>模块</th>
            <th>总问题数</th>
            <th>复杂度问题</th>
            <th>缺少文档</th>
            <th>死代码</th>
            <th>高耦合</th>
        </tr>
        {rows}
    </table>
</body>
</html>"""
    
    def _generate_complexity_chart_html(self, distribution: Dict) -> str:
        """生成复杂度分布图表HTML"""
        return f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>代码复杂度分布</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        .container {{ max-width: 800px; margin: 0 auto; }}
        .stats {{ background: #f0f0f0; padding: 15px; border-radius: 8px; margin-bottom: 20px; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>代码复杂度分布</h1>
        <div class="stats">
            <p><strong>统计摘要：</strong></p>
            <p>总实体数: {distribution['statistics']['total']}</p>
            <p>平均复杂度: {distribution['statistics']['avg_complexity']:.2f}</p>
            <p>最大复杂度: {distribution['statistics']['max_complexity']}</p>
            <p>高复杂度实体(>15): {distribution['statistics']['high_complexity_count']}</p>
        </div>
        <canvas id="complexityChart"></canvas>
    </div>
    <script>
        const ctx = document.getElementById('complexityChart').getContext('2d');
        const chart = new Chart(ctx, {{
            type: 'bar',
            data: {{
                labels: {list(distribution['by_range'].keys())},
                datasets: [{{
                    label: '实体数量',
                    data: {list(distribution['by_range'].values())},
                    backgroundColor: ['#4CAF50', '#8BC34A', '#FFC107', '#FF5722', '#F44336']
                }}]
            }},
            options: {{
                responsive: true,
                plugins: {{
                    title: {{ display: true, text: '复杂度分布' }}
                }}
            }}
        }});
    </script>
</body>
</html>"""
