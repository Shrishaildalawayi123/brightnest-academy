#!/usr/bin/env bash
# =============================================================
# setup-ec2.sh — One-time EC2 instance bootstrap
# Tested on Ubuntu 22.04 LTS (t3.medium, ap-south-1 Mumbai)
#
# USAGE:
#   chmod +x setup-ec2.sh
#   sudo ./setup-ec2.sh
#
# After this script completes:
#   1. Create /opt/brightnest/.env  (copy from .env.example, fill secrets)
#   2. Copy docker-compose.prod.yml to /opt/brightnest/docker-compose.yml
#   3. Run deploy/aws/scripts/deploy.sh to start the application
#   4. Run certbot for SSL (see nginx section below)
# =============================================================
set -euo pipefail

echo "============================================="
echo "  BrightNest Academy — EC2 Bootstrap"
echo "============================================="

# --------------------------------------------------------
# 0) Verify running as root
# --------------------------------------------------------
if [ "$(id -u)" != "0" ]; then
  echo "ERROR: run as root (sudo ./setup-ec2.sh)" >&2
  exit 1
fi

# --------------------------------------------------------
# 1) System update
# --------------------------------------------------------
echo "[1/7] Updating system packages..."
apt-get update -qq
apt-get upgrade -y -qq

# --------------------------------------------------------
# 2) Install required packages
# --------------------------------------------------------
echo "[2/7] Installing prerequisites..."
apt-get install -y -qq \
  curl \
  ca-certificates \
  gnupg \
  lsb-release \
  nginx \
  certbot \
  python3-certbot-nginx \
  ufw \
  fail2ban \
  unzip \
  htop \
  jq

# --------------------------------------------------------
# 3) Install Docker (official repo)
# --------------------------------------------------------
echo "[3/7] Installing Docker..."
if ! command -v docker &> /dev/null; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg

  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    | tee /etc/apt/sources.list.d/docker.list > /dev/null

  apt-get update -qq
  apt-get install -y -qq \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin

  systemctl enable docker
  systemctl start docker
  echo "Docker installed: $(docker --version)"
else
  echo "Docker already installed: $(docker --version)"
fi

# Add ubuntu user to docker group (avoids sudo for docker commands)
usermod -aG docker ubuntu || true

# --------------------------------------------------------
# 4) Create application directory structure
# --------------------------------------------------------
echo "[4/7] Creating /opt/brightnest directory..."
mkdir -p /opt/brightnest
# Restrict to root; app reads secrets via env_file in compose
chmod 750 /opt/brightnest

# --------------------------------------------------------
# 5) Configure firewall (ufw)
# --------------------------------------------------------
echo "[5/7] Configuring firewall..."
ufw --force reset > /dev/null
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp   comment "SSH"
ufw allow 80/tcp   comment "HTTP (Let's Encrypt + redirect)"
ufw allow 443/tcp  comment "HTTPS"
# Port 8080 is NOT exposed externally — Nginx proxies to 127.0.0.1:8080
ufw --force enable
echo "UFW status:"
ufw status verbose

# --------------------------------------------------------
# 6) Harden SSH + configure fail2ban
# --------------------------------------------------------
echo "[6/7] Hardening SSH and enabling fail2ban..."

# Basic fail2ban jail for SSH
cat > /etc/fail2ban/jail.d/sshd.conf << 'SSH_JAIL'
[sshd]
enabled = true
port    = ssh
backend = systemd
maxretry = 5
findtime = 300
bantime  = 3600
SSH_JAIL

systemctl enable fail2ban
systemctl restart fail2ban

# --------------------------------------------------------
# 7) Set up Nginx site
# --------------------------------------------------------
echo "[7/7] Configuring Nginx..."

# Remove default site
rm -f /etc/nginx/sites-enabled/default

# Note: nginx-brightnest.conf references the domain; copy it
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_CONF_SRC="$(realpath "${SCRIPT_DIR}/../nginx-brightnest.conf")"

if [ -f "${NGINX_CONF_SRC}" ]; then
  cp "${NGINX_CONF_SRC}" /etc/nginx/sites-available/brightnest
  ln -sf /etc/nginx/sites-available/brightnest /etc/nginx/sites-enabled/brightnest
  nginx -t && systemctl reload nginx
  echo "Nginx configured from: ${NGINX_CONF_SRC}"
else
  echo "WARN: ${NGINX_CONF_SRC} not found; Nginx site not configured."
  echo "      Copy nginx-brightnest.conf manually to /etc/nginx/sites-available/brightnest"
fi

# --------------------------------------------------------
# Done
# --------------------------------------------------------
echo ""
echo "============================================="
echo "  Bootstrap COMPLETE"
echo "============================================="
echo ""
echo "NEXT STEPS:"
echo "  1. Create /opt/brightnest/.env (copy from .env.example, fill secrets)"
echo "     chmod 600 /opt/brightnest/.env"
echo ""
echo "  2. Copy docker-compose.prod.yml:"
echo "     cp /path/to/repo/shrishail-academy/docker-compose.prod.yml \\"
echo "        /opt/brightnest/docker-compose.yml"
echo ""
echo "  3. Obtain SSL certificate (domain must already resolve to this IP):"
echo "     certbot --nginx -d brightnest-academy.com -d www.brightnest-academy.com"
echo ""
echo "  4. Start the application:"
echo "     bash /path/to/repo/shrishail-academy/deploy/aws/scripts/deploy.sh"
echo ""
echo "  5. Verify health:"
echo "     curl -fsS https://brightnest-academy.com/health"
