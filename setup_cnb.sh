#!/usr/bin/env bash
set -euo pipefail

cd /workspace

echo "=== 1. 系统依赖 ==="
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y python3 python3-pip python3-venv git unzip

echo "=== 2. 创建目录 ==="
mkdir -p /workspace/stable-diffusion-webui/models/Stable-diffusion
mkdir -p /workspace/output/lora_output
mkdir -p /workspace/dataset

echo "=== 3. 部署基础模型 ==="
if [ -f /workspace/v1-5-pruned-emaonly.safetensors ]; then
  cp -f /workspace/v1-5-pruned-emaonly.safetensors \
    /workspace/stable-diffusion-webui/models/Stable-diffusion/
  echo "模型已就位"
else
  echo "未找到 /workspace/v1-5-pruned-emaonly.safetensors"
fi

echo "=== 4. 解压数据集 ==="
if [ -f /workspace/cnb_dataset_upload.zip ]; then
  rm -rf /workspace/dataset
  mkdir -p /workspace/dataset
  unzip -o /workspace/cnb_dataset_upload.zip -d /workspace/dataset
else
  echo "未找到 /workspace/cnb_dataset_upload.zip"
fi

echo "=== 5. 安装 kohya_ss ==="
if [ ! -d /workspace/kohya_ss ]; then
  git clone --depth 1 https://github.com/bmaltais/kohya_ss.git /workspace/kohya_ss
fi
python3 -m pip install --upgrade pip
python3 -m pip install -r /workspace/kohya_ss/requirements_linux.txt

echo "=== 6. 结果检查 ==="
ls -lh /workspace/stable-diffusion-webui/models/Stable-diffusion/ || true
find /workspace/dataset -maxdepth 2 -type f | head -20 || true

echo "=== SETUP DONE ==="
