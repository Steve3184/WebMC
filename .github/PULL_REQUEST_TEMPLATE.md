name: Pull Request
description: Submit changes to the project
body:
  - type: markdown
    attributes:
      value: |
        ## 📝 Pull Request

        感谢你提交 PR！请填写以下信息。

  - type: textarea
    id: description
    attributes:
      label: PR 描述
      description: 简要描述这个 PR 做了什么
      placeholder: |
        - 添加了新的 GLFW 回调实现
        - 修复了鼠标点击事件丢失的问题
        - 重构了 WindowBackend 接口
    validations:
      required: true

  - type: dropdown
    id: type
    attributes:
      label: PR 类型
      options:
        - Bug Fix
        - New Feature
        - Refactoring
        - Documentation
        - Performance
        - Testing
        - Other

  - type: textarea
    id: related
    attributes:
      label: 相关 Issue
      description: 链接相关的 Issue (如 #12, fixes #34)
      placeholder: "Fixes #12"

  - type: checkboxes
    id: checklist
    attributes:
      label: 检查清单
      options:
        - label: 我的代码遵循项目的代码规范
        - label: 我已经测试过这个更改
        - label: 我已经添加了必要的文档
        - label: 测试已通过 (如适用)

  - type: textarea
    id: testing
    attributes:
      label: 测试说明
      description: 描述你如何测试这个更改
      placeholder: |
        - 在本地构建成功
        - 手动测试了以下场景：
          - 键盘输入 ✓
          - 鼠标点击 ✓

  - type: textarea
    id: screenshots
    attributes:
      label: 截图/视频
      description: 如果有 UI 变更，请提供截图
