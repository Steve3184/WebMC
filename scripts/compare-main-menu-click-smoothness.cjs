const fs = require('fs');
const path = require('path');

const ROOT = process.cwd();

function usage() {
  return [
    'Usage:',
    '  node scripts/compare-main-menu-click-smoothness.cjs <baseline.json> <rerun.json> --out <comparison.json>',
    '',
    'Options:',
    '  --label <text>  Comparison label written to the output JSON.',
    '  --out <file>    Write comparison JSON to this path. If omitted, prints JSON to stdout.',
    '  --help          Show this help.'
  ].join('\n');
}

function parseArgs(argv) {
  const options = {
    label: 'main-menu click smoothness comparison',
    out: null,
    reports: []
  };

  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--help' || arg === '-h') {
      options.help = true;
      continue;
    }
    if (arg === '--out') {
      options.out = argv[++i];
      continue;
    }
    if (arg === '--label') {
      options.label = argv[++i];
      continue;
    }
    if (arg.startsWith('--')) {
      throw new Error(`Unknown option: ${arg}`);
    }
    options.reports.push(arg);
  }

  if (!options.help && options.reports.length !== 2) {
    throw new Error(`Expected exactly 2 reports, got ${options.reports.length}`);
  }
  if (options.out === undefined || options.label === undefined) {
    throw new Error('Missing value for option');
  }
  return options;
}

function resolveRepoPath(file) {
  return path.isAbsolute(file) ? file : path.join(ROOT, file);
}

function readJson(file) {
  const fullPath = resolveRepoPath(file);
  return JSON.parse(fs.readFileSync(fullPath, 'utf8'));
}

function state(report) {
  return (
    report.afterState ||
    report.finalState ||
    (report.readySnapshot && report.readySnapshot.webState) ||
    {}
  );
}

function countArray(value) {
  return Array.isArray(value) ? value.length : 0;
}

function finiteNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function rate(report, key) {
  if (report.metrics && report.metrics[key]) {
    const metricRate = finiteNumber(report.metrics[key].rate);
    if (metricRate !== null) return metricRate;
  }
  return finiteNumber(report[`${key}Rate`]);
}

function bool(value) {
  return value === true;
}

function summarize(entry) {
  const report = readJson(entry.path);
  const after = state(report);
  const menuActionSnapshot = report.menuActionSnapshot || {};
  const menuActionState = menuActionSnapshot.webState || {};

  return {
    label: entry.label,
    path: entry.path,
    ok: bool(report.ok),
    diagnostics: bool(report.diagnostics),
    frameProbeEnabled: bool(report.frameProbeEnabled),
    menuReadyAtMs: finiteNumber(report.menuReadyAtMs),
    menuActionReadyAtMs: finiteNumber(report.menuActionReadyAtMs),
    clickToWorldReadyMs: finiteNumber(report.clickToWorldReadyMs),
    actionableGate: {
      screen: menuActionState.screen || null,
      gameLoadFinished: bool(menuActionState.gameLoadFinished),
      levelPresent: bool(menuActionState.levelPresent),
      playerPresent: bool(menuActionState.playerPresent),
      autoStartRequested: bool(menuActionSnapshot.autoStartRequested),
      menuButtonDisabled: bool(menuActionSnapshot.menuButtonDisabled),
      menuButtonText: menuActionSnapshot.menuButtonText || ''
    },
    worldReady: {
      screen: after.screen || null,
      levelPresent: bool(after.levelPresent),
      playerPresent: bool(after.playerPresent),
      renderWorld: bool(after.renderWorld),
      worldRenderEligible: bool(after.worldRenderEligible),
      webTerrainReady: bool(after.webTerrainReady)
    },
    rates: {
      raf: rate(report, 'raf'),
      mcFrame: rate(report, 'mcFrame'),
      present: rate(report, 'present'),
      clientTick: rate(report, 'clientTick')
    },
    health: {
      cantKeepUpCount: finiteNumber(report.cantKeepUpCount),
      uboResizeLogCount: finiteNumber(report.uboResizeLogCount),
      gpuStallReadPixelsCount: finiteNumber(report.gpuStallReadPixelsCount),
      pageErrors: Array.isArray(report.pageErrors) ? report.pageErrors.length : 0,
      requestFailures: Array.isArray(report.requestFailures) ? report.requestFailures.length : 0,
      urlUnchangedAfterClick: bool(report.urlUnchangedAfterClick),
      noMainFrameNavigationAfterClick: bool(report.noMainFrameNavigationAfterClick)
    },
    diagnosticArrayCounts: {
      shaderPipelineStageEvents: countArray(after.shaderPipelineStageEvents),
      shaderTranslationCacheEvents: countArray(after.shaderTranslationCacheEvents),
      shaderTranslatorStageEvents: countArray(after.shaderTranslatorStageEvents),
      shaderReloadEvents: countArray(after.shaderReloadEvents)
    }
  };
}

function diffNumber(next, previous) {
  if (next === null || previous === null) return null;
  return Number((next - previous).toFixed(2));
}

function delta(base, rerun) {
  return {
    menuReadyAtMs: diffNumber(rerun.menuReadyAtMs, base.menuReadyAtMs),
    menuActionReadyAtMs: diffNumber(rerun.menuActionReadyAtMs, base.menuActionReadyAtMs),
    clickToWorldReadyMs: diffNumber(rerun.clickToWorldReadyMs, base.clickToWorldReadyMs),
    rates: {
      raf: diffNumber(rerun.rates.raf, base.rates.raf),
      mcFrame: diffNumber(rerun.rates.mcFrame, base.rates.mcFrame),
      present: diffNumber(rerun.rates.present, base.rates.present),
      clientTick: diffNumber(rerun.rates.clientTick, base.rates.clientTick)
    }
  };
}

function sameDiagnosticState(summary) {
  return (
    summary.diagnostics === false &&
    summary.frameProbeEnabled === false &&
    Object.values(summary.diagnosticArrayCounts).every((count) => count === 0)
  );
}

function main() {
  let options;
  try {
    options = parseArgs(process.argv.slice(2));
  } catch (err) {
    console.error(String((err && err.message) || err));
    console.error(usage());
    process.exit(2);
  }

  if (options.help) {
    console.log(usage());
    return;
  }

  const reports = options.reports.map((reportPath, index) => summarize({
    label: index === 0 ? 'baseline' : 'rerun',
    path: reportPath
  }));
  const result = {
    generatedAt: new Date().toISOString(),
    comparison: options.label,
    reports,
    delta: delta(reports[0], reports[1]),
    interpretation: [
      reports.every((report) => report.ok)
        ? 'Both reports have ok=true.'
        : 'At least one report has ok=false.',
      reports.every(sameDiagnosticState)
        ? 'Both reports prove normal mode: diagnostics disabled, frame probe disabled, and shader diagnostic arrays empty.'
        : 'At least one report does not prove normal diagnostic state.',
      'Rate deltas should be interpreted with host-load context; this comparison does not establish a low-load performance baseline by itself.'
    ]
  };

  const text = JSON.stringify(result, null, 2);
  if (options.out) {
    const outPath = resolveRepoPath(options.out);
    fs.mkdirSync(path.dirname(outPath), { recursive: true });
    fs.writeFileSync(outPath, text);
    console.log(`clickSmooth.compare=${outPath}`);
    console.log(`clickSmooth.delta=${JSON.stringify(result.delta)}`);
  } else {
    console.log(text);
  }
}

main();
