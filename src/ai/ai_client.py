import os
import json
from typing import List, Dict, Any, Optional
import openai
from openai import OpenAI
import requests

class AIClient:
    """LLM客户端，支持多种大模型接口"""
    
    def __init__(self, config_path: str = "config/config.yaml"):
        self.config = self._load_config(config_path)
        ai_config = self.config.get("ai", {})
        
        self.provider = ai_config.get("provider", "openai")
        self.api_key = ai_config.get("api_key", os.getenv("OPENAI_API_KEY"))
        self.base_url = ai_config.get("base_url", "https://api.openai.com/v1")
        self.model = ai_config.get("model", "gpt-4o")
        self.max_tokens = ai_config.get("max_tokens", 4000)
        self.temperature = ai_config.get("temperature", 0.1)
        
        if self.provider == "openai":
            self.client = OpenAI(
                api_key=self.api_key,
                base_url=self.base_url
            )
        elif self.provider == "anthropic":
            # 可扩展支持Anthropic Claude
            pass
        elif self.provider == "qwen":
            # 可扩展支持通义千问
            pass
    
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """加载配置文件"""
        import yaml
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                return yaml.safe_load(f)
        except Exception as e:
            raise Exception(f"加载配置文件失败: {str(e)}")
    
    def chat_completion(
        self,
        messages: List[Dict[str, str]],
        system_prompt: Optional[str] = None,
        **kwargs
    ) -> str:
        """调用大模型聊天接口"""
        if system_prompt:
            messages = [{"role": "system", "content": system_prompt}] + messages
        
        try:
            if self.provider == "openai":
                response = self.client.chat.completions.create(
                    model=kwargs.get("model", self.model),
                    messages=messages,
                    max_tokens=kwargs.get("max_tokens", self.max_tokens),
                    temperature=kwargs.get("temperature", self.temperature)
                )
                return response.choices[0].message.content.strip()
            else:
                raise ValueError(f"不支持的模型提供商: {self.provider}")
        except Exception as e:
            print(f"调用大模型失败: {str(e)}")
            return ""
    
    def analyze_code(
        self,
        code_content: str,
        analysis_type: str = "refactor",
        context: Optional[str] = None
    ) -> Dict[str, Any]:
        """分析代码"""
        system_prompt = f"""你是资深软件工程师，擅长{analysis_type}分析。请分析以下代码，返回JSON格式的分析结果，包含问题点、优化建议、风险评估等字段。"""
        
        user_content = f"代码内容:\n```\n{code_content}\n```\n"
        if context:
            user_content += f"\n上下文信息:\n{context}\n"
        
        messages = [{"role": "user", "content": user_content}]
        response = self.chat_completion(messages, system_prompt)
        
        try:
            return json.loads(response)
        except:
            return {"raw_response": response}
    
    def generate_test_cases(
        self,
        code_content: str,
        language: str = "python",
        test_framework: str = "pytest"
    ) -> str:
        """生成测试用例"""
        system_prompt = f"""你是测试专家，擅长为{language}代码生成{test_framework}测试用例。请返回完整的可执行测试代码。"""
        
        user_content = f"需要生成测试用例的代码:\n```\n{code_content}\n```\n"
        
        messages = [{"role": "user", "content": user_content}]
        return self.chat_completion(messages, system_prompt)
    
    def validate_knowledge(
        self,
        knowledge_entry: Dict[str, Any],
        source_content: str
    ) -> Dict[str, Any]:
        """验证知识准确性"""
        system_prompt = """你是知识验证专家，请验证提取的知识是否与原始内容一致。返回JSON格式结果，包含is_valid（布尔值）、confidence（0-1的置信度）、errors（错误列表）、suggestions（修正建议）字段。"""
        
        user_content = f"提取的知识:\n{json.dumps(knowledge_entry, ensure_ascii=False, indent=2)}\n\n原始内容:\n{source_content}\n"
        
        messages = [{"role": "user", "content": user_content}]
        response = self.chat_completion(messages, system_prompt)
        
        try:
            return json.loads(response)
        except:
            return {"raw_response": response}
    
    def optimize_knowledge(
        self,
        knowledge_graph: Dict[str, Any],
        optimization_goal: str = "refactoring_support"
    ) -> Dict[str, Any]:
        """优化知识图谱"""
        system_prompt = f"""你是知识图谱优化专家，目标是{optimization_goal}。请优化给定的知识图谱，返回优化后的JSON格式知识图谱，重点补充重构所需的技术细节、依赖关系、复杂度分析等信息。"""
        
        user_content = f"原始知识图谱:\n{json.dumps(knowledge_graph, ensure_ascii=False, indent=2)}\n"
        
        messages = [{"role": "user", "content": user_content}]
        response = self.chat_completion(messages, system_prompt)
        
        try:
            return json.loads(response)
        except:
            return {"raw_response": response}
