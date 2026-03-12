#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <image-ref>"
  echo "Example: $0 ghcr.io/my-org/brightnest-academy:abc123"
  exit 1
fi

IMAGE_REF="$1"
COMPOSE_DIR="${COMPOSE_DIR:-/opt/brightnest}"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yml"
APP_NAME="brightnest-academy"

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "Compose file not found: $COMPOSE_FILE" >&2
  exit 1
fi

PREVIOUS_IMAGE=""
if docker ps --format '{{.Names}}' | grep -q "^${APP_NAME}$"; then
  PREVIOUS_IMAGE=$(docker inspect --format='{{.Config.Image}}' "$APP_NAME" 2>/dev/null || true)
fi

if [ -n "$PREVIOUS_IMAGE" ]; then
  echo "$PREVIOUS_IMAGE" > "${COMPOSE_DIR}/.previous_image"
fi

echo "Deploying image: $IMAGE_REF"
sed -i "s|^[[:space:]]*image:.*|    image: ${IMAGE_REF}|" "$COMPOSE_FILE"

cd "$COMPOSE_DIR"
docker compose pull
docker compose up -d --remove-orphans

"$(dirname "$0")/verify-health.sh"

echo "Deployment successful: $IMAGE_REF"
