param(
    [string]$Gateway = 'http://localhost:19080',
    [string]$Keycloak = 'http://localhost:18180',
    [string]$User = 'demo',
    [string]$Password = 'demo123'
)
# Ajoute des clients, paiements et virements de demonstration via la gateway.
# Les bases H2 sont en memoire : relancer ce script apres chaque redemarrage.
$ErrorActionPreference = 'Stop'

# Avec Docker, la gateway exige un jeton Keycloak ; avec LANCER.ps1, Keycloak est absent.
$headers = @{}
try {
    $token = Invoke-RestMethod -Method POST "$Keycloak/realms/adria-bank/protocol/openid-connect/token" -TimeoutSec 5 `
        -Body @{ grant_type = 'password'; client_id = 'adria-cli'; username = $User; password = $Password }
    $headers.Authorization = "Bearer $($token.access_token)"
    Write-Host "Connecte a Keycloak en tant que $User."
} catch { Write-Host 'Keycloak indisponible : appels sans jeton.' }

function Invoke-Bank([string]$method, [string]$path, $body) {
    $params = @{ Method = $method; Uri = "$Gateway$path"; ContentType = 'application/json'; TimeoutSec = 15; Headers = $headers }
    if ($body) { $params.Body = ($body | ConvertTo-Json -Compress) }
    # PowerShell 5.1 renvoie un tableau JSON comme un seul objet : on le deroule.
    Invoke-RestMethod @params | ForEach-Object { $_ }
}

$accounts = @()
for ($i = 0; $i -lt 20 -and -not $accounts; $i++) {
    try { $accounts = @(Invoke-Bank GET '/api/accounts') } catch { Start-Sleep -Seconds 3 }
}
if (-not $accounts) { throw "La gateway ne repond pas sur $Gateway. Demarrer l'application d'abord." }
if ($accounts | Where-Object customerReference -eq 'CLIENT-004') {
    Write-Host 'Donnees de demonstration deja presentes.'
    return
}

$clients = @(
    @{ customerReference = 'CLIENT-004'; currency = 'MAD'; balance = 8000.00 },
    @{ customerReference = 'CLIENT-005'; currency = 'MAD'; balance = 3200.00 },
    @{ customerReference = 'CLIENT-006'; currency = 'MAD'; balance = 12500.00 },
    @{ customerReference = 'CLIENT-007'; currency = 'EUR'; balance = 2400.00 },
    @{ customerReference = 'CLIENT-008'; currency = 'EUR'; balance = 950.00 },
    @{ customerReference = 'CLIENT-009'; currency = 'USD'; balance = 4100.00 },
    @{ customerReference = 'CLIENT-010'; currency = 'USD'; balance = 1800.00 }
)
foreach ($client in $clients) { $accounts += Invoke-Bank POST '/api/accounts' $client }
Write-Host "$($clients.Count) clients crees."
$byRef = @{}
foreach ($a in $accounts) { $byRef[$a.customerReference] = $a }

$payments = @(
    @('CLIENT-001', 320.50, 'SHOPPING', 'MA'), @('CLIENT-001', 85.00, 'FOOD', 'MA'),
    @('CLIENT-001', 45.00, 'TRANSPORT', 'MA'), @('CLIENT-002', 199.99, 'SHOPPING', 'MA'),
    @('CLIENT-002', 62.30, 'FOOD', 'MA'), @('CLIENT-003', 74.90, 'FOOD', 'FR'),
    @('CLIENT-003', 150.00, 'SHOPPING', 'FR'), @('CLIENT-004', 899.00, 'SHOPPING', 'MA'),
    @('CLIENT-004', 120.00, 'TRANSPORT', 'MA'), @('CLIENT-005', 240.00, 'OTHER', 'MA'),
    @('CLIENT-006', 975.00, 'SHOPPING', 'FR'), @('CLIENT-006', 38.50, 'FOOD', 'MA'),
    @('CLIENT-007', 59.00, 'TRANSPORT', 'FR'), @('CLIENT-008', 210.00, 'SHOPPING', 'FR'),
    @('CLIENT-009', 430.00, 'SHOPPING', 'US'), @('CLIENT-009', 27.80, 'FOOD', 'US'),
    @('CLIENT-010', 95.00, 'TRANSPORT', 'US')
)
foreach ($p in $payments) {
    $a = $byRef[$p[0]]
    $null = Invoke-Bank POST '/api/transactions' @{ accountId = $a.id; amount = $p[1]; currency = $a.currency; merchantCategory = $p[2]; country = $p[3] }
}
Write-Host "$($payments.Count) paiements crees."

$transfers = @(
    @('CLIENT-001', 'CLIENT-002', 500.00), @('CLIENT-006', 'CLIENT-004', 1000.00),
    @('CLIENT-004', 'CLIENT-005', 250.00), @('CLIENT-002', 'CLIENT-001', 150.00),
    @('CLIENT-007', 'CLIENT-003', 300.00), @('CLIENT-009', 'CLIENT-010', 400.00)
)
foreach ($t in $transfers) {
    $source = $byRef[$t[0]]
    $null = Invoke-Bank POST '/api/transactions/transfer' @{ sourceAccountId = $source.id; targetAccountId = $byRef[$t[1]].id; amount = $t[2]; currency = $source.currency }
}
Write-Host "$($transfers.Count) virements crees."

$null = Invoke-Bank PATCH "/api/accounts/$($byRef['CLIENT-008'].id)/status" @{ status = 'BLOCKED' }
Write-Host 'CLIENT-008 bloque (pour tester les refus).'
