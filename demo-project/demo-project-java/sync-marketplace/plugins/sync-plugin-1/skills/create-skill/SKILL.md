---
name: create-skill
description: >
  Create a new Claude Code skill (a SKILL.md with frontmatter and
  instructions) for this project. Use whenever the user says "create a
  skill", "make a skill", "add a skill", "turn this workflow into a skill",
  "write a SKILL.md" or "I keep asking Claude to do X, can we automate it",
  even if they never say the word "skill". Interviews the user, checks for
  overlap with existing skills, drafts the file in .claude/skills/, and
  verifies it with a few realistic test prompts.
---

# Create Skill

A skill is a folder containing a `SKILL.md`. Claude sees only its `name` and
`description` up front, and loads the body when the description matches the
task. So the description decides whether the skill ever fires, and the body
decides whether it works once it does.

## 1. Capture intent

If the conversation already contains a workflow the user wants captured, pull
the answers from it first (tools used, order of steps, corrections the user
made, output format) and only ask about the gaps. Otherwise ask, briefly:

1. What should the skill make Claude do?
2. What would the user actually type to trigger it?
3. What does a good result look like (files, format, commands that must pass)?
4. Does it have side effects (push, deploy, delete, send)? If yes, it should
   require confirmation and set `disable-model-invocation: true`.

Ask only what you cannot infer. Do not interrogate.

## 2. Check for overlap

Read the existing skills in `.claude/skills/` and the skill list in your
context. If a skill already covers most of this, propose extending it instead
of adding a near-duplicate. Two skills with overlapping descriptions fight
over the same prompts and both get worse.

## 3. Choose name and location

- Project skill (default, checked in for the team): `.claude/skills/<name>/SKILL.md`
- Personal skill (only if the user asks): `~/.claude/skills/<name>/SKILL.md`
- `<name>`: lowercase, hyphenated, matches the folder name, says what it does.

If the folder already exists, stop and ask before overwriting anything.

## 4. Write SKILL.md

Frontmatter:

```yaml
---
name: <folder-name>
description: >
  <what it does + when to use it>
---
```

Add `disable-model-invocation: true` for anything with side effects, so it
only runs when the user explicitly invokes it.

**Description.** All "when to use" information goes here, not in the body.
Say what the skill does and list concrete trigger phrases and contexts. Claude
tends to under-trigger, so lean slightly pushy: include cases where the user
does not name the skill but clearly needs it.

**Body.** Write in the imperative and explain why each instruction exists.
A model that knows the reason generalises to cases you did not anticipate;
a bare rule it can only follow literally. If you reach for ALWAYS or NEVER in
capitals, replace it with the reasoning. Include:

- The steps, in order, with the exact commands to run
- The output format, with a short example if the format is strict
- What to do on failure, and when to stop and ask the user
- Project rules the skill must respect (see CLAUDE.md)

**Size and structure.** Keep the body under about 500 lines. Move long
reference material to `references/<topic>.md` and say when to read it. Put
repeatable, deterministic work in `scripts/` and call it rather than
re-describing it in prose. The `deploy` skill does this: the script is the
source of truth, the skill only wraps it.

## 5. Guardrails

- Follow `CLAUDE.md`: never install dependencies without confirming, never
  modify secrets in `application.properties` without flagging it, never push
  to main, never remove existing tests.
- A skill's behaviour must match what its description promises. No hidden
  side effects, no credential handling, nothing that exfiltrates data.
- Do not hard-code secrets, tokens or personal data into a skill.

## 6. Verify

Write 2 or 3 realistic prompts, the kind a teammate would really type, not
abstract ones. For each, either run it in a subagent with the skill's path
and check the result, or walk through the skill yourself. Show the user the
prompts and the outcomes, note anything that went wrong, and fix the skill.
Also check the negative case: pick a near-miss prompt and confirm the
description would not claim it.

## 7. Report

Tell the user the file path, the description you wrote, how to invoke it
(automatically, or via `/<name>`), and what you tested. If the skill changed
code behaviour in the repo, remind them that new code needs tests, per
`CLAUDE.md`.

For heavier iteration (benchmarks, side-by-side baselines, description
tuning), suggest the built-in `anthropic-skills:skill-creator` skill.
