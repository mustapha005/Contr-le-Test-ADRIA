param([switch]$SkipBuild, [switch]$WithAi, [switch]$WithKafka)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$localDir = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Path $localDir -Force | Out-Null
if (-not $SkipBuild) {
    & mvn -B -ntp package
    if ($LASTEXITCODE -ne 0) { throw 'La compilation Maven a échoué.' }
}
$keyFile = Join-Path $localDir 'gemini-key.txt'
if ($WithAi -and -not $env:GEMINI_API_KEY -and (Test-Path -LiteralPath $keyFile)) {
    $env:GEMINI_API_KEY = (Get-Content -LiteralPath $keyFile -Raw).Trim()
}
if ($WithAi -and -not $env:GEMINI_API_KEY) { throw 'Définir GEMINI_API_KEY avant de démarrer le chatbot.' }
if ($WithKafka) { $env:KAFKA_ENABLED = 'true' } else { $env:KAFKA_ENABLED = 'false' }

function Start-BankService([string]$name, [int]$port) {
    $jar = Join-Path $projectRoot "backend/$name/target/$name-1.0.0.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw "Fichier absent : $jar. Exécuter mvn package." }
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
        throw "Le port $port est déjà occupé. Arrêter l'ancien service avant de relancer."
    }
    $process = Start-Process -FilePath 'java' -ArgumentList @('-Xms64m', '-Xmx256m', '-jar', "`"$jar`"") -WindowStyle Hidden -PassThru -WorkingDirectory $projectRoot -RedirectStandardOutput (Join-Path $localDir "$name.log") -RedirectStandardError (Join-Path $localDir "$name.err.log")
    @{ pid = $process.Id; jar = $jar } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $localDir "$name.process.json")
    $ready = $false
    for ($i = 0; $i -lt 90; $i++) {
        if ($process.HasExited) { throw "$name s'est arrêté. Voir .local/$name.log et .local/$name.err.log." }
        try {
            $health = Invoke-RestMethod "http://localhost:$port/actuator/health" -TimeoutSec 2
            if ($health.status -eq 'UP') { $ready = $true; break }
        } catch { }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw "$name ne répond pas. Voir .local/$name.log." }
    Write-Host "$name prêt sur le port $port"
}

Start-BankService 'discovery-service' 19761
Start-BankService 'config-service' 19888
Start-BankService 'account-service' 19081
Start-BankService 'transaction-service' 19082
Start-BankService 'gateway-service' 19080
if ($WithKafka) { Start-BankService 'event-consumer' 19084 }
if ($WithAi) { Start-BankService 'chatbot-service' 19083 }
Write-Host 'Backend démarré. Eureka peut prendre environ 30 secondes pour propager les services.'
Write-Host 'Frontend : cd frontend ; npm install ; npm run dev'
Write-Host 'Application : http://localhost:15173'
