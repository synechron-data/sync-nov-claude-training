#!/usr/bin/env bash
#
# PreToolUse demo hook: shows the different ways a hook can respond.
#
# Pick a mode by editing DEFAULT_MODE below, or set PRETOOL_MODE in the env:
#   allow    - exit 0, no output: tool call proceeds normally (default)
#   log      - log the stdin payload, then allow
#   override - allow, but rewrite the tool input via updatedInput
#   block    - exit 2 hard block; stderr is shown to Claude as the reason
#   deny     - JSON permissionDecision 'deny' on stdout, exit 0
#
# Requires: jq (for parsing the stdin payload and building JSON responses).
# Deliberately no `set -e`: logging must never change the hook's decision.

DEFAULT_MODE="allow"
MODE="${PRETOOL_MODE:-$DEFAULT_MODE}"

LOG_FILE="${CLAUDE_PROJECT_DIR:-.}/.claude/hook-logs/pretooluse-demo.log"

log_message() {
    # logging must never change the hook's decision
    {
        mkdir -p "$(dirname "$LOG_FILE")" &&
            printf '%s\n' "$1" >> "$LOG_FILE"
    } 2>/dev/null || true
}

log_payload() {
    # POSIX TZ string (IST = UTC+5:30, no DST): works even where tzdata is missing,
    # e.g. Git Bash on Windows. Format matches toLocaleString('sv-SE').
    local time_stamp tool_name
    time_stamp="$(TZ='IST-5:30' date '+%Y-%m-%d %H:%M:%S')"
    tool_name="$(printf '%s' "$RAW" | jq -r '.tool_name // empty' 2>/dev/null)"

    log_message ""
    log_message "$(printf -- '-%.0s' {1..60})"
    log_message "[$time_stamp] mode=$MODE tool=$tool_name"
    log_message "STDIN PAYLOAD:"
    log_message "$(printf '%s' "$RAW" | jq . 2>/dev/null)"
}

RAW="$(cat)"

# Parse check: jq must exist and the payload must be a JSON object.
if ! command -v jq >/dev/null 2>&1 ||
    ! printf '%s' "$RAW" | jq -e 'type == "object"' >/dev/null 2>&1; then
    if [[ "$MODE" == "block" || "$MODE" == "deny" ]]; then
        echo "[pretooluse-demo] could not parse hook input; blocking." >&2
        exit 2
    fi
    exit 0
fi

case "$MODE" in
    # ------------------------------------ Allow and logging
    log)
        log_payload
        exit 0
        ;;

    # ------------------------------------ Allow and override with logging
    # permissionDecision 'allow' skips the permission prompt, so an 'ask'
    # rule (e.g. git push) would not be asked. Safe here only because the
    # command is replaced with a harmless echo.
    override)
        log_payload
        RESPONSE="$(printf '%s' "$RAW" | jq -c '{
            hookSpecificOutput: {
                hookEventName: "PreToolUse",
                permissionDecision: "allow",
                permissionDecisionReason: "[pretooluse-demo] hook allowed and overrode this tool call.",
                updatedInput: ((.tool_input // {}) + {command: "echo overridden by custom script"})
            }
        }')"
        log_message "$(printf '%s' "$RESPONSE" | jq .)"
        printf '%s' "$RESPONSE"
        exit 0
        ;;

    # ---------------------------------- Hard Block
    # Exit code 2 hard-blocks the tool call; Claude Code treats it as an error
    # and shows stderr output (if any) as the reason - no JSON response needed.
    block)
        log_payload
        log_message "BLOCKED: exit 2 - see stderr for reason"
        echo "[pretooluse-demo] hook blocked this tool call." >&2
        exit 2
        ;;

    # ---------------------------------- Hard Block with Deny
    # Deny: rejects the tool call via structured JSON on stdout while exiting
    # cleanly (0). permissionDecisionReason is shown to Claude, explaining why
    # the call was denied - unlike exit 2, no stderr text is needed.
    deny)
        log_payload
        RESPONSE='{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"[pretooluse-demo] hook blocked this tool call."}}'
        log_message "BLOCK RESPONSE:"
        log_message "$(printf '%s' "$RESPONSE" | jq .)"
        printf '%s' "$RESPONSE"
        exit 0
        ;;

    # ---------------------------------- Allow (default)
    *)
        exit 0
        ;;
esac
