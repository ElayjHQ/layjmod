const express = require('express');
const fs = require('fs');
const path = require('path');

const PORT = 8080;

function loadServers() {
  const file = path.join(__dirname, 'servers.json');
  const example = path.join(__dirname, 'servers.example.json');
  if (!fs.existsSync(file)) {
    fs.copyFileSync(example, file);
    console.log('servers.json created from template - edit tokens/urls before use');
  }
  const conf = JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
  return conf.targets.filter(t => t && t.name);
}

const targets = loadServers();

const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '8kb' }));
app.use(express.static(path.join(__dirname, 'public')));

const findTarget = name => targets.find(t => t.name === name);

function describeError(err) {
  if (err && err.name === 'AbortError') return 'timed out waiting for the server';
  const code = err && err.cause && err.cause.code;
  if (code === 'ECONNREFUSED') return 'server offline (connection refused)';
  if (code === 'EHOSTUNREACH' || code === 'ENETUNREACH') return 'unreachable network';
  if (code === 'ETIMEDOUT') return 'connection timed out';
  return err && err.message ? 'proxy error: ' + err.message : 'proxy error';
}

async function callTarget(target, endpoint, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), options.timeoutMs || 20000);
  try {
    const res = await fetch(target.url + endpoint, {
      method: options.method || 'GET',
      headers: Object.assign({
        Authorization: 'Bearer ' + target.token,
      }, options.headers || {}),
      body: options.body,
      signal: controller.signal,
    });
    const data = await res.json().catch(() => ({ ok: false, error: 'invalid response from mod API' }));
    return { status: res.status, data };
  } finally {
    clearTimeout(timer);
  }
}

app.get('/api/targets', (req, res) => {
  res.json({ targets: targets.map(t => ({ name: t.name, configured: Boolean(t.url) })) });
});

app.get('/api/status/:target', async (req, res) => {
  const t = findTarget(req.params.target);
  if (!t) return res.status(404).json({ ok: false, error: 'unknown target' });
  if (!t.url) return res.status(400).json({ ok: false, error: 'target not configured yet' });
  try {
    const { status, data } = await callTarget(t, '/api/status', { timeoutMs: 8000 });
    if (status === 401) {
      return res.status(status).json({ ok: false, error: 'token rejected - update servers.json' });
    }
    if (!data.ok) {
      return res.status(status === 200 ? 502 : status).json(data);
    }
    return res.status(200).json(data);
  } catch (err) {
    return res.status(503).json({ ok: false, error: describeError(err) });
  }
});

app.post('/api/command/:target', async (req, res) => {
  const t = findTarget(req.params.target);
  if (!t) return res.status(404).json({ ok: false, error: 'unknown target' });
  if (!t.url) return res.status(400).json({ ok: false, error: 'target not configured yet' });
  const command = typeof req.body.command === 'string' ? req.body.command : '';
  if (!command.trim()) return res.status(400).json({ ok: false, error: 'empty command' });
  try {
    const { status, data } = await callTarget(t, '/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command }),
      timeoutMs: 25000,
    });
    if (status === 401) {
      return res.status(status).json({ ok: false, error: 'token rejected - update servers.json' });
    }
    return res.status(data.ok ? 200 : status === 200 ? 502 : status).json(data);
  } catch (err) {
    return res.status(503).json({ ok: false, error: describeError(err) });
  }
});

app.listen(PORT, '127.0.0.1', () => {
  console.log('LayjMod website on http://localhost:' + PORT);
  console.log('targets: ' + targets.map(t => t.name + (t.url ? ' (configured)' : ' (unconfigured)')).join(', '));
});
