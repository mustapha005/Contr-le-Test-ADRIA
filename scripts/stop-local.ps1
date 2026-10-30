$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localDir = Join-Path $projectRoot '.local'
if (-not (Test-Path -LiteralPath $localDir)) { return }
Get-ChildItem -LiteralPath $localDir -Filter '*.process.json' | ForEach-Object {
    $saved = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
    $running = Get-CimInstance Win32_Process -Filter "ProcessId = $($saved.pid)" -ErrorAction SilentlyContinue
    # Vérifier le chemin du JAR : un PID peut avoir été réutilisé par un autre programme.
    if ($running -and $running.CommandLine -and $running.CommandLine.Contains($saved.jar)) {
        Stop-Process -Id $saved.pid
        Write-Host "Arrêt : $($_.BaseName)"
    }
    Remove-Item -LiteralPath $_.FullName
}
