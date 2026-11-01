#!/usr/bin/env python3
"""
smoke_test.py: Starts the microservice, performs live HTTP smoke tests, and records real server telemetry.
"""
import subprocess
import time
import urllib.request
import urllib.error
import os
import sys
from datetime import datetime

def main():
    project_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    jar_path = os.path.join(project_dir, "target", "tiered-rate-limiter-1.0.0.jar")

    if not os.path.exists(jar_path):
        print(f"JAR not found: {jar_path}")
        sys.exit(1)

    print(f"Launching microservice: {jar_path}")
    server_proc = subprocess.Popen(
        ["java", "-jar", jar_path],
        cwd=project_dir,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT
    )

    # Wait for server ready
    server_ready = False
    for attempt in range(30):
        try:
            req = urllib.request.Request("http://localhost:8080/health")
            with urllib.request.urlopen(req, timeout=1) as resp:
                if resp.status == 200:
                    server_ready = True
                    break
        except Exception:
            time.sleep(1)

    log = []
    log.append("============================================================")
    log.append(" LIVE HTTP SERVER SMOKE TEST EXECUTION PROOF")
    log.append(" Microservice: edu.sezg534.ratelimiter (Port 8080)")
    log.append(f" Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    log.append("============================================================\n")

    if not server_ready:
        log.append("ERROR: Spring Boot server failed to initialize within 30 seconds.")
    else:
        # 1. Health check
        req = urllib.request.Request("http://localhost:8080/health")
        with urllib.request.urlopen(req) as resp:
            body = resp.read().decode("utf-8")
            log.append("[1] GET /health (Unconstrained health check):")
            log.append(f"    HTTP Status   : {resp.status} OK")
            log.append(f"    Response Body : {body}")
        log.append("")

        # 2. Missing key
        try:
            req = urllib.request.Request("http://localhost:8080/api/resource")
            urllib.request.urlopen(req)
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8")
            log.append("[2] GET /api/resource (Missing X-API-Key header):")
            log.append(f"    HTTP Status   : {e.code} Unauthorized")
            log.append(f"    Response Body : {body}")
        log.append("")

        # 3. Unknown key
        try:
            req = urllib.request.Request("http://localhost:8080/api/resource", headers={"X-API-Key": "key_unknown_999"})
            urllib.request.urlopen(req)
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8")
            log.append("[3] GET /api/resource (Unknown X-API-Key: 'key_unknown_999'):")
            log.append(f"    HTTP Status   : {e.code} Unauthorized")
            log.append(f"    Response Body : {body}")
        log.append("")

        # 4. Free tier burst (capacity = 5)
        log.append("[4] Consecutive burst requests for Free Tier (key_free_123, capacity=5):")
        for i in range(1, 8):
            req = urllib.request.Request("http://localhost:8080/api/resource", headers={"X-API-Key": "key_free_123"})
            try:
                with urllib.request.urlopen(req) as resp:
                    lim = resp.headers.get("X-RateLimit-Limit")
                    rem = resp.headers.get("X-RateLimit-Remaining")
                    res = resp.headers.get("X-RateLimit-Reset")
                    log.append(f"    Request {i}: HTTP 200 OK | Limit: {lim} | Remaining: {rem} | Reset: {res}")
            except urllib.error.HTTPError as e:
                lim = e.headers.get("X-RateLimit-Limit")
                rem = e.headers.get("X-RateLimit-Remaining")
                retry = e.headers.get("Retry-After")
                log.append(f"    Request {i}: HTTP {e.code} Too Many Requests (THROTTLED) | Remaining: {rem} | Retry-After: {retry}s")

    # Terminate server
    server_proc.terminate()
    try:
        server_proc.wait(timeout=5)
    except subprocess.TimeoutExpired:
        server_proc.kill()

    out_dir = os.path.join(project_dir, "evidence", "final-generation", "reports")
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, "live-http-smoke-test.log")
    with open(out_file, "w", encoding="utf-8") as f:
        f.write("\n".join(log))

    print("\n".join(log))
    print(f"\nSaved live smoke test proof to: {out_file}")

if __name__ == "__main__":
    main()
