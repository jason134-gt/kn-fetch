import json
from typing import Dict, Any, List, Tuple
from pathlib import Path

from .ai_client import AIClient
from ..gitnexus import KnowledgeGraph, AnalysisResult

class SelfValidator:
    """自验证引擎，验证知识提取的准确性和完整性"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.ai_client = AIClient(config_path)
        self.config = self._load_config(config_path)
        self.validation_config = self.config.get("validation", {})
        self.threshold = self.validation_config.get("accuracy_threshold", 0.8)
        
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """加载配置文件"""
        import yaml
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                return yaml.safe_load(f)
        except Exception as e:
            raise Exception(f"加载配置文件失败: {str(e)}")
    
    def validate_knowledge_graph(
        self,
        graph: KnowledgeGraph,
        source_directory: str
    ) -> Dict[str, Any]:
        """验证整个知识图谱的准确性"""
        print("开始验证知识图谱...")
        
        validation_results = {
            "overall_accuracy": 0.0,
            "total_entities_validated": 0,
            "valid_entities": 0,
            "invalid_entities": 0,
            "entity_errors": [],
            "overall_status": "pending"
        }
        
        # 抽样验证，避免验证所有实体消耗过多tokens
        sample_size = min(100, len(graph.entities))  # 最多验证100个实体
        entities_to_validate = list(graph.entities.values())[:sample_size]
        
        print(f"抽样验证 {len(entities_to_validate)} 个实体...")
        
        for entity in entities_to_validate:
            # 获取原始文件内容
            file_path = Path(source_directory) / entity.file_path
            if not file_path.exists():
                continue
            
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    source_content = f.read()
                
                validation_result = self.validate_entity(entity, source_content)
                validation_results["total_entities_validated"] += 1
                
                if validation_result["is_valid"]:
                    validation_results["valid_entities"] += 1
                else:
                    validation_results["invalid_entities"] += 1
                    validation_results["entity_errors"].append({
                        "entity_id": entity.id,
                        "entity_name": entity.name,
                        "file_path": entity.file_path,
                        "errors": validation_result["errors"]
                    })
                
            except Exception as e:
                print(f"验证实体 {entity.name} 失败: {str(e)}")
                continue
        
        # 计算整体准确率
        if validation_results["total_entities_validated"] > 0:
            validation_results["overall_accuracy"] = (
                validation_results["valid_entities"] / validation_results["total_entities_validated"]
            )
        
        # 确定整体状态
        if validation_results["overall_accuracy"] >= self.threshold:
            validation_results["overall_status"] = "passed"
        else:
            validation_results["overall_status"] = "failed"
        
        print(f"验证完成，整体准确率: {validation_results['overall_accuracy']:.2%}, 状态: {validation_results['overall_status']}")
        
        return validation_results
    
    def validate_entity(
        self,
        entity: Dict[str, Any],
        source_content: str
    ) -> Dict[str, Any]:
        """验证单个实体的准确性"""
        validation_result = self.ai_client.validate_knowledge(
            entity.model_dump() if hasattr(entity, 'model_dump') else entity,
            source_content
        )
        
        default_result = {
            "is_valid": True,
            "confidence": 1.0,
            "errors": [],
            "suggestions": []
        }
        
        return {**default_result, **validation_result}
    
    def validate_analysis_result(
        self,
        result: AnalysisResult,
        file_content: str
    ) -> Dict[str, Any]:
        """验证单个文件的分析结果"""
        validation_results = {
            "is_valid": True,
            "accuracy": 1.0,
            "errors": [],
            "missing_entities": [],
            "incorrect_entities": []
        }
        
        for entity in result.entities:
            entity_validation = self.validate_entity(entity, file_content)
            if not entity_validation["is_valid"]:
                validation_results["is_valid"] = False
                validation_results["errors"].extend(entity_validation["errors"])
                validation_results["incorrect_entities"].append({
                    "name": entity.name,
                    "errors": entity_validation["errors"]
                })
        
        return validation_results
    
    def generate_validation_report(
        self,
        validation_results: Dict[str, Any],
        output_path: str
    ) -> str:
        """生成验证报告"""
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        content = "# 知识提取验证报告\n\n"
        content += f"验证时间: {self._get_current_time()}\n"
        content += f"整体准确率: {validation_results['overall_accuracy']:.2%}\n"
        content += f"验证状态: {'✅ 通过' if validation_results['overall_status'] == 'passed' else '❌ 失败'}\n\n"
        
        content += "## 统计信息\n"
        content += f"- 总验证实体数: {validation_results['total_entities_validated']}\n"
        content += f"- 有效实体数: {validation_results['valid_entities']}\n"
        content += f"- 无效实体数: {validation_results['invalid_entities']}\n\n"
        
        if validation_results["invalid_entities"] > 0:
            content += "## 错误详情\n"
            for error in validation_results["entity_errors"]:
                content += f"### {error['entity_name']} ({error['file_path']})\n"
                for err in error["errors"]:
                    content += f"- {err}\n"
                content += "\n"
        
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(content)
        
        return str(output_path)
    
    def _get_current_time(self) -> str:
        """获取当前时间字符串"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
