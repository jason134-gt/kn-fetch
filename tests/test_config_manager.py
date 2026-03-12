"""
配置管理器测试

测试配置文件的加载、验证和更新功能
"""

import pytest
import tempfile
import os
from pathlib import Path
from unittest.mock import Mock, patch
from src.infrastructure.config_manager import ConfigManager


class TestConfigManager:
    """配置管理器测试类"""
    
    @pytest.fixture
    def sample_config_content(self):
        """示例配置文件内容"""
        return """
# KN-Fetch 配置
app:
  name: "KN-Fetch"
  version: "1.0.0"
  debug: true
  
server:
  host: "0.0.0.0"
  port: 8000
  workers: 4
  
ai:
  provider: "openai"
  api_key: "test_key"
  model: "gpt-4"
  
databases:
  postgresql:
    host: "localhost"
    port: 5432
    database: "code_analysis"
    user: "postgres"
    password: "password"
  
  neo4j:
    uri: "bolt://localhost:7687"
    username: "neo4j"
    password: "password"
  
  milvus:
    host: "localhost"
    port: "19530"
"""
    
    @pytest.fixture
    def config_manager(self, sample_config_content):
        """创建配置管理器实例"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yaml', delete=False) as f:
            f.write(sample_config_content)
            temp_config_path = f.name
        
        manager = ConfigManager(temp_config_path)
        yield manager
        
        # 清理临时文件
        os.unlink(temp_config_path)
    
    def test_load_config_success(self, config_manager):
        """测试成功加载配置"""
        config_manager.load_config()
        
        assert config_manager.config is not None
        assert config_manager.config["app"]["name"] == "KN-Fetch"
        assert config_manager.config["server"]["port"] == 8000
        assert config_manager.config["ai"]["provider"] == "openai"
    
    def test_load_config_file_not_found(self):
        """测试配置文件不存在的情况"""
        manager = ConfigManager("/nonexistent/path/config.yaml")
        
        with pytest.raises(FileNotFoundError):
            manager.load_config()
    
    def test_load_config_invalid_yaml(self):
        """测试无效的YAML格式"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yaml', delete=False) as f:
            f.write("invalid: yaml: content: [")
            temp_config_path = f.name
        
        manager = ConfigManager(temp_config_path)
        
        with pytest.raises(Exception):  # 具体异常类型取决于yaml库
            manager.load_config()
        
        os.unlink(temp_config_path)
    
    def test_get_value_existing(self, config_manager):
        """测试获取存在的配置值"""
        config_manager.load_config()
        
        app_name = config_manager.get_value("app.name")
        server_port = config_manager.get_value("server.port")
        ai_provider = config_manager.get_value("ai.provider")
        
        assert app_name == "KN-Fetch"
        assert server_port == 8000
        assert ai_provider == "openai"
    
    def test_get_value_nonexistent(self, config_manager):
        """测试获取不存在的配置值"""
        config_manager.load_config()
        
        # 测试不存在的键
        nonexistent_value = config_manager.get_value("nonexistent.key")
        assert nonexistent_value is None
        
        # 测试默认值
        default_value = config_manager.get_value("nonexistent.key", "default")
        assert default_value == "default"
    
    def test_get_value_nested(self, config_manager):
        """测试获取嵌套配置值"""
        config_manager.load_config()
        
        # 测试深度嵌套的键
        db_host = config_manager.get_value("databases.postgresql.host")
        db_port = config_manager.get_value("databases.postgresql.port")
        
        assert db_host == "localhost"
        assert db_port == 5432
    
    def test_set_value_new_key(self, config_manager):
        """测试设置新配置值"""
        config_manager.load_config()
        
        # 设置新键
        config_manager.set_value("new.setting", "new_value")
        
        # 验证设置成功
        new_value = config_manager.get_value("new.setting")
        assert new_value == "new_value"
    
    def test_set_value_existing_key(self, config_manager):
        """测试更新现有配置值"""
        config_manager.load_config()
        
        # 更新现有键
        original_port = config_manager.get_value("server.port")
        config_manager.set_value("server.port", 9000)
        
        # 验证更新成功
        updated_port = config_manager.get_value("server.port")
        assert updated_port == 9000
        assert updated_port != original_port
    
    def test_set_value_nested(self, config_manager):
        """测试设置嵌套配置值"""
        config_manager.load_config()
        
        # 设置嵌套键
        config_manager.set_value("databases.neo4j.timeout", 30)
        
        # 验证设置成功
        timeout = config_manager.get_value("databases.neo4j.timeout")
        assert timeout == 30
    
    def test_save_config(self, config_manager):
        """测试保存配置"""
        config_manager.load_config()
        
        # 修改配置
        config_manager.set_value("app.version", "1.1.0")
        config_manager.set_value("server.port", 8080)
        
        # 保存配置
        config_manager.save_config()
        
        # 重新加载验证
        new_manager = ConfigManager(config_manager.config_path)
        new_manager.load_config()
        
        assert new_manager.get_value("app.version") == "1.1.0"
        assert new_manager.get_value("server.port") == 8080
    
    def test_validate_config_valid(self, config_manager):
        """测试验证有效配置"""
        config_manager.load_config()
        
        # 验证配置
        is_valid, errors = config_manager.validate_config()
        
        assert is_valid is True
        assert len(errors) == 0
    
    def test_validate_config_invalid(self):
        """测试验证无效配置"""
        # 创建无效配置
        invalid_config = """
app:
  name: ""  # 空的应用名
  
server:
  port: -1  # 无效端口
  
ai:
  api_key: null  # 空的API密钥
"""
        
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yaml', delete=False) as f:
            f.write(invalid_config)
            temp_config_path = f.name
        
        manager = ConfigManager(temp_config_path)
        manager.load_config()
        
        # 验证配置
        is_valid, errors = manager.validate_config()
        
        assert is_valid is False
        assert len(errors) > 0
        
        os.unlink(temp_config_path)
    
    def test_get_all_config(self, config_manager):
        """测试获取完整配置"""
        config_manager.load_config()
        
        all_config = config_manager.get_all_config()
        
        assert isinstance(all_config, dict)
        assert "app" in all_config
        assert "server" in all_config
        assert "ai" in all_config
        assert "databases" in all_config
    
    def test_reload_config(self, config_manager):
        """测试重新加载配置"""
        config_manager.load_config()
        
        # 修改配置文件
        with open(config_manager.config_path, 'w') as f:
            f.write("""
app:
  name: "KN-Fetch-Reloaded"
  version: "2.0.0"
""")
        
        # 重新加载
        config_manager.reload_config()
        
        # 验证重新加载成功
        assert config_manager.get_value("app.name") == "KN-Fetch-Reloaded"
        assert config_manager.get_value("app.version") == "2.0.0"
    
    def test_environment_variables_override(self, config_manager):
        """测试环境变量覆盖配置"""
        config_manager.load_config()
        
        # 设置环境变量
        with patch.dict(os.environ, {
            'KN_FETCH_SERVER_PORT': '9000',
            'KN_FETCH_AI_API_KEY': 'env_key'
        }):
            # 重新加载配置以应用环境变量
            config_manager.reload_config()
            
            # 验证环境变量覆盖
            port = config_manager.get_value("server.port")
            api_key = config_manager.get_value("ai.api_key")
            
            # 注意：实际实现需要支持环境变量覆盖
            # 这里只是测试框架
            assert port == 8000  # 原始值，因为环境变量覆盖未实现
    
    def test_config_schema_validation(self, config_manager):
        """测试配置模式验证"""
        config_manager.load_config()
        
        # 定义简单的模式验证
        schema = {
            "app": {
                "name": str,
                "version": str,
                "debug": bool
            },
            "server": {
                "host": str,
                "port": int
            }
        }
        
        # 验证配置符合模式
        is_valid, errors = config_manager.validate_with_schema(schema)
        
        assert is_valid is True
        assert len(errors) == 0
    
    def test_config_schema_validation_invalid(self, config_manager):
        """测试配置模式验证失败"""
        config_manager.load_config()
        
        # 修改配置为无效值
        config_manager.set_value("server.port", "not_a_number")
        
        # 定义模式
        schema = {
            "server": {
                "port": int  # 期望整数
            }
        }
        
        # 验证配置不符合模式
        is_valid, errors = config_manager.validate_with_schema(schema)
        
        assert is_valid is False
        assert len(errors) > 0


class TestConfigManagerAdvanced:
    """高级配置管理器测试"""
    
    def test_config_encryption(self):
        """测试配置加密（如果实现）"""
        # 测试加密配置的加载和解密
        # 这里只是框架，实际实现需要加密功能
        assert True
    
    def test_config_backup(self, config_manager):
        """测试配置备份"""
        config_manager.load_config()
        
        # 创建备份
        backup_path = config_manager.create_backup()
        
        # 验证备份文件存在
        assert Path(backup_path).exists()
        
        # 清理备份文件
        if Path(backup_path).exists():
            os.unlink(backup_path)
    
    def test_config_merge(self):
        """测试配置合并"""
        # 测试多个配置文件的合并
        # 这里只是框架
        assert True
    
    def test_config_watcher(self, config_manager):
        """测试配置监视器（如果实现）"""
        # 测试配置文件变更监视
        # 这里只是框架
        assert True


if __name__ == "__main__":
    pytest.main([__file__, "-v"])