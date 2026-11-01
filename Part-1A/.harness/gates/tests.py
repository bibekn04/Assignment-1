#!/usr/bin/env python3
"""
tests.py: Gate 3 - Unit & HTTP Contract tests runner and JaCoCo line coverage evaluator.
"""
import os
import sys
import subprocess
import glob
import csv
import xml.etree.ElementTree as ET
import json

def run_tests_and_coverage(project_dir: str) -> dict:
    mvn_cmd = os.path.join(project_dir, "mvnw.cmd" if os.name == "nt" else "mvnw")
    if not os.path.exists(mvn_cmd):
        mvn_cmd = "mvn"

    cmd = [mvn_cmd, "test", "jacoco:report"]
    proc = subprocess.run(cmd, cwd=project_dir, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)

    # 1. Parse Surefire XMLs
    surefire_dir = os.path.join(project_dir, "target", "surefire-reports")
    xml_files = glob.glob(os.path.join(surefire_dir, "TEST-*.xml"))

    total_tests = 0
    failures_count = 0
    errors_count = 0
    failed_tests = []
    passed_tests = []

    for xf in xml_files:
        try:
            tree = ET.parse(xf)
            root = tree.getroot()
            for tc in root.findall("testcase"):
                total_tests += 1
                cname = tc.attrib.get("classname", "")
                mname = tc.attrib.get("name", "")
                fail_elem = tc.find("failure")
                err_elem = tc.find("error")
                if fail_elem is not None:
                    failures_count += 1
                    failed_tests.append({
                        "name": f"{cname}#{mname}",
                        "error": fail_elem.attrib.get("message", "") or (fail_elem.text or "")[:200]
                    })
                elif err_elem is not None:
                    errors_count += 1
                    failed_tests.append({
                        "name": f"{cname}#{mname}",
                        "error": err_elem.attrib.get("message", "") or (err_elem.text or "")[:200]
                    })
                else:
                    passed_tests.append(f"{cname}#{mname}")
        except Exception as e:
            pass

    # 2. Parse JaCoCo CSV
    jacoco_csv = os.path.join(project_dir, "target", "site", "jacoco", "jacoco.csv")
    line_coverage = 0.0
    total_lines = 0
    covered_lines = 0

    if os.path.exists(jacoco_csv):
        try:
            with open(jacoco_csv, "r", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                for row in reader:
                    missed = int(row.get("LINE_MISSED", 0))
                    covered = int(row.get("LINE_COVERED", 0))
                    total_lines += (missed + covered)
                    covered_lines += covered
            if total_lines > 0:
                line_coverage = (covered_lines / total_lines) * 100.0
        except Exception:
            pass

    coverage_passed = line_coverage >= 90.0 if total_lines > 0 else (total_tests == 0)
    tests_passed = (proc.returncode == 0) and (total_tests > 0) and (failures_count == 0) and (errors_count == 0)

    overall_passed = tests_passed and (line_coverage >= 90.0)

    return {
        "passed": overall_passed,
        "status": "PASS" if overall_passed else "FAIL",
        "total_tests": total_tests,
        "passed_tests_count": len(passed_tests),
        "failures_count": failures_count + errors_count,
        "failed_tests": failed_tests,
        "passed_tests": passed_tests,
        "line_coverage_pct": round(line_coverage, 2),
        "total_lines": total_lines,
        "covered_lines": covered_lines,
        "stdout_tail": proc.stdout[-1500:] if not overall_passed else "",
        "message": f"Tests: {len(passed_tests)}/{total_tests} passed. Line Coverage: {line_coverage:.1f}% (Required: >=90%)"
    }

if __name__ == "__main__":
    proj = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    res = run_tests_and_coverage(proj)
    print(json.dumps(res, indent=2))
    sys.exit(0 if res["passed"] else 1)
