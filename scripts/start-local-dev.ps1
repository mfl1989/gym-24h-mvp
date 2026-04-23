param(
    [switch]$OpenBrowser
)

$ErrorActionPreference = 'Stop'

function Stop-ListeningProcess {
    param(
        [int]$Port
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $connections) {
        return
    }

    $processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $processIds) {
        if ($processId -and $processId -ne 0) {
            try {
                Stop-Process -Id $processId -Force -ErrorAction Stop
            } catch {
                Write-Warning "Failed to stop PID $processId on port ${Port}: $($_.Exception.Message)"
            }
        }
    }
}

function Wait-HttpOk {
    param(
        [string[]]$Uris,
        [int]$MaxAttempts = 40,
        [int]$DelayMilliseconds = 1000
    )

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        foreach ($uri in $Uris) {
            try {
                $response = Invoke-WebRequest -Uri $uri -Method Get -UseBasicParsing -TimeoutSec 2
                if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                    return $true
                }
            } catch {
            }
        }

        Start-Sleep -Milliseconds $DelayMilliseconds
    }

    return $false
}

function Wait-PortListening {
    param(
        [int]$Port,
        [int]$MaxAttempts = 40,
        [int]$DelayMilliseconds = 1000
    )

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $listening = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($listening) {
            return $true
        }

        Start-Sleep -Milliseconds $DelayMilliseconds
    }

    return $false
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$frontendRoot = Join-Path $repoRoot 'gym24h-frontend'
$gradleBat = Join-Path $repoRoot 'gradle-8.5\bin\gradle.bat'

if (-not (Test-Path $gradleBat)) {
    throw "Gradle not found: $gradleBat"
}

if (-not (Test-Path (Join-Path $frontendRoot 'package.json'))) {
    throw "Frontend package.json not found: $frontendRoot"
}

Write-Host 'Stopping existing local servers on ports 8080 and 5173...'
Stop-ListeningProcess -Port 8080
Stop-ListeningProcess -Port 5173

$backendCommand = "Set-Location '$repoRoot'; & '$gradleBat' bootRun --args='--spring.profiles.active=local'"
$frontendCommand = "Set-Location '$frontendRoot'; npm run dev -- --host 0.0.0.0"

Write-Host 'Starting backend on http://localhost:8080 ...'
$backendProcess = Start-Process -FilePath 'powershell.exe' -ArgumentList '-NoExit', '-Command', $backendCommand -PassThru

if (-not (Wait-HttpOk -Uris @('http://localhost:8080/actuator', 'http://127.0.0.1:8080/actuator'))) {
    throw 'Backend did not become ready on http://localhost:8080'
}

Write-Host 'Starting frontend on http://localhost:5173 ...'
$frontendProcess = Start-Process -FilePath 'powershell.exe' -ArgumentList '-NoExit', '-Command', $frontendCommand -PassThru

if (-not (Wait-PortListening -Port 5173 -MaxAttempts 60)) {
    throw 'Frontend did not start listening on port 5173'
}

if (-not (Wait-HttpOk -Uris @('http://localhost:5173', 'http://127.0.0.1:5173') -MaxAttempts 60)) {
    Write-Warning 'Frontend process is listening on 5173, but HTTP probe did not return in time. You can still try opening http://localhost:5173 manually.'
}

Write-Host ''
Write-Host 'Local dev environment is ready.' -ForegroundColor Green
Write-Host 'C-end:           http://localhost:5173/'
Write-Host 'Admin scanner:   http://localhost:5173/admin/scanner'
Write-Host 'Admin token:     super-secret-admin-key'
Write-Host "Backend PID:     $($backendProcess.Id)"
Write-Host "Frontend PID:    $($frontendProcess.Id)"

if ($OpenBrowser) {
    Start-Process 'http://localhost:5173/'
}