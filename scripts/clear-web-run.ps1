# Close browsers and unlock game.js
$ErrorActionPreference = 'SilentlyContinue'

# Kill any browser processes that might have game.js open
$browsers = @('chrome', 'msedge', 'firefox', 'iexplore')
foreach ($b in $browsers) {
    Get-Process $b -ErrorAction SilentlyContinue | Stop-Process -Force
}

# Wait a moment
Start-Sleep -Seconds 2

# Delete locked files
$webRunDir = "M:\Users\l\Desktop\webmc1\work\build\web-run"
if (Test-Path $webRunDir) {
    Remove-Item "$webRunDir\*" -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "Cleared web-run directory"
}

Write-Host "Done"
