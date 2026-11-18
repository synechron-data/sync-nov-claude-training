---
name: task-module-idor-and-validation
description: State of object-level authz and input validation in backend task module (TaskService/TaskController) as of 2026-11-18 audit
metadata:
  type: feedback
---

Audit 2026-11-18 of backend/src/main/java/com/technizer/taskapi/task/:
- `TaskService.delete` has NO ownership check (deleteById only) — any user deletes any task; silent 200 for any id. Critical IDOR.
- `TaskService.markCompleted` has NO ownership check and returns the full victim Task (title/description/ownerId) — write + read IDOR. Code comment marks it as a deliberate demo TODO.
- `TaskService.update` DOES check ownership and returns 404 for foreign tasks (good reference pattern for fixes).
- Task IDs are sequential IDENTITY longs, so enumeration is trivial.
- No mass assignment: request DTOs are records with only title/description/dueDate. But controllers return the `Task` entity directly (exposes ownerId).
- Bean Validation is inert (no `@Valid` anywhere); `create()` accepts blank title; no length limits (description default VARCHAR(255) -> 500 on overflow).

**Why:** Re-audits should verify whether these were fixed rather than rediscovering them.
**How to apply:** On next task-module audit, check these exact methods first; if fixed, update this note. See [[jwt-validation-fail-open]] and [[config-files-access-denied]].
