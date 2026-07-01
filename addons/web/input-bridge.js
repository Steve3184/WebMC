// input-bridge.js - JavaScript side of InputBridge
// Bridges DOM events to TeaVM Java InputBridge

(function() {
    'use strict';

    // Check if InputBridge is available (will be exported by TeaVM)
    function ensureInputBridge() {
        if (typeof window.java_input_InputBridge === 'undefined') {
            console.warn('[input-bridge] InputBridge not yet available, retrying...');
            setTimeout(ensureInputBridge, 100);
            return false;
        }
        return true;
    }

    // Key event handler
    function handleKeyDown(e) {
        if (!window.java_input_InputBridge) return;
        var key = e.keyCode || e.which;
        var scancode = e.location || 0;
        var action = 1; // GLFW_PRESS
        var mods = (e.ctrlKey ? 0x0002 : 0) | (e.shiftKey ? 0x0001 : 0) | (e.altKey ? 0x0004 : 0) | (e.metaKey ? 0x0008 : 0);
        try {
            window.java_input_InputBridge.queueKeyEvent(key, scancode, action, mods);
        } catch(err) {
            console.error('[input-bridge] queueKeyEvent error:', err);
        }
    }

    function handleKeyUp(e) {
        if (!window.java_input_InputBridge) return;
        var key = e.keyCode || e.which;
        var scancode = e.location || 0;
        var action = 0; // GLFW_RELEASE
        var mods = (e.ctrlKey ? 0x0002 : 0) | (e.shiftKey ? 0x0001 : 0) | (e.altKey ? 0x0004 : 0) | (e.metaKey ? 0x0008 : 0);
        try {
            window.java_input_InputBridge.queueKeyEvent(key, scancode, action, mods);
        } catch(err) {
            console.error('[input-bridge] queueKeyEvent error:', err);
        }
    }

    function handleKeyPress(e) {
        if (!window.java_input_InputBridge) return;
        var codepoint = e.charCode || 0;
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
        var action = 1; // GLFW_PRESS
        var mods = (e.ctrlKey ? 0x0002 : 0) | (e.shiftKey ? 0x0001 : 0) | (e.altKey ? 0x0004 : 0) | (e.metaKey ? 0x0008 : 0);
        try {
            window.java_input_InputBridge.queueMouseButtonEvent(button, action, mods);
        } catch(err) {
            console.error('[input-bridge] queueMouseButtonEvent error:', err);
        }
    }

    function handleMouseUp(e) {
        if (!window.java_input_InputBridge) return;
        var button = e.button;
        var action = 0; // GLFW_RELEASE
        var mods = (e.ctrlKey ? 0x0002 : 0) | (e.shiftKey ? 0x0001 : 0) | (e.altKey ? 0x0004 : 0) | (e.metaKey ? 0x0008 : 0);
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
            console.warn('[input-bridge] No canvas found, cannot install listeners');
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

        console.log('[input-bridge] Event listeners installed');
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