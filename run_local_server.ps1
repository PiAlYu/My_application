$ErrorActionPreference = "Stop"

if (Get-Command py -ErrorAction SilentlyContinue) {
    py -3 .\local_server\server.py --host 0.0.0.0 --port 8080
    exit $LASTEXITCODE
}

if (Get-Command python -ErrorAction SilentlyContinue) {
    python .\local_server\server.py --host 0.0.0.0 --port 8080
    exit $LASTEXITCODE
}

Write-Error "Python 3 is not installed or not in PATH."
