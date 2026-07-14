package top.steve3184.webmc.input;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import top.steve3184.webmc.game.WebCamera;
import top.steve3184.webmc.teavm.WebLog;

/**
 * WebMC 输入控制器
 * 处理键盘和鼠标事件
 */
public class WebInputManager {

    private WebCamera camera;
    private JSObject targetElement;

    // 键状态
    private boolean forward = false;
    private boolean backward = false;
    private boolean left = false;
    private boolean right = false;
    private boolean jump = false;
    private boolean sprint = false;

    // 鼠标状态
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean isMouseDown = false;

    // 初始化标记
    private boolean initialized = false;

    public WebInputManager(WebCamera camera) {
        this.camera = camera;
    }

    /**
     * 初始化输入监听
     */
    public void init(JSObject canvas) {
        if (initialized) return;

        this.targetElement = canvas;

        // 安装事件监听
        initKeyboardListeners();
        initMouseListeners();
        initPointerLock();

        initialized = true;
        WebLog.info("WebInputManager initialized");
    }

    @JSBody(params = {"manager", "canvas"}, script =
        "var canvas = typeof canvas === 'string' ? document.getElementById(canvas) : canvas;" +
        "if (!canvas) { console.error('Canvas not found'); return; }" +
        "" +
        "// Keyboard events" +
        "document.addEventListener('keydown', function(e) { " +
        "  var key = e.code || e.key;" +
        "  manager.onKeyDown(key);" +
        "  if (['KeyW','KeyA','KeyS','KeyD','Space','ShiftLeft','ShiftRight'].indexOf(key) >= 0) {" +
        "    e.preventDefault();" +
        "  }" +
        "}, false);" +
        "" +
        "document.addEventListener('keyup', function(e) { " +
        "  var key = e.code || e.key;" +
        "  manager.onKeyUp(key);" +
        "}, false);" +
        "" +
        "// Mouse click for pointer lock" +
        "canvas.addEventListener('click', function() { " +
        "  canvas.requestPointerLock();" +
        "}, false);" +
        "" +
        "// Pointer lock change" +
        "document.addEventListener('pointerlockchange', function() { " +
        "  manager.onPointerLockChange(document.pointerLockElement === canvas);" +
        "}, false);" +
        "" +
        "// Mouse move (only when pointer locked)" +
        "document.addEventListener('mousemove', function(e) { " +
        "  if (document.pointerLockElement === canvas) {" +
        "    manager.onMouseMove(e.movementX, e.movementY);" +
        "  }" +
        "}, false);" +
        "" +
        "console.log('[WebInputManager] Event listeners installed');"
    )
    private static native void installEventListeners(WebInputManager manager, JSObject canvas);

    private void initKeyboardListeners() {
        if (targetElement != null) {
            installEventListeners(this, targetElement);
        }
    }

    private void initMouseListeners() {
        // 通过 JavaScript 回调处理
    }

    private void initPointerLock() {
        // 通过 JavaScript 处理
    }

    /**
     * 键盘按下回调 (从 JS 调用)
     */
    public void onKeyDown(String key) {
        switch (key) {
            case "KeyW": forward = true; break;
            case "KeyS": backward = true; break;
            case "KeyA": left = true; break;
            case "KeyD": right = true; break;
            case "Space": jump = true; break;
            case "ShiftLeft":
            case "ShiftRight": sprint = true; break;
        }
    }

    /**
     * 键盘释放回调 (从 JS 调用)
     */
    public void onKeyUp(String key) {
        switch (key) {
            case "KeyW": forward = false; break;
            case "KeyS": backward = false; break;
            case "KeyA": left = false; break;
            case "KeyD": right = false; break;
            case "Space": jump = false; break;
            case "ShiftLeft":
            case "ShiftRight": sprint = false; break;
        }
    }

    /**
     * 鼠标移动回调 (从 JS 调用)
     */
    public void onMouseMove(double deltaX, double deltaY) {
        if (camera != null) {
            camera.onMouseMove(deltaX, deltaY);
        }
    }

    /**
     * 指针锁定状态变化
     */
    public void onPointerLockChange(boolean locked) {
        if (camera != null) {
            camera.setPointerLocked(locked);
        }
        WebLog.info("Pointer lock: " + (locked ? "enabled" : "disabled"));
    }

    /**
     * 更新输入状态
     */
    public void update(float deltaTime) {
        if (camera != null) {
            camera.update(deltaTime, forward, backward, left, right, jump, sprint);
        }
    }

    /**
     * 获取移动状态
     */
    public boolean isForward() { return forward; }
    public boolean isBackward() { return backward; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isJump() { return jump; }
    public boolean isSprint() { return sprint; }

    public void setCamera(WebCamera camera) {
        this.camera = camera;
    }
}
