[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [switch]$KeepReports,
    [switch]$KeepProofArtifacts
)

$ErrorActionPreference = 'Stop'

function Resolve-RepoRoot {
    $root = Resolve-Path (Join-Path $PSScriptRoot '..')
    return $root.Path
}

function Assert-InRepo {
    param(
        [string]$RepoRoot,
        [string]$TargetPath
    )
    $normalizedRoot = [System.IO.Path]::GetFullPath($RepoRoot).TrimEnd('\') + '\'
    $normalizedTarget = [System.IO.Path]::GetFullPath($TargetPath)
    if (-not $normalizedTarget.StartsWith($normalizedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside repository: $normalizedTarget"
    }
}

function Resolve-RepoPath {
    param(
        [string]$RepoRoot,
        [string]$RelativePath
    )
    return [System.IO.Path]::GetFullPath((Join-Path $RepoRoot $RelativePath))
}

function Assert-WithinBasePath {
    param(
        [string]$BasePath,
        [string]$TargetPath
    )

    $normalizedBase = [System.IO.Path]::GetFullPath($BasePath).TrimEnd('\') + '\'
    $normalizedTarget = [System.IO.Path]::GetFullPath($TargetPath)
    if (-not $normalizedTarget.StartsWith($normalizedBase, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside expected base: $normalizedTarget"
    }
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

function Remove-LocalPath {
    param(
        [string]$RepoRoot,
        [string]$RelativePath,
        [switch]$Recurse
    )
    $target = Join-Path $RepoRoot $RelativePath
    Assert-InRepo -RepoRoot $RepoRoot -TargetPath $target
    if (-not (Test-Path -LiteralPath $target)) {
        return
    }
    if (-not $PSCmdlet.ShouldProcess($RelativePath, 'Remove local artifact')) {
        return
    }
    if ($Recurse) {
        Remove-Item -LiteralPath $target -Recurse -Force
    } else {
        Remove-Item -LiteralPath $target -Force
    }
    Write-Host "[clean-local] removed $RelativePath"
}

function Remove-OutputExceptProofArtifacts {
    param(
        [string]$RepoRoot,
        [string[]]$ProofArtifacts
    )

    $outputDir = Resolve-RepoPath -RepoRoot $RepoRoot -RelativePath 'output'
    Assert-InRepo -RepoRoot $RepoRoot -TargetPath $outputDir
    if (-not (Test-Path -LiteralPath $outputDir)) {
        return
    }

    $allowed = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($artifact in $ProofArtifacts) {
        [void]$allowed.Add($artifact.Replace('\', '/'))
    }

    Get-ChildItem -LiteralPath $outputDir -Recurse -File |
        ForEach-Object {
            $relative = Convert-ToRepoRelativePath -RepoRoot $RepoRoot -Path $_.FullName
            if ($allowed.Contains($relative)) {
                return
            }
            Assert-InRepo -RepoRoot $RepoRoot -TargetPath $_.FullName
            if (-not $PSCmdlet.ShouldProcess($relative, 'Remove local artifact')) {
                return
            }
            Remove-Item -LiteralPath $_.FullName -Force
            Write-Host "[clean-local] removed $relative"
        }

    Get-ChildItem -LiteralPath $outputDir -Recurse -Directory |
        Sort-Object FullName -Descending |
        ForEach-Object {
            if ((Get-ChildItem -LiteralPath $_.FullName -Force | Select-Object -First 1)) {
                return
            }
            $relative = Convert-ToRepoRelativePath -RepoRoot $RepoRoot -Path $_.FullName
            Assert-InRepo -RepoRoot $RepoRoot -TargetPath $_.FullName
            if (-not $PSCmdlet.ShouldProcess($relative, 'Remove empty local artifact directory')) {
                return
            }
            Remove-Item -LiteralPath $_.FullName -Force
            Write-Host "[clean-local] removed $relative"
        }
}

function Remove-StalePlaywrightMcpProfiles {
    param(
        [int]$MinAgeHours = 8
    )

    if ([string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        return
    }

    $profilesRoot = [System.IO.Path]::GetFullPath((Join-Path $env:LOCALAPPDATA 'ms-playwright-mcp'))
    if (-not (Test-Path -LiteralPath $profilesRoot)) {
        return
    }

    $cutoff = (Get-Date).AddHours(-1 * [Math]::Max($MinAgeHours, 1))
    $previousWhatIfPreference = $WhatIfPreference
    $trackedCommandLines = @()
    try {
        $WhatIfPreference = $false
        $trackedCommandLines = @(
            Get-CimInstance Win32_Process -Filter "Name = 'chrome.exe' OR Name = 'msedge.exe' OR Name = 'node.exe'" -ErrorAction SilentlyContinue |
                ForEach-Object { $_.CommandLine }
        )
    } finally {
        $WhatIfPreference = $previousWhatIfPreference
    }

    Get-ChildItem -LiteralPath $profilesRoot -Directory -Filter 'mcp-chrome-*' -ErrorAction SilentlyContinue |
        ForEach-Object {
            $target = $_.FullName
            Assert-WithinBasePath -BasePath $profilesRoot -TargetPath $target
            if ($_.LastWriteTime -gt $cutoff) {
                return
            }

            $inUse = $trackedCommandLines |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and $_.IndexOf($target, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 } |
                Select-Object -First 1
            if ($inUse) {
                return
            }

            $relative = [System.IO.Path]::GetFileName($target)
            if (-not $PSCmdlet.ShouldProcess($target, 'Remove stale Playwright MCP profile')) {
                return
            }
            Remove-Item -LiteralPath $target -Recurse -Force
            Write-Host "[clean-local] removed stale Playwright MCP profile $relative"
        }
}

$repoRoot = Resolve-RepoRoot
Write-Host "[clean-local] repo: $repoRoot"
$proofArtifacts = Read-ProofArtifactAllowlist -RepoRoot $repoRoot
if ($KeepProofArtifacts -and $proofArtifacts.Count -eq 0) {
    throw "KeepProofArtifacts requires a non-empty scripts/proof-artifacts.json allowlist"
}

$dirTargets = @(
    'chrome_user_data',
    'data',
    'log',
    '.workbuddy',
    '.claude',
    'addons/teavm-runtime/bin'
)

$fileTargets = @(
    '.cursorrules',
    '.antigravityignore',
    'config.toml',
    'DownloadWrapper.class',
    'dump.rdb',
    'web/webmc.js',
    'web/manifest.json',
    'web/vfs.tar.xz'
)

foreach ($dir in $dirTargets) {
    Remove-LocalPath -RepoRoot $repoRoot -RelativePath $dir -Recurse
}

if ($KeepProofArtifacts) {
    Remove-OutputExceptProofArtifacts -RepoRoot $repoRoot -ProofArtifacts $proofArtifacts
} else {
    Remove-LocalPath -RepoRoot $repoRoot -RelativePath 'output' -Recurse
}

foreach ($file in $fileTargets) {
    Remove-LocalPath -RepoRoot $repoRoot -RelativePath $file
}

if (-not $KeepReports) {
    $reportsDir = Join-Path $repoRoot 'docs/reports'
    Assert-InRepo -RepoRoot $repoRoot -TargetPath $reportsDir
    if (Test-Path -LiteralPath $reportsDir) {
        Get-ChildItem -LiteralPath $reportsDir -File |
            Where-Object { $_.Name -ne 'README.md' } |
            ForEach-Object {
                if (-not $PSCmdlet.ShouldProcess("docs/reports/$($_.Name)", 'Remove report artifact')) {
                    return
                }
                Remove-Item -LiteralPath $_.FullName -Force
                Write-Host "[clean-local] removed docs/reports/$($_.Name)"
            }
    }
}

Remove-StalePlaywrightMcpProfiles

Write-Host "[clean-local] done"
