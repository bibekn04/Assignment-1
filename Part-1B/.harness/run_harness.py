#!/usr/bin/env python3
"""
Single Non-Deterministic Evaluation Harness Runner.
Part 1B - SE* ZG534

Executes the 16-case benchmark dataset against the Code Review Assistant,
applies Layer 1 (Structural), Layer 2 (Invariant), Layer 3 (Semantic) evaluation,
checks Constitution P12 thresholds, and outputs evidence/eval-results.json.
"""

import sys
import os
import json
import time
import urllib.request
import urllib.error
from datetime import datetime

# Import local evaluators
sys.path.insert(0, os.path.dirname(__file__))
from evaluator import evaluate_layer1_structural, evaluate_layer2_invariants, evaluate_layer3_semantic

DATASET_PATH = os.path.join(os.path.dirname(__file__), "..", "evals", "dataset.json")
RESULTS_PATH = os.path.join(os.path.dirname(__file__), "..", "evidence", "eval-results.json")
API_ENDPOINT = "http://localhost:8080/api/v1/review"

# Constitution Principle P12 Declared Thresholds
THRESHOLD_SCHEMA_PASS_RATE = 1.00     # 100%
THRESHOLD_INVARIANT_PASS_RATE = 1.00  # 100%
THRESHOLD_SEMANTIC_MEAN = 3.50        # >= 3.5 / 5.0
THRESHOLD_SECURITY_DETECTION = 0.90   # >= 90%
THRESHOLD_SECRET_ECHO = 0             # 0 leaks

def call_service(case):
    payload = {
        "code": case["input_code"],
        "intent": case.get("intent", "BOTH"),
        "language": case.get("language", "java"),
        "context": case.get("context", "")
    }
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(API_ENDPOINT, data=data, headers={"Content-Type": "application/json"})
    
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))

def main():
    print("=" * 80)
    print("  PART 1B: NON-DETERMINISTIC AI EVALUATION HARNESS")
    print("  Application: Semantic Code Review & Refactoring Assistant")
    print("=" * 80)

    if not os.path.exists(DATASET_PATH):
        print(f"Error: dataset file not found at {DATASET_PATH}")
        sys.exit(1)

    with open(DATASET_PATH, "r", encoding="utf-8") as f:
        benchmark = json.load(f)

    cases = benchmark.get("cases", [])
    total_cases = len(cases)
    print(f"Loaded {total_cases} benchmark test cases from {DATASET_PATH}")
    print(f"Connecting to service endpoint: {API_ENDPOINT}\n")

    results = []
    layer1_passes = 0
    layer2_passes = 0
    layer3_scores = []
    security_expected = 0
    security_detected = 0
    secret_leaks = 0

    print(f"{'ID':<7} | {'Category':<22} | {'Layer 1 (Schema)':<16} | {'Layer 2 (Invar)':<15} | {'Layer 3 (Score)':<15} | Status")
    print("-" * 90)

    for case in cases:
        case_id = case["id"]
        category = case["category"]
        
        try:
            response = call_service(case)
        except Exception as e:
            print(f"{case_id:<7} | {category:<22} | FAILED (Service: {str(e)[:25]})")
            results.append({
                "id": case_id,
                "category": category,
                "error": str(e),
                "passed": False
            })
            continue

        # Layer 1
        l1_pass, l1_errs = evaluate_layer1_structural(response)
        if l1_pass:
            layer1_passes += 1
            
        # Layer 2
        l2_pass, l2_errs = evaluate_layer2_invariants(case, response)
        if l2_pass:
            layer2_passes += 1
            
        # Security & Secret stats
        if case.get("expected_invariants", {}).get("must_have_security_finding", False):
            security_expected += 1
            has_sec = any(f.get("category") == "SECURITY" for f in response.get("findings", []))
            if has_sec:
                security_detected += 1
                
        if case.get("expected_invariants", {}).get("must_not_contain_secret"):
            secret = case["expected_invariants"]["must_not_contain_secret"]
            if secret in json.dumps(response):
                secret_leaks += 1
                
        # Layer 3
        l3_score, l3_notes = evaluate_layer3_semantic(case, response)
        layer3_scores.append(l3_score)

        case_passed = l1_pass and l2_pass and (l3_score >= 3.0)
        status_str = "PASS" if case_passed else "FAIL"

        l1_str = "PASS" if l1_pass else f"FAIL ({len(l1_errs)})"
        l2_str = "PASS" if l2_pass else f"FAIL ({len(l2_errs)})"
        l3_str = f"{l3_score:.1f} / 5.0"

        print(f"{case_id:<7} | {category:<22} | {l1_str:<16} | {l2_str:<15} | {l3_str:<15} | {status_str}")

        results.append({
            "id": case_id,
            "category": category,
            "layer1_structural": {"passed": l1_pass, "errors": l1_errs},
            "layer2_invariants": {"passed": l2_pass, "errors": l2_errs},
            "layer3_semantic": {"score": l3_score, "notes": l3_notes},
            "response_snapshot": response,
            "overall_case_pass": case_passed
        })

    # Summary Metrics
    l1_rate = (layer1_passes / total_cases) if total_cases > 0 else 0
    l2_rate = (layer2_passes / total_cases) if total_cases > 0 else 0
    l3_mean = (sum(layer3_scores) / len(layer3_scores)) if layer3_scores else 0
    sec_rate = (security_detected / security_expected) if security_expected > 0 else 1.0

    print("-" * 90)
    print("\nEVALUATION HARNESS AGGREGATE SUMMARY:")
    print(f"  • Total Test Cases:               {total_cases}")
    print(f"  • Layer 1 Schema Pass Rate:       {l1_rate*100:.1f}% (Declared Gate: 100%)")
    print(f"  • Layer 2 Invariant Pass Rate:    {l2_rate*100:.1f}% (Declared Gate: 100%)")
    print(f"  • Layer 3 Mean Semantic Score:    {l3_mean:.2f} / 5.0 (Declared Gate: >= 3.50)")
    print(f"  • Security Detection Rate:        {sec_rate*100:.1f}% (Declared Gate: >= 90%)")
    print(f"  • Secret Leaks Count:             {secret_leaks} (Declared Gate: 0)")

    # Aggregate PASS condition
    aggregate_pass = (
        l1_rate >= THRESHOLD_SCHEMA_PASS_RATE and
        l2_rate >= THRESHOLD_INVARIANT_PASS_RATE and
        l3_mean >= THRESHOLD_SEMANTIC_MEAN and
        sec_rate >= THRESHOLD_SECURITY_DETECTION and
        secret_leaks <= THRESHOLD_SECRET_ECHO
    )

    print("\n" + "=" * 80)
    if aggregate_pass:
        print("  FINAL HARNESS RESULT: >>> ALL GATES PASSED <<<")
    else:
        print("  FINAL HARNESS RESULT: >>> HARNESS FAILED GATES <<<")
    print("=" * 80 + "\n")

    os.makedirs(os.path.dirname(RESULTS_PATH), exist_ok=True)
    summary_output = {
        "timestamp": datetime.now().isoformat(),
        "total_cases": total_cases,
        "aggregate_pass": aggregate_pass,
        "metrics": {
            "layer1_schema_pass_rate": round(l1_rate, 4),
            "layer2_invariant_pass_rate": round(l2_rate, 4),
            "layer3_mean_semantic_score": round(l3_mean, 2),
            "security_detection_rate": round(sec_rate, 4),
            "secret_echo_count": secret_leaks
        },
        "thresholds": {
            "layer1_schema_required": THRESHOLD_SCHEMA_PASS_RATE,
            "layer2_invariant_required": THRESHOLD_INVARIANT_PASS_RATE,
            "layer3_semantic_required": THRESHOLD_SEMANTIC_MEAN,
            "security_detection_required": THRESHOLD_SECURITY_DETECTION,
            "secret_echo_max": THRESHOLD_SECRET_ECHO
        },
        "case_results": results
    }

    with open(RESULTS_PATH, "w", encoding="utf-8") as f:
        json.dump(summary_output, f, indent=2)

    print(f"Saved full evaluation results to: {RESULTS_PATH}")
    return 0 if aggregate_pass else 1

if __name__ == "__main__":
    sys.exit(main())
