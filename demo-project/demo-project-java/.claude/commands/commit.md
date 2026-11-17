---
name: commit
description: Generate a Conventional Commits message, branch, commit, and push
disable-model-invocation: true
---

You are a senior developer writing a commit message that will be read during
code review and referenced in the git log for months to come.

This project follows Conventional Commits as defined in CLAUDE.md. Valid
types are: feat, fix, docs, test, refactor. Valid scopes are the module
folders: auth, task, user, config (backend), and auth, tasks (frontend).

First run git status to see what files are modified, untracked, or already
staged. Then stage only the files relevant to the change with git add <paths>
— never run git add -A or git add ., and leave out unrelated files such as
.claude/ settings and local config copies. If application.properties (or any
secrets/key file) is among the changes, do not stage it: stop and flag it to
me explicitly. Then run git diff --staged to inspect
exactly what has been staged. Base the commit message entirely on
what you observe — do not guess or summarise from memory.

Generate a Conventional Commits message for the staged changes.

Format:
  type(scope): short summary under 72 characters

  - Bullet point explaining what changed and why, not how
  - One bullet per logical change group
  - Reference issue numbers if visible in the diff (e.g. closes #204) or Jira
    keys (e.g. TASK-2)

Do not add a Co-Authored-By trailer or any reference to Claude in the commit
message.

Once the commit message is ready:

1. Check the current branch with git branch --show-current.
2. If the current branch is main or master, look for an issue number in the
   staged diff or commit message (e.g. "closes #204" or "#204") or a Jira key
   (e.g. "TASK-2"). Use the commit type as the branch prefix: if a number is
   found, create and switch to <type>/issue-<number> (e.g. feat/issue-204)
   using git checkout -b. If a Jira key is found, use <type>/<key> (e.g.
   fix/TASK-2). If neither is found, use <type>/<scope>-<short-summary>
   derived from the commit message, lowercase kebab-case, ASCII letters,
   digits and hyphens only, at most 50 characters.
3. Commit with the multi-line message passed safely (a heredoc, or one -m per
   paragraph/bullet) — do not inline a multi-line message inside a single
   quoted git commit -m "<message>".
4. Run git push -u origin <branch-name> to push the branch to the remote.

If already on a non-main branch, skip step 2 — commit and push to the
current branch with git push -u origin <current-branch>.

Always follow CLAUDE.md — never push directly to main.

Output only the commit message. No explanation, no commentary, no preamble.

Be precise and factual. Avoid filler words like update, improve, fix up,
tweak, or adjust. Every word should earn its place.