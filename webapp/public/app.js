const SELECTED_TARGET_KEY = 'layjmod-target';
let history = [];
let historyIndex = -1;

const $ = id => document.getElementById(id);

const log = $('log');

function esc(s) {
  const div = document.createElement('div');
  div.textContent = s;
  return div.innerHTML;
}

function colorize(line) {
  let cls = '';
  if (/^\[?Server\]?|broadcast/i.test(line) || line.includes('[Server]')) cls = 'broadcast';
  if (/^error|unknown|incomplete|invalid|failure|^(\S.*Error)/i.test(line)) cls = 'err';
  if (/^[+-]{0,2}(Time|Weather|Saved|There are)/i.test(line)) cls = 'info';
  return cls;
}

function appendLine(text, cls) {
  const t = new Date().toLocaleTimeString([], { hour12: false });
  const row = document.createElement('div');
  row.className = 'line ' + (cls || '');
  row.innerHTML = '<span class="time">' + esc(t) + '</span> ' + esc(text);
  log.appendChild(row);
  log.scrollTop = log.scrollHeight;
  while (log.children.length > 500) log.removeChild(log.firstChild);
}

function setDot(state, text) {
  const dot = $('dot');
  dot.className = 'dot ' + (state === 'on' ? 'on' : state === 'warn' ? 'warn' : 'off');
  $('statusText').textContent = text;
}

async function targets() {
  const res = await fetch('/api/targets');
  const data = await res.json();
  const sel = $('target');
  sel.innerHTML = '';
  data.targets.forEach(t => {
    const o = document.createElement('option');
    o.value = t.name;
    o.textContent = t.name + (t.configured ? '' : ' (not configured)');
    o.disabled = !t.configured;
    sel.appendChild(o);
  });
  const saved = localStorage.getItem(SELECTED_TARGET_KEY);
  if (saved && [...sel.options].some(o => !o.disabled && o.value === saved)) {
    sel.value = saved;
  }
  return sel.value;
}

async function checkStatus(announceFailures) {
  const target = $('target').value;
  if (!target) return;
  try {
    const res = await fetch('/api/status/' + target);
    const data = await res.json();
    if (data.ok) {
      setDot('on', target + '  |  ' + data.version + '  |  ' + data.online + '/' + data.max +
        ' online  |  TPS ' + data.tps);
    } else {
      setDot('warn', data.error || 'server responded but with an error');
      if (announceFailures) appendLine('WARNING: ' + (data.error || 'status error'), 'err');
    }
  } catch (err) {
    setDot('warn', 'website backend unreachable');
  }
}

async function send() {
  const target = $('target').value;
  const cmd = $('cmd').value.trim().replace(/^\//, '');
  if (!cmd) return;
  $('cmd').value = '';
  if (history[history.length - 1] !== cmd) {
    history.push(cmd);
    if (history.length > 100) history.shift();
  }
  historyIndex = -1;
  appendLine('> ' + cmd, 'echo');
  try {
    const res = await fetch('/api/command/' + target, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: cmd }),
    });
    const data = await res.json();
    if (!data.ok) {
      appendLine((data.error || 'command failed'), 'err');
      return;
    }
    const out = data.output || '';
    if (!out) {
      const isBroadcast = /^(say|broadcast)\b/i.test(cmd);
      appendLine(isBroadcast ? '(broadcast sent - no console echo)' : '(no output)', 'muted');
    } else {
      out.split('\n').forEach(line => appendLine(line, colorize(line)));
    }
  } catch (err) {
    appendLine('website backend error: ' + err.message, 'err');
  }
}

function setupShortcuts() {
  $('cmdform').addEventListener('submit', ev => {
    ev.preventDefault();
    send();
  });
  $('cmd').addEventListener('keydown', ev => {
    if (ev.key === 'ArrowUp') {
      if (!history.length) return;
      if (historyIndex === -1) historyIndex = history.length - 1;
      else historyIndex = Math.max(0, historyIndex - 1);
      $('cmd').value = history[historyIndex];
      ev.preventDefault();
    } else if (ev.key === 'ArrowDown') {
      if (historyIndex === -1) return;
      historyIndex += 1;
      if (historyIndex >= history.length) {
        historyIndex = -1;
        $('cmd').value = '';
      } else {
        $('cmd').value = history[historyIndex];
      }
      ev.preventDefault();
    }
  });
}

document.querySelectorAll('.quickbar button').forEach(b => {
  b.addEventListener('click', () => {
    $('cmd').value = b.dataset.cmd;
    send();
  });
});

$('target').addEventListener('change', () => {
  localStorage.setItem(SELECTED_TARGET_KEY, $('target').value);
  appendLine('switched to ' + $('target').value, 'info');
  checkStatus(true);
});

(async () => {
  await targets();
  log.textContent = '';
  appendLine('layjmod console loaded - target: ' + $('target').value, 'info');
  setupShortcuts();
  await checkStatus(true);
  setInterval(() => checkStatus(false), 5000);
})();
