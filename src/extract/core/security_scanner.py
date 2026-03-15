"""
安全漏洞扫描Agent
实现：SAST静态安全扫描，识别SQL注入、XSS、敏感信息泄露等
"""
import re
import logging
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, field
from enum import Enum
from collections import defaultdict

from ..gitnexus.models import CodeEntity, EntityType

logger = logging.getLogger(__name__)


class VulnerabilityType(str, Enum):
    """漏洞类型"""
    SQL_INJECTION = "sql_injection"
    XSS = "xss"
    COMMAND_INJECTION = "command_injection"
    PATH_TRAVERSAL = "path_traversal"
    HARDCODED_SECRET = "hardcoded_secret"
    INSECURE_DESERIALIZATION = "insecure_deserialization"
    SSRF = "ssrf"
    OPEN_REDIRECT = "open_redirect"
    WEAK_CRYPTO = "weak_crypto"
    SENSITIVE_DATA_EXPOSURE = "sensitive_data_exposure"
    UNVALIDATED_INPUT = "unvalidated_input"
    INFORMATION_DISCLOSURE = "information_disclosure"


class Severity(str, Enum):
    """严重程度"""
    CRITICAL = "critical"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
    INFO = "info"


@dataclass
class Vulnerability:
    """安全漏洞"""
    id: str
    vulnerability_type: VulnerabilityType
    severity: Severity
    entity_id: str
    entity_name: str
    file_path: str
    line_start: int
    line_end: int
    
    # 漏洞详情
    description: str
    vulnerable_code: str
    remediation: str
    
    # 元数据
    cwe_id: Optional[str] = None  # CWE编号
    owasp_category: Optional[str] = None  # OWASP分类
    confidence: float = 1.0
    references: List[str] = field(default_factory=list)


@dataclass
class SecurityScanResult:
    """安全扫描结果"""
    total_scanned: int
    vulnerabilities: List[Vulnerability]
    statistics: Dict[str, Any]
    recommendations: List[str]
    compliance_report: Dict[str, Any]


class SecurityScanner:
    """安全漏洞扫描器"""
    
    # CWE映射
    CWE_MAPPING = {
        VulnerabilityType.SQL_INJECTION: "CWE-89",
        VulnerabilityType.XSS: "CWE-79",
        VulnerabilityType.COMMAND_INJECTION: "CWE-78",
        VulnerabilityType.PATH_TRAVERSAL: "CWE-22",
        VulnerabilityType.HARDCODED_SECRET: "CWE-798",
        VulnerabilityType.SSRF: "CWE-918",
        VulnerabilityType.OPEN_REDIRECT: "CWE-601",
        VulnerabilityType.WEAK_CRYPTO: "CWE-327",
        VulnerabilityType.INSECURE_DESERIALIZATION: "CWE-502",
    }
    
    # OWASP映射
    OWASP_MAPPING = {
        VulnerabilityType.SQL_INJECTION: "A03:2021 - Injection",
        VulnerabilityType.XSS: "A03:2021 - Injection",
        VulnerabilityType.COMMAND_INJECTION: "A03:2021 - Injection",
        VulnerabilityType.HARDCODED_SECRET: "A07:2021 - Identification and Authentication Failures",
        VulnerabilityType.SSRF: "A10:2021 - Server-Side Request Forgery",
        VulnerabilityType.INSECURE_DESERIALIZATION: "A08:2021 - Software and Data Integrity Failures",
        VulnerabilityType.SENSITIVE_DATA_EXPOSURE: "A02:2021 - Cryptographic Failures",
    }
    
    def __init__(self, config: Dict[str, Any] = None):
        self.config = config or {}
        
        # 初始化检测规则
        self._init_detection_rules()
    
    def _init_detection_rules(self):
        """初始化检测规则"""
        self.rules = []
        
        # SQL注入规则
        self.rules.extend([
            {
                "type": VulnerabilityType.SQL_INJECTION,
                "severity": Severity.HIGH,
                "patterns": [
                    r'execute\s*\(\s*["\'].*%s.*["\']',  # 参数化查询但格式错误
                    r'execute\s*\(\s*["\'].*\+.*["\']',  # 字符串拼接SQL
                    r'execute\s*\(\s*f["\'].*\{.*\}.*["\']',  # f-string SQL
                    r'\.format\s*\([^)]*\).*execute',  # format格式化SQL
                    r'cursor\.execute\s*\(\s*["\'].*SELECT.*\+',  # 拼接SELECT
                    r'cursor\.execute\s*\(\s*["\'].*INSERT.*\+',  # 拼接INSERT
                    r'cursor\.execute\s*\(\s*["\'].*UPDATE.*\+',  # 拼接UPDATE
                    r'cursor\.execute\s*\(\s*["\'].*DELETE.*\+',  # 拼接DELETE
                ],
                "description": "潜在的SQL注入漏洞：使用字符串拼接构造SQL查询",
                "remediation": "使用参数化查询或ORM框架，避免字符串拼接SQL",
                "confidence": 0.8
            }
        ])
        
        # 命令注入规则
        self.rules.extend([
            {
                "type": VulnerabilityType.COMMAND_INJECTION,
                "severity": Severity.CRITICAL,
                "patterns": [
                    r'os\.system\s*\([^)]*\+',  # os.system拼接
                    r'os\.popen\s*\([^)]*\+',   # os.popen拼接
                    r'subprocess\.(call|run|Popen)\s*\([^)]*shell\s*=\s*True',  # shell=True
                    r'subprocess\.(call|run|Popen)\s*\([^)]*\+',  # 拼接命令
                    r'eval\s*\([^)]*\+',  # eval拼接
                    r'exec\s*\([^)]*\+',  # exec拼接
                ],
                "description": "潜在的命令注入漏洞：使用用户输入构造系统命令",
                "remediation": "避免使用shell=True，使用参数列表形式传递命令，对用户输入进行严格验证",
                "confidence": 0.85
            }
        ])
        
        # 路径遍历规则
        self.rules.extend([
            {
                "type": VulnerabilityType.PATH_TRAVERSAL,
                "severity": Severity.HIGH,
                "patterns": [
                    r'open\s*\([^)]*\+',  # open拼接路径
                    r'open\s*\([^)]*request\.',  # open使用request参数
                    r'open\s*\([^)]*input\s*\(',  # open使用input
                    r'\.read\s*\([^)]*\+',  # read拼接路径
                    r'send_file\s*\([^)]*\+',  # send_file拼接
                    r'send_from_directory\s*\([^)]*request\.',  # 使用request
                ],
                "description": "潜在的路径遍历漏洞：使用用户输入构造文件路径",
                "remediation": "验证和规范化文件路径，使用白名单限制可访问目录",
                "confidence": 0.75
            }
        ])
        
        # 硬编码密钥规则
        self.rules.extend([
            {
                "type": VulnerabilityType.HARDCODED_SECRET,
                "severity": Severity.HIGH,
                "patterns": [
                    r'(?i)(password|passwd|pwd)\s*=\s*["\'][^"\']{8,}["\']',  # 密码
                    r'(?i)(api[_-]?key|apikey)\s*=\s*["\'][a-zA-Z0-9]{20,}["\']',  # API Key
                    r'(?i)(secret[_-]?key|secretkey)\s*=\s*["\'][a-zA-Z0-9]{16,}["\']',  # Secret Key
                    r'(?i)(access[_-]?token|accesstoken)\s*=\s*["\'][a-zA-Z0-9]{20,}["\']',  # Token
                    r'(?i)(private[_-]?key)\s*=\s*["\']-----BEGIN',  # 私钥
                    r'(?i)aws[_-]?secret[_-]?access[_-]?key\s*=\s*["\'][a-zA-Z0-9/+=]{40}["\']',  # AWS密钥
                    r'sk-[a-zA-Z0-9]{20,}',  # OpenAI Key格式
                    r'AKIA[A-Z0-9]{16}',  # AWS Access Key格式
                ],
                "description": "发现硬编码的敏感信息：密钥、密码或令牌",
                "remediation": "使用环境变量或密钥管理服务存储敏感信息，不要将密钥硬编码在代码中",
                "confidence": 0.9
            }
        ])
        
        # XSS规则
        self.rules.extend([
            {
                "type": VulnerabilityType.XSS,
                "severity": Severity.MEDIUM,
                "patterns": [
                    r'render_template_string\s*\([^)]*request\.',  # 模板注入
                    r'Markup\s*\([^)]*\+',  # Markup拼接
                    r'\|safe(?!\s*\})',  # Jinja2 safe过滤器
                    r'innerHTML\s*=',  # JS innerHTML
                    r'document\.write\s*\(',  # document.write
                ],
                "description": "潜在的XSS漏洞：未正确转义用户输入",
                "remediation": "对用户输入进行HTML转义，避免使用|safe过滤器，使用模板引擎的自动转义功能",
                "confidence": 0.7
            }
        ])
        
        # SSRF规则
        self.rules.extend([
            {
                "type": VulnerabilityType.SSRF,
                "severity": Severity.HIGH,
                "patterns": [
                    r'requests\.(get|post|put|delete)\s*\([^)]*request\.',  # 使用request参数
                    r'urllib\.request\.urlopen\s*\([^)]*\+',  # 拼接URL
                    r'httpx\.(get|post)\s*\([^)]*request\.',  # httpx使用request
                    r'aiohttp[^)]*request\.',  # aiohttp
                ],
                "description": "潜在的SSRF漏洞：使用用户输入构造外部请求URL",
                "remediation": "验证和限制请求目标，使用白名单机制，禁止访问内网地址",
                "confidence": 0.75
            }
        ])
        
        # 弱加密规则
        self.rules.extend([
            {
                "type": VulnerabilityType.WEAK_CRYPTO,
                "severity": Severity.MEDIUM,
                "patterns": [
                    r'hashlib\.(md5|sha1)\s*\(',  # 弱哈希算法
                    r'DES\.new\s*\(',  # DES加密
                    r'from\s+Crypto\.Cipher\s+import\s+DES',  # DES导入
                    r'random\.random\s*\([^)]*\).*password',  # 弱随机数生成密码
                    r'random\.randint\s*\([^)]*\).*key',  # 弱随机数生成密钥
                ],
                "description": "使用弱加密算法或不安全的随机数生成器",
                "remediation": "使用强加密算法（如AES-256, SHA-256），密码学用途使用secrets模块",
                "confidence": 0.85
            }
        ])
        
        # 敏感信息暴露规则
        self.rules.extend([
            {
                "type": VulnerabilityType.SENSITIVE_DATA_EXPOSURE,
                "severity": Severity.MEDIUM,
                "patterns": [
                    r'print\s*\([^)]*password',  # 打印密码
                    r'print\s*\([^)]*token',  # 打印token
                    r'print\s*\([^)]*secret',  # 打印secret
                    r'logging\.(debug|info|warning)\s*\([^)]*password',  # 日志记录密码
                    r'logger\.(debug|info|warning)\s*\([^)]*token',  # 日志记录token
                    r'return\s+[^)]*password',  # 返回密码
                    r'json\.dumps\s*\([^)]*password',  # JSON序列化密码
                ],
                "description": "敏感信息可能被暴露到日志或响应中",
                "remediation": "避免在日志中记录敏感信息，敏感字段在返回前应被脱敏",
                "confidence": 0.7
            }
        ])
        
        # 未验证输入规则
        self.rules.extend([
            {
                "type": VulnerabilityType.UNVALIDATED_INPUT,
                "severity": Severity.MEDIUM,
                "patterns": [
                    r'request\.(args|form|json)\[[\'"][^\'"]+[\'"]\]\s*\)',  # 直接使用request参数
                    r'int\s*\(\s*request\.',  # 未捕获异常的类型转换
                    r'float\s*\(\s*request\.',  # 未捕获异常的类型转换
                ],
                "description": "未验证的用户输入直接使用",
                "remediation": "对所有用户输入进行验证和清洗，使用验证框架（如pydantic, marshmallow）",
                "confidence": 0.6
            }
        ])
    
    def scan(self, entities: List[CodeEntity]) -> SecurityScanResult:
        """扫描安全漏洞"""
        vulnerabilities = []
        vuln_id = 0
        
        # 扫描每个实体
        for entity in entities:
            if not entity.content:
                continue
            
            # 跳过非代码实体
            if entity.entity_type in [EntityType.COMMENT, EntityType.DOCSTRING, EntityType.TODO]:
                continue
            
            # 应用所有规则
            for rule in self.rules:
                for pattern in rule["patterns"]:
                    try:
                        matches = re.finditer(pattern, entity.content, re.IGNORECASE | re.MULTILINE)
                        for match in matches:
                            vuln_id += 1
                            
                            # 计算行号
                            line_start = entity.content[:match.start()].count('\n') + entity.start_line
                            line_end = entity.content[:match.end()].count('\n') + entity.start_line
                            
                            vulnerability = Vulnerability(
                                id=f"vuln_{vuln_id}",
                                vulnerability_type=rule["type"],
                                severity=rule["severity"],
                                entity_id=entity.id,
                                entity_name=entity.name,
                                file_path=entity.file_path,
                                line_start=line_start,
                                line_end=line_end,
                                description=rule["description"],
                                vulnerable_code=match.group(0)[:100],
                                remediation=rule["remediation"],
                                cwe_id=self.CWE_MAPPING.get(rule["type"]),
                                owasp_category=self.OWASP_MAPPING.get(rule["type"]),
                                confidence=rule.get("confidence", 1.0)
                            )
                            vulnerabilities.append(vulnerability)
                    except Exception as e:
                        logger.warning(f"规则匹配失败 {pattern}: {e}")
        
        # 去重
        vulnerabilities = self._deduplicate_vulnerabilities(vulnerabilities)
        
        # 生成统计
        statistics = self._generate_statistics(entities, vulnerabilities)
        
        # 生成建议
        recommendations = self._generate_recommendations(vulnerabilities, statistics)
        
        # 生成合规报告
        compliance_report = self._generate_compliance_report(vulnerabilities)
        
        return SecurityScanResult(
            total_scanned=len(entities),
            vulnerabilities=vulnerabilities,
            statistics=statistics,
            recommendations=recommendations,
            compliance_report=compliance_report
        )
    
    def _deduplicate_vulnerabilities(self, vulnerabilities: List[Vulnerability]) -> List[Vulnerability]:
        """去重漏洞"""
        seen = set()
        unique = []
        
        for vuln in vulnerabilities:
            key = (vuln.entity_id, vuln.vulnerability_type, vuln.line_start)
            if key not in seen:
                seen.add(key)
                unique.append(vuln)
        
        return sorted(unique, key=lambda v: (v.severity.value, -v.confidence))
    
    def _generate_statistics(
        self, 
        entities: List[CodeEntity],
        vulnerabilities: List[Vulnerability]
    ) -> Dict[str, Any]:
        """生成统计信息"""
        severity_counts = defaultdict(int)
        type_counts = defaultdict(int)
        file_counts = defaultdict(int)
        
        for vuln in vulnerabilities:
            severity_counts[vuln.severity.value] += 1
            type_counts[vuln.vulnerability_type.value] += 1
            file_counts[vuln.file_path] += 1
        
        # 计算安全评分 (0-100)
        critical = severity_counts.get("critical", 0)
        high = severity_counts.get("high", 0)
        medium = severity_counts.get("medium", 0)
        low = severity_counts.get("low", 0)
        
        security_score = max(0, 100 - (critical * 30 + high * 15 + medium * 5 + low * 2))
        
        return {
            "total_vulnerabilities": len(vulnerabilities),
            "severity_distribution": dict(severity_counts),
            "type_distribution": dict(type_counts),
            "affected_files": len(file_counts),
            "security_score": security_score,
            "risk_level": self._get_risk_level(security_score)
        }
    
    def _get_risk_level(self, score: int) -> str:
        """根据分数获取风险等级"""
        if score >= 80:
            return "低风险"
        elif score >= 60:
            return "中等风险"
        elif score >= 40:
            return "高风险"
        else:
            return "极高风险"
    
    def _generate_recommendations(
        self, 
        vulnerabilities: List[Vulnerability],
        statistics: Dict[str, Any]
    ) -> List[str]:
        """生成建议"""
        recommendations = []
        
        # 根据漏洞类型生成建议
        type_set = set(v.vulnerability_type for v in vulnerabilities)
        
        if VulnerabilityType.SQL_INJECTION in type_set:
            recommendations.append("发现SQL注入风险，建议全面审查数据库查询代码，使用ORM或参数化查询")
        
        if VulnerabilityType.COMMAND_INJECTION in type_set:
            recommendations.append("发现命令注入风险，建议审查所有系统命令调用，避免shell=True")
        
        if VulnerabilityType.HARDCODED_SECRET in type_set:
            recommendations.append("发现硬编码密钥，立即更换这些密钥，使用环境变量或密钥管理服务")
        
        if VulnerabilityType.XSS in type_set:
            recommendations.append("发现XSS风险，建议启用模板引擎的自动转义功能")
        
        if VulnerabilityType.SSRF in type_set:
            recommendations.append("发现SSRF风险，建议对用户输入的URL进行严格验证和白名单限制")
        
        # 根据严重程度生成建议
        critical_count = statistics.get("severity_distribution", {}).get("critical", 0)
        high_count = statistics.get("severity_distribution", {}).get("high", 0)
        
        if critical_count > 0:
            recommendations.insert(0, f"发现 {critical_count} 个严重漏洞，需要立即修复！")
        
        if high_count > 0:
            recommendations.append(f"发现 {high_count} 个高危漏洞，建议在下一个迭代周期内修复")
        
        if not recommendations:
            recommendations.append("未发现明显安全漏洞，建议定期进行安全扫描")
        
        return recommendations
    
    def _generate_compliance_report(self, vulnerabilities: List[Vulnerability]) -> Dict[str, Any]:
        """生成合规报告"""
        owasp_counts = defaultdict(int)
        cwe_counts = defaultdict(int)
        
        for vuln in vulnerabilities:
            if vuln.owasp_category:
                owasp_counts[vuln.owasp_category] += 1
            if vuln.cwe_id:
                cwe_counts[vuln.cwe_id] += 1
        
        return {
            "owasp_top_10": dict(owasp_counts),
            "cwe_top_issues": dict(sorted(cwe_counts.items(), key=lambda x: x[1], reverse=True)[:10]),
            "standards": {
                "OWASP Top 10 2021": {
                    "compliant": len(owasp_counts) == 0,
                    "violations": list(owasp_counts.keys())
                }
            }
        }
    
    def export_report(self, result: SecurityScanResult, output_path: str) -> str:
        """导出报告"""
        import json
        
        def convert(obj):
            if hasattr(obj, '__dict__'):
                return {k: convert(v) for k, v in obj.__dict__.items()}
            elif isinstance(obj, list):
                return [convert(item) for item in obj]
            elif isinstance(obj, dict):
                return {k: convert(v) for k, v in obj.items()}
            elif isinstance(obj, Enum):
                return obj.value
            return obj
        
        data = convert(result)
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        return output_path
    
    def generate_markdown_report(self, result: SecurityScanResult) -> str:
        """生成Markdown格式的报告"""
        lines = [
            "# 安全漏洞扫描报告",
            "",
            f"扫描时间：{self._get_current_time()}",
            f"扫描文件/实体数：{result.total_scanned}",
            "",
            "## 扫描摘要",
            "",
            f"- **安全评分**：{result.statistics.get('security_score', 0)}/100",
            f"- **风险等级**：{result.statistics.get('risk_level', '未知')}",
            f"- **发现漏洞总数**：{result.statistics.get('total_vulnerabilities', 0)}",
            "",
            "### 漏洞严重程度分布",
            "",
        ]
        
        severity_dist = result.statistics.get("severity_distribution", {})
        for severity, count in severity_dist.items():
            lines.append(f"- {severity}: {count}")
        
        lines.extend([
            "",
            "## 漏洞详情",
            ""
        ])
        
        for vuln in result.vulnerabilities[:50]:  # 限制显示数量
            lines.extend([
                f"### {vuln.id}: {vuln.vulnerability_type.value}",
                "",
                f"- **严重程度**：{vuln.severity.value}",
                f"- **位置**：`{vuln.file_path}:{vuln.line_start}`",
                f"- **描述**：{vuln.description}",
                f"- **问题代码**：`{vuln.vulnerable_code}`",
                f"- **修复建议**：{vuln.remediation}",
                ""
            ])
        
        lines.extend([
            "## 修复建议",
            ""
        ])
        
        for i, rec in enumerate(result.recommendations, 1):
            lines.append(f"{i}. {rec}")
        
        return "\n".join(lines)
    
    def _get_current_time(self) -> str:
        """获取当前时间"""
        from datetime import datetime
        return datetime.now().strftime('%Y-%m-%d %H:%M:%S')
