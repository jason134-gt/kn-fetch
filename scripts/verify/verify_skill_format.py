"""
验证Skill格式符合性
检查所有文档生成代码是否正确实现Skill格式
"""
import re
from pathlib import Path

def check_skill_format_in_code(file_path: str) -> dict:
    """检查代码文件中的Skill格式实现"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    result = {
        "file": file_path,
        "has_type_skill": '"type": "skill"' in content or "'type': 'skill'" in content,
        "has_version": '"version"' in content or "'version'" in content,
        "has_category": '"category"' in content or "'category'" in content,
        "has_tags": '"tags"' in content or "'tags'" in content,
        "has_created": '"created"' in content or "'created'" in content,
        "uses_yaml_dump": "yaml.dump" in content,
    }
    
    result["is_skill_compliant"] = all([
        result["has_type_skill"],
        result["has_version"],
        result["has_category"],
        result["has_tags"],
        result["has_created"],
    ])
    
    return result

def check_generated_document(file_path: str) -> dict:
    """检查生成的文档是否符合Skill格式"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 检查是否有YAML前置元数据
    has_frontmatter = content.startswith('---\n')
    
    if has_frontmatter:
        # 提取YAML部分
        parts = content.split('---\n')
        if len(parts) >= 3:
            yaml_content = parts[1]
            
            return {
                "file": file_path,
                "has_frontmatter": True,
                "has_type": "type:" in yaml_content,
                "has_version": "version:" in yaml_content,
                "has_category": "category:" in yaml_content,
                "has_tags": "tags:" in yaml_content,
                "has_created": "created:" in yaml_content,
                "is_skill_compliant": all([
                    "type:" in yaml_content,
                    "version:" in yaml_content,
                    "category:" in yaml_content,
                    "tags:" in yaml_content,
                    "created:" in yaml_content,
                ])
            }
    
    return {
        "file": file_path,
        "has_frontmatter": False,
        "is_skill_compliant": False
    }

def main():
    print("=" * 60)
    print("Skill格式验证报告")
    print("=" * 60)
    
    # 检查代码文件
    print("\n[1] 检查代码文件中的Skill格式实现")
    print("-" * 40)
    
    code_files = [
        "src/ai/deep_knowledge_analyzer.py",
        "src/agents/documentation.py",
        "src/core/knowledge_document_generator.py"
    ]
    
    for file in code_files:
        if Path(file).exists():
            result = check_skill_format_in_code(file)
            status = "[OK]" if result["is_skill_compliant"] else "[FAIL]"
            print(f"{status} {file}")
            if not result["is_skill_compliant"]:
                print(f"     缺少: type={result['has_type_skill']}, version={result['has_version']}, "
                      f"category={result['has_category']}, tags={result['has_tags']}")
    
    # 检查生成的文档
    print("\n[2] 检查已生成的文档")
    print("-" * 40)
    
    doc_patterns = [
        "output/doc/design/*.md",
        "output/doc/api/*.md",
        "output/doc/uml/*.md",
        "output/doc/message-flows/*.md",
        "output/doc/entities/**/*.md",
        "output/doc/architecture/*.md"
    ]
    
    import glob
    total_docs = 0
    compliant_docs = 0
    
    for pattern in doc_patterns:
        for doc_file in glob.glob(pattern, recursive=True):
            if "index.md" not in doc_file:  # 跳过索引文件
                result = check_generated_document(doc_file)
                total_docs += 1
                if result["is_skill_compliant"]:
                    compliant_docs += 1
    
    print(f"总计: {total_docs} 个文档")
    print(f"符合Skill格式: {compliant_docs} 个")
    print(f"符合率: {compliant_docs/total_docs*100:.1f}%" if total_docs > 0 else "N/A")
    
    # 总结
    print("\n" + "=" * 60)
    print("验证结果总结")
    print("=" * 60)
    print("已修改的文件:")
    print("  1. src/ai/deep_knowledge_analyzer.py - 设计文档生成")
    print("  2. src/agents/documentation.py - 文档Agent")
    print("  3. src/core/knowledge_document_generator.py - 索引文档生成")
    print("\nSkill格式标准:")
    print("  - type: skill")
    print("  - version: 1.0")
    print("  - category: [文档分类]")
    print("  - tags: [标签列表]")
    print("  - created: [时间戳]")

if __name__ == "__main__":
    main()
