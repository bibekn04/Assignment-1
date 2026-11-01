# Quickstart Guide

## Prerequisites
* **Java 17 (LTS)** (`java -version`, `javac -version`)
* **Python 3.10+** (`python --version`)

## Running the Application
From `Part-1A/`:
```bash
./mvnw spring-boot:run
```

## Running the Deterministic Harness
To run all 5 verification gates independently:
```bash
python .harness/run_harness.py
```

## Manual Verification via `curl`
```bash
# Health check (unlimited)
curl -i http://localhost:8080/health

# Authorized request (Free Tier)
curl -i -H "X-API-Key: key_free_123" http://localhost:8080/api/resource

# Missing Key (401)
curl -i http://localhost:8080/api/resource
```
