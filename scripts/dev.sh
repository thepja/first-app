#!/usr/bin/env bash
# Lance l'application en développement : API Java + PostgreSQL embarqué (port 8080) et Angular (port 4200).
# Les données sont conservées dans .dev-db/ entre deux lancements. Ctrl+C arrête tout.
# Prérequis : Java 21 et Node.js 24.
set -euo pipefail
cd "$(dirname "$0")/.."

command -v node >/dev/null || { echo "Node.js 24 est requis (https://nodejs.org)." >&2; exit 1; }
[ -d frontend/node_modules ] || (cd frontend && npm ci --no-audit --no-fund)

./mvnw -q spring-boot:test-run -Dspring-boot.run.main-class=com.example.app.DevApplication &
backend=$!
trap 'kill $backend 2>/dev/null' EXIT

(cd frontend && npx ng serve --open)
