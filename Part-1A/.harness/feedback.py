#!/usr/bin/env python3
"""
feedback.py: Generates structured failure feedback (feedback.md) for the coding agent.
"""
import os
import json
from typing import Dict, Any, List

MUST_NOT_CHANGE = [
    "src/test/**",
    ".harness/**",
    "specs/**",
    "config/checkstyle.xml",
    "config/spotbugs-exclude.xml",
    "pom.xml (coverage/lint/sast plugin thresholds)"
]

def format_feedback(failed_gate: str, failure_details: Dict[str, Any]) -> str:
    """Creates a markdown feedback message instructing the coding agent on repair."""
    lines = [
        "# HARNESS FAILURE FEEDBACK",
        f"**Failed Gate**: {failed_gate.upper()}",
        "",
        "## Failure Summary",
        f"- **Message**: {failure_details.get('message', 'Gate check failed')}",
    ]

    if "violations" in failure_details and failure_details["violations"]:
        lines.append("\n### Violations / Findings")
        for v in failure_details["violations"][:15]:
            lines.append(f"- {v}")
        if len(failure_details["violations"]) > 15:
            lines.append(f"- ... and {len(failure_details['violations']) - 15} more.")

    if "failed_tests" in failure_details and failure_details["failed_tests"]:
        lines.append("\n### Failed Tests")
        for t in failure_details["failed_tests"][:10]:
            lines.append(f"- **Test**: `{t.get('name')}`")
            if "ac" in t:
                lines.append(f"  **Acceptance Criterion**: `{t.get('ac')}`")
            lines.append(f"  **Error**: {t.get('error', '').strip()}")

    if "uncovered_acs" in failure_details and failure_details["uncovered_acs"]:
        lines.append("\n### Untraced / Failing Acceptance Criteria")
        for ac in failure_details["uncovered_acs"]:
            lines.append(f"- `{ac}`")

    if "stdout_tail" in failure_details and failure_details["stdout_tail"]:
        lines.append("\n### Process Output Snippet")
        lines.append("```text")
        lines.append(failure_details["stdout_tail"][-2000:])
        lines.append("```")

    lines.extend([
        "",
        "---",
        "## Agent Instructions for Self-Healing",
        "1. Fix the production code in `src/main/` so these gates pass.",
        "2. Do NOT modify tests, specs, harness scripts, or gate configuration files.",
        "3. Protected files that MUST NOT be edited:",
    ])
    for item in MUST_NOT_CHANGE:
        lines.append(f"   - `{item}`")
    lines.append("")
    lines.append("If this failure reveals a genuine defect in the specification, report it instead of hacking the test.")
    return "\n".join(lines)
