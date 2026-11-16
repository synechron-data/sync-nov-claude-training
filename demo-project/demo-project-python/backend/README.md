# Task API — Python backend

A Python / FastAPI port of the Spring Boot backend in the Java edition of
this demo repo (`demo-project-java`). It exposes the same REST API on the same
port (8080), so the Angular frontend works unchanged.

- **Stack:** Python 3.11+, FastAPI, Uvicorn, SQLAlchemy 2 + SQLite (in-memory),
  PyJWT (HS256), bcrypt password hashing, Pydantic request validation
- **Tests:** pytest (`tests/`)

## Running

With [uv](https://docs.astral.sh/uv/) (recommended):

```bash
cd backend
uv sync                       # creates .venv and installs everything
uv run python -m app          # http://localhost:8080
uv run pytest                 # run the tests
```

For auto-reload while editing: `uv run uvicorn app.main:app --port 8080 --reload`.

With plain pip (pip 25.1+ for `--group`):

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate        # macOS/Linux: source .venv/bin/activate
pip install -e . --group dev
python -m app
pytest
```

Interactive API docs (Swagger UI) are at `http://localhost:8080/docs` — use
the **Authorize** button to paste a token from `/api/auth/login`.

## Java → Python map

| Java (`demo-project-java/backend/`)       | Python (`backend/app/`)                         |
|-------------------------------------------|-------------------------------------------------|
| `TaskApiApplication.java`                 | `main.py` (+ `__main__.py` to run on port 8080) |
| `application.properties`                  | `config.py`                                     |
| `auth/AuthController`, `AuthService`      | `auth/router.py`, `auth/service.py` (+ `auth/schemas.py`) |
| `auth/JwtUtil`                            | `auth/jwt_util.py`                              |
| `auth/JwtAuthFilter`                      | `get_current_user_id()` in `dependencies.py`    |
| `config/SecurityConfig` (+ BCrypt bean)   | CORS in `main.py`, beans in `dependencies.py`, `auth/password_encoder.py` |
| `exception/GlobalExceptionHandler`        | `exceptions.py`                                 |
| `task/Task`, `TaskRepository`, `TaskService`, `TaskController` | `tasks/models.py`, `repository.py`, `service.py`, `router.py` (+ `schemas.py`) |
| `user/User`, `UserRepository`             | `users/models.py`, `users/repository.py`        |
| JPA / H2                                  | `database.py` / SQLite in-memory                |
| `@NotBlank`                               | `NotBlankStr` in `common.py`                    |
| `IllegalArgumentException` → 400          | `ValueError` → 400                              |
| `NoSuchElementException` → 404            | `LookupError` → 404                             |

Spring's constructor injection maps to FastAPI `Depends(...)`: every service is
built in `dependencies.py`, and tests can replace any of them through
`app.dependency_overrides`.

## Deliberate issues (kept on purpose — same as the Java backend)

1. `JwtUtil.is_token_valid()` always returns `True`, so token expiry is never checked.
2. `TaskService.mark_completed()` and `.delete()` don't check task ownership (IDOR).
3. `JWT_SECRET` in `config.py` is a hardcoded demo secret.
4. `tests/test_task_service.py` and `tests/test_auth_service.py` are placeholders only.

## Differences from the Java backend

- **Unauthenticated requests get `401`**, where Spring Security returns `403`
  (same as the .NET port). The body is `{"error": "..."}`.
- **Request validation is enforced.** The Java DTOs declare `@Email`/`@NotBlank`
  but the controllers don't use `@Valid`, so those checks never run. Here
  Pydantic enforces them and returns `400 {"error": "..."}` (same as the .NET port).
- **No database console.** There is no equivalent of the H2 web console, but
  there is Swagger UI at `/docs`.
- **Python naming.** Code uses snake_case (`owner_id`, `created_at`); the JSON
  is camelCase (`ownerId`, `createdAt`), so the shape is unchanged.
