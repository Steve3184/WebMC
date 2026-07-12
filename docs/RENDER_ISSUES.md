# WebMC 渲染问题诊断与修复

## 当前渲染架构

```
WebGpuDevice         - 设备管理、流水线编译
    ↓
WebCompiledRenderPipeline - 着色器编译、WebGL 程序管理
    ↓
WebRenderPass        - 渲染状态、绘制调用
    ↓
WebCommandEncoder    - 命令编码、纹理上传
    ↓
WebGpuTexture        - WebGL 纹理、FBO
WebGpuBuffer         - VBO/IBO/UBO 缓冲区
```

## 1. 🟢 着色器编译错误

### 问题描述
GLSL 语法不兼容导致着色器编译失败

### 当前处理
`GlslTranslator.java` 已处理:
- `1.0f` → `1.0` (float 后缀移除)
- `ivec2 / float` → `vec2(ivec2) / float`
- `vec2 = ivec2` → `vec2 = vec2(ivec2)`
- `samplerBuffer` → `sampler2D` (通过 texelFetch 模拟)
- 浮点/整数混合运算

### 潜在问题
```java
// GlslTranslator.java:67-73
if (source.contains("isamplerBuffer") || source.contains("usamplerBuffer")) {
    return null;  // 拒绝编译含整数 samplerBuffer 的着色器
}
```

**影响**: 如果 MC 使用 `isamplerBuffer`/`usamplerBuffer`，着色器会完全跳过渲染。

### 解决方案
如遇到 samplerBuffer 问题，需要实现软件模拟:
```java
// 替代方案：在纹理上传时将整数数据编码到浮点纹理
// 然后在着色器中使用位运算提取
```

## 2. 🔵 渲染黑屏/白屏

### 常见原因

#### 2.1 缓冲区未上传
```java
// WebGpuBuffer.java:83-108
public void uploadAt(int offset, ByteBuffer data) {
    // 检查诊断日志中是否有 "uploadAt" 记录
    // 如果没有，说明数据从未上传到 GPU
}
```

**诊断**: 开启诊断后查看 `uploadAt` 日志

#### 2.2 纹理未创建
```java
// WebGpuTexture.java:34-60
public void ensureGlTexture(WebGL2RenderingContext gl) {
    if (glTex != null) return;  // 懒创建
    // ...
}
```

**诊断**: 检查 `glTex` 是否为 null

#### 2.3 UBO 绑定失败
```java
// WebRenderPass.java:262-268
if (isValidUboRange(offset, length, wb.glAllocSize())) {
    bindBufferRange(gl, ..., wb, offset, length);
} else {
    bindBufferBase(gl, ..., wb);  // 回退到 base bind
}
```

**诊断**: 检查 `isValidUboRange` 返回 false 的情况

#### 2.4 FBO 不完整
```java
// WebRenderPass.java:80-88
int status = gl.checkFramebufferStatus(...);
if (status != WebGL2RenderingContext.FRAMEBUFFER_COMPLETE) {
    // 报告 FBO 错误
}
```

**诊断**: 检查日志中的 `FBO incomplete` 消息

### 深度测试问题
```java
// WebRenderPass.java:164-168
// 关键修复：当混合启用时不写入深度缓冲区
if (blendingEnabled) {
    gl.depthMask(false);  // 防止透明物体深度冲突
} else {
    gl.depthMask(p.isWriteDepth());
}
```

## 3. 🟡 渲染闪烁/撕裂

### 3.1 深度冲突 (Z-fighting)
**原因**: 多个面片在同一深度绘制

**当前处理**:
```java
// WebRenderPass.java:183-190
float biasScale = p.getDepthBiasScaleFactor();
float biasConst = p.getDepthBiasConstant();
if (biasScale != 0.0f || biasConst != 0.0f) {
    gl.enable(WebGL2RenderingContext.POLYGON_OFFSET_FILL);
    gl.polygonOffset(biasScale, biasConst);
}
```

### 3.2 透明物体排序
**问题**: 透明物体需要从后到前排序才能正确渲染

**当前状态**: MC 使用多通道渲染处理透明物体

### 3.3 闪烁的 UI 元素
**可能原因**:
- 视口未正确设置
- 混合模式不正确

## 4. 🟠 性能问题

### 4.1 绘制调用过多
```java
// WebRenderPass.java 统计
private static int drawCallCount = 0;
private static int setPipelineCount = 0;
```

**诊断**: 查看每帧 `drawCallCount` 值

### 4.2 着色器编译开销
```java
// WebGpuDevice.java 流水线缓存
private final Map<RenderPipeline, WebCompiledRenderPipeline> pipelineCache = new ConcurrentHashMap<>();
```

**优化**: 已实现缓存机制

### 4.3 纹理上传频繁
```java
// WebGpuBuffer.java 统计
private static int uploadCount = 0;
private static long uploadBytes = 0;
```

## 5. 🔴 特定材质渲染错误

### 5.1 水和透明物体
**关键代码**:
```java
// WebRenderPass.java:159-168
// CRITICAL FIX: 当混合启用时不写入深度缓冲区
if (blendingEnabled) {
    gl.depthMask(false);
}
```

**原理**: 透明物体不应该写入深度缓冲区，否则会导致其他透明物体被遮挡

### 5.2 天空盒
**问题**: 可能是 samplerBuffer 使用导致的

### 5.3 粒子效果
**可能问题**:
- 混合模式不正确
- 深度写入冲突

## 诊断工具

### 开启诊断模式
在 `index.html` 或 URL 参数中添加:
```javascript
window.webmcDiagnostics = true;
// 或 URL: ?diagnostics=1
```

### 查看诊断日志
```
[mc-web/diag] frame=X draws=Y setPipeline=Z ...
[mc-web/diag] uploadAt #N target=VBO size=X ...
[mc-web/diag] drawIndexed #1 pipeline=... idxCount=X
[mc-web/gl] setPipeline null program ...
```

### 常见错误码
| 错误码 | 含义 | 解决方案 |
|--------|------|----------|
| 0x0502 | INVALID_ENUM | 检查纹理格式参数 |
| 0x0506 | INVALID_VALUE | 检查缓冲区偏移/大小 |
| 0x0507 | INVALID_OPERATION | 检查操作顺序 |
| 0x0500 | NO_ERROR | 无错误 |
| 0x8CD5 | FRAMEBUFFER_INCOMPLETE | 检查 FBO 配置 |

## 修复清单

### 已验证修复
- ✅ 透明物体深度写入 (`depthMask(false)`)
- ✅ 着色器翻译 (GLSL → GLSL ES)
- ✅ UBO 对齐 (256 字节)
- ✅ 纹理格式映射

### 待优化
- ⬜ 整数 samplerBuffer 支持
- ⬜ 多采样 (MSAA) 支持
- ⬜ 后期处理效果

## 测试方法

### 本地测试
```bash
./gradlew compileTeavm
# 然后在浏览器中打开 build/web-run/index.html
```

### 诊断模式
```javascript
// 浏览器控制台
window.webmcDiagnostics = true;
location.reload();
```

### 检查 WebGL 状态
```javascript
// 控制台执行
const gl = document.getElementById('game-canvas').getContext('webgl2');
console.log('GL Vendor:', gl.getParameter(gl.VENDOR));
console.log('GL Renderer:', gl.getParameter(gl.RENDERER));
console.log('GL Version:', gl.getParameter(gl.VERSION));
console.log('Max Texture Size:', gl.getParameter(gl.MAX_TEXTURE_SIZE));
```
