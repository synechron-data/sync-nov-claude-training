# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Python edition of the Claude Code training demo: a Task Manager (email/password auth + per-user tasks). `backend/` is a FastAPI port of the Spring Boot backend in the sibling `demo-project-java`, exposing the same REST API on port 8080. `frontend/` is an Angular 17 (standalone components) app on port 4200 that works unchanged against any of the sibling backends.

## Commands

Backend (run from `backend/`; uses [uv](https://docs.astral.sh/uv/)):

```bash
uv sync                                   # create .venv, install deps (first time)
uv run python -m app                      # API on http://localhost:8080 (Swagger UI at /docs)
uv run uvicorn app.main:app --port 8080 --reload   # auto-reload while editing
uv run pytest                             # all tests
uv run pytest tests/test_task_service.py::test_name   # single test
```

Without uv: `python -m venv .venv`, activate, `pip install -e . --group dev` (pip 25.1+), then `python -m app` / `pytest`.

Frontend (run from `frontend/`): `npm install`, `npm start` (ng serve), `npm run build`, `npm test` (Karma/Jasmine).

There is no lint/format config in the repo; the training material uses `uvx ruff format <file>` for `.py` files.

## Backend architecture

Package-per-feature under `backend/app/` (`auth/`, `tasks/`, `users/`), each following the same layering: `router.py` (HTTP) → `service.py` (logic) → `repository.py` (SQLAlchemy queries), with `models.py` (ORM) and `schemas.py` (Pydantic) alongside. The code deliberately mirrors the Java structure (see the Java→Python table in `backend/README.md`).

Cross-cutting pieces that span several files:

- **Dependency wiring is centralized in `app/dependencies.py`.** It builds every service from a per-request `Session` (`get_db`) and exposes `AuthServiceDep`, `TaskServiceDep`, and `CurrentUserId` aliases for routers. `get_current_user_id()` is the Python equivalent of Spring's `JwtAuthFilter`: it reads the Bearer token and returns the user id, or raises 401. Tests replace services via `app.dependency_overrides`.
- **Errors are mapped by exception type, not raised as HTTP errors.** Services raise `ValueError` → 400 and `LookupError` → 404; `app/exceptions.py` turns them (plus Pydantic validation errors, HTTP errors, and unexpected 500s) into a uniform `{"error": "..."}` body. The 500 handler is a *middleware* registered before CORS in `main.py` so that error responses still get CORS headers — keep that ordering.
- **JSON is camelCase, Python is snake_case.** Schemas inherit `CamelModel` (`app/common.py`), which uses an alias generator with `populate_by_name=True`. `NotBlankStr` is the `@NotBlank` equivalent. Pydantic enforces validation (the Java version did not), returning 400.
- **Database is in-memory SQLite** (`DATABASE_URL = "sqlite://"`), shared across sessions via `StaticPool` so it survives for the app's lifetime and is wiped on restart. Tables are created at import time in `create_app()`; new model modules must be imported in `database.create_tables()` to register on `Base.metadata`.
- **Settings are plain constants in `app/config.py`** (no env loading): port, CORS origins (`http://localhost:4200`), JWT secret and expiry.
- Unauthenticated requests return **401** (Spring returned 403).

## Frontend architecture

Angular 17 standalone components, no NgModules. `app.config.ts` registers `core/auth.interceptor.ts`, which attaches `Authorization: Bearer <token>` from `AuthService` to every request. Feature code lives in `auth/` (login, `AuthService`) and `tasks/` (task list, `TaskService`); routes are in `app.routes.ts`.

## Deliberate bugs — do not fix unless asked

This repo is a training fixture; these flaws are intentional and are used in live demos (see `README.md`):

1. `JwtUtil.is_token_valid()` (`app/auth/jwt_util.py`) always returns `True`, so expired tokens are accepted.
2. `TaskService.mark_completed()` / `.delete()` (`app/tasks/service.py`) don't check task ownership (IDOR).
3. `JWT_SECRET` in `app/config.py` is a hardcoded demo secret.
4. `tests/test_task_service.py` and `tests/test_auth_service.py` are placeholders only.

The README also says this repo should ship without `CLAUDE.md`, `.claude/`, or Spec Kit files because participants create them live during the session — this file was added by explicit request, so remove it if the demo needs a clean slate.
