# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Angular 17 app (standalone components, no NgModules), TypeScript ~5.4, RxJS 7.8. Dev server on port 4200; expects the backend API on `http://localhost:8080`. Run everything from this `frontend/` directory.

## Commands

```bash
npm install
npm start          # ng serve → http://localhost:4200
npm run build      # production build
npm run watch      # dev-config build with --watch
npm test           # ng test (Karma + Jasmine, needs Chrome)
npx ng test --include src/app/tasks/task.service.spec.ts   # single spec file
```

No spec files exist yet, and there is no lint script.

## Architecture

- **Bootstrap:** `main.ts` → `app.component.ts` (root) with providers in `app/app.config.ts`: `provideRouter(routes)` and `provideHttpClient(withInterceptors([authInterceptor]))`. Register any new global provider or interceptor there.
- **Routes** (`app.routes.ts`): `''` → redirects to `login`; `login` → `LoginComponent`; `tasks` → `TaskListComponent`. There are no route guards, so `/tasks` is reachable without a token (the API will respond 401).
- **Auth flow:** `auth/auth.service.ts` calls `/api/auth/login|register`, and stores the returned JWT in `localStorage` under `task_api_token`. `core/auth.interceptor.ts` (a functional `HttpInterceptorFn`) reads it via `AuthService.getToken()` and adds `Authorization: Bearer <token>` to every request. Nothing handles 401 responses or token expiry.
- **Data services:** `tasks/task.service.ts` wraps `/api/tasks` (list, create, `PATCH /{id}/complete`, delete). The `Task` interface mirrors the backend's camelCase JSON (`ownerId`, `createdAt` as ISO string) — keep it in sync with `backend/app/tasks/schemas.py`.
- **Components** declare `standalone: true` and import what they use directly (`CommonModule`, `FormsModule`); templates and styles are mostly inline. Services use constructor injection with `providedIn: 'root'`.

## Gotchas

- API base URLs are hardcoded as `http://localhost:8080/...` inside each service (no environment files or proxy). Changing the backend port or host means editing both `auth.service.ts` and `task.service.ts`, plus `CORS_ALLOWED_ORIGINS` in the backend `config.py` if the frontend origin changes.
- The task list reloads via `refresh()` after each mutation instead of updating local state.
- Backend validation errors arrive as `{"error": "..."}` (HTTP 400); components currently don't surface errors from failed requests.
