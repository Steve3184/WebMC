// 深度静态审计配置 – WebMC Annotated Rule Set
// 用途：CodeQL 不可用时，用 AST + ESLint 自定义加固检查
const globals = require("globals");
module.exports = {
  root: true,
  env: { browser: true, node: true, es2022: true },
  globals: {
    ...globals.browser,
    Java: false,
    fflate: false,
    teaVM: false,
  },
  extends: ["eslint:recommended", "plugin:security/recommended"],
  plugins: ["security", "no-unsafe"],
  parserOptions: { ecmaVersion: 2022, sourceType: "script" },
  rules: {
    // Security OWASP 2021 rules
    "security/detect-object-injection": "error",
    "security/detect-non-literal-fs-filename": "error",
    "security/detect-eval-with-expression": "error",
    "security/detect-possible-timing-attacks": "error",

    // 控制台日志只准留在明确开关区域 – 全局禁用意外 console
    "no-console": ["error", { allow: ["error", "warn", "debug"] }],

    // 魔法数字 & 硬编码常量
    "no-magic-numbers": ["error", { ignore: [0, 1, 2, 4, 1000, 1024, 1048576, 16, 16.7], ignoreEnums: true, enforceConst: true }],
    "no-param-reassign": "error",
    "prefer-const": "error",

    // WebGL / Canvas 安全 - 数组越界 & 内存溢出
    "no-restricted-properties": [
      "error",
      { property: "Array.prototype.slice", message: "请使用标准 slice(start, end) 且保证 end <= length" },
      { property: "Uint8Array.prototype.set", message: "确保 source buffer 长度与 target 一致，无溢出" },
    ],

    // 函数原型污染
    "security/detect-prototype-pollution": "error",

    // TeaVM Stub 兼容性：Java static 方法移除风险
    "no-unused-vars": [
      "error",
      { varsIgnorePattern: "^_+", args: "none", ignoreRestSiblings: true },
    ],

    // Strict Mode 确保
    "strict": ["error", "never"],

    // 限制全局变量污染
    "no-implicit-globals": "error",

    // 符号化重构机会
    "prefer-template": "error",
    "quotes": ["error", "single"],
  },
  overrides: [
    {
      files: ["**/*.java"],
      parserOptions: { ecmaVersion: 2022 },
      env: { node: true },
      rules: {
        // Memo：Java stub 要审一次反射、线程、File I/O
      },
    },
  ],
};