#!/usr/bin/env node
// 简单的文本分割脚本（不解析 AST，避免内存问题）

import { readFileSync, writeFileSync, mkdirSync } from 'fs';
import { join } from 'path';

const CHUNK_SIZE_MB = 50;
const CHUNK_SIZE_BYTES = CHUNK_SIZE_MB * 1024 * 1024;

async function simpleSplit(inputFile, outputDir) {
  console.log('===== Simple Code Splitting (Text-based) =====');
  console.log(`Input: ${inputFile}`);
  console.log(`Output: ${outputDir}\n`);

  console.log('[1/4] Reading file...');
  const source = readFileSync(inputFile, 'utf-8');
  const sizeMB = (source.length / 1024 / 1024).toFixed(2);
  console.log(`  Size: ${sizeMB} MB\n`);

  console.log('[2/4] Splitting by size...');
  mkdirSync(outputDir, { recursive: true });

  const lines = source.split('\n');
  const chunks = [];
  let currentChunk = [];
  let currentSize = 0;

  for (const line of lines) {
    const lineSize = line.length + 1; // +1 for newline

    if (currentSize + lineSize > CHUNK_SIZE_BYTES && currentChunk.length > 0) {
      chunks.push(currentChunk.join('\n'));
      currentChunk = [];
      currentSize = 0;
    }

    currentChunk.push(line);
    currentSize += lineSize;
  }

  if (currentChunk.length > 0) {
    chunks.push(currentChunk.join('\n'));
  }

  console.log(`  Created ${chunks.length} chunks\n`);

  console.log('[3/4] Writing chunks...');
  chunks.forEach((chunk, index) => {
    const filename = index === 0 ? 'main.js' : `chunk-${index}.js`;
    const filepath = join(outputDir, filename);
    writeFileSync(filepath, chunk, 'utf-8');
    const chunkSizeMB = (chunk.length / 1024 / 1024).toFixed(2);
    console.log(`  ${filename} (${chunkSizeMB} MB)`);
  });

  console.log('\n[4/4] Creating loader...');
  const loaderCode = `
// Simple chunk loader
(function() {
  'use strict';

  const CHUNK_COUNT = ${chunks.length};
  const loadedChunks = new Set([0]); // main.js is already loaded

  function loadChunk(index) {
    if (loadedChunks.has(index) || index >= CHUNK_COUNT) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'chunks/chunk-' + index + '.js';
      script.onload = () => {
        loadedChunks.add(index);
        console.log('[Loader] Loaded chunk-' + index + '.js');
        resolve();
      };
      script.onerror = () => reject(new Error('Failed to load chunk-' + index));
      document.head.appendChild(script);
    });
  }

  // Preload all chunks sequentially
  async function preloadAll() {
    for (let i = 1; i < CHUNK_COUNT; i++) {
      await loadChunk(i);
    }
    console.log('[Loader] All chunks loaded');
  }

  // Start preloading 5 seconds after page load
  setTimeout(preloadAll, 5000);

  window.__webmcLoadChunk = loadChunk;
  window.__webmcPreloadAll = preloadAll;
})();
`;

  writeFileSync(join(outputDir, 'loader.js'), loaderCode, 'utf-8');
  console.log('  loader.js\n');

  console.log('===== Done =====');
  console.log(`\nCreated ${chunks.length} chunks in ${outputDir}/`);
  console.log('Main chunk: main.js');
  console.log(`Other chunks: chunk-1.js to chunk-${chunks.length - 1}.js`);
  console.log('\nTo use:');
  console.log('1. Copy main.js as game.js');
  console.log('2. Copy chunks/ directory');
  console.log('3. Load loader.js after game.js');
}

// Run
const [inputFile, outputDir] = process.argv.slice(2);
if (!inputFile || !outputDir) {
  console.error('Usage: node simple-split.mjs <input.js> <output-dir>');
  process.exit(1);
}

simpleSplit(inputFile, outputDir);
