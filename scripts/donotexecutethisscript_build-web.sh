#!/usr/bin/env bash
# Build the web target end-to-end and stage dist/.
#
#   1. MCWEB_WEB=1 apply-patches.sh    (work/ contains the TeaVM gradle fragment)
#   2. cd work && ./gradlew generateJavaScript   (TeaVM emits build/generated/teavm/js/game.js)
#   3. copy game.js + addons/web/* → dist/
#   4. tell the user how to serve dist/
#
# Use scripts/setup.sh first (one-shot) to generate the MC source tree.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$DIR/lib/common.sh"

require_upstream_setup

# Re-apply with web fragment enabled.
log "rebuilding work/ with MCWEB_WEB=1"
MCWEB_WEB=1 "$DIR/apply-patches.sh"

# Drive TeaVM.
cd "$WORK_DIR"
log "running ./gradlew generateJavaScript (downloads TeaVM 0.13.1 plugin on first run)"
# _JAVA_OPTIONS propagates -Xss to the TeaVM out-of-process daemon's JVM
# (gradle-jvmargs only affects the gradle daemon itself). 512m stack is needed
# because TeaVM's AsyncMethodFinder recurses without a depth limit on our
# enlarged Bootstrap call graph.
export _JAVA_OPTIONS="-Xss512m"
./gradlew --console=plain generateJavaScript

DIST="$PROJECT_ROOT/dist"
mkdir -p "$DIST"

# TeaVM default output path: build/generated/teavm/js/<targetFileName>
TEAVM_OUT="$WORK_DIR/build/generated/teavm/js"
if [[ ! -d "$TEAVM_OUT" ]]; then
    err "TeaVM output dir not found at $TEAVM_OUT — did generateJavaScript actually succeed?"
fi

log "copying TeaVM output → $DIST"
cp -rv "$TEAVM_OUT"/* "$DIST/"

if [[ -d "$ADDONS_DIR/web" ]]; then
    log "copying addons/web/* → $DIST"
    cp -rv "$ADDONS_DIR/web"/* "$DIST/"
fi

ok "dist/ ready"
echo
echo "  To test locally, serve dist/ over HTTP (file:// won't work for ES modules):"
echo "    cd $DIST && python3 -m http.server 8080"
echo "  Then open http://localhost:8080/"
