#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""全面检查所有问题修复情况和Skill格式规范"""

import json
from pathlib import Path
import yaml

# 读取知识图谱
with open("output/knowledge_graph", "r", encoding="utf-8") as f:
    data = json.load(f)

entities = data.get("entities", {})
relationships = data.get("relationships", [])

print("=" * 80)
print("Comprehensive Issue Check and Skill Format Validation")
print("=" * 80)

# ==================== Issue 1: Relationship Count ====================
print("\n[Issue 1] Relationship Extraction")
print("-" * 80)
print(f"Total entities: {len(entities)}")
print(f"Total relationships: {len(relationships)}")

if len(relationships) > 0:
    print("[OK] Fixed: Relationship data successfully extracted")
    # 统计关系类型
    rel_types = {}
    for rel in relationships:
        rel_type = rel.get('relationship_type', 'unknown')
        rel_types[rel_type] = rel_types.get(rel_type, 0) + 1
    print(f"   Relationship types: {rel_types}")
else:
    print("[FAIL] Not fixed: Relationship data is still 0")

# ==================== Issue 2: UML Document Completeness ====================
print("\n[Issue 2] UML Document Completeness")
print("-" * 80)

uml_dir = Path("output/doc/uml")
uml_files = list(uml_dir.glob("*.md")) if uml_dir.exists() else []

print(f"UML document count: {len(uml_files)}")
print("UML document list:")
for f in uml_files:
    print(f"  - {f.name}")

required_uml = ["tech-stack.md", "class-diagram.md", "call-diagram.md", "module-diagram.md"]
has_sequence = any("sequence-diagram" in f.name for f in uml_files)

missing_uml = [r for r in required_uml if not (uml_dir / r).exists()]

if not missing_uml and has_sequence:
    print("[OK] Fixed: UML documents complete, including sequence diagrams")
else:
    if missing_uml:
        print(f"[FAIL] Missing UML documents: {missing_uml}")
    if not has_sequence:
        print("[FAIL] Missing sequence diagrams")

# ==================== Issue 3: Sequence Diagrams and Business Logic ====================
print("\n[Issue 3] Sequence Diagrams and Business Logic Annotations")
print("-" * 80)

sequence_files = [f for f in uml_files if "sequence-diagram" in f.name]
print(f"Sequence diagram count: {len(sequence_files)}")

# 检查时序图是否包含业务逻辑说明
has_business_logic = False
if sequence_files:
    sample_file = sequence_files[0]
    with open(sample_file, 'r', encoding='utf-8') as f:
        content = f.read()
        has_business_logic = "业务逻辑说明" in content or "业务逻辑" in content or "Business Logic" in content

if sequence_files and has_business_logic:
    print(f"[OK] Fixed: Generated {len(sequence_files)} sequence diagrams with business logic annotations")
elif sequence_files:
    print(f"[PARTIAL] Generated {len(sequence_files)} sequence diagrams but missing business logic annotations")
else:
    print("[FAIL] Not fixed: Missing sequence diagrams")

# ==================== Issue 4: API Documentation ====================
print("\n[Issue 4] API Interface Documentation")
print("-" * 80)

api_dir = Path("output/doc/api")
api_files = list(api_dir.glob("*.md")) if api_dir.exists() else []

print(f"API document count: {len(api_files)}")

if api_files:
    print(f"[OK] Fixed: Generated {len(api_files)} API documents")
    # 检查API文档是否包含方法列表
    sample_api = api_dir / "index.md"
    if sample_api.exists():
        with open(sample_api, 'r', encoding='utf-8') as f:
            content = f.read()
            api_count = content.count("- [")
            print(f"   API endpoint count: {api_count}")
else:
    print("[FAIL] Not fixed: Missing API interface documents")

# ==================== Issue 5: Message Flow Documentation ====================
print("\n[Issue 5] Message Flow Documentation")
print("-" * 80)

msg_dir = Path("output/doc/message-flows")
msg_files = list(msg_dir.glob("*.md")) if msg_dir.exists() else []

print(f"Message flow document count: {len(msg_files)}")

if msg_files:
    print(f"[OK] Fixed: Generated {len(msg_files)} message flow documents")
    # 检查是否包含流程图
    flow_diagram = msg_dir / "flow-diagram.md"
    if flow_diagram.exists():
        print("   [OK] Includes message flow diagram")
else:
    print("[FAIL] Not fixed: Missing message flow documents")

# ==================== Skill Format Validation ====================
print("\n[Skill Format Specification Validation]")
print("-" * 80)

def check_skill_format(file_path):
    """Check if file conforms to Skill format"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # Check YAML frontmatter
        if not content.startswith("---\n"):
            return False, "Missing YAML frontmatter"
        
        # Extract YAML part
        parts = content.split("---\n", 2)
        if len(parts) < 3:
            return False, "Invalid YAML format"
        
        yaml_content = parts[1]
        
        # Parse YAML
        try:
            metadata = yaml.safe_load(yaml_content)
        except:
            return False, "YAML parsing failed"
        
        # Check required fields
        required_fields = ['type', 'version', 'category']
        missing_fields = [f for f in required_fields if f not in metadata]
        
        if missing_fields:
            return False, f"Missing fields: {missing_fields}"
        
        return True, "Conforms to Skill format"
    except Exception as e:
        return False, str(e)

# Sample check of various document types
sample_files = {
    "UML Sequence Diagram": "output/doc/uml/sequence-diagram-1.md",
    "API Document": "output/doc/api/index.md",
    "Message Flow Document": "output/doc/message-flows/index.md",
    "Design Document": "output/doc/design/project_overview.md",
    "Entity Document": None
}

# Find an entity document
entities_dir = Path("output/doc/entities")
if entities_dir.exists():
    for subtype_dir in entities_dir.iterdir():
        if subtype_dir.is_dir():
            entity_files = list(subtype_dir.glob("*.md"))
            if entity_files:
                sample_files["Entity Document"] = str(entity_files[0])
                break

skill_results = {}
for doc_type, file_path in sample_files.items():
    if file_path and Path(file_path).exists():
        is_valid, message = check_skill_format(file_path)
        skill_results[doc_type] = (is_valid, message)
        status = "[OK]" if is_valid else "[FAIL]"
        print(f"{status} {doc_type}: {message}")
    else:
        skill_results[doc_type] = (False, "File not found")
        print(f"[FAIL] {doc_type}: File not found")

# ==================== Document Structure Completeness ====================
print("\n[Document Structure Completeness]")
print("-" * 80)

doc_structure = {
    "entities/": "Entity Knowledge Documents",
    "modules/": "Module Knowledge Documents",
    "architecture/": "Architecture Documents",
    "uml/": "UML Design Documents",
    "design/": "Design Documents",
    "business/": "Business Flow Documents",
    "api/": "API Interface Documents",
    "message-flows/": "Message Flow Documents"
}

for folder, desc in doc_structure.items():
    folder_path = Path("output/doc") / folder
    if folder_path.exists():
        file_count = len(list(folder_path.glob("**/*.md")))
        print(f"[OK] {desc} ({folder}): {file_count} documents")
    else:
        print(f"[FAIL] {desc} ({folder}): Directory not found")

# ==================== Summary ====================
print("\n" + "=" * 80)
print("Fix Status Summary")
print("=" * 80)

issues = [
    ("Relationship extraction", len(relationships) > 0),
    ("UML document completeness", len(uml_files) >= 4 and has_sequence),
    ("Sequence diagrams and business logic", len(sequence_files) > 0 and has_business_logic),
    ("API interface documents", len(api_files) > 0),
    ("Message flow documents", len(msg_files) > 0),
]

skill_compliant = sum(1 for v, (is_valid, _) in skill_results.items() if is_valid)
total_docs = len(skill_results)

print("\nIssue Fix Status:")
for i, (issue, fixed) in enumerate(issues, 1):
    status = "[OK] Fixed" if fixed else "[FAIL] Not fixed"
    print(f"{i}. {issue}: {status}")

print(f"\nSkill Format Compliance:")
print(f"Compliant document types: {skill_compliant}/{total_docs}")

all_fixed = all(fixed for _, fixed in issues)
all_skill = skill_compliant == total_docs

if all_fixed and all_skill:
    print("\n[SUCCESS] All issues fixed, all documents conform to Skill format!")
elif all_fixed:
    print(f"\n[OK] All issues fixed, but {total_docs - skill_compliant} document types not conforming to Skill format")
else:
    print("\n[WARNING] Some issues not fully fixed")

print("=" * 80)
