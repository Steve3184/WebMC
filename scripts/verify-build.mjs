#!/usr/bin/env node
// 测试脚本：验证编译产物和运行测试

import { readFileSync, statSync, existsSync } from 'fs';
import { execSync } from 'child_process';

console.log('=== WebMC 构建验证测试 ===\n');

// 1. 检查 game.js 是否生成
console.log('1. 检查编译产物...');
const gameJsPath = 'work/build/generated/teavm/js/game.js';
if (existsSync(gameJsPath)) {
  const stats = statSync(gameJsPath);
  const sizeMB = (stats.size / 1024 / 1024).toFixed(2);
  console.log(`   ✓ game.js 已生成 (${sizeMB} MB)`);
} else {
  console.log('   ✗ game.js 未找到');
  process.exit(1);
}

// 2. 检查 game.vfs 是否生成
console.log('\n2. 检查 VFS 资源包...');
const vfsPath = 'work/build/game.vfs';
if (existsSync(vfsPath)) {
  const stats = statSync(vfsPath);
  const sizeMB = (stats.size / 1024 / 1024).toFixed(2);
  console.log(`   ✓ game.vfs 已生成 (${sizeMB} MB)`);
} else {
  console.log('   ✗ game.vfs 未找到');
}

// 3. 检查 web-run 目录
console.log('\n3. 检查 web-run 输出目录...');
const webRunFiles = [
  'work/build/web-run/index.html',
  'work/build/web-run/bootstrap.js',
  'work/build/web-run/game.js',
  'work/build/web-run/game.vfs'
];

webRunFiles.forEach(file => {
  if (existsSync(file)) {
    console.log(`   ✓ ${file.split('/').pop()}`);
  } else {
    console.log(`   ✗ ${file.split('/').pop()} 未找到`);
  }
});

// 4. 分析 shader 编译日志
console.log('\n4. 分析 shader 编译结果...');
if (existsSync('build.log')) {
  const log = readFileSync('build.log', 'utf-8');

  // 查找 shader 相关错误
  const shaderErrors = log.match(/\[mc-probe\] ShaderManager.*invalid/gi);
  const glErrors = log.match(/\[mc-web\/gl\] Shader compile error/gi);

  if (shaderErrors || glErrors) {
    console.log(`   ⚠ 发现 shader 问题:`);
    if (shaderErrors) console.log(`     - ShaderManager 报告: ${shaderErrors.length} 条`);
    if (glErrors) console.log(`     - GL 编译错误: ${glErrors.length} 条`);
    console.log(`   查看 build.log 获取详情`);
  } else {
    console.log(`   ✓ 未发现 shader 编译错误`);
  }

  // 查找编译时间
  const timeMatch = log.match(/BUILD SUCCESSFUL in ([\d\w\s]+)/);
  if (timeMatch) {
    console.log(`\n✓ 编译成功，耗时: ${timeMatch[1]}`);
  }
} else {
  console.log('   ⚠ 编译日志未找到');
}

console.log('\n=== 测试完成 ===');
