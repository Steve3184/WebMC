const http = require('http');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');
const { chromium } = require('playwright');

const ROOT = path.join(process.cwd(), 'work', 'build', 'web-run');
const OUT_DIR = path.join(process.cwd(), 'output', 'playwright');
const WAIT_MENU_MS = Number(process.env.WAIT_MENU_MS || 210000);
const WAIT_WORLD_MS = Number(process.env.WAIT_WORLD_MS || 420000);
const SAMPLE_MS = Number(process.env.SAMPLE_MS || 15000);
const READ_STATE_TIMEOUT_MS = Number(process.env.READ_STATE_TIMEOUT_MS || 2500);
const VIEWPORT_WIDTH = Number(process.env.VIEWPORT_WIDTH || 1280);
const VIEWPORT_HEIGHT = Number(process.env.VIEWPORT_HEIGHT || 720);
const WEBMC_DIAGNOSTICS = !/^(0|false|no|off)$/i.test(String(process.env.WEBMC_DIAGNOSTICS || '0'));
const WEBMC_FRAMEPROBE = !/^(0|false|no|off)$/i.test(String(process.env.WEBMC_FRAMEPROBE || '0'));

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.wasm': 'application/wasm',
  '.bin': 'application/octet-stream',
  '.vfs': 'application/octet-stream',
  '.json': 'application/json; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png'
};

function safeJoin(root, reqUrl) {
  const parsed = new URL(reqUrl || '/', 'http://localhost');
  let pathname = decodeURIComponent(parsed.pathname || '/');
  if (pathname === '/' || pathname === '') pathname = '/index.html';
  const abs = path.normalize(path.join(root, pathname));
  return abs.startsWith(root) ? abs : null;
}

function createServer(beacons) {
  return http.createServer((req, res) => {
    const parsed = new URL(req.url || '/', 'http://localhost');
    if (parsed.pathname === '/__webmc_state') {
      let state = null;
      const raw = parsed.searchParams.get('d');
      if (raw) {
        try {
          state = JSON.parse(raw);
        } catch {
          state = { parseError: true, raw };
        }
      }
      beacons.push({
        receivedAt: Date.now(),
        source: parsed.searchParams.get('source') || '',
        state
      });
      res.writeHead(204);
      res.end();
      return;
    }

    const filePath = safeJoin(ROOT, req.url || '/');
    if (!filePath) {
      res.writeHead(403);
      res.end('Forbidden');
      return;
    }
    fs.readFile(filePath, (err, data) => {
      if (err) {
        res.writeHead(404);
        res.end('Not Found');
        return;
      }
      res.writeHead(200, {
        'Content-Type': MIME[path.extname(filePath).toLowerCase()] || 'application/octet-stream',
        'Cache-Control': 'no-cache'
      });
      res.end(data);
    });
  });
}

function latestBeaconState(beacons) {
  for (let i = beacons.length - 1; i >= 0; i--) {
    if (beacons[i] && beacons[i].state) return beacons[i].state;
  }
  return null;
}

async function evaluateWithTimeout(page, fn, timeoutMs = READ_STATE_TIMEOUT_MS) {
  return Promise.race([
    page.evaluate(fn).catch((err) => ({ __error: String((err && err.stack) || err) })),
    new Promise((resolve) => setTimeout(() => resolve({ __timeout: true }), timeoutMs))
  ]);
}

async function readSnapshot(page, beacons) {
  const readStartedAt = Date.now();
  const value = await evaluateWithTimeout(page, () => {
    const clone = (input) => {
      try {
        return input == null ? null : JSON.parse(JSON.stringify(input));
      } catch (err) {
        return { cloneError: String(err) };
      }
    };
    const menu = document.getElementById('webmc-main-menu');
    return {
      webState:
        clone(window.__webmcState || null) ||
        clone((window.__webmcLatestState && window.__webmcLatestState.state) || null),
      startupTimeline: clone(window.__webmcStartupTimeline || null),
      autoStartRequested: !!window.webmcAutoStartRequested,
      autoStartExperimentalWorld: window.webmcAutoStartExperimentalWorld || null,
      pendingAutoStartExperimentalWorld: window.__webmcPendingAutoStartExperimentalWorld || null,
      runtimeWorldStartRequestedAt: Number(window.__webmcRuntimeWorldStartRequestedAt || 0),
      runtimeWorldStartReleasedAt: Number(window.__webmcRuntimeWorldStartReleasedAt || 0),
      engineMenuReadyAt: Number(window.__webmcEngineMenuReadyAt || 0),
      menuVisible: !!(menu && menu.classList.contains('show')),
      menuButtonDisabled: !!document.querySelector('#webmc-main-menu button')?.disabled,
      menuButtonText: document.querySelector('#webmc-main-menu button')?.textContent || '',
      menuStatusText: document.querySelector('#webmc-main-menu .menu-status')?.textContent || '',
      engineMenuReady: !!window.__webmcEngineMenuReady,
      bootHidden: !!document.getElementById('boot')?.classList.contains('hidden'),
      statusText: document.getElementById('status')?.textContent || '',
      rafCount: Number(window.__webmcRafCount || 0),
      href: location.href
    };
  });
  const readDurationMs = Date.now() - readStartedAt;
  if (!value || value.__timeout || value.__error) {
    return {
      webState: latestBeaconState(beacons),
      readProblem: value || null,
      readDurationMs
    };
  }
  if (!value.webState) value.webState = latestBeaconState(beacons);
  value.readDurationMs = readDurationMs;
  return value;
}

function noteReadProblem(readProblems, phase, relMs, snapshot) {
  if (!snapshot) return;
  if (snapshot.readProblem || finiteNumber(snapshot.readDurationMs) > READ_STATE_TIMEOUT_MS / 2) {
    readProblems.push({
      phase,
      relMs,
      readDurationMs: finiteNumber(snapshot.readDurationMs),
      problem: snapshot.readProblem || null,
      screen: snapshot.webState && snapshot.webState.screen,
      levelPresent: !!(snapshot.webState && snapshot.webState.levelPresent),
      playerPresent: !!(snapshot.webState && snapshot.webState.playerPresent)
    });
    if (readProblems.length > 120) readProblems.shift();
  }
}

function hasUsableMetricSnapshot(snapshot) {
  const state = snapshot && snapshot.webState;
  return !!(
    state &&
    Number.isFinite(Number(snapshot.rafCount)) &&
    Number.isFinite(Number(state.mcFrameCount)) &&
    Number.isFinite(Number(state.presentCount)) &&
    Number.isFinite(Number(state.clientTickCount))
  );
}

async function readMetricSnapshot(page, beacons, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs;
  let snapshot = null;
  do {
    snapshot = await readSnapshot(page, beacons);
    if (hasUsableMetricSnapshot(snapshot)) {
      return snapshot;
    }
    await page.waitForTimeout(250);
  } while (Date.now() < deadline);
  return snapshot;
}

function isMenuReady(snapshot) {
  const state = (snapshot && snapshot.webState) || {};
  return (
    snapshot &&
    snapshot.menuVisible === true &&
    snapshot.autoStartRequested === false &&
    !state.levelPresent &&
    !state.playerPresent
  );
}

function isMenuActionReady(snapshot) {
  const state = (snapshot && snapshot.webState) || {};
  return (
    isMenuReady(snapshot) &&
    snapshot.menuButtonDisabled === false &&
    snapshot.menuButtonText === 'Singleplayer'
  );
}

function isEngineMenuReady(snapshot) {
  const state = (snapshot && snapshot.webState) || {};
  return (
    snapshot &&
    snapshot.engineMenuReady === true &&
    state.screen === 'TitleScreen' &&
    state.gameLoadFinished === true &&
    !state.levelPresent &&
    !state.playerPresent
  );
}

function isWorldReady(state) {
  return !!(
    state &&
    state.levelPresent &&
    state.playerPresent &&
    state.renderWorld &&
    state.worldRenderEligible &&
    state.webTerrainReady &&
    state.screen === 'null' &&
    state.overlay === 'null'
  );
}

function finiteNumber(value, fallback = 0) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function positiveNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : 0;
}

function summarizeState(state) {
  if (!state || typeof state !== 'object') return null;
  return {
    screen: state.screen || null,
    overlay: state.overlay || null,
    levelPresent: !!state.levelPresent,
    playerPresent: !!state.playerPresent,
    renderWorld: !!state.renderWorld,
    worldRenderEligible: !!state.worldRenderEligible,
    webTerrainReady: !!state.webTerrainReady,
    visibleSections: finiteNumber(state.visibleSections),
    renderedSections: finiteNumber(state.renderedSections),
    requiredRenderedSections: finiteNumber(state.requiredRenderedSections),
    visibleDirtySections: finiteNumber(state.visibleDirtySections),
    visibleUncompiledSections: finiteNumber(state.visibleUncompiledSections),
    visibleMissingNeighborSections: finiteNumber(state.visibleMissingNeighborSections),
    visibleSectionsNeedingBuild: finiteNumber(state.visibleSectionsNeedingBuild),
    sectionQueueEmpty: state.sectionQueueEmpty,
    hasRenderedAllSections: state.hasRenderedAllSections,
    presentCount: finiteNumber(state.presentCount),
    mcFrameCount: finiteNumber(state.mcFrameCount),
    clientTickCount: finiteNumber(state.clientTickCount),
    levelRenderUpdates: finiteNumber(state.levelRenderUpdates),
    renderGateUpdates: finiteNumber(state.renderGateUpdates),
    fpsString: state.fpsString || null
  };
}

function markFirst(progress, group, key, relMs, snapshot) {
  if (progress[group][key] == null) {
    progress[group][key] = relMs;
    if (snapshot) progress.firstSamples[key] = summarizeState(snapshot.webState || snapshot);
  }
}

function createWorldProgress() {
  return {
    phaseTimesMs: {},
    screenTimesMs: {},
    firstSamples: {},
    max: {
      visibleSections: 0,
      renderedSections: 0,
      requiredRenderedSections: 0,
      visibleDirtySections: 0,
      visibleUncompiledSections: 0,
      visibleMissingNeighborSections: 0,
      visibleSectionsNeedingBuild: 0,
      presentCount: 0,
      mcFrameCount: 0,
      clientTickCount: 0,
      levelRenderUpdates: 0,
      renderGateUpdates: 0
    },
    stateChanges: []
  };
}

function noteWorldProgress(progress, snapshot, relMs) {
  if (!snapshot || !snapshot.webState) return;
  const state = snapshot.webState;
  const screen = typeof state.screen === 'string' ? state.screen : '';
  const overlay = typeof state.overlay === 'string' ? state.overlay : '';
  if (screen) markFirst(progress, 'screenTimesMs', screen, relMs, snapshot);
  if (state.levelPresent) markFirst(progress, 'phaseTimesMs', 'firstLevelPresentAtMs', relMs, snapshot);
  if (state.playerPresent) markFirst(progress, 'phaseTimesMs', 'firstPlayerPresentAtMs', relMs, snapshot);
  if (state.levelPresent && state.playerPresent) {
    markFirst(progress, 'phaseTimesMs', 'firstLevelAndPlayerAtMs', relMs, snapshot);
  }
  if (state.renderWorld) markFirst(progress, 'phaseTimesMs', 'firstRenderWorldAtMs', relMs, snapshot);
  if (state.worldRenderEligible) markFirst(progress, 'phaseTimesMs', 'firstWorldRenderEligibleAtMs', relMs, snapshot);
  if (screen === 'null') markFirst(progress, 'phaseTimesMs', 'firstScreenNullAtMs', relMs, snapshot);
  if (overlay === 'null') markFirst(progress, 'phaseTimesMs', 'firstOverlayNullAtMs', relMs, snapshot);
  if (screen === 'null' && overlay === 'null') {
    markFirst(progress, 'phaseTimesMs', 'firstScreenNullNoOverlayAtMs', relMs, snapshot);
  }
  if (state.levelPresent && state.playerPresent && state.renderWorld && state.worldRenderEligible) {
    markFirst(progress, 'phaseTimesMs', 'firstPlayableNoTerrainAtMs', relMs, snapshot);
  }
  if (finiteNumber(state.visibleSections) > 0) {
    markFirst(progress, 'phaseTimesMs', 'firstVisibleSectionsAtMs', relMs, snapshot);
  }
  if (finiteNumber(state.renderedSections) > 0) {
    markFirst(progress, 'phaseTimesMs', 'firstRenderedSectionAtMs', relMs, snapshot);
  }
  if (finiteNumber(state.requiredRenderedSections) > 0) {
    markFirst(progress, 'phaseTimesMs', 'firstRequiredSectionsAtMs', relMs, snapshot);
  }
  if (
    finiteNumber(state.requiredRenderedSections) > 0 &&
    finiteNumber(state.renderedSections) >= finiteNumber(state.requiredRenderedSections)
  ) {
    markFirst(progress, 'phaseTimesMs', 'firstSufficientRenderedSectionsAtMs', relMs, snapshot);
  }
  if (state.sectionQueueEmpty === true) {
    markFirst(progress, 'phaseTimesMs', 'firstSectionQueueEmptyAtMs', relMs, snapshot);
  }
  if (finiteNumber(state.visibleDirtySections) > 0) {
    markFirst(progress, 'phaseTimesMs', 'firstVisibleDirtySectionsAtMs', relMs, snapshot);
  }
  if (finiteNumber(state.visibleUncompiledSections) > 0) {
    markFirst(progress, 'phaseTimesMs', 'firstVisibleUncompiledSectionsAtMs', relMs, snapshot);
  }
  if (finiteNumber(state.visibleMissingNeighborSections) > 0) {
    markFirst(progress, 'phaseTimesMs', 'firstVisibleMissingNeighborSectionsAtMs', relMs, snapshot);
  }
  if (finiteNumber(state.visibleSections) > 0 && finiteNumber(state.visibleSectionsNeedingBuild) === 0) {
    markFirst(progress, 'phaseTimesMs', 'firstNoVisibleSectionsNeedingBuildAtMs', relMs, snapshot);
  }
  if (finiteNumber(state.visibleSections) > 0 && finiteNumber(state.visibleUncompiledSections) === 0) {
    markFirst(progress, 'phaseTimesMs', 'firstNoVisibleUncompiledSectionsAtMs', relMs, snapshot);
  }
  if (state.hasRenderedAllSections) {
    markFirst(progress, 'phaseTimesMs', 'firstHasRenderedAllSectionsAtMs', relMs, snapshot);
  }
  if (state.webTerrainReady) {
    markFirst(progress, 'phaseTimesMs', 'firstWebTerrainReadyAtMs', relMs, snapshot);
  }
  if (isWorldReady(state)) {
    markFirst(progress, 'phaseTimesMs', 'firstWorldReadyAtMs', relMs, snapshot);
  }

  for (const key of Object.keys(progress.max)) {
    progress.max[key] = Math.max(progress.max[key], finiteNumber(state[key]));
  }

  const signature = [
    screen,
    overlay,
    state.levelPresent ? 'L1' : 'L0',
    state.playerPresent ? 'P1' : 'P0',
    state.renderWorld ? 'R1' : 'R0',
    state.worldRenderEligible ? 'E1' : 'E0',
    state.webTerrainReady ? 'T1' : 'T0',
    finiteNumber(state.renderedSections),
    finiteNumber(state.visibleSections),
    finiteNumber(state.requiredRenderedSections),
    finiteNumber(state.visibleDirtySections),
    finiteNumber(state.visibleUncompiledSections),
    finiteNumber(state.visibleMissingNeighborSections),
    finiteNumber(state.visibleSectionsNeedingBuild),
    state.sectionQueueEmpty,
    state.hasRenderedAllSections
  ].join('|');
  const previous = progress.stateChanges[progress.stateChanges.length - 1];
  if (!previous || previous.signature !== signature) {
    progress.stateChanges.push({ relMs, signature, state: summarizeState(state) });
    if (progress.stateChanges.length > 80) progress.stateChanges.shift();
  }
}

function collectConsoleMilestones(consoleEvents, clickAt) {
  const patterns = [
    ['runtimeStartExperimentalWorldAtMs', 'runtime start experimental world'],
    ['autoStartExperimentalWorldAtMs', 'auto start experimental world'],
    ['screenWebExperimentalWorldLoadAtMs', 'setScreen: net.minecraft.client.gui.screens.WebExperimentalWorldLoadScreen'],
    ['screenGenericMessageAtMs', 'setScreen: net.minecraft.client.gui.screens.GenericMessageScreen'],
    ['loadedRecipesAtMs', 'Loaded 1407 recipes'],
    ['loadedAdvancementsAtMs', 'Loaded 1520 advancements'],
    ['startingIntegratedServerAtMs', 'Starting integrated minecraft server'],
    ['preparingStartRegionAtMs', 'Preparing start region'],
    ['screenWebWaitingForServerAtMs', 'setScreen: net.minecraft.client.gui.screens.WebWaitingForServerScreen'],
    ['playerLoggedInAtMs', 'logged in with entity id'],
    ['screenReceivingLevelAtMs', 'setScreen: net.minecraft.client.gui.screens.ReceivingLevelScreen'],
    ['loadedPlayerAdvancementsAtMs', 'Loaded 2 advancements'],
    ['levelReadyLogAtMs', 'levelLoadStatusManager.levelReady'],
    ['screenNullAtMs', 'setScreen: null']
  ];
  const milestones = {};
  for (const event of consoleEvents) {
    if (!event || event.t < clickAt) continue;
    for (const [key, pattern] of patterns) {
      if (milestones[key] == null && event.text.includes(pattern)) {
        milestones[key] = event.t - clickAt;
      }
    }
  }
  return milestones;
}

function collectDiagnosticEvents(consoleEvents, clickAt) {
  const patterns = [
    '[mc-web/serverchunks]',
    '[mc-web/chunkfuture]',
    '[mc-web/chunkgen]',
    '[mc-web/chunkfull]',
    '[mc-web/chunks]',
    '[mc-web/clientpkt]',
    '[mc-web/clientlogin]',
    '[mc-web/renderGate]',
    '[mc-web/clienttick]',
    '[mc-web/worldload]',
    '[mc-web/render/state]',
    '[mc-web/compileSections]',
    '[mc-web/sectionDispatch]',
    '[mc-web/sectionTask]',
    '[mc-web/packetutils]',
    '[mc-web/server] prepareTickingChunk.complete',
    'logged in with entity id',
    'Starting integrated minecraft server',
    'Preparing start region',
    'setScreen: net.minecraft.client.gui.screens.ReceivingLevelScreen',
    'setScreen: null'
  ];
  const events = [];
  for (const event of consoleEvents) {
    if (!event || event.t < clickAt) continue;
    if (!patterns.some((pattern) => event.text.includes(pattern))) continue;
    events.push({
      relMs: event.t - clickAt,
      type: event.type,
      text: event.text
    });
    if (events.length > 500) {
      events.shift();
    }
  }
  return events;
}

function compactStartupTimeline(timeline) {
  if (!Array.isArray(timeline)) return [];
  return timeline
    .filter((event) => event && typeof event === 'object')
    .slice(0, 320)
    .map((event) => ({
      name: String(event.name || ''),
      detail: String(event.detail || ''),
      elapsedMs: finiteNumber(event.elapsedMs),
      at: finiteNumber(event.at)
    }))
    .filter((event) => event.name);
}

function latestStartupTimeline(...snapshots) {
  let best = [];
  for (const snapshot of snapshots) {
    const timeline = compactStartupTimeline(snapshot && snapshot.startupTimeline);
    if (timeline.length >= best.length) {
      best = timeline;
    }
  }
  return best;
}

function parseStartupConsole(text) {
  const match = String(text || '').match(/^\[mc-web\/startup\]\s+(\d+)ms\s+(\S+)(?:\s+(.*))?$/);
  if (!match) return null;
  return {
    elapsedMs: finiteNumber(match[1]),
    name: match[2],
    detail: match[3] || ''
  };
}

function collectStartupMilestones(consoleEvents, startupTimeline, clickAt) {
  const milestones = {};
  for (const event of compactStartupTimeline(startupTimeline)) {
    if (milestones[event.name] == null) {
      milestones[event.name] = event.elapsedMs;
    }
  }

  let startupOriginT = null;
  for (const event of consoleEvents) {
    const parsed = parseStartupConsole(event && event.text);
    if (!parsed) continue;
    startupOriginT = event.t - parsed.elapsedMs;
    if (parsed.name === 'bootstrap:start') break;
  }

  const patterns = [
    ['mainOptionParserCreatedAtMs', '[mc-probe] Main.main: OptionParser created'],
    ['mainBeforeParseAtMs', '[mc-probe] Main.main: about to parse args'],
    ['mainArgsParsedAtMs', '[mc-probe] Main.main: args parsed'],
    ['mainPreBootstrapTryAtMs', '[mc-probe] Main.main: pre-bootstrap try block'],
    ['mainBeforeTryDetectVersionAtMs', '[mc-probe] Main.main: before tryDetectVersion'],
    ['mainBeforeReportAppInfoAtMs', '[mc-probe] Main.main: before reportAppInfo'],
    ['mainSkipDataFixersAtMs', '[mc-probe] Main.main: skip DataFixers.optimize in web runtime'],
    ['mainBeforeCrashReportPreloadAtMs', '[mc-probe] Main.main: before CrashReport.preload'],
    ['mainSkipCrashReportPreloadAtMs', '[mc-probe] Main.main: skip CrashReport.preload in web runtime'],
    ['mainBeforeBootstrapAtMs', '[mc-probe] Main.main: before Bootstrap.bootStrap'],
    ['mainStageBeforeBootstrapAtMs', '[mc-main-stage] before-bootstrap'],
    ['bootstrapStartAtMs', '[mc-probe] Bootstrap.bootStrap: start'],
    ['bootstrapBuiltInRegistryAccessAtMs', '[mc-probe] Bootstrap: BuiltInRegistries.REGISTRY access'],
    ['bootstrapFireBlockAtMs', '[mc-probe] Bootstrap: FireBlock.bootStrap'],
    ['fireBlockEnteredAtMs', '[mc-probe] FireBlock.bootStrap entered'],
    ['fireBlockGotBlocksFireAtMs', '[mc-probe] FireBlock.bootStrap got Blocks.FIRE'],
    ['bootstrapComposterBlockAtMs', '[mc-probe] Bootstrap: ComposterBlock.bootStrap'],
    ['bootstrapEntityTypePlayerAtMs', '[mc-probe] Bootstrap: EntityType.PLAYER lookup'],
    ['bootstrapEntitySelectorOptionsAtMs', '[mc-probe] Bootstrap: EntitySelectorOptions'],
    ['bootstrapDispenseItemBehaviorAtMs', '[mc-probe] Bootstrap: DispenseItemBehavior'],
    ['bootstrapCauldronInteractionAtMs', '[mc-probe] Bootstrap: CauldronInteraction'],
    ['bootstrapBuiltInRegistriesAtMs', '[mc-probe] Bootstrap: BuiltInRegistries.bootStrap'],
    ['bootstrapCreativeModeTabsValidateAtMs', '[mc-probe] Bootstrap: CreativeModeTabs.validate'],
    ['bootstrapWrapStreamsAtMs', '[mc-probe] Bootstrap: wrapStreams'],
    ['bootstrapDoneAtMs', '[mc-probe] Bootstrap: done'],
    ['mainBeforeClientBootstrapAtMs', '[mc-probe] Main.main: before ClientBootstrap.bootstrap'],
    ['mainStageAfterBootstrapAtMs', '[mc-main-stage] after-bootstrap'],
    ['mainSkipShutdownHookAtMs', '[mc-probe] Main.main: skip Runtime.addShutdownHook in web runtime'],
    ['mainBeforeRenderThreadAtMs', '[mc-probe] Main.main: before RenderSystem.initRenderThread'],
    ['mainBeforeNewMinecraftAtMs', '[mc-probe] Main.main: before new Minecraft(gameconfig)'],
    ['mainBeforeMinecraftClassForNameAtMs', '[mc-main-stage] before-minecraft-classforname'],
    ['mainAfterMinecraftClassForNameAtMs', '[mc-main-stage] after-minecraft-classforname'],
    ['webMainStartAtMs', '[mc-web] WebMain.main start'],
    ['minecraftInitEnteredAtMs', '[mc-probe] Minecraft.<init>: 1 entered'],
    ['minecraftInitOptionsAtMs', '[mc-probe] Minecraft.<init>: 12 Options'],
    ['minecraftInitRenderSystemAtMs', '[mc-probe] Minecraft.<init>: 18.8 RenderSystem.initRenderer'],
    ['minecraftInitModelManagerAtMs', '[mc-probe] Minecraft.<init>: 35 ModelManager'],
    ['minecraftInitSetScreenAtMs', '[mc-probe] Minecraft.<init>: 62 setScreen'],
    ['minecraftInitStartReloadAtMs', '[mc-probe] Minecraft.<init>: 64 startReload'],
    ['minecraftInitCreateReloadAtMs', '[mc-probe] Minecraft.<init>: 65 createReload'],
    ['minecraftInitDoneAtMs', '[mc-probe] Minecraft.<init>: 69 done'],
    ['modelManagerReloadStartAtMs', '[mc-probe] ModelManager.reload start'],
    ['modelManagerApplyDoneAtMs', '[mc-probe] ModelManager.apply done'],
    ['simpleReloadFirstApplyDoneAtMs', 'phase=apply:done']
  ];

  for (const event of consoleEvents) {
    if (!event || !event.text) continue;
    const parsed = parseStartupConsole(event.text);
    if (parsed && milestones[parsed.name] == null) {
      milestones[parsed.name] = parsed.elapsedMs;
    }
    if (startupOriginT == null) continue;
    for (const [key, pattern] of patterns) {
      if (milestones[key] == null && event.text.includes(pattern)) {
        milestones[key] = event.t - startupOriginT;
      }
    }
  }

  return milestones;
}

function compactDiagnosticTimeline(snapshot, key) {
  const state = snapshot && snapshot.webState;
  const events = state && Array.isArray(state[key]) ? state[key] : [];
  return events.slice(0, 180).map((event) => ({
    elapsedMs: finiteNumber(event && event.elapsedMs),
    phase: String((event && event.phase) || ''),
    detail: String((event && event.detail) || ''),
    value: finiteNumber(event && event.value),
    at: finiteNumber(event && event.at)
  }));
}

function latestDiagnosticTimeline(key, ...snapshots) {
  let best = [];
  for (const snapshot of snapshots) {
    const timeline = compactDiagnosticTimeline(snapshot, key);
    if (timeline.length >= best.length) {
      best = timeline;
    }
  }
  return best;
}

function readAbsoluteSnapshotTime(snapshot, key) {
  return positiveNumber(snapshot && snapshot[key]);
}

function relFromAbsoluteTime(absMs, originMs) {
  return positiveNumber(absMs) ? Math.max(0, absMs - originMs) : null;
}

function buildQueuedStartMetrics({
  clickAt,
  worldStart,
  clickToWorldReadyMs,
  afterClickSnapshot,
  engineMenuReadyAfterClickAtMs,
  readySnapshot,
  worldProgress
}) {
  const pollOffsetMs = worldStart - clickAt;
  const clickToWorldReadyFromClickMs = clickToWorldReadyMs == null ? null : clickToWorldReadyMs + pollOffsetMs;
  const requestedAtAbs = readAbsoluteSnapshotTime(afterClickSnapshot, 'runtimeWorldStartRequestedAt');
  const releasedAtAbs = readAbsoluteSnapshotTime(readySnapshot, 'runtimeWorldStartReleasedAt');
  const engineMenuReadyAtAbs = readAbsoluteSnapshotTime(readySnapshot, 'engineMenuReadyAt');
  const requestedAtMs = relFromAbsoluteTime(requestedAtAbs, clickAt);
  const releasedAtMs = relFromAbsoluteTime(releasedAtAbs, clickAt);
  const engineMenuReadyAtMs = relFromAbsoluteTime(engineMenuReadyAtAbs, clickAt);
  const firstLevelAndPlayerAtMs =
    worldProgress && worldProgress.phaseTimesMs && worldProgress.phaseTimesMs.firstLevelAndPlayerAtMs != null
      ? worldProgress.phaseTimesMs.firstLevelAndPlayerAtMs + pollOffsetMs
      : null;
  const firstWebTerrainReadyAtMs =
    worldProgress && worldProgress.phaseTimesMs && worldProgress.phaseTimesMs.firstWebTerrainReadyAtMs != null
      ? worldProgress.phaseTimesMs.firstWebTerrainReadyAtMs + pollOffsetMs
      : null;
  const firstSufficientRenderedSectionsAtMs =
    worldProgress && worldProgress.phaseTimesMs && worldProgress.phaseTimesMs.firstSufficientRenderedSectionsAtMs != null
      ? worldProgress.phaseTimesMs.firstSufficientRenderedSectionsAtMs + pollOffsetMs
      : null;
  const firstHasRenderedAllSectionsAtMs =
    worldProgress && worldProgress.phaseTimesMs && worldProgress.phaseTimesMs.firstHasRenderedAllSectionsAtMs != null
      ? worldProgress.phaseTimesMs.firstHasRenderedAllSectionsAtMs + pollOffsetMs
      : null;
  const firstNoVisibleSectionsNeedingBuildAtMs =
    worldProgress && worldProgress.phaseTimesMs && worldProgress.phaseTimesMs.firstNoVisibleSectionsNeedingBuildAtMs != null
      ? worldProgress.phaseTimesMs.firstNoVisibleSectionsNeedingBuildAtMs + pollOffsetMs
      : null;
  const firstNoVisibleUncompiledSectionsAtMs =
    worldProgress && worldProgress.phaseTimesMs && worldProgress.phaseTimesMs.firstNoVisibleUncompiledSectionsAtMs != null
      ? worldProgress.phaseTimesMs.firstNoVisibleUncompiledSectionsAtMs + pollOffsetMs
      : null;

  return {
    pollOffsetMs,
    clickToWorldReadyFromClickMs,
    runtimeWorldStartRequestedAtMs: requestedAtMs,
    runtimeWorldStartReleasedAtMs: releasedAtMs,
    queuedWorldStartWaitMs: requestedAtMs == null || releasedAtMs == null ? null : Math.max(0, releasedAtMs - requestedAtMs),
    releasedToWorldReadyMs: releasedAtMs == null || clickToWorldReadyFromClickMs == null ? null : Math.max(0, clickToWorldReadyFromClickMs - releasedAtMs),
    engineMenuReadyAtMs,
    engineMenuReadyAfterClickAtMs,
    engineMenuReadyToReleaseMs:
      engineMenuReadyAtMs == null || releasedAtMs == null ? null : Math.max(0, releasedAtMs - engineMenuReadyAtMs),
    firstLevelAndPlayerAtMs,
    firstSufficientRenderedSectionsAtMs,
    firstHasRenderedAllSectionsAtMs,
    firstNoVisibleSectionsNeedingBuildAtMs,
    firstNoVisibleUncompiledSectionsAtMs,
    firstWebTerrainReadyAtMs,
    firstLevelAndPlayerAfterReleaseMs:
      firstLevelAndPlayerAtMs == null || releasedAtMs == null ? null : Math.max(0, firstLevelAndPlayerAtMs - releasedAtMs),
    firstSufficientRenderedSectionsAfterReleaseMs:
      firstSufficientRenderedSectionsAtMs == null || releasedAtMs == null
        ? null
        : Math.max(0, firstSufficientRenderedSectionsAtMs - releasedAtMs),
    firstHasRenderedAllSectionsAfterReleaseMs:
      firstHasRenderedAllSectionsAtMs == null || releasedAtMs == null ? null : Math.max(0, firstHasRenderedAllSectionsAtMs - releasedAtMs),
    firstNoVisibleSectionsNeedingBuildAfterReleaseMs:
      firstNoVisibleSectionsNeedingBuildAtMs == null || releasedAtMs == null
        ? null
        : Math.max(0, firstNoVisibleSectionsNeedingBuildAtMs - releasedAtMs),
    firstNoVisibleUncompiledSectionsAfterReleaseMs:
      firstNoVisibleUncompiledSectionsAtMs == null || releasedAtMs == null
        ? null
        : Math.max(0, firstNoVisibleUncompiledSectionsAtMs - releasedAtMs),
    firstWebTerrainReadyAfterReleaseMs:
      firstWebTerrainReadyAtMs == null || releasedAtMs == null ? null : Math.max(0, firstWebTerrainReadyAtMs - releasedAtMs)
  };
}

async function readPageDebugSnapshot(page, beacons) {
  const snapshot = await readSnapshot(page, beacons);
  return {
    snapshot,
    beaconTail: beacons.slice(-30)
  };
}

async function writeFailureArtifacts(page, result, consoleEvents, beacons, label) {
  const safeLabel = String(label || 'failure').replace(/[^a-z0-9_-]+/gi, '-').toLowerCase();
  const base = path.join(OUT_DIR, `main-menu-click-smoothness-${safeLabel}`);
  result.consoleLog = `${base}.console.json`;
  result.debugSnapshot = await readPageDebugSnapshot(page, beacons).catch((err) => ({
    readError: String((err && err.stack) || err)
  }));
  fs.writeFileSync(result.consoleLog, JSON.stringify(consoleEvents, null, 2));
  result.screenshot = `${base}.png`;
  await page.screenshot({ path: result.screenshot, fullPage: true, timeout: 30000 }).catch((err) => {
    result.screenshotError = String((err && err.stack) || err);
  });
}

function deltaRate(after, before, key, sampleMs) {
  const a = Number((after && after[key]) || 0);
  const b = Number((before && before[key]) || 0);
  return {
    before: b,
    after: a,
    delta: a - b,
    rate: sampleMs > 0 ? ((a - b) * 1000) / sampleMs : 0
  };
}

function safeRound(value, digits = 1) {
  const number = Number(value);
  if (!Number.isFinite(number)) return null;
  const factor = 10 ** digits;
  return Math.round(number * factor) / factor;
}

function summarizeNodeMemory() {
  const usage = process.memoryUsage();
  return {
    rssMb: safeRound(usage.rss / (1024 * 1024)),
    heapTotalMb: safeRound(usage.heapTotal / (1024 * 1024)),
    heapUsedMb: safeRound(usage.heapUsed / (1024 * 1024)),
    externalMb: safeRound(usage.external / (1024 * 1024))
  };
}

function listPlaywrightMcpProfiles() {
  const localAppData = process.env.LOCALAPPDATA;
  if (!localAppData) return [];
  const root = path.join(localAppData, 'ms-playwright-mcp');
  if (!fs.existsSync(root)) return [];
  return fs
    .readdirSync(root, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && /^mcp-chrome-/i.test(entry.name))
    .map((entry) => {
      const fullPath = path.join(root, entry.name);
      const stat = fs.statSync(fullPath);
      return {
        name: entry.name,
        fullPath,
        lastWriteTimeIso: stat.mtime.toISOString(),
        ageMinutes: safeRound((Date.now() - stat.mtimeMs) / 60000)
      };
    })
    .sort((a, b) => a.ageMinutes - b.ageMinutes)
    .slice(0, 20);
}

function captureTrackedProcesses() {
  if (process.platform !== 'win32') return [];
  const command = `
$top = Get-Process -Name java,node,chrome,msedge -ErrorAction SilentlyContinue |
  Sort-Object WorkingSet64 -Descending |
  Select-Object -First 25
if (-not $top) {
  '[]'
  exit 0
}
$detailsWanted = @(
  $top | Where-Object {
    ($_.WorkingSet64 -ge 200MB) -or
    ($_.ProcessName -in @('java', 'chrome', 'msedge')) -or
    ($_.ProcessName -eq 'node' -and $_.WorkingSet64 -ge 150MB)
  } | Select-Object -First 10
)
$details = @{}
if ($detailsWanted.Count -gt 0) {
  $filter = [string]::Join(' OR ', @($detailsWanted | ForEach-Object { "ProcessId = $($_.Id)" }))
  Get-CimInstance Win32_Process -Filter $filter -ErrorAction SilentlyContinue | ForEach-Object {
    $details[[int]$_.ProcessId] = $_
  }
}
$top | ForEach-Object {
  $detail = $details[[int]$_.Id]
  [pscustomobject]@{
    processId = $_.Id
    parentProcessId = if ($detail) { $detail.ParentProcessId } else { $null }
    name = "$($_.ProcessName).exe"
    workingSetMb = [math]::Round($_.WorkingSet64 / 1MB, 1)
    cpuSeconds = if ($_.CPU -ne $null) { [math]::Round($_.CPU, 1) } else { $null }
    commandLine = if ($detail) { $detail.CommandLine } else { $null }
    commandLineOmitted = -not [bool]$detail
  }
} | ConvertTo-Json -Depth 4 -Compress
`;
  try {
    const raw = execFileSync(
      'powershell',
      ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', command],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'], timeout: 12000 }
    ).trim();
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [parsed];
  } catch (error) {
    return [{ captureError: String((error && error.message) || error) }];
  }
}

function buildHostLoadAssessment(snapshot) {
  const freeMemoryMb = Number(snapshot && snapshot.freeMemoryMb) || 0;
  const trackedProcesses = Array.isArray(snapshot && snapshot.trackedProcesses) ? snapshot.trackedProcesses : [];
  const heavyProcesses = trackedProcesses.filter(
    (proc) =>
      Number.isFinite(Number(proc && proc.workingSetMb)) &&
      (Number(proc.workingSetMb) >= 200 || Number(proc.cpuSeconds) >= 300)
  );
  const assessments = [];
  if (freeMemoryMb > 0 && freeMemoryMb < 2048) {
    assessments.push(`lowFreeMemory:${freeMemoryMb}MB`);
  }
  if (heavyProcesses.length >= 3) {
    assessments.push(`heavyTrackedProcesses:${heavyProcesses.length}`);
  }
  const staleProfiles = Array.isArray(snapshot && snapshot.playwrightMcpProfiles)
    ? snapshot.playwrightMcpProfiles.filter((profile) => Number(profile.ageMinutes) >= 480)
    : [];
  if (staleProfiles.length > 0) {
    assessments.push(`stalePlaywrightMcpProfiles:${staleProfiles.length}`);
  }
  return {
    assessments,
    heavyProcesses: heavyProcesses.slice(0, 10)
  };
}

function captureHostLoadSnapshot() {
  const snapshot = {
    capturedAtIso: new Date().toISOString(),
    platform: process.platform,
    release: os.release(),
    arch: process.arch,
    cpuCount: Array.isArray(os.cpus()) ? os.cpus().length : null,
    totalMemoryMb: safeRound(os.totalmem() / (1024 * 1024)),
    freeMemoryMb: safeRound(os.freemem() / (1024 * 1024)),
    processUptimeSec: safeRound(process.uptime()),
    nodeMemory: summarizeNodeMemory(),
    playwrightMcpProfiles: listPlaywrightMcpProfiles(),
    trackedProcesses: captureTrackedProcesses()
  };
  snapshot.assessment = buildHostLoadAssessment(snapshot);
  return snapshot;
}

(async () => {
  if (!fs.existsSync(path.join(ROOT, 'index.html'))) {
    throw new Error(`Missing web-run build at ${ROOT}. Run npm run phase197:build first.`);
  }
  fs.mkdirSync(OUT_DIR, { recursive: true });

  const beacons = [];
  const consoleEvents = [];
  const pageErrors = [];
  const requestFailures = [];
  const mainFrameNavigations = [];
  const readProblems = [];
  const server = createServer(beacons);
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  const url = `http://127.0.0.1:${port}/?boot=mcMain${WEBMC_DIAGNOSTICS ? '&diagnostics=1' : ''}${WEBMC_FRAMEPROBE ? '&frameprobe=1' : ''}&t=${Date.now()}`;

  let browser = null;
  let page = null;
  let result = null;
  const hostLoadBeforeRun = captureHostLoadSnapshot();
  try {
    browser = await chromium.launch({ headless: true });
    page = await browser.newPage({
      viewport: { width: VIEWPORT_WIDTH, height: VIEWPORT_HEIGHT }
    });
    await page.addInitScript(({ diagnostics, frameProbe }) => {
      window.webmcBootMode = 'mcMain';
      window.webmcDiagnostics = !!diagnostics;
      window.webmcFrameProbe = !!frameProbe;
      window.__webmcRafCount = 0;
      const loop = () => {
        window.__webmcRafCount = (window.__webmcRafCount || 0) + 1;
        requestAnimationFrame(loop);
      };
      requestAnimationFrame(loop);
    }, { diagnostics: WEBMC_DIAGNOSTICS, frameProbe: WEBMC_FRAMEPROBE });

    page.on('console', (msg) => {
      consoleEvents.push({ type: msg.type(), text: msg.text(), t: Date.now() });
    });
    page.on('pageerror', (err) => pageErrors.push(String((err && err.stack) || err)));
    page.on('requestfailed', (req) => {
      requestFailures.push({
        url: req.url(),
        error: req.failure() && req.failure().errorText
      });
    });
    page.on('framenavigated', (frame) => {
      if (frame === page.mainFrame()) {
        mainFrameNavigations.push({ t: Date.now(), url: frame.url() });
      }
    });

    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 90000 });

    const menuStart = Date.now();
    let menuReadyAtMs = null;
    let menuSnapshot = null;
    while (Date.now() - menuStart < WAIT_MENU_MS) {
      menuSnapshot = await readSnapshot(page, beacons);
      noteReadProblem(readProblems, 'menu', Date.now() - menuStart, menuSnapshot);
      if (isMenuReady(menuSnapshot)) {
        menuReadyAtMs = Date.now() - menuStart;
        break;
      }
      await page.waitForTimeout(1000);
    }
    if (menuReadyAtMs == null) {
      throw new Error(`Timed out waiting for menu. latest=${JSON.stringify(menuSnapshot)}`);
    }

    let menuActionReadyAtMs = null;
    let menuActionSnapshot = null;
    while (Date.now() - menuStart < WAIT_MENU_MS) {
      menuActionSnapshot = await readSnapshot(page, beacons);
      noteReadProblem(readProblems, 'menuAction', Date.now() - menuStart, menuActionSnapshot);
      if (isMenuActionReady(menuActionSnapshot)) {
        menuActionReadyAtMs = Date.now() - menuStart;
        break;
      }
      await page.waitForTimeout(1000);
    }
    if (menuActionReadyAtMs == null) {
      throw new Error(`Timed out waiting for actionable menu. latest=${JSON.stringify(menuActionSnapshot)}`);
    }

    const navigationCountBeforeClick = mainFrameNavigations.length;
    const clickAt = Date.now();
    await page.locator('#webmc-main-menu button').click({ timeout: 30000 });
    const afterClickSnapshot = await readSnapshot(page, beacons);
    noteReadProblem(readProblems, 'afterClick', Date.now() - clickAt, afterClickSnapshot);

    const worldStart = Date.now();
    const worldProgress = createWorldProgress();
    noteWorldProgress(worldProgress, afterClickSnapshot, Date.now() - worldStart);
    let engineMenuReadyAfterClickAtMs = null;
    let engineMenuReadyAfterClickSnapshot = null;
    let clickToWorldReadyMs = null;
    let readySnapshot = null;
    while (Date.now() - worldStart < WAIT_WORLD_MS) {
      readySnapshot = await readSnapshot(page, beacons);
      noteReadProblem(readProblems, 'world', Date.now() - worldStart, readySnapshot);
      noteWorldProgress(worldProgress, readySnapshot, Date.now() - worldStart);
      if (engineMenuReadyAfterClickAtMs == null && isEngineMenuReady(readySnapshot)) {
        engineMenuReadyAfterClickAtMs = Date.now() - worldStart;
        engineMenuReadyAfterClickSnapshot = readySnapshot;
      }
      if (isWorldReady(readySnapshot.webState)) {
        clickToWorldReadyMs = Date.now() - worldStart;
        break;
      }
      await page.waitForTimeout(1000);
    }
    if (clickToWorldReadyMs == null) {
      throw new Error(`Timed out waiting for playable world after click. latest=${JSON.stringify(readySnapshot)}`);
    }

    const before = await readMetricSnapshot(page, beacons);
    noteReadProblem(readProblems, 'sampleBefore', Date.now() - worldStart, before);
    await page.waitForTimeout(SAMPLE_MS);
    const after = await readMetricSnapshot(page, beacons);
    noteReadProblem(readProblems, 'sampleAfter', Date.now() - worldStart, after);
    const beforeState = before.webState || {};
    const afterState = after.webState || {};
    const metrics = {
      sampleMs: SAMPLE_MS,
      raf: {
        before: Number(before.rafCount || 0),
        after: Number(after.rafCount || 0),
        delta: Number(after.rafCount || 0) - Number(before.rafCount || 0),
        rate: (Number(after.rafCount || 0) - Number(before.rafCount || 0)) * 1000 / SAMPLE_MS
      },
      mcFrame: deltaRate(afterState, beforeState, 'mcFrameCount', SAMPLE_MS),
      present: deltaRate(afterState, beforeState, 'presentCount', SAMPLE_MS),
      renderGate: deltaRate(afterState, beforeState, 'renderGateUpdates', SAMPLE_MS),
      levelRender: deltaRate(afterState, beforeState, 'levelRenderUpdates', SAMPLE_MS),
      clientTick: deltaRate(afterState, beforeState, 'clientTickCount', SAMPLE_MS)
    };

    const screenshot = path.join(OUT_DIR, 'main-menu-click-smoothness-latest.png');
    await page.screenshot({ path: screenshot, fullPage: true, timeout: 30000 }).catch((err) => {
      consoleEvents.push({
        type: 'screenshot-error',
        text: String((err && err.stack) || err),
        t: Date.now()
      });
    });

    const startupTimeline = latestStartupTimeline(menuSnapshot, menuActionSnapshot, afterClickSnapshot, readySnapshot, before, after);
    const startupMilestonesMs = collectStartupMilestones(consoleEvents, startupTimeline, clickAt);
    const modelReloadTimeline = latestDiagnosticTimeline(
      'modelReloadEvents',
      menuSnapshot,
      menuActionSnapshot,
      afterClickSnapshot,
      readySnapshot,
      before,
      after
    );
    const reloadTimeline = latestDiagnosticTimeline(
      'reloadEvents',
      menuSnapshot,
      menuActionSnapshot,
      afterClickSnapshot,
      readySnapshot,
      before,
      after
    );
    const queuedStartMetrics = buildQueuedStartMetrics({
      clickAt,
      worldStart,
      clickToWorldReadyMs,
      afterClickSnapshot,
      engineMenuReadyAfterClickAtMs,
      readySnapshot,
      worldProgress
    });

    result = {
      ok: hasUsableMetricSnapshot(before) && hasUsableMetricSnapshot(after) && metrics.present.rate >= 10 && metrics.mcFrame.rate >= 10,
      url,
      diagnostics: WEBMC_DIAGNOSTICS,
      frameProbeEnabled: WEBMC_FRAMEPROBE,
      hostLoadBeforeRun,
      hostLoadAfterRun: captureHostLoadSnapshot(),
      startupTimeline,
      startupMilestonesMs,
      modelReloadTimeline,
      reloadTimeline,
      menuReadyAtMs,
      menuSnapshot,
      menuActionReadyAtMs,
      menuActionSnapshot,
      engineMenuReadyAfterClickAtMs,
      engineMenuReadyAfterClickSnapshot,
      clickToWorldReadyMs,
      queuedStartMetrics,
      clickAtMs: clickAt - menuStart,
      afterClickSnapshot,
      readySnapshot,
      worldProgress,
      consoleMilestonesMs: collectConsoleMilestones(consoleEvents, clickAt),
      diagnosticEvents: collectDiagnosticEvents(consoleEvents, clickAt),
      beforeState,
      afterState,
      metrics,
      urlUnchangedAfterClick: page.url().startsWith(`http://127.0.0.1:${port}/?boot=mcMain`),
      noMainFrameNavigationAfterClick: mainFrameNavigations.length === navigationCountBeforeClick,
      mainFrameNavigations,
      runtimeStartLogSeen: consoleEvents.some(
        (event) =>
          event.text.includes('auto start experimental world') ||
          event.text.includes('setScreen: net.minecraft.client.gui.screens.WebExperimentalWorldLoadScreen')
      ),
      pageErrors,
      requestFailures,
      readProblems,
      uboResizeLogCount: consoleEvents.filter((event) => event.text.includes('Resizing Dynamic Transforms UBO')).length,
      cantKeepUpCount: consoleEvents.filter((event) => event.text.includes("Can't keep up")).length,
      gpuStallReadPixelsCount: consoleEvents.filter((event) => event.text.includes('GPU stall due to ReadPixels')).length,
      consoleTail: consoleEvents.slice(-100),
      screenshot
    };
    if (!result.ok || !result.urlUnchangedAfterClick || !result.noMainFrameNavigationAfterClick) {
      process.exitCode = 1;
    }
  } catch (err) {
    result = {
      ok: false,
      error: String((err && err.stack) || err),
      hostLoadBeforeRun,
      hostLoadAfterRun: captureHostLoadSnapshot(),
      pageErrors,
      requestFailures,
      readProblems,
      beaconsTail: beacons.slice(-12),
      consoleTail: consoleEvents.slice(-100),
      url
    };
    if (page) {
      await writeFailureArtifacts(page, result, consoleEvents, beacons, 'failure').catch((artifactErr) => {
        result.failureArtifactError = String((artifactErr && artifactErr.stack) || artifactErr);
      });
    }
    process.exitCode = 1;
  } finally {
    if (browser) await browser.close().catch(() => {});
    await new Promise((resolve) => server.close(resolve)).catch(() => {});
    const report = path.join(OUT_DIR, 'main-menu-click-smoothness-latest.json');
    fs.writeFileSync(report, JSON.stringify(result, null, 2));
    console.log(`clickSmooth.report=${report}`);
    console.log(`clickSmooth.ok=${!!result.ok}`);
    if (result && result.metrics) {
      console.log(`clickSmooth.menuReadyAtMs=${result.menuReadyAtMs}`);
      console.log(`clickSmooth.menuActionReadyAtMs=${result.menuActionReadyAtMs}`);
      console.log(`clickSmooth.clickToWorldReadyMs=${result.clickToWorldReadyMs}`);
      if (result.queuedStartMetrics) {
        console.log(`clickSmooth.queuedWorldStartWaitMs=${result.queuedStartMetrics.queuedWorldStartWaitMs}`);
        console.log(`clickSmooth.releasedToWorldReadyMs=${result.queuedStartMetrics.releasedToWorldReadyMs}`);
        console.log(`clickSmooth.clickToWorldReadyFromClickMs=${result.queuedStartMetrics.clickToWorldReadyFromClickMs}`);
      }
      if (result.startupMilestonesMs) {
        const startup = result.startupMilestonesMs;
        console.log(`clickSmooth.webfsPreloadDoneAtMs=${startup['webfs:preload:done']}`);
        console.log(`clickSmooth.mcMainBeginAtMs=${startup['webmain:mc-main:begin']}`);
        console.log(`clickSmooth.modelManagerReloadStartAtMs=${startup.modelManagerReloadStartAtMs}`);
      }
      if (result.worldProgress && result.worldProgress.phaseTimesMs) {
        const phase = result.worldProgress.phaseTimesMs;
        console.log(`clickSmooth.firstLevelAndPlayerAtMs=${phase.firstLevelAndPlayerAtMs}`);
        console.log(`clickSmooth.firstPlayableNoTerrainAtMs=${phase.firstPlayableNoTerrainAtMs}`);
        console.log(`clickSmooth.firstRenderedSectionAtMs=${phase.firstRenderedSectionAtMs}`);
        console.log(`clickSmooth.firstWebTerrainReadyAtMs=${phase.firstWebTerrainReadyAtMs}`);
      }
      console.log(`clickSmooth.rafRate=${result.metrics.raf.rate.toFixed(2)}`);
      console.log(`clickSmooth.mcFrameRate=${result.metrics.mcFrame.rate.toFixed(2)}`);
      console.log(`clickSmooth.presentRate=${result.metrics.present.rate.toFixed(2)}`);
      console.log(`clickSmooth.clientTickRate=${result.metrics.clientTick.rate.toFixed(2)}`);
      console.log(`clickSmooth.cantKeepUpCount=${result.cantKeepUpCount}`);
      console.log(`clickSmooth.uboResizeLogCount=${result.uboResizeLogCount}`);
      console.log(`clickSmooth.urlUnchangedAfterClick=${result.urlUnchangedAfterClick}`);
      console.log(`clickSmooth.noMainFrameNavigationAfterClick=${result.noMainFrameNavigationAfterClick}`);
    }
  }
})();
