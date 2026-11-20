# API reference

Base URL (development): `http://localhost:8080`. All request and response bodies are JSON.

See [architecture.md](architecture.md) for how authentication and error handling work.

## Authentication

Register or log in to get a JWT, then send it on every `/api/tasks` request:

```
Authorization: Bearer <token>
```

Tokens expire after `app.jwt.expiry-minutes` (default 60). A missing, malformed, bad-signature or expired token gets an **empty-body `403 Forbidden`**.

## Errors

Application errors share one shape:

```json
{ "error": "Task not found: 42" }
```

| Status | When |
|---|---|
| 400 | Business rule failed (`Invalid credentials`, `Email already registered: …`, `Title must not be blank`) or malformed body (`Malformed request body`) |
| 403 | No valid token (empty body, no JSON) |
| 404 | Task not found |
| 500 | Unexpected server error (`Unexpected server error`) |

## Auth endpoints

Public. No token needed.

### `POST /api/auth/register`

Creates a user and returns a token, so the client is logged in immediately.

Request:

```json
{ "email": "ada@example.com", "password": "s3cret", "displayName": "Ada" }
```

Response `200`:

```json
{ "token": "eyJhbGciOi..." }
```

Errors: `400` if the email is already registered.

### `POST /api/auth/login`

Request:

```json
{ "email": "ada@example.com", "password": "s3cret" }
```

Response `200`: `{ "token": "eyJhbGciOi..." }`

Errors: `400 Invalid credentials` for an unknown email or a wrong password. Both give the same message, so the response doesn't reveal which emails exist.

## Task endpoints

All require `Authorization: Bearer <token>`. Tasks are scoped to the authenticated user: the owner is taken from the token, never from the request.

### The Task object

```json
{
  "id": 7,
  "title": "Write docs",
  "description": "Architecture and API",
  "completed": false,
  "dueDate": "2026-12-01",
  "ownerId": 1,
  "createdAt": "2026-11-18T09:30:00.123Z",
  "priority": "MEDIUM"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | number | Generated |
| `title` | string | Required |
| `description` | string \| null | Optional |
| `completed` | boolean | Starts `false` |
| `dueDate` | `yyyy-MM-dd` \| null | Optional |
| `ownerId` | number | ID of the owning user |
| `createdAt` | ISO-8601 instant | Set on creation |
| `priority` | `LOW` \| `MEDIUM` \| `HIGH` | Defaults to `MEDIUM`. **Read-only for now**: no endpoint accepts it |

### `GET /api/tasks`

Lists the caller's tasks. Response `200`: `Task[]`.

### `GET /api/tasks/completed`

Lists only the caller's completed tasks. Response `200`: `Task[]`.

### `POST /api/tasks`

Request:

```json
{ "title": "Write docs", "description": "Architecture and API", "dueDate": "2026-12-01" }
```

`description` and `dueDate` are optional. Response `200`: the created `Task`.

> The `@NotBlank` annotation on `title` is not what enforces this (Bean Validation is inert, see [Known limitations](architecture.md#known-limitations)). `TaskService.create()` checks the title itself.

Errors:
- `400 Title must not be blank` if `title` is missing, empty or only whitespace. Nothing is saved.
- `400 Malformed request body` for invalid JSON or a `dueDate` that is not `yyyy-MM-dd`.

A non-blank title is stored exactly as given, with no trimming.

### `PUT /api/tasks/{id}`

Replaces `title`, `description` and `dueDate`. It is a full update of those three fields: omitting `description` or `dueDate` clears them. It does not change `completed` or `priority`.

Request:

```json
{ "title": "Write docs v2", "description": null, "dueDate": "2026-12-15" }
```

Response `200`: the updated `Task`.

Errors:
- `400 Title must not be blank`
- `404` if the task doesn't exist **or belongs to another user** (the same response either way, so IDs can't be probed)

### `PATCH /api/tasks/{id}/complete`

Marks the task completed. No body. Response `200`: the updated `Task`.

Errors: `404` if the task doesn't exist.

> Ownership is not currently checked on this endpoint, so a user can complete any user's task by ID. See [Known limitations](architecture.md#known-limitations).

### `DELETE /api/tasks/{id}`

Deletes the caller's own task. No body. Response `200` with an empty body.

Errors:
- `404 Task not found: {id}` if the task doesn't exist **or belongs to another user** (the same response either way, so IDs can't be probed). Nothing is deleted.

## Summary

| Method | Path | Auth | Body | Success |
|---|---|---|---|---|
| POST | `/api/auth/register` | no | `{email, password, displayName}` | `{token}` |
| POST | `/api/auth/login` | no | `{email, password}` | `{token}` |
| GET | `/api/tasks` | yes | | `Task[]` |
| GET | `/api/tasks/completed` | yes | | `Task[]` |
| POST | `/api/tasks` | yes | `{title, description?, dueDate?}` | `Task` |
| PUT | `/api/tasks/{id}` | yes | `{title, description?, dueDate?}` | `Task` |
| PATCH | `/api/tasks/{id}/complete` | yes | | `Task` |
| DELETE | `/api/tasks/{id}` | yes | | empty |

## Example session (curl)

```bash
# Register and capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"s3cret","displayName":"Ada"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

# Create and list tasks
curl -s -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Write docs","dueDate":"2026-12-01"}'

curl -s http://localhost:8080/api/tasks -H "Authorization: Bearer $TOKEN"
```
