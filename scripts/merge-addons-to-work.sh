#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
WORK_DIR="$ROOT_DIR/work/src/main/java"
ADDONS_DIR="$ROOT_DIR/addons"

# Number of parallel jobs (default: number of CPU cores, max 8)
PARALLEL_JOBS="${PARALLEL_JOBS:-$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)}"
[ "$PARALLEL_JOBS" -gt 8 ] && PARALLEL_JOBS=8

echo "=== Merging addons to work directory ==="
echo "Work source: $WORK_DIR"
echo "Addons source: $ADDONS_DIR"
echo "Parallel jobs: $PARALLEL_JOBS"
echo ""

# Check if rsync is available (preferred for efficiency)
if command -v rsync &>/dev/null; then
    echo "Using rsync for efficient file synchronization"
    USE_RSYNC=true
else
    echo "rsync not found, using optimized cp"
    USE_RSYNC=false
fi

# Mapping: addon relative path → work relative path (from work/src/main/java)
declare -A MAPPINGS=(
    ["lwjgl-stubs/src/main/java"]="org"
    ["blaze3d-impl/src/main/java/com/mojang/logging"]="com/mojang/logging"
    ["blaze3d-impl/src/main/java/top"]="top"
    ["jdk-stubs/jdk-extra/java/awt"]="java/awt"
    ["jdk-stubs/jdk-extra/java/beans"]="java/beans"
    ["jdk-stubs/jdk-extra/java/io"]="java/io"
    ["jdk-stubs/jdk-extra/java/lang"]="java/lang"
    ["jdk-stubs/jdk-extra/java/lang/invoke"]="java/lang/invoke"
    ["jdk-stubs/jdk-extra/java/lang/management"]="java/lang/management"
)

if [ "$USE_RSYNC" = true ]; then
    # rsync method
    for addon_rel in "${!MAPPINGS[@]}"; do
        addon_src="$ADDONS_DIR/$addon_rel"
        work_rel="${MAPPINGS[$addon_rel]}"
        work_dest="$WORK_DIR/$work_rel"

        if [ ! -d "$addon_src" ]; then
            echo "[Skip] $addon_src - source not found"
            continue
        fi

        echo "[Addon] $addon_rel → $work_rel"

        rsync -a --checksum --update --no-relative "$addon_src/" "$work_dest/"
    done
else
    # Fallback: optimized cp with find
    copy_addon() {
        local addon_rel=$1
        local addon_src="$ADDONS_DIR/$addon_rel"
        local work_rel="${MAPPINGS[$addon_rel]}"
        local work_dest="$WORK_DIR/$work_rel"

        if [ ! -d "$addon_src" ]; then
            echo "[Skip] $addon_src - source not found"
            return
        fi

        echo "[Addon] $addon_rel → $work_rel"

        # Create target directory first
        mkdir -p "$work_dest"

        # Copy files with smart update detection (only newer/missing)
        find "$addon_src" -name "*.java" -type f | while IFS= read -r file; do
            rel="${file#$addon_src/}"
            target="$work_dest/$rel"

            if [ ! -f "$target" ] || [ "$file" -nt "$target" ]; then
                mkdir -p "$(dirname "$target")"
                cp "$file" "$target"
                echo "  + $rel"
            fi
        done
    }

    for addon_rel in "${!MAPPINGS[@]}"; do
        copy_addon "$addon_rel"
    done
fi

echo ""
echo "=== Merge complete ==="
echo "Run './scripts/rebuild-patches-fast.py' to generate patches"
