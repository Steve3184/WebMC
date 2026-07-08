package top.steve3184.webmc.teavm.glfw;

/**
 * Pure-Java interface that GLFW stubs delegate to. Concrete impl in
 * teavm-runtime wires DOM events, performance.now, navigator.clipboard.
 */
public interface WindowBackend {
    boolean init();
    void terminate();
    double time();
    void setTime(double t);

    long createWindow(int w, int h, String title);
    void destroyWindow(long handle);
    void makeContextCurrent(long handle);
    boolean shouldClose(long handle);
    void setShouldClose(long handle, boolean v);
    void setTitle(long handle, String title);
    void setSize(long handle, int w, int h);
    void getFramebufferSize(long handle, int[] w, int[] h);
    void getWindowSize(long handle, int[] w, int[] h);

    void setInputMode(long handle, int mode, int value);
    int  getInputMode(long handle, int mode);

    boolean getKey(long handle, int key);
    boolean getMouseButton(long handle, int button);
    void    getCursorPos(long handle, double[] x, double[] y);

    String getClipboard();
    void   setClipboard(String s);

    void setKeyCallback(long handle, org.lwjgl.glfw.GLFWKeyCallback cb);
    void setCharCallback(long handle, org.lwjgl.glfw.GLFWCharCallback cb);
    void setMouseButtonCallback(long handle, org.lwjgl.glfw.GLFWMouseButtonCallback cb);
    void setCursorPosCallback(long handle, org.lwjgl.glfw.GLFWCursorPosCallback cb);
    void setScrollCallback(long handle, org.lwjgl.glfw.GLFWScrollCallback cb);
    void setFramebufferSizeCallback(long handle, org.lwjgl.glfw.GLFWFramebufferSizeCallback cb);

    // I-interface forms used by MC's lambda/method-ref call sites
    void setKeyCallbackI(long handle, org.lwjgl.glfw.GLFWKeyCallbackI cb);
    void setCharCallbackI(long handle, org.lwjgl.glfw.GLFWCharCallbackI cb);
    void setMouseButtonCallbackI(long handle, org.lwjgl.glfw.GLFWMouseButtonCallbackI cb);
    void setCursorPosCallbackI(long handle, org.lwjgl.glfw.GLFWCursorPosCallbackI cb);
    void setScrollCallbackI(long handle, org.lwjgl.glfw.GLFWScrollCallbackI cb);
    void setFramebufferSizeCallbackI(long handle, org.lwjgl.glfw.GLFWFramebufferSizeCallbackI cb);
}
