#!/usr/bin/env bash
# Smoke test du conteneur : démarrage en lecture seule, /health, /hello, puis arrêt propre sur SIGTERM.
# Usage : scripts/smoke-test.sh <image>
set -euo pipefail

image="${1:?Usage: $0 <image>}"

docker run -d -p 8080:8080 --read-only --tmpfs /tmp --name first-app "$image"
trap 'docker rm -f first-app >/dev/null 2>&1 || true' EXIT
for _ in $(seq 1 30); do curl -fs http://localhost:8080/health && break; sleep 1; done
test "$(curl -fs 'http://localhost:8080/hello?name=CI')" = "Hello, CI!"
docker stop --time 10 first-app
docker logs first-app
