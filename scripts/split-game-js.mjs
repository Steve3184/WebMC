#!/usr/bin/env node

/**
 * TeaVM Code Splitting Script
 *
 * Splits a monolithic game.js (70+ MB) into multiple chunks for faster loading.
 * Analyzes AST to identify class boundaries and groups them by package/module.
 *
 * Usage: node split-game-js.mjs <input.js> <output-dir>
 * Example: node split-game-js.mjs work/build/generated/teavm/js/game.js work/build/generated/teavm/js/chunks
 */

import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';
import { Parser } from 'acorn';
import { generate } from 'escodegen';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Configuration
const CHUNK_SIZE_MB = 8;
const CHUNK_SIZE_BYTES = CHUNK_SIZE_MB * 1024 * 1024;

// Chunk splitting rules: categorize functions by name patterns
const CHUNK_RULES = [
  {
    name: 'main',
    description: 'Core runtime, java.lang, and entry point',
    patterns: [
      /^main$/,                                    // Entry function
      /^\$rt_/,                                    // TeaVM runtime helpers
      /^jl_/,                                      // java.lang.*
      /^jlr_/,                                     // java.lang.reflect.*
      /^ju_Arrays$/,                               // java.util.Arrays
      /^ju_Objects$/,                              // java.util.Objects
      /^jt_Charset/,                               // java.text.Charset
      /^top_steve3184_webmc_web_WebMain/,         // WebMain entry
      /^net_minecraft_client_Minecraft$/,         // Minecraft main class
      /^net_minecraft_client_main_GameConfig/,    // Game config
      /^com_mojang_logging/,                       // Logging system
    ],
    priority: 1  // Highest priority, must be in main chunk
  },
  {
    name: 'world',
    description: 'World generation, chunks, biomes, structures',
    patterns: [
      /^net_minecraft_world_level(?!_entity)/,     // World/Level (but not entity)
      /^net_minecraft_server_level/,               // Server-side level
      /^net_minecraft_world_level_chunk/,          // Chunk system
      /^net_minecraft_world_level_levelgen/,       // Level generation
      /^net_minecraft_world_level_biome/,          // Biomes
      /^net_minecraft_world_level_block/,          // Block states
      /^net_minecraft_world_level_storage/,        // World storage
      /^net_minecraft_core_Registry/,              // Registries
      /^net_minecraft_core_Holder/,                // Registry holders
      /^net_minecraft_data_worldgen/,              // World generation data
    ]
  },
  {
    name: 'render',
    description: 'Rendering pipeline, shaders, GPU',
    patterns: [
      /^com_mojang_blaze3d/,                       // Blaze3D rendering
      /^top_steve3184_webmc_gpu/,                  // WebGPU implementation
      /^net_minecraft_client_renderer/,            // Client renderer
      /^net_minecraft_client_model/,               // Models
      /^net_minecraft_client_particle/,            // Particle system
      /^net_minecraft_client_color/,               // Color providers
    ]
  },
  {
    name: 'entity',
    description: 'Entity system, AI, physics',
    patterns: [
      /^net_minecraft_world_entity/,               // Entity system
      /^net_minecraft_world_phys/,                 // Physics
      /^net_minecraft_world_damagesource/,         // Damage sources
      /^net_minecraft_world_item/,                 // Items
      /^net_minecraft_world_inventory/,            // Inventory
    ]
  },
  {
    name: 'ui',
    description: 'GUI, screens, resources, fonts',
    patterns: [
      /^net_minecraft_client_gui/,                 // GUI system
      /^net_minecraft_client_resources/,           // Resource loading
      /^net_minecraft_network_chat/,               // Chat/text components
      /^net_minecraft_client_KeyMapping/,          // Key bindings
      /^net_minecraft_client_Options/,             // Game options
    ]
  },
  {
    name: 'audio',
    description: 'Sound system, music',
    patterns: [
      /^com_mojang_blaze3d_audio/,                 // Blaze3D audio
      /^net_minecraft_client_sounds/,              // Client sounds
      /^net_minecraft_sounds/,                     // Sound events
    ]
  },
  {
    name: 'network',
    description: 'Networking, protocol, packets',
    patterns: [
      /^net_minecraft_network(?!_chat)/,           // Network (but not chat)
      /^net_minecraft_server_network/,             // Server networking
    ]
  }
];

/**
 * Parse command line arguments
 */
function parseArgs() {
  const args = process.argv.slice(2);

  if (args.length < 2) {
    console.error('Usage: node split-game-js.mjs <input.js> <output-dir>');
    console.error('');
    console.error('Arguments:');
    console.error('  input.js    Path to the monolithic game.js file');
    console.error('  output-dir  Directory where chunks will be written');
    process.exit(1);
  }

  const [inputFile, outputDir] = args;

  if (!existsSync(inputFile)) {
    console.error(`Error: Input file does not exist: ${inputFile}`);
    process.exit(1);
  }

  return { inputFile, outputDir };
}

/**
 * Main splitting logic
 */
async function splitGameJs(inputFile, outputDir) {
  console.log('===== TeaVM Code Splitting =====');
  console.log(`Input: ${inputFile}`);
  console.log(`Output: ${outputDir}`);
  console.log('');

  // Step 1: Read source file
  console.log('[1/6] Reading game.js...');
  const source = readFileSync(inputFile, 'utf-8');
  const sizeInMB = (source.length / 1024 / 1024).toFixed(2);
  console.log(`  File size: ${sizeInMB} MB`);
  console.log('');

  // Step 2: Parse AST
  console.log('[2/6] Parsing AST (this may take 10-30 seconds)...');
  const startParse = Date.now();
  let ast;

  try {
    ast = Parser.parse(source, {
      ecmaVersion: 2020,
      sourceType: 'script'
    });
  } catch (err) {
    console.error('Parse error:', err.message);
    console.error('This usually means the input file is not valid JavaScript.');
    process.exit(1);
  }

  const parseTime = ((Date.now() - startParse) / 1000).toFixed(1);
  console.log(`  Parsed in ${parseTime}s`);
  console.log('');

  // Step 3: Extract IIFE body
  console.log('[3/6] Analyzing structure...');

  // TeaVM wraps everything in (function() { ... })()
  const firstExpr = ast.body[0];
  if (firstExpr.type !== 'ExpressionStatement' ||
      firstExpr.expression.type !== 'CallExpression') {
    console.error('Error: Expected IIFE pattern (function(){...})()');
    console.error('The file structure does not match expected TeaVM output.');
    process.exit(1);
  }

  const iife = firstExpr.expression.callee;
  if (iife.type !== 'FunctionExpression') {
    console.error('Error: IIFE callee is not a function expression');
    process.exit(1);
  }

  const statements = iife.body.body;
  console.log(`  Found ${statements.length} top-level statements`);

  // Separate function declarations from other statements
  const functionDefs = [];
  const otherStatements = [];

  for (const stmt of statements) {
    if (stmt.type === 'FunctionDeclaration') {
      functionDefs.push(stmt);
    } else {
      otherStatements.push(stmt);
    }
  }

  console.log(`  Functions: ${functionDefs.length}`);
  console.log(`  Other statements (vars, etc.): ${otherStatements.length}`);
  console.log('');

  // Step 4: Categorize functions into chunks
  console.log('[4/6] Categorizing functions into chunks...');

  const chunks = new Map();
  CHUNK_RULES.forEach(rule => {
    chunks.set(rule.name, {
      functions: [],
      rule: rule
    });
  });
  chunks.set('misc', {
    functions: [],
    rule: { name: 'misc', description: 'Uncategorized functions' }
  });

  for (const fn of functionDefs) {
    const name = fn.id.name;
    let matched = false;

    for (const rule of CHUNK_RULES) {
      if (rule.patterns.some(p => p.test(name))) {
        chunks.get(rule.name).functions.push(fn);
        matched = true;
        break;
      }
    }

    if (!matched) {
      chunks.get('misc').functions.push(fn);
    }
  }

  // Print initial distribution
  console.log('  Initial distribution:');
  for (const [name, chunk] of chunks) {
    if (chunk.functions.length === 0) continue;

    const sampleSize = Math.min(3, chunk.functions.length);
    const samples = chunk.functions.slice(0, sampleSize).map(fn => fn.id.name);

    console.log(`    ${name}: ${chunk.functions.length} functions`);
    console.log(`      Examples: ${samples.join(', ')}`);
  }
  console.log('');

  // Step 5: Split large misc chunk
  const miscChunk = chunks.get('misc');
  if (miscChunk.functions.length > 0) {
    console.log('[5/6] Analyzing chunk sizes and splitting misc...');

    // Calculate sizes
    const chunkSizes = new Map();
    for (const [name, chunk] of chunks) {
      if (chunk.functions.length === 0) continue;

      let size = 0;
      for (const fn of chunk.functions) {
        const code = generate(fn, { format: { compact: true } });
        size += code.length;
      }
      chunkSizes.set(name, size);

      const sizeMB = (size / 1024 / 1024).toFixed(2);
      console.log(`    ${name}: ${sizeMB} MB`);
    }

    // Split misc if too large
    const miscSize = chunkSizes.get('misc') || 0;
    if (miscSize > CHUNK_SIZE_BYTES) {
      console.log('');
      console.log(`  Misc chunk is ${(miscSize / 1024 / 1024).toFixed(2)} MB, splitting...`);

      const miscFns = miscChunk.functions;
      chunks.delete('misc');

      let currentChunk = [];
      let currentSize = 0;
      let chunkIndex = 0;

      for (const fn of miscFns) {
        const fnCode = generate(fn, { format: { compact: true } });
        const fnSize = fnCode.length;

        if (currentSize + fnSize > CHUNK_SIZE_BYTES && currentChunk.length > 0) {
          chunks.set(`misc-${chunkIndex}`, {
            functions: currentChunk,
            rule: { name: `misc-${chunkIndex}`, description: `Miscellaneous functions (part ${chunkIndex + 1})` }
          });
          console.log(`    Created misc-${chunkIndex}: ${currentChunk.length} functions, ${(currentSize / 1024 / 1024).toFixed(2)} MB`);

          currentChunk = [];
          currentSize = 0;
          chunkIndex++;
        }

        currentChunk.push(fn);
        currentSize += fnSize;
      }

      if (currentChunk.length > 0) {
        chunks.set(`misc-${chunkIndex}`, {
          functions: currentChunk,
          rule: { name: `misc-${chunkIndex}`, description: `Miscellaneous functions (part ${chunkIndex + 1})` }
        });
        console.log(`    Created misc-${chunkIndex}: ${currentChunk.length} functions, ${(currentSize / 1024 / 1024).toFixed(2)} MB`);
      }
    }
  } else {
    console.log('[5/6] Analyzing chunk sizes...');
    for (const [name, chunk] of chunks) {
      if (chunk.functions.length === 0) continue;

      let size = 0;
      for (const fn of chunk.functions) {
        const code = generate(fn, { format: { compact: true } });
        size += code.length;
      }

      const sizeMB = (size / 1024 / 1024).toFixed(2);
      console.log(`    ${name}: ${sizeMB} MB`);
    }
  }
  console.log('');

  // Step 6: Generate chunk files
  console.log('[6/6] Writing chunk files...');
  mkdirSync(outputDir, { recursive: true });

  const chunkNames = [];

  for (const [name, chunk] of chunks) {
    if (chunk.functions.length === 0) continue;

    chunkNames.push(name);

    // Build chunk AST
    const chunkStatements = [];

    // Include runtime/globals only in main chunk
    if (name === 'main') {
      chunkStatements.push(...otherStatements);
    }

    // Add all functions in this chunk
    chunkStatements.push(...chunk.functions);

    // Wrap in IIFE
    const chunkAst = {
      type: 'Program',
      sourceType: 'script',
      body: [
        {
          type: 'ExpressionStatement',
          expression: {
            type: 'CallExpression',
            callee: {
              type: 'FunctionExpression',
              id: null,
              params: [],
              body: {
                type: 'BlockStatement',
                body: chunkStatements
              }
            },
            arguments: []
          }
        }
      ]
    };

    // Generate code
    console.log(`  Generating ${name}.js...`);
    const startGen = Date.now();
    const chunkCode = generate(chunkAst, {
      format: {
        indent: {
          style: '  '
        },
        compact: false
      }
    });
    const genTime = ((Date.now() - startGen) / 1000).toFixed(1);

    // Write to file
    const outputFile = join(outputDir, `${name}.js`);
    writeFileSync(outputFile, chunkCode, 'utf-8');

    const fileSizeMB = (chunkCode.length / 1024 / 1024).toFixed(2);
    console.log(`    Wrote ${fileSizeMB} MB in ${genTime}s`);
  }

  console.log('');

  // Generate loader
  generateLoader(outputDir, chunkNames);

  // Generate manifest
  generateManifest(outputDir, chunks);

  console.log('');
  console.log('===== Splitting Complete =====');
  console.log(`Total chunks: ${chunkNames.length}`);
  console.log(`Main chunk: main.js (loaded immediately)`);
  console.log(`Other chunks: loaded on-demand or preloaded`);
  console.log('');
  console.log('Next steps:');
  console.log('1. Test the chunks by serving them via HTTP server');
  console.log('2. Update bootstrap.js to load main.js and loader.js');
  console.log('3. Monitor loading performance in browser DevTools');
}

/**
 * Generate dynamic loader script
 */
function generateLoader(outputDir, chunkNames) {
  console.log('  Generating loader.js...');

  const loaderCode = `// Auto-generated chunk loader for WebMC
// DO NOT EDIT - regenerated on each build

(function() {
  'use strict';

  const CHUNKS = ${JSON.stringify(chunkNames, null, 2)};
  const CHUNK_BASE_PATH = 'chunks/';

  const loadedChunks = new Set(['main']);  // main is already loaded
  const pendingChunks = new Map();
  const loadStartTimes = new Map();

  /**
   * Load a chunk by name
   * @param {string} name - Chunk name (without .js extension)
   * @returns {Promise<void>}
   */
  function loadChunk(name) {
    if (loadedChunks.has(name)) {
      return Promise.resolve();
    }

    if (pendingChunks.has(name)) {
      return pendingChunks.get(name);
    }

    console.log('[ChunkLoader] Loading chunk:', name);
    loadStartTimes.set(name, Date.now());

    const promise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = CHUNK_BASE_PATH + name + '.js';

      script.onload = () => {
        const loadTime = Date.now() - loadStartTimes.get(name);
        console.log(\`[ChunkLoader] Loaded \${name} in \${loadTime}ms\`);

        loadedChunks.add(name);
        loadStartTimes.delete(name);
        pendingChunks.delete(name);
        resolve();
      };

      script.onerror = () => {
        console.error('[ChunkLoader] Failed to load chunk:', name);
        pendingChunks.delete(name);
        loadStartTimes.delete(name);
        reject(new Error('Failed to load chunk: ' + name));
      };

      document.head.appendChild(script);
    });

    pendingChunks.set(name, promise);
    return promise;
  }

  /**
   * Preload all chunks in the background
   * Called after initial startup to improve responsiveness
   */
  function preloadChunks() {
    const toLoad = CHUNKS.filter(name => !loadedChunks.has(name));

    if (toLoad.length === 0) {
      console.log('[ChunkLoader] All chunks already loaded');
      return;
    }

    console.log('[ChunkLoader] Preloading', toLoad.length, 'chunks:', toLoad.join(', '));

    toLoad.forEach(name => {
      loadChunk(name).catch(err => {
        console.error('[ChunkLoader] Preload failed for', name, ':', err.message);
      });
    });
  }

  /**
   * Get loading status
   */
  function getLoadingStatus() {
    return {
      total: CHUNKS.length,
      loaded: loadedChunks.size,
      pending: pendingChunks.size,
      remaining: CHUNKS.length - loadedChunks.size
    };
  }

  // Expose API
  window.__webmcLoadChunk = loadChunk;
  window.__webmcPreloadChunks = preloadChunks;
  window.__webmcChunkStatus = getLoadingStatus;

  console.log('[ChunkLoader] Initialized with', CHUNKS.length, 'chunks');

  // Start preloading after 5 seconds (avoid interfering with initial startup)
  setTimeout(() => {
    console.log('[ChunkLoader] Starting background preload...');
    preloadChunks();
  }, 5000);
})();
`;

  const loaderPath = join(outputDir, 'loader.js');
  writeFileSync(loaderPath, loaderCode, 'utf-8');
  console.log(`    Wrote loader.js`);
}

/**
 * Generate manifest file with metadata
 */
function generateManifest(outputDir, chunks) {
  console.log('  Generating manifest.json...');

  const manifest = {
    version: 1,
    generatedAt: new Date().toISOString(),
    chunks: []
  };

  for (const [name, chunk] of chunks) {
    if (chunk.functions.length === 0) continue;

    manifest.chunks.push({
      name: name,
      description: chunk.rule.description,
      functionCount: chunk.functions.length,
      priority: chunk.rule.priority || 99
    });
  }

  const manifestPath = join(outputDir, 'manifest.json');
  writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), 'utf-8');
  console.log(`    Wrote manifest.json`);
}

// Execute
const { inputFile, outputDir } = parseArgs();
splitGameJs(inputFile, outputDir);
