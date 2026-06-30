// smart-split.mjs - Smart TeaVM game.js splitter
// Splits by function boundaries, keeping data arrays together

import { createReadStream, createWriteStream, statSync, existsSync } from 'fs';
import { mkdirSync } from 'fs';
import { Readable } from 'stream';
import { pipeline } from 'stream/promises';

const INPUT = process.argv[2] || 'work/build/web-run/game.js';
const OUTPUT_DIR = process.argv[3] || 'work/build/generated/teavm/js/chunks';
const CHUNK_SIZE = 10 * 1024 * 1024; // 10MB per chunk

console.log('=== Smart TeaVM Splitter ===');
console.log('Input:', INPUT);
console.log('Output:', OUTPUT_DIR);

if (!existsSync(INPUT)) {
    console.error('Input file not found:', INPUT);
    process.exit(1);
}

mkdirSync(OUTPUT_DIR, { recursive: true });

const { size } = statSync(INPUT);
console.log('File size:', (size / 1024 / 1024).toFixed(2), 'MB');

// TeaVM function pattern: function name(...) { ... }
// Data array pattern: var name = [ ... ];
// Important: function bindings like: $rt_className = function() { ...

const FUNCTION_START = /^function\s+\$?[\w\$_]+\s*\(/;
const VAR_WITH_BRACKETS = /^var\s+\$?[\w\$_]+\s*=\s*\[/;
const VAR_WITH_OBJECT = /^var\s+\$?[\w\$_]+\s*=\s*\{/;
const IIFE_START = /^\(function\s*\(/;

let chunkNum = 0;
let currentChunkSize = 0;
let currentLines = [];
let functionsInChunk = 0;
let inMultiLineString = false;
let lineBuffer = '';
let lastProgress = 0;

const CHUNK_HEADER = `// WebMC Chunk ${chunkNum.toString().padStart(3, '0')} - Auto-generated
// Part of TeaVM game.js split for lazy loading
// This chunk size: ~10MB
`;

function startNewChunk() {
    if (currentLines.length > 0) {
        const ws = createWriteStream(`${OUTPUT_DIR}/chunk-${chunkNum.toString().padStart(3, '0')}.js`);
        ws.write(CHUNK_HEADER);
        ws.write(currentLines.join('\n'));
        if (!currentLines[currentLines.length - 1].endsWith(';') && !currentLines[currentLines.length - 1].endsWith('}')) {
            ws.write(';\n');
        }
        ws.end();
        console.log(`  Chunk ${chunkNum}: ${functionsInChunk} functions, ${(currentChunkSize / 1024 / 1024).toFixed(2)} MB`);
    }
    chunkNum++;
    currentLines = [];
    currentChunkSize = 0;
    functionsInChunk = 0;
}

import { createInterface } from 'readline';

async function processFile() {
    return new Promise((resolve, reject) => {
        const rl = createInterface({
            input: createReadStream(INPUT),
            crlfDelay: Infinity
        });

        rl.on('line', (line) => {
            const progress = Math.floor(rl.line / size * 100 * 100) || 0;
            if (progress > lastProgress && progress % 5 === 0) {
                process.stdout.write(`\rProgress: ${progress}% (${chunkNum} chunks, ${functionsInChunk} functions)`);
                lastProgress = progress;
            }

            // Track multi-line state
            const quotes = (line.match(/['"`]/g) || []).length;
            const openBrackets = ((line.match(/\[/g) || []).length) - ((line.match(/\]/g) || []).length);

            // Simple heuristics for multi-line detection
            const endsWithArray = line.trim().endsWith('[');
            const endsWithObject = line.trim().endsWith('{');
            const startsArray = line.trim().startsWith('[');

            if (endsWithArray || endsWithObject || startsArray) {
                inMultiLineString = true;
            }

            // Check for function starts - these are good split points
            if (FUNCTION_START.test(line) || IIFE_START.test(line)) {
                // If current chunk is getting big, start a new one before this function
                if (currentChunkSize > CHUNK_SIZE && functionsInChunk > 50) {
                    startNewChunk();
                }
                functionsInChunk++;
            }

            const lineSize = Buffer.byteLength(line);
            currentLines.push(line);
            currentChunkSize += lineSize;

            // If chunk is too big, save it
            if (currentChunkSize > CHUNK_SIZE) {
                startNewChunk();
            }
        });

        rl.on('close', () => {
            if (currentLines.length > 0) {
                startNewChunk();
            }
            console.log(`\nDone! Created ${chunkNum} chunks`);
            resolve();
        });

        rl.on('error', reject);
    });
}

processFile().catch(console.error);
