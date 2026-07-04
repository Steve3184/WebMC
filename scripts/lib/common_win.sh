#!/usr/bin/env bash
# Common shell helpers. `source` from other scripts.
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
UPSTREAM_DIR="$PROJECT_ROOT/upstream"
WORK_DIR="$PROJECT_ROOT/work"
PATCHES_DIR="$PROJECT_ROOT/patches"
ADDONS_DIR="$PROJECT_ROOT/addons"
SHADERS_DIR="$PROJECT_ROOT/shaders"

UPSTREAM_REPO="https://github.com/Hexeption/MCP-Reborn.git"
UPSTREAM_COMMIT="763bd65d34646a1b9625f5b9705aa7aad2ba2688"

MC_SOURCE_REL="src/main/java"
MC_RESOURCES_REL="src/main/resources"

log() { printf '\033[1;36m[%s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*" >&2; }
err() { printf '\033[1;31m[%s] ERROR:\033[0m %s\n' "$(date +%H:%M:%S)" "$*" >&2; exit 1; }
ok()  { printf '\033[1;32m[%s] OK:\033[0m %s\n' "$(date +%H:%M:%S)" "$*" >&2; }

require_cmd() {
    for c in "$@"; do
        command -v "$c" >/dev/null 2>&1 || err "missing command: $c"
    done
}

require_upstream_setup() {
    [[ -d "$UPSTREAM_DIR/$MC_SOURCE_REL" ]] || \
        err "upstream/$MC_SOURCE_REL not found. Run scripts/setup.sh first."
}
