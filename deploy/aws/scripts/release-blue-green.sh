#!/usr/bin/env bash
# deploy <image@sha256:digest> <git-sha> | rollback
set -Eeuo pipefail
umask 077
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
RELEASE_DIR=${RELEASE_DIR:-/opt/brightnest}
ENV_FILE=${ENV_FILE:-${RELEASE_DIR}/.env}
UPSTREAM_FILE=${UPSTREAM_FILE:-/etc/nginx/brightnest/active-backend.conf}
PUBLIC_URL=${PUBLIC_URL:-https://brightnest-academy.com}
DOCKER_NETWORK=${DOCKER_NETWORK:-brightnest}
STATE_FILE="${RELEASE_DIR}/active-release"
PREVIOUS_FILE="${RELEASE_DIR}/previous-release"
MODE=${1:-}
fail() { echo "$*" >&2; exit 1; }
[[ "$MODE" == deploy || "$MODE" == rollback ]] || fail 'Usage: release-blue-green.sh deploy <digest-ref> <git-sha> | rollback'
[[ "$PUBLIC_URL" =~ ^https://[A-Za-z0-9.-]+$ ]] || fail 'PUBLIC_URL must be an HTTPS origin'
mkdir -p "$RELEASE_DIR"
exec 9>"${RELEASE_DIR}/release.lock"
flock -n 9 || fail 'Another release operation is in progress'
[[ -f "$STATE_FILE" && -f "$UPSTREAM_FILE" ]] || fail 'Initialize active-release and the Nginx upstream using the runbook first'
read -r active_slot active_image active_revision active_port < "$STATE_FILE"
validate_record() {
  local slot=$1 image=$2 revision=$3 port=$4
  case "$slot:$port" in blue:18080|green:18081) ;;
    legacy:8080) [[ "$image" == legacy && "$revision" == legacy ]] && return 0; return 1 ;;
    *) return 1 ;;
  esac
  [[ "$image" =~ ^ghcr.io/[a-z0-9._/-]+@sha256:[a-f0-9]{64}$ && "$revision" =~ ^[a-f0-9]{40}$ ]]
}
validate_record "$active_slot" "$active_image" "$active_revision" "$active_port" || fail 'Invalid active release record'
[[ "$(cat "$UPSTREAM_FILE")" == "server 127.0.0.1:${active_port};" ]] || fail 'Release state disagrees with Nginx upstream; refusing to guess'
sudo nginx -t
work=$(mktemp -d "${RELEASE_DIR}/release.XXXXXX")
sudo cp "$UPSTREAM_FILE" "$work/upstream.before"
cp "$STATE_FILE" "$work/state.before"
if [[ -f "$PREVIOUS_FILE" ]]; then cp "$PREVIOUS_FILE" "$work/previous.before"; fi
switched=false
candidate_started=false
finished=false
candidate_container=''
# Return success only for HTTP 200, dependency readiness UP, and the intended revision.
probe() {
  local url=$1 revision=$2 code
  code=$(curl -sS --max-time 10 -o "$work/health.json" -w '%{http_code}' \
    -H 'X-Forwarded-Proto: https' "$url") || return 1
  [[ "$code" == 200 ]] || return 1
  python3 - "$work/health.json" "$revision" <<'PY'
import json, sys
try:
    data = json.load(open(sys.argv[1]))
    ok = data.get('status') == 'UP' and (sys.argv[2] == 'legacy' or data.get('revision') == sys.argv[2])
except (OSError, ValueError):
    ok = False
sys.exit(0 if ok else 1)
PY
}
wait_ready() {
  local url=$1 revision=$2
  for ((attempt=1; attempt<=${HEALTH_ATTEMPTS:-30}; attempt++)); do
    if probe "$url" "$revision"; then return 0; fi
    sleep "${HEALTH_INTERVAL:-4}"
  done
  return 1
}
# Install in the same directory, then rename: Nginx never sees a partial include.
install_upstream() {
  sudo install -m 644 "$1" "${UPSTREAM_FILE}.next"
  sudo mv -f "${UPSTREAM_FILE}.next" "$UPSTREAM_FILE"
}
cleanup() {
  local rc=$?
  trap - EXIT
  if [[ "$finished" != true && "$switched" == true ]]; then
    echo 'Release failed; restoring the previous upstream and release records' >&2
    if install_upstream "$work/upstream.before" && sudo nginx -t && sudo systemctl reload nginx \
      && wait_ready "$PUBLIC_URL/health" "$active_revision"; then
      cp "$work/state.before" "$STATE_FILE"
      if [[ -f "$work/previous.before" ]]; then cp "$work/previous.before" "$PREVIOUS_FILE"; else rm -f "$PREVIOUS_FILE"; fi
      switched=false
    else
      echo 'CRITICAL: rollback verification failed; retaining both containers and transaction evidence' >&2
      echo "Transaction evidence: $work" >&2
      exit 1
    fi
  fi
  if [[ "$finished" != true && "$candidate_started" == true && "$switched" == false ]]; then
    docker rm -f "$candidate_container" >/dev/null || true
  fi
  rm -rf "$work"
  exit "$rc"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
# Prove the serving release works before touching an inactive slot.
wait_ready "$PUBLIC_URL/health" "$active_revision" || fail 'Existing release is not healthy; refusing deployment'
if [[ "$MODE" == deploy ]]; then
  [[ $# == 3 ]] || fail 'deploy requires an immutable image reference and Git SHA'
  image=$2
  revision=$3
  if [[ "$active_slot" == blue ]]; then slot=green; port=18081; else slot=blue; port=18080; fi
  validate_record "$slot" "$image" "$revision" "$port" || fail 'Invalid immutable image reference or Git SHA'
  python3 "$SCRIPT_DIR/validate-release-env.py" "$ENV_FILE"
  docker network inspect "$DOCKER_NETWORK" >/dev/null
  docker pull "$image"
  [[ "$(docker image inspect --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}' "$image")" == "$revision" ]] \
    || fail 'Image revision label does not match the tested commit'
  candidate_container="brightnest-${slot}"
  docker rm -f "$candidate_container" >/dev/null 2>&1 || true
  # Set before docker run so cleanup also handles a partially created container.
  candidate_started=true
  docker run -d --name "$candidate_container" --network "$DOCKER_NETWORK" \
    --restart unless-stopped --read-only --tmpfs /tmp:rw,noexec,nosuid,size=100m \
    --security-opt no-new-privileges:true --memory 512m --cpus 1 \
    --log-opt max-size=10m --log-opt max-file=5 \
    --env-file "$ENV_FILE" -e SPRING_PROFILES_ACTIVE=prod \
    -e RATE_LIMIT_BACKEND=redis -e RESUME_STORAGE_S3_ENABLED=true \
    -p "127.0.0.1:${port}:8080" "$image" >/dev/null
else
  [[ $# == 1 && -f "$PREVIOUS_FILE" ]] || fail 'No previous release is recorded'
  read -r slot image revision port < "$PREVIOUS_FILE"
  validate_record "$slot" "$image" "$revision" "$port" || fail 'Invalid previous release record'
  [[ "$port" != "$active_port" ]] || fail 'Previous release is already active'
  # Previous container stays running after deployment; do not recreate it with changed secrets.
fi
wait_ready "http://127.0.0.1:${port}/health" "$revision" || fail 'Candidate readiness failed'
printf 'server 127.0.0.1:%s;\n' "$port" > "$work/upstream.next"
# Mark before installation so even nginx -t/reload failures restore the old file.
switched=true
install_upstream "$work/upstream.next"
sudo nginx -t
sudo systemctl reload nginx
wait_ready "$PUBLIC_URL/health" "$revision" || fail 'HTTPS verification failed after switch'
cp "$work/state.before" "$work/previous.next"
printf '%s %s %s %s\n' "$slot" "$image" "$revision" "$port" > "$work/state.next"
mv -f "$work/previous.next" "$PREVIOUS_FILE"
mv -f "$work/state.next" "$STATE_FILE"
finished=true
echo "Release verified: slot=$slot revision=$revision; previous container retained"
