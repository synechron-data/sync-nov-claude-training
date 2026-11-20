---
name: task-module-coverage-baseline
description: Known test gaps and baselines for the task module (backend + frontend); 2026-11-18 audit plus 2026-11-20 update after PR #5 (delete ownership fix)
metadata:
  type: project
---

Baselines (2026-11-18): backend `mvn test` = 74 tests green. No jacoco configured. Frontend karma (use `--browsers=ChromeHeadless`) = 28 green; Stmts 89%, Branches 100%, Funcs 80% (16/20).

Update 2026-11-20 (branch fix/issue-3, PR #5): TaskService.delete now ownership-scoped via TaskRepository.findByIdAndOwnerId. Counts: TaskServiceTest 32, TaskRepositoryTest 19, TaskControllerTest 9 (still no controller.delete test), new TaskDeleteOwnershipTest 5 (first MockMvc test in repo: standaloneSetup, real controller/service/GlobalExceptionHandler, mocked repo, principal via SecurityContextHolder; no security filter chain). spring-security-test is in the pom.

Known gaps still open: markCompleted (no ownership check, no tests) and listForOwner service tests; TaskController list/create/complete/delete direct tests; no @SpringBootTest/@WebMvcTest exercising SecurityConfig + JwtAuthFilter (so unauthenticated -> empty 403 is untested anywhere); `/api/tasks/abc` (type mismatch) hits GlobalExceptionHandler catch-all -> 500, untested. Two "same response for missing vs foreign" tests (service + MockMvc) are tautological because the repository is mocked (second one compares the same request with itself). TaskServiceTest has an empty `placeholder()` test. Frontend TaskListComponent.complete/remove untested; few 4xx/5xx tests.

**Why:** ownership checks are security-adjacent (Critical); delete is now fixed, markCompleted is the remaining deliberate bug.
**How to apply:** on re-audit, re-check these first. Never suggest removing the placeholder test, only adding real ones.
