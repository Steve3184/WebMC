# WebMC 项目状态

Last updated: 2026-05-27

## 当前目标

保持 phase-197 发布门槛可重复通过，并将验收证据输出到 `docs/reports/`。

## 当前结论

- 最新完整验收（2026-05-27 20:30 CST）：`runtimeCheckMcMainPhase197Rc` PASS。
- RC 汇总报告：`docs/reports/mcmain-phase197-latest.json`
  - `scenario.mode=rc`
  - `scenario.steps`: `rc-smoke-1/2/3`、`rc-soak`、`rc-save-reload` 全部 `pass=true`
  - `phase.worldloadSuccessAtMs`、`phase.reloadWorldloadSuccessAtMs` 均非空
- 当前状态：产品级门槛已按既定流程一次性跑通。

## 残余风险

- reload 与长窗口期间仍可见运行时错误日志（例如 `'$hasNext'`、`'$hashCode1'`、`'$resolve'`）。
- 这些错误在本轮未阻断 gate，但属于下一轮稳定性压降候选项。

## 标准验收命令

- `work\gradlew.bat -p work compileJava --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work "-Pwebmc.experimentalMcMain=true" "-Pwebmc.mcMainPhase=197" "-Pwebmc.teavmOutOfProcess=true" generateJavaScript assembleWebRun --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work runtimeCheckMcMainPhase197 --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work runtimeCheckMcMainPhase197Soak --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work runtimeCheckMcMainPhase197SaveReload --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work runtimeCheckMcMainPhase197Rc --no-daemon --max-workers=1 --console=plain`
- `npm run phase197:smoke` / `npm run phase197:soak` / `npm run phase197:save-reload` / `npm run phase197:rc`

## 证据策略

- 历史本地测试 JSON 可清理，不作为长期仓库资产。
- 需要追溯时，重新执行验收命令生成同轮报告与截图。
- 报告最小检查项：
  - smoke: `phase.worldloadSuccessAtMs` 非空
  - soak: `phase.soakCompletedAtMs` 非空
  - save-reload: `phase.titleReturnedAtMs` 与 `phase.reloadWorldloadSuccessAtMs` 非空
  - rc: `scenario.steps` 完整且全部 PASS

## 仓库维护动作

- 本地清理脚本：`scripts/clean-local.ps1`
- 默认会清理本地运行痕迹与 `docs/reports` 历史 JSON（保留 `README.md`）
- 如需保留报告：`scripts/clean-local.ps1 -KeepReports`
- 本地门禁检查：`npm run verify:local`（失败返回非 0）
- 保留报告时门禁检查：`npm run verify:local:allow-reports`
- 验收探针稳定入口：`scripts/runtime-check-mcmain-phase197.cjs`（`runtimeCheckMcMainPhase197*` 已改为调用此路径）

## 失败分类契约

- `startup-stall-before-place`
- `startup-stall-after-place-before-render`
- `render-starvation`
- `world-ready-timeout`
- `soak-regression`
- `save-reload-regression`
- `orphan-process-contamination`
