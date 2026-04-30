param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9]{12}$')]
    [string]$AwsAccountId,

    [Parameter()]
    [string]$AwsRegion = 'ap-northeast-1',

    [Parameter()]
    [string]$RepoName = 'gym24h-backend',

    [Parameter()]
    [string]$ImageTag = 'latest'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Write-Step {
    param(
        [string]$Message
    )

    Write-Host $Message -ForegroundColor Green
}

function Fail {
    param(
        [string]$Message
    )

    Write-Host $Message -ForegroundColor Red
    exit 1
}

function Assert-LastExitCode {
    param(
        [string]$FailureMessage
    )

    if ($LASTEXITCODE -ne 0) {
        Fail $FailureMessage
    }
}

function Resolve-GradleCommand {
    param(
        [string]$RepoRoot
    )

    $wrapperPath = Join-Path $RepoRoot 'gradlew.bat'
    if (Test-Path $wrapperPath) {
        return $wrapperPath
    }

    $bundledGradlePath = Join-Path $RepoRoot 'gradle-8.5\bin\gradle.bat'
    if (Test-Path $bundledGradlePath) {
        return $bundledGradlePath
    }

    Fail "Gradle executable was not found. Expected $wrapperPath or $bundledGradlePath"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleCommand = Resolve-GradleCommand -RepoRoot $repoRoot
$registry = "$AwsAccountId.dkr.ecr.$AwsRegion.amazonaws.com"
$remoteImage = "$registry/$RepoName`:$ImageTag"

foreach ($commandName in @('aws', 'docker')) {
    if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
        Fail "$commandName command was not found. Install it and make sure it is available in PATH."
    }
}

Push-Location $repoRoot

try {
    Write-Step 'Step 1/5: Building executable jar with Gradle bootJar ...'
    & $gradleCommand bootJar
    Assert-LastExitCode 'Gradle bootJar failed. Aborting image build.'

    $jarCandidates = Get-ChildItem -Path (Join-Path $repoRoot 'build\libs') -Filter '*-SNAPSHOT.jar' -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending

    if (-not $jarCandidates) {
        Fail 'No executable jar matching *-SNAPSHOT.jar was found under build/libs after bootJar.'
    }

    Write-Step "Step 2/5: Using ECR registry $registry"

    Write-Step 'Step 3/5: Logging in to Amazon ECR with AWS CLI ...'
    aws ecr get-login-password --region $AwsRegion | docker login --username AWS --password-stdin $registry
    Assert-LastExitCode 'Amazon ECR login failed. Check AWS CLI credentials and Docker login state.'

    Write-Step "Step 4/5: Building Docker image $RepoName`:$ImageTag ..."
    docker build -t "$RepoName`:$ImageTag" .
    Assert-LastExitCode 'Docker build failed. Aborting push.'

    Write-Step "Step 5/5: Tagging and pushing image to $remoteImage ..."
    docker tag "$RepoName`:$ImageTag" $remoteImage
    Assert-LastExitCode 'Docker tag failed. Aborting push.'

    docker push $remoteImage
    Assert-LastExitCode 'Docker push failed. Aborting.'

    Write-Step "Image push completed successfully: $remoteImage"
} catch {
    Fail $_.Exception.Message
} finally {
    Pop-Location
}