#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

echo "Compiling VFS WASM module..."

if command -v clang &> /dev/null; then
    echo "Using clang..."
    clang --target=wasm32 -O3 -nostdlib \
        -Wl,--no-entry -Wl,--export-all -Wl,--strip-all -Wl,--allow-undefined \
        -o ../dist/vfs.wasm vfs.c
elif command -v emcc &> /dev/null; then
    echo "Using emcc..."
    emcc vfs.c -O3 \
        -s WASM=1 -s STANDALONE_WASM=1 \
        -s EXPORTED_FUNCTIONS="['_vfs_parse','_vfs_find','_vfs_get_data_offset','_vfs_get_data_len','_vfs_get_path_len','_vfs_get_path_offset','_vfs_get_blob','_vfs_get_path_strings','_vfs_get_entry_count']" \
        -s EXPORTED_RUNTIME_METHODS="[]" \
        -o ../dist/vfs.wasm
else
    echo "ERROR: Neither clang nor emcc found in PATH"
    exit 1
fi

if [ -f ../dist/vfs.wasm ]; then
    echo ""
    echo "SUCCESS: dist/vfs.wasm created"
    ls -lh ../dist/vfs.wasm
else
    echo "FAILED: vfs.wasm not created"
    exit 1
fi
