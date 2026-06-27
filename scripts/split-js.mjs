// scripts/split-js.mjs
// Splits TeaVM output JS into per-class files and generates manifest.
// TeaVM outputs a large JS file with all classes concatenated.
// This script parses it and splits by class declaration.

import { readFileSync, writeFileSync, mkdirSync } from 'fs';
import { join } from 'path';

const args = process.argv.slice(2);
if (args.length < 2) {
  console.error('Usage: node split-js.mjs <input.js> <output-dir>');
  process.exit(1);
}

const inputFile = args[0];
const outDir = args[1];

console.log('[split-js] Input:', inputFile);
console.log('[split-js] Output:', outDir);

const content = readFileSync(inputFile, 'utf-8');

// TeaVM uses $ sign for class separators in output.
// e.g.: webmc$noinit$Main$main$$$init$$$0 = function() { ...
const classPattern = /(\S+?)\s*=\s*function\s*\(\s*\)\s*{[\s\S]*?^(?=}\s*\n?\s*(?:webmc\$)|\}\s*;?\s*$|\}\s*\/\/\/CLASS_END)/gm;

// Better approach: split by class boundaries detected by TeaVM's naming convention
// Class names in TeaVM JS: package/name$Class = function() { ... };
const classRegex = /^(\S+?)\s*=\s*function\s*\(\s*\)\s*{/gm;

const chunks = [];
let lastIndex = 0;
let match;

while ((match = classRegex.exec(content)) !== null) {
  const className = match[1];
  chunks.push({
    name: className,
    start: lastIndex,
    end: match.index
  });
  lastIndex = match.index;
}

// Last chunk
chunks.push({ name: '_footer', start: lastIndex, end: content.length });

console.log('[split-js] Found', chunks.length - 1, 'classes');

// Filter out the footer and non-class chunks
const classChunks = chunks.filter(c => c.name !== '_footer' && !c.name.startsWith('$'));

// Create output directory
mkdirSync(outDir, { recursive: true });

const manifest = [];

for (const chunk of classChunks) {
  const chunkContent = content.slice(chunk.start, chunk.end).trim();
  if (!chunkContent) continue;

  // Convert class name to file path:
  //   webmc$net$minecraft$Class -> net/minecraft/Class.js
  const jsName = chunk.name.replace(/\$/g, '/');
  const fileName = (jsName.endsWith('.js') ? jsName : jsName + '.js');
  const fullPath = join(outDir, fileName);

  // Ensure parent directory exists
  const dir = join(outDir, jsName.slice(0, jsName.lastIndexOf('/')));
  if (dir) mkdirSync(dir, { recursive: true });

  writeFileSync(fullPath, chunkContent + '\n');
  manifest.push(fileName);
}

// Write footer (if any) as webmc.js
if (chunks.length > 0) {
  const footer = chunks[chunks.length - 1];
  if (footer.name === '_footer') {
    writeFileSync(join(outDir, 'webmc.js'), content.slice(footer.start, footer.end).trim());
    manifest.push('webmc.js');
  }
}

// Write manifest
const manifestPath = join(outDir, 'manifest.json');
writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));

console.log('[split-js] Wrote', manifest.length, 'files to', outDir);
console.log('[split-js] Manifest:', manifestPath);
