// fix-teavm-labels.js - Fix TeaVM broken label statements
// TeaVM generates "break main" without the "main:" label in nested functions
// This script converts all such broken labels to "return;"

const fs = require('fs');
const path = process.argv[2] || 'work/build/web-run/game.js';

console.log('Reading game.js...');
let code = fs.readFileSync(path, 'utf8');
const originalSize = code.length;
console.log(`File size: ${(originalSize / 1024 / 1024).toFixed(1)} MB`);

const fixes = [];

// Strategy: Parse the file to find all functions and track their labels
// Then convert "break X" to "return" if X is not a valid label in that scope

const lines = code.split('\n');
let modified = 0;
let currentFunctionLabels = new Set(['main']); // main is always valid
let functionStack = []; // Stack of label sets for nested functions

for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    // Track entering/exiting functions
    if (trimmed.includes('=>') && trimmed.includes('function')) {
        // Starting a new function, push current labels to stack
        functionStack.push(new Set(currentFunctionLabels));
        currentFunctionLabels = new Set(['main']); // Reset for new function scope
    }

    // Check for function definitions (regular)
    if (/^function\s+\w+\s*\(/.test(trimmed)) {
        functionStack.push(new Set(currentFunctionLabels));
        currentFunctionLabels = new Set(['main']);
    }

    // Track closing braces to pop function scope
    const openBraces = (trimmed.match(/\{/g) || []).length;
    const closeBraces = (trimmed.match(/\}/g) || []).length;

    if (closeBraces > 0 && functionStack.length > 0) {
        // Pop scope after processing this line
        setTimeout(() => {}, 0); // async trick to process after
    }

    // Find all labels in this scope
    const labelMatches = trimmed.match(/^(\w+):/);
    if (labelMatches) {
        currentFunctionLabels.add(labelMatches[1]);
    }

    // Find all "break X" statements and check if X is valid
    const breakMatches = trimmed.match(/break\s+(\w+)\s*;/g);
    if (breakMatches) {
        for (const match of breakMatches) {
            const label = match.match(/break\s+(\w+)\s*;/)[1];
            if (!currentFunctionLabels.has(label)) {
                // Invalid label - this is the TeaVM bug!
                const newLine = line.replace(`break ${label};`, 'return;');
                if (newLine !== line) {
                    lines[i] = newLine;
                    modified++;
                }
            }
        }
    }

    // Track closing braces
    if (closeBraces > 0 && functionStack.length > 0) {
        // After closing brace, pop scope if we're at depth 0
        // This is approximate - the real fix would need proper parsing
    }
}

console.log(`Found ${modified} broken "break X" statements`);

// Alternative strategy: Just replace all "break X" where X is a single lowercase letter
// These are almost certainly broken (TeaVM's state machine labels)
// Valid labels are typically: main, while, switch statements
console.log('\nUsing heuristic fix: converting all "break [a-z];" to "return;"...');

let heuristicModified = 0;
for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    // Match "break X;" where X is a single lowercase letter
    const newLine = line.replace(/\bbreak\s+([a-z])\s*;/g, (match, label) => {
        heuristicModified++;
        return 'return;';
    });
    if (newLine !== line) {
        lines[i] = newLine;
    }
}

console.log(`Heuristic fix modified ${heuristicModified} statements`);

// Also fix "break main" that's outside the main function scope
// by looking for patterns like "break main;" not preceded by "main: while"
console.log('\nUsing context-aware fix for "break main"...');

let contextModified = 0;
for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (line.includes('break main;')) {
        // Check if there's a "main:" label nearby (within 1000 lines before)
        let hasLabel = false;
        for (let j = Math.max(0, i - 1000); j < i; j++) {
            if (lines[j].includes('main:')) {
                hasLabel = true;
                break;
            }
        }

        if (!hasLabel) {
            lines[i] = line.replace('break main;', 'return;');
            contextModified++;
        }
    }
}

console.log(`Context-aware fix modified ${contextModified} "break main" statements`);

// Now fix nested function "break main" issues more carefully
// The problem: TeaVM generates nested arrow functions with "main:" labels
// but the "break main" in nested callbacks can't reach the outer label
console.log('\nFixing nested function "break main" issues...');

let nestedModified = 0;
let inNestedFunction = 0;
let nestedFunctionDepth = 0;

for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    // Detect arrow function start with label
    if (/^\w+\s*=\s*[^=]+\s*=>/.test(trimmed) || /^\w+\s*:\s*\w+\s*=>/.test(trimmed)) {
        inNestedFunction++;
        nestedFunctionDepth = 0;
    }

    // Track brace depth
    const openBraces = (trimmed.match(/\{/g) || []).length;
    const closeBraces = (trimmed.match(/\}/g) || []).length;

    if (inNestedFunction > 0) {
        nestedFunctionDepth += openBraces - closeBraces;

        // Fix "break main" in nested functions
        if (line.includes('break main;')) {
            lines[i] = line.replace('break main;', 'return;');
            nestedModified++;
        }

        // End of nested function
        if (nestedFunctionDepth <= 0 && closeBraces > 0 && inNestedFunction > 0) {
            inNestedFunction--;
        }
    }
}

console.log(`Nested function fix modified ${nestedModified} statements`);

// Final pass: Replace any remaining obviously broken labels
console.log('\nFinal pass: fixing remaining broken labels...');

let finalModified = 0;
for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    // Match any single-letter labeled break that's not "break;" (which is valid)
    const newLine = line.replace(/\bbreak\s+([a-z])\s*;/g, (match, label) => {
        // Only fix if this label wasn't defined in the surrounding context
        // For safety, we'll fix all single-letter labels since they're almost never valid
        finalModified++;
        return 'return;';
    });
    if (newLine !== line) {
        lines[i] = newLine;
    }
}

console.log(`Final pass modified ${finalModified} statements`);

const totalModified = modified + heuristicModified + contextModified + nestedModified + finalModified;
console.log(`\nTotal fixes applied: ${totalModified}`);

code = lines.join('\n');

// Verify syntax on first 1MB
console.log('\nVerifying syntax on first 1MB...');
const firstMB = code.slice(0, 1024 * 1024);
try {
    new Function(firstMB);
    console.log('First 1MB syntax OK');
} catch (e) {
    console.log('First 1MB syntax error:', e.message);
    // Find the error location
    const errorLine = e.message.match(/at position (\d+)/);
    if (errorLine) {
        const pos = parseInt(errorLine[1]);
        const linesBeforeError = firstMB.slice(0, pos).split('\n').length;
        console.log(`Error around line ${linesBeforeError} in original file`);
        console.log('Context:');
        console.log(lines.slice(Math.max(0, linesBeforeError - 3), linesBeforeError + 3).join('\n'));
    }
}

console.log(`\nOutput size: ${(code.length / 1024 / 1024).toFixed(1)} MB`);
console.log('\nWriting fixed game.js...');
fs.writeFileSync(path, code);
console.log('Done!');