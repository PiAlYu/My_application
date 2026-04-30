param(
    [int]$Port = 8787
)

$ErrorActionPreference = "Stop"

Write-Host "Store Checklist PWA local server"
Write-Host "Folder: $PSScriptRoot"
Write-Host "URL:    http://127.0.0.1:$Port"
Write-Host "Press Ctrl+C to stop."

Push-Location $PSScriptRoot
try {
    $commands = @(
        "py -3 -m http.server $Port",
        "python -m http.server $Port"
    )

    foreach ($command in $commands) {
        Write-Host "Trying: $command"
        $process = Start-Process `
            -FilePath "cmd.exe" `
            -ArgumentList "/c $command" `
            -NoNewWindow `
            -Wait `
            -PassThru

        if ($process.ExitCode -eq 0) {
            return
        }
    }

    throw "Не удалось запустить локальный сервер. Установите Python 3 (команда py или python)."
}
finally {
    Pop-Location
}
