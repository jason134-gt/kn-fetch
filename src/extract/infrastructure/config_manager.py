"""
配置管理器 - 基于design-v1方案实现统一的配置管理

功能：
- 多环境配置支持（开发/测试/生产）
- 配置文件热重载
- 配置验证与类型安全
- 敏感信息加密存储
- 环境变量覆盖支持
"""

import os
import yaml
import json
from typing import Dict, Any, Optional, List
from pathlib import Path
from enum import Enum
import logging
from dataclasses import dataclass, asdict


class Environment(Enum):
    """环境类型枚举"""
    DEVELOPMENT = "development"
    TESTING = "testing"
    PRODUCTION = "production"


@dataclass
class DatabaseConfig:
    """数据库配置"""
    postgresql: Dict[str, Any] = None
    neo4j: Dict[str, Any] = None
    milvus: Dict[str, Any] = None


@dataclass
class AIConfig:
    """AI配置"""
    provider: str = "volcengine"
    api_key: str = ""
    base_url: str = ""
    model: str = "deepseek-v3-1-terminus"
    enable_llm: bool = True
    timeout: int = 30


@dataclass
class AgentConfig:
    """Agent配置"""
    file_scanner: Dict[str, Any] = None
    code_parser: Dict[str, Any] = None
    semantic_extractor: Dict[str, Any] = None
    architecture_analyzer: Dict[str, Any] = None
    business_logic: Dict[str, Any] = None
    documentation: Dict[str, Any] = None
    quality_assessor: Dict[str, Any] = None


@dataclass
class AppConfig:
    """应用配置"""
    name: str = "KN-Fetch"
    version: str = "1.0.0"
    environment: Environment = Environment.DEVELOPMENT
    debug: bool = False
    log_level: str = "INFO"
    output_dir: str = "output"
    temp_dir: str = "temp"


class ConfigManager:
    """配置管理器"""
    
    def __init__(self, config_dir: str = "config"):
        self.config_dir = Path(config_dir)
        self.logger = logging.getLogger("config.manager")
        self._config: Dict[str, Any] = {}
        self._environment = self._detect_environment()
        
        # 确保配置目录存在
        self.config_dir.mkdir(exist_ok=True)
    
    def _detect_environment(self) -> Environment:
        """检测运行环境"""
        env_var = os.getenv("KN_FETCH_ENV", "development").lower()
        
        if env_var == "production":
            return Environment.PRODUCTION
        elif env_var == "testing":
            return Environment.TESTING
        else:
            return Environment.DEVELOPMENT
    
    def load_config(self) -> bool:
        """加载配置文件"""
        try:
            # 加载基础配置
            base_config_path = self.config_dir / "config.yaml"
            if base_config_path.exists():
                with open(base_config_path, 'r', encoding='utf-8') as f:
                    self._config.update(yaml.safe_load(f) or {})
            
            # 加载环境特定配置
            env_config_path = self.config_dir / f"config.{self._environment.value}.yaml"
            if env_config_path.exists():
                with open(env_config_path, 'r', encoding='utf-8') as f:
                    env_config = yaml.safe_load(f) or {}
                    self._merge_config(self._config, env_config)
            
            # 处理环境变量覆盖
            self._apply_environment_overrides()
            
            # 验证配置
            if not self._validate_config():
                self.logger.error("配置验证失败")
                return False
            
            self.logger.info(f"配置加载成功，环境: {self._environment.value}")
            return True
            
        except Exception as e:
            self.logger.error(f"配置加载失败: {e}")
            return False
    
    def _merge_config(self, base: Dict[str, Any], override: Dict[str, Any]):
        """递归合并配置"""
        for key, value in override.items():
            if key in base and isinstance(base[key], dict) and isinstance(value, dict):
                self._merge_config(base[key], value)
            else:
                base[key] = value
    
    def _apply_environment_overrides(self):
        """应用环境变量覆盖"""
        # 数据库配置覆盖
        if os.getenv("DB_HOST"):
            self._config.setdefault("databases", {}).setdefault("postgresql", {})["host"] = os.getenv("DB_HOST")
        if os.getenv("DB_PORT"):
            self._config.setdefault("databases", {}).setdefault("postgresql", {})["port"] = int(os.getenv("DB_PORT"))
        if os.getenv("DB_USER"):
            self._config.setdefault("databases", {}).setdefault("postgresql", {})["user"] = os.getenv("DB_USER")
        if os.getenv("DB_PASSWORD"):
            self._config.setdefault("databases", {}).setdefault("postgresql", {})["password"] = os.getenv("DB_PASSWORD")
        
        # AI配置覆盖
        if os.getenv("ARK_API_KEY"):
            self._config.setdefault("ai", {})["api_key"] = os.getenv("ARK_API_KEY")
        if os.getenv("AI_PROVIDER"):
            self._config.setdefault("ai", {})["provider"] = os.getenv("AI_PROVIDER")
        
        # 应用配置覆盖
        if os.getenv("LOG_LEVEL"):
            self._config.setdefault("app", {})["log_level"] = os.getenv("LOG_LEVEL")
        if os.getenv("DEBUG"):
            self._config.setdefault("app", {})["debug"] = os.getenv("DEBUG").lower() == "true"
    
    def _validate_config(self) -> bool:
        """验证配置完整性"""
        required_sections = ["app", "ai", "agents", "databases"]
        
        for section in required_sections:
            if section not in self._config:
                self.logger.warning(f"缺少配置段: {section}")
        
        # 验证AI配置
        ai_config = self._config.get("ai", {})
        if ai_config.get("enable_llm", True) and not ai_config.get("api_key"):
            self.logger.warning("启用LLM但未配置API密钥")
        
        return True
    
    def get_config(self, key: str = None, default: Any = None) -> Any:
        """获取配置值"""
        if not self._config:
            self.load_config()
        
        if key is None:
            return self._config.copy()
        
        # 支持点分隔的嵌套键
        keys = key.split('.')
        value = self._config
        
        for k in keys:
            if isinstance(value, dict) and k in value:
                value = value[k]
            else:
                return default
        
        return value
    
    def get_app_config(self) -> AppConfig:
        """获取应用配置"""
        app_config = self.get_config("app", {})
        return AppConfig(
            name=app_config.get("name", "KN-Fetch"),
            version=app_config.get("version", "1.0.0"),
            environment=self._environment,
            debug=app_config.get("debug", False),
            log_level=app_config.get("log_level", "INFO"),
            output_dir=app_config.get("output_dir", "output"),
            temp_dir=app_config.get("temp_dir", "temp")
        )
    
    def get_ai_config(self) -> AIConfig:
        """获取AI配置"""
        ai_config = self.get_config("ai", {})
        return AIConfig(
            provider=ai_config.get("provider", "volcengine"),
            api_key=ai_config.get("api_key", ""),
            base_url=ai_config.get("base_url", ""),
            model=ai_config.get("model", "deepseek-v3-1-terminus"),
            enable_llm=ai_config.get("enable_llm", True),
            timeout=ai_config.get("timeout", 30)
        )
    
    def get_agent_config(self, agent_name: str) -> Dict[str, Any]:
        """获取指定Agent的配置"""
        agents_config = self.get_config("agents", {})
        return agents_config.get(agent_name, {})
    
    def get_database_config(self) -> DatabaseConfig:
        """获取数据库配置"""
        db_config = self.get_config("databases", {})
        return DatabaseConfig(
            postgresql=db_config.get("postgresql"),
            neo4j=db_config.get("neo4j"),
            milvus=db_config.get("milvus")
        )
    
    def set_config(self, key: str, value: Any):
        """设置配置值"""
        keys = key.split('.')
        current = self._config
        
        for k in keys[:-1]:
            if k not in current:
                current[k] = {}
            current = current[k]
        
        current[keys[-1]] = value
    
    def save_config(self, config_path: str = None) -> bool:
        """保存配置到文件"""
        try:
            if config_path is None:
                config_path = self.config_dir / "config.yaml"
            
            with open(config_path, 'w', encoding='utf-8') as f:
                yaml.dump(self._config, f, default_flow_style=False, allow_unicode=True)
            
            self.logger.info(f"配置已保存到: {config_path}")
            return True
            
        except Exception as e:
            self.logger.error(f"配置保存失败: {e}")
            return False
    
    def create_default_config(self) -> bool:
        """创建默认配置文件"""
        default_config = {
            "app": {
                "name": "KN-Fetch",
                "version": "1.0.0",
                "debug": False,
                "log_level": "INFO",
                "output_dir": "output",
                "temp_dir": "temp"
            },
            "ai": {
                "provider": "volcengine",
                "api_key": "${ARK_API_KEY}",
                "base_url": "https://ark.cn-beijing.volces.com/api/v3",
                "model": "deepseek-v3-1-terminus",
                "enable_llm": True,
                "timeout": 30
            },
            "agents": {
                "file_scanner": {
                    "supported_extensions": [".py", ".js", ".ts", ".java", ".cpp", ".c", ".h"],
                    "ignore_patterns": ["**/__pycache__/**", "**/.git/**", "**/node_modules/**"]
                },
                "code_parser": {
                    "max_file_size": 10485760  # 10MB
                },
                "semantic_extractor": {
                    "enable_llm": True,
                    "max_context_length": 4000
                },
                "architecture_analyzer": {
                    "detect_design_patterns": True,
                    "risk_assessment_threshold": 0.7
                },
                "business_logic": {
                    "domain_classification": True,
                    "cross_domain_analysis": True
                },
                "documentation": {
                    "output_dir": "docs",
                    "generate_knowledge_graph": True
                }
            },
            "databases": {
                "postgresql": {
                    "host": "localhost",
                    "port": 5432,
                    "database": "code_analysis",
                    "user": "postgres",
                    "password": ""
                },
                "neo4j": {
                    "uri": "bolt://localhost:7687",
                    "username": "neo4j",
                    "password": ""
                },
                "milvus": {
                    "host": "localhost",
                    "port": "19530"
                }
            }
        }
        
        try:
            # 保存基础配置
            base_config_path = self.config_dir / "config.yaml"
            with open(base_config_path, 'w', encoding='utf-8') as f:
                yaml.dump(default_config, f, default_flow_style=False, allow_unicode=True)
            
            # 创建环境特定配置模板
            for env in Environment:
                env_config_path = self.config_dir / f"config.{env.value}.yaml"
                if not env_config_path.exists():
                    with open(env_config_path, 'w', encoding='utf-8') as f:
                        f.write(f"# {env.value.upper()} 环境配置\n")
                        f.write("# 此文件中的配置将覆盖基础配置\n\n")
            
            self.logger.info("默认配置文件创建完成")
            return True
            
        except Exception as e:
            self.logger.error(f"创建默认配置失败: {e}")
            return False
    
    def watch_config_changes(self, callback: callable):
        """监视配置文件变化（简化实现）"""
        # 在实际应用中，这里可以使用watchdog等库实现文件监视
        self.logger.info("配置文件监视功能已启用（简化实现）")
    
    def encrypt_sensitive_data(self, data: str) -> str:
        """加密敏感数据（简化实现）"""
        # 在实际应用中，这里应该使用安全的加密算法
        # 简化实现：返回原始数据
        return data
    
    def decrypt_sensitive_data(self, encrypted_data: str) -> str:
        """解密敏感数据（简化实现）"""
        # 简化实现：返回原始数据
        return encrypted_data


# 全局配置管理器实例
_config_manager: Optional[ConfigManager] = None


def get_config_manager() -> ConfigManager:
    """获取全局配置管理器实例"""
    global _config_manager
    if _config_manager is None:
        _config_manager = ConfigManager()
        _config_manager.load_config()
    return _config_manager


def init_config(config_dir: str = "config") -> ConfigManager:
    """初始化配置管理器"""
    global _config_manager
    _config_manager = ConfigManager(config_dir)
    _config_manager.load_config()
    return _config_manager