#!/usr/bin/env node
/**
 * Fix game.js for Node v24+ compatibility.
 * Issue: Node v24 has a parser bug where `BigInt(0),` followed by a newline and
 * `$rt_java.Long_udiv = ...` causes "Identifier '$rt_java' has already been declared".
 * Fix: Insert a semicolon after Long_div's closing brace before the comma.
 */
const fs = require('fs');
const path = process.argv[2] || 'work/build/web-run/game.js';
let code = fs.readFileSync(path, 'utf8');
let original = code;
let count = 0;

// Pattern: },\n    $rt_java.  ← this is what causes the problem
// When a BigInt(0), precedes $rt_java.X on the next line, Node v24 misparses.
// Fix: Change the trailing comma to a semicolon for all lines matching:
//     BigInt(0),$rt_java. OR
//     <BigInt expression>,\n    $rt_java.
// This breaks the comma-chaining that triggers the bug.
const before = code.length;

// Fix 1: Long_div followed by $rt_java.Long_udiv
// Change:    Long_div = ..., <newline> $rt_java.Long_udiv
// To:        Long_div = ...;\n    $rt_java.Long_udiv
code = code.replace(
    /Long_div = \(a, b\) => \{ if \(b === Long_ZERO\) return Long_ZERO; var r = a \/ b; return Number\.isFinite\(r\) \? BigInt\.asIntN\(64, r\) : Long_ZERO; \},\n    \$rt_java\.Long_udiv/g,
    (match) => {
        count++;
        return match.replace('},', '};');
    }
);

// Fix 2: Long_div followed by $rt_java.Long_urem
// Change:    Long_div = ..., <newline> $rt_java.Long_urem
code = code.replace(
    /Long_div = \(a, b\) => \{ if \(b === Long_ZERO\) return Long_ZERO; var r = a \/ b; return Number\.isFinite\(r\) \? BigInt\.asIntN\(64, r\) : Long_ZERO; \},\n    \$rt_java\.Long_urem/g,
    (match) => {
        count++;
        return match.replace('},', '};');
    }
);

// Fix 3: Long_rem followed by $rt_java.Long_urem
// The Long_rem line ends with a comma before $rt_java.Long_urem
// Change:    Long_rem = ..., <newline> $rt_java.Long_urem
code = code.replace(
    /Long_rem = \(a, b\) => \{ if \(b === Long_ZERO\) return Long_ZERO; var r = a % b; return Number\.isFinite\(r\) \? BigInt\.asIntN\(64, r\) : Long_ZERO; \},\n    \$rt_java\.Long_urem/g,
    (match) => {
        count++;
        return match.replace('},', '};');
    }
);

// Fix 4: More broadly, find ALL cases where
//     <expression ending with >,\n    $rt_java.
// and replace the comma with semicolon
// Be specific: match BigInt expressions followed by $rt_java on next line
const bigIntThenRthJava = /(\bBigInt[^;,\n]{0,200}?),(\n    \$rt_java\.)/g;
let broadCount = 0;
code = code.replace(bigIntThenRthJava, (match, expr, nlPart) => {
    // Only replace if this looks like a comma-chain continuation
    if (expr.includes('Long_') || expr.includes('BigInt')) {
        broadCount++;
        return expr + ';' + nlPart;
    }
    return match;
});

if (broadCount > 0) count += broadCount;

const after = code.length;
console.log(`Fixed ${count} comma→semicolon conversions (${before} → ${after} bytes)`);

if (code !== original) {
    fs.writeFileSync(path, code);
    console.log(`Written: ${path}`);

    // Verify
    try {
        require('child_process').execSync(`node --check "${path}"`, { stdio: 'pipe' });
        console.log('✓ node --check PASSED');
    } catch(e) {
        console.log('✗ node --check FAILED');
        // Revert
        fs.writeFileSync(path, original);
        console.log('Reverted to original');
        process.exit(1);
    }
} else {
    console.log('No changes needed');
}
