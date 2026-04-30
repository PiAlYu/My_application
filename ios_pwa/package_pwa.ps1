param(
    [string]$OutputName = "store-checklist-ios-pwa.zip"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path -LiteralPath $PSScriptRoot
$outputPath = Join-Path -Path (Split-Path -Path $root -Parent) -ChildPath $OutputName

if (Test-Path -LiteralPath $outputPath) {
    Remove-Item -LiteralPath $outputPath -Force
}

$files = @(
    "index.html",
    "styles.css",
    "app.js",
    "manifest.webmanifest",
    "sw.js",
    "README.md",
    "serve_local.ps1",
    "package_pwa.ps1",
    "icons\*"
)

Push-Location $root
try {
    Compress-Archive -Path $files -DestinationPath $outputPath -Force
}
finally {
    Pop-Location
}

Get-Item -LiteralPath $outputPath | Select-Object FullName,Length | Format-Table -AutoSize
