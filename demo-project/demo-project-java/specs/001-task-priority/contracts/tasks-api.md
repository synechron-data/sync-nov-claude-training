# Contract: Tasks API (priority additions)

Base path `/api/tasks`. All endpoints require the existing JWT bearer token; the caller's user id is
taken from the token and every operation is scoped to that user. Only the differences from the
current behaviour are described. Errors use the existing body `{ "error": "<message>" }`.

## Task representation (response)

Existing properties are unchanged. One property is added:

```json
{
  "id": 1,
  "title": "Write report",
  "description": "Q4",
  "completed": false,
  "dueDate": "2026-12-01",
  "ownerId": 7,
  "createdAt": "2026-11-18T10:15:30Z",
  "priority": "MEDIUM"
}
```

`priority` is always present and is one of `"LOW"`, `"MEDIUM"`, `"HIGH"`.

## GET /api/tasks

Optional query parameters (all case-insensitive for values):

| Parameter   | Values                  | Effect                                                    |
|-------------|-------------------------|-----------------------------------------------------------|
| `priority`  | `low`, `medium`, `high` | Only tasks with that priority                             |
| `completed` | `true`, `false`         | Only completed or only incomplete tasks                   |
| `sort`      | `priority`              | Sort by priority; otherwise the order is unchanged        |
| `order`     | `desc` (default), `asc` | `desc`: High, Medium, Low. `asc`: Low, Medium, High       |

- Parameters combine with AND; sorting is applied to the filtered set and is stable.
- A parameter that is present but blank (for example `?priority=` or `?sort=`) is treated as if it
  were absent. This differs from a blank `priority` in a create or update body, which is rejected.
- `order` without `sort` is accepted and ignored.
- No parameters gives exactly the previous response (plus `priority` on each task).
- **200**: JSON array of tasks (empty array if none match).
- **400**: unknown value for `priority`, `completed`, `sort` or `order`.
- **403**: missing or invalid token (existing behaviour).

`GET /api/tasks/completed` is unchanged.

## POST /api/tasks

Request body gains an optional `priority`:

```json
{ "title": "Write report", "description": "Q4", "dueDate": "2026-12-01", "priority": "high" }
```

| `priority` in body            | Result                                  |
|-------------------------------|-----------------------------------------|
| omitted                       | Task created with `MEDIUM`              |
| `low` / `medium` / `high` (any case) | Task created with that priority  |
| `null`, `""`, or any other value | **400**, nothing created             |

**200**: the created task (existing behaviour for a successful create).

## PUT /api/tasks/{id}

Request body gains an optional `priority`, same value rules as create, except:

| `priority` in body            | Result                                          |
|-------------------------------|-------------------------------------------------|
| omitted                       | Update succeeds; stored priority unchanged      |
| valid value (any case)        | Priority replaced                               |
| `null`, `""`, or any other value | **400**, task unchanged                      |

- **404** if the task does not exist or belongs to another user (nothing changes).

## Unchanged endpoints

`PATCH /api/tasks/{id}/complete` and `DELETE /api/tasks/{id}` are not modified.

## Backward-compatibility guarantees

1. A client that never sends `priority` can create, update and list tasks without any change.
2. Every property previously returned is still returned with the same name, type and format.
3. Unknown properties in responses are additive only; no property is removed or renamed.
