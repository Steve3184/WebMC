"""
Minecraft LoRA 训练启动器
支持本地与 CNB 场景下的无交互训练、配置保存与命令打印
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path
from typing import Any

PROJECT_DIR = Path(__file__).resolve().parent
DEFAULT_DATASET_DIR = PROJECT_DIR / "dataset"
DEFAULT_OUTPUT_ROOT = PROJECT_DIR / "output"
DEFAULT_LORA_OUTPUT_DIR = DEFAULT_OUTPUT_ROOT / "lora_output"
DEFAULT_MODEL_DIRS = [
    PROJECT_DIR / "models" / "Stable-diffusion",
    PROJECT_DIR / "stable-diffusion-webui" / "models" / "Stable-diffusion",
]
DEFAULT_KOHYA_DIR = PROJECT_DIR / "kohya_ss"
DEFAULT_TRAIN_SCRIPT = DEFAULT_KOHYA_DIR / "sd-scripts" / "train_network.py"
DEFAULT_PYTHON = Path(r"C:\Users\l\AppData\Local\Programs\Python\Python310\python.exe")

PRESETS: dict[str, dict[str, Any]] = {
    "smoke": {
        "name": "冒烟测试（约 20 steps）",
        "description": "快速验证路径、依赖、数据集和输出链路是否正常",
        "params": {
            "output_subdir": "smoke_test",
            "output_name": "minecraft_item_smoke",
            "save_model_as": "safetensors",
            "resolution": "512,512",
            "enable_bucket": True,
            "min_bucket_reso": 256,
            "max_bucket_reso": 512,
            "caption_extension": ".txt",
            "shuffle_caption": True,
            "max_train_steps": 20,
            "train_batch_size": 1,
            "gradient_accumulation_steps": 1,
            "max_token_length": 150,
            "learning_rate": "1e-4",
            "unet_lr": "1e-4",
            "text_encoder_lr": "5e-5",
            "lr_scheduler": "cosine",
            "lr_warmup_steps": 2,
            "network_module": "networks.lora",
            "network_dim": 16,
            "network_alpha": 8,
            "optimizer_type": "AdamW8bit",
            "mixed_precision": "fp16",
            "save_precision": "fp16",
            "xformers": True,
            "gradient_checkpointing": True,
            "cache_latents": True,
            "save_every_n_epochs": 1,
            "log_prefix": "smoke",
            "seed": 42,
            "clip_skip": 2,
        },
    },
    "quick": {
        "name": "快速测试（约 500 steps）",
        "description": "适合 CNB 或本地先跑一版可用模型",
        "params": {
            "output_subdir": "lora_output",
            "output_name": "minecraft_lora_quick",
            "save_model_as": "safetensors",
            "resolution": "512,512",
            "enable_bucket": True,
            "min_bucket_reso": 256,
            "max_bucket_reso": 512,
            "caption_extension": ".txt",
            "shuffle_caption": True,
            "max_train_steps": 500,
            "train_batch_size": 1,
            "gradient_accumulation_steps": 1,
            "max_token_length": 150,
            "learning_rate": "3e-4",
            "unet_lr": "3e-4",
            "text_encoder_lr": "1e-4",
            "lr_scheduler": "cosine",
            "lr_warmup_steps": 50,
            "network_module": "networks.lora",
            "network_dim": 32,
            "network_alpha": 16,
            "optimizer_type": "AdamW8bit",
            "mixed_precision": "fp16",
            "save_precision": "fp16",
            "xformers": True,
            "gradient_checkpointing": True,
            "cache_latents": True,
            "save_every_n_epochs": 1,
            "log_prefix": "quick",
            "seed": 42,
            "clip_skip": 2,
        },
    },
    "standard": {
        "name": "标准训练（约 5000 steps）",
        "description": "适合本地正式训练，CNB 上建议缩短步数后再用",
        "params": {
            "output_subdir": "lora_output",
            "output_name": "minecraft_lora_standard",
            "save_model_as": "safetensors",
            "resolution": "512,512",
            "enable_bucket": True,
            "min_bucket_reso": 256,
            "max_bucket_reso": 512,
            "caption_extension": ".txt",
            "shuffle_caption": True,
            "max_train_steps": 5000,
            "train_batch_size": 2,
            "gradient_accumulation_steps": 2,
            "max_token_length": 150,
            "learning_rate": "1e-4",
            "unet_lr": "1e-4",
            "text_encoder_lr": "5e-5",
            "lr_scheduler": "cosine_with_restarts",
            "lr_warmup_steps": 200,
            "lr_scheduler_num_cycles": 2,
            "network_module": "networks.lora",
            "network_dim": 64,
            "network_alpha": 32,
            "optimizer_type": "AdamW8bit",
            "mixed_precision": "fp16",
            "save_precision": "fp16",
            "xformers": True,
            "gradient_checkpointing": True,
            "cache_latents": True,
            "save_every_n_epochs": 2,
            "log_prefix": "standard",
            "seed": 42,
            "clip_skip": 2,
            "noise_offset": 0.0357,
        },
    },
    "quality": {
        "name": "高质量训练（约 15000 steps）",
        "description": "长时训练预设，建议只在本地或更稳定的 GPU 环境使用",
        "params": {
            "output_subdir": "lora_output",
            "output_name": "minecraft_lora_quality",
            "save_model_as": "safetensors",
            "resolution": "512,512",
            "enable_bucket": True,
            "min_bucket_reso": 256,
            "max_bucket_reso": 512,
            "caption_extension": ".txt",
            "shuffle_caption": True,
            "max_train_steps": 15000,
            "train_batch_size": 2,
            "gradient_accumulation_steps": 4,
            "max_token_length": 150,
            "learning_rate": "5e-5",
            "unet_lr": "5e-5",
            "text_encoder_lr": "2e-5",
            "lr_scheduler": "cosine_with_restarts",
            "lr_warmup_steps": 500,
            "lr_scheduler_num_cycles": 3,
            "network_module": "networks.lora",
            "network_dim": 128,
            "network_alpha": 64,
            "optimizer_type": "AdamW8bit",
            "optimizer_args": ["weight_decay=0.01"],
            "mixed_precision": "fp16",
            "save_precision": "fp16",
            "xformers": True,
            "gradient_checkpointing": True,
            "cache_latents": True,
            "save_every_n_epochs": 3,
            "log_prefix": "quality",
            "seed": 42,
            "clip_skip": 2,
            "noise_offset": 0.0357,
        },
    },
}


def resolve_python_executable() -> Path:
    current = Path(sys.executable)
    if current.exists():
        return current
    if DEFAULT_PYTHON.exists():
        return DEFAULT_PYTHON
    return Path("python")


def list_dataset_subdirs(dataset_dir: Path) -> list[dict[str, Any]]:
    if not dataset_dir.exists():
        return []

    rows: list[dict[str, Any]] = []
    for subdir in sorted(dataset_dir.iterdir()):
        if not subdir.is_dir():
            continue
        images = [p for p in subdir.iterdir() if p.suffix.lower() in {".png", ".jpg", ".jpeg", ".webp"}]
        captions = [p for p in subdir.iterdir() if p.suffix.lower() == ".txt"]
        rows.append(
            {
                "name": subdir.name,
                "image_count": len(images),
                "caption_count": len(captions),
            }
        )
    return rows


def print_dataset_summary(dataset_dir: Path) -> None:
    print("\n数据集检查:")
    rows = list_dataset_subdirs(dataset_dir)
    if not rows:
        print(f"  未找到可用数据集目录: {dataset_dir}")
        return

    total_images = 0
    total_captions = 0
    for row in rows:
        total_images += row["image_count"]
        total_captions += row["caption_count"]
        print(f"  {row['name']}")
        print(f"     图片: {row['image_count']} | 标签: {row['caption_count']}")
        if row["image_count"] > 0 and row["caption_count"] == 0:
            print("     警告: 有图片但没有标签，请先生成标签")
    print(f"  总计: {total_images} 张图 / {total_captions} 个标签")


def find_sd_models() -> list[Path]:
    models: list[Path] = []
    for model_dir in DEFAULT_MODEL_DIRS:
        if not model_dir.exists():
            continue
        for model in sorted(model_dir.iterdir()):
            if model.is_file() and model.suffix.lower() in {".safetensors", ".ckpt", ".pt"}:
                models.append(model)
    return models


def print_model_list(models: list[Path]) -> None:
    print(f"\n可用 SD 基础模型: {len(models)}")
    if not models:
        print("  未找到模型，请使用 --model-path 指定基础模型")
        return
    for idx, model in enumerate(models, start=1):
        size_mb = model.stat().st_size / 1024 / 1024
        print(f"  {idx}. {model} ({size_mb:.0f} MB)")


def choose_model(models: list[Path], model_index: int | None, model_path: str | None) -> Path:
    if model_path:
        chosen = Path(model_path).expanduser()
        if not chosen.is_absolute():
            chosen = (PROJECT_DIR / chosen).resolve()
        if not chosen.exists():
            raise FileNotFoundError(f"模型不存在: {chosen}")
        return chosen

    if model_index is not None:
        if model_index < 1 or model_index > len(models):
            raise ValueError(f"模型编号超出范围: {model_index}")
        return models[model_index - 1]

    if len(models) == 1:
        return models[0]
    if not models:
        raise FileNotFoundError("未找到任何基础模型，请传入 --model-path")

    if not sys.stdin.isatty():
        raise ValueError("存在多个基础模型，请用 --model-index 或 --model-path 指定")

    while True:
        raw = input(f"选择模型编号 (1-{len(models)}): ").strip()
        if raw.isdigit() and 1 <= int(raw) <= len(models):
            return models[int(raw) - 1]
        print("输入无效，请重新输入")


def choose_preset(preset_name: str | None) -> str:
    if preset_name and preset_name in PRESETS:
        return preset_name

    if not sys.stdin.isatty():
        return "standard"

    print("\n选择训练配置:")
    for key, config in PRESETS.items():
        print(f"  {key}: {config['name']}")
        print(f"      {config['description']}")
    raw = input("\n选择配置 (smoke/quick/standard/quality): ").strip().lower()
    if raw in PRESETS:
        return raw
    print("  默认使用: standard")
    return "standard"


def choose_action(cli_action: str | None) -> str:
    if cli_action:
        return cli_action
    if not sys.stdin.isatty():
        return "print"

    print("\n选择启动方式:")
    print("  1. 打印训练命令")
    print("  2. 仅保存配置")
    print("  3. 直接启动训练")
    print("  4. 启动 Kohya GUI")
    raw = input("\n选择 (1-4): ").strip()
    mapping = {"1": "print", "2": "save", "3": "run", "4": "gui"}
    return mapping.get(raw, "print")


def normalize_path(value: str | None, fallback: Path) -> Path:
    if not value:
        return fallback.resolve()
    path = Path(value).expanduser()
    if not path.is_absolute():
        path = (PROJECT_DIR / path).resolve()
    return path.resolve()


def build_params(
    preset_name: str,
    model_path: Path,
    dataset_dir: Path,
    output_root: Path,
    output_dir_override: str | None,
    logging_dir_override: str | None,
    output_name_override: str | None,
    max_train_steps_override: int | None,
) -> dict[str, Any]:
    preset = PRESETS[preset_name]
    params = dict(preset["params"])

    output_root = output_root.resolve()
    default_output_dir = output_root / params.pop("output_subdir")
    output_dir = normalize_path(output_dir_override, default_output_dir)
    logging_dir = normalize_path(logging_dir_override, output_dir / "logs")

    output_dir.mkdir(parents=True, exist_ok=True)
    logging_dir.mkdir(parents=True, exist_ok=True)

    params["pretrained_model_name_or_path"] = model_path.resolve()
    params["train_data_dir"] = dataset_dir.resolve()
    params["output_dir"] = output_dir
    params["logging_dir"] = logging_dir

    if output_name_override:
        params["output_name"] = output_name_override
    if max_train_steps_override is not None:
        params["max_train_steps"] = max_train_steps_override
        params["lr_warmup_steps"] = max(1, min(params["lr_warmup_steps"], max_train_steps_override // 10 or 1))

    return params


def clone_params_for_save(params: dict[str, Any]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in params.items():
        if isinstance(value, Path):
            result[key] = value.as_posix()
        else:
            result[key] = value
    return result


def save_config(preset_name: str, params: dict[str, Any]) -> Path:
    config_path = params["output_dir"] / f"config_{preset_name}.json"
    with config_path.open("w", encoding="utf-8") as f:
        json.dump(clone_params_for_save(params), f, indent=2, ensure_ascii=False)
    return config_path


def build_command(params: dict[str, Any], kohya_dir: Path, train_script: Path) -> list[str]:
    command = [
        str(resolve_python_executable()),
        "-m",
        "accelerate.commands.launch",
        "--num_cpu_threads_per_process=4",
        str(train_script),
        f"--pretrained_model_name_or_path={params['pretrained_model_name_or_path']}",
        f"--train_data_dir={params['train_data_dir']}",
        f"--output_dir={params['output_dir']}",
        f"--output_name={params['output_name']}",
        f"--save_model_as={params['save_model_as']}",
        f"--resolution={params['resolution']}",
        f"--min_bucket_reso={params['min_bucket_reso']}",
        f"--max_bucket_reso={params['max_bucket_reso']}",
        f"--caption_extension={params['caption_extension']}",
        f"--train_batch_size={params['train_batch_size']}",
        f"--gradient_accumulation_steps={params['gradient_accumulation_steps']}",
        f"--max_train_steps={params['max_train_steps']}",
        f"--learning_rate={params['learning_rate']}",
        f"--unet_lr={params['unet_lr']}",
        f"--text_encoder_lr={params['text_encoder_lr']}",
        f"--lr_scheduler={params['lr_scheduler']}",
        f"--lr_warmup_steps={params['lr_warmup_steps']}",
        f"--network_module={params['network_module']}",
        f"--network_dim={params['network_dim']}",
        f"--network_alpha={params['network_alpha']}",
        f"--optimizer_type={params['optimizer_type']}",
        f"--mixed_precision={params['mixed_precision']}",
        f"--save_precision={params['save_precision']}",
        f"--logging_dir={params['logging_dir']}",
        f"--log_prefix={params['log_prefix']}",
        f"--seed={params['seed']}",
        f"--clip_skip={params['clip_skip']}",
        f"--max_token_length={params['max_token_length']}",
    ]

    if params.get("enable_bucket"):
        command.append("--enable_bucket")
    if params.get("shuffle_caption"):
        command.append("--shuffle_caption")
    if params.get("xformers"):
        command.append("--xformers")
    if params.get("gradient_checkpointing"):
        command.append("--gradient_checkpointing")
    if params.get("cache_latents"):
        command.append("--cache_latents")
    if params.get("save_every_n_epochs"):
        command.append(f"--save_every_n_epochs={params['save_every_n_epochs']}")
    if params.get("lr_scheduler_num_cycles"):
        command.append(f"--lr_scheduler_num_cycles={params['lr_scheduler_num_cycles']}")
    if params.get("noise_offset") is not None:
        command.append(f"--noise_offset={params['noise_offset']}")
    if params.get("optimizer_args"):
        for item in params["optimizer_args"]:
            command.extend(["--optimizer_args", str(item)])

    return command


def print_command(command: list[str]) -> None:
    print("\n训练命令:")
    print("=" * 50)
    print(subprocess.list2cmdline(command))


def validate_environment(dataset_dir: Path, model_path: Path, kohya_dir: Path, train_script: Path) -> None:
    if not dataset_dir.exists():
        raise FileNotFoundError(f"未找到数据集目录: {dataset_dir}")
    if not model_path.exists():
        raise FileNotFoundError(f"未找到基础模型: {model_path}")
    if not kohya_dir.exists():
        raise FileNotFoundError(f"未找到 kohya_ss 目录: {kohya_dir}")
    if not train_script.exists():
        raise FileNotFoundError(f"未找到训练脚本: {train_script}")


def print_summary(
    preset_name: str,
    dataset_dir: Path,
    model_path: Path,
    config_path: Path,
    params: dict[str, Any],
    kohya_dir: Path,
    train_script: Path,
) -> None:
    print(f"\n预设: {preset_name} - {PRESETS[preset_name]['name']}")
    print(f"Python: {resolve_python_executable()}")
    print(f"Kohya 目录: {kohya_dir}")
    print(f"训练脚本: {train_script}")
    print(f"模型: {model_path}")
    print(f"数据集: {dataset_dir}")
    print(f"输出目录: {params['output_dir']}")
    print(f"日志目录: {params['logging_dir']}")
    print(f"配置文件: {config_path}")
    print(f"最大步数: {params['max_train_steps']}")
    print(f"batch_size: {params['train_batch_size']} | grad_accum: {params['gradient_accumulation_steps']}")


def run_training(command: list[str], kohya_dir: Path) -> int:
    print("\n开始启动训练...\n")
    result = subprocess.run(command, cwd=kohya_dir)
    return result.returncode


def start_kohya_gui(kohya_dir: Path) -> int:
    gui_bat = kohya_dir / "gui.bat"
    if not gui_bat.exists():
        raise FileNotFoundError(f"未找到 gui.bat: {gui_bat}")
    print(f"\n启动 GUI: {gui_bat}")
    print("浏览器地址通常为 http://localhost:7860")
    result = subprocess.run(str(gui_bat), cwd=kohya_dir, shell=True)
    return result.returncode


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Minecraft LoRA 训练启动器")
    parser.add_argument("--preset", choices=list(PRESETS.keys()), help="训练预设")
    parser.add_argument("--model-index", type=int, help="模型编号，从 1 开始")
    parser.add_argument("--model-path", help="手动指定基础模型路径")
    parser.add_argument("--dataset-dir", help="训练数据集目录，默认使用项目下 dataset")
    parser.add_argument("--output-root", help="输出根目录，默认使用项目下 output")
    parser.add_argument("--output-dir", help="训练产物目录，优先级高于 output-root")
    parser.add_argument("--logging-dir", help="日志目录，默认在输出目录下 logs")
    parser.add_argument("--output-name", help="覆盖预设里的 output_name")
    parser.add_argument("--max-train-steps", type=int, help="覆盖预设里的最大训练步数")
    parser.add_argument("--kohya-dir", help="kohya_ss 目录，默认使用项目下 kohya_ss")
    parser.add_argument("--action", choices=["print", "save", "run", "gui"], help="执行动作")
    parser.add_argument("--list-models", action="store_true", help="仅列出可用模型")
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    dataset_dir = normalize_path(args.dataset_dir, DEFAULT_DATASET_DIR)
    output_root = normalize_path(args.output_root, DEFAULT_OUTPUT_ROOT)
    kohya_dir = normalize_path(args.kohya_dir, DEFAULT_KOHYA_DIR)
    train_script = kohya_dir / "sd-scripts" / "train_network.py"

    print()
    print("╔══════════════════════════════════════════════════════════╗")
    print("║     Minecraft LoRA 训练启动器                          ║")
    print("╚══════════════════════════════════════════════════════════╝")
    print(f"项目目录: {PROJECT_DIR}")

    print_dataset_summary(dataset_dir)
    models = find_sd_models()
    print_model_list(models)

    if args.list_models:
        return 0

    preset_name = choose_preset(args.preset)
    model_path = choose_model(models, args.model_index, args.model_path)
    validate_environment(dataset_dir, model_path, kohya_dir, train_script)

    params = build_params(
        preset_name=preset_name,
        model_path=model_path,
        dataset_dir=dataset_dir,
        output_root=output_root,
        output_dir_override=args.output_dir,
        logging_dir_override=args.logging_dir,
        output_name_override=args.output_name,
        max_train_steps_override=args.max_train_steps,
    )
    config_path = save_config(preset_name, params)
    print_summary(preset_name, dataset_dir, model_path, config_path, params, kohya_dir, train_script)

    action = choose_action(args.action)
    if action == "gui":
        return start_kohya_gui(kohya_dir)

    command = build_command(params, kohya_dir, train_script)
    if action == "print":
        print_command(command)
        return 0
    if action == "save":
        print("\n配置已保存，可稍后再运行训练。")
        return 0
    if action == "run":
        print_command(command)
        return run_training(command, kohya_dir)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
