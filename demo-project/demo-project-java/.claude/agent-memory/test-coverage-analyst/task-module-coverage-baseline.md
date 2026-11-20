---
name: task-module-coverage-baseline
description: Known test gaps and baselines for the task module (backend + frontend) as of 2026-11-18 audit; where tests live and how to run them
metadata:
  type: project
---

Baselines (2026-11-18): backend `mvn test` = 74 tests green (task pkg: TaskServiceTest 20, TaskRepositoryTest 14, TaskControllerTest 9, PriorityTest). No jacoco configured. Frontend karma (use `--browsers=ChromeHeadless`) = 28 green; Stmts 89%, Branches 100%, Funcs 80% (16/20).

Known gaps: TaskService.markCompleted/delete/listForOwner and TaskController list/create-validation/complete/delete have zero tests; no MockMvc test exists anywhere in backend (no routing/403/status-code/JSON-binding tests); TaskServiceTest has an empty `placeholder()` test. Frontend TaskListComponent.complete/remove untested; no error-path (4xx/5xx) tests except one 400 on TaskService.update.

**Why:** markCompleted/delete lack ownership checks (deliberate training bugs per CLAUDE.md), so tests there are security-adjacent (Critical).
**How to apply:** on re-audit, re-check these first; they may have been filled in by the write-tests skill. Never suggest removing the placeholder test, only adding real ones.
