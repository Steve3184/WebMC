// bootstrap.js — page-side glue for the TeaVM build.
//
// TeaVM with moduleType=NONE produces a non-module IIFE that registers the
// entry point as a global function (default name `main`). It expects to be
// invoked with `main(args, callback)`. We just include game.js via a
// classic <script> tag (set in index.html), then call window.main here.

(function () {
    // ── Guard BigInt / Math from NaN / Infinity before game.js runs ──────────
    // TeaVM callers invoke BigInt(x) and BigInt.asIntN/asUintN(bits, x) with
    // Math.min/max results that can be NaN.  Without this, Chrome throws
    // "RangeError: The number NaN cannot be converted to a BigInt".
    (function () {
        var OrigBigInt  = BigInt;
        var OrigAsIntN  = BigInt.asIntN;
        var OrigAsUintN = BigInt.asUintN;
        BigInt = function (v) {
            if (v !== v || !Number.isFinite(v)) return OrigBigInt(0);
            try { return OrigAsIntN(32, v); } catch (e) { return OrigBigInt(0); }
        };
        BigInt.asIntN  = function (bits, val) {
            if (val !== val || !Number.isFinite(val)) return OrigAsIntN(bits, OrigBigInt(0));
            try { return OrigAsIntN(bits, val); } catch (e) { return OrigAsIntN(bits, OrigBigInt(0)); }
        };
        BigInt.asUintN = function (bits, val) {
            if (val !== val || !Number.isFinite(val)) return OrigAsUintN(bits, OrigBigInt(0));
            try { return OrigAsUintN(bits, val); } catch (e) { return OrigAsUintN(bits, OrigBigInt(0)); }
        };
        // Also protect Math.min/max which produce NaN
        var OrigMath_min = Math.min;
        var OrigMath_max = Math.max;
        Math.min = function () {
            for (var i = 0; i < arguments.length; i++) if (arguments[i] !== arguments[i]) return 0;
            return OrigMath_min.apply(this, arguments);
        };
        Math.max = function () {
            for (var i = 0; i < arguments.length; i++) if (arguments[i] !== arguments[i]) return 0;
            return OrigMath_max.apply(this, arguments);
        };
    })();

    const params = new URLSearchParams(window.location.search);
    const modeFromUrl = params.get('boot');
    const presetBootMode = typeof window.webmcBootMode === 'string' ? window.webmcBootMode : null;
    const resolvedBootMode =
        modeFromUrl === 'webSafeBoot' || modeFromUrl === 'mcMain'
            ? modeFromUrl
            : presetBootMode === 'webSafeBoot' || presetBootMode === 'mcMain'
                ? presetBootMode
                : 'mcMain';
    const explicitAutoStartWorld =
        params.get('autoStartExperimentalWorld') ||
        params.get('webmcAutoStartExperimentalWorld');
    const worldNameFromUrl =
        params.get('world') ||
        params.get('worldName');
    const autoStartParam = params.get('autostart');
    const autoStartExplicitlyDisabled = autoStartParam != null && /^(0|false|off|no)$/i.test(autoStartParam);
    const autoStartExplicitlyEnabled = autoStartParam != null && !autoStartExplicitlyDisabled;
    const diagnosticsParam = params.get('diagnostics');
    const diagnosticsEnabled = diagnosticsParam == null
        ? !!window.webmcDiagnostics
        : /^(1|true|on|yes)$/i.test(diagnosticsParam);
    const autoStartEnabled =
        resolvedBootMode === 'mcMain' &&
        !autoStartExplicitlyDisabled &&
        (autoStartExplicitlyEnabled || !!(explicitAutoStartWorld && explicitAutoStartWorld.trim()));
    const startupOriginAt = window.__webmcStartupOriginAt || Date.now();
    window.__webmcStartupOriginAt = startupOriginAt;

    function recordStartup(name, detail) {
        if (!diagnosticsEnabled) {
            return;
        }
        const now = Date.now();
        const event = {
            name: String(name || ''),
            detail: String(detail || ''),
            at: now,
            elapsedMs: now - startupOriginAt
        };
        const timeline = window.__webmcStartupTimeline || (window.__webmcStartupTimeline = []);
        if (timeline.length < 320) {
            timeline.push(event);
        }
        console.log(
            '[mc-web/startup] ' +
                event.elapsedMs +
                'ms ' +
                event.name +
                (event.detail ? ' ' + event.detail : '')
        );
    }

    window.__webmcStartupMark = recordStartup;
    recordStartup('bootstrap:start', 'boot=' + resolvedBootMode + ' autoStart=' + autoStartEnabled);

    function installConsoleFilter() {
        if (typeof window.__webmcInstallConsoleFilter === 'function') {
            window.__webmcInstallConsoleFilter();
            return;
        }
        if (diagnosticsEnabled || window.__webmcConsoleFilterInstalled) {
            return;
        }
        window.__webmcConsoleFilterInstalled = true;
        const noisyPrefixes = [
            '[mc-web/serverchunks]',
            '[mc-web/clientpkt]',
            '[mc-web/clientlogin]',
            '[mc-web/chunks]',
            '[mc-web/sectionCompile]',
            '[mc-web/sectionDispatch]',
            '[mc-web/sectionTask]',
            '[mc-web/sectionCopy]',
            '[mc-web/sectionMesh]',
            '[mc-web/compileSections]',
            '[mc-web/setupRender]',
            '[mc-web/mainpass]',
            '[mc-web/skypass]',
            '[mc-web/collectVisibleEntities]',
            '[mc-web/occlusion]',
            '[mc-web/render/state]',
            '[mc-web/renderLevel]',
            '[mc-web/syncher]',
            '[mc-main-stage]',
            '[stdout-test]',
            '[stderr-test]',
            '[INFO] mc: [mc-web/',
            '[WARN] mc: Couldn\'t find glyph',
            '[mc-web/chunkgen]',
            '[mc-web/clienttick]',
            '[mc-web/servergame]',
            '[mc-web/server]',
            '[mc-web/serverlevel]',
            '[mc-web/level]',
            '[mc-web/gameload]',
            '[mc-web/worldloader]',
            '[mc-web/worldload]',
            '[mc-web/ctor]',
            '[mc-web/packrepo]',
            '[mc-web/loopback]',
            '[mc-web/packetutils]',
            '[mc-web/entityRenderers]',
            '[mc-web/regionCache]',
            '[mc-web/poi]',
            '[mc-web/run]',
            '[mc-web/vfs] WebFs.boot:',
            '[mc-web/vfs] WebFs.preload:',
            '[mc-web/vfs] WebFs: parsing',
            '[mc-web/vfs] WebFs: ... loaded',
            '[mc-probe] SpriteLoader',
            '[mc-probe] Stitcher',
            '[mc-probe] TinyPng',
            '[mc-probe] ModelManager',
            '[mc-probe] SimpleReloadInstance'
        ];
        for (const level of ['log', 'info', 'warn', 'error']) {
            const original = console[level] && console[level].bind(console);
            if (!original) continue;
            console[level] = (...args) => {
                const text = args.length ? String(args[0]) : '';
                if (text.length > 8192) {
                    return;
                }
                if (noisyPrefixes.some((prefix) => text.startsWith(prefix))) {
                    return;
                }
                original(...args);
            };
        }
    }

    // Boot modes:
    // - mcMain (default): run the browser-playable experimental entry path
    // - webSafeBoot: run browser bootstrap only; skip MC Main.main
    window.webmcBootMode = resolvedBootMode;
    window.webmcAutoStartRequested = autoStartEnabled;
    window.webmcDiagnostics = diagnosticsEnabled;
    installConsoleFilter();

    const $boot     = document.getElementById('boot');
    const $status   = document.getElementById('status');
    const $progress = document.getElementById('progress');
    const $progressBar = $progress ? $progress.querySelector('div') : null;
    const $hint     = document.getElementById('hint');
    const $error    = document.getElementById('error');
    const $canvas   = document.getElementById('canvas');
    let bootHidden = false;
    let latestBootStatus = '';
    let latestProgress = 0;
    let $mainMenu = null;
    let $mainMenuButton = null;
    let $mainMenuStatus = null;
    let engineMenuReady = false;
    let pendingWorldStartName = '';
    let pendingWorldStartNeedsSettle = false;
    let worldStartReleaseScheduled = false;
    const WORLD_START_RELEASE_SETTLE_MS = 500;

    function ensureWebAudioState() {
        const state = window.__webmcState || (window.__webmcState = {});
        const AudioCtor = window.AudioContext || window.webkitAudioContext;
        if (!AudioCtor) {
            state.audioSupported = false;
            state.audioUnlocked = false;
            state.audioState = 'unsupported';
            return null;
        }

        if (!window.__webmcAudioContext) {
            try {
                window.__webmcAudioContext = new AudioCtor();
            } catch (err) {
                state.audioSupported = false;
                state.audioUnlocked = false;
                state.audioState = 'failed';
                state.audioError = String(err && err.message ? err.message : err);
                return null;
            }
        }

        const ctx = window.__webmcAudioContext;
        state.audioSupported = true;
        state.audioUnlocked = ctx.state === 'running';
        state.audioState = String(ctx.state || 'unknown');
        return ctx;
    }

    function tryUnlockWebAudio() {
        const ctx = ensureWebAudioState();
        if (!ctx) {
            return;
        }
        if (ctx.state === 'running') {
            return;
        }

        try {
            const result = ctx.resume && ctx.resume();
            if (result && typeof result.then === 'function') {
                result.then(() => ensureWebAudioState()).catch(() => ensureWebAudioState());
            } else {
                ensureWebAudioState();
            }
        } catch (err) {
            const state = window.__webmcState || (window.__webmcState = {});
            state.audioError = String(err && err.message ? err.message : err);
            ensureWebAudioState();
        }
    }

    // Expose global function for TeaVM/Java to call when user interaction triggers audio
    window.__webmcUnlockAudio = tryUnlockWebAudio;

    /**
     * Check if audio context is running and update state.
     * Called periodically by Java side to verify audio status.
     */
    function pollAudioState() {
        const ctx = window.__webmcAudioContext;
        if (!ctx) {
            return false;
        }
        if (ctx.state === 'running') {
            ensureWebAudioState();
            return true;
        }
        // Try to resume if suspended
        if (ctx.state === 'suspended') {
            tryUnlockWebAudio();
        }
        return false;
    }
    window.__webmcPollAudioState = pollAudioState;

    function hideBoot() {
        if (!bootHidden && $boot) {
            bootHidden = true;
            $boot.classList.add('hidden');
        }
    }

    function showBoot() {
        if (bootHidden && $boot) {
            bootHidden = false;
            $boot.classList.remove('hidden');
        }
    }

    function pickWorldName() {
        return (
            (worldNameFromUrl && worldNameFromUrl.trim()) ||
            (explicitAutoStartWorld && explicitAutoStartWorld.trim()) ||
            'Web World'
        );
    }

    function releaseExperimentalWorldStart(worldName) {
        if (window.webmcAutoStartExperimentalWorld) {
            return;
        }
        window.webmcAutoStartExperimentalWorld = worldName || pendingWorldStartName || pickWorldName();
        window.__webmcRuntimeWorldStartReleasedAt = Date.now();
        window.__webmcPendingAutoStartExperimentalWorld = null;
        pendingWorldStartName = '';
        pendingWorldStartNeedsSettle = false;
        setBootStatus('Loading world...', 40);
    }

    function scheduleWorldStartRelease() {
        if (
            !pendingWorldStartName ||
            !engineMenuReady ||
            window.webmcAutoStartExperimentalWorld ||
            worldStartReleaseScheduled
        ) {
            return;
        }

        worldStartReleaseScheduled = true;
        const release = () => {
            worldStartReleaseScheduled = false;
            if (!pendingWorldStartName || !engineMenuReady || window.webmcAutoStartExperimentalWorld) {
                return;
            }
            releaseExperimentalWorldStart(pendingWorldStartName);
        };
        const delayMs = pendingWorldStartNeedsSettle ? WORLD_START_RELEASE_SETTLE_MS : 0;
        const afterFrame = () => setTimeout(release, delayMs);
        if (typeof requestAnimationFrame === 'function') {
            requestAnimationFrame(afterFrame);
        } else {
            afterFrame();
        }
    }

    function queueExperimentalWorldStart(worldName) {
        const pickedWorldName = worldName || pickWorldName();
        window.webmcAutoStartRequested = true;
        window.__webmcRuntimeWorldStartRequestedAt = Date.now();
        window.__webmcPendingAutoStartExperimentalWorld = pickedWorldName;
        pendingWorldStartName = pickedWorldName;
        pendingWorldStartNeedsSettle = !engineMenuReady;
        showBoot();
        setBootStatus('Loading world...', 40);
        hideMainMenu();
        updateMainMenuActionState();
        scheduleWorldStartRelease();
    }

    function requestExperimentalWorldStart() {
        if (window.webmcAutoStartRequested) {
            return;
        }
        tryUnlockWebAudio();
        queueExperimentalWorldStart(pickWorldName());
    }

    function updateMainMenuActionState() {
        if (!$mainMenuButton) {
            return;
        }
        const waitingForEngine = !engineMenuReady && !window.webmcAutoStartRequested;
        $mainMenuButton.disabled = !!window.webmcAutoStartRequested;
        $mainMenuButton.textContent = 'Singleplayer';
        if ($mainMenuStatus) {
            $mainMenuStatus.textContent = waitingForEngine ? 'Preparing Minecraft...' : '';
        }
    }

    function ensureMainMenu() {
        if ($mainMenu) {
            updateMainMenuActionState();
            return $mainMenu;
        }

        const style = document.createElement('style');
        style.textContent = [
            '#webmc-main-menu{position:absolute;inset:0;display:none;align-items:center;justify-content:center;pointer-events:auto;background:linear-gradient(180deg,rgba(10,14,20,.72),rgba(0,0,0,.88));color:#fff;font-family:monospace;}',
            '#webmc-main-menu.show{display:flex;}',
            '#webmc-main-menu .menu-panel{display:flex;flex-direction:column;align-items:center;gap:14px;width:min(360px,80vw);}',
            '#webmc-main-menu .title{font-size:38px;line-height:1;font-weight:700;text-shadow:0 3px 0 #111,0 0 18px rgba(255,255,255,.18);}',
            '#webmc-main-menu .menu-status{height:18px;font-size:14px;color:#cfcfcf;text-shadow:1px 1px 0 #111;}',
            '#webmc-main-menu button{width:100%;height:42px;border:2px solid #1b1b1b;background:#777;color:#fff;font:700 18px monospace;text-shadow:1px 1px 0 #111;box-shadow:inset 2px 2px 0 rgba(255,255,255,.28),inset -2px -2px 0 rgba(0,0,0,.35);cursor:pointer;}',
            '#webmc-main-menu button:hover{background:#8d8d8d;}',
            '#webmc-main-menu button:active{background:#666;box-shadow:inset -2px -2px 0 rgba(255,255,255,.18),inset 2px 2px 0 rgba(0,0,0,.45);}',
            '#webmc-main-menu button:disabled{opacity:.58;cursor:default;background:#555;}'
        ].join('');
        document.head.appendChild(style);

        $mainMenu = document.createElement('div');
        $mainMenu.id = 'webmc-main-menu';
        $mainMenu.innerHTML = '<div class="menu-panel"><div class="title">Minecraft</div><div class="menu-status"></div><button type="button">Singleplayer</button></div>';
        $mainMenuButton = $mainMenu.querySelector('button');
        $mainMenuStatus = $mainMenu.querySelector('.menu-status');
        $mainMenuButton.addEventListener('click', requestExperimentalWorldStart);
        updateMainMenuActionState();
        document.body.appendChild($mainMenu);
        return $mainMenu;
    }

    function showMainMenu() {
        hideBoot();
        ensureMainMenu().classList.add('show');
        recordStartup('main-menu:show', 'engineReady=' + engineMenuReady);
    }

    function hideMainMenu() {
        if ($mainMenu) {
            $mainMenu.classList.remove('show');
        }
    }

    function showPointerHint() {
        if ($hint) {
            $hint.classList.add('show');
        }
    }

    function hidePointerHint() {
        if ($hint) {
            $hint.classList.remove('show');
        }
    }

    function setBootStatus(text, progressPercent) {
        if (!$status || !text) {
            return;
        }
        if (latestBootStatus !== text) {
            latestBootStatus = text;
            $status.textContent = text;
        }

        if (typeof progressPercent === 'number' && $progressBar) {
            const nextProgress = Math.max(latestProgress, Math.min(100, progressPercent));
            if (nextProgress !== latestProgress) {
                latestProgress = nextProgress;
                $progressBar.style.width = latestProgress + '%';
            }
        }
    }

    function numberFromState(state, name) {
        const value = state ? Number(state[name] || 0) : 0;
        return Number.isFinite(value) ? value : 0;
    }

    function handleWebMcState(source, state) {
        window.__webmcLatestState = {
            source: source || '',
            state: state,
            at: Date.now()
        };

        if (!state || window.webmcBootMode !== 'mcMain') {
            return;
        }

        const visibleSections = numberFromState(state, 'visibleSections');
        const renderedSections = numberFromState(state, 'renderedSections');
        const requiredRenderedSections = numberFromState(state, 'requiredRenderedSections');
        const levelRenderUpdates = numberFromState(state, 'levelRenderUpdates');
        const presentCount = numberFromState(state, 'presentCount');
        const hasRequiredRenderedSections =
            requiredRenderedSections > 0 &&
            renderedSections >= requiredRenderedSections;
        const terrainReady =
            !!state.webTerrainReady ||
            (hasRequiredRenderedSections && !!state.hasRenderedAllSections);
        const playableTerrainReady =
            !!state.webTerrainReady ||
            hasRequiredRenderedSections;
        const hasPresentedTerrain =
            state.levelPresent &&
            state.playerPresent &&
            playableTerrainReady &&
            (source === 'levelRender' || levelRenderUpdates > 0 || presentCount > 0);

        if (hasPresentedTerrain) {
            if (!terrainReady) {
                setBootStatus('Rendering terrain...', 95);
            }
            hideMainMenu();
            hideBoot();
            if (document.pointerLockElement === $canvas) {
                hidePointerHint();
            } else {
                showPointerHint();
            }
            return;
        }

        if (
            state.gameLoadFinished &&
            !state.levelPresent &&
            !state.playerPresent &&
            state.screen === 'TitleScreen'
        ) {
            engineMenuReady = true;
            window.__webmcEngineMenuReady = true;
            window.__webmcEngineMenuReadyAt = window.__webmcEngineMenuReadyAt || Date.now();
            recordStartup('engine-menu:ready', 'source=' + (source || ''));
            updateMainMenuActionState();
            if (pendingWorldStartName) {
                showBoot();
                hideMainMenu();
                setBootStatus('Loading world...', 40);
                scheduleWorldStartRelease();
                return;
            }
            if (window.webmcAutoStartRequested) {
                showBoot();
                hideMainMenu();
                setBootStatus('Loading world...', 40);
                return;
            }
            setBootStatus('Main menu ready', 30);
            showMainMenu();
            return;
        }

        if (bootHidden && !window.webmcAutoStartRequested) {
            return;
        }

        if (state.levelPresent && state.playerPresent && renderedSections > 0) {
            const terrainProgress = requiredRenderedSections > 0 
                ? 60 + (renderedSections / requiredRenderedSections) * 30 
                : 60;
            setBootStatus('Building terrain... (' + renderedSections + '/' + (requiredRenderedSections || '?') + ')', terrainProgress);
            return;
        }

        if (state.levelPresent && state.playerPresent) {
            setBootStatus('Preparing terrain...', 55);
            return;
        }

        if (state.gameLoadFinished) {
            setBootStatus('Loading world...', 45);
            return;
        }

        if (visibleSections > 0 || source === 'levelRender') {
            setBootStatus('Building terrain...', 65);
            return;
        }

        setBootStatus('Loading Minecraft...', 20);
    }

    function inspectStateUrl(value) {
        try {
            const url = new URL(String(value), window.location.href);
            if (url.pathname !== '/__webmc_state') {
                return false;
            }
            const raw = url.searchParams.get('d');
            if (!raw) {
                return true;
            }
            handleWebMcState(url.searchParams.get('source') || '', JSON.parse(raw));
            return true;
        } catch (e) {
            // Some runtime beacons are intentionally truncated for URL size. The
            // authoritative same-page state is still available without parsing.
            if (window.__webmcState) {
                handleWebMcState('directStateFallback', window.__webmcState);
            }
            return true;
        }
    }

    function installStateBeaconObserver() {
        const descriptor = Object.getOwnPropertyDescriptor(HTMLImageElement.prototype, 'src');
        if (!descriptor || typeof descriptor.set !== 'function' || !descriptor.configurable) {
            return;
        }

        Object.defineProperty(HTMLImageElement.prototype, 'src', {
            configurable: true,
            enumerable: descriptor.enumerable,
            get: descriptor.get,
            set: function (value) {
                if (inspectStateUrl(value)) {
                    return;
                }
                return descriptor.set.call(this, value);
            }
        });
    }

    function installStatePollingFallback() {
        if (window.__webmcStatePollingInstalled) {
            return;
        }
        window.__webmcStatePollingInstalled = true;
        const poll = () => {
            if (window.webmcBootMode !== 'mcMain') {
                return;
            }
            if (window.__webmcState) {
                handleWebMcState('directStatePoll', window.__webmcState);
            }
            window.setTimeout(poll, engineMenuReady || window.webmcAutoStartRequested ? 1000 : 250);
        };
        window.setTimeout(poll, 250);
    }

    function fatal(msg) {
        hideMainMenu();
        hideBoot();
        if ($error) {
            $error.classList.add('show');
            $error.textContent = String(msg && msg.stack ? msg.stack : msg);
        }
        console.error(msg);
    }

    window.addEventListener('error', (e) => fatal(e.error || e.message || e));
    window.addEventListener('unhandledrejection', (e) => fatal(e.reason || e));
    document.addEventListener('pointerdown', tryUnlockWebAudio, { passive: true });
    document.addEventListener('keydown', tryUnlockWebAudio, { passive: true });
    document.addEventListener('pointerlockchange', () => {
        if (document.pointerLockElement === $canvas) {
            hidePointerHint();
        } else if (window.__webmcState && window.__webmcState.levelPresent && window.__webmcState.playerPresent) {
            showPointerHint();
        }
    });

    // Sanity: WebGL2 must be available before we hand off to game.js.
    // We don't acquire the context here — WebMain does that via @JSBody.
    if (!HTMLCanvasElement.prototype.getContext) {
        fatal('Canvas not supported.');
        return;
    }

    // ── Input Bridge: DOM Events → TeaVM InputBridge ────────────────────────
    function setupInputBridge() {
        if (typeof window.initialize !== 'function') {
            console.warn('[bootstrap] InputBridge.initialize not found, skipping input setup');
            return;
        }

        // Initialize Java side
        window.initialize();

        const canvas = $canvas;

        // Key events
        canvas.addEventListener('keydown', (e) => {
            if (window.queueKeyEvent) {
                window.queueKeyEvent(e.keyCode || e.which, 0, 1, getMods(e));
            }
        }, { passive: true });
        canvas.addEventListener('keyup', (e) => {
            if (window.queueKeyEvent) {
                window.queueKeyEvent(e.keyCode || e.which, 0, 0, getMods(e));
            }
        }, { passive: true });
        canvas.addEventListener('keypress', (e) => {
            if (window.queueCharEvent) {
                window.queueCharEvent(e.keyCode || e.charCode);
            }
        }, { passive: true });

        // Mouse button events
        canvas.addEventListener('mousedown', (e) => {
            if (window.queueMouseButtonEvent) {
                window.queueMouseButtonEvent(mapMouseButton(e.button), 1, getMods(e));
            }
        });
        canvas.addEventListener('mouseup', (e) => {
            if (window.queueMouseButtonEvent) {
                window.queueMouseButtonEvent(mapMouseButton(e.button), 0, getMods(e));
            }
        });

        // Mouse move events
        canvas.addEventListener('mousemove', (e) => {
            if (window.queueCursorPosEvent) {
                window.queueCursorPosEvent(e.clientX, e.clientY);
            }
        }, { passive: true });

        // Scroll events
        canvas.addEventListener('wheel', (e) => {
            if (window.queueScrollEvent) {
                e.preventDefault();
                window.queueScrollEvent(-Math.sign(e.deltaX), -Math.sign(e.deltaY));
            }
        }, { passive: false });

        // Focus events
        window.addEventListener('blur', () => {
            if (window.queueFocusEvent) window.queueFocusEvent(false);
        });
        window.addEventListener('focus', () => {
            if (window.queueFocusEvent) window.queueFocusEvent(true);
        });

        // Framebuffer resize
        function updateFramebufferSize() {
            if (window.queueFramebufferSizeEvent) {
                const dpr = window.devicePixelRatio || 1;
                window.queueFramebufferSizeEvent(
                    Math.floor(canvas.clientWidth * dpr),
                    Math.floor(canvas.clientHeight * dpr)
                );
            }
        }
        window.addEventListener('resize', updateFramebufferSize);
        updateFramebufferSize();

        console.log('[bootstrap] InputBridge initialized');
    }

    function getMods(e) {
        return (e.ctrlKey ? 2 : 0) | (e.shiftKey ? 1 : 0) | (e.altKey ? 4 : 0) | (e.metaKey ? 8 : 0);
    }

    function mapMouseButton(button) {
        // Maps DOM button to GLFW mouse button
        // 0=left, 1=middle, 2=right, 3=back, 4=forward
        switch (button) {
            case 0: return 0;  // left
            case 1: return 2;  // middle
            case 2: return 1;  // right
            case 3: return 3;  // back
            case 4: return 4;  // forward
            default: return button;
        }
    }

    if (window.webmcBootMode === 'mcMain') {
        ensureWebAudioState();
        installStateBeaconObserver();
        installStatePollingFallback();
        if (autoStartEnabled) {
            queueExperimentalWorldStart(pickWorldName());
        } else {
            setBootStatus('Main menu ready', 5);
            showMainMenu();
        }
    }

    function runTeaVmMain() {
        if (typeof window.main !== 'function') {
            fatal('game.js did not register window.main. Check teavm.js.moduleType / entryPointName.');
            return;
        }

        if (window.webmcBootMode !== 'mcMain') {
            setBootStatus('Starting (' + window.webmcBootMode + ')...', 15);
        } else if (window.webmcAutoStartRequested) {
            setBootStatus('Loading world...', 40);
        }

        // TeaVM main signature: main(args: string[], callback?: () => void)
        recordStartup('teavm-main:begin', 'boot=' + window.webmcBootMode);
        window.main([], function () {
            recordStartup('teavm-main:callback', 'boot=' + window.webmcBootMode);
            // Set up input bridge after TeaVM is initialized
            setupInputBridge();
            if (window.webmcBootMode !== 'mcMain') {
                setTimeout(hideBoot, 250);
            }
        });
    }

    async function preloadVfs() {
        // Check if VFS preload is needed and configured
        const vfsUrl = window.webmcVfsUrl || 'game.vfs';
        const vfsVersion = window.webmcVfsVersion || '1.21.8';

        if (!window.VfsLoader) {
            console.warn('[bootstrap] VfsLoader not available, skipping VFS preload');
            return;
        }

        try {
            recordStartup('vfs:preload:start', vfsUrl);
            setBootStatus('Loading assets...', 5);

            await window.VfsLoader.preloadToWebFs(vfsUrl, {
                version: vfsVersion,
                onProgress: (percent, status) => {
                    // Map VFS loading to 5-20% of overall progress
                    const mappedPercent = 5 + (percent * 0.15);
                    setBootStatus(status || 'Loading assets...', Math.floor(mappedPercent));
                }
            });

            recordStartup('vfs:preload:done', vfsUrl);
        } catch (err) {
            console.error('[bootstrap] VFS preload failed:', err);
            // Non-fatal: game.js will fall back to sync XHR in WebFs.preload()
        }
    }

    function loadGameScript() {
        const script = document.createElement('script');
        script.src = 'game.js?t=' + 1782803944862;
        script.async = true;
        setBootStatus('Loading game.js...', 20);
        recordStartup('game-js:request', script.src);
        script.onload = () => {
            recordStartup('game-js:load', script.src);
            setBootStatus('Starting game...', 25);
            runTeaVmMain();
        };
        script.onerror = () => fatal('Failed to load game.js.');
        document.head.appendChild(script);
    }

    async function initializeAndLoadGame() {
        // Preload VFS first if available
        if (window.VfsLoader && window.webmcEnableVfsPreload !== false) {
            try {
                await preloadVfs();
            } catch (err) {
                console.warn('[bootstrap] VFS preload failed, continuing with fallback:', err);
            }
        }

        // Load game.js
        loadGameScript();
    }

    recordStartup('game-js:schedule', 'raf=' + (typeof requestAnimationFrame === 'function'));
    if (typeof requestAnimationFrame === 'function') {
        requestAnimationFrame(() => setTimeout(initializeAndLoadGame, 0));
    } else {
        setTimeout(initializeAndLoadGame, 0);
    }
})();
