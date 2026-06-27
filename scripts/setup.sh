#!/usr/bin/env bash
# End-to-end first-time setup:
#   1. Clone MCP-Reborn at the pinned commit (if missing)
#   2. Run gradle setup to generate the MC source
#   3. Build the work/ tree (apply patches + sync addons)
#
# Idempotent: rerun after pulling new patches/ to refresh work/.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$DIR/lib/common.sh"

require_cmd git rsync patch java

# Step 1: clone
if [[ ! -d "$UPSTREAM_DIR/.git" ]]; then
    log "cloning MCP-Reborn @ $UPSTREAM_COMMIT (shallow)"
    mkdir -p "$UPSTREAM_DIR"
    git -C "$UPSTREAM_DIR" init -q
    git -C "$UPSTREAM_DIR" remote add origin "$UPSTREAM_REPO"
    git -C "$UPSTREAM_DIR" fetch --depth 1 origin "$UPSTREAM_COMMIT"
    git -C "$UPSTREAM_DIR" checkout -q "$UPSTREAM_COMMIT"
    mkdir -p "$UPSTREAM_DIR/projects/mcp"
    ok "cloned"
else
    HEAD="$(git -C "$UPSTREAM_DIR" rev-parse HEAD)"
    [[ "$HEAD" = "$UPSTREAM_COMMIT" ]] || \
        log "WARN: upstream HEAD ($HEAD) != pinned ($UPSTREAM_COMMIT)"
fi

# Step 2: gradle setup (skip if src already present)
if [[ ! -d "$UPSTREAM_DIR/$MC_SOURCE_REL" ]]; then
    log "running ./gradlew setup in upstream/ (this can take 10–30 min on slow networks)"
    (cd "$UPSTREAM_DIR" && ./gradlew --console=plain setup)
    ok "MC source generated at upstream/$MC_SOURCE_REL"
else
    log "upstream MC source already present, skipping gradle setup"
fi

# Step 3: build work/ tree
"$DIR/apply-patches.sh"

ok "setup complete. To build for web: scripts/build-web.sh"
