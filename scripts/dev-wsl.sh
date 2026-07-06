#!/usr/bin/env bash
#===============================================
# WebMC 开发环境一键脚本 (WSL/Linux)
#===============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[1;36m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_ok()     { echo -e "${GREEN}[ OK ]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

#===============================================
# 检测环境
#===============================================
detect_env() {
    log_info "检测运行环境..."

    # 检测是否 WSL
    if grep -qi microsoft /proc/version 2>/dev/null; then
        IS_WSL=true
        log_ok "检测到 WSL 环境"
    else
        IS_WSL=false
        log_ok "检测到 Linux/macOS 环境"
    fi

    # 检测 Java
    if command -v java &>/dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -1)
        log_ok "Java: $JAVA_VERSION"
    else
        log_error "未检测到 Java，请安装 JDK 21+"
        return 1
    fi

    # 检测 Gradle
    if command -v gradle &>/dev/null; then
        GRADLE_VERSION=$(gradle --version | head -1)
        log_ok "Gradle: $GRADLE_VERSION"
    else
        log_warn "Gradle 未检测到，将使用 Gradle Wrapper"
    fi

    # 检测 Node.js
    if command -v node &>/dev/null; then
        NODE_VERSION=$(node --version)
        log_ok "Node.js: $NODE_VERSION"
    else
        log_error "未检测到 Node.js，请安装"
        return 1
    fi

    # 检测 rsync
    if command -v rsync &>/dev/null; then
        log_ok "rsync 已安装"
    else
        log_warn "rsync 未安装，Windows 用户请安装 Git for Windows (含 rsync)"
        if $IS_WSL; then
            log_info "尝试安装 rsync..."
            sudo apt-get update && sudo apt-get install -y rsync 2>/dev/null || true
        fi
    fi

    return 0
}

#===============================================
# 初始化子模块
#===============================================
init_submodules() {
    log_info "初始化 Git 子模块..."

    if [ -f "$PROJECT_ROOT/.gitmodules" ]; then
        git submodule update --init --recursive
        log_ok "子模块初始化完成"
    else
        log_warn "无 .gitmodules 文件，跳过"
    fi
}

#===============================================
# 同步 upstream 到 work
#===============================================
sync_upstream() {
    log_info "同步 upstream 到 work..."

    UPSTREAM_DIR="$PROJECT_ROOT/upstream"
    WORK_DIR="$PROJECT_ROOT/work"
    PATCHES_DIR="$PROJECT_ROOT/patches"

    if [ ! -d "$UPSTREAM_DIR" ]; then
        log_error "upstream 目录不存在，请先运行 setup"
        return 1
    fi

    # 检查 patches 目录
    if [ ! -d "$PATCHES_DIR" ]; then
        log_warn "patches 目录不存在，创建中..."
        mkdir -p "$PATCHES_DIR"
    fi

    # rsync upstream -> work (排除 .git, build, .gradle)
    if command -v rsync &>/dev/null; then
        log_info "使用 rsync 同步..."
        rsync -a \
            --exclude='.git' \
            --exclude='.gradle' \
            --exclude='build' \
            --exclude='out' \
            --exclude='run' \
            "$UPSTREAM_DIR/" "$WORK_DIR/"
        log_ok "同步完成"
    else
        log_warn "rsync 不可用，尝试 cp..."
        cp -r "$UPSTREAM_DIR"/* "$WORK_DIR/" 2>/dev/null || true
    fi
}

#===============================================
# 应用 patches
#===============================================
apply_patches() {
    log_info "应用 patches..."

    PATCHES_DIR="$PROJECT_ROOT/patches"
    WORK_DIR="$PROJECT_ROOT/work"
    MC_SOURCE_REL="src/main/java"

    if [ ! -d "$PATCHES_DIR" ]; then
        log_warn "无 patches 目录，跳过"
        return 0
    fi

    PATCH_COUNT=$(find "$PATCHES_DIR" -name "*.patch" -type f 2>/dev/null | wc -l)
    if [ "$PATCH_COUNT" -eq 0 ]; then
        log_warn "无 .patch 文件，跳过"
        return 0
    fi

    log_info "找到 $PATCH_COUNT 个 patches..."

    # 应用所有 patches
    find "$PATCHES_DIR" -name "*.patch" -type f -print0 | sort -z | while IFS= read -r -d '' p; do
        target=$(echo "$p" | sed "s|$PATCHES_DIR/||; s|\.patch$||")
        log_info "  应用: $target"
        (cd "$WORK_DIR/$MC_SOURCE_REL" && patch -p1 --no-backup-if-mismatch < "$p") \
            || log_warn "  失败: $target (可能已应用)"
    done

    log_ok "Patches 应用完成"
}

#===============================================
# 构建 Web 版本
#===============================================
build_web() {
    log_info "构建 Web 版本..."

    WORK_DIR="$PROJECT_ROOT/work"

    cd "$WORK_DIR"

    # 使用 Gradle 构建
    if [ -f "gradlew" ]; then
        chmod +x gradlew
        log_info "运行 ./gradlew :mcp-reborn_client:build..."
        ./gradlew :mcp-reborn_client:build --no-daemon 2>&1 | tail -20
    else
        log_error "gradlew 不存在"
        return 1
    fi

    log_ok "Web 构建完成"
}

#===============================================
# 启动开发服务器
#===============================================
start_dev_server() {
    log_info "启动开发服务器..."

    BUILD_DIR="$PROJECT_ROOT/build/web-run"

    if [ ! -d "$BUILD_DIR" ]; then
        log_error "构建目录不存在，请先构建"
        return 1
    fi

    # 启动静态服务器
    if command -v python3 &>/dev/null; then
        log_info "使用 Python 启动服务器 http://localhost:8080"
        cd "$BUILD_DIR"
        python3 -m http.server 8080
    elif command -v node &>/dev/null; then
        log_info "使用 Node.js 启动服务器 http://localhost:8080"
        cd "$PROJECT_ROOT"
        node -e "
            const http = require('http');
            const fs = require('fs');
            const path = require('path');
            const port = 8080;
            const dir = '$BUILD_DIR';

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
        "
    else
        log_error "无可用的 HTTP 服务器"
        return 1
    fi
}

#===============================================
# 一键构建 + 启动
#===============================================
build_and_serve() {
    log_info "=========================================="
    log_info "  WebMC 一键构建"
    log_info "=========================================="

    detect_env || return 1
    init_submodules
    sync_upstream
    apply_patches
    build_web

    log_info "=========================================="
    log_ok "构建完成!"
    log_info "=========================================="

    # 启动开发服务器
    start_dev_server
}

#===============================================
# 显示帮助
#===============================================
show_help() {
    cat << EOF
WebMC 开发环境脚本

用法: $0 <命令>

命令:
    help        显示此帮助
    env         检测环境
    init        初始化子模块
    sync        同步 upstream 到 work
    patches     应用 patches
    build       构建 Web 版本
    serve       启动开发服务器
    all         一键构建并启动 (默认)

示例:
    $0           # 一键构建并启动
    $0 build     # 仅构建
    $0 serve     # 仅启动服务器

EOF
}

#===============================================
# 主入口
#===============================================
main() {
    CMD="${1:-all}"

    case "$CMD" in
        help|--help|-h)
            show_help
            ;;
        env)
            detect_env
            ;;
        init)
            init_submodules
            ;;
        sync)
            sync_upstream
            ;;
        patches)
            apply_patches
            ;;
        build)
            detect_env
            build_web
            ;;
        serve)
            start_dev_server
            ;;
        all)
            build_and_serve
            ;;
        *)
            log_error "未知命令: $CMD"
            show_help
            return 1
            ;;
    esac
}

main "$@"
