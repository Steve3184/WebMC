const fs = require('fs');
const vm = require('vm');

const content = fs.readFileSync('build/web-run/game.js', 'utf8');

console.log('File size:', content.length, 'bytes');

// Try to compile with Function constructor
try {
    new Function(content);
    console.log('Function constructor: SUCCESS');
} catch (e) {
    console.log('Function constructor FAILED:', e.message);

    // Try to find where it fails
    const lines = content.split('\n');
    console.log('Total lines:', lines.length);

    // Check for problematic patterns
    const problems = [];

    // 1. Check for ASI issues (line starting with binary operator)
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        const prevLine = i > 0 ? lines[i-1].trim() : '';

        if (line && prevLine && !prevLine.endsWith(';') && !prevLine.endsWith('{') &&
            !prevLine.endsWith('}') && !prevLine.endsWith(',') &&
            /^[+\-*\/%&|^!<>=?]/.test(line)) {
            problems.push({line: i+1, type: 'ASI', text: prevLine.substring(prevLine.length-30) + ' <- ' + line.substring(0, 30)});
        }
    }

    if (problems.length > 0) {
        console.log('\nPotential ASI issues:', problems.slice(0, 5));
    }

    // 2. Check for regex that could be confused with division
    // Look for common patterns like "if (x) /pattern/.test(y)"
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        if (/[^/]\/[^/".\[\]()?{};\n]/.test(line) && !line.includes('Regex') && !line.includes('RegExp')) {
            // Potential regex issue
        }
    }
}

// Try Node's native parser if available
try {
    const acorn = require('acorn');
    console.log('\nAcorn available, trying to parse...');
} catch (e) {
    console.log('\nAcorn not available, skipping native parse');
}
