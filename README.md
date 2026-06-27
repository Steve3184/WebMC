# Minecraft 物品贴图 AI 生成 & LoRA 训练项目

> 基于 Stable Diffusion + LoRA 的 Minecraft 物品/纹理贴图生成工具集
> 支持 16×16、32×32、64×64、128×128 像素风格

## 📁 项目结构

```
D:\MinecraftTextureAI\
├── 启动工具箱.bat              # 🚀 一键启动入口（双击运行）
├── setup_project.py           # 📥 项目初始化（下载模型、克隆仓库）
├── generate_workflow.py       # 🎨 一键生成工作流（生成→像素化→打包）
├── train_lora.py              # 🏋️ LoRA 训练启动器
├── README.md                  # 📖 本文件
│
├── stable-diffusion-webui/    # SD-WebUI（AI 生成界面）
│   └── models/
│       ├── Lora/              # LoRA 模型
│       └── Stable-diffusion/  # SD 基础模型
│
├── kohya_ss/                  # Kohya_ss（LoRA 训练工具）
│
├── dataset/                   # 训练数据集
│   ├── 10_minecraft_item/     # Minecraft 物品图片 + 标签
│   └── reg_images/            # 正则化图片（可选）
│
├── scripts/                   # 工具脚本
│   ├── pixelate.py            # 像素化降采样
│   ├── caption_generator.py   # 标签生成器
│   ├── sd_api_generate.py     # SD-WebUI API 批量生成
│   ├── pack_builder.py        # 资源包打包
│   └── extract_textures.py    # 从 Minecraft JAR 提取贴图
│
└── output/                    # 输出目录
    ├── raw/                   # 原始生成图片
    ├── 16x16/                 # 16×16 像素贴图
    ├── 32x32/                 # 32×32 像素贴图
    ├── lora_output/           # 训练输出的 LoRA
    └── resourcepacks/         # 打包好的资源包
```

## 🚀 快速开始

### 1. 初始化项目

双击 `启动工具箱.bat`，选择 **1. 项目初始化**

或命令行：
```bash
python setup_project.py
```

这将自动：
- 下载 Plixel-Minecraft LoRA 模型
- 下载 SD 2.1 基础模型（约 5GB）
- 克隆 Minecraft-Lora-Training 仓库

### 2. 启动 SD-WebUI

双击 `启动工具箱.bat`，选择 **2. 启动 SD-WebUI**

或命令行：
```bash
cd stable-diffusion-webui
webui.bat --api --listen
```

浏览器打开 http://localhost:7860

### 3. 生成贴图

双击 `启动工具箱.bat`，选择 **4. 一键生成贴图工作流**

或命令行：
```bash
python generate_workflow.py
```

### 4. 像素化 & 打包

```bash
# 降采样到 16×16
python scripts/pixelate.py -i ./output/raw -o ./output/16x16 -s 16

# 打包资源包
python scripts/pack_builder.py -t ./output/16x16 -o ./output/resourcepacks -n "My_Pack"
```

## 🏋️ LoRA 训练

### 准备数据集

```bash
# 从 Minecraft JAR 提取贴图
python scripts/extract_textures.py

# 生成训练标签
python scripts/caption_generator.py -d ./dataset/10_minecraft_item
```

### 启动训练

双击 `启动工具箱.bat`，选择 **5. LoRA 训练启动器**

或命令行：
```bash
python train_lora.py
```

提供三种训练预设：
- **快速测试**：~10 分钟，验证流程
- **标准训练**：1-3 小时，推荐
- **高质量训练**：3-8 小时，追求最佳效果

## 📋 提示词模板

### 基础物品
```
minecraft item, [物品描述], pixel art style, flat shading, transparent background, vanilla minecraft style, clean pixel edges, limited color palette, no text, centered
```

### 加 LoRA
```
<lora:plixel-minecraft:1.2>, minecraft item icon, [详细描述], 16x16 pixel art, blocky, retro game style, flat colors, sharp pixels, transparent background
```

### 负面提示词
```
blurry, low resolution, realistic, 3d, photo, gradient, text, watermark, extra details, deformed, noisy, jpeg artifacts
```

## 🔗 相关项目

| 项目 | 链接 |
|------|------|
| Stable Diffusion WebUI | https://github.com/AUTOMATIC1111/stable-diffusion-webui |
| Kohya_ss | https://github.com/bmaltais/kohya_ss |
| Minecraft LoRA Training | https://github.com/Jack-Bagel/Minecraft-Lora-Training |
| Plixel-Minecraft LoRA | https://huggingface.co/OVAWARE/plixel-minecraft |
| Minecraft-Items-SDXL-LoRA | https://huggingface.co/wybxc/minecraft-items-sdxl-lora |
| TextureMaker | https://github.com/LyFl0w/TextureMaker |
| Piskel（像素画编辑器） | https://www.piskelapp.com |

## ⚙️ 硬件要求

- **最低**：NVIDIA GPU ≥ 8GB VRAM
- **推荐**：NVIDIA GPU ≥ 12GB VRAM
- **存储**：至少 15GB 可用空间

## 📝 工作流总结

```
准备数据集 → 训练 LoRA → SD-WebUI 生成 → 像素化降采样 → 资源包打包 → 放入 .minecraft/resourcepacks/
```
