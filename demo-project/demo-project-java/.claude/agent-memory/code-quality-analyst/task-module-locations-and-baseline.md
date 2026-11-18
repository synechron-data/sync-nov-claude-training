---
name: task-module-locations-and-baseline
description: Where key task-module logic lives and the CLAUDE.md compliance baseline (what was already compliant)
metadata:
  type: feedback
---

**Where things live** (paths relative to the repo root `demo-project-java/`):
- Backend task module: `backend/src/main/java/com/technizer/taskapi/task/` holds `TaskController`, `TaskService`, `TaskRepository`, `Task`, `Priority`. Tests mirror it under `backend/src/test/java/.../task/`: `TaskServiceTest`, `TaskControllerTest`, `TaskRepositoryTest`, `PriorityTest`.
- Error mapping: `backend/src/main/java/com/technizer/taskapi/exception/GlobalExceptionHandler.java`. `IllegalArgumentException` maps to 400, `NoSuchElementException` to 404, `HttpMessageNotReadableException` to 400, and everything else to a generic 500.
- Frontend: `frontend/src/app/tasks/` holds `task.service.ts` (HTTP calls plus the `Task` and `Priority` types), `task-list.component.ts` (inline template and styles), and the `*.spec.ts` files.
- Extra standards docs: `CLAUDE.md` at the repo root, `backend/CLAUDE.md` and `frontend/CLAUDE.md`. The sub-CLAUDE.md files document intentional quirks: inert Bean Validation, 403 instead of 401, and `deleteById` being a silent no-op. They also say controllers may return entities directly, so do not flag that as High.

**CLAUDE.md compliance baseline** (all compliant at 2026-11-18):
- Constructor injection: yes, in backend and frontend. No `@Autowired` fields.
- Standalone components: yes. `TaskListComponent` has `standalone: true`.
- Services own HTTP: yes. Only `task.service.ts` uses `HttpClient`.
- DTOs as records: yes (the request records). Responses return the entity.
- Tests: partial, see [[task-module-known-issues]].

**Why:** this avoids re-deriving the layout and avoids flagging already-compliant standards.
**How to apply:** start a task-module audit here, then verify the files still exist.
