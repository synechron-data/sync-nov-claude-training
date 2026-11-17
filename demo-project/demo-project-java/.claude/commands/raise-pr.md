---
name: raise-pr
description: Raise a GitHub pull request for the current branch targeting main
disable-model-invocation: true
---

You are a senior developer raising a pull request for a fix or feature branch.

Steps:

1. Run git branch --show-current to get the current branch name. If it is main
   or master, stop and tell me — there is nothing to raise a PR from.
2. Run git log origin/main..HEAD --oneline to list all commits on this branch.
   If there are none, stop and tell me.
3. Run git diff origin/main...HEAD to inspect all changes introduced by this
   branch. (Use origin/main, not local main, which may be stale.)
4. Check if the branch name or commits reference an issue number (e.g.
   fix/issue-204 or "closes #204") or a Jira key (e.g. fix/TASK-2 or
   "TASK-2"). Extract it if present.
5. Run gh auth status. If gh is not authenticated, stop and tell me instead of
   continuing.

Using what you observe, generate a PR title and body:

Title format:
  type(scope): short summary under 72 characters  (if the branch has one
  commit, reuse its subject; otherwise write one summarising the whole branch)

Body format:
  ## What changed
  - Bullet points describing each logical change

  ## Why it changed
  - The problem or issue this PR resolves (reference issue number or Jira
    key if found)

  ## How to test
  - Step-by-step instructions to verify the fix or feature works

Then:

1. If the branch has no upstream yet, push it first (before creating the PR):
     git push -u origin <branch-name>
2. Create the PR. Write the body to a temporary file in the scratchpad
   directory and pass it with --body-file, so markdown, backticks and quotes
   are not mangled by the shell:
     gh pr create --base main --title "<title>" --body-file <file>

Never push to main. Output the PR title, the body, and the PR URL returned by
gh. No other commentary.
