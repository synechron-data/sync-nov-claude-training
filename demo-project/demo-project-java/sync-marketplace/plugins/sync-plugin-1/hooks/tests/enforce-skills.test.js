// Run with: node --test .claude/hooks/tests
const test = require('node:test');
const assert = require('node:assert');
const path = require('path');
const { spawnSync } = require('child_process');
const os = require('os');
const fs = require('fs');

const hook = require('../enforce-skills.js');

const ROOT = path.resolve(__dirname, '..', '..', '..');
const abs = rel => path.join(ROOT, ...rel.split('/'));

const prompt = text => JSON.stringify({ type: 'user', message: { content: text } });
const toolResult = JSON.stringify({ type: 'user', message: { content: [{ type: 'tool_result', content: 'ok' }] } });
const assistant = (...blocks) => JSON.stringify({ type: 'assistant', message: { content: blocks } });
const edit = (rel, name = 'Edit') => assistant({ type: 'tool_use', name, input: { file_path: abs(rel) } });
const skill = name => assistant({ type: 'tool_use', name: 'Skill', input: { skill: name } });

const CONTROLLER = 'backend/src/main/java/com/technizer/taskapi/task/TaskController.java';
const SERVICE = 'backend/src/main/java/com/technizer/taskapi/task/TaskService.java';

const decide = (lines, active = false) => hook.decide(lines, active, ROOT);

test('needsTests: backend main code and frontend app code, not specs or styles', () => {
  assert.ok(hook.needsTests(SERVICE));
  assert.ok(hook.needsTests('frontend/src/app/tasks/task-list.component.ts'));
  assert.ok(hook.needsTests('frontend/src/app/app.component.html'));
  assert.ok(!hook.needsTests('frontend/src/app/tasks/task-list.component.spec.ts'));
  assert.ok(!hook.needsTests('frontend/src/app/app.component.css'));
  assert.ok(!hook.needsTests('backend/src/test/java/com/technizer/taskapi/task/TaskServiceTest.java'));
  assert.ok(!hook.needsTests('docs/api.md'));
});

test('needsDocs: controllers, entities, auth/config/exception, routes and services', () => {
  assert.ok(hook.needsDocs(CONTROLLER));
  assert.ok(hook.needsDocs('backend/src/main/java/com/technizer/taskapi/task/Task.java'));
  assert.ok(hook.needsDocs('backend/src/main/java/com/technizer/taskapi/auth/JwtUtil.java'));
  assert.ok(hook.needsDocs('backend/src/main/java/com/technizer/taskapi/config/SecurityConfig.java'));
  assert.ok(hook.needsDocs('backend/src/main/java/com/technizer/taskapi/exception/GlobalExceptionHandler.java'));
  assert.ok(hook.needsDocs('frontend/src/app/app.routes.ts'));
  assert.ok(hook.needsDocs('frontend/src/app/tasks/task.service.ts'));
  assert.ok(!hook.needsDocs(SERVICE));
  assert.ok(!hook.needsDocs('backend/src/main/java/com/technizer/taskapi/task/TaskRepository.java'));
  assert.ok(!hook.needsDocs('frontend/src/app/tasks/task.service.spec.ts'));
  assert.ok(!hook.needsDocs('frontend/src/app/tasks/task-list.component.ts'));
});

test('toRelative: normalises separators and rejects paths outside the project', () => {
  assert.strictEqual(hook.toRelative(abs(CONTROLLER), ROOT), CONTROLLER);
  assert.strictEqual(hook.toRelative(path.join(os.tmpdir(), 'x.java'), ROOT), null);
});

test('no edits: nothing is required', () => {
  const { missing, output } = decide([prompt('explain this'), assistant({ type: 'text', text: 'hi' })]);
  assert.deepStrictEqual(missing, []);
  assert.strictEqual(output, null);
});

test('service edit without write-tests: blocks and asks only for write-tests', () => {
  const { missing, output } = decide([prompt('add method'), edit(SERVICE)]);
  assert.deepStrictEqual(missing, ['write-tests']);
  assert.strictEqual(output.decision, 'block');
  assert.match(output.reason, /write-tests/);
  assert.doesNotMatch(output.reason, /write-docs/);
});

test('controller edit without skills: blocks and asks for both, tests first', () => {
  const { missing, output } = decide([prompt('add endpoint'), edit(CONTROLLER)]);
  assert.deepStrictEqual(missing, ['write-tests', 'write-docs']);
  assert.ok(output.reason.indexOf('write-tests') < output.reason.indexOf('write-docs'));
});

test('both skills invoked in order after the edit: allowed', () => {
  const lines = [prompt('add endpoint'), edit(CONTROLLER), toolResult, skill('write-tests'), skill('write-docs')];
  assert.strictEqual(decide(lines).output, null);
});

test('write-docs before write-tests does not satisfy the ordering rule', () => {
  const lines = [prompt('x'), edit(CONTROLLER), skill('write-docs'), skill('write-tests')];
  assert.deepStrictEqual(decide(lines).missing, ['write-docs']);
});

test('skill invoked before the edit does not count', () => {
  const lines = [prompt('x'), skill('write-tests'), edit(SERVICE)];
  assert.deepStrictEqual(decide(lines).missing, ['write-tests']);
});

test('edits to tests, docs, README and hooks are ignored', () => {
  const lines = [
    prompt('x'),
    edit('backend/src/test/java/com/technizer/taskapi/task/TaskServiceTest.java'),
    edit('frontend/src/app/tasks/task.service.spec.ts', 'Write'),
    edit('docs/api.md'),
    edit('README.md'),
    edit('.claude/hooks/enforce-skills.js'),
  ];
  assert.strictEqual(decide(lines).output, null);
});

test('only the current turn counts: earlier-turn edits are forgotten', () => {
  const lines = [prompt('first'), edit(SERVICE), prompt('second'), assistant({ type: 'text', text: 'ok' })];
  assert.strictEqual(decide(lines).output, null);
});

test('plugin-namespaced skill names are matched by bare name', () => {
  const lines = [prompt('x'), edit(SERVICE), skill('someplugin:write-tests')];
  assert.strictEqual(decide(lines).output, null);
});

test('stop_hook_active: warns instead of blocking, so it cannot loop', () => {
  const { output } = decide([prompt('x'), edit(CONTROLLER)], true);
  assert.strictEqual(output.decision, undefined);
  assert.match(output.systemMessage, /write-tests, write-docs/);
});

test('malformed transcript lines are skipped', () => {
  const lines = ['not json', '', prompt('x'), edit(SERVICE)];
  assert.deepStrictEqual(decide(lines).missing, ['write-tests']);
});

test('CLI: reads the transcript from stdin JSON and prints a block decision', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'enforce-skills-'));
  const transcript = path.join(dir, 't.jsonl');
  fs.writeFileSync(transcript, [prompt('x'), edit(SERVICE)].join('\n'));
  const res = spawnSync('node', [path.join(__dirname, '..', 'enforce-skills.js')], {
    input: JSON.stringify({ transcript_path: transcript, session_id: 'test', stop_hook_active: false }),
    encoding: 'utf8',
    env: { ...process.env, CLAUDE_PROJECT_DIR: ROOT },
  });
  assert.strictEqual(res.status, 0);
  assert.strictEqual(JSON.parse(res.stdout).decision, 'block');
  fs.rmSync(dir, { recursive: true, force: true });
});
