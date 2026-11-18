<!--
Sync Impact Report (temporary; remove before committing)
Version change: (unfilled template) → 1.0.0
Modified principles: none renamed (all five principle slots were unfilled placeholders)
Added sections: Core Principles I–V, Technology Stack & Constraints, Development Workflow, Governance
Removed sections: none
Follow-up TODOs:
- RATIFICATION_DATE set to 2026-11-18 (date of first adoption in this repo); amend if the team
  adopted these rules earlier.
Source of rules: CLAUDE.md (project instructions) at the repository root.
-->
# Task Manager Constitution

## Core Principles

### I. Layered, Package-Scoped Architecture
Backend code MUST live in its feature package under `com.technizer.taskapi`: `auth/` for
authentication and JWT, `task/` for task CRUD, `user/` for the user entity and repository.
Frontend code MUST live under `frontend/src/app/` in `auth/` (login/register) or `tasks/` (task
list UI). New code MUST be placed in the package that owns its responsibility; cross-package
dependencies MUST go through public types, not internals.
Rationale: clear ownership keeps features independently understandable and testable.

### II. Backend Coding Standards
Backend classes MUST use constructor injection only; field injection (`@Autowired` on fields) is
prohibited. DTOs MUST be Java records wherever practical; a non-record DTO MUST be justified in
the PR. Backend code targets Java 17 and Spring Boot 3.2.
Rationale: constructor injection makes dependencies explicit and objects testable without a
container; records give immutable, boilerplate-free DTOs.

### III. Frontend Coding Standards
New Angular features MUST use standalone components; new NgModules are prohibited. Services MUST
own all HTTP calls; components MUST NOT call `HttpClient` directly.
Rationale: a single HTTP seam keeps components simple and lets tests mock one service.

### IV. Tests Accompany Every Change (NON-NEGOTIABLE)
Every code change or addition MUST ship with test cases. Backend tests use JUnit 5 and MUST mirror
the `main/java` structure under `src/test/java`; frontend tests use Jasmine/Karma (`ng test`).
Existing tests MUST NOT be removed. After adding or modifying any method, endpoint, service,
repository query, controller or component, the `write-tests` skill MUST be invoked (not
hand-written replacements) before the work is reported as done.
Rationale: consistent, skill-generated tests keep coverage uniform and prevent regressions.

### V. Stateless JWT Security and Secret Hygiene
Authentication MUST be stateless JWT via Spring Security; server-side sessions MUST NOT be
introduced. Secrets in `application.properties` MUST NOT be modified without explicitly flagging
the change in the PR or conversation, and secrets MUST NOT be committed in new files.
Rationale: statelessness keeps the API horizontally scalable; explicit flagging prevents silent
credential or signing-key changes.

## Technology Stack & Constraints

- Backend: Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA, H2 (development only).
- Frontend: Angular 17 with TypeScript.
- New dependencies (Maven or npm) MUST NOT be installed without first confirming with the
  maintainers.
- Changes that alter this stack MUST be made through a constitution amendment.

## Development Workflow

- Commits MUST follow Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`.
- Direct pushes to `main` are prohibited; all changes MUST go through a branch and pull request.
- Pull request descriptions MUST include: what changed, why, and how to test.
- A change is not complete until its tests exist, pass, and comply with Principle IV.

## Governance

This constitution supersedes other practices where they conflict; `CLAUDE.md` provides runtime
development guidance and MUST remain consistent with it.

- **Amendments**: proposed via pull request that edits this file, states the rationale, and is
  approved by a maintainer. Rules MUST be reflected in `CLAUDE.md` when they affect day-to-day work.
- **Versioning**: semantic. MAJOR for backward-incompatible principle removal or redefinition;
  MINOR for a new principle/section or materially expanded guidance; PATCH for clarifications and
  wording fixes.
- **Compliance review**: every PR review MUST verify compliance with the principles above, and any
  deviation MUST be justified in the PR description.

**Version**: 1.0.0 | **Ratified**: 2026-11-18 | **Last Amended**: 2026-11-18
