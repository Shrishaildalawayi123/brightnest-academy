# BrightNest Academy – AWS Deployment Guide (Nginx → Docker → Spring Boot)

This guide matches the target production flow:

`User → Nginx (80/443) → Docker container (127.0.0.1:8080) → Spring Boot`

## 0) DNS cutover (GoDaddy → EC2)

You confirmed you want to use the root domain. Pointing `brightnest-academy.com` to EC2 will replace whatever is currently hosted on GoDaddy Website Builder.

Recommended approach:

1. Allocate an **Elastic IP** in AWS and attach it to your EC2 instance (so IP does not change).
2. In GoDaddy DNS, update:

- `@` A record → your EC2 **Elastic IP**
- `www` A record → your EC2 **Elastic IP** (or use a CNAME to `@` if you prefer)

3. Wait for propagation (can take minutes to hours).

Important: Certbot HTTP validation requires that `brightnest-academy.com` already resolves to this EC2 instance and that ports `80` and `443` are open in the Security Group.

## 1) Server prerequisites

- Ubuntu 22.04+ (typical EC2)
- Docker + Docker Compose plugin installed
- A domain name (`brightnest-academy.com` + `www`) pointing to the instance public IP (typically an Elastic IP)

Builds/tests **must** be produced with **Java 21** before you push or deploy images. Mockito/Byte Buddy/Jacoco in this repo are pinned to JDK 21 bytecode.

- Workstation check: `java -version` and `mvn -v` should both report 21.x
- Windows tip: set `JAVA_HOME` to your JDK 21 install (`setx JAVA_HOME "C:\\Program Files\\Java\\jdk-21" /M` and restart shells/VS Code)

Create a directory for env config (do not commit secrets):

- `/opt/brightnest/.env` (recommended location)
- `/opt/brightnest/docker-compose.yml`

Example (do not commit secrets):

```
DB_HOST=...
DB_PORT=3306
DB_NAME=shrishail_academy
DB_USER=...
DB_PASS=...
JWT_SECRET=... (64+ chars)
ADMIN_EMAIL=...
ADMIN_PASSWORD=...
PORT=8080
SPRING_PROFILES_ACTIVE=prod

# Optional overrides
# CORS_ORIGINS=https://brightnest-academy.com,https://www.brightnest-academy.com
# HTTPS_REQUIRED=true
# COOKIE_SECURE=true
```

## 2) Nginx reverse proxy (do NOT expose 8080)

Install Nginx:

```
sudo apt-get update
sudo apt-get install -y nginx
```

Add site config:

- Copy `deploy/aws/nginx-brightnest.conf` to `/etc/nginx/sites-available/brightnest`
- Symlink it into `/etc/nginx/sites-enabled/`

```
sudo cp /path/to/repo/shrishail-academy/deploy/aws/nginx-brightnest.conf /etc/nginx/sites-available/brightnest
sudo ln -s /etc/nginx/sites-available/brightnest /etc/nginx/sites-enabled/brightnest
sudo nginx -t
sudo systemctl reload nginx
```

This proxies to `http://127.0.0.1:8080` where the container is bound.

## 2.1) Application container (Docker Compose)

You have two common deployment options:

### Option A (recommended): pull a prebuilt image

Use a Compose file that pulls an image (Docker Hub or GHCR), then restart:

- bind `127.0.0.1:8080:8080` (so only Nginx can reach it)

Repo examples:

- Docker Hub (via GitHub Actions): the workflow uploads `docker-compose.prod.yml` and rewrites `build: .` to `image: <your_dockerhub_user>/brightnest-academy:latest` on the server.
- GHCR (alternative/manual):

- `deploy/aws/docker-compose.ghcr.yml`

Server location (example):

- `/opt/brightnest/docker-compose.yml`

### Option B: build on the server

Use:

- `docker-compose.prod.yml`

Note: it also binds `127.0.0.1:8080:8080` so you still keep 8080 private.

If you are using the included GitHub Actions workflow (`.github/workflows/deploy.yml`), it deploys into:

- `/opt/brightnest/docker-compose.yml`

and expects your secrets to already exist on the server at:

- `/opt/brightnest/.env`

- pulls `ghcr.io/<owner>/brightnest-academy:latest`
- runs it as `brightnest-academy`
- binds `127.0.0.1:8080:8080` (so only Nginx can reach it)

Start/refresh the container:

```
cd /opt/brightnest
sudo docker compose pull || true
sudo docker compose up -d
```

## 3) SSL certificate (Certbot)

The Nginx config (`nginx-brightnest.conf`) already includes full SSL/TLS configuration pointing to Let's Encrypt certificate paths. You must obtain the certificates before enabling the HTTPS server block.

### Initial setup

Install certbot:

```bash
sudo snap install core; sudo snap refresh core
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/bin/certbot
```

**Step 1**: First deploy with a temporary HTTP-only Nginx config to pass ACME validation:

```bash
# Start Nginx with only the port-80 server block active
sudo certbot certonly --webroot -w /var/www/certbot \
  -d brightnest-academy.com -d www.brightnest-academy.com \
  --non-interactive --agree-tos -m admin@brightnest-academy.com
```

**Step 2**: Once certificates are issued, deploy the full `nginx-brightnest.conf` (which has both HTTP redirect and HTTPS blocks):

```bash
sudo cp deploy/aws/nginx-brightnest.conf /etc/nginx/sites-available/brightnest
sudo nginx -t && sudo systemctl reload nginx
```

### Auto-renewal

Certbot's snap package installs a systemd timer for auto-renewal. Verify:

```bash
sudo certbot renew --dry-run
```

Certificates renew automatically every ~60 days. The timer runs twice daily.

## 4) AWS Security Group rules

Allow only:

- `22/tcp` from **your IP only**
- `80/tcp` from `0.0.0.0/0`
- `443/tcp` from `0.0.0.0/0`

Block everything else.

Critical: do NOT allow `8080` from the internet.

## 5) Monitoring + log rotation

### Nginx logs

Enable rotation (usually already installed):

- `/var/log/nginx/access.log`
- `/var/log/nginx/error.log`

Ubuntu uses logrotate by default:

- `/etc/logrotate.d/nginx`

### Docker logs

If you use the default `json-file` driver, configure rotation in `/etc/docker/daemon.json`:

```
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "5"
  }
}
```

Then restart docker:

```
sudo systemctl restart docker
```

### CPU / memory alerts

Minimum viable approach:

- Use CloudWatch Agent or your preferred monitoring stack.
- Set alarms on CPUUtilization and memory (via agent).

## 6) Health endpoint

Use the public health endpoint for:

- Docker healthcheck
- load balancer checks
- monitoring

Endpoint:

- `GET /health`

This endpoint is explicitly permitted by `SecurityConfig` and is safe for uptime checks.

If you prefer actuator for health checks, ensure `/actuator/health` remains publicly accessible.

## 7) Operational scripts (recommended)

Use the scripts under `deploy/aws/scripts` from the project root or copy them to `/opt/brightnest/deploy/aws/scripts` on the server.

- Deploy immutable image + health gate:

```bash
bash deploy/aws/scripts/deploy-release.sh ghcr.io/<owner>/brightnest-academy:<git-sha>
```

- Rollback to previous known-good image:

```bash
bash deploy/aws/scripts/rollback-release.sh
```

- Verify app health explicitly:

```bash
bash deploy/aws/scripts/verify-health.sh
```

- Create MySQL backup:

```bash
bash deploy/aws/scripts/backup-mysql.sh
```

- Install automated backup cron:

```bash
bash deploy/aws/scripts/install-backup-cron.sh
```

- Bootstrap all ops tasks in one step (chmod + cron + CloudWatch):

```bash
bash deploy/aws/scripts/bootstrap-ops.sh
```

## 8) Monitoring bootstrap (CloudWatch)

CloudWatch agent config is provided at:

- `deploy/aws/monitoring/cloudwatch-agent-config.json`

Setup command:

```bash
bash deploy/aws/scripts/setup-monitoring.sh
```

## 9) Full operations documentation

For the complete enterprise checklist, architecture, testing strategy, CI/CD design, and DR plan, see:

- `docs/operations/PRODUCTION_READINESS_GUIDE.md`
- `docs/operations/DEPLOYMENT_RUNBOOK.md`
- `docs/operations/BACKUP_DR_RUNBOOK.md`
