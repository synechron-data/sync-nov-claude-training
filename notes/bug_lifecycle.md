# Bug Lifecycle Demo

## Before you start

- Steps 2, 3 and 5 use the team commands from the handbook and need GitHub MCP / `gh` connected.
- Step 6 needs agent teams turned on. Add `"CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"` under `"env"` in `.claude/settings.json` (or export it) before starting `claude`.

---

## 1. Find the vulnerability

**Option A (recommended for code already on `main`):**

```
@agent-security-analyst audit the auth service
```

**Option B: the built-in `/security-review`**

- It takes no file or folder argument. `/security-review authservice` does not narrow it.
- It reviews the **diff** between your current branch and origin's default branch, and needs an `origin` remote. If the vulnerable code is already on `main`, it has nothing to review.

> **Note:** the handbook's custom `.claude/commands/security-review.md` uses the same name as the built-in command. Rename it (e.g. `owasp-review.md` → `/owasp-review`) so it's clear which one runs.

## 2. Raise the issue

```
/create-issue for Critical Vuln 1
```

## 3. Fix the issue

```
/fix-issue 1
```

## 4. Commit

```
/commit for fix #1
```

## 5. Raise the pull request

```
/raise-pr
```

## 6. Review the PR with an agent team

```
Spawn three teammates to review PR #2:
- security: focused exclusively on security implications
- performance: focused on performance and scalability
- tests: validating test coverage and edge cases

Have them each review independently, then synthesize findings into a single report.
```

- Asking for teammates is enough to start the team. If Claude uses ordinary subagents instead, ask again and say "agent team".
- If the lead starts doing the review itself, tell it: `Wait for your teammates to complete their tasks before proceeding`.

## 7. Merge and close the PR with a comment

Merge the PR and close it with a comment.

## 8. Standup

```
/standup
```
