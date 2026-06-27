// scripts/build-vfs.mjs
// Pack assets/data to WebFs binary format:
// "MCVF" + u32le(version=1) + u32le(count) + repeated(pathLen u16le, path utf8, dataLen u32le, data)
// Usage: node build-vfs.mjs <work-dir-or-build-dir> <output.mc-vfs.bin>

import { existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from 'fs';
import path from 'path';

const args = process.argv.slice(2);
if (args.length < 2) {
  console.error('Usage: node build-vfs.mjs <work-dir> <output.mc-vfs.bin>');
  process.exit(1);
}

const workDir = path.resolve(args[0]);
const outputFile = path.resolve(args[1]);
if (!existsSync(workDir)) {
  console.error('Work directory not found:', workDir);
  process.exit(1);
}

let mcResources = path.join(workDir, 'src', 'main', 'resources');
if (!existsSync(mcResources)) {
  mcResources = path.join(workDir, 'resources', 'main');
}

const projectRoot = path.basename(workDir) === 'build'
  ? path.resolve(workDir, '..', '..')
  : path.resolve(workDir, '..');
const baseVfs = process.env.WEBMC_BASE_VFS
  ? path.resolve(process.env.WEBMC_BASE_VFS)
  : path.join(projectRoot, 'dist', 'mc-vfs_lite.bin');

const roots = ['assets', 'data']
  .map((n) => ({ name: n, abs: path.join(mcResources, n) }))
  .filter((x) => existsSync(x.abs));

if (roots.length === 0 && !existsSync(baseVfs)) {
  console.error('No game resources found at', mcResources);
  process.exit(1);
}

function walkFiles(absBase, relBase, out) {
  const entries = readdirSync(absBase, { withFileTypes: true });
  for (const ent of entries) {
    const abs = path.join(absBase, ent.name);
    const rel = relBase ? `${relBase}/${ent.name}` : ent.name;
    if (ent.isDirectory()) {
      walkFiles(abs, rel, out);
    } else if (ent.isFile()) {
      out.push({ abs, rel: rel.replaceAll('\\', '/') });
    }
  }
}

const entries = new Map();

function normalizeRel(rel) {
  return rel.replaceAll('\\', '/').replace(/^\/+/, '');
}

function readU16LE(buf, off) {
  return buf.readUInt16LE(off);
}

function readU32LE(buf, off) {
  return buf.readUInt32LE(off);
}

function loadBaseVfs(abs) {
  const buf = readFileSync(abs);
  let pos = 0;
  if (buf.subarray(pos, pos + 4).toString('ascii') !== 'MCVF') {
    throw new Error(`Bad base VFS magic: ${abs}`);
  }
  pos += 4;
  const version = readU32LE(buf, pos); pos += 4;
  if (version !== 1) {
    throw new Error(`Unsupported base VFS version ${version}: ${abs}`);
  }
  const count = readU32LE(buf, pos); pos += 4;
  for (let i = 0; i < count; i++) {
    const pathLen = readU16LE(buf, pos); pos += 2;
    const rel = normalizeRel(buf.subarray(pos, pos + pathLen).toString('utf8')); pos += pathLen;
    const dataLen = readU32LE(buf, pos); pos += 4;
    const data = Buffer.from(buf.subarray(pos, pos + dataLen)); pos += dataLen;
    entries.set(rel, data);
  }
  console.log('[build-vfs] Base VFS:', abs);
  console.log('[build-vfs] Base file count:', count);
}

if (existsSync(baseVfs)) {
  loadBaseVfs(baseVfs);
} else {
  console.log('[build-vfs] Base VFS not found, packing overlay resources only:', baseVfs);
}

const files = [];
for (const r of roots) {
  walkFiles(r.abs, r.name, files);
}
for (const f of files) {
  entries.set(normalizeRel(f.rel), readFileSync(f.abs));
}

console.log('[build-vfs] Building MCVF from:', mcResources);
console.log('[build-vfs] Overlay file count:', files.length);
console.log('[build-vfs] Final file count:', entries.size);

const chunks = [];
let totalPayload = 0;

function u16le(v) {
  const b = Buffer.allocUnsafe(2);
  b.writeUInt16LE(v, 0);
  return b;
}
function u32le(v) {
  const b = Buffer.allocUnsafe(4);
  b.writeUInt32LE(v >>> 0, 0);
  return b;
}

chunks.push(Buffer.from('MCVF', 'ascii'));
chunks.push(u32le(1));
chunks.push(u32le(entries.size));

for (const [rel, data] of [...entries.entries()].sort((a, b) => a[0].localeCompare(b[0]))) {
  const relBytes = Buffer.from(rel, 'utf8');
  if (relBytes.length > 0xffff) {
    throw new Error(`Path too long for MCVF u16: ${rel}`);
  }
  chunks.push(u16le(relBytes.length));
  chunks.push(relBytes);
  chunks.push(u32le(data.length));
  chunks.push(data);
  totalPayload += data.length;
}

mkdirSync(path.dirname(outputFile), { recursive: true });
writeFileSync(outputFile, Buffer.concat(chunks));

const outSize = statSync(outputFile).size;
console.log('[build-vfs] Done:', outputFile);
console.log('[build-vfs] Payload bytes:', totalPayload);
console.log('[build-vfs] Output bytes:', outSize);
