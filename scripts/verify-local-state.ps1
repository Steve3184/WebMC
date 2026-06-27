[CmdletBinding()]
param(
    [switch]$AllowReports,
    [switch]$AllowProofArtifacts
)

$ErrorActionPreference = 'Stop'

function Resolve-RepoRoot {
    $root = Resolve-Path (Join-Path $PSScriptRoot '..')
    return $root.Path
}

function Resolve-RepoPath {
    param(
        [string]$RepoRoot,
        [string]$RelativePath
    )
    return [System.IO.Path]::GetFullPath((Join-Path $RepoRoot $RelativePath))
}

function Test-Exists {
    param(
        [string]$RepoRoot,
        [string]$RelativePath
    )
    $target = Resolve-RepoPath -RepoRoot $RepoRoot -RelativePath $RelativePath
    return (Test-Path -LiteralPath $target)
}

function Read-ProofArtifactAllowlist {
    param(
        [string]$RepoRoot
    )

    $allowlistPath = Resolve-RepoPath -RepoRoot $RepoRoot -RelativePath 'scripts/proof-artifacts.json'
    if (-not (Test-Path -LiteralPath $allowlistPath)) {
        return @()
    }
    $json = Get-Content -LiteralPath $allowlistPath -Raw | ConvertFrom-Json
    return @($json.proofArtifacts | ForEach-Object {
        $artifact = ([string]$_).Replace('\', '/')
        if ([string]::IsNullOrWhiteSpace($artifact)) {
            throw "Invalid empty proof artifact entry in scripts/proof-artifacts.json"
        }
        if ([System.IO.Path]::IsPathRooted($artifact) -or $artifact.Contains('..')) {
            throw "Invalid proof artifact path: $artifact"
        }
        if (-not $artifact.StartsWith('output/', [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Proof artifact must be under output/: $artifact"
        }
        $artifact
    })
}

function Convert-ToRepoRelativePath {
    param(
        [string]$RepoRoot,
        [string]$Path
    )

    $root = [System.IO.Path]::GetFullPath($RepoRoot).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    if (-not $fullPath.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $fullPath
    }
    return $fullPath.Substring($root.Length).Replace('\', '/')
}

function Invoke-LocalVerifier {
    param(
        [string]$Label,
        [scriptblock]$Command
    )

    Write-Host "[verify-local] $Label"
    try {
        & $Command
        if ($LASTEXITCODE -ne 0) {
            $failures.Add("${Label} failed with exit code $LASTEXITCODE")
        }
    } catch {
        $failures.Add("${Label} failed: $($_.Exception.Message)")
    }
}

function Get-UnexpectedOutputArtifacts {
    param(
        [string]$RepoRoot,
        [string[]]$AllowedProofArtifacts
    )

    $outputDir = Resolve-RepoPath -RepoRoot $RepoRoot -RelativePath 'output'
    if (-not (Test-Path -LiteralPath $outputDir)) {
        return @()
    }

    $allowed = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($artifact in $AllowedProofArtifacts) {
        [void]$allowed.Add($artifact.Replace('\', '/'))
    }

    $unexpected = New-Object System.Collections.Generic.List[string]
    Get-ChildItem -LiteralPath $outputDir -Recurse -File |
        ForEach-Object {
            $relative = Convert-ToRepoRelativePath -RepoRoot $RepoRoot -Path $_.FullName
            if (-not $allowed.Contains($relative)) {
                $unexpected.Add($relative)
            }
        }
    return @($unexpected)
}

$repoRoot = Resolve-RepoRoot
Set-Location -LiteralPath $repoRoot

$failures = New-Object System.Collections.Generic.List[string]
$allowedProofArtifacts = Read-ProofArtifactAllowlist -RepoRoot $repoRoot
if ($AllowProofArtifacts -and $allowedProofArtifacts.Count -eq 0) {
    throw "AllowProofArtifacts requires a non-empty scripts/proof-artifacts.json allowlist"
}

$artifactPaths = @(
    '.claude',
    '.workbuddy',
    '.cursorrules',
    '.antigravityignore',
    'chrome_user_data',
    'data',
    'log',
    'output',
    'config.toml',
    'DownloadWrapper.class',
    'dump.rdb',
    'web/webmc.js',
    'web/manifest.json',
    'web/vfs.tar.xz',
    'addons/teavm-runtime/bin'
)

foreach ($path in $artifactPaths) {
    if (Test-Exists -RepoRoot $repoRoot -RelativePath $path) {
        if ($path -eq 'output' -and $AllowProofArtifacts) {
            $unexpectedOutputArtifacts = Get-UnexpectedOutputArtifacts -RepoRoot $repoRoot -AllowedProofArtifacts $allowedProofArtifacts
            if ($unexpectedOutputArtifacts.Count -eq 0) {
                continue
            }
            $preview = ($unexpectedOutputArtifacts | Select-Object -First 8) -join ', '
            $failures.Add("local artifact exists: output ($($unexpectedOutputArtifacts.Count) unexpected files; first: $preview)")
            continue
        }
        $failures.Add("local artifact exists: $path")
    }
}

$reportsDir = Resolve-RepoPath -RepoRoot $repoRoot -RelativePath 'docs/reports'
if (Test-Path -LiteralPath $reportsDir) {
    $extraReports = Get-ChildItem -LiteralPath $reportsDir -File |
        Where-Object { $_.Name -ne 'README.md' }
    if (-not $AllowReports) {
        foreach ($report in $extraReports) {
            $failures.Add("report artifact exists: docs/reports/$($report.Name)")
        }
    }
}

$docFiles = @(
    'README.md',
    'ARCHITECTURE.md',
    'docs/DEVELOPMENT.md',
    'docs/PROJECT_STATUS.md',
    'docs/PRODUCT_READINESS.md'
)
$forbiddenDocPattern = '(?i)\b(as an ai|chatgpt|copilot|codex|claude|assistant|llm|gpt)\b'

foreach ($doc in $docFiles) {
    $docPath = Resolve-RepoPath -RepoRoot $repoRoot -RelativePath $doc
    if (-not (Test-Path -LiteralPath $docPath)) {
        $failures.Add("required doc missing: $doc")
        continue
    }
    $matches = Select-String -Path $docPath -Pattern $forbiddenDocPattern
    foreach ($match in $matches) {
        $failures.Add("forbidden phrase in ${doc}:$($match.LineNumber)")
    }
}

Invoke-LocalVerifier -Label 'verify:glsl-float-int' -Command {
    & node (Resolve-RepoPath -RepoRoot $repoRoot -RelativePath 'scripts/verify-glsl-float-int-scanner.cjs')
}

$clickSmoothBaseline = 'output/playwright/main-menu-click-smoothness-glsl-translator-float-int-scanner-normal-20260609.json'
$clickSmoothRerun = 'output/playwright/main-menu-click-smoothness-glsl-translator-float-int-scanner-normal-rerun-20260609.json'
$clickSmoothComparison = 'output/playwright/main-menu-click-smoothness-glsl-translator-float-int-scanner-normal-comparison-20260609.json'

if (
    (Test-Exists -RepoRoot $repoRoot -RelativePath $clickSmoothBaseline) -and
    (Test-Exists -RepoRoot $repoRoot -RelativePath $clickSmoothRerun)
) {
    Invoke-LocalVerifier -Label 'compare:click-smoothness' -Command {
        & node `
            (Resolve-RepoPath -RepoRoot $repoRoot -RelativePath 'scripts/compare-main-menu-click-smoothness.cjs') `
            (Resolve-RepoPath -RepoRoot $repoRoot -RelativePath $clickSmoothBaseline) `
            (Resolve-RepoPath -RepoRoot $repoRoot -RelativePath $clickSmoothRerun) `
            '--label' `
            'glsl-translator-float-int-scanner normal proof vs rerun' `
            '--out' `
            (Resolve-RepoPath -RepoRoot $repoRoot -RelativePath $clickSmoothComparison)
    }
} else {
    Write-Host '[verify-local] compare:click-smoothness skipped (proof reports missing)'
}

if ($failures.Count -gt 0) {
    Write-Host '[verify-local] FAIL'
    foreach ($failure in $failures) {
        Write-Host " - $failure"
    }
    exit 1
}

Write-Host '[verify-local] PASS'
