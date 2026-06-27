# WebMC 开发指南

Last updated: 2026-05-27

## 目标

保持 `patches/` 与 `addons/` 为唯一长期维护面，确保 `work/` 和运行产物可重复生成。

## phase-197 探针入口

- 统一入口脚本：`scripts/runtime-check-mcmain-phase197.cjs`
- `runtimeCheckMcMainPhase197*` Gradle 任务通过该脚本执行场景，不再依赖 `work/` 内临时脚本文件。

## 目录约定

- `patches/`: 对 Minecraft 源码的补丁，作为核心变更资产。
- `addons/`: Web 运行时桥接与 stub 扩展代码。
- `scripts/`: setup、补丁应用、补丁回提、构建辅助脚本。
- `work/`: 构建工作树（可重建，不作为长期手工编辑面）。
- `docs/`: 发布门槛、项目状态、开发约定。
- `docs/reports/`: 运行时验收证据输出目录（本地产物，默认不提交）。

## 标准开发流程

1. 初始化与重建工作树

```sh
./scripts/setup.sh
./scripts/apply-patches.sh
```

2. 在 `work/src/main/java` 验证改动后回提补丁

```sh
./scripts/rebuild-patches.sh
```

3. 构建与基础运行检查

```sh
cd work
./gradlew.bat clean buildWebMC --no-daemon
./gradlew.bat runtimeCheckWebRun --no-daemon
```

4. 清理本地运行痕迹（可选，提交前建议执行）

```sh
./scripts/clean-local.ps1
./scripts/clean-local.ps1 -WhatIf
npm run verify:local
npm run verify:local:allow-reports
```

5. phase-197 发布验收（见 `docs/PRODUCT_READINESS.md`）

```sh
work\gradlew.bat -p work runtimeCheckMcMainPhase197 --no-daemon --max-workers=1 --console=plain
work\gradlew.bat -p work runtimeCheckMcMainPhase197Soak --no-daemon --max-workers=1 --console=plain
work\gradlew.bat -p work runtimeCheckMcMainPhase197SaveReload --no-daemon --max-workers=1 --console=plain
work\gradlew.bat -p work runtimeCheckMcMainPhase197Rc --no-daemon --max-workers=1 --console=plain

# 同等 npm 快捷入口
npm run phase197:smoke
npm run phase197:soak
npm run phase197:save-reload
npm run phase197:rc
```

## 开发约束

- 不直接提交 `work/`、`dist/`、`out/` 这类构建产物。
- 运行时回归以自动化报告字段为准，不以人工观察替代。
- 新增实验脚本必须归入 `scripts/` 或 `work/` 的任务链路，临时测试文件不得放在仓库根目录。

## 人工工程化约束

- 结论必须绑定命令与结果，避免只写过程叙述。
- 未复现或未验证的内容，不写成通过结论。
- 文档只保留当前有效规则，历史排障日志放在外部归档，不堆在主文档。
- 每轮改动保持小步可回滚，先做最小必要变更再扩展。
