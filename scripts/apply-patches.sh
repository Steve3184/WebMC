#!/usr/bin/env bash
# Build work/ from upstream/ + patches/ + addons/.
# Destructive on work/: `rm -rf work/` then re-stage. Never edit work/ by hand;
# edit upstream-derived files via patches, edit our own code in addons/.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$DIR/lib/common.sh"

require_upstream_setup
require_cmd rsync patch

log "wiping work/"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

log "rsync upstream → work (excluding .git, build, .gradle)"
rsync -a \
    --exclude='.git' \
    --exclude='.gradle' \
    --exclude='build' \
    --exclude='out' \
    --exclude='run' \
    "$UPSTREAM_DIR/" "$WORK_DIR/"

# apply patches
if [[ -d "$PATCHES_DIR" ]] && [[ -n "$(find "$PATCHES_DIR" -name '*.patch' -type f -print -quit 2>/dev/null)" ]]; then
    log "applying patches"
    count=0
    while IFS= read -r -d '' p; do
        rel="${p#$PATCHES_DIR/}"
        log "  $rel"
        # Patches use unified diff labels rooted at the MC source dir
        # (a/com/mojang/... rather than a/src/main/java/com/mojang/...) so the
        # patch tool needs cwd=work/src/main/java with -p1 to find files.
        (cd "$WORK_DIR/$MC_SOURCE_REL" && patch -p1 --no-backup-if-mismatch < "$p") \
            || err "patch failed: $rel"
        count=$((count+1))
    done < <(find "$PATCHES_DIR" -name '*.patch' -type f -print0 | sort -z)
    ok "applied $count patch(es)"
else
    log "no patches in $PATCHES_DIR (skipping)"
fi

# Inject addons. All addons go into src/main/java so the TeaVM plugin (which
# scans the main source set by default) can see them. The build.gradle.fragment
# excludes the real org.lwjgl artifacts from the compile classpath so our stubs
# under org.lwjgl.* are the only ones the compiler / TeaVM see.
#
# Trade-off: ./gradlew runclient (desktop) is broken — that path needs the real
# LWJGL natives. We don't care; phase 4+ targets the browser.
if [[ -d "$ADDONS_DIR/blaze3d-impl/src/main/java" ]]; then
    log "injecting addons/blaze3d-impl → work/$MC_SOURCE_REL"
    rsync -a "$ADDONS_DIR/blaze3d-impl/src/main/java/" "$WORK_DIR/$MC_SOURCE_REL/"
fi
if [[ -d "$ADDONS_DIR/lwjgl-stubs/src/main/java" ]]; then
    log "injecting addons/lwjgl-stubs → work/$MC_SOURCE_REL"
    rsync -a "$ADDONS_DIR/lwjgl-stubs/src/main/java/" "$WORK_DIR/$MC_SOURCE_REL/"
fi
if [[ -d "$ADDONS_DIR/teavm-runtime/src/main/java" ]]; then
    log "injecting addons/teavm-runtime → work/$MC_SOURCE_REL"
    rsync -a "$ADDONS_DIR/teavm-runtime/src/main/java/" "$WORK_DIR/$MC_SOURCE_REL/"
fi
if [[ -d "$ADDONS_DIR/jdk-stubs" ]]; then
    # jdk-stubs/locks and jdk-stubs/sun-misc shadow JDK packages (java.util.concurrent.locks
    # and sun.misc respectively). They CANNOT live in src/main/java because javac rejects
    # "package already exists in module java.base / jdk.unsupported". We instead let the
    # TeaVM build fragment compile them with --patch-module flags into separate dirs and
    # add the dirs to runtimeClasspath only — TeaVM sees them, javac compileJava does not.
    # Nothing to inject into work/ here; the fragment reads from addons/jdk-stubs directly.
    log "addons/jdk-stubs present (TeaVM-only path; compiled via build.gradle fragment)"
fi

# inject shader overrides
if [[ -d "$SHADERS_DIR" ]] && [[ -n "$(ls -A "$SHADERS_DIR" 2>/dev/null)" ]]; then
    SHADER_DEST="$WORK_DIR/$MC_RESOURCES_REL/assets/minecraft/shaders"
    mkdir -p "$SHADER_DEST"
    log "injecting shaders/ → $SHADER_DEST"
    rsync -a "$SHADERS_DIR/" "$SHADER_DEST/"
fi

# Always-on build.gradle overrides: exclude real LWJGL (our stubs replace it
# in source). Without this, both our stubs and the real maven LWJGL artifacts
# would have org.lwjgl.* and the Java compiler would fail on duplicate classes.
# We target compileClasspath / runtimeClasspath only — MCP-Reborn's `:shade`
# configuration gets resolved early in build.gradle and barfs if we touch it.
{
    echo ""
    echo "// === injected by scripts/apply-patches.sh: kick real LWJGL off the classpath ==="
    echo "afterEvaluate {"
    echo "    ['compileClasspath', 'runtimeClasspath', 'testCompileClasspath', 'testRuntimeClasspath'].each { n ->"
    echo "        def c = configurations.findByName(n)"
    echo "        if (c != null) {"
    echo "            c.exclude group: 'org.lwjgl'"
    echo "            // com.mojang:logging is replaced by our shadow LogUtils"
    echo "            // (addons/blaze3d-impl/com/mojang/logging/LogUtils.java) that uses"
    echo "            // SLF4J NOPLogger directly — bypasses log4j2's static-init chain"
    echo "            // (~70 missing methods on TeaVM's analyzer)."
    echo "            c.exclude group: 'com.mojang', module: 'logging'"
    echo "            // Cut log4j entirely — log4j-slf4j-impl is the SLF4J binding"
    echo "            // that pulls log4j-core via ServiceLoader. Without it SLF4J"
    echo "            // falls back to NOPLoggerFactory which we want."
    echo "            c.exclude group: 'org.apache.logging.log4j'"
    echo "        }"
    echo "    }"
    echo "}"
    echo ""
    echo "// teavm-jso annotations are referenced from main-classpath HelloMain (CLASS-retention,"
    echo "// no runtime cost on desktop). Always added so compileJava resolves @JSBody."
    echo "repositories { mavenCentral() }"
    echo "dependencies {"
    echo "    implementation 'org.teavm:teavm-jso:0.13.1'"
    echo "    // teavm-core hosts the VirtualFileSystem SPI used by"
    echo "    // top.steve3184.webmc.vfs.WebFs (compileOnly because TeaVM's"
    echo "    // classlib already provides these at runtime)."
    echo "    compileOnly 'org.teavm:teavm-core:0.13.1'"
    echo "}"
} >> "$WORK_DIR/build.gradle"

# splice teavm gradle fragment if present.
if [[ -f "$ADDONS_DIR/teavm-runtime/build.gradle.fragment" ]]; then
    log "appending TeaVM gradle fragment to work/build.gradle"
    {
        echo ""
        echo "// === injected from addons/teavm-runtime/build.gradle.fragment ==="
        cat "$ADDONS_DIR/teavm-runtime/build.gradle.fragment"
    } >> "$WORK_DIR/build.gradle"
fi

# Relax JVM TLS settings for environments where JDK 21's default
# (TLS 1.2/1.3 only, with conservative jdk.tls.disabledAlgorithms) breaks
# handshakes against maven.minecraftforge.net or other repos. Symptom is
# "Remote host terminated the handshake" while Firefox/curl can fetch fine.
# Append to whatever org.gradle.jvmargs already says (last definition wins).
{
    echo ""
    echo "# === injected by scripts/apply-patches.sh: relax TLS for slow/weird networks ==="
    echo 'systemProp.https.protocols=TLSv1,TLSv1.1,TLSv1.2,TLSv1.3'
    echo 'systemProp.jdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2,TLSv1.3'
    echo 'systemProp.jdk.tls.disabledAlgorithms='
    echo 'systemProp.com.sun.net.ssl.checkRevocation=false'
    # disable HTTP/2 client preference (sometimes Cloudflare ALPN trips Java)
    echo 'systemProp.jdk.httpclient.allowRestrictedHeaders=connection,content-length,host,upgrade'
    echo 'org.gradle.jvmargs=-Xmx6G -Xss512M -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2,TLSv1.3 -Djdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2,TLSv1.3 -Djdk.tls.disabledAlgorithms= -Dnet.minecraftforge.gradle.check.certs=false'
} >> "$WORK_DIR/gradle.properties"

ok "work/ ready"
