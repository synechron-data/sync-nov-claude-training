# Plan: Due dates on tasks

## Context
Users want to give a task a due date. Decisions confirmed with the user:
- **Date only.** Use `java.time.LocalDate`, sent over JSON as `"yyyy-MM-dd"`.
- **Optional field on the existing create/update APIs.** No new endpoint. `null` means no due date, or clears an existing one.
- **Past dates are allowed.** The UI highlights overdue tasks that aren't complete.
- **Backend and frontend** are both in scope.

Spring Boot's default Jackson setup already writes `java.time` types as ISO strings (`createdAt` relies on this today), and H2 builds the schema from the entities. So no config changes, new dependencies or `application.properties` edits are needed.

## Progress checklist

### 0. Setup
- [x] Commit this plan file (`docs: add plan for task due date`)
- [x] Create a feature branch (not `main`) — `feat/task-due-date`

### 1. Backend (`backend/src/main/java/com/technizer/taskapi/task/`)
- [x] `Task.java`: add nullable `private LocalDate dueDate;` with getter and setter. Leave the existing 3-arg constructor alone so current callers and tests keep compiling.
- [x] `TaskService.java`: `create(Long ownerId, String title, String description, LocalDate dueDate)` sets `dueDate` before saving.
- [x] `TaskService.java`: `update(Long ownerId, Long taskId, String title, String description, LocalDate dueDate)` calls `task.setDueDate(dueDate)` next to the title and description. This is a full replace, consistent with PUT semantics.
- [x] `TaskService.java`: change the existing signatures instead of adding overloads. The only callers are the controller and the tests.
- [x] `TaskController.java`: add `LocalDate dueDate` as a new last component on both `CreateTaskRequest` and `UpdateTaskRequest`.
- [x] `TaskController.java`: pass `request.dueDate()` through in `create(...)` and `update(...)`.
- [x] Check that an unparseable date (`"dueDate":"bad"`) returns 400. If the catch-all handler in `GlobalExceptionHandler` turns it into a 500, add a 400 handler for `HttpMessageNotReadableException`. (Added the handler; the live HTTP check is still in section 4.)
- [x] Update the existing calls to `create`/`update` and the record constructors in `TaskServiceTest`, `TaskControllerTest` and `TaskRepositoryTest` for the new argument. Change call signatures only; don't remove any tests.

### 2. Frontend (`frontend/src/app/tasks/`)
- [x] `task.service.ts`: add `dueDate: string | null` to the `Task` interface, keeping it in sync with the entity.
- [x] `task.service.ts`: `create(title, description, dueDate: string | null)` adds `dueDate` to the request body.
- [x] `task.service.ts`: add `update(id, title, description, dueDate)` that calls `PUT ${baseUrl}/${id}` (the service doesn't have one yet).
- [x] `task-list.component.ts`: add `<input type="date" [(ngModel)]="newDueDate">` to the new-task row, send it in `addTask()` (empty string → `null`), and reset it afterwards.
- [x] `task-list.component.ts`: show `Due: {{ task.dueDate | date:'mediumDate' }}` on each item when it's set.
- [x] `task-list.component.ts`: add a small `<input type="date">` per item; on `(change)` call `taskService.update(task.id, task.title, task.description, value || null)` and then `refresh()`.
- [x] `task-list.component.ts`: add an `isOverdue(task)` helper (not completed and `dueDate < today`). Compare `yyyy-MM-dd` strings built from the local date to avoid timezone shifts. Bind it with `[class.overdue]`.
- [x] Keep the added CSS to a couple of lines (`.overdue`, `.due`) to stay within the 2 kB per-component style budget.

### 3. Tests (invoke the `write-tests` skill after the code changes; don't hand-write them)
- [x] Invoke the `write-tests` skill for the backend changes
- [x] Invoke the `write-tests` skill for the frontend changes
- [x] `TaskServiceTest`: create stores the due date
- [x] `TaskServiceTest`: create works with a null due date
- [x] `TaskServiceTest`: update sets, changes and clears the due date
- [x] `TaskServiceTest`: updating another owner's task still throws
- [x] `TaskControllerTest`: create and update pass `dueDate` through to the service
- [x] `TaskRepositoryTest`: `dueDate` survives a save and reload
- [x] `GlobalExceptionHandlerTest` (new): 400 handler for malformed request bodies
- [x] Frontend: `TaskService` sends `dueDate` in the create and update bodies (`HttpTestingController`) — `task.service.spec.ts`
- [x] Frontend: `TaskListComponent.isOverdue` is covered — `task-list.component.spec.ts`
- [x] Frontend: `addTask` passes `null` for an empty date

### 4. Verification
- [x] `cd backend && mvn test`: all tests pass, old and new (41 tests; there is no `mvnw` in `backend/`)
- [x] `cd frontend && npx ng test --watch=false --browsers=ChromeHeadless`: all tests pass (28 tests)
- [x] `cd frontend && npx ng build`: checks strict templates and the style budget
- [x] Manual: `POST /api/tasks` with `{"title":"x","dueDate":"2026-12-01"}` returns `"dueDate":"2026-12-01"`
- [x] Manual: `PUT /api/tasks/{id}` with `"dueDate":null` clears it
- [x] Manual: `"dueDate":"bad"` returns 400, not 500 (also `2026-13-45` → 400; omitted field → `null`; past date accepted)
- [x] Manual (UI): set a date on a new task, change it inline, and confirm a past-dated incomplete task is highlighted as overdue (also confirmed: completing an overdue task removes the highlight; future dates stay unhighlighted)

### 5. Wrap-up
- [x] Commit the code as `feat(task): add optional due date to tasks` (separate `test:` commit if preferred) — `3522fc8` (feat), `4896d65` (test)
- [ ] PR description covers what changed, why and how to test
