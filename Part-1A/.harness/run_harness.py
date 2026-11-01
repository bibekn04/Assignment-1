#!/usr/bin/env python3
"""
run_harness.py: Master Deterministic Evaluation Harness for Assignment 1 (Part 1A).
Executes Gates 0 through 4, preserves structured run evidence, and appends to harness-log.md.
"""
import os
import sys
import json
import time
import shutil
import subprocess
from datetime import datetime

from typing import Tuple

# Import gates
sys.path.insert(0, os.path.dirname(__file__))
from gates.integrity import check_integrity
from gates.lint import run_lint
from gates.security import run_security
from gates.tests import run_tests_and_coverage
from gates.traceability import verify_traceability
from feedback import format_feedback

def get_git_commit(project_dir: str) -> str:
    try:
        res = subprocess.run(["git", "rev-parse", "HEAD"], cwd=project_dir, capture_output=True, text=True)
        return res.stdout.strip() if res.returncode == 0 else "unknown"
    except Exception:
        return "unknown"

def get_next_run_dir(evidence_dir: str) -> Tuple[str, str]:
    results_dir = os.path.join(evidence_dir, "harness-results")
    os.makedirs(results_dir, exist_ok=True)
    existing_runs = [d for d in os.listdir(results_dir) if d.startswith("run-")]
    numbers = []
    for r in existing_runs:
        try:
            numbers.append(int(r.split("-")[1]))
        except ValueError:
            pass
    next_num = max(numbers, default=0) + 1
    run_name = f"run-{next_num:03d}"
    run_path = os.path.join(results_dir, run_name)
    os.makedirs(run_path, exist_ok=True)
    return run_name, run_path

def append_to_harness_log(evidence_dir: str, run_name: str, commit_sha: str, summary: dict):
    log_file = os.path.join(evidence_dir, "harness-log.md")
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    overall = summary["overall_status"]

    lines = [
        f"\n### Harness Execution: `{run_name}`",
        f"- **Timestamp**: {timestamp}",
        f"- **Commit**: `{commit_sha[:8]}`",
        f"- **Overall Outcome**: **{overall}**",
        "- **Gate Results**:",
        f"  - Gate 0 (Integrity): `{summary['gates']['gate_0_integrity']['status']}` - {summary['gates']['gate_0_integrity']['message']}",
        f"  - Gate 1 (Lint): `{summary['gates']['gate_1_lint']['status']}` - {summary['gates']['gate_1_lint']['message']}",
        f"  - Gate 2 (Security): `{summary['gates']['gate_2_security']['status']}` - {summary['gates']['gate_2_security']['message']}",
        f"  - Gate 3 (Tests & Coverage): `{summary['gates']['gate_3_tests']['status']}` - {summary['gates']['gate_3_tests']['message']}",
        f"  - Gate 4 (Traceability): `{summary['gates']['gate_4_traceability']['status']}` - {summary['gates']['gate_4_traceability']['message']}",
        f"- **Run Artifacts**: `evidence/harness-results/{run_name}/`",
        ""
    ]

    mode = "a" if os.path.exists(log_file) else "w"
    if mode == "w":
        header = "# Part 1A Deterministic Harness Execution Log\n\nThis document records the chronological record of all automated harness verification executions, feedback cycles, and evaluation results.\n"
        with open(log_file, "w", encoding="utf-8") as f:
            f.write(header)

    with open(log_file, "a", encoding="utf-8") as f:
        f.write("\n".join(lines))

def main():
    project_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    evidence_dir = os.path.join(project_dir, "evidence")
    os.makedirs(evidence_dir, exist_ok=True)

    run_name, run_dir = get_next_run_dir(evidence_dir)
    print(f"============================================================")
    print(f" Starting Deterministic Harness: {run_name}")
    print(f" Project Directory: {project_dir}")
    print(f"============================================================")

    start_time = time.time()
    commit_sha = get_git_commit(project_dir)

    summary = {
        "run_name": run_name,
        "timestamp": datetime.now().isoformat(),
        "git_commit": commit_sha,
        "java_version": "17.0.4.1",
        "gates": {},
        "overall_status": "PASS"
    }

    # Gate 0: Integrity
    print("\n[1/5] Running Gate 0: Integrity Guard...")
    res_g0 = check_integrity(project_dir)
    summary["gates"]["gate_0_integrity"] = res_g0
    with open(os.path.join(run_dir, "integrity.json"), "w", encoding="utf-8") as f:
        json.dump(res_g0, f, indent=2)

    if not res_g0["passed"]:
        summary["overall_status"] = "FAIL"
        feedback = format_feedback("integrity", res_g0)
        with open(os.path.join(run_dir, "feedback.md"), "w", encoding="utf-8") as f:
            f.write(feedback)
        print(" Gate 0 FAILED.")

    # Gate 1: Lint
    print("[2/5] Running Gate 1: Checkstyle Lint Guard...")
    res_g1 = run_lint(project_dir)
    summary["gates"]["gate_1_lint"] = res_g1
    if not res_g1["passed"] and summary["overall_status"] == "PASS":
        summary["overall_status"] = "FAIL"
        feedback = format_feedback("lint", res_g1)
        with open(os.path.join(run_dir, "feedback.md"), "w", encoding="utf-8") as f:
            f.write(feedback)

    # Copy checkstyle report if exists
    cs_xml = os.path.join(project_dir, "target", "checkstyle-result.xml")
    if os.path.exists(cs_xml):
        shutil.copy2(cs_xml, os.path.join(run_dir, "checkstyle.xml"))

    # Gate 2: Security
    print("[3/5] Running Gate 2: SpotBugs + FindSecBugs SAST Guard...")
    res_g2 = run_security(project_dir)
    summary["gates"]["gate_2_security"] = res_g2
    if not res_g2["passed"] and summary["overall_status"] == "PASS":
        summary["overall_status"] = "FAIL"
        feedback = format_feedback("security", res_g2)
        with open(os.path.join(run_dir, "feedback.md"), "w", encoding="utf-8") as f:
            f.write(feedback)

    # Copy spotbugs report if exists
    sb_xml = os.path.join(project_dir, "target", "spotbugsXml.xml")
    if os.path.exists(sb_xml):
        shutil.copy2(sb_xml, os.path.join(run_dir, "spotbugs.xml"))

    # Gate 3: Tests & Coverage
    print("[4/5] Running Gate 3: Unit/Contract Tests & JaCoCo Coverage Guard...")
    res_g3 = run_tests_and_coverage(project_dir)
    summary["gates"]["gate_3_tests"] = res_g3
    if not res_g3["passed"] and summary["overall_status"] == "PASS":
        summary["overall_status"] = "FAIL"
        feedback = format_feedback("tests", res_g3)
        with open(os.path.join(run_dir, "feedback.md"), "w", encoding="utf-8") as f:
            f.write(feedback)

    # Copy surefire reports & jacoco.csv
    surefire_src = os.path.join(project_dir, "target", "surefire-reports")
    if os.path.exists(surefire_src):
        surefire_dest = os.path.join(run_dir, "surefire")
        if os.path.exists(surefire_dest):
            shutil.rmtree(surefire_dest)
        shutil.copytree(surefire_src, surefire_dest)

    jacoco_src = os.path.join(project_dir, "target", "site", "jacoco", "jacoco.csv")
    if os.path.exists(jacoco_src):
        shutil.copy2(jacoco_src, os.path.join(run_dir, "jacoco.csv"))

    # Gate 4: Traceability
    print("[5/5] Running Gate 4: Acceptance Criteria Traceability Guard...")
    res_g4 = verify_traceability(project_dir, res_g3)
    summary["gates"]["gate_4_traceability"] = res_g4
    with open(os.path.join(run_dir, "traceability.json"), "w", encoding="utf-8") as f:
        json.dump(res_g4, f, indent=2)

    if not res_g4["passed"] and summary["overall_status"] == "PASS":
        summary["overall_status"] = "FAIL"
        feedback = format_feedback("traceability", res_g4)
        with open(os.path.join(run_dir, "feedback.md"), "w", encoding="utf-8") as f:
            f.write(feedback)

    duration = round(time.time() - start_time, 2)
    summary["duration_seconds"] = duration

    # Save summary.json
    with open(os.path.join(run_dir, "summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2)

    # Append to harness-log.md
    append_to_harness_log(evidence_dir, run_name, commit_sha, summary)

    print("\n------------------------------------------------------------")
    print(f" Harness Execution Complete: {run_name} [{summary['overall_status']}] in {duration}s")
    for gname, ginfo in summary["gates"].items():
        print(f"  * {gname}: {ginfo.get('status')} ({ginfo.get('message')})")
    print(f" Results preserved in: {os.path.relpath(run_dir, project_dir)}")
    print("------------------------------------------------------------")

    sys.exit(0 if summary["overall_status"] == "PASS" else 1)

if __name__ == "__main__":
    main()
