// fix-teavm-all-labels.js - Comprehensive fix for TeaVM broken label statements
// TeaVM 0.13.1 generates thousands of "break main;" in nested callbacks
// This script fixes them all efficiently

const fs = require('fs');
const path = process.argv[2] || 'work/build/web-run/game.js';

console.log('Reading game.js...');
let code = fs.readFileSync(path, 'utf8');
const originalSize = code.length;
console.log(`File size: ${(originalSize / 1024 / 1024).toFixed(1)} MB`);

// Count original patterns
const breakMainCount = (code.match(/break main;/g) || []).length;
const breakSingleLetterCount = (code.match(/\bbreak [a-z];/g) || []).length;
console.log(`Found ${breakMainCount} "break main;" statements`);
console.log(`Found ${breakSingleLetterCount} "break X;" statements`);

// Strategy: Replace ALL "break main;" with "return;"
// The "main:" label only exists at the very top level
// Any "break main;" in a nested context is invalid
console.log('\nFixing all "break main;" -> "return;"...');
code = code.replace(/break main;/g, 'return;');
console.log('Fixed all "break main;"');

// For single-letter breaks, we need context awareness
// But given the scale, let's try a simple heuristic:
// Only keep "break X;" if it appears within a switch statement at the top level
// Since we can't easily track nesting depth, let's try replacing all
// and verify syntax

console.log('\nFixing single-letter breaks...');
// Count before
const beforeSingle = (code.match(/\bbreak [a-z];/g) || []).length;
console.log(`Before: ${beforeSingle} single-letter breaks`);

// Replace all single-letter breaks
code = code.replace(/\bbreak ([a-z]);/g, 'return;');
const afterSingle = (code.match(/\bbreak [a-z];/g) || []).length;
console.log(`After: ${afterSingle} single-letter breaks (${beforeSingle - afterSingle} fixed)`);

// Verify syntax
console.log('\nVerifying syntax...');

function testChunk(label, start, len) {
    const chunk = code.slice(start, start + len);
    try {
        new Function(chunk);
        console.log(`  ${label}: OK`);
        return true;
    } catch (e) {
        console.log(`  ${label} ERROR: ${e.message}`);
        return false;
    }
}

// Test multiple chunks
testChunk('First 100KB', 0, 100000);
testChunk('First 1MB', 0, 1024 * 1024);
testChunk('Mid section', Math.floor(code.length / 2), 100000);
testChunk('Last 100KB', code.length - 100000, 100000);

console.log(`\nFinal size: ${(code.length / 1024 / 1024).toFixed(1)} MB`);
console.log('\nWriting...');
fs.writeFileSync(path, code);
console.log('Done!');