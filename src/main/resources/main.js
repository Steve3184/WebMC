// WebMC Bootstrap — minimal entry point
// Loads VFS, sets up socket redirect, then starts the game.
// No UI — purely functional. See index.html.

(async function () {
  'use strict';

  // ── VFS init ────────────────────────────────────────────────
  if (typeof VFS === 'undefined') {
    console.error('[WebMC] VFS class not found — vfs.js may not have loaded');
    return;
  }

  const vfs = new VFS();
  await vfs.init('vfs.tar.xz');
  window._vfs = vfs;

  // ── Socket redirect ─────────────────────────────────────────
  if (typeof SocketRedirect !== 'undefined') {
    const redirect = new SocketRedirect();
    redirect.init({ wsUrl: 'ws://' + location.host + '/ws' });
    window._socketRedirect = redirect;
  }

  // ── Load game engine (split-js) ─────────────────────────────
  const manifest = await fetch('manifest.json').then(r => r.json());

  // Load runtime.js first (entry point + TeaVM bootstrap)
  await new Promise((res, rej) => {
    const s = document.createElement('script');
    s.src = 'runtime.js';
    s.onload = res;
    s.onerror = () => rej(new Error('Failed to load runtime.js'));
    document.head.appendChild(s);
  });

  // Load all class files in parallel
  const classEntries = Object.entries(manifest.classes || {});
  const classPromises = classEntries.map(([name, path]) =>
    new Promise((res, rej) => {
      const s = document.createElement('script');
      s.src = path;
      s.onload = res;
      s.onerror = () => rej(new Error(`Failed to load ${path} (${name})`));
      document.head.appendChild(s);
    })
  );
  await Promise.all(classPromises);

  // ── Start game ──────────────────────────────────────────────
  if (typeof window.webmcStart === 'function') {
    window.webmcStart();
  } else if (typeof window.main === 'function') {
    window.main();
  } else if (typeof window.teavm_main === 'function') {
    window.teavm_main();
  } else {
    console.warn('[WebMC] No entry point found');
  }
})();
