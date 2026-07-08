#!/usr/bin/env bash
# Optimized parallel patch regeneration
# Uses xargs -P for parallel diff execution
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$DIR/lib/common.sh"

require_upstream_setup

UPSTREAM_SRC="$UPSTREAM_DIR/$MC_SOURCE_REL"
WORK_SRC="$WORK_DIR/$MC_SOURCE_REL"
PATCHES_DIR="$DIR/../patches"

[[ -d "$WORK_SRC" ]] || err "work/ not built; run scripts/setup.sh first"

# Build ADDON_FILES set (for filtering)
declare -A ADDON_FILES=()
for addon in \
        "$ADDONS_DIR/blaze3d-impl/src/main/java" \
        "$ADDONS_DIR/lwjgl-stubs/src/main/java" \
        "$ADDONS_DIR/teavm-runtime/src/main/java"; do
    [[ -d "$addon" ]] || continue
    while IFS= read -r -d '' f; do
        rel="${f#$addon/}"
        ADDON_FILES["$rel"]=1
    done < <(find "$addon" -type f -print0)
done

# Export for subshell
export UPSTREAM_SRC WORK_SRC PATCHES_DIR
declare -p ADDON_FILES > /tmp/addon_files.$$.json

log "wiping patches/"
rm -rf "$PATCHES_DIR"
mkdir -p "$PATCHES_DIR"

# Function to process single file
process_file() {
    local f="$1"
    local addon_json="/tmp/addon_files.$$.json"

    # Load ADDON_FILES from json
    local ADDON_FILES=$(python3 -c "
import json, sys
data = json.load(open('$addon_json'))
for k, v in data.items():
    print(k)
" 2>/dev/null | grep -Fxf <(echo "") || true)

    local rel="${f#$WORK_SRC/}"

    # Check if addon file (quick string check)
    if [[ "$rel" == com/mojang/blaze3d/* ]] || \
       [[ "$rel" == org/lwjgl/* ]] || \
       [[ "$rel" == top/steve3184/webmc/* ]]; then
        exit 0
    fi

    local upstream_f="$UPSTREAM_SRC/$rel"
    local patch_path="$PATCHES_DIR/$rel.patch"

    if [[ -f "$upstream_f" ]]; then
        if ! diff -q "$upstream_f" "$f" >/dev/null 2>&1; then
            mkdir -p "$(dirname "$patch_path")"
            diff -u --label "a/$rel" --label "b/$rel" "$upstream_f" "$f" > "$patch_path" || true
            echo "modified:$rel"
        fi
    else
        mkdir -p "$(dirname "$patch_path")"
        diff -u --label "a/$rel" --label "b/$rel" /dev/null "$f" > "$patch_path" || true
        echo "new:$rel"
    fi
}

export -f process_file 2>/dev/null || true

log "Finding modified files..."
modified=0
new=0
deleted=0

# Process modified/new files with parallel xargs
find "$WORK_SRC" -type f -name '*.java' -print0 2>/dev/null | \
    xargs -0 -P 8 -I{} bash -c '
        f="{}"
        rel="${f#$WORK_SRC/}"

        # Skip addon files
        case "$rel" in
            com/mojang/blaze3d/*|org/lwjgl/*|top/steve3184/webmc/*) exit 0 ;;
        esac

        upstream_f="$UPSTREAM_SRC/$rel"

        if [[ -f "$upstream_f" ]]; then
            if ! diff -q "$upstream_f" "$f" >/dev/null 2>&1; then
                patch_path="$PATCHES_DIR/$rel.patch"
                mkdir -p "$(dirname "$patch_path")"
                diff -u --label "a/$rel" --label "b/$rel" "$upstream_f" "$f" > "$patch_path" || true
                echo "modified:$rel" >&3
            fi
        else
            patch_path="$PATCHES_DIR/$rel.patch"
            mkdir -p "$(dirname "$patch_path")"
            diff -u --label "a/$rel" --label "b/$rel" /dev/null "$f" > "$patch_path" || true
            echo "new:$rel" >&3
        fi
    ' 3>&1 | while read -r line; do
        case "$line" in
            modified:*) ((modified++)) ;;
            new:*) ((new++)) ;;
        esac
        # Progress indicator every 100 files
        total=$((modified + new))
        if (( total % 500 == 0 )); then
            echo -ne "\r  Processed $total files..."
        fi
    done

echo "" # newline after progress

# Process deleted files
find "$UPSTREAM_SRC" -type f -name '*.java' -print0 2>/dev/null | \
    xargs -0 -P 8 -I{} bash -c '
        f="{}"
        rel="${f#$UPSTREAM_SRC/}"
        work_f="$WORK_SRC/$rel"

        if [[ ! -f "$work_f" ]]; then
            patch_path="$PATCHES_DIR/$rel.patch"
            mkdir -p "$(dirname "$patch_path")"
            diff -u --label "a/$rel" --label "b/$rel" "$f" /dev/null > "$patch_path" || true
            echo "deleted:$rel" >&3
        fi
    ' 3>&1 | while read -r line; do
        case "$line" in
            deleted:*) ((deleted++)) ;;
        esac
    done

rm -f /tmp/addon_files.$$.json

total=$((modified + new))
ok "generated $total patch(es), $deleted deletion(s)"
