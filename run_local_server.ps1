param(
    [string]$ReadToken = "",
    [string]$WriteToken = ""
)

$ErrorActionPreference = "Stop"

$serverArgs = @(
    ".\local_server\server.py",
    "--host", "0.0.0.0",
    "--port", "8080"
)

if ($ReadToken) {
    $serverArgs += @("--read-token", $ReadToken)
}

if ($WriteToken) {
    $serverArgs += @("--write-token", $WriteToken)
}

if (Get-Command py -ErrorAction SilentlyContinue) {
    & py -3 @serverArgs
    exit $LASTEXITCODE
}

if (Get-Command python -ErrorAction SilentlyContinue) {
    & python @serverArgs
    exit $LASTEXITCODE
}

Write-Error "Python 3 is not installed or not in PATH."
