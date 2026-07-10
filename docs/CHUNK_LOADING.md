# WebMC 区块加载和地形渲染系统

## 概述

WebMC 使用 Minecraft 原生的基于区块的地形渲染系统，通过 TeaVM 编译后在浏览器中运行。以下是系统的详细架构。

---

## 1. 区块 (Chunk) 系统

### 1.1 基本概念

| 术语 | 说明 |
|------|------|
| **Chunk** | 16×16×世界高度的方块数据单元 |
| **Section** | 16×16×16 的子区块，用于分级渲染 |
| **Region** | 32×32 Chunk 的集合，存储在 .mca 文件中 |
| **ViewDistance** | 渲染距离，以 Chunk 为单位 |

### 1.2 区块层级结构

```
World
├── Region (.mca file)
│   ├── Chunk (16×16×Y)
│   │   ├── Section (16×16×16) × N
│   │   │   └── Block Data + Light Data
│   │   └── Chunk Status
│   └── Chunk ...
└── Region ...
```

---

## 2. 渲染距离 (View Distance)

### 2.1 配置位置

WebMC 使用 `Options.java` 中的渲染距离设置：

```java
// upstream/projects/mcp/src/main/java/net/minecraft/client/Options.java:1319
this.renderDistance = new OptionInstance<>(
    "options.renderDistance",
    OptionInstance.noTooltip(),
    OptionInstance.Lifecycle.stable(),
    OptionInstance.ValidatedValues.inRange(2, 32),
    8,
    OptionInstance.Tickable<Boolean>(),
    Options.GenericOptions.RENDER_DISTANCE
);
```

### 2.2 有效渲染距离计算

```java
// Options.java:1805
public int getEffectiveRenderDistance() {
    return this.serverRenderDistance > 0
        ? Math.min(this.renderDistance.get(), this.serverRenderDistance)
        : this.renderDistance.get();
}
```

### 2.3 Web 端限制

| 渲染距离 | Chunk 数量 | 约等于 |
|---------|-----------|--------|
| 2 | ~25 | 近距离 |
| 4 | ~81 | 中等 |
| 8 | ~289 | 标准 |
| 12 | ~625 | 远距离 |
| 16 | ~1089 | 超远 |

---

## 3. 分区加载 (Sectioned Loading)

### 3.1 Section 渲染系统

LevelRenderer 使用 `SectionRenderDispatcher` 来管理区块渲染：

```java
// upstream/projects/mcp/src/main/java/net/minecraft/client/renderer/LevelRenderer.java:143
private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections =
    new ObjectArrayList<>(10000);
```

### 3.2 编译流程

```
compileSections()  ← LevelRenderer.java:483
    │
    ├── 更新可见 Section 列表
    │
    ├── SectionCompiler.compile()  ← 每个 Section
    │   │
    │   ├── 生成区块几何体 (mesh)
    │   ├── 优化 (greedy meshing)
    │   └── 生成 GPU 缓冲区
    │
    └── 添加到可见列表
```

### 3.3 Section 状态

```java
// SectionRenderDispatcher 中的状态
public enum SectionStatus {
    BLOCKS,      // 方块数据已加载
    STREAMING,   // 正在流式传输
    POSTPROCESSED, // 后处理完成
    FINALIZED,   // 渲染就绪
    EMPTY        // 无内容
}
```

---

## 4. 区块预加载 (Chunk Preloading)

### 4.1 AsyncChunkLoader 架构

WebMC 实现了异步区块加载器：

```java
// addons/teavm-runtime/src/main/java/top/steve3184/webmc/chunk/AsyncChunkLoader.java

public CompletableFuture<CompoundTag> loadChunkAsync(ChunkPos pos) {
    // 1. 优先从 VFS 加载
    // 2. 其次从预取缓存加载
    // 3. 最后从网络 HTTP 加载
}
```

### 4.2 预取策略

```java
// 预取相邻 8 个区域
public void prefetchAdjacentRegions(ChunkPos center) {
    for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
            // 跳过中心区域
            if (dx == 0 && dz == 0) continue;
            // 异步预取
            CompletableFuture.runAsync(() -> prefetchRegion(fx, fz), executor);
        }
    }
}
```

### 4.3 缓存策略

| 缓存类型 | 大小限制 | 说明 |
|---------|---------|------|
| Region Cache | 64 个区域 | 内存缓存 |
| VFS Cache | 无限制 | 虚拟文件系统 |
| HTTP Cache | 按浏览器策略 | 网络资源 |

---

## 5. 懒加载 (Lazy Loading)

### 5.1 懒加载触发

WebMC 使用 `Ticket` 系统来管理区块加载优先级：

```java
// Minecraft 原生机制
Ticket<ChunkPos> ticket = new Ticket<>(
    ChunkPos.class,
    TicketType.STATIC,
    level
);
chunkSource.addTicket(TicketType.STATIC, ticket);
```

### 5.2 加载队列

```
玩家移动 → 触发新 Chunk 需求
    │
    ├── 生成 Ticket
    │
    ├── 添加到 ChunkTaskDispatcher
    │
    ├── 按距离优先级排序
    │
    └── 异步加载
```

### 5.3 卸载策略

当区块超出加载范围时：
1. 降低 Ticket 优先级
2. 等待所有正在进行的操作完成
3. 释放区块数据
4. 保留渲染数据（如果正在渲染）

---

## 6. 可见范围动态加载

### 6.1 ViewArea 系统

```java
// upstream/projects/mcp/src/main/java/net/minecraft/client/renderer/ViewArea.java:20
public class ViewArea {
    private final int viewDistance;

    // 检查区块是否在可见范围内
    public boolean isChunkLoaded(int sectionX, int sectionZ) {
        return sectionX >= cameraSectionPos.x() - viewDistance
            && sectionX <= cameraSectionPos.x() + viewDistance
            && sectionZ >= cameraSectionPos.z() - viewDistance
            && sectionZ <= cameraSectionPos.z() + viewDistance;
    }
}
```

### 6.2 动态调整

```java
// LevelRenderer.java:372
if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
    // 重新计算可见区块
    this.viewArea = new ViewArea(
        this.sectionRenderDispatcher,
        this.level,
        this.minecraft.options.getEffectiveRenderDistance(),
        this
    );
}
```

### 6.3 帧间加载策略

| 帧类型 | 加载数量 | 说明 |
|-------|---------|------|
| 主帧 | 全部可见 | 完整渲染 |
| 追赶帧 | 最近 + 2 | 快速加载新区域 |
| 空闲帧 | 渐进加载 | 填充远处区域 |

---

## 7. 状态监控

### 7.1 State Beacon 系统

WebMC 通过 `__webmcState` 对象监控渲染状态：

```javascript
// Java 端设置
window.__webmcState = {
    visibleSections: 150,           // 可见的 Section 数量
    renderedSections: 120,         // 已渲染的 Section 数量
    requiredRenderedSections: 200,  // 所需数量
    levelPresent: true,            // 地形是否存在
    playerPresent: true,           // 玩家是否存在
    hasRenderedAllSections: true,   // 是否全部渲染完成
    webTerrainReady: true          // Web 特有标记
};
```

### 7.2 Bootstrap 状态机

```javascript
// bootstrap.js 状态转换
1. "Loading Minecraft..." (20%)
2. "Loading world..." (40%)
3. "Preparing terrain..." (55%)
4. "Building terrain..." (60-90%)
   - 显示进度: "Building terrain... (120/200)"
5. "Rendering terrain..." (95%)
6. "Main menu ready" / 隐藏启动画面
```

---

## 8. 性能优化

### 8.1 当前优化措施

| 优化 | 说明 |
|------|------|
| **Section 编译** | 并行编译多个 Section |
| **Greedy Meshing** | 减少顶点和面数 |
| **Frustum Culling** | 视锥体裁剪 |
| **Occlusion Culling** | 遮蔽剔除 |
| **LOD** | 细节层级 |

### 8.2 渲染器统计

```java
// LevelRenderer.java
public int countRenderedSections()  // 已渲染的 Section 数
public boolean hasRenderedAllSections()  // 是否全部完成
public int getNextSectionIndex()  // 下一个待渲染的 Section
```

---

## 9. 未来优化方向

### 9.1 潜在改进

1. **自适应渲染距离**
   - 根据 FPS 动态调整
   - 低性能时自动降低

2. **渐进式区块加载**
   - 低分辨率先行
   - 后台细化

3. **更好的预取算法**
   - 基于运动预测
   - 基于热点分析

### 9.2 Web 特有优化

1. **VFS 预加载优化**
   - 批量加载相邻区域
   - 后台流式传输

2. **内存管理**
   - LRU 缓存淘汰
   - 主动释放远处区块

---

## 10. 优化组件

### 10.1 AdaptiveRenderDistance (自适应渲染距离)

根据 FPS 动态调整渲染距离：

```java
// addons/teavm-runtime/src/main/java/top/steve3184/webmc/render/AdaptiveRenderDistance.java
public final class AdaptiveRenderDistance {
    private static final int MIN_RENDER_DISTANCE = 4;
    private static final int MAX_RENDER_DISTANCE = 12;
    private static final int FPS_LOW_THRESHOLD = 30;   // < 30 FPS → 降低
    private static final int FPS_HIGH_THRESHOLD = 55;  // > 55 FPS → 提高
    private static final int LOW_FPS_TRIGGER_SECONDS = 3;
    private static final int HIGH_FPS_TRIGGER_SECONDS = 5;
}
```

**工作原理：**
- 使用 `requestAnimationFrame` 监控 FPS
- FPS < 30 持续 3 秒 → 降低渲染距离 2 档
- FPS > 55 持续 5 秒 → 提高渲染距离 1 档
- 调整间隔冷却: 2 秒

### 10.2 ChunkPrefetcher (智能预取器)

基于移动预测的区块预取：

```java
// addons/teavm-runtime/src/main/java/top/steve3184/webmc/chunk/ChunkPrefetcher.java
public final class ChunkPrefetcher {
    // 70% 向前偏置, 30% 向后
    private static final double DIRECTIONAL_BIAS = 0.7;
    // 预测前方 2 个区块
    private static final int LOOK_AHEAD_DISTANCE = 2;
}
```

**功能：**
- 跟踪玩家速度和方向
- 根据移动方向优先预取区块
- 动态调整预取优先级

### 10.3 ChunkMemoryManager (内存管理器)

LRU 缓存 + 内存压力监控：

```java
// addons/teavm-runtime/src/main/java/top/steve3184/webmc/chunk/ChunkMemoryManager.java
public final class ChunkMemoryManager {
    private static final int DEFAULT_MAX_CHUNKS = 512;  // ~400MB
    private static final double MEMORY_PRESSURE_THRESHOLD = 0.8;
    private static final int SAFE_BUFFER = 2;  // 渲染距离外保留缓冲
}
```

**功能：**
- LRU 缓存，最多 512 个区块
- 监控 `performance.memory` API
- 内存压力 > 80% 时自动淘汰远处区块
- 每 10 秒记录统计

### 10.4 集成架构

```
AsyncChunkLoader
    ├── AdaptiveRenderDistance.getInstance()
    │       └── requestAnimationFrame 监控 FPS
    ├── ChunkPrefetcher
    │       └── updatePlayerPosition() 跟踪移动
    └── ChunkMemoryManager.getInstance()
            └── onChunkLoaded() / checkMemoryPressure()
```

### 10.5 UI 集成

Bootstrap.js 显示自适应信息：

```javascript
// 右上角显示 RD: 8
window.__webmcOnAdaptiveDistanceChange = function(dist, fps, enabled) {
    updateAdaptiveInfo({
        adaptiveRenderDistance: dist,
        adaptiveFps: fps,
        adaptiveEnabled: enabled
    });
};
```

---

## 11. 相关文件

| 文件 | 用途 |
|------|------|
| `AsyncChunkLoader.java` | 异步区块加载器 (已优化) |
| `LevelRenderer.java` | 地形渲染器 |
| `ViewArea.java` | 视野区域管理 |
| `SectionRenderDispatcher.java` | Section 调度器 |
| `AdaptiveRenderDistance.java` | **新增**: 自适应渲染距离 |
| `ChunkPrefetcher.java` | **新增**: 智能预取器 |
| `ChunkMemoryManager.java` | **新增**: 内存管理器 |
| `bootstrap.js` | 启动状态监控 + 自适应 UI |
