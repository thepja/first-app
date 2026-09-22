#!/usr/bin/env bash
# Déploie le chart Helm dans l'environnement donné et attend que les pods soient prêts.
# En cas d'échec, Helm revient automatiquement à la version précédente (--atomic).
# Usage : scripts/helm-deploy.sh <staging|production> <image:tag> [options helm...]
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <staging|production> <image:tag> [options helm...]" >&2
  exit 1
fi

env="$1"
image="$2"
shift 2
chart="$(cd "$(dirname "$0")/.." && pwd)/helm/first-app"

if [[ ! -f "$chart/values-${env}.yaml" ]]; then
  echo "Environnement inconnu : ${env}" >&2
  exit 1
fi

helm upgrade --install first-app "$chart" \
  --namespace "first-app-${env}" --create-namespace \
  -f "$chart/values-${env}.yaml" \
  --set image.repository="${image%:*}" \
  --set-string image.tag="${image##*:}" \
  --atomic --wait --timeout 5m \
  --history-max 10 \
  "$@"
