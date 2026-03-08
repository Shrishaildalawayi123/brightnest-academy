# 🚀 BrightNest Academy - Complete AWS Production Launch Guide

**Date:** March 8, 2026  
**Platform:** Spring Boot 3.2.2 + Java 21 + MySQL 8.0  
**Target:** AWS Production SaaS Deployment  
**Audience:** Developers using VS Code, GitHub, Docker, and AWS Console

---

## 📋 Executive Summary

This guide provides a **complete, step-by-step implementation roadmap** to launch BrightNest Academy as a production-ready SaaS education platform on AWS.

**Architecture Overview:**
```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                       │
│  Users (HTTPS) → CloudFront/Route 53 → AWS Infrastructure            │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
            ┌───────▼────────┐       ┌───────▼────────┐
            │   Frontend      │       │    Backend     │
            │   React → S3    │       │   Spring Boot  │
            │   CloudFront    │       │   ECS/EC2      │
            └─────────────────┘       └────────┬───────┘
                                               │
                    ┌──────────────────────────┼──────────────────┐
                    │                          │                  │
            ┌───────▼────────┐     ┌───────────▼──────┐  ┌───────▼────────┐
            │   RDS MySQL     │     │   S3 Storage     │  │  ElastiCache   │
            │   Multi-AZ      │     │   (Resumes)      │  │  Redis         │
            └─────────────────┘     └──────────────────┘  └────────────────┘
                    │
            ┌───────▼────────┐
            │  CloudWatch     │
            │  Monitoring     │
            └─────────────────┘
```

**Current Status:** 78% Production Ready  
**Blockers:** SSL/TLS, Nginx hardening, monitoring setup  
**Timeline:** 5-7 days for full production launch

---

## 🎯 Pre-Flight Checklist

### Local Development Environment

- [ ] **Java 21 JDK installed** - `java -version` shows 21.x
- [ ] **Maven 3.8+** - `mvn -v`
- [ ] **Docker Desktop** - Running and accessible
- [ ] **Node.js 18+** (for testing) - `node -v`
- [ ] **VS Code** with extensions:
  - Java Extension Pack
  - Spring Boot Extension Pack
  - Docker
  - AWS Toolkit
- [ ] **Git configured** - Connected to GitHub
- [ ] **AWS CLI installed** - `aws --version`

### AWS Account Setup

- [ ] **AWS Account created** with billing alerts enabled
- [ ] **IAM user created** with programmatic access
- [ ] **AWS CLI configured** - `aws configure`
- [ ] **Domain registered** (GoDaddy or Route 53)
- [ ] **GitHub repository** - Codebase pushed

---

## 📊 Phase-by-Phase Implementation

---

# PHASE 1: Local Preparation & Testing (Day 1)

## Step 1.1: Verify Local Build

**Goal:** Ensure the application builds and tests pass locally.

### VS Code Terminal Commands:

```powershell
# Navigate to project directory
cd "d:\Tuition class website\shrishail-academy"

# Clean build with tests
mvn clean verify

# Expected: BUILD SUCCESS + All tests pass
```

**Validation:**
- ✅ Maven build completes without errors
- ✅ 178+ tests pass (JUnit + chaos tests)
- ✅ JaCoCo coverage report generated in `target/site/jacoco/index.html`

**Troubleshooting:**
- If tests fail: Check MySQL is running on localhost:3306
- Database connection error: Update `application.properties` DB credentials
- Java version mismatch: Set `JAVA_HOME` to JDK 21 directory

---

## Step 1.2: Docker Build & Test

**Goal:** Validate the Dockerfile builds and runs correctly.

### VS Code Terminal:

```powershell
# Build Docker image
docker build -t brightnest-academy:local .

# Verify image created
docker images | Select-String brightnest

# Expected output: brightnest-academy   local   [image-id]   [time]   ~150MB
```

### Run Container Locally:

Create `.env.local` file in project root (DO NOT COMMIT):

```env
DB_HOST=host.docker.internal
DB_PORT=3306
DB_NAME=shrishail_academy
DB_USER=root
DB_PASS=root
JWT_SECRET=Local-Test-Secret-Key-Must-Be-At-Least-64-Characters-Long-For-HS512!!
ADMIN_EMAIL=admin@brightnest.local
ADMIN_PASSWORD=Admin@123
SPRING_PROFILES_ACTIVE=dev
```

Run container:

```powershell
# Run with environment file
docker run --rm -p 8080:8080 --env-file .env.local brightnest-academy:local

# In another terminal, test health endpoint
curl http://localhost:8080/health

# Expected: {"status":"UP"}
```

**Validation:**
- ✅ Container starts without errors
- ✅ Health endpoint returns 200 OK
- ✅ Login endpoint accessible: `POST http://localhost:8080/api/auth/login`

---

## Step 1.3: Security Audit (Using Existing Tests)

**Goal:** Run security validation tests.

```powershell
# Run security-specific tests
mvn test -Dtest="*Security*,*Auth*,*CSRF*"

# Run chaos/resilience tests
mvn test -Dtest="com.shrishailacademy.chaos.*"
```

**Validation:**
- ✅ JWT token validation works correctly
- ✅ CSRF protection active
- ✅ SQL injection tests pass
- ✅ Rate limiting tests pass

---

# PHASE 2: AWS Infrastructure Setup (Day 2)

## Step 2.1: Create AWS VPC & Networking

**AWS Console → VPC Dashboard**

### Create VPC:

1. **Navigate:** VPC → Your VPCs → Create VPC
2. **VPC Settings:**
   - Name: `brightnest-prod-vpc`
   - IPv4 CIDR: `10.0.0.0/16`
   - Tenancy: Default
   - Click **Create VPC**

### Create Subnets (Multi-AZ):

**Public Subnet 1 (us-east-1a):**
- VPC: `brightnest-prod-vpc`
- Name: `brightnest-public-1a`
- AZ: us-east-1a
- IPv4 CIDR: `10.0.1.0/24`

**Public Subnet 2 (us-east-1b):**
- Name: `brightnest-public-1b`
- AZ: us-east-1b
- IPv4 CIDR: `10.0.2.0/24`

**Private Subnet 1 (us-east-1a):**
- Name: `brightnest-private-1a`
- AZ: us-east-1a
- IPv4 CIDR: `10.0.11.0/24`

**Private Subnet 2 (us-east-1b):**
- Name: `brightnest-private-1b`
- AZ: us-east-1b
- IPv4 CIDR: `10.0.12.0/24`

### Create Internet Gateway:

1. VPC → Internet Gateways → Create
2. Name: `brightnest-igw`
3. Attach to VPC: `brightnest-prod-vpc`

### Create Route Tables:

**Public Route Table:**
- Name: `brightnest-public-rt`
- VPC: `brightnest-prod-vpc`
- Add route: `0.0.0.0/0` → `brightnest-igw`
- Associate with: `brightnest-public-1a`, `brightnest-public-1b`

---

## Step 2.2: Create RDS MySQL Database

**AWS Console → RDS → Create Database**

### Database Settings:

**Engine:**
- Engine: MySQL 8.0.35 (or latest)
- Templates: **Production**

**Availability & Durability:**
- Multi-AZ: ✅ **Yes** (for high availability)

**DB Instance:**
- Instance class: `db.t3.micro` (Free tier) or `db.t3.small` (Production)
- Storage: 20 GB gp3 (General Purpose SSD)
- Enable storage autoscaling: ✅ Yes (max 100 GB)

**Credentials:**
- Master username: `brightnestadmin`
- Password: *(Generate strong password - save in AWS Secrets Manager)*

**Connectivity:**
- VPC: `brightnest-prod-vpc`
- Subnet group: Create new → Use private subnets
- Public access: **No**
- VPC Security Group: Create new → `brightnest-rds-sg`
- Availability Zone: No preference

**Database Authentication:**
- Password authentication

**Additional Configuration:**
- Initial database name: `brightnest_academy`
- Backup retention: 7 days
- Enable automated backups: ✅ Yes
- Enable encryption: ✅ Yes
- Performance Insights: ✅ Enable (15 days retention)

**Cost:** ~$15-30/month for db.t3.small

### Update Security Group:

**RDS Security Group (`brightnest-rds-sg`):**
- Inbound rule: MySQL (3306) from `brightnest-app-sg` (will create next)

### Save RDS Endpoint:

After creation (takes 5-10 minutes), note:
```
brightnest-prod.xxxxxxxxx.us-east-1.rds.amazonaws.com:3306
```

---

## Step 2.3: Create Secrets Manager for Credentials

**AWS Console → Secrets Manager → Store a new secret**

### Database Credentials:

1. Secret type: **Credentials for RDS database**
2. Select RDS instance: `brightnest-prod`
3. Secret name: `brightnest/prod/db-credentials`
4. Rotation: Enable automatic rotation (30 days)

### JWT Secret:

1. Secret type: **Other type of secret**
2. Key/value pairs:
   ```json
   {
     "JWT_SECRET": "Generate-64-Plus-Character-Random-String-Here-Use-Online-Generator",
     "JWT_REFRESH_PEPPER": "Another-Random-64-Character-String-For-Refresh-Tokens"
   }
   ```
3. Secret name: `brightnest/prod/jwt-secrets`

### Admin Credentials:

```json
{
  "ADMIN_EMAIL": "admin@brightnest-academy.com",
  "ADMIN_PASSWORD": "SecurePassword123!@#"
}
```
Secret name: `brightnest/prod/admin-credentials`

**Cost:** $0.40/secret/month

---

## Step 2.4: Initialize Database Schema

**Option A: Using MySQL Workbench (Recommended for first setup)**

1. Install MySQL Workbench
2. Create SSH tunnel to EC2 bastion (if RDS is private)
3. Connect to RDS endpoint
4. Run schema initialization:

```sql
-- Use the provided schema
SOURCE d:/Tuition class website/shrishail-academy/database/schema.sql;

-- Verify tables created
SHOW TABLES;

-- Run seed data
SOURCE d:/Tuition class website/shrishail-academy/database/seed.sql;
```

**Option B: Using AWS Console Session Manager**

If you have an EC2 bastion host:

```bash
# Connect to EC2 instance
aws ssm start-session --target i-xxxxxxxxx

# Install MySQL client
sudo apt-get update
sudo apt-get install -y mysql-client

# Connect to RDS
mysql -h brightnest-prod.xxxxxxxxx.us-east-1.rds.amazonaws.com \
      -u brightnestadmin -p brightnest_academy

# Run schema file
mysql> source /path/to/schema.sql;
```

---

# PHASE 3: Backend Deployment (Day 3)

## Step 3.1: Choose Deployment Strategy

### **Option A: AWS ECS Fargate (Recommended for SaaS)**

**Pros:**
- Fully managed (no server management)
- Auto-scaling built-in
- Pay per second
- Integrated with Application Load Balancer
- Easy blue/green deployments

**Cons:**
- Slightly more expensive than EC2
- Cold start latency (minimal with proper warming)

**Cost:** ~$15-30/month for 1 container (0.25 vCPU, 512 MB RAM)

### **Option B: EC2 with Docker**

**Pros:**
- More control
- Potentially cheaper for sustained workloads
- Easier to debug

**Cons:**
- Manual server management
- Manual scaling configuration
- OS patching required

**Cost:** ~$10-15/month for t3.small

**Recommended:** Start with **ECS Fargate** for easier management and scaling.

---

## Step 3.2: Setup ECS Fargate Deployment

### Create ECS Cluster:

**AWS Console → ECS → Create Cluster**

- Cluster name: `brightnest-prod-cluster`
- Infrastructure: AWS Fargate (serverless)
- Click **Create**

### Create ECR Repository:

**AWS Console → ECR → Create Repository**

- Repository name: `brightnest-academy`
- Image tag mutability: Mutable
- Scan on push: ✅ Enable
- Click **Create**

**Note URI:** `123456789012.dkr.ecr.us-east-1.amazonaws.com/brightnest-academy`

### Push Docker Image to ECR:

**VS Code Terminal:**

```powershell
# AWS CLI login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Build image for production
docker build -t brightnest-academy:latest .

# Tag for ECR
docker tag brightnest-academy:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/brightnest-academy:latest

# Push to ECR
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/brightnest-academy:latest

# Expected: Successfully pushed
```

---

## Step 3.3: Create ECS Task Definition

**AWS Console → ECS → Task Definitions → Create new**

### Task Definition Configuration:

**Infrastructure:**
- Launch type: Fargate
- OS: Linux/X86_64
- CPU: 0.5 vCPU
- Memory: 1 GB
- Task role: `ecsTaskRole` (create if needed)
- Task execution role: `ecsTaskExecutionRole`

**Container Definition:**
- Container name: `brightnest-app`
- Image URI: `123456789012.dkr.ecr.us-east-1.amazonaws.com/brightnest-academy:latest`
- Port mappings: `8080` (Container), Protocol: TCP
- Essential: ✅ Yes

**Environment Variables:**
```json
{
  "SPRING_PROFILES_ACTIVE": "prod",
  "DB_HOST": "brightnest-prod.xxxxxxxxx.us-east-1.rds.amazonaws.com",
  "DB_PORT": "3306",
  "DB_NAME": "brightnest_academy",
  "CORS_ORIGINS": "https://brightnest-academy.com,https://www.brightnest-academy.com"
}
```

**Secrets (from Secrets Manager):**
- `DB_USER`: brightnest/prod/db-credentials:username
- `DB_PASS`: brightnest/prod/db-credentials:password
- `JWT_SECRET`: brightnest/prod/jwt-secrets:JWT_SECRET
- `ADMIN_EMAIL`: brightnest/prod/admin-credentials:ADMIN_EMAIL
- `ADMIN_PASSWORD`: brightnest/prod/admin-credentials:ADMIN_PASSWORD

**Health Check:**
```json
{
  "command": ["CMD-SHELL", "curl -f http://localhost:8080/health || exit 1"],
  "interval": 30,
  "timeout": 5,
  "retries": 3,
  "startPeriod": 60
}
```

**Logging:**
- Log driver: awslogs
- Log group: `/ecs/brightnest-academy`
- Region: us-east-1
- Stream prefix: `ecs`

**Click Create**

---

## Step 3.4: Create Application Load Balancer

**AWS Console → EC2 → Load Balancers → Create**

### Load Balancer Configuration:

- Type: **Application Load Balancer**
- Name: `brightnest-prod-alb`
- Scheme: **Internet-facing**
- IP address type: IPv4

### Network Mapping:
- VPC: `brightnest-prod-vpc`
- Availability Zones: 
  - us-east-1a: `brightnest-public-1a`
  - us-east-1b: `brightnest-public-1b`

### Security Groups:

Create new: `brightnest-alb-sg`
- Inbound:
  - HTTP (80) from 0.0.0.0/0
  - HTTPS (443) from 0.0.0.0/0
- Outbound: All traffic

### Listeners:

**HTTP Listener (Port 80):**
- Default action: Redirect to HTTPS (443)

**HTTPS Listener (Port 443):**
- Default action: Forward to target group (create next)
- SSL Certificate: Request from ACM (next section)

### Create Target Group:

- Target type: **IP addresses** (for Fargate)
- Target group name: `brightnest-app-tg`
- Protocol: HTTP
- Port: 8080
- VPC: `brightnest-prod-vpc`
- Health check path: `/health`
- Health check interval: 30 seconds
- Healthy threshold: 2
- Unhealthy threshold: 3
- Timeout: 5 seconds

**Click Create**

---

## Step 3.5: Request SSL Certificate (ACM)

**AWS Console → Certificate Manager → Request Certificate**

1. Certificate type: **Public certificate**
2. Domain names:
   - `brightnest-academy.com`
   - `www.brightnest-academy.com`
   - `*.brightnest-academy.com` (optional wildcard)
3. Validation method: **DNS validation** (recommended)
4. Key algorithm: RSA 2048
5. Click **Request**

### DNS Validation:

**For GoDaddy:**
1. ACM will show CNAME records to add
2. Login to GoDaddy → DNS Management
3. Add CNAME records exactly as shown in ACM
4. Wait 5-30 minutes for validation

**Record example:**
```
_abc123.brightnest-academy.com → /
_xyz789.acm-validations.aws
```

Once validated, status shows **Issued**.

---

## Step 3.6: Create ECS Service

**AWS Console → ECS → Clusters → brightnest-prod-cluster → Create Service**

### Service Configuration:

**Deployment:**
- Launch type: Fargate
- Task Definition: `brightnest-app` (latest version)
- Service name: `brightnest-app-service`
- Desired tasks: 2 (for high availability)

**Networking:**
- VPC: `brightnest-prod-vpc`
- Subnets: Select private subnets (`brightnest-private-1a`, `brightnest-private-1b`)
- Security group: Create new → `brightnest-app-sg`
  - Inbound: Port 8080 from `brightnest-alb-sg`
- Public IP: DISABLED (behind ALB)

**Load Balancing:**
- Load balancer type: Application Load Balancer
- Load balancer: `brightnest-prod-alb`
- Listener: 443:HTTPS
- Target group: `brightnest-app-tg`
- Health check grace period: 60 seconds

**Auto Scaling (Optional but Recommended):**
- Service auto scaling: ✅ Enable
- Minimum tasks: 2
- Maximum tasks: 6
- Scaling policy: Target tracking
- Metric: Average CPU utilization
- Target value: 70%

**Click Create**

### Verify Deployment:

Wait 5-10 minutes for tasks to start.

**Check:**
1. ECS → Clusters → Tasks → Status should be **RUNNING**
2. Target Group → Targets → Health status should be **Healthy**
3. ALB DNS name → Test in browser (will be HTTP initially)

---

# PHASE 4: Domain & HTTPS Configuration (Day 4)

## Step 4.1: Configure Route 53 (or GoDaddy DNS)

### Option A: Migrate to Route 53 (Recommended)

**Benefits:** Automatic ACM integration, health checks, failover

**Steps:**
1. AWS Console → Route 53 → Hosted Zones → Create
2. Domain name: `brightnest-academy.com`
3. Type: Public
4. Create hosted zone
5. Note the 4 nameservers (ns-xxx.awsdns-xx.com)
6. Update nameservers in GoDaddy:
   - GoDaddy → Domain Settings → Nameservers → Change
   - Set to custom nameservers (AWS Route 53 NS records)

**Create Records:**

**A Record (Root domain):**
- Record name: (leave blank)
- Type: A
- Alias: ✅ Yes
- Route traffic to: Alias to Application Load Balancer
- Region: us-east-1
- Load balancer: `brightnest-prod-alb`

**A Record (www):**
- Record name: `www`
- Type: A
- Alias: ✅ Yes
- Route traffic to: Same as root

### Option B: Use GoDaddy DNS

**GoDaddy DNS Settings:**

1. Login to GoDaddy → My Products → Domain
2. DNS Management
3. Update A records:
   - Type: A, Name: `@`, Value: `<ALB DNS resolved IP>` (NOT recommended - use Route 53 alias instead)
   - Type: CNAME, Name: `www`, Value: `brightnest-academy.com`

**Note:** Using ALB IP directly is NOT recommended. Use Route 53 alias for best practice.

---

## Step 4.2: Update Load Balancer with SSL

**AWS Console → EC2 → Load Balancers → brightnest-prod-alb**

### Add HTTPS Listener:

1. Listeners tab → Add listener
2. Protocol: HTTPS
3. Port: 443
4. Default actions: Forward to `brightnest-app-tg`
5. Security policy: `ELBSecurityPolicy-TLS13-1-2-2021-06` (recommended)
6. SSL Certificate: Select ACM certificate for `brightnest-academy.com`
7. Save

### Update HTTP Listener to Redirect:

1. Edit HTTP:80 listener
2. Default action: Redirect to HTTPS
3. Port: 443
4. Status code: 301 (Permanent)
5. Save

---

## Step 4.3: Test HTTPS Access

**Browser Test:**

```
https://brightnest-academy.com
https://www.brightnest-academy.com
```

**Expected:**
- ✅ Valid SSL certificate (green padlock)
- ✅ No browser warnings
- ✅ Application loads correctly

**API Test:**

```powershell
# Test health endpoint
curl https://brightnest-academy.com/health

# Expected: {"status":"UP"}

# Test login endpoint (should return 400 without credentials, not 401 or 500)
curl -X POST https://brightnest-academy.com/api/auth/login -H "Content-Type: application/json"
```

---

# PHASE 5: Frontend Deployment (Day 4-5)

## Step 5.1: Prepare Frontend Code

**Assuming React frontend exists in your project:**

### Update API Configuration:

**frontend/src/config.js (or .env):**

```javascript
// Production API URL
export const API_BASE_URL = 'https://brightnest-academy.com/api';

// OR if using environment variables
VITE_API_URL=https://brightnest-academy.com/api
REACT_APP_API_URL=https://brightnest-academy.com/api
```

### Build Frontend:

```powershell
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Build for production
npm run build

# Output: dist/ or build/ directory
```

---

## Step 5.2: Deploy to AWS S3 + CloudFront

### Create S3 Bucket:

**AWS Console → S3 → Create Bucket**

- Bucket name: `brightnest-frontend-prod` (must be globally unique)
- Region: us-east-1
- Block all public access: ✅ **UNCHECK** (we'll use CloudFront)
- Bucket versioning: Enable
- Encryption: Enable (SSE-S3)
- Create bucket

### Enable Static Website Hosting:

1. Properties tab → Static website hosting
2. Enable
3. Index document: `index.html`
4. Error document: `index.html` (for SPA routing)
5. Save

### Upload Build Files:

**VS Code Terminal:**

```powershell
# Sync build to S3
aws s3 sync ./dist s3://brightnest-frontend-prod --delete

# Set public read permissions (if needed)
aws s3 sync ./dist s3://brightnest-frontend-prod --acl public-read --delete
```

---

## Step 5.3: Create CloudFront Distribution

**AWS Console → CloudFront → Create Distribution**

### Origin Settings:

- Origin domain: Select S3 bucket `brightnest-frontend-prod`
- Origin path: (leave blank)
- Name: `brightnest-frontend-origin`
- Origin access: **Origin access control** (recommended)
  - Create control setting → Create

### Default Cache Behavior:

- Viewer protocol policy: **Redirect HTTP to HTTPS**
- Allowed HTTP methods: GET, HEAD, OPTIONS
- Cache policy: **CachingOptimized**
- Origin request policy: None

### Distribution Settings:

- Price class: Use all edge locations (best performance)
- Alternate domain names (CNAMEs):
  - `app.brightnest-academy.com`
  - OR use root domain if frontend-only
- SSL Certificate: Select ACM certificate
- Default root object: `index.html`

### Custom Error Responses (for SPA):

Add error response:
- HTTP error code: 403
- Response page path: `/index.html`
- HTTP response code: 200

Add error response:
- HTTP error code: 404
- Response page path: `/index.html`
- HTTP response code: 200

**Click Create**

**Wait:** CloudFront deployment takes 15-30 minutes.

---

## Step 5.4: Update DNS for Frontend

**Route 53:**

Create A record:
- Name: `app` (or leave blank for root)
- Type: A
- Alias: Yes
- Route traffic to: CloudFront distribution
- Select your distribution

---

## Step 5.5: Alternative: AWS Amplify Hosting

**Simpler option for React apps:**

**AWS Console → Amplify → New App → Host web app**

1. Connect GitHub repository
2. Select branch: `main`
3. Build settings (auto-detected for React):
   ```yaml
   version: 1
   frontend:
     phases:
       preBuild:
         commands:
           - npm ci
       build:
         commands:
           - npm run build
     artifacts:
       baseDirectory: dist
       files:
         - '**/*'
     cache:
       paths:
         - node_modules/**/*
   ```
4. Environment variables:
   ```
   VITE_API_URL=https://brightnest-academy.com/api
   ```
5. Save and deploy

**Amplify automatically:**
- Builds on git push
- Provides HTTPS
- Gives you URL: `https://main.xxxxx.amplifyapp.com`
- Custom domain setup available

**Cost:** Free for <15 GB bandwidth/month, then $0.15/GB

---

# PHASE 6: CI/CD Pipeline (Day 5)

## Step 6.1: Update GitHub Actions Workflow

**File:** `.github/workflows/deploy.yml`

### Update ECR Push Configuration:

```yaml
name: Deploy to AWS Production

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: read
  id-token: write  # For OIDC authentication

env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY: brightnest-academy
  ECS_CLUSTER: brightnest-prod-cluster
  ECS_SERVICE: brightnest-app-service
  ECS_TASK_DEFINITION: brightnest-app

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'

      - name: Run tests
        working-directory: ./shrishail-academy
        env:
          SPRING_PROFILES_ACTIVE: test
        run: mvn clean verify

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build and push Docker image
        working-directory: ./shrishail-academy
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:latest .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest

      - name: Update ECS service
        run: |
          aws ecs update-service \
            --cluster ${{ env.ECS_CLUSTER }} \
            --service ${{ env.ECS_SERVICE }} \
            --force-new-deployment \
            --region ${{ env.AWS_REGION }}

      - name: Wait for service stability
        run: |
          aws ecs wait services-stable \
            --cluster ${{ env.ECS_CLUSTER }} \
            --services ${{ env.ECS_SERVICE }} \
            --region ${{ env.AWS_REGION }}
```

---

## Step 6.2: Setup AWS OIDC for GitHub Actions

**Recommended:** Use OIDC instead of long-lived access keys.

### Create IAM OIDC Provider:

**AWS Console → IAM → Identity Providers → Add Provider**

- Provider type: OpenID Connect
- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`
- Click **Add Provider**

### Create IAM Role for GitHub Actions:

**IAM → Roles → Create Role**

- Trusted entity: Web identity
- Identity provider: `token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`
- GitHub organization: Your username
- GitHub repository: `shrishail-academy`
- Click **Next**

**Attach Policies:**
- `AmazonEC2ContainerRegistryPowerUser`
- `AmazonECS_FullAccess`
- Create custom policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService",
        "ecs:DescribeServices",
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    }
  ]
}
```

**Role name:** `GitHubActionsDeployRole`

**Copy Role ARN:** `arn:aws:iam::123456789012:role/GitHubActionsDeployRole`

### Add to GitHub Secrets:

**GitHub → Repository → Settings → Secrets and variables → Actions**

Add secret:
- Name: `AWS_ROLE_ARN`
- Value: `arn:aws:iam::123456789012:role/GitHubActionsDeployRole`

---

## Step 6.3: Test CI/CD Pipeline

**Trigger deployment:**

```powershell
# Make a small change
echo "# CI/CD Test" >> README.md

# Commit and push
git add README.md
git commit -m "test: trigger CI/CD pipeline"
git push origin main
```

**Monitor:**
1. GitHub → Actions tab → Watch workflow run
2. AWS Console → ECS → Services → Deployments tab
3. CloudWatch → Log groups → `/ecs/brightnest-academy`

**Validation:**
- ✅ GitHub Actions: All steps pass
- ✅ ECR: New image tagged with commit SHA
- ✅ ECS: New task revision deployed
- ✅ Target Group: New tasks become healthy
- ✅ Application: No downtime during deployment

---

# PHASE 7: Security Hardening (Day 6)

## Step 7.1: Enable AWS WAF

**AWS Console → WAF & Shield → Create Web ACL**

### Web ACL Configuration:

- Name: `brightnest-waf`
- Resource type: Application Load Balancer
- Region: us-east-1
- Associated resources: Select `brightnest-prod-alb`

### Add Managed Rule Groups:

1. **AWS Managed Rules - Core rule set**
   - Protection against common threats (SQLi, XSS)
   
2. **AWS Managed Rules - Known bad inputs**
   - Blocks requests with malicious payloads

3. **AWS Managed Rules - SQL database**
   - Additional SQL injection protection

4. **AWS Managed Rules - Linux operating system**
   - Blocks Linux-specific exploits

### Custom Rate Limiting Rules:

**Rule 1: General rate limit**
- Name: `RateLimit-GeneralAPI`
- Type: Rate-based rule
- Rate limit: 2000 requests per 5 minutes (400/min)
- Action: Block
- Scope: All requests

**Rule 2: Login endpoint protection**
- Name: `RateLimit-Login`
- Type: Rate-based rule
- Rate limit: 100 requests per 5 minutes (20/min)
- URI path: `/api/auth/login`
- Action: Block

**Rule 3: Geographic restriction (optional)**
- Name: `GeoBlock-Restricted`
- Type: Geo match
- Block countries: (Select high-risk countries if needed)
- Action: Block

**Cost:** $5/month + $1/million requests

---

## Step 7.2: Update Security Groups

### Review and Harden:

**ALB Security Group (`brightnest-alb-sg`):**
```
Inbound:
- HTTPS (443) from 0.0.0.0/0
- HTTP (80) from 0.0.0.0/0 (redirect only)

Outbound:
- Port 8080 to brightnest-app-sg only
```

**App Security Group (`brightnest-app-sg`):**
```
Inbound:
- Port 8080 from brightnest-alb-sg ONLY

Outbound:
- Port 3306 to brightnest-rds-sg (MySQL)
- Port 443 to 0.0.0.0/0 (for external API calls if needed)
```

**RDS Security Group (`brightnest-rds-sg`):**
```
Inbound:
- Port 3306 from brightnest-app-sg ONLY

Outbound:
- None needed (database doesn't initiate connections)
```

---

## Step 7.3: Enable CloudTrail & Config

### CloudTrail:

**AWS Console → CloudTrail → Create Trail**

- Trail name: `brightnest-audit-trail`
- Storage location: Create new S3 bucket
- Log file validation: ✅ Enable
- SNS notification: Optional
- CloudWatch Logs: ✅ Enable
- Log events:
  - Management events: ✅ All
  - Data events: S3, Lambda (if used)
  - Insights events: ✅ Enable

**Cost:** ~$2/month for management events

### AWS Config:

**AWS Console → Config → Get Started**

- Record all resources: ✅ Yes
- Include global resources: ✅ Yes
- S3 bucket: Create new
- SNS topic: Create new
- Rules to enable:
  - encrypted-volumes
  - rds-snapshots-public-prohibited
  - s3-bucket-public-read-prohibited
  - vpc-sg-open-only-to-authorized-ports

---

## Step 7.4: Secrets Rotation

**AWS Console → Secrets Manager → brightnest/prod/db-credentials**

### Enable Auto-Rotation:

1. Edit rotation
2. Enable automatic rotation: ✅ Yes
3. Rotation schedule: 30 days
4. Rotation function: Create new Lambda (AWS-provided)
5. Use separate credentials: Recommended (create master rotation user)
6. Save

**Verify:** Rotation Lambda created and RDS permissions configured.

---

# PHASE 8: Monitoring & Observability (Day 6)

## Step 8.1: CloudWatch Dashboards

**AWS Console → CloudWatch → Dashboards → Create**

### Create Dashboard: `BrightNest-Production`

**Widgets to add:**

1. **ALB Metrics:**
   - Active connection count
   - Target response time
   - HTTP 4xx/5xx errors
   - Healthy/Unhealthy host count

2. **ECS Metrics:**
   - CPU utilization
   - Memory utilization
   - Running task count

3. **RDS Metrics:**
   - CPU utilization
   - Database connections
   - Read/Write IOPS
   - Free storage space

4. **Application Metrics (Custom):**
   - API response time (from logs)
   - Login success/failure rate
   - Active users

---

## Step 8.2: CloudWatch Alarms

### Critical Alarms:

**1. ALB Target Health:**
```
Metric: HealthyHostCount
Threshold: < 1
Period: 1 minute
Alarm action: SNS topic → Email/SMS
```

**2. RDS CPU High:**
```
Metric: CPUUtilization
Threshold: > 80%
Period: 5 minutes
Datapoints: 2 of 3
```

**3. RDS Storage Low:**
```
Metric: FreeStorageSpace
Threshold: < 2 GB
```

**4. ECS Task Count:**
```
Metric: RunningTaskCount
Threshold: < 1
Period: 1 minute
```

**5. Application Errors:**
```
Log group: /ecs/brightnest-academy
Filter: "ERROR"
Threshold: > 10 occurrences in 5 minutes
```

### Create SNS Topic:

**AWS Console → SNS → Create Topic**

- Name: `brightnest-alerts`
- Type: Standard
- Create subscription:
  - Protocol: Email
  - Endpoint: your-email@example.com
  - Confirm subscription via email

**Cost:** $0 (within free tier for email)

---

## Step 8.3: Application Performance Monitoring (Optional)

### Option A: AWS X-Ray

**Enable in application.properties:**
```properties
# X-Ray tracing
spring.application.name=brightnest-academy
management.tracing.sampling.probability=0.1
```

**Add dependency to pom.xml:**
```xml
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-xray</artifactId>
</dependency>
```

**Grant permissions to ECS task role:**
- `AWSXRayDaemonWriteAccess`

### Option B: Prometheus + Grafana (Advanced)

**Your application already exposes `/actuator/prometheus`**

1. Deploy Prometheus on EC2 or ECS
2. Configure scraping from ECS tasks
3. Deploy Grafana for visualization
4. Import Spring Boot dashboard templates

**Cost:** ~$10-20/month for small EC2 instances

---

# PHASE 9: Backup & Disaster Recovery (Day 7)

## Step 9.1: RDS Automated Backups

**Already configured during RDS creation:**
- ✅ Automated backups: 7-day retention
- ✅ Backup window: AWS-managed
- ✅ Snapshot before delete: Enabled

### Create Manual Snapshot (Best Practice):

**Before major deployments:**

```powershell
# AWS CLI
aws rds create-db-snapshot \
  --db-instance-identifier brightnest-prod \
  --db-snapshot-identifier brightnest-prod-pre-deploy-$(date +%Y%m%d)
```

---

## Step 9.2: S3 Backup for Uploads

**Create lifecycle policy for resume uploads:**

**S3 Bucket → Management → Lifecycle rules**

- Rule name: `archive-old-resumes`
- Scope: Prefix `resumes/`
- Transitions:
  - After 90 days → S3 Glacier Flexible Retrieval
  - After 365 days → S3 Glacier Deep Archive
- Expiration: After 7 years (compliance)

---

## Step 9.3: Disaster Recovery Plan

### Recovery Time Objective (RTO): 30 minutes

**Scenario: Complete region failure**

**Recovery Steps:**

1. **Restore RDS from snapshot:**
   ```bash
   aws rds restore-db-instance-from-db-snapshot \
     --db-instance-identifier brightnest-prod-restored \
     --db-snapshot-identifier <latest-snapshot> \
     --availability-zone us-west-2a
   ```

2. **Update ECS task definition** with new RDS endpoint

3. **Update DNS** in Route 53 to new region's ALB

**Estimated RTO:** 20-30 minutes  
**RPO (Recovery Point Objective):** 5 minutes (automated backups)

### Multi-Region Setup (Future Enhancement):

- Primary: us-east-1
- DR Region: us-west-2
- RDS cross-region read replica
- Route 53 failover routing

---

# PHASE 10: Production Launch Checklist (Day 7)

## Pre-Launch Validation

### Infrastructure:

- [ ] ECS tasks running and healthy (2+ tasks)
- [ ] RDS Multi-AZ enabled and accessible
- [ ] ALB health checks passing
- [ ] CloudFront distribution deployed (if using)
- [ ] SSL certificate installed and valid
- [ ] DNS records propagated (check globally: whatsmydns.net)
- [ ] Security groups configured correctly
- [ ] WAF rules active and tested
- [ ] Secrets Manager storing all credentials
- [ ] No hardcoded secrets in code

### Application:

- [ ] All 178+ tests passing in production environment
- [ ] Database schema deployed and validated
- [ ] Seed data loaded (admin user created)
- [ ] Login works via HTTPS
- [ ] JWT authentication validated
- [ ] CSRF protection enabled
- [ ] CORS configured for production domain
- [ ] Rate limiting active
- [ ] File upload working (S3 integration)
- [ ] Email notifications configured (if applicable)

### Monitoring:

- [ ] CloudWatch alarms configured and tested
- [ ] SNS notifications working (email confirmed)
- [ ] CloudWatch Logs streaming from ECS
- [ ] CloudWatch dashboard created
- [ ] Error tracking configured
- [ ] Performance metrics visible

### Security:

- [ ] WAF rules tested (rate limiting, SQLi, XSS)
- [ ] Security headers validated (CSP, HSTS, X-Frame-Options)
- [ ] HTTPS enforced (HTTP redirects to HTTPS)
- [ ] Database encryption enabled
- [ ] S3 bucket policies secure
- [ ] IAM roles follow least privilege
- [ ] CloudTrail logging enabled
- [ ] Secrets rotation scheduled

### CI/CD:

- [ ] GitHub Actions workflow tested
- [ ] Automated deployments working
- [ ] Rollback procedure documented
- [ ] Blue/green deployment configured (optional)

---

## Launch Day Procedure

### T-minus 24 hours:

1. ✅ Create final RDS snapshot
2. ✅ Review all alarms and verify SNS emails
3. ✅ Run full test suite
4. ✅ Brief team on monitoring procedures

### T-minus 1 hour:

1. ✅ Deploy latest code to production
2. ✅ Verify all tasks healthy
3. ✅ Test critical user journeys:
   - User registration → Login → Dashboard
   - Course enrollment
   - File upload
   - Payment flow (if applicable)

### T-0 (DNS Cutover):

1. **Update DNS:**
   ```
   GoDaddy or Route 53:
   - A record @ → ALB DNS
   - A record www → ALB DNS
   ```

2. **Monitor propagation**
   ```powershell
   # Check DNS resolution
   nslookup brightnest-academy.com
   
   # Expected: Your ALB's IP addresses
   ```

3. **Test from multiple locations:**
   - Use https://www.whatsmydns.net
   - Mobile devices (cellular network)
   - Incognito browser windows

### T+1 hour (Post-launch monitoring):

1. Monitor CloudWatch dashboard every 5 minutes
2. Check error logs for unexpected issues
3. Verify user registrations working
4. Test login from different browsers/devices
5. Monitor ALB metrics (request count, errors, latency)

---

# PHASE 11: Cost Optimization

## Monthly Cost Estimate

| Service                  | Tier/Size          | Monthly Cost |
| ------------------------ | ------------------ | ------------ |
| **ECS Fargate**          | 2 tasks (0.5vCPU)  | $30          |
| **RDS MySQL**            | db.t3.small        | $30          |
| **Application Load Balancer** | Standard      | $22          |
| **S3 Storage**           | 50 GB              | $1           |
| **CloudFront**           | 50 GB transfer     | $4           |
| **ECR**                  | 10 GB images       | $1           |
| **Route 53**             | 1 hosted zone      | $1           |
| **ACM Certificate**      | Free               | $0           |
| **Secrets Manager**      | 3 secrets          | $1.20        |
| **CloudWatch**           | Logs + Alarms      | $5           |
| **WAF**                  | Basic rules        | $6           |
| **Data Transfer**        | 100 GB out         | $9           |
| **TOTAL**                |                    | **~$110/mo** |

### Free Tier Benefits (First 12 months):

- 750 hours EC2 t2.micro/t3.micro
- 20 GB RDS storage
- 5 GB S3 storage
- 1 million Lambda requests
- 10 GB CloudFront data transfer

**Effective first-year cost:** ~$70-80/month

---

## Cost Optimization Strategies

### Immediate Optimizations:

1. **Use Reserved Instances (RDS):**
   - 1-year commitment: 30% savings
   - 3-year commitment: 50% savings
   
2. **Right-size ECS tasks:**
   - Start with 0.25 vCPU / 512 MB RAM
   - Scale up only if needed
   
3. **Enable S3 Intelligent Tiering:**
   - Automatically moves objects to cheaper tiers

4. **Use CloudFront caching aggressively:**
   - Reduces origin requests to ECS/S3

### Long-term Optimizations:

1. **Migrate to Savings Plans** (after 6 months of stable usage)
2. **Implement auto-scaling** to reduce idle capacity
3. **Use Lambda for low-traffic APIs** (if applicable)
4. **Compress assets** (gzip/Brotli) to reduce bandwidth
5. **Monitor and delete unused resources** monthly

---

# PHASE 12: Scaling Strategy

## Traffic Growth Scenarios

### Scenario 1: 100-1,000 active users

**Current architecture handles this easily.**

- ECS: 2 tasks (0.5 vCPU each)
- RDS: db.t3.small
- Monthly cost: $110

### Scenario 2: 1,000-10,000 active users

**Optimizations needed:**

1. **ECS Auto-scaling:**
   - Min: 3 tasks
   - Max: 10 tasks
   - Scale on CPU > 70% or ALB requests > 10,000/min

2. **RDS Upgrade:**
   - Instance: db.t3.medium (2 vCPU, 4 GB RAM)
   - Read replica for reporting queries

3. **Add Redis (ElastiCache):**
   - Cache frequently accessed data
   - Session storage
   - Rate limit storage
   - Type: cache.t3.micro ($12/month)

4. **Enable CloudFront caching for API:**
   - Cache GET endpoints (courses, testimonials, etc.)

**Monthly cost:** ~$250-350

### Scenario 3: 10,000-100,000 active users

**Major architectural changes:**

1. **ECS Cluster:**
   - Min: 10 tasks
   - Max: 50 tasks
   - Larger instance sizes (1 vCPU, 2 GB RAM)

2. **RDS:**
   - Instance: db.r6g.xlarge (4 vCPU, 32 GB RAM)
   - Multi-AZ + Read replicas (2-3)
   - Aurora MySQL (better scalability)

3. **ElastiCache Redis:**
   - cache.r6g.large (cluster mode)
   - Multiple availability zones

4. **S3 + CloudFront:**
   - Aggressive caching
   - Separate CDN for static assets

5. **DynamoDB for event logs:**
   - Move audit logs, user activities to DynamoDB

6. **SQS for async processing:**
   - Email notifications
   - Report generation
   - File processing

**Monthly cost:** ~$1,500-2,500

---

## Horizontal Scaling Implementation

### Enable ECS Auto-scaling:

**AWS Console → ECS → Services → brightnest-app-service → Update**

**Auto Scaling:**
- Minimum tasks: 2
- Desired tasks: 2
- Maximum tasks: 10

**Scaling Policies:**

**Policy 1: CPU-based scaling**
```
Metric: CPUUtilization
Target value: 70%
Scale-out cooldown: 60 seconds
Scale-in cooldown: 300 seconds
```

**Policy 2: Request-based scaling**
```
Metric: ALBRequestCountPerTarget
Target value: 1000 requests/minute/target
```

### Database Read Replicas:

**AWS Console → RDS → brightnest-prod → Actions → Create read replica**

- Region: Same (us-east-1)
- Instance class: Same as primary
- Multi-AZ: No (read replica)
- Name: `brightnest-prod-read-replica`

**Update application.properties:**
```properties
# Primary datasource (writes)
spring.datasource.url=jdbc:mysql://brightnest-prod.xxx.rds.amazonaws.com:3306/brightnest_academy

# Read-only datasource (reports, analytics)
spring.datasource.read.url=jdbc:mysql://brightnest-prod-read-replica.xxx.rds.amazonaws.com:3306/brightnest_academy
```

---

# PHASE 13: Long-term Improvements

## Performance Enhancements

### 1. Database Query Optimization

**Action Items:**
- [ ] Enable RDS Performance Insights
- [ ] Identify slow queries (> 1 second)
- [ ] Add indexes for frequently queried columns
- [ ] Implement query result caching (Redis)

**Example:**
```sql
-- Add composite index for tenant queries
CREATE INDEX idx_course_tenant_status ON courses(tenant_id, status);

-- Add covering index for enrollment lookups
CREATE INDEX idx_enrollment_user_course ON enrollments(user_id, course_id, status);
```

### 2. API Response Caching

**Implement Redis caching:**

```java
@Cacheable(value = "courses", key = "#tenantId")
public List<CourseResponse> getAllCourses(Long tenantId) {
    // Cached for 5 minutes
}

@CacheEvict(value = "courses", key = "#tenantId")
public void createCourse(Long tenantId, CourseCreateRequest request) {
    // Invalidates cache on create/update
}
```

### 3. CDN for Static Assets

**Move to CloudFront:**
- Course images
- User avatars
- PDF documents
- Frontend assets

### 4. Lazy Loading & Pagination

**Ensure all list endpoints are paginated:**
```java
@GetMapping("/courses")
public Page<CourseResponse> getCourses(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size);
    return courseService.findAll(pageable);
}
```

---

## Security Enhancements

### 1. Implement Content Security Policy (CSP)

**Update SecurityConfig.java:**
```java
http.headers()
    .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';");
```

### 2. Add API Rate Limiting per User

**Current:** Global rate limiting  
**Improvement:** Per-user rate limiting with Redis

```java
@RateLimit(key = "#authentication.name", limit = 100, period = 60)
public ResponseEntity<?> apiEndpoint(Authentication authentication) {
    // 100 requests per minute per user
}
```

### 3. Implement OAuth 2.0 / Social Login

**Add Google/GitHub login:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### 4. Penetration Testing

**Schedule annual pen-tests:**
- OWASP ZAP automated scans
- Manual security audit
- Third-party security firm (recommended before Series A)

---

## Maintainability Improvements

### 1. Automated Database Migrations

**Use Flyway or Liquibase:**

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

**Migration files:** `src/main/resources/db/migration/`
```
V1__Initial_schema.sql
V2__Add_blog_tables.sql
V3__Add_tenant_settings.sql
```

### 2. API Documentation

**Enable Swagger/OpenAPI (dev only):**

```java
@Configuration
@Profile("!prod")
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("BrightNest Academy API")
                .version("1.0.0"));
    }
}
```

Access: `http://localhost:8080/swagger-ui.html`

### 3. Structured Logging

**Implement JSON logging for CloudWatch:**

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
</dependency>
```

**logback-spring.xml:**
```xml
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeContext>true</includeContext>
    </encoder>
</appender>
```

### 4. Feature Flags

**Implement feature toggles:**
```java
@Service
public class FeatureFlagService {
    
    public boolean isFeatureEnabled(String feature, Long tenantId) {
        // Check DynamoDB or database for feature flags
        return featureRepository.isEnabled(feature, tenantId);
    }
}
```

**Usage:**
```java
if (featureFlagService.isFeatureEnabled("new-dashboard", tenantId)) {
    return renderNewDashboard();
} else {
    return renderLegacyDashboard();
}
```

---

# PHASE 14: Final Go-Live Checklist

## 1 Week Before Launch

- [ ] **Load testing completed**
  - Use K6 scripts: `performance/k6/stress.js`
  - Target: 10x expected peak traffic
  - Identify bottlenecks and optimize

- [ ] **Security audit completed**
  - OWASP Top 10 verified
  - Penetration testing done
  - All critical/high vulnerabilities fixed

- [ ] **Disaster recovery tested**
  - RDS restore tested
  - ECS task recreation tested
  - Failover to secondary region tested (if applicable)

- [ ] **Documentation complete**
  - Architecture diagrams updated
  - Runbooks for common issues
  - Oncall playbook created

- [ ] **Team training**
  - Support team trained on platform
  - Developers trained on deployment process
  - Oncall rotation scheduled

## 1 Day Before Launch

- [ ] **Final deployment to production**
  - Latest code deployed
  - Database migrations run
  - All smoke tests pass

- [ ] **DNS records ready** (but not yet cutover)
  - TTL reduced to 300 seconds (5 minutes)
  - New records prepared in Route 53

- [ ] **Monitoring verified**
  - All CloudWatch alarms active
  - PagerDuty/Oncall integrated
  - Dashboards accessible to team

- [ ] **Communication plan ready**
  - Status page set up (statuspage.io)
  - Email templates for issues
  - Social media accounts ready

- [ ] **Rollback plan tested**
  - Can roll back to previous version in < 5 minutes
  - Database rollback scripts ready (if needed)

## Launch Day (Hour-by-Hour)

### H-2: Pre-launch checks
```powershell
# Verify all services healthy
aws ecs describe-services --cluster brightnest-prod-cluster --services brightnest-app-service

# Check target health
aws elbv2 describe-target-health --target-group-arn arn:aws:elasticloadbalancing:...

# Verify database
mysql -h brightnest-prod.xxx.rds.amazonaws.com -u admin -p -e "SELECT COUNT(*) FROM users;"
```

### H-1: Team standup
- [ ] All team members ready
- [ ] Emergency contacts confirmed
- [ ] Rollback plan reviewed

### H-0: DNS Cutover
```powershell
# Update Route 53 A record
aws route53 change-resource-record-sets --hosted-zone-id Z123456 --change-batch file://dns-change.json
```

**dns-change.json:**
```json
{
  "Changes": [
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "brightnest-academy.com",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "Z35SXDOTRQ7X7K",
          "DNSName": "brightnest-prod-alb-123456.us-east-1.elb.amazonaws.com",
          "EvaluateTargetHealth": true
        }
      }
    }
  ]
}
```

### H+1: Monitor everything
- [ ] CloudWatch dashboard - no errors
- [ ] ALB request count increasing
- [ ] Target health - all healthy
- [ ] Application logs - no exceptions
- [ ] User registrations working

### H+4: First checkpoint
- [ ] Review metrics
- [ ] Check user feedback
- [ ] Resolve minor issues
- [ ] Post status update

### H+24: Post-launch review
- [ ] Conduct retrospective
- [ ] Document lessons learned
- [ ] Plan hotfixes if needed
- [ ] Celebrate! 🎉

---

# APPENDIX

## A. Useful AWS CLI Commands

### ECS Management
```powershell
# List running tasks
aws ecs list-tasks --cluster brightnest-prod-cluster --service-name brightnest-app-service

# View task details
aws ecs describe-tasks --cluster brightnest-prod-cluster --tasks <task-arn>

# Force new deployment (rolling restart)
aws ecs update-service --cluster brightnest-prod-cluster --service brightnest-app-service --force-new-deployment

# Scale service
aws ecs update-service --cluster brightnest-prod-cluster --service brightnest-app-service --desired-count 4
```

### RDS Management
```powershell
# Create manual snapshot
aws rds create-db-snapshot --db-instance-identifier brightnest-prod --db-snapshot-identifier pre-deploy-$(date +%Y%m%d)

# List snapshots
aws rds describe-db-snapshots --db-instance-identifier brightnest-prod

# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier brightnest-prod-restored \
  --db-snapshot-identifier <snapshot-id>
```

### Logs
```powershell
# Tail ECS logs
aws logs tail /ecs/brightnest-academy --follow

# Search for errors
aws logs filter-log-events --log-group-name /ecs/brightnest-academy --filter-pattern "ERROR"
```

### Secrets
```powershell
# Retrieve secret
aws secretsmanager get-secret-value --secret-id brightnest/prod/jwt-secrets

# Update secret
aws secretsmanager update-secret --secret-id brightnest/prod/jwt-secrets --secret-string '{"JWT_SECRET":"new-value"}'
```

---

## B. Troubleshooting Guide

### Issue: Tasks keep restarting

**Diagnosis:**
```powershell
# Check task stopped reason
aws ecs describe-tasks --cluster brightnest-prod-cluster --tasks <task-arn> --query 'tasks[0].stoppedReason'

# Check logs
aws logs tail /ecs/brightnest-academy --since 10m
```

**Common causes:**
- Database connection failure (check security groups)
- Invalid secrets (check Secrets Manager)
- OOM (increase memory in task definition)
- Health check failing (increase grace period)

### Issue: High latency

**Diagnosis:**
```powershell
# Check ALB metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApplicationELB \
  --metric-name TargetResponseTime \
  --dimensions Name=LoadBalancer,Value=app/brightnest-prod-alb/xxx \
  --start-time 2026-03-08T00:00:00Z \
  --end-time 2026-03-08T23:59:59Z \
  --period 300 \
  --statistics Average
```

**Common causes:**
- Slow database queries (check RDS Performance Insights)
- Insufficient ECS CPU/memory
- Missing indexes
- Lack of caching

### Issue: 502 Bad Gateway

**Causes:**
- All targets unhealthy
- Task crashed before health check passed
- Security group blocking ALB → ECS traffic

**Fix:**
```powershell
# Check target health
aws elbv2 describe-target-health --target-group-arn <arn>

# Expected: "State": "healthy"
```

### Issue: Database connection timeout

**Diagnosis:**
1. Check security group allows port 3306 from ECS
2. Verify RDS endpoint is correct in Secrets Manager
3. Test from ECS task:

```bash
# Get shell on ECS task
aws ecs execute-command --cluster brightnest-prod-cluster --task <task-id> --command "/bin/sh"

# Test connection
nc -zv brightnest-prod.xxx.rds.amazonaws.com 3306
```

---

## C. Rollback Procedures

### Rollback ECS Deployment

**Quick rollback to previous task definition:**

```powershell
# List task definition revisions
aws ecs list-task-definitions --family-prefix brightnest-app

# Update service to previous revision
aws ecs update-service \
  --cluster brightnest-prod-cluster \
  --service brightnest-app-service \
  --task-definition brightnest-app:5
```

### Rollback Database Migration

**If Flyway/Liquibase migration failed:**

```sql
-- Flyway repair (marks failed migration as fixed)
mvn flyway:repair

-- Manual rollback (if you have down migrations)
SOURCE db/migration/rollback/V10__rollback.sql;
```

**If data corruption:**

```powershell
# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier brightnest-prod-temp \
  --db-snapshot-identifier <snapshot-before-migration>

# Migrate data back if needed
# Update task definition to point to restored instance temporarily
```

---

## D. Contact & Support

### AWS Support

- **Basic:** Included (community forums)
- **Developer:** $29/month (business hours email)
- **Business:** $100/month (24/7 phone + chat)

**Recommended:** Start with Developer, upgrade to Business when revenue > $10k/month

### Third-party Services

**Monitoring:**
- Datadog (APM): $15/host/month
- New Relic: $99/month
- Sentry (error tracking): Free tier available

**Status Page:**
- statuspage.io: $29/month
- Atlassian Statuspage: $29/month

**Uptime Monitoring:**
- UptimeRobot: Free for 50 monitors
- Pingdom: $10/month

---

## E. Success Metrics

### Week 1 Targets

- [ ] 99.9% uptime (< 10 minutes downtime)
- [ ] Average response time < 200ms
- [ ] Zero critical security incidents
- [ ] < 1% error rate
- [ ] 100 registered users

### Month 1 Targets

- [ ] 99.95% uptime
- [ ] P95 response time < 500ms
- [ ] 1,000 registered users
- [ ] 100 active daily users
- [ ] Customer satisfaction > 4.5/5

### Quarter 1 Targets

- [ ] 99.99% uptime (< 1 hour downtime)
- [ ] 10,000 registered users
- [ ] 1,000 daily active users
- [ ] Auto-scaling working (tested under load)
- [ ] Profitable unit economics

---

## F. Next Steps After Launch

1. **Marketing launch**
   - Press release
   - Social media campaign
   - Product Hunt launch

2. **User feedback collection**
   - In-app surveys
   - User interviews
   - Analytics tracking (Google Analytics, Mixpanel)

3. **Continuous improvement**
   - Weekly deployment cadence
   - A/B testing infrastructure
   - Feature experimentation

4. **Team expansion**
   - Hire DevOps engineer
   - Customer success team
   - QA automation engineer

5. **Advanced features**
   - Mobile app (React Native)
   - AI-powered recommendations
   - Advanced analytics dashboard
   - Multi-language support

---

**END OF GUIDE**

**Document Version:** 1.0  
**Last Updated:** March 8, 2026  
**Maintained By:** BrightNest DevOps Team

For questions or updates, please submit a pull request or contact devops@brightnest-academy.com

---

**🎉 Good luck with your production launch! 🚀**
