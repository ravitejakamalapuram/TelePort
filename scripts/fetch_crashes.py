#!/usr/bin/env python3
"""
fetch_crashes.py — Fetch crash data from Firebase Crashlytics.

This script queries the Firebase Crashlytics REST API (v1beta1) for recent
crash issues and outputs a structured JSON array to stdout.  It authenticates
via a Google Cloud service-account key whose path is supplied through the
GOOGLE_APPLICATION_CREDENTIALS environment variable.

If the REST API is unavailable (e.g. the API is not enabled in the GCP
project), the script falls back to the Firebase Admin SDK.

Usage:
    python scripts/fetch_crashes.py \
        --project-id my-firebase-project \
        --hours 24 \
        --min-events 5
"""

from __future__ import annotations

import argparse
import json
import logging
import os
import sys
from datetime import datetime, timedelta, timezone
from typing import Any

# ---------------------------------------------------------------------------
# Dependency check — give a helpful message instead of a raw ImportError.
# ---------------------------------------------------------------------------
_MISSING_DEPS: list[str] = []

try:
    from google.oauth2 import service_account as google_sa  # type: ignore
    import google.auth.transport.requests as google_requests  # type: ignore
except ImportError:
    _MISSING_DEPS.append("google-auth")

try:
    import requests  # type: ignore
except ImportError:
    _MISSING_DEPS.append("requests")

if _MISSING_DEPS:
    sys.stderr.write(
        "ERROR: Missing required Python packages: "
        + ", ".join(_MISSING_DEPS)
        + "\n\n"
        "Install them with:\n"
        "  pip install google-auth google-auth-httplib2 requests\n"
    )
    sys.exit(1)

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%dT%H:%M:%S%z",
    stream=sys.stderr,  # keep stdout clean for JSON output
)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
CRASHLYTICS_API_BASE = "https://firebasecrashlytics.googleapis.com/v1beta1"
SCOPES = [
    "https://www.googleapis.com/auth/firebase.readonly",
    "https://www.googleapis.com/auth/cloud-platform",
]


# ===================================================================
# REST API approach
# ===================================================================

def _get_access_token(sa_path: str) -> str:
    """Return an OAuth2 access token from a service-account JSON key."""
    credentials = google_sa.Credentials.from_service_account_file(
        sa_path, scopes=SCOPES
    )
    credentials.refresh(google_requests.Request())
    return credentials.token  # type: ignore[return-value]


def _rest_fetch_issues(
    project_id: str,
    access_token: str,
    since: datetime,
    min_events: int,
) -> list[dict[str, Any]]:
    """Fetch open crash issues from the Crashlytics REST API."""
    url = f"{CRASHLYTICS_API_BASE}/projects/{project_id}/issues"
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json",
    }

    # Build a filter string for the API.
    # The v1beta1 API accepts RFC 3339 timestamps and field filters.
    since_rfc3339 = since.strftime("%Y-%m-%dT%H:%M:%SZ")
    filter_str = (
        f'eventType="FATAL" AND state="OPEN" AND '
        f'lastSeenTime>="{since_rfc3339}"'
    )
    params: dict[str, Any] = {
        "filter": filter_str,
        "pageSize": 100,
    }

    issues: list[dict[str, Any]] = []
    next_page_token: str | None = None

    while True:
        if next_page_token:
            params["pageToken"] = next_page_token

        logger.info("Requesting issues from Crashlytics REST API …")
        resp = requests.get(url, headers=headers, params=params, timeout=30)
        resp.raise_for_status()
        data = resp.json()

        for issue in data.get("issues", []):
            event_count = int(issue.get("eventCount", 0))
            if event_count >= min_events:
                issues.append(issue)

        next_page_token = data.get("nextPageToken")
        if not next_page_token:
            break

    return issues


def _rest_fetch_event_details(
    project_id: str,
    issue_id: str,
    access_token: str,
) -> dict[str, Any]:
    """Fetch the latest event (including stack trace) for an issue."""
    url = (
        f"{CRASHLYTICS_API_BASE}/projects/{project_id}"
        f"/issues/{issue_id}/events"
    )
    headers = {"Authorization": f"Bearer {access_token}"}
    params = {"pageSize": 1}  # latest event only

    resp = requests.get(url, headers=headers, params=params, timeout=30)
    resp.raise_for_status()
    events = resp.json().get("events", [])
    return events[0] if events else {}


def _extract_stack_trace(event: dict[str, Any]) -> str:
    """Best-effort extraction of the stack trace from an event payload."""
    # The REST API nests the trace under different keys depending on the
    # platform.  We try the most common paths.
    for path in (
        ("crashEvent", "stackTrace"),
        ("stackTrace",),
        ("exceptionInfo", "stackTrace"),
    ):
        obj = event
        for key in path:
            obj = obj.get(key, {}) if isinstance(obj, dict) else {}
        if isinstance(obj, str) and obj:
            return obj

    # Sometimes the trace is stored as structured frames — flatten them.
    frames = event.get("crashEvent", {}).get("frames", [])
    if frames:
        lines = []
        for f in frames:
            file_name = f.get("file", "<unknown>")
            line_no = f.get("line", "?")
            symbol = f.get("symbol", "")
            lines.append(f"  at {symbol} ({file_name}:{line_no})")
        return "\n".join(lines)

    # Last resort: pretty-print the whole event so the caller still gets
    # something useful.
    return json.dumps(event, indent=2)


def _extract_device_info(event: dict[str, Any]) -> dict[str, str]:
    """Extract device model and OS version from an event."""
    device = event.get("deviceInfo", event.get("device", {}))
    return {
        "model": device.get("model", "unknown"),
        "os_version": device.get("osVersion", device.get("androidVersion", "unknown")),
    }


def fetch_via_rest_api(
    project_id: str,
    hours: int,
    min_events: int,
) -> list[dict[str, Any]]:
    """Primary path — use the Crashlytics REST API."""
    sa_path = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS", "")
    if not sa_path or not os.path.isfile(sa_path):
        raise EnvironmentError(
            "GOOGLE_APPLICATION_CREDENTIALS is not set or the file does not "
            "exist.  Point it at your service-account JSON key file."
        )

    access_token = _get_access_token(sa_path)
    since = datetime.now(timezone.utc) - timedelta(hours=hours)

    raw_issues = _rest_fetch_issues(project_id, access_token, since, min_events)
    logger.info("Fetched %d issue(s) matching the filter.", len(raw_issues))

    results: list[dict[str, Any]] = []
    for issue in raw_issues:
        issue_id = issue.get("issueId", issue.get("name", "").split("/")[-1])
        logger.info("Fetching event details for issue %s …", issue_id)

        event = _rest_fetch_event_details(project_id, issue_id, access_token)

        results.append(
            {
                "issue_id": issue_id,
                "title": issue.get("title", ""),
                "subtitle": issue.get("subtitle", ""),
                "event_count": int(issue.get("eventCount", 0)),
                "affected_users": int(issue.get("distinctUsersCount", 0)),
                "first_seen": issue.get("firstSeenTime", ""),
                "last_seen": issue.get("lastSeenTime", ""),
                "stack_trace": _extract_stack_trace(event),
                "device_info": _extract_device_info(event),
            }
        )

    return results


# ===================================================================
# Firebase Admin SDK fallback
# ===================================================================

def fetch_via_admin_sdk(
    project_id: str,
    hours: int,
    min_events: int,
) -> list[dict[str, Any]]:
    """Fallback — use the Firebase Admin SDK if the REST API is unavailable.

    NOTE:  As of 2025 the Admin SDK does not expose Crashlytics data
    directly.  This function attempts to use the underlying Google Cloud
    APIs through the Admin SDK's credential management, then falls back to
    returning an empty list with a descriptive warning.
    """
    try:
        import firebase_admin  # type: ignore
        from firebase_admin import credentials as fb_credentials  # type: ignore
    except ImportError:
        logger.error(
            "firebase-admin is not installed.  Install it with:\n"
            "  pip install firebase-admin\n"
            "Alternatively, enable the Crashlytics REST API in your GCP "
            "project and re-run this script."
        )
        return []

    sa_path = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS", "")
    if not sa_path or not os.path.isfile(sa_path):
        logger.error(
            "GOOGLE_APPLICATION_CREDENTIALS is not set or the file does not "
            "exist.  A service-account key is required for the Admin SDK "
            "fallback as well."
        )
        return []

    # Initialize the Admin SDK (idempotent).
    try:
        firebase_admin.get_app()
    except ValueError:
        cred = fb_credentials.Certificate(sa_path)
        firebase_admin.initialize_app(cred, {"projectId": project_id})

    # The Admin SDK does not have a native Crashlytics module.  We attempt to
    # use its managed credentials to call the REST API anyway.
    logger.warning(
        "The Firebase Admin SDK does not expose a Crashlytics API.  "
        "Attempting to call the REST endpoint using Admin SDK credentials …"
    )

    try:
        from google.auth.transport.requests import AuthorizedSession  # type: ignore

        cred_obj = firebase_admin.get_app().credential.get_credential()
        session = AuthorizedSession(cred_obj)

        since = datetime.now(timezone.utc) - timedelta(hours=hours)
        since_rfc3339 = since.strftime("%Y-%m-%dT%H:%M:%SZ")
        url = f"{CRASHLYTICS_API_BASE}/projects/{project_id}/issues"
        params = {
            "filter": (
                f'eventType="FATAL" AND state="OPEN" AND '
                f'lastSeenTime>="{since_rfc3339}"'
            ),
            "pageSize": 100,
        }

        resp = session.get(url, params=params, timeout=30)
        resp.raise_for_status()
        raw_issues = resp.json().get("issues", [])

        results: list[dict[str, Any]] = []
        for issue in raw_issues:
            event_count = int(issue.get("eventCount", 0))
            if event_count < min_events:
                continue

            issue_id = issue.get("issueId", issue.get("name", "").split("/")[-1])
            results.append(
                {
                    "issue_id": issue_id,
                    "title": issue.get("title", ""),
                    "subtitle": issue.get("subtitle", ""),
                    "event_count": event_count,
                    "affected_users": int(issue.get("distinctUsersCount", 0)),
                    "first_seen": issue.get("firstSeenTime", ""),
                    "last_seen": issue.get("lastSeenTime", ""),
                    "stack_trace": "",
                    "device_info": {"model": "unknown", "os_version": "unknown"},
                }
            )
        return results

    except Exception as exc:
        logger.error("Admin SDK fallback also failed: %s", exc)
        logger.error(
            "To use this script you need to:\n"
            "  1. Enable the Firebase Crashlytics API in your GCP Console, OR\n"
            "  2. Ensure your service-account has the "
            "'firebasecrashlytics.issues.list' permission.\n"
            "  3. Install all required packages:\n"
            "       pip install google-auth google-auth-httplib2 requests "
            "firebase-admin"
        )
        return []


# ===================================================================
# CLI entry point
# ===================================================================

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fetch recent crash data from Firebase Crashlytics."
    )
    parser.add_argument(
        "--project-id",
        required=True,
        help="Firebase / GCP project ID (e.g. 'my-app-12345').",
    )
    parser.add_argument(
        "--hours",
        type=int,
        default=24,
        help="Look-back window in hours (default: 24).",
    )
    parser.add_argument(
        "--min-events",
        type=int,
        default=1,
        help="Minimum number of events for an issue to be included (default: 1).",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    logger.info(
        "Fetching crashes for project '%s' from the last %d hour(s) "
        "(min events: %d) …",
        args.project_id,
        args.hours,
        args.min_events,
    )

    # --- Primary: REST API ---------------------------------------------------
    try:
        crashes = fetch_via_rest_api(
            project_id=args.project_id,
            hours=args.hours,
            min_events=args.min_events,
        )
        logger.info("REST API returned %d crash(es).", len(crashes))
    except Exception as exc:
        logger.warning("REST API approach failed: %s", exc)
        logger.info("Falling back to Firebase Admin SDK …")

        # --- Fallback: Admin SDK ----------------------------------------------
        crashes = fetch_via_admin_sdk(
            project_id=args.project_id,
            hours=args.hours,
            min_events=args.min_events,
        )
        logger.info("Admin SDK fallback returned %d crash(es).", len(crashes))

    # --- Output ---------------------------------------------------------------
    json.dump(crashes, sys.stdout, indent=2, default=str)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
