// bootstrap.js — page-side glue for the TeaVM build.
//
// TeaVM with moduleType=NONE produces a non-module IIFE that registers the
// entry point as a global function (default name `main`). It expects to be
// invoked with `main(args, callback)`. We just include game.js via a
// classic <script> tag (set in index.html), then call window.main here.

(function () {
    // ── Protect BigInt.asIntN / asUintN from NaN / Infinity ────────────────────
    // TeaVM's Long_fromNumber guards BigInt() directly, but callers also invoke
    // BigInt.asIntN / asUintN with NaN from Math.min/max results.  Patch both here
    // so the browser never sees "cannot convert NaN to BigInt".
    (function () {
        var OrigAsIntN = BigInt.asIntN;
        var OrigAsUintN = BigInt.asUintN;
        BigInt.asIntN = function (bits, val) {
            if (val !== val || !Number.isFinite(val)) { return OrigAsIntN(bits, BigInt(0)); }
            try { return OrigAsIntN(bits, val); } catch (e) { return OrigAsIntN(bits, BigInt(0)); }
        };
        BigInt.asUintN = function (bits, val) {
            if (val !== val || !Number.isFinite(val)) { return OrigAsUintN(bits, BigInt(0)); }
            try { return OrigAsUintN(bits, val); } catch (e) { return OrigAsUintN(bits, BigInt(0)); }
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
            '#webmc-main-menu .menu-panel{display:flex;flex-direction:column;align-items:center;gap:12px;width:min(360px,80vw);}',
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
        $mainMenu.innerHTML = '<div class="menu-panel"><div class="title">Minecraft</div><div class="menu-status"></div><button type="button" id="btn-singleplayer">Singleplayer</button><button type="button" id="btn-multiplayer">Multiplayer</button><button type="button" id="btn-resourcepacks">Resource Packs</button></div>';
        $mainMenuButton = $mainMenu.querySelector('#btn-singleplayer');
        $mainMenuStatus = $mainMenu.querySelector('.menu-status');
        $mainMenuButton.addEventListener('click', requestExperimentalWorldStart);
        // Multiplayer button
        var $multiplayerBtn = $mainMenu.querySelector('#btn-multiplayer');
        if ($multiplayerBtn) {
            $multiplayerBtn.addEventListener('click', function() {
                if (window.WebMCMultiplayer) {
                    window.WebMCMultiplayer.showMenu();
                } else {
                    console.warn('[bootstrap] Multiplayer UI not available');
                }
            });
        }
        // Resource Packs button
        var $rpBtn = $mainMenu.querySelector('#btn-resourcepacks');
        if ($rpBtn) {
            $rpBtn.addEventListener('click', function() {
                showResourcePackMenu();
            });
        }
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

    // ── Chat System ────────────────────────────────────────────────────────────
    const MAX_CHAT_MESSAGES = 100;
    const MAX_VISIBLE_LINES = 8;
    let chatMessages = [];
    let chatHistory = []; // Full history for scrollback
    let chatVisible = false;
    let chatInputVisible = false;
    let chatContainer = null;
    let chatMessagesEl = null;
    let chatInputContainer = null;
    let chatInputEl = null;
    let chatHintEl = null;
    let chatInputFocused = false;
    let lastChatVisibility = false;
    let chatInitialized = false;

    // Chat player name colors
    const PLAYER_COLORS = [
        '#4ade80', '#60a5fa', '#f472b6', '#fbbf24',
        '#a78bfa', '#34d399', '#f87171', '#38bdf8'
    ];

    function getPlayerColor(name) {
        if (!name) return '#ffffff';
        let hash = 0;
        for (let i = 0; i < name.length; i++) {
            hash = ((hash << 5) - hash) + name.charCodeAt(i);
            hash = hash & hash;
        }
        return PLAYER_COLORS[Math.abs(hash) % PLAYER_COLORS.length];
    }

    function initChatUI() {
        if (chatInitialized) return;
        chatInitialized = true;

        chatContainer = document.getElementById('chat-container');
        chatMessagesEl = document.getElementById('chat-messages');
        chatInputContainer = document.getElementById('chat-input-container');
        chatInputEl = document.getElementById('chat-input');
        chatHintEl = document.createElement('div');
        chatHintEl.id = 'chat-hint';
        chatHintEl.textContent = 'Press T to chat';
        if (chatContainer) {
            chatContainer.appendChild(chatHintEl);
        }

        // Set up input event handlers
        if (chatInputEl) {
            chatInputEl.addEventListener('input', function() {
                // Auto-resize or other input handling
            });
        }

        console.log('[bootstrap] Chat UI initialized');
    }

    function addChatMessage(text, type, sender) {
        if (!text) return;

        type = type || 'chat';
        const timestamp = Date.now();
        const message = { text, type, sender, time: timestamp };

        // Add to messages array
        chatMessages.push(message);
        chatHistory.push(message);

        // Trim old messages
        if (chatMessages.length > MAX_CHAT_MESSAGES) {
            chatMessages = chatMessages.slice(-MAX_CHAT_MESSAGES);
        }
        if (chatHistory.length > MAX_CHAT_MESSAGES) {
            chatHistory = chatHistory.slice(-MAX_CHAT_MESSAGES);
        }

        renderChatMessages();

        // Log for debugging
        if (type !== 'sent') {
            console.log('[Chat] ' + (sender ? '<' + sender + '> ' : '') + text);
        }
    }

    function renderChatMessages() {
        if (!chatMessagesEl) return;

        // Only show recent messages
        const visibleMessages = chatMessages.slice(-MAX_VISIBLE_LINES);
        chatMessagesEl.innerHTML = '';

        visibleMessages.forEach(function(msg) {
            const div = document.createElement('div');
            div.className = 'chat-message ' + (msg.type || 'chat');

            // Add timestamp for older messages
            if (msg.time && Date.now() - msg.time > 60000) {
                const timeSpan = document.createElement('span');
                timeSpan.className = 'timestamp';
                const date = new Date(msg.time);
                timeSpan.textContent = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' ';
                div.appendChild(timeSpan);
            }

            if (msg.sender) {
                const senderSpan = document.createElement('span');
                senderSpan.className = 'sender';
                senderSpan.textContent = msg.sender + ': ';
                senderSpan.style.color = getPlayerColor(msg.sender);
                div.appendChild(senderSpan);
            }

            // Handle formatted text
            const textNode = document.createTextNode(msg.text);
            div.appendChild(textNode);
            chatMessagesEl.appendChild(div);
        });

        // Scroll to bottom
        chatMessagesEl.scrollTop = chatMessagesEl.scrollHeight;
    }

    function showChat() {
        if (!chatContainer) return;
        chatContainer.classList.remove('hidden');
        chatVisible = true;
        lastChatVisibility = true;
    }

    function hideChat() {
        if (!chatContainer) return;
        chatContainer.classList.add('hidden');
        chatVisible = false;
    }

    function showChatInput() {
        if (!chatInputContainer || !chatInputEl) return;
        chatInputVisible = true;
        chatInputContainer.classList.add('visible');
        chatInputEl.value = '';
        chatInputEl.focus();

        // Show chat container
        showChat();

        // Request pointer lock release for text input
        if (document.pointerLockElement) {
            document.exitPointerLock();
        }
    }

    function hideChatInput() {
        if (!chatInputContainer) return;
        chatInputVisible = false;
        chatInputContainer.classList.remove('visible');
        if (chatInputEl) {
            chatInputEl.blur();
        }
    }

    function sendChatMessage(text) {
        text = (text || '').trim();
        if (!text) return;

        // Send to Java side via the global bridge
        if (window.__webmcSendChatMessage) {
            window.__webmcSendChatMessage(text);
        } else {
            console.warn('[bootstrap] __webmcSendChatMessage not available, simulating send');
        }

        // Echo the message locally (will be replaced by server echo in multiplayer)
        addChatMessage(text, 'sent');
        hideChatInput();
    }

    function handleChatKeyDown(e) {
        // T or / key opens chat
        if ((e.key === 't' || e.key === 'T' || e.key === '/') && !chatInputVisible) {
            if (!chatInputFocused) {
                // Only if not in another text field and game is loaded
                if (window.__webmcState && window.__webmcState.levelPresent) {
                    showChat();
                    showChatInput();
                    // If / was pressed, add it to input
                    if (e.key === '/') {
                        setTimeout(function() {
                            if (chatInputEl) chatInputEl.value = '/';
                        }, 0);
                    }
                    e.preventDefault();
                    return;
                }
            }
        }

        // Escape closes chat
        if (e.key === 'Escape') {
            if (chatInputVisible) {
                hideChatInput();
                e.preventDefault();
            }
        }

        // Enter sends chat
        if (e.key === 'Enter' && chatInputVisible) {
            if (chatInputEl) {
                sendChatMessage(chatInputEl.value);
            }
            e.preventDefault();
        }
    }

    function handleChatFocus() {
        chatInputFocused = true;
    }

    function handleChatBlur() {
        chatInputFocused = false;
        setTimeout(function() {
            if (!chatInputFocused && chatInputEl && !chatInputEl.matches(':focus')) {
                // Keep chat visible but hide input
            }
        }, 100);
    }

    // Install chat keyboard handler
    function installChatHandler() {
        if (window.__webmcChatHandlerInstalled) return;
        window.__webmcChatHandlerInstalled = true;

        document.addEventListener('keydown', handleChatKeyDown, { capture: true });

        // Also handle clicking on chat input
        if (chatInputEl) {
            chatInputEl.addEventListener('focus', handleChatFocus);
            chatInputEl.addEventListener('blur', handleChatBlur);
        }

        // Handle canvas click to release chat focus
        document.addEventListener('click', function(e) {
            if (chatInputVisible && e.target === document.getElementById('game-canvas')) {
                hideChatInput();
            }
        });

        console.log('[bootstrap] Chat handler installed');
    }

    // Expose chat API for Java side
    window.__webmcAddChatMessage = addChatMessage;
    window.__webmcShowChat = showChat;
    window.__webmcHideChat = hideChat;
    window.__webmcGetChatHistory = function() { return chatHistory; };

    // Handle outgoing chat messages from Java bridge
    window.__webmcHandleOutgoingChat = function(msg) {
        msg = (msg || '').trim();
        if (!msg) return;

        // Check if this is a command
        if (msg.startsWith('/')) {
            console.log('[bootstrap] Command (via chat UI):', msg);
            // Commands are handled by the game engine
        } else {
            console.log('[bootstrap] Chat message to send:', msg);
            // Send via MultiplayerManager if connected
            if (window.SocketRedirect && window.SocketRedirect._ws) {
                SocketRedirect.sendChat(msg);
            } else {
                // Local message - add to chat directly
                addChatMessage(msg, 'sent');
            }
        }
    };

    // ── Chat System End ────────────────────────────────────────────────────────

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

        // Handle resource reload progress - show detailed loading status
        if (state.reloadStateAt && !state.gameLoadFinished) {
            const reloadProgress = calculateReloadProgress(state);
            if (reloadProgress >= 0) {
                const listenerName = state.reloadListener ? state.reloadListener.split('.').pop() : 'preparing';
                const phase = state.reloadPhase || 'preparing';
                setBootStatus('Loading: ' + phase + ' (' + listenerName + ')', 5 + Math.floor(reloadProgress * 25));
                return;
            }
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

            // Initialize chat system when world is loaded
            if (!window.__webmcChatInitialized) {
                window.__webmcChatInitialized = true;
                // Register the chat bridge for JavaScript -> Java communication
                window.webmcChatBridge = {
                    onOutgoingChatMessage: function(msg) {
                        if (window.__webmcHandleOutgoingChat) {
                            window.__webmcHandleOutgoingChat(msg);
                        } else {
                            console.warn('[bootstrap] Chat bridge not ready for:', msg);
                        }
                    }
                };
                // Show chat hint after a short delay
                setTimeout(function() {
                    if (chatHintEl) chatHintEl.style.display = 'block';
                }, 2000);
            }

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

    function calculateReloadProgress(state) {
        // Resource reload progress calculation based on SimpleReloadInstance tracking
        if (!state.reloadListenerCount || state.reloadListenerCount <= 0) {
            return -1;
        }

        const totalListeners = state.reloadListenerCount;
        const preparingCount = state.reloadPreparingCount || 0;
        const finishedReloads = state.reloadFinishedReloads || 0;

        // Calculate progress: preparation phase (50%) + apply phase (50%)
        const prepProgress = Math.max(0, totalListeners - preparingCount) / totalListeners;
        const applyProgress = Math.min(1, finishedReloads / totalListeners);

        return (prepProgress * 0.5) + (applyProgress * 0.5);
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

    if (window.webmcBootMode === 'mcMain') {
        ensureWebAudioState();
        initChatUI();
        installChatHandler();
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
        script.src = 'game.js';
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

    // ── Resource Pack Menu System ─────────────────────────────────────────────
    let resourcePackMenuInitialized = false;
    let $resourcePackMenu = null;
    let $packList = null;
    let $rpProgress = null;
    let $rpProgressBar = null;
    let $rpProgressStatus = null;

    function initResourcePackMenu() {
        if (resourcePackMenuInitialized) return;
        resourcePackMenuInitialized = true;

        $resourcePackMenu = document.getElementById('resourcepack-menu');
        $packList = document.getElementById('pack-list');
        $rpProgress = document.getElementById('rp-progress');
        $rpProgressBar = $rpProgress ? $rpProgress.querySelector('.bar div') : null;
        $rpProgressStatus = $rpProgress ? $rpProgress.querySelector('.status') : null;

        // Close button
        var $closeBtn = document.getElementById('rp-close');
        if ($closeBtn) {
            $closeBtn.addEventListener('click', hideResourcePackMenu);
        }

        // Add local pack button
        var $addLocalBtn = document.getElementById('rp-add-local');
        var $fileInput = document.getElementById('rp-file-input');
        if ($addLocalBtn && $fileInput) {
            $addLocalBtn.addEventListener('click', function() {
                $fileInput.click();
            });
            $fileInput.addEventListener('change', function(e) {
                if (e.target.files && e.target.files.length > 0) {
                    handleAddLocalPack(e.target.files[0]);
                    e.target.value = ''; // Reset for re-selection
                }
            });
        }

        // Add URL button
        var $addUrlBtn = document.getElementById('rp-add-url');
        var $urlInput = document.getElementById('rp-url-input');
        if ($addUrlBtn && $urlInput) {
            $addUrlBtn.addEventListener('click', function() {
                var url = $urlInput.value.trim();
                if (url) {
                    handleAddRemotePack(url);
                    $urlInput.value = '';
                }
            });
            $urlInput.addEventListener('keydown', function(e) {
                if (e.key === 'Enter') {
                    var url = $urlInput.value.trim();
                    if (url) {
                        handleAddRemotePack(url);
                        $urlInput.value = '';
                    }
                }
            });
        }

        // Reload button
        var $reloadBtn = document.getElementById('rp-reload');
        if ($reloadBtn) {
            $reloadBtn.addEventListener('click', handleReloadPacks);
        }

        // Clear all button
        var $clearBtn = document.getElementById('rp-clear');
        if ($clearBtn) {
            $clearBtn.addEventListener('click', handleClearAllPacks);
        }

        // Set up ResourcePackBridge callbacks
        if (typeof ResourcePackBridge !== 'undefined') {
            ResourcePackBridge.onPackAdded(function(pack) {
                renderPackList();
                showResourcePackProgress(50, 'Pack added: ' + pack.name);
                setTimeout(function() { hideResourcePackProgress(); }, 1500);
            });

            ResourcePackBridge.onPackRemoved(function(pack) {
                renderPackList();
            });

            ResourcePackBridge.onPackLoaded(function(pack, err) {
                renderPackList();
                if (!err) {
                    showResourcePackProgress(100, 'Pack loaded: ' + pack.name);
                    setTimeout(function() { hideResourcePackProgress(); }, 1000);
                }
            });

            ResourcePackBridge.onProgress(function(percent, status) {
                showResourcePackProgress(percent, status);
            });

            ResourcePackBridge.onError(function(err) {
                console.error('[ResourcePackBridge UI]', err);
                showResourcePackProgress(0, 'Error: ' + err);
                setTimeout(function() { hideResourcePackProgress(); }, 3000);
            });
        }

        // Initial render
        renderPackList();
    }

    function showResourcePackMenu() {
        if (!resourcePackMenuInitialized) {
            initResourcePackMenu();
        }
        if ($resourcePackMenu) {
            renderPackList();
            $resourcePackMenu.classList.add('show');
        }
    }

    function hideResourcePackMenu() {
        if ($resourcePackMenu) {
            $resourcePackMenu.classList.remove('show');
        }
    }

    function renderPackList() {
        if (!$packList) return;

        var packs = [];
        if (typeof ResourcePackBridge !== 'undefined') {
            packs = ResourcePackBridge.getPacks();
        }

        $packList.innerHTML = '';

        packs.forEach(function(pack) {
            var li = document.createElement('li');
            li.className = 'pack-item';
            if (!pack.enabled) li.classList.add('disabled');
            li.dataset.packId = pack.id;

            // Drag handle
            var dragHandle = document.createElement('span');
            dragHandle.className = 'drag-handle';
            dragHandle.textContent = '☰'; // Hamburger icon
            dragHandle.title = 'Drag to reorder';
            li.appendChild(dragHandle);

            // Checkbox
            var checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.checked = pack.enabled;
            checkbox.title = 'Enable/disable pack';
            checkbox.addEventListener('change', function() {
                handleTogglePack(pack.id, checkbox.checked);
            });
            li.appendChild(checkbox);

            // Pack icon
            var icon = document.createElement('div');
            icon.className = 'pack-icon';
            icon.textContent = pack.sourceType === 'local' ? '\u{1F4C1}' : '\u{1F310}';
            li.appendChild(icon);

            // Pack info
            var info = document.createElement('div');
            info.className = 'pack-info';

            var name = document.createElement('div');
            name.className = 'pack-name';
            name.textContent = pack.name;
            name.title = pack.name;
            info.appendChild(name);

            var meta = document.createElement('div');
            meta.className = 'pack-meta';

            // Pack format
            var format = document.createElement('span');
            format.textContent = 'v' + (pack.packFormat || '?');
            if (!pack.compatible) {
                format.className = 'error';
                format.title = 'May not be compatible with this version';
            }
            meta.appendChild(format);

            // File size
            if (pack.fileSizeFormatted) {
                var size = document.createElement('span');
                size.textContent = pack.fileSizeFormatted;
                meta.appendChild(size);
            }

            // Status
            if (pack.loaded) {
                var status = document.createElement('span');
                status.textContent = 'Loaded';
                status.style.color = '#4ade80';
                meta.appendChild(status);
            } else if (pack.error) {
                var error = document.createElement('span');
                error.className = 'error';
                error.textContent = 'Error';
                error.title = pack.error;
                meta.appendChild(error);
            }

            // Source type
            var source = document.createElement('span');
            source.textContent = pack.sourceType === 'local' ? 'Local' : 'Remote';
            meta.appendChild(source);

            info.appendChild(meta);
            li.appendChild(info);

            // Actions
            var actions = document.createElement('div');
            actions.className = 'pack-actions';

            var removeBtn = document.createElement('button');
            removeBtn.className = 'danger';
            removeBtn.textContent = 'Remove';
            removeBtn.title = 'Remove this pack';
            removeBtn.addEventListener('click', function() {
                handleRemovePack(pack.id);
            });
            actions.appendChild(removeBtn);

            li.appendChild(actions);

            // Drag and drop for reordering
            makePackItemDraggable(li);

            $packList.appendChild(li);
        });

        if (packs.length === 0) {
            var empty = document.createElement('li');
            empty.className = 'pack-item';
            empty.style.justifyContent = 'center';
            empty.style.color = '#888';
            empty.textContent = 'No resource packs added';
            $packList.appendChild(empty);
        }
    }

    function makePackItemDraggable(li) {
        var dragHandle = li.querySelector('.drag-handle');
        if (!dragHandle) return;

        var startY, startIndex;

        dragHandle.addEventListener('mousedown', function(e) {
            e.preventDefault();
            startY = e.clientY;
            startIndex = Array.from($packList.children).indexOf(li);
            li.style.opacity = '0.5';
            document.body.style.cursor = 'grabbing';

            function onMouseMove(e) {
                var deltaY = e.clientY - startY;
                var items = Array.from($packList.children).filter(function(c) {
                    return c.classList.contains('pack-item');
                });

                var currentIndex = Array.from($packList.children).indexOf(li);
                var newIndex = currentIndex;

                // Find new position based on mouse movement
                for (var i = 0; i < items.length; i++) {
                    var rect = items[i].getBoundingClientRect();
                    var midY = rect.top + rect.height / 2;
                    if (e.clientY < midY && i < currentIndex) {
                        newIndex = i;
                    } else if (e.clientY > midY && i > currentIndex) {
                        newIndex = i;
                    }
                }

                if (newIndex !== currentIndex && newIndex >= 0) {
                    // Move DOM element
                    var targetItem = items[newIndex];
                    if (newIndex > currentIndex) {
                        $packList.insertBefore(li, targetItem.nextSibling);
                    } else {
                        $packList.insertBefore(li, targetItem);
                    }
                }
            }

            function onMouseUp() {
                li.style.opacity = '';
                document.body.style.cursor = '';

                // Update pack order in bridge
                var items = Array.from($packList.children).filter(function(c) {
                    return c.classList.contains('pack-item');
                });
                var newOrder = items.map(function(item) { return item.dataset.packId; });

                // Apply new order to bridge
                if (typeof ResourcePackBridge !== 'undefined') {
                    for (var i = 0; i < newOrder.length; i++) {
                        var currentPos = -1;
                        var packs = ResourcePackBridge.getPacks();
                        for (var j = 0; j < packs.length; j++) {
                            if (packs[j].id === newOrder[i]) {
                                currentPos = j;
                                break;
                            }
                        }
                        if (currentPos !== i && currentPos >= 0) {
                            ResourcePackBridge.movePack(newOrder[i], i);
                        }
                    }
                }

                document.removeEventListener('mousemove', onMouseMove);
                document.removeEventListener('mouseup', onMouseUp);
            }

            document.addEventListener('mousemove', onMouseMove);
            document.addEventListener('mouseup', onMouseUp);
        });
    }

    function handleAddLocalPack(file) {
        if (typeof ResourcePackBridge !== 'undefined') {
            showResourcePackProgress(0, 'Adding ' + file.name + '...');
            ResourcePackBridge.addLocalPack(file, function(err, pack) {
                if (err) {
                    showResourcePackProgress(0, 'Error: ' + err);
                    setTimeout(function() { hideResourcePackProgress(); }, 3000);
                } else {
                    showResourcePackProgress(100, 'Pack added!');
                    setTimeout(function() { hideResourcePackProgress(); }, 1500);
                }
            });
        }
    }

    function handleAddRemotePack(url) {
        if (typeof ResourcePackBridge !== 'undefined') {
            var name = url.split('/').pop().replace(/\.(zip|mcpack)$/i, '') || 'Remote Pack';
            showResourcePackProgress(0, 'Adding remote pack...');
            ResourcePackBridge.addRemotePack(name, url, function(err, pack) {
                if (err) {
                    showResourcePackProgress(0, 'Error: ' + err);
                    setTimeout(function() { hideResourcePackProgress(); }, 3000);
                } else {
                    showResourcePackProgress(100, 'Pack added!');
                    setTimeout(function() { hideResourcePackProgress(); }, 1500);
                }
            });
        }
    }

    function handleRemovePack(packId) {
        if (typeof ResourcePackBridge !== 'undefined') {
            ResourcePackBridge.removePack(packId);
        }
    }

    function handleTogglePack(packId, enabled) {
        if (typeof ResourcePackBridge !== 'undefined') {
            ResourcePackBridge.setPackEnabled(packId, enabled);
            renderPackList();
        }
    }

    function handleReloadPacks() {
        if (typeof ResourcePackBridge !== 'undefined') {
            showResourcePackProgress(0, 'Reloading packs...');
            ResourcePackBridge.reload(function(err) {
                if (err) {
                    showResourcePackProgress(0, 'Error: ' + err);
                    setTimeout(function() { hideResourcePackProgress(); }, 3000);
                } else {
                    showResourcePackProgress(100, 'All packs reloaded!');
                    setTimeout(function() { hideResourcePackProgress(); }, 1500);
                }
            });
        }
    }

    function handleClearAllPacks() {
        if (confirm('Remove all resource packs?')) {
            if (typeof ResourcePackBridge !== 'undefined') {
                var packs = ResourcePackBridge.getPacks();
                for (var i = 0; i < packs.length; i++) {
                    ResourcePackBridge.removePack(packs[i].id);
                }
            }
        }
    }

    function showResourcePackProgress(percent, status) {
        if ($rpProgress) {
            $rpProgress.classList.add('show');
            if ($rpProgressBar) {
                $rpProgressBar.style.width = Math.max(0, Math.min(100, percent)) + '%';
            }
            if ($rpProgressStatus) {
                $rpProgressStatus.textContent = status || '';
            }
        }
    }

    function hideResourcePackProgress() {
        if ($rpProgress) {
            $rpProgress.classList.remove('show');
        }
    }

    // Expose resource pack menu API for Java side
    window.__webmcShowResourcePackMenu = showResourcePackMenu;
    window.__webmcHideResourcePackMenu = hideResourcePackMenu;

    // ── Resource Pack Menu System End ─────────────────────────────────────────
    // Provides JavaScript-side VFS write capability for resource packs

    /**
     * Write bytes to the VFS at a given path.
     * This bridges to the TeaVM Java side via the Game global.
     */
    window.writeVfsFile = function(path, bytes) {
        if (typeof Game !== 'undefined' && typeof Game.writeVfsFile === 'function') {
            return Game.writeVfsFile(path, bytes);
        }
        console.warn('[bootstrap] writeVfsFile: Game.writeVfsFile not available');
        return false;
    };

    /**
     * Read bytes from VFS at a given path.
     */
    window.readVfsFile = function(path, callback) {
        if (typeof Game !== 'undefined' && typeof Game.readVfsFile === 'function') {
            return Game.readVfsFile(path, callback);
        }
        console.warn('[bootstrap] readVfsFile: Game.readVfsFile not available');
        if (callback) callback(null, 'VFS not available');
        return false;
    };

    /**
     * Check if a path exists in VFS.
     */
    window.vfsExists = function(path) {
        if (typeof Game !== 'undefined' && typeof Game.vfsExists === 'function') {
            return Game.vfsExists(path);
        }
        return false;
    };

    /**
     * Get resource pack loading status for UI.
     */
    window.getResourcePackStatus = function() {
        if (typeof ResourcePackBridge !== 'undefined') {
            return {
                packCount: ResourcePackBridge.getPacks().length,
                enabled: ResourcePackBridge._enabled,
                ready: true
            };
        }
        return {
            packCount: 0,
            enabled: false,
            ready: false
        };
    };

    // ── Resource Pack VFS Bridge End ─────────────────────────────────────────
})();
