# Feature Specification: Task Priority

**Feature Branch**: `001-task-priority`

**Created**: 2026-11-18

**Status**: Draft

**Input**: User description: "Let users set a priority of Low, Medium, or High on a task when creating or editing it. In the task list, users can filter tasks by priority and by completion status, and sort by priority. Tasks that existed before this change are treated as Medium priority. Clients that don't send a priority must keep working, existing API responses must not break, and users only ever see their own tasks."

## Clarifications

### Session 2026-11-18

- Q: Can a user remove a task's priority so it has none, or does every task always keep Low, Medium, or High? → A: Every task always has a priority; it can never be cleared. An empty or null priority in an update is rejected as invalid, and a user who wants to "reset" picks Medium.
- Q: When no sort is chosen, what order does the list use, and is priority sorting default or opt-in? → A: With no sort chosen, the list keeps its current (pre-feature) order. Sorting by priority is opt-in (High → Medium → Low), can be reversed, and tasks with equal priority keep the current order.
- Q: How do the priority and completion-status filters combine, and can multiple priorities be chosen at once? → A: The filters combine with AND (a task must match both). Each filter takes a single value at a time (priority: Low, Medium, High, or All; status: Complete, Incomplete, or All); multi-select priority is out of scope.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Set a priority on a task (Priority: P1)

A signed-in user creates a new task or edits an existing one and chooses a priority of Low, Medium, or High. The chosen priority is saved with the task and shown wherever the task appears.

**Why this priority**: Without the ability to record a priority, filtering and sorting have nothing to work with. This is the foundation and, on its own, already lets users mark what matters.

**Independent Test**: Create a task with priority High, reload the task list, and confirm the task still shows High; then edit it to Low and confirm the change persists.

**Acceptance Scenarios**:

1. **Given** a signed-in user on the create-task form, **When** they choose High and save, **Then** the task is created with High priority and displays it.
2. **Given** a signed-in user with an existing task, **When** they edit the task and change priority to Low, **Then** the task saves with Low priority and displays it.
3. **Given** a signed-in user on the create-task form, **When** they save without choosing a priority, **Then** the task is created with Medium priority.
4. **Given** a task created before this feature existed, **When** the user views it, **Then** it displays Medium priority.

---

### User Story 2 - Filter the task list by priority and completion status (Priority: P2)

A user narrows the task list to just the tasks they care about right now, by choosing a priority (Low, Medium, or High), a completion status (complete or incomplete), or both together.

**Why this priority**: Filtering is the main payoff of priorities: it lets users focus. It depends on Story 1 but is useful even without sorting.

**Independent Test**: With a mix of tasks across priorities and completion states, apply each filter alone and in combination and confirm only matching tasks are listed.

**Acceptance Scenarios**:

1. **Given** tasks of mixed priorities, **When** the user filters by High, **Then** only High-priority tasks are listed.
2. **Given** a mix of completed and incomplete tasks, **When** the user filters by incomplete, **Then** only incomplete tasks are listed.
3. **Given** a mix of tasks, **When** the user filters by High and incomplete, **Then** only tasks that are both High priority and incomplete are listed.
4. **Given** an active filter, **When** the user clears it, **Then** all of the user's tasks are listed again.
5. **Given** a filter that matches no tasks, **When** it is applied, **Then** the user sees a clear "no matching tasks" message rather than an error or blank screen.

---

### User Story 3 - Sort the task list by priority (Priority: P3)

A user orders the task list by priority so the most important tasks appear first (High, then Medium, then Low), and can reverse the order.

**Why this priority**: Sorting is a convenience on top of filtering; users can already find what matters without it.

**Independent Test**: With tasks of all three priorities, apply the priority sort and confirm the visible order is High, Medium, Low; reverse it and confirm Low, Medium, High.

**Acceptance Scenarios**:

1. **Given** tasks of mixed priorities and no sort chosen, **When** the user views the list, **Then** tasks appear in the same order as before this feature.
2. **Given** tasks of mixed priorities, **When** the user sorts by priority, **Then** High tasks appear before Medium, and Medium before Low, with equal-priority tasks in their original relative order.
3. **Given** the list is sorted by priority, **When** the user reverses the order, **Then** Low tasks appear first, then Medium, then High.
4. **Given** filters are active, **When** the user sorts by priority, **Then** the sort applies only to the filtered tasks.

---

### User Story 4 - Existing data and clients keep working (Priority: P1)

People and systems that used the task list before this change see no disruption: old tasks carry a sensible default priority, clients that never send a priority continue to create and update tasks successfully, and the information previously returned for each task is still returned in the same form.

**Why this priority**: Breaking existing users or clients would be a regression worse than not shipping the feature, so this is a release-blocking requirement.

**Independent Test**: Using a client written before this feature (which sends no priority), create, update, and list tasks and confirm every operation succeeds and previously available task information is unchanged.

**Acceptance Scenarios**:

1. **Given** tasks that existed before this feature, **When** the user lists them, **Then** each is treated as Medium priority and all its previous information is intact.
2. **Given** a client that does not send a priority, **When** it creates a task, **Then** the task is created successfully with Medium priority.
3. **Given** a client that does not send a priority, **When** it updates an existing task, **Then** the update succeeds and the task's current priority is left unchanged.
4. **Given** a client built before this feature, **When** it reads task data, **Then** everything it relied on is still present and unchanged; the priority is only an addition.

---

### User Story 5 - Users only see their own tasks (Priority: P1)

Whatever filter or sort a user applies, the results contain only that user's own tasks. One user can never see, or infer the existence of, another user's tasks, and cannot set or change the priority of another user's task.

**Why this priority**: Privacy of task data is a baseline expectation; new filtering and sorting capabilities must not weaken it.

**Independent Test**: With two users who each have tasks of every priority, apply every filter and sort combination as each user and confirm no result ever includes the other user's tasks.

**Acceptance Scenarios**:

1. **Given** two users with tasks, **When** either applies any filter or sort, **Then** only their own tasks appear.
2. **Given** a task owned by another user, **When** a user attempts to change its priority, **Then** the attempt is rejected and the task is unchanged.

---

### Edge Cases

- A user (or client) submits an invalid priority value (for example "Urgent", an empty string, or an explicit null): the request is rejected with a clear message and the task is not saved or changed. This differs from omitting the priority, which is allowed and leaves the current priority as is.
- A user wants to "remove" a priority: this is not possible; the edit form offers only Low, Medium, and High, and Medium serves as the neutral/reset choice.
- A user submits a priority with different capitalization (for example "high"): the value is accepted regardless of capitalization and stored as the standard Low/Medium/High form.
- Many tasks share the same priority: tasks within the same priority keep the list's pre-feature order, so the list does not shuffle between views.
- The user clears the priority sort: the list returns to the pre-feature order.
- A user has no tasks, or no tasks match the filter: an empty-state message is shown.
- A user edits a task's priority while a priority filter is active: the list reflects the change right away (the task disappears from the filtered view if it no longer matches).
- Completing a task while a completion-status filter is active: the list reflects the change right away and keeps the active filters and sort. (Un-completing a task is not part of this feature.)
- Adding, editing or deleting a task while filters or a sort are active: the list refreshes with the same filters and sort still applied.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to choose a priority of Low, Medium, or High when creating a task.
- **FR-002**: Users MUST be able to change a task's priority when editing it.
- **FR-003**: When a task is created without a priority, the system MUST assign Medium priority.
- **FR-004**: Tasks that existed before this feature MUST be treated as Medium priority.
- **FR-005**: When an update omits the priority entirely, the system MUST leave the task's existing priority unchanged.
- **FR-006**: The system MUST reject any priority other than Low, Medium, or High, including an explicitly empty or null priority, with a clear error and MUST NOT change the task.
- **FR-006a**: Every task MUST always have exactly one priority; a priority cannot be cleared or left unset once a task exists.
- **FR-007**: Each task MUST display its priority in the task list.
- **FR-008**: Users MUST be able to filter the task list by a single priority at a time (Low, Medium, or High), or choose "All" to apply no priority filter.
- **FR-009**: Users MUST be able to filter the task list by a single completion status at a time (complete or incomplete), or choose "All" to apply no status filter.
- **FR-010**: The priority filter and the completion-status filter MUST be combinable with AND logic: a task MUST be listed only if it satisfies every active filter.
- **FR-011**: Users MUST be able to clear filters (set each back to "All") to see all of their tasks again.
- **FR-012**: Users MUST be able to opt in to sorting the task list by priority, ordered High, then Medium, then Low, and MUST be able to reverse it.
- **FR-012a**: When the user has not chosen a sort, the task list MUST keep the order it had before this feature.
- **FR-013**: Sorting and filtering MUST work together; sorting applies to the filtered set.
- **FR-014**: Within tasks of equal priority, the system MUST keep them in the list's pre-feature order, in both the normal and reversed priority sort.
- **FR-015**: Clients that do not send a priority MUST be able to create, update, and list tasks without any change on their side.
- **FR-016**: All task information returned before this feature MUST continue to be returned in the same form; priority MUST be additive only.
- **FR-017**: A user MUST only ever see their own tasks in every view, including every filter and sort combination.
- **FR-018**: A user MUST NOT be able to read, set, or change the priority of another user's task.

### Key Entities *(include if feature involves data)*

- **Task**: A unit of work owned by one user. Existing attributes are unchanged; it gains a **priority** that is exactly one of Low, Medium, or High, defaulting to Medium.
- **Priority**: A three-level ordered value (Low < Medium < High) used for display, filtering, and sorting.
- **User**: The owner of tasks; scopes every task view and change.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can set or change a task's priority in no more than one additional step compared with creating or editing a task today.
- **SC-002**: A user can narrow the list to "incomplete High-priority tasks" in no more than 3 interactions.
- **SC-003**: 100% of tasks that existed before this feature appear as Medium priority with all their previous information intact.
- **SC-004**: 100% of operations performed by a client that sends no priority succeed exactly as they did before this feature.
- **SC-005**: In testing with at least two users, 0 results across all filter and sort combinations include another user's task.
- **SC-006**: With a list of 500 tasks, applying a filter or sort updates the visible list in under 1 second.

## Assumptions

- Priorities are exactly three fixed levels (Low, Medium, High); custom or numeric priorities are out of scope.
- Medium is the default priority for new tasks created without one and for pre-existing tasks.
- Completion status already exists on tasks; this feature only adds filtering by it.
- Filter and sort choices apply to the current view and are not required to persist across sessions.
- Filters combine with AND logic; selecting multiple priorities at once (for example "High or Medium") is out of scope for this version.
- Priority values are matched without regard to capitalization; any other text is invalid.
- Task ownership and sign-in already exist; this feature reuses them and adds no new roles or sharing.
- Notifications, due-date interactions, and bulk priority changes are out of scope.
