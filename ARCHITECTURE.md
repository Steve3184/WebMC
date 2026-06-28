# WebMC 架构说明

WebMC 的核心目标是把 Minecraft 1.21.8 通过 TeaVM 编译后在浏览器运行，并保持源码改动可追踪、可重建。

## 目录角色

```text
work/       构建工作树（Java 编译与 TeaVM 输出）
patches/    对上游源码的补丁（长期维护资产）
addons/     Web 运行时桥接与 stub
            ├── web/           浏览器侧壳与静态资源（index.html, bootstrap.js 等）
            ├── teavm-runtime/ TeaVM Java→JS 桥接
            └── lwjgl-stubs/   LWJGL API 桩（GL/OpenAL/STB）
scripts/    初始化、补丁应用、构建辅助脚本
docs/       规范、状态、发布门槛
```

## 构建链路

1. 应用补丁：`scripts/apply-patches.sh`
2. 生成/编译：Gradle + TeaVM（`work/build.gradle`）
3. 打包资源：`scripts/build-vfs.mjs`
4. 代码分割（Phase 2）：`scripts/split-js.mjs`
5. 运行验收：`runtimeCheckMcMainPhase197*`

## 关键约束

- `work/` 只作为构建工作树，不作为长期手工修改面。
- 浏览器入口是 `addons/web/index.html` + `addons/web/bootstrap.js`，游戏主体由构建产物加载。
- 资源以 VFS 形式提供，运行时按需读取。
- 网络路径走 Web 兼容通道，不依赖原生桌面网络栈。
- 发布验收以 `docs/PRODUCT_READINESS.md` 的自动化门槛为准。

## 阶段规划

### Phase 1 (已完成)
- TeaVM Java→JS 编译链路
- WebGL2 后端实现
- 基础渲染管道
- world loading 通过 phase-197 验收

### Phase 2 (计划中)
- VFS IndexedDB 缓存（首次 6-15s → 二次 0.5-1s）
- game.js 代码分割（70MB → 8-10MB chunks）
- WASM 音频解码
- OpenAL 全功能实现

## 维护原则

- 先改 `patches/` 和 `addons/`，再通过脚本重建 `work/`。
- 避免提交构建产物和本地运行证据。
- 变更优先小步、可回归、可复现。
