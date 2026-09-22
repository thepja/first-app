#!/usr/bin/env bash
# Déploie le chart Helm dans l'environnement donné et attend la fin du rollout.
# En cas d'échec, Helm revient automatiquement à la version précédente (--atomic).
# Usage : scripts/helm-deploy.sh <staging|production> <image:tag> [options helm...]
set -euo pipefail

env="$1"
image="$2"
shift 2
chart="$(dirname "$0")/../helm/first-app"

helm upgrade --install first-app "$chart" \
  --namespace "first-app-${env}" --create-namespace \
  -f "$chart/values-${env}.yaml" \
  --set image.repository="${image%:*}" \
  --set image.tag="${image##*:}" \
  --atomic --wait --timeout 5m \
  "$@"
