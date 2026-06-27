"""
准备 CNB 训练所需的上传包与远端执行脚本。
"""

from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path

IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".webp"}
BASE_DIR = Path(__file__).resolve().parent
DEFAULT_DATASET_DIR = BASE_DIR / "dataset"
DEFAULT_OUTPUT_DIR = BASE_DIR / "output"
DEFAULT_MODEL_CANDIDATES = [
    BASE_DIR / "stable-diffusion-webui" / "models" / "Stable-diffusion" / "v1-5-pruned-emaonly.safetensors",
    BASE_DIR / "models" / "Stable-diffusion" / "v1-5-pruned-emaonly.safetensors",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="准备 CNB LoRA 训练上传包")
    parser.add_argument("--preset", default="quick", choices=["smoke", "quick", "standard", "quality"])
    parser.add_argument("--dataset-dir", help="训练数据集目录，默认项目下 dataset")
    parser.add_argument("--output-dir", help="输出目录，默认项目下 output")
    parser.add_argument("--model-path", help="基础模型路径，仅用于生成远端命令说明")
    parser.add_argument("--dataset-zip-name", default="cnb_dataset_upload.zip", help="数据集压缩包文件名")
    parser.add_argument("--include-raw", action="store_true", help="打包 dataset/raw")
    return parser.parse_args()


def normalize_path(value: str | None, fallback: Path) -> Path:
    if not value:
        return fallback.resolve()
    path = Path(value).expanduser()
    if not path.is_absolute():
        path = (BASE_DIR / path).resolve()
    return path.resolve()


def choose_model_path(cli_model_path: str | None) -> Path:
    if cli_model_path:
        return normalize_path(cli_model_path, BASE_DIR)
    for candidate in DEFAULT_MODEL_CANDIDATES:
        if candidate.exists():
            return candidate.resolve()
    return DEFAULT_MODEL_CANDIDATES[0].resolve()


def collect_dataset_dirs(dataset_dir: Path, include_raw: bool) -> list[Path]:
    if not dataset_dir.exists():
        raise FileNotFoundError(f"未找到数据集目录: {dataset_dir}")

    results: list[Path] = []
    for child in sorted(dataset_dir.iterdir()):
        if not child.is_dir():
            continue
        if not include_raw and child.name.lower() == "raw":
            continue
        if child.name.startswith("."):
            continue
        results.append(child)

    if not results:
        raise FileNotFoundError(f"数据集目录为空: {dataset_dir}")
    return results


def count_images(folder: Path) -> int:
    return sum(1 for path in folder.rglob("*") if path.is_file() and path.suffix.lower() in IMAGE_EXTS)


def count_captions(folder: Path) -> int:
    return sum(1 for path in folder.rglob("*.txt") if path.is_file())


def build_dataset_zip(dataset_dirs: list[Path], zip_path: Path) -> dict[str, object]:
    zip_path.parent.mkdir(parents=True, exist_ok=True)
    file_count = 0

    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for dataset_subdir in dataset_dirs:
            for file_path in sorted(dataset_subdir.rglob("*")):
                if not file_path.is_file():
                    continue
                archive_name = file_path.relative_to(dataset_subdir.parent).as_posix()
                zf.write(file_path, archive_name)
                file_count += 1

    return {
        "zip_path": zip_path,
        "dataset_dirs": [path.name for path in dataset_dirs],
        "file_count": file_count,
        "image_count": sum(count_images(path) for path in dataset_dirs),
        "caption_count": sum(count_captions(path) for path in dataset_dirs),
    }


def build_remote_script(
    script_path: Path,
    dataset_zip_name: str,
    preset: str,
    model_path: Path,
) -> None:
    remote_model_path = f"/workspace/stable-diffusion-webui/models/Stable-diffusion/{model_path.name}"
    script = f"""#!/usr/bin/env bash
set -euo pipefail

cd /workspace

echo "=== 1. 系统依赖 ==="
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y python3 python3-pip python3-venv git unzip

echo "=== 2. 目录准备 ==="
mkdir -p /workspace/stable-diffusion-webui/models/Stable-diffusion
mkdir -p /workspace/output/lora_output

echo "=== 3. 校验训练资产 ==="
test -f /workspace/{dataset_zip_name}
test -f /workspace/{model_path.name}

echo "=== 4. 部署基础模型 ==="
cp -f /workspace/{model_path.name} {remote_model_path}

echo "=== 5. 解压数据集 ==="
rm -rf /workspace/dataset
mkdir -p /workspace/dataset
unzip -o /workspace/{dataset_zip_name} -d /workspace/dataset

echo "=== 6. 安装 kohya_ss ==="
if [ ! -d /workspace/kohya_ss ]; then
  git clone --depth 1 https://github.com/bmaltais/kohya_ss.git /workspace/kohya_ss
fi
python3 -m pip install --upgrade pip
python3 -m pip install -r /workspace/kohya_ss/requirements_linux.txt

echo "=== 7. 启动训练 ==="
python3 /workspace/train_lora.py \\
  --preset {preset} \\
  --model-path {remote_model_path} \\
  --dataset-dir /workspace/dataset \\
  --output-dir /workspace/output/lora_output \\
  --kohya-dir /workspace/kohya_ss \\
  --action run

echo "=== 8. 训练结果 ==="
ls -lh /workspace/output/lora_output || true
"""
    script_path.write_text(script, encoding="utf-8")


def build_manifest(
    manifest_path: Path,
    preset: str,
    model_path: Path,
    zip_summary: dict[str, object],
    remote_script_name: str,
) -> None:
    manifest = {
        "preset": preset,
        "model_filename": model_path.name,
        "dataset_zip": Path(str(zip_summary["zip_path"])).name,
        "dataset_dirs": zip_summary["dataset_dirs"],
        "image_count": zip_summary["image_count"],
        "caption_count": zip_summary["caption_count"],
        "remote_script": remote_script_name,
        "upload_to_workspace": [
            "train_lora.py",
            "create_train_config.py",
            Path(str(zip_summary["zip_path"])).name,
            model_path.name,
            remote_script_name,
        ],
    }
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")


def main() -> None:
    args = parse_args()
    dataset_dir = normalize_path(args.dataset_dir, DEFAULT_DATASET_DIR)
    output_dir = normalize_path(args.output_dir, DEFAULT_OUTPUT_DIR)
    model_path = choose_model_path(args.model_path)

    dataset_dirs = collect_dataset_dirs(dataset_dir, include_raw=args.include_raw)
    zip_path = output_dir / args.dataset_zip_name
    remote_script_path = output_dir / "cnb_run_training.sh"
    manifest_path = output_dir / "cnb_training_manifest.json"

    zip_summary = build_dataset_zip(dataset_dirs, zip_path)
    build_remote_script(remote_script_path, args.dataset_zip_name, args.preset, model_path)
    build_manifest(manifest_path, args.preset, model_path, zip_summary, remote_script_path.name)

    print(f"[OK] 数据集压缩包: {zip_path}")
    print(f"[OK] 远端执行脚本: {remote_script_path}")
    print(f"[OK] 训练清单: {manifest_path}")
    print(f"     预设: {args.preset}")
    print(f"     数据目录: {', '.join(str(name) for name in zip_summary['dataset_dirs'])}")
    print(f"     图片: {zip_summary['image_count']} | 标签: {zip_summary['caption_count']}")
    print(f"     打包文件数: {zip_summary['file_count']}")
    print(f"     基础模型文件名: {model_path.name}")
    if not model_path.exists():
        print(f"[WARN] 本地未找到基础模型，请手动准备: {model_path}")


if __name__ == "__main__":
    main()
