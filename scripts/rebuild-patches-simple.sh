#!/usr/bin/env bash
# Fast single-threaded patch generator with progress
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$DIR/lib/common.sh"

require_upstream_setup

UPSTREAM_SRC="$UPSTREAM_DIR/$MC_SOURCE_REL"
WORK_SRC="$WORK_DIR/$MC_SOURCE_REL"
PATCHES_DIR="$DIR/../patches"

[[ -d "$WORK_SRC" ]] || err "work/ not built; run scripts/setup.sh first"

# Log function (override from common.sh)
ok() { echo -e "\033[1;32m[OK]\033[0m $1"; }

log "wiping patches/"
rm -rf "$PATCHES_DIR"
mkdir -p "$PATCHES_DIR"

modified=0
new=0
deleted=0

# Process work files
log "scanning work/src/..."
total=$(find "$WORK_SRC" -type f -name '*.java' 2>/dev/null | wc -l)
count=0

while IFS= read -r -d '' f; do
    rel="${f#$WORK_SRC/}"

    # Skip addon files (quick string check)
    case "$rel" in
        com/mojang/blaze3d/*|org/lwjgl/*|top/steve3184/webmc/*) ((count++)); continue ;;
    esac

    upstream_f="$UPSTREAM_SRC/$rel"

    if [[ -f "$upstream_f" ]]; then
        if ! diff -q "$upstream_f" "$f" >/dev/null 2>&1; then
            patch_path="$PATCHES_DIR/$rel.patch"
            mkdir -p "$(dirname "$patch_path")"
            diff -u --label "a/$rel" --label "b/$rel" "$upstream_f" "$f" > "$patch_path" || true
            ((modified++)) || true
        fi
    else
        patch_path="$PATCHES_DIR/$rel.patch"
        mkdir -p "$(dirname "$patch_path")"
        diff -u --label "a/$rel" --label "b/$rel" /dev/null "$f" > "$patch_path" || true
        ((new++))
    fi

    ((count++)) || true
    if (( count % 500 == 0 )); then
        echo -ne "\r  Processed $count/$total files (modified=$modified, new=$new)..."
    fi
done < <(find "$WORK_SRC" -type f -name '*.java' -print0)

echo "" # newline

# Process deleted files
log "scanning deleted files..."
while IFS= read -r -d '' f; do
    rel="${f#$UPSTREAM_SRC/}"

    case "$rel" in
        com/mojang/blaze3d/*|org/lwjgl/*|top/steve3184/webmc/*) continue ;;
    esac

    if [[ ! -f "$WORK_SRC/$rel" ]]; then
        patch_path="$PATCHES_DIR/$rel.patch"
        mkdir -p "$(dirname "$patch_path")"
        diff -u --label "a/$rel" --label "b/$rel" "$f" /dev/null > "$patch_path" || true
        ((deleted++))
    fi
done < <(find "$UPSTREAM_SRC" -type f -name '*.java' -print0)

total_gen=$((modified + new))
ok "generated $total_gen patch(es), $deleted deletion(s)"
