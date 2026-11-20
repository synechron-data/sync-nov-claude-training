---
name: task-module-perf-findings
description: Known performance issues in the task module (unindexed ownerId, unpaginated list endpoints, refetch-after-mutate in Angular, no OnPush) as of the 2026-11-18 audit
metadata:
  type: project
---

Audit of backend `task/` and frontend `tasks/` on 2026-11-18. Verify against the code before reuse.

- `Task.ownerId` is a plain `Long` column with no `@Index` and no FK to `app_user`; every list query filters on it (`TaskRepository.findByOwnerId`, `findByOwnerIdAndCompletedTrue`).
- `TaskController.list` and `/completed` return unbounded `List<Task>` (no `Pageable`).
- There is no N+1 risk today: Task has no JPA associations, only a raw `ownerId`.
- No `@Transactional` anywhere in `TaskService`. `markCompleted` and `update` do `findById` then `save` (redundant, dirty checking would do).
- `TaskService.delete` ignores ownerId (correctness/security, out of lane).
- Frontend `TaskListComponent` has no `OnPush`. After every mutation it calls `refresh()`, which refetches the full list. `isOverdue()` and `today()` run per row on every CD cycle. `*ngFor` has no `trackBy`. The `ngModelChange` on the date input fires an immediate PUT with no debounce.
- No `src/main/resources` files were found (no application.properties or migrations), so schema and index strategy could not be inspected. Backend `mvn test` and frontend tests were not run in this audit.
