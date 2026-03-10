#!/usr/bin/env bash
set -euo pipefail

COMPOSE_DIR="${COMPOSE_DIR:-/opt/brightnest}"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yml"
PREVIOUS_FILE="${COMPOSE_DIR}/.previous_image"

if [ ! -f "$PREVIOUS_FILE" ]; then
  echo "No previous image snapshot found: $PREVIOUS_FILE" >&2
  exit 1
fi

PREVIOUS_IMAGE=$(cat "$PREVIOUS_FILE")
if [ -z "$PREVIOUS_IMAGE" ]; then
  echo "Previous image value is empty in $PREVIOUS_FILE" >&2
  exit 1
fi

echo "Rolling back to: $PREVIOUS_IMAGE"
sed -i "s|^[[:space:]]*image:.*|    image: ${PREVIOUS_IMAGE}|" "$COMPOSE_FILE"

cd "$COMPOSE_DIR"
docker compose pull || true
docker compose up -d --remove-orphans

"$(dirname "$0")/verify-health.sh"

echo "Rollback successful: $PREVIOUS_IMAGE"
