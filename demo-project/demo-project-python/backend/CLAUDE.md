# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

FastAPI + SQLAlchemy 2 + SQLite (in-memory) + PyJWT/bcrypt backend, serving the REST API on port 8080 for the Angular frontend. Python 3.11+. Run everything from this `backend/` directory.

## Commands

```bash
uv sync                                          # first time: create .venv, install deps
uv run python -m app                             # start API (http://localhost:8080, Swagger UI at /docs)
uv run uvicorn app.main:app --port 8080 --reload # auto-reload
uv run pytest                                    # all tests (testpaths=tests, pythonpath=.)
uv run pytest tests/test_task_service.py::test_name   # single test
```

Without uv: `pip install -e . --group dev` (pip 25.1+), then `python -m app` / `pytest`. No linter is configured; format with `uvx ruff format <file>`.

## Structure and conventions

`app/` is package-per-feature (`auth/`, `tasks/`, `users/`). Each feature layers `router.py` → `service.py` → `repository.py`, with `models.py` (SQLAlchemy) and `schemas.py` (Pydantic). When adding a feature, follow `tasks/` as the template.

- **Wiring lives in `dependencies.py`.** Services and repositories are never constructed in routers; add a `get_*_service` provider plus an `Annotated[..., Depends(...)]` alias there, then take the alias as a router parameter (see `tasks/router.py`). The current user id comes from the `CurrentUserId` alias. In tests, build services directly with a fake/real repository, or use `app.dependency_overrides` for router tests (`httpx` is a dev dependency).
- **Raise plain exceptions from services, not `HTTPException`.** `ValueError` → 400, `LookupError` → 404 via `exceptions.py`; all error bodies are `{"error": "..."}`. Auth failures are the exception: `dependencies.py` raises 401 `HTTPException`s.
- **Schemas extend `CamelModel`** (`common.py`) so JSON is camelCase while Python stays snake_case; use `NotBlankStr` for required non-empty strings. Response schemas set `from_attributes=True` to serialize ORM objects directly.
- **New ORM model modules must be imported in `database.create_tables()`**, otherwise their tables are never created. Tables are created at import time of `app.main`.
- **Config is constants in `config.py`**, imported as `from app import config` — no env vars or `.env`.
- The DB is a single shared in-memory connection (`StaticPool`); data is lost on restart and tests that hit the real engine share state.
- JWT: HS256, `sub` = user id as string. `JwtUtil.extract_user_id` decodes with `verify_exp=False` on purpose (expiry is meant to be enforced by `is_token_valid`).

## Deliberate bugs — do not fix unless asked

Training fixtures: `JwtUtil.is_token_valid()` always returns `True` (`auth/jwt_util.py`); `TaskService.mark_completed()`/`.delete()` skip the ownership check (`tasks/service.py`); `JWT_SECRET` is hardcoded in `config.py`; `tests/test_*_service.py` are placeholders. If a task touches these areas, point the issue out rather than silently fixing it.
