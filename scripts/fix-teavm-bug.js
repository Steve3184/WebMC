// fix-teavm-bug.js - Fix TeaVM 0.13.1 comma-list code generation bug
// Pattern: "$rt_java.Long_XXX = ... };" followed by "let VarName = ..."
// Fix: Change "};" to "}," so the comma-list continues properly

const fs = require('fs');
const path = process.argv[2] || 'work/build/web-run/game.js';

console.log('Reading game.js...');
let code = fs.readFileSync(path, 'utf8');
const originalSize = code.length;

// More precise pattern: $rt_java.Long_XXX = <arrow-function-or-expression> };
// followed by whitespace+let
// The arrow function may have braces, so we need to match carefully
// Strategy: find ";$" preceded by "$rt_java." and followed by whitespace+let
const fixedCode = code.replace(
    /(\$rt_java\.\w+\s*=\s*(?:[^{}]*|\((?:[^)]*)\)\s*=>\s*(?:[^{}]*|(?:\{[^}]*\}))))\s*}\s*;(\s*)/g,
    (m, prefix, suffix) => {
        return prefix + ' },' + suffix;
    }
);

// Also handle cases with complex bodies
const fixedCode2 = fixedCode.replace(
    /(\$rt_java\.\w+\s*=\s*(?:\((?:[^)]*)\)\s*=>\s*)?(?:\{[^}]*(?:\{[^}]*\}[^}]*)*\}))?\s*}\s*;(\s*)/g,
    (m, prefix, suffix) => {
        // Only fix if preceded by $rt_java.
        if (prefix.includes('$rt_java.')) {
            return prefix + ' },' + suffix;
        }
        return m;
    }
);

const diff = code.length - fixedCode2.length;
console.log(`Made ${diff > 0 ? diff : 0} bytes of changes`);

console.log('Writing fixed game.js...');
fs.writeFileSync(path, fixedCode2);

console.log('Done!');