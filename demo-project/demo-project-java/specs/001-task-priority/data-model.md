# Data Model: Task Priority

## Priority (new value type)

| Constant | Rank | Display label |
|----------|------|---------------|
| `LOW`    | 1    | Low           |
| `MEDIUM` | 2    | Medium        |
| `HIGH`   | 3    | High          |

- Exactly three values; ordered Low < Medium < High by rank (used for sorting only).
- `parse(text)`: trims, matches case-insensitively, returns the constant; any other text, including
  empty, throws `IllegalArgumentException` with a message listing the allowed values.

## Task (existing entity, one new attribute)

| Attribute   | Type       | Rules                                                           | Change |
|-------------|------------|-----------------------------------------------------------------|--------|
| id          | Long       | generated                                                       | none   |
| title       | String     | required, not blank                                             | none   |
| description | String     | optional                                                        | none   |
| completed   | boolean    | default false                                                   | none   |
| dueDate     | LocalDate  | optional                                                        | none   |
| ownerId     | Long       | required; set from authenticated user                           | none   |
| createdAt   | Instant    | defaults to creation time                                       | none   |
| **priority**| Priority   | required, never null; default `MEDIUM`; stored as text; DB default `'MEDIUM'` | **new** |

### Validation rules (from the spec)

- Create: `priority` omitted gives `MEDIUM` (FR-003); present and valid gives that value; any other
  value, including explicit null or empty, is rejected (FR-006).
- Update: `priority` omitted leaves the stored value unchanged (FR-005); present and valid replaces
  it; explicit null, empty or unknown is rejected and nothing is changed (FR-006, FR-006a).
- Only the owner may update a task; a non-owner receives "not found" and nothing changes (FR-018).

### State and lifecycle

Priority has no transitions beyond "set to another of the three values". It is independent of
`completed`. Pre-existing rows read as `MEDIUM` (FR-004).

### Relationships

Unchanged: a task belongs to one user via `ownerId`.

## Query model (list view, not persisted)

| Input       | Values                                  | Absent means                  |
|-------------|-----------------------------------------|-------------------------------|
| `priority`  | low, medium, high (any case)            | no priority filter            |
| `completed` | true, false                             | no status filter              |
| `sort`      | `priority`                              | pre-feature order             |
| `order`     | `desc` (High first), `asc` (Low first)  | `desc`; only meaningful with `sort` |

A blank value is treated the same as an absent one for every input above. Filters combine with AND (FR-010). Sorting applies after filtering (FR-013) and is stable, so equal
priorities keep the pre-feature order (FR-014).
