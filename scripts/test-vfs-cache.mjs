#!/usr/bin/env node
// 测试脚本：测试 VFS 缓存功能

import { readFileSync } from 'fs';

console.log('=== VFS 缓存测试指南 ===\n');

console.log('1. 启动开发服务器:');
console.log('   cd work && ./gradlew serveWebRun');
console.log('   或者: npm run serve (如果配置了)\n');

console.log('2. 打开浏览器开发者工具 (F12)');
console.log('   - 打开 Application 标签页');
console.log('   - 左侧选择 IndexedDB');
console.log('   - 展开 webmc_vfs 数据库\n');

console.log('3. 首次加载测试:');
console.log('   - 访问 http://localhost:8080');
console.log('   - 查看 Console，应该看到:');
console.log('     [VfsCache] Miss: game.vfs');
console.log('     [VfsCache] Cached: game.vfs (~150 MB)');
console.log('   - 记录加载时间\n');

console.log('4. 二次加载测试:');
console.log('   - 刷新页面 (F5)');
console.log('   - 查看 Console，应该看到:');
console.log('     [VfsCache] Hit: game.vfs (150.xx MB)');
console.log('   - 记录加载时间（应该快 80-90%）\n');

console.log('5. 清除缓存测试:');
console.log('   - 在 Console 执行: await window.webmc.clearCache()');
console.log('   - 刷新页面，应该重新下载\n');

console.log('6. 预期结果:');
console.log('   首次加载: 3-15s (下载 + 解压 + 缓存)');
console.log('   二次加载: 0.5-1s (从 IndexedDB 读取)');
console.log('   提升: 80-90%\n');

console.log('=== 测试提示 ===');
console.log('- 如果看不到 IndexedDB，检查是否在无痕模式');
console.log('- Safari 可能会弹出存储权限提示');
console.log('- 容量不足时会自动降级到内存缓存');
