---

description: "Task list for Task Priority feature implementation"
---

# Tasks: Task Priority

**Input**: Design documents from `/specs/001-task-priority/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/tasks-api.md, quickstart.md

**Tests**: INCLUDED. The project constitution (Principle IV) and `CLAUDE.md` require tests for every code change. Every test task below MUST be done by invoking the `write-tests` skill (Skill tool), not by hand-writing tests, and existing tests MUST NOT be removed. Invoke the skill right after each implementation task that adds or changes a method, endpoint, service or component (as `CLAUDE.md` requires), covering that change; the test tasks listed after a story's implementation tasks name the tests the invocations must produce and are checked off once those tests exist and pass.

**Organization**: Tasks are grouped by user story. Backend and frontend tasks touch different files and can run in parallel where marked [P].

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story the task belongs to (US1 to US5, as numbered in spec.md)
- Include exact file paths in descriptions

## Path Conventions

- Backend main: `backend/src/main/java/com/technizer/taskapi/task/`
- Backend tests: `backend/src/test/java/com/technizer/taskapi/task/`
- Frontend: `frontend/src/app/tasks/`
- Field definitions are cited as `data-model.md` `Entity.field`; do not restate them in code comments or tasks.

---

## Phase 1: Setup

**Purpose**: Confirm a green baseline before changing anything.

- [X] T001 Run `mvn test` in `backend/` and `npx ng test --watch=false --browsers=ChromeHeadless` in `frontend/`; record that all existing tests pass so later failures are attributable to this feature

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The `Priority` type and the new `Task` attribute that every story uses.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Create `Priority` enum (LOW, MEDIUM, HIGH with rank 1 to 3 and a case-insensitive, trimming `parse(String)` that throws `IllegalArgumentException` listing the allowed values for blank or unknown text) in `backend/src/main/java/com/technizer/taskapi/task/Priority.java`, per data-model.md `Priority`
- [X] T003 Add the `priority` attribute to `Task` in `backend/src/main/java/com/technizer/taskapi/task/Task.java` with getter and setter, per data-model.md `Task.priority` (default MEDIUM, never null, stored as text, database default `'MEDIUM'`); keep the existing constructors unchanged (depends on T002)
- [X] T004 [P] Add a `Priority` type (`'LOW' | 'MEDIUM' | 'HIGH'`) and a `priority` property to the `Task` interface in `frontend/src/app/tasks/task.service.ts`, keeping it in sync with the backend JSON per data-model.md `Task.priority`
- [X] T005 [P] Use the `write-tests` skill to create `backend/src/test/java/com/technizer/taskapi/task/PriorityTest.java` covering `Priority.parse` (any case, surrounding spaces, blank, null-safe failure, unknown value) and rank ordering (depends on T002)
- [X] T006 Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskRepositoryTest.java` so a saved `Task` without an explicit priority persists and reloads as MEDIUM (depends on T003)

**Checkpoint**: Foundation ready; user stories can begin.

---

## Phase 3: User Story 1 - Set a priority on a task (Priority: P1) 🎯 MVP

**Goal**: A signed-in user can choose Low, Medium or High when creating or editing a task, and sees it in the list.

**Independent Test**: Create a task with High, reload the list and confirm it shows High; edit it to Low and confirm that persists; create one with no priority and confirm Medium (spec Story 1 scenarios 1 to 4).

### Implementation for User Story 1

- [ ] T007 [US1] In `backend/src/main/java/com/technizer/taskapi/task/TaskService.java` add a `create(ownerId, title, description, dueDate, Priority)` overload (null priority gives MEDIUM, per data-model.md `Task.priority`) and make the existing 4-argument `create` delegate to it so current callers and tests still compile
- [ ] T008 [US1] In `backend/src/main/java/com/technizer/taskapi/task/TaskService.java` add an `update(ownerId, taskId, title, description, dueDate, Priority)` overload (null priority leaves the stored value unchanged; apply priority only after the existing ownership check) and make the existing 5-argument `update` delegate to it (depends on T007)
- [ ] T009 [US1] In `backend/src/main/java/com/technizer/taskapi/task/TaskController.java` add an optional `priority` property, typed as Jackson `JsonNode`, to `CreateTaskRequest` and `UpdateTaskRequest`, and a private helper that maps it per research.md R3 and contracts/tasks-api.md: missing gives `null`, explicit JSON null, blank, non-text or unknown gives `IllegalArgumentException` via `Priority.parse` (so a 400), valid gives the `Priority`; pass the result to the new service overloads (depends on T008)
- [ ] T010 [P] [US1] In `frontend/src/app/tasks/task.service.ts` add an optional `priority` argument to `create` and `update`, including `priority` in the request body only when provided (depends on T004)
- [ ] T011 [US1] In `frontend/src/app/tasks/task-list.component.ts` add a priority select (Low, Medium, High; default Medium) to the new-task row, a priority select on each task row that calls `TaskService.update` with the chosen priority, and a visible priority label on each task; keep inline styles compact for the 2 kB stylesheet budget (depends on T010)

### Tests for User Story 1

- [ ] T012 [US1] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskServiceTest.java`: create with and without priority, update with a new priority, update with null leaves it unchanged (depends on T008)
- [ ] T013 [US1] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskControllerTest.java`: POST and PUT with `low`, `HIGH`, omitted, explicit `null`, `""` and `"urgent"` (200 versus 400 with an `error` body, task unchanged on 400) (depends on T009)
- [ ] T014 [P] [US1] Use the `write-tests` skill to extend `frontend/src/app/tasks/task.service.spec.ts`: `create` and `update` send `priority` when given (depends on T010)
- [ ] T015 [US1] Use the `write-tests` skill to extend `frontend/src/app/tasks/task-list.component.spec.ts`: adding a task with a chosen priority, changing a task's priority, and rendering the label (depends on T011)

**Checkpoint**: Priorities can be set, edited and seen. This is the MVP.

---

## Phase 4: User Story 4 - Existing data and clients keep working (Priority: P1)

**Goal**: Old tasks read as Medium, clients that never send a priority keep working, and previously returned information is unchanged.

**Independent Test**: With a client that sends no priority, create, update and list tasks; every call succeeds and every earlier response property is still present (spec Story 4 scenarios 1 to 4).

> Mostly verification of the additive design from Phases 2 and 3, so it is mostly test tasks. If a test exposes a regression, fix it in the file named by the failing behaviour.

- [ ] T016 [P] [US4] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskControllerTest.java`: old-style POST and PUT bodies (no `priority`) succeed, PUT leaves priority unchanged, and the list response still contains `id`, `title`, `description`, `completed`, `dueDate`, `ownerId` and `createdAt` with unchanged types (contracts/tasks-api.md, compatibility guarantees 1 and 2)
- [ ] T017 [P] [US4] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskServiceTest.java`: the original 4-argument `create` and 5-argument `update` overloads behave as before (MEDIUM for create, priority unchanged for update)
- [ ] T018 [US4] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskRepositoryTest.java`: a row inserted without a priority value (native insert) reads back as MEDIUM, per data-model.md `Task.priority` (FR-004) (depends on T006)
- [ ] T019 [P] [US4] Use the `write-tests` skill to extend `frontend/src/app/tasks/task.service.spec.ts`: `update` called without a priority sends a body with no `priority` key
- [ ] T020 [P] [US4] Use the `write-tests` skill to extend `frontend/src/app/tasks/task-list.component.spec.ts`: changing the due date does not send or alter priority
- [ ] T021 [US4] Confirm no existing test was removed or weakened by this feature (review `git diff` of all `*Test.java` and `*.spec.ts` files) and that both existing suites still pass

**Checkpoint**: Backward compatibility is proven for data, API and UI callers.

---

## Phase 5: User Story 2 - Filter by priority and completion status (Priority: P2)

**Goal**: Users can narrow the list by one priority, one completion status, or both (AND), and clear the filters.

**Independent Test**: With mixed tasks, apply each filter alone and combined and confirm only matching tasks appear; an empty result shows a "no matching tasks" message (spec Story 2 scenarios 1 to 5).

### Implementation for User Story 2

- [ ] T022 [US2] In `backend/src/main/java/com/technizer/taskapi/task/TaskService.java` add `listForOwner(ownerId, String priority, String completed)` that starts from the existing owner-scoped `findByOwnerId`, parses `priority` via `Priority.parse` and `completed` as `true` or `false` (anything else throws `IllegalArgumentException`), and filters with AND; null or blank parameters mean no filter and give the unchanged list (research.md R1, R6; data-model.md Query model); keep the existing `listForOwner(ownerId)` delegating to it
- [ ] T023 [US2] In `backend/src/main/java/com/technizer/taskapi/task/TaskController.java` change `list()` to accept optional `priority` and `completed` `@RequestParam` values typed as `String` (not enum or boolean, so bad values become 400 and not 500) and pass them to the service (depends on T022)
- [ ] T024 [P] [US2] In `frontend/src/app/tasks/task.service.ts` change `list()` to accept an optional filters object (`priority`, `completed`) and send only the provided values as query parameters; calling it with no argument must behave as today (depends on T004)
- [ ] T025 [US2] In `frontend/src/app/tasks/task-list.component.ts` add a priority filter (All, Low, Medium, High) and a status filter (All, Complete, Incomplete) above the list, re-fetch through `TaskService.list` on change, show a "no matching tasks" message when the result is empty, and let each filter be reset to "All"; store the active filter (and later sort) state in the component and make `refresh()` always pass it, so that adding, completing, deleting or editing a task (priority or due date) refreshes the list with the same filters still applied (spec Edge Cases) (depends on T024)

### Tests for User Story 2

- [ ] T026 [US2] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskServiceTest.java`: priority only, completed only, both, neither, case-insensitive values, blank values ignored, invalid values throw (depends on T022)
- [ ] T027 [US2] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskControllerTest.java`: `GET /api/tasks` with `priority`, `completed`, both, none, `?priority=&completed=` (blank) behaving like no filter, and `?completed=maybe` and `?priority=urgent` returning 400 (not 500) (depends on T023)
- [ ] T028 [P] [US2] Use the `write-tests` skill to extend `frontend/src/app/tasks/task.service.spec.ts`: `list` builds the correct query string for each filter combination and none (depends on T024)
- [ ] T029 [US2] Use the `write-tests` skill to extend `frontend/src/app/tasks/task-list.component.spec.ts`: changing each filter re-fetches with the right values, "All" clears, empty result message, and after add, complete, delete and priority edit the next `list` call still carries the active filters (depends on T025)

**Checkpoint**: Filtering works independently of sorting.

---

## Phase 6: User Story 3 - Sort by priority (Priority: P3)

**Goal**: Users can opt in to sorting by priority (High, Medium, Low), reverse it, and return to the original order.

**Independent Test**: With all three priorities present, sort and see High, Medium, Low; reverse and see Low, Medium, High; clear and see the original order; ties keep their original order (spec Story 3 scenarios 1 to 4).

### Implementation for User Story 3

- [ ] T030 [US3] In `backend/src/main/java/com/technizer/taskapi/task/TaskService.java` add `listForOwner(ownerId, priority, completed, String sort, String order)`: filter as in T022, then, only when `sort` equals `priority` (case-insensitive), apply a stable sort by `Priority` rank (`order` `desc` or absent gives High first, `asc` gives Low first); blank `sort` or `order` are treated as absent and `order` without `sort` is ignored (contracts/tasks-api.md); any other non-blank `sort` or `order` value throws `IllegalArgumentException`; no `sort` leaves the order untouched (research.md R2); make the 3-argument overload delegate to it (depends on T022)
- [ ] T031 [US3] In `backend/src/main/java/com/technizer/taskapi/task/TaskController.java` add optional `sort` and `order` `String` request parameters to `list()` and pass them through (depends on T023, T030)
- [ ] T032 [P] [US3] In `frontend/src/app/tasks/task.service.ts` extend the `list()` filters object with optional `sort` and `order`, sent only when provided (depends on T024)
- [ ] T033 [US3] In `frontend/src/app/tasks/task-list.component.ts` add a sort control (None, Priority high to low, Priority low to high) that re-fetches through `TaskService.list`; "None" restores the original order (depends on T025, T032)

### Tests for User Story 3

- [ ] T034 [US3] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskServiceTest.java`: desc and asc order, stability for equal priorities in both directions, sort combined with filters, no sort leaves repository order, blank `sort`/`order` ignored, `order` without `sort` ignored, invalid `sort` or `order` throws, and a 500-task mixed list is filtered and sorted correctly (SC-006 correctness at scale; the timing check is quickstart scenario 11) (depends on T030)
- [ ] T035 [US3] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskControllerTest.java`: `sort=priority`, `sort=priority&order=asc`, combined with filters, blank `sort`/`order` behaving like absent, and invalid values giving 400 (depends on T031)
- [ ] T036 [P] [US3] Use the `write-tests` skill to extend `frontend/src/app/tasks/task.service.spec.ts`: `list` sends `sort` and `order` only when provided (depends on T032)
- [ ] T037 [US3] Use the `write-tests` skill to extend `frontend/src/app/tasks/task-list.component.spec.ts`: choosing each sort option re-fetches with the right values and "None" clears it (depends on T033)

**Checkpoint**: Sorting works alone and together with filters.

---

## Phase 7: User Story 5 - Users only see their own tasks (Priority: P1)

**Goal**: No filter, sort or edit can expose or change another user's task.

**Independent Test**: With two users each holding tasks of every priority, apply every filter and sort as each user and confirm no result includes the other's tasks; a priority edit on another user's task is rejected (spec Story 5 scenarios 1 and 2).

> Scheduled after Stories 2 and 3 because it verifies their list behaviour. The update-ownership part already ships with Story 1 (T008) and is re-verified here.

- [ ] T038 [US5] Review `backend/src/main/java/com/technizer/taskapi/task/TaskService.java` and confirm that listing only ever starts from `findByOwnerId(ownerId)` and that `update` writes the new priority only after the existing ownership check; fix any deviation (research.md R7)
- [ ] T039 [US5] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskServiceTest.java`: with two owners' tasks, every filter and sort combination returns only the caller's tasks, and `findAll` is never used for listing (depends on T030, T038)
- [ ] T040 [US5] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskServiceTest.java`: updating another user's task with a new priority throws `NoSuchElementException`, nothing is saved and the stored priority is unchanged (depends on T038)
- [ ] T041 [US5] Use the `write-tests` skill to extend `backend/src/test/java/com/technizer/taskapi/task/TaskControllerTest.java`: `PUT` by a non-owner returns 404 with an `error` body, and filtered or sorted `GET` calls pass the authenticated user's id (not a request-supplied one) to the service (depends on T031)

**Checkpoint**: Ownership is verified for every new capability.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Whole-feature verification.

- [ ] T042 Run `npx ng build --configuration development` and a production `npx ng build` in `frontend/` and confirm the `task-list.component` inline styles stay within the 2 kB warning and 4 kB error budget
- [ ] T043 Run the full backend (`mvn test`) and frontend (`npx ng test --watch=false --browsers=ChromeHeadless`) suites and confirm everything passes
- [ ] T044 Run the scenarios in `specs/001-task-priority/quickstart.md` against the running app (API and UI) and confirm each expected outcome, including blank query values (scenario 10) and the 500-task timing check against SC-006 (scenario 11)
- [ ] T045 Confirm via `git diff` that no secrets or `application.properties` were touched and that `backend/pom.xml` and `frontend/package.json` gained no new dependencies (constitution Principles V and the dependency rule)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks all stories.
- **US1 (Phase 3)**: after Foundational.
- **US4 (Phase 4)**: after US1 (it verifies the overloads and request handling added there).
- **US2 (Phase 5)**: after Foundational; independent of US1 for filtering, but it shares files with US1, so run it after US1 to avoid merge conflicts.
- **US3 (Phase 6)**: after US2 (extends the same list method and controller parameters).
- **US5 (Phase 7)**: after US2 and US3.
- **Polish (Phase 8)**: after all stories.

### Within Each Story

Service, then controller, then tests (backend). Frontend service, then component, then specs.
Backend and frontend chains are independent and can proceed in parallel.

### Parallel Opportunities

- T002 and T004 (different sides); T005 alongside T003/T004.
- In US1: the frontend chain (T010, T011, T014, T015) can run alongside the backend chain (T007 to T009, T012, T013).
- Same pattern in US2 (T024, T025, T028, T029 versus T022, T023, T026, T027) and US3 (T032, T033, T036, T037 versus T030, T031, T034, T035).
- In US4: T016, T017, T019 and T020 touch different files.

---

## Parallel Example: User Story 1

```text
# Backend chain                         # Frontend chain
T007 -> T008 -> T009 -> T012, T013      T010 -> T011 -> T014, T015
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phases 1 and 2.
2. Phase 3 (US1). Stop and validate: set, edit and view priorities.
3. Then Phase 4 (US4) before any release, because breaking existing clients is release-blocking.

### Incremental Delivery

1. Foundation, then US1 and US4: safe, backward-compatible priorities (releasable).
2. Add US2 (filters), then US3 (sorting), each releasable.
3. US5 verifies ownership across the new list capabilities before the final polish.

---

## Notes

- Run `write-tests` immediately after each implementation task that changes code, not only at the end of a story; the listed test tasks are the checklist of what those runs must cover (`CLAUDE.md`, constitution Principle IV).
- Out of scope (documented, not fixed here): `markCompleted` and `delete` lack ownership checks; they do not touch priority.
- Commit after each task or logical group using Conventional Commits (`feat:`, `test:`); never push directly to `main` (work is on branch `001-task-priority`).
