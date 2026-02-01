# Build: run from repo root: docker build -f gateway/Dockerfile .
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY gateway/pom.xml gateway/
COPY gateway/src gateway/src
RUN apk add --no-cache maven && \
    mvn -f gateway/pom.xml dependency:go-offline -B -q && \
    mvn -f gateway/pom.xml package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine
RUN adduser -D -s /bin/sh app
WORKDIR /app
COPY --from=builder /build/gateway/target/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
