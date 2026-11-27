# syntax=docker/dockerfile:1

# alpine-образы Temurin публикуются только для linux/amd64; на Apple Silicon
# сборка идёт через эмуляцию либо с -noble тегами через эти ARG
ARG BUILD_IMAGE=maven:3.9-eclipse-temurin-17-alpine
ARG RUNTIME_IMAGE=eclipse-temurin:17-jre-alpine

FROM ${BUILD_IMAGE} AS build
WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline

COPY src ./src
COPY openapi ./openapi
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -Dmaven.test.skip=true

FROM ${RUNTIME_IMAGE}
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app

COPY --from=build --chown=app:app /workspace/target/notes-service-*.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
