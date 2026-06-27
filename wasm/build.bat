@echo off
cd /d "%~dp0.."
"D:\clang+llvm-22.1.5-x86_64-pc-windows-msvc\bin\clang.exe" --target=wasm32 -O0 -nostdlib -Wl,--no-entry -Wl,--export-all -Wl,--strip-all -Wl,--allow-undefined -o "dist\vfs.wasm" "wasm\vfs.c"
dir "dist\vfs.wasm"
