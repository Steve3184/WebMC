# WebMC 开发环境一键脚本 (PowerShell)
# 使用方法: .\dev.ps1 <命令>

param(
    [Parameter(Position=0)]
    [ValidateSet("help", "env", "init", "sync", "patches", "build", "serve", "all")]
    [string]$Command = "all"
)

$ErrorActionPreference = "Stop"
$PROJECT_ROOT = $PSScriptRoot -replace "\\scripts$", ""

# 颜色输出
function Write-Info { param($msg) Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "[ OK ]  $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Error { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red }

#===============================================
# 检测环境
#===============================================
function Detect-Env {
    Write-Info "检测运行环境..."

    # 检测 Java
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) {
        $version = java -version 2>&1 | Select-Object -First 1
        Write-Ok "Java: $version"
    } else {
        Write-Error "未检测到 Java，请安装 JDK 21+"
        return $false
    }

    # 检测 Gradle
    $gradle = Get-Command gradle -ErrorAction SilentlyContinue
    if ($gradle) {
        Write-Ok "Gradle: $(gradle --version | Select-Object -First 1)"
    } else {
        Write-Warn "Gradle 未检测到，将使用 Gradle Wrapper"
    }

    # 检测 Node.js
    $node = Get-Command node -ErrorAction SilentlyContinue
    if ($node) {
        Write-Ok "Node.js: $(node --version)"
    } else {
        Write-Error "未检测到 Node.js，请安装"
        return $false
    }

    # 检测 rsync (Git for Windows 自带)
    $rsync = Get-Command rsync -ErrorAction SilentlyContinue
    if ($rsync) {
        Write-Ok "rsync 已安装"
    } else {
        Write-Warn "rsync 未安装，部分功能可能不可用"
    }

    return $true
}

#===============================================
# 初始化子模块
#===============================================
function Init-Submodules {
    Write-Info "初始化 Git 子模块..."

    Push-Location $PROJECT_ROOT
    try {
        git submodule update --init --recursive
        Write-Ok "子模块初始化完成"
    } catch {
        Write-Warn "子模块初始化失败: $_"
    }
    Pop-Location
}

#===============================================
# 同步 upstream 到 work
#===============================================
function Sync-Upstream {
    Write-Info "同步 upstream 到 work..."

    $upstreamDir = Join-Path $PROJECT_ROOT "upstream"
    $workDir = Join-Path $PROJECT_ROOT "work"
    $patchesDir = Join-Path $PROJECT_ROOT "patches"

    if (-not (Test-Path $upstreamDir)) {
        Write-Error "upstream 目录不存在，请先运行 setup"
        return $false
    }

    if (-not (Test-Path $patchesDir)) {
        Write-Warn "patches 目录不存在，创建中..."
        New-Item -ItemType Directory -Path $patchesDir -Force | Out-Null
    }

    # 使用 robocopy 同步 (Windows 原生)
    Write-Info "使用 robocopy 同步..."
    $exclude = "/XD", ".git", ".gradle", "build", "out", "run"
    robocopy $upstreamDir $workDir /E /NFL /NDL /NJH /NJS /NC /NS /NP $exclude *>&1 | Out-Null

    Write-Ok "同步完成"
    return $true
}

#===============================================
# 应用 patches
#===============================================
function Apply-Patches {
    Write-Info "应用 patches..."

    $patchesDir = Join-Path $PROJECT_ROOT "patches"
    $workDir = Join-Path $PROJECT_ROOT "work"
    $mcSourceRel = "src\main\java"

    if (-not (Test-Path $patchesDir)) {
        Write-Warn "无 patches 目录，跳过"
        return
    }

    $patchFiles = Get-ChildItem -Path $patchesDir -Filter "*.patch" -Recurse -File
    if ($patchFiles.Count -eq 0) {
        Write-Warn "无 .patch 文件，跳过"
        return
    }

    Write-Info "找到 $($patchFiles.Count) 个 patches..."

    foreach ($patch in $patchFiles) {
        $relativePath = $patch.FullName.Substring($patchesDir.Length + 1) -replace "\\.patch$", ""
        $relativePath = $relativePath -replace "\\", "/"

        Write-Info "  应用: $relativePath"

        $targetDir = Join-Path $workDir $mcSourceRel
        $targetDir = $targetDir.Split("\")[0..($targetDir.Split("\").Length - 2)] -join "\"

        # 使用 patch 命令 (需要 Git for Windows)
        $patchCmd = Get-Command patch -ErrorAction SilentlyContinue
        if ($patchCmd) {
            Push-Location $targetDir
            try {
                & patch -p1 --no-backup-if-mismatch --quiet < $patch.FullName 2>&1 | Out-Null
            } catch {
                Write-Warn "  失败: $relativePath"
            }
            Pop-Location
        } else {
            Write-Warn "  跳过: patch 命令不可用"
            break
        }
    }

    Write-Ok "Patches 应用完成"
}

#===============================================
# 构建 Web 版本
#===============================================
function Build-Web {
    Write-Info "构建 Web 版本..."

    $workDir = Join-Path $PROJECT_ROOT "work"

    Push-Location $workDir
    try {
        if (Test-Path "gradlew.bat") {
            Write-Info "运行 .\gradlew.bat :mcp-reborn_client:build..."
            & .\gradlew.bat :mcp-reborn_client:build --no-daemon 2>&1 | Select-Object -Last 20
        } elseif (Test-Path "gradlew") {
            Write-Info "运行 ./gradlew :mcp-reborn_client:build..."
            & .\gradlew :mcp-reborn_client:build --no-daemon 2>&1 | Select-Object -Last 20
        } else {
            Write-Error "gradlew 不存在"
            return $false
        }
        Write-Ok "Web 构建完成"
        return $true
    } catch {
        Write-Error "构建失败: $_"
        return $false
    } finally {
        Pop-Location
    }
}

#===============================================
# 启动开发服务器
#===============================================
function Start-DevServer {
    Write-Info "启动开发服务器..."

    $buildDir = Join-Path $PROJECT_ROOT "build\web-run"

    if (-not (Test-Path $buildDir)) {
        Write-Error "构建目录不存在，请先构建"
        return $false
    }

    Write-Info "启动服务器 http://localhost:8080"
    Write-Info "按 Ctrl+C 停止服务器"

    # 使用 Node.js 启动简单服务器
    $serverCode = @"
const http = require('http');
const fs = require('fs');
const path = require('path');
const port = 8080;
const dir = '$buildDir'.replace(/\\/g, '/');

const mimeTypes = {
    '.html': 'text/html',
    '.js': 'application/javascript',
    '.css': 'text/css',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.gz': 'application/gzip'
};

http.createServer((req, res) => {
    let file = path.join(dir, req.url === '/' ? 'index.html' : req.url);
    const ext = path.extname(file).toLowerCase();
    const mime = mimeTypes[ext] || 'application/octet-stream';

    fs.readFile(file, (err, data) => {
        if (err) {
            res.writeHead(404);
            res.end('Not found');
            return;
        }
        res.writeHead(200, { 'Content-Type': mime });
        res.end(data);
    });
}).listen(port, () => console.log('Server at http://localhost:' + port));
"@

    $tempFile = [System.IO.Path]::GetTempFileName() + ".js"
    $serverCode | Out-File -FilePath $tempFile -Encoding UTF8

    try {
        node $tempFile
    } finally {
        Remove-Item $tempFile -ErrorAction SilentlyContinue
    }
}

#===============================================
# 一键构建 + 启动
#===============================================
function Build-All {
    Write-Info "=========================================="
    Write-Info "  WebMC 一键构建"
    Write-Info "=========================================="

    if (-not (Detect-Env)) { return }

    Init-Submodules
    Sync-Upstream
    Apply-Patches
    Build-Web

    Write-Info "=========================================="
    Write-Ok "构建完成!"
    Write-Info "=========================================="

    Start-DevServer
}

#===============================================
# 显示帮助
#===============================================
function Show-Help {
    @"
WebMC 开发环境脚本

用法: .\dev.ps1 <命令>

命令:
    help       显示此帮助
    env        检测环境
    init       初始化子模块
    sync       同步 upstream 到 work
    patches    应用 patches
    build      构建 Web 版本
    serve      启动开发服务器
    all        一键构建并启动 (默认)

示例:
    .\dev.ps1          # 一键构建并启动
    .\dev.ps1 build   # 仅构建
    .\dev.ps1 serve   # 仅启动服务器

在 WSL 中也可以使用:
    bash scripts/dev-wsl.sh

"@
}

#===============================================
# 主入口
#===============================================
switch ($Command) {
    "help"   { Show-Help }
    "env"    { Detect-Env }
    "init"   { Init-Submodules }
    "sync"   { Sync-Upstream }
    "patches"{ Apply-Patches }
    "build"  {
        Detect-Env
        Build-Web
    }
    "serve"  { Start-DevServer }
    "all"    { Build-All }
}
