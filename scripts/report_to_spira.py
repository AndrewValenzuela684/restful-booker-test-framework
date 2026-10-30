#!/usr/bin/env python3
"""
report_to_spira.py

Reads the Cucumber JSON report produced by this project's test runs
(target/Cucumber.json) and reports pass/fail results back to SpiraTeam
against whichever test case each scenario is linked to, using SpiraTeam's
REST API (projects/{project_id}/test-runs/record).

Usage:
    mvn test                          # or -Dtest=SmokeRunner, however you run it
    python scripts/report_to_spira.py           # reports results
    python scripts/report_to_spira.py --dry-run # prints what WOULD be sent, no API call

Config:
    Reads connection details from
    src/test/resources/Config/spira.properties
    (spira.baseUrl / spira.projectId / spira.username / spira.apiKey).
    Any of these can be overridden with environment variables
    SPIRA_BASE_URL / SPIRA_PROJECT_ID / SPIRA_USERNAME / SPIRA_API_KEY.

Mapping:
    Add a new entry to SCENARIO_TO_TESTCASE below every time you create a
    new test case in SpiraTeam for a scenario in this project. The key is
    (Feature name, Scenario/Scenario Outline name) exactly as written in
    the .feature file; the value is the numeric SpiraTeam Test Case ID
    (the number in [TC:xxx]).
"""

import json
import os
import sys
import urllib.request
import urllib.error
import urllib.parse
from datetime import datetime, timedelta

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
CONFIG_PATH = os.path.join(PROJECT_ROOT, "src", "test", "resources", "Config", "spira.properties")
CUCUMBER_JSON_PATH = os.path.join(PROJECT_ROOT, "target", "Cucumber.json")

# (Feature name, Scenario name) -> SpiraTeam Test Case ID.
# A Scenario Outline's Examples rows all share one name here, so they get
# aggregated into a single pass/fail result for that one test case.
SCENARIO_TO_TESTCASE = {
    ("Login Functionalities", "Invalid Admin login shows an error"): 290,
}


def load_config():
    config = {}
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, "r") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                key, value = line.split("=", 1)
                config[key.strip()] = value.strip()

    base_url = os.environ.get("SPIRA_BASE_URL", config.get("spira.baseUrl"))
    project_id = os.environ.get("SPIRA_PROJECT_ID", config.get("spira.projectId"))
    username = os.environ.get("SPIRA_USERNAME", config.get("spira.username"))
    api_key = os.environ.get("SPIRA_API_KEY", config.get("spira.apiKey"))

    missing = [name for name, val in [
        ("base_url", base_url), ("project_id", project_id),
        ("username", username), ("api_key", api_key),
    ] if not val]
    if missing:
        sys.exit(
            f"Missing Spira config: {', '.join(missing)}. "
            f"Set them in {CONFIG_PATH} or as environment variables."
        )

    return base_url.rstrip("/"), int(project_id), username, api_key


def load_cucumber_results(path):
    if not os.path.exists(path):
        sys.exit(f"Cucumber report not found at {path}. Run the tests first (e.g. mvn test).")
    with open(path, "r") as f:
        return json.load(f)


def summarize_scenarios(features):
    """
    Groups Cucumber JSON scenario elements by (feature name, scenario name).
    A Scenario Outline's Examples rows all share the same name, so this
    aggregates them into one overall pass/fail per named scenario.
    """
    summaries = {}
    for feature in features:
        feature_name = feature.get("name", "")
        for element in feature.get("elements", []):
            if element.get("type") != "scenario":
                continue
            scenario_name = element.get("name", "")
            key = (feature_name, scenario_name)
            summary = summaries.setdefault(key, {
                "passed": True,
                "duration_ns": 0,
                "failures": [],
                "example_count": 0,
            })
            summary["example_count"] += 1
            for step in element.get("steps", []):
                result = step.get("result", {})
                summary["duration_ns"] += result.get("duration", 0) or 0
                status = result.get("status")
                if status != "passed":
                    summary["passed"] = False
                    err = result.get("error_message", "")
                    first_line = err.splitlines()[0] if err else status
                    summary["failures"].append(
                        f"{step.get('keyword', '').strip()} {step.get('name', '')} -> {first_line}"
                    )
    return summaries


def record_test_run(base_url, project_id, username, api_key,
                     test_case_id, scenario_name, passed, duration_ns,
                     failures, example_count, dry_run):
    start = datetime.utcnow()
    end = start + timedelta(seconds=duration_ns / 1e9)

    if passed:
        message = f"All {example_count} example row(s) passed." if example_count > 1 else "Passed."
        stack_trace = "No failures."
    else:
        message = f"{len(failures)} failing step(s) across {example_count} example row(s)."
        stack_trace = "\n".join(failures) or "See CI logs for details."

    body = {
        "TestRunFormatId": 1,  # 1 = Plain Text
        "RunnerName": "Cucumber-JUnit",
        "RunnerTestName": scenario_name,
        "RunnerAssertCount": len(failures),
        "RunnerMessage": message,
        "RunnerStackTrace": stack_trace,
        "TestCaseId": test_case_id,
        "ExecutionStatusId": 2 if passed else 1,  # 2 = Passed, 1 = Failed
        "StartDate": start.strftime("%Y-%m-%dT%H:%M:%S"),
        "EndDate": end.strftime("%Y-%m-%dT%H:%M:%S"),
        "ProjectId": project_id,
    }

    if dry_run:
        print(f"[DRY RUN] [TC:{test_case_id}] {scenario_name} -> "
              f"{'PASSED' if passed else 'FAILED'}")
        print(json.dumps(body, indent=2))
        return

    url = (
        f"{base_url}/Services/v7_0/RestService.svc/projects/{project_id}/test-runs/record"
        f"?username={urllib.parse.quote(username)}&api-key={urllib.parse.quote(api_key)}"
    )

    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, method="POST", headers={
        "Content-Type": "application/json",
        "Accept": "application/json",
    })

    try:
        with urllib.request.urlopen(req) as resp:
            print(f"[TC:{test_case_id}] {scenario_name} -> {'PASSED' if passed else 'FAILED'} "
                  f"(reported, HTTP {resp.status})")
    except urllib.error.HTTPError as e:
        print(f"[TC:{test_case_id}] {scenario_name} -> FAILED TO REPORT "
              f"(HTTP {e.code}): {e.read().decode('utf-8', errors='replace')}")
    except urllib.error.URLError as e:
        print(f"[TC:{test_case_id}] {scenario_name} -> FAILED TO REPORT (connection error): {e}")


def main():
    dry_run = "--dry-run" in sys.argv

    base_url, project_id, username, api_key = load_config()
    report = load_cucumber_results(CUCUMBER_JSON_PATH)
    summaries = summarize_scenarios(report)

    reported = 0
    for (feature_name, scenario_name), test_case_id in SCENARIO_TO_TESTCASE.items():
        summary = summaries.get((feature_name, scenario_name))
        if summary is None:
            print(f"[TC:{test_case_id}] No results found for '{scenario_name}' in "
                  f"'{feature_name}' -- did it run in this test pass? "
                  f"(check the exact name against Login.feature)")
            continue
        record_test_run(
            base_url, project_id, username, api_key,
            test_case_id, scenario_name,
            summary["passed"], summary["duration_ns"], summary["failures"],
            summary["example_count"], dry_run,
        )
        reported += 1

    if reported == 0:
        sys.exit(1)


if __name__ == "__main__":
    main()
