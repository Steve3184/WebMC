$clang = "D:\clang+llvm-22.1.5-x86_64-pc-windows-msvc\bin\clang.exe"
$out = "m:\Users\l\Desktop\webmc1\dist\vfs.wasm"
$src = "m:\Users\l\Desktop\webmc1\wasm\vfs.c"
$args = @(
    "--target=wasm32",
    "-O0",
    "-nostdlib",
    "-Wl,--no-entry",
    "-Wl,--export-all",
    "-Wl,--strip-all",
    "-Wl,--allow-undefined",
    "-o",
    $out,
    $src
)
& $clang $args
Get-Item $out | Select-Object Length
