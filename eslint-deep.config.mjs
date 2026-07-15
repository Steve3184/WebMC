// DeepScan JS Flat Config – works with ES2024 + vacuolated AST
export default [
  {
    name: "WebMC Deep Audit",
    files: ["**/*.{js,mjs,ts}"],
    ignores: ["**/node_modules/*", "**/build/*", "**/*test*.js"],
    languageOptions: {
      globals: { ...require("globals").browser },
      ecmaVersion: 2024,
      sourceType: "script",
    },
    rules: {
      // 20 OWASP rules + TeaVM/GLFW Safe
      "no-eval": "error",
      "no-new-func": "error",
      "no-implied-eval": "error",
      "security/detect-object-injection": "error",
      "security/detect-possible-timing-attacks": "error",
      "security/detect-non-literal-fs-filename": "error",
      "security/detect-new-buffer": "error",
      "no-console": ["error", { allow: ["error", "warn"] }],

      // 安全数字 & 字符串池检查
      "no-magic-numbers": ["warn", { ignore: [0, 1, 2, 4, 16, 32, 1000, 1024, 1048576, 8192] }],
      "prefer-const": "error",
      "eqeqeq": "error",
      "@typescript-eslint/no-explicit-any": ["error", { fixToUnknown: true, ignoreRestArgs: false }],

      // WebGL / Array 边界（Buffer overflow prone）
      "array-callback-return": "error",
      "no-empty": "warn",
    },
  },
];