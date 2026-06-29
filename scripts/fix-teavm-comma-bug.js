// fix-teavm-comma-bug.js - Fix TeaVM 0.13.1 code generation
// Replace "};\n    let " with "},\n    let " in game.js

const fs = require('fs');
const path = 'M:/Users/l/Desktop/webmc1/work/build/web-run/game.js';

console.log('Reading...');
const code = fs.readFileSync(path, 'utf8');
console.log(`Size: ${(code.length / 1024 / 1024).toFixed(1)} MB`);

// Pattern: }; followed by newline, then spaces+let
// This is a known TeaVM bug where };, appears as };,\n in the generated code
const fixed = code.replace(/}\s*;(\s*)$/gm, (m, ws) => {
    // Only fix if next meaningful line is let
    return '},' + ws;
});

// Wait, this is wrong - the regex is per-line. Let me check the actual pattern.
// The issue is "};\n    let " on adjacent lines
// So we need multi-line replacement

// Count occurrences
const lines = code.split('\n');
let count = 0;
for (let i = 0; i < lines.length - 1; i++) {
    if (lines[i].trimEnd().endsWith('};') && lines[i+1].match(/^\s+let\s/)) {
        count++;
    }
}
console.log(`Found ${count} occurrences of "};\\n    let " pattern`);

if (count > 0) {
    // Replace in one pass
    let src = code;
    let out = '';
    let pos = 0;
    let fixedCount = 0;

    while (pos < src.length) {
        // Find "};" followed by "\n" then spaces then "let "
        const idx = src.indexOf('};\n', pos);
        if (idx === -1) {
            out += src.slice(pos);
            break;
        }
        // Check if next non-empty line starts with let
        const nlIdx = idx + 2; // \n position
        if (src[nlIdx] === '\n') {
            let nextLineStart = nlIdx + 1;
            // Skip spaces on next line
            while (nextLineStart < src.length && src[nextLineStart] === ' ') {
                nextLineStart++;
            }
            if (src.substring(nextLineStart, nextLineStart + 4) === 'let ') {
                // This is a bug! Fix it
                out += src.slice(pos, idx + 1); // include ";"
                out += ','; // replace ";" with ","
                out += '\n'; // keep newline
                pos = nlIdx + 1;
                fixedCount++;
                continue;
            }
        }
        out += src.slice(pos, idx + 3);
        pos = idx + 3;
    }

    console.log(`Fixed ${fixedCount} occurrences`);

    // Only write if changed
    if (fixedCount > 0) {
        console.log('Writing...');
        fs.writeFileSync(path, out);
        console.log('Done!');
    } else {
        console.log('No changes needed');
    }
} else {
    console.log('No occurrences found');
}