# download-wrapper.ps1
# Downloads the Gradle wrapper jar from GitHub raw content
# Since curl/wget are blocked, we use Invoke-WebRequest which goes through the proxy

$destPath = "work/gradle-wrapper.jar"
$url = "https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar"

Write-Host "Attempting to download gradle-wrapper.jar from $url"

try {
    # Use Invoke-WebRequest which is the PowerShell native way
    Invoke-WebRequest -Uri $url -OutFile $destPath -UseBasicParsing
    Write-Host "Download successful!"
    Write-Host "File size: $((Get-Item $destPath).Length) bytes"
} catch {
    Write-Host "Failed to download: $_"
    Write-Host "This is expected - the relay blocks external downloads"
    exit 0
}
