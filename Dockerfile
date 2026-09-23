# syntax=docker/dockerfile:1

# Images de base épinglées par digest (mises à jour par Dependabot)

# 1. Front-end Angular
FROM node:24-alpine@sha256:ebfe2f90462722a7a4de65e91990e97fe0d401c70e0e762c5b53302f905ec1c1 AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npx ng build

# 2. Serveur Java, avec le front-end embarqué dans le JAR (classpath static/)
FROM eclipse-temurin:21-jdk-noble@sha256:4d271cd5e0624598cf563342f47281b09cb364bc13acbbd7251f49f83470018d AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
COPY --from=frontend /frontend/dist/frontend/browser src/main/resources/static
# Les tests et contrôles qualité sont exécutés par la CI avant la construction de l'image.
# Version : REVISION (CI), sinon 0.0.0-<sha court> quand Render construit l'image, sinon SNAPSHOT.
ARG REVISION=""
ARG RENDER_GIT_COMMIT=""
RUN --mount=type=cache,target=/root/.m2 \
    version="$REVISION"; \
    if [ -z "$version" ] && [ -n "$RENDER_GIT_COMMIT" ]; then \
      version="0.0.0-$(echo "$RENDER_GIT_COMMIT" | cut -c1-7)"; \
    fi; \
    ./mvnw -B -q package -Drevision="${version:-0.0.0-SNAPSHOT}" \
      -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true

FROM eclipse-temurin:21-jre-noble@sha256:7739f0ffce786528961eea6bf46d9610ee968ac6127c9b2e93494757bdecce9f
WORKDIR /app
COPY --from=build /build/target/first-app.jar app.jar
EXPOSE 8080
# Utilisateur non-root (uid 1000 = « ubuntu » dans l'image de base)
USER 1000:1000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
