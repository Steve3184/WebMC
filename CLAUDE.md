# WebMC 开发规范

本文档为 Claude Code 等 AI 工具提供项目上下文和开发指南。

## 项目概述

WebMC 是一个将 Minecraft 1.21.8 编译为 WebAssembly 并在浏览器中运行的项目。

### 核心架构

```
Minecraft 源码 (work/ 子模块)
    ↓
TeaVM 编译 (Java → WASM/JS)
    ↓
WebMC 桥接层 (addons/)
    ↓
浏览器 (WebGL + DOM 事件)
```

### 关键技术栈

- **TeaVM**: Java → WebAssembly 编译器
- **GLFW Stub**: 浏览器事件到 GLFW API 的映射
- **Blaze3D**: 3D 渲染抽象层
- **Gradle**: 构建系统

## 目录约定

| 目录 | 用途 |
|------|------|
| `addons/` | WebMC 特有代码 (Web 桥接、Stub) |
| `work/` | MCP-Reborn 子模块 (只读上游) |
| `upstream/` | Minecraft 上游引用 |
| `patches/` | 针对 work 的补丁文件 |
| `build/web-run/` | TeaVM 构建输出 |
| `scripts/` | 构建脚本 |

## 模块职责

### addons/lwjgl-stubs/

**职责**: GLFW API stub 实现

浏览器没有原生 GLFW，需要将浏览器事件转换为 GLFW 调用。

```
addons/lwjgl-stubs/src/main/java/org/lwjgl/glfw/
├── GLFW.java                 # 主类，静态方法派发到 WindowBackend
├── Callbacks.java            # 回调基类
├── callbacks/                # 旧版回调 (已废弃)
└── *CallbackI.java          # 回调接口 (MC 使用)
```

**关键接口**:
```java
public interface WindowBackend {
    void onKey(long window, int key, int scancode, int action, int mods);
    void onMouseButton(long window, int button, int action, int mods);
    void onMouseMove(long window, double x, double y);
    void void onScroll(long window, double x, double y);
}
```

### addons/teavm-runtime/

**职责**: TeaVM 运行时适配

- `glfw/WindowBackend.java` - 实际实现
- `glfw/WindowBackendHolder.java` - 单例管理

### addons/web/

**职责**: Web 前端入口

- HTML 加载
- Canvas 初始化
- 事件监听绑定

## 代码规范

### Java 命名

- **包名**: `top.steve3184.webmc.teavm.<模块>.<子模块>`
- **类名**: 驼峰式，如 `WindowBackendHolder`
- **接口**: 带 `I` 后缀表示回调，如 `GLFWKeyCallbackI`

### TeaVM 兼容

某些 Java 特性需要避免或特殊处理：

| 限制 | 替代方案 |
|------|----------|
| 反射受限 | 使用 `@TeavmMethodReplacement` 或直接调用 |
| 线程不存在 | 使用 `requestAnimationFrame` 模拟 |
| File I/O | 使用 IndexedDB/VirtualFS |
| 原生库 | 全部 stub 为 JS 实现 |

### 输入事件处理

GLFW 回调是 MC 输入系统的核心。确保：

1. 事件派发顺序正确 (key → char → mouse)
2. 修饰键 (Shift/Ctrl/Alt) 正确传递
3. 坐标转换正确 (CSS → GLFW 坐标系)

## Git 工作流

### 分支策略

```
main          # 稳定版本
├── develop   # 开发分支 (如需要)
└── feature/* # 功能分支
```

### 提交规范

使用 Conventional Commits:

```
feat: 添加新功能
fix: 修复 bug
chore: 维护任务
docs: 文档更新
refactor: 重构
test: 测试
```

### 子模块管理

**work/** 是 MCP-Reborn 子模块，只读。

```bash
# 更新到新版本
git submodule update --remote work
git add work
git commit -m "chore(work): update to MCP Reborn x.x.x"
```

### Patches 管理

```bash
# 导出 patches (修改 work 后)
cd work && ./gradlew exportPatches

# 应用 patches
./gradlew importPatches
```

## 构建命令

```bash
# 完整构建
./gradlew build

# 仅 TeaVM (Web 版本)
./gradlew compileTeavm

# 仅客户端
./gradlew compileClient

# 启动开发服务器
./gradlew run
```

## 常见问题

### Q: TeaVM 编译失败

检查：
1. Java 版本是否为 21+
2. `addons/` 模块是否正确引入
3. 是否有未实现的 stub 方法

### Q: 浏览器白屏

1. 检查控制台错误
2. 确认 WebGL 是否启用
3. 检查 VFS 资源加载

### Q: 输入不工作

1. 检查 GLFW 回调是否注册
2. 检查 `WindowBackend` 实现
3. 检查浏览器事件是否正确派发

## 调试技巧

### 浏览器端

```javascript
// 在 bootstrap.js 中添加日志
window.debug = {
    input: (e) => console.log('Input:', e),
    render: () => console.log('Render tick')
};
```

### TeaVM 端

```java
// 使用 System.out 调试
@JSBody(params = {}, script = "console.log('Hello from Java!')")
static native void log();
```

## 资源

- [TeaVM 文档](https://teavm.org/docs/)
- [LWJGL 文档](https://www.lwjgl.org/documentation)
- [MCP-Reborn](https://github.com/Hexeption/MCP-Reborn)
