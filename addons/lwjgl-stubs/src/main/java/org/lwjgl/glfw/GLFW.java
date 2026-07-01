package org.lwjgl.glfw;

// Pure LWJGL stubs - TeaVM implementations live in webmc module

import org.lwjgl.glfw.callbacks.*;

public final class GLFW {
    public static final int GLFW_RELEASE = 0;
    public static final int GLFW_PRESS = 1;
    public static final int GLFW_REPEAT = 2;
    public static final int GLFW_CURSOR_NORMAL = 0;
    public static final int GLFW_CURSOR_HIDDEN = 0;
    public static final int GLFW_CURSOR_DISABLED = 0;
    public static final int GLFW_CURSOR = 0x00033001;
    public static final int GLFW_KEY_UNKNOWN = -1;
    public static final int GLFW_KEY_SPACE = 32;
    public static final int GLFW_KEY_ESCAPE = 256;
    public static final int GLFW_KEY_ENTER = 257;
    public static final int GLFW_KEY_TAB = 258;
    public static final int GLFW_KEY_BACKSPACE = 259;
    public static final int GLFW_KEY_INSERT = 260;
    public static final int GLFW_KEY_DELETE = 261;
    public static final int GLFW_KEY_RIGHT = 262;
    public static final int GLFW_KEY_LEFT = 263;
    public static final int GLFW_KEY_DOWN = 264;
    public static final int GLFW_KEY_UP = 265;
    public static final int GLFW_KEY_PAGE_UP = 266;
    public static final int GLFW_KEY_PAGE_DOWN = 267;
    public static final int GLFW_KEY_HOME = 268;
    public static final int GLFW_KEY_END = 269;
    public static final int GLFW_KEY_CAPS_LOCK = 280;
    public static final int GLFW_KEY_SCROLL_LOCK = 281;
    public static final int GLFW_KEY_NUM_LOCK = 282;
    public static final int GLFW_KEY_PRINT_SCREEN = 283;
    public static final int GLFW_KEY_PAUSE = 284;
    public static final int GLFW_KEY_F1 = 290;
    public static final int GLFW_KEY_F2 = 291;
    public static final int GLFW_KEY_F3 = 292;
    public static final int GLFW_KEY_F4 = 293;
    public static final int GLFW_KEY_F5 = 294;
    public static final int GLFW_KEY_F6 = 295;
    public static final int GLFW_KEY_F7 = 296;
    public static final int GLFW_KEY_F8 = 297;
    public static final int GLFW_KEY_F9 = 298;
    public static final int GLFW_KEY_F10 = 299;
    public static final int GLFW_KEY_F11 = 300;
    public static final int GLFW_KEY_F12 = 301;
    public static final int GLFW_KEY_SHIFT = 16;
    public static final int GLFW_KEY_CONTROL = 17;
    public static final int GLFW_KEY_ALT = 18;
    public static final int GLFW_MOD_SHIFT = 1;
    public static final int GLFW_MOD_CONTROL = 2;
    public static final int GLFW_MOD_ALT = 4;
    public static final int GLFW_MOUSE_BUTTON_LEFT = 0;
    public static final int GLFW_MOUSE_BUTTON_RIGHT = 1;
    public static final int GLFW_MOUSE_BUTTON_MIDDLE = 2;
    public static final int GLFW_JOYSTICK_1 = 0;

    private GLFW() {}

    public static native long glfwCreateWindow(int w, int h, String title, long monitor, long share);
    public static native void glfwDestroyWindow(long window);
    public static native void glfwMakeContextCurrent(long window);
    public static native void glfwSwapBuffers(long window);
    public static native void glfwPollEvents();
    public static native void glfwWaitEvents();
    public static native void glfwSwapInterval(int interval);
    public static native boolean glfwWindowShouldClose(long window);
    public static native void glfwSetWindowShouldClose(long window, boolean value);
    public static native String glfwGetClipboardString(long window);
    public static native void glfwSetClipboardString(long window, String text);
    public static native void glfwSetCursorPos(long window, double x, double y);
    public static native void glfwSetInputMode(long window, int mode, int value);
    public static native int glfwGetKey(long window, int key);
    public static native int glfwGetMouseButton(long window, int button);
    public static native double glfwGetCursorX(long window);
    public static native double glfwGetCursorY(long window);
    public static native long glfwGetPrimaryMonitor();
    public static native int glfwGetVideoMode(int monitor);
    public static native void glfwInit();
    public static native void glfwTerminate();
    public static native long glfwGetWindowUserPointer(long window);
    public static native void glfwSetWindowUserPointer(long window, long ptr);
    public static native long glfwGetMonitors();

    // ========== Callback Setters (implemented as native in webmc module) ==========

    public static native void glfwSetKeyCallback(long window, GLFWKeyCallback cb);
    public static native void glfwSetCharCallback(long window, GLFWCharCallback cb);
    public static native void glfwSetCharModsCallback(long window, GLFWCharModsCallback cb);
    public static native void glfwSetMouseButtonCallback(long window, GLFWMouseButtonCallback cb);
    public static native void glfwSetCursorPosCallback(long window, GLFWCursorPosCallback cb);
    public static native void glfwSetScrollCallback(long window, GLFWScrollCallback cb);
    public static native void glfwSetFramebufferSizeCallback(long window, GLFWFramebufferSizeCallback cb);
    public static native void glfwSetWindowFocusCallback(long window, GLFWWindowFocusCallback cb);
}
