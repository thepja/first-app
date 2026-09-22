# syntax=docker/dockerfile:1

# Images de base épinglées par digest (mises à jour par Dependabot)
FROM eclipse-temurin:21-jdk-noble@sha256:4d271cd5e0624598cf563342f47281b09cb364bc13acbbd7251f49f83470018d AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
# Les tests et contrôles qualité sont exécutés par la CI avant la construction de l'image
ARG REVISION=0.0.0-SNAPSHOT
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package \
    -Drevision=${REVISION} -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true

FROM eclipse-temurin:21-jre-noble@sha256:7739f0ffce786528961eea6bf46d9610ee968ac6127c9b2e93494757bdecce9f
WORKDIR /app
COPY --from=build /build/target/first-app.jar app.jar
EXPOSE 8080
# Utilisateur non-root (uid 1000 = « ubuntu » dans l'image de base)
USER 1000:1000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
