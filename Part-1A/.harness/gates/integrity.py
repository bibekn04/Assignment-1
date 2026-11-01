#!/usr/bin/env python3
"""
integrity.py: Gate 0 - Anti-tampering and baseline verification guard.
"""
import os
import sys
import json
import hashlib
import glob
import re

def sha256_file(filepath: str) -> str:
    if not os.path.exists(filepath):
        return ""
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while True:
            chunk = f.read(8192)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest()

def check_integrity(project_dir: str) -> dict:
    baseline_path = os.path.join(project_dir, ".harness", "baseline.json")
    if not os.path.exists(baseline_path):
        return {
            "passed": True,
            "status": "UNLOCKED",
            "message": "Baseline not yet locked (Phase 0-6 pre-lock state)."
        }

    with open(baseline_path, "r", encoding="utf-8") as f:
        baseline = json.load(f)

    violations = []

    # 1. Check config file hashes
    for config_file, expected_hash in baseline.get("file_hashes", {}).items():
        actual_path = os.path.join(project_dir, config_file)
        actual_hash = sha256_file(actual_path)
        if actual_hash != expected_hash:
            violations.append(f"Protected file modified: {config_file} (hash mismatch)")

    # 2. Check for @Disabled or @Ignore annotations in test files
    test_files = glob.glob(os.path.join(project_dir, "src", "test", "java", "**", "*.java"), recursive=True)
    for tf in test_files:
        with open(tf, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()
            if re.search(r"@(?:Disabled|Ignore)\b", content):
                violations.append(f"Forbidden test disabling detected in {os.path.basename(tf)}")

    # 3. Check test method inventory
    current_test_methods = []
    for tf in test_files:
        with open(tf, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                match = re.search(r"void\s+([a-zA-Z0-9_]+)\s*\(", line)
                if match:
                    current_test_methods.append(match.group(1))

    for required_method in baseline.get("test_methods", []):
        if required_method not in current_test_methods:
            violations.append(f"Locked test method was removed or renamed: {required_method}")

    if violations:
        return {
            "passed": False,
            "status": "FAIL",
            "message": f"Integrity violations found ({len(violations)})",
            "violations": violations
        }

    return {
        "passed": True,
        "status": "PASS",
        "message": "All integrity baseline checks passed."
    }

if __name__ == "__main__":
    proj = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    res = check_integrity(proj)
    print(json.dumps(res, indent=2))
    sys.exit(0 if res["passed"] else 1)
