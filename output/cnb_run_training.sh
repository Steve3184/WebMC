#!/usr/bin/env bash
set -euo pipefail

cd /workspace

echo "=== 1. 系统依赖 ==="
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y python3 python3-pip python3-venv git unzip

echo "=== 2. 目录准备 ==="
mkdir -p /workspace/stable-diffusion-webui/models/Stable-diffusion
mkdir -p /workspace/output/lora_output

echo "=== 3. 校验训练资产 ==="
test -f /workspace/cnb_dataset_upload.zip
test -f /workspace/v1-5-pruned-emaonly.safetensors

echo "=== 4. 部署基础模型 ==="
cp -f /workspace/v1-5-pruned-emaonly.safetensors /workspace/stable-diffusion-webui/models/Stable-diffusion/v1-5-pruned-emaonly.safetensors

echo "=== 5. 解压数据集 ==="
rm -rf /workspace/dataset
mkdir -p /workspace/dataset
unzip -o /workspace/cnb_dataset_upload.zip -d /workspace/dataset

echo "=== 6. 安装 kohya_ss ==="
if [ ! -d /workspace/kohya_ss ]; then
  git clone --depth 1 https://github.com/bmaltais/kohya_ss.git /workspace/kohya_ss
fi
python3 -m pip install --upgrade pip
python3 -m pip install -r /workspace/kohya_ss/requirements_linux.txt

echo "=== 7. 启动训练 ==="
python3 /workspace/train_lora.py \
  --preset quick \
  --model-path /workspace/stable-diffusion-webui/models/Stable-diffusion/v1-5-pruned-emaonly.safetensors \
  --dataset-dir /workspace/dataset \
  --output-dir /workspace/output/lora_output \
  --kohya-dir /workspace/kohya_ss \
  --action run

echo "=== 8. 训练结果 ==="
ls -lh /workspace/output/lora_output || true
