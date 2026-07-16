package org.lwjgl.glfw;

import org.lwjgl.PointerBuffer;
import top.steve3184.webmc.teavm.glfw.WindowBackend;
import top.steve3184.webmc.teavm.glfw.WindowBackendHolder;

/**
 * Stub of {@code org.lwjgl.glfw.GLFW}. Window/input/cursor/clipboard funnel
 * through {@link WindowBackend} (DOM-event implementation in teavm-runtime).
 *
 * Constants are taken from GLFW 3.3 headers and MUST keep their numeric values
 * because MC stores them as raw ints in keybindings/save data.
 *
 * Many setXxxCallback methods are overloaded twice:
 *   1. Taking the *Callback abstract class (LWJGL classic)
 *   2. Taking the *CallbackI functional interface (used by MC's lambda/method-ref call sites)
 */
public final class GLFW {

    // ---- Boolean ----
    public static final int GLFW_TRUE  = 1;
    public static final int GLFW_FALSE = 0;
    public static final int GLFW_DONT_CARE = -1;

    // ---- Hint names ----
    public static final int GLFW_RESIZABLE       = 0x00020003;
    public static final int GLFW_VISIBLE         = 0x00020004;
    public static final int GLFW_DECORATED       = 0x00020005;
    public static final int GLFW_FOCUSED         = 0x00020001;
    public static final int GLFW_ICONIFIED       = 0x00020002;
    public static final int GLFW_MAXIMIZED       = 0x00020008;
    public static final int GLFW_CONTEXT_VERSION_MAJOR = 0x00022002;
    public static final int GLFW_CONTEXT_VERSION_MINOR = 0x00022003;
    public static final int GLFW_OPENGL_PROFILE        = 0x00022008;
    public static final int GLFW_OPENGL_FORWARD_COMPAT = 0x00022006;
    public static final int GLFW_OPENGL_DEBUG_CONTEXT  = 0x00022007;
    public static final int GLFW_OPENGL_CORE_PROFILE   = 0x00032001;

    // ---- Platform ----
    public static final int GLFW_PLATFORM_WIN32   = 0x00060001;
    public static final int GLFW_PLATFORM_COCOA   = 0x00060002;
    public static final int GLFW_PLATFORM_WAYLAND = 0x00060003;
    public static final int GLFW_PLATFORM_X11     = 0x00060004;
    public static final int GLFW_PLATFORM_NULL    = 0x00060005;
    public static final int GLFW_ANY_PLATFORM     = 0x00060000;

    // ---- Key actions ----
    public static final int GLFW_RELEASE = 0;
    public static final int GLFW_PRESS   = 1;
    public static final int GLFW_REPEAT  = 2;

    // ---- Mouse buttons ----
    public static final int GLFW_MOUSE_BUTTON_1      = 0;
    public static final int GLFW_MOUSE_BUTTON_2      = 1;
    public static final int GLFW_MOUSE_BUTTON_3      = 2;
    public static final int GLFW_MOUSE_BUTTON_LEFT   = GLFW_MOUSE_BUTTON_1;
    public static final int GLFW_MOUSE_BUTTON_RIGHT  = GLFW_MOUSE_BUTTON_2;
    public static final int GLFW_MOUSE_BUTTON_MIDDLE = GLFW_MOUSE_BUTTON_3;

    // ---- Cursor mode ----
    public static final int GLFW_CURSOR             = 0x00033001;
    public static final int GLFW_CURSOR_NORMAL      = 0x00034001;
    public static final int GLFW_CURSOR_HIDDEN      = 0x00034002;
    public static final int GLFW_CURSOR_DISABLED    = 0x00034003; // → Pointer Lock
    public static final int GLFW_RAW_MOUSE_MOTION   = 0x00033005;
    public static final int GLFW_STICKY_KEYS        = 0x00033002;
    public static final int GLFW_STICKY_MOUSE_BUTTONS = 0x00033003;

    // ---- Modifier keys ----
    public static final int GLFW_MOD_SHIFT     = 0x0001;
    public static final int GLFW_MOD_CONTROL   = 0x0002;
    public static final int GLFW_MOD_ALT       = 0x0004;
    public static final int GLFW_MOD_SUPER     = 0x0008;
    public static final int GLFW_MOD_CAPS_LOCK = 0x0010;
    public static final int GLFW_MOD_NUM_LOCK  = 0x0020;

    // ---- Key codes (ASCII printable range) ----
    public static final int GLFW_KEY_SPACE         = 32;
    public static final int GLFW_KEY_EXCLAM         = 33;   // !
    public static final int GLFW_KEY_QUOTE          = 39;   // '
    public static final int GLFW_KEY_COMMA          = 44;   // ,
    public static final int GLFW_KEY_MINUS          = 45;   // -
    public static final int GLFW_KEY_PERIOD         = 46;   // .
    public static final int GLFW_KEY_SLASH          = 47;   // /
    public static final int GLFW_KEY_0              = 48;
    public static final int GLFW_KEY_1              = 49;
    public static final int GLFW_KEY_2              = 50;
    public static final int GLFW_KEY_3              = 51;
    public static final int GLFW_KEY_4              = 52;
    public static final int GLFW_KEY_5              = 53;
    public static final int GLFW_KEY_6              = 54;
    public static final int GLFW_KEY_7              = 55;
    public static final int GLFW_KEY_8              = 56;
    public static final int GLFW_KEY_9              = 57;
    public static final int GLFW_KEY_SEMICOLON      = 59;   // ;
    public static final int GLFW_KEY_EQUAL          = 61;   // =
    public static final int GLFW_KEY_LEFT_BRACKET   = 91;   // [
    public static final int GLFW_KEY_BACKSLASH      = 92;   // \
    public static final int GLFW_KEY_RIGHT_BRACKET  = 93;   // ]
    public static final int GLFW_KEY_GRAVE_ACCENT   = 96;   // `
    public static final int GLFW_KEY_WORLD_1       = 161;  // non-US #1
    public static final int GLFW_KEY_WORLD_2       = 162;  // non-US #2

    // ---- Letter keys (A-Z) ----
    public static final int GLFW_KEY_A              = 65;
    public static final int GLFW_KEY_B              = 66;
    public static final int GLFW_KEY_C              = 67;
    public static final int GLFW_KEY_D              = 68;
    public static final int GLFW_KEY_E              = 69;
    public static final int GLFW_KEY_F              = 70;
    public static final int GLFW_KEY_G              = 71;
    public static final int GLFW_KEY_H              = 72;
    public static final int GLFW_KEY_I              = 73;
    public static final int GLFW_KEY_J              = 74;
    public static final int GLFW_KEY_K              = 75;
    public static final int GLFW_KEY_L              = 76;
    public static final int GLFW_KEY_M              = 77;
    public static final int GLFW_KEY_N              = 78;
    public static final int GLFW_KEY_O              = 79;
    public static final int GLFW_KEY_P              = 80;
    public static final int GLFW_KEY_Q              = 81;
    public static final int GLFW_KEY_R              = 82;
    public static final int GLFW_KEY_S              = 83;
    public static final int GLFW_KEY_T              = 84;
    public static final int GLFW_KEY_U              = 85;
    public static final int GLFW_KEY_V              = 86;
    public static final int GLFW_KEY_W              = 87;
    public static final int GLFW_KEY_X              = 88;
    public static final int GLFW_KEY_Y              = 89;
    public static final int GLFW_KEY_Z              = 90;

    // ---- Function keys ----
    public static final int GLFW_KEY_ESCAPE         = 256;
    public static final int GLFW_KEY_ENTER          = 257;
    public static final int GLFW_KEY_TAB            = 258;
    public static final int GLFW_KEY_BACKSPACE     = 259;
    public static final int GLFW_KEY_INSERT         = 260;
    public static final int GLFW_KEY_DELETE         = 261;
    public static final int GLFW_KEY_RIGHT          = 262;
    public static final int GLFW_KEY_LEFT           = 263;
    public static final int GLFW_KEY_DOWN           = 264;
    public static final int GLFW_KEY_UP             = 265;
    public static final int GLFW_KEY_PAGE_UP        = 266;
    public static final int GLFW_KEY_PAGE_DOWN      = 267;
    public static final int GLFW_KEY_HOME           = 268;
    public static final int GLFW_KEY_END            = 269;
    public static final int GLFW_KEY_CAPS_LOCK      = 280;
    public static final int GLFW_KEY_SCROLL_LOCK   = 281;
    public static final int GLFW_KEY_NUM_LOCK      = 282;
    public static final int GLFW_KEY_PRINT_SCREEN  = 283;
    public static final int GLFW_KEY_PAUSE         = 284;
    public static final int GLFW_KEY_F1             = 290;
    public static final int GLFW_KEY_F2             = 291;
    public static final int GLFW_KEY_F3             = 292;
    public static final int GLFW_KEY_F4             = 293;
    public static final int GLFW_KEY_F5             = 294;
    public static final int GLFW_KEY_F6             = 295;
    public static final int GLFW_KEY_F7             = 296;
    public static final int GLFW_KEY_F8             = 297;
    public static final int GLFW_KEY_F9             = 298;
    public static final int GLFW_KEY_F10            = 299;
    public static final int GLFW_KEY_F11            = 300;
    public static final int GLFW_KEY_F12            = 301;
    public static final int GLFW_KEY_F13            = 302;
    public static final int GLFW_KEY_F14            = 303;
    public static final int GLFW_KEY_F15            = 304;
    public static final int GLFW_KEY_F16            = 305;
    public static final int GLFW_KEY_F17            = 306;
    public static final int GLFW_KEY_F18            = 307;
    public static final int GLFW_KEY_F19            = 308;
    public static final int GLFW_KEY_F20            = 309;
    public static final int GLFW_KEY_F21            = 310;
    public static final int GLFW_KEY_F22            = 311;
    public static final int GLFW_KEY_F23            = 312;
    public static final int GLFW_KEY_F24            = 313;
    public static final int GLFW_KEY_F25            = 314;
    public static final int GLFW_KEY_KP_0           = 320;
    public static final int GLFW_KEY_KP_1           = 321;
    public static final int GLFW_KEY_KP_2           = 322;
    public static final int GLFW_KEY_KP_3           = 323;
    public static final int GLFW_KEY_KP_4           = 324;
    public static final int GLFW_KEY_KP_5           = 325;
    public static final int GLFW_KEY_KP_6           = 326;
    public static final int GLFW_KEY_KP_7           = 327;
    public static final int GLFW_KEY_KP_8           = 328;
    public static final int GLFW_KEY_KP_9           = 329;
    public static final int GLFW_KEY_KP_DECIMAL     = 330;
    public static final int GLFW_KEY_KP_DIVIDE      = 331;
    public static final int GLFW_KEY_KP_MULTIPLY    = 332;
    public static final int GLFW_KEY_KP_SUBTRACT   = 333;
    public static final int GLFW_KEY_KP_ADD        = 334;
    public static final int GLFW_KEY_KP_ENTER       = 335;
    public static final int GLFW_KEY_KP_EQUAL       = 336;
    public static final int GLFW_KEY_LEFT_SHIFT      = 340;
    public static final int GLFW_KEY_LEFT_CONTROL    = 341;
    public static final int GLFW_KEY_LEFT_ALT       = 342;
    public static final int GLFW_KEY_LEFT_SUPER     = 343;
    public static final int GLFW_KEY_RIGHT_SHIFT     = 344;
    public static final int GLFW_KEY_RIGHT_CONTROL   = 345;
    public static final int GLFW_KEY_RIGHT_ALT      = 346;
    public static final int GLFW_KEY_RIGHT_SUPER    = 347;
    public static final int GLFW_KEY_MENU          = 348;

    // ---- Lifecycle ----
    public static boolean glfwInit()                     { return WindowBackendHolder.current().init(); }
    public static void    glfwTerminate()                { WindowBackendHolder.current().terminate(); }
    public static void    glfwPollEvents()               {
        // mc-web: yield to the browser event loop so the tab doesn't freeze.
        // MC's render loop calls glfwPollEvents each frame; TeaVM implements
        // Thread.sleep asynchronously by suspending the green-thread via
        // setTimeout, which lets rAF, DOM events, and paint tick.
        try { Thread.sleep(1); } catch (InterruptedException ignored) {}
    }
    public static void    glfwWaitEvents()               {
        try { Thread.sleep(1); } catch (InterruptedException ignored) {}
    }
    public static void    glfwWaitEventsTimeout(double t){
        try { Thread.sleep(Math.max(1, (long)(t * 1000))); } catch (InterruptedException ignored) {}
    }
    public static void    glfwPostEmptyEvent()           { /* no-op */ }
    public static double  glfwGetTime()                  { return WindowBackendHolder.current().time(); }
    public static void    glfwSetTime(double t)          { WindowBackendHolder.current().setTime(t); }
    public static String  glfwGetVersionString()         { return "WebGL2-GLFW-Stub/0.1"; }
    public static int     glfwGetPlatform()              { return GLFW_PLATFORM_NULL; }
    public static int     glfwGetError(PointerBuffer description) { return 0; }

    // ---- Window ----
    public static long glfwCreateWindow(int w, int h, CharSequence title, long monitor, long share) {
        return WindowBackendHolder.current().createWindow(w, h, title == null ? "" : title.toString());
    }
    public static void glfwDestroyWindow(long win)             { WindowBackendHolder.current().destroyWindow(win); }
    public static void glfwMakeContextCurrent(long win)        { WindowBackendHolder.current().makeContextCurrent(win); }
    public static void glfwSwapBuffers(long win) {
        // WebGL 无 swapBuffers；用 glFinish 同步，禁用误用以规避 Context Lost
        if (win != 0L) WindowBackendHolder.current().makeContextCurrent(win);
        org.lwjgl.opengl.GL11.glFinish();
    }
    public static void glfwSwapInterval(int interval)          { /* no-op; controlled by requestAnimationFrame. */ }
    public static boolean glfwWindowShouldClose(long win)      { return WindowBackendHolder.current().shouldClose(win); }
    public static void glfwSetWindowShouldClose(long win, boolean v) { WindowBackendHolder.current().setShouldClose(win, v); }
    public static void glfwShowWindow(long win)                { /* no-op. */ }
    public static void glfwHideWindow(long win)                { /* no-op. */ }
    public static void glfwSetWindowTitle(long win, CharSequence title) { WindowBackendHolder.current().setTitle(win, title.toString()); }
    public static void glfwSetWindowSize(long win, int w, int h)        { WindowBackendHolder.current().setSize(win, w, h); }
    public static void glfwSetWindowPos(long win, int x, int y)         { /* no-op; canvas position is DOM-controlled. */ }
    public static void glfwSetWindowSizeLimits(long win, int minW, int minH, int maxW, int maxH) { /* no-op */ }
    public static void glfwSetWindowAspectRatio(long win, int n, int d) { /* no-op */ }
    public static int  glfwGetWindowAttrib(long win, int attrib)        { return 0; }
    public static void glfwSetWindowAttrib(long win, int attrib, int v) { /* no-op */ }
    public static long glfwGetWindowMonitor(long win)                   { return 0L; }
    public static void glfwSetWindowMonitor(long win, long mon, int x, int y, int w, int h, int rate) { /* no-op */ }
    public static void glfwSetWindowIcon(long win, GLFWImage.Buffer images) { /* no-op */ }
    public static void glfwIconifyWindow(long win)                      { /* no-op */ }
    public static void glfwRestoreWindow(long win)                      { /* no-op */ }
    public static void glfwMaximizeWindow(long win)                     { /* no-op */ }
    public static String glfwGetKeyName(int key, int scancode)          { return null; }
    public static int  glfwGetKeyScancode(int key)                      { return 0; }

    // ---- Hints ----
    public static void glfwDefaultWindowHints()           { /* no-op. */ }
    public static void glfwWindowHint(int hint, int value) { /* no-op; we always create a WebGL2 ctx. */ }
    public static void glfwWindowHintString(int hint, CharSequence v) { /* no-op */ }

    // ---- Framebuffer / window size queries (callers pass int[] of size 1) ----
    public static void glfwGetFramebufferSize(long win, int[] w, int[] h) {
        WindowBackendHolder.current().getFramebufferSize(win, w, h);
    }
    public static void glfwGetWindowSize(long win, int[] w, int[] h) {
        WindowBackendHolder.current().getWindowSize(win, w, h);
    }
    public static void glfwGetWindowPos(long win, int[] x, int[] y) {
        if (x != null && x.length > 0) x[0] = 0;
        if (y != null && y.length > 0) y[0] = 0;
    }
    public static void glfwGetWindowContentScale(long win, float[] xscale, float[] yscale) {
        if (xscale != null && xscale.length > 0) xscale[0] = 1.0f;
        if (yscale != null && yscale.length > 0) yscale[0] = 1.0f;
    }

    // ---- Cursor ----
    public static void glfwSetInputMode(long win, int mode, int value) {
        WindowBackendHolder.current().setInputMode(win, mode, value);
    }
    public static int  glfwGetInputMode(long win, int mode) {
        return WindowBackendHolder.current().getInputMode(win, mode);
    }
    public static boolean glfwRawMouseMotionSupported() { return false; }

    // ---- Key state polling ----
    public static int glfwGetKey(long win, int key) {
        return WindowBackendHolder.current().getKey(win, key) ? GLFW_PRESS : GLFW_RELEASE;
    }
    public static int glfwGetMouseButton(long win, int button) {
        return WindowBackendHolder.current().getMouseButton(win, button) ? GLFW_PRESS : GLFW_RELEASE;
    }
    public static void glfwGetCursorPos(long win, double[] x, double[] y) {
        WindowBackendHolder.current().getCursorPos(win, x, y);
    }
    public static void glfwSetCursorPos(long win, double x, double y) { /* no-op in browser. */ }

    // ---- Clipboard ----
    public static String glfwGetClipboardString(long win) { return WindowBackendHolder.current().getClipboard(); }
    public static void   glfwSetClipboardString(long win, CharSequence s) { WindowBackendHolder.current().setClipboard(s.toString()); }
    public static void   glfwSetClipboardString(long win, java.nio.ByteBuffer s) {
        if (s == null) return;
        byte[] arr = new byte[s.remaining()];
        s.duplicate().get(arr);
        WindowBackendHolder.current().setClipboard(new String(arr, java.nio.charset.StandardCharsets.UTF_8));
    }

    // ---- Error handling ----
    public static GLFWErrorCallback glfwSetErrorCallback(GLFWErrorCallback cb)  { return null; }
    public static GLFWErrorCallback glfwSetErrorCallback(GLFWErrorCallbackI cb) { return null; }

    // ---- Callback registration. Each has 2 overloads: abstract-class form and I-interface form. ----
    public static GLFWKeyCallback   glfwSetKeyCallback(long win, GLFWKeyCallback cb)         { WindowBackendHolder.current().setKeyCallback(win, cb); return null; }
    public static GLFWKeyCallback   glfwSetKeyCallback(long win, GLFWKeyCallbackI cb)        { WindowBackendHolder.current().setKeyCallbackI(win, cb); return null; }

    public static GLFWCharCallback  glfwSetCharCallback(long win, GLFWCharCallback cb)       { WindowBackendHolder.current().setCharCallback(win, cb); return null; }
    public static GLFWCharCallback  glfwSetCharCallback(long win, GLFWCharCallbackI cb)      { WindowBackendHolder.current().setCharCallbackI(win, cb); return null; }

    public static Object glfwSetCharModsCallback(long win, GLFWCharModsCallbackI cb) { return null; }

    public static GLFWMouseButtonCallback glfwSetMouseButtonCallback(long win, GLFWMouseButtonCallback cb) { WindowBackendHolder.current().setMouseButtonCallback(win, cb); return null; }
    public static GLFWMouseButtonCallback glfwSetMouseButtonCallback(long win, GLFWMouseButtonCallbackI cb) { WindowBackendHolder.current().setMouseButtonCallbackI(win, cb); return null; }

    public static GLFWCursorPosCallback   glfwSetCursorPosCallback(long win, GLFWCursorPosCallback cb)     { WindowBackendHolder.current().setCursorPosCallback(win, cb); return null; }
    public static GLFWCursorPosCallback   glfwSetCursorPosCallback(long win, GLFWCursorPosCallbackI cb)    { WindowBackendHolder.current().setCursorPosCallbackI(win, cb); return null; }

    public static GLFWScrollCallback      glfwSetScrollCallback(long win, GLFWScrollCallback cb)          { WindowBackendHolder.current().setScrollCallback(win, cb); return null; }
    public static GLFWScrollCallback      glfwSetScrollCallback(long win, GLFWScrollCallbackI cb)         { WindowBackendHolder.current().setScrollCallbackI(win, cb); return null; }

    public static GLFWFramebufferSizeCallback glfwSetFramebufferSizeCallback(long win, GLFWFramebufferSizeCallback cb) { WindowBackendHolder.current().setFramebufferSizeCallback(win, cb); return null; }
    public static GLFWFramebufferSizeCallback glfwSetFramebufferSizeCallback(long win, GLFWFramebufferSizeCallbackI cb) { WindowBackendHolder.current().setFramebufferSizeCallbackI(win, cb); return null; }

    public static GLFWWindowFocusCallback glfwSetWindowFocusCallback(long win, GLFWWindowFocusCallback cb)   { return null; }
    public static GLFWWindowFocusCallback glfwSetWindowFocusCallback(long win, GLFWWindowFocusCallbackI cb)  { return null; }

    public static GLFWWindowCloseCallback glfwSetWindowCloseCallback(long win, GLFWWindowCloseCallback cb)   { return null; }
    public static GLFWWindowCloseCallback glfwSetWindowCloseCallback(long win, GLFWWindowCloseCallbackI cb)  { return null; }

    public static Object glfwSetWindowSizeCallback(long win, GLFWWindowSizeCallbackI cb)    { return null; }
    public static Object glfwSetWindowPosCallback(long win, GLFWWindowPosCallbackI cb)      { return null; }
    public static Object glfwSetCursorEnterCallback(long win, GLFWCursorEnterCallbackI cb)  { return null; }
    public static Object glfwSetWindowIconifyCallback(long win, GLFWWindowIconifyCallbackI cb) { return null; }
    public static long glfwGetCurrentContext()                          { return 0L; }

    public static GLFWMonitorCallback glfwSetMonitorCallback(GLFWMonitorCallbackI cb) { return null; }
    public static Object glfwSetDropCallback(long win, GLFWDropCallback cb)                { return null; }
    public static Object glfwSetDropCallback(long win, GLFWDropCallbackI cb)               { return null; }
    public static Object glfwSetJoystickCallback(GLFWJoystickCallbackI cb)                 { return null; }

    // ---- Monitor / video mode (mostly stubs returning a single fake monitor) ----
    public static long glfwGetPrimaryMonitor()                 { return 1L; }
    public static PointerBuffer glfwGetMonitors() {
        PointerBuffer pb = PointerBuffer.allocateDirect(1);
        pb.put(0, 1L);
        pb.limit(1);
        return pb;
    }
    public static GLFWVidMode glfwGetVideoMode(long monitor)   { return GLFWVidMode.fakeFullscreen(); }
    public static GLFWVidMode.Buffer glfwGetVideoModes(long monitor) {
        return new GLFWVidMode.Buffer(new GLFWVidMode[]{ GLFWVidMode.fakeFullscreen() });
    }
    public static void glfwGetMonitorPos(long monitor, int[] x, int[] y) {
        if (x != null && x.length > 0) x[0] = 0;
        if (y != null && y.length > 0) y[0] = 0;
    }
    public static void glfwGetMonitorPhysicalSize(long monitor, int[] w, int[] h) {
        if (w != null && w.length > 0) w[0] = 600;
        if (h != null && h.length > 0) h[0] = 340;
    }
    public static String glfwGetMonitorName(long monitor)      { return "Browser Window"; }
    public static void glfwGetMonitorContentScale(long monitor, float[] xscale, float[] yscale) {
        if (xscale != null && xscale.length > 0) xscale[0] = 1.0f;
        if (yscale != null && yscale.length > 0) yscale[0] = 1.0f;
    }

    private GLFW() {}
}
