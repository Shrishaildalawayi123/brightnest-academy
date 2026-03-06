#!/usr/bin/env bash
# =============================================================
# deploy.sh — Pull latest Docker image and restart application
# Run this on EC2 after any new image is pushed to Docker Hub.
#
# Called automatically by GitHub Actions (deploy.yml), or
# run manually:
#   bash deploy/aws/scripts/deploy.sh [IMAGE_TAG]
#
# Environment:
#   IMAGE_NAME  - Docker Hub image (default: derive from .env or arg)
#   IMAGE_TAG   - Tag to deploy (default: latest)
#   COMPOSE_DIR - Location of docker-compose.yml (default: /opt/brightnest)
# =============================================================
set -euo pipefail

COMPOSE_DIR="${COMPOSE_DIR:-/opt/brightnest}"
IMAGE_TAG="${1:-${IMAGE_TAG:-latest}}"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yml"

echo "============================================="
echo "  BrightNest Academy — Deploy"
echo "  Image tag : ${IMAGE_TAG}"
echo "  Compose   : ${COMPOSE_FILE}"
echo "============================================="

# --------------------------------------------------------
# Pre-flight checks
# --------------------------------------------------------
if [ ! -f "${COMPOSE_FILE}" ]; then
  echo "ERROR: ${COMPOSE_FILE} not found." >&2
  echo "       Copy docker-compose.prod.yml from the repo to ${COMPOSE_FILE}" >&2
  exit 1
fi

if [ ! -f "${COMPOSE_DIR}/.env" ]; then
  echo "ERROR: ${COMPOSE_DIR}/.env not found." >&2
  echo "       Create it from .env.example and populate all secrets." >&2
  exit 1
fi

# --------------------------------------------------------
# Capture currently running image for rollback
# --------------------------------------------------------
PREVIOUS_IMAGE=""
if docker ps --format '{{.Names}}' | grep -q '^brightnest-academy$'; then
  PREVIOUS_IMAGE=$(docker inspect --format='{{.Config.Image}}' brightnest-academy 2>/dev/null || true)
  echo "Current running image: ${PREVIOUS_IMAGE}"
fi

# --------------------------------------------------------
# Update the image tag in docker-compose.yml
# --------------------------------------------------------
# Replace any existing `image:` line under the app service
if grep -q "^[[:space:]]*image:" "${COMPOSE_FILE}"; then
  # Sed-replace the image tag only (preserve registry/repo prefix)
  CURRENT_IMAGE_LINE=$(grep "^[[:space:]]*image:" "${COMPOSE_FILE}" | head -1 | sed 's/[[:space:]]//g' | cut -d: -f2-)
  # Extract registry+repo (everything before the last colon)
  REPO_PREFIX=$(echo "${CURRENT_IMAGE_LINE}" | sed 's/:[^:]*$//')
  NEW_IMAGE="${REPO_PREFIX}:${IMAGE_TAG}"
  sed -i "s|^[[:space:]]*image:.*|    image: ${NEW_IMAGE}|" "${COMPOSE_FILE}"
  echo "Updated image to: ${NEW_IMAGE}"
else
  echo "WARN: No 'image:' line found in ${COMPOSE_FILE}; skipping tag update."
fi

# --------------------------------------------------------
# Pull and restart
# --------------------------------------------------------
cd "${COMPOSE_DIR}"

echo "Pulling image..."
docker compose pull

echo "Starting containers..."
docker compose up -d --remove-orphans

# --------------------------------------------------------
# Health check (30 attempts × 5 s = 150 s timeout)
# --------------------------------------------------------
echo "Waiting for health check..."
HEALTH_OK=false
for i in $(seq 1 30); do
  if curl -fsS --max-time 3 http://127.0.0.1:8080/health 2>/dev/null | grep -q '"status"'; then
    HEALTH_OK=true
    echo "Health check passed (attempt ${i})"
    break
  fi
  echo "  attempt ${i}/30 — not ready yet, waiting 5s..."
  sleep 5
done

# --------------------------------------------------------
# Rollback on failure
# --------------------------------------------------------
if [ "${HEALTH_OK}" != "true" ]; then
  echo ""
  echo "ERROR: Health check failed after 150 seconds!" >&2

  if [ -n "${PREVIOUS_IMAGE}" ]; then
    echo "Rolling back to: ${PREVIOUS_IMAGE}" >&2
    sed -i "s|^[[:space:]]*image:.*|    image: ${PREVIOUS_IMAGE}|" "${COMPOSE_FILE}"
    docker compose pull || true
    docker compose up -d
    echo "Rollback complete. Investigate logs:" >&2
    echo "  docker logs brightnest-academy --tail 100" >&2
  else
    echo "No previous image to roll back to — leaving new container running." >&2
  fi

  exit 1
fi

# --------------------------------------------------------
# Cleanup old images
# --------------------------------------------------------
echo "Cleaning up dangling images..."
docker image prune -f > /dev/null || true

echo ""
echo "============================================="
echo "  Deploy COMPLETE  ✓"
echo "  $(docker inspect --format='{{.Config.Image}}' brightnest-academy)"
echo "============================================="
