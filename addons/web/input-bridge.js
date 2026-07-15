// input-bridge.js - JavaScript side of InputBridge
// Bridges DOM events to TeaVM Java InputBridge

(function() {
    'use strict';

    // Input state tracking for polling methods (glfwGetKey, glfwGetMouseButton)
    var keyState = {};
    var mouseButtonState = {};

    // Security: Input validation constants
    var MAX_KEYCODE = 65535;
    var MAX_SCANCODE = 65535;
    var MAX_MOUSE_BUTTON = 7;
    var MAX_MODS = 0x001F;
    var MAX_COORD_VALUE = 1000000;
    var MAX_SCROLL_VALUE = 100;

    // Validate and clamp numeric input values
    function clampUint16(value) {
        if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
        return Math.max(0, Math.min(0xFFFF, Math.floor(value)));
    }

    function clampUint8(value) {
        if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
        return Math.max(0, Math.min(0xFF, Math.floor(value)));
    }

    function clampInt(value, min, max) {
        if (typeof value !== 'number' || !Number.isFinite(value)) return 0;
        return Math.max(min, Math.min(max, value));
    }

    // Check if InputBridge is available (will be exported by TeaVM)
    function ensureInputBridge() {
        if (typeof window.java_input_InputBridge === 'undefined') {
            // //console.warn('[input-bridge] InputBridge not yet available, retrying...');
            setTimeout(ensureInputBridge, 100);
            return false;
        }
        return true;
    }

    /**
     * Update key state - called on keydown/keyup
     * @param {number} key - The key code
     * @param {boolean} pressed - True if key is down, false if released
     */
    function updateKeyState(key, pressed) {
        var safeKey = clampUint16(key);
        keyState[safeKey] = !!pressed;
    }

    /**
     * Update mouse button state - called on mousedown/mouseup
     * @param {number} button - The mouse button (0=left, 1=middle, 2=right)
     * @param {boolean} pressed - True if button is down, false if released
     */
    function updateMouseButtonState(button, pressed) {
        var safeButton = clampUint8(button);
        mouseButtonState[safeButton] = !!pressed;
    }

    /**
     * Get the current state of a key (for polling methods)
     * @param {number} key - The key code
     * @returns {boolean} True if key is currently pressed
     */
    function getKeyState(key) {
        var safeKey = clampUint16(key);
        return !!keyState[safeKey];
    }

    /**
     * Get the current state of a mouse button (for polling methods)
     * @param {number} button - The mouse button (0=left, 1=middle, 2=right)
     * @returns {boolean} True if button is currently pressed
     */
    function getMouseButtonState(button) {
        var safeButton = clampUint8(button);
        return !!mouseButtonState[safeButton];
    }

    // Expose state functions globally for Java to poll
    window.__webmcInputBridge = {
        updateKeyState: updateKeyState,
        updateMouseButtonState: updateMouseButtonState,
        getKeyState: getKeyState,
        getMouseButtonState: getMouseButtonState
    };

    // Key event handler
    function handleKeyDown(e) {
        if (!window.java_input_InputBridge) return;
        var key = e.keyCode || e.which;
        // SECURITY: Validate keyCode range (GLFW supports 0-65535)
        if (key < 0 || key > 65535 || !Number.isFinite(key)) {
            return;
        }
        var scancode = e.location || 0;
        var action = 1; // GLFW_PRESS
        var mods = (e.ctrlKey ? 0x0002 : 0) | (e.shiftKey ? 0x0001 : 0) | (e.altKey ? 0x0004 : 0) | (e.metaKey ? 0x0008 : 0);
        // Update key state for polling methods
        updateKeyState(key, true);
        try {
            window.java_input_InputBridge.queueKeyEvent(key, scancode, action, mods);
        } catch(err) {
            console.error('[input-bridge] queueKeyEvent error:', err);
        }
    }

    function handleKeyUp(e) {
        if (!window.java_input_InputBridge) return;
        var key = e.keyCode || e.which;
        // SECURITY: Validate keyCode range (GLFW supports 0-65535)
        if (key < 0 || key > 65535 || !Number.isFinite(key)) {
            return;
        }
        var scancode = e.location || 0;
        var action = 0; // GLFW_RELEASE
        var mods = (e.ctrlKey ? 0x0002 : 0) | (e.shiftKey ? 0x0001 : 0) | (e.altKey ? 0x0004 : 0) | (e.metaKey ? 0x0008 : 0);
        // Update key state for polling methods
        updateKeyState(key, false);
        try {
            window.java_input_InputBridge.queueKeyEvent(key, scancode, action, mods);
        } catch(err) {
            console.error('[input-bridge] queueKeyEvent error:', err);
        }
    }

    function handleKeyPress(e) {
        if (!window.java_input_InputBridge) return;
        var codepoint = e.charCode || 0;
        // SECURITY: Validate Unicode codepoint range (0 to 0x10FFFF)
        if (codepoint < 0 || codepoint > 0x10FFFF || !Number.isFinite(codepoint)) {
            return;
        }
        if (codepoint > 0) {
            try {
                window.java_input_InputBridge.queueCharEvent(codepoint);
            } catch(err) {
                console.error('[input-bridge] queueCharEvent error:', err);
            }
        }
    }

    // Mouse button handler
    function handleMouseDown(e) {
        if (!window.java_input_InputBridge) return;
        var button = e.button; // 0=left, 1=middle, 2=right
        // SECURITY: Validate mouse button range (GLFW supports 0-7)
        if (button < 0 || button > 7 || !Number.isFinite(button)) {
            return;
        }
        var action = 1; // GLFW_PRESS
        var mods = (e.ctrlKey ? 0x0002 : 0) | (e.shiftKey ? 0x0001 : 0) | (e.altKey ? 0x0004 : 0) | (e.metaKey ? 0x0008 : 0);
        // Update mouse button state for polling methods
        updateMouseButtonState(button, true);
        try {
            window.java_input_InputBridge.queueMouseButtonEvent(button, action, mods);
        } catch(err) {
            console.error('[input-bridge] queueMouseButtonEvent error:', err);
        }
    }

    function handleMouseUp(e) {
        if (!window.java_input_InputBridge) return;
        var button = e.button;
        // SECURITY: Validate mouse button range (GLFW supports 0-7)
        if (button < 0 || button > 7 || !Number.isFinite(button)) {
            return;
        }
        var action = 0; // GLFW_RELEASE
        var mods = (e.ctrlKey ? 0x0002 : 0) | (e.shiftKey ? 0x0001 : 0) | (e.altKey ? 0x0004 : 0) | (e.metaKey ? 0x0008 : 0);
        // Update mouse button state for polling methods
        updateMouseButtonState(button, false);
        try {
            window.java_input_InputBridge.queueMouseButtonEvent(button, action, mods);
        } catch(err) {
            console.error('[input-bridge] queueMouseButtonEvent error:', err);
        }
    }

    // Mouse move handler
    function handleMouseMove(e) {
        if (!window.java_input_InputBridge) return;
        var canvas = document.getElementById('canvas') || document.getElementById('game-canvas');
        if (!canvas) return;

        var rect = canvas.getBoundingClientRect();
        var x = e.clientX - rect.left;
        var y = e.clientY - rect.top;

        // SECURITY: Validate cursor position bounds
        // Clamp to reasonable range to prevent overflow in Java side
        var maxCoord = 65536;
        if (!Number.isFinite(x) || !Number.isFinite(y) || x < -maxCoord || x > maxCoord || y < -maxCoord || y > maxCoord) {
            return;
        }

        // Scale by canvas size
        var scaleX = canvas.width / rect.width;
        var scaleY = canvas.height / rect.height;
        x *= scaleX;
        y *= scaleY;

        try {
            window.java_input_InputBridge.queueCursorPosEvent(x, y);
        } catch(err) {
            console.error('[input-bridge] queueCursorPosEvent error:', err);
        }
    }

    // Scroll handler
    function handleWheel(e) {
        if (!window.java_input_InputBridge) return;
        e.preventDefault();
        var xoffset = e.deltaX || 0;
        var yoffset = e.deltaY || 0;
        // SECURITY: Clamp scroll delta to prevent overflow
        var maxScroll = 100;
        if (!Number.isFinite(xoffset)) xoffset = 0;
        if (!Number.isFinite(yoffset)) yoffset = 0;
        xoffset = Math.max(-maxScroll, Math.min(maxScroll, xoffset));
        yoffset = Math.max(-maxScroll, Math.min(maxScroll, yoffset));
        try {
            window.java_input_InputBridge.queueScrollEvent(xoffset, -yoffset); // Negate for Minecraft convention
        } catch(err) {
            console.error('[input-bridge] queueScrollEvent error:', err);
        }
    }

    // Focus handler
    function handleFocus(e) {
        if (!window.java_input_InputBridge) return;
        try {
            window.java_input_InputBridge.queueFocusEvent(true);
        } catch(err) {
            console.error('[input-bridge] queueFocusEvent error:', err);
        }
    }

    function handleBlur(e) {
        if (!window.java_input_InputBridge) return;
        try {
            window.java_input_InputBridge.queueFocusEvent(false);
        } catch(err) {
            console.error('[input-bridge] queueFocusEvent error:', err);
        }
    }

    // Resize handler
    function handleResize(e) {
        if (!window.java_input_InputBridge) return;
        var canvas = document.getElementById('canvas') || document.getElementById('game-canvas');
        if (!canvas) return;
        try {
            window.java_input_InputBridge.queueFramebufferSizeEvent(canvas.width, canvas.height);
        } catch(err) {
            console.error('[input-bridge] queueFramebufferSizeEvent error:', err);
        }
    }

    // Install all event listeners
    function installEventListeners() {
        var canvas = document.getElementById('canvas') || document.getElementById('game-canvas');
        if (!canvas) {
            // //console.warn('[input-bridge] No canvas found, cannot install listeners');
            return;
        }

        // Keyboard events - attach to document for global capture
        document.addEventListener('keydown', handleKeyDown, { passive: true });
        document.addEventListener('keyup', handleKeyUp, { passive: true });
        document.addEventListener('keypress', handleKeyPress, { passive: true });

        // Mouse events - attach to canvas
        canvas.addEventListener('mousedown', handleMouseDown, { passive: false });
        canvas.addEventListener('mouseup', handleMouseUp, { passive: true });
        canvas.addEventListener('mousemove', handleMouseMove, { passive: true });

        // Wheel - must be non-passive to preventDefault
        canvas.addEventListener('wheel', handleWheel, { passive: false });

        // Focus events
        window.addEventListener('focus', handleFocus, { passive: true });
        window.addEventListener('blur', handleBlur, { passive: true });

        // Resize events
        window.addEventListener('resize', handleResize, { passive: true });

        // //console.log('[input-bridge] Event listeners installed');
    }

    // Wait for DOM and TeaVM to be ready
    function init() {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', onReady);
        } else {
            onReady();
        }
    }

    function onReady() {
        // Wait for InputBridge to be exported by TeaVM
        function waitForBridge() {
            if (typeof window.java_input_InputBridge !== 'undefined') {
                installEventListeners();
            } else {
                setTimeout(waitForBridge, 50);
            }
        }
        waitForBridge();
    }

    // Export for manual initialization if needed
    window.__webmcInputBridgeInit = init;
    init();

})();