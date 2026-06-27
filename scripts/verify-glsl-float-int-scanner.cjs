const fs = require('fs');
const path = require('path');

const ROOT = process.cwd();

function oldRegexRewrite(source) {
  return source
    .replace(/(\.\w+|\))(\s*[*/]\s*)(\d+)(?!\.\d|\.|\w)/g, '$1$2$3.0')
    .replace(/(?<![\w.])(\d+)(\s*[*/]\s*)(?=\w+\.)/g, '$1.0$2');
}

function isDigit(c) {
  return c >= '0' && c <= '9';
}

function isWord(c) {
  return !!c && (
    (c >= 'a' && c <= 'z') ||
    (c >= 'A' && c <= 'Z') ||
    (c >= '0' && c <= '9') ||
    c === '_'
  );
}

function isWhitespace(c) {
  return c === ' ' || c === '\n' || c === '\r' || c === '\t' || c === '\f';
}

function skipWhitespaceForward(source, index) {
  while (index < source.length && isWhitespace(source[index])) index++;
  return index;
}

function skipWhitespaceBackward(source, index) {
  while (index >= 0 && isWhitespace(source[index])) index--;
  return index;
}

function scanDigitsForward(source, index) {
  while (index < source.length && isDigit(source[index])) index++;
  return index;
}

function scanDigitsBackward(source, index) {
  while (index >= 0 && isDigit(source[index])) index--;
  return index + 1;
}

function leftLooksFloat(source, leftEnd) {
  if (leftEnd < 0) return false;
  if (source[leftEnd] === ')') return true;
  if (!isWord(source[leftEnd])) return false;

  let start = leftEnd;
  while (start >= 0 && isWord(source[start])) start--;
  return start >= 0 && source[start] === '.' && start < leftEnd;
}

function rightLooksIdentifierField(source, rightStart) {
  if (rightStart >= source.length || !isWord(source[rightStart])) return false;

  let index = rightStart + 1;
  while (index < source.length && isWord(source[index])) index++;
  return index < source.length && source[index] === '.';
}

function isBareIntegerStart(source, digitStart) {
  const before = digitStart - 1;
  if (before < 0) return true;
  const c = source[before];
  return c !== '.' && !isWord(c);
}

function isBareIntegerEnd(source, digitEnd) {
  if (digitEnd >= source.length) return true;
  const c = source[digitEnd];
  return c !== '.' && !isWord(c);
}

function scannerRewrite(source) {
  let out = null;
  let copyFrom = 0;

  for (let i = 0; i < source.length; i++) {
    const c = source[i];
    if (c !== '*' && c !== '/') continue;

    const leftEnd = skipWhitespaceBackward(source, i - 1);
    const rightStart = skipWhitespaceForward(source, i + 1);

    if (rightStart < source.length && isDigit(source[rightStart]) && leftLooksFloat(source, leftEnd)) {
      const digitEnd = scanDigitsForward(source, rightStart);
      if (isBareIntegerEnd(source, digitEnd)) {
        if (out === null) out = [];
        out.push(source.slice(copyFrom, digitEnd), '.0');
        copyFrom = digitEnd;
        i = digitEnd - 1;
        continue;
      }
    }

    if (leftEnd >= 0 && isDigit(source[leftEnd]) && rightLooksIdentifierField(source, rightStart)) {
      const digitStart = scanDigitsBackward(source, leftEnd);
      if (isBareIntegerStart(source, digitStart)) {
        const digitEnd = leftEnd + 1;
        if (digitEnd > copyFrom) {
          if (out === null) out = [];
          out.push(source.slice(copyFrom, digitEnd), '.0');
          copyFrom = digitEnd;
        }
      }
    }
  }

  if (out === null) return source;
  out.push(source.slice(copyFrom));
  return out.join('');
}

function walk(dir, files = []) {
  if (!fs.existsSync(dir)) return files;
  for (const name of fs.readdirSync(dir)) {
    const file = path.join(dir, name);
    const stat = fs.statSync(file);
    if (stat.isDirectory()) {
      walk(file, files);
    } else if (/\.(vsh|fsh|glsl)$/i.test(name)) {
      files.push(file);
    }
  }
  return files;
}

function firstDifference(a, b) {
  let index = 0;
  while (index < a.length && index < b.length && a[index] === b[index]) index++;
  return index;
}

function checkCase(label, source) {
  const expected = oldRegexRewrite(source);
  const actual = scannerRewrite(source);
  if (expected !== actual) {
    const index = firstDifference(expected, actual);
    throw new Error(JSON.stringify({
      label,
      index,
      source: source.slice(Math.max(0, index - 100), index + 180),
      expected: expected.slice(Math.max(0, index - 100), index + 180),
      actual: actual.slice(Math.max(0, index - 100), index + 180)
    }, null, 2));
  }
}

const edgeCases = [
  'texCoord.x * 16',
  'texCoord.x*16;',
  'foo() / 15',
  '16 * texCoord.x',
  '116 * texCoord.x',
  'a16 * texCoord.x',
  '.x * 16.0',
  '.x * 16u',
  '.x * 16;',
  '1 * uv.x * 2',
  '.x * 1 / uv.y',
  'ivec2(uv) / 16',
  'uv / 16',
  '1.0 * uv.x',
  '1*uv.x',
  '1 / uv.x',
  'a.1 * 2',
  'color.rgb / 255',
  'normal.x / 2\n2 * normal.y',
  'float v = clamp(foo(), 0, 1) / 2;'
];

const shaderRoots = [
  path.join(ROOT, 'work', 'build', 'resources', 'main', 'assets'),
  path.join(ROOT, 'work', 'src', 'main', 'resources', 'assets'),
  path.join(ROOT, 'shaders')
];

let checked = 0;
for (const source of edgeCases) {
  checkCase(`edge:${checked}`, source);
  checked++;
}

for (const root of shaderRoots) {
  for (const file of walk(root)) {
    checkCase(path.relative(ROOT, file), fs.readFileSync(file, 'utf8'));
    checked++;
  }
}

console.log(`glslFloatIntScanner.ok=true checked=${checked}`);
