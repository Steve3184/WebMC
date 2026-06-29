const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const { chromium } = require('playwright');

const REPO_ROOT = path.resolve(__dirname, '..');
const WORK_DIR = path.join(REPO_ROOT, 'work');
const ROOT = path.join(WORK_DIR, 'build', 'web-run');
const REPORTS_DIR = path.join(REPO_ROOT, 'docs', 'reports');
const EVIDENCE_DIR = path.join(REPO_ROOT, 'output', 'playwright');
const logPath = path.join(WORK_DIR, 'playwright-webmc-latest.log');
fs.writeFileSync(logPath, 'start ' + new Date().toISOString() + '\n');
const log = (line) => fs.appendFileSync(logPath, line + '\n');

const PORT = Number(process.env.WEBMC_PORT || 58080);
const WAIT_MS = Number(process.env.WAIT_MS || 700000);
const STALL_MS = Number(process.env.STALL_MS || 80000);
const PLAYABLE_TICK_MIN = Number(process.env.PLAYABLE_TICK_MIN || 3);
const PLAYABLE_GRACE_MS = Number(process.env.PLAYABLE_GRACE_MS || 90000);
const SMOKE_MAX_ATTEMPTS = Math.max(1, Number(process.env.SMOKE_MAX_ATTEMPTS || 2));
const SMOKE_RETRY_WAIT_MS = Math.max(WAIT_MS, Number(process.env.SMOKE_RETRY_WAIT_MS || 900000));
const SMOKE_RETRY_DELAY_MS = Math.max(0, Number(process.env.SMOKE_RETRY_DELAY_MS || 2000));
const SOAK_MAX_ATTEMPTS = Math.max(1, Number(process.env.SOAK_MAX_ATTEMPTS || 2));
const SOAK_RETRY_WAIT_MS = Math.max(WAIT_MS, Number(process.env.SOAK_RETRY_WAIT_MS || 900000));
const SAVE_RELOAD_MAX_ATTEMPTS = Math.max(1, Number(process.env.SAVE_RELOAD_MAX_ATTEMPTS || 2));
const SAVE_RELOAD_RETRY_WAIT_MS = Math.max(WAIT_MS, Number(process.env.SAVE_RELOAD_RETRY_WAIT_MS || 900000));
const REQUIRE_WORLDLOAD_SUCCESS = String(process.env.REQUIRE_WORLDLOAD_SUCCESS || 'true').toLowerCase() === 'true';
const WEBMC_DIAGNOSTICS = String(process.env.WEBMC_DIAGNOSTICS || '1').toLowerCase() !== '0';
const SCENARIO = String(process.env.SCENARIO || 'smoke').trim().toLowerCase();
const REPEAT_COUNT = Math.max(1, Number(process.env.REPEAT_COUNT || 3));
const SOAK_MS = Math.max(0, Number(process.env.SOAK_MS || 600000));
const WORLD_NAME = String(process.env.WEBMC_WORLD_NAME || 'Web World').trim() || 'Web World';
const BASE_URL = 'http://localhost:' + PORT + '/';

const FRONTIER_MARKERS = [
  '[mc-web] WebMain.main start',
  '[mc-web] WebGL2 init:',
  '[mc-web] MC main call begin',
  '[mc-web] MC main call end',
  '[mc-web] MC main skipped in webSafeBoot mode',
  '[mc-web] MC main returned normally',
  '[mc-web] MC main threw:',
  '[mc-probe] Main.main:',
  '[mc-main-stage]',
  '[mc-probe] init game caught:',
  '[mc-web/vfs] WebFs.preload: fetch failed',
  '[mc-probe] Minecraft.run: web persistent tick ',
  '[mc-web/createfresh] begin levelId=',
  '[mc-web/createfresh] access ok levelId=',
  '[mc-web/createfresh] deleting existing experimental levelId=',
  '[mc-web/createfresh] deleted existing experimental levelId=',
  '[mc-web/createfresh] prepare WorldLoader packConfig begin',
  '[mc-web/createfresh] loadWorldDataAsync schedule',
  '[mc-web/createfresh] loadWorldDataBlocking begin',
  '[mc-web/createfresh] WorldLoader.load begin',
  '[mc-web/createfresh] WorldLoader.load join',
  '[mc-web/createfresh] WorldLoader.load async begin',
  '[mc-web/createfresh] loadWorldDataAsync end',
  '[mc-web/createfresh] loadWorldDataBlocking end',
  '[mc-web/createfresh] doWorldLoad begin',
  '[mc-web/createfresh] doWorldLoad returned',
  '[mc-web/createfresh] failed levelId=',
  '[mc-web/doworld] begin',
  '[mc-web/doworld] MinecraftServer.spin begin',
  '[mc-web/doworld] MinecraftServer.spin end',
  '[mc-web/doworld] wait progressListener begin',
  '[mc-web/doworld] wait progressListener elapsedMs=',
  '[mc-web/doworld] wait progressListener end',
  '[mc-web/doworld] wait server ready begin',
  '[mc-web/doworld] wait server ready elapsedMs=',
  '[mc-web/doworld] wait server ready end',
  '[mc-web/loopback]',
  '[mc-web/server]',
  '[mc-web/servergame]',
  '[mc-web/configpacks]',
  '[mc-web/chunkgen]',
  '[mc-web/clientlogin]',
  '[mc-web/clienttick]',
  '[mc-web/clientpkt]',
  '[mc-web/packetutils]',
  '[mc-web/level]',
  '[mc-web/gameload]',
  '[mc-web/renderGate]',
  '[mc-web/renderGate/state]',
  '[mc-web/render]',
  '[mc-web/renderLevel]',
  '[mc-web/setupRender]',
  '[mc-web/compileSections]',
  '[mc-web/render/state]',
  '[mc-web/renderGraph/v2]',
  '[mc-web/mainpass]',
  '[mc-web/skypass]',
  '[mc-web/entityRenderers]',
  '[mc-web/collectVisibleEntities]',
  '[mc-web/ctor]',
  '[mc-web/worldloader] stage=',
  '[mc-web/worldloader] failed early:',
  '[mc-web/worldload] experimental start requested:',
  '[mc-web/worldload] probe success:',
  '[mc-web/worldload] probe failed:',
  '[mc-web/worldload] probe blocked:',
  '[mc-probe] ctor-watchdog:',
  '[mc-probe] BlockableEventLoop.<init>:',
  '[mc-web/pause] init showPauseMenu=',
  '[mc-web/pause] disconnectButton',
  '[mc-web/pause] disconnect.click',
  '[mc-web/pause] disconnect.hotkey',
  '[mc-web/pause] disconnect.haltIntegratedServer',
  '[mc-web/pause] disconnect.web-automation',
  '[mc-web] setScreen:'
];

const SCREEN_MARKERS = {
  pause: [
    '[mc-web] setScreen: net.minecraft.client.gui.screens.PauseScreen',
    'screen=PauseScreen'
  ],
  title: [
    '[mc-web] setScreen: net.minecraft.client.gui.screens.TitleScreen',
    'screen=TitleScreen'
  ],
  selectWorld: [
    '[mc-web] setScreen: net.minecraft.client.gui.screens.worldselection.SelectWorldScreen',
    'screen=SelectWorldScreen'
  ],
  placeholder: [
    '[mc-web] setScreen: net.minecraft.client.gui.screens.WebEnterWorldPlaceholderScreen',
    'screen=WebEnterWorldPlaceholderScreen'
  ],
  experimental: [
    '[mc-web] setScreen: net.minecraft.client.gui.screens.WebExperimentalWorldLoadScreen',
    'screen=WebExperimentalWorldLoadScreen'
  ]
};

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForHealthyServer(url, timeoutMs) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    try {
      const res = await fetch(url, { method: 'GET' });
      const body = await res.text();
      if (res.ok && body.includes('bootstrap.js')) {
        return true;
      }
      log('[server-check] unexpected response status=' + res.status);
    } catch (err) {
      log('[server-check] pending ' + String(err));
    }
    await delay(250);
  }
  return false;
}

async function isHealthyServer(url) {
  try {
    const res = await fetch(url, { method: 'GET' });
    const body = await res.text();
    return res.ok && body.includes('bootstrap.js');
  } catch (_) {
    return false;
  }
}

function nowMs() {
  return Date.now();
}

function ensureDirSync(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function isoFileStamp(date = new Date()) {
  return date.toISOString().replace(/[:.]/g, '-');
}

function pickFrontierLine(text) {
  return FRONTIER_MARKERS.some((marker) => text.startsWith(marker));
}

function summarizeFrontier(lines) {
  const frontier = lines.filter((line) => pickFrontierLine(line));
  const hasFrontier = (needle) => frontier.some((line) => line.includes(needle));
  const last = frontier.slice(-15);
  const counts = FRONTIER_MARKERS.map((marker) => {
    const count = frontier.filter((line) => line.includes(marker)).length;
    return marker + '=' + count;
  });
  const maxPersistentTick = lines.reduce((max, line) => {
    const m = line.match(/\[mc-probe\] Minecraft\.run: web persistent tick (\d+)/);
    return m ? Math.max(max, Number(m[1])) : max;
  }, -1);
  const stageFlags = {
    sawMcMainCallBegin: hasFrontier('[mc-web] MC main call begin'),
    sawMcMainCallEnd: hasFrontier('[mc-web] MC main call end'),
    sawMainMainProbe: hasFrontier('[mc-probe] Main.main:'),
    sawCreateFreshBegin: hasFrontier('[mc-web/createfresh] begin levelId='),
    sawAsyncLoadScheduled: hasFrontier('[mc-web/createfresh] loadWorldDataAsync schedule'),
    sawWorldLoaderAsyncBegin: hasFrontier('[mc-web/createfresh] WorldLoader.load async begin'),
    sawWorldLoaderResultFactory: hasFrontier('[mc-web/worldloader] stage=resultFactory'),
    sawAsyncLoadEnd: hasFrontier('[mc-web/createfresh] loadWorldDataAsync end'),
    enteredDoWorldLoad: hasFrontier('[mc-web/createfresh] doWorldLoad begin'),
    enteredWaitServerReadyBegin: hasFrontier('[mc-web/doworld] wait server ready begin'),
    enteredWaitServerReadyEnd: hasFrontier('[mc-web/doworld] wait server ready end'),
    sawLoopback: hasFrontier('[mc-web/loopback]'),
    sawWorldloaderStage: hasFrontier('[mc-web/worldloader] stage='),
    sawHandleLoginAfterResetPos: hasFrontier('[mc-web/clientlogin] handleLogin.afterResetPos'),
    sawHandleLoginBeforeReadPlayerId: hasFrontier('[mc-web/clientlogin] handleLogin.beforeReadPlayerId'),
    sawHandleLoginAfterReadPlayerId: hasFrontier('[mc-web/clientlogin] handleLogin.afterReadPlayerId'),
    sawHandleLoginBeforeSetId: hasFrontier('[mc-web/clientlogin] handleLogin.beforeSetId'),
    sawHandleLoginAfterSetId: hasFrontier('[mc-web/clientlogin] handleLogin.afterSetId'),
    sawHandleLoginBeforeAddEntity: hasFrontier('[mc-web/clientlogin] handleLogin.beforeAddEntity'),
    sawClientLevelFastPathLocalPlayer: hasFrontier('[mc-web/clientlogin] clientLevel.addEntity fastPathLocalPlayer'),
    sawHandleLoginAfterAddEntity: hasFrontier('[mc-web/clientlogin] handleLogin.afterAddEntity'),
    sawHandleLoginEnd: hasFrontier('[mc-web/clientlogin] handleLogin.end')
  };
  const returnIndex = lines.findIndex((line) => line.includes('[mc-web] MC main returned normally'));
  const linesAfterReturn = returnIndex >= 0 ? lines.slice(returnIndex + 1) : [];
  const postReturn = {
    sawAnyLogAfterReturn: linesAfterReturn.length > 0,
    sawMainMainProbeAfterReturn: linesAfterReturn.some((line) => line.includes('[mc-probe] Main.main:')),
    sawPersistentTickAfterReturn: linesAfterReturn.some((line) => line.includes('[mc-probe] Minecraft.run: web persistent tick ')),
    sawLoopbackAfterReturn: linesAfterReturn.some((line) => line.includes('[mc-web/loopback]')),
    sawWorldloaderAfterReturn: linesAfterReturn.some((line) => line.includes('[mc-web/worldloader] stage=')),
    sawDoWorldLoadAfterReturn: linesAfterReturn.some((line) => line.includes('[mc-web/createfresh] doWorldLoad begin')),
    sawWaitServerReadyAfterReturn: linesAfterReturn.some((line) => line.includes('[mc-web/doworld] wait server ready')),
    logsAfterReturnCount: linesAfterReturn.length
  };
  return { count: frontier.length, counts, last, maxPersistentTick, stageFlags, postReturn };
}

function countMatchingLines(lines, pattern) {
  if (pattern instanceof RegExp) {
    return lines.filter((line) => pattern.test(line)).length;
  }
  return lines.filter((line) => line.includes(pattern)).length;
}

function getScenarioPlayableGraceMs(mode) {
  if (mode === 'save-reload') {
    return 0;
  }
  return PLAYABLE_GRACE_MS;
}

function pushUniquePosition(positions, seen, x, y, source) {
  const key = `${Math.round(x)}:${Math.round(y)}`;
  if (seen.has(key)) {
    return;
  }
  seen.add(key);
  positions.push({ x: Math.round(x), y: Math.round(y), source });
}

function addRectGridPositions(positions, seen, rect, sourcePrefix) {
  if (!rect || rect.width <= 0 || rect.height <= 0) {
    return;
  }
  const xs = [rect.left + 12, rect.centerX, rect.left + rect.width - 12];
  const ys = [rect.top + 8, rect.centerY, rect.top + rect.height - 8];
  for (let xi = 0; xi < xs.length; xi++) {
    for (let yi = 0; yi < ys.length; yi++) {
      pushUniquePosition(positions, seen, xs[xi], ys[yi], `${sourcePrefix}-${xi}-${yi}`);
    }
  }
}

function hasAnyLine(lines, patterns) {
  return lines.some((line) => patterns.some((pattern) => line.includes(pattern)));
}

function getLatestPauseDisconnectTarget(lines) {
  const re = /\[mc-web\/pause\] disconnectButton x=(\d+) y=(\d+) w=(\d+) h=(\d+) screenW=(\d+) screenH=(\d+)/;
  for (let i = lines.length - 1; i >= 0; i--) {
    const m = lines[i].match(re);
    if (m) {
      const x = Number(m[1]);
      const y = Number(m[2]);
      const w = Number(m[3]);
      const h = Number(m[4]);
      if (Number.isFinite(x) && Number.isFinite(y) && Number.isFinite(w) && Number.isFinite(h) && w > 0 && h > 0) {
        return {
          left: x,
          top: y,
          width: w,
          height: h,
          centerX: Math.round(x + w / 2),
          centerY: Math.round(y + h / 2)
        };
      }
    }
  }
  return null;
}

function getLatestPauseInitState(lines) {
  const re = /\[mc-web\/pause\] init showPauseMenu=(true|false)/;
  for (let i = lines.length - 1; i >= 0; i--) {
    const m = lines[i].match(re);
    if (m) {
      return m[1] === 'true';
    }
  }
  return null;
}

function createProgressAccumulator() {
  return {
    phaseTime: {
      worldloadRequestedAt: null,
      worldloadSuccessAt: null,
      serverReadyBeginAt: null,
      serverReadyEndAt: null,
      loopbackAt: null,
      playableLoopAt: null,
      serverConfigBeforeNewPlayerAt: null,
      serverConfigBeforePlaceCallAt: null,
      placeNewPlayerEnterAt: null,
      placeNewPlayerAfterLoginPacketAt: null,
      serverConfigAfterPlaceNewPlayerAt: null,
      firstWorldloadSuccessAt: null,
      secondWorldloadSuccessAt: null,
      secondWorldloadRequestedAt: null
    },
    maxPersistentTick: -1,
    counts: {
      worldloadRequestedCount: 0,
      worldloadSuccessCount: 0,
      titleScreenCount: 0,
      pauseScreenCount: 0,
      selectWorldCount: 0,
      placeholderScreenCount: 0,
      experimentalScreenCount: 0,
      waitingForServerScreenCount: 0,
      receivingLevelScreenCount: 0,
      doWorldBeginCount: 0,
      setupRenderCount: 0,
      compileSectionsCount: 0,
      renderStateCount: 0,
      renderLevelCount: 0,
      statePlayableCount: 0
    },
    state: {
      lastSignature: '',
      maxRenderedSections: 0,
      maxPresentCount: 0
    }
  };
}

function finiteNumber(value, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function requiredRenderedSectionsForState(webState) {
  const visibleSections = finiteNumber(webState && webState.visibleSections);
  if (visibleSections <= 0) {
    return 0;
  }
  const coverageTarget = Math.ceil(visibleSections * 0.75);
  return Math.min(visibleSections, Math.max(Math.min(8, visibleSections), coverageTarget));
}

function isTerrainReadyState(webState) {
  if (!webState || typeof webState !== 'object') {
    return false;
  }
  if (webState.webTerrainReady) {
    return true;
  }
  const renderedSections = finiteNumber(webState.renderedSections);
  const requiredRenderedSections = finiteNumber(webState.requiredRenderedSections) || requiredRenderedSectionsForState(webState);
  return !!(
    requiredRenderedSections > 0 &&
    renderedSections >= requiredRenderedSections &&
    webState.hasRenderedAllSections
  );
}

function applyWebStateProgress(acc, webState, relMs) {
  if (!webState || typeof webState !== 'object') {
    return false;
  }

  let changed = false;
  const screen = typeof webState.screen === 'string' ? webState.screen : '';
  const renderedSections = finiteNumber(webState.renderedSections);
  const visibleSections = finiteNumber(webState.visibleSections);
  const requiredRenderedSections = finiteNumber(webState.requiredRenderedSections) || requiredRenderedSectionsForState(webState);
  const terrainReady = isTerrainReadyState(webState);
  const presentCount = finiteNumber(webState.presentCount);
  const levelRenderUpdates = finiteNumber(webState.levelRenderUpdates);
  const levelPresent = !!webState.levelPresent;
  const playerPresent = !!webState.playerPresent;
  const signature = [
    screen,
    levelPresent ? 'L1' : 'L0',
    playerPresent ? 'P1' : 'P0',
    renderedSections,
    visibleSections,
    requiredRenderedSections,
    terrainReady ? 'T1' : 'T0',
    presentCount,
    levelRenderUpdates
  ].join('|');

  if (signature !== acc.state.lastSignature) {
    acc.state.lastSignature = signature;
    changed = true;
  }
  if (renderedSections > acc.state.maxRenderedSections) {
    acc.state.maxRenderedSections = renderedSections;
    changed = true;
  }
  if (presentCount > acc.state.maxPresentCount) {
    acc.state.maxPresentCount = presentCount;
    changed = true;
  }

  if (screen === 'TitleScreen' && acc.counts.titleScreenCount === 0) {
    acc.counts.titleScreenCount = 1;
    changed = true;
  } else if (screen === 'PauseScreen' && acc.counts.pauseScreenCount === 0) {
    acc.counts.pauseScreenCount = 1;
    changed = true;
  } else if (screen === 'SelectWorldScreen' && acc.counts.selectWorldCount === 0) {
    acc.counts.selectWorldCount = 1;
    changed = true;
  } else if (screen === 'WebEnterWorldPlaceholderScreen' && acc.counts.placeholderScreenCount === 0) {
    acc.counts.placeholderScreenCount = 1;
    changed = true;
  } else if (screen === 'WebExperimentalWorldLoadScreen' && acc.counts.experimentalScreenCount === 0) {
    acc.counts.experimentalScreenCount = 1;
    changed = true;
  } else if (screen === 'WebWaitingForServerScreen' && acc.counts.waitingForServerScreenCount === 0) {
    acc.counts.waitingForServerScreenCount = 1;
    changed = true;
  } else if (screen === 'ReceivingLevelScreen' && acc.counts.receivingLevelScreenCount === 0) {
    acc.counts.receivingLevelScreenCount = 1;
    changed = true;
  }

  if (levelPresent && playerPresent) {
    if (acc.phaseTime.worldloadRequestedAt === null) {
      acc.phaseTime.worldloadRequestedAt = relMs;
      acc.counts.worldloadRequestedCount = Math.max(acc.counts.worldloadRequestedCount, 1);
      changed = true;
    }
    if (acc.phaseTime.serverReadyBeginAt === null) {
      acc.phaseTime.serverReadyBeginAt = relMs;
      changed = true;
    }
    if (acc.phaseTime.serverReadyEndAt === null) {
      acc.phaseTime.serverReadyEndAt = relMs;
      changed = true;
    }
    if (acc.phaseTime.loopbackAt === null) {
      acc.phaseTime.loopbackAt = relMs;
      changed = true;
    }
    if (acc.phaseTime.serverConfigAfterPlaceNewPlayerAt === null) {
      acc.phaseTime.serverConfigAfterPlaceNewPlayerAt = relMs;
      changed = true;
    }
  }

  if (visibleSections > 0) {
    acc.counts.setupRenderCount = Math.max(acc.counts.setupRenderCount, 1);
  }
  if (renderedSections > 0) {
    acc.counts.compileSectionsCount = Math.max(acc.counts.compileSectionsCount, 1);
    acc.counts.renderLevelCount = Math.max(acc.counts.renderLevelCount, 1);
  }

  if (terrainReady) {
    acc.counts.statePlayableCount = Math.max(acc.counts.statePlayableCount, 1);
    acc.counts.worldloadSuccessCount = Math.max(acc.counts.worldloadSuccessCount, 1);
    if (acc.phaseTime.worldloadSuccessAt === null) {
      acc.phaseTime.worldloadSuccessAt = relMs;
      changed = true;
    }
    if (acc.phaseTime.firstWorldloadSuccessAt === null) {
      acc.phaseTime.firstWorldloadSuccessAt = relMs;
      changed = true;
    }
    if (acc.phaseTime.playableLoopAt === null) {
      acc.phaseTime.playableLoopAt = relMs;
      changed = true;
    }
    acc.maxPersistentTick = Math.max(acc.maxPersistentTick, PLAYABLE_TICK_MIN);
  }

  return changed;
}

function applyProgressEvent(acc, event, navStartMs) {
  const line = event.line;
  const relMs = event.ts - navStartMs;

  if (line.includes('[mc-web/worldload] experimental start requested:')) {
    acc.counts.worldloadRequestedCount++;
    if (acc.phaseTime.worldloadRequestedAt === null) {
      acc.phaseTime.worldloadRequestedAt = relMs;
    } else if (acc.counts.worldloadRequestedCount === 2 && acc.phaseTime.secondWorldloadRequestedAt === null) {
      acc.phaseTime.secondWorldloadRequestedAt = relMs;
    }
  }
  if (line.includes('[mc-web/worldload] probe success:')) {
    acc.counts.worldloadSuccessCount++;
    if (acc.phaseTime.worldloadSuccessAt === null) {
      acc.phaseTime.worldloadSuccessAt = relMs;
    }
    if (acc.phaseTime.firstWorldloadSuccessAt === null) {
      acc.phaseTime.firstWorldloadSuccessAt = relMs;
    } else if (acc.counts.worldloadSuccessCount === 2 && acc.phaseTime.secondWorldloadSuccessAt === null) {
      acc.phaseTime.secondWorldloadSuccessAt = relMs;
    }
  }
  if (acc.phaseTime.serverReadyBeginAt === null && line.includes('[mc-web/doworld] wait server ready begin')) {
    acc.phaseTime.serverReadyBeginAt = relMs;
  }
  if (acc.phaseTime.serverReadyEndAt === null && line.includes('[mc-web/doworld] wait server ready end')) {
    acc.phaseTime.serverReadyEndAt = relMs;
  }
  if (acc.phaseTime.loopbackAt === null && line.includes('[mc-web/loopback]')) {
    acc.phaseTime.loopbackAt = relMs;
  }
  if (acc.phaseTime.serverConfigBeforeNewPlayerAt === null && line.includes('[mc-web/loopback] serverConfig.beforeNewServerPlayer')) {
    acc.phaseTime.serverConfigBeforeNewPlayerAt = relMs;
  }
  if (acc.phaseTime.serverConfigBeforePlaceCallAt === null && line.includes('[mc-web/loopback] serverConfig.beforePlaceCall')) {
    acc.phaseTime.serverConfigBeforePlaceCallAt = relMs;
  }
  if (acc.phaseTime.placeNewPlayerEnterAt === null && line.includes('[mc-web/loopback] placeNewPlayer.enter')) {
    acc.phaseTime.placeNewPlayerEnterAt = relMs;
  }
  if (acc.phaseTime.placeNewPlayerAfterLoginPacketAt === null && line.includes('[mc-web/loopback] placeNewPlayer.afterLoginPacket')) {
    acc.phaseTime.placeNewPlayerAfterLoginPacketAt = relMs;
  }
  if (acc.phaseTime.serverConfigAfterPlaceNewPlayerAt === null && line.includes('[mc-web/loopback] serverConfig.afterPlaceNewPlayer')) {
    acc.phaseTime.serverConfigAfterPlaceNewPlayerAt = relMs;
  }

  if (line.includes('[mc-web] setScreen: net.minecraft.client.gui.screens.TitleScreen')) {
    acc.counts.titleScreenCount++;
  }
  if (line.includes('[mc-web] setScreen: net.minecraft.client.gui.screens.PauseScreen')) {
    acc.counts.pauseScreenCount++;
  }
  if (line.includes('[mc-web] setScreen: net.minecraft.client.gui.screens.worldselection.SelectWorldScreen')) {
    acc.counts.selectWorldCount++;
  }
  if (line.includes('[mc-web] setScreen: net.minecraft.client.gui.screens.WebEnterWorldPlaceholderScreen')) {
    acc.counts.placeholderScreenCount++;
  }
  if (line.includes('[mc-web] setScreen: net.minecraft.client.gui.screens.WebExperimentalWorldLoadScreen')) {
    acc.counts.experimentalScreenCount++;
  }
  if (line.includes('[mc-web] setScreen: net.minecraft.client.gui.screens.WebWaitingForServerScreen')) {
    acc.counts.waitingForServerScreenCount++;
  }
  if (line.includes('[mc-web] setScreen: net.minecraft.client.gui.screens.ReceivingLevelScreen')) {
    acc.counts.receivingLevelScreenCount++;
  }

  if (line.includes('[mc-web/doworld] begin')) {
    acc.counts.doWorldBeginCount++;
  }

  if (line.includes('[mc-web/setupRender]')) {
    acc.counts.setupRenderCount++;
  }
  if (line.includes('[mc-web/compileSections]')) {
    acc.counts.compileSectionsCount++;
  }
  if (line.includes('[mc-web/render/state]')) {
    acc.counts.renderStateCount++;
  }
  if (line.includes('[mc-web/renderLevel]')) {
    acc.counts.renderLevelCount++;
  }

  const tickMatch = line.match(/\[mc-probe\] Minecraft\.run: web persistent tick (\d+)/);
  if (tickMatch) {
    acc.maxPersistentTick = Math.max(acc.maxPersistentTick, Number(tickMatch[1]));
    if (acc.phaseTime.playableLoopAt === null && acc.maxPersistentTick >= PLAYABLE_TICK_MIN) {
      acc.phaseTime.playableLoopAt = relMs;
    }
  }
}

function projectProgressAccumulator(acc) {
  const phaseTime = { ...acc.phaseTime };
  const counts = { ...acc.counts };
  const hasWorldloadRequested = phaseTime.worldloadRequestedAt !== null;
  const hasWorldloadSuccess = phaseTime.worldloadSuccessAt !== null;
  const hasWorldload = REQUIRE_WORLDLOAD_SUCCESS ? hasWorldloadSuccess : (hasWorldloadRequested || hasWorldloadSuccess);
  const hasServerReady = phaseTime.serverReadyBeginAt !== null || phaseTime.serverReadyEndAt !== null;
  const hasLoopback = phaseTime.loopbackAt !== null;
  const hasPlacedPlayer = phaseTime.serverConfigAfterPlaceNewPlayerAt !== null;
  const playable = hasWorldload && hasServerReady && hasLoopback && hasPlacedPlayer && acc.maxPersistentTick >= PLAYABLE_TICK_MIN;
  return {
    phaseTime,
    maxPersistentTick: acc.maxPersistentTick,
    state: { ...acc.state },
    hasWorldload,
    hasWorldloadRequested,
    hasWorldloadSuccess,
    hasServerReady,
    hasLoopback,
    hasPlacedPlayer,
    playable,
    counts
  };
}

function syncProgress(ctx) {
  const events = ctx.consoleEvents;
  for (let i = ctx.progressEventCursor; i < events.length; i++) {
    applyProgressEvent(ctx.progressAccumulator, events[i], ctx.navStart);
  }
  ctx.progressEventCursor = events.length;
  ctx.progressSnapshot = projectProgressAccumulator(ctx.progressAccumulator);
  return ctx.progressSnapshot;
}

function syncFrontierCount(ctx) {
  for (let i = ctx.frontierLineCursor; i < ctx.consoleLines.length; i++) {
    if (pickFrontierLine(ctx.consoleLines[i])) {
      ctx.frontierCount++;
    }
  }
  ctx.frontierLineCursor = ctx.consoleLines.length;
  return ctx.frontierCount;
}

function evaluateProgress(events, navStartMs) {
  const phaseTime = {
    worldloadRequestedAt: null,
    worldloadSuccessAt: null,
    serverReadyBeginAt: null,
    serverReadyEndAt: null,
    loopbackAt: null,
    playableLoopAt: null,
    serverConfigBeforeNewPlayerAt: null,
    serverConfigBeforePlaceCallAt: null,
    placeNewPlayerEnterAt: null,
    placeNewPlayerAfterLoginPacketAt: null,
    serverConfigAfterPlaceNewPlayerAt: null,
    firstWorldloadSuccessAt: null,
    secondWorldloadSuccessAt: null,
    secondWorldloadRequestedAt: null
  };
  let maxPersistentTick = -1;
  let worldloadRequestedCount = 0;
  let worldloadSuccessCount = 0;
  let titleScreenCount = 0;
  let pauseScreenCount = 0;
  let selectWorldCount = 0;
  let placeholderScreenCount = 0;
  let experimentalScreenCount = 0;
  let waitingForServerScreenCount = 0;
  let receivingLevelScreenCount = 0;
  let doWorldBeginCount = 0;

  for (const event of events) {
    const line = event.line;
    const relMs = event.ts - navStartMs;

    if (line.includes('[mc-web/worldload] experimental start requested:')) {
      worldloadRequestedCount++;
      if (phaseTime.worldloadRequestedAt === null) {
        phaseTime.worldloadRequestedAt = relMs;
      } else if (worldloadRequestedCount === 2 && phaseTime.secondWorldloadRequestedAt === null) {
        phaseTime.secondWorldloadRequestedAt = relMs;
      }
    }
    if (line.includes('[mc-web/worldload] probe success:')) {
      worldloadSuccessCount++;
      if (phaseTime.worldloadSuccessAt === null) {
        phaseTime.worldloadSuccessAt = relMs;
      }
      if (phaseTime.firstWorldloadSuccessAt === null) {
        phaseTime.firstWorldloadSuccessAt = relMs;
      } else if (worldloadSuccessCount === 2 && phaseTime.secondWorldloadSuccessAt === null) {
        phaseTime.secondWorldloadSuccessAt = relMs;
      }
    }
    if (phaseTime.serverReadyBeginAt === null && line.includes('[mc-web/doworld] wait server ready begin')) {
      phaseTime.serverReadyBeginAt = relMs;
    }
    if (phaseTime.serverReadyEndAt === null && line.includes('[mc-web/doworld] wait server ready end')) {
      phaseTime.serverReadyEndAt = relMs;
    }
    if (phaseTime.loopbackAt === null && line.includes('[mc-web/loopback]')) {
      phaseTime.loopbackAt = relMs;
    }
    if (phaseTime.serverConfigBeforeNewPlayerAt === null && line.includes('[mc-web/loopback] serverConfig.beforeNewServerPlayer')) {
      phaseTime.serverConfigBeforeNewPlayerAt = relMs;
    }
    if (phaseTime.serverConfigBeforePlaceCallAt === null && line.includes('[mc-web/loopback] serverConfig.beforePlaceCall')) {
      phaseTime.serverConfigBeforePlaceCallAt = relMs;
    }
    if (phaseTime.placeNewPlayerEnterAt === null && line.includes('[mc-web/loopback] placeNewPlayer.enter')) {
      phaseTime.placeNewPlayerEnterAt = relMs;
    }
    if (phaseTime.placeNewPlayerAfterLoginPacketAt === null && line.includes('[mc-web/loopback] placeNewPlayer.afterLoginPacket')) {
      phaseTime.placeNewPlayerAfterLoginPacketAt = relMs;
    }
    if (phaseTime.serverConfigAfterPlaceNewPlayerAt === null && line.includes('[mc-web/loopback] serverConfig.afterPlaceNewPlayer')) {
      phaseTime.serverConfigAfterPlaceNewPlayerAt = relMs;
    }

    if (line.includes('setScreen: net.minecraft.client.gui.screens.TitleScreen') || line.includes('screen=TitleScreen')) {
      titleScreenCount++;
    }
    if (line.includes('setScreen: net.minecraft.client.gui.screens.PauseScreen') || line.includes('screen=PauseScreen')) {
      pauseScreenCount++;
    }
    if (line.includes('setScreen: net.minecraft.client.gui.screens.worldselection.SelectWorldScreen') || line.includes('screen=SelectWorldScreen')) {
      selectWorldCount++;
    }
    if (line.includes('setScreen: net.minecraft.client.gui.screens.WebEnterWorldPlaceholderScreen') || line.includes('screen=WebEnterWorldPlaceholderScreen')) {
      placeholderScreenCount++;
    }
    if (line.includes('setScreen: net.minecraft.client.gui.screens.WebExperimentalWorldLoadScreen') || line.includes('screen=WebExperimentalWorldLoadScreen')) {
      experimentalScreenCount++;
    }
    if (line.includes('setScreen: net.minecraft.client.gui.screens.WebWaitingForServerScreen') || line.includes('screen=WebWaitingForServerScreen')) {
      waitingForServerScreenCount++;
    }
    if (line.includes('setScreen: net.minecraft.client.gui.screens.ReceivingLevelScreen') || line.includes('screen=ReceivingLevelScreen')) {
      receivingLevelScreenCount++;
    }
    if (line.includes('[mc-web/doworld] begin')) {
      doWorldBeginCount++;
    }

    const tickMatch = line.match(/\[mc-probe\] Minecraft\.run: web persistent tick (\d+)/);
    if (tickMatch) {
      maxPersistentTick = Math.max(maxPersistentTick, Number(tickMatch[1]));
      if (phaseTime.playableLoopAt === null && maxPersistentTick >= PLAYABLE_TICK_MIN) {
        phaseTime.playableLoopAt = relMs;
      }
    }
  }

  const hasWorldloadRequested = phaseTime.worldloadRequestedAt !== null;
  const hasWorldloadSuccess = phaseTime.worldloadSuccessAt !== null;
  const hasWorldload = REQUIRE_WORLDLOAD_SUCCESS ? hasWorldloadSuccess : (hasWorldloadRequested || hasWorldloadSuccess);
  const hasServerReady = phaseTime.serverReadyBeginAt !== null || phaseTime.serverReadyEndAt !== null;
  const hasLoopback = phaseTime.loopbackAt !== null;
  const hasPlacedPlayer = phaseTime.serverConfigAfterPlaceNewPlayerAt !== null;
  const playable = hasWorldload && hasServerReady && hasLoopback && hasPlacedPlayer && maxPersistentTick >= PLAYABLE_TICK_MIN;

  return {
    phaseTime,
    maxPersistentTick,
    hasWorldload,
    hasWorldloadRequested,
    hasWorldloadSuccess,
    hasServerReady,
    hasLoopback,
    hasPlacedPlayer,
    playable,
    counts: {
      worldloadRequestedCount,
      worldloadSuccessCount,
      titleScreenCount,
      pauseScreenCount,
      selectWorldCount,
      placeholderScreenCount,
      experimentalScreenCount,
      waitingForServerScreenCount,
      receivingLevelScreenCount,
      doWorldBeginCount,
      setupRenderCount: countMatchingLines(events.map((event) => event.line), '[mc-web/setupRender]'),
      compileSectionsCount: countMatchingLines(events.map((event) => event.line), '[mc-web/compileSections]'),
      renderStateCount: countMatchingLines(events.map((event) => event.line), '[mc-web/render/state]'),
      renderLevelCount: countMatchingLines(events.map((event) => event.line), '[mc-web/renderLevel]')
    }
  };
}

function createScenarioState(mode, contextLabel) {
  return {
    mode,
    contextLabel,
    repeatCount: mode === 'rc' ? REPEAT_COUNT : 1,
    soakMs: SOAK_MS,
    currentStep: mode,
    attempt: 1,
    maxAttempts: 1,
    completed: false,
    fullChainCompleted: false,
    failureCategory: null,
    steps: [],
    smokeAttemptCount: 1,
    smokeMaxAttempts: 1,
    smokePassedAttempt: null,
    smokeAttempts: []
  };
}

function createPhaseExtras() {
  return {
    firstWorldloadSuccessAtMs: null,
    pauseOpenedAtMs: null,
    quitToTitleAtMs: null,
    titleReturnedAtMs: null,
    reloadRequestedAtMs: null,
    reloadWorldloadSuccessAtMs: null,
    soakCompletedAtMs: null
  };
}

function createEvidenceBucket() {
  return {
    screenshot: null,
    stages: {}
  };
}

async function captureScenarioScreenshot(page, stamp, label, updateLatest) {
  ensureDirSync(EVIDENCE_DIR);
  const archivePath = path.join(EVIDENCE_DIR, `mcmain-phase197-${label}-${stamp}.png`);
  let latestPath = null;
  const capture = async (targetPath) => {
    const canvas = page.locator('#canvas, canvas').first();
    try {
      await canvas.waitFor({ state: 'attached', timeout: 5000 });
      await canvas.screenshot({ path: targetPath, timeout: 20000 });
      return 'canvas';
    } catch (canvasErr) {
      log(`[scenario] screenshot canvas fallback failed: ${canvasErr && canvasErr.message ? canvasErr.message : String(canvasErr)}`);
      try {
        await page.screenshot({ path: targetPath, timeout: 60000, animations: 'disabled' });
        return 'page';
      } catch (pageErr) {
        log(`[scenario] screenshot page fallback failed: ${pageErr && pageErr.message ? pageErr.message : String(pageErr)}`);
        try {
          const dataUrl = await page.evaluate(() => {
            const node = document.querySelector('#canvas') || document.querySelector('canvas');
            if (!node || typeof node.toDataURL !== 'function') {
              return null;
            }
            return node.toDataURL('image/png');
          });
          const prefix = 'data:image/png;base64,';
          if (typeof dataUrl === 'string' && dataUrl.startsWith(prefix)) {
            fs.writeFileSync(targetPath, Buffer.from(dataUrl.slice(prefix.length), 'base64'));
            return 'canvas-dataurl';
          }
        } catch (dataErr) {
          log(`[scenario] screenshot dataurl fallback failed: ${dataErr && dataErr.message ? dataErr.message : String(dataErr)}`);
        }
        throw pageErr;
      }
    }
  };
  try {
    if (updateLatest) {
      latestPath = path.join(EVIDENCE_DIR, 'mcmain-phase197-worldready-latest.png');
      const captureMode = await capture(latestPath);
      fs.copyFileSync(latestPath, archivePath);
      log(`[scenario] screenshot captured mode=${captureMode} path=${latestPath}`);
    } else {
      const captureMode = await capture(archivePath);
      log(`[scenario] screenshot captured mode=${captureMode} path=${archivePath}`);
    }
  } catch (err) {
    const errorText = err && err.stack ? err.stack : String(err);
    log(`[scenario] screenshot failed label=${label} error=${errorText}`);
    const viewport = page.viewportSize() || {};
    return {
      ok: false,
      width: viewport.width || 0,
      height: viewport.height || 0,
      latestPath,
      archivePath: null,
      error: errorText
    };
  }
  const viewport = page.viewportSize() || {};
  return {
    ok: true,
    width: viewport.width || 0,
    height: viewport.height || 0,
    latestPath,
    archivePath
  };
}

function writeProbeReport(report, options = {}) {
  const writeLatest = options.writeLatest !== false;
  try {
    ensureDirSync(REPORTS_DIR);
    const latestPath = path.join(REPORTS_DIR, 'mcmain-phase197-latest.json');
    const archivePath = path.join(REPORTS_DIR, 'mcmain-phase197-' + isoFileStamp(new Date(report.finishedAt)) + '.json');
    const json = JSON.stringify(report, null, 2) + '\n';
    if (writeLatest) {
      fs.writeFileSync(latestPath, json);
      log('probe.report.latest ' + latestPath);
    }
    fs.writeFileSync(archivePath, json);
    log('probe.report.archive ' + archivePath);
    return { ok: true, latestPath: writeLatest ? latestPath : null, archivePath };
  } catch (err) {
    log('[probe-report-error] ' + (err && err.stack ? err.stack : String(err)));
    return { ok: false, latestPath: null, archivePath: null, error: err && err.stack ? err.stack : String(err) };
  }
}

function shouldRetainConsoleLine(line) {
  if (!line) {
    return false;
  }
  if (pickFrontierLine(line)) {
    return true;
  }
  if (line.length > 8192) {
    return false;
  }
  if (line.includes('screen=')) {
    return true;
  }
  if (line.includes('[ERROR]') || line.includes('Exception') || line.includes('Error:')) {
    return true;
  }
  return false;
}

async function launchSession(contextLabel) {
  let browser = null;
  let server = null;
  const consoleLines = [];
  const consoleEvents = [];
  const pageErrors = [];
  const requestFailures = [];
  const serverMessages = [];
  let frontierLogSeen = 0;
  let reusedServer = false;

  try {
    log(`[scenario] ${contextLabel} server.begin root=${ROOT}`);
    const existingHealthy = await isHealthyServer(BASE_URL);
    if (existingHealthy) {
      reusedServer = true;
      log(`[scenario] ${contextLabel} reusing healthy server ${BASE_URL}`);
    } else {
      server = spawn(process.execPath, [path.join(WORK_DIR, 'serve-web-run.cjs')], {
        cwd: WORK_DIR,
        stdio: ['ignore', 'pipe', 'pipe'],
        env: { ...process.env, WEBMC_PORT: String(PORT) }
      });
      server.stdout.on('data', (d) => {
        const line = String(d).trimEnd();
        if (line) {
          serverMessages.push(line);
          log('[server] ' + line);
        }
      });
      server.stderr.on('data', (d) => {
        const line = String(d).trimEnd();
        if (line) {
          serverMessages.push(line);
          log('[server-err] ' + line);
        }
      });

      const healthy = await waitForHealthyServer(BASE_URL, 15000);
      if (!healthy) {
        throw new Error('local web-run server did not become healthy at ' + BASE_URL);
      }
    }

    const launchStart = nowMs();
    browser = await chromium.launch({ headless: true });
    log(`[scenario] ${contextLabel} launch.ok +${nowMs() - launchStart}ms`);
    const page = await browser.newPage();
    page.on('console', (msg) => {
      const line = msg.text();
      const keep = shouldRetainConsoleLine(line);
      if (keep) {
        consoleLines.push(line);
        consoleEvents.push({ ts: nowMs(), line });
      }
      const shouldLogConsoleError = line.includes('Exception')
        || line.includes('Error:')
        || line.includes('[ERROR]')
        || line.includes('TypeError')
        || line.includes('NullPointerException');
      if (shouldLogConsoleError) {
        log('[console-' + msg.type() + '] ' + line);
      }
      if (keep && pickFrontierLine(line)) {
        frontierLogSeen++;
        const shouldLogFrontier = frontierLogSeen <= 400
          || frontierLogSeen % 500 === 0
          || line.includes('[mc-web/worldload] probe success:')
          || line.includes('[mc-web] setScreen:');
        if (shouldLogFrontier) {
          log('[frontier] ' + line);
        }
      }
    });
    page.on('pageerror', (err) => {
      const line = err && err.stack ? err.stack : String(err);
      pageErrors.push(line);
      log('[pageerror] ' + line);
    });
    page.on('requestfailed', (req) => {
      const line = req.url() + ' ' + req.failure().errorText;
      requestFailures.push(line);
      log('[requestfailed] ' + line);
    });
    await page.addInitScript(({ diagnostics }) => {
      window.webmcBootMode = 'mcMain';
      window.webmcDiagnostics = Boolean(diagnostics);
    }, { diagnostics: WEBMC_DIAGNOSTICS });
    const bust = Date.now();
    const url = BASE_URL
      + '?boot=mcMain&autostart=1&world='
      + encodeURIComponent(WORLD_NAME)
      + (WEBMC_DIAGNOSTICS ? '&diagnostics=1' : '')
      + '&t='
      + bust;
    log(`[scenario] ${contextLabel} goto.begin ${url}`);
    const navStart = nowMs();
    const response = await page.goto(url, { waitUntil: 'commit', timeout: 20000 });
    log(`[scenario] ${contextLabel} goto.commit ${response ? response.status() : 'no-response'} +${nowMs() - navStart}ms`);

    return {
      contextLabel,
      browser,
      server,
      page,
      url,
      navStart,
      consoleLines,
      consoleEvents,
      pageErrors,
      requestFailures,
      serverMessages,
      reusedServer,
      lastStatus: '',
      lastError: '',
      lastStatusChangedAt: nowMs(),
      cachedUiState: { status: '', error: '' },
      lastUiReadAt: 0,
      lastUiTimeoutLogAt: 0,
      frontierCount: 0,
      frontierLineCursor: 0,
      lastFrontierCount: 0,
      progressAccumulator: createProgressAccumulator(),
      progressEventCursor: 0,
      progressSnapshot: null,
      lastProgressAt: nowMs(),
      lastProgressKind: 'init',
      waitMsUsed: WAIT_MS,
      phaseExtras: createPhaseExtras(),
      evidence: createEvidenceBucket(),
      scenario: createScenarioState(SCENARIO, contextLabel)
    };
  } catch (err) {
    if (browser) {
      try {
        await browser.close();
      } catch (_) {
        // ignore
      }
    }
    if (server && !server.killed) {
      try {
        server.kill();
      } catch (_) {
        // ignore
      }
    }
    throw err;
  }
}

async function closeSession(ctx) {
  if (ctx.browser) {
    try {
      await ctx.browser.close();
      log(`[scenario] ${ctx.contextLabel} browser.closed`);
    } catch (err) {
      log('[close-error] ' + String(err));
    }
  }
  if (ctx.server && !ctx.server.killed) {
    try {
      ctx.server.kill();
      log(`[scenario] ${ctx.contextLabel} server.killed`);
    } catch (err) {
      log('[server-close-error] ' + String(err));
    }
  }
}

async function readUiStateWithTimeout(ctx, timeoutMs = 2000) {
  try {
    const timeoutPromise = new Promise((resolve) => {
      setTimeout(() => resolve({ __probeTimeout: true }), timeoutMs);
    });
    const evalPromise = ctx.page.evaluate(() => {
      const directState = window.__webmcState && typeof window.__webmcState === 'object'
        ? { ...window.__webmcState }
        : null;
      const latest = window.__webmcLatestState && typeof window.__webmcLatestState === 'object'
        ? window.__webmcLatestState
        : null;
      return {
        status: document.getElementById('status')?.textContent || '',
        error: document.getElementById('error')?.textContent || '',
        webState: directState || (latest && latest.state ? latest.state : null),
        webStateSource: latest && latest.source ? String(latest.source) : '',
        webStateAt: latest && latest.at ? Number(latest.at) : 0
      };
    });
    const result = await Promise.race([evalPromise, timeoutPromise]);
    if (result && result.__probeTimeout) {
      const now = nowMs();
      if (!ctx.lastUiTimeoutLogAt || now - ctx.lastUiTimeoutLogAt >= 30000) {
        ctx.lastUiTimeoutLogAt = now;
        log(`[scenario] ${ctx.contextLabel} uiState.read.timeout ${timeoutMs}ms`);
      }
      return { status: ctx.lastStatus || '', error: '', webState: ctx.lastWebState || null };
    }
    return result || { status: '', error: '' };
  } catch (err) {
    log(`[scenario] ${ctx.contextLabel} uiState.read.error ${err && err.message ? err.message : String(err)}`);
    return { status: ctx.lastStatus || '', error: '', webState: ctx.lastWebState || null };
  }
}

async function collectSnapshot(ctx) {
  const now = nowMs();
  let uiState = ctx.cachedUiState || { status: '', error: '' };
  if (!ctx.lastUiReadAt || now - ctx.lastUiReadAt >= 1000) {
    uiState = await readUiStateWithTimeout(ctx, 2000);
    ctx.cachedUiState = uiState;
    ctx.lastUiReadAt = nowMs();
  }

  if (uiState.status !== ctx.lastStatus) {
    ctx.lastStatus = uiState.status;
    ctx.lastStatusChangedAt = now;
  }
  ctx.lastError = uiState.error || '';

  const frontierCount = syncFrontierCount(ctx);
  if (frontierCount !== ctx.lastFrontierCount) {
    ctx.lastFrontierCount = frontierCount;
    ctx.lastProgressAt = now;
    ctx.lastProgressKind = 'frontier-log';
  } else if (now - ctx.lastStatusChangedAt < 500) {
    ctx.lastProgressAt = now;
    ctx.lastProgressKind = 'status-change';
  }

  if (uiState.webState) {
    ctx.lastWebState = uiState.webState;
  }

  const relMs = now - ctx.navStart;
  const stateProgress = applyWebStateProgress(ctx.progressAccumulator, ctx.lastWebState, relMs);
  if (stateProgress) {
    ctx.lastProgressAt = now;
    ctx.lastProgressKind = 'web-state';
  }

  const progress = syncProgress(ctx);
  if (ctx.phaseExtras.firstWorldloadSuccessAtMs === null && progress.phaseTime.firstWorldloadSuccessAt !== null) {
    ctx.phaseExtras.firstWorldloadSuccessAtMs = progress.phaseTime.firstWorldloadSuccessAt;
  }
  if (ctx.phaseExtras.reloadWorldloadSuccessAtMs === null && progress.phaseTime.secondWorldloadSuccessAt !== null) {
    ctx.phaseExtras.reloadWorldloadSuccessAtMs = progress.phaseTime.secondWorldloadSuccessAt;
  }

  return {
    now,
    elapsedMs: now - ctx.navStart,
    uiState,
    progress,
    frontierCount,
    pageErrors: ctx.pageErrors.slice(),
    requestFailures: ctx.requestFailures.slice(),
    consoleLines: ctx.consoleLines,
    webState: ctx.lastWebState
  };
}

function detectTerminalFailure(ctx, snapshot, allowStall) {
  const progress = snapshot.progress;
  const logs = snapshot.consoleLines;

  if (snapshot.uiState.error) {
    return {
      terminal: 'dom-error: ' + snapshot.uiState.error,
      classification: 'dom-error',
      failureCategory: 'orphan-process-contamination'
    };
  }
  if (snapshot.pageErrors.length > 0) {
    return {
      terminal: 'page-error',
      classification: 'dom-error',
      failureCategory: 'orphan-process-contamination'
    };
  }
  if (logs.some((line) => line.includes('[mc-web] MC main threw:'))) {
    return {
      terminal: 'mc-main-threw',
      classification: 'dom-error',
      failureCategory: 'orphan-process-contamination'
    };
  }
  if (allowStall && nowMs() - ctx.lastProgressAt >= STALL_MS) {
    return {
      terminal: 'stalled-no-progress',
      classification: 'stuck',
      failureCategory: classifyFailureCategory(progress, 'stalled-no-progress', ctx.scenario.mode, false)
    };
  }
  return null;
}

async function waitForCondition(ctx, options) {
  const timeoutMs = options.timeoutMs;
  const deadline = nowMs() + timeoutMs;
  const allowStall = options.allowStall !== false;

  while (nowMs() <= deadline) {
    const snapshot = await collectSnapshot(ctx);
    const failure = detectTerminalFailure(ctx, snapshot, allowStall);
    if (failure) {
      return { ok: false, failure, snapshot };
    }
    if (options.predicate(snapshot)) {
      return { ok: true, snapshot };
    }
    await ctx.page.waitForTimeout(options.intervalMs || 250);
  }

  const snapshot = await collectSnapshot(ctx);
  const terminalAtDeadline = detectTerminalFailure(ctx, snapshot, allowStall);
  if (terminalAtDeadline) {
    return { ok: false, failure: terminalAtDeadline, snapshot };
  }
  if (options.predicate(snapshot)) {
    return { ok: true, snapshot };
  }
  return {
    ok: false,
    failure: {
      terminal: 'timeout',
      classification: 'slow-startup',
      failureCategory: classifyFailureCategory(snapshot.progress, 'timeout', ctx.scenario.mode, options.afterWorldReady === true)
    },
    snapshot
  };
}

function classifyFailureCategory(progress, terminal, mode, afterWorldReady) {
  if (mode === 'soak' && afterWorldReady) {
    return 'soak-regression';
  }
  if (mode === 'save-reload' && afterWorldReady) {
    return 'save-reload-regression';
  }
  if (terminal && terminal.startsWith('dom-error')) {
    return 'orphan-process-contamination';
  }
  if (progress.phaseTime.placeNewPlayerEnterAt === null) {
    return 'startup-stall-before-place';
  }
  if (
    progress.phaseTime.placeNewPlayerEnterAt !== null &&
    progress.counts.setupRenderCount === 0 &&
    progress.counts.compileSectionsCount === 0 &&
    progress.phaseTime.worldloadSuccessAt === null
  ) {
    return 'startup-stall-after-place-before-render';
  }
  if (
    progress.phaseTime.placeNewPlayerEnterAt !== null &&
    (progress.counts.renderLevelCount > 0 || progress.counts.setupRenderCount > 0) &&
    progress.counts.compileSectionsCount === 0 &&
    progress.phaseTime.worldloadSuccessAt === null
  ) {
    return 'render-starvation';
  }
  if (progress.phaseTime.worldloadSuccessAt === null) {
    return 'world-ready-timeout';
  }
  return 'world-ready-timeout';
}

async function waitForWorldReady(ctx, targetSuccessCount, options = {}) {
  const graceMs = options.graceMs ?? getScenarioPlayableGraceMs(ctx.scenario.mode);
  const allowStall = options.allowStall !== undefined ? options.allowStall : false;
  const timeoutMs = Math.max(1, options.timeoutMs ?? WAIT_MS);
  const result = await waitForCondition(ctx, {
    timeoutMs,
    allowStall,
    predicate(snapshot) {
      return snapshot.progress.playable && snapshot.progress.counts.worldloadSuccessCount >= targetSuccessCount;
    }
  });
  if (!result.ok) {
    return result;
  }

  if (graceMs <= 0) {
    return result;
  }

  let firstPlayableAt = nowMs();
  const graceResult = await waitForCondition(ctx, {
    timeoutMs: Math.max(graceMs, 1),
    predicate(snapshot) {
      const meets = snapshot.progress.playable && snapshot.progress.counts.worldloadSuccessCount >= targetSuccessCount;
      if (!meets) {
        firstPlayableAt = nowMs();
        return false;
      }
      return nowMs() - firstPlayableAt >= graceMs;
    }
  });
  return graceResult.ok ? graceResult : graceResult;
}

async function focusCanvas(ctx) {
  await clickCanvasPosition(ctx, 20, 20, 'focus');
}

async function getCanvasRect(ctx) {
  try {
    const rect = await ctx.page.evaluate(() => {
      const canvas = document.querySelector('#canvas');
      if (!canvas) {
        return null;
      }
      const r = canvas.getBoundingClientRect();
      return { left: r.left, top: r.top, width: r.width, height: r.height };
    });
    if (!rect) {
      return null;
    }
    if (!Number.isFinite(rect.left) || !Number.isFinite(rect.top) || !Number.isFinite(rect.width) || !Number.isFinite(rect.height)) {
      return null;
    }
    return rect;
  } catch (err) {
    log(`[scenario] ${ctx.contextLabel} canvas.rect.error ${err && err.message ? err.message : String(err)}`);
    return null;
  }
}

async function clickCanvasPosition(ctx, relativeX, relativeY, source) {
  const rect = await getCanvasRect(ctx);
  if (rect && rect.width > 0 && rect.height > 0) {
    const absoluteX = rect.left + relativeX;
    const absoluteY = rect.top + relativeY;
    await ctx.page.mouse.click(absoluteX, absoluteY);
    return { usedCanvasRect: true, absoluteX, absoluteY };
  }
  await ctx.page.mouse.click(relativeX, relativeY);
  return { usedCanvasRect: false, absoluteX: relativeX, absoluteY: relativeY };
}

async function openPauseMenu(ctx) {
  await focusCanvas(ctx);
  for (let attempt = 0; attempt < 3; attempt++) {
    await ctx.page.keyboard.press('Escape');
    const result = await waitForCondition(ctx, {
      timeoutMs: 3000,
      allowStall: false,
      predicate(snapshot) {
        return hasAnyLine(snapshot.consoleLines, SCREEN_MARKERS.pause);
      }
    });
    if (result.ok) {
      const showPauseMenu = getLatestPauseInitState(result.snapshot.consoleLines);
      if (showPauseMenu === false) {
        log(`[scenario] ${ctx.contextLabel} pause.opened showPauseMenu=false attempt=${attempt + 1}`);
        await ctx.page.keyboard.press('Escape');
        await delay(200);
        continue;
      }
      if (ctx.phaseExtras.pauseOpenedAtMs === null) {
        ctx.phaseExtras.pauseOpenedAtMs = result.snapshot.elapsedMs;
      }
      log(
        `[scenario] ${ctx.contextLabel} pause.opened at=${ctx.phaseExtras.pauseOpenedAtMs}`
          + (showPauseMenu === null ? ' showPauseMenu=unknown' : ' showPauseMenu=true')
      );
      return result;
    }
  }
  return {
    ok: false,
    failure: {
      terminal: 'pause-open-failed',
      classification: 'stuck',
      failureCategory: 'save-reload-regression'
    },
    snapshot: await collectSnapshot(ctx)
  };
}

async function clickPauseQuitToTitle(ctx) {
  await focusCanvas(ctx);
  const getDisconnectSignalCount = (lines) =>
    countMatchingLines(lines, '[mc-web/pause] disconnect.click') + countMatchingLines(lines, '[mc-web/pause] disconnect.hotkey');
  const positions = [];
  const seen = new Set();
  const loggedTarget = getLatestPauseDisconnectTarget(ctx.consoleLines);
  if (loggedTarget) {
    addRectGridPositions(positions, seen, loggedTarget, 'pause-log');
  }
  const fallbackXs = [640, 628, 652, 612, 668, 427, 401, 453];
  const fallbackYs = [294, 286, 302, 278, 310, 246, 234, 258, 270];
  for (const x of fallbackXs) {
    for (const y of fallbackYs) {
      pushUniquePosition(positions, seen, x, y, `fallback-${x}-${y}`);
    }
  }

  const keyboardStrategies = [
    { name: 'f8-disconnect', sequence: ['F8'] },
    { name: 'shift-tab-enter', sequence: ['Shift+Tab', 'Enter'] },
    { name: 'tab7-enter', sequence: ['Tab', 'Tab', 'Tab', 'Tab', 'Tab', 'Tab', 'Tab', 'Enter'] },
    { name: 'tab8-enter', sequence: ['Tab', 'Tab', 'Tab', 'Tab', 'Tab', 'Tab', 'Tab', 'Tab', 'Enter'] }
  ];

  for (const strategy of keyboardStrategies) {
    const beforeProgress = syncProgress(ctx);
    const beforeTitleCount = beforeProgress.counts.titleScreenCount;
    const beforeDisconnectSignalCount = getDisconnectSignalCount(ctx.consoleLines);
    ctx.phaseExtras.quitToTitleAtMs = nowMs() - ctx.navStart;
    log(`[scenario] ${ctx.contextLabel} pause.quit.keys strategy=${strategy.name}`);
    for (const key of strategy.sequence) {
      await ctx.page.keyboard.press(key);
      await delay(120);
    }
    const activation = await waitForCondition(ctx, {
      timeoutMs: 4000,
      allowStall: false,
      predicate(snapshot) {
        return (
          snapshot.progress.counts.titleScreenCount > beforeTitleCount ||
          getDisconnectSignalCount(snapshot.consoleLines) > beforeDisconnectSignalCount ||
          !hasAnyLine(snapshot.consoleLines, SCREEN_MARKERS.pause)
        );
      }
    });
    if (!activation.ok) {
      continue;
    }

    if (activation.snapshot.progress.counts.titleScreenCount > beforeTitleCount) {
      ctx.phaseExtras.titleReturnedAtMs = activation.snapshot.elapsedMs;
      log(`[scenario] ${ctx.contextLabel} title.returned at=${ctx.phaseExtras.titleReturnedAtMs}`);
      return activation;
    }

    const titleResult = await waitForCondition(ctx, {
      timeoutMs: 20000,
      allowStall: false,
      predicate(snapshot) {
        return snapshot.progress.counts.titleScreenCount > beforeTitleCount;
      }
    });
    if (titleResult.ok) {
      ctx.phaseExtras.titleReturnedAtMs = titleResult.snapshot.elapsedMs;
      log(`[scenario] ${ctx.contextLabel} title.returned at=${ctx.phaseExtras.titleReturnedAtMs}`);
      return titleResult;
    }

    if (!hasAnyLine(titleResult.snapshot.consoleLines, SCREEN_MARKERS.pause)) {
      const reopen = await openPauseMenu(ctx);
      if (!reopen.ok) {
        return reopen;
      }
    }
  }

  for (const position of positions) {
    const beforeProgress = syncProgress(ctx);
    const beforeTitleCount = beforeProgress.counts.titleScreenCount;
    const beforeDisconnectSignalCount = getDisconnectSignalCount(ctx.consoleLines);
    ctx.phaseExtras.quitToTitleAtMs = nowMs() - ctx.navStart;
    log(
      `[scenario] ${ctx.contextLabel} pause.quit.click x=${position.x} y=${position.y}` + (position.source ? ` source=${position.source}` : '')
    );
    await clickCanvasPosition(ctx, position.x, position.y, position.source || 'pause-quit');
    const activation = await waitForCondition(ctx, {
      timeoutMs: 5000,
      allowStall: false,
      predicate(snapshot) {
        return (
          snapshot.progress.counts.titleScreenCount > beforeTitleCount ||
          getDisconnectSignalCount(snapshot.consoleLines) > beforeDisconnectSignalCount ||
          !hasAnyLine(snapshot.consoleLines, SCREEN_MARKERS.pause)
        );
      }
    });
    if (!activation.ok) {
      continue;
    }

    if (activation.snapshot.progress.counts.titleScreenCount > beforeTitleCount) {
      ctx.phaseExtras.titleReturnedAtMs = activation.snapshot.elapsedMs;
      log(`[scenario] ${ctx.contextLabel} title.returned at=${ctx.phaseExtras.titleReturnedAtMs}`);
      return activation;
    }

    const result = await waitForCondition(ctx, {
      timeoutMs: 30000,
      allowStall: false,
      predicate(snapshot) {
        return snapshot.progress.counts.titleScreenCount > beforeTitleCount;
      }
    });
    if (result.ok) {
      ctx.phaseExtras.titleReturnedAtMs = result.snapshot.elapsedMs;
      log(`[scenario] ${ctx.contextLabel} title.returned at=${ctx.phaseExtras.titleReturnedAtMs}`);
      return result;
    }

    if (hasAnyLine(result.snapshot.consoleLines, SCREEN_MARKERS.pause)) {
      continue;
    }
  }

  return {
    ok: false,
    failure: {
      terminal: 'quit-to-title-failed',
      classification: 'stuck',
      failureCategory: 'save-reload-regression'
    },
    snapshot: await collectSnapshot(ctx)
  };
}

async function reenterSavedWorld(ctx) {
  await focusCanvas(ctx);
  const titleToSelect = [
    { x: 640, y: 238 },
    { x: 628, y: 238 },
    { x: 652, y: 238 },
    { x: 640, y: 230 },
    { x: 640, y: 246 },
    { x: 427, y: 216 },
    { x: 427, y: 228 },
    { x: 427, y: 204 },
    { x: 427, y: 240 }
  ];
  const selectToPlay = [
    { x: 561, y: 678 },
    { x: 549, y: 678 },
    { x: 573, y: 678 },
    { x: 561, y: 670 },
    { x: 561, y: 686 },
    { x: 270, y: 396 },
    { x: 270, y: 408 },
    { x: 270, y: 384 },
    { x: 244, y: 396 }
  ];

  let selectWorldReached = false;
  for (const position of titleToSelect) {
    const beforeProgress = syncProgress(ctx);
    const beforeSelectWorldCount = beforeProgress.counts.selectWorldCount;
    await clickCanvasPosition(ctx, position.x, position.y, 'title-to-select');
    const result = await waitForCondition(ctx, {
      timeoutMs: 15000,
      allowStall: false,
      predicate(snapshot) {
        return snapshot.progress.counts.selectWorldCount > beforeSelectWorldCount;
      }
    });
    if (result.ok) {
      selectWorldReached = true;
      break;
    }
  }
  if (!selectWorldReached) {
    return {
      ok: false,
      failure: {
        terminal: 'select-world-open-failed',
        classification: 'stuck',
        failureCategory: 'save-reload-regression'
      },
      snapshot: await collectSnapshot(ctx)
    };
  }

  for (const position of selectToPlay) {
    const beforeProgress = syncProgress(ctx);
    const beforeWorldloadRequestedCount = beforeProgress.counts.worldloadRequestedCount;
    const beforeWorldloadSuccessCount = beforeProgress.counts.worldloadSuccessCount;
    const beforePlaceholderCount = beforeProgress.counts.placeholderScreenCount || 0;
    const beforeExperimentalCount = beforeProgress.counts.experimentalScreenCount || 0;
    const beforeWaitingForServerCount = beforeProgress.counts.waitingForServerScreenCount || 0;
    const beforeReceivingLevelCount = beforeProgress.counts.receivingLevelScreenCount || 0;
    const beforeDoWorldBeginCount = beforeProgress.counts.doWorldBeginCount || 0;
    ctx.phaseExtras.reloadRequestedAtMs = nowMs() - ctx.navStart;
    await clickCanvasPosition(ctx, position.x, position.y, 'select-to-play');
    const result = await waitForCondition(ctx, {
      timeoutMs: 30000,
      allowStall: false,
      predicate(snapshot) {
        return (
          snapshot.progress.counts.worldloadSuccessCount > beforeWorldloadSuccessCount ||
          snapshot.progress.counts.worldloadRequestedCount > beforeWorldloadRequestedCount ||
          (snapshot.progress.counts.placeholderScreenCount || 0) > beforePlaceholderCount ||
          (snapshot.progress.counts.experimentalScreenCount || 0) > beforeExperimentalCount ||
          (snapshot.progress.counts.waitingForServerScreenCount || 0) > beforeWaitingForServerCount ||
          (snapshot.progress.counts.receivingLevelScreenCount || 0) > beforeReceivingLevelCount ||
          (snapshot.progress.counts.doWorldBeginCount || 0) > beforeDoWorldBeginCount ||
          snapshot.consoleLines.some((line) => line.includes('[mc-web/selectworld] web open existing world:'))
        );
      }
    });
    if (result.ok) {
      return result;
    }
  }

  return {
    ok: false,
    failure: {
      terminal: 'reload-request-failed',
      classification: 'stuck',
      failureCategory: 'save-reload-regression'
    },
    snapshot: await collectSnapshot(ctx)
  };
}

function buildReportBase(ctx, scenarioMode, terminal, classification, failureCategory, finishedAtIso, elapsedMs) {
  const summary = summarizeFrontier(ctx.consoleLines);
  const progress = syncProgress(ctx);
  const phase = {
    worldloadRequestedAtMs: progress.phaseTime.worldloadRequestedAt,
    worldloadSuccessAtMs: progress.phaseTime.worldloadSuccessAt,
    serverReadyBeginAtMs: progress.phaseTime.serverReadyBeginAt,
    serverReadyEndAtMs: progress.phaseTime.serverReadyEndAt,
    loopbackAtMs: progress.phaseTime.loopbackAt,
    playableLoopAtMs: progress.phaseTime.playableLoopAt,
    serverConfigBeforeNewPlayerAtMs: progress.phaseTime.serverConfigBeforeNewPlayerAt,
    serverConfigBeforePlaceCallAtMs: progress.phaseTime.serverConfigBeforePlaceCallAt,
    placeNewPlayerEnterAtMs: progress.phaseTime.placeNewPlayerEnterAt,
    placeNewPlayerAfterLoginPacketAtMs: progress.phaseTime.placeNewPlayerAfterLoginPacketAt,
    serverConfigAfterPlaceNewPlayerAtMs: progress.phaseTime.serverConfigAfterPlaceNewPlayerAt,
    maxPersistentTick: progress.maxPersistentTick,
    requireWorldloadSuccess: REQUIRE_WORLDLOAD_SUCCESS,
    hasWorldload: progress.hasWorldload,
    hasWorldloadRequested: progress.hasWorldloadRequested,
    hasWorldloadSuccess: progress.hasWorldloadSuccess,
    hasServerReady: progress.hasServerReady,
    hasLoopback: progress.hasLoopback,
    hasPlacedPlayer: progress.hasPlacedPlayer,
    firstWorldloadSuccessAtMs: ctx.phaseExtras.firstWorldloadSuccessAtMs,
    pauseOpenedAtMs: ctx.phaseExtras.pauseOpenedAtMs,
    quitToTitleAtMs: ctx.phaseExtras.quitToTitleAtMs,
    titleReturnedAtMs: ctx.phaseExtras.titleReturnedAtMs,
    reloadRequestedAtMs: ctx.phaseExtras.reloadRequestedAtMs,
    reloadWorldloadSuccessAtMs: ctx.phaseExtras.reloadWorldloadSuccessAtMs,
    soakCompletedAtMs: ctx.phaseExtras.soakCompletedAtMs,
    maxRenderedSectionsFromState: progress.state.maxRenderedSections,
    maxPresentCountFromState: progress.state.maxPresentCount
  };

  const pass = classification === 'playable-loop';
  return {
    schemaVersion: 1,
    generatedAt: finishedAtIso,
    finishedAt: finishedAtIso,
    port: PORT,
    url: ctx.url,
    root: ROOT,
    waitMs: ctx.waitMsUsed ?? WAIT_MS,
    stallMs: STALL_MS,
    playableTickMin: PLAYABLE_TICK_MIN,
    soakMs: SOAK_MS,
    webmcDiagnostics: WEBMC_DIAGNOSTICS,
    elapsedMs,
    endReason: terminal === 'timeout' ? 'timeout' : 'terminal-condition',
    terminal,
    status: {
      lastStatus: ctx.lastStatus,
      lastError: ctx.lastError
    },
    classification,
    failureCategory,
    pass,
    progress: {
      lastKind: ctx.lastProgressKind,
      sinceLastMs: nowMs() - ctx.lastProgressAt
    },
    scenario: {
      mode: scenarioMode,
      repeatCount: scenarioMode === 'rc' ? REPEAT_COUNT : 1,
      soakMs: SOAK_MS,
      currentStep: ctx.scenario.currentStep,
      attempt: ctx.scenario.attempt,
      maxAttempts: ctx.scenario.maxAttempts,
      completed: ctx.scenario.completed,
      fullChainCompleted: ctx.scenario.fullChainCompleted,
      failureCategory: ctx.scenario.failureCategory,
      steps: ctx.scenario.steps,
      smokeAttemptCount: ctx.scenario.smokeAttemptCount,
      smokeMaxAttempts: ctx.scenario.smokeMaxAttempts,
      smokePassedAttempt: ctx.scenario.smokePassedAttempt,
      smokeAttempts: ctx.scenario.smokeAttempts
    },
    phase,
    evidence: ctx.evidence,
    frontier: summary,
    logPath
  };
}

function logSummary(summary, progress, classification, pass) {
  log('probe.classification ' + classification);
  log('probe.pass ' + (pass ? 'PASS' : 'FAIL'));
  log('probe.progress.lastKind ' + summary.lastKind);
  log('probe.progress.sinceLastMs ' + summary.sinceLastMs);
  log('probe.phase.worldloadRequestedAtMs ' + progress.phaseTime.worldloadRequestedAt);
  log('probe.phase.worldloadSuccessAtMs ' + progress.phaseTime.worldloadSuccessAt);
  log('probe.phase.serverReadyBeginAtMs ' + progress.phaseTime.serverReadyBeginAt);
  log('probe.phase.serverReadyEndAtMs ' + progress.phaseTime.serverReadyEndAt);
  log('probe.phase.loopbackAtMs ' + progress.phaseTime.loopbackAt);
  log('probe.phase.playableLoopAtMs ' + progress.phaseTime.playableLoopAt);
  log('probe.phase.serverConfigBeforeNewPlayerAtMs ' + progress.phaseTime.serverConfigBeforeNewPlayerAt);
  log('probe.phase.serverConfigBeforePlaceCallAtMs ' + progress.phaseTime.serverConfigBeforePlaceCallAt);
  log('probe.phase.placeNewPlayerEnterAtMs ' + progress.phaseTime.placeNewPlayerEnterAt);
  log('probe.phase.placeNewPlayerAfterLoginPacketAtMs ' + progress.phaseTime.placeNewPlayerAfterLoginPacketAt);
  log('probe.phase.serverConfigAfterPlaceNewPlayerAtMs ' + progress.phaseTime.serverConfigAfterPlaceNewPlayerAt);
  log('probe.phase.firstWorldloadSuccessAtMs ' + progress.phaseTime.firstWorldloadSuccessAt);
  log('probe.phase.secondWorldloadSuccessAtMs ' + progress.phaseTime.secondWorldloadSuccessAt);
  log('probe.phase.maxPersistentTick ' + progress.maxPersistentTick);
}

async function executeStandaloneScenario(mode, contextLabel, options = {}) {
  const ctx = await launchSession(contextLabel);
  ctx.scenario = createScenarioState(mode, contextLabel);
  ctx.scenario.currentStep = mode;
  ctx.scenario.attempt = Math.max(1, Number(options.attemptIndex || 1));
  ctx.scenario.maxAttempts = Math.max(1, Number(options.maxAttempts || 1));
  ctx.scenario.smokeAttemptCount = ctx.scenario.attempt;
  ctx.scenario.smokeMaxAttempts = ctx.scenario.maxAttempts;
  ctx.waitMsUsed = Math.max(1, Number(options.waitMs || WAIT_MS));
  let terminal = 'timeout-no-terminal';
  let classification = 'slow-startup';
  let failureCategory = null;
  let report = null;

  try {
    const firstWorld = await waitForWorldReady(ctx, 1, {
      graceMs: getScenarioPlayableGraceMs(mode),
      timeoutMs: ctx.waitMsUsed
    });
    if (!firstWorld.ok) {
      terminal = firstWorld.failure.terminal;
      classification = firstWorld.failure.classification;
      failureCategory = firstWorld.failure.failureCategory;
    } else {
      const stamp = isoFileStamp();
      ctx.evidence.screenshot = await captureScenarioScreenshot(
        ctx.page,
        stamp,
        options.screenshotLabel || 'worldready',
        options.updateLatest !== false
      );
      ctx.evidence.stages.firstWorld = ctx.evidence.screenshot;

      if (mode === 'smoke') {
        ctx.scenario.completed = true;
        classification = 'playable-loop';
        terminal = 'playable-loop';
      } else if (mode === 'soak') {
        const soakDeadline = Math.max(0, SOAK_MS);
        const soakResult = await waitForCondition(ctx, {
          timeoutMs: soakDeadline,
          afterWorldReady: true,
          predicate(snapshot) {
            return nowMs() - firstWorld.snapshot.now >= soakDeadline;
          }
        });
        if (!soakResult.ok) {
          terminal = soakResult.failure.terminal;
          classification = soakResult.failure.classification;
          failureCategory = 'soak-regression';
        } else {
          ctx.phaseExtras.soakCompletedAtMs = soakResult.snapshot.elapsedMs;
          ctx.evidence.stages.soakCompleted = await captureScenarioScreenshot(
            ctx.page,
            isoFileStamp(),
            options.soakLabel || 'soak-complete',
            false
          );
          ctx.scenario.completed = true;
          ctx.scenario.fullChainCompleted = true;
          classification = 'playable-loop';
          terminal = 'playable-loop';
        }
      } else if (mode === 'save-reload') {
        const pause = await openPauseMenu(ctx);
        if (!pause.ok) {
          terminal = pause.failure.terminal;
          classification = pause.failure.classification;
          failureCategory = pause.failure.failureCategory;
        } else {
          ctx.evidence.stages.pauseMenu = await captureScenarioScreenshot(
            ctx.page,
            isoFileStamp(),
            options.pauseLabel || 'pause-menu',
            false
          );
          const quit = await clickPauseQuitToTitle(ctx);
          if (!quit.ok) {
            terminal = quit.failure.terminal;
            classification = quit.failure.classification;
            failureCategory = quit.failure.failureCategory;
          } else {
            ctx.evidence.stages.titleReturn = await captureScenarioScreenshot(
              ctx.page,
              isoFileStamp(),
              options.titleLabel || 'title-return',
              false
            );
            const reloadStart = await reenterSavedWorld(ctx);
            if (!reloadStart.ok) {
              terminal = reloadStart.failure.terminal;
              classification = reloadStart.failure.classification;
              failureCategory = reloadStart.failure.failureCategory;
            } else {
              const reloadWorld = await waitForWorldReady(ctx, 2, {
                graceMs: getScenarioPlayableGraceMs(mode)
              });
              if (!reloadWorld.ok) {
                terminal = reloadWorld.failure.terminal;
                classification = reloadWorld.failure.classification;
                failureCategory = 'save-reload-regression';
              } else {
                ctx.phaseExtras.reloadWorldloadSuccessAtMs = reloadWorld.snapshot.progress.phaseTime.secondWorldloadSuccessAt;
                ctx.evidence.stages.reloadWorld = await captureScenarioScreenshot(
                  ctx.page,
                  isoFileStamp(),
                  options.reloadLabel || 'reload-worldready',
                  false
                );
                ctx.scenario.completed = true;
                ctx.scenario.fullChainCompleted = true;
                classification = 'playable-loop';
                terminal = 'playable-loop';
              }
            }
          }
        }
      } else {
        throw new Error('Unsupported scenario mode: ' + mode);
      }
    }

    ctx.scenario.failureCategory = failureCategory;
    const finishedAtIso = new Date().toISOString();
    report = buildReportBase(ctx, mode, terminal, classification, failureCategory, finishedAtIso, nowMs() - ctx.navStart);
    logSummary(report.progress, syncProgress(ctx), classification, report.pass);
    const writeResult = writeProbeReport(report, { writeLatest: options.writeLatest !== false });
    report.reportPaths = writeResult;
    if (!report.pass && options.setExitCode !== false) {
      process.exitCode = 1;
    }
    return report;
  } finally {
    await closeSession(ctx);
  }
}

const STARTUP_RETRYABLE_FAILURE_CATEGORIES = new Set([
  'startup-stall-before-place',
  'startup-stall-after-place-before-render',
  'render-starvation',
  'world-ready-timeout'
]);

function isStartupRetryableFailure(report) {
  if (!report || report.pass) {
    return false;
  }
  if (!STARTUP_RETRYABLE_FAILURE_CATEGORIES.has(String(report.failureCategory || ''))) {
    return false;
  }
  const phase = report.phase || {};
  return phase.firstWorldloadSuccessAtMs == null && phase.worldloadSuccessAtMs == null;
}

async function executeSmokeWithRetries(contextLabel, options = {}) {
  const maxAttempts = Math.max(1, Number(options.maxAttempts || SMOKE_MAX_ATTEMPTS));
  const attempts = [];
  let finalReport = null;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const waitMsForAttempt = attempt === 1 ? WAIT_MS : SMOKE_RETRY_WAIT_MS;
    const attemptSuffix = maxAttempts > 1 ? `-attempt-${attempt}` : '';
    const report = await executeStandaloneScenario('smoke', `${contextLabel}${attemptSuffix}`, {
      writeLatest: false,
      updateLatest: options.updateLatest !== false,
      screenshotLabel: options.screenshotLabel ? `${options.screenshotLabel}${attemptSuffix}` : undefined,
      waitMs: waitMsForAttempt,
      attemptIndex: attempt,
      maxAttempts,
      setExitCode: false
    });

    attempts.push({
      attempt,
      waitMs: waitMsForAttempt,
      pass: report.pass,
      classification: report.classification,
      failureCategory: report.failureCategory,
      terminal: report.terminal,
      archivePath: report.reportPaths ? report.reportPaths.archivePath : null
    });

    finalReport = report;
    if (report.pass) {
      break;
    }
    if (attempt < maxAttempts && SMOKE_RETRY_DELAY_MS > 0) {
      log(`[scenario] ${contextLabel} smoke retry scheduled attempt=${attempt + 1}/${maxAttempts} delayMs=${SMOKE_RETRY_DELAY_MS}`);
      await delay(SMOKE_RETRY_DELAY_MS);
    }
  }

  if (!finalReport) {
    throw new Error(`Smoke retries produced no report for ${contextLabel}`);
  }

  const passedAttempt = attempts.find((attemptItem) => attemptItem.pass);
  finalReport.scenario = {
    ...finalReport.scenario,
    currentStep: contextLabel,
    attempt: finalReport.scenario.attempt || attempts.length,
    maxAttempts,
    smokeAttemptCount: attempts.length,
    smokeMaxAttempts: maxAttempts,
    smokePassedAttempt: passedAttempt ? passedAttempt.attempt : null,
    smokeAttempts: attempts
  };
  finalReport.generatedAt = new Date().toISOString();
  finalReport.finishedAt = finalReport.generatedAt;
  finalReport.reportPaths = writeProbeReport(finalReport, { writeLatest: options.writeLatest !== false });
  if (!finalReport.pass && options.setExitCode !== false) {
    process.exitCode = 1;
  }
  return finalReport;
}

async function executeSoakWithRetries(contextLabel, options = {}) {
  const maxAttempts = Math.max(1, Number(options.maxAttempts || SOAK_MAX_ATTEMPTS));
  const attempts = [];
  let finalReport = null;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const waitMsForAttempt = attempt === 1 ? WAIT_MS : SOAK_RETRY_WAIT_MS;
    const attemptSuffix = maxAttempts > 1 ? `-attempt-${attempt}` : '';
    const report = await executeStandaloneScenario('soak', `${contextLabel}${attemptSuffix}`, {
      writeLatest: false,
      updateLatest: options.updateLatest !== false,
      screenshotLabel: options.screenshotLabel ? `${options.screenshotLabel}${attemptSuffix}` : undefined,
      soakLabel: options.soakLabel ? `${options.soakLabel}${attemptSuffix}` : undefined,
      waitMs: waitMsForAttempt,
      attemptIndex: attempt,
      maxAttempts,
      setExitCode: false
    });

    attempts.push({
      attempt,
      waitMs: waitMsForAttempt,
      pass: report.pass,
      classification: report.classification,
      failureCategory: report.failureCategory,
      terminal: report.terminal,
      archivePath: report.reportPaths ? report.reportPaths.archivePath : null
    });

    finalReport = report;
    if (report.pass) {
      break;
    }
    if (!isStartupRetryableFailure(report)) {
      break;
    }
    if (attempt < maxAttempts && SMOKE_RETRY_DELAY_MS > 0) {
      log(`[scenario] ${contextLabel} soak retry scheduled attempt=${attempt + 1}/${maxAttempts} delayMs=${SMOKE_RETRY_DELAY_MS}`);
      await delay(SMOKE_RETRY_DELAY_MS);
    }
  }

  if (!finalReport) {
    throw new Error(`Soak retries produced no report for ${contextLabel}`);
  }

  const passedAttempt = attempts.find((attemptItem) => attemptItem.pass);
  finalReport.scenario = {
    ...finalReport.scenario,
    currentStep: contextLabel,
    attempt: finalReport.scenario.attempt || attempts.length,
    maxAttempts,
    soakAttemptCount: attempts.length,
    soakMaxAttempts: maxAttempts,
    soakPassedAttempt: passedAttempt ? passedAttempt.attempt : null,
    soakAttempts: attempts
  };
  finalReport.generatedAt = new Date().toISOString();
  finalReport.finishedAt = finalReport.generatedAt;
  finalReport.reportPaths = writeProbeReport(finalReport, { writeLatest: options.writeLatest !== false });
  if (!finalReport.pass && options.setExitCode !== false) {
    process.exitCode = 1;
  }
  return finalReport;
}

async function executeSaveReloadWithRetries(contextLabel, options = {}) {
  const maxAttempts = Math.max(1, Number(options.maxAttempts || SAVE_RELOAD_MAX_ATTEMPTS));
  const attempts = [];
  let finalReport = null;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const waitMsForAttempt = attempt === 1 ? WAIT_MS : SAVE_RELOAD_RETRY_WAIT_MS;
    const attemptSuffix = maxAttempts > 1 ? `-attempt-${attempt}` : '';
    const report = await executeStandaloneScenario('save-reload', `${contextLabel}${attemptSuffix}`, {
      writeLatest: false,
      updateLatest: options.updateLatest !== false,
      screenshotLabel: options.screenshotLabel ? `${options.screenshotLabel}${attemptSuffix}` : undefined,
      pauseLabel: options.pauseLabel ? `${options.pauseLabel}${attemptSuffix}` : undefined,
      titleLabel: options.titleLabel ? `${options.titleLabel}${attemptSuffix}` : undefined,
      reloadLabel: options.reloadLabel ? `${options.reloadLabel}${attemptSuffix}` : undefined,
      waitMs: waitMsForAttempt,
      attemptIndex: attempt,
      maxAttempts,
      setExitCode: false
    });

    attempts.push({
      attempt,
      waitMs: waitMsForAttempt,
      pass: report.pass,
      classification: report.classification,
      failureCategory: report.failureCategory,
      terminal: report.terminal,
      archivePath: report.reportPaths ? report.reportPaths.archivePath : null
    });

    finalReport = report;
    if (report.pass) {
      break;
    }
    if (!isStartupRetryableFailure(report)) {
      break;
    }
    if (attempt < maxAttempts && SMOKE_RETRY_DELAY_MS > 0) {
      log(`[scenario] ${contextLabel} save-reload retry scheduled attempt=${attempt + 1}/${maxAttempts} delayMs=${SMOKE_RETRY_DELAY_MS}`);
      await delay(SMOKE_RETRY_DELAY_MS);
    }
  }

  if (!finalReport) {
    throw new Error(`Save-reload retries produced no report for ${contextLabel}`);
  }

  const passedAttempt = attempts.find((attemptItem) => attemptItem.pass);
  finalReport.scenario = {
    ...finalReport.scenario,
    currentStep: contextLabel,
    attempt: finalReport.scenario.attempt || attempts.length,
    maxAttempts,
    saveReloadAttemptCount: attempts.length,
    saveReloadMaxAttempts: maxAttempts,
    saveReloadPassedAttempt: passedAttempt ? passedAttempt.attempt : null,
    saveReloadAttempts: attempts
  };
  finalReport.generatedAt = new Date().toISOString();
  finalReport.finishedAt = finalReport.generatedAt;
  finalReport.reportPaths = writeProbeReport(finalReport, { writeLatest: options.writeLatest !== false });
  if (!finalReport.pass && options.setExitCode !== false) {
    process.exitCode = 1;
  }
  return finalReport;
}

async function executeRcScenario() {
  const aggregate = {
    steps: [],
    pass: true,
    classification: 'playable-loop',
    failureCategory: null,
    terminal: 'playable-loop'
  };
  let finalReport = null;

  for (let i = 0; i < REPEAT_COUNT; i++) {
    const stepMode = 'smoke';
    const label = `rc-smoke-${i + 1}`;
    const report = await executeSmokeWithRetries(label, {
      writeLatest: false,
      updateLatest: false,
      screenshotLabel: `${label}-worldready`,
      setExitCode: false
    });
    aggregate.steps.push({
      name: label,
      mode: stepMode,
      pass: report.pass,
      classification: report.classification,
      failureCategory: report.failureCategory,
      archivePath: report.reportPaths ? report.reportPaths.archivePath : null,
      attempts: report.scenario && Array.isArray(report.scenario.smokeAttempts) ? report.scenario.smokeAttempts : []
    });
    finalReport = report;
    if (!report.pass) {
      aggregate.pass = false;
      aggregate.classification = report.classification;
      aggregate.failureCategory = report.failureCategory;
      aggregate.terminal = report.terminal;
      break;
    }
  }

  if (aggregate.pass) {
    const soakReport = await executeSoakWithRetries('rc-soak', {
      writeLatest: false,
      updateLatest: false,
      screenshotLabel: 'rc-soak-worldready',
      soakLabel: 'rc-soak-complete',
      setExitCode: false
    });
    aggregate.steps.push({
      name: 'rc-soak',
      mode: 'soak',
      pass: soakReport.pass,
      classification: soakReport.classification,
      failureCategory: soakReport.failureCategory,
      archivePath: soakReport.reportPaths ? soakReport.reportPaths.archivePath : null,
      attempts: soakReport.scenario && Array.isArray(soakReport.scenario.soakAttempts)
        ? soakReport.scenario.soakAttempts
        : []
    });
    finalReport = soakReport;
    if (!soakReport.pass) {
      aggregate.pass = false;
      aggregate.classification = soakReport.classification;
      aggregate.failureCategory = soakReport.failureCategory;
      aggregate.terminal = soakReport.terminal;
    }
  }

  if (aggregate.pass) {
    const saveReloadReport = await executeSaveReloadWithRetries('rc-save-reload', {
      writeLatest: false,
      updateLatest: false,
      screenshotLabel: 'rc-save-reload-worldready',
      pauseLabel: 'rc-save-reload-pause-menu',
      titleLabel: 'rc-save-reload-title-return',
      reloadLabel: 'rc-save-reload-reload-worldready',
      setExitCode: false
    });
    aggregate.steps.push({
      name: 'rc-save-reload',
      mode: 'save-reload',
      pass: saveReloadReport.pass,
      classification: saveReloadReport.classification,
      failureCategory: saveReloadReport.failureCategory,
      archivePath: saveReloadReport.reportPaths ? saveReloadReport.reportPaths.archivePath : null,
      attempts: saveReloadReport.scenario && Array.isArray(saveReloadReport.scenario.saveReloadAttempts)
        ? saveReloadReport.scenario.saveReloadAttempts
        : []
    });
    finalReport = saveReloadReport;
    if (!saveReloadReport.pass) {
      aggregate.pass = false;
      aggregate.classification = saveReloadReport.classification;
      aggregate.failureCategory = saveReloadReport.failureCategory;
      aggregate.terminal = saveReloadReport.terminal;
    }
  }

  if (!finalReport) {
    throw new Error('RC scenario produced no sub-report');
  }

  const aggregateReport = JSON.parse(JSON.stringify(finalReport));
  aggregateReport.generatedAt = new Date().toISOString();
  aggregateReport.finishedAt = aggregateReport.generatedAt;
  aggregateReport.elapsedMs = null;
  aggregateReport.pass = aggregate.pass;
  aggregateReport.classification = aggregate.pass ? 'playable-loop' : aggregate.classification;
  aggregateReport.failureCategory = aggregate.failureCategory;
  aggregateReport.terminal = aggregate.terminal;
  aggregateReport.endReason = aggregate.pass ? 'terminal-condition' : finalReport.endReason;
  aggregateReport.scenario = {
    mode: 'rc',
    repeatCount: REPEAT_COUNT,
    soakMs: SOAK_MS,
    currentStep: aggregate.pass ? 'complete' : aggregate.steps[aggregate.steps.length - 1].name,
    attempt: 1,
    maxAttempts: 1,
    completed: aggregate.pass,
    fullChainCompleted: aggregate.pass,
    failureCategory: aggregate.failureCategory,
    steps: aggregate.steps,
    smokeAttemptCount: null,
    smokeMaxAttempts: SMOKE_MAX_ATTEMPTS,
    smokePassedAttempt: null,
    smokeAttempts: []
  };
  const writeResult = writeProbeReport(aggregateReport, { writeLatest: true });
  aggregateReport.reportPaths = writeResult;
  if (!aggregate.pass) {
    process.exitCode = 1;
  }
  return aggregateReport;
}

(async () => {
  try {
    log('serve.root ' + ROOT);
    log('probe.scenario ' + SCENARIO);
    log('probe.repeatCount ' + REPEAT_COUNT);
    log('probe.soakMs ' + SOAK_MS);
    log('probe.waitMs ' + WAIT_MS);
    log('probe.smokeMaxAttempts ' + SMOKE_MAX_ATTEMPTS);
    log('probe.smokeRetryWaitMs ' + SMOKE_RETRY_WAIT_MS);
    log('probe.smokeRetryDelayMs ' + SMOKE_RETRY_DELAY_MS);
    log('probe.soakMaxAttempts ' + SOAK_MAX_ATTEMPTS);
    log('probe.soakRetryWaitMs ' + SOAK_RETRY_WAIT_MS);
    log('probe.saveReloadMaxAttempts ' + SAVE_RELOAD_MAX_ATTEMPTS);
    log('probe.saveReloadRetryWaitMs ' + SAVE_RELOAD_RETRY_WAIT_MS);
    log('probe.requireWorldloadSuccess ' + REQUIRE_WORLDLOAD_SUCCESS);

    if (SCENARIO === 'smoke') {
      await executeSmokeWithRetries('smoke', { writeLatest: true });
    } else if (SCENARIO === 'soak') {
      await executeSoakWithRetries('soak', {
        writeLatest: true,
        screenshotLabel: 'soak-worldready',
        soakLabel: 'soak-complete'
      });
    } else if (SCENARIO === 'save-reload') {
      await executeSaveReloadWithRetries('save-reload', {
        writeLatest: true,
        screenshotLabel: 'save-reload-worldready',
        pauseLabel: 'save-reload-pause-menu',
        titleLabel: 'save-reload-title-return',
        reloadLabel: 'save-reload-reload-worldready'
      });
    } else if (SCENARIO === 'rc') {
      await executeRcScenario();
    } else {
      throw new Error('Unsupported SCENARIO=' + SCENARIO);
    }
  } catch (err) {
    log('[script-error] ' + (err && err.stack ? err.stack : String(err)));
    process.exitCode = 1;
  } finally {
    log('end ' + new Date().toISOString());
  }
})();
