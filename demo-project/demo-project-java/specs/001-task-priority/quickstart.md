# Quickstart: Validating Task Priority

Runnable checks that prove the feature works end to end. Details of the API are in
[contracts/tasks-api.md](contracts/tasks-api.md) and of the data in [data-model.md](data-model.md).

## Prerequisites

- JDK 17 and Maven; Node and npm; Chrome for headless frontend tests.
- Two registered users (A and B) and a bearer token for each, obtained through the existing login
  flow. Below, `$TOKEN_A` and `$TOKEN_B` stand for those tokens.

## Run automated tests

```bash
cd backend && mvn test
cd frontend && npx ng test --watch=false --browsers=ChromeHeadless
```

Expected: all existing tests still pass, plus the new priority tests.

## Run the app

```bash
cd backend && mvn spring-boot:run        # API on http://localhost:8080
cd frontend && npx ng serve              # UI on http://localhost:4200
```

## Manual API scenarios

1. **Default priority (FR-003, SC-004)**: create a task without `priority`.
   Expected: 200, response contains `"priority": "MEDIUM"`.
2. **Set priority (FR-001)**: create a task with `"priority": "high"`.
   Expected: `"priority": "HIGH"`.
3. **Edit priority (FR-002)**: `PUT` that task with `"priority": "low"`.
   Expected: `"priority": "LOW"`.
4. **Omitted on update (FR-005)**: `PUT` the same task with only `title`.
   Expected: 200, priority still `LOW`.
5. **Invalid values (FR-006, FR-006a)**: create or update with `"priority": "urgent"`, `""`, and `null`.
   Expected: each returns 400 with an `error` message; the task is unchanged.
6. **Filtering (FR-008 to FR-011)**: with tasks of mixed priority and completion state, call
   `GET /api/tasks?priority=high`, `?completed=false`, and `?priority=high&completed=false`.
   Expected: only matching tasks; a bad value such as `?completed=maybe` returns 400.
7. **Sorting (FR-012 to FR-014)**: `GET /api/tasks?sort=priority` then `?sort=priority&order=asc`.
   Expected: High, Medium, Low then Low, Medium, High; equal priorities in the original order.
   Without `sort`, the order matches the pre-feature order.
8. **Ownership (FR-017, FR-018)**: as user B, list with every filter and sort; as user B, `PUT` a
   task owned by A.
   Expected: B never sees A's tasks; the `PUT` returns 404 and A's task is unchanged.
9. **Backward compatibility (FR-015, FR-016)**: confirm an old-style request body (no `priority`)
   works for create, update and list, and that all earlier response properties are still present.
10. **Blank query values**: call `GET /api/tasks?priority=&completed=&sort=`.
    Expected: 200 with the same result as `GET /api/tasks` (blank values are ignored).
11. **Scale (SC-006)**: as user A, create 500 tasks of mixed priority and completion state, then call
    `GET /api/tasks?priority=high&completed=false&sort=priority`.
    Expected: correct results, and the response (and the UI update when the same filters are chosen)
    appears in under 1 second.

## Manual UI scenarios

1. Add a task choosing High; it shows High in the list.
2. Change a task's priority from its row; the change persists after reload.
3. Use the priority filter and status filter together; the list narrows as expected, "All" clears each.
4. Choose sort by priority, then reverse it; clear it and confirm the original order returns.
5. Apply a filter that matches nothing; a "no matching tasks" message appears.

## Pass criteria

All automated tests pass and every scenario above gives the expected result.
