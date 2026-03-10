# =============================================================
# set-github-secrets.ps1 — Configure GitHub Actions secrets
#
# Sets all secrets required by .github/workflows/deploy.yml and
# ci-cd.yml so CI/CD auto-deploys to AWS on every push to main.
#
# PREREQUISITES:
#   1. GitHub CLI installed: https://cli.github.com/
#      winget install GitHub.cli     (Windows)
#   2. Authenticated: gh auth login
#   3. Run AFTER provision-aws.ps1 (you'll need the EC2 Elastic IP)
#   4. Have your Docker Hub access token ready
#      (https://hub.docker.com -> Account Settings -> Security -> New Token)
#   5. Have your SSH private key file (.pem) from AWS key pair
#
# USAGE:
#   .\set-github-secrets.ps1 `
#     -DeployHost     "13.234.56.78" `
#     -DeployUser     "ubuntu" `
#     -SshKeyPath     "C:\path\to\brightnest-key.pem" `
#     -DockerHubUser  "yourdockerhubusername" `
#     -DockerHubToken "dckr_pat_xxxxxxxx"
#
# OPTIONAL — prod application secrets (stored as GitHub secrets,
# injected into EC2 .env by deploy.yml if you extend the workflow):
#   -JwtSecret          (auto-generated if not provided)
#   -ProdAdminEmail
#   -ProdAdminPassword
#   -RdsHost
#   -RdsPassword
# =============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$DeployHost,

    [Parameter(Mandatory=$true)]
    [string]$DeployUser,

    [Parameter(Mandatory=$true)]
    [string]$SshKeyPath,

    [Parameter(Mandatory=$true)]
    [string]$DockerHubUser,

    [Parameter(Mandatory=$true)]
    [string]$DockerHubToken,

    [string]$DeployPort         = "22",
    [string]$AwsRegion          = "ap-south-1",

    # Optional prod secrets (stored in GitHub for reference / future automation)
    [string]$JwtSecret          = "",
    [string]$JwtRefreshPepper   = "",
    [string]$ProdAdminEmail     = "",
    [string]$ProdAdminPassword  = "",
    [string]$RdsHost            = "",
    [string]$RdsPassword        = "",
    [string]$RedisHost          = "",
    [string]$CorsOrigins        = "https://brightnest-academy.com,https://www.brightnest-academy.com",

    # GitHub repo (auto-detected from git remote if not specified)
    [string]$GithubRepo         = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# --------------------------------------------------------
# 0) Check GitHub CLI
# --------------------------------------------------------
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Error @"
GitHub CLI (gh) not found.
Install it:  winget install GitHub.cli
Then auth:   gh auth login
"@
    exit 1
}

$authStatus = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "GitHub CLI not authenticated. Run: gh auth login"
    exit 1
}

# Auto-detect repo from git remote
if ([string]::IsNullOrWhiteSpace($GithubRepo)) {
    $remoteUrl = git remote get-url origin 2>$null
    if ($remoteUrl -match "github\.com[:/]([^/]+/[^/]+?)(?:\.git)?$") {
        $GithubRepo = $Matches[1]
    } else {
        Write-Error "Could not detect GitHub repo. Pass -GithubRepo owner/name"
        exit 1
    }
}
Write-Host "Target repo: $GithubRepo" -ForegroundColor Cyan

# --------------------------------------------------------
# 1) Read SSH private key
# --------------------------------------------------------
if (-not (Test-Path $SshKeyPath)) {
    Write-Error "SSH key not found: $SshKeyPath"
    exit 1
}
$sshKey = Get-Content $SshKeyPath -Raw
if (-not $sshKey.Contains("BEGIN")) {
    Write-Error "File does not look like a PEM private key: $SshKeyPath"
    exit 1
}
Write-Host "SSH key loaded from: $SshKeyPath"

# --------------------------------------------------------
# 2) Generate secrets if not provided
# --------------------------------------------------------
function New-RandomSecret([int]$bytes = 48) {
    $randomBytes = New-Object byte[] $bytes
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($randomBytes)
    return [Convert]::ToBase64String($randomBytes)
}

if ([string]::IsNullOrWhiteSpace($JwtSecret)) {
    $JwtSecret = New-RandomSecret 64
    Write-Host "  JWT_SECRET auto-generated (64 random bytes)" -ForegroundColor Yellow
}
if ([string]::IsNullOrWhiteSpace($JwtRefreshPepper)) {
    $JwtRefreshPepper = New-RandomSecret 32
    Write-Host "  JWT_REFRESH_PEPPER auto-generated" -ForegroundColor Yellow
}

# --------------------------------------------------------
# 3) Define all secrets to set
# --------------------------------------------------------
$secrets = [ordered]@{
    # CI/CD — Docker Hub
    "DOCKERHUB_USERNAME"   = $DockerHubUser
    "DOCKERHUB_TOKEN"      = $DockerHubToken

    # CI/CD — SSH deploy target
    "DEPLOY_HOST"          = $DeployHost
    "DEPLOY_USER"          = $DeployUser
    "DEPLOY_SSH_KEY"       = $sshKey
    "DEPLOY_PORT"          = $DeployPort
}

# Add optional prod secrets if provided
if (-not [string]::IsNullOrWhiteSpace($JwtSecret))         { $secrets["PROD_JWT_SECRET"]          = $JwtSecret }
if (-not [string]::IsNullOrWhiteSpace($JwtRefreshPepper))  { $secrets["PROD_JWT_REFRESH_PEPPER"]   = $JwtRefreshPepper }
if (-not [string]::IsNullOrWhiteSpace($ProdAdminEmail))    { $secrets["PROD_ADMIN_EMAIL"]          = $ProdAdminEmail }
if (-not [string]::IsNullOrWhiteSpace($ProdAdminPassword)) { $secrets["PROD_ADMIN_PASSWORD"]       = $ProdAdminPassword }
if (-not [string]::IsNullOrWhiteSpace($RdsHost))           { $secrets["PROD_DB_HOST"]              = $RdsHost }
if (-not [string]::IsNullOrWhiteSpace($RdsPassword))       { $secrets["PROD_DB_PASS"]              = $RdsPassword }
if (-not [string]::IsNullOrWhiteSpace($RedisHost))         { $secrets["PROD_REDIS_HOST"]           = $RedisHost }
if (-not [string]::IsNullOrWhiteSpace($CorsOrigins))       { $secrets["PROD_CORS_ORIGINS"]         = $CorsOrigins }
if (-not [string]::IsNullOrWhiteSpace($AwsRegion))         { $secrets["AWS_REGION"]                = $AwsRegion }

# --------------------------------------------------------
# 4) Set each secret via gh CLI
# --------------------------------------------------------
Write-Host "`nSetting $($secrets.Count) secrets on $GithubRepo..." -ForegroundColor Cyan

$success = 0
$failed  = 0

foreach ($entry in $secrets.GetEnumerator()) {
    $name  = $entry.Key
    $value = $entry.Value

    if ([string]::IsNullOrWhiteSpace($value)) {
        Write-Host "  SKIP  $name (empty)" -ForegroundColor DarkGray
        continue
    }

    try {
        # gh secret set reads the value from stdin to avoid shell history leaks
        $value | gh secret set $name --repo $GithubRepo 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            # Mask sensitive values in output
            $display = if ($name -match "TOKEN|SECRET|PASSWORD|KEY|PASS") { "***" } else { $value }
            Write-Host "  OK    $name = $display" -ForegroundColor Green
            $success++
        } else {
            Write-Host "  FAIL  $name" -ForegroundColor Red
            $failed++
        }
    } catch {
        Write-Host "  ERROR $name : $_" -ForegroundColor Red
        $failed++
    }
}

# --------------------------------------------------------
# 5) Verify secrets are registered
# --------------------------------------------------------
Write-Host "`nVerifying secrets..." -ForegroundColor Cyan
$registered = gh secret list --repo $GithubRepo --json name --jq ".[].name" 2>/dev/null
Write-Host $registered

# --------------------------------------------------------
# Done
# --------------------------------------------------------
Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  GitHub Secrets Setup COMPLETE" -ForegroundColor Green
Write-Host "  Set: $success   Failed: $failed" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""
Write-Host "CI/CD is now fully configured." -ForegroundColor Green
Write-Host "Push any commit to main to trigger an automatic deploy to EC2:"
Write-Host "  git push origin main"
Write-Host ""
Write-Host "Monitor the deployment at:"
Write-Host "  https://github.com/$GithubRepo/actions"
Write-Host ""

if (-not [string]::IsNullOrWhiteSpace($JwtSecret) -and $secrets.ContainsKey("PROD_JWT_SECRET")) {
    Write-Host "IMPORTANT: Save these generated secrets — they cannot be retrieved from GitHub:" -ForegroundColor Yellow
    Write-Host "  JWT_SECRET       : $JwtSecret"
    if (-not [string]::IsNullOrWhiteSpace($JwtRefreshPepper)) {
        Write-Host "  JWT_REFRESH_PEPPER: $JwtRefreshPepper"
    }
    Write-Host ""
    Write-Host "Add them to /opt/brightnest/.env on the EC2 server as JWT_SECRET and JWT_REFRESH_PEPPER." -ForegroundColor Yellow
}
