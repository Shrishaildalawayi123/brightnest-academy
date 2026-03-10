#!/usr/bin/env bash
# =============================================================
# setup-ssl.sh — Obtain Let's Encrypt SSL cert for BrightNest
#
# Run this on the EC2 server AFTER:
#   1. setup-ec2.sh has completed (Nginx installed + configured)
#   2. DNS A records point to this EC2 Elastic IP:
#        @ -> <elastic-ip>
#        www -> <elastic-ip>
#   3. Domain resolves: dig +short brightnest-academy.com
#
# USAGE (run on EC2 as root or with sudo):
#   sudo bash setup-ssl.sh
#
# Or with custom domain:
#   sudo DOMAIN=brightnest-academy.com bash setup-ssl.sh
# =============================================================
set -euo pipefail

DOMAIN="${DOMAIN:-brightnest-academy.com}"
WWW_DOMAIN="www.${DOMAIN}"
EMAIL="${CERTBOT_EMAIL:-admin@${DOMAIN}}"
WEBROOT="/var/www/certbot"
NGINX_AVAILABLE="/etc/nginx/sites-available/brightnest"
NGINX_ENABLED="/etc/nginx/sites-enabled/brightnest"

echo "============================================="
echo "  BrightNest Academy — SSL Setup"
echo "  Domain  : ${DOMAIN}"
echo "  Email   : ${EMAIL}"
echo "============================================="

# --------------------------------------------------------
# 0) Verify running as root
# --------------------------------------------------------
if [ "$(id -u)" != "0" ]; then
    echo "ERROR: run as root (sudo bash setup-ssl.sh)" >&2
    exit 1
fi

# --------------------------------------------------------
# 1) Verify DNS resolves to this machine
# --------------------------------------------------------
echo "[1/5] Checking DNS resolution..."
MY_IP=$(curl -fsSL --max-time 5 https://checkip.amazonaws.com || true)
DOMAIN_IP=$(dig +short "${DOMAIN}" | grep -E '^[0-9]+\.' | head -1 || true)

if [ -z "${DOMAIN_IP}" ]; then
    echo "ERROR: ${DOMAIN} does not resolve. Update GoDaddy A record to ${MY_IP} and wait for propagation." >&2
    exit 1
fi

if [ "${DOMAIN_IP}" != "${MY_IP}" ]; then
    echo "WARNING: ${DOMAIN} resolves to ${DOMAIN_IP}, but this server is ${MY_IP}." >&2
    echo "         DNS may not have propagated yet. Certbot ACME validation will fail." >&2
    read -r -p "Continue anyway? (y/N): " confirm
    [ "${confirm}" = "y" ] || exit 1
fi
echo "  DNS OK: ${DOMAIN} -> ${DOMAIN_IP}"

# --------------------------------------------------------
# 2) Install certbot via snap (idempotent)
# --------------------------------------------------------
echo "[2/5] Installing/refreshing certbot..."
if ! command -v certbot &> /dev/null; then
    snap install core 2>/dev/null || true
    snap refresh core 2>/dev/null || true
    snap install --classic certbot
    ln -sf /snap/bin/certbot /usr/bin/certbot
fi
certbot --version

# --------------------------------------------------------
# 3) Prepare webroot for ACME HTTP-01 challenge
# --------------------------------------------------------
echo "[3/5] Preparing Nginx for ACME challenge..."
mkdir -p "${WEBROOT}"

# Write a minimal HTTP-only Nginx config for ACME validation
# (the full nginx-brightnest.conf SSL block will fail without certs)
cat > /etc/nginx/sites-available/brightnest-acme << ACME_CONF
server {
    listen 80;
    listen [::]:80;
    server_name ${DOMAIN} ${WWW_DOMAIN};

    location /.well-known/acme-challenge/ {
        root ${WEBROOT};
    }

    location / {
        return 200 'ACME validation server';
    }
}
ACME_CONF

# Temporarily use ACME config (swap back after cert is obtained)
if [ -f "${NGINX_ENABLED}" ]; then
    cp "${NGINX_ENABLED}" /tmp/brightnest-nginx-backup.conf || true
fi
ln -sf /etc/nginx/sites-available/brightnest-acme "${NGINX_ENABLED}"
nginx -t
systemctl reload nginx

# --------------------------------------------------------
# 4) Obtain certificate
# --------------------------------------------------------
echo "[4/5] Obtaining certificate (${DOMAIN} + ${WWW_DOMAIN})..."
certbot certonly \
    --webroot \
    --webroot-path="${WEBROOT}" \
    --non-interactive \
    --agree-tos \
    --email "${EMAIL}" \
    --domains "${DOMAIN},${WWW_DOMAIN}" \
    --rsa-key-size 4096 \
    --hsts \
    --redirect

CERT_PATH="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
if [ ! -f "${CERT_PATH}" ]; then
    echo "ERROR: Certificate not found at ${CERT_PATH}" >&2
    exit 1
fi
echo "  Certificate obtained: ${CERT_PATH}"

# --------------------------------------------------------
# 5) Re-enable full Nginx config (HTTPS)
# --------------------------------------------------------
echo "[5/5] Activating HTTPS Nginx config..."

# Restore the full nginx-brightnest.conf (has both HTTP and HTTPS blocks)
REPO_CONF="/opt/repo/shrishail-academy/deploy/aws/nginx-brightnest.conf"
if [ -f "${REPO_CONF}" ]; then
    # Update domain references if needed
    sed -i "s|brightnest-academy.com|${DOMAIN}|g" "${REPO_CONF}"
    cp "${REPO_CONF}" "${NGINX_AVAILABLE}"
    ln -sf "${NGINX_AVAILABLE}" "${NGINX_ENABLED}"
elif [ -f "/tmp/brightnest-nginx-backup.conf" ]; then
    cp "/tmp/brightnest-nginx-backup.conf" "${NGINX_ENABLED}"
fi

nginx -t
systemctl reload nginx

# --------------------------------------------------------
# 6) Auto-renewal cron (certbot renews when <30 days left)
# --------------------------------------------------------
echo "Setting up auto-renewal..."
# certbot snap auto-renews via systemd timer; also add a cron fallback
if ! crontab -l 2>/dev/null | grep -q "certbot renew"; then
    (crontab -l 2>/dev/null; echo "0 3 * * * certbot renew --quiet --deploy-hook 'systemctl reload nginx'") | crontab -
fi
echo "  Auto-renewal cron configured (daily at 03:00)"

# --------------------------------------------------------
# Done
# --------------------------------------------------------
echo ""
echo "============================================="
echo "  SSL Setup COMPLETE  ✓"
echo "  https://${DOMAIN}"
echo "============================================="
echo ""
echo "Test your SSL: curl -I https://${DOMAIN}/health"
echo "SSL Labs:      https://www.ssllabs.com/ssltest/analyze.html?d=${DOMAIN}"
