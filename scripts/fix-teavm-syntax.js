// fix-teavm-syntax.js - Fix TeaVM 0.13.1 code generation bugs
// Bug: try-catch blocks in IIFE are missing closing "}" before catch
// Pattern: "return 0;} catch(e)" -> "return 0; } catch(e)"

const fs = require('fs');
const path = process.argv[2] || 'work/build/web-run/game.js';

console.log('Reading game.js...');
let code = fs.readFileSync(path, 'utf8');
const originalSize = code.length;

console.log(`File size: ${(originalSize / 1024 / 1024).toFixed(1)} MB`);

// Fix 1: "return 0;} catch(e)" -> "return 0; } catch(e)"
// This happens in JSBody-generated try-catch blocks
const pattern1 = /return 0;\} catch\(e\)/g;
let count1 = 0;
let match;
while ((match = pattern1.exec(code)) !== null) {
    count1++;
}
console.log(`Found ${count1} occurrences of "return 0;} catch(e)" pattern`);

code = code.replace(/return 0;\} catch\(e\)/g, 'return 0; } catch(e)');

// Fix 2: Similar pattern with return -1
const pattern2 = /return -1;\} catch\(e\)/g;
let count2 = 0;
while ((match = pattern2.exec(code)) !== null) {
    count2++;
}
console.log(`Found ${count2} occurrences of "return -1;} catch(e)" pattern`);

code = code.replace(/return -1;\} catch\(e\)/g, 'return -1; } catch(e)');

// Fix 3: console.error followed by catch - missing closing brace
const pattern3 = /console\.error\([^)]*\);\s*\} catch\(e\)/g;
let count3 = 0;
while ((match = pattern3.exec(code)) !== null) {
    count3++;
}
console.log(`Found ${count3} occurrences of "console.error(...);} catch(e)" pattern`);

code = code.replace(/console\.error\([^)]*\);\s*\} catch\(e\)/g, (m) => m.replace('} catch', '} catch'));

// Fix 4: "let VarName = ..., $rt_java.MethodName = ..."
// This is invalid - let can't be in middle of comma-separated list
const pattern4 = /,(\s*)let\s+(\w+)\s*=/g;
let count4 = 0;
while ((match = pattern4.exec(code)) !== null) {
    count4++;
}
console.log(`Found ${count4} occurrences of ", let VarName = ..." pattern`);

code = code.replace(/,(\s*)let\s+(\w+)\s*=/g, (m, ws, varName) => {
    return `;\n    let ${varName} =`;
});

console.log(`Writing fixed game.js...`);
fs.writeFileSync(path, code);

console.log(`Done! Original: ${(originalSize / 1024 / 1024).toFixed(1)} MB, Fixed: ${(code.length / 1024 / 1024).toFixed(1)} MB`);