param(
    [string]$ApiKey,
    [string]$ForwardTo = 'http://localhost:8080/webhooks/stripe',
    [string]$Events = 'checkout.session.completed,invoice.payment_succeeded,invoice.payment_failed',
    [switch]$PrintSecretOnly
)

$ErrorActionPreference = 'Stop'

function Resolve-StripeCliPath {
    $command = Get-Command stripe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $wingetPath = Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages\Stripe.StripeCli_Microsoft.Winget.Source_8wekyb3d8bbwe\stripe.exe'
    if (Test-Path $wingetPath) {
        return $wingetPath
    }

    throw 'Stripe CLI not found. Install it first with: winget install --id Stripe.StripeCli --accept-package-agreements --accept-source-agreements'
}

function Resolve-ApiKey {
    param(
        [string]$ProvidedApiKey,
        [string]$RepoRoot
    )

    if ($ProvidedApiKey) {
        return $ProvidedApiKey.Trim()
    }

    if ($env:STRIPE_API_KEY) {
        return $env:STRIPE_API_KEY.Trim()
    }

    $localSecretsPath = Join-Path $RepoRoot 'config\local-secrets.yml'
    if (-not (Test-Path $localSecretsPath)) {
        throw 'Stripe API key is missing. Pass -ApiKey, set STRIPE_API_KEY, or add stripe.api.key to config/local-secrets.yml'
    }

    $content = Get-Content -Path $localSecretsPath -Raw -Encoding UTF8
    $match = [regex]::Match($content, '(?ms)^\s*stripe\s*:\s*$.*?^\s*api\s*:\s*$.*?^\s*key\s*:\s*["'']?(?<value>[^"''\r\n#]+)')
    if (-not $match.Success) {
        throw 'stripe.api.key was not found in config/local-secrets.yml'
    }

    return $match.Groups['value'].Value.Trim()
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$stripeExe = Resolve-StripeCliPath
$resolvedApiKey = Resolve-ApiKey -ProvidedApiKey $ApiKey -RepoRoot $repoRoot

$arguments = @(
    'listen'
    '--api-key'
    $resolvedApiKey
)

if ($PrintSecretOnly) {
    $arguments += '--print-secret'
} else {
    $arguments += @(
        '--events'
        $Events
        '--forward-to'
        $ForwardTo
    )
}

Write-Host "Using Stripe CLI: $stripeExe"
if ($PrintSecretOnly) {
    Write-Host 'Printing webhook secret only...'
} else {
    Write-Host "Forwarding Stripe events to: $ForwardTo"
    Write-Host "Events: $Events"
}

& $stripeExe @arguments