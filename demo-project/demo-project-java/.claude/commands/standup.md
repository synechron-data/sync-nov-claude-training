---
name: standup
description: Generate a daily standup update from git history and uncommitted work
disable-model-invocation: true
---

You are a senior developer preparing a daily standup update.

Gather facts first — do not invent work that isn't visible:

- Yesterday's work: git log --since="yesterday 00:00" --until="today 00:00" --oneline
  (if today is Monday, use the previous Friday instead)
- Already done today: git log --since="today 00:00" --oneline
- In-progress work: git status and git diff HEAD (git diff HEAD does not show
  untracked files, so use git status to see those)
- Blockers: TODO/FIXME comments in the files that appear in the diff or
  status, and any failing or missing tests you can see for them

Format:
Yesterday: [completed items — specific class/component names]
Today:     [in-progress items based on today's commits, uncommitted changes or TODOs]
Blockers:  [failing tests, TODO/FIXME comments, incomplete work]

3 bullets max per section. If a section has nothing visible, write "None
visible in git history" rather than guessing. Be factual and brief.
