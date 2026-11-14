# Data Model & Schema Contract
**Part 1B: Semantic Code Review & Refactoring Assistant**

---

## 1. Input Request Schema (`ReviewRequest`)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ReviewRequest",
  "type": "object",
  "required": ["code"],
  "properties": {
    "code": {
      "type": "string",
      "description": "Source code or diff to be reviewed/refactored."
    },
    "intent": {
      "type": "string",
      "enum": ["REVIEW", "REFACTOR", "BOTH"],
      "default": "BOTH",
      "description": "Desired objective of the assistance."
    },
    "language": {
      "type": "string",
      "default": "java",
      "description": "Programming language (e.g. java, python, javascript, sql)."
    },
    "context": {
      "type": "string",
      "description": "Optional background or design context."
    }
  }
}
```

---

## 2. Output Response Schema Contract (`ReviewResponse`)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ReviewResponse",
  "type": "object",
  "required": [
    "intent",
    "summary",
    "overall_quality",
    "findings",
    "refactoring_suggestions",
    "clarification_needed"
  ],
  "properties": {
    "intent": {
      "type": "string",
      "enum": ["REVIEW", "REFACTOR", "BOTH"]
    },
    "summary": {
      "type": "string",
      "description": "High-level summary of findings or refactoring advice."
    },
    "overall_quality": {
      "type": "string",
      "enum": ["POOR", "NEEDS_IMPROVEMENT", "ACCEPTABLE", "GOOD", "EXCELLENT"]
    },
    "findings": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["id", "severity", "category", "line_range", "title", "description", "recommendation"],
        "properties": {
          "id": { "type": "string" },
          "severity": {
            "type": "string",
            "enum": ["CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"]
          },
          "category": {
            "type": "string",
            "enum": ["SECURITY", "PERFORMANCE", "STYLE", "CORRECTNESS", "MAINTAINABILITY", "READABILITY"]
          },
          "line_range": { "type": "string" },
          "title": { "type": "string" },
          "description": { "type": "string" },
          "recommendation": { "type": "string" }
        }
      }
    },
    "refactoring_suggestions": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["id", "title", "rationale", "before_snippet", "after_snippet"],
        "properties": {
          "id": { "type": "string" },
          "title": { "type": "string" },
          "rationale": { "type": "string" },
          "before_snippet": { "type": "string" },
          "after_snippet": { "type": "string" }
        }
      }
    },
    "clarification_needed": { "type": "boolean" },
    "clarification_message": { "type": ["string", "null"] }
  }
}
```
