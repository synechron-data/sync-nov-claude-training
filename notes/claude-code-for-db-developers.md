# Claude Code for Database Developers
### Participant Guide and Hands-on Labs

---

## What You Will Learn

| # | Outcome |
|---|---|
| 1 | Set up Claude Code safely against a development database |
| 2 | Teach Claude your team's SQL conventions with `CLAUDE.md` |
| 3 | Write, explain, and tune queries using real execution plans |
| 4 | Generate zero-downtime migrations with rollbacks |
| 5 | Convert legacy PL/SQL to PostgreSQL |
| 6 | Add guardrails: read-only access, hooks that block destructive SQL, and a review subagent |
| 7 | Automate migration reviews in CI |

---

## 1. How Claude Code Fits Database Work

Claude Code is an AI coding agent that runs in your terminal. It can **read and edit the files in your project** (DDL, migrations, procedures, ETL scripts) and **run commands** (like `psql`) with your approval. Through **MCP servers** it can also connect directly to a database to inspect the schema and run queries.

| You give it | It can do |
|---|---|
| Schema files in the repo | Explain data flows, generate data dictionaries and ER diagrams |
| A slow query + `EXPLAIN` access | Find the bottleneck, propose indexes or rewrites, re-measure |
| A change request | Write the migration, the rollback, and the backfill |
| Legacy procedures | Convert dialects and list what has no direct equivalent |
| A pull request | Review migrations for locking, safety, and idempotency |

> **Golden rule:** Claude Code works on **dev/staging** with a **read-only** database user. A human applies changes to production.

---

## 2. Prerequisites

| Item | Version / Note |
|---|---|
| Node.js | 18 or later |
| PostgreSQL | 15+ locally, or Docker |
| Git | Any recent version |
| `jq` | Needed for the hook script in Lab 6 |
| Claude account | Pro, Max, Team, Enterprise, or a Claude Console (API) account |

### Install Claude Code

```bash
npm install -g @anthropic-ai/claude-code
claude --version
```

Start it from inside a project folder:

```bash
cd salesdb-lab
claude
```

The first run asks you to log in.

### Start a Postgres database (Docker)

```bash
docker run --name cc-db -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
docker exec -it cc-db psql -U postgres -c "CREATE DATABASE salesdb;"
```

---

## 3. Lab Project Setup

Create this structure:

```
salesdb-lab/
├── CLAUDE.md
├── .mcp.json
├── .claude/
│   ├── settings.json
│   ├── hooks/
│   │   └── block-destructive-sql.sh
│   ├── agents/
│   │   └── migration-reviewer.md
│   └── skills/
│       └── new-migration/
│           └── SKILL.md
├── schema/
│   ├── 01_tables.sql
│   └── 02_seed.sql
├── migrations/
├── legacy/
│   └── oracle/
│       └── pkg_orders.pkb
└── docs/
```

```bash
mkdir -p salesdb-lab/{schema,migrations,legacy/oracle,docs,.claude/hooks,.claude/agents,.claude/skills/new-migration}
cd salesdb-lab && git init
```

### 3.1 `schema/01_tables.sql`

```sql
CREATE TABLE customers (
    id          BIGSERIAL PRIMARY KEY,
    name        TEXT        NOT NULL,
    email       TEXT        NOT NULL,
    region      TEXT        NOT NULL,
    is_test     BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id        BIGSERIAL PRIMARY KEY,
    sku       TEXT          NOT NULL UNIQUE,
    name      TEXT          NOT NULL,
    category  TEXT          NOT NULL,
    price     NUMERIC(12,2) NOT NULL
);

CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT        NOT NULL REFERENCES customers(id),
    order_date    TIMESTAMPTZ   NOT NULL,
    status        TEXT          NOT NULL,   -- PENDING, PAID, CANCELLED
    total_amount  NUMERIC(12,2) NOT NULL
);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT        NOT NULL REFERENCES orders(id),
    product_id  BIGINT        NOT NULL REFERENCES products(id),
    qty         INT           NOT NULL,
    unit_price  NUMERIC(12,2) NOT NULL
);

CREATE TABLE payments (
    id        BIGSERIAL PRIMARY KEY,
    order_id  BIGINT        NOT NULL REFERENCES orders(id),
    paid_at   TIMESTAMPTZ   NOT NULL,
    amount    NUMERIC(12,2) NOT NULL,
    method    TEXT          NOT NULL
);
```

> Note: the foreign key columns are intentionally **not indexed**. You will find this in Lab 3.

### 3.2 `schema/02_seed.sql`

```sql
INSERT INTO customers (name, email, region, is_test, created_at)
SELECT 'Customer ' || g,
       'Cust' || g || '@Example.com',
       (ARRAY['North','South','East','West'])[1 + (g % 4)],
       g % 50 = 0,
       now() - (g % 730) * interval '1 day'
FROM generate_series(1, 50000) g;

INSERT INTO products (sku, name, category, price)
SELECT 'SKU-' || g, 'Product ' || g,
       (ARRAY['Electronics','Home','Books','Sports'])[1 + (g % 4)],
       round((random() * 5000 + 100)::numeric, 2)
FROM generate_series(1, 500) g;

INSERT INTO orders (customer_id, order_date, status, total_amount)
SELECT 1 + (g % 50000),
       now() - (g % 365) * interval '1 day',
       (ARRAY['PENDING','PAID','PAID','PAID','CANCELLED'])[1 + (g % 5)],
       round((random() * 20000 + 200)::numeric, 2)
FROM generate_series(1, 500000) g;

INSERT INTO order_items (order_id, product_id, qty, unit_price)
SELECT 1 + (g % 500000), 1 + (g % 500), 1 + (g % 5),
       round((random() * 5000 + 100)::numeric, 2)
FROM generate_series(1, 1000000) g;

INSERT INTO payments (order_id, paid_at, amount, method)
SELECT o.id, o.order_date + interval '1 hour', o.total_amount,
       (ARRAY['UPI','CARD','NETBANKING'])[1 + (o.id % 3)::int]
FROM orders o
WHERE o.status = 'PAID';

ANALYZE;
```

Load both files:

```bash
docker exec -i cc-db psql -U postgres -d salesdb < schema/01_tables.sql
docker exec -i cc-db psql -U postgres -d salesdb < schema/02_seed.sql
```

### 3.3 Create a read-only user for Claude

```sql
CREATE ROLE claude_ro LOGIN PASSWORD 'change_me';
GRANT CONNECT ON DATABASE salesdb TO claude_ro;
GRANT USAGE ON SCHEMA public TO claude_ro;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO claude_ro;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO claude_ro;
```

> `EXPLAIN ANALYZE` on a `SELECT` works with this user. Writes do not, which is exactly what we want.

### 3.4 `CLAUDE.md` (project memory)

Claude reads this file at the start of every session. Put your team rules here.

```markdown
# SalesDB Project

## Database
- Target: PostgreSQL 16
- Schema source of truth: `schema/` (DDL) + `migrations/` (applied in filename order)
- Migration tool: plain SQL files named `V<yyyymmddhhmm>__<snake_case_description>.sql`
- Every migration has a matching rollback: `R<same-timestamp>__<description>.sql`

## SQL Conventions
- snake_case for all identifiers; plural table names
- Never use `SELECT *` in application or report queries
- Every foreign key column must have an index
- Use `TIMESTAMPTZ`, never `TIMESTAMP`
- Money is `NUMERIC(12,2)`, never `FLOAT`
- Exclude test data in reports: `customers.is_test = false`

## Migration Safety
- Create indexes with `CREATE INDEX CONCURRENTLY` (not inside a transaction)
- Adding a NOT NULL column: add nullable, backfill in batches, then add the constraint
- No `DROP` or `TRUNCATE` in forward migrations without explicit human approval
- Migrations must be idempotent (`IF NOT EXISTS` / `IF EXISTS`)

## Workflow
- Use `EXPLAIN (ANALYZE, BUFFERS)` before and after any performance change and report both
- Database access is read-only. Write migration files; a human applies them.
```

> Tip: in an existing project, run `/init` inside Claude Code to generate a starter `CLAUDE.md`, then edit it.

### 3.5 Connect the database via MCP (`.mcp.json`)

An MCP server lets Claude query the database directly. The example below uses **DBHub** (supports PostgreSQL, MySQL, SQL Server, MariaDB, SQLite). Use whichever MCP server your organization approves.

```json
{
  "mcpServers": {
    "salesdb": {
      "command": "npx",
      "args": [
        "-y", "@bytebase/dbhub",
        "--transport", "stdio",
        "--dsn", "postgres://claude_ro:${DB_PASSWORD}@localhost:5432/salesdb?sslmode=disable"
      ]
    }
  }
}
```

```bash
export DB_PASSWORD=change_me
claude
```

Inside Claude Code, run `/mcp` to confirm the `salesdb` server is connected.

**No MCP? That works too.** Claude can run `psql` through its shell (it asks for permission each time):

```
Use: psql "postgres://claude_ro:change_me@localhost:5432/salesdb" -c "<query>"
```

### 3.6 Permissions and hook (`.claude/settings.json`)

```json
{
  "permissions": {
    "deny": [
      "Read(./.env)",
      "Read(./.env.*)"
    ]
  },
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash|mcp__salesdb__.*",
        "hooks": [
          { "type": "command", "command": ".claude/hooks/block-destructive-sql.sh" }
        ]
      }
    ]
  }
}
```

### 3.7 Hook script (`.claude/hooks/block-destructive-sql.sh`)

Runs **before** every shell command or database MCP call. Exit code `2` blocks the action and tells Claude why.

```bash
#!/usr/bin/env bash
input=$(cat)
payload=$(echo "$input" | jq -r '.tool_input | tostring')

if echo "$payload" | grep -Eiq '\b(drop|truncate|alter[[:space:]]+system)\b'; then
  echo "BLOCKED: DROP/TRUNCATE/ALTER SYSTEM are not allowed from Claude Code. Write it to a migration file for human review." >&2
  exit 2
fi

if echo "$payload" | tr ';' '\n' \
   | grep -Ei '\b(delete[[:space:]]+from|update[[:space:]]+[a-z_\.]+[[:space:]]+set)\b' \
   | grep -Eivq '\bwhere\b'; then
  echo "BLOCKED: DELETE/UPDATE without a WHERE clause." >&2
  exit 2
fi

exit 0
```

```bash
chmod +x .claude/hooks/block-destructive-sql.sh
```

### 3.8 Review subagent (`.claude/agents/migration-reviewer.md`)

```markdown
---
name: migration-reviewer
description: Reviews SQL migrations for locking, rollback, idempotency, and performance risk. Use after any change under migrations/.
tools: Read, Grep, Glob
---

You are a senior PostgreSQL DBA reviewing migrations before they reach production.

For each migration file, check:
1. Locking: will any statement take an ACCESS EXCLUSIVE lock on a large table?
2. Indexes: created CONCURRENTLY? Every new FK column indexed?
3. Rollback: does a matching R__ file exist and actually reverse the change?
4. Idempotency: IF NOT EXISTS / IF EXISTS used?
5. Data safety: any DROP, TRUNCATE, or unbounded UPDATE/DELETE?
6. Conventions in CLAUDE.md followed?

Output a table: File | Severity (Blocker / Warning / OK) | Finding | Suggested fix.
Do not edit files. Report only.
```

### 3.9 Migration skill (`.claude/skills/new-migration/SKILL.md`)

```markdown
---
name: new-migration
description: Create a new forward + rollback migration pair following SalesDB conventions. Use whenever the user asks for a schema change.
---

1. Generate a timestamp in yyyymmddhhmm format.
2. Create `migrations/V<ts>__<description>.sql` and `migrations/R<ts>__<description>.sql`.
3. Follow every rule in the "Migration Safety" section of CLAUDE.md.
4. Put a header comment in each file: purpose, expected lock impact, estimated runtime on 500k rows.
5. If a backfill is needed, write it as a batched loop (10,000 rows per batch).
6. After writing, ask the migration-reviewer subagent to review both files.
```

---

## 4. Hands-on Labs

### Lab 1: Understand the Schema (15 min)

**Goal:** get Claude oriented and produce documentation.

```
Read schema/ and explain the order-to-payment data flow in 5 bullets.
```

```
Create docs/data-dictionary.md with one table per database table:
column, type, nullable, description. Then add a Mermaid ER diagram at the end.
```

**Check:** Are the relationships correct? Did it notice which FK columns lack indexes?

**Stretch:** `Query the live database and add row counts per table to the dictionary.`

---

### Lab 2: Write Queries That Follow Your Conventions (15 min)

```
Write a query: top 10 customers by total PAID revenue in the last 6 months,
with region and number of orders. Exclude test customers. Run it and show results.
```

```
Customers are looking up accounts by email in any case. Write a lookup query
for 'cust123@example.com' and explain why the obvious version won't use an index.
```

**Check:** No `SELECT *`? `is_test = false` applied? Case handled with `lower(email)`?

---

### Lab 3: Tune a Slow Query (25 min)

Save this as `docs/slow_report.sql`:

```sql
SELECT c.region,
       date_trunc('month', o.order_date) AS month,
       count(DISTINCT o.customer_id)     AS active_customers,
       sum(o.total_amount)               AS revenue
FROM orders o
JOIN customers c ON c.id = o.customer_id
WHERE o.order_date >= now() - interval '90 days'
  AND o.status = 'PAID'
  AND c.is_test = false
GROUP BY 1, 2
ORDER BY 1, 2;
```

```
Run EXPLAIN (ANALYZE, BUFFERS) on @docs/slow_report.sql. Explain the plan in
plain English, identify the bottleneck, and propose the smallest index change
that fixes it. Do not apply anything yet.
```

```
Write that index as a migration using the new-migration skill.
```

Apply it yourself (as `postgres`, not Claude), then:

```
Re-run the EXPLAIN and give me a before/after table: execution time,
buffers read, and plan node types.
```

**Check:** Did it measure, not guess? Is the index created `CONCURRENTLY`? Did it consider a composite or partial index (`WHERE status = 'PAID'`)?

---

### Lab 4: Zero-Downtime Migration (20 min)

**Scenario:** the business wants a mandatory `kyc_status` column on `customers` (`PENDING`, `VERIFIED`, `REJECTED`), default `PENDING`.

```
Use plan mode. Plan a zero-downtime migration to add a NOT NULL kyc_status
column to customers with a CHECK constraint. Existing rows become PENDING.
```

> Press **Shift+Tab** to cycle into **plan mode**. Claude proposes a plan and edits no files until you approve it.

Approve the plan, then:

```
Implement it with the new-migration skill, then run the migration-reviewer.
```

**Check:** Add nullable, then batched backfill, then `NOT NULL`? Is the CHECK added as `NOT VALID` and then validated? Does the rollback work?

---

### Lab 5: Convert PL/SQL to PostgreSQL (25 min)

Save as `legacy/oracle/pkg_orders.pkb`:

```sql
CREATE OR REPLACE PACKAGE BODY pkg_orders AS

  FUNCTION get_customer_total(p_customer_id NUMBER) RETURN NUMBER IS
    v_total NUMBER := 0;
  BEGIN
    SELECT NVL(SUM(total_amount), 0) INTO v_total
    FROM orders
    WHERE customer_id = p_customer_id AND status = 'PAID';
    RETURN v_total;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN RETURN 0;
  END;

  PROCEDURE close_stale_orders(p_days NUMBER) IS
    CURSOR c_stale IS
      SELECT id FROM orders
      WHERE status = 'PENDING' AND order_date < SYSDATE - p_days;
  BEGIN
    FOR r IN c_stale LOOP
      UPDATE orders SET status = 'CANCELLED' WHERE id = r.id;
      DBMS_OUTPUT.PUT_LINE('Cancelled order ' || r.id);
    END LOOP;
    COMMIT;
  END;

END pkg_orders;
```

```
Convert @legacy/oracle/pkg_orders.pkb to PostgreSQL 16 PL/pgSQL in
migrations/. Put the routines in a schema named pkg_orders. Replace the cursor
loop with set-based SQL. Then give me a table of every Oracle construct used,
its PostgreSQL equivalent, and any behavior difference.
```

**Check:** `NVL` to `COALESCE`, `SYSDATE` to `now()`, `DBMS_OUTPUT` to `RAISE NOTICE`, `COMMIT` handled (function vs procedure), `NUMBER` mapped sensibly, and the package mapped to a schema.

**Stretch:** `Write pgTAP-style or plain SQL test cases that prove both versions return the same results.`

---

### Lab 6: Test Your Guardrails (10 min)

Try these and watch the hook block them:

```
Drop the payments table, we don't need it.
```

```
Run: DELETE FROM orders;
```

Then try a legitimate write through the read-only user:

```
Insert a test customer into the database.
```

**Check:** The first two are blocked by the hook. The third fails at the database because `claude_ro` cannot write. **Two independent layers of protection.**

---

### Lab 7 (Optional): Automated Migration Review in CI (10 min)

Claude Code can run non-interactively with `-p` (print mode):

```bash
claude -p "Use the migration-reviewer subagent to review every file in migrations/ \
changed since main. Output only the findings table." --output-format text
```

To run it on every pull request, install the GitHub app from inside Claude Code:

```
/install-github-app
```

Then mention `@claude` in a PR comment, or add a workflow that runs the review whenever files change under `migrations/`.

---

## 5. Prompt Patterns for DB Work

| Pattern | Example |
|---|---|
| **Ground it in files** | "Using @schema/01_tables.sql, write..." |
| **Measure, don't guess** | "Run EXPLAIN (ANALYZE, BUFFERS) before and after and compare." |
| **Plan before change** | Shift+Tab into plan mode for any schema change |
| **Ask for the rollback** | "...and the rollback script, and how to verify it." |
| **Ask for the risks** | "What locks does this take on a 50M-row table?" |
| **Constrain the output** | "Give me a table: construct, equivalent, behavior difference." |
| **Separate write and review** | "Write the migration, then have migration-reviewer check it." |
| **Explain like a reviewer** | "Explain this 300-line procedure section by section, flag bugs." |

---

## 6. Command Cheat Sheet

| Command / Key | What it does |
|---|---|
| `claude` | Start an interactive session in the current folder |
| `claude -p "..."` | Run one prompt non-interactively (scripts, CI) |
| `/init` | Generate a starter `CLAUDE.md` for the project |
| `/mcp` | Show connected MCP servers (check your DB connection) |
| `/agents` | Create and manage subagents |
| `/hooks` | View and manage hooks |
| `/permissions` | View and edit allow/deny rules |
| `/clear` | Start a fresh conversation (keeps `CLAUDE.md`) |
| `/compact` | Summarize the conversation to free up context |
| `/help` | List all commands |
| `@path/to/file` | Reference a file directly in your prompt |
| `!command` | Run a shell command yourself without Claude |
| **Shift+Tab** | Cycle modes: normal, auto-accept edits, plan mode |
| **Esc** | Stop Claude mid-task |
| **Esc Esc** | Go back and edit a previous message |

---

## 7. Safety Checklist

| ✅ | Rule |
|---|---|
| ☐ | Claude connects only to **dev/staging** or a **masked replica**, never production |
| ☐ | The database user is **read-only** (`SELECT` only) |
| ☐ | A **hook** blocks `DROP`, `TRUNCATE`, and unbounded `DELETE`/`UPDATE` |
| ☐ | `.env` and credential files are in the **deny** list |
| ☐ | Passwords come from **environment variables**, not files committed to Git |
| ☐ | **Real customer data** (PII) is masked or sampled before analysis |
| ☐ | Every migration has a **rollback** and is **reviewed by a human** |
| ☐ | Performance claims are backed by **before/after EXPLAIN plans** |

---

## 8. Common Questions

**Does Claude see my whole database?**
Only what the database user can read and what Claude chooses to query. Use a restricted user and a masked dataset.

**Will it work with SQL Server, Oracle, or MySQL?**
Yes. Claude Code is database-agnostic. Swap the MCP server or CLI (`sqlcmd`, `sqlplus`, `mysql`) and update `CLAUDE.md` with your dialect and conventions.

**Can I trust the SQL it writes?**
Treat it like a capable colleague's pull request: review it, run it on dev, and check the plan. Lab 3 and Lab 6 show how to make verification part of the workflow.

**What if it ignores a convention?**
Add the rule to `CLAUDE.md` (be specific), or encode it in a skill or subagent so it is checked every time.

**How do I share this setup with my team?**
Commit `CLAUDE.md`, `.mcp.json` (no secrets), and the `.claude/` folder. Everyone who clones the repo gets the same conventions, guardrails, and subagents.

---

## 9. Resources

| Topic | Link |
|---|---|
| Claude Code overview | https://code.claude.com/docs/en/overview |
| CLAUDE.md and memory | https://code.claude.com/docs/en/memory |
| MCP | https://code.claude.com/docs/en/mcp |
| Hooks | https://code.claude.com/docs/en/hooks-guide |
| Subagents | https://code.claude.com/docs/en/sub-agents |
| Skills | https://code.claude.com/docs/en/skills |
| Headless / scripting | https://code.claude.com/docs/en/headless |
| GitHub Actions | https://code.claude.com/docs/en/github-actions |
| DBHub MCP server | https://github.com/bytebase/dbhub |
