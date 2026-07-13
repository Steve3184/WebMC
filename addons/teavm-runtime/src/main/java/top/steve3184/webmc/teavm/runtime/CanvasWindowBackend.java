package top.steve3184.webmc.teavm.runtime;

import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import top.steve3184.webmc.teavm.glfw.WindowBackend;

public final class CanvasWindowBackend implements WindowBackend {
    private long nextHandle = 1L;
    private double startTime;
    private boolean shouldClose;
    private final Map<Long, Callbacks> windows = new HashMap<>();
    private HTMLCanvasElement canvas;
    private boolean domListenersRegistered = false;

    // Mouse state tracking
    private double cursorX = 0.0;
    private double cursorY = 0.0;
    private final boolean[] mouseButtons = new boolean[8];
    // Key state tracking - supports up to 512 key codes (matches GLFW range)
    private final boolean[] keyStates = new boolean[512];

    // Focus state
    private boolean hasFocus = false;

    private static final class Callbacks {
        GLFWKeyCallback key;
        GLFWCharCallback character;
        GLFWMouseButtonCallback mouseButton;
        GLFWCursorPosCallback cursorPos;
        GLFWCursorPosCallback cursorEnter;
        GLFWScrollCallback scroll;
        GLFWFramebufferSizeCallback framebufferSize;
        // I-interface forms (used by MC)
        org.lwjgl.glfw.GLFWKeyCallbackI keyI;
        org.lwjgl.glfw.GLFWCharCallbackI characterI;
        org.lwjgl.glfw.GLFWMouseButtonCallbackI mouseButtonI;
        org.lwjgl.glfw.GLFWCursorPosCallbackI cursorPosI;
        org.lwjgl.glfw.GLFWScrollCallbackI scrollI;
        org.lwjgl.glfw.GLFWFramebufferSizeCallbackI framebufferSizeI;
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public void terminate() {
        if (canvas != null && domListenersRegistered) {
            removeEventListener(canvas, "keydown", keyDownListener);
            removeEventListener(canvas, "keyup", keyUpListener);
            removeEventListener(canvas, "keypress", keyPressListener);
            removeEventListener(canvas, "mousedown", mouseDownListener);
            removeEventListener(canvas, "mouseup", mouseUpListener);
            removeEventListener(canvas, "mousemove", mouseMoveListener);
            removeEventListener(canvas, "wheel", wheelListener);
            removeWindowEventListener("blur", blurListener);
            removeWindowEventListener("focus", focusListener);
            removeWindowEventListener("resize", resizeListener);
            domListenersRegistered = false;
        }
        windows.clear();
    }

    @Override
    public double time() {
        return System.nanoTime() / 1_000_000_000.0 - startTime;
    }

    @Override
    public void setTime(double time) {
        startTime = System.nanoTime() / 1_000_000_000.0 - time;
    }

    @Override
    public long createWindow(int width, int height, String title) {
        long handle = nextHandle++;
        windows.put(handle, new Callbacks());

        // Register DOM event listeners on first window creation
        if (!domListenersRegistered) {
            registerDomListeners();
            domListenersRegistered = true;
        }

        return handle;
    }

    private void registerDomListeners() {
        // Get canvas element
        canvas = getCanvasElement();
        if (canvas == null) {
            log("CanvasWindowBackend: canvas element not found");
            return;
        }

        // Enable tab focus so canvas can receive keyboard events
        setCanvasTabIndex(canvas, 0);

        // Make canvas focusable for keyboard input
        addEventListener(canvas, "keydown", keyDownListener);
        addEventListener(canvas, "keyup", keyUpListener);
        addEventListener(canvas, "keypress", keyPressListener);

        // Mouse events
        addEventListener(canvas, "mousedown", mouseDownListener);
        addEventListener(canvas, "mouseup", mouseUpListener);
        addEventListener(canvas, "mousemove", mouseMoveListener);
        addEventListener(canvas, "wheel", wheelListener);

        // Window events
        addWindowEventListener("blur", blurListener);
        addWindowEventListener("focus", focusListener);
        addWindowEventListener("resize", resizeListener);

        // Register clipboard copy listener
        registerClipboardListener();

        log("CanvasWindowBackend: DOM listeners registered");
    }

    // ---- Native DOM access ----

    @JSBody(script =
        "document.addEventListener('copy', function(e) {" +
        "  var text = '';" +
        "  if (e.clipboardData) {" +
        "    text = e.clipboardData.getData('text/plain') || '';" +
        "  }" +
        "  if (typeof window.__webmcOnClipboardCopy === 'function') {" +
        "    window.__webmcOnClipboardCopy(text);" +
        "  }" +
        "}, false);")
    private static native void registerClipboardListener();

    @JSBody(script =
        "var c = document.getElementById('game-canvas');" +
        "if (c && c.tagName === 'CANVAS') return c;" +
        "return null;")
    private static native HTMLCanvasElement getCanvasElement();

    @JSBody(params = {"canvas", "index"}, script = "canvas.tabIndex = index;")
    private static native void setCanvasTabIndex(HTMLCanvasElement canvas, int index);

    @JSBody(params = {"target", "type", "listener"}, script =
        "target.addEventListener(type, listener);")
    private static native void addEventListener(EventTarget target, String type, EventListener<?> listener);

    @JSBody(params = {"target", "type", "listener"}, script =
        "target.removeEventListener(type, listener);")
    private static native void removeEventListener(EventTarget target, String type, EventListener<?> listener);

    @JSBody(params = {"type", "listener"}, script =
        "window.addEventListener(type, listener);")
    private static native void addWindowEventListener(String type, EventListener<?> listener);

    @JSBody(params = {"type", "listener"}, script =
        "window.removeEventListener(type, listener);")
    private static native void removeWindowEventListener(String type, EventListener<?> listener);

    @JSBody(params = "msg", script = "console.log('[CanvasWindowBackend] ' + msg);")
    private static native void log(String msg);

    // ---- Native event property accessors ----

    @JSBody(params = {"event", "prop"}, script = "return event[prop];")
    private static native int getEventIntProp(JSObject event, String prop);

    @JSBody(params = {"event", "prop"}, script = "return event[prop];")
    private static native double getEventDoubleProp(JSObject event, String prop);

    @JSBody(params = {"event", "prop"}, script = "return event[prop] || false;")
    private static native boolean getEventBoolProp(JSObject event, String prop);

    @JSBody(params = {"canvas", "prop"}, script = "return canvas[prop];")
    private static native int getCanvasIntProp(HTMLCanvasElement canvas, String prop);

    @JSBody(params = "canvas", script = "canvas.focus();")
    private static native void focusCanvas(HTMLCanvasElement canvas);

    // ---- Event Listeners ----

    private final EventListener<Event> keyDownListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            JSObject event = (JSObject) evt;
            int keyCode = getEventIntProp(event, "keyCode");
            int key = domKeyCodeToGlfw(keyCode);
            int scancode = domKeyCodeToGlfwScancode(keyCode);
            int action = GLFW.GLFW_PRESS;
            int mods = getGlfwMods(event);

            // Track key state for polling methods
            if (key >= 0 && key < keyStates.length) {
                keyStates[key] = true;
            }

            invokeKeyCallbacks(getFocusedWindowHandle(), key, scancode, action, mods);

            // For printable characters, also invoke char callback
            if (key >= 32 && key <= 126) {
                invokeCharCallbacks(getFocusedWindowHandle(), key);
            }

            // Prevent default for game keys to avoid browser shortcuts
            if (isGameKey(key)) {
                preventDefault(event);
            }
        }
    };

    private final EventListener<Event> keyUpListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            JSObject event = (JSObject) evt;
            int keyCode = getEventIntProp(event, "keyCode");
            int key = domKeyCodeToGlfw(keyCode);
            int scancode = domKeyCodeToGlfwScancode(keyCode);
            int action = GLFW.GLFW_RELEASE;
            int mods = getGlfwMods(event);

            // Track key state for polling methods
            if (key >= 0 && key < keyStates.length) {
                keyStates[key] = false;
            }

            invokeKeyCallbacks(getFocusedWindowHandle(), key, scancode, action, mods);
        }
    };

    private final EventListener<Event> keyPressListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            JSObject event = (JSObject) evt;
            int charCode = getEventIntProp(event, "charCode");
            if (charCode > 0) {
                invokeCharCallbacks(getFocusedWindowHandle(), charCode);
            }
        }
    };

    private final EventListener<Event> mouseDownListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            JSObject event = (JSObject) evt;
            int button = domButtonToGlfw(getEventIntProp(event, "button"));
            if (button >= 0 && button < mouseButtons.length) {
                mouseButtons[button] = true;
            }
            int action = GLFW.GLFW_PRESS;
            int mods = getGlfwMods(event);

            // Update cursor position
            cursorX = getEventDoubleProp(event, "clientX");
            cursorY = getEventDoubleProp(event, "clientY");

            invokeMouseButtonCallbacks(getWindowHandle(), button, action, mods);

            // Focus canvas on click
            if (canvas != null) {
                focusCanvas(canvas);
            }
        }
    };

    private final EventListener<Event> mouseUpListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            JSObject event = (JSObject) evt;
            int button = domButtonToGlfw(getEventIntProp(event, "button"));
            if (button >= 0 && button < mouseButtons.length) {
                mouseButtons[button] = false;
            }
            int action = GLFW.GLFW_RELEASE;
            int mods = getGlfwMods(event);

            invokeMouseButtonCallbacks(getWindowHandle(), button, action, mods);
        }
    };

    private final EventListener<Event> mouseMoveListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            JSObject event = (JSObject) evt;
            cursorX = getEventDoubleProp(event, "clientX");
            cursorY = getEventDoubleProp(event, "clientY");

            invokeCursorPosCallbacks(getWindowHandle(), cursorX, cursorY);
        }
    };

    private final EventListener<Event> wheelListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            JSObject event = (JSObject) evt;
            double xOffset = getEventDoubleProp(event, "deltaX");
            double yOffset = getEventDoubleProp(event, "deltaY");

            // Normalize: browser scroll values vary, GLFW expects 1.0 per line
            invokeScrollCallbacks(getWindowHandle(), xOffset, yOffset);
            preventDefault(event);
        }
    };

    private final EventListener<Event> blurListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            // Window lost focus - clear all key and mouse states
            hasFocus = false;
            clearAllKeyStates();
            clearAllMouseButtonStates();
            invokeWindowFocusCallbacks(getWindowHandle(), false);
        }
    };

    private final EventListener<Event> focusListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            // Window gained focus
            hasFocus = true;
            invokeWindowFocusCallbacks(getWindowHandle(), true);
        }
    };

    private final EventListener<Event> resizeListener = new EventListener<Event>() {
        @Override
        public void handleEvent(Event evt) {
            if (canvas != null) {
                int width = getCanvasIntProp(canvas, "clientWidth");
                int height = getCanvasIntProp(canvas, "clientHeight");
                invokeFramebufferSizeCallbacks(getWindowHandle(), width, height);
            }
        }
    };

    @JSBody(params = "event", script = "event.preventDefault();")
    private static native void preventDefault(JSObject event);

    // ---- Helper methods ----

    private static long getWindowHandle() {
        return 1L;
    }

    private static long getFocusedWindowHandle() {
        return 1L;
    }

    private void invokeKeyCallbacks(long handle, int key, int scancode, int action, int mods) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) {
            if (callbacks.key != null) {
                callbacks.key.invoke(handle, key, scancode, action, mods);
            }
            if (callbacks.keyI != null) {
                callbacks.keyI.invoke(handle, key, scancode, action, mods);
            }
        }
    }

    private void invokeCharCallbacks(long handle, int codepoint) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) {
            if (callbacks.character != null) {
                callbacks.character.invoke(handle, codepoint);
            }
            if (callbacks.characterI != null) {
                callbacks.characterI.invoke(handle, codepoint);
            }
        }
    }

    private void invokeMouseButtonCallbacks(long handle, int button, int action, int mods) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) {
            if (callbacks.mouseButton != null) {
                callbacks.mouseButton.invoke(handle, button, action, mods);
            }
            if (callbacks.mouseButtonI != null) {
                callbacks.mouseButtonI.invoke(handle, button, action, mods);
            }
        }
    }

    private void invokeCursorPosCallbacks(long handle, double xpos, double ypos) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) {
            if (callbacks.cursorPos != null) {
                callbacks.cursorPos.invoke(handle, xpos, ypos);
            }
            if (callbacks.cursorPosI != null) {
                callbacks.cursorPosI.invoke(handle, xpos, ypos);
            }
        }
    }

    private void invokeScrollCallbacks(long handle, double xoffset, double yoffset) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) {
            if (callbacks.scroll != null) {
                callbacks.scroll.invoke(handle, xoffset, yoffset);
            }
            if (callbacks.scrollI != null) {
                callbacks.scrollI.invoke(handle, xoffset, yoffset);
            }
        }
    }

    private void invokeFramebufferSizeCallbacks(long handle, int width, int height) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) {
            if (callbacks.framebufferSize != null) {
                callbacks.framebufferSize.invoke(handle, width, height);
            }
            if (callbacks.framebufferSizeI != null) {
                callbacks.framebufferSizeI.invoke(handle, width, height);
            }
        }
    }

    private void invokeWindowFocusCallbacks(long handle, boolean focused) {
        // Window focus callbacks - handled via I interface
    }

    /**
     * Clear all key states (called on blur/focus loss)
     */
    private void clearAllKeyStates() {
        for (int i = 0; i < keyStates.length; i++) {
            keyStates[i] = false;
        }
    }

    /**
     * Clear all mouse button states (called on blur/focus loss)
     */
    private void clearAllMouseButtonStates() {
        for (int i = 0; i < mouseButtons.length; i++) {
            mouseButtons[i] = false;
        }
    }

    // ---- Key code mapping ----

    private static int domKeyCodeToGlfw(int keyCode) {
        switch (keyCode) {
            case 8: return GLFW.GLFW_KEY_BACKSPACE;
            case 9: return GLFW.GLFW_KEY_TAB;
            case 13: return GLFW.GLFW_KEY_ENTER;
            case 16: return GLFW.GLFW_KEY_LEFT_SHIFT;
            case 17: return GLFW.GLFW_KEY_LEFT_CONTROL;
            case 18: return GLFW.GLFW_KEY_LEFT_ALT;
            case 19: return GLFW.GLFW_KEY_PAUSE;
            case 27: return GLFW.GLFW_KEY_ESCAPE;
            case 32: return GLFW.GLFW_KEY_SPACE;
            case 33: return GLFW.GLFW_KEY_PAGE_UP;
            case 34: return GLFW.GLFW_KEY_PAGE_DOWN;
            case 35: return GLFW.GLFW_KEY_END;
            case 36: return GLFW.GLFW_KEY_HOME;
            case 37: return GLFW.GLFW_KEY_LEFT;
            case 38: return GLFW.GLFW_KEY_UP;
            case 39: return GLFW.GLFW_KEY_RIGHT;
            case 40: return GLFW.GLFW_KEY_DOWN;
            case 45: return GLFW.GLFW_KEY_INSERT;
            case 46: return GLFW.GLFW_KEY_DELETE;
            case 48: return GLFW.GLFW_KEY_0;
            case 49: return GLFW.GLFW_KEY_1;
            case 50: return GLFW.GLFW_KEY_2;
            case 51: return GLFW.GLFW_KEY_3;
            case 52: return GLFW.GLFW_KEY_4;
            case 53: return GLFW.GLFW_KEY_5;
            case 54: return GLFW.GLFW_KEY_6;
            case 55: return GLFW.GLFW_KEY_7;
            case 56: return GLFW.GLFW_KEY_8;
            case 57: return GLFW.GLFW_KEY_9;
            case 65: return GLFW.GLFW_KEY_A;
            case 66: return GLFW.GLFW_KEY_B;
            case 67: return GLFW.GLFW_KEY_C;
            case 68: return GLFW.GLFW_KEY_D;
            case 69: return GLFW.GLFW_KEY_E;
            case 70: return GLFW.GLFW_KEY_F;
            case 71: return GLFW.GLFW_KEY_G;
            case 72: return GLFW.GLFW_KEY_H;
            case 73: return GLFW.GLFW_KEY_I;
            case 74: return GLFW.GLFW_KEY_J;
            case 75: return GLFW.GLFW_KEY_K;
            case 76: return GLFW.GLFW_KEY_L;
            case 77: return GLFW.GLFW_KEY_M;
            case 78: return GLFW.GLFW_KEY_N;
            case 79: return GLFW.GLFW_KEY_O;
            case 80: return GLFW.GLFW_KEY_P;
            case 81: return GLFW.GLFW_KEY_Q;
            case 82: return GLFW.GLFW_KEY_R;
            case 83: return GLFW.GLFW_KEY_S;
            case 84: return GLFW.GLFW_KEY_T;
            case 85: return GLFW.GLFW_KEY_U;
            case 86: return GLFW.GLFW_KEY_V;
            case 87: return GLFW.GLFW_KEY_W;
            case 88: return GLFW.GLFW_KEY_X;
            case 89: return GLFW.GLFW_KEY_Y;
            case 90: return GLFW.GLFW_KEY_Z;
            case 112: return 290; // F1
            case 113: return 291; // F2
            case 114: return 292; // F3
            case 115: return 293; // F4
            case 116: return 294; // F5
            case 117: return 295; // F6
            case 118: return 296; // F7
            case 119: return 297; // F8
            case 120: return 298; // F9
            case 121: return 299; // F10
            case 122: return 300; // F11
            case 123: return 301; // F12
            case 144: return 282; // NUM_LOCK
            case 186: return 59;  // SEMICOLON
            case 187: return 61;  // EQUALS
            case 189: return 45;  // MINUS
            case 191: return 47;  // SLASH
            case 192: return 96;  // GRAVE
            case 219: return 91;  // LEFT_BRACKET
            case 220: return 92;  // BACKSLASH
            case 221: return 93;  // RIGHT_BRACKET
            case 222: return 39;  // APOSTROPHE
            default: return keyCode;
        }
    }

    private static int domKeyCodeToGlfwScancode(int keyCode) {
        return keyCode;
    }

    private static int domButtonToGlfw(int button) {
        switch (button) {
            case 0: return 0;  // Left button
            case 1: return 2;  // Middle button
            case 2: return 1;  // Right button
            default: return button;
        }
    }

    private static int getGlfwMods(JSObject event) {
        int mods = 0;
        if (getEventBoolProp(event, "shiftKey")) mods |= GLFW.GLFW_MOD_SHIFT;
        if (getEventBoolProp(event, "ctrlKey")) mods |= GLFW.GLFW_MOD_CONTROL;
        if (getEventBoolProp(event, "altKey")) mods |= GLFW.GLFW_MOD_ALT;
        if (getEventBoolProp(event, "metaKey")) mods |= GLFW.GLFW_MOD_SUPER;
        return mods;
    }

    private static boolean isGameKey(int key) {
        return key != GLFW.GLFW_KEY_F5 &&
               key != GLFW.GLFW_KEY_F12 &&
               key != GLFW.GLFW_KEY_TAB;
    }

    // ---- WindowBackend interface implementation ----

    @Override
    public void destroyWindow(long handle) {
        windows.remove(handle);
    }

    @Override
    public void makeContextCurrent(long handle) {
    }

    @Override
    public boolean shouldClose(long handle) {
        return shouldClose;
    }

    @Override
    public void setShouldClose(long handle, boolean value) {
        shouldClose = value;
    }

    @Override
    public void setTitle(long handle, String title) {
    }

    @Override
    public void setSize(long handle, int width, int height) {
    }

    @Override
    public void getFramebufferSize(long handle, int[] width, int[] height) {
        if (canvas != null) {
            int dpr = getDevicePixelRatio();
            if (width != null && width.length > 0) {
                width[0] = getCanvasIntProp(canvas, "clientWidth") * dpr;
            }
            if (height != null && height.length > 0) {
                height[0] = getCanvasIntProp(canvas, "clientHeight") * dpr;
            }
        } else {
            if (width != null && width.length > 0) width[0] = 1280;
            if (height != null && height.length > 0) height[0] = 720;
        }
    }

    @JSBody(script = "return window.devicePixelRatio || 1;")
    private static native int getDevicePixelRatio();

    @Override
    public void getWindowSize(long handle, int[] width, int[] height) {
        if (canvas != null) {
            if (width != null && width.length > 0) {
                width[0] = getCanvasIntProp(canvas, "clientWidth");
            }
            if (height != null && height.length > 0) {
                height[0] = getCanvasIntProp(canvas, "clientHeight");
            }
        } else {
            getFramebufferSize(handle, width, height);
        }
    }

    @Override
    public void setInputMode(long handle, int mode, int value) {
        if (mode == GLFW.GLFW_CURSOR && canvas != null) {
            switch (value) {
                case GLFW.GLFW_CURSOR_DISABLED:
                    requestPointerLock(canvas);
                    break;
                case GLFW.GLFW_CURSOR_HIDDEN:
                case GLFW.GLFW_CURSOR_NORMAL:
                    exitPointerLock();
                    break;
            }
        }
    }

    @JSBody(params = "canvas", script = "canvas.requestPointerLock();")
    private static native void requestPointerLock(HTMLCanvasElement canvas);

    @JSBody(script = "document.exitPointerLock();")
    private static native void exitPointerLock();

    @Override
    public int getInputMode(long handle, int mode) {
        if (mode == GLFW.GLFW_CURSOR) {
            if (isPointerLocked()) {
                return GLFW.GLFW_CURSOR_DISABLED;
            }
            return GLFW.GLFW_CURSOR_NORMAL;
        }
        return 0;
    }

    @JSBody(script = "return document.pointerLockElement !== null;")
    private static native boolean isPointerLocked();

    @Override
    public boolean getKey(long handle, int key) {
        if (key >= 0 && key < keyStates.length) {
            return keyStates[key];
        }
        return false;
    }

    @Override
    public boolean getMouseButton(long handle, int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button];
        }
        return false;
    }

    @Override
    public void getCursorPos(long handle, double[] x, double[] y) {
        if (x != null && x.length > 0) x[0] = cursorX;
        if (y != null && y.length > 0) y[0] = cursorY;
    }

    @Override
    public void setCursorPos(long handle, double x, double y) {
        cursorX = x;
        cursorY = y;
    }

    private static String cachedClipboard = "";

    public static void onBrowserCopy(String text) {
        cachedClipboard = text != null ? text : "";
    }

    @Override
    public String getClipboard() {
        return cachedClipboard;
    }

    @JSBody(params = "text", script =
        "try {" +
        "  if (navigator && navigator.clipboard && navigator.clipboard.writeText) {" +
        "    navigator.clipboard.writeText(text).catch(function() {});" +
        "  }" +
        "} catch (e) {}")
    private static native void writeClipboardNative(String text);

    @Override
    public void setClipboard(String value) {
        cachedClipboard = value != null ? value : "";
        writeClipboardNative(cachedClipboard);
    }

    @Override
    public void setKeyCallback(long handle, GLFWKeyCallback callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.key = callback;
    }

    @Override
    public void setCharCallback(long handle, GLFWCharCallback callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.character = callback;
    }

    @Override
    public void setMouseButtonCallback(long handle, GLFWMouseButtonCallback callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.mouseButton = callback;
    }

    @Override
    public void setCursorPosCallback(long handle, GLFWCursorPosCallback callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.cursorPos = callback;
    }

    @Override
    public void setScrollCallback(long handle, GLFWScrollCallback callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.scroll = callback;
    }

    @Override
    public void setFramebufferSizeCallback(long handle, GLFWFramebufferSizeCallback callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.framebufferSize = callback;
    }

    // ---- I-interface callback setters (used by MC) ----

    @Override
    public void setKeyCallbackI(long handle, org.lwjgl.glfw.GLFWKeyCallbackI callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.keyI = callback;
    }

    @Override
    public void setCharCallbackI(long handle, org.lwjgl.glfw.GLFWCharCallbackI callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.characterI = callback;
    }

    @Override
    public void setMouseButtonCallbackI(long handle, org.lwjgl.glfw.GLFWMouseButtonCallbackI callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.mouseButtonI = callback;
    }

    @Override
    public void setCursorPosCallbackI(long handle, org.lwjgl.glfw.GLFWCursorPosCallbackI callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.cursorPosI = callback;
    }

    @Override
    public void setScrollCallbackI(long handle, org.lwjgl.glfw.GLFWScrollCallbackI callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.scrollI = callback;
    }

    @Override
    public void setFramebufferSizeCallbackI(long handle, org.lwjgl.glfw.GLFWFramebufferSizeCallbackI callback) {
        Callbacks callbacks = windows.get(handle);
        if (callbacks != null) callbacks.framebufferSizeI = callback;
    }
}
