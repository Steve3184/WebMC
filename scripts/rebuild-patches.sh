#!/usr/bin/env bash
# Regenerate patches/ from work/ vs upstream/.
#
# How it works:
#   1. For each .java file under work/src/main/java that is part of upstream
#      (i.e. exists in upstream/src/main/java), diff -u against upstream
#      and write to patches/<rel>.patch if non-empty.
#   2. Files NOT in upstream are addons/own-source — never patched.
#   3. Files DELETED in work but present in upstream → emit deletion patch.
#
# Run after editing work/ to capture changes back into patches/ for VCS.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$DIR/lib/common.sh"

require_upstream_setup
[[ -d "$WORK_DIR/$MC_SOURCE_REL" ]] || err "work/ not built; run scripts/setup.sh first"

UPSTREAM_SRC="$UPSTREAM_DIR/$MC_SOURCE_REL"
WORK_SRC="$WORK_DIR/$MC_SOURCE_REL"

# Build a set of files that are 'addon-owned' (in addons/, not patches).
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

log "wiping patches/"
rm -rf "$PATCHES_DIR"
mkdir -p "$PATCHES_DIR"

generated=0
deleted=0

# 1) modified or added (compared to upstream) files in work/, excluding addons.
while IFS= read -r -d '' f; do
    rel="${f#$WORK_SRC/}"
    [[ -n "${ADDON_FILES[$rel]:-}" ]] && continue
    upstream_f="$UPSTREAM_SRC/$rel"

    if [[ -f "$upstream_f" ]]; then
        # Modified vs upstream?
        if ! diff -q "$upstream_f" "$f" >/dev/null 2>&1; then
            patch_path="$PATCHES_DIR/$rel.patch"
            mkdir -p "$(dirname "$patch_path")"
            diff -u --label "a/$rel" --label "b/$rel" "$upstream_f" "$f" > "$patch_path" || true
            generated=$((generated+1))
        fi
    else
        # New non-addon file under MC source tree → counts as a 'new file' patch.
        # Empty /dev/null vs file gives a complete-add diff.
        patch_path="$PATCHES_DIR/$rel.patch"
        mkdir -p "$(dirname "$patch_path")"
        diff -u --label "a/$rel" --label "b/$rel" /dev/null "$f" > "$patch_path" || true
        generated=$((generated+1))
    fi
done < <(find "$WORK_SRC" -type f -name '*.java' -print0)

# 2) files deleted in work/ vs upstream/
while IFS= read -r -d '' f; do
    rel="${f#$UPSTREAM_SRC/}"
    [[ -f "$WORK_SRC/$rel" ]] && continue
    patch_path="$PATCHES_DIR/$rel.patch"
    mkdir -p "$(dirname "$patch_path")"
    diff -u --label "a/$rel" --label "b/$rel" "$f" /dev/null > "$patch_path" || true
    deleted=$((deleted+1))
done < <(find "$UPSTREAM_SRC" -type f -name '*.java' -print0)

ok "generated $generated patch(es), $deleted deletion(s)"
