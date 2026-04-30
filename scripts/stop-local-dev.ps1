param()

$ErrorActionPreference = 'SilentlyContinue'

$repoRoot = Split-Path -Parent $PSScriptRoot
$frontendRoot = Join-Path $repoRoot 'gym24h-frontend'
$stoppedProcessIds = [System.Collections.Generic.HashSet[int]]::new()

function Write-GreenStatus {
    param(
        [string]$Message
    )

    Write-Host $Message -ForegroundColor Green
}

function Get-MatchingProcesses {
    param(
        [string[]]$ProcessNames,
        [string[]]$CommandLinePatterns
    )

    $processes = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -in $ProcessNames }
    if (-not $processes) {
        return @()
    }

    return @(
        $processes | Where-Object {
            $commandLine = $_.CommandLine
            if (-not $commandLine) {
                return $false
            }

            foreach ($pattern in $CommandLinePatterns) {
                if ($commandLine -like $pattern) {
                    return $true
                }
            }

            return $false
        }
    )
}

function Stop-ProcessGroup {
    param(
        [string]$Label,
        [string[]]$ProcessNames,
        [string[]]$CommandLinePatterns
    )

    $processes = Get-MatchingProcesses -ProcessNames $ProcessNames -CommandLinePatterns $CommandLinePatterns |
        Sort-Object ProcessId -Unique

    $stoppedCount = 0
    foreach ($process in $processes) {
        if ($stoppedProcessIds.Contains([int]$process.ProcessId)) {
            continue
        }

        try {
            Stop-Process -Id $process.ProcessId -Force -ErrorAction Stop
            $null = $stoppedProcessIds.Add([int]$process.ProcessId)
            $stoppedCount++
        } catch {
        }
    }

    if ($stoppedCount -gt 0) {
        Write-GreenStatus "[OK] $Label processes cleaned ($stoppedCount)"
        return
    }

    Write-GreenStatus "[OK] No $Label processes found"
}

Stop-ProcessGroup -Label 'backend' -ProcessNames @('java.exe', 'cmd.exe', 'powershell.exe') -CommandLinePatterns @(
    "*$($repoRoot)*bootRun --args='--spring.profiles.active=local'*",
    "*$($repoRoot)*bootRun --args=--spring.profiles.active=local*",
    "*$($repoRoot)*org.gradle.launcher.GradleMain bootRun*",
    "*$($repoRoot)*build\\classes\\java\\main*",
    "*$($repoRoot)*org.gradle.launcher.daemon.bootstrap.GradleDaemon*"
)

Stop-ProcessGroup -Label 'frontend' -ProcessNames @('node.exe', 'cmd.exe', 'powershell.exe') -CommandLinePatterns @(
    "*$($frontendRoot)*npm run dev -- --host 0.0.0.0*",
    "*$($frontendRoot)*vite*",
    "*$($frontendRoot)*--host 0.0.0.0*"
)

Stop-ProcessGroup -Label 'iot-mock' -ProcessNames @('node.exe', 'powershell.exe', 'cmd.exe') -CommandLinePatterns @(
    "*$($repoRoot)*scripts\\mock-iot-server.js*",
    '*mock-iot-server.js*',
    '*localhost:8081/mock/door-lock/unlock*'
)

Stop-ProcessGroup -Label 'stripe' -ProcessNames @('stripe.exe', 'powershell.exe') -CommandLinePatterns @(
    '*start-stripe-listener.ps1*',
    '*stripe listen*--forward-to http://localhost:8080/webhooks/stripe*'
)

Stop-ProcessGroup -Label 'ngrok' -ProcessNames @('ngrok.exe', 'cmd.exe') -CommandLinePatterns @(
    '*ngrok.exe http 8080*',
    '*\\ngrok.exe*'
)

Stop-ProcessGroup -Label 'cloudflare' -ProcessNames @('cloudflared.exe', 'node.exe', 'cmd.exe') -CommandLinePatterns @(
    '*cloudflared*',
    '*untun@latest tunnel http://localhost:5173*',
    '*tunnel http://localhost:5173*'
)

Write-GreenStatus '[OK] Local development environment stopped'