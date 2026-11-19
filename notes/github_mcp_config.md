# GitHub MCP for Claude Code (Remote Server + Personal Access Token)

Let Claude Code read and write GitHub issues, pull requests, and repository contents through GitHub's official remote MCP server.

**What you'll set up:**

1. A fine-grained Personal Access Token (PAT) scoped to your repo
2. The token, stored privately in an environment variable
3. The GitHub MCP server entry in `.mcp.json`
4. A test to confirm it works

---

## Step 1: Create a fine-grained PAT

1. On GitHub, click your avatar → **Settings**.
2. Scroll to the bottom of the left sidebar and open **Developer settings**.
3. Open **Personal access tokens** → **Fine-grained tokens** → **Generate new token**.
4. Fill in the form:
   - **Token name:** `claude-code-github-mcp`
   - **Expiration:** 7 to 30 days is enough for training. A short expiry limits the damage if the token leaks.
   - **Resource owner:** your user account, or your org if the repo lives there (orgs may require admin approval).
   - **Repository access:** **Only select repositories** → pick the demo repo. Avoid "All repositories".
5. Under **Repository permissions**, set:

   | Permission | Access | Used for |
   |---|---|---|
   | Contents | Read and write | commits and pushes |
   | Issues | Read and write | `/fix-issue`, `/create-issue` |
   | Pull requests | Read and write | raising and reviewing PRs |
   | Metadata | Read-only | added automatically |

6. Click **Generate token** and **copy it immediately**. GitHub shows it only once.

> [!WARNING]
> The PAT is a secret. Anyone who has it can act as you on the selected repos. Don't paste it into chat, tickets, or any file that gets committed.

## Step 2: Store the token in an environment variable

The token goes in an environment variable named `GITHUB_PAT`, never in a file in the repo.

**PowerShell (persists for your user):**

```powershell
[Environment]::SetEnvironmentVariable("GITHUB_PAT", "github_pat_xxxx", "User")
```

**PowerShell (current session only):**

```powershell
$env:GITHUB_PAT = "github_pat_xxxx"
```

**Bash / Git Bash (current session only):**

```bash
export GITHUB_PAT="github_pat_xxxx"
```

**CMD (persists for your user):**

```cmd
setx GITHUB_PAT "github_pat_xxxx"
```

`setx` doesn't change the CMD window you run it in. Close it and open a new one before starting Claude Code.

**CMD (current window only):**

```cmd
set GITHUB_PAT=github_pat_xxxx
```

Don't put spaces around `=` and don't add quotes in the `set` form, because CMD would store the quotes as part of the token.

To check it's set, run `echo %GITHUB_PAT%` in CMD (this prints the token, so don't do it on a shared screen). A better check is `if defined GITHUB_PAT (echo set) else (echo missing)`.

After setting a persistent variable, **open a new terminal**. Claude Code must be started from a shell that already has the variable.

## Step 3: Configure the MCP server

Add the `github` entry inside `mcpServers` in `.mcp.json` at the project root:

```json
{
  "mcpServers": {
    "github": {
      "type": "http",
      "url": "https://api.githubcopilot.com/mcp/",
      "headers": {
        "Authorization": "Bearer ${GITHUB_PAT}"
      }
    }
  }
}
```

If the file already has other servers (for example `atlassian`), add `github` next to them. Don't replace the file.

- `${GITHUB_PAT}` is expanded from the environment when Claude Code starts, so the token itself is never in the file. This is why `.mcp.json` is safe to commit and share.
- Everyone who clones the project gets the same config and only sets their own `GITHUB_PAT`.

**Or add it with the CLI.** Keep the `--header` value in **single quotes**:

```bash
claude mcp add --transport http github https://api.githubcopilot.com/mcp/ \
  --header 'Authorization: Bearer ${GITHUB_PAT}' --scope project
```

> [!WARNING]
> In double quotes, bash expands `${GITHUB_PAT}` and writes your real token into `.mcp.json` (PowerShell expands it to an empty string). After running the command, open `.mcp.json` and confirm it still shows the literal `${GITHUB_PAT}` before you commit.

Restart Claude Code after you edit `.mcp.json`. The first time it sees a project-scoped server it asks you to approve it. Accept the prompt.

## Step 4: Test it

1. Start `claude` in the project.
2. Run `/mcp` and confirm `github` shows as **connected**. Outside a session, `claude mcp list` shows the same.
3. Ask: *"List the open issues in this repo."* A list of issues (or an empty-list reply) means it works.

### Not working?

| Symptom | Check |
|---|---|
| `github` shows failed or needs auth | `GITHUB_PAT` isn't set in the shell that started Claude Code. Set it, open a **new** terminal, and restart `claude`. |
| 401 Unauthorized | The token is expired, revoked, or mistyped. Generate a new one. |
| 403 or 404 on issues or PRs | The token isn't scoped to this repo, or is missing a permission from Step 1. Edit the token's Repository access and permissions. |
| Org repo, still 403 | The org may require admin approval for fine-grained tokens, or restricts PATs. Ask an org admin. |
| Token appears in `.mcp.json` | The `--header` was in double quotes. Replace the value with `Bearer ${GITHUB_PAT}`, **revoke the leaked token**, and generate a new one. |
| Server not listed at all | Check that the JSON is valid and `github` is inside `mcpServers`. Restart the session. |
