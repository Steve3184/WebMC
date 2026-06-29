// fix-teavm-let-bug.js - Fix TeaVM 0.13.1 code generation
// Remove "let " after "},\n    " pattern

const fs = require('fs');
const path = 'M:/Users/l/Desktop/webmc1/work/build/web-run/game.js';

console.log('Reading...');
const code = fs.readFileSync(path, 'utf8');
console.log(`Size: ${(code.length / 1024 / 1024).toFixed(1)} MB`);

// Count occurrences
const countMatch = (code.match(/},\n    let /g) || []).length;
console.log(`Found ${countMatch} occurrences of "},\\n    let " pattern`);

if (countMatch > 0) {
    // Replace all ", let " after "}," pattern
    const fixed = code.replace(/},\n    let /g, '},\n    ');

    // Verify count dropped
    const newCount = (fixed.match(/},\n    let /g) || []).length;
    console.log(`After fix: ${newCount} remaining`);

    if (newCount === 0 && fixed.length > 0) {
        console.log('Writing...');
        fs.writeFileSync(path, fixed);
        console.log('Done!');
    } else {
        console.log('Warning: Some occurrences remain or file is empty');
    }
} else {
    console.log('No occurrences found');
}