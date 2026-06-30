package top.steve3184.webmc.teavm.runtime;

import top.steve3184.webmc.teavm.glfw.WindowBackend;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.lwjgl.glfw.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.HashMap;
import java.util.Map;

/**
 * GLFW stub backend backed by HTML canvas + DOM events.
 *
 * Phase 0: scaffolding only. Phase 1: wire up DOM listeners through TeaVM JSO.
 */
public final class CanvasWindowBackend implements WindowBackend {
    private long nextHandle = 1L;
    private double startTime = 0.0;
    private boolean shouldClose = false;
    private long activeHandle = 0L;
    private double cursorX;
    private double cursorY;
    private int cursorMode = GLFW.GLFW_CURSOR_NORMAL;
    private int inputLogCount;

    private final Map<Long, Callbacks> windows = new HashMap<>();
    private final boolean[] keys = new boolean[512];
    private final boolean[] mouseButtons = new boolean[8];
    private String clipboard = "";

    private static final class Callbacks {
        GLFWKeyCallback key;
        GLFWCharCallback chr;
        GLFWCharModsCallbackI chrMods;
        GLFWMouseButtonCallback mb;
        GLFWCursorPosCallback cp;
        GLFWScrollCallback scr;
        GLFWFramebufferSizeCallback fbsz;
        GLFWWindowFocusCallback focus;
    }

    @JSFunctor
    private interface KeyEventHandler extends JSObject {
        void handle(int key, int scancode, int action, int mods);
    }

    @JSFunctor
    private interface CharEventHandler extends JSObject {
        void handle(int codepoint, int mods);
    }

    @JSFunctor
    private interface MouseMoveHandler extends JSObject {
        void handle(double x, double y);
    }

    @JSFunctor
    private interface MouseButtonHandler extends JSObject {
        void handle(int button, int action, int mods);
    }

    @JSFunctor
    private interface ScrollHandler extends JSObject {
        void handle(double xoffset, double yoffset);
    }

    @JSBody(params = {"key", "chr", "move", "button", "scroll"}, script =
        "var canvas = document.getElementById('game-canvas');" +
        "if (!canvas || canvas.__webmcInputInstalled) return;" +
        "canvas.__webmcInputInstalled = true;" +
        "canvas.tabIndex = canvas.tabIndex || 0;" +
        "canvas.__webmcCursor = canvas.__webmcCursor || { x: (canvas.width || 1280) / 2, y: (canvas.height || 720) / 2 };" +
        "canvas.__webmcCursorMode = canvas.__webmcCursorMode || 0x00034001;" +
        "var mods = function(e) { return (e.shiftKey ? 1 : 0) | (e.ctrlKey ? 2 : 0) | (e.altKey ? 4 : 0) | (e.metaKey ? 8 : 0); };" +
        "var latestState = function() {" +
        "  var holder = window.__webmcLatestState;" +
        "  return holder && holder.state || null;" +
        "};" +
        "var isNullScreen = function(value) { return value == null || value === 'null'; };" +
        "var isInGamePointerTarget = function() {" +
        "  var state = latestState();" +
        "  return !!(state && isNullScreen(state.screen) && isNullScreen(state.overlay) && state.levelPresent && state.playerPresent && state.worldRenderEligible);" +
        "};" +
        "var syncCursorToCenter = function() {" +
        "  var c = canvas.__webmcCursor || (canvas.__webmcCursor = { x: 0, y: 0 });" +
        "  c.x = (canvas.width || 1280) / 2;" +
        "  c.y = (canvas.height || 720) / 2;" +
        "};" +
        "var requestPointerLockSafely = function() {" +
        "  if (!canvas.requestPointerLock || document.pointerLockElement === canvas) return;" +
        "  try {" +
        "    syncCursorToCenter();" +
        "    canvas.__webmcPointerLockPending = true;" +
        "    var pending = canvas.requestPointerLock();" +
        "    if (pending && pending.then) pending.then(function() { canvas.__webmcPointerLockPending = false; }, function() { canvas.__webmcPointerLockPending = true; });" +
        "  } catch (err) { canvas.__webmcPointerLockPending = true; }" +
        "};" +
        "document.addEventListener('pointerlockchange', function() { if (document.pointerLockElement === canvas) { canvas.__webmcPointerLockPending = false; syncCursorToCenter(); } });" +
        "var requestLockIfDisabled = function() {" +
        "  if (canvas.__webmcCursorMode === 0x00034003 || isInGamePointerTarget()) requestPointerLockSafely();" +
        "};" +
        "var glfwKey = function(e) {" +
        "  var k = e.key || '';" +
        "  if (k.length === 1) return k.toUpperCase().charCodeAt(0);" +
        "  switch (k) {" +
        "    case 'Escape': return 256; case 'Enter': return 257; case 'Tab': return 258; case 'Backspace': return 259;" +
        "    case 'Insert': return 260; case 'Delete': return 261; case 'ArrowRight': return 262; case 'ArrowLeft': return 263;" +
        "    case 'ArrowDown': return 264; case 'ArrowUp': return 265; case 'PageUp': return 266; case 'PageDown': return 267;" +
        "    case 'Home': return 268; case 'End': return 269; case 'Shift': return e.location === 2 ? 344 : 340;" +
        "    case 'Control': return e.location === 2 ? 345 : 341; case 'Alt': return e.location === 2 ? 346 : 342;" +
        "    case 'F1': return 290; case 'F2': return 291; case 'F3': return 292; case 'F4': return 293;" +
        "    case 'F5': return 294; case 'F6': return 295; case 'F7': return 296; case 'F8': return 297;" +
        "    case 'F9': return 298; case 'F10': return 299; case 'F11': return 300; case 'F12': return 301;" +
        "    default: return -1;" +
        "  }" +
        "};" +
        "var mapMouseButton = function(button) {" +
        "  if (button === 2) return 1;" +
        "  if (button === 1) return 2;" +
        "  return button;" +
        "};" +
        "var pos = function(e) {" +
        "  var r = canvas.getBoundingClientRect();" +
        "  var w = canvas.width || r.width || 1280;" +
        "  var h = canvas.height || r.height || 720;" +
        "  return [(e.clientX - r.left) * w / Math.max(1, r.width), (e.clientY - r.top) * h / Math.max(1, r.height)];" +
        "};" +
        "canvas.addEventListener('mousemove', function(e) {" +
        "  var c = canvas.__webmcCursor;" +
        "  if (document.pointerLockElement === canvas) {" +
        "    c.x += e.movementX || 0;" +
        "    c.y += e.movementY || 0;" +
        "    move(c.x, c.y);" +
        "    e.preventDefault();" +
        "    return;" +
        "  }" +
        "  var p = pos(e); c.x = p[0]; c.y = p[1]; move(c.x, c.y);" +
        "});" +
        "canvas.addEventListener('mousedown', function($event) { canvas.focus(); requestLockIfDisabled(); var c = canvas.__webmcCursor; var p = document.pointerLockElement === canvas ? [c.x, c.y] : pos($event); var mapped = mapMouseButton($event.button); c.x = p[0]; c.y = p[1]; console.log('[mc-web/input] dom mousedown button=' + $event.button + ' mapped=' + mapped + ' x=' + p[0] + ' y=' + p[1]); if (document.pointerLockElement !== canvas) move(p[0], p[1]); button(mapped, 1, mods($event)); $event.preventDefault(); });" +
        "canvas.addEventListener('mouseup', function($event) { var c = canvas.__webmcCursor; var p = document.pointerLockElement === canvas ? [c.x, c.y] : pos($event); var mapped = mapMouseButton($event.button); c.x = p[0]; c.y = p[1]; console.log('[mc-web/input] dom mouseup button=' + $event.button + ' mapped=' + mapped + ' x=' + p[0] + ' y=' + p[1]); if (document.pointerLockElement !== canvas) move(p[0], p[1]); button(mapped, 0, mods($event)); requestLockIfDisabled(); $event.preventDefault(); });" +
        "canvas.addEventListener('contextmenu', function(e) { e.preventDefault(); });" +
        "canvas.addEventListener('wheel', function(e) { scroll(e.deltaX === 0 ? 0 : -Math.sign(e.deltaX), e.deltaY === 0 ? 0 : -Math.sign(e.deltaY)); e.preventDefault(); }, { passive: false });" +
        "canvas.addEventListener('keydown', function(e) { var k = glfwKey(e); key(k, e.keyCode || 0, e.repeat ? 2 : 1, mods(e)); if (k !== -1) e.preventDefault(); });" +
        "canvas.addEventListener('keyup', function(e) { var k = glfwKey(e); key(k, e.keyCode || 0, 0, mods(e)); if (k !== -1) e.preventDefault(); });" +
        "canvas.addEventListener('keypress', function(e) { var cp = e.key && e.key.length === 1 ? e.key.codePointAt(0) : 0; if (cp > 0) chr(cp, mods(e)); });" +
        "canvas.addEventListener('focus', function() { if (window.__webmcFocus) window.__webmcFocus(true); });" +
        "canvas.addEventListener('blur', function() { if (window.__webmcFocus) window.__webmcFocus(false); });" +
        "window.addEventListener('focus', function() { if (window.__webmcFocus) window.__webmcFocus(true); });" +
        "window.addEventListener('blur', function() { if (window.__webmcFocus) window.__webmcFocus(false); });" +
        "document.addEventListener('visibilitychange', function() { if (window.__webmcFocus) window.__webmcFocus(document.visibilityState === 'visible'); });" +
        "console.log('[mc-web/input] DOM input listeners installed');")
    private static native void installDomInput(
        KeyEventHandler key,
        CharEventHandler chr,
        MouseMoveHandler move,
        MouseButtonHandler button,
        ScrollHandler scroll
    );

    @JSFunctor
    private interface FocusHandler extends JSObject {
        void handle(boolean focused);
    }

    @JSBody(params = {"focus"}, script =
        "window.__webmcFocus = function(focused) { focus(!!focused); };")
    private static native void installFocusBridge(FocusHandler focus);

    @JSBody(params = {"mode"}, script =
        "try {" +
        "  var canvas = document.getElementById('game-canvas');" +
        "  if (!canvas) return;" +
        "  canvas.__webmcCursorMode = mode;" +
        "  if (mode === 0x00034003) {" +
        "    if (document.pointerLockElement !== canvas && canvas.requestPointerLock) {" +
        "      canvas.__webmcPointerLockPending = true;" +
        "      var pending = canvas.requestPointerLock();" +
        "      if (pending && pending.then) pending.then(function() { canvas.__webmcPointerLockPending = false; }, function() { canvas.__webmcPointerLockPending = true; });" +
        "    }" +
        "  } else if (document.pointerLockElement === canvas && document.exitPointerLock) {" +
        "    document.exitPointerLock();" +
        "  }" +
        "} catch (err) { console.warn('[mc-web/input] cursor mode update failed', err); }")
    private static native void setDomCursorMode(int mode);

    @JSBody(params = {"x", "y"}, script =
        "try {" +
        "  var canvas = document.getElementById('game-canvas');" +
        "  if (!canvas) return;" +
        "  var c = canvas.__webmcCursor || (canvas.__webmcCursor = { x: 0, y: 0 });" +
        "  c.x = x;" +
        "  c.y = y;" +
        "} catch (err) {}")
    private static native void syncDomCursor(double x, double y);

    @JSBody(script =
        "try {" +
        "  return String(window.__webmcClipboard || '');" +
        "} catch (err) { return ''; }")
    private static native String getDomClipboardCache();

    @JSBody(params = {"text"}, script =
        "try {" +
        "  window.__webmcClipboard = String(text || '');" +
        "  if (navigator.clipboard && navigator.clipboard.writeText) {" +
        "    navigator.clipboard.writeText(window.__webmcClipboard).catch(function() {});" +
        "  }" +
        "} catch (err) {}")
    private static native void setDomClipboard(String text);

    @JSBody(script =
        "try {" +
        "  if (navigator.clipboard && navigator.clipboard.readText) {" +
        "    navigator.clipboard.readText().then(function(text) { window.__webmcClipboard = String(text || ''); }).catch(function() {});" +
        "  }" +
        "} catch (err) {}")
    private static native void requestDomClipboardRefresh();

    @Override public boolean init() {
        installDomInput(this::handleKeyEvent, this::handleCharEvent, this::handleMouseMove, this::handleMouseButton, this::handleScroll);
        installFocusBridge(this::handleFocusChange);
        return true;
    }
    @Override public void terminate() { /* listeners live for the page lifetime */ }

    @Override public double time() {
        return WebTime.seconds() - startTime;
    }
    @Override public void setTime(double t) { startTime = WebTime.seconds() - t; }

    @Override public long createWindow(int w, int h, String title) {
        long h2 = nextHandle++;
        activeHandle = h2;
        windows.put(h2, new Callbacks());
        jsResizeCanvas(w, h);
        return h2;
    }
    @Override public void destroyWindow(long handle)         { windows.remove(handle); }
    @Override public void makeContextCurrent(long handle)    { /* JS: ensure gl context bound. */ }
    @Override public boolean shouldClose(long handle)        { return shouldClose; }
    @Override public void setShouldClose(long handle, boolean v) { shouldClose = v; }
    @Override public void setTitle(long handle, String title) { /* TODO: document.title = title */ }
    @Override public void setSize(long handle, int w, int h) {
        jsResizeCanvas(w, h);
    }

    @JSBody(params = {"w", "h"}, script =
        "try {" +
        "  var c = document.getElementById('game-canvas');" +
        "  if (!c) return;" +
        "  c.width = w;" +
        "  c.height = h;" +
        "  var gl = c.getContext('webgl2');" +
        "  if (gl) gl.viewport(0, 0, w, h);" +
        "} catch(e) {}")
    private static native void jsResizeCanvas(int w, int h);

    @JSBody(script =
        "try {" +
        "  var c = document.getElementById('game-canvas');" +
        "  if (!c) return [1280, 720];" +
        "  var w = c.width || 1280;" +
        "  var h = c.height || 720;" +
        "  if (w <= 300 || h <= 150) {" +
        "    var r = c.getBoundingClientRect();" +
        "    if (r && r.width > 0 && r.height > 0) return [Math.round(r.width), Math.round(r.height)];" +
        "  }" +
        "  return [w, h];" +
        "} catch(e) { return [1280, 720]; }")
    private static native int[] jsGetCanvasSize();

    @Override public void getFramebufferSize(long handle, int[] w, int[] h) {
        int[] real = jsGetCanvasSize();
        if (w != null && w.length > 0) w[0] = real[0];
        if (h != null && h.length > 0) h[0] = real[1];
    }
    @Override public void getWindowSize(long handle, int[] w, int[] h) {
        getFramebufferSize(handle, w, h);
    }

    @Override public void setInputMode(long handle, int mode, int value) {
        if (mode == GLFW.GLFW_CURSOR) {
            cursorMode = value;
            setDomCursorMode(value);
        }
    }
    @Override public int getInputMode(long handle, int mode) { return mode == GLFW.GLFW_CURSOR ? cursorMode : 0; }

    @Override public boolean getKey(long handle, int key)            { return key >= 0 && key < keys.length && keys[key]; }
    @Override public boolean getMouseButton(long handle, int button) { return button >= 0 && button < mouseButtons.length && mouseButtons[button]; }
    @Override public void getCursorPos(long handle, double[] x, double[] y) {
        if (x != null && x.length > 0) x[0] = cursorX;
        if (y != null && y.length > 0) y[0] = cursorY;
    }
    public void setCursorPos(long handle, double x, double y) {
        cursorX = x;
        cursorY = y;
        syncDomCursor(x, y);
        Callbacks c = windows.get(handle);
        if (c != null && c.cp != null) c.cp.invoke(handle, x, y);
    }

    @Override public String getClipboard() {
        requestDomClipboardRefresh();
        String domClipboard = getDomClipboardCache();
        if (domClipboard != null && !domClipboard.isEmpty()) {
            this.clipboard = domClipboard;
        }
        return this.clipboard;
    }
    @Override public void setClipboard(String s) {
        this.clipboard = s == null ? "" : s;
        setDomClipboard(this.clipboard);
    }

    @Override public void setKeyCallback(long h, GLFWKeyCallback cb)         { Callbacks c = windows.get(h); if (c != null) c.key = cb; System.err.println("[mc-web/input] key callback registered=" + (cb != null)); }
    @Override public void setCharCallback(long h, GLFWCharCallback cb)       { Callbacks c = windows.get(h); if (c != null) c.chr = cb; System.err.println("[mc-web/input] char callback registered=" + (cb != null)); }
    public void setCharModsCallback(long h, GLFWCharModsCallbackI cb) { Callbacks c = windows.get(h); if (c != null) c.chrMods = cb; System.err.println("[mc-web/input] charMods callback registered=" + (cb != null)); }
    @Override public void setMouseButtonCallback(long h, GLFWMouseButtonCallback cb) { Callbacks c = windows.get(h); if (c != null) c.mb = cb; System.err.println("[mc-web/input] mouse button callback registered=" + (cb != null)); }
    @Override public void setCursorPosCallback(long h, GLFWCursorPosCallback cb)     { Callbacks c = windows.get(h); if (c != null) c.cp = cb; System.err.println("[mc-web/input] cursor callback registered=" + (cb != null)); }
    @Override public void setScrollCallback(long h, GLFWScrollCallback cb)           { Callbacks c = windows.get(h); if (c != null) c.scr = cb; System.err.println("[mc-web/input] scroll callback registered=" + (cb != null)); }
    @Override public void setFramebufferSizeCallback(long h, GLFWFramebufferSizeCallback cb) { Callbacks c = windows.get(h); if (c != null) c.fbsz = cb; }
    @Override public void setWindowFocusCallback(long h, GLFWWindowFocusCallback cb) { Callbacks c = windows.get(h); if (c != null) c.focus = cb; }

    private void handleKeyEvent(int key, int scancode, int action, int mods) {
        if (key >= 0 && key < keys.length) {
            keys[key] = action != GLFW.GLFW_RELEASE;
        }
        Callbacks c = windows.get(activeHandle);
        if (c != null && c.key != null) c.key.invoke(activeHandle, key, scancode, action, mods);
    }

    private void handleCharEvent(int codepoint, int mods) {
        Callbacks c = windows.get(activeHandle);
        if (c != null) {
            if (c.chr != null) c.chr.invoke(activeHandle, codepoint);
            if (c.chrMods != null) c.chrMods.invoke(activeHandle, codepoint, mods);
        }
    }

    private void handleMouseMove(double x, double y) {
        cursorX = x;
        cursorY = y;
        Callbacks c = windows.get(activeHandle);
        if (c != null && c.cp != null) c.cp.invoke(activeHandle, x, y);
    }

    private void handleMouseButton(int button, int action, int mods) {
        if (inputLogCount < 16) {
            Callbacks callbacks = windows.get(activeHandle);
            System.err.println("[mc-web/input] java mouse button handle=" + activeHandle + " button=" + button + " action=" + action + " x=" + cursorX + " y=" + cursorY + " callback=" + (callbacks != null && callbacks.mb != null));
            inputLogCount++;
        }
        if (button >= 0 && button < mouseButtons.length) {
            mouseButtons[button] = action != GLFW.GLFW_RELEASE;
        }
        Callbacks c = windows.get(activeHandle);
        if (c != null && c.mb != null) c.mb.invoke(activeHandle, button, action, mods);
    }

    private void handleScroll(double xoffset, double yoffset) {
        Callbacks c = windows.get(activeHandle);
        if (c != null && c.scr != null) c.scr.invoke(activeHandle, xoffset, yoffset);
    }

    private void handleFocusChange(boolean focused) {
        Callbacks c = windows.get(activeHandle);
        if (c != null && c.focus != null) {
            c.focus.invoke(activeHandle, focused);
        }
    }
}
