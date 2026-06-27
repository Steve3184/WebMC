@echo off
setlocal
chcp 65001 >nul

echo ============================================
echo  开始 Minecraft LoRA 训练
echo ============================================
echo.

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

if defined PYTHON_EXE (
    set "PYTHON=%PYTHON_EXE%"
) else (
    set "PYTHON=C:\Users\l\AppData\Local\Programs\Python\Python310\python.exe"
    if not exist "%PYTHON%" set "PYTHON=python"
)

if defined DATASET_DIR (
    set "TRAIN_DATASET_DIR=%DATASET_DIR%"
) else (
    set "TRAIN_DATASET_DIR=%PROJECT_DIR%\dataset"
)

if defined OUTPUT_ROOT (
    set "TRAIN_OUTPUT_ROOT=%OUTPUT_ROOT%"
) else (
    set "TRAIN_OUTPUT_ROOT=%PROJECT_DIR%\output"
)

if defined KOHYA_DIR (
    set "TRAIN_KOHYA_DIR=%KOHYA_DIR%"
) else (
    set "TRAIN_KOHYA_DIR=%PROJECT_DIR%\kohya_ss"
)

if defined MODEL_PATH (
    set "TRAIN_MODEL_PATH=%MODEL_PATH%"
) else (
    set "TRAIN_MODEL_PATH=%PROJECT_DIR%\stable-diffusion-webui\models\Stable-diffusion\v1-5-pruned-emaonly.safetensors"
    if not exist "%TRAIN_MODEL_PATH%" set "TRAIN_MODEL_PATH=%PROJECT_DIR%\models\Stable-diffusion\v1-5-pruned-emaonly.safetensors"
)

if defined TRAIN_PRESET (
    set "PRESET=%TRAIN_PRESET%"
) else (
    set "PRESET=quick"
)

if not exist "%TRAIN_DATASET_DIR%" (
    echo [ERROR] 未找到数据集目录:
    echo   %TRAIN_DATASET_DIR%
    pause
    exit /b 1
)

if not exist "%TRAIN_MODEL_PATH%" (
    echo [ERROR] 未找到基础模型:
    echo   %TRAIN_MODEL_PATH%
    pause
    exit /b 1
)

if not exist "%TRAIN_KOHYA_DIR%\sd-scripts\train_network.py" (
    echo [ERROR] 未找到 Kohya 训练脚本:
    echo   %TRAIN_KOHYA_DIR%\sd-scripts\train_network.py
    pause
    exit /b 1
)

echo [INFO] Python: %PYTHON%
echo [INFO] Preset: %PRESET%
echo [INFO] Dataset: %TRAIN_DATASET_DIR%
echo [INFO] Model: %TRAIN_MODEL_PATH%
echo [INFO] Output root: %TRAIN_OUTPUT_ROOT%
echo [INFO] Kohya: %TRAIN_KOHYA_DIR%
echo.

echo [Check] 验证 Python 环境...
"%PYTHON%" -c "import torch; print('  PyTorch:', torch.__version__, '| CUDA available:', torch.cuda.is_available()); import accelerate, xformers, diffusers, safetensors, bitsandbytes; print('  accelerate/xformers/diffusers/safetensors/bitsandbytes OK')" 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] 缺少训练依赖或 Python 不可用，请先运行 install_all.bat
    pause
    exit /b 1
)
echo.

echo [Step 1/2] 生成训练配置...
"%PYTHON%" "%PROJECT_DIR%\create_train_config.py" --preset "%PRESET%" --dataset-dir "%TRAIN_DATASET_DIR%" --output-root "%TRAIN_OUTPUT_ROOT%" --model-path "%TRAIN_MODEL_PATH%"
if %errorlevel% neq 0 (
    echo [ERROR] 配置生成失败！
    pause
    exit /b 1
)
echo.

echo [Step 2/2] 启动训练...
"%PYTHON%" "%PROJECT_DIR%\train_lora.py" --preset "%PRESET%" --dataset-dir "%TRAIN_DATASET_DIR%" --output-root "%TRAIN_OUTPUT_ROOT%" --model-path "%TRAIN_MODEL_PATH%" --kohya-dir "%TRAIN_KOHYA_DIR%" --action run

echo.
echo ============================================
if %errorlevel% equ 0 (
    echo  Training complete! Check output under:
    echo  %TRAIN_OUTPUT_ROOT%
) else (
    echo  Training interrupted. Check errors above.
)
echo ============================================
pause
endlocal
