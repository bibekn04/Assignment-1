#!/usr/bin/env python3
"""
Three-Layer Evaluation Engine for Non-Deterministic AI Code Review Assistant.
Part 1B - SE* ZG534

Layer 1 — Structural Conformance (Schema, enums, required fields)
Layer 2 — Property & Invariant Validation (Clarification, security detection, secret masking, refactor diff)
Layer 3 — Semantic / Quality Evaluation (Rubric fidelity, keyword overlap, quality calibration)
"""

import json
import re
from typing import Dict, Any, Tuple, List

VALID_INTENTS = {"REVIEW", "REFACTOR", "BOTH"}
VALID_SEVERITIES = {"CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"}
VALID_CATEGORIES = {"SECURITY", "PERFORMANCE", "STYLE", "CORRECTNESS", "MAINTAINABILITY", "READABILITY", "CONCURRENCY"}
VALID_QUALITIES = {"POOR", "NEEDS_IMPROVEMENT", "ACCEPTABLE", "GOOD", "EXCELLENT"}

def evaluate_layer1_structural(response_data: Dict[str, Any]) -> Tuple[bool, List[str]]:
    """
    Layer 1: Structural Validation.
    Validates JSON syntax, presence of mandatory fields, valid types, and enumerations.
    """
    errors = []
    
    # Required top-level keys
    required_keys = ["intent", "summary", "overall_quality", "findings", "refactoring_suggestions", "clarification_needed"]
    for k in required_keys:
        if k not in response_data:
            errors.append(f"Missing required field: '{k}'")
            
    if errors:
        return False, errors
        
    # Enums
    intent = response_data.get("intent")
    if intent not in VALID_INTENTS:
        errors.append(f"Invalid intent enum: '{intent}'")
        
    quality = response_data.get("overall_quality")
    if quality not in VALID_QUALITIES:
        errors.append(f"Invalid overall_quality enum: '{quality}'")
        
    # Type checks
    if not isinstance(response_data.get("summary", ""), str):
        errors.append("'summary' must be a string")
        
    if not isinstance(response_data.get("clarification_needed"), bool):
        errors.append("'clarification_needed' must be a boolean")
        
    findings = response_data.get("findings", [])
    if not isinstance(findings, list):
        errors.append("'findings' must be a list")
    else:
        for idx, f in enumerate(findings):
            if not isinstance(f, dict):
                errors.append(f"Finding [{idx}] must be an object")
                continue
            for fk in ["id", "severity", "category", "title", "description", "recommendation"]:
                if fk not in f:
                    errors.append(f"Finding [{idx}] missing field: '{fk}'")
            if f.get("severity") not in VALID_SEVERITIES:
                errors.append(f"Finding [{idx}] invalid severity: '{f.get('severity')}'")
            if f.get("category") not in VALID_CATEGORIES:
                errors.append(f"Finding [{idx}] invalid category: '{f.get('category')}'")

    refactorings = response_data.get("refactoring_suggestions", [])
    if not isinstance(refactorings, list):
        errors.append("'refactoring_suggestions' must be a list")
    else:
        for idx, r in enumerate(refactorings):
            if not isinstance(r, dict):
                errors.append(f"Refactoring [{idx}] must be an object")
                continue
            for rk in ["id", "title", "rationale", "before_snippet", "after_snippet"]:
                if rk not in r:
                    errors.append(f"Refactoring [{idx}] missing field: '{rk}'")
                    
    return len(errors) == 0, errors

def evaluate_layer2_invariants(case: Dict[str, Any], response_data: Dict[str, Any]) -> Tuple[bool, List[str]]:
    """
    Layer 2: Property & Invariant Validation.
    Validates deterministic rules independent of surface phrasing.
    """
    errors = []
    expected_inv = case.get("expected_invariants", {})
    
    # Invariant: Clarification
    if expected_inv.get("clarification_needed", False):
        if not response_data.get("clarification_needed", False):
            errors.append("Expected clarification_needed=True for missing/malformed input, but was False")
        if expected_inv.get("has_clarification_message", False):
            msg = response_data.get("clarification_message")
            if not msg or not str(msg).strip():
                errors.append("Expected non-empty clarification_message, but was empty/null")
    else:
        if response_data.get("clarification_needed", False):
            errors.append("Unexpected clarification_needed=True for valid code")

    # Invariant: Security Flaw Detection
    if expected_inv.get("must_have_security_finding", False):
        findings = response_data.get("findings", [])
        has_sec = any(f.get("category") == "SECURITY" and f.get("severity") in {"CRITICAL", "HIGH"} for f in findings)
        if not has_sec:
            errors.append("Expected at least one CRITICAL/HIGH SECURITY finding, but none found")

    # Invariant: Secret Non-Echo
    secret_to_check = expected_inv.get("must_not_contain_secret")
    if secret_to_check:
        full_json_str = json.dumps(response_data)
        if secret_to_check in full_json_str:
            errors.append(f"CRITICAL VIOLATION: Secret value echoed verbatim in output! (P6 Secret Non-Echo breached)")

    # Invariant: Refactoring Coherence (before != after)
    if expected_inv.get("refactoring_must_differ", False):
        refactorings = response_data.get("refactoring_suggestions", [])
        for r in refactorings:
            before = (r.get("before_snippet") or "").strip()
            after = (r.get("after_snippet") or "").strip()
            if before == after:
                errors.append(f"Refactoring suggestion '{r.get('id')}' has identical before and after snippets (P8 breached)")

    # Invariant: Prohibited Qualities
    prohibited_qualities = expected_inv.get("prohibited_qualities", [])
    if response_data.get("overall_quality") in prohibited_qualities:
        errors.append(f"overall_quality '{response_data.get('overall_quality')}' is prohibited for this defect category")

    return len(errors) == 0, errors

def evaluate_layer3_semantic(case: Dict[str, Any], response_data: Dict[str, Any]) -> Tuple[float, List[str]]:
    """
    Layer 3: Semantic Quality Evaluation.
    Evaluates semantic coverage of expected domain keywords, recommendations, and quality calibration.
    Returns (score_out_of_5, notes).
    """
    notes = []
    score = 5.0
    
    # 1. Summary quality
    summary = response_data.get("summary", "")
    if len(summary.strip()) < 15:
        score -= 1.0
        notes.append("Summary is excessively brief (<15 chars)")
        
    # 2. Expected keywords / semantic concept presence
    expected_keywords = case.get("expected_keywords", [])
    if expected_keywords:
        full_text = json.dumps(response_data).lower()
        matched_kw = [kw for kw in expected_keywords if kw.lower() in full_text]
        match_ratio = len(matched_kw) / len(expected_keywords)
        
        if match_ratio < 0.25:
            score -= 2.0
            notes.append(f"Low domain keyword match ({len(matched_kw)}/{len(expected_keywords)}): {matched_kw}")
        elif match_ratio < 0.50:
            score -= 1.0
            notes.append(f"Moderate domain keyword match ({len(matched_kw)}/{len(expected_keywords)}): {matched_kw}")
        else:
            notes.append(f"Strong domain keyword match ({len(matched_kw)}/{len(expected_keywords)})")
            
    # 3. Actionable recommendation verification
    findings = response_data.get("findings", [])
    if findings:
        empty_recs = [f.get("id") for f in findings if len((f.get("recommendation") or "").strip()) < 10]
        if empty_recs:
            score -= 1.0
            notes.append(f"Findings {empty_recs} have inadequate recommendations (<10 chars)")
            
    score = max(1.0, min(5.0, score))
    return round(score, 2), notes
