// fix-teavm-nested-labels.js - Fix TeaVM broken label statements ONLY in nested functions
// TeaVM generates "break main" in nested callbacks where the outer "main:" label is unreachable
// This script fixes ONLY those specific cases

const fs = require('fs');
const path = process.argv[2] || 'work/build/web-run/game.js';

console.log('Reading game.js...');
let code = fs.readFileSync(path, 'utf8');
const originalSize = code.length;
console.log(`File size: ${(originalSize / 1024 / 1024).toFixed(1)} MB`);

const lines = code.split('\n');
let modified = 0;

// Track nesting depth: 0 = top level, 1+ = nested inside some function
// We track "depth" which increases when entering nested code
let depth = 0;
let inTopLevelSwitch = false;
let braceStack = [];

// Known valid label targets (from switch statements at any level)
const validLabels = new Set(['main']);

// Helper to check if we're in a nested context
function isNested() {
    return depth > 1; // More than 1 level deep = nested callback
}

// Process line by line
for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    // Track brace depth for nesting
    const opens = (trimmed.match(/\{/g) || []).length;
    const closes = (trimmed.match(/\}/g) || []).length;

    // Track entering nested functions (arrow functions, callbacks)
    // Pattern: "var name = () => {" or "var name = function() {" or "=> {"
    const isArrowFunction = /=\s*\(?\s*\w*\s*\)?\s*=>/.test(trimmed) && trimmed.includes('{');
    const isCallbackAssignment = /=\s*function\s*\(/.test(trimmed);
    const isMethodDefinition = /^\s*\w+\s*:\s*function\s*\(/.test(trimmed);

    // Enter nested function context
    if ((isArrowFunction || isCallbackAssignment || isMethodDefinition) && opens > 0) {
        depth += opens;
    }

    // Check for "break main" at nested depth
    if (depth > 1 && line.includes('break main;')) {
        lines[i] = line.replace('break main;', 'return;');
        modified++;
        continue;
    }

    // Also fix "break [letter]" at nested depth (less common but possible)
    if (depth > 1) {
        // Single letter labels after "break " - these are almost never valid in nested functions
        const newLine = line.replace(/\bbreak\s+([a-z])\s*;/g, (match, label) => {
            modified++;
            return 'return;';
        });
        if (newLine !== line) {
            lines[i] = newLine;
        }
    }

    // Exit nested context
    if (closes > 0 && depth > closes) {
        depth -= closes;
        if (depth < 0) depth = 0;
    }
}

console.log(`Fixed ${modified} broken "break" statements in nested functions`);

// Verify syntax
console.log('\nVerifying syntax...');
const firstMB = lines.slice(0, 50000).join('\n').slice(0, 1024 * 1024);
try {
    new Function(firstMB);
    console.log('First 1MB: OK');
} catch (e) {
    console.log('First 1MB error:', e.message);
}

code = lines.join('\n');
console.log(`\nWriting... (${(code.length / 1024 / 1024).toFixed(1)} MB)`);
fs.writeFileSync(path, code);
console.log('Done!');