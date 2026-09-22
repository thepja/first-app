# first-app

Petite application Java 21 (serveur HTTP sans dépendance) avec une chaîne CI/CD complète sur GitHub Actions.

## Endpoints

| Route | Réponse |
|-------|---------|
| `GET /health` | `OK` |
| `GET /hello?name=Alice` | `Hello, Alice!` |

## En local

```bash
./mvnw verify                 # compile + tests + packaging
java -jar target/first-app.jar
curl "localhost:8080/hello?name=Alice"

docker build -t first-app .
docker run -p 8080:8080 first-app
```

Le port est configurable via la variable d'environnement `PORT`.

## Pipeline CI/CD (`.github/workflows/ci-cd.yml`)

| Étape | Quand | Quoi |
|-------|-------|------|
| **Build & Test** | chaque push / PR | `mvn verify` (tests JUnit 5), JAR et rapports publiés en artefacts |
| **Docker image** | chaque push / PR | build de l'image + smoke test du conteneur ; push sur `ghcr.io/<owner>/first-app` hors PR |
| **Kubernetes (kind)** | chaque push / PR | `helm lint`, puis déploiement Kustomize **et** Helm sur un cluster kind éphémère, avec test du service (`helm test`) |
| **Deploy staging** | push sur la branche par défaut (`feat/1.0`) | déploie via Helm l'image `sha-xxxxxxx` dans le namespace `first-app-staging` |
| **Release & deploy production** | tag `v*` | release GitHub avec le JAR + déploiement Helm de l'image `X.Y.Z` dans `first-app-production` |

Pour déployer une version :

```bash
git tag v1.0.0 && git push origin v1.0.0
```

L'image est alors publiée avec les tags `1.0.0`, `sha-xxxx`, et `latest` pour la branche par défaut.

## Kubernetes

Deux façons de déployer, qui produisent les mêmes ressources :

- **Helm** (`helm/first-app/`) : utilisé par la CI pour les déploiements (historique des releases, rollback automatique en cas d'échec grâce à `--atomic`).
- **Kustomize** (`k8s/`) : alternative sans outil supplémentaire, testée aussi en CI.

⚠️ N'utilisez pas les deux sur le même namespace : Helm refusera de reprendre des ressources créées par Kustomize.

### Helm

```
helm/first-app/
├── Chart.yaml
├── values.yaml              # valeurs par défaut (image, ressources, ingress désactivé…)
├── values-staging.yaml      # 1 réplica
├── values-production.yaml   # 3 réplicas
└── templates/               # Deployment, Service, Ingress (optionnel), test helm
```

```bash
scripts/helm-deploy.sh staging ghcr.io/thepja/first-app:latest
helm test first-app -n first-app-staging
helm history first-app -n first-app-staging
helm rollback first-app -n first-app-staging     # retour à la version précédente
```

Pour exposer l'application, activer l'ingress dans le fichier de valeurs :

```yaml
ingress:
  enabled: true
  className: nginx
  hosts:
    - host: first-app.mondomaine.fr
      paths: [{ path: /, pathType: Prefix }]
```

### Kustomize

Manifests dans `k8s/` :

```
k8s/
├── base/                  # Deployment (probes /health, non-root, limites) + Service
└── overlays/
    ├── staging/           # namespace first-app-staging, 1 réplica
    └── production/        # namespace first-app-production, 3 réplicas
```

Déploiement manuel avec Kustomize :

```bash
scripts/k8s-deploy.sh staging ghcr.io/thepja/first-app:latest
kubectl -n first-app-staging port-forward svc/first-app 8080:80
```

### Brancher un vrai cluster

1. Dans *Settings → Environments*, créer les environnements `staging` et `production`
   (on peut ajouter une validation manuelle sur `production`).
2. Dans chacun, ajouter le secret `KUBE_CONFIG` : le kubeconfig encodé en base64
   (`base64 -w0 ~/.kube/config`). Sans ce secret, l'étape de déploiement est ignorée avec un avertissement.
3. L'image GHCR est privée par défaut : soit la rendre publique
   (*Packages → first-app → Package settings*), soit créer un secret de pull dans chaque namespace
   et le référencer (`imagePullSecrets` dans `values.yaml` pour Helm, ou dans le Deployment pour Kustomize).
