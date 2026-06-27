@echo off
setlocal
chcp 65001 >nul

echo ============================================
echo  启动 Stable Diffusion WebUI
echo  加载 Minecraft LoRA 支持
echo ============================================
echo.

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"
set "SD_DIR=%PROJECT_DIR%\stable-diffusion-webui"
set "LORA_SRC=%PROJECT_DIR%\output\lora_output"
set "LORA_DST=%SD_DIR%\models\Lora"
set "MODEL_PATH=%SD_DIR%\models\Stable-diffusion\v1-5-pruned-emaonly.safetensors"

if not exist "%SD_DIR%\webui.bat" (
    echo [ERROR] 未找到 SD-WebUI 启动脚本:
    echo   %SD_DIR%\webui.bat
    pause
    exit /b 1
)

if not exist "%MODEL_PATH%" (
    echo [ERROR] 未找到基础模型:
    echo   %MODEL_PATH%
    pause
    exit /b 1
)

if not exist "%LORA_DST%" mkdir "%LORA_DST%"

if exist "%LORA_SRC%\*.safetensors" (
    for %%f in ("%LORA_SRC%\*.safetensors") do (
        if not exist "%LORA_DST%\%%~nxf" (
            echo 复制新 LoRA: %%~nxf
            copy "%%f" "%LORA_DST%\" >nul
        )
    )
)

echo 启动 SD-WebUI（API 模式 + 监听所有接口）...
echo 浏览器打开: http://127.0.0.1:7860
echo.

cd /d "%SD_DIR%"
call webui.bat ^
    --api ^
    --listen ^
    --xformers ^
    --medvram ^
    --no-half-vae ^
    --skip-python-version-check ^
    --ckpt "%MODEL_PATH%"

pause
endlocal
