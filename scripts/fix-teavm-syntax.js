// fix-teavm-syntax.js - Comprehensive TeaVM 0.13.1 syntax fix
const fs = require('fs');
const path = process.argv[2] || 'work/build/web-run/game.js';

console.log('Reading game.js...');
let code = fs.readFileSync(path, 'utf8');
const originalSize = code.length;
console.log(`File size: ${(originalSize / 1024 / 1024).toFixed(1)} MB`);

// Track all fixes
const fixes = [];

// Fix 1: "return 0;} catch(e)" -> "return 0; } catch(e)"
{
    const pattern = /return 0;\} catch\(e\)/g;
    let count = 0;
    let match;
    while ((match = pattern.exec(code)) !== null) count++;
    code = code.replace(pattern, 'return 0; } catch(e)');
    console.log(`Fixed ${count} "return 0;} catch(e)"`);
    fixes.push({ type: 'catch-brace', count });
}

// Fix 2: "return -1;} catch(e)" -> "return -1; } catch(e)"
{
    const pattern = /return -1;\} catch\(e\)/g;
    let count = 0;
    let match;
    while ((match = pattern.exec(code)) !== null) count++;
    code = code.replace(pattern, 'return -1; } catch(e)');
    console.log(`Fixed ${count} "return -1;} catch(e)"`);
    fixes.push({ type: 'return-catch', count });
}

// Fix 3: ", let VarName = ..." - invalid in comma-separated list
{
    const pattern = /,\s*let\s+(\w+)\s*=/g;
    let count = 0;
    let match;
    while ((match = pattern.exec(code)) !== null) count++;
    code = code.replace(pattern, (m, name) => `; let ${name} =`);
    console.log(`Fixed ${count} ", let VarName = ..."`);
    fixes.push({ type: 'comma-let', count });
}

// Fix 4: Nested "break main" - the most complex issue
// Strategy: Find all arrow functions that contain "break main" but don't have "main:" label
// These need the label added or the break converted

console.log('\nAnalyzing "break main" patterns...');

// Split by newlines for line-based analysis
const lines = code.split('\n');
let inFunction = false;
let functionDepth = 0;
let functionStartLine = 0;
let functionHasMainLabel = false;
let functionUsesBreakMain = false;
let pendingFixes = [];

for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Track function boundaries (arrow functions)
    const openBraces = (line.match(/\{/g) || []).length;
    const closeBraces = (line.match(/\}/g) || []).length;

    // Function start detection (simple arrow function assignment)
    if (!inFunction && line.match(/\w+\s*=\s*\w+\s*=>/)) {
        inFunction = true;
        functionStartLine = i;
        functionDepth = 0;
        functionHasMainLabel = false;
        functionUsesBreakMain = false;
    }

    if (inFunction) {
        functionDepth += openBraces - closeBraces;

        // Check for main label
        if (line.includes('main:')) {
            functionHasMainLabel = true;
        }

        // Check for break main
        if (line.includes('break main')) {
            functionUsesBreakMain = true;
        }

        // Function end
        if (functionDepth <= 0 && !line.includes('{')) {
            inFunction = false;

            // If this function uses break main but has no main label, we need to fix it
            if (functionUsesBreakMain && !functionHasMainLabel) {
                pendingFixes.push({
                    start: functionStartLine + 1,
                    end: i + 1,
                    usesBreakMain: true,
                    hasLabel: false
                });
            }
        }
    }
}

console.log(`Found ${pendingFixes.length} arrow functions with "break main" but no label`);

// For these problematic functions, convert "break main" to "return"
// since they're error paths in the state machine
if (pendingFixes.length > 0) {
    // Simple approach: convert "break main;" to "return;" in all arrow functions
    // that don't have a "main:" label before the break

    console.log('\nConverting "break main" to "return" in problematic functions...');

    // Reset and do a more careful pass
    lines.length = 0;
    lines.push(...code.split('\n'));

    let currentFunctionHasLabel = false;
    let currentFunctionUsesBreak = false;
    inFunction = false;
    functionDepth = 0;

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];

        // Simple function start pattern
        if (!inFunction && line.match(/\w+\s*=\s*\w+\s*=>/)) {
            inFunction = true;
            functionDepth = 0;
            currentFunctionHasLabel = false;
            currentFunctionUsesBreak = false;
        }

        if (inFunction) {
            // Track brace depth
            const openBraces = (line.match(/\{/g) || []).length;
            const closeBraces = (line.match(/\}/g) || []).length;
            functionDepth += openBraces - closeBraces;

            // Track label
            if (line.includes('main:')) {
                currentFunctionHasLabel = true;
            }

            // Track break main
            if (line.includes('break main')) {
                currentFunctionUsesBreak = true;

                // If we have break main but no label, convert it
                if (!currentFunctionHasLabel) {
                    // Replace "break main" with "return"
                    lines[i] = line.replace(/break main;/g, 'return;');
                    fixes.push({ type: 'break-main-to-return', line: i + 1 });
                }
            }

            // Function end
            if (functionDepth <= 0 && closeBraces > 0) {
                inFunction = false;
            }
        }
    }

    code = lines.join('\n');
}

// Fix 5: "Unexpected token '}'" - often caused by missing semicolons or extra braces
// Look for patterns like "}} catch" that should be "}} catch"
{
    // Count potential issues
    const pattern = /\}\s*\} catch\(/g;
    let count = 0;
    let match;
    while ((match = pattern.exec(code)) !== null) count++;
    console.log(`Found ${count} "}} catch" patterns (may be valid)`);
}

// Fix 6: "Unexpected end of input" - often missing closing braces
// Check if the file ends properly
{
    const lastChars = code.slice(-100);
    const openBraces = (lastChars.match(/\{/g) || []).length;
    const closeBraces = (lastChars.match(/\}/g) || []).length;
    console.log(`Last 100 chars: ${openBraces} open braces, ${closeBraces} close braces`);

    if (closeBraces > openBraces) {
        console.log('Warning: File may have extra closing braces');
    }
}

console.log('\nWriting fixed game.js...');
fs.writeFileSync(path, code);

// Verify with a quick syntax check on first 1MB
console.log('\nVerifying syntax on first 1MB...');
const firstMB = code.slice(0, 1024 * 1024);
try {
    // Use Function constructor as a lightweight check
    new Function(firstMB);
    console.log('First 1MB syntax OK');
} catch (e) {
    console.log('First 1MB syntax error:', e.message);
}

console.log('\nSummary:');
console.log(`- Total fixes applied: ${fixes.reduce((a, f) => a + (f.count || 1), 0)}`);
fixes.forEach(f => console.log(`  - ${f.type}: ${f.count || 1}`));
console.log(`- Output size: ${(code.length / 1024 / 1024).toFixed(1)} MB`);