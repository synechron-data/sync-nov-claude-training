# Bug & Feature Lifecycle — GitHub Issues + Spec Kit + Claude Code

*End-to-end workflow: GitHub Issues track the work, Spec Kit structures it, GitHub holds the code and pull requests, and Claude Code does the work. Everything runs through one tool, GitHub.*

> **Sources:** [Spec Kit bug fixing quickstart](https://github.github.com/spec-kit/guides/bugfix.html), [Spec Kit quickstart](https://github.github.com/spec-kit/quickstart.html), [GitHub MCP Server](https://github.com/github/github-mcp-server), [Linking a pull request to an issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue), [Claude Code MCP docs](https://code.claude.com/docs/en/mcp), [Claude Code agent teams](https://code.claude.com/docs/en/agent-teams)
> **Related notes:** `spec-kit-installation.md`, `spec-kit-workflow.md`, `github_mcp_config.md`, `bug-lifecycle-jira-speckit.md` (the Jira version of this guide)

## The Big Picture

| Tool | Role |
|---|---|
| **GitHub Issues** (via GitHub MCP) | Source of truth for work items: bugs, features, sub-issues, status labels, comments |
| **Spec Kit** | Structure: `assess → fix → test` for bugs; `specify → plan → tasks → implement → converge` for features |
| **GitHub** (via `gh` and the GitHub MCP) | Branches, pull requests, reviews, merges |
| **Claude Code** | Runs every step: audits, issue updates, Spec Kit skills, commits, reviews |

**Linking rule used throughout:** put the **issue number** (e.g. `#12`) in the branch name, the commit message, and the PR description as `Fixes #12`. GitHub then links the PR to the issue, shows it in the issue's **Development** panel, and **closes the issue automatically** when the PR merges into the default branch. No extra app or integration is needed.

Two flows:

- **Part A — Bug:** GitHub Issue (bug) → Spec Kit bug extension → PR → agent-team review → merge → issue auto-closes
- **Part B — Feature:** GitHub Issue (feature) → Spec Kit feature workflow → sub-issues → PR → merge → issue auto-closes

---

## Prerequisites (one-time setup)

### 1. Spec Kit in the repository

Follow `spec-kit-installation.md`, then add the bug extension:

```bash
specify init --here --force --integration claude     # if not already initialized
specify extension add bug                            # adds /speckit-bug-assess, -fix, -test
```

Commit the generated `.specify/` and `.claude/skills/` folders.

### 2. GitHub repository setup

Everything in this guide assumes a repository with Issues turned on, a small set of labels, and the right access. Set it up once. You need **Admin** on the repository (or ask an org owner).

#### 2.1 Turn on Issues

**Repository → Settings → General → Features** → tick **Issues**. Forks have Issues off by default, so check this if the repo is a fork.

#### 2.2 Create the labels

GitHub has only two built-in states, **open** and **closed**. This guide adds labels for the in-between status and for priority:

| Label | Used for |
|---|---|
| `bug` | Part A issues (usually exists already) |
| `enhancement` | Part B issues (usually exists already) |
| `security` | Security findings |
| `priority: highest` | Step A2 |
| `in progress` | Work has started (A3 / B2) |
| `in review` | PR is open (A8 / B11) |

Create the missing ones from the repository folder:

```bash
gh label create "security" --color B60205 --description "Security finding"
gh label create "priority: highest" --color D93F0B
gh label create "in progress" --color FBCA04
gh label create "in review" --color 0E8A16
```

The status flow is therefore **open → `in progress` → `in review` → closed**. Use these exact names; the prompts in Parts A and B refer to them. If a label already exists, `gh` reports an error for it and you can ignore that.

#### 2.3 Issue types (optional)

If the repository belongs to an **organization** that has **Issue types** enabled, Claude can set the type (**Bug**, **Feature**, **Task**) when it creates an issue. An org owner turns this on in **Organization → Settings → Planning → Issue types**. Personal repositories don't have issue types, so this guide uses the `bug` and `enhancement` labels instead and works either way.

#### 2.4 Sub-issues (for Part B)

Part B breaks a feature into one **sub-issue** per phase. Sub-issues are available on github.com repositories with no setup: open an issue and use **Create sub-issue**. If your GitHub Enterprise Server version doesn't have them, use a task list (`- [ ] ...`) in the parent issue instead and tick the boxes as phases finish.

#### 2.5 GitHub Projects board (optional)

A **GitHub Projects** board gives you a Kanban view of the labels. If you add one, turn on its built-in workflows (**Project → ⋯ → Workflows**): *Item added to project → Todo*, *Item closed → Done*, *Pull request merged → Done*. Then merging a PR moves the card for you. Skip this for the demo if you want to keep things simple; the labels are enough.

#### 2.6 Check access

Everyone who runs this workflow through Claude Code needs **Write** role or higher on the repository (to create issues, edit labels, push branches and open PRs). Claude acts as the signed-in token owner, so it can only do what that person can do.

#### 2.7 Verify the repository

Open these read-only URLs while signed in to GitHub, replacing `<owner>/<repo>`:

| Check | URL | Expect |
|---|---|---|
| Issues enabled | `https://github.com/<owner>/<repo>/issues` | The Issues list, not a 404 |
| Labels | `https://github.com/<owner>/<repo>/labels` | `bug`, `enhancement`, `security`, `priority: highest`, `in progress`, `in review` |

Or, once the MCP server (step 3) is connected, ask Claude:

```
For repository <owner>/<repo>, list the labels and tell me whether Issues are enabled. Don't change anything.
```

### 3. GitHub MCP Server

This lets Claude Code read and update GitHub issues and pull requests as you (create issues, comment, add labels, assign, open sub-issues, read PR diffs and review comments). It is **required** for this guide because GitHub Issues are the tracker.

Use GitHub's **official remote server** (`https://api.githubcopilot.com/mcp/`). Don't use the old `@modelcontextprotocol/server-github` npm package. It is archived and unmaintained, and on Windows `npx` also needs a `cmd /c` wrapper.

GitHub's server uses a **personal access token**, not a browser sign-in.

#### 3.1 Create a fine-grained token (each person)

1. Sign in to **github.com** with the account you'll use for the demo.
2. Click your **profile picture** (top right) → **Settings**.
3. In the left sidebar, scroll to the bottom → **Developer settings**.
4. **Personal access tokens → Fine-grained tokens** → **Generate new token**.
   Direct link: `https://github.com/settings/personal-access-tokens/new`
   (GitHub may ask you to confirm your password or 2FA. Do that yourself.)
5. Fill in the top of the form:
   - **Token name:** e.g. `claude-code-mcp`
   - **Description:** e.g. `Claude Code GitHub MCP for ms-sep-claude-training`
   - **Resource owner:** choose the **organization** that owns the repo (e.g. `synechron-data`), not your personal account. Otherwise the org repo won't appear in the next step.
   - **Expiration:** 30 days (or a custom short date).
6. **Repository access:** select **Only select repositories** → pick your repo (e.g. `ms-sep-claude-training`).
7. **Permissions → Repository permissions**. Click **Add permissions** (or expand the list) and set:

   | Permission | Access | Used for |
   |---|---|---|
   | **Issues** | Read and write | Create, edit, label, assign, comment on issues; sub-issues |
   | **Pull requests** | Read and write | Open and read PRs and diffs, post review comments (A9) |
   | **Contents** | Read and write | Read files, branches, commits |
   | **Actions** | Read-only | Check CI runs before merge (A11) |
   | **Metadata** | Read-only | Added automatically. Required. |

   If you use a Projects board (2.5), also add the **Projects** permission (organization-level for org boards). Leave the other **Account** and **Organization** permissions at *No access*.
8. Click **Generate token** (review the summary if prompted, then confirm).
9. **Copy the token now** (starts with `github_pat_`). GitHub shows it only once. Go straight to 3.2. Don't paste it into chat, notes or files.

**If the organization requires approval:** the token shows **Pending** under *Fine-grained tokens* and can't access org repos yet. An org owner approves it in **Organization → Settings → Personal access tokens → Pending requests**. If the org blocks fine-grained tokens entirely, an owner must allow them in **Organization → Settings → Personal access tokens → Settings**.

**Renewing:** when it expires, open the token under **Fine-grained tokens → Regenerate token**, then update `GITHUB_PAT` (3.2).

#### 3.2 Store the token in an environment variable (each person)

Never put the token in a file you commit.

Windows (PowerShell, persists for your user):

```powershell
[Environment]::SetEnvironmentVariable("GITHUB_PAT", "github_pat_xxx", "User")
```

Windows (CMD, persists for your user):

```cmd
setx GITHUB_PAT "github_pat_xxx"
```

macOS / Linux (add to `~/.zshrc` or `~/.bashrc`):

```bash
export GITHUB_PAT="github_pat_xxx"
```

Open a **new terminal** afterwards so the variable is loaded. Claude Code must be started from a terminal that has it.

#### 3.3 Add the server to the project (once, committed)

Add a `github` entry to the repo's `.mcp.json`. For `demo-project` the file is:

```json
{
  "mcpServers": {
    "github": {
      "type": "http",
      "url": "https://api.githubcopilot.com/mcp/",
      "headers": {
        "Authorization": "Bearer ${GITHUB_PAT}"
      }
    }
  }
}
```

Claude Code replaces `${GITHUB_PAT}` with each person's own environment variable when it starts the server, so the committed file never contains a token.

Or add it from the CLI. Keep the header value in **single quotes** so your shell doesn't expand it and write the real token into the file:

```bash
claude mcp add --transport http github https://api.githubcopilot.com/mcp/ \
  --header 'Authorization: Bearer ${GITHUB_PAT}' --scope project
```

Check the config:

```bash
claude mcp get github
```

Expect `Scope: Project config (shared via .mcp.json)`, the URL, and the header showing `Bearer ${GITHUB_PAT}` (the variable, not the token).

Check `.gitignore` doesn't exclude `.mcp.json`, then commit it:

```bash
git add .mcp.json
git commit -m "chore: add GitHub MCP server"
```

#### 3.4 Approve and verify

1. Start Claude Code in the repo (from the new terminal).
2. Approve the `github` project server when prompted (or in `/mcp`). Until you do, `claude mcp list` shows it as **Pending approval**.
3. `/mcp` should show `github` as **connected**.
4. Ask:

   ```
   List the open issues in synechron-data/ms-sep-claude-training.
   ```

5. Then confirm Claude can write, and that the token covers what this guide needs:

   ```
   What can you do with GitHub issues and pull requests here? List the tools you have for issues, sub-issues, labels and PRs. Don't change anything.
   ```

   If sub-issue or label tools are missing, the server or token may not support them yet. Fall back to the alternatives in 2.4, and to `gh label` / `gh issue edit` for labels.

#### 3.5 Troubleshooting

| Symptom | Fix |
|---|---|
| `401 Unauthorized` / fails to connect | The token is invalid, expired or revoked, or `GITHUB_PAT` isn't set in the terminal Claude Code was started from. Test the token: `(Invoke-WebRequest https://api.github.com/user -Headers @{Authorization="Bearer $env:GITHUB_PAT";"User-Agent"="t"}).StatusCode` in PowerShell should print `200`. If not, regenerate it (3.1). After `setx` or `SetEnvironmentVariable`, open a **new** terminal. |
| Server stays **Pending approval** | Start `claude` in the repo and approve it, or approve it in `/mcp` |
| Repo not found / `403` | The token doesn't include the repo or lacks a permission, or the org hasn't approved it yet |
| Can read issues but not create or label them | The token's **Issues** permission is *Read-only*. Edit the token to **Read and write** |
| Stops working after a few weeks | Token expired. Generate a new one and update `GITHUB_PAT` |
| Token appears in `.mcp.json` | The `--header` was in double quotes. Replace the value with `Bearer ${GITHUB_PAT}`, **revoke the leaked token**, and make a new one |

### 4. `gh` CLI

The label setup (2.2) and the merge step use `gh`:

```bash
gh auth login
gh auth status
```

Claude can use the GitHub MCP for most things, but `gh` is the simplest way to create labels and to merge.

### 5. Team commands and agents

This guide uses the handbook's team commands and agents:

| Used here | From |
|---|---|
| `/commit`, `/raise-pr`, `/standup` | `.claude/commands/` (handbook Module 6) |
| `@agent-security-analyst` and the other analyst agents | `.claude/agents/` (handbook Module 8) |

### 6. Agent teams (for the PR review step)

Add to `.claude/settings.json` (or `.claude/settings.local.json`):

```json
{
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
  }
}
```

While agent teams are on, subagents that Claude names may launch as teammates. Turn it back to `"0"` when you're done with the review step if that gets in the way.

### 7. Permissions

GitHub writes (create or edit issues, add labels, comment), `git push` and `gh pr merge` should stay **ask**, not allow. They're visible to other people and hard to undo. Approve each one deliberately.

---

## Part A — Bug Lifecycle

```
A1 Find ─► A2 Raise issue ─► A3 Branch ─► A4 Assess ─► A5 Fix ─► A6 Test
   ─► A7 Commit ─► A8 PR ─► A9 Team review ─► A10 Address findings
   ─► A11 Merge ─► A12 Confirm closed ─► A13 Standup
```

### A1 — Find the problem

Use the security specialist agent to audit existing code:

```
@agent-security-analyst audit the auth package and list every finding with severity
```

**Why an agent, not `/security-review`:** the built-in `/security-review` only reviews the diff between your branch and origin's default branch. For code that's already on `main`, it has nothing to review.

**Check:** you have a list of findings with severity, file and method. Note **every** finding you intend to track, not just the first.

### A2 — Raise each finding as a GitHub issue

For each finding:

```
Create a GitHub issue in synechron-data/ms-sep-claude-training for the "JwtUtil.isTokenValid always returns true" finding.
Title: short title. Body sections: Summary, Current Behaviour, Expected Behaviour,
Steps to Reproduce, Files Affected, Acceptance Criteria. Labels: bug, security, priority: highest.
If the repo has issue types, set the type to Bug.
Show me the draft before creating it.
```

**Check:** open each returned issue link in GitHub. Note the numbers, e.g. `#12` (JWT validation) and `#13` (task ownership check). Confirm the labels are attached.

Repeat A3–A12 for **each** bug. The steps below use `#12`.

### A3 — Create a branch and start work

```bash
git checkout main && git pull
git checkout -b fix/12-jwt-validation
```

Then in Claude Code:

```
On issue #12: assign it to me, add the label "in progress", and add a comment with the branch name fix/12-jwt-validation.
```

Creating the branch **yourself, with the number in its name**, keeps the work easy to trace. `/commit` commits to the current branch when you're not on `main`, so the number carries through.

### A4 — Assess the bug (Spec Kit, read-only)

```
/speckit-bug-assess GitHub issue #12 — JwtUtil.isTokenValid() in the auth package always returns true, so expired or tampered JWTs are accepted by every protected endpoint. Expected: invalid signature or expired token is rejected with 401. Read the full issue from GitHub before assessing. slug=issue-12-jwt-validation
```

**What happens:** Claude investigates the code, judges whether it's a real bug, locates the code paths, and proposes a remediation. It writes `.specify/bugs/issue-12-jwt-validation/assessment.md` and **changes no source code**.

**Check before continuing:**
- The diagnosis is supported by the code it cites.
- The proposed remediation is scoped to the right files.
- It includes a reproduction you can actually run.

Don't proceed on an unsupported diagnosis. Re-run assess with more evidence instead.

Optional — record it on the issue:

```
Add a comment to issue #12 summarising the diagnosis and proposed fix from .specify/bugs/issue-12-jwt-validation/assessment.md.
```

### A5 — Fix (Spec Kit)

```
/speckit-bug-fix slug=issue-12-jwt-validation
```

**What happens:** Claude applies the assessed remediation (this is the **only** bug step that edits source code) and records the change in `.specify/bugs/issue-12-jwt-validation/fix.md`. If it has to go beyond the assessed files, it logs that under **Deviations from Assessment** instead of silently expanding scope.

**Check:** review the diff (`/diff`) and read any deviations.

### A6 — Test (Spec Kit)

```
/speckit-bug-test slug=issue-12-jwt-validation
```

**What happens:** Claude re-runs the reproduction and the relevant tests and writes `.specify/bugs/issue-12-jwt-validation/test.md` with a verdict. It doesn't edit code.

| Verdict | Meaning | Next |
|---|---|---|
| `verified` | Reproduction and tests exercised successfully | Continue to A7 |
| `partial` | Some verification couldn't be completed | Supply the missing environment or evidence, re-run A6 |
| `failed` | A problem remains | Go back to A4 or A5 with the new evidence, then re-run A6 |

Also run the full suites so nothing else broke:

```
Run mvn -f backend/pom.xml test and npm --prefix frontend test -- --watch=false and report the results.
```

**Do not continue unless the verdict is `verified` and both suites pass.**

### A7 — Commit

```
/commit Fixes GitHub issue #12. Include (#12) at the end of the commit summary.
```

**What happens:** `/commit` stages everything (code, tests, and the `.specify/bugs/…` records), writes a Conventional Commits message, commits to `fix/12-jwt-validation`, and pushes. Approve the push when prompted.

**Check:** `git log -1` shows `#12` in the message.

> Don't use `Fixes #12` in the **commit** message on a feature branch if you don't want the issue closed early. Closing keywords only take effect when the commit reaches the default branch, but putting the keyword in the **PR description** (A8) is the cleaner convention.

### A8 — Raise the pull request

```
/raise-pr This fixes GitHub issue #12. Put "Fixes #12" in the PR description so the issue is linked and closes on merge, include a "Related issue" section linking the issue, and link the Spec Kit records in .specify/bugs/issue-12-jwt-validation/.
```

**Check:** open the PR. Note the **PR number** from the output (don't assume it). Confirm the **Development** panel of the PR shows issue `#12` as linked, and that the PR appears in the issue's own Development panel.

Then:

```
On issue #12, replace the label "in progress" with "in review" and add a comment with the PR link.
```

### A9 — Review the PR with an agent team

```
Spawn three teammates to review PR #<pr-number>:
- security: using the security-analyst agent type, verify the JWT fix rejects expired and tampered tokens and introduces no new bypass
- performance: using the performance-analyst agent type, check the change adds no per-request overhead in the filter chain
- tests: using the test-coverage-analyst agent type, check the tests cover valid, expired, malformed and wrongly-signed tokens, and compare them with .specify/bugs/issue-12-jwt-validation/test.md

Have them review independently, then synthesize one report with findings grouped by severity.
```

If the lead starts reviewing itself: `Wait for your teammates to complete their tasks before proceeding`.

**Keep the report** and post it where the team can see it:

```
Post the synthesized review as a comment on PR #<pr-number> and add a one-paragraph summary as a comment on issue #12.
```

### A10 — Address review findings

If there are Critical/High findings:

```
Fix the Critical and High findings from the review on this branch. Don't change anything else.
```

Then repeat:

1. `/speckit-bug-test slug=issue-12-jwt-validation` — must be `verified`
2. Run both test suites
3. `/commit Addresses review findings for #12.`

Findings you decide not to fix now → raise them as new issues (A2) and note the numbers in a PR comment.

### A11 — Merge (human decision)

Merging to `main` is irreversible and visible to everyone, so a person makes this call.

**Check first:** CI is green, review findings are resolved or ticketed, at least one human approval.

```bash
gh pr merge <pr-number> --squash --delete-branch
```

(Or merge in the GitHub web UI. Asking Claude to run the command is fine too. Approve the prompt.)

Because the PR description contains `Fixes #12` and the PR targets the default branch, **GitHub closes issue #12 automatically** when the merge completes.

### A12 — Confirm the issue is closed

```
Check that issue #12 is closed. Add a comment with the merged PR link and the verification verdict from .specify/bugs/issue-12-jwt-validation/test.md, and remove the label "in review".
```

**Check:** issue `#12` shows **Closed**, with the merged PR linked. If it is still open, the PR didn't target the default branch or the keyword was missing; close it manually and add the PR link.

### A13 — Standup

```
/standup
```

---

## Part B — Feature Lifecycle with Spec Kit

Use this when the issue is a **feature** (new behaviour) rather than a bug. See `spec-kit-workflow.md` for what each Spec Kit step does.

```
B1 Issue ─► B2 Branch ─► B3 Specify ─► B4 Clarify ─► B5 Plan ─► B6 Tasks
   ─► B7 Sub-issues ─► B8 Analyze ─► B9 Implement ─► B10 Converge
   ─► B11 Commit & PR ─► B12 Review & merge ─► B13 Confirm closed
```

### B1 — Start from a GitHub issue

Use an existing feature issue, or create one:

```
Create a GitHub issue in synechron-data/ms-sep-claude-training: "Task priority with filtering and sorting". Label: enhancement (and type Feature if the repo has issue types). Body: users can set Low/Medium/High priority, filter by priority and completion status, and sort by priority; existing tasks default to Medium; existing API responses must not break. Show me the draft first.
```

Note the number, e.g. `#20`.

### B2 — Branch and start work

```bash
git checkout main && git pull
git checkout -b feature/20-task-priority
```

```
On issue #20: assign it to me and add the label "in progress".
```

### B3 — Specify from the issue

```
/speckit-specify Implement GitHub issue #20. Read the issue and its acceptance criteria from GitHub first. Describe what users need and why — no tech stack.
```

**Output:** `specs/<NNN>-<name>/spec.md` and `checklists/requirements.md`.

**Check:** every acceptance criterion in the GitHub issue appears in `spec.md`. Anything in the spec that's *not* in the issue is scope creep. Remove it or agree it with the issue owner.

### B4 — Clarify (and sync decisions back to the issue)

```
/speckit-clarify
```

Answer the questions. Then keep the issue consistent with the spec:

```
Add a comment to issue #20 listing the clarification decisions now recorded in spec.md.
```

### B5 — Plan

```
/speckit-plan Use the existing Spring Boot 3 / JPA backend and Angular frontend and follow the existing architecture and test conventions.
```

**Check:** `plan.md`, `data-model.md` and `contracts/` reference real code and respect the constitution.

### B6 — Tasks

```
/speckit-tasks
```

**Output:** `tasks.md` in phases: Setup, Foundational, one per user story, Polish.

### B7 — Mirror the work breakdown as sub-issues

Create one **sub-issue per phase** (not per task; per-task is usually too granular):

```
For GitHub issue #20, create one sub-issue per phase in specs/<NNN>-<name>/tasks.md (Setup, Foundational, each user story, Polish). Put the phase's task IDs and descriptions in each sub-issue body. Add each as a sub-issue of #20. Show me the list before creating anything.
```

**Check:** the sub-issues appear under `#20` with a progress bar. Note their numbers.

> **If sub-issue tools aren't available** (see 3.4), have Claude add a task list to the body of `#20` instead:
> `- [ ] Setup`, `- [ ] Foundational`, and so on, and tick each box as the phase finishes.

### B8 — Analyze

```
/speckit-analyze
```

Fix any findings at the source step (specify / plan / tasks) and re-run until clean.

### B9 — Implement, phase by phase

For each phase:

```
Add the label "in progress" to the Setup sub-issue of #20.
```
```
/speckit-implement Implement only the Setup phase. Run mvn test and npm test after each task and stop if anything fails.
```
```
Close the Setup sub-issue of #20 and comment which tasks in tasks.md were completed.
```

Repeat for Foundational, each user story phase, and Polish. Commit after each phase:

```
/commit #20 <phase name> phase.
```

### B10 — Converge

```
/speckit-converge
```

If it appends tasks, implement them (B9), then converge again until it reports **Converged**.

Record it:

```
Add a comment to issue #20: implementation converged against specs/<NNN>-<name>/spec.md.
```

### B11 — Commit and raise the PR

```
/commit #20 final changes.
```
```
/raise-pr Implements GitHub issue #20. Put "Fixes #20" in the PR description, and link specs/<NNN>-<name>/spec.md and plan.md.
```
```
On issue #20, replace the label "in progress" with "in review" and add the PR link as a comment.
```

### B12 — Review and merge

Run the agent-team review as in **A9**, with briefs based on the spec:

```
Spawn three teammates to review PR #<pr-number>:
- security: new query parameters are validated and never return another user's tasks
- performance: filtered/sorted queries use appropriate indexes, no N+1
- tests: every acceptance criterion in specs/<NNN>-<name>/spec.md has a test

Have them review independently, then synthesize one report.
```

Address findings (as **A10**), then a person merges (as **A11**). `Fixes #20` closes the parent issue on merge.

### B13 — Confirm closed

```
Check that issue #20 and all its sub-issues are closed. Close any that are still open, remove the label "in review", and add a comment on #20 with the merged PR link.
```

```
/standup
```

---

## Artifact & Link Map

| What | Where | Linked by |
|---|---|---|
| Bug report | GitHub issue `#12` (labels `bug`, `security`) | — |
| Bug diagnosis / fix record / verification | `.specify/bugs/<slug>/assessment.md`, `fix.md`, `test.md` | Issue comments, PR description |
| Feature request | GitHub issue `#20` + phase sub-issues | — |
| Spec / plan / tasks | `specs/<NNN>-<name>/` | Issue comments, PR description |
| Code changes | Branch `fix/12-…` / `feature/20-…` | Issue number in branch name |
| Commits | Commit messages mentioning the issue number | `#12` in message |
| Review | PR comment (agent-team report) + issue summary comment | PR number, `Fixes #12` in PR description |

## Quick Reference

| Step | Bug | Feature |
|---|---|---|
| Find / define | `@agent-security-analyst audit …` → GitHub issue (`bug`) | GitHub issue (`enhancement`) |
| Branch | `fix/<number>-<slug>` | `feature/<number>-<slug>` |
| Understand | `/speckit-bug-assess … slug=<slug>` | `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-analyze` |
| Build | `/speckit-bug-fix slug=<slug>` | `/speckit-implement` per phase |
| Verify | `/speckit-bug-test slug=<slug>` (must be `verified`) + test suites | `/speckit-converge` (must be Converged) + test suites |
| Ship | `/commit #<n>` → `/raise-pr` (`Fixes #<n>`) → team review → human merge | same |
| Track | Labels: `in progress` → `in review` → closed (automatic on merge), with comments | same, plus sub-issues per phase |

## Common Mistakes

| Mistake | Instead |
|---|---|
| Hard-coding issue or PR numbers | Use the number each step returns |
| Letting `/commit` pick a branch name | Create `fix/<number>-…` or `feature/<number>-…` yourself before starting |
| Tracking only the first audit finding | Raise every finding you intend to fix; ticket the rest |
| Skipping the assessment review | Don't run `/speckit-bug-fix` on a diagnosis you haven't checked |
| Merging on a `partial` bug verdict | Get to `verified` first |
| Losing the agent-team report when the session ends | Post it on the PR and summarise it on the issue |
| Letting Claude merge without a human decision | Keep merge and issue writes as ask-permission actions |
| Creating a sub-issue for every line of `tasks.md` | One sub-issue per phase |
| Using `/security-review` to audit code already on `main` | Use the security-analyst agent; `/security-review` reviews branch diffs |
| Forgetting `Fixes #<n>` in the PR description | Add it, or the issue stays open after merge and the PR isn't linked |
| `Fixes #<n>` on a PR that doesn't target the default branch | The issue won't auto-close. Close it by hand after merge |
| Token with *Read-only* Issues permission | Set **Issues** to *Read and write* on the PAT (3.1) |
| Committing a real token in `.mcp.json` | Keep `Bearer ${GITHUB_PAT}` and use single quotes with `claude mcp add --header` |
