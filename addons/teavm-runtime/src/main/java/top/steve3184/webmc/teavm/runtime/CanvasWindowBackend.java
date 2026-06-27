package top.steve3184.webmc.teavm.runtime;

import java.util.HashMap;
import java.util.Map;
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

    private static final class Callbacks {
        GLFWKeyCallback key;
        GLFWCharCallback character;
        GLFWMouseButtonCallback mouseButton;
        GLFWCursorPosCallback cursorPos;
        GLFWScrollCallback scroll;
        GLFWFramebufferSizeCallback framebufferSize;
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public void terminate() {
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
        return handle;
    }

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
        if (width != null && width.length > 0) {
            width[0] = 1280;
        }
        if (height != null && height.length > 0) {
            height[0] = 720;
        }
    }

    @Override
    public void getWindowSize(long handle, int[] width, int[] height) {
        getFramebufferSize(handle, width, height);
    }

    @Override
    public void setInputMode(long handle, int mode, int value) {
    }

    @Override
    public int getInputMode(long handle, int mode) {
        return mode == GLFW.GLFW_CURSOR ? GLFW.GLFW_CURSOR_NORMAL : 0;
    }

    @Override
    public boolean getKey(long handle, int key) {
        return false;
    }

    @Override
    public boolean getMouseButton(long handle, int button) {
        return false;
    }

    @Override
    public void getCursorPos(long handle, double[] x, double[] y) {
        if (x != null && x.length > 0) {
            x[0] = 0.0;
        }
        if (y != null && y.length > 0) {
            y[0] = 0.0;
        }
    }

    @Override
    public String getClipboard() {
        return "";
    }

    @Override
    public void setClipboard(String value) {
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
}
