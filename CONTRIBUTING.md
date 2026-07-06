# 贡献指南

感谢你考虑为 WebMC 贡献代码！

## 如何贡献

### 1. Fork & Clone

```bash
# Fork 后克隆你的仓库
git clone https://github.com/YOUR_USERNAME/WebMC.git
cd WebMC

# 添加上游仓库
git remote add upstream https://github.com/Steve3184/WebMC.git
```

### 2. 创建分支

```bash
# 从 main 创建功能分支
git checkout -b feature/your-feature-name

# 或修复分支
git checkout -b fix/issue-description
```

### 3. 开发

```bash
# 同步上游更改
git fetch upstream
git merge upstream/main

# 安装依赖
git submodule update --init --recursive

# 本地构建测试
bash scripts/dev-wsl.sh build
```

### 4. 提交

```bash
# 暂存更改
git add .

# 提交 (使用语义化提交信息)
git commit -m "feat: add new GLFW callback"

# 推送到你的 fork
git push origin feature/your-feature-name
```

### 5. 创建 Pull Request

1. 在 GitHub 上创建 PR
2. 填写 PR 模板
3. 等待审查

## 提交规范

### 格式

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### 类型

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更改 |
| `style` | 代码格式 (不影响功能) |
| `refactor` | 重构 (非功能变更) |
| `perf` | 性能优化 |
| `test` | 添加测试 |
| `chore` | 构建/工具更改 |

### 示例

```
feat(lwjgl-stubs): add mouse scroll callback

Implement GLFWScrollCallbackI for scroll wheel input support.
Fixes #12

Closes #12
```

## 代码规范

### Java

- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 使用 4 空格缩进
- 变量名清晰表达意图

### 命名约定

```java
// 类名: 驼峰式
public class WindowBackend { }

// 接口: 带 I 后缀 (LWJGL 惯例)
public interface GLFWKeyCallbackI { }

// 常量: 全大写下划线分隔
public static final int GLFW_PRESS = 1;
```

### 文档注释

```java
/**
 * Stub implementation of GLFW key callback.
 * 
 * <p>Maps browser keydown/keyup events to Minecraft input system.
 *
 * @see GLFWKeyCallback
 */
public class GLFWKeyCallbackI implements GLFWKeyCallback {
    // ...
}
```

## TeaVM 兼容性

WebMC 运行在浏览器环境中，有些 Java 特性不可用：

### 禁止使用

- ❌ `Thread.sleep()` - 使用 `setTimeout`
- ❌ `System.exit()` - 使用异常处理
- ❌ 原生库调用 - 全部 JS stub
- ❌ 复杂反射 - 预编译无法优化

### 推荐使用

- ✅ `@JSBody` 注解调用 JS
- ✅ `requestAnimationFrame` 循环
- ✅ IndexedDB 存储

## 测试

### 本地测试

```bash
# 运行完整构建
bash scripts/dev-wsl.sh all

# 检查输出
open build/web-run/index.html
```

### 手动测试清单

- [ ] 游戏启动成功
- [ ] 主菜单显示
- [ ] 点击单人游戏能进入世界选择
- [ ] 键盘输入正常
- [ ] 鼠标点击正常
- [ ] 鼠标移动正常
- [ ] 滚动正常
- [ ] 控制台无 Error 级别错误

## 问题报告

### Bug 报告

请包含：

1. **复现步骤**: 清晰的操作步骤
2. **预期行为**: 应该发生什么
3. **实际行为**: 实际发生了什么
4. **环境信息**:
   - 浏览器版本
   - 操作系统
   - Java 版本
5. **控制台错误**: 完整的错误信息

### 功能请求

请包含：

1. **使用场景**: 为什么要这个功能
2. **期望实现**: 功能应该如何工作
3. **替代方案**: 你考虑过其他方案吗

## 许可证

通过贡献代码，你同意将你的代码按照项目的开源许可证发布。

## 行为准则

请尊重所有参与者：

- 使用友好和包容的语言
- 尊重不同的观点和经验
- 建设性地接受建设性的批评
- 专注于社区的最佳利益

## 联系方式

- [GitHub Issues](https://github.com/Steve3184/WebMC/issues)
- [GitHub Discussions](https://github.com/Steve3184/WebMC/discussions)
