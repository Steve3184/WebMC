package top.steve3184.webmc.teavm.glfw;

import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

/**
 * Backend interface for GLFW window operations.
 * Implemented by teavm-runtime module.
 */
public interface WindowBackend {
    boolean init();
    void terminate();
    double time();
    void setTime(double t);

    long createWindow(int w, int h, String title);
    void destroyWindow(long win);
    void makeContextCurrent(long win);
    boolean shouldClose(long win);
    void setShouldClose(long win, boolean v);
    void setTitle(long win, String title);
    void setSize(long win, int w, int h);
    void getFramebufferSize(long win, int[] w, int[] h);
    void getWindowSize(long win, int[] w, int[] h);
    void setInputMode(long win, int mode, int value);
    int getInputMode(long win, int mode);
    boolean getKey(long win, int key);
    boolean getMouseButton(long win, int button);
    void getCursorPos(long win, double[] x, double[] y);
    void setCursorPos(long win, double x, double y);
    String getClipboard();
    void setClipboard(String value);

    // Callback setters (classic form)
    void setKeyCallback(long handle, GLFWKeyCallback callback);
    void setCharCallback(long handle, GLFWCharCallback callback);
    void setMouseButtonCallback(long handle, GLFWMouseButtonCallback callback);
    void setCursorPosCallback(long handle, GLFWCursorPosCallback callback);
    void setScrollCallback(long handle, GLFWScrollCallback callback);
    void setFramebufferSizeCallback(long handle, GLFWFramebufferSizeCallback callback);

    // Callback setters (I-interface form, used by MC)
    void setKeyCallbackI(long handle, org.lwjgl.glfw.GLFWKeyCallbackI callback);
    void setCharCallbackI(long handle, org.lwjgl.glfw.GLFWCharCallbackI callback);
    void setMouseButtonCallbackI(long handle, org.lwjgl.glfw.GLFWMouseButtonCallbackI callback);
    void setCursorPosCallbackI(long handle, org.lwjgl.glfw.GLFWCursorPosCallbackI callback);
    void setScrollCallbackI(long handle, org.lwjgl.glfw.GLFWScrollCallbackI callback);
    void setFramebufferSizeCallbackI(long handle, org.lwjgl.glfw.GLFWFramebufferSizeCallbackI callback);
}
