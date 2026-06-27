# CNB 训练说明

## 目标
把本地数据集和基础模型上传到 CNB，执行短时 LoRA 训练，再把产物拉回本地。

推荐顺序：

1. `smoke`：20 steps，验证链路
2. `quick`：500 steps，作为 CNB 主训练方案

不建议直接在 CNB 上跑 `standard` / `quality`。

## 当前可用流程
本仓库现在统一使用下面这套入口：

1. 本地生成上传包
```bash
python prepare_cnb_training.py --preset quick
```

这会生成：

- `output/cnb_dataset_upload.zip`
- `output/cnb_run_training.sh`
- `output/cnb_training_manifest.json`

2. 上传到 CNB 工作区根目录 `/workspace`

需要上传这些文件：

- `train_lora.py`
- `create_train_config.py`
- `output/cnb_dataset_upload.zip`
- 基础模型文件，例如 `v1-5-pruned-emaonly.safetensors`
- `output/cnb_run_training.sh`

3. 在 CNB Terminal 执行
```bash
cd /workspace
bash cnb_run_training.sh
```

## CNB 目录约定
- `/workspace/dataset`
- `/workspace/kohya_ss`
- `/workspace/stable-diffusion-webui/models/Stable-diffusion`
- `/workspace/output/lora_output`

## 直接命令
如果你不想走脚本，也可以直接运行：

```bash
python3 /workspace/train_lora.py \
  --preset quick \
  --model-path /workspace/stable-diffusion-webui/models/Stable-diffusion/v1-5-pruned-emaonly.safetensors \
  --dataset-dir /workspace/dataset \
  --output-dir /workspace/output/lora_output \
  --kohya-dir /workspace/kohya_ss \
  --action run
```

## 回传本地
训练完成后，从 CNB 下载这些内容：

- `/workspace/output/lora_output/*.safetensors`
- `/workspace/output/lora_output/config_*.json`
- `/workspace/output/lora_output/logs/`

本地建议落盘：

- LoRA 模型放到 `stable-diffusion-webui/models/Lora/`
- CNB 训练归档放到 `output/cnb_downloads/`
