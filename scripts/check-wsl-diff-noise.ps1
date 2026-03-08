param(
    [string]$RepoPath = "C:\Users\ASUS\Documents\servers\remote\source\Pufferfish"
)

$resolvedRepo = (Resolve-Path $RepoPath).Path
$wslRepo = "/mnt/" + $resolvedRepo.Substring(0, 1).ToLower() + $resolvedRepo.Substring(2).Replace("\", "/")

$statusLines = & wsl.exe bash -lc "cd '$wslRepo' && git status --porcelain=v1"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to read git status from WSL for $resolvedRepo"
}

if (-not $statusLines) {
    Write-Output "WSL git working tree is clean."
    exit 0
}

$lineEndingOnly = New-Object System.Collections.Generic.List[string]
$realDiff = New-Object System.Collections.Generic.List[string]
$untracked = New-Object System.Collections.Generic.List[string]
$unknown = New-Object System.Collections.Generic.List[string]

foreach ($line in $statusLines) {
    if ([string]::IsNullOrWhiteSpace($line)) {
        continue
    }

    $status = $line.Substring(0, 2)
    $file = $line.Substring(3)

    if ($status -eq "??") {
        $untracked.Add($file)
        continue
    }

    $escapedFile = $file.Replace("'", "'\''")
    & wsl.exe bash -lc "cd '$wslRepo' && git diff --ignore-cr-at-eol --quiet -- '$escapedFile'"
    $diffExit = $LASTEXITCODE

    if ($diffExit -eq 0) {
        $lineEndingOnly.Add($file)
        continue
    }

    & wsl.exe bash -lc "cd '$wslRepo' && git diff --quiet -- '$escapedFile'"
    $rawExit = $LASTEXITCODE

    if ($rawExit -eq 1) {
        $realDiff.Add($file)
    } else {
        $unknown.Add("$file (git diff exit=$diffExit raw=$rawExit)")
    }
}

Write-Output "Repo: $resolvedRepo"
Write-Output "WSL diff classification:"
Write-Output "  line-ending-only: $($lineEndingOnly.Count)"
Write-Output "  real-content-diff: $($realDiff.Count)"
Write-Output "  untracked: $($untracked.Count)"
Write-Output "  unknown: $($unknown.Count)"

if ($lineEndingOnly.Count -gt 0) {
    Write-Output ""
    Write-Output "[line-ending-only]"
    $lineEndingOnly | Sort-Object | ForEach-Object { Write-Output $_ }
}

if ($realDiff.Count -gt 0) {
    Write-Output ""
    Write-Output "[real-content-diff]"
    $realDiff | Sort-Object | ForEach-Object { Write-Output $_ }
}

if ($untracked.Count -gt 0) {
    Write-Output ""
    Write-Output "[untracked]"
    $untracked | Sort-Object | ForEach-Object { Write-Output $_ }
}

if ($unknown.Count -gt 0) {
    Write-Output ""
    Write-Output "[unknown]"
    $unknown | Sort-Object | ForEach-Object { Write-Output $_ }
}
