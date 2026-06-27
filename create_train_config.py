"""
Kohya_ss LoRA 训练配置生成脚本
支持本地和 CNB 场景下按预设生成训练配置
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
DEFAULT_DATASET_DIR = BASE_DIR / "dataset"
DEFAULT_OUTPUT_ROOT = BASE_DIR / "output"
DEFAULT_MODEL_DIRS = [
    BASE_DIR / "models" / "Stable-diffusion",
    BASE_DIR / "stable-diffusion-webui" / "models" / "Stable-diffusion",
]

PRESETS = {
    "smoke": {
        "output_subdir": "smoke_test",
        "output_name": "minecraft_item_smoke",
        "max_train_steps": 20,
        "train_batch_size": 1,
        "gradient_accumulation_steps": 1,
        "learning_rate": "1e-4",
        "unet_lr": "1e-4",
        "text_encoder_lr": "5e-5",
        "lr_scheduler": "cosine",
        "lr_warmup_steps": 2,
        "network_dim": 16,
        "network_alpha": 8,
        "save_every_n_epochs": 1,
        "log_prefix": "smoke",
    },
    "quick": {
        "output_subdir": "lora_output",
        "output_name": "minecraft_lora_quick",
        "max_train_steps": 500,
        "train_batch_size": 1,
        "gradient_accumulation_steps": 1,
        "learning_rate": "3e-4",
        "unet_lr": "3e-4",
        "text_encoder_lr": "1e-4",
        "lr_scheduler": "cosine",
        "lr_warmup_steps": 50,
        "network_dim": 32,
        "network_alpha": 16,
        "save_every_n_epochs": 1,
        "log_prefix": "quick",
    },
    "standard": {
        "output_subdir": "lora_output",
        "output_name": "minecraft_lora_standard",
        "max_train_steps": 5000,
        "train_batch_size": 2,
        "gradient_accumulation_steps": 2,
        "learning_rate": "2e-4",
        "unet_lr": "2e-4",
        "text_encoder_lr": "5e-5",
        "lr_scheduler": "cosine_with_restarts",
        "lr_warmup_steps": 200,
        "lr_scheduler_num_cycles": 2,
        "network_dim": 64,
        "network_alpha": 32,
        "save_every_n_epochs": 2,
        "log_prefix": "standard",
        "noise_offset": 0.0357,
    },
    "quality": {
        "output_subdir": "lora_output",
        "output_name": "minecraft_lora_quality",
        "max_train_steps": 15000,
        "train_batch_size": 2,
        "gradient_accumulation_steps": 4,
        "learning_rate": "5e-5",
        "unet_lr": "5e-5",
        "text_encoder_lr": "2e-5",
        "lr_scheduler": "cosine_with_restarts",
        "lr_warmup_steps": 500,
        "lr_scheduler_num_cycles": 3,
        "network_dim": 128,
        "network_alpha": 64,
        "save_every_n_epochs": 3,
        "log_prefix": "quality",
        "noise_offset": 0.0357,
        "optimizer_args": ["weight_decay=0.01"],
    },
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="生成 Minecraft LoRA 训练配置")
    parser.add_argument("--preset", choices=list(PRESETS.keys()), default="standard")
    parser.add_argument("--dataset-dir", help="数据集目录")
    parser.add_argument("--output-root", help="输出根目录，默认 output")
    parser.add_argument("--output-dir", help="训练产物目录，优先级高于 output-root")
    parser.add_argument("--model-path", help="基础模型路径")
    parser.add_argument("--output-name", help="覆盖输出模型名")
    parser.add_argument("--max-train-steps", type=int, help="覆盖训练步数")
    return parser.parse_args()


def normalize_path(value: str | None, fallback: Path) -> Path:
    if not value:
        return fallback.resolve()
    path = Path(value).expanduser()
    if not path.is_absolute():
        path = (BASE_DIR / path).resolve()
    return path.resolve()


def count_training_images(dataset_dir: Path) -> int:
    exts = {".png", ".jpg", ".jpeg", ".webp"}
    return sum(1 for p in dataset_dir.rglob("*") if p.is_file() and p.suffix.lower() in exts)


def auto_model_path() -> Path | None:
    for model_dir in DEFAULT_MODEL_DIRS:
        if not model_dir.exists():
            continue
        for candidate in sorted(model_dir.iterdir()):
            if candidate.is_file() and candidate.suffix.lower() in {".safetensors", ".ckpt", ".pt"}:
                return candidate.resolve()
    return None


def as_posix(path: Path) -> str:
    return path.resolve().as_posix()


def build_config(args: argparse.Namespace) -> tuple[dict, Path, int, Path]:
    preset = dict(PRESETS[args.preset])
    dataset_dir = normalize_path(args.dataset_dir, DEFAULT_DATASET_DIR)
    output_root = normalize_path(args.output_root, DEFAULT_OUTPUT_ROOT)
    output_dir = normalize_path(args.output_dir, output_root / preset.pop("output_subdir"))

    model_path = normalize_path(args.model_path, auto_model_path() or (BASE_DIR / "stable-diffusion-webui" / "models" / "Stable-diffusion" / "v1-5-pruned-emaonly.safetensors"))

    png_count = count_training_images(dataset_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    logging_dir = output_dir / "logs"
    logging_dir.mkdir(parents=True, exist_ok=True)

    max_train_steps = args.max_train_steps if args.max_train_steps is not None else preset["max_train_steps"]
    lr_warmup_steps = min(preset["lr_warmup_steps"], max(1, max_train_steps // 10))

    config = {
        "pretrained_model_name_or_path": as_posix(model_path),
        "train_data_dir": as_posix(dataset_dir),
        "output_dir": as_posix(output_dir),
        "output_name": args.output_name or preset["output_name"],
        "save_model_as": "safetensors",
        "resolution": "512,512",
        "enable_bucket": True,
        "min_bucket_reso": 256,
        "max_bucket_reso": 512,
        "caption_extension": ".txt",
        "shuffle_caption": True,
        "max_train_steps": max_train_steps,
        "train_batch_size": preset["train_batch_size"],
        "gradient_accumulation_steps": preset["gradient_accumulation_steps"],
        "max_token_length": 150,
        "learning_rate": preset["learning_rate"],
        "unet_lr": preset["unet_lr"],
        "text_encoder_lr": preset["text_encoder_lr"],
        "lr_scheduler": preset["lr_scheduler"],
        "lr_warmup_steps": lr_warmup_steps,
        "network_module": "networks.lora",
        "network_dim": preset["network_dim"],
        "network_alpha": preset["network_alpha"],
        "optimizer_type": "AdamW8bit",
        "mixed_precision": "fp16",
        "save_precision": "fp16",
        "xformers": True,
        "gradient_checkpointing": True,
        "cache_latents": True,
        "cache_latents_to_disk": False,
        "save_every_n_epochs": preset["save_every_n_epochs"],
        "save_last_n_epochs": 5,
        "logging_dir": as_posix(logging_dir),
        "log_prefix": preset["log_prefix"],
        "seed": 42,
        "clip_skip": 2,
        "max_grad_norm": 1.0,
    }

    if "lr_scheduler_num_cycles" in preset:
        config["lr_scheduler_num_cycles"] = preset["lr_scheduler_num_cycles"]
    if "noise_offset" in preset:
        config["noise_offset"] = preset["noise_offset"]
    if "optimizer_args" in preset:
        config["optimizer_args"] = preset["optimizer_args"]

    return config, output_dir, png_count, model_path


def main() -> None:
    args = parse_args()
    config, output_dir, png_count, model_path = build_config(args)

    config_path = output_dir / f"config_{args.preset}.json"
    with config_path.open("w", encoding="utf-8") as f:
        json.dump(config, f, indent=2, ensure_ascii=False)

    alias_path = output_dir / "minecraft_lora_config.json"
    with alias_path.open("w", encoding="utf-8") as f:
        json.dump(config, f, indent=2, ensure_ascii=False)

    print(f"[OK] 训练配置已生成: {config_path}")
    print(f"[OK] 兼容配置已生成: {alias_path}")
    print(f"     预设: {args.preset}")
    print(f"     模型: {model_path}")
    print(f"     数据集: {png_count} 张图像")
    print(f"     输出目录: {output_dir}")
    print(f"     总步数: {config['max_train_steps']}")
    if not model_path.exists():
        print(f"[WARN] 未找到基础模型: {model_path}")


if __name__ == "__main__":
    main()
