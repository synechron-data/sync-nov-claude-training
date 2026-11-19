#!/usr/bin/env python3
"""
PreToolUse demo hook: shows the different ways a hook can respond.

Pick a mode by editing DEFAULT_MODE below, or set PRETOOL_MODE in the env:
  allow    - exit 0, no output: tool call proceeds normally (default)
  log      - log the stdin payload, then allow
  override - allow, but rewrite the tool input via updatedInput
  block    - exit 2 hard block; stderr is shown to Claude as the reason
  deny     - JSON permissionDecision 'deny' on stdout, exit 0
"""

import json
import os
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

DEFAULT_MODE = "allow"
MODE = os.environ.get("PRETOOL_MODE") or DEFAULT_MODE

LOG_FILE = Path(os.environ.get("CLAUDE_PROJECT_DIR", ".")) / ".claude" / "hook-logs" / "pretooluse-demo.log"


def log_message(message: str) -> None:
    try:
        LOG_FILE.parent.mkdir(parents=True, exist_ok=True)
        with LOG_FILE.open("a", encoding="utf-8") as f:
            f.write(message + "\n")
    except Exception:
        # logging must never change the hook's decision
        pass


def _kolkata_now() -> datetime:
    try:
        from zoneinfo import ZoneInfo
        return datetime.now(ZoneInfo("Asia/Kolkata"))
    except Exception:
        # zoneinfo/tzdata may be missing on Windows; India has no DST, so UTC+5:30 is exact
        return datetime.now(timezone(timedelta(hours=5, minutes=30)))


def log_payload(payload: dict) -> None:
    # same "YYYY-MM-DD HH:MM:SS" format as toLocaleString('sv-SE')
    time_stamp = _kolkata_now().strftime("%Y-%m-%d %H:%M:%S")
    log_message("\n" + "-" * 60)
    log_message(f"[{time_stamp}] mode={MODE} tool={payload.get('tool_name')}")
    log_message("STDIN PAYLOAD:")
    log_message(json.dumps(payload, indent=2, ensure_ascii=False))


def main() -> int:
    raw = sys.stdin.buffer.read().decode("utf-8", errors="replace")

    try:
        payload = json.loads(raw)
        if not isinstance(payload, dict):
            raise ValueError("payload is not a JSON object")
    except ValueError:
        if MODE in ("block", "deny"):
            sys.stderr.write("[pretooluse-demo] could not parse hook input; blocking.\n")
            return 2
        return 0

    # ------------------------------------ Allow and logging
    if MODE == "log":
        log_payload(payload)
        return 0

    # ------------------------------------ Allow and override with logging
    # permissionDecision 'allow' skips the permission prompt, so an 'ask'
    # rule (e.g. git push) would not be asked. Safe here only because the
    # command is replaced with a harmless echo.
    if MODE == "override":
        log_payload(payload)
        response = {
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "allow",
                "permissionDecisionReason": "[pretooluse-demo] hook allowed and overrode this tool call.",
                "updatedInput": {**(payload.get("tool_input") or {}), "command": "echo overridden by custom script"},
            }
        }
        log_message(json.dumps(response, indent=2, ensure_ascii=False))
        sys.stdout.write(json.dumps(response))
        return 0

    # ---------------------------------- Hard Block
    # Exit code 2 hard-blocks the tool call; Claude Code treats it as an error
    # and shows stderr output (if any) as the reason - no JSON response needed.
    if MODE == "block":
        log_payload(payload)
        log_message("BLOCKED: exit code 2 - see stderr for reason")
        sys.stderr.write("[pretooluse-demo] hook blocked this tool call.\n")
        return 2

    # ---------------------------------- Hard Block with Deny
    # Deny: rejects the tool call via structured JSON on stdout while exiting
    # cleanly (0). permissionDecisionReason is shown to Claude, explaining why
    # the call was denied - unlike exit 2, no stderr text is needed.
    if MODE == "deny":
        log_payload(payload)
        response = {
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "deny",
                "permissionDecisionReason": "[pretooluse-demo] hook blocked this tool call.",
            }
        }
        log_message("BLOCK RESPONSE:")
        log_message(json.dumps(response, indent=2, ensure_ascii=False))
        sys.stdout.write(json.dumps(response))
        return 0

    # ---------------------------------- Allow (default)
    return 0


if __name__ == "__main__":
    sys.exit(main())
