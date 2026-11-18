# Research: Task Priority

No `NEEDS CLARIFICATION` items remained in the Technical Context. The decisions below resolve the
design questions found while reading the existing code.

## R1. Where to filter and sort

- **Decision**: Server-side, via optional query parameters on `GET /api/tasks`
  (`priority`, `completed`, `sort`, `order`), applied in `TaskService` to the list returned by the
  existing owner-scoped `findByOwnerId`.
- **Rationale**: Owner scoping stays in one place (the repository query) so no filter can widen
  access (FR-017). API clients get the capability too, and the logic is unit-testable. Per-user lists
  are small (spec SC-006 uses 500), so in-memory filtering is well within budget.
- **Alternatives considered**: (a) Client-side only: simplest, but not testable at the API and
  duplicates logic for other clients. (b) New derived repository queries or Specifications: more
  repository code for no benefit at this scale and adds more `@Query` surface to test.

## R2. How sorting works and what "pre-feature order" is

- **Decision**: With no `sort`, return `findByOwnerId` results untouched, which is today's behaviour.
  With `sort=priority`, apply a stable sort by priority rank (HIGH, MEDIUM, LOW for `order=desc`,
  the default; reversed for `order=asc`). Ties keep their incoming order because `List.sort` is stable.
- **Rationale**: Satisfies FR-012, FR-012a and FR-014. Sorting by the stored enum name in the
  database would order alphabetically (HIGH, LOW, MEDIUM), which is wrong, so the comparator uses an
  explicit rank.
- **Alternatives considered**: Store priority as an integer rank column. Rejected: less readable
  data and the same result is achieved with a comparator.
- **Note**: There is no explicit `ORDER BY` on `findByOwnerId` today, so "pre-feature order" is the
  database's natural order (primary-key order in H2). The plan preserves that rather than imposing a
  new one.

## R3. Distinguishing "priority omitted" from "priority is null or empty"

- **Decision**: In the request records, declare `priority` as a Jackson `JsonNode`. A missing field
  arrives as Java `null` (meaning "not sent"); an explicit JSON `null` arrives as a `NullNode`. The
  controller converts it with `Priority.parse(...)`: missing returns `null` (leave unchanged on
  update, Medium on create), while explicit null, blank, non-text or unknown text throws
  `IllegalArgumentException`, which `GlobalExceptionHandler` already maps to 400.
- **Rationale**: FR-005 requires omitted to leave priority unchanged, while FR-006 and FR-006a require
  explicit null or empty to be rejected. A plain `String` or enum field cannot tell these apart, and
  `JsonNullable` would be a new dependency, which the constitution says to confirm first.
- **Alternatives considered**: Treat null as omitted (simpler, but contradicts clarification Q1);
  `Optional<String>` (Jackson does not distinguish absent from null for it by default).

## R4. Value format and case handling

- **Decision**: Accept `low`, `Medium`, `HIGH` and so on, case-insensitively and trimmed. Store the
  enum as a string (`@Enumerated(EnumType.STRING)`), and serialise as the upper-case constant
  (`"LOW"`, `"MEDIUM"`, `"HIGH"`). The UI maps these to "Low / Medium / High" labels.
- **Rationale**: Matches the spec assumption that matching ignores capitalisation. String storage
  survives enum re-ordering, unlike ordinal.

## R5. Defaulting existing tasks to Medium

- **Decision**: The entity field initialises to `Priority.MEDIUM`, the column is `NOT NULL`, and the
  column definition carries a database default of `'MEDIUM'`.
- **Rationale**: No `application.properties` exists, so Spring Boot defaults to an in-memory H2 with
  schema generated at startup; nothing persists between runs today, but if a persistent database or
  `ddl-auto=update` is introduced later, the column default lets Hibernate add the column to a table
  with existing rows without failing, and old rows read as Medium (FR-004).
- **Alternatives considered**: A migration tool (Flyway or Liquibase): a new dependency and
  unnecessary for the current setup. Nullable column with a null-to-Medium getter: leaves nulls in
  the data.

## R6. Error handling for bad query parameters

- **Decision**: Take `priority`, `completed`, `sort` and `order` as `String` request parameters and
  parse them in the service layer, throwing `IllegalArgumentException` for bad values.
- **Rationale**: `GlobalExceptionHandler` has a catch-all returning 500. A typed parameter that fails
  conversion (for example `completed=maybe`) raises `MethodArgumentTypeMismatchException`, which
  would fall into that catch-all. Manual parsing keeps these as clean 400s with a JSON error body.

## R7. Ownership (FR-017, FR-018)

- **Decision**: No change to the access model. Listing uses `findByOwnerId(ownerId)` first, so every
  filter and sort acts only on the caller's tasks. `update` already hides other users' tasks behind
  a 404 and writes priority only after that check.
- **Out of scope (found, not fixed here)**: `markCompleted` and `delete` lack ownership checks (the
  repo documents `markCompleted` as a deliberate training gap). They do not change priority, so they
  do not affect FR-018, but they are worth tracking separately.

## R8. Frontend shape

- **Decision**: Keep the inline-template standalone `TaskListComponent` and add template-driven
  controls (`[(ngModel)]`): a priority select on the new-task row, a priority select on each task row
  for editing, and filter and sort selects above the list. Filter and sort changes call
  `TaskService.list(filters)`. `TaskService.update` sends `priority` only when provided, so the
  existing due-date edit leaves priority unchanged.
- **Rationale**: Follows the `frontend/CLAUDE.md` conventions (`*ngFor`, `ngModel`, services own
  HTTP, strict templates). The inline stylesheet has a 2 kB warning and 4 kB error budget, so new
  rules must stay compact.
- **Alternatives considered**: Client-side filtering of the already-loaded list. Rejected for
  consistency with R1.
