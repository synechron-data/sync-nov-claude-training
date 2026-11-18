---
name: write-docs
description: >
  Write or update the project documentation (docs/architecture.md, docs/api.md
  and the README API quick reference) from the actual Spring Boot / Angular
  code. Use when the user says "document this", "update the docs", "write the
  API docs", "document the endpoints", "refresh the README" or "docs are out
  of date", and also after adding or changing an endpoint, controller, entity,
  DTO record, security/JWT config, CORS config or exception handler, even if
  the user does not mention docs. Not for code review, tests or CLAUDE.md edits.
---

# Write Docs

The docs live in the repo as Markdown, next to the code. They are for people,
so they explain behaviour; `CLAUDE.md` holds coding conventions for developers
and Claude, so do not copy it. Everything you write must come from the code as
it is now, because docs that describe intended behaviour mislead readers.

## Doc set

| File | Contains |
|---|---|
| `docs/architecture.md` | System overview, backend packages, data model (Mermaid ER), auth flow (Mermaid sequence), security/CORS config, error-handling table, frontend structure, known limitations |
| `docs/api.md` | Auth, error shape, every endpoint, the Task object, a summary table, a curl example |
| `README.md` | Overview, run instructions, **API quick reference**, the Documentation links |

Use the existing files as the template for structure, tone and heading style.
If a file doesn't exist yet, create it with the same sections. Optional extras
(`docs/frontend.md`, `docs/contributing.md`, `docs/adr/`) only when the user asks.

## Steps

1. **Find what changed.** If the user named a feature, start there. Otherwise run
   `git diff main --stat` and `git log -5 --oneline` to see recently changed
   controllers, entities, config and frontend services. For a full refresh, read
   every file under `backend/src/main/java/com/technizer/taskapi/` and
   `frontend/src/app/`.
2. **Read the code, not the old docs.** Existing docs may already be stale, which
   is often why the user is asking. Read these sources:
   - Controllers: verb, path, `@AuthenticationPrincipal`, request records, return type
   - Services: business rules, which exceptions they throw, ownership checks
   - `GlobalExceptionHandler`: exception to HTTP status and body
   - `SecurityConfig`: public routes, CORS origins/methods, session policy
   - `JwtUtil`: claims and expiry (the property *names*, never the secret value)
   - Entities: fields, types, nullability, defaults
   - Frontend `*.service.ts` and `app.routes.ts` for the client side
3. **Update `docs/api.md`.** For each endpoint document: verb and path (as a
   heading like ``### `PUT /api/tasks/{id}` ``, because the check script parses
   that exact form), auth requirement, request body, success response, and error
   statuses. Derive error statuses by following the service's thrown exception
   through `GlobalExceptionHandler`. Don't guess them. Note when something is
   inert or surprising (for example `@NotBlank` without `@Valid`).
4. **Sync the Task object and data model.** The JSON example, the field table in
   `docs/api.md` and the ER diagram in `docs/architecture.md` must match the
   entity fields. If an entity field exists but no endpoint can set it, say so
   explicitly, because readers will assume it can be set.
5. **Update the README API quick reference** to the same endpoint list as `docs/api.md`.
6. **Update `docs/architecture.md`** for any change to packages, auth flow,
   security config, error handling or the frontend structure. Use Mermaid for
   diagrams (it renders on GitHub and diffs cleanly).
7. **Record limitations honestly.** Keep the "Known limitations" section in
   sync with what the code does today: add newly discovered gaps, and remove
   one only when you've verified in the code that it was fixed. Some limitations
   are deliberate training bugs listed in the README, so describe them neutrally and do not "fix" them.
8. **Verify.** Run `bash .claude/skills/write-docs/scripts/check-endpoints.sh`
   from the repo root. It compares `@*Mapping` annotations in the controllers
   with the headings in `docs/api.md`. If it reports a difference, fix the docs
   and re-run. Don't report done until it prints `OK`. Then re-read the Task
   JSON example against `Task.java` once more, since the script can't check fields.

## Rules

- **Never read or print `application.properties` secrets.** Reading that file is
  blocked in this project. Document property names and defaults from the
  `@Value` annotations in code (for example `app.jwt.expiry-minutes`, default 60)
  and never write a secret value into any doc. If a property can only be learned
  from that file, say it is unverified and ask the user.
- **Don't delete existing content without saying so.** If a section is obsolete,
  replace it, and list what you removed in your summary. Hand-written parts of
  the README (trainer notes, setup checklist) are not yours to rewrite. Only touch
  the sections named above.
- **Don't invent behaviour.** If you can't confirm something from the code
  (for example runtime behaviour of a filter), mark it as unverified or leave it out.
- **No new dependencies.** Generating docs with springdoc/Swagger would add one.
  Suggest it and wait for the user's confirmation instead of adding it.
- **Doc-only changes need no new tests.** If you notice a code bug while
  documenting, document the behaviour, tell the user, and do not fix it as part
  of this task.

## Output

Finish with a short summary: files created or changed, endpoints added or
removed, anything you removed, anything unverified, and the result of the
check script. If asked to commit, use a Conventional Commit on a branch
other than `main`, for example `docs: update API reference for task endpoints`.
