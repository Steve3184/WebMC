@echo off
"D:\clang+llvm-22.1.5-x86_64-pc-windows-msvc\bin\clang.exe" --target=wasm32 -O3 -nostdlib -Wl,--no-entry -Wl,--export-all -Wl,--strip-all -Wl,--allow-undefined -o "M:\Users\l\Desktop\webmc1\dist\vfs.wasm" "M:\Users\l\Desktop\webmc1\wasm\vfs.c"
if errorlevel 1 (
    echo Build failed
    exit /b 1
)
