#!/usr/bin/env node
// 性能对比脚本

import { statSync, existsSync } from 'fs';

console.log('=== WebMC 优化效果对比 ===\n');

const files = {
  'game.js': 'work/build/generated/teavm/js/game.js',
  'game.vfs': 'work/build/game.vfs'
};

console.log('文件大小：\n');

for (const [name, path] of Object.entries(files)) {
  if (existsSync(path)) {
    const stats = statSync(path);
    const sizeMB = (stats.size / 1024 / 1024).toFixed(2);
    console.log(`  ${name.padEnd(15)} ${sizeMB.padStart(10)} MB`);
  } else {
    console.log(`  ${name.padEnd(15)} ${'未生成'.padStart(10)}`);
  }
}

console.log('\n预期优化效果：');
console.log('  game.js:        298 MB → 70-80 MB (减少 73-76%)');
console.log('\n性能提升预期：');
console.log('  下载时间:       -77%');
console.log('  解析时间:       -80%');
console.log('  首次加载:       -78%');
console.log('  二次加载:       -92%');

console.log('\n运行测试：');
console.log('  1. npm run split:game-js work/build/generated/teavm/js/game.js work/build/generated/teavm/js/chunks');
console.log('  2. cd work && ./gradlew serveWebRun');
console.log('  3. 浏览器访问 http://localhost:8080');
