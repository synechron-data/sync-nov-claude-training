# Frontend guide

The Angular 17 single-page app in `frontend/`. It talks to the REST API described in [api.md](api.md). For how it fits with the backend, see [architecture.md](architecture.md).

## Stack

- Angular 17.3, **standalone components only** (no NgModules), esbuild-based `application` builder
- RxJS 7.8, TypeScript in `strict` mode, Angular `strictTemplates` (template type errors fail the build)
- Tests: Jasmine + Karma
- Formatting: `.editorconfig` sets 2-space indent and single quotes in `.ts` files

## Running it

```bash
cd frontend
npm install
npm start            # ng serve on http://localhost:4200
```

The backend must be running on port 8080 for login and tasks to work. It only accepts CORS requests from `http://localhost:4200`.

| Command | Purpose |
|---|---|
| `npm start` | Dev server with live reload |
| `npm test` | Karma in watch mode (opens Chrome) |
| `npx ng test --watch=false --browsers=ChromeHeadless` | Single CI-style test run |
| `npm run build` | Production build into `dist/` (enforces size budgets) |
| `npx ng build --configuration development` | Build without production budgets or optimisation |
| `npx ng generate component tasks/task-detail` | Scaffold a standalone component |

## Project layout

```
frontend/src/app/
├── app.config.ts            providers: router + HttpClient with the auth interceptor
├── app.routes.ts            route table
├── app.component.*          root shell: only a <router-outlet>
├── auth/
│   ├── auth.service.ts      register / login / logout, token storage
│   └── login.component.ts   combined login + register form
├── core/
│   └── auth.interceptor.ts  adds the Bearer token to every request
└── tasks/
    ├── task.service.ts      HTTP calls for /api/tasks, plus the Task type
    └── task-list.component.ts  the task list screen
```

One folder per feature, each holding its service and component(s). `core/` is for cross-cutting HTTP plumbing.

## Routes

| Path | Component | Notes |
|---|---|---|
| `` | | Redirects to `login` |
| `login` | `LoginComponent` | Toggles between "Log in" and "Register" in the same component |
| `tasks` | `TaskListComponent` | After a successful login or register the app navigates here |

There is no route guard. Anyone can open `/tasks` directly, and the page loads, but its API calls get a 403 and nothing is shown (see [Known gaps](#known-gaps)).

## How authentication works on the client

1. `LoginComponent.submit()` calls `AuthService.login()` or `register()`.
2. On success, `AuthService` stores the returned token in `localStorage` under the key `task_api_token`.
3. `authInterceptor` (a functional interceptor registered in `app.config.ts`) reads the token with `AuthService.getToken()` and, if present, clones every outgoing request with `Authorization: Bearer <token>`.
4. `AuthService.logout()` removes the token, but nothing calls it yet.

"Logged in" simply means "a token exists in `localStorage`". Expiry is never checked client-side.

## Services and data

Services own all HTTP. Components never inject `HttpClient`. Both services are `providedIn: 'root'`, return `Observable`s, and hard-code the API base URL (`http://localhost:8080/api/...`).

**`TaskService`**

| Method | HTTP call |
|---|---|
| `list()` | `GET /api/tasks` |
| `create(title, description, dueDate = null)` | `POST /api/tasks` |
| `update(id, title, description, dueDate)` | `PUT /api/tasks/{id}` |
| `complete(id)` | `PATCH /api/tasks/{id}/complete` |
| `delete(id)` | `DELETE /api/tasks/{id}` |

`GET /api/tasks/completed` has no client method yet.

The `Task` interface mirrors the backend entity's JSON exactly. **Keep the two in sync** when either changes:

```ts
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Task {
  id: number;
  title: string;
  description: string;
  completed: boolean;
  dueDate: string | null;   // yyyy-MM-dd
  ownerId: number;
  createdAt: string;        // ISO-8601 instant
  priority: Priority;       // read-only for now: the API cannot set it
}
```

## What the task screen does

`TaskListComponent` loads the list on init and calls `refresh()` after every change, so the list always reflects the server.

- **Add:** title, description and an optional due date. A blank title is ignored and no request is sent. The form clears after a successful add.
- **Due date:** each task has a date input to change or clear it. Clearing sends `null` (an empty string is converted). The update call resends the task's existing title and description, since `PUT` replaces all three fields.
- **Overdue:** a task is overdue when it is not completed, has a due date, and the due date is before today. The comparison is a string compare on `yyyy-MM-dd` values, which avoids time-zone shifts. A task due today is not overdue. Overdue tasks get the `overdue` CSS class (red, bold due text).
- **Complete / Delete:** buttons per task. Complete is hidden once a task is completed.
- **Priority** is part of the `Task` type but is not shown or editable in the UI.

## Conventions

- **Components:** `standalone: true` with **inline** `template` and `styles`, importing `CommonModule` and `FormsModule` directly. `AppComponent` is the exception and uses separate `.html` and `.css` files.
- **Forms:** template-driven (`[(ngModel)]`), not reactive forms.
- **Templates:** the older `*ngIf` / `*ngFor` directives, not the Angular 17 `@if` / `@for` control flow. Stay consistent within a file.
- **Dependency injection:** constructor injection in components and services. The functional interceptor uses `inject()`.
- **No NgModules** for new features.
- **Build budget:** each component stylesheet has a **2 kB warning / 4 kB error** limit in the production build. Inline `styles` count, so a large inline style block can fail `npm run build`.

## Adding a feature

1. Create `src/app/<feature>/` with a service and a standalone component (`npx ng generate component <feature>/<name>` scaffolds the component).
2. Put every HTTP call in the feature service, and type its request and response shapes to match [api.md](api.md).
3. Register the route in `app.routes.ts`.
4. Add specs next to the code (`*.spec.ts`). See below.
5. Update the docs. The `write-docs` skill does this (see [contributing.md](contributing.md)).

## Testing

Specs live next to the code as `*.spec.ts` and run with Karma/Jasmine.

- **Service specs** use `provideHttpClient()` with `provideHttpClientTesting()` and an `HttpTestingController`, and call `httpMock.verify()` after each test so unexpected requests fail. See `tasks/task.service.spec.ts`.
- **Component specs** replace the service with `jasmine.createSpyObj<TaskService>(...)` and provide it with `{ provide: TaskService, useValue: ... }`. They cover both methods and rendered output. See `tasks/task-list.component.spec.ts`.
- Both existing specs use small `task()` factory helpers with overrides, which keeps fixtures short.

Current coverage is the `tasks/` feature only. There are **no specs yet** for `AuthService`, `LoginComponent` or `authInterceptor`.

## Known gaps

These describe how the code behaves today.

- **No logout.** There is no logout button. `isLoggedIn()` and `logout()` exist but nothing calls them, and nothing clears a token the server has rejected.
- **No route guard** on `/tasks`.
- **Errors are handled inconsistently.** `LoginComponent` shows one generic message ("Something went wrong. Check your details and try again.") for every failure. `TaskListComponent` has no error handlers, so failed calls fail silently, for example when the token is missing or expired.
- **The server returns an empty-body 403**, not a 401, for a missing or invalid token. Client code that wants to react to an auth failure has to look for 403.
- **Hard-coded API URL** in both services, so there is no environment-based configuration.
- **Page title:** `AppComponent.title` is unused. The browser tab title comes from `src/index.html`, which currently says "Frontend".
- **Priority and completed-tasks** exist in the API or types but have no UI.
