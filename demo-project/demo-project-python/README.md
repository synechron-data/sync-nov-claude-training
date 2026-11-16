# Task Manager — Claude Code Training Demo Repo (Python)

A small, real, full-stack app used as the live demo project for the Claude Code
training. It's intentionally simple: a Task Manager with email/password auth
and a per-user task list.

This is the **Python edition** of the demo repo. It has the same Angular
frontend, REST API, and deliberate bugs as the Java edition
(`demo-project-java`). Pick whichever backend you're most comfortable with.

- **Backend:** Python 3.11+, FastAPI, SQLAlchemy 2, SQLite (in-memory), JWT auth
- **Frontend:** Angular 17 (standalone components), TypeScript

## Structure

```
demo-project-python/
├── backend/     ← FastAPI REST API (port 8080)
│   ├── pyproject.toml
│   ├── app/       ← auth/, tasks/, users/, config.py, database.py, main.py, …
│   └── tests/     ← pytest tests
├── frontend/    ← Angular app (port 4200)
└── README.md
```

This repo does **not** include a `CLAUDE.md`, `.claude/` folder, or a
Spec Kit setup — those are created live by participants during the session
(Modules 4, 6, 7, 8, 9–10, and 11). Do not pre-create them.

## Prerequisites

- Python 3.11+ (`python --version`)
- [uv](https://docs.astral.sh/uv/) (recommended — `uv --version`), or pip 25.1+
- Node.js 18+ and npm
- Claude Code installed and authenticated (`npm install -g @anthropic-ai/claude-code`)
- A GitHub repo this project has been pushed to (needed for the GitHub MCP
  and bug-lifecycle demo in Module 13)
- A Jira project/board (needed for the Jira MCP demo in Module 13)

## Running the backend

```bash
cd backend
uv sync                 # first time only: creates .venv and installs dependencies
uv run python -m app
```

The API starts on `http://localhost:8080`. The database is an in-memory SQLite
database. It is created on startup and wiped when the API stops. Interactive
API docs (Swagger UI) are at `http://localhost:8080/docs`.

Run the tests with:

```bash
cd backend
uv run pytest
```

Using pip instead of uv? See `backend/README.md`.

## Running the frontend

```bash
cd frontend
npm install
npm start
```

The app starts on `http://localhost:4200` and expects the backend to be
running on port 8080 (CORS is already configured for this).

## API quick reference

```
POST /api/auth/register   { email, password, displayName }  → { token }
POST /api/auth/login      { email, password }                → { token }
GET    /api/tasks                                             (auth required)
POST   /api/tasks         { title, description }              (auth required)
PATCH  /api/tasks/{id}/complete                                (auth required)
DELETE /api/tasks/{id}                                         (auth required)
```

All `/api/tasks/**` endpoints require `Authorization: Bearer <token>`.

## Deliberate issues (for training demos — do not fix before the session)

These are intentional and are the basis for specific module demos. See the
Trainer Handbook for exactly where each one is used.

1. **`JwtUtil.is_token_valid()`** (`backend/app/auth/jwt_util.py`) always
   returns `True` — expired tokens are still accepted. Used in the Module 13
   `/security-review` → `/create-issue` → `/fix-issue` → `/commit`
   bug-lifecycle demo.
2. **`TaskService.mark_completed()` and `.delete()`**
   (`backend/app/tasks/service.py`) do not check that the task belongs to the
   requesting user (broken object-level authorization / IDOR). Used as a
   secondary finding in the same demo, or as a standalone code-review skill
   demo in Module 6.
3. **`JWT_SECRET`** (`backend/app/config.py`) is a hardcoded demo secret
   committed to the repo. Used to illustrate the `deny` list in
   `settings.json` (Module 4) and as a finding in the code-review skill
   (Module 6).
4. **Test coverage is placeholder-only** (`test_task_service.py`,
   `test_auth_service.py`) — used for the auto test-writing skill demo
   (Module 6) and as a Spec Kit implementation target (Modules 9–10).

## Using the Trainer Handbook with this edition

The handbook's examples are written for the Java edition. Substitute these
when following along:

| Handbook (Java)                         | This repo (Python)                            |
|-----------------------------------------|-----------------------------------------------|
| `mvn spring-boot:run`                   | `uv run python -m app`                        |
| `mvn test` / `mvn -f backend/pom.xml test` | `uv run pytest` / `uv run --directory backend pytest` |
| `mvn clean package`                     | `uv build`                                    |
| `Bash(mvn *)`                           | `Bash(uv *)`                                  |
| `Write(backend/pom.xml)`                | `Write(backend/pyproject.toml)`               |
| `Read(**/application.properties)` (deny) | `Read(**/config.py)` (deny) — **required**, or the Module 4 secret demo won't block anything |
| `backend/src/main/java/com/technizer/taskapi/auth/` | `backend/app/auth/`                |
| `Task.java`, `TaskController.java`      | `tasks/models.py`, `tasks/router.py`          |
| `JwtAuthFilter` (Spring Security filter) | `get_current_user_id()` (FastAPI dependency in `app/dependencies.py`) |
| JPA / H2                                | SQLAlchemy / SQLite                           |
| JUnit 5, `src/test/java`                | pytest, `backend/tests`                       |
| Module 11 hook: `.java` → `fmt-maven-plugin` | `.py` → `uvx ruff format <file>`         |

## Trainer setup checklist

- [ ] Push this repo to a real GitHub repository (private is fine) before
      the session — Module 13's GitHub MCP demo needs a live remote
- [ ] Create a matching Jira project/board with at least one sample issue
      type configured — Module 13's Jira MCP demo needs a live project
- [ ] Confirm `uv run python -m app` and `npm start` both work on the
      training machine the day before
- [ ] Do **not** run `/security-review` or fix the deliberate bugs before
      the session
