---
name: task-module-known-issues
description: Open code-quality findings in the task module (backend task/ and frontend tasks/) from the 2026-11-18 audit
metadata:
  type: feedback
---

Open issues found in the 2026-11-18 audit of the task module. Re-verify against current code before citing.

- **Ownership gap:** `TaskService.markCompleted` and `TaskService.delete` ignore `ownerId`. `update` does enforce ownership, so the same check exists in one place only. Suggested fix: a shared private `findOwned(ownerId, taskId)` helper used by all three. `deleteById` on a missing id is a silent no-op, so delete also returns 200 for any id.
- **Title validation:** only `update` rejects a blank or null title. `create` does not, and the DB column is `nullable = false`.
- **Duplicate DTOs:** `TaskController.CreateTaskRequest` and `UpdateTaskRequest` are identical records. Their `@NotBlank` is inert because there is no `@Valid`.
- **Dead code:** `Priority.parse` and `Priority.getRank` have no callers in main code. No endpoint accepts a priority, though `Task` exposes it in JSON.
- **Frontend error handling:** every `subscribe` in `TaskListComponent` has no error handler, so failures are silent.
- **Test gaps:** `TaskServiceTest` has an empty `placeholder()` test and a stale header comment. No tests exist for `markCompleted`, `delete`, `listForOwner`, or the controller's `list`, `complete`, `delete`.
- **Minor:** `today()` is duplicated in `task-list.component.ts` and its spec as `dateOffset`. `TaskService.baseUrl` is hardcoded to localhost. The TS `description` type should be `string | null`.

**Why:** these were the main maintainability and standards findings, 13 in total (3 High, 5 Medium, 5 Low).
**How to apply:** on a re-audit, check which of these are fixed and report only what remains or is new.

See [[task-module-locations-and-baseline]].
