#!/usr/bin/env bash
# Déploie un overlay Kustomize avec une image donnée et attend la fin du rollout.
# Usage : scripts/k8s-deploy.sh <staging|production> <image:tag>
set -euo pipefail

overlay="$1"
image="$2"
namespace="first-app-${overlay}"
dir="$(dirname "$0")/../k8s/.deploy"

mkdir -p "$dir"
cat > "$dir/kustomization.yaml" <<YAML
resources:
  - ../overlays/${overlay}
images:
  - name: first-app
    newName: ${image%:*}
    newTag: "${image##*:}"
YAML

kubectl apply -k "$dir"
kubectl -n "$namespace" rollout status deployment/first-app --timeout=180s
