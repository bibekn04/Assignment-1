#!/usr/bin/env python3
"""
traceability.py: Gate 4 - Bidirectional Acceptance Criteria traceability verification.
"""
import os
import sys
import re
import glob
import json
import xml.etree.ElementTree as ET

def verify_traceability(project_dir: str, test_gate_results: dict = None) -> dict:
    spec_path = os.path.join(project_dir, "specs", "001-tiered-rate-limiter", "spec.md")
    if not os.path.exists(spec_path):
        return {
            "passed": False,
            "status": "FAIL",
            "message": f"Specification file missing: {spec_path}"
        }

    # 1. Extract AC IDs from spec.md
    with open(spec_path, "r", encoding="utf-8") as f:
        spec_content = f.read()
    defined_acs = sorted(list(set(re.findall(r"\b(AC-\d{2})\b", spec_content))))

    # 2. Extract mappings from @AcceptanceCriterion annotations in test files
    test_files = glob.glob(os.path.join(project_dir, "src", "test", "java", "**", "*.java"), recursive=True)
    ac_to_tests = {ac: [] for ac in defined_acs}

    for tf in test_files:
        with open(tf, "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()
        current_acs = []
        for line in lines:
            ac_matches = re.findall(r'@AcceptanceCriterion\(\s*"([^"]+)"\s*\)', line)
            if ac_matches:
                current_acs.extend(ac_matches)
            method_match = re.search(r"void\s+([a-zA-Z0-9_]+)\s*\(", line)
            if method_match and current_acs:
                method_name = method_match.group(1)
                for ac in current_acs:
                    if ac in ac_to_tests:
                        ac_to_tests[ac].append(method_name)
                current_acs = []

    # 3. Check which tests actually passed from Surefire reports
    passed_methods = set()
    surefire_dir = os.path.join(project_dir, "target", "surefire-reports")
    xml_files = glob.glob(os.path.join(surefire_dir, "TEST-*.xml"))

    for xf in xml_files:
        try:
            tree = ET.parse(xf)
            root = tree.getroot()
            for tc in root.findall("testcase"):
                mname = tc.attrib.get("name", "")
                if tc.find("failure") is None and tc.find("error") is None:
                    # Strip parameters if parameterized test
                    clean_name = re.sub(r"\(.*\)", "", mname)
                    passed_methods.add(clean_name)
                    passed_methods.add(mname)
        except Exception:
            pass

    # 4. Determine AC coverage status
    covered_acs = []
    uncovered_acs = []

    for ac, tests in ac_to_tests.items():
        passing_tests_for_ac = [t for t in tests if t in passed_methods]
        if passing_tests_for_ac:
            covered_acs.append(ac)
        else:
            uncovered_acs.append(ac)

    coverage_pct = (len(covered_acs) / len(defined_acs) * 100.0) if defined_acs else 0.0
    passed = (len(uncovered_acs) == 0) and (len(defined_acs) > 0)

    return {
        "passed": passed,
        "status": "PASS" if passed else "FAIL",
        "defined_acs_count": len(defined_acs),
        "covered_acs_count": len(covered_acs),
        "coverage_pct": round(coverage_pct, 1),
        "uncovered_acs": uncovered_acs,
        "ac_mapping": ac_to_tests,
        "message": f"Traceability: {len(covered_acs)}/{len(defined_acs)} ACs covered by passing tests ({coverage_pct:.1f}%)."
    }

if __name__ == "__main__":
    proj = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    res = verify_traceability(proj)
    print(json.dumps(res, indent=2))
    sys.exit(0 if res["passed"] else 1)
