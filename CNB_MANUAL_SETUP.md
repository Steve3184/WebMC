# CNB 工作区手动执行指南

## 工作区地址
https://cnb-aeg-1jo5qp9a0-001.cnb.space/?folder=/workspace

## 推荐方式
优先在本地先执行：

```bash
python prepare_cnb_training.py --preset quick
```

然后把这些文件上传到 CNB 的 `/workspace`：

- `train_lora.py`
- `create_train_config.py`
- `output/cnb_dataset_upload.zip`
- `output/cnb_run_training.sh`
- `v1-5-pruned-emaonly.safetensors`

## 执行步骤
### 1. 打开 Terminal
在 CNB 工作区中打开终端。

### 2. 执行训练脚本
```bash
cd /workspace
bash cnb_run_training.sh
```

### 3. 如果只想手工执行
```bash
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y python3 python3-pip python3-venv git unzip
mkdir -p /workspace/stable-diffusion-webui/models/Stable-diffusion
mkdir -p /workspace/output/lora_output
cp -f /workspace/v1-5-pruned-emaonly.safetensors /workspace/stable-diffusion-webui/models/Stable-diffusion/
rm -rf /workspace/dataset
mkdir -p /workspace/dataset
unzip -o /workspace/cnb_dataset_upload.zip -d /workspace/dataset
git clone --depth 1 https://github.com/bmaltais/kohya_ss.git /workspace/kohya_ss || true
python3 -m pip install --upgrade pip
python3 -m pip install -r /workspace/kohya_ss/requirements_linux.txt
python3 /workspace/train_lora.py --preset quick --model-path /workspace/stable-diffusion-webui/models/Stable-diffusion/v1-5-pruned-emaonly.safetensors --dataset-dir /workspace/dataset --output-dir /workspace/output/lora_output --kohya-dir /workspace/kohya_ss --action run
```

## 结果检查
```bash
ls -lh /workspace/output/lora_output
find /workspace/output/lora_output -name "*.safetensors"
```

## 故障排查
### `unzip` 不存在
```bash
apt-get update && apt-get install -y unzip
```

### `pip` 不存在
```bash
python3 -m ensurepip --upgrade || true
python3 -m pip install --upgrade pip
```

### 模型路径不对
```bash
find /workspace -name "*.safetensors" 2>/dev/null
```
