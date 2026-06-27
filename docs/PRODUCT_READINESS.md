# WebMC Product Readiness (Phase 197)

Last updated: 2026-05-27

## 发布门槛

1. `runtimeCheckMcMainPhase197` 连续 3 次 strict smoke PASS，且每次 `phase.worldloadSuccessAtMs` 非空、`evidence.stages.firstWorld.ok=true`。  
2. `runtimeCheckMcMainPhase197Soak` 至少 1 次 PASS，且 `phase.soakCompletedAtMs` 非空、`evidence.stages.soakCompleted.ok=true`。  
3. `runtimeCheckMcMainPhase197SaveReload` 至少 1 次 PASS，且以下字段均非空：`phase.firstWorldloadSuccessAtMs`、`phase.titleReturnedAtMs`、`phase.reloadWorldloadSuccessAtMs`；同时 `pauseMenu/titleReturn/reloadWorld` 三段截图 `ok=true`。  
4. `runtimeCheckMcMainPhase197Rc` PASS（`3 smoke + 1 soak + 1 save-reload` 串行通过）。  

## 最新验收快照

- 时间：2026-05-27 20:30 CST
- 命令：`work\gradlew.bat -p work runtimeCheckMcMainPhase197Rc --no-daemon --max-workers=1 --console=plain`
- 结论：PASS
- 报告：`docs/reports/mcmain-phase197-latest.json`
- 关键字段：
  - `scenario.mode=rc`
  - `scenario.steps` 全部 `pass=true`
  - `phase.worldloadSuccessAtMs`、`phase.soakCompletedAtMs`（rc-soak 子报告）、`phase.reloadWorldloadSuccessAtMs` 非空

## 默认参数

- `REQUIRE_WORLDLOAD_SUCCESS=true`
- `WAIT_MS=700000`
- `PLAYABLE_GRACE_MS=90000`
- `SOAK_MS=600000`
- `REPEAT_COUNT=3`
- `SMOKE_MAX_ATTEMPTS=2`
- `SMOKE_RETRY_WAIT_MS=900000`
- `SMOKE_RETRY_DELAY_MS=2000`
- `SOAK_MAX_ATTEMPTS=2`
- `SOAK_RETRY_WAIT_MS=900000`
- `SAVE_RELOAD_MAX_ATTEMPTS=2`
- `SAVE_RELOAD_RETRY_WAIT_MS=900000`

## 执行命令

- `work\gradlew.bat -p work compileJava --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work "-Pwebmc.experimentalMcMain=true" "-Pwebmc.mcMainPhase=197" "-Pwebmc.teavmOutOfProcess=true" generateJavaScript assembleWebRun --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work runtimeCheckMcMainPhase197 --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work runtimeCheckMcMainPhase197Soak --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work runtimeCheckMcMainPhase197SaveReload --no-daemon --max-workers=1 --console=plain`
- `work\gradlew.bat -p work runtimeCheckMcMainPhase197Rc --no-daemon --max-workers=1 --console=plain`
- `npm run phase197:smoke` / `npm run phase197:soak` / `npm run phase197:save-reload` / `npm run phase197:rc`

## 失败分类

- `startup-stall-before-place`
- `startup-stall-after-place-before-render`
- `render-starvation`
- `world-ready-timeout`
- `soak-regression`
- `save-reload-regression`
- `orphan-process-contamination`
