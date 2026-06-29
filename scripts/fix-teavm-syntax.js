// fix-teavm-syntax.js - Fix TeaVM 0.13.1 code generation bugs
// Pattern: "let X = ..., let Y = ..." -> split into separate let statements

const fs = require('fs');
const path = process.argv[2] || 'work/build/web-run/game.js';

console.log('Reading game.js...');
let code = fs.readFileSync(path, 'utf8');
const originalSize = code.length;

console.log(`File size: ${(originalSize / 1024 / 1024).toFixed(1)} MB`);

// Pattern 1: "let VarName = ..., $rt_java.MethodName = ..."
// This is invalid - let can't be in middle of comma-separated list
// Fix: insert semicolon before "let" when it follows a comma
const pattern1 = /,(\s*)let\s+\w+\s*=/g;
let match;
let count1 = 0;
while ((match = pattern1.exec(code)) !== null) {
    count1++;
}
console.log(`Found ${count1} occurrences of ", let VarName = ..." pattern`);

code = code.replace(/,(\s*)let\s+(\w+)\s*=/g, (m, ws, varName) => {
    return `;\n    let ${varName} =`;
});

// Pattern 2: Also fix if it starts with "let" after a property assignment in a comma list
// "$rt_java.prop = ..., let VarName = ..."
// Already covered by Pattern 1

console.log(`Writing fixed game.js...`);
fs.writeFileSync(path, code);

console.log(`Done! Original: ${(originalSize / 1024 / 1024).toFixed(1)} MB, Fixed: ${(code.length / 1024 / 1024).toFixed(1)} MB`);