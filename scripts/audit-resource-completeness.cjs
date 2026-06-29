const fs = require('fs');
const path = require('path');

const repoRoot = path.resolve(__dirname, '..');
const srcRoot = path.join(repoRoot, 'work', 'src', 'main', 'java');
const overlayRoot = path.join(repoRoot, 'work', 'build', 'resources', 'main');
const baseVfs = path.join(repoRoot, 'dist', 'mc-vfs.bin');

function walk(dir, out = []) {
  if (!fs.existsSync(dir)) {
    return out;
  }
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const abs = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(abs, out);
    } else if (entry.isFile()) {
      out.push(abs);
    }
  }
  return out;
}

function normalizeRel(relPath) {
  return relPath.replaceAll('\\', '/').replace(/^\/+/, '');
}

function readBaseVfs(filePath) {
  const entries = new Set();
  if (!fs.existsSync(filePath)) {
    return entries;
  }

  const buf = fs.readFileSync(filePath);
  let pos = 0;
  const magic = buf.subarray(pos, pos + 4).toString('ascii');
  pos += 4;
  if (magic !== 'MCVF') {
    throw new Error(`Bad VFS magic: ${filePath}`);
  }
  const version = buf.readUInt32LE(pos);
  pos += 4;
  if (version !== 1) {
    throw new Error(`Unsupported VFS version ${version}: ${filePath}`);
  }
  const count = buf.readUInt32LE(pos);
  pos += 4;
  for (let i = 0; i < count; i++) {
    const pathLen = buf.readUInt16LE(pos);
    pos += 2;
    const rel = normalizeRel(buf.subarray(pos, pos + pathLen).toString('utf8'));
    pos += pathLen;
    const dataLen = buf.readUInt32LE(pos);
    pos += 4 + dataLen;
    entries.add(rel);
  }
  return entries;
}

function collectOverlayEntries(root) {
  const entries = new Set();
  for (const abs of walk(root)) {
    const rel = normalizeRel(path.relative(root, abs));
    entries.add(rel);
  }
  return entries;
}

function collectExpectedAssetPaths() {
  const resourcePaths = new Map();
  const defaultPattern = /ResourceLocation\.(withDefaultNamespace|parse)\("([^"]+)"\)/g;
  const namespacedPattern = /ResourceLocation\.fromNamespaceAndPath\("([^"]+)",\s*"([^"]+)"\)/g;

  for (const file of walk(srcRoot)) {
    if (!file.endsWith('.java')) continue;
    const text = fs.readFileSync(file, 'utf8');
    collectLiteralMatches(resourcePaths, file, text, defaultPattern, (match) => ({
      namespace: 'minecraft',
      relPath: match[2],
    }));
    collectLiteralMatches(resourcePaths, file, text, namespacedPattern, (match) => ({
      namespace: match[1],
      relPath: match[2],
    }));
  }

  return resourcePaths;
}

function collectLiteralMatches(resourcePaths, file, text, pattern, extractor) {
  let match;
  while ((match = pattern.exec(text)) !== null) {
    const { namespace, relPath } = extractor(match);
    if (!(relPath.startsWith('textures/') || relPath.startsWith('models/') || relPath.startsWith('particles/') || relPath.endsWith('.png') || relPath.endsWith('.json'))) {
      continue;
    }

    if (shouldIgnoreStaticAuditPath(relPath)) {
      continue;
    }

    const assetPath = normalizeRel(`assets/${namespace}/${relPath}`);
    if (!resourcePaths.has(assetPath)) {
      resourcePaths.set(assetPath, []);
    }
    resourcePaths.get(assetPath).push(path.relative(repoRoot, file));
  }
}

function shouldIgnoreStaticAuditPath(relPath) {
  if (relPath.startsWith('textures/atlas/')) {
    return true;
  }
  if (relPath.includes('%s')) {
    return true;
  }
  if (relPath.endsWith('/') || relPath.endsWith('_') || relPath.endsWith('panorama')) {
    return true;
  }
  return false;
}

function main() {
  const baseEntries = readBaseVfs(baseVfs);
  const overlayEntries = collectOverlayEntries(overlayRoot);
  const available = new Set([...baseEntries, ...overlayEntries]);
  const expected = collectExpectedAssetPaths();

  const missing = [];
  for (const [assetPath, refs] of expected.entries()) {
    if (!available.has(assetPath)) {
      missing.push({ assetPath, refs });
    }
  }

  missing.sort((a, b) => a.assetPath.localeCompare(b.assetPath));

  const report = {
    generatedAt: new Date().toISOString(),
    baseVfsExists: fs.existsSync(baseVfs),
    overlayRootExists: fs.existsSync(overlayRoot),
    expectedCount: expected.size,
    availableCount: available.size,
    missingCount: missing.length,
    missing,
  };

  const outPath = path.join(repoRoot, 'docs', 'reports', 'resource-audit.json');
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, JSON.stringify(report, null, 2));

  console.log(`[resource-audit] expected=${report.expectedCount} available=${report.availableCount} missing=${report.missingCount}`);
  console.log(`[resource-audit] report=${path.relative(repoRoot, outPath)}`);

  for (const item of missing.slice(0, 30)) {
    console.log(`[resource-audit] missing ${item.assetPath}`);
  }
}

main();
