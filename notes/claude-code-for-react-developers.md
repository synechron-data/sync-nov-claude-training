# Claude Code for React Developers
### Participant Guide and Hands-on Labs

---

## What You Will Learn

| # | Outcome |
|---|---|
| 1 | Set up Claude Code on a React + TypeScript project |
| 2 | Teach Claude your component, state, and testing conventions with `CLAUDE.md` |
| 3 | Fix real React bugs (effects, race conditions, stale state) test-first |
| 4 | Find and fix rendering, correctness, and accessibility issues |
| 5 | Build a feature from a spec using plan mode and a reusable skill |
| 6 | Migrate class components to hooks + TypeScript |
| 7 | Let Claude open your app in a real browser to verify its own work |
| 8 | Add guardrails: auto-format, typecheck-before-finish, and blocked risky commands |

---

## 1. How Claude Code Fits React Work

Claude Code is an AI coding agent that runs in your terminal. It **reads and edits your project files**, **runs commands** (`npm test`, `tsc`, `lint`) with your approval, and through **MCP servers** can drive a real browser to click through your UI.

| You give it | It can do |
|---|---|
| A component + a bug report | Reproduce with a failing test, fix, and prove the fix |
| A design spec or screenshot | Build the component, styles, and tests to your conventions |
| A slow or janky screen | Find unnecessary work, unstable keys, prop mutation, and fix them |
| Legacy class components | Convert to hooks + TypeScript and flag hidden bugs |
| A running dev server | Open it in a browser, test flows, check accessibility |
| A pull request | Review for hooks rules, a11y, performance, and missing tests |

> **Golden rule:** Claude proposes, **tests prove**, and **you review the diff**. Never accept a change you have not seen pass `test`, `lint`, and `typecheck`.

---

## 2. Prerequisites

| Item | Version / Note |
|---|---|
| Node.js | 20 or later |
| Git | Any recent version |
| `jq` | Needed for the hook scripts |
| Browser | Chrome or Chromium (for the Playwright MCP lab) |
| React DevTools | Browser extension, used for the Profiler in Lab 3 |
| Claude account | Pro, Max, Team, Enterprise, or a Claude Console (API) account |

### Install Claude Code

```bash
npm install -g @anthropic-ai/claude-code
claude --version
```

---

## 3. Lab Project Setup

### 3.1 Create the app

```bash
npm create vite@latest taskboard -- --template react-ts
cd taskboard
npm install
npm install -D vitest jsdom @testing-library/react @testing-library/dom \
  @testing-library/jest-dom @testing-library/user-event prettier
npm pkg set scripts.test="vitest" scripts.typecheck="tsc -b"
git init && git add -A && git commit -m "Initial scaffold"
```

Target structure once setup is complete:

```
taskboard/
├── CLAUDE.md
├── .mcp.json
├── .prettierrc
├── .claude/
│   ├── settings.json
│   ├── hooks/
│   │   ├── format-file.sh
│   │   └── typecheck-on-stop.sh
│   ├── agents/
│   │   └── react-reviewer.md
│   └── skills/
│       └── new-component/
│           └── SKILL.md
├── src/
│   ├── api/tasks.ts
│   ├── components/
│   │   ├── TaskSearch.tsx
│   │   ├── TaskList.tsx
│   │   └── TaskList.test.tsx
│   ├── legacy/TaskStats.jsx
│   ├── setupTests.ts
│   └── App.tsx
├── docs/
└── vite.config.ts
```

```bash
mkdir -p src/api src/components src/legacy docs .claude/hooks .claude/agents .claude/skills/new-component
```

### 3.2 Configure tests

**`vite.config.ts`** (replace the contents)

```ts
/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.ts',
  },
})
```

**`src/setupTests.ts`**

```ts
import '@testing-library/jest-dom/vitest'
```

**`tsconfig.app.json`**: find the `"types"` line and add `vitest/globals`:

```json
"types": ["vite/client", "vitest/globals"],
```

**`.prettierrc`**

```json
{ "semi": false, "singleQuote": true }
```

### 3.3 Sample app code

> These files contain **intentional bugs**. Do not fix them yet; the labs do that.

**`src/api/tasks.ts`** (fake API with random network latency)

```ts
export type Task = {
  id: number
  title: string
  assignee: string
  status: 'todo' | 'doing' | 'done'
  priority: 'low' | 'medium' | 'high'
}

const TITLES = ['Fix login bug', 'Write docs', 'Review PR', 'Update deps', 'Design banner']
const PEOPLE = ['Asha', 'Ravi', 'Meera', 'John', 'Sara']

export const TASKS: Task[] = Array.from({ length: 2000 }, (_, i) => ({
  id: i + 1,
  title: `${TITLES[i % 5]} #${i + 1}`,
  assignee: PEOPLE[(i * 3) % 5],
  status: (['todo', 'doing', 'done'] as const)[i % 3],
  priority: (['low', 'medium', 'high'] as const)[(i * 7) % 3],
}))

// Simulates a network call with random latency (200 to 1400 ms)
export function searchTasks(query: string): Promise<Task[]> {
  const delay = 200 + Math.random() * 1200
  return new Promise((resolve) =>
    setTimeout(
      () => resolve(TASKS.filter((t) => t.title.toLowerCase().includes(query.toLowerCase()))),
      delay,
    ),
  )
}
```

**`src/components/TaskSearch.tsx`**

```tsx
import { useEffect, useState } from 'react'
import { searchTasks, type Task } from '../api/tasks'
import TaskList from './TaskList'

export default function TaskSearch() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Task[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    searchTasks(query).then((data) => {
      setResults(data)
      setLoading(false)
    })
  }, [query])

  return (
    <div>
      <input
        placeholder="Search tasks"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      {loading && <p>Loading...</p>}
      <TaskList tasks={results} onToggle={(id) => console.log('toggle', id)} />
    </div>
  )
}
```

**`src/components/TaskList.tsx`**

```tsx
import type { Task } from '../api/tasks'

type Props = {
  tasks: Task[]
  onToggle: (id: number) => void
}

export default function TaskList({ tasks, onToggle }: Props) {
  const sorted = tasks.sort((a, b) => a.title.localeCompare(b.title))

  return (
    <div>
      {sorted.map((task, index) => (
        <div key={index} className="task-row" onClick={() => onToggle(task.id)}>
          <span style={{ color: task.priority === 'high' ? 'red' : '#999' }}>
            {task.priority}
          </span>
          <span>{task.title}</span>
          <span>{task.assignee}</span>
        </div>
      ))}
    </div>
  )
}
```

**`src/components/TaskList.test.tsx`**

```tsx
import { render, screen } from '@testing-library/react'
import TaskList from './TaskList'
import type { Task } from '../api/tasks'

const tasks: Task[] = [
  { id: 1, title: 'B task', assignee: 'Asha', status: 'todo', priority: 'high' },
  { id: 2, title: 'A task', assignee: 'Ravi', status: 'done', priority: 'low' },
]

test('renders every task title', () => {
  render(<TaskList tasks={tasks} onToggle={() => {}} />)
  expect(screen.getByText('A task')).toBeInTheDocument()
  expect(screen.getByText('B task')).toBeInTheDocument()
})
```

**`src/legacy/TaskStats.jsx`** (legacy class component, not used in the app yet)

```jsx
import React from 'react'

export default class TaskStats extends React.Component {
  constructor(props) {
    super(props)
    this.state = { counts: { todo: 0, doing: 0, done: 0 }, lastUpdated: null }
    this.handleRefresh = this.handleRefresh.bind(this)
  }

  componentDidMount() {
    this.calculate()
    this.timer = setInterval(() => this.calculate(), 5000)
  }

  componentDidUpdate(prevProps) {
    if (prevProps.tasks !== this.props.tasks) {
      this.calculate()
    }
  }

  calculate() {
    const counts = { todo: 0, doing: 0, done: 0 }
    this.props.tasks.forEach((t) => {
      counts[t.status]++
    })
    this.setState({ counts, lastUpdated: new Date() })
  }

  handleRefresh() {
    this.calculate()
  }

  render() {
    const { counts, lastUpdated } = this.state
    return (
      <div className="stats">
        <h3>Task Stats</h3>
        <ul>
          <li>To do: {counts.todo}</li>
          <li>Doing: {counts.doing}</li>
          <li>Done: {counts.done}</li>
        </ul>
        <small>Updated: {lastUpdated && lastUpdated.toLocaleTimeString()}</small>
        <button onClick={this.handleRefresh}>Refresh</button>
      </div>
    )
  }
}
```

**`src/App.tsx`** (replace the contents)

```tsx
import TaskSearch from './components/TaskSearch'

export default function App() {
  return (
    <main>
      <h1>TaskBoard</h1>
      <TaskSearch />
    </main>
  )
}
```

**Verify the setup:**

```bash
npm run typecheck && npm test -- --run && npm run lint && npm run dev
```

Open the app, type quickly in the search box, and notice the results sometimes do not match what you typed. That is Lab 2.

### 3.4 `CLAUDE.md` (project memory)

Claude reads this file at the start of every session. Put your team rules here.

```markdown
# TaskBoard

## Stack
- React 19 + TypeScript, Vite, Vitest + React Testing Library, Prettier, oxlint
- Commands: `npm run dev`, `npm test -- --run`, `npm run lint`, `npm run typecheck`, `npm run build`

## Components
- Function components only. One component per file, default export, PascalCase filename.
- Props typed with a `type Props = {...}` declared above the component. No `any`.
- Keep components under ~150 lines. Extract hooks into `src/hooks/useXxx.ts`.

## State and Effects
- Derive values during render. Do not copy props or derived data into state.
- `useEffect` is only for syncing with external systems (network, timers, subscriptions, DOM APIs).
- Every effect that fetches must handle out-of-order responses (AbortController or an ignore flag).
- Every effect that starts a timer or subscription must clean it up.
- Never mutate props or state (no `.sort()` / `.push()` on them; use `toSorted()` or copies).
- Keys must be stable IDs, never array indexes.

## Accessibility (WCAG 2.1 AA)
- Interactive elements are `<button>`, `<a>`, or form controls. Never `div onClick`.
- Every input has a visible `<label>`. Group related controls with `<fieldset>` + `<legend>`.
- Do not convey meaning with color alone. Text contrast at least 4.5:1.

## Testing
- Every new component gets a `*.test.tsx` next to it.
- Query by role and accessible name (`getByRole('button', { name: /save/i })`), not test IDs or classes.
- Use `@testing-library/user-event` for interactions. Use fake timers for debounce/latency.
- For bug fixes: write a failing test first, then fix.

## Performance
- Measure before optimizing. Do not add `memo` / `useMemo` / `useCallback` without a reason stated in a comment.
- Lists over ~200 rows: paginate or virtualize.

## Workflow
- Do not add new npm dependencies without asking first.
- After changes, run test, lint, and typecheck, and report the results.
```

> Tip: in an existing project, run `/init` inside Claude Code to generate a starter `CLAUDE.md`, then edit it.

### 3.5 Give Claude a browser: Playwright MCP (`.mcp.json`)

The Playwright MCP server lets Claude open your running app, click, type, read the accessibility tree, and take screenshots.

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["@playwright/mcp@latest"]
    }
  }
}
```

Or add it from the command line:

```bash
claude mcp add playwright -- npx @playwright/mcp@latest
```

Inside Claude Code, run `/mcp` to confirm `playwright` is connected.

### 3.6 Permissions and hooks (`.claude/settings.json`)

```json
{
  "permissions": {
    "allow": [
      "Bash(npm test:*)",
      "Bash(npm run lint)",
      "Bash(npm run typecheck)",
      "Bash(npx vitest:*)"
    ],
    "deny": [
      "Read(./.env)",
      "Read(./.env.*)",
      "Bash(npm publish:*)",
      "Bash(git push:*)"
    ]
  },
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          { "type": "command", "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/format-file.sh" }
        ]
      }
    ],
    "Stop": [
      {
        "hooks": [
          { "type": "command", "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/typecheck-on-stop.sh" }
        ]
      }
    ]
  }
}
```

| Rule | Effect |
|---|---|
| `allow` | Claude runs tests, lint, and typecheck without asking every time |
| `deny` | Claude cannot read secrets, publish packages, or push to Git |
| `PostToolUse` hook | Every file Claude edits is auto-formatted and lint-fixed |
| `Stop` hook | Claude cannot say "done" while TypeScript has errors |

### 3.7 Hook scripts

**`.claude/hooks/format-file.sh`**

```bash
#!/usr/bin/env bash
# PostToolUse: format and lint-fix the file Claude just edited
file=$(jq -r '.tool_input.file_path // empty')

case "$file" in
  *.ts|*.tsx|*.js|*.jsx|*.css|*.json)
    npx prettier --write "$file" >/dev/null 2>&1
    npx oxlint --fix "$file" >/dev/null 2>&1
    ;;
esac
exit 0
```

**`.claude/hooks/typecheck-on-stop.sh`**

```bash
#!/usr/bin/env bash
# Stop: do not let Claude finish while TypeScript has errors
input=$(cat)

# Avoid an endless loop if Claude is already continuing because of this hook
if [ "$(echo "$input" | jq -r '.stop_hook_active')" = "true" ]; then
  exit 0
fi

if ! out=$(npx tsc -b 2>&1); then
  echo "TypeScript errors found. Fix them before finishing:" >&2
  echo "$out" >&2
  exit 2
fi
exit 0
```

```bash
chmod +x .claude/hooks/*.sh
```

> Exit code `2` sends the error text back to Claude, and it keeps working until the types are clean.

### 3.8 Review subagent (`.claude/agents/react-reviewer.md`)

```markdown
---
name: react-reviewer
description: Reviews React components for hooks correctness, accessibility, performance, and test coverage. Use after creating or changing any component.
tools: Read, Grep, Glob
---

You are a senior React engineer reviewing code before it is merged.

For each changed component, check:
1. Hooks: rules of hooks, missing or unnecessary dependencies, effects used for derived data, missing cleanup, race conditions in fetches.
2. State: duplicated or derivable state, mutation of props/state, unstable keys.
3. Accessibility: semantic elements, labels, keyboard access, focus management, color-only meaning, contrast.
4. Performance: unnecessary re-renders, expensive work in render without reason, large unvirtualized lists.
5. Tests: does a test exist, does it query by role, does it cover the main interaction and edge cases?
6. Conventions in CLAUDE.md followed?

Output a table: File | Line | Severity (Blocker / Warning / Nit) | Finding | Suggested fix.
Do not edit files. Report only.
```

### 3.9 Component skill (`.claude/skills/new-component/SKILL.md`)

```markdown
---
name: new-component
description: Create a new React component with types, styles, and tests following TaskBoard conventions. Use whenever the user asks for a new component or UI feature.
---

1. Confirm the component name and where it lives (default: `src/components/<Name>.tsx`).
2. Write the component following every rule in CLAUDE.md (Components, State and Effects, Accessibility).
3. Put styles in `src/components/<Name>.module.css` if any styling is needed.
4. Write `src/components/<Name>.test.tsx` covering: initial render, each user interaction, and one edge case (empty or error state). Query by role.
5. Run `npm test -- --run`, `npm run lint`, and `npm run typecheck`. Fix anything that fails.
6. Ask the react-reviewer subagent to review the new files and address any Blocker findings.
7. Summarize: files created, test results, and any reviewer warnings left open.
```

Commit everything before starting the labs:

```bash
git add -A && git commit -m "Lab setup"
```

---

## 4. Hands-on Labs

> **Tip:** commit after each lab. If Claude goes in a wrong direction, `git checkout .` takes you back, or press **Esc Esc** to rewind the conversation.

### Lab 1: Understand the Codebase (10 min)

**Goal:** get Claude oriented and produce documentation.

```
Explain how data flows from the search box to the rendered list. Keep it to 5 bullets.
```

```
Create docs/components.md: one section per component with a props table
(name, type, required, description) and a Mermaid diagram of the component tree.
```

**Check:** Is the props table accurate? Did it mention anything suspicious it noticed along the way?

---

### Lab 2: Fix a Race Condition, Test-First (25 min)

**Bug report:** *"When I type fast, the results sometimes show matches for an older search term."*

```
Read @src/components/TaskSearch.tsx and @src/api/tasks.ts. Explain why the
results can show the wrong search. Do not fix it yet.
```

```
Write a failing test in src/components/TaskSearch.test.tsx that reproduces it:
mock searchTasks so the first request resolves AFTER the second, and assert the
final results match the latest query. Use fake timers. Run it and show me it fails.
```

```
Now fix it. Also debounce the search by 300 ms and give the input a visible label.
Run the tests and show they pass.
```

**Check:**
- Did the test **fail before** and **pass after**?
- Does the fix ignore stale responses (ignore flag or `AbortController`) in the effect cleanup?
- Is the debounce cleaned up on unmount?
- Does the input now have a `<label>`?

**Stretch:** `Extract the logic into a reusable useDebouncedSearch hook with its own tests.`

---

### Lab 3: Correctness, Performance, and Accessibility in TaskList (25 min)

First, measure. Open React DevTools, go to **Profiler**, record yourself typing a few characters, and note the render time for `TaskList`.

```
Review @src/components/TaskList.tsx against CLAUDE.md. List every issue with
its category (correctness, performance, accessibility), why it matters, and the
fix. Do not change any code yet.
```

```
Fix the correctness and accessibility issues first, then the performance ones.
Update the tests to cover: sorting does not mutate the input, and a task can be
toggled with the keyboard.
```

Record in the Profiler again and compare.

**Check:** Did it find all of these?

| Issue | Category |
|---|---|
| `tasks.sort()` mutates the parent's array | Correctness |
| `key={index}` causes wrong reuse when the list changes | Correctness |
| `div onClick` is not focusable or keyboard operable | Accessibility |
| Priority shown by color only; `#999` fails contrast | Accessibility |
| 2,000 rows rendered at once | Performance |

**Stretch:** `Virtualize the list. Ask me before adding any dependency, and explain the trade-offs of the options.`

---

### Lab 4: Build a Feature from a Spec (30 min)

**Spec: TaskFilters**
- Filter by **status** (checkboxes: To do, Doing, Done; multiple allowed)
- Filter by **priority** (select: All, Low, Medium, High)
- A **Clear filters** button
- Filters are stored in the **URL query string** (`?status=todo,doing&priority=high`) so the view can be shared
- Filtering is **derived during render**, not copied into state
- Fully keyboard accessible, with `fieldset` + `legend` for the status group

Press **Shift+Tab** until you see **plan mode**, then:

```
Plan the TaskFilters feature from this spec: <paste the spec>.
Show me the files you will create or change, the state design, and the test cases.
```

Review the plan, push back on anything you disagree with, then approve and:

```
Implement it using the new-component skill.
```

**Check:**
- Did it keep only the source of truth (URL) and derive the filtered list?
- Do the tests cover each filter, the combination, Clear, and loading from a URL?
- Did the reviewer subagent run, and were Blocker findings addressed?

**Stretch:** paste or drag a screenshot of a design into the terminal and ask: `Style TaskFilters to match this design using CSS modules.`

---

### Lab 5: Migrate a Class Component to Hooks + TypeScript (20 min)

```
Convert @src/legacy/TaskStats.jsx to a TypeScript function component at
src/components/TaskStats.tsx. Before converting, list any bugs in the original.
Then write tests and render it in App above the search, using the full task list.
```

**Check:** Did it find these?

| Original | Expected in the new version |
|---|---|
| `setInterval` is never cleared (memory leak) | Effect returns a cleanup that clears the interval |
| `counts` stored in state but derivable from props | Computed during render (with `useMemo` only if justified) |
| `componentDidUpdate` sync logic | Disappears once counts are derived |
| Untyped props | `type Props = { tasks: Task[] }` |

**Discussion:** does the 5-second refresh still make sense once counts are derived from props? Ask Claude and decide.

---

### Lab 6: Let Claude Test in a Real Browser (20 min)

Start the dev server in a second terminal:

```bash
npm run dev
```

Then in Claude Code:

```
Using the Playwright MCP, open http://localhost:5173. Search for "docs",
apply the High priority filter, then clear filters. Report anything that looks
broken, and take a screenshot of each step.
```

```
Using the browser's accessibility snapshot, audit the page: check that every
control has an accessible name, the tab order makes sense, and the filters work
with the keyboard only. Give me a findings table, then fix the issues.
```

**Check:** Did it actually navigate and interact, or just read code? Did keyboard-only navigation reach every control?

**Stretch:** `Turn the flow you just tested into a Playwright test in e2e/filters.spec.ts. Ask before installing @playwright/test.`

---

### Lab 7: Test Your Guardrails (10 min)

Try each of these and observe what happens:

| Prompt | Expected result |
|---|---|
| `Add a function with messy formatting to src/utils.ts` | The file is auto-formatted after the edit (PostToolUse hook) |
| `Change the Task type's id to string, but only update tasks.ts` | Claude is stopped by the typecheck and must fix the callers (Stop hook) |
| `Read the .env file` | Blocked by the deny rule |
| `Publish this package to npm` | Blocked by the deny rule |
| `Push my changes to GitHub` | Blocked by the deny rule |

**Check:** run `/hooks` and `/permissions` inside Claude Code to see what is active.

---

### Lab 8 (Optional): Automated PR Review in CI (10 min)

Claude Code can run non-interactively with `-p` (print mode):

```bash
claude -p "Use the react-reviewer subagent to review every component changed \
since main. Output only the findings table." --output-format text
```

To run it on pull requests, install the GitHub app from inside Claude Code:

```
/install-github-app
```

Then mention `@claude` in a PR comment, or add a workflow that runs the review whenever files change under `src/`.

---

## 5. Prompt Patterns for React Work

| Pattern | Example |
|---|---|
| **Explain before fixing** | "Explain why this happens. Do not fix it yet." |
| **Test first** | "Write a failing test that reproduces it, run it, then fix." |
| **Ground it in files** | "Following @src/components/TaskList.tsx and CLAUDE.md, build..." |
| **Plan before big changes** | Shift+Tab into plan mode for features and refactors |
| **Measure, don't guess** | "I recorded 120 ms in the Profiler. What is causing it?" |
| **Show, don't describe** | Paste a screenshot: "Make it look like this." |
| **Verify in the browser** | "Open the app with Playwright and confirm the fix." |
| **Constrain dependencies** | "Ask before adding any npm package." |
| **Separate write and review** | "Build it, then have react-reviewer check it." |

---

## 6. Command Cheat Sheet

| Command / Key | What it does |
|---|---|
| `claude` | Start an interactive session in the current folder |
| `claude -p "..."` | Run one prompt non-interactively (scripts, CI) |
| `/init` | Generate a starter `CLAUDE.md` for the project |
| `/mcp` | Show connected MCP servers (check Playwright) |
| `/agents` | Create and manage subagents |
| `/hooks` | View and manage hooks |
| `/permissions` | View and edit allow/deny rules |
| `/clear` | Start a fresh conversation (keeps `CLAUDE.md`) |
| `/compact` | Summarize the conversation to free up context |
| `/help` | List all commands |
| `@path/to/file` | Reference a file directly in your prompt |
| `!command` | Run a shell command yourself without Claude |
| Paste / drag an image | Share a screenshot or design mockup |
| **Shift+Tab** | Cycle modes: normal, auto-accept edits, plan mode |
| **Esc** | Stop Claude mid-task |
| **Esc Esc** | Go back and edit a previous message |

---

## 7. Safety and Quality Checklist

| ✅ | Rule |
|---|---|
| ☐ | Commit before each task so any change can be undone with Git |
| ☐ | `.env` and secret files are in the **deny** list |
| ☐ | `npm publish` and `git push` are **denied**; a human ships |
| ☐ | New **dependencies** require your approval |
| ☐ | Every change passes **test, lint, and typecheck** before you accept it |
| ☐ | Bug fixes come with a test that **failed before** the fix |
| ☐ | You **read the diff** (`git diff`) before committing |
| ☐ | UI changes are **verified in a browser**, not just in tests |
| ☐ | Accessibility is checked with the **keyboard only** at least once |

---

## 8. Common Questions

**Does it work with Next.js, Remix, or React Native?**
Yes. Update `CLAUDE.md` with your framework, routing, and data-fetching conventions (for example, Server vs Client Components in Next.js). The workflow stays the same.

**Can it turn a Figma design into components?**
Paste a screenshot directly into the terminal, or connect a Figma MCP server so Claude can read design details. Always review spacing and tokens against your design system.

**It keeps using `useEffect` for derived state. How do I stop that?**
Make the rule explicit in `CLAUDE.md` (see "State and Effects"), and let the `react-reviewer` subagent catch it. Rules in files are followed far more reliably than reminders in chat.

**Will it install random packages?**
Not if you say so in `CLAUDE.md` and keep the default permission prompts. You approve every command that is not on your allow list.

**How do I share this setup with my team?**
Commit `CLAUDE.md`, `.mcp.json`, `.prettierrc`, and the `.claude/` folder. Everyone who clones the repo gets the same conventions, hooks, subagents, and skills.

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
| Playwright MCP | https://github.com/microsoft/playwright-mcp |
| React: You Might Not Need an Effect | https://react.dev/learn/you-might-not-need-an-effect |
| Testing Library queries | https://testing-library.com/docs/queries/about |
