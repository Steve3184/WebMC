package top.steve3184.webmc.game;

import org.teavm.jso.JSBody;
import top.steve3184.webmc.teavm.WebLog;

/**
 * WebMC 相机控制器
 * 支持 WASD 移动、鼠标视角控制、跳跃
 */
public class WebCamera {

    // 相机位置 (Minecraft 坐标)
    private float x, y, z;

    // 旋转角度 (弧度)
    private float yaw;   // 水平旋转
    private float pitch; // 垂直旋转

    // 移动速度
    private static final float MOVE_SPEED = 5.0f;
    private static final float SPRINT_MULTIPLIER = 1.5f;
    private static final float MOUSE_SENSITIVITY = 0.15f;

    // 跳跃相关
    private float velocityY = 0;
    private static final float GRAVITY = -25.0f;
    private static final float JUMP_VELOCITY = 8.0f;
    private boolean isOnGround = true;

    // 视角锁定状态
    private boolean isPointerLocked = false;

    public WebCamera() {
        // 默认位置：Y=70 在草地上方
        this.x = 0;
        this.y = 70;
        this.z = 0;
        this.yaw = 0;
        this.pitch = 0;
    }

    public WebCamera(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 0;
        this.pitch = 0;
    }

    /**
     * 更新相机状态
     */
    public void update(float deltaTime, boolean forward, boolean backward,
                       boolean left, boolean right, boolean jump, boolean sprint) {

        if (!isPointerLocked) {
            return;
        }

        float speed = MOVE_SPEED * (sprint ? SPRINT_MULTIPLIER : 1.0f);
        float dt = deltaTime;

        // 计算移动方向
        float moveX = 0, moveZ = 0;

        if (forward) {
            moveX -= (float) Math.sin(yaw);
            moveZ -= (float) Math.cos(yaw);
        }
        if (backward) {
            moveX += (float) Math.sin(yaw);
            moveZ += (float) Math.cos(yaw);
        }
        if (left) {
            moveX -= (float) Math.cos(yaw);
            moveZ += (float) Math.sin(yaw);
        }
        if (right) {
            moveX += (float) Math.cos(yaw);
            moveZ -= (float) Math.sin(yaw);
        }

        // 标准化移动向量
        float len = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (len > 0) {
            moveX /= len;
            moveZ /= len;
        }

        // 应用移动
        x += moveX * speed * dt;
        z += moveZ * speed * dt;

        // 跳跃物理
        if (jump && isOnGround) {
            velocityY = JUMP_VELOCITY;
            isOnGround = false;
        }

        velocityY += GRAVITY * dt;
        y += velocityY * dt;

        // 简单的地面碰撞检测
        if (y <= 62) {
            y = 62;
            velocityY = 0;
            isOnGround = true;
        }
    }

    /**
     * 处理鼠标移动
     */
    public void onMouseMove(double deltaX, double deltaY) {
        if (!isPointerLocked) {
            return;
        }

        yaw += deltaX * MOUSE_SENSITIVITY * 0.0174533f; // 转换为弧度
        pitch -= deltaY * MOUSE_SENSITIVITY * 0.0174533f;

        // 限制俯仰角度
        if (pitch > 89 * 0.0174533f) pitch = 89 * 0.0174533f;
        if (pitch < -89 * 0.0174533f) pitch = -89 * 0.0174533f;

        // 保持偏航在 0-360 范围内
        while (yaw < 0) yaw += 360 * 0.0174533f;
        while (yaw >= 360 * 0.0174533f) yaw -= 360 * 0.0174533f;
    }

    /**
     * 获取相机位置
     */
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }

    /**
     * 获取旋转角度 (弧度)
     */
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    /**
     * 获取前向向量
     */
    public float[] getLookVector() {
        float[] vec = new float[3];
        vec[0] = -(float) Math.sin(yaw) * (float) Math.cos(pitch);
        vec[1] = (float) Math.sin(pitch);
        vec[2] = -(float) Math.cos(yaw) * (float) Math.cos(pitch);
        return vec;
    }

    /**
     * 设置位置
     */
    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * 设置视角锁定状态
     */
    public void setPointerLocked(boolean locked) {
        this.isPointerLocked = locked;
    }

    public boolean isPointerLocked() {
        return isPointerLocked;
    }

    /**
     * 获取俯仰 (度数)
     */
    public float getPitchDegrees() {
        return (float) Math.toDegrees(pitch);
    }

    /**
     * 获取偏航 (度数)
     */
    public float getYawDegrees() {
        return (float) Math.toDegrees(yaw);
    }
}
