param([switch]$WithAi, [switch]$WithKafka, [switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
Set-Location -LiteralPath $projectRoot

foreach ($command in @('java', 'mvn', 'node', 'npm')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "Commande manquante : $command. Installer Java 17+, Maven 3.9+ et Node.js 22.12+ ou 24 (voir README.md)."
    }
}

$ports = @(19761, 19888, 19081, 19082, 19080, 15173)
if ($WithAi) { $ports += 19083 }
if ($WithKafka) { $ports += 19084 }
$occupied = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in $ports }
if ($occupied) {
    $portList = ($occupied.LocalPort | Sort-Object -Unique) -join ', '
    throw "Ports deja occupes : $portList. Arreter l'ancienne instance avec son script ARRETER.ps1 avant de lancer cette copie."
}

$keyFile = Join-Path $projectRoot '.local/gemini-key.txt'
if ($WithAi -and -not $env:GEMINI_API_KEY -and -not (Test-Path -LiteralPath $keyFile)) {
    throw 'Definir GEMINI_API_KEY avant de lancer avec -WithAi (voir README.md).'
}

$frontendDir = Join-Path $projectRoot 'frontend'
$viteScript = Join-Path $frontendDir 'node_modules/vite/bin/vite.js'
Push-Location -LiteralPath $frontendDir
try {
    if (-not $SkipBuild -or -not (Test-Path -LiteralPath $viteScript)) {
        & npm ci --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'Installation des dependances React impossible.' }
    }
} finally { Pop-Location }

& (Join-Path $projectRoot 'scripts/start-local.ps1') -SkipBuild:$SkipBuild -WithAi:$WithAi -WithKafka:$WithKafka
Set-Location -LiteralPath $projectRoot
$localDir = Join-Path $projectRoot '.local'
$process = Start-Process -FilePath (Get-Command node).Source -ArgumentList @("`"$viteScript`"", '--host', '127.0.0.1') -WorkingDirectory $frontendDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $localDir 'frontend.log') -RedirectStandardError (Join-Path $localDir 'frontend.err.log')
@{ pid = $process.Id; jar = $viteScript } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $localDir 'frontend.process.json')

$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    if ($process.HasExited) { throw 'Le frontend est arrete. Voir .local/frontend.err.log.' }
    try {
        $response = Invoke-WebRequest 'http://127.0.0.1:15173' -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw 'Le frontend ne repond pas. Voir .local/frontend.log.' }
Write-Host ''
Write-Host 'Application prete : http://localhost:15173'
Write-Host 'Pour tout arreter : .\ARRETER.ps1'
