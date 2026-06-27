# CNB 远端训练工作记录

## 目的
把当前项目的 CNB 训练流程固定下来，避免后续继续混用旧参数、旧文档和半自动脚本。

## 当前统一入口
本地准备：

```bash
python prepare_cnb_training.py --preset quick
```

CNB 执行：

```bash
cd /workspace
bash cnb_run_training.sh
```

## 这次收敛了什么
- 新增 `prepare_cnb_training.py`，负责生成数据集压缩包、远端执行脚本、训练清单。
- 更新 `setup_cnb.sh`，改成 Linux 环境可直接执行的 setup 版本。
- 修正文档中的旧参数。
  旧写法里有 `--model`、`--dataset`、`--output`。
  当前实际支持的是 `--model-path`、`--dataset-dir`、`--output-dir`、`--kohya-dir`。

## 上传清单
CNB `/workspace` 里至少要有：

- `train_lora.py`
- `create_train_config.py`
- `cnb_dataset_upload.zip`
- `cnb_run_training.sh`
- `v1-5-pruned-emaonly.safetensors`

## 约束
- 不把大模型、数据集压缩包提交进 Git。
- CNB 只推荐跑 `smoke` 和 `quick`。
- 如果后面要自动化浏览器上传或下载，优先在这个文档继续补，不要再分散到临时脚本里。
