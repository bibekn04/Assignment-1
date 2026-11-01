#!/usr/bin/env python3
"""
security.py: Gate 2 - SpotBugs + FindSecBugs static application security testing (SAST).
"""
import os
import sys
import subprocess
import xml.etree.ElementTree as ET
import json

def run_security(project_dir: str) -> dict:
    mvn_cmd = os.path.join(project_dir, "mvnw.cmd" if os.name == "nt" else "mvnw")
    if not os.path.exists(mvn_cmd):
        mvn_cmd = "mvn"

    cmd = [mvn_cmd, "compile", "spotbugs:check"]
    proc = subprocess.run(cmd, cwd=project_dir, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)

    result_xml = os.path.join(project_dir, "target", "spotbugsXml.xml")
    findings = []

    if os.path.exists(result_xml):
        try:
            tree = ET.parse(result_xml)
            root = tree.getroot()
            for bug in root.findall("BugInstance"):
                b_type = bug.attrib.get("type", "Unknown")
                category = bug.attrib.get("category", "")
                priority = bug.attrib.get("priority", "")
                msg_elem = bug.find("LongMessage")
                msg = msg_elem.text if msg_elem is not None else b_type
                findings.append(f"[{category}][Priority {priority}] {b_type}: {msg}")
        except Exception as e:
            findings.append(f"Failed to parse spotbugsXml.xml: {e}")

    passed = (proc.returncode == 0) and (len(findings) == 0)
    return {
        "passed": passed,
        "status": "PASS" if passed else "FAIL",
        "findings_count": len(findings),
        "violations": findings,
        "stdout_tail": proc.stdout[-1500:] if not passed else "",
        "message": f"SpotBugs reported {len(findings)} security/correctness findings."
    }

if __name__ == "__main__":
    proj = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    res = run_security(proj)
    print(json.dumps(res, indent=2))
    sys.exit(0 if res["passed"] else 1)
