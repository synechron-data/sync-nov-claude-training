#!/usr/bin/env node
// Stop hook: enforces the CLAUDE.md rules that say to invoke the `write-tests`
// and `write-docs` skills (via the Skill tool) after code changes.
//
// Looks at the turn that just finished (everything after the last real user
// prompt), works out which skills are owed from the files that were edited, and
// blocks the stop once if they were not invoked. If Claude is already
// continuing because of an earlier block (stop_hook_active), it only warns, so
// this can never loop.
//
// Limits: matches by file path, and only sees Edit/Write/NotebookEdit tool
// calls (not files changed through Bash). The transcript format is internal to
// Claude Code, so the parsing below may need updating if that format changes.
const fs = require('fs');
const path = require('path');

const PROJECT_DIR = process.env.CLAUDE_PROJECT_DIR || process.cwd();
const LOG_FILE = path.join(PROJECT_DIR, '.claude', 'hook-logs', 'enforce-skills.log');

const EDIT_TOOLS = new Set(['Edit', 'Write', 'NotebookEdit']);

// Project-relative, forward-slash path, or null if the file is outside the project.
function toRelative(filePath, projectDir = PROJECT_DIR) {
  const rel = path.relative(projectDir, path.resolve(projectDir, filePath));
  // On Windows a file on another drive comes back as an absolute path.
  if (rel.startsWith('..') || path.isAbsolute(rel)) return null;
  return rel.split(path.sep).join('/');
}

// Code that needs tests: backend main code, frontend app code (not specs, not styles).
function needsTests(rel) {
  if (/^backend\/src\/main\/java\/.+\.java$/.test(rel)) return true;
  return /^frontend\/src\/app\/.+\.(ts|html)$/.test(rel) && !/\.spec\.ts$/.test(rel);
}

// Code that changes documented behaviour: controllers, entities, DTO records,
// auth/security/CORS config, exception handling, frontend routes and services.
function needsDocs(rel) {
  const java = /^backend\/src\/main\/java\/.+\/([^/]+)\.java$/.exec(rel);
  if (java) {
    if (/\/(auth|config|exception)\//.test(rel)) return true;
    return !/(Service|Repository|Application)$/.test(java[1]);
  }
  if (rel === 'backend/src/main/resources/application.properties') return true;
  return /^frontend\/src\/app\/(.*\/)?([^/]*\.service\.ts|app\.routes\.ts|[^/]*\.interceptor\.ts)$/.test(rel)
    && !/\.spec\.ts$/.test(rel);
}

function isRealUserPrompt(entry) {
  if (entry.type !== 'user') return false;
  const content = entry.message?.content;
  // Tool results are also recorded as "user" entries; they don't start a new turn.
  return !(Array.isArray(content) && content.some(c => c.type === 'tool_result'));
}

// Ordered list of events for the current turn: { kind: 'edit', rel } | { kind: 'skill', name }.
function collectTurnEvents(transcriptLines, projectDir = PROJECT_DIR) {
  let events = [];
  for (const line of transcriptLines) {
    if (!line.trim()) continue;
    let entry;
    try { entry = JSON.parse(line); } catch { continue; }
    if (isRealUserPrompt(entry)) { events = []; continue; }
    if (entry.type !== 'assistant' || !Array.isArray(entry.message?.content)) continue;
    for (const block of entry.message.content) {
      if (block.type !== 'tool_use') continue;
      if (EDIT_TOOLS.has(block.name)) {
        const file = block.input?.file_path ?? block.input?.notebook_path;
        const rel = file && toRelative(file, projectDir);
        if (rel) events.push({ kind: 'edit', rel });
      } else if (block.name === 'Skill' && block.input?.skill) {
        // Plugin skills are namespaced ("plugin:skill"); keep the bare name.
        events.push({ kind: 'skill', name: String(block.input.skill).split(':').pop() });
      }
    }
  }
  return events;
}

// Which skills are still owed. A skill counts if it was invoked after the first
// edit that required it; write-docs must also come after write-tests when both are owed.
function missingSkills(events) {
  const firstEdit = pred => events.findIndex(e => e.kind === 'edit' && pred(e.rel));
  const skillAfter = (name, from) => events.findIndex((e, i) => i > from && e.kind === 'skill' && e.name === name);

  const testsEdit = firstEdit(needsTests);
  const docsEdit = firstEdit(needsDocs);
  const missing = [];

  let testsAt = -1;
  if (testsEdit >= 0) {
    testsAt = skillAfter('write-tests', testsEdit);
    if (testsAt < 0) missing.push('write-tests');
  }
  if (docsEdit >= 0) {
    const docsAt = skillAfter('write-docs', testsEdit >= 0 ? Math.max(docsEdit, testsAt) : docsEdit);
    if (docsAt < 0) missing.push('write-docs');
  }
  return missing;
}

function buildReason(missing) {
  const steps = missing.map(s => `\`${s}\``).join(' and then ');
  return `CLAUDE.md requires skills you have not invoked since editing code this turn. `
    + `Invoke ${steps} via the Skill tool now, `
    + `then report the task as done. Do not hand-write the tests or docs instead.`;
}

function decide(transcriptLines, stopHookActive, projectDir = PROJECT_DIR) {
  const missing = missingSkills(collectTurnEvents(transcriptLines, projectDir));
  if (missing.length === 0) return { missing, output: null };
  if (stopHookActive) {
    // Already blocked once; don't loop. Warn and let the turn finish.
    return { missing, output: { systemMessage: `enforce-skills: still not invoked: ${missing.join(', ')}` } };
  }
  return { missing, output: { decision: 'block', reason: buildReason(missing) } };
}

function main() {
  let raw = '';
  process.stdin.on('data', chunk => (raw += chunk));
  process.stdin.on('end', () => {
    let input = {};
    try { input = JSON.parse(raw); } catch { /* manual test run */ }

    const transcriptPath = input.transcript_path;
    const lines = transcriptPath && fs.existsSync(transcriptPath)
      ? fs.readFileSync(transcriptPath, 'utf8').split('\n')
      : [];
    const { missing, output } = decide(lines, input.stop_hook_active === true);

    fs.mkdirSync(path.dirname(LOG_FILE), { recursive: true });
    const result = output ? (output.decision === 'block' ? 'BLOCK' : 'WARN') : 'OK';
    fs.appendFileSync(LOG_FILE,
      `[${new Date().toISOString()}] session=${input.session_id ?? 'manual'} result=${result} missing=${missing.join(',') || '-'}\n`,
      'utf8');

    if (output) console.log(JSON.stringify(output));
  });
}

if (require.main === module) main();

module.exports = { toRelative, needsTests, needsDocs, collectTurnEvents, missingSkills, decide };
