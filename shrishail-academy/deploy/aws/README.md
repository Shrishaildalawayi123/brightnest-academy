# BrightNest Academy – AWS Deployment Guide (Nginx → Docker → Spring Boot)

This guide matches the target production flow:

`User → Nginx (80/443) → Docker container (127.0.0.1:8080) → Spring Boot`

## 1) Server prerequisites

- Ubuntu 22.04+ (typical EC2)
- Docker + Docker Compose plugin installed
- A domain name (e.g. `brightnest-academy.com`) pointing to the instance public IP

Create a directory for env config:

- `/opt/brightnest/.env` (used by the GitHub Actions SSH deploy step)
- `/opt/brightnest/docker-compose.yml` (uploaded automatically by the CI/CD workflow)

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

The CI/CD workflow deploys using Docker Compose on the server:

- pulls `ghcr.io/<owner>/brightnest-academy:latest`
- runs it as `brightnest-academy`
- binds `127.0.0.1:8080:8080` (so only Nginx can reach it)

Compose file source in repo:

- `deploy/aws/docker-compose.ghcr.yml`

On the server it is placed as:

- `/opt/brightnest/docker-compose.yml`

## 3) SSL certificate (Certbot)

Install certbot for nginx:

```
sudo snap install core; sudo snap refresh core
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/bin/certbot
```

Issue and install the cert:

```
sudo certbot --nginx -d brightnest-academy.com -d www.brightnest-academy.com
```

Certbot will:

- configure TLS
- (optionally) redirect HTTP → HTTPS

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

Use Spring Boot actuator health for:

- Docker healthcheck
- load balancer checks
- monitoring

Endpoint:

- `GET /actuator/health`

In production config, it returns minimal info like:

```
{"status":"UP"}
```
