// patch-high-1-glfw-swap.js – AST patch for High-1
const fs = require('fs');
const path = require('path');

const targetFile = path.join(__dirname, '../addons/lwjgl-stubs/src/main/java/org/lwjgl/glfw/GLFW.java');
const content = fs.readFileSync(targetFile, 'utf8');

if (!content.includes('public static void glfwSwapBuffers(long win) {')) {
  console.error('❌ 目标文件结构已改变，手动检查 abort');
  process.exit(1);
}

// 基于 AST-safe 字符串替换：把 sleep-loop 替换为 glFinish()
const patched = content
  .replace(
    /public static void glfwSwapBuffers\(long win\) \{[\s\S]*?try \{ Thread\.sleep\(1\); \} catch \(InterruptedException ignored\) \{\}[\s\S]*?\}/,
    `public static void glfwSwapBuffers(long win) {
    // High-1: WebGL 无 swapBuffers；用 glFinish 同步并禁止误用以规避 BFCP
    if (win != 0L) WindowBackendHolder.current().makeContextCurrent(win);
    org.lwjgl.opengl.GL11.glFinish();
  }`
  );

fs.writeFileSync(targetFile, patched);
console.log('✅ High-1 patch applied: glfwSwapBuffers → glFinish()');