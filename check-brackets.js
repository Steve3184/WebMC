const fs = require('fs');
const content = fs.readFileSync('build/web-run/game.js', 'utf8');

// Check for bracket mismatches in valid code regions
let paren = 0, brace = 0, bracket = 0;
let line = 1;
let inString = false;
let stringChar = '';
let escape = false;
let problem = null;

for (let i = 0; i < content.length; i++) {
    const c = content[i];

    if (escape) {
        escape = false;
        continue;
    }

    if (c === '\\' && inString) {
        escape = true;
        continue;
    }

    if (!inString && (c === '"' || c === "'" || c === '`')) {
        inString = true;
        stringChar = c;
        continue;
    }

    if (inString && c === stringChar) {
        inString = false;
        continue;
    }

    if (!inString) {
        if (c === '(') paren++;
        if (c === ')') paren--;
        if (c === '{') brace++;
        if (c === '}') brace--;
        if (c === '[') bracket++;
        if (c === ']') bracket--;

        if (paren < 0 || brace < 0 || bracket < 0) {
            problem = { pos: i, char: c, line };
            break;
        }
    }

    if (c === '\n') line++;
}

console.log('Final counts - Paren:', paren, 'Brace:', brace, 'Bracket:', bracket);
if (problem) {
    console.log('Problem at pos', problem.pos, 'line', problem.line, ':', JSON.stringify(content.substring(problem.pos-20, problem.pos+20)));
} else {
    console.log('No bracket mismatch found in valid code regions');
}
