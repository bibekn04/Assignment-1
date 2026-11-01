#!/usr/bin/env python3
"""
lint.py: Gate 1 - Checkstyle static analysis and style guard.
"""
import os
import sys
import subprocess
import xml.etree.ElementTree as ET
import json

def run_lint(project_dir: str) -> dict:
    mvn_cmd = os.path.join(project_dir, "mvnw.cmd" if os.name == "nt" else "mvnw")
    if not os.path.exists(mvn_cmd):
        mvn_cmd = "mvn"

    cmd = [mvn_cmd, "checkstyle:check"]
    proc = subprocess.run(cmd, cwd=project_dir, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)

    result_xml = os.path.join(project_dir, "target", "checkstyle-result.xml")
    violations = []

    if os.path.exists(result_xml):
        try:
            tree = ET.parse(result_xml)
            root = tree.getroot()
            for file_elem in root.findall("file"):
                fname = os.path.relpath(file_elem.attrib.get("name", ""), project_dir)
                for err in file_elem.findall("error"):
                    line = err.attrib.get("line", "?")
                    msg = err.attrib.get("message", "")
                    violations.append(f"{fname}:{line} - {msg}")
        except Exception as e:
            violations.append(f"Failed to parse checkstyle-result.xml: {e}")

    passed = (proc.returncode == 0) and (len(violations) == 0)
    return {
        "passed": passed,
        "status": "PASS" if passed else "FAIL",
        "violations_count": len(violations),
        "violations": violations,
        "stdout_tail": proc.stdout[-1500:] if not passed else "",
        "message": f"Checkstyle reported {len(violations)} violations."
    }

if __name__ == "__main__":
    proj = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    res = run_lint(proj)
    print(json.dumps(res, indent=2))
    sys.exit(0 if res["passed"] else 1)
