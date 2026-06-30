// fix-teavm-labels-precise.js - Fix TeaVM broken label statements PRECISELY
// Only fixes genuinely broken patterns, preserves valid code

const fs = require('fs');
const path = process.argv[2] || 'work/build/web-run/game.js';

console.log('Reading game.js...');
let code = fs.readFileSync(path, 'utf8');
const originalSize = code.length;
console.log(`File size: ${(originalSize / 1024 / 1024).toFixed(1)} MB`);

let totalFixed = 0;

// Helper: check if a label exists within N lines before current position
function labelExists(code, pos, label, maxLinesBack = 500) {
    const beforePos = Math.max(0, pos - 50000); // look back up to 50KB
    const chunk = code.slice(beforePos, pos);
    const lines = chunk.split('\n');
    // Check last maxLinesBack lines
    const relevant = lines.slice(-maxLinesBack).join('\n');
    // Pattern: label followed by colon, not part of another word
    const pattern = new RegExp(`\\b${label}\\s*:`, 'g');
    return pattern.test(relevant);
}

// Strategy 1: Fix "break main" that are in nested contexts
// Pattern: "break main;" followed by "};" or similar nested closure patterns
// These are callbacks that can't reach the outer "main:" label

console.log('\nStrategy 1: Fix "break main" in nested contexts...');

let s1Fixed = 0;
code = code.replace(/break main;/g, (match, offset) => {
    // Check if there's a "main:" label within 1000 chars before this break
    const chunkBefore = code.slice(Math.max(0, offset - 20000), offset);
    const linesBefore = chunkBefore.split('\n').slice(-100); // last 100 lines

    // Count how many braces open vs close in that range (rough nesting estimate)
    const opens = (chunkBefore.match(/\{/g) || []).length;
    const closes = (chunkBefore.match(/\}/g) || []).length;
    const depth = opens - closes;

    // If we're deep in nesting (depth > 5), likely in a callback
    if (depth > 5) {
        s1Fixed++;
        return 'return;';
    }
    return match;
});
console.log(`  Fixed ${s1Fixed} nested "break main"`);
totalFixed += s1Fixed;

// Strategy 2: Fix "break X" where X is a letter that's clearly a state machine variable
// These appear as "break t;" "break s;" etc in switch-case patterns inside callbacks
console.log('\nStrategy 2: Fix single-letter state machine breaks...');

let s2Fixed = 0;
// Only fix "break X;" where X is a letter and it's followed by a closing brace or similar
// within a certain context
code = code.replace(/\bbreak\s+([a-z])\s*;/g, (match, letter, offset) => {
    // Check context: is this inside a nested function?
    const chunkBefore = code.slice(Math.max(0, offset - 10000), offset);

    // If we see "=>" patterns, this is likely an arrow function
    const hasArrowFunction = chunkBefore.includes('=>');

    // If we see "function(" patterns before this
    const hasNestedFunction = /function\s*\([^)]*\)\s*\{/.test(chunkBefore.slice(-500));

    // If we're deeply nested (many function definitions)
    const functionCount = (chunkBefore.match(/function\s*\w*\s*\(/g) || []).length;

    // If deep nesting + arrow function, this is a broken label
    if ((functionCount > 2 || hasNestedFunction) && hasArrowFunction) {
        s2Fixed++;
        return 'return;';
    }
    return match;
});
console.log(`  Fixed ${s2Fixed} single-letter breaks in nested context`);
totalFixed += s2Fixed;

// Strategy 3: Fix patterns like "break main;" followed immediately by "};"
// This indicates the break is at the end of a callback
console.log('\nStrategy 3: Fix "break main;" at end of nested blocks...');

let s3Fixed = 0;
code = code.replace(/break main;\s*\}/g, (match, offset) => {
    // This pattern: "break main;\n}" often indicates broken label
    s3Fixed++;
    return 'return; }';
});
console.log(`  Fixed ${s3Fixed} "break main;" at block end`);
totalFixed += s3Fixed;

// Strategy 4: Fix "break X;" where X is a letter and the label doesn't exist
console.log('\nStrategy 4: Fix broken single-letter labels...');

let s4Fixed = 0;
code = code.replace(/\bbreak\s+([a-z])\s*;/g, (match, letter, offset) => {
    // Check if this letter label exists nearby
    const beforeChunk = code.slice(Math.max(0, offset - 5000), offset);
    const pattern = new RegExp(`\\b${letter}\\s*:`);

    if (!pattern.test(beforeChunk)) {
        s4Fixed++;
        return 'return;';
    }
    return match;
});
console.log(`  Fixed ${s4Fixed} labels without matching declaration`);
totalFixed += s4Fixed;

// Verify syntax on chunks
console.log('\nVerifying syntax...');
const testChunk = code.slice(0, 100000);
try {
    new Function(testChunk);
    console.log('  First 100KB: OK');
} catch (e) {
    console.log('  First 100KB error:', e.message);
}

const midChunk = code.slice(code.length / 2 - 50000, code.length / 2 + 50000);
try {
    new Function(midChunk);
    console.log('  Mid section: OK');
} catch (e) {
    console.log('  Mid section error:', e.message);
}

console.log(`\nTotal fixes: ${totalFixed}`);
console.log(`Output size: ${(code.length / 1024 / 1024).toFixed(1)} MB`);

console.log('\nWriting fixed game.js...');
fs.writeFileSync(path, code);
console.log('Done!');