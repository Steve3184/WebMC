# 渲染系统改进建议

## 1. VAO 管理改进

### 当前状态
```java
// WebMain.java - 创建并绑定默认 VAO
bindDefaultVao(gl);  // 立即模式 VAO

// WebRenderPass.java - 直接设置顶点属性
gl.vertexAttribPointer(loc, count, glType, normalized, stride, offset);
gl.enableVertexAttribArray(loc);
```

### 建议改进
WebGL2 要求使用 VAO，但当前实现没有显式管理 VAO 状态。考虑：

```java
// 在 WebRenderPass 构造函数中创建专用 VAO
public WebRenderPass(...) {
    // 创建渲染通道专用 VAO
    int vaoId = gl.createVertexArray();
    vaoCache.put(this, vaoId);
}
```

## 2. 实例化渲染支持

### 当前状态
```java
// WebRenderPass.java
if (instanceCount > 1) {
    gl.drawElementsInstanced(mode, indexCount, indexGlType, (int) offset, instanceCount);
} else {
    gl.drawElements(mode, indexCount, indexGlType, (int) offset);
}
```

### 缺失功能
没有调用 `vertexAttribDivisor` 来支持实例化渲染的顶点属性：

```java
// 需要在 setVertexBuffer 中添加
if (instanceCount > 1) {
    gl.vertexAttribDivisor(loc, 1);  // 每实例更新一次
}
```

## 3. 深度缓冲区格式

### 当前状态
```java
// WebGpuTexture.java
case DEPTH32: return 0x8CAC; // GL_DEPTH_COMPONENT32F
```

### 建议
Minecraft 可能使用 DEPTH24_STENCIL8，但 WebGL2 支持的深度格式有限：

```java
// 可选的深度格式
case DEPTH24: return 0x81A6; // GL_DEPTH_COMPONENT24 (如果支持)
// 或使用 DEPTH32F_STENCIL8 (0x8CAD)
```

## 4. 纹理压缩支持

### 当前状态
只支持 RGBA8/RED8/RED8I/DEPTH32

### 缺失格式
- S3TC/DXT 压缩纹理 (WebGL1)
- ASTC 压缩纹理 (WebGL2)
- ETC2 压缩纹理 (WebGL2)

### 解决方案
```java
// 添加纹理压缩格式支持
public static int internalFormatFor(TextureFormat fmt) {
    switch (fmt) {
        // ...
        case BC1: return 0x83F0; // GL_COMPRESSED_RGBA_S3TC_DXT1_EXT
        case BC3: return 0x83F2; // GL_COMPRESSED_RGBA_S3TC_DXT5_EXT
        case BC7: return 0x8E8C; // GL_COMPRESSED_RGBA_BPTC_UNORM
        default: return 0x8058; // GL_RGBA8
    }
}
```

## 5. 多采样 (MSAA)

### 当前状态
没有 MSAA 支持

### 建议
```java
// 在创建 RenderPass 时配置采样数
public WebRenderPass(..., int samples) {
    if (samples > 1) {
        gl.getExtension('EXT_multisampled_render_to_texture');
        // 配置 MSAA
    }
}
```

## 6. 后期处理效果

### 当前状态
没有后期处理管线

### 建议
```java
// 添加后期处理渲染通道
public class WebPostProcessPass {
    private WebGpuTexture colorTexture;
    private WebGpuTexture bloomTexture;
    // 模糊、泛光、色调映射等
}
```

## 7. 性能优化

### 7.1 批量绘制
```java
// 当前：每帧可能有多次 draw 调用
// 建议：使用 glMultiDrawElements 批量处理
gl.multiDrawElements(mode, firstIndices, indexGlType, ...);
```

### 7.2 纹理数组
```java
// 使用纹理数组减少绑定调用
gl.activeTexture(UNIT);
gl.bindTexture(TEXTURE_2D_ARRAY, atlasArray);
```

### 7.3 GPU 像素回读优化
```java
// WebCommandEncoder.java 中的 readPixels 是同步的，性能开销大
// 建议：使用 requestAnimationFrame 延迟读取或使用 WebGL lost context 恢复
```

## 8. 常见问题修复

### 8.1 透明物体渲染顺序
```java
// 当前代码已处理 depthMask(false) 用于透明物体
// 但需要确保 MC 的渲染顺序正确
```

### 8.2 天空盒渲染
```java
// 检查是否使用了 samplerBuffer
// 如果使用了，需要在 GlslTranslator 中添加替代实现
```

### 8.3 粒子效果
```java
// 粒子通常使用混合模式 SRC_ALPHA, ONE_MINUS_SRC_ALPHA
// 确保 applyState 正确处理混合函数
```

## 9. 调试工具增强

### 9.1 添加渲染状态快照
```java
public static String getRenderStateDump(WebGL2RenderingContext gl) {
    return String.format(
        "Viewport: %d,%d,%d,%d\n" +
        "DepthTest: %b\n" +
        "DepthMask: %b\n" +
        "Blend: %b\n" +
        "Cull: %b\n" +
        "Scissor: %b\n",
        // ... 获取当前状态
    );
}
```

### 9.2 着色器错误恢复
```java
// 当着色器编译失败时，使用备用着色器
if (compiled.program() == null) {
    return compileFallbackShader(pipeline);
}
```

## 10. 测试清单

- [ ] 验证着色器编译无错误
- [ ] 检查缓冲区数据上传
- [ ] 验证纹理格式映射正确
- [ ] 测试透明物体渲染
- [ ] 验证深度测试工作
- [ ] 检查实例化渲染
- [ ] 测试天空盒
- [ ] 验证 UI 渲染
- [ ] 性能测试 (FPS 计数器)
- [ ] WebGL 上下文丢失恢复
