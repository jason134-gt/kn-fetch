class GitNexusError(Exception):
    """GitNexus基础异常类"""
    pass

class ConfigurationError(GitNexusError):
    """配置错误"""
    pass

class AnalysisError(GitNexusError):
    """分析错误"""
    pass

class ParseError(GitNexusError):
    """解析错误"""
    pass

class ExportError(GitNexusError):
    """导出错误"""
    pass

class GitOperationError(GitNexusError):
    """Git操作错误"""
    pass

class StorageError(GitNexusError):
    """存储错误"""
    pass
