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
| **Release** | tag `v*` | création d'une release GitHub avec le JAR (environnement `production`) |

Pour déployer une version :

```bash
git tag v1.0.0 && git push origin v1.0.0
```

L'image est alors publiée avec les tags `1.0.0`, `sha-xxxx`, et `latest` pour la branche `main`.
