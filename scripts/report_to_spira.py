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
    No mapping table to maintain here. Tag a scenario in its .feature file
    with the SpiraTeam Test Case ID it covers, e.g.:

        @smoke @negative @TC290
        Scenario Outline: Invalid Admin login shows an error

    This script reads that tag straight out of the Cucumber JSON report and
    reports to that test case automatically. A Scenario Outline's Examples
    rows all carry the same tag, so they're aggregated into one overall
    pass/fail for that one test case. A scenario with no @TC### tag is
    simply skipped (not reported anywhere) -- tag it when you're ready to
    track it in SpiraTeam.

Release/Sprint:
    CURRENT_RELEASE_ID below tags every reported Test Run against a
    SpiraTeam Release/Sprint (the number in that Release's URL, e.g.
    .../Release/154.aspx -> 154), so SpiraTeam's Test Coverage % for that
    sprint updates automatically. Update this constant at the start of
    each new sprint. Set it to None to stop tagging a release at all.
"""

import json
import os
import re
import sys
import urllib.request
import urllib.error
import urllib.parse
from datetime import datetime, timedelta, timezone

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
CONFIG_PATH = os.path.join(PROJECT_ROOT, "src", "test", "resources", "Config", "spira.properties")
CUCUMBER_JSON_PATH = os.path.join(PROJECT_ROOT, "target", "Cucumber.json")

# Matches a Gherkin tag like @TC290 and captures the number.
TEST_CASE_TAG_PATTERN = re.compile(r"^@TC(\d+)$", re.IGNORECASE)

# Update this at the start of each new sprint (see "Release/Sprint" above).
CURRENT_RELEASE_ID = 154  # Sprint 1


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


def extract_test_case_ids(element):
    """Returns the list of SpiraTeam Test Case IDs tagged on this scenario
    element, e.g. a tag @TC290 on it returns [290]. Usually zero or one,
    but a scenario can carry more than one @TC### tag if it covers
    multiple test cases."""
    ids = []
    for tag in element.get("tags", []):
        match = TEST_CASE_TAG_PATTERN.match(tag.get("name", ""))
        if match:
            ids.append(int(match.group(1)))
    return ids


def summarize_scenarios(features):
    """
    Groups Cucumber JSON scenario elements by the SpiraTeam Test Case ID
    found in their @TC### tag. A Scenario Outline's Examples rows all
    carry the same tag, so they're aggregated into one overall pass/fail
    per test case. Scenarios with no @TC### tag are skipped entirely.
    """
    summaries = {}
    for feature in features:
        feature_name = feature.get("name", "")
        for element in feature.get("elements", []):
            if element.get("type") != "scenario":
                continue
            test_case_ids = extract_test_case_ids(element)
            if not test_case_ids:
                continue
            scenario_name = element.get("name", "")
            for test_case_id in test_case_ids:
                summary = summaries.setdefault(test_case_id, {
                    "feature_name": feature_name,
                    "scenario_name": scenario_name,
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
    """Returns True if the result was successfully recorded (or this was a
    dry run), False if the actual API call failed."""
    start = datetime.now(timezone.utc)
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

    if CURRENT_RELEASE_ID is not None:
        body["ReleaseId"] = CURRENT_RELEASE_ID

    if dry_run:
        print(f"[DRY RUN] [TC:{test_case_id}] {scenario_name} -> "
              f"{'PASSED' if passed else 'FAILED'}")
        print(json.dumps(body, indent=2))
        return True

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
            return True
    except urllib.error.HTTPError as e:
        print(f"[TC:{test_case_id}] {scenario_name} -> FAILED TO REPORT "
              f"(HTTP {e.code}): {e.read().decode('utf-8', errors='replace')}")
        return False
    except urllib.error.URLError as e:
        print(f"[TC:{test_case_id}] {scenario_name} -> FAILED TO REPORT (connection error): {e}")
        return False


def main():
    dry_run = "--dry-run" in sys.argv

    base_url, project_id, username, api_key = load_config()
    report = load_cucumber_results(CUCUMBER_JSON_PATH)
    summaries = summarize_scenarios(report)

    if not summaries:
        print("No @TC###-tagged scenarios found in this Cucumber report -- nothing to report. "
              "Tag a scenario (e.g. @TC290) in its .feature file to have it reported.")
        sys.exit(1)

    any_api_failure = False
    for test_case_id, summary in summaries.items():
        ok = record_test_run(
            base_url, project_id, username, api_key,
            test_case_id, summary["scenario_name"],
            summary["passed"], summary["duration_ns"], summary["failures"],
            summary["example_count"], dry_run,
        )
        if not ok:
            any_api_failure = True

    # Exit non-zero if the API itself rejected a report -- a 200 from
    # Spira's API is not the same thing as the underlying test passing,
    # so don't let an API-level failure hide behind a green CI step.
    if any_api_failure:
        sys.exit(1)


if __name__ == "__main__":
    main()
