const fs = require('fs');
let content = fs.readFileSync('addons/web/bootstrap.js', 'utf8');
const lines = content.split('\n');

// Lines 8-24 (indices 7-24) - the IIFE
const oldSnippet = lines.slice(7, 25).join('\n');

const newSnippet = `(function () {
    // ── Patch BigInt constructor to handle NaN / Infinity ────────────────────
    // TeaVM's Long_fromNumber calls BigInt(NaN) in many places.
    var OrigBigInt = BigInt;
    BigInt = function(val) {
        var n = Number(val);
        if (n !== n || !Number.isFinite(n)) { return OrigBigInt(0); }
        try { return OrigBigInt(val); } catch(e) { return OrigBigInt(0); }
    };
    BigInt.prototype = OrigBigInt.prototype;

    // ── Protect BigInt.asIntN / asUintN from NaN / Infinity ────────────────────
    var OrigAsIntN = BigInt.asIntN;
    var OrigAsUintN = BigInt.asUintN;
    BigInt.asIntN = function (bits, val) {
        if (val !== val || !Number.isFinite(val)) { return OrigAsIntN(bits, BigInt(0)); }
        try { return OrigAsIntN(bits, val); } catch (e) { return OrigAsIntN(bits, BigInt(0)); }
    };
    BigInt.asUintN = function (bits, val) {
        if (val !== val || !Number.isFinite(val)) { return OrigAsUintN(bits, BigInt(0)); }
        try { return OrigAsUintN(bits, val); } catch (e) { return OrigAsUintN(bits, BigInt(0)); }
    };
})

`;

if (content.includes(oldSnippet)) {
    content = content.replace(oldSnippet, newSnippet);
    fs.writeFileSync('addons/web/bootstrap.js', content);
    console.log('SUCCESS: Patched BigInt constructor');
} else {
    console.log('ERROR: Pattern not found');
    console.log('Looking for:', JSON.stringify(oldSnippet.slice(0, 100)));
}
