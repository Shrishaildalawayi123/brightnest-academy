# =============================================================
# provision-aws.ps1 — Provision BrightNest Academy on AWS
#
# Provisions:
#   - Security Groups (EC2 + RDS)
#   - RDS MySQL 8.0 (db.t3.micro, free-tier eligible)
#   - ElastiCache Redis (cache.t3.micro, optional)
#   - EC2 t3.medium (Ubuntu 22.04) with Docker + Nginx bootstrapped
#   - Elastic IP attached to EC2
#
# PREREQUISITES (run once before this script):
#   1. Install AWS CLI v2: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html
#   2. Configure credentials: aws configure
#      (provide Access Key ID, Secret Access Key, region: ap-south-1)
#   3. Create a key pair in AWS Console -> EC2 -> Key Pairs
#      Download the .pem file and save it securely (needed for SSH)
#
# USAGE:
#   .\provision-aws.ps1 `
#     -KeyPairName "brightnest-key" `
#     -DbPassword  "YourStrongDbPass123!" `
#     -Region      "ap-south-1"
# =============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$KeyPairName,

    [Parameter(Mandatory=$true)]
    [string]$DbPassword,

    [string]$Region      = "ap-south-1",
    [string]$AppName     = "brightnest-academy",
    [string]$InstanceType = "t3.medium",
    [string]$DbInstanceClass = "db.t3.micro",
    [string]$DbName      = "brightnest_academy",
    [string]$DbUser      = "brightnest_app",
    [switch]$ProvisionRedis = $false  # set -ProvisionRedis to also create ElastiCache
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# --------------------------------------------------------
# 0) Check AWS CLI
# --------------------------------------------------------
if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    Write-Error "AWS CLI not found. Install from https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html"
    exit 1
}

$identity = aws sts get-caller-identity --output json 2>&1 | ConvertFrom-Json
if (-not $identity.Account) {
    Write-Error "AWS credentials not configured. Run 'aws configure' first."
    exit 1
}
Write-Host "Logged in as account: $($identity.Account)" -ForegroundColor Green

# --------------------------------------------------------
# 1) Get default VPC and subnets
# --------------------------------------------------------
Write-Host "`n[1/7] Getting default VPC..." -ForegroundColor Cyan
$vpcId = aws ec2 describe-vpcs --region $Region `
    --filters "Name=is-default,Values=true" `
    --query "Vpcs[0].VpcId" --output text
Write-Host "  VPC: $vpcId"

$subnetIds = (aws ec2 describe-subnets --region $Region `
    --filters "Name=vpc-id,Values=$vpcId" `
    --query "Subnets[*].SubnetId" --output text) -split "\s+"
Write-Host "  Subnets: $($subnetIds -join ', ')"

# --------------------------------------------------------
# 2) Security Groups
# --------------------------------------------------------
Write-Host "`n[2/7] Creating security groups..." -ForegroundColor Cyan

# EC2 Security Group
$ec2SgId = aws ec2 create-security-group --region $Region `
    --group-name "${AppName}-ec2-sg" `
    --description "BrightNest EC2 - HTTP/HTTPS/SSH" `
    --vpc-id $vpcId `
    --query "GroupId" --output text

aws ec2 authorize-security-group-ingress --region $Region `
    --group-id $ec2SgId `
    --ip-permissions `
    "IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=0.0.0.0/0,Description=SSH}]" `
    "IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges=[{CidrIp=0.0.0.0/0,Description=HTTP}]" `
    "IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges=[{CidrIp=0.0.0.0/0,Description=HTTPS}]" | Out-Null
Write-Host "  EC2 SG: $ec2SgId"

# RDS Security Group (allow 3306 only from EC2 SG)
$rdsSgId = aws ec2 create-security-group --region $Region `
    --group-name "${AppName}-rds-sg" `
    --description "BrightNest RDS - MySQL from EC2 only" `
    --vpc-id $vpcId `
    --query "GroupId" --output text

aws ec2 authorize-security-group-ingress --region $Region `
    --group-id $rdsSgId `
    --protocol tcp --port 3306 `
    --source-group $ec2SgId | Out-Null
Write-Host "  RDS SG: $rdsSgId"

# --------------------------------------------------------
# 3) RDS Subnet Group
# --------------------------------------------------------
Write-Host "`n[3/7] Creating RDS subnet group..." -ForegroundColor Cyan
$subnetList = ($subnetIds | ForEach-Object { $_ }) -join " "
aws rds create-db-subnet-group --region $Region `
    --db-subnet-group-name "${AppName}-subnet-group" `
    --db-subnet-group-description "BrightNest RDS subnet group" `
    --subnet-ids $subnetIds | Out-Null
Write-Host "  RDS subnet group created"

# --------------------------------------------------------
# 4) RDS MySQL 8.0
# --------------------------------------------------------
Write-Host "`n[4/7] Creating RDS MySQL 8.0 (this takes ~5 minutes)..." -ForegroundColor Cyan
aws rds create-db-instance --region $Region `
    --db-instance-identifier "${AppName}-db" `
    --db-instance-class $DbInstanceClass `
    --engine mysql `
    --engine-version "8.0" `
    --master-username admin `
    --master-user-password $DbPassword `
    --db-name $DbName `
    --vpc-security-group-ids $rdsSgId `
    --db-subnet-group-name "${AppName}-subnet-group" `
    --backup-retention-period 7 `
    --storage-type gp2 `
    --allocated-storage 20 `
    --no-publicly-accessible `
    --deletion-protection `
    --tags "Key=Project,Value=${AppName}" | Out-Null
Write-Host "  RDS instance creation started (status: creating)"

# --------------------------------------------------------
# 5) ElastiCache Redis (optional)
# --------------------------------------------------------
if ($ProvisionRedis) {
    Write-Host "`n[5/7] Creating ElastiCache Redis (cache.t3.micro)..." -ForegroundColor Cyan

    # Redis SG
    $redisSgId = aws ec2 create-security-group --region $Region `
        --group-name "${AppName}-redis-sg" `
        --description "BrightNest ElastiCache Redis - from EC2 only" `
        --vpc-id $vpcId `
        --query "GroupId" --output text
    aws ec2 authorize-security-group-ingress --region $Region `
        --group-id $redisSgId `
        --protocol tcp --port 6379 `
        --source-group $ec2SgId | Out-Null

    # Redis subnet group
    aws elasticache create-cache-subnet-group --region $Region `
        --cache-subnet-group-name "${AppName}-redis-subnet" `
        --cache-subnet-group-description "BrightNest Redis subnet group" `
        --subnet-ids $subnetIds | Out-Null

    aws elasticache create-cache-cluster --region $Region `
        --cache-cluster-id "${AppName}-redis" `
        --cache-node-type "cache.t3.micro" `
        --engine redis `
        --engine-version "7.0" `
        --num-cache-nodes 1 `
        --cache-subnet-group-name "${AppName}-redis-subnet" `
        --security-group-ids $redisSgId `
        --tags "Key=Project,Value=${AppName}" | Out-Null
    Write-Host "  ElastiCache Redis creation started"
} else {
    Write-Host "`n[5/7] Skipping ElastiCache (pass -ProvisionRedis to create it)" -ForegroundColor Yellow
    Write-Host "       Rate limiting will fall back to in-memory mode."
}

# --------------------------------------------------------
# 6) EC2 Ubuntu 22.04 with userdata
# --------------------------------------------------------
Write-Host "`n[6/7] Launching EC2 t3.medium..." -ForegroundColor Cyan

# Get latest Ubuntu 22.04 LTS AMI for the region
$amiId = aws ec2 describe-images --region $Region `
    --owners 099720109477 `
    --filters `
      "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" `
      "Name=state,Values=available" `
    --query "sort_by(Images, &CreationDate)[-1].ImageId" `
    --output text
Write-Host "  AMI: $amiId (Ubuntu 22.04 LTS)"

# Userdata: install this repo's setup-ec2.sh + clone + run it
$userdata = @'
#!/bin/bash
set -euo pipefail

# Install git, clone repo, run bootstrap
apt-get update -qq
apt-get install -y -qq git

# Clone the repo (public)
git clone https://github.com/Shrishaildalawayi123/brightnest-academy.git /opt/repo

# Run the bootstrap script
bash /opt/repo/shrishail-academy/deploy/aws/scripts/setup-ec2.sh

# Touch a marker so we know provisioning ran
touch /opt/brightnest/.provisioned
echo "EC2 userdata complete" >> /var/log/brightnest-provision.log
'@

$userdataB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($userdata))

$instanceId = aws ec2 run-instances --region $Region `
    --image-id $amiId `
    --instance-type $InstanceType `
    --key-name $KeyPairName `
    --security-group-ids $ec2SgId `
    --subnet-id $subnetIds[0] `
    --user-data $userdataB64 `
    --block-device-mappings "DeviceName=/dev/sda1,Ebs={VolumeSize=30,VolumeType=gp3,DeleteOnTermination=true}" `
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${AppName}},{Key=Project,Value=${AppName}}]" `
    --query "Instances[0].InstanceId" --output text
Write-Host "  EC2 instance: $instanceId"

# Allocate and attach Elastic IP
$allocationId = aws ec2 allocate-address --region $Region `
    --domain vpc --query "AllocationId" --output text
Write-Host "  Elastic IP allocated: $allocationId (waiting for instance to be running...)"

aws ec2 wait instance-running --region $Region --instance-ids $instanceId
aws ec2 associate-address --region $Region `
    --instance-id $instanceId --allocation-id $allocationId | Out-Null

$elasticIp = aws ec2 describe-addresses --region $Region `
    --allocation-ids $allocationId `
    --query "Addresses[0].PublicIp" --output text
Write-Host "  Elastic IP: $elasticIp" -ForegroundColor Green

# --------------------------------------------------------
# 7) Wait for RDS and get its endpoint
# --------------------------------------------------------
Write-Host "`n[7/7] Waiting for RDS to become available (can take 5-10 min)..." -ForegroundColor Cyan
aws rds wait db-instance-available --region $Region `
    --db-instance-identifier "${AppName}-db"

$rdsEndpoint = aws rds describe-db-instances --region $Region `
    --db-instance-identifier "${AppName}-db" `
    --query "DBInstances[0].Endpoint.Address" --output text
Write-Host "  RDS endpoint: $rdsEndpoint" -ForegroundColor Green

# --------------------------------------------------------
# Output summary
# --------------------------------------------------------
$summary = @"

=============================================================
  BrightNest Academy — AWS Provisioning COMPLETE
=============================================================

  EC2 Instance  : $instanceId
  Elastic IP    : $elasticIp
  RDS Endpoint  : $rdsEndpoint

=============================================================
  NEXT STEPS
=============================================================

1) Point DNS to your Elastic IP:
   GoDaddy DNS: Set @ A record -> $elasticIp
                Set www CNAME -> @

2) SSH into EC2 (wait ~3 min for userdata to finish):
   ssh -i "$KeyPairName.pem" ubuntu@$elasticIp

3) Create /opt/brightnest/.env on the server:
   sudo nano /opt/brightnest/.env

   Paste and fill in from .env.example:
     DB_HOST=$rdsEndpoint
     DB_PORT=3306
     DB_NAME=$DbName
     DB_USER=$DbUser
     DB_PASS=<same password you used here>
     JWT_SECRET=<openssl rand -base64 64>
     ADMIN_EMAIL=admin@brightnest-academy.com
     ADMIN_PASSWORD=<your admin password>
     SPRING_PROFILES_ACTIVE=prod
     PORT=8080

   sudo chmod 600 /opt/brightnest/.env

4) Copy docker-compose.prod.yml to the server:
   scp -i "$KeyPairName.pem" shrishail-academy/docker-compose.prod.yml \
       ubuntu@${elasticIp}:/opt/brightnest/docker-compose.yml

5) Initialize the RDS database:
   mysql -h $rdsEndpoint -u admin -p < shrishail-academy/database/schema.sql
   mysql -h $rdsEndpoint -u admin -p < shrishail-academy/database/seed.sql
   mysql -h $rdsEndpoint -u admin -p < shrishail-academy/deploy/aws/aws-rds-init.sql

6) Get SSL certificate (run on EC2 after DNS propagates):
   bash /opt/repo/shrishail-academy/deploy/aws/scripts/setup-ssl.sh

7) Add GitHub Secrets (run from your laptop):
   .\deploy\aws\scripts\set-github-secrets.ps1 ``
     -DeployHost     "$elasticIp" ``
     -DeployUser     "ubuntu" ``
     -SshKeyPath     "$KeyPairName.pem" ``
     -DockerHubUser  "<your_dockerhub_username>" ``
     -DockerHubToken "<your_dockerhub_token>"

=============================================================
"@

Write-Host $summary -ForegroundColor Green

# Save outputs to file for reference
$summary | Out-File -FilePath ".\aws-provision-output.txt" -Encoding utf8
Write-Host "Saved to: .\aws-provision-output.txt" -ForegroundColor Yellow
