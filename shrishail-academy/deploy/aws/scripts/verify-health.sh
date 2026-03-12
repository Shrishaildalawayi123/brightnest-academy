#!/usr/bin/env bash
set -euo pipefail

HEALTH_URL="${1:-http://127.0.0.1:8080/health}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

for i in $(seq 1 "$MAX_ATTEMPTS"); do
  if curl -fsS --max-time 4 "$HEALTH_URL" | grep -q '"status"'; then
    echo "Health check passed on attempt $i: $HEALTH_URL"
    exit 0
  fi
  echo "Attempt $i/$MAX_ATTEMPTS failed; retrying in ${SLEEP_SECONDS}s..."
  sleep "$SLEEP_SECONDS"
done

echo "Health check failed after ${MAX_ATTEMPTS} attempts: $HEALTH_URL" >&2
exit 1
