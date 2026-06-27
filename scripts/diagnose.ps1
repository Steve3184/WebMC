# Diagnose WebMC project status
Write-Host "=== UPSTREAM CHECK ==="
if (Test-Path "upstream") {
    $upFiles = Get-ChildItem "upstream" -Recurse -File | Measure-Object | Select-Object -ExpandProperty Count
    Write-Host "  Files in upstream/: $upFiles"
    if (Test-Path "upstream\build.gradle") {
        Write-Host "  build.gradle: YES"
    } else {
        Write-Host "  build.gradle: NO"
    }
    $javaFiles = Get-ChildItem "upstream\src\main\java" -Recurse -File -ErrorAction SilentlyContinue | Measure-Object | Select-Object -ExpandProperty Count
    Write-Host "  Java sources in src/main/java: $javaFiles"
} else {
    Write-Host "  upstream/: NOT FOUND"
}

Write-Host ""
Write-Host "=== PATCHES CHECK ==="
$patches = Get-ChildItem "patches" -Filter "*.patch" -ErrorAction SilentlyContinue | Measure-Object | Select-Object -ExpandProperty Count
Write-Host "  .patch files: $patches"

Write-Host ""
Write-Host "=== WORK CHECK ==="
if (Test-Path "work") {
    $wFiles = Get-ChildItem "work" -Recurse -File | Measure-Object | Select-Object -ExpandProperty Count
    Write-Host "  Files in work/: $wFiles"
} else {
    Write-Host "  work/: NOT FOUND"
}

Write-Host ""
Write-Host "=== ADDONS CHECK ==="
if (Test-Path "addons") {
    Get-ChildItem "addons" -Directory | ForEach-Object {
        $sub = Get-ChildItem $_.FullName -Recurse -File | Measure-Object | Select-Object -ExpandProperty Count
        Write-Host "  $_.Name/: $sub files"
    }
} else {
    Write-Host "  addons/: NOT FOUND"
}

Write-Host ""
Write-Host "=== BUILD FILES ==="
@("upstream\build.gradle", "addons\build.gradle", "addons\teavm-runtime\build.gradle", "addons\teavm-runtime\build.gradle.fragment") | ForEach-Object {
    if (Test-Path $_) { Write-Host "  $_: EXISTS" } else { Write-Host "  $_: MISSING" }
}

Write-Host ""
Write-Host "=== DIST/WEB OUTPUT ==="
if (Test-Path "dist") {
    Get-ChildItem "dist" -Recurse -File | ForEach-Object { Write-Host "  dist/$_" }
} else {
    Write-Host "  dist/: NOT FOUND"
}
if (Test-Path "wasm") {
    Get-ChildItem "wasm" -Recurse -File | ForEach-Object { Write-Host "  wasm/$_" }
}

Write-Host ""
Write-Host "=== JAVA VERSION ==="
try { java -version 2>&1 } catch { Write-Host "  java: NOT FOUND" }
try { javac -version 2>&1 } catch { Write-Host "  javac: NOT FOUND" }

Write-Host ""
Write-Host "=== GRADLE ==="
if (Test-Path "upstream\gradlew.bat") { Write-Host "  gradlew.bat: EXISTS" }
else { Write-Host "  gradlew.bat: MISSING" }

Write-Host ""
Write-Host "=== NODE ==="
try { node --version } catch { Write-Host "  node: NOT FOUND" }
if (Test-Path "package.json") { Write-Host "  package.json: EXISTS" }
else { Write-Host "  package.json: MISSING" }

Write-Host ""
Write-Host "=== QUICK JAVA COMPILATION TEST ==="
# Check if any Java source compiles
$testFile = "test_compile.java"
@"
public class $testFile {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
"@ | Out-File -FilePath $testFile -Encoding UTF8
try {
    $result = javac $testFile 2>&1
    if ($LASTEXITCODE -eq 0) { Write-Host "  javac: WORKS" } else { Write-Host "  javac: FAILED - $result" }
} catch { Write-Host "  javac: ERROR - $_" }
if (Test-Path $testFile) { Remove-Item $testFile -ErrorAction SilentlyContinue }
if (Test-Path "$testFile.class") { Remove-Item "$testFile.class" -ErrorAction SilentlyContinue }
