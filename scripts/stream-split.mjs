// stream-split.mjs - Memory-efficient game.js splitter
// Uses streaming to avoid memory issues

import { createReadStream, createWriteStream, statSync } from 'fs';
import { createInterface } from 'readline';

const INPUT = process.argv[2] || 'work/build/web-run/game.js';
const OUTPUT_DIR = process.argv[3] || 'work/build/generated/teavm/js/chunks';
const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB per chunk

console.log('=== Stream Splitter ===');
console.log('Input:', INPUT);
console.log('Output:', OUTPUT_DIR);

const { size } = statSync(INPUT);
console.log('File size:', (size / 1024 / 1024).toFixed(2), 'MB');

// We'll use a simple approach: read in chunks and split at logical boundaries
// TeaVM structure: mostly data arrays at the end, code in the middle

import { mkdirSync } from 'fs';
mkdirSync(OUTPUT_DIR, { recursive: true });

// Read the whole file in streaming mode
let bytesRead = 0;
let chunkNum = 0;
let chunkContent = '';
const CHUNK_BYTES = 5 * 1024 * 1024;

const rl = createInterface({
  input: createReadStream(INPUT),
  crlfDelay: Infinity,
});

const ws = createWriteStream(`${OUTPUT_DIR}/chunk-000.js`);
ws.write('// Game.js chunk 000 - Header\n');

let lastProgress = 0;

rl.on('line', (line) => {
  chunkContent += line + '\n';
  bytesRead += Buffer.byteLength(line) + 1;

  const progress = Math.floor(bytesRead / size * 100);
  if (progress > lastProgress && progress % 10 === 0) {
    console.log(`Progress: ${progress}% (${chunkNum} chunks)`);
    lastProgress = progress;
  }

  // Every 5MB, write a chunk
  if (Buffer.byteLength(chunkContent) > CHUNK_BYTES) {
    ws.write(chunkContent);
    ws.end();
    chunkNum++;
    ws = createWriteStream(`${OUTPUT_DIR}/chunk-${String(chunkNum).padStart(3, '0')}.js`);
    ws.write(`// Game.js chunk ${String(chunkNum).padStart(3, '0')}\n`);
    chunkContent = '';
  }
});

rl.on('close', () => {
  // Write final chunk
  if (chunkContent) {
    ws.write(chunkContent);
  }
  ws.end();
  console.log(`Done! Created ${chunkNum + 1} chunks`);
});
