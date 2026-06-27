"""
Minecraft 物品贴图一键生成工作流
SD-WebUI 生成 → 像素化降采样 → 资源包打包
"""

import os
import sys
import json
import time
import argparse

# 项目路径
PROJECT_DIR = r"D:\MinecraftTextureAI"
SD_WEBUI_DIR = os.path.join(PROJECT_DIR, "stable-diffusion-webui")
LORA_DIR = os.path.join(SD_WEBUI_DIR, "models", "Lora")
SCRIPTS_DIR = os.path.join(PROJECT_DIR, "scripts")
OUTPUT_DIR = os.path.join(PROJECT_DIR, "output")


# ===== Minecraft 物品提示词库 =====
ITEM_PROMPTS = {
    # === 武器 ===
    "wooden_sword": "minecraft item, wooden sword with simple blade, pixel art, flat shading, transparent background, vanilla style",
    "stone_sword": "minecraft item, stone sword with rough edge, pixel art, flat shading, transparent background, vanilla style",
    "iron_sword": "minecraft item, iron sword with clean blade, pixel art, flat shading, transparent background, vanilla style",
    "golden_sword": "minecraft item, golden sword with ornate handle, pixel art, flat colors, transparent background, vanilla style",
    "diamond_sword": "minecraft item, glowing diamond sword with blue runes, enchanted, pixel art, flat shading, transparent background, vanilla style",
    "netherite_sword": "minecraft item, dark netherite sword with fire glow, pixel art, flat shading, transparent background, vanilla style",
    "bow": "minecraft item, wooden bow with string, pixel art, flat shading, transparent background, vanilla style",
    "crossbow": "minecraft item, wooden crossbow with metal mechanism, pixel art, flat shading, vanilla style",
    "trident": "minecraft item, trident with three prongs, pixel art, flat shading, transparent background, vanilla style",
    "arrow": "minecraft item, arrow with flint tip, pixel art, flat shading, vanilla style",
    # === 工具 ===
    "wooden_pickaxe": "minecraft item, wooden pickaxe with simple head, pixel art, flat shading, vanilla style",
    "stone_pickaxe": "minecraft item, stone pickaxe, pixel art, flat shading, vanilla style",
    "iron_pickaxe": "minecraft item, iron pickaxe with wooden handle, worn texture, pixel art style, clean edges",
    "golden_pickaxe": "minecraft item, golden pickaxe with ornate handle, pixel art, flat colors, transparent background, vanilla style",
    "diamond_pickaxe": "minecraft item, diamond pickaxe with blue gem head, pixel art, flat shading, vanilla style",
    "netherite_pickaxe": "minecraft item, dark netherite pickaxe, pixel art, flat shading, vanilla style",
    "wooden_axe": "minecraft item, wooden axe, pixel art, flat shading, vanilla style",
    "stone_axe": "minecraft item, stone axe, pixel art, flat shading, vanilla style",
    "iron_axe": "minecraft item, iron axe, pixel art, flat shading, vanilla style",
    "golden_axe": "minecraft item, golden axe, pixel art, flat shading, vanilla style",
    "diamond_axe": "minecraft item, diamond axe, pixel art, flat shading, vanilla style",
    "netherite_axe": "minecraft item, netherite axe, pixel art, flat shading, vanilla style",
    "wooden_shovel": "minecraft item, wooden shovel, pixel art, flat shading, vanilla style",
    "stone_shovel": "minecraft item, stone shovel, pixel art, flat shading, vanilla style",
    "iron_shovel": "minecraft item, iron shovel, pixel art, flat shading, vanilla style",
    "golden_shovel": "minecraft item, golden shovel, pixel art, flat shading, vanilla style",
    "diamond_shovel": "minecraft item, diamond shovel, pixel art, flat shading, vanilla style",
    "netherite_shovel": "minecraft item, netherite shovel, pixel art, flat shading, vanilla style",
    # === 防具 ===
    "iron_helmet": "minecraft item, iron helmet, pixel art, flat shading, vanilla style",
    "iron_chestplate": "minecraft item, iron chestplate, pixel art, flat shading, vanilla style",
    "diamond_helmet": "minecraft item, diamond helmet with blue gem, pixel art, flat shading, vanilla style",
    "diamond_chestplate": "minecraft item, diamond chestplate with blue gems, pixel art, flat shading, vanilla style",
    # === 食物 ===
    "apple": "minecraft item, red apple, pixel art, flat colors, centered, vanilla style",
    "golden_apple": "minecraft item, golden apple with glow, pixel art, flat colors, vanilla style",
    "bread": "minecraft item, bread loaf, pixel art, flat colors, centered, vanilla style",
    "cooked_beef": "minecraft item, cooked steak with grill marks, juicy, pixel art, flat colors, centered, vanilla style",
    "cooked_porkchop": "minecraft item, cooked porkchop, pixel art, flat colors, vanilla style",
    # === 材料 ===
    "coal": "minecraft item, lump of coal, pixel art, flat shading, vanilla style",
    "iron_ingot": "minecraft item, iron ingot bar, pixel art, flat shading, vanilla style",
    "gold_ingot": "minecraft item, gold ingot bar, pixel art, flat shading, vanilla style",
    "diamond": "minecraft item, diamond gem, pixel art, flat shading, vanilla style",
    "emerald": "minecraft item, emerald gem, pixel art, flat shading, vanilla style",
    "netherite_ingot": "minecraft item, dark netherite ingot, pixel art, flat shading, vanilla style",
    "lapis_lazuli": "minecraft item, lapis lazuli gem, pixel art, flat shading, vanilla style",
    # === 药水 ===
    "potion": "minecraft item, purple potion bottle with swirling liquid, pixel art icon, centered, vanilla style",
    # === 杂项 ===
    "torch": "minecraft item, wooden torch with bright flame, pixel art, flat shading, vanilla style",
    "bucket": "minecraft item, iron bucket, pixel art, flat shading, vanilla style",
    "compass": "minecraft item, compass with red needle, pixel art, flat shading, vanilla style",
    "clock": "minecraft item, clock with golden rim, pixel art, flat shading, vanilla style",
    "map": "minecraft item, exploration map, pixel art, flat shading, vanilla style",
    "book": "minecraft item, book, pixel art, flat shading, vanilla style",
    "enchanted_book": "minecraft item, enchanted book with glowing purple runes, magic, pixel art, flat shading, vanilla style",
    "ender_pearl": "minecraft item, green ender pearl with swirling particles, pixel art, centered, vanilla style",
    "totem_of_undying": "minecraft item, totem of undying with face, pixel art, flat shading, vanilla style",
    "shield": "minecraft item, wooden shield with iron border, defense, pixel art, flat colors, centered, vanilla style",
    "elytra": "minecraft item, elytra wings, pixel art, flat shading, vanilla style",
    # === 自定义 ===
    "crystal_staff": "minecraft item, crystal staff with floating gems, magic wand, pixel art, flat shading, vanilla style",
    "fire_wand": "minecraft item, fire wand with flame tip, magic, pixel art, flat shading, vanilla style",
    "ancient_amulet": "minecraft item, ancient golden amulet with emerald gem, relic style, pixel art, simple lighting, transparent background",
}

NEGATIVE_PROMPT = "blurry, low resolution, realistic, 3d, photo, gradient, text, watermark, extra details, deformed, noisy, jpeg artifacts, anti-aliased, smooth edges"
ENHANCE_SUFFIX = ", (minecraft style:1.3), pixel perfect, 16-bit aesthetic, no anti-aliasing, limited minecraft color palette"


def check_api(api_base):
    """检查 SD-WebUI API 是否可用"""
    try:
        import urllib.request
        req = urllib.request.Request(f"{api_base}/sdapi/v1/sd-models")
        with urllib.request.urlopen(req, timeout=5) as resp:
            return True
    except Exception:
        return False


def get_available_loras():
    """列出可用的 LoRA 模型"""
    if not os.path.isdir(LORA_DIR):
        return []
    return [os.path.splitext(f)[0] for f in os.listdir(LORA_DIR)
            if f.endswith((".safetensors", ".pt"))]


def generate_item(api_base, item_name, prompt, lora_name=None, lora_weight=1.0,
                  width=512, height=512, steps=30, cfg_scale=8, sampler="Euler a",
                  output_dir=None):
    """通过 API 生成单张图片"""
    import urllib.request
    import base64

    if output_dir is None:
        output_dir = os.path.join(OUTPUT_DIR, "raw")
    os.makedirs(output_dir, exist_ok=True)

    # 组装提示词
    full_prompt = prompt + ENHANCE_SUFFIX
    if lora_name:
        full_prompt = f"<lora:{lora_name}:{lora_weight}>, {full_prompt}"

    payload = {
        "prompt": full_prompt,
        "negative_prompt": NEGATIVE_PROMPT,
        "steps": steps,
        "cfg_scale": cfg_scale,
        "width": width,
        "height": height,
        "sampler_name": sampler,
        "seed": -1,
    }

    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        f"{api_base}/sdapi/v1/txt2img",
        data=data,
        headers={"Content-Type": "application/json"},
    )

    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            result = json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"    ❌ 生成失败: {e}")
        return None

    if "images" in result:
        for i, img_data in enumerate(result["images"]):
            img_bytes = base64.b64decode(img_data)
            filepath = os.path.join(output_dir, f"{item_name}.png")
            # 如果已存在则加序号
            n = 1
            while os.path.exists(filepath):
                filepath = os.path.join(output_dir, f"{item_name}_{n}.png")
                n += 1
            with open(filepath, "wb") as f:
                f.write(img_bytes)
            return filepath

    return None


def pixelate_items(input_dir, sizes=(16, 32)):
    """像素化降采样"""
    from PIL import Image

    for target_size in sizes:
        output_dir = os.path.join(OUTPUT_DIR, f"{target_size}x{target_size}")
        os.makedirs(output_dir, exist_ok=True)

        files = [f for f in os.listdir(input_dir) if f.lower().endswith(".png")]
        for fname in files:
            src = os.path.join(input_dir, fname)
            dst = os.path.join(output_dir, fname)
            img = Image.open(src).convert("RGBA")
            img = img.resize((target_size, target_size), Image.NEAREST)
            img.save(dst)
            print(f"    ✓ {fname} → {target_size}×{target_size}")


def build_pack(textures_dir, pack_name, resolution=16, pack_format=32):
    """构建资源包"""
    pack_dir = os.path.join(OUTPUT_DIR, "resourcepacks", pack_name)
    textures_target = os.path.join(pack_dir, "assets", "minecraft", "textures", "item")
    os.makedirs(textures_target, exist_ok=True)

    # pack.mcmeta
    mcmeta = {
        "pack": {
            "pack_format": pack_format,
            "description": f"AI Generated {resolution}x{resolution} Minecraft Texture Pack"
        }
    }
    with open(os.path.join(pack_dir, "pack.mcmeta"), "w", encoding="utf-8") as f:
        json.dump(mcmeta, f, indent=2, ensure_ascii=False)

    # 复制贴图
    files = [f for f in os.listdir(textures_dir) if f.lower().endswith(".png")]
    for fname in files:
        src = os.path.join(textures_dir, fname)
        dst = os.path.join(textures_target, fname)
        import shutil
        shutil.copy2(src, dst)

    print(f"  📦 资源包: {pack_dir}")
    print(f"  📋 包含 {len(files)} 个贴图")
    print(f"  📋 使用: 复制到 .minecraft/resourcepacks/")
    return pack_dir


def main():
    print()
    print("╔══════════════════════════════════════════════════════════╗")
    print("║     Minecraft 物品贴图一键生成工作流                   ║")
    print("╚══════════════════════════════════════════════════════════╝")

    api_base = "http://127.0.0.1:7860"

    # 检查 API
    print("\n🔍 检查 SD-WebUI...")
    if check_api(api_base):
        print("  ✅ SD-WebUI API 可用")
    else:
        print("  ❌ SD-WebUI API 不可用")
        print("  请先启动 SD-WebUI：")
        print(f"    cd {SD_WEBUI_DIR}")
        print(f"    webui.bat --api --listen")
        print()
        choice = input("  是否继续（离线模式，仅像素化/打包）？[y/N]: ").strip().lower()
        if choice != "y":
            return

    # 显示可用 LoRA
    loras = get_available_loras()
    print(f"\n📋 可用 LoRA 模型: {len(loras)}")
    for l in loras:
        print(f"  • {l}")

    # 选择物品
    print(f"\n🗡️ 可用物品预设: {len(ITEM_PROMPTS)}")
    item_names = list(ITEM_PROMPTS.keys())

    print("\n选择生成模式：")
    print("  1. 生成单个物品")
    print("  2. 生成武器类（剑、弓、弩、三叉戟）")
    print("  3. 生成工具类（镐、斧、锹）")
    print("  4. 生成材料类（矿石、锭、宝石）")
    print("  5. 生成全部物品")
    print("  6. 自定义提示词")
    print("  7. 仅像素化（从已有图片）")
    print("  8. 仅打包资源包")

    choice = input("\n请选择 (1-8): ").strip()

    target_items = []
    custom_prompt = None

    weapon_items = ["wooden_sword", "stone_sword", "iron_sword", "golden_sword",
                    "diamond_sword", "netherite_sword", "bow", "crossbow", "trident", "arrow"]
    tool_items = ["wooden_pickaxe", "stone_pickaxe", "iron_pickaxe", "golden_pickaxe",
                  "diamond_pickaxe", "netherite_pickaxe", "wooden_axe", "stone_axe",
                  "iron_axe", "golden_axe", "diamond_axe", "netherite_axe"]
    material_items = ["coal", "iron_ingot", "gold_ingot", "diamond", "emerald",
                      "netherite_ingot", "lapis_lazuli"]

    if choice == "1":
        name = input("输入物品名称: ").strip()
        if name in ITEM_PROMPTS:
            target_items = [name]
        else:
            print(f"❌ 未知物品: {name}")
            return
    elif choice == "2":
        target_items = weapon_items
    elif choice == "3":
        target_items = tool_items
    elif choice == "4":
        target_items = material_items
    elif choice == "5":
        target_items = item_names
    elif choice == "6":
        custom_prompt = input("输入自定义提示词: ").strip()
        target_items = ["custom"]
        ITEM_PROMPTS["custom"] = custom_prompt
    elif choice == "7":
        raw_dir = os.path.join(OUTPUT_DIR, "raw")
        sizes_input = input(f"目标尺寸（空格分隔，默认 16 32）: ").strip()
        sizes = [int(s) for s in (sizes_input.split() or ["16", "32"])]
        pixelate_items(raw_dir, sizes)
        return
    elif choice == "8":
        res_input = input("贴图分辨率（默认 16）: ").strip()
        resolution = int(res_input) if res_input else 16
        tex_dir = os.path.join(OUTPUT_DIR, f"{resolution}x{resolution}")
        name_input = input("资源包名称（默认 AI_Texture_Pack）: ").strip()
        pack_name = name_input or "AI_Texture_Pack"
        build_pack(tex_dir, pack_name, resolution)
        return

    if not target_items:
        print("❌ 未选择物品")
        return

    # LoRA 选择
    lora_name = None
    lora_weight = 1.0
    if loras:
        print(f"\n选择 LoRA（留空跳过）:")
        for i, l in enumerate(loras):
            print(f"  {i + 1}. {l}")
        lora_choice = input(f"选择编号 (1-{len(loras)}) 或回车跳过: ").strip()
        if lora_choice.isdigit() and 1 <= int(lora_choice) <= len(loras):
            lora_name = loras[int(lora_choice) - 1]
            w = input(f"LoRA 权重（默认 1.2）: ").strip()
            lora_weight = float(w) if w else 1.2
            print(f"  ✅ 使用 LoRA: {lora_name} (权重 {lora_weight})")

    # 生成参数
    steps = 30
    cfg = 8
    count_per_item = 1

    # 开始生成
    print(f"\n🎨 开始生成 {len(target_items)} 个物品...")
    print("=" * 50)

    raw_dir = os.path.join(OUTPUT_DIR, "raw")
    generated = []

    for i, item_name in enumerate(target_items, 1):
        prompt = ITEM_PROMPTS[item_name]
        print(f"\n[{i}/{len(target_items)}] {item_name}")
        print(f"  提示词: {prompt[:70]}...")

        filepath = generate_item(
            api_base, item_name, prompt,
            lora_name=lora_name, lora_weight=lora_weight,
            output_dir=raw_dir,
        )

        if filepath:
            generated.append(filepath)
            print(f"  ✅ 已保存: {filepath}")
        else:
            print(f"  ❌ 生成失败")

    if not generated:
        print("\n❌ 没有成功生成任何图片")
        return

    # 像素化
    print(f"\n📐 像素化降采样...")
    sizes_input = input("目标尺寸（空格分隔，默认 16 32，回车使用默认）: ").strip()
    sizes = [int(s) for s in (sizes_input.split() or ["16", "32"])]
    pixelate_items(raw_dir, sizes)

    # 打包
    pack_choice = input("\n📦 是否打包为资源包？[Y/n]: ").strip().lower()
    if pack_choice != "n":
        for size in sizes:
            tex_dir = os.path.join(OUTPUT_DIR, f"{size}x{size}")
            pack_name = f"AI_Texture_Pack_{size}x{size}"
            build_pack(tex_dir, pack_name, size)

    print(f"\n🎉 完成！共生成 {len(generated)} 张贴图")
    print(f"📂 原始图片: {raw_dir}")
    for size in sizes:
        print(f"📂 {size}×{size} 贴图: {os.path.join(OUTPUT_DIR, f'{size}x{size}')}")


if __name__ == "__main__":
    main()
