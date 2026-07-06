# WebMC - Minecraft in Your Browser

将 Minecraft 1.21.8 编译为 WebAssembly，在浏览器中运行。

![WebMC](webmc-preview.png)

## 功能特性

- 🎮 **浏览器运行** - 无需安装，直接在网页中游玩 Minecraft
- 🌐 **跨平台** - 支持 Windows、Linux、macOS
- 🔧 **可扩展** - 模块化架构，易于添加新功能
- ⚡ **现代化** - 基于 TeaVM WebAssembly 编译

## 技术架构

```
MCP-Reborn (Minecraft 源码)
    ↓ TeaVM 编译
    ↓ + WebGL/LWJGL Stubs
浏览器可运行版本
```

### 核心模块

| 模块 | 说明 |
|------|------|
| `addons/lwjgl-stubs/` | GLFW API stub (浏览器事件映射) |
| `addons/blaze3d-impl/` | 3D 渲染实现 |
| `addons/teavm-runtime/` | TeaVM 运行时支持 |
| `addons/web/` | Web 前端入口 |

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- Gradle 8+
- 现代浏览器 (Chrome 90+, Firefox 90+, Safari 16+)

### 构建

```bash
# 克隆项目
git clone https://github.com/Steve3184/WebMC.git
cd WebMC

# 初始化子模块
git submodule update --init --recursive

# 一键构建 (Linux/macOS)
bash scripts/dev-wsl.sh all

# 一键构建 (Windows PowerShell)
.\scripts\dev.ps1 all

# 仅构建
bash scripts/dev-wsl.sh build

# 仅启动开发服务器
bash scripts/dev-wsl.sh serve
```

### 开发

```bash
# 同步 upstream (MCP-Reborn)
bash scripts/dev-wsl.sh sync

# 应用 patches
bash scripts/dev-wsl.sh patch

# 查看帮助
bash scripts/dev-wsl.sh help
```

### 访问

构建完成后，打开 `build/web-run/index.html` 或启动开发服务器后访问 `http://localhost:8080`

## 目录结构

```
WebMC/
├── addons/              # WebMC 特有代码
│   ├── lwjgl-stubs/    # LWJGL/GLFW stub
│   ├── blaze3d-impl/   # 渲染实现
│   ├── teavm-runtime/  # 运行时
│   └── web/            # Web 入口
├── build/               # 构建输出
├── patches/             # 针对 MC 源码的补丁
├── scripts/             # 构建脚本
├── work/                # MCP-Reborn (子模块)
└── upstream/            # 上游引用
```

## 工作流程

### 更新 Minecraft 版本

```bash
# 1. 更新 submodule 指向新版本
cd work && git fetch origin && git checkout <new-tag>
cd .. && git add work && git commit -m "chore: update to MC x.xx"

# 2. 重新导出 patches
cd work && ../gradlew exportPatches
cd .. && git add patches && git commit

# 3. 重新构建
bash scripts/dev-wsl.sh build
```

### 添加新 Stub

1. 在 `addons/<module>/src/main/java/` 创建 Java 文件
2. 实现对应的 GLFW/LWJGL 接口
3. 重新构建测试

## 技术细节

### 输入处理

```
浏览器事件 (keydown, click, mousemove)
    ↓
GLFWCallbacks (GLFWKeyCallback, GLFWMouseButtonCallback, etc.)
    ↓
Minecraft InputSystem
```

### 渲染管线

```
Minecraft RenderSystem
    ↓
Blaze3D (3D 渲染抽象)
    ↓
WebGL (浏览器)
```

## 许可证

本项目基于 [MCP-Reborn](https://github.com/Hexeption/MCP-Reborn) 构建。

**重要**: 禁止发布由本工具生成的任何代码。Minecraft 代码受 Mojang 许可证保护。

## 致谢

- [MCP-Reborn](https://github.com/Hexeption/MCP-Reborn) - Minecraft Mod Coder Pack
- [TeaVM](https://teavm.org/) - Java to WebAssembly/JS 编译器
- [LWJGL](https://www.lwjgl.org/) - Lightweight Java Game Library

## 链接

- [GitHub](https://github.com/Steve3184/WebMC)
- [Issues](https://github.com/Steve3184/WebMC/issues)
- [Discussions](https://github.com/Steve3184/WebMC/discussions)
