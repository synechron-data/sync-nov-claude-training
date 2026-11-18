# Contributing

How to set up, change and submit work on the Task Manager. For how the system works, see [architecture.md](architecture.md), [api.md](api.md) and [frontend.md](frontend.md).

## Prerequisites

- Java 17+ and Maven 3.9+
- Node.js 18+ and npm
- Chrome (the frontend tests run in Karma/Chrome)
- Optional: Claude Code, which reads the project's `CLAUDE.md` files and skills

## Setup and run

```bash
# Backend: http://localhost:8080
cd backend
mvn spring-boot:run

# Frontend: http://localhost:4200 (in a second terminal)
cd frontend
npm install
npm start
```

The backend uses an in-memory H2 database, so data resets on every restart. The H2 console is at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:taskdb`, user `sa`, blank password).

## Repository layout

```
backend/     Spring Boot API (com.technizer.taskapi: auth, task, user, config, exception)
frontend/    Angular app (auth, tasks, core)
docs/        Project documentation
specs/       Spec Kit feature specs, plans and tasks (for example specs/001-task-priority)
plans/       Per-feature implementation plans (for example plans/task-due-date.md)
.claude/     Project skills for Claude Code
CLAUDE.md    Conventions for developers and Claude (root, backend/ and frontend/ each have one)
```

## Coding standards

The full list is in the `CLAUDE.md` files. In short:

**Backend**
- Constructor injection with `private final` fields. No field injection.
- DTOs as Java records where practical. Controllers return entities or records directly, not `ResponseEntity`.
- Entities keep a public no-arg constructor, a convenience constructor and explicit getters/setters. There is no Lombok.
- Beware: Bean Validation annotations on request records are **not enforced** (no `@Valid`). Validate in the service, as `TaskService.update()` does. If you add `@Valid`, also add a handler for `MethodArgumentNotValidException`, or the catch-all handler turns it into a 500.

**Frontend**
- Standalone components only. No NgModules for new features.
- Services own HTTP. Components never call `HttpClient`.
- Follow the existing style: inline template and styles, template-driven forms, `*ngIf` / `*ngFor`. See [frontend.md](frontend.md#conventions).

## Tests

Every code change or addition comes with tests, and existing tests are never removed.

| Where | Framework | Run |
|---|---|---|
| Backend | JUnit 5, Mockito, AssertJ | `cd backend && mvn test` |
| Frontend | Jasmine / Karma | `cd frontend && npx ng test --watch=false --browsers=ChromeHeadless` |

Backend tests mirror the `src/main/java` structure under `src/test/java`. Tips:

- **Service tests:** mock the repository with Mockito. For `AuthService`, use a real `BCryptPasswordEncoder` and build `JwtUtil` directly with a secret of at least 32 bytes (a shorter one throws `WeakKeyException`).
- **Controller (MockMvc) tests:** don't use `@WithMockUser`. Its principal isn't a `Long`, so `@AuthenticationPrincipal Long ownerId` resolves to `null`. Use `.with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))`, or send a real token from `JwtUtil`.

Frontend test patterns are described in [frontend.md](frontend.md#testing).

## Working with Claude Code

The repo ships project skills in `.claude/skills/`, and `CLAUDE.md` makes two of them mandatory after a change:

| Skill | When it runs | What it does |
|---|---|---|
| `write-tests` | After adding or changing any method, endpoint, service, repository query, controller or component | Generates JUnit 5 (backend) or Jasmine (frontend) tests |
| `write-docs` | After any change that affects documented behaviour: endpoints, request/response records, entities, auth/security/CORS config, exception handling, frontend routes and services | Updates `docs/` and the README API quick reference from the code, then runs the endpoint check |

Other skills: `team-code-review` (review/audit), `create-skill`, `deploy`, and the `speckit-*` set for spec-driven work.

### Keeping the docs in sync (with or without Claude)

When you change an endpoint, entity or security config:

1. Update `docs/api.md`, using one heading per endpoint in the exact form ``### `VERB /path` ``.
2. Update the Task object and the ER diagram if entity fields changed, plus the README API quick reference.
3. Run the check from the repo root:

   ```bash
   bash .claude/skills/write-docs/scripts/check-endpoints.sh
   ```

   It compares the `@*Mapping` annotations in the controllers with the headings in `docs/api.md` and exits non-zero if they differ. It checks endpoints only. Field-level accuracy is on you.

## Git workflow

- **Conventional Commits:** `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, optionally with a scope, for example `feat(task): add optional due date to tasks`.
- **Branches and PRs:** work on a feature branch and open a pull request (see the merged `feat/task-due-date` PRs). Claude Code is configured never to push directly to `main`.
- **PR descriptions include:** what changed, why, and how to test.

## Rules and cautions

- **Confirm before adding dependencies** (Maven or npm).
- **Secrets:** don't modify `application.properties` secrets without flagging it explicitly, and never put real secrets in the repo. The JWT secret currently committed is a demo value. `app.jwt.secret` and `app.jwt.expiry-minutes` (default 60) are the relevant properties.
- **Deliberate issues:** this repo is a Claude Code training demo, and the README lists intentional bugs (always-true `isTokenValid()`, missing ownership checks on complete and delete, the hard-coded secret, and the test-coverage note). Don't fix them ahead of a training session unless that's the task you were given.
