const fs = require('fs');
const buf = fs.readFileSync('work/build/web-run/game.js');
const lines = buf.toString('utf8').split('\n');

// Find line 160-167 context
console.log('=== Context around the issue ===');
for (let i = 155; i < 175; i++) {
    const prefix = i === 166 ? '>>>' : '   ';
    console.log(prefix + (i+1) + '| ' + lines[i].substring(0, 120));
}

// Try to reproduce with a minimal example
console.log('\n=== Minimal reproduction ===');
const minimal = `"use strict";
var main;
(function() {
    let $rt_java = {};
    $rt_java.foo = 1;
    $rt_java.bar = 2;
    $rt_java.Long_udiv = (a, b) => 0;
    $rt_java.Long_urem = (a, b) => 0;
})();`;
try {
    new Function(minimal);
    console.log('minimal: OK');
} catch(e) {
    console.log('minimal: FAIL - ' + e.message);
}

// What about adding a comma after Long_gt?
const withComma = `"use strict";
var main;
(function() {
    let $rt_java = {};
    $rt_java.foo = 1;
    $rt_java.bar = 2,
    $rt_java.Long_udiv = (a, b) => 0;
})();`;
try {
    new Function(withComma);
    console.log('withComma: OK');
} catch(e) {
    console.log('withComma: FAIL - ' + e.message);
}

// What about missing semicolons?
const noSemi = `"use strict";
var main;
(function() {
    let $rt_java = {}
    $rt_java.foo = 1
    $rt_java.Long_udiv = (a, b) => 0
})()`;
try {
    new Function(noSemi);
    console.log('noSemi: OK');
} catch(e) {
    console.log('noSemi: FAIL - ' + e.message);
}