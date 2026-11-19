<#
 PreToolUse demo hook: shows the different ways a hook can respond.

 Pick a mode by editing $DefaultMode below, or set PRETOOL_MODE in the env:
   allow    - exit 0, no output: tool call proceeds normally (default)
   log      - log the stdin payload, then allow
   override - allow, but rewrite the tool input via updatedInput
   block    - exit 2 hard block; stderr is shown to Claude as the reason
   deny     - JSON permissionDecision 'deny' on stdout, exit 0

 Works on Windows PowerShell 5.1 and PowerShell 7+.
 Deliberately no $ErrorActionPreference = 'Stop': logging must never change
 the hook's decision.
#>

$DefaultMode = 'allow'
$Mode = if ($env:PRETOOL_MODE) { $env:PRETOOL_MODE } else { $DefaultMode }

$ProjectDir = if ($env:CLAUDE_PROJECT_DIR) { $env:CLAUDE_PROJECT_DIR } else { '.' }
$LogFile = Join-Path $ProjectDir '.claude/hook-logs/pretooluse-demo.log'

function Write-LogMessage {
    param([string]$Message)
    try {
        $dir = Split-Path -Parent $LogFile
        [System.IO.Directory]::CreateDirectory($dir) | Out-Null
        # UTF-8 without BOM (Add-Content -Encoding utf8 writes a BOM on 5.1)
        [System.IO.File]::AppendAllText($LogFile, $Message + "`n", (New-Object System.Text.UTF8Encoding($false)))
    } catch {
        # logging must never change the hook's decision
    }
}

function Write-LogPayload {
    param($Payload)
    # IST = UTC+5:30, no DST. Computed from UTC so it needs no timezone database
    # (Windows and IANA zone ids differ). Format matches toLocaleString('sv-SE').
    $timeStamp = (Get-Date).ToUniversalTime().AddMinutes(330).ToString('yyyy-MM-dd HH:mm:ss')
    Write-LogMessage ''
    Write-LogMessage ('-' * 60)
    Write-LogMessage "[$timeStamp] mode=$Mode tool=$($Payload.tool_name)"
    Write-LogMessage 'STDIN PAYLOAD:'
    Write-LogMessage ($Payload | ConvertTo-Json -Depth 20)
}

function Write-Stdout {
    param([string]$Text)
    [Console]::Out.Write($Text)
}

function Write-Stderr {
    param([string]$Text)
    [Console]::Error.WriteLine($Text)
}

# Read all of stdin as UTF-8
$reader = New-Object System.IO.StreamReader([Console]::OpenStandardInput(), (New-Object System.Text.UTF8Encoding($false)))
$raw = $reader.ReadToEnd()

# Parse check: payload must be a JSON object
$payload = $null
try {
    $payload = $raw | ConvertFrom-Json -ErrorAction Stop
} catch {
    $payload = $null
}

if ($null -eq $payload -or $payload -isnot [System.Management.Automation.PSCustomObject]) {
    if ($Mode -eq 'block' -or $Mode -eq 'deny') {
        Write-Stderr '[pretooluse-demo] could not parse hook input; blocking.'
        exit 2
    }
    exit 0
}

switch ($Mode) {
    # ------------------------------------ Allow and logging
    'log' {
        Write-LogPayload $payload
        exit 0
    }

    # ------------------------------------ Allow and override with logging
    # permissionDecision 'allow' skips the permission prompt, so an 'ask'
    # rule (e.g. git push) would not be asked. Safe here only because the
    # command is replaced with a harmless echo.
    'override' {
        Write-LogPayload $payload

        $updatedInput = [ordered]@{}
        if ($payload.tool_input) {
            foreach ($prop in $payload.tool_input.PSObject.Properties) {
                $updatedInput[$prop.Name] = $prop.Value
            }
        }
        $updatedInput['command'] = 'echo overridden by custom script'

        $response = [ordered]@{
            hookSpecificOutput = [ordered]@{
                hookEventName            = 'PreToolUse'
                permissionDecision       = 'allow'
                permissionDecisionReason = '[pretooluse-demo] hook allowed and overrode this tool call.'
                updatedInput             = $updatedInput
            }
        }
        Write-LogMessage ($response | ConvertTo-Json -Depth 20)
        Write-Stdout ($response | ConvertTo-Json -Depth 20 -Compress)
        exit 0
    }

    # ---------------------------------- Hard Block
    # Exit code 2 hard-blocks the tool call; Claude Code treats it as an error
    # and shows stderr output (if any) as the reason - no JSON response needed.
    'block' {
        Write-LogPayload $payload
        Write-LogMessage 'BLOCKED: exit 2 - see stderr for reason'
        Write-Stderr '[pretooluse-demo] hook blocked this tool call.'
        exit 2
    }

    # ---------------------------------- Hard Block with Deny
    # Deny: rejects the tool call via structured JSON on stdout while exiting
    # cleanly (0). permissionDecisionReason is shown to Claude, explaining why
    # the call was denied - unlike exit 2, no stderr text is needed.
    'deny' {
        Write-LogPayload $payload
        $response = [ordered]@{
            hookSpecificOutput = [ordered]@{
                hookEventName            = 'PreToolUse'
                permissionDecision       = 'deny'
                permissionDecisionReason = '[pretooluse-demo] hook blocked this tool call.'
            }
        }
        Write-LogMessage 'BLOCK RESPONSE:'
        Write-LogMessage ($response | ConvertTo-Json -Depth 20)
        Write-Stdout ($response | ConvertTo-Json -Depth 20 -Compress)
        exit 0
    }

    # ---------------------------------- Allow (default)
    default {
        exit 0
    }
}
