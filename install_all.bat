@echo off
setlocal
chcp 65001 >nul

echo ============================================
echo  Minecraft LoRA 训练环境一键安装
echo  支持本地 Windows 与 CNB Linux 场景
echo ============================================
echo.

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"
set "PYTHON_EXE=C:\Users\l\AppData\Local\Programs\Python\Python310\python.exe"
if not exist "%PYTHON_EXE%" set "PYTHON_EXE=python"
set "KOHYA=%PROJECT_DIR%\kohya_ss"

echo [INFO] 使用 Python: %PYTHON_EXE%
"%PYTHON_EXE%" --version
echo.

echo [Step 1/5] 升级 pip...
"%PYTHON_EXE%" -m pip install --upgrade pip setuptools wheel
if %errorlevel% neq 0 (
    echo [ERROR] pip 升级失败
    pause
    exit /b 1
)
echo.

echo [Step 2/5] 安装 PyTorch CUDA 依赖...
"%PYTHON_EXE%" -m pip install torch torchvision --index-url https://download.pytorch.org/whl/cu128
if %errorlevel% neq 0 (
    echo [WARN] PyTorch CUDA 安装失败，继续检查现有环境...
)
echo.

echo [Step 3/5] 安装训练核心依赖...
"%PYTHON_EXE%" -m pip install xformers --extra-index-url https://download.pytorch.org/whl/cu128
"%PYTHON_EXE%" -m pip install accelerate diffusers transformers safetensors tensorboard voluptuous "bitsandbytes>=0.45.0"
if %errorlevel% neq 0 (
    echo [ERROR] 核心依赖安装失败
    pause
    exit /b 1
)
echo.

echo [Step 4/5] 安装 Kohya 依赖...
if exist "%KOHYA%\requirements_windows.txt" (
    "%PYTHON_EXE%" -m pip install -r "%KOHYA%\requirements_windows.txt"
) else if exist "%KOHYA%\requirements_linux.txt" (
    "%PYTHON_EXE%" -m pip install -r "%KOHYA%\requirements_linux.txt"
) else (
    echo [WARN] 未找到 requirements_windows.txt 或 requirements_linux.txt，跳过 Kohya 扩展依赖安装
)
if %errorlevel% neq 0 (
    echo [WARN] Kohya 依赖存在失败项，可先尝试运行 train_lora.py 查看实际缺失项
)
echo.

echo [Step 5/5] 验证 CUDA 和训练模块...
"%PYTHON_EXE%" -c "import torch, accelerate, xformers, diffusers, safetensors, bitsandbytes, voluptuous; print('PyTorch:', torch.__version__); print('CUDA available:', torch.cuda.is_available()); print('GPU:', torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'NOT FOUND'); print('STATUS: training environment ready')"
if %errorlevel% neq 0 (
    echo [ERROR] 环境验证失败，请检查上面的报错
    pause
    exit /b 1
)

echo.
echo ============================================
echo  安装完成！下一步可运行 start_train.bat 或 train_lora.py
echo ============================================
pause
endlocal
